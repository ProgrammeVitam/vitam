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

import fr.gouv.vitam.common.storage.tapelibrary.ReadWritePriority;
import fr.gouv.vitam.common.storage.tapelibrary.TapeDriveConf;
import fr.gouv.vitam.storage.engine.common.model.QueueMessageType;
import fr.gouv.vitam.storage.engine.common.model.ReadOrder;
import fr.gouv.vitam.storage.engine.common.model.ReadWriteOrder;
import fr.gouv.vitam.storage.engine.common.model.TapeCatalog;
import fr.gouv.vitam.storage.engine.common.model.WriteOrder;
import fr.gouv.vitam.storage.offers.tape.cas.AccessRequestManager;
import fr.gouv.vitam.storage.offers.tape.cas.ArchiveCacheStorage;
import fr.gouv.vitam.storage.offers.tape.cas.ArchiveReferentialRepository;
import fr.gouv.vitam.storage.offers.tape.dto.TapeDriveState;
import fr.gouv.vitam.storage.offers.tape.dto.TapeDriveStatus;
import fr.gouv.vitam.storage.offers.tape.exception.QueueException;
import fr.gouv.vitam.storage.offers.tape.spec.QueueRepository;
import fr.gouv.vitam.storage.offers.tape.spec.TapeCatalogService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeDriveCommandService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeDriveService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeLibraryPool;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.internal.verification.Times;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TapeDriveWorkerManagerTest {

    private static final int FULL_CARTRIDGE_THRESHOLD = 2_000_000;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private QueueRepository queueRepository;

    @Mock
    private ArchiveReferentialRepository archiveReferentialRepository;

    @Mock
    private AccessRequestManager accessRequestManager;

    @Mock
    private TapeLibraryPool tapeLibraryPool;

    @Mock
    private Map<Integer, TapeCatalog> driveTape;

    @Mock
    private ArchiveCacheStorage archiveCacheStorage;

    @Mock
    private TapeCatalogService tapeCatalogService;

    @Mock
    private TapeDriveService driveService0;

    @Mock
    private TapeDriveCommandService tapeDriveCommandService0;

    @Mock
    private TapeDriveService driveService1;

    @Mock
    private TapeDriveCommandService tapeDriveCommandService1;

    private TapeDriveWorkerManager tapeDriveWorkerManager;
    private File inputTarDir;

    @Before
    public void setUp() throws Exception {
        Set<Map.Entry<Integer, TapeDriveService>> drives = Set.of(
            Map.entry(0, driveService0),
            Map.entry(1, driveService1)
        );
        doReturn(drives).when(tapeLibraryPool).drives();

        TapeDriveConf tapeDriveConf0 = new TapeDriveConf();
        tapeDriveConf0.setIndex(0);
        doReturn(tapeDriveConf0).when(driveService0).getTapeDriveConf();
        doReturn(tapeDriveCommandService0).when(driveService0).getDriveCommandService();

        TapeDriveConf tapeDriveConf1 = new TapeDriveConf();
        tapeDriveConf1.setIndex(1);
        doReturn(tapeDriveConf1).when(driveService1).getTapeDriveConf();
        doReturn(tapeDriveCommandService1).when(driveService1).getDriveCommandService();

        tapeDriveWorkerManager = new TapeDriveWorkerManager(
            queueRepository,
            archiveReferentialRepository,
            accessRequestManager,
            tapeLibraryPool,
            driveTape,
            "",
            false,
            archiveCacheStorage,
            tapeCatalogService,
            FULL_CARTRIDGE_THRESHOLD
        );

        inputTarDir = temporaryFolder.newFolder("inputTars");
    }

    @After
    public void tearDown() {}

    @Test
    public void test_constructor() {
        assertThatCode(
            () ->
                new TapeDriveWorkerManager(
                    mock(QueueRepository.class),
                    mock(ArchiveReferentialRepository.class),
                    accessRequestManager,
                    mock(TapeLibraryPool.class),
                    mock(Map.class),
                    inputTarDir.getAbsolutePath(),
                    false,
                    archiveCacheStorage,
                    tapeCatalogService,
                    FULL_CARTRIDGE_THRESHOLD
                )
        ).doesNotThrowAnyException();

        assertThatThrownBy(
            () ->
                new TapeDriveWorkerManager(
                    mock(QueueRepository.class),
                    mock(ArchiveReferentialRepository.class),
                    accessRequestManager,
                    mock(TapeLibraryPool.class),
                    null,
                    inputTarDir.getAbsolutePath(),
                    false,
                    archiveCacheStorage,
                    tapeCatalogService,
                    FULL_CARTRIDGE_THRESHOLD
                )
        ).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(
            () ->
                new TapeDriveWorkerManager(
                    mock(QueueRepository.class),
                    mock(ArchiveReferentialRepository.class),
                    accessRequestManager,
                    null,
                    mock(Map.class),
                    inputTarDir.getAbsolutePath(),
                    false,
                    archiveCacheStorage,
                    tapeCatalogService,
                    FULL_CARTRIDGE_THRESHOLD
                )
        ).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(
            () ->
                new TapeDriveWorkerManager(
                    mock(QueueRepository.class),
                    null,
                    accessRequestManager,
                    mock(TapeLibraryPool.class),
                    mock(Map.class),
                    inputTarDir.getAbsolutePath(),
                    false,
                    archiveCacheStorage,
                    tapeCatalogService,
                    FULL_CARTRIDGE_THRESHOLD
                )
        ).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(
            () ->
                new TapeDriveWorkerManager(
                    null,
                    mock(ArchiveReferentialRepository.class),
                    accessRequestManager,
                    mock(TapeLibraryPool.class),
                    mock(Map.class),
                    inputTarDir.getAbsolutePath(),
                    false,
                    archiveCacheStorage,
                    tapeCatalogService,
                    FULL_CARTRIDGE_THRESHOLD
                )
        ).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(
            () ->
                new TapeDriveWorkerManager(
                    mock(QueueRepository.class),
                    mock(ArchiveReferentialRepository.class),
                    null,
                    mock(TapeLibraryPool.class),
                    mock(Map.class),
                    inputTarDir.getAbsolutePath(),
                    false,
                    archiveCacheStorage,
                    tapeCatalogService,
                    FULL_CARTRIDGE_THRESHOLD
                )
        ).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(
            () ->
                new TapeDriveWorkerManager(
                    mock(QueueRepository.class),
                    null,
                    accessRequestManager,
                    mock(TapeLibraryPool.class),
                    mock(Map.class),
                    inputTarDir.getAbsolutePath(),
                    false,
                    archiveCacheStorage,
                    tapeCatalogService,
                    FULL_CARTRIDGE_THRESHOLD
                )
        ).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(
            () ->
                new TapeDriveWorkerManager(
                    mock(QueueRepository.class),
                    mock(ArchiveReferentialRepository.class),
                    accessRequestManager,
                    mock(TapeLibraryPool.class),
                    mock(Map.class),
                    inputTarDir.getAbsolutePath(),
                    false,
                    null,
                    tapeCatalogService,
                    FULL_CARTRIDGE_THRESHOLD
                )
        ).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(
            () ->
                new TapeDriveWorkerManager(
                    mock(QueueRepository.class),
                    mock(ArchiveReferentialRepository.class),
                    accessRequestManager,
                    mock(TapeLibraryPool.class),
                    mock(Map.class),
                    inputTarDir.getAbsolutePath(),
                    false,
                    archiveCacheStorage,
                    null,
                    FULL_CARTRIDGE_THRESHOLD
                )
        ).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(
            () ->
                new TapeDriveWorkerManager(
                    mock(QueueRepository.class),
                    mock(ArchiveReferentialRepository.class),
                    accessRequestManager,
                    mock(TapeLibraryPool.class),
                    mock(Map.class),
                    inputTarDir.getAbsolutePath(),
                    false,
                    archiveCacheStorage,
                    tapeCatalogService,
                    null
                )
        ).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(
            () ->
                new TapeDriveWorkerManager(
                    mock(QueueRepository.class),
                    mock(ArchiveReferentialRepository.class),
                    accessRequestManager,
                    mock(TapeLibraryPool.class),
                    mock(Map.class),
                    inputTarDir.getAbsolutePath(),
                    false,
                    archiveCacheStorage,
                    tapeCatalogService,
                    0
                )
        ).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(
            () ->
                new TapeDriveWorkerManager(
                    mock(QueueRepository.class),
                    mock(ArchiveReferentialRepository.class),
                    accessRequestManager,
                    mock(TapeLibraryPool.class),
                    mock(Map.class),
                    inputTarDir.getAbsolutePath(),
                    false,
                    archiveCacheStorage,
                    tapeCatalogService,
                    1_000_000_001
                )
        ).isInstanceOf(IllegalArgumentException.class);
    }

    // ===================================
    // =    Write priority
    // ===================================
    @Test
    public void test_consume_produce_current_tape_not_null_not_empty_priority_write_return_write_order()
        throws QueueException {
        TapeDriveWorker driveWorker = mock(TapeDriveWorker.class);
        TapeCatalog tapeCatalog = mock(TapeCatalog.class);
        when(driveWorker.getCurrentTape()).thenReturn(tapeCatalog);
        when(driveWorker.getIndex()).thenReturn(1);
        when(driveWorker.getPriority()).thenReturn(ReadWritePriority.WRITE);

        WriteOrder writeOrder = mock(WriteOrder.class);
        when(writeOrder.isWriteOrder()).thenReturn(true);
        when(queueRepository.receive(any(), eq(QueueMessageType.WriteOrder))).thenReturn(Optional.of(writeOrder));
        // Test consume write order
        Optional<? extends ReadWriteOrder> order = tapeDriveWorkerManager.consume(driveWorker);
        // Get write order => not found
        verify(queueRepository, new Times(1)).receive(any(), eq(QueueMessageType.WriteOrder));
        verify(queueRepository, new Times(0)).receive(any(), eq(QueueMessageType.ReadOrder));
        verify(queueRepository, new Times(0)).receive(eq(QueueMessageType.WriteOrder));
        verify(queueRepository, new Times(0)).receive(eq(QueueMessageType.ReadOrder));

        assertThat(order).isPresent();
        assertThat(order.get().isWriteOrder()).isTrue();
    }

    @Test
    public void test_consume_produce_current_tape_not_null_not_empty_priority_write_return_read_order()
        throws QueueException {
        TapeDriveWorker driveWorker = mock(TapeDriveWorker.class);
        TapeCatalog tapeCatalog = mock(TapeCatalog.class);
        when(driveWorker.getCurrentTape()).thenReturn(tapeCatalog);
        when(driveWorker.getIndex()).thenReturn(1);
        when(driveWorker.getPriority()).thenReturn(ReadWritePriority.WRITE);

        ReadOrder readOrder = mock(ReadOrder.class);
        when(readOrder.isWriteOrder()).thenReturn(false);
        when(queueRepository.receive(any(), eq(QueueMessageType.WriteOrder))).thenReturn(Optional.empty());
        when(queueRepository.receive(any(), eq(QueueMessageType.ReadOrder))).thenReturn(Optional.of(readOrder));

        // Test consume write order
        Optional<? extends ReadWriteOrder> order = tapeDriveWorkerManager.consume(driveWorker);

        verify(queueRepository, new Times(1)).receive(any(), eq(QueueMessageType.WriteOrder));
        verify(queueRepository, new Times(1)).receive(any(), eq(QueueMessageType.ReadOrder));
        verify(queueRepository, new Times(0)).receive(eq(QueueMessageType.WriteOrder));
        verify(queueRepository, new Times(0)).receive(eq(QueueMessageType.ReadOrder));

        assertThat(order).isPresent();
        assertThat(order.get().isWriteOrder()).isFalse();
    }

    @Test
    public void test_consume_produce_current_tape_not_null_not_empty_priority_write_return_write_order_excluding_active_buckets()
        throws QueueException {
        TapeDriveWorker driveWorker = mock(TapeDriveWorker.class);
        TapeCatalog tapeCatalog = mock(TapeCatalog.class);
        when(driveWorker.getCurrentTape()).thenReturn(tapeCatalog);
        when(driveWorker.getIndex()).thenReturn(1);
        when(driveWorker.getPriority()).thenReturn(ReadWritePriority.WRITE);

        WriteOrder writeOrder = mock(WriteOrder.class);
        when(writeOrder.isWriteOrder()).thenReturn(true);
        when(queueRepository.receive(any(), eq(QueueMessageType.WriteOrder)))
            .thenReturn(Optional.empty())
            .thenReturn(Optional.of(writeOrder));

        when(queueRepository.receive(any(), eq(QueueMessageType.ReadOrder))).thenReturn(Optional.empty());

        // Test consume write order
        Optional<? extends ReadWriteOrder> order = tapeDriveWorkerManager.consume(driveWorker);

        verify(queueRepository, new Times(2)).receive(any(), eq(QueueMessageType.WriteOrder));
        verify(queueRepository, new Times(1)).receive(any(), eq(QueueMessageType.ReadOrder));
        verify(queueRepository, new Times(0)).receive(eq(QueueMessageType.WriteOrder));
        verify(queueRepository, new Times(0)).receive(eq(QueueMessageType.ReadOrder));

        assertThat(order).isPresent();
        assertThat(order.get().isWriteOrder()).isTrue();
    }

    @Test
    public void test_consume_produce_current_tape_not_null_not_empty_priority_write_return_any_next_write_order()
        throws QueueException {
        TapeDriveWorker driveWorker = mock(TapeDriveWorker.class);
        TapeCatalog tapeCatalog = mock(TapeCatalog.class);
        when(driveWorker.getCurrentTape()).thenReturn(tapeCatalog);
        when(driveWorker.getIndex()).thenReturn(1);
        when(driveWorker.getPriority()).thenReturn(ReadWritePriority.WRITE);

        WriteOrder writeOrder = mock(WriteOrder.class);
        when(writeOrder.isWriteOrder()).thenReturn(true);
        when(queueRepository.receive(any(), eq(QueueMessageType.WriteOrder)))
            .thenReturn(Optional.empty())
            .thenReturn(Optional.empty());

        when(queueRepository.receive(eq(QueueMessageType.WriteOrder))).thenReturn(Optional.of(writeOrder));

        when(queueRepository.receive(any(), eq(QueueMessageType.ReadOrder))).thenReturn(Optional.empty());

        // Test consume write order
        Optional<? extends ReadWriteOrder> order = tapeDriveWorkerManager.consume(driveWorker);

        verify(queueRepository, new Times(2)).receive(any(), eq(QueueMessageType.WriteOrder));
        verify(queueRepository, new Times(1)).receive(any(), eq(QueueMessageType.ReadOrder));
        verify(queueRepository, new Times(1)).receive(eq(QueueMessageType.WriteOrder));
        verify(queueRepository, new Times(0)).receive(eq(QueueMessageType.ReadOrder));

        assertThat(order).isPresent();
        assertThat(order.get().isWriteOrder()).isTrue();
    }

    @Test
    public void test_consume_produce_current_tape_not_null_not_empty_priority_write_return_read_order_excluding_active_buckets()
        throws QueueException {
        TapeDriveWorker driveWorker = mock(TapeDriveWorker.class);
        TapeCatalog tapeCatalog = mock(TapeCatalog.class);
        when(driveWorker.getCurrentTape()).thenReturn(tapeCatalog);
        when(driveWorker.getIndex()).thenReturn(1);
        when(driveWorker.getPriority()).thenReturn(ReadWritePriority.WRITE);

        ReadOrder readOrder = mock(ReadOrder.class);
        when(readOrder.isWriteOrder()).thenReturn(false);
        when(queueRepository.receive(any(), eq(QueueMessageType.WriteOrder)))
            .thenReturn(Optional.empty())
            .thenReturn(Optional.empty());

        when(queueRepository.receive(eq(QueueMessageType.WriteOrder))).thenReturn(Optional.empty());

        when(queueRepository.receive(any(), eq(QueueMessageType.ReadOrder)))
            .thenReturn(Optional.empty())
            .thenReturn(Optional.of(readOrder));

        // Test consume write order
        Optional<? extends ReadWriteOrder> order = tapeDriveWorkerManager.consume(driveWorker);

        verify(queueRepository, new Times(2)).receive(any(), eq(QueueMessageType.WriteOrder));
        verify(queueRepository, new Times(2)).receive(any(), eq(QueueMessageType.ReadOrder));
        verify(queueRepository, new Times(1)).receive(eq(QueueMessageType.WriteOrder));
        verify(queueRepository, new Times(0)).receive(eq(QueueMessageType.ReadOrder));

        assertThat(order).isPresent();
        assertThat(order.get().isWriteOrder()).isFalse();
    }

    @Test
    public void test_consume_produce_current_tape_not_null_not_empty_priority_write_do_not_return_any_next_read_order()
        throws QueueException {
        TapeDriveWorker driveWorker = mock(TapeDriveWorker.class);
        TapeCatalog tapeCatalog = mock(TapeCatalog.class);
        when(driveWorker.getCurrentTape()).thenReturn(tapeCatalog);
        when(driveWorker.getIndex()).thenReturn(1);
        when(driveWorker.getPriority()).thenReturn(ReadWritePriority.WRITE);

        ReadOrder readOrder = mock(ReadOrder.class);
        when(readOrder.isWriteOrder()).thenReturn(false);

        when(queueRepository.receive(any(), eq(QueueMessageType.WriteOrder)))
            .thenReturn(Optional.empty())
            .thenReturn(Optional.empty());

        when(queueRepository.receive(eq(QueueMessageType.WriteOrder))).thenReturn(Optional.empty());

        when(queueRepository.receive(any(), eq(QueueMessageType.ReadOrder)))
            .thenReturn(Optional.empty())
            .thenReturn(Optional.empty());

        when(queueRepository.receive(eq(QueueMessageType.ReadOrder))).thenReturn(Optional.of(readOrder));

        // Test consume write order
        Optional<? extends ReadWriteOrder> order = tapeDriveWorkerManager.consume(driveWorker);

        verify(queueRepository, new Times(2)).receive(any(), eq(QueueMessageType.WriteOrder));
        verify(queueRepository, new Times(2)).receive(any(), eq(QueueMessageType.ReadOrder));
        verify(queueRepository, new Times(1)).receive(eq(QueueMessageType.WriteOrder));
        verify(queueRepository, new Times(0)).receive(eq(QueueMessageType.ReadOrder));

        assertThat(order).isNotPresent();
    }

    @Test
    public void test_consume_produce_current_tape_not_null_not_empty_priority_write_not_found_orders()
        throws QueueException {
        TapeDriveWorker driveWorker = mock(TapeDriveWorker.class);
        TapeCatalog tapeCatalog = mock(TapeCatalog.class);
        when(driveWorker.getCurrentTape()).thenReturn(tapeCatalog);
        when(driveWorker.getIndex()).thenReturn(1);
        when(driveWorker.getPriority()).thenReturn(ReadWritePriority.WRITE);

        when(queueRepository.receive(any(), eq(QueueMessageType.WriteOrder)))
            .thenReturn(Optional.empty())
            .thenReturn(Optional.empty());

        when(queueRepository.receive(eq(QueueMessageType.WriteOrder))).thenReturn(Optional.empty());

        when(queueRepository.receive(any(), eq(QueueMessageType.ReadOrder)))
            .thenReturn(Optional.empty())
            .thenReturn(Optional.empty());

        when(queueRepository.receive(eq(QueueMessageType.ReadOrder))).thenReturn(Optional.empty());

        // Test consume write order
        Optional<? extends ReadWriteOrder> order = tapeDriveWorkerManager.consume(driveWorker);

        verify(queueRepository, new Times(2)).receive(any(), eq(QueueMessageType.WriteOrder));
        verify(queueRepository, new Times(2)).receive(any(), eq(QueueMessageType.ReadOrder));
        verify(queueRepository, new Times(1)).receive(eq(QueueMessageType.WriteOrder));
        verify(queueRepository, new Times(0)).receive(eq(QueueMessageType.ReadOrder));

        assertThat(order).isNotPresent();
    }

    // ===================================
    // =    Read priority
    // ===================================
    @Test
    public void test_consume_produce_current_tape_not_null_not_empty_priority_read_return_read_order()
        throws QueueException {
        TapeDriveWorker driveWorker = mock(TapeDriveWorker.class);
        TapeCatalog tapeCatalog = mock(TapeCatalog.class);
        when(driveWorker.getCurrentTape()).thenReturn(tapeCatalog);
        when(driveWorker.getIndex()).thenReturn(1);
        when(driveWorker.getPriority()).thenReturn(ReadWritePriority.READ);

        ReadOrder readOrder = mock(ReadOrder.class);
        when(readOrder.isWriteOrder()).thenReturn(false);
        when(queueRepository.receive(any(), eq(QueueMessageType.ReadOrder))).thenReturn(Optional.of(readOrder));
        // Test consume write order
        Optional<? extends ReadWriteOrder> order = tapeDriveWorkerManager.consume(driveWorker);
        // Get write order => not found
        verify(queueRepository, new Times(0)).receive(any(), eq(QueueMessageType.WriteOrder));
        verify(queueRepository, new Times(1)).receive(any(), eq(QueueMessageType.ReadOrder));
        verify(queueRepository, new Times(0)).receive(eq(QueueMessageType.WriteOrder));
        verify(queueRepository, new Times(0)).receive(eq(QueueMessageType.ReadOrder));

        assertThat(order).isPresent();
        assertThat(order.get().isWriteOrder()).isFalse();
    }

    @Test
    public void test_consume_produce_current_tape_not_null_not_empty_priority_read_return_write_order()
        throws QueueException {
        TapeDriveWorker driveWorker = mock(TapeDriveWorker.class);
        TapeCatalog tapeCatalog = mock(TapeCatalog.class);
        when(driveWorker.getCurrentTape()).thenReturn(tapeCatalog);
        when(driveWorker.getIndex()).thenReturn(1);
        when(driveWorker.getPriority()).thenReturn(ReadWritePriority.READ);

        WriteOrder writeOrder = mock(WriteOrder.class);
        when(writeOrder.isWriteOrder()).thenReturn(true);
        when(queueRepository.receive(any(), eq(QueueMessageType.WriteOrder))).thenReturn(Optional.of(writeOrder));
        when(queueRepository.receive(any(), eq(QueueMessageType.ReadOrder))).thenReturn(Optional.empty());

        // Test consume write order
        Optional<? extends ReadWriteOrder> order = tapeDriveWorkerManager.consume(driveWorker);

        verify(queueRepository, new Times(1)).receive(any(), eq(QueueMessageType.WriteOrder));
        verify(queueRepository, new Times(1)).receive(any(), eq(QueueMessageType.ReadOrder));
        verify(queueRepository, new Times(0)).receive(eq(QueueMessageType.WriteOrder));
        verify(queueRepository, new Times(0)).receive(eq(QueueMessageType.ReadOrder));

        assertThat(order).isPresent();
        assertThat(order.get().isWriteOrder()).isTrue();
    }

    @Test
    public void test_consume_produce_current_tape_not_null_not_empty_priority_read_return_read_order_excluding_active_buckets()
        throws QueueException {
        TapeDriveWorker driveWorker = mock(TapeDriveWorker.class);
        TapeCatalog tapeCatalog = mock(TapeCatalog.class);
        when(driveWorker.getCurrentTape()).thenReturn(tapeCatalog);
        when(driveWorker.getIndex()).thenReturn(1);
        when(driveWorker.getPriority()).thenReturn(ReadWritePriority.READ);

        ReadOrder readOrder = mock(ReadOrder.class);
        when(readOrder.isWriteOrder()).thenReturn(false);
        when(queueRepository.receive(any(), eq(QueueMessageType.ReadOrder)))
            .thenReturn(Optional.empty())
            .thenReturn(Optional.of(readOrder));

        when(queueRepository.receive(any(), eq(QueueMessageType.WriteOrder))).thenReturn(Optional.empty());

        // Test consume write order
        Optional<? extends ReadWriteOrder> order = tapeDriveWorkerManager.consume(driveWorker);

        verify(queueRepository, new Times(1)).receive(any(), eq(QueueMessageType.WriteOrder));
        verify(queueRepository, new Times(2)).receive(any(), eq(QueueMessageType.ReadOrder));
        verify(queueRepository, new Times(0)).receive(eq(QueueMessageType.WriteOrder));
        verify(queueRepository, new Times(0)).receive(eq(QueueMessageType.ReadOrder));

        assertThat(order).isPresent();
        assertThat(order.get().isWriteOrder()).isFalse();
    }

    @Test
    public void test_consume_produce_current_tape_not_null_not_empty_priority_read_do_not_return_any_next_read_order()
        throws QueueException {
        TapeDriveWorker driveWorker = mock(TapeDriveWorker.class);
        TapeCatalog tapeCatalog = mock(TapeCatalog.class);
        when(driveWorker.getCurrentTape()).thenReturn(tapeCatalog);
        when(driveWorker.getIndex()).thenReturn(1);
        when(driveWorker.getPriority()).thenReturn(ReadWritePriority.READ);

        ReadOrder readOrder = mock(ReadOrder.class);
        when(readOrder.isWriteOrder()).thenReturn(false);
        when(queueRepository.receive(any(), eq(QueueMessageType.WriteOrder))).thenReturn(Optional.empty());

        when(queueRepository.receive(eq(QueueMessageType.ReadOrder))).thenReturn(Optional.of(readOrder));

        when(queueRepository.receive(any(), eq(QueueMessageType.ReadOrder)))
            .thenReturn(Optional.empty())
            .thenReturn(Optional.empty());

        // Test consume write order
        Optional<? extends ReadWriteOrder> order = tapeDriveWorkerManager.consume(driveWorker);

        verify(queueRepository, new Times(2)).receive(any(), eq(QueueMessageType.WriteOrder));
        verify(queueRepository, new Times(2)).receive(any(), eq(QueueMessageType.ReadOrder));
        verify(queueRepository, new Times(1)).receive(eq(QueueMessageType.WriteOrder));
        verify(queueRepository, new Times(0)).receive(eq(QueueMessageType.ReadOrder));

        assertThat(order).isNotPresent();
    }

    @Test
    public void test_consume_produce_current_tape_not_null_not_empty_priority_read_return_write_order_excluding_active_buckets()
        throws QueueException {
        TapeDriveWorker driveWorker = mock(TapeDriveWorker.class);
        TapeCatalog tapeCatalog = mock(TapeCatalog.class);
        when(driveWorker.getCurrentTape()).thenReturn(tapeCatalog);
        when(driveWorker.getIndex()).thenReturn(1);
        when(driveWorker.getPriority()).thenReturn(ReadWritePriority.READ);

        WriteOrder writeOrder = mock(WriteOrder.class);
        when(writeOrder.isWriteOrder()).thenReturn(true);
        when(queueRepository.receive(any(), eq(QueueMessageType.ReadOrder)))
            .thenReturn(Optional.empty())
            .thenReturn(Optional.empty());

        when(queueRepository.receive(eq(QueueMessageType.ReadOrder))).thenReturn(Optional.empty());

        when(queueRepository.receive(any(), eq(QueueMessageType.WriteOrder)))
            .thenReturn(Optional.empty())
            .thenReturn(Optional.of(writeOrder));

        // Test consume write order
        Optional<? extends ReadWriteOrder> order = tapeDriveWorkerManager.consume(driveWorker);

        verify(queueRepository, new Times(2)).receive(any(), eq(QueueMessageType.WriteOrder));
        verify(queueRepository, new Times(2)).receive(any(), eq(QueueMessageType.ReadOrder));
        verify(queueRepository, new Times(0)).receive(eq(QueueMessageType.WriteOrder));
        verify(queueRepository, new Times(0)).receive(eq(QueueMessageType.ReadOrder));

        assertThat(order).isPresent();
        assertThat(order.get().isWriteOrder()).isTrue();
    }

    @Test
    public void test_consume_produce_current_tape_not_null_not_empty_priority_read_return_any_next_write_order()
        throws QueueException {
        TapeDriveWorker driveWorker = mock(TapeDriveWorker.class);
        TapeCatalog tapeCatalog = mock(TapeCatalog.class);
        when(driveWorker.getCurrentTape()).thenReturn(tapeCatalog);
        when(driveWorker.getIndex()).thenReturn(1);
        when(driveWorker.getPriority()).thenReturn(ReadWritePriority.READ);

        WriteOrder writeOrder = mock(WriteOrder.class);
        when(writeOrder.isWriteOrder()).thenReturn(true);

        when(queueRepository.receive(any(), eq(QueueMessageType.WriteOrder)))
            .thenReturn(Optional.empty())
            .thenReturn(Optional.empty());

        when(queueRepository.receive(eq(QueueMessageType.ReadOrder))).thenReturn(Optional.empty());

        when(queueRepository.receive(any(), eq(QueueMessageType.ReadOrder)))
            .thenReturn(Optional.empty())
            .thenReturn(Optional.empty());

        when(queueRepository.receive(eq(QueueMessageType.WriteOrder))).thenReturn(Optional.of(writeOrder));

        // Test consume write order
        Optional<? extends ReadWriteOrder> order = tapeDriveWorkerManager.consume(driveWorker);

        verify(queueRepository, new Times(2)).receive(any(), eq(QueueMessageType.WriteOrder));
        verify(queueRepository, new Times(2)).receive(any(), eq(QueueMessageType.ReadOrder));
        verify(queueRepository, new Times(1)).receive(eq(QueueMessageType.WriteOrder));
        verify(queueRepository, new Times(0)).receive(eq(QueueMessageType.ReadOrder));

        assertThat(order).isPresent();
        assertThat(order.get().isWriteOrder()).isTrue();
    }

    @Test
    public void test_consume_produce_current_tape_not_null_not_empty_priority_read_not_found_orders()
        throws QueueException {
        TapeDriveWorker driveWorker = mock(TapeDriveWorker.class);
        TapeCatalog tapeCatalog = mock(TapeCatalog.class);
        when(driveWorker.getCurrentTape()).thenReturn(tapeCatalog);
        when(driveWorker.getIndex()).thenReturn(1);
        when(driveWorker.getPriority()).thenReturn(ReadWritePriority.READ);

        when(queueRepository.receive(any(), eq(QueueMessageType.WriteOrder)))
            .thenReturn(Optional.empty())
            .thenReturn(Optional.empty());

        when(queueRepository.receive(eq(QueueMessageType.WriteOrder))).thenReturn(Optional.empty());

        when(queueRepository.receive(any(), eq(QueueMessageType.ReadOrder)))
            .thenReturn(Optional.empty())
            .thenReturn(Optional.empty());

        when(queueRepository.receive(eq(QueueMessageType.ReadOrder))).thenReturn(Optional.empty());

        // Test consume write order
        Optional<? extends ReadWriteOrder> order = tapeDriveWorkerManager.consume(driveWorker);

        verify(queueRepository, new Times(2)).receive(any(), eq(QueueMessageType.WriteOrder));
        verify(queueRepository, new Times(2)).receive(any(), eq(QueueMessageType.ReadOrder));
        verify(queueRepository, new Times(1)).receive(eq(QueueMessageType.WriteOrder));
        verify(queueRepository, new Times(0)).receive(eq(QueueMessageType.ReadOrder));

        assertThat(order).isNotPresent();
    }

    @Test
    public void test_drive_initialization_on_bootstrap() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(2);

        doAnswer(args -> {
            countDownLatch.await();
            TapeDriveState tapeDriveState = new TapeDriveState();
            tapeDriveState.addToDriveStatuses(TapeDriveStatus.DR_OPEN);
            return tapeDriveState;
        })
            .when(tapeDriveCommandService0)
            .status();

        doAnswer(args -> {
            countDownLatch.await();
            TapeDriveState tapeDriveState = new TapeDriveState();
            tapeDriveState.addToDriveStatuses(TapeDriveStatus.DR_OPEN);
            return tapeDriveState;
        })
            .when(tapeDriveCommandService1)
            .status();

        CompletableFuture<Void> initializationCompletableFuture = CompletableFuture.runAsync(
            () -> tapeDriveWorkerManager.initializeOnBootstrap()
        );

        Thread.sleep(2000);
        assertThat(initializationCompletableFuture).isNotCompleted();

        countDownLatch.countDown();
        countDownLatch.countDown();

        assertThatCode(() -> initializationCompletableFuture.get(5, TimeUnit.SECONDS)).doesNotThrowAnyException();

        verify(tapeDriveCommandService0).status();
        verify(tapeDriveCommandService1).status();
    }
    // TODO: 28/03/19 test shutdown
}
