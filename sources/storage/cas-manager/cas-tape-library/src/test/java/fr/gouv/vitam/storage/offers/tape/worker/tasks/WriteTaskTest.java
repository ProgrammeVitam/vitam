package fr.gouv.vitam.storage.offers.tape.worker.tasks;

import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.storage.tapelibrary.TapeDriveConf;
import fr.gouv.vitam.storage.engine.common.model.TapeCatalog;
import fr.gouv.vitam.storage.offers.tape.dto.CommandResponse;
import fr.gouv.vitam.storage.offers.tape.order.WriteOrder;
import fr.gouv.vitam.storage.offers.tape.spec.TapeCatalogService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeDriveService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeReadWriteService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeRobotPool;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class WriteTaskTest {

    public static final String FAKE_BUCKET = "fakeBucket";
    public static final String FAKE_LIBRARY = "fakeLibrary";
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private TapeRobotPool tapeRobotPool;

    @Mock
    private TapeDriveService tapeDriveService;

    @Mock
    private TapeCatalogService tapeCatalogService;

    @Mock
    private TapeReadWriteService tapeReadWriteService;

    @Before
    public void setUp() throws Exception {

        reset(tapeRobotPool);
        reset(tapeDriveService);
        reset(tapeCatalogService);
        reset(tapeReadWriteService);

        when(tapeDriveService.getReadWriteService(eq(TapeDriveService.ReadWriteCmd.DD)))
            .thenAnswer(o -> tapeReadWriteService);
    }

    @Test
    public void testConstructor() {
        // When
        when(tapeDriveService.getTapeDriveConf()).thenAnswer(o -> mock(TapeDriveConf.class));

        // Test constructors
        try {
            new WriteTask(null, null, null, null, null, 10l);
            fail("should throw exception");
        } catch (IllegalArgumentException e) {
        }

        try {
            new WriteTask(mock(WriteOrder.class), null, null, null, null, 10l);
            fail("should throw exception");
        } catch (IllegalArgumentException e) {
        }

        try {
            new WriteTask(null, null, tapeRobotPool, null, null, 10l);
            fail("should throw exception");
        } catch (IllegalArgumentException e) {
        }

        try {
            new WriteTask(null, null, null, tapeDriveService, null, 10l);
            fail("should throw exception");
        } catch (IllegalArgumentException e) {
        }

        try {
            new WriteTask(null, null, null, null, tapeCatalogService, 10l);
            fail("should throw exception");
        } catch (IllegalArgumentException e) {
        }

        try {
            new WriteTask(mock(WriteOrder.class), null, tapeRobotPool, tapeDriveService,
                tapeCatalogService, 10l);
        } catch (IllegalArgumentException e) {
            fail("should not throw exception");
        }
    }

    @Test
    public void test_write_task_current_tape_not_null_no_label_success() {
        // When
        when(tapeDriveService.getTapeDriveConf()).thenAnswer(o -> mock(TapeDriveConf.class));
        TapeCatalog tapeCatalog =
            new TapeCatalog().setLibrary(FAKE_LIBRARY).setBucket(FAKE_BUCKET).setRemainingSize(11l);

        WriteOrder writeOrder = new WriteOrder().setBucket(FAKE_BUCKET).setFilePath("/tmp/faketar.tar").setSize(10l);

        WriteTask writeTask = new WriteTask(writeOrder, tapeCatalog, tapeRobotPool, tapeDriveService,
            tapeCatalogService, 10l);

        when(tapeReadWriteService.writeToTape(anyLong(), anyString(), contains(WriteTask.TAPE_LABEL)))
            .thenReturn(new CommandResponse().setStatus(StatusCode.OK));
        when(tapeReadWriteService.writeToTape(anyLong(), anyString(), eq(writeOrder.getFilePath())))
            .thenReturn(new CommandResponse().setStatus(StatusCode.OK));

        // Case one current t
        ReadWriteResult result = writeTask.get();


    }

    @Test
    public void cancel() {
    }

    @Test
    public void isCancelled() {
    }

    @Test
    public void isDone() {
    }
}