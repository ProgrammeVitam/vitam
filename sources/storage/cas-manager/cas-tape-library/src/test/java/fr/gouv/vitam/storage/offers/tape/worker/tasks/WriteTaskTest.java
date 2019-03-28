package fr.gouv.vitam.storage.offers.tape.worker.tasks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Optional;

import fr.gouv.vitam.common.PropertiesUtils;
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
import fr.gouv.vitam.storage.offers.tape.dto.TapeDriveState;
import fr.gouv.vitam.storage.offers.tape.dto.TapeDriveStatus;
import fr.gouv.vitam.storage.offers.tape.dto.TapeResponse;
import fr.gouv.vitam.storage.offers.tape.exception.TapeCatalogException;
import fr.gouv.vitam.storage.offers.tape.spec.TapeCatalogService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeDriveCommandService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeDriveService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeLoadUnloadService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeReadWriteService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeRobotPool;
import fr.gouv.vitam.storage.offers.tape.spec.TapeRobotService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.internal.verification.Times;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class WriteTaskTest {

    public static final String FAKE_BUCKET = "fakeBucket";
    public static final String FAKE_LIBRARY = "fakeLibrary";
    public static final String LTO_6 = "LTO-6";
    public static final String TAPE_CODE = "VIT0001";
    public static final String TEST_TAR = "tar/testtar.tar";
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

    @Before
    public void setUp() throws Exception {

        reset(tapeRobotPool);
        reset(tapeRobotService);
        reset(tapeDriveService);
        reset(tapeCatalogService);
        reset(tapeReadWriteService);
        reset(tapeDriveCommandService);
        reset(tapeLoadUnloadService);

        when(tapeDriveService.getReadWriteService(eq(TapeDriveService.ReadWriteCmd.DD)))
            .thenAnswer(o -> tapeReadWriteService);
        when(tapeDriveService.getDriveCommandService()).thenAnswer(o -> tapeDriveCommandService);
        when(tapeRobotPool.checkoutRobotService()).thenAnswer(o -> tapeRobotService);
        when(tapeRobotService.getLoadUnloadService()).thenAnswer(o -> tapeLoadUnloadService);
    }

    @Test
    public void test_write_task_constructor() {
        // When
        when(tapeDriveService.getTapeDriveConf()).thenAnswer(o -> mock(TapeDriveConf.class));

        // Test constructors
        try {
            new WriteTask(null, null, null, null, null);
            fail("should throw exception");
        } catch (IllegalArgumentException e) {
        }

        try {
            new WriteTask(mock(WriteOrder.class), null, null, null, null);
            fail("should throw exception");
        } catch (IllegalArgumentException e) {
        }

        try {
            new WriteTask(null, null, tapeRobotPool, null, null);
            fail("should throw exception");
        } catch (IllegalArgumentException e) {
        }

        try {
            new WriteTask(null, null, null, tapeDriveService, null);
            fail("should throw exception");
        } catch (IllegalArgumentException e) {
        }

        try {
            new WriteTask(null, null, null, null, tapeCatalogService);
            fail("should throw exception");
        } catch (IllegalArgumentException e) {
        }

        try {
            new WriteTask(mock(WriteOrder.class), null, tapeRobotPool, tapeDriveService, tapeCatalogService);
        } catch (IllegalArgumentException e) {
            fail("should not throw exception");
        }
    }

    @Test
    public void test_current_tape_not_null_and_empty_label_success() throws Exception {
        // When
        when(tapeDriveService.getTapeDriveConf()).thenAnswer(o -> mock(TapeDriveConf.class));
        String file = PropertiesUtils.getResourceFile(TEST_TAR).getAbsolutePath();

        // Given WriterOrder and TapeCatalog
        WriteOrder writeOrder = new WriteOrder().setBucket(FAKE_BUCKET).setFilePath(file).setSize(10l);

        TapeCatalog tapeCatalog = getTapeCatalog(false, false, TapeState.EMPTY);

        // When write to tape (label and file)
        when(tapeReadWriteService.writeToTape(contains(WriteTask.TAPE_LABEL)))
            .thenReturn(new TapeResponse(StatusCode.OK));
        when(tapeReadWriteService.writeToTape(eq(writeOrder.getFilePath())))
            .thenReturn(new TapeResponse(StatusCode.OK));


        WriteTask writeTask =
            new WriteTask(writeOrder, tapeCatalog, tapeRobotPool, tapeDriveService, tapeCatalogService);

        ReadWriteResult result = writeTask.get();

        // Then
        verify(tapeReadWriteService, new Times(1)).writeToTape(contains(WriteTask.TAPE_LABEL));
        verify(tapeReadWriteService, new Times(1)).writeToTape(eq(writeOrder.getFilePath()));

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(StatusCode.OK);
        assertThat(result.getOrderState()).isEqualTo(QueueState.COMPLETED);

        assertThat(result.getCurrentTape()).isNotNull();

        assertThat(result.getCurrentTape().getFileCount()).isEqualTo(2);
        assertThat(result.getCurrentTape().getTapeState()).isEqualTo(TapeState.OPEN);

        TapeCatalogLabel label = result.getCurrentTape().getLabel();

        assertThat(label).isNotNull();
        assertThat(label)
            .extracting(TapeCatalogLabel.ID, TapeCatalogLabel.CODE, TapeCatalogLabel.BUCKET, TapeCatalogLabel.TYPE)
            .contains(tapeCatalog.getId(), tapeCatalog.getCode(), tapeCatalog.getBucket(), tapeCatalog.getType());
    }

    @Test
    public void test_current_tape_not_null_and_have_label_success() throws Exception {
        // When
        when(tapeDriveService.getTapeDriveConf()).thenAnswer(o -> mock(TapeDriveConf.class));
        String file = PropertiesUtils.getResourceFile(TEST_TAR).getAbsolutePath();

        WriteOrder writeOrder = new WriteOrder().setBucket(FAKE_BUCKET).setFilePath(file).setSize(10l);

        TapeCatalog tapeCatalog = getTapeCatalog(true, false, TapeState.EMPTY);

        when(tapeReadWriteService.writeToTape(eq(writeOrder.getFilePath())))
            .thenReturn(new TapeResponse(StatusCode.OK));



        WriteTask writeTask =
            new WriteTask(writeOrder, tapeCatalog, tapeRobotPool, tapeDriveService, tapeCatalogService);

        ReadWriteResult result = writeTask.get();

        verify(tapeReadWriteService, new Times(0)).writeToTape(contains(WriteTask.TAPE_LABEL));
        verify(tapeReadWriteService, new Times(1)).writeToTape(eq(writeOrder.getFilePath()));



        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(StatusCode.OK);
        assertThat(result.getOrderState()).isEqualTo(QueueState.COMPLETED);

        assertThat(result.getCurrentTape()).isNotNull();
        assertThat(result.getCurrentTape().getFileCount()).isEqualTo(1);
        assertThat(result.getCurrentTape().getTapeState()).isEqualTo(TapeState.OPEN);

        TapeCatalogLabel label = result.getCurrentTape().getLabel();

        assertThat(label).isNotNull();
        assertThat(label)
            .extracting(TapeCatalogLabel.ID, TapeCatalogLabel.CODE, TapeCatalogLabel.BUCKET, TapeCatalogLabel.TYPE)
            .contains(tapeCatalog.getId(), tapeCatalog.getCode(), tapeCatalog.getBucket(), tapeCatalog.getType());
    }


    @Test
    public void test_current_tape_not_null_and_empty_label_with_retry_success() throws Exception {
        // When
        when(tapeDriveService.getTapeDriveConf()).thenAnswer(o -> mock(TapeDriveConf.class));
        String file = PropertiesUtils.getResourceFile(TEST_TAR).getAbsolutePath();

        WriteOrder writeOrder = new WriteOrder().setBucket(FAKE_BUCKET).setFilePath(file).setSize(10l);

        TapeCatalog tapeCatalog = getTapeCatalog(false, false, TapeState.EMPTY);

        when(tapeReadWriteService.writeToTape(contains(WriteTask.TAPE_LABEL)))
            .thenReturn(new TapeResponse(JsonHandler.createObjectNode(), StatusCode.KO))
            .thenReturn(new TapeResponse(StatusCode.OK));

        when(tapeDriveCommandService.rewind()).thenReturn(new TapeResponse(StatusCode.OK));
        when(tapeDriveCommandService.goToPosition(anyInt())).thenReturn(new TapeResponse(StatusCode.OK));

        when(tapeReadWriteService.writeToTape(eq(writeOrder.getFilePath())))
            .thenReturn(new TapeResponse(StatusCode.OK));


        WriteTask writeTask =
            new WriteTask(writeOrder, tapeCatalog, tapeRobotPool, tapeDriveService, tapeCatalogService);

        ReadWriteResult result = writeTask.get();

        verify(tapeReadWriteService, new Times(2)).writeToTape(contains(WriteTask.TAPE_LABEL));
        verify(tapeReadWriteService, new Times(1)).writeToTape(eq(writeOrder.getFilePath()));

        verify(tapeDriveCommandService, new Times(1)).rewind();
        verify(tapeDriveCommandService, new Times(1)).goToPosition(anyInt());



        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(StatusCode.OK);
        assertThat(result.getOrderState()).isEqualTo(QueueState.COMPLETED);

        assertThat(result.getCurrentTape()).isNotNull();
        assertThat(result.getCurrentTape().getFileCount()).isEqualTo(2);
        assertThat(result.getCurrentTape().getTapeState()).isEqualTo(TapeState.OPEN);


        TapeCatalogLabel label = result.getCurrentTape().getLabel();

        assertThat(label).isNotNull();
        assertThat(label)
            .extracting(TapeCatalogLabel.ID, TapeCatalogLabel.CODE, TapeCatalogLabel.BUCKET, TapeCatalogLabel.TYPE)
            .contains(tapeCatalog.getId(), tapeCatalog.getCode(), tapeCatalog.getBucket(), tapeCatalog.getType());
    }


    @Test
    public void test_current_tape_not_null_and_have_label__with_retry_success() throws Exception {
        // When
        when(tapeDriveService.getTapeDriveConf()).thenAnswer(o -> mock(TapeDriveConf.class));
        String file = PropertiesUtils.getResourceFile(
            TEST_TAR).getAbsolutePath();

        WriteOrder writeOrder = new WriteOrder().setBucket(FAKE_BUCKET).setFilePath(file).setSize(10l);

        TapeCatalog tapeCatalog = getTapeCatalog(true, false, TapeState.EMPTY);

        when(tapeReadWriteService.writeToTape(contains(WriteTask.TAPE_LABEL)))
            .thenReturn(new TapeResponse(StatusCode.OK));

        when(tapeReadWriteService.writeToTape(eq(writeOrder.getFilePath())))
            .thenReturn(new TapeResponse(JsonHandler.createObjectNode(), StatusCode.KO))
            .thenReturn(new TapeResponse(JsonHandler.createObjectNode(), StatusCode.KO))
            .thenReturn(new TapeResponse(StatusCode.OK));


        when(tapeDriveCommandService.status())
            .thenReturn(new TapeDriveState(JsonHandler.createObjectNode(), StatusCode.KO))
            .thenReturn(new TapeDriveState(StatusCode.OK));

        when(tapeDriveCommandService.rewind())
            .thenReturn(new TapeResponse(StatusCode.OK))
            .thenReturn(new TapeResponse(StatusCode.OK));

        when(tapeDriveCommandService.goToPosition(anyInt()))
            .thenReturn(new TapeResponse(StatusCode.OK))
            .thenReturn(new TapeResponse(StatusCode.OK));


        WriteTask writeTask =
            new WriteTask(writeOrder, tapeCatalog, tapeRobotPool, tapeDriveService, tapeCatalogService);

        ReadWriteResult result = writeTask.get();

        verify(tapeReadWriteService, new Times(0)).writeToTape(contains(WriteTask.TAPE_LABEL));
        verify(tapeReadWriteService, new Times(3)).writeToTape(eq(writeOrder.getFilePath()));

        verify(tapeDriveCommandService, new Times(2)).rewind();
        verify(tapeDriveCommandService, new Times(2)).status();
        verify(tapeDriveCommandService, new Times(2)).goToPosition(anyInt());


        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(StatusCode.OK);
        assertThat(result.getOrderState()).isEqualTo(QueueState.COMPLETED);

        assertThat(result.getCurrentTape()).isNotNull();
        assertThat(result.getCurrentTape().getFileCount()).isEqualTo(1);
        assertThat(result.getCurrentTape().getTapeState()).isEqualTo(TapeState.OPEN);

        TapeCatalogLabel label = result.getCurrentTape().getLabel();

        assertThat(label).isNotNull();
        assertThat(label)
            .extracting(TapeCatalogLabel.ID, TapeCatalogLabel.CODE, TapeCatalogLabel.BUCKET, TapeCatalogLabel.TYPE)
            .contains(tapeCatalog.getId(), tapeCatalog.getCode(), tapeCatalog.getBucket(), tapeCatalog.getType());
    }


    @Test
    public void test_current_tape_null_then_load_tape_with_empty_label_success()
        throws Exception {
        // When
        when(tapeDriveService.getTapeDriveConf()).thenAnswer(o -> mock(TapeDriveConf.class));
        String file = PropertiesUtils.getResourceFile(TEST_TAR).getAbsolutePath();

        // given
        WriteOrder writeOrder =
            new WriteOrder().setBucket(FAKE_BUCKET).setFilePath(file).setSize(10l);

        WriteTask writeTask =
            new WriteTask(writeOrder, null, tapeRobotPool, tapeDriveService, tapeCatalogService);

        TapeCatalog tapeCatalog = getTapeCatalog(false, false, TapeState.EMPTY);
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

        when(tapeReadWriteService.writeToTape(eq(writeOrder.getFilePath())))
            .thenReturn(new TapeResponse(StatusCode.OK));

        when(tapeDriveCommandService.rewind())
            .thenReturn(new TapeResponse(StatusCode.OK));

        when(tapeDriveCommandService.goToPosition(anyInt()))
            .thenReturn(new TapeResponse(JsonHandler.createObjectNode(), StatusCode.KO))
            .thenReturn(new TapeResponse(StatusCode.OK));

        ReadWriteResult result = writeTask.get();

        // Then
        verify(tapeRobotPool, new Times(1)).checkoutRobotService();
        verify(tapeRobotService, new Times(1)).getLoadUnloadService();
        verify(tapeDriveCommandService, new Times(2)).status();
        verify(tapeDriveCommandService, new Times(2)).goToPosition(anyInt());
        verify(tapeLoadUnloadService, new Times(1)).loadTape(anyInt(), anyInt());
        verify(tapeReadWriteService, new Times(1)).writeToTape(contains(WriteTask.TAPE_LABEL));
        verify(tapeReadWriteService, new Times(1)).writeToTape(eq(writeOrder.getFilePath()));
        //verify(tapeReadWriteService, new Times(1)).readFromTape(anyString());

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
        String file = PropertiesUtils.getResourceFile(TEST_TAR).getAbsolutePath();

        // given
        WriteOrder writeOrder =
            new WriteOrder().setBucket(FAKE_BUCKET).setFilePath(file).setSize(10l);

        WriteTask writeTask =
            new WriteTask(writeOrder, null, tapeRobotPool, tapeDriveService, tapeCatalogService);

        TapeCatalog tapeCatalog = getTapeCatalog(false, false, TapeState.EMPTY);
        when(tapeCatalogService.receive(any(), eq(QueueMessageType.TapeCatalog))).thenReturn(
            Optional.of(tapeCatalog));

        when(tapeLoadUnloadService.loadTape(anyInt(), anyInt())).thenReturn(new TapeResponse(StatusCode.OK));

        TapeDriveState tapeDriveState = new TapeDriveState(StatusCode.OK);
        tapeDriveState.setCartridge("LTO-6");

        when(tapeDriveCommandService.rewind())
            .thenReturn(new TapeResponse(StatusCode.OK));

        when(tapeDriveCommandService.goToPosition(anyInt()))
            .thenReturn(new TapeResponse(StatusCode.OK));

        ReadWriteResult result = writeTask.get();

        // Then
        verify(tapeRobotPool, new Times(1)).checkoutRobotService();
        verify(tapeRobotService, new Times(1)).getLoadUnloadService();
        verify(tapeDriveCommandService, new Times(1)).goToPosition(anyInt());
        verify(tapeDriveCommandService, new Times(1)).rewind();
        verify(tapeLoadUnloadService, new Times(1)).loadTape(anyInt(), anyInt());

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(StatusCode.FATAL);
        assertThat(result.getOrderState()).isEqualTo(QueueState.READY);

        assertThat(result.getCurrentTape()).isNotNull();

        assertThat(result.getCurrentTape().getFileCount()).isEqualTo(0);
        assertThat(result.getCurrentTape().getType()).isEqualTo(tapeDriveState.getCartridge());
        assertThat(result.getCurrentTape().getTapeState()).isEqualTo(TapeState.EMPTY);

        TapeCatalogLabel label = result.getCurrentTape().getLabel();

        assertThat(label).isNull();
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
        String file = PropertiesUtils.getResourceFile(TEST_TAR).getAbsolutePath();

        // given
        WriteOrder writeOrder =
            new WriteOrder().setBucket(FAKE_BUCKET).setFilePath(file).setSize(10l);

        WriteTask writeTask =
            new WriteTask(writeOrder, null, tapeRobotPool, tapeDriveService, tapeCatalogService);

        TapeCatalog tapeCatalog = getTapeCatalog(true, false, TapeState.OPEN);
        when(tapeCatalogService.receive(any(), eq(QueueMessageType.TapeCatalog))).thenReturn(
            Optional.of(tapeCatalog));

        when(tapeLoadUnloadService.loadTape(anyInt(), anyInt())).thenReturn(new TapeResponse(StatusCode.OK));

        TapeDriveState tapeDriveState = new TapeDriveState(StatusCode.OK);
        tapeDriveState.setCartridge("LTO-6");

        when(tapeDriveCommandService.rewind())
            .thenReturn(new TapeResponse(StatusCode.OK));

        when(tapeDriveCommandService.goToPosition(anyInt()))
            .thenReturn(new TapeResponse(StatusCode.OK));

        when(tapeReadWriteService.readFromTape(anyString())).thenAnswer(o -> {
            String filePath = o.getArgument(1);
            JsonHandler.writeAsFile(new TapeCatalogLabel("DISCARDED_GUID", "FAKE_CODE"), new File(filePath));
            return new TapeResponse(JsonHandler.createObjectNode(), StatusCode.OK);
        });

        ReadWriteResult result = writeTask.get();

        // Then
        verify(tapeRobotPool, new Times(1)).checkoutRobotService();
        verify(tapeRobotService, new Times(1)).getLoadUnloadService();
        verify(tapeDriveCommandService, new Times(1)).rewind();
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
     * Check label of tape catalog and loaded tape
     * Tape catalog declare a not empty label
     * when try to read label of loaded tape then error KO_LABEL_NOT_FOUND
     */
    @Test
    public void test_current_tape_null_then_load_tape_check_label_read_label_error()
        throws Exception {
        // When
        when(tapeDriveService.getTapeDriveConf()).thenAnswer(o -> mock(TapeDriveConf.class));
        String file = PropertiesUtils.getResourceFile(TEST_TAR).getAbsolutePath();

        // given
        WriteOrder writeOrder =
            new WriteOrder().setBucket(FAKE_BUCKET).setFilePath(file).setSize(10l);

        WriteTask writeTask =
            new WriteTask(writeOrder, null, tapeRobotPool, tapeDriveService, tapeCatalogService);

        TapeCatalog tapeCatalog = getTapeCatalog(true, false, TapeState.OPEN);
        when(tapeCatalogService.receive(any(), eq(QueueMessageType.TapeCatalog))).thenReturn(
            Optional.of(tapeCatalog));

        when(tapeLoadUnloadService.loadTape(anyInt(), anyInt())).thenReturn(new TapeResponse(StatusCode.OK));

        TapeDriveState tapeDriveState = new TapeDriveState(StatusCode.OK);
        tapeDriveState.setCartridge("LTO-6");

        when(tapeDriveCommandService.rewind())
            .thenReturn(new TapeResponse(StatusCode.OK));

        when(tapeDriveCommandService.goToPosition(anyInt()))
            .thenReturn(new TapeResponse(StatusCode.OK));

        when(tapeReadWriteService.readFromTape(anyString()))
            .thenReturn(new TapeResponse(JsonHandler.createObjectNode(), StatusCode.KO));

        ReadWriteResult result = writeTask.get();

        // Then
        verify(tapeRobotPool, new Times(1)).checkoutRobotService();
        verify(tapeRobotService, new Times(1)).getLoadUnloadService();
        verify(tapeDriveCommandService, new Times(1)).rewind();
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
        String file = PropertiesUtils.getResourceFile(TEST_TAR).getAbsolutePath();

        // given
        WriteOrder writeOrder =
            new WriteOrder().setBucket(FAKE_BUCKET).setFilePath(file).setSize(10l);

        WriteTask writeTask =
            new WriteTask(writeOrder, getTapeCatalog(true, false, TapeState.CONFLICT), tapeRobotPool, tapeDriveService,
                tapeCatalogService);

        TapeCatalog tapeCatalog = getTapeCatalog(false, false, TapeState.EMPTY);
        when(tapeCatalogService.receive(any(), eq(QueueMessageType.TapeCatalog))).thenReturn(
            Optional.of(tapeCatalog));

        when(tapeLoadUnloadService.unloadTape(anyInt(), anyInt())).thenReturn(new TapeResponse(StatusCode.OK));

        when(tapeLoadUnloadService.loadTape(anyInt(), anyInt())).thenReturn(new TapeResponse(StatusCode.OK));

        TapeDriveState tapeDriveState = new TapeDriveState(StatusCode.OK);
        tapeDriveState.setCartridge("VIT0001");

        when(tapeDriveCommandService.status())
            .thenReturn(new TapeDriveState(JsonHandler.createObjectNode(), StatusCode.KO))
            .thenReturn(tapeDriveState);


        // When Write Label and file to tape
        when(tapeReadWriteService.writeToTape(contains(WriteTask.TAPE_LABEL)))
            .thenReturn(new TapeResponse(StatusCode.OK));

        when(tapeReadWriteService.writeToTape(eq(writeOrder.getFilePath())))
            .thenReturn(new TapeResponse(StatusCode.OK));

        ReadWriteResult result = writeTask.get();

        // Then
        verify(tapeRobotPool, new Times(2)).checkoutRobotService();
        verify(tapeRobotService, new Times(2)).getLoadUnloadService();
        verify(tapeDriveCommandService, new Times(2)).status();
        verify(tapeLoadUnloadService, new Times(1)).loadTape(anyInt(), anyInt());
        verify(tapeLoadUnloadService, new Times(1)).unloadTape(anyInt(), anyInt());
        verify(tapeReadWriteService, new Times(1)).writeToTape(contains(WriteTask.TAPE_LABEL));
        verify(tapeReadWriteService, new Times(1)).writeToTape(eq(writeOrder.getFilePath()));

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
            .extracting(TapeCatalogLabel.ID, TapeCatalogLabel.CODE, TapeCatalogLabel.BUCKET, TapeCatalogLabel.TYPE)
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
        String file = PropertiesUtils.getResourceFile(TEST_TAR).getAbsolutePath();

        // given
        WriteOrder writeOrder =
            new WriteOrder().setBucket(FAKE_BUCKET).setFilePath(file).setSize(10l);

        WriteTask writeTask =
            new WriteTask(writeOrder, getTapeCatalog(true, false, TapeState.OPEN), tapeRobotPool, tapeDriveService,
                tapeCatalogService);

        TapeCatalog tapeCatalog = getTapeCatalog(false, false, TapeState.EMPTY);
        when(tapeCatalogService.receive(any(), eq(QueueMessageType.TapeCatalog))).thenReturn(
            Optional.of(tapeCatalog));

        when(tapeLoadUnloadService.unloadTape(anyInt(), anyInt())).thenReturn(new TapeResponse(StatusCode.OK));

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
        when(tapeReadWriteService.writeToTape(eq(writeOrder.getFilePath())))
            .thenReturn(new TapeResponse(JsonHandler.createObjectNode(), StatusCode.KO))
            .thenReturn(new TapeResponse(StatusCode.OK));

        ReadWriteResult result = writeTask.get();

        // Then
        verify(tapeRobotPool, new Times(2)).checkoutRobotService();
        verify(tapeRobotService, new Times(2)).getLoadUnloadService();
        verify(tapeDriveCommandService, new Times(3)).status();
        verify(tapeLoadUnloadService, new Times(1)).loadTape(anyInt(), anyInt());
        verify(tapeLoadUnloadService, new Times(1)).unloadTape(anyInt(), anyInt());
        verify(tapeReadWriteService, new Times(1)).writeToTape(contains(WriteTask.TAPE_LABEL));
        verify(tapeReadWriteService, new Times(2)).writeToTape(eq(writeOrder.getFilePath()));

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
            .extracting(TapeCatalogLabel.ID, TapeCatalogLabel.CODE, TapeCatalogLabel.BUCKET, TapeCatalogLabel.TYPE)
            .contains(tapeCatalog.getId(), tapeCatalog.getCode(), tapeCatalog.getBucket(), tapeCatalog.getType());
    }

    @Test
    public void test_write_to_tape_ko_try_with_next_tape_success()
        throws Exception {
        // When
        when(tapeDriveService.getTapeDriveConf()).thenAnswer(o -> mock(TapeDriveConf.class));
        String file = PropertiesUtils.getResourceFile(TEST_TAR).getAbsolutePath();

        // given
        WriteOrder writeOrder =
            new WriteOrder().setBucket(FAKE_BUCKET).setFilePath(file).setSize(10l);

        WriteTask writeTask =
            new WriteTask(writeOrder, getTapeCatalog(true, false, TapeState.OPEN), tapeRobotPool, tapeDriveService,
                tapeCatalogService);


        TapeCatalog tapeCatalog = getTapeCatalog(false, false, TapeState.EMPTY);
        when(tapeCatalogService.receive(any(), eq(QueueMessageType.TapeCatalog))).thenReturn(
            Optional.of(tapeCatalog));

        when(tapeLoadUnloadService.unloadTape(anyInt(), anyInt())).thenReturn(new TapeResponse(StatusCode.OK));

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
        when(tapeReadWriteService.writeToTape(eq(writeOrder.getFilePath())))
            .thenReturn(new TapeResponse(JsonHandler.createObjectNode(), StatusCode.KO))
            .thenReturn(new TapeResponse(JsonHandler.createObjectNode(), StatusCode.KO))
            .thenReturn(new TapeResponse(JsonHandler.createObjectNode(), StatusCode.KO))
            .thenReturn(new TapeResponse(StatusCode.OK));

        when(tapeDriveCommandService.rewind())
            .thenReturn(new TapeResponse(StatusCode.OK))
            .thenReturn(new TapeResponse(StatusCode.OK));

        when(tapeDriveCommandService.goToPosition(anyInt()))
            .thenReturn(new TapeResponse(StatusCode.OK))
            .thenReturn(new TapeResponse(StatusCode.OK));

        ReadWriteResult result = writeTask.get();

        // Then
        verify(tapeDriveCommandService, new Times(2)).status();
        verify(tapeDriveCommandService, new Times(2)).rewind();
        verify(tapeDriveCommandService, new Times(2)).goToPosition(anyInt());
        verify(tapeLoadUnloadService, new Times(1)).loadTape(anyInt(), anyInt());
        verify(tapeLoadUnloadService, new Times(1)).unloadTape(anyInt(), anyInt());
        verify(tapeReadWriteService, new Times(1)).writeToTape(contains(WriteTask.TAPE_LABEL));
        verify(tapeReadWriteService, new Times(4)).writeToTape(eq(writeOrder.getFilePath()));

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
            .extracting(TapeCatalogLabel.ID, TapeCatalogLabel.CODE, TapeCatalogLabel.BUCKET, TapeCatalogLabel.TYPE)
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
        String file = PropertiesUtils.getResourceFile(TEST_TAR).getAbsolutePath();

        // given
        WriteOrder writeOrder =
            new WriteOrder().setBucket(FAKE_BUCKET).setFilePath(file).setSize(10l);

        WriteTask writeTask =
            new WriteTask(writeOrder, getTapeCatalog(true, false, TapeState.OPEN), tapeRobotPool, tapeDriveService,
                tapeCatalogService);


        TapeCatalog tapeCatalog = getTapeCatalog(false, false, TapeState.EMPTY);
        when(tapeCatalogService.receive(any(), eq(QueueMessageType.TapeCatalog))).thenReturn(
            Optional.of(tapeCatalog));

        when(tapeLoadUnloadService.unloadTape(anyInt(), anyInt())).thenReturn(new TapeResponse(StatusCode.OK));

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
        when(tapeReadWriteService.writeToTape(eq(writeOrder.getFilePath())))
            .thenReturn(new TapeResponse(JsonHandler.createObjectNode(), StatusCode.KO))
            .thenReturn(new TapeResponse(JsonHandler.createObjectNode(), StatusCode.KO))
            .thenReturn(new TapeResponse(JsonHandler.createObjectNode(), StatusCode.KO))
            .thenReturn(new TapeResponse(JsonHandler.createObjectNode(), StatusCode.KO))
            .thenReturn(new TapeResponse(JsonHandler.createObjectNode(), StatusCode.KO))
            .thenReturn(new TapeResponse(JsonHandler.createObjectNode(), StatusCode.KO));

        when(tapeDriveCommandService.rewind())
            .thenReturn(new TapeResponse(StatusCode.OK))
            .thenReturn(new TapeResponse(StatusCode.OK))
            .thenReturn(new TapeResponse(StatusCode.OK))
            .thenReturn(new TapeResponse(StatusCode.OK));

        when(tapeDriveCommandService.goToPosition(anyInt()))
            .thenReturn(new TapeResponse(StatusCode.OK))
            .thenReturn(new TapeResponse(StatusCode.OK))
            .thenReturn(new TapeResponse(StatusCode.OK))
            .thenReturn(new TapeResponse(StatusCode.OK));

        ReadWriteResult result = writeTask.get();

        // Then
        verify(tapeDriveCommandService, new Times(3)).status();
        verify(tapeDriveCommandService, new Times(4)).rewind();
        verify(tapeDriveCommandService, new Times(4)).goToPosition(anyInt());
        verify(tapeLoadUnloadService, new Times(1)).loadTape(anyInt(), anyInt());
        verify(tapeLoadUnloadService, new Times(1)).unloadTape(anyInt(), anyInt());
        verify(tapeReadWriteService, new Times(1)).writeToTape(contains(WriteTask.TAPE_LABEL));
        verify(tapeReadWriteService, new Times(6)).writeToTape(eq(writeOrder.getFilePath()));

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
        String file = PropertiesUtils.getResourceFile(TEST_TAR).getAbsolutePath();

        // given
        WriteOrder writeOrder =
            new WriteOrder().setBucket(FAKE_BUCKET).setFilePath(file).setSize(10l);

        WriteTask writeTask =
            new WriteTask(writeOrder, getTapeCatalog(true, false, TapeState.CONFLICT), tapeRobotPool, tapeDriveService,
                tapeCatalogService);

        when(tapeCatalogService.update(any(), anyMap()))
            .thenThrow(new TapeCatalogException(""))
            .thenThrow(new TapeCatalogException(""))
            .thenThrow(new TapeCatalogException(""));

        when(tapeLoadUnloadService.unloadTape(anyInt(), anyInt())).thenReturn(new TapeResponse(StatusCode.OK));

        ReadWriteResult result = writeTask.get();

        // Then
        verify(tapeCatalogService, new Times(3)).update(any(), anyMap());
        verify(tapeLoadUnloadService, new Times(1)).unloadTape(anyInt(), anyInt());

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
            new WriteOrder().setBucket(FAKE_BUCKET).setFilePath("/tmp/" + GUIDFactory.newGUID().getId()).setSize(10l);

        WriteTask writeTask =
            new WriteTask(writeOrder, null, tapeRobotPool, tapeDriveService, tapeCatalogService);

        ReadWriteResult result = writeTask.get();

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(StatusCode.FATAL);
        assertThat(result.getOrderState()).isEqualTo(QueueState.ERROR);
        assertThat(result.getCurrentTape()).isNull();

    }

    @Test
    public void test_write_to_tape_retry_status_ko()
        throws Exception {
        // When
        when(tapeDriveService.getTapeDriveConf()).thenAnswer(o -> mock(TapeDriveConf.class));
        String file = PropertiesUtils.getResourceFile(TEST_TAR).getAbsolutePath();

        // given
        WriteOrder writeOrder =
            new WriteOrder().setBucket(FAKE_BUCKET).setFilePath(file).setSize(10l);

        WriteTask writeTask =
            new WriteTask(writeOrder, getTapeCatalog(true, false, TapeState.OPEN), tapeRobotPool, tapeDriveService,
                tapeCatalogService);


        when(tapeDriveCommandService.status())
            .thenReturn(new TapeDriveState(JsonHandler.createObjectNode(), StatusCode.KO))
            .thenReturn(new TapeDriveState(JsonHandler.createObjectNode(), StatusCode.KO))
            .thenReturn(new TapeDriveState(JsonHandler.createObjectNode(), StatusCode.KO));

        // First write response ko (end of tape) the second write should be a new tape
        when(tapeReadWriteService.writeToTape(eq(writeOrder.getFilePath())))
            .thenReturn(new TapeResponse(JsonHandler.createObjectNode(), StatusCode.KO));

        ReadWriteResult result = writeTask.get();

        // Then
        verify(tapeDriveCommandService, new Times(3)).status();
        verify(tapeReadWriteService, new Times(1)).writeToTape(eq(writeOrder.getFilePath()));

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(StatusCode.FATAL);
        assertThat(result.getOrderState()).isEqualTo(QueueState.ERROR);

        assertThat(result.getCurrentTape()).isNotNull();
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
        return tapeCatalog;
    }
}