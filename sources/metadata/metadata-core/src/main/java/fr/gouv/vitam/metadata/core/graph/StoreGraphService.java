/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
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
package fr.gouv.vitam.metadata.core.graph;

import com.google.common.annotations.VisibleForTesting;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCursor;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.api.VitamRepositoryProvider;
import fr.gouv.vitam.common.database.utils.MetadataDocumentHelper;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.metrics.VitamCommonMetrics;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.core.database.collections.Unit;
import fr.gouv.vitam.metadata.core.metrics.CommonMetadataMetrics;
import fr.gouv.vitam.metadata.core.reconstruction.RestoreBackupService;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageAlreadyExistsClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.Order;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import fr.gouv.vitam.workspace.common.CompressInformation;
import io.prometheus.client.Histogram;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.mongodb.client.model.Projections.include;

/**
 * This class get units where calculated data are modified
 * Zip generated files and store the zipped file in the offer.
 */
public class StoreGraphService {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(StoreGraphService.class);

    public static final LocalDateTime INITIAL_START_DATE = LocalDateTime.of(1970, 1, 1, 0, 0);
    public static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss-SSS");

    public static final String GRAPH = "graph";
    public static final String UNDERSCORE = "_";
    public static final String ZIP_EXTENTION = ".zip";
    public static final String ZIP_PREFIX_NAME = "store_graph_";
    public static final String $_GTE = "$gte";
    public static final String $_LT = "$lt";
    static final int LAST_GRAPHSTORE_OFFERLOG_BATCH_SIZE = 1;

    private VitamRepositoryProvider vitamRepositoryProvider;
    private RestoreBackupService restoreBackupService;

    private final WorkspaceClientFactory workspaceClientFactory;
    private final StorageClientFactory storageClientFactory;

    private static AtomicBoolean alreadyRunningLock = new AtomicBoolean(false);

    /**
     * @param vitamRepositoryProvider
     * @param restoreBackupService
     * @param workspaceClientFactory
     * @param storageClientFactory
     */
    @VisibleForTesting
    public StoreGraphService(
        VitamRepositoryProvider vitamRepositoryProvider,
        RestoreBackupService restoreBackupService,
        WorkspaceClientFactory workspaceClientFactory,
        StorageClientFactory storageClientFactory
    ) {
        this.vitamRepositoryProvider = vitamRepositoryProvider;
        this.restoreBackupService = restoreBackupService;
        this.workspaceClientFactory = workspaceClientFactory;
        this.storageClientFactory = storageClientFactory;
    }

    /**
     * @param vitamRepositoryProvider
     */
    public StoreGraphService(VitamRepositoryProvider vitamRepositoryProvider) {
        this.vitamRepositoryProvider = vitamRepositoryProvider;
        this.restoreBackupService = new RestoreBackupService();
        this.workspaceClientFactory = WorkspaceClientFactory.getInstance();
        this.storageClientFactory = StorageClientFactory.getInstance();
    }

    /**
     * As the files generated are zip files.
     * There name is the last store date
     *
     * Load from the offer, the latest zip file
     * Get His name
     * The name of zip file is fromDate_endDate.zip yyyy-MM-dd-HH-mm-ss-SSS_yyyy-MM-dd-HH-mm-ss-SSS.zip
     * Format the name to LocalDateTime
     *
     * @param metadataCollections the concerned collection
     * @return The date of the last store operation
     * @throws StoreGraphException
     */
    public LocalDateTime getLastGraphStoreDate(MetadataCollections metadataCollections) throws StoreGraphException {
        try {
            DataCategory dataCategory = getDataCategory(metadataCollections);
            Iterator<OfferLog> offerLogIterator = restoreBackupService.getListing(
                VitamConfiguration.getDefaultStrategy(),
                null,
                dataCategory,
                null,
                null,
                Order.DESC,
                LAST_GRAPHSTORE_OFFERLOG_BATCH_SIZE
            );

            if (!offerLogIterator.hasNext()) {
                // Case where no offer log found. Means that no timestamp zip file saved yet in the offer
                return INITIAL_START_DATE;
            }

            return parseGraphEndDateFromFileName(offerLogIterator.next().getFileName());
        } catch (Exception e) {
            throw new StoreGraphException(e);
        }
    }

