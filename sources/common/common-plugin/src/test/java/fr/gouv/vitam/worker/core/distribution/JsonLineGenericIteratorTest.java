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

package fr.gouv.vitam.worker.core.distribution;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import org.apache.commons.io.input.NullInputStream;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static fr.gouv.vitam.common.json.JsonHandler.JSON_NODE_TYPE_REFERENCE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class JsonLineGenericIteratorTest {

    @Test
    public void testEmptyFileThenHasNotNextEntry() {
        JsonLineGenericIterator<JsonNode> jsonLineGenericIterator = new JsonLineGenericIterator<>(
            new NullInputStream(0),
            JSON_NODE_TYPE_REFERENCE
        );
        assertThat(jsonLineGenericIterator.hasNext()).isFalse();
        assertThatThrownBy(jsonLineGenericIterator::next).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void testSingleEntryFileThenParseEntry() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (JsonLineWriter writer = new JsonLineWriter(byteArrayOutputStream)) {
            writer.addEntry(JsonHandler.createObjectNode().put("_id", "id1"));
        }

        JsonLineGenericIterator<JsonNode> jsonLineGenericIterator = new JsonLineGenericIterator<>(
            byteArrayOutputStream.toInputStream(),
            JSON_NODE_TYPE_REFERENCE
        );

        assertThat(jsonLineGenericIterator.hasNext()).isTrue();
        assertThat(jsonLineGenericIterator.next().get("_id").asText()).isEqualTo("id1");

        assertThat(jsonLineGenericIterator.hasNext()).isFalse();
    }

    @Test
    public void testMultipleEntryFileThenParseEntry() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (JsonLineWriter writer = new JsonLineWriter(byteArrayOutputStream)) {
            writer.addEntry(JsonHandler.createObjectNode().put("_id", "id1"));
            writer.addEntry(JsonHandler.createObjectNode().put("_id", "id2"));
            writer.addEntry(JsonHandler.createObjectNode().put("_id", "id3"));
        }

        JsonLineGenericIterator<JsonNode> jsonLineGenericIterator = new JsonLineGenericIterator<>(
            byteArrayOutputStream.toInputStream(),
            JSON_NODE_TYPE_REFERENCE
        );

        assertThat(jsonLineGenericIterator.hasNext()).isTrue();
        assertThat(jsonLineGenericIterator.next().get("_id").asText()).isEqualTo("id1");

        assertThat(jsonLineGenericIterator.hasNext()).isTrue();
        assertThat(jsonLineGenericIterator.next().get("_id").asText()).isEqualTo("id2");

        assertThat(jsonLineGenericIterator.hasNext()).isTrue();
        assertThat(jsonLineGenericIterator.next().get("_id").asText()).isEqualTo("id3");

        assertThat(jsonLineGenericIterator.hasNext()).isFalse();
    }

    @Test
    public void testFullLineConsumingBeforeReadingNextLine() {
        String data = "{\"_id\":\"id1\"}                 \n" + "{\"_id\":\"id2\"}                 ";

        // Jackson may not consume the whole line stream if it ends with spacing or \n
        // Reading byte per byte forces ensuring all data is read / avoid jackson read buffer size
        InputStream inputStream = JunitHelper.getPerByteInputStream(
            new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8))
        );

        JsonLineGenericIterator<JsonNode> jsonLineGenericIterator = new JsonLineGenericIterator<>(
            inputStream,
            JSON_NODE_TYPE_REFERENCE
        );

        assertThat(jsonLineGenericIterator.hasNext()).isTrue();
        assertThat(jsonLineGenericIterator.next().get("_id").asText()).isEqualTo("id1");

        assertThat(jsonLineGenericIterator.hasNext()).isTrue();
        assertThat(jsonLineGenericIterator.next().get("_id").asText()).isEqualTo("id2");

        assertThat(jsonLineGenericIterator.hasNext()).isFalse();
    }
}
