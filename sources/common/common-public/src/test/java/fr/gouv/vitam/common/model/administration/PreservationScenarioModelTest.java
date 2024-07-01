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
package fr.gouv.vitam.common.model.administration;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.model.administration.preservation.PreservationScenarioModel;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static fr.gouv.vitam.common.json.JsonHandler.getFromFile;
import static org.assertj.core.api.Assertions.assertThat;

public class PreservationScenarioModelTest {

    private PreservationScenarioModel model;

    @Before
    public void setUp() throws Exception {
        //given
        model = getFromFile(
            PropertiesUtils.getResourceFile("preservation/completeScenario.json"),
            PreservationScenarioModel.class
        );
    }

    @Test
    public void shouldFailGriffinIdFromModel() throws Exception {
        model.setDefaultGriffin(null);

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
        assertThat(model.getGriffinByFormat("x-fmt/178").get().getActionDetail().get(0).getType()).isEqualTo(
            ActionTypePreservation.ANALYSE
        );

        assertThat(
            model.getGriffinByFormat("x-fmt/178").get().getActionDetail().get(1).getValues().getExtension()
        ).isEqualTo("pdf");
        assertThat(
            model.getGriffinByFormat("x-fmt/178").get().getActionDetail().get(1).getValues().getArgs()
        ).isEqualTo(Lists.newArrayList("-quality", "90"));

        assertThat(model.getGriffinByFormat("sre").get().getGriffinIdentifier()).isEqualTo("GRI-0000005");
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
