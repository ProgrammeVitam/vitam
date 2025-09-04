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
import fr.gouv.vitam.collect.common.dto.ProjectDto;
import fr.gouv.vitam.collect.common.dto.TransactionDto;
import fr.gouv.vitam.common.FileUtil;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import jakarta.ws.rs.core.Response;
import net.javacrumbs.jsonunit.JsonAssert;
import net.javacrumbs.jsonunit.core.Option;
import org.apache.commons.lang3.time.StopWatch;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.Fail;
import org.junit.Assume;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static jakarta.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

/**
 * step defining collect behaviors
 */
public class CollectStep extends CommonStep {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CollectStep.class);
    public static final String TRANSACTION_RETURN_AN_ERROR = "request find_transaction return an error: ";
    private static final String CREATED = "créé";
    private static final String DELETED = "supprimé";
    private static final String UNIT_UP = "Unit-up";

    public CollectStep(World world) {
        super(world);
    }

    /**
     * This check is done before every scenario and it avoids failures if collect (external) service is not deployed!
     */
    @Given("^Le module de collect est deployé")
    public void checkSkippableScenario() {
        try {
            world.getCollectExternalClient().checkStatus();
        } catch (Exception e) {
            LOGGER.warn("Collect service is not deployed or not up!");
            Assume.assumeTrue(false);
        }
    }

    @When("^j'initialise le project")
    public void init_project() throws Throwable {
        ProjectDto queryJSON = JsonHandler.getFromString(world.getQuery(), ProjectDto.class);

        RequestResponse<JsonNode> resultedRequestResponse = world
            .getCollectExternalClient()
            .initProject(new VitamContext(world.getTenantId()), queryJSON);

        if (resultedRequestResponse.isOk()) {
            RequestResponseOK<JsonNode> requestResponseOK = (RequestResponseOK<JsonNode>) resultedRequestResponse;
            assertThat(requestResponseOK.getResults()).isNotEmpty();
            world.setResults(requestResponseOK.getResults());
            ProjectDto projectDto = JsonHandler.getFromString(world.getResults().get(0).toString(), ProjectDto.class);
            world.setProjectId(projectDto.getId());
        } else {
            VitamError vitamError = (VitamError) resultedRequestResponse;
            Fail.fail("request initProject return an error: " + vitamError.getCode());
        }
    }

    /**
     * define a json from a file to reuse it after
     *
     * @param jsonFilename name of the file containing the json
     * @throws Throwable
     */
    @When("^j'utilise le fichier json suivant (.*)$")
    public void i_use_the_following_file_query(String jsonFilename) throws Throwable {
        Path jsonFile = Paths.get(world.getBaseDirectory(), jsonFilename);
        String json = FileUtil.readFile(jsonFile.toFile());
        json = replaceUnitUp(json);

        world.setQuery(json);
    }

    private String replaceUnitUp(String query) {
        if (world.getUnitId() != null) {
            query = query.replace(UNIT_UP, world.getUnitId());
        }
        return query;
    }

    @When("^je recherche le projet")
    public void find_project() throws Throwable {
        ProjectDto projectDto = JsonHandler.getFromString(world.getResults().get(0).toString(), ProjectDto.class);
        RequestResponse<JsonNode> requestResponse = world
            .getCollectExternalClient()
            .getProjectById(new VitamContext(world.getTenantId()), projectDto.getId());

        if (requestResponse.isOk()) {
            RequestResponseOK<JsonNode> requestResponseOK = (RequestResponseOK<JsonNode>) requestResponse;
            assertThat(requestResponseOK.getResults()).isNotEmpty();
            world.setResults(requestResponseOK.getResults());
        } else {
            VitamError vitamError = (VitamError) requestResponse;
            Fail.fail("request initProject return an error: " + vitamError.getCode());
        }
    }

    /**
     * update field name of project
     *
     * @param name
     * @throws Throwable
     */
    @When("^je met a jour le projet avec le nom (.*)$")
    public void update_project(String name) throws Throwable {
        ProjectDto projectDto = JsonHandler.getFromString(world.getResults().get(0).toString(), ProjectDto.class);
        projectDto.setName(name);
        RequestResponse<JsonNode> requestResponse = world
            .getCollectExternalClient()
            .updateProject(new VitamContext(world.getTenantId()), projectDto);
        if (requestResponse.isOk()) {
            RequestResponseOK<JsonNode> requestResponseOK = (RequestResponseOK<JsonNode>) requestResponse;
            assertThat(requestResponseOK.getResults()).isNotEmpty();
            world.setResults(requestResponseOK.getResults());
            ProjectDto updatedProjectDto = JsonHandler.getFromString(
                world.getResults().get(0).toString(),
                ProjectDto.class
            );
            assertThat(updatedProjectDto.getName()).isEqualTo(projectDto.getName());
            assertThat(updatedProjectDto.getId()).isEqualTo(projectDto.getId());
        } else {
            VitamError vitamError = (VitamError) requestResponse;
            Fail.fail("request update project return an error: " + vitamError.getCode());
        }
    }

    /**
     * update field name of transaction
     *
     * @param name
     */
    @When("^je met a jour la transaction avec le nom (.*)$")
    public void update_transaction(String name) throws Exception {
        TransactionDto transactionDto = JsonHandler.getFromString(
            world.getResults().get(0).toString(),
            TransactionDto.class
        );
        transactionDto.setName(name);
        RequestResponse<JsonNode> requestResponse = world
            .getCollectExternalClient()
            .updateTransaction(new VitamContext(world.getTenantId()), transactionDto);
        if (requestResponse.isOk()) {
            RequestResponseOK<JsonNode> requestResponseOK = (RequestResponseOK<JsonNode>) requestResponse;
            assertThat(requestResponseOK.getResults()).isNotEmpty();
            world.setResults(requestResponseOK.getResults());
            TransactionDto updatedTransactionDto = JsonHandler.getFromString(
                world.getResults().get(0).toString(),
                TransactionDto.class
            );
            assertThat(updatedTransactionDto.getName()).isEqualTo(transactionDto.getName());
            assertThat(updatedTransactionDto.getId()).isEqualTo(transactionDto.getId());
        } else {
            VitamError vitamError = (VitamError) requestResponse;
            Fail.fail("request update project return an error: " + vitamError.getCode());
        }
    }

    /**
     * purge project
     *
     * @throws Throwable
     */
    @When("^je supprime le projet")
    public void purge_project() throws Throwable {
        ProjectDto projectDto = JsonHandler.getFromString(world.getResults().get(0).toString(), ProjectDto.class);
        RequestResponse<JsonNode> requestResponse = world
            .getCollectExternalClient()
            .deleteProjectById(new VitamContext(world.getTenantId()), projectDto.getId());
        if (requestResponse.getHttpCode() != 200) {
            VitamError vitamError = (VitamError) requestResponse;
            Fail.fail("request purge project return an error: " + vitamError.getCode());
        }
    }

    /**
     * Check result of action
     *
     * @param action
     */
    @Then("^le projet est (.*) en succès$")
    public void checkOperationProject(String action) throws InvalidParseOperationException {
        ProjectDto projectDto = JsonHandler.getFromString(world.getResults().get(0).toString(), ProjectDto.class);
        switch (action) {
            case CREATED:
                checkCreatedProject(projectDto);
                break;
            case DELETED:
                checkDeletedProject(projectDto);
                break;
            default:
                Fail.fail("Not recognized action !");
                break;
        }
    }

    private void checkDeletedProject(ProjectDto projectDto) {
        assertThatThrownBy(
            () ->
                world
                    .getCollectExternalClient()
                    .getProjectById(new VitamContext(world.getTenantId()), projectDto.getId())
        ).isInstanceOf(VitamClientException.class);
    }

    private void checkCreatedProject(ProjectDto projectDto) throws InvalidParseOperationException {
        try {
            RequestResponse<JsonNode> requestResponse = world
                .getCollectExternalClient()
                .getProjectById(new VitamContext(world.getTenantId()), projectDto.getId());
            RequestResponseOK<JsonNode> requestResponseOK = (RequestResponseOK<JsonNode>) requestResponse;
            assertThat(requestResponseOK.getResults()).isNotEmpty();
            world.setResults(requestResponseOK.getResults());
            ProjectDto createdProject = JsonHandler.getFromString(
                world.getResults().get(0).toString(),
                ProjectDto.class
            );
            assertThat(createdProject.getId()).isEqualTo(projectDto.getId());
            assertThat(createdProject.getName()).isEqualTo(projectDto.getName());
        } catch (VitamClientException e) {
            Fail.fail(e.getMessage());
        }
    }

    @When("^j'initialise une transaction")
    public void init_transaction() throws Throwable {
        TransactionDto queryJSON = JsonHandler.getFromString(world.getQuery(), TransactionDto.class);

        RequestResponse<JsonNode> requestResponse = world
            .getCollectExternalClient()
            .initTransaction(new VitamContext(world.getTenantId()), queryJSON, world.getProjectId());

        if (requestResponse.isOk()) {
            RequestResponseOK<JsonNode> requestResponseOK = (RequestResponseOK<JsonNode>) requestResponse;
            assertThat(requestResponseOK.getResults()).isNotEmpty();
            world.setResults(requestResponseOK.getResults());
        } else {
            VitamError vitamError = (VitamError) requestResponse;
            Fail.fail("request initTransaction return an error: " + vitamError.getCode());
        }
    }

    @When("^je recherche la transaction")
    public void find_transaction() throws Throwable {
        TransactionDto transactionDto = JsonHandler.getFromString(
            world.getResults().get(0).toString(),
            TransactionDto.class
        );
        RequestResponse<JsonNode> requestResponse = world
            .getCollectExternalClient()
            .getTransactionById(new VitamContext(world.getTenantId()), transactionDto.getId());

        if (requestResponse.isOk()) {
            RequestResponseOK<JsonNode> requestResponseOK = (RequestResponseOK<JsonNode>) requestResponse;
            assertThat(requestResponseOK.getResults()).isNotEmpty();
            world.setResults(requestResponseOK.getResults());
            TransactionDto myTransactionDto = JsonHandler.getFromString(
                world.getResults().get(0).toString(),
                TransactionDto.class
            );
            world.setTransactionId(myTransactionDto.getId());
            assertThat(myTransactionDto.getStatus()).isEqualTo("OPEN");
        } else {
            VitamError vitamError = (VitamError) requestResponse;
            Fail.fail(TRANSACTION_RETURN_AN_ERROR + vitamError.getCode());
        }
    }

    @When("^je crée une au")
    public void uploadUnit() throws Exception {
        String transactionId = world.getTransactionId();
        JsonNode archiveUnitJson = JsonHandler.getFromString(world.getQuery());
        RequestResponse<JsonNode> response = world
            .getCollectExternalClient()
            .uploadArchiveUnit(new VitamContext(world.getTenantId()), archiveUnitJson, transactionId);
        assertThat(response.isOk()).isTrue();
        assertThat(((RequestResponseOK) response).getFirstResult()).isNotNull();
        assertThat(((RequestResponseOK<JsonNode>) response).getFirstResult().get("#id")).isNotNull();
        world.setUnitId(((RequestResponseOK<JsonNode>) response).getFirstResult().get("#id").asText());
    }

    @When("^je recherche l'au par rapport à son titre")
    public void getUnitByDslQuery() throws Exception {
        String transactionId = world.getTransactionId();

        String unitDsl =
            "{\"$roots\": [],\"$query\": [{\"$eq\" : {\"Title\":\"My title3\"}}],\"$filter\": {\"$offset\": 0,\"$limit\": 100},\"$projection\": {}}";

        RequestResponse<JsonNode> response = world
            .getCollectExternalClient()
            .getUnitsByTransaction(
                new VitamContext(world.getTenantId()),
                transactionId,
                JsonHandler.getFromString(unitDsl)
            );
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(((RequestResponseOK) response).getFirstResult()).isNotNull();
        assertThat(((RequestResponseOK) response).getHits().getTotal()).isEqualTo(1);
    }

    @When("^je crée un GOT")
    public void uploadGot() throws Exception {
        String unitId = world.getUnitId();
        Integer version = 1;
        String usage = "TextContent";

        JsonNode gotJson = JsonHandler.getFromString(world.getQuery());
        RequestResponse<JsonNode> response = world
            .getCollectExternalClient()
            .addObjectGroup(new VitamContext(world.getTenantId()), unitId, version, gotJson, usage);
        assertThat(response.isOk()).isTrue();
        assertThat(((RequestResponseOK) response).getFirstResult()).isNotNull();
        assertThat(((RequestResponseOK<JsonNode>) response).getFirstResult().get("_id")).isNotNull();
        ((RequestResponseOK<JsonNode>) response).getFirstResult().get("_id").textValue();
    }

    @When("^j'upload le fichier suivant (.*)$")
    public void uploadBinary(String binaryFilename) throws Exception {
        String unitId = world.getUnitId();
        Integer version = 1;
        String usage = "TextContent";

        try (InputStream inputStream = Files.newInputStream(Paths.get(world.getBaseDirectory(), binaryFilename))) {
            Response response = world
                .getCollectExternalClient()
                .addBinary(new VitamContext(world.getTenantId()), unitId, version, inputStream, usage);
            Assertions.assertThat(response.getStatus()).isEqualTo(200);
        }
    }

    @When("^je clôture et je constate son statut (.*) ou (.*)$")
    public void closeTransaction(String status1, String status2) throws Exception {
        String transactionId = world.getTransactionId();
        RequestResponse response = world
            .getCollectExternalClient()
            .closeTransaction(new VitamContext(world.getTenantId()), transactionId);
        Assertions.assertThat(response.getStatus()).isEqualTo(200);

        RequestResponse<JsonNode> requestResponse = world
            .getCollectExternalClient()
            .getTransactionById(new VitamContext(world.getTenantId()), world.getTransactionId());
        if (requestResponse.isOk()) {
            RequestResponseOK<JsonNode> requestResponseOK = (RequestResponseOK<JsonNode>) requestResponse;
            assertThat(requestResponseOK.getResults()).isNotEmpty();
            TransactionDto myTransactionDto = JsonHandler.getFromString(
                requestResponseOK.getResults().get(0).toString(),
                TransactionDto.class
            );
            assertThat(myTransactionDto.getStatus()).isIn(status1, status2);
        } else {
            VitamError vitamError = (VitamError) requestResponse;
            Fail.fail(TRANSACTION_RETURN_AN_ERROR + vitamError.getCode());
        }
    }

    @When("^j'envoie le SIP et je constate son statut (.*)$")
    public void sentSip(String status) throws Exception {
        RequestResponse response = world
            .getCollectExternalClient()
            .ingest(new VitamContext(world.getTenantId()), world.getTransactionId());
        if (SUCCESSFUL.equals((response).getStatus())) {
            Assertions.assertThat(response.getStatus()).isEqualTo(200);
        }

        RequestResponse<JsonNode> requestResponse = world
            .getCollectExternalClient()
            .getTransactionById(new VitamContext(world.getTenantId()), world.getTransactionId());
        if (requestResponse.isOk()) {
            RequestResponseOK<JsonNode> requestResponseOK = (RequestResponseOK<JsonNode>) requestResponse;
            assertThat(requestResponseOK.getResults()).isNotEmpty();
            TransactionDto myTransactionDto = JsonHandler.getFromString(
                requestResponseOK.getResults().get(0).toString(),
                TransactionDto.class
            );
            assertThat(myTransactionDto.getStatus()).isEqualTo(status);
            assertThat(myTransactionDto.getVitamOperationId()).isNotNull();
            assertThat(myTransactionDto.getVitamOperationId()).isNotEmpty();
        } else {
            VitamError vitamError = (VitamError) requestResponse;
            Fail.fail(TRANSACTION_RETURN_AN_ERROR + vitamError.getCode());
        }
    }

    public boolean waitTransaction(String status, int nbTry, long timeWait, TimeUnit timeUnit) throws VitamException {
        StopWatch stopWatch = StopWatch.createStarted();
        do {
            RequestResponse<JsonNode> requestResponse = world
                .getCollectExternalClient()
                .getTransactionById(new VitamContext(world.getTenantId()), world.getTransactionId());
            if (requestResponse.isOk()) {
                RequestResponseOK<JsonNode> requestResponseOK = (RequestResponseOK<JsonNode>) requestResponse;
                TransactionDto myTransactionDto = JsonHandler.getFromString(
                    requestResponseOK.getResults().get(0).toString(),
                    TransactionDto.class
                );

                if (status.equals(myTransactionDto.getStatus())) {
                    return true;
                } else if (stopWatch.getTime(TimeUnit.MINUTES) > 15) {
                    return false;
                }

                if (null != timeUnit) {
                    timeWait = timeUnit.toMillis(timeWait);
                }
                nbTry--;

                if (nbTry > 0) {
                    try {
                        Thread.sleep(timeWait);
                    } catch (InterruptedException e) {
                        SysErrLogger.FAKE_LOGGER.ignoreLog(e);
                    }
                }
            } else {
                throw new VitamException((VitamError) requestResponse);
            }
        } while (nbTry > 0);
        return false;
    }

    @Given("^je reçois un statut OK depuis l'ingest et je constate son statut (.*)$")
    public void verifyOkStatus(String status) throws Exception {
        verifyStatus(status);
    }

    @Given("^je reçois un statut KO depuis l'ingest et je constate son statut (.*)$")
    public void verifyKOStatus(String status) throws Exception {
        verifyStatus(status);
    }

    public void verifyStatus(String status) throws Exception {
        boolean processTimeout = waitTransaction(status, 100, 5_000L, TimeUnit.MILLISECONDS);
        if (!processTimeout) {
            fail("Sip processing not finished : operation (" + world.getOperationId() + "). Timeout exceeded.");
        }

        RequestResponse<JsonNode> requestResponse = world
            .getCollectExternalClient()
            .getTransactionById(new VitamContext(world.getTenantId()), world.getTransactionId());
        if (requestResponse.isOk()) {
            RequestResponseOK<JsonNode> requestResponseOK = (RequestResponseOK<JsonNode>) requestResponse;
            assertThat(requestResponseOK.getResults()).isNotEmpty();
            TransactionDto myTransactionDto = JsonHandler.getFromString(
                requestResponseOK.getResults().get(0).toString(),
                TransactionDto.class
            );

            assertThat(myTransactionDto.getStatus()).isEqualTo(status);
            world.setOperationId(myTransactionDto.getVitamOperationId());
        } else {
            VitamError vitamError = (VitamError) requestResponse;
            Fail.fail(TRANSACTION_RETURN_AN_ERROR + vitamError.getCode());
        }
    }

    @When("^je vérifie que l'unité est rattaché au noeud de l'arbre de positionnement$")
    public void verify_rattachement() throws Throwable {
        JsonNode unit = world.getResults().get(0);
        assertThat(unit.get("#unitups")).isNotNull().anyMatch(node -> node.asText().equals(world.getUnitId()));
    }

    @When("^j'envoie l'arborescence bureautique suivante (.*)$")
    public void should_upload_project_zip(String arboFileName) throws Exception {
        try (InputStream inputStream = Files.newInputStream(Paths.get(world.getBaseDirectory(), arboFileName))) {
            RequestResponse<JsonNode> response = world
                .getCollectExternalClient()
                .uploadZipToTransaction(
                    new VitamContext(world.getTenantId()),
                    world.getTransactionId(),
                    inputStream,
                    null,
                    null
                );
            Assertions.assertThat(response.getStatus()).isEqualTo(200);
        }
    }

    @When("^j'envoie un fichier de mise à jour CSV (.*)$")
    public void should_upload_metadata_csv(String arboFileName) throws Exception {
        try (InputStream inputStream = Files.newInputStream(Paths.get(world.getBaseDirectory(), arboFileName))) {
            RequestResponse<JsonNode> response = world
                .getCollectExternalClient()
                .updateUnitsWithCsvMetadata(
                    new VitamContext(world.getTenantId()),
                    world.getTransactionId(),
                    inputStream
                );
            Assertions.assertThat(response.getStatus()).isEqualTo(200);
        }
    }

    @When("^j'envoie un fichier de mise à jour JSONL (.*)$")
    public void should_upload_metadata_jsonl(String arboFileName) throws Exception {
        try (InputStream inputStream = Files.newInputStream(Paths.get(world.getBaseDirectory(), arboFileName))) {
            RequestResponse<JsonNode> response = world
                .getCollectExternalClient()
                .updateUnitsWithJsonlMetadata(
                    new VitamContext(world.getTenantId()),
                    world.getTransactionId(),
                    inputStream
                );
            Assertions.assertThat(response.getStatus()).isEqualTo(200);
        }
    }

    @When("^je constate que des métadonnées correspondent au fichier json (.+)$")
    public void json_metadata_are_for_particular_result(String filename) throws Throwable {
        RequestResponse<JsonNode> requestResponse = world
            .getCollectExternalClient()
            .getUnitsByTransaction(
                new VitamContext(world.getTenantId()),
                world.getTransactionId(),
                new SelectMultiQuery().getFinalSelect()
            );
        Path file = Paths.get(world.getBaseDirectory(), filename);
        JsonNode expectedJson;
        try (InputStream inputStream = Files.newInputStream(file, StandardOpenOption.READ)) {
            expectedJson = JsonHandler.getFromInputStream(inputStream);
        }
        RequestResponseOK<JsonNode> requestResponseOK = (RequestResponseOK<JsonNode>) requestResponse;
        assertThat(requestResponseOK.getResults()).isNotEmpty();

        JsonAssert.assertJsonEquals(
            JsonHandler.toJsonNode(requestResponseOK.getResults()),
            expectedJson,
            JsonAssert.when(Option.IGNORING_ARRAY_ORDER).whenIgnoringPaths(
                List.of(
                    "[*]." + VitamFieldsHelper.initialOperation(),
                    "[*]." + VitamFieldsHelper.id(),
                    "[*]." + VitamFieldsHelper.batchId(),
                    "[*]." + VitamFieldsHelper.unitups(),
                    "[*]." + VitamFieldsHelper.object(),
                    "[*]." + VitamFieldsHelper.allunitups(),
                    "[*]." + VitamFieldsHelper.initialOperation(),
                    "[*]." + VitamFieldsHelper.approximateCreationDate(),
                    "[*]." + VitamFieldsHelper.version(),
                    "[*]." + VitamFieldsHelper.unitType(),
                    "[*]." + VitamFieldsHelper.min(),
                    "[*]." + VitamFieldsHelper.max(),
                    "[*]." + VitamFieldsHelper.tenant(),
                    "[*]." + VitamFieldsHelper.originatingAgencies(),
                    "[*]." + VitamFieldsHelper.approximateUpdateDate()
                )
            )
        );
    }

    @When("^je constate qu'une AU ainsi qu'un GOT sont créés")
    public void should_find_au() throws Exception {
        RequestResponse<JsonNode> requestResponse = world
            .getCollectExternalClient()
            .getUnitsByTransaction(
                new VitamContext(world.getTenantId()),
                world.getTransactionId(),
                new SelectMultiQuery().getFinalSelect()
            );
        if (requestResponse.isOk()) {
            RequestResponseOK<JsonNode> requestResponseOK = (RequestResponseOK<JsonNode>) requestResponse;
            assertThat(requestResponseOK.getResults()).isNotEmpty();
            assertThat(requestResponseOK.getFirstResult().get("#id")).isNotNull();
            Optional<JsonNode> got = requestResponseOK
                .getResults()
                .stream()
                .filter(jsonNode -> "Item".equals(jsonNode.get("DescriptionLevel").asText()))
                .findFirst();
            assertThat(got).isNotNull();
        } else {
            VitamError vitamError = (VitamError) requestResponse;
            Fail.fail(TRANSACTION_RETURN_AN_ERROR + vitamError.getCode());
        }
    }
}