    public static LocalDateTime parseGraphStartDateFromFileName(String fileName) {
        return LocalDateTime.from(formatter.parse(fileName.split(UNDERSCORE, 2)[0]));
    }

    public static LocalDateTime parseGraphEndDateFromFileName(String fileName) {
        return LocalDateTime.from(formatter.parse(fileName.split(UNDERSCORE, 2)[1]));
    }

    private DataCategory getDataCategory(MetadataCollections metadataCollections) throws StoreGraphException {
        DataCategory dataCategory;
        switch (metadataCollections) {
            case UNIT:
                dataCategory = DataCategory.UNIT_GRAPH;
                break;
            case OBJECTGROUP:
                dataCategory = DataCategory.OBJECTGROUP_GRAPH;
                break;
            default:
                throw new StoreGraphException("DataCategory " + metadataCollections + " not managed");
        }
        return dataCategory;
    }

    public boolean isInProgress() {
        return alreadyRunningLock.get();
    }

    /**
     * If no graph store in progress, try to start one
     * Should be exposed in the API
     *
     * @return the map of collection:number of treated documents
     */
    public Map<MetadataCollections, Integer> tryStoreGraph() throws StoreGraphException {
        // Increment log shipping event
        CommonMetadataMetrics.LOG_SHIPPING_COUNTER.inc();

        boolean tryStore = alreadyRunningLock.compareAndSet(false, true);
        final Map<MetadataCollections, Integer> map = new HashMap<>();
        map.put(MetadataCollections.UNIT, 0);
        map.put(MetadataCollections.OBJECTGROUP, 0);
        Integer totalTreatedDocuments = 0;

        if (tryStore) {
            LOGGER.info("Start Graph store GOT and UNIT ...");

            try {
                CompletableFuture<Integer>[] futures = new CompletableFuture[] {
                    createCompletableFeature(MetadataCollections.UNIT, map),
                    createCompletableFeature(MetadataCollections.OBJECTGROUP, map),
                };
                // Start async the features
                CompletableFuture<Integer> result = CompletableFuture.allOf(futures)
                    .thenApply(v -> Stream.of(futures).map(CompletableFuture::join).collect(Collectors.toList()))
                    .thenApply(numberOfDocuments -> numberOfDocuments.stream().mapToInt(o -> o).sum())
                    .exceptionally(th -> {
                        throw new RuntimeException(th);
                    });

                totalTreatedDocuments = result.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new StoreGraphException(e);
            } finally {
                alreadyRunningLock.set(false);
            }
            LOGGER.info("Graph store GOT and UNIT total : " + totalTreatedDocuments + " : (" + map.toString() + ")");
        } else {
            LOGGER.info("Graph store is already running ...");
        }
        return map;
    }

    private CompletableFuture<Integer> createCompletableFeature(
        MetadataCollections metadataCollections,
        Map<MetadataCollections, Integer> map
    ) {
        return CompletableFuture.supplyAsync(
            () -> {
                Integer numberOfDocuments = storeGraph(metadataCollections);
                map.put(metadataCollections, numberOfDocuments);
                return numberOfDocuments;
            },
            VitamThreadPoolExecutor.getDefaultExecutor()
        );
    }

