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
package fr.gouv.vitam.logbook.common.traceability;

import fr.gouv.vitam.common.security.merkletree.MerkleTreeAlgo;
import fr.gouv.vitam.common.timestamp.TimestampGenerator;
import fr.gouv.vitam.logbook.common.model.TraceabilityEvent;
import fr.gouv.vitam.logbook.common.model.TraceabilityFile;
import fr.gouv.vitam.logbook.common.model.TraceabilityType;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TraceabilityServiceTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void secureData_should_compute_exact_final_file_length_after_stream_close() throws Exception {
        // Given
        TimestampGenerator timestampGenerator = mock(TimestampGenerator.class);
        LogbookTraceabilityHelper helper = mock(LogbookTraceabilityHelper.class);
        Integer tenantId = 0;
        File tmpFolder = folder.newFolder();

        // Stubbing basic helper calls to avoid NullPointerException
        when(helper.getTraceabilityEndDate()).thenReturn("2026-05-27T16:15:16");
        when(helper.getTraceabilityStartDate()).thenReturn("2026-05-27T16:00:00");
        when(helper.getZipName()).thenReturn("testZip");
        when(helper.getTraceabilityType()).thenReturn(TraceabilityType.OPERATION);
        when(helper.getStepName()).thenReturn("ST_STEP");
        when(helper.getTimestampStepName()).thenReturn("ST_TS_STEP");

        // Stubbing timestampGenerator to return mock token
        byte[] mockTimestampToken = "mockToken".getBytes();
        when(timestampGenerator.generateToken(any(), any(), any())).thenReturn(mockTimestampToken);

        // When helper.saveDataInZip is called, write some dummy zip data so the ZIP is populated
        doAnswer(invocation -> {
            MerkleTreeAlgo merkleAlgo = invocation.getArgument(0);
            TraceabilityFile traceabilityFile = invocation.getArgument(1);

            // Add a leaf to the Merkle algorithm so generateMerkle() doesn't return null
            merkleAlgo.addLeaf("This is a leaf.".getBytes());

            traceabilityFile.initStoreLog();
            traceabilityFile.storeLog("This is a line of data to secure.".getBytes());
            traceabilityFile.closeStoreLog();
            return null;
        })
            .when(helper)
            .saveDataInZip(any(MerkleTreeAlgo.class), any(TraceabilityFile.class));

        TraceabilityService traceabilityService = new TraceabilityService(
            timestampGenerator,
            helper,
            tenantId,
            tmpFolder
        );

        // When
        traceabilityService.secureData("strategyId");

        // Then
        ArgumentCaptor<TraceabilityEvent> eventCaptor = ArgumentCaptor.forClass(TraceabilityEvent.class);
        ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);

        verify(helper).storeAndDeleteZip(
            eq(tenantId),
            eq("strategyId"),
            fileCaptor.capture(),
            any(String.class),
            eventCaptor.capture()
        );

        File writtenZipFile = fileCaptor.getValue();
        TraceabilityEvent capturedEvent = eventCaptor.getValue();

        // Verify that the ZIP file actually exists and has positive length
        assertThat(writtenZipFile).exists();
        long actualLengthOnDisk = writtenZipFile.length();
        assertThat(actualLengthOnDisk).isGreaterThan(0);

        // Assert that the size recorded in the event matches exactly the final closed file length on disk
        assertThat(capturedEvent.getSize()).isEqualTo(actualLengthOnDisk);
    }
}
