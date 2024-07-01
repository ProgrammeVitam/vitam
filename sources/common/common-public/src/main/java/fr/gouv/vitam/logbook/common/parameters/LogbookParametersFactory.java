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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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
public class LogbookParametersFactory {

    private static final Set<LogbookParameterName> genericMandatoryOperation = new HashSet<>();

    static {
        genericMandatoryOperation.add(LogbookParameterName.eventIdentifier);
        genericMandatoryOperation.add(LogbookParameterName.eventType);
        genericMandatoryOperation.add(LogbookParameterName.eventIdentifierProcess);
        genericMandatoryOperation.add(LogbookParameterName.eventTypeProcess);
        genericMandatoryOperation.add(LogbookParameterName.outcome);
        genericMandatoryOperation.add(LogbookParameterName.outcomeDetailMessage);
        genericMandatoryOperation.add(LogbookParameterName.eventIdentifierRequest);
    }

    private LogbookParametersFactory() {
        // do nothing
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
     * @return the default Mandatory fields set for Operation
     */
    public static Set<LogbookParameterName> getDefaultOperationMandatory() {
        return Collections.unmodifiableSet(new HashSet<>(genericMandatoryOperation));
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
}
