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

import fr.gouv.vitam.collect.internal.core.exceptions.CollectInvalidCsvFormatException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CsvErrorAccumulatorTest {

    @Test
    public void testNoErrorReported() throws CollectInvalidCsvFormatException {
        try (CsvErrorAccumulator csvErrorAccumulator = new CsvErrorAccumulator()) {
            // close does not throw exception
            assertThatCode(csvErrorAccumulator::close).doesNotThrowAnyException();
        }
    }

    @Test
    public void testOneErrorReported() throws CollectInvalidCsvFormatException {
        try (CsvErrorAccumulator csvErrorAccumulator = new CsvErrorAccumulator()) {
            csvErrorAccumulator.report("msg1");

            // Close triggers exception with 1 error
            assertThatThrownBy(csvErrorAccumulator::close)
                .isInstanceOf(CollectInvalidCsvFormatException.class)
                .hasMessage(
                    """
                    CSV validation failed. 1 error:
                    - msg1"""
                );
        }
    }

    @Test
    public void testMultipleErrorsReported() throws CollectInvalidCsvFormatException {
        try (CsvErrorAccumulator csvErrorAccumulator = new CsvErrorAccumulator()) {
            csvErrorAccumulator.report("msg1");
            csvErrorAccumulator.report("msg2");

            // Close triggers exception with 2 errors
            assertThatThrownBy(csvErrorAccumulator::close)
                .isInstanceOf(CollectInvalidCsvFormatException.class)
                .hasMessage(
                    """
                    CSV validation failed. 2 errors:
                    - msg1
                    - msg2"""
                );
        }
    }

    @Test
    public void testTooManyErrorsReported() throws CollectInvalidCsvFormatException {
        try (CsvErrorAccumulator csvErrorAccumulator = new CsvErrorAccumulator()) {
            // First 19 are buffered
            assertThatCode(() -> {
                for (int i = 1; i <= 19; i++) {
                    csvErrorAccumulator.report("msg" + i);
                }
            }).doesNotThrowAnyException();

            // 20th call triggers exception
            assertThatThrownBy(() -> csvErrorAccumulator.report("msg20"))
                .isInstanceOf(CollectInvalidCsvFormatException.class)
                .hasMessageStartingWith(
                    """
                    CSV validation failed. At least 20 errors:
                    - msg1
                    - msg2
                    - msg3"""
                )
                .hasMessageEndingWith(
                    """
                    - msg19
                    - msg20"""
                );

            // close does not throw exception
            assertThatCode(csvErrorAccumulator::close).doesNotThrowAnyException();
        }
    }
}
