/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common.jsonl;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.json.JsonHandler;
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

public class JsonLineIteratorTest {

    @Test
    public void testEmptyFileThenHasNotNextEntry() {
        JsonLineIterator<JsonNode> jsonLineGenericIterator = new JsonLineIterator<>(
            new NullInputStream(0),
            JSON_NODE_TYPE_REFERENCE
        );
        assertThat(jsonLineGenericIterator.hasNext()).isFalse();
        assertThatThrownBy(jsonLineGenericIterator::next).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void testSingleEntryFileThenParseEntry() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (JsonLineWriter<JsonNode> writer = new JsonLineWriter<>(byteArrayOutputStream)) {
            writer.addEntry(JsonHandler.createObjectNode().put("_id", "id1"));
        }

        JsonLineIterator<JsonNode> jsonLineGenericIterator = new JsonLineIterator<>(
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
        try (JsonLineWriter<JsonNode> writer = new JsonLineWriter<>(byteArrayOutputStream)) {
            writer.addEntry(JsonHandler.createObjectNode().put("_id", "id1"));
            writer.addEntry(JsonHandler.createObjectNode().put("_id", "id2"));
            writer.addEntry(JsonHandler.createObjectNode().put("_id", "id3"));
        }

        JsonLineIterator<JsonNode> jsonLineGenericIterator = new JsonLineIterator<>(
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
        InputStream inputStream = getPerByteInputStream(
            new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8))
        );

        JsonLineIterator<JsonNode> jsonLineGenericIterator = new JsonLineIterator<>(
            inputStream,
            JSON_NODE_TYPE_REFERENCE
        );

        assertThat(jsonLineGenericIterator.hasNext()).isTrue();
        assertThat(jsonLineGenericIterator.next().get("_id").asText()).isEqualTo("id1");

        assertThat(jsonLineGenericIterator.hasNext()).isTrue();
        assertThat(jsonLineGenericIterator.next().get("_id").asText()).isEqualTo("id2");

        assertThat(jsonLineGenericIterator.hasNext()).isFalse();
    }

    private static InputStream getPerByteInputStream(InputStream inputStream) {
        return new InputStream() {
            @Override
            public int read() throws IOException {
                return inputStream.read();
            }

            @Override
            public int read(byte[] b) throws IOException {
                return read(b, 0, b.length);
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                if (len == 0) {
                    return 0;
                }
                int read = read();
                if (read == -1) {
                    return -1;
                }
                b[off] = (byte) read;
                return 1;
            }
        };
    }
}
