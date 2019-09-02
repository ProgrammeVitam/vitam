package fr.gouv.vitam.common;

import fr.gouv.vitam.access.internal.client.AccessInternalClientFactory;
import fr.gouv.vitam.access.internal.rest.AccessInternalMain;
import fr.gouv.vitam.batch.report.client.BatchReportClientFactory;
import fr.gouv.vitam.batch.report.rest.BatchReportMain;
import fr.gouv.vitam.batch.report.rest.server.BatchReportConfiguration;
import fr.gouv.vitam.common.client.MockOrRestClient;
import fr.gouv.vitam.common.client.VitamClientFactoryInterface;
import fr.gouv.vitam.common.client.configuration.ClientConfigurationImpl;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.common.storage.cas.container.api.ContentAddressableStorageAbstract;
import fr.gouv.vitam.common.tmp.TempFolderRule;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.server.AdminManagementConfiguration;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClientFactory;
import fr.gouv.vitam.ingest.internal.upload.rest.IngestInternalMain;
import fr.gouv.vitam.logbook.common.server.LogbookConfiguration;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.api.config.MetaDataConfiguration;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.metadata.rest.MetadataMain;
import fr.gouv.vitam.processing.common.exception.PluginException;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.processing.management.rest.ProcessManagementMain;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.server.rest.StorageConfiguration;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.storage.offers.rest.DefaultOfferMain;
import fr.gouv.vitam.storage.offers.rest.OfferConfiguration;
import fr.gouv.vitam.worker.client.WorkerClientConfiguration;
import fr.gouv.vitam.worker.client.WorkerClientFactory;
import fr.gouv.vitam.worker.server.rest.WorkerMain;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import org.junit.rules.ExternalResource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static fr.gouv.vitam.common.PropertiesUtils.readYaml;
import static fr.gouv.vitam.common.PropertiesUtils.writeYaml;

public class VitamServerRunner extends ExternalResource {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(VitamServerRunner.class);


    // Constants

    public static final long SLEEP_TIME = 20l;
    public static final long NB_TRY = 18000;


    public static final int PORT_SERVICE_WORKSPACE = 8094;
    public static final int PORT_SERVICE_WORKSPACE_ADMIN = 28094;
    public static final int PORT_SERVICE_METADATA = 8098;
    public static final int PORT_SERVICE_METADATA_ADMIN = 28098;
    public static final int PORT_SERVICE_LOGBOOK = 8099;
    public static final int PORT_SERVICE_LOGBOOK_ADMIN = 28099;
    public static final int PORT_SERVICE_STORAGE = 8193;
    public static final int PORT_SERVICE_STORAGE_ADMIN = 28193;
    public static final int PORT_SERVICE_FUNCTIONAL_ADMIN = 8093;
    public static final int PORT_SERVICE_FUNCTIONAL_ADMIN_ADMIN = 28093;
    public static final int PORT_SERVICE_OFFER = 8194;
    public static final int PORT_SERVICE_OFFER_ADMIN = 28194;
    public static final int PORT_SERVICE_WORKER = 8096;
    public static final int PORT_SERVICE_PROCESSING = 8097;
    public static final int PORT_SERVICE_INGEST_INTERNAL = 8095;
    public static final int PORT_SERVICE_ACCESS_INTERNAL = 8092;
    public static final int PORT_SERVICE_BATCH_REPORT = 8089;


    public static final String WORKSPACE_URL = "http://localhost:" + PORT_SERVICE_WORKSPACE;
    public static final String METADATA_URL = "http://localhost:" + PORT_SERVICE_METADATA;
    public static final String PROCESSING_URL = "http://localhost:" + PORT_SERVICE_PROCESSING;
    public static final String LOGBOOK_URL = "http://localhost:" + PORT_SERVICE_LOGBOOK;

