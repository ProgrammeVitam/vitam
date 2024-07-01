/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "https://cecill.info".
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
 */
package fr.gouv.vitam.worker.server.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.ServerIdentity;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.client.BasicClient;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.client.VitamClientFactoryInterface;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.processing.LifecycleState;
import fr.gouv.vitam.common.model.processing.Step;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.common.xml.XMLInputFactoryUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.rest.MetadataMain;
import fr.gouv.vitam.processing.common.model.WorkerBean;
import fr.gouv.vitam.processing.common.model.WorkerRemoteConfiguration;
import fr.gouv.vitam.processing.common.parameter.DefaultWorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.processing.management.rest.ProcessManagementMain;
import fr.gouv.vitam.worker.client.WorkerClient;
import fr.gouv.vitam.worker.client.WorkerClientConfiguration;
import fr.gouv.vitam.worker.client.WorkerClientFactory;
import fr.gouv.vitam.worker.common.DescriptionStep;
import fr.gouv.vitam.worker.server.registration.WorkerRegister;
import fr.gouv.vitam.worker.server.rest.WorkerMain;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import io.restassured.RestAssured;
import org.apache.commons.io.IOUtils;
import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static io.restassured.RestAssured.get;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Worker integration test
 */
public class WorkerIT extends VitamRuleRunner {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(WorkerIT.class);

    @ClassRule
    public static VitamServerRunner runner = new VitamServerRunner(
        WorkerIT.class,
        mongoRule.getMongoDatabase().getName(),
        ElasticsearchRule.getClusterName(),
        Sets.newHashSet(
            MetadataMain.class,
            WorkerMain.class,
            LogbookMain.class,
            WorkspaceMain.class,
            ProcessManagementMain.class
        )
    );

    private static final String SIP_FOLDER = "SIP";
    private static final String METADATA_PATH = "/metadata/v1";
    private static final String PROCESSING_PATH = "/processing/v1";
    private static final String WORKER_PATH = "/worker/v1";
    private static final String WORKSPACE_PATH = "/workspace/v1";

    private static String CONFIG_WORKER_CLIENT_PATH = "";

    private WorkspaceClient workspaceClient;
    private WorkerClient workerClient;
    private WorkerClientConfiguration workerClientConfiguration;

    private ProcessingManagementClient processingClient;

    private static String CONTAINER_NAME = GUIDFactory.newGUID().toString();
    private static final String SIP_FILE_OK_NAME = "integration-worker/SIP.zip";
    private static final String SIP_ARBO_COMPLEXE_FILE_OK = "integration-worker/SIP_arbor_OK.zip";
    private static final String SIP_WITHOUT_MANIFEST = "integration-worker/SIP_no_manifest.zip";
    private static final String SIP_CONFORMITY_KO = "integration-worker/SIP_Conformity_KO.zip";
    private static final String STORAGE_INFO_JSON = "integration-worker/storageInfo.json";
    private static final String STORAGE_INFO_PATH = "StorageInfo/storageInfo.json";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        handleBeforeClass(Arrays.asList(0, 1), Collections.emptyMap());
        CONFIG_WORKER_CLIENT_PATH = PropertiesUtils.getResourcePath("common/worker-client.conf").toString();

