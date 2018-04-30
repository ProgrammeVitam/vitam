package fr.gouv.vitam.reconstruction.integration.test;

import static fr.gouv.vitam.common.PropertiesUtils.readYaml;
import static fr.gouv.vitam.common.PropertiesUtils.writeYaml;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.client.MongoCollection;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.client.configuration.ClientConfigurationImpl;
import fr.gouv.vitam.common.database.api.impl.VitamElasticsearchRepository;
import fr.gouv.vitam.common.database.api.impl.VitamMongoRepository;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.offset.OffsetRepository;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.database.server.mongodb.SimpleMongoDBAccess;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.guid.GUIDFactory;
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
import fr.gouv.vitam.logbook.common.server.LogbookConfiguration;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.api.config.MetaDataConfiguration;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.core.database.collections.MongoDbAccessMetadataImpl;
import fr.gouv.vitam.metadata.core.database.collections.VitamRepositoryFactory;
import fr.gouv.vitam.metadata.core.database.collections.VitamRepositoryProvider;
import fr.gouv.vitam.metadata.core.graph.StoreGraphException;
import fr.gouv.vitam.metadata.core.graph.StoreGraphService;
import fr.gouv.vitam.metadata.core.model.ReconstructionRequestItem;
import fr.gouv.vitam.metadata.core.model.ReconstructionResponseItem;
import fr.gouv.vitam.metadata.core.reconstruction.RestoreBackupService;
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
import okhttp3.OkHttpClient;
import org.apache.commons.io.FileUtils;
import org.assertj.core.util.Files;
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
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;

/**
 * Integration tests for the reconstruction of metadatas. <br/>
 */
public class ReconstructionMetadataIT {

