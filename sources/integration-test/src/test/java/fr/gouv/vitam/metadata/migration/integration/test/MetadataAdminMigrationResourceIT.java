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
import com.google.common.collect.Sets;
import fr.gouv.vitam.access.internal.client.AccessInternalClient;
import fr.gouv.vitam.access.internal.client.AccessInternalClientFactory;
import fr.gouv.vitam.access.internal.common.exception.AccessInternalClientServerException;
import fr.gouv.vitam.access.internal.rest.AccessInternalMain;
import fr.gouv.vitam.batch.report.rest.BatchReportMain;
import fr.gouv.vitam.common.DataLoader;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.VitamTestHelper;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.client.VitamClientFactoryInterface;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchIndexAlias;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.format.identification.FormatIdentifierFactory;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.dip.DataObjectVersions;
import fr.gouv.vitam.common.model.export.ExportRequest;
import fr.gouv.vitam.common.model.export.transfer.TransferRequest;
import fr.gouv.vitam.common.model.export.transfer.TransferRequestParameters;
import fr.gouv.vitam.common.model.logbook.LogbookOperation;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.common.time.LogicalClockRule;
import fr.gouv.vitam.common.utils.SupportedSedaVersions;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.ingest.internal.upload.rest.IngestInternalMain;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.rest.MetadataMain;
import fr.gouv.vitam.processing.management.rest.ProcessManagementMain;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.storage.offers.rest.DefaultOfferMain;
import fr.gouv.vitam.worker.server.rest.WorkerMain;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static fr.gouv.vitam.common.VitamTestHelper.verifyOperation;
import static fr.gouv.vitam.common.guid.GUIDFactory.newOperationLogbookGUID;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.common.model.administration.DataObjectVersionType.BINARY_MASTER;
import static fr.gouv.vitam.common.thread.VitamThreadUtils.getVitamSession;
import static org.assertj.core.api.Assertions.assertThat;

public class MetadataAdminMigrationResourceIT extends VitamRuleRunner {

    private static final Integer TENANT_ID = 0;
    private static final String CONTRACT_ID = "aName5";
    private static final String CONTEXT_ID = "Context_IT";
    private static final String METADATA_URL = "http://localhost:28098";
    private static final String BASIC_AUTHN_USER = "user";
    private static final String BASIC_AUTHN_PWD = "pwd";

