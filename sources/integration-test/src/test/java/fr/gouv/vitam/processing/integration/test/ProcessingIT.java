/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
package fr.gouv.vitam.processing.integration.test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.Streams;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.model.InsertOneModel;
import fr.gouv.vitam.batch.report.rest.BatchReportMain;
import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.DataLoader;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.client.VitamClientFactoryInterface;
import fr.gouv.vitam.common.database.builder.query.CompareQuery;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.query.action.SetAction;
import fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.GLOBAL;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTIONARGS;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.multiple.UpdateMultiQuery;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.builder.request.single.Update;
import fr.gouv.vitam.common.database.parser.request.adapter.SingleVarNameAdapter;
import fr.gouv.vitam.common.database.parser.request.single.UpdateParserSingle;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchIndexAlias;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.InternalServerException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.format.identification.FormatIdentifierFactory;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.model.GraphComputeResponse;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.ProcessPause;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.UpdateWorkflowConstants;
import fr.gouv.vitam.common.model.administration.AccessionRegisterSummaryModel;
import fr.gouv.vitam.common.model.administration.IngestContractCheckState;
import fr.gouv.vitam.common.model.administration.IngestContractModel;
import fr.gouv.vitam.common.server.HeaderIdHelper;
import fr.gouv.vitam.common.stream.StreamUtils;
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
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookOperation;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookTransformData;
import fr.gouv.vitam.logbook.common.server.exception.LogbookExecutionException;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.api.model.BulkUnitInsertEntry;
import fr.gouv.vitam.metadata.api.model.BulkUnitInsertRequest;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.metadata.core.UnitInheritedRule;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.core.database.collections.MetadataDocument;
import fr.gouv.vitam.metadata.core.database.collections.ObjectGroup;
import fr.gouv.vitam.metadata.core.database.collections.Unit;
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
import fr.gouv.vitam.worker.server.rest.WorkerMain;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import io.restassured.RestAssured;
import net.javacrumbs.jsonunit.JsonAssert;
import org.apache.commons.io.FileUtils;
import org.bson.Document;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.exists;
import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTION.FIELDS;
import static fr.gouv.vitam.common.guid.GUIDFactory.newOperationLogbookGUID;
import static fr.gouv.vitam.common.json.JsonHandler.writeToInpustream;
import static fr.gouv.vitam.common.model.ProcessAction.RESUME;
import static fr.gouv.vitam.common.model.logbook.LogbookEvent.OUT_DETAIL;
import static fr.gouv.vitam.common.model.logbook.LogbookOperation.EVENTS;
import static fr.gouv.vitam.ingest.external.integration.test.IngestExternalIT.INTEGRATION_INGEST_EXTERNAL_EXPECTED_LOGBOOK_JSON;
import static fr.gouv.vitam.ingest.external.integration.test.IngestExternalIT.OPERATION_ID_REPLACE;
import static fr.gouv.vitam.logbook.common.parameters.Contexts.COMPUTE_INHERITED_RULES;
import static fr.gouv.vitam.logbook.common.parameters.Contexts.DEFAULT_WORKFLOW;
import static fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess.INGEST;
import static fr.gouv.vitam.logbook.common.server.database.collections.LogbookDocument.EVENT_DETAILS;
import static io.restassured.RestAssured.get;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Processing integration test
 */
public class ProcessingIT extends VitamRuleRunner {

    private static final String BIG_WORKFLOW = "BIG_WORKFLOW";
    @ClassRule
    public static VitamServerRunner runner =
        new VitamServerRunner(ProcessingIT.class, mongoRule.getMongoDatabase().getName(),
            ElasticsearchRule.getClusterName(),
            Sets.newHashSet(
                MetadataMain.class,
                WorkerMain.class,
                AdminManagementMain.class,
                LogbookMain.class,
                WorkspaceMain.class,
                ProcessManagementMain.class,
                BatchReportMain.class
            ));
    private static final String PROCESSING_UNIT_PLAN = "integration-processing/unit_plan_metadata.json";
    private static final String INGEST_CONTRACTS_PLAN = "integration-processing/ingest_contracts_plan.json";
    private static final long SLEEP_TIME = 20L;
    private static final long NB_TRY = 18000;
    private static final String SIP_FILE_WRONG_DATE = "integration-processing/SIP_INGEST_WRONG_DATE.zip";
    private static final String SIP_KO_AU_REF_OBJ =
        "integration-processing/KO_SIP_1986_unit_declare_IDobjet_au_lieu_IDGOT.zip";
    private static final String SIP_KO_MANIFEST_URI = "integration-processing/KO_MANIFESTE-URI.zip";

    private static final String RESULTS = "$results";

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


    private static final String SIP_FILE_OK_NAME = "integration-processing/SIP-test.zip";
    private static final String SIP_RATP = "integration-processing/RATP-base.zip";

    private static final String SIP_FILE_OK_BIRTH_PLACE = "integration-processing/unit_schema_validation_ko.zip";
    private static final String SIP_PROFIL_OK = "integration-processing/SIP_ok_profil.zip";
    private static final String SIP_INGEST_CONTRACT_UNKNOW = "integration-processing/SIP_INGEST_CONTRACT_UNKNOW.zip";
    private static final String SIP_INGEST_CONTRACT_NOT_IN_CONTEXT =
        "integration-processing/SIP_INGEST_CONTRACT_NOT_IN_CONTEXT.zip";
    private static final String SIP_FILE_OK_WITH_SYSTEMID = "integration-processing/SIP_with_systemID.zip";

    private static final String SIP_FILE_ADD_AU_LINK_OK_NAME_TARGET = "integration-processing";
    private static final String SIP_FILE_ADD_AU_LINK_OK_NAME = "integration-processing/OK_SIP_ADD_AU_LINK";
    private static final String link_to_manifest_and_existing_unit =
        "integration-processing/link_to_manifest_and_existing_unit";
    private static final String SIP_FILE_ADD_AU_LINK_BY_QUERY_OK_NAME = "integration-processing/OK_SIP_ADD_AU_LINK_BY_QUERY";

    private static final String LINK_AU_TO_EXISTING_GOT_OK_NAME = "integration-processing/OK_LINK_AU_TO_EXISTING_GOT";
    private static final String LINK_AU_TO_EXISTING_GOT_OK_NAME_TARGET = "integration-processing";
    private static final String SIP_FILE_TAR_OK_NAME = "integration-processing/SIP.tar";
    private static final String SIP_INHERITED_RULE_CA1_OK = "integration-processing/1069_CA1.zip";
    private static final String SIP_INHERITED_RULE_CA4_OK = "integration-processing/1069_CA4.zip";
    private static final String SIP_FUND_REGISTER_OK = "integration-processing/OK-registre-fonds.zip";
    private static final String SIP_WITHOUT_MANIFEST = "integration-processing/SIP_no_manifest.zip";
    private static final String SIP_NO_FORMAT = "integration-processing/SIP_NO_FORMAT.zip";
    private static final String SIP_NO_FORMAT_NO_TAG = "integration-processing/SIP_NO_FORMAT_TAG.zip";
    private static final String SIP_NB_OBJ_INCORRECT_IN_MANIFEST = "integration-processing/SIP_Conformity_KO.zip";
    private static final String SIP_BUG_2721 = "integration-processing/bug2721_2racines_meme_rattachement.zip";
    private static final String SIP_WITHOUT_OBJ = "integration-processing/OK_SIP_sans_objet.zip";
    private static final String SIP_WITHOUT_FUND_REGISTER = "integration-processing/KO_registre_des_fonds.zip";
    private static final String SIP_BORD_AU_REF_PHYS_OBJECT = "integration-processing/KO_BORD_AUrefphysobject.zip";
    private static final String SIP_MANIFEST_INCORRECT_REFERENCE = "integration-processing/KO_Reference_Unexisting.zip";
    private static final String SIP_REFERENCE_CONTRACT_KO = "integration-processing/KO_SIP_2_GO_contract.zip";
    private static final String SIP_COMPLEX_RULES = "integration-processing/OK_RULES_COMPLEXE_COMPLETE.zip";
    private static final String SIP_APPRAISAL_RULES = "integration-processing/bug_appraisal.zip";

    private static final String SIP_FILE_KO_AU_REF_BDO = "integration-processing/SIP_KO_ArchiveUnit_ref_BDO.zip";
    private static final String SIP_BUG_2182 = "integration-processing/SIP_bug_2182.zip";
    private static final String SIP_FILE_1791_CA1 = "integration-processing/SIP_FILE_1791_CA1.zip";
    private static final String SIP_FILE_1791_CA2 = "integration-processing/SIP_FILE_1791_CA2.zip";

    private static final String OK_SIP_SIGNATURE = "integration-processing/Signature_OK.zip";

    private static final String SIP_ARBRE_3062 = "integration-processing/3062_arbre.zip";

    private static final String SIP_PROD_SERV_A = "integration-processing/Sip_A.zip";
    private static final String SIP_MDD_SEDA_GOT = "integration-processing/SIP_MDD_SEDA_GOT.zip";
    private static final String SIP_UNKNOWN_FIELD_SEDA_GOT = "integration-processing/SIP_UNKNOWN_FIELD_SEDA_GOT.zip";
    private static final String SIP_PROD_SERV_B_ATTACHED = "integration-processing/SIP_B";
    private static final String ADD_OBJET_TO_GOT = "integration-processing/ADD_OBJET_TO_GOT";

    private static final String SIP_FULL_SEDA_2_1 = "integration-processing/OK_SIP_FULL_SEDA2.1.zip";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        handleBeforeClass(Arrays.asList(0, 1), Collections.emptyMap());
        CONFIG_BIG_WORKER_PATH = PropertiesUtils.getResourcePath("integration-processing/bigworker.conf").toString();

        FormatIdentifierFactory.getInstance().changeConfigurationFile(VitamServerRunner.FORMAT_IDENTIFIERS_CONF);

        processMonitoring = ProcessMonitoringImpl.getInstance();

