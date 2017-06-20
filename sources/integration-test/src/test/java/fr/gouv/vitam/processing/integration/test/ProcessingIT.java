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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.bson.Document;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.restassured.RestAssured;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.model.Filters;

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
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.client.configuration.ClientConfigurationImpl;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.GLOBAL;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTION;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTIONARGS;
import fr.gouv.vitam.common.database.builder.request.multiple.InsertMultiQuery;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.format.identification.FormatIdentifierFactory;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.guid.GUIDReader;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.junit.JunitHelper.ElasticsearchTestConfiguration;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.client.model.IngestContractModel;
import fr.gouv.vitam.functional.administration.client.model.ProfileModel;
import fr.gouv.vitam.functional.administration.rest.AdminManagementApplication;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.Contexts;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.logbook.rest.LogbookApplication;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.metadata.core.UnitInheritedRule;
import fr.gouv.vitam.metadata.rest.MetaDataApplication;
import fr.gouv.vitam.processing.common.exception.ProcessingStorageWorkspaceException;
import fr.gouv.vitam.processing.common.model.ProcessWorkflow;
import fr.gouv.vitam.processing.data.core.ProcessDataAccessImpl;
import fr.gouv.vitam.processing.data.core.management.ProcessDataManagement;
import fr.gouv.vitam.processing.data.core.management.WorkspaceProcessDataManagement;
import fr.gouv.vitam.processing.engine.core.monitoring.ProcessMonitoringImpl;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.processing.management.rest.ProcessManagementApplication;
import fr.gouv.vitam.worker.server.rest.WorkerApplication;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import fr.gouv.vitam.workspace.rest.WorkspaceApplication;

/**
 * Processing integration test
 */