    /**
     * Should be called only for the method tryStoreGraph
     *
     * @param metadataCollections the collection concerned by the store graph
     * @return False if an exception occurs of if no unit graph was stored.
     * True if a stored zip file is created and saved in the storage.
     */
    private Integer storeGraph(MetadataCollections metadataCollections) {
        final GUID storeOperation = GUIDFactory.newGUID();
        VitamThreadUtils.getVitamSession().setTenantId(VitamConfiguration.getAdminTenant());
        VitamThreadUtils.getVitamSession().setRequestId(storeOperation);
        final String containerName = storeOperation.getId();

        final Histogram.Timer timer = CommonMetadataMetrics.LOG_SHIPPING_DURATION.labels(
            metadataCollections.name()
        ).startTimer();

        try {
            LOGGER.info("Start graph store " + metadataCollections.getName() + " ...");

            LocalDateTime lastStoreDate;
            try {
                lastStoreDate = getLastGraphStoreDate(metadataCollections);
            } catch (StoreGraphException e) {
                VitamCommonMetrics.CONSISTENCY_ERROR_COUNTER.labels(
                    String.valueOf(ParameterHelper.getTenantParameter()),
                    "StoreGraph"
                ).inc();
                LOGGER.error("[Consistency ERROR] : Error while getting the last store date from the offer ", e);

                return 0;
            }

            final LocalDateTime currentStoreDate = LocalDateUtil.now()
                .minus(VitamConfiguration.getStoreGraphOverlapDelay(), ChronoUnit.SECONDS);
            if (currentStoreDate.isBefore(lastStoreDate)) {
                VitamCommonMetrics.CONSISTENCY_ERROR_COUNTER.labels(
                    String.valueOf(ParameterHelper.getTenantParameter()),
                    "StoreGraph"
                ).inc();
                LOGGER.error(
                    "[Consistency ERROR] : The last store date should not be newer than the current date. " +
                    "Someone have modified the file name in the offer ?!!"
                );
                return 0;
            }

            // Date in MongoDB
            final String startDate = LocalDateUtil.getFormattedDateTimeForMongo(lastStoreDate);
            final String endDate = LocalDateUtil.getFormattedDateTimeForMongo(currentStoreDate);
            // Zip file name in the storage
            final String graph_store_name = lastStoreDate.format(formatter) + "_" + currentStoreDate.format(formatter);

            try {
                DataCategory dataCategory = getDataCategory(metadataCollections);
                String graphFolder = metadataCollections.name().toLowerCase() + "_" + GRAPH;
                String graphZipName = ZIP_PREFIX_NAME + metadataCollections.name().toLowerCase() + ZIP_EXTENTION;
                tryCreateContainer(containerName, graphFolder);

                // Find all units where _graph_last_persisted_date is between gte to lastStoreDate lt to currentStoreDate
                final BasicDBObject query = getMongoQuery(startDate, endDate);

                final Bson projection = getProjection(metadataCollections);
                final MongoCursor<Document> cursor = vitamRepositoryProvider
                    .getVitamMongoRepository(metadataCollections.getVitamCollection())
                    .findDocuments(query, VitamConfiguration.getBatchSize())
                    .projection(projection)
                    .iterator();

                List<Document> documents = new ArrayList<>();

                boolean is_graph_updated = false;
                // Save in the workspace: Json Array of MONGO_BATCH_SIZE of unit graph
                // Then all files will be zipped and saved in the storage
                int totalTreatedDocuments = 0;
                while (cursor.hasNext()) {
                    documents.add(cursor.next());
                    if (!cursor.hasNext() || documents.size() >= VitamConfiguration.getBatchSize()) {
                        totalTreatedDocuments = totalTreatedDocuments + documents.size();
                        storeInWorkspace(containerName, graphFolder, documents);
                        is_graph_updated = true;
                        documents = new ArrayList<>();
                    }
                }

                // Save in the storage the zipped file
                if (is_graph_updated) {
                    zipAndSaveInOffer(dataCategory, containerName, graphFolder, graphZipName, graph_store_name);
                    LOGGER.info("End save graph of " + metadataCollections.name() + " in the storage");
                    return totalTreatedDocuments;
                } else {
                    LOGGER.info(
                        "End without any save graph of " +
                        metadataCollections.name() +
                        " . No document found with updated graph"
                    );
                    return 0;
                }
            } catch (StoreGraphException e) {
                VitamCommonMetrics.CONSISTENCY_ERROR_COUNTER.labels(
                    String.valueOf(ParameterHelper.getTenantParameter()),
                    "StoreGraph"
                ).inc();
                LOGGER.error(
                    String.format(
                        "[Consistency ERROR] : Error while graph store (%s) form (%s) to (%s)",
                        metadataCollections.name(),
                        startDate,
                        endDate
                    ),
                    e
                );
                return 0;
            } finally {
                try {
                    cleanWorkspace(containerName);
                } catch (Exception e) {
                    LOGGER.error(e);
                }
            }
        } finally {
            timer.observeDuration();
        }
    }

    /**
     * @param metadataCollections
     * @return Mongo projection
     */
    private Bson getProjection(MetadataCollections metadataCollections) throws StoreGraphException {
        switch (metadataCollections) {
            case UNIT:
                return include(MetadataDocumentHelper.getComputedGraphUnitFields());
            case OBJECTGROUP:
                return include(MetadataDocumentHelper.getComputedGraphObjectGroupFields());
        }
        throw new StoreGraphException("The collection " + metadataCollections + " is not managed");
    }

