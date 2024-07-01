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
import fr.gouv.vitam.common.client.DefaultClient;
import fr.gouv.vitam.common.client.IngestCollection;
import fr.gouv.vitam.common.client.VitamRequestBuilder;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.NotAcceptableClientException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.exception.WorkflowNotFoundException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.ProcessQuery;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.processing.ProcessDetail;
import fr.gouv.vitam.common.model.processing.WorkFlow;
import fr.gouv.vitam.ingest.internal.common.exception.IllegalZipFileNameException;
import fr.gouv.vitam.ingest.internal.common.exception.IngestInternalClientConflictException;
import fr.gouv.vitam.ingest.internal.common.exception.IngestInternalClientNotFoundException;
import fr.gouv.vitam.ingest.internal.common.exception.IngestInternalClientServerException;
import fr.gouv.vitam.ingest.internal.common.exception.IngestInternalServerUnavailableClientException;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.InputStream;
import java.util.Optional;

import static fr.gouv.vitam.common.client.VitamRequestBuilder.delete;
import static fr.gouv.vitam.common.client.VitamRequestBuilder.get;
import static fr.gouv.vitam.common.client.VitamRequestBuilder.head;
import static fr.gouv.vitam.common.client.VitamRequestBuilder.post;
import static fr.gouv.vitam.common.client.VitamRequestBuilder.put;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;

/**
 * Rest client implementation for Ingest Internal
 */
class IngestInternalClientRest extends DefaultClient implements IngestInternalClient {

    private static final String PROCESS_WORKFLOW_NOT_FOUND_FOR_OPERATION = "Process Workflow not found for operation :";
    private static final String REQUEST_PRECONDITION_FAILED = "Request precondition failed";
    private static final String REQUEST_CONFLICT = "Request conflict";
    private static final String INVALID_PARSE_OPERATION = "Invalid Parse Operation";
    private static final String NOT_FOUND_EXCEPTION = "Not Found Exception";
    private static final String UNAUTHORIZED = "Unauthorized";
    private static final String SERVICE_UNAVAILABLE_EXCEPTION = "Workspace Server Error";
    private static final String NOT_ACCEPTABLE_EXCEPTION = "File or folder name is not allowed";
    private static final String INTERNAL_SERVER_ERROR = "Internal Server Error";

    private static final String LOGBOOK_URL = "/logbooks";
    private static final String INGEST_URL = "/ingests";
    private static final String BLANK_OBJECT_ID = "object identifier should be filled";
    private static final String BLANK_TYPE = "Type should be filled";

    private static final String BLANK_OPERATION_ID = "Operation identifier should be filled";
    private static final String OPERATION_URI = "/operations";
    private static final String WORKFLOWS_URI = "/workflows";

    /**
     * Constructor
     *
     * @param factory
     */
    IngestInternalClientRest(IngestInternalClientFactory factory) {
        super(factory);
    }

    @Override
    public void uploadInitialLogbook(Iterable<LogbookOperationParameters> logbookParametersList) throws VitamException {
        try (
            Response response = make(
                post().withPath(LOGBOOK_URL).withBody(logbookParametersList, "check Upload Parameter").withJson()
            )
        ) {
            check(response);
        }
    }

    @Override
    public void upload(InputStream inputStream, MediaType archiveMimeType, WorkFlow workflow, String actionAfterInit)
        throws VitamException {
        ParametersChecker.checkParameter("context Id Request must not be null", workflow);
        VitamRequestBuilder request = post()
            .withPath(INGEST_URL)
            .withHeader(GlobalDataRest.X_CONTEXT_ID, workflow.getIdentifier())
            .withHeader(GlobalDataRest.X_TYPE_PROCESS, workflow.getTypeProc())
            .withHeader(GlobalDataRest.X_ACTION, actionAfterInit)
            .withHeader(GlobalDataRest.X_ACTION_INIT, ProcessAction.START)
            .withBody(inputStream, "Body cannot be null")
            .withContentType(archiveMimeType)
            .withOctetAccept();
        try (Response response = make(request)) {
            check(response);
        } catch (NotAcceptableClientException e) {
            throw new IllegalZipFileNameException("File or folder name is not allowed", e);
        } catch (IngestInternalClientServerException | IngestInternalServerUnavailableClientException e) {
            throw new IngestInternalClientServerException(e);
        } catch (VitamClientException e) {
            throw new VitamException(e);
        }
    }

