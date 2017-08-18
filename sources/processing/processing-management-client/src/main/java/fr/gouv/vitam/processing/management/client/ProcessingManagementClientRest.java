/*******************************************************************************
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
 *******************************************************************************/
package fr.gouv.vitam.processing.management.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

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
import fr.gouv.vitam.common.model.ProcessQuery;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.processing.common.ProcessingEntry;
import fr.gouv.vitam.processing.common.exception.ProcessingBadRequestException;
import fr.gouv.vitam.processing.common.exception.WorkerAlreadyExistsException;
import fr.gouv.vitam.processing.common.model.WorkerBean;
import fr.gouv.vitam.processing.common.parameter.WorkerParameterName;

/**
 * Processing Management Client
 */
class ProcessingManagementClientRest extends DefaultClient implements ProcessingManagementClient {

    private static final String ERR_CONTAINER_IS_MANDATORY = "Container is mandatory";
    private static final String ERR_WORKFLOW_IS_MANDATORY = "Workflow is mandatory";
    private static final String PROCESSING_INTERNAL_SERVER_ERROR = "Processing Internal Server Error";
    private static final String INTERNAL_SERVER_ERROR2 = "Internal Server Error";
    private static final String ILLEGAL_ARGUMENT = "Illegal Argument";
    private static final String WORKFLOW_NOT_FOUND = "Workflow Not Found";
    private static final String BAD_REQUEST_EXCEPTION = "Bad Request Exception";

