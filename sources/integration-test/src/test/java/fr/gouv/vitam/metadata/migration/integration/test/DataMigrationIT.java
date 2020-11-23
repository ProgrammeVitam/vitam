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
package fr.gouv.vitam.metadata.migration.integration.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import com.mongodb.client.FindIterable;
import fr.gouv.vitam.access.internal.rest.AccessInternalMain;
import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.DataLoader;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.accesslog.AccessLogUtils;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.client.VitamClientFactoryInterface;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchIndexAlias;
import fr.gouv.vitam.common.database.utils.MetadataDocumentHelper;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.format.identification.FormatIdentifierFactory;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.database.server.mongodb.BsonHelper;
import fr.gouv.vitam.common.json.CanonicalJsonFormatter;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.MetadataStorageHelper;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.processing.WorkFlow;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClient;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClientFactory;
import fr.gouv.vitam.ingest.internal.upload.rest.IngestInternalMain;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.core.database.collections.MetadataDocument;
import fr.gouv.vitam.metadata.rest.MetadataMain;
import fr.gouv.vitam.processing.common.model.ProcessWorkflow;
import fr.gouv.vitam.processing.data.core.ProcessDataAccessImpl;
import fr.gouv.vitam.processing.engine.core.monitoring.ProcessMonitoringImpl;
import fr.gouv.vitam.processing.management.rest.ProcessManagementMain;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.storage.offers.rest.DefaultOfferMain;
import fr.gouv.vitam.worker.core.plugin.migration.MigrationObjectGroups;
import fr.gouv.vitam.worker.core.plugin.migration.MigrationUnits;
import fr.gouv.vitam.worker.server.rest.WorkerMain;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import io.restassured.RestAssured;
import net.javacrumbs.jsonunit.JsonAssert;
import net.javacrumbs.jsonunit.core.Option;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import org.apache.commons.collections4.IterableUtils;
import org.bson.Document;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.http.Header;
import retrofit2.http.POST;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.VitamTestHelper.waitOperation;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static fr.gouv.vitam.common.guid.GUIDFactory.newOperationLogbookGUID;
import static fr.gouv.vitam.storage.engine.common.model.DataCategory.UNIT;
import static io.restassured.RestAssured.get;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DataMigrationIT extends VitamRuleRunner {

    @ClassRule
    public static VitamServerRunner runner =
        new VitamServerRunner(
            DataMigrationIT.class, mongoRule.getMongoDatabase().getName(),
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
                StorageMain.class,
                DefaultOfferMain.class
            ));

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    private static final long SLEEP_TIME = 20L;
    private static final long NB_TRY = 18000;
    private static final String JSON_EXTENSION = ".json";
    private static final Integer tenantId = 0;
    private static final String METADATA_PATH = "/metadata/v1";
    private static final String PROCESSING_PATH = "/processing/v1";
    private static final String WORKER_PATH = "/worker/v1";
    private static final String WORKSPACE_PATH = "/workspace/v1";
    private static final String LOGBOOK_PATH = "/logbook/v1";
    private static final String INGEST_INTERNAL_PATH = "/ingest/v1";
    private static final String ACCESS_INTERNAL_PATH = "/access-internal/v1";
    private static final String STORAGE_PATH = "/storage/v1";
    private static final String OFFER_PATH = "/offer/v1";
    private static final String BASIC_AUTHN_USER = "user";
    private static final String BASIC_AUTHN_PWD = "pwd";
    private static final String ADMIN_MANAGEMENT_URL =
        "http://localhost:" + VitamServerRunner.PORT_SERVICE_FUNCTIONAL_ADMIN_ADMIN;
    private static MetadataAdminDataMigrationService metadataAdminDataMigrationService;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        handleBeforeClass(Arrays.asList(0, 1), Collections.emptyMap());

        FormatIdentifierFactory.getInstance().changeConfigurationFile(VitamServerRunner.FORMAT_IDENTIFIERS_CONF);

        new DataLoader("integration-processing").prepareData();

        // Metadata migration service interface - replace non existing client
        final OkHttpClient okHttpClient = new OkHttpClient.Builder().build();

        Retrofit retrofit_admin_management =
            new Retrofit.Builder().client(okHttpClient).baseUrl(ADMIN_MANAGEMENT_URL)
                .addConverterFactory(JacksonConverterFactory.create()).build();

        metadataAdminDataMigrationService =
            retrofit_admin_management.create(MetadataAdminDataMigrationService.class);
    }


    public static void prepareVitamSession() {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        VitamThreadUtils.getVitamSession().setContractId("aName");
        VitamThreadUtils.getVitamSession().setContextId("Context_IT");
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        handleAfterClass();
        StorageClientFactory storageClientFactory = StorageClientFactory.getInstance();
        storageClientFactory.setVitamClientType(VitamClientFactoryInterface.VitamClientType.PRODUCTION);
        runAfter();
        VitamClientFactory.resetConnections();
    }

    @After
    public void afterTest() {
        VitamThreadUtils.getVitamSession().setContractId("aName");
        VitamThreadUtils.getVitamSession().setContextId("Context_IT");

        ProcessDataAccessImpl.getInstance().clearWorkflow();
        runAfterMongo(Sets.newHashSet(
            MetadataCollections.UNIT.getName(),
            MetadataCollections.OBJECTGROUP.getName(),
            FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getName(),
            FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getName(),
            LogbookCollections.OPERATION.getName(),
            LogbookCollections.LIFECYCLE_UNIT.getName(),
            LogbookCollections.LIFECYCLE_OBJECTGROUP.getName(),
            LogbookCollections.LIFECYCLE_OBJECTGROUP.getName(),
            LogbookCollections.LIFECYCLE_UNIT_IN_PROCESS.getName()

        ));

        runAfterEs(
            ElasticsearchIndexAlias.ofMultiTenantCollection(MetadataCollections.UNIT.getName(), 0),
            ElasticsearchIndexAlias.ofMultiTenantCollection(MetadataCollections.UNIT.getName(), 1),
            ElasticsearchIndexAlias.ofMultiTenantCollection(MetadataCollections.OBJECTGROUP.getName(), 0),
            ElasticsearchIndexAlias.ofMultiTenantCollection(MetadataCollections.OBJECTGROUP.getName(), 1),
            ElasticsearchIndexAlias.ofMultiTenantCollection(LogbookCollections.OPERATION.getName(), 0),
            ElasticsearchIndexAlias.ofMultiTenantCollection(LogbookCollections.OPERATION.getName(), 1),
            ElasticsearchIndexAlias.ofCrossTenantCollection(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getName()),
            ElasticsearchIndexAlias.ofCrossTenantCollection(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getName())
        );
    }

    @RunWithCustomExecutor
    @Test
    public void testServersStatus() {
        RestAssured.port = VitamServerRunner.PORT_SERVICE_PROCESSING;
        RestAssured.basePath = PROCESSING_PATH;
        get("/status").then().statusCode(javax.ws.rs.core.Response.Status.NO_CONTENT.getStatusCode());

        RestAssured.port = VitamServerRunner.PORT_SERVICE_WORKSPACE;
        RestAssured.basePath = WORKSPACE_PATH;
        get("/status").then().statusCode(javax.ws.rs.core.Response.Status.NO_CONTENT.getStatusCode());

        RestAssured.port = VitamServerRunner.PORT_SERVICE_METADATA;
        RestAssured.basePath = METADATA_PATH;
        get("/status").then().statusCode(javax.ws.rs.core.Response.Status.NO_CONTENT.getStatusCode());

        RestAssured.port = VitamServerRunner.PORT_SERVICE_WORKER;
        RestAssured.basePath = WORKER_PATH;
        get("/status").then().statusCode(javax.ws.rs.core.Response.Status.NO_CONTENT.getStatusCode());

        RestAssured.port = VitamServerRunner.PORT_SERVICE_LOGBOOK;
        RestAssured.basePath = LOGBOOK_PATH;
        get("/status").then().statusCode(javax.ws.rs.core.Response.Status.NO_CONTENT.getStatusCode());

        RestAssured.port = VitamServerRunner.PORT_SERVICE_INGEST_INTERNAL;
        RestAssured.basePath = INGEST_INTERNAL_PATH;
        get("/status").then().statusCode(javax.ws.rs.core.Response.Status.NO_CONTENT.getStatusCode());

        RestAssured.port = VitamServerRunner.PORT_SERVICE_ACCESS_INTERNAL;
        RestAssured.basePath = ACCESS_INTERNAL_PATH;
        get("/status").then().statusCode(javax.ws.rs.core.Response.Status.NO_CONTENT.getStatusCode());

        RestAssured.port = VitamServerRunner.PORT_SERVICE_STORAGE;
        RestAssured.basePath = STORAGE_PATH;
        get("/status").then().statusCode(javax.ws.rs.core.Response.Status.NO_CONTENT.getStatusCode());

        RestAssured.port = VitamServerRunner.PORT_SERVICE_OFFER;
        RestAssured.basePath = OFFER_PATH;
        get("/status").then().statusCode(javax.ws.rs.core.Response.Status.NO_CONTENT.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void startMetadataDataMigration_failedAuthn() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);

        Response<Void>
            response = metadataAdminDataMigrationService.metadataDataMigration("BAD TOKEN", tenantId).execute();
        assertThat(response.isSuccessful()).isFalse();
    }

    @Test
    @RunWithCustomExecutor
    public void startMetadataDataMigration_emptyDataSet() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);

        Response<Void>
            response =
            metadataAdminDataMigrationService.metadataDataMigration(getBasicAuthnToken(), tenantId).execute();
        assertThat(response.isSuccessful()).isTrue();
        String requestId = response.headers().get(GlobalDataRest.X_REQUEST_ID);

        awaitForWorkflowTerminationWithStatus(requestId, StatusCode.WARNING);

        checkReport(requestId, Collections.emptyList(), Collections.emptyList());
    }

    @Test
    @RunWithCustomExecutor
    public void startMetadataDataMigration_fullDataSet() throws Exception {

        // Given
        doIngest(PropertiesUtils.getResourceAsStream("elimination/TEST_ELIMINATION_V2.zip"), StatusCode.OK);

        List<JsonNode> unitsBefore = getMetadata(MetadataCollections.UNIT);
        List<JsonNode> objectGroupsBefore = getMetadata(MetadataCollections.OBJECTGROUP);

        // When
        Response<Void> response =
            metadataAdminDataMigrationService.metadataDataMigration(getBasicAuthnToken(), tenantId).execute();
        assertThat(response.isSuccessful()).isTrue();

        // Then
        String requestId = response.headers().get(GlobalDataRest.X_REQUEST_ID);
        awaitForWorkflowTerminationWithStatus(requestId, StatusCode.OK);

        List<JsonNode> unitsAfter = getMetadata(MetadataCollections.UNIT);
        List<JsonNode> objectGroupsAfter = getMetadata(MetadataCollections.OBJECTGROUP);

        assertMetadataEquals(unitsBefore, unitsAfter);
        assertMetadataEquals(objectGroupsBefore, objectGroupsAfter);

        List<JsonNode> rawUnitLifeCycles;
        List<JsonNode> rawObjectGroupLifeCycles;
        try (LogbookLifeCyclesClient logbookLifeCyclesClient = LogbookLifeCyclesClientFactory.getInstance()
            .getClient()) {
            List<String> unitIds =
                unitsAfter.stream().map(unit -> unit.get(VitamFieldsHelper.id()).asText()).collect(Collectors.toList());
            List<String> objectGroupIds = objectGroupsAfter.stream().map(og -> og.get(VitamFieldsHelper.id()).asText())
                .collect(Collectors.toList());
            rawUnitLifeCycles = logbookLifeCyclesClient.getRawUnitLifeCycleByIds(unitIds);
            rawObjectGroupLifeCycles = logbookLifeCyclesClient.getRawObjectGroupLifeCycleByIds(objectGroupIds);
        }

        for (JsonNode rawUnitLFC : rawUnitLifeCycles) {
            String lastEventType =
                rawUnitLFC.get("events").get(rawUnitLFC.get("events").size() - 1).get("evType").asText();
            assertThat(lastEventType).isEqualTo(MigrationUnits.LFC_UPDATE_MIGRATION_UNITS);
        }

        for (JsonNode rawObjectGroupLFC : rawObjectGroupLifeCycles) {
            String lastEventType =
                rawObjectGroupLFC.get("events").get(rawObjectGroupLFC.get("events").size() - 1).get("evType").asText();
            assertThat(lastEventType).isEqualTo(MigrationObjectGroups.LFC_UPDATE_MIGRATION_OBJECT);
        }

        FindIterable<Document> rawUnits = MetadataCollections.UNIT.getCollection().find();
        FindIterable<Document> rawObjectGroups = MetadataCollections.OBJECTGROUP.getCollection().find();

        Map<String, JsonNode> rawUnitsById = mapById(rawUnits);
        Map<String, JsonNode> rawUnitLfcById = mapByField(rawUnitLifeCycles, "_id");
        Map<String, JsonNode> rawObjectGroupsById = mapById(rawObjectGroups);
        Map<String, JsonNode> rawObjectGroupLfcById = mapByField(rawObjectGroupLifeCycles, "_id");

        for (String unitId : rawUnitsById.keySet()) {
            checkStoredUnit(rawUnitsById.get(unitId), rawUnitLfcById.get(unitId));
        }

        for (String objectGroupId : rawObjectGroupsById.keySet()) {
            checkStoredObjectGroup(rawObjectGroupsById.get(objectGroupId), rawObjectGroupLfcById.get(objectGroupId));
        }

        checkReport(requestId, unitsBefore, objectGroupsBefore);
    }

    private void checkStoredUnit(JsonNode unit, JsonNode lfc)
        throws StorageNotFoundException, StorageServerClientException {

        MetadataDocumentHelper.removeComputedFieldsFromUnit(unit);

        JsonNode docWithLfc = MetadataStorageHelper.getUnitWithLFC(unit, lfc);

        InputStream expectedStoredDocument = CanonicalJsonFormatter.serialize(docWithLfc);

        javax.ws.rs.core.Response response = null;
        try (StorageClient client = StorageClientFactory.getInstance().getClient()) {
            response =
                client.getContainerAsync("default", unit.get(MetadataDocument.ID).asText() + JSON_EXTENSION, UNIT,
                    AccessLogUtils.getNoLogAccessLog());
            InputStream storedInputStream = response.readEntity(InputStream.class);

            assertThat(storedInputStream).hasSameContentAs(expectedStoredDocument);
        } finally {
            StreamUtils.consumeAnyEntityAndClose(response);
        }
    }

    private void checkStoredObjectGroup(JsonNode og, JsonNode lfc)
        throws StorageNotFoundException, StorageServerClientException {

        MetadataDocumentHelper.removeComputedFieldsFromObjectGroup(og);

        JsonNode docWithLfc = MetadataStorageHelper.getGotWithLFC(og, lfc);

        InputStream expectedStoredDocument = CanonicalJsonFormatter.serialize(docWithLfc);

        javax.ws.rs.core.Response response = null;
        try (StorageClient client = StorageClientFactory.getInstance().getClient()) {
            response =
                client.getContainerAsync("default", og.get(MetadataDocument.ID).asText() + JSON_EXTENSION,
                    DataCategory.OBJECTGROUP, AccessLogUtils.getNoLogAccessLog());
            InputStream storedInputStream = response.readEntity(InputStream.class);

            assertThat(storedInputStream).hasSameContentAs(expectedStoredDocument);
        } finally {
            StreamUtils.consumeAnyEntityAndClose(response);
        }
    }

    private void checkReport(String operationId, List<JsonNode> units, List<JsonNode> objectGroups)
        throws StorageNotFoundException, StorageServerClientException, InvalidParseOperationException {

        javax.ws.rs.core.Response response = null;
        try (StorageClient client = StorageClientFactory.getInstance().getClient()) {
            response =
                client.getContainerAsync("default", operationId + JSON_EXTENSION, DataCategory.REPORT,
                    AccessLogUtils.getNoLogAccessLog());
            InputStream storedInputStream = response.readEntity(InputStream.class);

            ObjectNode expectedReport = JsonHandler.createObjectNode();
            expectedReport.set("units", JsonHandler.toJsonNode(
                units.stream().map(unit -> unit.get(VitamFieldsHelper.id()).asText()).collect(Collectors.toList())));
            expectedReport.set("objectGroups", JsonHandler.toJsonNode(
                objectGroups.stream().map(og -> og.get(VitamFieldsHelper.id()).asText()).collect(Collectors.toList())));

            JsonAssert.assertJsonEquals(expectedReport, JsonHandler.getFromInputStream(storedInputStream),
                JsonAssert.when(Option.IGNORING_ARRAY_ORDER));

        } finally {
            StreamUtils.consumeAnyEntityAndClose(response);
        }
    }

    private Map<String, JsonNode> mapById(FindIterable<Document> units) {
        Map<String, JsonNode> rawUnitsById;
        rawUnitsById = IterableUtils.toList(units).stream()
            .map(doc -> {
                try {
                    return BsonHelper.fromDocumentToJsonNode(doc);
                } catch (InvalidParseOperationException e) {
                    throw new RuntimeException(e);
                }
            }).collect(Collectors.toMap(doc -> doc.get(MetadataDocument.ID).asText(), doc -> doc));
        return rawUnitsById;
    }

    private void assertMetadataEquals(List<JsonNode> before, List<JsonNode> after) {

        Map<String, JsonNode> mdByIdBefore = mapByField(before, VitamFieldsHelper.id());
        Map<String, JsonNode> mdByIdAfter = mapByField(after, VitamFieldsHelper.id());

        assertThat(mdByIdAfter.keySet()).isEqualTo(mdByIdBefore.keySet());
        for (String id : mdByIdBefore.keySet()) {

            // FIXME : Bug 6147

            JsonAssert.assertJsonEquals(mdByIdAfter.get(id), mdByIdBefore.get(id),
                JsonAssert.whenIgnoringPaths(
                    "#qualifiers[*].versions[*].FileInfo.LastModified",
                    "#qualifiers[*].versions[*].PhysicalDimensions.Height.dValue",
                    "#qualifiers[*].versions[*].PhysicalDimensions.Weight.dValue",
                    "#qualifiers[*].versions[*].PhysicalId",
                    "#qualifiers[*].versions[*].FileInfo.DateCreatedByApplication"));
        }
    }

    private Map<String, JsonNode> mapByField(List<JsonNode> metadata, String field) {
        return metadata.stream()
            .collect(Collectors.toMap(
                md -> md.get(field).asText(),
                md -> md
            ));
    }

    private List<JsonNode> getMetadata(MetadataCollections unit)
        throws InvalidCreateOperationException, MetaDataDocumentSizeException, MetaDataExecutionException,
        InvalidParseOperationException, MetaDataClientServerException {
        try (MetaDataClient client = MetaDataClientFactory.getInstance().getClient()) {
            SelectMultiQuery selectMultiQuery = new SelectMultiQuery();
            selectMultiQuery.addQueries(eq(VitamFieldsHelper.tenant(), tenantId));
            JsonNode result;
            if (unit == MetadataCollections.UNIT) {
                result = client.selectUnits(selectMultiQuery.getFinalSelect());
            } else {
                result = client.selectObjectGroups(selectMultiQuery.getFinalSelect());
            }
            RequestResponseOK<JsonNode> fromJsonNode =
                RequestResponseOK.getFromJsonNode(result);
            return fromJsonNode.getResults();
        }
    }

    private String doIngest(InputStream zipInputStreamSipObject, StatusCode expectedStatusCode) throws VitamException {
        final GUID ingestOperationGuid = newOperationLogbookGUID(tenantId);
        prepareVitamSession();
        VitamThreadUtils.getVitamSession().setRequestId(ingestOperationGuid);
        // workspace client unzip SIP in workspace

        // init default logbook operation
        final List<LogbookOperationParameters> params = new ArrayList<>();
        final LogbookOperationParameters initParameters = LogbookParameterHelper.newLogbookOperationParameters(
            ingestOperationGuid, "Process_SIP_unitary", ingestOperationGuid,
            LogbookTypeProcess.INGEST, StatusCode.STARTED,
            ingestOperationGuid.toString(), ingestOperationGuid);
        params.add(initParameters);

        // call ingest
        IngestInternalClientFactory.getInstance().changeServerPort(VitamServerRunner.PORT_SERVICE_INGEST_INTERNAL);
        final IngestInternalClient client = IngestInternalClientFactory.getInstance().getClient();
        client.uploadInitialLogbook(params);

        // init workflow before execution
        WorkFlow workflow = WorkFlow.of("DEFAULT_WORKFLOW", "PROCESS_SIP_UNITARY", "INGEST");
        client.initWorkflow(workflow);

        client.upload(zipInputStreamSipObject, CommonMediaType.ZIP_TYPE, workflow, ProcessAction.RESUME.name());

        awaitForWorkflowTerminationWithStatus(ingestOperationGuid.getId(), expectedStatusCode);
        return ingestOperationGuid.getId();
    }

    private void awaitForWorkflowTerminationWithStatus(String operationGuid, StatusCode expectedStatusCode) {

        waitOperation(operationGuid);

        ProcessWorkflow processWorkflow =
            ProcessMonitoringImpl.getInstance().findOneProcessWorkflow(operationGuid, tenantId);

        assertNotNull(processWorkflow);
        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
        assertEquals(expectedStatusCode, processWorkflow.getStatus());
    }

    private String getBasicAuthnToken() {
        return Credentials.basic(BASIC_AUTHN_USER, BASIC_AUTHN_PWD);
    }

    public interface MetadataAdminDataMigrationService {

        @POST("/adminmanagement/v1/migrate")
        Call<Void> metadataDataMigration(
            @Header("Authorization") String basicAuthnToken,
            @Header(GlobalDataRest.X_TENANT_ID) int tenant);

    }
}
