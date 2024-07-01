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
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.common.HandlerIO;
import net.javacrumbs.jsonunit.JsonAssert;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;

public class ComputeInheritedRulesActionPluginTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Mock
    private MetaDataClientFactory metaDataClientFactory;

    @Mock
    private MetaDataClient metaDataClient;

    @Mock
    private HandlerIO HandlerIO;

    private ComputeInheritedRulesActionPlugin ComputeInheritedRulesActionPlugin;
    private WorkerParameters workerParameters;

    @Before
    public void setUp() throws Exception {
        when(metaDataClientFactory.getClient()).thenReturn(metaDataClient);
        ComputeInheritedRulesActionPlugin = new ComputeInheritedRulesActionPlugin(metaDataClientFactory);
        workerParameters = WorkerParametersFactory.newWorkerParameters();
        workerParameters.setObjectNameList(Lists.newArrayList("a", "b", "c", "d"));
        List<String> tenant = Arrays.asList("0", "2");
        VitamConfiguration.setIndexInheritedRulesWithRulesIdByTenant(tenant);
        VitamConfiguration.setIndexInheritedRulesWithAPIV2OutputByTenant(tenant);
    }

    @Test
    @RunWithCustomExecutor
    public void should_launch_plugin_with_multiple_input_then_return_ok() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(0);
        JsonNode response = getJsonNodeResponse();
        given(metaDataClient.selectUnitsWithInheritedRules(ArgumentMatchers.any())).willReturn(response);
        // When
        List<ItemStatus> itemStatus = ComputeInheritedRulesActionPlugin.executeList(workerParameters, HandlerIO);
        // Then
        assertThat(itemStatus).hasSize(4);
        assertThat(itemStatus.stream().filter(i -> i.getGlobalStatus() == StatusCode.OK)).hasSize(4);
    }

    @Test
    @RunWithCustomExecutor
    public void check_full_computed_inherited_rules() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(0);
        JsonNode response = getJsonNodeResponse();
        JsonNode expectedJson = getExpectedJsonNode();

        ArgumentCaptor<JsonNode> objectNodeArgumentCaptor = initializeMockWithResponse(response);
        // When
        List<ItemStatus> itemStatus = ComputeInheritedRulesActionPlugin.executeList(workerParameters, HandlerIO);
        // Then
        assertThat(itemStatus).hasSize(4);
        assertThat(itemStatus.stream().filter(i -> i.getGlobalStatus() == StatusCode.OK)).hasSize(4);
        JsonNode updatedUnit = objectNodeArgumentCaptor.getValue();

        JsonAssert.assertJsonEquals(
            expectedJson,
            updatedUnit,
            JsonAssert.whenIgnoringPaths("$action[*].$set.#computedInheritedRules.indexationDate")
        );
    }

    private ArgumentCaptor<JsonNode> initializeMockWithResponse(JsonNode response)
        throws MetaDataDocumentSizeException, InvalidParseOperationException, MetaDataClientServerException, MetaDataExecutionException, MetaDataNotFoundException {
        given(metaDataClient.selectUnitsWithInheritedRules(ArgumentMatchers.any())).willReturn(response);
        ArgumentCaptor<JsonNode> objectNodeArgumentCaptor = ArgumentCaptor.forClass(JsonNode.class);
        when(
            metaDataClient.updateUnitById(objectNodeArgumentCaptor.capture(), ArgumentMatchers.anyString())
        ).thenReturn(null);
        return objectNodeArgumentCaptor;
    }

    private JsonNode getJsonNodeResponse() throws InvalidParseOperationException, FileNotFoundException {
        return JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream("computeInheritedRules/InheritedRulesResponse.json")
        );
    }

    private JsonNode getExpectedJsonNode() throws InvalidParseOperationException, FileNotFoundException {
        return JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream("computeInheritedRules/response.json")
        );
    }
}
