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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.util.Map;

import javax.ws.rs.core.Response.Status;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

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
import fr.gouv.vitam.common.client2.configuration.ClientConfigurationImpl;
import fr.gouv.vitam.common.format.identification.FormatIdentifierFactory;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.junit.JunitHelper.ElasticsearchTestConfiguration;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.rest.AdminManagementApplication;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.logbook.rest.LogbookApplication;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.metadata.rest.MetaDataApplication;
import fr.gouv.vitam.processing.common.exception.ProcessingBadRequestException;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
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
public class ProcessingIT {
    private static final String JETTY_FUNCTIONAL_ADMIN_PORT = "jetty.functional-admin.port";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProcessingIT.class);
    private static final int DATABASE_PORT = 12346;
    private static MongodExecutable mongodExecutable;
    static MongodProcess mongod;

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    private final static String CLUSTER_NAME = "vitam-cluster";
    static JunitHelper junitHelper;
    private static int TCP_PORT = 54321;
    private static int HTTP_PORT = 54320;

    private static final int PORT_SERVICE_WORKER = 8098;
    private static final int PORT_SERVICE_WORKSPACE = 8094;
    private static final int PORT_SERVICE_METADATA = 8096;
    private static final int PORT_SERVICE_PROCESSING = 8097;
    private static final int PORT_SERVICE_FUNCTIONAL_ADMIN = 8093;
    private static final int PORT_SERVICE_LOGBOOK = 8099;

    private static final String SIP_FOLDER = "SIP";
    private static final String METADATA_PATH = "/metadata/v1";
    private static final String PROCESSING_PATH = "/processing/v1";
    private static final String WORKER_PATH = "/worker/v1";
    private static final String WORKSPACE_PATH = "/workspace/v1";
    private static final String LOGBOOK_PATH = "/logbook/v1";

    private static String CONFIG_WORKER_PATH = "";
    private static String CONFIG_WORKSPACE_PATH = "";
    private static String CONFIG_METADATA_PATH = "";
    private static String CONFIG_PROCESSING_PATH = "";
    private static String CONFIG_FUNCTIONAL_ADMIN_PATH = "";
    private static String CONFIG_FUNCTIONAL_CLIENT_PATH = "";
    private static String CONFIG_LOGBOOK_PATH = "";
    private static String CONFIG_SIEGFRIED_PATH = "";

    // private static VitamServer workerApplication;
    private static MetaDataApplication medtadataApplication;
    private static WorkerApplication wkrapplication; 
    private static AdminManagementApplication adminApplication;
    private static LogbookApplication lgbapplication;
    private static WorkspaceApplication workspaceApplication;
    private WorkspaceClient workspaceClient;
    private ProcessingManagementClient processingClient;
    private static ProcessMonitoringImpl processMonitoring;

    private static final String WORKSPACE_URL = "http://localhost:" + PORT_SERVICE_WORKSPACE;
    private static final String PROCESSING_URL = "http://localhost:" + PORT_SERVICE_PROCESSING;

    private static String WORFKLOW_NAME = "DefaultIngestWorkflow";
    private static String CONTAINER_NAME;
    private static String SIP_FILE_OK_NAME = "integration-processing/SIP-test.zip";
    private static String SIP_FILE_TAR_OK_NAME = "integration-processing/SIP.tar";

    private static String SIP_ARBO_COMPLEXE_FILE_OK = "integration-processing/SIP_arbor_OK.zip";
    private static String SIP_FUND_REGISTER_OK = "integration-processing/OK-registre-fonds.zip";
    private static String SIP_WITHOUT_MANIFEST = "integration-processing/SIP_no_manifest.zip";
    private static String SIP_NO_FORMAT = "integration-processing/SIP_NO_FORMAT.zip";
    private static String SIP_DOUBLE_BM = "integration-processing/SIP_DoubleBM.zip";
    private static String SIP_NO_FORMAT_NO_TAG = "integration-processing/SIP_NO_FORMAT_TAG.zip";
    private static String SIP_NB_OBJ_INCORRECT_IN_MANIFEST = "integration-processing/SIP_Conformity_KO.zip";
    private static String SIP_ORPHELINS = "integration-processing/SIP-orphelins.zip";
    private static String SIP_OBJECT_SANS_GOT = "integration-processing/SIP-objetssansGOT.zip";
    private static ElasticsearchTestConfiguration config = null;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        CONFIG_METADATA_PATH = PropertiesUtils.getResourcePath("integration-processing/metadata.conf").toString();
        CONFIG_WORKER_PATH = PropertiesUtils.getResourcePath("integration-processing/worker.conf").toString();
        CONFIG_WORKSPACE_PATH = PropertiesUtils.getResourcePath("integration-processing/workspace.conf").toString();
        CONFIG_PROCESSING_PATH = PropertiesUtils.getResourcePath("integration-processing/processing.conf").toString();
        CONFIG_SIEGFRIED_PATH =
            PropertiesUtils.getResourcePath("integration-processing/format-identifiers.conf").toString();
        CONFIG_FUNCTIONAL_ADMIN_PATH =
            PropertiesUtils.getResourcePath("integration-processing/functional-administration.conf").toString();
        CONFIG_FUNCTIONAL_CLIENT_PATH =
            PropertiesUtils.getResourcePath("integration-processing/functional-administration-client-it.conf")
                .toString();

        CONFIG_LOGBOOK_PATH = PropertiesUtils.getResourcePath("integration-processing/logbook.conf").toString();
        CONFIG_SIEGFRIED_PATH =
            PropertiesUtils.getResourcePath("integration-processing/format-identifiers.conf").toString();

        // ES
        config = JunitHelper.startElasticsearchForTest(tempFolder, CLUSTER_NAME, TCP_PORT, HTTP_PORT);

        final MongodStarter starter = MongodStarter.getDefaultInstance();

        mongodExecutable = starter.prepare(new MongodConfigBuilder()
            .version(Version.Main.PRODUCTION)
            .net(new Net(DATABASE_PORT, Network.localhostIsIPv6()))
            .build());
        mongod = mongodExecutable.start();

        // AdminManagementClientFactory.getInstance().changeConfigurationFile(CONFIG_FUNCTIONAL_CLIENT_PATH);

        // launch metadata
        SystemPropertyUtil.set(MetaDataApplication.PARAMETER_JETTY_SERVER_PORT,
            Integer.toString(PORT_SERVICE_METADATA));
        medtadataApplication = new MetaDataApplication(CONFIG_METADATA_PATH);
        medtadataApplication.start();
        SystemPropertyUtil.clear(MetaDataApplication.PARAMETER_JETTY_SERVER_PORT);

        MetaDataClientFactory.changeMode(new ClientConfigurationImpl("localhost", PORT_SERVICE_METADATA));

        // launch workspace
        SystemPropertyUtil.set(WorkspaceApplication.PARAMETER_JETTY_SERVER_PORT,
            Integer.toString(PORT_SERVICE_WORKSPACE));
        workspaceApplication = new WorkspaceApplication(CONFIG_WORKSPACE_PATH);
        workspaceApplication .start();
        SystemPropertyUtil.clear(WorkspaceApplication.PARAMETER_JETTY_SERVER_PORT);
        WorkspaceClientFactory.changeMode(WORKSPACE_URL);

        // launch logbook
        SystemPropertyUtil
            .set(LogbookApplication.PARAMETER_JETTY_SERVER_PORT, Integer.toString(PORT_SERVICE_LOGBOOK));
        lgbapplication = new LogbookApplication(CONFIG_LOGBOOK_PATH);
        lgbapplication.start();
        SystemPropertyUtil.clear(LogbookApplication.PARAMETER_JETTY_SERVER_PORT);

        LogbookOperationsClientFactory.changeMode(new ClientConfigurationImpl("localhost", PORT_SERVICE_LOGBOOK));

        // launch processing
        SystemPropertyUtil.set(ProcessManagementApplication.PARAMETER_JETTY_SERVER_PORT,
            Integer.toString(PORT_SERVICE_PROCESSING));
        ProcessManagementApplication.startApplication(CONFIG_PROCESSING_PATH);
        SystemPropertyUtil.clear(ProcessManagementApplication.PARAMETER_JETTY_SERVER_PORT);

        // launch worker
        SystemPropertyUtil.set("jetty.worker.port", Integer.toString(PORT_SERVICE_WORKER));
        wkrapplication = new WorkerApplication(CONFIG_WORKER_PATH);
        wkrapplication.start();
        SystemPropertyUtil.clear("jetty.worker.port");

        FormatIdentifierFactory.getInstance().changeConfigurationFile(CONFIG_SIEGFRIED_PATH);

        // launch functional Admin server
        AdminManagementApplication adminApplication = new AdminManagementApplication(CONFIG_FUNCTIONAL_ADMIN_PATH);
        adminApplication.start();

        AdminManagementClientFactory
            .changeMode(new ClientConfigurationImpl("localhost", PORT_SERVICE_FUNCTIONAL_ADMIN));
        AdminManagementClient adminClient = AdminManagementClientFactory.getInstance().getClient();
        // VitamClientFactory;
        adminClient
            .importFormat(PropertiesUtils.getResourceAsStream("integration-processing/DROID_SignatureFile_V88.xml"));

        processMonitoring = ProcessMonitoringImpl.getInstance();

        CONTAINER_NAME = GUIDFactory.newGUID().toString();

    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        if (config == null) {
            return;
        }
        JunitHelper.stopElasticsearchForTest(config);
        mongod.stop();
        mongodExecutable.stop();
        try {
            workspaceApplication.stop();
            wkrapplication.stop();
            lgbapplication.stop();
            ProcessManagementApplication.stop();
            medtadataApplication.stop();
        } catch (final Exception e) {
            LOGGER.error(e);
        }
    }


    @Test
    public void testServersStatus() throws Exception {
        try {
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

            RestAssured.port = PORT_SERVICE_LOGBOOK;
            RestAssured.basePath = LOGBOOK_PATH;
            get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());
        } catch (Exception e) {
            e.printStackTrace();
            fail("should not raized an exception");
        }
    }

    @Test
    public void testWorkflow() throws Exception {
        try {
          GUID operationGuid = GUIDFactory.newOperationLogbookGUID(0);
          GUID objectGuid = GUIDFactory.newManifestGUID(0);
          String containerName = objectGuid.getId();
          createLogbookOperation(operationGuid, objectGuid);

          // workspace client dezip SIP in workspace
          RestAssured.port = PORT_SERVICE_WORKSPACE;
          RestAssured.basePath = WORKSPACE_PATH;
          final InputStream zipInputStreamSipObject =
              PropertiesUtils.getResourceAsStream(SIP_FILE_OK_NAME);
          workspaceClient = WorkspaceClientFactory.getInstance().getClient();
          workspaceClient.createContainer(containerName);
          workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP,
              zipInputStreamSipObject);
          // call processing
          RestAssured.port = PORT_SERVICE_PROCESSING;
          RestAssured.basePath = PROCESSING_PATH;
          processingClient = new ProcessingManagementClient(PROCESSING_URL);
          ItemStatus ret = processingClient.executeVitamProcess(containerName, WORFKLOW_NAME);
          assertNotNull(ret);
          // check conformity in warning state
          assertEquals(StatusCode.WARNING, ret.getGlobalStatus());

          // checkMonitoring - meaning something has been added in the monitoring tool
          Map<String, ProcessStep> map = processMonitoring.getWorkflowStatus(ret.getItemId());
          assertNotNull(map);
        } catch (Exception e) {
            e.printStackTrace();
            fail("should not raized an exception");
        }
    }

    @Test
    public void testWorkflowWithTarSIP() throws Exception {
        try {
          GUID operationGuid = GUIDFactory.newOperationLogbookGUID(0);
          GUID objectGuid = GUIDFactory.newManifestGUID(0);
          String containerName = objectGuid.getId();
          createLogbookOperation(operationGuid, objectGuid);
          // workspace client dezip SIP in workspace
          RestAssured.port = PORT_SERVICE_WORKSPACE;
          RestAssured.basePath = WORKSPACE_PATH;

          InputStream zipInputStreamSipObject =
              Thread.currentThread().getContextClassLoader().getResourceAsStream(SIP_FILE_TAR_OK_NAME);
          workspaceClient = WorkspaceClientFactory.getInstance().getClient();
          workspaceClient.createContainer(containerName);
          workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.TAR,
              zipInputStreamSipObject);

          // call processing
          RestAssured.port = PORT_SERVICE_PROCESSING;
          RestAssured.basePath = PROCESSING_PATH;
          processingClient = new ProcessingManagementClient(PROCESSING_URL);
          final ItemStatus ret = processingClient.executeVitamProcess(containerName, WORFKLOW_NAME);
          assertNotNull(ret);
          // format file warning state
          assertEquals(StatusCode.WARNING, ret.getGlobalStatus());

          // checkMonitoring - meaning something has been added in the monitoring tool
          Map<String, ProcessStep> map = processMonitoring.getWorkflowStatus(ret.getItemId());
          assertNotNull(map);
        } catch (Exception e) {
            e.printStackTrace();
            fail("should not raized an exception");
        }
    }

    @Test
    public void testWorkflow_with_complexe_unit_seda() throws Exception {
        try {
          GUID operationGuid = GUIDFactory.newOperationLogbookGUID(0);
          GUID objectGuid = GUIDFactory.newManifestGUID(0);
          String containerName = objectGuid.getId();
          createLogbookOperation(operationGuid, objectGuid);

          // workspace client dezip SIP in workspace
          RestAssured.port = PORT_SERVICE_WORKSPACE;
          RestAssured.basePath = WORKSPACE_PATH;

          final InputStream zipInputStreamSipObject =
              PropertiesUtils.getResourceAsStream(SIP_ARBO_COMPLEXE_FILE_OK);
          workspaceClient = WorkspaceClientFactory.getInstance().getClient();
          workspaceClient.createContainer(containerName);
          workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP, zipInputStreamSipObject);

          // call processing
          RestAssured.port = PORT_SERVICE_PROCESSING;
          RestAssured.basePath = PROCESSING_PATH;
          processingClient = new ProcessingManagementClient(PROCESSING_URL);
          final ItemStatus ret = processingClient.executeVitamProcess(containerName, WORFKLOW_NAME);
          assertNotNull(ret);
          // File format warning state
          assertEquals(StatusCode.WARNING, ret.getGlobalStatus());

          // checkMonitoring - meaning something has been added in the monitoring tool
          Map<String, ProcessStep> map = processMonitoring.getWorkflowStatus(ret.getItemId());
          assertNotNull(map);
        } catch (Exception e) {
            e.printStackTrace();
            fail("should not raized an exception");
        }
    }

    @Test
    public void testWorkflow_with_accession_register() throws Exception {
        try {
          GUID operationGuid = GUIDFactory.newOperationLogbookGUID(0);
          GUID objectGuid = GUIDFactory.newManifestGUID(0);
          String containerName = objectGuid.getId();
          createLogbookOperation(operationGuid, objectGuid);

          // workspace client dezip SIP in workspace
          RestAssured.port = PORT_SERVICE_WORKSPACE;
          RestAssured.basePath = WORKSPACE_PATH;

          final InputStream zipInputStreamSipObject =
              PropertiesUtils.getResourceAsStream(SIP_FUND_REGISTER_OK);
          workspaceClient = WorkspaceClientFactory.getInstance().getClient();
          workspaceClient.createContainer(containerName);
          workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP, zipInputStreamSipObject);

          // call processing
          RestAssured.port = PORT_SERVICE_PROCESSING;
          RestAssured.basePath = PROCESSING_PATH;
          processingClient = new ProcessingManagementClient(PROCESSING_URL);
          final ItemStatus ret = processingClient.executeVitamProcess(containerName, WORFKLOW_NAME);
          assertNotNull(ret);
          // File format in warning state
          assertEquals(StatusCode.WARNING, ret.getGlobalStatus());

          // checkMonitoring - meaning something has been added in the monitoring tool
          Map<String, ProcessStep> map = processMonitoring.getWorkflowStatus(ret.getItemId());
          assertNotNull(map);
        } catch (Exception e) {
            e.printStackTrace();
            fail("should not raized an exception");
        }
    }

    @Test
    public void testWorkflowWithSipNoManifest() throws Exception {
        GUID operationGuid = GUIDFactory.newOperationLogbookGUID(0);
        GUID objectGuid = GUIDFactory.newManifestGUID(0);
        String containerName = objectGuid.getId();
        createLogbookOperation(operationGuid, objectGuid);

        // workspace client dezip SIP in workspace
        RestAssured.port = PORT_SERVICE_WORKSPACE;
        RestAssured.basePath = WORKSPACE_PATH;

        final InputStream zipInputStreamSipObject =
            PropertiesUtils.getResourceAsStream(SIP_WITHOUT_MANIFEST);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP, zipInputStreamSipObject);

        // call processing
        RestAssured.port = PORT_SERVICE_PROCESSING;
        RestAssured.basePath = PROCESSING_PATH;
        processingClient = new ProcessingManagementClient(PROCESSING_URL);
        final ItemStatus ret = processingClient.executeVitamProcess(containerName, WORFKLOW_NAME);
        assertNotNull(ret);
        // format file warning state
        assertEquals(StatusCode.KO, ret.getGlobalStatus());
    }

    @Test
    public void testWorkflowSipNoFormat() throws Exception {
        try {
          GUID operationGuid = GUIDFactory.newOperationLogbookGUID(0);
          GUID objectGuid = GUIDFactory.newManifestGUID(0);
          String containerName = objectGuid.getId();
          createLogbookOperation(operationGuid, objectGuid);

          // workspace client dezip SIP in workspace
          RestAssured.port = PORT_SERVICE_WORKSPACE;
          RestAssured.basePath = WORKSPACE_PATH;

          final InputStream zipInputStreamSipObject =
              PropertiesUtils.getResourceAsStream(SIP_NO_FORMAT);
          workspaceClient = WorkspaceClientFactory.getInstance().getClient();
          workspaceClient.createContainer(containerName);
          workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP, zipInputStreamSipObject);

          // call processing
          RestAssured.port = PORT_SERVICE_PROCESSING;
          RestAssured.basePath = PROCESSING_PATH;
          processingClient = new ProcessingManagementClient(PROCESSING_URL);
          final ItemStatus ret = processingClient.executeVitamProcess(containerName, WORFKLOW_NAME);
          assertNotNull(ret);
          // format file warning state
          assertEquals(StatusCode.WARNING, ret.getGlobalStatus());

          // checkMonitoring - meaning something has been added in the monitoring tool
          Map<String, ProcessStep> map = processMonitoring.getWorkflowStatus(ret.getItemId());
          assertNotNull(map);
        } catch (Exception e) {
            e.printStackTrace();
            fail("should not raized an exception");
        }
    }


    @Test
    public void testWorkflowSipDoubleVersionBM() throws Exception {
        GUID operationGuid = GUIDFactory.newOperationLogbookGUID(0);
        GUID objectGuid = GUIDFactory.newManifestGUID(0);
        String containerName = objectGuid.getId();
        createLogbookOperation(operationGuid, objectGuid);

        // workspace client dezip SIP in workspace
        RestAssured.port = PORT_SERVICE_WORKSPACE;
        RestAssured.basePath = WORKSPACE_PATH;

        final InputStream zipInputStreamSipObject =
            PropertiesUtils.getResourceAsStream(SIP_DOUBLE_BM);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP, zipInputStreamSipObject);

        // call processing
        RestAssured.port = PORT_SERVICE_PROCESSING;
        RestAssured.basePath = PROCESSING_PATH;
        processingClient = new ProcessingManagementClient(PROCESSING_URL);
        final ItemStatus ret = processingClient.executeVitamProcess(containerName, WORFKLOW_NAME);
        assertNotNull(ret);
        assertEquals(StatusCode.KO, ret.getGlobalStatus());
    }


    @Test
    public void testWorkflowSipNoFormatNoTag() throws Exception {
        try {
          GUID operationGuid = GUIDFactory.newOperationLogbookGUID(0);
          GUID objectGuid = GUIDFactory.newManifestGUID(0);
          String containerName = objectGuid.getId();
          createLogbookOperation(operationGuid, objectGuid);

          // workspace client dezip SIP in workspace
          RestAssured.port = PORT_SERVICE_WORKSPACE;
          RestAssured.basePath = WORKSPACE_PATH;

          final InputStream zipInputStreamSipObject =
              PropertiesUtils.getResourceAsStream(SIP_NO_FORMAT_NO_TAG);
          workspaceClient = WorkspaceClientFactory.getInstance().getClient();
          workspaceClient.createContainer(containerName);
          workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP, zipInputStreamSipObject);
          // call processing
          RestAssured.port = PORT_SERVICE_PROCESSING;
          RestAssured.basePath = PROCESSING_PATH;
          processingClient = new ProcessingManagementClient(PROCESSING_URL);
          final ItemStatus ret = processingClient.executeVitamProcess(containerName, WORFKLOW_NAME);
          assertNotNull(ret);
          // format file warning state
          assertEquals(StatusCode.WARNING, ret.getGlobalStatus());

          // checkMonitoring - meaning something has been added in the monitoring tool
          Map<String, ProcessStep> map = processMonitoring.getWorkflowStatus(ret.getItemId());
          assertNotNull(map);
        } catch (Exception e) {
            e.printStackTrace();
            fail("should not raized an exception");
        }
    }



    @Test
    public void testWorkflowWithManifestIncorrectObjectNumber() throws Exception {
        GUID operationGuid = GUIDFactory.newOperationLogbookGUID(0);
        GUID objectGuid = GUIDFactory.newManifestGUID(0);
        String containerName = objectGuid.getId();
        createLogbookOperation(operationGuid, objectGuid);

        // workspace client dezip SIP in workspace
        RestAssured.port = PORT_SERVICE_WORKSPACE;
        RestAssured.basePath = WORKSPACE_PATH;

        final InputStream zipInputStreamSipObject =
            PropertiesUtils.getResourceAsStream(SIP_NB_OBJ_INCORRECT_IN_MANIFEST);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP, zipInputStreamSipObject);

        // call processing
        RestAssured.port = PORT_SERVICE_PROCESSING;
        RestAssured.basePath = PROCESSING_PATH;
        processingClient = new ProcessingManagementClient(PROCESSING_URL);
        // An action returns KO => the step is in KO => the workflow is OK
        final ItemStatus ret = processingClient.executeVitamProcess(containerName, WORFKLOW_NAME);
        assertNotNull(ret);
        // format file warning state
        assertEquals(StatusCode.KO, ret.getGlobalStatus());
    }

    @Test
    public void testWorkflowWithOrphelins() throws Exception {
        GUID operationGuid = GUIDFactory.newOperationLogbookGUID(0);
        GUID objectGuid = GUIDFactory.newManifestGUID(0);
        String containerName = objectGuid.getId();
        createLogbookOperation(operationGuid, objectGuid);

        // workspace client dezip SIP in workspace
        RestAssured.port = PORT_SERVICE_WORKSPACE;
        RestAssured.basePath = WORKSPACE_PATH;

        final InputStream zipInputStreamSipObject =
            PropertiesUtils.getResourceAsStream(SIP_ORPHELINS);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP, zipInputStreamSipObject);

        // call processing
        RestAssured.port = PORT_SERVICE_PROCESSING;
        RestAssured.basePath = PROCESSING_PATH;
        processingClient = new ProcessingManagementClient(PROCESSING_URL);
        final ItemStatus ret = processingClient.executeVitamProcess(containerName, WORFKLOW_NAME);
        assertNotNull(ret);
        // format file warning state
        assertEquals(StatusCode.KO, ret.getGlobalStatus());
    }


    @Test
    public void testWorkflow_withoutObjectGroups() throws Exception {
        try {
          GUID operationGuid = GUIDFactory.newOperationLogbookGUID(0);
          GUID objectGuid = GUIDFactory.newManifestGUID(0);
          String containerName = objectGuid.getId();
          createLogbookOperation(operationGuid, objectGuid);

          // workspace client dezip SIP in workspace
          RestAssured.port = PORT_SERVICE_WORKSPACE;
          RestAssured.basePath = WORKSPACE_PATH;

          final InputStream zipInputStreamSipObject =
              PropertiesUtils.getResourceAsStream(SIP_OBJECT_SANS_GOT);
          workspaceClient = WorkspaceClientFactory.getInstance().getClient();
          workspaceClient.createContainer(containerName);
          workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP, zipInputStreamSipObject);

          // call processing
          RestAssured.port = PORT_SERVICE_PROCESSING;
          RestAssured.basePath = PROCESSING_PATH;
          processingClient = new ProcessingManagementClient(PROCESSING_URL);
          final ItemStatus ret = processingClient.executeVitamProcess(containerName, WORFKLOW_NAME);
          assertNotNull(ret);
          // File formar warning state
          assertEquals(StatusCode.WARNING, ret.getGlobalStatus());

          // checkMonitoring - meaning something has been added in the monitoring tool
          Map<String, ProcessStep> map = processMonitoring.getWorkflowStatus(ret.getItemId());
          assertNotNull(map);
        } catch (Exception e) {
            e.printStackTrace();
            fail("should not raized an exception");
        }
    }


    public void createLogbookOperation(GUID operationId, GUID objectId)
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException,
        LogbookClientNotFoundException {

        LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient();

        LogbookOperationParameters initParameters = LogbookParametersFactory.newLogbookOperationParameters(
            operationId, "Process_SIP_unitary", objectId,
            LogbookTypeProcess.INGEST, StatusCode.STARTED,
            operationId != null ? operationId.toString() : "outcomeDetailMessage",
            operationId);
        logbookClient.create(initParameters);
    }
}
