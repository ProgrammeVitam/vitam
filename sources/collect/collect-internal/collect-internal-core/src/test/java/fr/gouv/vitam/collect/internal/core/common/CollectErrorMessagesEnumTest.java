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
package fr.gouv.vitam.collect.internal.core.common;

import org.junit.Test;

import java.util.Collections;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

public class CollectErrorMessagesEnumTest {

    private static final ResourceBundle errorMessagesBundle = ResourceBundle.getBundle("collect-error-messages");

    @Test
    public void checkThatAllKeysHaveTranslationTest() {
        Pattern parameterPattern = Pattern.compile("\\{\\{([a-zA-Z]+)}}");
        for (CollectErrorMessagesEnum collectErrorMessagesEnum : CollectErrorMessagesEnum.values()) {
            // Check that translation exists
            String messageKey = "error." + collectErrorMessagesEnum.name();
            assertThatCode(() -> errorMessagesBundle.getString(messageKey)).doesNotThrowAnyException();
            // Check that all parameters in message are valid
            String errorMessage = errorMessagesBundle.getString(messageKey);
            Matcher matcher = parameterPattern.matcher(errorMessage);
            while (matcher.find()) {
                String parameter = matcher.group(1);
                assertThat(CollectErrorParamEnum.getByKey(parameter))
                    .withFailMessage("Parameter '" + parameter + "' not found in CollectErrorParamEnum")
                    .isNotNull();
            }
        }
    }

    @Test
    public void checkThatAllMessagesHaveCorrespondingKey() {
        for (String key : Collections.list(errorMessagesBundle.getKeys())) {
            // Check that key has the correct format
            assertThat(key).startsWith("error.");
            // Check that the key is in the CollectErrorMessagesEnum enum
            String enumKey = key.substring(6);
            assertThatCode(() -> CollectErrorMessagesEnum.valueOf(enumKey))
                .withFailMessage("Enum Key '" + enumKey + "' not found in CollectErrorMessagesEnum")
                .doesNotThrowAnyException();
        }
    }
}
