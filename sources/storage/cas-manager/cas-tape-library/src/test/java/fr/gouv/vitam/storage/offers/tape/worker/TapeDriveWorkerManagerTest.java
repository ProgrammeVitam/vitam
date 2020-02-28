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
package fr.gouv.vitam.storage.offers.tape.worker;

import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.storage.tapelibrary.ReadWritePriority;
import fr.gouv.vitam.storage.engine.common.model.QueueMessageEntity;
import fr.gouv.vitam.storage.engine.common.model.QueueMessageType;
import fr.gouv.vitam.storage.engine.common.model.ReadOrder;
import fr.gouv.vitam.storage.engine.common.model.ReadWriteOrder;
import fr.gouv.vitam.storage.engine.common.model.TapeCatalog;
import fr.gouv.vitam.storage.engine.common.model.WriteOrder;
import fr.gouv.vitam.storage.offers.tape.cas.ArchiveOutputRetentionPolicy;
import fr.gouv.vitam.storage.offers.tape.cas.ReadRequestReferentialRepository;
import fr.gouv.vitam.storage.offers.tape.cas.ArchiveReferentialRepository;
import fr.gouv.vitam.storage.offers.tape.exception.QueueException;
import fr.gouv.vitam.storage.offers.tape.spec.QueueRepository;
import fr.gouv.vitam.storage.offers.tape.spec.TapeLibraryPool;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.internal.verification.Times;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class TapeDriveWorkerManagerTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private QueueRepository queueRepository;

    @Mock
    private ArchiveReferentialRepository archiveReferentialRepository;

    @Mock
    private ReadRequestReferentialRepository readRequestReferentialRepository;

    @Mock
    private TapeLibraryPool tapeLibraryPool;

    @Mock
    private Map<Integer, TapeCatalog> driveTape;

    @Mock
    private ArchiveOutputRetentionPolicy archiveOutputRetentionPolicy;

    private TapeDriveWorkerManager tapeDriveWorkerManager;

    @Before
    public void setUp() throws Exception {
        reset(queueRepository);
        reset(tapeLibraryPool);
        reset(driveTape);
        reset(archiveOutputRetentionPolicy);

        tapeDriveWorkerManager = new TapeDriveWorkerManager(
            queueRepository, archiveReferentialRepository, readRequestReferentialRepository, tapeLibraryPool, driveTape, "", false,
            archiveOutputRetentionPolicy);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void test_constructor() {
        new TapeDriveWorkerManager(mock(QueueRepository.class), mock(ArchiveReferentialRepository.class), readRequestReferentialRepository,
            mock(TapeLibraryPool.class), mock(Map.class),
            "/tmp", false, archiveOutputRetentionPolicy);

        try {
            new TapeDriveWorkerManager(mock(QueueRepository.class), mock(ArchiveReferentialRepository.class), readRequestReferentialRepository,
                mock(TapeLibraryPool.class), null, "/tmp", false, archiveOutputRetentionPolicy);
            fail("should fail driveTape map is required");
        } catch (Exception e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }


        try {
            new TapeDriveWorkerManager(mock(QueueRepository.class), mock(ArchiveReferentialRepository.class), readRequestReferentialRepository, null,
                mock(Map.class), "/tmp", false, archiveOutputRetentionPolicy);
            fail("should fail tape library pool is required");
        } catch (Exception e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }



        try {
            new TapeDriveWorkerManager(mock(QueueRepository.class), null, readRequestReferentialRepository, mock(TapeLibraryPool.class), mock(Map.class),
                "/tmp", false, archiveOutputRetentionPolicy);
            fail("should fail tar referential repository is required");
        } catch (Exception e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }

        try {
            new TapeDriveWorkerManager(null, mock(ArchiveReferentialRepository.class), readRequestReferentialRepository, mock(TapeLibraryPool.class),
                mock(Map.class), "/tmp", false, archiveOutputRetentionPolicy);
            fail("should fail read write queue is required");
        } catch (Exception e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }

        try {
            new TapeDriveWorkerManager(mock(QueueRepository.class), mock(ArchiveReferentialRepository.class), null, mock(TapeLibraryPool.class),
                    mock(Map.class), "/tmp", false, archiveOutputRetentionPolicy);
            fail("should fail read request repository is required");
        } catch (Exception e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }


        try {
            new TapeDriveWorkerManager(mock(QueueRepository.class), mock(ArchiveReferentialRepository.class), readRequestReferentialRepository, mock(TapeLibraryPool.class),
                mock(Map.class), "/tmp", false, null);
            fail("should fail read request archiveOutputRetentionPolicy is required");
        } catch (Exception e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }
    }

    @Test
    public void enqueue_ok() throws QueueException {
        QueueMessageEntity queueMessageEntity = mock(QueueMessageEntity.class);
        doAnswer(o -> true).when(queueRepository).add(any());
        tapeDriveWorkerManager.enqueue(queueMessageEntity);

        Assertions.assertThat(tapeDriveWorkerManager.getQueue()).isNotNull();
    }


    @Test(expected = QueueException.class)
    public void enqueue_ko() throws QueueException {
        QueueMessageEntity queueMessageEntity = mock(QueueMessageEntity.class);
        doThrow(new QueueException("")).when(queueRepository).add(any());
        tapeDriveWorkerManager.enqueue(queueMessageEntity);
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
        when(driveWorker.getPriority())
            .thenReturn(ReadWritePriority.WRITE);

        WriteOrder writeOrder = mock(WriteOrder.class);
        when(writeOrder.isWriteOrder()).thenReturn(true);
        when(queueRepository.receive(any(), eq(QueueMessageType.WriteOrder))).thenReturn(Optional.of(
            writeOrder));
        // Test consume write order
        Optional<? extends ReadWriteOrder> order = tapeDriveWorkerManager.consume(driveWorker);
        // Get write order => not found
        verify(queueRepository, new Times(1)).receive(any(), eq(QueueMessageType.WriteOrder));
        verify(queueRepository, new Times(0)).receive(any(), eq(QueueMessageType.ReadOrder));
        verify(queueRepository, new Times(0)).receive(eq(QueueMessageType.WriteOrder));
        verify(queueRepository, new Times(0)).receive(eq(QueueMessageType.ReadOrder));

        Assertions.assertThat(order).isPresent();
        Assertions.assertThat(order.get().isWriteOrder()).isTrue();
    }

    @Test
    public void test_consume_produce_current_tape_not_null_not_empty_priority_write_return_read_order()
        throws QueueException {
        TapeDriveWorker driveWorker = mock(TapeDriveWorker.class);
        TapeCatalog tapeCatalog = mock(TapeCatalog.class);
        when(driveWorker.getCurrentTape()).thenReturn(tapeCatalog);
        when(driveWorker.getIndex()).thenReturn(1);
        when(driveWorker.getPriority())
            .thenReturn(ReadWritePriority.WRITE);

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

        Assertions.assertThat(order).isPresent();
        Assertions.assertThat(order.get().isWriteOrder()).isFalse();
    }


    @Test
    public void test_consume_produce_current_tape_not_null_not_empty_priority_write_return_write_order_excluding_active_buckets()
        throws QueueException {
        TapeDriveWorker driveWorker = mock(TapeDriveWorker.class);
        TapeCatalog tapeCatalog = mock(TapeCatalog.class);
        when(driveWorker.getCurrentTape()).thenReturn(tapeCatalog);
        when(driveWorker.getIndex()).thenReturn(1);
        when(driveWorker.getPriority())
            .thenReturn(ReadWritePriority.WRITE);

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

        Assertions.assertThat(order).isPresent();
        Assertions.assertThat(order.get().isWriteOrder()).isTrue();
    }

    @Test
    public void test_consume_produce_current_tape_not_null_not_empty_priority_write_return_any_next_write_order()
        throws QueueException {
        TapeDriveWorker driveWorker = mock(TapeDriveWorker.class);
        TapeCatalog tapeCatalog = mock(TapeCatalog.class);
        when(driveWorker.getCurrentTape()).thenReturn(tapeCatalog);
        when(driveWorker.getIndex()).thenReturn(1);
        when(driveWorker.getPriority())
            .thenReturn(ReadWritePriority.WRITE);

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

        Assertions.assertThat(order).isPresent();
        Assertions.assertThat(order.get().isWriteOrder()).isTrue();
    }


    @Test
    public void test_consume_produce_current_tape_not_null_not_empty_priority_write_return_read_order_excluding_active_buckets()
        throws QueueException {
        TapeDriveWorker driveWorker = mock(TapeDriveWorker.class);
        TapeCatalog tapeCatalog = mock(TapeCatalog.class);
        when(driveWorker.getCurrentTape()).thenReturn(tapeCatalog);
        when(driveWorker.getIndex()).thenReturn(1);
        when(driveWorker.getPriority())
            .thenReturn(ReadWritePriority.WRITE);

        ReadOrder readOrder = mock(ReadOrder.class);
        when(readOrder.isWriteOrder()).thenReturn(false);
        when(queueRepository.receive(any(), eq(QueueMessageType.WriteOrder)))
            .thenReturn(Optional.empty())
            .thenReturn(Optional.empty());

        when(queueRepository.receive(eq(QueueMessageType.WriteOrder))).thenReturn(Optional.empty());


        when(queueRepository.receive(any(), eq(QueueMessageType.ReadOrder))).thenReturn(Optional.empty())
            .thenReturn(Optional.of(readOrder));

        // Test consume write order
        Optional<? extends ReadWriteOrder> order = tapeDriveWorkerManager.consume(driveWorker);

        verify(queueRepository, new Times(2)).receive(any(), eq(QueueMessageType.WriteOrder));
        verify(queueRepository, new Times(2)).receive(any(), eq(QueueMessageType.ReadOrder));
        verify(queueRepository, new Times(1)).receive(eq(QueueMessageType.WriteOrder));
        verify(queueRepository, new Times(0)).receive(eq(QueueMessageType.ReadOrder));

        Assertions.assertThat(order).isPresent();
        Assertions.assertThat(order.get().isWriteOrder()).isFalse();
    }

    @Test
    public void test_consume_produce_current_tape_not_null_not_empty_priority_write_do_not_return_any_next_read_order()
        throws QueueException {
        TapeDriveWorker driveWorker = mock(TapeDriveWorker.class);
        TapeCatalog tapeCatalog = mock(TapeCatalog.class);
        when(driveWorker.getCurrentTape()).thenReturn(tapeCatalog);
        when(driveWorker.getIndex()).thenReturn(1);
        when(driveWorker.getPriority())
            .thenReturn(ReadWritePriority.WRITE);

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

        Assertions.assertThat(order).isNotPresent();
    }



    @Test
    public void test_consume_produce_current_tape_not_null_not_empty_priority_write_not_found_orders()
        throws QueueException {
        TapeDriveWorker driveWorker = mock(TapeDriveWorker.class);
        TapeCatalog tapeCatalog = mock(TapeCatalog.class);
        when(driveWorker.getCurrentTape()).thenReturn(tapeCatalog);
        when(driveWorker.getIndex()).thenReturn(1);
        when(driveWorker.getPriority())
            .thenReturn(ReadWritePriority.WRITE);

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

        Assertions.assertThat(order).isNotPresent();
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
        when(driveWorker.getPriority())
            .thenReturn(ReadWritePriority.READ);

        ReadOrder readOrder = mock(ReadOrder.class);
        when(readOrder.isWriteOrder()).thenReturn(false);
        when(queueRepository.receive(any(), eq(QueueMessageType.ReadOrder))).thenReturn(Optional.of(
            readOrder));
        // Test consume write order
        Optional<? extends ReadWriteOrder> order = tapeDriveWorkerManager.consume(driveWorker);
        // Get write order => not found
        verify(queueRepository, new Times(0)).receive(any(), eq(QueueMessageType.WriteOrder));
        verify(queueRepository, new Times(1)).receive(any(), eq(QueueMessageType.ReadOrder));
        verify(queueRepository, new Times(0)).receive(eq(QueueMessageType.WriteOrder));
        verify(queueRepository, new Times(0)).receive(eq(QueueMessageType.ReadOrder));

        Assertions.assertThat(order).isPresent();
        Assertions.assertThat(order.get().isWriteOrder()).isFalse();
    }

    @Test
    public void test_consume_produce_current_tape_not_null_not_empty_priority_read_return_write_order()
        throws QueueException {
        TapeDriveWorker driveWorker = mock(TapeDriveWorker.class);
        TapeCatalog tapeCatalog = mock(TapeCatalog.class);
        when(driveWorker.getCurrentTape()).thenReturn(tapeCatalog);
        when(driveWorker.getIndex()).thenReturn(1);
        when(driveWorker.getPriority())
            .thenReturn(ReadWritePriority.READ);

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

        Assertions.assertThat(order).isPresent();
        Assertions.assertThat(order.get().isWriteOrder()).isTrue();
    }


    @Test
    public void test_consume_produce_current_tape_not_null_not_empty_priority_read_return_read_order_excluding_active_buckets()
        throws QueueException {
        TapeDriveWorker driveWorker = mock(TapeDriveWorker.class);
        TapeCatalog tapeCatalog = mock(TapeCatalog.class);
        when(driveWorker.getCurrentTape()).thenReturn(tapeCatalog);
        when(driveWorker.getIndex()).thenReturn(1);
        when(driveWorker.getPriority())
            .thenReturn(ReadWritePriority.READ);

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

        Assertions.assertThat(order).isPresent();
        Assertions.assertThat(order.get().isWriteOrder()).isFalse();
    }

    @Test
    public void test_consume_produce_current_tape_not_null_not_empty_priority_read_do_not_return_any_next_read_order()
        throws QueueException {
        TapeDriveWorker driveWorker = mock(TapeDriveWorker.class);
        TapeCatalog tapeCatalog = mock(TapeCatalog.class);
        when(driveWorker.getCurrentTape()).thenReturn(tapeCatalog);
        when(driveWorker.getIndex()).thenReturn(1);
        when(driveWorker.getPriority())
            .thenReturn(ReadWritePriority.READ);

        ReadOrder readOrder = mock(ReadOrder.class);
        when(readOrder.isWriteOrder()).thenReturn(false);
        when(queueRepository.receive(any(), eq(QueueMessageType.WriteOrder)))
            .thenReturn(Optional.empty());

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

        Assertions.assertThat(order).isNotPresent();
    }



    @Test
    public void test_consume_produce_current_tape_not_null_not_empty_priority_read_return_write_order_excluding_active_buckets()
        throws QueueException {
        TapeDriveWorker driveWorker = mock(TapeDriveWorker.class);
        TapeCatalog tapeCatalog = mock(TapeCatalog.class);
        when(driveWorker.getCurrentTape()).thenReturn(tapeCatalog);
        when(driveWorker.getIndex()).thenReturn(1);
        when(driveWorker.getPriority())
            .thenReturn(ReadWritePriority.READ);

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

        Assertions.assertThat(order).isPresent();
        Assertions.assertThat(order.get().isWriteOrder()).isTrue();
    }


    @Test
    public void test_consume_produce_current_tape_not_null_not_empty_priority_read_return_any_next_write_order()
        throws QueueException {
        TapeDriveWorker driveWorker = mock(TapeDriveWorker.class);
        TapeCatalog tapeCatalog = mock(TapeCatalog.class);
        when(driveWorker.getCurrentTape()).thenReturn(tapeCatalog);
        when(driveWorker.getIndex()).thenReturn(1);
        when(driveWorker.getPriority())
            .thenReturn(ReadWritePriority.READ);

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

        Assertions.assertThat(order).isPresent();
        Assertions.assertThat(order.get().isWriteOrder()).isTrue();
    }



    @Test
    public void test_consume_produce_current_tape_not_null_not_empty_priority_read_not_found_orders()
        throws QueueException {
        TapeDriveWorker driveWorker = mock(TapeDriveWorker.class);
        TapeCatalog tapeCatalog = mock(TapeCatalog.class);
        when(driveWorker.getCurrentTape()).thenReturn(tapeCatalog);
        when(driveWorker.getIndex()).thenReturn(1);
        when(driveWorker.getPriority())
            .thenReturn(ReadWritePriority.READ);

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

        Assertions.assertThat(order).isNotPresent();
    }

    // TODO: 28/03/19 test shutdown
}
