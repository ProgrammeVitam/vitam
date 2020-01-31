/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.functional.administration.migration.r7r8;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.collection.CloseableIterator;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.thread.VitamThreadFactory;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.AccessionRegisterDetail;
import fr.gouv.vitam.functional.administration.common.FunctionalBackupService;
import fr.gouv.vitam.functional.administration.common.exception.FunctionalBackupServiceException;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Data migration service.
 */
public class AccessionRegisterMigrationService {

    /**
     * Vitam Logger.
     */
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AccessionRegisterMigrationService.class);

    private final AccessionRegisterMigrationRepository accessionRegisterMigrationRepository;
    private final FunctionalBackupService functionalBackupService;

    private final AtomicBoolean isRunning = new AtomicBoolean();

    /**
     * Constructor
     */
    public AccessionRegisterMigrationService(FunctionalBackupService functionalBackupService) {
        this(new AccessionRegisterMigrationRepository(), functionalBackupService);
    }

    /**
     * Constructor for testing
     */
    @VisibleForTesting
    AccessionRegisterMigrationService(AccessionRegisterMigrationRepository dataMigrationRepository, FunctionalBackupService functionalBackupService) {
        this.accessionRegisterMigrationRepository = dataMigrationRepository;
        this.functionalBackupService = functionalBackupService;
    }

    public boolean isMigrationInProgress() {
        return isRunning.get();
    }

    public boolean tryStartMigration(MigrationAction migrationAction) {

        boolean lockAcquired = isRunning.compareAndSet(false, true);
        if (!lockAcquired) {
            // A migration is already running
            return false;
        }

        VitamThreadPoolExecutor.getDefaultExecutor().execute(() -> {
            try {
                LOGGER.info("Starting data migration");
                switch (migrationAction) {
                    case MIGRATE:
                        // To be executed in primary site
                        mongoDataUpdate();
                        break;
                    case PURGE:
                        // To be executed in secondary site
                        purge();
                        break;
                    default:
                        throw new IllegalArgumentException("Not implemented migration action");
                }
            } catch (Exception e) {
                LOGGER.error("A fatal error occurred during data migration", e);
            } finally {
                isRunning.set(false);
                LOGGER.info("Data migration finished");
            }
        });

        return true;
    }


    public void purge() throws InterruptedException {

        LOGGER.info(String.format("Start purge Accession Register (detail and summary) ..."));
        int nbThreads = Math.max(Runtime.getRuntime().availableProcessors(), 16);
        ExecutorService executor = Executors.newFixedThreadPool(nbThreads, VitamThreadFactory.getInstance());

        try {
            CompletableFuture.allOf(
                    new CompletableFuture[]{
                            CompletableFuture.runAsync(() -> accessionRegisterMigrationRepository.purgeMongo(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL), executor),
                            CompletableFuture.runAsync(() -> accessionRegisterMigrationRepository.purgeElasticsearch(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL), executor),
                            CompletableFuture.runAsync(() -> accessionRegisterMigrationRepository.purgeMongo(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY), executor),
                            CompletableFuture.runAsync(() -> accessionRegisterMigrationRepository.purgeElasticsearch(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY), executor)
                    }
            ).join();
        } finally {
            LOGGER.info(String.format("End purge Accession Register (detail and summary) ..."));
            executor.shutdown();
            executor.awaitTermination(10L, TimeUnit.MINUTES);
        }
    }


    void mongoDataUpdate() throws InterruptedException, DatabaseException, FunctionalBackupServiceException {
        processAccessionRegisters(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL);
        processAccessionRegisters(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY);
    }

    private void processAccessionRegisters(FunctionalAdminCollections collection) throws InterruptedException, DatabaseException, FunctionalBackupServiceException {
        try (CloseableIterator<List<Document>> bulkRegisterIterator =
                     accessionRegisterMigrationRepository.selectAccessionRegistesBulk(collection)) {
            processAccessionRegistersByBulk(bulkRegisterIterator, collection);
        }
    }

    private void processAccessionRegistersByBulk(Iterator<List<Document>> bulkRegisterIterator, FunctionalAdminCollections collection) throws InterruptedException, DatabaseException, FunctionalBackupServiceException {

        LOGGER.info(String.format("Updating %s ...", collection.getName()));
        int nbThreads = Math.max(Runtime.getRuntime().availableProcessors(), 16);
        ExecutorService executor = Executors.newFixedThreadPool(nbThreads, VitamThreadFactory.getInstance());

        StopWatch sw = StopWatch.createStarted();
        AtomicInteger updatedRegisters = new AtomicInteger();
        AtomicInteger registersWithErrors = new AtomicInteger();

        try {

            while (bulkRegisterIterator.hasNext()) {
                List<Document> bulkRegisters = bulkRegisterIterator.next();
                processRegisterBulk(executor, bulkRegisters, sw, updatedRegisters, registersWithErrors, collection);
            }

        } finally {
            executor.shutdown();
            executor.awaitTermination(10L, TimeUnit.MINUTES);
        }
    }

    private void processRegisterBulk(ExecutorService executor, List<Document> registersToUpdate,
                                     StopWatch sw, AtomicInteger updatedRegistersCount,
                                     AtomicInteger registersWithErrorsCount, FunctionalAdminCollections collection) throws DatabaseException, FunctionalBackupServiceException {

        List<Document> updatedRegisters = ListUtils.synchronizedList(new ArrayList<>());

        final Integer scopedTenant = VitamThreadUtils.getVitamSession().getTenantId();
        final String scopedXRequestId = VitamThreadUtils.getVitamSession().getRequestId();

        CompletableFuture[] futures =
                registersToUpdate.stream().map(register -> CompletableFuture.runAsync(() -> {
                    try {

                        VitamThreadUtils.getVitamSession().setTenantId(scopedTenant);
                        VitamThreadUtils.getVitamSession().setRequestId(scopedXRequestId);

                        switch (collection) {
                            case ACCESSION_REGISTER_DETAIL:
                                migrateAccessionRegisterDetail(register);
                                break;
                            case ACCESSION_REGISTER_SUMMARY:
                                migrateAccessionRegisterSummary(register);
                                break;
                        }

                        updatedRegisters.add(register);
                    } catch (Exception e) {
                        LOGGER.error(String.format("An error occurred during %s migration: %s %s",
                                collection.getName(), register.get(AccessionRegisterDetail.ID), e));
                    }
                }, executor)).toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(futures).join();

        switch (collection) {
            case ACCESSION_REGISTER_DETAIL:
                this.accessionRegisterMigrationRepository.bulkReplaceAccessionRegisters(updatedRegisters, FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL);

                // Save in offers
                for (Document doc : updatedRegisters) {
                    functionalBackupService.saveDocument(collection, doc);
                }

                break;
            case ACCESSION_REGISTER_SUMMARY:
                this.accessionRegisterMigrationRepository.bulkReplaceAccessionRegisters(updatedRegisters, FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY);
                break;
        }
        updatedRegistersCount.addAndGet(updatedRegisters.size());
        registersWithErrorsCount.addAndGet(registersToUpdate.size() - updatedRegisters.size());

        LOGGER.info(String.format(
                "%s migration progress : elapsed time= %d seconds, updated registers= %d, registers with errors= %d",
                collection.getName(), sw.getTime(TimeUnit.SECONDS), updatedRegistersCount.get(), registersWithErrorsCount.get()));
    }

    private void migrateAccessionRegisterDetail(Document register) {
        register.remove("Symbolic");
        ((Document) register.get("TotalObjectGroups")).remove("attached");
        ((Document) register.get("TotalObjectGroups")).remove("detached");
        ((Document) register.get("TotalObjectGroups")).remove("symbolicRemained");

        ((Document) register.get("TotalUnits")).remove("attached");
        ((Document) register.get("TotalUnits")).remove("detached");
        ((Document) register.get("TotalUnits")).remove("symbolicRemained");

        ((Document) register.get("TotalObjects")).remove("attached");
        ((Document) register.get("TotalObjects")).remove("detached");
        ((Document) register.get("TotalObjects")).remove("symbolicRemained");

        ((Document) register.get("ObjectSize")).remove("attached");
        ((Document) register.get("ObjectSize")).remove("detached");
        ((Document) register.get("ObjectSize")).remove("symbolicRemained");

        String opi = register.getString("Identifier");
        String opc = register.getString("OperationGroup");

        register.append("Opi", opi);
        register.append("Opc", opc);

        register.remove("Identifier");
        register.remove("OperationGroup");

        register.append("Events", Arrays.asList(new Document("Opc", opc)
                .append("OpType", "INGEST")
                .append("Gots", ((Document) register.get("TotalObjectGroups")).get("remained"))
                .append("Units", ((Document) register.get("TotalUnits")).get("remained"))
                .append("Objects", ((Document) register.get("TotalObjects")).get("remained"))
                .append("ObjSize", ((Document) register.get("ObjectSize")).get("remained"))
                .append("CreationDate", register.getString("StartDate"))
        ));
    }

    private void migrateAccessionRegisterSummary(Document register) {
        ((Document) register.get("TotalObjectGroups")).remove("attached");
        ((Document) register.get("TotalObjectGroups")).remove("detached");
        ((Document) register.get("TotalObjectGroups")).remove("symbolicRemained");

        ((Document) register.get("TotalUnits")).remove("attached");
        ((Document) register.get("TotalUnits")).remove("detached");
        ((Document) register.get("TotalUnits")).remove("symbolicRemained");

        ((Document) register.get("TotalObjects")).remove("attached");
        ((Document) register.get("TotalObjects")).remove("detached");
        ((Document) register.get("TotalObjects")).remove("symbolicRemained");

        ((Document) register.get("ObjectSize")).remove("attached");
        ((Document) register.get("ObjectSize")).remove("detached");
        ((Document) register.get("ObjectSize")).remove("symbolicRemained");
    }
}
