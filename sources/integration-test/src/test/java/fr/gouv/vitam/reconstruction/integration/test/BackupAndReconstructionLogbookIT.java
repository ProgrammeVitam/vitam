/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
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
package fr.gouv.vitam.reconstruction.integration.test;

import static fr.gouv.vitam.common.PropertiesUtils.readYaml;
import static fr.gouv.vitam.common.PropertiesUtils.writeYaml;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.and;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.exists;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.client.configuration.ClientConfigurationImpl;
import fr.gouv.vitam.common.database.builder.query.BooleanQuery;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.InvalidGuidOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.guid.GUIDReader;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.VitamConstants;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.model.administration.AccessionRegisterDetailModel;
import fr.gouv.vitam.common.model.administration.AccessionRegisterSummaryModel;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.server.AdminManagementConfiguration;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.model.reconstruction.ReconstructionRequestItem;
import fr.gouv.vitam.logbook.common.model.reconstruction.ReconstructionResponseItem;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.LogbookConfiguration;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.core.database.collections.MongoDbAccessMetadataImpl;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.Order;
import fr.gouv.vitam.storage.engine.server.rest.StorageConfiguration;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.storage.offers.common.database.OfferLogDatabaseService;
import fr.gouv.vitam.storage.offers.common.rest.DefaultOfferMain;
import fr.gouv.vitam.storage.offers.common.rest.OfferConfiguration;
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
 * Integration tests for the reconstruction services. <br/>
 */
public class BackupAndReconstructionLogbookIT {

    /**
     * Vitam logger.
     */
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(BackupAndReconstructionLogbookIT.class);

    private static final String ACCESS_CONTRACT =
        "integration-reconstruction/data/access_contract_every_originating_angency.json";

    private static final String LOGBOOK_0_GUID = "aecaaaaaaceeytj5abrzmalbvy426faaaaaq";
    private static final String LOGBOOK_0_EVENT_GUID = "aedqaaaaaceeytj5abrzmalbvy43cpyaaaba";
    private static final String REGISTER_0_JSON = "integration-reconstruction/data/register_0.json";
    private static final String LOGBOOK_1_GUID = "aecaaaaaaceeytj5abrzmalbvy42qsaaaaaq";
    private static final String LOGBOOK_1_EVENT_GUID = "aedqaaaaaceeytj5abrzmalbvy42vhyaaaba";
    private static final String REGISTER_1_JSON = "integration-reconstruction/data/register_1.json";
    private static final String LOGBOOK_2_GUID = "aecaaaaaaceeytj5abrzmalbvybpaeiaaaaq";
    private static final String LOGBOOK_2_EVENT_GUID = "aedqaaaaaceeytj5abrzmalbvybpkaiaaaba";


    private static final String DEFAULT_OFFER_CONF = "integration-reconstruction/storage-default-offer.conf";
    private static final String LOGBOOK_CONF = "integration-reconstruction/logbook.conf";
    private static final String STORAGE_CONF = "integration-reconstruction/storage-engine.conf";
    private static final String WORKSPACE_CONF = "integration-reconstruction/workspace.conf";
    private static final String ADMIN_MANAGEMENT_CONF =
        "integration-reconstruction/functional-administration.conf";

    private static final String OFFER_FOLDER = "offer";
    private static final int TENANT_0 = 0;
    private static final int TENANT_1 = 1;

    private static final int PORT_SERVICE_WORKSPACE = 8094;
    private static final int PORT_SERVICE_FUNCTIONAL_ADMIN = 8093;
    private static final int PORT_SERVICE_LOGBOOK = 8099;
    private static final int PORT_SERVICE_STORAGE = 8193;
    private static final int PORT_SERVICE_OFFER = 8194;

    private static final String WORKSPACE_URL = "http://localhost:" + PORT_SERVICE_WORKSPACE;
    private static final String LOGBOOK_URL = "http://localhost:" + PORT_SERVICE_LOGBOOK;

