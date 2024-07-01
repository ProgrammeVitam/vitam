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
package fr.gouv.vitam.logbook.common.parameters;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.VitamSession;
import fr.gouv.vitam.common.thread.VitamThreadUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static fr.gouv.vitam.common.i18n.VitamLogbookMessages.getOutcomeDetail;

/**
 * Logbook parameters factory </br>
 *
 * Factory to get LogbookParameters object </br>
 *
 * Example:
 *
 * <pre>
 *     {@code
 *      // Retrieves logbook operation parameters with standard required fields
 *      LogbookOperationParameters parameters = LogbookParametersFactory.getLogbookOperationParameters();
 *
 *      // Retrieves logbook operation parameters with standard required fields and specifics required fields
 *      Set<LogbookParameterName> specificMandatoryFields = new HashSet<>()
 *      // add specific fields
 *      specificMandatoryFields.add(LogbookParameterName.objectIdentifier);
 *      specificMandatoryFields.add(LogbookParameterName.agentIdentifier);
 *
 *      // Retrieves parameter object
 *      parameters = LogbookParametersFactory.getLogbookOperationParameters(specificMandatoryFields);
 *     }
 * </pre>
 */

// TODO P1 : see WorkerParametersFactory and create a common factory
// for all "vitam parameters" using generic (for example)
public class LogbookParameterHelper {

    private static final String NO_PARAMETER_CAN_BE_NULL_OR_EMPTY = "No parameter can be null or empty";
    private static final Set<LogbookParameterName> genericMandatoryOperation;
    private static final Set<LogbookParameterName> genericMandatoryLifeCycle = new HashSet<>();

    static {
        genericMandatoryOperation = LogbookParametersFactory.getDefaultOperationMandatory();

        genericMandatoryLifeCycle.add(LogbookParameterName.eventIdentifier);
        genericMandatoryLifeCycle.add(LogbookParameterName.eventType);
        genericMandatoryLifeCycle.add(LogbookParameterName.eventIdentifierProcess);
        genericMandatoryLifeCycle.add(LogbookParameterName.eventTypeProcess);
        genericMandatoryLifeCycle.add(LogbookParameterName.outcome);
        genericMandatoryLifeCycle.add(LogbookParameterName.outcomeDetail);
        genericMandatoryLifeCycle.add(LogbookParameterName.outcomeDetailMessage);
        genericMandatoryLifeCycle.add(LogbookParameterName.objectIdentifier);
    }

    private LogbookParameterHelper() {
        // do nothing
    }

    /**
     * Get a new Empty LogbookOperationParamaters object
     *
     * @param mandatoryFieldsToAdd set of LogbookParameterName to add to the default mandatory fields, can be null
     * @return the LogbookOperationParameters
     */
    static LogbookOperationParameters newLogbookOperationParameters(Set<LogbookParameterName> mandatoryFieldsToAdd) {
        return new LogbookOperationParameters(initLogbookOperationMandatoriesParameters(mandatoryFieldsToAdd));
    }

    /**
     * Get a new Empty LogbookLifeCycleUnitParameters object
     *
     * @param mandatoryFieldsToAdd set of LogbookParameterName to add to the default mandatory fields, can be null
     * @return the LogbookLifeCycleUnitParameters
     */
    static LogbookLifeCycleUnitParameters newLogbookLifeCycleUnitParameters(
        Set<LogbookParameterName> mandatoryFieldsToAdd
    ) {
        return new LogbookLifeCycleUnitParameters(initLogbookLifeCycleMandatoriesParameters(mandatoryFieldsToAdd));
    }

    /**
     * Get a new Empty LogbookLifeCycleObjectGroupParameters object
     *
     * @param mandatoryFieldsToAdd set of LogbookParameterName to add to the default mandatory fields, can be null
     * @return the LogbookLifeCycleObjectGroupParameters
     */
    static LogbookLifeCycleObjectGroupParameters newLogbookLifeCycleObjectGroupParameters(
        Set<LogbookParameterName> mandatoryFieldsToAdd
    ) {
        return new LogbookLifeCycleObjectGroupParameters(
            initLogbookLifeCycleMandatoriesParameters(mandatoryFieldsToAdd)
        );
    }

