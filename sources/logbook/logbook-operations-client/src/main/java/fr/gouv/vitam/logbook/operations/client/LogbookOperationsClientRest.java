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

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ServerIdentity;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.logbook.common.client.ErrorMessage;
import fr.gouv.vitam.logbook.common.client.StatusMessage;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;

/**
 * Logbook operation client
 */
public class LogbookOperationsClientRest implements LogbookClient {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogbookOperationsClientRest.class);
    private static final String RESOURCE_PATH = "/logbook/v1";
    private static final String OPERATIONS_URL = "/operations";
    private static final String STATUS = "/status";
    private static final ServerIdentity SERVER_IDENTITY = ServerIdentity.getInstance();

    private final String serviceUrl;
    private final Client client;

    LogbookOperationsClientRest(String server, int port) {
        serviceUrl = "http://" + server + ":" + port + RESOURCE_PATH;
        final ClientConfig config = new ClientConfig();
        config.register(JacksonJsonProvider.class);
        config.register(JacksonFeature.class);
        client = ClientBuilder.newClient(config);
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
        final Response response = client.target(serviceUrl).path(OPERATIONS_URL + "/" + eip).request()
            .post(Entity.json(parameters));
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
        final Response response = client.target(serviceUrl).path(OPERATIONS_URL + "/" + eip).request()
            .put(Entity.json(parameters));
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
    }

    @Override
    public void close() {
        client.close();
    }

    @Override
    public StatusMessage status() throws LogbookClientServerException {
        final Response response = client.target(serviceUrl).path(STATUS).request().get();
        final Status status = Status.fromStatusCode(response.getStatus());
        switch (status) {
            case OK:
                StatusMessage message;
                if (response.hasEntity()) {
                    message = response.readEntity(StatusMessage.class);
                } else {
                    message = new StatusMessage();
                }
                return message;
            case NO_CONTENT:
                return new StatusMessage();
            default:
                LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage() + ':' + status.getReasonPhrase());
                throw new LogbookClientServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
        }
    }

    @Override
    public JsonNode selectOperation(String select) throws LogbookClientException, InvalidParseOperationException {
        final Response response = client.target(serviceUrl).path(OPERATIONS_URL).request(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON).header("X-Http-Method-Override", "GET")
            .post(Entity.entity(select, MediaType.APPLICATION_JSON), Response.class);

        if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
            LOGGER.error(ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
            throw new LogbookClientNotFoundException(ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
        } else if (response.getStatus() == Status.PRECONDITION_FAILED.getStatusCode()) {
            LOGGER.error("Illegal Entry Parameter");
            throw new LogbookClientException("Request procondition failed");
        }

        return JsonHandler.getFromString(response.readEntity(String.class));
    }

    @Override
    public JsonNode selectOperationbyId(String processId)
        throws LogbookClientException, InvalidParseOperationException {
        final Response response = client.target(serviceUrl).path(OPERATIONS_URL + "/" + processId).request()

            .accept(MediaType.APPLICATION_JSON).header("X-Http-Method-Override", "GET")
            .post(Entity.entity(LogbookParametersFactory.newLogbookOperationParameters(), MediaType.APPLICATION_JSON),
                Response.class);

        if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
            LOGGER.error(ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
            throw new LogbookClientNotFoundException(ErrorMessage.LOGBOOK_NOT_FOUND.getMessage());
        } else if (response.getStatus() == Status.PRECONDITION_FAILED.getStatusCode()) {
            LOGGER.error("Illegal Entry Parameter");
            throw new LogbookClientException("Request procondition failed");
        }

        return JsonHandler.getFromString(response.readEntity(String.class));
    }
}