    public static final String DEFAULT_OFFER_CONF = "common/storage-default-offer.conf";
    public static final String LOGBOOK_CONF = "common/logbook.conf";
    public static final String STORAGE_CONF = "common/storage-engine.conf";
    public static final String WORKSPACE_CONF = "common/workspace.conf";
    public static final String METADATA_CONF = "common/metadata.conf";
    public static final String ADMIN_MANAGEMENT_CONF = "common/functional-administration.conf";
    public static final String ACCESS_INTERNAL_CONF = "common/access-internal.conf";
    public static final String INGEST_INTERNAL_CONF = "common/ingest-internal.conf";
    public static final String BATCH_REPORT_CONF = "common/batch-report.conf";
    public static final String BATCH_REPORT_CLIENT_PATH = "common/batch-report-client.conf";
    public static final String PROCESSING_CONF = "common/processing.conf";
    public static final String CONFIG_WORKER_PATH = "common/worker.conf";
    public static final String FORMAT_IDENTIFIERS_CONF = "common/format-identifiers.conf";
    public static final String OFFER_FOLDER = "offer";



    private static MetadataMain metadataMain;
    private static ProcessManagementMain processManagementMain;
    private static WorkspaceMain workspaceMain;
    private static LogbookMain logbookMain;
    private static DefaultOfferMain defaultOfferMain;
    private static StorageMain storageMain;
    private static AdminManagementMain adminManagementMain;
    private static IngestInternalMain ingestInternalMain;
    private static AccessInternalMain accessInternalMain;
    private static BatchReportMain batchReportMain;
    private static WorkerMain workerMain;

    public final static TempFolderRule tempFolderRule = new TempFolderRule();

