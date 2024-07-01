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
package fr.gouv.vitam.processing.management.client;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.client.AbstractMockClient;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessPause;
import fr.gouv.vitam.common.model.ProcessQuery;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.processing.Action;
import fr.gouv.vitam.common.model.processing.ActionDefinition;
import fr.gouv.vitam.common.model.processing.Distribution;
import fr.gouv.vitam.common.model.processing.DistributionKind;
import fr.gouv.vitam.common.model.processing.IOParameter;
import fr.gouv.vitam.common.model.processing.ProcessBehavior;
import fr.gouv.vitam.common.model.processing.ProcessDetail;
import fr.gouv.vitam.common.model.processing.ProcessingUri;
import fr.gouv.vitam.common.model.processing.Step;
import fr.gouv.vitam.common.model.processing.WorkFlow;
import fr.gouv.vitam.processing.common.ProcessingEntry;
import fr.gouv.vitam.processing.common.model.WorkerBean;

import javax.ws.rs.core.Response.Status;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 *
 */
public class ProcessingManagementClientMock extends AbstractMockClient implements ProcessingManagementClient {

    private static final String FAKE_EXECUTION_STATUS = "Fake";

    ProcessingManagementClientMock() {
        // Empty
    }

    @Override
    public ItemStatus getOperationProcessStatus(String id) {
        final List<Integer> status = new ArrayList<>();
        status.add(0);
        status.add(0);
        status.add(1);
        status.add(0);
        status.add(0);
        status.add(0);
        return new ItemStatus("FakeId", "FakeMessage", StatusCode.OK, status, Collections.emptyMap(), null, null);
    }

    @Override
    public RequestResponse<ItemStatus> getOperationProcessExecutionDetails(String id) {
        final List<Integer> status = new ArrayList<>();
        status.add(0);
        status.add(0);
        status.add(1);
        status.add(0);
        status.add(0);
        status.add(0);
        ItemStatus itemStatus = new ItemStatus(
            "FakeId",
            "FakeMessage",
            StatusCode.OK,
            status,
            Collections.emptyMap(),
            null,
            null
        );

        return new RequestResponseOK<ItemStatus>()
            .addResult(itemStatus)
            .setHttpCode(StatusCode.OK.getEquivalentHttpStatus().getStatusCode());
    }

    @Override
    public RequestResponse<ItemStatus> cancelOperationProcessExecution(String id) {
        final List<Integer> status = new ArrayList<>();
        status.add(0);
        status.add(0);
        status.add(1);
        status.add(0);
        status.add(0);
        status.add(0);
        final ItemStatus itemStatus = new ItemStatus(
            "FakeId",
            "FakeMessage",
            StatusCode.OK,
            status,
            Collections.emptyMap(),
            null,
            null
        );

        return new RequestResponseOK<ItemStatus>().addResult(itemStatus);
    }

    @Override
    public RequestResponse<ItemStatus> updateOperationActionProcess(String actionId, String operationId) {
        return new RequestResponseOK<>();
    }

    @Override
    public RequestResponse<ItemStatus> executeOperationProcess(String operationId, String workflow, String actionId) {
        return new RequestResponseOK<ItemStatus>().addHeader(
            GlobalDataRest.X_GLOBAL_EXECUTION_STATE,
            FAKE_EXECUTION_STATUS
        );
    }

    @Override
    public void registerWorker(String familyId, String workerId, WorkerBean workerDescription) {}

    @Override
    public void unregisterWorker(String familyId, String workerId) {}

    @Override
    public void initVitamProcess(String container, String workflowId) {}

    @Override
    public RequestResponse<ProcessDetail> listOperationsDetails(ProcessQuery query) {
        ProcessDetail pw = new ProcessDetail();
        pw.setOperationId(GUIDFactory.newOperationLogbookGUID(0).toString());
        pw.setGlobalState(ProcessState.RUNNING.toString());
        pw.setStepStatus(StatusCode.STARTED.toString());
        return new RequestResponseOK<ProcessDetail>().addResult(pw);
    }

    @Override
    public RequestResponse<WorkFlow> getWorkflowDefinitions() {
        List<WorkFlow> workflowDefinitions = new ArrayList<>();
        WorkFlow workflow = new WorkFlow();

        List<Action> actions = new ArrayList<>();
        actions.add(
            getAction(
                "CHECK_DIGEST",
                ProcessBehavior.BLOCKING,
                new ArrayList<>(
                    Arrays.asList(new IOParameter().setName("algo").setUri(new ProcessingUri("VALUE", "SHA-512")))
                ),
                null
            )
        );
        actions.add(getAction("OG_OBJECTS_FORMAT_CHECK", ProcessBehavior.BLOCKING, null, null));

        List<Step> steps = new ArrayList<>();
        steps.add(
            new Step()
                .setWorkerGroupId("DefaultWorker")
                .setStepName("STP_OG_CHECK_AND_TRANSFORME")
                .setBehavior(ProcessBehavior.BLOCKING)
                .setDistribution(
                    new Distribution().setKind(DistributionKind.LIST_ORDERING_IN_FILE).setElement("ObjectGroup")
                )
                .setActions(actions)
        );

        workflow.setId("DefaultIngestWorkflow");
        workflow.setIdentifier("PROCESS_SIP_UNITARY");
        workflow.setName("Default Ingest Workflow");
        workflow.setTypeProc("INGEST");
        workflow.setComment("DefaultIngestWorkflow comment");
        workflow.setSteps(steps);
        workflowDefinitions.add(workflow);

        return new RequestResponseOK().addResult(workflowDefinitions).setHttpCode(Status.OK.getStatusCode());
    }

    @Override
    public Optional<WorkFlow> getWorkflowDetails(String WorkflowIdentifier) {
        throw new IllegalStateException("Method getWorkflowDetails not implemented");
    }

    @Override
    public void initVitamProcess(ProcessingEntry entry) {}

    /**
     * Create a POJO action
     *
     * @param actionKey action key
     * @param actionBehavior action behavior
     * @param in list of IO ins
     * @param out list of IO outs
     * @return Action object
     */
    private Action getAction(
        String actionKey,
        ProcessBehavior actionBehavior,
        List<IOParameter> in,
        List<IOParameter> out
    ) {
        ActionDefinition actionDefinition = new ActionDefinition();
        actionDefinition.setActionKey(actionKey);
        actionDefinition.setBehavior(actionBehavior);
        actionDefinition.setIn(in);
        actionDefinition.setOut(out);
        return new Action().setActionDefinition(actionDefinition);
    }

    @Override
    public RequestResponse<ProcessPause> forcePause(ProcessPause info) {
        return new RequestResponseOK<ProcessPause>().addResult(info).setHttpCode(Status.OK.getStatusCode());
    }

    @Override
    public RequestResponse<ProcessPause> removeForcePause(ProcessPause info) {
        return new RequestResponseOK<ProcessPause>().addResult(info).setHttpCode(Status.OK.getStatusCode());
    }
}
