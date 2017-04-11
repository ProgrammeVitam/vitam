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

package fr.gouv.vitam.logbook.operations.client;

import java.util.Queue;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.client.DefaultClient;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.logbook.common.client.ErrorMessage;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationsClientHelper;

/**
 * Logbook operation REST client
 */
class LogbookOperationsClientRest extends DefaultClient implements LogbookOperationsClient {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogbookOperationsClientRest.class);
    private static final String OPERATIONS_URL = "/operations";
    private static final String TRACEABILITY_URI = "/operations/traceability";

    private final LogbookOperationsClientHelper helper = new LogbookOperationsClientHelper();

    LogbookOperationsClientRest(LogbookOperationsClientFactory factory) {
        super(factory);
    }

    @Override
    public void create(LogbookOperationParameters parameters)
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException {
        final String eip = LogbookOperationsClientHelper.checkLogbookParameters(parameters);
        Response response = null;
        try {
            response = performRequest(HttpMethod.POST, OPERATIONS_URL + "/" + eip, null,
                parameters, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);
            final Status status = Status.fromStatusCode(response.getStatus());
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

    @Override
    public void update(LogbookOperationParameters parameters)
        throws LogbookClientBadRequestException, LogbookClientNotFoundException, LogbookClientServerException {
        final String eip = LogbookOperationsClientHelper.checkLogbookParameters(parameters);
        Response response = null;
        try {
            response = performRequest(HttpMethod.PUT, OPERATIONS_URL + "/" + eip, null,
                parameters, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);
            final Status status = Status.fromStatusCode(response.getStatus());
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

    @Override
    public JsonNode selectOperation(JsonNode select) throws LogbookClientException, InvalidParseOperationException {
        Response response = null;
        try {
            response = performRequest(HttpMethod.GET, OPERATIONS_URL, null,
                select, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);

            if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
                LOGGER.error(ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
                throw new LogbookClientNotFoundException(ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
            } else if (response.getStatus() == Status.PRECONDITION_FAILED.getStatusCode()) {
                LOGGER.error("Illegal Entry Parameter");
                throw new LogbookClientException("Request procondition failed");
            } else if (response.getStatus() == Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
                LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
                throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
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
    public JsonNode selectOperationById(String processId, JsonNode queryDsl)
        throws LogbookClientException, InvalidParseOperationException {
        Response response = null;
        try {
            response = performRequest(HttpMethod.GET, OPERATIONS_URL + "/" + processId, null,
                queryDsl, MediaType.APPLICATION_JSON_TYPE,
                MediaType.APPLICATION_JSON_TYPE);

            if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
                LOGGER.error(ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
                throw new LogbookClientNotFoundException(ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
            } else if (response.getStatus() == Status.PRECONDITION_FAILED.getStatusCode()) {
                LOGGER.error("Illegal Entry Parameter");
                throw new LogbookClientException("Request procondition failed");
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
    public void createDelegate(LogbookOperationParameters parameters) throws LogbookClientAlreadyExistsException {
        helper.createDelegate(parameters);
    }

    @Override
    public void updateDelegate(LogbookOperationParameters parameters) throws LogbookClientNotFoundException {
        helper.updateDelegate(parameters);
    }

    @Override
    public void bulkCreate(String eventIdProc, Iterable<LogbookOperationParameters> queue)
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException,
        LogbookClientServerException {
        if (queue != null) {
            Response response = null;
            try {
                response = performRequest(HttpMethod.POST, OPERATIONS_URL, null,
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
    public void commitCreateDelegate(String eventIdProc)
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException,
        LogbookClientServerException {
        final Queue<LogbookOperationParameters> queue = helper.removeCreateDelegate(eventIdProc);
        bulkCreate(eventIdProc, queue);
    }

    @Override
    public void bulkUpdate(String eventIdProc, Iterable<LogbookOperationParameters> queue)
        throws LogbookClientNotFoundException, LogbookClientBadRequestException, LogbookClientServerException {
        if (queue != null) {
            Response response = null;
            try {
                response = performRequest(HttpMethod.PUT, OPERATIONS_URL, null,
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
    public void commitUpdateDelegate(String eventIdProc)
        throws LogbookClientBadRequestException, LogbookClientNotFoundException, LogbookClientServerException {
        final Queue<LogbookOperationParameters> queue = helper.removeUpdateDelegate(eventIdProc);
        bulkUpdate(eventIdProc, queue);
    }

    @Override
    public void close() {
        super.close();
        helper.clear();
    }

    @Override
    public RequestResponseOK traceability() throws LogbookClientServerException, InvalidParseOperationException {
        Response response = null;
        try {
            final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
            headers.add(GlobalDataRest.X_TENANT_ID, ParameterHelper.getTenantParameter());
            response = performRequest(HttpMethod.POST, TRACEABILITY_URI, headers, MediaType.APPLICATION_JSON_TYPE);
            final Status status = Status.fromStatusCode(response.getStatus());
            switch (status) {
                case OK:
                    LOGGER.debug(" " + Response.Status.OK.getReasonPhrase());
                    break;
                default:
                    LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage() + ':' + status.getReasonPhrase());
                    throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
            }
            return RequestResponse.parseRequestResponseOk(response);
        } catch (final VitamClientInternalException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } finally {
            consumeAnyEntityAndClose(response);
        }

    }
}
