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
package fr.gouv.vitam.worker.server.integration;

import static com.jayway.restassured.RestAssured.get;
import static org.elasticsearch.node.NodeBuilder.nodeBuilder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import fr.gouv.vitam.metadata.rest.MetaDataApplication;
import fr.gouv.vitam.processing.common.exception.WorkerAlreadyExistsException;
import fr.gouv.vitam.processing.common.model.EngineResponse;
import fr.gouv.vitam.processing.common.model.StatusCode;
import fr.gouv.vitam.processing.common.model.Step;
import fr.gouv.vitam.processing.common.model.WorkerBean;
import fr.gouv.vitam.processing.common.model.WorkerRemoteConfiguration;
import fr.gouv.vitam.processing.common.parameter.DefaultWorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.processing.management.rest.ProcessManagementApplication;
import fr.gouv.vitam.worker.client.WorkerClient;
import fr.gouv.vitam.worker.client.WorkerClientConfiguration;
import fr.gouv.vitam.worker.client.WorkerClientFactory;
import fr.gouv.vitam.worker.client.WorkerClientFactory.WorkerClientType;
import fr.gouv.vitam.worker.common.DescriptionStep;
import fr.gouv.vitam.worker.server.registration.WorkerRegister;
import fr.gouv.vitam.worker.server.rest.WorkerApplication;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import fr.gouv.vitam.workspace.rest.WorkspaceApplication;

/**
 * Worker integration test
 */
public class WorkerIT {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(WorkerIT.class);
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

    private static MetaDataApplication medtadataApplication;

    private WorkspaceClient workspaceClient;
    private WorkerClient workerClient;
    private ProcessingManagementClient processingClient;

    private static final String WORKSPACE_URL = "http://localhost:" + PORT_SERVICE_WORKSPACE;
    private static final String METADATA_URL = "http://localhost:" + PORT_SERVICE_METADATA;
    private static final String PROCESSING_URL = "http://localhost:" + PORT_SERVICE_PROCESSING;

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

        WorkerClientFactory.setConfiguration(WorkerClientType.WORKER, getWorkerClientConfiguration());
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

    private static WorkerClientConfiguration getWorkerClientConfiguration() {
        WorkerClientConfiguration workerClientConfiguration =
            new WorkerClientConfiguration("localhost",
                PORT_SERVICE_WORKER);
        return workerClientConfiguration;
    }

    @Test
    public void testServersStatus() throws Exception {
        RestAssured.port = PORT_SERVICE_WORKER;
        RestAssured.basePath = WORKER_PATH;
        get("/status").then().statusCode(200);

        RestAssured.port = PORT_SERVICE_WORKSPACE;
        RestAssured.basePath = WORKSPACE_PATH;
        get("/status").then().statusCode(200);

        RestAssured.port = PORT_SERVICE_PROCESSING;
        RestAssured.basePath = PROCESSING_PATH;
        get("/status").then().statusCode(200);

        RestAssured.port = PORT_SERVICE_METADATA;
        RestAssured.basePath = METADATA_PATH;
        get("/status").then().statusCode(200);
    }

    @Test
    public void testWorkflow() throws Exception {
        CONTAINER_NAME = GUIDFactory.newManifestGUID(0).getId();

        // workspace client dezip SIP in workspace
        RestAssured.port = PORT_SERVICE_WORKSPACE;
        RestAssured.basePath = WORKSPACE_PATH;

        InputStream zipInputStreamSipObject =
            Thread.currentThread().getContextClassLoader().getResourceAsStream(SIP_FILE_OK_NAME);
        workspaceClient = WorkspaceClientFactory.create(WORKSPACE_URL);
        workspaceClient.createContainer(CONTAINER_NAME);
        workspaceClient.unzipObject(CONTAINER_NAME, SIP_FOLDER, zipInputStreamSipObject);

        // call processing
        RestAssured.port = PORT_SERVICE_WORKER;
        RestAssured.basePath = WORKER_PATH;

        workerClient = WorkerClientFactory.getInstance().getWorkerClient();
        List<EngineResponse> retStepControl =
            workerClient.submitStep("resquestId", getDescriptionStep("integration/step_control_SIP.json"));
        assertNotNull(retStepControl);
        for (EngineResponse response : retStepControl) {
            assertEquals(StatusCode.OK, response.getStatus());
        }

        List<EngineResponse> retStepCheckStorage =
            workerClient.submitStep("resquestId", getDescriptionStep("integration/step_storage_SIP.json"));
        assertNotNull(retStepCheckStorage);
        for (EngineResponse response : retStepCheckStorage) {
            assertEquals(StatusCode.OK, response.getStatus());
        }

        DescriptionStep descriptionStepUnit = getDescriptionStep("integration/step_units_SIP.json");
        descriptionStepUnit.getWorkParams().setObjectName(unitName());
        List<EngineResponse> retStepStoreUnit = workerClient.submitStep("resquestId", descriptionStepUnit);
        assertNotNull(retStepStoreUnit);
        for (EngineResponse response : retStepStoreUnit) {
            assertEquals(StatusCode.OK, response.getStatus());
        }

        DescriptionStep descriptionStepOg = getDescriptionStep("integration/step_objects_SIP.json");
        descriptionStepOg.getWorkParams().setObjectName(objectGroupName());
        List<EngineResponse> retStepStoreOg = workerClient.submitStep("resquestId", descriptionStepOg);
        assertNotNull(retStepStoreOg);
        for (EngineResponse response : retStepStoreOg) {
            assertEquals(StatusCode.OK, response.getStatus());
        }

        workspaceClient.deleteContainer(CONTAINER_NAME);
    }


