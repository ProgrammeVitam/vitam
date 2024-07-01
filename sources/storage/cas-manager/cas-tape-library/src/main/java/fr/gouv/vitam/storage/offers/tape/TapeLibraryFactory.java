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
package fr.gouv.vitam.storage.offers.tape;

import com.google.common.annotations.VisibleForTesting;
import com.mongodb.client.MongoDatabase;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.security.IllegalPathException;
import fr.gouv.vitam.common.storage.tapelibrary.TapeDriveConf;
import fr.gouv.vitam.common.storage.tapelibrary.TapeLibraryConf;
import fr.gouv.vitam.common.storage.tapelibrary.TapeLibraryConfiguration;
import fr.gouv.vitam.common.storage.tapelibrary.TapeRobotConf;
import fr.gouv.vitam.storage.engine.common.collection.OfferCollections;
import fr.gouv.vitam.storage.engine.common.model.TapeCatalog;
import fr.gouv.vitam.storage.offers.tape.cas.AccessRequestManager;
import fr.gouv.vitam.storage.offers.tape.cas.AccessRequestReferentialRepository;
import fr.gouv.vitam.storage.offers.tape.cas.ArchiveCacheEvictionController;
import fr.gouv.vitam.storage.offers.tape.cas.ArchiveCacheStorage;
import fr.gouv.vitam.storage.offers.tape.cas.ArchiveReferentialRepository;
import fr.gouv.vitam.storage.offers.tape.cas.BackupFileStorage;
import fr.gouv.vitam.storage.offers.tape.cas.BasicFileStorage;
import fr.gouv.vitam.storage.offers.tape.cas.BucketTopologyHelper;
import fr.gouv.vitam.storage.offers.tape.cas.FileBucketTarCreatorManager;
import fr.gouv.vitam.storage.offers.tape.cas.IncompleteWriteOrderBootstrapRecovery;
import fr.gouv.vitam.storage.offers.tape.cas.ObjectReferentialRepository;
import fr.gouv.vitam.storage.offers.tape.cas.TapeLibraryContentAddressableStorage;
import fr.gouv.vitam.storage.offers.tape.cas.TarFileRapairer;
import fr.gouv.vitam.storage.offers.tape.cas.WriteOrderCreator;
import fr.gouv.vitam.storage.offers.tape.cas.WriteOrderCreatorBootstrapRecovery;
import fr.gouv.vitam.storage.offers.tape.dto.TapeLibrarySpec;
import fr.gouv.vitam.storage.offers.tape.exception.TapeCatalogException;
import fr.gouv.vitam.storage.offers.tape.exception.TapeCommandException;
import fr.gouv.vitam.storage.offers.tape.impl.TapeDriveManager;
import fr.gouv.vitam.storage.offers.tape.impl.TapeRobotManager;
import fr.gouv.vitam.storage.offers.tape.impl.catalog.TapeCatalogRepository;
import fr.gouv.vitam.storage.offers.tape.impl.catalog.TapeCatalogServiceImpl;
import fr.gouv.vitam.storage.offers.tape.impl.queue.QueueRepositoryImpl;
import fr.gouv.vitam.storage.offers.tape.metrics.AccessRequestMetrics;
import fr.gouv.vitam.storage.offers.tape.metrics.ArchiveCacheMetrics;
import fr.gouv.vitam.storage.offers.tape.metrics.DriveWorkerMetrics;
import fr.gouv.vitam.storage.offers.tape.metrics.OrderQueueMetrics;
import fr.gouv.vitam.storage.offers.tape.metrics.TapeCatalogMetrics;
import fr.gouv.vitam.storage.offers.tape.pool.TapeLibraryPoolImpl;
import fr.gouv.vitam.storage.offers.tape.spec.TapeCatalogService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeDriveService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeLibraryPool;
import fr.gouv.vitam.storage.offers.tape.spec.TapeRobotService;
import fr.gouv.vitam.storage.offers.tape.worker.TapeDriveWorkerManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.apache.commons.lang.StringUtils.isBlank;

public class TapeLibraryFactory {

    private static final TapeLibraryFactory instance = new TapeLibraryFactory();
    private static final long MB_BYTES = 1_000_000L;

    private final ConcurrentMap<String, TapeLibraryPool> tapeLibraryPool = new ConcurrentHashMap<>();
    private final TapeServiceCreator defaultTapeServiceCreator = new TapeServiceCreatorImpl();
    private final ConcurrentMap<String, TapeDriveWorkerManager> tapeDriveWorkerManagers = new ConcurrentHashMap<>();

