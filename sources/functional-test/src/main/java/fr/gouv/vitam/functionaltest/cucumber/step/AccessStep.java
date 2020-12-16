/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
import com.google.common.collect.Iterables;
import cucumber.api.DataTable;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import fr.gouv.vitam.access.external.api.AdminCollections;
import fr.gouv.vitam.access.external.client.VitamPoolingClient;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientException;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientNotFoundException;
import fr.gouv.vitam.common.FileUtil;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.FacetBucket;
import fr.gouv.vitam.common.model.FacetResult;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.model.administration.ContextModel;
import fr.gouv.vitam.common.model.administration.PermissionModel;
import fr.gouv.vitam.common.model.elimination.EliminationRequestBody;
import fr.gouv.vitam.common.model.logbook.LogbookOperation;
import fr.gouv.vitam.common.thread.VitamThreadFactory;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.common.utils.JsonSorter;
import net.javacrumbs.jsonunit.JsonAssert;
import net.javacrumbs.jsonunit.core.Option;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Fail;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static fr.gouv.vitam.access.external.api.AdminCollections.AGENCIES;
import static fr.gouv.vitam.access.external.api.AdminCollections.FORMATS;
import static fr.gouv.vitam.access.external.api.AdminCollections.RULES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * step defining access glue
 */
