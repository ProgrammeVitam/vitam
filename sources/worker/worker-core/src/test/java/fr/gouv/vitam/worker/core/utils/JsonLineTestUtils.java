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
package fr.gouv.vitam.worker.core.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.worker.core.distribution.JsonLineGenericIterator;
import net.javacrumbs.jsonunit.JsonAssert;
import org.apache.commons.collections4.IteratorUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonLineTestUtils {

    private static final TypeReference<JsonNode> JSON_NODE_TYPE_REFERENCE = new TypeReference<JsonNode>() {};

    public static void assertJsonlReportsEqual(InputStream actualInputStream, InputStream expectedReportInputStream)
        throws InvalidParseOperationException {
        try (
            JsonLineGenericIterator<JsonNode> resultReportIterator = new JsonLineGenericIterator<>(
                actualInputStream,
                JSON_NODE_TYPE_REFERENCE
            );
            JsonLineGenericIterator<JsonNode> expectedReportIterator = new JsonLineGenericIterator<>(
                expectedReportInputStream,
                JSON_NODE_TYPE_REFERENCE
            );
        ) {
            JsonAssert.assertJsonEquals(
                JsonHandler.toJsonNode(IteratorUtils.toList(resultReportIterator)),
                JsonHandler.toJsonNode(IteratorUtils.toList(expectedReportIterator))
            );
        }
    }

    public static void assertJsonlReportsEqualUnordered(File actual, File expected, int headerLines)
        throws IOException {
        assertJsonlReportsEqualUnordered(new FileInputStream(actual), new FileInputStream(expected), headerLines);
    }

    public static void assertJsonlReportsEqualUnordered(
        InputStream actualInputStream,
        InputStream expectedReportInputStream,
        int headerLines
    ) {
        try (
            JsonLineGenericIterator<JsonNode> actualReportIterator = new JsonLineGenericIterator<>(
                actualInputStream,
                JSON_NODE_TYPE_REFERENCE
            );
            JsonLineGenericIterator<JsonNode> expectedReportIterator = new JsonLineGenericIterator<>(
                expectedReportInputStream,
                JSON_NODE_TYPE_REFERENCE
            );
        ) {
            // Compare headers
            for (int i = 0; i < headerLines; i++) {
                JsonAssert.assertJsonEquals(actualReportIterator.next(), expectedReportIterator.next());
            }

            // Compare entries grouped by id
            List<JsonNode> actualLines = IteratorUtils.toList(actualReportIterator);
            List<JsonNode> expectedLines = IteratorUtils.toList(expectedReportIterator);
            actualLines.sort(Comparator.comparing(json -> json.get("id").asText()));
            expectedLines.sort(Comparator.comparing(json -> json.get("id").asText()));

            assertThat(actualLines).hasSameSizeAs(expectedLines);
            for (int i = 0; i < actualLines.size(); i++) {
                JsonAssert.assertJsonEquals(actualLines.get(i), expectedLines.get(i));
            }
        }
    }
}
