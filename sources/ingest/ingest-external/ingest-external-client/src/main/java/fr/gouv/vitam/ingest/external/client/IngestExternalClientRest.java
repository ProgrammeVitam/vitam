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

import static fr.gouv.vitam.common.GlobalDataRest.X_ACTION;
import static fr.gouv.vitam.common.GlobalDataRest.X_CONTEXT_ID;
import static fr.gouv.vitam.common.GlobalDataRest.X_TENANT_ID;
import static fr.gouv.vitam.common.model.ProcessExecutionStatus.CANCELLED;
import static fr.gouv.vitam.common.model.ProcessExecutionStatus.COMPLETED;
import static fr.gouv.vitam.common.model.ProcessExecutionStatus.FAILED;
import static org.apache.http.HttpHeaders.EXPECT;
import static org.apache.http.protocol.HTTP.EXPECT_CONTINUE;

import java.io.InputStream;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.client.DefaultClient;
import fr.gouv.vitam.common.client.IngestCollection;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InternalServerException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.ProcessExecutionStatus;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.ingest.external.api.exception.IngestExternalException;
import fr.gouv.vitam.ingest.external.common.client.ErrorMessage;

/**
 * Ingest External client
 */
class IngestExternalClientRest extends DefaultClient implements IngestExternalClient {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IngestExternalClientRest.class);
    private static final String INGEST_URL = "/ingests";
    private static final String BLANK_OBJECT_ID = "object identifier should be filled";
    private static final String BLANK_TYPE = "Type should be filled";
    private static final String CONTEXT_ID_MUST_HAVE_A_VALID_VALUE = "Context id must have a valid value";
    private static final String BLANK_OPERATION_ID = "Operation identifier should be filled";
    private static final String OPERATION_URI = "/operations";
    private static final String REQUEST_PRECONDITION_FAILED = "Request precondition failed";
    private static final String NOT_FOUND_EXCEPTION = "Not Found Exception";
    private static final String UNAUTHORIZED = "Unauthorized";
    private static final int TIME_TO_SLEEP = 1000; // Tree seconds

    IngestExternalClientRest(IngestExternalClientFactory factory) {
        super(factory);
    }

    @Override
    public Response upload(InputStream stream, Integer tenantId, String contextId, String action)
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
                    break;
                case BAD_REQUEST:
                    LOGGER.error(ErrorMessage.INGEST_EXTERNAL_UPLOAD_ERROR.getMessage());
                    break;
                case PARTIAL_CONTENT:
                    LOGGER.warn(ErrorMessage.INGEST_EXTERNAL_UPLOAD_WITH_WARNING.getMessage());
                    break;
                case INTERNAL_SERVER_ERROR:
                    LOGGER.warn(ErrorMessage.INGEST_EXTERNAL_UPLOAD_ERROR.getMessage());
                    break;
                default:
                    throw new IngestExternalException("Unknown error");
            }
        } catch (final VitamClientInternalException e) {
            LOGGER.error("Ingest External Internal Server Error", e);
            throw new IngestExternalException("Ingest External Internal Server Error", e);
        } finally {
            if (response != null && response.getStatus() != Status.ACCEPTED.getStatusCode() &&
                response.getStatus() != Status.BAD_REQUEST.getStatusCode() &&
                response.getStatus() != Status.PARTIAL_CONTENT.getStatusCode() &&
                response.getStatus() != Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
                consumeAnyEntityAndClose(response);
            }
        }
        return response;
    }

    @Override
    public Response uploadAndWaitAtr(InputStream stream, Integer tenantId, String contextId, String action) {

        try (IngestExternalClient client = IngestExternalClientFactory.getInstance().getClient()) {
            Response response = client.upload(stream, tenantId, contextId, action);
            String id = response.getHeaderString(GlobalDataRest.X_REQUEST_ID);
            if (!Status.fromStatusCode(response.getStatus()).equals(Status.ACCEPTED)) {

                return response;
            }
            while (Status.fromStatusCode(response.getStatus()).equals(Status.ACCEPTED)) {
                //SLEEP
                try {
                    Thread.sleep(TIME_TO_SLEEP);
                } catch (InterruptedException ex) {
                    LOGGER.error(ex);
                    Thread.currentThread().interrupt();
                    return response;
                }

                try {
                    response = getOperationStatus(id, tenantId);
                } catch (Exception e) {
                    LOGGER.error(e);
                    return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e).build();
                }
            }
            return response;
        } catch (final VitamException e) {
            LOGGER.error("IngestExternalException in Upload sip", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e).build();
        }
    }

    public Response downloadObjectAsync(String objectId, IngestCollection type, Integer tenantId)
        throws IngestExternalException {

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
            throw new IngestExternalException("Ingest External Internal Server Error", e);
        } finally {
            if (response != null && response.getStatus() != Status.OK.getStatusCode()) {
                consumeAnyEntityAndClose(response);
            }
        }
        return response;
    }

    @Override
    public Response initWorkFlow(String contextId) throws VitamException {
        ParametersChecker.checkParameter("Params cannot be null", contextId);
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(GlobalDataRest.X_CONTEXT_ID, contextId);
        headers.add(GlobalDataRest.X_ACTION, ProcessAction.INIT);

        // add header action id default init
        Response response = null;
        try {
            response = performRequest(HttpMethod.POST, INGEST_URL, headers, MediaType.APPLICATION_OCTET_STREAM_TYPE);
            if (Status.ACCEPTED.getStatusCode() == response.getStatus()) {
                LOGGER.warn("SIP Warning : " + Status.ACCEPTED.getReasonPhrase());
            } else {
                LOGGER.error("SIP Upload Error: " + Status.fromStatusCode(response.getStatus()).getReasonPhrase());
            }
            return Response.fromResponse(response).build();
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public Response executeOperationProcess(String operationId, String workflow, String contextId, String actionId)
        throws VitamClientException {
        ParametersChecker.checkParameter(BLANK_OPERATION_ID, operationId);
        ParametersChecker.checkParameter(CONTEXT_ID_MUST_HAVE_A_VALID_VALUE, contextId);
        Response response = null;
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(GlobalDataRest.X_CONTEXT_ID, contextId);
        try {
            response =
                performRequest(HttpMethod.POST, OPERATION_URI + "/" + operationId, headers,
                    MediaType.APPLICATION_JSON_TYPE);
            if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
                LOGGER.warn("SIP Warning : " + Response.Status.NOT_FOUND.getReasonPhrase());
                throw new VitamClientInternalException(NOT_FOUND_EXCEPTION);
            } else if (response.getStatus() == Status.PRECONDITION_FAILED.getStatusCode()) {
                LOGGER.warn("SIP Warning : " + Response.Status.PRECONDITION_FAILED.getReasonPhrase());
                throw new VitamClientInternalException(REQUEST_PRECONDITION_FAILED);

            } else if (response.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                LOGGER.warn("SIP Warning : " + Response.Status.UNAUTHORIZED.getReasonPhrase());
                throw new VitamClientInternalException(UNAUTHORIZED);
            } else if (response.getStatus() == Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
                LOGGER.warn("SIP Warning : " + Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase());
                throw new VitamClientInternalException(INTERNAL_SERVER_ERROR);
            }
            return Response.fromResponse(response).build();
        } catch (VitamClientInternalException e) {
            LOGGER.error("VitamClientInternalException: ", e);
            throw new VitamClientException(e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    // use with async : do not close the answer.
    @Override
    public Response updateOperationActionProcess(String actionId, String operationId) throws VitamClientException {
        ParametersChecker.checkParameter(BLANK_OPERATION_ID, operationId);

        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(GlobalDataRest.X_ACTION, actionId);
        headers.add(GlobalDataRest.X_TENANT_ID, VitamThreadUtils.getVitamSession().getTenantId());

        Response response = null;
        try {
            response =
                performRequest(HttpMethod.PUT, OPERATION_URI + "/" + operationId,
                    headers,
                    MediaType.APPLICATION_JSON_TYPE);
            if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
                LOGGER.warn("SIP Warning : " + Response.Status.NOT_FOUND.getReasonPhrase());
                throw new VitamClientInternalException(NOT_FOUND_EXCEPTION);
            } else if (response.getStatus() == Status.PRECONDITION_FAILED.getStatusCode()) {
                LOGGER.warn("SIP Warning : " + Response.Status.PRECONDITION_FAILED.getReasonPhrase());
                throw new VitamClientInternalException(REQUEST_PRECONDITION_FAILED);

            } else if (response.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                LOGGER.warn("SIP Warning : " + Response.Status.UNAUTHORIZED.getReasonPhrase());
                throw new ProcessingException(UNAUTHORIZED);
            } else if (response.getStatus() == Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
                LOGGER.warn("SIP Warning : " + Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase());
                throw new VitamClientInternalException(INTERNAL_SERVER_ERROR);
            }
            return Response.fromResponse(response).build();

        } catch (VitamClientInternalException e) {
            LOGGER.error("VitamClientInternalException: ", e);
            throw new VitamClientException(e);
        }

    }

    @Override
    public ItemStatus getOperationProcessStatus(String id) throws VitamClientException {
        ParametersChecker.checkParameter(BLANK_OPERATION_ID, id);
        Response response = null;
        try {
            response =
                performRequest(HttpMethod.HEAD, OPERATION_URI + "/" + id, null, MediaType.APPLICATION_JSON_TYPE);
        } catch (VitamClientInternalException e) {
            LOGGER.error("VitamClientInternalException: ", e);
            throw new VitamClientException(e);
        }
        if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
            LOGGER.warn("SIP Warning : " + Response.Status.NOT_FOUND.getReasonPhrase());
            throw new VitamClientInternalException(NOT_FOUND_EXCEPTION);
        } else if (response.getStatus() == Status.PRECONDITION_FAILED.getStatusCode()) {
            LOGGER.warn("SIP Warning : " + Response.Status.PRECONDITION_FAILED.getReasonPhrase());
            throw new VitamClientInternalException(REQUEST_PRECONDITION_FAILED);

        } else if (response.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
            LOGGER.warn("SIP Warning : " + Response.Status.UNAUTHORIZED.getReasonPhrase());
            throw new VitamClientInternalException(UNAUTHORIZED);
        } else if (response.getStatus() == Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
            LOGGER.warn("SIP Warning : " + Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase());
            throw new VitamClientInternalException(INTERNAL_SERVER_ERROR);
        }
        return response.readEntity(ItemStatus.class);
    }

    @Override
    public Response getOperationStatus(String id, Integer tenantId) throws VitamClientException {
        ParametersChecker.checkParameter(BLANK_OPERATION_ID, id);
        Response response = null;
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(GlobalDataRest.X_ACTION, id);
        headers.add(GlobalDataRest.X_TENANT_ID, tenantId);
        ItemStatus pwok = null ;
        Status status = Status.ACCEPTED;
        try {
            response =
               performRequest(HttpMethod.GET, OPERATION_URI + "/" + id, headers, MediaType.APPLICATION_JSON_TYPE);
               pwok =  response.readEntity(ItemStatus.class);
               if (pwok == null ) {
                  return Response.status(Status.ACCEPTED).entity(pwok).header(GlobalDataRest.X_REQUEST_ID,id).build();
               }
             if  (
                     pwok.getGlobalExecutionStatus().equals(ProcessExecutionStatus.COMPLETED)||
                     pwok.getGlobalExecutionStatus().equals(ProcessExecutionStatus.CANCELLED) ||
                     pwok.getGlobalExecutionStatus().equals(ProcessExecutionStatus.FAILED)
                 )
             {
                 status = Status.OK;
             }
        } catch (VitamClientInternalException e) {
            LOGGER.error("VitamClientInternalException: ", e);
            throw new VitamClientException(e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
        return Response.status(status).entity(pwok).header(GlobalDataRest.X_REQUEST_ID,id).build();
    }


    @Override
    public ItemStatus getOperationProcessExecutionDetails(String id, JsonNode query) throws VitamClientException {
        ParametersChecker.checkParameter(BLANK_OPERATION_ID, id);
        Response response = null;
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(GlobalDataRest.X_ACTION, id);
        headers.add(GlobalDataRest.X_TENANT_ID, 0);
        try {
            response =
                performRequest(HttpMethod.GET, OPERATION_URI + "/" + id, null,
                    MediaType.APPLICATION_JSON_TYPE);
            if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
                LOGGER.warn("SIP Warning : " + Response.Status.NOT_FOUND.getReasonPhrase());
                throw new VitamClientInternalException(NOT_FOUND_EXCEPTION);
            } else if (response.getStatus() == Status.PRECONDITION_FAILED.getStatusCode()) {
                LOGGER.warn("SIP Warning : " + Response.Status.PRECONDITION_FAILED.getReasonPhrase());
                throw new VitamClientInternalException(REQUEST_PRECONDITION_FAILED);

            } else if (response.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                LOGGER.warn("SIP Warning : " + Response.Status.UNAUTHORIZED.getReasonPhrase());
                throw new VitamClientInternalException(UNAUTHORIZED);
            } else if (response.getStatus() == Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
                LOGGER.warn("SIP Warning : " + Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase());
                throw new VitamClientInternalException(INTERNAL_SERVER_ERROR);
            }

            return response.readEntity(ItemStatus.class);

        } catch (VitamClientInternalException e) {
            LOGGER.error("VitamClientInternalException: ", e);
            throw new VitamClientException(e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public Response cancelOperationProcessExecution(String id) throws VitamClientException, BadRequestException {
        ParametersChecker.checkParameter(BLANK_OPERATION_ID, id);
        Response response = null;
        try {
            final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
            headers.add(GlobalDataRest.X_TENANT_ID, VitamThreadUtils.getVitamSession().getTenantId());
            response =
                performRequest(HttpMethod.DELETE, OPERATION_URI + "/" + id, headers, MediaType.APPLICATION_JSON_TYPE);
            if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
                LOGGER.warn("SIP Warning : " + Response.Status.NOT_FOUND.getReasonPhrase());
                throw new VitamClientInternalException(NOT_FOUND_EXCEPTION);
            } else if (response.getStatus() == Status.PRECONDITION_FAILED.getStatusCode()) {
                LOGGER.warn("SIP Warning : " + Response.Status.PRECONDITION_FAILED.getReasonPhrase());
                throw new VitamClientInternalException(REQUEST_PRECONDITION_FAILED);

            } else if (response.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                LOGGER.warn("SIP Warning : " + Response.Status.UNAUTHORIZED.getReasonPhrase());
                throw new BadRequestException(UNAUTHORIZED);
            } else if (response.getStatus() == Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
                LOGGER.warn("SIP Warning : " + Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase());
                throw new VitamClientInternalException(INTERNAL_SERVER_ERROR);
            }
            return Response.fromResponse(response).build();

        } catch (VitamClientInternalException e) {
            LOGGER.error("VitamClientInternalException: ", e);
            throw new VitamClientException(e);
        } finally {
            consumeAnyEntityAndClose(response);
        }

    }

    @Override
    public ItemStatus updateVitamProcess(String contextId, String actionId, String container, String workflow)
        throws InternalServerException, BadRequestException, VitamClientException {
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        // FIXME : bad comparaison
        if (actionId.equals(ProcessAction.START)) {
            ParametersChecker.checkParameter(CONTEXT_ID_MUST_HAVE_A_VALID_VALUE, contextId);
            headers.add(GlobalDataRest.X_CONTEXT_ID, contextId);
        }
        Response response = null;
        try {
            response =
                performRequest(HttpMethod.PUT, OPERATION_URI,
                    headers,
                    MediaType.APPLICATION_JSON_TYPE);

            if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
                LOGGER.warn("SIP Warning : " + Response.Status.NOT_FOUND.getReasonPhrase());
                throw new VitamClientInternalException(NOT_FOUND_EXCEPTION);
            } else if (response.getStatus() == Status.PRECONDITION_FAILED.getStatusCode()) {
                LOGGER.warn("SIP Warning : " + Response.Status.PRECONDITION_FAILED.getReasonPhrase());
                throw new VitamClientInternalException(REQUEST_PRECONDITION_FAILED);

            } else if (response.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                LOGGER.warn("SIP Warning : " + Response.Status.UNAUTHORIZED.getReasonPhrase());
                throw new VitamClientInternalException(UNAUTHORIZED);
            } else if (response.getStatus() == Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
                LOGGER.warn("SIP Warning : " + Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase());
                throw new VitamClientInternalException(INTERNAL_SERVER_ERROR);
            }


            return response.readEntity(ItemStatus.class);

        } catch (VitamClientInternalException e) {
            LOGGER.error("VitamClientInternalException: ", e);
            throw new VitamClientException(e);
        } finally {
            consumeAnyEntityAndClose(response);
        }

    }

    @Override
    public Response initVitamProcess(String contextId, String container, String workFlow)
        throws InternalServerException, VitamClientException {
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(GlobalDataRest.X_CONTEXT_ID, ProcessAction.INIT);

        Response response = null;
        try {
            response =
                performRequest(HttpMethod.PUT, OPERATION_URI,
                    headers,
                    MediaType.APPLICATION_JSON_TYPE);
            if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
                LOGGER.warn("SIP Warning : " + Response.Status.NOT_FOUND.getReasonPhrase());
                throw new VitamClientInternalException(NOT_FOUND_EXCEPTION);
            } else if (response.getStatus() == Status.PRECONDITION_FAILED.getStatusCode()) {
                LOGGER.warn("SIP Warning : " + Response.Status.PRECONDITION_FAILED.getReasonPhrase());
                throw new VitamClientInternalException(REQUEST_PRECONDITION_FAILED);

            } else if (response.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                LOGGER.warn("SIP Warning : " + Response.Status.UNAUTHORIZED.getReasonPhrase());
                throw new VitamClientInternalException(UNAUTHORIZED);
            } else if (response.getStatus() == Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
                LOGGER.warn("SIP Warning : " + Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase());
                throw new VitamClientInternalException(INTERNAL_SERVER_ERROR);
            }
            return Response.fromResponse(response).build();
        } catch (VitamClientInternalException e) {
            LOGGER.error("VitamClientInternalException: ", e);
            throw new VitamClientException(e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public Response listOperationsDetails() throws VitamClientException {
        Response response = null;
        try {
            final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
            headers.add(GlobalDataRest.X_TENANT_ID, VitamThreadUtils.getVitamSession().getTenantId());

            response = performRequest(HttpMethod.GET, OPERATION_URI, headers,
                MediaType.APPLICATION_JSON_TYPE);
            return Response.fromResponse(response).build();

        } catch (VitamClientInternalException e) {
            LOGGER.error("VitamClientInternalException: ", e);
            throw new VitamClientException(e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }
}
