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
package fr.gouv.vitam.storage.offers.tape.impl;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Uninterruptibles;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.database.server.query.QueryCriteria;
import fr.gouv.vitam.common.database.server.query.QueryCriteriaOperator;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.storage.tapelibrary.TapeDriveConf;
import fr.gouv.vitam.common.storage.tapelibrary.TapeLibraryConfiguration;
import fr.gouv.vitam.common.tmp.TempFolderRule;
import fr.gouv.vitam.storage.engine.common.collection.OfferCollections;
import fr.gouv.vitam.storage.engine.common.model.QueueState;
import fr.gouv.vitam.storage.engine.common.model.ReadOrder;
import fr.gouv.vitam.storage.engine.common.model.TapeCatalog;
import fr.gouv.vitam.storage.offers.tape.TapeLibraryFactory;
import fr.gouv.vitam.storage.offers.tape.cas.AccessRequestManager;
import fr.gouv.vitam.storage.offers.tape.cas.ArchiveCacheStorage;
import fr.gouv.vitam.storage.offers.tape.dto.TapeDrive;
import fr.gouv.vitam.storage.offers.tape.dto.TapeDriveSpec;
import fr.gouv.vitam.storage.offers.tape.dto.TapeLibrarySpec;
import fr.gouv.vitam.storage.offers.tape.dto.TapeSlot;
import fr.gouv.vitam.storage.offers.tape.exception.TapeCommandException;
import fr.gouv.vitam.storage.offers.tape.impl.catalog.TapeCatalogRepository;
import fr.gouv.vitam.storage.offers.tape.impl.catalog.TapeCatalogServiceImpl;
import fr.gouv.vitam.storage.offers.tape.impl.readwrite.TapeLibraryServiceImpl;
import fr.gouv.vitam.storage.offers.tape.process.Output;
import fr.gouv.vitam.storage.offers.tape.process.ProcessExecutor;
import fr.gouv.vitam.storage.offers.tape.spec.TapeCatalogService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeDriveCommandService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeDriveService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeLibraryPool;
import fr.gouv.vitam.storage.offers.tape.spec.TapeLoadUnloadService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeReadWriteService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeRobotService;
import fr.gouv.vitam.storage.offers.tape.worker.tasks.ReadTask;
import fr.gouv.vitam.storage.offers.tape.worker.tasks.ReadWriteResult;
import org.apache.commons.lang3.time.StopWatch;
import org.assertj.core.api.Assertions;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

/**
 * To be able to run test :
 * - Setup local scsi tape devices (mapped to VTL or a physical tape library) with at least 10 slots & 4 drives
 * - Configure offer-tape-test.conf with robot & drive devices (use ls -l  /dev/tape/by-id/ to list devices)
 * - Ensure user belongs to "tape" group to be able to invoke mt & mtx commands
 * - Remove @Ignore annotation locally
 * - Hint, to set up locally a VTL, the vitam/vtl-utils repository can be used.
 * /!\ WARNING : This test will erase all tapes
 */
@Ignore("Will not work if no tape library connected with a good configuration file")
public class TapeLibraryIT {

    private static final int FULL_CARTRIDGE_THRESHOLD = 2_000_000;
    public static final String OFFER_TAPE_TEST_CONF = "offer-tape-test.conf";
    public static final long TIMEOUT_IN_MILLISECONDS = 60000L;
    public static final Integer SLOT_INDEX = 3;
    public static final Integer DRIVE_INDEX = 2;

    @ClassRule
    public static TempFolderRule tempFolderRule = new TempFolderRule();

    @ClassRule
    public static MongoRule mongoRule = new MongoRule(
        MongoDbAccess.getMongoClientSettingsBuilder(),
        OfferCollections.TAPE_CATALOG.getName()
    );

    private static TapeLibraryFactory tapeLibraryFactory;
    private static TapeLibraryConfiguration configuration;
    private static AccessRequestManager accessRequestManager;
    private static ArchiveCacheStorage archiveCacheStorage;

