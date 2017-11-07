/**
 * objectRequestResponseOK * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
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

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.and;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.in;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.assertj.core.api.AutoCloseableSoftAssertions;
import org.assertj.core.api.Fail;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Iterables;

import cucumber.api.DataTable;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import fr.gouv.vitam.access.external.api.AdminCollections;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientException;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientNotFoundException;
import fr.gouv.vitam.common.FileUtil;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTIONARGS;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.AccessUnauthorizedException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.model.logbook.LogbookLifecycle;
import fr.gouv.vitam.common.model.logbook.LogbookOperation;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;

/**
 * step defining access glue
 */
public class AccessStep {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AccessStep.class);

    private static final String UNIT_GUID = "UNIT_GUID";

    private static String CONTRACT_WITH_LINK = "[{" +
        "\"Name\":\"contrat_de_rattachement_TNR\"," +
        "\"Description\":\"Rattachant les SIP à une AU\"," +
        "\"Status\" : \"ACTIVE\"," +
        "\"LastUpdate\":\"10/12/2016\"," +
        "\"CreationDate\":\"10/12/2016\"," +
        "\"ActivationDate\":\"10/12/2016\"," +
        "\"DeactivationDate\":\"10/12/2016\"," +
        "\"LinkParentId\": \"" + UNIT_GUID + "\"}]";

    private static final String OPERATION_ID = "Operation-Id";

    private static final String UNIT_PREFIX = "unit:";

    private static final String REGEX = "(\\{\\{(.*?)\\}\\})";

    private static final String TITLE = "Title";

    private List<JsonNode> results;

    private World world;

    private String query;

    private StatusCode statusCode;

    private Status auditStatus;
    private static String savedUnit;
    private RequestResponse requestResponse;

    public AccessStep(World world) {
        this.world = world;
    }

    /**
     * check if the metadata are valid.
     *
     * @param dataTable dataTable
     * @throws Throwable
     */
    @Then("^les metadonnées sont$")
    public void metadata_are(DataTable dataTable) throws Throwable {


        JsonNode firstJsonNode = Iterables.get(results, 0);

        List<List<String>> raws = dataTable.raw();

        for (List<String> raw : raws) {
            String key = raw.get(0);
            boolean isArray = false;
            boolean isOfArray = false;


            if (null != key && key.endsWith(".array[][]")) {
                key = key.replace(".array[][]", "");
                isOfArray = true;
            }

            if (null != key && key.endsWith(".array[]")) {
                key = key.replace(".array[]", "");
                isArray = true;
            }

            String resultValue = getResultValue(firstJsonNode, key);
            if (null != resultValue) {
                resultValue = resultValue.replace("\n", "").replace("\\n", "");
            }
            String resultExpected = transformToGuid(raw.get(1));
            if (null != resultExpected) {
                resultExpected = resultExpected.replace("\n", "").replace("\\n", "");
            }

            if (!isArray && !isOfArray) {
                assertThat(resultValue).contains(resultExpected);
            } else {
                if (isArray) {
                    Set<String> resultArray =
                        JsonHandler.getFromStringAsTypeRefence(resultValue, new TypeReference<Set<String>>() {});

                    Set<String> expectedrray =
                        JsonHandler.getFromStringAsTypeRefence(resultExpected, new TypeReference<Set<String>>() {});
                    assertThat(resultArray).isEqualTo(expectedrray);
                } else {
                    Set<Set<String>> resultArray =
                        JsonHandler.getFromStringAsTypeRefence(resultValue, new TypeReference<Set<Set<String>>>() {});

                    Set<Set<String>> expectedrray =
                        JsonHandler
                            .getFromStringAsTypeRefence(resultExpected, new TypeReference<Set<Set<String>>>() {});

                    assertThat(expectedrray).isEqualTo(resultArray);
                }
            }
        }
    }


    /**
     * Upload contract with noeud
     *
     * @param title
     * @throws IOException
     */
    @When("^j'importe le contrat d'entrée avec le noeud de rattachement dont le titre est (.*)")
    public void upload_contract_ingest_with_noeud(String title) throws Throwable {
        try {
            String unitGuid = replaceTitleByGUID(title);
            String newContract = CONTRACT_WITH_LINK.replace(UNIT_GUID, unitGuid);
            JsonNode node = JsonHandler.getFromString(newContract);
            world.getAdminClient().createIngestContracts(
                new VitamContext(world.getTenantId()).setApplicationSessionId(world.getApplicationSessionId()),
                new ByteArrayInputStream(newContract.getBytes()));
        } catch (AccessExternalClientException | IllegalStateException | InvalidParseOperationException e) {
            // Do Nothing
            LOGGER.warn("Contrat d'entrée est déjà importé");
        }
    }

    /**
     * @param lastJsonNode
     * @param raw
     * @return
     * @throws Throwable
     */
    private String getResultValue(JsonNode lastJsonNode, String raw) throws Throwable {
        String rawCopy = transformToGuid(raw);
        String[] paths = rawCopy.split("\\.");
        for (String path : paths) {
            if (lastJsonNode.isArray()) {
                try {
                    int value = Integer.valueOf(path);
                    lastJsonNode = lastJsonNode.get(value);
                } catch (NumberFormatException e) {
                    LOGGER.warn(e);
                }
            } else {
                lastJsonNode = lastJsonNode.get(path);
            }
        }

        return JsonHandler.unprettyPrint(lastJsonNode);
    }

    private String transformToGuid(String raw) throws Throwable {

        Matcher matcher = Pattern.compile(REGEX)
            .matcher(raw);
        String rawCopy = new String(raw);
        Map<String, String> unitToGuid = new HashMap<>();
        while (matcher.find()) {
            String unit = matcher.group(1);
            String unitTitle = unit.substring(2, unit.length() - 2).replace(UNIT_PREFIX, "").trim();
            String unitGuid = "";
            if (unitToGuid.get(unitTitle) != null) {
                unitGuid = unitToGuid.get(unitTitle);
            } else {
                unitGuid = replaceTitleByGUID(unitTitle);
                unitToGuid.put(unitTitle, unitGuid);
            }
            rawCopy = rawCopy.replace(unit, unitGuid);
        }
        return rawCopy;
    }


    private String replaceTitleByGUID(String auTitle) throws Throwable {
        String auId = "";
        SelectMultiQuery searchQuery = new SelectMultiQuery();
        searchQuery.addQueries(
            and().add(eq(TITLE, auTitle)).add(in(VitamFieldsHelper.operations(), world.getOperationId()))
                .setDepthLimit(20));
        RequestResponse requestResponse =
            world.getAccessClient().selectUnits(
                new VitamContext(world.getTenantId()).setAccessContract(world.getContractId())
                    .setApplicationSessionId(world.getApplicationSessionId()),
                searchQuery.getFinalSelect());
        if (requestResponse.isOk()) {
            RequestResponseOK<JsonNode> requestResponseOK = (RequestResponseOK<JsonNode>) requestResponse;
            if (requestResponseOK.getHits().getTotal() == 0) {
                Fail.fail("Archive Unit not found : title = " + auTitle);
            }
            JsonNode firstJsonNode = Iterables.get(requestResponseOK.getResults(), 0);
            if (firstJsonNode.get(VitamFieldsHelper.id()) != null) {
                auId = firstJsonNode.get(VitamFieldsHelper.id()).textValue();
            }
        } else {
            VitamError vitamError = (VitamError) requestResponse;
            Fail.fail("request selectUnit return an error: " + vitamError.getCode());
        }
        return auId;
    }

    /**
     * Get a specific field value from a result identified by its index
     *
     * @param field field name
     * @param numResult number of the result in results
     * @return value if found or null
     * @throws Throwable
     */
    private String getValueFromResult(String field, int numResult) throws Throwable {
        if (results.size() < numResult) {
            Fail.fail("numResult " + numResult + " > result size " + results.size());
        }
        JsonNode result = Iterables.get(results, numResult);
        result = result.get(field);
        return result.textValue();
    }

    /**
     * check if the number of result is OK
     *
     * @param numberOfResult number of result.
     * @throws Throwable
     */
    @Then("^le nombre de résultat est (\\d+)$")
    public void number_of_result_are(int numberOfResult) throws Throwable {
        assertThat(results).hasSize(numberOfResult);
    }

    /**
     * check if the status of the select result is unauthorized
     *
     * @param status
     * @throws Throwable
     */
    @Then("^le statut de select résultat est (.*)$")
    public void the_status_of_the_select_result(String status) throws Throwable {
        JsonNode queryJSON = JsonHandler.getFromString(query);

        requestResponse = world.getAccessClient().selectUnits(
            new VitamContext(world.getTenantId()).setAccessContract(world.getContractId())
                .setApplicationSessionId(world.getApplicationSessionId()),
            queryJSON);

        the_status_of_the_request(status);
    }


    /**
     * check if the status of the select result is unauthorized
     *
     * @param status
     * @throws Throwable
     */
    @Then("^le statut de la requete est (.*)$")
    public void the_status_of_the_request(String status) throws Throwable {
        Status expectedStatus = Status.fromStatusCode(requestResponse.getHttpCode());

        assertThat(expectedStatus).as("Invalid status %d", requestResponse.getHttpCode()).isNotNull();
        assertThat(expectedStatus.getReasonPhrase()).isEqualTo(status);
    }

    /**
     * check if the status of the update result is unauthorized
     *
     * @param status
     * @throws Throwable
     */
    @Then("^le statut de update résultat est (.*)$")
    public void the_status_of_the_update_result(String status) throws Throwable {
        JsonNode queryJSON = JsonHandler.getFromString(query);
        String s = null;
        // get id of last result
        String unitId = getValueFromResult("#id", 0);
        RequestResponse<JsonNode> requestResponse = world.getAccessClient().updateUnitbyId(
            new VitamContext(world.getTenantId()).setAccessContract(world.getContractId())
                .setApplicationSessionId(world.getApplicationSessionId()),
            queryJSON, unitId);
        assertThat(requestResponse.isOk()).isFalse();
        final VitamError vitamError = (VitamError) requestResponse;
        assertThat(Response.Status.valueOf(status.toUpperCase()).getStatusCode()).isEqualTo(vitamError.getHttpCode());
    }

    /**
     * define a query from a file to reuse it after
     *
     * @param queryFilename name of the file containing the query
     * @throws Throwable
     */
    @When("^j'utilise le fichier de requête suivant (.*)$")
    public void i_use_the_following_file_query(String queryFilename) throws Throwable {
        Path queryFile = Paths.get(world.getBaseDirectory(), queryFilename);
        this.query = FileUtil.readFile(queryFile.toFile());
        if (world.getOperationId() != null) {
            this.query = this.query.replace(OPERATION_ID, world.getOperationId());
        }
    }

    /**
     * replace in the loaded query the given parameter by the given value
     * 
     * @param parameter parameter name in the query
     * @param value the valeur to replace the parameter
     * @throws Throwable
     */
    @When("^j'utilise dans la requête le paramètre (.*) avec la valeur (.*)$")
    public void i_use_the_following_parameter_query_with_values(String parameter, String value) throws Throwable {
        this.query = this.query.replace(parameter, value);
    }

    /**
     * replace in the loaded query the string {{guid}} by the guid of the first unit found for given title
     * 
     * @param title title of the unit
     * @throws Throwable
     */
    @When("^j'utilise dans la requête le GUID de l'unité archivistique pour le titre (.*)$")
    public void i_use_the_following_unit_guid_for_title(String title) throws Throwable {
        String unitGuid = replaceTitleByGUID(title);
        this.query = this.query.replace("{{guid}}", unitGuid);
    }

    /**
     * define a query to reuse it after
     *
     * @param query
     * @throws Throwable
     */
    @When("^j'utilise la requête suivante$")
    public void i_use_the_following_query(String query) throws Throwable {
        this.query = query;
        if (world.getOperationId() != null) {
            this.query = this.query.replace(OPERATION_ID, world.getOperationId());
        }
    }

    /**
     * define a query to reuse it after
     *
     * @param query
     * @throws Throwable
     */
    @When("^j'utilise la requête suivante avec l'identifient sauvégardé$")
    public void i_use_the_following_query_with_saved(String query) throws Throwable {
        this.query = query;
        if (world.getOperationId() != null) {
            this.query = this.query.replace(OPERATION_ID, world.getOperationId());
        }
    }

    /**
     * search an archive unit according to the query define before
     *
     * @throws Throwable
     */
    @When("^je recherche les unités archivistiques$")
    public void search_archive_unit() throws Throwable {
        JsonNode queryJSON = JsonHandler.getFromString(query);
        RequestResponse<JsonNode> requestResponse = world.getAccessClient().selectUnits(
            new VitamContext(world.getTenantId()).setAccessContract(world.getContractId())
                .setApplicationSessionId(world.getApplicationSessionId()),
            queryJSON);
        if (requestResponse.isOk()) {
            RequestResponseOK<JsonNode> requestResponseOK = (RequestResponseOK<JsonNode>) requestResponse;
            results = requestResponseOK.getResults();
        } else {
            VitamError vitamError = (VitamError) requestResponse;
            Fail.fail("request selectUnit return an error: " + vitamError.getCode());
        }
    }

    @When("^je recherche les unités archivistiques pour trouver l'unite (.*)$")
    public void search_archive_unit(String originatingSystemId) throws Throwable {


        this.query = this.query.replace("Originating_System_Id", originatingSystemId);

        JsonNode queryJSON = JsonHandler.getFromString(query);

        RequestResponse<JsonNode> requestResponse = world.getAccessClient().selectUnits(
            new VitamContext(world.getTenantId()).setAccessContract(world.getContractId())
                .setApplicationSessionId(world.getApplicationSessionId()),
            queryJSON);
        if (requestResponse.isOk()) {
            RequestResponseOK<JsonNode> requestResponseOK = (RequestResponseOK<JsonNode>) requestResponse;
            results = requestResponseOK.getResults();
        } else {
            VitamError vitamError = (VitamError) requestResponse;
            Fail.fail("request selectUnit return an error: " + vitamError.getCode());
        }
    }



    /**
     * search an archive unit according to the query define before
     *
     * @throws Throwable
     */
    @When("^je recherche une unité archivistique et je recupère son id$")
    public void search_one_archive_unit() throws Throwable {
        JsonNode queryJSON = JsonHandler.getFromString(query);
        RequestResponse<JsonNode> requestResponse = world.getAccessClient().selectUnits(
            new VitamContext(world.getTenantId()).setAccessContract(world.getContractId())
                .setApplicationSessionId(world.getApplicationSessionId()),
            queryJSON);
        if (requestResponse.isOk()) {
            RequestResponseOK<JsonNode> requestResponseOK = (RequestResponseOK<JsonNode>) requestResponse;
            world.setUnitId(requestResponseOK.getResults().get(0).get("#id").asText());
        } else {
            VitamError vitamError = (VitamError) requestResponse;
            Fail.fail("request selectUnit return an error: " + vitamError.getCode());
        }
    }

    /**
     * update an archive unit according to the query define before
     *
     * @throws Throwable
     */
    @When("^je modifie les unités archivistiques$")
    public void update_archive_unit() throws Throwable {
        JsonNode queryJSON = JsonHandler.getFromString(query);
        // get id of last result
        String unitId = getValueFromResult("#id", 0);
        savedUnit = unitId;
        RequestResponse<JsonNode> requestResponse =
            world.getAccessClient().updateUnitbyId(
                new VitamContext(world.getTenantId()).setAccessContract(world.getContractId())
                    .setApplicationSessionId(world.getApplicationSessionId()),
                queryJSON, unitId);
        if (requestResponse.isOk()) {
            RequestResponseOK<JsonNode> requestResponseOK = (RequestResponseOK<JsonNode>) requestResponse;
            results = requestResponseOK.getResults();
        } else {
            VitamError vitamError = (VitamError) requestResponse;
            Fail.fail("request selectUnit return an error: " + vitamError.getCode());
        }
    }


    /**
     * update an archive unit according to the query define before
     *
     * @throws Throwable
     */
    @When("^je modifie l'unité archivistique avec la requete$")
    public void update_archive_unit_with_query(String query) throws Throwable {
        JsonNode queryJSON = JsonHandler.getFromString(query);
        // get id of last result
        requestResponse =
            world.getAccessClient()
                .updateUnitbyId(new VitamContext(world.getTenantId()).setAccessContract(world.getContractId()),
                    queryJSON, savedUnit);

    }

    /**
     * Search an archive unit and retrieve object groups according to the query define before. Step 1 : request search
     * unit with #object in projection. Step 2 : on each unit search object group.
     *
     * @throws Throwable
     */
    @When("^je recherche les groupes d'objets des unités archivistiques$")
    public void search_archive_unit_object_group() throws Throwable {
        ObjectNode queryJSON = (ObjectNode) JsonHandler.getFromString(query);
        // update projection to unsure object id is in projection
        if (queryJSON.get("$projection") == null) {
            queryJSON.set("$projection", JsonHandler.createObjectNode());
        }
        if (queryJSON.get("$projection").get("$fields") == null) {
            ((ObjectNode) queryJSON.get("$projection")).set("$fields", JsonHandler.createObjectNode());
        }
        if (queryJSON.get("$projection").get("$fields").get("#object") == null) {
            ((ObjectNode) queryJSON.get("$projection").get("$fields")).put("#object", 1);
        }

        // Search units
        RequestResponse<JsonNode> requestResponseUnit = world.getAccessClient().selectUnits(
            new VitamContext(world.getTenantId()).setAccessContract(world.getContractId())
                .setApplicationSessionId(world.getApplicationSessionId()),
            queryJSON);
        if (requestResponseUnit.isOk()) {
            RequestResponseOK<JsonNode> responseOK = (RequestResponseOK<JsonNode>) requestResponseUnit;
            List<JsonNode> unitResults = responseOK.getResults();
            RequestResponseOK<JsonNode> objectGroupsResponseOK = new RequestResponseOK<>();
            for (JsonNode unitResult : unitResults) {
                // search object group on unit
                RequestResponse responseObjectGroup =
                    world.getAccessClient().selectObjectMetadatasByUnitId(
                        new VitamContext(world.getTenantId()).setAccessContract(world.getContractId())
                            .setApplicationSessionId(world.getApplicationSessionId()),
                        new SelectMultiQuery().getFinalSelectById(),
                        unitResult.get("#id").asText());
                if (responseObjectGroup.isOk()) {
                    List<JsonNode> objectGroupResults =
                        ((RequestResponseOK<JsonNode>) responseObjectGroup).getResults();
                    if (objectGroupResults != null && !objectGroupResults.isEmpty()) {
                        objectGroupsResponseOK.addAllResults(objectGroupResults);
                    }
                } else {
                    VitamError vitamError = (VitamError) responseObjectGroup;
                    Fail.fail("request selectObject return an error: " + vitamError.getCode());
                }
            }
            results = objectGroupsResponseOK.getResults();
        } else {
            VitamError vitamError = (VitamError) requestResponseUnit;
            Fail.fail("request selectUnit for GOT return an error: " + vitamError.getCode());
        }
    }

    /**
     * Search an archive unit and retrieve object groups according to the query define before. Search object group with
     * archive unit Id
     *
     * @throws Throwable
     */
    @When("^je recherche les groupes d'objets de l'unité archivistique dont le titre est (.*)$")
    public void search_archive_unit_object_group(String title) throws Throwable {
        String unitId = replaceTitleByGUID(title);
        RequestResponse responseObjectGroup =
            world.getAccessClient().selectObjectMetadatasByUnitId(
                new VitamContext(world.getTenantId()).setAccessContract(world.getContractId())
                    .setApplicationSessionId(world.getApplicationSessionId()),
                new SelectMultiQuery().getFinalSelectById(), unitId);
        if (responseObjectGroup.isOk()) {
            results = ((RequestResponseOK<JsonNode>) responseObjectGroup).getResults();
        } else {
            VitamError vitamError = (VitamError) responseObjectGroup;
            Fail.fail("request selectObject return an error: " + vitamError.getCode());
        }
    }

    /**
     * search logbook operations according to the query define before
     *
     * @throws Throwable
     */
    @When("^je recherche les journaux d'opération$")
    public void search_logbook_operation() throws Throwable {
        JsonNode queryJSON = JsonHandler.getFromString(query);
        RequestResponse<LogbookOperation> requestResponse =
            world.getAccessClient().selectOperations(
                new VitamContext(world.getTenantId()).setAccessContract(world.getContractId())
                    .setApplicationSessionId(world.getApplicationSessionId()),
                queryJSON);
        if (requestResponse.isOk()) {
            RequestResponseOK<LogbookOperation> requestResponseOK =
                (RequestResponseOK<LogbookOperation>) requestResponse;
            results = requestResponseOK.getResultsAsJsonNodes();
        } else {
            VitamError vitamError = (VitamError) requestResponse;
            Fail.fail("request selectOperation return an error: " + vitamError.getCode());
        }
    }

    /**
     * search an accession register detail according to the originating agency and the query define before
     *
     * @param originatingAgency originating agency
     * @throws Throwable
     */
    @When("^je recherche les détails des registres de fond pour le service producteur (.*)$")
    public void search_accession_regiter_detail(String originatingAgency) throws Throwable {
        JsonNode queryJSON = JsonHandler.getFromString(query);
        RequestResponse<JsonNode> requestResponse =
            world.getAdminClient().getAccessionRegisterDetail(
                new VitamContext(world.getTenantId()).setAccessContract(world.getContractId())
                    .setApplicationSessionId(world.getApplicationSessionId()),
                originatingAgency, queryJSON);
        if (requestResponse.isOk()) {
            RequestResponseOK<JsonNode> requestResponseOK = (RequestResponseOK<JsonNode>) requestResponse;
            results = requestResponseOK.getResults();
        } else {
            VitamError vitamError = (VitamError) requestResponse;
            Fail.fail("request selectOperation return an error: " + vitamError.getCode());
        }
    }

    /**
     * Import or Check an admin referential file
     *
     * @param action the action we want to execute : "vérifie" for check / "importe" for import
     * @param filename name of the file to import or check
     * @param collection name of the collection
     * @throws Throwable
     */
    @When("^(?:je |j')(.*) le fichier nommé (.*) (?:pour|dans) le référentiel (.*)$")
    public void i_import_or_check_the_file_for_the_admin_collection(String action, String filename, String collection)
        throws Throwable {
        Path file = Paths.get(world.getBaseDirectory(), filename);
        try (InputStream inputStream = Files.newInputStream(file, StandardOpenOption.READ)) {
            AdminCollections adminCollection = AdminCollections.valueOf(collection);
            int status = 0;
            results = new ArrayList<>();
            if ("vérifie".equals(action)) {
                // status =
                if (AdminCollections.FORMATS.equals(adminCollection)) {
                    world.getAdminClient().checkFormats(
                        new VitamContext(world.getTenantId()).setApplicationSessionId(world.getApplicationSessionId()),
                        inputStream);
                } else if (AdminCollections.RULES.equals(adminCollection)) {
                    world.getAdminClient().checkRules(
                        new VitamContext(world.getTenantId()).setApplicationSessionId(world.getApplicationSessionId()),
                        inputStream);
                }
            } else if ("importe".equals(action)) {
                if (AdminCollections.FORMATS.equals(adminCollection)) {
                    RequestResponse response =
                        world.getAdminClient().createFormats(
                            new VitamContext(world.getTenantId())
                                .setApplicationSessionId(world.getApplicationSessionId()),
                            inputStream, filename);
                    status = response.getHttpCode();
                } else if (AdminCollections.RULES.equals(adminCollection)) {
                    RequestResponse response =
                        world.getAdminClient().createRules(
                            new VitamContext(world.getTenantId())
                                .setApplicationSessionId(world.getApplicationSessionId()),
                            inputStream, filename);
                    status = response.getHttpCode();
                }
            }
            if (status != 0) {
                results.add(JsonHandler.createObjectNode().put("Code", String.valueOf(status)));
            }
        } catch (Exception e) {
            LOGGER.warn("Referentiels collection already imported");
        }
    }

    /**
     * Search in admin collection according to the query define before
     *
     * @param collection name of the collection
     * @throws Throwable
     */
    @When("^je recherche les données dans le référentiel (.*)$")
    public void search_in_admin_collection(String collection) throws Throwable {
        JsonNode queryJSON = JsonHandler.getFromString(query);
        AdminCollections adminCollection = AdminCollections.valueOf(collection);
        RequestResponse requestResponse = null;
        switch (adminCollection) {
            case FORMATS:
                requestResponse = world.getAdminClient().findFormats(
                    new VitamContext(world.getTenantId()).setAccessContract(null)
                        .setApplicationSessionId(world.getApplicationSessionId()),
                    queryJSON);
                break;
            case RULES:
                requestResponse = world.getAdminClient().findRules(
                    new VitamContext(world.getTenantId()).setAccessContract(null)
                        .setApplicationSessionId(world.getApplicationSessionId()),
                    queryJSON);
                break;
            case ACCESS_CONTRACTS:
                requestResponse = world.getAdminClient().findAccessContracts(
                    new VitamContext(world.getTenantId()).setAccessContract(null)
                        .setApplicationSessionId(world.getApplicationSessionId()),
                    queryJSON);
                break;
            case INGEST_CONTRACTS:
                requestResponse = world.getAdminClient().findIngestContracts(
                    new VitamContext(world.getTenantId()).setAccessContract(null)
                        .setApplicationSessionId(world.getApplicationSessionId()),
                    queryJSON);
                break;
            case CONTEXTS:
                requestResponse = world.getAdminClient().findContexts(
                    new VitamContext(world.getTenantId()).setAccessContract(null)
                        .setApplicationSessionId(world.getApplicationSessionId()),
                    queryJSON);
                break;
            case PROFILE:
                requestResponse = world.getAdminClient().findProfiles(
                    new VitamContext(world.getTenantId()).setAccessContract(null)
                        .setApplicationSessionId(world.getApplicationSessionId()),
                    queryJSON);
                break;
            case ACCESSION_REGISTERS:
                requestResponse = world.getAdminClient().findAccessionRegister(
                    new VitamContext(world.getTenantId()).setAccessContract(null)
                        .setApplicationSessionId(world.getApplicationSessionId()),
                    queryJSON);
                break;
            default:
                break;
        }

        if (requestResponse != null && requestResponse.isOk()) {
            results = ((RequestResponseOK) requestResponse).getResultsAsJsonNodes();
        } else if (requestResponse != null) {
            VitamError vitamError = (VitamError) requestResponse;
            Fail.fail("request findDocuments return an error: " + vitamError.getCode());
        } else {
            Fail.fail("Collection not found " + collection);
        }
    }

    /**
     * Search logbook of unit with unit title
     *
     * @param title of unit
     * @throws Throwable
     */
    @When("^je recherche le JCV de l'unité archivistique dont le titre est (.*)$")
    public void search_LFC_Unit_with_title(String title) throws Throwable {
        String unitId = replaceTitleByGUID(title);
        RequestResponse<LogbookLifecycle> requestResponse =
            world.getAccessClient().selectUnitLifeCycleById(
                new VitamContext(world.getTenantId()).setAccessContract(world.getContractId())
                    .setApplicationSessionId(world.getApplicationSessionId()),
                unitId, new Select().getFinalSelect());
        if (requestResponse.isOk()) {
            RequestResponseOK<LogbookLifecycle> requestResponseOK =
                (RequestResponseOK<LogbookLifecycle>) requestResponse;
            results = requestResponseOK.getResultsAsJsonNodes();
        } else {
            VitamError vitamError = (VitamError) requestResponse;
            Fail.fail("request findDocuments return an error: " + vitamError.getCode());
        }
    }

    /**
     * Search logbook of object group with unit title
     *
     * @param title of unit
     * @throws Throwable
     */
    @When("^je recherche le JCV du groupe d'objet de l'unité archivistique dont le titre est (.*)$")
    public void search_LFC_OG_with_Unit_title(String title) throws Throwable {
        String unitId = replaceTitleByGUID(title);
        RequestResponse<JsonNode> requestResponse =
            world.getAccessClient().selectUnitbyId(
                new VitamContext(world.getTenantId()).setAccessContract(world.getContractId())
                    .setApplicationSessionId(world.getApplicationSessionId()),
                new SelectMultiQuery().getFinalSelectById(), unitId);
        if (requestResponse.isOk()) {
            RequestResponseOK<JsonNode> requestResponseOK = (RequestResponseOK<JsonNode>) requestResponse;
            JsonNode unit = requestResponseOK.getResults().get(0);
            if (unit.get(PROJECTIONARGS.OBJECT.exactToken()).asText().isEmpty()) {
                VitamError vitamError = (VitamError) requestResponse;
                Fail.fail("Unit does not have object");
            }
            RequestResponse<LogbookLifecycle> requestResponseLFC =
                world.getAccessClient().selectObjectGroupLifeCycleById(
                    new VitamContext(world.getTenantId()).setAccessContract(world.getContractId())
                        .setApplicationSessionId(world.getApplicationSessionId()),
                    unit.get(PROJECTIONARGS.OBJECT.exactToken()).asText(), new Select().getFinalSelect());
            if (requestResponseLFC.isOk()) {
                RequestResponseOK<LogbookLifecycle> requestResponseLFCOK =
                    (RequestResponseOK<LogbookLifecycle>) requestResponseLFC;
                results = requestResponseLFCOK.getResultsAsJsonNodes();
            } else {
                VitamError vitamError = (VitamError) requestResponse;
                Fail.fail("request selectObjectGroupLifeCycleById return an error: " + vitamError.getCode());
            }
        } else {
            VitamError vitamError = (VitamError) requestResponse;
            Fail.fail("request selectUnitbyId return an error: " + vitamError.getCode());
        }
    }


    /**
     * check if the status is valid for a list of event type according to logbook lifecycle
     *
     * @param eventNames list of event
     * @param eventStatus status of event
     * @throws LogbookClientException
     * @throws InvalidParseOperationException
     * @throws AccessUnauthorizedException
     */
    @Then("^le[s]? statut[s]? de JCV (?:de l'événement|des événements) (.*) (?:est|sont) (.*)$")
    public void the_LFC_status_are(List<String> eventNames, String eventStatus)
        throws Throwable {
        ArrayNode actual = (ArrayNode) results.get(0).get("events");
        List<JsonNode> list = (List<JsonNode>) JsonHandler.toArrayList(actual);
        try (AutoCloseableSoftAssertions softly = new AutoCloseableSoftAssertions()) {
            for (String eventName : eventNames) {
                List<JsonNode> events =
                    list.stream().filter(event -> eventName.equals(event.get("evType").textValue()))
                        .filter(event -> !event.get("outcome").textValue().equals("STARTED"))
                        .collect(Collectors.toList());

                JsonNode onlyElement = events.get(0);

                String currentStatus = onlyElement.get("outcome").textValue();
                softly.assertThat(currentStatus)
                    .as("event %s has status %s but excepted status is %s.", eventName, currentStatus, eventStatus)
                    .isEqualTo(eventStatus);
            }
        }
    }


    @When("^je modifie le contrat d'accès (.*) avec le fichier de requête suivant (.*)$")
    public void je_modifie_le_contrat_d_accès(String name, String queryFilename) throws Throwable {
        Path queryFile = Paths.get(world.getBaseDirectory(), queryFilename);
        this.query = FileUtil.readFile(queryFile.toFile());
        if (world.getOperationId() != null) {
            this.query = this.query.replace(OPERATION_ID, world.getOperationId());
        }

        JsonNode queryDsl = JsonHandler.getFromString(query);
        world.getAdminClient().updateAccessContract(
            new VitamContext(world.getTenantId()).setApplicationSessionId(world.getApplicationSessionId()),
            get_contract_id_by_name(name), queryDsl);
    }

    private String get_contract_id_by_name(String name)
        throws AccessExternalClientNotFoundException, AccessExternalClientException, InvalidParseOperationException,
        VitamClientException {

        String QUERY = "{\"$query\":{\"$and\":[{\"$eq\":{\"Name\":\"" + name +
            "\"}}]},\"$filter\":{},\"$projection\":{}}";
        JsonNode queryDsl = JsonHandler.getFromString(QUERY);

        RequestResponse<AccessContractModel> requestResponse =
            world.getAdminClient().findAccessContracts(
                new VitamContext(world.getTenantId()).setAccessContract(null)
                    .setApplicationSessionId(world.getApplicationSessionId()),
                queryDsl);
        if (requestResponse.isOk()) {
            return ((RequestResponseOK<AccessContractModel>) requestResponse).getFirstResult().getId();
        }
        throw new VitamClientException("Access contract was found");
    }

    @When("^je veux faire l'audit des objets du service producteur \"([^\"]*)\"$")
    public void je_lance_l_audit_en_service_producteur(String originatingAgnecy) throws Throwable {
        String QUERY = "{auditType:\"originatingAgency\",objectId:\"" + originatingAgnecy +
            "\"}";
        JsonNode auditOption = JsonHandler.getFromString(QUERY);

        assertThat(world.getAdminClient().launchAudit(
            new VitamContext(world.getTenantId()).setAccessContract(world.getContractId())
                .setApplicationSessionId(world.getApplicationSessionId()),
            auditOption).isOk()).isTrue();
    }

    @When("^je veux faire l'audit des objets de tenant (\\d+)$")
    public void je_veux_faire_l_audit_des_objets_de_tenant(int tenant) throws Throwable {
        String QUERY =
            "{auditType:\"tenant\",objectId:\"" + tenant + "\"}";
        JsonNode auditOption = JsonHandler.getFromString(QUERY);

        assertThat(world.getAdminClient().launchAudit(
            new VitamContext(world.getTenantId()).setAccessContract(world.getContractId())
                .setApplicationSessionId(world.getApplicationSessionId()),
            auditOption).isOk()).isTrue();
    }

    @Then("^le réultat de l'audit est succès$")
    public void le_réultat_de_l_audit_est_succès() throws Throwable {
        assertThat(auditStatus.getStatusCode()).isEqualTo(202);
    }

}
