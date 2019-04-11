/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 *******************************************************************************/
package fr.gouv.vitam.logbook.administration.integration.test;

import static fr.gouv.vitam.common.PropertiesUtils.readYaml;
import static fr.gouv.vitam.common.PropertiesUtils.writeYaml;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.Response;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.restassured.RestAssured;
import fr.gouv.vitam.access.internal.client.AccessInternalClientFactory;
import fr.gouv.vitam.access.internal.rest.AccessInternalMain;
import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.client.VitamRequestIterator;
import fr.gouv.vitam.common.client.configuration.ClientConfigurationImpl;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.format.identification.FormatIdentifierFactory;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.model.administration.ContextModel;
import fr.gouv.vitam.common.model.administration.IngestContractModel;
import fr.gouv.vitam.common.model.administration.SecurityProfileModel;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.server.AdminManagementConfiguration;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClient;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClientFactory;
import fr.gouv.vitam.ingest.internal.upload.rest.IngestInternalMain;
import fr.gouv.vitam.logbook.administration.core.api.LogbookCheckConsistencyService;
import fr.gouv.vitam.logbook.administration.core.impl.LogbookCheckConsistencyServiceImpl;
import fr.gouv.vitam.logbook.common.model.coherence.LogbookCheckError;
import fr.gouv.vitam.logbook.common.model.coherence.LogbookCheckEvent;
import fr.gouv.vitam.logbook.common.model.coherence.LogbookCheckResult;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.LogbookConfiguration;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.logbook.common.server.database.collections.VitamRepositoryFactory;
import fr.gouv.vitam.logbook.common.server.database.collections.VitamRepositoryProvider;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.api.config.MetaDataConfiguration;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.metadata.core.database.collections.MongoDbAccessMetadataImpl;
import fr.gouv.vitam.metadata.rest.MetadataMain;
import fr.gouv.vitam.processing.common.model.ProcessWorkflow;
import fr.gouv.vitam.processing.engine.core.monitoring.ProcessMonitoringImpl;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.processing.management.rest.ProcessManagementMain;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.server.rest.StorageConfiguration;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.storage.offers.common.database.OfferLogDatabaseService;
import fr.gouv.vitam.storage.offers.common.rest.DefaultOfferMain;
import fr.gouv.vitam.storage.offers.common.rest.OfferConfiguration;
import fr.gouv.vitam.worker.server.rest.WorkerMain;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import org.apache.commons.io.FileUtils;
import org.assertj.core.util.Files;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Integration test of LogbookCheckConsistency services.
 */
public class LogbookCheckConsistencyIT {

