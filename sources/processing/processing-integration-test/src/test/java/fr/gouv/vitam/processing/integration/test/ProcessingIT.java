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
package fr.gouv.vitam.processing.integration.test;

import static com.jayway.restassured.RestAssured.get;
import static org.elasticsearch.node.NodeBuilder.nodeBuilder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.InputStream;
import java.util.Map;

import javax.ws.rs.core.Response.Status;

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
import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.format.identification.FormatIdentifierFactory;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.rest.AdminManagementApplication;
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

//
/**
 * Processing integration test
 */
public class ProcessingIT {
    private static final String JETTY_FUNCTIONAL_ADMIN_PORT = "jetty.functional-admin.port";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProcessingIT.class);
    private static final int DATABASE_PORT = 12346;
    private static MongodExecutable mongodExecutable;
    static MongodProcess mongod;

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    private static File elasticsearchHome;
    private final static String CLUSTER_NAME = "vitam-cluster";
    static JunitHelper junitHelper;
    private static int TCP_PORT = 54321;
    private static int HTTP_PORT = 54320;
    private static Node node;

    private static final int PORT_SERVICE_WORKER = 8098;
    private static final int PORT_SERVICE_WORKSPACE = 8094;
    private static final int PORT_SERVICE_METADATA = 8096;
    private static final int PORT_SERVICE_PROCESSING = 8097;
    private static final int PORT_SERVICE_FUNCTIONAL_ADMIN = 8093;

    private static final String SIP_FOLDER = "SIP";
    private static final String METADATA_PATH = "/metadata/v1";
    private static final String PROCESSING_PATH = "/processing/v1";
    private static final String WORKER_PATH = "/worker/v1";
    private static final String WORKSPACE_PATH = "/workspace/v1";

    private static String CONFIG_WORKER_PATH = "";
    private static String CONFIG_WORKSPACE_PATH = "";
    private static String CONFIG_METADATA_PATH = "";
    private static String CONFIG_PROCESSING_PATH = "";
    private static String CONFIG_FUNCTIONAL_ADMIN_PATH = "";
    private static String CONFIG_FUNCTIONAL_CLIENT_PATH = "";
    private static String CONFIG_SIEGFRIED_PATH = "";

    // private static VitamServer workerApplication;
    private static MetaDataApplication medtadataApplication;
    private static WorkerApplication wkrapplication;

    private WorkspaceClient workspaceClient;
    private ProcessingManagementClient processingClient;
    private static ProcessMonitoringImpl processMonitoring;

    private static final String WORKSPACE_URL = "http://localhost:" + PORT_SERVICE_WORKSPACE;
    private static final String PROCESSING_URL = "http://localhost:" + PORT_SERVICE_PROCESSING;

    private static String WORFKLOW_NAME = "DefaultIngestWorkflow";
    private static String CONTAINER_NAME;
    private static String SIP_FILE_OK_NAME = "integration/SIP-test.zip";
    private static String SIP_FILE_TAR_OK_NAME = "integration/SIP.tar";

    private static String SIP_ARBO_COMPLEXE_FILE_OK = "integration/SIP_arbor_OK.zip";
    private static String SIP_FUND_REGISTER_OK = "integration/OK-registre-fonds.zip";
    private static String SIP_WITHOUT_MANIFEST = "integration/SIP_no_manifest.zip";
    private static String SIP_NO_FORMAT = "integration/SIP_NO_FORMAT.zip";
    private static String SIP_NO_FORMAT_NO_TAG = "integration/SIP_NO_FORMAT_TAG.zip";
    private static String SIP_NB_OBJ_INCORRECT_IN_MANIFEST = "integration/SIP_Conformity_KO.zip";
    private static String SIP_ORPHELINS = "integration/SIP-orphelins.zip";
    private static String SIP_OBJECT_SANS_GOT = "integration/SIP-objetssansGOT.zip";

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        mongod.stop();
        mongodExecutable.stop();
        node.close();
        try {
            WorkspaceApplication.stop();
            wkrapplication.stop();
            ProcessManagementApplication.stop();
            MetaDataApplication.stop();
        } catch (final Exception e) {
            LOGGER.error(e);
        }
    }

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        elasticsearchHome = tempFolder.newFolder();
        CONFIG_METADATA_PATH = PropertiesUtils.getResourcePath("integration/metadata.conf").toString();
        CONFIG_WORKER_PATH = PropertiesUtils.getResourcePath("integration/worker.conf").toString();
        CONFIG_WORKSPACE_PATH = PropertiesUtils.getResourcePath("integration/workspace.conf").toString();
        CONFIG_PROCESSING_PATH = PropertiesUtils.getResourcePath("integration/processing.conf").toString();
        CONFIG_SIEGFRIED_PATH = PropertiesUtils.getResourcePath("integration/format-identifiers.conf").toString();
        CONFIG_FUNCTIONAL_ADMIN_PATH =
            PropertiesUtils.getResourcePath("integration/functional-administration.conf").toString();
        CONFIG_FUNCTIONAL_CLIENT_PATH =
            PropertiesUtils.getResourcePath("integration/functional-administration-client-it.conf").toString();

        final Settings settings = Settings.settingsBuilder()
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

        AdminManagementClientFactory.getInstance().changeConfigurationFile(CONFIG_FUNCTIONAL_CLIENT_PATH);
        // launch metadata
        medtadataApplication = new MetaDataApplication();
        SystemPropertyUtil.set(MetaDataApplication.PARAMETER_JETTY_SERVER_PORT,
            Integer.toString(PORT_SERVICE_METADATA));
        medtadataApplication.configure(CONFIG_METADATA_PATH);

        // launch processing
        SystemPropertyUtil.set(ProcessManagementApplication.PARAMETER_JETTY_SERVER_PORT,
            Integer.toString(PORT_SERVICE_PROCESSING));
        ProcessManagementApplication.startApplication(CONFIG_PROCESSING_PATH);

        // launch worker
        SystemPropertyUtil.set("jetty.worker.port", Integer.toString(PORT_SERVICE_WORKER));
        wkrapplication = new WorkerApplication(CONFIG_WORKER_PATH);
        wkrapplication.start();

        // launch workspace
        SystemPropertyUtil.set(WorkspaceApplication.PARAMETER_JETTY_SERVER_PORT,
            Integer.toString(PORT_SERVICE_WORKSPACE));
        WorkspaceApplication.startApplication(CONFIG_WORKSPACE_PATH);

        FormatIdentifierFactory.getInstance().changeConfigurationFile(CONFIG_SIEGFRIED_PATH);

        // launch functional Admin
        SystemPropertyUtil.set(JETTY_FUNCTIONAL_ADMIN_PORT, Integer.toString(PORT_SERVICE_FUNCTIONAL_ADMIN));
        AdminManagementApplication.startApplication(CONFIG_FUNCTIONAL_ADMIN_PATH);


        AdminManagementClient adminClient = AdminManagementClientFactory.getInstance().getAdminManagementClient();
        adminClient.importFormat(PropertiesUtils.getResourceAsStream("integration/DROID_SignatureFile_V88.xml"));

        processMonitoring = ProcessMonitoringImpl.getInstance();

        CONTAINER_NAME = GUIDFactory.newGUID().toString();
    }


    @Test
    public void testServersStatus() throws Exception {
        RestAssured.port = PORT_SERVICE_PROCESSING;
        RestAssured.basePath = PROCESSING_PATH;

        get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());

        RestAssured.port = PORT_SERVICE_WORKSPACE;
        RestAssured.basePath = WORKSPACE_PATH;
        get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());

        RestAssured.port = PORT_SERVICE_METADATA;
        RestAssured.basePath = METADATA_PATH;
        get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());

        RestAssured.port = PORT_SERVICE_WORKER;
        RestAssured.basePath = WORKER_PATH;
        get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());

    }

    @Test
    public void testWorkflow() throws Exception {

        final String containerName = GUIDFactory.newManifestGUID(0).getId();
        // workspace client dezip SIP in workspace
        RestAssured.port = PORT_SERVICE_WORKSPACE;
        RestAssured.basePath = WORKSPACE_PATH;
        final InputStream zipInputStreamSipObject =
            PropertiesUtils.getResourceAsStream(SIP_FILE_OK_NAME);
        workspaceClient = WorkspaceClientFactory.create(WORKSPACE_URL);
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP,
            zipInputStreamSipObject);
        // call processing
        RestAssured.port = PORT_SERVICE_PROCESSING;
        RestAssured.basePath = PROCESSING_PATH;
        processingClient = new ProcessingManagementClient(PROCESSING_URL);
        String ret = processingClient.executeVitamProcess(containerName, WORFKLOW_NAME);
        assertNotNull(ret);
        JsonNode node = JsonHandler.getFromString(ret);
        assertNotNull(node);
        assertEquals("OK", node.get("status").asText());

        // checkMonitoring - meaning something has been added in the monitoring tool
        Map<String, ProcessStep> map = processMonitoring.getWorkflowStatus(node.get("processId").asText());
        assertNotNull(map);
    }

    @Test
    public void testWorkflowWithTarSIP() throws Exception {
        final String containerName = GUIDFactory.newManifestGUID(0).getId();
        // workspace client dezip SIP in workspace
        RestAssured.port = PORT_SERVICE_WORKSPACE;
        RestAssured.basePath = WORKSPACE_PATH;

        InputStream zipInputStreamSipObject =
            Thread.currentThread().getContextClassLoader().getResourceAsStream(SIP_FILE_TAR_OK_NAME);
        workspaceClient = WorkspaceClientFactory.create(WORKSPACE_URL);
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.TAR,
            zipInputStreamSipObject);

        // call processing
        RestAssured.port = PORT_SERVICE_PROCESSING;
        RestAssured.basePath = PROCESSING_PATH;
        processingClient = new ProcessingManagementClient(PROCESSING_URL);
        final String ret = processingClient.executeVitamProcess(containerName, WORFKLOW_NAME);
        assertNotNull(ret);
        final JsonNode node = JsonHandler.getFromString(ret);
        assertNotNull(node);
        assertEquals("OK", node.get("status").asText());

        // checkMonitoring - meaning something has been added in the monitoring tool
        final Map<String, ProcessStep> map = processMonitoring.getWorkflowStatus(node.get("processId").asText());
        assertNotNull(map);
    }

    @Test
    public void testWorkflow_with_complexe_unit_seda() throws Exception {
        final String containerName = GUIDFactory.newManifestGUID(0).getId();

        // workspace client dezip SIP in workspace
        RestAssured.port = PORT_SERVICE_WORKSPACE;
        RestAssured.basePath = WORKSPACE_PATH;

        final InputStream zipInputStreamSipObject =
            PropertiesUtils.getResourceAsStream(SIP_ARBO_COMPLEXE_FILE_OK);
        workspaceClient = WorkspaceClientFactory.create(WORKSPACE_URL);
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP, zipInputStreamSipObject);

        // call processing
        RestAssured.port = PORT_SERVICE_PROCESSING;
        RestAssured.basePath = PROCESSING_PATH;
        processingClient = new ProcessingManagementClient(PROCESSING_URL);
        final String ret = processingClient.executeVitamProcess(containerName, WORFKLOW_NAME);
        assertNotNull(ret);
        final JsonNode node = JsonHandler.getFromString(ret);
        assertNotNull(node);

        assertEquals("OK", node.get("status").asText());
    }

    @Test
    public void testWorkflow_with_accession_register() throws Exception {
        final String containerName = GUIDFactory.newManifestGUID(0).getId();

        // workspace client dezip SIP in workspace
        RestAssured.port = PORT_SERVICE_WORKSPACE;
        RestAssured.basePath = WORKSPACE_PATH;

        final InputStream zipInputStreamSipObject =
            PropertiesUtils.getResourceAsStream(SIP_FUND_REGISTER_OK);
        workspaceClient = WorkspaceClientFactory.create(WORKSPACE_URL);
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP, zipInputStreamSipObject);

        // call processing
        RestAssured.port = PORT_SERVICE_PROCESSING;
        RestAssured.basePath = PROCESSING_PATH;
        processingClient = new ProcessingManagementClient(PROCESSING_URL);
        final String ret = processingClient.executeVitamProcess(containerName, WORFKLOW_NAME);
        assertNotNull(ret);
        final JsonNode node = JsonHandler.getFromString(ret);
        assertNotNull(node);

        assertEquals("OK", node.get("status").asText());
    }

    @Test(expected = ProcessingBadRequestException.class)
    public void testWorkflowWithSipNoManifest() throws Exception {
        final String containerName = GUIDFactory.newManifestGUID(0).getId();

        // workspace client dezip SIP in workspace
        RestAssured.port = PORT_SERVICE_WORKSPACE;
        RestAssured.basePath = WORKSPACE_PATH;

        final InputStream zipInputStreamSipObject =
            PropertiesUtils.getResourceAsStream(SIP_WITHOUT_MANIFEST);
        workspaceClient = WorkspaceClientFactory.create(WORKSPACE_URL);
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP, zipInputStreamSipObject);

        // call processing
        RestAssured.port = PORT_SERVICE_PROCESSING;
        RestAssured.basePath = PROCESSING_PATH;
        processingClient = new ProcessingManagementClient(PROCESSING_URL);
        processingClient.executeVitamProcess(containerName, WORFKLOW_NAME);
    }

    @Test
    public void testWorkflowSipNoFormat() throws Exception {
        final String containerName = GUIDFactory.newManifestGUID(0).getId();

        // workspace client dezip SIP in workspace
        RestAssured.port = PORT_SERVICE_WORKSPACE;
        RestAssured.basePath = WORKSPACE_PATH;

        final InputStream zipInputStreamSipObject =
            PropertiesUtils.getResourceAsStream(SIP_NO_FORMAT);
        workspaceClient = WorkspaceClientFactory.create(WORKSPACE_URL);
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP, zipInputStreamSipObject);

        // call processing
        RestAssured.port = PORT_SERVICE_PROCESSING;
        RestAssured.basePath = PROCESSING_PATH;
        processingClient = new ProcessingManagementClient(PROCESSING_URL);
        final String ret = processingClient.executeVitamProcess(containerName, WORFKLOW_NAME);
        assertNotNull(ret);
        final JsonNode node = JsonHandler.getFromString(ret);
        assertNotNull(node);
        assertEquals("OK", node.get("status").asText());

        // checkMonitoring - meaning something has been added in the monitoring tool
        final Map<String, ProcessStep> map = processMonitoring.getWorkflowStatus(node.get("processId").asText());
        assertNotNull(map);
    }


    @Test(expected = ProcessingBadRequestException.class)
    public void testWorkflowSipNoFormatNoTag() throws Exception {
        final String containerName = GUIDFactory.newManifestGUID(0).getId();

        // workspace client dezip SIP in workspace
        RestAssured.port = PORT_SERVICE_WORKSPACE;
        RestAssured.basePath = WORKSPACE_PATH;

        final InputStream zipInputStreamSipObject =
            PropertiesUtils.getResourceAsStream(SIP_NO_FORMAT_NO_TAG);
        workspaceClient = WorkspaceClientFactory.create(WORKSPACE_URL);
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP, zipInputStreamSipObject);
        // call processing
        RestAssured.port = PORT_SERVICE_PROCESSING;
        RestAssured.basePath = PROCESSING_PATH;
        processingClient = new ProcessingManagementClient(PROCESSING_URL);
        processingClient.executeVitamProcess(containerName, WORFKLOW_NAME);
    }



    @Test(expected = ProcessingBadRequestException.class)
    public void testWorkflowWithManifestIncorrectObjectNumber() throws Exception {
        final String containerName = GUIDFactory.newManifestGUID(0).getId();

        // workspace client dezip SIP in workspace
        RestAssured.port = PORT_SERVICE_WORKSPACE;
        RestAssured.basePath = WORKSPACE_PATH;

        final InputStream zipInputStreamSipObject =
            PropertiesUtils.getResourceAsStream(SIP_NB_OBJ_INCORRECT_IN_MANIFEST);
        workspaceClient = WorkspaceClientFactory.create(WORKSPACE_URL);
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP, zipInputStreamSipObject);

        // call processing
        RestAssured.port = PORT_SERVICE_PROCESSING;
        RestAssured.basePath = PROCESSING_PATH;
        processingClient = new ProcessingManagementClient(PROCESSING_URL);
        // An action returns KO => the step is in KO => the workflow is OK
        processingClient.executeVitamProcess(containerName, WORFKLOW_NAME);
    }

    @Test(expected = ProcessingBadRequestException.class)
    public void testWorkflowWithOrphelins() throws Exception {
        final String containerName = GUIDFactory.newManifestGUID(0).getId();

        // workspace client dezip SIP in workspace
        RestAssured.port = PORT_SERVICE_WORKSPACE;
        RestAssured.basePath = WORKSPACE_PATH;

        final InputStream zipInputStreamSipObject =
            PropertiesUtils.getResourceAsStream(SIP_ORPHELINS);
        workspaceClient = WorkspaceClientFactory.create(WORKSPACE_URL);
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP, zipInputStreamSipObject);

        // call processing
        RestAssured.port = PORT_SERVICE_PROCESSING;
        RestAssured.basePath = PROCESSING_PATH;
        processingClient = new ProcessingManagementClient(PROCESSING_URL);
        processingClient.executeVitamProcess(containerName, WORFKLOW_NAME);
    }


    @Test
    public void testWorkflow_withoutObjectGroups() throws Exception {
        final String containerName = GUIDFactory.newManifestGUID(0).getId();

        // workspace client dezip SIP in workspace
        RestAssured.port = PORT_SERVICE_WORKSPACE;
        RestAssured.basePath = WORKSPACE_PATH;

        final InputStream zipInputStreamSipObject =
            PropertiesUtils.getResourceAsStream(SIP_OBJECT_SANS_GOT);
        workspaceClient = WorkspaceClientFactory.create(WORKSPACE_URL);
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP, zipInputStreamSipObject);

        // call processing
        RestAssured.port = PORT_SERVICE_PROCESSING;
        RestAssured.basePath = PROCESSING_PATH;
        processingClient = new ProcessingManagementClient(PROCESSING_URL);
        final String ret = processingClient.executeVitamProcess(containerName, WORFKLOW_NAME);
        assertNotNull(ret);
        final JsonNode node = JsonHandler.getFromString(ret);
        assertNotNull(node);

        assertEquals("OK", node.get("status").asText());
    }
}
