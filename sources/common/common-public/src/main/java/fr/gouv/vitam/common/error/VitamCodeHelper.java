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

package fr.gouv.vitam.common.error;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.ParametersChecker;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper to get error message or VitamCode from Service, Domain and item or from error code
 */
public class VitamCodeHelper {

    private static final String INVALID_ARGUMENT_TO_RETRIEVE_ERROR_MESSAGE =
        "Invalid argument to retrieve error message";
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
     * @throws IllegalArgumentException thrown if the code is wrong format or if VitamCode associated to code does not
     * exists
     */
    public static VitamCode getFrom(String code) {
        ParametersChecker.checkParameter("Code cannot be null or empty", code);
        if (code.length() != VITAM_CODE_SIZE || !VitamCodeHelper.isAlphanumericCode(code)) {
            throw new IllegalArgumentException("Code must have six characters alphanumerics");
        }
        final ServiceName service = ServiceName.getFromCode(
            code.substring(START_SERVICE_INDEX, END_SERVICE_START_DOMAIN_INDEX)
        );
        final DomainName domain = DomainName.getFromCode(
            code.substring(END_SERVICE_START_DOMAIN_INDEX, END_DOMAIN_START_ITEM_INDEX)
        );
        final String item = code.substring(END_DOMAIN_START_ITEM_INDEX, END_ITEM_INDEX);

        return getFrom(service, domain, item);
    }

    /**
     * Get VitamCode from Service, Domain and item values
     *
     * @param service the service
     * @param domain the domain
     * @param item the item
     * @return VitamCode if exists
     * @throws IllegalArgumentException thrown if one argument at least is null or if VitamCode associated to the wanted
     * Service, Domain and item does not exist
     */
    public static VitamCode getFrom(ServiceName service, DomainName domain, String item) {
        for (final VitamCode vitamCode : VitamCode.values()) {
            if (
                vitamCode.getService().equals(service) &&
                vitamCode.getDomain().equals(domain) &&
                vitamCode.getItem().equals(item)
            ) {
                return vitamCode;
            }
        }
        throw new IllegalArgumentException(
            "Cannot find VitamCode from {" + service.getCode() + domain.getCode() + item + "} " + "code"
        );
    }

    /**
     * Get the message from the Service, Domain and item values
     *
     * @param service the service
     * @param domain the domain
     * @param item the item
     * @return the String message if exists
     * @throws IllegalArgumentException thrown if one argument at least is null or if VitamCode (and the message)
     * associated to the wanted Service, Domain and item does not exist
     */
    public static String getMessage(ServiceName service, DomainName domain, String item) {
        ParametersChecker.checkParameter(INVALID_ARGUMENT_TO_RETRIEVE_ERROR_MESSAGE, service, domain, item);
        return getFrom(service, domain, item).getMessage();
    }

    /**
     * Get the message from vitam code
     *
     * @param vitamCode the code
     * @return the String message if exists
     * @throws IllegalArgumentException thrown if vitamCode is null or if VitamCode (and the message) associated to the
     * wanted code does not exist
     */
    public static String getMessageFromVitamCode(VitamCode vitamCode) {
        ParametersChecker.checkParameter(INVALID_ARGUMENT_TO_RETRIEVE_ERROR_MESSAGE, vitamCode);
        return getMessage(vitamCode.getService(), vitamCode.getDomain(), vitamCode.getItem());
    }

    /**
     * Get the message from code
     *
     * @param vitamCode the code
     * @return the String message if exists
     * @throws IllegalArgumentException thrown if vitamCode is null or if VitamCode (and the message) associated to the
     * wanted code does not exist
     */
    public static String getMessage(String vitamCode) {
        ParametersChecker.checkParameter(INVALID_ARGUMENT_TO_RETRIEVE_ERROR_MESSAGE, vitamCode);
        return getFrom(vitamCode).getMessage();
    }

