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

import fr.gouv.vitam.collect.common.exception.CollectInternalMultipleErrorsDetailsException;
import fr.gouv.vitam.collect.internal.core.common.CollectErrorMessagesEnum;
import fr.gouv.vitam.collect.internal.core.common.CollectErrorParamEnum;
import fr.gouv.vitam.common.error.VitamErrorDetails;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AbstractErrorAccumulatorTest {

    @Test
    public void testNoErrorReported() throws CollectInternalMultipleErrorsDetailsException {
        try (MyErrorAccumulator errorAccumulator = new MyErrorAccumulator()) {
            // close does not throw exception
            assertThatCode(errorAccumulator::close).doesNotThrowAnyException();
        }
    }

    @Test
    public void testOneErrorReported() throws CollectInternalMultipleErrorsDetailsException {
        try (MyErrorAccumulator errorAccumulator = new MyErrorAccumulator()) {
            errorAccumulator.reportOneError(
                CollectErrorMessagesEnum.DUPLICATE_HEADER_NAME,
                Map.of(CollectErrorParamEnum.HEADER, "file.txt")
            );

            // Close triggers exception with 1 error
            assertThatThrownBy(errorAccumulator::close)
                .isInstanceOf(CollectInternalMultipleErrorsDetailsException.class)
                .hasMessage(
                    """
                    Test prefix. 1 error:
                    - Invalid header names. Duplicate header name 'file.txt'"""
                );
        }
    }

    @Test
    public void testMultipleErrorsReported() throws CollectInternalMultipleErrorsDetailsException {
        try (MyErrorAccumulator errorAccumulator = new MyErrorAccumulator()) {
            errorAccumulator.reportOneError(
                CollectErrorMessagesEnum.INVALID_HEADER_NAME_TEMPLATE,
                Map.of(
                    CollectErrorParamEnum.HEADER,
                    "tooLongHeader",
                    CollectErrorParamEnum.MESSAGE,
                    CollectErrorDetailHelper.generateErrorMessage(
                        CollectErrorMessagesEnum.HEADER_NAME_TOO_LONG,
                        Map.of()
                    )
                )
            );
            errorAccumulator.reportOneError(
                CollectErrorMessagesEnum.INVALID_HEADER_NAME_TEMPLATE,
                Map.of(
                    CollectErrorParamEnum.HEADER,
                    "tooLongHeader2",
                    CollectErrorParamEnum.MESSAGE,
                    CollectErrorDetailHelper.generateErrorMessage(
                        CollectErrorMessagesEnum.HEADER_NAME_TOO_LONG,
                        Map.of()
                    )
                )
            );

            // Close triggers exception with 2 errors
            assertThatThrownBy(errorAccumulator::close)
                .isInstanceOf(CollectInternalMultipleErrorsDetailsException.class)
                .hasMessage(
                    """
                    Test prefix. 2 errors:
                    - Invalid header name 'tooLongHeader': Header name is too long
                    - Invalid header name 'tooLongHeader2': Header name is too long"""
                );
        }
    }

    @Test
    public void testTooManyErrorsReported() throws CollectInternalMultipleErrorsDetailsException {
        try (MyErrorAccumulator errorAccumulator = new MyErrorAccumulator()) {
            // First 19 are buffered
            assertThatCode(() -> {
                for (int i = 1; i <= 19; i++) {
                    errorAccumulator.reportOneError(
                        CollectErrorMessagesEnum.NO_UNIT_MATCHES_SELECTION_CRITERIA,
                        Map.of(CollectErrorParamEnum.LINE_NUMBER, Integer.toString(i))
                    );
                }
            }).doesNotThrowAnyException();

            // 20th call triggers exception
            assertThatThrownBy(
                () ->
                    errorAccumulator.reportOneError(
                        CollectErrorMessagesEnum.NO_UNIT_MATCHES_SELECTION_CRITERIA,
                        Map.of(CollectErrorParamEnum.LINE_NUMBER, "20")
                    )
            )
                .isInstanceOf(CollectInternalMultipleErrorsDetailsException.class)
                .hasMessageStartingWith(
                    """
                    Test prefix. At least 20 errors:
                    - CSV record at line 1 : No unit matches selection criteria
                    - CSV record at line 2 : No unit matches selection criteria
                    - CSV record at line 3 : No unit matches selection criteria"""
                )
                .hasMessageEndingWith(
                    """
                    - CSV record at line 19 : No unit matches selection criteria
                    - CSV record at line 20 : No unit matches selection criteria"""
                );

            // close does not throw exception
            assertThatCode(errorAccumulator::close).doesNotThrowAnyException();
        }
    }

    private static class MyErrorAccumulator
        extends AbstractErrorAccumulator<CollectInternalMultipleErrorsDetailsException> {

        protected MyErrorAccumulator() {
            super(20);
        }

        @Override
        protected CollectInternalMultipleErrorsDetailsException buildException(
            String errorMessage,
            List<VitamErrorDetails> errorsDetails
        ) {
            return new CollectInternalMultipleErrorsDetailsException("Test prefix. " + errorMessage, errorsDetails);
        }
    }
}