public class ProcessingIT {
    private static final String UNIT_ATTACHEMENT_ID = "aeaqaaaaaagbcaacaang6ak4ts6paliaaaaq";
    private static final String UNIT_PLAN_ATTACHEMENT_ID = "aeaqaaaaaagbcaacabht2ak4x66x2baaaaaq";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProcessingIT.class);
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
    private static String CONFIG_BIG_WORKER_PATH = "";
    private static String CONFIG_WORKSPACE_PATH = "";
    private static String CONFIG_METADATA_PATH = "";
    private static String CONFIG_PROCESSING_PATH = "";
    private static String CONFIG_FUNCTIONAL_ADMIN_PATH = "";
    private static String CONFIG_FUNCTIONAL_CLIENT_PATH = "";
    private static String CONFIG_LOGBOOK_PATH = "";
    private static String CONFIG_SIEGFRIED_PATH = "";

    // private static VitamServer workerApplication;
    private static MetaDataApplication metadataApplication;
    private static WorkerApplication wkrapplication;
    private static AdminManagementApplication adminApplication;
    private static LogbookApplication lgbapplication;
    private static WorkspaceApplication workspaceApplication;
    private static ProcessManagementApplication processManagementApplication;
    private WorkspaceClient workspaceClient;
    private ProcessingManagementClient processingClient;
    private static ProcessMonitoringImpl processMonitoring;

    private static final String WORKSPACE_URL = "http://localhost:" + PORT_SERVICE_WORKSPACE;
    private static final String PROCESSING_URL = "http://localhost:" + PORT_SERVICE_PROCESSING;

    private static String WORFKLOW_NAME_2 = "DefaultIngestWorkflow";
    private static String WORFKLOW_NAME = "DefaultIngestWorkflow";
    private static String INGEST_TREE_WORFKLOW = "DefaultHoldingSchemeWorkflow";
    private static String INGEST_PLAN_WORFKLOW = "DefaultFilingSchemeWorkflow";
    private static String BIG_WORFKLOW_NAME = "BigIngestWorkflow";
    private static String SIP_FILE_OK_NAME = "integration-processing/SIP-test.zip";
    private static String SIP_PROFIL_OK = "integration-processing/SIP_ok_profil.zip";
    private static String SIP_FILE_OK_WITH_SYSTEMID = "integration-processing/SIP_with_systemID.zip";
    // TODO : use for IT test to add a link between two AUs (US 1686)
    private static String SIP_FILE_AU_LINK_OK_NAME_TARGET = "integration-processing";
    // TODO : use for IT test to add a link between two AUs (US 1686)
    private static String SIP_FILE_AU_LINK_OK_NAME = "integration-processing/OK_SIP_AU_LINK";
    private static String SIP_FILE_ADD_AU_LINK_OK_NAME_TARGET = "integration-processing";
    private static String SIP_FILE_ADD_AU_LINK_OK_NAME = "integration-processing/OK_SIP_ADD_AU_LINK";

    private static String LINK_AU_TO_EXISTING_GOT_OK_NAME = "integration-processing/OK_LINK_AU_TO_EXISTING_GOT";
    private static String LINK_AU_TO_EXISTING_GOT_OK_NAME_TARGET = "integration-processing";
    private static String SIP_FILE_TAR_OK_NAME = "integration-processing/SIP.tar";
    private static String SIP_INHERITED_RULE_CA1_OK = "integration-processing/1069_CA1.zip";
    private static String SIP_INHERITED_RULE_CA4_OK = "integration-processing/1069_CA4.zip";
    private static String SIP_ARBO_COMPLEXE_FILE_OK = "integration-processing/OK-registre-fonds.zip";
    private static String SIP_FUND_REGISTER_OK = "integration-processing/OK-registre-fonds.zip";
    private static String SIP_WITHOUT_MANIFEST = "integration-processing/SIP_no_manifest.zip";
    private static String SIP_NO_FORMAT = "integration-processing/SIP_NO_FORMAT.zip";
    private static String SIP_DOUBLE_BM = "integration-processing/SIP_DoubleBM.zip";
    private static String SIP_NO_FORMAT_NO_TAG = "integration-processing/SIP_NO_FORMAT_TAG.zip";
    private static String SIP_NB_OBJ_INCORRECT_IN_MANIFEST = "integration-processing/SIP_Conformity_KO.zip";
    private static String SIP_ORPHELINS = "integration-processing/SIP-orphelins.zip";
    private static String SIP_OBJECT_SANS_GOT = "integration-processing/SIP-objetssansGOT.zip";
    private static String SIP_BUG_2721 = "integration-processing/bug2721_2racines_meme_rattachement.zip";
    private static String SIP_WITHOUT_OBJ = "integration-processing/OK_SIP_sans_objet.zip";
    private static String SIP_WITHOUT_FUND_REGISTER = "integration-processing/KO_registre_des_fonds.zip";
    private static String SIP_BORD_AU_REF_PHYS_OBJECT = "integration-processing/KO_BORD_AUrefphysobject.zip";
    private static String SIP_MANIFEST_INCORRECT_REFERENCE = "integration-processing/KO_Reference_Unexisting.zip";
    private static String SIP_REFERENCE_CONTRACT_KO = "integration-processing/KO_SIP_2_GO_contract.zip";

    private static String SIP_FILE_KO_AU_REF_BDO = "integration-processing/SIP_KO_ArchiveUnit_ref_BDO.zip";
    private static String SIP_BUG_2182 = "integration-processing/SIP_bug_2182.zip";
    private static String SIP_ARBRE = "integration-processing/test_arbre.zip";
    private static String SIP_PLAN = "integration-processing/test_plan.zip";
    private static String SIP_FILE_1791_CA1 = "integration-processing/SIP_FILE_1791_CA1.zip";
    private static String SIP_FILE_1791_CA2 = "integration-processing/SIP_FILE_1791_CA2.zip";

    private static ElasticsearchTestConfiguration config = null;

    private final static String DUMMY_REQUEST_ID = "reqId";
    private static boolean imported = false;
    private static String defautDataFolder = VitamConfiguration.getVitamDataFolder();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        VitamConfiguration.getConfiguration()
            .setData(PropertiesUtils.getResourcePath("integration-processing/").toString());
        CONFIG_METADATA_PATH = PropertiesUtils.getResourcePath("integration-processing/metadata.conf").toString();
        CONFIG_WORKER_PATH = PropertiesUtils.getResourcePath("integration-processing/worker.conf").toString();
        CONFIG_BIG_WORKER_PATH = PropertiesUtils.getResourcePath("integration-processing/bigworker.conf").toString();
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

        File tempFolder = temporaryFolder.newFolder();
        System.setProperty("vitam.tmp.folder", tempFolder.getAbsolutePath());

        SystemPropertyUtil.refresh();


        // ES
        config = JunitHelper.startElasticsearchForTest(temporaryFolder, CLUSTER_NAME, TCP_PORT, HTTP_PORT);

        final MongodStarter starter = MongodStarter.getDefaultInstance();

        mongodExecutable = starter.prepare(new MongodConfigBuilder()
            .version(Version.Main.PRODUCTION)
            .net(new Net(DATABASE_PORT, Network.localhostIsIPv6()))
            .build());
        mongod = mongodExecutable.start();

        mongoClient = new MongoClient(new ServerAddress("localhost", DATABASE_PORT));
        // launch metadata
        SystemPropertyUtil.set(MetaDataApplication.PARAMETER_JETTY_SERVER_PORT,
            Integer.toString(PORT_SERVICE_METADATA));
        metadataApplication = new MetaDataApplication(CONFIG_METADATA_PATH);
        metadataApplication.start();
        SystemPropertyUtil.clear(MetaDataApplication.PARAMETER_JETTY_SERVER_PORT);

        MetaDataClientFactory.changeMode(new ClientConfigurationImpl("localhost", PORT_SERVICE_METADATA));

        // launch workspace
        SystemPropertyUtil.set(WorkspaceApplication.PARAMETER_JETTY_SERVER_PORT,
            Integer.toString(PORT_SERVICE_WORKSPACE));
        workspaceApplication = new WorkspaceApplication(CONFIG_WORKSPACE_PATH);
        workspaceApplication.start();
        SystemPropertyUtil.clear(WorkspaceApplication.PARAMETER_JETTY_SERVER_PORT);

        WorkspaceClientFactory.changeMode(WORKSPACE_URL);

        // launch logbook
        SystemPropertyUtil
            .set(LogbookApplication.PARAMETER_JETTY_SERVER_PORT, Integer.toString(PORT_SERVICE_LOGBOOK));
        lgbapplication = new LogbookApplication(CONFIG_LOGBOOK_PATH);
        lgbapplication.start();
        SystemPropertyUtil.clear(LogbookApplication.PARAMETER_JETTY_SERVER_PORT);

        LogbookOperationsClientFactory.changeMode(new ClientConfigurationImpl("localhost", PORT_SERVICE_LOGBOOK));
        LogbookLifeCyclesClientFactory.changeMode(new ClientConfigurationImpl("localhost", PORT_SERVICE_LOGBOOK));

        // launch processing
        SystemPropertyUtil.set(ProcessManagementApplication.PARAMETER_JETTY_SERVER_PORT,
            Integer.toString(PORT_SERVICE_PROCESSING));
        processManagementApplication = new ProcessManagementApplication(CONFIG_PROCESSING_PATH);
        processManagementApplication.start();
        SystemPropertyUtil.clear(ProcessManagementApplication.PARAMETER_JETTY_SERVER_PORT);

        ProcessingManagementClientFactory.changeConfigurationUrl(PROCESSING_URL);

        // launch worker
        SystemPropertyUtil.set("jetty.worker.port", Integer.toString(PORT_SERVICE_WORKER));
        wkrapplication = new WorkerApplication(CONFIG_WORKER_PATH);
        wkrapplication.start();
        SystemPropertyUtil.clear("jetty.worker.port");

        FormatIdentifierFactory.getInstance().changeConfigurationFile(CONFIG_SIEGFRIED_PATH);

        // launch functional Admin server
        adminApplication = new AdminManagementApplication(CONFIG_FUNCTIONAL_ADMIN_PATH);
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

        mongod.stop();
        mongodExecutable.stop();

        try {
            workspaceApplication.stop();
            adminApplication.stop();
            wkrapplication.stop();
            lgbapplication.stop();
            processManagementApplication.stop();
            metadataApplication.stop();
            mongoClient.close();
        } catch (final Exception e) {
            LOGGER.error(e);
        }
    }


    @After
    public void afterTest() {
        MongoDatabase db = mongoClient.getDatabase("Vitam");
        db.getCollection("Unit") .deleteMany(new Document());
        db.getCollection("ObjectGroup") .deleteMany(new Document());
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
        } catch (final Exception e) {
            e.printStackTrace();
            fail("should not raized an exception");
        }
    }

    private void tryImportFile() {
        flush();

        if (!imported) {
            try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
                client
                    .importFormat(
                        PropertiesUtils.getResourceAsStream("integration-processing/DROID_SignatureFile_V88.xml"));

                // Import Rules
                client.importRulesFile(
                    PropertiesUtils.getResourceAsStream("integration-processing/jeu_donnees_OK_regles_CSV_regles.csv"));

                File fileProfiles = PropertiesUtils.getResourceFile("integration-processing/OK_profil.json");
                List<ProfileModel> profileModelList = JsonHandler.getFromFileAsTypeRefence(fileProfiles, new TypeReference<List<ProfileModel>>(){});
                RequestResponse improrResponse = client.createProfiles(profileModelList);

                RequestResponseOK<ProfileModel> response = (RequestResponseOK<ProfileModel>) client.findProfiles(new Select().getFinalSelect());
                client.importProfileFile(response.getResults().get(0).getId(),
                    PropertiesUtils.getResourceAsStream("integration-processing/Profil20.rng"));

                MetaDataClient metaDataClient = MetaDataClientFactory.getInstance().getClient();
                LogbookLifeCyclesClient logbookLFCClient = LogbookLifeCyclesClientFactory.getInstance().getClient();
                metaDataClient.insertUnit(
                    new InsertMultiQuery().addData((ObjectNode)
                        JsonHandler.getFromFile(PropertiesUtils.getResourceFile("integration-processing/unit_plan_metadata.json"))).getFinalInsert());
                logbookLFCClient.create(
                    LogbookParametersFactory.newLogbookLifeCycleUnitParameters(
                        GUIDFactory.newOperationLogbookGUID(tenantId),
                        "INGEST",
                        GUIDFactory.newOperationLogbookGUID(tenantId),
                        LogbookTypeProcess.INGEST,
                        StatusCode.OK,
                        "Process_SIP_unitary.OK",
                        UNIT_PLAN_ATTACHEMENT_ID,
                        GUIDReader.getGUID(UNIT_PLAN_ATTACHEMENT_ID)));
                
                // import contract
                File fileContracts = PropertiesUtils.getResourceFile("integration-processing/referential_contracts_ok.json");
                List<IngestContractModel> IngestContractModelList = JsonHandler.getFromFileAsTypeRefence(fileContracts, new TypeReference<List<IngestContractModel>>(){});

                Status importStatus = client.importIngestContracts(IngestContractModelList);
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
        while (! processingClient.isOperationCompleted(operationId)) {
            try {
                Thread.sleep(SLEEP_TIME);
            } catch (InterruptedException e) {
                SysErrLogger.FAKE_LOGGER.ignoreLog(e);
            }
            if (nbTry == NB_TRY) break;
            nbTry ++;
        }
    }
    /**
     * This test needs Siegfried already running and started as:<br/>
     * sf -server localhost:8999<br/>
     * <br/>
     * If not started, this test will be ignored.
     *
     * @throws Exception
     */
    @Test
    public void testTryWithSiegfried() throws Exception {
        final String CONFIG_SIEGFRIED_PATH_REAL =
            PropertiesUtils.getResourcePath("integration-processing/format-identifiers-real.conf").toString();
        try {
            FormatIdentifierFactory.getInstance().changeConfigurationFile(CONFIG_SIEGFRIED_PATH_REAL);
            FormatIdentifierFactory.getInstance().getFormatIdentifierFor("siegfried-local").status();
            testWorkflow();
        } catch (final Exception e) {
            // Ignore
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
            Assume.assumeTrue("Real Siegfried not running", false);
        } finally {
            FormatIdentifierFactory.getInstance().changeConfigurationFile(CONFIG_SIEGFRIED_PATH);
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflow() throws Exception {
        try {
            VitamThreadUtils.getVitamSession().setTenantId(tenantId);
            tryImportFile();
            final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            final GUID objectGuid = GUIDFactory.newManifestGUID(tenantId);
            final String containerName = objectGuid.getId();
            createLogbookOperation(operationGuid, objectGuid);

            // workspace client dezip SIP in workspace
            RestAssured.port = PORT_SERVICE_WORKSPACE;
            RestAssured.basePath = WORKSPACE_PATH;
            final InputStream zipInputStreamSipObject =
                PropertiesUtils.getResourceAsStream(SIP_BUG_2721);
            workspaceClient = WorkspaceClientFactory.getInstance().getClient();
            workspaceClient.createContainer(containerName);
            workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP,
                zipInputStreamSipObject);
            // call processing
            RestAssured.port = PORT_SERVICE_PROCESSING;
            RestAssured.basePath = PROCESSING_PATH;

            MetaDataClient metaDataClient = MetaDataClientFactory.getInstance().getClient();
            metaDataClient.insertUnit(
                new InsertMultiQuery().addData((ObjectNode)
                    JsonHandler.getFromFile(PropertiesUtils.getResourceFile("integration-processing/unit_metadata.json"))).getFinalInsert());
            LogbookLifeCyclesClient logbookLFCClient = LogbookLifeCyclesClientFactory.getInstance().getClient();
            logbookLFCClient.create(
                LogbookParametersFactory.newLogbookLifeCycleUnitParameters(
                    GUIDFactory.newOperationLogbookGUID(tenantId),
                    "INGEST",
                    GUIDFactory.newOperationLogbookGUID(tenantId),
                    LogbookTypeProcess.INGEST,
                    StatusCode.OK,
                    "Process_SIP_unitary.OK",
                    UNIT_ATTACHEMENT_ID,
                    GUIDReader.getGUID(UNIT_ATTACHEMENT_ID)));

            processingClient = ProcessingManagementClientFactory.getInstance().getClient();
            processingClient.initVitamProcess(Contexts.DEFAULT_WORKFLOW.name(), containerName, WORFKLOW_NAME);

            final Response ret =
                processingClient.updateOperationActionProcess(ProcessAction.RESUME.getValue(), containerName);
            assertNotNull(ret);
            assertEquals(Status.ACCEPTED.getStatusCode(), ret.getStatus());

            wait(containerName);
            ProcessWorkflow processWorkflow =
                processMonitoring.findOneProcessWorkflow(containerName, tenantId);
            assertNotNull(processWorkflow);
            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
            assertEquals(StatusCode.WARNING, processWorkflow.getStatus());

            LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient();
            fr.gouv.vitam.common.database.builder.request.single.Select selectQuery =
                new fr.gouv.vitam.common.database.builder.request.single.Select();
            selectQuery.setQuery(QueryHelper.eq("evIdProc", containerName));
            JsonNode logbookResult = logbookClient.selectOperation(selectQuery.getFinalSelect());

            assertEquals(logbookResult.get("$results").get(0).get("events").get(0).get("outDetail").asText(),
                "STP_INGEST_FINALISATION.OK");
        } catch (final Exception e) {
            e.printStackTrace();
            fail("should not raized an exception");
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowProfil() throws Exception {
        try {
            VitamThreadUtils.getVitamSession().setTenantId(tenantId);
            tryImportFile();
            final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            final GUID objectGuid = GUIDFactory.newManifestGUID(tenantId);
            final String containerName = objectGuid.getId();
            createLogbookOperation(operationGuid, objectGuid);

            // workspace client dezip SIP in workspace
            RestAssured.port = PORT_SERVICE_WORKSPACE;
            RestAssured.basePath = WORKSPACE_PATH;
            final InputStream zipInputStreamSipObject =
                PropertiesUtils.getResourceAsStream(SIP_PROFIL_OK);
            workspaceClient = WorkspaceClientFactory.getInstance().getClient();
            workspaceClient.createContainer(containerName);
            workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP,
                zipInputStreamSipObject);
            // call processing
            RestAssured.port = PORT_SERVICE_PROCESSING;
            RestAssured.basePath = PROCESSING_PATH;

            processingClient = ProcessingManagementClientFactory.getInstance().getClient();
            processingClient.initVitamProcess(Contexts.DEFAULT_WORKFLOW.name(), containerName, WORFKLOW_NAME);

            final RequestResponse<JsonNode> ret =
                processingClient.executeOperationProcess(containerName, WORFKLOW_NAME,
                    Contexts.DEFAULT_WORKFLOW.name(), ProcessAction.RESUME.getValue());

            assertNotNull(ret);
            assertEquals(Status.ACCEPTED.getStatusCode(), ret.getStatus());

            wait(containerName);
            ProcessWorkflow processWorkflow =
                processMonitoring.findOneProcessWorkflow(containerName, tenantId);
            assertNotNull(processWorkflow);
            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
        } catch (final Exception e) {
            e.printStackTrace();
            fail("should not raized an exception");
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowIngestTree() throws Exception {
        try {
            VitamThreadUtils.getVitamSession().setTenantId(tenantId);
            tryImportFile();
            final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            final GUID objectGuid = GUIDFactory.newManifestGUID(tenantId);
            final String containerName = objectGuid.getId();
            createLogbookOperation(operationGuid, objectGuid);

            // workspace client dezip SIP in workspace
            RestAssured.port = PORT_SERVICE_WORKSPACE;
            RestAssured.basePath = WORKSPACE_PATH;
            final InputStream zipInputStreamSipObject =
                PropertiesUtils.getResourceAsStream(SIP_ARBRE);
            workspaceClient = WorkspaceClientFactory.getInstance().getClient();
            workspaceClient.createContainer(containerName);
            workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP,
                zipInputStreamSipObject);
            // call processing
            RestAssured.port = PORT_SERVICE_PROCESSING;
            RestAssured.basePath = PROCESSING_PATH;

            processingClient = ProcessingManagementClientFactory.getInstance().getClient();
            processingClient.initVitamProcess(Contexts.HOLDING_SCHEME.name(), containerName, INGEST_TREE_WORFKLOW);
            final Response ret =
                processingClient.updateOperationActionProcess(ProcessAction.RESUME.getValue(), containerName);
            assertNotNull(ret);

            assertEquals(Status.ACCEPTED.getStatusCode(), ret.getStatus());

            wait(containerName);
            ProcessWorkflow processWorkflow =
                processMonitoring.findOneProcessWorkflow(containerName, tenantId);
            assertNotNull(processWorkflow);
            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
            assertEquals(StatusCode.OK, processWorkflow.getStatus());
        } catch (final Exception e) {
            e.printStackTrace();
            fail("should not raized an exception");
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowIngestPlan() throws Exception {
        try {
            VitamThreadUtils.getVitamSession().setTenantId(tenantId);
            tryImportFile();
            final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            final GUID objectGuid = GUIDFactory.newManifestGUID(tenantId);
            final String containerName = objectGuid.getId();
            createLogbookOperation(operationGuid, objectGuid);

            // workspace client dezip SIP in workspace
            RestAssured.port = PORT_SERVICE_WORKSPACE;
            RestAssured.basePath = WORKSPACE_PATH;
            final InputStream zipInputStreamSipObject =
                PropertiesUtils.getResourceAsStream(SIP_PLAN);
            workspaceClient = WorkspaceClientFactory.getInstance().getClient();
            workspaceClient.createContainer(containerName);
            workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP,
                zipInputStreamSipObject);
            // call processing
            RestAssured.port = PORT_SERVICE_PROCESSING;
            RestAssured.basePath = PROCESSING_PATH;

            processingClient = ProcessingManagementClientFactory.getInstance().getClient();
            processingClient.initVitamProcess(Contexts.DEFAULT_WORKFLOW.name(), containerName, INGEST_PLAN_WORFKLOW);
            final Response ret =
                processingClient.updateOperationActionProcess(ProcessAction.RESUME.getValue(), containerName);
            assertNotNull(ret);

            assertEquals(Status.ACCEPTED.getStatusCode(), ret.getStatus());

            wait(containerName);
            ProcessWorkflow processWorkflow =
                processMonitoring.findOneProcessWorkflow(containerName, tenantId);
            assertNotNull(processWorkflow);
            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
            assertEquals(StatusCode.OK, processWorkflow.getStatus());
        } catch (final Exception e) {
            e.printStackTrace();
            fail("should not raized an exception");
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowWithSIPContainsSystemId() throws Exception {
        try {
            VitamThreadUtils.getVitamSession().setTenantId(tenantId);
            tryImportFile();
            final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            final GUID objectGuid = GUIDFactory.newManifestGUID(tenantId);
            final String containerName = objectGuid.getId();
            createLogbookOperation(operationGuid, objectGuid);

            // workspace client dezip SIP in workspace
            RestAssured.port = PORT_SERVICE_WORKSPACE;
            RestAssured.basePath = WORKSPACE_PATH;
            final InputStream zipInputStreamSipObject =
                PropertiesUtils.getResourceAsStream(SIP_FILE_OK_WITH_SYSTEMID);
            workspaceClient = WorkspaceClientFactory.getInstance().getClient();
            workspaceClient.createContainer(containerName);
            workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP,
                zipInputStreamSipObject);
            // call processing
            RestAssured.port = PORT_SERVICE_PROCESSING;
            RestAssured.basePath = PROCESSING_PATH;

            processingClient = ProcessingManagementClientFactory.getInstance().getClient();
            processingClient.initVitamProcess(Contexts.DEFAULT_WORKFLOW.name(), containerName, WORFKLOW_NAME);
            final RequestResponse<JsonNode> ret =
                processingClient.executeOperationProcess(containerName, WORFKLOW_NAME,
                    Contexts.DEFAULT_WORKFLOW.name(), ProcessAction.RESUME.getValue());
            assertNotNull(ret);
            assertEquals(Status.ACCEPTED.getStatusCode(), ret.getStatus());

            wait(containerName);
            ProcessWorkflow processWorkflow =
                processMonitoring.findOneProcessWorkflow(containerName, tenantId);
            assertNotNull(processWorkflow);
            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
            assertEquals(StatusCode.WARNING, processWorkflow.getStatus());
        } catch (final Exception e) {
            e.printStackTrace();
            fail("should not raized an exception");
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowWithTarSIP() throws Exception {
        try {
            VitamThreadUtils.getVitamSession().setTenantId(tenantId);
            tryImportFile();
            final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            final GUID objectGuid = GUIDFactory.newManifestGUID(tenantId);
            final String containerName = objectGuid.getId();
            createLogbookOperation(operationGuid, objectGuid);
            // workspace client dezip SIP in workspace
            RestAssured.port = PORT_SERVICE_WORKSPACE;
            RestAssured.basePath = WORKSPACE_PATH;

            final InputStream zipInputStreamSipObject =
                Thread.currentThread().getContextClassLoader().getResourceAsStream(SIP_FILE_TAR_OK_NAME);
            workspaceClient = WorkspaceClientFactory.getInstance().getClient();
            workspaceClient.createContainer(containerName);
            workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.TAR,
                zipInputStreamSipObject);

            // call processing
            RestAssured.port = PORT_SERVICE_PROCESSING;
            RestAssured.basePath = PROCESSING_PATH;

            processingClient = ProcessingManagementClientFactory.getInstance().getClient();
            processingClient.initVitamProcess(Contexts.DEFAULT_WORKFLOW.name(), containerName, WORFKLOW_NAME);
            final RequestResponse<JsonNode> ret =
                processingClient.executeOperationProcess(containerName, WORFKLOW_NAME,
                    Contexts.DEFAULT_WORKFLOW.name(), ProcessAction.RESUME.getValue());

            assertNotNull(ret);
            assertEquals(Status.ACCEPTED.getStatusCode(), ret.getStatus());

            wait(containerName);
            ProcessWorkflow processWorkflow =
                processMonitoring.findOneProcessWorkflow(containerName, tenantId);
            assertNotNull(processWorkflow);
            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
            assertEquals(StatusCode.WARNING, processWorkflow.getStatus());
        } catch (final Exception e) {
            e.printStackTrace();
            fail("should not raized an exception");
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflow_with_complexe_unit_seda() throws Exception {
        try {
            VitamThreadUtils.getVitamSession().setTenantId(tenantId);
            tryImportFile();
            final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            final GUID objectGuid = GUIDFactory.newManifestGUID(tenantId);
            final String containerName = objectGuid.getId();
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

            processingClient = ProcessingManagementClientFactory.getInstance().getClient();
            processingClient.initVitamProcess(Contexts.DEFAULT_WORKFLOW.name(), containerName, WORFKLOW_NAME);

            final RequestResponse<JsonNode> ret =
                processingClient.executeOperationProcess(containerName, WORFKLOW_NAME,
                    Contexts.DEFAULT_WORKFLOW.name(), ProcessAction.RESUME.getValue());

            assertNotNull(ret);
            assertEquals(Status.ACCEPTED.getStatusCode(), ret.getStatus());

            wait(containerName);
            ProcessWorkflow processWorkflow =
                processMonitoring.findOneProcessWorkflow(containerName, tenantId);
            assertNotNull(processWorkflow);
            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
            assertEquals(StatusCode.WARNING, processWorkflow.getStatus());
        } catch (final Exception e) {
            e.printStackTrace();
            fail("should not raized an exception");
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflow_with_herited_ruleCA1() throws Exception {
        try {
            VitamThreadUtils.getVitamSession().setTenantId(tenantId);
            tryImportFile();
            final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            final GUID objectGuid = GUIDFactory.newManifestGUID(tenantId);
            final String containerName = objectGuid.getId();
            createLogbookOperation(operationGuid, objectGuid);

            // call processing
            RestAssured.port = PORT_SERVICE_PROCESSING;
            RestAssured.basePath = PROCESSING_PATH;
            processingClient = ProcessingManagementClientFactory.getInstance().getClient();

            // workspace client dezip SIP in workspace
            RestAssured.port = PORT_SERVICE_WORKSPACE;
            RestAssured.basePath = WORKSPACE_PATH;


            final InputStream zipInputStreamSipObject =
                PropertiesUtils.getResourceAsStream(SIP_INHERITED_RULE_CA1_OK);
            workspaceClient = WorkspaceClientFactory.getInstance().getClient();
            workspaceClient.createContainer(containerName);
            workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP, zipInputStreamSipObject);

            processingClient.initVitamProcess(Contexts.DEFAULT_WORKFLOW.name(), containerName, WORFKLOW_NAME_2);
            final RequestResponse<JsonNode> ret =
                processingClient.executeOperationProcess(containerName, WORFKLOW_NAME_2,
                    Contexts.DEFAULT_WORKFLOW.name(),
                    ProcessAction.RESUME.getValue());
            assertNotNull(ret);

            assertEquals(Status.ACCEPTED.getStatusCode(), ret.getStatus());

            wait(containerName);
            ProcessWorkflow processWorkflow =
                processMonitoring.findOneProcessWorkflow(containerName, tenantId);
            assertNotNull(processWorkflow);
            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
            assertEquals(StatusCode.WARNING, processWorkflow.getStatus());

            MetaDataClient metaDataClient = MetaDataClientFactory.getInstance().getClient();
            SelectMultiQuery query = new SelectMultiQuery();
            query.addQueries(QueryHelper.eq("Title", "AU4").setRelativeDepthLimit(5));
            query.addProjection(JsonHandler.createObjectNode().set(PROJECTION.FIELDS.exactToken(),
                JsonHandler.createObjectNode()
                    .put(GLOBAL.RULES.exactToken(), 1).put("Title", 1)
                    .put(PROJECTIONARGS.MANAGEMENT.exactToken(), 1)));
            JsonNode result = metaDataClient.selectUnits(query.getFinalSelect());
            assertNotNull(
                result.get("$results").get(0).get(UnitInheritedRule.INHERITED_RULE).get("StorageRule").get("R1"));
        } catch (final Exception e) {
            e.printStackTrace();
            fail("should not raized an exception");
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflow_with_herited_ruleCA4() throws Exception {
        try {
            VitamThreadUtils.getVitamSession().setTenantId(tenantId);
            tryImportFile();
            final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            final GUID objectGuid = GUIDFactory.newManifestGUID(tenantId);
            final String containerName = objectGuid.getId();
            createLogbookOperation(operationGuid, objectGuid);

            // workspace client dezip SIP in workspace
            RestAssured.port = PORT_SERVICE_WORKSPACE;
            RestAssured.basePath = WORKSPACE_PATH;

            final InputStream zipInputStreamSipObject =
                PropertiesUtils.getResourceAsStream(SIP_INHERITED_RULE_CA4_OK);
            workspaceClient = WorkspaceClientFactory.getInstance().getClient();
            workspaceClient.createContainer(containerName);
            workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP, zipInputStreamSipObject);

            // call processing
            RestAssured.port = PORT_SERVICE_PROCESSING;
            RestAssured.basePath = PROCESSING_PATH;
            processingClient = ProcessingManagementClientFactory.getInstance().getClient();
            processingClient.initVitamProcess(Contexts.DEFAULT_WORKFLOW.name(), containerName, WORFKLOW_NAME);
            final RequestResponse<JsonNode> ret =
                processingClient.executeOperationProcess(containerName, WORFKLOW_NAME,
                    Contexts.DEFAULT_WORKFLOW.name(), ProcessAction.RESUME.getValue());
            assertNotNull(ret);
            assertEquals(Status.ACCEPTED.getStatusCode(), ret.getStatus());

            wait(containerName);
            ProcessWorkflow processWorkflow =
                processMonitoring.findOneProcessWorkflow(containerName, tenantId);
            assertNotNull(processWorkflow);
            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
            assertEquals(StatusCode.WARNING, processWorkflow.getStatus());

            MetaDataClient metaDataClient = MetaDataClientFactory.getInstance().getClient();
            SelectMultiQuery query = new SelectMultiQuery();
            query.addQueries(QueryHelper.eq("Title", "AU4").setRelativeDepthLimit(5));
            query.addProjection(JsonHandler.createObjectNode().set(PROJECTION.FIELDS.exactToken(),
                JsonHandler.createObjectNode()
                    .put(GLOBAL.RULES.exactToken(), 1).put("Title", 1)
                    .put(PROJECTIONARGS.MANAGEMENT.exactToken(), 1)));
            JsonNode result = metaDataClient.selectUnits(query.getFinalSelect());

            assertNotNull(
                result.get("$results").get(0).get(UnitInheritedRule.INHERITED_RULE).get("StorageRule").get("R1"));
        } catch (final Exception e) {
            e.printStackTrace();
            fail("should not raized an exception");
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflow_with_accession_register() throws Exception {
        try {
            VitamThreadUtils.getVitamSession().setTenantId(tenantId);
            tryImportFile();
            final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            final GUID objectGuid = GUIDFactory.newManifestGUID(tenantId);
            final String containerName = objectGuid.getId();
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

            processingClient = ProcessingManagementClientFactory.getInstance().getClient();
            processingClient.initVitamProcess(Contexts.DEFAULT_WORKFLOW.name(), containerName, WORFKLOW_NAME);

            final RequestResponse<JsonNode> ret =
                processingClient.executeOperationProcess(containerName, WORFKLOW_NAME,
                    Contexts.DEFAULT_WORKFLOW.name(), ProcessAction.RESUME.getValue());
            assertNotNull(ret);
            assertEquals(Status.ACCEPTED.getStatusCode(), ret.getStatus());

            wait(containerName);
            ProcessWorkflow processWorkflow =
                processMonitoring.findOneProcessWorkflow(containerName, tenantId);
            assertNotNull(processWorkflow);
            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
            assertEquals(StatusCode.WARNING, processWorkflow.getStatus());
        } catch (final Exception e) {
            e.printStackTrace();
            fail("should not raized an exception");
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowWithSipNoManifest() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        tryImportFile();
        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        final GUID objectGuid = GUIDFactory.newManifestGUID(tenantId);
        final String containerName = objectGuid.getId();
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
        processingClient = ProcessingManagementClientFactory.getInstance().getClient();
        processingClient.initVitamProcess(Contexts.DEFAULT_WORKFLOW.name(), containerName, WORFKLOW_NAME);
        final RequestResponse<JsonNode> ret =
            processingClient.executeOperationProcess(containerName, WORFKLOW_NAME,
                Contexts.DEFAULT_WORKFLOW.name(), ProcessAction.RESUME.getValue());
        assertNotNull(ret);
        assertEquals(Status.ACCEPTED.getStatusCode(), ret.getStatus());

        wait(containerName);
        ProcessWorkflow processWorkflow =
            processMonitoring.findOneProcessWorkflow(containerName, tenantId);
        assertNotNull(processWorkflow);
        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
        assertEquals(StatusCode.KO, processWorkflow.getStatus());
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowSipNoFormat() throws Exception {
        try {
            VitamThreadUtils.getVitamSession().setTenantId(tenantId);
            tryImportFile();
            final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            final GUID objectGuid = GUIDFactory.newManifestGUID(tenantId);
            final String containerName = objectGuid.getId();
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

            processingClient = ProcessingManagementClientFactory.getInstance().getClient();
            processingClient.initVitamProcess(Contexts.DEFAULT_WORKFLOW.name(), containerName, WORFKLOW_NAME);
            final RequestResponse<JsonNode> ret =
                processingClient.executeOperationProcess(containerName, WORFKLOW_NAME,
                    Contexts.DEFAULT_WORKFLOW.name(), ProcessAction.RESUME.getValue());
            assertNotNull(ret);
            assertEquals(Status.ACCEPTED.getStatusCode(), ret.getStatus());

            wait(containerName);
            ProcessWorkflow processWorkflow =
                processMonitoring.findOneProcessWorkflow(containerName, tenantId);
            assertNotNull(processWorkflow);
            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
            assertEquals(StatusCode.WARNING, processWorkflow.getStatus());
        } catch (final Exception e) {
            e.printStackTrace();
            fail("should not raized an exception");
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowSipDoubleVersionBM() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        tryImportFile();
        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        final GUID objectGuid = GUIDFactory.newManifestGUID(tenantId);
        final String containerName = objectGuid.getId();
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
        processingClient = ProcessingManagementClientFactory.getInstance().getClient();
        processingClient.initVitamProcess(Contexts.DEFAULT_WORKFLOW.name(), containerName, WORFKLOW_NAME);
        final RequestResponse<JsonNode> ret =
            processingClient.executeOperationProcess(containerName, WORFKLOW_NAME,
                Contexts.DEFAULT_WORKFLOW.name(), ProcessAction.RESUME.getValue());
        assertNotNull(ret);
        assertEquals(Status.ACCEPTED.getStatusCode(), ret.getStatus());

        wait(containerName);
        ProcessWorkflow processWorkflow =
            processMonitoring.findOneProcessWorkflow(containerName, tenantId);
        assertNotNull(processWorkflow);
        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
        assertEquals(StatusCode.KO, processWorkflow.getStatus());
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowSipNoFormatNoTag() throws Exception {

        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        tryImportFile();
        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        final GUID objectGuid = GUIDFactory.newManifestGUID(tenantId);
        final String containerName = objectGuid.getId();
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

        processingClient = ProcessingManagementClientFactory.getInstance().getClient();
        processingClient.initVitamProcess(Contexts.DEFAULT_WORKFLOW.name(), containerName, WORFKLOW_NAME);
        final RequestResponse<JsonNode> ret =
            processingClient.executeOperationProcess(containerName, WORFKLOW_NAME,
                Contexts.DEFAULT_WORKFLOW.name(), ProcessAction.RESUME.getValue());
        assertNotNull(ret);
        assertEquals(Status.ACCEPTED.getStatusCode(), ret.getStatus());

        wait(containerName);
        ProcessWorkflow processWorkflow =
            processMonitoring.findOneProcessWorkflow(containerName, tenantId);
        assertNotNull(processWorkflow);
        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
        assertEquals(StatusCode.KO, processWorkflow.getStatus());
    }


    @RunWithCustomExecutor
    @Test
    public void testWorkflowWithManifestIncorrectObjectNumber() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        tryImportFile();
        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        final GUID objectGuid = GUIDFactory.newManifestGUID(tenantId);
        final String containerName = objectGuid.getId();
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
        ///////
        processingClient = ProcessingManagementClientFactory.getInstance().getClient();
        processingClient.initVitamProcess(Contexts.DEFAULT_WORKFLOW.name(), containerName, WORFKLOW_NAME);

        final RequestResponse<JsonNode> ret =
            processingClient.executeOperationProcess(containerName, WORFKLOW_NAME,
                Contexts.DEFAULT_WORKFLOW.name(), ProcessAction.RESUME.getValue());
        assertNotNull(ret);

        assertEquals(Status.ACCEPTED.getStatusCode(), ret.getStatus());

        wait(containerName);
        ProcessWorkflow processWorkflow =
            processMonitoring.findOneProcessWorkflow(containerName, tenantId);
        assertNotNull(processWorkflow);
        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
        assertEquals(StatusCode.KO, processWorkflow.getStatus());
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowWithOrphelins() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        tryImportFile();
        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        final GUID objectGuid = GUIDFactory.newManifestGUID(tenantId);
        final String containerName = objectGuid.getId();
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
        processingClient = ProcessingManagementClientFactory.getInstance().getClient();
        processingClient.initVitamProcess(Contexts.DEFAULT_WORKFLOW.name(), containerName, WORFKLOW_NAME);

        final RequestResponse<JsonNode> ret =
            processingClient.executeOperationProcess(containerName, WORFKLOW_NAME,
                Contexts.DEFAULT_WORKFLOW.name(), ProcessAction.RESUME.getValue());
        assertNotNull(ret);
        assertEquals(Status.ACCEPTED.getStatusCode(), ret.getStatus());

        wait(containerName);
        ProcessWorkflow processWorkflow =
            processMonitoring.findOneProcessWorkflow(containerName, tenantId);
        assertNotNull(processWorkflow);
        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
        assertEquals(StatusCode.KO, processWorkflow.getStatus());
    }


    @RunWithCustomExecutor
    @Test
    public void testWorkflowWithoutObjectGroups() throws Exception {
        try {
            VitamThreadUtils.getVitamSession().setTenantId(tenantId);
            tryImportFile();
            final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            final GUID objectGuid = GUIDFactory.newManifestGUID(tenantId);
            final String containerName = objectGuid.getId();
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

            processingClient = ProcessingManagementClientFactory.getInstance().getClient();
            processingClient.initVitamProcess(Contexts.DEFAULT_WORKFLOW.name(), containerName, WORFKLOW_NAME);

            final RequestResponse<JsonNode> ret =
                processingClient.executeOperationProcess(containerName, WORFKLOW_NAME,
                    Contexts.DEFAULT_WORKFLOW.name(), ProcessAction.RESUME.getValue());
            assertNotNull(ret);
            assertEquals(Status.ACCEPTED.getStatusCode(), ret.getStatus());

            wait(containerName);
            ProcessWorkflow processWorkflow =
                processMonitoring.findOneProcessWorkflow(containerName, tenantId);
            assertNotNull(processWorkflow);
            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
            assertEquals(StatusCode.WARNING, processWorkflow.getStatus());
        } catch (final Exception e) {
            e.printStackTrace();
            fail("should not raized an exception");
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowWithSipWithoutObject() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        tryImportFile();
        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        final GUID objectGuid = GUIDFactory.newManifestGUID(tenantId);
        final String containerName = objectGuid.getId();
        createLogbookOperation(operationGuid, objectGuid);

        // workspace client dezip SIP in workspace
        RestAssured.port = PORT_SERVICE_WORKSPACE;
        RestAssured.basePath = WORKSPACE_PATH;

        final InputStream zipInputStreamSipObject =
            PropertiesUtils.getResourceAsStream(SIP_WITHOUT_OBJ);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP, zipInputStreamSipObject);

        // call processing
        RestAssured.port = PORT_SERVICE_PROCESSING;
        RestAssured.basePath = PROCESSING_PATH;
        processingClient = ProcessingManagementClientFactory.getInstance().getClient();
        processingClient.initVitamProcess(Contexts.DEFAULT_WORKFLOW.name(), containerName, WORFKLOW_NAME);

        final RequestResponse<JsonNode> ret =
            processingClient.executeOperationProcess(containerName, WORFKLOW_NAME, Contexts.DEFAULT_WORKFLOW.name(),
                ProcessAction.RESUME.getValue());
        assertNotNull(ret);

        assertEquals(Status.ACCEPTED.getStatusCode(), ret.getStatus());

        wait(containerName);

        ProcessWorkflow processWorkflow =
            processMonitoring.findOneProcessWorkflow(containerName, tenantId);

        // check conformity in warning state
        // File format warning state
        assertEquals(StatusCode.WARNING, processWorkflow.getStatus());
        // completed execution status
        assertEquals(ProcessState.COMPLETED,processWorkflow.getState());
        // checkMonitoring - meaning something has been added in the monitoring tool

    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowKOwithATRKOFilled() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        tryImportFile();
        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        final GUID objectGuid = GUIDFactory.newManifestGUID(tenantId);
        final String containerName = objectGuid.getId();
        createLogbookOperation(operationGuid, objectGuid);

        // workspace client dezip SIP in workspace
        RestAssured.port = PORT_SERVICE_WORKSPACE;
        RestAssured.basePath = WORKSPACE_PATH;

        final InputStream zipInputStreamSipObject =
            PropertiesUtils.getResourceAsStream(SIP_WITHOUT_FUND_REGISTER);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP, zipInputStreamSipObject);

        // call processing
        RestAssured.port = PORT_SERVICE_PROCESSING;
        RestAssured.basePath = PROCESSING_PATH;
        processingClient = ProcessingManagementClientFactory.getInstance().getClient();
        processingClient.initVitamProcess(Contexts.DEFAULT_WORKFLOW.name(), containerName, WORFKLOW_NAME);
        final RequestResponse<JsonNode> ret =
            processingClient.executeOperationProcess(containerName, WORFKLOW_NAME,
                Contexts.DEFAULT_WORKFLOW.name(), ProcessAction.RESUME.getValue());
        assertNotNull(ret);

        assertEquals(Status.ACCEPTED.getStatusCode(), ret.getStatus());

        wait(containerName);
        ProcessWorkflow processWorkflow =
            processMonitoring.findOneProcessWorkflow(containerName, tenantId);
        assertNotNull(processWorkflow);
        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
        assertEquals(StatusCode.KO, processWorkflow.getStatus());
    }

    @RunWithCustomExecutor
    // as now errors with xml are handled in ExtractSeda (not a FATAL but a KO
    // it s no longer an exception that is obtained
    @Test
    public void testWorkflowSipCausesFatalThenProcessingInternalServerException() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        tryImportFile();
        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        final GUID objectGuid = GUIDFactory.newManifestGUID(tenantId);
        final String containerName = objectGuid.getId();
        createLogbookOperation(operationGuid, objectGuid);

        // workspace client dezip SIP in workspace
        RestAssured.port = PORT_SERVICE_WORKSPACE;
        RestAssured.basePath = WORKSPACE_PATH;

        final InputStream zipInputStreamSipObject =
            PropertiesUtils.getResourceAsStream(SIP_BORD_AU_REF_PHYS_OBJECT);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP, zipInputStreamSipObject);

        // call processing
        RestAssured.port = PORT_SERVICE_PROCESSING;
        RestAssured.basePath = PROCESSING_PATH;
        processingClient = ProcessingManagementClientFactory.getInstance().getClient();
        processingClient.initVitamProcess(Contexts.DEFAULT_WORKFLOW.name(), containerName, WORFKLOW_NAME);
        final RequestResponse<JsonNode> ret =
            processingClient.executeOperationProcess(containerName, WORFKLOW_NAME,
                Contexts.DEFAULT_WORKFLOW.name(), ProcessAction.RESUME.getValue());
        assertNotNull(ret);
        assertEquals(Status.ACCEPTED.getStatusCode(), ret.getStatus());

        wait(containerName);
        ProcessWorkflow processWorkflow =
            processMonitoring.findOneProcessWorkflow(containerName, tenantId);
        assertNotNull(processWorkflow);
        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
        assertEquals(StatusCode.KO, processWorkflow.getStatus());
    }

    // Status Warn
    @RunWithCustomExecutor
    @Test
    public void testWorkflowAddAndLinkSIP() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        tryImportFile();
        // 1. First we create an AU by sip
        try {
            final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            final GUID objectGuid = GUIDFactory.newManifestGUID(tenantId);
            final String containerName = objectGuid.getId();
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

            processingClient = ProcessingManagementClientFactory.getInstance().getClient();
            processingClient.initVitamProcess(Contexts.DEFAULT_WORKFLOW.name(), containerName, WORFKLOW_NAME);
            final RequestResponse<JsonNode> ret =
                processingClient.executeOperationProcess(containerName, WORFKLOW_NAME,
                    Contexts.DEFAULT_WORKFLOW.name(), ProcessAction.RESUME.getValue());

            assertNotNull(ret);
            assertEquals(Status.ACCEPTED.getStatusCode(), ret.getStatus());

            wait(containerName);
            ProcessWorkflow processWorkflow =
                processMonitoring.findOneProcessWorkflow(containerName, tenantId);
            assertNotNull(processWorkflow);
            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
            assertEquals(StatusCode.WARNING, processWorkflow.getStatus());
        } catch (final Exception e) {
            e.printStackTrace();
            fail("should not raized an exception");
        }

        String zipPath = null;
        // 2. then we link another SIP to it
        String zipName = ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE - 1) + ".zip";

        // prepare zip
        final MongoDatabase db = mongoClient.getDatabase("Vitam");
        MongoIterable<Document> resultUnits = db.getCollection("Unit").find();
        Document unit = resultUnits.first();
        String idUnit = (String) unit.get("_id");
        replaceStringInFile(SIP_FILE_ADD_AU_LINK_OK_NAME + "/manifest.xml", "(?<=<SystemId>).*?(?=</SystemId>)",
            idUnit);
        zipPath = PropertiesUtils.getResourcePath(SIP_FILE_ADD_AU_LINK_OK_NAME_TARGET).toAbsolutePath().toString() + "/" + zipName;
        zipFolder(PropertiesUtils.getResourcePath(SIP_FILE_ADD_AU_LINK_OK_NAME), zipPath);


        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        final GUID objectGuid = GUIDFactory.newManifestGUID(tenantId);
        final String containerName = objectGuid.getId();
        createLogbookOperation(operationGuid, objectGuid);

        // workspace client dezip SIP in workspace
        RestAssured.port = PORT_SERVICE_WORKSPACE;
        RestAssured.basePath = WORKSPACE_PATH;
        // use link sip
        final InputStream zipStream = new FileInputStream(new File(
            PropertiesUtils.getResourcePath(SIP_FILE_ADD_AU_LINK_OK_NAME_TARGET).toAbsolutePath() +
                "/" + zipName));

        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP,
            zipStream);

        // call processing
        RestAssured.port = PORT_SERVICE_PROCESSING;
        RestAssured.basePath = PROCESSING_PATH;

        processingClient = ProcessingManagementClientFactory.getInstance().getClient();
        processingClient.initVitamProcess(Contexts.DEFAULT_WORKFLOW.name(), containerName, WORFKLOW_NAME);
        final RequestResponse<JsonNode> ret =
            processingClient.executeOperationProcess(containerName, WORFKLOW_NAME,
                Contexts.DEFAULT_WORKFLOW.name(), ProcessAction.RESUME.getValue());
        assertNotNull(ret);
        assertEquals(Status.ACCEPTED.getStatusCode(), ret.getStatus());

        wait(containerName);
        ProcessWorkflow processWorkflow = processMonitoring.findOneProcessWorkflow(containerName, tenantId);
        assertNotNull(processWorkflow);
        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
        assertEquals(StatusCode.WARNING, processWorkflow.getStatus());
        assertNotNull(processWorkflow.getSteps());

        // check results
        MongoIterable<Document> modifiedParentUnit = db.getCollection("Unit").find(Filters.eq("_id", idUnit));
        assertNotNull(modifiedParentUnit);
        assertNotNull(modifiedParentUnit.first());
        Document parentUnit = modifiedParentUnit.first();

        MongoIterable<Document> newChildUnit = db.getCollection("Unit").find(Filters.eq("_up", idUnit));
        assertNotNull(newChildUnit);
        assertNotNull(newChildUnit.first());
        try {
            Files.delete(new File(zipPath).toPath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowAddAndLinkSIPKo() throws Exception {

        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        tryImportFile();
        // We link to a non existing unit
        String zipPath = null;
        String zipName = ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE - 1) + ".zip";

        // prepare zip
        replaceStringInFile(SIP_FILE_ADD_AU_LINK_OK_NAME + "/manifest.xml", "(?<=<SystemId>).*?(?=</SystemId>)",
            "aeaaaaaaaaaam7mxabxccakzrw47heqaaaaq");

        zipPath = PropertiesUtils.getResourcePath(SIP_FILE_ADD_AU_LINK_OK_NAME_TARGET).toAbsolutePath().toString() + "/" + zipName;
        zipFolder(PropertiesUtils.getResourcePath(SIP_FILE_ADD_AU_LINK_OK_NAME), zipPath);


        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        final GUID objectGuid = GUIDFactory.newManifestGUID(tenantId);
        final String containerName = objectGuid.getId();
        createLogbookOperation(operationGuid, objectGuid);

        // workspace client dezip SIP in workspace
        RestAssured.port = PORT_SERVICE_WORKSPACE;
        RestAssured.basePath = WORKSPACE_PATH;
        // use link sip
        final InputStream zipStream = new FileInputStream(new File(
            PropertiesUtils.getResourcePath(SIP_FILE_ADD_AU_LINK_OK_NAME_TARGET).toAbsolutePath() +
                "/" + zipName));

        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP,
            zipStream);

        // call processing
        RestAssured.port = PORT_SERVICE_PROCESSING;
        RestAssured.basePath = PROCESSING_PATH;
        ///////
        processingClient = ProcessingManagementClientFactory.getInstance().getClient();
        processingClient.initVitamProcess(Contexts.DEFAULT_WORKFLOW.name(), containerName, WORFKLOW_NAME);
        final RequestResponse<JsonNode> ret =
            processingClient.executeOperationProcess(containerName, WORFKLOW_NAME,
                Contexts.DEFAULT_WORKFLOW.name(), ProcessAction.RESUME.getValue());
        assertNotNull(ret);
        assertEquals(Status.ACCEPTED.getStatusCode(), ret.getStatus());

        wait(containerName);
        ProcessWorkflow processWorkflow =
            processMonitoring.findOneProcessWorkflow(containerName, tenantId);
        assertNotNull(processWorkflow);
        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
        assertEquals(StatusCode.KO, processWorkflow.getStatus());
        assertNotNull(processWorkflow.getSteps());
        try {
            Files.delete(new File(zipPath).toPath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Test attach existing ObjectGroup to unit
     * 1. Upload SIP
     * 2. Get created GOT
     * 3. Update manifest and set existing GOT
     * 4. Upload the new SIP
     * @throws Exception
     */
    @RunWithCustomExecutor
    @Test
    public void testLinkUnitToExistingGOTOK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        tryImportFile();
        // 1. First we create an AU by sip
        try {
            final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            final GUID objectGuid = GUIDFactory.newManifestGUID(tenantId);
            final String containerName = objectGuid.getId();
            createLogbookOperation(operationGuid, objectGuid);

            // workspace client dezip SIP in workspace
            RestAssured.port = PORT_SERVICE_WORKSPACE;
            RestAssured.basePath = WORKSPACE_PATH;
            final InputStream zipInputStreamSipObject =
                PropertiesUtils.getResourceAsStream(SIP_FILE_OK_NAME);
            workspaceClient = WorkspaceClientFactory.getInstance().getClient();
            workspaceClient.createContainer(containerName);
            workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP, zipInputStreamSipObject);
            // call processing
            RestAssured.port = PORT_SERVICE_PROCESSING;
            RestAssured.basePath = PROCESSING_PATH;

            processingClient = ProcessingManagementClientFactory.getInstance().getClient();
            processingClient.initVitamProcess(Contexts.DEFAULT_WORKFLOW.name(), containerName, WORFKLOW_NAME);
            final RequestResponse<JsonNode> ret =
                processingClient.executeOperationProcess(containerName, WORFKLOW_NAME,
                    Contexts.DEFAULT_WORKFLOW.name(), ProcessAction.RESUME.getValue());
            assertNotNull(ret);
            assertEquals(Status.ACCEPTED.getStatusCode(), ret.getStatus());

            wait(containerName);
            ProcessWorkflow processWorkflow =
                processMonitoring.findOneProcessWorkflow(containerName, tenantId);
            assertNotNull(processWorkflow);
            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
            assertEquals(StatusCode.WARNING, processWorkflow.getStatus());
        } catch (final Exception e) {
            e.printStackTrace();
            fail("should not raized an exception");
        }

        // 2. then we link another SIP to it
        String zipPath = null;
        String zipName = ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE - 1) + ".zip";

        // prepare zip
        final MongoDatabase db = mongoClient.getDatabase("Vitam");
        MongoIterable<Document> resultUnits = db.getCollection("Unit").find();
        Document unit = resultUnits.first();
        String idGot = (String) unit.get("_og");
        replaceStringInFile(LINK_AU_TO_EXISTING_GOT_OK_NAME + "/manifest.xml", "(?<=<DataObjectGroupExistingReferenceId>).*?(?=</DataObjectGroupExistingReferenceId>)",
            idGot);

        zipPath = PropertiesUtils.getResourcePath(LINK_AU_TO_EXISTING_GOT_OK_NAME_TARGET).toAbsolutePath().toString() + "/" + zipName;
        zipFolder(PropertiesUtils.getResourcePath(LINK_AU_TO_EXISTING_GOT_OK_NAME), zipPath);


        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        final GUID objectGuid = GUIDFactory.newManifestGUID(tenantId);
        final String containerName = objectGuid.getId();
        createLogbookOperation(operationGuid, objectGuid);

        // workspace client dezip SIP in workspace
        RestAssured.port = PORT_SERVICE_WORKSPACE;
        RestAssured.basePath = WORKSPACE_PATH;
        // use link sip
        final InputStream zipStream = new FileInputStream(new File(
            PropertiesUtils.getResourcePath(LINK_AU_TO_EXISTING_GOT_OK_NAME_TARGET).toAbsolutePath() +
                "/" + zipName));

        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP,
            zipStream);

        // call processing
        RestAssured.port = PORT_SERVICE_PROCESSING;
        RestAssured.basePath = PROCESSING_PATH;

        processingClient = ProcessingManagementClientFactory.getInstance().getClient();
        processingClient.initVitamProcess(Contexts.DEFAULT_WORKFLOW.name(), containerName, WORFKLOW_NAME);
        final RequestResponse<JsonNode> ret =
            processingClient.executeOperationProcess(containerName, WORFKLOW_NAME,
                Contexts.DEFAULT_WORKFLOW.name(), ProcessAction.RESUME.getValue());
        assertNotNull(ret);
        assertEquals(Status.ACCEPTED.getStatusCode(), ret.getStatus());

        wait(containerName);
        ProcessWorkflow processWorkflow = processMonitoring.findOneProcessWorkflow(containerName, tenantId);
        assertNotNull(processWorkflow);
        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
        assertEquals(StatusCode.WARNING, processWorkflow.getStatus());
        assertNotNull(processWorkflow.getSteps());

        // check got have to units
        assertEquals(db.getCollection("Unit").count(Filters.eq("_og", idGot)), 2);
        try {
            Files.delete(new File(zipPath).toPath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Test attach existing ObjectGroup to unit, but guid of the existing got is fake and really exists
     * @throws Exception
     */
    @RunWithCustomExecutor
    @Test
    public void testLinkUnitToExistingGOTFakeGuidKO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        tryImportFile();
        String zipName = ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE - 1) + ".zip";

        // prepare zip
        String idGot = "FakeGuid";
        replaceStringInFile(LINK_AU_TO_EXISTING_GOT_OK_NAME + "/manifest.xml", "(?<=<DataObjectGroupExistingReferenceId>).*?(?=</DataObjectGroupExistingReferenceId>)",
            idGot);
        zipFolder(PropertiesUtils.getResourcePath(LINK_AU_TO_EXISTING_GOT_OK_NAME),
            PropertiesUtils.getResourcePath(LINK_AU_TO_EXISTING_GOT_OK_NAME_TARGET).toAbsolutePath().toString() +
                "/" + zipName);


        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        final GUID objectGuid = GUIDFactory.newManifestGUID(tenantId);
        final String containerName = objectGuid.getId();
        createLogbookOperation(operationGuid, objectGuid);

        // workspace client dezip SIP in workspace
        RestAssured.port = PORT_SERVICE_WORKSPACE;
        RestAssured.basePath = WORKSPACE_PATH;
        // use link sip
        final InputStream zipStream = new FileInputStream(new File(
            PropertiesUtils.getResourcePath(LINK_AU_TO_EXISTING_GOT_OK_NAME_TARGET).toAbsolutePath() +
                "/" + zipName));

        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP,
            zipStream);

        // call processing
        RestAssured.port = PORT_SERVICE_PROCESSING;
        RestAssured.basePath = PROCESSING_PATH;

        processingClient = ProcessingManagementClientFactory.getInstance().getClient();
        processingClient.initVitamProcess(Contexts.DEFAULT_WORKFLOW.name(), containerName, WORFKLOW_NAME);
        final RequestResponse<JsonNode> ret =
            processingClient.executeOperationProcess(containerName, WORFKLOW_NAME,
                Contexts.DEFAULT_WORKFLOW.name(), ProcessAction.RESUME.getValue());
        assertNotNull(ret);
        assertEquals(Status.ACCEPTED.getStatusCode(), ret.getStatus());

        wait(containerName);
        ProcessWorkflow processWorkflow = processMonitoring.findOneProcessWorkflow(containerName, tenantId);
        assertNotNull(processWorkflow);
        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
        assertEquals(StatusCode.KO, processWorkflow.getStatus());
        assertNotNull(processWorkflow.getSteps());
        try {
            Files.delete(new File(
                PropertiesUtils.getResourcePath(LINK_AU_TO_EXISTING_GOT_OK_NAME_TARGET).toAbsolutePath().toString() +
                    "/" + zipName).toPath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void replaceStringInFile(String targetFilename, String textToReplace, String replacementText)
        throws IOException {
        Path path = PropertiesUtils.getResourcePath(targetFilename);
        Charset charset = StandardCharsets.UTF_8;

        String content = new String(Files.readAllBytes(path), charset);
        content = content.replaceAll(textToReplace, replacementText);
        Files.write(path, content.getBytes(charset));
    }


    private void zipFolder(final Path path, final String zipFilePath) throws IOException {
        try (
            FileOutputStream fos = new FileOutputStream(zipFilePath);
            ZipOutputStream zos = new ZipOutputStream(fos)) {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    zos.putNextEntry(new ZipEntry(path.relativize(file).toString()));
                    Files.copy(file, zos);
                    zos.closeEntry();
                    return FileVisitResult.CONTINUE;
                }

                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    zos.putNextEntry(new ZipEntry(path.relativize(dir).toString() + "/"));
                    zos.closeEntry();
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    public void createLogbookOperation(GUID operationId, GUID objectId)
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException,
        LogbookClientNotFoundException {

        final LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient();

        final LogbookOperationParameters initParameters = LogbookParametersFactory.newLogbookOperationParameters(
            operationId, "Process_SIP_unitary", objectId,
            LogbookTypeProcess.INGEST, StatusCode.STARTED,
            operationId != null ? operationId.toString() : "outcomeDetailMessage",
            operationId);
        logbookClient.create(initParameters);
    }


    @RunWithCustomExecutor
    @Test
    public void testBigWorkflow() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        tryImportFile();
        // re-launch worker
        wkrapplication.stop();
        // FIXME Sleep to be removed when asynchronous mode is implemented
        Thread.sleep(8500);
        SystemPropertyUtil.set("jetty.worker.port", Integer.toString(PORT_SERVICE_WORKER));
        wkrapplication = new WorkerApplication(CONFIG_BIG_WORKER_PATH);
        wkrapplication.start();
        try {
            final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            final GUID objectGuid = GUIDFactory.newManifestGUID(tenantId);
            final String containerName = objectGuid.getId();
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
            processingClient = ProcessingManagementClientFactory.getInstance().getClient();
            processingClient.initVitamProcess(Contexts.DEFAULT_WORKFLOW.name(), containerName, WORFKLOW_NAME);

            final RequestResponse<JsonNode> ret = processingClient.executeOperationProcess(containerName, WORFKLOW_NAME,
                Contexts.DEFAULT_WORKFLOW.name(), ProcessAction.RESUME.getValue());
            assertNotNull(ret);
            assertEquals(Status.ACCEPTED.getStatusCode(), ret.getStatus());

            wait(containerName);
            ProcessWorkflow processWorkflow =
                processMonitoring.findOneProcessWorkflow(containerName, tenantId);
            assertNotNull(processWorkflow);
            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
            assertEquals(StatusCode.WARNING, processWorkflow.getStatus());
        } catch (final Exception e) {
            e.printStackTrace();
            fail("should not raized an exception");
        }

        wkrapplication.stop();
        SystemPropertyUtil.set("jetty.worker.port", Integer.toString(PORT_SERVICE_WORKER));
        wkrapplication = new WorkerApplication(CONFIG_WORKER_PATH);
        wkrapplication.start();
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowIncorrectManifestReference() throws Exception {

        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        tryImportFile();
        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        final GUID objectGuid = GUIDFactory.newManifestGUID(tenantId);
        final String containerName = objectGuid.getId();
        createLogbookOperation(operationGuid, objectGuid);

        // workspace client dezip SIP in workspace
        RestAssured.port = PORT_SERVICE_WORKSPACE;
        RestAssured.basePath = WORKSPACE_PATH;

        final InputStream zipInputStreamSipObject =
            PropertiesUtils.getResourceAsStream(SIP_MANIFEST_INCORRECT_REFERENCE);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP, zipInputStreamSipObject);

        // call processing
        RestAssured.port = PORT_SERVICE_PROCESSING;
        RestAssured.basePath = PROCESSING_PATH;

        processingClient = ProcessingManagementClientFactory.getInstance().getClient();
        processingClient.initVitamProcess(Contexts.DEFAULT_WORKFLOW.name(), containerName, WORFKLOW_NAME);
        final RequestResponse<JsonNode> ret =
            processingClient.executeOperationProcess(containerName, WORFKLOW_NAME,
                Contexts.DEFAULT_WORKFLOW.name(), ProcessAction.RESUME.getValue());
        assertNotNull(ret);
        assertEquals(Status.ACCEPTED.getStatusCode(), ret.getStatus());

        wait(containerName);
        ProcessWorkflow processWorkflow =
            processMonitoring.findOneProcessWorkflow(containerName, tenantId);
        assertNotNull(processWorkflow);
        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
        assertEquals(StatusCode.KO, processWorkflow.getStatus());
        assertNotNull(processWorkflow.getSteps());
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkerUnregister() throws Exception {
        try {
            VitamThreadUtils.getVitamSession().setTenantId(tenantId);

            wkrapplication.stop();
            SystemPropertyUtil.set("jetty.worker.port", Integer.toString(PORT_SERVICE_WORKER));
            wkrapplication = new WorkerApplication(CONFIG_WORKER_PATH);
            wkrapplication.start();
            processManagementApplication.stop();
            SystemPropertyUtil.set(ProcessManagementApplication.PARAMETER_JETTY_SERVER_PORT,
                Integer.toString(PORT_SERVICE_PROCESSING));
            processManagementApplication = new ProcessManagementApplication(CONFIG_PROCESSING_PATH);
            processManagementApplication.start();
            SystemPropertyUtil.clear(ProcessManagementApplication.PARAMETER_JETTY_SERVER_PORT);
            // Wait processing server start
            Thread.sleep(10000);
        } catch (final Exception e) {
            e.printStackTrace();
            fail("should not raized an exception");
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowBug2182() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        tryImportFile();
        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        final GUID objectGuid = GUIDFactory.newManifestGUID(tenantId);
        final String containerName = objectGuid.getId();
        createLogbookOperation(operationGuid, objectGuid);

        // workspace client dezip SIP in workspace
        RestAssured.port = PORT_SERVICE_WORKSPACE;
        RestAssured.basePath = WORKSPACE_PATH;

        final InputStream zipInputStreamSipObject =
            PropertiesUtils.getResourceAsStream(SIP_BUG_2182);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP, zipInputStreamSipObject);

        // call processing
        RestAssured.port = PORT_SERVICE_PROCESSING;
        RestAssured.basePath = PROCESSING_PATH;
        processingClient = ProcessingManagementClientFactory.getInstance().getClient();
        processingClient.initVitamProcess(Contexts.DEFAULT_WORKFLOW.name(), containerName, WORFKLOW_NAME);
        final RequestResponse<JsonNode> ret =
            processingClient.executeOperationProcess(containerName, WORFKLOW_NAME,
                Contexts.DEFAULT_WORKFLOW.name(), ProcessAction.RESUME.getValue());
        assertNotNull(ret);
        assertEquals(Status.ACCEPTED.getStatusCode(), ret.getStatus());

        wait(containerName);
        ProcessWorkflow processWorkflow =
            processMonitoring.findOneProcessWorkflow(containerName, tenantId);
        assertNotNull(processWorkflow);
        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
        assertEquals(StatusCode.KO, processWorkflow.getStatus());
    }


    @RunWithCustomExecutor
    @Test
    public void testWorkflowWithSIP_KO_AU_ref_BDO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        tryImportFile();
        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        final GUID objectGuid = GUIDFactory.newManifestGUID(tenantId);
        final String containerName = objectGuid.getId();
        createLogbookOperation(operationGuid, objectGuid);

        // workspace client dezip SIP in workspace
        RestAssured.port = PORT_SERVICE_WORKSPACE;
        RestAssured.basePath = WORKSPACE_PATH;

        final InputStream zipInputStreamSipObject =
            PropertiesUtils.getResourceAsStream(SIP_FILE_KO_AU_REF_BDO);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP, zipInputStreamSipObject);

        // call processing
        RestAssured.port = PORT_SERVICE_PROCESSING;
        RestAssured.basePath = PROCESSING_PATH;
        processingClient = ProcessingManagementClientFactory.getInstance().getClient();
        processingClient.initVitamProcess(Contexts.DEFAULT_WORKFLOW.name(), containerName, WORFKLOW_NAME);

        final RequestResponse<JsonNode> ret =
            processingClient.executeOperationProcess(containerName, WORFKLOW_NAME,
                Contexts.DEFAULT_WORKFLOW.name(), ProcessAction.RESUME.getValue());
        assertNotNull(ret);
        assertEquals(Status.ACCEPTED.getStatusCode(), ret.getStatus());

        wait(containerName);
        ProcessWorkflow processWorkflow =
            processMonitoring.findOneProcessWorkflow(containerName, tenantId);
        assertNotNull(processWorkflow);
        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
        assertEquals(StatusCode.KO, processWorkflow.getStatus());
    }

    @RunWithCustomExecutor
    @Test
    public void testPauseWorkflow() throws Exception {
        try {
            VitamThreadUtils.getVitamSession().setTenantId(tenantId);
            tryImportFile();
            final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            final GUID objectGuid = GUIDFactory.newManifestGUID(tenantId);
            final String containerName = objectGuid.getId();
            createLogbookOperation(operationGuid, objectGuid);

            final InputStream zipInputStreamSipObject =
                PropertiesUtils.getResourceAsStream(SIP_FILE_OK_NAME);
            workspaceClient = WorkspaceClientFactory.getInstance().getClient();
            workspaceClient.createContainer(containerName);
            workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP,
                zipInputStreamSipObject);

            processingClient = ProcessingManagementClientFactory.getInstance().getClient();
            processingClient.initVitamProcess(Contexts.DEFAULT_WORKFLOW.name(), containerName, WORFKLOW_NAME);

            ProcessDataManagement dataManagement = WorkspaceProcessDataManagement.getInstance();
            assertNotNull(dataManagement);

            assertNotNull(dataManagement.getProcessWorkflow(String.valueOf(ServerIdentity.getInstance().getServerId()),
                containerName));

            Response ret = processingClient.updateOperationActionProcess(ProcessAction.NEXT.getValue(),
                containerName);
            assertEquals(Status.ACCEPTED.getStatusCode(), ret.getStatus());

            wait(containerName);
            ProcessWorkflow processWorkflow =
                processMonitoring.findOneProcessWorkflow(containerName, tenantId);

            assertNotNull(processWorkflow);
            assertEquals(ProcessState.PAUSE, processWorkflow.getState());
            assertEquals(StatusCode.OK, processWorkflow.getStatus());
            // Let the processing do the job
            ret = processingClient.updateOperationActionProcess(ProcessAction.NEXT.getValue(),
                containerName);

            assertEquals(Status.ACCEPTED.getStatusCode(), ret.getStatus());

            wait(containerName);
            processWorkflow =
                processMonitoring.findOneProcessWorkflow(containerName, tenantId);

            assertNotNull(processWorkflow);
            assertEquals(ProcessState.PAUSE, processWorkflow.getState());
            assertEquals(StatusCode.WARNING, processWorkflow.getStatus());


            ret = processingClient.updateOperationActionProcess(ProcessAction.RESUME.getValue(),
                containerName);
            // Let the processing do the job
            assertEquals(Status.ACCEPTED.getStatusCode(), ret.getStatus());

            wait(containerName);
            processWorkflow =
                processMonitoring.findOneProcessWorkflow(containerName, tenantId);

            assertNotNull(processWorkflow);
            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
            assertEquals(StatusCode.WARNING, processWorkflow.getStatus());

            boolean exc = false;
            try {
                dataManagement.getProcessWorkflow(String.valueOf(ServerIdentity.getInstance().getServerId()),
                    containerName);
            } catch (ProcessingStorageWorkspaceException e) {
                exc = true;
            }

            // TODO the #2627 the workflow is not removed from workspace until 24h
            // assertTrue(exc);
        } catch (final Exception e) {
            e.printStackTrace();
            fail("should not raized an exception");
        }
    }


    @RunWithCustomExecutor
    @Test
    public void testWorkflowJsonValidationKOCA1() throws Exception {
        try {
            VitamThreadUtils.getVitamSession().setTenantId(tenantId);
            tryImportFile();
            final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            final GUID objectGuid = GUIDFactory.newManifestGUID(tenantId);
            final String containerName = objectGuid.getId();
            createLogbookOperation(operationGuid, objectGuid);

            // workspace client dezip SIP in workspace
            RestAssured.port = PORT_SERVICE_WORKSPACE;
            RestAssured.basePath = WORKSPACE_PATH;
            final InputStream zipInputStreamSipObject =
                PropertiesUtils.getResourceAsStream(SIP_FILE_1791_CA1);
            workspaceClient = WorkspaceClientFactory.getInstance().getClient();
            workspaceClient.createContainer(containerName);
            workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP,
                zipInputStreamSipObject);
            // call processing
            RestAssured.port = PORT_SERVICE_PROCESSING;
            RestAssured.basePath = PROCESSING_PATH;

            processingClient = ProcessingManagementClientFactory.getInstance().getClient();
            processingClient.initVitamProcess(Contexts.DEFAULT_WORKFLOW.name(), containerName, WORFKLOW_NAME);

            final Response ret =
                processingClient.updateOperationActionProcess(ProcessAction.RESUME.getValue(), containerName);
            assertNotNull(ret);
            assertEquals(Status.ACCEPTED.getStatusCode(), ret.getStatus());

            wait(containerName);
            ProcessWorkflow processWorkflow =
                processMonitoring.findOneProcessWorkflow(containerName, tenantId);
            assertNotNull(processWorkflow);
            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
            assertEquals(StatusCode.KO, processWorkflow.getStatus());

        } catch (final Exception e) {
            e.printStackTrace();
            fail("should not raized an exception");
        }
    }


    @RunWithCustomExecutor
    @Test
    public void testWorkflowJsonValidationKOCA2() throws Exception {
        try {
            VitamThreadUtils.getVitamSession().setTenantId(tenantId);
            tryImportFile();
            final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            final GUID objectGuid = GUIDFactory.newManifestGUID(tenantId);
            final String containerName = objectGuid.getId();
            createLogbookOperation(operationGuid, objectGuid);

            // workspace client dezip SIP in workspace
            RestAssured.port = PORT_SERVICE_WORKSPACE;
            RestAssured.basePath = WORKSPACE_PATH;
            final InputStream zipInputStreamSipObject =
                PropertiesUtils.getResourceAsStream(SIP_FILE_1791_CA2);
            workspaceClient = WorkspaceClientFactory.getInstance().getClient();
            workspaceClient.createContainer(containerName);
            workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP,
                zipInputStreamSipObject);
            // call processing
            RestAssured.port = PORT_SERVICE_PROCESSING;
            RestAssured.basePath = PROCESSING_PATH;

            processingClient = ProcessingManagementClientFactory.getInstance().getClient();
            processingClient.initVitamProcess(Contexts.DEFAULT_WORKFLOW.name(), containerName, WORFKLOW_NAME);

            final Response ret =
                processingClient.updateOperationActionProcess(ProcessAction.RESUME.getValue(), containerName);
            assertNotNull(ret);
            assertEquals(Status.ACCEPTED.getStatusCode(), ret.getStatus());

            wait(containerName);
            ProcessWorkflow processWorkflow =
                processMonitoring.findOneProcessWorkflow(containerName, tenantId);
            assertNotNull(processWorkflow);
            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
            assertEquals(StatusCode.KO, processWorkflow.getStatus());

        } catch (final Exception e) {
            e.printStackTrace();
            fail("should not raized an exception");
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowWithContractKO() throws Exception {
        try {
            VitamThreadUtils.getVitamSession().setTenantId(tenantId);
            tryImportFile();
            final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            final GUID objectGuid = GUIDFactory.newManifestGUID(tenantId);
            final String containerName = objectGuid.getId();
            createLogbookOperation(operationGuid, objectGuid);

            // workspace client dezip SIP in workspace
            RestAssured.port = PORT_SERVICE_WORKSPACE;
            RestAssured.basePath = WORKSPACE_PATH;
            final InputStream zipInputStreamSipObject =
                PropertiesUtils.getResourceAsStream(SIP_REFERENCE_CONTRACT_KO);
            workspaceClient = WorkspaceClientFactory.getInstance().getClient();
            workspaceClient.createContainer(containerName);
            workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP,
                zipInputStreamSipObject);
            // call processing
            RestAssured.port = PORT_SERVICE_PROCESSING;
            RestAssured.basePath = PROCESSING_PATH;

            processingClient = ProcessingManagementClientFactory.getInstance().getClient();
            processingClient.initVitamProcess(Contexts.DEFAULT_WORKFLOW.name(), containerName, WORFKLOW_NAME);

            final RequestResponse<JsonNode> ret =
                processingClient.executeOperationProcess(containerName, WORFKLOW_NAME,
                    Contexts.DEFAULT_WORKFLOW.name(), ProcessAction.RESUME.getValue());
            assertNotNull(ret);
            assertEquals(Status.ACCEPTED.getStatusCode(), ret.getStatus());

            wait(containerName);
            ProcessWorkflow processWorkflow =
                processMonitoring.findOneProcessWorkflow(containerName, tenantId);
            assertNotNull(processWorkflow);
            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
            assertEquals(StatusCode.KO, processWorkflow.getStatus());
        } catch (final Exception e) {
            e.printStackTrace();
            fail("should not raized an exception");
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowSIPContractProdService() throws Exception {
        try {
            flush();

            VitamThreadUtils.getVitamSession().setTenantId(tenantId);
            tryImportFile();
            final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            final GUID objectGuid = GUIDFactory.newManifestGUID(tenantId);
            final String containerName = objectGuid.getId();
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

            processingClient = ProcessingManagementClientFactory.getInstance().getClient();
            processingClient.initVitamProcess(Contexts.DEFAULT_WORKFLOW.name(), containerName, WORFKLOW_NAME);

            final Response ret =
                processingClient.updateOperationActionProcess(ProcessAction.RESUME.getValue(), containerName);
            assertNotNull(ret);

            assertEquals(Status.ACCEPTED.getStatusCode(), ret.getStatus());

            wait(containerName);
            ProcessWorkflow processWorkflow =
                processMonitoring.findOneProcessWorkflow(containerName, tenantId);
            assertNotNull(processWorkflow);
            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
            assertEquals(StatusCode.WARNING, processWorkflow.getStatus());

            LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient();
            fr.gouv.vitam.common.database.builder.request.single.Select selectQuery =
                new fr.gouv.vitam.common.database.builder.request.single.Select();
            selectQuery.setQuery(QueryHelper.eq("evIdProc", containerName));
            JsonNode logbookResult = logbookClient.selectOperation(selectQuery.getFinalSelect());

            assertEquals(logbookResult.get("$results").get(0).get("events").get(0).get("outDetail").asText(),
                "STP_INGEST_FINALISATION.OK");

        } catch (final Exception e) {
            e.printStackTrace();
            fail("should not raized an exception");
        }
    }

}
