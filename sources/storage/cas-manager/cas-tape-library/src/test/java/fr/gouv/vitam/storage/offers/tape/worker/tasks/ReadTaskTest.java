/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ReadTaskTest {

    public static final String FAKE_LIBRARY = "fakeLibrary";
    public static final String FAKE_TAPE_CODE = "fakeTapeCode";
    public static final Integer SLOT_INDEX = 1;
    public static final Integer DRIVE_INDEX = 0;
    private static final Integer FILE_POSITION = 0;
    private static final String FAKE_BUCKET = "fakeBucket";
    private static final String FAKE_FILE_BUCKET_ID = "fakeFileBucketId";
    private static final long FILE_SIZE = 1_234_567_890_123L;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

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
    private File outputDir;

    @Before
    public void setUp() throws IOException {
        when(tapeDriveService.getReadWriteService(eq(TapeDriveService.ReadWriteCmd.DD)))
            .thenAnswer(o -> tapeReadWriteService);

        outputDir = temporaryFolder.newFolder("outputTars");

        fileName = GUIDFactory.newGUID().getId() + ".tar";
        when(tapeReadWriteService.getOutputDirectory()).thenReturn(outputDir.getAbsolutePath());
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
        assertThatThrownBy(() -> new ReadTask(null, mock(TapeCatalog.class), mock(TapeLibraryServiceImpl.class),
            mock(TapeCatalogService.class), mock(ReadRequestReferentialRepository.class),
            archiveOutputRetentionPolicy)
        ).withFailMessage("read order is required")
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new ReadTask(mock(ReadOrder.class), mock(TapeCatalog.class), null,
            mock(TapeCatalogService.class), mock(ReadRequestReferentialRepository.class),
            archiveOutputRetentionPolicy)
        ).withFailMessage("tape library service is required")
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new ReadTask(mock(ReadOrder.class), mock(TapeCatalog.class),
            mock(TapeLibraryServiceImpl.class), null, mock(ReadRequestReferentialRepository.class),
            archiveOutputRetentionPolicy)
        ).withFailMessage("tape catalog service is required")
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new ReadTask(mock(ReadOrder.class), mock(TapeCatalog.class),
            mock(TapeLibraryServiceImpl.class), mock(TapeCatalogService.class), null, archiveOutputRetentionPolicy)
        ).withFailMessage("read request repository is required")
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new ReadTask(mock(ReadOrder.class), mock(TapeCatalog.class),
            mock(TapeLibraryServiceImpl.class), mock(TapeCatalogService.class),
            mock(ReadRequestReferentialRepository.class), null)
        ).withFailMessage("read request archiveOutputRetentionPolicy is required")
            .isInstanceOf(IllegalArgumentException.class);
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

        ReadOrder readOrder = new ReadOrder(FAKE_TAPE_CODE, FILE_POSITION, fileName, FAKE_BUCKET, FAKE_FILE_BUCKET_ID,
            FILE_SIZE);

        ReadTask readTask =
            new ReadTask(readOrder, tapeCatalog, new TapeLibraryServiceImpl(tapeDriveService, tapeRobotPool),
                tapeCatalogService, readRequestReferentialRepository, archiveOutputRetentionPolicy);

        doAnswer(
            invocationOnMock -> {
                fileTest = Files.createFile(outputDir.toPath().resolve(fileName + ReadTask.TEMP_EXT));
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
    public void testReadTaskWhenCurrentTapeIsNotNullAndNotEligibleAndTheEligibleTapeIsAvailable() throws Exception {
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

        ReadOrder readOrder = new ReadOrder(FAKE_TAPE_CODE, FILE_POSITION, fileName, FAKE_BUCKET, FAKE_FILE_BUCKET_ID,
            FILE_SIZE);

        ReadTask readTask =
            new ReadTask(readOrder, currentTape, new TapeLibraryServiceImpl(tapeDriveService, tapeRobotPool),
                tapeCatalogService, readRequestReferentialRepository, archiveOutputRetentionPolicy);

        doAnswer(
            invocationOnMock -> {
                fileTest = Files.createFile(outputDir.toPath().resolve(fileName + ReadTask.TEMP_EXT));
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

        ReadOrder readOrder = new ReadOrder(FAKE_TAPE_CODE, FILE_POSITION, fileName, FAKE_BUCKET, FAKE_FILE_BUCKET_ID,
            FILE_SIZE);

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
            .thenReturn(List.of(appropriateTape));
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
        assertThat(result.getStatus()).isEqualTo(StatusCode.KO);
        assertThat(result.getOrderState()).isEqualTo(QueueState.READY);
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

        ReadOrder readOrder = new ReadOrder(FAKE_TAPE_CODE, FILE_POSITION, fileName, FAKE_BUCKET, FAKE_FILE_BUCKET_ID,
            FILE_SIZE);

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

        ReadOrder readOrder = new ReadOrder(FAKE_TAPE_CODE, FILE_POSITION, fileName, FAKE_BUCKET, FAKE_FILE_BUCKET_ID,
            FILE_SIZE);

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

        ReadOrder readOrder = new ReadOrder(FAKE_TAPE_CODE, FILE_POSITION, fileName, FAKE_BUCKET, FAKE_FILE_BUCKET_ID,
            FILE_SIZE);

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
        throws Exception {
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

        ReadOrder readOrder = new ReadOrder(FAKE_TAPE_CODE, FILE_POSITION, fileName, FAKE_BUCKET, FAKE_FILE_BUCKET_ID,
            FILE_SIZE);

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
        throws Exception {
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

        ReadOrder readOrder = new ReadOrder(FAKE_TAPE_CODE, FILE_POSITION, fileName, FAKE_BUCKET, FAKE_FILE_BUCKET_ID,
            FILE_SIZE);

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
        tapeDriveState.setDriveStatuses(List.of(TapeDriveStatus.ONLINE));
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
    public void testReadTaskWhenCurrentTapeIsNotEligibleAndLoadEligibleTapeThenErrorWhenReadIt() throws Exception {
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

        ReadOrder readOrder = new ReadOrder(FAKE_TAPE_CODE, FILE_POSITION, fileName, FAKE_BUCKET, FAKE_FILE_BUCKET_ID,
            FILE_SIZE);

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
    public void testReadTaskCurrentTapeIsNullAndEligibleTapeFound() throws Exception {
        // When
        when(tapeDriveService.getTapeDriveConf()).thenAnswer(o -> mock(TapeDriveConf.class));
        TapeCatalog tapeCatalog = new TapeCatalog()
            .setLibrary(FAKE_LIBRARY)
            .setCode(FAKE_TAPE_CODE)
            .setCurrentLocation(new TapeLocation(SLOT_INDEX, TapeLocationType.SLOT));

        ReadOrder readOrder = new ReadOrder(FAKE_TAPE_CODE, FILE_POSITION, fileName, FAKE_BUCKET, FAKE_FILE_BUCKET_ID,
            FILE_SIZE);

        ReadTask readTask =
            new ReadTask(readOrder, null, new TapeLibraryServiceImpl(tapeDriveService, tapeRobotPool),
                tapeCatalogService, readRequestReferentialRepository, archiveOutputRetentionPolicy);

        doAnswer(
            invocationOnMock -> {
                fileTest = Files.createFile(outputDir.toPath().resolve(fileName + ReadTask.TEMP_EXT));
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
