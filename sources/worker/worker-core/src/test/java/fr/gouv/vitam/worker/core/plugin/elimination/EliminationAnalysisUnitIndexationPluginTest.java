package fr.gouv.vitam.worker.core.plugin.elimination;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.plugin.elimination.model.EliminationAnalysisResult;
import fr.gouv.vitam.worker.core.plugin.elimination.model.EliminationGlobalStatus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

public class EliminationAnalysisUnitIndexationPluginTest {

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private MetaDataClientFactory metaDataClientFactory;

    @Mock
    private MetaDataClient metaDataClient;

    @InjectMocks
    private EliminationAnalysisUnitIndexationPlugin instance;

    @Mock
    private HandlerIO handlerIO;

    private WorkerParameters parameters;

    @Before
    public void init() throws Exception {

        VitamThreadUtils.getVitamSession().setTenantId(0);
        VitamThreadUtils.getVitamSession().setRequestId("opId");

        given(metaDataClientFactory.getClient()).willReturn(metaDataClient);

        this.parameters = WorkerParametersFactory.newWorkerParameters().setWorkerGUID(GUIDFactory
            .newGUID()).setContainerName(VitamThreadUtils.getVitamSession().getRequestId())
            .setRequestId(VitamThreadUtils.getVitamSession().getRequestId())
            .setObjectName("unit1")
            .setCurrentStep("StepName");

        EliminationAnalysisResult eliminationAnalysisResult = new EliminationAnalysisResult(
            "opId", EliminationGlobalStatus.DESTROY,
            new HashSet<>(Collections.singletonList("sp1")),
            Collections.emptySet(),
            Collections.emptyList());

        doReturn(JsonHandler.writeToInpustream(eliminationAnalysisResult))
            .when(handlerIO).getInputStreamFromWorkspace("unitMetadata/unit1.json");
    }

    @Test
    @RunWithCustomExecutor
    public void testIndexUnit_OK() throws Exception {

        // Given

        // When
        instance.execute(parameters, handlerIO);

        // Then
        ArgumentCaptor<JsonNode> updateDsl = ArgumentCaptor.forClass(JsonNode.class);

        verify(metaDataClient).updateUnitbyId(updateDsl.capture(), eq("unit1"));
        String update = JsonHandler.unprettyPrint(updateDsl.getValue());
        assertThat(update).contains("{\"$push\":{\"#elimination\"");
        assertThat(update).contains("\"OperationId\":\"opId\"");
    }

    @Test
    @RunWithCustomExecutor
    public void testIndexUnit_OnMetadataExceptionThenFatal() throws Exception {

        // Given
        doThrow(MetaDataExecutionException.class).when(metaDataClient).updateUnitbyId(any(), any());

        // When / Then
        ItemStatus itemStatus = instance.execute(parameters, handlerIO);
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.FATAL);
    }
}
