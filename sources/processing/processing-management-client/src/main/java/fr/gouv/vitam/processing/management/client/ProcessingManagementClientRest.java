/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.client.DefaultClient;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InternalServerException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.exception.WorkflowNotFoundException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.ProcessPause;
import fr.gouv.vitam.common.model.ProcessQuery;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.processing.ProcessDetail;
import fr.gouv.vitam.common.model.processing.WorkFlow;
import fr.gouv.vitam.processing.common.ProcessingEntry;
import fr.gouv.vitam.processing.common.exception.ProcessingBadRequestException;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.exception.WorkerAlreadyExistsException;
import fr.gouv.vitam.processing.common.model.WorkerBean;
import fr.gouv.vitam.processing.common.parameter.WorkerParameterName;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Processing Management Client
 */
class ProcessingManagementClientRest extends DefaultClient implements ProcessingManagementClient {

    private static final String ERR_CONTAINER_IS_MANDATORY = "Container is mandatory";
    private static final String ERR_WORKFLOW_IS_MANDATORY = "Workflow is mandatory";
    private static final String PROCESSING_INTERNAL_SERVER_ERROR = "Processing Internal Server Error";
    private static final String INTERNAL_SERVER_ERROR2 = "Internal Server Error";
    private static final String ILLEGAL_ARGUMENT = "Illegal Argument";
    private static final String NOT_FOUND = "Not Found";
    private static final String BAD_REQUEST_EXCEPTION = "Bad Request Exception";

    private static final String OPERATION_URI = "/operations";
    private static final String WORKFLOWS_URI = "/workflows";
    private static final String FORCE_PAUSE_URI = "/forcepause";
    private static final String REMOVE_FORCE_PAUSE_URI = "/removeforcepause";

