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

package fr.gouv.vitam.collect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import fr.gouv.vitam.access.external.rest.AccessExternalMain;
import fr.gouv.vitam.access.internal.rest.AccessInternalMain;
import fr.gouv.vitam.collect.external.client.CollectClient;
import fr.gouv.vitam.collect.external.client.CollectClientFactory;
import fr.gouv.vitam.collect.external.dto.ProjectDto;
import fr.gouv.vitam.collect.external.dto.TransactionDto;
import fr.gouv.vitam.collect.internal.CollectMain;
import fr.gouv.vitam.common.DataLoader;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.DataObjectVersionType;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.ingest.external.rest.IngestExternalMain;
import fr.gouv.vitam.ingest.internal.upload.rest.IngestInternalMain;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.rest.MetadataMain;
import fr.gouv.vitam.processing.management.rest.ProcessManagementMain;
import fr.gouv.vitam.worker.server.rest.WorkerMain;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import org.assertj.core.api.Assertions;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.text.ParseException;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

@Ignore
public class CollectIT extends VitamRuleRunner {

    private static final String JSON_NODE_UNIT = "collect/upload_au_collect.json";
    private static final String JSON_NODE_OBJECT = "collect/upload_got_collect.json";
    private static final String BINARY_FILE = "collect/Plan-Barbusse.txt";

    private static final Integer tenantId = 0;
    private static final String APPLICATION_SESSION_ID = "ApplicationSessionId";
    private static final String ACCESS_CONTRACT = "ContratTNR";
    private static final String ZIP_FILE = "collect/sampleStream.zip";
    private static final String OPI = "#opi";

