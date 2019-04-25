package fr.gouv.vitam.metadata.management.integration.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.util.JSON;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.client.configuration.ClientConfigurationImpl;
import fr.gouv.vitam.common.database.api.impl.VitamElasticsearchRepository;
import fr.gouv.vitam.common.database.api.impl.VitamMongoRepository;
import fr.gouv.vitam.common.database.offset.OffsetRepository;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.database.server.mongodb.SimpleMongoDBAccess;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.server.LogbookConfiguration;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.api.config.MetaDataConfiguration;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.core.database.collections.MetadataDocument;
import fr.gouv.vitam.metadata.core.database.collections.MongoDbAccessMetadataImpl;
import fr.gouv.vitam.metadata.core.database.collections.ObjectGroup;
import fr.gouv.vitam.metadata.core.database.collections.Unit;
import fr.gouv.vitam.metadata.core.database.collections.VitamRepositoryFactory;
import fr.gouv.vitam.metadata.core.model.ReconstructionRequestItem;
import fr.gouv.vitam.metadata.core.model.ReconstructionResponseItem;
import fr.gouv.vitam.metadata.rest.MetadataMain;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageAlreadyExistsClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.Order;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.storage.engine.server.rest.StorageConfiguration;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.storage.offers.common.database.OfferLogDatabaseService;
import fr.gouv.vitam.storage.offers.common.database.OfferSequenceDatabaseService;
import fr.gouv.vitam.storage.offers.common.rest.DefaultOfferMain;
import fr.gouv.vitam.storage.offers.common.rest.OfferConfiguration;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import net.javacrumbs.jsonunit.JsonAssert;
import net.javacrumbs.jsonunit.core.Option;
import okhttp3.OkHttpClient;
import org.apache.commons.io.FileUtils;
import org.assertj.core.util.Lists;
import org.bson.Document;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.mongodb.client.model.Indexes.ascending;
import static com.mongodb.client.model.Sorts.orderBy;
import static fr.gouv.vitam.common.PropertiesUtils.readYaml;
import static fr.gouv.vitam.common.PropertiesUtils.writeYaml;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the reconstruction of metadata. <br/>
 */
public class MetadataManagementIT {

    /**
     * Vitam logger.
     */
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(MetadataManagementIT.class);

    private static final String unit_with_graph_0 = "integration-metadata-management/data/unit_with_graph_0.json";
    private static final String unit_with_graph_1 = "integration-metadata-management/data/unit_with_graph_1.json";
    private static final String unit_with_graph_2 = "integration-metadata-management/data/unit_with_graph_2.json";
    private static final String unit_with_graph_2_v2 = "integration-metadata-management/data/unit_with_graph_2_v2.json";
    private static final String unit_with_graph_3 = "integration-metadata-management/data/unit_with_graph_3.json";


    private static final String unit_with_graph_0_guid = "aeaqaaaaaahmtusqabktwaldc34sm5yaaaaq";
    private static final String unit_with_graph_1_guid = "aeaqaaaaaahmtusqabktwaldc34sm6aaaada";
    private static final String unit_with_graph_2_guid = "aeaqaaaaaahlm6sdabkeoaldc3hq6kyaaaca";
    private static final String unit_with_graph_3_guid = "aeaqaaaaaahlm6sdabkeoaldc3hq6laaaaaq";
    private static final String unit_with_graph_4_guid = "aeaqaaaaaahlm6sdabkeaaldc3hq6laaaaaq";


    private static final String got_with_graph_0 = "integration-metadata-management/data/got_0.json";
    private static final String got_with_graph_1 = "integration-metadata-management/data/got_1.json";
    private static final String got_with_graph_2 = "integration-metadata-management/data/got_2.json";
    private static final String got_with_graph_2_v2 = "integration-metadata-management/data/got_2_v2.json";


    private static final String got_with_graph_0_guid = "aebaaaaaaahlm6sdabzmoalc4pzqrpqaaaaq";
    private static final String got_with_graph_1_guid = "aebaaaaaaahlm6sdabzmoalc4pzqrtqaaaaq";
    private static final String got_with_graph_2_guid = "aebaaaaaaahlm6sdabzmoalc4pzxztyaaaaq";


    private static final String unit_graph_zip_file_name = "1970-01-01-00-00-00-000_2018-04-20-17-00-01-444";
    private static final String unit_graph_zip_file =
        "integration-metadata-management/data/1970-01-01-00-00-00-000_2018-04-20-17-00-01-444";

    private static final String got_graph_zip_file_name = "1970-01-01-00-00-00-000_2018-04-20-17-00-01-471";
    private static final String got_graph_zip_file =
        "integration-metadata-management/data/1970-01-01-00-00-00-000_2018-04-20-17-00-01-471";

    private static final String reclassification_units = "integration-metadata-management/reclassification/units.json";
    private static final String reclassification_units_lfc =
        "integration-metadata-management/reclassification/units_lfc.json";
    private static final String reclassification_gots = "integration-metadata-management/reclassification/gots.json";
    private static final String dsl_attach_arbre_ingest_ko =
        "integration-metadata-management/reclassification/dsl_attach_arbre_ingest_ko.json";
    private static final String dsl_attach_arbre_plan_ko =
        "integration-metadata-management/reclassification/dsl_attach_arbre_plan_ko.json";
    private static final String dsl_attach_plan_ingest_ko =
        "integration-metadata-management/reclassification/dsl_attach_plan_ingest_ko.json";
    private static final String dsl_cycle_ko =
        "integration-metadata-management/reclassification/dsl_cycle_ko.json";
    private static final String dsl_attach_detach_ok_parallel =
        "integration-metadata-management/reclassification/dsl_attach_detach_ok_parallel.json";
    private static final String dsl_attach_detach_ok =
        "integration-metadata-management/reclassification/dsl_attach_detach_ok.json";

    private static final String DEFAULT_OFFER_CONF = "integration-metadata-management/storage-default-offer.conf";
    private static final String LOGBOOK_CONF = "integration-metadata-management/logbook.conf";
    private static final String STORAGE_CONF = "integration-metadata-management/storage-engine.conf";
    private static final String WORKSPACE_CONF = "integration-metadata-management/workspace.conf";
    private static final String METADATA_CONF = "integration-metadata-management/metadata.conf";


    private static final String OFFER_FOLDER = "offer";
    private static final int TENANT_0 = 0;
    private static final int TENANT_1 = 1;

    private static final int PORT_SERVICE_WORKSPACE = 8094;
    private static final int PORT_SERVICE_METADATA = 8098;
    private static final int PORT_SERVICE_LOGBOOK = 8099;
    private static final int PORT_SERVICE_STORAGE = 8193;
    private static final int PORT_SERVICE_OFFER = 8194;

    private static final String WORKSPACE_URL = "http://localhost:" + PORT_SERVICE_WORKSPACE;
    private static final String METADATA_URL = "http://localhost:" + PORT_SERVICE_METADATA;
    public static final String JSON_EXTENTION = ".json";

    private static WorkspaceMain workspaceMain;
    private static WorkspaceClient workspaceClient;

    private static LogbookMain logbookMain;

    private static StorageMain storageMain;
    private static StorageClient storageClient;

    private static DefaultOfferMain defaultOfferMain;

    private static MetadataMain metadataMain;
    private static OffsetRepository offsetRepository;

    private MetadataManagementResource metadataManagementResource;

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    @ClassRule
    public static MongoRule mongoRule =
        new MongoRule(MongoDbAccessMetadataImpl.getMongoClientOptions(), "Vitam-Test",
            MetadataCollections.UNIT.getName(), MetadataCollections.OBJECTGROUP.getName(),
            OfferSequenceDatabaseService.OFFER_SEQUENCE_COLLECTION, OffsetRepository.COLLECTION_NAME,
            OfferLogDatabaseService.OFFER_LOG_COLLECTION_NAME);

