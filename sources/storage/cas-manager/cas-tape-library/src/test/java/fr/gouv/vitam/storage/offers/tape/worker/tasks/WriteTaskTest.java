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
import fr.gouv.vitam.storage.offers.tape.cas.ArchiveReferentialRepository;
import fr.gouv.vitam.storage.offers.tape.dto.TapeDriveState;
import fr.gouv.vitam.storage.offers.tape.dto.TapeDriveStatus;
import fr.gouv.vitam.storage.offers.tape.dto.TapeResponse;
import fr.gouv.vitam.storage.offers.tape.exception.TapeCatalogException;
import fr.gouv.vitam.storage.offers.tape.impl.readwrite.TapeLibraryServiceImpl;
import fr.gouv.vitam.storage.offers.tape.spec.TapeCatalogService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeDriveCommandService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeDriveService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeLoadUnloadService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeReadWriteService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeRobotPool;
import fr.gouv.vitam.storage.offers.tape.spec.TapeRobotService;
import fr.gouv.vitam.storage.offers.tape.utils.LocalFileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.internal.verification.Times;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WriteTaskTest {

    public static final String FAKE_BUCKET = "fakeBucket";
    public static final String FAKE_LIBRARY = "fakeLibrary";
    public static final String LTO_6 = "LTO-6";
    public static final String TAPE_CODE = "VIT0001";
    private static final String FAKE_FILE_PATH = "fakeFilePath";
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

    @Before
    public void setUp() throws Exception {

        reset(tapeRobotPool);
        reset(tapeRobotService);
        reset(tapeDriveService);
        reset(tapeCatalogService);
        reset(tapeReadWriteService);
        reset(tapeDriveCommandService);
        reset(tapeLoadUnloadService);
        reset(archiveReferentialRepository);

        when(tapeDriveService.getReadWriteService(eq(TapeDriveService.ReadWriteCmd.DD)))
            .thenAnswer(o -> tapeReadWriteService);
        when(tapeDriveService.getDriveCommandService()).thenAnswer(o -> tapeDriveCommandService);
        when(tapeRobotPool.checkoutRobotService()).thenAnswer(o -> tapeRobotService);
        when(tapeRobotService.getLoadUnloadService()).thenAnswer(o -> tapeLoadUnloadService);

        when(tapeReadWriteService.getOutputDirectory()).thenReturn("/tmp");
        when(tapeReadWriteService.getInputDirectory()).thenReturn("/tmp");
    }

    @Test
    public void test_write_task_constructor() {
        // When
        when(tapeDriveService.getTapeDriveConf()).thenAnswer(o -> mock(TapeDriveConf.class));

        // Test constructors
        try {
            new WriteTask(mock(WriteOrder.class), null, null, tapeCatalogService,
                archiveReferentialRepository, null, false);
            fail("should fail tapeLibraryService is required");
        } catch (IllegalArgumentException e) {
        }

        try {
            new WriteTask(mock(WriteOrder.class), null, new TapeLibraryServiceImpl(tapeDriveService, tapeRobotPool),
                null, archiveReferentialRepository,
                null, false);
            fail("should fail tapeCatalogService is required");
        } catch (IllegalArgumentException e) {
        }

        try {
            new WriteTask(mock(WriteOrder.class), null, new TapeLibraryServiceImpl(tapeDriveService, tapeRobotPool),
                tapeCatalogService, null,
                null, false);
            fail("should fail archiveReferentialRepository is required");
        } catch (IllegalArgumentException e) {
        }

        try {
            new WriteTask(mock(WriteOrder.class), null, new TapeLibraryServiceImpl(tapeDriveService, tapeRobotPool),
                tapeCatalogService,
                archiveReferentialRepository, null, false);
        } catch (IllegalArgumentException e) {
            fail("should not throw exception");
        }

        try {
            new WriteTask(mock(WriteOrder.class), null, new TapeLibraryServiceImpl(tapeDriveService, tapeRobotPool),
                tapeCatalogService,
                archiveReferentialRepository, "", false);
        } catch (IllegalArgumentException e) {
            fail("should not throw exception");
        }
    }

    @Test
    public void test_current_tape_not_null_and_empty_label_success() throws Exception {
        // When
        when(tapeDriveService.getTapeDriveConf()).thenAnswer(o -> mock(TapeDriveConf.class));
        String file = getTarFileName();

        // Given WriterOrder and TapeCatalog
        WriteOrder writeOrder =
            new WriteOrder().setBucket(FAKE_BUCKET).setFilePath(FAKE_FILE_PATH).setArchiveId(file).setSize(10l);

        TapeCatalog tapeCatalog = getTapeCatalog(false, false, TapeState.EMPTY);

        // When write to tape (label and file)
        when(tapeReadWriteService.writeToTape(contains(WriteTask.TAPE_LABEL)))
            .thenReturn(new TapeResponse(StatusCode.OK));
        when(tapeReadWriteService.writeToTape(eq(FAKE_FILE_PATH + "/" + writeOrder.getArchiveId())))
            .thenReturn(new TapeResponse(StatusCode.OK));

        WriteTask writeTask =
            new WriteTask(writeOrder, tapeCatalog, new TapeLibraryServiceImpl(tapeDriveService, tapeRobotPool),
                tapeCatalogService, archiveReferentialRepository, "/tmp",
                false);

        ReadWriteResult result = writeTask.get();

        // Then
        verify(tapeReadWriteService, new Times(1)).writeToTape(contains(WriteTask.TAPE_LABEL));
        verify(tapeReadWriteService, new Times(1)).writeToTape(eq(FAKE_FILE_PATH + "/" + writeOrder.getArchiveId()));

        ArgumentCaptor<String> fileName = ArgumentCaptor.forClass(String.class);

        verify(tapeReadWriteService, new Times(2)).writeToTape(fileName.capture());

        assertThat(fileName.getAllValues()).hasSize(2);

        assertThat(fileName.getAllValues().get(0)).contains(LocalFileUtils.INPUT_TAR_TMP_FOLDER).contains(WriteTask.TAPE_LABEL);
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
    }

    @Test
    public void test_current_tape_not_null_and_have_label_success() throws Exception {
        // When
        when(tapeDriveService.getTapeDriveConf()).thenAnswer(o -> mock(TapeDriveConf.class));
        String file = getTarFileName();

        WriteOrder writeOrder =
            new WriteOrder().setBucket(FAKE_BUCKET).setFilePath(FAKE_FILE_PATH).setArchiveId(file).setSize(10l);

        TapeCatalog tapeCatalog = getTapeCatalog(true, false, TapeState.EMPTY);

        when(tapeReadWriteService.writeToTape(eq(FAKE_FILE_PATH + "/" + writeOrder.getArchiveId())))
            .thenReturn(new TapeResponse(StatusCode.OK));

        WriteTask writeTask =
            new WriteTask(writeOrder, tapeCatalog, new TapeLibraryServiceImpl(tapeDriveService, tapeRobotPool),
                tapeCatalogService, archiveReferentialRepository, "/tmp",
                false);

        ReadWriteResult result = writeTask.get();

        verify(tapeReadWriteService, new Times(0)).writeToTape(contains(WriteTask.TAPE_LABEL));
        verify(tapeReadWriteService, new Times(1)).writeToTape(eq(FAKE_FILE_PATH + "/" + writeOrder.getArchiveId()));



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
    }

    @Test
    public void test_current_tape_null_then_load_tape_with_empty_label_success()
        throws Exception {
        // When
        when(tapeDriveService.getTapeDriveConf()).thenAnswer(o -> mock(TapeDriveConf.class));
        String file = getTarFileName();

        // given
        WriteOrder writeOrder =
            new WriteOrder().setBucket(FAKE_BUCKET).setFilePath(FAKE_FILE_PATH).setArchiveId(file).setSize(10l);

        WriteTask writeTask =
            new WriteTask(writeOrder, null, new TapeLibraryServiceImpl(tapeDriveService, tapeRobotPool),
                tapeCatalogService,
                archiveReferentialRepository, "/tmp", false);

        TapeCatalog tapeCatalog = getTapeCatalog(false, false, TapeState.EMPTY);
        tapeCatalog.setCurrentLocation(new TapeLocation(1, TapeLocationType.SLOT));
        when(tapeCatalogService.receive(any(), eq(QueueMessageType.TapeCatalog))).thenReturn(
            Optional.of(tapeCatalog));

        when(tapeLoadUnloadService.loadTape(anyInt(), anyInt())).thenReturn(new TapeResponse(StatusCode.OK));

        TapeDriveState tapeDriveState = new TapeDriveState(StatusCode.OK);
        tapeDriveState.setCartridge("LTO-6");

        when(tapeDriveCommandService.status())
            .thenReturn(new TapeDriveState(JsonHandler.createObjectNode(), StatusCode.KO))
            .thenReturn(tapeDriveState);


        // When Write Label and file to tape
        when(tapeReadWriteService.writeToTape(contains(WriteTask.TAPE_LABEL)))
            .thenReturn(new TapeResponse(StatusCode.OK));

        when(tapeReadWriteService.writeToTape(eq(FAKE_FILE_PATH + "/" + writeOrder.getArchiveId())))
            .thenReturn(new TapeResponse(StatusCode.OK));

        when(tapeDriveCommandService.move(anyInt(), anyBoolean()))
            .thenReturn(new TapeResponse(JsonHandler.createObjectNode(), StatusCode.KO))
            .thenReturn(new TapeResponse(StatusCode.OK));

        ReadWriteResult result = writeTask.get();

        // Then
        verify(tapeRobotPool, new Times(1)).checkoutRobotService();
        verify(tapeRobotService, new Times(1)).getLoadUnloadService();
        verify(tapeDriveCommandService, new Times(2)).status();
        verify(tapeDriveCommandService, new Times(1)).move(anyInt(), anyBoolean());
        verify(tapeLoadUnloadService, new Times(1)).loadTape(anyInt(), anyInt());
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

        // given
        WriteOrder writeOrder =
            new WriteOrder().setBucket(FAKE_BUCKET).setFilePath(FAKE_FILE_PATH).setArchiveId(file).setSize(10l);

        WriteTask writeTask =
            new WriteTask(writeOrder, null, new TapeLibraryServiceImpl(tapeDriveService, tapeRobotPool),
                tapeCatalogService,
                archiveReferentialRepository, "/tmp", false);

        TapeCatalog tapeCatalog = getTapeCatalog(false, false, TapeState.EMPTY);
        tapeCatalog.setCurrentLocation(new TapeLocation(1, TapeLocationType.SLOT));
        when(tapeCatalogService.receive(any(), eq(QueueMessageType.TapeCatalog))).thenReturn(
            Optional.of(tapeCatalog));

        when(tapeLoadUnloadService.loadTape(anyInt(), anyInt())).thenReturn(new TapeResponse(StatusCode.OK));

        TapeDriveState tapeDriveState = new TapeDriveState(StatusCode.OK);
        tapeDriveState.setCartridge("LTO-6");

        when(tapeDriveCommandService.move(anyInt(), anyBoolean()))
            .thenReturn(new TapeResponse(StatusCode.OK));

        ReadWriteResult result = writeTask.get();

        // Then
        verify(tapeRobotPool, new Times(1)).checkoutRobotService();
        verify(tapeRobotService, new Times(1)).getLoadUnloadService();
        verify(tapeDriveCommandService, new Times(1)).move(anyInt(), anyBoolean());
        verify(tapeLoadUnloadService, new Times(1)).loadTape(anyInt(), anyInt());

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(StatusCode.FATAL);
        assertThat(result.getOrderState()).isEqualTo(QueueState.READY);

        assertThat(result.getCurrentTape()).isNotNull();

        assertThat(result.getCurrentTape().getFileCount()).isEqualTo(0);
        assertThat(result.getCurrentTape().getType()).isEqualTo(tapeDriveState.getCartridge());
        assertThat(result.getCurrentTape().getTapeState()).isEqualTo(TapeState.CONFLICT);

        TapeCatalogLabel label = result.getCurrentTape().getLabel();

        assertThat(label).isNull();
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

        // given
        WriteOrder writeOrder =
            new WriteOrder().setBucket(FAKE_BUCKET).setFilePath(FAKE_FILE_PATH).setArchiveId(file).setSize(10l);

        WriteTask writeTask =
            new WriteTask(writeOrder, null, new TapeLibraryServiceImpl(tapeDriveService, tapeRobotPool),
                tapeCatalogService,
                archiveReferentialRepository, "/tmp", true);

        TapeCatalog tapeCatalog = getTapeCatalog(false, false, TapeState.EMPTY);
        tapeCatalog.setCurrentLocation(new TapeLocation(1, TapeLocationType.SLOT));
        when(tapeCatalogService.receive(any(), eq(QueueMessageType.TapeCatalog))).thenReturn(
            Optional.of(tapeCatalog));

        when(tapeLoadUnloadService.loadTape(anyInt(), anyInt())).thenReturn(new TapeResponse(StatusCode.OK));

        TapeDriveState tapeDriveState = new TapeDriveState(StatusCode.OK);
        tapeDriveState.setCartridge("LTO-6");

        when(tapeDriveCommandService.status())
            .thenReturn(new TapeDriveState(JsonHandler.createObjectNode(), StatusCode.KO))
            .thenReturn(tapeDriveState);


        // When Write Label and file to tape
        when(tapeReadWriteService.writeToTape(contains(WriteTask.TAPE_LABEL)))
            .thenReturn(new TapeResponse(StatusCode.OK));

        when(tapeReadWriteService.writeToTape(eq(FAKE_FILE_PATH + "/" + writeOrder.getArchiveId())))
            .thenReturn(new TapeResponse(StatusCode.OK));
        when(tapeDriveCommandService.rewind())
            .thenReturn(new TapeResponse(StatusCode.OK));

        when(tapeDriveCommandService.move(anyInt(), anyBoolean()))
            .thenReturn(new TapeResponse(StatusCode.OK));

        ReadWriteResult result = writeTask.get();

        // Then
        verify(tapeRobotPool, new Times(1)).checkoutRobotService();
        verify(tapeRobotService, new Times(1)).getLoadUnloadService();
        verify(tapeDriveCommandService, new Times(2)).status();
        verify(tapeDriveCommandService, new Times(1)).move(anyInt(), anyBoolean());
        verify(tapeLoadUnloadService, new Times(1)).loadTape(anyInt(), anyInt());
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
    }


    /**
     * Check label of tape catalog and loaded tape
     * Tape catalog declare a not empty label but different from the label of loaded tape
     */
    @Test
    public void test_current_tape_null_then_load_tape_check_label_discord_error()
        throws Exception {
        // When
        when(tapeDriveService.getTapeDriveConf()).thenAnswer(o -> mock(TapeDriveConf.class));
        String file = getTarFileName();

        // given
        WriteOrder writeOrder =
            new WriteOrder().setBucket(FAKE_BUCKET).setFilePath(FAKE_FILE_PATH).setArchiveId(file).setSize(10l);

        WriteTask writeTask =
            new WriteTask(writeOrder, null, new TapeLibraryServiceImpl(tapeDriveService, tapeRobotPool),
                tapeCatalogService,
                archiveReferentialRepository, "/tmp", false);

        TapeCatalog tapeCatalog = getTapeCatalog(true, false, TapeState.OPEN);
        tapeCatalog.setCurrentLocation(new TapeLocation(1, TapeLocationType.SLOT));
        when(tapeCatalogService.receive(any(), eq(QueueMessageType.TapeCatalog))).thenReturn(
            Optional.of(tapeCatalog));

        when(tapeLoadUnloadService.loadTape(anyInt(), anyInt())).thenReturn(new TapeResponse(StatusCode.OK));

        TapeDriveState tapeDriveState = new TapeDriveState(StatusCode.OK);
        tapeDriveState.setCartridge("LTO-6");

        when(tapeDriveCommandService.move(anyInt(), anyBoolean()))
            .thenReturn(new TapeResponse(StatusCode.OK));

        when(tapeReadWriteService.readFromTape(anyString())).thenAnswer(o -> {
            String filePath = o.getArgument(0);
            JsonHandler.writeAsFile(new TapeCatalogLabel("DISCARDED_GUID", "FAKE_CODE"), new File(filePath));
            return new TapeResponse(JsonHandler.createObjectNode(), StatusCode.OK);
        });

        ReadWriteResult result = writeTask.get();

        // Then
        verify(tapeRobotPool, new Times(1)).checkoutRobotService();
        verify(tapeRobotService, new Times(1)).getLoadUnloadService();
        verify(tapeReadWriteService, new Times(1)).readFromTape(anyString());
        verify(tapeLoadUnloadService, new Times(1)).loadTape(anyInt(), anyInt());

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(StatusCode.FATAL);
        assertThat(result.getOrderState()).isEqualTo(QueueState.READY);

        assertThat(result.getCurrentTape()).isNotNull();

        assertThat(result.getCurrentTape().getFileCount()).isEqualTo(0);
        assertThat(result.getCurrentTape().getType()).isEqualTo(tapeDriveState.getCartridge());
        assertThat(result.getCurrentTape().getTapeState()).isEqualTo(TapeState.CONFLICT);
    }

    /**
     * Check label of tape catalog and loaded tape
     * Tape catalog declare a not empty label
     * when try to read label of loaded tape then error KO_ON_READ_LABEL
     */
    @Test
    public void test_current_tape_null_then_load_tape_check_label_read_label_error()
        throws Exception {
        // When
        when(tapeDriveService.getTapeDriveConf()).thenAnswer(o -> mock(TapeDriveConf.class));
        String file = getTarFileName();

        // given
        WriteOrder writeOrder =
            new WriteOrder().setBucket(FAKE_BUCKET).setFilePath(FAKE_FILE_PATH).setArchiveId(file).setSize(10l);

        WriteTask writeTask =
            new WriteTask(writeOrder, null, new TapeLibraryServiceImpl(tapeDriveService, tapeRobotPool),
                tapeCatalogService,
                archiveReferentialRepository, "/tmp", false);

        TapeCatalog tapeCatalog = getTapeCatalog(true, false, TapeState.OPEN);
        tapeCatalog.setCurrentLocation(new TapeLocation(1, TapeLocationType.SLOT));
        when(tapeCatalogService.receive(any(), eq(QueueMessageType.TapeCatalog))).thenReturn(
            Optional.of(tapeCatalog));

        when(tapeLoadUnloadService.loadTape(anyInt(), anyInt())).thenReturn(new TapeResponse(StatusCode.OK));

        TapeDriveState tapeDriveState = new TapeDriveState(StatusCode.OK);
        tapeDriveState.setCartridge("LTO-6");

        when(tapeDriveCommandService.move(anyInt(), anyBoolean()))
            .thenReturn(new TapeResponse(StatusCode.OK));

        when(tapeReadWriteService.readFromTape(anyString()))
            .thenReturn(new TapeResponse(JsonHandler.createObjectNode(), StatusCode.KO));

        ReadWriteResult result = writeTask.get();

        // Then
        verify(tapeRobotPool, new Times(1)).checkoutRobotService();
        verify(tapeRobotService, new Times(1)).getLoadUnloadService();
        verify(tapeReadWriteService, new Times(1)).readFromTape(anyString());
        verify(tapeLoadUnloadService, new Times(1)).loadTape(anyInt(), anyInt());

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(StatusCode.FATAL);
        assertThat(result.getOrderState()).isEqualTo(QueueState.READY);

        assertThat(result.getCurrentTape()).isNotNull();

        assertThat(result.getCurrentTape().getFileCount()).isEqualTo(0);
        assertThat(result.getCurrentTape().getType()).isEqualTo(tapeDriveState.getCartridge());
        assertThat(result.getCurrentTape().getTapeState()).isEqualTo(TapeState.OPEN);
    }

    /**
     * Tape have an incident close => state conflict
     */
    @Test
    public void test_current_tape_conflict_state_then_unload_load_tape_with_empty_label_success()
        throws Exception {
        // When
        when(tapeDriveService.getTapeDriveConf()).thenAnswer(o -> mock(TapeDriveConf.class));
        String file = getTarFileName();

        // given
        WriteOrder writeOrder =
            new WriteOrder().setBucket(FAKE_BUCKET).setFilePath(FAKE_FILE_PATH).setArchiveId(file).setSize(10l);

        WriteTask writeTask =
            new WriteTask(writeOrder, getTapeCatalog(true, false, TapeState.CONFLICT),
                new TapeLibraryServiceImpl(tapeDriveService, tapeRobotPool),
                tapeCatalogService, archiveReferentialRepository, "/tmp", false);

        TapeCatalog tapeCatalog = getTapeCatalog(false, false, TapeState.EMPTY);
        tapeCatalog.setCurrentLocation(new TapeLocation(1, TapeLocationType.SLOT));
        when(tapeCatalogService.receive(any(), eq(QueueMessageType.TapeCatalog))).thenReturn(
            Optional.of(tapeCatalog));

        when(tapeDriveCommandService.eject()).thenReturn(new TapeResponse(StatusCode.OK));
        when(tapeLoadUnloadService.unloadTape(anyInt(), anyInt())).thenReturn(new TapeResponse(StatusCode.OK));

        when(tapeLoadUnloadService.loadTape(anyInt(), anyInt())).thenReturn(new TapeResponse(StatusCode.OK));

        TapeDriveState tapeDriveState = new TapeDriveState(StatusCode.OK);
        tapeDriveState.setCartridge("VIT0001");

        when(tapeDriveCommandService.status())
            .thenReturn(new TapeDriveState(JsonHandler.createObjectNode(), StatusCode.KO))
            .thenReturn(tapeDriveState);

        when(tapeDriveCommandService.move(anyInt(), anyBoolean()))
            .thenReturn(new TapeResponse(JsonHandler.createObjectNode(), StatusCode.KO))
            .thenReturn(new TapeResponse(StatusCode.OK));

        // When Write Label and file to tape
        when(tapeReadWriteService.writeToTape(contains(WriteTask.TAPE_LABEL)))
            .thenReturn(new TapeResponse(StatusCode.OK));

        when(tapeReadWriteService.writeToTape(eq(FAKE_FILE_PATH + "/" + writeOrder.getArchiveId())))
            .thenReturn(new TapeResponse(StatusCode.OK));

        ReadWriteResult result = writeTask.get();

        // Then
        verify(tapeRobotPool, new Times(2)).checkoutRobotService();
        verify(tapeRobotService, new Times(2)).getLoadUnloadService();
        verify(tapeDriveCommandService, new Times(2)).status();
        verify(tapeDriveCommandService, new Times(1)).move(anyInt(), anyBoolean());
        verify(tapeDriveCommandService, new Times(1)).eject();

        verify(tapeLoadUnloadService, new Times(1)).loadTape(anyInt(), anyInt());
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
    }

    /**
     * When drive try to write a file to the current tape, error occurs. Status command return that drive is at end of tape
     * Means that the tape is full.
     * Unload current tape and mark FULL
     * Find new tape and do stuff
     */
    @Test
    public void test_current_tape_end_of_tape_then_unload_load_tape_with_empty_label_success()
        throws Exception {
        // When
        when(tapeDriveService.getTapeDriveConf()).thenAnswer(o -> mock(TapeDriveConf.class));
        String file = getTarFileName();

        // given
        WriteOrder writeOrder =
            new WriteOrder().setBucket(FAKE_BUCKET).setFilePath(FAKE_FILE_PATH).setArchiveId(file).setSize(10l);

        WriteTask writeTask =
            new WriteTask(writeOrder, getTapeCatalog(true, false, TapeState.OPEN),
                new TapeLibraryServiceImpl(tapeDriveService, tapeRobotPool),
                tapeCatalogService, archiveReferentialRepository, "/tmp", false);

        TapeCatalog tapeCatalog = getTapeCatalog(false, false, TapeState.EMPTY);
        tapeCatalog.setCurrentLocation(new TapeLocation(1, TapeLocationType.SLOT));
        when(tapeCatalogService.receive(any(), eq(QueueMessageType.TapeCatalog))).thenReturn(
            Optional.of(tapeCatalog));

        when(tapeLoadUnloadService.unloadTape(anyInt(), anyInt())).thenReturn(new TapeResponse(StatusCode.OK));
        when(tapeDriveCommandService.eject()).thenReturn(new TapeResponse(StatusCode.OK));

        when(tapeLoadUnloadService.loadTape(anyInt(), anyInt())).thenReturn(new TapeResponse(StatusCode.OK));

        TapeDriveState tapeDriveState = new TapeDriveState(StatusCode.OK);
        tapeDriveState.setCartridge("VIT0001");

        TapeDriveState driveStateEndOfTape = new TapeDriveState(JsonHandler.createObjectNode(), StatusCode.OK);
        driveStateEndOfTape.addToDriveStatuses(TapeDriveStatus.EOT);

        when(tapeDriveCommandService.status())
            .thenReturn(driveStateEndOfTape)
            .thenReturn(new TapeDriveState(JsonHandler.createObjectNode(), StatusCode.KO))
            .thenReturn(tapeDriveState);


        // When Write Label and file to tape
        when(tapeReadWriteService.writeToTape(contains(WriteTask.TAPE_LABEL)))
            .thenReturn(new TapeResponse(StatusCode.OK));

        // First write response ko (end of tape) the second write should be a new tape
        when(tapeReadWriteService.writeToTape(eq(FAKE_FILE_PATH + "/" + writeOrder.getArchiveId())))
            .thenReturn(new TapeResponse(JsonHandler.createObjectNode(), StatusCode.KO))
            .thenReturn(new TapeResponse(StatusCode.OK));

        when(tapeDriveCommandService.move(anyInt(), anyBoolean()))
            .thenReturn(new TapeResponse(JsonHandler.createObjectNode(), StatusCode.KO));

        ReadWriteResult result = writeTask.get();

        // Then
        verify(tapeRobotPool, new Times(2)).checkoutRobotService();
        verify(tapeRobotService, new Times(2)).getLoadUnloadService();
        verify(tapeDriveCommandService, new Times(3)).status();
        verify(tapeDriveCommandService, new Times(1)).eject();
        verify(tapeDriveCommandService, new Times(1)).move(anyInt(), anyBoolean());
        verify(tapeLoadUnloadService, new Times(1)).loadTape(anyInt(), anyInt());
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
    }

    @Test
    public void test_write_to_tape_ko_try_with_next_tape_success()
        throws Exception {
        // When
        when(tapeDriveService.getTapeDriveConf()).thenAnswer(o -> mock(TapeDriveConf.class));
        String file = getTarFileName();

        // given
        WriteOrder writeOrder =
            new WriteOrder().setBucket(FAKE_BUCKET).setFilePath(FAKE_FILE_PATH).setArchiveId(file).setSize(10l);

        TapeCatalog tape = getTapeCatalog(true, false, TapeState.OPEN);
        WriteTask writeTask =
            new WriteTask(writeOrder, tape,
                new TapeLibraryServiceImpl(tapeDriveService, tapeRobotPool),
                tapeCatalogService, archiveReferentialRepository, "/tmp", false);


        TapeCatalog tapeCatalog = getTapeCatalog(false, false, TapeState.EMPTY);
        when(tapeCatalogService.receive(any(), eq(QueueMessageType.TapeCatalog))).thenReturn(
            Optional.of(tapeCatalog));

        when(tapeLoadUnloadService.unloadTape(anyInt(), anyInt())).thenReturn(new TapeResponse(StatusCode.OK));
        when(tapeDriveCommandService.eject()).thenReturn(new TapeResponse(StatusCode.OK));

        when(tapeLoadUnloadService.loadTape(anyInt(), anyInt())).thenReturn(new TapeResponse(StatusCode.OK));


        // When Write Label and file to tape
        when(tapeReadWriteService.writeToTape(contains(WriteTask.TAPE_LABEL)))
            .thenReturn(new TapeResponse(StatusCode.OK));

        TapeDriveState tapeDriveState = new TapeDriveState(StatusCode.OK);
        tapeDriveState.setCartridge("VIT0001");

        when(tapeDriveCommandService.status())
            .thenReturn(new TapeDriveState(StatusCode.OK))
            .thenReturn(tapeDriveState);

        // First write response ko (end of tape) the second write should be a new tape
        when(tapeReadWriteService.writeToTape(eq(FAKE_FILE_PATH + "/" + writeOrder.getArchiveId())))
            .thenReturn(new TapeResponse(JsonHandler.createObjectNode(), StatusCode.KO))
            .thenReturn(new TapeResponse(StatusCode.OK));

        when(tapeDriveCommandService.move(anyInt(), anyBoolean()))
            .thenReturn(new TapeResponse(JsonHandler.createArrayNode(), StatusCode.KO));

        ReadWriteResult result = writeTask.get();

        // Then
        verify(tapeDriveCommandService, new Times(2)).status();
        verify(tapeDriveCommandService, new Times(1)).eject();
        verify(tapeDriveCommandService, new Times(1)).move(anyInt(), anyBoolean());
        verify(tapeLoadUnloadService, new Times(1)).loadTape(anyInt(), anyInt());
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
    }

    //***************************************
    // KO
    //***************************************

    @Test
    public void test_write_to_tape_ko_try_with_next_tape_ko()
        throws Exception {
        // When
        when(tapeDriveService.getTapeDriveConf()).thenAnswer(o -> mock(TapeDriveConf.class));
        String file = getTarFileName();

        // given
        WriteOrder writeOrder =
            new WriteOrder().setBucket(FAKE_BUCKET).setFilePath(FAKE_FILE_PATH).setArchiveId(file).setSize(10l);

        WriteTask writeTask =
            new WriteTask(writeOrder, getTapeCatalog(true, false, TapeState.OPEN),
                new TapeLibraryServiceImpl(tapeDriveService, tapeRobotPool),
                tapeCatalogService, archiveReferentialRepository, "/tmp", false);


        TapeCatalog tapeCatalog = getTapeCatalog(false, false, TapeState.EMPTY);
        when(tapeCatalogService.receive(any(), eq(QueueMessageType.TapeCatalog))).thenReturn(
            Optional.of(tapeCatalog));

        when(tapeLoadUnloadService.unloadTape(anyInt(), anyInt())).thenReturn(new TapeResponse(StatusCode.OK));
        when(tapeDriveCommandService.eject()).thenReturn(new TapeResponse(StatusCode.OK));

        when(tapeLoadUnloadService.loadTape(anyInt(), anyInt())).thenReturn(new TapeResponse(StatusCode.OK));


        // When Write Label and file to tape
        when(tapeReadWriteService.writeToTape(contains(WriteTask.TAPE_LABEL)))
            .thenReturn(new TapeResponse(StatusCode.OK));

        TapeDriveState tapeDriveState = new TapeDriveState(StatusCode.OK);
        tapeDriveState.setCartridge("VIT0001");

        when(tapeDriveCommandService.status())
            .thenReturn(new TapeDriveState(StatusCode.OK))
            .thenReturn(tapeDriveState)
            .thenReturn(new TapeDriveState(StatusCode.OK));

        // First write response ko (end of tape) the second write should be a new tape
        when(tapeReadWriteService.writeToTape(eq(FAKE_FILE_PATH + "/" + writeOrder.getArchiveId())))
            .thenReturn(new TapeResponse(JsonHandler.createObjectNode(), StatusCode.KO))
            .thenReturn(new TapeResponse(JsonHandler.createObjectNode(), StatusCode.KO));

        when(tapeDriveCommandService.move(anyInt(), anyBoolean()))
            .thenReturn(new TapeResponse(JsonHandler.createArrayNode(), StatusCode.KO));

        ReadWriteResult result = writeTask.get();

        // Then
        verify(tapeDriveCommandService, new Times(3)).status();
        verify(tapeDriveCommandService, new Times(1)).eject();
        verify(tapeDriveCommandService, new Times(1)).move(anyInt(), anyBoolean());
        verify(tapeLoadUnloadService, new Times(1)).loadTape(anyInt(), anyInt());
        verify(tapeLoadUnloadService, new Times(1)).unloadTape(anyInt(), anyInt());
        verify(tapeReadWriteService, new Times(1)).writeToTape(contains(WriteTask.TAPE_LABEL));
        verify(tapeReadWriteService, new Times(2)).writeToTape(eq(FAKE_FILE_PATH + "/" + writeOrder.getArchiveId()));

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(StatusCode.FATAL);
        assertThat(result.getOrderState()).isEqualTo(QueueState.READY);

        assertThat(result.getCurrentTape()).isNotNull();
    }


    @Test
    public void test_write_to_tape_ko_on_db_persist_tape_back_to_catalog_fatal()
        throws Exception {
        // When
        when(tapeDriveService.getTapeDriveConf()).thenAnswer(o -> mock(TapeDriveConf.class));
        String file = getTarFileName();

        // given
        WriteOrder writeOrder =
            new WriteOrder().setBucket(FAKE_BUCKET).setFilePath(FAKE_FILE_PATH).setArchiveId(file).setSize(10l);

        WriteTask writeTask =
            new WriteTask(writeOrder, getTapeCatalog(true, false, TapeState.CONFLICT),
                new TapeLibraryServiceImpl(tapeDriveService, tapeRobotPool),
                tapeCatalogService, archiveReferentialRepository, "/tmp", false);

        when(tapeCatalogService.update(any(), anyMap()))
            .thenThrow(new TapeCatalogException(""))
            .thenThrow(new TapeCatalogException(""))
            .thenThrow(new TapeCatalogException(""));

        when(tapeLoadUnloadService.unloadTape(anyInt(), anyInt())).thenReturn(new TapeResponse(StatusCode.OK));
        when(tapeDriveCommandService.eject()).thenReturn(new TapeResponse(StatusCode.OK));

        ReadWriteResult result = writeTask.get();

        // Then
        verify(tapeCatalogService, new Times(3)).update(any(), anyMap());
        verify(tapeLoadUnloadService, new Times(1)).unloadTape(anyInt(), anyInt());
        verify(tapeDriveCommandService, new Times(1)).eject();

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(StatusCode.FATAL);
        assertThat(result.getOrderState()).isEqualTo(QueueState.READY);

        assertThat(result.getCurrentTape()).isNotNull();
    }

    @Test
    public void test_writeOrder_file_not_found() {
        // When
        when(tapeDriveService.getTapeDriveConf()).thenAnswer(o -> mock(TapeDriveConf.class));
        WriteOrder writeOrder =
            new WriteOrder().setBucket(FAKE_BUCKET).setFilePath(FAKE_FILE_PATH)
                .setArchiveId(GUIDFactory.newGUID().getId()).setSize(10l);

        WriteTask writeTask =
            new WriteTask(writeOrder, null, new TapeLibraryServiceImpl(tapeDriveService, tapeRobotPool),
                tapeCatalogService,
                archiveReferentialRepository, "", false);

        ReadWriteResult result = writeTask.get();

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(StatusCode.FATAL);
        assertThat(result.getOrderState()).isEqualTo(QueueState.ERROR);
        assertThat(result.getCurrentTape()).isNull();

    }

    @Test
    public void test_write_when_tape_is_wrong_then_eject_tape_ko()
        throws Exception {
        // When
        when(tapeDriveService.getTapeDriveConf()).thenAnswer(o -> mock(TapeDriveConf.class));
        String file = getTarFileName();

        // given
        WriteOrder writeOrder =
            new WriteOrder().setBucket(FAKE_BUCKET).setFilePath(FAKE_FILE_PATH).setArchiveId(file).setSize(10l);

        TapeCatalog tape = getTapeCatalog(true, false, TapeState.OPEN);
        tape.setBucket("wrongBucket");

        WriteTask writeTask =
            new WriteTask(writeOrder, tape, new TapeLibraryServiceImpl(tapeDriveService, tapeRobotPool),
                tapeCatalogService, archiveReferentialRepository, "/tmp", false);

        when(tapeDriveCommandService.eject()).thenReturn(new TapeResponse(StatusCode.KO));

        ReadWriteResult result = writeTask.get();

        // Then
        verify(tapeDriveCommandService, new Times(1)).eject();
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(StatusCode.FATAL);
        assertThat(result.getOrderState()).isEqualTo(QueueState.READY);
    }

    private String getTarFileName() throws IOException {
        Path resolve = Paths.get("/tmp").resolve(FAKE_FILE_PATH);
        Files.createDirectories(resolve);
        Path filePath = Paths.get("/tmp", FAKE_FILE_PATH).resolve(GUIDFactory.newGUID().getId() + ".tar");
        Path file = Files.createFile(filePath);
        return file.toFile().getName();

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