    private TapeLibraryContentAddressableStorage tapeLibraryContentAddressableStorage;
    private BackupFileStorage backupFileStorage;
    private TapeCatalogService tapeCatalogService;
    private AccessRequestManager accessRequestManager;
    private ArchiveCacheStorage archiveCacheStorage;
    private TapeServiceCreator tapeServiceCreator = defaultTapeServiceCreator;

    private TapeLibraryFactory() {}

    public void initialize(TapeLibraryConfiguration configuration, MongoDatabase mongoDatabase)
        throws IOException, IllegalPathException {
        ParametersChecker.checkParameter("All params are required", configuration, mongoDatabase);
        ParametersChecker.checkParameter(
            "Missing cache capacity configuration settings",
            configuration.getCachedTarMaxStorageSpaceInMB(),
            configuration.getCachedTarEvictionStorageSpaceThresholdInMB(),
            configuration.getCachedTarSafeStorageSpaceThresholdInMB()
        );

        createWorkingDirectories(configuration);

        Map<String, TapeLibraryConf> libraries = configuration.getTapeLibraries();

        TapeCatalogRepository tapeCatalogRepository = new TapeCatalogRepository(
            mongoDatabase.getCollection(OfferCollections.TAPE_CATALOG.getName())
        );

        tapeCatalogService = new TapeCatalogServiceImpl(tapeCatalogRepository);

        BucketTopologyHelper bucketTopologyHelper = new BucketTopologyHelper(configuration.getTopology());

        ObjectReferentialRepository objectReferentialRepository = new ObjectReferentialRepository(
            mongoDatabase.getCollection(OfferCollections.TAPE_OBJECT_REFERENTIAL.getName())
        );
        ArchiveReferentialRepository archiveReferentialRepository = new ArchiveReferentialRepository(
            mongoDatabase.getCollection(OfferCollections.TAPE_ARCHIVE_REFERENTIAL.getName())
        );
        AccessRequestReferentialRepository accessRequestReferentialRepository = new AccessRequestReferentialRepository(
            mongoDatabase.getCollection(OfferCollections.ACCESS_REQUEST_REFERENTIAL.getName())
        );
        QueueRepositoryImpl readWriteQueue = new QueueRepositoryImpl(
            mongoDatabase.getCollection(OfferCollections.TAPE_QUEUE_MESSAGE.getName())
        );

        ArchiveCacheEvictionController archiveCacheEvictionController = new ArchiveCacheEvictionController(
            accessRequestReferentialRepository,
            objectReferentialRepository,
            bucketTopologyHelper
        );

        archiveCacheStorage = new ArchiveCacheStorage(
            configuration.getCachedTarStorageFolder(),
            bucketTopologyHelper,
            archiveCacheEvictionController,
            configuration.getCachedTarMaxStorageSpaceInMB() * MB_BYTES,
            configuration.getCachedTarEvictionStorageSpaceThresholdInMB() * MB_BYTES,
            configuration.getCachedTarSafeStorageSpaceThresholdInMB() * MB_BYTES
        );

        WriteOrderCreator writeOrderCreator = new WriteOrderCreator(archiveReferentialRepository, readWriteQueue);

        TarFileRapairer tarFileRapairer = new TarFileRapairer(objectReferentialRepository);
        WriteOrderCreatorBootstrapRecovery writeOrderCreatorBootstrapRecovery = new WriteOrderCreatorBootstrapRecovery(
            configuration.getInputTarStorageFolder(),
            archiveReferentialRepository,
            bucketTopologyHelper,
            writeOrderCreator,
            tarFileRapairer,
            archiveCacheStorage
        );

        backupFileStorage = new BackupFileStorage(
            archiveReferentialRepository,
            writeOrderCreator,
            BucketTopologyHelper.BACKUP_BUCKET,
            BucketTopologyHelper.BACKUP_FILE_BUCKET,
            configuration.getInputTarStorageFolder()
        );

        BasicFileStorage basicFileStorage = new BasicFileStorage(configuration.getInputFileStorageFolder());
        FileBucketTarCreatorManager fileBucketTarCreatorManager = new FileBucketTarCreatorManager(
            configuration,
            basicFileStorage,
            bucketTopologyHelper,
            objectReferentialRepository,
            archiveReferentialRepository,
            writeOrderCreator
        );

        accessRequestManager = new AccessRequestManager(
            objectReferentialRepository,
            archiveReferentialRepository,
            accessRequestReferentialRepository,
            archiveCacheStorage,
            bucketTopologyHelper,
            readWriteQueue,
            configuration.getMaxAccessRequestSize(),
            configuration.getReadyAccessRequestExpirationDelay(),
            configuration.getReadyAccessRequestExpirationUnit(),
            configuration.getReadyAccessRequestPurgeDelay(),
            configuration.getReadyAccessRequestPurgeUnit(),
            configuration.getAccessRequestCleanupTaskIntervalDelay(),
            configuration.getAccessRequestCleanupTaskIntervalUnit()
        );

        tapeLibraryContentAddressableStorage = new TapeLibraryContentAddressableStorage(
            basicFileStorage,
            objectReferentialRepository,
            archiveReferentialRepository,
            accessRequestManager,
            fileBucketTarCreatorManager,
            archiveCacheStorage,
            archiveCacheEvictionController,
            bucketTopologyHelper
        );

        // Start AccessRequest expiration handler
        accessRequestManager.startExpirationHandler();

        // Change all running orders to ready state
        readWriteQueue.initializeOnBootstrap();

        // Create tar creation orders from inputFiles folder
        writeOrderCreatorBootstrapRecovery.initializeOnBootstrap();

        // Create tar WriteOrders from inputTars folder
        fileBucketTarCreatorManager.initializeOnBootstrap();

        // Cleanup incomplete TAR files
        IncompleteWriteOrderBootstrapRecovery incompleteWriteOrderBootstrapRecovery =
            new IncompleteWriteOrderBootstrapRecovery(configuration.getTmpTarOutputStorageFolder());
        incompleteWriteOrderBootstrapRecovery.initializeOnBootstrap();

        // Initialize & start workers
        for (String tapeLibraryIdentifier : libraries.keySet()) {
            TapeLibraryConf tapeLibraryConf = libraries.get(tapeLibraryIdentifier);

            BlockingQueue<TapeRobotService> robotServices = new ArrayBlockingQueue<>(
                tapeLibraryConf.getRobots().size(),
                true
            );
            ConcurrentHashMap<Integer, TapeDriveService> driveServices = new ConcurrentHashMap<>();

            for (TapeRobotConf tapeRobotConf : tapeLibraryConf.getRobots()) {
                final TapeRobotService robotService = this.tapeServiceCreator.createRobotService(tapeRobotConf);
                robotServices.add(robotService);
            }

            for (TapeDriveConf tapeDriveConf : tapeLibraryConf.getDrives()) {
                final TapeDriveService tapeDriveService =
                    this.tapeServiceCreator.createTapeDriveService(configuration, tapeDriveConf);
                driveServices.put(tapeDriveConf.getIndex(), tapeDriveService);
            }

            if (robotServices.size() > 0 && driveServices.size() > 0) {
                tapeLibraryPool.putIfAbsent(
                    tapeLibraryIdentifier,
                    new TapeLibraryPoolImpl(tapeLibraryIdentifier, robotServices, driveServices)
                );
            }

            // init tape catalog
            TapeLibraryPool libraryPool = tapeLibraryPool.get(tapeLibraryIdentifier);
            Map<Integer, TapeCatalog> driveTape = new HashMap<>();
            try {
                TapeRobotService robot = libraryPool.checkoutRobotService();
                if (robot != null) {
                    try {
                        TapeLibrarySpec libraryState = robot.getLoadUnloadService().status();

                        driveTape = tapeCatalogService.init(tapeLibraryIdentifier, libraryState);
                    } catch (TapeCommandException e) {
                        throw new RuntimeException(
                            "Robot status command return ko :" + JsonHandler.unprettyPrint(e.getDetails()),
                            e
                        );
                    } finally {
                        libraryPool.pushRobotService(robot);
                    }
                }
            } catch (InterruptedException | TapeCatalogException e) {
                throw new RuntimeException(e);
            }

            // Start all workers
            TapeDriveWorkerManager tapeDriveWorkerManager = new TapeDriveWorkerManager(
                readWriteQueue,
                archiveReferentialRepository,
                accessRequestManager,
                libraryPool,
                driveTape,
                configuration.getInputTarStorageFolder(),
                configuration.isForceOverrideNonEmptyCartridges(),
                archiveCacheStorage,
                tapeCatalogService,
                tapeLibraryConf.getFullCartridgeDetectionThresholdInMB()
            );

            // Initialize drives on bootstrap
            tapeDriveWorkerManager.initializeOnBootstrap();

            tapeDriveWorkerManagers.put(tapeLibraryIdentifier, tapeDriveWorkerManager);
        }

        // Everything's alright. Start tar creation listeners
        writeOrderCreator.startListener();
        fileBucketTarCreatorManager.startListeners();

        // Start tape drive workers
        for (TapeDriveWorkerManager tapeDriveWorkerManager : tapeDriveWorkerManagers.values()) {
            tapeDriveWorkerManager.startWorkers();
        }

        // Initialize monitoring metrics
        ArchiveCacheMetrics.initializeMetrics(archiveCacheStorage);
        AccessRequestMetrics.initializeMetrics(accessRequestReferentialRepository);
        OrderQueueMetrics.initializeMetrics(readWriteQueue);
        TapeCatalogMetrics.initializeMetrics(tapeCatalogRepository);
        for (String tapeLibrary : tapeDriveWorkerManagers.keySet()) {
            DriveWorkerMetrics.initializeMetrics(tapeLibrary, tapeDriveWorkerManagers.get(tapeLibrary));
        }
    }