    @BeforeClass
    public static void beforeClass() throws Exception {
        configuration = PropertiesUtils.readYaml(
            PropertiesUtils.findFile(OFFER_TAPE_TEST_CONF),
            TapeLibraryConfiguration.class
        );

        VitamConfiguration.setTenants(Lists.newArrayList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
        String inputFileStorageFolder = PropertiesUtils.getResourceFile("tar/").getAbsolutePath();
        configuration.setInputFileStorageFolder(inputFileStorageFolder);
        configuration.setInputTarStorageFolder(inputFileStorageFolder);
        configuration.setTmpTarOutputStorageFolder(tempFolderRule.newFolder().getAbsolutePath());
        configuration.setCachedTarStorageFolder(tempFolderRule.newFolder().getAbsolutePath());
        tapeLibraryFactory = TapeLibraryFactory.getInstance();
        tapeLibraryFactory.initialize(configuration, mongoRule.getMongoDatabase());
        accessRequestManager = tapeLibraryFactory.getAccessRequestManager();
        archiveCacheStorage = tapeLibraryFactory.getArchiveCacheStorage();
        eraseAllTapes();
    }

    private static void eraseAllTapes() throws Exception {
        TapeLibraryPool tapeLibraryPool = tapeLibraryFactory.getFirstTapeLibraryPool();
        TapeRobotService tapeRobotService = null;
        try {
            tapeRobotService = tapeLibraryPool.checkoutRobotService();
            TapeLoadUnloadService loadUnloadService = tapeRobotService.getLoadUnloadService();

            TapeLibrarySpec status = loadUnloadService.status();
            assertThat(status).isNotNull();

            // Erase & unload tapes online in drives
            for (TapeDrive tapeDrive : status.getDrives()) {
                if (tapeDrive.getTape() != null) {
                    TapeDriveService tapeDriveService = null;
                    try {
                        tapeDriveService = tapeLibraryPool.checkoutDriveService(tapeDrive.getIndex());

                        TapeDriveCommandService driveCommandService = tapeDriveService.getDriveCommandService();
                        TapeDriveConf tapeDriveConf = tapeDriveService.getTapeDriveConf();

                        eraseTape(driveCommandService, tapeDriveConf);

                        assertThatCode(
                            () -> loadUnloadService.unloadTape(tapeDrive.getTape().getSlotIndex(), tapeDrive.getIndex())
                        ).doesNotThrowAnyException();
                    } finally {
                        if (tapeDriveService != null) {
                            tapeLibraryPool.pushDriveService(tapeDriveService);
                        }
                    }
                }
            }

            // Erase other tapes
            TapeDrive firstTapeDrive = status.getDrives().get(0);

            TapeDriveService tapeDriveService = null;
            try {
                tapeDriveService = tapeLibraryPool.checkoutDriveService(firstTapeDrive.getIndex());

                TapeDriveCommandService driveCommandService = tapeDriveService.getDriveCommandService();
                TapeDriveConf tapeDriveConf = tapeDriveService.getTapeDriveConf();

                for (TapeSlot slot : status.getSlots()) {
                    if (slot.getTape() != null) {
                        assertThatCode(
                            () -> loadUnloadService.loadTape(slot.getIndex(), firstTapeDrive.getIndex())
                        ).doesNotThrowAnyException();

                        eraseTape(driveCommandService, tapeDriveConf);

                        assertThatCode(
                            () -> loadUnloadService.unloadTape(slot.getIndex(), firstTapeDrive.getIndex())
                        ).doesNotThrowAnyException();
                    }
                }
            } finally {
                if (tapeDriveService != null) {
                    tapeLibraryPool.pushDriveService(tapeDriveService);
                }
            }
        } finally {
            if (tapeRobotService != null) {
                tapeLibraryPool.pushRobotService(tapeRobotService);
            }
        }
    }

    private static void eraseTape(TapeDriveCommandService driveCommandService, TapeDriveConf tapeDriveConf) {
        assertThatCode(driveCommandService::rewind).doesNotThrowAnyException();

        List<String> args = Lists.newArrayList("-f", tapeDriveConf.getDevice(), "erase");
        Output output = ProcessExecutor.getInstance()
            .execute(tapeDriveConf.getMtPath(), tapeDriveConf.getTimeoutInMilliseconds(), args);
        assertThat(output.getExitCode()).isEqualTo(0);
    }

    @Test
    public void testInit() {
        // Just runs eraseAllTapes()
    }

    @AfterClass
    public static void setDownAfterClass() {
        mongoRule.handleAfterClass();
    }

    @Test
    public void statusInitialization() {
        Assertions.assertThat(tapeLibraryFactory.getTapeLibraryPool()).hasSize(1);
    }

    @Test
    public void statusRobotStatus() throws InterruptedException, TapeCommandException {
        TapeLibraryPool tapeLibraryPool = tapeLibraryFactory.getFirstTapeLibraryPool();

        TapeRobotService tapeRobotService = tapeLibraryPool.checkoutRobotService(5, TimeUnit.MILLISECONDS);
        assertThat(tapeRobotService).isNotNull();

        try {
            TapeLibrarySpec state = tapeRobotService.getLoadUnloadService().status();
            assertThat(state).isNotNull();

            // As only one robot exists, then should return null
            TapeRobotService tapeRobotService1 = tapeLibraryPool.checkoutRobotService(1, TimeUnit.MILLISECONDS);
            assertThat(tapeRobotService1).isNull();
        } finally {
            tapeLibraryPool.pushRobotService(tapeRobotService);
        }

        TapeRobotService tapeRobotService2 = tapeLibraryPool.checkoutRobotService(5, TimeUnit.MILLISECONDS);
        try {
            assertThat(tapeRobotService2).isNotNull();
        } finally {
            tapeLibraryPool.pushRobotService(tapeRobotService2);
        }

        TapeDriveService tapeDriveService1 = tapeLibraryPool.checkoutDriveService(0);

        try {
            TapeDriveSpec driveState1 = tapeDriveService1.getDriveCommandService().status();
            assertThat(driveState1).isNotNull();
        } finally {
            tapeLibraryPool.pushDriveService(tapeDriveService1);
        }

        TapeDriveService tapeDriveService2 = tapeLibraryPool.checkoutDriveService(1);

        try {
            TapeDriveSpec driveState2 = tapeDriveService2.getDriveCommandService().status();
            assertThat(driveState2).isNotNull();
        } finally {
            tapeLibraryPool.pushDriveService(tapeDriveService2);
        }

        TapeDriveService tapeDriveService3 = tapeLibraryPool.checkoutDriveService(2);
        try {
            TapeDriveSpec driveState3 = tapeDriveService3.getDriveCommandService().status();
            assertThat(driveState3).isNotNull();
        } finally {
            tapeLibraryPool.pushDriveService(tapeDriveService3);
        }

        TapeDriveService tapeDriveService4 = tapeLibraryPool.checkoutDriveService(3);
        try {
            TapeDriveSpec driveState4 = tapeDriveService4.getDriveCommandService().status();
            assertThat(driveState4).isNotNull();
        } finally {
            tapeLibraryPool.pushDriveService(tapeDriveService4);
        }
    }

    @Test
    public void testLoadUnloadTape() throws InterruptedException, TapeCommandException {
        TapeLibraryPool tapeLibraryPool = tapeLibraryFactory.getFirstTapeLibraryPool();

        TapeRobotService tapeRobotService = tapeLibraryPool.checkoutRobotService(
            TIMEOUT_IN_MILLISECONDS,
            TimeUnit.MILLISECONDS
        );
        assertThat(tapeRobotService).isNotNull();

        try {
            TapeLibrarySpec state = tapeRobotService.getLoadUnloadService().status();
            assertThat(state).isNotNull();

            assertThatCode(
                () -> tapeRobotService.getLoadUnloadService().loadTape(SLOT_INDEX, DRIVE_INDEX)
            ).doesNotThrowAnyException();

            // Load already loaded tape KO
            assertThatThrownBy(
                () -> tapeRobotService.getLoadUnloadService().loadTape(SLOT_INDEX, DRIVE_INDEX)
            ).isInstanceOf(TapeCommandException.class);

            // unload tape OK
            assertThatCode(
                () -> tapeRobotService.getLoadUnloadService().unloadTape(SLOT_INDEX, DRIVE_INDEX)
            ).doesNotThrowAnyException();

            // unload already loaded tape KO
            assertThatThrownBy(
                () -> tapeRobotService.getLoadUnloadService().unloadTape(SLOT_INDEX, DRIVE_INDEX)
            ).isInstanceOf(TapeCommandException.class);
        } finally {
            tapeLibraryPool.pushRobotService(tapeRobotService);
        }
    }

    @Test
    public void test_load_rewind_write_read_rewind_then_unload_tape() throws Exception {
        TapeLibraryPool tapeLibraryPool = tapeLibraryFactory.getFirstTapeLibraryPool();

        TapeRobotService tapeRobotService = tapeLibraryPool.checkoutRobotService(
            TIMEOUT_IN_MILLISECONDS,
            TimeUnit.MILLISECONDS
        );
        assertThat(tapeRobotService).isNotNull();

        TapeDriveService tapeDriveService = tapeLibraryPool.checkoutDriveService(DRIVE_INDEX);
        assertThat(tapeDriveService).isNotNull();

        try {
            TapeLibrarySpec state = tapeRobotService.getLoadUnloadService().status();
            assertThat(state).isNotNull();

            // 1 Load Tape
            assertThatCode(
                () -> tapeRobotService.getLoadUnloadService().loadTape(SLOT_INDEX, DRIVE_INDEX)
            ).doesNotThrowAnyException();

            TapeReadWriteService ddReadWriteService = tapeDriveService.getReadWriteService();

            TapeDriveCommandService driveCommandService = tapeDriveService.getDriveCommandService();

            // 2 erase tape
            assertThatCode(driveCommandService::rewind).doesNotThrowAnyException();

            //3 Write file to tape
            assertThatCode(() -> ddReadWriteService.writeToTape("testtar.tar")).doesNotThrowAnyException();

            //4 Rewind
            assertThatCode(driveCommandService::rewind).doesNotThrowAnyException();

            // 5 Read file from tape with dd command
            assertThatCode(() -> ddReadWriteService.readFromTape("testtar.tar")).doesNotThrowAnyException();
            // TODO: 25/02/19 Check parse response

            //6 Rewind
            assertThatCode(driveCommandService::rewind).doesNotThrowAnyException();

            //8 Rewind
            assertThatCode(driveCommandService::rewind).doesNotThrowAnyException();
        } finally {
            try {
                tapeRobotService.getLoadUnloadService().unloadTape(SLOT_INDEX, DRIVE_INDEX);
            } finally {
                tapeLibraryPool.pushDriveService(tapeDriveService);
                tapeLibraryPool.pushRobotService(tapeRobotService);
            }
        }
    }

    @Test
    public void test_read_files_from_tape() throws Exception {
        TapeLibraryPool tapeLibraryPool = tapeLibraryFactory.getFirstTapeLibraryPool();

        TapeRobotService tapeRobotService = tapeLibraryPool.checkoutRobotService(10, TimeUnit.MILLISECONDS);
        assertThat(tapeRobotService).isNotNull();

        TapeDriveService tapeDriveService = tapeLibraryPool.checkoutDriveService(DRIVE_INDEX);
        assertThat(tapeDriveService).isNotNull();

        try {
            TapeLibrarySpec state = tapeRobotService.getLoadUnloadService().status();
            assertThat(state).isNotNull();

            // 1 Load Tape
            assertThatCode(
                () -> tapeRobotService.getLoadUnloadService().loadTape(SLOT_INDEX, DRIVE_INDEX)
            ).doesNotThrowAnyException();

            TapeReadWriteService ddReadWriteService = tapeDriveService.getReadWriteService();

            TapeDriveCommandService driveCommandService = tapeDriveService.getDriveCommandService();

            // 2 erase tape
            assertThatCode(driveCommandService::rewind).doesNotThrowAnyException();

            //3 Write files to tape
            // file 1
            assertThatCode(() -> ddReadWriteService.writeToTape("testtar.tar")).doesNotThrowAnyException();

            // file 2
            assertThatCode(() -> ddReadWriteService.writeToTape("testtar_2.tar")).doesNotThrowAnyException();

            // read file from tape with given position
            TapeCatalogRepository tapeCatalogRepository = new TapeCatalogRepository(
                mongoRule.getMongoDatabase().getCollection(OfferCollections.TAPE_CATALOG.getName())
            );

            TapeCatalogService tapeCatalogService = new TapeCatalogServiceImpl(tapeCatalogRepository);
            String tapeCode = state.getSlots().get(SLOT_INDEX - 1).getTape().getVolumeTag();
            TapeCatalog workerCurrentTape = tapeCatalogService
                .find(List.of(new QueryCriteria(TapeCatalog.CODE, tapeCode, QueryCriteriaOperator.EQ)))
                .get(0);

            ReadTask readTask1 = new ReadTask(
                new ReadOrder(tapeCode, 0, "testtar.tar", "bucket", "test-objects", 10_240L),
                workerCurrentTape,
                new TapeLibraryServiceImpl(tapeDriveService, tapeLibraryPool, FULL_CARTRIDGE_THRESHOLD),
                tapeCatalogService,
                accessRequestManager,
                archiveCacheStorage
            );

            ReadTask readTask2 = new ReadTask(
                new ReadOrder(tapeCode, 1, "testtar_2.tar", "bucket", "test-objects", 6_144L),
                workerCurrentTape,
                new TapeLibraryServiceImpl(tapeDriveService, tapeLibraryPool, FULL_CARTRIDGE_THRESHOLD),
                tapeCatalogService,
                accessRequestManager,
                archiveCacheStorage
            );

            // Classical read
            ReadWriteResult result1 = readTask1.get();
            assertThat(result1).isNotNull();
            assertThat(result1.getOrderState()).isEqualTo(QueueState.COMPLETED);

            String outputFile = configuration.getCachedTarStorageFolder() + "/test-objects/testtar.tar";
            File outputTarFile = new File(outputFile);
            Assertions.assertThat(outputTarFile).exists();
            Assertions.assertThat(outputTarFile.length()).isEqualTo(10_240L);

            assertThat(
                archiveCacheStorage.tryReadArchive("test-objects", "testtar.tar").orElseThrow()
            ).hasSameContentAs(PropertiesUtils.getResourceAsStream("tar/testtar.tar"));

            ReadWriteResult result2 = readTask2.get();
            assertThat(result2).isNotNull();
            assertThat(result2.getOrderState()).isEqualTo(QueueState.COMPLETED);

            outputFile = configuration.getCachedTarStorageFolder() + "/test-objects/testtar_2.tar";
            outputTarFile = new File(outputFile);
            Assertions.assertThat(outputTarFile).exists();
            Assertions.assertThat(outputTarFile.length()).isEqualTo(6_144L);

            assertThat(
                archiveCacheStorage.tryReadArchive("test-objects", "testtar_2.tar").orElseThrow()
            ).hasSameContentAs(PropertiesUtils.getResourceAsStream("tar/testtar_2.tar"));

            // Make entries expire
            archiveCacheStorage.reserveArchiveStorageSpace("test-objects", "testtar_3.tar", 999_999);
            archiveCacheStorage.reserveArchiveStorageSpace("test-objects", "testtar_4.tar", 999_999);

            StopWatch stopWatch = StopWatch.createStarted();
            while (
                stopWatch.getTime(TimeUnit.SECONDS) < 5 &&
                archiveCacheStorage.containsArchive("test-objects", "testtar_2.tar")
            ) {
                Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
            }
            assertThat(archiveCacheStorage.containsArchive("test-objects", "testtar.tar")).isFalse();
            assertThat(archiveCacheStorage.containsArchive("test-objects", "testtar_2.tar")).isFalse();

            archiveCacheStorage.cancelReservedArchive("test-objects", "testtar_3.tar");
            archiveCacheStorage.cancelReservedArchive("test-objects", "testtar_4.tar");

            // Test of move backward (bsfm) : We are in position 2 try to re-read second file
            result2 = readTask2.get();
            assertThat(result2).isNotNull();
            assertThat(result2.getOrderState()).isEqualTo(QueueState.COMPLETED);

            outputFile = configuration.getCachedTarStorageFolder() + "/test-objects/testtar_2.tar";
            outputTarFile = new File(outputFile);
            Assertions.assertThat(outputTarFile).exists();
            Assertions.assertThat(outputTarFile.length()).isEqualTo(6_144L);

            assertThat(
                archiveCacheStorage.tryReadArchive("test-objects", "testtar_2.tar").orElseThrow()
            ).hasSameContentAs(PropertiesUtils.getResourceAsStream("tar/testtar_2.tar"));

            // Test of move backward rewind : We are in position 2 try to re-read first file
            result1 = readTask1.get();
            assertThat(result1).isNotNull();
            assertThat(result1.getOrderState()).isEqualTo(QueueState.COMPLETED);

            outputFile = configuration.getCachedTarStorageFolder() + "/test-objects/testtar.tar";
            outputTarFile = new File(outputFile);
            Assertions.assertThat(outputTarFile).exists();
            Assertions.assertThat(outputTarFile.length()).isEqualTo(10_240L);

            assertThat(
                archiveCacheStorage.tryReadArchive("test-objects", "testtar.tar").orElseThrow()
            ).hasSameContentAs(PropertiesUtils.getResourceAsStream("tar/testtar.tar"));
        } finally {
            try {
                tapeRobotService.getLoadUnloadService().unloadTape(SLOT_INDEX, DRIVE_INDEX);
            } finally {
                tapeLibraryPool.pushDriveService(tapeDriveService);
                tapeLibraryPool.pushRobotService(tapeRobotService);
            }
        }
    }
}
