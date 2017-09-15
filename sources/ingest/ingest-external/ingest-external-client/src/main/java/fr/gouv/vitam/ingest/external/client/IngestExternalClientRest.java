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
package fr.gouv.vitam.ingest.external.client;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.external.client.DefaultClient;
import fr.gouv.vitam.common.external.client.IngestCollection;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessQuery;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.processing.ProcessDetail;
import fr.gouv.vitam.common.model.processing.WorkFlow;
import fr.gouv.vitam.ingest.external.api.exception.IngestExternalException;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import static org.apache.http.HttpHeaders.EXPECT;
import static org.apache.http.protocol.HTTP.EXPECT_CONTINUE;

/**
 * Ingest External client
 */
class IngestExternalClientRest extends DefaultClient implements IngestExternalClient {
    private static final String INGEST_EXTERNAL_MODULE = "IngestExternalModule";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IngestExternalClientRest.class);
    private static final String INGEST_URL = "/ingests";
    private static final String BLANK_OBJECT_ID = "object identifier should be filled";
    private static final String BLANK_TYPE = "Type should be filled";
    private static final String BLANK_OPERATION_ID = "Operation identifier should be filled";
    private static final String BLANK_TENANT_ID = "Tenant identifier should be filled";
    private static final String BLANK_ACTION_ID = "Action should be filled";
    private static final String OPERATION_URI = "/operations";
    private static final String WORKFLOWS_URI = "/workflows";
    private static final String REQUEST_PRECONDITION_FAILED = "Request precondition failed";
    private static final String NOT_FOUND_EXCEPTION = "Not Found Exception";
    private static final String UNAUTHORIZED = "Unauthorized";

    IngestExternalClientRest(IngestExternalClientFactory factory) {
        super(factory);
    }

    @Override
    public RequestResponse<JsonNode> upload(InputStream stream, Integer tenantId, String contextId, String action)
        throws IngestExternalException {

        ParametersChecker.checkParameter("Stream is a mandatory parameter", stream);
        ParametersChecker.checkParameter("Tenant identifier is a mandatory parameter", tenantId);
        Response response = null;
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(GlobalDataRest.X_CONTEXT_ID, contextId);
        headers.add(GlobalDataRest.X_TENANT_ID, tenantId);
        headers.add(GlobalDataRest.X_ACTION, action);
        headers.add(EXPECT, EXPECT_CONTINUE);

        try {
            response = performRequest(HttpMethod.POST, INGEST_URL, headers,
                stream, MediaType.APPLICATION_OCTET_STREAM_TYPE, MediaType.APPLICATION_XML_TYPE);
            final Status status = Status.fromStatusCode(response.getStatus());
            switch (status) {
                case ACCEPTED:
                    LOGGER.debug(Status.ACCEPTED.getReasonPhrase());
                    return new RequestResponseOK<JsonNode>().parseHeadersFromResponse(response)
                        .setHttpCode(response.getStatus());
                case BAD_REQUEST:
                case PARTIAL_CONTENT:
                case INTERNAL_SERVER_ERROR:
                    LOGGER.error(ErrorMessage.INGEST_EXTERNAL_UPLOAD_ERROR.getMessage());
                    final VitamError vitamError = new VitamError(VitamCode.INGEST_EXTERNAL_UPLOAD_ERROR.getItem())
                        .setMessage(VitamCode.INGEST_EXTERNAL_UPLOAD_ERROR.getMessage())
                        .setState(StatusCode.KO.name())
                        .setContext(INGEST_EXTERNAL_MODULE);

                    return vitamError.setHttpCode(status.getStatusCode())
                        .setDescription(VitamCode.INGEST_EXTERNAL_UPLOAD_ERROR.getMessage() + " Cause : " +
                            status.getReasonPhrase());
                case SERVICE_UNAVAILABLE:
                    LOGGER.error(ErrorMessage.INGEST_EXTERNAL_UPLOAD_ERROR.getMessage());
                    final VitamError vitamErrorFatal = new VitamError(VitamCode.INGEST_EXTERNAL_UPLOAD_ERROR.getItem())
                        .setMessage(response.readEntity(String.class))
                        .setState(StatusCode.FATAL.name())
                        .setContext("IngestExternalModule");

                    return vitamErrorFatal.setHttpCode(status.getStatusCode())
                        .setDescription(VitamCode.INGEST_EXTERNAL_UPLOAD_ERROR.getMessage() + " Cause : " +
                            status.getReasonPhrase());
                default:
                    throw new IngestExternalException("Unknown error: " + status.getReasonPhrase());
            }
        } catch (final VitamClientInternalException e) {
            LOGGER.error("Ingest External Internal Server Error", e);
            throw new IngestExternalException("Ingest External Internal Server Error", e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    public Response downloadObjectAsync(String objectId, IngestCollection type, Integer tenantId)
        throws VitamClientException {

        ParametersChecker.checkParameter(BLANK_OBJECT_ID, objectId);
        ParametersChecker.checkParameter(BLANK_TYPE, type);

        Response response = null;
        MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(GlobalDataRest.X_TENANT_ID, tenantId);
        try {
            response = performRequest(HttpMethod.GET, INGEST_URL + "/" + objectId + "/" + type.getCollectionName(),
                headers, MediaType.APPLICATION_OCTET_STREAM_TYPE);

        } catch (final VitamClientInternalException e) {
            LOGGER.error("VitamClientInternalException: ", e);
            throw new VitamClientException(e);
        }
        return response;
    }

    @Override
    public RequestResponse<ItemStatus> updateOperationActionProcess(String actionId, String operationId,
        Integer tenantId)
        throws VitamClientException {

        ParametersChecker.checkParameter(BLANK_OPERATION_ID, operationId);
        ParametersChecker.checkParameter(BLANK_TENANT_ID, tenantId);
        ParametersChecker.checkParameter(BLANK_ACTION_ID, actionId);
        Response response = null;
        try {

            final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
            headers.add(GlobalDataRest.X_TENANT_ID, tenantId);
            headers.add(GlobalDataRest.X_ACTION, actionId);
            response =
                performRequest(HttpMethod.PUT, OPERATION_URI + "/" + operationId, headers,
                    MediaType.APPLICATION_JSON_TYPE);
            return RequestResponse.parseFromResponse(response, ItemStatus.class);

        } catch (IllegalStateException e) {
            LOGGER.error("Could not parse server response ", e);
            throw createExceptionFromResponse(response);
        } catch (VitamClientInternalException e) {
            LOGGER.error("VitamClientInternalException: ", e);
            throw new VitamClientException(e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<ItemStatus> getOperationProcessStatus(String id, Integer tenantId)
        throws VitamClientException {
        ParametersChecker.checkParameter(BLANK_OPERATION_ID, id);
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(GlobalDataRest.X_TENANT_ID, tenantId);
        Response response = null;
        try {
            response =
                performRequest(HttpMethod.HEAD, OPERATION_URI + "/" + id, headers, MediaType.APPLICATION_JSON_TYPE);

            if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
                LOGGER.warn("SIP Warning : " + Response.Status.NOT_FOUND.getReasonPhrase());
                return VitamCodeHelper.toVitamError(VitamCode.INGEST_EXTERNAL_NOT_FOUND, NOT_FOUND_EXCEPTION);
            } else if (response.getStatus() == Status.PRECONDITION_FAILED.getStatusCode()) {
                LOGGER.warn("SIP Warning : " + Response.Status.PRECONDITION_FAILED.getReasonPhrase());
                return VitamCodeHelper.toVitamError(VitamCode.INGEST_EXTERNAL_PRECONDITION_FAILED,
                    REQUEST_PRECONDITION_FAILED);
            } else if (response.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                LOGGER.warn("SIP Warning : " + Response.Status.UNAUTHORIZED.getReasonPhrase());
                return VitamCodeHelper.toVitamError(VitamCode.INGEST_EXTERNAL_UNAUTHORIZED, UNAUTHORIZED);
            } else if (response.getStatus() != Status.OK.getStatusCode() &&
                response.getStatus() != Status.ACCEPTED.getStatusCode()) {
                LOGGER.warn("SIP Warning : " + Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase());
                return VitamCodeHelper.toVitamError(VitamCode.INGEST_EXTERNAL_INTERNAL_SERVER_ERROR,
                    INTERNAL_SERVER_ERROR);
            }

            ItemStatus itemStatus = new ItemStatus()
                .setGlobalState(ProcessState.valueOf(response.getHeaderString(GlobalDataRest.X_GLOBAL_EXECUTION_STATE)))
                .setLogbookTypeProcess(response.getHeaderString(GlobalDataRest.X_CONTEXT_ID))
                .increment(StatusCode.valueOf(response.getHeaderString(GlobalDataRest.X_GLOBAL_EXECUTION_STATUS)));
            return new RequestResponseOK<ItemStatus>().addResult(itemStatus).setHttpCode(response.getStatus());


        } catch (VitamClientInternalException e) {
            LOGGER.error("VitamClientInternalException: ", e);
            throw new VitamClientException(e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public boolean wait(int tenantId, String processId, ProcessState state, int nbTry, long timeWait, TimeUnit timeUnit)
        throws VitamException {
        for (int i = 0; i < nbTry; i++) {
            final RequestResponse<ItemStatus> requestResponse = this.getOperationProcessStatus(processId, tenantId);
            if (requestResponse.isOk()) {
                ItemStatus itemStatus = ((RequestResponseOK<ItemStatus>) requestResponse).getResults().get(0);
                final ProcessState processState = itemStatus.getGlobalState();
                final StatusCode statusCode = itemStatus.getGlobalStatus();

                switch (processState) {
                    case COMPLETED:
                        return true;
                    case PAUSE:
                        if (StatusCode.STARTED.compareTo(statusCode) <= 0) {
                            return true;
                        }
                        break;
                    case RUNNING:
                        break;
                }

                if (null != timeUnit) {
                    timeWait = timeUnit.toMillis(timeWait);
                }
                try {
                    Thread.sleep(timeWait);
                } catch (InterruptedException e) {
                    SysErrLogger.FAKE_LOGGER.ignoreLog(e);
                }
            } else {
                throw new VitamException((VitamError) requestResponse);
            }
        }
        return false;

    }

    @Override
    public boolean wait(int tenantId, String processId, int nbTry, long timeout, TimeUnit timeUnit)
        throws VitamException {
        return wait(tenantId, processId, ProcessState.COMPLETED, nbTry, timeout, timeUnit);
    }

    @Override
    public boolean wait(int tenantId, String processId, ProcessState state) throws VitamException {
        return wait(tenantId, processId, state, Integer.MAX_VALUE, 1000l, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean wait(int tenantId, String processId) throws VitamException {
        return wait(tenantId, processId, ProcessState.COMPLETED);
    }

    @Override
    public RequestResponse<ItemStatus> getOperationProcessExecutionDetails(String id, Integer tenantId)
        throws VitamClientException {
        ParametersChecker.checkParameter(BLANK_OPERATION_ID, id);
        ParametersChecker.checkParameter(BLANK_TENANT_ID, tenantId);

        Response response = null;
        try {
            final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
            headers.add(GlobalDataRest.X_TENANT_ID, tenantId);
            headers.add(GlobalDataRest.X_ACTION, id);
            response =
                performRequest(HttpMethod.GET, OPERATION_URI + "/" + id, headers, MediaType.APPLICATION_JSON_TYPE);
            return RequestResponse.parseFromResponse(response, ItemStatus.class);

        } catch (IllegalStateException e) {
            LOGGER.error("Could not parse server response ", e);
            throw createExceptionFromResponse(response);
        } catch (VitamClientInternalException e) {
            LOGGER.error("VitamClientInternalException: ", e);
            throw new VitamClientException(e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<ItemStatus> cancelOperationProcessExecution(String id, Integer tenantId)
        throws VitamClientException, IllegalArgumentException {
        ParametersChecker.checkParameter(BLANK_OPERATION_ID, id);
        ParametersChecker.checkParameter(BLANK_TENANT_ID, tenantId);
        Response response = null;
        try {
            final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
            headers.add(GlobalDataRest.X_TENANT_ID, tenantId);
            response =
                performRequest(HttpMethod.DELETE, OPERATION_URI + "/" + id, headers, MediaType.APPLICATION_JSON_TYPE);
            return RequestResponse.parseFromResponse(response, ItemStatus.class);

        } catch (IllegalStateException e) {
            LOGGER.error("Could not parse server response ", e);
            throw createExceptionFromResponse(response);
        } catch (VitamClientInternalException e) {
            LOGGER.error("VitamClientInternalException: ", e);
            throw new VitamClientException(e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<ProcessDetail> listOperationsDetails(Integer tenantId, ProcessQuery query)
        throws VitamClientException {
        Response response = null;
        try {
            final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
            headers.add(GlobalDataRest.X_TENANT_ID, tenantId);

            if (query == null) {
                query = new ProcessQuery();
            }

            response = performRequest(HttpMethod.GET, OPERATION_URI, headers, JsonHandler.toJsonNode(query),
                MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);
            return RequestResponse.parseFromResponse(response, ProcessDetail.class);

        } catch (IllegalStateException e) {
            LOGGER.error("Could not parse server response ", e);
            throw createExceptionFromResponse(response);
        } catch (InvalidParseOperationException e) {
            LOGGER.error("Could not parse query ", e);
            throw new VitamClientException(e);
        } catch (VitamClientInternalException e) {
            LOGGER.error("VitamClientInternalException: ", e);
            throw new VitamClientException(e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<WorkFlow> getWorkflowDefinitions(Integer tenantId) throws VitamClientException {
        Response response = null;
        try {
            final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
            headers.add(GlobalDataRest.X_TENANT_ID, tenantId);

            response = performRequest(HttpMethod.GET, WORKFLOWS_URI, headers, MediaType.APPLICATION_JSON_TYPE);
            return RequestResponse.parseFromResponse(response, WorkFlow.class);

        } catch (IllegalStateException e) {
            LOGGER.error("Could not parse server response ", e);
            throw createExceptionFromResponse(response);
        } catch (VitamClientInternalException e) {
            LOGGER.error("VitamClientInternalException: ", e);
            throw new VitamClientException(e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }
}
