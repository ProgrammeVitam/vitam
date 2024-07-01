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
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import fr.gouv.vitam.access.external.client.VitamPoolingClient;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.PreservationRequest;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.preservation.GriffinModel;
import fr.gouv.vitam.common.model.administration.preservation.PreservationScenarioModel;
import fr.gouv.vitam.common.model.objectgroup.ObjectGroupResponse;
import fr.gouv.vitam.common.model.objectgroup.QualifiersModel;
import org.assertj.core.api.Fail;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static fr.gouv.vitam.common.GlobalDataRest.X_REQUEST_ID;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.and;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static fr.gouv.vitam.common.model.PreservationVersion.FIRST;
import static fr.gouv.vitam.common.model.PreservationVersion.LAST;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Java6Assertions.fail;

/**
 * PreservationStep class
 */
public class PreservationStep extends CommonStep {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(PreservationStep.class);

    public static final String EMPTY_JSON_FILE = "empty.json";

    public PreservationStep(World world) {
        super(world);
    }

    @When("^j'importe le griffon nommé (.*)$")
    public void importGriffin(String fileName) {
        Path file = Paths.get(world.getBaseDirectory(), fileName);
        try (InputStream inputStream = Files.newInputStream(file, StandardOpenOption.READ)) {
            VitamContext vitamContext = new VitamContext(world.getTenantId());
            vitamContext.setApplicationSessionId(world.getApplicationSessionId());

            RequestResponse response = world.getAdminClient().importGriffin(vitamContext, inputStream, fileName);

            final String operationId = response.getHeaderString(X_REQUEST_ID);
            world.setOperationId(operationId);

            ArrayList<JsonNode> results = new ArrayList<>();
            results.add(JsonHandler.createObjectNode().put("Code", valueOf(response.getHttpCode())));
            world.setResults(results);
        } catch (Exception e) {
            Fail.fail("failed to  upload griffin file  " + e);
        }
    }

    @When("^j'importe le preservation Scenario nommé (.*)$")
    public void importPreservation(String fileName) {
        Path file = Paths.get(world.getBaseDirectory(), fileName);

        try (InputStream inputStream = Files.newInputStream(file, StandardOpenOption.READ)) {
            VitamContext vitamContext = new VitamContext(world.getTenantId());
            vitamContext.setApplicationSessionId(world.getApplicationSessionId());

            RequestResponse response = world
                .getAdminClient()
                .importPreservationScenario(vitamContext, inputStream, fileName);

            final String operationId = response.getHeaderString(X_REQUEST_ID);
            world.setOperationId(operationId);

            ArrayList<JsonNode> results = new ArrayList<>();
            results.add(JsonHandler.createObjectNode().put("Code", valueOf(response.getHttpCode())));
            world.setResults(results);
        } catch (Exception e) {
            Fail.fail("failed to  upload griffin file  " + e);
        }
    }

    @When("^je cherche le griffon nommé (.*)$")
    @SuppressWarnings("unchecked")
    public void searchGriffinById(String identifier) throws VitamClientException, InvalidParseOperationException {
        VitamContext vitamContext = new VitamContext(world.getTenantId());
        vitamContext.setApplicationSessionId(world.getApplicationSessionId());
        RequestResponse<GriffinModel> response = world.getAdminClient().findGriffinById(vitamContext, identifier);

        assertThat(response.getHttpCode()).isEqualTo(200);

        world.setResults((List<JsonNode>) ((RequestResponseOK) response).getResultsAsJsonNodes());
    }

    @Then("^le griffon nommé (.*) n'existe pas$")
    @SuppressWarnings("unchecked")
    public void searchNotGriffinById(String identifier) throws VitamClientException {
        VitamContext vitamContext = new VitamContext(world.getTenantId());
        vitamContext.setApplicationSessionId(world.getApplicationSessionId());
        RequestResponse<GriffinModel> response = world.getAdminClient().findGriffinById(vitamContext, identifier);

        assertThat(response.getHttpCode()).isEqualTo(404);
    }

    @Then("^le scénario de preservation nommé (.*) n'existe pas$")
    @SuppressWarnings("unchecked")
    public void searchNotExistantPreservationById(String identifier) throws VitamClientException {
        VitamContext vitamContext = new VitamContext(world.getTenantId());
        vitamContext.setApplicationSessionId(world.getApplicationSessionId());
        RequestResponse<PreservationScenarioModel> response = world
            .getAdminClient()
            .findPreservationScenarioById(vitamContext, identifier);

        assertThat(response.getHttpCode()).isEqualTo(404);
    }

    @When("^je cherche le scénario de preservation nommé (.*)$")
    @SuppressWarnings("unchecked")
    public void searchPreservationById(String identifier) throws Exception {
        VitamContext vitamContext = new VitamContext(world.getTenantId());
        vitamContext.setApplicationSessionId(world.getApplicationSessionId());
        RequestResponse<PreservationScenarioModel> response = world
            .getAdminClient()
            .findPreservationScenarioById(vitamContext, identifier);

        assertThat(response.getHttpCode()).isEqualTo(200);

        world.setResults((List<JsonNode>) ((RequestResponseOK) response).getResultsAsJsonNodes());
    }

