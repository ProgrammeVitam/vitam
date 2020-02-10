/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "https://cecill.info".
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
package fr.gouv.vitam.metadata.core.migration;

import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.collection.CloseableIterator;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.thread.VitamThreadFactory;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.core.database.collections.MongoDbMetadataRepository;
import fr.gouv.vitam.metadata.core.database.collections.Unit;
import fr.gouv.vitam.metadata.core.database.collections.UnitGraphModel;
import fr.gouv.vitam.metadata.core.graph.GraphLoader;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.time.StopWatch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Data migration service.
 */
public class DataMigrationService {

    /**
     * Vitam Logger.
     */
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(DataMigrationService.class);

    private final DataMigrationRepository dataMigrationRepository;

    private final AtomicBoolean isRunning = new AtomicBoolean();

    private final GraphLoader graphLoader;

    /**
     * Constructor
     */
    public DataMigrationService() {
        this(new DataMigrationRepository(),
            new GraphLoader(
                new MongoDbMetadataRepository(() -> MetadataCollections.UNIT.getCollection())
            )
        );
    }

    /**
     * Constructor for testing
     */
    @VisibleForTesting
    DataMigrationService(DataMigrationRepository dataMigrationRepository, GraphLoader graphLoader) {
        this.dataMigrationRepository = dataMigrationRepository;
        this.graphLoader = graphLoader;
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
                // Set admin tenant, and
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

    void mongoDataUpdate() throws InterruptedException {

        processUnits();

        processObjectGroups();
    }

    private void processUnits() throws InterruptedException {
        try (CloseableIterator<List<Unit>> bulkUnitIterator =
            dataMigrationRepository.selectUnitBulkInTopDownHierarchyLevel()) {

            processUnitsByBulk(bulkUnitIterator);
        }
    }

    private void processUnitsByBulk(Iterator<List<Unit>> bulkUnitIterator) throws InterruptedException {

        LOGGER.info("Updating units...");
        int nbThreads = Math.max(Runtime.getRuntime().availableProcessors(), 16);
        ExecutorService executor = Executors.newFixedThreadPool(nbThreads, VitamThreadFactory.getInstance());

        StopWatch sw = StopWatch.createStarted();
        AtomicInteger updatedUnits = new AtomicInteger();
        AtomicInteger unitsWithErrors = new AtomicInteger();

        try {

            while (bulkUnitIterator.hasNext()) {
                List<Unit> bulkUnits = bulkUnitIterator.next();
                processBulk(executor, bulkUnits, sw, updatedUnits, unitsWithErrors);
            }

        } finally {
            executor.shutdown();
            executor.awaitTermination(10L, TimeUnit.MINUTES);
        }
    }

    private void processBulk(ExecutorService executor, List<Unit> unitsToUpdate,
        StopWatch sw, AtomicInteger updatedUnitCount, AtomicInteger unitsWithErrorsCount) {

        Set<String> directParentIds = new HashSet<>();
        unitsToUpdate.forEach(unit -> directParentIds.addAll(unit.getCollectionOrEmpty(Unit.UP)));

        Map<String, UnitGraphModel> directParentById;
        try {
            directParentById = graphLoader.loadGraphInfo(directParentIds);
        } catch (MetaDataNotFoundException e) {
            throw new VitamRuntimeException(e);
        }

        List<Unit> updatedUnits = ListUtils.synchronizedList(new ArrayList<>());

        final Integer scopedTenant = VitamThreadUtils.getVitamSession().getTenantId();
        final String scopedXRequestId = VitamThreadUtils.getVitamSession().getRequestId();

        CompletableFuture[] futures =
            unitsToUpdate.stream().map(unit -> CompletableFuture.runAsync(() -> {
                try {
                    VitamThreadUtils.getVitamSession().setTenantId(scopedTenant);
                    VitamThreadUtils.getVitamSession().setRequestId(scopedXRequestId);

                    processDocument(unit, directParentById);
                    updatedUnits.add(unit);
                } catch (Exception e) {
                    LOGGER.error("An error occurred during unit migration: " + unit.getId(), e);
                }
            }, executor)).toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(futures).join();

        this.dataMigrationRepository.bulkReplaceUnits(updatedUnits);

        updatedUnitCount.addAndGet(updatedUnits.size());
        unitsWithErrorsCount.addAndGet(unitsToUpdate.size() - updatedUnits.size());

        LOGGER.info(String.format(
            "Unit migration progress : elapsed time= %d seconds, updated units= %d, units with errors= %d",
            sw.getTime(TimeUnit.SECONDS), updatedUnitCount.get(), unitsWithErrorsCount.get()));
    }

    private void processDocument(Unit unit, Map<String, UnitGraphModel> directParentById) {

        buildParentGraph(unit, directParentById);
        updateUnitSedaModel(unit);
    }

    private void buildParentGraph(Unit unit, Map<String, UnitGraphModel> directParentById) {

        Collection<String> directParentIds = unit.getCollectionOrEmpty(Unit.UP);
        UnitGraphModel unitGraphModel = new UnitGraphModel(unit);
        directParentIds.forEach(parentId -> {
            unitGraphModel.addParent(directParentById.get(parentId));
        });
        unit.mergeWith(unitGraphModel);
    }

    private void processObjectGroups() {

        StopWatch sw = StopWatch.createStarted();
        AtomicInteger updatedObjectCount = new AtomicInteger();

        try (CloseableIterator<List<String>> objectGroupListIterator = dataMigrationRepository
            .selectObjectGroupBulk()) {
            objectGroupListIterator.forEachRemaining(
                objectGroupIds -> {
                    processObjectGroupBulk(objectGroupIds);

                    updatedObjectCount.addAndGet(objectGroupIds.size());

                    LOGGER.info(String.format(
                        "Object Group migration progress : elapsed time= %d seconds, updated object groups= %d",
                        sw.getTime(TimeUnit.SECONDS), updatedObjectCount.get()));

                });
        }
    }

    private void processObjectGroupBulk(List<String> objectGroupIds) {
        dataMigrationRepository.bulkUpgradeObjectGroups(objectGroupIds);
    }

    private void updateUnitSedaModel(Unit unit) {
        SedaConverterTool.convertUnitToSeda21(unit);
    }
}
