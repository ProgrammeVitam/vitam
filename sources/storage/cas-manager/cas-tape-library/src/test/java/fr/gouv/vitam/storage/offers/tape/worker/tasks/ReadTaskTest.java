package fr.gouv.vitam.storage.offers.tape.worker.tasks;

import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.storage.tapelibrary.TapeDriveConf;
import fr.gouv.vitam.storage.engine.common.model.QueueState;
import fr.gouv.vitam.storage.engine.common.model.ReadOrder;
import fr.gouv.vitam.storage.engine.common.model.TapeCatalog;
import fr.gouv.vitam.storage.engine.common.model.TapeLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeLocationType;
import fr.gouv.vitam.storage.offers.tape.cas.ArchiveOutputRetentionPolicy;
import fr.gouv.vitam.storage.offers.tape.cas.ReadRequestReferentialRepository;
import fr.gouv.vitam.storage.offers.tape.dto.TapeDriveState;
import fr.gouv.vitam.storage.offers.tape.dto.TapeDriveStatus;
import fr.gouv.vitam.storage.offers.tape.dto.TapeResponse;
import fr.gouv.vitam.storage.offers.tape.exception.QueueException;
import fr.gouv.vitam.storage.offers.tape.exception.TapeCatalogException;
import fr.gouv.vitam.storage.offers.tape.impl.readwrite.TapeLibraryServiceImpl;
import fr.gouv.vitam.storage.offers.tape.spec.TapeCatalogService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeDriveCommandService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeDriveService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeLoadUnloadService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeReadWriteService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeRobotPool;
import fr.gouv.vitam.storage.offers.tape.spec.TapeRobotService;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

public class ReadTaskTest {
    public static final String FAKE_LIBRARY = "fakeLibrary";
    public static final String FAKE_TAPE_CODE = "fakeTapeCode";
    public static final Integer SLOT_INDEX = 1;
    public static final Integer DRIVE_INDEX = 0;
    private static final Integer FILE_POSITION = 0;
    private final String outputDirectory = "/tmp";

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private TapeRobotPool tapeRobotPool;

    @Mock
    private TapeDriveService tapeDriveService;

    @Mock
    private TapeCatalogService tapeCatalogService;

    @Mock
    private ReadRequestReferentialRepository readRequestReferentialRepository;

    @Mock
    private TapeReadWriteService tapeReadWriteService;

    @Mock
    private TapeDriveCommandService tapeDriveCommandService;

    @Mock
    private ArchiveOutputRetentionPolicy archiveOutputRetentionPolicy;

    private Path fileTest;
    private String fileName;

    @Before
    public void setUp() throws IOException {

        reset(tapeRobotPool);
        reset(tapeDriveService);
        reset(tapeCatalogService);
        reset(tapeReadWriteService);
        reset(tapeReadWriteService);
        reset(archiveOutputRetentionPolicy);

        when(tapeDriveService.getReadWriteService(eq(TapeDriveService.ReadWriteCmd.DD)))
            .thenAnswer(o -> tapeReadWriteService);

        fileName = GUIDFactory.newGUID().getId() + ".tar";
        when(tapeReadWriteService.getOutputDirectory()).thenReturn(outputDirectory);
    }

    @After
    public void tearDown() {
        try {
            if (fileTest != null) {
                Files.delete(fileTest);
            }
        } catch (IOException e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }
    }