    /**
     * Get between query
     *
     * @param startDate
     * @param endDate
     * @return BasicDBObject GRAPH_LAST_PERSISTED_DATE between startDate and endDate
     */
    private BasicDBObject getMongoQuery(String startDate, String endDate) {
        return new BasicDBObject(
            Unit.GRAPH_LAST_PERSISTED_DATE,
            new BasicDBObject($_GTE, startDate).append($_LT, endDate)
        );
    }

    /**
     * Clean workspace and remove the container of the graph store operation
     *
     * @param containerName
     */
    private void cleanWorkspace(String containerName) {
        try (WorkspaceClient workspaceClient = workspaceClientFactory.getClient()) {
            if (workspaceClient.isExistingContainer(containerName)) {
                workspaceClient.deleteContainer(containerName, true);
            }
        } catch (ContentAddressableStorageServerException | ContentAddressableStorageNotFoundException e) {
            LOGGER.error("Error while cleaning graph store container: ", e);
        }
    }

    /**
     * Create the graph store container in the workspace if not exists
     *
     * @param containerName
     * @param graphFolder the graph folder name
     * @throws StoreGraphException
     */
    private void tryCreateContainer(String containerName, String graphFolder) throws StoreGraphException {
        try (WorkspaceClient workspaceClient = workspaceClientFactory.getClient()) {
            if (!workspaceClient.isExistingContainer(containerName)) {
                workspaceClient.createContainer(containerName);
                workspaceClient.createFolder(containerName, graphFolder);
            }
        } catch (ContentAddressableStorageServerException | ContentAddressableStorageAlreadyExistException e) {
            throw new StoreGraphException(e);
        }
    }

    /**
     * @param dataCategory (Unit or GOT)
     * @param containerName
     * @param graphFolder the name of graph folder in the container
     * @param graphZipName the name if the zipFile in the container
     * @param graph_store_name the name of the zip file in the offer
     * @throws StoreGraphException
     */
    public void zipAndSaveInOffer(
        DataCategory dataCategory,
        String containerName,
        String graphFolder,
        String graphZipName,
        String graph_store_name
    ) throws StoreGraphException {
        try (final WorkspaceClient workspaceClient = workspaceClientFactory.getClient()) {
            CompressInformation compressInformation = new CompressInformation();
            Collections.addAll(compressInformation.getFiles(), graphFolder);
            compressInformation.setOutputFile(graphZipName);
            compressInformation.setOutputContainer(containerName);
            workspaceClient.compress(containerName, compressInformation);
        } catch (ContentAddressableStorageServerException e) {
            throw new StoreGraphException(e);
        }

        try (final StorageClient storageClient = storageClientFactory.getClient()) {
            final ObjectDescription description = new ObjectDescription();
            description.setWorkspaceContainerGUID(containerName);
            description.setWorkspaceObjectURI(graphZipName);

            storageClient.storeFileFromWorkspace(
                VitamConfiguration.getDefaultStrategy(),
                dataCategory,
                graph_store_name,
                description
            );
        } catch (
            StorageAlreadyExistsClientException | StorageNotFoundClientException | StorageServerClientException e
        ) {
            throw new StoreGraphException(e);
        }
    }

    /**
     * Connect to workspace and store the collection of documents as json file
     * The destination container is containerName
     * The destination folder is GRAPH
     *
     * @param documents
     */
    public void storeInWorkspace(String containerName, String graphFolder, List<Document> documents)
        throws StoreGraphException {
        InputStream inputStream;
        try {
            inputStream = JsonHandler.writeToInpustream(documents);
        } catch (InvalidParseOperationException e) {
            throw new StoreGraphException(e);
        }

        try (final WorkspaceClient workspaceClient = workspaceClientFactory.getClient()) {
            workspaceClient.putObject(containerName, graphFolder + "/" + GUIDFactory.newGUID().getId(), inputStream);
        } catch (ContentAddressableStorageServerException e) {
            throw new StoreGraphException(e);
        } finally {
            StreamUtils.closeSilently(inputStream);
        }
    }
}