    /**
     * Get a new Empty LogbookOperationParameters object. <br>
     * Use in internal assignment. Not recommended in general usage.
     *
     * @return the LogbookOperationParameters
     */
    public static LogbookOperationParameters newLogbookOperationParameters() {
        return new LogbookOperationParameters(initLogbookOperationMandatoriesParameters(null));
    }

    /**
     * Get a new Empty LogbookLifeCycleUnitParameters object. <br>
     * Use in internal assignment. Not recommended in general usage.
     *
     * @return the LogbookLifeCycleUnitParameters
     */
    public static LogbookLifeCycleUnitParameters newLogbookLifeCycleUnitParameters() {
        return new LogbookLifeCycleUnitParameters(initLogbookLifeCycleMandatoriesParameters(null));
    }

    /**
     * Get a new Empty LogbookLifeCycleObjectGroupParameters object. <br>
     * Use in internal assignment. Not recommended in general usage.
     *
     * @return the LogbookLifeCycleObjectGroupParameters
     */
    public static LogbookLifeCycleObjectGroupParameters newLogbookLifeCycleObjectGroupParameters() {
        return new LogbookLifeCycleObjectGroupParameters(initLogbookLifeCycleMandatoriesParameters(null));
    }

    /**
     * Get a new LogbookOperationParameters object
     *
     * @param eventIdentifier the event id of LogbookOperationParameters to create
     * @param eventType the event type of LogbookOperationParameters to create
     * @param eventIdentifierProcess the event id process of LogbookOperationParameters to create
     * @param eventTypeProcess the event type process of LogbookOperationParameters to create
     * @param outcome the outcome of LogbookOperationParameters to create
     * @param outcomeDetailMessage the outcome detail message of of LogbookOperationParameters to create
     * @param eventIdentifierRequest the event id request of LogbookOperationParameters to create
     * @return the LogbookOperationParameters
     * @throws IllegalArgumentException if any parameter is null or empty
     */
    public static LogbookOperationParameters newLogbookOperationParameters(
        GUID eventIdentifier,
        String eventType,
        GUID eventIdentifierProcess,
        LogbookTypeProcess eventTypeProcess,
        StatusCode outcome,
        String outcomeDetailMessage,
        GUID eventIdentifierRequest
    ) {
        ParametersChecker.checkParameter(
            NO_PARAMETER_CAN_BE_NULL_OR_EMPTY,
            eventIdentifier,
            eventIdentifierProcess,
            eventIdentifierRequest,
            outcome,
            eventTypeProcess
        );
        ParametersChecker.checkParameter(NO_PARAMETER_CAN_BE_NULL_OR_EMPTY, eventType, outcomeDetailMessage);
        final LogbookOperationParameters parameters = newLogbookOperationParameters();

        final VitamSession vitamSession = VitamThreadUtils.getVitamSession();
        String applicationSessionId = vitamSession.getApplicationSessionId();
        String contextId = vitamSession.getContextId();
        String personalCertificate = vitamSession.getPersonalCertificate();

        return (LogbookOperationParameters) parameters
            .putParameterValue(LogbookParameterName.eventIdentifier, eventIdentifier.getId())
            .putParameterValue(LogbookParameterName.eventType, eventType)
            .putParameterValue(LogbookParameterName.eventIdentifierProcess, eventIdentifierProcess.getId())
            .setTypeProcess(eventTypeProcess)
            .setStatus(outcome)
            .putParameterValue(LogbookParameterName.outcomeDetailMessage, outcomeDetailMessage)
            .putParameterValue(LogbookParameterName.eventIdentifierRequest, eventIdentifierRequest.getId())
            .putParameterValue(LogbookParameterName.outcomeDetail, getOutcomeDetail(eventType, outcome))
            .putParameterValue(LogbookParameterName.objectIdentifier, eventIdentifierProcess.getId())
            .putParameterValue(LogbookParameterName.agentIdentifierApplication, contextId)
            .putParameterValue(LogbookParameterName.agentIdentifierApplicationSession, applicationSessionId)
            .putParameterValue(LogbookParameterName.agentIdentifierPersonae, personalCertificate);
    }

