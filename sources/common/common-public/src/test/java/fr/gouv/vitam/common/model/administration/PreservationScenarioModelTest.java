package fr.gouv.vitam.common.model.administration;

import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static fr.gouv.vitam.common.json.JsonHandler.getFromString;
import static org.assertj.core.api.Assertions.assertThat;

public class PreservationScenarioModelTest {

    private PreservationScenarioModel model;

    @Before
    public void setUp() throws Exception {
        //given
        String stringModel = getModelString();
        model = getFromString(stringModel, PreservationScenarioModel.class);
    }

    @Test
    public void shouldFailGriffinIdFromModel() {

        Optional<String> griffinIdentifierByFormat3 = model.getGriffinIdentifierByFormat("toto");
        assertThat(griffinIdentifierByFormat3).isEmpty();
    }

    @Test
    public void shouldGetGriffinIdFromModel() {

        Optional<String> griffinIdentifierByFormat1 = model.getGriffinIdentifierByFormat("fmt/290");
        Optional<String> griffinIdentifierByFormat2 = model.getGriffinIdentifierByFormat("x-fmt/178");

        assertThat(griffinIdentifierByFormat1).isPresent();
        assertThat(griffinIdentifierByFormat2).isPresent();

        String identifier1 = griffinIdentifierByFormat1.get();
        String identifier2 = griffinIdentifierByFormat2.get();

        assertThat(identifier1).isEqualTo("GRI-0000023");
        assertThat(identifier2).isEqualTo("GRI-0000012");

        assertThat(model.getGriffinByFormat("x-fmt/178").get().isDebug()).isTrue();
        assertThat(model.getGriffinByFormat("x-fmt/178").get().getActionDetail().get(0).getType())
            .isEqualTo(ActionTypePreservation.ANALYSE);

        assertThat(
            model.getGriffinByFormat("x-fmt/178").get().getActionDetail().get(1).getValuesPreservation().getExtension())
            .isEqualTo("pdf");
        assertThat(
            model.getGriffinByFormat("x-fmt/178").get().getActionDetail().get(1).getValuesPreservation().getArgs())
            .isEqualTo(Lists.newArrayList("-quality", "90"));

    }

    @Test
    public void givenPreservationAsStringShouldCreateModel() {

        assertThat(model.getName()).isEqualTo("Normalisation d'entrée");
    }

    @Test
    public void shouldGetAllIdentifiers() {

        assertThat(model.getAllGriffinIdentifiers()).containsOnly("GRI-0000023", "GRI-0000012", "GRI-0000005");
    }

