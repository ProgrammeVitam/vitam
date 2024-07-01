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
package fr.gouv.vitam.functionaltest.cucumber.step;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientException;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.OntologyModel;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class OntologyStep extends CommonStep {

    public OntologyStep(World world) {
        super(world);
    }

    private Path fileName;

    private OntologyModel ontologyModel;

    /**
     * define a ontology file
     *
     * @param fileName name of a sip
     */
    @Given("^un fichier ontologie nommé (.*)$")
    public void setFileName(String fileName) {
        this.fileName = Paths.get(world.getBaseDirectory(), fileName);
    }

    @When("^j'importe l'ontologie$")
    public void importOntology() throws InvalidParseOperationException, AccessExternalClientException, IOException {
        importOntology(false);
    }

    @When("^j'importe l'ontologie en mode forcé$")
    public void forceImportOntology()
        throws InvalidParseOperationException, AccessExternalClientException, IOException {
        importOntology(true);
    }

    private void importOntology(boolean forceUpdate)
        throws IOException, InvalidParseOperationException, AccessExternalClientException {
        try (InputStream inputStream = Files.newInputStream(fileName, StandardOpenOption.READ)) {
            VitamContext vitamContext = new VitamContext(world.getTenantId());
            vitamContext.setApplicationSessionId(world.getApplicationSessionId());

            RequestResponse requestResponse = world
                .getAdminClient()
                .importOntologies(forceUpdate, vitamContext, inputStream);
            final String operationId = requestResponse.getHeaderString(GlobalDataRest.X_REQUEST_ID);
            world.setOperationId(operationId);

            String httpCode = String.valueOf(requestResponse.getHttpCode());
            ObjectNode responseCode = JsonHandler.createObjectNode();
            responseCode.put("Code", httpCode);
            List<JsonNode> result = new ArrayList<>();
            result.add(responseCode);
            world.setResults(result);
        }
    }

    @When("^je recherche le vocabulaire intitulé (.*)$")
    public void searchOntologyByIdentifier(String identifer) throws VitamClientException {
        VitamContext vitamContext = new VitamContext(world.getTenantId());
        vitamContext.setApplicationSessionId(world.getApplicationSessionId());
        RequestResponse requestResponse = world.getAdminClient().findOntologyById(vitamContext, identifer);
        assertThat(requestResponse.isOk()).isTrue();

        ontologyModel = (OntologyModel) ((RequestResponseOK) requestResponse).getFirstResult();
    }

    @Then("^le type du vocabulaire est (.*)$")
    public void ontology_type_is(String type) {
        assertThat(ontologyModel.getType().name()).isEqualTo(type);
    }
}