    private static final String OPERATION_URI = "/operations";
    private static final String WORKFLOWS_URI = "/workflows";
    private static final String INGESTS_URI = "/ingests";
    private static final String OPERATION_ID_URI = "/id";
    private static final String CONTEXT_ID_MUST_HAVE_A_VALID_VALUE = "Context id must have a valid value";
    private static final String ACTION_ID_MUST_HAVE_A_VALID_VALUE = "Action id must have a valid value";
    private static final String BLANK_OPERATION_ID = "Operation identifier should be filled";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProcessingManagementClientRest.class);
    private static final String ERR_ACTION_IS_MANDATORY = "The action is mandatory";

    /**
     * Constructor
     *
     * @param factory
     */
    ProcessingManagementClientRest(ProcessingManagementClientFactory factory) {
        super(factory);
    }

    @Override
    public void initVitamProcess(String contextId, String container, String workflow)
        throws InternalServerException, BadRequestException {
        initVitamProcess(contextId, new ProcessingEntry(container, workflow));
    }

    @Override
    public void initVitamProcess(String contextId, ProcessingEntry entry)
        throws InternalServerException, BadRequestException {
        Response response = null;
        ParametersChecker.checkParameter("Params cannot be null", contextId);
        ParametersChecker.checkParameter(ERR_CONTAINER_IS_MANDATORY, entry.getContainer());
        ParametersChecker.checkParameter(ERR_WORKFLOW_IS_MANDATORY, entry.getWorkflow());
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();

        headers.add(GlobalDataRest.X_CONTEXT_ID, contextId);
        headers.add(GlobalDataRest.X_ACTION, ProcessAction.INIT);
        // add header action id default init
        try {
            response = performRequest(HttpMethod.POST, OPERATION_URI + "/" + entry.getContainer(), headers,
                entry,
                MediaType.APPLICATION_JSON_TYPE,
                MediaType.APPLICATION_JSON_TYPE);
            if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
                throw new WorkflowNotFoundException(WORKFLOW_NOT_FOUND);
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
            LOGGER.error(e);
            throw new InternalServerException(INTERNAL_SERVER_ERROR2, e);
        } catch (final VitamClientInternalException e) {
            LOGGER.error(PROCESSING_INTERNAL_SERVER_ERROR, e);
            throw new InternalServerException(INTERNAL_SERVER_ERROR2, e);
        } catch (final BadRequestException e) {
            throw new BadRequestException(BAD_REQUEST_EXCEPTION);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public ItemStatus updateVitamProcess(String contextId, String actionId, String container, String workflow)
        throws InternalServerException, BadRequestException {
        ParametersChecker.checkParameter(CONTEXT_ID_MUST_HAVE_A_VALID_VALUE, contextId);
        ParametersChecker.checkParameter(ACTION_ID_MUST_HAVE_A_VALID_VALUE, actionId);
        ParametersChecker.checkParameter("container is a mandatory parameter", container);
        ParametersChecker.checkParameter("workflow is a mandatory parameter", workflow);
        Response response = null;
        try {
            response =
                performRequest(HttpMethod.PUT, OPERATION_URI, getDefaultHeaders(contextId, actionId),
                    JsonHandler.toJsonNode(new ProcessingEntry(container, workflow)), MediaType.APPLICATION_JSON_TYPE,
                    MediaType.APPLICATION_JSON_TYPE);
            if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
                throw new WorkflowNotFoundException(WORKFLOW_NOT_FOUND);
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
        } catch (final javax.ws.rs.ProcessingException e) {
            LOGGER.error(e);
            throw new InternalServerException(INTERNAL_SERVER_ERROR2, e);
        } catch (final VitamClientInternalException e) {
            LOGGER.error(PROCESSING_INTERNAL_SERVER_ERROR, e);
            throw new InternalServerException(INTERNAL_SERVER_ERROR2, e);
        } catch (final InvalidParseOperationException e) {
            throw new IllegalArgumentException(ILLEGAL_ARGUMENT, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<JsonNode> executeOperationProcess(String operationId, String workflow, String contextId,
        String actionId)
        throws InternalServerException, BadRequestException, WorkflowNotFoundException {

        ParametersChecker.checkParameter(BLANK_OPERATION_ID, operationId);
        ParametersChecker.checkParameter(CONTEXT_ID_MUST_HAVE_A_VALID_VALUE, contextId);
        ParametersChecker.checkParameter(ACTION_ID_MUST_HAVE_A_VALID_VALUE, actionId);
        ParametersChecker.checkParameter("workflow is a mandatory parameter", workflow);
        Response response = null;
        try {
            response =
                performRequest(HttpMethod.POST, OPERATION_URI + "/" + operationId,
                    getDefaultHeaders(contextId, actionId),
                    JsonHandler.toJsonNode(new ProcessingEntry(operationId, workflow)), MediaType.APPLICATION_JSON_TYPE,
                    MediaType.APPLICATION_JSON_TYPE);
            if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
                throw new WorkflowNotFoundException(WORKFLOW_NOT_FOUND);
            } else if (response.getStatus() == Status.PRECONDITION_FAILED.getStatusCode()) {
                throw new IllegalArgumentException(ILLEGAL_ARGUMENT);
            } else if (response.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                throw new NotAuthorizedException(ILLEGAL_ARGUMENT);
            }

            // XXX: theoretically OK status case
            // Don't we thrown an exception if it is another status ?
            return new RequestResponseOK<JsonNode>().parseHeadersFromResponse(response);

        } catch (final javax.ws.rs.ProcessingException e) {
            LOGGER.error(e);
            throw new InternalServerException(INTERNAL_SERVER_ERROR2, e);
        } catch (final VitamClientInternalException e) {
            LOGGER.error(PROCESSING_INTERNAL_SERVER_ERROR, e);
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
        throws InternalServerException, BadRequestException {
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
                throw new WorkflowNotFoundException(WORKFLOW_NOT_FOUND);
            } else if (response.getStatus() == Status.PRECONDITION_FAILED.getStatusCode()) {
                throw new IllegalArgumentException(ILLEGAL_ARGUMENT);
            } else if (response.getStatus() == Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
                throw new InternalServerException(INTERNAL_SERVER_ERROR2);
            }else if (response.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                throw new InternalServerException(INTERNAL_SERVER_ERROR2);
            }

            // XXX: theoretically OK status case
            // Don't we thrown an exception if it is another status ?

            ItemStatus itemStatus = response.readEntity(ItemStatus.class);
            return new RequestResponseOK<ItemStatus>().addResult(itemStatus).parseHeadersFromResponse(response);
        } catch (final javax.ws.rs.ProcessingException e) {
            LOGGER.error(e);
            throw new InternalServerException(INTERNAL_SERVER_ERROR2, e);
        } catch (final VitamClientInternalException e) {
            LOGGER.error(PROCESSING_INTERNAL_SERVER_ERROR, e);
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
                throw new WorkflowNotFoundException(WORKFLOW_NOT_FOUND);
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
            LOGGER.error(e);
            throw new WorkflowNotFoundException(WORKFLOW_NOT_FOUND, e);
        } catch (final javax.ws.rs.ProcessingException e) {
            LOGGER.error(e);
            throw new InternalServerException(INTERNAL_SERVER_ERROR2, e);
        } catch (final VitamClientInternalException e) {
            LOGGER.error(PROCESSING_INTERNAL_SERVER_ERROR, e);
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

    // TODO FIXE ME query never user
    @Override
    public ItemStatus getOperationProcessExecutionDetails(String id, JsonNode query)
        throws InternalServerException, BadRequestException {
        ParametersChecker.checkParameter(BLANK_OPERATION_ID, id);
        Response response = null;
        try {
            response =
                performRequest(HttpMethod.GET, OPERATION_URI + "/" + id,
                    null, query,
                    MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);
            if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
                throw new WorkflowNotFoundException(WORKFLOW_NOT_FOUND);
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
            LOGGER.error(e);
            throw new WorkflowNotFoundException(WORKFLOW_NOT_FOUND, e);
        } catch (final javax.ws.rs.ProcessingException e) {
            LOGGER.error(e);
            throw new InternalServerException(INTERNAL_SERVER_ERROR2, e);
        } catch (final VitamClientInternalException e) {
            LOGGER.error(PROCESSING_INTERNAL_SERVER_ERROR, e);
            throw new InternalServerException(INTERNAL_SERVER_ERROR2, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public ItemStatus cancelOperationProcessExecution(String id)
        throws InternalServerException, BadRequestException {
        ParametersChecker.checkParameter(BLANK_OPERATION_ID, id);
        Response response = null;
        try {
            response =
                performRequest(HttpMethod.DELETE, OPERATION_URI + "/" + id,
                    null, MediaType.APPLICATION_JSON_TYPE);
            if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
                throw new WorkflowNotFoundException(WORKFLOW_NOT_FOUND);
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
            LOGGER.error(e);
            throw new InternalServerException(INTERNAL_SERVER_ERROR2, e);
        } catch (final VitamClientInternalException e) {
            LOGGER.error(PROCESSING_INTERNAL_SERVER_ERROR, e);
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
            LOGGER.error(PROCESSING_INTERNAL_SERVER_ERROR, e);
            throw new ProcessingBadRequestException(INTERNAL_SERVER_ERROR2, e);
        } catch (final InvalidParseOperationException e) {
            throw new IllegalArgumentException(ILLEGAL_ARGUMENT, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public void unregisterWorker(String familyId, String workerId)
        throws ProcessingBadRequestException {
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
            LOGGER.error(PROCESSING_INTERNAL_SERVER_ERROR, e);
            throw new ProcessingBadRequestException(INTERNAL_SERVER_ERROR2, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }

    }

    @Override
    @Deprecated
    public void initWorkFlow(String contextId) throws InternalServerException, BadRequestException {
        Response response = null;
        ParametersChecker.checkParameter("Params cannot be null", contextId);
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(GlobalDataRest.X_CONTEXT_ID, contextId);
        headers.add(GlobalDataRest.X_ACTION, ProcessAction.INIT);
        // add header action id default init
        try {
            response = performRequest(HttpMethod.POST, OPERATION_URI, headers,
                MediaType.APPLICATION_JSON_TYPE);
            if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
                throw new WorkflowNotFoundException(WORKFLOW_NOT_FOUND);
            } else if (response.getStatus() == Status.PRECONDITION_FAILED.getStatusCode()) {
                throw new IllegalArgumentException(ILLEGAL_ARGUMENT);
            } else if (response.getStatus() == Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
                throw new InternalServerException(INTERNAL_SERVER_ERROR2);
            } else if (response.getStatus() == Status.BAD_REQUEST.getStatusCode()) {
                throw new BadRequestException(BAD_REQUEST_EXCEPTION);
            }

            // XXX: theoretically OK status case
            // Don't we thrown an exception if it is another status ?
        } catch (final javax.ws.rs.ProcessingException e) {
            LOGGER.error(e);
            throw new InternalServerException(INTERNAL_SERVER_ERROR2, e);
        } catch (final VitamClientInternalException e) {
            LOGGER.error(PROCESSING_INTERNAL_SERVER_ERROR, e);
            throw new InternalServerException(INTERNAL_SERVER_ERROR2, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<JsonNode> listOperationsDetails(ProcessQuery query) throws VitamClientException {
        Response response = null;
        try {
            response =
                performRequest(HttpMethod.GET, "/operations", null, JsonHandler.toJsonNode(query),
                    MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);

            if (response.getStatus() == Status.PRECONDITION_FAILED.getStatusCode()) {
                throw new VitamClientException(ILLEGAL_ARGUMENT);
            }

            List<JsonNode> list =
                JsonHandler.getFromString(response.readEntity(String.class), List.class, JsonNode.class);

            return new RequestResponseOK<JsonNode>().addAllResults(list).parseHeadersFromResponse(response);
        } catch (VitamClientInternalException e) {
            LOGGER.error("VitamClientInternalException: ", e);
            throw new VitamClientException(e);
        } catch (final InvalidParseOperationException e) {
            throw new IllegalArgumentException(ILLEGAL_ARGUMENT, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Response executeCheckTraceabilityWorkFlow(String checkOperationId,
        JsonNode query, String workflow, String contextId, String actionId)
        throws InternalServerException, BadRequestException, WorkflowNotFoundException {

        ParametersChecker.checkParameter(BLANK_OPERATION_ID, checkOperationId);
        ParametersChecker.checkParameter(CONTEXT_ID_MUST_HAVE_A_VALID_VALUE, contextId);
        ParametersChecker.checkParameter(ACTION_ID_MUST_HAVE_A_VALID_VALUE, actionId);
        ParametersChecker.checkParameter("workflow is a mandatory parameter", workflow);


        Response response = null;
        try {
            // Add extra parameters to start correctly the check process
            Map<String, String> checkExtraParams = new HashMap<>();
            checkExtraParams.put(WorkerParameterName.logbookRequest.toString(), JsonHandler.unprettyPrint(query));
            ProcessingEntry processingEntry = new ProcessingEntry(checkOperationId, workflow);
            processingEntry.setExtraParams(checkExtraParams);

            response =
                performRequest(HttpMethod.POST, OPERATION_URI + "/" + checkOperationId,
                    getDefaultHeaders(contextId, actionId), processingEntry, MediaType.APPLICATION_JSON_TYPE,
                    MediaType.APPLICATION_JSON_TYPE);

            if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
                throw new WorkflowNotFoundException(WORKFLOW_NOT_FOUND);
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
            LOGGER.error(e);
            throw new InternalServerException(INTERNAL_SERVER_ERROR2, e);
        } catch (final VitamClientInternalException e) {
            LOGGER.error(PROCESSING_INTERNAL_SERVER_ERROR, e);
            throw new InternalServerException(INTERNAL_SERVER_ERROR2, e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<JsonNode> getWorkflowDefinitions() throws VitamClientException {

        Response response = null;
        try {
            response = performRequest(HttpMethod.GET, WORKFLOWS_URI, null, null, null,
                MediaType.APPLICATION_JSON_TYPE);

            if (response.getStatus() == Status.PRECONDITION_FAILED.getStatusCode()) {
                throw new VitamClientException(ILLEGAL_ARGUMENT);
            } else if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
                throw new VitamClientException(WORKFLOW_NOT_FOUND);
            } else if (response.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                throw new VitamClientException(ILLEGAL_ARGUMENT);
            } else if (response.getStatus() == Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
                throw new VitamClientException(INTERNAL_SERVER_ERROR2);
            }

            JsonNode workFlow =
                JsonHandler.getFromString(response.readEntity(String.class), JsonNode.class);

            return new RequestResponseOK<JsonNode>().addResult(workFlow)
                .parseHeadersFromResponse(response);
        } catch (VitamClientInternalException | InvalidParseOperationException e) {
            LOGGER.error("VitamClientInternalException: ", e);
            throw new VitamClientException(e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

}
