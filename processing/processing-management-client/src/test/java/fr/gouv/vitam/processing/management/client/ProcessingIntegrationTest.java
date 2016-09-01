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
package fr.gouv.vitam.processing.management.client;

import static com.jayway.restassured.RestAssured.get;
import static org.elasticsearch.node.NodeBuilder.nodeBuilder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
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
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.metadata.rest.MetaDataApplication;
import fr.gouv.vitam.processing.common.exception.ProcessingBadRequestException;
import fr.gouv.vitam.processing.common.model.ProcessStep;
import fr.gouv.vitam.processing.engine.core.monitoring.ProcessMonitoringImpl;
import fr.gouv.vitam.processing.management.rest.ProcessManagementApplication;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import fr.gouv.vitam.workspace.rest.WorkspaceApplication;

/**
 *
 */
public class ProcessingIntegrationTest {
    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();
    private static File elasticsearchHome;

    private final static String CLUSTER_NAME = "vitam-cluster";
    private static int TCP_PORT = 54321;
    private static int HTTP_PORT = 54320;
    private static Node node;
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProcessingIntegrationTest.class);
    private static final int DATABASE_PORT = 12346;
    private static MongodExecutable mongodExecutable;
    static MongodProcess mongod;

    private static int PORT_SERVICE_PROCESSING;
    private static final int PORT_SERVICE_WORKSPACE = 8094;
    private static final int PORT_SERVICE_METADATA = 8096;

    private static final String SIP_FOLDER = "SIP";
    private static final String METADATA_PATH = "/metadata/v1";
    private static final String PROCESSING_PATH = "/processing/api/v0.0.3";
    private static final String WORKSPACE_PATH = "/workspace/v1";

    private static Path CONFIG_PROCESSING_PATH;
    private static Path CONFIG_WORKSPACE_PATH;
    private static Path CONFIG_METADATA_PATH;

    private static JunitHelper junitHelper;

    private static ProcessManagementApplication processApplication;
    private static WorkspaceApplication workspaceApplication;
    private static MetaDataApplication medtadataApplication;

    private WorkspaceClient workspaceClient;
    private ProcessingManagementClient processingClient;

    private static ProcessMonitoringImpl processMonitoring;
    private static String WORKSPACE_URL;
    private static String PROCESSING_URL;

    private static String WORFKLOW_NAME = "DefaultIngestWorkflow";
    private static String CONTAINER_NAME = GUIDFactory.newGUID().toString();
    private static String SIP_FILE_OK_NAME = "SIP.zip";
    private static String SIP_ARBO_COMPLEXE_FILE_OK = "SIP_arbor_OK.zip";
    private static String SIP_WITHOUT_MANIFEST = "SIP_no_manifest.zip";
    private static String SIP_NB_OBJ_INCORRECT_IN_MANIFEST = "SIP_Conformity_KO.zip";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        CONFIG_METADATA_PATH = PropertiesUtils.getResourcesPath("metadata.conf");
        CONFIG_PROCESSING_PATH = PropertiesUtils.getResourcesPath("processing.conf");
        CONFIG_WORKSPACE_PATH = PropertiesUtils.getResourcesPath("workspace.conf");

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
        junitHelper = new JunitHelper();
        medtadataApplication = new MetaDataApplication();

        SystemPropertyUtil
            .set(MetaDataApplication.PARAMETER_JETTY_SERVER_PORT, Integer.toString(PORT_SERVICE_METADATA));
        medtadataApplication.configure("metadata.conf");


        // launch processing
        PORT_SERVICE_PROCESSING = junitHelper.findAvailablePort();
        PROCESSING_URL = "http://localhost:" + PORT_SERVICE_PROCESSING;
        SystemPropertyUtil
            .set(ProcessManagementApplication.PARAMETER_JETTY_SERVER_PORT, Integer.toString(PORT_SERVICE_PROCESSING));
        processApplication = new ProcessManagementApplication();
        ProcessManagementApplication.startApplication("processing.conf");

        // launch workspace
        WORKSPACE_URL = "http://localhost:" + PORT_SERVICE_WORKSPACE;
        SystemPropertyUtil
            .set(WorkspaceApplication.PARAMETER_JETTY_SERVER_PORT, Integer.toString(PORT_SERVICE_WORKSPACE));
        workspaceApplication = new WorkspaceApplication();
        WorkspaceApplication.startApplication("workspace.conf");

        CONTAINER_NAME = GUIDFactory.newGUID().toString();
        processMonitoring = ProcessMonitoringImpl.getInstance();
    }

    @AfterClass
    public static void tearDownAfterClass() {
        mongod.stop();
        mongodExecutable.stop();
        try {
            workspaceApplication.stop();
            processApplication.stop();
            medtadataApplication.stop();
            junitHelper.releasePort(PORT_SERVICE_METADATA);

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
    public void testWorkflowWithManifestIncorrectObject() throws Exception {
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