    public TapeLibraryContentAddressableStorage getTapeLibraryContentAddressableStorage() {
        return tapeLibraryContentAddressableStorage;
    }

    public static TapeLibraryFactory getInstance() {
        return instance;
    }

    public ConcurrentMap<String, TapeLibraryPool> getTapeLibraryPool() {
        return tapeLibraryPool;
    }

    public TapeLibraryPool getFirstTapeLibraryPool() {
        return tapeLibraryPool.values().iterator().next();
    }

    private void createWorkingDirectories(TapeLibraryConfiguration configuration) throws IOException {
        if (
            isBlank(configuration.getInputFileStorageFolder()) ||
            isBlank(configuration.getInputTarStorageFolder()) ||
            isBlank(configuration.getTmpTarOutputStorageFolder()) ||
            isBlank(configuration.getCachedTarStorageFolder())
        ) {
            throw new VitamRuntimeException("Tape storage configuration");
        }
        createDirectory(configuration.getInputFileStorageFolder());
        createDirectory(configuration.getInputTarStorageFolder());
        createDirectory(configuration.getTmpTarOutputStorageFolder());
        createDirectory(configuration.getCachedTarStorageFolder());
    }

    private void createDirectory(String pathStr) throws IOException {
        Path path = Paths.get(pathStr);
        //if directory exists?
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
    }