        StorageClientFactory storageClientFactory = StorageClientFactory.getInstance();
        storageClientFactory.setVitamClientType(VitamClientFactoryInterface.VitamClientType.MOCK);
        new DataLoader("integration-processing").prepareData();

    }


    public static void prepareVitamSession() {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        VitamThreadUtils.getVitamSession().setContractId("aName");
        VitamThreadUtils.getVitamSession().setContextId("Context_IT");
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        handleAfterClass();
        StorageClientFactory storageClientFactory = StorageClientFactory.getInstance();
        storageClientFactory.setVitamClientType(VitamClientFactoryInterface.VitamClientType.PRODUCTION);
        runAfter();
        VitamClientFactory.resetConnections();
    }

    @After
    public void afterTest() {
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

        runAfterEs(
            ElasticsearchIndexAlias.ofMultiTenantCollection(MetadataCollections.UNIT.getName(), 0),
            ElasticsearchIndexAlias.ofMultiTenantCollection(MetadataCollections.UNIT.getName(), 1),
            ElasticsearchIndexAlias.ofMultiTenantCollection(MetadataCollections.OBJECTGROUP.getName(), 0),
            ElasticsearchIndexAlias.ofMultiTenantCollection(MetadataCollections.OBJECTGROUP.getName(), 1),
            ElasticsearchIndexAlias.ofMultiTenantCollection(LogbookCollections.OPERATION.getName(), 0),
            ElasticsearchIndexAlias.ofMultiTenantCollection(LogbookCollections.OPERATION.getName(), 1),
            ElasticsearchIndexAlias.ofCrossTenantCollection(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getName()),
            ElasticsearchIndexAlias.ofCrossTenantCollection(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getName())
        );
    }

    @RunWithCustomExecutor
    @Test
    public void testServersStatus() {
        RestAssured.port = VitamServerRunner.PORT_SERVICE_PROCESSING;
        RestAssured.basePath = PROCESSING_PATH;

        get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());

        RestAssured.port = VitamServerRunner.PORT_SERVICE_WORKSPACE;
        RestAssured.basePath = WORKSPACE_PATH;
        get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());

        RestAssured.port = VitamServerRunner.PORT_SERVICE_METADATA;
        RestAssured.basePath = METADATA_PATH;
        get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());

        RestAssured.port = VitamServerRunner.PORT_SERVICE_WORKER;
        RestAssured.basePath = WORKER_PATH;
        get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());

        RestAssured.port = VitamServerRunner.PORT_SERVICE_LOGBOOK;
        RestAssured.basePath = LOGBOOK_PATH;
        get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());
    }

    private void wait(String operationId) {
        int nbTry = 0;
        ProcessingManagementClient processingClient = ProcessingManagementClientFactory.getInstance().getClient();
        while (!processingClient.isNotRunning(operationId)) {
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
            FormatIdentifierFactory.getInstance().changeConfigurationFile(VitamServerRunner.FORMAT_IDENTIFIERS_CONF);
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
            String bulkProcessId = GUIDFactory.newGUID().toString();
            metaDataClient.insertUnitBulk(
                new BulkUnitInsertRequest(Arrays.asList(
                    new BulkUnitInsertEntry(Collections.emptySet(), addOpiToMetadata(JsonHandler
                            .getFromFile(PropertiesUtils.getResourceFile("integration-processing/unit_metadata.json")),
                        bulkProcessId)),
                    new BulkUnitInsertEntry(Collections.emptySet(),
                        addOpiToMetadata(JsonHandler.getFromFile(PropertiesUtils.getResourceFile(PROCESSING_UNIT_PLAN)),
                            bulkProcessId))
                )));
            writeUnitsLogbook(bulkProcessId);

            metaDataClient.refreshUnits();

            processingClient = ProcessingManagementClientFactory.getInstance().getClient();
            processingClient.initVitamProcess(containerName, DEFAULT_WORKFLOW.name());

            RequestResponse<ItemStatus> ret =
                processingClient.updateOperationActionProcess(RESUME.getValue(), containerName);
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
            JsonNode events = logbookResult.get(RESULTS).get(0).get(EVENTS);
            verifyEvent(events, "ROLL_BACK.OK");
            verifyEvent(events, "PROCESS_SIP_UNITARY.WARNING");

            assertEquals(logbookResult.get("$results").get(0).get("obIdIn").asText(),
                "bug2721_2racines_meme_rattachement");
            assertThat(logbookResult.get("$results").get(0).get("evParentId")).isExactlyInstanceOf(NullNode.class);
            JsonNode agIdExt = JsonHandler.getFromString(logbookResult.get("$results").get(0).get("agIdExt").asText());
            assertEquals(agIdExt.get("originatingAgency").asText(), "producteur1");

            // lets check the accession register
            Select query = new Select();
            query.setLimitFilter(0, 1);
            RequestResponse<AccessionRegisterSummaryModel> resp = functionalClient.getAccessionRegister(query.getFinalSelect());
            assertThat(resp).isInstanceOf(RequestResponseOK.class);
            assertThat(((RequestResponseOK<AccessionRegisterSummaryModel>) resp).getHits().getTotal()).isEqualTo(1);
            assertThat(((RequestResponseOK<AccessionRegisterSummaryModel>) resp).getHits().getSize()).isEqualTo(1);

            // check if unit is valid
            MongoIterable<Document> resultCheckUnits = MetadataCollections.UNIT.getCollection().find();
            Document unitCheck = resultCheckUnits.first();
            assertThat(unitCheck.get("_storage")).isNotNull();
            Document storageUnit = (Document) unitCheck.get("_storage");
            assertThat(storageUnit.get("_nbc")).isNull();
            assertThat(storageUnit.get("offerIds")).isNull();

            // check if units are valid
            MongoIterable<Document> resultCheckObjectGroups = MetadataCollections.OBJECTGROUP.getCollection().find();
            Document objectGroupCheck = resultCheckObjectGroups.first();
            assertThat(objectGroupCheck.get("_storage")).isNotNull();
            Document storageObjectGroup = (Document) objectGroupCheck.get("_storage");
            assertThat(storageObjectGroup.get("_nbc")).isNull();
            assertThat(storageObjectGroup.get("offerIds")).isNull();

            List<Document> qualifiers = (List<Document>) objectGroupCheck.get("_qualifiers");
            assertThat(qualifiers.size()).isEqualTo(3);
            Document binaryMaster = qualifiers.get(0);
            assertThat(binaryMaster.get("_nbc")).isNotNull();
            assertThat(binaryMaster.get("_nbc")).isEqualTo(1);

            List<Document> versions = (List<Document>) binaryMaster.get("versions");
            assertThat(versions.size()).isEqualTo(1);
            Document version = versions.get(0);
            Document storageVersion = (Document) version.get("_storage");
            assertThat(storageVersion.get("_nbc")).isNull();
            assertThat(storageVersion.get("offerIds")).isNull();
        }
    }

    private void writeUnitsLogbook(String bulkProcessId)
        throws FileNotFoundException, InvalidParseOperationException, DatabaseException, LogbookExecutionException {
        InputStream logbookIs =
            PropertiesUtils.getResourceAsStream(INTEGRATION_INGEST_EXTERNAL_EXPECTED_LOGBOOK_JSON);
        JsonNode logbookJsonNode = JsonHandler.getFromInputStream(
            logbookIs);

        ((ObjectNode) logbookJsonNode).put(VitamDocument.TENANT_ID, tenantId);
        String json = JsonHandler.prettyPrint(logbookJsonNode).replace(OPERATION_ID_REPLACE, bulkProcessId);

        VitamDocument<LogbookOperation> logbookDocument = new LogbookOperation(json);

        LogbookCollections.OPERATION.getCollection().bulkWrite(
            Collections.singletonList(new InsertOneModel<VitamDocument<LogbookOperation>>(logbookDocument)));

        insertLogbookToElasticsearch(logbookDocument);
    }

    private void insertLogbookToElasticsearch(VitamDocument vitamDocument)
        throws LogbookExecutionException {
        Integer tenantId = HeaderIdHelper.getTenantId();
        String id = vitamDocument.getId();
        vitamDocument.remove(VitamDocument.ID);
        vitamDocument.remove(VitamDocument.SCORE);
        new LogbookTransformData().transformDataForElastic(vitamDocument);
        LogbookCollections.OPERATION.getEsClient()
            .indexEntry(LogbookCollections.OPERATION, tenantId, id, vitamDocument);
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowMassUpdate() throws Exception {
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
            String bulkProcessId = GUIDFactory.newGUID().toString();
            metaDataClient.insertUnitBulk(
                new BulkUnitInsertRequest(Arrays.asList(
                    new BulkUnitInsertEntry(Collections.emptySet(), addOpiToMetadata(JsonHandler
                            .getFromFile(PropertiesUtils.getResourceFile("integration-processing/unit_metadata.json")),
                        bulkProcessId)),
                    new BulkUnitInsertEntry(Collections.emptySet(),
                        addOpiToMetadata(JsonHandler.getFromFile(PropertiesUtils.getResourceFile(PROCESSING_UNIT_PLAN)),
                            bulkProcessId))
                )));
            writeUnitsLogbook(bulkProcessId);

            metaDataClient.refreshUnits();
            // import contract
            File fileContracts = PropertiesUtils.getResourceFile(INGEST_CONTRACTS_PLAN);
            List<IngestContractModel> IngestContractModelList =
                JsonHandler.getFromFileAsTypeReference(fileContracts, new TypeReference<>() {
                });

            functionalClient.importIngestContracts(IngestContractModelList);

            processingClient = ProcessingManagementClientFactory.getInstance().getClient();
            processingClient.initVitamProcess(containerName, DEFAULT_WORKFLOW.name());

            RequestResponse<ItemStatus> ret =
                processingClient.updateOperationActionProcess(RESUME.getValue(), containerName);
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
            JsonNode events = logbookResult.get(RESULTS).get(0).get(EVENTS);
            verifyEvent(events, "ROLL_BACK.OK");
            verifyEvent(events, "PROCESS_SIP_UNITARY.WARNING");

            assertEquals(logbookResult.get("$results").get(0).get("obIdIn").asText(),
                "bug2721_2racines_meme_rattachement");
            assertThat(logbookResult.get("$results").get(0).get("evParentId")).isExactlyInstanceOf(NullNode.class);
            JsonNode agIdExt = JsonHandler.getFromString(logbookResult.get("$results").get(0).get("agIdExt").asText());
            assertEquals(agIdExt.get("originatingAgency").asText(), "producteur1");

            // lets check the accession register
            Select select = new Select();
            select.setQuery(QueryHelper.eq("OriginatingAgency", "producteur1"));
            RequestResponse<AccessionRegisterSummaryModel> resp = functionalClient.getAccessionRegister(select.getFinalSelect());
            assertThat(resp).isInstanceOf(RequestResponseOK.class);
            assertThat(((RequestResponseOK<AccessionRegisterSummaryModel>) resp).getHits().getTotal()).isEqualTo(1);
            assertThat(((RequestResponseOK<AccessionRegisterSummaryModel>) resp).getHits().getSize()).isEqualTo(1);

            // check if unit is valid
            MongoIterable<Document> resultCheckUnits = MetadataCollections.UNIT.getCollection().find();
            Document unitCheck = resultCheckUnits.first();
            assertThat(unitCheck.get("_storage")).isNotNull();
            Document storageUnit = (Document) unitCheck.get("_storage");
            assertThat(storageUnit.get("_nbc")).isNull();
            assertThat(storageUnit.get("offerIds")).isNull();

            // check if units are valid
            MongoIterable<Document> resultCheckObjectGroups = MetadataCollections.OBJECTGROUP.getCollection().find();
            Document objectGroupCheck = resultCheckObjectGroups.first();
            assertThat(objectGroupCheck.get("_storage")).isNotNull();
            Document storageObjectGroup = (Document) objectGroupCheck.get("_storage");
            assertThat(storageObjectGroup.get("_nbc")).isNull();
            assertThat(storageObjectGroup.get("offerIds")).isNull();

            List<Document> qualifiers = (List<Document>) objectGroupCheck.get("_qualifiers");
            assertThat(qualifiers.size()).isEqualTo(3);
            Document binaryMaster = qualifiers.get(0);
            assertThat(binaryMaster.get("_nbc")).isNotNull();
            assertThat(binaryMaster.get("_nbc")).isEqualTo(1);

            List<Document> versions = (List<Document>) binaryMaster.get("versions");
            assertThat(versions.size()).isEqualTo(1);
            Document version = versions.get(0);
            Document storageVersion = (Document) version.get("_storage");
            assertThat(storageVersion.get("_nbc")).isNull();
            assertThat(storageVersion.get("offerIds")).isNull();
        }
    }

    private JsonNode addOpiToMetadata(JsonNode jsonNode, String operationId) {
        return ((ObjectNode) jsonNode).put(MetadataDocument.OPI, operationId);
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
        processingClient.initVitamProcess(containerName, DEFAULT_WORKFLOW.name());

        final RequestResponse<ItemStatus> ret =
            processingClient.executeOperationProcess(containerName, DEFAULT_WORKFLOW.name(),
                RESUME.getValue());

        assertNotNull(ret);
        assertThat(ret.isOk()).isTrue();
        assertEquals(Status.ACCEPTED.getStatusCode(), ret.getStatus());

        wait(containerName);
        ProcessWorkflow processWorkflow =
            processMonitoring.findOneProcessWorkflow(containerName, tenantId);
        assertNotNull(processWorkflow);
        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());

        LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient();
        JsonNode logbookResult = logbookClient.selectOperationById(containerName);
        JsonNode logbookNode = logbookResult.get("$results").get(0);
        assertEquals("CHECK_HEADER.CHECK_CONTRACT_INGEST.CONTRACT_UNKNOWN.KO",
            logbookNode.get("events").get(5).get("outDetail").asText());
        assertThat(logbookResult.get("$results").get(0).get("evParentId")).isExactlyInstanceOf(NullNode.class);
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
        processingClient.initVitamProcess(containerName, DEFAULT_WORKFLOW.name());

        final RequestResponse<ItemStatus> ret =
            processingClient.executeOperationProcess(containerName, DEFAULT_WORKFLOW.name(),
                RESUME.getValue());

        assertNotNull(ret);
        assertThat(ret.isOk()).isTrue();
        assertEquals(Status.ACCEPTED.getStatusCode(), ret.getStatus());

        wait(containerName);
        ProcessWorkflow processWorkflow =
            processMonitoring.findOneProcessWorkflow(containerName, tenantId);
        assertNotNull(processWorkflow);
        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());

        LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient();
        JsonNode logbookResult = logbookClient.selectOperationById(containerName);
        JsonNode logbookNode = logbookResult.get("$results").get(0);
        assertThat(logbookNode.get("events").get(5).get("outDetail").asText())
            .isEqualTo("CHECK_HEADER.CHECK_CONTRACT_INGEST.CONTRACT_NOT_IN_CONTEXT.KO");
        assertThat(logbookResult.get("$results").get(0).get("evParentId")).isExactlyInstanceOf(NullNode.class);
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowProfil() throws Exception {
        prepareVitamSession();

        final String containerName = ingestSIP(SIP_PROFIL_OK, DEFAULT_WORKFLOW.name(), StatusCode.WARNING);

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
        assertThat(logbookNode.get("evParentId")).isExactlyInstanceOf(NullNode.class);
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
        processingClient.initVitamProcess(containerName, DEFAULT_WORKFLOW.name());
        final RequestResponse<ItemStatus> ret = processingClient
            .executeOperationProcess(containerName, DEFAULT_WORKFLOW.name(), RESUME.getValue());
        assertNotNull(ret);
        assertThat(ret.isOk()).isTrue();
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
        processingClient.initVitamProcess(containerName, DEFAULT_WORKFLOW.name());
        final RequestResponse<ItemStatus> ret =
            processingClient.executeOperationProcess(containerName, DEFAULT_WORKFLOW.name(),
                RESUME.getValue());

        assertNotNull(ret);
        assertThat(ret.isOk()).isTrue();
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

        processingClient.initVitamProcess(containerName, DEFAULT_WORKFLOW.name());
        final RequestResponse<ItemStatus> ret =
            processingClient.executeOperationProcess(containerName, DEFAULT_WORKFLOW.name(),
                RESUME.getValue());

        assertNotNull(ret);
        assertThat(ret.isOk()).isTrue();
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

        ingestSIP(SIP_INHERITED_RULE_CA4_OK, DEFAULT_WORKFLOW.name(), StatusCode.WARNING);

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

        ingestSIP(SIP_FUND_REGISTER_OK, DEFAULT_WORKFLOW.name(), StatusCode.WARNING);
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowWithSipNoManifest() throws Exception {
        prepareVitamSession();

        ingestSIP(SIP_WITHOUT_MANIFEST, DEFAULT_WORKFLOW.name(), StatusCode.KO);
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowSipNoFormat() throws Exception {
        prepareVitamSession();

        ingestSIP(SIP_NO_FORMAT, DEFAULT_WORKFLOW.name(), StatusCode.OK);
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowSipNoFormatNoTag() throws Exception {

        prepareVitamSession();

        ingestSIP(SIP_NO_FORMAT_NO_TAG, DEFAULT_WORKFLOW.name(), StatusCode.OK);
    }


    @RunWithCustomExecutor
    @Test
    public void testWorkflowWithManifestIncorrectObjectNumber() throws Exception {
        prepareVitamSession();

        ingestSIP(SIP_NB_OBJ_INCORRECT_IN_MANIFEST, DEFAULT_WORKFLOW.name(), StatusCode.KO);
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
        processingClient.initVitamProcess(containerName, DEFAULT_WORKFLOW.name());

        final RequestResponse<ItemStatus> ret =
            processingClient.executeOperationProcess(containerName, DEFAULT_WORKFLOW.name(),
                RESUME.getValue());

        assertNotNull(ret);
        assertThat(ret.isOk()).isTrue();
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

        ingestSIP(SIP_WITHOUT_FUND_REGISTER, DEFAULT_WORKFLOW.name(), StatusCode.KO);
    }

    @RunWithCustomExecutor
    // as now errors with xml are handled in ExtractSeda (not a FATAL but a KO
    // it s no longer an exception that is obtained
    @Test
    public void testWorkflowSipCausesFatalThenProcessingInternalServerException() throws Exception {
        prepareVitamSession();

        ingestSIP(SIP_BORD_AU_REF_PHYS_OBJECT, DEFAULT_WORKFLOW.name(), StatusCode.KO);
    }

    @RunWithCustomExecutor
    @Test
    public void testworkFlowAudit() throws Exception {
        // Given
        prepareVitamSession();

        String opi = ingestSIP(OK_SIP_SIGNATURE, DEFAULT_WORKFLOW.name(), StatusCode.OK);

        String containerName = createOperationContainer();

        processingClient = ProcessingManagementClientFactory.getInstance().getClient();

        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);
        ObjectNode options =
            JsonHandler.createObjectNode().put("correctiveOption", false);
        workspaceClient.putObject(containerName, "evidenceOptions", JsonHandler.writeToInpustream(options));

        Select select = new Select();
        select.setQuery(QueryHelper.eq("#opi", opi));

        workspaceClient
            .putObject(containerName, "query.json", JsonHandler.writeToInpustream(select.getFinalSelect()));

        processingClient.initVitamProcess(containerName, Contexts.EVIDENCE_AUDIT.name());
        // When
        RequestResponse<ItemStatus> jsonNodeRequestResponse =
            processingClient.executeOperationProcess(containerName, Contexts.EVIDENCE_AUDIT.name(),
                RESUME.getValue());


        assertNotNull(jsonNodeRequestResponse);
        assertThat(jsonNodeRequestResponse.isOk()).isTrue();
        assertEquals(Status.ACCEPTED.getStatusCode(), jsonNodeRequestResponse.getStatus());

        wait(containerName);

        ProcessWorkflow processWorkflow =
            processMonitoring.findOneProcessWorkflow(containerName, tenantId);
        assertNotNull(processWorkflow);

        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
        assertEquals(StatusCode.WARNING, processWorkflow.getStatus());
    }

    @RunWithCustomExecutor
    @Test
    public void test_link_to_invalid_unit_guid_then_to_not_exists_unit_ko() throws Exception {

        prepareVitamSession();

        // We link to a non existing unit
        String zipName = ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE - 1) + ".zip";

        replaceStringInFile(SIP_FILE_ADD_AU_LINK_OK_NAME + "/manifest.xml", "(?<=<SystemId>).*?(?=</SystemId>)",
            "UnvalidGuid:");
        // prepare zip
        String zipPath =
            PropertiesUtils.getResourcePath(SIP_FILE_ADD_AU_LINK_OK_NAME_TARGET).toAbsolutePath().toString() +
                "/" + zipName;
        zipFolder(PropertiesUtils.getResourcePath(SIP_FILE_ADD_AU_LINK_OK_NAME), zipPath);
        ProcessWorkflow processWorflow =
            ingest(zipPath, DEFAULT_WORKFLOW, StatusCode.KO);

        String operationId = processWorflow.getOperationId();

        Document operation =
            (Document) LogbookCollections.OPERATION.getCollection().find(eq("_id", operationId)).first();
        assertThat(operation).isNotNull();
        assertTrue(operation.toString().contains("CHECK_DATAOBJECTPACKAGE.CHECK_MANIFEST.INVALID_GUID_ATTACHMENT.KO"));

        try {
            Files.delete(new File(zipPath).toPath());
        } catch (Exception e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }

        // We link to a non existing unit
        zipName = ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE - 1) + "1.zip";

        replaceStringInFile(SIP_FILE_ADD_AU_LINK_OK_NAME + "/manifest.xml", "(?<=<SystemId>).*?(?=</SystemId>)",
            "aeaqaaaabeha2624aaqjmalhotiigyyaaaca");
        // prepare zip
        zipPath =
            PropertiesUtils.getResourcePath(SIP_FILE_ADD_AU_LINK_OK_NAME_TARGET).toAbsolutePath().toString() + "/" +
                zipName;
        zipFolder(PropertiesUtils.getResourcePath(SIP_FILE_ADD_AU_LINK_OK_NAME), zipPath);
        processWorflow =
            ingest(zipPath, DEFAULT_WORKFLOW, StatusCode.KO);

        operationId = processWorflow.getOperationId();

        operation = (Document) LogbookCollections.OPERATION.getCollection().find(eq("_id", operationId)).first();
        assertThat(operation).isNotNull();
        assertTrue(operation.toString().contains("CHECK_DATAOBJECTPACKAGE.CHECK_MANIFEST.NOT_FOUND_ATTACHMENT.KO"));
        try {
            Files.delete(new File(zipPath).toPath());
        } catch (Exception e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }
    }


    @RunWithCustomExecutor
    @Test
    public void test_link_holdingscheme_to_filingscheme_ko() throws Exception {
        prepareVitamSession();
        // Import Filing scheme (Plan)
        ProcessWorkflow pw = ingest(PropertiesUtils.getResourcePath(SIP_RATP).toUri().getPath(), Contexts.FILING_SCHEME,
            StatusCode.OK);

        Document operation =
            (Document) LogbookCollections.OPERATION.getCollection().find(eq("_id", pw.getOperationId())).first();
        assertThat(operation).isNotNull();
        assertTrue(operation.toString().contains("CHECK_ARCHIVE_UNIT_PROFILE.OK"));

        // get one unit
        MongoIterable<Document> resultUnits = MetadataCollections.UNIT.getCollection().find();
        MongoCursor<Document> cursor = resultUnits.iterator();
        String unit = cursor.next().getString("_id");

        String tmp =
            FileUtils.getTempDirectoryPath() + "/" + ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE - 1);
        FileUtils.forceMkdir(new File(tmp));

        // Enable attachment
        updateIngestContractLinkParentId("", IngestContractCheckState.AUTHORIZED.name(), null);

        // Create SIP with attachment to unit
        String zipName = ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE - 1) + "1.zip";
        replaceStringInFile(link_to_manifest_and_existing_unit + "/manifest.xml", "(?<=<SystemId>).*?(?=</SystemId>)",
            unit);
        String zipPath = tmp + "/" + zipName;
        zipFolder(PropertiesUtils.getResourcePath(link_to_manifest_and_existing_unit), zipPath);

        // Attach to unitChild KO existing unit should not have a parent in the manifest
        ProcessWorkflow processWorkflow =
            ingest(zipPath, Contexts.HOLDING_SCHEME, StatusCode.KO);

        String operationId = processWorkflow.getOperationId();
        operation =
            (Document) LogbookCollections.OPERATION.getCollection().find(eq("_id", operationId)).first();
        assertThat(operation).isNotNull();
        assertTrue(operation.toString().contains("CHECK_DATAOBJECTPACKAGE.CHECK_MANIFEST.UNAUTHORIZED_ATTACHMENT.KO"));

        try {
            FileUtils.deleteDirectory(new File(tmp));
        } catch (Exception e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }

    }

    @RunWithCustomExecutor
    @Test
    public void test_multiple_unit_link_cases() throws Exception {
        prepareVitamSession();
        // 1. First we create an AU by sip (Tree) (RATP_1 -> RATP_2)
        String ingestPath = PropertiesUtils.getResourcePath(SIP_RATP).toUri().getPath();
        // Ingest PLAN de classement
        ingest(ingestPath, Contexts.FILING_SCHEME,
            StatusCode.OK);

        // 2. Get id of both au from 1 and 2
        MongoIterable<Document> resultUnits = MetadataCollections.UNIT.getCollection().find();
        MongoCursor<Document> cursor = resultUnits.iterator();
        Document doc1 = null;
        Document doc2 = null;
        Document doc3 = null;
        while (cursor.hasNext()) {
            Document document = cursor.next();
            switch (document.getString("Title")) {
                case "RATP 1":
                    doc1 = document;
                    break;
                case "RATP 2":
                    doc2 = document;
                    break;
                case "RATP 3":
                    doc3 = document;
                    break;
                default:
            }
        }

        assertNotNull(doc1);
        assertNotNull(doc2);
        assertNotNull(doc3);

        String unitRoot = doc1.getString("_id");
        String unitChild = doc2.getString("_id");
        String unitChildOfChild = doc3.getString("_id");


        //3. Get number of events in LFC of both unit1 and unit2
        MongoCursor<Document> logbookCursor =
            LogbookCollections.LIFECYCLE_UNIT.getCollection().find(eq(Unit.ID, unitRoot)).iterator();
        Document lfcUnit1 = logbookCursor.next();
        List<JsonNode> eventsUnit1 = lfcUnit1.get("events", List.class);
        int lcfUnit1Size = eventsUnit1.size();

        logbookCursor = LogbookCollections.LIFECYCLE_UNIT.getCollection().find(eq(Unit.ID, unitChild)).iterator();
        Document lfcUnit2 = logbookCursor.next();
        List<JsonNode> eventsUnit2 = lfcUnit2.get("events", List.class);
        int lcfUnit2Size = eventsUnit2.size();

        String tmp =
            FileUtils.getTempDirectoryPath() + "/" + ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE - 1);
        FileUtils.forceMkdir(new File(tmp));

        // Enable attachment
        updateIngestContractLinkParentId("", IngestContractCheckState.AUTHORIZED.name(), null);

        // Ingest
        // Create SIP with unitRoot
        String zipName3 = ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE - 1) + "2.zip";
        replaceStringInFile(link_to_manifest_and_existing_unit + "/manifest.xml", "(?<=<SystemId>).*?(?=</SystemId>)",
            unitChild);
        String zipPath3 = tmp + "/" + zipName3;
        zipFolder(PropertiesUtils.getResourcePath(link_to_manifest_and_existing_unit), zipPath3);

        // Attach to unitChild KO existing unit should not have a parent in the manifest
        ProcessWorkflow processWorkflow =
            ingest(zipPath3, DEFAULT_WORKFLOW, StatusCode.KO);
        String operationId = processWorkflow.getOperationId();
        Document operation =
            (Document) LogbookCollections.OPERATION.getCollection().find(eq("_id", operationId)).first();
        assertThat(operation).isNotNull();
        assertTrue(operation.toString()
            .contains("CHECK_DATAOBJECTPACKAGE.CHECK_MANIFEST.MODIFY_PARENT_EXISTING_UNIT_UNAUTHORIZED.KO"));


        // Now update the ingest contract, set the check to ACTIVE and the link parent id takes unitChild value
        List<String> checkParentId = new ArrayList<>();
        checkParentId.add(unitChild);
        updateIngestContractLinkParentId(unitChild, "AUTHORIZED", checkParentId);

        // Create SIP with unitChild
        String zipName1 = ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE - 1) + ".zip";
        replaceStringInFile(SIP_FILE_ADD_AU_LINK_OK_NAME + "/manifest.xml", "(?<=<SystemId>).*?(?=</SystemId>)",
            unitChildOfChild);
        String zipPath1 = tmp + "/" + zipName1;
        zipFolder(PropertiesUtils.getResourcePath(SIP_FILE_ADD_AU_LINK_OK_NAME), zipPath1);

        // Attach to unitChild OK
        processWorkflow =
            ingest(zipPath1, DEFAULT_WORKFLOW, StatusCode.OK);

        operationId = processWorkflow.getOperationId();

        List<Document> units =
            Lists.newArrayList(MetadataCollections.UNIT.getCollection().find(eq(Unit.OPS, operationId)));

        // Check that we auto attach by IngestContract
        for (Document au : units) {
            List<String> parents = au.get(Unit.UP, List.class);
            if (au.getString("Title").equals("Shout no be attached by ingest contract")) {
                assertThat(parents).hasSize(1);
                assertThat(parents).doesNotContain(unitChildOfChild);
            } else {
                //Auto attach by ingest contract to LinkParentId
                assertThat(parents).contains(unitChild);
                if (au.getString("Title").equals("Shout be attached by ingest contract")) {
                    assertThat(parents).contains(unitChildOfChild);
                }
            }
        }

        // Try to attach HOLDING to INGEST
        // Attach to unitChild KO Unauthorized attach HOLDING TO FILING SCHEME
        // Ingest Arbre de positionnement
        processWorkflow =
            ingest(ingestPath, Contexts.HOLDING_SCHEME, StatusCode.KO);

        operationId = processWorkflow.getOperationId();
        operation = (Document) LogbookCollections.OPERATION.getCollection().find(eq("_id", operationId)).first();
        assertThat(operation).isNotNull();
        assertTrue(operation.toString().contains("CHECK_DATAOBJECTPACKAGE.CHECK_MANIFEST.UNAUTHORIZED_ATTACHMENT.KO"));

        // 6.2 ingest here should be KO, we link an incorrect id (not a child of the referenced au in the ingest contract) into the sip
        // Create SIP with unitRoot
        String zipName2 = ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE - 1) + "1.zip";
        replaceStringInFile(SIP_FILE_ADD_AU_LINK_OK_NAME + "/manifest.xml", "(?<=<SystemId>).*?(?=</SystemId>)",
            unitRoot);
        String zipPath2 = tmp + "/" + zipName2;
        zipFolder(PropertiesUtils.getResourcePath(SIP_FILE_ADD_AU_LINK_OK_NAME), zipPath2);

        // Attach to unitChild KO because of ingest contract restriction
        processWorkflow =
            ingest(zipPath2, DEFAULT_WORKFLOW, StatusCode.KO);

        // Check that we have an AU where in his up we have idUnit
        MongoIterable<Document> newChildUnit = MetadataCollections.UNIT.getCollection().find(eq("_up", unitRoot));
        assertNotNull(newChildUnit);
        assertNotNull(newChildUnit.first());

        operationId = processWorkflow.getOperationId();
        operation = (Document) LogbookCollections.OPERATION.getCollection().find(eq("_id", operationId)).first();
        assertThat(operation).isNotNull();
        assertTrue(operation.toString().contains("CHECK_DATAOBJECTPACKAGE.CHECK_MANIFEST.UNAUTHORIZED_ATTACHMENT.KO"));

        // Test Null Parent Link
        updateIngestContractLinkParentId("", "AUTHORIZED", null);
        // Ingest should be OK
        processWorkflow =
            ingest(zipPath2, DEFAULT_WORKFLOW, StatusCode.KO);

        operationId = processWorkflow.getOperationId();
        operation = (Document) LogbookCollections.OPERATION.getCollection().find(eq("_id", operationId)).first();
        assertThat(operation).isNotNull();
        assertTrue(operation.toString().contains("CHECK_DATAOBJECTPACKAGE.CHECK_MANIFEST.UNAUTHORIZED_ATTACHMENT.KO"));

        // Now put check as inactive for the ingest contract
        updateIngestContractLinkParentId("", "AUTHORIZED", new ArrayList<>());
        // Ingest should be OK
        ingest(zipPath2, DEFAULT_WORKFLOW, StatusCode.OK);

        // For all cases, the LFC of unit 1 and unit 2 must not be modified
        // Check unit 1 LFC not modified
        logbookCursor = LogbookCollections.LIFECYCLE_UNIT.getCollection().find(eq(Unit.ID, unitRoot)).iterator();
        lfcUnit1 = logbookCursor.next();
        eventsUnit1 = lfcUnit1.get("events", List.class);
        int newLcfUnit1Size = eventsUnit1.size();
        assertThat(newLcfUnit1Size).isEqualTo(lcfUnit1Size);

        // Check unit 2 LFC not modified
        logbookCursor = LogbookCollections.LIFECYCLE_UNIT.getCollection().find(eq(Unit.ID, unitChild)).iterator();
        lfcUnit2 = logbookCursor.next();
        eventsUnit2 = lfcUnit2.get("events", List.class);
        int newLcfUnit2Size = eventsUnit2.size();
        assertThat(newLcfUnit2Size).isEqualTo(lcfUnit2Size);

        try {
            FileUtils.deleteDirectory(new File(tmp));
        } catch (Exception e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }
    }

    private ProcessWorkflow ingest(String sipFilePath, Contexts contexts,
        StatusCode expectedStatus) throws Exception {
        String operationId = createOperationContainer();
        // use link sip
        final InputStream zipStream = new FileInputStream(new File(sipFilePath));
        WorkspaceClient workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(operationId);
        workspaceClient.uncompressObject(operationId, SIP_FOLDER, CommonMediaType.ZIP, zipStream);

        ProcessingManagementClient processingClient = ProcessingManagementClientFactory.getInstance().getClient();
        processingClient.initVitamProcess(operationId, contexts.name());
        final RequestResponse<ItemStatus> resp =
            processingClient.executeOperationProcess(operationId, contexts.name(), ProcessAction.RESUME.getValue());
        assertNotNull(resp);
        assertThat(resp.isOk()).isTrue();
        assertEquals(Status.ACCEPTED.getStatusCode(), resp.getStatus());
        wait(operationId);
        ProcessWorkflow processWorkflow =
            ProcessMonitoringImpl.getInstance().findOneProcessWorkflow(operationId, tenantId);
        assertThat(processWorkflow).isNotNull();
        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
        assertEquals(expectedStatus, processWorkflow.getStatus());

        return processWorkflow;
    }

    private void updateIngestContractLinkParentId(String linkParentId, String checkParentLink,
        List<String> checkParentId)
        throws Exception {
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(tenantId));
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            final UpdateParserSingle updateParserActive = new UpdateParserSingle(new SingleVarNameAdapter());
            final SetAction setLinkParentId = UpdateActionHelper.set(IngestContractModel.LINK_PARENT_ID, linkParentId);
            final SetAction setCheckParentLink =
                UpdateActionHelper.set(IngestContractModel.TAG_CHECK_PARENT_LINK, checkParentLink);

            final Update updateLinkParent = new Update();
            updateLinkParent.setQuery(QueryHelper.eq("Identifier", "ArchivalAgreement0"));
            updateLinkParent.addActions(setLinkParentId, setCheckParentLink);
            if (checkParentId != null) {
                final SetAction setCheckParentId =
                    UpdateActionHelper.set(IngestContractModel.TAG_CHECK_PARENT_ID, checkParentId);
                updateLinkParent.addActions(setCheckParentId);
            }
            updateParserActive.parse(updateLinkParent.getFinalUpdate());
            JsonNode queryDsl = updateParserActive.getRequest().getFinalUpdate();
            RequestResponse<IngestContractModel> requestResponse = client.updateIngestContract("ArchivalAgreement0", queryDsl);
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
     * <p>
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
        processingClient.initVitamProcess(containerName2, DEFAULT_WORKFLOW.name());
        final RequestResponse<ItemStatus> ret2 =
            processingClient.executeOperationProcess(containerName2, DEFAULT_WORKFLOW.name(),
                RESUME.getValue());

        assertNotNull(ret2);
        assertThat(ret2.isOk()).isTrue();
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
        ingestSIP(SIP_PROD_SERV_A, DEFAULT_WORKFLOW.name(), StatusCode.WARNING);

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
        processingClient.initVitamProcess(containerName2, DEFAULT_WORKFLOW.name());
        final RequestResponse<ItemStatus> ret2 =
            processingClient.executeOperationProcess(containerName2, DEFAULT_WORKFLOW.name(),
                RESUME.getValue());

        assertNotNull(ret2);
        assertThat(ret2.isOk()).isTrue();
        assertEquals(Status.ACCEPTED.getStatusCode(), ret2.getStatus());

        wait(containerName2);
        ProcessWorkflow processWorkflow2 = processMonitoring.findOneProcessWorkflow(containerName2, tenantId);
        assertNotNull(processWorkflow2);
        assertEquals(ProcessState.COMPLETED, processWorkflow2.getState());
        assertEquals(StatusCode.OK, processWorkflow2.getStatus());
        assertNotNull(processWorkflow2.getSteps());

        // Check that we have an AU where in his up we have idUnit
        MongoIterable<Document> newChildUnit = MetadataCollections.UNIT.getCollection().find(eq("_up", idUnit));
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
        processingClient.initVitamProcess(containerName3, DEFAULT_WORKFLOW.name());
        final RequestResponse<ItemStatus> ret3 =
            processingClient.executeOperationProcess(containerName3, DEFAULT_WORKFLOW.name(),
                RESUME.getValue());

        assertNotNull(ret3);
        assertThat(ret3.isOk()).isTrue();
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
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
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
        processingClient.initVitamProcess(containerName, DEFAULT_WORKFLOW.name());
        final RequestResponse<ItemStatus> ret =
            processingClient.executeOperationProcess(containerName, DEFAULT_WORKFLOW.name(),
                RESUME.getValue());

        assertNotNull(ret);
        assertThat(ret.isOk()).isTrue();
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
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
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
        final String containerName = ingestSIP(SIP_FILE_OK_NAME, DEFAULT_WORKFLOW.name(), StatusCode.WARNING);

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
        processingClient.initVitamProcess(containerName2, DEFAULT_WORKFLOW.name());
        final RequestResponse<ItemStatus> ret2 =
            processingClient.executeOperationProcess(containerName2, DEFAULT_WORKFLOW.name(),
                RESUME.getValue());

        assertNotNull(ret2);
        assertThat(ret2.isOk()).isTrue();
        assertEquals(Status.ACCEPTED.getStatusCode(), ret2.getStatus());

        wait(containerName2);
        ProcessWorkflow processWorkflow2 = processMonitoring.findOneProcessWorkflow(containerName2, tenantId);
        assertNotNull(processWorkflow2);
        assertEquals(ProcessState.COMPLETED, processWorkflow2.getState());
        assertEquals(StatusCode.WARNING, processWorkflow2.getStatus());
        assertNotNull(processWorkflow2.getSteps());

        // check got have to units
        assertEquals(MetadataCollections.UNIT.getCollection().count(eq("_og", idGot)), 2);

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
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
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
        processingClient.initVitamProcess(containerName, DEFAULT_WORKFLOW.name());
        final RequestResponse<ItemStatus> ret =
            processingClient.executeOperationProcess(containerName, DEFAULT_WORKFLOW.name(),
                RESUME.getValue());
        assertNotNull(ret);
        assertThat(ret.isOk()).isTrue();
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
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }
    }

    private void replaceStringInFile(String targetFilename, String textToReplace, String replacementText)
        throws IOException {
        Path path = PropertiesUtils.getResourcePath(targetFilename);
        Charset charset = StandardCharsets.UTF_8;

        String content = Files.readString(path, charset);
        content = content.replaceAll(textToReplace, replacementText);
        Files.write(path, content.getBytes(charset));
    }


    private void zipFolder(final Path path, final String zipFilePath) throws IOException {
        try (
            FileOutputStream fos = new FileOutputStream(zipFilePath);
            ZipOutputStream zos = new ZipOutputStream(fos)) {
            Files.walkFileTree(path, new SimpleFileVisitor<>() {
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
        createLogbookOperation(operationId, objectId, null, INGEST);
    }

    public void createLogbookOperation(GUID operationId, GUID objectId, String type, LogbookTypeProcess typeProc)
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException {

        final LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient();
        if (type == null) {
            type = "Process_SIP_unitary";
        }

        final LogbookOperationParameters initParameters = LogbookParameterHelper.newLogbookOperationParameters(
            operationId, type, objectId,
            typeProc, StatusCode.STARTED,
            operationId != null ? operationId.toString() : "outcomeDetailMessage",
            operationId);
        if ("EXPORT_DIP".equals(type)) {
            initParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                VitamLogbookMessages.getLabelOp("EXPORT_DIP.STARTED") + " : " + operationId);
        }
        ObjectNode rightsStatementIdentifier = JsonHandler.createObjectNode();
        rightsStatementIdentifier.put("AccessContract", VitamThreadUtils.getVitamSession().getContractId());
        initParameters
            .putParameterValue(LogbookParameterName.rightsStatementIdentifier, rightsStatementIdentifier.toString());
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
        ingestSIP(SIP_FILE_OK_NAME, BIG_WORKFLOW, StatusCode.WARNING);

        runner.stopWorkerServer();
        runner.startWorkerServer(VitamServerRunner.CONFIG_WORKER_PATH);
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
        processingClient.initVitamProcess(containerName, DEFAULT_WORKFLOW.name());
        final RequestResponse<ItemStatus> ret =
            processingClient.executeOperationProcess(containerName, DEFAULT_WORKFLOW.name(),
                RESUME.getValue());

        assertNotNull(ret);
        assertThat(ret.isOk()).isTrue();
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
        runner.startWorkerServer(VitamServerRunner.CONFIG_WORKER_PATH);
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

        ingestSIP(SIP_BUG_2182, DEFAULT_WORKFLOW.name(), StatusCode.KO);
    }


    @RunWithCustomExecutor
    @Test
    public void testWorkflowWithSIP_KO_AU_ref_BDO() throws Exception {
        prepareVitamSession();

        ingestSIP(SIP_FILE_KO_AU_REF_BDO, DEFAULT_WORKFLOW.name(), StatusCode.KO);
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
        processingClient.initVitamProcess(containerName, DEFAULT_WORKFLOW.name());

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


        ret = processingClient.updateOperationActionProcess(RESUME.getValue(),
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
        processingClient.initVitamProcess(containerName, DEFAULT_WORKFLOW.name());

        RequestResponse<ItemStatus> ret =
            processingClient.updateOperationActionProcess(RESUME.getValue(), containerName);
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
        processingClient.initVitamProcess(containerName, DEFAULT_WORKFLOW.name());


        RequestResponse<ItemStatus> ret =
            processingClient.updateOperationActionProcess(RESUME.getValue(), containerName);
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

        ingestSIP(SIP_REFERENCE_CONTRACT_KO, DEFAULT_WORKFLOW.name(), StatusCode.KO);
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
        processingClient.initVitamProcess(containerName, DEFAULT_WORKFLOW.name());

        RequestResponse<ItemStatus> ret =
            processingClient.updateOperationActionProcess(RESUME.getValue(), containerName);
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
        JsonNode events = logbookResult.get(RESULTS).get(0).get(EVENTS);
        verifyEvent(events, "ROLL_BACK.OK");
        verifyEvent(events, "PROCESS_SIP_UNITARY.WARNING");
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
        processingClient.initVitamProcess(containerName, Contexts.HOLDING_SCHEME.name());
        RequestResponse<ItemStatus> ret =
            processingClient.updateOperationActionProcess(RESUME.getValue(), containerName);
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

        ingestSIP(OK_SIP_SIGNATURE, DEFAULT_WORKFLOW.name(), StatusCode.OK);
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowComputeInheritedRules() throws Exception {
        prepareVitamSession();

        // Given ingest
        final String ingestOperation = ingestSIP(SIP_COMPLEX_RULES, DEFAULT_WORKFLOW.name(), StatusCode.OK);

        // Ensure no computed inherited rules by default
        SelectMultiQuery select = new SelectMultiQuery();
        CompareQuery query = QueryHelper.eq(VitamFieldsHelper.initialOperation(), ingestOperation);
        select.setQuery(query);

        MetaDataClient metaDataClient = MetaDataClientFactory.getInstance().getClient();
        JsonNode selectUnitsAfterIngest = metaDataClient.selectUnits(select.getFinalSelect()).get("$results");

        assertThat(selectUnitsAfterIngest.elements())
            .extracting(unit -> unit.get(VitamFieldsHelper.validComputedInheritedRules()))
            .allMatch(Objects::isNull);
        assertThat(selectUnitsAfterIngest.elements())
            .extracting(unit -> unit.get(VitamFieldsHelper.computedInheritedRules()))
            .allMatch(Objects::isNull);

        // When
        computeInheritedRules(select);

        // Then
        JsonNode selectUnitsAfterComputedInheritedRules =
            metaDataClient.selectUnits(select.getFinalSelect()).get("$results");

        assertThat(selectUnitsAfterComputedInheritedRules.elements())
            .extracting(unit -> unit.get(VitamFieldsHelper.validComputedInheritedRules()))
            .allMatch(JsonNode::booleanValue);
        assertThat(selectUnitsAfterComputedInheritedRules.elements())
            .extracting(unit -> unit.get(VitamFieldsHelper.computedInheritedRules()))
            .allMatch(Objects::nonNull);
        ObjectNode computedInheritedRules =
            (ObjectNode) Streams.stream(selectUnitsAfterComputedInheritedRules.elements())
                .filter(unit -> unit.get("Title").textValue().equals("Pereire.txt"))
                .findFirst()
                .get()
                .get(VitamFieldsHelper.computedInheritedRules());
        assertThat(computedInheritedRules.get("indexationDate")).isNotNull();
        // Check format ignoring indexationDate fields
        computedInheritedRules.remove("indexationDate");
        JsonAssert.assertJsonEquals(
            JsonHandler.getFromInputStream(PropertiesUtils
                .getResourceAsStream("integration-processing/expectedPereireComputedInheritedRules.json")),
            computedInheritedRules);
    }

    private void computeInheritedRules(SelectMultiQuery select)
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException,
        ContentAddressableStorageAlreadyExistException, ContentAddressableStorageServerException,
        InvalidParseOperationException, InternalServerException, BadRequestException, VitamClientException {
        final String computedInheritedRulesProcess = createOperationContainer();

        workspaceClient.createContainer(computedInheritedRulesProcess);
        workspaceClient
            .putObject(computedInheritedRulesProcess, "query.json", writeToInpustream(select.getFinalSelect()));
        processingClient
            .initVitamProcess(new ProcessingEntry(computedInheritedRulesProcess, COMPUTE_INHERITED_RULES.name()));
        RequestResponse<ItemStatus> cirResponse = processingClient
            .executeOperationProcess(computedInheritedRulesProcess, COMPUTE_INHERITED_RULES.name(), RESUME.getValue());
        assertNotNull(cirResponse);
        assertTrue(cirResponse.isOk());
        assertEquals(Status.ACCEPTED.getStatusCode(), cirResponse.getStatus());
        wait(computedInheritedRulesProcess);
        ProcessWorkflow cirWorkflow = processMonitoring.findOneProcessWorkflow(computedInheritedRulesProcess, tenantId);
        assertNotNull(cirWorkflow);
        assertEquals(ProcessState.COMPLETED, cirWorkflow.getState());
        assertEquals(StatusCode.OK, cirWorkflow.getStatus());
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowRulesUpdateWithoutComputedInheritedRules() throws Exception {
        prepareVitamSession();

        final String ingestOperation = ingestSIP(SIP_COMPLEX_RULES, DEFAULT_WORKFLOW.name(), StatusCode.OK);

        // Check no computed inherited rules by default
        SelectMultiQuery select = new SelectMultiQuery();
        CompareQuery query = QueryHelper.eq(VitamFieldsHelper.initialOperation(), ingestOperation);
        select.setQuery(query);

        MetaDataClient metaDataClient = MetaDataClientFactory.getInstance().getClient();
        JsonNode selectUnitsAfterIngest = metaDataClient.selectUnits(select.getFinalSelect()).get("$results");

        assertThat(selectUnitsAfterIngest.elements())
            .extracting(unit -> unit.get(VitamFieldsHelper.validComputedInheritedRules()))
            .allMatch(Objects::isNull);
        assertThat(selectUnitsAfterIngest.elements())
            .extracting(unit -> unit.get(VitamFieldsHelper.computedInheritedRules()))
            .allMatch(Objects::isNull);

        // Rule update

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
        processingClient.initVitamProcess(containerName, Contexts.UPDATE_RULES_ARCHIVE_UNITS.name());
        RequestResponse<ItemStatus> ret =
            processingClient.updateOperationActionProcess(RESUME.getValue(), containerName);
        assertNotNull(ret);
        assertTrue(ret.isOk());
        assertEquals(Status.ACCEPTED.getStatusCode(), ret.getStatus());

        wait(containerName);
        ProcessWorkflow processWorkflow = processMonitoring.findOneProcessWorkflow(containerName, tenantId);
        assertNotNull(processWorkflow);
        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
        assertEquals(StatusCode.OK, processWorkflow.getStatus());

        ArrayList<Document> logbookLifeCycleUnits =
            Lists.newArrayList(LogbookCollections.LIFECYCLE_UNIT.getCollection().find().iterator());

        List<Document> currentLogbookLifeCycleUnits =
            logbookLifeCycleUnits.stream().filter(t -> t.get("evIdProc").equals(ingestOperation))
                .collect(Collectors.toList());
        currentLogbookLifeCycleUnits.forEach((lifecycle) -> {
            List<Document> events = (List<Document>) lifecycle.get("events");
            List<Document> lifecycleEvent =
                events.stream().filter(t -> t.get("outDetail").equals("LFC.UPDATE_UNIT_RULES.OK"))
                    .collect(Collectors.toList());
            if (lifecycleEvent != null && lifecycleEvent.size() > 0) {
                String evDetData = Iterables.getOnlyElement(lifecycleEvent).getString(EVENT_DETAILS);
                assertThat(evDetData).containsIgnoringCase("diff");
                assertThat(evDetData).contains(ingestOperation);
                assertThat(Iterables.getOnlyElement(lifecycleEvent).getString("outMessg")).isEqualTo(
                    "Succès de la mise à jour des règles de gestion de l'unité archivistique");
            }
        });

        JsonNode selectUnitsAfterRuleUpdate = metaDataClient.selectUnits(select.getFinalSelect()).get("$results");

        // Check end dates
        assertThat(
            Streams.stream(selectUnitsAfterRuleUpdate.elements())
                .filter(unit -> unit.has("#management") &&
                    unit.get("#management").has("AccessRule") &&
                    unit.get("#management").get("AccessRule").has("Rules"))
                .flatMap(unit -> Streams.stream(unit.get("#management").get("AccessRule").get("Rules").elements()))
                .filter(rule -> rule.get("Rule").asText().equals("ACC-00003"))
        ).allMatch(rule -> rule.get("EndDate").asText().equals(
            LocalDateUtil.getLocalDateFromSimpleFormattedDate(rule.get("StartDate").asText()).plusYears(30).format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd"))));

        // Ensure no computed inherited rules
        assertThat(selectUnitsAfterRuleUpdate.elements())
            .extracting(unit -> unit.get(VitamFieldsHelper.validComputedInheritedRules()))
            .allMatch(Objects::isNull);
        assertThat(selectUnitsAfterRuleUpdate.elements())
            .extracting(unit -> unit.get(VitamFieldsHelper.computedInheritedRules()))
            .allMatch(Objects::isNull);
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowRulesUpdateWithValidComputedInheritedRules() throws Exception {
        prepareVitamSession();

        final String ingestOperation = ingestSIP(SIP_COMPLEX_RULES, DEFAULT_WORKFLOW.name(), StatusCode.OK);

        // computedInheritedRules
        final String computedInheritedRulesProcess = createOperationContainer();
        SelectMultiQuery select = new SelectMultiQuery();
        CompareQuery query = QueryHelper.eq(VitamFieldsHelper.initialOperation(), ingestOperation);
        select.setQuery(query);

        workspaceClient.createContainer(computedInheritedRulesProcess);
        workspaceClient
            .putObject(computedInheritedRulesProcess, "query.json", writeToInpustream(select.getFinalSelect()));
        processingClient
            .initVitamProcess(new ProcessingEntry(computedInheritedRulesProcess, COMPUTE_INHERITED_RULES.name()));
        RequestResponse<ItemStatus> cirResponse = processingClient
            .executeOperationProcess(computedInheritedRulesProcess, COMPUTE_INHERITED_RULES.name(), RESUME.getValue());
        assertNotNull(cirResponse);
        assertThat(cirResponse.isOk()).isTrue();
        assertEquals(Status.ACCEPTED.getStatusCode(), cirResponse.getStatus());

        wait(computedInheritedRulesProcess);
        ProcessWorkflow cirWorkflow = processMonitoring.findOneProcessWorkflow(computedInheritedRulesProcess, tenantId);
        assertNotNull(cirWorkflow);
        assertEquals(ProcessState.COMPLETED, cirWorkflow.getState());
        assertEquals(StatusCode.OK, cirWorkflow.getStatus());

        // Verify computed inherited rules existence
        MetaDataClient metaDataClient = MetaDataClientFactory.getInstance().getClient();
        JsonNode selectUnitsAfterComputedInheritedRules =
            metaDataClient.selectUnits(select.getFinalSelect()).get("$results");

        assertThat(selectUnitsAfterComputedInheritedRules.elements())
            .extracting(unit -> unit.get(VitamFieldsHelper.validComputedInheritedRules()))
            .allMatch(JsonNode::booleanValue);
        assertThat(selectUnitsAfterComputedInheritedRules.elements())
            .extracting(unit -> unit.get(VitamFieldsHelper.computedInheritedRules()))
            .allMatch(Objects::nonNull);

        // Rule update

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
        processingClient.initVitamProcess(containerName, Contexts.UPDATE_RULES_ARCHIVE_UNITS.name());
        RequestResponse<ItemStatus> ret =
            processingClient.updateOperationActionProcess(RESUME.getValue(), containerName);
        assertNotNull(ret);

        assertEquals(Status.ACCEPTED.getStatusCode(), ret.getStatus());

        wait(containerName);
        ProcessWorkflow processWorkflow = processMonitoring.findOneProcessWorkflow(containerName, tenantId);
        assertNotNull(processWorkflow);
        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
        assertEquals(StatusCode.OK, processWorkflow.getStatus());

        ArrayList<Document> logbookLifeCycleUnits =
            Lists.newArrayList(LogbookCollections.LIFECYCLE_UNIT.getCollection().find().iterator());

        List<Document> currentLogbookLifeCycleUnits =
            logbookLifeCycleUnits.stream().filter(t -> t.get("evIdProc").equals(ingestOperation))
                .collect(Collectors.toList());
        currentLogbookLifeCycleUnits.forEach((lifecycle) -> {
            List<Document> events = (List<Document>) lifecycle.get("events");
            List<Document> lifecycleEvent =
                events.stream().filter(t -> t.get("outDetail").equals("LFC.UPDATE_UNIT_RULES.OK"))
                    .collect(Collectors.toList());
            if (lifecycleEvent != null && lifecycleEvent.size() > 0) {
                String evDetData = Iterables.getOnlyElement(lifecycleEvent).getString(EVENT_DETAILS);
                assertThat(evDetData).containsIgnoringCase("diff");
                assertThat(evDetData).contains(ingestOperation);
                assertThat(Iterables.getOnlyElement(lifecycleEvent).getString("outMessg")).isEqualTo(
                    "Succès de la mise à jour des règles de gestion de l'unité archivistique");
            }
        });

        JsonNode selectUnitsAfterRuleUpdate = metaDataClient.selectUnits(select.getFinalSelect()).get("$results");

        // Check end dates
        assertThat(
            Streams.stream(selectUnitsAfterRuleUpdate.elements())
                .filter(unit -> unit.has("#management") &&
                    unit.get("#management").has("AccessRule") &&
                    unit.get("#management").get("AccessRule").has("Rules"))
                .flatMap(unit -> Streams.stream(unit.get("#management").get("AccessRule").get("Rules").elements()))
                .filter(rule -> rule.get("Rule").asText().equals("ACC-00003"))
        ).allMatch(rule -> rule.get("EndDate").asText().equals(
            LocalDateUtil.getLocalDateFromSimpleFormattedDate(rule.get("StartDate").asText()).plusYears(30).format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd"))));

        // Check computed inherited rules invalidation of updated units and all then children
        List<String> updatedUnitIds = Streams.stream(selectUnitsAfterRuleUpdate.elements())
            .filter(unit -> unit.has("#management") &&
                unit.get("#management").has("AccessRule") &&
                unit.get("#management").get("AccessRule").has("Rules") &&
                Streams.stream(unit.get("#management").get("AccessRule").get("Rules").elements())
                    .anyMatch(rule -> rule.get("Rule").asText().equals("ACC-00003")))
            .map(unit -> unit.get("#id").asText())
            .collect(Collectors.toList());

        Set<String> updateUnitIdsAndTheirChildren =
            Streams.stream(selectUnitsAfterRuleUpdate.elements())
                .filter(unit -> updatedUnitIds.contains(unit.get("#id").asText()) ||
                    Streams.stream(unit.get("#allunitups").elements())
                        .anyMatch(entry -> updatedUnitIds.contains(entry.asText())))
                .map(unit -> unit.get("#id").asText())
                .collect(Collectors.toSet());

        // Check units to be invalidated
        assertThat(Streams.stream(selectUnitsAfterRuleUpdate.elements())
            .filter(unit -> updateUnitIdsAndTheirChildren.contains(unit.get("#id").asText())))
            .extracting(unit -> unit.get(VitamFieldsHelper.validComputedInheritedRules()))
            .allMatch(entry -> !entry.booleanValue());
        assertThat(Streams.stream(selectUnitsAfterRuleUpdate.elements())
            .filter(unit -> updateUnitIdsAndTheirChildren.contains(unit.get("#id").asText())))
            .extracting(unit -> unit.get(VitamFieldsHelper.computedInheritedRules()))
            .allMatch(Objects::isNull);

        // Check units NOT to be invalidated
        assertThat(Streams.stream(selectUnitsAfterRuleUpdate.elements())
            .filter(unit -> !updateUnitIdsAndTheirChildren.contains(unit.get("#id").asText())))
            .extracting(unit -> unit.get(VitamFieldsHelper.validComputedInheritedRules()))
            .allMatch(JsonNode::booleanValue);
        assertThat(Streams.stream(selectUnitsAfterRuleUpdate.elements())
            .filter(unit -> !updateUnitIdsAndTheirChildren.contains(unit.get("#id").asText())))
            .extracting(unit -> unit.get(VitamFieldsHelper.computedInheritedRules()))
            .allMatch(Objects::nonNull);
    }

    private String ingestSIP(String sipFileName, String workflowName, StatusCode expectedStatus)
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException,
        FileNotFoundException, ContentAddressableStorageException, BadRequestException, InternalServerException,
        VitamClientException {
        final String ingestContainerName = createOperationContainer();
        final InputStream zipInputStreamSipObject = PropertiesUtils.getResourceAsStream(sipFileName);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(ingestContainerName);
        workspaceClient.uncompressObject(ingestContainerName, SIP_FOLDER, CommonMediaType.ZIP, zipInputStreamSipObject);

        // call processing
        processingClient = ProcessingManagementClientFactory.getInstance().getClient();
        processingClient.initVitamProcess(ingestContainerName, workflowName);
        RequestResponse<ItemStatus> ret2 =
            processingClient.executeOperationProcess(ingestContainerName, workflowName, RESUME.getValue());
        assertNotNull(ret2);
        assertTrue(ret2.isOk());
        assertEquals(Status.ACCEPTED.getStatusCode(), ret2.getStatus());
        wait(ingestContainerName);
        ProcessWorkflow processWorkflow2 = processMonitoring.findOneProcessWorkflow(ingestContainerName, tenantId);
        assertNotNull(processWorkflow2);
        assertEquals(ProcessState.COMPLETED, processWorkflow2.getState());
        assertEquals(expectedStatus, processWorkflow2.getStatus());
        return ingestContainerName;
    }

    @RunWithCustomExecutor
    @Test
    public void test_attach_to_au_then_add_object_to_with_alternative_date_format_ontology() throws Exception {
        prepareVitamSession();
        // 1. First we create an AU by sip
        String containerName = createOperationContainer();

        InputStream zipStream =
            PropertiesUtils.getResourceAsStream(SIP_MDD_SEDA_GOT);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP,
            zipStream);

        StreamUtils.closeSilently(zipStream);

        // call processing
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(tenantId));

        processingClient = ProcessingManagementClientFactory.getInstance().getClient();
        processingClient.initVitamProcess(containerName, DEFAULT_WORKFLOW.name());
        RequestResponse<ItemStatus> requestResponse =
            processingClient.executeOperationProcess(containerName, DEFAULT_WORKFLOW.name(),
                RESUME.getValue());

        assertNotNull(requestResponse);
        assertThat(requestResponse.isOk()).isTrue();
        assertEquals(Status.ACCEPTED.getStatusCode(), requestResponse.getStatus());
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(tenantId));

        wait(containerName);
        ProcessWorkflow processWorkflow =
            processMonitoring.findOneProcessWorkflow(containerName, tenantId);
        assertNotNull(processWorkflow);
        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
        assertEquals(StatusCode.WARNING, processWorkflow.getStatus());

        // 2. Add object to an existing GOT
        containerName = createOperationContainer();
        String zipName = ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE - 1) + ".zip";

        // prepare zip
        MongoIterable<Document> resultUnits = MetadataCollections.UNIT.getCollection().find(exists("_og", true));
        Document unit = resultUnits.first();
        String idUnit = unit.getString("_id");
        String idGot = unit.getString("_og");

        replaceStringInFile(ADD_OBJET_TO_GOT + "/manifest.xml", "(?<=<SystemId>).*?(?=</SystemId>)",
            idUnit);
        String zipPath =
            PropertiesUtils.getResourcePath(SIP_FILE_ADD_AU_LINK_OK_NAME_TARGET).toAbsolutePath().toString() +
                "/" + zipName;
        zipFolder(PropertiesUtils.getResourcePath(ADD_OBJET_TO_GOT), zipPath);


        // use link sip
        zipStream = new FileInputStream(new File(
            PropertiesUtils.getResourcePath(SIP_FILE_ADD_AU_LINK_OK_NAME_TARGET).toAbsolutePath() +
                "/" + zipName));

        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP,
            zipStream);

        StreamUtils.closeSilently(zipStream);

        processingClient = ProcessingManagementClientFactory.getInstance().getClient();
        processingClient.initVitamProcess(containerName, DEFAULT_WORKFLOW.name());
        requestResponse =
            processingClient.executeOperationProcess(containerName, DEFAULT_WORKFLOW.name(),
                RESUME.getValue());
        assertNotNull(requestResponse);
        assertEquals(Status.ACCEPTED.getStatusCode(), requestResponse.getStatus());

        wait(containerName);
        processWorkflow = processMonitoring.findOneProcessWorkflow(containerName, tenantId);
        assertNotNull(processWorkflow);
        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
        assertEquals(StatusCode.WARNING, processWorkflow.getStatus());
        assertNotNull(processWorkflow.getSteps());

        MongoIterable<Document> resultGots = MetadataCollections.OBJECTGROUP.getCollection().find(eq("_id", idGot));
        Document got = resultGots.first();
        assertNotNull(got);
        JsonNode gotJson = JsonHandler.getFromString(got.toJson());
        List<JsonNode> versions = gotJson.findValues("versions");
        assertEquals(2, versions.size());

        try {
            Files.delete(new File(zipPath).toPath());
        } catch (Exception e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }

    }

    @RunWithCustomExecutor
    @Test
    public void test_attach_to_au_then_add_object_to_with_invalid_type_external_ontology() throws Exception {
        prepareVitamSession();
        // 1. First we create an AU by sip
        String containerName = createOperationContainer();

        InputStream zipStream =
            PropertiesUtils.getResourceAsStream(SIP_UNKNOWN_FIELD_SEDA_GOT);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP,
            zipStream);

        StreamUtils.closeSilently(zipStream);

        // call processing
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(tenantId));

        processingClient = ProcessingManagementClientFactory.getInstance().getClient();
        processingClient.initVitamProcess(containerName, DEFAULT_WORKFLOW.name());
        RequestResponse<ItemStatus> requestResponse =
            processingClient.executeOperationProcess(containerName, DEFAULT_WORKFLOW.name(),
                RESUME.getValue());

        assertNotNull(requestResponse);
        assertThat(requestResponse.isOk()).isTrue();
        assertEquals(Status.ACCEPTED.getStatusCode(), requestResponse.getStatus());
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(tenantId));

        wait(containerName);
        ProcessWorkflow processWorkflow =
            processMonitoring.findOneProcessWorkflow(containerName, tenantId);
        assertNotNull(processWorkflow);
        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
        assertEquals(StatusCode.WARNING, processWorkflow.getStatus());

        Document operation =
            (Document) LogbookCollections.OPERATION.getCollection().find(eq("_id", containerName)).first();
        System.out.println(JsonHandler.prettyPrint(operation));

        // 2. Add object to an existing GOT
        containerName = createOperationContainer();
        String zipName = ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE - 1) + ".zip";

        // prepare zip
        MongoIterable<Document> resultUnits = MetadataCollections.UNIT.getCollection().find(exists("_og", true));
        Document unit = resultUnits.first();
        String idUnit = unit.getString("_id");
        String idGot = unit.getString("_og");

        replaceStringInFile(ADD_OBJET_TO_GOT + "/manifest.xml", "(?<=<SystemId>).*?(?=</SystemId>)",
            idUnit);
        String zipPath =
            PropertiesUtils.getResourcePath(SIP_FILE_ADD_AU_LINK_OK_NAME_TARGET).toAbsolutePath().toString() +
                "/" + zipName;
        zipFolder(PropertiesUtils.getResourcePath(ADD_OBJET_TO_GOT), zipPath);


        // use link sip
        zipStream = new FileInputStream(new File(
            PropertiesUtils.getResourcePath(SIP_FILE_ADD_AU_LINK_OK_NAME_TARGET).toAbsolutePath() +
                "/" + zipName));

        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP,
            zipStream);

        StreamUtils.closeSilently(zipStream);

        processingClient = ProcessingManagementClientFactory.getInstance().getClient();
        processingClient.initVitamProcess(containerName, DEFAULT_WORKFLOW.name());
        requestResponse =
            processingClient.executeOperationProcess(containerName, DEFAULT_WORKFLOW.name(),
                RESUME.getValue());
        assertNotNull(requestResponse);
        assertEquals(Status.ACCEPTED.getStatusCode(), requestResponse.getStatus());

        wait(containerName);
        processWorkflow = processMonitoring.findOneProcessWorkflow(containerName, tenantId);
        assertNotNull(processWorkflow);
        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
        assertEquals(StatusCode.KO, processWorkflow.getStatus());
        assertNotNull(processWorkflow.getSteps());
        operation =
            (Document) LogbookCollections.OPERATION.getCollection().find(eq("_id", containerName)).first();
        System.out.println(JsonHandler.prettyPrint(operation));

        MongoIterable<Document> resultGots = MetadataCollections.OBJECTGROUP.getCollection().find(eq("_id", idGot));
        Document got = resultGots.first();
        assertNotNull(got);
        JsonNode gotJson = JsonHandler.getFromString(got.toJson());
        List<JsonNode> versions = gotJson.findValues("versions");
        assertEquals(1, versions.size());

        try {
            Files.delete(new File(zipPath).toPath());
        } catch (Exception e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }

    }


    @RunWithCustomExecutor
    @Test
    public void test_attach_to_au_then_add_object_to_got_then_check_accession_register() throws Exception {
        prepareVitamSession();
        // 1. First we create an AU by sip
        String containerName = createOperationContainer();

        InputStream zipStream =
            PropertiesUtils.getResourceAsStream(SIP_PROD_SERV_A);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP,
            zipStream);

        StreamUtils.closeSilently(zipStream);

        // call processing
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(tenantId));

        processingClient = ProcessingManagementClientFactory.getInstance().getClient();
        processingClient.initVitamProcess(containerName, DEFAULT_WORKFLOW.name());
        RequestResponse<ItemStatus> requestResponse =
            processingClient.executeOperationProcess(containerName, DEFAULT_WORKFLOW.name(),
                RESUME.getValue());

        assertNotNull(requestResponse);
        assertThat(requestResponse.isOk()).isTrue();
        assertEquals(Status.ACCEPTED.getStatusCode(), requestResponse.getStatus());
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(tenantId));

        wait(containerName);
        ProcessWorkflow processWorkflow =
            processMonitoring.findOneProcessWorkflow(containerName, tenantId);
        assertNotNull(processWorkflow);
        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
        assertEquals(StatusCode.WARNING, processWorkflow.getStatus());

        // 2. Attach to an existing Unit
        String zipName = ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE - 1) + ".zip";

        // prepare zip
        MongoIterable<Document> resultUnits = MetadataCollections.UNIT.getCollection().find();
        Document unit = resultUnits.first();
        String idUnit = (String) unit.get("_id");
        String opiBefore = (String) unit.get("_opi");
        replaceStringInFile(SIP_PROD_SERV_B_ATTACHED + "/manifest.xml", "(?<=<SystemId>).*?(?=</SystemId>)",
            idUnit);
        String zipPath =
            PropertiesUtils.getResourcePath(SIP_FILE_ADD_AU_LINK_OK_NAME_TARGET).toAbsolutePath().toString() +
                "/" + zipName;
        zipFolder(PropertiesUtils.getResourcePath(SIP_PROD_SERV_B_ATTACHED), zipPath);

        containerName = createOperationContainer();

        // use link sip
        zipStream = new FileInputStream(new File(
            PropertiesUtils.getResourcePath(SIP_FILE_ADD_AU_LINK_OK_NAME_TARGET).toAbsolutePath() +
                "/" + zipName));

        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP,
            zipStream);

        StreamUtils.closeSilently(zipStream);

        processingClient = ProcessingManagementClientFactory.getInstance().getClient();
        processingClient.initVitamProcess(containerName, DEFAULT_WORKFLOW.name());
        requestResponse =
            processingClient.executeOperationProcess(containerName, DEFAULT_WORKFLOW.name(),
                RESUME.getValue());
        assertNotNull(requestResponse);
        assertEquals(Status.ACCEPTED.getStatusCode(), requestResponse.getStatus());

        wait(containerName);
        processWorkflow = processMonitoring.findOneProcessWorkflow(containerName, tenantId);
        assertNotNull(processWorkflow);
        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
        assertEquals(StatusCode.WARNING, processWorkflow.getStatus());
        assertNotNull(processWorkflow.getSteps());

        try {
            Files.delete(new File(zipPath).toPath());
        } catch (Exception e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }

        // Check _opi does not changed
        MongoIterable<Document> resultUnitsAfter =
            MetadataCollections.UNIT.getCollection().find(eq("_id", idUnit));
        Document unitAfter = resultUnitsAfter.first();
        String opiAfter = (String) unitAfter.get("_opi");
        assertEquals(opiBefore, opiAfter);


        // Check accession register detail
        long countDetails = FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getCollection().count(
            eq("OriginatingAgency", "P-A"));
        assertThat(countDetails).isEqualTo(1);

        long countSummary = FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getCollection().count(
            eq("OriginatingAgency", "P-A"));
        assertThat(countSummary).isEqualTo(1);


        MongoIterable<Document> accessReg =
            FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getCollection()
                .find(eq("OriginatingAgency", "P-A"));
        assertNotNull(accessReg);
        assertNotNull(accessReg.first());
        Document accessRegDoc = accessReg.first();
        // 2 units are attached - 1 was previously added
        assertEquals("1.0",
            ((Document) accessRegDoc.get("TotalUnits")).get(AccessionRegisterSummary.INGESTED).toString());

        assertEquals("1.0",
            ((Document) accessRegDoc.get("TotalObjects")).get(AccessionRegisterSummary.INGESTED).toString());

        // 1 Got is attached - 1 was previously added
        assertEquals("1.0",
            ((Document) accessRegDoc.get("TotalObjectGroups")).get(AccessionRegisterSummary.INGESTED).toString());

        // 285804 octets is attached - 4109 was previously added
        assertEquals("4109.0",
            ((Document) accessRegDoc.get("ObjectSize")).get(AccessionRegisterSummary.INGESTED).toString());

        // 3. Add object to an existing GOT
        containerName = createOperationContainer();
        zipName = ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE - 1) + ".zip";

        replaceStringInFile(ADD_OBJET_TO_GOT + "/manifest.xml", "(?<=<SystemId>).*?(?=</SystemId>)",
            idUnit);
        zipPath =
            PropertiesUtils.getResourcePath(SIP_FILE_ADD_AU_LINK_OK_NAME_TARGET).toAbsolutePath().toString() +
                "/" + zipName;
        zipFolder(PropertiesUtils.getResourcePath(ADD_OBJET_TO_GOT), zipPath);


        // use link sip
        zipStream = new FileInputStream(new File(
            PropertiesUtils.getResourcePath(SIP_FILE_ADD_AU_LINK_OK_NAME_TARGET).toAbsolutePath() +
                "/" + zipName));

        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP,
            zipStream);

        StreamUtils.closeSilently(zipStream);

        processingClient = ProcessingManagementClientFactory.getInstance().getClient();
        processingClient.initVitamProcess(containerName, DEFAULT_WORKFLOW.name());
        requestResponse =
            processingClient.executeOperationProcess(containerName, DEFAULT_WORKFLOW.name(),
                RESUME.getValue());
        assertNotNull(requestResponse);
        assertEquals(Status.ACCEPTED.getStatusCode(), requestResponse.getStatus());

        wait(containerName);
        processWorkflow = processMonitoring.findOneProcessWorkflow(containerName, tenantId);
        assertNotNull(processWorkflow);
        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
        assertEquals(StatusCode.WARNING, processWorkflow.getStatus());
        assertNotNull(processWorkflow.getSteps());

        try {
            Files.delete(new File(zipPath).toPath());
        } catch (Exception e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }

        // Re-check accession register detail

        countDetails = FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getCollection().count(
            eq("OriginatingAgency", "P-A"));
        assertThat(countDetails).isEqualTo(2);

        countSummary = FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getCollection().count(
            eq("OriginatingAgency", "P-A"));
        assertThat(countSummary).isEqualTo(1);

        accessReg =
            FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getCollection()
                .find(eq("OriginatingAgency", "P-A"));
        assertNotNull(accessReg);
        assertNotNull(accessReg.first());

        accessRegDoc = accessReg.first();
        // 2 units are attached - 1 was previously added
        assertEquals("2.0",
            ((Document) accessRegDoc.get("TotalUnits")).get(AccessionRegisterSummary.INGESTED).toString());

        assertEquals("2.0",
            ((Document) accessRegDoc.get("TotalObjects")).get(AccessionRegisterSummary.INGESTED).toString());

        // 1 Got is attached - 1 was previously added
        assertEquals("1.0",
            ((Document) accessRegDoc.get("TotalObjectGroups")).get(AccessionRegisterSummary.INGESTED).toString());

        // 285804 octets is attached - 4109 was previously added
        assertEquals("14077.0",
            ((Document) accessRegDoc.get("ObjectSize")).get(AccessionRegisterSummary.INGESTED).toString());


        // Check global accession register count
        countDetails = FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getCollection().countDocuments();
        assertThat(countDetails).isEqualTo(3);

        countSummary = FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getCollection().countDocuments();
        assertThat(countSummary).isEqualTo(2);
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
            String bulkProcessId = GUIDFactory.newGUID().toString();
            metaDataClient.insertUnitBulk(
                new BulkUnitInsertRequest(Arrays.asList(
                    new BulkUnitInsertEntry(Collections.emptySet(), addOpiToMetadata(JsonHandler
                            .getFromFile(PropertiesUtils.getResourceFile("integration-processing/unit_metadata.json")),
                        bulkProcessId)),
                    new BulkUnitInsertEntry(Collections.emptySet(),
                        addOpiToMetadata(JsonHandler.getFromFile(PropertiesUtils.getResourceFile(PROCESSING_UNIT_PLAN)),
                            bulkProcessId))
                )));
            writeUnitsLogbook(bulkProcessId);

            metaDataClient.refreshUnits();

            processingClient = ProcessingManagementClientFactory.getInstance().getClient();
            // Testing blank workflow
            processingClient.initVitamProcess(containerName, Contexts.BLANK_TEST.name());

            RequestResponse<ItemStatus> ret =
                processingClient.updateOperationActionProcess(RESUME.getValue(), containerName);
            assertNotNull(ret);
            assertEquals(Status.ACCEPTED.getStatusCode(), ret.getStatus());

            wait(containerName);
            ProcessWorkflow processWorkflow = processMonitoring.findOneProcessWorkflow(containerName, tenantId);
            assertNotNull(processWorkflow);
            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
            assertEquals(StatusCode.WARNING, processWorkflow.getStatus());

            Document operation =
                (Document) LogbookCollections.OPERATION.getCollection().find(eq("_id", containerName)).first();
            assertThat(operation).isNotNull();
            assertTrue(operation.toString().contains("CHECK_ARCHIVE_UNIT_PROFILE.OK"));

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
        processingClient.initVitamProcess(containerName, DEFAULT_WORKFLOW.name());
        // wait a little bit
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(tenantId));

        RequestResponse<ItemStatus> resp = processingClient
            .executeOperationProcess(containerName, DEFAULT_WORKFLOW.name(), RESUME.getValue());
        // wait a little bit
        assertNotNull(resp);
        assertThat(resp.isOk()).isTrue();
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

        ingestSIP(SIP_FILE_WRONG_DATE, DEFAULT_WORKFLOW.name(), StatusCode.KO);
    }

    @RunWithCustomExecutor
    @Test
    public void testIngestWithAURefObjShouldEndWithKO() throws Exception {
        prepareVitamSession();

        ingestSIP(SIP_KO_AU_REF_OBJ, DEFAULT_WORKFLOW.name(), StatusCode.KO);
    }

    @RunWithCustomExecutor
    @Test
    public void testIngestWithWrongUriShouldEndWithKO() throws Exception {
        prepareVitamSession();

        ingestSIP(SIP_KO_MANIFEST_URI, DEFAULT_WORKFLOW.name(), StatusCode.KO);
    }

    @RunWithCustomExecutor
    @Test
    public void should_not_insert_empty_arrayNode_in_appraisal_rule_when_ingest_SIP() throws Exception {
        prepareVitamSession();

        // 1. First we create an AU by sip
        ingestSIP(SIP_APPRAISAL_RULES, DEFAULT_WORKFLOW.name(), StatusCode.OK);

        MongoIterable<Document> resultUnits =
            MetadataCollections.UNIT.getCollection().find(eq("Title", "Porte de Pantin"));
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

        ingestSIP(SIP_FULL_SEDA_2_1, DEFAULT_WORKFLOW.name(), StatusCode.WARNING);

        MongoIterable<Document> resultUnits =
            MetadataCollections.UNIT.getCollection().find(eq("Title", "monSIP"));
        final Document unitToAssert = resultUnits.first();

        //AgentType fullname field

        List<Object> addressees = (List<Object>) unitToAssert.get("Addressee");
        assertThat(addressees).isNotNull().isNotEmpty();

        Document addressee = (Document) addressees.get(0);
        assertThat(addressee.get("FullName")).isEqualTo("Iulius Caesar Divus");

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
            .find(eq("_qualifiers.versions.Uri", "Content/IfTPz6AWS1VwRfNSlhsq83sMNPidvA.pdf"));
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

    @RunWithCustomExecutor
    @Test
    public void testWorkflowWithForcedPause() throws Exception {
        prepareVitamSession();

        final String containerName = createOperationContainer();

        // workspace client unzip SIP in workspace
        final InputStream zipInputStreamSipObject =
            PropertiesUtils.getResourceAsStream(SIP_FUND_REGISTER_OK);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP,
            zipInputStreamSipObject);
        // call processing
        processingClient = ProcessingManagementClientFactory.getInstance().getClient();
        processingClient.initVitamProcess(containerName, DEFAULT_WORKFLOW.name());
        //Add a pause for the tenant on INGEST process
        ProcessPause info = new ProcessPause("INGEST", tenantId, null);
        processingClient.forcePause(info);
        final RequestResponse<ItemStatus> ret =
            processingClient.executeOperationProcess(containerName, DEFAULT_WORKFLOW.name(),
                RESUME.getValue());

        assertNotNull(ret);
        assertThat(ret.isOk()).isTrue();
        assertEquals(Status.ACCEPTED.getStatusCode(), ret.getStatus());

        wait(containerName);

        ProcessWorkflow processWorkflow =
            processMonitoring.findOneProcessWorkflow(containerName, tenantId);
        assertNotNull(processWorkflow);
        // Verify pause
        assertEquals(ProcessState.PAUSE, processWorkflow.getState());
        assertEquals(StatusCode.OK, processWorkflow.getStatus());

        //Remove all pause for the tenant
        ProcessPause remove = new ProcessPause();
        remove.setTenant(tenantId);
        processingClient.removeForcePause(remove);

        processingClient.executeOperationProcess(containerName, DEFAULT_WORKFLOW.name(),
            RESUME.getValue());

        processWorkflow =
            processMonitoring.findOneProcessWorkflow(containerName, tenantId);
        assertNotNull(processWorkflow);

        wait(containerName);
        // Verify no pause
        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
        assertEquals(StatusCode.WARNING, processWorkflow.getStatus());
    }


    @RunWithCustomExecutor
    @Test
    public void test_add_objects_to_existing_object_group_OK() throws Exception {
        prepareVitamSession();

        String sip_ko = "integration-processing/add_objects_to_gots/KO_SIP_MULTIPLE_USAGES_MULTIPLE_VERSIONS.zip";
        String sip_ok_1 = "integration-processing/add_objects_to_gots/OK_SIP_MULTIPLE_USAGES.zip";
        String sip_ok_2 = "integration-processing/add_objects_to_gots/OK_ADD_OBJECTS.zip";

        // 1. Test Usage with multiple versions KO

        String containerName0 = createOperationContainer();

        VitamThreadUtils.getVitamSession().setRequestId(containerName0);
        // workspace client unzip SIP in workspace
        InputStream zipInputStreamSipObject = PropertiesUtils.getResourceAsStream(sip_ko);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName0);
        workspaceClient.uncompressObject(containerName0, SIP_FOLDER, CommonMediaType.ZIP, zipInputStreamSipObject);
        // call processing
        processingClient = ProcessingManagementClientFactory.getInstance().getClient();
        processingClient.initVitamProcess(containerName0, DEFAULT_WORKFLOW.name());
        RequestResponse<ItemStatus> ret =
            processingClient.executeOperationProcess(containerName0, DEFAULT_WORKFLOW.name(),
                RESUME.getValue());

        assertNotNull(ret);
        assertThat(ret.isOk()).isTrue();
        assertEquals(Status.ACCEPTED.getStatusCode(), ret.getStatus());

        wait(containerName0);
        ProcessWorkflow processWorkflow =
            processMonitoring.findOneProcessWorkflow(containerName0, tenantId);
        assertNotNull(processWorkflow);
        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
        assertEquals(StatusCode.KO, processWorkflow.getStatus());


        // 2. Test multiple usages with only one version for each one
        String containerName = createOperationContainer();

        VitamThreadUtils.getVitamSession().setRequestId(containerName);
        // workspace client unzip SIP in workspace
        zipInputStreamSipObject = PropertiesUtils.getResourceAsStream(sip_ok_1);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP, zipInputStreamSipObject);
        // call processing
        processingClient = ProcessingManagementClientFactory.getInstance().getClient();
        processingClient.initVitamProcess(containerName, DEFAULT_WORKFLOW.name());
        ret =
            processingClient.executeOperationProcess(containerName, DEFAULT_WORKFLOW.name(),
                RESUME.getValue());
        assertNotNull(ret);
        assertEquals(Status.ACCEPTED.getStatusCode(), ret.getStatus());

        wait(containerName);
        processWorkflow =
            processMonitoring.findOneProcessWorkflow(containerName, tenantId);
        assertNotNull(processWorkflow);
        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
        assertEquals(StatusCode.OK, processWorkflow.getStatus());

        assertThat(MetadataCollections.OBJECTGROUP.getCollection().countDocuments()).isEqualTo(1L);
        ObjectGroup got =
            (ObjectGroup) MetadataCollections.OBJECTGROUP.getCollection().find(ObjectGroup.class).iterator().next();
        assertThat(got.get(ObjectGroup.OPS, List.class)).hasSize(1);
        assertThat(got.get(ObjectGroup.QUALIFIERS, List.class)).hasSize(2);
        assertThat(got.get(ObjectGroup.QUALIFIERS, List.class)).extracting("qualifier", "_nbc")
            .contains(tuple("BinaryMaster", 1), tuple("PhysicalMaster", 1));

        // 2. Add objects to existing got
        String containerName2 = createOperationContainer();
        VitamThreadUtils.getVitamSession().setRequestId(containerName2);

        InputStream zipInputStreamSipObject2 = PropertiesUtils.getResourceAsStream(sip_ok_2);


        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName2);
        workspaceClient.uncompressObject(containerName2, SIP_FOLDER, CommonMediaType.ZIP, zipInputStreamSipObject2);

        // call processing
        processingClient = ProcessingManagementClientFactory.getInstance().getClient();
        processingClient.initVitamProcess(containerName2, DEFAULT_WORKFLOW.name());
        final RequestResponse<ItemStatus> ret2 =
            processingClient.executeOperationProcess(containerName2, DEFAULT_WORKFLOW.name(),
                RESUME.getValue());

        assertNotNull(ret2);
        assertThat(ret2.isOk()).isTrue();
        assertEquals(Status.ACCEPTED.getStatusCode(), ret2.getStatus());

        wait(containerName2);
        ProcessWorkflow processWorkflow2 = processMonitoring.findOneProcessWorkflow(containerName2, tenantId);
        assertNotNull(processWorkflow2);
        assertEquals(ProcessState.COMPLETED, processWorkflow2.getState());
        assertEquals(StatusCode.WARNING, processWorkflow2.getStatus());
        assertNotNull(processWorkflow2.getSteps());

        // Check fix bug_5178 bug_5117
        assertThat(MetadataCollections.OBJECTGROUP.getCollection().countDocuments()).isEqualTo(1L);
        got = (ObjectGroup) MetadataCollections.OBJECTGROUP.getCollection().find(ObjectGroup.class).iterator().next();
        assertThat(got.get(ObjectGroup.OPS, List.class)).hasSize(2);
        assertThat(got.get(ObjectGroup.QUALIFIERS, List.class)).hasSize(2);
        assertThat(got.get(ObjectGroup.QUALIFIERS, List.class)).extracting("qualifier", "_nbc")
            .contains(tuple("BinaryMaster", 3), tuple("PhysicalMaster", 1));

        // Fix check bug 5199. Assert that all versions of all qualifiers have DataObjectGroupId equals to got id
        String gotId = got.getString(ObjectGroup.ID);
        Stream<String> stream = ((List<Document>) got.get(ObjectGroup.QUALIFIERS, List.class))
            .stream()
            .flatMap(o -> ((List<Document>) o.get("versions", List.class)).stream())
            .map(o -> o.getString(SedaConstants.TAG_DATA_OBJECT_GROUPE_ID));

        assertThat(stream).hasSize(4).allMatch(gotId::equals);
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowReclassificationWithComputedInheritedRules() throws Exception {
        prepareVitamSession();

        // Given ingest
        final String ingestOperation =
            ingestSIP("integration-processing/4_UNITS_2_GOTS.zip", DEFAULT_WORKFLOW.name(), StatusCode.OK);

        // Compute inherited rules
        SelectMultiQuery select = new SelectMultiQuery();
        CompareQuery query = QueryHelper.eq(VitamFieldsHelper.initialOperation(), ingestOperation);
        select.setQuery(query);

        computeInheritedRules(select);

        // Then
        MetaDataClient metaDataClient = MetaDataClientFactory.getInstance().getClient();
        JsonNode selectUnitsAfterComputedInheritedRules =
            metaDataClient.selectUnits(select.getFinalSelect()).get("$results");

        assertThat(selectUnitsAfterComputedInheritedRules.elements())
            .extracting(unit -> unit.get(VitamFieldsHelper.validComputedInheritedRules()))
            .allMatch(JsonNode::booleanValue);
        assertThat(selectUnitsAfterComputedInheritedRules.elements())
            .extracting(unit -> unit.get(VitamFieldsHelper.computedInheritedRules()))
            .allMatch(Objects::nonNull);

        // When running reclassification : Attach C to B
        /*
         *         A                       A
         *      ↗  ↑            ==>      ↗ ↑
         *    B    |                   B   |
         *         |                     ↖ |
         *         C                       C
         *         ↑                       ↑
         *         D                       D
         */

        UpdateMultiQuery reclassificationRequest = new UpdateMultiQuery();
        reclassificationRequest.setQuery(QueryHelper.eq(VitamFieldsHelper.id(),
            getUnitId(getUnitIdByTitle(selectUnitsAfterComputedInheritedRules, "UnitC"))));
        reclassificationRequest.addActions(UpdateActionHelper.add(VitamFieldsHelper.unitups(),
            getUnitId(getUnitIdByTitle(selectUnitsAfterComputedInheritedRules, "UnitB"))));
        JsonNode reclassificationQuery = JsonHandler.createArrayNode().add(reclassificationRequest.getFinalUpdate());

        final String reclassificationWorkflow = createOperationContainer();
        VitamThreadUtils.getVitamSession().setRequestId(reclassificationWorkflow);

        workspaceClient.createContainer(reclassificationWorkflow);
        workspaceClient
            .putObject(reclassificationWorkflow, "request.json", writeToInpustream(reclassificationQuery));
        processingClient
            .initVitamProcess(new ProcessingEntry(reclassificationWorkflow, Contexts.RECLASSIFICATION.name()));
        RequestResponse<ItemStatus> cirResponse = processingClient
            .executeOperationProcess(reclassificationWorkflow, Contexts.RECLASSIFICATION.name(), RESUME.getValue());
        assertNotNull(cirResponse);
        assertTrue(cirResponse.isOk());
        assertEquals(Status.ACCEPTED.getStatusCode(), cirResponse.getStatus());
        wait(reclassificationWorkflow);
        ProcessWorkflow cirWorkflow = processMonitoring.findOneProcessWorkflow(reclassificationWorkflow, tenantId);
        assertNotNull(cirWorkflow);
        assertEquals(ProcessState.COMPLETED, cirWorkflow.getState());
        assertEquals(StatusCode.OK, cirWorkflow.getStatus());

        // Then
        JsonNode selectUnitsAfterReclassification =
            metaDataClient.selectUnits(select.getFinalSelect()).get("$results");

        // Basic reclassification check
        JsonNode unitA = getUnitIdByTitle(selectUnitsAfterReclassification, "UnitA");
        JsonNode unitB = getUnitIdByTitle(selectUnitsAfterReclassification, "UnitB");
        JsonNode unitC = getUnitIdByTitle(selectUnitsAfterReclassification, "UnitC");
        JsonNode unitD = getUnitIdByTitle(selectUnitsAfterReclassification, "UnitD");

        assertThat(getUnitParents(unitA)).isEmpty();
        assertThat(getUnitParents(unitB)).containsExactlyInAnyOrder(getUnitId(unitA));
        assertThat(getUnitParents(unitC)).containsExactlyInAnyOrder(getUnitId(unitA), getUnitId(unitB));
        assertThat(getUnitParents(unitD)).containsExactlyInAnyOrder(getUnitId(unitC));

        // Check computed inherited rules invalidation for unit C and its children D
        assertThat(unitA.get(VitamFieldsHelper.validComputedInheritedRules()).booleanValue()).isTrue();
        assertThat(unitB.get(VitamFieldsHelper.validComputedInheritedRules()).booleanValue()).isTrue();
        assertThat(unitC.get(VitamFieldsHelper.validComputedInheritedRules()).booleanValue()).isFalse();
        assertThat(unitD.get(VitamFieldsHelper.validComputedInheritedRules()).booleanValue()).isFalse();

        assertThat(unitA.get(VitamFieldsHelper.computedInheritedRules())).isNotNull();
        assertThat(unitB.get(VitamFieldsHelper.computedInheritedRules())).isNotNull();
        assertThat(unitC.get(VitamFieldsHelper.computedInheritedRules())).isNull();
        assertThat(unitD.get(VitamFieldsHelper.computedInheritedRules())).isNull();
    }

    @RunWithCustomExecutor
    @Test
    public void testWorkflowReclassificationWithoutComputedInheritedRules() throws Exception {
        prepareVitamSession();

        // Given ingest
        final String ingestOperation =
            ingestSIP("integration-processing/4_UNITS_2_GOTS.zip", DEFAULT_WORKFLOW.name(), StatusCode.OK);

        // Check no computed inherited rules after ingest
        SelectMultiQuery select = new SelectMultiQuery();
        CompareQuery query = QueryHelper.eq(VitamFieldsHelper.initialOperation(), ingestOperation);
        select.setQuery(query);

        MetaDataClient metaDataClient = MetaDataClientFactory.getInstance().getClient();
        JsonNode selectUnitsAfterComputedInheritedRules =
            metaDataClient.selectUnits(select.getFinalSelect()).get("$results");

        assertThat(selectUnitsAfterComputedInheritedRules.elements())
            .extracting(unit -> unit.get(VitamFieldsHelper.validComputedInheritedRules()))
            .allMatch(Objects::isNull);
        assertThat(selectUnitsAfterComputedInheritedRules.elements())
            .extracting(unit -> unit.get(VitamFieldsHelper.computedInheritedRules()))
            .allMatch(Objects::isNull);

        // When running reclassification : Attach C to B
        /*
         *         A                       A
         *      ↗  ↑            ==>      ↗ ↑
         *    B    |                   B   |
         *         |                     ↖ |
         *         C                       C
         *         ↑                       ↑
         *         D                       D
         */

        UpdateMultiQuery reclassificationRequest = new UpdateMultiQuery();
        reclassificationRequest.setQuery(QueryHelper.eq(VitamFieldsHelper.id(),
            getUnitId(getUnitIdByTitle(selectUnitsAfterComputedInheritedRules, "UnitC"))));
        reclassificationRequest.addActions(UpdateActionHelper.add(VitamFieldsHelper.unitups(),
            getUnitId(getUnitIdByTitle(selectUnitsAfterComputedInheritedRules, "UnitB"))));
        JsonNode reclassificationQuery = JsonHandler.createArrayNode().add(reclassificationRequest.getFinalUpdate());

        final String reclassificationWorkflow = createOperationContainer();
        VitamThreadUtils.getVitamSession().setRequestId(reclassificationWorkflow);

        workspaceClient.createContainer(reclassificationWorkflow);
        workspaceClient
            .putObject(reclassificationWorkflow, "request.json", writeToInpustream(reclassificationQuery));
        processingClient
            .initVitamProcess(new ProcessingEntry(reclassificationWorkflow, Contexts.RECLASSIFICATION.name()));
        RequestResponse<ItemStatus> cirResponse = processingClient
            .executeOperationProcess(reclassificationWorkflow, Contexts.RECLASSIFICATION.name(), RESUME.getValue());
        assertNotNull(cirResponse);
        assertTrue(cirResponse.isOk());
        assertEquals(Status.ACCEPTED.getStatusCode(), cirResponse.getStatus());
        wait(reclassificationWorkflow);
        ProcessWorkflow cirWorkflow = processMonitoring.findOneProcessWorkflow(reclassificationWorkflow, tenantId);
        assertNotNull(cirWorkflow);
        assertEquals(ProcessState.COMPLETED, cirWorkflow.getState());
        assertEquals(StatusCode.OK, cirWorkflow.getStatus());

        // Then
        JsonNode selectUnitsAfterReclassification =
            metaDataClient.selectUnits(select.getFinalSelect()).get("$results");

        // Basic reclassification check
        JsonNode unitA = getUnitIdByTitle(selectUnitsAfterReclassification, "UnitA");
        JsonNode unitB = getUnitIdByTitle(selectUnitsAfterReclassification, "UnitB");
        JsonNode unitC = getUnitIdByTitle(selectUnitsAfterReclassification, "UnitC");
        JsonNode unitD = getUnitIdByTitle(selectUnitsAfterReclassification, "UnitD");

        assertThat(getUnitParents(unitA)).isEmpty();
        assertThat(getUnitParents(unitB)).containsExactlyInAnyOrder(getUnitId(unitA));
        assertThat(getUnitParents(unitC)).containsExactlyInAnyOrder(getUnitId(unitA), getUnitId(unitB));
        assertThat(getUnitParents(unitD)).containsExactlyInAnyOrder(getUnitId(unitC));

        // Check computed inherited rules are still non indexed
        assertThat(selectUnitsAfterReclassification.elements())
            .extracting(unit -> unit.get(VitamFieldsHelper.validComputedInheritedRules()))
            .allMatch(Objects::isNull);
        assertThat(selectUnitsAfterReclassification.elements())
            .extracting(unit -> unit.get(VitamFieldsHelper.computedInheritedRules()))
            .allMatch(Objects::isNull);
    }

    @RunWithCustomExecutor
    @Test
    public void testGraphComputationWithComputedInheritedRules() throws Exception {
        prepareVitamSession();

        // Given ingest
        final String ingestOperation =
            ingestSIP("integration-processing/4_UNITS_2_GOTS.zip", DEFAULT_WORKFLOW.name(), StatusCode.OK);

        // Compute inherited rules
        SelectMultiQuery select = new SelectMultiQuery();
        CompareQuery query = QueryHelper.eq(VitamFieldsHelper.initialOperation(), ingestOperation);
        select.setQuery(query);

        computeInheritedRules(select);

        // Then
        MetaDataClient metaDataClient = MetaDataClientFactory.getInstance().getClient();
        JsonNode selectUnitsAfterComputedInheritedRules =
            metaDataClient.selectUnits(select.getFinalSelect()).get("$results");

        assertThat(selectUnitsAfterComputedInheritedRules.elements())
            .extracting(unit -> unit.get(VitamFieldsHelper.validComputedInheritedRules()))
            .allMatch(JsonNode::booleanValue);
        assertThat(selectUnitsAfterComputedInheritedRules.elements())
            .extracting(unit -> unit.get(VitamFieldsHelper.computedInheritedRules()))
            .allMatch(Objects::nonNull);

        // When recompute graph, no computed inherited rules invalidation
        GraphComputeResponse graphComputeResponse = metaDataClient.computeGraph(select.getFinalSelect());

        // Then
        assertThat(graphComputeResponse.getUnitCount()).isEqualTo(4);
        assertThat(graphComputeResponse.getGotCount()).isEqualTo(2);
        assertThat(graphComputeResponse.getErrorMessage()).isNull();

        // Check computed inherited rules have not been invalidated
        assertThat(selectUnitsAfterComputedInheritedRules.elements())
            .extracting(unit -> unit.get(VitamFieldsHelper.validComputedInheritedRules()))
            .allMatch(JsonNode::booleanValue);
        assertThat(selectUnitsAfterComputedInheritedRules.elements())
            .extracting(unit -> unit.get(VitamFieldsHelper.computedInheritedRules()))
            .allMatch(Objects::nonNull);
    }

    private JsonNode getUnitIdByTitle(JsonNode units, String title) {
        return Streams.stream(units.elements())
            .filter(unit -> unit.get("Title").asText().equals(title))
            .findFirst().get();
    }

    private String getUnitId(JsonNode unit) {
        return unit.get(VitamFieldsHelper.id()).asText();
    }

    private List<String> getUnitParents(JsonNode unit) {
        return Streams.stream(unit.get(VitamFieldsHelper.unitups()).elements())
            .map(JsonNode::asText)
            .collect(Collectors.toList());
    }

    private void verifyEvent(JsonNode events, String s) {
        List<JsonNode> massUpdateFinalized = events.findValues(OUT_DETAIL).stream()
                .filter(e -> e.asText().equals(s))
                .collect(Collectors.toList());
        assertThat(massUpdateFinalized.size()).isGreaterThan(0);
    }
}
