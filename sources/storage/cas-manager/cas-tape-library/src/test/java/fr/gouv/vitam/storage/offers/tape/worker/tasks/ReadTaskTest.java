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
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.storage.tapelibrary.TapeDriveConf;
import fr.gouv.vitam.storage.engine.common.model.QueueState;
import fr.gouv.vitam.storage.engine.common.model.ReadOrder;
import fr.gouv.vitam.storage.engine.common.model.TapeCatalog;
import fr.gouv.vitam.storage.engine.common.model.TapeCatalogLabel;
import fr.gouv.vitam.storage.engine.common.model.TapeLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeLocationType;
import fr.gouv.vitam.storage.offers.tape.cas.AccessRequestManager;
import fr.gouv.vitam.storage.offers.tape.cas.ArchiveCacheStorage;
import fr.gouv.vitam.storage.offers.tape.dto.TapeDriveState;
import fr.gouv.vitam.storage.offers.tape.dto.TapeDriveStatus;
import fr.gouv.vitam.storage.offers.tape.exception.AccessRequestReferentialException;
import fr.gouv.vitam.storage.offers.tape.exception.QueueException;
import fr.gouv.vitam.storage.offers.tape.exception.TapeCommandException;
import fr.gouv.vitam.storage.offers.tape.impl.readwrite.TapeLibraryServiceImpl;
import fr.gouv.vitam.storage.offers.tape.spec.TapeCatalogService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeDriveCommandService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeDriveService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeLoadUnloadService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeReadWriteService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeRobotPool;
import fr.gouv.vitam.storage.offers.tape.spec.TapeRobotService;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class ReadTaskTest {

    public static final String FAKE_LIBRARY = "fakeLibrary";
    public static final String FAKE_TAPE_CODE = "fakeTapeCode";
    public static final Integer SLOT_INDEX = 1;
    public static final Integer DRIVE_INDEX = 0;
    private static final Integer FILE_POSITION = 12;
    private static final String FAKE_BUCKET = "fakeBucket";
    private static final String FAKE_FILE_BUCKET_ID = "fakeFileBucketId";
    private static final long FILE_SIZE = 1_234_567_890_123L;
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
    private TapeCatalogService tapeCatalogService;

    @Mock
    private AccessRequestManager accessRequestManager;

    @Mock
    private TapeReadWriteService tapeReadWriteService;

    @Mock
    private TapeDriveCommandService tapeDriveCommandService;

    @Mock
    private ArchiveCacheStorage archiveCacheStorage;

    private Path fileTest;
    private String fileName;
    private File tmpTarOutputDir;

    @Before
    public void setUp() throws IOException {
        when(tapeDriveService.getReadWriteService()).thenAnswer(o -> tapeReadWriteService);

        tmpTarOutputDir = temporaryFolder.newFolder("tmpTarOutput");

        fileName = GUIDFactory.newGUID().getId() + ".tar";
        when(tapeReadWriteService.getTmpOutputStorageFolder()).thenReturn(tmpTarOutputDir.getAbsolutePath());
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
        assertThatThrownBy(
            () ->
                new ReadTask(
                    null,
                    mock(TapeCatalog.class),
                    mock(TapeLibraryServiceImpl.class),
                    mock(TapeCatalogService.class),
                    mock(AccessRequestManager.class),
                    archiveCacheStorage
                )
        )
            .withFailMessage("read order is required")
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(
            () ->
                new ReadTask(
                    mock(ReadOrder.class),
                    mock(TapeCatalog.class),
                    null,
                    mock(TapeCatalogService.class),
                    mock(AccessRequestManager.class),
                    archiveCacheStorage
                )
        )
            .withFailMessage("tape library service is required")
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(
            () ->
                new ReadTask(
                    mock(ReadOrder.class),
                    mock(TapeCatalog.class),
                    mock(TapeLibraryServiceImpl.class),
                    null,
                    mock(AccessRequestManager.class),
                    archiveCacheStorage
                )
        )
            .withFailMessage("tape catalog service is required")
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(
            () ->
                new ReadTask(
                    mock(ReadOrder.class),
                    mock(TapeCatalog.class),
                    mock(TapeLibraryServiceImpl.class),
                    mock(TapeCatalogService.class),
                    null,
                    archiveCacheStorage
                )
        )
            .withFailMessage("Archive storage manager is required")
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(
            () ->
                new ReadTask(
                    mock(ReadOrder.class),
                    mock(TapeCatalog.class),
                    mock(TapeLibraryServiceImpl.class),
                    mock(TapeCatalogService.class),
                    mock(AccessRequestManager.class),
                    null
                )
        )
            .withFailMessage("Archive cache storage is required")
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testReadTaskCurrentTapeIsNotNullAndEligible() throws Exception {
        // When
        TapeCatalogLabel tapeCatalogLabel = new TapeCatalogLabel().setCode(FAKE_TAPE_CODE).setBucket(FAKE_BUCKET);

        when(tapeDriveService.getTapeDriveConf()).thenAnswer(o -> mock(TapeDriveConf.class));
        TapeCatalog tapeCatalog = new TapeCatalog()
            .setLibrary(FAKE_LIBRARY)
            .setCode(FAKE_TAPE_CODE)
            .setCurrentPosition(FILE_POSITION)
            .setCurrentLocation(new TapeLocation(DRIVE_INDEX, TapeLocationType.DRIVE))
            .setLabel(tapeCatalogLabel);

        ReadOrder readOrder = new ReadOrder(
            FAKE_TAPE_CODE,
            FILE_POSITION,
            fileName,
            FAKE_BUCKET,
            FAKE_FILE_BUCKET_ID,
            FILE_SIZE
        );

        ReadTask readTask = new ReadTask(
            readOrder,
            tapeCatalog,
            new TapeLibraryServiceImpl(tapeDriveService, tapeRobotPool, FULL_CARTRIDGE_THRESHOLD),
            tapeCatalogService,
            accessRequestManager,
            archiveCacheStorage
        );

        fileTest = Files.createFile(tmpTarOutputDir.toPath().resolve(fileName + ReadTask.TEMP_EXT));

        doAnswer(args -> {
            String filePath = args.getArgument(0);
            FileUtils.write(tmpTarOutputDir.toPath().resolve(filePath).toFile(), "data", StandardCharsets.UTF_8);
            return null;
        })
            .when(tapeReadWriteService)
            .readFromTape(any());

        when(tapeDriveService.getDriveCommandService()).thenReturn(tapeDriveCommandService);
        doNothing().when(tapeDriveCommandService).rewind();
        doNothing().when(tapeDriveCommandService).move(anyInt(), anyBoolean());
        doNothing().when(tapeDriveCommandService).eject();

        doReturn(false).when(archiveCacheStorage).containsArchive(FAKE_FILE_BUCKET_ID, fileName);
        doNothing().when(archiveCacheStorage).reserveArchiveStorageSpace(FAKE_FILE_BUCKET_ID, fileName, FILE_SIZE);
        doAnswer(args -> {
            assertThat(fileTest).isNotNull();
            assertThat(args.getArgument(0, Path.class)).isEqualTo(fileTest);
            assertThat(fileTest).exists();
            assertThat(fileTest).hasContent("data");
            return null;
        })
            .when(archiveCacheStorage)
            .moveArchiveToCache(any(), eq(FAKE_FILE_BUCKET_ID), eq(fileName));

        // Case one current t
        ReadWriteResult result = readTask.get();

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(StatusCode.OK);
        assertThat(result.getOrderState()).isEqualTo(QueueState.COMPLETED);
        assertThat(result.getCurrentTape()).isNotNull();
        assertThat(result.getCurrentTape().getCode()).isEqualTo(FAKE_TAPE_CODE);
        assertThat(
            result.getCurrentTape().getCurrentLocation().equals(new TapeLocation(DRIVE_INDEX, TapeLocationType.DRIVE))
        ).isEqualTo(true);
        assertThat(result.getCurrentTape().getCurrentPosition()).isEqualTo(FILE_POSITION + 1);

        InOrder inOrder = Mockito.inOrder(tapeReadWriteService, tapeDriveCommandService);
        // Read file
        inOrder.verify(tapeReadWriteService).getTmpOutputStorageFolder();
        inOrder.verify(tapeReadWriteService).readFromTape(any());
        inOrder.verifyNoMoreInteractions();

        verify(archiveCacheStorage).containsArchive(FAKE_FILE_BUCKET_ID, fileName);
        verify(archiveCacheStorage).reserveArchiveStorageSpace(FAKE_FILE_BUCKET_ID, fileName, FILE_SIZE);
        verify(archiveCacheStorage).moveArchiveToCache(eq(fileTest), eq(FAKE_FILE_BUCKET_ID), eq(fileName));
        verifyNoMoreInteractions(archiveCacheStorage);

        verify(accessRequestManager).updateAccessRequestWhenArchiveReady(fileName);
        verifyNoMoreInteractions(accessRequestManager);
    }

    @Test
    public void testReadTaskWhenCurrentTapeIsNotNullAndNotEligibleAndTheEligibleTapeIsAvailableToReadFirstFileAfterLabel()
        throws Exception {
        // When

        when(tapeDriveService.getTapeDriveConf()).thenAnswer(o -> mock(TapeDriveConf.class));
        TapeCatalog currentTape = new TapeCatalog()
            .setLibrary(FAKE_LIBRARY)
            .setCode("tape")
            .setCurrentLocation(new TapeLocation(DRIVE_INDEX, TapeLocationType.DRIVE))
            .setPreviousLocation(new TapeLocation(SLOT_INDEX + 1, TapeLocationType.SLOT));

        TapeCatalogLabel tapeCatalogLabel = new TapeCatalogLabel().setCode(FAKE_TAPE_CODE).setBucket(FAKE_BUCKET);

        TapeCatalog appropriateTape = new TapeCatalog()
            .setLibrary(FAKE_LIBRARY)
            .setCode(FAKE_TAPE_CODE)
            .setCurrentLocation(new TapeLocation(SLOT_INDEX, TapeLocationType.SLOT))
            .setLabel(tapeCatalogLabel);

        int filePosition = 1;
        ReadOrder readOrder = new ReadOrder(
            FAKE_TAPE_CODE,
            filePosition,
            fileName,
            FAKE_BUCKET,
            FAKE_FILE_BUCKET_ID,
            FILE_SIZE
        );

        ReadTask readTask = new ReadTask(
            readOrder,
            currentTape,
            new TapeLibraryServiceImpl(tapeDriveService, tapeRobotPool, FULL_CARTRIDGE_THRESHOLD),
            tapeCatalogService,
            accessRequestManager,
            archiveCacheStorage
        );

        fileTest = Files.createFile(tmpTarOutputDir.toPath().resolve(fileName + ReadTask.TEMP_EXT));
        doAnswer(args -> {
            String labelPath = args.getArgument(0);
            JsonHandler.writeAsFile(tapeCatalogLabel, tmpTarOutputDir.toPath().resolve(labelPath).toFile());
            return null;
        })
            .doAnswer(args -> {
                String filePath = args.getArgument(0);
                FileUtils.write(tmpTarOutputDir.toPath().resolve(filePath).toFile(), "data", StandardCharsets.UTF_8);
                return null;
            })
            .when(tapeReadWriteService)
            .readFromTape(any());

        when(tapeDriveService.getDriveCommandService()).thenReturn(tapeDriveCommandService);
        doNothing().when(tapeDriveCommandService).rewind();
        doNothing().when(tapeDriveCommandService).move(anyInt(), anyBoolean());
        when(tapeCatalogService.receive(any())).thenReturn(Optional.of(appropriateTape));
        when(tapeDriveService.getTapeDriveConf().getIndex()).thenReturn(DRIVE_INDEX);
        TapeRobotService tapeRobotService = mock(TapeRobotService.class);
        when(tapeRobotPool.checkoutRobotService()).thenReturn(tapeRobotService);
        TapeLoadUnloadService tapeLoadUnloadService = mock(TapeLoadUnloadService.class);
        when(tapeRobotService.getLoadUnloadService()).thenReturn(tapeLoadUnloadService);
        doNothing().when(tapeLoadUnloadService).loadTape(anyInt(), anyInt());
        doNothing().when(tapeDriveCommandService).eject();
        doNothing().when(tapeLoadUnloadService).unloadTape(anyInt(), anyInt());

        doReturn(false).when(archiveCacheStorage).containsArchive(FAKE_FILE_BUCKET_ID, fileName);
        doNothing().when(archiveCacheStorage).reserveArchiveStorageSpace(FAKE_FILE_BUCKET_ID, fileName, FILE_SIZE);
        doAnswer(args -> {
            assertThat(fileTest).isNotNull();
            assertThat(args.getArgument(0, Path.class)).isEqualTo(fileTest);
            assertThat(fileTest).exists();
            assertThat(fileTest).hasContent("data");
            return null;
        })
            .when(archiveCacheStorage)
            .moveArchiveToCache(any(), eq(FAKE_FILE_BUCKET_ID), eq(fileName));

        // Case one current t
        ReadWriteResult result = readTask.get();

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(StatusCode.OK);
        assertThat(result.getOrderState()).isEqualTo(QueueState.COMPLETED);
        assertThat(result.getCurrentTape()).isNotNull();
        assertThat(result.getCurrentTape().getCode()).isEqualTo(FAKE_TAPE_CODE);
        assertThat(
            result.getCurrentTape().getCurrentLocation().equals(new TapeLocation(DRIVE_INDEX, TapeLocationType.DRIVE))
        ).isEqualTo(true);
        assertThat(
            currentTape.getCurrentLocation().equals(new TapeLocation(SLOT_INDEX + 1, TapeLocationType.SLOT))
        ).isEqualTo(true);
        assertThat(result.getCurrentTape().getCurrentPosition()).isEqualTo(filePosition + 1);

        InOrder inOrder = Mockito.inOrder(tapeReadWriteService, tapeLoadUnloadService, tapeDriveCommandService);
        // Eject current tape
        inOrder.verify(tapeDriveCommandService).eject();
        inOrder.verify(tapeLoadUnloadService).unloadTape(SLOT_INDEX + 1, DRIVE_INDEX);
        // Load target tape
        inOrder.verify(tapeLoadUnloadService).loadTape(SLOT_INDEX, DRIVE_INDEX);
        inOrder.verify(tapeDriveCommandService).rewind();
        // Check label
        inOrder.verify(tapeReadWriteService).getTmpOutputStorageFolder();
        inOrder.verify(tapeReadWriteService).readFromTape(any());
        // Read file
        inOrder.verify(tapeReadWriteService).getTmpOutputStorageFolder();
        inOrder.verify(tapeReadWriteService).readFromTape(any());
        inOrder.verifyNoMoreInteractions();

        verify(archiveCacheStorage).containsArchive(FAKE_FILE_BUCKET_ID, fileName);
        verify(archiveCacheStorage).reserveArchiveStorageSpace(FAKE_FILE_BUCKET_ID, fileName, FILE_SIZE);
        verify(archiveCacheStorage).moveArchiveToCache(eq(fileTest), eq(FAKE_FILE_BUCKET_ID), eq(fileName));
        verifyNoMoreInteractions(archiveCacheStorage);

        verify(accessRequestManager).updateAccessRequestWhenArchiveReady(fileName);
        verifyNoMoreInteractions(accessRequestManager);
    }

    @Test
    public void testReadTaskWhenCurrentTapeIsNotNullAndNotEligibleAndTheEligibleTapeIsAvailableToReadFileAfterLabel()
        throws Exception {
        // When

        when(tapeDriveService.getTapeDriveConf()).thenAnswer(o -> mock(TapeDriveConf.class));
        TapeCatalog currentTape = new TapeCatalog()
            .setLibrary(FAKE_LIBRARY)
            .setCode("tape")
            .setCurrentLocation(new TapeLocation(DRIVE_INDEX, TapeLocationType.DRIVE))
            .setPreviousLocation(new TapeLocation(SLOT_INDEX + 1, TapeLocationType.SLOT));

        TapeCatalogLabel tapeCatalogLabel = new TapeCatalogLabel().setCode(FAKE_TAPE_CODE).setBucket(FAKE_BUCKET);

        TapeCatalog appropriateTape = new TapeCatalog()
            .setLibrary(FAKE_LIBRARY)
            .setCode(FAKE_TAPE_CODE)
            .setCurrentLocation(new TapeLocation(SLOT_INDEX, TapeLocationType.SLOT))
            .setLabel(tapeCatalogLabel);

        ReadOrder readOrder = new ReadOrder(
            FAKE_TAPE_CODE,
            FILE_POSITION,
            fileName,
            FAKE_BUCKET,
            FAKE_FILE_BUCKET_ID,
            FILE_SIZE
        );

        ReadTask readTask = new ReadTask(
            readOrder,
            currentTape,
            new TapeLibraryServiceImpl(tapeDriveService, tapeRobotPool, FULL_CARTRIDGE_THRESHOLD),
            tapeCatalogService,
            accessRequestManager,
            archiveCacheStorage
        );

        fileTest = Files.createFile(tmpTarOutputDir.toPath().resolve(fileName + ReadTask.TEMP_EXT));
        doAnswer(args -> {
            String labelPath = args.getArgument(0);
            JsonHandler.writeAsFile(tapeCatalogLabel, tmpTarOutputDir.toPath().resolve(labelPath).toFile());
            return null;
        })
            .doAnswer(args -> {
                String filePath = args.getArgument(0);
                FileUtils.write(tmpTarOutputDir.toPath().resolve(filePath).toFile(), "data", StandardCharsets.UTF_8);
                return null;
            })
            .when(tapeReadWriteService)
            .readFromTape(any());

        when(tapeDriveService.getDriveCommandService()).thenReturn(tapeDriveCommandService);
        doNothing().when(tapeDriveCommandService).rewind();
        doNothing().when(tapeDriveCommandService).move(anyInt(), anyBoolean());
        when(tapeCatalogService.receive(any())).thenReturn(Optional.of(appropriateTape));
        when(tapeDriveService.getTapeDriveConf().getIndex()).thenReturn(DRIVE_INDEX);
        TapeRobotService tapeRobotService = mock(TapeRobotService.class);
        when(tapeRobotPool.checkoutRobotService()).thenReturn(tapeRobotService);
        TapeLoadUnloadService tapeLoadUnloadService = mock(TapeLoadUnloadService.class);
        when(tapeRobotService.getLoadUnloadService()).thenReturn(tapeLoadUnloadService);
        doNothing().when(tapeLoadUnloadService).loadTape(anyInt(), anyInt());
        doNothing().when(tapeDriveCommandService).eject();
        doNothing().when(tapeLoadUnloadService).unloadTape(anyInt(), anyInt());

        doReturn(false).when(archiveCacheStorage).containsArchive(FAKE_FILE_BUCKET_ID, fileName);
        doNothing().when(archiveCacheStorage).reserveArchiveStorageSpace(FAKE_FILE_BUCKET_ID, fileName, FILE_SIZE);
        doAnswer(args -> {
            assertThat(fileTest).isNotNull();
            assertThat(args.getArgument(0, Path.class)).isEqualTo(fileTest);
            assertThat(fileTest).exists();
            assertThat(fileTest).hasContent("data");
            return null;
        })
            .when(archiveCacheStorage)
            .moveArchiveToCache(any(), eq(FAKE_FILE_BUCKET_ID), eq(fileName));

        // Case one current t
        ReadWriteResult result = readTask.get();

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(StatusCode.OK);
        assertThat(result.getOrderState()).isEqualTo(QueueState.COMPLETED);
        assertThat(result.getCurrentTape()).isNotNull();
        assertThat(result.getCurrentTape().getCode()).isEqualTo(FAKE_TAPE_CODE);
        assertThat(
            result.getCurrentTape().getCurrentLocation().equals(new TapeLocation(DRIVE_INDEX, TapeLocationType.DRIVE))
        ).isEqualTo(true);
        assertThat(
            currentTape.getCurrentLocation().equals(new TapeLocation(SLOT_INDEX + 1, TapeLocationType.SLOT))
        ).isEqualTo(true);
        assertThat(result.getCurrentTape().getCurrentPosition()).isEqualTo(FILE_POSITION + 1);

        InOrder inOrder = Mockito.inOrder(tapeReadWriteService, tapeLoadUnloadService, tapeDriveCommandService);
        // Eject current tape
        inOrder.verify(tapeDriveCommandService).eject();
        inOrder.verify(tapeLoadUnloadService).unloadTape(SLOT_INDEX + 1, DRIVE_INDEX);
        // Load target tape
        inOrder.verify(tapeLoadUnloadService).loadTape(SLOT_INDEX, DRIVE_INDEX);
        inOrder.verify(tapeDriveCommandService).rewind();
        // Check label
        inOrder.verify(tapeReadWriteService).getTmpOutputStorageFolder();
        inOrder.verify(tapeReadWriteService).readFromTape(any());
        // Move tape to position "FILE_POSITION" & read file
        inOrder.verify(tapeReadWriteService).getTmpOutputStorageFolder();
        inOrder.verify(tapeDriveCommandService).move(FILE_POSITION - 1, false);
        inOrder.verify(tapeReadWriteService).readFromTape(any());
        inOrder.verifyNoMoreInteractions();

        verify(archiveCacheStorage).containsArchive(FAKE_FILE_BUCKET_ID, fileName);
        verify(archiveCacheStorage).reserveArchiveStorageSpace(FAKE_FILE_BUCKET_ID, fileName, FILE_SIZE);
        verify(archiveCacheStorage).moveArchiveToCache(eq(fileTest), eq(FAKE_FILE_BUCKET_ID), eq(fileName));
        verifyNoMoreInteractions(archiveCacheStorage);

        verify(accessRequestManager).updateAccessRequestWhenArchiveReady(fileName);
        verifyNoMoreInteractions(accessRequestManager);
    }

    @Test
    public void testReadTaskWhenCurrentTapeIsNotNullAndNotEligibleAndTheEligibleTapeLabelMismatch() throws Exception {
        // When

        when(tapeDriveService.getTapeDriveConf()).thenAnswer(o -> mock(TapeDriveConf.class));
        TapeCatalog currentTape = new TapeCatalog()
            .setLibrary(FAKE_LIBRARY)
            .setCode("tape")
            .setCurrentLocation(new TapeLocation(DRIVE_INDEX, TapeLocationType.DRIVE))
            .setPreviousLocation(new TapeLocation(SLOT_INDEX + 1, TapeLocationType.SLOT));

        TapeCatalogLabel tapeCatalogLabel = new TapeCatalogLabel().setCode(FAKE_TAPE_CODE).setBucket(FAKE_BUCKET);

        TapeCatalog appropriateTape = new TapeCatalog()
            .setLibrary(FAKE_LIBRARY)
            .setCode(FAKE_TAPE_CODE)
            .setCurrentLocation(new TapeLocation(SLOT_INDEX, TapeLocationType.SLOT))
            .setLabel(tapeCatalogLabel);

        int filePosition = 1;
        ReadOrder readOrder = new ReadOrder(
            FAKE_TAPE_CODE,
            filePosition,
            fileName,
            FAKE_BUCKET,
            FAKE_FILE_BUCKET_ID,
            FILE_SIZE
        );

        ReadTask readTask = new ReadTask(
            readOrder,
            currentTape,
            new TapeLibraryServiceImpl(tapeDriveService, tapeRobotPool, FULL_CARTRIDGE_THRESHOLD),
            tapeCatalogService,
            accessRequestManager,
            archiveCacheStorage
        );

        fileTest = Files.createFile(tmpTarOutputDir.toPath().resolve(fileName + ReadTask.TEMP_EXT));
        doAnswer(args -> {
            String labelPath = args.getArgument(0);
            TapeCatalogLabel wrongLabel = new TapeCatalogLabel().setCode("AnotherTape").setBucket(FAKE_BUCKET);
            JsonHandler.writeAsFile(wrongLabel, tmpTarOutputDir.toPath().resolve(labelPath).toFile());
            return null;
        })
            .doAnswer(args -> {
                String filePath = args.getArgument(0);
                FileUtils.write(tmpTarOutputDir.toPath().resolve(filePath).toFile(), "data", StandardCharsets.UTF_8);
                return null;
            })
            .when(tapeReadWriteService)
            .readFromTape(any());

        when(tapeDriveService.getDriveCommandService()).thenReturn(tapeDriveCommandService);
        doNothing().when(tapeDriveCommandService).rewind();
        doNothing().when(tapeDriveCommandService).move(anyInt(), anyBoolean());
        when(tapeCatalogService.receive(any())).thenReturn(Optional.of(appropriateTape));
        when(tapeDriveService.getTapeDriveConf().getIndex()).thenReturn(DRIVE_INDEX);
        TapeRobotService tapeRobotService = mock(TapeRobotService.class);
        when(tapeRobotPool.checkoutRobotService()).thenReturn(tapeRobotService);
        TapeLoadUnloadService tapeLoadUnloadService = mock(TapeLoadUnloadService.class);
        when(tapeRobotService.getLoadUnloadService()).thenReturn(tapeLoadUnloadService);
        doNothing().when(tapeLoadUnloadService).loadTape(anyInt(), anyInt());
        doNothing().when(tapeDriveCommandService).eject();
        doNothing().when(tapeLoadUnloadService).unloadTape(anyInt(), anyInt());

        doReturn(false).when(archiveCacheStorage).containsArchive(FAKE_FILE_BUCKET_ID, fileName);
        doNothing().when(archiveCacheStorage).reserveArchiveStorageSpace(FAKE_FILE_BUCKET_ID, fileName, FILE_SIZE);
        doAnswer(args -> {
            assertThat(fileTest).isNotNull();
            assertThat(args.getArgument(0, Path.class)).isEqualTo(fileTest);
            assertThat(fileTest).exists();
            assertThat(fileTest).hasContent("data");
            return null;
        })
            .when(archiveCacheStorage)
            .moveArchiveToCache(any(), eq(FAKE_FILE_BUCKET_ID), eq(fileName));

        // Case one current t
        ReadWriteResult result = readTask.get();

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(StatusCode.FATAL);
        assertThat(result.getOrderState()).isEqualTo(QueueState.ERROR);

        assertThat(result.getCurrentTape()).isNotNull();
        assertThat(result.getCurrentTape().getCode()).isEqualTo(FAKE_TAPE_CODE);

        InOrder inOrder = Mockito.inOrder(tapeReadWriteService, tapeLoadUnloadService, tapeDriveCommandService);
        // Eject current tape
        inOrder.verify(tapeDriveCommandService).eject();
        inOrder.verify(tapeLoadUnloadService).unloadTape(SLOT_INDEX + 1, DRIVE_INDEX);
        // Load target tape
        inOrder.verify(tapeLoadUnloadService).loadTape(SLOT_INDEX, DRIVE_INDEX);
        inOrder.verify(tapeDriveCommandService).rewind();
        // Check label
        inOrder.verify(tapeReadWriteService).getTmpOutputStorageFolder();
        inOrder.verify(tapeReadWriteService).readFromTape(any());
        inOrder.verifyNoMoreInteractions();

        verifyNoMoreInteractions(archiveCacheStorage);
        verifyNoMoreInteractions(accessRequestManager);
    }

    @Test
    public void testReadTaskWhenCurrentTapeIsNotNullAndNotEligibleAndTheEligibleTapeLabelReadError() throws Exception {
        // When

        when(tapeDriveService.getTapeDriveConf()).thenAnswer(o -> mock(TapeDriveConf.class));
        TapeCatalog currentTape = new TapeCatalog()
            .setLibrary(FAKE_LIBRARY)
            .setCode("tape")
            .setCurrentLocation(new TapeLocation(DRIVE_INDEX, TapeLocationType.DRIVE))
            .setPreviousLocation(new TapeLocation(SLOT_INDEX + 1, TapeLocationType.SLOT));

        TapeCatalogLabel tapeCatalogLabel = new TapeCatalogLabel().setCode(FAKE_TAPE_CODE).setBucket(FAKE_BUCKET);

        TapeCatalog appropriateTape = new TapeCatalog()
            .setLibrary(FAKE_LIBRARY)
            .setCode(FAKE_TAPE_CODE)
            .setCurrentLocation(new TapeLocation(SLOT_INDEX, TapeLocationType.SLOT))
            .setLabel(tapeCatalogLabel);

        int filePosition = 1;
        ReadOrder readOrder = new ReadOrder(
            FAKE_TAPE_CODE,
            filePosition,
            fileName,
            FAKE_BUCKET,
            FAKE_FILE_BUCKET_ID,
            FILE_SIZE
        );

        ReadTask readTask = new ReadTask(
            readOrder,
            currentTape,
            new TapeLibraryServiceImpl(tapeDriveService, tapeRobotPool, FULL_CARTRIDGE_THRESHOLD),
            tapeCatalogService,
            accessRequestManager,
            archiveCacheStorage
        );

        fileTest = Files.createFile(tmpTarOutputDir.toPath().resolve(fileName + ReadTask.TEMP_EXT));
        doThrow(new TapeCommandException("label error")).when(tapeReadWriteService).readFromTape(any());

        when(tapeDriveService.getDriveCommandService()).thenReturn(tapeDriveCommandService);
        doNothing().when(tapeDriveCommandService).rewind();
        doNothing().when(tapeDriveCommandService).move(anyInt(), anyBoolean());
        when(tapeCatalogService.receive(any())).thenReturn(Optional.of(appropriateTape));
        when(tapeDriveService.getTapeDriveConf().getIndex()).thenReturn(DRIVE_INDEX);
        TapeRobotService tapeRobotService = mock(TapeRobotService.class);
        when(tapeRobotPool.checkoutRobotService()).thenReturn(tapeRobotService);
        TapeLoadUnloadService tapeLoadUnloadService = mock(TapeLoadUnloadService.class);
        when(tapeRobotService.getLoadUnloadService()).thenReturn(tapeLoadUnloadService);
        doNothing().when(tapeLoadUnloadService).loadTape(anyInt(), anyInt());
        doNothing().when(tapeDriveCommandService).eject();
        doNothing().when(tapeLoadUnloadService).unloadTape(anyInt(), anyInt());

        doReturn(false).when(archiveCacheStorage).containsArchive(FAKE_FILE_BUCKET_ID, fileName);
        doNothing().when(archiveCacheStorage).reserveArchiveStorageSpace(FAKE_FILE_BUCKET_ID, fileName, FILE_SIZE);
        doAnswer(args -> {
            assertThat(fileTest).isNotNull();
            assertThat(args.getArgument(0, Path.class)).isEqualTo(fileTest);
            assertThat(fileTest).exists();
            assertThat(fileTest).hasContent("data");
            return null;
        })
            .when(archiveCacheStorage)
            .moveArchiveToCache(any(), eq(FAKE_FILE_BUCKET_ID), eq(fileName));

        // Case one current t
        ReadWriteResult result = readTask.get();

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(StatusCode.FATAL);
        assertThat(result.getOrderState()).isEqualTo(QueueState.ERROR);

        assertThat(result.getCurrentTape()).isNotNull();
        assertThat(result.getCurrentTape().getCode()).isEqualTo(FAKE_TAPE_CODE);

        InOrder inOrder = Mockito.inOrder(tapeReadWriteService, tapeLoadUnloadService, tapeDriveCommandService);
        // Eject current tape
        inOrder.verify(tapeDriveCommandService).eject();
        inOrder.verify(tapeLoadUnloadService).unloadTape(SLOT_INDEX + 1, DRIVE_INDEX);
        // Load target tape
        inOrder.verify(tapeLoadUnloadService).loadTape(SLOT_INDEX, DRIVE_INDEX);
        inOrder.verify(tapeDriveCommandService).rewind();
        // Check label
        inOrder.verify(tapeReadWriteService).getTmpOutputStorageFolder();
        inOrder.verify(tapeReadWriteService).readFromTape(any());
        inOrder.verifyNoMoreInteractions();

        verifyNoMoreInteractions(archiveCacheStorage);
        verifyNoMoreInteractions(accessRequestManager);
    }

    @Test
    public void testReadTaskWhenCurrentTapeIsNotNullAndNotEligibleAndTheEligibleTapeIsBusy() throws Exception {
        // When
        when(tapeDriveService.getTapeDriveConf()).thenAnswer(o -> mock(TapeDriveConf.class));
        TapeCatalog currentTape = new TapeCatalog()
            .setLibrary(FAKE_LIBRARY)
            .setCode("tape")
            .setCurrentLocation(new TapeLocation(DRIVE_INDEX, TapeLocationType.DRIVE))
            .setPreviousLocation(new TapeLocation(SLOT_INDEX + 1, TapeLocationType.SLOT));

        TapeCatalogLabel tapeCatalogLabel = new TapeCatalogLabel().setCode(FAKE_TAPE_CODE).setBucket(FAKE_BUCKET);

        TapeCatalog appropriateTape = new TapeCatalog()
            .setLibrary(FAKE_LIBRARY)
            .setCode(FAKE_TAPE_CODE)
            .setCurrentLocation(new TapeLocation(SLOT_INDEX, TapeLocationType.SLOT))
            .setLabel(tapeCatalogLabel);

        ReadOrder readOrder = new ReadOrder(
            FAKE_TAPE_CODE,
            FILE_POSITION,
            fileName,
            FAKE_BUCKET,
            FAKE_FILE_BUCKET_ID,
            FILE_SIZE
        );

        ReadTask readTask = new ReadTask(
            readOrder,
            currentTape,
            new TapeLibraryServiceImpl(tapeDriveService, tapeRobotPool, FULL_CARTRIDGE_THRESHOLD),
            tapeCatalogService,
            accessRequestManager,
            archiveCacheStorage
        );

        doNothing().when(tapeReadWriteService).readFromTape(startsWith(readOrder.getFileName()));
        when(tapeDriveService.getDriveCommandService()).thenReturn(tapeDriveCommandService);
        doNothing().when(tapeDriveCommandService).rewind();
        doNothing().when(tapeDriveCommandService).move(anyInt(), anyBoolean());
        when(tapeCatalogService.receive(any())).thenReturn(Optional.empty());
        when(tapeCatalogService.find(any())).thenReturn(List.of(appropriateTape));
        when(tapeDriveService.getTapeDriveConf().getIndex()).thenReturn(DRIVE_INDEX);
        TapeRobotService tapeRobotService = mock(TapeRobotService.class);
        when(tapeRobotPool.checkoutRobotService()).thenReturn(tapeRobotService);
        TapeLoadUnloadService tapeLoadUnloadService = mock(TapeLoadUnloadService.class);
        when(tapeRobotService.getLoadUnloadService()).thenReturn(tapeLoadUnloadService);
        doNothing().when(tapeLoadUnloadService).loadTape(anyInt(), anyInt());
        doNothing().when(tapeLoadUnloadService).unloadTape(anyInt(), anyInt());
        doNothing().when(tapeDriveCommandService).eject();

        // Case one current t
        ReadWriteResult result = readTask.get();

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(StatusCode.KO);
        assertThat(result.getOrderState()).isEqualTo(QueueState.READY);
        assertThat(result.getCurrentTape()).isNull();

        InOrder inOrder = Mockito.inOrder(tapeReadWriteService, tapeLoadUnloadService, tapeDriveCommandService);
        // Eject current tape
        inOrder.verify(tapeDriveCommandService).eject();
        inOrder.verify(tapeLoadUnloadService).unloadTape(SLOT_INDEX + 1, DRIVE_INDEX);
        inOrder.verifyNoMoreInteractions();

        verifyZeroInteractions(archiveCacheStorage, accessRequestManager);
    }

    @Test
    public void testReadTaskWhenCurrentTapeIsNotNullAndNotEligibleAndTheEligibleTapeIsUnknown() throws Exception {
        // When
        when(tapeDriveService.getTapeDriveConf()).thenAnswer(o -> mock(TapeDriveConf.class));
        TapeCatalog currentTape = new TapeCatalog()
            .setLibrary(FAKE_LIBRARY)
            .setCode("tape")
            .setCurrentLocation(new TapeLocation(DRIVE_INDEX, TapeLocationType.DRIVE))
            .setPreviousLocation(new TapeLocation(SLOT_INDEX + 1, TapeLocationType.SLOT));

        ReadOrder readOrder = new ReadOrder(
            FAKE_TAPE_CODE,
            FILE_POSITION,
            fileName,
            FAKE_BUCKET,
            FAKE_FILE_BUCKET_ID,
            FILE_SIZE
        );

        ReadTask readTask = new ReadTask(
            readOrder,
            currentTape,
            new TapeLibraryServiceImpl(tapeDriveService, tapeRobotPool, FULL_CARTRIDGE_THRESHOLD),
            tapeCatalogService,
            accessRequestManager,
            archiveCacheStorage
        );

        doNothing().when(tapeReadWriteService).readFromTape(startsWith(readOrder.getFileName()));
        when(tapeDriveService.getDriveCommandService()).thenReturn(tapeDriveCommandService);
        doNothing().when(tapeDriveCommandService).rewind();
        doNothing().when(tapeDriveCommandService).move(anyInt(), anyBoolean());
        when(tapeCatalogService.receive(any())).thenReturn(Optional.empty());
        when(tapeCatalogService.find(any())).thenReturn(Collections.emptyList());
        when(tapeDriveService.getTapeDriveConf().getIndex()).thenReturn(DRIVE_INDEX);
        TapeRobotService tapeRobotService = mock(TapeRobotService.class);
        when(tapeRobotPool.checkoutRobotService()).thenReturn(tapeRobotService);
        TapeLoadUnloadService tapeLoadUnloadService = mock(TapeLoadUnloadService.class);
        when(tapeRobotService.getLoadUnloadService()).thenReturn(tapeLoadUnloadService);
        doNothing().when(tapeLoadUnloadService).loadTape(anyInt(), anyInt());
        doNothing().when(tapeDriveCommandService).eject();
        doNothing().when(tapeLoadUnloadService).unloadTape(anyInt(), anyInt());

        // Case one current t
        ReadWriteResult result = readTask.get();

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(StatusCode.FATAL);
        assertThat(result.getOrderState()).isEqualTo(QueueState.ERROR);
        assertThat(result.getCurrentTape()).isNull();

        InOrder inOrder = Mockito.inOrder(tapeReadWriteService, tapeLoadUnloadService, tapeDriveCommandService);
        // Eject current tape
        inOrder.verify(tapeDriveCommandService).eject();
        inOrder.verify(tapeLoadUnloadService).unloadTape(SLOT_INDEX + 1, DRIVE_INDEX);
        inOrder.verifyNoMoreInteractions();

        verifyZeroInteractions(archiveCacheStorage, accessRequestManager);
    }

    @Test
    public void testReadTaskWhenCurrentTapeIsNotNullAndNotEligibleAndTapeCatalogThrownException() throws Exception {
        // When
        when(tapeDriveService.getTapeDriveConf()).thenAnswer(o -> mock(TapeDriveConf.class));
        TapeCatalog currentTape = new TapeCatalog()
            .setLibrary(FAKE_LIBRARY)
            .setCode("tape")
            .setCurrentLocation(new TapeLocation(DRIVE_INDEX, TapeLocationType.DRIVE))
            .setPreviousLocation(new TapeLocation(SLOT_INDEX + 1, TapeLocationType.SLOT));

        ReadOrder readOrder = new ReadOrder(
            FAKE_TAPE_CODE,
            FILE_POSITION,
            fileName,
            FAKE_BUCKET,
            FAKE_FILE_BUCKET_ID,
            FILE_SIZE
        );

        ReadTask readTask = new ReadTask(
            readOrder,
            currentTape,
            new TapeLibraryServiceImpl(tapeDriveService, tapeRobotPool, FULL_CARTRIDGE_THRESHOLD),
            tapeCatalogService,
            accessRequestManager,
            archiveCacheStorage
        );

        doNothing().when(tapeReadWriteService).readFromTape(startsWith(readOrder.getFileName()));
        when(tapeDriveService.getDriveCommandService()).thenReturn(tapeDriveCommandService);
        doNothing().when(tapeDriveCommandService).rewind();
        doNothing().when(tapeDriveCommandService).move(anyInt(), anyBoolean());
        when(tapeCatalogService.receive(any())).thenThrow(QueueException.class);
        when(tapeDriveService.getTapeDriveConf().getIndex()).thenReturn(DRIVE_INDEX);
        TapeRobotService tapeRobotService = mock(TapeRobotService.class);
        when(tapeRobotPool.checkoutRobotService()).thenReturn(tapeRobotService);
        TapeLoadUnloadService tapeLoadUnloadService = mock(TapeLoadUnloadService.class);
        when(tapeRobotService.getLoadUnloadService()).thenReturn(tapeLoadUnloadService);
        doNothing().when(tapeLoadUnloadService).loadTape(anyInt(), anyInt());
        doNothing().when(tapeDriveCommandService).eject();
        doNothing().when(tapeLoadUnloadService).unloadTape(anyInt(), anyInt());

        // Case one current t
        ReadWriteResult result = readTask.get();

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(StatusCode.KO);
        assertThat(result.getOrderState()).isEqualTo(QueueState.READY);
        assertThat(result.getCurrentTape()).isNull();

        InOrder inOrder = Mockito.inOrder(tapeReadWriteService, tapeLoadUnloadService, tapeDriveCommandService);
        // Eject current tape
        inOrder.verify(tapeDriveCommandService).eject();
        inOrder.verify(tapeLoadUnloadService).unloadTape(SLOT_INDEX + 1, DRIVE_INDEX);
        inOrder.verifyNoMoreInteractions();

        verifyZeroInteractions(archiveCacheStorage, accessRequestManager);
    }

    @Test
    public void testReadTaskWhenCurrentTapeIsNotNullAndNotEligibleAndTheEligibleTapeIsOutside() throws Exception {
        // When
        when(tapeDriveService.getTapeDriveConf()).thenAnswer(o -> mock(TapeDriveConf.class));
        TapeCatalog currentTape = new TapeCatalog()
            .setLibrary(FAKE_LIBRARY)
            .setCode("tape")
            .setCurrentLocation(new TapeLocation(DRIVE_INDEX, TapeLocationType.DRIVE))
            .setPreviousLocation(new TapeLocation(SLOT_INDEX + 1, TapeLocationType.SLOT));

        TapeCatalogLabel tapeCatalogLabel = new TapeCatalogLabel().setCode(FAKE_TAPE_CODE).setBucket(FAKE_BUCKET);

        TapeCatalog appropriateTape = new TapeCatalog()
            .setLibrary(FAKE_LIBRARY)
            .setCode(FAKE_TAPE_CODE)
            .setCurrentLocation(new TapeLocation(null, TapeLocationType.OUTSIDE))
            .setLabel(tapeCatalogLabel);

        ReadOrder readOrder = new ReadOrder(
            FAKE_TAPE_CODE,
            FILE_POSITION,
            fileName,
            FAKE_BUCKET,
            FAKE_FILE_BUCKET_ID,
            FILE_SIZE
        );

        ReadTask readTask = new ReadTask(
            readOrder,
            currentTape,
            new TapeLibraryServiceImpl(tapeDriveService, tapeRobotPool, FULL_CARTRIDGE_THRESHOLD),
            tapeCatalogService,
            accessRequestManager,
            archiveCacheStorage
        );

        doNothing().when(tapeReadWriteService).readFromTape(startsWith(readOrder.getFileName()));
        when(tapeDriveService.getDriveCommandService()).thenReturn(tapeDriveCommandService);
        doNothing().when(tapeDriveCommandService).rewind();
        doNothing().when(tapeDriveCommandService).move(anyInt(), anyBoolean());
        when(tapeCatalogService.receive(any())).thenReturn(Optional.of(appropriateTape));
        when(tapeDriveService.getTapeDriveConf().getIndex()).thenReturn(DRIVE_INDEX);
        TapeRobotService tapeRobotService = mock(TapeRobotService.class);
        when(tapeRobotPool.checkoutRobotService()).thenReturn(tapeRobotService);
        TapeLoadUnloadService tapeLoadUnloadService = mock(TapeLoadUnloadService.class);
        when(tapeRobotService.getLoadUnloadService()).thenReturn(tapeLoadUnloadService);
        doNothing().when(tapeLoadUnloadService).loadTape(anyInt(), anyInt());
        doNothing().when(tapeLoadUnloadService).unloadTape(anyInt(), anyInt());

        doNothing().when(tapeDriveCommandService).eject();

        // Case one current t
        ReadWriteResult result = readTask.get();

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(StatusCode.FATAL);
        assertThat(result.getOrderState()).isEqualTo(QueueState.ERROR);
        assertThat(result.getCurrentTape()).isNull();

        InOrder inOrder = Mockito.inOrder(tapeReadWriteService, tapeLoadUnloadService, tapeDriveCommandService);
        // Eject current tape
        inOrder.verify(tapeDriveCommandService).eject();
        inOrder.verify(tapeLoadUnloadService).unloadTape(SLOT_INDEX + 1, DRIVE_INDEX);
        inOrder.verifyNoMoreInteractions();

        verifyZeroInteractions(archiveCacheStorage, accessRequestManager);
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

        TapeCatalogLabel tapeCatalogLabel = new TapeCatalogLabel().setCode(FAKE_TAPE_CODE).setBucket(FAKE_BUCKET);

        TapeCatalog appropriateTape = new TapeCatalog()
            .setLibrary(FAKE_LIBRARY)
            .setCode(FAKE_TAPE_CODE)
            .setCurrentLocation(new TapeLocation(SLOT_INDEX, TapeLocationType.SLOT))
            .setLabel(tapeCatalogLabel);

        ReadOrder readOrder = new ReadOrder(
            FAKE_TAPE_CODE,
            FILE_POSITION,
            fileName,
            FAKE_BUCKET,
            FAKE_FILE_BUCKET_ID,
            FILE_SIZE
        );

        ReadTask readTask = new ReadTask(
            readOrder,
            currentTape,
            new TapeLibraryServiceImpl(tapeDriveService, tapeRobotPool, FULL_CARTRIDGE_THRESHOLD),
            tapeCatalogService,
            accessRequestManager,
            archiveCacheStorage
        );

        doNothing().when(tapeReadWriteService).readFromTape(startsWith(readOrder.getFileName()));
        when(tapeDriveService.getDriveCommandService()).thenReturn(tapeDriveCommandService);
        doNothing().when(tapeDriveCommandService).rewind();
        doNothing().when(tapeDriveCommandService).move(anyInt(), anyBoolean());
        when(tapeCatalogService.receive(any())).thenReturn(Optional.of(appropriateTape));
        when(tapeDriveService.getTapeDriveConf().getIndex()).thenReturn(DRIVE_INDEX);
        TapeRobotService tapeRobotService = mock(TapeRobotService.class);
        when(tapeRobotPool.checkoutRobotService()).thenReturn(tapeRobotService);
        TapeLoadUnloadService tapeLoadUnloadService = mock(TapeLoadUnloadService.class);
        when(tapeRobotService.getLoadUnloadService()).thenReturn(tapeLoadUnloadService);
        doThrow(new TapeCommandException("error")).when(tapeLoadUnloadService).loadTape(anyInt(), anyInt());
        doNothing().when(tapeDriveCommandService).eject();
        doNothing().when(tapeLoadUnloadService).unloadTape(anyInt(), anyInt());
        when(tapeDriveCommandService.status()).thenReturn(new TapeDriveState());

        // Case one current t
        ReadWriteResult result = readTask.get();

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(StatusCode.FATAL);
        assertThat(result.getOrderState()).isEqualTo(QueueState.ERROR);
        assertThat(result.getCurrentTape()).isNull();

        InOrder inOrder = Mockito.inOrder(tapeReadWriteService, tapeLoadUnloadService, tapeDriveCommandService);
        // Eject current tape
        inOrder.verify(tapeDriveCommandService).eject();
        inOrder.verify(tapeLoadUnloadService).unloadTape(SLOT_INDEX + 1, DRIVE_INDEX);
        // Load target tape
        inOrder.verify(tapeLoadUnloadService).loadTape(SLOT_INDEX, DRIVE_INDEX);
        inOrder.verifyNoMoreInteractions();

        verifyZeroInteractions(archiveCacheStorage, accessRequestManager);
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

        TapeCatalogLabel tapeCatalogLabel = new TapeCatalogLabel().setCode(FAKE_TAPE_CODE).setBucket(FAKE_BUCKET);

        TapeCatalog appropriateTape = new TapeCatalog()
            .setLibrary(FAKE_LIBRARY)
            .setCode(FAKE_TAPE_CODE)
            .setCurrentLocation(new TapeLocation(SLOT_INDEX, TapeLocationType.SLOT))
            .setLabel(tapeCatalogLabel);

        ReadOrder readOrder = new ReadOrder(
            FAKE_TAPE_CODE,
            FILE_POSITION,
            fileName,
            FAKE_BUCKET,
            FAKE_FILE_BUCKET_ID,
            FILE_SIZE
        );

        ReadTask readTask = new ReadTask(
            readOrder,
            currentTape,
            new TapeLibraryServiceImpl(tapeDriveService, tapeRobotPool, FULL_CARTRIDGE_THRESHOLD),
            tapeCatalogService,
            accessRequestManager,
            archiveCacheStorage
        );

        doNothing().when(tapeReadWriteService).readFromTape(startsWith(readOrder.getFileName()));
        when(tapeDriveService.getDriveCommandService()).thenReturn(tapeDriveCommandService);
        doNothing().when(tapeDriveCommandService).rewind();
        doNothing().when(tapeDriveCommandService).move(anyInt(), anyBoolean());
        when(tapeCatalogService.receive(any())).thenReturn(Optional.of(appropriateTape));
        when(tapeDriveService.getTapeDriveConf().getIndex()).thenReturn(DRIVE_INDEX);
        TapeRobotService tapeRobotService = mock(TapeRobotService.class);
        when(tapeRobotPool.checkoutRobotService()).thenReturn(tapeRobotService);
        TapeLoadUnloadService tapeLoadUnloadService = mock(TapeLoadUnloadService.class);
        when(tapeRobotService.getLoadUnloadService()).thenReturn(tapeLoadUnloadService);
        doNothing().when(tapeDriveCommandService).eject();
        doThrow(new TapeCommandException("error")).when(tapeLoadUnloadService).unloadTape(anyInt(), anyInt());
        TapeDriveState tapeDriveState = new TapeDriveState();
        tapeDriveState.setDriveStatuses(List.of(TapeDriveStatus.ONLINE));
        when(tapeDriveCommandService.status()).thenReturn(tapeDriveState);
        doNothing().when(tapeLoadUnloadService).loadTape(anyInt(), anyInt());

        // Case one current t
        ReadWriteResult result = readTask.get();

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(StatusCode.FATAL);
        assertThat(result.getOrderState()).isEqualTo(QueueState.READY);
        assertThat(result.getCurrentTape()).isNotNull();

        InOrder inOrder = Mockito.inOrder(tapeReadWriteService, tapeLoadUnloadService, tapeDriveCommandService);
        // Eject current tape
        inOrder.verify(tapeDriveCommandService).eject();
        inOrder.verify(tapeLoadUnloadService).unloadTape(SLOT_INDEX + 1, DRIVE_INDEX);
        inOrder.verifyNoMoreInteractions();

        verifyZeroInteractions(archiveCacheStorage, accessRequestManager);
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

        TapeCatalogLabel tapeCatalogLabel = new TapeCatalogLabel().setCode(FAKE_TAPE_CODE).setBucket(FAKE_BUCKET);

        TapeCatalog appropriateTape = new TapeCatalog()
            .setLibrary(FAKE_LIBRARY)
            .setCode(FAKE_TAPE_CODE)
            .setCurrentLocation(new TapeLocation(SLOT_INDEX, TapeLocationType.SLOT))
            .setLabel(tapeCatalogLabel);

        ReadOrder readOrder = new ReadOrder(
            FAKE_TAPE_CODE,
            FILE_POSITION,
            fileName,
            FAKE_BUCKET,
            FAKE_FILE_BUCKET_ID,
            FILE_SIZE
        );

        ReadTask readTask = new ReadTask(
            readOrder,
            currentTape,
            new TapeLibraryServiceImpl(tapeDriveService, tapeRobotPool, FULL_CARTRIDGE_THRESHOLD),
            tapeCatalogService,
            accessRequestManager,
            archiveCacheStorage
        );

        fileTest = Files.createFile(tmpTarOutputDir.toPath().resolve(fileName + ReadTask.TEMP_EXT));
        doAnswer(args -> {
            String labelPath = args.getArgument(0);
            JsonHandler.writeAsFile(tapeCatalogLabel, tmpTarOutputDir.toPath().resolve(labelPath).toFile());
            return null;
        })
            .doThrow(new TapeCommandException("error"))
            .when(tapeReadWriteService)
            .readFromTape(any());
        when(tapeDriveService.getDriveCommandService()).thenReturn(tapeDriveCommandService);
        doNothing().when(tapeDriveCommandService).rewind();
        doNothing().when(tapeDriveCommandService).move(anyInt(), anyBoolean());
        when(tapeCatalogService.receive(any())).thenReturn(Optional.of(appropriateTape));
        when(tapeDriveService.getTapeDriveConf().getIndex()).thenReturn(DRIVE_INDEX);
        TapeRobotService tapeRobotService = mock(TapeRobotService.class);
        when(tapeRobotPool.checkoutRobotService()).thenReturn(tapeRobotService);
        doNothing().when(tapeDriveCommandService).eject();
        TapeLoadUnloadService tapeLoadUnloadService = mock(TapeLoadUnloadService.class);
        when(tapeRobotService.getLoadUnloadService()).thenReturn(tapeLoadUnloadService);
        doNothing().when(tapeLoadUnloadService).unloadTape(anyInt(), anyInt());
        when(tapeDriveCommandService.status()).thenReturn(new TapeDriveState());
        doNothing().when(tapeLoadUnloadService).loadTape(anyInt(), anyInt());

        doReturn(false).when(archiveCacheStorage).containsArchive(FAKE_FILE_BUCKET_ID, fileName);
        doNothing().when(archiveCacheStorage).reserveArchiveStorageSpace(FAKE_FILE_BUCKET_ID, fileName, FILE_SIZE);
        doNothing().when(archiveCacheStorage).cancelReservedArchive(FAKE_FILE_BUCKET_ID, fileName);

        // Case one current t
        ReadWriteResult result = readTask.get();

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(StatusCode.FATAL);
        assertThat(result.getOrderState()).isEqualTo(QueueState.ERROR);
        assertThat(result.getCurrentTape()).isNotNull();

        InOrder inOrder = Mockito.inOrder(tapeReadWriteService, tapeLoadUnloadService, tapeDriveCommandService);
        // Eject current tape
        inOrder.verify(tapeDriveCommandService).eject();
        inOrder.verify(tapeLoadUnloadService).unloadTape(SLOT_INDEX + 1, DRIVE_INDEX);
        // Load target tape
        inOrder.verify(tapeLoadUnloadService).loadTape(SLOT_INDEX, DRIVE_INDEX);
        inOrder.verify(tapeDriveCommandService).rewind();
        // Check label
        inOrder.verify(tapeReadWriteService).getTmpOutputStorageFolder();
        inOrder.verify(tapeReadWriteService).readFromTape(any());
        // Move tape to position "FILE_POSITION" & read file
        inOrder.verify(tapeReadWriteService).getTmpOutputStorageFolder();
        inOrder.verify(tapeDriveCommandService).move(FILE_POSITION - 1, false);
        inOrder.verify(tapeReadWriteService).readFromTape(any());
        inOrder.verifyNoMoreInteractions();

        verify(archiveCacheStorage).containsArchive(FAKE_FILE_BUCKET_ID, fileName);
        verify(archiveCacheStorage).reserveArchiveStorageSpace(FAKE_FILE_BUCKET_ID, fileName, FILE_SIZE);
        verify(archiveCacheStorage).cancelReservedArchive(eq(FAKE_FILE_BUCKET_ID), eq(fileName));
        verifyNoMoreInteractions(archiveCacheStorage, accessRequestManager);
    }

    @Test
    public void testReadTaskCurrentTapeIsNullAndEligibleTapeFound() throws Exception {
        // When
        when(tapeDriveService.getTapeDriveConf()).thenAnswer(o -> mock(TapeDriveConf.class));

        TapeCatalogLabel tapeCatalogLabel = new TapeCatalogLabel().setCode(FAKE_TAPE_CODE).setBucket(FAKE_BUCKET);

        TapeCatalog tapeCatalog = new TapeCatalog()
            .setLibrary(FAKE_LIBRARY)
            .setCode(FAKE_TAPE_CODE)
            .setCurrentLocation(new TapeLocation(SLOT_INDEX, TapeLocationType.SLOT))
            .setLabel(tapeCatalogLabel);

        ReadOrder readOrder = new ReadOrder(
            FAKE_TAPE_CODE,
            FILE_POSITION,
            fileName,
            FAKE_BUCKET,
            FAKE_FILE_BUCKET_ID,
            FILE_SIZE
        );

        ReadTask readTask = new ReadTask(
            readOrder,
            null,
            new TapeLibraryServiceImpl(tapeDriveService, tapeRobotPool, FULL_CARTRIDGE_THRESHOLD),
            tapeCatalogService,
            accessRequestManager,
            archiveCacheStorage
        );

        fileTest = Files.createFile(tmpTarOutputDir.toPath().resolve(fileName + ReadTask.TEMP_EXT));
        doAnswer(args -> {
            String labelPath = args.getArgument(0);
            JsonHandler.writeAsFile(tapeCatalogLabel, tmpTarOutputDir.toPath().resolve(labelPath).toFile());
            return null;
        })
            .doAnswer(args -> {
                String filePath = args.getArgument(0);
                FileUtils.write(tmpTarOutputDir.toPath().resolve(filePath).toFile(), "data", StandardCharsets.UTF_8);
                return null;
            })
            .when(tapeReadWriteService)
            .readFromTape(any());

        when(tapeDriveService.getDriveCommandService()).thenReturn(tapeDriveCommandService);
        doNothing().when(tapeDriveCommandService).rewind();
        doNothing().when(tapeDriveCommandService).move(anyInt(), anyBoolean());
        when(tapeCatalogService.receive(any())).thenReturn(Optional.of(tapeCatalog));
        when(tapeDriveService.getTapeDriveConf().getIndex()).thenReturn(DRIVE_INDEX);
        when(tapeRobotPool.checkoutRobotService()).thenReturn(mock(TapeRobotService.class));
        TapeLoadUnloadService tapeLoadUnloadService = mock(TapeLoadUnloadService.class);
        when(tapeRobotPool.checkoutRobotService().getLoadUnloadService()).thenReturn(tapeLoadUnloadService);
        doNothing().when(tapeLoadUnloadService).loadTape(anyInt(), anyInt());

        doNothing().when(tapeDriveCommandService).eject();

        doReturn(false).when(archiveCacheStorage).containsArchive(FAKE_FILE_BUCKET_ID, fileName);
        doNothing().when(archiveCacheStorage).reserveArchiveStorageSpace(FAKE_FILE_BUCKET_ID, fileName, FILE_SIZE);
        doAnswer(args -> {
            assertThat(fileTest).isNotNull();
            assertThat(args.getArgument(0, Path.class)).isEqualTo(fileTest);
            assertThat(fileTest).exists();
            return null;
        })
            .when(archiveCacheStorage)
            .moveArchiveToCache(any(), eq(FAKE_FILE_BUCKET_ID), eq(fileName));

        // Case one current t
        ReadWriteResult result = readTask.get();

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(StatusCode.OK);
        assertThat(result.getOrderState()).isEqualTo(QueueState.COMPLETED);
        assertThat(result.getCurrentTape()).isNotNull();
        assertThat(result.getCurrentTape().getCode()).isEqualTo(FAKE_TAPE_CODE);
        assertThat(
            result.getCurrentTape().getCurrentLocation().equals(new TapeLocation(DRIVE_INDEX, TapeLocationType.DRIVE))
        ).isEqualTo(true);
        assertThat(result.getCurrentTape().getCurrentPosition()).isEqualTo(FILE_POSITION + 1);

        InOrder inOrder = Mockito.inOrder(tapeReadWriteService, tapeLoadUnloadService, tapeDriveCommandService);
        // Load target tape
        inOrder.verify(tapeLoadUnloadService).loadTape(SLOT_INDEX, DRIVE_INDEX);
        inOrder.verify(tapeDriveCommandService).rewind();
        // Check label
        inOrder.verify(tapeReadWriteService).getTmpOutputStorageFolder();
        inOrder.verify(tapeReadWriteService).readFromTape(any());
        // Move tape to position "FILE_POSITION" & read file
        inOrder.verify(tapeReadWriteService).getTmpOutputStorageFolder();
        inOrder.verify(tapeDriveCommandService).move(FILE_POSITION - 1, false);
        inOrder.verify(tapeReadWriteService).readFromTape(any());
        inOrder.verifyNoMoreInteractions();

        verify(archiveCacheStorage).containsArchive(FAKE_FILE_BUCKET_ID, fileName);
        verify(archiveCacheStorage).reserveArchiveStorageSpace(FAKE_FILE_BUCKET_ID, fileName, FILE_SIZE);
        verify(archiveCacheStorage).moveArchiveToCache(eq(fileTest), eq(FAKE_FILE_BUCKET_ID), eq(fileName));
        verifyNoMoreInteractions(archiveCacheStorage);

        verify(accessRequestManager).updateAccessRequestWhenArchiveReady(fileName);
        verifyNoMoreInteractions(accessRequestManager);
    }

    @Test
    public void cacheDiskSpaceFullThenNoDataReadFromTape() throws Exception {
        // When
        when(tapeDriveService.getTapeDriveConf()).thenAnswer(o -> mock(TapeDriveConf.class));

        TapeCatalogLabel tapeCatalogLabel = new TapeCatalogLabel().setCode(FAKE_TAPE_CODE).setBucket(FAKE_BUCKET);

        TapeCatalog tapeCatalog = new TapeCatalog()
            .setLibrary(FAKE_LIBRARY)
            .setCode(FAKE_TAPE_CODE)
            .setCurrentPosition(FILE_POSITION)
            .setCurrentLocation(new TapeLocation(DRIVE_INDEX, TapeLocationType.DRIVE))
            .setLabel(tapeCatalogLabel);

        ReadOrder readOrder = new ReadOrder(
            FAKE_TAPE_CODE,
            FILE_POSITION,
            fileName,
            FAKE_BUCKET,
            FAKE_FILE_BUCKET_ID,
            FILE_SIZE
        );

        ReadTask readTask = new ReadTask(
            readOrder,
            tapeCatalog,
            new TapeLibraryServiceImpl(tapeDriveService, tapeRobotPool, FULL_CARTRIDGE_THRESHOLD),
            tapeCatalogService,
            accessRequestManager,
            archiveCacheStorage
        );

        doReturn(false).when(archiveCacheStorage).containsArchive(FAKE_FILE_BUCKET_ID, fileName);
        doThrow(IllegalStateException.class)
            .when(archiveCacheStorage)
            .reserveArchiveStorageSpace(FAKE_FILE_BUCKET_ID, fileName, FILE_SIZE);

        fileTest = Files.createFile(tmpTarOutputDir.toPath().resolve(fileName + ReadTask.TEMP_EXT));

        doAnswer(args -> {
            String filePath = args.getArgument(0);
            FileUtils.write(tmpTarOutputDir.toPath().resolve(filePath).toFile(), "data", StandardCharsets.UTF_8);
            return null;
        })
            .when(tapeReadWriteService)
            .readFromTape(any());

        when(tapeDriveService.getDriveCommandService()).thenReturn(tapeDriveCommandService);

        // When
        ReadWriteResult result = readTask.get();

        // Then
        assertThat(result.getStatus()).isEqualTo(StatusCode.FATAL);
        assertThat(result.getOrderState()).isEqualTo(QueueState.ERROR);

        InOrder inOrder = Mockito.inOrder(tapeReadWriteService, tapeDriveCommandService);
        inOrder.verify(tapeReadWriteService).getTmpOutputStorageFolder();
        inOrder.verifyNoMoreInteractions();

        verify(archiveCacheStorage).containsArchive(FAKE_FILE_BUCKET_ID, fileName);
        verify(archiveCacheStorage).reserveArchiveStorageSpace(FAKE_FILE_BUCKET_ID, fileName, FILE_SIZE);
        verifyNoMoreInteractions(archiveCacheStorage);

        verify(tapeReadWriteService, never()).readFromTape(any());
        verifyZeroInteractions(tapeDriveCommandService, accessRequestManager);
    }

    @Test
    public void whenAccessRequestManagerUpdateFailureThenReadOrderCompletesSuccessfully() throws Exception {
        // When
        when(tapeDriveService.getTapeDriveConf()).thenAnswer(o -> mock(TapeDriveConf.class));

        TapeCatalogLabel tapeCatalogLabel = new TapeCatalogLabel().setCode(FAKE_TAPE_CODE).setBucket(FAKE_BUCKET);

        TapeCatalog tapeCatalog = new TapeCatalog()
            .setLibrary(FAKE_LIBRARY)
            .setCode(FAKE_TAPE_CODE)
            .setCurrentPosition(FILE_POSITION)
            .setCurrentLocation(new TapeLocation(DRIVE_INDEX, TapeLocationType.DRIVE))
            .setLabel(tapeCatalogLabel);

        ReadOrder readOrder = new ReadOrder(
            FAKE_TAPE_CODE,
            FILE_POSITION,
            fileName,
            FAKE_BUCKET,
            FAKE_FILE_BUCKET_ID,
            FILE_SIZE
        );

        ReadTask readTask = new ReadTask(
            readOrder,
            tapeCatalog,
            new TapeLibraryServiceImpl(tapeDriveService, tapeRobotPool, FULL_CARTRIDGE_THRESHOLD),
            tapeCatalogService,
            accessRequestManager,
            archiveCacheStorage
        );

        doAnswer(args -> {
            String filePath = args.getArgument(0);
            FileUtils.write(tmpTarOutputDir.toPath().resolve(filePath).toFile(), "data", StandardCharsets.UTF_8);
            return null;
        })
            .when(tapeReadWriteService)
            .readFromTape(any());

        when(tapeDriveService.getDriveCommandService()).thenReturn(tapeDriveCommandService);
        doNothing().when(tapeDriveCommandService).rewind();
        doNothing().when(tapeDriveCommandService).move(anyInt(), anyBoolean());
        doNothing().when(tapeDriveCommandService).eject();

        doReturn(false).when(archiveCacheStorage).containsArchive(FAKE_FILE_BUCKET_ID, fileName);
        doNothing().when(archiveCacheStorage).reserveArchiveStorageSpace(FAKE_FILE_BUCKET_ID, fileName, FILE_SIZE);
        doAnswer(args -> {
            assertThat(fileTest).isNotNull();
            assertThat(args.getArgument(0, Path.class)).isEqualTo(fileTest);
            assertThat(fileTest).exists();
            return null;
        })
            .when(archiveCacheStorage)
            .moveArchiveToCache(any(), eq(FAKE_FILE_BUCKET_ID), eq(fileName));

        doThrow(new AccessRequestReferentialException("prb"))
            .when(accessRequestManager)
            .updateAccessRequestWhenArchiveReady(fileName);

        doAnswer(invocationOnMock -> {
            fileTest = Files.createFile(tmpTarOutputDir.toPath().resolve(fileName + ReadTask.TEMP_EXT));
            return null;
        })
            .when(tapeReadWriteService)
            .readFromTape(any());

        // When
        ReadWriteResult result = readTask.get();

        // Then
        assertThat(result.getStatus()).isEqualTo(StatusCode.OK);
        assertThat(result.getOrderState()).isEqualTo(QueueState.COMPLETED);

        InOrder inOrder = Mockito.inOrder(tapeReadWriteService, tapeDriveCommandService);
        // Read file
        inOrder.verify(tapeReadWriteService).getTmpOutputStorageFolder();
        inOrder.verify(tapeReadWriteService).readFromTape(any());
        inOrder.verifyNoMoreInteractions();

        verify(archiveCacheStorage).containsArchive(FAKE_FILE_BUCKET_ID, fileName);
        verify(archiveCacheStorage).reserveArchiveStorageSpace(FAKE_FILE_BUCKET_ID, fileName, FILE_SIZE);
        verify(archiveCacheStorage).moveArchiveToCache(eq(fileTest), eq(FAKE_FILE_BUCKET_ID), eq(fileName));
        verifyNoMoreInteractions(archiveCacheStorage);

        verify(accessRequestManager).updateAccessRequestWhenArchiveReady(fileName);
        verifyNoMoreInteractions(accessRequestManager);
    }
}
