/**
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

package fr.gouv.vitam.logbook.common.parameters;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.guid.GUID;

/**
 * Factory to get LogbookParameters object
 *
 * Example:
 *
 * <pre>
 *     {@code
 *      // Retrieve logbook operation parameters with standard required fields
 *      LogbookOperationParameters parameters = LogbookParametersFactory.getLogbookOperationParameters();
 *
 *      // Retrieve logbook operation parameters with standard required fields and specifics required fields
 *      Set<LogbookParameterName> specificMandatoryFields = new HashSet<>()
 *      // add specific fields
 *      specificMandatoryFields.add(LogbookParameterName.objectIdentifier);
 *      specificMandatoryFields.add(LogbookParameterName.agentIdentifier);
 *
 *      // Retrieve parameter object
 *      parameters = LogbookParametersFactory.getLogbookOperationParameters(specificMandatoryFields);
 *     }
 * </pre>
 */
public class LogbookParametersFactory {

    private static final Set<LogbookParameterName> genericMandatory = new HashSet<>();

    static {
        genericMandatory.add(LogbookParameterName.eventIdentifier);
        genericMandatory.add(LogbookParameterName.eventType);
        genericMandatory.add(LogbookParameterName.eventIdentifierProcess);
        genericMandatory.add(LogbookParameterName.eventTypeProcess);
        genericMandatory.add(LogbookParameterName.outcome);
        genericMandatory.add(LogbookParameterName.outcomeDetailMessage);
        genericMandatory.add(LogbookParameterName.eventIdentifierRequest);
    }

    private LogbookParametersFactory() {
        // do nothing
    }

    /**
     * Get a new LogbookOperationParamaters object
     *
     * @param mandatoryFieldsToAdd set of LogbookParameterName to add to the default mandatory fields, can be null
     * @return the LogbookOperationParameters
     */
    public static LogbookOperationParameters newLogbookOperationParameters(
        Set<LogbookParameterName> mandatoryFieldsToAdd) {
        return new LogbookOperationParameters(
            initLogbookMandatoriesParameters(mandatoryFieldsToAdd));
    }

    /**
     * Get a new LogbookOperationParameters object
     *
     * @return the LogbookOperationParameters
     */
    public static LogbookOperationParameters newLogbookOperationParameters() {
        return new LogbookOperationParameters(
            initLogbookMandatoriesParameters(null));
    }

    /**
     * Get a new LogbookOperationParameters object
     *
     * @param eventIdentifier
     * @param eventType
     * @param eventIdentifierProcess
     * @param eventTypeProcess
     * @param outcome
     * @param outcomeDetailMessage
     * @param eventIdentifierRequest
     *
     * @return the LogbookOperationParameters
     * @throws IllegalArgumentException if any parameter is null or empty
     * @deprecated Use the other using more constrained values
     */
    @Deprecated
    public static LogbookOperationParameters newLogbookOperationParameters(String eventIdentifier,
        String eventType, String eventIdentifierProcess, LogbookTypeProcess eventTypeProcess,
        LogbookOutcome outcome, String outcomeDetailMessage,
        String eventIdentifierRequest) {
        ParametersChecker.checkParameter("No parameter can be null or empty", eventIdentifier,
            eventType, eventIdentifierProcess, outcomeDetailMessage, eventIdentifierRequest);
        ParametersChecker.checkParameter("No parameter can be null or empty", outcome, eventTypeProcess);
        final LogbookOperationParameters parameters =
            new LogbookOperationParameters(initLogbookMandatoriesParameters(null));
        return parameters.putParameterValue(LogbookParameterName.eventIdentifier, eventIdentifier)
            .putParameterValue(LogbookParameterName.eventType, eventType)
            .putParameterValue(LogbookParameterName.eventIdentifierProcess, eventIdentifierProcess)
            .setTypeProcess(eventTypeProcess)
            .setStatus(outcome)
            .putParameterValue(LogbookParameterName.outcomeDetailMessage, outcomeDetailMessage)
            .putParameterValue(LogbookParameterName.eventIdentifierRequest, eventIdentifierRequest);
    }

    /**
     * Get a new LogbookOperationParameters object
     *
     * @param eventIdentifier
     * @param eventType
     * @param eventIdentifierProcess
     * @param eventTypeProcess
     * @param outcome
     * @param outcomeDetailMessage
     * @param eventIdentifierRequest
     *
     * @return the LogbookOperationParameters
     * @throws IllegalArgumentException if any parameter is null or empty
     */
    public static LogbookOperationParameters newLogbookOperationParameters(GUID eventIdentifier,
        String eventType, GUID eventIdentifierProcess, LogbookTypeProcess eventTypeProcess,
        LogbookOutcome outcome, GUID outcomeDetailMessage,
        GUID eventIdentifierRequest) {
        ParametersChecker.checkParameter("No parameter can be null or empty", eventIdentifier,
            eventType, eventIdentifierProcess, outcomeDetailMessage, eventIdentifierRequest);
        ParametersChecker.checkParameter("No parameter can be null or empty", outcome, eventTypeProcess);
        final LogbookOperationParameters parameters =
            new LogbookOperationParameters(initLogbookMandatoriesParameters(null));
        return parameters.putParameterValue(LogbookParameterName.eventIdentifier, eventIdentifier.getId())
            .putParameterValue(LogbookParameterName.eventType, eventType)
            .putParameterValue(LogbookParameterName.eventIdentifierProcess, eventIdentifierProcess.getId())
            .setTypeProcess(eventTypeProcess)
            .setStatus(outcome)
            .putParameterValue(LogbookParameterName.outcomeDetailMessage, outcomeDetailMessage.getId())
            .putParameterValue(LogbookParameterName.eventIdentifierRequest, eventIdentifierRequest.getId());
    }

    /**
     *
     * @return the default Mandatory fields set
     */
    public static Set<LogbookParameterName> getDefaultMandatory() {
        return Collections.unmodifiableSet(new HashSet<>(genericMandatory));
    }

    private static Set<LogbookParameterName> initLogbookMandatoriesParameters(
        Set<LogbookParameterName> mandatoryFieldsToAdd) {
        final Set<LogbookParameterName> mandatory = new HashSet<>(genericMandatory);
        if (mandatoryFieldsToAdd != null) {
            mandatory.addAll(mandatoryFieldsToAdd);
        }
        return Collections.unmodifiableSet(mandatory);
    }
}
