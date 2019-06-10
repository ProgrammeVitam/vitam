package fr.gouv.vitam.storage.offers.tape.cas;

import fr.gouv.vitam.storage.engine.common.model.WriteOrder;
import fr.gouv.vitam.storage.offers.tape.spec.QueueRepository;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class WriteOrderCreatorTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private TarReferentialRepository tarReferentialRepository;

    @Mock
    private QueueRepository readWriteQueue;

    @InjectMocks
    private WriteOrderCreator writeOrderCreator;

    @Test
    public void processMessage() throws Exception {

        // Given
        WriteOrder message = new WriteOrder()
            .setArchiveId("tarId")
            .setBucket("bucket")
            .setFilePath("/path/to/file.tar")
            .setSize(1000L)
            .setDigest("digest");

        CountDownLatch countDownLatch = new CountDownLatch(1);
        doAnswer((args) -> {
            countDownLatch.countDown();
            return null;
        }).when(readWriteQueue).addIfAbsent(any(), any());


        // When
        writeOrderCreator.startListener();
        writeOrderCreator.addToQueue(message);

        // Await termination
        assertThat(countDownLatch.await(1, TimeUnit.MINUTES)).isTrue();

        // Verify
        verify(tarReferentialRepository).updateLocationToReadyOnDisk("tarId", 1000L, "digest");
        verifyNoMoreInteractions(tarReferentialRepository);

        verify(readWriteQueue).addIfAbsent(anyList(), eq(message));
        verifyNoMoreInteractions(readWriteQueue);
    }
}
