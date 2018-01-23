package fr.gouv.vitam.reconstruction.integration.test;

import static fr.gouv.vitam.common.PropertiesUtils.readYaml;
import static fr.gouv.vitam.common.PropertiesUtils.writeYaml;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.assertj.core.util.Files;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.client.configuration.ClientConfigurationImpl;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
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
import fr.gouv.vitam.storage.offers.common.rest.DefaultOfferMain;
import fr.gouv.vitam.storage.offers.common.rest.OfferConfiguration;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import okhttp3.OkHttpClient;
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
public class ReconstructionMetadatasIT {

    /**
     * Vitam logger.
     */
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ReconstructionMetadatasIT.class);

    private static final String OP_GUID = "aeaqaaaaaagbcaacaang6ak4ts6zzzzzzzzz";
    private static final String UNIT_0_JSON = "integration-reconstruction/data/unit_0.json";
    private static final String UNIT_0_GUID = "aeaqaaaaaagf2rjkaa4kkalbspglplyaaaca";
    private static final String UNIT_1_JSON = "integration-reconstruction/data/unit_1.json";
    private static final String UNIT_1_GUID = "aeaqaaaaaagf2rjkaa4kkalbsqboewaaaacq";
    private static final String UNIT_2_JSON = "integration-reconstruction/data/unit_2.json";
    private static final String UNIT_2_GUID = "aeaqaaaaaahdwvstab7eialbsrah7pyaaabq";

    private static final String GOT_0_JSON = "integration-reconstruction/data/got_0.json";
    private static final String GOT_0_GUID = "aebaaaaaaahdwvstab7eialbsrq4wgyaaaaq";
    private static final String GOT_1_JSON = "integration-reconstruction/data/got_1.json";
    private static final String GOT_1_GUID = "aebaaaaaaahdwvstab7eialbsrrj4viaaacq";
    private static final String GOT_2_JSON = "integration-reconstruction/data/got_2.json";
    private static final String GOT_2_GUID = "aebaaaaaaagf2rjkaa4kkalbspgxieyaaaaq";

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

    private static WorkspaceMain workspaceMain;
    private static WorkspaceClient workspaceClient;

    private static LogbookMain logbookMain;

    private static StorageMain storageMain;
    private static StorageClient storageClient;

    private static DefaultOfferMain defaultOfferMain;

    private static MetadataMain metadataMain;

    private MetadataReconstructionService reconstructionService;

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    @ClassRule
    public static MongoRule mongoRule =
        new MongoRule(MongoDbAccessMetadataImpl.getMongoClientOptions(), "Vitam-Test",
            MetadataCollections.UNIT.getName(), MetadataCollections.OBJECTGROUP.getName(),
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
            // .readTimeout(600, TimeUnit.SECONDS)
            // .connectTimeout(600, TimeUnit.SECONDS)
            .build();
        Retrofit retrofit =
            new Retrofit.Builder().client(okHttpClient).baseUrl(METADATA_URL)
                .addConverterFactory(JacksonConverterFactory.create()).build();
        reconstructionService = retrofit.create(MetadataReconstructionService.class);
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
    public void testReconstructionMetadatas() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        MetaDataClient metadataClient = MetaDataClientFactory.getInstance().getClient();
        LogbookLifeCyclesClient lifecycleClient = LogbookLifeCyclesClientFactory.getInstance().getClient();

        // 0. prepare data
        workspaceClient.createContainer(OP_GUID);
        createInWorkspace(UNIT_0_GUID, UNIT_0_JSON, DataCategory.UNIT);
        createInWorkspace(UNIT_1_GUID, UNIT_1_JSON, DataCategory.UNIT);
        createInWorkspace(UNIT_2_GUID, UNIT_2_JSON, DataCategory.UNIT);

        createInWorkspace(GOT_0_GUID, GOT_0_JSON, DataCategory.OBJECTGROUP);
        createInWorkspace(GOT_1_GUID, GOT_1_JSON, DataCategory.OBJECTGROUP);
        createInWorkspace(GOT_2_GUID, GOT_2_JSON, DataCategory.OBJECTGROUP);

        RequestResponse<OfferLog> offerLogResponse1 =
            storageClient.getOfferLogs("default", DataCategory.UNIT, 0L, 10, Order.ASC);
        assertThat(offerLogResponse1).isNotNull();
        assertThat(offerLogResponse1.isOk()).isTrue();
        assertThat(((RequestResponseOK<OfferLog>) offerLogResponse1).getResults().size()).isEqualTo(3);

        List<ReconstructionRequestItem> reconstructionItems;
        ReconstructionRequestItem reconstructionItem1;
        ReconstructionRequestItem reconstructionItem2;
        Response<List<ReconstructionResponseItem>> response;
        RequestResponse<JsonNode> metadataResponse;
        JsonNode lifecycleResponse;

        // 1. reconstruct unit
        reconstructionItems = new ArrayList<>();
        reconstructionItem1 = new ReconstructionRequestItem();
        reconstructionItem1.setCollection(DataCategory.UNIT.name());
        reconstructionItem1.setLimit(2);
        reconstructionItem1.setOffset(0L);
        reconstructionItem1.setTenant(TENANT_0);
        reconstructionItems.add(reconstructionItem1);
        response = reconstructionService.reconstructCollection("" + TENANT_0, reconstructionItems).execute();
        assertThat(response.code()).isEqualTo(200);
        assertThat(response.body().size()).isEqualTo(1);
        assertThat(response.body().get(0).getOffset()).isEqualTo(2L);
        assertThat(response.body().get(0).getTenant()).isEqualTo(0);
        assertThat(response.body().get(0).getStatus()).isEqualTo(StatusCode.OK);

        metadataResponse = metadataClient.getUnitByIdRaw(UNIT_0_GUID);
        assertThat(metadataResponse.isOk());
        assertThat(((RequestResponseOK<JsonNode>) metadataResponse).getResults().size()).isEqualTo(1);
        assertThat(((RequestResponseOK<JsonNode>) metadataResponse).getFirstResult().get("_id").asText())
            .isEqualTo(UNIT_0_GUID);
        assertThat(((RequestResponseOK<JsonNode>) metadataResponse).getFirstResult().get("_v").asInt()).isEqualTo(0);

        lifecycleResponse =
            lifecycleClient.selectUnitLifeCycleById(UNIT_0_GUID,
                getSelectQueryProjectionSimple(UNIT_0_GUID).getFinalSelect());
        assertThat(lifecycleResponse).isNotNull();
        assertThat(lifecycleResponse.get("$results")).isNotNull();
        assertThat(lifecycleResponse.get("$results").size()).isEqualTo(1);
        assertThat(lifecycleResponse.get("$results").get(0).get("_id").asText()).isEqualTo(UNIT_0_GUID);
        assertThat(lifecycleResponse.get("$results").get(0).get("_v").asInt()).isEqualTo(4);

        metadataResponse = metadataClient.getUnitByIdRaw(UNIT_1_GUID);
        assertThat(metadataResponse.isOk());
        assertThat(((RequestResponseOK<JsonNode>) metadataResponse).getResults().size()).isEqualTo(1);
        assertThat(((RequestResponseOK<JsonNode>) metadataResponse).getFirstResult().get("_id").asText())
            .isEqualTo(UNIT_1_GUID);
        assertThat(((RequestResponseOK<JsonNode>) metadataResponse).getFirstResult().get("_v").asInt()).isEqualTo(0);

        lifecycleResponse =
            lifecycleClient.selectUnitLifeCycleById(UNIT_1_GUID,
                getSelectQueryProjectionSimple(UNIT_1_GUID).getFinalSelect());
        assertThat(lifecycleResponse).isNotNull();
        assertThat(lifecycleResponse.get("$results")).isNotNull();
        assertThat(lifecycleResponse.get("$results").size()).isEqualTo(1);
        assertThat(lifecycleResponse.get("$results").get(0).get("_id").asText()).isEqualTo(UNIT_1_GUID);
        assertThat(lifecycleResponse.get("$results").get(0).get("_v").asInt()).isEqualTo(4);


        // 2. reconstruct object group
        reconstructionItems = new ArrayList<>();
        reconstructionItem2 = new ReconstructionRequestItem();
        reconstructionItem2.setCollection(DataCategory.OBJECTGROUP.name());
        reconstructionItem2.setLimit(2);
        reconstructionItem2.setOffset(0L);
        reconstructionItem2.setTenant(TENANT_0);
        reconstructionItems.add(reconstructionItem2);
        response = reconstructionService.reconstructCollection("" + TENANT_0, reconstructionItems).execute();
        assertThat(response.code()).isEqualTo(200);
        assertThat(response.body().size()).isEqualTo(1);
        assertThat(response.body().get(0).getOffset()).isEqualTo(5L);
        assertThat(response.body().get(0).getStatus()).isEqualTo(StatusCode.OK);

        metadataResponse = metadataClient.getObjectGroupByIdRaw(GOT_0_GUID);
        assertThat(metadataResponse.isOk());
        assertThat(((RequestResponseOK<JsonNode>) metadataResponse).getResults().size()).isEqualTo(1);
        assertThat(((RequestResponseOK<JsonNode>) metadataResponse).getFirstResult().get("_id").asText())
            .isEqualTo(GOT_0_GUID);
        assertThat(((RequestResponseOK<JsonNode>) metadataResponse).getFirstResult().get("_v").asInt()).isEqualTo(0);

        lifecycleResponse =
            lifecycleClient.selectObjectGroupLifeCycleById(GOT_0_GUID,
                getSelectQueryProjectionSimple(GOT_0_GUID).getFinalSelect());
        assertThat(lifecycleResponse).isNotNull();
        assertThat(lifecycleResponse.get("$results")).isNotNull();
        assertThat(lifecycleResponse.get("$results").size()).isEqualTo(1);
        assertThat(lifecycleResponse.get("$results").get(0).get("_id").asText()).isEqualTo(GOT_0_GUID);
        assertThat(lifecycleResponse.get("$results").get(0).get("_v").asInt()).isEqualTo(8);

        metadataResponse = metadataClient.getObjectGroupByIdRaw(GOT_1_GUID);
        assertThat(metadataResponse.isOk());
        assertThat(((RequestResponseOK<JsonNode>) metadataResponse).getResults().size()).isEqualTo(1);
        assertThat(((RequestResponseOK<JsonNode>) metadataResponse).getFirstResult().get("_id").asText())
            .isEqualTo(GOT_1_GUID);
        assertThat(((RequestResponseOK<JsonNode>) metadataResponse).getFirstResult().get("_v").asInt()).isEqualTo(0);

        lifecycleResponse =
            lifecycleClient.selectObjectGroupLifeCycleById(GOT_1_GUID,
                getSelectQueryProjectionSimple(GOT_1_GUID).getFinalSelect());
        assertThat(lifecycleResponse).isNotNull();
        assertThat(lifecycleResponse.get("$results")).isNotNull();
        assertThat(lifecycleResponse.get("$results").size()).isEqualTo(1);
        assertThat(lifecycleResponse.get("$results").get(0).get("_id").asText()).isEqualTo(GOT_1_GUID);
        assertThat(lifecycleResponse.get("$results").get(0).get("_v").asInt()).isEqualTo(8);

        // 3. relaunch reconstruct for unit and got
        reconstructionItems = new ArrayList<>();
        reconstructionItems.add(reconstructionItem1);
        reconstructionItems.add(reconstructionItem2);
        response = reconstructionService.reconstructCollection("" + TENANT_0, reconstructionItems).execute();
        assertThat(response.code()).isEqualTo(200);
        assertThat(response.body().size()).isEqualTo(2);
        assertThat(response.body().get(0).getOffset()).isEqualTo(2L);
        assertThat(response.body().get(0).getStatus()).isEqualTo(StatusCode.OK);
        assertThat(response.body().get(1).getOffset()).isEqualTo(5L);
        assertThat(response.body().get(1).getStatus()).isEqualTo(StatusCode.OK);

        // 4. reconstruct next for unit and got
        reconstructionItems = new ArrayList<>();
        reconstructionItem1.setOffset(2L);
        reconstructionItems.add(reconstructionItem1);
        reconstructionItem2.setOffset(5L);
        reconstructionItems.add(reconstructionItem2);
        response = reconstructionService.reconstructCollection("" + TENANT_0, reconstructionItems).execute();
        assertThat(response.code()).isEqualTo(200);
        assertThat(response.body().size()).isEqualTo(2);
        assertThat(response.body().get(0).getOffset()).isEqualTo(3L);
        assertThat(response.body().get(0).getStatus()).isEqualTo(StatusCode.OK);
        assertThat(response.body().get(1).getOffset()).isEqualTo(6L);
        assertThat(response.body().get(1).getStatus()).isEqualTo(StatusCode.OK);

        metadataResponse = metadataClient.getUnitByIdRaw(UNIT_2_GUID);
        assertThat(metadataResponse.isOk());
        assertThat(((RequestResponseOK<JsonNode>) metadataResponse).getResults().size()).isEqualTo(1);
        assertThat(((RequestResponseOK<JsonNode>) metadataResponse).getFirstResult().get("_id").asText())
            .isEqualTo(UNIT_2_GUID);
        assertThat(((RequestResponseOK<JsonNode>) metadataResponse).getFirstResult().get("_v").asInt()).isEqualTo(0);

        lifecycleResponse =
            lifecycleClient.selectUnitLifeCycleById(UNIT_2_GUID,
                getSelectQueryProjectionSimple(UNIT_2_GUID).getFinalSelect());
        assertThat(lifecycleResponse).isNotNull();
        assertThat(lifecycleResponse.get("$results")).isNotNull();
        assertThat(lifecycleResponse.get("$results").size()).isEqualTo(1);
        assertThat(lifecycleResponse.get("$results").get(0).get("_id").asText()).isEqualTo(UNIT_2_GUID);
        assertThat(lifecycleResponse.get("$results").get(0).get("_v").asInt()).isEqualTo(4);

        metadataResponse = metadataClient.getObjectGroupByIdRaw(GOT_2_GUID);
        assertThat(metadataResponse.isOk());
        assertThat(((RequestResponseOK<JsonNode>) metadataResponse).getResults().size()).isEqualTo(1);
        assertThat(((RequestResponseOK<JsonNode>) metadataResponse).getFirstResult().get("_id").asText())
            .isEqualTo(GOT_2_GUID);
        assertThat(((RequestResponseOK<JsonNode>) metadataResponse).getFirstResult().get("_v").asInt()).isEqualTo(0);

        lifecycleResponse =
            lifecycleClient.selectObjectGroupLifeCycleById(GOT_2_GUID,
                getSelectQueryProjectionSimple(GOT_2_GUID).getFinalSelect());
        assertThat(lifecycleResponse).isNotNull();
        assertThat(lifecycleResponse.get("$results")).isNotNull();
        assertThat(lifecycleResponse.get("$results").size()).isEqualTo(1);
        assertThat(lifecycleResponse.get("$results").get(0).get("_id").asText()).isEqualTo(GOT_2_GUID);
        assertThat(lifecycleResponse.get("$results").get(0).get("_v").asInt()).isEqualTo(8);

        // 5. reconstruct nothing for unit and got
        reconstructionItems = new ArrayList<>();
        reconstructionItem1.setOffset(3L);
        reconstructionItems.add(reconstructionItem1);
        reconstructionItem2.setOffset(6L);
        reconstructionItems.add(reconstructionItem2);

        response = reconstructionService.reconstructCollection("" + TENANT_0, reconstructionItems).execute();
        assertThat(response.code()).isEqualTo(200);
        assertThat(response.body().size()).isEqualTo(2);
        assertThat(response.body().get(0).getOffset()).isEqualTo(3L);
        assertThat(response.body().get(0).getStatus()).isEqualTo(StatusCode.OK);
        assertThat(response.body().get(1).getOffset()).isEqualTo(6L);
        assertThat(response.body().get(1).getStatus()).isEqualTo(StatusCode.OK);

        // 6. reconstruct on unused tenants
        reconstructionItems = new ArrayList<>();
        reconstructionItem1.setOffset(0L);
        reconstructionItem1.setTenant(TENANT_1);
        reconstructionItems.add(reconstructionItem1);
        reconstructionItem2.setOffset(0L);
        reconstructionItem2.setTenant(TENANT_1);
        reconstructionItems.add(reconstructionItem2);

        response = reconstructionService.reconstructCollection("" + TENANT_0, reconstructionItems).execute();
        assertThat(response.code()).isEqualTo(200);
        assertThat(response.body().size()).isEqualTo(2);
        assertThat(response.body().get(0).getOffset()).isEqualTo(0L);
        assertThat(response.body().get(0).getStatus()).isEqualTo(StatusCode.OK);
        assertThat(response.body().get(1).getOffset()).isEqualTo(0L);
        assertThat(response.body().get(1).getStatus()).isEqualTo(StatusCode.OK);

        // 6. reconstruct on unit and another invalid collection
        reconstructionItems = new ArrayList<>();
        reconstructionItem1.setOffset(0L);
        reconstructionItem1.setTenant(TENANT_0);
        reconstructionItems.add(reconstructionItem1);
        reconstructionItem2.setCollection(DataCategory.MANIFEST.name());
        reconstructionItem2.setOffset(0L);
        reconstructionItem2.setTenant(TENANT_0);
        reconstructionItems.add(reconstructionItem2);

        response = reconstructionService.reconstructCollection("" + TENANT_0, reconstructionItems).execute();
        assertThat(response.code()).isEqualTo(200);
        assertThat(response.body().size()).isEqualTo(2);
        assertThat(response.body().get(0).getOffset()).isEqualTo(2L);
        assertThat(response.body().get(0).getStatus()).isEqualTo(StatusCode.OK);
        assertThat(response.body().get(1).getOffset()).isEqualTo(0L);
        assertThat(response.body().get(1).getStatus()).isEqualTo(StatusCode.KO);


    }

    private Select getSelectQueryProjectionSimple(String guid) throws InvalidCreateOperationException {
        Select select = new Select();
        select.setQuery(QueryHelper.eq("obId", guid));
        return select;
    }

    private void createInWorkspace(String guid, String jsonFile, DataCategory type)
        throws ContentAddressableStorageAlreadyExistException, ContentAddressableStorageServerException, IOException,
        FileNotFoundException, StorageAlreadyExistsClientException, StorageNotFoundClientException,
        StorageServerClientException {
        final ObjectDescription objectDescription = new ObjectDescription();
        objectDescription.setWorkspaceContainerGUID(OP_GUID);
        objectDescription.setObjectName(guid + ".json");
        objectDescription.setType(type);
        objectDescription.setWorkspaceObjectURI(guid + ".json");

        try (FileInputStream stream = new FileInputStream(PropertiesUtils.findFile(jsonFile))) {
            workspaceClient.putObject(OP_GUID, guid + ".json", stream);
        }
        storageClient.storeFileFromWorkspace("default", type, guid, objectDescription);
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
