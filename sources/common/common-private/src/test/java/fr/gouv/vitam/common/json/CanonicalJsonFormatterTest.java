package fr.gouv.vitam.common.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.PropertiesUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

public class CanonicalJsonFormatterTest {

    @Test
    public void serialize() throws Exception {
        String inputJson = "json_canocalization/test_input.json";
        String expectedOutput = "json_canocalization/expected_output.json";
        try (InputStream is = PropertiesUtils.getResourceAsStream(inputJson);
            InputStream expectedInputStream = PropertiesUtils.getResourceAsStream(expectedOutput)) {
            JsonNode jsonNode = JsonHandler.getFromInputStream(is);

            InputStream resultInputStream = CanonicalJsonFormatter.serialize(jsonNode);
            assertThat(IOUtils.contentEquals(resultInputStream, expectedInputStream)).isTrue();
        }
    }


    @Test
    public void testSerializeBinary() {
        ObjectNode jsonNode = JsonHandler.createObjectNode();
        jsonNode.put("binary", "123456789az".getBytes());

        byte[] result = CanonicalJsonFormatter.serializeToByteArray(jsonNode);

        assertThat(new String(result)).isEqualTo("{\"binary\":\"MTIzNDU2Nzg5YXo=\"}");
    }
}
