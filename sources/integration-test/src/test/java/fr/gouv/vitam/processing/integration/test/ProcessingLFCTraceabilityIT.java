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
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;

import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.restassured.RestAssured;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;
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
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.client.configuration.ClientConfigurationImpl;
import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.format.identification.FormatIdentifierFactory;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.guid.GUIDReader;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.junit.JunitHelper.ElasticsearchTestConfiguration;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.ContextModel;
import fr.gouv.vitam.common.model.administration.IngestContractModel;
import fr.gouv.vitam.common.model.administration.ProfileModel;
import fr.gouv.vitam.common.model.administration.SecurityProfileModel;
import fr.gouv.vitam.common.storage.StorageConfiguration;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.Contexts;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleObjectGroupParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleUnitParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookDocument;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.metadata.rest.MetadataMain;
import fr.gouv.vitam.processing.common.ProcessingEntry;
import fr.gouv.vitam.processing.common.model.ProcessWorkflow;
import fr.gouv.vitam.processing.common.parameter.WorkerParameterName;
import fr.gouv.vitam.processing.data.core.ProcessDataAccessImpl;
import fr.gouv.vitam.processing.engine.core.monitoring.ProcessMonitoringImpl;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.processing.management.rest.ProcessManagementMain;
import fr.gouv.vitam.worker.server.rest.WorkerMain;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import org.bson.Document;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Processing integration test
 */
