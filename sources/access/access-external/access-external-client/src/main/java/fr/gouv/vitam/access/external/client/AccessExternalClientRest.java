package fr.gouv.vitam.access.external.client;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.access.external.common.exception.AccessExternalClientNotFoundException;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientServerException;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.exception.AccessUnauthorizedException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.external.client.DefaultClient;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.logbook.LogbookLifecycle;
import fr.gouv.vitam.common.model.logbook.LogbookOperation;
import fr.gouv.vitam.logbook.common.client.ErrorMessage;

/**
 * Rest client implementation for Access External
 */
class AccessExternalClientRest extends DefaultClient implements AccessExternalClient {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AccessExternalClientRest.class);

    private static final String INVALID_PARSE_OPERATION = "Invalid Parse Operation";
    private static final String UNITS = "/units/";
    private static final String OBJECTS = "/objects/";
    private static final String BLANK_DSL = "select DSL is blank";
    private static final String BLANK_UNIT_ID = "unit identifier should be filled";
    private static final String BLANK_OBJECT_ID = "object identifier should be filled";
    private static final String BLANK_OBJECT_GROUP_ID = "object identifier should be filled";
    private static final String BLANK_USAGE = "usage should be filled";
    private static final String BLANK_VERSION = "usage version should be filled";

    private static final String LOGBOOK_OPERATIONS_URL = "/operations";
    private static final String LOGBOOK_UNIT_LIFECYCLE_URL = "/unitlifecycles";
    private static final String LOGBOOK_OBJECT_LIFECYCLE_URL = "/objectgrouplifecycles";

    AccessExternalClientRest(AccessExternalClientFactory factory) {
        super(factory);
    }

    @Override
    public RequestResponse<JsonNode> selectUnits(VitamContext vitamContext, JsonNode selectQuery)
        throws VitamClientException {
        Response response = null;

        MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putAll(vitamContext.getHeaders());

        try {
            response = performRequest(HttpMethod.GET, "/units", headers,
                selectQuery, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE, false);
            return RequestResponse.parseFromResponse(response, JsonNode.class);

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
    public RequestResponse<JsonNode> selectUnitbyId(VitamContext vitamContext, JsonNode selectQuery, String unitId)
        throws VitamClientException {
        Response response = null;

        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putAll(vitamContext.getHeaders());

        ParametersChecker.checkParameter(BLANK_UNIT_ID, unitId);

        try {
            response = performRequest(HttpMethod.GET, UNITS + unitId, headers,
                selectQuery, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE, false);
            return RequestResponse.parseFromResponse(response, JsonNode.class);

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
    public RequestResponse<JsonNode> updateUnitbyId(VitamContext vitamContext, JsonNode updateQuery, String unitId)
        throws VitamClientException {
        Response response = null;

        MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putAll(vitamContext.getHeaders());

        ParametersChecker.checkParameter(BLANK_UNIT_ID, unitId);

        try {
            response = performRequest(HttpMethod.PUT, UNITS + unitId, headers, updateQuery,
                MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE, false);
            return RequestResponse.parseFromResponse(response, JsonNode.class);

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
    public RequestResponse<JsonNode> selectObjectMetadatasByUnitId(VitamContext vitamContext,
        JsonNode selectObjectQuery,
        String unitId)
        throws VitamClientException {

        if (selectObjectQuery == null || selectObjectQuery.size() == 0) {
            throw new IllegalArgumentException(BLANK_DSL);
        }
        ParametersChecker.checkParameter(BLANK_OBJECT_ID, unitId);

        Response response = null;
        MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putAll(vitamContext.getHeaders());
        try {
            response = performRequest(HttpMethod.GET, "/units/" + unitId + "/object", headers,
                selectObjectQuery, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE, false);
            return RequestResponse.parseFromResponse(response, JsonNode.class);

        } catch (IllegalStateException e) {
            LOGGER.error("Could not parse server response ", e);
            throw createExceptionFromResponse(response);
        } catch (final VitamClientInternalException e) {
            LOGGER.error("VitamClientInternalException: ", e);
            throw new VitamClientException(e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }


    @Override
    public Response getObjectStreamByUnitId(VitamContext vitamContext, JsonNode selectObjectQuery,
        String unitId,
        String usage,
        int version)
        throws VitamClientException {

        if (selectObjectQuery == null || selectObjectQuery.size() == 0) {
            throw new IllegalArgumentException(BLANK_DSL);
        }
        ParametersChecker.checkParameter(BLANK_OBJECT_ID, unitId);
        ParametersChecker.checkParameter(BLANK_USAGE, usage);
        ParametersChecker.checkParameter(BLANK_VERSION, version);

        Response response = null;
        MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putAll(vitamContext.getHeaders());
        headers.add(GlobalDataRest.X_QUALIFIER, usage);
        headers.add(GlobalDataRest.X_VERSION, version);

        try {
            response = performRequest(HttpMethod.GET, "/units/" + unitId + "/object", headers,
                selectObjectQuery, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_OCTET_STREAM_TYPE, false);

        } catch (final VitamClientInternalException e) {
            LOGGER.error("VitamClientInternalException: ", e);
            throw new VitamClientException(e);
        }
        return response;
    }

    @Override
    @Deprecated
    public Response getObject(VitamContext vitamContext, JsonNode selectObjectQuery,
        String objectId,
        String usage, int version)
        throws InvalidParseOperationException, AccessExternalClientServerException,
        AccessExternalClientNotFoundException, AccessUnauthorizedException {

        if (selectObjectQuery == null || selectObjectQuery.size() == 0) {
            throw new IllegalArgumentException(BLANK_DSL);
        }
        ParametersChecker.checkParameter(BLANK_OBJECT_GROUP_ID, objectId);
        ParametersChecker.checkParameter(BLANK_USAGE, usage);
        ParametersChecker.checkParameter(BLANK_VERSION, version);

        Response response = null;
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(GlobalDataRest.X_HTTP_METHOD_OVERRIDE, HttpMethod.GET);
        headers.add(GlobalDataRest.X_QUALIFIER, usage);
        headers.add(GlobalDataRest.X_VERSION, version);
        headers.putAll(vitamContext.getHeaders());


        try {
            response = performRequest(HttpMethod.POST, OBJECTS + objectId, headers,
                selectObjectQuery, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_OCTET_STREAM_TYPE);
            final Response.Status status = Response.Status.fromStatusCode(response.getStatus());
            if (response.getStatus() == Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
                LOGGER.error("Internal Server Error" + " : " + status.getReasonPhrase());
                throw new AccessExternalClientServerException("Internal Server Error");
            } else if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
                throw new AccessExternalClientNotFoundException(status.getReasonPhrase());
            } else if (response.getStatus() == Status.BAD_REQUEST.getStatusCode()) {
                throw new InvalidParseOperationException(INVALID_PARSE_OPERATION);
            } else if (response.getStatus() == Status.PRECONDITION_FAILED.getStatusCode()) {
                throw new AccessExternalClientServerException(response.getStatusInfo().getReasonPhrase());
            } else if (response.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                throw new AccessUnauthorizedException(response.getStatusInfo().getReasonPhrase());
            }

            return response;
        } catch (final VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new AccessExternalClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            if (response != null && response.getStatus() != Status.OK.getStatusCode()) {
                consumeAnyEntityAndClose(response);
            }
        }
    }

    /* Logbook external */

    @Override
    public RequestResponse<LogbookOperation> selectOperation(VitamContext vitamContext,
        JsonNode select)
        throws VitamClientException {
        Response response = null;
        try {
            MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
            headers.putAll(vitamContext.getHeaders());
            response = performRequest(HttpMethod.GET, LOGBOOK_OPERATIONS_URL, headers, select,
                MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE, false);
            return RequestResponse.parseFromResponse(response, LogbookOperation.class);

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
    public RequestResponse<LogbookOperation> selectOperationbyId(VitamContext vitamContext,
        String processId, JsonNode select)
        throws VitamClientException {

        Response response = null;
        MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putAll(vitamContext.getHeaders());
        try {
            response = performRequest(HttpMethod.GET, LOGBOOK_OPERATIONS_URL + "/" + processId, headers,
                select, MediaType.APPLICATION_JSON_TYPE,
                MediaType.APPLICATION_JSON_TYPE, false);
            return RequestResponse.parseFromResponse(response, LogbookOperation.class);

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
    public RequestResponse<LogbookLifecycle> selectUnitLifeCycleById(VitamContext vitamContext, String idUnit,
        JsonNode select)
        throws VitamClientException {
        Response response = null;
        ParametersChecker.checkParameter(BLANK_UNIT_ID, idUnit);
        MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putAll(vitamContext.getHeaders());
        try {
            response =
                performRequest(HttpMethod.GET, LOGBOOK_UNIT_LIFECYCLE_URL + "/" + idUnit, headers,
                    select, MediaType.APPLICATION_JSON_TYPE,
                    MediaType.APPLICATION_JSON_TYPE, false);
            return RequestResponse.parseFromResponse(response, LogbookLifecycle.class);

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
    public RequestResponse<LogbookLifecycle> selectObjectGroupLifeCycleById(
        VitamContext vitamContext, String idObject, JsonNode select)
        throws VitamClientException {
        Response response = null;
        ParametersChecker.checkParameter(BLANK_OBJECT_GROUP_ID, idObject);
        MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putAll(vitamContext.getHeaders());
        try {
            response = performRequest(HttpMethod.GET, LOGBOOK_OBJECT_LIFECYCLE_URL + "/" + idObject,
                headers,
                select, MediaType.APPLICATION_JSON_TYPE,
                MediaType.APPLICATION_JSON_TYPE, false);

            return RequestResponse.parseFromResponse(response, LogbookLifecycle.class);

        } catch (IllegalStateException e) {
            LOGGER.error("Could not parse server response ", e);
            throw createExceptionFromResponse(response);
        } catch (VitamClientInternalException e) {
            LOGGER.error("VitamClientInternalException: ", e);
            throw new VitamClientException(e);
        }
    }

    @Override
    public Response getUnitByIdWithXMLFormat(VitamContext vitamContext, JsonNode queryDsl,
        String idUnit)
        throws AccessExternalClientServerException {

        ParametersChecker.checkParameter(BLANK_DSL, queryDsl);
        ParametersChecker.checkParameter(BLANK_UNIT_ID, idUnit);

        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putAll(vitamContext.getHeaders());

        Response response = null;
        try {
            response = performRequest(HttpMethod.GET, "units/" + idUnit, headers, queryDsl,
                MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_XML_TYPE);
            return response;
        } catch (VitamClientInternalException e) {
            consumeAnyEntityAndClose(response);
            LOGGER.error("Error while getUnitByIdWithXMLFormat ", e);
            throw new AccessExternalClientServerException(e);
        }
    }

    @Override
    public Response getObjectGroupByIdWithXMLFormat(VitamContext vitamContext,
        JsonNode queryDsl, String idUnit)
        throws AccessExternalClientServerException {
        ParametersChecker.checkParameter(BLANK_DSL, queryDsl);
        ParametersChecker.checkParameter(BLANK_UNIT_ID, idUnit);
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.putAll(vitamContext.getHeaders());
        Response response = null;
        try {
            response = performRequest(HttpMethod.GET, "units/" + idUnit + "/object", headers, queryDsl,
                MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_XML_TYPE);
            return response;
        } catch (VitamClientInternalException e) {
            consumeAnyEntityAndClose(response);
            LOGGER.error("Error while getObjectGroupByIdWithXMLFormat ", e);
            throw new AccessExternalClientServerException(e);
        }
    }
}