    /**
     * Get a new LogbookOperationParameters object for subtask
     *
     * @param eventIdentifier the event id of LogbookOperationParameters to create
     * @param eventType the event type of LogbookOperationParameters to create
     * @param eventIdentifierProcess the event id process of LogbookOperationParameters to create
     * @param eventTypeProcess the event type process of LogbookOperationParameters to create
     * @param outcome the outcome of LogbookOperationParameters to create
     * @param subtask may be null
     * @param appendedDetailMessage may be null, the detail message
     * @param eventIdentifierRequest the event id request of LogbookOperationParameters to create
     * @return the LogbookOperationParameters
     * @throws IllegalArgumentException if any parameter is null or empty
     */
    public static LogbookOperationParameters newLogbookOperationParameters(
        GUID eventIdentifier,
        String eventType,
        GUID eventIdentifierProcess,
        LogbookTypeProcess eventTypeProcess,
        StatusCode outcome,
        String subtask,
        String appendedDetailMessage,
        GUID eventIdentifierRequest
    ) {
        ParametersChecker.checkParameter(
            NO_PARAMETER_CAN_BE_NULL_OR_EMPTY,
            eventIdentifier,
            eventIdentifierProcess,
            eventIdentifierRequest,
            outcome,
            eventTypeProcess
        );
        ParametersChecker.checkParameter(NO_PARAMETER_CAN_BE_NULL_OR_EMPTY, eventType);

        final VitamSession vitamSession = VitamThreadUtils.getVitamSession();
        String applicationSessionId = vitamSession.getApplicationSessionId();
        String contextId = vitamSession.getContextId();

        final LogbookOperationParameters parameters = newLogbookOperationParameters();
        parameters
            .putParameterValue(LogbookParameterName.eventIdentifier, eventIdentifier.getId())
            .putParameterValue(LogbookParameterName.eventIdentifierProcess, eventIdentifierProcess.getId())
            .setTypeProcess(eventTypeProcess)
            .putParameterValue(LogbookParameterName.eventIdentifierRequest, eventIdentifierRequest.getId())
            .putParameterValue(LogbookParameterName.agentIdentifierApplication, contextId)
            .putParameterValue(LogbookParameterName.agentIdentifierApplicationSession, applicationSessionId);
        parameters.setFinalStatus(eventType, subtask, outcome, appendedDetailMessage);
        return parameters;
    }

    /**
     * Get a new LogbookLifeCycleUnitParameters object
     *
     * @param eventIdentifier the event id of LogbookLifeCycleUnitParameters to create
     * @param eventType the event type of LogbookLifeCycleUnitParameters to create
     * @param eventIdentifierProcess the event id process of LogbookLifeCycleUnitParameters to create
     * @param eventTypeProcess the event type process of LogbookLifeCycleUnitParameters to create
     * @param outcome the outcome of LogbookLifeCycleUnitParameters to create
     * @param outcomeDetail the outcome detail of LogbookLifeCycleUnitParameters to create
     * @param outcomeDetailMessage the outcome detail message of LogbookLifeCycleUnitParameters to create
     * @param objectIdentifier the object id of LogbookLifeCycleUnitParameters to create
     * @return the LogbookLifeCycleUnitParameters
     * @throws IllegalArgumentException if any parameter is null or empty
     */
    public static LogbookLifeCycleUnitParameters newLogbookLifeCycleUnitParameters(
        GUID eventIdentifier,
        String eventType,
        GUID eventIdentifierProcess,
        LogbookTypeProcess eventTypeProcess,
        StatusCode outcome,
        String outcomeDetail,
        String outcomeDetailMessage,
        GUID objectIdentifier
    ) {
        ParametersChecker.checkParameter(
            NO_PARAMETER_CAN_BE_NULL_OR_EMPTY,
            eventIdentifier,
            eventIdentifierProcess,
            eventTypeProcess,
            outcome,
            objectIdentifier
        );
        ParametersChecker.checkParameter(
            NO_PARAMETER_CAN_BE_NULL_OR_EMPTY,
            eventType,
            outcomeDetail,
            outcomeDetailMessage
        );
        final LogbookLifeCycleUnitParameters parameters = newLogbookLifeCycleUnitParameters();
        return (LogbookLifeCycleUnitParameters) parameters
            .putParameterValue(LogbookParameterName.eventIdentifier, eventIdentifier.getId())
            .putParameterValue(LogbookParameterName.eventType, eventType)
            .putParameterValue(LogbookParameterName.eventIdentifierProcess, eventIdentifierProcess.getId())
            .setTypeProcess(eventTypeProcess)
            .setStatus(outcome)
            .putParameterValue(LogbookParameterName.outcomeDetail, outcomeDetail)
            .putParameterValue(LogbookParameterName.outcomeDetailMessage, outcomeDetailMessage)
            .putParameterValue(LogbookParameterName.objectIdentifier, objectIdentifier.getId());
    }

