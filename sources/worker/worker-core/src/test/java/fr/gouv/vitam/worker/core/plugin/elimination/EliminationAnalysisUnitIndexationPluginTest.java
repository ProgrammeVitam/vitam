package fr.gouv.vitam.worker.core.plugin.elimination;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.worker.core.plugin.elimination.exception.EliminationException;
import fr.gouv.vitam.worker.core.plugin.elimination.model.EliminationAnalysisResult;
import fr.gouv.vitam.worker.core.plugin.elimination.model.EliminationGlobalStatus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
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

    @Before
    public void init() {

        VitamThreadUtils.getVitamSession().setTenantId(0);
        VitamThreadUtils.getVitamSession().setRequestId("opId");

        given(metaDataClientFactory.getClient()).willReturn(metaDataClient);
    }

    @Test
    @RunWithCustomExecutor
    public void testIndexUnit_OK() throws Exception {

        // Given
        EliminationAnalysisUnitIndexationPlugin instance = new EliminationAnalysisUnitIndexationPlugin(
            metaDataClientFactory,
            new EliminationAnalysisService()
        );

        EliminationAnalysisResult eliminationAnalysisResult = new EliminationAnalysisResult(
            "opId", EliminationGlobalStatus.DESTROY,
            new HashSet<>(Collections.singletonList("sp1")),
            Collections.emptySet(),
            Collections.emptyList());

        // When
        instance.indexUnit("unit1", eliminationAnalysisResult);

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
        EliminationAnalysisUnitIndexationPlugin instance = new EliminationAnalysisUnitIndexationPlugin(
            metaDataClientFactory,
            new EliminationAnalysisService()
        );

        EliminationAnalysisResult eliminationAnalysisResult = new EliminationAnalysisResult(
            "opId", EliminationGlobalStatus.DESTROY,
            new HashSet<>(Collections.singletonList("sp1")),
            Collections.emptySet(),
            Collections.emptyList());

        doThrow(MetaDataExecutionException.class).when(metaDataClient).updateUnitbyId(any(), any());

        // When / Then
        assertThatThrownBy(() -> instance.indexUnit("unit1", eliminationAnalysisResult))
            .isInstanceOf(EliminationException.class)
            .hasCauseInstanceOf(MetaDataExecutionException.class)
            .matches(e -> ((EliminationException)e).getStatusCode() == StatusCode.FATAL);
    }
}