    @ClassRule
    public static VitamServerRunner runner =
        new VitamServerRunner(CollectIT.class, mongoRule.getMongoDatabase().getName(),
            ElasticsearchRule.getClusterName(),
            Sets.newHashSet(
                MetadataMain.class,
                WorkerMain.class,
                AdminManagementMain.class,
                LogbookMain.class,
                WorkspaceMain.class,
                ProcessManagementMain.class,
                AccessInternalMain.class,
                IngestInternalMain.class,
                AccessExternalMain.class,
                IngestExternalMain.class,
                CollectMain.class));
    private static String transactionGuuid;
    private static String projectGuuid;
    private static String unitGuuid;
    private static String objectGroupGuuid;
    private static String usage = DataObjectVersionType.BINARY_MASTER.getName();
    private static Integer version = 1;
    private static CollectClient collectClient;
    private final static String ATTACHEMENT_UNIT_ID = "aeeaaaaaaceevqftaammeamaqvje33aaaaaq";

    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private VitamContext vitamContext = new VitamContext(tenantId)
        .setApplicationSessionId(APPLICATION_SESSION_ID)
        .setAccessContract(ACCESS_CONTRACT);

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        handleBeforeClass(Arrays.asList(0, 1), Collections.emptyMap());
        collectClient = CollectClientFactory.getInstance().getClient();
        new DataLoader("integration-ingest-internal").prepareData();
    }

    @AfterClass
    public static void tearDownAfterClass() {
        handleAfterClass();
        runAfter();
        fr.gouv.vitam.common.client.VitamClientFactory.resetConnections();
        fr.gouv.vitam.common.external.client.VitamClientFactory.resetConnections();
    }


    @RunWithCustomExecutor
    @Test
    public void should_perform_collect_operation() throws Exception {

        ProjectDto projectDto = initProjectData();

        RequestResponse<JsonNode> response = collectClient.initProject(vitamContext, projectDto);
        Assertions.assertThat(response.getStatus()).isEqualTo(200);

        ProjectDto projectDtoResult =
            JsonHandler.getFromJsonNode(JsonHandler.toJsonNode(((RequestResponseOK) response).getFirstResult()),
                ProjectDto.class);
        transactionGuuid = projectDtoResult.getTransactionId();
        projectGuuid = projectDtoResult.getId();
        getProjectById();
        uploadProjectZip();
        uploadUnit();
        getUnitById(unitGuuid);
        getUnitByDslQuery();
        getAttachementUnit(transactionGuuid);
        getUnitsByProjectId();
        uploadGot();
        getObjectById(objectGroupGuuid);
        uploadBinary();
        closeTransaction();
        ingest();
        updateProject();

        createTransactionByProject();
    }

    private void createTransactionByProject() throws VitamClientException, InvalidParseOperationException {

        TransactionDto transaction =
            new TransactionDto("XXXX00000111111", null, null, null,
                null, null, null, null,
                "comment", null, null, null, null, null);

        RequestResponse<JsonNode> response =
            collectClient.initTransaction(vitamContext, transaction, projectGuuid);
        Assertions.assertThat(response.getStatus()).isEqualTo(200);
        RequestResponseOK<JsonNode> requestResponseOK = (RequestResponseOK<JsonNode>) response;
        TransactionDto transactionDtoResult =
            JsonHandler.getFromString(requestResponseOK.getFirstResult().toString(), TransactionDto.class);
        assertThat(transactionDtoResult.getComment()).isEqualTo("comment");
    }

    private static ProjectDto initProjectData() {
        ProjectDto projectDto = new ProjectDto();
        projectDto.setArchivalAgencyIdentifier("Vitam");
        projectDto.setTransferingAgencyIdentifier("AN");
        projectDto.setOriginatingAgencyIdentifier("MICHEL_MERCIER");
        projectDto.setSubmissionAgencyIdentifier("MICHEL_MERCIER");
        projectDto.setMessageIdentifier("20220302-000005");
        projectDto.setArchivalAgencyIdentifier("IC-000001");
        projectDto.setArchivalProfile("ArchiveProfile");
        projectDto.setComment("Versement du service producteur : Cabinet de Michel Mercier");
        projectDto.setUnitUp(ATTACHEMENT_UNIT_ID);
        projectDto.setName("This is my Name !");
        return projectDto;
    }

    private void getUnitsByProjectId()
        throws VitamClientException, JsonProcessingException, InvalidCreateOperationException,
        InvalidParseOperationException {
        // Given
        ProjectDto projectDtoResult = getProjectDtoById(projectGuuid);

        // When
        SelectMultiQuery selectUnit = new SelectMultiQuery();
        selectUnit.getQueries().add(QueryHelper.exists(OPI));
        selectUnit.setLimitFilter(0, VitamConfiguration.getBatchSize());
        RequestResponse<JsonNode> response = collectClient.getUnitsByProjectId(vitamContext, projectDtoResult.getId(),
            selectUnit.getFinalSelect());

        // Then
        assertThat(response.isOk());
        List<ObjectNode> units = ((RequestResponseOK) response).getResults();
        assertThat(units.size()).isEqualTo(17);
        assertThat(units.get(0).get("Title")).isNotNull();
    }

    private ProjectDto getProjectDtoById(String projectId)
        throws VitamClientException, JsonProcessingException, InvalidParseOperationException {
        RequestResponse<JsonNode> response = collectClient.getProjectById(vitamContext, projectId);
        assertThat(response.isOk()).isTrue();
        return JsonHandler.getFromJsonNode(JsonHandler.toJsonNode(((RequestResponseOK) response).getFirstResult()),
            ProjectDto.class);
    }

    public void uploadUnit() throws Exception {
        JsonNode archiveUnitJson = JsonHandler.getFromString(PropertiesUtils.getResourceAsString(JSON_NODE_UNIT));
        RequestResponseOK<JsonNode> response =
            collectClient.uploadArchiveUnit(vitamContext, archiveUnitJson, transactionGuuid);
        assertThat(response.isOk()).isTrue();
        assertThat(response.getFirstResult()).isNotNull();
        assertThat(response.getFirstResult().get("#id")).isNotNull();
        unitGuuid = response.getFirstResult().get("#id").textValue();

    }

    public void uploadGot() throws Exception {
        JsonNode gotJson = JsonHandler.getFromString(PropertiesUtils.getResourceAsString(JSON_NODE_OBJECT));
        RequestResponseOK<JsonNode> response =
            collectClient.addObjectGroup(vitamContext, unitGuuid, version, gotJson, usage);
        assertThat(response.isOk()).isTrue();
        assertThat(response.getFirstResult()).isNotNull();
        assertThat(response.getFirstResult().get("id")).isNotNull();
        objectGroupGuuid = response.getFirstResult().get("id").textValue();
    }

    public void uploadBinary() throws Exception {
        try (InputStream inputStream =
            PropertiesUtils.getResourceAsStream(BINARY_FILE)) {
            Response response = collectClient.addBinary(vitamContext, unitGuuid, version, inputStream, usage);
            Assertions.assertThat(response.getStatus()).isEqualTo(200);
        }
    }

    public void closeTransaction() throws Exception {
        Response response = collectClient.closeTransaction(vitamContext, transactionGuuid);
        Assertions.assertThat(response.getStatus()).isEqualTo(200);
    }


    public void ingest() throws Exception {
        RequestResponseOK<JsonNode> response = collectClient.ingest(vitamContext, transactionGuuid);
        assertThat(response.isOk()).isTrue();
        assertThat(response.getFirstResult()).isNotNull();
        assertThat(response.getFirstResult().get("id")).isNotNull();
        String operationId = response.getFirstResult().get("id").textValue();
        Assertions.assertThat(operationId).isNotNull();
    }


    public void getProjectById() throws Exception {
        ProjectDto projectDtoResult = getProjectDtoById(projectGuuid);

        assertThat(projectDtoResult).isNotNull();
        assertThat(projectDtoResult.getId()).isEqualTo(projectGuuid);
    }

    public void getUnitByDslQuery() throws Exception {
        String unitDsl = "{\"$roots\": [],\"$query\": [{\"$eq\": {\"#opi\": \"" + transactionGuuid +
            "\"} }],\"$filter\": {\"$offset\": 0,\"$limit\": 100},\"$projection\": {}}";
        RequestResponseOK<JsonNode> response =
            collectClient.selectUnits(vitamContext, JsonHandler.getFromString(unitDsl));
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getFirstResult()).isNotNull();
        assertThat(response.getHits().getTotal()).isEqualTo(1);
    }

    public void getUnitById(String unitId) throws Exception {
        RequestResponseOK<JsonNode> response = collectClient.getUnitById(vitamContext, unitId);
        assertThat(response.isOk()).isTrue();
        assertThat(response.getFirstResult()).isNotNull();
        assertThat(response.getFirstResult().get("#id")).isNotNull();
        assertThat(response.getFirstResult().get("#id").textValue()).isEqualTo(unitId);
        assertThat(response.getFirstResult().get(OPI).textValue()).isEqualTo(transactionGuuid);
    }

    public void getAttachementUnit(String transactionId) throws Exception {
        RequestResponseOK<JsonNode> response = collectClient.getUnitsByTransaction(vitamContext, transactionId);
        assertThat(response.isOk()).isTrue();
        assertThat(response.getFirstResult()).isNotNull();
        ArrayNode arrayNodeUnits = (ArrayNode) response.getFirstResult().get("$results");
        JsonNode attachmentAu = StreamSupport.stream(arrayNodeUnits.spliterator(), false)
            .filter(unit -> unit.get("DescriptionLevel").textValue().equals("Series")
            ).findFirst().get();



        assertThat(attachmentAu).isNotNull();
        assertThat(attachmentAu.get("#id")).isNotNull();
        assertThat(attachmentAu.get("DescriptionLevel").textValue()).isEqualTo("Series");
        assertThat(attachmentAu.get("#management").get("UpdateOperation").get("SystemId").textValue())
            .isEqualTo(ATTACHEMENT_UNIT_ID);
    }

    public void getObjectById(String objectId) throws Exception {
        RequestResponseOK<JsonNode> response = collectClient.getObjectById(vitamContext, objectId);
        assertThat(response.isOk()).isTrue();
        assertThat(response.getFirstResult()).isNotNull();
        assertThat(response.getFirstResult().get("_id")).isNotNull();
        assertThat(response.getFirstResult().get("_id").textValue()).isEqualTo(objectGroupGuuid);
    }

    public void updateProject()
        throws VitamClientException, JsonProcessingException, InvalidParseOperationException, ParseException {

        // GIVEN
        ProjectDto projectDto = initProjectData();
        RequestResponse<JsonNode> createdProject = collectClient.initProject(vitamContext, projectDto);
        projectDto = JsonHandler.getFromString(((RequestResponseOK) createdProject).getFirstResult().toString(),
            ProjectDto.class);

        ProjectDto projectDtoResult = getProjectDtoById(projectDto.getId());
        assertThat(projectDtoResult).isNotNull();
        assertThat(projectDtoResult.getCreationDate()).isNotNull();
        assertThat(projectDtoResult.getId()).isEqualTo(projectDto.getId());
        assertThat(LocalDateUtil.getDate(projectDtoResult.getCreationDate())).isEqualTo(
            LocalDateUtil.getDate(projectDtoResult.getLastUpdate()));

        // WHEN
        projectDtoResult.setComment("COMMENT AFTER UPDATE");
        collectClient.updateProject(vitamContext, projectDtoResult);
        ProjectDto projectDtoResultAfterUpdate = getProjectDtoById(projectDtoResult.getId());

        // THEN
        assertThat(projectDtoResultAfterUpdate.getComment()).isNotEqualTo(projectDto.getComment());
        assertThat(projectDtoResultAfterUpdate.getComment()).isEqualTo("COMMENT AFTER UPDATE");
        assertThat(projectDtoResultAfterUpdate.getLastUpdate()).isNotNull();
        assertTrue(LocalDateUtil.getDate(projectDtoResultAfterUpdate.getLastUpdate())
            .after(LocalDateUtil.getDate(projectDtoResultAfterUpdate.getCreationDate())));
    }

    public void uploadProjectZip() throws Exception {
        try (InputStream inputStream =
            PropertiesUtils.getResourceAsStream(ZIP_FILE)) {
            Response response = collectClient.uploadProjectZip(vitamContext, projectGuuid, inputStream);
            Assertions.assertThat(response.getStatus()).isEqualTo(200);
        }
    }



}