    @ClassRule
    public static ElasticsearchRule elasticsearchRule =
        new ElasticsearchRule(org.assertj.core.util.Files.newTemporaryFolder(), MetadataCollections.UNIT.getName(),
            MetadataCollections.OBJECTGROUP.getName());


    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @BeforeClass
    public static void setupBeforeClass() throws Exception {

        File vitamTempFolder = tempFolder.newFolder();
        SystemPropertyUtil.set("vitam.tmp.folder", vitamTempFolder.getAbsolutePath());

        // launch ES
        final List<ElasticsearchNode> nodesEs = new ArrayList<>();
        nodesEs.add(new ElasticsearchNode("localhost", ElasticsearchRule.getTcpPort()));

        StorageClientFactory.changeMode(new ClientConfigurationImpl("localhost", PORT_SERVICE_STORAGE));
        storageClient = StorageClientFactory.getInstance().getClient();

        // launch workspace
        SystemPropertyUtil.set(WorkspaceMain.PARAMETER_JETTY_SERVER_PORT,
            Integer.toString(PORT_SERVICE_WORKSPACE));

        final File workspaceConfigFile = PropertiesUtils.findFile(WORKSPACE_CONF);

        fr.gouv.vitam.common.storage.StorageConfiguration workspaceConfiguration =
            PropertiesUtils.readYaml(workspaceConfigFile, fr.gouv.vitam.common.storage.StorageConfiguration.class);
        workspaceConfiguration.setStoragePath(vitamTempFolder.getAbsolutePath());

        writeYaml(workspaceConfigFile, workspaceConfiguration);

        workspaceMain = new WorkspaceMain(workspaceConfigFile.getAbsolutePath());
        workspaceMain.start();
        SystemPropertyUtil.clear(WorkspaceMain.PARAMETER_JETTY_SERVER_PORT);
        WorkspaceClientFactory.changeMode(WORKSPACE_URL);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();

        // launch logbook
        SystemPropertyUtil
            .set(LogbookMain.PARAMETER_JETTY_SERVER_PORT, Integer.toString(PORT_SERVICE_LOGBOOK));
        final File logbookConfigFile = PropertiesUtils.findFile(LOGBOOK_CONF);
        final LogbookConfiguration logbookConfiguration =
            PropertiesUtils.readYaml(logbookConfigFile, LogbookConfiguration.class);
        logbookConfiguration.setElasticsearchNodes(nodesEs);
        logbookConfiguration.getMongoDbNodes().get(0).setDbPort(MongoRule.getDataBasePort());
        logbookConfiguration.setWorkspaceUrl("http://localhost:" + PORT_SERVICE_WORKSPACE);

        PropertiesUtils.writeYaml(logbookConfigFile, logbookConfiguration);

        logbookMain = new LogbookMain(logbookConfigFile.getAbsolutePath());
        logbookMain.start();
        SystemPropertyUtil.clear(LogbookMain.PARAMETER_JETTY_SERVER_PORT);
        LogbookOperationsClientFactory.changeMode(new ClientConfigurationImpl("localhost", PORT_SERVICE_LOGBOOK));
        LogbookLifeCyclesClientFactory.changeMode(new ClientConfigurationImpl("localhost", PORT_SERVICE_LOGBOOK));

        // launch metadata
        SystemPropertyUtil.set(MetadataMain.PARAMETER_JETTY_SERVER_PORT, Integer.toString(PORT_SERVICE_METADATA));
        final File metadataConfig = PropertiesUtils.findFile(METADATA_CONF);
        final MetaDataConfiguration realMetadataConfig =
            PropertiesUtils.readYaml(metadataConfig, MetaDataConfiguration.class);
        realMetadataConfig.getMongoDbNodes().get(0).setDbPort(MongoRule.getDataBasePort());
        realMetadataConfig.setDbName(mongoRule.getMongoDatabase().getName());
        realMetadataConfig.setElasticsearchNodes(nodesEs);
        realMetadataConfig.setClusterName(elasticsearchRule.getClusterName());

        PropertiesUtils.writeYaml(metadataConfig, realMetadataConfig);

        metadataMain = new MetadataMain(metadataConfig.getAbsolutePath());
        metadataMain.start();
        SystemPropertyUtil.clear(MetadataMain.PARAMETER_JETTY_SERVER_PORT);
        MetaDataClientFactory.changeMode(new ClientConfigurationImpl("localhost", PORT_SERVICE_METADATA));

        // launch offer
        SystemPropertyUtil
            .set(DefaultOfferMain.PARAMETER_JETTY_SERVER_PORT, Integer.toString(PORT_SERVICE_OFFER));
        final File offerConfig = PropertiesUtils.findFile(DEFAULT_OFFER_CONF);
        final OfferConfiguration offerConfiguration = PropertiesUtils.readYaml(offerConfig, OfferConfiguration.class);
        List<MongoDbNode> mongoDbNodes = offerConfiguration.getMongoDbNodes();
        mongoDbNodes.get(0).setDbPort(MongoRule.getDataBasePort());
        offerConfiguration.setMongoDbNodes(mongoDbNodes);
        PropertiesUtils.writeYaml(offerConfig, offerConfiguration);

        defaultOfferMain = new DefaultOfferMain(offerConfig.getAbsolutePath());
        defaultOfferMain.start();
        SystemPropertyUtil.clear(DefaultOfferMain.PARAMETER_JETTY_SERVER_PORT);

        // launch storage engine
        File storageConfigurationFile = PropertiesUtils.findFile(STORAGE_CONF);
        final StorageConfiguration serverConfiguration = readYaml(storageConfigurationFile, StorageConfiguration.class);
        serverConfiguration
            .setUrlWorkspace("http://localhost:" + PORT_SERVICE_WORKSPACE);

        serverConfiguration.setZippingDirecorty(tempFolder.newFolder().getAbsolutePath());
        serverConfiguration.setLoggingDirectory(tempFolder.newFolder().getAbsolutePath());

        writeYaml(storageConfigurationFile, serverConfiguration);

        SystemPropertyUtil
            .set(StorageMain.PARAMETER_JETTY_SERVER_PORT, Integer.toString(PORT_SERVICE_STORAGE));
        storageMain = new StorageMain(STORAGE_CONF);
        storageMain.start();
        SystemPropertyUtil.clear(StorageMain.PARAMETER_JETTY_SERVER_PORT);

        MongoDbAccess mongoDbAccess = new SimpleMongoDBAccess(mongoRule.getMongoClient(), "Vitam-Test");
        offsetRepository = new OffsetRepository(mongoDbAccess);
    }


    @AfterClass
    public static void afterClass() throws Exception {

        if (workspaceClient != null) {
            workspaceClient.close();
        }
        if (workspaceMain != null) {
            workspaceMain.stop();
        }
        File offerFolder = new File(OFFER_FOLDER);
        if (offerFolder.exists()) {
            try {
                // if clean offer delete did not work
                FileUtils.cleanDirectory(offerFolder);
                FileUtils.deleteDirectory(offerFolder);
            } catch (Exception e) {
                LOGGER.error("ERROR: Exception has been thrown when cleanning offer:", e);
            }
        }
        if (storageClient != null) {
            storageClient.close();
        }
        if (defaultOfferMain != null) {
            defaultOfferMain.stop();
        }
        if (storageMain != null) {
            storageMain.stop();
        }
        if (metadataMain != null) {
            metadataMain.stop();
        }
        if (logbookMain != null) {
            logbookMain.stop();
        }
        elasticsearchRule.afterClass();
    }

