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
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.PropertiesUtils;
import net.javacrumbs.jsonunit.JsonAssert;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

public class CanonicalJsonFormatterTest {

    @Test
    public void whenSerializeCheckBinaryData() throws Exception {
        String inputJson = "json_canonicalization/test_input.json";
        String expectedOutput = "json_canonicalization/expected_output.json";
        try (
            InputStream is = PropertiesUtils.getResourceAsStream(inputJson);
            InputStream expectedInputStream = PropertiesUtils.getResourceAsStream(expectedOutput)
        ) {
            JsonNode jsonNode = JsonHandler.getFromInputStream(is);

            InputStream resultInputStream = CanonicalJsonFormatter.serialize(jsonNode);
            assertThat(IOUtils.contentEquals(resultInputStream, expectedInputStream)).isTrue();
        }
    }

    @Test
    public void whenSerializeCheckDataParsing() throws Exception {
        String inputJson = "json_canonicalization/test_input.json";
        JsonNode initialJson = JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(inputJson));
        JsonNode canonicalJson = JsonHandler.getFromInputStream(CanonicalJsonFormatter.serialize(initialJson));

        JsonAssert.assertJsonEquals(initialJson.toString(), canonicalJson.toString());
    }

    @Test
    public void testSerializeBinary() {
        ObjectNode jsonNode = JsonHandler.createObjectNode();
        jsonNode.put("binary", "123456789az".getBytes());

        byte[] result = CanonicalJsonFormatter.serializeToByteArray(jsonNode);

        assertThat(new String(result)).isEqualTo("{\"binary\":\"MTIzNDU2Nzg5YXo=\"}");
    }
}
