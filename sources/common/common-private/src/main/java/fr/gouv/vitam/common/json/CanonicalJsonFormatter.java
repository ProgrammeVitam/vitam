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
package fr.gouv.vitam.common.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.BaseXx;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * Json serializer with canonicalization. Ensures deterministic encoding for consistent hashing purposes.
 *
 * Serializes a json node object in a canonical deterministic encoding.
 * This encoding must never be changed to avoid inconsistent hashing of previously encoded data.
 *
 * String literals are encoded as follows in the format '"char1char2..."'
 * - Simple escaping is applied for and only for the following characters : '"', '\\', '\b', '\f', \n', '\r' &amp; '\t'.
 * - Printable ascii chars are not escaped [32 - 127]
 * - 4 digit hex escaping for any other character (\u1234).
 *
 * Integer literals are encoded as is (0, 1234, -1234)
 *
 * Floating numbers are formatted using Double.format()
 *
 * Arrays are encoded is the '[entry1,entry2...]' format.
 *
 * Objects entries are encoded with sorted keys in the '{"key1":value1,...)}'
 *
 * No pretty-printing is appended (no line breaks or spacing is appended).
 */
public final class CanonicalJsonFormatter {

    private static final char[] hexChars = "0123456789abcdef".toCharArray();

    /**
     * Serializes a json node object in a canonical deterministic encoding.
     *
     * @param node the json node
     * @returns Input stream
     */
    public static InputStream serialize(JsonNode node) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(8192);
            serialize(node, outputStream);
            return outputStream.toInputStream();
        } catch (IOException e) {
            throw new IllegalStateException("Unexpected IO exception on memory stream", e);
        }
    }

    /**
     * Serializes a json node object in a canonical deterministic encoding.
     *
     * @param node the json node
     * @returns Input stream
     */
    public static byte[] serializeToByteArray(JsonNode node) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(8192);
            serialize(node, outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Unexpected IO exception on memory stream", e);
        }
    }

    /**
     * Serializes a json node object in a canonical deterministic encoding.
     *
     * @param node the json node
     * @param outputStream output stream to which json node is serialized.
     * @throws IOException
     */
    public static void serialize(JsonNode node, OutputStream outputStream) throws IOException {
        try (AsciiWriter writer = new AsciiWriter(outputStream)) {
            new CanonicalJsonFormatter(writer).format(node);
            writer.flush();
        }
    }

    private final AsciiWriter writer;

    private CanonicalJsonFormatter(AsciiWriter writer) {
        this.writer = writer;
    }

    private void format(JsonNode node) throws IOException {
        if (node.isObject()) {
            formatObject((ObjectNode) node);
        } else if (node.isArray()) {
            formatArray((ArrayNode) node);
        } else if (node.isTextual()) {
            formatString(node.textValue());
        } else if (node.isNull()) {
            formatNull();
        } else if (node.isBoolean()) {
            formatBoolean(node.booleanValue());
        } else if (node.isInt()) {
            formatInt(node.intValue());
        } else if (node.isLong()) {
            formatLong(node.longValue());
        } else if (node.isFloat()) {
            formatFloat(node.floatValue());
        } else if (node.isDouble()) {
            formatDouble(node.doubleValue());
        } else if (node.isBinary()) {
            formatBinary(node.binaryValue());
        } else {
            throw new UnsupportedOperationException(
                "Unsupported json node type " + node.getNodeType() + " '" + node.toString() + "'"
            );
        }
    }

    private void formatDouble(double val) throws IOException {
        writer.append(Double.toString(val));
    }

    private void formatFloat(float val) throws IOException {
        writer.append(Float.toString(val));
    }

    private void formatLong(long val) throws IOException {
        writer.append(Long.toString(val));
    }

    private void formatInt(int val) throws IOException {
        writer.append(Integer.toString(val));
    }

    private void formatBoolean(boolean flag) throws IOException {
        writer.append(flag ? "true" : "false");
    }

    private void formatNull() throws IOException {
        writer.append("null");
    }

    private void formatObject(ObjectNode node) throws IOException {
        List<String> sortedKeys = IteratorUtils.toList(node.fieldNames());
        sortedKeys.sort(Comparator.naturalOrder());

        writer.append('{');
        for (int i = 0; i < sortedKeys.size(); i++) {
            if (i > 0) {
                writer.append(',');
            }

            String key = sortedKeys.get(i);
            formatString(key);
            writer.append(':');
            format(node.get(key));
        }
        writer.append('}');
    }

    private void formatArray(ArrayNode node) throws IOException {
        writer.append('[');

        Iterator<JsonNode> iterator = node.iterator();

        if (iterator.hasNext()) {
            format(iterator.next());

            while (iterator.hasNext()) {
                writer.append(',');
                format(iterator.next());
            }
        }

        writer.append(']');
    }

    private void formatString(String string) throws IOException {
        writer.append('"');

        int lastPos = 0;
        int curPos = 0;

        for (int curPor = 0; curPor < string.length(); curPor++) {
            char c = string.charAt(curPos);

            if (c < 32 || c > 127 || c == '"' || c == '\\') {
                writer.append(string, lastPos, curPos);
                lastPos = curPos + 1;

                switch (c) {
                    case '"':
                        writer.append("\\\"");
                        break;
                    case '\\':
                        writer.append("\\\\");
                        break;
                    case '\b':
                        writer.append("\\b");
                        break;
                    case '\f':
                        writer.append("\\f");
                        break;
                    case '\n':
                        writer.append("\\n");
                        break;
                    case '\r':
                        writer.append("\\r");
                        break;
                    case '\t':
                        writer.append("\\t");
                        break;
                    default:
                        writer.append("\\u");
                        writer.append(hexChars[(((int) c) >> 12) & 0xf]);
                        writer.append(hexChars[(((int) c) >> 8) & 0xf]);
                        writer.append(hexChars[(((int) c) >> 4) & 0xf]);
                        writer.append(hexChars[((int) c) & 0xf]);
                        break;
                }
            }

            curPos++;
        }

        writer.append(string, lastPos, curPos);

        writer.append('"');
    }

    private void formatBinary(byte[] data) throws IOException {
        writer.append('"');
        writer.append(BaseXx.getBase64(data));
        writer.append('"');
    }

    private static final class AsciiWriter implements AutoCloseable {

        private final OutputStream outputStream;

        AsciiWriter(OutputStream os) {
            outputStream = os;
        }

        public void append(char c) throws IOException {
            outputStream.write(c);
        }

        public void append(String s) throws IOException {
            int n = s.length();
            for (int i = 0; i < n; i++) {
                outputStream.write(s.charAt(i));
            }
        }

        public void append(String s, int start, int end) throws java.io.IOException {
            for (int i = start; i < end; i++) {
                outputStream.write(s.charAt(i));
            }
        }

        public void flush() throws java.io.IOException {
            outputStream.flush();
        }

        public void close() throws java.io.IOException {
            outputStream.close();
        }
    }
}
