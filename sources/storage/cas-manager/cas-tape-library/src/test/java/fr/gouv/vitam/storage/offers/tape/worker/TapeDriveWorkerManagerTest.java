package fr.gouv.vitam.storage.offers.tape.worker;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.storage.tapelibrary.ReadWritePriority;
import fr.gouv.vitam.storage.engine.common.model.QueueMessageEntity;
import fr.gouv.vitam.storage.engine.common.model.QueueMessageType;
import fr.gouv.vitam.storage.engine.common.model.ReadOrder;
import fr.gouv.vitam.storage.engine.common.model.ReadWriteOrder;
import fr.gouv.vitam.storage.engine.common.model.TapeCatalog;
import fr.gouv.vitam.storage.engine.common.model.WriteOrder;
import fr.gouv.vitam.storage.offers.tape.exception.QueueException;
import fr.gouv.vitam.storage.offers.tape.spec.QueueRepository;
import fr.gouv.vitam.storage.offers.tape.spec.TapeLibraryPool;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.internal.verification.Times;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class TapeDriveWorkerManagerTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private QueueRepository queueRepository;

    @Mock
    private TapeLibraryPool tapeLibraryPool;

    @Mock
    private Map<String, TapeCatalog> driveTape;

    @InjectMocks
    private TapeDriveWorkerManager tapeDriveWorkerManager;

    @Before
    public void setUp() throws Exception {
        reset(queueRepository);
        reset(tapeLibraryPool);
        reset(driveTape);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void test_constructor() {
        new TapeDriveWorkerManager(mock(QueueRepository.class), mock(TapeLibraryPool.class), mock(Map.class));

        try {
            new TapeDriveWorkerManager(mock(QueueRepository.class), mock(TapeLibraryPool.class), null);
            fail("should fail driveTape map is required");
        } catch (Exception e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }


        try {
            new TapeDriveWorkerManager(mock(QueueRepository.class), null, mock(Map.class));
            fail("should fail tape library pool is required");
        } catch (Exception e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }



        try {
            new TapeDriveWorkerManager(null, mock(TapeLibraryPool.class), mock(Map.class));
            fail("should fail read write queue is required");
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

    @Test
    public void test_consume_produce() throws QueueException {

        TapeDriveWorker driveWorker = mock(TapeDriveWorker.class);
        when(driveWorker.getIndex()).thenReturn(1);
        when(driveWorker.getPriority())
            .thenReturn(ReadWritePriority.WRITE)
            .thenReturn(ReadWritePriority.READ)
            .thenReturn(ReadWritePriority.WRITE)
            .thenReturn(ReadWritePriority.READ);
        // Test consume write order
        Optional<? extends ReadWriteOrder> order = tapeDriveWorkerManager.consume(driveWorker);
        // Get write order => not found
        verify(queueRepository, new Times(1)).receive(any(), eq(QueueMessageType.WriteOrder));
        // Then Get read order => not found, the try get read order => not found
        verify(queueRepository, new Times(2)).receive(any(), eq(QueueMessageType.ReadOrder));
        Assertions.assertThat(order).isNotPresent();

        // Test consume read order
        reset(queueRepository);
        tapeDriveWorkerManager.consume(driveWorker);
        verify(queueRepository, new Times(1)).receive(any(), eq(QueueMessageType.ReadOrder));
        verify(queueRepository, new Times(1)).receive(any(), eq(QueueMessageType.WriteOrder));
        Assertions.assertThat(order).isNotPresent();

        // Test consume read order and priority write (no write order found)
        reset(queueRepository);
        when(queueRepository.receive(any(), eq(QueueMessageType.WriteOrder)))
            .thenReturn(Optional.of(new WriteOrder()));
        order = tapeDriveWorkerManager.consume(driveWorker);
        verify(queueRepository, new Times(1)).receive(any(), eq(QueueMessageType.WriteOrder));
        verify(queueRepository, new Times(0)).receive(any(), eq(QueueMessageType.ReadOrder));
        Assertions.assertThat(order).isPresent();
        Assertions.assertThat(order.get()).isInstanceOf(WriteOrder.class);


        // Test consume write order and priority read (no read order found)
        reset(queueRepository);
        when(queueRepository.receive(any(), eq(QueueMessageType.ReadOrder)))
            .thenReturn(Optional.of(new ReadOrder()));
        order = tapeDriveWorkerManager.consume(driveWorker);
        verify(queueRepository, new Times(1)).receive(any(), eq(QueueMessageType.ReadOrder));
        verify(queueRepository, new Times(0)).receive(any(), eq(QueueMessageType.WriteOrder));

        Assertions.assertThat(order).isPresent();
        Assertions.assertThat(order.get()).isInstanceOf(ReadOrder.class);
    }

    // TODO: 28/03/19 test shutdown
}