    private static final String ACTION_ID_MUST_HAVE_A_VALID_VALUE = "Action id must have a valid value";
    private static final String BLANK_OPERATION_ID = "Operation identifier should be filled";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProcessingManagementClientRest.class);
    private static final String OPERATIONS_PATH = "/operations";
    private static final String INTERNAL_SERVER_ERROR_MSG = "Internal Server Error";

    ProcessingManagementClientRest(ProcessingManagementClientFactory factory) {
        super(factory);
    }

    @Override
    public void initVitamProcess(String container, String workflowId) throws BadRequestException, InternalServerException {
        initVitamProcess(new ProcessingEntry(container, workflowId));
    }

    @Override
    public void initVitamProcess( ProcessingEntry entry) throws InternalServerException, BadRequestException {
        Response response = null;
        ParametersChecker.checkParameter("Params cannot be null", entry);
        ParametersChecker.checkParameter(ERR_CONTAINER_IS_MANDATORY, entry.getContainer());
        ParametersChecker.checkParameter(ERR_WORKFLOW_IS_MANDATORY, entry.getWorkflow());
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();

        headers.add(GlobalDataRest.X_CONTEXT_ID, entry.getWorkflow());
        headers.add(GlobalDataRest.X_ACTION, ProcessAction.INIT);
        // add header action id default init
        try {
            response = performRequest(HttpMethod.POST, OPERATION_URI + "/" + entry.getContainer(), headers,
                    entry,
                    MediaType.APPLICATION_JSON_TYPE,
                    MediaType.APPLICATION_JSON_TYPE);
            if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
                throw new WorkflowNotFoundException(NOT_FOUND);
            } else if (response.getStatus() == Status.PRECONDITION_FAILED.getStatusCode()) {
                throw new IllegalArgumentException(ILLEGAL_ARGUMENT);
            } else if (response.getStatus() == Status.BAD_REQUEST.getStatusCode()) {
                throw new BadRequestException(BAD_REQUEST_EXCEPTION);
            } else if (response.getStatus() == Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
                throw new InternalServerException(INTERNAL_SERVER_ERROR);
            }

            // XXX: theoretically OK status case
            // Don't we thrown an exception if it is another status ?
        } catch (final javax.ws.rs.ProcessingException e) {
            LOGGER.debug(e);
            throw new InternalServerException(INTERNAL_SERVER_ERROR2, e);
        } catch (final VitamClientInternalException e) {
            LOGGER.debug(PROCESSING_INTERNAL_SERVER_ERROR, e);
            throw new InternalServerException(INTERNAL_SERVER_ERROR2, e);
        } catch (final BadRequestException e) {
            throw new BadRequestException(BAD_REQUEST_EXCEPTION);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }


    @Override
    public RequestResponse<JsonNode> executeOperationProcess(String operationId, String workflowId, String actionId)
        throws InternalServerException, WorkflowNotFoundException {

        ParametersChecker.checkParameter(BLANK_OPERATION_ID, operationId);
        ParametersChecker.checkParameter(ACTION_ID_MUST_HAVE_A_VALID_VALUE, actionId);
        ParametersChecker.checkParameter("workflow is a mandatory parameter", workflowId);
        Response response = null;
        try {
            response = performRequest(HttpMethod.POST, OPERATION_URI + "/" + operationId,
                getDefaultHeaders(workflowId, actionId),
                JsonHandler.toJsonNode(new ProcessingEntry(operationId, workflowId)), MediaType.APPLICATION_JSON_TYPE,
                MediaType.APPLICATION_JSON_TYPE);
            if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
                throw new WorkflowNotFoundException(NOT_FOUND);
            } else if (response.getStatus() == Status.PRECONDITION_FAILED.getStatusCode()) {
                throw new IllegalArgumentException(ILLEGAL_ARGUMENT);
            } else if (response.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                throw new NotAuthorizedException(ILLEGAL_ARGUMENT);
            }

            // XXX: theoretically OK status case
            // Don't we thrown an exception if it is another status ?
            return new RequestResponseOK<JsonNode>().parseHeadersFromResponse(response);

        } catch (final javax.ws.rs.ProcessingException e) {
            LOGGER.debug(e);
            throw new InternalServerException(INTERNAL_SERVER_ERROR2, e);
        } catch (final VitamClientInternalException e) {
            LOGGER.debug(PROCESSING_INTERNAL_SERVER_ERROR, e);
            throw new InternalServerException(INTERNAL_SERVER_ERROR2, e);
        } catch (final InvalidParseOperationException e) {
            throw new IllegalArgumentException(ILLEGAL_ARGUMENT, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    /**
     * Generate the default header map
     *
     * @param contextId the context id
     * @param actionId  the storage action id
     * @return header map
     */
    private MultivaluedHashMap<String, Object> getDefaultHeaders(String contextId, String actionId) {
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(GlobalDataRest.X_ACTION, actionId);
        headers.add(GlobalDataRest.X_CONTEXT_ID, contextId);
        return headers;
    }

    @Override
    public RequestResponse<ItemStatus> updateOperationActionProcess(String actionId, String operationId)
            throws InternalServerException {
        ParametersChecker.checkParameter(BLANK_OPERATION_ID, operationId);
        ParametersChecker.checkParameter(ACTION_ID_MUST_HAVE_A_VALID_VALUE, actionId);
        Response response = null;
        try {
            response =
                    performRequest(HttpMethod.PUT, OPERATION_URI + "/" + operationId,
                            getDefaultHeaders(null, actionId),
                            null, MediaType.APPLICATION_JSON_TYPE,
                            MediaType.APPLICATION_JSON_TYPE);
            if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
                throw new WorkflowNotFoundException(NOT_FOUND);
            } else if (response.getStatus() == Status.PRECONDITION_FAILED.getStatusCode()) {
                throw new IllegalArgumentException(ILLEGAL_ARGUMENT);
            } else if (response.getStatus() == Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
                throw new InternalServerException(INTERNAL_SERVER_ERROR2);
            } else if (response.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                throw new InternalServerException(INTERNAL_SERVER_ERROR2);
            }

            // XXX: theoretically OK status case
            // Don't we thrown an exception if it is another status ?

            ItemStatus itemStatus = response.readEntity(ItemStatus.class);
            return new RequestResponseOK<ItemStatus>().addResult(itemStatus).parseHeadersFromResponse(response);
        } catch (final javax.ws.rs.ProcessingException e) {
            LOGGER.debug(e);
            throw new InternalServerException(INTERNAL_SERVER_ERROR2, e);
        } catch (final VitamClientInternalException e) {
            LOGGER.debug(PROCESSING_INTERNAL_SERVER_ERROR, e);
            throw new InternalServerException(INTERNAL_SERVER_ERROR2, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public ItemStatus getOperationProcessStatus(String id) throws InternalServerException, BadRequestException {
        ParametersChecker.checkParameter(BLANK_OPERATION_ID, id);
        Response response = null;
        try {
            response =
                    performRequest(HttpMethod.HEAD, OPERATION_URI + "/" + id,
                            null,
                            MediaType.APPLICATION_JSON_TYPE);
            if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
                throw new WorkflowNotFoundException(NOT_FOUND);
            } else if (response.getStatus() == Status.PRECONDITION_FAILED.getStatusCode()) {
                throw new IllegalArgumentException(ILLEGAL_ARGUMENT);
            } else if (response.getStatus() == Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
                throw new InternalServerException(INTERNAL_SERVER_ERROR2);
            } else if (response.getStatus() == Status.BAD_REQUEST.getStatusCode()) {
                throw new BadRequestException(BAD_REQUEST_EXCEPTION);
            } else if (response.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                throw new IllegalArgumentException(Status.UNAUTHORIZED.getReasonPhrase());
            }

            return new ItemStatus()
                    .setGlobalState(ProcessState.valueOf(response.getHeaderString(GlobalDataRest.X_GLOBAL_EXECUTION_STATE)))
                    .setLogbookTypeProcess(response.getHeaderString(GlobalDataRest.X_CONTEXT_ID))
                    .increment(StatusCode.valueOf(response.getHeaderString(GlobalDataRest.X_GLOBAL_EXECUTION_STATUS)));

        } catch (final WorkflowNotFoundException e) {
            LOGGER.debug(e);
            throw new WorkflowNotFoundException(NOT_FOUND, e);
        } catch (final javax.ws.rs.ProcessingException e) {
            LOGGER.debug(e);
            throw new InternalServerException(INTERNAL_SERVER_ERROR2, e);
        } catch (final VitamClientInternalException e) {
            LOGGER.debug(PROCESSING_INTERNAL_SERVER_ERROR, e);
            throw new InternalServerException(INTERNAL_SERVER_ERROR2, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    /**
     * Return false if status accepted Return true otherwise
     *
     * @param operationId
     * @return
     */
    @Override
    public boolean isOperationCompleted(String operationId) {
        ParametersChecker.checkParameter(BLANK_OPERATION_ID, operationId);
        Response response = null;
        try {
            response =
                    performRequest(HttpMethod.HEAD, OPERATION_URI + "/" + operationId,
                            null,
                            MediaType.APPLICATION_JSON_TYPE);

            if (response.getStatus() == Status.ACCEPTED.getStatusCode()) {
                final ProcessState state =
                        ProcessState.valueOf(response.getHeaderString(GlobalDataRest.X_GLOBAL_EXECUTION_STATE));
                final StatusCode status =
                        StatusCode.valueOf(response.getHeaderString(GlobalDataRest.X_GLOBAL_EXECUTION_STATUS));

                if (ProcessState.PAUSE.equals(state) && StatusCode.STARTED.compareTo(status) <= 0) {
                    return true;
                } else {
                    return false;
                }
            } else {
                return true;
            }
        } catch (final Exception e) {
            return true;
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public ItemStatus getOperationProcessExecutionDetails(String id)
            throws InternalServerException, BadRequestException {
        ParametersChecker.checkParameter(BLANK_OPERATION_ID, id);
        Response response = null;
        try {
            response =
                    performRequest(HttpMethod.GET, OPERATION_URI + "/" + id,
                            null, MediaType.APPLICATION_JSON_TYPE);
            if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
                throw new WorkflowNotFoundException(NOT_FOUND);
            } else if (response.getStatus() == Status.PRECONDITION_FAILED.getStatusCode()) {
                throw new IllegalArgumentException(ILLEGAL_ARGUMENT);
            } else if (response.getStatus() == Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
                throw new InternalServerException(INTERNAL_SERVER_ERROR2);
            } else if (response.getStatus() == Status.BAD_REQUEST.getStatusCode()) {
                throw new BadRequestException(BAD_REQUEST_EXCEPTION);
            } else if (response.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                throw new IllegalArgumentException(Status.UNAUTHORIZED.getReasonPhrase());
            }

            // XXX: theoretically OK status case
            // Don't we thrown an exception if it is another status ?
            return response.readEntity(ItemStatus.class);
        } catch (final WorkflowNotFoundException e) {
            LOGGER.debug(e);
            throw new WorkflowNotFoundException(NOT_FOUND, e);
        } catch (final javax.ws.rs.ProcessingException e) {
            LOGGER.debug(e);
            throw new InternalServerException(INTERNAL_SERVER_ERROR2, e);
        } catch (final VitamClientInternalException e) {
            LOGGER.debug(PROCESSING_INTERNAL_SERVER_ERROR, e);
            throw new InternalServerException(INTERNAL_SERVER_ERROR2, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public ItemStatus cancelOperationProcessExecution(String id)
            throws InternalServerException {
        ParametersChecker.checkParameter(BLANK_OPERATION_ID, id);
        Response response = null;
        try {
            response =
                    performRequest(HttpMethod.DELETE, OPERATION_URI + "/" + id,
                            null, MediaType.APPLICATION_JSON_TYPE);
            if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
                throw new WorkflowNotFoundException(NOT_FOUND);
            } else if (response.getStatus() == Status.PRECONDITION_FAILED.getStatusCode()) {
                throw new IllegalArgumentException(ILLEGAL_ARGUMENT);
            } else if (response.getStatus() == Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
                throw new InternalServerException(INTERNAL_SERVER_ERROR2);
            } else if (response.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                throw new InternalServerException(INTERNAL_SERVER_ERROR2);
            }

            // XXX: theoretically OK status case
            // Don't we thrown an exception if it is another status ?
            return response.readEntity(ItemStatus.class);
        } catch (final javax.ws.rs.ProcessingException e) {
            LOGGER.debug(e);
            throw new InternalServerException(INTERNAL_SERVER_ERROR2, e);
        } catch (final VitamClientInternalException e) {
            LOGGER.debug(PROCESSING_INTERNAL_SERVER_ERROR, e);
            throw new InternalServerException(INTERNAL_SERVER_ERROR2, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public void registerWorker(String familyId, String workerId, WorkerBean workerDescription)
            throws ProcessingBadRequestException, WorkerAlreadyExistsException {
        ParametersChecker.checkParameter("familyId is a mandatory parameter", familyId);
        ParametersChecker.checkParameter("workerId is a mandatory parameter", workerId);
        ParametersChecker.checkParameter("workerDescription is a mandatory parameter", workerDescription);
        Response response = null;
        try {
            response =
                    performRequest(HttpMethod.POST, "/worker_family/" + familyId + "/" + "workers" + "/" + workerId, null,
                            JsonHandler.toJsonNode(workerDescription), MediaType.APPLICATION_JSON_TYPE,
                            MediaType.APPLICATION_JSON_TYPE);

            if (response.getStatus() == Status.BAD_REQUEST.getStatusCode()) {
                throw new ProcessingBadRequestException("Bad Request");
            } else if (response.getStatus() == Status.CONFLICT.getStatusCode()) {
                throw new WorkerAlreadyExistsException("Worker already exist");
            }
        } catch (final VitamClientInternalException e) {
            LOGGER.debug(PROCESSING_INTERNAL_SERVER_ERROR, e);
            throw new ProcessingBadRequestException(INTERNAL_SERVER_ERROR2, e);
        } catch (final InvalidParseOperationException e) {
            throw new IllegalArgumentException(ILLEGAL_ARGUMENT, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public void unregisterWorker(String familyId, String workerId) throws ProcessingBadRequestException {

        ParametersChecker.checkParameter("familyId is a mandatory parameter", familyId);
        ParametersChecker.checkParameter("workerId is a mandatory parameter", workerId);
        Response response = null;
        try {
            response =
                    performRequest(HttpMethod.DELETE, "/worker_family/" + familyId + "/" + "workers" +
                            "/" + workerId, null, MediaType.APPLICATION_JSON_TYPE);

            if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
                throw new ProcessingBadRequestException("Worker Family, or worker does not exist");
            }
        } catch (final VitamClientInternalException e) {
            LOGGER.debug(PROCESSING_INTERNAL_SERVER_ERROR, e);
            throw new ProcessingBadRequestException(INTERNAL_SERVER_ERROR2, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }

    }

    @Override
    public RequestResponse<ProcessDetail> listOperationsDetails(ProcessQuery query) throws VitamClientException {
        Response response = null;
        try {
            response =
                    performRequest(HttpMethod.GET, OPERATIONS_PATH, null, JsonHandler.toJsonNode(query),
                            MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);
            return RequestResponse.parseFromResponse(response, ProcessDetail.class);

        } catch (VitamClientInternalException e) {
            LOGGER.debug("VitamClientInternalException: ", e);
            throw new VitamClientException(e);
        } catch (final InvalidParseOperationException e) {
            throw new IllegalArgumentException(ILLEGAL_ARGUMENT, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public Response executeCheckTraceabilityWorkFlow(String checkOperationId, JsonNode query, String workflowId, String actionId)
            throws InternalServerException, WorkflowNotFoundException {

        ParametersChecker.checkParameter(BLANK_OPERATION_ID, checkOperationId);
        ParametersChecker.checkParameter(ACTION_ID_MUST_HAVE_A_VALID_VALUE, actionId);
        ParametersChecker.checkParameter("workflow is a mandatory parameter", workflowId);


        Response response = null;
        try {
            // Add extra parameters to start correctly the check process
            Map<String, String> checkExtraParams = new HashMap<>();
            checkExtraParams.put(WorkerParameterName.logbookRequest.toString(), JsonHandler.unprettyPrint(query));
            ProcessingEntry processingEntry = new ProcessingEntry(checkOperationId, workflowId);
            processingEntry.setExtraParams(checkExtraParams);

            response = performRequest(HttpMethod.POST, OPERATION_URI + "/" + checkOperationId,
                getDefaultHeaders(workflowId, actionId), processingEntry, MediaType.APPLICATION_JSON_TYPE,
                MediaType.APPLICATION_JSON_TYPE);

            if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
                throw new WorkflowNotFoundException(NOT_FOUND);
            } else if (response.getStatus() == Status.PRECONDITION_FAILED.getStatusCode()) {
                throw new IllegalArgumentException(ILLEGAL_ARGUMENT);
            } else if (response.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                throw new NotAuthorizedException(ILLEGAL_ARGUMENT);
            } else if (response.getStatus() == Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
                throw new InternalServerException(INTERNAL_SERVER_ERROR2);
            }

            // Return the created verification logbookOperation
            return Response.fromResponse(response).build();
        } catch (final javax.ws.rs.ProcessingException e) {
            LOGGER.debug(e);
            throw new InternalServerException(INTERNAL_SERVER_ERROR2, e);
        } catch (final VitamClientInternalException e) {
            LOGGER.debug(PROCESSING_INTERNAL_SERVER_ERROR, e);
            throw new InternalServerException(INTERNAL_SERVER_ERROR2, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<WorkFlow> getWorkflowDefinitions() throws VitamClientException {
        Response response = null;
        try {

            response = performRequest(HttpMethod.GET, WORKFLOWS_URI, null, null, null, MediaType.APPLICATION_JSON_TYPE);
            return RequestResponse.parseFromResponse(response, WorkFlow.class);

        } catch (IllegalStateException e) {
            LOGGER.debug("Could not parse server response ", e);
            throw createExceptionFromResponse(response);
        } catch (VitamClientInternalException e) {
            LOGGER.debug("VitamClientInternalException: ", e);
            throw new VitamClientException(e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public Optional<WorkFlow> getWorkflowDetails(String workflowIdentifier) throws VitamClientException {
        Response response = null;
        try {
            response = performRequest(HttpMethod.GET, WORKFLOWS_URI + "/" + workflowIdentifier, null, null, null, MediaType.APPLICATION_JSON_TYPE);

            if (response.getStatus() == Status.OK.getStatusCode()) {
                return Optional.of(response.readEntity(WorkFlow.class));
            } else if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
                return Optional.empty();
            } else {
                throw new VitamClientException("Internal Error Server : " + response.readEntity(String.class));
            }

        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<ProcessPause> forcePause(ProcessPause info) throws ProcessingException {
        ParametersChecker.checkParameter("The input ProcessPause json is mandatory", info);
        Response response = null;
        try {
            response = performRequest(HttpMethod.POST, FORCE_PAUSE_URI, null, info,
                    MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);

            return RequestResponse.parseFromResponse(response, ProcessPause.class);

        } catch (VitamClientInternalException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            throw new ProcessingException(INTERNAL_SERVER_ERROR_MSG, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<ProcessPause> removeForcePause(ProcessPause info) throws ProcessingException {
        ParametersChecker.checkParameter("The input ProcessPause json is mandatory", info);
        Response response = null;
        try {
            response = performRequest(HttpMethod.POST, REMOVE_FORCE_PAUSE_URI, null, info,
                    MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);

            return RequestResponse.parseFromResponse(response, ProcessPause.class);

        } catch (VitamClientInternalException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            throw new ProcessingException(INTERNAL_SERVER_ERROR_MSG, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }
}
