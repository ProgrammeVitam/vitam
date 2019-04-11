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
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static fr.gouv.vitam.common.guid.GUIDFactory.newOperationLogbookGUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import com.mongodb.util.JSON;
import fr.gouv.vitam.common.client.IngestCollection;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.guid.GUIDReader;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.Contexts;
import org.apache.commons.io.FileUtils;
import org.bson.Document;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.restassured.RestAssured;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import fr.gouv.vitam.access.internal.client.AccessInternalClient;
import fr.gouv.vitam.access.internal.client.AccessInternalClientFactory;
import fr.gouv.vitam.access.internal.common.exception.AccessInternalClientNotFoundException;
import fr.gouv.vitam.access.internal.rest.AccessInternalMain;
import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.client.configuration.ClientConfigurationImpl;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.action.SetAction;
import fr.gouv.vitam.common.database.builder.query.action.UnsetAction;
import fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.multiple.UpdateMultiQuery;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.builder.request.single.Update;
import fr.gouv.vitam.common.database.parser.request.adapter.SingleVarNameAdapter;
import fr.gouv.vitam.common.database.parser.request.single.SelectParserSingle;
import fr.gouv.vitam.common.database.parser.request.single.UpdateParserSingle;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InternalServerException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.format.identification.FormatIdentifierFactory;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.junit.JunitHelper.ElasticsearchTestConfiguration;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.model.administration.ArchiveUnitProfileModel;
import fr.gouv.vitam.common.model.administration.ContextModel;
import fr.gouv.vitam.common.model.administration.IngestContractModel;
import fr.gouv.vitam.common.model.administration.SecurityProfileModel;
import fr.gouv.vitam.common.storage.StorageConfiguration;
import fr.gouv.vitam.common.stream.SizedInputStream;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClient;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClientFactory;
import fr.gouv.vitam.ingest.internal.upload.rest.IngestInternalMain;
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
import fr.gouv.vitam.metadata.core.database.collections.Unit;
import fr.gouv.vitam.metadata.rest.MetadataMain;
import fr.gouv.vitam.processing.common.model.ProcessWorkflow;
import fr.gouv.vitam.processing.data.core.ProcessDataAccessImpl;
import fr.gouv.vitam.processing.engine.core.monitoring.ProcessMonitoringImpl;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.processing.management.rest.ProcessManagementMain;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.worker.server.rest.WorkerMain;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;

/**
 * Ingest Internal integration test
 */