    /**
     * Get parameterized message from code
     *
     * @param vitamCode the code
     * @param params parameters to add to message
     * @return parameterized message
     * @throws IllegalArgumentException thrown if vitamCode or params is null if VitamCode (and the message) associated
     * to the wanted code does not exist
     */
    public static String getParametrizedMessageFromVitamCode(VitamCode vitamCode, Object... params) {
        ParametersChecker.checkParameter(INVALID_ARGUMENT_TO_RETRIEVE_ERROR_MESSAGE, vitamCode, params);
        return String.format(vitamCode.getMessage(), params);
    }

    /**
     * Get parameterized message from code
     *
     * @param vitamCode the code
     * @param params parameters to add to message
     * @return parameterized message
     * @throws IllegalArgumentException thrown if vitamCode or params is null or if VitamCode (and the message)
     * associated to the wanted code does not exist
     */
    public static String getParametrizedMessageFromCode(String vitamCode, Object... params) {
        ParametersChecker.checkParameter(INVALID_ARGUMENT_TO_RETRIEVE_ERROR_MESSAGE, vitamCode, params);
        return String.format(getMessage(vitamCode), params);
    }

    /**
     * Get parameterized message from Service, Domain and item values
     *
     * @param service the service
     * @param domain the domain
     * @param item the item
     * @param params the parameters
     * @return parameterized message
     * @throws IllegalArgumentException thrown if one argument at least is null or if VitamCode (and the message)
     * associated to the wanted code does not exist
     */
    public static String getParametrizedMessage(ServiceName service, DomainName domain, String item, Object... params) {
        ParametersChecker.checkParameter(INVALID_ARGUMENT_TO_RETRIEVE_ERROR_MESSAGE, service, domain, item);
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
        final List<VitamCode> codes = new ArrayList<>();
        for (final VitamCode vitamCode : VitamCode.values()) {
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
        final List<VitamCode> codes = new ArrayList<>();
        for (final VitamCode vitamCode : VitamCode.values()) {
            if (vitamCode.getService().equals(service)) {
                codes.add(vitamCode);
            }
        }
        return codes;
    }

    /**
     * Get the vitam code
     *
     * @param vitamCode to get
     * @return the vitam code in String
     */
    public static String getCode(VitamCode vitamCode) {
        return vitamCode.getService().getCode() + vitamCode.getDomain().getCode() + vitamCode.getItem();
    }

    /**
     * Get formatted message for Logger
     *
     * @param vitamCode the Vitam code
     * @param params parameters for the message
     * @return formatted parameterized message
     * @throws IllegalArgumentException thrown if vitamCode
     */
    public static String getLogMessage(VitamCode vitamCode, Object... params) {
        ParametersChecker.checkParameter(INVALID_ARGUMENT_TO_RETRIEVE_ERROR_MESSAGE, vitamCode);
        return String.format("[%s] %s", getCode(vitamCode), getParametrizedMessageFromVitamCode(vitamCode, params));
    }

    /**
     * Transform a vitamCode to a VitamError with the given description
     *
     * @param vitamCode the vitamCode
     * @param description the description
     * @return the vitamError
     */
    public static VitamError<JsonNode> toVitamError(VitamCode vitamCode, String description) {
        return toVitamError(vitamCode, description, JsonNode.class);
    }

    /**
     * Transform a vitamCode to a VitamError with the given description
     *
     * @param vitamCode the vitamCode
     * @param description the description
     * @param clasz the class type
     * @return the vitamError
     */
    public static <T> VitamError<T> toVitamError(
        VitamCode vitamCode,
        String description,
        @SuppressWarnings("unused") Class<T> clasz
    ) {
        return new VitamError<T>(VitamCodeHelper.getCode(vitamCode))
            .setContext(vitamCode.getService().getName())
            .setState(vitamCode.getDomain().getName())
            .setHttpCode(vitamCode.getStatus().getStatusCode())
            .setMessage(vitamCode.getMessage())
            .setDescription(description != null && !description.isEmpty() ? description : vitamCode.getMessage());
    }
}
