/*******************************************************************************
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
 *******************************************************************************/
package fr.gouv.vitam.metadata.core.migration.r7tor8;

import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.collection.CloseableIterator;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.functional.administration.common.AccessionRegisterDetail;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.bson.Document;

import java.util.*;
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

    private final AccessionRegisterMigrationRepository dataMigrationRepository;

    private final AtomicBoolean isRunning = new AtomicBoolean();

    /**
     * Constructor
     */
    public AccessionRegisterMigrationService() {
        this(new AccessionRegisterMigrationRepository());
    }

    /**
     * Constructor for testing
     */
    @VisibleForTesting
    AccessionRegisterMigrationService(AccessionRegisterMigrationRepository dataMigrationRepository) {
        this.dataMigrationRepository = dataMigrationRepository;
    }

    public boolean isMongoDataUpdateInProgress() {
        return isRunning.get();
    }

    public boolean tryStartMongoDataUpdate() {

        boolean lockAcquired = isRunning.compareAndSet(false, true);
        if (!lockAcquired) {
            // A migration is already running
            return false;
        }

        VitamThreadPoolExecutor.getDefaultExecutor().execute(() -> {
            try {
                LOGGER.info("Starting data migration");
                mongoDataUpdate();
            } catch (Exception e) {
                LOGGER.error("A fatal error occurred during data migration", e);
            } finally {
                isRunning.set(false);
                LOGGER.info("Data migration finished");
            }
        });

        return true;
    }

    void mongoDataUpdate(FunctionalAdminCollections accessionRegisterDetailCollection, FunctionalAdminCollections accessionRegisterSummaryCollection) throws InterruptedException {
        processAccessionRegisters(accessionRegisterDetailCollection);
        processAccessionRegisters(accessionRegisterSummaryCollection);
    }


    void mongoDataUpdate() throws InterruptedException {

        dataMigrationRepository.migrateIndexes();

        mongoDataUpdate(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL, FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY);
    }

    private void processAccessionRegisters(FunctionalAdminCollections collection) throws InterruptedException {
        try (CloseableIterator<List<Document>> bulkRegisterIterator =
            dataMigrationRepository.selectAccessionRegistesBulk(collection)) {
            processAccessionRegistersByBulk(bulkRegisterIterator, collection);
        }
    }

    private void processAccessionRegistersByBulk(Iterator<List<Document>> bulkRegisterIterator, FunctionalAdminCollections collection) throws InterruptedException {

        LOGGER.info(String.format("Updating %s ...", collection.getName()));
        int nbThreads = Math.max(Runtime.getRuntime().availableProcessors(), 16);
        ExecutorService executor = Executors.newFixedThreadPool(nbThreads);

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
                                     AtomicInteger registersWithErrorsCount, FunctionalAdminCollections collection) {
        
        List<Document> updatedRegisters = ListUtils.synchronizedList(new ArrayList<>());

        CompletableFuture[] futures =
            registersToUpdate.stream().map(register -> CompletableFuture.runAsync(() -> {
                try {
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

        this.dataMigrationRepository.bulkReplaceOrUpdateAccessionRegisters(updatedRegisters, collection);

        updatedRegistersCount.addAndGet(updatedRegisters.size());
        registersWithErrorsCount.addAndGet(registersToUpdate.size() - updatedRegisters.size());

        LOGGER.info(String.format(
            "%s migration progress : elapsed time= %d seconds, updated registers= %d, registers with errors= %d",
            collection.getName(), sw.getTime(TimeUnit.SECONDS), updatedRegistersCount.get(), registersWithErrorsCount.get()));
    }

    private void migrateAccessionRegisterDetail(Document register)  {
        register.remove("Symbolic");
        ((Document)register.get("TotalObjectGroups")).remove("attached");
        ((Document)register.get("TotalObjectGroups")).remove("detached");
        ((Document)register.get("TotalObjectGroups")).remove("symbolicRemained");

        ((Document)register.get("TotalUnits")).remove("attached");
        ((Document)register.get("TotalUnits")).remove("detached");
        ((Document)register.get("TotalUnits")).remove("symbolicRemained");

        ((Document)register.get("TotalObjects")).remove("attached");
        ((Document)register.get("TotalObjects")).remove("detached");
        ((Document)register.get("TotalObjects")).remove("symbolicRemained");

        ((Document)register.get("ObjectSize")).remove("attached");
        ((Document)register.get("ObjectSize")).remove("detached");
        ((Document)register.get("ObjectSize")).remove("symbolicRemained");

        register.put("Opc", register.get("Identifier"));
        register.put("Opi", register.get("OperationGroup"));
        register.remove("Identifier");
        register.remove("OperationGroup");

        register.put("Events", Arrays.asList(new Document()
                .append("Opc", register.get("Opc"))
                .append("OpType", "INGEST")
                .append("Gots", ((Document)register.get("TotalObjectGroups")).get("remained"))
                .append("Units", ((Document)register.get("TotalUnits")).get("remained"))
                .append("Objects", ((Document)register.get("TotalObjects")).get("remained"))
                .append("ObjSize", ((Document)register.get("ObjectSize")).get("remained"))
                .append("CreationDate", register.get("StartDate"))
        ));
    }

    private void migrateAccessionRegisterSummary(Document register)  {
        ((Document)register.get("TotalObjectGroups")).remove("attached");
        ((Document)register.get("TotalObjectGroups")).remove("detached");
        ((Document)register.get("TotalObjectGroups")).remove("symbolicRemained");

        ((Document)register.get("TotalUnits")).remove("attached");
        ((Document)register.get("TotalUnits")).remove("detached");
        ((Document)register.get("TotalUnits")).remove("symbolicRemained");

        ((Document)register.get("TotalObjects")).remove("attached");
        ((Document)register.get("TotalObjects")).remove("detached");
        ((Document)register.get("TotalObjects")).remove("symbolicRemained");

        ((Document)register.get("ObjectSize")).remove("attached");
        ((Document)register.get("ObjectSize")).remove("detached");
        ((Document)register.get("ObjectSize")).remove("symbolicRemained");
    }
}
