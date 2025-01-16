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

import org.apache.commons.lang3.StringUtils;

import java.util.Set;
import java.util.stream.Collectors;

import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.STARTS_WITH_DIGIT_PATTERN;
import static fr.gouv.vitam.collect.internal.core.csv.CsvMetadataUtils.matchesPattern;

public class FieldNameValidationUtils {

    public static final int MAX_FIELD_NAME_LENGTH = 100;

    private static final Set<Character> RESERVED_CHARACTERS =
        "'\"`,;:°$§&# .*+=/|\\(){}[]@~^!?<>%".chars().mapToObj(c -> (char) c).collect(Collectors.toSet());

    private FieldNameValidationUtils() {}

    /**
     * Validate regular Vitam field name (too long, empty, '.' separator, spaces, unprintable chars, does not start with '_' / '-' / digit...)
     * @throws IllegalArgumentException if validation fails
     */
    public static void validateRegularVitamFieldName(String fieldName) throws IllegalArgumentException {
        if (StringUtils.isAllBlank(fieldName)) {
            throw new IllegalArgumentException("Empty or blank field name");
        }

        for (char c : fieldName.toCharArray()) {
            if (Character.isISOControl(c) || !Character.isDefined(c) || RESERVED_CHARACTERS.contains(c)) {
                throw new IllegalArgumentException("Reserved / illegal characters");
            }
        }

        if (fieldName.length() > MAX_FIELD_NAME_LENGTH) {
            throw new IllegalArgumentException("Field name too long");
        }

        if (fieldName.startsWith("_") || fieldName.startsWith("-")) {
            throw new IllegalArgumentException("Field name cannot start with '_' or '-'");
        }

        if (matchesPattern(fieldName, STARTS_WITH_DIGIT_PATTERN)) {
            throw new IllegalArgumentException("Field name cannot start with a digit");
        }
    }
}
