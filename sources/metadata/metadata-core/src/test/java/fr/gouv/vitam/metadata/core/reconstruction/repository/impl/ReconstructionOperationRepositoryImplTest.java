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
package fr.gouv.vitam.metadata.core.reconstruction.repository.impl;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.metadata.core.reconstruction.model.ReconstructionOperation;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class ReconstructionOperationRepositoryImplTest {

    public static final String DSL_QUERY =
        "[{\"$query\":{\"$and\":[{\"$range\":{\"#lastPersistedDate\":{\"$gte\":\"2023-06-01T00:00\",\"$lt\":\"2023-06-30T00:00\"}}},{\"$in\":{\"evType\":[\"ELIMINATION_ACTION\",\"DELETE_GOT_VERSIONS\",\"TRANSFER_REPLY\"]}},{\"$in\":{\"events.outDetail\":[\"ELIMINATION_ACTION.OK\",\"ELIMINATION_ACTION.WARNING\",\"DELETE_GOT_VERSIONS.OK\",\"TRANSFER_REPLY.OK\"]}}]},\"$filter\":{\"$limit\":10000,\"$orderby\":{\"#lastPersistedDate\":1}},\"$projection\":{}}]";
    private static final String LOGBOOKS_JSON = "logbooks.json";

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    ReconstructionOperationRepositoryImpl reconstructionOperationRepository;

    @Mock
    LogbookOperationsClientFactory logbookOperationsClientFactory;

    @Mock
    LogbookOperationsClient logbookOperationsClient;

    @Before
    public void setup() {
        when(logbookOperationsClientFactory.getClient()).thenReturn(logbookOperationsClient);
        reconstructionOperationRepository = new ReconstructionOperationRepositoryImpl(logbookOperationsClientFactory);
    }

    @Test
    public void testFetchReconstructionOperations() throws Exception {
        // Given
        final LocalDateTime from = LocalDateUtil.getLocalDateFromSimpleFormattedDate("2023-06-01").atStartOfDay();
        final LocalDateTime to = LocalDateUtil.getLocalDateFromSimpleFormattedDate("2023-06-30").atStartOfDay();
        JsonNode logbookResponse = JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(LOGBOOKS_JSON));
        ArgumentCaptor<JsonNode> jsonNodeArgumentCaptor = ArgumentCaptor.forClass(JsonNode.class);
        when(
            logbookOperationsClient.selectOperation(
                jsonNodeArgumentCaptor.capture(),
                any(Boolean.class),
                any(Boolean.class)
            )
        ).thenReturn(logbookResponse);

        // When
        final List<ReconstructionOperation> reconstructionOperations =
            reconstructionOperationRepository.fetchReconstructionOperations(from, to);

        // Then
        assertThat(jsonNodeArgumentCaptor.getAllValues().toString()).isEqualTo(DSL_QUERY);
        assertThat(reconstructionOperations.size()).isEqualTo(4);
    }
}
