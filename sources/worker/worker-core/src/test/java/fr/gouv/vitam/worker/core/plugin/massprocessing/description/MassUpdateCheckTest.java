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
package fr.gouv.vitam.worker.core.plugin.massprocessing.description;

import fr.gouv.vitam.common.InternalActionKeysRetriever;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.worker.core.plugin.preservation.TestHandlerIO;
import fr.gouv.vitam.worker.core.plugin.preservation.TestWorkerParameter;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;

import static fr.gouv.vitam.common.model.StatusCode.KO;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.worker.core.plugin.preservation.TestWorkerParameter.TestWorkerParameterBuilder.workerParameterBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

public class MassUpdateCheckTest {
    private final TestWorkerParameter EMPTY_WORKER_PARAMETER = workerParameterBuilder().build();
    private final TestHandlerIO handlerIO = new TestHandlerIO();

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @InjectMocks
    private MassUpdateCheck massUpdateCheck;

    @Mock
    private InternalActionKeysRetriever internalActionKeysRetriever;

    @Test
    public void should_return_KO_status_when_query_dsl_contains_internal_fields() throws Exception {
        // Given
        handlerIO.setJsonFromWorkspace("query.json", JsonHandler.createObjectNode());
        given(internalActionKeysRetriever.getInternalActionKeyFields(any())).willReturn(
            Collections.singletonList("_INTERNAL_FIELD"));

        // When
        ItemStatus itemStatus = massUpdateCheck.execute(EMPTY_WORKER_PARAMETER, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(KO);
    }

    @Test
    public void should_return_OK() throws Exception {
        // Given
        handlerIO.setJsonFromWorkspace("query.json", JsonHandler.createObjectNode());
        given(internalActionKeysRetriever.getInternalActionKeyFields(any())).willReturn(Collections.emptyList());

        // When
        ItemStatus itemStatus = massUpdateCheck.execute(EMPTY_WORKER_PARAMETER, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(OK);
    }
}
