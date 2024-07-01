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
package fr.gouv.vitam.ingest.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import fr.gouv.vitam.access.internal.client.AccessInternalClient;
import fr.gouv.vitam.access.internal.client.AccessInternalClientFactory;
import fr.gouv.vitam.access.internal.common.exception.AccessInternalClientNotFoundException;
import fr.gouv.vitam.access.internal.common.exception.AccessInternalClientServerException;
import fr.gouv.vitam.access.internal.rest.AccessInternalMain;
import fr.gouv.vitam.batch.report.rest.BatchReportMain;
import fr.gouv.vitam.common.DataLoader;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.VitamTestHelper;
import fr.gouv.vitam.common.accesslog.AccessLogUtils;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.builder.request.single.Update;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchIndexAlias;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.AccessUnauthorizedException;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.format.identification.FormatIdentifierFactory;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.model.administration.ActivationStatus;
import fr.gouv.vitam.common.model.config.CollectionConfiguration;
import fr.gouv.vitam.common.model.processing.WorkFlow;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.stream.VitamAsyncInputStream;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.AccessContract;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialNotFoundException;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.ingest.internal.upload.rest.IngestInternalMain;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.server.config.DefaultCollectionConfiguration;
import fr.gouv.vitam.logbook.common.server.config.LogbookIndexationConfiguration;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.core.config.GroupedTenantConfiguration;
import fr.gouv.vitam.metadata.core.config.MetadataIndexationConfiguration;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.rest.MetadataMain;
import fr.gouv.vitam.processing.common.model.ProcessWorkflow;
import fr.gouv.vitam.processing.data.core.ProcessDataAccessImpl;
import fr.gouv.vitam.processing.engine.core.monitoring.ProcessMonitoringImpl;
import fr.gouv.vitam.processing.management.rest.ProcessManagementMain;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageUnavailableDataFromAsyncOfferClientException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.storage.offers.rest.DefaultOfferMain;
import fr.gouv.vitam.worker.server.rest.WorkerMain;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import io.restassured.RestAssured;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;

import static fr.gouv.vitam.common.VitamTestHelper.verifyOperation;
import static fr.gouv.vitam.common.VitamTestHelper.waitOperation;
import static fr.gouv.vitam.common.guid.GUIDFactory.newOperationLogbookGUID;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static io.restassured.RestAssured.get;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Ingest Internal integration test
 */
