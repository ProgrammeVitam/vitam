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
import cucumber.api.DataTable;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientException;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.AgenciesModel;

import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 *
 */
public class AgenciesStep extends CommonStep {

    public AgenciesStep(World world) {
        super(world);
    }

    private String fileName;

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
     * define a sip
     *
     * @param fileName name of a sip
     */
    @Given("^un fichier de service producteur nommé (.*)$")
    public void a_sip_named(String fileName) {
        this.fileName = fileName;
    }

    @Then("^j'importe les services producteurs$")
    public void uploadAgency() {
        uploadAgency(true);
    }

    @Then("^j'importe les services producteurs sans échec$")
    public void uploadAgency_without_failure() {
        uploadAgency(null);
    }

    @Then("^j'importe les services producteurs incorrects")
    public void uploadAgency_with_failure() {
        uploadAgency(false);
    }

    private void uploadAgency(Boolean expectedStatus) {
        Path sip = Paths.get(world.getBaseDirectory(), fileName);
        try (InputStream inputStream = Files.newInputStream(sip, StandardOpenOption.READ)) {
            RequestResponse response = world
                .getAdminClient()
                .createAgencies(new VitamContext(world.getTenantId()), inputStream, fileName);
            if (expectedStatus != null) {
                if (expectedStatus) {
                    assertThat(response.getHttpCode()).isEqualTo(Response.Status.CREATED.getStatusCode());
                } else {
                    assertThat(response.getHttpCode()).isNotEqualTo(Response.Status.CREATED.getStatusCode());
                }
            }
            final String operationId = response.getHeaderString(GlobalDataRest.X_REQUEST_ID);
            world.setOperationId(operationId);
        } catch (Exception e) {
            fail("should not produce this exception", e);
        }
    }

    @When("^je cherche un service producteur nommé (.*)")
    public void search_contracts(String name)
        throws AccessExternalClientException, InvalidParseOperationException, InvalidCreateOperationException, VitamClientException {
        final fr.gouv.vitam.common.database.builder.request.single.Select select =
            new fr.gouv.vitam.common.database.builder.request.single.Select();

        select.setQuery(eq("Name", name));
        final JsonNode query = select.getFinalSelect();

        RequestResponse<AgenciesModel> ingestResponse = world
            .getAdminClient()
            .findAgencies(new VitamContext(world.getTenantId()).setAccessContract("ContratTNR"), query);

        assertThat(ingestResponse.isOk()).isTrue();

        List<JsonNode> results = ((RequestResponseOK<AgenciesModel>) ingestResponse).getResultsAsJsonNodes();
        if (!results.isEmpty()) {
            this.setModel(results.get(0));
        } else {
            this.setModel(null);
        }
    }

    @Then("^le service producteur existe$")
    public void agencies_found() {
        assertThat(this.getModel()).isNotNull();
    }

    @Then("^le service producteur n'existe pas$")
    public void agencies_not_found() {
        assertThat(this.getModel()).isNull();
    }

    @Then("^les métadonnées du service sont$")
    public void metadata_are(DataTable dataTable) throws Throwable {
        List<List<String>> raws = dataTable.raw();
        for (List<String> raw : raws) {
            String index = raw.get(0);
            String value = raw.get(1);
            assertThat(value).contains(this.getModel().get(index).asText());
        }
    }
}
