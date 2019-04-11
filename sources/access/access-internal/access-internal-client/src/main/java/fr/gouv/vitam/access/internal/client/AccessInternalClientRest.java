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

package fr.gouv.vitam.access.internal.client;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.access.internal.common.exception.AccessInternalClientNotFoundException;
import fr.gouv.vitam.access.internal.common.exception.AccessInternalClientServerException;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.client.DefaultClient;
import fr.gouv.vitam.common.exception.AccessUnauthorizedException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.NoWritingPermissionException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.client.ErrorMessage;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;

/**
 * Access client <br>
 * <br>
 */

class AccessInternalClientRest extends DefaultClient implements AccessInternalClient {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AccessInternalClientRest.class);

    private static final String INVALID_PARSE_OPERATION = "Invalid Parse Operation";
    private static final String FORBIDDEN_OPERATION = "Empty query cannot be executed";
    private static final String REQUEST_PRECONDITION_FAILED = "Request precondition failed";
    private static final String NOT_FOUND_EXCEPTION = "Unit not found";
    private static final String ACCESS_CONTRACT_EXCEPTION = "Access by Contract Exception";
    private static final String NO_WRITING_PERMISSION = "No Writing Permission";
    private static final String BLANK_DSL = "select DSL is blank";
    private static final String BLANK_UNIT_ID = "unit identifier should be filled";
    private static final String BLANK_OBJECT_ID = "object identifier should be filled";
    private static final String BLANK_OBJECT_GROUP_ID = "object identifier should be filled";
    private static final String BLANK_USAGE = "usage should be filled";
    private static final String BLANK_VERSION = "usage version should be filled";
    private static final String BLANK_TRACEABILITY_OPERATION_ID = "traceability operation identifier should be filled";

    private static final String LOGBOOK_OPERATIONS_URL = "/operations";
    private static final String LOGBOOK_UNIT_LIFECYCLE_URL = "/unitlifecycles";
    private static final String LOGBOOK_OBJECT_LIFECYCLE_URL = "/objectgrouplifecycles";
    private static final String LOGBOOK_CHECK = "/traceability/check";

    private static final String CHECKS_OPERATION_TRACEABILITY_OK = "Checks operation traceability is OK";
    private static final String OBJECTS = "objects/";
    private static final String DIPEXPORT = "dipexport/";
    private static final String UNITS = "units/";
     private static final String ILLEGAL_ENTRY_PARAMETER = "Illegal Entry Parameter";

    AccessInternalClientRest(AccessInternalClientFactory factory) {
        super(factory);
    }

    @Override
    public RequestResponse<JsonNode> selectUnits(JsonNode selectQuery) throws InvalidParseOperationException,
        AccessInternalClientServerException, AccessInternalClientNotFoundException, AccessUnauthorizedException,
        fr.gouv.vitam.common.exception.BadRequestException {
        ParametersChecker.checkParameter(BLANK_DSL, selectQuery);
        VitamThreadUtils.getVitamSession().checkValidRequestId();

        Response response = null;
        LOGGER.debug("DEBUG: start selectUnits {}", selectQuery);
        try {
            response = performRequest(HttpMethod.GET, UNITS, null, selectQuery, MediaType.APPLICATION_JSON_TYPE,
                MediaType.APPLICATION_JSON_TYPE);
            if (response.getStatus() == Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
                throw new AccessInternalClientServerException(INTERNAL_SERVER_ERROR);// access-common
            } else if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) { // access-common
                throw new AccessInternalClientNotFoundException(NOT_FOUND_EXCEPTION);
            } else if (response.getStatus() == Status.BAD_REQUEST.getStatusCode()) {
                throw new InvalidParseOperationException(INVALID_PARSE_OPERATION);// common
            } else if (response.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                throw new AccessUnauthorizedException(ACCESS_CONTRACT_EXCEPTION);
            } else if (response.getStatus() == Status.FORBIDDEN.getStatusCode()) {
                throw new fr.gouv.vitam.common.exception.BadRequestException(FORBIDDEN_OPERATION);
            }
            LOGGER.debug("DEBUG: end selectUnits {}", response);
            return RequestResponse.parseFromResponse(response);
        } catch (final VitamClientInternalException e) {
            throw new AccessInternalClientServerException(INTERNAL_SERVER_ERROR, e); // access-common
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<JsonNode> selectUnitbyId(JsonNode selectQuery, String idUnit)
        throws InvalidParseOperationException,
        AccessInternalClientServerException, AccessInternalClientNotFoundException, AccessUnauthorizedException {

        ParametersChecker.checkParameter(BLANK_DSL, selectQuery);
        ParametersChecker.checkParameter(BLANK_UNIT_ID, idUnit);
        VitamThreadUtils.getVitamSession().checkValidRequestId();
        Response response = null;
        try {
            response = performRequest(HttpMethod.GET, UNITS + idUnit, null, selectQuery,
                MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);

            if (response.getStatus() == Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
                throw new AccessInternalClientServerException(INTERNAL_SERVER_ERROR); // access-common
            } else if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) { // access-common
                throw new AccessInternalClientNotFoundException(NOT_FOUND_EXCEPTION);
            } else if (response.getStatus() == Status.BAD_REQUEST.getStatusCode()) {
                throw new InvalidParseOperationException(INVALID_PARSE_OPERATION);// common
            } else if (response.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                throw new AccessUnauthorizedException(ACCESS_CONTRACT_EXCEPTION);
            }

            return RequestResponse.parseFromResponse(response);
        } catch (final VitamClientInternalException e) {
            throw new AccessInternalClientServerException(INTERNAL_SERVER_ERROR, e); // access-common
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<JsonNode> updateUnitbyId(JsonNode updateQuery, String unitId)
        throws InvalidParseOperationException,
        AccessInternalClientServerException, AccessInternalClientNotFoundException, NoWritingPermissionException,
        AccessUnauthorizedException {
        ParametersChecker.checkParameter(BLANK_DSL, updateQuery);
        ParametersChecker.checkParameter(BLANK_UNIT_ID, unitId);
        VitamThreadUtils.getVitamSession().checkValidRequestId();

        Response response = null;
        try {
            response = performRequest(HttpMethod.PUT, UNITS + unitId, null, updateQuery,
                MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);
            if (response.getStatus() == Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
                throw new AccessInternalClientServerException(INTERNAL_SERVER_ERROR); // access-common
            } else if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) { // access-common
                throw new AccessInternalClientNotFoundException(NOT_FOUND_EXCEPTION);
            } else if (response.getStatus() == Status.BAD_REQUEST.getStatusCode()) {
                try {
                    return RequestResponse.parseVitamError(response);
                } catch (final InvalidParseOperationException e) {
                    LOGGER.info("Cant parse error as vitamError, throw a new exception");
                }
                throw new InvalidParseOperationException(INVALID_PARSE_OPERATION);// common
            } else if (response.getStatus() == Status.METHOD_NOT_ALLOWED.getStatusCode()) {
                throw new NoWritingPermissionException(NO_WRITING_PERMISSION);
            } else if (response.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                throw new AccessUnauthorizedException(ACCESS_CONTRACT_EXCEPTION);
            }
            return RequestResponse.parseFromResponse(response);
        } catch (final VitamClientInternalException e) {
            throw new AccessInternalClientServerException(INTERNAL_SERVER_ERROR, e); // access-common
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<JsonNode> selectObjectbyId(JsonNode selectObjectQuery, String objectId)
        throws InvalidParseOperationException,
        AccessInternalClientServerException, AccessInternalClientNotFoundException, AccessUnauthorizedException {
        ParametersChecker.checkParameter(BLANK_DSL, selectObjectQuery);
        ParametersChecker.checkParameter(BLANK_OBJECT_ID, objectId);

        VitamThreadUtils.getVitamSession().checkValidRequestId();

        Response response = null;
        try {
            final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
            response = performRequest(HttpMethod.GET, OBJECTS + objectId, headers, selectObjectQuery,
                MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);
            final Response.Status status = Status.fromStatusCode(response.getStatus());
            if (response.getStatus() == Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
                LOGGER.error(INTERNAL_SERVER_ERROR + " : " + status.getReasonPhrase());
                throw new AccessInternalClientServerException(INTERNAL_SERVER_ERROR);
            } else if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
                throw new AccessInternalClientNotFoundException(status.getReasonPhrase());
            } else if (response.getStatus() == Status.BAD_REQUEST.getStatusCode()) {
                throw new InvalidParseOperationException(INVALID_PARSE_OPERATION);
            } else if (response.getStatus() == Status.PRECONDITION_FAILED.getStatusCode()) {
                throw new IllegalArgumentException(response.getStatusInfo().getReasonPhrase());
            } else if (response.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                throw new AccessUnauthorizedException(ACCESS_CONTRACT_EXCEPTION);
            }

            return RequestResponse.parseFromResponse(response);

        } catch (final VitamClientInternalException e) {
            throw new AccessInternalClientServerException(INTERNAL_SERVER_ERROR, e); // access-common
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public Response getObject(String objectGroupId, String usage, int version)
        throws InvalidParseOperationException, AccessInternalClientServerException,
        AccessInternalClientNotFoundException, AccessUnauthorizedException {
        ParametersChecker.checkParameter(BLANK_OBJECT_GROUP_ID, objectGroupId);
        ParametersChecker.checkParameter(BLANK_USAGE, usage);
        ParametersChecker.checkParameter(BLANK_VERSION, version);
        VitamThreadUtils.getVitamSession().checkValidRequestId();
        Response response = null;
        Status status = Status.BAD_REQUEST;
        try {
            final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
            headers.add(GlobalDataRest.X_QUALIFIER, usage);
            headers.add(GlobalDataRest.X_VERSION, version);
            response = performRequest(HttpMethod.GET, OBJECTS + objectGroupId, headers, null,
                MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_OCTET_STREAM_TYPE);
            status = Status.fromStatusCode(response.getStatus());
            switch (status) {
                case INTERNAL_SERVER_ERROR:
                    LOGGER.error(INTERNAL_SERVER_ERROR + " : " + status.getReasonPhrase());
                    throw new AccessInternalClientServerException(INTERNAL_SERVER_ERROR);
                case NOT_FOUND:
                    throw new AccessInternalClientNotFoundException(status.getReasonPhrase());
                case BAD_REQUEST:
                    throw new InvalidParseOperationException(INVALID_PARSE_OPERATION);
                case PRECONDITION_FAILED:
                    throw new IllegalArgumentException(response.getStatusInfo().getReasonPhrase());
                case OK:
                    break;
                case UNAUTHORIZED:
                    throw new AccessUnauthorizedException(ACCESS_CONTRACT_EXCEPTION);
                default:
                    LOGGER.error(INTERNAL_SERVER_ERROR + " : " + status.getReasonPhrase());
                    throw new AccessInternalClientServerException(
                        INTERNAL_SERVER_ERROR + " : " + status.getReasonPhrase());
            }
            return response;
        } catch (final VitamClientInternalException e) {
            throw new AccessInternalClientServerException(INTERNAL_SERVER_ERROR, e); // access-common
        } finally {
            if (status != Status.OK) {
                consumeAnyEntityAndClose(response);
            }
        }
    }

    /* Logbook internal */

    @Override
    public RequestResponse<JsonNode> selectOperation(JsonNode select)
        throws LogbookClientException, InvalidParseOperationException, AccessUnauthorizedException {
        Response response = null;
        try {
            response = performRequest(HttpMethod.GET, LOGBOOK_OPERATIONS_URL, null, select,
                MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE, false);

            if (response.getStatus() == Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
                throw new LogbookClientServerException(INTERNAL_SERVER_ERROR);
            } else if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
                LOGGER.error(ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
                throw new LogbookClientNotFoundException(ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
            } else if (response.getStatus() == Status.PRECONDITION_FAILED.getStatusCode()) {
                LOGGER.error(ILLEGAL_ENTRY_PARAMETER);
                throw new LogbookClientException(REQUEST_PRECONDITION_FAILED);
            } else if (response.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                throw new AccessUnauthorizedException(ACCESS_CONTRACT_EXCEPTION);
            }

            return RequestResponse.parseFromResponse(response);
        } catch (final VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<JsonNode> selectOperationById(String processId, JsonNode select)
        throws LogbookClientException, InvalidParseOperationException, AccessUnauthorizedException {
        Response response = null;
        try {
            final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
            response = performRequest(HttpMethod.GET, LOGBOOK_OPERATIONS_URL + "/" + processId, headers,
                select, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE, false);

            if (response.getStatus() == Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
                throw new LogbookClientServerException(INTERNAL_SERVER_ERROR);
            } else if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
                LOGGER.error(ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
                throw new LogbookClientNotFoundException(ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
            } else if (response.getStatus() == Status.PRECONDITION_FAILED.getStatusCode()) {
                LOGGER.error(ILLEGAL_ENTRY_PARAMETER);
                throw new LogbookClientException(REQUEST_PRECONDITION_FAILED);
            } else if (response.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                throw new AccessUnauthorizedException(ACCESS_CONTRACT_EXCEPTION);
            }

            return RequestResponse.parseFromResponse(response);
        } catch (final VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<JsonNode> selectUnitLifeCycleById(String idUnit, JsonNode queryDsl)
        throws LogbookClientException, InvalidParseOperationException, AccessUnauthorizedException {
        VitamThreadUtils.getVitamSession().checkValidRequestId();
        Response response = null;

        try {
            response = performRequest(HttpMethod.GET, LOGBOOK_UNIT_LIFECYCLE_URL + "/" + idUnit, null, queryDsl,
                MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE, false);

            if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
                LOGGER.error(ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
                throw new LogbookClientNotFoundException(ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
            } else if (response.getStatus() == Response.Status.PRECONDITION_FAILED.getStatusCode()) {
                LOGGER.error(ILLEGAL_ENTRY_PARAMETER);
                throw new LogbookClientException(REQUEST_PRECONDITION_FAILED);
            } else if (response.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                throw new AccessUnauthorizedException(ACCESS_CONTRACT_EXCEPTION);
            }

            return RequestResponse.parseFromResponse(response);
        } catch (final VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<JsonNode> selectUnitLifeCycle(JsonNode queryDsl)
        throws LogbookClientException, InvalidParseOperationException, AccessUnauthorizedException {
        VitamThreadUtils.getVitamSession().checkValidRequestId();
        Response response = null;

        try {
            response = performRequest(HttpMethod.GET, LOGBOOK_UNIT_LIFECYCLE_URL, null, queryDsl,
                MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE, false);

            if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
                LOGGER.error(ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
                throw new LogbookClientNotFoundException(ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
            } else if (response.getStatus() == Response.Status.PRECONDITION_FAILED.getStatusCode()) {
                LOGGER.error(ILLEGAL_ENTRY_PARAMETER);
                throw new LogbookClientException(REQUEST_PRECONDITION_FAILED);
            } else if (response.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                throw new AccessUnauthorizedException(ACCESS_CONTRACT_EXCEPTION);
            }

            return RequestResponse.parseFromResponse(response);
        } catch (final VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse<JsonNode> selectObjectGroupLifeCycleById(String idObject, JsonNode queryDsl)
        throws LogbookClientException, InvalidParseOperationException, AccessUnauthorizedException {
        VitamThreadUtils.getVitamSession().checkValidRequestId();

        Response response = null;

        try {
            response = performRequest(HttpMethod.GET, LOGBOOK_OBJECT_LIFECYCLE_URL + "/" + idObject, null,
                queryDsl, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE, false);

            if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
                LOGGER.error(ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
                throw new LogbookClientNotFoundException(ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
            } else if (response.getStatus() == Response.Status.PRECONDITION_FAILED.getStatusCode()) {
                LOGGER.error(ILLEGAL_ENTRY_PARAMETER);
                throw new LogbookClientException(REQUEST_PRECONDITION_FAILED);
            } else if (response.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                throw new AccessUnauthorizedException(ACCESS_CONTRACT_EXCEPTION);
            }

            return RequestResponse.parseFromResponse(response);
        } catch (final VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }

    }

    @Override
    public RequestResponse<JsonNode> checkTraceabilityOperation(JsonNode query)
        throws LogbookClientServerException, InvalidParseOperationException, AccessUnauthorizedException {
        Response response = null;
        try {
            response = performRequest(HttpMethod.POST, LOGBOOK_CHECK, null, query, MediaType.APPLICATION_JSON_TYPE,
                MediaType.APPLICATION_JSON_TYPE);
            final Status status = Status.fromStatusCode(response.getStatus());
            switch (status) {
                case OK:
                    LOGGER.info(CHECKS_OPERATION_TRACEABILITY_OK);
                    return RequestResponse.parseFromResponse(response);
                case UNAUTHORIZED:
                    throw new AccessUnauthorizedException(ACCESS_CONTRACT_EXCEPTION);
                case EXPECTATION_FAILED:
                case BAD_REQUEST:
                case NOT_FOUND:
                    LOGGER.error("checks operation tracebility is " + status.name() + ":" + status.getReasonPhrase() +
                        JsonHandler.prettyPrint(response.getEntity()));
                    return RequestResponse.parseFromResponse(response);
                default:
                    LOGGER.error("checks operation tracebility is " + status.name() + ":" + status.getReasonPhrase());
                    throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
            }
        } catch (VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }


    @Override
    public Response downloadTraceabilityFile(String operationId)
        throws AccessInternalClientServerException, AccessInternalClientNotFoundException,
        InvalidParseOperationException, AccessUnauthorizedException {

        ParametersChecker.checkParameter(BLANK_TRACEABILITY_OPERATION_ID, operationId);

        Response response = null;

        Status status = Status.BAD_REQUEST;
        try {
            response = performRequest(HttpMethod.GET, "traceability/" + operationId + "/content", null, null,
                null, MediaType.APPLICATION_OCTET_STREAM_TYPE);
            status = Status.fromStatusCode(response.getStatus());
            switch (status) {
                case INTERNAL_SERVER_ERROR:
                    LOGGER.error(INTERNAL_SERVER_ERROR + " : " + status.getReasonPhrase());
                    throw new AccessInternalClientServerException(INTERNAL_SERVER_ERROR);
                case NOT_FOUND:
                    throw new AccessInternalClientNotFoundException(status.getReasonPhrase());
                case BAD_REQUEST:
                    throw new InvalidParseOperationException(INVALID_PARSE_OPERATION);
                case OK:
                    break;
                case UNAUTHORIZED:
                    throw new AccessUnauthorizedException(ACCESS_CONTRACT_EXCEPTION);
                default:
                    LOGGER.error(INTERNAL_SERVER_ERROR + " : " + status.getReasonPhrase());
                    throw new AccessInternalClientServerException(
                        INTERNAL_SERVER_ERROR + " : " + status.getReasonPhrase());
            }
            return response;
        } catch (final VitamClientInternalException e) {
            throw new AccessInternalClientServerException(INTERNAL_SERVER_ERROR, e); // access-common
        } finally {
            if (status != Status.OK) {
                consumeAnyEntityAndClose(response);
            }
        }
    }

    @Override
    public RequestResponse<JsonNode> exportDIP(JsonNode queryDSL) throws AccessInternalClientServerException {
        ParametersChecker.checkParameter(BLANK_DSL, queryDSL);
        VitamThreadUtils.getVitamSession().checkValidRequestId();
        Response response = null;
        try {
            response = performRequest(HttpMethod.POST, DIPEXPORT, null, queryDSL,
                MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);
            return RequestResponse.parseFromResponse(response);
        } catch (final VitamClientInternalException e) {
            consumeAnyEntityAndClose(response);
            throw new AccessInternalClientServerException(INTERNAL_SERVER_ERROR, e); // access-common
        } catch (final Exception e) {
            consumeAnyEntityAndClose(response);
            throw e;
        }
    }

    @Override
    public Response findDIPByID(String id) throws AccessInternalClientServerException {
        ParametersChecker.checkParameter(BLANK_DSL, id);
        VitamThreadUtils.getVitamSession().checkValidRequestId();
        Response response = null;
        try {
            response =
                performRequest(HttpMethod.GET, DIPEXPORT + id + "/dip", null, MediaType.APPLICATION_OCTET_STREAM_TYPE);
            return response;
        } catch (final VitamClientInternalException e) {
            consumeAnyEntityAndClose(response);
            throw new AccessInternalClientServerException(INTERNAL_SERVER_ERROR, e); // access-common
        } catch (final Exception e) {
            consumeAnyEntityAndClose(response);
            throw e;
        }
    }

}