    @Test
    public void testConstructor() {
        // When
        when(tapeDriveService.getTapeDriveConf()).thenAnswer(o -> mock(TapeDriveConf.class));

        // Test constructors
        try {
            new ReadTask(null, mock(TapeCatalog.class), mock(TapeLibraryServiceImpl.class),
                mock(TapeCatalogService.class), mock(ReadRequestReferentialRepository.class),
                archiveOutputRetentionPolicy);
            fail("read order is required");
        } catch (IllegalArgumentException e) {
        }

        try {
            new ReadTask(mock(ReadOrder.class), mock(TapeCatalog.class), null,
                mock(TapeCatalogService.class), mock(ReadRequestReferentialRepository.class),
                archiveOutputRetentionPolicy);
            fail("tape library service is required");
        } catch (IllegalArgumentException e) {
        }

        try {
            new ReadTask(mock(ReadOrder.class), mock(TapeCatalog.class), mock(TapeLibraryServiceImpl.class),
                null, mock(ReadRequestReferentialRepository.class), archiveOutputRetentionPolicy);
            fail("tape catalog service is required");
        } catch (IllegalArgumentException e) {
        }

        try {
            new ReadTask(mock(ReadOrder.class), mock(TapeCatalog.class), mock(TapeLibraryServiceImpl.class),
                mock(TapeCatalogService.class), null, archiveOutputRetentionPolicy);
            fail("read request repository is required");
        } catch (IllegalArgumentException e) {
        }

        try {
            new ReadTask(mock(ReadOrder.class), mock(TapeCatalog.class), mock(TapeLibraryServiceImpl.class),
                mock(TapeCatalogService.class), mock(ReadRequestReferentialRepository.class), null);
            fail("read request archiveOutputRetentionPolicy is required");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void testReadTaskCurrentTapeIsNotNullAndEligible() {
        // When
        when(tapeDriveService.getTapeDriveConf()).thenAnswer(o -> mock(TapeDriveConf.class));
        TapeCatalog tapeCatalog = new TapeCatalog()
            .setLibrary(FAKE_LIBRARY)
            .setCode(FAKE_TAPE_CODE)
            .setCurrentPosition(FILE_POSITION)
            .setCurrentLocation(new TapeLocation(DRIVE_INDEX, TapeLocationType.DRIVE));

        ReadOrder readOrder = new ReadOrder().setTapeCode(FAKE_TAPE_CODE)
            .setFilePosition(FILE_POSITION)
            .setFileName(fileName);

        ReadTask readTask =
            new ReadTask(readOrder, tapeCatalog, new TapeLibraryServiceImpl(tapeDriveService, tapeRobotPool),
                tapeCatalogService, readRequestReferentialRepository, archiveOutputRetentionPolicy);

        doAnswer(
            invocationOnMock -> {
                fileTest = Files.createFile(Paths.get(outputDirectory + File.separator + fileName + ReadTask.TEMP_EXT));
                return new TapeResponse(StatusCode.OK);
            }
        ).when(tapeReadWriteService).readFromTape(any());

        when(tapeDriveService.getDriveCommandService())
            .thenReturn(tapeDriveCommandService);
        when(tapeDriveService.getDriveCommandService().rewind())
            .thenReturn(new TapeResponse(StatusCode.OK));
        when(tapeDriveService.getDriveCommandService().move(anyInt(), anyBoolean()))
            .thenReturn(new TapeResponse(StatusCode.OK));
        when(tapeDriveService.getDriveCommandService().move(anyInt(), anyBoolean()))
            .thenReturn(new TapeResponse(StatusCode.OK));
        when(tapeDriveCommandService.eject()).thenReturn(new TapeResponse(StatusCode.OK));

        // Case one current t
        ReadWriteResult result = readTask.get();

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(StatusCode.OK);
        assertThat(result.getOrderState()).isEqualTo(QueueState.COMPLETED);
        assertThat(result.getCurrentTape()).isNotNull();
        assertThat(result.getCurrentTape().getCode()).isEqualTo(FAKE_TAPE_CODE);
        assertThat(result.getCurrentTape().getCurrentLocation()
            .equals(new TapeLocation(DRIVE_INDEX, TapeLocationType.DRIVE))).isEqualTo(true);
        assertThat(result.getCurrentTape().getCurrentPosition()).isEqualTo(FILE_POSITION + 1);

    }

    @Test
    public void testReadTaskWhenCurrentTapeIsNotNullAndNotEligibleAndTheEligibleTapeIsAvailable()
        throws InterruptedException, QueueException {
        // When
        when(tapeDriveService.getTapeDriveConf()).thenAnswer(o -> mock(TapeDriveConf.class));
        TapeCatalog currentTape = new TapeCatalog()
            .setLibrary(FAKE_LIBRARY)
            .setCode("tape")
            .setCurrentLocation(new TapeLocation(DRIVE_INDEX, TapeLocationType.DRIVE))
            .setPreviousLocation(new TapeLocation(SLOT_INDEX + 1, TapeLocationType.SLOT));

        TapeCatalog appropriateTape = new TapeCatalog()
            .setLibrary(FAKE_LIBRARY)
            .setCode(FAKE_TAPE_CODE)
            .setCurrentLocation(new TapeLocation(SLOT_INDEX, TapeLocationType.SLOT));

        ReadOrder readOrder = new ReadOrder().setTapeCode(FAKE_TAPE_CODE)
            .setFilePosition(FILE_POSITION)
            .setFileName(fileName);

        ReadTask readTask =
            new ReadTask(readOrder, currentTape, new TapeLibraryServiceImpl(tapeDriveService, tapeRobotPool),
                tapeCatalogService, readRequestReferentialRepository, archiveOutputRetentionPolicy);

        doAnswer(
            invocationOnMock -> {
                fileTest = Files.createFile(Paths.get(outputDirectory + File.separator + fileName + ReadTask.TEMP_EXT));
                return new TapeResponse(StatusCode.OK);
            }
        ).when(tapeReadWriteService).readFromTape(any());

        when(tapeDriveService.getDriveCommandService())
            .thenReturn(tapeDriveCommandService);
        when(tapeDriveService.getDriveCommandService().rewind())
            .thenReturn(new TapeResponse(StatusCode.OK));
        when(tapeDriveService.getDriveCommandService().move(anyInt(), anyBoolean()))
            .thenReturn(new TapeResponse(StatusCode.OK));
        when(tapeDriveService.getDriveCommandService().move(anyInt(), anyBoolean()))
            .thenReturn(new TapeResponse(StatusCode.OK));
        when(tapeCatalogService.receive(any(), any()))
            .thenReturn(Optional.of(appropriateTape));
        when(tapeDriveService.getTapeDriveConf().getIndex())
            .thenReturn(DRIVE_INDEX);
        when(tapeRobotPool.checkoutRobotService())
            .thenReturn(mock(TapeRobotService.class));
        when(tapeRobotPool.checkoutRobotService().getLoadUnloadService())
            .thenReturn(mock(TapeLoadUnloadService.class));
        when(tapeRobotPool.checkoutRobotService().getLoadUnloadService()
            .loadTape(anyInt(), anyInt())).thenReturn(new TapeResponse(StatusCode.OK));
        when(tapeDriveCommandService.eject()).thenReturn(new TapeResponse(StatusCode.OK));
        when(tapeRobotPool.checkoutRobotService().getLoadUnloadService()
            .unloadTape(anyInt(), anyInt())).thenReturn(new TapeResponse(StatusCode.OK));

        // Case one current t
        ReadWriteResult result = readTask.get();

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(StatusCode.OK);
        assertThat(result.getOrderState()).isEqualTo(QueueState.COMPLETED);
        assertThat(result.getCurrentTape()).isNotNull();
        assertThat(result.getCurrentTape().getCode()).isEqualTo(FAKE_TAPE_CODE);
        assertThat(
            result.getCurrentTape().getCurrentLocation().equals(new TapeLocation(DRIVE_INDEX, TapeLocationType.DRIVE)))
            .isEqualTo(true);
        assertThat(currentTape.getCurrentLocation().equals(new TapeLocation(SLOT_INDEX + 1, TapeLocationType.SLOT)))
            .isEqualTo(true);
        assertThat(result.getCurrentTape().getCurrentPosition()).isEqualTo(FILE_POSITION + 1);
    }

    @Test
    public void testReadTaskWhenCurrentTapeIsNotNullAndNotEligibleAndTheEligibleTapeIsBusy()
        throws InterruptedException, QueueException, TapeCatalogException {
        // When
        when(tapeDriveService.getTapeDriveConf()).thenAnswer(o -> mock(TapeDriveConf.class));
        TapeCatalog currentTape = new TapeCatalog()
            .setLibrary(FAKE_LIBRARY)
            .setCode("tape")
            .setCurrentLocation(new TapeLocation(DRIVE_INDEX, TapeLocationType.DRIVE))
            .setPreviousLocation(new TapeLocation(SLOT_INDEX + 1, TapeLocationType.SLOT));

        TapeCatalog appropriateTape = new TapeCatalog()
            .setLibrary(FAKE_LIBRARY)
            .setCode(FAKE_TAPE_CODE)
            .setCurrentLocation(new TapeLocation(SLOT_INDEX, TapeLocationType.SLOT));

        ReadOrder readOrder = new ReadOrder().setTapeCode(FAKE_TAPE_CODE)
            .setFilePosition(FILE_POSITION)
            .setFileName(fileName);

        ReadTask readTask =
            new ReadTask(readOrder, currentTape, new TapeLibraryServiceImpl(tapeDriveService, tapeRobotPool),
                tapeCatalogService, readRequestReferentialRepository, archiveOutputRetentionPolicy);

        when(tapeReadWriteService.readFromTape(startsWith(readOrder.getFileName())))
            .thenReturn(new TapeResponse(StatusCode.OK));
        when(tapeDriveService.getDriveCommandService())
            .thenReturn(tapeDriveCommandService);
        when(tapeDriveService.getDriveCommandService().rewind())
            .thenReturn(new TapeResponse(StatusCode.OK));
        when(tapeDriveService.getDriveCommandService().move(anyInt(), anyBoolean()))
            .thenReturn(new TapeResponse(StatusCode.OK));
        when(tapeDriveService.getDriveCommandService().move(anyInt(), anyBoolean()))
            .thenReturn(new TapeResponse(StatusCode.OK));
        when(tapeCatalogService.receive(any(), any()))
            .thenReturn(Optional.empty());
        when(tapeCatalogService.find(any()))
            .thenReturn(Arrays.asList(appropriateTape));
        when(tapeDriveService.getTapeDriveConf().getIndex())
            .thenReturn(DRIVE_INDEX);
        when(tapeRobotPool.checkoutRobotService())
            .thenReturn(mock(TapeRobotService.class));
        when(tapeRobotPool.checkoutRobotService().getLoadUnloadService())
            .thenReturn(mock(TapeLoadUnloadService.class));
        when(tapeRobotPool.checkoutRobotService().getLoadUnloadService()
            .loadTape(anyInt(), anyInt())).thenReturn(new TapeResponse(StatusCode.OK));
        when(tapeRobotPool.checkoutRobotService().getLoadUnloadService()
            .unloadTape(anyInt(), anyInt())).thenReturn(new TapeResponse(StatusCode.OK));

        when(tapeDriveCommandService.eject()).thenReturn(new TapeResponse(StatusCode.OK));

        // Case one current t
        ReadWriteResult result = readTask.get();

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(StatusCode.FATAL);
        assertThat(result.getOrderState()).isEqualTo(QueueState.ERROR);
        assertThat(result.getCurrentTape()).isNull();
    }

    @Test
    public void testReadTaskWhenCurrentTapeIsNotNullAndNotEligibleAndTheEligibleTapeIsUnknown()
        throws InterruptedException, QueueException, TapeCatalogException {
        // When
        when(tapeDriveService.getTapeDriveConf()).thenAnswer(o -> mock(TapeDriveConf.class));
        TapeCatalog currentTape = new TapeCatalog()
            .setLibrary(FAKE_LIBRARY)
            .setCode("tape")
            .setCurrentLocation(new TapeLocation(DRIVE_INDEX, TapeLocationType.DRIVE))
            .setPreviousLocation(new TapeLocation(SLOT_INDEX + 1, TapeLocationType.SLOT));

        ReadOrder readOrder = new ReadOrder().setTapeCode(FAKE_TAPE_CODE)
            .setFilePosition(FILE_POSITION)
            .setFileName(fileName);

        ReadTask readTask =
            new ReadTask(readOrder, currentTape, new TapeLibraryServiceImpl(tapeDriveService, tapeRobotPool),
                tapeCatalogService, readRequestReferentialRepository, archiveOutputRetentionPolicy);

        when(tapeReadWriteService.readFromTape(startsWith(readOrder.getFileName())))
            .thenReturn(new TapeResponse(StatusCode.OK));
        when(tapeDriveService.getDriveCommandService())
            .thenReturn(tapeDriveCommandService);
        when(tapeDriveService.getDriveCommandService().rewind())
            .thenReturn(new TapeResponse(StatusCode.OK));
        when(tapeDriveService.getDriveCommandService().move(anyInt(), anyBoolean()))
            .thenReturn(new TapeResponse(StatusCode.OK));
        when(tapeDriveService.getDriveCommandService().move(anyInt(), anyBoolean()))
            .thenReturn(new TapeResponse(StatusCode.OK));
        when(tapeCatalogService.receive(any(), any()))
            .thenReturn(Optional.empty());
        when(tapeCatalogService.find(any()))
            .thenReturn(Collections.emptyList());
        when(tapeDriveService.getTapeDriveConf().getIndex())
            .thenReturn(DRIVE_INDEX);
        when(tapeRobotPool.checkoutRobotService())
            .thenReturn(mock(TapeRobotService.class));
        when(tapeRobotPool.checkoutRobotService().getLoadUnloadService())
            .thenReturn(mock(TapeLoadUnloadService.class));
        when(tapeRobotPool.checkoutRobotService().getLoadUnloadService()
            .loadTape(anyInt(), anyInt())).thenReturn(new TapeResponse(StatusCode.OK));
        when(tapeDriveCommandService.eject()).thenReturn(new TapeResponse(StatusCode.OK));
        when(tapeRobotPool.checkoutRobotService().getLoadUnloadService()
            .unloadTape(anyInt(), anyInt())).thenReturn(new TapeResponse(StatusCode.OK));


        // Case one current t
        ReadWriteResult result = readTask.get();

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(StatusCode.FATAL);
        assertThat(result.getOrderState()).isEqualTo(QueueState.ERROR);
        assertThat(result.getCurrentTape()).isNull();
    }

    @Test
    public void testReadTaskWhenCurrentTapeIsNotNullAndNotEligibleAndTapeCatalogThrownException()
        throws InterruptedException, QueueException {
        // When
        when(tapeDriveService.getTapeDriveConf()).thenAnswer(o -> mock(TapeDriveConf.class));
        TapeCatalog currentTape = new TapeCatalog()
            .setLibrary(FAKE_LIBRARY)
            .setCode("tape")
            .setCurrentLocation(new TapeLocation(DRIVE_INDEX, TapeLocationType.DRIVE))
            .setPreviousLocation(new TapeLocation(SLOT_INDEX + 1, TapeLocationType.SLOT));

        ReadOrder readOrder = new ReadOrder().setTapeCode(FAKE_TAPE_CODE)
            .setFilePosition(FILE_POSITION)
            .setFileName(fileName);

        ReadTask readTask =
            new ReadTask(readOrder, currentTape, new TapeLibraryServiceImpl(tapeDriveService, tapeRobotPool),
                tapeCatalogService, readRequestReferentialRepository, archiveOutputRetentionPolicy);

        when(tapeReadWriteService.readFromTape(startsWith(readOrder.getFileName())))
            .thenReturn(new TapeResponse(StatusCode.OK));
        when(tapeDriveService.getDriveCommandService())
            .thenReturn(tapeDriveCommandService);
        when(tapeDriveService.getDriveCommandService().rewind())
            .thenReturn(new TapeResponse(StatusCode.OK));
        when(tapeDriveService.getDriveCommandService().move(anyInt(), anyBoolean()))
            .thenReturn(new TapeResponse(StatusCode.OK));
        when(tapeDriveService.getDriveCommandService().move(anyInt(), anyBoolean()))
            .thenReturn(new TapeResponse(StatusCode.OK));
        when(tapeCatalogService.receive(any(), any()))
            .thenThrow(QueueException.class);
        when(tapeDriveService.getTapeDriveConf().getIndex())
            .thenReturn(DRIVE_INDEX);
        when(tapeRobotPool.checkoutRobotService())
            .thenReturn(mock(TapeRobotService.class));
        when(tapeRobotPool.checkoutRobotService().getLoadUnloadService())
            .thenReturn(mock(TapeLoadUnloadService.class));
        when(tapeRobotPool.checkoutRobotService().getLoadUnloadService()
            .loadTape(anyInt(), anyInt())).thenReturn(new TapeResponse(StatusCode.OK));
        when(tapeDriveCommandService.eject()).thenReturn(new TapeResponse(StatusCode.OK));
        when(tapeRobotPool.checkoutRobotService().getLoadUnloadService()
            .unloadTape(anyInt(), anyInt())).thenReturn(new TapeResponse(StatusCode.OK));

        // Case one current t
        ReadWriteResult result = readTask.get();

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(StatusCode.KO);
        assertThat(result.getOrderState()).isEqualTo(QueueState.READY);
        assertThat(result.getCurrentTape()).isNull();
    }

    @Test
    public void testReadTaskWhenCurrentTapeIsNotNullAndNotEligibleAndTheEligibleTapeIsOutside()
        throws InterruptedException, QueueException {
        // When
        when(tapeDriveService.getTapeDriveConf()).thenAnswer(o -> mock(TapeDriveConf.class));
        TapeCatalog currentTape = new TapeCatalog()
            .setLibrary(FAKE_LIBRARY)
            .setCode("tape")
            .setCurrentLocation(new TapeLocation(DRIVE_INDEX, TapeLocationType.DRIVE))
            .setPreviousLocation(new TapeLocation(SLOT_INDEX + 1, TapeLocationType.SLOT));

        TapeCatalog appropriateTape = new TapeCatalog()
            .setLibrary(FAKE_LIBRARY)
            .setCode(FAKE_TAPE_CODE)
            .setCurrentLocation(new TapeLocation(null, TapeLocationType.OUTSIDE));

        ReadOrder readOrder = new ReadOrder().setTapeCode(FAKE_TAPE_CODE)
            .setFilePosition(FILE_POSITION)
            .setFileName(fileName);

        ReadTask readTask =
            new ReadTask(readOrder, currentTape, new TapeLibraryServiceImpl(tapeDriveService, tapeRobotPool),
                tapeCatalogService, readRequestReferentialRepository, archiveOutputRetentionPolicy);

        when(tapeReadWriteService.readFromTape(startsWith(readOrder.getFileName())))
            .thenReturn(new TapeResponse(StatusCode.OK));
        when(tapeDriveService.getDriveCommandService())
            .thenReturn(tapeDriveCommandService);
        when(tapeDriveService.getDriveCommandService().rewind())
            .thenReturn(new TapeResponse(StatusCode.OK));
        when(tapeDriveService.getDriveCommandService().move(anyInt(), anyBoolean()))
            .thenReturn(new TapeResponse(StatusCode.OK));
        when(tapeDriveService.getDriveCommandService().move(anyInt(), anyBoolean()))
            .thenReturn(new TapeResponse(StatusCode.OK));
        when(tapeCatalogService.receive(any(), any()))
            .thenReturn(Optional.of(appropriateTape));
        when(tapeDriveService.getTapeDriveConf().getIndex())
            .thenReturn(DRIVE_INDEX);
        when(tapeRobotPool.checkoutRobotService())
            .thenReturn(mock(TapeRobotService.class));
        when(tapeRobotPool.checkoutRobotService().getLoadUnloadService())
            .thenReturn(mock(TapeLoadUnloadService.class));
        when(tapeRobotPool.checkoutRobotService().getLoadUnloadService()
            .loadTape(anyInt(), anyInt())).thenReturn(new TapeResponse(StatusCode.OK));
        when(tapeRobotPool.checkoutRobotService().getLoadUnloadService()
            .unloadTape(anyInt(), anyInt())).thenReturn(new TapeResponse(StatusCode.OK));

        when(tapeDriveCommandService.eject()).thenReturn(new TapeResponse(StatusCode.OK));

        // Case one current t
        ReadWriteResult result = readTask.get();

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(StatusCode.FATAL);
        assertThat(result.getOrderState()).isEqualTo(QueueState.ERROR);
        assertThat(result.getCurrentTape()).isNull();
    }


    @Test
    public void testReadTaskWhenCurrentTapeIsNotNullAndNotEligibleThenErrorWhenLoadingTheEligibleTape()
        throws InterruptedException, QueueException {
        // When
        when(tapeDriveService.getTapeDriveConf()).thenAnswer(o -> mock(TapeDriveConf.class));
        TapeCatalog currentTape = new TapeCatalog()
            .setLibrary(FAKE_LIBRARY)
            .setCode("tape")
            .setCurrentLocation(new TapeLocation(DRIVE_INDEX, TapeLocationType.DRIVE))
            .setPreviousLocation(new TapeLocation(SLOT_INDEX + 1, TapeLocationType.SLOT));

        TapeCatalog appropriateTape = new TapeCatalog()
            .setLibrary(FAKE_LIBRARY)
            .setCode(FAKE_TAPE_CODE)
            .setCurrentLocation(new TapeLocation(SLOT_INDEX, TapeLocationType.SLOT));

        ReadOrder readOrder = new ReadOrder().setTapeCode(FAKE_TAPE_CODE)
            .setFilePosition(FILE_POSITION)
            .setFileName(fileName);

        ReadTask readTask =
            new ReadTask(readOrder, currentTape, new TapeLibraryServiceImpl(tapeDriveService, tapeRobotPool),
                tapeCatalogService, readRequestReferentialRepository, archiveOutputRetentionPolicy);

        when(tapeReadWriteService.readFromTape(startsWith(readOrder.getFileName())))
            .thenReturn(new TapeResponse(StatusCode.OK));
        when(tapeDriveService.getDriveCommandService())
            .thenReturn(tapeDriveCommandService);
        when(tapeDriveService.getDriveCommandService().rewind())
            .thenReturn(new TapeResponse(StatusCode.OK));
        when(tapeDriveService.getDriveCommandService().move(anyInt(), anyBoolean()))
            .thenReturn(new TapeResponse(StatusCode.OK));
        when(tapeDriveService.getDriveCommandService().move(anyInt(), anyBoolean()))
            .thenReturn(new TapeResponse(StatusCode.OK));
        when(tapeCatalogService.receive(any(), any()))
            .thenReturn(Optional.of(appropriateTape));
        when(tapeDriveService.getTapeDriveConf().getIndex())
            .thenReturn(DRIVE_INDEX);
        when(tapeRobotPool.checkoutRobotService())
            .thenReturn(mock(TapeRobotService.class));
        when(tapeRobotPool.checkoutRobotService().getLoadUnloadService())
            .thenReturn(mock(TapeLoadUnloadService.class));
        when(tapeRobotPool.checkoutRobotService().getLoadUnloadService()
            .loadTape(anyInt(), anyInt())).thenReturn(new TapeResponse(StatusCode.FATAL));
        when(tapeDriveCommandService.eject()).thenReturn(new TapeResponse(StatusCode.OK));
        when(tapeRobotPool.checkoutRobotService().getLoadUnloadService()
            .unloadTape(anyInt(), anyInt())).thenReturn(new TapeResponse(StatusCode.OK));
        when(tapeDriveCommandService.status())
            .thenReturn(new TapeDriveState(StatusCode.OK));

        // Case one current t
        ReadWriteResult result = readTask.get();

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(StatusCode.FATAL);
        assertThat(result.getOrderState()).isEqualTo(QueueState.ERROR);
        assertThat(result.getCurrentTape()).isNull();
    }


    @Test
    public void testReadTaskWhenCurrentTapeIsNotNullAndNotEligibleThenErrorWhenUnloadingTheCurrentTape()
        throws InterruptedException, QueueException {
        // When
        when(tapeDriveService.getTapeDriveConf()).thenAnswer(o -> mock(TapeDriveConf.class));
        TapeCatalog currentTape = new TapeCatalog()
            .setLibrary(FAKE_LIBRARY)
            .setCode("tape")
            .setCurrentLocation(new TapeLocation(DRIVE_INDEX, TapeLocationType.DRIVE))
            .setPreviousLocation(new TapeLocation(SLOT_INDEX + 1, TapeLocationType.SLOT));

        TapeCatalog appropriateTape = new TapeCatalog()
            .setLibrary(FAKE_LIBRARY)
            .setCode(FAKE_TAPE_CODE)
            .setCurrentLocation(new TapeLocation(SLOT_INDEX, TapeLocationType.SLOT));

        ReadOrder readOrder = new ReadOrder().setTapeCode(FAKE_TAPE_CODE)
            .setFilePosition(FILE_POSITION)
            .setFileName(fileName);

        ReadTask readTask =
            new ReadTask(readOrder, currentTape, new TapeLibraryServiceImpl(tapeDriveService, tapeRobotPool),
                tapeCatalogService, readRequestReferentialRepository, archiveOutputRetentionPolicy);

        when(tapeReadWriteService.readFromTape(startsWith(readOrder.getFileName())))
            .thenReturn(new TapeResponse(StatusCode.OK));
        when(tapeDriveService.getDriveCommandService())
            .thenReturn(tapeDriveCommandService);
        when(tapeDriveService.getDriveCommandService().rewind())
            .thenReturn(new TapeResponse(StatusCode.OK));
        when(tapeDriveService.getDriveCommandService().move(anyInt(), anyBoolean()))
            .thenReturn(new TapeResponse(StatusCode.OK));
        when(tapeDriveService.getDriveCommandService().move(anyInt(), anyBoolean()))
            .thenReturn(new TapeResponse(StatusCode.OK));
        when(tapeCatalogService.receive(any(), any()))
            .thenReturn(Optional.of(appropriateTape));
        when(tapeDriveService.getTapeDriveConf().getIndex())
            .thenReturn(DRIVE_INDEX);
        when(tapeRobotPool.checkoutRobotService())
            .thenReturn(mock(TapeRobotService.class));
        when(tapeRobotPool.checkoutRobotService().getLoadUnloadService())
            .thenReturn(mock(TapeLoadUnloadService.class));
        when(tapeDriveCommandService.eject()).thenReturn(new TapeResponse(StatusCode.OK));
        when(tapeRobotPool.checkoutRobotService().getLoadUnloadService()
            .unloadTape(anyInt(), anyInt())).thenReturn(new TapeResponse(StatusCode.FATAL));
        TapeDriveState tapeDriveState = new TapeDriveState(StatusCode.OK);
        tapeDriveState.setDriveStatuses(Arrays.asList(TapeDriveStatus.ONLINE));
        when(tapeDriveCommandService.status())
            .thenReturn(tapeDriveState);
        when(tapeRobotPool.checkoutRobotService().getLoadUnloadService()
            .loadTape(anyInt(), anyInt())).thenReturn(new TapeResponse(StatusCode.OK));

        // Case one current t
        ReadWriteResult result = readTask.get();

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(StatusCode.FATAL);
        assertThat(result.getOrderState()).isEqualTo(QueueState.READY);
        assertThat(result.getCurrentTape()).isNotNull();
    }


    @Test
    public void testReadTaskWhenCurrentTapeIsNotEligibleAndLoadEligibleTapeThenErrorWhenReadIt()
        throws InterruptedException, QueueException {
        // When
        when(tapeDriveService.getTapeDriveConf()).thenAnswer(o -> mock(TapeDriveConf.class));
        TapeCatalog currentTape = new TapeCatalog()
            .setLibrary(FAKE_LIBRARY)
            .setCode("tape")
            .setCurrentLocation(new TapeLocation(DRIVE_INDEX, TapeLocationType.DRIVE))
            .setPreviousLocation(new TapeLocation(SLOT_INDEX + 1, TapeLocationType.SLOT));

        TapeCatalog appropriateTape = new TapeCatalog()
            .setLibrary(FAKE_LIBRARY)
            .setCode(FAKE_TAPE_CODE)
            .setCurrentLocation(new TapeLocation(SLOT_INDEX, TapeLocationType.SLOT));

        ReadOrder readOrder = new ReadOrder().setTapeCode(FAKE_TAPE_CODE)
            .setFilePosition(FILE_POSITION)
            .setFileName(fileName);

        ReadTask readTask =
            new ReadTask(readOrder, currentTape, new TapeLibraryServiceImpl(tapeDriveService, tapeRobotPool),
                tapeCatalogService, readRequestReferentialRepository, archiveOutputRetentionPolicy);

        when(tapeReadWriteService.readFromTape(startsWith(readOrder.getFileName())))
            .thenReturn(new TapeResponse(StatusCode.FATAL));
        when(tapeDriveService.getDriveCommandService())
            .thenReturn(tapeDriveCommandService);
        when(tapeDriveService.getDriveCommandService().rewind())
            .thenReturn(new TapeResponse(StatusCode.OK));
        when(tapeDriveService.getDriveCommandService().move(anyInt(), anyBoolean()))
            .thenReturn(new TapeResponse(StatusCode.OK));
        when(tapeDriveService.getDriveCommandService().move(anyInt(), anyBoolean()))
            .thenReturn(new TapeResponse(StatusCode.OK));
        when(tapeCatalogService.receive(any(), any()))
            .thenReturn(Optional.of(appropriateTape));
        when(tapeDriveService.getTapeDriveConf().getIndex())
            .thenReturn(DRIVE_INDEX);
        when(tapeRobotPool.checkoutRobotService())
            .thenReturn(mock(TapeRobotService.class));
        when(tapeDriveCommandService.eject()).thenReturn(new TapeResponse(StatusCode.OK));
        when(tapeRobotPool.checkoutRobotService().getLoadUnloadService())
            .thenReturn(mock(TapeLoadUnloadService.class));
        when(tapeRobotPool.checkoutRobotService().getLoadUnloadService()
            .unloadTape(anyInt(), anyInt())).thenReturn(new TapeResponse(StatusCode.OK));
        when(tapeDriveCommandService.status())
            .thenReturn(new TapeDriveState(StatusCode.OK));
        when(tapeRobotPool.checkoutRobotService().getLoadUnloadService()
            .loadTape(anyInt(), anyInt())).thenReturn(new TapeResponse(StatusCode.OK));

        // Case one current t
        ReadWriteResult result = readTask.get();

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(StatusCode.FATAL);
        assertThat(result.getOrderState()).isEqualTo(QueueState.ERROR);
        assertThat(result.getCurrentTape()).isNotNull();
    }


    @Test
    public void testReadTaskCurrentTapeIsNullAndEligibleTapeFound() throws QueueException, InterruptedException {
        // When
        when(tapeDriveService.getTapeDriveConf()).thenAnswer(o -> mock(TapeDriveConf.class));
        TapeCatalog tapeCatalog = new TapeCatalog()
            .setLibrary(FAKE_LIBRARY)
            .setCode(FAKE_TAPE_CODE)
            .setCurrentLocation(new TapeLocation(SLOT_INDEX, TapeLocationType.SLOT));

        ReadOrder readOrder = new ReadOrder().setTapeCode(FAKE_TAPE_CODE)
            .setFilePosition(FILE_POSITION)
            .setFileName(fileName);

        ReadTask readTask =
            new ReadTask(readOrder, null, new TapeLibraryServiceImpl(tapeDriveService, tapeRobotPool),
                tapeCatalogService, readRequestReferentialRepository, archiveOutputRetentionPolicy);

        doAnswer(
            invocationOnMock -> {
                fileTest = Files.createFile(Paths.get(outputDirectory + File.separator + fileName + ReadTask.TEMP_EXT));
                return new TapeResponse(StatusCode.OK);
            }
        ).when(tapeReadWriteService).readFromTape(any());

        when(tapeDriveService.getDriveCommandService())
            .thenReturn(tapeDriveCommandService);
        when(tapeDriveService.getDriveCommandService().rewind())
            .thenReturn(new TapeResponse(StatusCode.OK));
        when(tapeDriveService.getDriveCommandService().move(anyInt(), anyBoolean()))
            .thenReturn(new TapeResponse(StatusCode.OK));
        when(tapeDriveService.getDriveCommandService().move(anyInt(), anyBoolean()))
            .thenReturn(new TapeResponse(StatusCode.OK));
        when(tapeCatalogService.receive(any(), any()))
            .thenReturn(Optional.of(tapeCatalog));
        when(tapeDriveService.getTapeDriveConf().getIndex())
            .thenReturn(DRIVE_INDEX);
        when(tapeRobotPool.checkoutRobotService())
            .thenReturn(mock(TapeRobotService.class));
        when(tapeRobotPool.checkoutRobotService().getLoadUnloadService())
            .thenReturn(mock(TapeLoadUnloadService.class));
        when(tapeRobotPool.checkoutRobotService().getLoadUnloadService()
            .loadTape(anyInt(), anyInt())).thenReturn(new TapeResponse(StatusCode.OK));

        when(tapeDriveCommandService.eject()).thenReturn(new TapeResponse(StatusCode.OK));

        // Case one current t
        ReadWriteResult result = readTask.get();

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(StatusCode.OK);
        assertThat(result.getOrderState()).isEqualTo(QueueState.COMPLETED);
        assertThat(result.getCurrentTape()).isNotNull();
        assertThat(result.getCurrentTape().getCode()).isEqualTo(FAKE_TAPE_CODE);
        assertThat(result.getCurrentTape().getCurrentLocation()
            .equals(new TapeLocation(DRIVE_INDEX, TapeLocationType.DRIVE))).isEqualTo(true);
        assertThat(result.getCurrentTape().getCurrentPosition()).isEqualTo(FILE_POSITION + 1);

    }
}