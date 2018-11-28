package fr.gouv.vitam.common.model.administration;

import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import org.junit.Test;

import static fr.gouv.vitam.common.json.JsonHandler.getFromString;
import static fr.gouv.vitam.common.model.administration.ActionTypePreservation.ANALYSE;
import static fr.gouv.vitam.common.model.administration.ActionTypePreservation.GENERATE;
import static org.assertj.core.api.Assertions.assertThat;

public class GriffinByFormatTest {

    @Test
    public void shouldGenerateGriffinByFormat() throws InvalidParseOperationException {

        String s = "{\n" +
            "  \"FormatList\": [\n" +
            "    \"fmt/136\",\n" +
            "    \"fmt/137\",\n" +
            "    \"fmt/138\",\n" +
            "    \"fmt/139\",\n" +
            "    \"fmt/290\",\n" +
            "    \"fmt/294\",\n" +
            "    \"fmt/292\",\n" +
            "    \"fmt/296\",\n" +
            "    \"fmt/291\",\n" +
            "    \"fmt/295\",\n" +
            "    \"fmt/293\",\n" +
            "    \"fmt/297\"\n" +
            "  ],\n" +
            "  \"GriffinIdentifier\": \"GRI-0000023\",\n" +
            "  \"TimeOut\": 20,\n" +
            "  \"MaxSize\": 10000000,\n" +
            "  \"ActionDetail\": [\n" +
            "    {\n" +
            "      \"Action\": \"ANALYSE\",\n" +
            "      \"Values\": {\n" +
            "        \"args\": [\n" +
            "          \"-strict\"\n" +
            "        ]\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"Action\": \"GENERATE\",\n" +
            "      \"Values\": {\n" +
            "        \"extension\": \"pdf\",\n" +
            "        \"args\": [\n" +
            "          \"-f\",\n" +
            "          \"pdf\",\n" +
            "          \"-e\",\n" +
            "          \"SelectedPdfVersion=1\"\n" +
            "        ]\n" +
            "      }\n" +
            "    }\n" +
            "  ]\n" +
            "}";

        GriffinByFormat griffinByFormat = getFromString(s, GriffinByFormat.class);

        assertThat(griffinByFormat.getMaxSize()).isEqualTo(10000000);
        assertThat(griffinByFormat.getTimeOut()).isEqualTo(20);
        assertThat(griffinByFormat.getActionDetail().get(0).actionTypePreservation).isEqualTo(ANALYSE);
        assertThat(griffinByFormat.getActionDetail().get(1).actionTypePreservation).isEqualTo(GENERATE);

    }
}
