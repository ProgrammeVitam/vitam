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
package fr.gouv.vitam.storage.offers.tape;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.storage.tapelibrary.TapeDriveConf;
import fr.gouv.vitam.common.storage.tapelibrary.TapeLibraryConf;
import fr.gouv.vitam.common.storage.tapelibrary.TapeLibraryConfiguration;
import fr.gouv.vitam.common.storage.tapelibrary.TapeRobotConf;
import fr.gouv.vitam.common.thread.VitamThreadFactory;
import fr.gouv.vitam.storage.engine.common.collection.OfferCollections;
import fr.gouv.vitam.storage.engine.common.model.TapeCatalog;
import fr.gouv.vitam.storage.offers.tape.cas.BackupFileStorage;
import fr.gouv.vitam.storage.offers.tape.cas.BasicFileStorage;
import fr.gouv.vitam.storage.offers.tape.cas.BucketTopologyHelper;
import fr.gouv.vitam.storage.offers.tape.cas.FileBucketTarCreatorManager;
import fr.gouv.vitam.storage.offers.tape.cas.ObjectReferentialRepository;
import fr.gouv.vitam.storage.offers.tape.cas.ReadRequestReferentialRepository;
import fr.gouv.vitam.storage.offers.tape.cas.TapeLibraryContentAddressableStorage;
import fr.gouv.vitam.storage.offers.tape.cas.TarFileRapairer;
import fr.gouv.vitam.storage.offers.tape.cas.ArchiveReferentialRepository;
import fr.gouv.vitam.storage.offers.tape.cas.TarsOutputRetention;
import fr.gouv.vitam.storage.offers.tape.cas.WriteOrderCreator;
import fr.gouv.vitam.storage.offers.tape.cas.WriteOrderCreatorBootstrapRecovery;
import fr.gouv.vitam.storage.offers.tape.dto.TapeLibrarySpec;
import fr.gouv.vitam.storage.offers.tape.dto.TapeResponse;
import fr.gouv.vitam.storage.offers.tape.exception.TapeCatalogException;
import fr.gouv.vitam.storage.offers.tape.impl.TapeDriveManager;
import fr.gouv.vitam.storage.offers.tape.impl.TapeRobotManager;
import fr.gouv.vitam.storage.offers.tape.impl.catalog.TapeCatalogRepository;
import fr.gouv.vitam.storage.offers.tape.impl.catalog.TapeCatalogServiceImpl;
import fr.gouv.vitam.storage.offers.tape.impl.queue.QueueRepositoryImpl;
import fr.gouv.vitam.storage.offers.tape.impl.readwrite.TapeLibraryServiceImpl;
import fr.gouv.vitam.storage.offers.tape.pool.TapeLibraryPoolImpl;
import fr.gouv.vitam.storage.offers.tape.spec.QueueRepository;
import fr.gouv.vitam.storage.offers.tape.spec.TapeCatalogService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeDriveService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeLibraryPool;
import fr.gouv.vitam.storage.offers.tape.spec.TapeRobotService;
import fr.gouv.vitam.storage.offers.tape.worker.TapeDriveWorkerManager;
import org.apache.logging.log4j.util.Strings;

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

public class TapeLibraryFactory {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(TapeLibraryFactory.class);

    private static final TapeLibraryFactory instance = new TapeLibraryFactory();
    private static final ConcurrentMap<String, TapeLibraryPool> tapeLibraryPool = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, TapeDriveWorkerManager> tapeDriveWorkerManagers = new ConcurrentHashMap<>();
    private TapeLibraryContentAddressableStorage tapeLibraryContentAddressableStorage;
    private BackupFileStorage backupFileStorage;
    private TapeCatalogService tapeCatalogService;

    private TapeLibraryFactory() {
    }

