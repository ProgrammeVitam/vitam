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
package fr.gouv.vitam.common.client;

import org.apache.commons.lang.math.IntRange;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class CustomVitamHttpStatusCodeTest {

    @Test
    public void checkStatusCodeRanges() {
        List<IntRange> availableRanges = List.of(
            new IntRange(110, 199),
            new IntRange(230, 299),
            new IntRange(320, 399),
            new IntRange(460, 489),
            new IntRange(530, 599)
        );

        for (CustomVitamHttpStatusCode statusCode : CustomVitamHttpStatusCode.values()) {
            boolean isValidStatusCode = availableRanges
                .stream()
                .anyMatch(range -> range.containsInteger(statusCode.getStatusCode()));
            assertThat(isValidStatusCode).isTrue();
        }
    }

    @Test
    public void checkStatusCodeDuplicates() {
        Set<Integer> existingStatusCodes = new HashSet<>();
        boolean uniqueStatusCodes = Arrays.stream(CustomVitamHttpStatusCode.values())
            .map(CustomVitamHttpStatusCode::getStatusCode)
            .allMatch(existingStatusCodes::add);

        assertThat(uniqueStatusCodes).isTrue();
    }
}