    @ClassRule
    public static VitamServerRunner runner = new VitamServerRunner(
        MetadataAdminMigrationResourceIT.class,
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
            BatchReportMain.class
        )
    );

    @ClassRule
    public static ElasticsearchRule elasticsearchRule = new ElasticsearchRule();

    private static MigrateUnitsWithTransferApi migrateUnitsWithTransferApi;

    @Rule
    public LogicalClockRule logicalClock = new LogicalClockRule();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        handleBeforeClass(Arrays.asList(0, 1, 2), Collections.emptyMap());
        String CONFIG_SIEGFRIED_PATH = PropertiesUtils.getResourcePath(
            "integration-ingest-internal/format-identifiers.conf"
        ).toString();

        FormatIdentifierFactory.getInstance().changeConfigurationFile(CONFIG_SIEGFRIED_PATH);

        new DataLoader("integration-ingest-internal").prepareData();

        final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .readTimeout(600, TimeUnit.SECONDS)
            .connectTimeout(600, TimeUnit.SECONDS)
            .build();
        Retrofit retrofit = new Retrofit.Builder()
            .client(okHttpClient)
            .baseUrl(METADATA_URL)
            .addConverterFactory(JacksonConverterFactory.create())
            .build();
        migrateUnitsWithTransferApi = retrofit.create(MigrateUnitsWithTransferApi.class);
    }

    @AfterClass
    public static void tearDownAfterClass() {
        handleAfterClass();
        StorageClientFactory storageClientFactory = StorageClientFactory.getInstance();
        storageClientFactory.setVitamClientType(VitamClientFactoryInterface.VitamClientType.PRODUCTION);
        runAfter();
        VitamClientFactory.resetConnections();
    }

    @Before
    public void setUpBefore() {
        VitamTestHelper.prepareVitamSession(TENANT_ID, CONTRACT_ID, CONTEXT_ID);
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(TENANT_ID));
    }

    @After
    public void setUpAfter() {
        runAfter();

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
    public void test_migrate_units_ko_when_authn_ko() throws Exception {
        // When
        retrofit2.Response<JsonNode> responseJson = migrateUnitsWithTransferApi
            .migrateUnitsWithTransferRequests(TENANT_ID, 100, "BAD CREDENTIALS")
            .execute();

        // Then
        assertThat(responseJson.code()).isEqualTo(Response.Status.UNAUTHORIZED.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void test_migrate_units_ko_when_missing_tenant() throws Exception {
        // When
        retrofit2.Response<JsonNode> responseJson = migrateUnitsWithTransferApi
            .migrateUnitsWithTransferRequests(null, 100, getBasicAuthnToken())
            .execute();

        // Then
        assertThat(responseJson.code()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void test_migrate_units_without_transfer_request() throws Exception {
        // Given
        final String ingestOpId = VitamTestHelper.doIngest(TENANT_ID, "sip/SimpleTree.zip");
        verifyOperation(ingestOpId, OK);

        SelectMultiQuery select = new SelectMultiQuery();
        select.setQuery(QueryHelper.in(VitamFieldsHelper.operations(), ingestOpId));

        // When
        retrofit2.Response<JsonNode> responseJson = migrateUnitsWithTransferApi
            .migrateUnitsWithTransferRequests(TENANT_ID, 100, getBasicAuthnToken())
            .execute();

        // Then
        assertThat(responseJson.code()).isEqualTo(Response.Status.ACCEPTED.getStatusCode());

        try (MetaDataClient metaDataClient = MetaDataClientFactory.getInstance().getClient()) {
            JsonNode unitsBefore = metaDataClient.selectUnits(select.getFinalSelect()).get("$results");
            assertThat(unitsBefore.size()).isEqualTo(2);
            for (JsonNode unit : unitsBefore) {
                assertThat(unit.get(VitamFieldsHelper.version()).asInt()).isEqualTo(0);
            }
        }
    }

    @Test
    @RunWithCustomExecutor
    public void test_migrate_units_with_transfer_request() throws Exception {
        // Given
        final String ingestOpId = VitamTestHelper.doIngest(TENANT_ID, "sip/SimpleTree.zip");
        verifyOperation(ingestOpId, OK);

        SelectMultiQuery select = new SelectMultiQuery();
        select.setQuery(QueryHelper.in(VitamFieldsHelper.operations(), ingestOpId));

        TransferRequest transferRequest = getTransferRequest(select, SupportedSedaVersions.SEDA_2_2);
        ExportRequest exportRequest = ExportRequest.from(transferRequest);

        String exportOperationId = exportDIP(exportRequest);
        VitamTestHelper.verifyOperation(exportOperationId, OK);

        try (MetaDataClient metaDataClient = MetaDataClientFactory.getInstance().getClient()) {
            JsonNode unitsBefore = metaDataClient.selectUnits(select.getFinalSelect()).get("$results");
            assertThat(unitsBefore.size()).isEqualTo(2);
            for (JsonNode unit : unitsBefore) {
                assertThat(unit.get(VitamFieldsHelper.version()).asInt()).isEqualTo(0);
            }
        }

        // When
        retrofit2.Response<JsonNode> responseJson = migrateUnitsWithTransferApi
            .migrateUnitsWithTransferRequests(TENANT_ID, 100, getBasicAuthnToken())
            .execute();

        // Then
        assertThat(responseJson.code()).isEqualTo(Response.Status.OK.getStatusCode());

        assertThat(responseJson.body()).isNotNull();
        RequestResponseOK<String> response = RequestResponseOK.getFromJsonNode(responseJson.body(), String.class);

        String migrationOperationId = response.getFirstResult();

        VitamTestHelper.verifyOperation(migrationOperationId, OK);
        LogbookOperation logbookOperation = VitamTestHelper.selectLogbookOperation(migrationOperationId);
        assertThat(logbookOperation.getEvIdAppSession()).isEqualTo("internal-units-with-transfer-requests-migration");

        try (MetaDataClient metaDataClient = MetaDataClientFactory.getInstance().getClient()) {
            JsonNode unitsBefore = metaDataClient.selectUnits(select.getFinalSelect()).get("$results");
            assertThat(unitsBefore.size()).isEqualTo(2);
            for (JsonNode unit : unitsBefore) {
                assertThat(unit.get(VitamFieldsHelper.version()).asInt()).isEqualTo(1);
                assertThat(unit.get(VitamFieldsHelper.operations()).toString()).contains(migrationOperationId);
            }
        }
    }

    @Test
    @RunWithCustomExecutor
    public void test_migrate_units_with_transfer_request_ko_when_threshold_exceeded() throws Exception {
        // Given
        final String ingestOpId = VitamTestHelper.doIngest(TENANT_ID, "sip/SimpleTree.zip");
        verifyOperation(ingestOpId, OK);

        SelectMultiQuery select = new SelectMultiQuery();
        select.setQuery(QueryHelper.in(VitamFieldsHelper.operations(), ingestOpId));

        TransferRequest transferRequest = getTransferRequest(select, SupportedSedaVersions.SEDA_2_2);
        ExportRequest exportRequest = ExportRequest.from(transferRequest);

        String exportOperationId = transfer(exportRequest);
        VitamTestHelper.verifyOperation(exportOperationId, OK);

        try (MetaDataClient metaDataClient = MetaDataClientFactory.getInstance().getClient()) {
            JsonNode unitsBefore = metaDataClient.selectUnits(select.getFinalSelect()).get("$results");
            assertThat(unitsBefore.size()).isEqualTo(2);
            for (JsonNode unit : unitsBefore) {
                assertThat(unit.get(VitamFieldsHelper.version()).asInt()).isEqualTo(0);
            }
        }

        // When
        retrofit2.Response<JsonNode> responseJson = migrateUnitsWithTransferApi
            .migrateUnitsWithTransferRequests(TENANT_ID, 1, getBasicAuthnToken())
            .execute();

        // Then
        assertThat(responseJson.code()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    }

    private String transfer(ExportRequest exportRequest) throws Exception {
        GUID transferGuid = newOperationLogbookGUID(TENANT_ID);
        getVitamSession().setRequestId(transferGuid);
        try (AccessInternalClient client = AccessInternalClientFactory.getInstance().getClient()) {
            client.exportByUsageFilter(exportRequest);
            VitamTestHelper.waitOperation(transferGuid.getId());

            return transferGuid.getId();
        }
    }

    private TransferRequest getTransferRequest(SelectMultiQuery select, SupportedSedaVersions seda22) {
        TransferRequest transferRequest = new TransferRequest(
            new DataObjectVersions(Collections.singleton(BINARY_MASTER.getName())),
            select.getFinalSelect(),
            false
        );
        transferRequest.setSedaVersion(seda22.getVersion());
        transferRequest.setTransferRequestParameters(getTransferRequestParameters());
        return transferRequest;
    }

    private TransferRequestParameters getTransferRequestParameters() {
        TransferRequestParameters exportRequestParameters = new TransferRequestParameters();
        exportRequestParameters.setArchivalAgencyIdentifier("Identifier4");
        exportRequestParameters.setArchivalAgreement("ArchivalAgreement0");
        exportRequestParameters.setOriginatingAgencyIdentifier("FRAN_NP_050056");
        exportRequestParameters.setSubmissionAgencyIdentifier("FRAN_NP_050056");
        exportRequestParameters.setRelatedTransferReference(
            List.of("RelatedTransferReference1", "RelatedTransferReference2")
        );
        return exportRequestParameters;
    }

    private String exportDIP(ExportRequest exportRequest) {
        GUID operationGuid = GUIDFactory.newOperationLogbookGUID(TENANT_ID);
        getVitamSession().setRequestId(operationGuid);
        try (AccessInternalClient client = AccessInternalClientFactory.getInstance().getClient()) {
            client.exportByUsageFilter(exportRequest);
            VitamTestHelper.waitOperation(operationGuid.getId());
            return operationGuid.getId();
        } catch (AccessInternalClientServerException e) {
            throw new RuntimeException("Error while running export DIP", e);
        }
    }

    private String getBasicAuthnToken() {
        return Credentials.basic(BASIC_AUTHN_USER, BASIC_AUTHN_PWD);
    }

    public interface MigrateUnitsWithTransferApi {
        @POST("/v1/migrateUnitsWithTransferRequests")
        @Headers({ "Accept: application/json" })
        Call<JsonNode> migrateUnitsWithTransferRequests(
            @Header("X-Tenant-Id") Integer tenant,
            @Query("X-Threshold") int threshold,
            @Header("Authorization") String basicAuthnToken
        );
    }
}
