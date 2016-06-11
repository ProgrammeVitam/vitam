/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.logbook.common.parameters.helper;

import java.util.Map;
import java.util.Set;

import com.google.common.base.Strings;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameters;

/**
 * Logbook parameters helper use to check required parameters
 */
public class LogbookParametersHelper {

    private LogbookParametersHelper() {
        // Do nothing
    }

    /**
     * Check parameter emptiness or nullity
     *
     * @param key the attribute name
     * @param value the attribute value to check
     * @param mandatories the set of mandatories field
     * @throws IllegalArgumentException if an argument is null or empty against mandatory
     */
    public static void checkNullOrEmptyParameter(LogbookParameterName key, String value,
        Set<LogbookParameterName> mandatories) {
        ParametersChecker.checkParameter("Key parameter", key);
        if (mandatories.contains(key)) {
            ParametersChecker.checkParameter(key.name(), value);
        }
    }

    /**
     * Check parameters emptiness or nullity
     *
     * @param parameters the map parameters (key = attribute name, value = attribute value)
     * @param mandatories the set of mandatories field
     * @throws IllegalArgumentException if an argument is null or empty against mandatory
     */
    public static void checkNullOrEmptyParameters(Map<LogbookParameterName, String> parameters,
        Set<LogbookParameterName> mandatories) {
        ParametersChecker.checkParameter("Check Or null parameter", parameters, mandatories);
        for (final LogbookParameterName key : mandatories) {
            if (Strings.isNullOrEmpty(parameters.get(key))) {
                throw new IllegalArgumentException(key + " parameter cannot be null or empty");
            }
        }
    }

    /**
     * Check parameters emptiness or nullity
     *
     * @param parameters
     * @throws IllegalArgumentException if an argument is null or empty against mandatory
     */
    public static void checkNullOrEmptyParameters(LogbookParameters parameters) {
        ParametersChecker.checkParameter("Check Or null parameter", parameters);
        checkNullOrEmptyParameters(parameters.getMapParameters(), parameters.getMandatoriesParameters());
    }
}