    @When("^je supprime les griffons et les scénarios de préservation sur tous les tenants")
    public void deleteAllPreservationBaseData() throws Exception {
        //inject empty preservation scenario to tenant all tenant if contain scenario preservation
        for (Integer tenant : VitamConfiguration.getTenants()) {
            VitamContext vitamContext = new VitamContext(tenant);
            vitamContext.setApplicationSessionId(world.getApplicationSessionId());
            //search all preservation scenario in the tenant
            Select select = new Select();
            select.setQuery(and().add(QueryHelper.exists("Name")).add(eq(VitamFieldsHelper.tenant(), tenant)));
            JsonNode queryDsl = select.getFinalSelect();

            RequestResponse<PreservationScenarioModel> scenarioList = world
                .getAdminClient()
                .findPreservationScenario(vitamContext, queryDsl);
            List resultsAsJsonNodes = ((RequestResponseOK) scenarioList).getResultsAsJsonNodes();
            if (!resultsAsJsonNodes.isEmpty()) {
                InputStream emptyJson = new ByteArrayInputStream("[]".getBytes());
                world.getAdminClient().importPreservationScenario(vitamContext, emptyJson, EMPTY_JSON_FILE);
            }
        }
        VitamContext vitamContext = new VitamContext(VitamConfiguration.getAdminTenant());
        vitamContext.setApplicationSessionId(world.getApplicationSessionId());
        InputStream emptyJson = new ByteArrayInputStream("[]".getBytes());
        world.getAdminClient().importGriffin(vitamContext, emptyJson, EMPTY_JSON_FILE);
    }

    @When("^je lance la preservation avec le scénario (.*) et pour l'usage (.*)$")
    public void launchPreservation(String scenarioId, String usage) throws Exception {
        VitamContext vitamContext = new VitamContext(world.getTenantId());
        vitamContext.setApplicationSessionId(world.getApplicationSessionId());
        vitamContext.setAccessContract(world.getContractId());

        String query = world.getQuery();

        JsonNode queryNode = JsonHandler.getFromString(query);

        PreservationRequest preservationRequest = new PreservationRequest(queryNode, scenarioId, usage, LAST, usage);
        RequestResponse response = world.getAccessClient().launchPreservation(vitamContext, preservationRequest);

        final String operationId = response.getHeaderString(X_REQUEST_ID);
        world.setOperationId(operationId);

        final VitamPoolingClient vitamPoolingClient = new VitamPoolingClient(world.getAdminClient());
        boolean processTimeout = vitamPoolingClient.wait(
            world.getTenantId(),
            operationId,
            ProcessState.COMPLETED,
            100,
            1_000L,
            TimeUnit.MILLISECONDS
        );

        if (!processTimeout) {
            fail("preservation processing not finished. Timeout exceeded.");
        }

        assertThat(operationId).as(format("%s not found for request", X_REQUEST_ID)).isNotNull();
    }

    @When("^je lance la preservation du scénario (.*) pour l'usage d'entrée (.*) et pour l'usage de sortie (.*)$")
    public void launchPreservation(String scenarioId, String usageEntree, String usageSortie) throws Exception {
        VitamContext vitamContext = new VitamContext(world.getTenantId());
        vitamContext.setApplicationSessionId(world.getApplicationSessionId());
        vitamContext.setAccessContract(world.getContractId());

        String query = world.getQuery();

        JsonNode queryNode = JsonHandler.getFromString(query);

        PreservationRequest preservationRequest = new PreservationRequest(
            queryNode,
            scenarioId,
            usageSortie,
            FIRST,
            usageEntree
        );
        RequestResponse response = world.getAccessClient().launchPreservation(vitamContext, preservationRequest);

        final String operationId = response.getHeaderString(X_REQUEST_ID);
        world.setOperationId(operationId);

        final VitamPoolingClient vitamPoolingClient = new VitamPoolingClient(world.getAdminClient());
        boolean processTimeout = vitamPoolingClient.wait(
            world.getTenantId(),
            operationId,
            ProcessState.COMPLETED,
            100,
            1_000L,
            TimeUnit.MILLISECONDS
        );

        if (!processTimeout) {
            fail("preservation processing not finished. Timeout exceeded.");
        }

        assertThat(operationId).as(format("%s not found for request", X_REQUEST_ID)).isNotNull();
    }

    @Then("^le type du binaire généré est un (.*) et de type (.*)$")
    public void verifyIfTypeBinaryHasBeenGenerated(String qualifierParam, String type)
        throws InvalidParseOperationException {
        ObjectGroupResponse objectGroupResponse = JsonHandler.getFromJsonNode(
            world.getResults().get(0),
            ObjectGroupResponse.class
        );

        String opiPreservation = world.getOperationId();

        Boolean versionExist = objectGroupResponse
            .getQualifiers()
            .stream()
            .filter(qualifier -> qualifierParam.equals(qualifier.getQualifier()))
            .map(QualifiersModel::getVersions)
            .flatMap(List::stream)
            .anyMatch(
                version ->
                    type.equals(version.getFormatIdentification().getMimeType()) &&
                    version.getOpi().equals(opiPreservation)
            );

        world.setOperationId(objectGroupResponse.getOpi());

        assertThat(versionExist).isEqualTo(true);
    }

    @Then("^Le fichier binaire du type du qualifier (.*) n'a pas été généré$")
    public void verifyBinaryIfNotGenerated(String qualifierParam) throws InvalidParseOperationException {
        ObjectGroupResponse objectGroupResponse = JsonHandler.getFromJsonNode(
            world.getResults().get(0),
            ObjectGroupResponse.class
        );

        String opiPreservation = world.getOperationId();

        boolean versionExist = objectGroupResponse
            .getQualifiers()
            .stream()
            .filter(qualifier -> qualifierParam.equals(qualifier.getQualifier()))
            .map(QualifiersModel::getVersions)
            .flatMap(List::stream)
            .anyMatch(version -> version.getOpi().equals(opiPreservation));

        world.setOperationId(objectGroupResponse.getOpi());
        assertThat(versionExist).isEqualTo(false);
    }
}
