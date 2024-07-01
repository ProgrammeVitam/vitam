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
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.client.DefaultClient;
import fr.gouv.vitam.common.client.VitamRequestBuilder;
import fr.gouv.vitam.common.exception.AccessUnauthorizedException;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.ConflictClientException;
import fr.gouv.vitam.common.exception.ForbiddenClientException;
import fr.gouv.vitam.common.exception.InternalServerException;
import fr.gouv.vitam.common.exception.PreconditionFailedClientException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.exception.WorkflowNotFoundException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.ProcessPause;
import fr.gouv.vitam.common.model.ProcessQuery;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.processing.ProcessDetail;
import fr.gouv.vitam.common.model.processing.WorkFlow;
import fr.gouv.vitam.processing.common.ProcessingEntry;
import fr.gouv.vitam.processing.common.exception.ProcessingBadRequestException;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.model.WorkerBean;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.Optional;

import static fr.gouv.vitam.common.client.VitamRequestBuilder.delete;
import static fr.gouv.vitam.common.client.VitamRequestBuilder.get;
import static fr.gouv.vitam.common.client.VitamRequestBuilder.head;
import static fr.gouv.vitam.common.client.VitamRequestBuilder.post;
import static fr.gouv.vitam.common.client.VitamRequestBuilder.put;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.fromStatusCode;

/**
 * Processing Management Client
 */