public class ProcessingLFCTraceabilityIT {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProcessingLFCTraceabilityIT.class);
    private static final int DATABASE_PORT = 12346;
    private static final long SLEEP_TIME = 100l;
    private static final long NB_TRY = 4800; // equivalent to 4 minutes
    private static MongodExecutable mongodExecutable;
    static MongodProcess mongod;
    static MongoClient mongoClient;

    private static final Integer tenantId = 0;

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @ClassRule
    public static TemporaryFolder temporaryFolder = new TemporaryFolder();

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
    private static String CONFIG_LOGBOOK_PATH = "";
    private static String CONFIG_SIEGFRIED_PATH = "";

    // private static VitamServer workerApplication;
    private static MetadataMain metadataMain;
    private static WorkerMain workerApplication;
    private static AdminManagementMain adminApplication;
    private static LogbookMain logbookApplication;
    private static WorkspaceMain workspaceMain;
    private static ProcessManagementMain processManagementApplication;
    private WorkspaceClient workspaceClient;
    private ProcessingManagementClient processingClient;
    private static ProcessMonitoringImpl processMonitoring;

    private static final String WORKSPACE_URL = "http://localhost:" + PORT_SERVICE_WORKSPACE;
    private static final String PROCESSING_URL = "http://localhost:" + PORT_SERVICE_PROCESSING;

    private static String WORFKLOW_NAME = "PROCESS_SIP_UNITARY";
    private static String LFC_TRACEABILITY_WORKFLOW = "LOGBOOK_LC_SECURISATION";

    private static String SIP_COMPLEX_RULES = "integration-processing/OK_RULES_COMPLEXE_COMPLETE.zip";

    private static ElasticsearchTestConfiguration config = null;

    private static boolean imported = false;
    private static String defautDataFolder = VitamConfiguration.getVitamDataFolder();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        VitamConfiguration.getConfiguration()
            .setData(PropertiesUtils.getResourcePath("integration-processing/").toString());
        CONFIG_METADATA_PATH = PropertiesUtils.getResourcePath("integration-processing/metadata.conf").toString();
        CONFIG_WORKER_PATH = PropertiesUtils.getResourcePath("integration-processing/worker.conf").toString();
        CONFIG_WORKSPACE_PATH = PropertiesUtils.getResourcePath("integration-processing/workspace.conf").toString();
        CONFIG_PROCESSING_PATH = PropertiesUtils.getResourcePath("integration-processing/processing.conf").toString();
        CONFIG_SIEGFRIED_PATH =
            PropertiesUtils.getResourcePath("integration-processing/format-identifiers.conf").toString();
        CONFIG_FUNCTIONAL_ADMIN_PATH =
            PropertiesUtils.getResourcePath("integration-processing/functional-administration.conf").toString();

        CONFIG_LOGBOOK_PATH = PropertiesUtils.getResourcePath("integration-processing/logbook.conf").toString();
        CONFIG_SIEGFRIED_PATH =
            PropertiesUtils.getResourcePath("integration-processing/format-identifiers.conf").toString();

        File tempFolder = temporaryFolder.newFolder();
        SystemPropertyUtil.set("vitam.tmp.folder", tempFolder.getAbsolutePath());

        // ES
        config = JunitHelper.startElasticsearchForTest(temporaryFolder, CLUSTER_NAME, TCP_PORT, HTTP_PORT);

        final MongodStarter starter = MongodStarter.getDefaultInstance();

        mongodExecutable = starter.prepare(new MongodConfigBuilder()
            .withLaunchArgument("--enableMajorityReadConcern")
            .version(Version.Main.PRODUCTION)
            .net(new Net(DATABASE_PORT, Network.localhostIsIPv6()))
            .build());
        mongod = mongodExecutable.start();

        mongoClient = new MongoClient(new ServerAddress("localhost", DATABASE_PORT));
        // launch metadata
        SystemPropertyUtil.set(MetadataMain.PARAMETER_JETTY_SERVER_PORT,
            Integer.toString(PORT_SERVICE_METADATA));
        metadataMain = new MetadataMain(CONFIG_METADATA_PATH);
        metadataMain.start();
        SystemPropertyUtil.clear(MetadataMain.PARAMETER_JETTY_SERVER_PORT);

        MetaDataClientFactory.changeMode(new ClientConfigurationImpl("localhost", PORT_SERVICE_METADATA));

        // launch workspace
        File workspaceConfigurationFile = PropertiesUtils.findFile(CONFIG_WORKSPACE_PATH);
        final StorageConfiguration workspaceConfiguration =
            PropertiesUtils.readYaml(workspaceConfigurationFile, StorageConfiguration.class);
        workspaceConfiguration.setStoragePath(tempFolder.getAbsolutePath());
        PropertiesUtils.writeYaml(workspaceConfigurationFile, workspaceConfiguration);

        SystemPropertyUtil.set(WorkspaceMain.PARAMETER_JETTY_SERVER_PORT,
            Integer.toString(PORT_SERVICE_WORKSPACE));
        workspaceMain = new WorkspaceMain(CONFIG_WORKSPACE_PATH);
        workspaceMain.start();
        SystemPropertyUtil.clear(WorkspaceMain.PARAMETER_JETTY_SERVER_PORT);

        WorkspaceClientFactory.changeMode(WORKSPACE_URL);

        // launch logbook
        SystemPropertyUtil
            .set(LogbookMain.PARAMETER_JETTY_SERVER_PORT, Integer.toString(PORT_SERVICE_LOGBOOK));
        logbookApplication = new LogbookMain(CONFIG_LOGBOOK_PATH);
        logbookApplication.start();
        SystemPropertyUtil.clear(LogbookMain.PARAMETER_JETTY_SERVER_PORT);

        LogbookOperationsClientFactory.changeMode(new ClientConfigurationImpl("localhost", PORT_SERVICE_LOGBOOK));
        LogbookLifeCyclesClientFactory.changeMode(new ClientConfigurationImpl("localhost", PORT_SERVICE_LOGBOOK));

        // launch processing
        SystemPropertyUtil.set(ProcessManagementMain.PARAMETER_JETTY_SERVER_PORT,
            Integer.toString(PORT_SERVICE_PROCESSING));
        processManagementApplication = new ProcessManagementMain(CONFIG_PROCESSING_PATH);
        processManagementApplication.start();
        SystemPropertyUtil.clear(ProcessManagementMain.PARAMETER_JETTY_SERVER_PORT);

        ProcessingManagementClientFactory.changeConfigurationUrl(PROCESSING_URL);

        // launch worker
        SystemPropertyUtil.set("jetty.worker.port", Integer.toString(PORT_SERVICE_WORKER));
        workerApplication = new WorkerMain(CONFIG_WORKER_PATH);
        workerApplication.start();
        SystemPropertyUtil.clear("jetty.worker.port");

        FormatIdentifierFactory.getInstance().changeConfigurationFile(CONFIG_SIEGFRIED_PATH);

        // launch functional Admin server
        adminApplication = new AdminManagementMain(CONFIG_FUNCTIONAL_ADMIN_PATH);
        adminApplication.start();

        AdminManagementClientFactory
            .changeMode(new ClientConfigurationImpl("localhost", PORT_SERVICE_FUNCTIONAL_ADMIN));


        processMonitoring = ProcessMonitoringImpl.getInstance();

    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        VitamConfiguration.getConfiguration().setData(defautDataFolder);
        if (config != null) {
            JunitHelper.stopElasticsearchForTest(config);
        }
        if (mongod != null) {
            mongod.stop();
        }
        if (mongodExecutable != null) {
            mongodExecutable.stop();
        }
        if (workspaceMain != null) {
            workspaceMain.stop();
        }
        if (adminApplication != null) {
            adminApplication.stop();
        }
        if (workerApplication != null) {
            workerApplication.stop();
        }
        if (logbookApplication != null) {
            logbookApplication.stop();
        }
        if (processManagementApplication != null) {
            processManagementApplication.stop();
        }
        if (metadataMain != null) {
            metadataMain.stop();
        }
        if (mongoClient != null) {
            mongoClient.close();
        }
    }


    @After
    public void afterTest() {
        MongoDatabase db = mongoClient.getDatabase("Vitam");
        db.getCollection("Unit").deleteMany(new Document());
        db.getCollection("ObjectGroup").deleteMany(new Document());
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

        RestAssured.port = PORT_SERVICE_LOGBOOK;
        RestAssured.basePath = LOGBOOK_PATH;
        get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());
    }

    private void tryImportFile() {
        VitamThreadUtils.getVitamSession().setContextId("Context_IT");
        flush();

        if (!imported) {
            try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
                VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(tenantId));
                client.importFormat(
                    PropertiesUtils.getResourceAsStream("integration-processing/DROID_SignatureFile_V88.xml"),
                    "DROID_SignatureFile_V88.xml");

                // Import Rules
                VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(tenantId));
                client.importRulesFile(
                    PropertiesUtils.getResourceAsStream("integration-processing/jeu_donnees_OK_regles_CSV_regles.csv"),
                    "jeu_donnees_OK_regles_CSV_regles.csv");

                VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(tenantId));
                client.importAgenciesFile(PropertiesUtils.getResourceAsStream("agencies.csv"), "agencies.csv");

                File fileProfiles = PropertiesUtils.getResourceFile("integration-processing/OK_profil.json");
                List<ProfileModel> profileModelList =
                    JsonHandler.getFromFileAsTypeRefence(fileProfiles, new TypeReference<List<ProfileModel>>() {
                    });
                VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(tenantId));
                client.createProfiles(profileModelList);

                VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(tenantId));
                RequestResponseOK<ProfileModel> response =
                    (RequestResponseOK<ProfileModel>) client.findProfiles(new Select().getFinalSelect());
                VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(tenantId));
                client.importProfileFile(response.getResults().get(0).getIdentifier(),
                    PropertiesUtils.getResourceAsStream("integration-processing/profil_ok.rng"));

                // import contract
                VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(tenantId));
                File fileContracts =
                    PropertiesUtils.getResourceFile("integration-processing/referential_contracts_ok.json");
                List<IngestContractModel> IngestContractModelList = JsonHandler.getFromFileAsTypeRefence(fileContracts,
                    new TypeReference<List<IngestContractModel>>() {
                    });

                VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(tenantId));
                Status importStatus = client.importIngestContracts(IngestContractModelList);

                // Import Security Profile
                VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(tenantId));
                client.importSecurityProfiles(JsonHandler
                    .getFromFileAsTypeRefence(
                        PropertiesUtils.getResourceFile("integration-processing/security_profile_ok.json"),
                        new TypeReference<List<SecurityProfileModel>>() {
                        }));

                // Import Context
                VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(tenantId));
                client.importContexts(JsonHandler
                    .getFromFileAsTypeRefence(PropertiesUtils.getResourceFile("integration-processing/contexts.json"),
                        new TypeReference<List<ContextModel>>() {
                        }));
            } catch (final Exception e) {
                LOGGER.error(e);
            }
            imported = true;
        }
    }

    private void flush() {
        ProcessDataAccessImpl.getInstance().clearWorkflow();
    }

    private void wait(String operationId) {
        int nbTry = 0;
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

    private void createLogbookOperation(GUID operationId, GUID objectId)
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException {
        createLogbookOperation(operationId, objectId, null);
    }

    private void createLogbookOperation(GUID operationId, GUID objectId, String type)
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException {

        final LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient();
        if (type == null) {
            type = "Process_SIP_unitary";
        }
        final LogbookOperationParameters initParameters = LogbookParametersFactory.newLogbookOperationParameters(
            operationId, type, objectId,
            "Process_SIP_unitary".equals(type) ? LogbookTypeProcess.INGEST : LogbookTypeProcess.TRACEABILITY,
            StatusCode.STARTED,
            operationId != null ? operationId.toString() : "outcomeDetailMessage",
            operationId);

        logbookClient.create(initParameters);
    }

    private void launchIngest() throws Exception {
        final GUID operationGuid2 = GUIDFactory.newOperationLogbookGUID(tenantId);
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid2);
        final GUID objectGuid2 = GUIDFactory.newManifestGUID(tenantId);
        final String containerName2 = objectGuid2.getId();
        createLogbookOperation(operationGuid2, objectGuid2);

        RestAssured.port = PORT_SERVICE_WORKSPACE;
        RestAssured.basePath = WORKSPACE_PATH;
        final InputStream zipInputStreamSipObject =
            PropertiesUtils.getResourceAsStream(SIP_COMPLEX_RULES);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName2);
        workspaceClient.uncompressObject(containerName2, SIP_FOLDER, CommonMediaType.ZIP,
            zipInputStreamSipObject);

        // call processing
        RestAssured.port = PORT_SERVICE_PROCESSING;
        RestAssured.basePath = PROCESSING_PATH;
        processingClient = ProcessingManagementClientFactory.getInstance().getClient();
        processingClient.initVitamProcess(Contexts.DEFAULT_WORKFLOW.name(), containerName2, WORFKLOW_NAME);
        final RequestResponse<JsonNode> ret2 =
            processingClient.executeOperationProcess(containerName2, WORFKLOW_NAME,
                Contexts.DEFAULT_WORKFLOW.name(), ProcessAction.RESUME.getValue());
        assertNotNull(ret2);
        assertEquals(Status.ACCEPTED.getStatusCode(), ret2.getStatus());
        wait(containerName2);
        ProcessWorkflow processWorkflow2 =
            processMonitoring.findOneProcessWorkflow(containerName2, tenantId);
        assertNotNull(processWorkflow2);
        assertEquals(ProcessState.COMPLETED, processWorkflow2.getState());
        assertEquals(StatusCode.OK, processWorkflow2.getStatus());
    }

    private String launchLogbookLFC(int overlapDelay) throws Exception {
        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        final GUID objectGuid = GUIDFactory.newManifestGUID(tenantId);
        final String containerName = objectGuid.getId();
        createLogbookOperation(operationGuid, objectGuid, "LOGBOOK_LC_SECURISATION");

        RestAssured.port = PORT_SERVICE_WORKSPACE;
        RestAssured.basePath = WORKSPACE_PATH;
        workspaceClient.createContainer(containerName);

        // lets call traceability for lifecycles
        RestAssured.port = PORT_SERVICE_PROCESSING;
        RestAssured.basePath = PROCESSING_PATH;

        ProcessingEntry processingEntry = new ProcessingEntry(containerName, LFC_TRACEABILITY_WORKFLOW);
        processingEntry.getExtraParams().put(
            WorkerParameterName.lifecycleTraceabilityOverlapDelayInSeconds.name(), Integer.toString(overlapDelay));

        processingClient.initVitamProcess(Contexts.SECURISATION_LC.name(), processingEntry);
        RequestResponse<ItemStatus> ret =
            processingClient.updateOperationActionProcess(ProcessAction.RESUME.getValue(), containerName);
        assertNotNull(ret);

        assertEquals(Status.ACCEPTED.getStatusCode(), ret.getStatus());
        return containerName;
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowLFCTraceability() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        tryImportFile();

        launchIngest();

        // Launch securization for the first time (no overlap delay)

        String containerName = launchLogbookLFC(0);
        wait(containerName);

        ProcessWorkflow processWorkflow =
            processMonitoring.findOneProcessWorkflow(containerName, tenantId);
        assertNotNull(processWorkflow);
        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
        assertEquals(StatusCode.OK, processWorkflow.getStatus());

        // Launch securization a second time, with no lfc added (no overlap delay)
        String containerName2nd = launchLogbookLFC(0);
        wait(containerName2nd);
        ProcessWorkflow processWorkflow2nd =
            processMonitoring.findOneProcessWorkflow(containerName2nd, tenantId);
        assertNotNull(processWorkflow2nd);
        assertEquals(ProcessState.COMPLETED, processWorkflow2nd.getState());
        // SINCE NO LFC ARE HANDLED, WARNING
        assertEquals(StatusCode.WARNING, processWorkflow2nd.getStatus());

        // Launch securization a third time, with no lfc added but with overlap delay
        String containerName3rd = launchLogbookLFC(300);
        wait(containerName3rd);
        ProcessWorkflow processWorkflow3rd =
            processMonitoring.findOneProcessWorkflow(containerName3rd, tenantId);
        assertNotNull(processWorkflow3rd);
        assertEquals(ProcessState.COMPLETED, processWorkflow3rd.getState());
        // OK since old ingest should be reselected
        assertEquals(StatusCode.OK, processWorkflow3rd.getStatus());

        // launch an ingest
        launchIngest();

        // Launch securization with new lfc added (even with no overlap delay)
        String containerName4th = launchLogbookLFC(0);
        wait(containerName4th);
        ProcessWorkflow processWorkflow4th =
            processMonitoring.findOneProcessWorkflow(containerName4th, tenantId);
        assertNotNull(processWorkflow4th);
        assertEquals(ProcessState.COMPLETED, processWorkflow4th.getState());
        assertEquals(StatusCode.OK, processWorkflow4th.getStatus());

        // simulate audit, update some object LFC
        changeOneGotLFC();
        // simulate unit update, update some unit LFC
        changeOneUnitLFC();


        // Launch securization a second time, with lfc updated > so status OK (even with no overlap delay)
        String containerName5th = launchLogbookLFC(0);
        wait(containerName5th);
        ProcessWorkflow processWorkflow5th =
            processMonitoring.findOneProcessWorkflow(containerName5th, tenantId);
        assertNotNull(processWorkflow5th);
        assertEquals(ProcessState.COMPLETED, processWorkflow5th.getState());
        assertEquals(StatusCode.OK, processWorkflow5th.getStatus());
    }

    private void changeOneGotLFC() throws Exception {
        try (LogbookLifeCyclesClient logbookLifeCyclesClient = LogbookLifeCyclesClientFactory.getInstance()
            .getClient()) {
            // search for got lfc
            final Query parentQuery = QueryHelper.gte("evDateTime", LocalDateTime.MIN.toString());
            final Select select = new Select();
            select.setQuery(parentQuery);
            select.addOrderByAscFilter("evDateTime");
            RequestResponseOK requestResponseOK = RequestResponseOK.getFromJsonNode(
                logbookLifeCyclesClient.selectObjectGroupLifeCycle(select.getFinalSelect()));
            List<JsonNode> foundObjectGroupLifecycles = requestResponseOK.getResults();
            assertTrue(foundObjectGroupLifecycles != null && foundObjectGroupLifecycles.size() > 0);

            // get one got lfc
            String oneGotLfc = foundObjectGroupLifecycles.get(0).get(LogbookDocument.ID).asText();

            // update got lfc
            final GUID updateLfcGuidStart = GUIDFactory.newOperationLogbookGUID(tenantId);
            LogbookLifeCycleObjectGroupParameters logbookLifeGotUpdateParameters =
                LogbookParametersFactory.newLogbookLifeCycleObjectGroupParameters(updateLfcGuidStart,
                    VitamLogbookMessages.getEventTypeLfc("AUDIT_CHECK_OBJECT"),
                    updateLfcGuidStart,
                    LogbookTypeProcess.AUDIT, StatusCode.KO,
                    VitamLogbookMessages.getOutcomeDetailLfc("AUDIT_CHECK_OBJECT", StatusCode.KO),
                    VitamLogbookMessages.getCodeLfc("AUDIT_CHECK_OBJECT", StatusCode.KO),
                    GUIDReader.getGUID(oneGotLfc));

            logbookLifeCyclesClient.update(logbookLifeGotUpdateParameters);
            logbookLifeCyclesClient.commit(logbookLifeGotUpdateParameters);
        }
    }

    private void changeOneUnitLFC() throws Exception {
        try (LogbookLifeCyclesClient logbookLifeCyclesClient = LogbookLifeCyclesClientFactory.getInstance()
            .getClient()) {
            // search for got lfc
            final Query parentQuery = QueryHelper.gte("evDateTime", LocalDateTime.MIN.toString());
            final Select select = new Select();
            select.setQuery(parentQuery);
            select.addOrderByAscFilter("evDateTime");
            RequestResponseOK requestResponseOK = RequestResponseOK.getFromJsonNode(
                logbookLifeCyclesClient.selectUnitLifeCycle(select.getFinalSelect()));
            List<JsonNode> foundUnitLifecycles = requestResponseOK.getResults();
            assertTrue(foundUnitLifecycles != null && foundUnitLifecycles.size() > 0);

            // get one got lfc
            String oneUnitLfc = foundUnitLifecycles.get(0).get(LogbookDocument.ID).asText();

            // update got lfc
            final GUID updateLfcGuidStart = GUIDFactory.newOperationLogbookGUID(tenantId);
            LogbookLifeCycleUnitParameters logbookLifeUnitUpdateParameters =
                LogbookParametersFactory.newLogbookLifeCycleUnitParameters(updateLfcGuidStart,
                    VitamLogbookMessages.getEventTypeLfc("UNIT_UPDATE"),
                    updateLfcGuidStart,
                    LogbookTypeProcess.UPDATE, StatusCode.OK,
                    VitamLogbookMessages.getOutcomeDetailLfc("UNIT_UPDATE", StatusCode.OK),
                    VitamLogbookMessages.getCodeLfc("UNIT_UPDATE", StatusCode.OK),
                    GUIDReader.getGUID(oneUnitLfc));

            logbookLifeCyclesClient.update(logbookLifeUnitUpdateParameters);
            logbookLifeCyclesClient.commit(logbookLifeUnitUpdateParameters);
        }
    }
}