        AdminManagementClientFactory instance = AdminManagementClientFactory.getInstance();
        instance.setVitamClientType(VitamClientFactoryInterface.VitamClientType.MOCK);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        handleAfterClass();
        runAfter();
        AdminManagementClientFactory instance = AdminManagementClientFactory.getInstance();
        instance.setVitamClientType(VitamClientFactoryInterface.VitamClientType.PRODUCTION);
        VitamClientFactory.resetConnections();
    }

    @After
    public void tearDown() {
        runAfter();
    }

    @Test
    public void testServersStatus() {
        RestAssured.port = runner.PORT_SERVICE_WORKER;
        RestAssured.basePath = WORKER_PATH;
        get(BasicClient.STATUS_URL).then().statusCode(204);

        RestAssured.port = runner.PORT_SERVICE_WORKSPACE;
        RestAssured.basePath = WORKSPACE_PATH;
        get(BasicClient.STATUS_URL).then().statusCode(204);

        RestAssured.port = runner.PORT_SERVICE_PROCESSING;
        RestAssured.basePath = PROCESSING_PATH;
        get(BasicClient.STATUS_URL).then().statusCode(204);

        RestAssured.port = runner.PORT_SERVICE_METADATA;
        RestAssured.basePath = METADATA_PATH;
        get(BasicClient.STATUS_URL).then().statusCode(204);
    }

    private void printAndCheckXmlConfiguration() throws Exception {
        LOGGER.warn(
            "XML Configuration: " +
            "\n\tjavax.xml.parsers.SAXParserFactory: " +
            SystemPropertyUtil.getNoCheck("javax.xml.parsers.SAXParserFactory") +
            "\n\tjavax.xml.parsers.DocumentBuilderFactory: " +
            SystemPropertyUtil.getNoCheck("javax.xml.parsers.DocumentBuilderFactory") +
            "\n\tjavax.xml.datatype.DatatypeFactory: " +
            SystemPropertyUtil.getNoCheck("javax.xml.datatype.DatatypeFactory") +
            "\n\tjavax.xml.stream.XMLEventFactory: " +
            SystemPropertyUtil.getNoCheck("javax.xml.stream.XMLEventFactory") +
            "\n\tjavax.xml.stream.XMLInputFactory: " +
            SystemPropertyUtil.getNoCheck("javax.xml.stream.XMLInputFactory") +
            "\n\tjavax.xml.stream.XMLOutputFactory: " +
            SystemPropertyUtil.getNoCheck("javax.xml.stream.XMLOutputFactory") +
            "\n\tjavax.xml.transform.TransformerFactory: " +
            SystemPropertyUtil.getNoCheck("javax.xml.transform.TransformerFactory") +
            "\n\tjavax.xml.validation.SchemaFactory: " +
            SystemPropertyUtil.getNoCheck("javax.xml.validation.SchemaFactory") +
            "\n\tjavax.xml.xpath.XPathFactory: " +
            SystemPropertyUtil.getNoCheck("javax.xml.xpath.XPathFactory")
        );

        LOGGER.warn(
            "XML Implementation: " +
            "\n\tjavax.xml.parsers.SAXParserFactory: " +
            javax.xml.parsers.SAXParserFactory.newInstance().getClass() +
            "\n\tjavax.xml.parsers.DocumentBuilderFactory: " +
            javax.xml.parsers.DocumentBuilderFactory.newInstance().getClass() +
            "\n\tjavax.xml.datatype.DatatypeFactory: " +
            javax.xml.datatype.DatatypeFactory.newInstance().getClass() +
            "\n\tjavax.xml.stream.XMLEventFactory: " +
            javax.xml.stream.XMLEventFactory.newFactory().getClass() +
            "\n\tjavax.xml.stream.XMLInputFactory: " +
            XMLInputFactoryUtils.newInstance().getClass() +
            "\n\tjavax.xml.stream.XMLOutputFactory: " +
            javax.xml.stream.XMLOutputFactory.newInstance().getClass() +
            "\n\tjavax.xml.transform.TransformerFactory: " +
            javax.xml.transform.TransformerFactory.newInstance().getClass() +
            "\n\tjavax.xml.validation.SchemaFactory: " +
            javax.xml.validation.SchemaFactory.newInstance("http://www.w3.org/XML/XMLSchema/v1.1").getClass() +
            "\n\tjavax.xml.xpath.XPathFactory: " +
            javax.xml.xpath.XPathFactory.newInstance().getClass()
        );
    }

    @Test
    public void testXsdConfiguration() throws Exception {
        assertThatCode(() -> printAndCheckXmlConfiguration()).doesNotThrowAnyException();
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflow() throws Exception {
        int tenantId = 0;
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        CONTAINER_NAME = GUIDFactory.newManifestGUID(tenantId).getId();
        VitamThreadUtils.getVitamSession().setRequestId(CONTAINER_NAME);
        VitamThreadUtils.getVitamSession().setContextId("Context_IT");

        // workspace client dezip SIP in workspace
        final InputStream zipInputStreamSipObject = PropertiesUtils.getResourceAsStream(SIP_FILE_OK_NAME);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(CONTAINER_NAME);
        workspaceClient.uncompressObject(CONTAINER_NAME, SIP_FOLDER, CommonMediaType.ZIP, zipInputStreamSipObject);

        final InputStream storageInfoJson = PropertiesUtils.getResourceAsStream(STORAGE_INFO_JSON);
        workspaceClient.putObject(CONTAINER_NAME, STORAGE_INFO_PATH, storageInfoJson);

        // call processing
        workerClientConfiguration = WorkerClientFactory.changeConfigurationFile("worker-client.conf");

        workerClient = WorkerClientFactory.getInstance(workerClientConfiguration).getClient();
        final ItemStatus retStepControl = workerClient.submitStep(
            getDescriptionStep("integration-worker/step_control_SIP.json")
        );
        assertNotNull(retStepControl);
        assertEquals(StatusCode.OK, retStepControl.getGlobalStatus());

        final ItemStatus retStepCheckStorage = workerClient.submitStep(
            getDescriptionStep("integration-worker/step_storage_SIP.json")
        );
        assertNotNull(retStepCheckStorage);
        assertEquals(StatusCode.OK, retStepCheckStorage.getGlobalStatus());

        final DescriptionStep descriptionStepUnit = getDescriptionStep("integration-worker/step_units_SIP.json");
        descriptionStepUnit.getWorkParams().setObjectName(unitName());
        descriptionStepUnit.getWorkParams().setObjectNameList(Lists.newArrayList(unitName()));
        final ItemStatus retStepStoreUnit = workerClient.submitStep(descriptionStepUnit);
        assertNotNull(retStepStoreUnit);
        assertEquals(StatusCode.OK, retStepStoreUnit.getGlobalStatus());

        final DescriptionStep descriptionStepOg = getDescriptionStep("integration-worker/step_objects_SIP.json");
        descriptionStepOg.getWorkParams().setObjectName(objectGroupName());
        descriptionStepOg.getWorkParams().setObjectNameList(Lists.newArrayList(objectGroupName()));
        final ItemStatus retStepStoreOg = workerClient.submitStep(descriptionStepOg);
        assertNotNull(retStepStoreOg);
        assertEquals(StatusCode.OK, retStepStoreOg.getGlobalStatus());

        workspaceClient.deleteContainer(CONTAINER_NAME, true);
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflow_with_complexe_unit_seda() throws Exception {
        int tenantId = 0;
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        CONTAINER_NAME = GUIDFactory.newManifestGUID(tenantId).getId();
        VitamThreadUtils.getVitamSession().setRequestId(CONTAINER_NAME);
        VitamThreadUtils.getVitamSession().setContextId("Context_IT");

        // workspace client dezip SIP in workspace
        final InputStream zipInputStreamSipObject = PropertiesUtils.getResourceAsStream(SIP_ARBO_COMPLEXE_FILE_OK);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(CONTAINER_NAME);
        workspaceClient.uncompressObject(CONTAINER_NAME, SIP_FOLDER, CommonMediaType.ZIP, zipInputStreamSipObject);

        final InputStream storageInfoJson = PropertiesUtils.getResourceAsStream(STORAGE_INFO_JSON);
        workspaceClient.putObject(CONTAINER_NAME, STORAGE_INFO_PATH, storageInfoJson);

        // call processing
        workerClientConfiguration = WorkerClientFactory.changeConfigurationFile(CONFIG_WORKER_CLIENT_PATH);

        workerClient = WorkerClientFactory.getInstance(workerClientConfiguration).getClient();
        final ItemStatus retStepControl = workerClient.submitStep(
            getDescriptionStep("integration-worker/step_control_SIP.json")
        );
        assertNotNull(retStepControl);
        assertEquals(StatusCode.OK, retStepControl.getGlobalStatus());

        final ItemStatus retStepCheckStorage = workerClient.submitStep(
            getDescriptionStep("integration-worker/step_storage_SIP.json")
        );
        assertNotNull(retStepCheckStorage);
        assertEquals(StatusCode.OK, retStepCheckStorage.getGlobalStatus());

        final DescriptionStep descriptionStepUnit = getDescriptionStep("integration-worker/step_units_SIP.json");
        descriptionStepUnit.getWorkParams().setObjectName(unitName());
        descriptionStepUnit.getWorkParams().setObjectNameList(Lists.newArrayList(unitName()));

        final ItemStatus retStepStoreUnit = workerClient.submitStep(descriptionStepUnit);
        assertNotNull(retStepStoreUnit);
        assertEquals(StatusCode.OK, retStepStoreUnit.getGlobalStatus());

        final DescriptionStep descriptionStepOg = getDescriptionStep("integration-worker/step_objects_SIP.json");
        descriptionStepOg.getWorkParams().setObjectName(objectGroupName());
        descriptionStepOg.getWorkParams().setObjectNameList(Lists.newArrayList(objectGroupName()));
        final ItemStatus retStepStoreOg = workerClient.submitStep(descriptionStepOg);
        assertNotNull(retStepStoreOg);
        assertEquals(StatusCode.OK, retStepStoreOg.getGlobalStatus());

        workspaceClient.deleteContainer(CONTAINER_NAME, true);
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowWithSipNoManifest() throws Exception {
        int tenantId = 0;
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        CONTAINER_NAME = GUIDFactory.newManifestGUID(tenantId).getId();
        VitamThreadUtils.getVitamSession().setRequestId(CONTAINER_NAME);
        VitamThreadUtils.getVitamSession().setContextId("Context_IT");
        // workspace client dezip SIP in workspace
        final InputStream zipInputStreamSipObject = PropertiesUtils.getResourceAsStream(SIP_WITHOUT_MANIFEST);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(CONTAINER_NAME);
        workspaceClient.uncompressObject(CONTAINER_NAME, SIP_FOLDER, CommonMediaType.ZIP, zipInputStreamSipObject);

        final InputStream storageInfoJson = PropertiesUtils.getResourceAsStream(STORAGE_INFO_JSON);
        workspaceClient.putObject(CONTAINER_NAME, STORAGE_INFO_PATH, storageInfoJson);

        // call processing
        workerClientConfiguration = WorkerClientFactory.changeConfigurationFile(CONFIG_WORKER_CLIENT_PATH);

        workerClient = WorkerClientFactory.getInstance(workerClientConfiguration).getClient();
        final ItemStatus retStepControl = workerClient.submitStep(
            getDescriptionStep("integration-worker/step_control_SIP.json")
        );
        assertNotNull(retStepControl);
        assertEquals(StatusCode.KO, retStepControl.getGlobalStatus());

        workspaceClient.deleteContainer(CONTAINER_NAME, true);
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowWithManifestConformityKO() throws Exception {
        int tenantId = 0;
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        CONTAINER_NAME = GUIDFactory.newManifestGUID(tenantId).getId();
        VitamThreadUtils.getVitamSession().setRequestId(CONTAINER_NAME);
        VitamThreadUtils.getVitamSession().setContextId("Context_IT");
        // workspace client dezip SIP in workspace
        final InputStream zipInputStreamSipObject = PropertiesUtils.getResourceAsStream(SIP_CONFORMITY_KO);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(CONTAINER_NAME);
        workspaceClient.uncompressObject(CONTAINER_NAME, SIP_FOLDER, CommonMediaType.ZIP, zipInputStreamSipObject);

        final InputStream storageInfoJson = PropertiesUtils.getResourceAsStream(STORAGE_INFO_JSON);
        workspaceClient.putObject(CONTAINER_NAME, STORAGE_INFO_PATH, storageInfoJson);

        // call processing
        workerClientConfiguration = WorkerClientFactory.changeConfigurationFile(CONFIG_WORKER_CLIENT_PATH);

        workerClient = WorkerClientFactory.getInstance(workerClientConfiguration).getClient();
        final ItemStatus retStepControl = workerClient.submitStep(
            getDescriptionStep("integration-worker/step_control_SIP.json")
        );
        assertNotNull(retStepControl);
        assertEquals(StatusCode.OK, retStepControl.getGlobalStatus());

        workspaceClient.deleteContainer(CONTAINER_NAME, true);
    }

    @Test
    public void testRegistration() throws Exception {
        String workerId = String.valueOf(ServerIdentity.getInstance().getGlobalPlatformId());
        final WorkerRemoteConfiguration remoteConfiguration = new WorkerRemoteConfiguration(
            "localhost",
            runner.PORT_SERVICE_WORKER
        );
        final WorkerBean workerBean = new WorkerBean(
            "name",
            WorkerRegister.DEFAULT_FAMILY,
            1,
            "active",
            remoteConfiguration
        );
        processingClient = ProcessingManagementClientFactory.getInstance().getClient();
        try {
            processingClient.registerWorker(WorkerRegister.DEFAULT_FAMILY, workerId, workerBean);
        } catch (final Exception e) {
            LOGGER.error(e);
            fail("Should not throw exception");
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
            final InputStream inputJSON = PropertiesUtils.getResourceAsStream(stepFilePath);
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
        descriptionStep.getWorkParams().setUrlMetadata(runner.METADATA_URL);
        descriptionStep.getWorkParams().setUrlWorkspace(runner.WORKSPACE_URL);
        descriptionStep.getWorkParams().setObjectName("SIP/manifest.xml");
        descriptionStep.getWorkParams().setObjectNameList(Lists.newArrayList("SIP/manifest.xml"));
        descriptionStep.getWorkParams().setLogbookTypeProcess(LogbookTypeProcess.INGEST);
        descriptionStep.getStep().defaultLifecycleLog(LifecycleState.TEMPORARY);
        return descriptionStep;
    }

    private String unitName() {
        String unitName = "";
        try {
            final InputStream stream = (InputStream) workspaceClient
                .getObject(CONTAINER_NAME, "UnitsLevel/ingestLevelStack.json")
                .getEntity();
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
            final InputStream stream = (InputStream) workspaceClient
                .getObject(CONTAINER_NAME, "Maps/OBJECT_GROUP_ID_TO_GUID_MAP.json")
                .getEntity();
            final Map<String, Object> map = JsonHandler.getMapFromString(IOUtils.toString(stream, "UTF-8"));
            objectName = (String) map.values().iterator().next();
        } catch (final Exception e) {
            LOGGER.error("Exception while retrieving objectGroup", e);
        }
        return objectName + ".json";
    }
}