    /**
     * Vitam logger.
     */
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogbookCheckConsistencyIT.class);

    private static final String INTEGRATION_LOGBOOK_ACCESS_INTERNAL_CONF = "integration-logbook/access-internal.conf";
    private static final String DEFAULT_OFFER_CONF = "integration-logbook/storage-default-offer.conf";
    private static final String ADMIN_MANAGEMENT_CONF = "integration-logbook/functional-administration.conf";
    private static final String INGEST_INTERNAL_CONF = "integration-logbook/ingest-internal.conf";
    private static final String STORAGE_CONF = "integration-logbook/storage-engine.conf";
    private static final String METADATA_CONF = "integration-logbook/metadata.conf";
    private static final String LOGBOOK_CONF = "integration-logbook/logbook.conf";
    private static final String WORKSPACE_CONF = "integration-logbook/workspace.conf";
    private static final String PROCESSING_CONF = "integration-logbook/processing.conf";
    private static final String CONFIG_WORKER_PATH = "integration-logbook/worker.conf";
    private static final String JETTY_ACCESS_INTERNAL_PORT = "jetty.access-internal.port";
    private static final String INTEGRATION_LOGBOOK_FORMAT_IDENTIFIERS_CONF =
        "integration-logbook/format-identifiers.conf";

    private static final String JETTY_WORKER_PORT = "jetty.worker.port";
    private static final String HTTP_LOCALHOST = "http://localhost:";
    private static final String VITAM_TMP_FOLDER = "vitam.tmp.folder";
    ;
    private static final String CONTAINER = "checklogbookreports";
    private static final String OFFER_FOLDER = "offer";
    private static final String STRATEGY_ID = "default";
    private static final String OBJECT_ID = "objectId";
    private static final String LOCALHOST = "localhost";

    private static final int PORT_SERVICE_FUNCTIONAL_ADMIN = 8093;
    private static final int PORT_SERVICE_INGEST_INTERNAL = 8095;
    private static final int PORT_SERVICE_ACCESS_INTERNAL = 8092;
    private static final int PORT_SERVICE_PROCESSING = 8097;
    private static final int PORT_SERVICE_WORKSPACE = 8094;
    private static final int PORT_SERVICE_METADATA = 8096;
    private static final int PORT_SERVICE_LOGBOOK = 8099;
    private static final int PORT_SERVICE_STORAGE = 8193;
    private static final int PORT_SERVICE_WORKER = 8098;
    private static final int PORT_SERVICE_OFFER = 8194;

    private static final String CONTEXT_ID = "DEFAULT_WORKFLOW_RESUME";
    private static final String WORKSPACE_PATH = "/workspace/v1";
    private static final String WORKSPACE_URL = "http://localhost:" + PORT_SERVICE_WORKSPACE;
    private static final String PROCESSING_URL = "http://localhost:" + PORT_SERVICE_PROCESSING;
    private static final String VITAM_TEST = "Vitam-Test";

    private static final String CHECK_LOGBOOK_DATA_AGENCIES = "integration-logbook/data/agencies.csv";
    private static final String ACCESS_CONTRATS_JSON = "integration-logbook/data/access_contrats.json";
    private static final String REFERENTIAL_CONTRACTS_OK_JSON =
        "integration-logbook/data/referential_contracts_ok.json";
    private static final String HECK_LOGBOOK_MGT_RULES_REF_CSV = "integration-logbook/data/MGT_RULES_REF.csv";
    private static final String MGT_RULES_REF_CSV = "MGT_RULES_REF.csv";
    private static final String AGENCIES_CSV = "agencies.csv";
    private static final String CHECK_LOGBOOK_DROID_SIGNATURE_FILE_V88_XML =
        "integration-logbook/data/DROID_SignatureFile_V88.xml";
    private static final String DROID_SIGNATURE_FILE_V88_XML = "DROID_SignatureFile_V88.xml";
    private static final String JETTY_INGEST_INTERNAL_PORT = "jetty.ingest-internal.port";

    private static final String SIP_KO_ARBO_RECURSIVE = "integration-logbook/data/KO_ARBO_recursif.zip";
    private static final String EXPECTED_RESULTS_JSON = "integration-logbook/data/expected_results.json";

    private static final String DEFAULT_WORKFLOW_RESUME = "DEFAULT_WORKFLOW_RESUME";
    private static final String ERROR_EXCEPTION_HAS_BEEN_THROWN_WHEN_CLEANING_OFFERS =
        "ERROR: Exception has been thrown when cleaning offers.";
    private static final String ERROR_EXCEPTION_HAS_BEEN_THROWN_WHEN_CLEANNING_WORKSPACE =
        "ERROR: Exception has been thrown when cleanning workspace:";

    private static final int TENANT_0 = 0;
    private static final long SLEEP_TIME = 100l;
    private static final long NB_TRY = 9600;
    
    private static MetadataMain medtadataApplication;
    private static WorkerMain workerApplication;
    private static WorkspaceMain workspaceMain;
    private static WorkspaceClient workspaceClient;
    private static ProcessManagementMain processManagementApplication;
    private static LogbookMain logbookApplication;
    private static StorageMain storageMain;
    private static StorageClient storageClient;
    private static DefaultOfferMain defaultOfferApplication;
    private static AdminManagementMain adminManagementMain;
    private static IngestInternalMain ingestInternalApplication;
    private static AccessInternalMain accessInternalApplication;

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    @ClassRule
    public static MongoRule mongoRule =
        new MongoRule(MongoDbAccessMetadataImpl.getMongoClientOptions(), VITAM_TEST,
            LogbookCollections.OPERATION.getName(), LogbookCollections.LIFECYCLE_UNIT.getName(),
            LogbookCollections.LIFECYCLE_OBJECTGROUP.getName(),
            OfferLogDatabaseService.OFFER_LOG_COLLECTION_NAME);

    @ClassRule
    public static ElasticsearchRule elasticsearchRule =
        new ElasticsearchRule(Files.newTemporaryFolder(), LogbookCollections.OPERATION.getName(),
            LogbookCollections.LIFECYCLE_UNIT.getName(),
            LogbookCollections.LIFECYCLE_OBJECTGROUP.getName());

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @BeforeClass
    public static void setupBeforeClass() throws Exception {

        File vitamTempFolder = tempFolder.newFolder();
        SystemPropertyUtil.set(VITAM_TMP_FOLDER, vitamTempFolder.getAbsolutePath());

        // launch functional Admin server
        final List<ElasticsearchNode> nodesEs = new ArrayList<>();
        nodesEs.add(new ElasticsearchNode(LOCALHOST, elasticsearchRule.getTcpPort()));

        // launch metadata
        final File metaConfig = PropertiesUtils.findFile(METADATA_CONF);
        final MetaDataConfiguration realMetadataConfig =
            PropertiesUtils.readYaml(metaConfig, MetaDataConfiguration.class);
        realMetadataConfig.getMongoDbNodes().get(0).setDbPort(mongoRule.getDataBasePort());
        realMetadataConfig.setDbName(mongoRule.getMongoDatabase().getName());
        realMetadataConfig.setElasticsearchNodes(nodesEs);
        realMetadataConfig.setClusterName(elasticsearchRule.getClusterName());
        PropertiesUtils.writeYaml(metaConfig, realMetadataConfig);

        SystemPropertyUtil.set(MetadataMain.PARAMETER_JETTY_SERVER_PORT,
            Integer.toString(PORT_SERVICE_METADATA));
        medtadataApplication = new MetadataMain(METADATA_CONF);
        medtadataApplication.start();
        SystemPropertyUtil.clear(MetadataMain.PARAMETER_JETTY_SERVER_PORT);
        MetaDataClientFactory.changeMode(new ClientConfigurationImpl("localhost", PORT_SERVICE_METADATA));

        // launch workspace
        SystemPropertyUtil.set(WorkspaceMain.PARAMETER_JETTY_SERVER_PORT,
            Integer.toString(PORT_SERVICE_WORKSPACE));

        final File workspaceConfigFile = PropertiesUtils.findFile(WORKSPACE_CONF);
        fr.gouv.vitam.common.storage.StorageConfiguration workspaceConfiguration =
            PropertiesUtils.readYaml(workspaceConfigFile, fr.gouv.vitam.common.storage.StorageConfiguration.class);
        workspaceConfiguration.setStoragePath(vitamTempFolder.getAbsolutePath());
        writeYaml(workspaceConfigFile, workspaceConfiguration);

        // prepare workspace
        workspaceMain = new WorkspaceMain(WORKSPACE_CONF);
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
        logbookConfiguration.getMongoDbNodes().get(0).setDbPort(mongoRule.getDataBasePort());
        logbookConfiguration.setWorkspaceUrl(HTTP_LOCALHOST + PORT_SERVICE_WORKSPACE);
        PropertiesUtils.writeYaml(logbookConfigFile, logbookConfiguration);

        logbookApplication = new LogbookMain(logbookConfigFile.getAbsolutePath());
        logbookApplication.start();
        SystemPropertyUtil.clear(LogbookMain.PARAMETER_JETTY_SERVER_PORT);
        LogbookOperationsClientFactory.changeMode(new ClientConfigurationImpl(LOCALHOST, PORT_SERVICE_LOGBOOK));
        LogbookLifeCyclesClientFactory.changeMode(new ClientConfigurationImpl(LOCALHOST, PORT_SERVICE_LOGBOOK));

        SystemPropertyUtil.set(ProcessManagementMain.PARAMETER_JETTY_SERVER_PORT,
            Integer.toString(PORT_SERVICE_PROCESSING));
        processManagementApplication = new ProcessManagementMain(PROCESSING_CONF);
        processManagementApplication.start();
        SystemPropertyUtil.clear(ProcessManagementMain.PARAMETER_JETTY_SERVER_PORT);

        // launch worker
        SystemPropertyUtil.set(JETTY_WORKER_PORT, Integer.toString(PORT_SERVICE_WORKER));
        workerApplication = new WorkerMain(CONFIG_WORKER_PATH);
        workerApplication.start();
        SystemPropertyUtil.clear(JETTY_WORKER_PORT);

        FormatIdentifierFactory.getInstance().changeConfigurationFile(INTEGRATION_LOGBOOK_FORMAT_IDENTIFIERS_CONF);

        // launch ingest-internal
        SystemPropertyUtil.set(JETTY_INGEST_INTERNAL_PORT,
            Integer.toString(PORT_SERVICE_INGEST_INTERNAL));
        ingestInternalApplication = new IngestInternalMain(INGEST_INTERNAL_CONF);
        ingestInternalApplication.start();
        SystemPropertyUtil.clear(JETTY_INGEST_INTERNAL_PORT);

        ProcessingManagementClientFactory.changeConfigurationUrl(PROCESSING_URL);

        final File adminConfig = PropertiesUtils.findFile(ADMIN_MANAGEMENT_CONF);
        final AdminManagementConfiguration realAdminConfig =
            PropertiesUtils.readYaml(adminConfig, AdminManagementConfiguration.class);
        realAdminConfig.getMongoDbNodes().get(0).setDbPort(mongoRule.getDataBasePort());
        realAdminConfig.setDbName(mongoRule.getMongoDatabase().getName());
        realAdminConfig.setElasticsearchNodes(nodesEs);
        realAdminConfig.setClusterName(elasticsearchRule.getClusterName());
        realAdminConfig.setWorkspaceUrl(HTTP_LOCALHOST + PORT_SERVICE_WORKSPACE);
        PropertiesUtils.writeYaml(adminConfig, realAdminConfig);

        // prepare functional admin
        adminManagementMain = new AdminManagementMain(adminConfig.getAbsolutePath());
        adminManagementMain.start();

        AdminManagementClientFactory
            .changeMode(new ClientConfigurationImpl(LOCALHOST, PORT_SERVICE_FUNCTIONAL_ADMIN));

        // prepare offer
        SystemPropertyUtil
            .set(DefaultOfferMain.PARAMETER_JETTY_SERVER_PORT, Integer.toString(PORT_SERVICE_OFFER));
        final File offerConfig = PropertiesUtils.findFile(DEFAULT_OFFER_CONF);
        final OfferConfiguration offerConfiguration = PropertiesUtils.readYaml(offerConfig, OfferConfiguration.class);
        List<MongoDbNode> mongoDbNodes = offerConfiguration.getMongoDbNodes();
        mongoDbNodes.get(0).setDbPort(MongoRule.getDataBasePort());
        offerConfiguration.setMongoDbNodes(mongoDbNodes);
        PropertiesUtils.writeYaml(offerConfig, offerConfiguration);
        defaultOfferApplication = new DefaultOfferMain(DEFAULT_OFFER_CONF);
        defaultOfferApplication.start();
        SystemPropertyUtil.clear(DefaultOfferMain.PARAMETER_JETTY_SERVER_PORT);

        // storage engine
        File storageConfigurationFile = PropertiesUtils.findFile(STORAGE_CONF);
        final StorageConfiguration serverConfiguration = readYaml(storageConfigurationFile, StorageConfiguration.class);
        serverConfiguration
            .setUrlWorkspace(HTTP_LOCALHOST + PORT_SERVICE_WORKSPACE);

        serverConfiguration.setZippingDirecorty(tempFolder.newFolder().getAbsolutePath());
        serverConfiguration.setLoggingDirectory(tempFolder.newFolder().getAbsolutePath());

        writeYaml(storageConfigurationFile, serverConfiguration);

        // prepare storage
        SystemPropertyUtil
            .set(StorageMain.PARAMETER_JETTY_SERVER_PORT, Integer.toString(PORT_SERVICE_STORAGE));
        storageMain = new StorageMain(STORAGE_CONF);
        storageMain.start();
        SystemPropertyUtil.clear(StorageMain.PARAMETER_JETTY_SERVER_PORT);

        StorageClientFactory.changeMode(new ClientConfigurationImpl(LOCALHOST, PORT_SERVICE_STORAGE));
        storageClient = StorageClientFactory.getInstance().getClient();

        SystemPropertyUtil.set(JETTY_ACCESS_INTERNAL_PORT, Integer.toString(PORT_SERVICE_ACCESS_INTERNAL));
        accessInternalApplication =
            new AccessInternalMain(INTEGRATION_LOGBOOK_ACCESS_INTERNAL_CONF);
        accessInternalApplication.start();
        SystemPropertyUtil.clear(JETTY_ACCESS_INTERNAL_PORT);
        AccessInternalClientFactory.getInstance().changeServerPort(PORT_SERVICE_ACCESS_INTERNAL);
    }

    @AfterClass
    public static void afterClass() throws Exception {

        // Ugly style but necessary because this is the folder representing the workspace
        File workspaceFolder = new File(CONTAINER);
        if (workspaceFolder.exists()) {
            try {
                // if clean workspace delete did not work
                FileUtils.cleanDirectory(workspaceFolder);
                FileUtils.deleteDirectory(workspaceFolder);
            } catch (Exception e) {
                LOGGER.error(ERROR_EXCEPTION_HAS_BEEN_THROWN_WHEN_CLEANNING_WORKSPACE, e);
            }
        }
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
                LOGGER.error(ERROR_EXCEPTION_HAS_BEEN_THROWN_WHEN_CLEANING_OFFERS, e);
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
        if (workerApplication != null) {
            workerApplication.stop();
        }
        if (processManagementApplication != null) {
            processManagementApplication.stop();
        }
        if (medtadataApplication != null) {
            medtadataApplication.stop();
        }
        if (adminManagementMain != null) {
            adminManagementMain.stop();
        }
        if (accessInternalApplication != null) {
            accessInternalApplication.stop();
        }
        elasticsearchRule.afterClass();
    }

    @Before
    public void setup() {
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(TENANT_0));
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        VitamThreadUtils.getVitamSession().setContextId("Context_IT");
    }

    @After
    public void tearDown() {
        // clean offers
        cleanOffers();

        mongoRule.handleAfter();
        elasticsearchRule.handleAfter();
    }

    /**
     * Logbook's properties check service.
     */
    private LogbookCheckConsistencyService coherenceCheckService;

    @Test
    @RunWithCustomExecutor
    public void testLogbookCoherenceCheck_withoutIncoherentLogbook() throws Exception {
        LOGGER.debug("Starting integration tests for logbook coherence checks.");

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        VitamThreadUtils.getVitamSession().setContextId("Context_IT");

        // Import of the agencies referential.
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            client.importAgenciesFile(PropertiesUtils.getResourceAsStream(
                    CHECK_LOGBOOK_DATA_AGENCIES), AGENCIES_CSV);
        }

        // logbook configuration
        final File logbookConfig = PropertiesUtils.findFile(LOGBOOK_CONF);
        final LogbookConfiguration configuration = PropertiesUtils.readYaml(logbookConfig, LogbookConfiguration.class);

        // get vitamRepository instance.
        final VitamRepositoryProvider vitamRepository = VitamRepositoryFactory.getInstance();

        // call the logbook coherence check service
        coherenceCheckService = new LogbookCheckConsistencyServiceImpl(configuration, vitamRepository);
        coherenceCheckService.logbookCoherenceCheckByTenant(TENANT_0);

        // verify offer check logbook report content
        VitamRequestIterator<JsonNode> result =
                storageClient.listContainer(STRATEGY_ID, DataCategory.CHECKLOGBOOKREPORTS);

        Assert.assertTrue(result.hasNext());
        JsonNode node = result.next();
        Assert.assertNotNull(node.get(OBJECT_ID));

        Response response =
                storageClient.getContainerAsync(STRATEGY_ID, node.get(OBJECT_ID).asText(),
                        DataCategory.CHECKLOGBOOKREPORTS);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        final InputStream inputStream =
                storageClient.getContainerAsync(STRATEGY_ID, node.get(OBJECT_ID).asText(), DataCategory.CHECKLOGBOOKREPORTS)
                        .readEntity(InputStream.class);

        ObjectMapper mapper = new ObjectMapper();
        LogbookCheckResult logbookCheckResult = JsonHandler.getFromInputStream(inputStream, LogbookCheckResult.class);
        assertNotNull(logbookCheckResult);

        Set<LogbookCheckEvent> logbookCheckedEvents = logbookCheckResult.getCheckedEvents();
        assertNotNull(logbookCheckedEvents);
        assertFalse(logbookCheckedEvents.isEmpty());

        Set<LogbookCheckError> logbookCheckErrors = logbookCheckResult.getCheckErrors();
        assertNotNull(logbookCheckErrors);
        assertTrue(logbookCheckErrors.isEmpty());
    }
    
    @Test
    @RunWithCustomExecutor
    public void testLogbookCoherenceCheckSIP_KO_withIncoherentLogbook() throws Exception {
        LOGGER.debug("Starting integration tests for logbook coherence checks.");

        // Import of data
        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(TENANT_0);
        VitamThreadUtils.getVitamSession().setContextId("Context_IT");
        importFiles();

        // workspace client dezip SIP in workspace
        RestAssured.port = PORT_SERVICE_WORKSPACE;
        RestAssured.basePath = WORKSPACE_PATH;

        final InputStream zipInputStreamSipObject = PropertiesUtils.getResourceAsStream(SIP_KO_ARBO_RECURSIVE);

        // init default logbook operation
        final List<LogbookOperationParameters> params = new ArrayList<>();
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        final LogbookOperationParameters initParameters = LogbookParametersFactory.newLogbookOperationParameters(
            operationGuid, "PROCESS_SIP_UNITARY", operationGuid,
            LogbookTypeProcess.INGEST,
            StatusCode.UNKNOWN,
            operationGuid != null ? operationGuid.toString() : "outcomeDetailMessage",
            operationGuid);
        params.add(initParameters);
        LOGGER.error(initParameters.toString());

        // call ingest
        IngestInternalClientFactory.getInstance().changeServerPort(PORT_SERVICE_INGEST_INTERNAL);
        final IngestInternalClient client = IngestInternalClientFactory.getInstance().getClient();
        client.uploadInitialLogbook(params);

        // init workflow before execution
        client.initWorkFlow(DEFAULT_WORKFLOW_RESUME);
        client.upload(zipInputStreamSipObject, CommonMediaType.ZIP_TYPE, CONTEXT_ID);
        wait(operationGuid.toString());

        ProcessWorkflow processWorkflow =
            ProcessMonitoringImpl.getInstance().findOneProcessWorkflow(operationGuid.toString(), TENANT_0);

        assertNotNull(processWorkflow);
        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());

        // logbook configuration
        final File logbookConfig = PropertiesUtils.findFile(LOGBOOK_CONF);
        final LogbookConfiguration configuration = PropertiesUtils.readYaml(logbookConfig, LogbookConfiguration.class);

        // get vitamRepository instance.
        final VitamRepositoryProvider vitamRepository = VitamRepositoryFactory.getInstance();

        // call the logbook coherence check service
        coherenceCheckService = new LogbookCheckConsistencyServiceImpl(configuration, vitamRepository);
        coherenceCheckService.logbookCoherenceCheckByTenant(TENANT_0);

        // verify generated logbook check consistency stored report
        VitamRequestIterator<JsonNode> result =
            storageClient.listContainer(STRATEGY_ID, DataCategory.CHECKLOGBOOKREPORTS);

        Assert.assertTrue(result.hasNext());
        JsonNode node = result.next();
        Assert.assertNotNull(node.get(OBJECT_ID));

        Response response =
            storageClient.getContainerAsync(STRATEGY_ID, node.get(OBJECT_ID).asText(),
                DataCategory.CHECKLOGBOOKREPORTS);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        final InputStream inputStream =
            storageClient.getContainerAsync(STRATEGY_ID, node.get(OBJECT_ID).asText(), DataCategory.CHECKLOGBOOKREPORTS)
                .readEntity(InputStream.class);

        ObjectMapper mapper = new ObjectMapper();
        LogbookCheckResult logbookCheckResult = JsonHandler.getFromInputStream(inputStream, LogbookCheckResult.class);
        assertNotNull(logbookCheckResult);

        Set<LogbookCheckEvent> logbookCheckedEvents = logbookCheckResult.getCheckedEvents();
        assertNotNull(logbookCheckedEvents);
        assertFalse(logbookCheckedEvents.isEmpty());

        Set<LogbookCheckError> logbookCheckErrors = logbookCheckResult.getCheckErrors();
        assertNotNull(logbookCheckErrors);
        assertFalse(logbookCheckErrors.isEmpty());
        
        Set<LogbookCheckError> expectedResults = mapper.readValue(
            PropertiesUtils.getResourceAsStream(EXPECTED_RESULTS_JSON), 
            new TypeReference<Set<LogbookCheckError>>() {
        });

        Assert.assertTrue(logbookCheckErrors.containsAll(expectedResults));
    }

    /**
     * import files.
     */
    private void importFiles() {

        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(TENANT_0));
            client.importFormat(PropertiesUtils.getResourceAsStream(CHECK_LOGBOOK_DROID_SIGNATURE_FILE_V88_XML),
                DROID_SIGNATURE_FILE_V88_XML);

            // Import Rules
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(TENANT_0));
            client.importRulesFile(PropertiesUtils.getResourceAsStream(HECK_LOGBOOK_MGT_RULES_REF_CSV),
                MGT_RULES_REF_CSV);

            // import service agent
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(TENANT_0));
            client.importAgenciesFile(PropertiesUtils.getResourceAsStream(CHECK_LOGBOOK_DATA_AGENCIES),
                AGENCIES_CSV);

            // import contract
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(TENANT_0));
            File fileContracts = PropertiesUtils.getResourceFile(REFERENTIAL_CONTRACTS_OK_JSON);
            List<IngestContractModel> IngestContractModelList = JsonHandler.getFromFileAsTypeRefence(fileContracts,
                new TypeReference<List<IngestContractModel>>() {
            });
            client.importIngestContracts(IngestContractModelList);

            // import contrat
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(TENANT_0));
            File fileAccessContracts = PropertiesUtils.getResourceFile(ACCESS_CONTRATS_JSON);
            List<AccessContractModel> accessContractModelList = JsonHandler
                .getFromFileAsTypeRefence(fileAccessContracts, new TypeReference<List<AccessContractModel>>() {
            });
            client.importAccessContracts(accessContractModelList);


            // Import Security Profile
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(TENANT_0));
            client.importSecurityProfiles(JsonHandler
                .getFromFileAsTypeRefence(
                    PropertiesUtils.getResourceFile("integration-logbook/data/security_profile_ok.json"),
                    new TypeReference<List<SecurityProfileModel>>() {
                    }));

            // Import Context
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(TENANT_0));
            client.importContexts(JsonHandler
                .getFromFileAsTypeRefence(PropertiesUtils.getResourceFile("integration-logbook/data/contexts.json"),
                    new TypeReference<List<ContextModel>>() {
                    }));

        } catch (final Exception e) {
            LOGGER.error(e);
        }
    }

    /**
     * wait antil the sip is loaded.
     *
     * @param operationId
     */
    private void wait(String operationId) {
        int nbTry = 0;
        ProcessingManagementClient processingClient =
            ProcessingManagementClientFactory.getInstance().getClient();
        while (!processingClient.isOperationCompleted(operationId)) {
            try {
                Thread.sleep(SLEEP_TIME);
            } catch (InterruptedException e) {
                SysErrLogger.FAKE_LOGGER.ignoreLog(e);
            }
            if (nbTry == NB_TRY)
                break;
            nbTry++;
        }
    }

    /**
     * Clean offers content.
     */
    private static void cleanOffers() {
        File directory = new File(OFFER_FOLDER);
        try {
            FileUtils.cleanDirectory(directory);
            FileUtils.deleteDirectory(directory);
        } catch (IOException | IllegalArgumentException e) {
            LOGGER.error(ERROR_EXCEPTION_HAS_BEEN_THROWN_WHEN_CLEANING_OFFERS, e);
        }
    }
}
