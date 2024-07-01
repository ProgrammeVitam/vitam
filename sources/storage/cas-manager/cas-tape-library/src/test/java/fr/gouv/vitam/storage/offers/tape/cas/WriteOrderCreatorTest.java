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
package fr.gouv.vitam.storage.offers.tape.cas;

import fr.gouv.vitam.storage.engine.common.model.QueueMessageType;
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
    private ArchiveReferentialRepository archiveReferentialRepository;

    @Mock
    private QueueRepository readWriteQueue;

    @InjectMocks
    private WriteOrderCreator writeOrderCreator;

    @Test
    public void processMessage() throws Exception {
        // Given
        WriteOrder message = new WriteOrder(
            "bucket",
            "file-bucket-id",
            "filePath",
            1000L,
            "digest",
            "tarId",
            QueueMessageType.WriteOrder
        );

        CountDownLatch countDownLatch = new CountDownLatch(1);
        doAnswer(args -> {
            countDownLatch.countDown();
            return null;
        })
            .when(readWriteQueue)
            .addIfAbsent(any(), any());

        // When
        writeOrderCreator.startListener();
        writeOrderCreator.addToQueue(message);

        // Await termination
        assertThat(countDownLatch.await(1, TimeUnit.MINUTES)).isTrue();

        // Verify
        verify(archiveReferentialRepository).updateLocationToReadyOnDisk("tarId", 1000L, "digest");
        verifyNoMoreInteractions(archiveReferentialRepository);

        verify(readWriteQueue).addIfAbsent(anyList(), eq(message));
        verifyNoMoreInteractions(readWriteQueue);
    }
}
