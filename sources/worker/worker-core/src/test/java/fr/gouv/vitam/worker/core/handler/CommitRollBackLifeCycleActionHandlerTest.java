package fr.gouv.vitam.worker.core.handler;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;

import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.BusinessObjectType;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.processing.IOParameter;
import fr.gouv.vitam.common.model.processing.ProcessingUri;
import fr.gouv.vitam.common.model.processing.UriPrefix;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.processing.common.parameter.WorkerParameterName;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.core.impl.HandlerIOImpl;

public class CommitRollBackLifeCycleActionHandlerTest {

    CommitLifeCycleActionHandler commitUnitHandler = new CommitLifeCycleUnitActionHandler();
    CommitLifeCycleActionHandler commitObjectGroupHandler = new CommitLifeCycleObjectGroupActionHandler();
    RollBackActionHandler rollBackHandler = new RollBackActionHandler();
    private HandlerIOImpl commitAction;
    private HandlerIOImpl rollBackAction;
    private static final String WORKSPACE_URL = "http://localhost:8083";
    private static final String METADATA_URL = "http://localhost:8084";
    private static final String COMMIT_STEP = "COMMIT_STEP";

    @Before
    public void before() {
        LogbookOperationsClientFactory.changeMode(null);
        LogbookLifeCyclesClientFactory.changeMode(null);
    }

    @Test
    public void givenOperationIdObjectIdThenReturnCommitOk() {
        GUID containerName = GUIDFactory.newGUID();
        String unit = "unit_1.xml";
        String object = "object_group_1.json";
        commitAction = new HandlerIOImpl(containerName.getId(), "workerId");

        WorkerParameters params =
                WorkerParametersFactory.newWorkerParameters()
                        .setUrlWorkspace(WORKSPACE_URL)
                        .setUrlMetadata(METADATA_URL)
                        .setObjectNameList(Lists.newArrayList(unit))
                        .setObjectName(unit)
                        .setCurrentStep(COMMIT_STEP)
                        .setContainerName(containerName.getId());

        // Commit a Unit lifeCycle
        ItemStatus response = commitUnitHandler.execute(params, commitAction);
        assertEquals(response.getGlobalStatus(), StatusCode.OK);

        // Commit an objectGroup lifeCycle
        params.setObjectName(object);
        response = commitObjectGroupHandler.execute(params, commitAction);
        assertEquals(response.getGlobalStatus(), StatusCode.OK);
    }

    @Test
    public void givenOperationIdObjectIdThenReturnRollBackOk() {
        GUID containerName = GUIDFactory.newGUID();
        String unit = "unit_1.xml";
        commitAction = new HandlerIOImpl(containerName.getId(), "workerId");

        WorkerParameters params =
                WorkerParametersFactory.newWorkerParameters()
                        .setUrlWorkspace(WORKSPACE_URL)
                        .setUrlMetadata(METADATA_URL)
                        .setObjectNameList(Lists.newArrayList(unit))
                        .setObjectName(unit)
                        .setCurrentStep(COMMIT_STEP)
                        .setContainerName(containerName.getId());

        // Commit a Unit lifeCycle
        List<IOParameter> in = new ArrayList<>();
        in.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.VALUE, BusinessObjectType.UNIT.toString())));
        commitAction.addInIOParameters(in);

        ItemStatus response = commitUnitHandler.execute(params, commitAction);
        assertEquals(response.getGlobalStatus(), StatusCode.OK);

        // RollBack with a successful workFlow
        params.putParameterValue(WorkerParameterName.workflowStatusKo, StatusCode.OK.toString());
        rollBackAction = new HandlerIOImpl(containerName.getId(), "workerId");

        response = rollBackHandler.execute(params, rollBackAction);
        assertEquals(response.getGlobalStatus(), StatusCode.OK);

        // RollBack with a failed workFlow
        params.putParameterValue(WorkerParameterName.workflowStatusKo, StatusCode.FATAL.toString());
        rollBackAction = new HandlerIOImpl(containerName.getId(), "workerId");

        response = rollBackHandler.execute(params, rollBackAction);
        assertEquals(response.getGlobalStatus(), StatusCode.OK);
    }

}