    private static WorkspaceMain workspaceMain;
    private static WorkspaceClient workspaceClient;

    private static LogbookMain logbookApplication;

    private static StorageMain storageMain;
    private static StorageClient storageClient;

    private static DefaultOfferMain defaultOfferApplication;

    private static AdminManagementMain adminManagementMain;

    private LogbookReconstructionService reconstructionService;

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    @ClassRule
    public static MongoRule mongoRule =
        new MongoRule(MongoDbAccessMetadataImpl.getMongoClientOptions(), "Vitam-Test",
            OfferLogDatabaseService.OFFER_LOG_COLLECTION_NAME, LogbookCollections.OPERATION.getName(),
            FunctionalAdminCollections.ACCESS_CONTRACT.name());

    @ClassRule
    public static ElasticsearchRule elasticsearchRule =
        new ElasticsearchRule(Files.newTemporaryFolder(), LogbookCollections.OPERATION.getName());


    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @BeforeClass
    public static void setupBeforeClass() throws Exception {

        File vitamTempFolder = tempFolder.newFolder();
        SystemPropertyUtil.set("vitam.tmp.folder", vitamTempFolder.getAbsolutePath());

        // launch functional Admin server
        final List<ElasticsearchNode> nodesEs = new ArrayList<>();
        nodesEs.add(new ElasticsearchNode("localhost", ElasticsearchRule.getTcpPort()));

        // init clients
        AdminManagementClientFactory
            .changeMode(new ClientConfigurationImpl("localhost", PORT_SERVICE_FUNCTIONAL_ADMIN));
        StorageClientFactory.changeMode(new ClientConfigurationImpl("localhost", PORT_SERVICE_STORAGE));
        storageClient = StorageClientFactory.getInstance().getClient();

        // prepare workspace
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

        // prepare logbook
        SystemPropertyUtil
            .set(LogbookMain.PARAMETER_JETTY_SERVER_PORT, Integer.toString(PORT_SERVICE_LOGBOOK));

        final File logbookConfigFile = PropertiesUtils.findFile(LOGBOOK_CONF);
        final LogbookConfiguration logbookConfiguration =
            PropertiesUtils.readYaml(logbookConfigFile, LogbookConfiguration.class);
        logbookConfiguration.setElasticsearchNodes(nodesEs);
        logbookConfiguration.getMongoDbNodes().get(0).setDbPort(MongoRule.getDataBasePort());
        logbookConfiguration.setWorkspaceUrl("http://localhost:" + PORT_SERVICE_WORKSPACE);

        PropertiesUtils.writeYaml(logbookConfigFile, logbookConfiguration);

        logbookApplication = new LogbookMain(logbookConfigFile.getAbsolutePath());
        logbookApplication.start();
        SystemPropertyUtil.clear(LogbookMain.PARAMETER_JETTY_SERVER_PORT);
        LogbookOperationsClientFactory.changeMode(new ClientConfigurationImpl("localhost", PORT_SERVICE_LOGBOOK));
        LogbookLifeCyclesClientFactory.changeMode(new ClientConfigurationImpl("localhost", PORT_SERVICE_LOGBOOK));

        // prepare functional admin
        final File adminConfig = PropertiesUtils.findFile(ADMIN_MANAGEMENT_CONF);
        final AdminManagementConfiguration realAdminConfig =
            PropertiesUtils.readYaml(adminConfig, AdminManagementConfiguration.class);
        realAdminConfig.getMongoDbNodes().get(0).setDbPort(MongoRule.getDataBasePort());
        realAdminConfig.setDbName(mongoRule.getMongoDatabase().getName());
        realAdminConfig.setElasticsearchNodes(nodesEs);
        realAdminConfig.setClusterName(elasticsearchRule.getClusterName());
        realAdminConfig.setWorkspaceUrl("http://localhost:" + PORT_SERVICE_WORKSPACE);
        PropertiesUtils.writeYaml(adminConfig, realAdminConfig);

        adminManagementMain = new AdminManagementMain(adminConfig.getAbsolutePath());
        adminManagementMain.start();

        // prepare offer
        SystemPropertyUtil
            .set(DefaultOfferMain.PARAMETER_JETTY_SERVER_PORT, Integer.toString(PORT_SERVICE_OFFER));
        final File offerConfig = PropertiesUtils.findFile(DEFAULT_OFFER_CONF);
        final OfferConfiguration offerConfiguration = PropertiesUtils.readYaml(offerConfig, OfferConfiguration.class);
        List<MongoDbNode> mongoDbNodes = offerConfiguration.getMongoDbNodes();
        mongoDbNodes.get(0).setDbPort(MongoRule.getDataBasePort());
        offerConfiguration.setMongoDbNodes(mongoDbNodes);
        PropertiesUtils.writeYaml(offerConfig, offerConfiguration);

        defaultOfferApplication = new DefaultOfferMain(offerConfig.getAbsolutePath());
        defaultOfferApplication.start();
        SystemPropertyUtil.clear(DefaultOfferMain.PARAMETER_JETTY_SERVER_PORT);

        // prepare storage
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
        if (defaultOfferApplication != null) {
            defaultOfferApplication.stop();
        }
        if (storageMain != null) {
            storageMain.stop();
        }
        if (logbookApplication != null) {
            logbookApplication.stop();
        }
        elasticsearchRule.afterClass();
    }

