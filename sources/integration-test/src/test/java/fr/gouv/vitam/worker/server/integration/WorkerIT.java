/*******************************************************************************
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
 *******************************************************************************/
package fr.gouv.vitam.worker.server.integration;

import static com.jayway.restassured.RestAssured.get;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
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
import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.ServerIdentity;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.client.BasicClient;
import fr.gouv.vitam.common.client.configuration.ClientConfiguration;
import fr.gouv.vitam.common.client.configuration.ClientConfigurationImpl;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.junit.JunitHelper.ElasticsearchTestConfiguration;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.logbook.rest.LogbookApplication;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.metadata.rest.MetaDataApplication;
import fr.gouv.vitam.processing.common.exception.WorkerAlreadyExistsException;
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
import fr.gouv.vitam.worker.common.DescriptionStep;
import fr.gouv.vitam.worker.server.registration.WorkerRegister;
import fr.gouv.vitam.worker.server.rest.WorkerApplication;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import fr.gouv.vitam.workspace.rest.WorkspaceApplication;

/**
 * Worker integration test TODO P1 : do a "worker-integration" module
 */
public class WorkerIT {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(WorkerIT.class);
    private static final int DATABASE_PORT = 12346;
    private static MongodExecutable mongodExecutable;
    static MongodProcess mongod;

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    private final static String CLUSTER_NAME = "vitam-cluster";
    private static int TCP_PORT = 54321;
    private static int HTTP_PORT = 54320;

    private static final int PORT_SERVICE_WORKER = 8098;
    private static final int PORT_SERVICE_WORKSPACE = 8094;
    private static final int PORT_SERVICE_METADATA = 8096;
    private static final int PORT_SERVICE_PROCESSING = 8097;
    private static final int PORT_SERVICE_LOGBOOK = 8099;

    private static final String SIP_FOLDER = "SIP";
    private static final String METADATA_PATH = "/metadata/v1";
    private static final String PROCESSING_PATH = "/processing/v1";
    private static final String WORKER_PATH = "/worker/v1";
    private static final String WORKSPACE_PATH = "/workspace/v1";

    private static String CONFIG_WORKER_PATH = "";
    private static String CONFIG_WORKER_CLIENT_PATH = "";

    private static String CONFIG_WORKSPACE_PATH = "";
    private static String CONFIG_METADATA_PATH = "";
    private static String CONFIG_PROCESSING_PATH = "";
    private static String CONFIG_LOGBOOK_PATH = "";
    private static MetaDataApplication metadataApplication;
    private static WorkerApplication wkrapplication;
    private static WorkspaceApplication workspaceApplication;
    private static ProcessManagementApplication processManagementApplication;

    private WorkspaceClient workspaceClient;
    private static LogbookApplication lgbapplication;
    private WorkerClient workerClient;
    private WorkerClientConfiguration workerClientConfiguration;

    private ProcessingManagementClient processingClient;
    private static ElasticsearchTestConfiguration config = null;

    private static final String WORKSPACE_URL = "http://localhost:" + PORT_SERVICE_WORKSPACE;
    private static final String METADATA_URL = "http://localhost:" + PORT_SERVICE_METADATA;
    private static final String PROCESSING_URL = "http://localhost:" + PORT_SERVICE_PROCESSING;

    private static String CONTAINER_NAME = GUIDFactory.newGUID().toString();
    private static String SIP_FILE_OK_NAME = "integration-worker/SIP.zip";
    private static String SIP_ARBO_COMPLEXE_FILE_OK = "integration-worker/SIP_arbor_OK.zip";
    private static String SIP_WITHOUT_MANIFEST = "integration-worker/SIP_no_manifest.zip";
    private static String SIP_CONFORMITY_KO = "integration-worker/SIP_Conformity_KO.zip";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        CONFIG_METADATA_PATH = PropertiesUtils.getResourcePath("integration-worker/metadata.conf").toString();
        CONFIG_WORKER_PATH = PropertiesUtils.getResourcePath("integration-worker/worker.conf").toString();
        CONFIG_WORKSPACE_PATH = PropertiesUtils.getResourcePath("integration-worker/workspace.conf").toString();
        CONFIG_PROCESSING_PATH = PropertiesUtils.getResourcePath("integration-worker/processing.conf").toString();
        CONFIG_LOGBOOK_PATH = PropertiesUtils.getResourcePath("integration-worker/logbook.conf").toString();
        CONFIG_WORKER_CLIENT_PATH = PropertiesUtils.getResourcePath("integration-worker/worker-client.conf").toString();

