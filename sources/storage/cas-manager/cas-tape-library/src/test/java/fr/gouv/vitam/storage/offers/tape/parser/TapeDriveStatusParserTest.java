package fr.gouv.vitam.storage.offers.tape.parser;

import static org.junit.Assert.*;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.storage.offers.tape.dto.TapeDriveState;
import fr.gouv.vitam.storage.offers.tape.dto.TapeDriveStatus;
import fr.gouv.vitam.storage.offers.tape.dto.TapeLibraryState;
import net.javacrumbs.jsonunit.JsonAssert;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class TapeDriveStatusParserTest {


    @Test
    public void testParseTapeDriveStatusOnline() throws Exception {
        String statusOutPut = PropertiesUtils.getResourceAsString("output/tape-drive-status-online.txt");

        final TapeDriveStatusParser tapeDriveStatusParser = new TapeDriveStatusParser();
        final TapeDriveState tapeDriveState = tapeDriveStatusParser.parse(statusOutPut);

        Assertions.assertThat(tapeDriveState).isNotNull();

        String expected = "result/tape-drive-status-online.json";
        JsonNode initialJson = JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(expected));
        JsonAssert.assertJsonEquals(JsonHandler.toJsonNode(tapeDriveState), initialJson);

    }


    @Test
    public void testParseTapeDriveStatusOpen() throws Exception {
        String statusOutPut = PropertiesUtils.getResourceAsString("output/tape-drive-status-open.txt");

        final TapeDriveStatusParser tapeDriveStatusParser = new TapeDriveStatusParser();
        final TapeDriveState tapeDriveState = tapeDriveStatusParser.parse(statusOutPut);

        Assertions.assertThat(tapeDriveState).isNotNull();

        String expected = "result/tape-drive-status-open.json";
        JsonNode initialJson = JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(expected));
        JsonAssert.assertJsonEquals(JsonHandler.toJsonNode(tapeDriveState), initialJson);

    }
}