    @Before
    public void setup() {
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(TENANT_0));
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        VitamThreadUtils.getVitamSession().setContractId(VitamConstants.EVERY_ORIGINATING_AGENCY);

        // reconstruct service interface - replace non existing client
        // uncomment timeouts for debug mode
        final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .readTimeout(600, TimeUnit.SECONDS)
            .connectTimeout(600, TimeUnit.SECONDS)
            .build();
        Retrofit retrofit =
            new Retrofit.Builder().client(okHttpClient).baseUrl(LOGBOOK_URL)
                .addConverterFactory(JacksonConverterFactory.create()).build();
        reconstructionService = retrofit.create(LogbookReconstructionService.class);

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
    public void testBackupAndReconstructOperationOk() throws Exception {

        List<ReconstructionRequestItem> reconstructionItems;
        ReconstructionRequestItem reconstructionItem1;
        ReconstructionRequestItem reconstructionItem2;
        Response<List<ReconstructionResponseItem>> response;
        JsonNode logbookResponse;
        RequestResponse<AccessionRegisterSummaryModel> accessionRegisterSummaryRsp;
        RequestResponse<AccessionRegisterDetailModel> accessionRegisterDetailRsp;
        LogbookOperationsClient client = LogbookOperationsClientFactory.getInstance().getClient();
        AdminManagementClient adminManagementClient = AdminManagementClientFactory.getInstance().getClient();


        // 0. Init data
        Path backup0Folder = Paths.get(OFFER_FOLDER, TENANT_0 + "_" + DataCategory.BACKUP_OPERATION.getFolder());
        Path backup1Folder = Paths.get(OFFER_FOLDER, TENANT_1 + "_" + DataCategory.BACKUP_OPERATION.getFolder());

        assertThat(java.nio.file.Files.exists(backup0Folder)).isFalse();
        assertThat(java.nio.file.Files.exists(backup1Folder)).isFalse();

        assertThat(java.nio.file.Files.exists(Paths.get(backup0Folder.toString(), LOGBOOK_0_GUID))).isFalse();
        assertThat(java.nio.file.Files.exists(Paths.get(backup0Folder.toString(), LOGBOOK_1_GUID))).isFalse();
        assertThat(java.nio.file.Files.exists(Paths.get(backup0Folder.toString(), LOGBOOK_2_GUID))).isFalse();

        client.create(getParamatersStart(LOGBOOK_0_GUID));
        assertThat(java.nio.file.Files.exists(Paths.get(backup0Folder.toString(), LOGBOOK_0_GUID))).isTrue();
        client.update(getParamatersAppend(LOGBOOK_0_GUID, LOGBOOK_0_EVENT_GUID, REGISTER_0_JSON, StatusCode.OK));
        assertThat(java.nio.file.Files.exists(Paths.get(backup0Folder.toString(), LOGBOOK_0_GUID))).isTrue();

        client.create(getParamatersStart(LOGBOOK_1_GUID));
        assertThat(java.nio.file.Files.exists(Paths.get(backup0Folder.toString(), LOGBOOK_1_GUID))).isTrue();
        client.update(getParamatersAppend(LOGBOOK_1_GUID, LOGBOOK_1_EVENT_GUID, REGISTER_1_JSON, StatusCode.WARNING));
        assertThat(java.nio.file.Files.exists(Paths.get(backup0Folder.toString(), LOGBOOK_1_GUID))).isTrue();

        client.create(getParamatersStart(LOGBOOK_2_GUID));
        assertThat(java.nio.file.Files.exists(Paths.get(backup0Folder.toString(), LOGBOOK_2_GUID))).isTrue();
        client.update(getParamatersAppend(LOGBOOK_2_GUID, LOGBOOK_2_EVENT_GUID, null, StatusCode.KO));
        assertThat(java.nio.file.Files.exists(Paths.get(backup0Folder.toString(), LOGBOOK_2_GUID))).isTrue();

        // import access contract
        File fileAccessContracts = PropertiesUtils.getResourceFile(ACCESS_CONTRACT);
        List<AccessContractModel> accessContractModelList = JsonHandler
            .getFromFileAsTypeRefence(fileAccessContracts, new TypeReference<List<AccessContractModel>>() {});
        adminManagementClient.importAccessContracts(accessContractModelList);

        mongoRule.getMongoCollection(LogbookCollections.OPERATION.getName())
            .deleteMany(new Document());

        assertThatCode(() -> {
            client.selectOperationById(LOGBOOK_0_GUID, getQueryDslId(LOGBOOK_0_GUID));
        }).isInstanceOf(LogbookClientNotFoundException.class);



        RequestResponse<OfferLog> offerLogResponse1 =
            storageClient.getOfferLogs("default", DataCategory.BACKUP_OPERATION, 0L, 10, Order.ASC);
        assertThat(offerLogResponse1).isNotNull();
        assertThat(offerLogResponse1.isOk()).isTrue();
        assertThat(((RequestResponseOK<OfferLog>) offerLogResponse1).getResults().size()).isEqualTo(9);
        assertThat(((RequestResponseOK<OfferLog>) offerLogResponse1).getResults().get(0).getSequence()).isEqualTo(1L);
        assertThat(((RequestResponseOK<OfferLog>) offerLogResponse1).getResults().get(0).getContainer())
            .isEqualTo(TENANT_0 + "_" + DataCategory.BACKUP_OPERATION.getFolder());
        assertThat(((RequestResponseOK<OfferLog>) offerLogResponse1).getResults().get(0).getFileName())
            .isEqualTo(LOGBOOK_0_GUID);
        assertThat(((RequestResponseOK<OfferLog>) offerLogResponse1).getResults().get(1).getSequence()).isEqualTo(2L);
        assertThat(((RequestResponseOK<OfferLog>) offerLogResponse1).getResults().get(1).getContainer())
            .isEqualTo(TENANT_0 + "_" + DataCategory.BACKUP_OPERATION.getFolder());
        assertThat(((RequestResponseOK<OfferLog>) offerLogResponse1).getResults().get(1).getFileName())
            .isEqualTo(LOGBOOK_0_GUID);

        RequestResponse<OfferLog> offerLogResponse2 =
            storageClient.getOfferLogs("default", DataCategory.BACKUP_OPERATION, 1L, 10, Order.DESC);
        assertThat(offerLogResponse2).isNotNull();
        assertThat(offerLogResponse2.isOk()).isTrue();
        assertThat(((RequestResponseOK<OfferLog>) offerLogResponse2).getResults().size()).isEqualTo(1);
        assertThat(((RequestResponseOK<OfferLog>) offerLogResponse2).getResults().get(0).getSequence()).isEqualTo(1L);
        assertThat(((RequestResponseOK<OfferLog>) offerLogResponse2).getResults().get(0).getFileName())
            .isEqualTo(LOGBOOK_0_GUID);

        RequestResponse<OfferLog> offerLogResponse3 =
            storageClient.getOfferLogs("default", DataCategory.BACKUP_OPERATION, null, 10, Order.DESC);
        assertThat(offerLogResponse3).isNotNull();
        assertThat(offerLogResponse3.isOk()).isTrue();
        assertThat(((RequestResponseOK<OfferLog>) offerLogResponse3).getResults().size()).isEqualTo(9);

        // 1. reconstruct operations
        reconstructionItems = new ArrayList<>();
        reconstructionItem1 = new ReconstructionRequestItem();
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

        logbookResponse = client.selectOperationById(LOGBOOK_0_GUID, getQueryDslId(LOGBOOK_0_GUID));
        RequestResponseOK<JsonNode> logbookOK = new RequestResponseOK<JsonNode>().getFromJsonNode(logbookResponse);
        assertThat((logbookOK).getResults().size()).isEqualTo(1);
        assertThat((logbookOK).getFirstResult().get("_id").asText()).isEqualTo(LOGBOOK_0_GUID);
        assertThat((logbookOK).getFirstResult().get("evId").asText()).isEqualTo(LOGBOOK_0_GUID);
        assertThat((logbookOK).getFirstResult().get("_v").asInt()).isEqualTo(1);

        accessionRegisterSummaryRsp =
            adminManagementClient.getAccessionRegister(getQueryDsOriginatinAgencylId("FRAN_NP_005568"));
        assertThat(accessionRegisterSummaryRsp.isOk()).isTrue();
        assertThat(((RequestResponseOK<AccessionRegisterSummaryModel>) accessionRegisterSummaryRsp).getResults().size())
            .isEqualTo(1);
        assertThat(((RequestResponseOK<AccessionRegisterSummaryModel>) accessionRegisterSummaryRsp).getResults()
            .get(0).getTotalObjects().getIngested()).isEqualTo(1);
        assertThat(((RequestResponseOK<AccessionRegisterSummaryModel>) accessionRegisterSummaryRsp).getResults()
            .get(0).getTotalObjects().getRemained()).isEqualTo(1);
        assertThat(((RequestResponseOK<AccessionRegisterSummaryModel>) accessionRegisterSummaryRsp).getResults()
            .get(0).getObjectSize().getIngested()).isEqualTo(29403);
        assertThat(((RequestResponseOK<AccessionRegisterSummaryModel>) accessionRegisterSummaryRsp).getResults()
            .get(0).getObjectSize().getRemained()).isEqualTo(29403);

        // 2. relaunch reconstruct operations
        reconstructionItems = new ArrayList<>();
        reconstructionItems.add(reconstructionItem1);
        response = reconstructionService.reconstructCollection("" + TENANT_0, reconstructionItems).execute();
        assertThat(response.code()).isEqualTo(200);
        assertThat(response.body().size()).isEqualTo(1);
        assertThat(response.body().get(0).getOffset()).isEqualTo(2L);
        assertThat(response.body().get(0).getStatus()).isEqualTo(StatusCode.OK);

        accessionRegisterSummaryRsp =
            adminManagementClient.getAccessionRegister(getQueryDsOriginatinAgencylId("FRAN_NP_005568"));
        assertThat(accessionRegisterSummaryRsp.isOk()).isTrue();
        assertThat(((RequestResponseOK<AccessionRegisterSummaryModel>) accessionRegisterSummaryRsp).getResults().size())
            .isEqualTo(1);
        assertThat(((RequestResponseOK<AccessionRegisterSummaryModel>) accessionRegisterSummaryRsp).getResults()
            .get(0).getTotalObjects().getIngested()).isEqualTo(1);
        assertThat(((RequestResponseOK<AccessionRegisterSummaryModel>) accessionRegisterSummaryRsp).getResults()
            .get(0).getTotalObjects().getRemained()).isEqualTo(1);
        assertThat(((RequestResponseOK<AccessionRegisterSummaryModel>) accessionRegisterSummaryRsp).getResults()
            .get(0).getObjectSize().getIngested()).isEqualTo(29403);
        assertThat(((RequestResponseOK<AccessionRegisterSummaryModel>) accessionRegisterSummaryRsp).getResults()
            .get(0).getObjectSize().getRemained()).isEqualTo(29403);


        // 3. reconstruct operation and another one after in same call
        reconstructionItems = new ArrayList<>();
        reconstructionItem1.setLimit(2);
        reconstructionItem1.setOffset(1L);
        reconstructionItem1.setTenant(TENANT_0);
        reconstructionItems.add(reconstructionItem1);
        reconstructionItem2 = new ReconstructionRequestItem();
        reconstructionItem2.setLimit(2);
        reconstructionItem2.setOffset(5L);
        reconstructionItem2.setTenant(TENANT_0);
        reconstructionItems.add(reconstructionItem2);
        response = reconstructionService.reconstructCollection("" + TENANT_0, reconstructionItems).execute();
        assertThat(response.code()).isEqualTo(200);
        assertThat(response.body().size()).isEqualTo(2);
        assertThat(response.body().get(0).getOffset()).isEqualTo(2L);
        assertThat(response.body().get(0).getStatus()).isEqualTo(StatusCode.OK);
        assertThat(response.body().get(1).getOffset()).isEqualTo(6L);
        assertThat(response.body().get(1).getStatus()).isEqualTo(StatusCode.OK);

        logbookResponse = client.selectOperationById(LOGBOOK_0_GUID, getQueryDslId(LOGBOOK_0_GUID));
        assertThat(logbookResponse.get("httpCode").asInt()).isEqualTo(200);
        assertThatCode(() -> {
            JsonNode localeResponse = client.selectOperationById(LOGBOOK_1_GUID, getQueryDslId(LOGBOOK_1_GUID));
            System.out.println(JsonHandler.prettyPrint(localeResponse));
        }).isInstanceOf(LogbookClientNotFoundException.class);
        logbookResponse = client.selectOperationById(LOGBOOK_2_GUID, getQueryDslId(LOGBOOK_2_GUID));
        assertThat(logbookResponse.get("httpCode").asInt()).isEqualTo(200);

        accessionRegisterSummaryRsp =
            adminManagementClient.getAccessionRegister(getQueryDsOriginatinAgencylId("FRAN_NP_005568"));
        assertThat(accessionRegisterSummaryRsp.isOk()).isTrue();
        assertThat(
            ((RequestResponseOK<AccessionRegisterSummaryModel>) accessionRegisterSummaryRsp).getResults().size())
                .isEqualTo(1);
        assertThat(((RequestResponseOK<AccessionRegisterSummaryModel>) accessionRegisterSummaryRsp).getResults()
            .get(0).getTotalObjects().getIngested()).isEqualTo(1);
        assertThat(((RequestResponseOK<AccessionRegisterSummaryModel>) accessionRegisterSummaryRsp).getResults()
            .get(0).getTotalObjects().getRemained()).isEqualTo(1);
        assertThat(((RequestResponseOK<AccessionRegisterSummaryModel>) accessionRegisterSummaryRsp).getResults()
            .get(0).getObjectSize().getIngested()).isEqualTo(29403);
        assertThat(((RequestResponseOK<AccessionRegisterSummaryModel>) accessionRegisterSummaryRsp).getResults()
            .get(0).getObjectSize().getRemained()).isEqualTo(29403);

        // 4. reconstruct nothing for logbook operation
        reconstructionItems = new ArrayList<>();
        reconstructionItem1.setOffset(7L);
        reconstructionItems.add(reconstructionItem1);

        response = reconstructionService.reconstructCollection("" + TENANT_0, reconstructionItems).execute();
        assertThat(response.code()).isEqualTo(200);
        assertThat(response.body().size()).isEqualTo(1);
        assertThat(response.body().get(0).getOffset()).isEqualTo(9L);
        assertThat(response.body().get(0).getStatus()).isEqualTo(StatusCode.OK);

        // 5. reconstruct on unused tenants
        reconstructionItems = new ArrayList<>();
        reconstructionItem1.setOffset(0L);
        reconstructionItem1.setTenant(TENANT_1);
        reconstructionItems.add(reconstructionItem1);

        response = reconstructionService.reconstructCollection("" + TENANT_0, reconstructionItems).execute();
        assertThat(response.code()).isEqualTo(200);
        assertThat(response.body().size()).isEqualTo(1);
        assertThat(response.body().get(0).getOffset()).isEqualTo(0L);
        assertThat(response.body().get(0).getStatus()).isEqualTo(StatusCode.OK);

        // 5. reconstruct all operations
        reconstructionItems = new ArrayList<>();
        reconstructionItem1.setOffset(0L);
        reconstructionItem1.setTenant(TENANT_0);
        reconstructionItem1.setLimit(15);
        reconstructionItems.add(reconstructionItem1);

        response = reconstructionService.reconstructCollection("" + TENANT_0, reconstructionItems).execute();
        assertThat(response.code()).isEqualTo(200);
        assertThat(response.body().size()).isEqualTo(1);
        assertThat(response.body().get(0).getOffset()).isEqualTo(10L);
        assertThat(response.body().get(0).getStatus()).isEqualTo(StatusCode.OK);

        accessionRegisterDetailRsp =
            adminManagementClient.getAccessionRegisterDetail("FRAN_NP_005568", getQueryDslOperationId(LOGBOOK_0_GUID));
        assertThat(accessionRegisterDetailRsp.isOk()).isTrue();
        assertThat(((RequestResponseOK<AccessionRegisterDetailModel>) accessionRegisterDetailRsp).getResults().size())
            .isEqualTo(1);

        accessionRegisterDetailRsp =
            adminManagementClient.getAccessionRegisterDetail("FRAN_NP_005568", getQueryDslOperationId(LOGBOOK_1_GUID));
        assertThat(accessionRegisterDetailRsp.isOk()).isTrue();
        assertThat(((RequestResponseOK<AccessionRegisterDetailModel>) accessionRegisterDetailRsp).getResults().size())
            .isEqualTo(1);

        accessionRegisterDetailRsp =
            adminManagementClient.getAccessionRegisterDetail("MICHEL_MERCIER", getQueryDslOperationId(LOGBOOK_1_GUID));
        assertThat(accessionRegisterDetailRsp.isOk()).isTrue();
        assertThat(((RequestResponseOK<AccessionRegisterDetailModel>) accessionRegisterDetailRsp).getResults().size())
            .isEqualTo(1);

        accessionRegisterSummaryRsp =
            adminManagementClient.getAccessionRegister(getQueryDsOriginatinAgencylId("FRAN_NP_005568"));
        assertThat(accessionRegisterSummaryRsp.isOk()).isTrue();
        assertThat(((RequestResponseOK<AccessionRegisterSummaryModel>) accessionRegisterSummaryRsp).getResults().size())
            .isEqualTo(1);
        assertThat(((RequestResponseOK<AccessionRegisterSummaryModel>) accessionRegisterSummaryRsp).getResults()
            .get(0).getTotalObjects().getIngested()).isEqualTo(2);
        assertThat(((RequestResponseOK<AccessionRegisterSummaryModel>) accessionRegisterSummaryRsp).getResults()
            .get(0).getTotalObjects().getRemained()).isEqualTo(2);
        assertThat(((RequestResponseOK<AccessionRegisterSummaryModel>) accessionRegisterSummaryRsp).getResults()
            .get(0).getObjectSize().getIngested()).isEqualTo(58806);
        assertThat(((RequestResponseOK<AccessionRegisterSummaryModel>) accessionRegisterSummaryRsp).getResults()
            .get(0).getObjectSize().getRemained()).isEqualTo(58806);

        accessionRegisterSummaryRsp =
            adminManagementClient.getAccessionRegister(getQueryDsOriginatinAgencylId("MICHEL_MERCIER"));
        assertThat(accessionRegisterSummaryRsp.isOk()).isTrue();
        assertThat(((RequestResponseOK<AccessionRegisterSummaryModel>) accessionRegisterSummaryRsp).getResults().size())
            .isEqualTo(1);
        assertThat(((RequestResponseOK<AccessionRegisterSummaryModel>) accessionRegisterSummaryRsp).getResults()
            .get(0).getTotalObjectsGroups().getIngested()).isEqualTo(244);
        assertThat(((RequestResponseOK<AccessionRegisterSummaryModel>) accessionRegisterSummaryRsp).getResults()
            .get(0).getTotalObjectsGroups().getRemained()).isEqualTo(244);
        assertThat(((RequestResponseOK<AccessionRegisterSummaryModel>) accessionRegisterSummaryRsp).getResults()
            .get(0).getTotalUnits().getIngested()).isEqualTo(249);
        assertThat(((RequestResponseOK<AccessionRegisterSummaryModel>) accessionRegisterSummaryRsp).getResults()
            .get(0).getTotalUnits().getRemained()).isEqualTo(249);
    }

