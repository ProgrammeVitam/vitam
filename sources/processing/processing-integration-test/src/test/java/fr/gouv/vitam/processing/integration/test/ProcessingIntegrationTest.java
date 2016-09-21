package fr.gouv.vitam.processing.integration.test;

import static com.jayway.restassured.RestAssured.get;
import static org.elasticsearch.node.NodeBuilder.nodeBuilder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.InputStream;
import java.util.Map;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.restassured.RestAssured;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.VitamServer;
import fr.gouv.vitam.metadata.rest.MetaDataApplication;
import fr.gouv.vitam.processing.common.exception.ProcessingBadRequestException;
import fr.gouv.vitam.processing.common.model.ProcessStep;
import fr.gouv.vitam.processing.engine.core.monitoring.ProcessMonitoringImpl;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.rest.ProcessManagementApplication;
import fr.gouv.vitam.worker.server.rest.WorkerApplication;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import fr.gouv.vitam.workspace.rest.WorkspaceApplication;

/**
 * Processing integration test
 */
public class ProcessingIntegrationTest {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProcessingIntegrationTest.class);
    private static final int DATABASE_PORT = 12346;
    private static MongodExecutable mongodExecutable;
    static MongodProcess mongod;

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();
    private static File elasticsearchHome;
    private final static String CLUSTER_NAME = "vitam-cluster";
    private static int TCP_PORT = 54321;
    private static int HTTP_PORT = 54320;
    private static Node node;

    private static final int PORT_SERVICE_WORKER = 8098;
    private static final int PORT_SERVICE_WORKSPACE = 8094;
    private static final int PORT_SERVICE_METADATA = 8096;
    private static final int PORT_SERVICE_PROCESSING = 8097;

    private static final String SIP_FOLDER = "SIP";
    private static final String METADATA_PATH = "/metadata/v1";
    private static final String PROCESSING_PATH = "/processing/v1";
    private static final String WORKER_PATH = "/worker/v1";
    private static final String WORKSPACE_PATH = "/workspace/v1";

    private static String CONFIG_WORKER_PATH = "";
    private static String CONFIG_WORKSPACE_PATH = "";
    private static String CONFIG_METADATA_PATH = "";
    private static String CONFIG_PROCESSING_PATH = "";

    //private static VitamServer workerApplication;
    private static MetaDataApplication medtadataApplication;

    private WorkspaceClient workspaceClient;
    private ProcessingManagementClient processingClient;
    private static ProcessMonitoringImpl processMonitoring;

    private static final String WORKSPACE_URL = "http://localhost:" + PORT_SERVICE_WORKSPACE;
    private static final String METADATA_URL = "http://localhost:" + PORT_SERVICE_METADATA;
    private static final String PROCESSING_URL = "http://localhost:" + PORT_SERVICE_PROCESSING;

    private static String WORFKLOW_NAME = "DefaultIngestWorkflow";
    private static String CONTAINER_NAME = GUIDFactory.newGUID().toString();
    private static String SIP_FILE_OK_NAME = "integration/SIP.zip";
    private static String SIP_ARBO_COMPLEXE_FILE_OK = "integration/SIP_arbor_OK.zip";
    private static String SIP_WITHOUT_MANIFEST = "integration/SIP_no_manifest.zip";
    private static String SIP_NB_OBJ_INCORRECT_IN_MANIFEST = "integration/SIP_Conformity_KO.zip";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        CONFIG_METADATA_PATH = PropertiesUtils.getResourcesPath("integration/metadata.conf").toString();
        CONFIG_WORKER_PATH = PropertiesUtils.getResourcesPath("integration/worker.conf").toString();
        CONFIG_WORKSPACE_PATH = PropertiesUtils.getResourcesPath("integration/workspace.conf").toString();
        CONFIG_PROCESSING_PATH = PropertiesUtils.getResourcesPath("integration/processing.conf").toString();

        elasticsearchHome = tempFolder.newFolder();
        Settings settings = Settings.settingsBuilder()
            .put("http.enabled", true)
            .put("discovery.zen.ping.multicast.enabled", false)
            .put("transport.tcp.port", TCP_PORT)
            .put("http.port", HTTP_PORT)
            .put("path.home", elasticsearchHome.getCanonicalPath())
            .build();
        node = nodeBuilder()
            .settings(settings)
            .client(false)
            .clusterName(CLUSTER_NAME)
            .node();
        node.start();

        final MongodStarter starter = MongodStarter.getDefaultInstance();

        mongodExecutable = starter.prepare(new MongodConfigBuilder()
            .version(Version.Main.PRODUCTION)
            .net(new Net(DATABASE_PORT, Network.localhostIsIPv6()))
            .build());
        mongod = mongodExecutable.start();

        // launch metadata
        medtadataApplication = new MetaDataApplication();
        SystemPropertyUtil
            .set(MetaDataApplication.PARAMETER_JETTY_SERVER_PORT, Integer.toString(PORT_SERVICE_METADATA));
        medtadataApplication.configure(CONFIG_METADATA_PATH);

        // launch processing
        SystemPropertyUtil
            .set(ProcessManagementApplication.PARAMETER_JETTY_SERVER_PORT, Integer.toString(PORT_SERVICE_PROCESSING));
        ProcessManagementApplication.startApplication(CONFIG_PROCESSING_PATH);

        // launch worker
        SystemPropertyUtil
            .set(WorkerApplication.PARAMETER_JETTY_SERVER_PORT, Integer.toString(PORT_SERVICE_WORKER));
        WorkerApplication.startApplication(CONFIG_WORKER_PATH);

        // launch workspace
        SystemPropertyUtil
            .set(WorkspaceApplication.PARAMETER_JETTY_SERVER_PORT, Integer.toString(PORT_SERVICE_WORKSPACE));
        WorkspaceApplication.startApplication(CONFIG_WORKSPACE_PATH);

        processMonitoring = ProcessMonitoringImpl.getInstance();
    }

    @AfterClass
    public static void tearDownAfterClass() {
        mongod.stop();
        mongodExecutable.stop();
        node.close();
        try {
            WorkspaceApplication.stop();
            WorkerApplication.stop();
            ProcessManagementApplication.stop();
            MetaDataApplication.stop();
        } catch (Exception e) {
            LOGGER.error(e);
        }
    }

    @Test
    public void testServersStatus() throws Exception {
        RestAssured.port = PORT_SERVICE_PROCESSING;
        RestAssured.basePath = PROCESSING_PATH;
        get("/status").then().statusCode(200);

        RestAssured.port = PORT_SERVICE_WORKSPACE;
        RestAssured.basePath = WORKSPACE_PATH;
        get("/status").then().statusCode(200);

        RestAssured.port = PORT_SERVICE_METADATA;
        RestAssured.basePath = METADATA_PATH;
        get("/status").then().statusCode(200);

        RestAssured.port = PORT_SERVICE_WORKER;
        RestAssured.basePath = WORKER_PATH;
        get("/status").then().statusCode(200);
    }

    @Test
    public void testWorkflow() throws Exception {

        // workspace client dezip SIP in workspace
        RestAssured.port = PORT_SERVICE_WORKSPACE;
        RestAssured.basePath = WORKSPACE_PATH;

        InputStream zipInputStreamSipObject =
            Thread.currentThread().getContextClassLoader().getResourceAsStream(SIP_FILE_OK_NAME);
        workspaceClient = WorkspaceClientFactory.create(WORKSPACE_URL);
        workspaceClient.createContainer(CONTAINER_NAME);
        workspaceClient.unzipObject(CONTAINER_NAME, SIP_FOLDER, zipInputStreamSipObject);

        // call processing
        RestAssured.port = PORT_SERVICE_PROCESSING;
        RestAssured.basePath = PROCESSING_PATH;
        processingClient = new ProcessingManagementClient(PROCESSING_URL);
        String ret = processingClient.executeVitamProcess(CONTAINER_NAME, WORFKLOW_NAME);
        assertNotNull(ret);
        JsonNode node = JsonHandler.getFromString(ret);
        assertNotNull(node);
        assertEquals("OK", node.get("status").asText());

        // checkMonitoring - meaning something has been added in the monitoring tool
        Map<String, ProcessStep> map = processMonitoring.getWorkflowStatus(node.get("processId").asText());
        assertNotNull(map);
    }

    @Test
    public void testWorkflow_with_complexe_unit_seda() throws Exception {
        String containerName = GUIDFactory.newManifestGUID(0).getId();

        // workspace client dezip SIP in workspace
        RestAssured.port = PORT_SERVICE_WORKSPACE;
        RestAssured.basePath = WORKSPACE_PATH;

        InputStream zipInputStreamSipObject =
            Thread.currentThread().getContextClassLoader().getResourceAsStream(SIP_ARBO_COMPLEXE_FILE_OK);
        workspaceClient = WorkspaceClientFactory.create(WORKSPACE_URL);
        workspaceClient.createContainer(containerName);
        workspaceClient.unzipObject(containerName, SIP_FOLDER, zipInputStreamSipObject);

        // call processing
        RestAssured.port = PORT_SERVICE_PROCESSING;
        RestAssured.basePath = PROCESSING_PATH;
        processingClient = new ProcessingManagementClient(PROCESSING_URL);
        String ret = processingClient.executeVitamProcess(containerName, WORFKLOW_NAME);
        assertNotNull(ret);
        JsonNode node = JsonHandler.getFromString(ret);
        assertNotNull(node);

        assertEquals("OK", node.get("status").asText());
    }

    @Test(expected = ProcessingBadRequestException.class)
    public void testWorkflowWithSipNoManifest() throws Exception {
        String containerName = GUIDFactory.newManifestGUID(0).getId();

        // workspace client dezip SIP in workspace
        RestAssured.port = PORT_SERVICE_WORKSPACE;
        RestAssured.basePath = WORKSPACE_PATH;

        InputStream zipInputStreamSipObject =
            Thread.currentThread().getContextClassLoader().getResourceAsStream(SIP_WITHOUT_MANIFEST);
        workspaceClient = WorkspaceClientFactory.create(WORKSPACE_URL);
        workspaceClient.createContainer(containerName);
        workspaceClient.unzipObject(containerName, SIP_FOLDER, zipInputStreamSipObject);

        // call processing
        RestAssured.port = PORT_SERVICE_PROCESSING;
        RestAssured.basePath = PROCESSING_PATH;
        processingClient = new ProcessingManagementClient(PROCESSING_URL);
        processingClient.executeVitamProcess(containerName, WORFKLOW_NAME);
    }

    @Test(expected = ProcessingBadRequestException.class)
    public void testWorkflowWithManifestIncorrectObjectNumber() throws Exception {
        String containerName = GUIDFactory.newManifestGUID(0).getId();

        // workspace client dezip SIP in workspace
        RestAssured.port = PORT_SERVICE_WORKSPACE;
        RestAssured.basePath = WORKSPACE_PATH;

        InputStream zipInputStreamSipObject =
            Thread.currentThread().getContextClassLoader().getResourceAsStream(SIP_NB_OBJ_INCORRECT_IN_MANIFEST);
        workspaceClient = WorkspaceClientFactory.create(WORKSPACE_URL);
        workspaceClient.createContainer(containerName);
        workspaceClient.unzipObject(containerName, SIP_FOLDER, zipInputStreamSipObject);

        // call processing
        RestAssured.port = PORT_SERVICE_PROCESSING;
        RestAssured.basePath = PROCESSING_PATH;
        processingClient = new ProcessingManagementClient(PROCESSING_URL);
        // An action returns KO => the step is in KO => the workflow is OK
        processingClient.executeVitamProcess(containerName, WORFKLOW_NAME);
    }
}