    /**
     * Get a new LogbookLifeCycleObjectGroupParameters object
     *
     * @param eventIdentifier the event id of LogbookLifeCycleObjectGroupParameters to create
     * @param eventType the event type of LogbookLifeCycleObjectGroupParameters to create
     * @param eventIdentifierProcess the event id process of LogbookLifeCycleObjectGroupParameters to create
     * @param eventTypeProcess the event type process of LogbookLifeCycleObjectGroupParameters to create
     * @param outcome the outcome of LogbookLifeCycleObjectGroupParameters to create
     * @param outcomeDetail the outcome detail of LogbookLifeCycleObjectGroupParameters to create
     * @param outcomeDetailMessage the outcome detail message of LogbookLifeCycleObjectGroupParameters to create
     * @param objectIdentifier the object id of LogbookLifeCycleObjectGroupParameters to create
     * @return the LogbookLifeCycleObjectGroupParameters
     * @throws IllegalArgumentException if any parameter is null or empty
     */
    public static LogbookLifeCycleObjectGroupParameters newLogbookLifeCycleObjectGroupParameters(
        GUID eventIdentifier,
        String eventType,
        GUID eventIdentifierProcess,
        LogbookTypeProcess eventTypeProcess,
        StatusCode outcome,
        String outcomeDetail,
        String outcomeDetailMessage,
        GUID objectIdentifier
    ) {
        ParametersChecker.checkParameter(
            NO_PARAMETER_CAN_BE_NULL_OR_EMPTY,
            eventIdentifier,
            eventIdentifierProcess,
            eventTypeProcess,
            outcome,
            objectIdentifier
        );
        ParametersChecker.checkParameter(
            NO_PARAMETER_CAN_BE_NULL_OR_EMPTY,
            eventType,
            outcomeDetail,
            outcomeDetailMessage
        );
        final LogbookLifeCycleObjectGroupParameters parameters = newLogbookLifeCycleObjectGroupParameters();
        return (LogbookLifeCycleObjectGroupParameters) parameters
            .putParameterValue(LogbookParameterName.eventIdentifier, eventIdentifier.getId())
            .putParameterValue(LogbookParameterName.eventType, eventType)
            .putParameterValue(LogbookParameterName.eventIdentifierProcess, eventIdentifierProcess.getId())
            .setTypeProcess(eventTypeProcess)
            .setStatus(outcome)
            .putParameterValue(LogbookParameterName.outcomeDetail, outcomeDetail)
            .putParameterValue(LogbookParameterName.outcomeDetailMessage, outcomeDetailMessage)
            .putParameterValue(LogbookParameterName.objectIdentifier, objectIdentifier.getId());
    }

    /**
     * @return the default Mandatory fields set for LifeCycle
     */
    public static Set<LogbookParameterName> getDefaultLifeCycleMandatory() {
        return Collections.unmodifiableSet(new HashSet<>(genericMandatoryLifeCycle));
    }

    /**
     * @param mandatoryFieldsToAdd
     * @return the new Set of parameter names
     */
    private static Set<LogbookParameterName> initLogbookOperationMandatoriesParameters(
        Set<LogbookParameterName> mandatoryFieldsToAdd
    ) {
        final Set<LogbookParameterName> mandatory = new HashSet<>(genericMandatoryOperation);
        if (mandatoryFieldsToAdd != null) {
            mandatory.addAll(mandatoryFieldsToAdd);
        }
        return Collections.unmodifiableSet(mandatory);
    }

    /**
     * @param mandatoryFieldsToAdd
     * @return the new Set of parameter names
     */
    private static Set<LogbookParameterName> initLogbookLifeCycleMandatoriesParameters(
        Set<LogbookParameterName> mandatoryFieldsToAdd
    ) {
        final Set<LogbookParameterName> mandatory = new HashSet<>(genericMandatoryLifeCycle);
        if (mandatoryFieldsToAdd != null) {
            mandatory.addAll(mandatoryFieldsToAdd);
        }
        return Collections.unmodifiableSet(mandatory);
    }
}