    public BackupFileStorage getBackupFileStorage() {
        return backupFileStorage;
    }

    public TapeCatalogService getTapeCatalogService() {
        return tapeCatalogService;
    }

    public AccessRequestManager getAccessRequestManager() {
        return accessRequestManager;
    }

    public ArchiveCacheStorage getArchiveCacheStorage() {
        return archiveCacheStorage;
    }

    @VisibleForTesting
    public void overrideTapeServiceCreatorForTesting(TapeServiceCreator tapeServiceCreator) {
        this.tapeServiceCreator = tapeServiceCreator;
    }

    @VisibleForTesting
    public void resetTapeLibraryFactoryAfterTests() {
        this.tapeLibraryPool.clear();
        this.tapeDriveWorkerManagers.clear();
        this.tapeLibraryContentAddressableStorage = null;
        this.backupFileStorage = null;
        this.tapeCatalogService = null;
        this.accessRequestManager = null;
        this.archiveCacheStorage = null;
        this.tapeServiceCreator = defaultTapeServiceCreator;
    }

    public interface TapeServiceCreator {
        TapeRobotService createRobotService(TapeRobotConf tapeRobotConf);

        TapeDriveService createTapeDriveService(TapeLibraryConfiguration configuration, TapeDriveConf tapeDriveConf);
    }

    private static final class TapeServiceCreatorImpl implements TapeServiceCreator {

        @Override
        public TapeRobotService createRobotService(TapeRobotConf tapeRobotConf) {
            return new TapeRobotManager(tapeRobotConf);
        }

        @Override
        public TapeDriveService createTapeDriveService(
            TapeLibraryConfiguration configuration,
            TapeDriveConf tapeDriveConf
        ) {
            return new TapeDriveManager(
                tapeDriveConf,
                configuration.getInputTarStorageFolder(),
                configuration.getTmpTarOutputStorageFolder()
            );
        }
    }
}