    private JsonNode getQueryDslOperationId(String operationId) throws InvalidCreateOperationException {
        Select select = new Select();
        select.setQuery(QueryHelper.eq("OperationIds", operationId));
        return select.getFinalSelect();
    }

    private JsonNode getQueryDsOriginatinAgencylId(String originatingAgency) throws InvalidCreateOperationException {
        Select selectQuery = new Select();
        selectQuery.setQuery(QueryHelper.eq("OriginatingAgency", originatingAgency));
        return selectQuery.getFinalSelect();
    }

    private JsonNode getQueryDslId(String operationId) throws InvalidCreateOperationException {
        Select select = new Select();
        select.setQuery(QueryHelper.eq("evIdProc", operationId));
        return select.getFinalSelect();
    }


    private LogbookOperationParameters getParamatersStart(String eip) throws InvalidGuidOperationException {
        return LogbookParametersFactory
            .newLogbookOperationParameters(GUIDReader.getGUID(eip), "eventType", GUIDReader.getGUID(eip),
                LogbookTypeProcess.INGEST,
                StatusCode.STARTED, "start ingest", GUIDReader.getGUID(eip));
    }

    private LogbookOperationParameters getParamatersAppend(String eip, String eiEvent, String evDetDataFile,
        StatusCode statusCode)
        throws InvalidGuidOperationException, InvalidParseOperationException, FileNotFoundException {

        LogbookOperationParameters params = LogbookParametersFactory.newLogbookOperationParameters(
            GUIDReader.getGUID(eiEvent), "ACCESSION_REGISTRATION", GUIDReader.getGUID(eip), LogbookTypeProcess.INGEST,
            statusCode, "end ingest", GUIDReader.getGUID(eip));
        if (evDetDataFile != null) {
            params.putParameterValue(LogbookParameterName.eventDetailData, JsonHandler
                .unprettyPrint(JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(evDetDataFile))));
        }
        return params;
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
            throw new VitamRuntimeException("beurk");
        }
    }

    public interface LogbookReconstructionService {
        @POST("/logbook/v1/reconstruction/operations")
        @Headers({
            "Accept: application/json",
            "Content-Type: application/json"
        })
        Call<List<ReconstructionResponseItem>> reconstructCollection(@Header("X-Tenant-Id") String tenant,
            @Body List<ReconstructionRequestItem> reconstructionItems);
    }
}
