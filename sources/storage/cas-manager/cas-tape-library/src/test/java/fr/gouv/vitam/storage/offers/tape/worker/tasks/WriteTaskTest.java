package fr.gouv.vitam.storage.offers.tape.worker.tasks;

import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.storage.tapelibrary.TapeDriveConf;
import fr.gouv.vitam.storage.engine.common.model.TapeCatalog;
import fr.gouv.vitam.storage.engine.common.model.TapeState;
import fr.gouv.vitam.storage.engine.common.model.WriteOrder;
import fr.gouv.vitam.storage.offers.tape.dto.TapeResponse;
import fr.gouv.vitam.storage.offers.tape.spec.TapeCatalogService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeDriveService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeReadWriteService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeRobotPool;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
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
    public void test_write_task_current_tape_not_null_no_label_success() throws FileNotFoundException {
        // When
        when(tapeDriveService.getTapeDriveConf()).thenAnswer(o -> mock(TapeDriveConf.class));
        TapeCatalog tapeCatalog =
            new TapeCatalog().setLibrary(FAKE_LIBRARY).setBucket(FAKE_BUCKET).setTapeState(TapeState.OPEN);

        String file = PropertiesUtils.getResourceFile("tar/testtar.tar").getAbsolutePath();

        WriteOrder writeOrder = new WriteOrder().setBucket(FAKE_BUCKET).setFilePath(file).setSize(10l);

        WriteTask writeTask =
            new WriteTask(writeOrder, tapeCatalog, tapeRobotPool, tapeDriveService, tapeCatalogService);

        when(tapeReadWriteService.writeToTape(anyString(), contains(WriteTask.TAPE_LABEL)))
            .thenReturn(new TapeResponse(StatusCode.OK));

        when(tapeReadWriteService.writeToTape(anyString(), eq(writeOrder.getFilePath())))
            .thenReturn(new TapeResponse(StatusCode.OK));

        // Case one current t
        ReadWriteResult result = writeTask.get();


    }
}