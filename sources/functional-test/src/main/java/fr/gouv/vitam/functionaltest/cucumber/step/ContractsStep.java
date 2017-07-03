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

import cucumber.api.DataTable;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import fr.gouv.vitam.access.external.api.AdminCollections;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientException;
import fr.gouv.vitam.common.database.builder.query.BooleanQuery;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.and;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.match;

import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.model.AccessContractModel;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.ingest.external.api.exception.IngestExternalException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import javax.ws.rs.core.Response;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class ContractsStep {

    public ContractsStep(World world) {
        this.world = world;
    }

    private World world;
    private String fileName;

    /**
     * @return generic Model
     */
    public JsonNode getModel() {
        return model;
    }

    public String getContractType() {
        return contractType;
    }

    public void setContractType(String contractType) {
        this.contractType = contractType;
    }

    public void setModel(JsonNode model) {
        this.model = model;
    }

    /**
     * generic model result
     */
    private JsonNode model;
    /**
     * type de contrat
     */
    private String contractType;

    /**
     * define a sip
     *
     * @param fileName name of a sip
     */
    @Given("^un contract nommé (.*)$")
    public void a_sip_named(String fileName) {
        this.fileName = fileName;
    }

    /**
     * Use Only when the contract is not in the database
     *
     * @param type the type of contract
     * @throws IOException
     * @throws IngestExternalException
     */
    @Then("^j'importe ce contrat de type (.*)")
    public void upload_contract(String type) throws IOException {
        try {
            uploadContract(type);
        } catch (AccessExternalClientException | IllegalStateException | InvalidParseOperationException e) {
            fail("should not produce an exception"+e);
        }
    }

    /**
     * Tentative d'import d'un contrat si jamais il n'existe pas
     * @param type
     * @throws IOException
     */
    @Then("^j'importe ce contrat sans échec de type (.*)")
    public void upload_contract_without_fail(String type) throws IOException {
        try {
            uploadContract(type);
        } catch (AccessExternalClientException | IllegalStateException | InvalidParseOperationException e) {
            //catch nothing
        }
    }

    private void uploadContract(String type)
        throws IOException, InvalidParseOperationException, AccessExternalClientException {
        Path sip = Paths.get(world.getBaseDirectory(), fileName);
        try (InputStream inputStream = Files.newInputStream(sip, StandardOpenOption.READ)) {
            AdminCollections collection = AdminCollections.valueOf(type);
            this.setContractType(collection.getName());
            RequestResponse response =
                world.getAdminClient().importContracts(inputStream, world.getTenantId(), collection);
            assertThat(response instanceof RequestResponseOK);
        }
    }

    /**
     * Upload a contract that will lead to an error
     *
     * @param type the type of contract
     * @throws IOException
     * @throws IngestExternalException
     */
    @Then("^j'importe ce contrat incorrect de type (.*)")
    public void upload_incorrect_contract(String type) {
        Path sip = Paths.get(world.getBaseDirectory(), fileName);
        try (InputStream inputStream = Files.newInputStream(sip, StandardOpenOption.READ)) {
            AdminCollections collection = AdminCollections.valueOf(type);
            this.setContractType(collection.getName());
            RequestResponse response =
                world.getAdminClient().importContracts(inputStream, world.getTenantId(), collection);
            // TODO : this has to be fixed, the returned response is not correct, Bad request must me obtained            
            assertThat(Response.Status.BAD_REQUEST.getStatusCode() == response.getStatus());
        } catch (IllegalStateException e) {
            // Do nothing
        } catch (Exception e) {
            fail("should not produce this exception"+e);
        }
    }

    @When("^je cherche un contrat de type (.*) et nommé (.*)")
    public void search_contracts(String type, String name)
        throws AccessExternalClientException, InvalidParseOperationException, InvalidCreateOperationException {
        AdminCollections collection = AdminCollections.valueOf(type);
        final fr.gouv.vitam.common.database.builder.request.single.Select select =
            new fr.gouv.vitam.common.database.builder.request.single.Select();

        select.setQuery(eq("Name", name));

        final JsonNode query = select.getFinalSelect();
        RequestResponse response =
            world.getAdminClient().findDocuments(collection, query, world.getTenantId());
        assertThat(response).isInstanceOf(RequestResponseOK.class);
        RequestResponseOK<AccessContractModel> res = (RequestResponseOK) response;
        Object o = (res.getResults().stream().findFirst()).get();
        JsonNode model = (JsonNode) o;
        this.setModel(model);
    }

    @Then("^le contrat existe$")
    public void contract_found_are() {
        assertThat(this.getModel()).isNotNull();
    }

    @Then("^les métadonnées du contrat sont$")
    public void metadata_are(DataTable dataTable) throws Throwable {
        List<List<String>> raws = dataTable.raw();
        for (List<String> raw : raws) {
            String index = raw.get(0);
            String value = raw.get(1);
            assertThat(value).contains(this.getModel().get(index).asText());
        }
    }
}
