package fr.gouv.vitam.access.external.client;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.access.external.api.AccessExtAPI;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientNotFoundException;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientServerException;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.client.DefaultClient;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.AccessUnauthorizedException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.NoWritingPermissionException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.logbook.common.client.ErrorMessage;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;

/**
 * Rest client implementation for Access External
 */
class AccessExternalClientRest extends DefaultClient implements AccessExternalClient {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AccessExternalClientRest.class);

    private static final String REQUEST_PRECONDITION_FAILED = "Request precondition failed";
    private static final String INVALID_PARSE_OPERATION = "Invalid Parse Operation";
    private static final String NOT_FOUND_EXCEPTION = "Not Found Exception";
    private static final String NO_WRITING_PERMISSION = "No Writing Permission";
    private static final String UNAUTHORIZED = "Unauthorized";
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
    private static final String LOGBOOK_CHECK = AccessExtAPI.TRACEABILITY_API + "/check";
    private static final Select emptySelectQuery = new Select();

    AccessExternalClientRest(AccessExternalClientFactory factory) {
        super(factory);
    }

    @Override
    public RequestResponse selectUnits(JsonNode selectQuery, Integer tenantId, String contractName)
        throws InvalidParseOperationException, AccessExternalClientServerException,
        AccessExternalClientNotFoundException, AccessUnauthorizedException {
        Response response = null;

        SanityChecker.checkJsonAll(selectQuery);
        if (selectQuery == null || selectQuery.size() == 0) {
            throw new IllegalArgumentException(BLANK_DSL);
        }

        MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(GlobalDataRest.X_TENANT_ID, tenantId);
        headers.add(GlobalDataRest.X_ACCESS_CONTRAT_ID, contractName);
        try {
            response = performRequest(HttpMethod.GET, "/units", headers,
                selectQuery, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE, false);

            RequestResponse requestResponse = RequestResponse.parseFromResponse(response);
            if (!requestResponse.isOk()) {
                return requestResponse;
            } else {
                final VitamError vitamError =  new VitamError(VitamCode.ACCESS_EXTERNAL_SELECT_UNITS_ERROR.getItem())
                    .setMessage(VitamCode.ACCESS_EXTERNAL_SELECT_UNITS_ERROR.getMessage())
                    .setState(StatusCode.KO.name())
                    .setContext("AccessExternalModule")
                    .setDescription(VitamCode.ACCESS_EXTERNAL_SELECT_UNITS_ERROR.getMessage());

                if (response.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                    return  vitamError.setHttpCode(Status.UNAUTHORIZED.getStatusCode())
                        .setDescription(VitamCode.ACCESS_EXTERNAL_SELECT_UNITS_ERROR.getMessage() + " Cause : " +
                            Status.UNAUTHORIZED.getReasonPhrase());
                } else if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
                    return  vitamError.setHttpCode(Status.NOT_FOUND.getStatusCode())
                        .setDescription(VitamCode.ACCESS_EXTERNAL_SELECT_UNITS_ERROR.getMessage() + " Cause : " +
                            Status.NOT_FOUND.getReasonPhrase());
                } else if (response.getStatus() == Status.PRECONDITION_FAILED.getStatusCode()) {
                    return  vitamError.setHttpCode(Status.PRECONDITION_FAILED.getStatusCode())
                        .setDescription(VitamCode.ACCESS_EXTERNAL_SELECT_UNITS_ERROR.getMessage() + " Cause : " +
                            Status.PRECONDITION_FAILED.getReasonPhrase());
                } else {
                    return requestResponse;
                }

            }

        } catch (final VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new AccessExternalClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse selectUnitbyId(JsonNode selectQuery, String unitId, Integer tenantId, String contractName)
        throws InvalidParseOperationException, AccessExternalClientServerException,
        AccessExternalClientNotFoundException, AccessUnauthorizedException {
        Response response = null;
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(GlobalDataRest.X_HTTP_METHOD_OVERRIDE, HttpMethod.GET);
        headers.add(GlobalDataRest.X_TENANT_ID, tenantId);
        headers.add(GlobalDataRest.X_ACCESS_CONTRAT_ID, contractName);

        SanityChecker.checkJsonAll(selectQuery);
        if (selectQuery == null || selectQuery.size() == 0) {
            throw new IllegalArgumentException(BLANK_DSL);
        }
        ParametersChecker.checkParameter(BLANK_UNIT_ID, unitId);

        try {
            response = performRequest(HttpMethod.POST, UNITS + unitId, headers,
                selectQuery, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE, false);

            RequestResponse requestResponse = RequestResponse.parseFromResponse(response);
            if (!requestResponse.isOk()) {
                return requestResponse;
            } else {
                final VitamError vitamError =  new VitamError(VitamCode.ACCESS_EXTERNAL_SELECT_UNIT_BY_ID_ERROR.getItem())
                    .setMessage(VitamCode.ACCESS_EXTERNAL_SELECT_UNIT_BY_ID_ERROR.getMessage())
                    .setState(StatusCode.KO.name())
                    .setContext("AccessExternalModule")
                    .setDescription(VitamCode.ACCESS_EXTERNAL_SELECT_UNIT_BY_ID_ERROR.getMessage());

                if (response.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                    return  vitamError.setHttpCode(Status.UNAUTHORIZED.getStatusCode())
                        .setDescription(VitamCode.ACCESS_EXTERNAL_SELECT_UNIT_BY_ID_ERROR.getMessage() + " Cause : " +
                            Status.UNAUTHORIZED.getReasonPhrase());
                } else if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
                    return  vitamError.setHttpCode(Status.NOT_FOUND.getStatusCode())
                        .setDescription(VitamCode.ACCESS_EXTERNAL_SELECT_UNIT_BY_ID_ERROR.getMessage() + " Cause : " +
                            Status.NOT_FOUND.getReasonPhrase());
                } else if (response.getStatus() == Status.BAD_REQUEST.getStatusCode()) {
                    return  vitamError.setHttpCode(Status.BAD_REQUEST.getStatusCode())
                        .setDescription(VitamCode.ACCESS_EXTERNAL_SELECT_UNIT_BY_ID_ERROR.getMessage() + " Cause : " +
                            Status.BAD_REQUEST.getReasonPhrase());
                } else {
                    return requestResponse;
                }
            }

        } catch (final VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new AccessExternalClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse updateUnitbyId(JsonNode updateQuery, String unitId, Integer tenantId, String contractName)
        throws InvalidParseOperationException, AccessExternalClientServerException,
        AccessExternalClientNotFoundException, NoWritingPermissionException, AccessUnauthorizedException {
        Response response = null;
        SanityChecker.checkJsonAll(updateQuery);
        if (updateQuery == null || updateQuery.size() == 0) {
            throw new IllegalArgumentException(BLANK_DSL);
        }
        ParametersChecker.checkParameter(BLANK_UNIT_ID, unitId);
        MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(GlobalDataRest.X_TENANT_ID, tenantId);
        headers.add(GlobalDataRest.X_ACCESS_CONTRAT_ID, contractName);

        try {
            response = performRequest(HttpMethod.PUT, UNITS + unitId, headers,
                updateQuery, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE, false);

            RequestResponse requestResponse = RequestResponse.parseFromResponse(response);
            if (!requestResponse.isOk()) {
                return requestResponse;
            } else {
                final VitamError vitamError =  new VitamError(VitamCode.ACCESS_EXTERNAL_UPDATE_UNIT_BY_ID_ERROR.getItem())
                    .setMessage(VitamCode.ACCESS_EXTERNAL_UPDATE_UNIT_BY_ID_ERROR.getMessage())
                    .setState(StatusCode.KO.name())
                    .setContext("AccessExternalModule")
                    .setDescription(VitamCode.ACCESS_EXTERNAL_UPDATE_UNIT_BY_ID_ERROR.getMessage());

                if (response.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                    return  vitamError.setHttpCode(Status.UNAUTHORIZED.getStatusCode())
                        .setDescription(VitamCode.ACCESS_EXTERNAL_UPDATE_UNIT_BY_ID_ERROR.getMessage() + " Cause : " +
                            Status.UNAUTHORIZED.getReasonPhrase());
                } else if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
                    return  vitamError.setHttpCode(Status.NOT_FOUND.getStatusCode())
                        .setDescription(VitamCode.ACCESS_EXTERNAL_UPDATE_UNIT_BY_ID_ERROR.getMessage() + " Cause : " +
                            Status.NOT_FOUND.getReasonPhrase());
                } else if (response.getStatus() == Status.BAD_REQUEST.getStatusCode()) {
                    return  vitamError.setHttpCode(Status.BAD_REQUEST.getStatusCode())
                        .setDescription(VitamCode.ACCESS_EXTERNAL_UPDATE_UNIT_BY_ID_ERROR.getMessage() + " Cause : " +
                            Status.BAD_REQUEST.getReasonPhrase());
                } else if (response.getStatus() == Status.METHOD_NOT_ALLOWED.getStatusCode()) {
                    return  vitamError.setHttpCode(Status.METHOD_NOT_ALLOWED.getStatusCode())
                        .setDescription(VitamCode.ACCESS_EXTERNAL_UPDATE_UNIT_BY_ID_ERROR.getMessage() + " Cause : " +
                            Status.METHOD_NOT_ALLOWED.getReasonPhrase());
                } else {
                    return requestResponse;
                }
            }

        } catch (final VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new AccessExternalClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }

    }

    @Override
    public RequestResponse selectObjectById(JsonNode selectObjectQuery, String unitId, Integer tenantId,
        String contractName)
        throws InvalidParseOperationException, AccessExternalClientServerException,
        AccessExternalClientNotFoundException, AccessUnauthorizedException {
        SanityChecker.checkJsonAll(selectObjectQuery);
        if (selectObjectQuery == null || selectObjectQuery.size() == 0) {
            throw new IllegalArgumentException(BLANK_DSL);
        }
        ParametersChecker.checkParameter(BLANK_OBJECT_ID, unitId);

        Response response = null;
        MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(GlobalDataRest.X_TENANT_ID, tenantId);
        headers.add(GlobalDataRest.X_ACCESS_CONTRAT_ID, contractName);
        try {
            response = performRequest(HttpMethod.GET, "/units/" + unitId + "/object", headers,
                selectObjectQuery, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE, false);

            final Status status = Status.fromStatusCode(response.getStatus());

            RequestResponse requestResponse = RequestResponse.parseFromResponse(response);
            if (!requestResponse.isOk()) {
                return requestResponse;
            } else {
                final VitamError vitamError = new VitamError(VitamCode.ACCESS_EXTERNAL_SELECT_OBJECT_BY_ID_ERROR.getItem())
                    .setMessage(VitamCode.ACCESS_EXTERNAL_SELECT_OBJECT_BY_ID_ERROR.getMessage())
                    .setState(StatusCode.KO.name())
                    .setContext("AccessExternalModule")
                    .setDescription(VitamCode.ACCESS_EXTERNAL_SELECT_OBJECT_BY_ID_ERROR.getMessage());

                if (response.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                    LOGGER.error("Internal Server Error" + " : " + status.getReasonPhrase());
                    return  vitamError.setHttpCode(Status.UNAUTHORIZED.getStatusCode())
                        .setDescription(VitamCode.ACCESS_EXTERNAL_SELECT_OBJECT_BY_ID_ERROR.getMessage() + " Cause : " +
                            Status.UNAUTHORIZED.getReasonPhrase());
                } else if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
                    return  vitamError.setHttpCode(Status.NOT_FOUND.getStatusCode())
                        .setDescription(VitamCode.ACCESS_EXTERNAL_SELECT_OBJECT_BY_ID_ERROR.getMessage() + " Cause : " +
                            Status.NOT_FOUND.getReasonPhrase());
                } else if (response.getStatus() == Status.BAD_REQUEST.getStatusCode()) {
                    return  vitamError.setHttpCode(Status.BAD_REQUEST.getStatusCode())
                        .setDescription(VitamCode.ACCESS_EXTERNAL_SELECT_OBJECT_BY_ID_ERROR.getMessage() + " Cause : " +
                            Status.BAD_REQUEST.getReasonPhrase());
                } else if (response.getStatus() == Status.PRECONDITION_FAILED.getStatusCode()) {
                    return  vitamError.setHttpCode(Status.PRECONDITION_FAILED.getStatusCode())
                        .setDescription(VitamCode.ACCESS_EXTERNAL_SELECT_OBJECT_BY_ID_ERROR.getMessage() + " Cause : " +
                            Status.PRECONDITION_FAILED.getReasonPhrase());
                } else {
                    return requestResponse;
                }
            }
        } catch (final VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new AccessExternalClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }


    @Override
    public Response getUnitObject(JsonNode selectObjectQuery, String unitId, String usage, int version,
        Integer tenantId, String contractName)
        throws InvalidParseOperationException, AccessExternalClientServerException,
        AccessExternalClientNotFoundException {
        SanityChecker.checkJsonAll(selectObjectQuery);
        if (selectObjectQuery == null || selectObjectQuery.size() == 0) {
            throw new IllegalArgumentException(BLANK_DSL);
        }
        ParametersChecker.checkParameter(BLANK_OBJECT_ID, unitId);
        ParametersChecker.checkParameter(BLANK_USAGE, usage);
        ParametersChecker.checkParameter(BLANK_VERSION, version);

        Response response = null;
        MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(GlobalDataRest.X_TENANT_ID, tenantId);
        headers.add(GlobalDataRest.X_ACCESS_CONTRAT_ID, contractName);
        headers.add(GlobalDataRest.X_QUALIFIER, usage);
        headers.add(GlobalDataRest.X_VERSION, version);
        try {
            response = performRequest(HttpMethod.GET, "/units/" + unitId + "/object", headers,
                selectObjectQuery, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_OCTET_STREAM_TYPE, false);

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
        } catch (final VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new AccessExternalClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            if (response != null && response.getStatus() != Status.OK.getStatusCode()) {
                consumeAnyEntityAndClose(response);
            }
        }
    }

    @Override
    public Response getObject(JsonNode selectObjectQuery, String objectId, String usage, int version, Integer tenantId,
        String contractName)
        throws InvalidParseOperationException, AccessExternalClientServerException,
        AccessExternalClientNotFoundException, AccessUnauthorizedException {
        SanityChecker.checkJsonAll(selectObjectQuery);
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
        headers.add(GlobalDataRest.X_TENANT_ID, tenantId);
        headers.add(GlobalDataRest.X_ACCESS_CONTRAT_ID, contractName);


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
    public RequestResponse selectOperation(JsonNode select, Integer tenantId, String contractName)
        throws LogbookClientException, InvalidParseOperationException, AccessUnauthorizedException {
        Response response = null;
        try {
            MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
            headers.add(GlobalDataRest.X_TENANT_ID, tenantId);
            headers.add(GlobalDataRest.X_ACCESS_CONTRAT_ID, contractName);
            response = performRequest(HttpMethod.GET, LOGBOOK_OPERATIONS_URL, headers,
                select, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE, false);

            RequestResponse requestResponse = RequestResponse.parseFromResponse(response);
            if (!requestResponse.isOk()) {
                return requestResponse;
            } else {
                final VitamError vitamError = new VitamError(VitamCode.ACCESS_EXTERNAL_SELECT_OPERATION_ERROR.getItem())
                    .setMessage(VitamCode.ACCESS_EXTERNAL_SELECT_OPERATION_ERROR.getMessage())
                    .setState(StatusCode.KO.name())
                    .setContext("AccessExternalModule")
                    .setDescription(VitamCode.ACCESS_EXTERNAL_SELECT_OPERATION_ERROR.getMessage());

                if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
                    LOGGER.error(ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
                    return  vitamError.setHttpCode(Status.NOT_FOUND.getStatusCode())
                        .setDescription(VitamCode.ACCESS_EXTERNAL_SELECT_OPERATION_ERROR.getMessage() + " Cause : " +
                            Status.NOT_FOUND.getReasonPhrase());
                } else if (response.getStatus() == Status.PRECONDITION_FAILED.getStatusCode()) {
                    LOGGER.error("Illegal Entry Parameter");
                    return  vitamError.setHttpCode(Status.PRECONDITION_FAILED.getStatusCode())
                        .setDescription(VitamCode.ACCESS_EXTERNAL_SELECT_OPERATION_ERROR.getMessage() + " Cause : " +
                            Status.PRECONDITION_FAILED.getReasonPhrase());
                } else if (response.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                    return  vitamError.setHttpCode(Status.UNAUTHORIZED.getStatusCode())
                        .setDescription(VitamCode.ACCESS_EXTERNAL_SELECT_OPERATION_ERROR.getMessage() + " Cause : " +
                            Status.UNAUTHORIZED.getReasonPhrase());
                } else {
                    return requestResponse;
                }
            }

        } catch (final VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse selectOperationbyId(String processId, Integer tenantId, String contractName)
        throws LogbookClientException, InvalidParseOperationException, AccessUnauthorizedException {
        Response response = null;
        MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(GlobalDataRest.X_TENANT_ID, tenantId);
        headers.add(GlobalDataRest.X_ACCESS_CONTRAT_ID, contractName);
        try {
            response = performRequest(HttpMethod.GET, LOGBOOK_OPERATIONS_URL + "/" + processId, headers,
                emptySelectQuery, MediaType.APPLICATION_JSON_TYPE,
                MediaType.APPLICATION_JSON_TYPE, false);

            RequestResponse requestResponse = RequestResponse.parseFromResponse(response);
            if (!requestResponse.isOk()) {
                return requestResponse;
            } else {
                final VitamError vitamError = new VitamError(VitamCode.ACCESS_EXTERNAL_SELECT_OPERATION_BY_ID_ERROR.getItem())
                    .setMessage(VitamCode.ACCESS_EXTERNAL_SELECT_OPERATION_BY_ID_ERROR.getMessage())
                    .setState(StatusCode.KO.name())
                    .setContext("AccessExternalModule")
                    .setDescription(VitamCode.ACCESS_EXTERNAL_SELECT_OPERATION_BY_ID_ERROR.getMessage());

                if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
                    LOGGER.error(ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
                    return  vitamError.setHttpCode(Status.NOT_FOUND.getStatusCode())
                        .setDescription(VitamCode.ACCESS_EXTERNAL_SELECT_OPERATION_BY_ID_ERROR.getMessage() + " Cause : " +
                            Status.NOT_FOUND.getReasonPhrase());
                } else if (response.getStatus() == Status.PRECONDITION_FAILED.getStatusCode()) {
                    LOGGER.error("Illegal Entry Parameter");
                    return  vitamError.setHttpCode(Status.PRECONDITION_FAILED.getStatusCode())
                        .setDescription(VitamCode.ACCESS_EXTERNAL_SELECT_OPERATION_BY_ID_ERROR.getMessage() + " Cause : " +
                            Status.PRECONDITION_FAILED.getReasonPhrase());
                } else if (response.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                    return  vitamError.setHttpCode(Status.UNAUTHORIZED.getStatusCode())
                        .setDescription(VitamCode.ACCESS_EXTERNAL_SELECT_OPERATION_BY_ID_ERROR.getMessage() + " Cause : " +
                            Status.UNAUTHORIZED.getReasonPhrase());
                } else {
                    return requestResponse;
                }
            }

        } catch (final VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse selectUnitLifeCycleById(String idUnit, Integer tenantId, String contractName)
        throws LogbookClientException, InvalidParseOperationException, AccessUnauthorizedException {
        Response response = null;
        MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(GlobalDataRest.X_TENANT_ID, tenantId);
        headers.add(GlobalDataRest.X_ACCESS_CONTRAT_ID, contractName);
        try {
            response =
                performRequest(HttpMethod.GET, LOGBOOK_UNIT_LIFECYCLE_URL + "/" + idUnit, headers,
                    emptySelectQuery, MediaType.APPLICATION_JSON_TYPE,
                    MediaType.APPLICATION_JSON_TYPE, false);

            RequestResponse requestResponse = RequestResponse.parseFromResponse(response);
            if (!requestResponse.isOk()) {
                return requestResponse;
            } else {
                final VitamError vitamError = new VitamError(VitamCode.ACCESS_EXTERNAL_SELECT_UNIT_LIFECYCLE_BY_ID_ERROR.getItem())
                    .setMessage(VitamCode.ACCESS_EXTERNAL_SELECT_OPERATION_BY_ID_ERROR.getMessage())
                    .setState(StatusCode.KO.name())
                    .setContext("AccessExternalModule")
                    .setDescription(VitamCode.ACCESS_EXTERNAL_SELECT_OPERATION_BY_ID_ERROR.getMessage());

                if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
                    LOGGER.error(ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
                    return  vitamError.setHttpCode(Status.NOT_FOUND.getStatusCode())
                        .setDescription(VitamCode.ACCESS_EXTERNAL_SELECT_UNIT_LIFECYCLE_BY_ID_ERROR.getMessage() + " Cause : " +
                            Status.NOT_FOUND.getReasonPhrase());
                } else if (response.getStatus() == Response.Status.PRECONDITION_FAILED.getStatusCode()) {
                    LOGGER.error("Illegal Entry Parameter");
                    return  vitamError.setHttpCode(Status.PRECONDITION_FAILED.getStatusCode())
                        .setDescription(VitamCode.ACCESS_EXTERNAL_SELECT_UNIT_LIFECYCLE_BY_ID_ERROR.getMessage() + " Cause : " +
                            Status.PRECONDITION_FAILED.getReasonPhrase());
                } else if (response.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                    return  vitamError.setHttpCode(Status.UNAUTHORIZED.getStatusCode())
                        .setDescription(VitamCode.ACCESS_EXTERNAL_SELECT_UNIT_LIFECYCLE_BY_ID_ERROR.getMessage() + " Cause : " +
                            Status.UNAUTHORIZED.getReasonPhrase());
                } else {
                    return requestResponse;
                }
            }

        } catch (final VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse selectUnitLifeCycle(JsonNode queryDsl, Integer tenantId, String contractName)
        throws LogbookClientException, InvalidParseOperationException, AccessUnauthorizedException {
        Response response = null;
        SanityChecker.checkJsonAll(queryDsl);
        MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(GlobalDataRest.X_TENANT_ID, tenantId);
        headers.add(GlobalDataRest.X_ACCESS_CONTRAT_ID, contractName);

        try {
            response = performRequest(HttpMethod.GET, LOGBOOK_UNIT_LIFECYCLE_URL, headers, queryDsl,
                MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE, false);

            RequestResponse requestResponse = RequestResponse.parseFromResponse(response);
            if (!requestResponse.isOk()) {
                return requestResponse;
            } else {
                final VitamError vitamError = new VitamError(VitamCode.ACCESS_EXTERNAL_SELECT_UNIT_LIFECYCLE_ERROR.getItem())
                    .setMessage(VitamCode.ACCESS_EXTERNAL_SELECT_UNIT_LIFECYCLE_ERROR.getMessage())
                    .setState(StatusCode.KO.name())
                    .setContext("AccessExternalModule")
                    .setDescription(VitamCode.ACCESS_EXTERNAL_SELECT_UNIT_LIFECYCLE_ERROR.getMessage());

                if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
                    LOGGER.error(ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
                    return  vitamError.setHttpCode(Status.NOT_FOUND.getStatusCode())
                        .setDescription(VitamCode.ACCESS_EXTERNAL_SELECT_UNIT_LIFECYCLE_ERROR.getMessage() + " Cause : " +
                            Status.NOT_FOUND.getReasonPhrase());
                } else if (response.getStatus() == Response.Status.PRECONDITION_FAILED.getStatusCode()) {
                    LOGGER.error("Illegal Entry Parameter");
                    return  vitamError.setHttpCode(Status.PRECONDITION_FAILED.getStatusCode())
                        .setDescription(VitamCode.ACCESS_EXTERNAL_SELECT_UNIT_LIFECYCLE_ERROR.getMessage() + " Cause : " +
                            Status.PRECONDITION_FAILED.getReasonPhrase());
                } else if (response.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                    return  vitamError.setHttpCode(Status.UNAUTHORIZED.getStatusCode())
                        .setDescription(VitamCode.ACCESS_EXTERNAL_SELECT_UNIT_LIFECYCLE_ERROR.getMessage() + " Cause : " +
                            Status.UNAUTHORIZED.getReasonPhrase());
                } else {
                    return requestResponse;
                }
            }

        } catch (final VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse selectObjectGroupLifeCycleById(String idObject, Integer tenantId, String contractName)
        throws LogbookClientException, InvalidParseOperationException, AccessUnauthorizedException {
        Response response = null;
        MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(GlobalDataRest.X_TENANT_ID, tenantId);
        headers.add(GlobalDataRest.X_ACCESS_CONTRAT_ID, contractName);
        try {
            response = performRequest(HttpMethod.GET, LOGBOOK_OBJECT_LIFECYCLE_URL + "/" + idObject,
                headers,
                emptySelectQuery, MediaType.APPLICATION_JSON_TYPE,
                MediaType.APPLICATION_JSON_TYPE, false);

            RequestResponse requestResponse = RequestResponse.parseFromResponse(response);
            if (!requestResponse.isOk()) {
                return requestResponse;
            } else {
                final VitamError vitamError = new VitamError(VitamCode.ACCESS_EXTERNAL_SELECT_OBJECT_GROUP_LIFECYCLE_BY_ID_ERROR.getItem())
                    .setMessage(VitamCode.ACCESS_EXTERNAL_SELECT_OBJECT_GROUP_LIFECYCLE_BY_ID_ERROR.getMessage())
                    .setState(StatusCode.KO.name())
                    .setContext("AccessExternalModule")
                    .setDescription(VitamCode.ACCESS_EXTERNAL_SELECT_OBJECT_GROUP_LIFECYCLE_BY_ID_ERROR.getMessage());

                if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
                    LOGGER.error(ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
                    return  vitamError.setHttpCode(Status.NOT_FOUND.getStatusCode())
                        .setDescription(VitamCode.ACCESS_EXTERNAL_SELECT_OBJECT_GROUP_LIFECYCLE_BY_ID_ERROR.getMessage() + " Cause : " +
                            Status.NOT_FOUND.getReasonPhrase());
                } else if (response.getStatus() == Response.Status.PRECONDITION_FAILED.getStatusCode()) {
                    LOGGER.error("Illegal Entry Parameter");
                    return  vitamError.setHttpCode(Status.PRECONDITION_FAILED.getStatusCode())
                        .setDescription(VitamCode.ACCESS_EXTERNAL_SELECT_OBJECT_GROUP_LIFECYCLE_BY_ID_ERROR.getMessage() + " Cause : " +
                            Status.PRECONDITION_FAILED.getReasonPhrase());
                } else if (response.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                    return  vitamError.setHttpCode(Status.UNAUTHORIZED.getStatusCode())
                        .setDescription(VitamCode.ACCESS_EXTERNAL_SELECT_OBJECT_GROUP_LIFECYCLE_BY_ID_ERROR.getMessage() + " Cause : " +
                            Status.UNAUTHORIZED.getReasonPhrase());
                } else {
                    return requestResponse;
                }
            }

        } catch (final VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }

    }

    @Override
    public RequestResponse getAccessionRegisterSummary(JsonNode query, Integer tenantId, String contractName)
        throws InvalidParseOperationException, AccessExternalClientServerException,
        AccessExternalClientNotFoundException, AccessUnauthorizedException {
        Response response = null;
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(GlobalDataRest.X_HTTP_METHOD_OVERRIDE, HttpMethod.GET);
        headers.add(GlobalDataRest.X_TENANT_ID, tenantId);
        headers.add(GlobalDataRest.X_ACCESS_CONTRAT_ID, contractName);

        try {
            response = performRequest(HttpMethod.POST, AccessExtAPI.ACCESSION_REGISTERS_API, headers,
                query, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE, false);

            RequestResponse requestResponse = RequestResponse.parseFromResponse(response);
            if (!requestResponse.isOk()) {
                return requestResponse;
            } else {
                final VitamError vitamError = new VitamError(VitamCode.ACCESS_EXTERNAL_GET_ACCESSION_REGISTER_SUMMARY_ERROR.getItem())
                    .setMessage(VitamCode.ACCESS_EXTERNAL_GET_ACCESSION_REGISTER_SUMMARY_ERROR.getMessage())
                    .setState(StatusCode.KO.name())
                    .setContext("AccessExternalModule")
                    .setDescription(VitamCode.ACCESS_EXTERNAL_GET_ACCESSION_REGISTER_SUMMARY_ERROR.getMessage());

                if (response.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                    return  vitamError.setHttpCode(Status.UNAUTHORIZED.getStatusCode())
                        .setDescription(VitamCode.ACCESS_EXTERNAL_GET_ACCESSION_REGISTER_SUMMARY_ERROR.getMessage() + " Cause : " +
                            Status.UNAUTHORIZED.getReasonPhrase());
                } else if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
                    return  vitamError.setHttpCode(Status.NOT_FOUND.getStatusCode())
                        .setDescription(VitamCode.ACCESS_EXTERNAL_GET_ACCESSION_REGISTER_SUMMARY_ERROR.getMessage() + " Cause : " +
                            Status.NOT_FOUND.getReasonPhrase());
                } else if (response.getStatus() == Status.PRECONDITION_FAILED.getStatusCode()) {
                    return  vitamError.setHttpCode(Status.PRECONDITION_FAILED.getStatusCode())
                        .setDescription(VitamCode.ACCESS_EXTERNAL_GET_ACCESSION_REGISTER_SUMMARY_ERROR.getMessage() + " Cause : " +
                            Status.PRECONDITION_FAILED.getReasonPhrase());
                } else {
                    return requestResponse;
                }
            }

        } catch (final VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new AccessExternalClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse getAccessionRegisterDetail(String id, JsonNode query, Integer tenantId, String contractName)
        throws InvalidParseOperationException, AccessExternalClientServerException,
        AccessExternalClientNotFoundException, AccessUnauthorizedException {
        Response response = null;
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(GlobalDataRest.X_HTTP_METHOD_OVERRIDE, HttpMethod.GET);
        headers.add(GlobalDataRest.X_TENANT_ID, tenantId);
        headers.add(GlobalDataRest.X_ACCESS_CONTRAT_ID, contractName);

        try {
            response = performRequest(HttpMethod.POST,
                AccessExtAPI.ACCESSION_REGISTERS_API + "/" + id + "/" +
                    AccessExtAPI.ACCESSION_REGISTERS_DETAIL,
                headers,
                query, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE, false);

            RequestResponse requestResponse = RequestResponse.parseFromResponse(response);
            if (!requestResponse.isOk()) {
                return requestResponse;
            } else {
                final VitamError vitamError = new VitamError(VitamCode.ACCESS_EXTERNAL_GET_ACCESSION_REGISTER_DETAIL_ERROR.getItem())
                    .setMessage(VitamCode.ACCESS_EXTERNAL_GET_ACCESSION_REGISTER_DETAIL_ERROR.getMessage())
                    .setState(StatusCode.KO.name())
                    .setContext("AccessExternalModule")
                    .setDescription(VitamCode.ACCESS_EXTERNAL_GET_ACCESSION_REGISTER_DETAIL_ERROR.getMessage());

                if (response.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                    return  vitamError.setHttpCode(Status.UNAUTHORIZED.getStatusCode())
                        .setDescription(VitamCode.ACCESS_EXTERNAL_GET_ACCESSION_REGISTER_DETAIL_ERROR.getMessage() + " Cause : " +
                            Status.UNAUTHORIZED.getReasonPhrase());
                } else if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
                    return  vitamError.setHttpCode(Status.NOT_FOUND.getStatusCode())
                        .setDescription(VitamCode.ACCESS_EXTERNAL_GET_ACCESSION_REGISTER_DETAIL_ERROR.getMessage() + " Cause : " +
                            Status.NOT_FOUND.getReasonPhrase());
                } else if (response.getStatus() == Status.PRECONDITION_FAILED.getStatusCode()) {
                    return  vitamError.setHttpCode(Status.PRECONDITION_FAILED.getStatusCode())
                        .setDescription(VitamCode.ACCESS_EXTERNAL_GET_ACCESSION_REGISTER_DETAIL_ERROR.getMessage() + " Cause : " +
                            Status.PRECONDITION_FAILED.getReasonPhrase());
                } else {
                    return requestResponse;
                }
            }
        } catch (final VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new AccessExternalClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse checkTraceabilityOperation(JsonNode query, Integer tenantId, String contractName)
        throws AccessExternalClientServerException, AccessUnauthorizedException {
        Response response = null;
        try {
            final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
            headers.add(GlobalDataRest.X_TENANT_ID, tenantId);
            headers.add(GlobalDataRest.X_ACCESS_CONTRAT_ID, contractName);

            response = performRequest(HttpMethod.POST, LOGBOOK_CHECK, headers, query, MediaType.APPLICATION_JSON_TYPE,
                MediaType.APPLICATION_JSON_TYPE);
            final Status status = Status.fromStatusCode(response.getStatus());

            RequestResponse requestResponse = RequestResponse.parseFromResponse(response);
            if (!requestResponse.isOk()) {
                return requestResponse;
            } else {
                final VitamError vitamError = new VitamError(VitamCode.ACCESS_EXTERNAL_CHECK_TRACEABILITY_OPERATION_ERROR.getItem())
                    .setMessage(VitamCode.ACCESS_EXTERNAL_CHECK_TRACEABILITY_OPERATION_ERROR.getMessage())
                    .setState(StatusCode.KO.name())
                    .setContext("AccessExternalModule")
                    .setDescription(VitamCode.ACCESS_EXTERNAL_CHECK_TRACEABILITY_OPERATION_ERROR.getMessage());

                switch (status) {
                    case OK:
                        return requestResponse;
                    case UNAUTHORIZED:
                        return  vitamError.setHttpCode(Status.UNAUTHORIZED.getStatusCode())
                            .setDescription(VitamCode.ACCESS_EXTERNAL_CHECK_TRACEABILITY_OPERATION_ERROR.getMessage() + " Cause : " +
                                Status.UNAUTHORIZED.getReasonPhrase());
                    default:
                        LOGGER
                            .error("checks operation tracebility is " + status.name() + ":" + status.getReasonPhrase());
                        return vitamError.setHttpCode(status.getStatusCode())
                            .setDescription(VitamCode.ACCESS_EXTERNAL_CHECK_TRACEABILITY_OPERATION_ERROR.getMessage() + " Cause : " +
                                status.getReasonPhrase());
                }
            }

        } catch (VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new AccessExternalClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public Response downloadTraceabilityOperationFile(String operationId, Integer tenantId, String contractName)
        throws AccessExternalClientServerException, AccessUnauthorizedException {
        Response response = null;
        try {
            final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
            headers.add(GlobalDataRest.X_TENANT_ID, tenantId);
            headers.add(GlobalDataRest.X_ACCESS_CONTRAT_ID, contractName);

            response = performRequest(HttpMethod.GET, AccessExtAPI.TRACEABILITY_API + "/" + operationId, headers, null,
                null, MediaType.APPLICATION_OCTET_STREAM_TYPE);

            final Status status = Status.fromStatusCode(response.getStatus());
            switch (status) {
                case OK:
                    return response;
                case UNAUTHORIZED:
                    throw new AccessUnauthorizedException(status.getReasonPhrase());
                default:
                    LOGGER.error("checks operation tracebility is " + status.name() + ":" + status.getReasonPhrase());
                    throw new AccessExternalClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
            }
        } catch (VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new AccessExternalClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            if (response != null && response.getStatus() != Status.OK.getStatusCode()) {
                consumeAnyEntityAndClose(response);
            }
        }
    }
}