    @Before
    public void setup() {
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(TENANT_0));
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);

        // reconstruct service interface - replace non existing client
        // uncomment timeouts for debug mode
        final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .readTimeout(600, TimeUnit.SECONDS)
            .connectTimeout(600, TimeUnit.SECONDS)
            .build();
        Retrofit retrofit =
            new Retrofit.Builder().client(okHttpClient).baseUrl(METADATA_URL)
                .addConverterFactory(JacksonConverterFactory.create()).build();
        metadataManagementResource = retrofit.create(MetadataManagementResource.class);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        storageClient = StorageClientFactory.getInstance().getClient();


        Map<MetadataCollections, VitamMongoRepository> mongoRepository = new HashMap<>();
        mongoRepository.put(MetadataCollections.UNIT,
            new VitamMongoRepository(mongoRule.getMongoCollection(MetadataCollections.UNIT.name())));
        mongoRepository.put(MetadataCollections.OBJECTGROUP,
            new VitamMongoRepository(mongoRule.getMongoCollection(MetadataCollections.OBJECTGROUP.name())));

        Map<MetadataCollections, VitamElasticsearchRepository> esRepository = new HashMap<>();
        esRepository.put(MetadataCollections.UNIT,
            new VitamElasticsearchRepository(elasticsearchRule.getClient(),
                MetadataCollections.UNIT.name().toLowerCase(), true));
        esRepository.put(MetadataCollections.OBJECTGROUP,
            new VitamElasticsearchRepository(elasticsearchRule.getClient(),
                MetadataCollections.OBJECTGROUP.name().toLowerCase(), true));

        VitamConfiguration.setAdminTenant(1);

        // ReconstructionService delete all unit and GOT without _tenant and older than 1 month.
        VitamConfiguration.setDeleteIncompleteReconstructedUnitDelay(Integer.MAX_VALUE);
    }

    @After
    public void tearDown() {
        // clean offers
        cleanOffers();

        mongoRule.handleAfter();
        elasticsearchRule.handleAfter();
    }

    @Test
    @RunWithCustomExecutor
    public void testReconstructionOfMetadataThenOK() throws Exception {
        // Clean offerLog
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        MetaDataClient metadataClient = MetaDataClientFactory.getInstance().getClient();
        LogbookLifeCyclesClient lifecycleClient = LogbookLifeCyclesClientFactory.getInstance().getClient();

        // 0. prepare data
        String container = GUIDFactory.newGUID().getId();
        workspaceClient.createContainer(container);
        storeFileToOffer(container, unit_with_graph_0_guid, JSON_EXTENTION, unit_with_graph_0, DataCategory.UNIT);
        storeFileToOffer(container, unit_with_graph_1_guid, JSON_EXTENTION, unit_with_graph_1, DataCategory.UNIT);
        storeFileToOffer(container, unit_with_graph_2_guid, JSON_EXTENTION, unit_with_graph_2, DataCategory.UNIT);

        storeFileToOffer(container, got_with_graph_0_guid, JSON_EXTENTION, got_with_graph_0, DataCategory.OBJECTGROUP);
        storeFileToOffer(container, got_with_graph_1_guid, JSON_EXTENTION, got_with_graph_1, DataCategory.OBJECTGROUP);
        storeFileToOffer(container, got_with_graph_2_guid, JSON_EXTENTION, got_with_graph_2, DataCategory.OBJECTGROUP);

        checkOfferLogSize(DataCategory.UNIT, 3);

        // 1. Reconstruct units
        reconstruction(DataCategory.UNIT, MetadataCollections.UNIT, 2, TENANT_0, 2L);
        ensureUnitExists(metadataClient, lifecycleClient, unit_with_graph_0_guid, 0, 5);
        ensureUnitExists(metadataClient, lifecycleClient, unit_with_graph_1_guid, 0, 4);

        // 2. Reconstruct object groups
        reconstruction(DataCategory.OBJECTGROUP, MetadataCollections.OBJECTGROUP, 2, TENANT_0, 5L);

        ensureGotExists(metadataClient, lifecycleClient, got_with_graph_0_guid, 0, 8);
        ensureGotExists(metadataClient, lifecycleClient, got_with_graph_1_guid, 0, 8);

        // 3. Rest offset and relaunch reconstruct for unit and got
        offsetRepository.createOrUpdateOffset(TENANT_0, MetadataCollections.UNIT.getName(), 0L);
        offsetRepository.createOrUpdateOffset(TENANT_0, MetadataCollections.OBJECTGROUP.getName(), 0L);

        reconstruction(DataCategory.UNIT, MetadataCollections.UNIT, 2, TENANT_0, 2L);
        reconstruction(DataCategory.OBJECTGROUP, MetadataCollections.OBJECTGROUP, 2, TENANT_0, 5L);

        // 4. Reconstruct next for unit and got
        reconstruction(DataCategory.UNIT, MetadataCollections.UNIT, 2, TENANT_0, 3L);
        reconstruction(DataCategory.OBJECTGROUP, MetadataCollections.OBJECTGROUP, 2, TENANT_0, 6L);

        ensureUnitExists(metadataClient, lifecycleClient, unit_with_graph_2_guid, 0, 5);
        ensureGotExists(metadataClient, lifecycleClient, got_with_graph_2_guid, 0, 8);

        // 5. Reconstruct nothing for unit and got
        reconstruction(DataCategory.UNIT, MetadataCollections.UNIT, 2, TENANT_0, 3L);
        reconstruction(DataCategory.OBJECTGROUP, MetadataCollections.OBJECTGROUP, 2, TENANT_0, 6L);

        // 6. Reconstruct on unused tenants
        offsetRepository.createOrUpdateOffset(TENANT_1, MetadataCollections.UNIT.getName(), 0L);
        offsetRepository.createOrUpdateOffset(TENANT_1, MetadataCollections.OBJECTGROUP.getName(), 0L);

        reconstruction(DataCategory.UNIT, MetadataCollections.UNIT, 2, TENANT_1, 0L);
        reconstruction(DataCategory.OBJECTGROUP, MetadataCollections.OBJECTGROUP, 2, TENANT_1, 0L);

        // 7. Reset Unit offset and reconstruct on unit and another invalid collection
        offsetRepository.createOrUpdateOffset(TENANT_0, MetadataCollections.UNIT.getName(), 0L);

        Response<List<ReconstructionResponseItem>> response;

        List<ReconstructionRequestItem> reconstructionItems = Arrays.asList(
            new ReconstructionRequestItem().setCollection(DataCategory.UNIT.name()).setTenant(TENANT_0).setLimit(2),
            new ReconstructionRequestItem().setCollection(DataCategory.MANIFEST.name()).setTenant(TENANT_0).setLimit(2)
        );

        response = metadataManagementResource.reconstructCollection(reconstructionItems).execute();
        assertThat(response.code()).isEqualTo(200);
        assertThat(response.body()).hasSize(2);
        assertThat(response.body().get(0).getStatus()).isEqualTo(StatusCode.OK);
        assertThat(response.body().get(1).getStatus()).isEqualTo(StatusCode.KO);
        assertThat(offsetRepository.findOffsetBy(TENANT_0, MetadataCollections.UNIT.getName())).isEqualTo(2L);
        assertThat(offsetRepository.findOffsetBy(TENANT_0, DataCategory.MANIFEST.name())).isEqualTo(0L);

        // Purge reconstructed documents with only graph data and older than a configured delay
        Call<Void> purge = metadataManagementResource.purgeReconstructedDocumentsWithGraphOnlyDataUNIT();
        assertThat(purge.execute().isSuccessful()).isTrue();

        purge = metadataManagementResource.purgeReconstructedDocumentsWithGraphOnlyDataOBJECTGROUP();
        assertThat(purge.execute().isSuccessful()).isTrue();

        purge = metadataManagementResource.purgeReconstructedDocumentsWithGraphOnlyDataUNITOBJECTGROUP();
        assertThat(purge.execute().isSuccessful()).isTrue();
    }

    private void checkOfferLogSize(DataCategory dataCategory, int size) throws StorageServerClientException {
        RequestResponse<OfferLog> offerLogResponse1 =
            storageClient.getOfferLogs("default", dataCategory, null, Integer.MAX_VALUE, Order.ASC);
        assertThat(offerLogResponse1).isNotNull();
        assertThat(offerLogResponse1.isOk()).isTrue();
        assertThat(((RequestResponseOK<OfferLog>) offerLogResponse1).getResults()).hasSize(size);
    }

    private void reconstruction(DataCategory dataCategory,
        MetadataCollections metadataCollection, int limit, int tenant,
        long expectedOffset) throws IOException {
        List<ReconstructionRequestItem> unitReconstructionItems = singletonList(
            new ReconstructionRequestItem()
                .setCollection(dataCategory.name())
                .setLimit(limit)
                .setTenant(tenant));
        Response<List<ReconstructionResponseItem>> response =
            metadataManagementResource.reconstructCollection(unitReconstructionItems).execute();
        assertThat(response.code()).isEqualTo(200);
        assertThat(response.body()).hasSize(1);
        assertThat(response.body().get(0).getTenant()).isEqualTo(tenant);
        assertThat(response.body().get(0).getStatus()).isEqualTo(StatusCode.OK);
        assertThat(offsetRepository.findOffsetBy(tenant, metadataCollection.getName())).isEqualTo(expectedOffset);
    }

    private void ensureUnitExists(MetaDataClient metadataClient,
        LogbookLifeCyclesClient lifecycleClient, String unitId, int metadataVersion, int lfcVersion)
        throws VitamClientException, LogbookClientException, InvalidParseOperationException {

        RequestResponse<JsonNode> metadataResponse = metadataClient.getUnitByIdRaw(unitId);
        assertThat(((RequestResponseOK<JsonNode>) metadataResponse).getResults().size()).isEqualTo(1);
        assertThat(((RequestResponseOK<JsonNode>) metadataResponse).getFirstResult().get("_id").asText())
            .isEqualTo(unitId);
        assertThat(((RequestResponseOK<JsonNode>) metadataResponse).getFirstResult().get("_v").asInt())
            .isEqualTo(metadataVersion);

        JsonNode lifecycleResponse = lifecycleClient.getRawUnitLifeCycleById(unitId);
        assertThat(lifecycleResponse).isNotNull();
        assertThat(lifecycleResponse.get("_id").asText()).isEqualTo(unitId);
        assertThat(lifecycleResponse.get("_v").asInt()).isEqualTo(lfcVersion);
    }

    private void ensureGotExists(MetaDataClient metadataClient,
        LogbookLifeCyclesClient lifecycleClient, String gotId, int metadataVersion, int lfcVersion)
        throws VitamClientException, LogbookClientException, InvalidParseOperationException {

        RequestResponse<JsonNode> metadataResponse = metadataClient.getObjectGroupByIdRaw(gotId);
        assertThat(((RequestResponseOK<JsonNode>) metadataResponse).getResults().size()).isEqualTo(1);
        assertThat(((RequestResponseOK<JsonNode>) metadataResponse).getFirstResult().get("_id").asText())
            .isEqualTo(gotId);
        assertThat(((RequestResponseOK<JsonNode>) metadataResponse).getFirstResult().get("_v").asInt())
            .isEqualTo(metadataVersion);

        JsonNode lifecycleResponse = lifecycleClient.getRawObjectGroupLifeCycleById(gotId);
        assertThat(lifecycleResponse).isNotNull();
        assertThat(lifecycleResponse.get("_id").asText()).isEqualTo(gotId);
        assertThat(lifecycleResponse.get("_v").asInt()).isEqualTo(lfcVersion);
    }

    @Test
    @RunWithCustomExecutor
    public void testStoreUnitGraphThenStoreThenOK() throws Exception {
        VitamConfiguration.setStoreGraphElementsPerFile(5);

        LocalDateTime dateTime =
            LocalDateTime.now().minus(VitamConfiguration.getStoreGraphOverlapDelay(), ChronoUnit.SECONDS);

        String dateInMongo = LocalDateUtil.getFormattedDateForMongo(dateTime);
        List<Document> documents = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            documents.add(Document.parse("{\"_id\": " + i + ", \"_glpd\": \"" + dateInMongo + "\" }"));
        }

        MetadataCollections.UNIT.getCollection().insertMany(documents);

        assertThat(MetadataCollections.UNIT.getCollection().count()).isEqualTo(10);

        Map<MetadataCollections, Integer> ok = metadataManagementResource.tryStoreGraph().execute().body();

        assertThat(ok.get(MetadataCollections.UNIT)).isEqualTo(10);


        dateTime = LocalDateTime.now().minus(VitamConfiguration.getStoreGraphOverlapDelay(), ChronoUnit.SECONDS);
        dateInMongo = LocalDateUtil.getFormattedDateForMongo(dateTime);
        documents = new ArrayList<>();
        for (int i = 10; i < 15; i++) {
            documents.add(Document.parse("{\"_id\": " + i + ", \"_glpd\": \"" + dateInMongo + "\" }"));
        }

        MetadataCollections.UNIT.getCollection().insertMany(documents);

        assertThat(MetadataCollections.UNIT.getCollection().count()).isEqualTo(15);

        ok = metadataManagementResource.tryStoreGraph().execute().body();
        assertThat(ok.get(MetadataCollections.UNIT)).isEqualTo(5);

        ok = metadataManagementResource.tryStoreGraph().execute().body();
        assertThat(ok.get(MetadataCollections.UNIT)).isEqualTo(0);
    }

    @Test
    @RunWithCustomExecutor
    public void testReconstruction_unit_then_unitgraph_in_two_phase_query_OK() throws Exception {
        // Clean offerLog
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        MetaDataClient metadataClient = MetaDataClientFactory.getInstance().getClient();

        // 0. prepare data
        String container = GUIDFactory.newGUID().getId();
        workspaceClient.createContainer(container);
        storeFileToOffer(container, unit_with_graph_0_guid, JSON_EXTENTION, unit_with_graph_0, DataCategory.UNIT);
        storeFileToOffer(container, unit_with_graph_1_guid, JSON_EXTENTION, unit_with_graph_1, DataCategory.UNIT);
        storeFileToOffer(container, unit_with_graph_2_guid, JSON_EXTENTION, unit_with_graph_2, DataCategory.UNIT);
        storeFileToOffer(container, unit_with_graph_3_guid, JSON_EXTENTION, unit_with_graph_3, DataCategory.UNIT);


        RequestResponse<OfferLog> offerLogResponse =
            storageClient.getOfferLogs("default", DataCategory.UNIT, 0L, 10, Order.ASC);
        assertThat(offerLogResponse).isNotNull();
        assertThat(offerLogResponse.isOk()).isTrue();
        assertThat(((RequestResponseOK<OfferLog>) offerLogResponse).getResults().size()).isEqualTo(4);
        // 1. reconstruct unit
        List<ReconstructionRequestItem> reconstructionItems = new ArrayList<>();
        ReconstructionRequestItem reconstructionItem = new ReconstructionRequestItem();
        reconstructionItem.setCollection(DataCategory.UNIT.name());
        reconstructionItem.setLimit(4);
        reconstructionItem.setTenant(TENANT_0);
        reconstructionItems.add(reconstructionItem);

        Response<List<ReconstructionResponseItem>> response =
            metadataManagementResource.reconstructCollection(reconstructionItems).execute();
        assertThat(response.code()).isEqualTo(200);
        assertThat(response.body().size()).isEqualTo(1);
        assertThat(offsetRepository.findOffsetBy(TENANT_0, MetadataCollections.UNIT.getName())).isEqualTo(4L);
        assertThat(response.body().get(0).getTenant()).isEqualTo(0);
        assertThat(response.body().get(0).getStatus()).isEqualTo(StatusCode.OK);

        RequestResponse<JsonNode> metadataResponse = metadataClient.getUnitByIdRaw(unit_with_graph_1_guid);
        assertThat(metadataResponse.isOk()).isTrue();
        assertThat(((RequestResponseOK<JsonNode>) metadataResponse).getResults().size()).isEqualTo(1);
        assertThat(((RequestResponseOK<JsonNode>) metadataResponse).getFirstResult().get("_id").asText())
            .isEqualTo(unit_with_graph_1_guid);
        assertThat(((RequestResponseOK<JsonNode>) metadataResponse).getFirstResult().get("_v").asInt()).isEqualTo(0);
        assertThat(((RequestResponseOK<JsonNode>) metadataResponse).getFirstResult().get("_sps").toString())
            .contains("SHOULD_BE_REMOVED_AFTER_RECONTRUCT");

        ///////////////////////////////////////////////////////////////
        //        Reconstruction Graph
        ///////////////////////////////////////////////////////////////
        VitamThreadUtils.getVitamSession().setTenantId(1);
        storeFileToOffer(container, unit_graph_zip_file_name, "", unit_graph_zip_file, DataCategory.UNIT_GRAPH);


        offerLogResponse =
            storageClient.getOfferLogs("default", DataCategory.UNIT_GRAPH, 0L, 10, Order.ASC);
        assertThat(offerLogResponse).isNotNull();
        assertThat(offerLogResponse.isOk()).isTrue();
        assertThat(((RequestResponseOK<OfferLog>) offerLogResponse).getResults().size()).isEqualTo(1);


        // 2. reconstruct object group
        reconstructionItems = new ArrayList<>();
        reconstructionItem = new ReconstructionRequestItem();
        reconstructionItem.setCollection(DataCategory.UNIT_GRAPH.name());
        reconstructionItem.setLimit(2);
        reconstructionItem.setTenant(TENANT_0);
        reconstructionItems.add(reconstructionItem);

        response = metadataManagementResource.reconstructCollection(reconstructionItems).execute();
        assertThat(response.code()).isEqualTo(200);
        assertThat(response.body().size()).isEqualTo(1);

        // Check Unit
        String expectedUnitJson = "integration-metadata-management/expected/units_1.json";
        assertDataSetEqualsExpectedFile(MetadataCollections.UNIT.getCollection(), expectedUnitJson);
        // Check Got
        String expectedGotJson = "integration-metadata-management/expected/gots_1.json";
        assertDataSetEqualsExpectedFile(MetadataCollections.OBJECTGROUP.getCollection(), expectedGotJson);


        assertThat(offsetRepository.findOffsetBy(1, DataCategory.UNIT_GRAPH.name()))
            .isEqualTo(5L);
        assertThat(response.body().get(0).getStatus()).isEqualTo(StatusCode.OK);
    }


    @Test
    @RunWithCustomExecutor
    public void testReconstruction_unit_then_got_then_unitgraph_then_gotgraph_in_one_phase_query_OK() throws Exception {
        // Clean offerLog
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        // 0. prepare data
        String container = GUIDFactory.newGUID().getId();
        workspaceClient.createContainer(container);
        storeFileToOffer(container, unit_with_graph_0_guid, JSON_EXTENTION, unit_with_graph_0, DataCategory.UNIT);
        storeFileToOffer(container, unit_with_graph_1_guid, JSON_EXTENTION, unit_with_graph_1, DataCategory.UNIT);
        storeFileToOffer(container, unit_with_graph_2_guid, JSON_EXTENTION, unit_with_graph_2, DataCategory.UNIT);
        storeFileToOffer(container, unit_with_graph_3_guid, JSON_EXTENTION, unit_with_graph_3, DataCategory.UNIT);

        storeFileToOffer(container, got_with_graph_0_guid, JSON_EXTENTION, got_with_graph_0, DataCategory.OBJECTGROUP);
        storeFileToOffer(container, got_with_graph_1_guid, JSON_EXTENTION, got_with_graph_1, DataCategory.OBJECTGROUP);
        storeFileToOffer(container, got_with_graph_2_guid, JSON_EXTENTION, got_with_graph_2, DataCategory.OBJECTGROUP);


        RequestResponse<OfferLog> offerLogResponse =
            storageClient.getOfferLogs("default", DataCategory.UNIT, 0L, 10, Order.ASC);
        assertThat(offerLogResponse).isNotNull();
        assertThat(offerLogResponse.isOk()).isTrue();
        assertThat(((RequestResponseOK<OfferLog>) offerLogResponse).getResults().size()).isEqualTo(4);

        offerLogResponse =
            storageClient.getOfferLogs("default", DataCategory.OBJECTGROUP, 0L, 10, Order.ASC);
        assertThat(offerLogResponse).isNotNull();
        assertThat(offerLogResponse.isOk()).isTrue();
        assertThat(((RequestResponseOK<OfferLog>) offerLogResponse).getResults().size()).isEqualTo(3);


        VitamThreadUtils.getVitamSession().setTenantId(TENANT_1);
        storeFileToOffer(container, unit_graph_zip_file_name, "", unit_graph_zip_file, DataCategory.UNIT_GRAPH);
        storeFileToOffer(container, got_graph_zip_file_name, "", got_graph_zip_file, DataCategory.OBJECTGROUP_GRAPH);

        offerLogResponse =
            storageClient.getOfferLogs("default", DataCategory.UNIT_GRAPH, 0L, 10, Order.ASC);
        assertThat(offerLogResponse).isNotNull();
        assertThat(offerLogResponse.isOk()).isTrue();
        assertThat(((RequestResponseOK<OfferLog>) offerLogResponse).getResults().size()).isEqualTo(1);

        offerLogResponse =
            storageClient.getOfferLogs("default", DataCategory.OBJECTGROUP_GRAPH, 0L, 10, Order.ASC);
        assertThat(offerLogResponse).isNotNull();
        assertThat(offerLogResponse.isOk()).isTrue();
        assertThat(((RequestResponseOK<OfferLog>) offerLogResponse).getResults().size()).isEqualTo(1);


        // Reconstruct ALL (Unit, ObjectGroup, Unit_Graph, ObjectGroup_Graph)
        List<ReconstructionRequestItem> reconstructionItems = new ArrayList<>();
        ReconstructionRequestItem reconstructionItem = new ReconstructionRequestItem();
        reconstructionItem.setCollection(DataCategory.UNIT.name());
        reconstructionItem.setLimit(4);
        reconstructionItem.setTenant(TENANT_0);
        reconstructionItems.add(reconstructionItem);

        reconstructionItem = new ReconstructionRequestItem();
        reconstructionItem.setCollection(DataCategory.OBJECTGROUP.name());
        reconstructionItem.setLimit(3);
        reconstructionItem.setTenant(TENANT_0);
        reconstructionItems.add(reconstructionItem);

        reconstructionItem = new ReconstructionRequestItem();
        reconstructionItem.setCollection(DataCategory.UNIT_GRAPH.name());
        reconstructionItem.setLimit(3);
        reconstructionItem.setTenant(TENANT_1);
        reconstructionItems.add(reconstructionItem);

        reconstructionItem = new ReconstructionRequestItem();
        reconstructionItem.setCollection(DataCategory.OBJECTGROUP_GRAPH.name());
        reconstructionItem.setLimit(3);
        reconstructionItem.setTenant(TENANT_1);
        reconstructionItems.add(reconstructionItem);


        Response<List<ReconstructionResponseItem>> response =
            metadataManagementResource.reconstructCollection(reconstructionItems).execute();
        assertThat(response.code()).isEqualTo(200);
        assertThat(response.body().size()).isEqualTo(4);
        assertThat(offsetRepository.findOffsetBy(TENANT_0, MetadataCollections.UNIT.getName())).isEqualTo(4L);
        assertThat(offsetRepository.findOffsetBy(TENANT_0, MetadataCollections.OBJECTGROUP.getName())).isEqualTo(7L);
        assertThat(offsetRepository.findOffsetBy(TENANT_1, DataCategory.UNIT_GRAPH.name())).isEqualTo(8L);
        assertThat(offsetRepository.findOffsetBy(TENANT_1, DataCategory.OBJECTGROUP_GRAPH.name())).isEqualTo(9L);
        assertThat(response.body().get(0).getTenant()).isEqualTo(0);
        assertThat(response.body().get(0).getCollection()).isEqualTo(DataCategory.UNIT.name());
        assertThat(response.body().get(0).getStatus()).isEqualTo(StatusCode.OK);

        assertThat(response.body().get(1).getTenant()).isEqualTo(0);
        assertThat(response.body().get(1).getCollection()).isEqualTo(DataCategory.OBJECTGROUP.name());
        assertThat(response.body().get(1).getStatus()).isEqualTo(StatusCode.OK);

        assertThat(response.body().get(2).getTenant()).isEqualTo(1);
        assertThat(response.body().get(2).getCollection()).isEqualTo(DataCategory.UNIT_GRAPH.name());
        assertThat(response.body().get(2).getStatus()).isEqualTo(StatusCode.OK);

        assertThat(response.body().get(3).getTenant()).isEqualTo(1);
        assertThat(response.body().get(3).getCollection()).isEqualTo(DataCategory.OBJECTGROUP_GRAPH.name());
        assertThat(response.body().get(3).getStatus()).isEqualTo(StatusCode.OK);


        // Check Unit
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        // Check Unit
        String expectedUnitJson = "integration-metadata-management/expected/units_2.json";
        assertDataSetEqualsExpectedFile(MetadataCollections.UNIT.getCollection(), expectedUnitJson);
        // Check Got
        String expectedGotJson = "integration-metadata-management/expected/gots_2.json";
        assertDataSetEqualsExpectedFile(MetadataCollections.OBJECTGROUP.getCollection(), expectedGotJson);
    }



    @Test
    @RunWithCustomExecutor
    public void testReconstruction_unitgraph_then_gotgraph_then_unit_then_got_in_two_phase_query_OK() throws Exception {
        // Clean offerLog
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        MetaDataClient metadataClient = MetaDataClientFactory.getInstance().getClient();
        // 0. prepare data
        String container = GUIDFactory.newGUID().getId();
        workspaceClient.createContainer(container);

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_1);
        storeFileToOffer(container, unit_graph_zip_file_name, "", unit_graph_zip_file, DataCategory.UNIT_GRAPH);
        storeFileToOffer(container, got_graph_zip_file_name, "", got_graph_zip_file, DataCategory.OBJECTGROUP_GRAPH);

        RequestResponse<OfferLog> offerLogResponse =
            storageClient.getOfferLogs("default", DataCategory.UNIT_GRAPH, 0L, 10, Order.ASC);
        assertThat(offerLogResponse).isNotNull();
        assertThat(offerLogResponse.isOk()).isTrue();
        assertThat(((RequestResponseOK<OfferLog>) offerLogResponse).getResults().size()).isEqualTo(1);

        offerLogResponse =
            storageClient.getOfferLogs("default", DataCategory.OBJECTGROUP_GRAPH, 0L, 10, Order.ASC);
        assertThat(offerLogResponse).isNotNull();
        assertThat(offerLogResponse.isOk()).isTrue();
        assertThat(((RequestResponseOK<OfferLog>) offerLogResponse).getResults().size()).isEqualTo(1);

        // Reconstruct Unit_Graph, ObjectGroup_Graph
        List<ReconstructionRequestItem> reconstructionItems = new ArrayList<>();
        ReconstructionRequestItem reconstructionItem = new ReconstructionRequestItem();
        reconstructionItem.setCollection(DataCategory.UNIT_GRAPH.name());
        reconstructionItem.setLimit(3);
        reconstructionItem.setTenant(TENANT_1);
        reconstructionItems.add(reconstructionItem);

        reconstructionItem = new ReconstructionRequestItem();
        reconstructionItem.setCollection(DataCategory.OBJECTGROUP_GRAPH.name());
        reconstructionItem.setLimit(3);
        reconstructionItem.setTenant(TENANT_1);
        reconstructionItems.add(reconstructionItem);

        Response<List<ReconstructionResponseItem>> response =
            metadataManagementResource.reconstructCollection(reconstructionItems).execute();
        assertThat(response.code()).isEqualTo(200);
        assertThat(response.body().size()).isEqualTo(2);
        assertThat(offsetRepository.findOffsetBy(TENANT_1, DataCategory.UNIT_GRAPH.name())).isEqualTo(1L);
        assertThat(offsetRepository.findOffsetBy(TENANT_1, DataCategory.OBJECTGROUP_GRAPH.name())).isEqualTo(2L);
        assertThat(response.body().get(0).getTenant()).isEqualTo(1);
        assertThat(response.body().get(0).getCollection()).isEqualTo(DataCategory.UNIT_GRAPH.name());
        assertThat(response.body().get(0).getStatus()).isEqualTo(StatusCode.OK);

        assertThat(response.body().get(1).getTenant()).isEqualTo(1);
        assertThat(response.body().get(1).getCollection()).isEqualTo(DataCategory.OBJECTGROUP_GRAPH.name());
        assertThat(response.body().get(1).getStatus()).isEqualTo(StatusCode.OK);

        // Check Unit
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        String expectedUnitJson = "integration-metadata-management/expected/units_3.json";
        assertDataSetEqualsExpectedFile(MetadataCollections.UNIT.getCollection(), expectedUnitJson);
        // Check Got
        String expectedGotJson = "integration-metadata-management/expected/gots_3.json";
        assertDataSetEqualsExpectedFile(MetadataCollections.OBJECTGROUP.getCollection(), expectedGotJson);

        storeFileToOffer(container, unit_with_graph_0_guid, JSON_EXTENTION, unit_with_graph_0, DataCategory.UNIT);
        storeFileToOffer(container, unit_with_graph_1_guid, JSON_EXTENTION, unit_with_graph_1, DataCategory.UNIT);
        storeFileToOffer(container, unit_with_graph_2_guid, JSON_EXTENTION, unit_with_graph_2, DataCategory.UNIT);
        storeFileToOffer(container, unit_with_graph_3_guid, JSON_EXTENTION, unit_with_graph_3, DataCategory.UNIT);

        storeFileToOffer(container, got_with_graph_0_guid, JSON_EXTENTION, got_with_graph_0, DataCategory.OBJECTGROUP);
        storeFileToOffer(container, got_with_graph_1_guid, JSON_EXTENTION, got_with_graph_1, DataCategory.OBJECTGROUP);
        storeFileToOffer(container, got_with_graph_2_guid, JSON_EXTENTION, got_with_graph_2, DataCategory.OBJECTGROUP);


        offerLogResponse =
            storageClient.getOfferLogs("default", DataCategory.UNIT, 0L, 10, Order.ASC);
        assertThat(offerLogResponse).isNotNull();
        assertThat(offerLogResponse.isOk()).isTrue();
        assertThat(((RequestResponseOK<OfferLog>) offerLogResponse).getResults().size()).isEqualTo(4);

        offerLogResponse =
            storageClient.getOfferLogs("default", DataCategory.OBJECTGROUP, 0L, 10, Order.ASC);
        assertThat(offerLogResponse).isNotNull();
        assertThat(offerLogResponse.isOk()).isTrue();
        assertThat(((RequestResponseOK<OfferLog>) offerLogResponse).getResults().size()).isEqualTo(3);


        // Reconstruct Unit, ObjectGroup
        reconstructionItems = new ArrayList<>();
        reconstructionItem = new ReconstructionRequestItem();
        reconstructionItem.setCollection(DataCategory.UNIT.name());
        reconstructionItem.setLimit(4);
        reconstructionItem.setTenant(TENANT_0);
        reconstructionItems.add(reconstructionItem);

        reconstructionItem = new ReconstructionRequestItem();
        reconstructionItem.setCollection(DataCategory.OBJECTGROUP.name());
        reconstructionItem.setLimit(4);
        reconstructionItem.setTenant(TENANT_0);
        reconstructionItems.add(reconstructionItem);



        response =
            metadataManagementResource.reconstructCollection(reconstructionItems).execute();
        assertThat(response.code()).isEqualTo(200);
        assertThat(response.body().size()).isEqualTo(2);
        assertThat(offsetRepository.findOffsetBy(TENANT_0, MetadataCollections.UNIT.getName())).isEqualTo(6L);
        assertThat(offsetRepository.findOffsetBy(TENANT_0, MetadataCollections.OBJECTGROUP.getName())).isEqualTo(9L);
        assertThat(response.body().get(0).getTenant()).isEqualTo(0);
        assertThat(response.body().get(0).getCollection()).isEqualTo(DataCategory.UNIT.name());
        assertThat(response.body().get(0).getStatus()).isEqualTo(StatusCode.OK);

        assertThat(response.body().get(1).getTenant()).isEqualTo(0);
        assertThat(response.body().get(1).getCollection()).isEqualTo(DataCategory.OBJECTGROUP.name());
        assertThat(response.body().get(1).getStatus()).isEqualTo(StatusCode.OK);



        // Check Unit
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        expectedUnitJson = "integration-metadata-management/expected/units_4.json";
        assertDataSetEqualsExpectedFile(MetadataCollections.UNIT.getCollection(), expectedUnitJson);
        // Check Got
        expectedGotJson = "integration-metadata-management/expected/gots_4.json";
        assertDataSetEqualsExpectedFile(MetadataCollections.OBJECTGROUP.getCollection(), expectedGotJson);
    }

    @Test
    @RunWithCustomExecutor
    public void testComputeUnitAndObjectGroupGraphForTrivialCases() throws Exception {
        Document au_without_parents = new Document(Unit.ID, "au_without_parents")
            .append(Unit.ORIGINATING_AGENCY, "OA4").append(Unit.ORIGINATING_AGENCIES,
                Lists.newArrayList("OA4", "OA1", "OA2"));
        Document got_without_unit = new Document(ObjectGroup.ID, "got_without_unit")
            .append(ObjectGroup.ORIGINATING_AGENCY, "OA2").append(ObjectGroup.ORIGINATING_AGENCIES,
                Lists.newArrayList("OA4", "OA1", "OA2"));

        MetadataCollections.UNIT.getCollection().insertOne(au_without_parents);
        MetadataCollections.OBJECTGROUP.getCollection().insertOne(got_without_unit);

        Map<MetadataCollections, Integer> body = metadataManagementResource.computeGraph().execute().body();
        assertThat(body.get(MetadataCollections.UNIT)).isEqualTo(1);
        assertThat(body.get(MetadataCollections.OBJECTGROUP)).isEqualTo(1);

        Document computedUnit =
            (Document) MetadataCollections.UNIT.getCollection().find(new Document("_id", "au_without_parents"))
                .iterator().next();
        assertThat(computedUnit.get(Unit.ORIGINATING_AGENCY, String.class)).isEqualTo("OA4");
        assertThat(computedUnit.get(Unit.ORIGINATING_AGENCIES, List.class)).hasSize(1).contains("OA4");

        Document computedGot =
            (Document) MetadataCollections.OBJECTGROUP.getCollection().find(new Document("_id", "got_without_unit"))
                .iterator().next();
        assertThat(computedGot.get(Unit.ORIGINATING_AGENCY, String.class)).isEqualTo("OA2");
        assertThat(computedGot.get(Unit.ORIGINATING_AGENCIES, List.class)).hasSize(1).contains("OA2");


        // Purge mongo
        mongoRule.handleAfter();
        // Re-insert
        MetadataCollections.UNIT.getCollection().insertOne(au_without_parents);
        Document got_with_unit_up = got_without_unit
            .append(ObjectGroup.UP, Lists.newArrayList("au_without_parents"));
        got_with_unit_up.put(ObjectGroup.ID, "got_with_unit_up");
        MetadataCollections.OBJECTGROUP.getCollection().insertOne(got_with_unit_up);

        body = metadataManagementResource.computeGraph().execute().body();
        assertThat(body.get(MetadataCollections.UNIT)).isEqualTo(1);
        assertThat(body.get(MetadataCollections.OBJECTGROUP)).isEqualTo(1);

        computedUnit =
            (Document) MetadataCollections.UNIT.getCollection().find(new Document("_id", "au_without_parents"))
                .iterator().next();
        assertThat(computedUnit.get(Unit.ORIGINATING_AGENCY, String.class)).isEqualTo("OA4");
        assertThat(computedUnit.get(Unit.ORIGINATING_AGENCIES, List.class)).hasSize(1).contains("OA4");

        computedGot =
            (Document) MetadataCollections.OBJECTGROUP.getCollection().find(new Document("_id", "got_with_unit_up"))
                .iterator().next();
        assertThat(computedGot.get(Unit.ORIGINATING_AGENCY, String.class)).isEqualTo("OA2");
        assertThat(computedGot.get(Unit.ORIGINATING_AGENCIES, List.class)).hasSize(2).contains("OA4", "OA2");
    }


    // See computegraph.png for more information
    private void initializeDbWithUnitAndObjectGroupData() throws DatabaseException {
        // Create units with or without graph data
        Document au1 = new Document(Unit.ID, "AU_1")
            .append(Unit.MAXDEPTH, 1)
            .append(Unit.TENANT_ID, 1)
            .append(Unit.UP, Lists.newArrayList())
            .append("fakefake", "fakefake")
            .append(Unit.GRAPH_LAST_PERSISTED_DATE, LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()))
            .append(Unit.ORIGINATING_AGENCY, "OA1").append(Unit.ORIGINATING_AGENCIES, Lists.newArrayList("OA1"));

        Document au2 = new Document(Unit.ID, "AU_2")
            .append(Unit.MAXDEPTH, 1)
            .append(Unit.TENANT_ID, 1)
            .append(Unit.UP, Lists.newArrayList())
            .append("fakefake", "fakefake")
            .append(Unit.GRAPH_LAST_PERSISTED_DATE, LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()))
            .append(Unit.ORIGINATING_AGENCY, "OA2").append(Unit.ORIGINATING_AGENCIES, Lists.newArrayList("OA2"));

        Document au3 =
            new Document(Unit.ID, "AU_3")
                .append(Unit.MAXDEPTH, 1)
                .append(Unit.TENANT_ID, 1)
                .append(Unit.OG, "GOT_8")
                .append(Unit.UP, Lists.newArrayList("AU_1"))
                .append(Unit.GRAPH_LAST_PERSISTED_DATE, LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()))
                .append(Unit.ORIGINATING_AGENCY, "OA1").append(Unit.ORIGINATING_AGENCIES,
                Lists.newArrayList("OA1"));

        Document au4 = new Document(Unit.ID, "AU_4")
            .append(Unit.MAXDEPTH, 1)
            .append(Unit.TENANT_ID, 1)
            .append(Unit.OG, "GOT_4")
            .append(Unit.UP, Lists.newArrayList("AU_1", "AU_2"))
            .append("fakefake", "fakefake")
            .append(Unit.GRAPH_LAST_PERSISTED_DATE, LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()))
            .append(Unit.ORIGINATING_AGENCY, "OA4").append(Unit.ORIGINATING_AGENCIES,
                Lists.newArrayList("OA4", "OA1", "OA2"));

        Document au5 = new Document(Unit.ID, "AU_5")
            .append(Unit.MAXDEPTH, 1)
            .append(Unit.TENANT_ID, 1)
            .append(Unit.UP, Lists.newArrayList("AU_2"))
            .append("fakefake", "fakefake")
            .append(Unit.GRAPH_LAST_PERSISTED_DATE, LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()))
            .append(Unit.ORIGINATING_AGENCY, "OA2").append(Unit.ORIGINATING_AGENCIES, Lists.newArrayList("OA2"));

        Document au6 = new Document(Unit.ID, "AU_6")
            .append(Unit.MAXDEPTH, 1)
            .append(Unit.TENANT_ID, 1)
            .append(Unit.OG, "GOT_6")
            .append(Unit.UP, Lists.newArrayList("AU_2", "AU_5"))
            .append(Unit.ORIGINATING_AGENCY, "OA2")
            .append(Unit.GRAPH_LAST_PERSISTED_DATE, LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()))
            .append(Unit.ORIGINATING_AGENCIES, Lists.newArrayList("OA2"));

        Document au7 =
            new Document(Unit.ID, "AU_7")
                .append(Unit.MAXDEPTH, 1)
                .append(Unit.TENANT_ID, 1)
                .append(Unit.OG, "GOT_8")
                .append(Unit.UP, Lists.newArrayList("AU_4"))
                .append(Unit.GRAPH_LAST_PERSISTED_DATE, LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()))
                .append(Unit.ORIGINATING_AGENCY, "OA4").append(Unit.ORIGINATING_AGENCIES,
                Lists.newArrayList("OA4", "OA1", "OA2"));

        Document au8 = new Document(Unit.ID, "AU_8")
            .append(Unit.MAXDEPTH, 1)
            .append(Unit.TENANT_ID, 1)
            .append(Unit.OG, "GOT_8")
            .append(Unit.UP, Lists.newArrayList("AU_6", "AU_4"))
            .append("fakefake", "fakefake")
            .append(Unit.GRAPH_LAST_PERSISTED_DATE, LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()))
            .append(Unit.ORIGINATING_AGENCY, "OA2").append(Unit.ORIGINATING_AGENCIES,
                Lists.newArrayList("OA4", "OA1", "OA2"));

        Document au9 = new Document(Unit.ID, "AU_9")
            .append(Unit.MAXDEPTH, 1)
            .append(Unit.TENANT_ID, 1)
            .append(Unit.OG, "GOT_9")
            .append(Unit.UP, Lists.newArrayList("AU_5", "AU_6"))
            .append("fakefake", "fakefake")
            .append(Unit.GRAPH_LAST_PERSISTED_DATE, LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()))
            .append(Unit.ORIGINATING_AGENCY, "OA2").append(Unit.ORIGINATING_AGENCIES, Lists.newArrayList("OA2"));

        Document au10 =
            new Document(Unit.ID, "AU_10")
                .append(Unit.MAXDEPTH, 1)
                .append(Unit.TENANT_ID, 1)
                .append(Unit.OG, "GOT_10")
                .append(Unit.UP, Lists.newArrayList("AU_8", "AU_9"))
                .append(Unit.GRAPH_LAST_PERSISTED_DATE, LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()))
                .append(Unit.ORIGINATING_AGENCY, "OA2").append(Unit.ORIGINATING_AGENCIES,
                Lists.newArrayList("OA4", "OA1", "OA2"));

        List<Document> units = Lists.newArrayList(au1, au2, au3, au4, au5, au6, au7, au8, au9, au10);
        VitamRepositoryFactory.getInstance().getVitamMongoRepository(MetadataCollections.UNIT).save(units);
        VitamRepositoryFactory.getInstance().getVitamESRepository(MetadataCollections.UNIT).save(units);

        ////////////////////////////////////////////////
        // Create corresponding ObjectGroup (only 4 GOT subject of compute graph as no _glpd defined on them)
        ///////////////////////////////////////////////
        Document got4 = new Document(ObjectGroup.ID, "GOT_4")
            .append(Unit.TENANT_ID, 1)
            .append(ObjectGroup.UP, Lists.newArrayList("AU_4"))
            .append(ObjectGroup.ORIGINATING_AGENCY, "OA4")
            .append(Unit.GRAPH_LAST_PERSISTED_DATE, LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()))
            .append(ObjectGroup.ORIGINATING_AGENCIES, Lists.newArrayList("OA4"));

        // Got 6 have Graph Data
        Document got6 = new Document(ObjectGroup.ID, "GOT_6")
            .append(Unit.TENANT_ID, 1)
            .append(ObjectGroup.GRAPH_LAST_PERSISTED_DATE, LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()))
            .append(ObjectGroup.UP, Lists.newArrayList("AU_6"))
            .append(ObjectGroup.ORIGINATING_AGENCY, "OA2").append(ObjectGroup.ORIGINATING_AGENCIES,
                Lists.newArrayList("OA4", "OA1", "OA2"));

        //Unit "AU_8", "AU_3", "AU_7" attached to got 8
        Document got8 = new Document(ObjectGroup.ID, "GOT_8")
            .append(Unit.TENANT_ID, 1)
            .append(ObjectGroup.UP, Lists.newArrayList("AU_8", "AU_3", "AU_7"))
            .append(Unit.GRAPH_LAST_PERSISTED_DATE, LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()))
            .append(ObjectGroup.ORIGINATING_AGENCY, "OA2");

        Document got9 = new Document(ObjectGroup.ID, "GOT_9")
            .append(Unit.TENANT_ID, 1)
            .append(ObjectGroup.UP, Lists.newArrayList("AU_9"))
            .append(Unit.GRAPH_LAST_PERSISTED_DATE, LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()))
            .append(ObjectGroup.ORIGINATING_AGENCY, "OA2");

        Document got10 =
            new Document(ObjectGroup.ID, "GOT_10")
                .append(Unit.TENANT_ID, 1)
                .append(ObjectGroup.UP, Lists.newArrayList("AU_10"))
                .append(Unit.GRAPH_LAST_PERSISTED_DATE, LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()))
                .append(ObjectGroup.ORIGINATING_AGENCY, "OA2");

        List<Document> gots = Lists.newArrayList(got4, got6, got8, got9, got10);
        VitamRepositoryFactory.getInstance().getVitamMongoRepository(MetadataCollections.OBJECTGROUP)
            .save(gots);
        VitamRepositoryFactory.getInstance().getVitamESRepository(MetadataCollections.OBJECTGROUP)
            .save(gots);
    }

    private void storeFileToOffer(String container, String fileName, String extension, String file, DataCategory type)
        throws ContentAddressableStorageServerException, IOException,
        StorageAlreadyExistsClientException, StorageNotFoundClientException,
        StorageServerClientException {
        final ObjectDescription objectDescription = new ObjectDescription();
        objectDescription.setWorkspaceContainerGUID(container);
        objectDescription.setObjectName(fileName + extension);
        objectDescription.setType(type);
        objectDescription.setWorkspaceObjectURI(fileName + extension);

        try (FileInputStream stream = new FileInputStream(PropertiesUtils.findFile(file))) {
            workspaceClient.putObject(container, fileName + extension, stream);
        }
        storageClient.storeFileFromWorkspace("default", type, fileName, objectDescription);
    }


    /**
     * Clean offers content.
     */
    private static void cleanOffers() {
        // ugly style but we don't have the digest herelo
        File directory = new File(OFFER_FOLDER);
        if (directory.exists()) {
            try {
                Files.walk(directory.toPath())
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            } catch (IOException | IllegalArgumentException e) {
                LOGGER.error("ERROR: Exception has been thrown when cleaning offers.", e);
            }
        }
    }

    public interface MetadataManagementResource {
        @POST("/metadata/v1/reconstruction")
        @Headers({
            "Accept: application/json",
            "Content-Type: application/json"
        })
        Call<List<ReconstructionResponseItem>> reconstructCollection(
            @Body List<ReconstructionRequestItem> reconstructionItems);



        @GET("/metadata/v1/storegraph")
        @Headers({
            "Accept: application/json",
            "Content-Type: application/json"
        })
        Call<Map<MetadataCollections, Integer>> tryStoreGraph();

        @GET("/metadata/v1/computegraph")
        @Headers({
            "Accept: application/json",
            "Content-Type: application/json"
        })
        Call<Map<MetadataCollections, Integer>> computeGraph();



        @DELETE("/metadata/v1/purgeGraphOnlyDocuments/UNIT")
        @Headers({
            "Accept: application/json",
            "Content-Type: application/json"
        })
        Call<Void> purgeReconstructedDocumentsWithGraphOnlyDataUNIT();

        @DELETE("/metadata/v1/purgeGraphOnlyDocuments/OBJECTGROUP")
        @Headers({
            "Accept: application/json",
            "Content-Type: application/json"
        })
        Call<Void> purgeReconstructedDocumentsWithGraphOnlyDataOBJECTGROUP();

        @DELETE("/metadata/v1/purgeGraphOnlyDocuments/UNIT_OBJECTGROUP")
        @Headers({
            "Accept: application/json",
            "Content-Type: application/json"
        })
        Call<Void> purgeReconstructedDocumentsWithGraphOnlyDataUNITOBJECTGROUP();
    }

    private <T> void assertDataSetEqualsExpectedFile(MongoCollection<T> mongoCollection, String expectedDataSetFile)
        throws InvalidParseOperationException, FileNotFoundException {

        ArrayNode unitDataSet = dumpDataSet(mongoCollection);

        String updatedUnitDataSet = JsonHandler.unprettyPrint(unitDataSet);
        String expectedUnitDataSet =
            JsonHandler.unprettyPrint(JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(
                expectedDataSetFile)));

        JsonAssert.assertJsonEquals(expectedUnitDataSet, updatedUnitDataSet,
            JsonAssert.when(Option.IGNORING_ARRAY_ORDER));
    }

    private <T> ArrayNode dumpDataSet(MongoCollection<T> mongoCollection) throws InvalidParseOperationException {

        ArrayNode dataSet = JsonHandler.createArrayNode();
        FindIterable<T> documents = mongoCollection.find()
            .sort(orderBy(ascending(MetadataDocument.ID)));

        for (T document : documents) {
            ObjectNode jsonUnit = (ObjectNode) JsonHandler.getFromString(JSON.serialize(document));

            // Replace _glpd with marker
            assertThat(jsonUnit.get(MetadataDocument.GRAPH_LAST_PERSISTED_DATE)).isNotNull();
            jsonUnit.put(MetadataDocument.GRAPH_LAST_PERSISTED_DATE, "#TIMESTAMP#");

            dataSet.add(jsonUnit);
        }

        return dataSet;
    }
}
