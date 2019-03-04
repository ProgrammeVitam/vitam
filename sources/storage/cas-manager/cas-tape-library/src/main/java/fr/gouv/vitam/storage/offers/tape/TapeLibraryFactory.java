package fr.gouv.vitam.storage.offers.tape;

import fr.gouv.vitam.common.storage.tapelibrary.TapeDriveConf;
import fr.gouv.vitam.common.storage.tapelibrary.TapeLibraryConf;
import fr.gouv.vitam.common.storage.tapelibrary.TapeLibraryConfiguration;
import fr.gouv.vitam.common.storage.tapelibrary.TapeRebotConf;
import fr.gouv.vitam.storage.offers.tape.impl.TapeDriveManager;
import fr.gouv.vitam.storage.offers.tape.impl.TapeRobotManager;
import fr.gouv.vitam.storage.offers.tape.impl.robot.MtxTapeLibraryService;
import fr.gouv.vitam.storage.offers.tape.pool.TapeLibraryPoolImpl;
import fr.gouv.vitam.storage.offers.tape.spec.TapeCatalogService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeDriveService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeLibraryPool;
import fr.gouv.vitam.storage.offers.tape.spec.TapeRobotService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeService;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class TapeLibraryFactory {

    private static final TapeLibraryFactory instance = new TapeLibraryFactory();
    private static final ConcurrentMap<String, TapeLibraryPool> tapeLibraryPool = new ConcurrentHashMap<>();

    private TapeLibraryFactory() {
    }

    public void initize(TapeLibraryConfiguration configuration) {
        Map<String, TapeLibraryConf> libaries = configuration.getTapeLibraries();

        for (String tapeLibraryIdentifier : libaries.keySet()) {
            TapeLibraryConf tapeLibraryConf = libaries.get(tapeLibraryIdentifier);

            BlockingQueue<TapeRobotService> robotServices =
                new ArrayBlockingQueue<>(tapeLibraryConf.getRobots().size(), true);
            ConcurrentHashMap<Integer, TapeDriveService> driveServices = new ConcurrentHashMap<>();



            for (TapeRebotConf tapeRebotConf : tapeLibraryConf.getRobots()) {
                // TODO: 05/03/19 Set TapeCatalogService implementation
                final TapeRobotService robotService = new TapeRobotManager(tapeRebotConf, null);
                robotServices.add(robotService);
            }

            for (TapeDriveConf tapeDriveConf : tapeLibraryConf.getDrives()) {
                final TapeDriveService tapeDriveService = new TapeDriveManager(tapeDriveConf);
                driveServices.put(tapeDriveConf.getIndex(), tapeDriveService);
            }

            if (robotServices.size() > 0 && driveServices.size() > 0) {
                tapeLibraryPool
                    .putIfAbsent(tapeLibraryIdentifier, new TapeLibraryPoolImpl(robotServices, driveServices));
            }
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
}