    private String getModelString() {
        return "{\"_id\": \"aefqaaaabahn6dttabew6alha45dfgqaaaaq\",\n" +
            "  \"Identifier\": \"PSC-000023\",\n" +
            "  \"Name\": \"Normalisation d'entrée\",\n" +
            "  \"Description\": \"Ce scénario permet de faire une validation des format et de créer une version de diffusion en PDF. Il est en général appliqué au contenu d'une entrée pour donner un retour de la qualité du versement et préparer une consultation fréquente.\",\n" +
            "  \"CreationDate\": \"2018-11-16T15:55:30.721\",\n" +
            "  \"LastUpdate\": \"2018-11-20T15:34:21.542\",\n" +
            "  \"ActionList\": [\n" +
            "    \"ANALYSE\",\n" +
            "    \"GENERATE\"\n" +
            "  ],\n" +
            "  \"MetadataFilter\": null,\n" +
            "  \"GriffinByFormat\": [\n" +
            "    {\n" +
            "      \"FormatList\": [\n" +
            "        \"fmt/136\",\n" +
            "        \"fmt/137\",\n" +
            "        \"fmt/138\",\n" +
            "        \"fmt/139\",\n" +
            "        \"fmt/290\",\n" +
            "        \"fmt/294\",\n" +
            "        \"fmt/292\",\n" +
            "        \"fmt/296\",\n" +
            "        \"fmt/291\",\n" +
            "        \"fmt/295\",\n" +
            "        \"fmt/293\",\n" +
            "        \"fmt/297\"\n" +
            "      ],\n" +
            "      \"GriffinIdentifier\": \"GRI-0000023\",\n" +
            "      \"TimeOut\": 20,\n" +
            "      \"MaxSize\": 10000000,\n" +
            "      \"Debug\": false,\n" +
            "      \"ActionDetail\": [\n" +
            "        {\n" +
            "          \"Type\": \"ANALYSE\",\n" +
            "          \"Values\": {\n" +
            "            \"Args\": [\n" +
            "              \"-strict\"\n" +
            "            ]\n" +
            "          }\n" +
            "        },\n" +
            "        {\n" +
            "          \"Type\": \"GENERATE\",\n" +
            "          \"Values\": {\n" +
            "            \"Extension\": \"pdf\",\n" +
            "            \"Args\": [\n" +
            "              \"-f\",\n" +
            "              \"pdf\",\n" +
            "              \"-e\",\n" +
            "              \"SelectedPdfVersion=1\"\n" +
            "            ]\n" +
            "          }\n" +
            "        }\n" +
            "      ]\n" +
            "    },\n" +
            "    {\n" +
            "      \"FormatList\": [\n" +
            "        \"fmt/41\",\n" +
            "        \"fmt/42\",\n" +
            "        \"x-fmt/398\",\n" +
            "        \"x-fmt/390\",\n" +
            "        \"x-fmt/391\",\n" +
            "        \"fmt/645\",\n" +
            "        \"fmt/43\",\n" +
            "        \"fmt/44\",\n" +
            "        \"fmt/112\",\n" +
            "        \"fmt/11\",\n" +
            "        \"fmt/12\",\n" +
            "        \"fmt/13\",\n" +
            "        \"fmt/935\",\n" +
            "        \"fmt/152\",\n" +
            "        \"fmt/399\",\n" +
            "        \"fmt/388\",\n" +
            "        \"fmt/387\",\n" +
            "        \"fmt/155\",\n" +
            "        \"fmt/353\",\n" +
            "        \"fmt/154\",\n" +
            "        \"fmt/153\",\n" +
            "        \"fmt/156\",\n" +
            "        \"x-fmt/392\",\n" +
            "        \"x-fmt/178\",\n" +
            "        \"fmt/408\",\n" +
            "        \"fmt/568\",\n" +
            "        \"fmt/567\",\n" +
            "        \"fmt/566\"\n" +
            "      ],\n" +
            "      \"GriffinIdentifier\": \"GRI-0000012\",\n" +
            "      \"TimeOut\": 10,\n" +
            "      \"MaxSize\": 10000000,\n" +
            "      \"Debug\": true,\n" +
            "      \"ActionDetail\": [\n" +
            "        {\n" +
            "          \"Type\": \"ANALYSE\",\n" +
            "          \"Values\": null\n" +
            "        },\n" +
            "        {\n" +
            "          \"Type\": \"GENERATE\",\n" +
            "          \"Values\": {\n" +
            "            \"Extension\": \"pdf\",\n" +
            "            \"Args\": [\n" +
            "              \"-quality\",\n" +
            "              \"90\"\n" +
            "            ]\n" +
            "          }\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  ],\n" +
            "  \"GriffinDefault\": {\n" +
            "    \"GriffinIdentifier\": \"GRI-0000005\",\n" +
            "    \"TimeOut\": 10,\n" +
            "    \"MaxSize\": 10000000,\n" +
            "    \"ActionDetail\": [\n" +
            "      {\n" +
            "        \"Type\": \"ANALYSE\",\n" +
            "        \"Values\": {\n" +
            "          \"Args\": [\n" +
            "            \"-strict\"\n" +
            "          ]\n" +
            "        }\n" +
            "      }\n" +
            "    ]\n" +
            "  }\n" +
            "}";
    }
}
