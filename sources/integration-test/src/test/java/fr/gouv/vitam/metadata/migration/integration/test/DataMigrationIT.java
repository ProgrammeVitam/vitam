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
package fr.gouv.vitam.metadata.migration.integration.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import com.mongodb.client.FindIterable;
import fr.gouv.vitam.access.external.client.AdminExternalClient;
import fr.gouv.vitam.access.external.client.AdminExternalClientFactory;
import fr.gouv.vitam.access.external.rest.AccessExternalMain;
import fr.gouv.vitam.access.internal.rest.AccessInternalMain;
import fr.gouv.vitam.common.DataLoader;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.VitamTestHelper;
import fr.gouv.vitam.common.accesslog.AccessLogUtils;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.client.VitamClientFactoryInterface;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchIndexAlias;
import fr.gouv.vitam.common.database.server.mongodb.BsonHelper;
import fr.gouv.vitam.common.database.utils.MetadataDocumentHelper;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.format.identification.FormatIdentifierFactory;
import fr.gouv.vitam.common.json.CanonicalJsonFormatter;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.model.DataMigrationBody;
import fr.gouv.vitam.common.model.MetadataStorageHelper;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.AccessionRegisterDetailModel;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.ingest.internal.upload.rest.IngestInternalMain;
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
import fr.gouv.vitam.processing.data.core.ProcessDataAccessImpl;
import fr.gouv.vitam.processing.management.rest.ProcessManagementMain;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageUnavailableDataFromAsyncOfferClientException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.storage.offers.rest.DefaultOfferMain;
import fr.gouv.vitam.worker.core.plugin.migration.MigrationUnits;
import fr.gouv.vitam.worker.server.rest.WorkerMain;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import net.javacrumbs.jsonunit.JsonAssert;
import net.javacrumbs.jsonunit.core.Option;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.lang3.tuple.Pair;
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
import retrofit2.http.Body;
import retrofit2.http.HEAD;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.VitamTestHelper.verifyOperation;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.functional.administration.common.AccessionRegisterDetail.COMMENT;
import static fr.gouv.vitam.functional.administration.common.AccessionRegisterDetail.OB_ID_IN;
import static fr.gouv.vitam.storage.engine.common.model.DataCategory.UNIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class DataMigrationIT extends VitamRuleRunner {

    @ClassRule
    public static VitamServerRunner runner = new VitamServerRunner(
        DataMigrationIT.class,
        mongoRule.getMongoDatabase().getName(),
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
            DefaultOfferMain.class,
            AccessExternalMain.class
        )
    );

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    private static final String JSON_EXTENSION = ".json";
    private static final Integer TENANT_ID = 0;
    private static final String BASIC_AUTHN_USER = "user";
    private static final String BASIC_AUTHN_PWD = "pwd";
    private static final String ADMIN_MANAGEMENT_URL =
        "http://localhost:" + VitamServerRunner.PORT_SERVICE_FUNCTIONAL_ADMIN_ADMIN;
    public static final String CONTRACT_ID = "aName";
    public static final String CONTEXT_IT = "Context_IT";
    public static final String IB_ID_IN_EXAMPLE = "TestObIdInForMigration";
    public static final VitamContext VITAM_CONTEXT = new VitamContext(TENANT_ID).setAccessContract(CONTRACT_ID);

    private static MetadataAdminDataMigrationService metadataAdminDataMigrationService;
    private static AdminExternalClient adminExternalClient;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        handleBeforeClass(Arrays.asList(0, 1, 2), Collections.emptyMap());
        adminExternalClient = AdminExternalClientFactory.getInstance().getClient();
        FormatIdentifierFactory.getInstance().changeConfigurationFile(VitamServerRunner.FORMAT_IDENTIFIERS_CONF);

        new DataLoader("integration-processing").prepareData();

        // Metadata migration service interface - replace non existing client
        final OkHttpClient okHttpClient = new OkHttpClient.Builder().build();

        Retrofit retrofit_admin_management = new Retrofit.Builder()
            .client(okHttpClient)
            .baseUrl(ADMIN_MANAGEMENT_URL)
            .addConverterFactory(JacksonConverterFactory.create())
            .build();

        metadataAdminDataMigrationService = retrofit_admin_management.create(MetadataAdminDataMigrationService.class);
    }

    public static void prepareVitamSession() {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        VitamThreadUtils.getVitamSession().setContractId(CONTRACT_ID);
        VitamThreadUtils.getVitamSession().setContextId(CONTEXT_IT);
    }

    @AfterClass
    public static void tearDownAfterClass() {
        handleAfterClass();
        StorageClientFactory storageClientFactory = StorageClientFactory.getInstance();
        storageClientFactory.setVitamClientType(VitamClientFactoryInterface.VitamClientType.PRODUCTION);
        runAfter();
        VitamClientFactory.resetConnections();
        fr.gouv.vitam.common.external.client.VitamClientFactory.resetConnections();
        shutdownUsedFactoriesCLients();
    }

    @After
    public void afterTest() {
        VitamThreadUtils.getVitamSession().setContractId(CONTRACT_ID);
        VitamThreadUtils.getVitamSession().setContextId(CONTEXT_IT);

        ProcessDataAccessImpl.getInstance().clearWorkflow();
        runAfterMongo(
            Sets.newHashSet(
                MetadataCollections.UNIT.getName(),
                MetadataCollections.OBJECTGROUP.getName(),
                FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getName(),
                FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getName(),
                LogbookCollections.OPERATION.getName(),
                LogbookCollections.LIFECYCLE_UNIT.getName(),
                LogbookCollections.LIFECYCLE_OBJECTGROUP.getName(),
                LogbookCollections.LIFECYCLE_OBJECTGROUP.getName(),
                LogbookCollections.LIFECYCLE_UNIT_IN_PROCESS.getName()
            )
        );

        runAfterEs(
            ElasticsearchIndexAlias.ofMultiTenantCollection(MetadataCollections.UNIT.getName(), 0),
            ElasticsearchIndexAlias.ofMultiTenantCollection(MetadataCollections.UNIT.getName(), 1),
            ElasticsearchIndexAlias.ofMultiTenantCollection(MetadataCollections.OBJECTGROUP.getName(), 0),
            ElasticsearchIndexAlias.ofMultiTenantCollection(MetadataCollections.OBJECTGROUP.getName(), 1),
            ElasticsearchIndexAlias.ofMultiTenantCollection(LogbookCollections.OPERATION.getName(), 0),
            ElasticsearchIndexAlias.ofMultiTenantCollection(LogbookCollections.OPERATION.getName(), 1),
            ElasticsearchIndexAlias.ofCrossTenantCollection(
                FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getName()
            ),
            ElasticsearchIndexAlias.ofCrossTenantCollection(
                FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getName()
            )
        );
    }

    @Test
    @RunWithCustomExecutor
    public void startMetadataDataMigration_failedAuthn() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        Response<Map<Integer, String>> response = metadataAdminDataMigrationService
            .metadataDataMigration("BAD TOKEN")
            .execute();
        assertThat(response.isSuccessful()).isFalse();
    }

    @Test
    @RunWithCustomExecutor
    public void startMetadataDataMigration_emptyDataSet() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        Response<Map<Integer, String>> response = metadataAdminDataMigrationService
            .metadataDataMigration(getBasicAuthnToken())
            .execute();

        assertThat(response.isSuccessful()).isTrue();
        assertNotNull(response.body());

        String requestId = response.body().get(TENANT_ID);

        waitMigration();

        checkReport(requestId, Collections.emptyList());
    }

    @Test
    @RunWithCustomExecutor
    public void startMetadataDataMigration_fullDataSet() throws Exception {
        // Given
        prepareVitamSession();
        String operationId = VitamTestHelper.doIngest(TENANT_ID, "elimination/TEST_ELIMINATION_V2.zip");
        verifyOperation(operationId, OK);

        List<JsonNode> unitsBefore = getMetadata(MetadataCollections.UNIT);

        // When
        Response<Map<Integer, String>> response = metadataAdminDataMigrationService
            .metadataDataMigration(getBasicAuthnToken())
            .execute();
        assertThat(response.isSuccessful()).isTrue();
        assertNotNull(response.body());

        String requestId = response.body().get(TENANT_ID);

        // Then
        waitMigration();

        List<JsonNode> unitsAfter = getMetadata(MetadataCollections.UNIT);
        dumpDataSet(unitsAfter, unitsBefore);

        assertMetadataEquals(unitsBefore, unitsAfter);

        List<JsonNode> rawUnitLifeCycles;
        try (
            LogbookLifeCyclesClient logbookLifeCyclesClient = LogbookLifeCyclesClientFactory.getInstance().getClient()
        ) {
            List<String> unitIds = unitsAfter
                .stream()
                .map(unit -> unit.get(VitamFieldsHelper.id()).asText())
                .collect(Collectors.toList());
            rawUnitLifeCycles = logbookLifeCyclesClient.getRawUnitLifeCycleByIds(unitIds);
        }

        for (JsonNode rawUnitLFC : rawUnitLifeCycles) {
            String lastEventType = rawUnitLFC
                .get("events")
                .get(rawUnitLFC.get("events").size() - 1)
                .get("evType")
                .asText();
            assertThat(lastEventType).isEqualTo(MigrationUnits.LFC_UPDATE_MIGRATION_UNITS);
        }

        FindIterable<Document> rawUnits = MetadataCollections.UNIT.getCollection().find();

        Map<String, JsonNode> rawUnitsById = mapById(rawUnits);
        Map<String, JsonNode> rawUnitLfcById = mapByField(rawUnitLifeCycles, "_id");

        for (String unitId : rawUnitsById.keySet()) {
            checkStoredUnit(rawUnitsById.get(unitId), rawUnitLfcById.get(unitId));
        }

        checkReport(requestId, unitsBefore);
    }

    private void dumpDataSet(List<JsonNode> unitsAfter, List<JsonNode> unitsBefore) {
        for (int i = 0; i < unitsAfter.size(); i++) {
            ObjectNode unitBefore = (ObjectNode) unitsBefore.get(i);
            ObjectNode unitAfter = (ObjectNode) unitsAfter.get(i);
            replaceDates(unitAfter);
            replaceDates(unitBefore);
        }
    }

    private void replaceDates(ObjectNode unit) {
        unit.put(VitamFieldsHelper.approximateCreationDate(), "#TIMESTAMP#");
        unit.put(VitamFieldsHelper.approximateUpdateDate(), "#TIMESTAMP#");
    }

    @Test
    @RunWithCustomExecutor
    public void startAccessionRegisterDetailsCollectionMigrationWithNoComments() throws Exception {
        // Given
        Pair<AccessionRegisterDetailModel, JsonNode> acRegDetBeforeUpdateWithDslQUery = getAcRegDetBeforeMigration(
            "migration_v5/1_UNIT_1_GOT_WITH_EMPTY_COMMENT.zip"
        );

        // Run Migration
        DataMigrationBody dataMigrationBody = new DataMigrationBody(
            FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getName(),
            List.of(OB_ID_IN, COMMENT),
            JsonHandler.toJsonNode(acRegDetBeforeUpdateWithDslQUery.getLeft())
        );
        Response<Void> response = metadataAdminDataMigrationService
            .runtCollectionMigration(dataMigrationBody)
            .execute();

        // Then
        assertThat(response.isSuccessful()).isTrue();
        RequestResponse<AccessionRegisterDetailModel> acRegDetResponseAfterUpdate =
            adminExternalClient.findAccessionRegisterDetails(
                VITAM_CONTEXT,
                acRegDetBeforeUpdateWithDslQUery.getRight()
            );
        AccessionRegisterDetailModel acRegDetAfterUpdate =
            ((RequestResponseOK<AccessionRegisterDetailModel>) acRegDetResponseAfterUpdate).getResults().get(0);
        assertEquals(IB_ID_IN_EXAMPLE, acRegDetAfterUpdate.getObIdIn());
        assertNull(acRegDetAfterUpdate.getComment());
    }

    @Test
    @RunWithCustomExecutor
    public void startAccessionRegisterDetailsCollectionMigration_withMultipleComments() throws Exception {
        // Given
        Pair<AccessionRegisterDetailModel, JsonNode> acRegDetBeforeUpdateWithDslQUery = getAcRegDetBeforeMigration(
            "migration_v5/1_UNIT_1_GOT_WITH_MULTIPLE_COMMENTS.zip"
        );

        // Run Migration
        DataMigrationBody dataMigrationBody = new DataMigrationBody(
            FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getName(),
            List.of(OB_ID_IN, COMMENT),
            JsonHandler.toJsonNode(acRegDetBeforeUpdateWithDslQUery.getLeft())
        );
        Response<Void> response = metadataAdminDataMigrationService
            .runtCollectionMigration(dataMigrationBody)
            .execute();

        // Then
        assertThat(response.isSuccessful()).isTrue();
        RequestResponse<AccessionRegisterDetailModel> acRegDetResponseAfterUpdate =
            adminExternalClient.findAccessionRegisterDetails(
                VITAM_CONTEXT,
                acRegDetBeforeUpdateWithDslQUery.getRight()
            );
        AccessionRegisterDetailModel acRegDetAfterUpdate =
            ((RequestResponseOK<AccessionRegisterDetailModel>) acRegDetResponseAfterUpdate).getResults().get(0);
        assertEquals(IB_ID_IN_EXAMPLE, acRegDetAfterUpdate.getObIdIn());
        assertEquals(2, acRegDetAfterUpdate.getComment().size());
    }

    private Pair<AccessionRegisterDetailModel, JsonNode> getAcRegDetBeforeMigration(String ingestZip)
        throws VitamException, InvalidCreateOperationException {
        prepareVitamSession();
        String operationId = VitamTestHelper.doIngest(TENANT_ID, ingestZip);
        verifyOperation(operationId, OK);

        JsonNode queryDslByOpi = getQueryDslByOpi(operationId);

        RequestResponse<AccessionRegisterDetailModel> acRegDetResponseBeforeUpdate =
            adminExternalClient.findAccessionRegisterDetails(VITAM_CONTEXT, queryDslByOpi);

        AccessionRegisterDetailModel acRegDetBeforeUpdate =
            ((RequestResponseOK<AccessionRegisterDetailModel>) acRegDetResponseBeforeUpdate).getResults().get(0);

        // Simulate update in comment ( wich is already done in migration playbook ) to avoid update identical document Exception in Mongo
        acRegDetBeforeUpdate.setObIdIn(IB_ID_IN_EXAMPLE);

        return Pair.of(acRegDetBeforeUpdate, queryDslByOpi);
    }

    private JsonNode getQueryDslByOpi(String Opi) throws InvalidCreateOperationException {
        Select select = new Select();
        Query query = QueryHelper.eq(AccessionRegisterDetailModel.OPI, Opi);
        select.setQuery(query);
        return select.getFinalSelect();
    }

    private void waitMigration() throws IOException {
        for (
            int nbtimes = 0;
            (nbtimes <= VitamServerRunner.NB_TRY &&
                (metadataAdminDataMigrationService.checkDataMigration(getBasicAuthnToken()).execute().code() !=
                    javax.ws.rs.core.Response.Status.OK.getStatusCode()));
            nbtimes++
        ) {
            try {
                TimeUnit.MILLISECONDS.sleep(VitamServerRunner.SLEEP_TIME);
            } catch (InterruptedException e) {
                SysErrLogger.FAKE_LOGGER.ignoreLog(e);
            }
        }
    }

    private void checkStoredUnit(JsonNode unit, JsonNode lfc)
        throws StorageNotFoundException, StorageServerClientException, StorageUnavailableDataFromAsyncOfferClientException {
        MetadataDocumentHelper.removeComputedFieldsFromUnit(unit);

        JsonNode docWithLfc = MetadataStorageHelper.getUnitWithLFC(unit, lfc);

        InputStream expectedStoredDocument = CanonicalJsonFormatter.serialize(docWithLfc);

        javax.ws.rs.core.Response response = null;
        try (StorageClient client = StorageClientFactory.getInstance().getClient()) {
            response = client.getContainerAsync(
                "default",
                unit.get(MetadataDocument.ID).asText() + JSON_EXTENSION,
                UNIT,
                AccessLogUtils.getNoLogAccessLog()
            );
            InputStream storedInputStream = response.readEntity(InputStream.class);

            assertThat(storedInputStream).hasSameContentAs(expectedStoredDocument);
        } finally {
            StreamUtils.consumeAnyEntityAndClose(response);
        }
    }

    private void checkReport(String operationId, List<JsonNode> units)
        throws StorageNotFoundException, StorageServerClientException, InvalidParseOperationException, StorageUnavailableDataFromAsyncOfferClientException {
        javax.ws.rs.core.Response response = null;
        try (StorageClient client = StorageClientFactory.getInstance().getClient()) {
            response = client.getContainerAsync(
                "default",
                operationId + JSON_EXTENSION,
                DataCategory.REPORT,
                AccessLogUtils.getNoLogAccessLog()
            );
            InputStream storedInputStream = response.readEntity(InputStream.class);

            ObjectNode expectedReport = JsonHandler.createObjectNode();
            expectedReport.set(
                "units",
                JsonHandler.toJsonNode(
                    units.stream().map(unit -> unit.get(VitamFieldsHelper.id()).asText()).collect(Collectors.toList())
                )
            );

            JsonAssert.assertJsonEquals(
                expectedReport,
                JsonHandler.getFromInputStream(storedInputStream),
                JsonAssert.when(Option.IGNORING_ARRAY_ORDER)
            );
        } finally {
            StreamUtils.consumeAnyEntityAndClose(response);
        }
    }

    private Map<String, JsonNode> mapById(FindIterable<Document> units) {
        Map<String, JsonNode> rawUnitsById;
        rawUnitsById = IterableUtils.toList(units)
            .stream()
            .map(doc -> {
                try {
                    return BsonHelper.fromDocumentToJsonNode(doc);
                } catch (InvalidParseOperationException e) {
                    throw new RuntimeException(e);
                }
            })
            .collect(Collectors.toMap(doc -> doc.get(MetadataDocument.ID).asText(), doc -> doc));
        return rawUnitsById;
    }

    private void assertMetadataEquals(List<JsonNode> before, List<JsonNode> after) {
        Map<String, JsonNode> mdByIdBefore = mapByField(before, VitamFieldsHelper.id());
        Map<String, JsonNode> mdByIdAfter = mapByField(after, VitamFieldsHelper.id());

        assertThat(mdByIdAfter.keySet()).isEqualTo(mdByIdBefore.keySet());
        for (String id : mdByIdBefore.keySet()) {
            JsonAssert.assertJsonEquals(
                mdByIdAfter.get(id),
                mdByIdBefore.get(id),
                JsonAssert.whenIgnoringPaths("#version", "#operations")
            );
        }
    }

    private Map<String, JsonNode> mapByField(List<JsonNode> metadata, String field) {
        return metadata.stream().collect(Collectors.toMap(md -> md.get(field).asText(), md -> md));
    }

    private List<JsonNode> getMetadata(MetadataCollections unit)
        throws InvalidCreateOperationException, MetaDataDocumentSizeException, MetaDataExecutionException, InvalidParseOperationException, MetaDataClientServerException {
        try (MetaDataClient client = MetaDataClientFactory.getInstance().getClient()) {
            SelectMultiQuery selectMultiQuery = new SelectMultiQuery();
            selectMultiQuery.addQueries(eq(VitamFieldsHelper.tenant(), TENANT_ID));
            JsonNode result;
            if (unit == MetadataCollections.UNIT) {
                result = client.selectUnits(selectMultiQuery.getFinalSelect());
            } else {
                result = client.selectObjectGroups(selectMultiQuery.getFinalSelect());
            }
            RequestResponseOK<JsonNode> fromJsonNode = RequestResponseOK.getFromJsonNode(result);
            return fromJsonNode.getResults();
        }
    }

    private static void shutdownUsedFactoriesCLients() {
        AdminExternalClientFactory.getInstance().shutdown();
    }

    private String getBasicAuthnToken() {
        return Credentials.basic(BASIC_AUTHN_USER, BASIC_AUTHN_PWD);
    }

    public interface MetadataAdminDataMigrationService {
        @POST("/adminmanagement/v1/startMigration")
        Call<Map<Integer, String>> metadataDataMigration(@Header("Authorization") String basicAuthnToken);

        @HEAD("/adminmanagement/v1/migrationStatus")
        Call<Void> checkDataMigration(@Header("Authorization") String basicAuthnToken);

        @PUT("/adminmanagement/v1/collectionMigration")
        Call<Void> runtCollectionMigration(@Body DataMigrationBody dataMigrationBody);
    }
}
