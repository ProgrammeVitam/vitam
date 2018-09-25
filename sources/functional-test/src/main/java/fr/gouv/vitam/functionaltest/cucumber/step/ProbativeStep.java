/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.functionaltest.cucumber.step;


import com.fasterxml.jackson.databind.JsonNode;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Then;
import fr.gouv.vitam.common.json.JsonHandler;

import java.io.IOException;
import java.util.List;

public class ProbativeStep {

    public static final String CONTEXT_IDENTIFIER = "CT-000001";

    public ProbativeStep(World world) {
        this.world = world;
    }

    private World world;
    private List<String> usages;
    private JsonNode query;

    /**
     * @return generic Model
     */
    public JsonNode getModel() {
        return model;
    }


    public void setModel(JsonNode model) {
        this.model = model;
    }

    /**
     * generic model result
     */
    private JsonNode model;


    /**
     * Tentative d'import d'un contrat si jamais il n'existe pas
     *
     * @param type
     * @throws IOException
     */
    @Then("^Je lance un rélevé de valeur probante avec la requete suivante (.*)")
    public void probativeValue(String query) throws IOException {
//        JsonHandler.getFromString()
   }
    @And("^Les usages sont (.*)")
    public void usages_ares(String type) throws IOException {
    }

}
