package fr.gouv.vitam.common.model.administration;

import fr.gouv.vitam.common.PropertiesUtils;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static fr.gouv.vitam.common.json.JsonHandler.getFromFile;
import static fr.gouv.vitam.common.json.JsonHandler.getFromString;
import static org.assertj.core.api.Assertions.assertThat;

public class PreservationScenarioModelTest {

    private PreservationScenarioModel model;

    @Before
    public void setUp() throws Exception {
        //given
        model = getFromFile(PropertiesUtils.getResourceFile("preservation/completeScenario.json"),
            PreservationScenarioModel.class);
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

        assertThat(
            model.getGriffinByFormat("sre").get().getGriffinIdentifier())
            .isEqualTo("GRI-0000005");

    }

    @Test
    public void givenPreservationAsStringShouldCreateModel() {

        assertThat(model.getName()).isEqualTo("Normalisation d'entrée");
    }

    @Test
    public void shouldGetAllIdentifiers() {

        assertThat(model.getAllGriffinIdentifiers()).containsOnly("GRI-0000023", "GRI-0000012", "GRI-0000005");
    }
}