public class IngestInternalIT {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IngestInternalIT.class);
    private static final int DATABASE_PORT = 12346;
    private static final String LINE_3 = "line 3";
    private static final String LINE_2 = "line 2";
    private static final String MONGO_DB_NAME = "Vitam";
    private static final String JEU_DONNEES_OK_REGLES_CSV_CSV = "jeu_donnees_OK_regles_CSV.csv";
    private static MongodExecutable mongodExecutable;
    static MongodProcess mongod;
    private static LogbookElasticsearchAccess esClient;
    private static final Integer tenantId = 0;
    private static final String contractId = "aName3";
    private static String DATA_MIGRATION = "DATA_MIGRATION";

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @ClassRule
    public static TemporaryFolder temporaryFolder = new TemporaryFolder();


    private static final long SLEEP_TIME = 100l;
    private static final long NB_TRY = 9600; // equivalent to 16 minute

    private static boolean imported = false;
    private final static String CLUSTER_NAME = "vitam-cluster";
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
    private static final String CONTEXT_ID_NEXT = "DEFAULT_WORKFLOW_NEXT";


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
    private static WorkerMain wkrapplication;
    private static AdminManagementMain adminApplication;
    private static LogbookMain logbookApplication;
    private static WorkspaceMain workspaceMain;
    private static ProcessManagementMain processManagementMain;
    private static AccessInternalMain accessInternalApplication;
    private static IngestInternalMain ingestInternalApplication;

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
    private static final String FILE_AGENCIES_OK = "functional-admin/agencies/agencies.csv";
    private static final String FILE_AGENCIES_AU_update = "functional-admin/agencies/agencies_update.csv";

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
    private static String SIP_KO_PHYSICAL_ARCHIVE_BINARY_IN_PHYSICAL =
        "integration-ingest-internal/KO_ArchivesPhysiques_BinaryInPhysical.zip";
    private static String SIP_KO_PHYSICAL_ARCHIVE_PHYSICAL_IN_BINARY =
        "integration-ingest-internal/KO_ArchivesPhysiques_PhysicalInBinary.zip";
    private static String SIP_KO_PHYSICAL_ARCHIVE_PHYSICAL_ID_EMPTY =
        "integration-ingest-internal/KO_ArchivesPhysiques_EmptyPhysicalId.zip";

    private static String SIP_OK_PHYSICAL_ARCHIVE_WITH_ATTACHMENT_FROM_CONTARCT = "integration-ingest-internal/" +
        "OK_ArchivesPhysiques_With_Attachment_Contract.zip";
    private static String SIP_ARBRE = "integration-ingest-internal/arbre_simple.zip";

    private static String SIP_4396 = "integration-ingest-internal/OK_SIP_ClassificationRule_noRuleID.zip";

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

        // ES client
        final List<ElasticsearchNode> esNodes = new ArrayList<>();
        esNodes.add(new ElasticsearchNode("localhost", config.getTcpPort()));
        esClient = new LogbookElasticsearchAccess(CLUSTER_NAME, esNodes);


        // launch metadata
        SystemPropertyUtil.set(MetadataMain.PARAMETER_JETTY_SERVER_PORT,
            Integer.toString(PORT_SERVICE_METADATA));
        medtadataApplication = new MetadataMain(CONFIG_METADATA_PATH);
        medtadataApplication.start();
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
        processManagementMain = new ProcessManagementMain(CONFIG_PROCESSING_PATH);
        processManagementMain.start();
        SystemPropertyUtil.clear(ProcessManagementMain.PARAMETER_JETTY_SERVER_PORT);

        // launch worker
        SystemPropertyUtil.set("jetty.worker.port", Integer.toString(PORT_SERVICE_WORKER));
        wkrapplication = new WorkerMain(CONFIG_WORKER_PATH);
        wkrapplication.start();
        SystemPropertyUtil.clear("jetty.worker.port");

        FormatIdentifierFactory.getInstance().changeConfigurationFile(CONFIG_SIEGFRIED_PATH);

        // launch ingest-internal
        SystemPropertyUtil.set("jetty.ingest-internal.port", Integer.toString(PORT_SERVICE_INGEST_INTERNAL));
        ingestInternalApplication = new IngestInternalMain(CONFIG_INGEST_INTERNAL_PATH);
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
        if (esClient != null) {
            esClient.close();
        }
        if (config != null) {
            JunitHelper.stopElasticsearchForTest(config);
        }
        if (mongod != null) {
            mongod.stop();
        }
        if (mongodExecutable != null) {
            mongodExecutable.stop();
        }
        if (ingestInternalApplication != null) {
            ingestInternalApplication.stop();
        }
        if (workspaceMain != null) {
            workspaceMain.stop();
        }
        if (wkrapplication != null) {
            wkrapplication.stop();
        }
        if (logbookApplication != null) {
            logbookApplication.stop();
        }
        if (processManagementMain != null) {
            processManagementMain.stop();
        }
        if (medtadataApplication != null) {
            medtadataApplication.stop();
        }
        if (adminApplication != null) {
            adminApplication.stop();
        }
        if (accessInternalApplication != null) {
            accessInternalApplication.stop();
        }
    }

    @Before
    public void setUp() throws Exception {
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(0));
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
        VitamThreadUtils.getVitamSession().setContextId("Context_IT");

        if (!imported) {
            try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
                VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(tenantId));
                client.importFormat(
                    PropertiesUtils.getResourceAsStream("integration-ingest-internal/DROID_SignatureFile_V88.xml"),
                    "DROID_SignatureFile_V88.xml");

                // Import Rules
                VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(tenantId));
                client.importRulesFile(
                    PropertiesUtils.getResourceAsStream("integration-ingest-internal/MGT_RULES_REF.csv"),
                    "MGT_RULES_REF.csv");

                // import service agent
                try {
                    VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(tenantId));
                    client.importAgenciesFile(PropertiesUtils.getResourceAsStream(FILE_AGENCIES_OK), FILE_AGENCIES_OK);

                } catch (Exception e) {

                }

                // import contract
                VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(tenantId));
                File fileContracts =
                    PropertiesUtils.getResourceFile("integration-ingest-internal/referential_contracts_ok.json");
                VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(tenantId));
                List<IngestContractModel> IngestContractModelList = JsonHandler.getFromFileAsTypeRefence(fileContracts,
                    new TypeReference<List<IngestContractModel>>() {});

                VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(tenantId));
                client.importIngestContracts(IngestContractModelList);

                // import contrat
                VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(tenantId));
                File fileAccessContracts = PropertiesUtils.getResourceFile("access_contrats.json");
                List<AccessContractModel> accessContractModelList = JsonHandler
                    .getFromFileAsTypeRefence(fileAccessContracts, new TypeReference<List<AccessContractModel>>() {});
                VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(tenantId));
                client.importAccessContracts(accessContractModelList);

                // Import Security Profile
                VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(tenantId));
                client.importSecurityProfiles(JsonHandler
                    .getFromFileAsTypeRefence(
                        PropertiesUtils.getResourceFile("integration-ingest-internal/security_profile_ok.json"),
                        new TypeReference<List<SecurityProfileModel>>() {}));

                // Import Context
                VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(tenantId));
                client.importContexts(JsonHandler
                    .getFromFileAsTypeRefence(
                        PropertiesUtils.getResourceFile("integration-ingest-internal/contexts.json"),
                        new TypeReference<List<ContextModel>>() {}));

                // Import Archive Unit Profile
                VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(tenantId));
                client.createArchiveUnitProfiles(JsonHandler
                    .getFromFileAsTypeRefence(
                        PropertiesUtils.getResourceFile("integration-ingest-internal/archive-unit-profile.json"),
                        new TypeReference<List<ArchiveUnitProfileModel>>() {}));
            } catch (final Exception e) {
                LOGGER.error(e);
            }
            imported = true;
        }
    }

    @RunWithCustomExecutor
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

        RestAssured.port = PORT_SERVICE_INGEST_INTERNAL;
        RestAssured.basePath = INGEST_INTERNAL_PATH;
        get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());

        RestAssured.port = PORT_SERVICE_ACCESS_INTERNAL;
        RestAssured.basePath = ACCESS_INTERNAL_PATH;
        get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());
    }

    /**
     * To check unit tree (ancestors _up, _us and _uds)
     *
     * @param unit the unit to check
     * @param metadataClient the metadataclient
     * @param upIds the wanted up ids list
     * @param usIds the wanted us ids list
     * @param udsIds the wanted uds ids / depth map
     * @throws Exception
     */
    private void checkUnitTree(JsonNode unit, MetaDataClient metadataClient, List<String> upIds, List<String> usIds,
        Map<String, Integer> udsIds)
        throws Exception {
        SelectMultiQuery query = new SelectMultiQuery();
        query.setProjection(JsonHandler.getFromString("{\"$fields\": {\"Title\": 1}}"));
        // _up / # up
        final JsonNode up = unit.get("#unitups");
        assertNotNull(up);
        assertEquals(upIds.size(), up.size());
        // Check ids
        JsonNode result;
        for (int i = 0; i < up.size(); i++) {
            result = metadataClient.selectUnitbyId(query.getFinalSelectById(), up.get(i).asText());
            assertNotNull(result);
            assertNotNull(result.get("$results"));
            assertEquals(1, result.get("$results").size());
            assertNotNull(result.get("$results").get(0).get("Title"));
            assertTrue(upIds.remove(result.get("$results").get(0).get("Title").asText()));
        }
        // _us / #us
        final JsonNode us = unit.get("#allunitups");
        assertNotNull(us);
        assertEquals(usIds.size(), us.size());
        // Check ids
        for (int i = 0; i < us.size(); i++) {
            result = metadataClient.selectUnitbyId(query.getFinalSelectById(), us.get(i).asText());
            assertNotNull(result);
            assertNotNull(result.get("$results"));
            assertEquals(1, result.get("$results").size());
            assertNotNull(result.get("$results").get(0).get("Title"));
            assertTrue(usIds.remove(result.get("$results").get(0).get("Title").asText()));
        }

        // _uds / #uds
        assertThat(unit.get("#uds")).isNull();
        assertThat(unit.get(Unit.UNITDEPTHS)).isNull();

        try (MongoClient mongoClient = new MongoClient(new ServerAddress("localhost", DATABASE_PORT))) {
            MongoCollection<Document> collection =
                mongoClient.getDatabase(MONGO_DB_NAME).getCollection(Unit.class.getSimpleName());

            FindIterable<Document> documents =
                collection.find(com.mongodb.client.model.Filters.eq("_id", unit.get("#id").asText()));
            Document first = documents.iterator().next();

            final JsonNode uds = JsonHandler.getFromString(JSON.serialize(first)).get(Unit.UNITDEPTHS);

            assertNotNull(uds);
            assertEquals(udsIds.size(), uds.size());
            // Check ids and depth
            String fieldName;
            Iterator fieldNames = uds.fieldNames();
            while (fieldNames.hasNext()) {
                fieldName = (String) fieldNames.next();
                result = metadataClient.selectUnitbyId(query.getFinalSelectById(), fieldName);
                assertNotNull(result);
                assertEquals(1, result.get("$results").size());
                assertNotNull(result.get("$results").get(0).get("Title"));
                assertNotNull(udsIds.get(result.get("$results").get(0).get("Title").asText()));
                assertEquals(udsIds.get(result.get("$results").get(0).get("Title").asText()).intValue(),
                    uds.get(fieldName)
                        .asInt());
            }
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testIngestInternal() throws Exception {
        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        try {
            VitamThreadUtils.getVitamSession().setTenantId(tenantId);
            tryImportFile();
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);

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
            client.uploadInitialLogbook(params);

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
            boolean needAuthorization = unit.get("#management").get("NeedAuthorization").asBoolean();
            assertTrue(needAuthorization);
            // Try to check OG
            select = new SelectMultiQuery();
            select.addRoots(og);
            select.setProjectionSliceOnQualifier();
            final JsonNode jsonResponse = metadataClient.selectObjectGrouptbyId(select.getFinalSelect(), og);
            LOGGER.warn("Result: " + jsonResponse);
            final List<String> valuesAsText = jsonResponse.get("$results").findValuesAsText("#id");
            final String objectId = valuesAsText.get(0);
            final StorageClient storageClient = StorageClientFactory.getInstance().getClient();
            Response responseStorage = storageClient.getContainerAsync("default", objectId,
                DataCategory.OBJECT);
            InputStream inputStream = responseStorage.readEntity(InputStream.class);
            SizedInputStream sizedInputStream = new SizedInputStream(inputStream);
            final long size = StreamUtils.closeSilently(sizedInputStream);
            LOGGER.warn("read: " + size);

            assertTrue(size > 1000);

            final AccessInternalClient accessClient = AccessInternalClientFactory.getInstance().getClient();
            responseStorage = accessClient.getObject(og, "BinaryMaster", 1);
            inputStream = responseStorage.readEntity(InputStream.class);

            // get initial lfc version
            String unitId = unit.findValuesAsText("#id").get(0);
            assertEquals(5, checkAndRetrieveLfcVersionForUnit(unitId, accessClient));

            // lets find details for the unit -> AccessRule should have been set
            RequestResponseOK<JsonNode> responseUnitBeforeUpdate =
                (RequestResponseOK) accessClient.selectUnitbyId(new SelectMultiQuery().getFinalSelect(), unitId);
            assertNotNull(responseUnitBeforeUpdate.getFirstResult().get("#management").get("AccessRule"));

            // execute update -> rules to be 'unset'
            Map<String, JsonNode> action = new HashMap<>();
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(tenantId));
            action.put("#management.AccessRule.Rules", JsonHandler.createArrayNode());
            UpdateMultiQuery updateQuery = new UpdateMultiQuery().addActions(new SetAction(action));
            updateQuery.addRoots(unitId);
            RequestResponse response = accessClient
                .updateUnitbyId(updateQuery.getFinalUpdate(), unitId);
            assertEquals(response.toJsonNode().get("$hits").get("size").asInt(), 1);

            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(tenantId));
            // execute update -> rules to be 'unset'
            UpdateMultiQuery updateQuery2 =
                new UpdateMultiQuery().addActions(new SetAction("ArchiveUnitProfile", "ArchiveUnitProfile"));
            updateQuery.addRoots(unitId);
            RequestResponse response3 = accessClient
                .updateUnitbyId(updateQuery2.getFinalUpdate(), unitId);
            assertTrue(!response3.isOk());

            // lets find details for the unit -> AccessRule should have been unset
            RequestResponseOK<JsonNode> responseUnitAfterUpdate =
                (RequestResponseOK) accessClient.selectUnitbyId(new SelectMultiQuery().getFinalSelect(), unitId);

            // check version incremented in lfc
            assertEquals(6, checkAndRetrieveLfcVersionForUnit(unitId, accessClient));
            assertEquals(responseUnitBeforeUpdate.getFirstResult().get("#opi"),
                responseUnitAfterUpdate.getFirstResult().get("#opi"));

            // execute update -> classification rules without classification owner
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(tenantId));

            UpdateMultiQuery updateQueryClassification = new UpdateMultiQuery()
                .addActions(new UnsetAction("#management.ClassificationRule.ClassificationOwner"));
            updateQueryClassification.addRoots(unitId);
            RequestResponse responseClassification = accessClient
                .updateUnitbyId(updateQueryClassification.getFinalUpdate(), unitId);
            assertTrue(!responseClassification.isOk());
            assertEquals(responseClassification.getHttpCode(), Status.BAD_REQUEST.getStatusCode());

            // execute update -> PreventInheritance
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(tenantId));

            String queryUpdate =
                "{\"$roots\":[\"" + unitId + "\"],\"$query\":[],\"$filter\":{}," +
                    "\"$action\":[{\"$set\": {\"#management.AccessRule.Inheritance\" : {\"PreventRulesId\": [\"ACC-00004\",\"ACC-00005\"], \"PreventInheritance\": false}}}]}";
            RequestResponse responsePreventInheritance = accessClient
                .updateUnitbyId(JsonHandler.getFromString(queryUpdate), unitId);
            assertTrue(responsePreventInheritance.isOk());
            assertEquals(responsePreventInheritance.getHttpCode(), Status.OK.getStatusCode());

            // lets find details for the unit -> AccessRule should have been set
            RequestResponseOK<JsonNode> responseUnitAfterUpdatePreventInheritance =
                (RequestResponseOK) accessClient.selectUnitbyId(new SelectMultiQuery().getFinalSelect(), unitId);
            assertEquals(2, responseUnitAfterUpdatePreventInheritance.getFirstResult().get("#management").get("AccessRule").get("Inheritance").get("PreventRulesId").size());
            assertEquals(false, responseUnitAfterUpdatePreventInheritance.getFirstResult().get("#management").get("AccessRule").get("Inheritance").get("PreventInheritance").asBoolean());

            sizedInputStream = new SizedInputStream(inputStream);
            final long size2 = StreamUtils.closeSilently(sizedInputStream);
            LOGGER.warn("read: " + size2);
            assertTrue(size2 == size);

            JsonNode logbookOperation =
                accessClient.selectOperationById(operationGuid.getId(), new SelectMultiQuery().getFinalSelect())
                    .toJsonNode();

            Set<String> eventIds = new HashSet<>();
            eventIds.add(logbookOperation.get("$results").get(0).get("evId").asText());
            logbookOperation.get("$results").get(0).get("events").forEach(event -> {
                if (event.get("evType").asText().contains("STP_UPLOAD_SIP")) {
                    assertThat(event.get("outDetail").asText()).contains("STP_UPLOAD_SIP");
                }
                eventIds.add(event.get("evId").asText());
            });

            // check evIds
            assertThat(eventIds.size()).isEqualTo(logbookOperation.get("$results").get(0).get("events").size() + 1);

            QueryBuilder query = QueryBuilders.matchQuery("_id", operationGuid.getId());
            SearchResponse elasticSearchResponse =
                esClient.search(LogbookCollections.OPERATION, tenantId, query, null, null, 0, 25);
            assertEquals(1, elasticSearchResponse.getHits().getTotalHits());
            assertNotNull(elasticSearchResponse.getHits().getAt(0));
            SearchHit hit = elasticSearchResponse.getHits().iterator().next();
            assertNotNull(hit);
            // TODO compare

            // lets try to update a unit that does not exist, an AccessInternalClientNotFoundException will be thrown
            try {
                VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(tenantId));
                response = accessClient.updateUnitbyId(new UpdateMultiQuery().getFinalUpdate(),
                    "aedqaaaaacfscicjabgwoak7xpw5pwyaaaaq");
                fail("should raized an exception");
            } catch (AccessInternalClientNotFoundException ex) {
                LOGGER.error(ex + " | " + response.toString());
            }
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

            throw e;
        }
    }

    private int checkAndRetrieveLfcVersionForUnit(String unitId, AccessInternalClient accessClient) throws Exception {
        return retrieveLfcForUnit(unitId, accessClient).get(LogbookDocument.VERSION).asInt();
    }

    private JsonNode retrieveLfcForUnit(String unitId, AccessInternalClient accessClient) throws Exception {
        final SelectParserSingle parser = new SelectParserSingle();
        Select selectLFC = new Select();
        parser.parse(selectLFC.getFinalSelect());
        parser.addCondition(QueryHelper.eq("obId", unitId));
        ObjectNode queryDsl = parser.getRequest().getFinalSelect();

        JsonNode lfcResponse = accessClient.selectUnitLifeCycleById(unitId, queryDsl).toJsonNode();
        final JsonNode result = lfcResponse.get("$results");
        assertNotNull(result);
        final JsonNode lfc = result.get(0);
        assertNotNull(lfc);

        return lfc;
    }


    @RunWithCustomExecutor
    @Test
    public void testPhysicalArchiveIngestInternal() throws Exception {
        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        try {
            VitamThreadUtils.getVitamSession().setTenantId(tenantId);
            tryImportFile();
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
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
            client.uploadInitialLogbook(params);

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
            final List<String> valuesAsText = jsonResponse.get("$results").findValuesAsText("#id");
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
            throw e;
        }
    }

    @Test
    @RunWithCustomExecutor
    public void should_download_csv_referential() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);

        FileInputStream stream = new FileInputStream(PropertiesUtils.findFile(FILE_AGENCIES_OK));
        String operationId = null;
        AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient();
        Status status = client.importAgenciesFile(stream, FILE_AGENCIES_OK);
        ResponseBuilder ResponseBuilder = Response.status(status);
        Response response = ResponseBuilder.build();
        assertEquals(response.getStatus(), Status.CREATED.getStatusCode());


        Select select = new Select();
        LogbookOperationsClient operationsClient = LogbookOperationsClientFactory.getInstance().getClient();
        select.setLimitFilter(0, 1);
        select.addOrderByDescFilter(LogbookMongoDbName.eventDateTime.getDbname());
        select.setQuery(eq(LogbookMongoDbName.eventType.getDbname(),
            "IMPORT_AGENCIES"));

        JsonNode logbookResult = operationsClient.selectOperation(select.getFinalSelect());
        assertThat(logbookResult).isNotNull();
        operationId = logbookResult.get("$results").get(0).get("evId").asText();


        // When
        IngestInternalClient ingestInternalClient = IngestInternalClientFactory.getInstance().getClient();
        Response responseInputStream =
            ingestInternalClient.downloadObjectAsync(operationId, IngestCollection.REFERENTIAL_AGENCIES_CSV);
        // Then
        assertThat(responseInputStream.getStatus()).isEqualTo(Status.OK.getStatusCode());
        InputStream inputStream = responseInputStream.readEntity(InputStream.class);
        File fileResponse = getFile(operationId, inputStream);
        HashSet<String> f2 = new HashSet<>(FileUtils.readLines(fileResponse, StandardCharsets.UTF_8));
        // Can't assert that the result it's the same that the input because storage is mock.
        assertThat(fileResponse).isNotNull();
        // test mock result
        assertThat(f2.size()).isEqualTo(1);

    }

    private File getFile(String operationId, InputStream inputStream) throws VitamClientException {
        File file = null;
        if (inputStream != null) {
            file = PropertiesUtils.fileFromTmpFolder(operationId + ".csv");
            try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                StreamUtils.copy(inputStream, fileOutputStream);
            } catch (IOException e) {
                throw new VitamClientException("Error during Report generation");
            }
        }
        return file;
    }

    @RunWithCustomExecutor
    @Test
    public void testPhysicalArchiveIngestInternalWithAttachmentFomContract() throws Exception {
        // prepare contract
        String linkParentId = null;
        // do ingest of a tree and get an AU UUID
        linkParentId = doIngestOfTreeAndGetOneParentAU();
        assertNotNull(linkParentId);

        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(tenantId));
        // find and update ingestContract
        updateIngestContractLinkParentId("ContractWithAttachment", linkParentId);

        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(tenantId));
        // do the ingest
        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        try {
            VitamThreadUtils.getVitamSession().setTenantId(tenantId);
            tryImportFile();
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            // workspace client dezip SIP in workspace
            RestAssured.port = PORT_SERVICE_WORKSPACE;
            RestAssured.basePath = WORKSPACE_PATH;
            final InputStream zipInputStreamSipObject =
                PropertiesUtils.getResourceAsStream(SIP_OK_PHYSICAL_ARCHIVE_WITH_ATTACHMENT_FROM_CONTARCT);

            // init default logbook operation
            final List<LogbookOperationParameters> params = new ArrayList<>();
            final LogbookOperationParameters initParameters = LogbookParametersFactory.newLogbookOperationParameters(
                operationGuid, "Process_SIP_unitary", operationGuid,
                LogbookTypeProcess.INGEST, StatusCode.STARTED,
                operationGuid != null ? operationGuid.toString() : "outcomeDetailMessage",
                operationGuid);
            params.add(initParameters);
            LOGGER.debug(initParameters.toString());

            // call ingest using updated contract
            IngestInternalClientFactory.getInstance().changeServerPort(PORT_SERVICE_INGEST_INTERNAL);
            final IngestInternalClient client = IngestInternalClientFactory.getInstance().getClient();
            client.uploadInitialLogbook(params);

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
            select.addQueries(QueryHelper.eq("Title", "Root AU ATTACHED"));
            final JsonNode node = metadataClient.selectUnits(select.getFinalSelect());
            LOGGER.debug(JsonHandler.prettyPrint(node));
            final JsonNode result = node.get("$results");
            assertNotNull(result);
            final JsonNode unit = result.get(0);
            assertNotNull(unit);

            // get unit lfc
            final AccessInternalClient accessClient = AccessInternalClientFactory.getInstance().getClient();
            JsonNode lfc = retrieveLfcForUnit(unit.get("#id").asText(), accessClient);
            assertNotNull(lfc);
            // check evDetData of checkManifest event
            JsonNode checkManifestEvent = lfc.get(LogbookDocument.EVENTS).get(0);
            assertTrue(checkManifestEvent.get("evType").asText().equals("LFC.CHECK_MANIFEST"));
            assertNotNull(checkManifestEvent.get("_lastPersistedDate"));
            assertTrue(
                checkManifestEvent.get("evDetData").asText().equals("{\n  \"_up\" : [ \"" + linkParentId + "\" ]\n}"));
        } catch (final Exception e) {
            LOGGER.error(e);
            try (LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient()) {
                fr.gouv.vitam.common.database.builder.request.single.Select selectQuery =
                    new fr.gouv.vitam.common.database.builder.request.single.Select();
                selectQuery.setQuery(QueryHelper.eq("evIdProc", operationGuid.getId()));
                JsonNode logbookResult = logbookClient.selectOperation(selectQuery.getFinalSelect());
                LOGGER.error(JsonHandler.prettyPrint(logbookResult));
            }
            throw e;
        }
    }

    private String doIngestOfTreeAndGetOneParentAU() throws Exception {
        try {
            VitamThreadUtils.getVitamSession().setTenantId(tenantId);
            final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
            tryImportFile();
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            // workspace client dezip SIP in workspace
            RestAssured.port = PORT_SERVICE_WORKSPACE;
            RestAssured.basePath = WORKSPACE_PATH;
            final InputStream zipInputStreamSipObject =
                PropertiesUtils.getResourceAsStream(SIP_ARBRE);

            // init default logbook operation
            final List<LogbookOperationParameters> params = new ArrayList<>();
            final LogbookOperationParameters initParameters = LogbookParametersFactory.newLogbookOperationParameters(
                operationGuid, "Process_SIP_unitary", operationGuid,
                LogbookTypeProcess.MASTERDATA, StatusCode.STARTED,
                operationGuid != null ? operationGuid.toString() : "outcomeDetailMessage",
                operationGuid);
            params.add(initParameters);
            LOGGER.error(initParameters.toString());

            // call ingest
            IngestInternalClientFactory.getInstance().changeServerPort(PORT_SERVICE_INGEST_INTERNAL);
            final IngestInternalClient client = IngestInternalClientFactory.getInstance().getClient();
            client.uploadInitialLogbook(params);

            // init workflow before execution
            client.initWorkFlow("HOLDING_SCHEME_RESUME");
            client.upload(zipInputStreamSipObject, CommonMediaType.ZIP_TYPE, "HOLDING_SCHEME_RESUME");

            wait(operationGuid.toString());

            ProcessWorkflow processWorkflow =
                ProcessMonitoringImpl.getInstance().findOneProcessWorkflow(operationGuid.toString(), tenantId);

            assertNotNull(processWorkflow);
            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
            assertEquals(StatusCode.OK, processWorkflow.getStatus());

            // Try to check AU - arborescence and parents stuff, without roots
            final MetaDataClient metadataClient = MetaDataClientFactory.getInstance().getClient();
            SelectMultiQuery select = new SelectMultiQuery();
            select.addQueries(QueryHelper.eq("Title", "Arbre simple"));
            final JsonNode node = metadataClient.selectUnits(select.getFinalSelect());
            LOGGER.debug(JsonHandler.prettyPrint(node));
            final JsonNode result = node.get("$results");
            assertNotNull(result);
            final JsonNode unit = result.get(0);
            assertNotNull(unit);

            return unit.get("#id").asText();
        } catch (final Exception e) {
            e.printStackTrace();
            fail("should not raized an exception");
        }

        return null;
    }

    private void updateIngestContractLinkParentId(String contractId, String linkParentId) throws Exception {
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            final UpdateParserSingle updateParserActive = new UpdateParserSingle(new SingleVarNameAdapter());
            final SetAction setLinkParentId = UpdateActionHelper.set(IngestContractModel.LINK_PARENT_ID, linkParentId);
            final Update updateLinkParent = new Update();
            updateLinkParent.setQuery(QueryHelper.eq("Identifier", contractId));
            updateLinkParent.addActions(setLinkParentId);
            updateParserActive.parse(updateLinkParent.getFinalUpdate());
            JsonNode queryDsl = updateParserActive.getRequest().getFinalUpdate();
            RequestResponse<IngestContractModel> requestResponse = client.updateIngestContract(contractId, queryDsl);
            assertTrue(requestResponse.isOk());
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testPhysicalArchiveWithBinaryMasterInPhysical() throws Exception {
        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        try {
            VitamThreadUtils.getVitamSession().setTenantId(tenantId);
            tryImportFile();
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            // workspace client dezip SIP in workspace
            RestAssured.port = PORT_SERVICE_WORKSPACE;
            RestAssured.basePath = WORKSPACE_PATH;
            final InputStream zipInputStreamSipObject =
                PropertiesUtils.getResourceAsStream(SIP_KO_PHYSICAL_ARCHIVE_BINARY_IN_PHYSICAL);

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
            client.uploadInitialLogbook(params);

            // init workflow before execution
            client.initWorkFlow("DEFAULT_WORKFLOW_RESUME");

            client.upload(zipInputStreamSipObject, CommonMediaType.ZIP_TYPE, CONTEXT_ID);

            wait(operationGuid.toString());

            ProcessWorkflow processWorkflow =
                ProcessMonitoringImpl.getInstance().findOneProcessWorkflow(operationGuid.toString(), tenantId);

            assertNotNull(processWorkflow);
            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
            assertEquals(StatusCode.KO, processWorkflow.getStatus());

        } catch (final Exception e) {
            LOGGER.error(e);
            try (LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient()) {
                fr.gouv.vitam.common.database.builder.request.single.Select selectQuery =
                    new fr.gouv.vitam.common.database.builder.request.single.Select();
                selectQuery.setQuery(QueryHelper.eq("evIdProc", operationGuid.getId()));
                JsonNode logbookResult = logbookClient.selectOperation(selectQuery.getFinalSelect());
                LOGGER.error(JsonHandler.prettyPrint(logbookResult));
            }
            throw e;
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testMigrationAfterIngestOk() throws Exception {
        GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);

        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        tryImportFile();
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);


        operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);

        GUID guid = GUIDReader.getGUID(operationGuid.getId());

        VitamThreadUtils.getVitamSession().setRequestId(guid.getId());

        createOperation(guid, LogbookOperationsClientFactory.getInstance());

        WorkspaceClientFactory.getInstance().getClient().createContainer(guid.getId());

        ProcessingManagementClient processingManagementClient =
            ProcessingManagementClientFactory.getInstance().getClient();
        processingManagementClient.initVitamProcess(Contexts.DATA_MIGRATION.name(), guid.getId(), DATA_MIGRATION);

        RequestResponse<JsonNode> jsonNodeRequestResponse =
            processingManagementClient.executeOperationProcess(guid.getId(), DATA_MIGRATION,
                Contexts.DEFAULT_WORKFLOW.name(), ProcessAction.RESUME.getValue());
        jsonNodeRequestResponse.toResponse();


        wait(operationGuid.toString());

        ProcessWorkflow processWorkflow =
            ProcessMonitoringImpl.getInstance().findOneProcessWorkflow(operationGuid.toString(), tenantId);

        assertNotNull(processWorkflow);
        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
        assertEquals(StatusCode.OK, processWorkflow.getStatus());

    }


    private void createOperation(GUID guid, LogbookOperationsClientFactory logbookOperationsClientFactory)
        throws LogbookClientBadRequestException {

        try (LogbookOperationsClient client = logbookOperationsClientFactory.getClient()) {

            final LogbookOperationParameters initParameter =
                LogbookParametersFactory.newLogbookOperationParameters(
                    guid,
                    "DATA_MIGRATION",
                    guid,
                    LogbookTypeProcess.DATA_MIGRATION,
                    StatusCode.STARTED,
                    VitamLogbookMessages.getLabelOp("DATA_MIGRATION.STARTED") + " : " + guid,
                    guid);
            client.create(initParameter);
        } catch (LogbookClientAlreadyExistsException | LogbookClientServerException e) {
            throw new VitamRuntimeException("Internal server error ", e);
        }
    }


    @RunWithCustomExecutor
    @Test
    public void testPhysicalArchiveWithPhysicalMasterInBinary() throws Exception {
        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        try {
            VitamThreadUtils.getVitamSession().setTenantId(tenantId);
            tryImportFile();
            // workspace client dezip SIP in workspace
            RestAssured.port = PORT_SERVICE_WORKSPACE;
            RestAssured.basePath = WORKSPACE_PATH;
            final InputStream zipInputStreamSipObject =
                PropertiesUtils.getResourceAsStream(SIP_KO_PHYSICAL_ARCHIVE_PHYSICAL_IN_BINARY);

            // init default logbook operation
            final List<LogbookOperationParameters> params = new ArrayList<>();
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
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
            client.uploadInitialLogbook(params);

            // init workflow before execution
            client.initWorkFlow("DEFAULT_WORKFLOW_RESUME");

            client.upload(zipInputStreamSipObject, CommonMediaType.ZIP_TYPE, CONTEXT_ID);

            wait(operationGuid.toString());

            ProcessWorkflow processWorkflow =
                ProcessMonitoringImpl.getInstance().findOneProcessWorkflow(operationGuid.toString(), tenantId);

            assertNotNull(processWorkflow);
            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
            assertEquals(StatusCode.KO, processWorkflow.getStatus());

        } catch (final Exception e) {
            LOGGER.error(e);
            try (LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient()) {
                fr.gouv.vitam.common.database.builder.request.single.Select selectQuery =
                    new fr.gouv.vitam.common.database.builder.request.single.Select();
                selectQuery.setQuery(QueryHelper.eq("evIdProc", operationGuid.getId()));
                JsonNode logbookResult = logbookClient.selectOperation(selectQuery.getFinalSelect());
                LOGGER.error(JsonHandler.prettyPrint(logbookResult));
            }
            throw e;
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testPhysicalArchiveWithEmptyPhysicalId() throws Exception {
        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        try {
            VitamThreadUtils.getVitamSession().setTenantId(tenantId);
            tryImportFile();
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            // workspace client dezip SIP in workspace
            RestAssured.port = PORT_SERVICE_WORKSPACE;
            RestAssured.basePath = WORKSPACE_PATH;
            final InputStream zipInputStreamSipObject =
                PropertiesUtils.getResourceAsStream(SIP_KO_PHYSICAL_ARCHIVE_PHYSICAL_ID_EMPTY);

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
            client.uploadInitialLogbook(params);

            // init workflow before execution
            client.initWorkFlow("DEFAULT_WORKFLOW_RESUME");

            client.upload(zipInputStreamSipObject, CommonMediaType.ZIP_TYPE, CONTEXT_ID);

            wait(operationGuid.toString());

            ProcessWorkflow processWorkflow =
                ProcessMonitoringImpl.getInstance().findOneProcessWorkflow(operationGuid.toString(), tenantId);

            assertNotNull(processWorkflow);
            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
            assertEquals(StatusCode.KO, processWorkflow.getStatus());

        } catch (final Exception e) {
            LOGGER.error(e);
            try (LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient()) {
                fr.gouv.vitam.common.database.builder.request.single.Select selectQuery =
                    new fr.gouv.vitam.common.database.builder.request.single.Select();
                selectQuery.setQuery(QueryHelper.eq("evIdProc", operationGuid.getId()));
                JsonNode logbookResult = logbookClient.selectOperation(selectQuery.getFinalSelect());
                LOGGER.error(JsonHandler.prettyPrint(logbookResult));
            }
            throw e;
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testIngestInternal2182CA1() throws Exception {
        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        tryImportFile();
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
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
        client.uploadInitialLogbook(params);

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
    }


    @RunWithCustomExecutor
    @Test
    public void testIngestInternal1791CA1() throws Exception {
        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        tryImportFile();
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
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
        client.uploadInitialLogbook(params);

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
    }

    @RunWithCustomExecutor
    @Test
    public void testIngestInternal1791CA2() throws Exception {
        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        tryImportFile();
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
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
        client.uploadInitialLogbook(params);

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
    }


    @RunWithCustomExecutor
    @Test
    public void testIngestWithManifestIncorrectObjectNumber() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        tryImportFile();
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
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
        client.uploadInitialLogbook(params);

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
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        tryImportFile();
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        // ProcessDataAccessImpl processData = ProcessDataAccessImpl.getInstance();
        // processData.initProcessWorkflow(ProcessPopulator.populate(WORFKLOW_NAME), operationGuid.getId(),
        // ProcessAction.INIT, LogbookTypeProcess.INGEST, tenantId);
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
        client.uploadInitialLogbook(params);

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
        assertEquals(unit.get("#management").size(), 1);

        // Check that only the rule declared in "ManagementMetaData" was added : StorageRule
        assertTrue(unit.get("#management").has("StorageRule"));
    }

    @RunWithCustomExecutor
    @Test
    public void testIngestWithManifestHavingBothUnitMgtAndMgtMetaDataRules() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);

        tryImportFile();
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);


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
        client.uploadInitialLogbook(params);

        // init workflow before execution
        client.initWorkFlow("DEFAULT_WORKFLOW_RESUME");

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
        assertEquals(unit.get("#management").size(), 2);

        // Check that both the rules declared in "ManagementMetaData" and in the unit were added : StorageRule +
        // AccessRule
        assertTrue(unit.get("#management").has("StorageRule"));
        assertTrue(unit.get("#management").has("AccessRule"));
    }

    // SHDGR__GR_4_H_3__001_0000
    @RunWithCustomExecutor
    @Test
    public void testIngestWithManifestHavingBothUnitMgtAndMgtMetaDataRulesWithoutObjects() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        tryImportFile();
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);

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
        client.uploadInitialLogbook(params);

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
        select.addQueries(QueryHelper.eq("Title", "LEVANT"));
        final JsonNode node = metadataClient.selectUnits(select.getFinalSelect());
        LOGGER.debug(JsonHandler.prettyPrint(node));
        final JsonNode result = node.get("$results");
        assertNotNull(result);
        final JsonNode unit = result.get(0);
        assertNotNull(unit);

        // Check the added management rules
        assertEquals(unit.get("#management").size(), 1);

        // Check that the rule declared in the unit was added : AccessRule
        assertTrue(unit.get("#management").has("AccessRule"));
    }


    @RunWithCustomExecutor
    @Test
    public void testIngestWithAddresseeFieldsInManifest() throws Exception {
        // Now that HTML patterns are refused, this test is now KO
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        tryImportFile();
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
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
        client.uploadInitialLogbook(params);

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
    }


    @RunWithCustomExecutor
    @Test
    public void testIngestWithServiceLevelInManifest() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        tryImportFile();
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
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
        client.uploadInitialLogbook(params);

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
    }

    @RunWithCustomExecutor
    @Test
    public void testIngestWithoutServiceLevelInManifest() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        tryImportFile();
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
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
        client.uploadInitialLogbook(params);

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
    }

    @RunWithCustomExecutor
    @Test
    public void testProdServicesOK() throws Exception {
        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        try {
            VitamThreadUtils.getVitamSession().setTenantId(tenantId);
            tryImportFile();
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
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
            client.uploadInitialLogbook(params);

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
            client2.uploadInitialLogbook(params2);

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
            throw e;
        }
    }

    @Test
    @RunWithCustomExecutor
    public void shouldImportRulesFile() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient();
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(tenantId));
        final Status status = client.importRulesFile(
            PropertiesUtils.getResourceAsStream(FILE_RULES_OK),
            JEU_DONNEES_OK_REGLES_CSV_CSV);
        ResponseBuilder ResponseBuilder = Response.status(status);
        Response response = ResponseBuilder.build();
        assertEquals(response.getStatus(), Status.CREATED.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void shouldImportAgencies() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);

        FileInputStream stream = new FileInputStream(PropertiesUtils.findFile(FILE_AGENCIES_OK));

        AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient();
        Status status = client.importAgenciesFile(stream, FILE_AGENCIES_OK);
        ResponseBuilder ResponseBuilder = Response.status(status);
        Response response = ResponseBuilder.build();
        assertEquals(response.getStatus(), Status.CREATED.getStatusCode());


        Select select = new Select();
        LogbookOperationsClient operationsClient = LogbookOperationsClientFactory.getInstance().getClient();
        select.setLimitFilter(0, 1);
        select.addOrderByDescFilter(LogbookMongoDbName.eventDateTime.getDbname());
        select.setQuery(eq(LogbookMongoDbName.eventType.getDbname(),
            "IMPORT_AGENCIES"));

        JsonNode logbookResult = operationsClient.selectOperation(select.getFinalSelect());
        assertThat(logbookResult).isNotNull();


        stream = new FileInputStream(PropertiesUtils.findFile(FILE_AGENCIES_AU_update));

        // import contrat
        File fileAccessContracts = PropertiesUtils.getResourceFile("access_contrats.json");
        List<AccessContractModel> accessContractModelList = JsonHandler
            .getFromFileAsTypeRefence(fileAccessContracts, new TypeReference<List<AccessContractModel>>() {});
        client.importAccessContracts(accessContractModelList);
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(tenantId));

        status = client.importAgenciesFile(stream, FILE_AGENCIES_AU_update);
        ResponseBuilder = Response.status(status);
        response = ResponseBuilder.build();
        assertEquals(response.getStatus(), Status.CREATED.getStatusCode());
    }


    @Test
    @RunWithCustomExecutor
    public void shouldRetrieveReportWhenCheckFileRules() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        FileInputStream stream = new FileInputStream(PropertiesUtils.findFile(FILE_RULES_KO_DUPLICATED_REFERENCE));
        FileInputStream streamErrorReport = new FileInputStream(PropertiesUtils.findFile(ERROR_REPORT_CONTENT));
        checkFileRulesWithCustomReferential(stream, streamErrorReport, LINE_3);
    }


    @Test
    @RunWithCustomExecutor
    public void shouldRetrieveReportWhencheckFileRulesError6000Day() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        FileInputStream stream = new FileInputStream(PropertiesUtils.findFile(FILE_RULES_KO_600000_DAY));
        FileInputStream streamErrorReport = new FileInputStream(PropertiesUtils.findFile(ERROR_REPORT_6000_DAYS));
        checkFileRulesWithCustomReferential(stream, streamErrorReport, LINE_2);
    }

    @Test
    @RunWithCustomExecutor
    public void shouldRetrieveReportWhencheckFileRulesError9000Years() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        FileInputStream stream = new FileInputStream(PropertiesUtils.findFile(FILE_RULES_KO_90000_YEAR));
        FileInputStream streamErrorReport = new FileInputStream(PropertiesUtils.findFile(ERROR_REPORT_9000_YEARS));
        checkFileRulesWithCustomReferential(stream, streamErrorReport, LINE_2);
    }

    @Test
    @RunWithCustomExecutor
    public void shouldRetrieveReportWhenCheckFileRulesErrorAnarchyRules() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        FileInputStream stream = new FileInputStream(PropertiesUtils.findFile(FILE_RULES_KO_ANARCHY_RULE));
        FileInputStream streamErrorReport =
            new FileInputStream(PropertiesUtils.findFile(ERROR_REPORT_ANARCHY_RULE));
        checkFileRulesWithCustomReferential(stream, streamErrorReport, LINE_2);
    }

    @Test
    @RunWithCustomExecutor
    public void shouldRetrieveReportWhenCheckFileRulesDecadeMeasurement() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        FileInputStream stream = new FileInputStream(PropertiesUtils.findFile(FILE_RULES_KO_DECADE_MEASURE));
        FileInputStream streamErrorReport =
            new FileInputStream(PropertiesUtils.findFile(ERROR_REPORT_DECADE_MEASURE));
        checkFileRulesWithCustomReferential(stream, streamErrorReport, LINE_2);
    }

    @Test
    @RunWithCustomExecutor
    public void shouldRetrieveReportWhenCheckFileRulesErrorNegativeDuration() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        FileInputStream stream = new FileInputStream(PropertiesUtils.findFile(FILE_RULES_KO_NEGATIVE_DURATION));
        FileInputStream streamErrorReport =
            new FileInputStream(PropertiesUtils.findFile(ERROR_REPORT_NEGATIVE_DURATION));
        checkFileRulesWithCustomReferential(stream, streamErrorReport, LINE_2);
    }

    @Test
    @RunWithCustomExecutor
    public void shouldRetrieveReportWhenCheckFileRulesErrorWrongComa() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        FileInputStream stream =
            new FileInputStream(PropertiesUtils.findFile(FILE_RULES_KO_REFERENCE_WITH_WRONG_COMMA));
        FileInputStream streamErrorReport =
            new FileInputStream(PropertiesUtils.findFile(ERROR_REPORT_REFERENCE_WITH_WRONG_COMA));
        checkFileRulesWithCustomReferential(stream, streamErrorReport,
            LINE_3);
    }

    @Test
    @RunWithCustomExecutor
    public void shouldRetrieveReportWhenCheckFileRulesErrorUnknowDuration() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        final FileInputStream stream =
            new FileInputStream(PropertiesUtils.findFile(FILE_RULES_KO_UNKNOWN_DURATION));
        final FileInputStream expectedStreamErrorReport =
            new FileInputStream(PropertiesUtils.findFile(ERROR_REPORT_UNKNOW_DURATION));
        checkFileRulesWithCustomReferential(stream, expectedStreamErrorReport,
            LINE_2);
    }

    /**
     * Check error report
     *
     * @param fileInputStreamToImport the given FileInputStream
     * @param expectedStreamErrorReport expected Stream error report
     */
    private void checkFileRulesWithCustomReferential(final FileInputStream fileInputStreamToImport,
        final FileInputStream expectedStreamErrorReport, String lineNumber)
        throws Exception {
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            final Response response = client.checkRulesFile(fileInputStreamToImport);
            final String readEntity = response.readEntity(String.class);
            final JsonNode responseEntityNode = JsonHandler.getFromString(readEntity);
            final JsonNode responseError = responseEntityNode.get("error").get(lineNumber).get(0).get("Code");
            final JsonNode expectedNode = JsonHandler.getFromInputStream(expectedStreamErrorReport);
            final JsonNode expectedError = expectedNode.get("error").get(lineNumber).get(0).get("Code");
            assertEquals(expectedError, responseError);
            assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testIngestInternalMultipleActions() throws Exception {

        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        try {
            VitamThreadUtils.getVitamSession().setTenantId(tenantId);
            tryImportFile();
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
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
            final IngestInternalClient client2 = IngestInternalClientFactory.getInstance().getClient();
            client2.uploadInitialLogbook(params);

            // init workflow before execution
            client2.initWorkFlow("DEFAULT_WORKFLOW_RESUME");

            client2.upload(zipInputStreamSipObject, CommonMediaType.ZIP_TYPE, CONTEXT_ID_NEXT);

            // lets wait till the step is finished
            waitStep(operationGuid.toString(), client2);

            ProcessWorkflow processWorkflow =
                ProcessMonitoringImpl.getInstance().findOneProcessWorkflow(operationGuid.toString(), tenantId);
            assertNotNull(processWorkflow);
            assertEquals(ProcessState.PAUSE, processWorkflow.getState());
            assertEquals(StatusCode.OK, processWorkflow.getStatus());

            ItemStatus itemStatus1 =
                client2.getOperationProcessExecutionDetails(operationGuid.toString());
            assertEquals(StatusCode.OK, itemStatus1.getGlobalStatus());

            assertNotNull(client2.getWorkflowDefinitions());

            // then finally we cancel the ingest
            ItemStatus itemStatusFinal = client2.cancelOperationProcessExecution(operationGuid.toString());
            // FATAL is thrown but this could be a bug somewher, so when it is fixed, change the value here
            assertEquals(StatusCode.FATAL, itemStatusFinal.getGlobalStatus());

        } catch (final Exception e) {
            LOGGER.error(e);
            try (LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient()) {
                fr.gouv.vitam.common.database.builder.request.single.Select selectQuery =
                    new fr.gouv.vitam.common.database.builder.request.single.Select();
                selectQuery.setQuery(QueryHelper.eq("evIdProc", operationGuid.getId()));
                JsonNode logbookResult = logbookClient.selectOperation(selectQuery.getFinalSelect());
                LOGGER.error(JsonHandler.prettyPrint(logbookResult));
            }
            throw e;
        }

    }

    private void waitStep(String operationId, IngestInternalClient client) {
        int nbTry = 0;
        while (true) {
            try {
                ItemStatus itemStatus = client.getOperationProcessStatus(operationId);
                if (itemStatus.getGlobalStatus() == StatusCode.OK) {
                    break;
                }
                Thread.sleep(SLEEP_TIME);
            } catch (VitamClientException | InternalServerException | BadRequestException | InterruptedException e) {
                SysErrLogger.FAKE_LOGGER.ignoreLog(e);
            }
            if (nbTry == NB_TRY)
                break;
            nbTry++;
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testIngestInternal4396() throws Exception {
        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        tryImportFile();
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        // workspace client dezip SIP in workspace
        RestAssured.port = PORT_SERVICE_WORKSPACE;
        RestAssured.basePath = WORKSPACE_PATH;
        final InputStream zipInputStreamSipObject =
            PropertiesUtils.getResourceAsStream(SIP_4396);

        // init default logbook operation
        final List<LogbookOperationParameters> params = new ArrayList<>();
        final LogbookOperationParameters initParameters = LogbookParametersFactory.newLogbookOperationParameters(
            operationGuid, "Process_SIP_unitary", operationGuid,
            LogbookTypeProcess.INGEST, StatusCode.STARTED,
            operationGuid != null ? operationGuid.toString() : "outcomeDetailMessage",
            operationGuid);
        params.add(initParameters);

        // call ingest
        IngestInternalClientFactory.getInstance().changeServerPort(PORT_SERVICE_INGEST_INTERNAL);
        final IngestInternalClient client = IngestInternalClientFactory.getInstance().getClient();
        client.uploadInitialLogbook(params);

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
        select.addQueries(QueryHelper.and().add(QueryHelper.match("Title", "monSIP")).add(QueryHelper.in("#operations", operationGuid.toString())));
        // Get AU
        final MetaDataClient metadataClient = MetaDataClientFactory.getInstance().getClient();
        final JsonNode node = metadataClient.selectUnits(select.getFinalSelect());

        // management for this unit
        LOGGER.warn(node.toString());
        assertNotNull(node);
        assertNotNull(node.get("$results"));
        assertEquals(1, node.get("$results").size());
        assertNotNull(node.get("$results").get(0).get("Title"));
        assertNotNull(node.get("$results").get(0).get("#management"));
        assertEquals("Secret Défense",node.get("$results").get(0).get("#management").get("ClassificationRule").get("ClassificationLevel").asText());
        assertEquals("ClassOWn",node.get("$results").get(0).get("#management").get("ClassificationRule").get("ClassificationOwner").asText());

    }

}
