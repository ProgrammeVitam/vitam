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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.storage.tapelibrary.TapeDriveConf;
import fr.gouv.vitam.common.storage.tapelibrary.TapeLibraryConf;
import fr.gouv.vitam.common.storage.tapelibrary.TapeLibraryConfiguration;
import fr.gouv.vitam.common.storage.tapelibrary.TapeRebotConf;
import fr.gouv.vitam.storage.offers.tape.dto.TapeLibrarySpec;
import fr.gouv.vitam.storage.offers.tape.exception.TapeCatalogException;
import fr.gouv.vitam.storage.offers.tape.impl.TapeDriveManager;
import fr.gouv.vitam.storage.offers.tape.impl.TapeRobotManager;
import fr.gouv.vitam.storage.offers.tape.impl.catalog.TapeCatalogServiceImpl;
import fr.gouv.vitam.storage.offers.tape.impl.queue.QueueRepositoryImpl;
import fr.gouv.vitam.storage.offers.tape.pool.TapeLibraryPoolImpl;
import fr.gouv.vitam.storage.offers.tape.spec.QueueRepository;
import fr.gouv.vitam.storage.offers.tape.spec.TapeCatalogService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeDriveService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeLibraryPool;
import fr.gouv.vitam.storage.offers.tape.spec.TapeRobotService;
import fr.gouv.vitam.storage.offers.tape.worker.TapeDriveWorkerManager;
import fr.gouv.vitam.storage.engine.common.collection.OfferCollections;
import fr.gouv.vitam.storage.engine.common.collection.OfferCollections;
import fr.gouv.vitam.storage.engine.common.model.TapeCatalog;

public class TapeLibraryFactory {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(TapeLibraryFactory.class);
    private static final TapeLibraryFactory instance = new TapeLibraryFactory();
    private static final ConcurrentMap<String, TapeLibraryPool> tapeLibraryPool = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, TapeDriveWorkerManager> tapeDriveWorkerManagers = new ConcurrentHashMap<>();

    private TapeLibraryFactory() {
    }

    public void initialize(TapeLibraryConfiguration configuration, MongoDbAccess mongoDbAccess) {
        Map<String, TapeLibraryConf> libraries = configuration.getTapeLibraries();

        final TapeCatalogService tapeCatalogService = new TapeCatalogServiceImpl(mongoDbAccess);
        final QueueRepository queueRepository = new QueueRepositoryImpl(mongoDbAccess.getMongoDatabase().getCollection(
            OfferCollections.OFFER_QUEUE.getName()));

        for (String tapeLibraryIdentifier : libraries.keySet()) {
            TapeLibraryConf tapeLibraryConf = libraries.get(tapeLibraryIdentifier);

            BlockingQueue<TapeRobotService> robotServices =
                new ArrayBlockingQueue<>(tapeLibraryConf.getRobots().size(), true);
            ConcurrentHashMap<Integer, TapeDriveService> driveServices = new ConcurrentHashMap<>();



            for (TapeRebotConf tapeRebotConf : tapeLibraryConf.getRobots()) {
                final TapeRobotService robotService = new TapeRobotManager(tapeRebotConf);
                robotServices.add(robotService);
            }

            for (TapeDriveConf tapeDriveConf : tapeLibraryConf.getDrives()) {
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

            tapeDriveWorkerManagers
                .put(tapeLibraryIdentifier, new TapeDriveWorkerManager(queueRepository, libraryPool, driveTape));
        }

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

    public ConcurrentMap<String, TapeDriveWorkerManager> getTapeDriveWorkerManagers() {
        return tapeDriveWorkerManagers;
    }

    public QueueRepository getReadWriteQueue() {
        if (tapeDriveWorkerManagers.isEmpty()) {
            throw new IllegalStateException("No QueueRepository initialized");
        }

        return tapeDriveWorkerManagers.values().iterator().next().getQueue();
    }
}