        // ES
        config = JunitHelper.startElasticsearchForTest(tempFolder, CLUSTER_NAME, TCP_PORT, HTTP_PORT);

        final MongodStarter starter = MongodStarter.getDefaultInstance();

        mongodExecutable = starter.prepare(new MongodConfigBuilder()
            .version(Version.Main.PRODUCTION)
            .net(new Net(DATABASE_PORT, Network.localhostIsIPv6()))
            .build());
        mongod = mongodExecutable.start();

        // launch metadata
        SystemPropertyUtil.set(MetaDataApplication.PARAMETER_JETTY_SERVER_PORT,
            Integer.toString(PORT_SERVICE_METADATA));
        metadataApplication = new MetaDataApplication(CONFIG_METADATA_PATH);
        metadataApplication.start();
        SystemPropertyUtil.clear(MetaDataApplication.PARAMETER_JETTY_SERVER_PORT);
        MetaDataClientFactory.changeMode(new ClientConfigurationImpl("localhost", PORT_SERVICE_METADATA));

        // launch logbook
        SystemPropertyUtil
            .set(LogbookApplication.PARAMETER_JETTY_SERVER_PORT, Integer.toString(PORT_SERVICE_LOGBOOK));
        lgbapplication = new LogbookApplication(CONFIG_LOGBOOK_PATH);
        lgbapplication.start();
        final ClientConfiguration configuration = new ClientConfigurationImpl("localhost", PORT_SERVICE_LOGBOOK);
        LogbookLifeCyclesClientFactory.changeMode(configuration);
        LogbookOperationsClientFactory.changeMode(configuration);

        // launch workspace
        SystemPropertyUtil
            .set(WorkspaceApplication.PARAMETER_JETTY_SERVER_PORT, Integer.toString(PORT_SERVICE_WORKSPACE));
        workspaceApplication = new WorkspaceApplication(CONFIG_WORKSPACE_PATH);
        workspaceApplication.start();
        WorkspaceClientFactory.changeMode(WORKSPACE_URL);

        // launch processing
        SystemPropertyUtil
            .set(ProcessManagementApplication.PARAMETER_JETTY_SERVER_PORT, Integer.toString(PORT_SERVICE_PROCESSING));
        processManagementApplication = new ProcessManagementApplication(CONFIG_PROCESSING_PATH);
        processManagementApplication.start();
        ProcessingManagementClientFactory.changeConfigurationUrl(PROCESSING_URL);