    @Test
    public void testWorkflow_with_complexe_unit_seda() throws Exception {
        CONTAINER_NAME = GUIDFactory.newManifestGUID(0).getId();

        // workspace client dezip SIP in workspace
        RestAssured.port = PORT_SERVICE_WORKSPACE;
        RestAssured.basePath = WORKSPACE_PATH;

        InputStream zipInputStreamSipObject =
            Thread.currentThread().getContextClassLoader().getResourceAsStream(SIP_ARBO_COMPLEXE_FILE_OK);
        workspaceClient = WorkspaceClientFactory.create(WORKSPACE_URL);
        workspaceClient.createContainer(CONTAINER_NAME);
        workspaceClient.unzipObject(CONTAINER_NAME, SIP_FOLDER, zipInputStreamSipObject);

        // call processing
        RestAssured.port = PORT_SERVICE_WORKER;
        RestAssured.basePath = WORKER_PATH;

        workerClient = WorkerClientFactory.getInstance().getWorkerClient();
        List<EngineResponse> retStepControl =
            workerClient.submitStep("resquestId", getDescriptionStep("integration/step_control_SIP.json"));
        assertNotNull(retStepControl);
        for (EngineResponse response : retStepControl) {
            assertEquals(StatusCode.OK, response.getStatus());
        }

        List<EngineResponse> retStepCheckStorage =
            workerClient.submitStep("resquestId", getDescriptionStep("integration/step_storage_SIP.json"));
        assertNotNull(retStepCheckStorage);
        for (EngineResponse response : retStepCheckStorage) {
            assertEquals(StatusCode.OK, response.getStatus());
        }

        DescriptionStep descriptionStepUnit = getDescriptionStep("integration/step_units_SIP.json");
        descriptionStepUnit.getWorkParams().setObjectName(unitName());
        List<EngineResponse> retStepStoreUnit = workerClient.submitStep("resquestId", descriptionStepUnit);
        assertNotNull(retStepStoreUnit);
        for (EngineResponse response : retStepStoreUnit) {
            assertEquals(StatusCode.OK, response.getStatus());
        }

        DescriptionStep descriptionStepOg = getDescriptionStep("integration/step_objects_SIP.json");
        descriptionStepOg.getWorkParams().setObjectName(objectGroupName());
        List<EngineResponse> retStepStoreOg = workerClient.submitStep("resquestId", descriptionStepOg);
        assertNotNull(retStepStoreOg);
        for (EngineResponse response : retStepStoreOg) {
            assertEquals(StatusCode.OK, response.getStatus());
        }

        workspaceClient.deleteContainer(CONTAINER_NAME);
    }

    @Test
    public void testWorkflowWithSipNoManifest() throws Exception {
        CONTAINER_NAME = GUIDFactory.newManifestGUID(0).getId();

        // workspace client dezip SIP in workspace
        RestAssured.port = PORT_SERVICE_WORKSPACE;
        RestAssured.basePath = WORKSPACE_PATH;

        InputStream zipInputStreamSipObject =
            Thread.currentThread().getContextClassLoader().getResourceAsStream(SIP_WITHOUT_MANIFEST);
        workspaceClient = WorkspaceClientFactory.create(WORKSPACE_URL);
        workspaceClient.createContainer(CONTAINER_NAME);
        workspaceClient.unzipObject(CONTAINER_NAME, SIP_FOLDER, zipInputStreamSipObject);

        // call processing
        RestAssured.port = PORT_SERVICE_WORKER;
        RestAssured.basePath = WORKER_PATH;

        workerClient = WorkerClientFactory.getInstance().getWorkerClient();
        List<EngineResponse> retStepControl =
            workerClient.submitStep("resquestId", getDescriptionStep("integration/step_control_SIP.json"));
        assertNotNull(retStepControl);
        for (EngineResponse response : retStepControl) {
            assertEquals(StatusCode.KO, response.getStatus());
        }

        workspaceClient.deleteContainer(CONTAINER_NAME);
    }

