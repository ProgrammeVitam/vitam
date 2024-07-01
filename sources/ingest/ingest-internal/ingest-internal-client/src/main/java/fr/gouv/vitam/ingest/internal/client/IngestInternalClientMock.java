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
package fr.gouv.vitam.ingest.internal.client;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.client.AbstractMockClient;
import fr.gouv.vitam.common.client.ClientMockResultHelper;
import fr.gouv.vitam.common.client.IngestCollection;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessQuery;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.processing.ProcessDetail;
import fr.gouv.vitam.common.model.processing.WorkFlow;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Mock client implementation for Ingest Internal
 */
public class IngestInternalClientMock extends AbstractMockClient implements IngestInternalClient {

    private static final String PARAMS_CANNOT_BE_NULL = "Params cannot be null";

    /**
     * identifier of execution
     */
    public static final String ID = "identifier1";
    private static final String FAKE_EXECUTION_STATUS = "Fake";

    @Override
    public void upload(InputStream inputStream, MediaType archiveType, WorkFlow workflowIdentifier, String action) {
        ParametersChecker.checkParameter(PARAMS_CANNOT_BE_NULL, inputStream, archiveType);
        StreamUtils.closeSilently(inputStream);
    }

    @Override
    public void uploadInitialLogbook(Iterable<LogbookOperationParameters> logbookParametersList) {
        ParametersChecker.checkParameter(PARAMS_CANNOT_BE_NULL, logbookParametersList);
    }

    @Override
    public Response downloadObjectAsync(String objectId, IngestCollection type) {
        return ClientMockResultHelper.getObjectStream();
    }

    public ItemStatus getOperationProcessStatus(String id) {
        return new ItemStatus(ID);
    }

    @Override
    public RequestResponse<ItemStatus> getOperationProcessExecutionDetails(String id) {
        return new RequestResponseOK<ItemStatus>().addResult(new ItemStatus(ID));
    }

    @Override
    public RequestResponse<ItemStatus> cancelOperationProcessExecution(String id) {
        final List<Integer> status = new ArrayList<>();
        status.add(Status.OK.getStatusCode());
        final ItemStatus itemStatus = new ItemStatus(
            id,
            "FakeMessage - The operation has been canceled",
            StatusCode.OK,
            status,
            Collections.emptyMap(),
            null,
            null
        );
        return new RequestResponseOK<ItemStatus>()
            .addResult(itemStatus)
            .addHeader(GlobalDataRest.X_GLOBAL_EXECUTION_STATE, FAKE_EXECUTION_STATUS)
            .setHttpCode(Status.ACCEPTED.getStatusCode());
    }

    @Override
    public RequestResponse<ItemStatus> updateOperationActionProcess(String actionId, String operationId) {
        return new RequestResponseOK<ItemStatus>()
            .addHeader(GlobalDataRest.X_GLOBAL_EXECUTION_STATE, FAKE_EXECUTION_STATUS)
            .setHttpCode(Status.OK.getStatusCode());
    }

    @Override
    public void initWorkflow(WorkFlow contextId) {}

    @Override
    public RequestResponse<ProcessDetail> listOperationsDetails(ProcessQuery query) {
        return new RequestResponseOK<ProcessDetail>()
            .addResult(new ProcessDetail())
            .setHttpCode(Status.OK.getStatusCode());
    }

    @Override
    public RequestResponse<WorkFlow> getWorkflowDefinitions() {
        return new RequestResponseOK<WorkFlow>().addResult(new WorkFlow()).setHttpCode(Status.OK.getStatusCode());
    }

    @Override
    public Optional<WorkFlow> getWorkflowDetails(String WorkflowIdentifier) {
        return Optional.of(WorkFlow.of("DEFAULT_WORKFLOW", "PROCESS_SIP_UNITARY", "INGEST"));
    }

    @Override
    public void saveObjectToWorkspace(String id, String objectName, InputStream inputStream)
        throws VitamClientException {}
}
