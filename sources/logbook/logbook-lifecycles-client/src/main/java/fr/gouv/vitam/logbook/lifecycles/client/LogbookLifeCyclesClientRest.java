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
package fr.gouv.vitam.logbook.lifecycles.client;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ServerIdentity;
import fr.gouv.vitam.common.client2.DefaultClient;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.logbook.common.client.ErrorMessage;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleObjectGroupParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleUnitParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;

/**
 * LogbookLifeCyclesClient REST implementation
 */
/**
 *
 */
class LogbookLifeCyclesClientRest extends DefaultClient implements LogbookLifeCyclesClient {

    private static final String REQUEST_PROCONDITION_FAILED = "Request procondition failed";
    private static final String ILLEGAL_ENTRY_PARAMETER = "Illegal Entry Parameter";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogbookLifeCyclesClientRest.class);
    private static final String LIFECYCLES_URL = "/lifecycles";
    private static final String UNIT_LIFECYCLES_URL = "/unitlifecycles";
    private static final String OBJECT_GROUP_LIFECYCLES_URL = "/objectgrouplifecycles";
    private static final ServerIdentity SERVER_IDENTITY = ServerIdentity.getInstance();
    private static final int TENANT_ID = 0;


    LogbookLifeCyclesClientRest(LogbookLifeCyclesClientFactory factory) {
        super(factory);
    }

    @Override
    public void create(LogbookParameters parameters)
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
        } catch (VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }

    }

    private String getServiceUrl(LogbookParameters parameters, String eip, String oid) {
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
    public void update(LogbookParameters parameters)
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
            response = performRequest(HttpMethod.PUT, getServiceUrl(parameters, eip, oid), null,
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
        } catch (VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public void commit(LogbookParameters parameters)
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
        } catch (VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public void rollback(LogbookParameters parameters)
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
        } catch (VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public JsonNode selectLifeCycles(String select) throws LogbookClientException, InvalidParseOperationException {
        Response response = null;
        try {
            final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
            headers.add(GlobalDataRest.X_HTTP_METHOD_OVERRIDE, HttpMethod.GET);
            response = performRequest(HttpMethod.POST, LIFECYCLES_URL, headers,
                select, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);

            if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
                LOGGER.error(ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
                throw new LogbookClientNotFoundException(ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
            } else if (response.getStatus() == Response.Status.PRECONDITION_FAILED.getStatusCode()) {
                LOGGER.error(ILLEGAL_ENTRY_PARAMETER);
                throw new LogbookClientException(REQUEST_PROCONDITION_FAILED);
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
    public JsonNode selectLifeCyclesById(String id) throws LogbookClientException, InvalidParseOperationException {
        Response response = null;
        try {
            final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
            headers.add(GlobalDataRest.X_HTTP_METHOD_OVERRIDE, HttpMethod.GET);
            response = performRequest(HttpMethod.POST, LIFECYCLES_URL + "/" + id, headers,
                LogbookParametersFactory.newLogbookOperationParameters(), MediaType.APPLICATION_JSON_TYPE,
                MediaType.APPLICATION_JSON_TYPE);

            if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
                LOGGER.error(ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
                throw new LogbookClientNotFoundException(ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
            } else if (response.getStatus() == Response.Status.PRECONDITION_FAILED.getStatusCode()) {
                LOGGER.error(ILLEGAL_ENTRY_PARAMETER);
                throw new LogbookClientException(REQUEST_PROCONDITION_FAILED);
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
    public JsonNode selectUnitLifeCycleById(String id) throws LogbookClientException, InvalidParseOperationException {
        // TODO : Request ID should be generated by the current context code, not the client directly
        final GUID guid = GUIDFactory.newRequestIdGUID(TENANT_ID);
        Response response = null;
        try {
            final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
            headers.add(GlobalDataRest.X_REQUEST_ID, guid.toString());
            response = performRequest(HttpMethod.GET, UNIT_LIFECYCLES_URL + "/" + id, null,
                MediaType.APPLICATION_JSON_TYPE);

            if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
                LOGGER.error(ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
                throw new LogbookClientNotFoundException(ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
            } else if (response.getStatus() == Response.Status.PRECONDITION_FAILED.getStatusCode()) {
                LOGGER.error(ILLEGAL_ENTRY_PARAMETER);
                throw new LogbookClientException(REQUEST_PROCONDITION_FAILED);
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
    public JsonNode selectObjectGroupLifeCycleById(String id)
        throws LogbookClientException, InvalidParseOperationException {
        // TODO : Request ID should be generated by the current context code, not the client directly
        final GUID guid = GUIDFactory.newRequestIdGUID(TENANT_ID);
        Response response = null;
        try {
            final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
            headers.add(GlobalDataRest.X_REQUEST_ID, guid.toString());
            response = performRequest(HttpMethod.GET, OBJECT_GROUP_LIFECYCLES_URL + "/" + id, null,
                MediaType.APPLICATION_JSON_TYPE);

            if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
                LOGGER.error(ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
                throw new LogbookClientNotFoundException(ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
            } else if (response.getStatus() == Response.Status.PRECONDITION_FAILED.getStatusCode()) {
                LOGGER.error(ILLEGAL_ENTRY_PARAMETER);
                throw new LogbookClientException(REQUEST_PROCONDITION_FAILED);
            }
            return JsonHandler.getFromString(response.readEntity(String.class));
        } catch (VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }
}