    /**
     * Vitam logger.
     */
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ReconstructionMetadataIT.class);

    private static final String unit_with_graph_0 = "integration-reconstruction/data/unit_with_graph_0.json";
    private static final String unit_with_graph_1 = "integration-reconstruction/data/unit_with_graph_1.json";
    private static final String unit_with_graph_2 = "integration-reconstruction/data/unit_with_graph_2.json";
    private static final String unit_with_graph_3 = "integration-reconstruction/data/unit_with_graph_3.json";


    private static final String unit_with_graph_0_guid = "aeaqaaaaaahmtusqabktwaldc34sm5yaaaaq";
    private static final String unit_with_graph_1_guid = "aeaqaaaaaahmtusqabktwaldc34sm6aaaada";
    private static final String unit_with_graph_2_guid = "aeaqaaaaaahlm6sdabkeoaldc3hq6kyaaaca";
    private static final String unit_with_graph_3_guid = "aeaqaaaaaahlm6sdabkeoaldc3hq6laaaaaq";
    private static final String unit_with_graph_4_guid = "aeaqaaaaaahlm6sdabkeaaldc3hq6laaaaaq";


    private static final String got_with_graph_0 = "integration-reconstruction/data/got_0.json";
    private static final String got_with_graph_1 = "integration-reconstruction/data/got_1.json";
    private static final String got_with_graph_2 = "integration-reconstruction/data/got_2.json";


    private static final String got_with_graph_0_guid = "aebaaaaaaahlm6sdabzmoalc4pzqrpqaaaaq";
    private static final String got_with_graph_1_guid = "aebaaaaaaahlm6sdabzmoalc4pzqrtqaaaaq";
    private static final String got_with_graph_2_guid = "aebaaaaaaahlm6sdabzmoalc4pzxztyaaaaq";


    private static final String unit_graph_zip_file_name = "1970-01-01-00-00-00-000_2018-04-20-17-00-01-444";
    private static final String unit_graph_zip_file =
        "integration-reconstruction/data/1970-01-01-00-00-00-000_2018-04-20-17-00-01-444";

    private static final String got_graph_zip_file_name = "1970-01-01-00-00-00-000_2018-04-20-17-00-01-471";
    private static final String got_graph_zip_file =
        "integration-reconstruction/data/1970-01-01-00-00-00-000_2018-04-20-17-00-01-471";

    private static final String DEFAULT_OFFER_CONF = "integration-reconstruction/storage-default-offer.conf";
    private static final String LOGBOOK_CONF = "integration-reconstruction/logbook.conf";
    private static final String STORAGE_CONF = "integration-reconstruction/storage-engine.conf";
    private static final String WORKSPACE_CONF = "integration-reconstruction/workspace.conf";
    private static final String METADATA_CONF = "integration-reconstruction/metadata.conf";

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

    private MetadataReconstructionService reconstructionService;
    private VitamRepositoryProvider vitamRepositoryProvider;
    private StoreGraphService storeGraphService;
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
        new ElasticsearchRule(Files.newTemporaryFolder(), MetadataCollections.UNIT.getName(),
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
        reconstructionService = retrofit.create(MetadataReconstructionService.class);


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


        vitamRepositoryProvider = VitamRepositoryFactory.getInstance(mongoRepository, esRepository);
        RestoreBackupService restoreBackupService = new RestoreBackupService();
        storeGraphService = new StoreGraphService(
            VitamRepositoryFactory.getInstance(),
            restoreBackupService,
            WorkspaceClientFactory.getInstance(),
            StorageClientFactory.getInstance());

        VitamConfiguration.setAdminTenant(1);
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
        createInWorkspace(container, unit_with_graph_0_guid, JSON_EXTENTION, unit_with_graph_0, DataCategory.UNIT);
        createInWorkspace(container, unit_with_graph_1_guid, JSON_EXTENTION, unit_with_graph_1, DataCategory.UNIT);
        createInWorkspace(container, unit_with_graph_2_guid, JSON_EXTENTION, unit_with_graph_2, DataCategory.UNIT);

        createInWorkspace(container, got_with_graph_0_guid, JSON_EXTENTION, got_with_graph_0, DataCategory.OBJECTGROUP);
        createInWorkspace(container, got_with_graph_1_guid, JSON_EXTENTION, got_with_graph_1, DataCategory.OBJECTGROUP);
        createInWorkspace(container, got_with_graph_2_guid, JSON_EXTENTION, got_with_graph_2, DataCategory.OBJECTGROUP);

        RequestResponse<OfferLog> offerLogResponse1 =
            storageClient.getOfferLogs("default", DataCategory.UNIT, 0L, 10, Order.ASC);
        assertThat(offerLogResponse1).isNotNull();
        assertThat(offerLogResponse1.isOk()).isTrue();
        assertThat(((RequestResponseOK<OfferLog>) offerLogResponse1).getResults().size()).isEqualTo(3);

        List<ReconstructionRequestItem> reconstructionItems;
        ReconstructionRequestItem reconstructionItem2;
        Response<List<ReconstructionResponseItem>> response;
        RequestResponse<JsonNode> metadataResponse;
        JsonNode lifecycleResponse;

        // 1. Reconstruct unit
        reconstructionItems = new ArrayList<>();
        ReconstructionRequestItem reconstructionItem1 = new ReconstructionRequestItem();
        reconstructionItem1.setCollection(DataCategory.UNIT.name());
        reconstructionItem1.setLimit(2);
        reconstructionItem1.setTenant(TENANT_0);
        reconstructionItems.add(reconstructionItem1);
        response = reconstructionService.reconstructCollection("" + TENANT_0, reconstructionItems).execute();
        assertThat(response.code()).isEqualTo(200);
        assertThat(response.body().size()).isEqualTo(1);
        assertThat(offsetRepository.findOffsetBy(TENANT_0, MetadataCollections.UNIT.getName())).isEqualTo(2L);
        assertThat(response.body().get(0).getTenant()).isEqualTo(0);
        assertThat(response.body().get(0).getStatus()).isEqualTo(StatusCode.OK);

        metadataResponse = metadataClient.getUnitByIdRaw(unit_with_graph_0_guid);
        assertThat(metadataResponse.isOk());
        assertThat(((RequestResponseOK<JsonNode>) metadataResponse).getResults().size()).isEqualTo(1);
        assertThat(((RequestResponseOK<JsonNode>) metadataResponse).getFirstResult().get("_id").asText())
            .isEqualTo(unit_with_graph_0_guid);
        assertThat(((RequestResponseOK<JsonNode>) metadataResponse).getFirstResult().get("_v").asInt()).isEqualTo(0);

        lifecycleResponse =
            lifecycleClient.selectUnitLifeCycleById(unit_with_graph_0_guid,
                getSelectQueryProjectionSimple(unit_with_graph_0_guid).getFinalSelect());
        assertThat(lifecycleResponse).isNotNull();
        assertThat(lifecycleResponse.get("$results")).isNotNull();
        assertThat(lifecycleResponse.get("$results").size()).isEqualTo(1);
        assertThat(lifecycleResponse.get("$results").get(0).get("_id").asText()).isEqualTo(unit_with_graph_0_guid);
        assertThat(lifecycleResponse.get("$results").get(0).get("_v").asInt()).isEqualTo(5);

        metadataResponse = metadataClient.getUnitByIdRaw(unit_with_graph_1_guid);
        assertThat(metadataResponse.isOk());
        assertThat(((RequestResponseOK<JsonNode>) metadataResponse).getResults().size()).isEqualTo(1);
        assertThat(((RequestResponseOK<JsonNode>) metadataResponse).getFirstResult().get("_id").asText())
            .isEqualTo(unit_with_graph_1_guid);
        assertThat(((RequestResponseOK<JsonNode>) metadataResponse).getFirstResult().get("_v").asInt()).isEqualTo(0);

        lifecycleResponse =
            lifecycleClient.selectUnitLifeCycleById(unit_with_graph_1_guid,
                getSelectQueryProjectionSimple(unit_with_graph_1_guid).getFinalSelect());
        assertThat(lifecycleResponse).isNotNull();
        assertThat(lifecycleResponse.get("$results")).isNotNull();
        assertThat(lifecycleResponse.get("$results").size()).isEqualTo(1);
        assertThat(lifecycleResponse.get("$results").get(0).get("_id").asText()).isEqualTo(unit_with_graph_1_guid);
        assertThat(lifecycleResponse.get("$results").get(0).get("_v").asInt()).isEqualTo(4);


        // 2. Reconstruct object group
        reconstructionItems = new ArrayList<>();
        reconstructionItem2 = new ReconstructionRequestItem();
        reconstructionItem2.setCollection(DataCategory.OBJECTGROUP.name());
        reconstructionItem2.setLimit(2);
        reconstructionItem2.setTenant(TENANT_0);
        reconstructionItems.add(reconstructionItem2);

        response = reconstructionService.reconstructCollection("" + TENANT_0, reconstructionItems).execute();
        assertThat(response.code()).isEqualTo(200);
        assertThat(response.body().size()).isEqualTo(1);
        assertThat(offsetRepository.findOffsetBy(TENANT_0, MetadataCollections.OBJECTGROUP.getName())).isEqualTo(5L);
        assertThat(response.body().get(0).getStatus()).isEqualTo(StatusCode.OK);

        metadataResponse = metadataClient.getObjectGroupByIdRaw(got_with_graph_0_guid);
        assertThat(metadataResponse.isOk());
        assertThat(((RequestResponseOK<JsonNode>) metadataResponse).getResults().size()).isEqualTo(1);
        JsonNode first = ((RequestResponseOK<JsonNode>) metadataResponse).getFirstResult();
        assertThat(first.get("_id").asText()).isEqualTo(got_with_graph_0_guid);
        assertThat(first.get("_v").asInt()).isEqualTo(0);

        lifecycleResponse =
            lifecycleClient.selectObjectGroupLifeCycleById(got_with_graph_0_guid,
                getSelectQueryProjectionSimple(got_with_graph_0_guid).getFinalSelect());
        assertThat(lifecycleResponse).isNotNull();
        assertThat(lifecycleResponse.get("$results")).isNotNull();
        assertThat(lifecycleResponse.get("$results").size()).isEqualTo(1);
        assertThat(lifecycleResponse.get("$results").get(0).get("_id").asText()).isEqualTo(got_with_graph_0_guid);
        assertThat(lifecycleResponse.get("$results").get(0).get("_v").asInt()).isEqualTo(8);

        metadataResponse = metadataClient.getObjectGroupByIdRaw(got_with_graph_1_guid);
        assertThat(metadataResponse.isOk());
        assertThat(((RequestResponseOK<JsonNode>) metadataResponse).getResults().size()).isEqualTo(1);
        assertThat(((RequestResponseOK<JsonNode>) metadataResponse).getFirstResult().get("_id").asText())
            .isEqualTo(got_with_graph_1_guid);
        assertThat(((RequestResponseOK<JsonNode>) metadataResponse).getFirstResult().get("_v").asInt()).isEqualTo(0);

        lifecycleResponse =
            lifecycleClient.selectObjectGroupLifeCycleById(got_with_graph_1_guid,
                getSelectQueryProjectionSimple(got_with_graph_1_guid).getFinalSelect());
        assertThat(lifecycleResponse).isNotNull();
        assertThat(lifecycleResponse.get("$results")).isNotNull();
        assertThat(lifecycleResponse.get("$results").size()).isEqualTo(1);
        assertThat(lifecycleResponse.get("$results").get(0).get("_id").asText()).isEqualTo(got_with_graph_1_guid);
        assertThat(lifecycleResponse.get("$results").get(0).get("_v").asInt()).isEqualTo(8);

        // 3. Rest offset and relaunch reconstruct for unit and got
        offsetRepository.createOrUpdateOffset(TENANT_0, MetadataCollections.UNIT.getName(), 0L);
        offsetRepository.createOrUpdateOffset(TENANT_0, MetadataCollections.OBJECTGROUP.getName(), 0L);


        reconstructionItems = new ArrayList<>();
        reconstructionItems.add(reconstructionItem1);
        reconstructionItems.add(reconstructionItem2);
        response = reconstructionService.reconstructCollection("" + TENANT_0, reconstructionItems).execute();
        assertThat(response.code()).isEqualTo(200);
        assertThat(response.body().size()).isEqualTo(2);
        assertThat(offsetRepository.findOffsetBy(TENANT_0, MetadataCollections.UNIT.getName())).isEqualTo(2L);
        assertThat(response.body().get(0).getStatus()).isEqualTo(StatusCode.OK);
        assertThat(offsetRepository.findOffsetBy(TENANT_0, MetadataCollections.OBJECTGROUP.getName())).isEqualTo(5L);
        assertThat(response.body().get(1).getStatus()).isEqualTo(StatusCode.OK);

        // 4. Reconstruct next for unit and got
        reconstructionItems = new ArrayList<>();
        reconstructionItems.add(reconstructionItem1);
        reconstructionItems.add(reconstructionItem2);
        response = reconstructionService.reconstructCollection("" + TENANT_0, reconstructionItems).execute();
        assertThat(response.code()).isEqualTo(200);
        assertThat(response.body().size()).isEqualTo(2);
        assertThat(offsetRepository.findOffsetBy(TENANT_0, MetadataCollections.UNIT.getName())).isEqualTo(3L);
        assertThat(response.body().get(0).getStatus()).isEqualTo(StatusCode.OK);
        assertThat(offsetRepository.findOffsetBy(TENANT_0, MetadataCollections.OBJECTGROUP.getName())).isEqualTo(6L);
        assertThat(response.body().get(1).getStatus()).isEqualTo(StatusCode.OK);

        metadataResponse = metadataClient.getUnitByIdRaw(unit_with_graph_2_guid);
        assertThat(metadataResponse.isOk());
        assertThat(((RequestResponseOK<JsonNode>) metadataResponse).getResults().size()).isEqualTo(1);
        assertThat(((RequestResponseOK<JsonNode>) metadataResponse).getFirstResult().get("_id").asText())
            .isEqualTo(unit_with_graph_2_guid);
        assertThat(((RequestResponseOK<JsonNode>) metadataResponse).getFirstResult().get("_v").asInt()).isEqualTo(0);

        lifecycleResponse =
            lifecycleClient.selectUnitLifeCycleById(unit_with_graph_2_guid,
                getSelectQueryProjectionSimple(unit_with_graph_2_guid).getFinalSelect());
        assertThat(lifecycleResponse).isNotNull();
        assertThat(lifecycleResponse.get("$results")).isNotNull();
        assertThat(lifecycleResponse.get("$results").size()).isEqualTo(1);
        assertThat(lifecycleResponse.get("$results").get(0).get("_id").asText()).isEqualTo(unit_with_graph_2_guid);
        assertThat(lifecycleResponse.get("$results").get(0).get("_v").asInt()).isEqualTo(5);

        metadataResponse = metadataClient.getObjectGroupByIdRaw(got_with_graph_2_guid);
        assertThat(metadataResponse.isOk());
        assertThat(((RequestResponseOK<JsonNode>) metadataResponse).getResults().size()).isEqualTo(1);
        assertThat(((RequestResponseOK<JsonNode>) metadataResponse).getFirstResult().get("_id").asText())
            .isEqualTo(got_with_graph_2_guid);
        assertThat(((RequestResponseOK<JsonNode>) metadataResponse).getFirstResult().get("_v").asInt()).isEqualTo(0);

        lifecycleResponse =
            lifecycleClient.selectObjectGroupLifeCycleById(got_with_graph_2_guid,
                getSelectQueryProjectionSimple(got_with_graph_2_guid).getFinalSelect());
        assertThat(lifecycleResponse).isNotNull();
        assertThat(lifecycleResponse.get("$results")).isNotNull();
        assertThat(lifecycleResponse.get("$results").size()).isEqualTo(1);
        assertThat(lifecycleResponse.get("$results").get(0).get("_id").asText()).isEqualTo(got_with_graph_2_guid);
        assertThat(lifecycleResponse.get("$results").get(0).get("_v").asInt()).isEqualTo(8);

        // 5. Reconstruct nothing for unit and got
        reconstructionItems = new ArrayList<>();
        reconstructionItems.add(reconstructionItem1);
        reconstructionItems.add(reconstructionItem2);

        response = reconstructionService.reconstructCollection("" + TENANT_0, reconstructionItems).execute();
        assertThat(response.code()).isEqualTo(200);
        assertThat(response.body().size()).isEqualTo(2);
        assertThat(offsetRepository.findOffsetBy(TENANT_0, MetadataCollections.UNIT.getName())).isEqualTo(3L);
        assertThat(response.body().get(0).getStatus()).isEqualTo(StatusCode.OK);
        assertThat(offsetRepository.findOffsetBy(TENANT_0, MetadataCollections.OBJECTGROUP.getName())).isEqualTo(6L);
        assertThat(response.body().get(1).getStatus()).isEqualTo(StatusCode.OK);

        // 6. Reconstruct on unused tenants
        reconstructionItems = new ArrayList<>();
        offsetRepository.createOrUpdateOffset(TENANT_1, MetadataCollections.UNIT.getName(), 0L);

        reconstructionItem1.setTenant(TENANT_1);
        reconstructionItems.add(reconstructionItem1);
        offsetRepository.createOrUpdateOffset(TENANT_1, MetadataCollections.OBJECTGROUP.getName(), 0L);
        reconstructionItem2.setTenant(TENANT_1);
        reconstructionItems.add(reconstructionItem2);

        response = reconstructionService.reconstructCollection("" + TENANT_0, reconstructionItems).execute();
        assertThat(response.code()).isEqualTo(200);
        assertThat(response.body().size()).isEqualTo(2);
        assertThat(offsetRepository.findOffsetBy(TENANT_1, MetadataCollections.UNIT.getName())).isEqualTo(0L);
        assertThat(response.body().get(0).getStatus()).isEqualTo(StatusCode.OK);
        assertThat(offsetRepository.findOffsetBy(TENANT_1, MetadataCollections.OBJECTGROUP.getName())).isEqualTo(0L);
        assertThat(response.body().get(1).getStatus()).isEqualTo(StatusCode.OK);

        // 6. Reset Unit offset and reconstruct on unit and another invalid collection
        offsetRepository.createOrUpdateOffset(TENANT_0, MetadataCollections.UNIT.getName(), 0L);
        reconstructionItems = new ArrayList<>();

        reconstructionItem1.setTenant(TENANT_0);
        reconstructionItems.add(reconstructionItem1);
        reconstructionItem2.setCollection(DataCategory.MANIFEST.name());
        reconstructionItem2.setTenant(TENANT_0);
        reconstructionItems.add(reconstructionItem2);

        response = reconstructionService.reconstructCollection("" + TENANT_0, reconstructionItems).execute();
        assertThat(response.code()).isEqualTo(200);
        assertThat(response.body().size()).isEqualTo(2);
        assertThat(offsetRepository.findOffsetBy(TENANT_0, MetadataCollections.UNIT.getName())).isEqualTo(2L);
        assertThat(response.body().get(0).getStatus()).isEqualTo(StatusCode.OK);
        assertThat(offsetRepository.findOffsetBy(TENANT_0, DataCategory.MANIFEST.name())).isEqualTo(0L);

        assertThat(response.body().get(1).getStatus()).isEqualTo(StatusCode.KO);
    }



    @Test
    @RunWithCustomExecutor
    public void testStoreUnitGraphThenStoreThenOK() throws DatabaseException, StoreGraphException {
        VitamConfiguration.setStoreGraphElementsPerFile(5);

        LocalDateTime dateTime =
            LocalDateTime.now().minus(VitamConfiguration.getStoreGraphOverlapDelay(), ChronoUnit.SECONDS);

        String dateInMongo = LocalDateUtil.getFormattedDateForMongo(dateTime);
        List<Document> documents = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            documents.add(Document.parse("{\"_id\": " + i + ", \"_glpd\": \"" + dateInMongo + "\" }"));
        }

        vitamRepositoryProvider.getVitamMongoRepository(MetadataCollections.UNIT).save(documents);

        MongoCollection<Document> collection = mongoRule.getMongoCollection(MetadataCollections.UNIT.name());
        long count = collection.count();
        assertThat(count).isEqualTo(10);

        Map<MetadataCollections, Integer> ok = storeGraphService.tryStoreGraph();
        assertThat(ok.get(MetadataCollections.UNIT)).isEqualTo(10);


        dateTime = LocalDateTime.now().minus(VitamConfiguration.getStoreGraphOverlapDelay(), ChronoUnit.SECONDS);
        dateInMongo = LocalDateUtil.getFormattedDateForMongo(dateTime);
        documents = new ArrayList<>();
        for (int i = 10; i < 15; i++) {
            documents.add(Document.parse("{\"_id\": " + i + ", \"_glpd\": \"" + dateInMongo + "\" }"));
        }

        vitamRepositoryProvider.getVitamMongoRepository(MetadataCollections.UNIT).save(documents);

        collection = mongoRule.getMongoCollection(MetadataCollections.UNIT.name());
        count = collection.count();
        assertThat(count).isEqualTo(15);

        ok = storeGraphService.tryStoreGraph();
        assertThat(ok.get(MetadataCollections.UNIT)).isEqualTo(5);

        ok = storeGraphService.tryStoreGraph();
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
        createInWorkspace(container, unit_with_graph_0_guid, JSON_EXTENTION, unit_with_graph_0, DataCategory.UNIT);
        createInWorkspace(container, unit_with_graph_1_guid, JSON_EXTENTION, unit_with_graph_1, DataCategory.UNIT);
        createInWorkspace(container, unit_with_graph_2_guid, JSON_EXTENTION, unit_with_graph_2, DataCategory.UNIT);
        createInWorkspace(container, unit_with_graph_3_guid, JSON_EXTENTION, unit_with_graph_3, DataCategory.UNIT);


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
            reconstructionService.reconstructCollection("" + TENANT_0, reconstructionItems).execute();
        assertThat(response.code()).isEqualTo(200);
        assertThat(response.body().size()).isEqualTo(1);
        assertThat(offsetRepository.findOffsetBy(TENANT_0, MetadataCollections.UNIT.getName())).isEqualTo(4L);
        assertThat(response.body().get(0).getTenant()).isEqualTo(0);
        assertThat(response.body().get(0).getStatus()).isEqualTo(StatusCode.OK);

        RequestResponse<JsonNode> metadataResponse = metadataClient.getUnitByIdRaw(unit_with_graph_1_guid);
        assertThat(metadataResponse.isOk());
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
        createInWorkspace(container, unit_graph_zip_file_name, "", unit_graph_zip_file, DataCategory.UNIT_GRAPH);


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

        response = reconstructionService.reconstructCollection("" + TENANT_0, reconstructionItems).execute();
        assertThat(response.code()).isEqualTo(200);
        assertThat(response.body().size()).isEqualTo(1);

        metadataResponse = metadataClient.getUnitByIdRaw(unit_with_graph_2_guid);
        assertThat(metadataResponse.isOk());
        assertThat(((RequestResponseOK<JsonNode>) metadataResponse).getResults().size()).isEqualTo(1);
        JsonNode first = ((RequestResponseOK<JsonNode>) metadataResponse).getFirstResult();
        assertThat(first.get("_graph").toString())
            .contains("aeaqaaaaaahlm6sdabkeoaldc3hq6kyaaaca/aeaqaaaaaahlm6sdabkeoaldc3hq6jaaaabq");
        assertThat(first.get("_us_sp").toString()).contains("FRAN_NP_009913");
        assertThat(first.get("_glpd").toString()).contains("2018-04-30T13:47:49.738");



        metadataResponse = metadataClient.getUnitByIdRaw(unit_with_graph_1_guid);
        assertThat(metadataResponse.isOk());
        assertThat(((RequestResponseOK<JsonNode>) metadataResponse).getResults().size()).isEqualTo(1);
        first = ((RequestResponseOK<JsonNode>) metadataResponse).getFirstResult();
        assertThat(first.get("_graph").toString())
            .contains("aeaqaaaaaahmtusqabktwaldc34sm6aaaada/aeaqaaaaaahmtusqabktwaldc34sm6aaaaba");
        assertThat(first.get("_graph").toString())
            .contains("aeaqaaaaaahmtusqabktwaldc34sm6aaaaba/aeaqaaaaaahmtusqabktwaldc34sm4yaaabq");
        assertThat(first.get("_us_sp").toString()).contains("Service_producteur");
        assertThat(first.get("_sps").toString()).doesNotContain("SHOULD_BE_REMOVED_AFTER_RECONTRUCT");
        assertThat(first.get("_us_sp").toString()).doesNotContain("SHOULD_BE_REMOVED_AFTER_RECONTRUCT");
        assertThat(first.get("_us_sp").toString()).doesNotContain("aeaqaaaaaahmtusqabktwaldc34sm4yaaabq");
        assertThat(first.get("_glpd").toString()).contains("2018-04-30T14:33:47.293");


        metadataResponse = metadataClient.getUnitByIdRaw(unit_with_graph_4_guid);
        assertThat(metadataResponse.isOk());
        assertThat(((RequestResponseOK<JsonNode>) metadataResponse).getResults().size()).isEqualTo(1);
        first = ((RequestResponseOK<JsonNode>) metadataResponse).getFirstResult();
        assertThat(first.get("_graph").toString())
            .contains("aeaqaaaaaahlm6sdabkeaaldc3hq6laaaaaq/aeaqaaaaaahlm6sdabkeoaldc3hq6kyaaaca");
        assertThat(first.get("_us_sp").toString()).contains("aeaqaaaaaahlm6sdabkeoaldc3hq6jaaaabq");
        assertThat(first.get("_glpd").toString()).contains("2018-04-30T13:47:50.000");
        assertThat(first.get("DescriptionLevel")).isNull();
        assertThat(first.get("_storage")).isNull();
        assertThat(first.get("_tenant")).isNull();
        assertThat(first.get("_v")).isNull();


        assertThat(offsetRepository.findOffsetBy(1, DataCategory.UNIT_GRAPH.name()))
            .isEqualTo(5L);
        assertThat(response.body().get(0).getStatus()).isEqualTo(StatusCode.OK);
    }


    @Test
    @RunWithCustomExecutor
    public void testReconstruction_unit_then_got_then_unitgraph_then_gotgraoh_in_one_phase_query_OK() throws Exception {
        // Clean offerLog
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        MetaDataClient metadataClient = MetaDataClientFactory.getInstance().getClient();

        // 0. prepare data
        String container = GUIDFactory.newGUID().getId();
        workspaceClient.createContainer(container);
        createInWorkspace(container, unit_with_graph_0_guid, JSON_EXTENTION, unit_with_graph_0, DataCategory.UNIT);
        createInWorkspace(container, unit_with_graph_1_guid, JSON_EXTENTION, unit_with_graph_1, DataCategory.UNIT);
        createInWorkspace(container, unit_with_graph_2_guid, JSON_EXTENTION, unit_with_graph_2, DataCategory.UNIT);
        createInWorkspace(container, unit_with_graph_3_guid, JSON_EXTENTION, unit_with_graph_3, DataCategory.UNIT);

        createInWorkspace(container, got_with_graph_0_guid, JSON_EXTENTION, got_with_graph_0, DataCategory.OBJECTGROUP);
        createInWorkspace(container, got_with_graph_1_guid, JSON_EXTENTION, got_with_graph_1, DataCategory.OBJECTGROUP);
        createInWorkspace(container, got_with_graph_2_guid, JSON_EXTENTION, got_with_graph_2, DataCategory.OBJECTGROUP);


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
        createInWorkspace(container, unit_graph_zip_file_name, "", unit_graph_zip_file, DataCategory.UNIT_GRAPH);
        createInWorkspace(container, got_graph_zip_file_name, "", got_graph_zip_file, DataCategory.OBJECTGROUP_GRAPH);

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
            reconstructionService.reconstructCollection("" + TENANT_0, reconstructionItems).execute();
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
        RequestResponse<JsonNode> metadataResponse = metadataClient.getUnitByIdRaw(unit_with_graph_2_guid);
        assertThat(metadataResponse.isOk());
        assertThat(((RequestResponseOK<JsonNode>) metadataResponse).getResults().size()).isEqualTo(1);
        JsonNode first = ((RequestResponseOK<JsonNode>) metadataResponse).getFirstResult();
        assertThat(first.get("_graph").toString())
            .contains("aeaqaaaaaahlm6sdabkeoaldc3hq6kyaaaca/aeaqaaaaaahlm6sdabkeoaldc3hq6jaaaabq");
        assertThat(first.get("_us_sp").toString()).contains("FRAN_NP_009913");
        assertThat(first.get("_glpd").toString()).contains("2018-04-30T13:47:49.738");


        metadataResponse = metadataClient.getUnitByIdRaw(unit_with_graph_1_guid);
        assertThat(metadataResponse.isOk());
        assertThat(((RequestResponseOK<JsonNode>) metadataResponse).getResults().size()).isEqualTo(1);
        first = ((RequestResponseOK<JsonNode>) metadataResponse).getFirstResult();
        assertThat(first.get("_graph").toString())
            .contains("aeaqaaaaaahmtusqabktwaldc34sm6aaaada/aeaqaaaaaahmtusqabktwaldc34sm6aaaaba");
        assertThat(first.get("_graph").toString())
            .contains("aeaqaaaaaahmtusqabktwaldc34sm6aaaaba/aeaqaaaaaahmtusqabktwaldc34sm4yaaabq");
        assertThat(first.get("_us_sp").toString()).contains("Service_producteur");
        assertThat(first.get("_sps").toString()).doesNotContain("SHOULD_BE_REMOVED_AFTER_RECONTRUCT");
        assertThat(first.get("_us_sp").toString()).doesNotContain("SHOULD_BE_REMOVED_AFTER_RECONTRUCT");
        assertThat(first.get("_us_sp").toString()).doesNotContain("aeaqaaaaaahmtusqabktwaldc34sm4yaaabq");
        assertThat(first.get("_glpd").toString()).contains("2018-04-30T14:33:47.293");


        metadataResponse = metadataClient.getUnitByIdRaw(unit_with_graph_4_guid);
        assertThat(metadataResponse.isOk());
        assertThat(((RequestResponseOK<JsonNode>) metadataResponse).getResults().size()).isEqualTo(1);
        first = ((RequestResponseOK<JsonNode>) metadataResponse).getFirstResult();
        assertThat(first.get("_graph").toString())
            .contains("aeaqaaaaaahlm6sdabkeaaldc3hq6laaaaaq/aeaqaaaaaahlm6sdabkeoaldc3hq6kyaaaca");
        assertThat(first.get("_us_sp").toString()).contains("aeaqaaaaaahlm6sdabkeoaldc3hq6jaaaabq");
        assertThat(first.get("_glpd").toString()).contains("2018-04-30T13:47:50.000");
        assertThat(first.get("DescriptionLevel")).isNull();
        assertThat(first.get("_storage")).isNull();
        assertThat(first.get("_tenant")).isNull();
        assertThat(first.get("_v")).isNull();


        // Check Object Group
        metadataResponse = metadataClient.getObjectGroupByIdRaw(got_with_graph_0_guid);
        assertThat(metadataResponse.isOk());
        assertThat(((RequestResponseOK<JsonNode>) metadataResponse).getResults().size()).isEqualTo(1);
        first = ((RequestResponseOK<JsonNode>) metadataResponse).getFirstResult();
        assertThat(first.get("_sp").asText()).isEqualTo("FRAN_NP_050770");
        assertThat(first.get("_v").asInt()).isEqualTo(0);
        assertThat(first.get("_glpd").asText()).isEqualTo("2018-04-20T16:46:31.735");
        assertThat(first.get("_sps").toString()).contains("FRAN_NP_050770");


        metadataResponse = metadataClient.getObjectGroupByIdRaw(got_with_graph_1_guid);
        assertThat(metadataResponse.isOk());
        assertThat(((RequestResponseOK<JsonNode>) metadataResponse).getResults().size()).isEqualTo(1);
        first = ((RequestResponseOK<JsonNode>) metadataResponse).getFirstResult();
        assertThat(first.get("_sp").asText()).isEqualTo("ABCDEFG");
        assertThat(first.get("_v").asInt()).isEqualTo(0);
        assertThat(first.get("_glpd").asText()).isEqualTo("2018-04-20T16:46:31.438");
        assertThat(first.get("_sps").toString()).contains("SHOULD_BE_ADDED");
    }



    @Test
    @RunWithCustomExecutor
    public void testReconstruction_unitgraph_then_gotgraoh_then_unit_then_got_in_two_phase_query_OK() throws Exception {
        // Clean offerLog
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        MetaDataClient metadataClient = MetaDataClientFactory.getInstance().getClient();

        // 0. prepare data
        String container = GUIDFactory.newGUID().getId();
        workspaceClient.createContainer(container);

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_1);
        createInWorkspace(container, unit_graph_zip_file_name, "", unit_graph_zip_file, DataCategory.UNIT_GRAPH);
        createInWorkspace(container, got_graph_zip_file_name, "", got_graph_zip_file, DataCategory.OBJECTGROUP_GRAPH);

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
            reconstructionService.reconstructCollection("" + TENANT_0, reconstructionItems).execute();
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
        RequestResponse<JsonNode> metadataResponse = metadataClient.getUnitByIdRaw(unit_with_graph_2_guid);
        assertThat(metadataResponse.isOk());
        assertThat(((RequestResponseOK<JsonNode>) metadataResponse).getResults().size()).isEqualTo(1);
        JsonNode first = ((RequestResponseOK<JsonNode>) metadataResponse).getFirstResult();
        assertThat(first.get("_graph").toString())
            .contains("aeaqaaaaaahlm6sdabkeoaldc3hq6kyaaaca/aeaqaaaaaahlm6sdabkeoaldc3hq6jaaaabq");
        assertThat(first.get("_us_sp").toString()).contains("FRAN_NP_009913");
        assertThat(first.get("_glpd").toString()).contains("2018-04-30T13:47:49.738");
        assertThat(first.get("DescriptionLevel")).isNull();
        assertThat(first.get("_storage")).isNull();
        assertThat(first.get("_tenant")).isNull();
        assertThat(first.get("_v")).isNull();

        metadataResponse = metadataClient.getUnitByIdRaw(unit_with_graph_1_guid);
        assertThat(metadataResponse.isOk());
        assertThat(((RequestResponseOK<JsonNode>) metadataResponse).getResults().size()).isEqualTo(1);
        first = ((RequestResponseOK<JsonNode>) metadataResponse).getFirstResult();
        assertThat(first.get("_graph").toString())
            .contains("aeaqaaaaaahmtusqabktwaldc34sm6aaaada/aeaqaaaaaahmtusqabktwaldc34sm6aaaaba");
        assertThat(first.get("_graph").toString())
            .contains("aeaqaaaaaahmtusqabktwaldc34sm6aaaaba/aeaqaaaaaahmtusqabktwaldc34sm4yaaabq");
        assertThat(first.get("_us_sp").toString()).contains("Service_producteur");
        assertThat(first.get("_sps").toString()).doesNotContain("SHOULD_BE_REMOVED_AFTER_RECONTRUCT");
        assertThat(first.get("_us_sp").toString()).doesNotContain("SHOULD_BE_REMOVED_AFTER_RECONTRUCT");
        assertThat(first.get("_us_sp").toString()).doesNotContain("aeaqaaaaaahmtusqabktwaldc34sm4yaaabq");
        assertThat(first.get("_glpd").toString()).contains("2018-04-30T14:33:47.293");
        assertThat(first.get("DescriptionLevel")).isNull();
        assertThat(first.get("_storage")).isNull();
        assertThat(first.get("_tenant")).isNull();
        assertThat(first.get("_v")).isNull();

        metadataResponse = metadataClient.getUnitByIdRaw(unit_with_graph_4_guid);
        assertThat(metadataResponse.isOk());
        assertThat(((RequestResponseOK<JsonNode>) metadataResponse).getResults().size()).isEqualTo(1);
        first = ((RequestResponseOK<JsonNode>) metadataResponse).getFirstResult();
        assertThat(first.get("_graph").toString())
            .contains("aeaqaaaaaahlm6sdabkeaaldc3hq6laaaaaq/aeaqaaaaaahlm6sdabkeoaldc3hq6kyaaaca");
        assertThat(first.get("_us_sp").toString()).contains("aeaqaaaaaahlm6sdabkeoaldc3hq6jaaaabq");
        assertThat(first.get("_glpd").toString()).contains("2018-04-30T13:47:50.000");
        assertThat(first.get("DescriptionLevel")).isNull();
        assertThat(first.get("_storage")).isNull();
        assertThat(first.get("_tenant")).isNull();
        assertThat(first.get("_v")).isNull();


        // Check Object Group
        metadataResponse = metadataClient.getObjectGroupByIdRaw(got_with_graph_0_guid);
        assertThat(metadataResponse.isOk());
        assertThat(((RequestResponseOK<JsonNode>) metadataResponse).getResults().size()).isEqualTo(1);
        first = ((RequestResponseOK<JsonNode>) metadataResponse).getFirstResult();
        assertThat(first.get("_glpd").asText()).isEqualTo("2018-04-20T16:46:31.735");
        assertThat(first.get("_sps").toString()).contains("FRAN_NP_050770");
        assertThat(first.get("_v")).isNull();
        assertThat(first.get("_sp")).isNull();

        metadataResponse = metadataClient.getObjectGroupByIdRaw(got_with_graph_1_guid);
        assertThat(metadataResponse.isOk());
        assertThat(((RequestResponseOK<JsonNode>) metadataResponse).getResults().size()).isEqualTo(1);
        first = ((RequestResponseOK<JsonNode>) metadataResponse).getFirstResult();
        assertThat(first.get("_glpd").asText()).isEqualTo("2018-04-20T16:46:31.438");
        assertThat(first.get("_sps").toString()).contains("SHOULD_BE_ADDED");
        assertThat(first.get("_v")).isNull();
        assertThat(first.get("_sp")).isNull();


        createInWorkspace(container, unit_with_graph_0_guid, JSON_EXTENTION, unit_with_graph_0, DataCategory.UNIT);
        createInWorkspace(container, unit_with_graph_1_guid, JSON_EXTENTION, unit_with_graph_1, DataCategory.UNIT);
        createInWorkspace(container, unit_with_graph_2_guid, JSON_EXTENTION, unit_with_graph_2, DataCategory.UNIT);
        createInWorkspace(container, unit_with_graph_3_guid, JSON_EXTENTION, unit_with_graph_3, DataCategory.UNIT);

        createInWorkspace(container, got_with_graph_0_guid, JSON_EXTENTION, got_with_graph_0, DataCategory.OBJECTGROUP);
        createInWorkspace(container, got_with_graph_1_guid, JSON_EXTENTION, got_with_graph_1, DataCategory.OBJECTGROUP);
        createInWorkspace(container, got_with_graph_2_guid, JSON_EXTENTION, got_with_graph_2, DataCategory.OBJECTGROUP);


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
            reconstructionService.reconstructCollection("" + TENANT_0, reconstructionItems).execute();
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
        metadataResponse = metadataClient.getUnitByIdRaw(unit_with_graph_2_guid);
        assertThat(metadataResponse.isOk());
        assertThat(((RequestResponseOK<JsonNode>) metadataResponse).getResults().size()).isEqualTo(1);
        first = ((RequestResponseOK<JsonNode>) metadataResponse).getFirstResult();
        assertThat(first.get("_graph").toString())
            .contains("aeaqaaaaaahlm6sdabkeoaldc3hq6kyaaaca/aeaqaaaaaahlm6sdabkeoaldc3hq6jaaaabq");
        assertThat(first.get("_us_sp").toString()).contains("FRAN_NP_009913");
        assertThat(first.get("_glpd").toString()).contains("2018-04-30T13:47:49.738");


        metadataResponse = metadataClient.getUnitByIdRaw(unit_with_graph_1_guid);
        assertThat(metadataResponse.isOk());
        assertThat(((RequestResponseOK<JsonNode>) metadataResponse).getResults().size()).isEqualTo(1);
        first = ((RequestResponseOK<JsonNode>) metadataResponse).getFirstResult();
        assertThat(first.get("_graph").toString())
            .contains("aeaqaaaaaahmtusqabktwaldc34sm6aaaada/aeaqaaaaaahmtusqabktwaldc34sm6aaaaba");
        assertThat(first.get("_graph").toString())
            .contains("aeaqaaaaaahmtusqabktwaldc34sm6aaaaba/aeaqaaaaaahmtusqabktwaldc34sm4yaaabq");
        assertThat(first.get("_us_sp").toString()).contains("Service_producteur");
        assertThat(first.get("_sps").toString()).doesNotContain("SHOULD_BE_REMOVED_AFTER_RECONTRUCT");
        assertThat(first.get("_us_sp").toString()).doesNotContain("SHOULD_BE_REMOVED_AFTER_RECONTRUCT");
        assertThat(first.get("_us_sp").toString()).doesNotContain("aeaqaaaaaahmtusqabktwaldc34sm4yaaabq");
        assertThat(first.get("_glpd").toString()).contains("2018-04-30T14:33:47.293");
        assertThat(first.get("_storage")).isNotNull();
        assertThat(first.get("_tenant")).isNotNull();
        assertThat(first.get("_v")).isNotNull();

        metadataResponse = metadataClient.getUnitByIdRaw(unit_with_graph_4_guid);
        assertThat(metadataResponse.isOk());
        assertThat(((RequestResponseOK<JsonNode>) metadataResponse).getResults().size()).isEqualTo(1);
        first = ((RequestResponseOK<JsonNode>) metadataResponse).getFirstResult();
        assertThat(first.get("_graph").toString())
            .contains("aeaqaaaaaahlm6sdabkeaaldc3hq6laaaaaq/aeaqaaaaaahlm6sdabkeoaldc3hq6kyaaaca");
        assertThat(first.get("_us_sp").toString()).contains("aeaqaaaaaahlm6sdabkeoaldc3hq6jaaaabq");
        assertThat(first.get("_glpd").toString()).contains("2018-04-30T13:47:50.000");

        // Check Object Group
        metadataResponse = metadataClient.getObjectGroupByIdRaw(got_with_graph_0_guid);
        assertThat(metadataResponse.isOk());
        assertThat(((RequestResponseOK<JsonNode>) metadataResponse).getResults().size()).isEqualTo(1);
        first = ((RequestResponseOK<JsonNode>) metadataResponse).getFirstResult();
        assertThat(first.get("_sp").asText()).isEqualTo("FRAN_NP_050770");
        assertThat(first.get("_v").asInt()).isEqualTo(0);
        assertThat(first.get("_glpd").asText()).isEqualTo("2018-04-20T16:46:31.735");
        assertThat(first.get("_sps").toString()).contains("FRAN_NP_050770");
        assertThat(first.get("_qualifiers")).isNotNull();
        assertThat(first.get("_tenant")).isNotNull();
        assertThat(first.get("_v")).isNotNull();

        metadataResponse = metadataClient.getObjectGroupByIdRaw(got_with_graph_1_guid);
        assertThat(metadataResponse.isOk());
        assertThat(((RequestResponseOK<JsonNode>) metadataResponse).getResults().size()).isEqualTo(1);
        first = ((RequestResponseOK<JsonNode>) metadataResponse).getFirstResult();
        assertThat(first.get("_sp").asText()).isEqualTo("ABCDEFG");
        assertThat(first.get("_v").asInt()).isEqualTo(0);
        assertThat(first.get("_glpd").asText()).isEqualTo("2018-04-20T16:46:31.438");
        assertThat(first.get("_sps").toString()).contains("SHOULD_BE_ADDED");
        assertThat(first.get("_qualifiers")).isNotNull();
        assertThat(first.get("_tenant")).isNotNull();
        assertThat(first.get("_v")).isNotNull();
    }

    private Select getSelectQueryProjectionSimple(String guid) throws InvalidCreateOperationException {
        Select select = new Select();
        select.setQuery(QueryHelper.eq("obId", guid));
        return select;
    }

    private void createInWorkspace(String container, String fileName, String extention, String file, DataCategory type)
        throws ContentAddressableStorageServerException, IOException,
        StorageAlreadyExistsClientException, StorageNotFoundClientException,
        StorageServerClientException {
        final ObjectDescription objectDescription = new ObjectDescription();
        objectDescription.setWorkspaceContainerGUID(container);
        objectDescription.setObjectName(fileName + extention);
        objectDescription.setType(type);
        objectDescription.setWorkspaceObjectURI(fileName + extention);

        try (FileInputStream stream = new FileInputStream(PropertiesUtils.findFile(file))) {
            workspaceClient.putObject(container, fileName + extention, stream);
        }
        storageClient.storeFileFromWorkspace("default", type, fileName, objectDescription);
    }

    /**
     * Clean offers content.
     */
    private static void cleanOffers() {
        // ugly style but we don't have the digest herelo
        File directory = new File(OFFER_FOLDER);
        try {
            FileUtils.cleanDirectory(directory);
            FileUtils.deleteDirectory(directory);
        } catch (IOException | IllegalArgumentException e) {
            LOGGER.error("ERROR: Exception has been thrown when cleaning offers.", e);
        }
    }

    public interface MetadataReconstructionService {
        @POST("/metadata/v1/reconstruction")
        @Headers({
            "Accept: application/json",
            "Content-Type: application/json"
        })
        Call<List<ReconstructionResponseItem>> reconstructCollection(@Header("X-Tenant-Id") String tenant,
            @Body List<ReconstructionRequestItem> reconstructionItems);
    }

}
