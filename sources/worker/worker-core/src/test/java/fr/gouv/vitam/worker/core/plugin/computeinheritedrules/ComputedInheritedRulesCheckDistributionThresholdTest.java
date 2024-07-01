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
package fr.gouv.vitam.worker.core.plugin.computeinheritedrules;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.plugin.common.CheckDistributionThresholdBase;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public class ComputedInheritedRulesCheckDistributionThresholdTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Mock
    private MetaDataClientFactory metaDataClientFactory;

    private static final int TENANT_ID = 0;

    private CheckDistributionThresholdBase checkDistributionThresholdBase;

    @Before
    public void setUp() throws Exception {
        VitamConfiguration.setDistributionThreshold(15L);

        checkDistributionThresholdBase = new ComputedInheritedRulesCheckDistributionThreshold(metaDataClientFactory);
    }

    @Test
    @RunWithCustomExecutor
    public void whenCheckDistributionComputedInheritedRulesDefaultThresholdOnSelectQueryThenReturnKO()
        throws Exception {
        // Given
        HandlerIO handlerIO = mock(HandlerIO.class);
        MetaDataClient metaDataClient = mock(MetaDataClient.class);
        given(metaDataClientFactory.getClient()).willReturn(metaDataClient);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        given(handlerIO.getInput(0)).willReturn("SELECT");
        given(handlerIO.getInput(1)).willReturn("query.json");

        JsonNode queryUnit = JsonHandler.getFromInputStream(
            getClass().getResourceAsStream("/computeInheritedRules/select_with_default_threshold.json")
        );
        given(handlerIO.getJsonFromWorkspace("query.json")).willReturn(queryUnit);
        given(metaDataClient.selectUnits(any())).willReturn(
            JsonHandler.getFromInputStream(
                getClass().getResourceAsStream("/computeInheritedRules/result_metadata_exeed_threshold.json")
            )
        );

        // When
        ItemStatus itemStatus = checkDistributionThresholdBase.execute(
            WorkerParametersFactory.newWorkerParameters(),
            handlerIO
        );

        // Then
        assertThat(itemStatus).isNotNull();
        assertThat(itemStatus.getData().toString()).isEqualTo(
            "{eventDetailData={\"error\":\"Too many units found. Threshold=100000000, found=100000002\"}}"
        );
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);
    }
}
