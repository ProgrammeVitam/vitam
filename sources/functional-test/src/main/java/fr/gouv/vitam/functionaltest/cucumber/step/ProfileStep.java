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
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.ProfileModel;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.match;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Profile Step
 */
public class ProfileStep extends CommonStep {

    public ProfileStep(World world) {
        super(world);
    }

    /**
     * ★★★ The file name ★★★
     */
    private String fileName;

    /**
     * generic model result
     */
    private JsonNode model;

    /**
     * ★★★ define a sip ★★★
     *
     * @param fileName name of a sip
     */
    @Given("^un profil nommé (.*)$")
    public void a_profile_named(String fileName) {
        this.fileName = fileName;
    }

    /**
     * @throws InvalidParseOperationException
     * @throws IOException
     * @throws AccessExternalClientException
     */
    @When("^j'importe le profile d'archivage$")
    public void create_profile() throws AccessExternalClientException, IOException, InvalidParseOperationException {
        create_profile(true);
    }

    /**
     * @throws InvalidParseOperationException
     * @throws IOException
     * @throws AccessExternalClientException
     */
    @When("^j'importe le profile d'archivage sans échec$")
    public void create_profile_ignoring_failure()
        throws AccessExternalClientException, IOException, InvalidParseOperationException {
        create_profile(null);
    }

    @When("^j'importe le profile d'archivage incorrect$")
    public void create_profile_with_expected_failure()
        throws AccessExternalClientException, IOException, InvalidParseOperationException {
        create_profile(false);
    }

    private void create_profile(Boolean expectedSuccessStatus)
        throws InvalidParseOperationException, IOException, AccessExternalClientException {
        Path profil = Paths.get(world.getBaseDirectory(), fileName);
        final RequestResponse response = world
            .getAdminClient()
            .createProfiles(
                new VitamContext(world.getTenantId()).setApplicationSessionId(world.getApplicationSessionId()),
                Files.newInputStream(profil, StandardOpenOption.READ)
            );

        String httpCode = String.valueOf(response.getHttpCode());
        ObjectNode responseCode = JsonHandler.createObjectNode();
        responseCode.put("Code", httpCode);
        List<JsonNode> result = new ArrayList<>();
        result.add(responseCode);
        world.setResults(result);

        final String operationId = response.getHeaderString(GlobalDataRest.X_REQUEST_ID);
        world.setOperationId(operationId);
        if (expectedSuccessStatus != null) {
            if (expectedSuccessStatus) {
                assertThat(response.getStatus()).isEqualTo(Response.Status.CREATED.getStatusCode());
                RequestResponseOK<ProfileModel> res = (RequestResponseOK) response;
                Object o = (res.getResults().stream().findFirst()).get();
                this.model = (JsonNode) o;
            } else {
                assertThat(response.getStatus()).isNotEqualTo(Response.Status.CREATED.getStatusCode());
            }
        }
    }

    @When("^je rattache un ficher à ce profil d'archivage$")
    public void import_profile() throws InvalidParseOperationException, IOException, AccessExternalClientException {
        import_profile(true);
    }

    @When("^je rattache un ficher à ce profil d'archivage sans échec$")
    public void import_profile_ignoring_failure()
        throws InvalidParseOperationException, IOException, AccessExternalClientException {
        import_profile(null);
    }

    @When("^je rattache un ficher incorrect à ce profil d'archivage")
    public void import_profile_with_expected_failure()
        throws InvalidParseOperationException, IOException, AccessExternalClientException {
        import_profile(false);
    }

    private void import_profile(Boolean expectedSuccessStatus)
        throws InvalidParseOperationException, IOException, AccessExternalClientException {
        Path profile = Paths.get(world.getBaseDirectory(), fileName);
        RequestResponse response = null;
        if (this.model != null && this.model.get("Identifier") != null) {
            response = world
                .getAdminClient()
                .createProfileFile(
                    new VitamContext(world.getTenantId()).setApplicationSessionId(world.getApplicationSessionId()),
                    this.model.get("Identifier").asText(),
                    Files.newInputStream(profile, StandardOpenOption.READ)
                );
        }
        if (expectedSuccessStatus != null) {
            assertThat(response).isNotNull();
            assertThat(response.isOk()).isEqualTo(expectedSuccessStatus);
        }
    }

    @When("^je cherche un profil nommé (.*)")
    public void search_profiles(String name)
        throws AccessExternalClientException, InvalidParseOperationException, InvalidCreateOperationException, VitamClientException {
        final fr.gouv.vitam.common.database.builder.request.single.Select select =
            new fr.gouv.vitam.common.database.builder.request.single.Select();
        select.setQuery(match("Name", name));
        final JsonNode query = select.getFinalSelect();
        RequestResponse<ProfileModel> requestResponse = world
            .getAdminClient()
            .findProfiles(
                new VitamContext(world.getTenantId())
                    .setAccessContract(null)
                    .setApplicationSessionId(world.getApplicationSessionId()),
                query
            );
        if (requestResponse.isOk()) {
            this.model = ((RequestResponseOK<ProfileModel>) requestResponse).getResultsAsJsonNodes().get(0);
        }
    }

    @Then("^le profil existe$")
    public void profile_found() {
        assertThat(this.model).isNotNull();
    }

    @Then("^le profil n'existe pas$")
    public void profile_not_found() {
        assertThat(this.model).isNull();
    }

    @Then("^les métadonnées du profil sont$")
    public void metadata_are(DataTable dataTable) throws Throwable {
        List<List<String>> raws = dataTable.raw();
        for (List<String> raw : raws) {
            String index = raw.get(0);
            String value = raw.get(1);
            assertThat(this.model.get(index).asText()).contains(value);
        }
    }
}
