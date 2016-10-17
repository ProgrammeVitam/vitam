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

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.client2.DefaultClient;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.processing.common.model.EngineResponse;
import fr.gouv.vitam.processing.common.model.OutcomeMessage;
import fr.gouv.vitam.processing.common.model.ProcessResponse;
import fr.gouv.vitam.worker.client.exception.WorkerNotFoundClientException;
import fr.gouv.vitam.worker.client.exception.WorkerServerClientException;
import fr.gouv.vitam.worker.common.DescriptionStep;

/**
 * WorkerClient implementation for production environment using REST API.
 */
public class WorkerClientRest extends DefaultClient implements WorkerClient {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(WorkerClientRest.class);
    private static final String REQUEST_ID_MUST_HAVE_A_VALID_VALUE = "request id must have a valid value";
    private static final String DATA_MUST_HAVE_A_VALID_VALUE = "data must have a valid value";

    WorkerClientRest(WorkerClientFactory factory) {
        super(factory);
    }

    @Override
    public List<EngineResponse> submitStep(String requestId, DescriptionStep step)
        throws WorkerNotFoundClientException, WorkerServerClientException {
        ParametersChecker.checkParameter(REQUEST_ID_MUST_HAVE_A_VALID_VALUE, requestId);
        ParametersChecker.checkParameter(DATA_MUST_HAVE_A_VALID_VALUE, step);
        Response response = null;
        try {
            response =
                performRequest(HttpMethod.POST, "/" + "tasks", getDefaultHeaders(requestId), 
                    JsonHandler.toJsonNode(step), MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);
            final JsonNode node = handleCommonResponseStatus(requestId, step, response, JsonNode.class);
            return getListResponses(node);
        } catch (final VitamClientInternalException e) {
            LOGGER.error("Worker Internal Server Error", e);
            throw new WorkerServerClientException("Worker Internal Server Error", e);
        } catch (InvalidParseOperationException e) {
            LOGGER.error("Worker Internal Server Error", e);
            throw new WorkerServerClientException("Step description incorrect", e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    /**
     * Generate the default header map
     *
     * @param asyncId the tenant id
     * @return header map
     */
    private MultivaluedHashMap<String, Object> getDefaultHeaders(String requestId) {
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(GlobalDataRest.X_REQUEST_ID, requestId);
        return headers;
    }


    /**
     * Common method to handle status responses
     *
     * @param requestId the current requestId
     * @param step the current step
     * @param response the JAX-RS response from the server
     * @param responseType the type to map the response into
     * @param <R> response type parameter
     * @return the Response mapped as an POJO
     * @throws VitamClientException the exception if any from the server
     */
    protected <R> R handleCommonResponseStatus(String requestId, DescriptionStep step, Response response, Class<R> responseType)
        throws WorkerNotFoundClientException, WorkerServerClientException {
        final Response.Status status = Response.Status.fromStatusCode(response.getStatus());
        switch (status) {
            case OK:
                return response.readEntity(responseType);
            case NOT_FOUND:
                throw new WorkerNotFoundClientException(status.getReasonPhrase());
            default:
                LOGGER.error(INTERNAL_SERVER_ERROR + " during execution of " + requestId
                    + " Request, stepname:  " + step.getStep().getStepName() + " : " + status.getReasonPhrase());
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
        final List<EngineResponse> responses = new ArrayList<>();
        if (node != null && node.isArray()) {
            for (final JsonNode engineResponseNode : node) {
                final ProcessResponse response = new ProcessResponse();
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
                final JsonNode outcomeMessages = engineResponseNode.get("outcomeMessages");
                if (outcomeMessages != null) {
                    if (outcomeMessages.isArray()) {
                        for (final JsonNode outcomeMessageNode : outcomeMessages) {
                            final Iterator<String> fieldNames = outcomeMessageNode.fieldNames();
                            while (fieldNames.hasNext()) {
                                final String actionKey = fieldNames.next();
                                final String messageValue = outcomeMessageNode.get(actionKey).asText();
                                final OutcomeMessage message = OutcomeMessage.valueOf(messageValue);
                                response.setOutcomeMessages(actionKey, message);
                            }
                        }
                    } else {
                        final Iterator<String> fieldNames = outcomeMessages.fieldNames();
                        while (fieldNames.hasNext()) {
                            final String actionKey = fieldNames.next();
                            final String messageValue = outcomeMessages.get(actionKey).asText();
                            final OutcomeMessage message = OutcomeMessage.valueOf(messageValue);
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
