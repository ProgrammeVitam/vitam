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
package fr.gouv.vitam.logbook.lifecycles.client;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.ServerIdentity;
import fr.gouv.vitam.common.client.DefaultClient;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.LifeCycleStatusCode;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.processing.DistributionType;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.logbook.common.client.ErrorMessage;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.model.LogbookLifeCycleObjectGroupModel;
import fr.gouv.vitam.logbook.common.model.LogbookLifeCycleUnitModel;
import fr.gouv.vitam.logbook.common.model.RawLifecycleByLastPersistedDateRequest;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleObjectGroupParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleParametersBulk;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleUnitParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
/**
 * LogbookLifeCyclesClient REST implementation
 */
class LogbookLifeCyclesClientRest extends DefaultClient implements LogbookLifeCyclesClient {

    private static final String REQUEST_PRECONDITION_FAILED = "Request precondition failed";
    private static final String ILLEGAL_ENTRY_PARAMETER = "Illegal Entry Parameter";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogbookLifeCyclesClientRest.class);
    // For Lifecycles under operations
    private static final String OPERATIONS_URL = "/operations";
    private static final String UNIT_LIFECYCLES_URL = "/unitlifecycles";
    private static final String OBJECT_GROUP_LIFECYCLES_URL = "/objectgrouplifecycles";
    private static final String OBJECT_GROUP_LIFECYCLES_RAW_BULK_URL = "/raw/objectgrouplifecycles/bulk";
    private static final String UNIT_LIFECYCLES_RAW_BULK_URL = "/raw/unitlifecycles/bulk";
    private static final String UNIT_LIFECYCLES_RAW_BY_ID_URL = "/raw/unitlifecycles/byid/";
    private static final String UNIT_LIFECYCLES_RAW_BY_IDS_URL = "/raw/unitlifecycles/byids";
    private static final String OBJECT_GROUP_LIFECYCLES_RAW_BY_ID_URL = "/raw/objectgrouplifecycles/byid/";
    private static final String OBJECT_GROUP_LIFECYCLES_RAW_BY_IDS_URL = "/raw/objectgrouplifecycles/byids";
    private static final String UNIT_LIFECYCLES_RAW_BY_LAST_PERSISTED_DATE_URL = "/raw/unitlifecycles/bylastpersisteddate";
    private static final String OBJECT_GROUP_LIFECYCLES_RAW_BY_LAST_PERSISTED_DATE_URL = "/raw/objectgrouplifecycles/bylastpersisteddate";
    private static final ServerIdentity SERVER_IDENTITY = ServerIdentity.getInstance();


    LogbookLifeCyclesClientRest(LogbookLifeCyclesClientFactory factory) {
        super(factory);
    }

    @Override
    public void create(LogbookLifeCycleParameters parameters)
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException {
        parameters.putParameterValue(LogbookParameterName.agentIdentifier,
            SERVER_IDENTITY.getJsonIdentity());
        parameters.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());
        ParameterHelper
            .checkNullOrEmptyParameters(parameters.getMapParameters(), parameters.getMandatoriesParameters());
        final String eip = parameters.getParameterValue(LogbookParameterName.eventIdentifierProcess);
        final String oid = parameters.getParameterValue(LogbookParameterName.objectIdentifier);
        Response response = null;
        try {
            response = performRequest(HttpMethod.POST, getServiceUrl(parameters, eip, oid), null,
                parameters, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);
            final Response.Status status = Response.Status.fromStatusCode(response.getStatus());
            switch (status) {
                case CREATED:
                    LOGGER.debug(eip + " " + Response.Status.CREATED.getReasonPhrase());
                    break;
                case CONFLICT:
                    LOGGER.error(eip + " " + ErrorMessage.LOGBOOK_ALREADY_EXIST.getMessage());
                    throw new LogbookClientAlreadyExistsException(ErrorMessage.LOGBOOK_ALREADY_EXIST.getMessage());
                case BAD_REQUEST:
                    LOGGER.error(eip + " " + ErrorMessage.LOGBOOK_MISSING_MANDATORY_PARAMETER.getMessage());
                    throw new LogbookClientBadRequestException(
                        ErrorMessage.LOGBOOK_MISSING_MANDATORY_PARAMETER.getMessage());
                default:
                    LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage() + ':' + status.getReasonPhrase());
                    throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
            }
        } catch (final VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }

    }

    private String getServiceUrl(LogbookLifeCycleParameters parameters, String eip, String oid) {
        String logBookLifeCycleUrl;
        if (parameters instanceof LogbookLifeCycleObjectGroupParameters) {
            logBookLifeCycleUrl = OBJECT_GROUP_LIFECYCLES_URL;
        } else if (parameters instanceof LogbookLifeCycleUnitParameters) {
            logBookLifeCycleUrl = UNIT_LIFECYCLES_URL;
        } else {
            throw new IllegalArgumentException("Parameters to be checked");
        }
        return "/operations/" + eip + logBookLifeCycleUrl + "/" + oid;
    }

    @Override
    public void update(LogbookLifeCycleParameters parameters)
        throws LogbookClientBadRequestException, LogbookClientNotFoundException, LogbookClientServerException {
        update(parameters, null);
    }

    @Override
    public void update(LogbookLifeCycleParameters parameters, LifeCycleStatusCode lifeCycleStatusCode)
        throws LogbookClientBadRequestException, LogbookClientNotFoundException, LogbookClientServerException {
        parameters.putParameterValue(LogbookParameterName.agentIdentifier,
            SERVER_IDENTITY.getJsonIdentity());
        if (parameters.getParameterValue(LogbookParameterName.eventDateTime) == null) {
            parameters.putParameterValue(LogbookParameterName.eventDateTime,
                LocalDateUtil.now().toString());
        }
        ParameterHelper
            .checkNullOrEmptyParameters(parameters.getMapParameters(), parameters.getMandatoriesParameters());
        final String eip = parameters.getParameterValue(LogbookParameterName.eventIdentifierProcess);
        final String oid = parameters.getParameterValue(LogbookParameterName.objectIdentifier);
        final String lid = parameters.getParameterValue(LogbookParameterName.lifeCycleIdentifier);

        // Add X-EVENT-STATUS header
        MultivaluedHashMap<String, Object> headers = null;
        if (lifeCycleStatusCode != null) {
            headers = new MultivaluedHashMap<>();
            headers.add(GlobalDataRest.X_EVENT_STATUS, lifeCycleStatusCode.toString());
        }

        Response response = null;
        try {
            response =
                performRequest(HttpMethod.PUT, getServiceUrl(parameters, eip, (lid != null) ? lid : oid), headers,
                    parameters, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);
            final Response.Status status = Response.Status.fromStatusCode(response.getStatus());
            switch (status) {
                case OK:
                    LOGGER.debug(eip + " " + Response.Status.OK.getReasonPhrase());
                    break;
                case NOT_FOUND:
                    LOGGER.error(eip + " " + ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
                    throw new LogbookClientNotFoundException(ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
                case BAD_REQUEST:
                    LOGGER.error(eip + " " + ErrorMessage.LOGBOOK_MISSING_MANDATORY_PARAMETER.getMessage());
                    throw new LogbookClientBadRequestException(
                        ErrorMessage.LOGBOOK_MISSING_MANDATORY_PARAMETER.getMessage());
                default:
                    LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage() + ':' + status.getReasonPhrase());
                    throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
            }
        } catch (final VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Deprecated
    @Override
    public void commit(LogbookLifeCycleParameters parameters)
        throws LogbookClientBadRequestException, LogbookClientNotFoundException, LogbookClientServerException {
        parameters.putParameterValue(LogbookParameterName.agentIdentifier,
            SERVER_IDENTITY.getJsonIdentity());
        parameters.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());
        ParameterHelper
            .checkNullOrEmptyParameters(parameters.getMapParameters(), parameters.getMandatoriesParameters());
        final String eip = parameters.getParameterValue(LogbookParameterName.eventIdentifierProcess);
        final String oid = parameters.getParameterValue(LogbookParameterName.objectIdentifier);
        Response response = null;
        try {
            response = performRequest(HttpMethod.PUT, getServiceUrl(parameters, eip, oid) + "/commit", null,
                MediaType.APPLICATION_JSON_TYPE);
            final Response.Status status = Response.Status.fromStatusCode(response.getStatus());
            switch (status) {
                case OK:
                    LOGGER.debug(oid + " " + Response.Status.OK.getReasonPhrase());
                    break;
                case NOT_FOUND:
                    LOGGER.error(oid + " " + ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
                    throw new LogbookClientNotFoundException(ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
                case BAD_REQUEST:
                    LOGGER.error(oid + " " + ErrorMessage.LOGBOOK_MISSING_MANDATORY_PARAMETER.getMessage());
                    throw new LogbookClientBadRequestException(
                        ErrorMessage.LOGBOOK_MISSING_MANDATORY_PARAMETER.getMessage());
                default:
                    LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage() + ':' + status.getReasonPhrase());
                    throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
            }
        } catch (final VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public void rollback(LogbookLifeCycleParameters parameters)
        throws LogbookClientBadRequestException, LogbookClientNotFoundException, LogbookClientServerException {
        parameters.putParameterValue(LogbookParameterName.agentIdentifier,
            SERVER_IDENTITY.getJsonIdentity());
        parameters.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());
        ParameterHelper
            .checkNullOrEmptyParameters(parameters.getMapParameters(), parameters.getMandatoriesParameters());
        final String eip = parameters.getParameterValue(LogbookParameterName.eventIdentifierProcess);
        final String oid = parameters.getParameterValue(LogbookParameterName.objectIdentifier);
        Response response = null;
        try {
            response = performRequest(HttpMethod.DELETE, getServiceUrl(parameters, eip, oid), null,
                MediaType.APPLICATION_JSON_TYPE);
            final Response.Status status = Response.Status.fromStatusCode(response.getStatus());
            switch (status) {
                case OK:
                    LOGGER.debug(oid + " " + Response.Status.OK.getReasonPhrase());
                    break;
                case NOT_FOUND:
                    LOGGER.error(oid + " " + ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
                    throw new LogbookClientNotFoundException(ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
                case BAD_REQUEST:
                    LOGGER.error(oid + " " + ErrorMessage.LOGBOOK_MISSING_MANDATORY_PARAMETER.getMessage());
                    throw new LogbookClientBadRequestException(
                        ErrorMessage.LOGBOOK_MISSING_MANDATORY_PARAMETER.getMessage());
                default:
                    LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage() + ':' + status.getReasonPhrase());
                    throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
            }
        } catch (final VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public JsonNode selectUnitLifeCycleById(String id, JsonNode queryDsl)
        throws LogbookClientException, InvalidParseOperationException {
        return selectUnitLifeCycleById(id, queryDsl, null);
    }

    @Override
    public JsonNode selectUnitLifeCycleById(String id, JsonNode queryDsl, LifeCycleStatusCode lifeCycleStatus)
        throws LogbookClientException, InvalidParseOperationException {
        Response response = null;
        try {
            MultivaluedHashMap<String, Object> headers = null;
            if (lifeCycleStatus != null) {
                headers = new MultivaluedHashMap<>();
                headers.add(GlobalDataRest.X_EVENT_STATUS, lifeCycleStatus.toString());
            }

            response = performRequest(HttpMethod.GET, UNIT_LIFECYCLES_URL + "/" + id, headers,
                queryDsl, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);

            if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
                LOGGER.error(ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
                throw new LogbookClientNotFoundException(ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
            } else if (response.getStatus() == Response.Status.PRECONDITION_FAILED.getStatusCode()) {
                LOGGER.error(ILLEGAL_ENTRY_PARAMETER);
                throw new LogbookClientException(REQUEST_PRECONDITION_FAILED);
            }
            return JsonHandler.getFromString(response.readEntity(String.class));
        } catch (final VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public JsonNode selectUnitLifeCycle(JsonNode queryDsl)
        throws LogbookClientException, InvalidParseOperationException {
        Response response = null;
        try {
            response = performRequest(HttpMethod.GET, UNIT_LIFECYCLES_URL, null,
                queryDsl, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);

            if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
                LOGGER.error(ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
                throw new LogbookClientNotFoundException(ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
            } else if (response.getStatus() == Response.Status.PRECONDITION_FAILED.getStatusCode()) {
                LOGGER.error(ILLEGAL_ENTRY_PARAMETER);
                throw new LogbookClientException(REQUEST_PRECONDITION_FAILED);
            }
            return JsonHandler.getFromString(response.readEntity(String.class));
        } catch (final VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public List<JsonNode> getRawUnitLifecyclesByLastPersistedDate(LocalDateTime startDate, LocalDateTime endDate,
        int limit) throws LogbookClientException, InvalidParseOperationException {

        return getRawLifecyclesByLastPersistedDate(UNIT_LIFECYCLES_RAW_BY_LAST_PERSISTED_DATE_URL,
            startDate, endDate, limit);
    }

    @Override
    public List<JsonNode> getRawObjectGroupLifecyclesByLastPersistedDate(LocalDateTime startDate,
        LocalDateTime endDate, int limit) throws LogbookClientException, InvalidParseOperationException {

        return getRawLifecyclesByLastPersistedDate(OBJECT_GROUP_LIFECYCLES_RAW_BY_LAST_PERSISTED_DATE_URL,
            startDate, endDate, limit);
    }

    private List<JsonNode> getRawLifecyclesByLastPersistedDate(String uri, LocalDateTime startDate,
        LocalDateTime endDate,
        int limit)
        throws InvalidParseOperationException, LogbookClientException {
        Response response = null;
        try {

            RawLifecycleByLastPersistedDateRequest
                request = new RawLifecycleByLastPersistedDateRequest(
                LocalDateUtil.getFormattedDateForMongo(startDate),
                LocalDateUtil.getFormattedDateForMongo(endDate),
                limit);
            JsonNode requestJsonNode = JsonHandler.toJsonNode(request);

            response = performRequest(HttpMethod.GET, uri, null,
                requestJsonNode, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);

            if (response.getStatus() == Status.PRECONDITION_FAILED.getStatusCode()) {
                LOGGER.error(ILLEGAL_ENTRY_PARAMETER);
                throw new LogbookClientException(REQUEST_PRECONDITION_FAILED);
            }

            RequestResponse<JsonNode> requestResponse = RequestResponse.parseFromResponse(response, JsonNode.class);
            if (!requestResponse.isOk()) {
                VitamError error = (VitamError) requestResponse;
                throw new LogbookClientException(error.getDescription());
            }

            return ((RequestResponseOK<JsonNode>) requestResponse).getResults();

        } catch (final VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public JsonNode selectObjectGroupLifeCycleById(String id, JsonNode queryDsl)
        throws LogbookClientException, InvalidParseOperationException {
        return selectObjectGroupLifeCycleById(id, queryDsl, null);
    }

    @Override
    public JsonNode selectObjectGroupLifeCycleById(String id, JsonNode queryDsl, LifeCycleStatusCode lifeCycleStatus)
        throws LogbookClientException, InvalidParseOperationException {
        Response response = null;
        try {

            MultivaluedHashMap<String, Object> headers = null;
            if (lifeCycleStatus != null) {
                headers = new MultivaluedHashMap<>();
                headers.add(GlobalDataRest.X_EVENT_STATUS, lifeCycleStatus.toString());
            }

            response = performRequest(HttpMethod.GET, OBJECT_GROUP_LIFECYCLES_URL + "/" + id,
                headers, queryDsl, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);

            if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
                LOGGER.error(ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
                throw new LogbookClientNotFoundException(ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
            } else if (response.getStatus() == Response.Status.PRECONDITION_FAILED.getStatusCode()) {
                LOGGER.error(ILLEGAL_ENTRY_PARAMETER);
                throw new LogbookClientException(REQUEST_PRECONDITION_FAILED);
            }
            return JsonHandler.getFromString(response.readEntity(String.class));
        } catch (final VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public JsonNode selectObjectGroupLifeCycle(JsonNode queryDsl)
        throws LogbookClientException, InvalidParseOperationException {
        Response response = null;
        try {
            response = performRequest(HttpMethod.GET, OBJECT_GROUP_LIFECYCLES_URL,
                null, queryDsl, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);
            if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
                LOGGER.error(ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
                throw new LogbookClientNotFoundException(ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
            } else if (response.getStatus() == Response.Status.PRECONDITION_FAILED.getStatusCode()) {
                LOGGER.error(ILLEGAL_ENTRY_PARAMETER);
                throw new LogbookClientException(REQUEST_PRECONDITION_FAILED);
            }
            return JsonHandler.getFromString(response.readEntity(String.class));
        } catch (final VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse objectGroupLifeCyclesByOperationIterator(String operationId,
        LifeCycleStatusCode lifeCycleStatus, JsonNode query)
        throws LogbookClientException, InvalidParseOperationException {
        Response response = null;
        try {
            MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
            if (lifeCycleStatus != null) {
                headers.add(GlobalDataRest.X_EVENT_STATUS, lifeCycleStatus.toString());
            }
            response = performRequest(HttpMethod.GET,
                OPERATIONS_URL + "/" + operationId + OBJECT_GROUP_LIFECYCLES_URL,
                headers, query, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);
            return RequestResponse.parseFromResponse(response, JsonNode.class);
        } catch (final IllegalArgumentException | VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.LOGBOOK_MISSING_MANDATORY_PARAMETER.getMessage(), e);
            throw new LogbookClientServerException(ErrorMessage.LOGBOOK_MISSING_MANDATORY_PARAMETER.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public RequestResponse unitLifeCyclesByOperationIterator(String operationId,
        LifeCycleStatusCode lifeCycleStatus, JsonNode query)
        throws LogbookClientException, InvalidParseOperationException {
        Response response = null;
        try {
            MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
            if (lifeCycleStatus != null) {
                headers.add(GlobalDataRest.X_EVENT_STATUS, lifeCycleStatus.toString());
            }
            response = performRequest(HttpMethod.GET, OPERATIONS_URL + "/" + operationId + UNIT_LIFECYCLES_URL,
                headers, query, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);
            return RequestResponse.parseFromResponse(response, JsonNode.class);
        } catch (final IllegalArgumentException | VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.LOGBOOK_MISSING_MANDATORY_PARAMETER.getMessage(), e);
            throw new LogbookClientServerException(ErrorMessage.LOGBOOK_MISSING_MANDATORY_PARAMETER.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public void bulkCreateUnit(String eventIdProc, Iterable<LogbookLifeCycleParameters> queue)
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException,
        LogbookClientServerException {
        bulkCreate(eventIdProc, queue, UNIT_LIFECYCLES_URL);
    }

    @Override
    public void bulkCreateObjectGroup(String eventIdProc, Iterable<LogbookLifeCycleParameters> queue)
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException,
        LogbookClientServerException {
        bulkCreate(eventIdProc, queue, OBJECT_GROUP_LIFECYCLES_URL);
    }

    private void bulkCreate(String eventIdProc, Iterable<LogbookLifeCycleParameters> queue, String uri)
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException,
        LogbookClientServerException {
        if (queue != null) {
            Response response = null;
            try {
                response = performRequest(HttpMethod.POST, OPERATIONS_URL + "/" + eventIdProc + uri, null,
                    queue, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);
                final Status status = Status.fromStatusCode(response.getStatus());
                switch (status) {
                    case CREATED:
                        LOGGER.debug(eventIdProc + " " + Response.Status.CREATED.getReasonPhrase());
                        break;
                    case CONFLICT:
                        LOGGER.error(eventIdProc + " " + ErrorMessage.LOGBOOK_ALREADY_EXIST.getMessage());
                        throw new LogbookClientAlreadyExistsException(ErrorMessage.LOGBOOK_ALREADY_EXIST.getMessage());
                    case BAD_REQUEST:
                        LOGGER.error(eventIdProc + " " + ErrorMessage.LOGBOOK_MISSING_MANDATORY_PARAMETER.getMessage());
                        throw new LogbookClientBadRequestException(
                            ErrorMessage.LOGBOOK_MISSING_MANDATORY_PARAMETER.getMessage());
                    default:
                        LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage() + ':' + status.getReasonPhrase());
                        throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
                }
            } catch (final VitamClientInternalException e) {
                LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
                throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            } finally {
                consumeAnyEntityAndClose(response);
            }
        } else {
            LOGGER.error(eventIdProc + " " + ErrorMessage.LOGBOOK_MISSING_MANDATORY_PARAMETER.getMessage());
            throw new LogbookClientBadRequestException(
                ErrorMessage.LOGBOOK_MISSING_MANDATORY_PARAMETER.getMessage());
        }
    }

    @Override
    public void bulkUpdateUnit(String eventIdProc, Iterable<LogbookLifeCycleParameters> queue)
        throws LogbookClientNotFoundException, LogbookClientBadRequestException, LogbookClientServerException {
        bulkUpdate(eventIdProc, queue, UNIT_LIFECYCLES_URL);
    }

    @Override
    public void bulkUpdateObjectGroup(String eventIdProc, Iterable<LogbookLifeCycleParameters> queue)
        throws LogbookClientNotFoundException, LogbookClientBadRequestException, LogbookClientServerException {
        bulkUpdate(eventIdProc, queue, OBJECT_GROUP_LIFECYCLES_URL);
    }

    private void bulkUpdate(String eventIdProc, Iterable<LogbookLifeCycleParameters> queue, String uri)
        throws LogbookClientNotFoundException, LogbookClientBadRequestException, LogbookClientServerException {
        if (queue != null) {
            Response response = null;
            try {
                response = performRequest(HttpMethod.PUT, OPERATIONS_URL + "/" + eventIdProc + uri, null,
                    queue, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);
                final Status status = Status.fromStatusCode(response.getStatus());
                switch (status) {
                    case OK:
                        LOGGER.debug(eventIdProc + " " + Response.Status.OK.getReasonPhrase());
                        break;
                    case NOT_FOUND:
                        LOGGER.error(eventIdProc + " " + ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
                        throw new LogbookClientNotFoundException(ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
                    case BAD_REQUEST:
                        LOGGER.error(eventIdProc + " " + ErrorMessage.LOGBOOK_MISSING_MANDATORY_PARAMETER.getMessage());
                        throw new LogbookClientBadRequestException(
                            ErrorMessage.LOGBOOK_MISSING_MANDATORY_PARAMETER.getMessage());
                    default:
                        LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage() + ':' + status.getReasonPhrase());
                        throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
                }
            } catch (final VitamClientInternalException e) {
                LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
                throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            } finally {
                consumeAnyEntityAndClose(response);
            }
        } else {
            LOGGER.error(eventIdProc + " " + ErrorMessage.LOGBOOK_MISSING_MANDATORY_PARAMETER.getMessage());
            throw new LogbookClientBadRequestException(
                ErrorMessage.LOGBOOK_MISSING_MANDATORY_PARAMETER.getMessage());
        }
    }

    @Override
    public void commitUnit(String operationId, String unitId)
        throws LogbookClientBadRequestException, LogbookClientNotFoundException, LogbookClientServerException {
        commitLifeCycle(operationId, unitId, UNIT_LIFECYCLES_URL);
    }

    @Override
    public void commitObjectGroup(String operationId, String objectGroupId)
        throws LogbookClientBadRequestException, LogbookClientNotFoundException, LogbookClientServerException {
        commitLifeCycle(operationId, objectGroupId, OBJECT_GROUP_LIFECYCLES_URL);
    }

    private void commitLifeCycle(String operationId, String idLc, String uri)
        throws LogbookClientBadRequestException, LogbookClientNotFoundException, LogbookClientServerException {

        Response response = null;
        String commitPath = OPERATIONS_URL + "/" + operationId + uri + "/" + idLc;
        // Add X-EVENT-STATUS header
        MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(GlobalDataRest.X_EVENT_STATUS, LifeCycleStatusCode.LIFE_CYCLE_COMMITTED.toString());

        try {
            // BIG HACK because we use the same method to update and commit the collection
            // BIG HACK: I use an empty JSON to by pass the rest easy check
            // The best way is probably to have two different resource but in the past, we have an another resource
            // .../commit
            // and we migrate with only one resource because I don't know : discuss with an architect
            response = performRequest(HttpMethod.PUT, commitPath, headers, JsonHandler.createObjectNode(),
                MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);
            final Response.Status status = Response.Status.fromStatusCode(response.getStatus());

            switch (status) {
                case OK:
                    LOGGER.debug(operationId + " " + Response.Status.OK.getReasonPhrase());
                    break;
                case NOT_FOUND:
                    LOGGER.error(operationId + " " + ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
                    throw new LogbookClientNotFoundException(ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
                case BAD_REQUEST:
                    LOGGER.error(operationId + " " + ErrorMessage.LOGBOOK_MISSING_MANDATORY_PARAMETER.getMessage());
                    throw new LogbookClientBadRequestException(
                        ErrorMessage.LOGBOOK_MISSING_MANDATORY_PARAMETER.getMessage());
                default:
                    LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage() + ':' + status.getReasonPhrase());
                    throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
            }
        } catch (final VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public void rollBackUnitsByOperation(String operationId)
        throws LogbookClientNotFoundException, LogbookClientBadRequestException, LogbookClientServerException {
        rollBackOperationObjects(operationId, UNIT_LIFECYCLES_URL);
    }

    @Override
    public void rollBackObjectGroupsByOperation(String operationId)
        throws LogbookClientNotFoundException, LogbookClientBadRequestException, LogbookClientServerException {
        rollBackOperationObjects(operationId, OBJECT_GROUP_LIFECYCLES_URL);
    }


    private void rollBackOperationObjects(String operationId, String uri)
        throws LogbookClientNotFoundException, LogbookClientBadRequestException, LogbookClientServerException {

        Response response = null;
        String rollBackPath = OPERATIONS_URL + "/" + operationId + uri;
        try {
            response = performRequest(HttpMethod.DELETE, rollBackPath, null,
                MediaType.APPLICATION_JSON_TYPE);
            final Response.Status status = Response.Status.fromStatusCode(response.getStatus());

            switch (status) {
                case OK:
                    LOGGER.debug(operationId + " " + Response.Status.OK.getReasonPhrase());
                    break;
                case NOT_FOUND:
                    LOGGER.error(operationId + " " + ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
                    throw new LogbookClientNotFoundException(ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
                case BAD_REQUEST:
                    LOGGER.error(operationId + " " + ErrorMessage.LOGBOOK_MISSING_MANDATORY_PARAMETER.getMessage());
                    throw new LogbookClientBadRequestException(
                        ErrorMessage.LOGBOOK_MISSING_MANDATORY_PARAMETER.getMessage());
                default:
                    LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage() + ':' + status.getReasonPhrase());
                    throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
            }
        } catch (final VitamClientInternalException e) {
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public LifeCycleStatusCode getUnitLifeCycleStatus(String unitId)
        throws LogbookClientNotFoundException, LogbookClientServerException {
        Response response = null;
        String headPath = UNIT_LIFECYCLES_URL + "/" + unitId;
        LifeCycleStatusCode lifeCycleStatusCode = null;
        try {
            response = performRequest(HttpMethod.HEAD, headPath, null, MediaType.APPLICATION_JSON_TYPE);
            final Response.Status status = Response.Status.fromStatusCode(response.getStatus());

            switch (status) {
                case OK:
                    LOGGER.debug(unitId + " " + Response.Status.OK.getReasonPhrase());
                    if (response.getHeaderString(GlobalDataRest.X_EVENT_STATUS) != null) {
                        lifeCycleStatusCode =
                            LifeCycleStatusCode.valueOf(response.getHeaderString(GlobalDataRest.X_EVENT_STATUS));
                    }

                    break;
                case NOT_FOUND:
                    LOGGER.debug(unitId + " " + ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
                    throw new LogbookClientNotFoundException(ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
                default:
                    LOGGER.debug(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage() + ':' + status.getReasonPhrase());
                    throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
            }
        } catch (final VitamClientInternalException | IllegalArgumentException e) {
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }

        return lifeCycleStatusCode;
    }

    @Override
    public LifeCycleStatusCode getObjectGroupLifeCycleStatus(String objectGroupId)
        throws LogbookClientNotFoundException, LogbookClientServerException {
        Response response = null;
        String headPath = OBJECT_GROUP_LIFECYCLES_URL + "/" + objectGroupId;
        LifeCycleStatusCode lifeCycleStatusCode = null;
        try {
            response = performRequest(HttpMethod.HEAD, headPath, null, MediaType.APPLICATION_JSON_TYPE);
            final Response.Status status = Response.Status.fromStatusCode(response.getStatus());

            switch (status) {
                case OK:
                    LOGGER.debug(objectGroupId + " " + Response.Status.OK.getReasonPhrase());
                    if (response.getHeaderString(GlobalDataRest.X_EVENT_STATUS) != null) {
                        lifeCycleStatusCode =
                            LifeCycleStatusCode.valueOf(response.getHeaderString(GlobalDataRest.X_EVENT_STATUS));
                    }

                    break;
                case NOT_FOUND:
                    LOGGER.debug(objectGroupId + " " + ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
                    throw new LogbookClientNotFoundException(ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
                default:
                    LOGGER.debug(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage() + ':' + status.getReasonPhrase());
                    throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
            }
        } catch (final VitamClientInternalException | IllegalArgumentException e) {
            LOGGER.debug(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }

        return lifeCycleStatusCode;
    }

    @Override
    public void bulkObjectGroup(String eventIdProc, List<LogbookLifeCycleObjectGroupModel> logbookLifeCycleModels)
        throws LogbookClientAlreadyExistsException, LogbookClientBadRequestException, LogbookClientServerException {
        Response response = null;
        try {

            response =
                performRequest(HttpMethod.PUT, OPERATIONS_URL + "/" + eventIdProc + "/lifecycles/objectgroup/bulk",
                    null,
                    logbookLifeCycleModels, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);
            final Status status = Status.fromStatusCode(response.getStatus());
            switch (status) {
                case CREATED:
                    LOGGER.debug(eventIdProc + " " + Response.Status.CREATED.getReasonPhrase());
                    break;
                case CONFLICT:
                    LOGGER.debug(eventIdProc + " " + ErrorMessage.LOGBOOK_ALREADY_EXIST.getMessage());
                    throw new LogbookClientAlreadyExistsException(ErrorMessage.LOGBOOK_ALREADY_EXIST.getMessage());
                case BAD_REQUEST:
                    LOGGER.debug(eventIdProc + " " + ErrorMessage.LOGBOOK_MISSING_MANDATORY_PARAMETER.getMessage());
                    throw new LogbookClientBadRequestException(
                        ErrorMessage.LOGBOOK_MISSING_MANDATORY_PARAMETER.getMessage());
                default:
                    LOGGER.debug(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage() + ':' + status.getReasonPhrase());
                    throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
            }
        } catch (final VitamClientInternalException e) {
            LOGGER.debug(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public void bulkUnit(String eventIdProc, List<LogbookLifeCycleUnitModel> logbookLifeCycleModels)
        throws LogbookClientAlreadyExistsException, LogbookClientBadRequestException, LogbookClientServerException {
        Response response = null;
        try {

            response =
                performRequest(HttpMethod.PUT, OPERATIONS_URL + "/" + eventIdProc + "/lifecycles/unit/bulk", null,
                    logbookLifeCycleModels, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);
            final Status status = Status.fromStatusCode(response.getStatus());
            switch (status) {
                case CREATED:
                    LOGGER.debug(eventIdProc + " " + Response.Status.CREATED.getReasonPhrase());
                    break;
                case CONFLICT:
                    LOGGER.debug(eventIdProc + " " + ErrorMessage.LOGBOOK_ALREADY_EXIST.getMessage());
                    throw new LogbookClientAlreadyExistsException(ErrorMessage.LOGBOOK_ALREADY_EXIST.getMessage());
                case BAD_REQUEST:
                    LOGGER.debug(eventIdProc + " " + ErrorMessage.LOGBOOK_MISSING_MANDATORY_PARAMETER.getMessage());
                    throw new LogbookClientBadRequestException(
                        ErrorMessage.LOGBOOK_MISSING_MANDATORY_PARAMETER.getMessage());
                default:
                    LOGGER.debug(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage() + ':' + status.getReasonPhrase());
                    throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
            }
        } catch (final VitamClientInternalException e) {
            LOGGER.debug(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public JsonNode getRawUnitLifeCycleById(String id)
        throws LogbookClientException {
        Response response = null;
        try {

            response = performRequest(HttpMethod.GET, UNIT_LIFECYCLES_RAW_BY_ID_URL + id, null,
                null, null, MediaType.APPLICATION_JSON_TYPE);

            if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
                LOGGER.error(ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
                throw new LogbookClientNotFoundException(ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
            } else if (response.getStatus() == Response.Status.PRECONDITION_FAILED.getStatusCode()) {
                LOGGER.error(ILLEGAL_ENTRY_PARAMETER);
                throw new LogbookClientException(REQUEST_PRECONDITION_FAILED);
            } else if (response.getStatus() != Response.Status.OK.getStatusCode()) {
                LOGGER.error(INTERNAL_SERVER_ERROR + " " + response.getStatus());
                throw new LogbookClientException(INTERNAL_SERVER_ERROR);
            }

            return ((RequestResponseOK<JsonNode>)RequestResponse.parseFromResponse(response)).getFirstResult();

        } catch (final VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public List<JsonNode> getRawUnitLifeCycleByIds(List<String> ids)
        throws LogbookClientException {
        Response response = null;
        try {

            response = performRequest(HttpMethod.GET, UNIT_LIFECYCLES_RAW_BY_IDS_URL, null,
                ids, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);

            if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
                LOGGER.error(ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
                throw new LogbookClientNotFoundException(ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
            } else if (response.getStatus() == Response.Status.PRECONDITION_FAILED.getStatusCode()) {
                LOGGER.error(ILLEGAL_ENTRY_PARAMETER);
                throw new LogbookClientException(REQUEST_PRECONDITION_FAILED);
            } else if (response.getStatus() != Response.Status.OK.getStatusCode()) {
                LOGGER.error(INTERNAL_SERVER_ERROR + " " + response.getStatus());
                throw new LogbookClientException(INTERNAL_SERVER_ERROR);
            }

            return ((RequestResponseOK<JsonNode>)RequestResponse.parseFromResponse(response)).getResults();

        } catch (final VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public JsonNode getRawObjectGroupLifeCycleById(String id)
        throws LogbookClientException {
        Response response = null;
        try {

            response = performRequest(HttpMethod.GET, OBJECT_GROUP_LIFECYCLES_RAW_BY_ID_URL + id, null,
                null, null, MediaType.APPLICATION_JSON_TYPE);

            if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
                LOGGER.error(ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
                throw new LogbookClientNotFoundException(ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
            } else if (response.getStatus() == Response.Status.PRECONDITION_FAILED.getStatusCode()) {
                LOGGER.error(ILLEGAL_ENTRY_PARAMETER);
                throw new LogbookClientException(REQUEST_PRECONDITION_FAILED);
            } else if (response.getStatus() != Response.Status.OK.getStatusCode()) {
                LOGGER.error(INTERNAL_SERVER_ERROR + " " + response.getStatus());
                throw new LogbookClientException(INTERNAL_SERVER_ERROR);
            }

            return ((RequestResponseOK<JsonNode>)RequestResponse.parseFromResponse(response)).getFirstResult();

        } catch (final VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public List<JsonNode> getRawObjectGroupLifeCycleByIds(List<String> ids)
        throws LogbookClientException {
        Response response = null;
        try {

            response = performRequest(HttpMethod.GET, OBJECT_GROUP_LIFECYCLES_RAW_BY_IDS_URL, null,
                ids, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);

            if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
                LOGGER.error(ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
                throw new LogbookClientNotFoundException(ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
            } else if (response.getStatus() == Response.Status.PRECONDITION_FAILED.getStatusCode()) {
                LOGGER.error(ILLEGAL_ENTRY_PARAMETER);
                throw new LogbookClientException(REQUEST_PRECONDITION_FAILED);
            } else if (response.getStatus() != Response.Status.OK.getStatusCode()) {
                LOGGER.error(INTERNAL_SERVER_ERROR + " " + response.getStatus());
                throw new LogbookClientException(INTERNAL_SERVER_ERROR);
            }

            return ((RequestResponseOK<JsonNode>)RequestResponse.parseFromResponse(response)).getResults();

        } catch (final VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public void createRawbulkObjectgrouplifecycles(List<JsonNode> logbookLifeCycleRaws)
        throws LogbookClientBadRequestException, LogbookClientServerException {
        createRawbulk(logbookLifeCycleRaws, OBJECT_GROUP_LIFECYCLES_RAW_BULK_URL);
    }

    @Override
    public void createRawbulkUnitlifecycles(List<JsonNode> logbookLifeCycleRaws)
        throws LogbookClientBadRequestException, LogbookClientServerException {
        createRawbulk(logbookLifeCycleRaws, UNIT_LIFECYCLES_RAW_BULK_URL);
    }

    @Override
    public void bulkLifeCycleTemporary(String operationId, DistributionType type, List<LogbookLifeCycleParametersBulk> logbookLifeCycleParametersBulk) throws VitamClientInternalException {
        bulkLFC(type, logbookLifeCycleParametersBulk, OPERATIONS_URL + "/" + operationId + "/bulklifecycles/%s/temporary");
    }

    @Override
    public void bulkLifeCycle(String operationId, DistributionType type, List<LogbookLifeCycleParametersBulk> logbookLifeCycleParametersBulk) throws VitamClientInternalException {
        bulkLFC(type, logbookLifeCycleParametersBulk, OPERATIONS_URL + "/" + operationId + "/bulklifecycles/%s");
    }

    private void bulkLFC(DistributionType type, List<LogbookLifeCycleParametersBulk> logbookLifeCycleParametersBulk, String uriPattern) throws VitamClientInternalException {
        Response response = null;
        try {
            switch (type) {

                case Units:
                    response = performRequest(HttpMethod.POST,  String.format(uriPattern, "unit"), null,
                        logbookLifeCycleParametersBulk, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);
                    break;

                case ObjectGroup:
                    response = performRequest(HttpMethod.POST, String.format(uriPattern, "got"), null,
                        logbookLifeCycleParametersBulk, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);
                    break;
                default:
                    response = null;
            }
            final Status status = Status.fromStatusCode(response.getStatus());

            if (status.getFamily() != Status.Family.SUCCESSFUL) {
                throw new VitamRuntimeException("unable to store lifecycle, error code is :" + status.getStatusCode());
            }
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public void deleteLifecycleUnitsBulk(Collection<String> unitsIds)
        throws LogbookClientServerException, LogbookClientBadRequestException {
        ParametersChecker.checkParameter("List has to be provided ", unitsIds);
        Response response = null;

        try {
            response =
                performRequest(HttpMethod.DELETE, "/lifeCycleUnits/bulkDelete", null, unitsIds, MediaType.APPLICATION_JSON_TYPE,
                    MediaType.APPLICATION_JSON_TYPE);
            final Status status = Status.fromStatusCode(response.getStatus());

            if (response.getStatus() == Status.OK.getStatusCode()) {
                // Every thing is OK
                return;
            }
            if (response.getStatus() == Status.BAD_REQUEST.getStatusCode()) {
                LOGGER.debug(ErrorMessage.LOGBOOK_MISSING_MANDATORY_PARAMETER.getMessage());
                throw new LogbookClientBadRequestException(
                    ErrorMessage.LOGBOOK_MISSING_MANDATORY_PARAMETER.getMessage());
            }
            LOGGER.debug(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage() + ':' + status.getReasonPhrase());
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());

        } catch (final VitamClientInternalException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR, e);
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override public void deleteLifecycleObjectGroupBulk(Collection<String> objectGroupIds)
        throws LogbookClientBadRequestException, LogbookClientServerException {

        Response response = null;
        try {
            response =
                performRequest(HttpMethod.DELETE, "/objectgrouplifecycles/bulkDelete", null, objectGroupIds, MediaType.APPLICATION_JSON_TYPE,
                    MediaType.APPLICATION_JSON_TYPE);
            final Status status = Status.fromStatusCode(response.getStatus());

            if (response.getStatus() == Status.OK.getStatusCode()) {
                // Every thing is OK
                return;
            }
            if (response.getStatus() == Status.BAD_REQUEST.getStatusCode()) {
                LOGGER.debug(ErrorMessage.LOGBOOK_MISSING_MANDATORY_PARAMETER.getMessage());
                throw new LogbookClientBadRequestException(
                    ErrorMessage.LOGBOOK_MISSING_MANDATORY_PARAMETER.getMessage());
            }
            LOGGER.debug(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage() + ':' + status.getReasonPhrase());
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());

        } catch (final VitamClientInternalException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR, e);
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    private void createRawbulk(List<JsonNode> logbookLifeCycleRaws, String url)
        throws LogbookClientBadRequestException, LogbookClientServerException {
        Response response = null;
        try {

            response =
                performRequest(HttpMethod.POST, url, null,
                    logbookLifeCycleRaws, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);
            final Status status = Status.fromStatusCode(response.getStatus());
            switch (status) {
                case CREATED:
                    LOGGER.debug(Response.Status.CREATED.getReasonPhrase());
                    break;
                case BAD_REQUEST:
                    LOGGER.debug(ErrorMessage.LOGBOOK_MISSING_MANDATORY_PARAMETER.getMessage());
                    throw new LogbookClientBadRequestException(
                        ErrorMessage.LOGBOOK_MISSING_MANDATORY_PARAMETER.getMessage());
                default:
                    LOGGER.debug(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage() + ':' + status.getReasonPhrase());
                    throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
            }
        } catch (final VitamClientInternalException e) {
            LOGGER.debug(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

}