    @Override
    public void initWorkflow(WorkFlow workFlow) throws VitamException {
        ParametersChecker.checkParameter("Params cannot be null", workFlow);
        VitamRequestBuilder request = post()
            .withPath(INGEST_URL)
            .withHeader(GlobalDataRest.X_CONTEXT_ID, workFlow.getId())
            .withHeader(GlobalDataRest.X_TYPE_PROCESS, workFlow.getTypeProc())
            .withHeader(GlobalDataRest.X_ACTION, ProcessAction.INIT)
            .withHeader(GlobalDataRest.X_ACTION_INIT, ProcessAction.INIT)
            .withOctetAccept();
        try (Response response = make(request)) {
            check(response);
        } catch (IngestInternalClientServerException | IngestInternalClientNotFoundException e) {
            throw new VitamClientException(e);
        }
    }

    @Override
    public Response downloadObjectAsync(String objectId, IngestCollection type)
        throws InvalidParseOperationException, IngestInternalClientServerException, IngestInternalClientNotFoundException {
        ParametersChecker.checkParameter(BLANK_OBJECT_ID, objectId);
        ParametersChecker.checkParameter(BLANK_TYPE, type);

        Response response = null;
        try {
            response = make(
                get().withPath(INGEST_URL + "/" + objectId + "/" + type.getCollectionName()).withOctetAccept()
            );
            check(response);
            return response;
        } catch (
            VitamClientException | NotAcceptableClientException | IngestInternalServerUnavailableClientException e
        ) {
            throw new IngestInternalClientServerException(e);
        } catch (IngestInternalClientNotFoundException e) {
            throw new IngestInternalClientNotFoundException(e);
        } finally {
            if (response != null && SUCCESSFUL != response.getStatusInfo().getFamily()) {
                response.close();
            }
        }
    }

    @Override
    public RequestResponse<ItemStatus> updateOperationActionProcess(String actionId, String operationId)
        throws VitamClientException {
        ParametersChecker.checkParameter(BLANK_OPERATION_ID, operationId);

        try (
            Response response = make(
                put()
                    .withPath(OPERATION_URI + "/" + operationId)
                    .withHeader(GlobalDataRest.X_ACTION, actionId)
                    .withJsonAccept()
            )
        ) {
            check(response);
            return RequestResponse.parseFromResponse(response, ItemStatus.class);
        } catch (
            InvalidParseOperationException
            | NotAcceptableClientException
            | IngestInternalClientServerException
            | IngestInternalServerUnavailableClientException
            | IngestInternalClientNotFoundException e
        ) {
            throw new VitamClientException(e);
        }
    }

    @Override
    public ItemStatus getOperationProcessStatus(String id) throws VitamClientException {
        ParametersChecker.checkParameter(BLANK_OPERATION_ID, id);

        try (Response response = make(head().withPath(OPERATION_URI + "/" + id).withJsonAccept())) {
            check(response);
            return new ItemStatus()
                .setGlobalState(ProcessState.valueOf(response.getHeaderString(GlobalDataRest.X_GLOBAL_EXECUTION_STATE)))
                .setLogbookTypeProcess(response.getHeaderString(GlobalDataRest.X_CONTEXT_ID))
                .increment(StatusCode.valueOf(response.getHeaderString(GlobalDataRest.X_GLOBAL_EXECUTION_STATUS)));
        } catch (
            InvalidParseOperationException
            | NotAcceptableClientException
            | IngestInternalServerUnavailableClientException e
        ) {
            throw new VitamClientException(e);
        } catch (
            VitamClientInternalException
            | IngestInternalClientNotFoundException
            | WorkflowNotFoundException
            | IngestInternalClientServerException e
        ) {
            throw new VitamClientInternalException(e);
        }
    }

    @Override
    public RequestResponse<ItemStatus> getOperationProcessExecutionDetails(String id) throws VitamClientException {
        ParametersChecker.checkParameter(BLANK_OPERATION_ID, id);
        try (Response response = make(get().withPath(OPERATION_URI + "/" + id).withJsonAccept())) {
            check(response);
            return RequestResponse.parseFromResponse(response, ItemStatus.class);
        } catch (
            InvalidParseOperationException
            | NotAcceptableClientException
            | IngestInternalServerUnavailableClientException e
        ) {
            throw new VitamClientException(e);
        } catch (
            VitamClientInternalException | IngestInternalClientServerException | IngestInternalClientNotFoundException e
        ) {
            throw new VitamClientInternalException(e);
        }
    }

    @Override
    public RequestResponse<ItemStatus> cancelOperationProcessExecution(String id) throws VitamClientException {
        ParametersChecker.checkParameter(BLANK_OPERATION_ID, id);
        try (Response response = make(delete().withPath(OPERATION_URI + "/" + id).withJsonAccept())) {
            check(response);
            return RequestResponse.parseFromResponse(response, ItemStatus.class);
        } catch (
            InvalidParseOperationException
            | NotAcceptableClientException
            | IngestInternalServerUnavailableClientException e
        ) {
            throw new VitamClientException(e);
        } catch (
            VitamClientInternalException
            | IngestInternalClientServerException
            | IngestInternalClientNotFoundException
            | IngestInternalClientConflictException e
        ) {
            throw new VitamClientInternalException(e);
        }
    }

