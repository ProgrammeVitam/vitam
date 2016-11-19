package fr.gouv.vitam.access.external.client;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import fr.gouv.vitam.common.thread.VitamThreadUtils;
import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.access.external.api.AccessCollections;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientNotFoundException;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientServerException;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.client2.DefaultClient;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.logbook.common.client.ErrorMessage;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;

/**
 * Rest client implementation for Access External
 */
class AccessExternalClientRest extends DefaultClient implements AccessExternalClient {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AccessExternalClientRest.class);

    private static final String REQUEST_PRECONDITION_FAILED = "Request precondition failed";
    private static final String INVALID_PARSE_OPERATION = "Invalid Parse Operation";
    private static final String NOT_FOUND_EXCEPTION = "Not Found Exception";
    private static final String UNAUTHORIZED = "Unauthorized";
    private static final String UNITS = "/units/";
    private static final String BLANK_DSL = "select DSL is blank";
    private static final String BLANK_UNIT_ID = "unit identifier should be filled";
    private static final String BLANK_OBJECT_ID = "object identifier should be filled";
    private static final String BLANK_OBJECT_GROUP_ID = "object identifier should be filled";
    private static final String BLANK_USAGE = "usage should be filled";
    private static final String BLANK_VERSION = "usage version should be filled";

    private static final int TENANT_ID = 0;

    private static final String LOGBOOK_OPERATIONS_URL = "/operations";
    private static final String LOGBOOK_UNIT_LIFECYCLE_URL = "/unitlifecycles";
    private static final String LOGBOOK_OBJECT_LIFECYCLE_URL = "/objectgrouplifecycles";
    private static final Select emptySelectQuery = new Select();

    AccessExternalClientRest(AccessExternalClientFactory factory) {
        super(factory);
    }

    // FIXME P0 remplacer partout les String query par JsonNode query OU proposer les 2 options
    @Override
    public JsonNode selectUnits(String selectQuery)
        throws InvalidParseOperationException, AccessExternalClientServerException, AccessExternalClientNotFoundException {
        Response response = null;
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(GlobalDataRest.X_HTTP_METHOD_OVERRIDE, HttpMethod.GET);

        if (StringUtils.isBlank(selectQuery)) {
            throw new IllegalArgumentException(BLANK_DSL);
        }

        try {
            response = performRequest(HttpMethod.POST, "/units", headers,
                selectQuery, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE, false);

            if (response.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                throw new AccessExternalClientServerException(UNAUTHORIZED);
            } else if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
                throw new AccessExternalClientNotFoundException(NOT_FOUND_EXCEPTION);
            } else if (response.getStatus() == Status.PRECONDITION_FAILED.getStatusCode()) {
                throw new InvalidParseOperationException(INVALID_PARSE_OPERATION);
            }
            return response.readEntity(JsonNode.class);

        } catch (VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new AccessExternalClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public JsonNode selectUnitbyId(String selectQuery, String unitId)
        throws InvalidParseOperationException, AccessExternalClientServerException, AccessExternalClientNotFoundException {
        Response response = null;
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(GlobalDataRest.X_HTTP_METHOD_OVERRIDE, HttpMethod.GET);

        if (StringUtils.isBlank(selectQuery)) {
            throw new IllegalArgumentException(BLANK_DSL);
        }
        if (StringUtils.isEmpty(unitId)) {
            throw new IllegalArgumentException(BLANK_UNIT_ID);
        }

        try {
            response = performRequest(HttpMethod.POST, UNITS + unitId, headers,
                selectQuery, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE, false);

            if (response.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                throw new AccessExternalClientServerException(UNAUTHORIZED);
            } else if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
                throw new AccessExternalClientNotFoundException(NOT_FOUND_EXCEPTION);
            } else if (response.getStatus() == Status.BAD_REQUEST.getStatusCode()) {
                throw new InvalidParseOperationException(INVALID_PARSE_OPERATION);
            }

            return response.readEntity(JsonNode.class);
        } catch (VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new AccessExternalClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public JsonNode updateUnitbyId(String updateQuery, String unitId)
        throws InvalidParseOperationException, AccessExternalClientServerException, AccessExternalClientNotFoundException {

        Response response = null;
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();

        if (StringUtils.isBlank(updateQuery)) {
            throw new IllegalArgumentException(BLANK_DSL);
        }
        if (StringUtils.isEmpty(unitId)) {
            throw new IllegalArgumentException(BLANK_UNIT_ID);
        }

        try {
            response = performRequest(HttpMethod.PUT, UNITS + unitId, headers,
                updateQuery, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE, false);

            if (response.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                throw new AccessExternalClientServerException(UNAUTHORIZED);
            } else if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
                throw new AccessExternalClientNotFoundException(NOT_FOUND_EXCEPTION);
            } else if (response.getStatus() == Status.BAD_REQUEST.getStatusCode()) {
                throw new InvalidParseOperationException(INVALID_PARSE_OPERATION);
            }

            return response.readEntity(JsonNode.class);
        } catch (VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new AccessExternalClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }

    }

    @Override
    public JsonNode selectObjectById(String selectObjectQuery, String objectId)
        throws InvalidParseOperationException, AccessExternalClientServerException, AccessExternalClientNotFoundException {
        ParametersChecker.checkParameter(BLANK_DSL, selectObjectQuery);
        ParametersChecker.checkParameter(BLANK_OBJECT_ID, objectId);

        Response response = null;
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();

        try {
            response = performRequest(HttpMethod.GET, "/objects/" + objectId, headers,
                selectObjectQuery, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE, false);

            final Status status = Status.fromStatusCode(response.getStatus());
            if (response.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                LOGGER.error("Internal Server Error" + " : " + status.getReasonPhrase());
                throw new AccessExternalClientServerException(UNAUTHORIZED);
            } else if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
                throw new AccessExternalClientNotFoundException(status.getReasonPhrase());
            } else if (response.getStatus() == Status.BAD_REQUEST.getStatusCode()) {
                throw new InvalidParseOperationException(INVALID_PARSE_OPERATION);
            } else if (response.getStatus() == Status.PRECONDITION_FAILED.getStatusCode()) {
                throw new AccessExternalClientServerException(response.getStatusInfo().getReasonPhrase());
            }
            return response.readEntity(JsonNode.class);
        } catch (VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new AccessExternalClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            if (response == null || response.getStatus() != Status.OK.getStatusCode()) {
                consumeAnyEntityAndClose(response);
            }
        }
    }

    @Override
    public Response getObject(String selectObjectQuery, String objectId, String usage, int version)
        throws InvalidParseOperationException, AccessExternalClientServerException, AccessExternalClientNotFoundException {
        ParametersChecker.checkParameter(BLANK_DSL, selectObjectQuery);
        ParametersChecker.checkParameter(BLANK_OBJECT_GROUP_ID, objectId);
        ParametersChecker.checkParameter(BLANK_USAGE, usage);
        ParametersChecker.checkParameter(BLANK_VERSION, version);

        Response response = null;
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(GlobalDataRest.X_HTTP_METHOD_OVERRIDE, HttpMethod.GET);
        headers.add(GlobalDataRest.X_TENANT_ID, TENANT_ID);
        headers.add(GlobalDataRest.X_QUALIFIER, usage);
        headers.add(GlobalDataRest.X_VERSION, version);

        try {
            response = performRequest(HttpMethod.POST, UNITS + objectId + "/object", headers,
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
            }

            return response;
        } catch (VitamClientInternalException e) {
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
    public JsonNode selectOperation(String select) throws LogbookClientException, InvalidParseOperationException {
        Response response = null;
        try {
            final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
            headers.add(GlobalDataRest.X_HTTP_METHOD_OVERRIDE, HttpMethod.GET);
            response = performRequest(HttpMethod.POST, LOGBOOK_OPERATIONS_URL, headers,
                select, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE, false);

            if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
                LOGGER.error(ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
                throw new LogbookClientNotFoundException(ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
            } else if (response.getStatus() == Status.PRECONDITION_FAILED.getStatusCode()) {
                LOGGER.error("Illegal Entry Parameter");
                throw new LogbookClientException(REQUEST_PRECONDITION_FAILED);
            }

            return JsonHandler.getFromString(response.readEntity(String.class));
        } catch (VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public JsonNode selectOperationbyId(String processId)
        throws LogbookClientException, InvalidParseOperationException {
        Response response = null;
        try {
            final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
            headers.add(GlobalDataRest.X_HTTP_METHOD_OVERRIDE, HttpMethod.GET);
            response = performRequest(HttpMethod.POST, LOGBOOK_OPERATIONS_URL + "/" + processId, headers,
                emptySelectQuery, MediaType.APPLICATION_JSON_TYPE,
                MediaType.APPLICATION_JSON_TYPE, false);

            if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
                LOGGER.error(ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
                throw new LogbookClientNotFoundException(ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
            } else if (response.getStatus() == Status.PRECONDITION_FAILED.getStatusCode()) {
                LOGGER.error("Illegal Entry Parameter");
                throw new LogbookClientException(REQUEST_PRECONDITION_FAILED);
            }

            return JsonHandler.getFromString(response.readEntity(String.class));
        } catch (VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public JsonNode selectUnitLifeCycleById(String idUnit)
        throws LogbookClientException, InvalidParseOperationException {
        Response response = null;
        try {
            response = performRequest(HttpMethod.GET, LOGBOOK_UNIT_LIFECYCLE_URL + "/" + idUnit, new MultivaluedHashMap<>(),
                emptySelectQuery, MediaType.APPLICATION_JSON_TYPE,
                MediaType.APPLICATION_JSON_TYPE, false);

            if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
                LOGGER.error(ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
                throw new LogbookClientNotFoundException(ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
            } else if (response.getStatus() == Response.Status.PRECONDITION_FAILED.getStatusCode()) {
                LOGGER.error("Illegal Entry Parameter");
                throw new LogbookClientException(REQUEST_PRECONDITION_FAILED);
            }

            return JsonHandler.getFromString(response.readEntity(String.class));
        } catch (VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public JsonNode selectObjectGroupLifeCycleById(String idObject)
        throws LogbookClientException, InvalidParseOperationException {
        Response response = null;
        try {
            response = performRequest(HttpMethod.GET, LOGBOOK_OBJECT_LIFECYCLE_URL + "/" + idObject, new MultivaluedHashMap<>(),
                emptySelectQuery, MediaType.APPLICATION_JSON_TYPE,
                MediaType.APPLICATION_JSON_TYPE, false);

            if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
                LOGGER.error(ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
                throw new LogbookClientNotFoundException(ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
            } else if (response.getStatus() == Response.Status.PRECONDITION_FAILED.getStatusCode()) {
                LOGGER.error("Illegal Entry Parameter");
                throw new LogbookClientException(REQUEST_PRECONDITION_FAILED);
            }

            return JsonHandler.getFromString(response.readEntity(String.class));
        } catch (VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }

    }

    @Override
    public JsonNode getAccessionRegisterSummary(JsonNode query)
        throws InvalidParseOperationException, AccessExternalClientServerException,
        AccessExternalClientNotFoundException {
        Response response = null;
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(GlobalDataRest.X_HTTP_METHOD_OVERRIDE, HttpMethod.GET);

        try {
            response = performRequest(HttpMethod.POST, AccessCollections.ACCESSION_REGISTER.getName(), headers,
                query, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE, false);

            if (response.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                throw new AccessExternalClientServerException(UNAUTHORIZED);
            } else if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
                throw new AccessExternalClientNotFoundException(NOT_FOUND_EXCEPTION);
            } else if (response.getStatus() == Status.PRECONDITION_FAILED.getStatusCode()) {
                throw new InvalidParseOperationException(INVALID_PARSE_OPERATION);
            }
            return response.readEntity(JsonNode.class);

        } catch (VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new AccessExternalClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public JsonNode getAccessionRegisterDetail(String id, JsonNode query)
        throws InvalidParseOperationException, AccessExternalClientServerException,
        AccessExternalClientNotFoundException {
        Response response = null;
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(GlobalDataRest.X_HTTP_METHOD_OVERRIDE, HttpMethod.GET);

        try {
            response = performRequest(HttpMethod.POST, 
                AccessCollections.ACCESSION_REGISTER.getName() + "/" + id + "/" + AccessCollections.ACCESSION_REGISTER_DETAIL.getName(), headers,
                query, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE, false);

            if (response.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                throw new AccessExternalClientServerException(UNAUTHORIZED);
            } else if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
                throw new AccessExternalClientNotFoundException(NOT_FOUND_EXCEPTION);
            } else if (response.getStatus() == Status.PRECONDITION_FAILED.getStatusCode()) {
                throw new InvalidParseOperationException(INVALID_PARSE_OPERATION);
            }
            return response.readEntity(JsonNode.class);

        } catch (VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new AccessExternalClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

}
