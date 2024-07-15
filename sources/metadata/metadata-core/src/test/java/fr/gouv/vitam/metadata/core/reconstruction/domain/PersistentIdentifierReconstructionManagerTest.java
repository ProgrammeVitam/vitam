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
package fr.gouv.vitam.metadata.core.reconstruction.domain;

import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.metadata.core.reconstruction.exception.ReconstructionException;
import fr.gouv.vitam.metadata.core.reconstruction.model.ReconstructionOperation;
import fr.gouv.vitam.metadata.core.reconstruction.repository.ReconstructionOperationRepository;
import fr.gouv.vitam.metadata.core.reconstruction.repository.ReconstructionResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static fr.gouv.vitam.metadata.core.reconstruction.repository.ReconstructionResponse.ReconstructionStatus.FAILURE;
import static fr.gouv.vitam.metadata.core.reconstruction.repository.ReconstructionResponse.ReconstructionStatus.SUCCESS;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class PersistentIdentifierReconstructionManagerTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private PersistentIdentifierReconstructionManager manager;

    @Mock
    private OperationReportParser operationReportParser;

    @Mock
    private ReconstructionOperationRepository reconstructionOperationRepository;

    @Before
    public void setUp() {
        manager = new PersistentIdentifierReconstructionManager(
            reconstructionOperationRepository,
            operationReportParser
        );
    }

    @Test
    public void reconstruct_Successful() throws Exception {
        LocalDateTime startDate = LocalDateUtil.now();
        LocalDateTime endDate = LocalDateUtil.now().plusDays(1);
        List<ReconstructionOperation> reconstructionOperations = new ArrayList<>();
        reconstructionOperations.add(
            ReconstructionOperation.builder()
                .setId("123")
                .setTenant(1)
                .setType("Elimination")
                .setLastPersistedDate("2023-01-01T00:00:00")
                .build()
        );
        reconstructionOperations.add(
            ReconstructionOperation.builder()
                .setId("234")
                .setTenant(2)
                .setType("Elimination")
                .setLastPersistedDate("2023-01-02T00:00:00")
                .build()
        );

        when(reconstructionOperationRepository.fetchReconstructionOperations(startDate, endDate)).thenReturn(
            reconstructionOperations
        );
        when(operationReportParser.processReportFromOperation(any(ReconstructionOperation.class))).thenReturn(
            LocalDateUtil.now()
        );

        ReconstructionResponse response = manager.reconstruct(startDate, endDate);

        assertThat(SUCCESS).isEqualTo(response.status);
        assertThat(response.getLastSuccessfulOperationDate()).isNotNull();
    }

    @Test
    public void reconstruct_Failure() throws ReconstructionException {
        LocalDateTime startDate = LocalDateUtil.now();
        LocalDateTime endDate = LocalDateUtil.now().plusDays(1);
        when(reconstructionOperationRepository.fetchReconstructionOperations(startDate, endDate)).thenThrow(
            new ReconstructionException("Reconstruction failure")
        );

        ReconstructionResponse response = manager.reconstruct(startDate, endDate);

        assertThat(FAILURE).isEqualTo(response.status);
        assertThat(response.getLastSuccessfulOperationDate()).isNotNull();
    }
}
