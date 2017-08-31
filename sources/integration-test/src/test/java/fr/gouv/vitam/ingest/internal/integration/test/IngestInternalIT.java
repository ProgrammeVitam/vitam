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
package fr.gouv.vitam.ingest.internal.integration.test;

import static com.jayway.restassured.RestAssured.get;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.bson.Document;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.jayway.restassured.RestAssured;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import fr.gouv.vitam.access.internal.client.AccessInternalClient;
import fr.gouv.vitam.access.internal.client.AccessInternalClientFactory;
import fr.gouv.vitam.access.internal.rest.AccessInternalMain;
import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.client.configuration.ClientConfigurationImpl;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.multiple.UpdateMultiQuery;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.format.identification.FormatIdentifierFactory;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.junit.JunitHelper.ElasticsearchTestConfiguration;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.AccessContractModel;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.stream.SizedInputStream;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.client.model.IngestContractModel;
import fr.gouv.vitam.functional.administration.common.exception.DatabaseConflictException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesImportInProgressException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClient;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClientFactory;
import fr.gouv.vitam.ingest.internal.upload.rest.IngestInternalApplication;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookDocument;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookElasticsearchAccess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbName;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookOperation;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.metadata.rest.MetadataMain;
import fr.gouv.vitam.processing.common.model.ProcessWorkflow;
import fr.gouv.vitam.processing.data.core.ProcessDataAccessImpl;
import fr.gouv.vitam.processing.engine.core.monitoring.ProcessMonitoringImpl;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.processing.management.rest.ProcessManagementApplication;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.common.model.StorageCollectionType;
import fr.gouv.vitam.worker.server.rest.WorkerApplication;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import fr.gouv.vitam.workspace.rest.WorkspaceApplication;

/**
 * Ingest Internal integration test
 */