public class AccessStep {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AccessStep.class);
    public static final String CONTEXT_IDENTIFIER = "CT-000001";
    public static final String INGEST_CONTRACT_IDENTIFIER = "contrat_de_rattachement_TNR";
    private static final String INHERITED_RULES = "InheritedRules";

    private static final String UNIT_GUID = "UNIT_GUID";
    private static String CONTRACT_WITH_LINK = "[{" +
        "\"Identifier\":\"contrat_de_rattachement_TNR\"," +
        "\"Name\":\"contrat_de_rattachement_TNR\"," +
        "\"Description\":\"Rattachant les SIP à une AU\"," +
        "\"Status\" : \"ACTIVE\"," +
        "\"LastUpdate\":\"10/12/2016\"," +
        "\"CreationDate\":\"10/12/2016\"," +
        "\"ActivationDate\":\"10/12/2016\"," +
        "\"DeactivationDate\":\"10/12/2016\"," +
        "\"LinkParentId\": \"" + UNIT_GUID + "\"}]";

    private static final String OPERATION_ID = "Operation-Id";
    private static final String NAMED_OPERATION_ID_PREFIX = "Named-Operation-Id<";
    private static final String NAMED_OPERATION_ID_SUFFIX = ">";
    private static final String ELIMINATION_OPERATION_ID = "Elimination-Operation-Id";

    private static final String UNIT_PREFIX = "unit:";


    private static final String REGEX = "(\\{\\{(.*?)\\}\\})";

    private JsonNode selectedInheritedCategoryResult;

    private List<FacetResult> facetResults;

    private World world;

    private Status auditStatus;
    private static String savedUnit;
    private RequestResponse requestResponse;

    public AccessStep(World world) {
        this.world = world;
    }

    /**
     * Check facet bucket value count
     *
     * @param facetName facet name
     * @param count     bucket count
     * @param value     bucket value
     * @throws Throwable when not valid
     */
    @Then("^le résultat pour la facet (.*) contient (\\d+) valeurs (.*)$")
    public void facetmetadata_contains_values_count(String facetName, int count, String value) throws Throwable {
        Optional<FacetResult> facetResult =
            facetResults.stream().filter(item -> item.getName().equals(facetName)).findFirst();
        assertThat(facetResult).as("facetResult with name " + facetName + " was not found").isPresent();
        Optional<FacetBucket> facetBucket =
            facetResult.get().getBuckets().stream().filter(item -> item.getValue().equals(value)).findFirst();
        assertThat(facetBucket).as("facetResult with name " + facetName + " does not contains value " + value)
            .isPresent();
        assertThat(facetBucket.get().getCount()).isEqualTo(count);
    }

    /**
     * Check facet does not contains bucket for value
     *
     * @param facetName facet name
     * @param value     value
     * @throws Throwable when not valid
     */
    @Then("^le résultat pour la facet (.*) ne contient pas la valeur (.*)$")
    public void facetmetadata_does_not_contains_value(String facetName, String value) throws Throwable {
        Optional<FacetResult> facetResult =
            facetResults.stream().filter(item -> item.getName().equals(facetName)).findFirst();
        assertThat(facetResult).as("facetResult with name " + facetName + " was not found").isPresent();
        Optional<FacetBucket> facetBucket =
            facetResult.get().getBuckets().stream().filter(item -> item.getValue().equals(value)).findFirst();
        assertThat(facetBucket).as("facetResult with name " + facetName + " contains value " + value).isNotPresent();
    }

    /**
     * check if the metadata are valid.
     *
     * @param dataTable dataTable
     * @throws Throwable
     */
    @Then("^les metadonnées sont$")
    public void metadata_are(DataTable dataTable) throws Throwable {
        metadata_are_for_particular_result(0, dataTable);

    }

    /**
     * check if the metadata are valid.
     *
     * @param dataTable    dataTable
     * @param resultNumber resultNumber
     * @throws Throwable
     */
    @Then("^les metadonnées pour le résultat (\\d+)$")
    public void metadata_are_for_particular_result(int resultNumber, DataTable dataTable) throws Throwable {
        // Transform results
        List<JsonNode> transformedResults = new ArrayList<>();
        for (JsonNode result : world.getResults()) {
            String resultAsString = JsonHandler.unprettyPrint(result);
            String resultAsStringTransformed = transformUnitTitleToGuid(resultAsString);
            transformedResults.add(JsonHandler.getFromString(resultAsStringTransformed));
        }
        DataTable transformedDataTable = getTransformedDataTable(dataTable);

        world.getAccessService().checkResultsForParticularData(transformedResults, resultNumber, transformedDataTable);
    }

    private DataTable getTransformedDataTable(DataTable dataTable) throws Throwable {
        // Transform validation data
        List<List<String>> modifiedRaws = new ArrayList<>();
        List<List<String>> raws = dataTable.raw();
        for (List<String> raw : raws) {
            List<String> modifiedSubRaws = new ArrayList<>();
            for (String subRaw : raw) {
                modifiedSubRaws.add(transformUnitTitleToGuid(subRaw));
            }
            modifiedRaws.add(modifiedSubRaws);
        }
        List<String> topCells = modifiedRaws.isEmpty() ? Collections.emptyList() : modifiedRaws.get(0);
        return dataTable.toTable(modifiedRaws, topCells.toArray(new String[0]));
    }

    /**
     * Upload contract with noeud
     *
     * @param title
     * @throws IOException
     */
    @When("^j'importe le contrat d'entrée avec le noeud de rattachement dont le titre est (.*)")
    public void upload_contract_ingest_with_noeud(String title) throws Throwable {

        boolean exists = false;
        try {
            String unitGuid =
                world.getAccessService().findUnitGUIDByTitleAndOperationId(world.getAccessClient(), world.getTenantId(),
                    world.getContractId(), world.getApplicationSessionId(), world.getOperationId(), title);
            String newContract = CONTRACT_WITH_LINK.replace(UNIT_GUID, unitGuid);
            world.getAdminClient().createIngestContracts(
                new VitamContext(world.getTenantId()).setApplicationSessionId(world.getApplicationSessionId()),
                new ByteArrayInputStream(newContract.getBytes()));
        } catch (AccessExternalClientException | IllegalStateException | InvalidParseOperationException e) {
            // Do Nothing
            LOGGER.warn("Contrat d'entrée est déjà importé");
            exists = true;
        }

        // TODO: 3/16/18 Ugly fix
        if (!exists) {
            // update context

            RequestResponse<ContextModel> res = world.getAdminClient()
                .findContextById(new VitamContext(world.getTenantId())
                    .setApplicationSessionId(world.getApplicationSessionId()), CONTEXT_IDENTIFIER);
            assertThat(res.isOk()).isTrue();
            ContextModel contextModel = ((RequestResponseOK<ContextModel>) res).getFirstResult();
            assertThat(contextModel).isNotNull();
            List<PermissionModel> permissions = contextModel.getPermissions();

            assertThat(permissions).isNotEmpty();


            boolean changed = false;
            for (PermissionModel p : permissions) {
                if (p.getTenant() != world.getTenantId()) {
                    continue;
                }

                if (null == p.getIngestContract()) {
                    p.setIngestContract(new HashSet<>());
                }
                changed = p.getIngestContract().add(INGEST_CONTRACT_IDENTIFIER) || changed;
            }

            if (changed) {
                ContractsStep.updateContext(world.getAdminClient(), world.getApplicationSessionId(), CONTEXT_IDENTIFIER,
                    permissions, true);
            }

        }
    }

    private String transformUnitTitleToGuid(String result) throws Throwable {

        Matcher matcher = Pattern.compile(REGEX)
            .matcher(result);
        String resultCopy = result;
        Map<String, String> unitToGuid = new HashMap<>();
        while (matcher.find()) {
            String unit = matcher.group(1);
            String unitTitle = unit.substring(2, unit.length() - 2).replace(UNIT_PREFIX, "").trim();
            String unitGuid;
            if (unitToGuid.get(unitTitle) != null) {
                unitGuid = unitToGuid.get(unitTitle);
            } else {
                unitGuid = getUnitGuidByTitle(unitTitle);
                unitToGuid.put(unitTitle, unitGuid);
            }
            resultCopy = resultCopy.replace(unit, unitGuid);
        }
        return resultCopy;
    }

    private String transformLoadedUnitTitleToGuid(String result) throws Throwable {

        Matcher matcher = Pattern.compile(REGEX)
            .matcher(result);
        String resultCopy = result;
        Map<String, String> unitToGuid = new HashMap<>();
        while (matcher.find()) {
            String unit = matcher.group(1);
            String unitTitle = unit.substring(2, unit.length() - 2).replace(UNIT_PREFIX, "").trim();
            String unitGuid;
            if (unitToGuid.get(unitTitle) != null) {
                unitGuid = unitToGuid.get(unitTitle);
            } else {
                unitGuid = selectLoadedUnitGuidByTitle(unitTitle);
                unitToGuid.put(unitTitle, unitGuid);
            }
            resultCopy = resultCopy.replace(unit, unitGuid);
        }
        return resultCopy;
    }

    private String getUnitGuidByTitle(String unitTitle) throws InvalidCreateOperationException, VitamClientException {
        String unitGuid;
        unitGuid = world.getAccessService().findUnitGUIDByTitleAndOperationId(world.getAccessClient(),
            world.getTenantId(), world.getContractId(), world.getApplicationSessionId(), world.getOperationId(),
            unitTitle);
        return unitGuid;
    }

    private String selectLoadedUnitGuidByTitle(String unitTitle) {
        List<JsonNode> results = world.getResults();

        for (JsonNode result : results) {
            if (result.get("Title").asText().equals(unitTitle)) {
                return result.get(VitamFieldsHelper.id()).asText();
            }
        }
        Fail.fail("No such unit with title '" + unitTitle + "'");
        throw new AssertionError("Never reached");
    }

    /**
     * Get a specific field value from a result identified by its index
     *
     * @param field     field name
     * @param numResult number of the result in results
     * @return value if found or null
     * @throws Throwable
     */
    private String getValueFromResult(String field, int numResult) throws Throwable {

        List<JsonNode> results = world.getResults();
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
        assertThat(world.getResults()).hasSize(numberOfResult);
    }

    /**
     * check if the status of the select result is unauthorized
     *
     * @param status
     * @throws Throwable
     */
    @Then("^le statut de select résultat est (.*)$")
    public void the_status_of_the_select_result(String status) throws Throwable {
        JsonNode queryJSON = JsonHandler.getFromString(world.getQuery());

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
        JsonNode queryJSON = JsonHandler.getFromString(world.getQuery());
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
        String query = FileUtil.readFile(queryFile.toFile());
        query = replaceOperationIds(query);
        world.setQuery(query);
    }

    /**
     * replace in the loaded query the given parameter by the given value
     *
     * @param parameter parameter name in the query
     * @param value     the valeur to replace the parameter
     * @throws Throwable
     */
    @When("^j'utilise dans la requête le paramètre (.*) avec la valeur (.*)$")
    public void i_use_the_following_parameter_query_with_values(String parameter, String value) throws Throwable {
        String query = world.getQuery().replace(parameter, value);
        world.setQuery(query);
    }

    /**
     * replace in the loaded query the given parameter by an id previously retrieved
     *
     * @param parameter parameter name in the query
     * @throws Throwable
     */
    @When("^j'utilise dans la requête le paramètre (.*) avec l'id$")
    public void i_use_the_following_parameter_query_with_an_id(String parameter) throws Throwable {
        String query = world.getQuery().replace(parameter, world.getUnitId());
        world.setQuery(query);
    }


    /**
     * replace in the loaded query the string {{guid}} by the guid of the first unit found for given title
     *
     * @param title title of the unit
     * @throws Throwable
     */
    @When("^j'utilise dans la requête le GUID de l'unité archivistique pour le titre (.*)$")
    public void i_use_the_following_unit_guid_for_title(String title) throws Throwable {
        String unitGuid = world.getAccessService().findUnitGUIDByTitleAndOperationId(world.getAccessClient(),
            world.getTenantId(), world.getContractId(), world.getApplicationSessionId(), world.getOperationId(), title);
        String query = world.getQuery().replace("{{guid}}", unitGuid);
        world.setQuery(query);
    }

    /**
     * define a query to reuse it after
     *
     * @param query
     * @throws Throwable
     */
    @When("^j'utilise la requête suivante$")
    public void i_use_the_following_query(String query) throws Throwable {
        String queryTmp = query;
        queryTmp = replaceOperationIds(queryTmp);
        world.setQuery(queryTmp);
    }

    @Then("^je nomme l'identifiant de l'opération (.*)$")
    public void saveOperationId(String operationIdName) throws Throwable {
        world.setNamedOperationId(operationIdName, world.getOperationId());
    }

    private String replaceOperationIds(String query) {
        if (world.getEliminationOperationId() != null) {
            query = query.replace(ELIMINATION_OPERATION_ID, world.getEliminationOperationId());
        }
        while (query.contains(NAMED_OPERATION_ID_PREFIX)) {
            int startIndex = query.indexOf(NAMED_OPERATION_ID_PREFIX);
            int endIndex = query.indexOf(NAMED_OPERATION_ID_SUFFIX, startIndex);
            String name = query.substring(startIndex + +NAMED_OPERATION_ID_PREFIX.length(), endIndex);
            query = query.replace(NAMED_OPERATION_ID_PREFIX + name + NAMED_OPERATION_ID_SUFFIX,
                world.getNamedOperationId(name));
        }
        if (world.getOperationId() != null) {
            query = query.replace(OPERATION_ID, world.getOperationId());
        }
        return query;
    }

    /**
     * search an archive unit according to the query define before
     *
     * @throws Throwable
     */
    @When("^je recherche les unités archivistiques$")
    public void search_archive_unit() throws Throwable {
        JsonNode queryJSON = JsonHandler.getFromString(world.getQuery());
        RequestResponse<JsonNode> requestResponse = world.getAccessClient().selectUnits(
            new VitamContext(world.getTenantId()).setAccessContract(world.getContractId())
                .setApplicationSessionId(world.getApplicationSessionId()),
            queryJSON);
        if (requestResponse.isOk()) {
            RequestResponseOK<JsonNode> requestResponseOK = (RequestResponseOK<JsonNode>) requestResponse;
            world.setResults(requestResponseOK.getResults());
            facetResults = requestResponseOK.getFacetResults();
        } else {
            VitamError vitamError = (VitamError) requestResponse;
            Fail.fail("request selectUnit return an error: " + vitamError.getCode());
        }
    }

    @When("^je recherche les unités archivistiques pour trouver l'unite (.*)$")
    public void search_archive_unit(String originatingSystemId) throws Throwable {


        String queryTmp = world.getQuery().replace("Originating_System_Id", originatingSystemId);
        world.setQuery(queryTmp);

        JsonNode queryJSON = JsonHandler.getFromString(world.getQuery());

        RequestResponse<JsonNode> requestResponse = world.getAccessClient().selectUnits(
            new VitamContext(world.getTenantId()).setAccessContract(world.getContractId())
                .setApplicationSessionId(world.getApplicationSessionId()),
            queryJSON);
        if (requestResponse.isOk()) {
            RequestResponseOK<JsonNode> requestResponseOK = (RequestResponseOK<JsonNode>) requestResponse;
            world.setResults(requestResponseOK.getResults());
            facetResults = requestResponseOK.getFacetResults();
        } else {
            VitamError vitamError = (VitamError) requestResponse;
            Fail.fail("request selectUnit return an error: " + vitamError.getCode());
        }
    }



    /**
     * search an archive unit according to the query define before
     *
     * @throws Throwable throw
     */
    @When("^je recherche une unité archivistique et je recupère son id$")
    public void search_one_archive_unit() throws Throwable {
        JsonNode queryJSON = JsonHandler.getFromString(world.getQuery());
        RequestResponse<JsonNode> requestResponse = world.getAccessClient().selectUnits(
            new VitamContext(world.getTenantId()).setAccessContract(world.getContractId())
                .setApplicationSessionId(world.getApplicationSessionId()),
            queryJSON);
        if (requestResponse.isOk()) {
            RequestResponseOK<JsonNode> requestResponseOK = (RequestResponseOK<JsonNode>) requestResponse;
            assertThat(requestResponseOK.getResults()).isNotEmpty();
            JsonNode unit = requestResponseOK.getResults().iterator().next();
            world.setUnitId(unit.get("#id").asText());
        } else {
            VitamError vitamError = (VitamError) requestResponse;
            Fail.fail("request selectUnit return an error: " + vitamError.getCode());
        }
    }

    /**
     * search an archive unit with inherited rules
     *
     * @throws Throwable
     */
    @When("^je recherche les unités archivistiques avec leurs règles de gestion héritées$")
    public void search_archive_units_with_inherited_rules() throws Throwable {
        JsonNode queryJSON = JsonHandler.getFromString(world.getQuery());
        RequestResponse<JsonNode> requestResponse = world.getAccessClient().selectUnitsWithInheritedRules(
            new VitamContext(world.getTenantId()).setAccessContract(world.getContractId())
                .setApplicationSessionId(world.getApplicationSessionId()),
            queryJSON);
        if (requestResponse.isOk()) {
            RequestResponseOK<JsonNode> requestResponseOK = (RequestResponseOK<JsonNode>) requestResponse;
            world.setResults(requestResponseOK.getResults());
            selectedInheritedCategoryResult = null;
            facetResults = requestResponseOK.getFacetResults();
        } else {
            VitamError vitamError = (VitamError) requestResponse;
            Fail.fail("request selectUnitsWithInheritedRules return an error: " + vitamError.getCode());
        }
    }

    /**
     * Select unit rule category
     */
    @When("^je sélectionne la catégorie (.*) pour l'unité (.*)$")
    public void metadata_are_for_particular_result(String category, String unitTitle)
        throws Throwable {

        JsonNode unitJson = selectUnitInheritedRulesByTitle(unitTitle);

        JsonNode categoryJson = unitJson.get(category);
        if (categoryJson == null) {
            Fail.fail("No such category " + category);
        }

        selectedInheritedCategoryResult = categoryJson;
    }

    @Then("^la catégorie contient (\\d+) règles et (\\d+) propriétés héritées$")
    public void rule_category_rules_and_properties_count_check(int nbRules, int nbProperties) throws Throwable {

        assertThat(selectedInheritedCategoryResult.get("Rules")).hasSize(nbRules);
        assertThat(selectedInheritedCategoryResult.get("Properties")).hasSize(nbProperties);
    }

    /**
     * search an object groups according to the query define before
     *
     * @throws Throwable
     */
    @When("^je recherche les groupes d'objets$")
    public void search_object_groups() throws Throwable {
        JsonNode queryJSON = JsonHandler.getFromString(world.getQuery());
        RequestResponse<JsonNode> requestResponse = world.getAccessClient().selectObjects(
            new VitamContext(world.getTenantId()).setAccessContract(world.getContractId())
                .setApplicationSessionId(world.getApplicationSessionId()),
            queryJSON);
        if (requestResponse.isOk()) {
            RequestResponseOK<JsonNode> requestResponseOK = (RequestResponseOK<JsonNode>) requestResponse;
            world.setResults(requestResponseOK.getResults());
            facetResults = requestResponseOK.getFacetResults();
        } else {
            VitamError vitamError = (VitamError) requestResponse;
            Fail.fail("request selectObjects return an error: " + vitamError.getCode());
        }
    }

    @Then("^la catégorie contient une règle (.*) héritée depuis l'unité (.*) avec pour métadonnées$")
    public void check_rule_metadata(String ruleId, String unitTitle, DataTable dataTable) throws Throwable {

        String unitGuid = getUnitGuidByTitle(unitTitle);

        JsonNode rule = null;
        for (JsonNode foundRule : selectedInheritedCategoryResult.get("Rules")) {
            if (foundRule.get("UnitId").asText().equals(unitGuid) && foundRule.get("Rule").asText().equals(ruleId)) {
                rule = foundRule;
                break;
            }
        }

        if (rule == null) {
            Fail.fail("No such rule " + ruleId + " inherited from unit " + unitTitle);
        }

        DataTable transformedDataTable = getTransformedDataTable(dataTable);
        world.getAccessService().checkResultsForParticularData(rule, transformedDataTable);
    }

    @Then("^la catégorie contient une propriété (.*) héritée depuis l'unité (.*) avec pour métadonnées$")
    public void check_property_metadata(String propertyName, String unitTitle, DataTable dataTable) throws Throwable {

        String unitGuid = getUnitGuidByTitle(unitTitle);

        JsonNode property = null;
        for (JsonNode foundProperty : selectedInheritedCategoryResult.get("Properties")) {
            if (foundProperty.get("UnitId").asText().equals(unitGuid) &&
                foundProperty.get("PropertyName").asText().equals(propertyName)) {
                property = foundProperty;
                break;
            }
        }

        if (property == null) {
            Fail.fail("No such property " + propertyName + " inherited from unit " + unitTitle);
        }

        DataTable transformedDataTable = getTransformedDataTable(dataTable);
        world.getAccessService().checkResultsForParticularData(property, transformedDataTable);
    }

    @Then("^les métadonnées de gestion correspondent au fichier json (.+)$")
    public void json_metadata_are_for_particular_result(String filename) throws Throwable {

        assertThat(world.getResults()).withFailMessage("Expecting single result").hasSize(1);
        JsonNode foundNode = world.getResults().get(0).get(VitamFieldsHelper.management());
        assertThat(foundNode).withFailMessage("No such field found " + filename).isNotNull();

        Path file = Paths.get(world.getBaseDirectory(), filename);
        JsonNode expectedJson;
        try (InputStream inputStream = Files.newInputStream(file, StandardOpenOption.READ)) {
            expectedJson = JsonHandler.getFromInputStream(inputStream);
        }

        // Check json
        assertManagementJsonEquals(foundNode, expectedJson);
    }

    @Then("^les règles hérités de l'unité (.*) correspondent au fichier json (.*)$")
    public void check_unit_inherited_rules_json(String unitTitle, String filename) throws Throwable {

        JsonNode actualJson = selectUnitInheritedRulesByTitle(unitTitle);

        Path file = Paths.get(world.getBaseDirectory(), filename);
        JsonNode expectedJson;
        try (InputStream inputStream = Files.newInputStream(file, StandardOpenOption.READ)) {

            String refJsonWithUnitTitles = IOUtils.toString(inputStream, StandardCharsets.UTF_8);

            String refJsonWithUnitGuids = transformLoadedUnitTitleToGuid(refJsonWithUnitTitles);

            expectedJson = JsonHandler.getFromString(refJsonWithUnitGuids);
        }

        // Check json
        assertManagementJsonEquals(actualJson, expectedJson);
    }

    private void assertManagementJsonEquals(JsonNode actualJson, JsonNode expectedJson) {
        List<String> orderedKeys = Arrays.asList("PropertyName", "Rule", "UnitId");
        JsonSorter.sortJsonEntriesByKeys(actualJson, orderedKeys);
        JsonSorter.sortJsonEntriesByKeys(expectedJson, orderedKeys);

        String actual = JsonHandler.unprettyPrint(actualJson);
        String expected = JsonHandler.unprettyPrint(expectedJson);

        try {
            JsonAssert.assertJsonEquals(expected, actual,
                JsonAssert.when(Option.IGNORING_ARRAY_ORDER));
        } catch (AssertionError e) {
            System.out.println("Actual  : " + actual);
            System.out.println("Expected: " + expected);
            throw e;
        }
    }

    private JsonNode selectUnitInheritedRulesByTitle(String unitTitle) {
        String unitGuid = selectLoadedUnitGuidByTitle(unitTitle);

        JsonNode unitJson = null;
        for (JsonNode result : world.getResults()) {
            if (result.get(VitamFieldsHelper.id()).asText().equals(unitGuid)) {
                unitJson = result;
                break;
            }
        }

        if (unitJson == null) {
            Fail.fail("No such unit with id '" + unitGuid + "' (title: " + unitTitle + ") in result set");
        }

        if (!unitJson.has(INHERITED_RULES)) {
            Fail.fail("Expected inherited rules definition");
        }

        return unitJson.get(INHERITED_RULES);
    }

    /**
     * search an archive unit according to the query define before
     *
     * @throws Throwable
     */
    @When("^je recherche une unité archivistique ayant un groupe d'objets et je recupère son id et son objet$")
    public void search_one_object_group() throws Throwable {
        JsonNode queryJSON = JsonHandler.getFromString(world.getQuery());
        RequestResponse<JsonNode> requestResponse = world.getAccessClient().selectUnits(
            new VitamContext(world.getTenantId()).setAccessContract(world.getContractId())
                .setApplicationSessionId(world.getApplicationSessionId()),
            queryJSON);
        if (requestResponse.isOk()) {
            RequestResponseOK<JsonNode> requestResponseOK = (RequestResponseOK<JsonNode>) requestResponse;
            assertThat(requestResponseOK.getResults()).isNotEmpty();
            world.setResults(requestResponseOK.getResults());
            JsonNode unit = requestResponseOK.getResults().iterator().next();
            world.setUnitId(unit.get("#id").asText());
            world.setObjectGroupId(unit.get("#object").asText());
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
        JsonNode queryJSON = JsonHandler.getFromString(world.getQuery());
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
            world.setResults(requestResponseOK.getResults());
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
        ObjectNode queryJSON = (ObjectNode) JsonHandler.getFromString(world.getQuery());
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
            world.setResults(objectGroupsResponseOK.getResults());

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
        String unitId = world.getAccessService().findUnitGUIDByTitleAndOperationId(world.getAccessClient(),
            world.getTenantId(), world.getContractId(), world.getApplicationSessionId(), world.getOperationId(), title);
        RequestResponse responseObjectGroup =
            world.getAccessClient().selectObjectMetadatasByUnitId(
                new VitamContext(world.getTenantId()).setAccessContract(world.getContractId())
                    .setApplicationSessionId(world.getApplicationSessionId()),
                new SelectMultiQuery().getFinalSelectById(), unitId);
        if (responseObjectGroup.isOk()) {
            List<JsonNode> results = ((RequestResponseOK<JsonNode>) responseObjectGroup).getResults();
            world.setResults(results);

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
        JsonNode queryJSON = JsonHandler.getFromString(world.getQuery());
        RequestResponse<LogbookOperation> requestResponse =
            world.getAccessClient().selectOperations(
                new VitamContext(world.getTenantId()).setAccessContract(world.getContractId())
                    .setApplicationSessionId(world.getApplicationSessionId()),
                queryJSON);
        if (requestResponse.isOk()) {
            RequestResponseOK<LogbookOperation> requestResponseOK =
                (RequestResponseOK<LogbookOperation>) requestResponse;
            List<JsonNode> results = requestResponseOK.getResultsAsJsonNodes();
            world.setResults(results);
        } else {
            VitamError vitamError = (VitamError) requestResponse;
            Fail.fail("request selectOperation return an error: " + vitamError.getCode());
        }
    }

    /**
     * Import or Check an admin referential file
     *
     * @param action     the action we want to execute : "vérifie" for check / "importe" for import
     * @param filename   name of the file to import or check
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
            ArrayList<JsonNode> results = new ArrayList<>();
            if ("vérifie".equals(action)) {
                status = actionVerify(inputStream, adminCollection);
            } else if ("importe".equals(action)) {
                status = actionImport(filename, inputStream, adminCollection);
            }
            if (status != 0) {
                results.add(JsonHandler.createObjectNode().put("Code", String.valueOf(status)));
            }
            world.setResults(results);
        } catch (Exception e) {
            LOGGER.warn("Referentiels collection already imported", e);
        }
    }

    private int actionVerify(InputStream inputStream, AdminCollections adminCollection)
        throws VitamClientException {
        int status = 0;
        VitamContext context =
            new VitamContext(world.getTenantId()).setApplicationSessionId(world.getApplicationSessionId());
        if (FORMATS.equals(adminCollection)) {
            status = world.getAdminClient().checkFormats(context, inputStream).getStatus();
        } else if (RULES.equals(adminCollection)) {
            status = world.getAdminClient().checkRules(context, inputStream).getStatus();
        } else if (AGENCIES.equals(adminCollection)) {
            status = world.getAdminClient().checkAgencies(context, inputStream).getStatus();
        }
        return status;
    }

    private int actionImport(String filename, InputStream inputStream, AdminCollections adminCollection)
        throws AccessExternalClientException, InvalidParseOperationException {
        int status = 0;
        RequestResponse response = null;
        if (FORMATS.equals(adminCollection)) {

            response = world.getAdminClient().createFormats(
                new VitamContext(world.getTenantId())
                    .setApplicationSessionId(world.getApplicationSessionId()),
                inputStream, filename);
            status = response.getHttpCode();
        } else if (RULES.equals(adminCollection)) {
            response =
                world.getAdminClient().createRules(
                    new VitamContext(world.getTenantId())
                        .setApplicationSessionId(world.getApplicationSessionId()),
                    inputStream, filename);
            status = response.getHttpCode();
        } else if (AGENCIES.equals(adminCollection)) {
            response =
                world.getAdminClient().createAgencies(
                    new VitamContext(world.getTenantId())
                        .setApplicationSessionId(world.getApplicationSessionId()),
                    inputStream, filename);
            status = response.getHttpCode();
        }
        if (response != null) {
            final String operationId = response.getHeaderString(GlobalDataRest.X_REQUEST_ID);
            world.setOperationId(operationId);
        }
        return status;
    }

    /**
     * Search in admin collection according to the query define before
     *
     * @param collection name of the collection
     * @throws Throwable
     */
    @When("^je recherche les données dans le référentiel (.*)$")
    public void search_in_admin_collection(String collection) throws Throwable {
        JsonNode queryJSON = JsonHandler.getFromString(world.getQuery());
        AdminCollections adminCollection = AdminCollections.valueOf(collection);
        RequestResponse requestResponse;
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
            case MANAGEMENT_CONTRACTS:
                requestResponse = world.getAdminClient().findManagementContracts(
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
            case ARCHIVE_UNIT_PROFILE:
                requestResponse = world.getAdminClient().findArchiveUnitProfiles(
                    new VitamContext(world.getTenantId()).setAccessContract(null)
                        .setApplicationSessionId(world.getApplicationSessionId()),
                    queryJSON);
                break;
            case SECURITY_PROFILES:
                requestResponse = world.getAdminClient().findSecurityProfiles(
                    new VitamContext(world.getTenantId()).setAccessContract(null)
                        .setApplicationSessionId(world.getApplicationSessionId()),
                    queryJSON);
                break;
            case AGENCIES:
            requestResponse = world.getAdminClient().findAgencies(
                    new VitamContext(world.getTenantId()).setAccessContract(null)
                        .setApplicationSessionId(world.getApplicationSessionId()),
                    queryJSON);
                break;
            default:
                throw new RuntimeException("Unknown collection " + adminCollection);
        }

        if (requestResponse != null && requestResponse.isOk()) {
            List results = ((RequestResponseOK) requestResponse).getResultsAsJsonNodes();
            world.setResults(results);
        } else if (requestResponse != null) {
            VitamError vitamError = (VitamError) requestResponse;
            Fail.fail("request findDocuments return an error: " + vitamError.getCode());
        } else {
            Fail.fail("Collection not found " + collection);
        }
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

    /**
     * elimination analysis
     *
     * @throws Throwable
     */
    @When("^je lance une analyse d'élimination avec pour date le (.*) qui se termine avec le statut (.*)$")
    public void start_elimination_analysis(String analysisDate, String status) throws Throwable {
        JsonNode queryJSON = JsonHandler.getFromString(world.getQuery());
        RequestResponse<JsonNode> requestResponse = world.getAccessClient().startEliminationAnalysis(
            new VitamContext(world.getTenantId()).setAccessContract(world.getContractId())
                .setApplicationSessionId(world.getApplicationSessionId()),
            new EliminationRequestBody(analysisDate, queryJSON));

        if (!requestResponse.isOk()) {
            VitamError vitamError = (VitamError) requestResponse;
            Fail.fail("request startEliminationAnalysis return an error: " + vitamError.getCode());
        }

        final String eliminationOperationId = requestResponse.getHeaderString(GlobalDataRest.X_REQUEST_ID);
        world.setEliminationOperationId(eliminationOperationId);

        checkOperationStatus(eliminationOperationId, StatusCode.valueOf(status));

    }

    /**
     * elimination action
     *
     * @throws Throwable
     */
    @When("^je lance une élimination définitive avec pour date le (.*) qui se termine avec le statut (.*)$")
    public void start_elimination_action(String deleteDate, String status) throws Throwable {
        JsonNode queryJSON = JsonHandler.getFromString(world.getQuery());
        RequestResponse<JsonNode> requestResponse = world.getAccessClient().startEliminationAction(
            new VitamContext(world.getTenantId()).setAccessContract(world.getContractId())
                .setApplicationSessionId(world.getApplicationSessionId()),
            new EliminationRequestBody(deleteDate, queryJSON));

        if (!requestResponse.isOk()) {
            VitamError vitamError = (VitamError) requestResponse;
            Fail.fail("request startEliminationAction return an error: " + vitamError.getCode());
        }

        final String eliminationOperationId = requestResponse.getHeaderString(GlobalDataRest.X_REQUEST_ID);
        world.setEliminationOperationId(eliminationOperationId);

        checkOperationStatus(eliminationOperationId, StatusCode.valueOf(status));
    }

    @When("^je veux faire un audit sur (.*) des objets par service producteur \"([^\"]*)\"$")
    public void je_lance_l_audit_en_service_producteur(String action, String originatingAgnecy) throws Throwable {
        String QUERY = null;
        auditStatus = null;
        if (action.equals("l'existence")) {
            QUERY = "{auditActions:\"AUDIT_FILE_EXISTING\",auditType:\"originatingagency\",objectId:\"" +
                originatingAgnecy +
                "\"}";
        } else if (action.equals("l'intégrité")) {
            QUERY = "{auditActions:\"AUDIT_FILE_INTEGRITY\",auditType:\"originatingagency\",objectId:\"" +
                originatingAgnecy +
                "\"}";
        }

        JsonNode auditOption = JsonHandler.getFromString(QUERY);
        VitamContext vitamContext = new VitamContext(world.getTenantId()).setAccessContract(world.getContractId())
            .setApplicationSessionId(world.getApplicationSessionId());

        RequestResponse response = world.getAdminClient().launchAudit(vitamContext, auditOption);
        assertThat(response.isOk()).isTrue();
        auditStatus = Status.ACCEPTED;
    }

    @When("^je veux faire un audit sur (.*) des objets par tenant (\\d+)$")
    public void je_veux_faire_l_audit_des_objets_de_tenant(String action, int tenant) throws Throwable {
        String QUERY = null;
        auditStatus = null;
        if (action.equals("l'existence")) {
            QUERY = "{auditActions:\"AUDIT_FILE_EXISTING\",auditType:\"tenant\",objectId:\"" + tenant + "\"}";
        } else if (action.equals("l'intégrité")) {
            QUERY = "{auditActions:\"AUDIT_FILE_INTEGRITY\",auditType:\"tenant\",objectId:\"" + tenant + "\"}";
        }

        JsonNode auditOption = JsonHandler.getFromString(QUERY);
        VitamContext vitamContext = new VitamContext(world.getTenantId()).setAccessContract(world.getContractId())
            .setApplicationSessionId(world.getApplicationSessionId());

        RequestResponse response = world.getAdminClient().launchAudit(vitamContext, auditOption);
        assertThat(response.isOk()).isTrue();
        auditStatus = Status.ACCEPTED;
    }
    @When("^je veux faire un audit sur (.*) des objets liés aux unités archivistiques de la requête$")
    public void je_veux_faire_l_audit_des_objets_par_requete(String action) throws Throwable {
        auditStatus = null;
        JsonNode query = JsonHandler.getFromString(world.getQuery());
        ObjectNode auditOption = JsonHandler.createObjectNode();
        if (action.equals("l'existence")) {
            auditOption.put("auditActions", "AUDIT_FILE_EXISTING");
        } else if (action.equals("l'intégrité")) {
            auditOption.put("auditActions", "AUDIT_FILE_INTEGRITY");
        }
        auditOption.put("auditType", "dsl");
        auditOption.set("query", query);
        VitamContext vitamContext = new VitamContext(world.getTenantId()).setAccessContract(world.getContractId())
            .setApplicationSessionId(world.getApplicationSessionId());

        RequestResponse response = world.getAdminClient().launchAudit(vitamContext, auditOption);
        assertThat(response.isOk()).isTrue();
        auditStatus = Status.ACCEPTED;
    }

    @Then("^le réultat de l'audit est succès$")
    public void le_réultat_de_l_audit_est_succès() throws Throwable {
        assertThat(auditStatus.getStatusCode()).isEqualTo(202);
    }

    @When("^je réalise un audit de traçabilité de la requete$")
    public void unit_traceability_audit() throws Throwable {

        JsonNode queryFromString = JsonHandler.getFromString(world.getQuery());

        // Run unit traceability audit
        VitamContext vitamContext = new VitamContext(world.getTenantId()).setAccessContract(world.getContractId())
            .setApplicationSessionId(world.getApplicationSessionId());
        RequestResponse requestResponse = world.getAdminClient().evidenceAudit(vitamContext, queryFromString);


        final String operationId = requestResponse.getHeaderString(GlobalDataRest.X_REQUEST_ID);

        world.setOperationId(operationId);
        final VitamPoolingClient vitamPoolingClient = new VitamPoolingClient(world.getAdminClient());

        boolean process_timeout = vitamPoolingClient
            .wait(world.getTenantId(), operationId, ProcessState.COMPLETED, 1800, 1_000L, TimeUnit.MILLISECONDS);
        if (!process_timeout) {
            fail("Sip processing not finished. Timeout exceeded.");
        }
    }


    @When("^on lance la traçabilité des journaux de cycles de vie des unités archivistiques$")
    public void unit_lfc_traceability() {
        runInVitamThread(() -> {
            VitamThreadUtils.getVitamSession().setTenantId(world.getTenantId());

            RequestResponseOK requestResponseOK = world.getLogbookOperationsClient().traceabilityLfcUnit();
            checkTraceabilityLfcResponseOKOrWarn(requestResponseOK);
        });
    }

    @When("^on lance la traçabilité des journaux de cycles de vie des groupes d'objets$")
    public void objectgroup_lfc_traceability() {
        runInVitamThread(() -> {
            VitamThreadUtils.getVitamSession().setTenantId(world.getTenantId());

            RequestResponseOK requestResponseOK = world.getLogbookOperationsClient().traceabilityLfcObjectGroup();
            checkTraceabilityLfcResponseOKOrWarn(requestResponseOK);
        });
    }

    private void checkTraceabilityLfcResponseOKOrWarn(RequestResponseOK requestResponseOK) throws VitamException {
        assertThat(requestResponseOK.isOk()).isTrue();

        final String traceabilityOperationId = requestResponseOK.getHeaderString(GlobalDataRest.X_REQUEST_ID);

        checkOperationStatus(traceabilityOperationId, StatusCode.OK, StatusCode.WARNING);
    }

    private void checkOperationStatus(String operationId, StatusCode... statuses) throws VitamException {

        assertThat(operationId).isNotNull();

        final VitamPoolingClient vitamPoolingClient = new VitamPoolingClient(world.getAdminClient());
        boolean process_timeout = vitamPoolingClient
            .wait(world.getTenantId(), operationId, ProcessState.COMPLETED, 1800, 1_000L,
                TimeUnit.MILLISECONDS);
        if (!process_timeout) {
            fail("Operation " + operationId + " timed out.");
        }

        VitamContext vitamContext =
            new VitamContext(world.getTenantId()).setAccessContract(world.getContractId())
                .setApplicationSessionId(world.getApplicationSessionId());
        RequestResponse<ItemStatus> operationProcessExecutionDetails =
            world.getAdminClient().getOperationProcessExecutionDetails(vitamContext, operationId);

        assertThat(operationProcessExecutionDetails.isOk()).isTrue();

        assertThat(((RequestResponseOK<ItemStatus>) operationProcessExecutionDetails).getFirstResult()
            .getGlobalStatus()).isIn((Object[]) statuses);
    }

    /**
     * runInVitamThread.
     *
     * @param
     */
    private void runInVitamThread(MyRunnable r) {

        AtomicReference<Throwable> exception = new AtomicReference<>();

        Thread thread = VitamThreadFactory.getInstance().newThread(() -> {
            try {
                r.run();
            } catch (Throwable e) {
                exception.set(e);
            }
        });

        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (exception.get() != null) {
            fail("Test failed with error", exception.get());
        }
    }

    public interface MyRunnable {
        void run() throws Throwable;
    }
}
