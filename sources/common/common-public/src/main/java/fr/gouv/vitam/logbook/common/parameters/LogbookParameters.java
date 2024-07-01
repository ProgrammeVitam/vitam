/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
package fr.gouv.vitam.logbook.common.parameters;

import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.parameter.VitamParameter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Logbook parameters
 */
public interface LogbookParameters extends VitamParameter<LogbookParameterName> {
    /**
     * The EventDateTime is set by the Logbook methods during creation or append
     *
     * @return the associated EventDateTime if set (or null if not set yet)
     */
    LocalDateTime getEventDateTime();

    /**
     * Set the outcome status
     *
     * @param outcome
     * @return this
     */
    LogbookParameters setStatus(StatusCode outcome);

    /**
     * Get the outcome status
     *
     * @return the status (or null if not set yet)
     * @throws IllegalArgumentException if the status is with incorrect value
     */
    StatusCode getStatus();

    /**
     * Set the process type
     *
     * @param process process type
     * @return this
     */
    LogbookParameters setTypeProcess(LogbookTypeProcess process);

    /**
     * Get the process type
     *
     * @return the process type (or null if not set yet)
     * @throws IllegalArgumentException if the process type is with incorrect value
     */
    LogbookTypeProcess getTypeProcess();

    /**
     * Put parameterValue on mapParamaters with parameterName key <br />
     * <br />
     * If parameterKey already exists, the override it (no check)
     *
     * @param parameterName the key of the parameter to put on the parameter map
     * @param parameterValue the value to put on the parameter map
     * @return actual instance of LogbookParameters (fluent like)
     * @throws IllegalArgumentException if the parameterName is null or if the parameterValue cannot be null or empty
     */
    LogbookParameters putParameterValue(LogbookParameterName parameterName, String parameterValue);

    /**
     * Get the parameter according to the parameterName
     *
     * @param parameterName
     * @return the value or null if not found
     * @throws IllegalArgumentException if the parameterName is null
     */
    String getParameterValue(LogbookParameterName parameterName);

    /**
     * Set from map using String as Key
     *
     * @param map
     * @return this
     */
    LogbookParameters setMap(Map<String, String> map);

    /**
     * Set from another LogbookParameters
     *
     * @param parameters
     * @return this
     */
    LogbookParameters setFromParameters(LogbookParameters parameters);

    /**
     * Update the current LogbookParameters with status and message
     *
     * @param handlerId the Handler Id
     * @param subTaskId the subTask Id if any (may be null)
     * @param code the Status CodeAdminManagementClientRestTest
     * @param additionalMessage the additional message (as " Details= ...") if any (may be null)
     * @param params the additional parameters for the message if any (may be null)
     * @return this
     */
    LogbookParameters setFinalStatus(
        String handlerId,
        String subTaskId,
        StatusCode code,
        String additionalMessage,
        String... params
    );

    /**
     * Update the current LogbookParameters with status and message
     *
     * @param handlerId the Handler Id
     * @param subTaskId the subTask Id if any (may be null)
     * @param additionalMessage the additional message (as " Details= ...") if any (may be null)
     * @param params the additional parameters for the message if any (may be null)
     * @return this
     */
    LogbookParameters setBeginningLog(String handlerId, String subTaskId, String additionalMessage, String... params);
}