public class IngestInternalTenantGroupIT extends VitamRuleRunner {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IngestInternalTenantGroupIT.class);

    private static final Integer tenantId = 0;
    private static final long SLEEP_TIME = 20L;
    private static final long NB_TRY = 18000; // equivalent to 16 minute
    private static final String METADATA_PATH = "/metadata/v1";
    private static final String PROCESSING_PATH = "/processing/v1";
    private static final String WORKER_PATH = "/worker/v1";
    private static final String WORKSPACE_PATH = "/workspace/v1";
    private static final String LOGBOOK_PATH = "/logbook/v1";
    private static final String INGEST_INTERNAL_PATH = "/ingest/v1";
    private static final String ACCESS_INTERNAL_PATH = "/access-internal/v1";
    private static final String STORAGE_PATH = "/storage/v1";
    private static final String OFFER_PATH = "/offer/v1";
    private static final String BATCH_REPORT_PATH = "/batchreport/v1";
    private static final String WORKFLOW_ID = "DEFAULT_WORKFLOW";
    private static final String WORKFLOW_IDENTIFIER = "PROCESS_SIP_UNITARY";
    private static final String XML = ".xml";
    public static final String TENANT_GROUP = "mygrp";

    @ClassRule
    public static VitamServerRunner runner = new VitamServerRunner(
        IngestInternalTenantGroupIT.class,
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

    private static String CONFIG_SIEGFRIED_PATH = "";
    private static final String TEST_ELIMINATION_V2_SIP = "elimination/TEST_ELIMINATION_V2.zip";
    private final WorkFlow workflow = WorkFlow.of(WORKFLOW_ID, WORKFLOW_IDENTIFIER, "INGEST");

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        handleBeforeClass(Arrays.asList(0, 1), Collections.emptyMap());
        CONFIG_SIEGFRIED_PATH = PropertiesUtils.getResourcePath(
            "integration-ingest-internal/format-identifiers.conf"
        ).toString();

        FormatIdentifierFactory.getInstance().changeConfigurationFile(CONFIG_SIEGFRIED_PATH);

        runner.stopServers();

        // Update config
        MetadataIndexationConfiguration metadataIndexationConfiguration = new MetadataIndexationConfiguration()
            .setDefaultCollectionConfiguration(
                new fr.gouv.vitam.metadata.core.config.DefaultCollectionConfiguration()
                    .setUnit(new CollectionConfiguration(1, 0))
                    .setObjectgroup(new CollectionConfiguration(1, 0))
            )
            .setGroupedTenantConfiguration(
                Collections.singletonList(
                    new GroupedTenantConfiguration()
                        .setName("mygrp")
                        .setTenants("0-1")
                        .setUnit(new CollectionConfiguration(1, 0))
                        .setObjectgroup(new CollectionConfiguration(1, 0))
                )
            );

        LogbookIndexationConfiguration logbookIndexationConfiguration = new LogbookIndexationConfiguration()
            .setDefaultCollectionConfiguration(
                new DefaultCollectionConfiguration().setLogbookoperation(new CollectionConfiguration(1, 0))
            )
            .setGroupedTenantConfiguration(
                Collections.singletonList(
                    new fr.gouv.vitam.logbook.common.server.config.GroupedTenantConfiguration()
                        .setName(TENANT_GROUP)
                        .setTenants("0-1")
                        .setLogbookoperation(new CollectionConfiguration(1, 0))
                )
            );

        runner.setCustomMetadataIndexationConfiguration(metadataIndexationConfiguration);
        runner.setCustomLogbookIndexationConfiguration(logbookIndexationConfiguration);

        // Restart servers
        runner.startServers();

        new DataLoader("integration-ingest-internal").prepareData();
    }

    @After
    public void afterTest() throws Exception {
        VitamThreadUtils.getVitamSession().setContractId("aName");
        VitamThreadUtils.getVitamSession().setContextId("Context_IT");

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
            ElasticsearchIndexAlias.ofMultiTenantCollection(MetadataCollections.UNIT.getName(), TENANT_GROUP),
            ElasticsearchIndexAlias.ofMultiTenantCollection(MetadataCollections.OBJECTGROUP.getName(), TENANT_GROUP),
            ElasticsearchIndexAlias.ofMultiTenantCollection(LogbookCollections.OPERATION.getName(), TENANT_GROUP),
            ElasticsearchIndexAlias.ofCrossTenantCollection(
                FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getName()
            ),
            ElasticsearchIndexAlias.ofCrossTenantCollection(
                FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getName()
            )
        );
    }

    @AfterClass
    public static void tearDownAfterClass() throws VitamApplicationServerException {
        // Stop running servers
        runner.stopServers();

        // Reset custom config
        runner.setCustomMetadataIndexationConfiguration(null);
        runner.setCustomLogbookIndexationConfiguration(null);

        handleAfterClass();
        runAfter();
        VitamClientFactory.resetConnections();
    }

    public static GUID prepareVitamSession(int tenantId) {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        VitamThreadUtils.getVitamSession().setContractId("TenantGroupAccessTest");
        VitamThreadUtils.getVitamSession().setContextId("Context_IT");
        final GUID accessContractRequestId = newOperationLogbookGUID(tenantId);
        VitamThreadUtils.getVitamSession().setRequestId(accessContractRequestId);
        return accessContractRequestId;
    }

    @Before
    public void setUpBefore() {}

    @RunWithCustomExecutor
    @Test
    public void testServersStatus() {
        RestAssured.port = VitamServerRunner.PORT_SERVICE_PROCESSING;
        RestAssured.basePath = PROCESSING_PATH;
        get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());

        RestAssured.port = VitamServerRunner.PORT_SERVICE_WORKSPACE;
        RestAssured.basePath = WORKSPACE_PATH;
        get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());

        RestAssured.port = VitamServerRunner.PORT_SERVICE_METADATA;
        RestAssured.basePath = METADATA_PATH;
        get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());

        RestAssured.port = VitamServerRunner.PORT_SERVICE_WORKER;
        RestAssured.basePath = WORKER_PATH;
        get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());

        RestAssured.port = VitamServerRunner.PORT_SERVICE_LOGBOOK;
        RestAssured.basePath = LOGBOOK_PATH;
        get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());

        RestAssured.port = VitamServerRunner.PORT_SERVICE_INGEST_INTERNAL;
        RestAssured.basePath = INGEST_INTERNAL_PATH;
        get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());

        RestAssured.port = VitamServerRunner.PORT_SERVICE_ACCESS_INTERNAL;
        RestAssured.basePath = ACCESS_INTERNAL_PATH;
        get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());

        RestAssured.port = VitamServerRunner.PORT_SERVICE_STORAGE;
        RestAssured.basePath = STORAGE_PATH;
        get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());

        RestAssured.port = VitamServerRunner.PORT_SERVICE_OFFER;
        RestAssured.basePath = OFFER_PATH;
        get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());

        RestAssured.port = VitamServerRunner.PORT_SERVICE_BATCH_REPORT;
        RestAssured.basePath = BATCH_REPORT_PATH;
        get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());
    }

    @RunWithCustomExecutor
    @Test
    public void testMasterdataTenantIdFilteringWithRightTenantId() throws Exception {
        // Given
        String accessContractIdentifier = RandomStringUtils.randomAlphabetic(10);
        prepareAccessContract(0, accessContractIdentifier);

        // Try find by Query
        String foundAccessContractIdTenant0 = searchAccessContract(
            0,
            AccessContract.IDENTIFIER,
            accessContractIdentifier
        );
        assertThat(foundAccessContractIdTenant0).isNotNull();

        // Try update
        prepareVitamSession(0);
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            Update update = new Update();
            update.setQuery(QueryHelper.eq(AccessContract.IDENTIFIER, accessContractIdentifier));
            update.addActions(UpdateActionHelper.set(AccessContract.NAME, accessContractIdentifier + "UpdatedName"));
            client.updateAccessContract(accessContractIdentifier, update.getFinalUpdate());
        }

        // Try find by Query
        String updatedAccessContractIdTenant0 = searchAccessContract(
            0,
            AccessContract.NAME,
            accessContractIdentifier + "UpdatedName"
        );
        assertThat(updatedAccessContractIdTenant0).isEqualTo(foundAccessContractIdTenant0);
    }

    @RunWithCustomExecutor
    @Test
    public void testMasterdataTenantIdFilteringWithWrongTenantId() throws Exception {
        // Given
        String accessContractIdentifier = RandomStringUtils.randomAlphabetic(10);
        prepareAccessContract(0, accessContractIdentifier);

        // Try find by Query
        String foundAccessContractIdTenant1 = searchAccessContract(
            1,
            AccessContract.IDENTIFIER,
            accessContractIdentifier
        );
        assertThat(foundAccessContractIdTenant1).isNull();

        // Try update
        prepareVitamSession(1);
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            Update update = new Update();
            update.setQuery(QueryHelper.eq(AccessContract.IDENTIFIER, accessContractIdentifier));
            update.addActions(UpdateActionHelper.set(AccessContract.NAME, accessContractIdentifier + "UpdatedName"));
            assertThatThrownBy(
                () -> client.updateAccessContract(accessContractIdentifier, update.getFinalUpdate())
            ).isInstanceOf(ReferentialNotFoundException.class);
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testMasterdataTenantIdFilteringOnContractWithSameIdentifierInDifferentTenants() throws Exception {
        // Given
        String accessContractIdentifier = RandomStringUtils.randomAlphabetic(10);
        prepareAccessContract(0, accessContractIdentifier);
        prepareAccessContract(1, accessContractIdentifier);

        // Try find by Query
        String foundAccessContractIdTenant0 = searchAccessContract(
            0,
            AccessContract.IDENTIFIER,
            accessContractIdentifier
        );
        assertThat(foundAccessContractIdTenant0).isNotNull();
        String foundAccessContractIdTenant1 = searchAccessContract(
            1,
            AccessContract.IDENTIFIER,
            accessContractIdentifier
        );
        assertThat(foundAccessContractIdTenant1).isNotNull();
        assertThat(foundAccessContractIdTenant1).isNotEqualTo(foundAccessContractIdTenant0);

        // Try update Tenant 0
        prepareVitamSession(0);
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            Update update = new Update();
            update.setQuery(QueryHelper.eq(AccessContract.IDENTIFIER, accessContractIdentifier));
            update.addActions(UpdateActionHelper.set(AccessContract.NAME, accessContractIdentifier + "UpdatedName"));
            client.updateAccessContract(accessContractIdentifier, update.getFinalUpdate());
        }

        // Check that access contract of tenant 0 has been updated
        String updatedAccessContractIdTenant0 = searchAccessContract(
            0,
            AccessContract.NAME,
            accessContractIdentifier + "UpdatedName"
        );
        assertThat(updatedAccessContractIdTenant0).isEqualTo(foundAccessContractIdTenant0);

        String updatedAccessContractIdTenant1 = searchAccessContract(
            0,
            AccessContract.NAME,
            accessContractIdentifier + "UpdatedName"
        );
        assertThat(updatedAccessContractIdTenant0).isNotNull();
    }

    @RunWithCustomExecutor
    @Test
    public void testSimpleIngestWithTenantFiltering() throws Exception {
        final AccessInternalClient accessInternalClient = AccessInternalClientFactory.getInstance().getClient();
        prepareAccessContract(0, "TenantGroupAccessTest");
        prepareAccessContract(1, "TenantGroupAccessTest");

        prepareVitamSession(tenantId);
        final String ingestOperationGuidTenant0 = VitamTestHelper.doIngest(0, TEST_ELIMINATION_V2_SIP);
        verifyOperation(ingestOperationGuidTenant0, OK);

        // Check results for tenant 0

        // Query DSLs
        final RequestResponseOK<JsonNode> ingestedUnitsTenant0 = selectUnitsByOpi(
            0,
            ingestOperationGuidTenant0,
            accessInternalClient
        );
        final RequestResponseOK<JsonNode> ingestedObjectGroupsTenant0 = selectGotsByOpi(
            0,
            ingestOperationGuidTenant0,
            accessInternalClient
        );

        assertThat(ingestedUnitsTenant0.getResults()).hasSize(8);
        assertThat(ingestedObjectGroupsTenant0.getResults()).hasSize(3);
        assertThat(getLogbookOperationByDsl(0, ingestOperationGuidTenant0)).isNotNull();

        // By Id
        String unitIdTenant0 = ingestedUnitsTenant0.getFirstResult().get(VitamFieldsHelper.id()).asText();
        String objectGroupIdTenant0 = ingestedUnitsTenant0.getFirstResult().get(VitamFieldsHelper.id()).asText();

        assertThat(selectUnitById(0, unitIdTenant0)).isNotNull();
        assertThat(selectObjectGroupById(0, objectGroupIdTenant0)).isNotNull();
        assertThat(getLogbookOperationById(0, ingestOperationGuidTenant0)).isNotNull();

        // Check results from another tenant 1

        // By DSL
        assertThat(selectUnitsByOpi(1, ingestOperationGuidTenant0, accessInternalClient).getResults()).isEmpty();
        assertThat(selectGotsByOpi(1, ingestOperationGuidTenant0, accessInternalClient).getResults()).isEmpty();
        assertThat(getLogbookOperationByDsl(1, ingestOperationGuidTenant0)).isNull();
        // By Id
        // FIXME : Missing tenant filtering (#6732 & #6734)
        // assertThat(selectUnitById(1, unitIdTenant0)).isNull();
        // assertThat(selectObjectGroupById(1, objectGroupIdTenant0)).isNull();
        // assertThat(getLogbookOperationById(1, ingestOperationGuidTenant0)).isNull();
    }

    private JsonNode selectUnitById(int tenantId, String unitId)
        throws AccessUnauthorizedException, AccessInternalClientServerException, AccessInternalClientNotFoundException, InvalidParseOperationException {
        prepareVitamSession(tenantId);

        try (final AccessInternalClient accessInternalClient = AccessInternalClientFactory.getInstance().getClient()) {
            RequestResponseOK<JsonNode> requestResponseOK = (RequestResponseOK<
                    JsonNode
                >) accessInternalClient.selectUnitbyId(new Select().getFinalSelectById(), unitId);
            assertThat(requestResponseOK.getResults().size()).isLessThanOrEqualTo(1);
            return (requestResponseOK.getResults().isEmpty()) ? null : requestResponseOK.getFirstResult();
        }
    }

    private JsonNode selectObjectGroupById(int tenantId, String unitId)
        throws AccessUnauthorizedException, AccessInternalClientServerException, AccessInternalClientNotFoundException, InvalidParseOperationException {
        prepareVitamSession(tenantId);

        try (final AccessInternalClient accessInternalClient = AccessInternalClientFactory.getInstance().getClient()) {
            RequestResponseOK<JsonNode> requestResponseOK = (RequestResponseOK<
                    JsonNode
                >) accessInternalClient.selectUnitbyId(new Select().getFinalSelectById(), unitId);
            assertThat(requestResponseOK.getResults().size()).isLessThanOrEqualTo(1);
            return (requestResponseOK.getResults().isEmpty()) ? null : requestResponseOK.getFirstResult();
        }
    }

    private String searchAccessContract(Integer tenantId, String fieldName, String value)
        throws InvalidParseOperationException, AdminManagementClientServerException, InvalidCreateOperationException {
        prepareVitamSession(tenantId);
        try (AdminManagementClient adminManagementClient = AdminManagementClientFactory.getInstance().getClient()) {
            Select select = new Select();
            Query query = QueryHelper.eq(fieldName, value);
            select.setQuery(query);
            JsonNode queryDsl = select.getFinalSelect();
            RequestResponseOK<AccessContractModel> accessContracts = (RequestResponseOK<
                    AccessContractModel
                >) adminManagementClient.findAccessContracts(queryDsl);
            assertThat(accessContracts.getResults().size()).isLessThanOrEqualTo(1);
            return accessContracts.getResults().isEmpty() ? null : accessContracts.getFirstResult().getId();
        }
    }

    private void prepareAccessContract(Integer tenantId, String identifier)
        throws InvalidParseOperationException, AdminManagementClientServerException {
        prepareVitamSession(tenantId);
        try (AdminManagementClient adminManagementClient = AdminManagementClientFactory.getInstance().getClient()) {
            AccessContractModel accessContractModel = new AccessContractModel();
            accessContractModel.setEveryOriginatingAgency(true);
            accessContractModel.setName(identifier);
            accessContractModel.setStatus(ActivationStatus.ACTIVE);
            accessContractModel.setIdentifier(identifier);
            adminManagementClient.importAccessContracts(Collections.singletonList(accessContractModel));
        }
    }

    private InputStream readStoredReport(String filename)
        throws StorageServerClientException, StorageNotFoundException, StorageUnavailableDataFromAsyncOfferClientException {
        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
            Response reportResponse = null;

            try {
                reportResponse = storageClient.getContainerAsync(
                    VitamConfiguration.getDefaultStrategy(),
                    filename,
                    DataCategory.REPORT,
                    AccessLogUtils.getNoLogAccessLog()
                );

                assertThat(reportResponse.getStatus()).isEqualTo(Status.OK.getStatusCode());

                return new VitamAsyncInputStream(reportResponse);
            } catch (
                RuntimeException
                | StorageServerClientException
                | StorageNotFoundException
                | StorageUnavailableDataFromAsyncOfferClientException e
            ) {
                StreamUtils.consumeAnyEntityAndClose(reportResponse);
                throw e;
            }
        }
    }

    private RequestResponseOK<JsonNode> selectGotsByOpi(
        int tenantId,
        String ingestOperationGuid,
        AccessInternalClient accessInternalClient
    )
        throws InvalidCreateOperationException, InvalidParseOperationException, AccessInternalClientServerException, AccessInternalClientNotFoundException, AccessUnauthorizedException, BadRequestException {
        prepareVitamSession(tenantId);
        SelectMultiQuery checkGotDslRequest = new SelectMultiQuery();
        checkGotDslRequest.addQueries(QueryHelper.eq(VitamFieldsHelper.initialOperation(), ingestOperationGuid));

        return (RequestResponseOK<JsonNode>) accessInternalClient.selectObjects(checkGotDslRequest.getFinalSelect());
    }

    private RequestResponseOK<JsonNode> selectUnitsByOpi(
        int tenantId,
        String ingestOperationGuid,
        AccessInternalClient accessInternalClient
    )
        throws InvalidCreateOperationException, InvalidParseOperationException, AccessInternalClientServerException, AccessInternalClientNotFoundException, AccessUnauthorizedException, BadRequestException {
        prepareVitamSession(tenantId);
        SelectMultiQuery checkDslRequest = new SelectMultiQuery();
        checkDslRequest.addQueries(QueryHelper.eq(VitamFieldsHelper.initialOperation(), ingestOperationGuid));

        return (RequestResponseOK<JsonNode>) accessInternalClient.selectUnits(checkDslRequest.getFinalSelect());
    }

    private void awaitForWorkflowTerminationWithStatus(String operationGuid, StatusCode expectedStatusCode) {
        waitOperation(operationGuid);

        ProcessWorkflow processWorkflow = ProcessMonitoringImpl.getInstance()
            .findOneProcessWorkflow(operationGuid, tenantId);

        try {
            assertNotNull(processWorkflow);
            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
            assertEquals(expectedStatusCode, processWorkflow.getStatus());
        } catch (AssertionError e) {
            tryLogLogbookOperation(operationGuid);
            tryLogATR(operationGuid);
            throw e;
        }
    }

    private JsonNode getLogbookOperationById(int tenantId, String operationId)
        throws LogbookClientException, InvalidParseOperationException {
        prepareVitamSession(tenantId);
        try (LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient()) {
            RequestResponseOK<JsonNode> requestResponseOK = RequestResponseOK.getFromJsonNode(
                logbookClient.selectOperationById(operationId)
            );
            assertThat(requestResponseOK.getResults().size()).isLessThanOrEqualTo(1);
            return requestResponseOK.getResults().isEmpty() ? null : requestResponseOK.getFirstResult();
        } catch (LogbookClientNotFoundException e) {
            return null;
        }
    }

    private JsonNode getLogbookOperationByDsl(int tenantId, String operationId)
        throws LogbookClientException, InvalidParseOperationException, InvalidCreateOperationException {
        prepareVitamSession(tenantId);
        try (LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient()) {
            Select select = new Select();
            select.setQuery(QueryHelper.eq(VitamFieldsHelper.id(), operationId));
            RequestResponseOK<JsonNode> requestResponseOK = RequestResponseOK.getFromJsonNode(
                logbookClient.selectOperation(select.getFinalSelect())
            );
            assertThat(requestResponseOK.getResults().size()).isLessThanOrEqualTo(1);
            return requestResponseOK.getResults().isEmpty() ? null : requestResponseOK.getFirstResult();
        } catch (LogbookClientNotFoundException e) {
            return null;
        }
    }

    private void tryLogLogbookOperation(String operationId) {
        try (LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient()) {
            JsonNode logbookOperation = logbookClient.selectOperationById(operationId);
            LOGGER.error("Operation logbook status : \n" + JsonHandler.prettyPrint(logbookOperation) + "\n\n\n");
        } catch (Exception e) {
            LOGGER.error("Could not retrieve logbook operation for operation " + operationId, e);
        }
    }

    private void tryLogATR(String operationId) {
        try (InputStream atr = readStoredReport(operationId + XML)) {
            LOGGER.error("Operation ATR : \n" + IOUtils.toString(atr, StandardCharsets.UTF_8) + "\n\n\n");
        } catch (StorageNotFoundException ignored) {} catch (Exception e) {
            LOGGER.error("Could not retrieve ATR for operation " + operationId, e);
        }
    }
}