class ProcessingManagementClientRest extends DefaultClient implements ProcessingManagementClient {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProcessingManagementClientRest.class);

    private static final String ERR_CONTAINER_IS_MANDATORY = "Container is mandatory";
    private static final String ERR_WORKFLOW_IS_MANDATORY = "Workflow is mandatory";
    private static final String ILLEGAL_ARGUMENT = "Illegal Argument";
    private static final String NOT_FOUND = "Not Found";
    private static final String BAD_REQUEST_EXCEPTION = "Bad Request Exception";

    private static final String OPERATION_URI = "/operations";
    private static final String WORKFLOWS_URI = "/workflows";
    private static final String FORCE_PAUSE_URI = "/forcepause";
    private static final String REMOVE_FORCE_PAUSE_URI = "/removeforcepause";

    private static final String ACTION_ID_MUST_HAVE_A_VALID_VALUE = "Action id must have a valid value";
    private static final String BLANK_OPERATION_ID = "Operation identifier should be filled";

    ProcessingManagementClientRest(ProcessingManagementClientFactory factory) {
        super(factory);
    }

    @Override
    public void initVitamProcess(String container, String workflowId)
        throws BadRequestException, InternalServerException {
        initVitamProcess(new ProcessingEntry(container, workflowId));
    }

    @Override
    public void initVitamProcess(ProcessingEntry entry) throws InternalServerException, BadRequestException {
        ParametersChecker.checkParameter(ERR_CONTAINER_IS_MANDATORY, entry.getContainer());
        ParametersChecker.checkParameter(ERR_WORKFLOW_IS_MANDATORY, entry.getWorkflow());

        VitamRequestBuilder request = post()
            .withPath(OPERATION_URI + "/" + entry.getContainer())
            .withHeader(GlobalDataRest.X_CONTEXT_ID, entry.getWorkflow())
            .withHeader(GlobalDataRest.X_ACTION, ProcessAction.INIT)
            .withBody(entry)
            .withJson();
        try (Response response = make(request)) {
            checkWithSpecificException(response);
        } catch (
            VitamClientInternalException
            | ForbiddenClientException
            | ConflictClientException
            | AccessUnauthorizedException e
        ) {
            throw new InternalServerException(INTERNAL_SERVER_ERROR.getReasonPhrase(), e);
        } catch (PreconditionFailedClientException e) {
            throw new IllegalArgumentException(ILLEGAL_ARGUMENT, e);
        }
    }

    @Override
    public RequestResponse<ItemStatus> executeOperationProcess(String operationId, String workflowId, String actionId)
        throws InternalServerException, VitamClientException {
        ParametersChecker.checkParameter(BLANK_OPERATION_ID, operationId);
        ParametersChecker.checkParameter(ACTION_ID_MUST_HAVE_A_VALID_VALUE, actionId);
        ParametersChecker.checkParameter("workflow is a mandatory parameter", workflowId);
        VitamRequestBuilder request = post()
            .withPath(OPERATION_URI + "/" + operationId)
            .withHeader(GlobalDataRest.X_ACTION, actionId)
            .withHeader(GlobalDataRest.X_CONTEXT_ID, workflowId)
            .withBody(new ProcessingEntry(operationId, workflowId))
            .withJson();

        try (Response response = make(request)) {
            checkWithSpecificException(response);
            return RequestResponse.parseFromResponse(response, ItemStatus.class);
        } catch (
            ConflictClientException
            | WorkflowNotFoundException
            | PreconditionFailedClientException
            | BadRequestException
            | ForbiddenClientException e
        ) {
            throw new VitamClientInternalException(e);
        }
    }

    @Override
    public RequestResponse<ItemStatus> updateOperationActionProcess(String actionId, String operationId)
        throws InternalServerException, VitamClientException {
        ParametersChecker.checkParameter(BLANK_OPERATION_ID, operationId);
        ParametersChecker.checkParameter(ACTION_ID_MUST_HAVE_A_VALID_VALUE, actionId);
        VitamRequestBuilder request = put()
            .withPath(OPERATION_URI + "/" + operationId)
            .withHeader(GlobalDataRest.X_ACTION, actionId)
            .withJson();
        try (Response response = make(request)) {
            checkWithSpecificException(response);
            return RequestResponse.parseFromResponse(response, ItemStatus.class);
        } catch (
            ForbiddenClientException
            | WorkflowNotFoundException
            | BadRequestException
            | ConflictClientException
            | PreconditionFailedClientException e
        ) {
            throw new VitamClientInternalException(e);
        }
    }

    @Override
    public ItemStatus getOperationProcessStatus(String operationId)
        throws InternalServerException, BadRequestException, VitamClientException {
        ParametersChecker.checkParameter(BLANK_OPERATION_ID, operationId);
        VitamRequestBuilder request = head().withPath(OPERATION_URI + "/" + operationId).withJsonAccept();
        Response response = null;
        try {
            response = make(request);
            checkWithSpecificException(response);
            return getItemStatusFromResponse(response);
        } catch (ForbiddenClientException | ConflictClientException e) {
            return getItemStatusFromResponse(response);
        } catch (PreconditionFailedClientException e) {
            throw new BadRequestException(ILLEGAL_ARGUMENT, e);
        } finally {
            if (response != null && !SUCCESSFUL.equals(response.getStatusInfo().getFamily())) {
                response.close();
            }
        }
    }

    private static ItemStatus getItemStatusFromResponse(Response response) {
        return new ItemStatus()
            .setGlobalState(ProcessState.valueOf(response.getHeaderString(GlobalDataRest.X_GLOBAL_EXECUTION_STATE)))
            .setLogbookTypeProcess(response.getHeaderString(GlobalDataRest.X_CONTEXT_ID))
            .increment(StatusCode.valueOf(response.getHeaderString(GlobalDataRest.X_GLOBAL_EXECUTION_STATUS)));
    }

    @Override
    public RequestResponse<ItemStatus> getOperationProcessExecutionDetails(String id)
        throws InternalServerException, VitamClientException {
        ParametersChecker.checkParameter(BLANK_OPERATION_ID, id);
        VitamRequestBuilder request = get().withPath(OPERATION_URI + "/" + id).withJsonAccept();
        try (Response response = make(request)) {
            checkWithSpecificException(response);
            return RequestResponse.parseFromResponse(response, ItemStatus.class);
        } catch (
            ForbiddenClientException
            | WorkflowNotFoundException
            | BadRequestException
            | ConflictClientException
            | PreconditionFailedClientException e
        ) {
            throw new VitamClientInternalException(e);
        }
    }

    @Override
    public RequestResponse<ItemStatus> cancelOperationProcessExecution(String id)
        throws InternalServerException, VitamClientException {
        ParametersChecker.checkParameter(BLANK_OPERATION_ID, id);
        VitamRequestBuilder request = delete().withPath(OPERATION_URI + "/" + id).withJsonAccept();
        try (Response response = make((request))) {
            checkWithSpecificException(response);
            return RequestResponse.parseFromResponse(response, ItemStatus.class);
        } catch (
            ForbiddenClientException
            | WorkflowNotFoundException
            | BadRequestException
            | ConflictClientException
            | PreconditionFailedClientException e
        ) {
            throw new VitamClientInternalException(e);
        }
    }

    @Override
    public void registerWorker(String familyId, String workerId, WorkerBean workerDescription)
        throws VitamClientInternalException, ProcessingBadRequestException {
        ParametersChecker.checkParameter("familyId is a mandatory parameter", familyId);
        ParametersChecker.checkParameter("workerId is a mandatory parameter", workerId);
        ParametersChecker.checkParameter("workerDescription is a mandatory parameter", workerDescription);

        VitamRequestBuilder request = post()
            .withPath("/worker_family/" + familyId + "/workers/" + workerId)
            .withBody(workerDescription)
            .withJson();
        try (Response response = make(request)) {
            checkWithSpecificException(response);
        } catch (
            final VitamClientInternalException
            | AccessUnauthorizedException
            | ConflictClientException
            | WorkflowNotFoundException
            | PreconditionFailedClientException
            | ForbiddenClientException e
        ) {
            throw new ProcessingBadRequestException(INTERNAL_SERVER_ERROR.getReasonPhrase(), e);
        } catch (BadRequestException e) {
            throw new ProcessingBadRequestException(BAD_REQUEST_EXCEPTION, e);
        } catch (InternalServerException e) {
            throw new VitamClientInternalException(
                "Internal error while trying to register worker : family (" +
                familyId +
                "), workerId (" +
                workerId +
                "",
                e
            );
        }
    }

    @Override
    public void unregisterWorker(String familyId, String workerId) throws ProcessingBadRequestException {
        ParametersChecker.checkParameter("familyId is a mandatory parameter", familyId);
        ParametersChecker.checkParameter("workerId is a mandatory parameter", workerId);
        VitamRequestBuilder request = delete()
            .withPath("/worker_family/" + familyId + "/workers/" + workerId)
            .withJsonAccept();
        try (Response response = make(request)) {
            checkWithSpecificException(response);
        } catch (
            ForbiddenClientException
            | WorkflowNotFoundException
            | BadRequestException
            | ConflictClientException
            | PreconditionFailedClientException
            | AccessUnauthorizedException
            | InternalServerException e
        ) {
            throw new ProcessingBadRequestException(e);
        } catch (final VitamClientInternalException e) {
            throw new ProcessingBadRequestException(INTERNAL_SERVER_ERROR.getReasonPhrase(), e);
        }
    }

    @Override
    public RequestResponse<ProcessDetail> listOperationsDetails(ProcessQuery query) throws VitamClientException {
        VitamRequestBuilder request = get().withPath(OPERATION_URI).withBody(query).withJson();
        try (Response response = make(request)) {
            checkWithSpecificException(response);
            return RequestResponse.parseFromResponse(response, ProcessDetail.class);
        } catch (
            ForbiddenClientException
            | WorkflowNotFoundException
            | BadRequestException
            | ConflictClientException
            | PreconditionFailedClientException
            | InternalServerException e
        ) {
            throw new VitamClientInternalException(e);
        }
    }

    @Override
    public RequestResponse<WorkFlow> getWorkflowDefinitions() throws VitamClientException {
        VitamRequestBuilder request = get().withPath(WORKFLOWS_URI).withJsonAccept();
        try (Response response = make(request)) {
            checkWithSpecificException(response);
            return RequestResponse.parseFromResponse(response, WorkFlow.class);
        } catch (
            ForbiddenClientException
            | WorkflowNotFoundException
            | BadRequestException
            | ConflictClientException
            | PreconditionFailedClientException
            | IllegalStateException
            | InternalServerException e
        ) {
            throw new VitamClientInternalException(e);
        }
    }

    @Override
    public Optional<WorkFlow> getWorkflowDetails(String workflowIdentifier) throws VitamClientException {
        VitamRequestBuilder request = get().withPath(WORKFLOWS_URI + "/" + workflowIdentifier).withJsonAccept();
        try (Response response = make(request)) {
            checkWithSpecificException(response);
            return Optional.of(response.readEntity(WorkFlow.class));
        } catch (
            ForbiddenClientException
            | BadRequestException
            | PreconditionFailedClientException
            | InternalServerException
            | ConflictClientException e
        ) {
            throw new VitamClientInternalException(e);
        } catch (WorkflowNotFoundException e) {
            return Optional.empty();
        }
    }

    @Override
    public RequestResponse<ProcessPause> forcePause(ProcessPause info) throws ProcessingException {
        VitamRequestBuilder request = post().withPath(FORCE_PAUSE_URI).withBody(info).withJson();

        try (Response response = make(request)) {
            checkWithSpecificException(response);
            return RequestResponse.parseFromResponse(response, ProcessPause.class);
        } catch (
            VitamClientInternalException
            | WorkflowNotFoundException
            | BadRequestException
            | AccessUnauthorizedException
            | ForbiddenClientException
            | ConflictClientException
            | PreconditionFailedClientException
            | InternalServerException e
        ) {
            throw new ProcessingException(e);
        }
    }

    @Override
    public RequestResponse<ProcessPause> removeForcePause(ProcessPause info) throws ProcessingException {
        VitamRequestBuilder request = post().withPath(REMOVE_FORCE_PAUSE_URI).withBody(info).withJson();

        try (Response response = make(request)) {
            checkWithSpecificException(response);
            return RequestResponse.parseFromResponse(response, ProcessPause.class);
        } catch (
            VitamClientInternalException
            | WorkflowNotFoundException
            | BadRequestException
            | AccessUnauthorizedException
            | ForbiddenClientException
            | ConflictClientException
            | PreconditionFailedClientException
            | InternalServerException e
        ) {
            throw new ProcessingException(e.getMessage(), e);
        }
    }

    private void checkWithSpecificException(Response response)
        throws WorkflowNotFoundException, BadRequestException, AccessUnauthorizedException, ForbiddenClientException, ConflictClientException, VitamClientInternalException, PreconditionFailedClientException, InternalServerException {
        final Status status = fromStatusCode(response.getStatus());

        if (SUCCESSFUL.equals(status.getFamily())) {
            return;
        }

        switch (status) {
            case NOT_FOUND:
                throw new WorkflowNotFoundException(status.getReasonPhrase());
            case BAD_REQUEST:
                String reason = (response.hasEntity())
                    ? response.readEntity(String.class)
                    : Response.Status.BAD_REQUEST.getReasonPhrase();
                throw new BadRequestException(reason);
            case UNAUTHORIZED:
                throw new AccessUnauthorizedException("Contract not found ");
            case FORBIDDEN:
                reason = (response.hasEntity())
                    ? response.readEntity(String.class)
                    : Response.Status.BAD_REQUEST.getReasonPhrase();
                throw new ForbiddenClientException(reason);
            case PRECONDITION_FAILED:
                throw new PreconditionFailedClientException("Precondition Failed");
            case CONFLICT:
                throw new ConflictClientException(Response.Status.CONFLICT.getReasonPhrase());
            case INTERNAL_SERVER_ERROR:
                throw new InternalServerException(INTERNAL_SERVER_ERROR.getReasonPhrase());
            default:
                throw new VitamClientInternalException(
                    String.format(
                        "Error with the response, get status: '%d' and reason '%s'.",
                        status,
                        status.getReasonPhrase()
                    )
                );
        }
    }
}