        // launch worker
        SystemPropertyUtil
            .set("jetty.worker.port", Integer.toString(PORT_SERVICE_WORKER));
        wkrapplication = new WorkerApplication(CONFIG_WORKER_PATH);
        wkrapplication.start();
        WorkerClientFactory.changeMode(getWorkerClientConfiguration());
    }

    @AfterClass
    public static void tearDownAfterClass() {
        if (config != null) {
            JunitHelper.stopElasticsearchForTest(config);
        }

        if (mongod == null) {
            return;
        }
        mongod.stop();
        mongodExecutable.stop();
        try {
            workspaceApplication.stop();
            wkrapplication.stop();
            lgbapplication.stop();
            processManagementApplication.stop();
            metadataApplication.stop();
        } catch (final Exception e) {
            LOGGER.error(e);
        }
    }

    private static WorkerClientConfiguration getWorkerClientConfiguration() {
        final WorkerClientConfiguration workerClientConfiguration =
            new WorkerClientConfiguration("localhost",
                PORT_SERVICE_WORKER);
        return workerClientConfiguration;
    }

    @Test
    public void testServersStatus() throws Exception {
        try {
            RestAssured.port = PORT_SERVICE_WORKER;
            RestAssured.basePath = WORKER_PATH;
            get(BasicClient.STATUS_URL).then().statusCode(204);

            RestAssured.port = PORT_SERVICE_WORKSPACE;
            RestAssured.basePath = WORKSPACE_PATH;
            get(BasicClient.STATUS_URL).then().statusCode(204);

            RestAssured.port = PORT_SERVICE_PROCESSING;
            RestAssured.basePath = PROCESSING_PATH;
            get(BasicClient.STATUS_URL).then().statusCode(204);

            RestAssured.port = PORT_SERVICE_METADATA;
            RestAssured.basePath = METADATA_PATH;
            get(BasicClient.STATUS_URL).then().statusCode(204);
        } catch (final Exception e) {
            e.printStackTrace();
            fail("should not raized an exception");
        }
    }

    private void printAndCheckXmlConfiguration() {
        LOGGER.warn("XML Configuration: " +
            "\n\tjavax.xml.parsers.SAXParserFactory: " + SystemPropertyUtil.get("javax.xml.parsers.SAXParserFactory") +
            "\n\tjavax.xml.parsers.DocumentBuilderFactory: " +
            SystemPropertyUtil.get("javax.xml.parsers.DocumentBuilderFactory") +
            "\n\tjavax.xml.datatype.DatatypeFactory: " + SystemPropertyUtil.get("javax.xml.datatype.DatatypeFactory") +
            "\n\tjavax.xml.stream.XMLEventFactory: " + SystemPropertyUtil.get("javax.xml.stream.XMLEventFactory") +
            "\n\tjavax.xml.stream.XMLInputFactory: " + SystemPropertyUtil.get("javax.xml.stream.XMLInputFactory") +
            "\n\tjavax.xml.stream.XMLOutputFactory: " + SystemPropertyUtil.get("javax.xml.stream.XMLOutputFactory") +
            "\n\tjavax.xml.transform.TransformerFactory: " +
            SystemPropertyUtil.get("javax.xml.transform.TransformerFactory") +
            "\n\tjavax.xml.validation.SchemaFactory: " + SystemPropertyUtil.get("javax.xml.validation.SchemaFactory") +
            "\n\tjavax.xml.xpath.XPathFactory: " + SystemPropertyUtil.get("javax.xml.xpath.XPathFactory"));
        try {
            LOGGER.warn("XML Implementation: " +
                "\n\tjavax.xml.parsers.SAXParserFactory: " +
                javax.xml.parsers.SAXParserFactory.newInstance().getClass() +
                "\n\tjavax.xml.parsers.DocumentBuilderFactory: " +
                javax.xml.parsers.DocumentBuilderFactory.newInstance().getClass() +
                "\n\tjavax.xml.datatype.DatatypeFactory: " +
                javax.xml.datatype.DatatypeFactory.newInstance().getClass() +
                "\n\tjavax.xml.stream.XMLEventFactory: " + javax.xml.stream.XMLEventFactory.newFactory().getClass() +
                "\n\tjavax.xml.stream.XMLInputFactory: " + javax.xml.stream.XMLInputFactory.newInstance().getClass() +
                "\n\tjavax.xml.stream.XMLOutputFactory: " + javax.xml.stream.XMLOutputFactory.newInstance().getClass() +
                "\n\tjavax.xml.transform.TransformerFactory: " +
                javax.xml.transform.TransformerFactory.newInstance().getClass() +
                "\n\tjavax.xml.validation.SchemaFactory: " +
                javax.xml.validation.SchemaFactory.newInstance("http://www.w3.org/XML/XMLSchema/v1.1").getClass() +
                "\n\tjavax.xml.xpath.XPathFactory: " + javax.xml.xpath.XPathFactory.newInstance().getClass());
        } catch (DatatypeConfigurationException | FactoryConfigurationError | TransformerFactoryConfigurationError e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testXsdConfiguration() {
        printAndCheckXmlConfiguration();
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflow() throws Exception {
        try {
            Integer tenantId = 0;
            VitamThreadUtils.getVitamSession().setTenantId(tenantId);
            CONTAINER_NAME = GUIDFactory.newManifestGUID(tenantId).getId();
            VitamThreadUtils.getVitamSession().setRequestId(CONTAINER_NAME);

            // workspace client dezip SIP in workspace
            RestAssured.port = PORT_SERVICE_WORKSPACE;
            RestAssured.basePath = WORKSPACE_PATH;

            final InputStream zipInputStreamSipObject =
                PropertiesUtils.getResourceAsStream(SIP_FILE_OK_NAME);
            workspaceClient = WorkspaceClientFactory.getInstance().getClient();
            workspaceClient.createContainer(CONTAINER_NAME);
            workspaceClient.uncompressObject(CONTAINER_NAME, SIP_FOLDER, CommonMediaType.ZIP, zipInputStreamSipObject);

            // call processing
            RestAssured.port = PORT_SERVICE_WORKER;
            RestAssured.basePath = WORKER_PATH;
            workerClientConfiguration = WorkerClientFactory.changeConfigurationFile("worker-client.conf");

            workerClient = WorkerClientFactory.getInstance(workerClientConfiguration).getClient();
            final ItemStatus retStepControl =
                workerClient.submitStep(getDescriptionStep("integration-worker/step_control_SIP.json"));
            assertNotNull(retStepControl);
            assertEquals(StatusCode.OK, retStepControl.getGlobalStatus());

            final ItemStatus retStepCheckStorage =
                workerClient.submitStep(getDescriptionStep("integration-worker/step_storage_SIP.json"));
            assertNotNull(retStepCheckStorage);
            assertEquals(StatusCode.OK, retStepCheckStorage.getGlobalStatus());

            final DescriptionStep descriptionStepUnit = getDescriptionStep("integration-worker/step_units_SIP.json");
            descriptionStepUnit.getWorkParams().setObjectName(unitName());
            final ItemStatus retStepStoreUnit = workerClient.submitStep(descriptionStepUnit);
            assertNotNull(retStepStoreUnit);
            assertEquals(StatusCode.OK, retStepStoreUnit.getGlobalStatus());

            final DescriptionStep descriptionStepOg = getDescriptionStep("integration-worker/step_objects_SIP.json");
            descriptionStepOg.getWorkParams().setObjectName(objectGroupName());
            final ItemStatus retStepStoreOg = workerClient.submitStep(descriptionStepOg);
            assertNotNull(retStepStoreOg);
            assertEquals(StatusCode.OK, retStepStoreOg.getGlobalStatus());

            workspaceClient.deleteContainer(CONTAINER_NAME, true);
        } catch (final Exception e) {
            e.printStackTrace();
            fail("should not raized an exception");
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflow_with_complexe_unit_seda() throws Exception {
        try {
            Integer tenantId = 0;
            VitamThreadUtils.getVitamSession().setTenantId(tenantId);
            CONTAINER_NAME = GUIDFactory.newManifestGUID(tenantId).getId();
            VitamThreadUtils.getVitamSession().setRequestId(CONTAINER_NAME);

            // workspace client dezip SIP in workspace
            RestAssured.port = PORT_SERVICE_WORKSPACE;
            RestAssured.basePath = WORKSPACE_PATH;

            final InputStream zipInputStreamSipObject =
                PropertiesUtils.getResourceAsStream(SIP_ARBO_COMPLEXE_FILE_OK);
            workspaceClient = WorkspaceClientFactory.getInstance().getClient();
            workspaceClient.createContainer(CONTAINER_NAME);
            workspaceClient.uncompressObject(CONTAINER_NAME, SIP_FOLDER, CommonMediaType.ZIP, zipInputStreamSipObject);


            // call processing
            RestAssured.port = PORT_SERVICE_WORKER;
            RestAssured.basePath = WORKER_PATH;
            workerClientConfiguration = WorkerClientFactory.changeConfigurationFile(CONFIG_WORKER_CLIENT_PATH);

            workerClient = WorkerClientFactory.getInstance(workerClientConfiguration).getClient();
            final ItemStatus retStepControl =
                workerClient.submitStep(getDescriptionStep("integration-worker/step_control_SIP.json"));
            assertNotNull(retStepControl);
            assertEquals(StatusCode.OK, retStepControl.getGlobalStatus());


            final ItemStatus retStepCheckStorage =
                workerClient.submitStep(getDescriptionStep("integration-worker/step_storage_SIP.json"));
            assertNotNull(retStepCheckStorage);
            assertEquals(StatusCode.OK, retStepCheckStorage.getGlobalStatus());

            final DescriptionStep descriptionStepUnit = getDescriptionStep("integration-worker/step_units_SIP.json");
            descriptionStepUnit.getWorkParams().setObjectName(unitName());
            final ItemStatus retStepStoreUnit = workerClient.submitStep(descriptionStepUnit);
            assertNotNull(retStepStoreUnit);
            assertEquals(StatusCode.OK, retStepStoreUnit.getGlobalStatus());

            final DescriptionStep descriptionStepOg = getDescriptionStep("integration-worker/step_objects_SIP.json");
            descriptionStepOg.getWorkParams().setObjectName(objectGroupName());
            final ItemStatus retStepStoreOg = workerClient.submitStep(descriptionStepOg);
            assertNotNull(retStepStoreOg);
            assertEquals(StatusCode.OK, retStepStoreOg.getGlobalStatus());

            workspaceClient.deleteContainer(CONTAINER_NAME, true);
        } catch (final Exception e) {
            e.printStackTrace();
            fail("should not raized an exception");
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowWithSipNoManifest() throws Exception {

        Integer tenantId = 0;
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        CONTAINER_NAME = GUIDFactory.newManifestGUID(tenantId).getId();
        VitamThreadUtils.getVitamSession().setRequestId(CONTAINER_NAME);

        // workspace client dezip SIP in workspace
        RestAssured.port = PORT_SERVICE_WORKSPACE;
        RestAssured.basePath = WORKSPACE_PATH;

        final InputStream zipInputStreamSipObject =
            PropertiesUtils.getResourceAsStream(SIP_WITHOUT_MANIFEST);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(CONTAINER_NAME);
        workspaceClient.uncompressObject(CONTAINER_NAME, SIP_FOLDER, CommonMediaType.ZIP, zipInputStreamSipObject);

        // call processing
        RestAssured.port = PORT_SERVICE_WORKER;
        RestAssured.basePath = WORKER_PATH;
        workerClientConfiguration = WorkerClientFactory.changeConfigurationFile(CONFIG_WORKER_CLIENT_PATH);

        workerClient = WorkerClientFactory.getInstance(workerClientConfiguration).getClient();
        final ItemStatus retStepControl =
            workerClient.submitStep(getDescriptionStep("integration-worker/step_control_SIP.json"));
        assertNotNull(retStepControl);
        assertEquals(StatusCode.KO, retStepControl.getGlobalStatus());

        workspaceClient.deleteContainer(CONTAINER_NAME, true);
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowWithManifestConformityKO() throws Exception {
        try {
            Integer tenantId = 0;
            VitamThreadUtils.getVitamSession().setTenantId(tenantId);
            CONTAINER_NAME = GUIDFactory.newManifestGUID(tenantId).getId();
            VitamThreadUtils.getVitamSession().setRequestId(CONTAINER_NAME);

            // workspace client dezip SIP in workspace
            RestAssured.port = PORT_SERVICE_WORKSPACE;
            RestAssured.basePath = WORKSPACE_PATH;

            final InputStream zipInputStreamSipObject =
                PropertiesUtils.getResourceAsStream(SIP_CONFORMITY_KO);
            workspaceClient = WorkspaceClientFactory.getInstance().getClient();
            workspaceClient.createContainer(CONTAINER_NAME);
            workspaceClient.uncompressObject(CONTAINER_NAME, SIP_FOLDER, CommonMediaType.ZIP, zipInputStreamSipObject);

            // call processing
            RestAssured.port = PORT_SERVICE_WORKER;
            RestAssured.basePath = WORKER_PATH;
            workerClientConfiguration = WorkerClientFactory.changeConfigurationFile(CONFIG_WORKER_CLIENT_PATH);

            workerClient = WorkerClientFactory.getInstance(workerClientConfiguration).getClient();
            final ItemStatus retStepControl =
                workerClient.submitStep(getDescriptionStep("integration-worker/step_control_SIP.json"));
            assertNotNull(retStepControl);
            assertEquals(3, retStepControl.getItemsStatus().size());
            assertEquals(StatusCode.OK, retStepControl.getGlobalStatus());

            workspaceClient.deleteContainer(CONTAINER_NAME, true);
        } catch (final Exception e) {
            e.printStackTrace();
            fail("should not raized an exception");
        }
    }

    @Test
    public void testRegistration() throws Exception {
        try {
            String workerId = String.valueOf(ServerIdentity.getInstance().getGlobalPlatformId());
            final WorkerRemoteConfiguration remoteConfiguration =
                new WorkerRemoteConfiguration("localhost", PORT_SERVICE_WORKER);
            final WorkerBean workerBean =
                new WorkerBean("name", WorkerRegister.DEFAULT_FAMILY, 1, 1L, "active", remoteConfiguration);
            processingClient = ProcessingManagementClientFactory.getInstance().getClient();
            try {
                processingClient.registerWorker(WorkerRegister.DEFAULT_FAMILY,workerId , workerBean);
                fail("Should have raized an exception");
            } catch (final WorkerAlreadyExistsException e) {
                processingClient.unregisterWorker(WorkerRegister.DEFAULT_FAMILY, workerId);
            }
            processingClient.registerWorker(WorkerRegister.DEFAULT_FAMILY, workerId, workerBean);
        } catch (final Exception e) {
            e.printStackTrace();
            fail("should not raized an exception");
        }
    }


    private DefaultWorkerParameters getWorkParams() {
        final DefaultWorkerParameters workparams = WorkerParametersFactory.newWorkerParameters();
        workparams.setContainerName(CONTAINER_NAME);
        return workparams;
    }

    private Step getStep(String stepFilePath) {
        final ObjectMapper objectMapper = new ObjectMapper();
        Step step = null;

        try {
            final InputStream inputJSON =
                PropertiesUtils.getResourceAsStream(stepFilePath);
            step = objectMapper.readValue(inputJSON, Step.class);

        } catch (final IOException e) {
            LOGGER.error("Exception while retrieving step", e);
        }
        return step;
    }

    private DescriptionStep getDescriptionStep(String stepFilePath) {
        final DescriptionStep descriptionStep = new DescriptionStep(getStep(stepFilePath), getWorkParams());
        descriptionStep.getWorkParams().setContainerName(CONTAINER_NAME);
        descriptionStep.getWorkParams().setCurrentStep(descriptionStep.getStep().getStepName());
        descriptionStep.getWorkParams().setUrlMetadata(METADATA_URL);
        descriptionStep.getWorkParams().setUrlWorkspace(WORKSPACE_URL);
        descriptionStep.getWorkParams().setObjectName("SIP/manifest.xml");
        descriptionStep.getWorkParams().setLogbookTypeProcess(LogbookTypeProcess.INGEST);
        return descriptionStep;
    }

    private String unitName() {
        String unitName = "";
        try {
            final InputStream stream = (InputStream) workspaceClient.getObject(CONTAINER_NAME,
                "UnitsLevel/ingestLevelStack.json").getEntity();
            final Map<String, Object> map = JsonHandler.getMapFromString(IOUtils.toString(stream, "UTF-8"));

            @SuppressWarnings("rawtypes")
            final ArrayList levelUnits = (ArrayList) map.values().iterator().next();
            if (levelUnits.size() > 0) {
                unitName = (String) levelUnits.get(0);
            }
        } catch (final Exception e) {
            LOGGER.error("Exception while retrieving objectGroup", e);
        }
        return unitName + ".json";
    }

    private String objectGroupName() {
        String objectName = "";
        try {
            final InputStream stream = (InputStream) workspaceClient.getObject(CONTAINER_NAME,
                "Maps/OBJECT_GROUP_ID_TO_GUID_MAP.json").getEntity();
            final Map<String, Object> map = JsonHandler.getMapFromString(IOUtils.toString(stream, "UTF-8"));
            objectName = (String) map.values().iterator().next();
        } catch (final Exception e) {
            LOGGER.error("Exception while retrieving objectGroup", e);
        }
        return objectName + ".json";
    }
}