    @Test
    public void testWorkflowWithManifestIncorrectObjectNumber() throws Exception {
        CONTAINER_NAME = GUIDFactory.newManifestGUID(0).getId();

        // workspace client dezip SIP in workspace
        RestAssured.port = PORT_SERVICE_WORKSPACE;
        RestAssured.basePath = WORKSPACE_PATH;

        InputStream zipInputStreamSipObject =
            Thread.currentThread().getContextClassLoader().getResourceAsStream(SIP_NB_OBJ_INCORRECT_IN_MANIFEST);
        workspaceClient = WorkspaceClientFactory.create(WORKSPACE_URL);
        workspaceClient.createContainer(CONTAINER_NAME);
        workspaceClient.unzipObject(CONTAINER_NAME, SIP_FOLDER, zipInputStreamSipObject);

        // call processing
        RestAssured.port = PORT_SERVICE_WORKER;
        RestAssured.basePath = WORKER_PATH;

        workerClient = WorkerClientFactory.getInstance().getWorkerClient();
        List<EngineResponse> retStepControl =
            workerClient.submitStep("resquestId", getDescriptionStep("integration/step_control_SIP.json"));
        assertNotNull(retStepControl);
        assertEquals(5, retStepControl.size());
        assertEquals(StatusCode.OK, retStepControl.get(0).getStatus());
        assertEquals(StatusCode.OK, retStepControl.get(1).getStatus());
        assertEquals(StatusCode.OK, retStepControl.get(2).getStatus());
        assertEquals(StatusCode.OK, retStepControl.get(3).getStatus());
        assertEquals(StatusCode.KO, retStepControl.get(4).getStatus());

        workspaceClient.deleteContainer(CONTAINER_NAME);
    }

    @Test
    public void testRegistration() throws Exception {
        WorkerRemoteConfiguration remoteConfiguration = new WorkerRemoteConfiguration("localhost", PORT_SERVICE_WORKER);
        WorkerBean workerBean =
            new WorkerBean("name", WorkerRegister.DEFAULT_FAMILY, 1L, 1L, "active", remoteConfiguration);
        processingClient = ProcessingManagementClientFactory.create(PROCESSING_URL);
        try {
            processingClient.registerWorker(WorkerRegister.DEFAULT_FAMILY, "1", workerBean);
            fail("Should have raized an exception");
        } catch (WorkerAlreadyExistsException e) {
            processingClient.unregisterWorker(WorkerRegister.DEFAULT_FAMILY, "1");
        }
        processingClient.registerWorker(WorkerRegister.DEFAULT_FAMILY, "1", workerBean);
    }


    private DefaultWorkerParameters getWorkParams() {
        DefaultWorkerParameters workparams = (DefaultWorkerParameters) WorkerParametersFactory.newWorkerParameters();
        workparams.setContainerName(CONTAINER_NAME);
        return workparams;
    }

    private Step getStep(String stepFilePath) {
        final ObjectMapper objectMapper = new ObjectMapper();
        Step step = null;

        try {
            final InputStream inputJSON =
                Thread.currentThread().getContextClassLoader().getResourceAsStream(stepFilePath);
            step = objectMapper.readValue(inputJSON, Step.class);

        } catch (final IOException e) {
            LOGGER.error("Exception while retrieving step", e);
        }
        return step;
    }

    private DescriptionStep getDescriptionStep(String stepFilePath) {
        DescriptionStep descriptionStep = new DescriptionStep(getStep(stepFilePath), getWorkParams());
        descriptionStep.getWorkParams().setContainerName(CONTAINER_NAME);
        descriptionStep.getWorkParams().setCurrentStep(descriptionStep.getStep().getStepName());
        descriptionStep.getWorkParams().setUrlMetadata(METADATA_URL);
        descriptionStep.getWorkParams().setUrlWorkspace(WORKSPACE_URL);
        descriptionStep.getWorkParams().setObjectName("SIP/manifest.xml");
        return descriptionStep;
    }

    private String unitName() {
        String objectName = "";
        try {
            InputStream stream = workspaceClient.getObject(CONTAINER_NAME,
                "Maps/ARCHIVE_ID_TO_GUID_MAP.json");
            Map<String, Object> map = JsonHandler.getMapFromString(IOUtils.toString(stream, "UTF-8"));
            objectName = (String) map.values().iterator().next();
        } catch (Exception e) {
            LOGGER.error("Exception while retrieving unit", e);
        }
        return objectName + ".xml";
    }

    private String objectGroupName() {
        String objectName = "";
        try {
            InputStream stream = workspaceClient.getObject(CONTAINER_NAME,
                "Maps/OBJECT_GROUP_ID_TO_GUID_MAP.json");
            Map<String, Object> map = JsonHandler.getMapFromString(IOUtils.toString(stream, "UTF-8"));
            objectName = (String) map.values().iterator().next();
        } catch (Exception e) {
            LOGGER.error("Exception while retrieving objectGroup", e);
        }
        return objectName + ".json";
    }
}
