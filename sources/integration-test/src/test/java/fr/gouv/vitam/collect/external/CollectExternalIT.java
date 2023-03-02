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

package fr.gouv.vitam.collect.external;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Sets;
import fr.gouv.vitam.collect.common.dto.CriteriaProjectDto;
import fr.gouv.vitam.collect.common.dto.ProjectDto;
import fr.gouv.vitam.collect.common.dto.TransactionDto;
import fr.gouv.vitam.collect.external.client.CollectExternalClient;
import fr.gouv.vitam.collect.external.client.CollectExternalClientFactory;
import fr.gouv.vitam.collect.internal.core.common.TransactionStatus;
import fr.gouv.vitam.common.DataLoader;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.core.database.collections.Unit;
import fr.gouv.vitam.metadata.rest.MetadataMain;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import net.javacrumbs.jsonunit.JsonAssert;
import net.javacrumbs.jsonunit.core.Option;
import org.apache.http.HttpStatus;
import org.assertj.core.api.Assertions;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.StreamSupport;

import static org.apache.hc.core5.http.HttpStatus.SC_OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CollectExternalIT extends VitamRuleRunner {

    @ClassRule public static VitamServerRunner runner =
        new VitamServerRunner(CollectExternalIT.class, mongoRule.getMongoDatabase().getName(),
            ElasticsearchRule.getClusterName(),
            Sets.newHashSet(MetadataMain.class, AdminManagementMain.class, LogbookMain.class, WorkspaceMain.class));


    private static final String JSON_NODE_UNIT = "collect/upload_au_collect.json";
    private static final String JSON_NODE_OBJECT = "collect/upload_got_collect.json";
    private static final String BINARY_FILE = "collect/Plan-Barbusse.txt";

    private static final Integer TENANT_ID = 0;
    private static final String OPI = "#opi";
    private static final String SUBMISSION_AGENCY_IDENTIFIER = "MICHEL_MERCIER";
    private static final String MESSAGE_IDENTIFIER = "20220302-000005";
    private static final String UNITS_UPDATED_BY_ZIP_PATH = "collect/units_with_description.json";

    private static final String ZIP_FILE = "collect/sampleStream.zip";
    private static final String UNITS_TO_UPDATE = "collect/updateMetadata/units.json";
    private static final String UNITS_UPDATED_BY_CSV_PATH = "collect/updateMetadata/units_updated.json";
    private static final String METADATA_FILE = "collect/updateMetadata/metadata.csv";


    private static CollectExternalClient collectExternalClient;
    private final static String ATTACHEMENT_UNIT_ID = "aeeaaaaaaceevqftaammeamaqvje33aaaaaq";
    private final VitamContext vitamContext = new VitamContext(TENANT_ID);

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        runner.startMetadataCollectServer();
        runner.startWorkspaceCollectServer();
        runner.startCollectInternalServer();
        runner.startCollectExternalServer();
        handleBeforeClass(Arrays.asList(0, 1), Collections.emptyMap());
        collectExternalClient = CollectExternalClientFactory.getInstance().getClient();
        new DataLoader("integration-ingest-internal").prepareData();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        runner.stopMetadataCollectServer(false);
        runner.stopWorkspaceCollectServer();
        runner.stopCollectExternalServer(false);
        runner.stopCollectExternalServer(false);
        handleAfterClass();
        runAfter();
        fr.gouv.vitam.common.client.VitamClientFactory.resetConnections();
        fr.gouv.vitam.common.external.client.VitamClientFactory.resetConnections();
    }


    @RunWithCustomExecutor
    public void should_perform_collect_operation() throws Exception {
/*
        ProjectDto projectDto = initProjectData();

        RequestResponse<JsonNode> response = collectClient.initProject(vitamContext, projectDto);
        Assertions.assertThat(response.getStatus()).isEqualTo(200);

        ProjectDto projectDtoResult =
            JsonHandler.getFromJsonNode(JsonHandler.toJsonNode(((RequestResponseOK) response).getFirstResult()),
                ProjectDto.class);
        String transactionGuid = projectDtoResult.getTransactionId();
        String projectGuid = projectDtoResult.getId();
        getProjectById();
        should_upload_project_zip();
        uploadUnit();
        getUnitById(unitGuid);
        getUnitByDslQuery();
        getAttachementUnit(transactionGuid);
        getTransactionById(transactionGuid);
        getUnitsByProjectId();
        uploadGot();
        getObjectById(objectGroupGuid);
        uploadBinary();
        closeTransaction();
        ingest();
        updateProject();

        createTransactionByProject();
        searchProjectById();
        searchProjectByMessageIdentifier();
        searchProjectBySubmissionAgencyIdentifier();*/


    }

    private void getTransactionById(String transactionGuuid) throws VitamClientException {
        RequestResponse<JsonNode> response = collectExternalClient.getTransactionById(vitamContext, transactionGuuid);
        assertThat(response.isOk()).isEqualTo(true);
        RequestResponseOK<JsonNode> requestResponseOK = (RequestResponseOK<JsonNode>) response;
        assertThat(requestResponseOK.getFirstResult().get("id").textValue()).isEqualTo(transactionGuuid);
        assertThat(requestResponseOK.getFirstResult().get("LegalStatus").textValue()).isEqualTo(
            TransactionStatus.OPEN.name());
    }

    private void searchProjectBySubmissionAgencyIdentifier()
        throws VitamClientException, InvalidParseOperationException {
        List<ProjectDto> projectsDtoResults = searchValue(SUBMISSION_AGENCY_IDENTIFIER.substring(2));
        assertThat(projectsDtoResults.get(0).getSubmissionAgencyIdentifier()).isEqualTo(SUBMISSION_AGENCY_IDENTIFIER);
    }

    private void searchProjectByMessageIdentifier() throws VitamClientException, InvalidParseOperationException {
        List<ProjectDto> projectsDtoResults = searchValue(MESSAGE_IDENTIFIER.substring(2));
        assertThat(projectsDtoResults.get(0).getMessageIdentifier()).isEqualTo(MESSAGE_IDENTIFIER);
    }



    private void searchProjectById() throws VitamClientException, InvalidParseOperationException {
        String projectId = null;
        List<ProjectDto> projectsDtoResults = searchValue(projectId);
        assertThat(projectsDtoResults.get(0).getId()).isEqualTo(projectId);
    }

    private List<ProjectDto> searchValue(String query) throws VitamClientException, InvalidParseOperationException {
        RequestResponse<JsonNode> response =
            collectExternalClient.searchProject(vitamContext, new CriteriaProjectDto(query));
        Assertions.assertThat(response.getStatus()).isEqualTo(200);
        RequestResponseOK<JsonNode> requestResponseOK = (RequestResponseOK<JsonNode>) response;
        return Arrays.asList(
            JsonHandler.getFromString(requestResponseOK.getFirstResult().toString(), ProjectDto[].class));
    }

    private void getTransactionByProjectId() throws VitamClientException, InvalidParseOperationException {
        String projectGuid = null;
        String transactionGuid = null;
        TransactionDto transactionDtoResult = getTransactionByProjectId(projectGuid);

        assertThat(transactionDtoResult).isNotNull();
        assertThat(transactionDtoResult.getId()).isEqualTo(transactionGuid);
    }



    private TransactionDto getTransactionByProjectId(String projectId)
        throws VitamClientException, InvalidParseOperationException {
        RequestResponse<JsonNode> response = collectExternalClient.getTransactionByProjectId(vitamContext, projectId);
        assertThat(response.isOk()).isTrue();
        return JsonHandler.getFromJsonNode(
            JsonHandler.toJsonNode(((RequestResponseOK<JsonNode>) response).getFirstResult()), TransactionDto.class);
    }

    private void createTransactionByProject() throws VitamClientException, InvalidParseOperationException {
        String projectGuid = null;
        TransactionDto transaction =
            new TransactionDto("XXXX00000111111", null, null, null, null, null, null, null, "comment", null, null, null,
                null, null, TransactionStatus.OPEN.toString());

        RequestResponse<JsonNode> response =
            collectExternalClient.initTransaction(vitamContext, transaction, projectGuid);
        Assertions.assertThat(response.getStatus()).isEqualTo(200);
        RequestResponseOK<JsonNode> requestResponseOK = (RequestResponseOK<JsonNode>) response;
        TransactionDto transactionDtoResult =
            JsonHandler.getFromString(requestResponseOK.getFirstResult().toString(), TransactionDto.class);
        assertThat(transactionDtoResult.getComment()).isEqualTo("comment");
    }

    private static ProjectDto initProjectData() {
        ProjectDto projectDto = new ProjectDto();
        projectDto.setTransferringAgencyIdentifier("AN");
        projectDto.setOriginatingAgencyIdentifier("MICHEL_MERCIER");
        projectDto.setSubmissionAgencyIdentifier(SUBMISSION_AGENCY_IDENTIFIER);
        projectDto.setMessageIdentifier(MESSAGE_IDENTIFIER);
        projectDto.setArchivalAgencyIdentifier("IC-000001");
        projectDto.setArchivalProfile("ArchiveProfile");
        projectDto.setLegalStatus(TransactionStatus.OPEN.name());
        projectDto.setComment("Versement du service producteur : Cabinet de Michel Mercier");
        projectDto.setUnitUp(ATTACHEMENT_UNIT_ID);
        projectDto.setName("This is my Name !");
        return projectDto;
    }


    private TransactionDto initTransaction() throws VitamClientException, InvalidParseOperationException {

        TransactionDto transaction = new TransactionDto();
        transaction.setName("Vitam Name");
        transaction.setArchivalAgreement("Archival agreement");
        transaction.setAcquisitionInformation("AcquisitionInformation");
        transaction.setArchivalAgencyIdentifier("Vitam");
        transaction.setTransferringAgencyIdentifier("AN");
        transaction.setOriginatingAgencyIdentifier("MICHEL_MERCIER");
        transaction.setSubmissionAgencyIdentifier(SUBMISSION_AGENCY_IDENTIFIER);
        transaction.setMessageIdentifier(MESSAGE_IDENTIFIER);
        transaction.setArchivalAgencyIdentifier("IC-000001");
        transaction.setArchivalProfile("ArchiveProfile");
        transaction.setLegalStatus(TransactionStatus.OPEN.name());
        transaction.setComment("Versement du service producteur : Cabinet de Michel Mercier");
        return transaction;
    }

    private ProjectDto getProjectDtoById(String projectId) throws VitamClientException, InvalidParseOperationException {
        RequestResponse<JsonNode> response = collectExternalClient.getProjectById(vitamContext, projectId);
        assertThat(response.isOk()).isTrue();
        return JsonHandler.getFromJsonNode(
            JsonHandler.toJsonNode(((RequestResponseOK<JsonNode>) response).getFirstResult()), ProjectDto.class);
    }

    public void uploadUnit() throws Exception {
        String transactionGuid = null;
        JsonNode archiveUnitJson = JsonHandler.getFromString(PropertiesUtils.getResourceAsString(JSON_NODE_UNIT));
        RequestResponse<JsonNode> response =
            collectExternalClient.uploadArchiveUnit(vitamContext, archiveUnitJson, transactionGuid);
        assertThat(response.isOk()).isTrue();
        assertThat(((RequestResponseOK<JsonNode>) response).getFirstResult()).isNotNull();
        assertThat(((RequestResponseOK<JsonNode>) response).getFirstResult().get("#id")).isNotNull();
        String unitGuid = ((RequestResponseOK<JsonNode>) response).getFirstResult().get("#id").textValue();

    }

    public void uploadGot() throws Exception {
        String unitGuid = null;
        Integer version = null;
        String usage = null;

        JsonNode gotJson = JsonHandler.getFromString(PropertiesUtils.getResourceAsString(JSON_NODE_OBJECT));
        RequestResponse<JsonNode> response =
            collectExternalClient.addObjectGroup(vitamContext, unitGuid, version, gotJson, usage);
        assertThat(((RequestResponseOK<JsonNode>) response).isOk()).isTrue();
        assertThat(((RequestResponseOK<JsonNode>) response).getFirstResult()).isNotNull();
        assertThat(((RequestResponseOK<JsonNode>) response).getFirstResult().get("id")).isNotNull();
        String objectGroupGuid = ((RequestResponseOK<JsonNode>) response).getFirstResult().get("id").textValue();
    }

    public void uploadBinary() throws Exception {
        String unitGuid = null;
        Integer version = null;
        String usage = null;

        try (InputStream inputStream = PropertiesUtils.getResourceAsStream(BINARY_FILE)) {
            Response response =
                collectExternalClient.addBinary(vitamContext, unitGuid, version, inputStream, usage);
            Assertions.assertThat(response.getStatus()).isEqualTo(200);
        }
    }

    public void closeTransaction(String transactionGuid) throws Exception {
        RequestResponse response = collectExternalClient.closeTransaction(vitamContext, transactionGuid);
        Assertions.assertThat(response.getStatus()).isEqualTo(200);
    }


    public void ingest() throws Exception {
        String transactionGuid = null;

        RequestResponse response = collectExternalClient.ingest(vitamContext, transactionGuid);
        assertEquals(response.getStatus(), HttpStatus.SC_OK);
    }


    public void getProjectById() throws Exception {
        String projectGuid = null;
        ProjectDto projectDtoResult = getProjectDtoById(projectGuid);

        assertThat(projectDtoResult).isNotNull();
        assertThat(projectDtoResult.getId()).isEqualTo(projectGuid);
    }

    public void getUnitById(String unitId) throws Exception {
        String transactionGuid = null;
        RequestResponse<JsonNode> response = collectExternalClient.getUnitById(vitamContext, unitId);
        assertThat(response.isOk()).isTrue();
        assertThat(((RequestResponseOK<JsonNode>) response).getFirstResult()).isNotNull();
        assertThat(((RequestResponseOK<JsonNode>) response).getFirstResult().get("#id")).isNotNull();
        assertThat(((RequestResponseOK<JsonNode>) response).getFirstResult().get("#id").textValue()).isEqualTo(unitId);
        assertThat(((RequestResponseOK<JsonNode>) response).getFirstResult().get(OPI).textValue()).isEqualTo(
            transactionGuid);
    }

    public void getAttachementUnit(String transactionId) throws Exception {
        RequestResponse<JsonNode> response = collectExternalClient.getUnitsByTransaction(vitamContext, transactionId,
            new SelectMultiQuery().getFinalSelect());
        assertThat(response.isOk()).isTrue();
        assertThat(((RequestResponseOK<JsonNode>) response).getFirstResult()).isNotNull();
        ArrayNode arrayNodeUnits =
            (ArrayNode) ((RequestResponseOK<JsonNode>) response).getFirstResult().get("$results");
        JsonNode attachmentAu = StreamSupport.stream(arrayNodeUnits.spliterator(), false)
            .filter(unit -> unit.get("DescriptionLevel").textValue().equals("Series")).findFirst().get();



        assertThat(attachmentAu).isNotNull();
        assertThat(attachmentAu.get("#id")).isNotNull();
        assertThat(attachmentAu.get("DescriptionLevel").textValue()).isEqualTo("Series");
        assertThat(attachmentAu.get("#management").get("UpdateOperation").get("SystemId").textValue()).isEqualTo(
            ATTACHEMENT_UNIT_ID);
    }

    public void getObjectById(String objectId) throws Exception {
        String objectGroupGuid = null;

        RequestResponse<JsonNode> response = collectExternalClient.getObjectById(vitamContext, objectId);
        assertThat(response.isOk()).isTrue();
        assertThat(((RequestResponseOK<JsonNode>) response).getFirstResult()).isNotNull();
        assertThat(((RequestResponseOK<JsonNode>) response).getFirstResult().get("_id")).isNotNull();
        assertThat(((RequestResponseOK<JsonNode>) response).getFirstResult().get("_id").textValue()).isEqualTo(
            objectGroupGuid);
    }

    public void updateProject()
        throws VitamClientException, JsonProcessingException, InvalidParseOperationException, ParseException {

        // GIVEN
        ProjectDto projectDto = initProjectData();
        RequestResponse<JsonNode> createdProject = collectExternalClient.initProject(vitamContext, projectDto);
        projectDto =
            JsonHandler.getFromString(((RequestResponseOK<JsonNode>) createdProject).getFirstResult().toString(),
                ProjectDto.class);

        ProjectDto projectDtoResult = getProjectDtoById(projectDto.getId());
        assertThat(projectDtoResult).isNotNull();
        assertThat(projectDtoResult.getCreationDate()).isNotNull();
        assertThat(projectDtoResult.getId()).isEqualTo(projectDto.getId());
        assertThat(LocalDateUtil.getDate(projectDtoResult.getCreationDate())).isEqualTo(
            LocalDateUtil.getDate(projectDtoResult.getLastUpdate()));

        // WHEN
        projectDtoResult.setComment("COMMENT AFTER UPDATE");
        collectExternalClient.updateProject(vitamContext, projectDtoResult);
        ProjectDto projectDtoResultAfterUpdate = getProjectDtoById(projectDtoResult.getId());

        // THEN
        assertThat(projectDtoResultAfterUpdate.getComment()).isNotEqualTo(projectDto.getComment());
        assertThat(projectDtoResultAfterUpdate.getComment()).isEqualTo("COMMENT AFTER UPDATE");
        assertThat(projectDtoResultAfterUpdate.getLastUpdate()).isNotNull();
        assertTrue(LocalDateUtil.getDate(projectDtoResultAfterUpdate.getLastUpdate())
            .after(LocalDateUtil.getDate(projectDtoResultAfterUpdate.getCreationDate())));
    }

    @Test
    @Ignore("we'll be fixed lately")
    public void should_upload_project_zip() throws Exception {
        ProjectDto projectDto = initProjectData();

        final RequestResponse<JsonNode> projectResponse = collectExternalClient.initProject(vitamContext, projectDto);
        Assertions.assertThat(projectResponse.getStatus()).isEqualTo(200);

        ProjectDto projectDtoResult =
            JsonHandler.getFromJsonNode(((RequestResponseOK<JsonNode>) projectResponse).getFirstResult(),
                ProjectDto.class);

        TransactionDto transactiondto = initTransaction();

        RequestResponse<JsonNode> transactionResponse =
            collectExternalClient.initTransaction(vitamContext, transactiondto, projectDtoResult.getId());
        Assertions.assertThat(transactionResponse.getStatus()).isEqualTo(200);

        RequestResponseOK<JsonNode> requestResponseOK = (RequestResponseOK<JsonNode>) transactionResponse;
        TransactionDto transactionDtoResult =
            JsonHandler.getFromString(requestResponseOK.getFirstResult().toString(), TransactionDto.class);
        try (InputStream inputStream = PropertiesUtils.getResourceAsStream(ZIP_FILE)) {
            RequestResponse response =
                collectExternalClient.uploadProjectZip(vitamContext, transactionDtoResult.getId(), inputStream);
            Assertions.assertThat(response.getStatus()).isEqualTo(200);
        }


        final RequestResponse<JsonNode> unitsByTransaction =
            collectExternalClient.getUnitsByTransaction(vitamContext, transactionDtoResult.getId(),
                new SelectMultiQuery().getFinalSelect());


        final JsonNode expectedUnits =
            JsonHandler.getFromFile(PropertiesUtils.getResourceFile(UNITS_UPDATED_BY_ZIP_PATH));

        JsonAssert.assertJsonEquals(
            JsonHandler.toJsonNode(((RequestResponseOK<JsonNode>) unitsByTransaction).getResults()), expectedUnits,
            JsonAssert.when(Option.IGNORING_ARRAY_ORDER).whenIgnoringPaths(
                List.of("[*]." + VitamFieldsHelper.id(), "[*]." + VitamFieldsHelper.unitups(),
                    "[*]." + VitamFieldsHelper.object(), "[*]." + VitamFieldsHelper.allunitups(),
                    "[*]." + VitamFieldsHelper.initialOperation(), "[*]." + VitamFieldsHelper.approximateCreationDate(),
                    "[*]." + VitamFieldsHelper.approximateUpdateDate())));
    }

    @Test
    public void should_update_metadata() throws Exception {
        ProjectDto projectDto = initProjectData();

        final RequestResponse<JsonNode> projectResponse = collectExternalClient.initProject(vitamContext, projectDto);
        Assertions.assertThat(projectResponse.getStatus()).isEqualTo(200);

        ProjectDto projectDtoResult =
            JsonHandler.getFromJsonNode(((RequestResponseOK<JsonNode>) projectResponse).getFirstResult(),
                ProjectDto.class);

        TransactionDto transactiondto = initTransaction();

        RequestResponse<JsonNode> transactionResponse =
            collectExternalClient.initTransaction(vitamContext, transactiondto, projectDtoResult.getId());
        Assertions.assertThat(transactionResponse.getStatus()).isEqualTo(200);

        RequestResponseOK<JsonNode> requestResponseOK = (RequestResponseOK<JsonNode>) transactionResponse;
        TransactionDto transactionDtoResult =
            JsonHandler.getFromString(requestResponseOK.getFirstResult().toString(), TransactionDto.class);

        try (InputStream inputStream = PropertiesUtils.getResourceAsStream(UNITS_TO_UPDATE)) {
            final List<Unit> units = JsonHandler.getFromInputStream(inputStream, List.class, Unit.class);
            for (Unit unit : units) {
                unit.put(Unit.OPI, transactionDtoResult.getId());
            }
            MetadataCollections.UNIT.<Unit>getCollection().insertMany(units);
            MetadataCollections.UNIT.getEsClient().insertFullDocuments(MetadataCollections.UNIT, TENANT_ID, units);
        }


        try (InputStream inputStream = PropertiesUtils.getResourceAsStream(METADATA_FILE)) {
            RequestResponse<JsonNode> response =
                collectExternalClient.updateUnits(vitamContext, transactionDtoResult.getId(), inputStream);
        }

        final RequestResponse<JsonNode> unitsByTransaction =
            collectExternalClient.getUnitsByTransaction(vitamContext, transactionDtoResult.getId(),
                new SelectMultiQuery().getFinalSelect());

        final JsonNode expectedUnits =
            JsonHandler.getFromFile(PropertiesUtils.getResourceFile(UNITS_UPDATED_BY_CSV_PATH));

        JsonAssert.assertJsonEquals(
            JsonHandler.toJsonNode(((RequestResponseOK<JsonNode>) unitsByTransaction).getResults()), expectedUnits,
            JsonAssert.when(Option.IGNORING_ARRAY_ORDER).whenIgnoringPaths(
                List.of("[*]." + VitamFieldsHelper.id(), "[*]." + VitamFieldsHelper.unitups(),
                    "[*]." + VitamFieldsHelper.object(), "[*]." + VitamFieldsHelper.allunitups(),
                    "[*]." + VitamFieldsHelper.initialOperation(), "[*]." + VitamFieldsHelper.approximateCreationDate(),
                    "[*]." + VitamFieldsHelper.approximateUpdateDate())));
    }

    @Test
    public void deleteTransactionById() throws Exception {
        ProjectDto projectDto = initProjectData();

        final RequestResponse<JsonNode> projectResponse = collectExternalClient.initProject(vitamContext, projectDto);
        Assertions.assertThat(projectResponse.getStatus()).isEqualTo(200);

        ProjectDto projectDtoResult =
            JsonHandler.getFromJsonNode(((RequestResponseOK<JsonNode>) projectResponse).getFirstResult(),
                ProjectDto.class);

        TransactionDto transactiondto = initTransaction();

        RequestResponse<JsonNode> transactionResponse =
            collectExternalClient.initTransaction(vitamContext, transactiondto, projectDtoResult.getId());
        Assertions.assertThat(transactionResponse.getStatus()).isEqualTo(200);

        RequestResponseOK<JsonNode> requestResponseOK = (RequestResponseOK<JsonNode>) transactionResponse;
        TransactionDto transactionDtoResult =
            JsonHandler.getFromString(requestResponseOK.getFirstResult().toString(), TransactionDto.class);

        RequestResponse<JsonNode> response =
            collectExternalClient.deleteTransactionById(vitamContext, transactionDtoResult.getId());
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    public void deleteProjectById() throws Exception {
        ProjectDto projectDto = initProjectData();

        final RequestResponse<JsonNode> projectResponse = collectExternalClient.initProject(vitamContext, projectDto);
        Assertions.assertThat(projectResponse.getStatus()).isEqualTo(200);

        ProjectDto projectDtoResult =
            JsonHandler.getFromJsonNode(((RequestResponseOK<JsonNode>) projectResponse).getFirstResult(),
                ProjectDto.class);
        String projectId = projectDtoResult.getId();

        RequestResponse<JsonNode> response = collectExternalClient.deleteProjectById(vitamContext, projectId);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    public void reopenAndAbortTransaction() throws Exception {
        ProjectDto projectDto = initProjectData();

        final RequestResponse<JsonNode> projectResponse = collectExternalClient.initProject(vitamContext, projectDto);
        Assertions.assertThat(projectResponse.getStatus()).isEqualTo(200);

        ProjectDto projectDtoResult =
            JsonHandler.getFromJsonNode(((RequestResponseOK<JsonNode>) projectResponse).getFirstResult(),
                ProjectDto.class);

        TransactionDto transactiondto = initTransaction();

        RequestResponse<JsonNode> transactionResponse =
            collectExternalClient.initTransaction(vitamContext, transactiondto, projectDtoResult.getId());
        Assertions.assertThat(transactionResponse.getStatus()).isEqualTo(200);

        RequestResponseOK<JsonNode> requestResponseOK = (RequestResponseOK<JsonNode>) transactionResponse;
        TransactionDto transactionDtoResult =
            JsonHandler.getFromString(requestResponseOK.getFirstResult().toString(), TransactionDto.class);


        String transactionId = transactionDtoResult.getId();
        collectExternalClient.closeTransaction(vitamContext, transactionId);
        verifyTransactionStatus(TransactionStatus.READY, transactionId);

        //test reopen
        collectExternalClient.reopenTransaction(vitamContext, transactionId);
        verifyTransactionStatus(TransactionStatus.OPEN, transactionId);


        //test abort
        collectExternalClient.abortTransaction(vitamContext, transactionId);
        verifyTransactionStatus(TransactionStatus.ABORTED, transactionId);


    }

    public void verifyTransactionStatus(TransactionStatus status, String transactionId) throws VitamClientException {
        RequestResponse<JsonNode> response = collectExternalClient.getTransactionById(vitamContext, transactionId);
        assertThat(response.isOk()).isTrue();
        RequestResponseOK<JsonNode> requestResponseOK = (RequestResponseOK<JsonNode>) response;
        assertThat(requestResponseOK.getFirstResult().get("#id").textValue()).isEqualTo(transactionId);
        assertThat(requestResponseOK.getFirstResult().get("Status").textValue()).isEqualTo(status.name());
    }

    @Test
    public void updateTransaction() throws Exception {
        ProjectDto projectDto = initProjectData();
        String newComment = "New Comment";
        final RequestResponse<JsonNode> projectResponse = collectExternalClient.initProject(vitamContext, projectDto);
        Assertions.assertThat(projectResponse.getStatus()).isEqualTo(200);

        ProjectDto projectDtoResult =
            JsonHandler.getFromJsonNode(((RequestResponseOK<JsonNode>) projectResponse).getFirstResult(),
                ProjectDto.class);
        TransactionDto initialTransaction = initTransaction();

        // INSERT TRANSACTION
        RequestResponse<JsonNode> transactionResponse =
            collectExternalClient.initTransaction(vitamContext, initialTransaction, projectDtoResult.getId());
        Assertions.assertThat(transactionResponse.getStatus()).isEqualTo(SC_OK);
        RequestResponseOK<JsonNode> requestResponseOK = (RequestResponseOK<JsonNode>) transactionResponse;
        TransactionDto transactionDtoResult =
            JsonHandler.getFromString(requestResponseOK.getFirstResult().toString(), TransactionDto.class);

        // GET PERSISTED TRANSACTION
        RequestResponse<JsonNode> persistedTransactionResponse =
            collectExternalClient.getTransactionById(vitamContext, transactionDtoResult.getId());
        Assertions.assertThat(persistedTransactionResponse.getStatus()).isEqualTo(SC_OK);
        TransactionDto persistedTransaction = JsonHandler.getFromString(
            (((RequestResponseOK<JsonNode>) persistedTransactionResponse).getFirstResult()).toString(),
            TransactionDto.class);
        assertNotNull(persistedTransaction.getCreationDate());
        assertEquals(persistedTransaction.getCreationDate(), persistedTransaction.getLastUpdate());
        assertEquals(TransactionStatus.OPEN.toString(), persistedTransaction.getStatus());
        assertEquals(initialTransaction.getName(), persistedTransaction.getName());
        assertEquals(initialTransaction.getArchivalProfile(), persistedTransaction.getArchivalProfile());
        assertEquals(initialTransaction.getAcquisitionInformation(), persistedTransaction.getAcquisitionInformation());
        assertEquals(initialTransaction.getMessageIdentifier(), persistedTransaction.getMessageIdentifier());

        assertThat(persistedTransaction.getComment()).isNotEqualTo(newComment);

        // Update Transaction
        transactionDtoResult.setComment(newComment);
        RequestResponse<JsonNode> updatedTransactionResponse =
            collectExternalClient.updateTransaction(vitamContext, transactionDtoResult);
        assertThat(updatedTransactionResponse.getStatus()).isEqualTo(SC_OK);
        TransactionDto updatedTransaction = JsonHandler.getFromString(
            (((RequestResponseOK<JsonNode>) updatedTransactionResponse).getFirstResult()).toString(),
            TransactionDto.class);

        assertThat(transactionDtoResult.getComment()).isEqualTo(newComment);
        assertNotNull(updatedTransaction.getCreationDate());
        assertNotNull(updatedTransaction.getLastUpdate());
        assertThat(updatedTransaction.getLastUpdate()).isGreaterThan(updatedTransaction.getCreationDate());
    }
}