package fr.gouv.vitam.storage.offers.tape.parser;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.storage.offers.tape.dto.TapeLibraryState;
import net.javacrumbs.jsonunit.JsonAssert;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class TapeLibraryStatusParserTest {

    @Test
    public void testParseTapeLibraryStatus() throws Exception {
        String statusOutPut = PropertiesUtils.getResourceAsString("output/tape-robot-stats.txt");

        final TapeLibraryStatusParser tapeLibraryStatusParser = new TapeLibraryStatusParser();
        final TapeLibraryState tapeLibraryState = tapeLibraryStatusParser.parse(statusOutPut);

        Assertions.assertThat(tapeLibraryState).isNotNull();

        String expected = "result/tape-robot-stats.json";
        JsonNode initialJson = JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(expected));
        JsonAssert.assertJsonEquals(JsonHandler.toJsonNode(tapeLibraryState), initialJson);

    }
}