    public void initialize(TapeLibraryConfiguration configuration, MongoDbAccess mongoDbAccess) throws IOException {

        ParametersChecker.checkParameter("All params are required", configuration, mongoDbAccess);
        createWorkingDirectories(configuration);

        Map<String, TapeLibraryConf> libraries = configuration.getTapeLibraries();

        TapeCatalogRepository tapeCatalogRepository = new TapeCatalogRepository(mongoDbAccess.getMongoDatabase()
            .getCollection(OfferCollections.TAPE_CATALOG.getName()));

        tapeCatalogService = new TapeCatalogServiceImpl(tapeCatalogRepository);

        BucketTopologyHelper bucketTopologyHelper = new BucketTopologyHelper(configuration.getTopology());

        ObjectReferentialRepository objectReferentialRepository =
            new ObjectReferentialRepository(mongoDbAccess.getMongoDatabase()
                .getCollection(OfferCollections.TAPE_OBJECT_REFERENTIAL.getName()));
        ArchiveReferentialRepository archiveReferentialRepository =
            new ArchiveReferentialRepository(mongoDbAccess.getMongoDatabase()
                .getCollection(OfferCollections.TAPE_ARCHIVE_REFERENTIAL.getName()));
        ReadRequestReferentialRepository readRequestReferentialRepository =
                new ReadRequestReferentialRepository(mongoDbAccess.getMongoDatabase()
                        .getCollection(OfferCollections.TAPE_READ_REQUEST_REFERENTIAL.getName()));
        QueueRepository readWriteQueue = new QueueRepositoryImpl(mongoDbAccess.getMongoDatabase().getCollection(
            OfferCollections.TAPE_QUEUE_MESSAGE.getName()));

        WriteOrderCreator writeOrderCreator = new WriteOrderCreator(
            archiveReferentialRepository, readWriteQueue);

        TarFileRapairer tarFileRapairer = new TarFileRapairer(objectReferentialRepository);
        WriteOrderCreatorBootstrapRecovery
            writeOrderCreatorBootstrapRecovery = new WriteOrderCreatorBootstrapRecovery(
            configuration.getInputTarStorageFolder(), archiveReferentialRepository,
            bucketTopologyHelper, writeOrderCreator, tarFileRapairer);


        backupFileStorage =
            new BackupFileStorage(archiveReferentialRepository, writeOrderCreator, BucketTopologyHelper.BACKUP_BUCKET,
                BucketTopologyHelper.BACKUP_FILE_BUCKET,
                configuration.getInputTarStorageFolder());


        BasicFileStorage basicFileStorage =
            new BasicFileStorage(configuration.getInputFileStorageFolder());
        FileBucketTarCreatorManager fileBucketTarCreatorManager =
            new FileBucketTarCreatorManager(configuration, basicFileStorage, bucketTopologyHelper,
                objectReferentialRepository, archiveReferentialRepository, writeOrderCreator);

        tapeLibraryContentAddressableStorage =
            new TapeLibraryContentAddressableStorage(basicFileStorage, objectReferentialRepository,
                archiveReferentialRepository, readRequestReferentialRepository, fileBucketTarCreatorManager, readWriteQueue,
                tapeCatalogService, configuration.getOutputTarStorageFolder());

        // Change all running orders to ready state
        readWriteQueue.initializeOnBootstrap();

        // Create tar creation orders from inputFiles folder
        writeOrderCreatorBootstrapRecovery.initializeOnBootstrap();

        // Create tar storage orders from inputTars folder
        fileBucketTarCreatorManager.initializeOnBootstrap();

        // Initialize & start workers
        for (String tapeLibraryIdentifier : libraries.keySet()) {
            TapeLibraryConf tapeLibraryConf = libraries.get(tapeLibraryIdentifier);

            BlockingQueue<TapeRobotService> robotServices =
                new ArrayBlockingQueue<>(tapeLibraryConf.getRobots().size(), true);
            ConcurrentHashMap<Integer, TapeDriveService> driveServices = new ConcurrentHashMap<>();

            for (TapeRobotConf tapeRobotConf : tapeLibraryConf.getRobots()) {
                tapeRobotConf.setUseSudo(configuration.isUseSudo());
                final TapeRobotService robotService = new TapeRobotManager(tapeRobotConf);
                robotServices.add(robotService);
            }

            for (TapeDriveConf tapeDriveConf : tapeLibraryConf.getDrives()) {
                tapeDriveConf.setUseSudo(configuration.isUseSudo());

                final TapeDriveService tapeDriveService = new TapeDriveManager(tapeDriveConf,
                    configuration.getInputTarStorageFolder(), configuration.getOutputTarStorageFolder());
                driveServices.put(tapeDriveConf.getIndex(), tapeDriveService);
            }

            if (robotServices.size() > 0 && driveServices.size() > 0) {
                tapeLibraryPool
                    .putIfAbsent(tapeLibraryIdentifier,
                        new TapeLibraryPoolImpl(tapeLibraryIdentifier, robotServices, driveServices,
                            tapeCatalogService));
            }

            // init tape catalog
            TapeLibraryPool libraryPool = tapeLibraryPool.get(tapeLibraryIdentifier);
            Map<Integer, TapeCatalog> driveTape = new HashMap<>();
            try {
                TapeRobotService robot = libraryPool.checkoutRobotService();
                if (robot != null) {
                    try {
                        TapeLibrarySpec libraryState = robot.getLoadUnloadService().status();

                        if (!libraryState.isOK()) {
                            throw new RuntimeException("Robot status command return ko :" +
                                JsonHandler.unprettyPrint(libraryState.getEntity()));
                        }

                        driveTape = tapeCatalogService.init(tapeLibraryIdentifier, libraryState);
                    } finally {
                        libraryPool.pushRobotService(robot);
                    }

                }
            } catch (InterruptedException | TapeCatalogException e) {
                LOGGER.error(e);
                throw new RuntimeException(e);
            }

            // force rewind
            forceRewindOnBootstrap(driveServices, driveTape);

            // Start all workers
            tapeDriveWorkerManagers
                .put(tapeLibraryIdentifier,
                    new TapeDriveWorkerManager(readWriteQueue, archiveReferentialRepository, readRequestReferentialRepository, libraryPool, driveTape,
                        configuration.getInputTarStorageFolder(), configuration.isForceOverrideNonEmptyCartridges()));
        }

        // Everything's alright. Start tar creation listeners
        writeOrderCreator.startListener();
        fileBucketTarCreatorManager.startListeners();

        // Start tape drive workers
        for (TapeDriveWorkerManager tapeDriveWorkerManager : tapeDriveWorkerManagers.values()) {
            tapeDriveWorkerManager.startWorkers();
        }

        // launch thread TarsOuptutRetention
        VitamThreadFactory.getInstance().newThread(new TarsOutputRetention(configuration.getOutputTarStorageFolder())).start();
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

        if (Strings.isBlank(configuration.getInputFileStorageFolder()) ||
            Strings.isBlank(configuration.getInputTarStorageFolder()) ||
            Strings.isBlank(configuration.getOutputTarStorageFolder())) {
            throw new VitamRuntimeException("Tape storage configuration");
        }
        createDirectory(configuration.getInputFileStorageFolder());
        createDirectory(configuration.getInputTarStorageFolder());
        createDirectory(configuration.getOutputTarStorageFolder());
    }

    private void createDirectory(String pathStr) throws IOException {
        Path path = Paths.get(pathStr);
        //if directory exists?
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
    }

    private void forceRewindOnBootstrap(ConcurrentHashMap<Integer, TapeDriveService> driveServices,
        Map<Integer, TapeCatalog> driveTape) {
        driveTape.keySet().forEach(driveIndex -> {
            TapeResponse rewindResponse =
                driveServices.get(driveIndex).getDriveCommandService().rewind();

            if (!rewindResponse.isOK()) {
                throw new RuntimeException("Cannot rewind tape " + JsonHandler.unprettyPrint(rewindResponse));
            }
        });
    }

    public BackupFileStorage getBackupFileStorage() {
        return backupFileStorage;
    }

    public TapeCatalogService getTapeCatalogService() {
        return tapeCatalogService;
    }
}