    @Override
    public RequestResponse<ProcessDetail> listOperationsDetails(ProcessQuery query) throws VitamClientException {
        try (
            Response response = make(get().withPath(OPERATION_URI).withBody(JsonHandler.toJsonNode(query)).withJson())
        ) {
            check(response);
            return RequestResponse.parseFromResponse(response, ProcessDetail.class);
        } catch (
            InvalidParseOperationException
            | NotAcceptableClientException
            | IngestInternalServerUnavailableClientException e
        ) {
            throw new VitamClientException(e);
        } catch (
            VitamClientInternalException
            | IngestInternalClientServerException
            | IngestInternalClientNotFoundException
            | IngestInternalClientConflictException e
        ) {
            throw new VitamClientInternalException(e);
        }
    }

    @Override
    public RequestResponse<WorkFlow> getWorkflowDefinitions() throws VitamClientException {
        try (Response response = make(get().withPath(WORKFLOWS_URI).withJsonAccept())) {
            check(response);
            return RequestResponse.parseFromResponse(response, WorkFlow.class);
        } catch (
            InvalidParseOperationException
            | NotAcceptableClientException
            | IngestInternalServerUnavailableClientException e
        ) {
            throw new VitamClientException(e);
        } catch (
            VitamClientInternalException
            | IngestInternalClientServerException
            | IngestInternalClientNotFoundException
            | IngestInternalClientConflictException e
        ) {
            throw new VitamClientInternalException(e);
        }
    }

    @Override
    public Optional<WorkFlow> getWorkflowDetails(String workflowIdentifier) throws VitamClientException {
        try (Response response = make(get().withPath(WORKFLOWS_URI + "/" + workflowIdentifier).withJsonAccept())) {
            check(response);
            return Optional.of(response.readEntity(WorkFlow.class));
        } catch (
            InvalidParseOperationException
            | NotAcceptableClientException
            | IngestInternalClientServerException
            | IngestInternalServerUnavailableClientException e
        ) {
            throw new VitamClientException("Internal Error Server", e);
        } catch (IngestInternalClientNotFoundException e) {
            return Optional.empty();
        }
    }

    @Override
    public void saveObjectToWorkspace(String id, String objectName, InputStream inputStream)
        throws VitamClientException {
        try (
            Response response = make(
                put().withPath("workspace" + "/" + id + "/" + objectName).withBody(inputStream).withJson()
            )
        ) {
            check(response);
        } catch (
            InvalidParseOperationException
            | NotAcceptableClientException
            | IngestInternalClientServerException
            | IngestInternalServerUnavailableClientException
            | IngestInternalClientNotFoundException e
        ) {
            throw new VitamClientException("Internal Error Server", e);
        }
    }

    private void check(Response response)
        throws VitamClientException, IngestInternalClientServerException, IngestInternalServerUnavailableClientException, InvalidParseOperationException, IngestInternalClientNotFoundException, NotAcceptableClientException {
        Status status = response.getStatusInfo().toEnum();
        if (SUCCESSFUL.equals(status.getFamily())) {
            return;
        }

        switch (status) {
            case INTERNAL_SERVER_ERROR:
                throw new IngestInternalClientServerException(INTERNAL_SERVER_ERROR);
            case NOT_ACCEPTABLE:
                throw new NotAcceptableClientException(NOT_ACCEPTABLE_EXCEPTION);
            case NO_CONTENT:
                throw new WorkflowNotFoundException(PROCESS_WORKFLOW_NOT_FOUND_FOR_OPERATION);
            case SERVICE_UNAVAILABLE:
                throw new IngestInternalServerUnavailableClientException(SERVICE_UNAVAILABLE_EXCEPTION);
            case NOT_FOUND:
                throw new IngestInternalClientNotFoundException(NOT_FOUND_EXCEPTION);
            case CONFLICT:
                throw new IngestInternalClientConflictException(REQUEST_CONFLICT);
            case BAD_REQUEST:
                throw new InvalidParseOperationException(INVALID_PARSE_OPERATION);
            case PRECONDITION_FAILED:
                throw new VitamClientInternalException(REQUEST_PRECONDITION_FAILED);
            case UNAUTHORIZED:
                throw new VitamClientInternalException(UNAUTHORIZED);
            default:
                throw new VitamClientException(status.getReasonPhrase());
        }
    }
}
