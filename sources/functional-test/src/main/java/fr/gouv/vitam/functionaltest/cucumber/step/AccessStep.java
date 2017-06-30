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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.assertj.core.api.AutoCloseableSoftAssertions;
import org.assertj.core.api.Fail;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Iterables;

import cucumber.api.DataTable;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import fr.gouv.vitam.access.external.api.AdminCollections;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientException;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientNotFoundException;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientServerException;
import fr.gouv.vitam.common.FileUtil;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTIONARGS;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.AccessUnauthorizedException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.functional.administration.client.model.ContextModel;
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
        "\"FilingParentId\": \"" + UNIT_GUID + "\"}]";

    private static final String OPERATION_ID = "Operation-Id";

    private static final String UNIT_PREFIX = "unit:";

    private static final String REGEX = "(\\{\\{(.*?)\\}\\})";

    private static final String TITLE = "Title";

    private List<JsonNode> results;

    private World world;

    private String query;
    
    private StatusCode statusCode;

    public AccessStep(World world) {
        this.world = world;
    }

    /**
     * check if the metadata are valid.
     *
     * @param dataTable
     * @throws Throwable
     */
    @Then("^les metadonnées sont$")
    public void metadata_are(DataTable dataTable) throws Throwable {

        JsonNode firstJsonNode = Iterables.get(results, 0);

        List<List<String>> raws = dataTable.raw();

        for (List<String> raw : raws) {
            String resultValue = getResultValue(firstJsonNode, raw.get(0));
            String resultExpected = transformToGuid(raw.get(1));
            assertThat(resultValue).contains(resultExpected);
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
            world.getAdminClient().importContracts(new ByteArrayInputStream(newContract.getBytes()), 
                world.getTenantId(), AdminCollections.ENTRY_CONTRACTS);
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
            world.getAccessClient().selectUnits(searchQuery.getFinalSelect(), world.getTenantId(),
                world.getContractId());
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
        String s = null;
        try{
            RequestResponse<JsonNode> requestResponse = world.getAccessClient().selectUnits(queryJSON,
            world.getTenantId(), world.getContractId());
        } catch (AccessUnauthorizedException e){
            s = Status.UNAUTHORIZED.toString();
        }
        assertThat(status).isEqualTo(s);
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
        try{
            RequestResponse<JsonNode> requestResponse = world.getAccessClient().updateUnitbyId(queryJSON, unitId,
            world.getTenantId(), world.getContractId());
        } catch (AccessUnauthorizedException e){
            s = Status.UNAUTHORIZED.toString();
        }
        assertThat(status).isEqualTo(s);
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
     * search an archive unit according to the query define before
     *
     * @throws Throwable
     */
    @When("^je recherche les unités archivistiques$")
    public void search_archive_unit() throws Throwable {
        JsonNode queryJSON = JsonHandler.getFromString(query);
        RequestResponse<JsonNode> requestResponse = world.getAccessClient().selectUnits(queryJSON,
            world.getTenantId(), world.getContractId());
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
        RequestResponse<JsonNode> requestResponse = world.getAccessClient().selectUnits(queryJSON,
                world.getTenantId(), world.getContractId());
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
        RequestResponse<JsonNode> requestResponse =
            world.getAccessClient().updateUnitbyId(queryJSON, unitId, world.getTenantId(), world.getContractId());
        if (requestResponse.isOk()) {
            RequestResponseOK<JsonNode> requestResponseOK = (RequestResponseOK<JsonNode>) requestResponse;
            results = requestResponseOK.getResults();
        } else {
            VitamError vitamError = (VitamError) requestResponse;
            Fail.fail("request selectUnit return an error: " + vitamError.getCode());
        }
    }


    /**
     * Search an archive unit and retrieve object groups according to the query define before. 
     * Search object group with archive unit Id
     *
     * @throws Throwable
     */
    @When("^je recherche les groupes d'objets de l'unité archivistique dont le titre est (.*)$")
    public void search_archive_unit_object_group(String title) throws Throwable {
        String unitId = replaceTitleByGUID(title);
        RequestResponse responseObjectGroup =
            world.getAccessClient().selectObjectById(new SelectMultiQuery().getFinalSelect(),
                unitId, world.getTenantId(), world.getContractId());
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
        RequestResponse<JsonNode> requestResponse =
            world.getAccessClient().selectOperation(queryJSON, world.getTenantId(), world.getContractId());
        if (requestResponse.isOk()) {
            RequestResponseOK<JsonNode> requestResponseOK = (RequestResponseOK<JsonNode>) requestResponse;
            results = requestResponseOK.getResults();
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
            world.getAccessClient().getAccessionRegisterDetail(originatingAgency, queryJSON,
                world.getTenantId(), world.getContractId());
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
            Status status = null;
            results = new ArrayList<>();
            if ("vérifie".equals(action)) {
                status =
                    world.getAdminClient().checkDocuments(adminCollection, inputStream, world.getTenantId());
            } else if ("importe".equals(action)) {
                status =
                    world.getAdminClient().createDocuments(adminCollection, inputStream, world.getTenantId());
            }
            if (status != null) {
                results.add(JsonHandler.createObjectNode().put("Code", String.valueOf(status.getStatusCode())));
            }
        } catch (Exception e) {
            LOGGER.warn("Referetiels collection already imported");
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
        RequestResponse<JsonNode> requestResponse =
            world.getAdminClient().findDocuments(adminCollection, queryJSON, world.getTenantId());
        if (requestResponse.isOk()) {
            RequestResponseOK<JsonNode> requestResponseOK = (RequestResponseOK<JsonNode>) requestResponse;
            results = requestResponseOK.getResults();
        } else {
            VitamError vitamError = (VitamError) requestResponse;
            Fail.fail("request findDocuments return an error: " + vitamError.getCode());
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
        RequestResponse<JsonNode> requestResponse =
            world.getAccessClient().selectUnitLifeCycleById(unitId, world.getTenantId(), world.getContractId());        
        if (requestResponse.isOk()) {
            RequestResponseOK<JsonNode> requestResponseOK = (RequestResponseOK<JsonNode>) requestResponse;
            results = requestResponseOK.getResults();
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
            world.getAccessClient().selectUnitbyId(new SelectMultiQuery().getFinalSelect(), 
                unitId, world.getTenantId(), world.getContractId());        
        if (requestResponse.isOk()) {
            RequestResponseOK<JsonNode> requestResponseOK = (RequestResponseOK<JsonNode>) requestResponse;
            JsonNode unit = requestResponseOK.getResults().get(0);
            if (unit.get(PROJECTIONARGS.OBJECT.exactToken()).asText().isEmpty()) {
                VitamError vitamError = (VitamError) requestResponse;
                Fail.fail("Unit does not have object");  
            }
            RequestResponse<JsonNode> requestResponseLFC =
                world.getAccessClient().selectObjectGroupLifeCycleById(
                    unit.get(PROJECTIONARGS.OBJECT.exactToken()).asText(), world.getTenantId(), world.getContractId()); 
            if (requestResponseLFC.isOk()) {
                RequestResponseOK<JsonNode> requestResponseLFCOK = (RequestResponseOK<JsonNode>) requestResponseLFC;
                results = requestResponseLFCOK.getResults();
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
        List<JsonNode> list = JsonHandler.toArrayList(actual);
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
    
    
    @When("^je télécharge le fichier binaire de l'unité archivistique nommé \"([^\"]*)\" à l'usage \"([^\"]*)\" version (\\d+)$")
    public void je_télécharge_le_fichier_binaire_à_l_usage_version(String title, String usage, int version) throws Throwable {
        final fr.gouv.vitam.common.database.builder.request.single.Select select =
            new fr.gouv.vitam.common.database.builder.request.single.Select();
        JsonNode queryDsl = select.getFinalSelect();
        try {
            Response response = world.getAccessClient().getObject(queryDsl, replaceTitleByGUID(title), usage, version, 
                world.getTenantId(), world.getContractId());
            statusCode = StatusCode.parseFromHttpStatus(response.getStatus());
        } catch (AccessExternalClientServerException | AccessExternalClientNotFoundException |
            AccessUnauthorizedException | InvalidParseOperationException e) {
            statusCode = StatusCode.parseFromHttpStatus(
                Response.status(Status.UNAUTHORIZED).build().getStatus());
        }
    }

    @Then("^le status de la réponse est (.*)$")
    public void checkStatut(String status) throws Throwable {
        if (status.equals("UNAUTHORIZED")) {
            assertThat(Response.Status.UNAUTHORIZED.getStatusCode() == statusCode.getEquivalentHttpStatus().getStatusCode());
        } else if (status.equals("OK")) {
            assertThat(Response.Status.OK.getStatusCode() == statusCode.getEquivalentHttpStatus().getStatusCode());
        }
        
    }

    @When("^je modifie le contrat d'accès (.*) avec le fichier de requête suivant (.*)$")
    public void je_modifie_le_contrat_d_accès(String name ,String queryFilename) throws Throwable {
        Path queryFile = Paths.get(world.getBaseDirectory(), queryFilename);
        this.query = FileUtil.readFile(queryFile.toFile());
        if (world.getOperationId() != null) {
            this.query = this.query.replace(OPERATION_ID, world.getOperationId());
        }
        
        JsonNode queryDsl = JsonHandler.getFromString(query);
        world.getAdminClient().updateAccessContract(get_contract_id_by_name(name),
            queryDsl, world.getTenantId());
    }
    
    private String get_contract_id_by_name(String name) 
        throws AccessExternalClientNotFoundException, AccessExternalClientException, InvalidParseOperationException{
        
        String QUERY = "{\"$query\":{\"$and\":[{\"$eq\":{\"Name\":\"" + name +
            "\"}}]},\"$filter\":{},\"$projection\":{}}";
        JsonNode queryDsl =JsonHandler.getFromString(QUERY);        
        
        RequestResponse<ContextModel> requestResponse = 
            world.getAdminClient().findDocuments(AdminCollections.ACCESS_CONTRACTS, queryDsl, world.getTenantId());
        return requestResponse.toJsonNode().findValue("_id").asText();
    }

}
