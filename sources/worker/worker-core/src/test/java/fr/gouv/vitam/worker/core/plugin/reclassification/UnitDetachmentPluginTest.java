package fr.gouv.vitam.worker.core.plugin.reclassification;

import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.LifeCycleStatusCode;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleUnitParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.plugin.reclassification.model.ReclassificationEventDetails;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Arrays;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class UnitDetachmentPluginTest {

    private static final String UNITS_TO_DETACH_DIR = "UnitsToDetach";

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory;
    @Mock
    private LogbookLifeCyclesClient logbookLifeCyclesClient;

    @Mock
    private MetaDataClientFactory metaDataClientFactory;
    @Mock
    private MetaDataClient metaDataClient;

    @InjectMocks
    private UnitDetachmentPlugin unitDetachmentPlugin;

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @Before
    public void init() {
        doReturn(logbookLifeCyclesClient).when(logbookLifeCyclesClientFactory).getClient();
        doReturn(metaDataClient).when(metaDataClientFactory).getClient();
    }


    @Test
    @RunWithCustomExecutor
    public void testDetachment() throws Exception {

        VitamThreadUtils.getVitamSession().setTenantId(0);

        // Given
        String containedId = GUIDFactory.newGUID().toString();
        String unitId = GUIDFactory.newGUID().toString();
        final WorkerParameters parameters =
            WorkerParametersFactory.newWorkerParameters().setWorkerGUID(GUIDFactory
                .newGUID()).setContainerName(containedId)
                .setObjectNameList(Lists.newArrayList(unitId))
                .setObjectName(unitId).setCurrentStep("StepName");

        HandlerIO handlerIO = mock(HandlerIO.class);
        doReturn(JsonHandler.toJsonNode(new HashSet<>(Arrays.asList("parentId1", "parentId2"))))
            .when(handlerIO).getJsonFromWorkspace(eq(UNITS_TO_DETACH_DIR + "/" + unitId));

        // When
        unitDetachmentPlugin.execute(parameters, handlerIO);

        // Then
        verify(metaDataClient).updateUnitById(any(), eq(unitId));

        ArgumentCaptor<LogbookLifeCycleUnitParameters> logbookLCParam =
            ArgumentCaptor.forClass(LogbookLifeCycleUnitParameters.class);
        verify(logbookLifeCyclesClient).update(logbookLCParam.capture(), eq(LifeCycleStatusCode.LIFE_CYCLE_COMMITTED));

        assertThat(logbookLCParam.getValue().getStatus()).isEqualTo(StatusCode.OK);
        String evDetData = logbookLCParam.getValue().getMapParameters().get(LogbookParameterName.eventDetailData);
        ReclassificationEventDetails eventDetails =
            JsonHandler.getFromString(evDetData, ReclassificationEventDetails.class);
        assertThat(eventDetails.getRemovedParents()).containsExactlyInAnyOrder("parentId1", "parentId2");
    }
}
