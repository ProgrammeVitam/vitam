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

package fr.gouv.vitam.collect.internal.core.csv;

import fr.gouv.vitam.collect.internal.core.common.CollectErrorMessagesEnum;
import org.apache.commons.lang3.StringUtils;

import java.util.Set;
import java.util.stream.Collectors;

import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.PREFIX_ID_HEADER;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.STARTS_WITH_DIGIT_PATTERN;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.matchesPattern;

public class FieldNameValidationUtils {

    public static final int MAX_FIELD_NAME_LENGTH = 100;

    private static final Set<Character> RESERVED_CHARACTERS =
        "'\"`,;:°$§&# .*+=/|\\(){}[]@~^!?<>%".chars().mapToObj(c -> (char) c).collect(Collectors.toSet());

    private FieldNameValidationUtils() {}

    /**
     * Validate regular Vitam field name (too long, empty, '.' separator, spaces, unprintable chars, does not start with '_' / '-' / digit...)
     * @return the error message or null if no error
     */
    public static CollectErrorMessagesEnum validateRegularVitamFieldName(String fieldName) {
        if (StringUtils.isAllBlank(fieldName)) {
            return CollectErrorMessagesEnum.EMPTY_BLANK_FIELD_NAME;
        }

        for (char c : fieldName.toCharArray()) {
            if (Character.isISOControl(c) || !Character.isDefined(c) || RESERVED_CHARACTERS.contains(c)) {
                return CollectErrorMessagesEnum.RESERVED_ILLEGAL_CHARACTERS;
            }
        }

        if (fieldName.length() > MAX_FIELD_NAME_LENGTH) {
            return CollectErrorMessagesEnum.FIELD_NAME_TOO_LONG;
        }

        if ((fieldName.startsWith("_") && !fieldName.equals(PREFIX_ID_HEADER)) || fieldName.startsWith("-")) {
            return CollectErrorMessagesEnum.FIELD_NAME_CANNOT_START_WITH_UNDERSCORE_OR_DASH;
        }

        if (matchesPattern(fieldName, STARTS_WITH_DIGIT_PATTERN)) {
            return CollectErrorMessagesEnum.FIELD_NAME_CANNOT_START_WITH_DIGIT;
        }

        return null;
    }
}
