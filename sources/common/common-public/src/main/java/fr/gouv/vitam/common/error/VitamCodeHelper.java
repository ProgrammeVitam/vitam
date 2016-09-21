/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital
 * archiving back-office system managing high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL 2.1
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL 2.1 license and that you accept its terms.
 */

package fr.gouv.vitam.common.error;

import java.util.ArrayList;
import java.util.List;

import fr.gouv.vitam.common.ParametersChecker;

/**
 * Helper to get error message or VitamCode from Service, Domain and item or from error code
 */
public class VitamCodeHelper {

    private static final int START_SERVICE_INDEX = 0;
    private static final int END_SERVICE_START_DOMAIN_INDEX = 2;
    private static final int END_DOMAIN_START_ITEM_INDEX = 4;
    private static final int END_ITEM_INDEX = 6;
    private static final int VITAM_CODE_SIZE = 6;

    private VitamCodeHelper() {
        // Nothing
    }

    /**
     * Check if the code is alphanumeric [A-Z0-9]
     *
     * @param code the code to check
     * @return true if code is aplhanumeric, false otherwise
     */
    public static boolean isAlphanumericCode(String code) {
        char c;
        for (int i = 0; i < code.length(); i++) {
            c = code.charAt(i);
            if ((c < 0x30 || c > 0x39) && (c < 0x41 || c > 0x5a)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get VitamCode from code
     *
     * @param code the code to get VitamCode
     * @return VitamCode if exists
     * @throws IllegalArgumentException thrown if the code is wrong format or if VitamCode associated to code does
     *                                  not exists
     */
    public static VitamCode getFrom(String code) {
        ParametersChecker.checkParameter("Code cannot be null or empty", code);
        if (code.length() != VITAM_CODE_SIZE || !VitamCodeHelper.isAlphanumericCode(code)) {
            throw new IllegalArgumentException("Code must have six characters alphanumerics");
        }
        ServiceName service =
            ServiceName.getFromCode(code.substring(START_SERVICE_INDEX, END_SERVICE_START_DOMAIN_INDEX));
        DomainName domain =
            DomainName.getFromCode(code.substring(END_SERVICE_START_DOMAIN_INDEX, END_DOMAIN_START_ITEM_INDEX));
        String item = code.substring(END_DOMAIN_START_ITEM_INDEX, END_ITEM_INDEX);

        return getFrom(service, domain, item);
    }

    /**
     * Get VitamCode from Service, Domain and item values
     *
     * @param service the service
     * @param domain  the domain
     * @param item    the item
     * @return VitamCode if exists
     * @throws IllegalArgumentException thrown if one argument at least is null or if VitamCode associated
     *                                  to the wanted Service, Domain and item does not exist
     */
    public static VitamCode getFrom(ServiceName service, DomainName domain, String item) {
        for (VitamCode vitamCode : VitamCode.values()) {
            if (vitamCode.getService().equals(service) && vitamCode.getDomain().equals(domain) && vitamCode.getItem()
                .equals(item)) {
                return vitamCode;
            }
        }
        throw new IllegalArgumentException(
            "Cannot find VitamCode from {" + service.getCode() + domain.getCode() + item + "} " +
                "code");
    }

    /**
     * Get the message from the Service, Domain and item values
     *
     * @param service the service
     * @param domain  the domain
     * @param item    the item
     * @return the String message if exists
     * @throws IllegalArgumentException thrown if one argument at least is null or if VitamCode (and the
     *                                  message) associated to the wanted Service, Domain and item does not exist
     */
    public static String getMessage(ServiceName service, DomainName domain, String item) {
        ParametersChecker.checkParameter("Invalid argument to retrieve error message", service, domain, item);
        return getFrom(service, domain, item).getMessage();
    }

    /**
     * Get the message from vitam code
     *
     * @param vitamCode the code
     * @return the String message if exists
     * @throws IllegalArgumentException thrown if vitamCode is null or if VitamCode (and the
     *                                  message) associated to the wanted code does not exist
     */
    public static String getMessageFromVitamCode(VitamCode vitamCode) {
        ParametersChecker.checkParameter("Invalid argument to retrieve error message", vitamCode);
        return getMessage(vitamCode.getService(), vitamCode.getDomain(), vitamCode.getItem());
    }
    
    /**
     * Get the message from code
     *
     * @param vitamCode the code
     * @return the String message if exists
     * @throws IllegalArgumentException thrown if vitamCode is null or if VitamCode (and the
     *                                  message) associated to the wanted code does not exist
     */
    public static String getMessage(String vitamCode) {
        ParametersChecker.checkParameter("Invalid argument to retrieve error message", vitamCode);
        return getFrom(vitamCode).getMessage();
    }

    /**
     * Get parametrized message from code
     *
     * @param vitamCode the code
     * @param params    parameters to add to message
     * @return parametrized message
     * @throws IllegalArgumentException thrown if vitamCode or params is null if VitamCode (and the
     *                                  message) associated to the wanted code does not exist
     */
    public static String getParametrizedMessageFromVitamCode(VitamCode vitamCode, String... params) {
        ParametersChecker.checkParameter("Invalid argument to retrieve error message", vitamCode, params);
        return String.format(vitamCode.getMessage(), params);
    }

    /**
     * Get parametrized message from code
     *
     * @param vitamCode the code
     * @param params    parameters to add to message
     * @return parametrized message
     * @throws IllegalArgumentException thrown if vitamCode or params is null or if VitamCode (and the
     *                                  message) associated to the wanted code does not exist
     */
    public static String getParametrizedMessageFromCode(String vitamCode, String... params) {
        ParametersChecker.checkParameter("Invalid argument to retrieve error message", vitamCode, params);
        return String.format(getMessage(vitamCode), params);
    }

    /**
     * Get parametrized message from Service, Domain and item values
     *
     * @param service the service
     * @param domain  the domain
     * @param item    the item
     * @param params  the parameters
     * @return parametrized message
     * @throws IllegalArgumentException thrown if one argument at least is null or if VitamCode (and the
     *                                  message) associated to the wanted code does not exist
     */
    public static String getParametrizedMessage(ServiceName service, DomainName domain, String item, String... params) {
        ParametersChecker.checkParameter("Invalid argument to retrieve error message", service, domain, item);
        return String.format(getFrom(service, domain, item).getMessage(), params);
    }

    /**
     * Get list of VitamCode from a Domain
     *
     * @param domain the domain
     * @return list of VitamCode or empty list
     * @throws IllegalArgumentException thrown if domain is null
     */
    public static List<VitamCode> getFromDomain(DomainName domain) {
        ParametersChecker.checkParameter("Invalid argument to retrieve Vitam code", domain);
        List<VitamCode> codes = new ArrayList<>();
        for (VitamCode vitamCode : VitamCode.values()) {
            if (vitamCode.getDomain().equals(domain)) {
                codes.add(vitamCode);
            }
        }
        return codes;
    }

    /**
     * Get list of VitamCode from a Service
     *
     * @param service the service
     * @return list of VitamCode or empty list
     * @throws IllegalArgumentException thrown if service is null
     */
    public static List<VitamCode> getFromService(ServiceName service) {
        ParametersChecker.checkParameter("Invalid argument to retrieve Vitam code", service);
        List<VitamCode> codes = new ArrayList<>();
        for (VitamCode vitamCode : VitamCode.values()) {
            if (vitamCode.getService().equals(service)) {
                codes.add(vitamCode);
            }
        }
        return codes;
    }

    /**
     * Get the vitam code
     *
     * @return the vitam code in String
     */
    public static String getCode(VitamCode vitamCode) {
        StringBuilder sb = new StringBuilder();
        sb.append(vitamCode.getService().getCode());
        sb.append(vitamCode.getDomain().getCode());
        sb.append(vitamCode.getItem());
        return sb.toString();
    }

    /**
     * Get formatted message for Logger
     *
     * @param vitamCode the Vitam code
     * @param params parameters for the message
     * @return formatted parametrized message
     * @throws IllegalArgumentException thrown if vitamCode
     */
    public static String getLogMessage(VitamCode vitamCode, String... params) {
        ParametersChecker.checkParameter("Invalid argument to retrieve error message", vitamCode);
        return String.format("[%s] %s", getCode(vitamCode), getParametrizedMessageFromVitamCode(vitamCode, params));
    }
}
