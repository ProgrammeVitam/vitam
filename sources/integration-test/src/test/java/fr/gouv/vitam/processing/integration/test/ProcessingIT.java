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
import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTION.FIELDS;
import static fr.gouv.vitam.common.guid.GUIDFactory.newOperationLogbookGUID;
import static fr.gouv.vitam.logbook.common.server.database.collections.LogbookDocument.EVENT_DETAILS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.jayway.restassured.RestAssured;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.model.Filters;
import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.DataLoader;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.client.VitamClientFactoryInterface;
import fr.gouv.vitam.common.database.builder.query.CompareQuery;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.query.action.SetAction;
import fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.GLOBAL;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTIONARGS;
import fr.gouv.vitam.common.database.builder.request.multiple.InsertMultiQuery;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.builder.request.single.Update;
import fr.gouv.vitam.common.database.parser.request.adapter.SingleVarNameAdapter;
import fr.gouv.vitam.common.database.parser.request.single.UpdateParserSingle;
import fr.gouv.vitam.common.format.identification.FormatIdentifierFactory;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.guid.GUIDReader;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.UpdateWorkflowConstants;
import fr.gouv.vitam.common.model.administration.AccessionRegisterDetailModel;
import fr.gouv.vitam.common.model.administration.IngestContractModel;
import fr.gouv.vitam.common.model.administration.RegisterValueDetailModel;
import fr.gouv.vitam.common.model.logbook.LogbookEventOperation;
import fr.gouv.vitam.common.model.logbook.LogbookOperation;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.AccessionRegisterSummary;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.Contexts;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbName;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.metadata.core.UnitInheritedRule;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.core.database.configuration.GlobalDatasDb;
import fr.gouv.vitam.metadata.rest.MetadataMain;
import fr.gouv.vitam.processing.common.ProcessingEntry;
import fr.gouv.vitam.processing.common.exception.ProcessingStorageWorkspaceException;
import fr.gouv.vitam.processing.common.model.ProcessWorkflow;
import fr.gouv.vitam.processing.data.core.ProcessDataAccessImpl;
import fr.gouv.vitam.processing.data.core.management.ProcessDataManagement;
import fr.gouv.vitam.processing.data.core.management.WorkspaceProcessDataManagement;
import fr.gouv.vitam.processing.engine.core.monitoring.ProcessMonitoringImpl;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.processing.management.rest.ProcessManagementMain;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.worker.core.plugin.CheckExistenceObjectPlugin;
import fr.gouv.vitam.worker.core.plugin.CheckIntegrityObjectPlugin;
import fr.gouv.vitam.worker.server.rest.WorkerMain;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import org.bson.Document;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Processing integration test
 */
public class ProcessingIT extends VitamRuleRunner {

    @ClassRule
    public static VitamServerRunner runner =
        new VitamServerRunner(ProcessingIT.class, mongoRule.getMongoDatabase().getName(),
            elasticsearchRule.getClusterName(),
            Sets.newHashSet(
                MetadataMain.class,
                WorkerMain.class,
                AdminManagementMain.class,
                LogbookMain.class,
                WorkspaceMain.class,
                ProcessManagementMain.class
            ));
    private static final String PROCESSING_UNIT_PLAN = "integration-processing/unit_plan_metadata.json";
    private static final String INGEST_CONTRACTS_PLAN = "integration-processing/ingest_contracts_plan.json";
    private static final String OG_ATTACHEMENT_ID = "aebaaaaaaacu6xzeabinwak6t5ecmmaaaaaq";
    private static final long SLEEP_TIME = 20l;
    private static final long NB_TRY = 18000;
    private static final String SIP_FILE_WRONG_DATE = "integration-processing/SIP_INGEST_WRONG_DATE.zip";
    private static final String SIP_KO_AU_REF_OBJ =
        "integration-processing/KO_SIP_1986_unit_declare_IDobjet_au_lieu_IDGOT.zip";
    private static final String SIP_KO_MANIFEST_URI = "integration-processing/KO_MANIFESTE-URI.zip";

    private static final Integer tenantId = 0;


    private static final String SIP_FOLDER = "SIP";
    private static final String METADATA_PATH = "/metadata/v1";
    private static final String PROCESSING_PATH = "/processing/v1";
    private static final String WORKER_PATH = "/worker/v1";
    private static final String WORKSPACE_PATH = "/workspace/v1";
    private static final String LOGBOOK_PATH = "/logbook/v1";

    private static String CONFIG_BIG_WORKER_PATH = "";


    private WorkspaceClient workspaceClient;
    private ProcessingManagementClient processingClient;
    private static ProcessMonitoringImpl processMonitoring;


    private static String WORFKLOW_NAME_2 = "PROCESS_SIP_UNITARY";
    private static String WORFKLOW_NAME = "PROCESS_SIP_UNITARY";
    private static String BLANK_WORKFLOW_NAME = "PROCESS_SIP_UNITARY_TEST";
    private static String INGEST_TREE_WORFKLOW = "HOLDINGSCHEME";
    private static String BIG_WORFKLOW_NAME = "BigIngestWorkflow";
    private static String UPD8_AU_WORKFLOW = "UPDATE_RULES_ARCHIVE_UNITS";
    private static String SIP_FILE_OK_NAME = "integration-processing/SIP-test.zip";
    private static String OK_RATTACHEMENT = "integration-processing/OK_Rattachement.zip";

    private static String SIP_FILE_OK_BIRTH_PLACE = "integration-processing/unit_schema_validation_ko.zip";
    private static String SIP_PROFIL_OK = "integration-processing/SIP_ok_profil.zip";
    private static String SIP_INGEST_CONTRACT_UNKNOW = "integration-processing/SIP_INGEST_CONTRACT_UNKNOW.zip";
    private static String SIP_INGEST_CONTRACT_NOT_IN_CONTEXT =
        "integration-processing/SIP_INGEST_CONTRACT_NOT_IN_CONTEXT.zip";
    private static String SIP_FILE_OK_WITH_SYSTEMID = "integration-processing/SIP_with_systemID.zip";

    private static String SIP_FILE_ADD_AU_LINK_OK_NAME_TARGET = "integration-processing";
    private static String SIP_FILE_ADD_AU_LINK_OK_NAME = "integration-processing/OK_SIP_ADD_AU_LINK";
    private static String SIP_FILE_ADD_AU_LINK_BY_QUERY_OK_NAME = "integration-processing/OK_SIP_ADD_AU_LINK_BY_QUERY";

    private static String LINK_AU_TO_EXISTING_GOT_OK_NAME = "integration-processing/OK_LINK_AU_TO_EXISTING_GOT";
    private static String LINK_AU_TO_EXISTING_GOT_OK_NAME_TARGET = "integration-processing";
    private static String SIP_FILE_TAR_OK_NAME = "integration-processing/SIP.tar";
    private static String SIP_INHERITED_RULE_CA1_OK = "integration-processing/1069_CA1.zip";
    private static String SIP_INHERITED_RULE_CA4_OK = "integration-processing/1069_CA4.zip";
    private static String SIP_FUND_REGISTER_OK = "integration-processing/OK-registre-fonds.zip";
    private static String SIP_WITHOUT_MANIFEST = "integration-processing/SIP_no_manifest.zip";
    private static String SIP_NO_FORMAT = "integration-processing/SIP_NO_FORMAT.zip";
    private static String SIP_NO_FORMAT_NO_TAG = "integration-processing/SIP_NO_FORMAT_TAG.zip";
    private static String SIP_NB_OBJ_INCORRECT_IN_MANIFEST = "integration-processing/SIP_Conformity_KO.zip";
    private static String SIP_BUG_2721 = "integration-processing/bug2721_2racines_meme_rattachement.zip";
    private static String SIP_WITHOUT_OBJ = "integration-processing/OK_SIP_sans_objet.zip";
    private static String SIP_WITHOUT_FUND_REGISTER = "integration-processing/KO_registre_des_fonds.zip";
    private static String SIP_BORD_AU_REF_PHYS_OBJECT = "integration-processing/KO_BORD_AUrefphysobject.zip";
    private static String SIP_MANIFEST_INCORRECT_REFERENCE = "integration-processing/KO_Reference_Unexisting.zip";
    private static String SIP_REFERENCE_CONTRACT_KO = "integration-processing/KO_SIP_2_GO_contract.zip";
    private static String SIP_COMPLEX_RULES = "integration-processing/OK_RULES_COMPLEXE_COMPLETE.zip";
    private static String SIP_APPRAISAL_RULES = "integration-processing/bug_appraisal.zip";

    private static String SIP_FILE_KO_AU_REF_BDO = "integration-processing/SIP_KO_ArchiveUnit_ref_BDO.zip";
    private static String SIP_BUG_2182 = "integration-processing/SIP_bug_2182.zip";
    private static String SIP_FILE_1791_CA1 = "integration-processing/SIP_FILE_1791_CA1.zip";
    private static String SIP_FILE_1791_CA2 = "integration-processing/SIP_FILE_1791_CA2.zip";

    private static String OK_SIP_SIGNATURE = "integration-processing/Signature_OK.zip";

    private static String SIP_ARBRE_3062 = "integration-processing/3062_arbre.zip";

    private static String SIP_PROD_SERV_A = "integration-processing/Sip_A.zip";
    private static String SIP_PROD_SERV_B_ATTACHED = "integration-processing/SIP_B";

    private static String SIP_FULL_SEDA_2_1 = "integration-processing/OK_SIP_FULL_SEDA2.1.zip";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        CONFIG_BIG_WORKER_PATH = PropertiesUtils.getResourcePath("integration-processing/bigworker.conf").toString();

        FormatIdentifierFactory.getInstance().changeConfigurationFile(runner.FORMAT_IDENTIFIERS_CONF);

        processMonitoring = ProcessMonitoringImpl.getInstance();

        StorageClientFactory storageClientFactory = StorageClientFactory.getInstance();
        storageClientFactory.setVitamClientType(VitamClientFactoryInterface.VitamClientType.MOCK);

        System.err.println("===============  Before Class =======================");