    static {
        try {
            tempFolderRule.create();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    Class<?> clazz;
    String dbname;
    String cluster;
    Set<Class> servers;

    public VitamServerRunner(Class<?> clazz, String dbname, String cluster,
        Set<Class> servers) {
        ParametersChecker.checkParameter("Params required ..", clazz, dbname, cluster, servers);
        this.clazz = clazz;
        this.dbname = dbname;
        this.cluster = cluster;
        this.servers = servers;
    }


    @Override
    protected void before() {
        try {
            LOGGER.warn(
                ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Start " + clazz.getSimpleName() +
                    " <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");

            VitamConfiguration.setHttpClientWaitingTime(1);
            VitamConfiguration.setHttpClientRandomWaitingSleep(1);
            VitamConfiguration.setHttpClientFirstAttemptWaitingTime(1);
            VitamConfiguration.setHttpClientRetry(1);

            if (Files.notExists(Paths.get(VitamConfiguration.getVitamDataFolder() + "/tmp/"))) {
                Files.createDirectories(Paths.get(VitamConfiguration.getVitamDataFolder() + "/tmp/"));
            }

            if (Files.notExists(Paths.get(VitamConfiguration.getVitamDataFolder() + "/zip/"))) {
                Files.createDirectories(Paths.get(VitamConfiguration.getVitamDataFolder() + "/zip/"));
            }

            if (Files.notExists(Paths.get(VitamConfiguration.getVitamDataFolder() + "/log/"))) {
                Files.createDirectories(Paths.get(VitamConfiguration.getVitamDataFolder() + "/log/"));
            }
            if (Files.notExists(Paths.get(VitamConfiguration.getVitamDataFolder() + "/storage/"))) {
                Files.createDirectories(Paths.get(VitamConfiguration.getVitamDataFolder() + "/storage/"));
            }



            // launch ES

            // launch workspace
            if (servers.contains(WorkspaceMain.class)) {
                startWorkspaceServer();
            }

            // launch logbook
            if (servers.contains(LogbookMain.class)) {
                startLogbookServer();
            } else {
                LogbookOperationsClientFactory.getInstance()
                    .setVitamClientType(VitamClientFactoryInterface.VitamClientType.MOCK);
                LogbookLifeCyclesClientFactory.getInstance()
                    .setVitamClientType(VitamClientFactoryInterface.VitamClientType.MOCK);
            }

            // launch metadata
            if (servers.contains(MetadataMain.class)) {
                startMetadataServer();
            } else {
                MetaDataClientFactory.changeMode(null);
            }

            // launch admin Management
            if (servers.contains(AdminManagementMain.class)) {
                startAdminManagementServer();
            } else {
                AdminManagementClientFactory.getInstance()
                    .setVitamClientType(VitamClientFactoryInterface.VitamClientType.MOCK);
            }

            // Launch offer
            if (servers.contains(DefaultOfferMain.class)) {
                startOfferServer();
            } else {
                StorageClientFactory.getInstance().setVitamClientType(VitamClientFactoryInterface.VitamClientType.MOCK);
            }

            // Launch storage engine
            if (servers.contains(StorageMain.class)) {
                startStorageServer();
            } else {
                StorageClientFactory.getInstance().setVitamClientType(VitamClientFactoryInterface.VitamClientType.MOCK);
            }

            // launch processing
            if (servers.contains(ProcessManagementMain.class)) {
                startProcessManagementServer();
            } else {
                ProcessingManagementClientFactory.changeMode(null);
            }
            // launch worker
            if (servers.contains(WorkerMain.class)) {
                startWorkerServer(CONFIG_WORKER_PATH);
            } else {
                stopWorkerServer();
            }

            if (servers.contains(AccessInternalMain.class)) {
                startAccessInternalServer();
            } else {
                AccessInternalClientFactory.getInstance()
                    .setVitamClientType(VitamClientFactoryInterface.VitamClientType.MOCK);
            }

            // IngestInternalMain
            if (servers.contains(IngestInternalMain.class)) {
                startIngestInternalServer();
            } else {
                IngestInternalClientFactory.getInstance()
                    .setVitamClientType(VitamClientFactoryInterface.VitamClientType.MOCK);
            }
            // IngestInternalMain
            if (servers.contains(BatchReportMain.class)) {
                startBatchReportServer();
                BatchReportClientFactory.changeMode(
                    BatchReportClientFactory.changeConfigurationFile(BATCH_REPORT_CLIENT_PATH));
            }

            waitServerStartOrStop(true);
        } catch (IOException | VitamApplicationServerException | PluginException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }


    private void startIngestInternalServer() throws VitamApplicationServerException {
        if (null != ingestInternalMain) {
            return;
        }

        SystemPropertyUtil.set(IngestInternalMain.PARAMETER_JETTY_SERVER_PORT,
            Integer.toString(PORT_SERVICE_INGEST_INTERNAL));
        LOGGER.warn("=== VitamServerRunner start  IngestInternalMain");
        ingestInternalMain = new IngestInternalMain(INGEST_INTERNAL_CONF);
        ingestInternalMain.start();
        SystemPropertyUtil.clear(IngestInternalMain.PARAMETER_JETTY_SERVER_PORT);
    }

    private void startBatchReportServer() throws VitamApplicationServerException, IOException {
        if (null != batchReportMain) {
            return;
        }
        SystemPropertyUtil
            .set(BatchReportMain.PARAMETER_JETTY_SERVER_PORT, Integer.toString(PORT_SERVICE_BATCH_REPORT));
        final File batchConfig = PropertiesUtils.findFile(BATCH_REPORT_CONF);
        final BatchReportConfiguration batchReportConfiguration =
            PropertiesUtils.readYaml(batchConfig, BatchReportConfiguration.class);
        List<MongoDbNode> mongoDbNodes = batchReportConfiguration.getMongoDbNodes();
        mongoDbNodes.get(0).setDbPort(MongoRule.getDataBasePort());
        batchReportConfiguration.setMongoDbNodes(mongoDbNodes);
        PropertiesUtils.writeYaml(batchConfig, batchReportConfiguration);
        LOGGER.warn("=== VitamServerRunner start  BatchReportMain");
        BatchReportClientFactory.changeMode(new ClientConfigurationImpl("localhost", 8015));
        batchReportMain = new BatchReportMain(batchConfig.getAbsolutePath());
        batchReportMain.start();
        SystemPropertyUtil.clear(BatchReportMain.PARAMETER_JETTY_SERVER_PORT);
    }

    private void stopIngestInternalServer(boolean mockWhenStop) throws VitamApplicationServerException {
        if (null == ingestInternalMain) {
            if (mockWhenStop) {
                IngestInternalClientFactory.getInstance()
                    .setVitamClientType(VitamClientFactoryInterface.VitamClientType.MOCK);
            }
            return;
        }

        LOGGER.warn("=== VitamServerRunner start  IngestInternalMain");
        ingestInternalMain.stop();
        ingestInternalMain = null;

        // Wait stop
        if (mockWhenStop) {
            IngestInternalClientFactory.getInstance()
                .setVitamClientType(VitamClientFactoryInterface.VitamClientType.MOCK);
        }
    }


    private void startAccessInternalServer() throws VitamApplicationServerException {
        if (null != accessInternalMain) {
            AccessInternalClientFactory.getInstance().changeServerPort(PORT_SERVICE_ACCESS_INTERNAL);
            return;
        }
        SystemPropertyUtil
            .set(AccessInternalMain.PARAMETER_JETTY_SERVER_PORT,
                Integer.toString(PORT_SERVICE_ACCESS_INTERNAL));
        LOGGER.warn("=== VitamServerRunner start  AccessInternalMain");
        AccessInternalClientFactory.getInstance().changeServerPort(PORT_SERVICE_ACCESS_INTERNAL);
        accessInternalMain =
            new AccessInternalMain(ACCESS_INTERNAL_CONF);
        accessInternalMain.start();
        SystemPropertyUtil.clear(AccessInternalMain.PARAMETER_JETTY_SERVER_PORT);

        // Wait startup
    }

    private void stopAccessInternalServer(boolean mockWhenStop) throws VitamApplicationServerException {
        if (null == accessInternalMain) {
            if (mockWhenStop) {
                AccessInternalClientFactory.getInstance()
                    .setVitamClientType(VitamClientFactoryInterface.VitamClientType.MOCK);
            }
            return;
        }
        LOGGER.warn("=== VitamServerRunner start  AccessInternalMain");
        accessInternalMain.stop();
        accessInternalMain = null;

        // Wait stop then Mock
        if (mockWhenStop) {
            AccessInternalClientFactory.getInstance()
                .setVitamClientType(VitamClientFactoryInterface.VitamClientType.MOCK);
        }
    }

    public void startStorageServer() throws IOException, VitamApplicationServerException {

        if (null != storageMain) {
            StorageClientFactory.changeMode(new ClientConfigurationImpl("localhost", PORT_SERVICE_STORAGE));
            return;
        }
        File storageConfigurationFile = PropertiesUtils.findFile(STORAGE_CONF);
        final StorageConfiguration serverConfiguration =
            readYaml(storageConfigurationFile, StorageConfiguration.class);
        serverConfiguration
            .setUrlWorkspace("http://localhost:" + PORT_SERVICE_WORKSPACE);

        serverConfiguration
            .setZippingDirecorty(
                Files.createTempDirectory(Paths.get(VitamConfiguration.getVitamDataFolder() + "/zip/"), "zip_").toFile()
                    .getAbsolutePath());
        serverConfiguration
            .setLoggingDirectory(
                Files.createTempDirectory(Paths.get(VitamConfiguration.getVitamDataFolder() + "/log/"), "log_").toFile()
                    .getAbsolutePath());
        writeYaml(storageConfigurationFile, serverConfiguration);

        SystemPropertyUtil
            .set(StorageMain.PARAMETER_JETTY_SERVER_PORT, Integer.toString(PORT_SERVICE_STORAGE));
        LOGGER.warn("=== VitamServerRunner start  StorageMain");

        StorageClientFactory.changeMode(new ClientConfigurationImpl("localhost", PORT_SERVICE_STORAGE));
        storageMain = new StorageMain(STORAGE_CONF);
        storageMain.start();
        SystemPropertyUtil.clear(StorageMain.PARAMETER_JETTY_SERVER_PORT);

        // Wait startup
    }

    public void stopStorageServer(boolean mockWhenStop) throws VitamApplicationServerException {
        if (null == storageMain) {
            if (mockWhenStop) {
                StorageClientFactory.getInstance()
                    .setVitamClientType(VitamClientFactoryInterface.VitamClientType.MOCK);
            }
            return;
        }

        LOGGER.warn("=== VitamServerRunner start  StorageMain");
        storageMain.stop();
        storageMain = null;
        // Wait stop then mock
        if (mockWhenStop) {
            StorageClientFactory.getInstance()
                .setVitamClientType(VitamClientFactoryInterface.VitamClientType.MOCK);
        }
    }

    public void startOfferServer() throws IOException, VitamApplicationServerException {
        if (null != defaultOfferMain) {
            return;
        }
        SystemPropertyUtil
            .set(DefaultOfferMain.PARAMETER_JETTY_SERVER_PORT, Integer.toString(PORT_SERVICE_OFFER));
        final File offerConfig = PropertiesUtils.findFile(DEFAULT_OFFER_CONF);
        final OfferConfiguration offerConfiguration =
            PropertiesUtils.readYaml(offerConfig, OfferConfiguration.class);
        List<MongoDbNode> mongoDbNodes = offerConfiguration.getMongoDbNodes();
        mongoDbNodes.get(0).setDbPort(MongoRule.getDataBasePort());
        offerConfiguration.setMongoDbNodes(mongoDbNodes);
        PropertiesUtils.writeYaml(offerConfig, offerConfiguration);
        LOGGER.warn("=== VitamServerRunner start  DefaultOfferMain");

        defaultOfferMain = new DefaultOfferMain(offerConfig.getAbsolutePath());
        defaultOfferMain.start();
        SystemPropertyUtil.clear(DefaultOfferMain.PARAMETER_JETTY_SERVER_PORT);
        ContentAddressableStorageAbstract.disableContainerCaching();
    }

    public void stopOfferServer(boolean mockWhenStop) throws VitamApplicationServerException {
        if (null == defaultOfferMain) {
            // If offer is not started, then storage should be mocked
            StorageClientFactory.getInstance().setVitamClientType(VitamClientFactoryInterface.VitamClientType.MOCK);
            return;
        }
        LOGGER.warn("=== VitamServerRunner start  DefaultOfferMain");
        defaultOfferMain.stop();
        defaultOfferMain = null;
        // If offer is not started, then storage should be mocked
        if (mockWhenStop) {
            StorageClientFactory.getInstance().setVitamClientType(VitamClientFactoryInterface.VitamClientType.MOCK);
        }
    }

    public void startAdminManagementServer() throws IOException, VitamApplicationServerException {

        if (null != adminManagementMain) {
            AdminManagementClientFactory
                .changeMode(new ClientConfigurationImpl("localhost", PORT_SERVICE_FUNCTIONAL_ADMIN));
            return;
        }
        final List<ElasticsearchNode> nodesEs = new ArrayList<>();
        nodesEs.add(new ElasticsearchNode("localhost", ElasticsearchRule.getTcpPort()));

        SystemPropertyUtil
            .set(JunitHelper.PARAMETER_JETTY_SERVER_PORT_ADMIN,
                Integer.toString(PORT_SERVICE_FUNCTIONAL_ADMIN_ADMIN));

        // prepare functional admin
        final File adminConfig = PropertiesUtils.findFile(ADMIN_MANAGEMENT_CONF);
        final AdminManagementConfiguration realAdminConfig =
            PropertiesUtils.readYaml(adminConfig, AdminManagementConfiguration.class);
        realAdminConfig.getMongoDbNodes().get(0).setDbPort(MongoRule.getDataBasePort());
        realAdminConfig.setDbName(dbname);
        realAdminConfig.setElasticsearchNodes(nodesEs);
        realAdminConfig.setClusterName(cluster);
        realAdminConfig.setWorkspaceUrl("http://localhost:" + PORT_SERVICE_WORKSPACE);
        PropertiesUtils.writeYaml(adminConfig, realAdminConfig);
        LOGGER.warn("=== VitamServerRunner start  AdminManagementMain");
        AdminManagementClientFactory
            .changeMode(new ClientConfigurationImpl("localhost", PORT_SERVICE_FUNCTIONAL_ADMIN));
        adminManagementMain = new AdminManagementMain(adminConfig.getAbsolutePath());
        adminManagementMain.start();

        // Wait startup
    }


    public void stopAdminManagementServer(boolean mockWhenStop) throws VitamApplicationServerException {
        if (null == adminManagementMain) {
            if (mockWhenStop) {
                AdminManagementClientFactory.getInstance()
                    .setVitamClientType(VitamClientFactoryInterface.VitamClientType.MOCK);
            }
            return;
        }
        LOGGER.warn("=== VitamServerRunner start  AdminManagementMain");
        adminManagementMain.stop();
        adminManagementMain = null;

        // Wait stop
        if (mockWhenStop) {
            AdminManagementClientFactory.getInstance()
                .setVitamClientType(VitamClientFactoryInterface.VitamClientType.MOCK);
        }
    }

    public void startLogbookServer()
        throws IOException, VitamApplicationServerException {

        if (null != logbookMain) {
            LogbookOperationsClientFactory
                .changeMode(new ClientConfigurationImpl("localhost", PORT_SERVICE_LOGBOOK));
            LogbookLifeCyclesClientFactory
                .changeMode(new ClientConfigurationImpl("localhost", PORT_SERVICE_LOGBOOK));
            return;
        }

        final List<ElasticsearchNode> nodesEs = new ArrayList<>();
        nodesEs.add(new ElasticsearchNode("localhost", ElasticsearchRule.getTcpPort()));

        SystemPropertyUtil
            .set(LogbookMain.PARAMETER_JETTY_SERVER_PORT, Integer.toString(PORT_SERVICE_LOGBOOK));
        final File logbookConfigFile = PropertiesUtils.findFile(LOGBOOK_CONF);
        final LogbookConfiguration logbookConfiguration =
            PropertiesUtils.readYaml(logbookConfigFile, LogbookConfiguration.class);
        logbookConfiguration.setElasticsearchNodes(nodesEs);
        logbookConfiguration.getMongoDbNodes().get(0).setDbPort(MongoRule.getDataBasePort());
        logbookConfiguration.setWorkspaceUrl("http://localhost:" + PORT_SERVICE_WORKSPACE);

        PropertiesUtils.writeYaml(logbookConfigFile, logbookConfiguration);

        LOGGER.warn("=== VitamServerRunner start  LogbookMain");
        LogbookOperationsClientFactory
            .changeMode(new ClientConfigurationImpl("localhost", PORT_SERVICE_LOGBOOK));
        LogbookLifeCyclesClientFactory
            .changeMode(new ClientConfigurationImpl("localhost", PORT_SERVICE_LOGBOOK));

        logbookMain = new LogbookMain(logbookConfigFile.getAbsolutePath());
        logbookMain.start();
        SystemPropertyUtil.clear(LogbookMain.PARAMETER_JETTY_SERVER_PORT);

        // Wait startup
    }

    private void stopLogbookServer(boolean mockWhenStop) throws VitamApplicationServerException {
        if (null == logbookMain) {
            if (mockWhenStop) {
                LogbookOperationsClientFactory.getInstance()
                    .setVitamClientType(VitamClientFactoryInterface.VitamClientType.MOCK);
                LogbookLifeCyclesClientFactory.getInstance()
                    .setVitamClientType(VitamClientFactoryInterface.VitamClientType.MOCK);
            }
            return;
        }
        LOGGER.warn("=== VitamServerRunner start  LogbookMain");
        logbookMain.stop();
        logbookMain = null;
        // Wait stop
        if (mockWhenStop) {
            LogbookOperationsClientFactory.getInstance()
                .setVitamClientType(VitamClientFactoryInterface.VitamClientType.MOCK);
            LogbookLifeCyclesClientFactory.getInstance()
                .setVitamClientType(VitamClientFactoryInterface.VitamClientType.MOCK);
        }
    }

    public void startWorkspaceServer() throws IOException, VitamApplicationServerException {
        if (null != workspaceMain) {
            WorkspaceClientFactory.changeMode(WORKSPACE_URL);
            return;
        }
        SystemPropertyUtil.set(WorkspaceMain.PARAMETER_JETTY_SERVER_PORT,
            Integer.toString(PORT_SERVICE_WORKSPACE));

        final File workspaceConfigFile = PropertiesUtils.findFile(WORKSPACE_CONF);

        fr.gouv.vitam.common.storage.StorageConfiguration workspaceConfiguration =
            PropertiesUtils
                .readYaml(workspaceConfigFile, fr.gouv.vitam.common.storage.StorageConfiguration.class);
        workspaceConfiguration.setStoragePath(VitamConfiguration.getVitamDataFolder() + "/storage/");

        writeYaml(workspaceConfigFile, workspaceConfiguration);

        LOGGER.warn("=== VitamServerRunner start  WorkspaceMain");
        WorkspaceClientFactory.changeMode(WORKSPACE_URL);
        workspaceMain = new WorkspaceMain(workspaceConfigFile.getAbsolutePath());
        workspaceMain.start();
        SystemPropertyUtil.clear(WorkspaceMain.PARAMETER_JETTY_SERVER_PORT);
    }


    private void stopWorkspaceServer() throws VitamApplicationServerException {
        if (null == workspaceMain) {
            return;
        }
        LOGGER.warn("=== VitamServerRunner start  WorkspaceMain");
        workspaceMain.stop();
        workspaceMain = null;
    }


    public void startMetadataServer()
        throws IOException, VitamApplicationServerException {
        if (null != metadataMain) {
            MetaDataClientFactory.changeMode(new ClientConfigurationImpl("localhost", PORT_SERVICE_METADATA));
            return;
        }

        final List<ElasticsearchNode> nodesEs = new ArrayList<>();
        nodesEs.add(new ElasticsearchNode("localhost", ElasticsearchRule.getTcpPort()));
        SystemPropertyUtil
            .set(MetadataMain.PARAMETER_JETTY_SERVER_PORT, Integer.toString(PORT_SERVICE_METADATA));
        SystemPropertyUtil
            .set(JunitHelper.PARAMETER_JETTY_SERVER_PORT_ADMIN,
                Integer.toString(PORT_SERVICE_METADATA_ADMIN));
        final File metadataConfig = PropertiesUtils.findFile(METADATA_CONF);
        final MetaDataConfiguration realMetadataConfig =
            PropertiesUtils.readYaml(metadataConfig, MetaDataConfiguration.class);
        realMetadataConfig.getMongoDbNodes().get(0).setDbPort(MongoRule.getDataBasePort());
        realMetadataConfig.setDbName(dbname);
        realMetadataConfig.setElasticsearchNodes(nodesEs);
        realMetadataConfig.setClusterName(cluster);

        PropertiesUtils.writeYaml(metadataConfig, realMetadataConfig);

        LOGGER.warn("=== VitamServerRunner start  MetadataMain");
        MetaDataClientFactory.changeMode(new ClientConfigurationImpl("localhost", PORT_SERVICE_METADATA));
        metadataMain = new MetadataMain(metadataConfig.getAbsolutePath());
        metadataMain.start();
        SystemPropertyUtil.clear(MetadataMain.PARAMETER_JETTY_SERVER_PORT);
        waitServer(true, MetaDataClientFactory.getInstance().getClient());
    }

    public void stopMetadataServer(boolean mockWhenStop) throws VitamApplicationServerException {
        if (null == metadataMain) {
            if (mockWhenStop) {
                MetaDataClientFactory.changeMode(null);
            }
            return;
        }
        LOGGER.warn("=== VitamServerRunner start  MetadataMain");
        metadataMain.stop();
        metadataMain = null;
        MetaDataClientFactory.changeMode(new ClientConfigurationImpl("localhost", PORT_SERVICE_METADATA));
        waitServer(false, MetaDataClientFactory.getInstance().getClient());
        if (mockWhenStop) {
            MetaDataClientFactory.changeMode(null);
        }

    }



    public void startProcessManagementServer() throws VitamApplicationServerException {
        if (null != processManagementMain) {
            ProcessingManagementClientFactory
                .changeMode(new ClientConfigurationImpl("localhost", PORT_SERVICE_PROCESSING));
            return;
        }
        SystemPropertyUtil.set(ProcessManagementMain.PARAMETER_JETTY_SERVER_PORT,
            Integer.toString(PORT_SERVICE_PROCESSING));
        LOGGER.warn("=== VitamServerRunner start  ProcessManagementMain");
        ProcessingManagementClientFactory
            .changeMode(new ClientConfigurationImpl("localhost", PORT_SERVICE_PROCESSING));
        processManagementMain = new ProcessManagementMain(PROCESSING_CONF);
        processManagementMain.start();
        SystemPropertyUtil.clear(ProcessManagementMain.PARAMETER_JETTY_SERVER_PORT);

        waitServer(true, ProcessingManagementClientFactory.getInstance().getClient());
    }

    public void stopProcessManagementServer(boolean mockWhenStop) throws VitamApplicationServerException {
        if (null == processManagementMain || mockWhenStop) {
            if (mockWhenStop) {
                ProcessingManagementClientFactory.changeMode(null);
            }
            return;
        }
        LOGGER.warn("=== VitamServerRunner start  ProcessManagementMain");
        processManagementMain.stop();
        processManagementMain = null;
        ProcessingManagementClientFactory
            .changeMode(new ClientConfigurationImpl("localhost", PORT_SERVICE_PROCESSING));
        waitServer(false, ProcessingManagementClientFactory.getInstance().getClient());
        if (mockWhenStop) {
            ProcessingManagementClientFactory.changeMode(null);
        }
    }

    public void startWorkerServer(String conf) throws PluginException, IOException, VitamApplicationServerException {
        if (null != workerMain) {
            return;
        }

        SystemPropertyUtil
            .set(WorkerMain.PARAMETER_JETTY_SERVER_PORT, Integer.toString(PORT_SERVICE_WORKER));
        LOGGER.warn("=== VitamServerRunner start  WorkerMain");
        workerMain = new WorkerMain(conf);
        workerMain.start();
        SystemPropertyUtil.clear(WorkerMain.PARAMETER_JETTY_SERVER_PORT);
        waitServer(true,
            WorkerClientFactory.getInstance(new WorkerClientConfiguration("localhost", PORT_SERVICE_WORKER))
                .getClient());

    }

    public void stopWorkerServer() throws VitamApplicationServerException {
        if (null == workerMain) {
            return;
        }
        LOGGER.warn("=== VitamServerRunner start  WorkerMain");
        workerMain.stop();
        workerMain = null;
        waitServer(false,
            WorkerClientFactory.getInstance(new WorkerClientConfiguration("localhost", PORT_SERVICE_WORKER))
                .getClient());
    }

    @Override
    protected void after() {
        try {
            cleanOffers();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            LOGGER.warn(
                ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> End " + clazz.getSimpleName() +
                    " <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
        }

    }


    void waitServerStartOrStop(boolean start) {
        if (null != workspaceMain) {
            waitServer(start, WorkspaceClientFactory.getInstance().getClient());
        }
        if (null != metadataMain) {
            waitServer(start, MetaDataClientFactory.getInstance().getClient());
        }
        if (null != processManagementMain) {
            waitServer(start, ProcessingManagementClientFactory.getInstance().getClient());
        }
        if (null != workerMain) {
            waitServer(start, ProcessingManagementClientFactory.getInstance().getClient());
        }
        if (null != defaultOfferMain) {
            waitServer(start, ProcessingManagementClientFactory.getInstance().getClient());
        }
        if (null != storageMain) {
            waitServer(start, StorageClientFactory.getInstance().getClient());
        }
        if (null != adminManagementMain) {
            waitServer(start, AdminManagementClientFactory.getInstance().getClient());
        }
        if (null != logbookMain) {
            waitServer(start, LogbookOperationsClientFactory.getInstance().getClient());
        }

        if (null != accessInternalMain) {
            waitServer(start, AccessInternalClientFactory.getInstance().getClient());
        }
        if (null != ingestInternalMain) {
            waitServer(start, IngestInternalClientFactory.getInstance().getClient());
        }
        if (null != batchReportMain) {
            waitServer(start, BatchReportClientFactory.getInstance().getClient());
        }
    }

    private void waitServer(boolean start, MockOrRestClient mockOrRestClient) {
        int nbTry = 500;
        if (start) {
            while (!checkStatus(mockOrRestClient)) {
                try {
                    Thread.sleep(60);
                } catch (InterruptedException e) {
                    SysErrLogger.FAKE_LOGGER.ignoreLog(e);
                }
                nbTry--;

                if (nbTry < 0) {
                    break;
                }
            }
        } else {
            while (checkStatus(mockOrRestClient)) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    SysErrLogger.FAKE_LOGGER.ignoreLog(e);
                }
                nbTry--;

                if (nbTry < 0) {
                    break;
                }
            }
        }
    }

    private boolean checkStatus(MockOrRestClient mockOrRestClient) {
        try {
            mockOrRestClient.checkStatus();
            return true;
        } catch (Exception e) {
            return false;
        }
    }


    /**
     * Clean offers content.
     */
    public static void cleanOffers() {
        // ugly style but we don't have the digest herelo
        File directory = new File(OFFER_FOLDER);
        if (directory.exists()) {
            try {
                Files.walk(directory.toPath())
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            } catch (IOException | IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
    }

    public void startServers() {
        before();
    }

    public void stopServers() {
        after();
    }
}
