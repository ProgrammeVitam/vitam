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
package fr.gouv.vitam.worker.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.client.AbstractClient;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.application.configuration.ClientConfigurationImpl;
import fr.gouv.vitam.processing.common.model.EngineResponse;
import fr.gouv.vitam.processing.common.model.OutcomeMessage;
import fr.gouv.vitam.processing.common.model.ProcessResponse;
import fr.gouv.vitam.processing.common.model.StatusCode;
import fr.gouv.vitam.worker.client.exception.WorkerNotFoundClientException;
import fr.gouv.vitam.worker.client.exception.WorkerServerClientException;
import fr.gouv.vitam.worker.common.DescriptionStep;

/**
 * WorkerClient implementation for production environment using REST API.
 */
public class WorkerClientRest extends AbstractClient implements WorkerClient {
    private static final String REQUEST_ID_MUST_HAVE_A_VALID_VALUE = "request id must have a valid value";
    private static final String DATA_MUST_HAVE_A_VALID_VALUE = "data must have a valid value";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(WorkerClientRest.class);

    WorkerClientRest(ClientConfigurationImpl clientConfiguration, String resourcePath,
        boolean suppressHttpCompliance) {
        super(clientConfiguration, resourcePath, suppressHttpCompliance);
    }

    WorkerClientRest(ClientConfigurationImpl clientConfiguration, String resourcePath, Client client) {
        super(clientConfiguration, resourcePath, client);
    }

    @Override
    public List<EngineResponse> submitStep(String requestId, DescriptionStep step)
        throws WorkerNotFoundClientException, WorkerServerClientException {
        ParametersChecker.checkParameter(REQUEST_ID_MUST_HAVE_A_VALID_VALUE, requestId);
        ParametersChecker.checkParameter(DATA_MUST_HAVE_A_VALID_VALUE, step);
        Response response = null;
        try {
            JsonNode stepJson = JsonHandler.toJsonNode(step);
            response =
                performGenericRequest("/" + "tasks", stepJson, MediaType.APPLICATION_JSON, getDefaultHeaders(requestId),
                    HttpMethod.POST, MediaType.APPLICATION_JSON);
            JsonNode node = (JsonNode) handleCommonResponseStatus(response, JsonNode.class);
            return getListResponses(node);
        } catch (javax.ws.rs.ProcessingException e) {
            LOGGER.error("Worker Internal Server Error", e);
            throw new WorkerServerClientException("Worker Internal Server Error", e);
        } catch (InvalidParseOperationException e) {
            LOGGER.error("Worker Client Error", e);
            throw new WorkerServerClientException("Step description incorrect", e);
        } finally {
            Optional.ofNullable(response).ifPresent(Response::close);
        }
    }

    /**
     * Generate the default header map
     * 
     * @param asyncId the tenant id
     * @return header map
     */
    private MultivaluedHashMap<String, Object> getDefaultHeaders(String requestId) {
        MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(GlobalDataRest.X_REQUEST_ID, requestId);
        return headers;
    }

    @Override
    protected <R> R handleCommonResponseStatus(Response response, Class<R> responseType)
        throws WorkerNotFoundClientException, WorkerServerClientException {
        final Response.Status status = Response.Status.fromStatusCode(response.getStatus());
        switch (status) {
            case OK:
                return response.readEntity(responseType);
            case NOT_FOUND:
                throw new WorkerNotFoundClientException(status.getReasonPhrase());
            default:
                LOGGER.error(INTERNAL_SERVER_ERROR + " : " + status.getReasonPhrase());
                throw new WorkerServerClientException(INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Create a List<EngineResponse> from the JsonNode representation <br>
     * TODO : replace EngineResponse with real usable POJO (and json compatible).
     * 
     * @param node representation of List<EngineResponse> as JsonNode
     * @return List<EngineResponse>
     */
    private List<EngineResponse> getListResponses(JsonNode node) {
        List<EngineResponse> responses = new ArrayList<>();
        if (node != null && node.isArray()) {
            for (JsonNode engineResponseNode : node) {
                ProcessResponse response = new ProcessResponse();
                if (engineResponseNode.get("processId") != null) {
                    response.setProcessId(engineResponseNode.get("processId").asText(null));
                }
                if (engineResponseNode.get("messageIdentifier") != null) {
                    response.setMessageIdentifier(engineResponseNode.get("messageIdentifier").asText(null));
                }
                if (engineResponseNode.get("value") != null) {
                    response.setStatus(StatusCode.valueOf(engineResponseNode.get("value").asText(null)));
                }
                if (engineResponseNode.get("errorNumber") != null) {
                    response.setErrorNumber(engineResponseNode.get("errorNumber").asInt());
                }
                JsonNode outcomeMessages = engineResponseNode.get("outcomeMessages");
                if (outcomeMessages != null) {
                    if (outcomeMessages.isArray()) {
                        for (JsonNode outcomeMessageNode : outcomeMessages) {
                            Iterator<String> fieldNames = outcomeMessageNode.fieldNames();
                            while (fieldNames.hasNext()) {
                                String actionKey = fieldNames.next();
                                String messageValue = outcomeMessageNode.get(actionKey).asText();
                                OutcomeMessage message = OutcomeMessage.valueOf(messageValue);
                                response.setOutcomeMessages(actionKey, message);
                            }
                        }
                    } else {
                        Iterator<String> fieldNames = outcomeMessages.fieldNames();
                        while (fieldNames.hasNext()) {
                            String actionKey = fieldNames.next();
                            String messageValue = outcomeMessages.get(actionKey).asText();
                            OutcomeMessage message = OutcomeMessage.valueOf(messageValue);
                            response.setOutcomeMessages(actionKey, message);
                        }
                    }
                }
                responses.add(response);
            }
        }
        return responses;
    }
}