        new DataLoader("integration-processing").prepareData();

    }



    public static void prepareVitamSession() {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        VitamThreadUtils.getVitamSession().setContractId("aName");
        VitamThreadUtils.getVitamSession().setContextId("Context_IT");
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {

        StorageClientFactory storageClientFactory = StorageClientFactory.getInstance();
        storageClientFactory.setVitamClientType(VitamClientFactoryInterface.VitamClientType.PRODUCTION);
        runAfter();
    }

    @After
    public void afterTest() throws Exception {
        VitamThreadUtils.getVitamSession().setContractId("aName");
        VitamThreadUtils.getVitamSession().setContextId("Context_IT");

        ProcessDataAccessImpl.getInstance().clearWorkflow();
        runAfterMongo(Sets.newHashSet(
            MetadataCollections.UNIT.getName(),
            MetadataCollections.OBJECTGROUP.getName(),
            FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getName(),
            FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getName(),
            LogbookCollections.OPERATION.getName(),
            LogbookCollections.LIFECYCLE_UNIT.getName(),
            LogbookCollections.LIFECYCLE_OBJECTGROUP.getName(),
            LogbookCollections.LIFECYCLE_OBJECTGROUP.getName(),
            LogbookCollections.LIFECYCLE_UNIT_IN_PROCESS.getName()

        ));

        runAfterEs(Sets.newHashSet(
            MetadataCollections.UNIT.getName().toLowerCase() + "_0",
            MetadataCollections.UNIT.getName().toLowerCase() + "_1",
            MetadataCollections.OBJECTGROUP.getName().toLowerCase() + "_0",
            MetadataCollections.OBJECTGROUP.getName().toLowerCase() + "_1",
            LogbookCollections.OPERATION.getName().toLowerCase() + "_0",
            LogbookCollections.OPERATION.getName().toLowerCase() + "_1",
            FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getName().toLowerCase(),
            FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getName().toLowerCase()
        ));
    }

    @RunWithCustomExecutor
    @Test
    public void testServersStatus() throws Exception {
        RestAssured.port = runner.PORT_SERVICE_PROCESSING;
        RestAssured.basePath = PROCESSING_PATH;

        get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());

        RestAssured.port = runner.PORT_SERVICE_WORKSPACE;
        RestAssured.basePath = WORKSPACE_PATH;
        get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());

        RestAssured.port = runner.PORT_SERVICE_METADATA;
        RestAssured.basePath = METADATA_PATH;
        get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());

        RestAssured.port = runner.PORT_SERVICE_WORKER;
        RestAssured.basePath = WORKER_PATH;
        get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());

        RestAssured.port = runner.PORT_SERVICE_LOGBOOK;
        RestAssured.basePath = LOGBOOK_PATH;
        get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());
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

    /**
     * This test needs Siegfried already running and started as:<br/>
     * sf -server localhost:8999<br/>
     * <br/>
     * If not started, this test will be ignored.
     *
     * @throws Exception
     */
    @RunWithCustomExecutor
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
            FormatIdentifierFactory.getInstance().changeConfigurationFile(runner.FORMAT_IDENTIFIERS_CONF);
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflow() throws Exception {
        prepareVitamSession();
        final GUID objectGuid = GUIDFactory.newManifestGUID(tenantId);
        final String containerName = objectGuid.getId();
        try (MetaDataClient metaDataClient = MetaDataClientFactory.getInstance().getClient();
            AdminManagementClient functionalClient = AdminManagementClientFactory.getInstance().getClient()) {

            final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            createLogbookOperation(operationGuid, objectGuid);

            // workspace client unzip SIP in workspace
            final InputStream zipInputStreamSipObject = PropertiesUtils.getResourceAsStream(SIP_BUG_2721);
            workspaceClient = WorkspaceClientFactory.getInstance().getClient();
            workspaceClient.createContainer(containerName);
            workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP,
                zipInputStreamSipObject);
            // call processing
            metaDataClient.insertUnit(
                new InsertMultiQuery()
                    .addData((ObjectNode) JsonHandler
                        .getFromFile(PropertiesUtils.getResourceFile("integration-processing/unit_metadata.json")))
                    .getFinalInsert());

            metaDataClient.insertUnit(
                new InsertMultiQuery()
                    .addData(
                        (ObjectNode) JsonHandler.getFromFile(PropertiesUtils.getResourceFile(PROCESSING_UNIT_PLAN)))
                    .getFinalInsert());

            metaDataClient.flushUnits();
            // import contract
            File fileContracts = PropertiesUtils.getResourceFile(INGEST_CONTRACTS_PLAN);
            List<IngestContractModel> IngestContractModelList =
                JsonHandler.getFromFileAsTypeRefence(fileContracts, new TypeReference<List<IngestContractModel>>() {
                });

            functionalClient.importIngestContracts(IngestContractModelList);

            processingClient = ProcessingManagementClientFactory.getInstance().getClient();
            processingClient.initVitamProcess(Contexts.DEFAULT_WORKFLOW.name(), containerName, WORFKLOW_NAME);

            RequestResponse<ItemStatus> ret =
                processingClient.updateOperationActionProcess(ProcessAction.RESUME.getValue(), containerName);
            assertNotNull(ret);
            assertEquals(Status.ACCEPTED.getStatusCode(), ret.getStatus());

            wait(containerName);
            ProcessWorkflow processWorkflow = processMonitoring.findOneProcessWorkflow(containerName, tenantId);
            assertNotNull(processWorkflow);
            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
            assertEquals(StatusCode.WARNING, processWorkflow.getStatus());

            LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient();
            fr.gouv.vitam.common.database.builder.request.single.Select selectQuery =
                new fr.gouv.vitam.common.database.builder.request.single.Select();
            selectQuery.setQuery(QueryHelper.eq("evIdProc", containerName));
            JsonNode logbookResult = logbookClient.selectOperation(selectQuery.getFinalSelect());

            // as logbookClient.selectOperation returns last two events and after removing STARTED from events
            // the order is main-event > sub-events, so events[0] will be "ROLL_BACK.OK" and not
            // "STP_INGEST_FINALISATION.OK"
            assertEquals(logbookResult.get("$results").get(0).get("events").get(0).get("outDetail").asText(),
                "ROLL_BACK.OK");
            assertEquals(logbookResult.get("$results").get(0).get("events").get(1).get("outDetail").asText(),
                "PROCESS_SIP_UNITARY.WARNING");

            assertEquals(logbookResult.get("$results").get(0).get("obIdIn").asText(),
                "bug2721_2racines_meme_rattachement");

            JsonNode agIdExt = JsonHandler.getFromString(logbookResult.get("$results").get(0).get("agIdExt").asText());
            assertEquals(agIdExt.get("originatingAgency").asText(), "producteur1");

            // lets check the accession register
            Select query = new Select();
            query.setLimitFilter(0, 1);
            RequestResponse resp = functionalClient.getAccessionRegister(query.getFinalSelect());
            assertThat(resp).isInstanceOf(RequestResponseOK.class);
            assertThat(((RequestResponseOK) resp).getHits().getTotal()).isEqualTo(2);
            assertThat(((RequestResponseOK) resp).getHits().getSize()).isEqualTo(1);

            // check if unit is valid
            MongoIterable<Document> resultCheckUnits = MetadataCollections.UNIT.getCollection().find();
            Document unitCheck = resultCheckUnits.first();
            assertThat(unitCheck.get("_storage")).isNotNull();
            Document storageUnit = (Document) unitCheck.get("_storage");
            assertThat(storageUnit.get("_nbc")).isNotNull();
            assertThat(storageUnit.get("_nbc")).isEqualTo(2);

            // check if units are valid
            MongoIterable<Document> resultCheckObjectGroups = MetadataCollections.OBJECTGROUP.getCollection().find();
            Document objectGroupCheck = resultCheckObjectGroups.first();
            assertThat(objectGroupCheck.get("_storage")).isNotNull();
            Document storageObjectGroup = (Document) objectGroupCheck.get("_storage");
            assertThat(storageObjectGroup.get("_nbc")).isNotNull();
            assertThat(storageObjectGroup.get("_nbc")).isEqualTo(2);

            List<Document> qualifiers = (List<Document>) objectGroupCheck.get("_qualifiers");
            assertThat(qualifiers.size()).isEqualTo(3);
            Document binaryMaster = qualifiers.get(0);
            assertThat(binaryMaster.get("_nbc")).isNotNull();
            assertThat(binaryMaster.get("_nbc")).isEqualTo(1);

            List<Document> versions = (List<Document>) binaryMaster.get("versions");
            assertThat(versions.size()).isEqualTo(1);
            Document version = versions.get(0);
            Document storageVersion = (Document) version.get("_storage");
            assertThat(storageVersion.get("_nbc")).isNotNull();
            assertThat(storageVersion.get("_nbc")).isEqualTo(1);
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testAudit() throws Exception {
        prepareVitamSession();
        try (MetaDataClient metaDataClient = MetaDataClientFactory.getInstance().getClient();
            LogbookLifeCyclesClient logbookLFCClient = LogbookLifeCyclesClientFactory.getInstance().getClient();
            LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient();
            AdminManagementClient functionalClient = AdminManagementClientFactory.getInstance().getClient()) {



            metaDataClient.insertObjectGroup(
                new InsertMultiQuery()
                    .addData((ObjectNode) JsonHandler
                        .getFromFile(PropertiesUtils.getResourceFile("integration-processing/og_metadata.json")))
                    .getFinalInsert());

            GUID logLfcId = GUIDFactory.newOperationLogbookGUID(tenantId);
            logbookLFCClient.create(
                LogbookParametersFactory.newLogbookLifeCycleObjectGroupParameters(
                    logLfcId,
                    "INGEST",
                    logLfcId,
                    LogbookTypeProcess.INGEST,
                    StatusCode.OK,
                    "Process_SIP_unitary.OK",
                    OG_ATTACHEMENT_ID,
                    GUIDReader.getGUID(OG_ATTACHEMENT_ID)));
            logbookLFCClient.commitObjectGroup(logLfcId.getId(), OG_ATTACHEMENT_ID);
            GUID opIngestId = GUIDFactory.newOperationLogbookGUID(tenantId);

            LogbookOperationParameters newLogbookOperationParameters =
                LogbookParametersFactory.newLogbookOperationParameters(
                    opIngestId,
                    "PROCESS_SIP_UNITARY",
                    opIngestId,
                    LogbookTypeProcess.INGEST,
                    StatusCode.STARTED,
                    "PROCESS_SIP_UNITARY.STARTED",
                    opIngestId);

            newLogbookOperationParameters.putParameterValue(
                LogbookParameterName.agIdExt, "{\"originatingAgency\":\"Vitam\"}");
            logbookClient.create(newLogbookOperationParameters);
            newLogbookOperationParameters.putParameterValue(
                LogbookParameterName.outcomeDetail, "PROCESS_SIP_UNITARY.OK");

            logbookClient.update(newLogbookOperationParameters);

            AccessionRegisterDetailModel register = new AccessionRegisterDetailModel();
            register.setIdentifier("Identifier");
            register.setOperationGroup("OP_GROUP");
            register.setOriginatingAgency("Vitam");
            register.setTotalObjects(new RegisterValueDetailModel(1, 0, 0));
            register.setTotalObjectsGroups(new RegisterValueDetailModel(1, 0, 0));
            register.setTotalUnits(new RegisterValueDetailModel(1, 0, 0));
            register.setObjectSize(new RegisterValueDetailModel(1, 0, 0));
            register.setEndDate("01/01/2017");
            register.setStartDate("01/01/2017");
            register.setLastUpdate("01/01/2017");
            functionalClient.createorUpdateAccessionRegister(register);

            // Test Audit
            final GUID opId = GUIDFactory.newRequestIdGUID(tenantId);
            final String auditId = opId.toString();

            final GUID operationAuditGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
            VitamThreadUtils.getVitamSession().setRequestId(operationAuditGuid);
            createLogbookOperation(operationAuditGuid, opId, "PROCESS_AUDIT", LogbookTypeProcess.AUDIT);
            final ProcessingEntry entry = new ProcessingEntry(auditId, Contexts.AUDIT_WORKFLOW.getEventType());
            entry.getExtraParams().put("objectId", "0");
            entry.getExtraParams().put("auditType", "tenant");
            entry.getExtraParams().put("auditActions",
                CheckExistenceObjectPlugin.getId() + "," + CheckIntegrityObjectPlugin.getId());
            processingClient = ProcessingManagementClientFactory.getInstance().getClient();
            processingClient.initVitamProcess(Contexts.AUDIT_WORKFLOW.name(), entry);

            processingClient.updateOperationActionProcess(ProcessAction.RESUME.getValue(), auditId);

            wait(auditId);

            ProcessWorkflow processAuditWorkflow = processMonitoring.findOneProcessWorkflow(auditId, tenantId);
            assertNotNull(processAuditWorkflow);
            assertEquals(ProcessState.COMPLETED, processAuditWorkflow.getState());
            assertEquals(StatusCode.OK, processAuditWorkflow.getStatus());

            fr.gouv.vitam.common.database.builder.request.single.Select selectQuery =
                new fr.gouv.vitam.common.database.builder.request.single.Select();
            selectQuery.setQuery(QueryHelper.eq("evTypeProc", "AUDIT"));
            JsonNode logbookResult = logbookClient.selectOperation(selectQuery.getFinalSelect());
            JsonNode logbookNode = logbookResult.get("$results").get(0);
            assertEquals(logbookNode.get(LogbookMongoDbName.eventDetailData.getDbname()).asText(),
                "{\n  \"Vitam\" : {\n    \"OK\" : 1,\n    \"KO\" : 0,\n    \"WARNING\" : 0\n  }\n}");
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowIngestContractUnknow() throws Exception {
        prepareVitamSession();

        final String containerName = createOperationContainer();

        // workspace client dezip SIP in workspace
        final InputStream zipInputStreamSipObject =
            PropertiesUtils.getResourceAsStream(SIP_INGEST_CONTRACT_UNKNOW);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP,
            zipInputStreamSipObject);
        // call processing

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

        LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient();
        fr.gouv.vitam.common.database.builder.request.single.Select selectQuery =
            new fr.gouv.vitam.common.database.builder.request.single.Select();
        JsonNode logbookResult = logbookClient.selectOperationById(containerName, selectQuery.getFinalSelect());
        JsonNode logbookNode = logbookResult.get("$results").get(0);
        assertEquals(logbookNode.get("events").get(6).get("outDetail").asText(),
            "CHECK_HEADER.CHECK_CONTRACT_INGEST.CONTRACT_UNKNOWN.KO");
    }


    @RunWithCustomExecutor
    @Test
    public void testWorkflowIngestContractNotInContextUnknow() throws Exception {
        prepareVitamSession();

        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        final GUID objectGuid = GUIDFactory.newManifestGUID(tenantId);
        final String containerName = objectGuid.getId();
        createLogbookOperation(operationGuid, objectGuid);

        // workspace client dezip SIP in workspace
        final InputStream zipInputStreamSipObject =
            PropertiesUtils.getResourceAsStream(SIP_INGEST_CONTRACT_NOT_IN_CONTEXT);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP,
            zipInputStreamSipObject);
        // call processing

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

        LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient();
        fr.gouv.vitam.common.database.builder.request.single.Select selectQuery =
            new fr.gouv.vitam.common.database.builder.request.single.Select();
        JsonNode logbookResult = logbookClient.selectOperationById(containerName, selectQuery.getFinalSelect());
        JsonNode logbookNode = logbookResult.get("$results").get(0);
        assertThat(logbookNode.get("events").get(6).get("outDetail").asText())
            .isEqualTo("CHECK_HEADER.CHECK_CONTRACT_INGEST.CONTRACT_NOT_IN_CONTEXT.KO");
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowProfil() throws Exception {
        prepareVitamSession();

        final String containerName = createOperationContainer();

        // workspace client dezip SIP in workspace
        final InputStream zipInputStreamSipObject =
            PropertiesUtils.getResourceAsStream(SIP_PROFIL_OK);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP,
            zipInputStreamSipObject);
        // call processing
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

        LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient();
        fr.gouv.vitam.common.database.builder.request.single.Select selectQuery =
            new fr.gouv.vitam.common.database.builder.request.single.Select();
        selectQuery.setQuery(QueryHelper.eq("evIdProc", containerName));
        JsonNode logbookResult = logbookClient.selectOperation(selectQuery.getFinalSelect());
        JsonNode logbookNode = logbookResult.get("$results").get(0);
        assertEquals(logbookNode.get("rightsStatementIdentifier").asText(),
            "{\"ArchivalAgreement\":\"IC_WITH_PROFILE\",\"ArchivalProfile\":\"PR-000001\"}");
        JsonNode agIdExt = JsonHandler.getFromString(logbookNode.get("agIdExt").asText());

        assertEquals(agIdExt.get("originatingAgency").asText(), "producteur1");
        assertEquals(agIdExt.get("ArchivalAgency").asText(), "producteur1");
        assertEquals(agIdExt.get("TransferringAgency").asText(), "producteur1");

        assertTrue(logbookNode.get("evDetData").asText().contains("EvDetailReq"));
        assertTrue(logbookNode.get("evDetData").asText().contains("EvDateTimeReq"));
        assertTrue(logbookNode.get("evDetData").asText().contains("ArchivalAgreement"));
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowWithSIPContainsSystemId() throws Exception {
        prepareVitamSession();

        final String containerName = createOperationContainer();

        // workspace client dezip SIP in workspace
        final InputStream zipInputStreamSipObject =
            PropertiesUtils.getResourceAsStream(SIP_FILE_OK_WITH_SYSTEMID);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP,
            zipInputStreamSipObject);
        // call processing
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
        assertThat(processWorkflow).isNotNull();
        assertThat(processWorkflow.getState()).isEqualTo(ProcessState.COMPLETED);
        assertThat(processWorkflow.getStatus()).isEqualTo(StatusCode.WARNING);
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowWithTarSIP() throws Exception {
        prepareVitamSession();

        final String containerName = createOperationContainer();

        // workspace client dezip SIP in workspace
        final InputStream zipInputStreamSipObject =
            Thread.currentThread().getContextClassLoader().getResourceAsStream(SIP_FILE_TAR_OK_NAME);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.TAR,
            zipInputStreamSipObject);

        // call processing
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
    }


    @RunWithCustomExecutor
    @Test
    public void testWorkflow_with_herited_ruleCA1() throws Exception {
        prepareVitamSession();

        final String containerName = createOperationContainer();

        // call processing
        processingClient = ProcessingManagementClientFactory.getInstance().getClient();

        // workspace client dezip SIP in workspace
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
        query.addProjection(JsonHandler.createObjectNode().set(FIELDS.exactToken(),
            JsonHandler.createObjectNode()
                .put(GLOBAL.RULES.exactToken(), 1).put("Title", 1)
                .put(PROJECTIONARGS.MANAGEMENT.exactToken(), 1)));

        JsonNode resultNoScroll = metaDataClient.selectUnits(query.getFinalSelect());
        assertFalse(JsonHandler.unprettyPrint(resultNoScroll.get("$hits")).contains("scrollId"));

        query.setScrollFilter(GlobalDatasDb.SCROLL_ACTIVATE_KEYWORD, GlobalDatasDb.DEFAULT_SCROLL_TIMEOUT, 100);
        JsonNode result = metaDataClient.selectUnits(query.getFinalSelect());
        assertNotNull(result.get("$results").get(0).get(UnitInheritedRule.INHERITED_RULE).get("StorageRule")
            .get("R1"));
        assertNotNull(result.get("$hits").get("scrollId"));
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            client.importRulesFile(PropertiesUtils.getResourceAsStream("integration-processing/new_rule.csv"),
                "new_rule.csv");
            JsonNode response = client.getRuleByID("R7");
            assertTrue(response.get("$results").size() > 0);
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflow_with_herited_ruleCA4() throws Exception {
        prepareVitamSession();

        final String containerName = createOperationContainer();

        // workspace client dezip SIP in workspace
        final InputStream zipInputStreamSipObject =
            PropertiesUtils.getResourceAsStream(SIP_INHERITED_RULE_CA4_OK);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP, zipInputStreamSipObject);

        // call processing
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

        MetaDataClient metaDataClient = MetaDataClientFactory.getInstance().getClient();
        SelectMultiQuery query = new SelectMultiQuery();
        query.addQueries(QueryHelper.eq("Title", "ArchiveUnite4").setRelativeDepthLimit(5));
        query.addProjection(JsonHandler.createObjectNode().set(FIELDS.exactToken(),
            JsonHandler.createObjectNode()
                .put(GLOBAL.RULES.exactToken(), 1).put("Title", 1)
                .put(PROJECTIONARGS.MANAGEMENT.exactToken(), 1)));
        JsonNode result = metaDataClient.selectUnits(query.getFinalSelect());
        assertNotNull(result.get("$results").get(0).get(UnitInheritedRule.INHERITED_RULE).get("StorageRule")
            .get("R1"));
        assertNull(result.get("$results").get(0).get(VitamFieldsHelper.management()).get("OriginatingAgency"));
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflow_with_accession_register() throws Exception {
        prepareVitamSession();

        final String containerName = createOperationContainer();

        // workspace client dezip SIP in workspace
        final InputStream zipInputStreamSipObject =
            PropertiesUtils.getResourceAsStream(SIP_FUND_REGISTER_OK);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP, zipInputStreamSipObject);

        // call processing
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
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowWithSipNoManifest() throws Exception {
        prepareVitamSession();

        final String containerName = createOperationContainer();

        // workspace client dezip SIP in workspace
        final InputStream zipInputStreamSipObject =
            PropertiesUtils.getResourceAsStream(SIP_WITHOUT_MANIFEST);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP, zipInputStreamSipObject);

        // call processing
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
        prepareVitamSession();

        final String containerName = createOperationContainer();

        // workspace client dezip SIP in workspace
        final InputStream zipInputStreamSipObject =
            PropertiesUtils.getResourceAsStream(SIP_NO_FORMAT);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP, zipInputStreamSipObject);

        // call processing
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
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowSipNoFormatNoTag() throws Exception {

        prepareVitamSession();

        final String containerName = createOperationContainer();

        // workspace client dezip SIP in workspace
        final InputStream zipInputStreamSipObject =
            PropertiesUtils.getResourceAsStream(SIP_NO_FORMAT_NO_TAG);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP, zipInputStreamSipObject);
        // call processing
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
        //N.B : The old status StatusCode.KO is do to Invalid content in manifest (validation ko against old SEDA 2.0 of no FormatIdentification tag)
        //In Seda 2.1 this is authorize and test pass to warning result status
        assertEquals(StatusCode.WARNING, processWorkflow.getStatus());
    }



    @RunWithCustomExecutor
    @Test
    public void testWorkflowWithManifestIncorrectObjectNumber() throws Exception {
        prepareVitamSession();

        final String containerName = createOperationContainer();

        // workspace client dezip SIP in workspace
        final InputStream zipInputStreamSipObject =
            PropertiesUtils.getResourceAsStream(SIP_NB_OBJ_INCORRECT_IN_MANIFEST);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP, zipInputStreamSipObject);

        // call processing
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
    public void testWorkflowWithSipWithoutObject() throws Exception {
        prepareVitamSession();

        final String containerName = createOperationContainer();

        // workspace client dezip SIP in workspace
        final InputStream zipInputStreamSipObject =
            PropertiesUtils.getResourceAsStream(SIP_WITHOUT_OBJ);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP, zipInputStreamSipObject);
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(tenantId));

        // call processing
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
        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
        // checkMonitoring - meaning something has been added in the monitoring tool

    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowKOwithATRKOFilled() throws Exception {
        prepareVitamSession();

        final String containerName = createOperationContainer();

        // workspace client dezip SIP in workspace
        final InputStream zipInputStreamSipObject =
            PropertiesUtils.getResourceAsStream(SIP_WITHOUT_FUND_REGISTER);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP, zipInputStreamSipObject);

        // call processing
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
        prepareVitamSession();

        final String containerName = createOperationContainer();

        // workspace client dezip SIP in workspace
        final InputStream zipInputStreamSipObject =
            PropertiesUtils.getResourceAsStream(SIP_BORD_AU_REF_PHYS_OBJECT);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP, zipInputStreamSipObject);

        // call processing
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
    public void testworkFlowAudit() throws Exception {
        // Given
        prepareVitamSession();


        String containerName = createOperationContainer();

        processingClient = ProcessingManagementClientFactory.getInstance().getClient();

        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);

        workspaceClient
            .putObject(containerName, "query.json", JsonHandler.writeToInpustream(new Select().getFinalSelect()));

        processingClient.initVitamProcess(Contexts.EVIDENCE_AUDIT.name(), containerName, "EVIDENCE_AUDIT");
        // When
        RequestResponse<JsonNode> jsonNodeRequestResponse =
            processingClient.executeOperationProcess(containerName, "EVIDENCE_AUDIT",
                Contexts.EVIDENCE_AUDIT.name(), ProcessAction.RESUME.getValue());


        assertNotNull(jsonNodeRequestResponse);
        assertEquals(Status.ACCEPTED.getStatusCode(), jsonNodeRequestResponse.getStatus());

        wait(containerName);

        ProcessWorkflow processWorkflow =
            processMonitoring.findOneProcessWorkflow(containerName, tenantId);
        assertNotNull(processWorkflow);

        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
    }

    @Test
    @RunWithCustomExecutor
    public void should_export_dip() throws Exception {
        // Given
        prepareVitamSession();


        String containerName = createOperationContainer();

        WorkspaceClient workspaceClient = WorkspaceClientFactory.getInstance().getClient();

        // upload SIP
        final InputStream zipInputStreamSipObject =
            PropertiesUtils.getResourceAsStream(SIP_FUND_REGISTER_OK);
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP,
            zipInputStreamSipObject);
        // call processing

        processingClient = ProcessingManagementClientFactory.getInstance().getClient();
        processingClient.initVitamProcess(Contexts.DEFAULT_WORKFLOW.name(), containerName, WORFKLOW_NAME);
        RequestResponse<ItemStatus> ret =
            processingClient.updateOperationActionProcess(ProcessAction.RESUME.getValue(), containerName);
        assertNotNull(ret);

        assertEquals(Response.Status.ACCEPTED.getStatusCode(), ret.getStatus());

        wait(containerName);

        Select select = new Select();
        CompareQuery eq = QueryHelper.eq("#operations", containerName);
        select.setQuery(eq);

        ObjectNode finalSelect = select.getFinalSelect();

        containerName = createOperationContainer("EXPORT_DIP", LogbookTypeProcess.EXPORT_DIP);

        workspaceClient.createContainer(containerName);

        workspaceClient.putObject(containerName, "query.json", JsonHandler.writeToInpustream(finalSelect));

        processingClient = ProcessingManagementClientFactory.getInstance().getClient();
        processingClient.initVitamProcess(Contexts.EXPORT_DIP.name(), containerName, "EXPORT_DIP");

        // When
        RequestResponse<JsonNode> jsonNodeRequestResponse =
            processingClient.executeOperationProcess(containerName, WORFKLOW_NAME,
                Contexts.DEFAULT_WORKFLOW.name(), ProcessAction.RESUME.getValue());

        // Then
        assertNotNull(jsonNodeRequestResponse);
        assertEquals(Response.Status.ACCEPTED.getStatusCode(), jsonNodeRequestResponse.getStatus());

        wait(containerName);
        ProcessWorkflow processWorkflow =
            processMonitoring.findOneProcessWorkflow(containerName, tenantId);
        assertNotNull(processWorkflow);
        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
        assertThat(processWorkflow.getStatus()).isEqualTo(StatusCode.OK);
    }


    // Attach AU to an existing AU by systemId = guid
    @RunWithCustomExecutor
    @Test
    public void testWorkflowAddAndLinkSIP() throws Exception {
        prepareVitamSession();

        // 1. First we create an AU by sip (Tree)
        final String containerName = createOperationContainer();

        // workspace client dezip SIP in workspace
        final InputStream zipInputStreamSipObject =
            PropertiesUtils.getResourceAsStream(OK_RATTACHEMENT);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP,
            zipInputStreamSipObject);

        // call processing
        processingClient = ProcessingManagementClientFactory.getInstance().getClient();
        processingClient.initVitamProcess(Contexts.DEFAULT_WORKFLOW.name(), containerName, INGEST_TREE_WORFKLOW);
        final RequestResponse<JsonNode> ret =
            processingClient.executeOperationProcess(containerName, INGEST_TREE_WORFKLOW,
                Contexts.DEFAULT_WORKFLOW.name(), ProcessAction.RESUME.getValue());

        assertNotNull(ret);
        assertEquals(Status.ACCEPTED.getStatusCode(), ret.getStatus());

        wait(containerName);
        ProcessWorkflow processWorkflow =
            processMonitoring.findOneProcessWorkflow(containerName, tenantId);
        assertNotNull(processWorkflow);
        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
        assertEquals(StatusCode.OK, processWorkflow.getStatus());


        // 2. First we create another AU by sip (Tree)
        final String containerName2 = createOperationContainer();
        final InputStream zipInputStreamSipObject2 =
            PropertiesUtils.getResourceAsStream(OK_RATTACHEMENT);
        workspaceClient.createContainer(containerName2);
        workspaceClient.uncompressObject(containerName2, SIP_FOLDER, CommonMediaType.ZIP,
            zipInputStreamSipObject2);
        // call processing
        processingClient.initVitamProcess(Contexts.DEFAULT_WORKFLOW.name(), containerName2, INGEST_TREE_WORFKLOW);
        final RequestResponse<JsonNode> ret2 =
            processingClient.executeOperationProcess(containerName2, INGEST_TREE_WORFKLOW,
                Contexts.DEFAULT_WORKFLOW.name(), ProcessAction.RESUME.getValue());
        assertNotNull(ret2);
        assertEquals(Status.ACCEPTED.getStatusCode(), ret2.getStatus());

        wait(containerName2);
        processWorkflow =
            processMonitoring.findOneProcessWorkflow(containerName2, tenantId);
        assertNotNull(processWorkflow);
        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
        assertEquals(StatusCode.OK, processWorkflow.getStatus());

        // 3. we get id of both au from 1 and 2
        MongoIterable<Document> resultUnits = MetadataCollections.UNIT.getCollection().find();
        MongoCursor<Document> cursor = resultUnits.iterator();
        Document unit1 = null;
        Document unit2 = null;
        if (cursor.hasNext()) {
            unit1 = cursor.next();
        }
        if (cursor.hasNext()) {
            unit2 = cursor.next();
        }
        assertNotNull(unit1);
        assertNotNull(unit2);
        String idUnit = (String) unit1.get("_id");
        String idUnit2 = (String) unit2.get("_id");

        // 4. creation of 2 zip files : 1 containing id1, the other one containing id2
        String zipPath = null;
        String zipName = ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE - 1) + ".zip";

        replaceStringInFile(SIP_FILE_ADD_AU_LINK_OK_NAME + "/manifest.xml", "(?<=<SystemId>).*?(?=</SystemId>)",
            idUnit);
        zipPath = PropertiesUtils.getResourcePath(SIP_FILE_ADD_AU_LINK_OK_NAME_TARGET).toAbsolutePath().toString() +
            "/" + zipName;
        zipFolder(PropertiesUtils.getResourcePath(SIP_FILE_ADD_AU_LINK_OK_NAME), zipPath);

        // we now create another zip file that will contain an incorrect GUID
        String zipPath2 = null;
        // 2. then we link another SIP to it
        String zipName2 = ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE - 1) + "1.zip";
        replaceStringInFile(SIP_FILE_ADD_AU_LINK_OK_NAME + "/manifest.xml", "(?<=<SystemId>).*?(?=</SystemId>)",
            idUnit2);
        zipPath2 = PropertiesUtils.getResourcePath(SIP_FILE_ADD_AU_LINK_OK_NAME_TARGET).toAbsolutePath().toString() +
            "/" + zipName2;
        zipFolder(PropertiesUtils.getResourcePath(SIP_FILE_ADD_AU_LINK_OK_NAME), zipPath2);
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(tenantId));

        // 5. we now update the ingest contract, we set the check to ACTIVE and the link parent id takes id1 value
        updateIngestContractLinkParentId("ArchivalAgreement0", idUnit, "ACTIVE");


        // 6. ingest here should be ok, we link the correct id (referenced in the ingest contract) to the sip
        final String containerName3 = createOperationContainer();

        // workspace client dezip SIP in workspace
        // use link sip
        final InputStream zipStream = new FileInputStream(new File(
            PropertiesUtils.getResourcePath(SIP_FILE_ADD_AU_LINK_OK_NAME_TARGET).toAbsolutePath() +
                "/" + zipName));

        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName3);

        workspaceClient.uncompressObject(containerName3, SIP_FOLDER, CommonMediaType.ZIP,
            zipStream);

        // call processing
        processingClient.initVitamProcess(Contexts.DEFAULT_WORKFLOW.name(), containerName3, WORFKLOW_NAME);
        final RequestResponse<JsonNode> ret3 =
            processingClient.executeOperationProcess(containerName3, WORFKLOW_NAME,
                Contexts.DEFAULT_WORKFLOW.name(), ProcessAction.RESUME.getValue());

        assertNotNull(ret3);
        assertEquals(Status.ACCEPTED.getStatusCode(), ret3.getStatus());

        wait(containerName3);
        ProcessWorkflow processWorkflow3 = processMonitoring.findOneProcessWorkflow(containerName3, tenantId);
        assertNotNull(processWorkflow3);
        assertEquals(ProcessState.COMPLETED, processWorkflow3.getState());
        assertEquals(StatusCode.WARNING, processWorkflow3.getStatus());
        assertNotNull(processWorkflow3.getSteps());


        // 6. ingest here should be KO, we link an incorrect id (not a child of the referenced au in the ingest contract) into the sip        
        final String containerName4 = createOperationContainer();
        // use link sip
        final InputStream zipStream2 = new FileInputStream(new File(
            PropertiesUtils.getResourcePath(SIP_FILE_ADD_AU_LINK_OK_NAME_TARGET).toAbsolutePath() +
                "/" + zipName2));
        workspaceClient.createContainer(containerName4);
        workspaceClient.uncompressObject(containerName4, SIP_FOLDER, CommonMediaType.ZIP,
            zipStream2);
        // call processing
        processingClient.initVitamProcess(Contexts.DEFAULT_WORKFLOW.name(), containerName4, WORFKLOW_NAME);
        final RequestResponse<JsonNode> ret4 =
            processingClient.executeOperationProcess(containerName4, WORFKLOW_NAME,
                Contexts.DEFAULT_WORKFLOW.name(), ProcessAction.RESUME.getValue());
        assertNotNull(ret4);
        assertEquals(Status.ACCEPTED.getStatusCode(), ret4.getStatus());
        wait(containerName4);
        ProcessWorkflow processWorkflow4 = processMonitoring.findOneProcessWorkflow(containerName4, tenantId);
        assertNotNull(processWorkflow4);
        assertEquals(ProcessState.COMPLETED, processWorkflow4.getState());
        assertEquals(StatusCode.KO, processWorkflow4.getStatus());

        // Check that we have an AU where in his up we have idUnit
        MongoIterable<Document> newChildUnit = MetadataCollections.UNIT.getCollection().find(Filters.eq("_up", idUnit));
        assertNotNull(newChildUnit);
        assertNotNull(newChildUnit.first());
        MongoIterable<Document> operation =
            LogbookCollections.OPERATION.getCollection().find(Filters.eq("_id", containerName4));
        assertNotNull(operation);
        assertNotNull(operation.first());
        assertTrue(operation.first().toString().contains("CHECK_MANIFEST_WRONG_ATTACHMENT_LINK.KO"));

        // 7. we now put che check as inactive for the ingest contract
        updateIngestContractLinkParentId("ArchivalAgreement0", "", "INACTIVE");

        // 8. ingest here should be ok (warning), as check is inactive, we do what we want to do         
        final String containerName5 = createOperationContainer();
        // use link sip
        final InputStream zipStream3 = new FileInputStream(new File(
            PropertiesUtils.getResourcePath(SIP_FILE_ADD_AU_LINK_OK_NAME_TARGET).toAbsolutePath() +
                "/" + zipName2));
        workspaceClient.createContainer(containerName5);
        workspaceClient.uncompressObject(containerName5, SIP_FOLDER, CommonMediaType.ZIP,
            zipStream3);
        // call processing
        processingClient.initVitamProcess(Contexts.DEFAULT_WORKFLOW.name(), containerName5, WORFKLOW_NAME);
        final RequestResponse<JsonNode> ret5 =
            processingClient.executeOperationProcess(containerName5, WORFKLOW_NAME,
                Contexts.DEFAULT_WORKFLOW.name(), ProcessAction.RESUME.getValue());
        assertNotNull(ret5);
        assertEquals(Status.ACCEPTED.getStatusCode(), ret4.getStatus());
        wait(containerName5);
        ProcessWorkflow processWorkflow5 = processMonitoring.findOneProcessWorkflow(containerName5, tenantId);
        assertNotNull(processWorkflow5);
        assertEquals(ProcessState.COMPLETED, processWorkflow5.getState());
        assertEquals(StatusCode.WARNING, processWorkflow5.getStatus());

        try {
            Files.delete(new File(zipPath).toPath());
            Files.delete(new File(zipPath2).toPath());
            //Files.delete(new File(zipPath3).toPath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateIngestContractLinkParentId(String contractId, String linkParentId, String checkParentLink)
        throws Exception {
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            final UpdateParserSingle updateParserActive = new UpdateParserSingle(new SingleVarNameAdapter());
            final SetAction setLinkParentId = UpdateActionHelper.set(IngestContractModel.LINK_PARENT_ID, linkParentId);
            final SetAction setCheckParentLink =
                UpdateActionHelper.set(IngestContractModel.TAG_CHECK_PARENT_LINK, checkParentLink);
            final Update updateLinkParent = new Update();
            updateLinkParent.setQuery(QueryHelper.eq("Identifier", contractId));
            updateLinkParent.addActions(setLinkParentId, setCheckParentLink);
            updateParserActive.parse(updateLinkParent.getFinalUpdate());
            JsonNode queryDsl = updateParserActive.getRequest().getFinalUpdate();
            RequestResponse<IngestContractModel> requestResponse = client.updateIngestContract(contractId, queryDsl);
            assertTrue(requestResponse.isOk());
        }
    }


    private String createOperationContainer()
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException {
        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        final GUID objectGuid = GUIDFactory.newManifestGUID(tenantId);
        createLogbookOperation(operationGuid, objectGuid);

        return objectGuid.getId();
    }

    private String createOperationContainer(String action, LogbookTypeProcess logbookTypeProcess)
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException {
        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        final GUID objectGuid = GUIDFactory.newManifestGUID(tenantId);
        final String containerName = objectGuid.getId();
        createLogbookOperation(operationGuid, objectGuid, action, logbookTypeProcess);

        return containerName;
    }

    /**
     * This is a duplicate test for attaching AU to an existing GOT But we want this to test Attach AU by query to an
     * existing one As the query by #object return the wanted number of AU in results We first attach AU to an existing
     * GOT Then in the test of attach to existing AU by query (the query by #object return more than one= > KO)
     *
     * Why after simulateAttachUnitToExistingGOT the returned GOT have two AU
     *
     * @return The id GOT that should have two AU
     * @throws Exception
     */
    public void simulateAttachUnitToExistingGOT(String idGot, String zipName) throws Exception {

        replaceStringInFile(LINK_AU_TO_EXISTING_GOT_OK_NAME + "/manifest.xml",
            "(?<=<DataObjectGroupExistingReferenceId>).*?(?=</DataObjectGroupExistingReferenceId>)",
            idGot);

        String zipPath =
            PropertiesUtils.getResourcePath(LINK_AU_TO_EXISTING_GOT_OK_NAME_TARGET).toAbsolutePath().toString() +
                "/" + zipName;
        zipFolder(PropertiesUtils.getResourcePath(LINK_AU_TO_EXISTING_GOT_OK_NAME), zipPath);

        final String containerName2 = createOperationContainer();

        // workspace client dezip SIP in workspace
        // use link sip
        final InputStream zipStream = new FileInputStream(new File(
            PropertiesUtils.getResourcePath(LINK_AU_TO_EXISTING_GOT_OK_NAME_TARGET).toAbsolutePath() +
                "/" + zipName));

        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName2);
        workspaceClient.uncompressObject(containerName2, SIP_FOLDER, CommonMediaType.ZIP, zipStream);

        // call processing
        processingClient = ProcessingManagementClientFactory.getInstance().getClient();
        processingClient.initVitamProcess(Contexts.DEFAULT_WORKFLOW.name(), containerName2, WORFKLOW_NAME);
        final RequestResponse<JsonNode> ret2 =
            processingClient.executeOperationProcess(containerName2, WORFKLOW_NAME,
                Contexts.DEFAULT_WORKFLOW.name(), ProcessAction.RESUME.getValue());
        assertNotNull(ret2);
        assertEquals(Status.ACCEPTED.getStatusCode(), ret2.getStatus());

        wait(containerName2);
        ProcessWorkflow processWorkflow2 = processMonitoring.findOneProcessWorkflow(containerName2, tenantId);
        assertNotNull(processWorkflow2);
        assertEquals(ProcessState.COMPLETED, processWorkflow2.getState());
        assertEquals(StatusCode.WARNING, processWorkflow2.getStatus());
        assertNotNull(processWorkflow2.getSteps());
    }

    // Attach given AU to an existing one by Query
    @RunWithCustomExecutor
    @Test
    public void testAttachUnitToExistingUnitByQueryOKAndMultipleQueryKO() throws Exception {
        prepareVitamSession();

        // 1. First we create an AU by sip
        final String containerName = createOperationContainer();

        // workspace client dezip SIP in workspace
        final InputStream zipInputStreamSipObject =
            PropertiesUtils.getResourceAsStream(SIP_PROD_SERV_A);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP,
            zipInputStreamSipObject);
        // call processing
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

        String zipPath;
        // 2. then we link another SIP to it
        String zipName = ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE - 1) + ".zip";

        // prepare zip
        MongoIterable<Document> resultUnits = MetadataCollections.UNIT.getCollection().find();
        Document unit = resultUnits.first();
        String idUnit = (String) unit.get("_id");
        String idGOT = (String) unit.get("_og");
        assertThat(idGOT).isNotNull();

        // Search unit by #object: {$eq : idGOT}
        replaceStringInFile(SIP_FILE_ADD_AU_LINK_BY_QUERY_OK_NAME + "/manifest.xml",
            "(?<=<MetadataName>).*?(?=</MetadataName>)", "#object");
        replaceStringInFile(SIP_FILE_ADD_AU_LINK_BY_QUERY_OK_NAME + "/manifest.xml",
            "(?<=<MetadataValue>).*?(?=</MetadataValue>)", idGOT);
        zipPath = PropertiesUtils.getResourcePath(SIP_FILE_ADD_AU_LINK_OK_NAME_TARGET).toAbsolutePath().toString() +
            "/" + zipName;
        zipFolder(PropertiesUtils.getResourcePath(SIP_FILE_ADD_AU_LINK_BY_QUERY_OK_NAME), zipPath);


        final String containerName2 = createOperationContainer();

        // workspace client dezip SIP in workspace
        // use link sip
        InputStream zipStream = new FileInputStream(new File(
            PropertiesUtils.getResourcePath(SIP_FILE_ADD_AU_LINK_OK_NAME_TARGET).toAbsolutePath() +
                "/" + zipName));

        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName2);
        workspaceClient.uncompressObject(containerName2, SIP_FOLDER, CommonMediaType.ZIP,
            zipStream);

        // call processing
        processingClient = ProcessingManagementClientFactory.getInstance().getClient();
        processingClient.initVitamProcess(Contexts.DEFAULT_WORKFLOW.name(), containerName2, WORFKLOW_NAME);
        final RequestResponse<JsonNode> ret2 =
            processingClient.executeOperationProcess(containerName2, WORFKLOW_NAME,
                Contexts.DEFAULT_WORKFLOW.name(), ProcessAction.RESUME.getValue());
        assertNotNull(ret2);
        assertEquals(Status.ACCEPTED.getStatusCode(), ret2.getStatus());

        wait(containerName2);
        ProcessWorkflow processWorkflow2 = processMonitoring.findOneProcessWorkflow(containerName2, tenantId);
        assertNotNull(processWorkflow2);
        assertEquals(ProcessState.COMPLETED, processWorkflow2.getState());
        assertEquals(StatusCode.WARNING, processWorkflow2.getStatus());
        assertNotNull(processWorkflow2.getSteps());

        // Check that we have an AU where in his up we have idUnit
        MongoIterable<Document> newChildUnit = MetadataCollections.UNIT.getCollection().find(Filters.eq("_up", idUnit));
        assertNotNull(newChildUnit);
        assertNotNull(newChildUnit.first());



        // Get the GOT that have two AU by executing the method simulateAttachUnitToExistingGOT
        simulateAttachUnitToExistingGOT(idGOT, zipName);

        // Search unit by #object: {$eq : idGOT}
        // As we have already attached AU to this GOT then the query will return more than one. KO
        replaceStringInFile(SIP_FILE_ADD_AU_LINK_BY_QUERY_OK_NAME + "/manifest.xml",
            "(?<=<MetadataName>).*?(?=</MetadataName>)", "#object");
        replaceStringInFile(SIP_FILE_ADD_AU_LINK_BY_QUERY_OK_NAME + "/manifest.xml",
            "(?<=<MetadataValue>).*?(?=</MetadataValue>)", idGOT);
        zipPath = PropertiesUtils.getResourcePath(SIP_FILE_ADD_AU_LINK_OK_NAME_TARGET).toAbsolutePath().toString() +
            "/" + zipName;
        zipFolder(PropertiesUtils.getResourcePath(SIP_FILE_ADD_AU_LINK_BY_QUERY_OK_NAME), zipPath);

        final String containerName3 = createOperationContainer();

        // workspace client dezip SIP in workspace
        // use link sip
        zipStream = new FileInputStream(new File(
            PropertiesUtils.getResourcePath(SIP_FILE_ADD_AU_LINK_OK_NAME_TARGET).toAbsolutePath() +
                "/" + zipName));

        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName3);
        workspaceClient.uncompressObject(containerName3, SIP_FOLDER, CommonMediaType.ZIP,
            zipStream);

        // call processing
        processingClient = ProcessingManagementClientFactory.getInstance().getClient();
        processingClient.initVitamProcess(Contexts.DEFAULT_WORKFLOW.name(), containerName3, WORFKLOW_NAME);
        final RequestResponse<JsonNode> ret3 =
            processingClient.executeOperationProcess(containerName3, WORFKLOW_NAME,
                Contexts.DEFAULT_WORKFLOW.name(), ProcessAction.RESUME.getValue());
        assertNotNull(ret3);
        assertEquals(Status.ACCEPTED.getStatusCode(), ret3.getStatus());

        wait(containerName3);
        ProcessWorkflow processWorkflow3 = processMonitoring.findOneProcessWorkflow(containerName3, tenantId);
        assertNotNull(processWorkflow3);
        assertEquals(ProcessState.COMPLETED, processWorkflow3.getState());
        assertEquals(StatusCode.KO, processWorkflow3.getStatus());
        assertNotNull(processWorkflow3.getSteps());
        try {
            Files.delete(new File(zipPath).toPath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowAddAndLinkSIPWithNotValidKeyValueKo() throws Exception {

        prepareVitamSession();

        // We link to a non existing unit
        String zipPath = null;
        String zipName = ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE - 1) + ".zip";

        replaceStringInFile(SIP_FILE_ADD_AU_LINK_OK_NAME + "/manifest.xml", "(?<=<SystemId>).*?(?=</SystemId>)",
            ":GUID_ARCHIVE_UNIT_PARENT:");
        // prepare zip
        zipPath = PropertiesUtils.getResourcePath(SIP_FILE_ADD_AU_LINK_OK_NAME_TARGET).toAbsolutePath().toString() +
            "/" + zipName;
        zipFolder(PropertiesUtils.getResourcePath(SIP_FILE_ADD_AU_LINK_OK_NAME), zipPath);


        final String containerName = createOperationContainer();

        // workspace client dezip SIP in workspace
        // use link sip
        final InputStream zipStream = new FileInputStream(new File(
            PropertiesUtils.getResourcePath(SIP_FILE_ADD_AU_LINK_OK_NAME_TARGET).toAbsolutePath() +
                "/" + zipName));

        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP,
            zipStream);

        // call processing
        // /////
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


    @RunWithCustomExecutor
    @Test
    public void testWorkflowAddAndLinkSIPWithNotValidGUIDSystemIDKo() throws Exception {

        prepareVitamSession();

        // We link to a non existing unit
        String zipPath = null;
        String zipName = ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE - 1) + ".zip";

        replaceStringInFile(SIP_FILE_ADD_AU_LINK_OK_NAME + "/manifest.xml", "(?<=<SystemId>).*?(?=</SystemId>)",
            "GUID_ARCHIVE_UNIT_PARENT");
        // prepare zip
        zipPath = PropertiesUtils.getResourcePath(SIP_FILE_ADD_AU_LINK_OK_NAME_TARGET).toAbsolutePath().toString() +
            "/" + zipName;
        zipFolder(PropertiesUtils.getResourcePath(SIP_FILE_ADD_AU_LINK_OK_NAME), zipPath);


        final String containerName = createOperationContainer();

        // workspace client dezip SIP in workspace
        // use link sip
        final InputStream zipStream = new FileInputStream(new File(
            PropertiesUtils.getResourcePath(SIP_FILE_ADD_AU_LINK_OK_NAME_TARGET).toAbsolutePath() +
                "/" + zipName));

        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP,
            zipStream);

        // call processing
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
     * Test attach existing ObjectGroup to unit 1. Upload SIP 2. Get created GOT 3. Update manifest and set existing GOT
     * 4. Upload the new SIP
     *
     * @throws Exception
     */
    @RunWithCustomExecutor
    @Test
    public void testLinkUnitToExistingGOTOK() throws Exception {
        prepareVitamSession();

        // 1. First we create an AU by sip
        final String containerName = createOperationContainer();

        // workspace client dezip SIP in workspace
        final InputStream zipInputStreamSipObject =
            PropertiesUtils.getResourceAsStream(SIP_FILE_OK_NAME);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP, zipInputStreamSipObject);
        // call processing
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

        // 2. then we link another SIP to it
        String zipPath = null;
        String zipName = ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE - 1) + ".zip";

        // prepare zip
        MongoIterable<Document> resultUnits = MetadataCollections.UNIT.getCollection().find();
        Document unit = resultUnits.first();
        String idGot = (String) unit.get("_og");
        replaceStringInFile(LINK_AU_TO_EXISTING_GOT_OK_NAME + "/manifest.xml",
            "(?<=<DataObjectGroupExistingReferenceId>).*?(?=</DataObjectGroupExistingReferenceId>)",
            idGot);

        zipPath = PropertiesUtils.getResourcePath(LINK_AU_TO_EXISTING_GOT_OK_NAME_TARGET).toAbsolutePath().toString() +
            "/" + zipName;
        zipFolder(PropertiesUtils.getResourcePath(LINK_AU_TO_EXISTING_GOT_OK_NAME), zipPath);

        final String containerName2 = createOperationContainer();

        // workspace client dezip SIP in workspace
        // use link sip
        final InputStream zipStream = new FileInputStream(new File(
            PropertiesUtils.getResourcePath(LINK_AU_TO_EXISTING_GOT_OK_NAME_TARGET).toAbsolutePath() +
                "/" + zipName));

        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName2);
        workspaceClient.uncompressObject(containerName2, SIP_FOLDER, CommonMediaType.ZIP, zipStream);

        // call processing
        processingClient = ProcessingManagementClientFactory.getInstance().getClient();
        processingClient.initVitamProcess(Contexts.DEFAULT_WORKFLOW.name(), containerName2, WORFKLOW_NAME);
        final RequestResponse<JsonNode> ret2 =
            processingClient.executeOperationProcess(containerName2, WORFKLOW_NAME,
                Contexts.DEFAULT_WORKFLOW.name(), ProcessAction.RESUME.getValue());
        assertNotNull(ret2);
        assertEquals(Status.ACCEPTED.getStatusCode(), ret2.getStatus());

        wait(containerName2);
        ProcessWorkflow processWorkflow2 = processMonitoring.findOneProcessWorkflow(containerName2, tenantId);
        assertNotNull(processWorkflow2);
        assertEquals(ProcessState.COMPLETED, processWorkflow2.getState());
        assertEquals(StatusCode.WARNING, processWorkflow2.getStatus());
        assertNotNull(processWorkflow2.getSteps());

        // check got have to units
        assertEquals(MetadataCollections.UNIT.getCollection().count(Filters.eq("_og", idGot)), 2);

        ArrayList<Document> logbookLifeCycleUnits =
            Lists.newArrayList(LogbookCollections.LIFECYCLE_UNIT.getCollection().find().iterator());

        List<Document> currentLogbookLifeCycleUnits =
            logbookLifeCycleUnits.stream().filter(t -> t.get("evIdProc").equals(containerName2))
                .collect(Collectors.toList());

        List<Document> events = (List<Document>) Iterables.getOnlyElement(currentLogbookLifeCycleUnits).get("events");

        List<Document> lifeCycles = events.stream().filter(t -> t.get("outDetail").equals("LFC.CHECK_MANIFEST.OK"))
            .collect(Collectors.toList());
        assertThat(Iterables.getOnlyElement(lifeCycles).getString(EVENT_DETAILS)).containsIgnoringCase(idGot);



        ArrayList<Document> logbookLifeCycleGOTs =
            Lists.newArrayList(LogbookCollections.LIFECYCLE_OBJECTGROUP.getCollection().find().iterator());


        List<Document> currentLogbookLifeCycleGots =
            logbookLifeCycleGOTs.stream().filter(t -> t.get("evIdProc").equals(containerName))
                .collect(Collectors.toList());

        events = (List<Document>) Iterables.getOnlyElement(currentLogbookLifeCycleGots).get("events");


        lifeCycles = events.stream().filter(t -> t.get("outDetail").equals("LFC.OBJECT_GROUP_UPDATE.OK"))
            .collect(Collectors.toList());
        assertThat(lifeCycles).hasSize(1);
        assertThat(Iterables.getOnlyElement(lifeCycles).getString(EVENT_DETAILS)).containsIgnoringCase("diff");


        try {
            Files.delete(new File(zipPath).toPath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Test attach existing ObjectGroup to unit, but guid of the existing got is fake and really exists
     *
     * @throws Exception
     */
    @RunWithCustomExecutor
    @Test
    public void testLinkUnitToExistingGOTFakeGuidKO() throws Exception {
        prepareVitamSession();

        String zipName = ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE - 1) + ".zip";

        // prepare zip
        String idGot = "aecaaaaaachwwr22aaudeak5ouo22jyaaaaq";
        replaceStringInFile(LINK_AU_TO_EXISTING_GOT_OK_NAME + "/manifest.xml",
            "(?<=<DataObjectGroupExistingReferenceId>).*?(?=</DataObjectGroupExistingReferenceId>)",
            idGot);
        zipFolder(PropertiesUtils.getResourcePath(LINK_AU_TO_EXISTING_GOT_OK_NAME),
            PropertiesUtils.getResourcePath(LINK_AU_TO_EXISTING_GOT_OK_NAME_TARGET).toAbsolutePath().toString() +
                "/" + zipName);


        final String containerName = createOperationContainer();

        // workspace client dezip SIP in workspace
        // use link sip
        final InputStream zipStream = new FileInputStream(new File(
            PropertiesUtils.getResourcePath(LINK_AU_TO_EXISTING_GOT_OK_NAME_TARGET).toAbsolutePath() +
                "/" + zipName));

        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP, zipStream);

        // call processing
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
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException {
        createLogbookOperation(operationId, objectId, null, LogbookTypeProcess.INGEST);
    }

    public void createLogbookOperation(GUID operationId, GUID objectId, String type, LogbookTypeProcess typeProc)
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException {

        final LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient();
        if (type == null) {
            type = "Process_SIP_unitary";
        }

        final LogbookOperationParameters initParameters = LogbookParametersFactory.newLogbookOperationParameters(
            operationId, type, objectId,
            typeProc, StatusCode.STARTED,
            operationId != null ? operationId.toString() : "outcomeDetailMessage",
            operationId);
        if ("EXPORT_DIP".equals(type)) {
            initParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                VitamLogbookMessages.getLabelOp("EXPORT_DIP.STARTED") + " : " + operationId);
        }
        logbookClient.create(initParameters);
    }

    @RunWithCustomExecutor
    @Ignore
    @Test
    public void testBigWorkflow() throws Exception {
        prepareVitamSession();

        // re-launch worker
        runner.stopWorkerServer();
        runner.startWorkerServer(CONFIG_BIG_WORKER_PATH);
        final String containerName = createOperationContainer();
        // workspace client dezip SIP in workspace
        final InputStream zipInputStreamSipObject =
            PropertiesUtils.getResourceAsStream(SIP_FILE_OK_NAME);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP,
            zipInputStreamSipObject);
        // call processing
        processingClient = ProcessingManagementClientFactory.getInstance().getClient();
        processingClient.initVitamProcess(Contexts.DEFAULT_WORKFLOW.name(), containerName, BIG_WORFKLOW_NAME);

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

        runner.stopWorkerServer();
        runner.startWorkerServer(runner.CONFIG_WORKER_PATH);
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowIncorrectManifestReference() throws Exception {

        prepareVitamSession();

        final String containerName = createOperationContainer();

        // workspace client dezip SIP in workspace
        final InputStream zipInputStreamSipObject =
            PropertiesUtils.getResourceAsStream(SIP_MANIFEST_INCORRECT_REFERENCE);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP, zipInputStreamSipObject);

        // call processing
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
    @Ignore
    public void testWorkerUnRegister() throws Exception {
        prepareVitamSession();

        // 1. Stop the worker this will unregister the worker
        runner.stopWorkerServer();
        Thread.sleep(500);

        // 2. Start the worker this will register the worker
        runner.startWorkerServer(runner.CONFIG_WORKER_PATH);
        Thread.sleep(500);

        // 3. Stop processing, this will make worker retry register
        runner.stopProcessManagementServer(false);

        runner.startProcessManagementServer();

        // For test, worker.conf is modified to have registerDelay: 1 (mean every one second worker try to register
        // it self
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowBug2182() throws Exception {
        prepareVitamSession();

        final String containerName = createOperationContainer();

        // workspace client dezip SIP in workspace
        final InputStream zipInputStreamSipObject =
            PropertiesUtils.getResourceAsStream(SIP_BUG_2182);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP, zipInputStreamSipObject);

        // call processing
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
        prepareVitamSession();

        final String containerName = createOperationContainer();

        // workspace client dezip SIP in workspace
        final InputStream zipInputStreamSipObject =
            PropertiesUtils.getResourceAsStream(SIP_FILE_KO_AU_REF_BDO);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP, zipInputStreamSipObject);

        // call processing
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
        prepareVitamSession();

        final String containerName = createOperationContainer();

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

        assertNotNull(dataManagement.getProcessWorkflow(VitamConfiguration.getWorkspaceWorkflowsFolder(),
            containerName));

        RequestResponse<ItemStatus> ret =
            processingClient.updateOperationActionProcess(ProcessAction.NEXT.getValue(),
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
            dataManagement.getProcessWorkflow(VitamConfiguration.getWorkspaceWorkflowsFolder(), containerName);
        } catch (ProcessingStorageWorkspaceException e) {
            exc = true;
        }

        // TODO the #2627 the workflow is not removed from workspace until 24h
        // assertTrue(exc);
    }


    @RunWithCustomExecutor
    @Test
    public void testWorkflowJsonValidationKOCA1() throws Exception {
        prepareVitamSession();

        final String containerName = createOperationContainer();

        // workspace client dezip SIP in workspace
        final InputStream zipInputStreamSipObject =
            PropertiesUtils.getResourceAsStream(SIP_FILE_1791_CA1);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP,
            zipInputStreamSipObject);
        // call processing
        processingClient = ProcessingManagementClientFactory.getInstance().getClient();
        processingClient.initVitamProcess(Contexts.DEFAULT_WORKFLOW.name(), containerName, WORFKLOW_NAME);

        RequestResponse<ItemStatus> ret =
            processingClient.updateOperationActionProcess(ProcessAction.RESUME.getValue(), containerName);
        assertNotNull(ret);
        assertEquals(Status.ACCEPTED.getStatusCode(), ret.getStatus());

        wait(containerName);
        ProcessWorkflow processWorkflow = processMonitoring.findOneProcessWorkflow(containerName, tenantId);
        assertThat(processWorkflow).isNotNull();
        assertThat(processWorkflow.getState()).isEqualTo(ProcessState.COMPLETED);
        assertThat(processWorkflow.getStatus()).isEqualTo(StatusCode.KO);
    }


    @RunWithCustomExecutor
    @Test
    public void testWorkflowJsonValidationKOCA2() throws Exception {
        prepareVitamSession();

        final String containerName = createOperationContainer();

        // workspace client dezip SIP in workspace
        final InputStream zipInputStreamSipObject =
            PropertiesUtils.getResourceAsStream(SIP_FILE_1791_CA2);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP,
            zipInputStreamSipObject);
        // call processing
        processingClient = ProcessingManagementClientFactory.getInstance().getClient();
        processingClient.initVitamProcess(Contexts.DEFAULT_WORKFLOW.name(), containerName, WORFKLOW_NAME);


        RequestResponse<ItemStatus> ret =
            processingClient.updateOperationActionProcess(ProcessAction.RESUME.getValue(), containerName);
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
    public void testWorkflowWithContractKO() throws Exception {
        prepareVitamSession();

        final String containerName = createOperationContainer();

        // workspace client dezip SIP in workspace
        final InputStream zipInputStreamSipObject =
            PropertiesUtils.getResourceAsStream(SIP_REFERENCE_CONTRACT_KO);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP,
            zipInputStreamSipObject);
        // call processing
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
    public void testWorkflowSIPContractProdService() throws Exception {
        prepareVitamSession();

        final String containerName = createOperationContainer();

        // workspace client dezip SIP in workspace
        final InputStream zipInputStreamSipObject =
            PropertiesUtils.getResourceAsStream(SIP_FILE_OK_NAME);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP,
            zipInputStreamSipObject);
        // call processing
        processingClient = ProcessingManagementClientFactory.getInstance().getClient();
        processingClient.initVitamProcess(Contexts.DEFAULT_WORKFLOW.name(), containerName, WORFKLOW_NAME);

        RequestResponse<ItemStatus> ret =
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

        // as logbookClient.selectOperation returns last two events and after removing STARTED from events
        // the order is main-event > sub-events, so events[0] will be "ROLL_BACK.OK" and not
        // "STP_INGEST_FINALISATION.OK"
        assertEquals(logbookResult.get("$results").get(0).get("events").get(0).get("outDetail").asText(),
            "ROLL_BACK.OK");
        assertEquals(logbookResult.get("$results").get(0).get("events").get(1).get("outDetail").asText(),
            "PROCESS_SIP_UNITARY.WARNING");
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowIngestBigTreeBugFix3062() throws Exception {
        prepareVitamSession();

        final String containerName = createOperationContainer();

        // workspace client dezip SIP in workspace
        final InputStream zipInputStreamSipObject =
            PropertiesUtils.getResourceAsStream(SIP_ARBRE_3062);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP,
            zipInputStreamSipObject);
        // call processing
        processingClient = ProcessingManagementClientFactory.getInstance().getClient();
        processingClient.initVitamProcess(Contexts.HOLDING_SCHEME.name(), containerName, INGEST_TREE_WORFKLOW);
        RequestResponse<ItemStatus> ret =
            processingClient.updateOperationActionProcess(ProcessAction.RESUME.getValue(), containerName);
        assertNotNull(ret);

        assertEquals(Status.ACCEPTED.getStatusCode(), ret.getStatus());

        wait(containerName);
        ProcessWorkflow processWorkflow =
            processMonitoring.findOneProcessWorkflow(containerName, tenantId);
        assertNotNull(processWorkflow);
        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
        assertEquals(StatusCode.OK, processWorkflow.getStatus());
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowOkSIPSignature() throws Exception {
        prepareVitamSession();

        final String containerName = createOperationContainer();

        // workspace client dezip SIP in workspace
        final InputStream zipInputStreamSipObject =
            PropertiesUtils.getResourceAsStream(OK_SIP_SIGNATURE);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP, zipInputStreamSipObject);

        // call processing
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
        assertEquals(StatusCode.OK, processWorkflow.getStatus());
    }



    @RunWithCustomExecutor
    @Test
    public void testWorkflowRulesUpdate() throws Exception {
        prepareVitamSession();

        final String containerName2 = createOperationContainer();
        final InputStream zipInputStreamSipObject =
            PropertiesUtils.getResourceAsStream(SIP_COMPLEX_RULES);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName2);
        workspaceClient.uncompressObject(containerName2, SIP_FOLDER, CommonMediaType.ZIP,
            zipInputStreamSipObject);

        // call processing
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

        final String containerName = createOperationContainer();

        // put rules into workspace
        final InputStream rulesStream =
            PropertiesUtils.getResourceAsStream("integration-processing/RULES.json");
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);
        workspaceClient.putObject(containerName,
            UpdateWorkflowConstants.PROCESSING_FOLDER + "/" + UpdateWorkflowConstants.UPDATED_RULES_JSON,
            rulesStream);
        // call processing
        processingClient.initVitamProcess(Contexts.UPDATE_RULES_ARCHIVE_UNITS.name(),
            containerName, UPD8_AU_WORKFLOW);
        RequestResponse<ItemStatus> ret =
            processingClient.updateOperationActionProcess(ProcessAction.RESUME.getValue(), containerName);
        assertNotNull(ret);

        assertEquals(Status.ACCEPTED.getStatusCode(), ret.getStatus());


        wait(containerName);
        ProcessWorkflow processWorkflow =
            processMonitoring.findOneProcessWorkflow(containerName, tenantId);
        assertNotNull(processWorkflow);
        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
        assertEquals(StatusCode.OK, processWorkflow.getStatus());

        ArrayList<Document> logbookLifeCycleUnits =
            Lists.newArrayList(LogbookCollections.LIFECYCLE_UNIT.getCollection().find().iterator());

        List<Document> currentLogbookLifeCycleUnits =
            logbookLifeCycleUnits.stream().filter(t -> t.get("evIdProc").equals(containerName2))
                .collect(Collectors.toList());
        currentLogbookLifeCycleUnits.forEach((lifecycle) -> {
            List<Document> events = (List<Document>) lifecycle.get("events");
            List<Document> lifecycleEvent =
                events.stream().filter(t -> t.get("outDetail").equals("LFC.UPDATE_UNIT_RULES.OK"))
                    .collect(Collectors.toList());
            if (lifecycleEvent != null && lifecycleEvent.size() > 0) {
                String evDetData = Iterables.getOnlyElement(lifecycleEvent).getString(EVENT_DETAILS);
                assertThat(evDetData).containsIgnoringCase("diff");
                assertThat(evDetData).contains(containerName2);
                assertThat(Iterables.getOnlyElement(lifecycleEvent).getString("outMessg")).isEqualTo(
                    "Succès de la mise à jour des règles de gestion de l'unité archivistique");
            }
        });
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowAddAttachementAndCheckRegister() throws Exception {
        prepareVitamSession();
        // 1. First we create an AU by sip
        final String containerName = createOperationContainer();

        final InputStream zipInputStreamSipObject =
            PropertiesUtils.getResourceAsStream(SIP_PROD_SERV_A);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP,
            zipInputStreamSipObject);
        // call processing
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(tenantId));

        processingClient = ProcessingManagementClientFactory.getInstance().getClient();
        processingClient.initVitamProcess(Contexts.DEFAULT_WORKFLOW.name(), containerName, WORFKLOW_NAME);
        final RequestResponse<JsonNode> ret =
            processingClient.executeOperationProcess(containerName, WORFKLOW_NAME,
                Contexts.DEFAULT_WORKFLOW.name(), ProcessAction.RESUME.getValue());

        assertNotNull(ret);

        assertEquals(Status.ACCEPTED.getStatusCode(), ret.getStatus());
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(tenantId));


        wait(containerName);
        ProcessWorkflow processWorkflow =
            processMonitoring.findOneProcessWorkflow(containerName, tenantId);
        assertNotNull(processWorkflow);
        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
        assertEquals(StatusCode.WARNING, processWorkflow.getStatus());

        String zipPath = null;
        // 2. then we link another SIP to it
        String zipName = ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE - 1) + ".zip";

        // prepare zip
        MongoIterable<Document> resultUnits = MetadataCollections.UNIT.getCollection().find();
        Document unit = resultUnits.first();
        String idUnit = (String) unit.get("_id");
        String opiBefore = (String) unit.get("_opi");
        replaceStringInFile(SIP_PROD_SERV_B_ATTACHED + "/manifest.xml", "(?<=<SystemId>).*?(?=</SystemId>)",
            idUnit);
        zipPath = PropertiesUtils.getResourcePath(SIP_FILE_ADD_AU_LINK_OK_NAME_TARGET).toAbsolutePath().toString() +
            "/" + zipName;
        zipFolder(PropertiesUtils.getResourcePath(SIP_PROD_SERV_B_ATTACHED), zipPath);

        final String containerName2 = createOperationContainer();

        // use link sip
        final InputStream zipStream = new FileInputStream(new File(
            PropertiesUtils.getResourcePath(SIP_FILE_ADD_AU_LINK_OK_NAME_TARGET).toAbsolutePath() +
                "/" + zipName));

        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName2);
        workspaceClient.uncompressObject(containerName2, SIP_FOLDER, CommonMediaType.ZIP,
            zipStream);

        processingClient = ProcessingManagementClientFactory.getInstance().getClient();
        processingClient.initVitamProcess(Contexts.DEFAULT_WORKFLOW.name(), containerName2, WORFKLOW_NAME);
        final RequestResponse<JsonNode> ret2 =
            processingClient.executeOperationProcess(containerName2, WORFKLOW_NAME,
                Contexts.DEFAULT_WORKFLOW.name(), ProcessAction.RESUME.getValue());
        assertNotNull(ret2);
        assertEquals(Status.ACCEPTED.getStatusCode(), ret2.getStatus());

        wait(containerName2);
        ProcessWorkflow processWorkflow2 = processMonitoring.findOneProcessWorkflow(containerName2, tenantId);
        assertNotNull(processWorkflow2);
        assertEquals(ProcessState.COMPLETED, processWorkflow2.getState());
        assertEquals(StatusCode.WARNING, processWorkflow2.getStatus());
        assertNotNull(processWorkflow2.getSteps());

        MongoIterable<Document> resultUnitsAfter =
            MetadataCollections.UNIT.getCollection().find(Filters.eq("_id", idUnit));
        Document unitAfter = resultUnitsAfter.first();
        String opiAfter = (String) unitAfter.get("_opi");

        assertEquals(opiBefore, opiAfter);

        LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient();
        JsonNode logbookResult = logbookClient.selectOperationById(containerName2,
            new fr.gouv.vitam.common.database.builder.request.single.Select().getFinalSelect());
        assertNotNull(logbookResult.get("$results").get(0));
        LogbookOperation logOperation =
            JsonHandler.getFromJsonNode(logbookResult.get("$results").get(0), LogbookOperation.class);
        List<LogbookEventOperation> events = logOperation.getEvents().stream()
            .filter(p -> (p.getEvType().equals("ACCESSION_REGISTRATION") && p.getOutcome().equals("OK")))
            .collect(Collectors.toList());
        events.forEach((event) -> {
            assertNotNull(event.getEvDetData());
            try {
                assertNotNull(JsonHandler.getFromString(event.getEvDetData()).get("Volumetry"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        MongoIterable<Document> accessReg =
            FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getCollection()
                .find(Filters.eq("OriginatingAgency", "P-A"));
        assertNotNull(accessReg);
        assertNotNull(accessReg.first());
        Document accessRegDoc = accessReg.first();
        // 2 units are attached - 1 was previously added
        // TODO: have to review this double (have to be a long)
        assertEquals("2.0",
            ((Document) accessRegDoc.get("TotalUnits")).get(AccessionRegisterSummary.SYMBOLIC_REMAINED).toString());
        assertEquals("2.0",
            ((Document) accessRegDoc.get("TotalUnits")).get(AccessionRegisterSummary.ATTACHED).toString());
        assertEquals("1.0",
            ((Document) accessRegDoc.get("TotalUnits")).get(AccessionRegisterSummary.INGESTED).toString());
        assertEquals("1.0",
            ((Document) accessRegDoc.get("TotalUnits")).get(AccessionRegisterSummary.REMAINED).toString());

        // 1 object is attached - 1 was previously added
        assertEquals("1.0",
            ((Document) accessRegDoc.get("TotalObjects")).get(AccessionRegisterSummary.SYMBOLIC_REMAINED)
                .toString());
        assertEquals("1.0",
            ((Document) accessRegDoc.get("TotalObjects")).get(AccessionRegisterSummary.ATTACHED).toString());
        assertEquals("1.0",
            ((Document) accessRegDoc.get("TotalObjects")).get(AccessionRegisterSummary.INGESTED).toString());

        // 1 Got is attached - 1 was previously added
        assertEquals("1.0", ((Document) accessRegDoc.get("TotalObjectGroups"))
            .get(AccessionRegisterSummary.SYMBOLIC_REMAINED).toString());
        assertEquals("1.0",
            ((Document) accessRegDoc.get("TotalObjectGroups")).get(AccessionRegisterSummary.ATTACHED).toString());
        assertEquals("1.0",
            ((Document) accessRegDoc.get("TotalObjectGroups")).get(AccessionRegisterSummary.INGESTED).toString());

        // 285804 octets is attached - 4109 was previously added
        assertEquals("285804.0",
            ((Document) accessRegDoc.get("ObjectSize")).get(AccessionRegisterSummary.SYMBOLIC_REMAINED).toString());
        assertEquals("285804.0",
            ((Document) accessRegDoc.get("ObjectSize")).get(AccessionRegisterSummary.ATTACHED).toString());
        assertEquals("4109.0",
            ((Document) accessRegDoc.get("ObjectSize")).get(AccessionRegisterSummary.INGESTED).toString());

        try {
            Files.delete(new File(zipPath).toPath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testBlankWorkflow() throws Exception {
        prepareVitamSession();
        try (MetaDataClient metaDataClient = MetaDataClientFactory.getInstance().getClient();
            AdminManagementClient functionalClient = AdminManagementClientFactory.getInstance().getClient()) {

            final String containerName = createOperationContainer();

            // workspace client dezip SIP in workspace
            final InputStream zipInputStreamSipObject = PropertiesUtils.getResourceAsStream(SIP_BUG_2721);
            workspaceClient = WorkspaceClientFactory.getInstance().getClient();
            workspaceClient.createContainer(containerName);
            workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP,
                zipInputStreamSipObject);
            // call processing
            metaDataClient.insertUnit(
                new InsertMultiQuery()
                    .addData((ObjectNode) JsonHandler
                        .getFromFile(PropertiesUtils.getResourceFile("integration-processing/unit_metadata.json")))
                    .getFinalInsert());

            metaDataClient.insertUnit(
                new InsertMultiQuery()
                    .addData(
                        (ObjectNode) JsonHandler.getFromFile(PropertiesUtils.getResourceFile(PROCESSING_UNIT_PLAN)))
                    .getFinalInsert());

            metaDataClient.flushUnits();
            // import contract
            File fileContracts = PropertiesUtils.getResourceFile(INGEST_CONTRACTS_PLAN);
            List<IngestContractModel> IngestContractModelList =
                JsonHandler.getFromFileAsTypeRefence(fileContracts, new TypeReference<List<IngestContractModel>>() {
                });

            functionalClient.importIngestContracts(IngestContractModelList);

            processingClient = ProcessingManagementClientFactory.getInstance().getClient();
            // Testing blank workflow
            processingClient.initVitamProcess(Contexts.BLANK_TEST.name(), containerName, BLANK_WORKFLOW_NAME);

            RequestResponse<ItemStatus> ret =
                processingClient.updateOperationActionProcess(ProcessAction.RESUME.getValue(), containerName);
            assertNotNull(ret);
            assertEquals(Status.ACCEPTED.getStatusCode(), ret.getStatus());

            wait(containerName);
            ProcessWorkflow processWorkflow = processMonitoring.findOneProcessWorkflow(containerName, tenantId);
            assertNotNull(processWorkflow);
            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
            assertEquals(StatusCode.WARNING, processWorkflow.getStatus());
        }
    }


    @RunWithCustomExecutor
    @Test
    public void testValidateArchiveUnitSchemaBirthPlaceOK() throws Exception {
        prepareVitamSession();

        final String containerName = createOperationContainer();

        // workspace client dezip SIP in workspace
        final InputStream zipInputStreamSipObject =
            PropertiesUtils.getResourceAsStream(SIP_FILE_OK_BIRTH_PLACE);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP,
            zipInputStreamSipObject);

        processingClient = ProcessingManagementClientFactory.getInstance().getClient();
        processingClient.initVitamProcess(Contexts.DEFAULT_WORKFLOW.name(), containerName,
            WORFKLOW_NAME);
        // wait a little bit
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(tenantId));

        RequestResponse<JsonNode> resp = processingClient.executeOperationProcess(containerName, WORFKLOW_NAME,
            LogbookTypeProcess.INGEST.toString(), ProcessAction.RESUME.getValue());
        // wait a little bit
        assertNotNull(resp);

        assertEquals(Response.Status.ACCEPTED.getStatusCode(), resp.getStatus());

        wait(containerName);
        ProcessWorkflow processWorkflow =
            ProcessMonitoringImpl.getInstance().findOneProcessWorkflow(containerName, tenantId);

        assertNotNull(processWorkflow);
        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
        assertEquals(StatusCode.WARNING, processWorkflow.getStatus());
    }


    @RunWithCustomExecutor
    @Test
    public void testIgestWithWrongDateShouldEndWithKO() throws Exception {
        prepareVitamSession();

        final String containerName = createOperationContainer();

        // workspace client dezip SIP in workspace
        final InputStream zipInputStreamSipObject =
            PropertiesUtils.getResourceAsStream(SIP_FILE_WRONG_DATE);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP,
            zipInputStreamSipObject);

        processingClient = ProcessingManagementClientFactory.getInstance().getClient();
        processingClient.initVitamProcess(Contexts.DEFAULT_WORKFLOW.name(), containerName, WORFKLOW_NAME);
        // wait a little bit

        RequestResponse<JsonNode> resp = processingClient
            .executeOperationProcess(containerName, WORFKLOW_NAME, LogbookTypeProcess.INGEST.toString(),
                ProcessAction.RESUME.getValue());
        // wait a little bit
        assertNotNull(resp);
        assertEquals(Response.Status.ACCEPTED.getStatusCode(), resp.getStatus());

        wait(containerName);

        ProcessWorkflow processWorkflow =
            ProcessMonitoringImpl.getInstance().findOneProcessWorkflow(containerName, tenantId);

        assertNotNull(processWorkflow);
        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
        assertEquals(StatusCode.KO, processWorkflow.getStatus());


    }

    @RunWithCustomExecutor
    @Test
    public void testIngestWithAURefObjShouldEndWithKO() throws Exception {
        prepareVitamSession();

        final String containerName = createOperationContainer();

        // workspace client dezip SIP in workspace
        final InputStream zipInputStreamSipObject =
            PropertiesUtils.getResourceAsStream(SIP_KO_AU_REF_OBJ);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP,
            zipInputStreamSipObject);

        processingClient = ProcessingManagementClientFactory.getInstance().getClient();
        processingClient.initVitamProcess(Contexts.DEFAULT_WORKFLOW.name(), containerName, WORFKLOW_NAME);
        // wait a little bit

        RequestResponse<JsonNode> resp = processingClient
            .executeOperationProcess(containerName, WORFKLOW_NAME, LogbookTypeProcess.INGEST.toString(),
                ProcessAction.RESUME.getValue());
        // wait a little bit
        assertNotNull(resp);
        assertEquals(Response.Status.ACCEPTED.getStatusCode(), resp.getStatus());

        wait(containerName);

        ProcessWorkflow processWorkflow =
            ProcessMonitoringImpl.getInstance().findOneProcessWorkflow(containerName, tenantId);

        assertNotNull(processWorkflow);
        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
        assertEquals(StatusCode.KO, processWorkflow.getStatus());


    }

    @RunWithCustomExecutor
    @Test
    public void testIngestWithWrongUriShouldEndWithKO() throws Exception {
        prepareVitamSession();

        final String containerName = createOperationContainer();

        // workspace client dezip SIP in workspace
        final InputStream zipInputStreamSipObject =
            PropertiesUtils.getResourceAsStream(SIP_KO_MANIFEST_URI);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP,
            zipInputStreamSipObject);

        processingClient = ProcessingManagementClientFactory.getInstance().getClient();
        processingClient.initVitamProcess(Contexts.DEFAULT_WORKFLOW.name(), containerName, WORFKLOW_NAME);
        // wait a little bit

        RequestResponse<JsonNode> resp = processingClient
            .executeOperationProcess(containerName, WORFKLOW_NAME, LogbookTypeProcess.INGEST.toString(),
                ProcessAction.RESUME.getValue());
        // wait a little bit
        assertNotNull(resp);
        assertEquals(Response.Status.ACCEPTED.getStatusCode(), resp.getStatus());

        wait(containerName);

        ProcessWorkflow processWorkflow =
            ProcessMonitoringImpl.getInstance().findOneProcessWorkflow(containerName, tenantId);

        assertNotNull(processWorkflow);
        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
        assertEquals(StatusCode.KO, processWorkflow.getStatus());

    }

    @RunWithCustomExecutor
    @Test
    public void should_not_insert_empty_arrayNode_in_appraisal_rule_when_ingest_SIP() throws Exception {
        prepareVitamSession();

        // 1. First we create an AU by sip
        final String containerName = createOperationContainer();

        // workspace client dezip SIP in workspace
        final InputStream zipInputStreamSipObject =
            PropertiesUtils.getResourceAsStream(SIP_APPRAISAL_RULES);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP,
            zipInputStreamSipObject);
        // call processing
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
        assertEquals(StatusCode.OK, processWorkflow.getStatus());

        MongoIterable<Document> resultUnits =
            MetadataCollections.UNIT.getCollection().find(Filters.eq("Title", "Porte de Pantin"));
        final Document unitToAssert = resultUnits.first();
        Document appraisalRule = ((Document) ((Document) unitToAssert.get("_mgt")).get("AppraisalRule"));
        final List<Object> rules = ((List<Object>) appraisalRule.get("Rules"));
        final String finalAction = (String) appraisalRule.get("FinalAction");
        assertThat(finalAction).isNotNull().isEqualTo("Keep");
        assertThat(rules).isNull();

    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowSipSeda2_1_full() throws Exception {
        prepareVitamSession();

        final String containerName = createOperationContainer();

        // workspace client dezip SIP in workspace
        final InputStream zipInputStreamSipObject =
            PropertiesUtils.getResourceAsStream(SIP_FULL_SEDA_2_1);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP, zipInputStreamSipObject);
        // call processing
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

        MongoIterable<Document> resultUnits =
            MetadataCollections.UNIT.getCollection().find(Filters.eq("Title", "monSIP"));
        final Document unitToAssert = resultUnits.first();

        //AgentType fullname field

        List<Object> addressees = (List<Object>) unitToAssert.get("Addressee");
        assertThat(addressees).isNotNull().isNotEmpty();

        Document addressee = (Document) addressees.get(0);
        assertThat(addressee.get("FullName")).isEqualTo("Iulius Caesar Divus");
        ;

        //sender
        List<Object> senders = (List<Object>) unitToAssert.get("Sender");
        assertThat(senders).isNotNull().isNotEmpty();

        Document sender = (Document) senders.get(0);
        final List<String> mandates = ((List<String>) sender.get("Mandate"));

        assertThat(sender.get("GivenName")).isEqualTo("Alexander");
        assertThat(mandates.size()).isEqualTo(2);
        assertThat(mandates.get(0)).isEqualTo("Mandataire_1");
        assertThat(mandates.get(1)).isEqualTo("Mandataire_2");

        //transmitter
        List<Object> transmitters = (List<Object>) unitToAssert.get("Transmitter");
        assertThat(senders).isNotNull().isNotEmpty();

        Document transmitter = (Document) transmitters.get(0);
        final List<String> functions = (List<String>) transmitter.get("Function");
        assertThat(functions).isNotNull().isNotEmpty();
        assertThat(functions.get(0)).isEqualTo("Service de transmission");

        //Content/IfTPz6AWS1VwRfNSlhsq83sMNPidvA.pdf
        MongoIterable<Document> gots = MetadataCollections.OBJECTGROUP.getCollection()
            .find(Filters.eq("_qualifiers.versions.Uri", "Content/IfTPz6AWS1VwRfNSlhsq83sMNPidvA.pdf"));
        final Document bdoWithMetadataJson = gots.first();

        List<Object> qualifiers = (List<Object>) bdoWithMetadataJson.get("_qualifiers");
        assertThat(qualifiers).isNotNull().isNotEmpty();

        List<Object> versions = (List<Object>) ((Document) qualifiers.get(0)).get("versions");
        assertThat(versions).isNotNull().isNotEmpty();
        Document version = (Document) versions.get(0);
        assertThat(version).isNotNull().isNotEmpty();
        Document fileInfo = (Document) version.get("FileInfo");
        assertNotNull(fileInfo);
        assertThat(fileInfo.get("LastModified")).isEqualTo("2016-06-03T15:28:00.000+02:00");
        assertThat(fileInfo.get("Filename")).isEqualTo("IfTPz6AWS1VwRfNSlhsq83sMNPidvA.pdf");

    }

}
