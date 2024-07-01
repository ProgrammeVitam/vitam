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
package fr.gouv.vitam.worker.core.plugin.bulkatomicupdate;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.common.HandlerIO;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public class CheckQueriesThresholdTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    private static final int TENANT_ID = 0;

    private CheckQueriesThreshold checkDistributionThreshold;

    @Before
    public void setUp() throws Exception {
        VitamConfiguration.setQueriesThreshold(2L);
        checkDistributionThreshold = new CheckQueriesThreshold();
    }

    @Test
    @RunWithCustomExecutor
    public void whenCheckQueriesWithoutThresholdThenReturnOK() throws Exception {
        // Given
        HandlerIO handlerIO = mock(HandlerIO.class);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        JsonNode queryUnit = JsonHandler.getFromInputStream(
            getClass().getResourceAsStream("/CheckQueriesThreshold/queryWithoutThreshold.json")
        );
        given(handlerIO.getJsonFromWorkspace("query.json")).willReturn(queryUnit);

        // When
        ItemStatus itemStatus = checkDistributionThreshold.execute(
            WorkerParametersFactory.newWorkerParameters(),
            handlerIO
        );

        // Then
        assertThat(itemStatus).isNotNull();
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);
    }

    @Test
    @RunWithCustomExecutor
    public void whenCheckQueriesWithoutThresholdThenReturnKO() throws Exception {
        // Given
        HandlerIO handlerIO = mock(HandlerIO.class);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        JsonNode queryUnit = JsonHandler.getFromInputStream(
            getClass().getResourceAsStream("/CheckQueriesThreshold/queryWithoutThresholdKO.json")
        );
        given(handlerIO.getJsonFromWorkspace("query.json")).willReturn(queryUnit);

        // When
        ItemStatus itemStatus = checkDistributionThreshold.execute(
            WorkerParametersFactory.newWorkerParameters(),
            handlerIO
        );

        // Then
        assertThat(itemStatus).isNotNull();
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);
    }

    @Test
    @RunWithCustomExecutor
    public void whenCheckQueriesWithThresholdLTDefaultThresholdThenReturnOK() throws Exception {
        // Given
        HandlerIO handlerIO = mock(HandlerIO.class);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        JsonNode queryUnit = JsonHandler.getFromInputStream(
            getClass().getResourceAsStream("/CheckQueriesThreshold/queryWithThreshold.json")
        );
        given(handlerIO.getJsonFromWorkspace("query.json")).willReturn(queryUnit);

        VitamConfiguration.setQueriesThreshold(6L);
        checkDistributionThreshold = new CheckQueriesThreshold();

        // When
        ItemStatus itemStatus = checkDistributionThreshold.execute(
            WorkerParametersFactory.newWorkerParameters(),
            handlerIO
        );

        // Then
        assertThat(itemStatus).isNotNull();
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);
    }

    @Test
    @RunWithCustomExecutor
    public void whenCheckQueriesWithThresholdGTDefaultThresholdThenReturnWARNING() throws Exception {
        // Given
        HandlerIO handlerIO = mock(HandlerIO.class);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        JsonNode queryUnit = JsonHandler.getFromInputStream(
            getClass().getResourceAsStream("/CheckQueriesThreshold/queryWithThreshold.json")
        );
        given(handlerIO.getJsonFromWorkspace("query.json")).willReturn(queryUnit);

        // When
        ItemStatus itemStatus = checkDistributionThreshold.execute(
            WorkerParametersFactory.newWorkerParameters(),
            handlerIO
        );

        // Then
        assertThat(itemStatus).isNotNull();
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.WARNING);
    }

    @Test
    @RunWithCustomExecutor
    public void whenCheckQueriesWithThresholdThenReturnKO() throws Exception {
        // Given
        HandlerIO handlerIO = mock(HandlerIO.class);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        JsonNode queryUnit = JsonHandler.getFromInputStream(
            getClass().getResourceAsStream("/CheckQueriesThreshold/queryWithThresholdKO.json")
        );
        given(handlerIO.getJsonFromWorkspace("query.json")).willReturn(queryUnit);

        // When
        ItemStatus itemStatus = checkDistributionThreshold.execute(
            WorkerParametersFactory.newWorkerParameters(),
            handlerIO
        );

        // Then
        assertThat(itemStatus).isNotNull();
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);
    }

    @Test
    @RunWithCustomExecutor
    public void whenCheckQueriesWithNullThresholdThenReturnKO() throws Exception {
        // Given
        HandlerIO handlerIO = mock(HandlerIO.class);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        JsonNode queryUnit = JsonHandler.getFromInputStream(
            getClass().getResourceAsStream("/CheckQueriesThreshold/queryWithNullThreshold.json")
        );
        given(handlerIO.getJsonFromWorkspace("query.json")).willReturn(queryUnit);

        // When
        ItemStatus itemStatus = checkDistributionThreshold.execute(
            WorkerParametersFactory.newWorkerParameters(),
            handlerIO
        );

        // Then
        assertThat(itemStatus).isNotNull();
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);
    }
}
