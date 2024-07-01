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
package fr.gouv.vitam.storage.offers.tape.worker;

import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.storage.tapelibrary.ReadWritePriority;
import fr.gouv.vitam.common.storage.tapelibrary.TapeDriveConf;
import fr.gouv.vitam.storage.engine.common.model.TapeCatalog;
import fr.gouv.vitam.storage.engine.common.model.TapeCatalogLabel;
import fr.gouv.vitam.storage.engine.common.model.TapeLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeLocationType;
import fr.gouv.vitam.storage.engine.common.model.TapeState;
import fr.gouv.vitam.storage.offers.tape.cas.AccessRequestManager;
import fr.gouv.vitam.storage.offers.tape.cas.ArchiveCacheStorage;
import fr.gouv.vitam.storage.offers.tape.cas.ArchiveReferentialRepository;
import fr.gouv.vitam.storage.offers.tape.dto.TapeDriveSpec;
import fr.gouv.vitam.storage.offers.tape.dto.TapeDriveState;
import fr.gouv.vitam.storage.offers.tape.dto.TapeDriveStatus;
import fr.gouv.vitam.storage.offers.tape.exception.QueueException;
import fr.gouv.vitam.storage.offers.tape.exception.ReadWriteException;
import fr.gouv.vitam.storage.offers.tape.exception.TapeCommandException;
import fr.gouv.vitam.storage.offers.tape.spec.TapeCatalogService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeDriveCommandService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeDriveService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeReadWriteService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeRobotPool;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.internal.verification.VerificationModeFactory;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class TapeDriveWorkerTest {

    private static final int FULL_CARTRIDGE_THRESHOLD = 2_000_000;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private TapeRobotPool tapeRobotPool;

    @Mock
    private TapeDriveService tapeDriveService;

    @Mock
    private ArchiveReferentialRepository archiveReferentialRepository;

    @Mock
    private AccessRequestManager accessRequestManager;

    @Mock
    private TapeCatalogService tapeCatalogService;

    @Mock
    private ArchiveCacheStorage archiveCacheStorage;

    @Spy
    private TapeDriveOrderConsumer tapeDriveOrderConsumer;

    @Mock
    private TapeDriveConf tapeDriveConf;

    @Mock
    private TapeDriveCommandService tapeDriveCommandService;

    @Mock
    private TapeReadWriteService tapeReadWriteService;

    private File inputTarDir;
    private File tmpTarOutputDir;

    @Before
    public void setUp() throws Exception {
        when(tapeDriveService.getTapeDriveConf()).thenReturn(tapeDriveConf);
        when(tapeDriveService.getDriveCommandService()).thenReturn(tapeDriveCommandService);
        when(tapeDriveService.getReadWriteService()).thenReturn(tapeReadWriteService);

        inputTarDir = temporaryFolder.newFolder("inputTars");

        tmpTarOutputDir = temporaryFolder.newFolder("tmpTarOutput");
        when(tapeReadWriteService.getTmpOutputStorageFolder()).thenReturn(tmpTarOutputDir.getAbsolutePath());
    }

    @After
    public void tearDown() {}

    @Test
    public void test_constructor() {
        new TapeDriveWorker(
            tapeRobotPool,
            tapeDriveService,
            tapeCatalogService,
            tapeDriveOrderConsumer,
            archiveReferentialRepository,
            accessRequestManager,
            null,
            inputTarDir.getAbsolutePath(),
            false,
            archiveCacheStorage,
            FULL_CARTRIDGE_THRESHOLD
        );

        try {
            new TapeDriveWorker(
                null,
                tapeDriveService,
                tapeCatalogService,
                tapeDriveOrderConsumer,
                archiveReferentialRepository,
                accessRequestManager,
                null,
                inputTarDir.getAbsolutePath(),
                false,
                archiveCacheStorage,
                FULL_CARTRIDGE_THRESHOLD
            );
            Assertions.fail("Should fail tapeRobotPool required");
        } catch (Exception e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }

        try {
            new TapeDriveWorker(
                tapeRobotPool,
                null,
                tapeCatalogService,
                tapeDriveOrderConsumer,
                archiveReferentialRepository,
                accessRequestManager,
                null,
                inputTarDir.getAbsolutePath(),
                false,
                archiveCacheStorage,
                FULL_CARTRIDGE_THRESHOLD
            );
            Assertions.fail("Should fail tapeDriveService required");
        } catch (Exception e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }

        try {
            new TapeDriveWorker(
                tapeRobotPool,
                tapeDriveService,
                null,
                tapeDriveOrderConsumer,
                archiveReferentialRepository,
                accessRequestManager,
                null,
                inputTarDir.getAbsolutePath(),
                false,
                archiveCacheStorage,
                FULL_CARTRIDGE_THRESHOLD
            );
            Assertions.fail("Should fail tapeCatalogService required");
        } catch (Exception e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }

        try {
            new TapeDriveWorker(
                tapeRobotPool,
                tapeDriveService,
                tapeCatalogService,
                null,
                archiveReferentialRepository,
                accessRequestManager,
                null,
                inputTarDir.getAbsolutePath(),
                false,
                archiveCacheStorage,
                FULL_CARTRIDGE_THRESHOLD
            );
            Assertions.fail("Should fail tapeDriveOrderConsumer required");
        } catch (Exception e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }

        try {
            new TapeDriveWorker(
                tapeRobotPool,
                tapeDriveService,
                tapeCatalogService,
                tapeDriveOrderConsumer,
                null,
                accessRequestManager,
                null,
                inputTarDir.getAbsolutePath(),
                false,
                archiveCacheStorage,
                FULL_CARTRIDGE_THRESHOLD
            );
            Assertions.fail("Should fail archiveReferentialRepository required");
        } catch (Exception e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }

        try {
            new TapeDriveWorker(
                tapeRobotPool,
                tapeDriveService,
                tapeCatalogService,
                tapeDriveOrderConsumer,
                archiveReferentialRepository,
                null,
                null,
                inputTarDir.getAbsolutePath(),
                false,
                archiveCacheStorage,
                FULL_CARTRIDGE_THRESHOLD
            );
            Assertions.fail("Should fail accessRequestManager required");
        } catch (Exception e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }

        try {
            new TapeDriveWorker(
                tapeRobotPool,
                tapeDriveService,
                tapeCatalogService,
                tapeDriveOrderConsumer,
                archiveReferentialRepository,
                accessRequestManager,
                null,
                inputTarDir.getAbsolutePath(),
                false,
                null,
                FULL_CARTRIDGE_THRESHOLD
            );
            Assertions.fail("Should fail archiveOutputRetentionPolicy required");
        } catch (Exception e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }
    }

    @Test
    public void run() {
        // TODO (djh): 28/03/19
    }

    @Test
    public void stop_wait() throws InterruptedException {
        TapeDriveWorker tapeDriveWorker = new TapeDriveWorker(
            tapeRobotPool,
            tapeDriveService,
            tapeCatalogService,
            tapeDriveOrderConsumer,
            archiveReferentialRepository,
            accessRequestManager,
            null,
            null,
            1000,
            false,
            archiveCacheStorage,
            FULL_CARTRIDGE_THRESHOLD
        );
        Thread thread1 = new Thread(tapeDriveWorker);
        thread1.start();
        tapeDriveWorker.stop();
        Thread.sleep(2);
        assertThat(tapeDriveWorker.isRunning()).isFalse();
    }

    @Test
    public void stop_no_wait() throws QueueException, InterruptedException {
        TapeDriveWorker tapeDriveWorker = new TapeDriveWorker(
            tapeRobotPool,
            tapeDriveService,
            tapeCatalogService,
            tapeDriveOrderConsumer,
            archiveReferentialRepository,
            accessRequestManager,
            null,
            null,
            100,
            false,
            archiveCacheStorage,
            FULL_CARTRIDGE_THRESHOLD
        );

        when(tapeDriveOrderConsumer.consume(any())).thenAnswer(o -> {
            Thread.sleep(20);
            return Optional.empty();
        });
        Thread thread1 = new Thread(tapeDriveWorker);
        thread1.start();
        Thread.sleep(5);

        assertThatThrownBy(() -> tapeDriveWorker.stop(1, TimeUnit.MICROSECONDS)).isInstanceOf(TimeoutException.class);
        assertThat(tapeDriveWorker.isRunning()).isTrue();

        Thread.sleep(150);

        assertThat(tapeDriveWorker.isRunning()).isFalse();
    }

    @Test
    public void test_get_index_ok() throws QueueException {
        TapeDriveWorker tapeDriveWorker = new TapeDriveWorker(
            tapeRobotPool,
            tapeDriveService,
            tapeCatalogService,
            tapeDriveOrderConsumer,
            archiveReferentialRepository,
            accessRequestManager,
            null,
            null,
            1000,
            false,
            archiveCacheStorage,
            FULL_CARTRIDGE_THRESHOLD
        );
        when(tapeDriveConf.getIndex()).thenReturn(1);
        when(tapeDriveOrderConsumer.consume(eq(tapeDriveWorker))).thenAnswer(o -> {
            Thread.sleep(5);
            return Optional.empty();
        });
        Thread thread1 = new Thread(tapeDriveWorker);
        thread1.start();
        assertThat(tapeDriveWorker.getIndex()).isEqualTo(1);
        verify(tapeDriveConf, VerificationModeFactory.times(3)).getIndex();

        tapeDriveWorker.stop();
        assertThat(tapeDriveWorker.isRunning()).isFalse();
    }

    @Test
    public void test_get_priority_ok() throws QueueException {
        TapeDriveWorker tapeDriveWorker = new TapeDriveWorker(
            tapeRobotPool,
            tapeDriveService,
            tapeCatalogService,
            tapeDriveOrderConsumer,
            archiveReferentialRepository,
            accessRequestManager,
            null,
            null,
            1000,
            false,
            archiveCacheStorage,
            FULL_CARTRIDGE_THRESHOLD
        );
        when(tapeDriveConf.getReadWritePriority()).thenReturn(ReadWritePriority.READ);
        when(tapeDriveOrderConsumer.consume(any())).thenAnswer(o -> {
            Thread.sleep(5);
            return Optional.empty();
        });
        Thread thread1 = new Thread(tapeDriveWorker);
        thread1.start();
        assertThat(tapeDriveWorker.getPriority()).isEqualTo(ReadWritePriority.READ);
        verify(tapeDriveConf, VerificationModeFactory.times(1)).getReadWritePriority();

        tapeDriveWorker.stop();
        assertThat(tapeDriveWorker.isRunning()).isFalse();
    }

    @Test
    public void test_get_read_write_result_and_current_tape() throws QueueException {
        TapeDriveWorker tapeDriveWorker = new TapeDriveWorker(
            tapeRobotPool,
            tapeDriveService,
            tapeCatalogService,
            tapeDriveOrderConsumer,
            archiveReferentialRepository,
            accessRequestManager,
            null,
            null,
            1000,
            false,
            archiveCacheStorage,
            FULL_CARTRIDGE_THRESHOLD
        );

        when(tapeDriveConf.getReadWritePriority()).thenReturn(ReadWritePriority.READ);
        when(tapeDriveOrderConsumer.consume(any())).thenAnswer(o -> {
            Thread.sleep(5);
            return Optional.empty();
        });
        Thread thread1 = new Thread(tapeDriveWorker);
        thread1.start();
        assertThat(tapeDriveWorker.getReadWriteResult()).isNull();
        assertThat(tapeDriveWorker.getCurrentTape()).isNull();

        tapeDriveWorker.stop();
        assertThat(tapeDriveWorker.isRunning()).isFalse();
    }

    @Test
    public void test_initialize_empty_drive_on_bootstrap() throws Exception {
        TapeDriveWorker tapeDriveWorker = new TapeDriveWorker(
            tapeRobotPool,
            tapeDriveService,
            tapeCatalogService,
            tapeDriveOrderConsumer,
            archiveReferentialRepository,
            accessRequestManager,
            null,
            null,
            1000,
            false,
            archiveCacheStorage,
            FULL_CARTRIDGE_THRESHOLD
        );

        TapeDriveSpec driveStatus = new TapeDriveState();
        driveStatus.getDriveStatuses().add(TapeDriveStatus.DR_OPEN);
        doReturn(driveStatus).when(tapeDriveCommandService).status();

        // When
        tapeDriveWorker.initializeOnBootstrap();

        // Then
        verify(tapeDriveCommandService).status();
        verifyNoMoreInteractions(tapeDriveCommandService);
    }

    @Test
    public void test_initialize_non_empty_loaded_drive_on_bootstrap() throws Exception {
        TapeCatalogLabel tapeCatalogLabel = new TapeCatalogLabel().setCode("tapeCode").setBucket("bucket");

        TapeCatalog tapeCatalog = new TapeCatalog()
            .setLibrary("lib")
            .setCode("tapeCode")
            .setCurrentPosition(12)
            .setTapeState(TapeState.OPEN)
            .setCurrentLocation(new TapeLocation(2, TapeLocationType.DRIVE))
            .setLabel(tapeCatalogLabel);

        TapeDriveWorker tapeDriveWorker = new TapeDriveWorker(
            tapeRobotPool,
            tapeDriveService,
            tapeCatalogService,
            tapeDriveOrderConsumer,
            archiveReferentialRepository,
            accessRequestManager,
            tapeCatalog,
            null,
            1000,
            false,
            archiveCacheStorage,
            FULL_CARTRIDGE_THRESHOLD
        );

        TapeDriveSpec driveStatus = new TapeDriveState();
        driveStatus.getDriveStatuses().add(TapeDriveStatus.ONLINE);
        doReturn(driveStatus).when(tapeDriveCommandService).status();

        doAnswer(args -> {
            String labelPath = args.getArgument(0);
            JsonHandler.writeAsFile(tapeCatalogLabel, tmpTarOutputDir.toPath().resolve(labelPath).toFile());
            return null;
        })
            .when(tapeReadWriteService)
            .readFromTape(any());

        // When
        tapeDriveWorker.initializeOnBootstrap();

        // Then
        InOrder inOrder = Mockito.inOrder(tapeDriveCommandService, tapeReadWriteService);
        // Check status
        inOrder.verify(tapeDriveCommandService).status();
        // Reset
        inOrder.verify(tapeDriveCommandService).rewind();
        // Check label
        inOrder.verify(tapeReadWriteService).getTmpOutputStorageFolder();
        inOrder.verify(tapeReadWriteService).readFromTape(any());
        inOrder.verifyNoMoreInteractions();

        verify(tapeCatalogService).replace(tapeCatalog);
    }

    @Test
    public void test_initialize_empty_loaded_drive_on_bootstrap() throws Exception {
        TapeCatalog tapeCatalog = new TapeCatalog()
            .setLibrary("lib")
            .setCode("tapeCode")
            .setCurrentPosition(0)
            .setTapeState(TapeState.EMPTY)
            .setCurrentLocation(new TapeLocation(2, TapeLocationType.DRIVE))
            .setLabel(null);

        TapeDriveWorker tapeDriveWorker = new TapeDriveWorker(
            tapeRobotPool,
            tapeDriveService,
            tapeCatalogService,
            tapeDriveOrderConsumer,
            archiveReferentialRepository,
            accessRequestManager,
            tapeCatalog,
            null,
            1000,
            false,
            archiveCacheStorage,
            FULL_CARTRIDGE_THRESHOLD
        );

        TapeDriveState driveStatus = new TapeDriveState();
        driveStatus.getDriveStatuses().add(TapeDriveStatus.ONLINE);
        driveStatus.setCartridge("LTO-6");
        doReturn(driveStatus).when(tapeDriveCommandService).status();

        doThrow(new TapeCommandException("Cannot advance, empty tape")).when(tapeDriveCommandService).move(1, false);

        // When
        tapeDriveWorker.initializeOnBootstrap();

        // Then
        InOrder inOrder = Mockito.inOrder(tapeDriveCommandService, tapeReadWriteService);
        // Check status
        inOrder.verify(tapeDriveCommandService).status();
        // Reset
        inOrder.verify(tapeDriveCommandService).rewind();
        // Check empty
        inOrder.verify(tapeDriveCommandService).move(1, false);
        // Get cartridge type from drive status information
        inOrder.verify(tapeDriveCommandService).status();
        inOrder.verifyNoMoreInteractions();

        // Ensure that cartridge type has been updated
        verify(tapeCatalogService).replace(tapeCatalog);
        assertThat(tapeCatalog.getType()).isEqualTo("LTO-6");
    }

    @Test
    public void test_initialize_loaded_drive_with_invalid_label_on_bootstrap() throws Exception {
        TapeCatalogLabel tapeCatalogLabel = new TapeCatalogLabel().setCode("tapeCode").setBucket("bucket");

        TapeCatalog tapeCatalog = new TapeCatalog()
            .setLibrary("lib")
            .setCode("tapeCode")
            .setCurrentPosition(12)
            .setTapeState(TapeState.OPEN)
            .setCurrentLocation(new TapeLocation(2, TapeLocationType.DRIVE))
            .setLabel(tapeCatalogLabel);

        TapeDriveWorker tapeDriveWorker = new TapeDriveWorker(
            tapeRobotPool,
            tapeDriveService,
            tapeCatalogService,
            tapeDriveOrderConsumer,
            archiveReferentialRepository,
            accessRequestManager,
            tapeCatalog,
            null,
            1000,
            false,
            archiveCacheStorage,
            FULL_CARTRIDGE_THRESHOLD
        );

        TapeDriveSpec driveStatus = new TapeDriveState();
        driveStatus.getDriveStatuses().add(TapeDriveStatus.ONLINE);
        doReturn(driveStatus).when(tapeDriveCommandService).status();

        doAnswer(args -> {
            TapeCatalogLabel invalidTapeCatalogLabel = new TapeCatalogLabel().setCode("WRONG").setBucket("bucket");

            String labelPath = args.getArgument(0);
            JsonHandler.writeAsFile(invalidTapeCatalogLabel, tmpTarOutputDir.toPath().resolve(labelPath).toFile());
            return null;
        })
            .when(tapeReadWriteService)
            .readFromTape(any());

        // When / Then
        assertThatThrownBy(tapeDriveWorker::initializeOnBootstrap).isInstanceOf(ReadWriteException.class);

        InOrder inOrder = Mockito.inOrder(tapeDriveCommandService, tapeReadWriteService);
        // Check status
        inOrder.verify(tapeDriveCommandService).status();
        // Reset
        inOrder.verify(tapeDriveCommandService).rewind();
        // Check label
        inOrder.verify(tapeReadWriteService).getTmpOutputStorageFolder();
        inOrder.verify(tapeReadWriteService).readFromTape(any());
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void test_initialize_ejected_drive_on_bootstrap() throws Exception {
        TapeCatalogLabel tapeCatalogLabel = new TapeCatalogLabel().setCode("tapeCode").setBucket("bucket");

        TapeCatalog tapeCatalog = new TapeCatalog()
            .setLibrary("lib")
            .setCode("tapeCode")
            .setCurrentPosition(12)
            .setTapeState(TapeState.OPEN)
            .setCurrentLocation(new TapeLocation(2, TapeLocationType.DRIVE))
            .setLabel(tapeCatalogLabel);

        TapeDriveWorker tapeDriveWorker = new TapeDriveWorker(
            tapeRobotPool,
            tapeDriveService,
            tapeCatalogService,
            tapeDriveOrderConsumer,
            archiveReferentialRepository,
            accessRequestManager,
            tapeCatalog,
            null,
            1000,
            false,
            archiveCacheStorage,
            FULL_CARTRIDGE_THRESHOLD
        );

        TapeDriveSpec driveStatus = new TapeDriveState();
        driveStatus.getDriveStatuses().add(TapeDriveStatus.DR_OPEN);
        doReturn(driveStatus).when(tapeDriveCommandService).status();

        doAnswer(args -> {
            String labelPath = args.getArgument(0);
            JsonHandler.writeAsFile(tapeCatalogLabel, tmpTarOutputDir.toPath().resolve(labelPath).toFile());
            return null;
        })
            .when(tapeReadWriteService)
            .readFromTape(any());

        // When / Then
        assertThatThrownBy(tapeDriveWorker::initializeOnBootstrap).isInstanceOf(IllegalStateException.class);

        InOrder inOrder = Mockito.inOrder(tapeDriveCommandService, tapeReadWriteService);
        // Check status
        inOrder.verify(tapeDriveCommandService).status();
        inOrder.verifyNoMoreInteractions();
    }
}
