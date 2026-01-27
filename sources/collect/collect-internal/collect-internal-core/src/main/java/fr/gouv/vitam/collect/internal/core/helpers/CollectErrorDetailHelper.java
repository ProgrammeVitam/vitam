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

package fr.gouv.vitam.collect.internal.core.helpers;

import fr.gouv.vitam.collect.common.exception.CollectInternalSingleErrorsDetailException;
import fr.gouv.vitam.collect.internal.core.common.CollectErrorMessagesEnum;
import fr.gouv.vitam.collect.internal.core.common.CollectErrorParamEnum;
import fr.gouv.vitam.common.error.VitamErrorDetails;
import org.apache.commons.text.StringSubstitutor;

import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class CollectErrorDetailHelper {

    private static final ResourceBundle errorMessagesBundle = ResourceBundle.getBundle("collect-error-messages");

    public static VitamErrorDetails generateVitamErrorsDetails(
        CollectErrorMessagesEnum error,
        Map<CollectErrorParamEnum, String> params
    ) {
        VitamErrorDetails details = new VitamErrorDetails();
        details.setKey(error.name());
        if (params != null) {
            details.setArgs(
                params.entrySet().stream().collect(Collectors.toMap(e -> e.getKey().name(), Map.Entry::getValue))
            );
        }
        return details;
    }

    public static String generateErrorMessage(
        CollectErrorMessagesEnum errorMessageKey,
        Map<CollectErrorParamEnum, String> errorMessageParams
    ) {
        String errorMessageTemplate;
        try {
            errorMessageTemplate = errorMessagesBundle.getString("error." + errorMessageKey.name());
        } catch (MissingResourceException e) {
            throw new IllegalArgumentException("Unable to find message for key " + errorMessageKey.name());
        }
        StringSubstitutor substitutor = new StringSubstitutor(
            errorMessageParams
                .entrySet()
                .stream()
                .collect(Collectors.toMap(e -> e.getKey().getKey(), Map.Entry::getValue)),
            "{{",
            "}}"
        );
        return substitutor.replace(errorMessageTemplate);
    }

    public static CollectInternalSingleErrorsDetailException generateException(CollectErrorMessagesEnum messageKey) {
        return generateException(messageKey, Map.of(), null);
    }

    public static CollectInternalSingleErrorsDetailException generateException(
        CollectErrorMessagesEnum messageKey,
        Map<CollectErrorParamEnum, String> messageParameters
    ) {
        return generateException(messageKey, messageParameters, null);
    }

    public static CollectInternalSingleErrorsDetailException generateException(
        CollectErrorMessagesEnum messageKey,
        Map<CollectErrorParamEnum, String> messageParameters,
        Throwable cause
    ) {
        String errorMessage = generateErrorMessage(messageKey, messageParameters);
        return new CollectInternalSingleErrorsDetailException(
            errorMessage,
            generateVitamErrorsDetails(messageKey, messageParameters),
            cause
        );
    }
}
