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

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class FieldNameValidationUtilsTest {

    @Test
    public void testBlankFieldNames() {
        List<String> fieldNames = Lists.newArrayList(null, "   \t   \n", "");

        for (String fieldName : fieldNames) {
            assertThatThrownBy(() -> FieldNameValidationUtils.validateRegularVitamFieldName(fieldName))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Empty or blank field name");
        }
    }

    @Test
    public void testReservedChars() {
        List<String> fieldNames = List.of(
            "A\u0000Z",
            "A\tZ",
            "A\u001FZ",
            "A\u007FZ",
            "A\u009FZ",
            "A A",
            "A\tA",
            "A\nA",
            "A\rA",
            "A'A",
            "A\"A",
            "A`A",
            "A,A",
            "A:A",
            "A°A",
            "A$A",
            "A§A",
            "A&A",
            "A#A",
            "A*A",
            "A+A",
            "A=A",
            "A/A",
            "A|A",
            "A\\A",
            "A(A",
            "A)A",
            "A{A",
            "A}A",
            "A[A",
            "A]A",
            "A@A",
            "A~A",
            "A^A",
            "A!A",
            "A?A",
            "A<A",
            "A>A",
            "A%A"
        );

        for (String fieldName : fieldNames) {
            assertThatThrownBy(() -> FieldNameValidationUtils.validateRegularVitamFieldName(fieldName))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Reserved / illegal characters");
        }
    }

    @Test
    public void testFieldNameTooLong() {
        String fieldName = StringUtils.repeat('A', 120);
        assertThatThrownBy(() -> FieldNameValidationUtils.validateRegularVitamFieldName(fieldName))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Field name too long");
    }

    @Test
    public void testValidFieldNameContainingDigits() {
        String fieldName = "Field1Name2";
        assertThatCode(
            () -> FieldNameValidationUtils.validateRegularVitamFieldName(fieldName)
        ).doesNotThrowAnyException();
    }

    @Test
    public void testInvalidFieldNameStartingWithDigit() {
        String fieldName = "1FieldName";
        assertThatThrownBy(() -> FieldNameValidationUtils.validateRegularVitamFieldName(fieldName))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Field name cannot start with a digit");
    }

    @Test
    public void testHeaderValidation_IllegalFieldNamesPrefix() {
        List<String> fieldNames = List.of("_AZ", "-AZ");
        for (String fieldName : fieldNames) {
            assertThatThrownBy(() -> FieldNameValidationUtils.validateRegularVitamFieldName(fieldName))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Field name cannot start with '_' or '-'");
        }
    }

    @Test
    public void testValidFieldNames() {
        List<String> fieldNames = List.of("Simple", "field_name-1", "F2", "fïe1dnàmê");
        for (String fieldName : fieldNames) {
            assertThatCode(
                () -> FieldNameValidationUtils.validateRegularVitamFieldName(fieldName)
            ).doesNotThrowAnyException();
        }
    }
}
