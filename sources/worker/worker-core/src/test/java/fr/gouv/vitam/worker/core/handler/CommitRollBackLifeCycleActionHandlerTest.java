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
package fr.gouv.vitam.worker.core.handler;

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
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

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
        commitAction = new HandlerIOImpl(
            containerName.getId(),
            "workerId",
            com.google.common.collect.Lists.newArrayList()
        );

        WorkerParameters params = WorkerParametersFactory.newWorkerParameters()
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
        commitAction = new HandlerIOImpl(
            containerName.getId(),
            "workerId",
            com.google.common.collect.Lists.newArrayList()
        );

        WorkerParameters params = WorkerParametersFactory.newWorkerParameters()
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
        rollBackAction = new HandlerIOImpl(
            containerName.getId(),
            "workerId",
            com.google.common.collect.Lists.newArrayList()
        );

        response = rollBackHandler.execute(params, rollBackAction);
        assertEquals(response.getGlobalStatus(), StatusCode.OK);

        // RollBack with a failed workFlow
        params.putParameterValue(WorkerParameterName.workflowStatusKo, StatusCode.FATAL.toString());
        rollBackAction = new HandlerIOImpl(
            containerName.getId(),
            "workerId",
            com.google.common.collect.Lists.newArrayList()
        );

        response = rollBackHandler.execute(params, rollBackAction);
        assertEquals(response.getGlobalStatus(), StatusCode.OK);
    }
}
