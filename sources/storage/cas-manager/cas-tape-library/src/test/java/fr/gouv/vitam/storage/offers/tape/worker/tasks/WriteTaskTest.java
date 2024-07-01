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
package fr.gouv.vitam.storage.offers.tape.worker.tasks;

import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.storage.tapelibrary.TapeDriveConf;
import fr.gouv.vitam.storage.engine.common.model.QueueMessageType;
import fr.gouv.vitam.storage.engine.common.model.QueueState;
import fr.gouv.vitam.storage.engine.common.model.TapeCatalog;
import fr.gouv.vitam.storage.engine.common.model.TapeCatalogLabel;
import fr.gouv.vitam.storage.engine.common.model.TapeLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeLocationType;
import fr.gouv.vitam.storage.engine.common.model.TapeState;
import fr.gouv.vitam.storage.engine.common.model.WriteOrder;
import fr.gouv.vitam.storage.offers.tape.cas.ArchiveCacheStorage;
import fr.gouv.vitam.storage.offers.tape.cas.ArchiveReferentialRepository;
import fr.gouv.vitam.storage.offers.tape.dto.TapeDriveState;
import fr.gouv.vitam.storage.offers.tape.exception.TapeCatalogException;
import fr.gouv.vitam.storage.offers.tape.exception.TapeCommandException;
import fr.gouv.vitam.storage.offers.tape.impl.readwrite.TapeLibraryServiceImpl;
import fr.gouv.vitam.storage.offers.tape.spec.TapeCatalogService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeDriveCommandService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeDriveService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeLoadUnloadService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeReadWriteService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeRobotPool;
import fr.gouv.vitam.storage.offers.tape.spec.TapeRobotService;
import fr.gouv.vitam.storage.offers.tape.utils.LocalFileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.internal.verification.Times;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class WriteTaskTest {

    private static final int FULL_CARTRIDGE_THRESHOLD = 2_000_000;
    private static final String FAKE_BUCKET = "fakeBucket";
    private static final String FAKE_FILE_BUCKET_ID = "fakeFileBucketId";
    private static final String FAKE_LIBRARY = "fakeLibrary";
    private static final String LTO_6 = "LTO-6";
    private static final String LTO_7 = "LTO-7";
    private static final String TAPE_CODE = "VIT0001";
    private static final String FAKE_FILE_PATH = "fakeFilePath";
    private static final String FAKE_DIGEST = "digest";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private TapeRobotPool tapeRobotPool;

    @Mock
    private TapeRobotService tapeRobotService;

    @Mock
    private TapeDriveService tapeDriveService;

    @Mock
    private TapeLoadUnloadService tapeLoadUnloadService;

    @Mock
    private TapeCatalogService tapeCatalogService;

    @Mock
    private TapeReadWriteService tapeReadWriteService;

    @Mock
    private TapeDriveCommandService tapeDriveCommandService;

    @Mock
    private ArchiveReferentialRepository archiveReferentialRepository;

    @Mock
    private ArchiveCacheStorage archiveCacheStorage;

    private File inputTarDir;

    @Before
    public void setUp() throws Exception {
        when(tapeDriveService.getReadWriteService()).thenAnswer(o -> tapeReadWriteService);
        when(tapeDriveService.getDriveCommandService()).thenAnswer(o -> tapeDriveCommandService);
        when(tapeRobotPool.checkoutRobotService()).thenAnswer(o -> tapeRobotService);
        when(tapeRobotService.getLoadUnloadService()).thenAnswer(o -> tapeLoadUnloadService);

        inputTarDir = temporaryFolder.newFolder("inputTars");
        File tmpTarOutputDir = temporaryFolder.newFolder("tmpTarOutput");

        when(tapeReadWriteService.getTmpOutputStorageFolder()).thenReturn(tmpTarOutputDir.getAbsolutePath());

        doNothing().when(archiveCacheStorage).reserveArchiveStorageSpace(anyString(), anyString(), anyLong());
        doAnswer(args -> {
            Path filePath = args.getArgument(0, Path.class);
            assertThat(filePath).exists();
            Files.delete(filePath);
            return null;
        })
            .when(archiveCacheStorage)
            .moveArchiveToCache(any(), anyString(), anyString());
    }

    @After
    public void cleanup() {
        verifyNoMoreInteractions(archiveCacheStorage);
    }

    @Test
    public void test_write_task_constructor() {
        // Given
        when(tapeDriveService.getTapeDriveConf()).thenAnswer(o -> mock(TapeDriveConf.class));

        // When / Then

        // tapeLibraryService
        assertThatThrownBy(
            () ->
                new WriteTask(
                    mock(WriteOrder.class),
                    null,
                    null,
                    tapeCatalogService,
                    archiveReferentialRepository,
                    archiveCacheStorage,
                    FAKE_FILE_PATH,
                    false
                )
        )
            .withFailMessage("should fail tapeLibraryService is required")
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(
            () ->
                new WriteTask(
                    mock(WriteOrder.class),
                    null,
                    new TapeLibraryServiceImpl(tapeDriveService, tapeRobotPool, FULL_CARTRIDGE_THRESHOLD),
                    null,
                    archiveReferentialRepository,
                    archiveCacheStorage,
                    FAKE_FILE_PATH,
                    false
                )
        ).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(
            () ->
                new WriteTask(
                    mock(WriteOrder.class),
                    null,
                    new TapeLibraryServiceImpl(tapeDriveService, tapeRobotPool, FULL_CARTRIDGE_THRESHOLD),
                    tapeCatalogService,
                    null,
                    archiveCacheStorage,
                    FAKE_FILE_PATH,
                    false
                )
        )
            .withFailMessage("should fail tapeCatalogService is required")
            .isInstanceOf(IllegalArgumentException.class);

        assertThatCode(
            () ->
                new WriteTask(
                    mock(WriteOrder.class),
                    null,
                    new TapeLibraryServiceImpl(tapeDriveService, tapeRobotPool, FULL_CARTRIDGE_THRESHOLD),
                    tapeCatalogService,
                    archiveReferentialRepository,
                    null,
                    FAKE_FILE_PATH,
                    false
                )
        )
            .withFailMessage("should fail archiveCacheStorage is required")
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(
            () ->
                new WriteTask(
                    mock(WriteOrder.class),
                    null,
                    new TapeLibraryServiceImpl(tapeDriveService, tapeRobotPool, FULL_CARTRIDGE_THRESHOLD),
                    tapeCatalogService,
                    archiveReferentialRepository,
                    archiveCacheStorage,
                    null,
                    false
                )
        )
            .withFailMessage("should fail inputTarPath is required")
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(
            () ->
                new WriteTask(
                    mock(WriteOrder.class),
                    null,
                    new TapeLibraryServiceImpl(tapeDriveService, tapeRobotPool, FULL_CARTRIDGE_THRESHOLD),
                    tapeCatalogService,
                    archiveReferentialRepository,
                    archiveCacheStorage,
                    "",
                    false
                )
        )
            .withFailMessage("should fail inputTarPath is required")
            .isInstanceOf(IllegalArgumentException.class);

        assertThatCode(
            () ->
                new WriteTask(
                    mock(WriteOrder.class),
                    null,
                    new TapeLibraryServiceImpl(tapeDriveService, tapeRobotPool, FULL_CARTRIDGE_THRESHOLD),
                    tapeCatalogService,
                    archiveReferentialRepository,
                    archiveCacheStorage,
                    FAKE_FILE_PATH,
                    false
                )
        ).doesNotThrowAnyException();
    }

    @Test
    public void test_write_file_with_loaded_tape_at_end_of_tape_position_then_write_OK() throws Exception {
        // Given
        when(tapeDriveService.getTapeDriveConf()).thenAnswer(o -> mock(TapeDriveConf.class));
        String file = getTarFileName();
        String filePath = FAKE_FILE_PATH + File.separator + file;

        WriteOrder writeOrder = new WriteOrder(
            FAKE_BUCKET,
            FAKE_FILE_BUCKET_ID,
            filePath,
            10L,
            FAKE_DIGEST,
            file,
            QueueMessageType.WriteOrder
        );

        TapeCatalog tape = getTapeCatalog(true, false, TapeState.OPEN);
        tape.setCurrentPosition(2);
        tape.setFileCount(2);

        WriteTask writeTask = new WriteTask(
            writeOrder,
            tape,
            new TapeLibraryServiceImpl(tapeDriveService, tapeRobotPool, FULL_CARTRIDGE_THRESHOLD),
            tapeCatalogService,
            archiveReferentialRepository,
            archiveCacheStorage,
            inputTarDir.getAbsolutePath(),
            false
        );

        doNothing().when(tapeReadWriteService).writeToTape(eq(FAKE_FILE_PATH + "/" + writeOrder.getArchiveId()));

        // When
        ReadWriteResult result = writeTask.get();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(StatusCode.OK);
        assertThat(result.getOrderState()).isEqualTo(QueueState.COMPLETED);

        verify(tapeDriveCommandService, never()).status();
        verify(tapeDriveCommandService, never()).eject();
        verify(tapeDriveCommandService, never()).move(anyInt(), anyBoolean());
        verify(tapeLoadUnloadService, never()).loadTape(anyInt(), anyInt());
        verify(tapeDriveCommandService, never()).rewind();
        verify(tapeLoadUnloadService, never()).unloadTape(anyInt(), anyInt());

        verify(tapeReadWriteService).writeToTape(eq(FAKE_FILE_PATH + "/" + writeOrder.getArchiveId()));

        assertThat(result.getCurrentTape()).isNotNull();

        assertThat(result.getCurrentTape().getFileCount()).isEqualTo(3);
        assertThat(result.getCurrentTape().getCurrentPosition()).isEqualTo(3);
        assertThat(result.getCurrentTape().getTapeState()).isEqualTo(TapeState.OPEN);

        verify(archiveCacheStorage).reserveArchiveStorageSpace(FAKE_FILE_BUCKET_ID, file, 10L);
        verify(archiveCacheStorage).moveArchiveToCache(
            eq(inputTarDir.toPath().resolve(filePath)),
            eq(FAKE_FILE_BUCKET_ID),
            eq(file)
        );
    }

    @Test
    public void test_current_tape_not_null_and_empty_label_success() throws Exception {
        // When
        when(tapeDriveService.getTapeDriveConf()).thenAnswer(o -> mock(TapeDriveConf.class));
        String file = getTarFileName();

        // Given WriterOrder and TapeCatalog
        String filePath = FAKE_FILE_PATH + File.separator + file;
        WriteOrder writeOrder = new WriteOrder(
            FAKE_BUCKET,
            FAKE_FILE_BUCKET_ID,
            filePath,
            10L,
            FAKE_DIGEST,
            file,
            QueueMessageType.WriteOrder
        );

        TapeCatalog tapeCatalog = getTapeCatalog(false, false, TapeState.EMPTY);

        // When write to tape (label and file)
        doNothing().when(tapeReadWriteService).writeToTape(contains(WriteTask.TAPE_LABEL));

        doNothing().when(tapeReadWriteService).writeToTape(eq(FAKE_FILE_PATH + "/" + writeOrder.getArchiveId()));

        doNothing().when(tapeDriveCommandService).rewind();

        WriteTask writeTask = new WriteTask(
            writeOrder,
            tapeCatalog,
            new TapeLibraryServiceImpl(tapeDriveService, tapeRobotPool, FULL_CARTRIDGE_THRESHOLD),
            tapeCatalogService,
            archiveReferentialRepository,
            archiveCacheStorage,
            inputTarDir.getAbsolutePath(),
            false
        );

        ReadWriteResult result = writeTask.get();

        // Then
        verify(tapeReadWriteService, new Times(1)).writeToTape(contains(WriteTask.TAPE_LABEL));

        verify(tapeReadWriteService, new Times(1)).writeToTape(eq(FAKE_FILE_PATH + "/" + writeOrder.getArchiveId()));
        verify(tapeDriveCommandService, times(1)).rewind();

        ArgumentCaptor<String> fileName = ArgumentCaptor.forClass(String.class);

        verify(tapeReadWriteService, new Times(2)).writeToTape(fileName.capture());

        assertThat(fileName.getAllValues()).hasSize(2);

        assertThat(fileName.getAllValues().get(0))
            .contains(LocalFileUtils.INPUT_TAR_TMP_FOLDER)
            .contains(WriteTask.TAPE_LABEL);
        assertThat(fileName.getAllValues().get(1)).contains(FAKE_FILE_PATH).contains(".tar");

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(StatusCode.OK);
        assertThat(result.getOrderState()).isEqualTo(QueueState.COMPLETED);

        assertThat(result.getCurrentTape()).isNotNull();

        assertThat(result.getCurrentTape().getFileCount()).isEqualTo(2);
        assertThat(result.getCurrentTape().getTapeState()).isEqualTo(TapeState.OPEN);

        TapeCatalogLabel label = result.getCurrentTape().getLabel();

        assertThat(label).isNotNull();
        assertThat(label)
            .extracting("id", TapeCatalogLabel.CODE, TapeCatalogLabel.BUCKET, TapeCatalogLabel.TYPE)
            .contains(tapeCatalog.getId(), tapeCatalog.getCode(), tapeCatalog.getBucket(), tapeCatalog.getType());

        verify(archiveCacheStorage).reserveArchiveStorageSpace(FAKE_FILE_BUCKET_ID, file, 10L);
        verify(archiveCacheStorage).moveArchiveToCache(
            eq(inputTarDir.toPath().resolve(filePath)),
            eq(FAKE_FILE_BUCKET_ID),
            eq(file)
        );
    }

    @Test
    public void test_current_tape_not_null_and_have_label_success() throws Exception {
        // When
        when(tapeDriveService.getTapeDriveConf()).thenAnswer(o -> mock(TapeDriveConf.class));
        String file = getTarFileName();

        String filePath = FAKE_FILE_PATH + File.separator + file;
        WriteOrder writeOrder = new WriteOrder(
            FAKE_BUCKET,
            FAKE_FILE_BUCKET_ID,
            filePath,
            10L,
            FAKE_DIGEST,
            file,
            QueueMessageType.WriteOrder
        );

        TapeCatalog tapeCatalog = getTapeCatalog(true, false, TapeState.EMPTY);

        doNothing().when(tapeReadWriteService).writeToTape(eq(FAKE_FILE_PATH + "/" + writeOrder.getArchiveId()));

        doNothing().when(tapeDriveCommandService).rewind();

        WriteTask writeTask = new WriteTask(
            writeOrder,
            tapeCatalog,
            new TapeLibraryServiceImpl(tapeDriveService, tapeRobotPool, FULL_CARTRIDGE_THRESHOLD),
            tapeCatalogService,
            archiveReferentialRepository,
            archiveCacheStorage,
            inputTarDir.getAbsolutePath(),
            false
        );

        ReadWriteResult result = writeTask.get();

        verify(tapeReadWriteService, new Times(0)).writeToTape(contains(WriteTask.TAPE_LABEL));

        verify(tapeReadWriteService, new Times(1)).writeToTape(eq(FAKE_FILE_PATH + "/" + writeOrder.getArchiveId()));
        verify(tapeDriveCommandService, times(1)).rewind();

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(StatusCode.OK);
        assertThat(result.getOrderState()).isEqualTo(QueueState.COMPLETED);

        assertThat(result.getCurrentTape()).isNotNull();
        assertThat(result.getCurrentTape().getFileCount()).isEqualTo(1);
        assertThat(result.getCurrentTape().getTapeState()).isEqualTo(TapeState.OPEN);

        TapeCatalogLabel label = result.getCurrentTape().getLabel();

        assertThat(label).isNotNull();
        assertThat(label)
            .extracting("id", TapeCatalogLabel.CODE, TapeCatalogLabel.BUCKET, TapeCatalogLabel.TYPE)
            .contains(tapeCatalog.getId(), tapeCatalog.getCode(), tapeCatalog.getBucket(), tapeCatalog.getType());

        verify(archiveCacheStorage).reserveArchiveStorageSpace(FAKE_FILE_BUCKET_ID, file, 10L);
        verify(archiveCacheStorage).moveArchiveToCache(
            eq(inputTarDir.toPath().resolve(filePath)),
            eq(FAKE_FILE_BUCKET_ID),
            eq(file)
        );
    }

    @Test
    public void test_current_tape_null_then_load_tape_with_empty_label_success() throws Exception {
        // When
        when(tapeDriveService.getTapeDriveConf()).thenAnswer(o -> mock(TapeDriveConf.class));
        String file = getTarFileName();
        String filePath = FAKE_FILE_PATH + File.separator + file;

        // given
        WriteOrder writeOrder = new WriteOrder(
            FAKE_BUCKET,
            FAKE_FILE_BUCKET_ID,
            filePath,
            10L,
            FAKE_DIGEST,
            file,
            QueueMessageType.WriteOrder
        );

        WriteTask writeTask = new WriteTask(
            writeOrder,
            null,
            new TapeLibraryServiceImpl(tapeDriveService, tapeRobotPool, FULL_CARTRIDGE_THRESHOLD),
            tapeCatalogService,
            archiveReferentialRepository,
            archiveCacheStorage,
            inputTarDir.getAbsolutePath(),
            false
        );

        TapeCatalog tapeCatalog = getTapeCatalog(false, false, TapeState.EMPTY);
        tapeCatalog.setCurrentLocation(new TapeLocation(1, TapeLocationType.SLOT));
        when(tapeCatalogService.receive(any())).thenReturn(Optional.of(tapeCatalog));

        doNothing().when(tapeLoadUnloadService).loadTape(anyInt(), anyInt());
        doNothing().when(tapeDriveCommandService).rewind();

        TapeDriveState tapeDriveState = new TapeDriveState();
        tapeDriveState.setCartridge("LTO-6");

        when(tapeDriveCommandService.status()).thenThrow(new TapeCommandException("error")).thenReturn(tapeDriveState);

        // When Write Label and file to tape
        doNothing().when(tapeReadWriteService).writeToTape(contains(WriteTask.TAPE_LABEL));

        doNothing().when(tapeReadWriteService).writeToTape(eq(FAKE_FILE_PATH + "/" + writeOrder.getArchiveId()));

        doThrow(new TapeCommandException("error"))
            .doNothing()
            .when(tapeDriveCommandService)
            .move(anyInt(), anyBoolean());

        ReadWriteResult result = writeTask.get();

        // Then
        verify(tapeRobotPool, new Times(1)).checkoutRobotService();
        verify(tapeRobotService, new Times(1)).getLoadUnloadService();
        verify(tapeDriveCommandService, new Times(2)).status();
        verify(tapeDriveCommandService, new Times(1)).move(anyInt(), anyBoolean());
        verify(tapeLoadUnloadService, new Times(1)).loadTape(anyInt(), anyInt());
        verify(tapeDriveCommandService, times(2)).rewind();
        verify(tapeReadWriteService, new Times(1)).writeToTape(contains(WriteTask.TAPE_LABEL));
        verify(tapeReadWriteService, new Times(1)).writeToTape(eq(FAKE_FILE_PATH + "/" + writeOrder.getArchiveId()));

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(StatusCode.OK);
        assertThat(result.getOrderState()).isEqualTo(QueueState.COMPLETED);

        assertThat(result.getCurrentTape()).isNotNull();

        assertThat(result.getCurrentTape().getFileCount()).isEqualTo(2);
        assertThat(result.getCurrentTape().getType()).isEqualTo(tapeDriveState.getCartridge());
        assertThat(result.getCurrentTape().getTapeState()).isEqualTo(TapeState.OPEN);

        TapeCatalogLabel label = result.getCurrentTape().getLabel();

        assertThat(label).isNotNull();
        assertThat(label)
            .extracting("id", TapeCatalogLabel.CODE, TapeCatalogLabel.BUCKET, TapeCatalogLabel.TYPE)
            .contains(tapeCatalog.getId(), tapeCatalog.getCode(), tapeCatalog.getBucket(), tapeCatalog.getType());

        verify(archiveCacheStorage).reserveArchiveStorageSpace(FAKE_FILE_BUCKET_ID, file, 10L);
        verify(archiveCacheStorage).moveArchiveToCache(
            eq(inputTarDir.toPath().resolve(filePath)),
            eq(FAKE_FILE_BUCKET_ID),
            eq(file)
        );
    }

    /**
     * TapeCatalog does not contains label
     * Loaded tape already contains file
     * Discard between tapeCatalog and loaded tape (perhaps someone changed manually the tape in the slot without re-init catalog)
     */
    @Test
    public void test_current_tape_null_then_load_tape_with_empty_label_and_loaded_tape_not_empty_error()
        throws Exception {
        // When
        when(tapeDriveService.getTapeDriveConf()).thenAnswer(o -> mock(TapeDriveConf.class));
        String file = getTarFileName();
        String filePath = FAKE_FILE_PATH + File.separator + file;

        // given
        WriteOrder writeOrder = new WriteOrder(
            FAKE_BUCKET,
            FAKE_FILE_BUCKET_ID,
            filePath,
            10L,
            FAKE_DIGEST,
            file,
            QueueMessageType.WriteOrder
        );

        WriteTask writeTask = new WriteTask(
            writeOrder,
            null,
            new TapeLibraryServiceImpl(tapeDriveService, tapeRobotPool, FULL_CARTRIDGE_THRESHOLD),
            tapeCatalogService,
            archiveReferentialRepository,
            archiveCacheStorage,
            inputTarDir.getAbsolutePath(),
            false
        );

        TapeCatalog tapeCatalog = getTapeCatalog(false, false, TapeState.EMPTY);
        tapeCatalog.setCurrentLocation(new TapeLocation(1, TapeLocationType.SLOT));
        when(tapeCatalogService.receive(any())).thenReturn(Optional.of(tapeCatalog));

        doNothing().when(tapeLoadUnloadService).loadTape(anyInt(), anyInt());
        doNothing().when(tapeDriveCommandService).rewind();

        TapeDriveState tapeDriveState = new TapeDriveState();
        tapeDriveState.setCartridge("LTO-6");

        doNothing().when(tapeDriveCommandService).move(anyInt(), anyBoolean());

        ReadWriteResult result = writeTask.get();

        // Then
        verify(tapeRobotPool, new Times(1)).checkoutRobotService();
        verify(tapeRobotService, new Times(1)).getLoadUnloadService();
        verify(tapeDriveCommandService, new Times(1)).move(anyInt(), anyBoolean());
        verify(tapeLoadUnloadService, new Times(1)).loadTape(anyInt(), anyInt());
        verify(tapeDriveCommandService, times(1)).rewind();

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(StatusCode.FATAL);
        assertThat(result.getOrderState()).isEqualTo(QueueState.READY);

        assertThat(result.getCurrentTape()).isNotNull();

        assertThat(result.getCurrentTape().getFileCount()).isEqualTo(0);
        assertThat(result.getCurrentTape().getType()).isEqualTo(tapeDriveState.getCartridge());
        assertThat(result.getCurrentTape().getTapeState()).isEqualTo(TapeState.CONFLICT);

        TapeCatalogLabel label = result.getCurrentTape().getLabel();

        assertThat(label).isNull();

        verifyNoMoreInteractions(archiveCacheStorage);
    }

    /**
     * TapeCatalog does not contains label
     * Loaded tape already contains file
     * Discard between tapeCatalog and loaded tape (perhaps someone changed manually the tape in the slot without re-init catalog)
     * Flag forceOverrideNonEmptyCartridges enabled
     */
    @Test
    public void test_current_tape_null_then_load_tape_with_empty_label_and_loaded_tape_not_empty_with_force_override_enabled_success()
        throws Exception {
        // When
        when(tapeDriveService.getTapeDriveConf()).thenAnswer(o -> mock(TapeDriveConf.class));
        String file = getTarFileName();
        String filePath = FAKE_FILE_PATH + File.separator + file;

        // given
        WriteOrder writeOrder = new WriteOrder(
            FAKE_BUCKET,
            FAKE_FILE_BUCKET_ID,
            filePath,
            10L,
            FAKE_DIGEST,
            file,
            QueueMessageType.WriteOrder
        );

        WriteTask writeTask = new WriteTask(
            writeOrder,
            null,
            new TapeLibraryServiceImpl(tapeDriveService, tapeRobotPool, FULL_CARTRIDGE_THRESHOLD),
            tapeCatalogService,
            archiveReferentialRepository,
            archiveCacheStorage,
            inputTarDir.getAbsolutePath(),
            true
        );

        TapeCatalog tapeCatalog = getTapeCatalog(false, false, TapeState.EMPTY);
        tapeCatalog.setCurrentLocation(new TapeLocation(1, TapeLocationType.SLOT));
        when(tapeCatalogService.receive(any())).thenReturn(Optional.of(tapeCatalog));

        doNothing().when(tapeLoadUnloadService).loadTape(anyInt(), anyInt());
        doNothing().when(tapeDriveCommandService).rewind();

        TapeDriveState tapeDriveState = new TapeDriveState();
        tapeDriveState.setCartridge("LTO-6");

        when(tapeDriveCommandService.status()).thenThrow(new TapeCommandException("error")).thenReturn(tapeDriveState);

        // When Write Label and file to tape
        doNothing().when(tapeReadWriteService).writeToTape(contains(WriteTask.TAPE_LABEL));

        doNothing().when(tapeReadWriteService).writeToTape(eq(FAKE_FILE_PATH + "/" + writeOrder.getArchiveId()));

        doNothing().when(tapeDriveCommandService).rewind();

        doNothing().when(tapeDriveCommandService).move(anyInt(), anyBoolean());

        ReadWriteResult result = writeTask.get();

        // Then
        verify(tapeRobotPool, new Times(1)).checkoutRobotService();
        verify(tapeRobotService, new Times(1)).getLoadUnloadService();
        verify(tapeDriveCommandService, new Times(2)).status();
        verify(tapeDriveCommandService, new Times(1)).move(anyInt(), anyBoolean());
        verify(tapeLoadUnloadService, new Times(1)).loadTape(anyInt(), anyInt());
        verify(tapeDriveCommandService, times(3)).rewind();
        verify(tapeReadWriteService, new Times(1)).writeToTape(contains(WriteTask.TAPE_LABEL));
        verify(tapeReadWriteService, new Times(1)).writeToTape(eq(FAKE_FILE_PATH + "/" + writeOrder.getArchiveId()));

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(StatusCode.OK);
        assertThat(result.getOrderState()).isEqualTo(QueueState.COMPLETED);

        assertThat(result.getCurrentTape()).isNotNull();

        assertThat(result.getCurrentTape().getFileCount()).isEqualTo(2);
        assertThat(result.getCurrentTape().getType()).isEqualTo(tapeDriveState.getCartridge());
        assertThat(result.getCurrentTape().getTapeState()).isEqualTo(TapeState.OPEN);

        TapeCatalogLabel label = result.getCurrentTape().getLabel();

        assertThat(label).isNotNull();
        assertThat(label)
            .extracting("id", TapeCatalogLabel.CODE, TapeCatalogLabel.BUCKET, TapeCatalogLabel.TYPE)
            .contains(tapeCatalog.getId(), tapeCatalog.getCode(), tapeCatalog.getBucket(), tapeCatalog.getType());

        verify(archiveCacheStorage).reserveArchiveStorageSpace(FAKE_FILE_BUCKET_ID, file, 10L);
        verify(archiveCacheStorage).moveArchiveToCache(
            eq(inputTarDir.toPath().resolve(filePath)),
            eq(FAKE_FILE_BUCKET_ID),
            eq(file)
        );
    }

    /**
     * Check label of tape catalog and loaded tape
     * Tape catalog declare a not empty label but different from the label of loaded tape
     */
    @Test
    public void test_current_tape_null_then_load_tape_check_label_discord_error() throws Exception {
        // When
        when(tapeDriveService.getTapeDriveConf()).thenAnswer(o -> mock(TapeDriveConf.class));
        String file = getTarFileName();
        String filePath = FAKE_FILE_PATH + File.separator + file;

        // given
        WriteOrder writeOrder = new WriteOrder(
            FAKE_BUCKET,
            FAKE_FILE_BUCKET_ID,
            filePath,
            10L,
            FAKE_DIGEST,
            file,
            QueueMessageType.WriteOrder
        );

        WriteTask writeTask = new WriteTask(
            writeOrder,
            null,
            new TapeLibraryServiceImpl(tapeDriveService, tapeRobotPool, FULL_CARTRIDGE_THRESHOLD),
            tapeCatalogService,
            archiveReferentialRepository,
            archiveCacheStorage,
            inputTarDir.getAbsolutePath(),
            false
        );

        TapeCatalog tapeCatalog = getTapeCatalog(true, false, TapeState.OPEN);
        tapeCatalog.setCurrentLocation(new TapeLocation(1, TapeLocationType.SLOT));
        when(tapeCatalogService.receive(any())).thenReturn(Optional.of(tapeCatalog));

        doNothing().when(tapeLoadUnloadService).loadTape(anyInt(), anyInt());
        doNothing().when(tapeDriveCommandService).rewind();

        TapeDriveState tapeDriveState = new TapeDriveState();
        tapeDriveState.setCartridge("LTO-6");

        doNothing().when(tapeDriveCommandService).move(anyInt(), anyBoolean());

        doAnswer(o -> {
            String argFilePath = o.getArgument(0);
            JsonHandler.writeAsFile(new TapeCatalogLabel("DISCARDED_GUID", "FAKE_CODE"), new File(argFilePath));
            return null;
        })
            .when(tapeReadWriteService)
            .readFromTape(anyString());

        ReadWriteResult result = writeTask.get();

        // Then
        verify(tapeRobotPool, new Times(1)).checkoutRobotService();
        verify(tapeRobotService, new Times(1)).getLoadUnloadService();
        verify(tapeReadWriteService, new Times(1)).readFromTape(anyString());
        verify(tapeLoadUnloadService, new Times(1)).loadTape(anyInt(), anyInt());
        verify(tapeDriveCommandService, times(1)).rewind();

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(StatusCode.FATAL);
        assertThat(result.getOrderState()).isEqualTo(QueueState.READY);

        assertThat(result.getCurrentTape()).isNotNull();

        assertThat(result.getCurrentTape().getFileCount()).isEqualTo(0);
        assertThat(result.getCurrentTape().getType()).isEqualTo(tapeDriveState.getCartridge());
        assertThat(result.getCurrentTape().getTapeState()).isEqualTo(TapeState.CONFLICT);

        verifyNoMoreInteractions(archiveCacheStorage);
    }

    /**
     * Check label of tape catalog and loaded tape
     * Tape catalog declare a not empty label
     * when try to read label of loaded tape then error KO_ON_READ_LABEL
     */
    @Test
    public void test_current_tape_null_then_load_tape_check_label_read_label_error() throws Exception {
        // When
        when(tapeDriveService.getTapeDriveConf()).thenAnswer(o -> mock(TapeDriveConf.class));
        String file = getTarFileName();
        String filePath = FAKE_FILE_PATH + File.separator + file;

        // given
        WriteOrder writeOrder = new WriteOrder(
            FAKE_BUCKET,
            FAKE_FILE_BUCKET_ID,
            filePath,
            10L,
            FAKE_DIGEST,
            file,
            QueueMessageType.WriteOrder
        );

        WriteTask writeTask = new WriteTask(
            writeOrder,
            null,
            new TapeLibraryServiceImpl(tapeDriveService, tapeRobotPool, FULL_CARTRIDGE_THRESHOLD),
            tapeCatalogService,
            archiveReferentialRepository,
            archiveCacheStorage,
            inputTarDir.getAbsolutePath(),
            false
        );

        TapeCatalog tapeCatalog = getTapeCatalog(true, false, TapeState.OPEN);
        tapeCatalog.setCurrentLocation(new TapeLocation(1, TapeLocationType.SLOT));
        when(tapeCatalogService.receive(any())).thenReturn(Optional.of(tapeCatalog));

        doNothing().when(tapeLoadUnloadService).loadTape(anyInt(), anyInt());
        doNothing().when(tapeDriveCommandService).rewind();

        TapeDriveState tapeDriveState = new TapeDriveState();
        tapeDriveState.setCartridge("LTO-6");

        doNothing().when(tapeDriveCommandService).move(anyInt(), anyBoolean());

        doThrow(new TapeCommandException("error")).when(tapeReadWriteService).readFromTape(anyString());

        ReadWriteResult result = writeTask.get();

        // Then
        verify(tapeRobotPool, new Times(1)).checkoutRobotService();
        verify(tapeRobotService, new Times(1)).getLoadUnloadService();
        verify(tapeReadWriteService, new Times(1)).readFromTape(anyString());
        verify(tapeLoadUnloadService, new Times(1)).loadTape(anyInt(), anyInt());
        verify(tapeDriveCommandService, times(1)).rewind();

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(StatusCode.FATAL);
        assertThat(result.getOrderState()).isEqualTo(QueueState.READY);

        assertThat(result.getCurrentTape()).isNotNull();

        assertThat(result.getCurrentTape().getFileCount()).isEqualTo(0);
        assertThat(result.getCurrentTape().getType()).isEqualTo(tapeDriveState.getCartridge());
        assertThat(result.getCurrentTape().getTapeState()).isEqualTo(TapeState.OPEN);

        verifyNoMoreInteractions(archiveCacheStorage);
    }

    /**
     * Tape have an incident close => state conflict
     */
    @Test
    public void test_current_tape_conflict_state_then_unload_load_tape_with_empty_label_success() throws Exception {
        // When
        when(tapeDriveService.getTapeDriveConf()).thenAnswer(o -> mock(TapeDriveConf.class));
        String file = getTarFileName();
        String filePath = FAKE_FILE_PATH + File.separator + file;

        // given
        WriteOrder writeOrder = new WriteOrder(
            FAKE_BUCKET,
            FAKE_FILE_BUCKET_ID,
            filePath,
            10L,
            FAKE_DIGEST,
            file,
            QueueMessageType.WriteOrder
        );

        WriteTask writeTask = new WriteTask(
            writeOrder,
            getTapeCatalog(true, false, TapeState.CONFLICT),
            new TapeLibraryServiceImpl(tapeDriveService, tapeRobotPool, FULL_CARTRIDGE_THRESHOLD),
            tapeCatalogService,
            archiveReferentialRepository,
            archiveCacheStorage,
            inputTarDir.getAbsolutePath(),
            false
        );

        TapeCatalog tapeCatalog = getTapeCatalog(false, false, TapeState.EMPTY);
        tapeCatalog.setCurrentLocation(new TapeLocation(1, TapeLocationType.SLOT));
        when(tapeCatalogService.receive(any())).thenReturn(Optional.of(tapeCatalog));

        doNothing().when(tapeDriveCommandService).eject();
        doNothing().when(tapeLoadUnloadService).unloadTape(anyInt(), anyInt());

        doNothing().when(tapeLoadUnloadService).loadTape(anyInt(), anyInt());
        doNothing().when(tapeDriveCommandService).rewind();

        TapeDriveState tapeDriveState = new TapeDriveState();
        tapeDriveState.setCartridge("VIT0001");

        when(tapeDriveCommandService.status()).thenThrow(new TapeCommandException("error")).thenReturn(tapeDriveState);

        doThrow(new TapeCommandException("error"))
            .doNothing()
            .when(tapeDriveCommandService)
            .move(anyInt(), anyBoolean());

        // When Write Label and file to tape
        doNothing().when(tapeReadWriteService).writeToTape(contains(WriteTask.TAPE_LABEL));

        doNothing().when(tapeReadWriteService).writeToTape(eq(FAKE_FILE_PATH + "/" + writeOrder.getArchiveId()));

        ReadWriteResult result = writeTask.get();

        // Then
        verify(tapeRobotPool, new Times(2)).checkoutRobotService();
        verify(tapeRobotService, new Times(2)).getLoadUnloadService();
        verify(tapeDriveCommandService, new Times(2)).status();
        verify(tapeDriveCommandService, new Times(1)).move(anyInt(), anyBoolean());
        verify(tapeDriveCommandService, new Times(1)).eject();

        verify(tapeLoadUnloadService, new Times(1)).loadTape(anyInt(), anyInt());
        verify(tapeDriveCommandService, times(2)).rewind();
        verify(tapeLoadUnloadService, new Times(1)).unloadTape(anyInt(), anyInt());
        verify(tapeReadWriteService, new Times(1)).writeToTape(contains(WriteTask.TAPE_LABEL));
        verify(tapeReadWriteService, new Times(1)).writeToTape(eq(FAKE_FILE_PATH + "/" + writeOrder.getArchiveId()));

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(StatusCode.OK);
        assertThat(result.getOrderState()).isEqualTo(QueueState.COMPLETED);

        assertThat(result.getCurrentTape()).isNotNull();

        assertThat(result.getCurrentTape().getFileCount()).isEqualTo(2);
        assertThat(result.getCurrentTape().getType()).isEqualTo(tapeDriveState.getCartridge());
        assertThat(result.getCurrentTape().getTapeState()).isEqualTo(TapeState.OPEN);

        TapeCatalogLabel label = result.getCurrentTape().getLabel();

        assertThat(label).isNotNull();
        assertThat(label)
            .extracting("id", TapeCatalogLabel.CODE, TapeCatalogLabel.BUCKET, TapeCatalogLabel.TYPE)
            .contains(tapeCatalog.getId(), tapeCatalog.getCode(), tapeCatalog.getBucket(), tapeCatalog.getType());

        verify(archiveCacheStorage).reserveArchiveStorageSpace(FAKE_FILE_BUCKET_ID, file, 10L);
        verify(archiveCacheStorage).moveArchiveToCache(
            eq(inputTarDir.toPath().resolve(filePath)),
            eq(FAKE_FILE_BUCKET_ID),
            eq(file)
        );
    }

    /**
     * When drive try to write a file to the current tape, error occurs. Status command return that drive is at end of tape
     * Means that the tape is full.
     * Unload current tape and mark FULL
     * Find new tape and do stuff
     */
    @Test
    public void test_current_tape_end_of_tape_then_unload_load_tape_with_empty_label_success() throws Exception {
        // When
        when(tapeDriveService.getTapeDriveConf()).thenAnswer(o -> mock(TapeDriveConf.class));
        String file = getTarFileName();
        String filePath = FAKE_FILE_PATH + File.separator + file;

        // given
        WriteOrder writeOrder = new WriteOrder(
            FAKE_BUCKET,
            FAKE_FILE_BUCKET_ID,
            filePath,
            10L,
            FAKE_DIGEST,
            file,
            QueueMessageType.WriteOrder
        );

        TapeCatalog almostFullTapeCatalog = getTapeCatalog(true, false, TapeState.OPEN);
        almostFullTapeCatalog.setWrittenBytes(1_000_000_000L);

        WriteTask writeTask = new WriteTask(
            writeOrder,
            almostFullTapeCatalog,
            new TapeLibraryServiceImpl(tapeDriveService, tapeRobotPool, 900),
            tapeCatalogService,
            archiveReferentialRepository,
            archiveCacheStorage,
            inputTarDir.getAbsolutePath(),
            false
        );

        TapeCatalog tapeCatalog = getTapeCatalog(false, false, TapeState.EMPTY);
        tapeCatalog.setCurrentLocation(new TapeLocation(1, TapeLocationType.SLOT));
        when(tapeCatalogService.receive(any())).thenReturn(Optional.of(tapeCatalog));

        doNothing().when(tapeLoadUnloadService).unloadTape(anyInt(), anyInt());
        doNothing().when(tapeDriveCommandService).eject();

        doNothing().when(tapeLoadUnloadService).loadTape(anyInt(), anyInt());
        doNothing().when(tapeDriveCommandService).rewind();

        TapeDriveState fullTapeStatus = new TapeDriveState();
        fullTapeStatus.setCartridge(LTO_6);

        TapeDriveState tapeDriveState = new TapeDriveState();
        tapeDriveState.setCartridge(LTO_7);

        when(tapeDriveCommandService.status())
            .thenReturn(fullTapeStatus)
            .thenThrow(new TapeCommandException("error"))
            .thenReturn(tapeDriveState);

        // When Write Label and file to tape
        doNothing().when(tapeReadWriteService).writeToTape(contains(WriteTask.TAPE_LABEL));

        // First write response ko (end of tape) the second write should be a new tape
        doThrow(new TapeCommandException("error"))
            .doNothing()
            .when(tapeReadWriteService)
            .writeToTape(eq(FAKE_FILE_PATH + "/" + writeOrder.getArchiveId()));

        doThrow(new TapeCommandException("error")).when(tapeDriveCommandService).move(anyInt(), anyBoolean());

        ReadWriteResult result = writeTask.get();

        // Then
        verify(tapeRobotPool, new Times(2)).checkoutRobotService();
        verify(tapeRobotService, new Times(2)).getLoadUnloadService();
        verify(tapeDriveCommandService, new Times(3)).status();
        verify(tapeDriveCommandService, new Times(1)).eject();
        verify(tapeDriveCommandService, new Times(1)).move(anyInt(), anyBoolean());
        verify(tapeLoadUnloadService, new Times(1)).loadTape(anyInt(), anyInt());
        verify(tapeDriveCommandService, times(3)).rewind();
        verify(tapeLoadUnloadService, new Times(1)).unloadTape(anyInt(), anyInt());
        verify(tapeReadWriteService, new Times(1)).writeToTape(contains(WriteTask.TAPE_LABEL));
        verify(tapeReadWriteService, new Times(2)).writeToTape(eq(FAKE_FILE_PATH + "/" + writeOrder.getArchiveId()));

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(StatusCode.OK);
        assertThat(result.getOrderState()).isEqualTo(QueueState.COMPLETED);

        assertThat(result.getCurrentTape()).isNotNull();

        assertThat(result.getCurrentTape().getFileCount()).isEqualTo(2);
        assertThat(result.getCurrentTape().getType()).isEqualTo(tapeDriveState.getCartridge());
        assertThat(result.getCurrentTape().getTapeState()).isEqualTo(TapeState.OPEN);

        TapeCatalogLabel label = result.getCurrentTape().getLabel();

        assertThat(label).isNotNull();
        assertThat(label)
            .extracting("id", TapeCatalogLabel.CODE, TapeCatalogLabel.BUCKET, TapeCatalogLabel.TYPE)
            .contains(tapeCatalog.getId(), tapeCatalog.getCode(), tapeCatalog.getBucket(), tapeCatalog.getType());

        verify(archiveCacheStorage).reserveArchiveStorageSpace(FAKE_FILE_BUCKET_ID, file, 10L);
        verify(archiveCacheStorage).moveArchiveToCache(
            eq(inputTarDir.toPath().resolve(filePath)),
            eq(FAKE_FILE_BUCKET_ID),
            eq(file)
        );
    }

    @Test
    public void test_write_to_tape_ko_try_with_next_tape_success() throws Exception {
        // When
        when(tapeDriveService.getTapeDriveConf()).thenAnswer(o -> mock(TapeDriveConf.class));
        String file = getTarFileName();
        String filePath = FAKE_FILE_PATH + File.separator + file;

        // given
        WriteOrder writeOrder = new WriteOrder(
            FAKE_BUCKET,
            FAKE_FILE_BUCKET_ID,
            filePath,
            10L,
            FAKE_DIGEST,
            file,
            QueueMessageType.WriteOrder
        );

        TapeCatalog writeKoTapeCatalog = getTapeCatalog(true, false, TapeState.OPEN);
        writeKoTapeCatalog.setWrittenBytes(1_000L);

        WriteTask writeTask = new WriteTask(
            writeOrder,
            writeKoTapeCatalog,
            new TapeLibraryServiceImpl(tapeDriveService, tapeRobotPool, 900),
            tapeCatalogService,
            archiveReferentialRepository,
            archiveCacheStorage,
            inputTarDir.getAbsolutePath(),
            false
        );

        TapeCatalog tapeCatalog = getTapeCatalog(false, false, TapeState.EMPTY);
        when(tapeCatalogService.receive(any())).thenReturn(Optional.of(tapeCatalog));

        doNothing().when(tapeLoadUnloadService).unloadTape(anyInt(), anyInt());
        doNothing().when(tapeDriveCommandService).eject();

        doNothing().when(tapeLoadUnloadService).loadTape(anyInt(), anyInt());
        doNothing().when(tapeDriveCommandService).rewind();

        // When Write Label and file to tape
        doNothing().when(tapeReadWriteService).writeToTape(contains(WriteTask.TAPE_LABEL));

        TapeDriveState koTapeStatus = new TapeDriveState();
        koTapeStatus.setCartridge(LTO_6);

        TapeDriveState tapeDriveState = new TapeDriveState();
        tapeDriveState.setCartridge(LTO_7);

        when(tapeDriveCommandService.status()).thenReturn(koTapeStatus).thenReturn(tapeDriveState);

        // First write response ko (corrupted tape) the second write should be a new tape
        doThrow(new TapeCommandException("error"))
            .doNothing()
            .when(tapeReadWriteService)
            .writeToTape(eq(FAKE_FILE_PATH + "/" + writeOrder.getArchiveId()));

        doThrow(new TapeCommandException("error")).when(tapeDriveCommandService).move(anyInt(), anyBoolean());

        ReadWriteResult result = writeTask.get();

        // Then
        verify(tapeDriveCommandService, new Times(2)).status();
        verify(tapeDriveCommandService, new Times(1)).eject();
        verify(tapeDriveCommandService, new Times(1)).move(anyInt(), anyBoolean());
        verify(tapeLoadUnloadService, new Times(1)).loadTape(anyInt(), anyInt());
        verify(tapeDriveCommandService, times(3)).rewind();
        verify(tapeLoadUnloadService, new Times(1)).unloadTape(anyInt(), anyInt());
        verify(tapeReadWriteService, new Times(2)).writeToTape(eq(FAKE_FILE_PATH + "/" + writeOrder.getArchiveId()));
        verify(tapeReadWriteService, new Times(1)).writeToTape(contains(WriteTask.TAPE_LABEL));

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(StatusCode.OK);
        assertThat(result.getOrderState()).isEqualTo(QueueState.COMPLETED);

        assertThat(result.getCurrentTape()).isNotNull();

        assertThat(result.getCurrentTape().getFileCount()).isEqualTo(2);
        assertThat(result.getCurrentTape().getType()).isEqualTo(tapeDriveState.getCartridge());
        assertThat(result.getCurrentTape().getTapeState()).isEqualTo(TapeState.OPEN);

        TapeCatalogLabel label = result.getCurrentTape().getLabel();

        assertThat(label).isNotNull();
        assertThat(label)
            .extracting("id", TapeCatalogLabel.CODE, TapeCatalogLabel.BUCKET, TapeCatalogLabel.TYPE)
            .contains(tapeCatalog.getId(), tapeCatalog.getCode(), tapeCatalog.getBucket(), tapeCatalog.getType());

        verify(archiveCacheStorage).reserveArchiveStorageSpace(FAKE_FILE_BUCKET_ID, file, 10L);
        verify(archiveCacheStorage).moveArchiveToCache(
            eq(inputTarDir.toPath().resolve(filePath)),
            eq(FAKE_FILE_BUCKET_ID),
            eq(file)
        );
    }

    //***************************************
    // KO
    //***************************************

    @Test
    public void test_write_to_tape_ko_try_with_next_tape_ko() throws Exception {
        // When
        when(tapeDriveService.getTapeDriveConf()).thenAnswer(o -> mock(TapeDriveConf.class));
        String file = getTarFileName();
        String filePath = FAKE_FILE_PATH + File.separator + file;

        // given
        WriteOrder writeOrder = new WriteOrder(
            FAKE_BUCKET,
            FAKE_FILE_BUCKET_ID,
            filePath,
            10L,
            FAKE_DIGEST,
            file,
            QueueMessageType.WriteOrder
        );

        TapeCatalog koTapeCatalog1 = getTapeCatalog(true, false, TapeState.OPEN);
        koTapeCatalog1.setWrittenBytes(1_000_000L);
        koTapeCatalog1.setType(LTO_6);

        WriteTask writeTask = new WriteTask(
            writeOrder,
            koTapeCatalog1,
            new TapeLibraryServiceImpl(tapeDriveService, tapeRobotPool, 900),
            tapeCatalogService,
            archiveReferentialRepository,
            archiveCacheStorage,
            inputTarDir.getAbsolutePath(),
            false
        );

        TapeCatalog tapeCatalog = getTapeCatalog(false, false, TapeState.EMPTY);
        koTapeCatalog1.setWrittenBytes(0L);
        tapeCatalog.setType(LTO_7);
        when(tapeCatalogService.receive(any())).thenReturn(Optional.of(tapeCatalog));

        doNothing().when(tapeLoadUnloadService).unloadTape(anyInt(), anyInt());
        doNothing().when(tapeDriveCommandService).eject();

        doNothing().when(tapeLoadUnloadService).loadTape(anyInt(), anyInt());
        doNothing().when(tapeDriveCommandService).rewind();

        // When Write Label and file to tape
        doNothing().when(tapeReadWriteService).writeToTape(contains(WriteTask.TAPE_LABEL));

        TapeDriveState tape1KoStatus = new TapeDriveState();
        tape1KoStatus.setCartridge(LTO_6);

        TapeDriveState tape2Status = new TapeDriveState();
        tape2Status.setCartridge(LTO_7);

        when(tapeDriveCommandService.status()).thenReturn(tape1KoStatus).thenReturn(tape2Status);

        // First write response ko (corrupted) the second write should be a new tape
        doThrow(new TapeCommandException("error"))
            .doThrow(new TapeCommandException("error"))
            .when(tapeReadWriteService)
            .writeToTape(eq(FAKE_FILE_PATH + "/" + writeOrder.getArchiveId()));

        doThrow(new TapeCommandException("error")).when(tapeDriveCommandService).move(anyInt(), anyBoolean());

        ReadWriteResult result = writeTask.get();

        // Then
        verify(tapeDriveCommandService, new Times(3)).status();
        verify(tapeDriveCommandService, new Times(1)).eject();
        verify(tapeDriveCommandService, new Times(1)).move(anyInt(), anyBoolean());
        verify(tapeLoadUnloadService, new Times(1)).loadTape(anyInt(), anyInt());
        verify(tapeDriveCommandService, times(3)).rewind();
        verify(tapeLoadUnloadService, new Times(1)).unloadTape(anyInt(), anyInt());
        verify(tapeReadWriteService, new Times(1)).writeToTape(contains(WriteTask.TAPE_LABEL));
        verify(tapeReadWriteService, new Times(2)).writeToTape(eq(FAKE_FILE_PATH + "/" + writeOrder.getArchiveId()));

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(StatusCode.FATAL);
        assertThat(result.getOrderState()).isEqualTo(QueueState.READY);

        assertThat(result.getCurrentTape()).isNotNull();

        verifyNoMoreInteractions(archiveCacheStorage);
    }

    @Test
    public void test_write_to_tape_ko_on_db_persist_tape_back_to_catalog_fatal() throws Exception {
        // When
        when(tapeDriveService.getTapeDriveConf()).thenAnswer(o -> mock(TapeDriveConf.class));
        String file = getTarFileName();
        String filePath = FAKE_FILE_PATH + File.separator + file;

        // given
        WriteOrder writeOrder = new WriteOrder(
            FAKE_BUCKET,
            FAKE_FILE_BUCKET_ID,
            filePath,
            10L,
            FAKE_DIGEST,
            file,
            QueueMessageType.WriteOrder
        );

        WriteTask writeTask = new WriteTask(
            writeOrder,
            getTapeCatalog(true, false, TapeState.CONFLICT),
            new TapeLibraryServiceImpl(tapeDriveService, tapeRobotPool, FULL_CARTRIDGE_THRESHOLD),
            tapeCatalogService,
            archiveReferentialRepository,
            archiveCacheStorage,
            inputTarDir.getAbsolutePath(),
            false
        );

        when(tapeCatalogService.update(any(), anyMap()))
            .thenThrow(new TapeCatalogException(""))
            .thenThrow(new TapeCatalogException(""))
            .thenThrow(new TapeCatalogException(""));

        doNothing().when(tapeLoadUnloadService).unloadTape(anyInt(), anyInt());
        doNothing().when(tapeDriveCommandService).eject();

        ReadWriteResult result = writeTask.get();

        // Then
        verify(tapeCatalogService, new Times(3)).update(any(), anyMap());
        verify(tapeLoadUnloadService, new Times(1)).unloadTape(anyInt(), anyInt());
        verify(tapeDriveCommandService, new Times(1)).eject();

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(StatusCode.FATAL);
        assertThat(result.getOrderState()).isEqualTo(QueueState.READY);

        assertThat(result.getCurrentTape()).isNotNull();

        verifyNoMoreInteractions(archiveCacheStorage);
    }

    @Test
    public void test_writeOrder_file_not_found() throws Exception {
        // When
        when(tapeDriveService.getTapeDriveConf()).thenAnswer(o -> mock(TapeDriveConf.class));
        String file = getTarFileName();
        WriteOrder writeOrder = new WriteOrder(
            FAKE_BUCKET,
            FAKE_FILE_BUCKET_ID,
            FAKE_FILE_PATH,
            10L,
            FAKE_DIGEST,
            file,
            QueueMessageType.WriteOrder
        );

        WriteTask writeTask = new WriteTask(
            writeOrder,
            null,
            new TapeLibraryServiceImpl(tapeDriveService, tapeRobotPool, FULL_CARTRIDGE_THRESHOLD),
            tapeCatalogService,
            archiveReferentialRepository,
            archiveCacheStorage,
            inputTarDir.getAbsolutePath(),
            false
        );

        ReadWriteResult result = writeTask.get();

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(StatusCode.FATAL);
        assertThat(result.getOrderState()).isEqualTo(QueueState.ERROR);
        assertThat(result.getCurrentTape()).isNull();

        verifyNoMoreInteractions(archiveCacheStorage);
    }

    @Test
    public void test_write_when_tape_is_wrong_then_eject_tape_ko() throws Exception {
        // When
        when(tapeDriveService.getTapeDriveConf()).thenAnswer(o -> mock(TapeDriveConf.class));
        String file = getTarFileName();
        String filePath = FAKE_FILE_PATH + File.separator + file;

        // given
        WriteOrder writeOrder = new WriteOrder(
            FAKE_BUCKET,
            FAKE_FILE_BUCKET_ID,
            filePath,
            10L,
            FAKE_DIGEST,
            file,
            QueueMessageType.WriteOrder
        );

        TapeCatalog tape = getTapeCatalog(true, false, TapeState.OPEN);
        tape.setBucket("wrongBucket");

        WriteTask writeTask = new WriteTask(
            writeOrder,
            tape,
            new TapeLibraryServiceImpl(tapeDriveService, tapeRobotPool, FULL_CARTRIDGE_THRESHOLD),
            tapeCatalogService,
            archiveReferentialRepository,
            archiveCacheStorage,
            inputTarDir.getAbsolutePath(),
            false
        );

        doThrow(new TapeCommandException("error")).when(tapeDriveCommandService).eject();

        ReadWriteResult result = writeTask.get();

        // Then
        verify(tapeDriveCommandService, new Times(1)).eject();
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(StatusCode.FATAL);
        assertThat(result.getOrderState()).isEqualTo(QueueState.READY);

        verifyNoMoreInteractions(archiveCacheStorage);
    }

    @Test
    public void test_write_file_with_cache_capacity_exceeded() throws Exception {
        // Given
        when(tapeDriveService.getTapeDriveConf()).thenAnswer(o -> mock(TapeDriveConf.class));
        String file = getTarFileName();
        String filePath = FAKE_FILE_PATH + File.separator + file;

        WriteOrder writeOrder = new WriteOrder(
            FAKE_BUCKET,
            FAKE_FILE_BUCKET_ID,
            filePath,
            10L,
            FAKE_DIGEST,
            file,
            QueueMessageType.WriteOrder
        );

        TapeCatalog tape = getTapeCatalog(true, false, TapeState.OPEN);
        tape.setCurrentPosition(2);
        tape.setFileCount(2);

        WriteTask writeTask = new WriteTask(
            writeOrder,
            tape,
            new TapeLibraryServiceImpl(tapeDriveService, tapeRobotPool, FULL_CARTRIDGE_THRESHOLD),
            tapeCatalogService,
            archiveReferentialRepository,
            archiveCacheStorage,
            inputTarDir.getAbsolutePath(),
            false
        );

        doNothing().when(tapeReadWriteService).writeToTape(eq(FAKE_FILE_PATH + "/" + writeOrder.getArchiveId()));

        doThrow(IllegalStateException.class)
            .when(archiveCacheStorage)
            .reserveArchiveStorageSpace(FAKE_FILE_BUCKET_ID, file, 10L);

        // When
        ReadWriteResult result = writeTask.get();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(StatusCode.FATAL);
        assertThat(result.getOrderState()).isEqualTo(QueueState.ERROR);

        verify(tapeDriveCommandService, never()).status();
        verify(tapeDriveCommandService, never()).eject();
        verify(tapeDriveCommandService, never()).move(anyInt(), anyBoolean());
        verify(tapeLoadUnloadService, never()).loadTape(anyInt(), anyInt());
        verify(tapeDriveCommandService, never()).rewind();
        verify(tapeLoadUnloadService, never()).unloadTape(anyInt(), anyInt());

        verify(tapeReadWriteService).writeToTape(eq(FAKE_FILE_PATH + "/" + writeOrder.getArchiveId()));

        assertThat(result.getCurrentTape()).isNotNull();

        assertThat(result.getCurrentTape().getFileCount()).isEqualTo(3);
        assertThat(result.getCurrentTape().getCurrentPosition()).isEqualTo(3);
        assertThat(result.getCurrentTape().getTapeState()).isEqualTo(TapeState.OPEN);

        verify(archiveCacheStorage).reserveArchiveStorageSpace(FAKE_FILE_BUCKET_ID, file, 10L);
        verifyNoMoreInteractions(archiveCacheStorage);
    }

    private String getTarFileName() throws IOException {
        Path parentDir = inputTarDir.toPath().resolve(FAKE_FILE_PATH);
        Files.createDirectories(parentDir);
        String filename = GUIDFactory.newGUID().getId() + ".tar";
        Files.createFile(parentDir.resolve(filename));
        return filename;
    }

    private TapeCatalog getTapeCatalog(boolean withLabel, boolean isWorm, TapeState tapeState) {
        TapeCatalog tapeCatalog = new TapeCatalog()
            .setLibrary(FAKE_LIBRARY)
            .setBucket(FAKE_BUCKET)
            .setType(LTO_6)
            .setCode(TAPE_CODE)
            .setWorm(isWorm)
            .setTapeState(tapeState);

        if (withLabel) {
            TapeCatalogLabel objLabel = new TapeCatalogLabel(tapeCatalog.getId(), tapeCatalog.getCode());
            objLabel.setAlternativeCode(tapeCatalog.getAlternativeCode());
            objLabel.setBucket(tapeCatalog.getBucket());
            objLabel.setType(tapeCatalog.getType());

            tapeCatalog.setLabel(objLabel);
        }

        tapeCatalog.setPreviousLocation(new TapeLocation(1, TapeLocationType.SLOT));
        tapeCatalog.setCurrentLocation(new TapeLocation(1, TapeLocationType.SLOT));
        return tapeCatalog;
    }
}
