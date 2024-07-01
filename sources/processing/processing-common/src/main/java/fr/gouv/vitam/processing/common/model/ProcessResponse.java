/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "https://cecill.info".
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
package fr.gouv.vitam.processing.common.model;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.model.StatusCode;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Process Response class</br>
 * Contains global process status, messages and list of action results <br>
 */
public class ProcessResponse implements EngineResponse {

    /**
     * Process Id
     */
    private String processId;

    /**
     * Enum status code
     */
    private StatusCode status;

    /**
     * Map of functional messages
     */
    private Map<String, OutcomeMessage> outcomeMessages;

    /**
     * List of functional messages
     */
    private Integer numberErrors;

    /**
     * Message identifier
     */
    private String messageId;

    /**
     * List of steps 's responses <br>
     *
     * key is stepName
     *
     * object is list of response 's action
     */
    private Map<String, List<EngineResponse>> stepResponses;

    /**
     * implementation of getStatus() of EngineResponse API class
     */
    @Override
    public StatusCode getStatus() {
        if (status == null) {
            return StatusCode.WARNING;
        }
        return status;
    }

    /**
     * implementation of setStatus() of EngineResponse API class
     */
    @Override
    public ProcessResponse setStatus(StatusCode status) {
        this.status = status;
        return this;
    }

    /**
     * implementation of getMessage() of EngineResponse API class
     */
    @Override
    public Map<String, OutcomeMessage> getOutcomeMessages() {
        if (outcomeMessages == null) {
            return Collections.emptyMap();
        }
        return outcomeMessages;
    }

    /**
     * implementation of setMessage() of EngineResponse API class
     */
    @Override
    public ProcessResponse setOutcomeMessages(String handlerId, OutcomeMessage message) {
        ParametersChecker.checkParameter("Handler id is a mandatory parameter", handlerId);
        ParametersChecker.checkParameter("Outcome Detail Message is a mandatory parameter", message);
        if (outcomeMessages == null) {
            outcomeMessages = new HashMap<>();
        }
        outcomeMessages.put(handlerId, message);
        return this;
    }

    /**
     * getStepResponses given the response of each step of workflow processing
     *
     * @return the stepResponses
     */

    public Map<String, List<EngineResponse>> getStepResponses() {
        if (stepResponses == null) {
            return Collections.emptyMap();
        }
        return stepResponses;
    }

    /**
     * setStepResponses, set the response at each step of workflow processing
     *
     * @param stepResponses the stepResponses to set
     * @return the updated ProcessResponse object
     */
    public ProcessResponse setStepResponses(Map<String, List<EngineResponse>> stepResponses) {
        if (stepResponses != null && !stepResponses.isEmpty()) {
            stepResponses.forEach((actionKey, responses) -> status = getGlobalProcessStatusCode(responses));
        }
        this.stepResponses = stepResponses;
        return this;
    }

    /**
     * getGlobalProcessStatusCode, return the global status of workflow processing
     *
     * @param responses list of step response
     * @return the status of StatusCode type
     */
    public StatusCode getGlobalProcessStatusCode(List<EngineResponse> responses) {
        StatusCode statusCode = StatusCode.OK;

        if (responses != null) {
            for (final EngineResponse response : responses) {
                if (StatusCode.FATAL == response.getStatus()) {
                    statusCode = StatusCode.FATAL;
                    break;
                } else if (StatusCode.KO == response.getStatus()) {
                    statusCode = StatusCode.KO;
                } else if (StatusCode.WARNING == response.getStatus() && status != StatusCode.KO) {
                    statusCode = StatusCode.WARNING;
                }
            }
        }
        return statusCode;
    }

    /**
     * getGlobalProcessOutcomeMessage, return the all outcome message of workflow processing
     *
     * @param responses message list
     * @return the global message
     */
    public static String getGlobalProcessOutcomeMessage(List<EngineResponse> responses) {
        final StringBuilder globalOutcomeMessage = new StringBuilder();
        final Map<String, Integer> histogramResponse = new LinkedHashMap<>();
        if (responses != null) {
            final int totalStepError = responses.stream().mapToInt(EngineResponse::getErrorNumber).sum();
            for (final EngineResponse response : responses) {
                for (final Entry<String, OutcomeMessage> entry : response.getOutcomeMessages().entrySet()) {
                    final String key = new StringBuilder(entry.getKey())
                        .append(" ")
                        .append(response.getStatus())
                        .toString();
                    final Integer nb = histogramResponse.get(key);
                    if (nb == null) {
                        histogramResponse.put(key, 1);
                    } else {
                        histogramResponse.put(key, nb + 1);
                    }
                }
            }
            for (final Entry<String, Integer> histogramClass : histogramResponse.entrySet()) {
                globalOutcomeMessage
                    .append(histogramClass.getKey())
                    .append(" : ")
                    .append(histogramClass.getValue())
                    .append("\n");
            }
            if (totalStepError > 0) {
                globalOutcomeMessage.append(". Nombre total d'erreurs : ").append(totalStepError);
            }
        }

        if (globalOutcomeMessage.length() == 0) {
            globalOutcomeMessage.append("DefaultMessage");
        }
        return globalOutcomeMessage.toString();
    }

    /**
     * getMessageFromResponse return message id from list of response
     *
     * @param responses list of step response
     * @return message id
     */
    public static String getMessageIdentifierFromResponse(List<EngineResponse> responses) {
        String messageId = "";

        if (responses != null) {
            for (final EngineResponse response : responses) {
                if (!response.getMessageIdentifier().isEmpty()) {
                    messageId = response.getMessageIdentifier();
                }
            }
        }
        return messageId;
    }

    /**
     * implementation of getValue() of EngineResponse API class
     */
    @Override
    public String getValue() {
        if (status == null) {
            return StatusCode.FATAL.name();
        }
        return status.name();
    }

    @Override
    public String getMessageIdentifier() {
        if (messageId == null) {
            return "";
        }
        return messageId;
    }

    @Override
    public EngineResponse setMessageIdentifier(String message) {
        if (message != null) {
            messageId = message;
        }
        return this;
    }

    @Override
    public int getErrorNumber() {
        if (numberErrors == null) {
            return 0;
        }
        return numberErrors;
    }

    @Override
    public EngineResponse setErrorNumber(int number) {
        ParametersChecker.checkParameter("Detail message is a mandatory parameter", number);
        numberErrors = number;
        return this;
    }

    /**
     * @return the processId
     */
    public String getProcessId() {
        return processId;
    }

    /**
     * @param processId the processId to set
     * @return the updated ProcessResponse object
     */
    public ProcessResponse setProcessId(String processId) {
        this.processId = processId;
        return this;
    }
}
