/*
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
package fr.gouv.vitam.common.parameter;

import java.util.Map;
import java.util.Set;

import com.google.common.base.Strings;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.thread.VitamThreadUtils;

/**
 * Parameters helper use to check required parameters
 */
public class ParameterHelper {

    private static final String NO_TENANT_ID = "Tenant id should be filled";

    private ParameterHelper() {
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
    public static <T extends Enum<T>> void checkNullOrEmptyParameter(T key, String value,
        Set<T> mandatories) {
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
    public static <T extends Enum<T>> void checkNullOrEmptyParameters(Map<T, String> parameters,
        Set<T> mandatories) {
        ParametersChecker.checkParameter("Check Or null parameter", parameters, mandatories);
        for (final T key : mandatories) {
            if (Strings.isNullOrEmpty(parameters.get(key))) {
                throw new IllegalArgumentException(key + " parameter cannot be null or empty");
            }
        }
    }

    /**
     * Check parameters emptiness or nullity
     *
     * @param parameters the template of vitam parameter
     * @throws IllegalArgumentException if an argument is null or empty against mandatory
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <T extends VitamParameter> void checkNullOrEmptyParameters(T parameters) {
        ParametersChecker.checkParameter("Check Or null parameter", parameters);
        checkNullOrEmptyParameters(parameters.getMapParameters(), parameters.getMandatoriesParameters());
    }

    /**
     * Get the tenant parameter from the Vitam Session
     *  getTenantParameter
     * @return the tenantId
     * @throws IllegalArgumentException if the tenant Id is not found in the session
     * @throws fr.gouv.vitam.common.exception.VitamThreadAccessException if there is no VitamThread
     */
    public static Integer getTenantParameter() {
        ParametersChecker.checkParameter("No session in Thread", VitamThreadUtils.getVitamSession());
        ParametersChecker.checkParameter(NO_TENANT_ID, VitamThreadUtils.getVitamSession().getTenantId());
        return VitamThreadUtils.getVitamSession().getTenantId();
    }

}