public class IngestInternalIT {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IngestInternalIT.class);
    private static final int DATABASE_PORT = 12346;
    private static MongodExecutable mongodExecutable;
    static MongodProcess mongod;
    private static LogbookElasticsearchAccess esClient;
    private static final Integer tenantId = 0;
    private static final String contractId = "aName3";

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @ClassRule
    public static TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static final long SLEEP_TIME = 100l;
    private static final long NB_TRY = 9600; // equivalent to 16 minute

    private static boolean imported = false;
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
    private static final int PORT_SERVICE_INGEST_INTERNAL = 8095;
    private static final int PORT_SERVICE_ACCESS_INTERNAL = 8092;

    private static final String METADATA_PATH = "/metadata/v1";
    private static final String PROCESSING_PATH = "/processing/v1";
    private static final String WORKER_PATH = "/worker/v1";
    private static final String WORKSPACE_PATH = "/workspace/v1";
    private static final String LOGBOOK_PATH = "/logbook/v1";
    private static final String INGEST_INTERNAL_PATH = "/ingest/v1";
    private static final String ACCESS_INTERNAL_PATH = "/access-internal/v1";
    private static final String CONTEXT_ID = "DEFAULT_WORKFLOW_RESUME";


    private static String CONFIG_WORKER_PATH = "";
    private static String CONFIG_WORKSPACE_PATH = "";
    private static String CONFIG_METADATA_PATH = "";
    private static String CONFIG_PROCESSING_PATH = "";
    private static String CONFIG_FUNCTIONAL_ADMIN_PATH = "";
    private static String CONFIG_LOGBOOK_PATH = "";
    private static String CONFIG_SIEGFRIED_PATH = "";
    private static String CONFIG_INGEST_INTERNAL_PATH = "";
    private static String CONFIG_ACCESS_INTERNAL_PATH = "";

    // private static VitamServer workerApplication;
    private static MetadataMain medtadataApplication;
    private static WorkerApplication wkrapplication;
    private static AdminManagementMain adminApplication;
    private static LogbookMain logbookApplication;
    private static WorkspaceApplication workspaceApplication;
    private static ProcessManagementApplication processManagementApplication;
    private static IngestInternalApplication ingestInternalApplication;
    private static AccessInternalMain accessInternalApplication;

    private static final String WORKSPACE_URL = "http://localhost:" + PORT_SERVICE_WORKSPACE;
    private static String SIP_TREE = "integration-ingest-internal/test_arbre.zip";
    private static String SIP_FILE_OK_NAME = "integration-ingest-internal/SIP-ingest-internal-ok.zip";
    private static String SIP_NB_OBJ_INCORRECT_IN_MANIFEST = "integration-ingest-internal/SIP_Conformity_KO.zip";
    private static String SIP_OK_WITH_MGT_META_DATA_ONLY_RULES = "integration-ingest-internal/SIP-MGTMETADATA-ONLY.zip";
    private static String SIP_OK_WITH_ADDRESSEE = "integration-ingest-internal/SIP_MAIL.zip";
    private static String SIP_OK_WITH_BOTH_UNITMGT_MGTMETADATA_RULES =
        "integration-ingest-internal/SIP-BOTH-UNITMGT-MGTMETADATA.zip";
    private static String SIP_OK_WITH_BOTH_UNITMGT_MGTMETADATA_RULES_WiTHOUT_OBJECTS =
        "integration-ingest-internal/SIP-BOTH-RULES-TYPES-WITHOUT-OBJECTS.zip";

    private static String SIP_KO_WITH_EMPTY_TITLE =
        "integration-processing/SIP_FILE_1791_CA1.zip";

    private static String SIP_KO_WITH_SPECIAL_CHARS =
        "integration-processing/SIP-2182-KO.zip";

    private static String SIP_KO_WITH_INCORRECT_DATE =
        "integration-processing/SIP_FILE_1791_CA2.zip";

    private static String SIP_OK_WITH_SERVICE_LEVEL =
        "integration-processing/SIP_2467_SERVICE_LEVEL.zip";
    private static String SIP_OK_WITHOUT_SERVICE_LEVEL =
        "integration-processing/SIP_2467_WITHOUT_SERVICE_LEVEL.zip";
    private static final String FILE_RULES_OK = "functional-admin/file-rules/jeu_donnees_OK_regles_CSV.csv";
    private static final String FILE_RULES_KO_DUPLICATED_REFERENCE =
        "functional-admin/file-rules/jeu_donnees_KO_regles_CSV_DuplicatedReference.csv";
    private static final String FILE_RULES_KO_UNKNOWN_DURATION =
        "functional-admin/file-rules/jeu_donnees_KO_regles_CSV_UNKNOWN_Duration.csv";
    private static final String FILE_RULES_KO_REFERENCE_WITH_WRONG_COMMA =
        "functional-admin/file-rules/jeu_donnees_KO_regles_CSV_ReferenceWithWrongComma.csv";
    private static final String FILE_RULES_KO_NEGATIVE_DURATION =
        "functional-admin/file-rules/jeu_donnees_KO_regles_CSV_Negative_Duration.csv";
    private static final String FILE_RULES_KO_DECADE_MEASURE =
        "functional-admin/file-rules/jeu_donnees_KO_regles_CSV_Decade_Measure.csv";
    private static final String FILE_RULES_KO_ANARCHY_RULE =
        "functional-admin/file-rules/jeu_donnees_KO_regles_CSV_AnarchyRule.csv";
    private static final String FILE_RULES_KO_90000_YEAR =
        "functional-admin/file-rules/jeu_donnees_KO_regles_CSV_90000_YEAR.csv";
    private static final String FILE_RULES_KO_600000_DAY =
        "functional-admin/file-rules/jeu_donnees_KO_regles_600000_DAY.csv";

    private static final String ERROR_REPORT_CONTENT = "functional-admin/file-rules/error_report_content.json";
    private static final String ERROR_REPORT_6000_DAYS = "functional-admin/file-rules/error_report_6000_days.json";
    private static final String ERROR_REPORT_9000_YEARS = "functional-admin/file-rules/error_report_9000_years.json";
    private static final String ERROR_REPORT_ANARCHY_RULE =
        "functional-admin/file-rules/error_report_anarchy_rules.json";
    private static final String ERROR_REPORT_DECADE_MEASURE =
        "functional-admin/file-rules/error_report_decade_measure.json";
    private static final String ERROR_REPORT_NEGATIVE_DURATION =
        "functional-admin/file-rules/error_report_negative_duration.json";
    private static final String ERROR_REPORT_REFERENCE_WITH_WRONG_COMA =
        "functional-admin/file-rules/error_report_reference_with_wrong_coma.json";
    private static final String ERROR_REPORT_UNKNOW_DURATION =
        "functional-admin/file-rules/error_report_unknow_duration.json";

    private static String SIP_OK_PHYSICAL_ARCHIVE = "integration-ingest-internal/OK_ArchivesPhysiques.zip";

    private static ElasticsearchTestConfiguration config = null;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        CONFIG_METADATA_PATH = PropertiesUtils.getResourcePath("integration-ingest-internal/metadata.conf").toString();
        CONFIG_WORKER_PATH = PropertiesUtils.getResourcePath("integration-ingest-internal/worker.conf").toString();
        CONFIG_WORKSPACE_PATH =
            PropertiesUtils.getResourcePath("integration-ingest-internal/workspace.conf").toString();
        CONFIG_PROCESSING_PATH =
            PropertiesUtils.getResourcePath("integration-ingest-internal/processing.conf").toString();
        CONFIG_FUNCTIONAL_ADMIN_PATH =
            PropertiesUtils.getResourcePath("integration-ingest-internal/functional-administration.conf").toString();

        CONFIG_LOGBOOK_PATH = PropertiesUtils.getResourcePath("integration-ingest-internal/logbook.conf").toString();
        CONFIG_SIEGFRIED_PATH =
            PropertiesUtils.getResourcePath("integration-ingest-internal/format-identifiers.conf").toString();

        CONFIG_INGEST_INTERNAL_PATH =
            PropertiesUtils.getResourcePath("integration-ingest-internal/ingest-internal.conf").toString();
        CONFIG_ACCESS_INTERNAL_PATH =
            PropertiesUtils.getResourcePath("integration-ingest-internal/access-internal.conf").toString();

        File tempFolder = temporaryFolder.newFolder();
        System.setProperty("vitam.tmp.folder", tempFolder.getAbsolutePath());

        SystemPropertyUtil.refresh();

        // ES
        config = JunitHelper.startElasticsearchForTest(temporaryFolder, CLUSTER_NAME, TCP_PORT, HTTP_PORT);

        final MongodStarter starter = MongodStarter.getDefaultInstance();

        mongodExecutable = starter.prepare(new MongodConfigBuilder()
            .withLaunchArgument("--enableMajorityReadConcern")
            .version(Version.Main.PRODUCTION)
            .net(new Net(DATABASE_PORT, Network.localhostIsIPv6()))
            .build());
        mongod = mongodExecutable.start();

        // ES client
        final List<ElasticsearchNode> nodes = new ArrayList<>();
        nodes.add(new ElasticsearchNode("localhost", config.getTcpPort()));
        esClient = new LogbookElasticsearchAccess(CLUSTER_NAME, nodes);

        // launch metadata
        SystemPropertyUtil.set(MetadataMain.PARAMETER_JETTY_SERVER_PORT,
            Integer.toString(PORT_SERVICE_METADATA));
        medtadataApplication = new MetadataMain(CONFIG_METADATA_PATH);
        medtadataApplication.start();
        SystemPropertyUtil.clear(MetadataMain.PARAMETER_JETTY_SERVER_PORT);

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
            .set(LogbookMain.PARAMETER_JETTY_SERVER_PORT, Integer.toString(PORT_SERVICE_LOGBOOK));
        logbookApplication = new LogbookMain(CONFIG_LOGBOOK_PATH);
        logbookApplication.start();
        SystemPropertyUtil.clear(LogbookMain.PARAMETER_JETTY_SERVER_PORT);

        LogbookOperationsClientFactory.changeMode(new ClientConfigurationImpl("localhost", PORT_SERVICE_LOGBOOK));
        LogbookLifeCyclesClientFactory.changeMode(new ClientConfigurationImpl("localhost", PORT_SERVICE_LOGBOOK));

        // launch processing
        SystemPropertyUtil.set(ProcessManagementApplication.PARAMETER_JETTY_SERVER_PORT,
            Integer.toString(PORT_SERVICE_PROCESSING));
        processManagementApplication = new ProcessManagementApplication(CONFIG_PROCESSING_PATH);
        processManagementApplication.start();
        SystemPropertyUtil.clear(ProcessManagementApplication.PARAMETER_JETTY_SERVER_PORT);

        // launch worker
        SystemPropertyUtil.set("jetty.worker.port", Integer.toString(PORT_SERVICE_WORKER));
        wkrapplication = new WorkerApplication(CONFIG_WORKER_PATH);
        wkrapplication.start();
        SystemPropertyUtil.clear("jetty.worker.port");

        FormatIdentifierFactory.getInstance().changeConfigurationFile(CONFIG_SIEGFRIED_PATH);

        // launch ingest-internal
        SystemPropertyUtil.set("jetty.ingest-internal.port", Integer.toString(PORT_SERVICE_INGEST_INTERNAL));
        ingestInternalApplication = new IngestInternalApplication(CONFIG_INGEST_INTERNAL_PATH);
        ingestInternalApplication.start();
        SystemPropertyUtil.clear("jetty.ingest-internal.port");

        // launch functional Admin server
        AdminManagementClientFactory
            .changeMode(new ClientConfigurationImpl("localhost", PORT_SERVICE_FUNCTIONAL_ADMIN));
        adminApplication = new AdminManagementMain(CONFIG_FUNCTIONAL_ADMIN_PATH);
        adminApplication.start();



        SystemPropertyUtil.set("jetty.access-internal.port", Integer.toString(PORT_SERVICE_ACCESS_INTERNAL));
        accessInternalApplication =
            new AccessInternalMain(CONFIG_ACCESS_INTERNAL_PATH);
        accessInternalApplication.start();
        SystemPropertyUtil.clear("jetty.access-internal.port");
        AccessInternalClientFactory.getInstance().changeServerPort(PORT_SERVICE_ACCESS_INTERNAL);

    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        esClient.close();
        if (config != null) {
            JunitHelper.stopElasticsearchForTest(config);
        }
        mongod.stop();
        mongodExecutable.stop();
        try {
            ingestInternalApplication.stop();
            workspaceApplication.stop();
            wkrapplication.stop();
            logbookApplication.stop();
            processManagementApplication.stop();
            medtadataApplication.stop();
            adminApplication.stop();
            accessInternalApplication.stop();
        } catch (final Exception e) {
            LOGGER.error(e);
        }
    }

    private void flush() {
        ProcessDataAccessImpl.getInstance().clearWorkflow();
    }

    private void wait(String operationId) {
        int nbTry = 0;
        ProcessingManagementClient processingClient =
            ProcessingManagementClientFactory.getInstance().getClient();
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

    private void tryImportFile() {

        VitamThreadUtils.getVitamSession().setContractId(contractId);
        flush();

        if (!imported) {
            try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
                client.importFormat(
                    PropertiesUtils.getResourceAsStream("integration-ingest-internal/DROID_SignatureFile_V88.xml"),
                    "DROID_SignatureFile_V88.xml");

                // Import Rules
                client.importRulesFile(
                    PropertiesUtils.getResourceAsStream("integration-ingest-internal/MGT_RULES_REF.csv"),
                    "MGT_RULES_REF.csv");

                // import contract
                File fileContracts =
                    PropertiesUtils.getResourceFile("integration-ingest-internal/referential_contracts_ok.json");
                List<IngestContractModel> IngestContractModelList = JsonHandler.getFromFileAsTypeRefence(fileContracts,
                    new TypeReference<List<IngestContractModel>>() {});

                client.importIngestContracts(IngestContractModelList);

                // import contrat
                File fileAccessContracts = PropertiesUtils.getResourceFile("access_contrats.json");
                List<AccessContractModel> accessContractModelList = JsonHandler
                    .getFromFileAsTypeRefence(fileAccessContracts, new TypeReference<List<AccessContractModel>>() {});
                client.importAccessContracts(accessContractModelList);
            } catch (final Exception e) {
                LOGGER.error(e);
            }
            imported = true;
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

            RestAssured.port = PORT_SERVICE_INGEST_INTERNAL;
            RestAssured.basePath = INGEST_INTERNAL_PATH;
            get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());

            RestAssured.port = PORT_SERVICE_ACCESS_INTERNAL;
            RestAssured.basePath = ACCESS_INTERNAL_PATH;
            get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());

        } catch (final Exception e) {
            e.printStackTrace();
            fail("should not raized an exception");
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testIngestInternal() throws Exception {
        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        try {
            VitamThreadUtils.getVitamSession().setTenantId(tenantId);
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            // ProcessDataAccessImpl processData = ProcessDataAccessImpl.getInstance();
            // processData.initProcessWorkflow(ProcessPopulator.populate(WORFKLOW_NAME), operationGuid.getId(),
            // ProcessAction.INIT, LogbookTypeProcess.INGEST, tenantId);
            tryImportFile();
            // workspace client dezip SIP in workspace
            RestAssured.port = PORT_SERVICE_WORKSPACE;
            RestAssured.basePath = WORKSPACE_PATH;
            final InputStream zipInputStreamSipObject =
                PropertiesUtils.getResourceAsStream(SIP_FILE_OK_NAME);

            // init default logbook operation
            final List<LogbookOperationParameters> params = new ArrayList<>();
            final LogbookOperationParameters initParameters = LogbookParametersFactory.newLogbookOperationParameters(
                operationGuid, "Process_SIP_unitary", operationGuid,
                LogbookTypeProcess.INGEST, StatusCode.STARTED,
                operationGuid != null ? operationGuid.toString() : "outcomeDetailMessage",
                operationGuid);
            params.add(initParameters);
            LOGGER.error(initParameters.toString());

            // call ingest
            IngestInternalClientFactory.getInstance().changeServerPort(PORT_SERVICE_INGEST_INTERNAL);
            final IngestInternalClient client = IngestInternalClientFactory.getInstance().getClient();
            final Response response2 = client.uploadInitialLogbook(params);
            assertEquals(response2.getStatus(), Status.CREATED.getStatusCode());

            // init workflow before execution
            client.initWorkFlow("DEFAULT_WORKFLOW_RESUME");

            client.upload(zipInputStreamSipObject, CommonMediaType.ZIP_TYPE, CONTEXT_ID);

            wait(operationGuid.toString());

            ProcessWorkflow processWorkflow =
                ProcessMonitoringImpl.getInstance().findOneProcessWorkflow(operationGuid.toString(), tenantId);

            assertNotNull(processWorkflow);
            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
            assertEquals(StatusCode.OK, processWorkflow.getStatus());

            // Try to check AU
            final MetaDataClient metadataClient = MetaDataClientFactory.getInstance().getClient();
            SelectMultiQuery select = new SelectMultiQuery();
            select.addQueries(QueryHelper.eq("Title", "Sensibilisation API"));
            final JsonNode node = metadataClient.selectUnits(select.getFinalSelect());
            LOGGER.debug(JsonHandler.prettyPrint(node));
            final JsonNode result = node.get("$results");
            assertNotNull(result);
            final JsonNode unit = result.get(0);
            assertNotNull(unit);
            final String og = unit.get("#object").asText();
            assertNotNull(og);
            // Try to check OG
            select = new SelectMultiQuery();
            select.addRoots(og);
            select.setProjectionSliceOnQualifier();
            final JsonNode jsonResponse = metadataClient.selectObjectGrouptbyId(select.getFinalSelect(), og);
            LOGGER.warn("Result: " + jsonResponse);
            final List<String> valuesAsText = jsonResponse.get("$results").findValuesAsText("_id");
            final String objectId = valuesAsText.get(0);
            final StorageClient storageClient = StorageClientFactory.getInstance().getClient();
            Response responseStorage = storageClient.getContainerAsync("default", objectId,
                StorageCollectionType.OBJECTS);
            InputStream inputStream = responseStorage.readEntity(InputStream.class);
            SizedInputStream sizedInputStream = new SizedInputStream(inputStream);
            final long size = StreamUtils.closeSilently(sizedInputStream);
            LOGGER.warn("read: " + size);

            assertTrue(size > 1000);

            final AccessInternalClient accessClient = AccessInternalClientFactory.getInstance().getClient();
            responseStorage = accessClient.getObject(new SelectMultiQuery().getFinalSelect(), og, "BinaryMaster", 1);
            inputStream = responseStorage.readEntity(InputStream.class);

            RequestResponse response = accessClient.updateUnitbyId(new UpdateMultiQuery().getFinalUpdate(),
                unit.findValuesAsText("#id").get(0));
            assertEquals(response.toJsonNode().get("$hits").get("size").asInt(), 1);

            sizedInputStream = new SizedInputStream(inputStream);
            final long size2 = StreamUtils.closeSilently(sizedInputStream);
            LOGGER.warn("read: " + size2);
            assertTrue(size2 == size);

            JsonNode logbookOperation =
                accessClient.selectOperationById(operationGuid.getId(), new SelectMultiQuery().getFinalSelect())
                    .toJsonNode();
            QueryBuilder query = QueryBuilders.matchQuery("_id", operationGuid.getId());
            SearchResponse elasticSearchResponse =
                esClient.search(LogbookCollections.OPERATION, tenantId, query, null, null, 0, 25);
            assertEquals(1, elasticSearchResponse.getHits().getTotalHits());
            assertNotNull(elasticSearchResponse.getHits().getAt(0));
            SearchHit hit = elasticSearchResponse.getHits().iterator().next();
            assertNotNull(hit);
            // TODO compare


        } catch (final Exception e) {
            LOGGER.error(e);
            SearchResponse elasticSearchResponse =
                esClient.search(LogbookCollections.OPERATION, tenantId, null, null, null, 0, 25);
            LOGGER.error("Total:" + (elasticSearchResponse.getHits().getTotalHits()));
            try (LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient()) {
                fr.gouv.vitam.common.database.builder.request.single.Select selectQuery =
                    new fr.gouv.vitam.common.database.builder.request.single.Select();
                selectQuery.setQuery(QueryHelper.eq("evIdProc", operationGuid.getId()));
                JsonNode logbookResult = logbookClient.selectOperation(selectQuery.getFinalSelect());
                LOGGER.error(JsonHandler.prettyPrint(logbookResult));
            }

            fail("should not raized an exception");
        }
    }


    @RunWithCustomExecutor
    @Test
    public void testPhysicalArchiveIngestInternal() throws Exception {
        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        try {
            VitamThreadUtils.getVitamSession().setTenantId(tenantId);
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            tryImportFile();
            // workspace client dezip SIP in workspace
            RestAssured.port = PORT_SERVICE_WORKSPACE;
            RestAssured.basePath = WORKSPACE_PATH;
            final InputStream zipInputStreamSipObject =
                PropertiesUtils.getResourceAsStream(SIP_OK_PHYSICAL_ARCHIVE);

            // init default logbook operation
            final List<LogbookOperationParameters> params = new ArrayList<>();
            final LogbookOperationParameters initParameters = LogbookParametersFactory.newLogbookOperationParameters(
                operationGuid, "Process_SIP_unitary", operationGuid,
                LogbookTypeProcess.INGEST, StatusCode.STARTED,
                operationGuid != null ? operationGuid.toString() : "outcomeDetailMessage",
                operationGuid);
            params.add(initParameters);
            LOGGER.debug(initParameters.toString());

            // call ingest
            IngestInternalClientFactory.getInstance().changeServerPort(PORT_SERVICE_INGEST_INTERNAL);
            final IngestInternalClient client = IngestInternalClientFactory.getInstance().getClient();
            final Response response2 = client.uploadInitialLogbook(params);
            assertEquals(response2.getStatus(), Status.CREATED.getStatusCode());

            // init workflow before execution
            client.initWorkFlow("DEFAULT_WORKFLOW_RESUME");

            client.upload(zipInputStreamSipObject, CommonMediaType.ZIP_TYPE, CONTEXT_ID);

            wait(operationGuid.toString());

            ProcessWorkflow processWorkflow =
                ProcessMonitoringImpl.getInstance().findOneProcessWorkflow(operationGuid.toString(), tenantId);

            assertNotNull(processWorkflow);
            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
            assertEquals(StatusCode.WARNING, processWorkflow.getStatus());

            // Try to check AU
            final MetaDataClient metadataClient = MetaDataClientFactory.getInstance().getClient();
            SelectMultiQuery select = new SelectMultiQuery();
            select.addQueries(QueryHelper.eq("Title", "Sed blandit mi dolor"));
            final JsonNode node = metadataClient.selectUnits(select.getFinalSelect());
            LOGGER.debug(JsonHandler.prettyPrint(node));
            final JsonNode result = node.get("$results");
            assertNotNull(result);
            final JsonNode unit = result.get(0);
            assertNotNull(unit);
            final String og = unit.get("#object").asText();
            assertNotNull(og);
            // Try to check OG
            select = new SelectMultiQuery();
            select.addRoots(og);
            select.setProjectionSliceOnQualifier();
            final JsonNode jsonResponse = metadataClient.selectObjectGrouptbyId(select.getFinalSelect(), og);
            LOGGER.warn("Result: " + jsonResponse);
            final List<String> valuesAsText = jsonResponse.get("$results").findValuesAsText("_id");
            final String objectId = valuesAsText.get(0);
            LOGGER.warn("read: " + objectId);

        } catch (final Exception e) {
            LOGGER.error(e);
            try (LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient()) {
                fr.gouv.vitam.common.database.builder.request.single.Select selectQuery =
                    new fr.gouv.vitam.common.database.builder.request.single.Select();
                selectQuery.setQuery(QueryHelper.eq("evIdProc", operationGuid.getId()));
                JsonNode logbookResult = logbookClient.selectOperation(selectQuery.getFinalSelect());
                LOGGER.error(JsonHandler.prettyPrint(logbookResult));
            }
            fail("should not raized an exception");
        }
    }


    @RunWithCustomExecutor
    @Test
    public void testIngestInternal2182CA1() throws Exception {
        try {
            final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
            VitamThreadUtils.getVitamSession().setTenantId(tenantId);
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            tryImportFile();
            // workspace client dezip SIP in workspace
            RestAssured.port = PORT_SERVICE_WORKSPACE;
            RestAssured.basePath = WORKSPACE_PATH;
            final InputStream zipInputStreamSipObject =
                PropertiesUtils.getResourceAsStream(SIP_KO_WITH_SPECIAL_CHARS);

            // init default logbook operation
            final List<LogbookOperationParameters> params = new ArrayList<>();
            final LogbookOperationParameters initParameters = LogbookParametersFactory.newLogbookOperationParameters(
                operationGuid, "Process_SIP_unitary", operationGuid,
                LogbookTypeProcess.INGEST, StatusCode.STARTED,
                operationGuid != null ? operationGuid.toString() : "outcomeDetailMessage",
                operationGuid);
            params.add(initParameters);
            LOGGER.error(initParameters.toString());

            // call ingest
            IngestInternalClientFactory.getInstance().changeServerPort(PORT_SERVICE_INGEST_INTERNAL);
            final IngestInternalClient client = IngestInternalClientFactory.getInstance().getClient();
            final Response response2 = client.uploadInitialLogbook(params);
            assertEquals(response2.getStatus(), Status.CREATED.getStatusCode());

            // init workflow before execution
            client.initWorkFlow("DEFAULT_WORKFLOW_RESUME");

            client.upload(zipInputStreamSipObject, CommonMediaType.ZIP_TYPE, CONTEXT_ID);

            wait(operationGuid.toString());

            ProcessWorkflow processWorkflow =
                ProcessMonitoringImpl.getInstance().findOneProcessWorkflow(operationGuid.toString(), tenantId);

            assertNotNull(processWorkflow);
            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
            assertEquals(StatusCode.KO, processWorkflow.getStatus());

            final AccessInternalClient accessClient = AccessInternalClientFactory.getInstance().getClient();
            JsonNode logbookOperation =
                accessClient.selectOperationById(operationGuid.getId(), new SelectMultiQuery().getFinalSelect())
                    .toJsonNode();
            boolean checkDataObject = true;
            final JsonNode elmt = logbookOperation.get("$results").get(0);
            final List<Document> logbookOperationEvents =
                (List<Document>) new LogbookOperation(elmt).get(LogbookDocument.EVENTS.toString());
            for (final Document event : logbookOperationEvents) {
                if (StatusCode.KO.toString()
                    .equals(event.get(LogbookMongoDbName.outcome.getDbname()).toString()) &&
                    event.get(LogbookMongoDbName.eventType.getDbname())
                        .equals("CHECK_UNIT_SCHEMA")) {
                    checkDataObject = false;
                    break;
                }
            }

            assertTrue(!checkDataObject);

        } catch (final Exception e) {
            e.printStackTrace();
            fail("should not raized an exception");
        }
    }


    @RunWithCustomExecutor
    @Test
    public void testIngestInternal1791CA1() throws Exception {
        try {
            final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
            VitamThreadUtils.getVitamSession().setTenantId(tenantId);
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            tryImportFile();
            // workspace client dezip SIP in workspace
            RestAssured.port = PORT_SERVICE_WORKSPACE;
            RestAssured.basePath = WORKSPACE_PATH;
            final InputStream zipInputStreamSipObject =
                PropertiesUtils.getResourceAsStream(SIP_KO_WITH_EMPTY_TITLE);

            // init default logbook operation
            final List<LogbookOperationParameters> params = new ArrayList<>();
            final LogbookOperationParameters initParameters = LogbookParametersFactory.newLogbookOperationParameters(
                operationGuid, "Process_SIP_unitary", operationGuid,
                LogbookTypeProcess.INGEST, StatusCode.STARTED,
                operationGuid != null ? operationGuid.toString() : "outcomeDetailMessage",
                operationGuid);
            params.add(initParameters);
            LOGGER.error(initParameters.toString());

            // call ingest
            IngestInternalClientFactory.getInstance().changeServerPort(PORT_SERVICE_INGEST_INTERNAL);
            final IngestInternalClient client = IngestInternalClientFactory.getInstance().getClient();
            final Response response2 = client.uploadInitialLogbook(params);
            assertEquals(response2.getStatus(), Status.CREATED.getStatusCode());

            // init workflow before execution
            client.initWorkFlow("DEFAULT_WORKFLOW_RESUME");

            client.upload(zipInputStreamSipObject, CommonMediaType.ZIP_TYPE, CONTEXT_ID);

            wait(operationGuid.toString());

            ProcessWorkflow processWorkflow =
                ProcessMonitoringImpl.getInstance().findOneProcessWorkflow(operationGuid.toString(), tenantId);

            assertNotNull(processWorkflow);
            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
            assertEquals(StatusCode.KO, processWorkflow.getStatus());


            final AccessInternalClient accessClient = AccessInternalClientFactory.getInstance().getClient();
            JsonNode logbookOperation =
                accessClient.selectOperationById(operationGuid.getId(), new SelectMultiQuery().getFinalSelect())
                    .toJsonNode();
            boolean checkUnitSuccess = true;
            final JsonNode elmt = logbookOperation.get("$results").get(0);
            final List<Document> logbookOperationEvents =
                (List<Document>) new LogbookOperation(elmt).get(LogbookDocument.EVENTS.toString());
            for (final Document event : logbookOperationEvents) {
                if (StatusCode.KO.toString()
                    .equals(event.get(LogbookMongoDbName.outcome.getDbname()).toString()) &&
                    event.get(LogbookMongoDbName.eventType.getDbname()).equals("CHECK_UNIT_SCHEMA")) {
                    checkUnitSuccess = false;
                    break;
                }
            }

            assertTrue(!checkUnitSuccess);

        } catch (final Exception e) {
            e.printStackTrace();
            fail("should not raized an exception");
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testIngestInternal1791CA2() throws Exception {
        try {
            final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
            VitamThreadUtils.getVitamSession().setTenantId(tenantId);
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            tryImportFile();
            // workspace client dezip SIP in workspace
            RestAssured.port = PORT_SERVICE_WORKSPACE;
            RestAssured.basePath = WORKSPACE_PATH;
            final InputStream zipInputStreamSipObject =
                PropertiesUtils.getResourceAsStream(SIP_KO_WITH_INCORRECT_DATE);

            // init default logbook operation
            final List<LogbookOperationParameters> params = new ArrayList<>();
            final LogbookOperationParameters initParameters = LogbookParametersFactory.newLogbookOperationParameters(
                operationGuid, "Process_SIP_unitary", operationGuid,
                LogbookTypeProcess.INGEST, StatusCode.STARTED,
                operationGuid != null ? operationGuid.toString() : "outcomeDetailMessage",
                operationGuid);
            params.add(initParameters);
            LOGGER.error(initParameters.toString());

            // call ingest
            IngestInternalClientFactory.getInstance().changeServerPort(PORT_SERVICE_INGEST_INTERNAL);
            final IngestInternalClient client = IngestInternalClientFactory.getInstance().getClient();
            final Response response2 = client.uploadInitialLogbook(params);
            assertEquals(response2.getStatus(), Status.CREATED.getStatusCode());

            // init workflow before execution
            client.initWorkFlow("DEFAULT_WORKFLOW_RESUME");

            client.upload(zipInputStreamSipObject, CommonMediaType.ZIP_TYPE, CONTEXT_ID);

            wait(operationGuid.toString());

            ProcessWorkflow processWorkflow =
                ProcessMonitoringImpl.getInstance().findOneProcessWorkflow(operationGuid.toString(), tenantId);

            assertNotNull(processWorkflow);
            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
            assertEquals(StatusCode.KO, processWorkflow.getStatus());


            final AccessInternalClient accessClient = AccessInternalClientFactory.getInstance().getClient();
            JsonNode logbookOperation =
                accessClient.selectOperationById(operationGuid.getId(), new SelectMultiQuery().getFinalSelect())
                    .toJsonNode();
            boolean checkUnitSuccess = true;
            final JsonNode elmt = logbookOperation.get("$results").get(0);
            final List<Document> logbookOperationEvents =
                (List<Document>) new LogbookOperation(elmt).get(LogbookDocument.EVENTS.toString());
            for (final Document event : logbookOperationEvents) {
                if (StatusCode.KO.toString()
                    .equals(event.get(LogbookMongoDbName.outcome.getDbname()).toString()) &&
                    event.get(LogbookMongoDbName.eventType.getDbname()).equals("CHECK_UNIT_SCHEMA")) {
                    checkUnitSuccess = false;
                    break;
                }
            }

            assertTrue(!checkUnitSuccess);

        } catch (final Exception e) {
            e.printStackTrace();
            fail("should not raized an exception");
        }
    }


    @RunWithCustomExecutor
    @Test
    public void testIngestWithManifestIncorrectObjectNumber() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        tryImportFile();
        // TODO: 6/6/17 why objectGuid ? The test fail on the logbook
        final GUID objectGuid = GUIDFactory.newManifestGUID(0);
        // workspace client dezip SIP in workspace
        RestAssured.port = PORT_SERVICE_WORKSPACE;
        RestAssured.basePath = WORKSPACE_PATH;
        final InputStream zipInputStreamSipObject =
            PropertiesUtils.getResourceAsStream(SIP_NB_OBJ_INCORRECT_IN_MANIFEST);

        final List<LogbookOperationParameters> params = new ArrayList<>();
        final LogbookOperationParameters initParameters = LogbookParametersFactory.newLogbookOperationParameters(
            operationGuid, "Process_SIP_unitary", operationGuid,
            LogbookTypeProcess.INGEST, StatusCode.STARTED,
            operationGuid != null ? operationGuid.toString() : "outcomeDetailMessage",
            operationGuid);
        params.add(initParameters);
        LOGGER.error(initParameters.toString());

        // call ingest
        IngestInternalClientFactory.getInstance().changeServerPort(PORT_SERVICE_INGEST_INTERNAL);
        final IngestInternalClient client = IngestInternalClientFactory.getInstance().getClient();
        final Response response2 = client.uploadInitialLogbook(params);
        assertEquals(response2.getStatus(), Status.CREATED.getStatusCode());

        // init workflow before execution
        client.initWorkFlow("DEFAULT_WORKFLOW_RESUME");

        client.upload(zipInputStreamSipObject, CommonMediaType.ZIP_TYPE, CONTEXT_ID);

        wait(operationGuid.toString());

        ProcessWorkflow processWorkflow =
            ProcessMonitoringImpl.getInstance().findOneProcessWorkflow(operationGuid.toString(), tenantId);

        assertNotNull(processWorkflow);
        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
        assertEquals(StatusCode.KO, processWorkflow.getStatus());

    }

    @RunWithCustomExecutor
    @Test
    public void testIngestWithManifestHavingMgtRules() throws Exception {
        try {
            VitamThreadUtils.getVitamSession().setTenantId(tenantId);
            final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            // ProcessDataAccessImpl processData = ProcessDataAccessImpl.getInstance();
            // processData.initProcessWorkflow(ProcessPopulator.populate(WORFKLOW_NAME), operationGuid.getId(),
            // ProcessAction.INIT, LogbookTypeProcess.INGEST, tenantId);
            tryImportFile();
            // workspace client dezip SIP in workspace
            RestAssured.port = PORT_SERVICE_WORKSPACE;
            RestAssured.basePath = WORKSPACE_PATH;
            final InputStream zipInputStreamSipObject =
                PropertiesUtils.getResourceAsStream(SIP_OK_WITH_MGT_META_DATA_ONLY_RULES);

            // init default logbook operation
            final List<LogbookOperationParameters> params = new ArrayList<>();
            final LogbookOperationParameters initParameters = LogbookParametersFactory.newLogbookOperationParameters(
                operationGuid, "Process_SIP_unitary", operationGuid,
                LogbookTypeProcess.INGEST, StatusCode.STARTED,
                operationGuid != null ? operationGuid.toString() : "outcomeDetailMessage",
                operationGuid);
            params.add(initParameters);
            LOGGER.error(initParameters.toString());

            // call ingest
            IngestInternalClientFactory.getInstance().changeServerPort(PORT_SERVICE_INGEST_INTERNAL);
            final IngestInternalClient client = IngestInternalClientFactory.getInstance().getClient();
            final Response response2 = client.uploadInitialLogbook(params);

            assertEquals(response2.getStatus(), Status.CREATED.getStatusCode());

            // init workflow before execution
            client.initWorkFlow("DEFAULT_WORKFLOW_RESUME");
            client.upload(zipInputStreamSipObject, CommonMediaType.ZIP_TYPE, CONTEXT_ID);

            wait(operationGuid.toString());

            ProcessWorkflow processWorkflow =
                ProcessMonitoringImpl.getInstance().findOneProcessWorkflow(operationGuid.toString(), tenantId);

            assertNotNull(processWorkflow);
            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
            assertEquals(StatusCode.WARNING, processWorkflow.getStatus());

            // Try to check AU
            final MetaDataClient metadataClient = MetaDataClientFactory.getInstance().getClient();
            SelectMultiQuery select = new SelectMultiQuery();
            select.addQueries(QueryHelper.eq("Title", "Unit with Management META DATA rules"));
            final JsonNode node = metadataClient.selectUnits(select.getFinalSelect());
            LOGGER.debug(JsonHandler.prettyPrint(node));
            final JsonNode result = node.get("$results");
            assertNotNull(result);
            final JsonNode unit = result.get(0);
            assertNotNull(unit);

            // Check the added management rules
            assertEquals(unit.get("#management").size(), 2);

            // Check that only the rule declared in "ManagementMetaData" was added : StorageRule
            assertTrue(unit.get("#management").has("StorageRule"));
        } catch (final Exception e) {
            e.printStackTrace();
            fail("should not raized an exception");
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testIngestWithManifestHavingBothUnitMgtAndMgtMetaDataRules() throws Exception {
        try {
            VitamThreadUtils.getVitamSession().setTenantId(tenantId);
            final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);

            tryImportFile();

            // workspace client dezip SIP in workspace
            RestAssured.port = PORT_SERVICE_WORKSPACE;
            RestAssured.basePath = WORKSPACE_PATH;
            final InputStream zipInputStreamSipObject =
                PropertiesUtils.getResourceAsStream(SIP_OK_WITH_BOTH_UNITMGT_MGTMETADATA_RULES);

            // init default logbook operation
            final List<LogbookOperationParameters> params = new ArrayList<>();
            final LogbookOperationParameters initParameters = LogbookParametersFactory.newLogbookOperationParameters(
                operationGuid, "Process_SIP_unitary", operationGuid,
                LogbookTypeProcess.INGEST, StatusCode.STARTED,
                operationGuid != null ? operationGuid.toString() : "outcomeDetailMessage",
                operationGuid);
            params.add(initParameters);
            LOGGER.error(initParameters.toString());

            // call ingest
            IngestInternalClientFactory.getInstance().changeServerPort(PORT_SERVICE_INGEST_INTERNAL);
            final IngestInternalClient client = IngestInternalClientFactory.getInstance().getClient();
            final Response response2 = client.uploadInitialLogbook(params);

            // init workflow before execution
            client.initWorkFlow("DEFAULT_WORKFLOW_RESUME");


            assertEquals(response2.getStatus(), Status.CREATED.getStatusCode());
            client.upload(zipInputStreamSipObject, CommonMediaType.ZIP_TYPE, CONTEXT_ID);

            wait(operationGuid.toString());

            ProcessWorkflow processWorkflow =
                ProcessMonitoringImpl.getInstance().findOneProcessWorkflow(operationGuid.toString(), tenantId);

            assertThat(processWorkflow).isNotNull();
            assertThat(processWorkflow.getState()).isEqualTo(ProcessState.COMPLETED);
            assertThat(processWorkflow.getStatus()).isEqualTo(StatusCode.WARNING);

            // Try to check AU
            final MetaDataClient metadataClient = MetaDataClientFactory.getInstance().getClient();
            SelectMultiQuery select = new SelectMultiQuery();
            select.addQueries(QueryHelper.eq("Title", "UNIT with both rules"));
            final JsonNode node = metadataClient.selectUnits(select.getFinalSelect());
            LOGGER.debug(JsonHandler.prettyPrint(node));
            final JsonNode result = node.get("$results");
            assertNotNull(result);
            final JsonNode unit = result.get(0);
            assertNotNull(unit);

            // Check the added management rules
            assertEquals(unit.get("#management").size(), 3);

            // Check that both the rules declared in "ManagementMetaData" and in the unit were added : StorageRule +
            // AccessRule
            assertTrue(unit.get("#management").has("StorageRule"));
            assertTrue(unit.get("#management").has("AccessRule"));
        } catch (final Exception e) {
            e.printStackTrace();
            fail("should not raized an exception");
        }
    }

    // SHDGR__GR_4_H_3__001_0000
    @RunWithCustomExecutor
    @Test
    public void testIngestWithManifestHavingBothUnitMgtAndMgtMetaDataRulesWithoutObjects() throws Exception {
        try {
            VitamThreadUtils.getVitamSession().setTenantId(tenantId);
            final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);

            tryImportFile();
            // workspace client dezip SIP in workspace
            RestAssured.port = PORT_SERVICE_WORKSPACE;
            RestAssured.basePath = WORKSPACE_PATH;
            final InputStream zipInputStreamSipObject =
                PropertiesUtils.getResourceAsStream(SIP_OK_WITH_BOTH_UNITMGT_MGTMETADATA_RULES_WiTHOUT_OBJECTS);

            // init default logbook operation
            final List<LogbookOperationParameters> params = new ArrayList<>();
            final LogbookOperationParameters initParameters = LogbookParametersFactory.newLogbookOperationParameters(
                operationGuid, "Process_SIP_unitary", operationGuid,
                LogbookTypeProcess.INGEST, StatusCode.STARTED,
                operationGuid != null ? operationGuid.toString() : "outcomeDetailMessage",
                operationGuid);
            params.add(initParameters);
            LOGGER.error(initParameters.toString());

            // call ingest
            IngestInternalClientFactory.getInstance().changeServerPort(PORT_SERVICE_INGEST_INTERNAL);
            final IngestInternalClient client = IngestInternalClientFactory.getInstance().getClient();
            final Response response2 = client.uploadInitialLogbook(params);

            // init workflow before execution
            client.initWorkFlow("DEFAULT_WORKFLOW_RESUME");

            assertEquals(response2.getStatus(), Status.CREATED.getStatusCode());
            client.upload(zipInputStreamSipObject, CommonMediaType.ZIP_TYPE, CONTEXT_ID);

            wait(operationGuid.toString());

            ProcessWorkflow processWorkflow =
                ProcessMonitoringImpl.getInstance().findOneProcessWorkflow(operationGuid.toString(), tenantId);

            assertNotNull(processWorkflow);
            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
            assertEquals(StatusCode.WARNING, processWorkflow.getStatus());

            // Try to check AU
            final MetaDataClient metadataClient = MetaDataClientFactory.getInstance().getClient();
            SelectMultiQuery select = new SelectMultiQuery();
            select.addQueries(QueryHelper.eq("Title", "LEVANT"));
            final JsonNode node = metadataClient.selectUnits(select.getFinalSelect());
            LOGGER.debug(JsonHandler.prettyPrint(node));
            final JsonNode result = node.get("$results");
            assertNotNull(result);
            final JsonNode unit = result.get(0);
            assertNotNull(unit);

            // Check the added management rules
            assertEquals(unit.get("#management").size(), 2);

            // Check that the rule declared in the unit was added : AccessRule
            assertTrue(unit.get("#management").has("AccessRule"));
        } catch (final Exception e) {
            e.printStackTrace();
            fail("should not raized an exception");
        }
    }


    @RunWithCustomExecutor
    @Test
    public void testIngestWithAddresseeFieldsInManifest() throws Exception {
        // Now that HTML patterns are refused, this test is now KO
        try {
            VitamThreadUtils.getVitamSession().setTenantId(tenantId);
            final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            tryImportFile();
            // workspace client dezip SIP in workspace
            RestAssured.port = PORT_SERVICE_WORKSPACE;
            RestAssured.basePath = WORKSPACE_PATH;
            final InputStream zipInputStreamSipObject =
                PropertiesUtils.getResourceAsStream(SIP_OK_WITH_ADDRESSEE);

            // init default logbook operation
            final List<LogbookOperationParameters> params = new ArrayList<>();
            final LogbookOperationParameters initParameters = LogbookParametersFactory.newLogbookOperationParameters(
                operationGuid, "Process_SIP_unitary", operationGuid,
                LogbookTypeProcess.INGEST, StatusCode.STARTED,
                operationGuid != null ? operationGuid.toString() : "outcomeDetailMessage",
                operationGuid);
            params.add(initParameters);
            LOGGER.error(initParameters.toString());

            // call ingest
            IngestInternalClientFactory.getInstance().changeServerPort(PORT_SERVICE_INGEST_INTERNAL);
            final IngestInternalClient client = IngestInternalClientFactory.getInstance().getClient();
            final Response response2 = client.uploadInitialLogbook(params);

            assertEquals(response2.getStatus(), Status.CREATED.getStatusCode());

            // init workflow before execution
            client.initWorkFlow("DEFAULT_WORKFLOW_RESUME");
            client.upload(zipInputStreamSipObject, CommonMediaType.ZIP_TYPE, CONTEXT_ID);

            wait(operationGuid.toString());

            ProcessWorkflow processWorkflow =
                ProcessMonitoringImpl.getInstance().findOneProcessWorkflow(operationGuid.toString(), tenantId);

            assertNotNull(processWorkflow);
            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());

            // TODO: 6/6/17 This test status code is KO why OK needed bellow checkUnitSuccess
            assertEquals(StatusCode.KO, processWorkflow.getStatus());

            final AccessInternalClient accessClient = AccessInternalClientFactory.getInstance().getClient();
            JsonNode logbookOperation =
                accessClient.selectOperationById(operationGuid.getId(), new SelectMultiQuery().getFinalSelect())
                    .toJsonNode();
            boolean checkUnitSuccess = false;
            final JsonNode elmt = logbookOperation.get("$results").get(0);
            final List<Document> logbookOperationEvents =
                (List<Document>) new LogbookOperation(elmt).get(LogbookDocument.EVENTS.toString());
            for (final Document event : logbookOperationEvents) {
                if (StatusCode.KO.toString()
                    .equals(event.get(LogbookMongoDbName.outcome.getDbname()).toString()) &&
                    event.get(LogbookMongoDbName.eventType.getDbname())
                        .equals("CHECK_UNIT_SCHEMA")) {
                    checkUnitSuccess = true;
                    break;
                }
            }

            assertTrue(checkUnitSuccess);

        } catch (final Exception e) {
            e.printStackTrace();
            fail("should not raized an exception");
        }
    }


    @RunWithCustomExecutor
    @Test
    public void testIngestWithServiceLevelInManifest() throws Exception {
        try {
            VitamThreadUtils.getVitamSession().setTenantId(tenantId);
            final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            tryImportFile();
            // workspace client dezip SIP in workspace
            RestAssured.port = PORT_SERVICE_WORKSPACE;
            RestAssured.basePath = WORKSPACE_PATH;
            final InputStream zipInputStreamSipObject =
                PropertiesUtils.getResourceAsStream(SIP_OK_WITH_SERVICE_LEVEL);

            // init default logbook operation
            final List<LogbookOperationParameters> params = new ArrayList<>();
            final LogbookOperationParameters initParameters = LogbookParametersFactory.newLogbookOperationParameters(
                operationGuid, "Process_SIP_unitary", operationGuid,
                LogbookTypeProcess.INGEST, StatusCode.STARTED,
                operationGuid != null ? operationGuid.toString() : "outcomeDetailMessage",
                operationGuid);
            params.add(initParameters);
            LOGGER.error(initParameters.toString());

            // call ingest
            IngestInternalClientFactory.getInstance().changeServerPort(PORT_SERVICE_INGEST_INTERNAL);
            final IngestInternalClient client = IngestInternalClientFactory.getInstance().getClient();
            final Response response2 = client.uploadInitialLogbook(params);

            assertEquals(response2.getStatus(), Status.CREATED.getStatusCode());

            // init workflow before execution
            client.initWorkFlow("DEFAULT_WORKFLOW_RESUME");
            client.upload(zipInputStreamSipObject, CommonMediaType.ZIP_TYPE, CONTEXT_ID);

            wait(operationGuid.toString());

            ProcessWorkflow processWorkflow =
                ProcessMonitoringImpl.getInstance().findOneProcessWorkflow(operationGuid.toString(), tenantId);

            assertNotNull(processWorkflow);
            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
            assertEquals(StatusCode.WARNING, processWorkflow.getStatus());

            final AccessInternalClient accessClient = AccessInternalClientFactory.getInstance().getClient();
            JsonNode logbookOperation =
                accessClient.selectOperationById(operationGuid.getId(), new SelectMultiQuery().getFinalSelect())
                    .toJsonNode();
            boolean checkServiceLevel = false;
            final JsonNode elmt = logbookOperation.get("$results").get(0);
            final List<Document> logbookOperationEvents =
                (List<Document>) new LogbookOperation(elmt).get(LogbookDocument.EVENTS.toString());
            for (final Document event : logbookOperationEvents) {
                if (StatusCode.OK.toString()
                    .equals(event.get(LogbookMongoDbName.outcome.getDbname()).toString()) &&
                    event.get(LogbookMongoDbName.outcomeDetail.getDbname()).equals("CHECK_DATAOBJECTPACKAGE.OK")) {
                    if ("ServiceLevel0".equals(
                        JsonHandler.getFromString(event.get(LogbookMongoDbName.eventDetailData.getDbname()).toString())
                            .get("ServiceLevel").asText())) {
                        checkServiceLevel = true;
                    }
                    break;
                }
            }

            assertTrue(checkServiceLevel);

        } catch (final Exception e) {
            e.printStackTrace();
            fail("should not raized an exception");
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testIngestWithoutServiceLevelInManifest() throws Exception {
        try {
            VitamThreadUtils.getVitamSession().setTenantId(tenantId);
            final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            tryImportFile();
            // workspace client dezip SIP in workspace
            RestAssured.port = PORT_SERVICE_WORKSPACE;
            RestAssured.basePath = WORKSPACE_PATH;
            final InputStream zipInputStreamSipObject =
                PropertiesUtils.getResourceAsStream(SIP_OK_WITHOUT_SERVICE_LEVEL);

            // init default logbook operation
            final List<LogbookOperationParameters> params = new ArrayList<>();
            final LogbookOperationParameters initParameters = LogbookParametersFactory.newLogbookOperationParameters(
                operationGuid, "Process_SIP_unitary", operationGuid,
                LogbookTypeProcess.INGEST, StatusCode.STARTED,
                operationGuid != null ? operationGuid.toString() : "outcomeDetailMessage",
                operationGuid);
            params.add(initParameters);
            LOGGER.error(initParameters.toString());

            // call ingest
            IngestInternalClientFactory.getInstance().changeServerPort(PORT_SERVICE_INGEST_INTERNAL);
            final IngestInternalClient client = IngestInternalClientFactory.getInstance().getClient();
            final Response response2 = client.uploadInitialLogbook(params);

            assertEquals(response2.getStatus(), Status.CREATED.getStatusCode());

            // init workflow before execution
            client.initWorkFlow("DEFAULT_WORKFLOW_RESUME");
            client.upload(zipInputStreamSipObject, CommonMediaType.ZIP_TYPE, CONTEXT_ID);

            wait(operationGuid.toString());

            ProcessWorkflow processWorkflow =
                ProcessMonitoringImpl.getInstance().findOneProcessWorkflow(operationGuid.toString(), tenantId);

            assertNotNull(processWorkflow);
            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
            assertEquals(StatusCode.WARNING, processWorkflow.getStatus());

            final AccessInternalClient accessClient = AccessInternalClientFactory.getInstance().getClient();
            JsonNode logbookOperation =
                accessClient.selectOperationById(operationGuid.getId(), new SelectMultiQuery().getFinalSelect())
                    .toJsonNode();
            boolean checkServiceLevel = false;
            final JsonNode elmt = logbookOperation.get("$results").get(0);
            final List<Document> logbookOperationEvents =
                (List<Document>) new LogbookOperation(elmt).get(LogbookDocument.EVENTS.toString());
            for (final Document event : logbookOperationEvents) {
                if (StatusCode.OK.toString()
                    .equals(event.get(LogbookMongoDbName.outcome.getDbname()).toString()) &&
                    event.get(LogbookMongoDbName.outcomeDetail.getDbname()).equals("CHECK_DATAOBJECTPACKAGE.OK")) {
                    if (JsonHandler.getFromString(event.get(LogbookMongoDbName.eventDetailData.getDbname()).toString())
                        .get("ServiceLevel") instanceof NullNode) {
                        checkServiceLevel = true;
                    }
                    break;
                }
            }

            assertTrue(checkServiceLevel);

        } catch (final Exception e) {
            e.printStackTrace();
            fail("should not raized an exception");
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testProdServicesOK() throws Exception {
        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        try {
            VitamThreadUtils.getVitamSession().setTenantId(tenantId);
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            tryImportFile();
            // workspace client dezip SIP in workspace
            RestAssured.port = PORT_SERVICE_WORKSPACE;
            RestAssured.basePath = WORKSPACE_PATH;
            final InputStream zipInputStreamSipObject =
                PropertiesUtils.getResourceAsStream(SIP_FILE_OK_NAME);

            // init default logbook operation
            final List<LogbookOperationParameters> params = new ArrayList<>();
            final LogbookOperationParameters initParameters = LogbookParametersFactory.newLogbookOperationParameters(
                operationGuid, "Process_SIP_unitary", operationGuid,
                LogbookTypeProcess.INGEST, StatusCode.STARTED,
                operationGuid != null ? operationGuid.toString() : "outcomeDetailMessage",
                operationGuid);
            params.add(initParameters);
            LOGGER.error(initParameters.toString());

            // call ingest
            IngestInternalClientFactory.getInstance().changeServerPort(PORT_SERVICE_INGEST_INTERNAL);
            final IngestInternalClient client = IngestInternalClientFactory.getInstance().getClient();
            final Response response2 = client.uploadInitialLogbook(params);
            assertEquals(response2.getStatus(), Status.CREATED.getStatusCode());

            // init workflow before execution
            client.initWorkFlow("DEFAULT_WORKFLOW_RESUME");
            client.upload(zipInputStreamSipObject, CommonMediaType.ZIP_TYPE, CONTEXT_ID);

            wait(operationGuid.toString());

            ProcessWorkflow processWorkflow =
                ProcessMonitoringImpl.getInstance().findOneProcessWorkflow(operationGuid.toString(), tenantId);

            assertNotNull(processWorkflow);
            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
            assertEquals(StatusCode.OK, processWorkflow.getStatus());

            SelectMultiQuery select = new SelectMultiQuery();
            select.addQueries(QueryHelper.match("Title", "Sensibilisation API"));
            // Get AU
            final AccessInternalClient accessClient = AccessInternalClientFactory.getInstance().getClient();
            RequestResponse<JsonNode> response = accessClient.selectUnits(select.getFinalSelect());
            assertTrue(response.isOk());

            // Get GOT
            LOGGER.warn(response.toString());
            final JsonNode node = response.toJsonNode().get("$results").get(0);
            LOGGER.warn(node.toString());
            final String unitId = node.get("#object").asText();


            SelectMultiQuery select2 = new SelectMultiQuery();
            response = accessClient.selectObjectbyId(select2.getFinalSelect(), unitId);
            assertTrue(response.isOk());

            // Get logbook
            SelectMultiQuery select3 = new SelectMultiQuery();
            select.addQueries(QueryHelper.eq("evType", "Process_SIP_unitary"));
            response = accessClient.selectOperation(select3.getFinalSelect());
            assertTrue(response.isOk());


            final GUID operationGuid2 = GUIDFactory.newOperationLogbookGUID(tenantId);
            final InputStream zipInputStreamSipObject2 =
                PropertiesUtils.getResourceAsStream(SIP_TREE);

            // init default logbook operation
            final List<LogbookOperationParameters> params2 = new ArrayList<>();
            final LogbookOperationParameters initParameters2 = LogbookParametersFactory.newLogbookOperationParameters(
                operationGuid2, "Process_SIP_unitary", operationGuid2,
                LogbookTypeProcess.MASTERDATA, StatusCode.STARTED,
                operationGuid2 != null ? operationGuid2.toString() : "outcomeDetailMessage",
                operationGuid2);
            params2.add(initParameters2);

            final IngestInternalClient client2 = IngestInternalClientFactory.getInstance().getClient();
            final Response response3 = client2.uploadInitialLogbook(params2);
            assertEquals(response3.getStatus(), Status.CREATED.getStatusCode());

            // init workflow before execution
            client2.initWorkFlow("HOLDING_SCHEME_RESUME");
            client2.upload(zipInputStreamSipObject2, CommonMediaType.ZIP_TYPE, "HOLDING_SCHEME_RESUME");

            VitamThreadUtils.getVitamSession().setContractId("aName4");

            wait(operationGuid.toString());

            processWorkflow =
                ProcessMonitoringImpl.getInstance().findOneProcessWorkflow(operationGuid.toString(), tenantId);

            assertNotNull(processWorkflow);
            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
            assertEquals(StatusCode.OK, processWorkflow.getStatus());

            SelectMultiQuery selectTree = new SelectMultiQuery();
            selectTree.addQueries(QueryHelper.eq("Title", "testArbre2").setDepthLimit(5));
            // Get AU
            RequestResponse<JsonNode> responseTree = accessClient.selectUnits(selectTree.getFinalSelect());
            assertTrue(responseTree.isOk());
            assertEquals(responseTree.toJsonNode().get("$hits").get("total").asInt(), 1);
        } catch (final Exception e) {
            LOGGER.error(e);
            try (LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient()) {
                fr.gouv.vitam.common.database.builder.request.single.Select selectQuery =
                    new fr.gouv.vitam.common.database.builder.request.single.Select();
                selectQuery.setQuery(QueryHelper.eq("evIdProc", operationGuid.getId()));
                JsonNode logbookResult = logbookClient.selectOperation(selectQuery.getFinalSelect());
                LOGGER.error(JsonHandler.prettyPrint(logbookResult));
            }
            fail("should not raized an exception");
        }
    }

    @Test
    @RunWithCustomExecutor
    public void shouldImportRulesFile() {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        try {
            FileInputStream stream = new FileInputStream(PropertiesUtils.findFile(FILE_RULES_OK));
            AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient();
            final Status status = client.importRulesFile(stream, FILE_RULES_OK);
            ResponseBuilder ResponseBuilder = Response.status(status);
            Response response = ResponseBuilder.build();
            assertEquals(response.getStatus(), Status.CREATED.getStatusCode());
        } catch (final DatabaseConflictException e) {
            LOGGER.error(e);
            fail(String.format("DatabaseConflictException %s", e.getCause()));
        } catch (final FileRulesImportInProgressException e) {
            LOGGER.error(e);
            fail(String.format("FileRulesImportInProgressException %s", e.getCause()));
        } catch (final ReferentialException e) {
            LOGGER.error(e);
            fail(String.format("ReferentialException %s", e.getCause()));
        } catch (FileNotFoundException e) {
            LOGGER.error(e);
            fail(String.format("FileNotFoundException %s", e.getCause()));
        }
    }

    @Test
    @RunWithCustomExecutor
    public void shouldRetrieveReportWhenCheckFileRules() throws WebApplicationException, IOException {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        FileInputStream stream = new FileInputStream(PropertiesUtils.findFile(FILE_RULES_KO_DUPLICATED_REFERENCE));
        FileInputStream streamErrorReport = new FileInputStream(PropertiesUtils.findFile(ERROR_REPORT_CONTENT));
        checkFileRulesWithCustomReferential(stream, streamErrorReport);
    }


    @Test
    @RunWithCustomExecutor
    public void shouldRetrieveReportWhencheckFileRulesError6000Day() throws WebApplicationException, IOException {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        FileInputStream stream = new FileInputStream(PropertiesUtils.findFile(FILE_RULES_KO_600000_DAY));
        FileInputStream streamErrorReport = new FileInputStream(PropertiesUtils.findFile(ERROR_REPORT_6000_DAYS));
        checkFileRulesWithCustomReferential(stream, streamErrorReport);
    }

    @Test
    @RunWithCustomExecutor
    public void shouldRetrieveReportWhencheckFileRulesError9000Years() throws WebApplicationException, IOException {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        FileInputStream stream = new FileInputStream(PropertiesUtils.findFile(FILE_RULES_KO_90000_YEAR));
        FileInputStream streamErrorReport = new FileInputStream(PropertiesUtils.findFile(ERROR_REPORT_9000_YEARS));
        checkFileRulesWithCustomReferential(stream, streamErrorReport);
    }

    @Test
    @RunWithCustomExecutor
    public void shouldRetrieveReportWhenCheckFileRulesErrorAnarchyRules() throws WebApplicationException, IOException {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        FileInputStream stream = new FileInputStream(PropertiesUtils.findFile(FILE_RULES_KO_ANARCHY_RULE));
        FileInputStream streamErrorReport =
            new FileInputStream(PropertiesUtils.findFile(ERROR_REPORT_ANARCHY_RULE));
        checkFileRulesWithCustomReferential(stream, streamErrorReport);
    }

    @Test
    @RunWithCustomExecutor
    public void shouldRetrieveReportWhenCheckFileRulesDecadeMeasurement() throws WebApplicationException, IOException {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        FileInputStream stream = new FileInputStream(PropertiesUtils.findFile(FILE_RULES_KO_DECADE_MEASURE));
        FileInputStream streamErrorReport =
            new FileInputStream(PropertiesUtils.findFile(ERROR_REPORT_DECADE_MEASURE));
        checkFileRulesWithCustomReferential(stream, streamErrorReport);
    }

    @Test
    @RunWithCustomExecutor
    public void shouldRetrieveReportWhenCheckFileRulesErrorNegativeDuration()
        throws WebApplicationException, IOException {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        FileInputStream stream = new FileInputStream(PropertiesUtils.findFile(FILE_RULES_KO_NEGATIVE_DURATION));
        FileInputStream streamErrorReport =
            new FileInputStream(PropertiesUtils.findFile(ERROR_REPORT_NEGATIVE_DURATION));
        checkFileRulesWithCustomReferential(stream, streamErrorReport);
    }

    @Test
    @RunWithCustomExecutor
    public void shouldRetrieveReportWhenCheckFileRulesErrorWrongComa() throws WebApplicationException, IOException {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        FileInputStream stream =
            new FileInputStream(PropertiesUtils.findFile(FILE_RULES_KO_REFERENCE_WITH_WRONG_COMMA));
        FileInputStream streamErrorReport =
            new FileInputStream(PropertiesUtils.findFile(ERROR_REPORT_REFERENCE_WITH_WRONG_COMA));
        checkFileRulesWithCustomReferential(stream, streamErrorReport);
    }

    @Test
    @RunWithCustomExecutor
    public void shouldRetrieveReportWhenCheckFileRulesErrorUnknowDuration()
        throws WebApplicationException, IOException {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        final FileInputStream stream =
            new FileInputStream(PropertiesUtils.findFile(FILE_RULES_KO_UNKNOWN_DURATION));
        final FileInputStream expectedStreamErrorReport =
            new FileInputStream(PropertiesUtils.findFile(ERROR_REPORT_UNKNOW_DURATION));
        checkFileRulesWithCustomReferential(stream, expectedStreamErrorReport);
    }

    /**
     * Check error report 
     * 
     * @param fileInputStreamToImport the given FileInputStream
     * @param expectedStreamErrorReport expected Stream error report
     */
    private void checkFileRulesWithCustomReferential(final FileInputStream fileInputStreamToImport,
        final FileInputStream expectedStreamErrorReport) {
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient();) {
            final Response response = client.checkRulesFile(fileInputStreamToImport);
            final String readEntity = response.readEntity(String.class);
            final JsonNode responseEntityNode = JsonHandler.getFromString(readEntity);
            final JsonNode responseError = responseEntityNode.get("error");
            final JsonNode expectedNode = JsonHandler.getFromInputStream(expectedStreamErrorReport);
            final JsonNode expectedError = expectedNode.get("error");
            assertEquals(expectedError, responseError);
            assertEquals(response.getStatus(), Status.BAD_REQUEST.getStatusCode());
        } catch (final ReferentialException e) {
            fail(String.format("ReferentialException %s", e.getCause()));
        } catch (InvalidParseOperationException e) {
            fail(String.format("InvalidParseOperationException %s", e.getCause()));
        }
    }
}
