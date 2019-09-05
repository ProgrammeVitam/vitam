/*
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
 */
package fr.gouv.vitam.ingest.internal.integration.test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import fr.gouv.vitam.access.internal.client.AccessInternalClient;
import fr.gouv.vitam.access.internal.client.AccessInternalClientFactory;
import fr.gouv.vitam.access.internal.common.exception.AccessInternalClientNotFoundException;
import fr.gouv.vitam.access.internal.core.AccessInternalModuleImpl;
import fr.gouv.vitam.access.internal.rest.AccessInternalMain;
import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.DataLoader;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.accesslog.AccessLogUtils;
import fr.gouv.vitam.common.client.IngestCollection;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.client.VitamClientFactoryInterface;
import fr.gouv.vitam.common.configuration.ClassificationLevel;
import fr.gouv.vitam.common.database.api.VitamRepositoryFactory;
import fr.gouv.vitam.common.database.builder.query.CompareQuery;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.query.action.SetAction;
import fr.gouv.vitam.common.database.builder.query.action.UnsetAction;
import fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.multiple.UpdateMultiQuery;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.builder.request.single.Update;
import fr.gouv.vitam.common.database.parser.request.adapter.SingleVarNameAdapter;
import fr.gouv.vitam.common.database.parser.request.single.SelectParserSingle;
import fr.gouv.vitam.common.database.parser.request.single.UpdateParserSingle;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.database.utils.AccessContractRestrictionHelper;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.AccessUnauthorizedException;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.InternalServerException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.format.identification.FormatIdentifierFactory;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.VitamConstants;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.model.administration.IngestContractModel;
import fr.gouv.vitam.common.model.elimination.EliminationRequestBody;
import fr.gouv.vitam.common.model.processing.WorkFlow;
import fr.gouv.vitam.common.model.rules.InheritedRuleCategoryResponseModel;
import fr.gouv.vitam.common.model.rules.UnitInheritedRulesResponseModel;
import fr.gouv.vitam.common.model.unit.CustodialHistoryModel;
import fr.gouv.vitam.common.model.unit.DataObjectReference;
import fr.gouv.vitam.common.stream.SizedInputStream;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClient;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClientFactory;
import fr.gouv.vitam.ingest.internal.upload.rest.IngestInternalMain;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookDocument;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookElasticsearchAccess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbName;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookOperation;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.core.database.collections.ObjectGroup;
import fr.gouv.vitam.metadata.core.database.collections.Unit;
import fr.gouv.vitam.metadata.rest.MetadataMain;
import fr.gouv.vitam.processing.common.model.ProcessWorkflow;
import fr.gouv.vitam.processing.engine.core.monitoring.ProcessMonitoringImpl;
import fr.gouv.vitam.processing.management.rest.ProcessManagementMain;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.worker.server.rest.WorkerMain;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import io.restassured.RestAssured;
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
import org.junit.Test;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.VitamServerRunner.PORT_SERVICE_LOGBOOK;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static fr.gouv.vitam.common.guid.GUIDFactory.newOperationLogbookGUID;
import static fr.gouv.vitam.preservation.ProcessManagementWaiter.waitOperation;
import static io.restassured.RestAssured.get;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Ingest Internal integration test
 */
public class IngestInternalIT extends VitamRuleRunner {
    private static final String HOLDING_SCHEME = "HOLDING_SCHEME";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IngestInternalIT.class);
    private static final String HOLDING_SCHEME_IDENTIFIER = "HOLDINGSCHEME";
    private static final String LINE_3 = "line 3";
    private static final String LINE_2 = "line 2";
    private static final String JEU_DONNEES_OK_REGLES_CSV_CSV = "jeu_donnees_OK_regles_CSV.csv";
    private static final Integer tenantId = 0;
    private static final long SLEEP_TIME = 20L;
    private static final long NB_TRY = 18000; // equivalent to 16 minute
    private static final String METADATA_PATH = "/metadata/v1";
    private static final String PROCESSING_PATH = "/processing/v1";
    private static final String WORKER_PATH = "/worker/v1";
    private static final String WORKSPACE_PATH = "/workspace/v1";
    private static final String LOGBOOK_PATH = "/logbook/v1";
    private static final String INGEST_INTERNAL_PATH = "/ingest/v1";
    private static final String ACCESS_INTERNAL_PATH = "/access-internal/v1";
    private static final String WORKFLOW_ID = "DEFAULT_WORKFLOW";
    private static final String CONTEXT_ID = "PROCESS_SIP_UNITARY";
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
    @ClassRule
    public static VitamServerRunner runner =
        new VitamServerRunner(IngestInternalIT.class, mongoRule.getMongoDatabase().getName(),
            ElasticsearchRule.getClusterName(),
            Sets.newHashSet(
                MetadataMain.class,
                WorkerMain.class,
                AdminManagementMain.class,
                LogbookMain.class,
                WorkspaceMain.class,
                ProcessManagementMain.class,
                AccessInternalMain.class,
                IngestInternalMain.class));
    private static String CONFIG_SIEGFRIED_PATH = "";
    private static String SIP_TREE = "integration-ingest-internal/test_arbre.zip";
    private static String SIP_FILE_OK_NAME = "integration-ingest-internal/SIP-ingest-internal-ok.zip";
    private static String SIP_SIP_ALL_METADATA_WITH_CUSTODIALHISTORYFILE =
        "integration-ingest-internal/sip_all_metadata_with_custodialhistoryfile.zip";
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
    private static String SIP_OK_PHYSICAL_ARCHIVE = "integration-ingest-internal/OK_ArchivesPhysiques.zip";
    private static String SIP_OK_PHYSICAL_ARCHIVE_FOR_LFC =
        "integration-ingest-internal/OK_ArchivesPhysiques_for_LFC.zip";
    private static String SIP_KO_PHYSICAL_ARCHIVE_BINARY_IN_PHYSICAL =
        "integration-ingest-internal/KO_ArchivesPhysiques_BinaryInPhysical.zip";
    private static String SIP_KO_PHYSICAL_ARCHIVE_PHYSICAL_IN_BINARY =
        "integration-ingest-internal/KO_ArchivesPhysiques_PhysicalInBinary.zip";
    private static String SIP_KO_PHYSICAL_ARCHIVE_PHYSICAL_ID_EMPTY =
        "integration-ingest-internal/KO_ArchivesPhysiques_EmptyPhysicalId.zip";
    private static String SIP_OK_PHYSICAL_ARCHIVE_WITH_ATTACHMENT_FROM_CONTARCT =
        "integration-ingest-internal/OK_ArchivesPhysiques_With_Attachment_Contract.zip";
    private static String SIP_ARBRE = "integration-ingest-internal/arbre_simple.zip";
    private static String SIP_4396 = "integration-ingest-internal/OK_SIP_ClassificationRule_noRuleID.zip";
    private static String OK_RULES_COMPLEX_COMPLETE_SIP =
        "integration-ingest-internal/1069_OK_RULES_COMPLEXE_COMPLETE.zip";
    private static String OK_OBIDIN_MESSAGE_IDENTIFIER =
        "integration-ingest-internal/SIP-ingest-internal-ok.zip";
    private static String SIP_ALGO_INCORRECT_IN_MANIFEST = "integration-ingest-internal/SIP_INCORRECT_ALGORITHM.zip";
    private static LogbookElasticsearchAccess esClient;
    private WorkFlow holding = WorkFlow.of(HOLDING_SCHEME, HOLDING_SCHEME_IDENTIFIER, "MASTERDATA");
    private WorkFlow ingestSip = WorkFlow.of(WORKFLOW_ID, CONTEXT_ID, "INGEST");

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        handleBeforeClass(0, 1);
        CONFIG_SIEGFRIED_PATH =
            PropertiesUtils.getResourcePath("integration-ingest-internal/format-identifiers.conf").toString();

        FormatIdentifierFactory.getInstance().changeConfigurationFile(CONFIG_SIEGFRIED_PATH);

        // ES client
        final List<ElasticsearchNode> esNodes = new ArrayList<>();
        esNodes.add(new ElasticsearchNode("localhost", ElasticsearchRule.getTcpPort()));
        esClient = new LogbookElasticsearchAccess(ElasticsearchRule.getClusterName(), esNodes);

        StorageClientFactory storageClientFactory = StorageClientFactory.getInstance();
        storageClientFactory.setVitamClientType(VitamClientFactoryInterface.VitamClientType.MOCK);
        new DataLoader("integration-ingest-internal").prepareData();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        handleAfterClass(0, 1);
        StorageClientFactory storageClientFactory = StorageClientFactory.getInstance();
        storageClientFactory.setVitamClientType(VitamClientFactoryInterface.VitamClientType.PRODUCTION);
        runAfter();
        VitamClientFactory.resetConnections();
    }

    public static void prepareVitamSession() {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        VitamThreadUtils.getVitamSession().setContractId("aName3");
        VitamThreadUtils.getVitamSession().setContextId("Context_IT");
    }

    @Before
    public void setUpBefore() throws Exception {
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(0));
    }

    @RunWithCustomExecutor
    @Test
    public void testServersStatus() throws Exception {
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

        RestAssured.port = PORT_SERVICE_LOGBOOK;
        RestAssured.basePath = LOGBOOK_PATH;
        get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());

        RestAssured.port = VitamServerRunner.PORT_SERVICE_INGEST_INTERNAL;
        RestAssured.basePath = INGEST_INTERNAL_PATH;
        get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());

        RestAssured.port = VitamServerRunner.PORT_SERVICE_ACCESS_INTERNAL;
        RestAssured.basePath = ACCESS_INTERNAL_PATH;
        get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());
    }

    @RunWithCustomExecutor
    @Test
    public void testIngestInternal() throws Exception {
        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        try {
            prepareVitamSession();
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);

            // workspace client unzip SIP in workspace
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

            // call ingest
            IngestInternalClientFactory.getInstance().changeServerPort(runner.PORT_SERVICE_INGEST_INTERNAL);
            final IngestInternalClient client = IngestInternalClientFactory.getInstance().getClient();
            client.uploadInitialLogbook(params);

            // init workflow before execution
            client.initWorkflow(ingestSip);

            client.upload(zipInputStreamSipObject, CommonMediaType.ZIP_TYPE, ingestSip, ProcessAction.RESUME.name());

            awaitForWorkflowTerminationWithStatus(operationGuid, StatusCode.OK);

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
            assertThat(unit.get("#management").get("NeedAuthorization").asBoolean()).isFalse();
            // Try to check OG
            select = new SelectMultiQuery();
            select.addRoots(og);
            final JsonNode jsonResponse = metadataClient.selectObjectGrouptbyId(select.getFinalSelect(), og);
            LOGGER.warn("Result: " + jsonResponse);
            RequestResponseOK<ObjectGroup> objectGroupResponse =
                JsonHandler.getFromJsonNode(jsonResponse, RequestResponseOK.class, ObjectGroup.class);
            assertThat(objectGroupResponse).isNotNull();
            List<ObjectGroup> objectGroupList = objectGroupResponse.getResults();
            assertThat(objectGroupList).hasSize(1);
            ObjectGroup objectGroup = objectGroupList.iterator().next();
            // Bug 5159: check that all ObjectGroup _up are in _us
            assertThat(objectGroup.get(VitamFieldsHelper.allunitups(), List.class))
                .containsAll(objectGroup.get(VitamFieldsHelper.unitups(), List.class));
            final String objectId = objectGroup.getId();
            final StorageClient storageClient = StorageClientFactory.getInstance().getClient();
            Response responseStorage =
                storageClient.getContainerAsync(VitamConfiguration.getDefaultStrategy(), objectId,
                    DataCategory.OBJECT, AccessLogUtils.getNoLogAccessLog());
            InputStream inputStream = responseStorage.readEntity(InputStream.class);
            SizedInputStream sizedInputStream = new SizedInputStream(inputStream);
            final long size = StreamUtils.closeSilently(sizedInputStream);
            LOGGER.warn("read: " + size);

            assertTrue(size > 1000);

            final AccessInternalClient accessClient = AccessInternalClientFactory.getInstance().getClient();
            responseStorage = accessClient.getObject(og, "BinaryMaster", 1, "unitId");
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
            RequestResponse<JsonNode> updateResponse =
                accessClient.updateUnitbyId(updateQuery2.getFinalUpdate(), unitId);
            assertThat(updateResponse.isOk()).isFalse();
            assertThat(((VitamError) updateResponse).getDescription()).contains("Archive Unit Profile not found");

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
                    "\"$action\":[{\"$set\": {\"#management.AccessRule.Inheritance\" : {\"PreventRulesId\": [], \"PreventInheritance\": false}}}]}";
            RequestResponse responsePreventInheritance = accessClient
                .updateUnitbyId(JsonHandler.getFromString(queryUpdate), unitId);
            assertTrue(responsePreventInheritance.isOk());
            assertEquals(responsePreventInheritance.getHttpCode(), Status.OK.getStatusCode());

            // lets find details for the unit -> AccessRule should have been set
            RequestResponseOK<JsonNode> responseUnitAfterUpdatePreventInheritance =
                (RequestResponseOK) accessClient.selectUnitbyId(new SelectMultiQuery().getFinalSelect(), unitId);
            assertEquals(0,
                responseUnitAfterUpdatePreventInheritance.getFirstResult().get("#management").get("AccessRule")
                    .get("Inheritance").get("PreventRulesId").size());
            assertEquals(false,
                responseUnitAfterUpdatePreventInheritance.getFirstResult().get("#management").get("AccessRule")
                    .get("Inheritance").get("PreventInheritance").asBoolean());

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
        ObjectNode queryDsl = parser.getRequest().getFinalSelect();

        JsonNode lfcResponse = accessClient.selectUnitLifeCycleById(unitId, queryDsl).toJsonNode();
        final JsonNode result = lfcResponse.get("$results");
        assertNotNull(result);
        final JsonNode lfc = result.get(0);
        assertNotNull(lfc);

        return lfc;
    }

    private JsonNode retrieveLfcForGot(String gotId, AccessInternalClient accessClient) throws Exception {
        final SelectParserSingle parser = new SelectParserSingle();
        Select selectLFC = new Select();
        parser.parse(selectLFC.getFinalSelect());
        ObjectNode queryDsl = parser.getRequest().getFinalSelect();

        JsonNode lfcResponse = accessClient.selectObjectGroupLifeCycleById(gotId, queryDsl).toJsonNode();
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
            prepareVitamSession();
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            // workspace client unzip SIP in workspace
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
            IngestInternalClientFactory.getInstance().changeServerPort(runner.PORT_SERVICE_INGEST_INTERNAL);
            final IngestInternalClient client = IngestInternalClientFactory.getInstance().getClient();
            client.uploadInitialLogbook(params);

            // init workflow before execution
            client.initWorkflow(ingestSip);

            client.upload(zipInputStreamSipObject, CommonMediaType.ZIP_TYPE, ingestSip, ProcessAction.RESUME.name());

            awaitForWorkflowTerminationWithStatus(operationGuid, StatusCode.WARNING);

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
        prepareVitamSession();

        FileInputStream stream = new FileInputStream(PropertiesUtils.findFile(FILE_AGENCIES_OK));
        String operationId;
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
            prepareVitamSession();
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            // workspace client unzip SIP in workspace
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
            IngestInternalClientFactory.getInstance().changeServerPort(runner.PORT_SERVICE_INGEST_INTERNAL);
            final IngestInternalClient client = IngestInternalClientFactory.getInstance().getClient();
            client.uploadInitialLogbook(params);

            // init workflow before execution
            client.initWorkflow(ingestSip);

            client.upload(zipInputStreamSipObject, CommonMediaType.ZIP_TYPE, ingestSip, ProcessAction.RESUME.name());

            awaitForWorkflowTerminationWithStatus(operationGuid, StatusCode.WARNING);

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

            JsonNode unitups = unit.get(BuilderToken.PROJECTIONARGS.UNITUPS.exactToken());
            assertTrue(unitups.isArray());
            assertTrue(unitups.toString().contains(linkParentId));

            // check evDetData of checkManifest event
            JsonNode checkManifestEvent = lfc.get(LogbookDocument.EVENTS).get(0);
            assertEquals(checkManifestEvent.get("evType").asText(), "LFC.CHECK_MANIFEST");
            assertNotNull(checkManifestEvent.get("_lastPersistedDate"));
            assertEquals(checkManifestEvent.get("evDetData").asText(),
                "{\n  \"_up\" : [ \"" + linkParentId + "\" ]\n}");
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
            prepareVitamSession();
            final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            // workspace client unzip SIP in workspace
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
            IngestInternalClientFactory.getInstance().changeServerPort(runner.PORT_SERVICE_INGEST_INTERNAL);
            final IngestInternalClient client = IngestInternalClientFactory.getInstance().getClient();
            client.uploadInitialLogbook(params);

            // init workflow before execution
            client.initWorkflow(holding);
            client.upload(zipInputStreamSipObject, CommonMediaType.ZIP_TYPE, holding, ProcessAction.RESUME.name());

            awaitForWorkflowTerminationWithStatus(operationGuid, StatusCode.OK);

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
            prepareVitamSession();
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            // workspace client unzip SIP in workspace
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
            IngestInternalClientFactory.getInstance().changeServerPort(runner.PORT_SERVICE_INGEST_INTERNAL);
            final IngestInternalClient client = IngestInternalClientFactory.getInstance().getClient();
            client.uploadInitialLogbook(params);

            // init workflow before execution
            client.initWorkflow(ingestSip);

            client.upload(zipInputStreamSipObject, CommonMediaType.ZIP_TYPE, ingestSip, ProcessAction.RESUME.name());

            awaitForWorkflowTerminationWithStatus(operationGuid, StatusCode.KO);

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
    public void testPhysicalArchiveWithPhysicalMasterInBinary() throws Exception {
        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        try {
            prepareVitamSession();
            // workspace client unzip SIP in workspace
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
            IngestInternalClientFactory.getInstance().changeServerPort(runner.PORT_SERVICE_INGEST_INTERNAL);
            final IngestInternalClient client = IngestInternalClientFactory.getInstance().getClient();
            client.uploadInitialLogbook(params);

            // init workflow before execution
            client.initWorkflow(ingestSip);

            client.upload(zipInputStreamSipObject, CommonMediaType.ZIP_TYPE, ingestSip, ProcessAction.RESUME.name());

            awaitForWorkflowTerminationWithStatus(operationGuid, StatusCode.KO);

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
            prepareVitamSession();
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            // workspace client unzip SIP in workspace
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
            IngestInternalClientFactory.getInstance().changeServerPort(runner.PORT_SERVICE_INGEST_INTERNAL);
            final IngestInternalClient client = IngestInternalClientFactory.getInstance().getClient();
            client.uploadInitialLogbook(params);

            // init workflow before execution
            client.initWorkflow(ingestSip);

            client.upload(zipInputStreamSipObject, CommonMediaType.ZIP_TYPE, ingestSip, ProcessAction.RESUME.name());

            awaitForWorkflowTerminationWithStatus(operationGuid, StatusCode.KO);

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
    public void testIngestInternal2182CA1() throws Exception {
        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        prepareVitamSession();
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        // workspace client unzip SIP in workspace
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
        IngestInternalClientFactory.getInstance().changeServerPort(runner.PORT_SERVICE_INGEST_INTERNAL);
        final IngestInternalClient client = IngestInternalClientFactory.getInstance().getClient();
        client.uploadInitialLogbook(params);

        // init workflow before execution
        client.initWorkflow(ingestSip);

        client.upload(zipInputStreamSipObject, CommonMediaType.ZIP_TYPE, ingestSip, ProcessAction.RESUME.name());

        awaitForWorkflowTerminationWithStatus(operationGuid, StatusCode.KO);

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
        prepareVitamSession();
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        // workspace client unzip SIP in workspace
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
        IngestInternalClientFactory.getInstance().changeServerPort(runner.PORT_SERVICE_INGEST_INTERNAL);
        final IngestInternalClient client = IngestInternalClientFactory.getInstance().getClient();
        client.uploadInitialLogbook(params);

        // init workflow before execution
        client.initWorkflow(ingestSip);

        client.upload(zipInputStreamSipObject, CommonMediaType.ZIP_TYPE, ingestSip, ProcessAction.RESUME.name());

        awaitForWorkflowTerminationWithStatus(operationGuid, StatusCode.KO);


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
        prepareVitamSession();
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        // workspace client unzip SIP in workspace
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
        IngestInternalClientFactory.getInstance().changeServerPort(runner.PORT_SERVICE_INGEST_INTERNAL);
        final IngestInternalClient client = IngestInternalClientFactory.getInstance().getClient();
        client.uploadInitialLogbook(params);

        // init workflow before execution
        client.initWorkflow(ingestSip);

        client.upload(zipInputStreamSipObject, CommonMediaType.ZIP_TYPE, ingestSip, ProcessAction.RESUME.name());

        awaitForWorkflowTerminationWithStatus(operationGuid, StatusCode.KO);


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
        prepareVitamSession();
        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        // TODO: 6/6/17 why objectGuid ? The test fail on the logbook
        final GUID objectGuid = GUIDFactory.newManifestGUID(0);
        // workspace client unzip SIP in workspace
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
        IngestInternalClientFactory.getInstance().changeServerPort(runner.PORT_SERVICE_INGEST_INTERNAL);
        final IngestInternalClient client = IngestInternalClientFactory.getInstance().getClient();
        client.uploadInitialLogbook(params);

        // init workflow before execution
        client.initWorkflow(ingestSip);

        client.upload(zipInputStreamSipObject, CommonMediaType.ZIP_TYPE, ingestSip, ProcessAction.RESUME.name());

        awaitForWorkflowTerminationWithStatus(operationGuid, StatusCode.KO);

    }

    @RunWithCustomExecutor
    @Test
    public void testIngestWithManifestHavingMgtRules() throws Exception {
        prepareVitamSession();
        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        // ProcessDataAccessImpl processData = ProcessDataAccessImpl.getInstance();
        // processData.initProcessWorkflow(ProcessPopulator.populate(WORFKLOW_NAME), operationGuid.getId(),
        // ProcessAction.INIT, LogbookTypeProcess.INGEST, tenantId);
        // workspace client unzip SIP in workspace
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
        IngestInternalClientFactory.getInstance().changeServerPort(runner.PORT_SERVICE_INGEST_INTERNAL);
        final IngestInternalClient client = IngestInternalClientFactory.getInstance().getClient();
        client.uploadInitialLogbook(params);

        // init workflow before execution
        client.initWorkflow(ingestSip);
        client.upload(zipInputStreamSipObject, CommonMediaType.ZIP_TYPE, ingestSip, ProcessAction.RESUME.name());

        awaitForWorkflowTerminationWithStatus(operationGuid, StatusCode.WARNING);

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
        prepareVitamSession();
        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);


        // workspace client unzip SIP in workspace
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
        IngestInternalClientFactory.getInstance().changeServerPort(runner.PORT_SERVICE_INGEST_INTERNAL);
        final IngestInternalClient client = IngestInternalClientFactory.getInstance().getClient();
        client.uploadInitialLogbook(params);

        // init workflow before execution
        client.initWorkflow(ingestSip);

        client.upload(zipInputStreamSipObject, CommonMediaType.ZIP_TYPE, ingestSip, ProcessAction.RESUME.name());
        waitOperation(NB_TRY, SLEEP_TIME, operationGuid.toString());

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
        prepareVitamSession();
        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);

        // workspace client unzip SIP in workspace
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
        IngestInternalClientFactory.getInstance().changeServerPort(runner.PORT_SERVICE_INGEST_INTERNAL);
        final IngestInternalClient client = IngestInternalClientFactory.getInstance().getClient();
        client.uploadInitialLogbook(params);

        // init workflow before execution
        client.initWorkflow(ingestSip);

        client.upload(zipInputStreamSipObject, CommonMediaType.ZIP_TYPE, ingestSip, ProcessAction.RESUME.name());

        awaitForWorkflowTerminationWithStatus(operationGuid, StatusCode.WARNING);

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
        prepareVitamSession();
        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        // workspace client unzip SIP in workspace
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
        IngestInternalClientFactory.getInstance().changeServerPort(runner.PORT_SERVICE_INGEST_INTERNAL);
        final IngestInternalClient client = IngestInternalClientFactory.getInstance().getClient();
        client.uploadInitialLogbook(params);

        // init workflow before execution
        client.initWorkflow(ingestSip);
        client.upload(zipInputStreamSipObject, CommonMediaType.ZIP_TYPE, ingestSip, ProcessAction.RESUME.name());

        awaitForWorkflowTerminationWithStatus(operationGuid, StatusCode.KO);

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
        prepareVitamSession();
        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        // workspace client unzip SIP in workspace
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
        IngestInternalClientFactory.getInstance().changeServerPort(runner.PORT_SERVICE_INGEST_INTERNAL);
        final IngestInternalClient client = IngestInternalClientFactory.getInstance().getClient();
        client.uploadInitialLogbook(params);

        // init workflow before execution
        client.initWorkflow(ingestSip);
        client.upload(zipInputStreamSipObject, CommonMediaType.ZIP_TYPE, ingestSip, ProcessAction.RESUME.name());

        awaitForWorkflowTerminationWithStatus(operationGuid, StatusCode.WARNING);

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
        prepareVitamSession();
        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        // workspace client unzip SIP in workspace
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
        IngestInternalClientFactory.getInstance().changeServerPort(runner.PORT_SERVICE_INGEST_INTERNAL);
        final IngestInternalClient client = IngestInternalClientFactory.getInstance().getClient();
        client.uploadInitialLogbook(params);

        // init workflow before execution
        client.initWorkflow(ingestSip);
        client.upload(zipInputStreamSipObject, CommonMediaType.ZIP_TYPE, ingestSip, ProcessAction.RESUME.name());

        awaitForWorkflowTerminationWithStatus(operationGuid, StatusCode.WARNING);

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
            prepareVitamSession();
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            // workspace client unzip SIP in workspace
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
            IngestInternalClientFactory.getInstance().changeServerPort(runner.PORT_SERVICE_INGEST_INTERNAL);
            final IngestInternalClient client = IngestInternalClientFactory.getInstance().getClient();
            client.uploadInitialLogbook(params);

            // init workflow before execution
            client.initWorkflow(ingestSip);
            client.upload(zipInputStreamSipObject, CommonMediaType.ZIP_TYPE, ingestSip, ProcessAction.RESUME.name());

            awaitForWorkflowTerminationWithStatus(operationGuid, StatusCode.OK);
            ProcessWorkflow processWorkflow;

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
            client2.initWorkflow(holding);
            client2.upload(zipInputStreamSipObject2, CommonMediaType.ZIP_TYPE, holding, ProcessAction.RESUME.name());

            VitamThreadUtils.getVitamSession().setContractId("aName4");

            waitOperation(NB_TRY, SLEEP_TIME, operationGuid.toString());
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
        prepareVitamSession();
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
        prepareVitamSession();

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
            .getFromFileAsTypeRefence(fileAccessContracts, new TypeReference<List<AccessContractModel>>() {
            });
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
        prepareVitamSession();
        FileInputStream stream = new FileInputStream(PropertiesUtils.findFile(FILE_RULES_KO_DUPLICATED_REFERENCE));
        FileInputStream streamErrorReport = new FileInputStream(PropertiesUtils.findFile(ERROR_REPORT_CONTENT));
        checkFileRulesWithCustomReferential(stream, streamErrorReport, LINE_3);
    }


    @Test
    @RunWithCustomExecutor
    public void shouldRetrieveReportWhencheckFileRulesError6000Day() throws Exception {
        prepareVitamSession();
        FileInputStream stream = new FileInputStream(PropertiesUtils.findFile(FILE_RULES_KO_600000_DAY));
        FileInputStream streamErrorReport = new FileInputStream(PropertiesUtils.findFile(ERROR_REPORT_6000_DAYS));
        checkFileRulesWithCustomReferential(stream, streamErrorReport, LINE_2);
    }

    @Test
    @RunWithCustomExecutor
    public void shouldRetrieveReportWhencheckFileRulesError9000Years() throws Exception {
        prepareVitamSession();
        FileInputStream stream = new FileInputStream(PropertiesUtils.findFile(FILE_RULES_KO_90000_YEAR));
        FileInputStream streamErrorReport = new FileInputStream(PropertiesUtils.findFile(ERROR_REPORT_9000_YEARS));
        checkFileRulesWithCustomReferential(stream, streamErrorReport, LINE_2);
    }

    @Test
    @RunWithCustomExecutor
    public void shouldRetrieveReportWhenCheckFileRulesErrorAnarchyRules() throws Exception {
        prepareVitamSession();
        FileInputStream stream = new FileInputStream(PropertiesUtils.findFile(FILE_RULES_KO_ANARCHY_RULE));
        FileInputStream streamErrorReport =
            new FileInputStream(PropertiesUtils.findFile(ERROR_REPORT_ANARCHY_RULE));
        checkFileRulesWithCustomReferential(stream, streamErrorReport, LINE_2);
    }

    @Test
    @RunWithCustomExecutor
    public void shouldRetrieveReportWhenCheckFileRulesDecadeMeasurement() throws Exception {
        prepareVitamSession();
        FileInputStream stream = new FileInputStream(PropertiesUtils.findFile(FILE_RULES_KO_DECADE_MEASURE));
        FileInputStream streamErrorReport =
            new FileInputStream(PropertiesUtils.findFile(ERROR_REPORT_DECADE_MEASURE));
        checkFileRulesWithCustomReferential(stream, streamErrorReport, LINE_2);
    }

    @Test
    @RunWithCustomExecutor
    public void shouldRetrieveReportWhenCheckFileRulesErrorNegativeDuration() throws Exception {
        prepareVitamSession();
        FileInputStream stream = new FileInputStream(PropertiesUtils.findFile(FILE_RULES_KO_NEGATIVE_DURATION));
        FileInputStream streamErrorReport =
            new FileInputStream(PropertiesUtils.findFile(ERROR_REPORT_NEGATIVE_DURATION));
        checkFileRulesWithCustomReferential(stream, streamErrorReport, LINE_2);
    }

    @Test
    @RunWithCustomExecutor
    public void shouldRetrieveReportWhenCheckFileRulesErrorWrongComa() throws Exception {
        prepareVitamSession();
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
        prepareVitamSession();
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
            prepareVitamSession();

            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            // workspace client unzip SIP in workspace
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
            IngestInternalClientFactory.getInstance().changeServerPort(runner.PORT_SERVICE_INGEST_INTERNAL);
            final IngestInternalClient client2 = IngestInternalClientFactory.getInstance().getClient();
            client2.uploadInitialLogbook(params);

            // init workflow before execution
            client2.initWorkflow(ingestSip);

            client2.upload(zipInputStreamSipObject, CommonMediaType.ZIP_TYPE, ingestSip, ProcessAction.NEXT.name());

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
        prepareVitamSession();
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        // workspace client unzip SIP in workspace
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
        IngestInternalClientFactory.getInstance().changeServerPort(runner.PORT_SERVICE_INGEST_INTERNAL);
        final IngestInternalClient client = IngestInternalClientFactory.getInstance().getClient();
        client.uploadInitialLogbook(params);

        // init workflow before execution
        client.initWorkflow(ingestSip);

        client.upload(zipInputStreamSipObject, CommonMediaType.ZIP_TYPE, ingestSip, ProcessAction.RESUME.name());

        awaitForWorkflowTerminationWithStatus(operationGuid, StatusCode.OK);

        SelectMultiQuery select = new SelectMultiQuery();
        select.addQueries(QueryHelper.and().add(QueryHelper.match("Title", "monSIP"))
            .add(QueryHelper.in("#operations", operationGuid.toString())));
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
        assertEquals("Secret Défense",
            node.get("$results").get(0).get("#management").get("ClassificationRule").get("ClassificationLevel")
                .asText());
        assertEquals("ClassOWn",
            node.get("$results").get(0).get("#management").get("ClassificationRule").get("ClassificationOwner")
                .asText());

    }


    @RunWithCustomExecutor
    @Test
    public void testApplyAccessContractSecurityFilter()
        throws FileNotFoundException, InvalidParseOperationException, DatabaseException,
        InvalidCreateOperationException {

        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        prepareVitamSession();
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        // UniqueTitleParent : aeaqaaaaaahmtusqabktwaldc34sm5yaaaaq
        // UniqueTitleChild : aeaqaaaaaahmtusqabktwaldc34sm5iaaabq
        final List<Document> unitList =
            JsonHandler.getFromFileAsTypeRefence(PropertiesUtils
                    .getResourceFile("integration-ingest-internal/data/units_tree_access_contract_test.json"),
                new TypeReference<List<Unit>>() {
                });

        // Save units in Mongo
        VitamRepositoryFactory.get().getVitamMongoRepository(MetadataCollections.UNIT.getVitamCollection())
            .save(unitList);

        // Save units in Elasticsearch
        VitamRepositoryFactory.get().getVitamESRepository(MetadataCollections.UNIT.getVitamCollection())
            .save(unitList);


        AccessContractModel accessContractModel = new AccessContractModel();
        accessContractModel.setEveryOriginatingAgency(true);
        VitamThreadUtils.getVitamSession().setContract(accessContractModel);

        // Test With originating agencies restriction
        try {
            List<String> allowList = new ArrayList<>();
            allowList.add("Secret Défense");

            ClassificationLevel classificationLevel = new ClassificationLevel();
            classificationLevel.setAllowList(allowList);
            classificationLevel.setAuthorizeNotDefined(true);
            AccessInternalModuleImpl accessInternalModule = new AccessInternalModuleImpl();

            CompareQuery query_1 =
                QueryHelper.eq("Title", "UniqueTitleParent");

            CompareQuery query_2 =
                QueryHelper.eq("Title", "UniqueTitleChild");
            query_2.setDepthLimit(1);

            SelectMultiQuery selectMultiple = new SelectMultiQuery();
            selectMultiple.addQueries(query_1, query_2);


            JsonNode newJson =
                AccessContractRestrictionHelper
                    .applyAccessContractRestrictionForUnitForSelect(selectMultiple.getFinalSelect(),
                        accessContractModel);

            assertThat(newJson).isNotNull();

            JsonNode result = accessInternalModule.selectUnit(newJson);
            assertThat(result).isNotNull();
            RequestResponseOK<JsonNode> res =
                JsonHandler.getFromString(result.toString(), RequestResponseOK.class, JsonNode.class);
            // UniqueTitleChild found
            assertThat(res.getResults()).hasSize(1);
            assertThat(res.getResults().iterator().next().toString()).contains("UniqueTitleChild");

            // Add Originating Agency Identifier0 then recheck. Should not return result
            accessContractModel.setEveryOriginatingAgency(false);
            accessContractModel.getOriginatingAgencies().add("Identifier0");

            newJson =
                AccessContractRestrictionHelper
                    .applyAccessContractRestrictionForUnitForSelect(selectMultiple.getFinalSelect(),
                        accessContractModel);
            assertThat(newJson).isNotNull();
            assertThat(newJson.toString().split("Identifier0")).hasSize(3);

            result = accessInternalModule.selectUnit(newJson);
            assertThat(result).isNotNull();
            res = JsonHandler.getFromString(result.toString(), RequestResponseOK.class, JsonNode.class);
            assertThat(res.getResults()).hasSize(0);


            // Add Originating Agency Identifier1 then recheck. Should return one result
            accessContractModel.getOriginatingAgencies().add("Identifier1");
            newJson =
                AccessContractRestrictionHelper
                    .applyAccessContractRestrictionForUnitForSelect(selectMultiple.getFinalSelect(),
                        accessContractModel);
            assertThat(newJson).isNotNull();
            assertThat(newJson.toString().split("Identifier0")).hasSize(3);

            result = accessInternalModule.selectUnit(newJson);
            assertThat(result).isNotNull();
            res = JsonHandler.getFromString(result.toString(), RequestResponseOK.class, JsonNode.class);
            // UniqueTitleChild found
            assertThat(res.getResults()).hasSize(1);
            assertThat(res.getResults().iterator().next().toString()).contains("UniqueTitleChild");

            // Set rootUnit to UniqueTitleParent guid. Should return result
            accessContractModel.getRootUnits().add("aeaqaaaaaahmtusqabktwaldc34sm5yaaaaq");
            newJson =
                AccessContractRestrictionHelper
                    .applyAccessContractRestrictionForUnitForSelect(selectMultiple.getFinalSelect(),
                        accessContractModel);
            assertThat(newJson).isNotNull();
            assertThat(newJson.toString().split("aeaqaaaaaahmtusqabktwaldc34sm5yaaaaq")).hasSize(5);

            result = accessInternalModule.selectUnit(newJson);
            assertThat(result).isNotNull();
            res = JsonHandler.getFromString(result.toString(), RequestResponseOK.class, JsonNode.class);
            // UniqueTitleChild found
            assertThat(res.getResults()).hasSize(1);
            assertThat(res.getResults().iterator().next().toString()).contains("UniqueTitleChild");

            // Set rootUnit to UniqueTitleChild guid. The first query should not return result as root unit restrict
            // access
            // => So for the second query no roots => should not return result
            accessContractModel.getRootUnits().clear();
            accessContractModel.getRootUnits().add("aeaqaaaaaahmtusqabktwaldc34sm5iaaabq");
            newJson =
                AccessContractRestrictionHelper
                    .applyAccessContractRestrictionForUnitForSelect(selectMultiple.getFinalSelect(),
                        accessContractModel);
            assertThat(newJson).isNotNull();
            assertThat(newJson.toString().split("aeaqaaaaaahmtusqabktwaldc34sm5iaaabq")).hasSize(5);

            result = accessInternalModule.selectUnit(newJson);
            assertThat(result).isNotNull();
            res = JsonHandler.getFromString(result.toString(), RequestResponseOK.class, JsonNode.class);
            assertThat(res.getResults()).hasSize(0);


            //////////////////////////
            /// Test Depth negative
            /////////////////////////
            query_1 =
                QueryHelper.eq("Title", "UniqueTitleChild");

            query_2 =
                QueryHelper.eq("Title", "UniqueTitleChild");
            query_2.setDepthLimit(-1);

            selectMultiple = new SelectMultiQuery();
            selectMultiple.addQueries(query_1, query_2);

            // Restrict to UniqueTitleChild
            accessContractModel.getRootUnits().clear();
            accessContractModel.getRootUnits().add("aeaqaaaaaahmtusqabktwaldc34sm5iaaabq");


            // Get UniqueTitleChild then Get parent UniqueTitleParent even AccessContract restrict access only
            // UniqueTitleChild
            // => should not return result
            newJson =
                AccessContractRestrictionHelper
                    .applyAccessContractRestrictionForUnitForSelect(selectMultiple.getFinalSelect(),
                        accessContractModel);
            assertThat(newJson).isNotNull();

            result = accessInternalModule.selectUnit(newJson);
            assertThat(result).isNotNull();
            res = JsonHandler.getFromString(result.toString(), RequestResponseOK.class, JsonNode.class);
            assertThat(res.getResults()).hasSize(0);


        } catch (Exception e) {
            LOGGER.error(e);
            fail("should not throw exception");
        }

    }

    @RunWithCustomExecutor
    @Test
    public void testIngestSipWithComplexRules() throws Exception {
        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        prepareVitamSession();
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        // workspace client unzip SIP in workspace
        final InputStream zipInputStreamSipObject =
            PropertiesUtils.getResourceAsStream(OK_RULES_COMPLEX_COMPLETE_SIP);

        // init default logbook operation
        final List<LogbookOperationParameters> params = new ArrayList<>();
        final LogbookOperationParameters initParameters = LogbookParametersFactory.newLogbookOperationParameters(
            operationGuid, "Process_SIP_unitary", operationGuid,
            LogbookTypeProcess.INGEST, StatusCode.STARTED,
            operationGuid != null ? operationGuid.toString() : "outcomeDetailMessage",
            operationGuid);
        params.add(initParameters);

        // call ingest
        IngestInternalClientFactory.getInstance().changeServerPort(runner.PORT_SERVICE_INGEST_INTERNAL);
        final IngestInternalClient client = IngestInternalClientFactory.getInstance().getClient();
        client.uploadInitialLogbook(params);

        // init workflow before execution
        client.initWorkflow(ingestSip);

        client.upload(zipInputStreamSipObject, CommonMediaType.ZIP_TYPE, ingestSip, ProcessAction.RESUME.name());

        awaitForWorkflowTerminationWithStatus(operationGuid, StatusCode.OK);

        // Select single unit with inherited rules

        SelectMultiQuery select1 = new SelectMultiQuery();
        select1.addQueries(QueryHelper.and().add(QueryHelper.match("Title", "Buttes-Chaumont"))
            .add(QueryHelper.eq(VitamFieldsHelper.initialOperation(), operationGuid.toString())));
        // Get AU
        final AccessInternalClient accessInternalClient = AccessInternalClientFactory.getInstance().getClient();
        final RequestResponseOK<JsonNode> results1 =
            (RequestResponseOK<JsonNode>) accessInternalClient.selectUnitsWithInheritedRules(select1.getFinalSelect());

        assertThat(results1.getResults()).hasSize(1);
        JsonNode unitButtesChaumont1 = results1.getFirstResult();

        validateButtesChaumontInheritedRules(unitButtesChaumont1);

        // Select multiple units with inherited rules

        SelectMultiQuery select2 = new SelectMultiQuery();
        select2.addQueries(QueryHelper.eq(VitamFieldsHelper.initialOperation(), operationGuid.toString()));
        // Get AU

        final RequestResponseOK<JsonNode> results2 =
            (RequestResponseOK<JsonNode>) accessInternalClient.selectUnitsWithInheritedRules(select2.getFinalSelect());

        assertThat(results2.getResults()).hasSize(28);

        JsonNode unitButtesChaumont2 = null;
        for (JsonNode jsonNode : results2.getResults()) {
            if (jsonNode.get("Title").asText().equals("Buttes-Chaumont")) {
                unitButtesChaumont2 = jsonNode;
            }
        }

        validateButtesChaumontInheritedRules(unitButtesChaumont2);

        // Select units with inherited rules with access contract


        VitamThreadUtils.getVitamSession().setContractId("aName4");
        final RequestResponseOK<JsonNode> results3 =
            (RequestResponseOK<JsonNode>) accessInternalClient.selectUnitsWithInheritedRules(select2.getFinalSelect());

        assertThat(results3.getResults()).hasSize(0);
    }

    private void validateButtesChaumontInheritedRules(JsonNode unitButtesChaumont)
        throws InvalidParseOperationException {

        assertThat(unitButtesChaumont.get("Title").asText()).isEqualTo("Buttes-Chaumont");
        UnitInheritedRulesResponseModel unitInheritedRules =
            JsonHandler.getFromJsonNode(unitButtesChaumont.get("InheritedRules"),
                UnitInheritedRulesResponseModel.class);

        InheritedRuleCategoryResponseModel storageRuleCategory =
            unitInheritedRules.getRuleCategories().get(VitamConstants.TAG_RULE_STORAGE);
        assertThat(storageRuleCategory.getProperties()).hasSize(0);
        assertThat(storageRuleCategory.getRules()).hasSize(0);

        InheritedRuleCategoryResponseModel appraisalRuleCategory =
            unitInheritedRules.getRuleCategories().get(VitamConstants.TAG_RULE_APPRAISAL);
        assertThat(appraisalRuleCategory.getProperties()).hasSize(1);
        assertThat(appraisalRuleCategory.getProperties().get(0).getPropertyName()).isEqualTo("FinalAction");
        assertThat(appraisalRuleCategory.getProperties().get(0).getPropertyValue()).isEqualTo("Keep");
        assertThat(appraisalRuleCategory.getProperties().get(0).getPaths()).hasSize(2);
        assertThat(appraisalRuleCategory.getProperties().get(0).getPaths().get(0)).hasSize(4);
        assertThat(appraisalRuleCategory.getProperties().get(0).getPaths().get(1)).hasSize(4);

        assertThat(appraisalRuleCategory.getRules()).hasSize(0);

        InheritedRuleCategoryResponseModel reuseRuleCategory =
            unitInheritedRules.getRuleCategories().get(VitamConstants.TAG_RULE_REUSE);
        assertThat(reuseRuleCategory.getProperties()).hasSize(0);
        assertThat(reuseRuleCategory.getRules()).hasSize(0);

        InheritedRuleCategoryResponseModel classificationRuleCategory =
            unitInheritedRules.getRuleCategories().get(VitamConstants.TAG_RULE_CLASSIFICATION);
        assertThat(classificationRuleCategory.getProperties()).hasSize(0);
        assertThat(classificationRuleCategory.getRules()).hasSize(0);


        InheritedRuleCategoryResponseModel disseminationRuleCategory =
            unitInheritedRules.getRuleCategories().get(VitamConstants.TAG_RULE_DISSEMINATION);
        assertThat(disseminationRuleCategory.getProperties()).hasSize(0);
        assertThat(disseminationRuleCategory.getRules()).hasSize(1);
        assertThat(disseminationRuleCategory.getRules().get(0).getPaths()).hasSize(2);
        assertThat(disseminationRuleCategory.getRules().get(0).getRuleId()).isEqualTo("DIS-00001");
        assertThat(disseminationRuleCategory.getRules().get(0).getStartDate()).isEqualTo("2000-01-01");
        assertThat(disseminationRuleCategory.getRules().get(0).getEndDate()).isEqualTo("2025-01-01");

        InheritedRuleCategoryResponseModel accessRuleCategory =
            unitInheritedRules.getRuleCategories().get(VitamConstants.TAG_RULE_ACCESS);
        assertThat(accessRuleCategory.getProperties()).hasSize(0);
        assertThat(accessRuleCategory.getRules()).hasSize(3);
    }

    @RunWithCustomExecutor
    @Test
    public void testEliminationAnalysisOverSipWithComplexRules() throws Exception {
        final GUID ingestOperationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        prepareVitamSession();
        VitamThreadUtils.getVitamSession().setRequestId(ingestOperationGuid);
        // workspace client unzip SIP in workspace
        final InputStream zipInputStreamSipObject =
            PropertiesUtils.getResourceAsStream(OK_RULES_COMPLEX_COMPLETE_SIP);

        // init default logbook operation
        final List<LogbookOperationParameters> params = new ArrayList<>();
        final LogbookOperationParameters initParameters = LogbookParametersFactory.newLogbookOperationParameters(
            ingestOperationGuid, "Process_SIP_unitary", ingestOperationGuid,
            LogbookTypeProcess.INGEST, StatusCode.STARTED,
            ingestOperationGuid != null ? ingestOperationGuid.toString() : "outcomeDetailMessage",
            ingestOperationGuid);
        params.add(initParameters);

        // call ingest
        IngestInternalClientFactory.getInstance().changeServerPort(runner.PORT_SERVICE_INGEST_INTERNAL);
        final IngestInternalClient client = IngestInternalClientFactory.getInstance().getClient();
        client.uploadInitialLogbook(params);

        // init workflow before execution
        client.initWorkflow(ingestSip);

        client.upload(zipInputStreamSipObject, CommonMediaType.ZIP_TYPE, ingestSip, ProcessAction.RESUME.name());

        awaitForWorkflowTerminationWithStatus(ingestOperationGuid, StatusCode.OK);

        // elimination analysis

        final GUID eliminationAnalysisOperationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        VitamThreadUtils.getVitamSession().setRequestId(eliminationAnalysisOperationGuid);

        SelectMultiQuery analysisDslRequest = new SelectMultiQuery();
        analysisDslRequest
            .addQueries(QueryHelper.eq(VitamFieldsHelper.initialOperation(), ingestOperationGuid.toString()));

        EliminationRequestBody eliminationRequestBody = new EliminationRequestBody(
            "2018-01-01", analysisDslRequest.getFinalSelect());

        final AccessInternalClient accessInternalClient = AccessInternalClientFactory.getInstance().getClient();
        final RequestResponse<JsonNode> analysisResult =
            accessInternalClient.startEliminationAnalysis(eliminationRequestBody);

        assertThat(analysisResult.isOk()).isTrue();

        awaitForWorkflowTerminationWithStatus(eliminationAnalysisOperationGuid, StatusCode.OK);

        // Check indexation querying
        SelectMultiQuery checkAnalysisDslRequest = new SelectMultiQuery();
        checkAnalysisDslRequest.addQueries(
            QueryHelper.and().add(QueryHelper.eq(VitamFieldsHelper.initialOperation(), ingestOperationGuid.toString()))
                .add(QueryHelper
                    .eq(VitamFieldsHelper.elimination() + ".OperationId", eliminationAnalysisOperationGuid.toString()))
                .add(QueryHelper.eq(VitamFieldsHelper.elimination() + ".GlobalStatus", "DESTROY")));

        checkAnalysisDslRequest.addUsedProjection(
            "Title", VitamFieldsHelper.elimination(), VitamFieldsHelper.version());

        final RequestResponseOK<JsonNode> indexedUnitsResponse =
            (RequestResponseOK<JsonNode>) accessInternalClient.selectUnits(checkAnalysisDslRequest.getFinalSelect());

        assertThat(indexedUnitsResponse.isOk()).isTrue();

        List<String> indexedUnitTitles = indexedUnitsResponse.getResults()
            .stream()
            .map(node -> node.get("Title").asText())
            .collect(Collectors.toList());

        assertThat(indexedUnitTitles)
            .containsExactlyInAnyOrder("Porte de Pantin", "Eglise de Pantin", "Stalingrad.txt");

        // Check access to #elimination field
        assertThat(indexedUnitsResponse.getFirstResult()
            .get(VitamFieldsHelper.elimination())
            .get(0)
            .get("OperationId").asText()).isEqualTo(eliminationAnalysisOperationGuid.toString());

        // Ensure document version has not been updated during indexation process
        assertThat(indexedUnitsResponse.getFirstResult().get(VitamFieldsHelper.version()).asInt())
            .isEqualTo(0);
    }

    @RunWithCustomExecutor
    @Test
    public void testLFCAccessForUnitAndGot() throws Exception {
        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        try {
            prepareVitamSession();
            VitamThreadUtils.getVitamSession().setContractId("aName4");
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
            // workspace client unzip SIP in workspace
            final InputStream zipInputStreamSipObject =
                PropertiesUtils.getResourceAsStream(SIP_OK_PHYSICAL_ARCHIVE_FOR_LFC);

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
            IngestInternalClientFactory.getInstance().changeServerPort(runner.PORT_SERVICE_INGEST_INTERNAL);
            final IngestInternalClient client = IngestInternalClientFactory.getInstance().getClient();
            client.uploadInitialLogbook(params);

            // init workflow before execution
            client.initWorkflow(ingestSip);

            client.upload(zipInputStreamSipObject, CommonMediaType.ZIP_TYPE, ingestSip, ProcessAction.RESUME.name());

            awaitForWorkflowTerminationWithStatus(operationGuid, StatusCode.WARNING);

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
            // select.setProjectionSliceOnQualifier();
            final JsonNode jsonResponse = metadataClient.selectObjectGrouptbyId(select.getFinalSelect(), og);
            LOGGER.warn("Result: " + jsonResponse);
            final List<String> valuesAsText = jsonResponse.get("$results").findValuesAsText("#id");
            final String objectId = valuesAsText.get(0);
            LOGGER.warn("read: " + objectId);

            final AccessInternalClient accessClient = AccessInternalClientFactory.getInstance().getClient();
            JsonNode lfcUnit = retrieveLfcForUnit(unit.get("#id").asText(), accessClient);
            assertNotNull(lfcUnit);

            JsonNode lfcGot = retrieveLfcForGot(objectId, accessClient);
            assertNotNull(lfcGot);

            VitamThreadUtils.getVitamSession().setContractId("aName3");

            try {
                retrieveLfcForUnit(unit.get("#id").asText(), accessClient);
            } catch (AccessUnauthorizedException e) {
                assertTrue(e.getMessage().equals("Access by Contract Exception"));
            }

            try {
                retrieveLfcForGot(objectId, accessClient);
            } catch (AccessUnauthorizedException e) {
                assertTrue(e.getMessage().equals("Access by Contract Exception"));
            }

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

    private void awaitForWorkflowTerminationWithStatus(GUID operationGuid, StatusCode status) {

        waitOperation(NB_TRY, SLEEP_TIME, operationGuid.toString());

        ProcessWorkflow processWorkflow =
            ProcessMonitoringImpl.getInstance().findOneProcessWorkflow(operationGuid.toString(), tenantId);

        assertNotNull(processWorkflow);
        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
        assertEquals(status, processWorkflow.getStatus());
    }



    @RunWithCustomExecutor
    @Test
    public void testExternalLogbook() throws Exception {
        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        final GUID operationGuidForRequestId = GUIDFactory.newOperationLogbookGUID(tenantId);
        prepareVitamSession();
        VitamThreadUtils.getVitamSession().setRequestId(operationGuidForRequestId);

        // external logbook creation

        // root logbook
        final LogbookOperationParameters logbookOperationparams =
            LogbookParametersFactory.newLogbookOperationParameters(
                operationGuid, "EXT_External_Operation", operationGuid,
                LogbookTypeProcess.EXTERNAL_LOGBOOK, StatusCode.STARTED,
                operationGuid != null ? operationGuid.toString() : "outcomeDetailMessage",
                operationGuid);
        logbookOperationparams.putParameterValue(LogbookParameterName.agentIdentifierApplicationSession,
            operationGuid.getId());
        logbookOperationparams.putParameterValue(LogbookParameterName.agIdExt,
            JsonHandler.unprettyPrint(JsonHandler.createObjectNode().put("extId", operationGuid.getId())));
        logbookOperationparams.putParameterValue(LogbookParameterName.objectIdentifierIncome, operationGuid.getId());

        final GUID eventGuid = GUIDFactory.newEventGUID(operationGuid);
        final LogbookOperationParameters eventParameters = LogbookParametersFactory
            .newLogbookOperationParameters(
                eventGuid,
                "EXT_External_Operation",
                operationGuid,
                LogbookTypeProcess.EXTERNAL_LOGBOOK,
                StatusCode.OK,
                "outcomeDetailMessage",
                operationGuid);
        Set<LogbookParameters> events = new LinkedHashSet<>();
        events.add(eventParameters);
        logbookOperationparams.setEvents(events);

        final LogbookOperationParameters logbookOperationparamsWrongType =
            LogbookParametersFactory.newLogbookOperationParameters(
                operationGuid, "External_Operation", operationGuid,
                LogbookTypeProcess.EXTERNAL_LOGBOOK, StatusCode.STARTED,
                operationGuid != null ? operationGuid.toString() : "outcomeDetailMessage",
                operationGuid);
        final LogbookOperationParameters logbookOperationparamsWrongTypeProc =
            LogbookParametersFactory.newLogbookOperationParameters(
                operationGuid, "EXT_External_Operation", operationGuid,
                LogbookTypeProcess.AUDIT, StatusCode.STARTED,
                operationGuid != null ? operationGuid.toString() : "outcomeDetailMessage",
                operationGuid);

        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient();
            AccessInternalClient accessClient = AccessInternalClientFactory.getInstance().getClient()) {

            assertEquals(Status.CREATED, client.createExternalOperation(logbookOperationparams));

            JsonNode logbookOperation =
                accessClient
                    .selectOperationById(operationGuidForRequestId.getId(), new SelectMultiQuery().getFinalSelect())
                    .toJsonNode();
            // assert certain parameters in the master are overloaded
            assertEquals(StatusCode.OK.name(), logbookOperation.get("$results").get(0)
                .get(LogbookMongoDbName.getLogbookMongoDbName(LogbookParameterName.outcome).getDbname()).asText());

            // assert certain parameters in the master are not overloaded
            assertEquals(operationGuid.getId(),
                logbookOperation.get("$results").get(0)
                    .get(LogbookMongoDbName
                        .getLogbookMongoDbName(LogbookParameterName.agentIdentifierApplicationSession).getDbname())
                    .asText());
            assertThat(logbookOperation.get("$results").get(0)
                .get(LogbookMongoDbName.getLogbookMongoDbName(LogbookParameterName.agIdExt).getDbname()).asText()
                .contains(operationGuid.getId()));
            assertEquals(operationGuid.getId(),
                logbookOperation.get("$results").get(0).get(
                    LogbookMongoDbName.getLogbookMongoDbName(LogbookParameterName.objectIdentifierIncome).getDbname())
                    .asText());
            assertEquals(1, logbookOperation.get("$results").get(0).get("events").size());
            logbookOperation.get("$results").get(0).get("events").forEach(event -> {
                if (event.get("evType").asText().contains("STP_UPLOAD_SIP")) {
                    assertThat(event.get(LogbookParameterName.eventTypeProcess.name()).asText())
                        .contains(LogbookTypeProcess.EXTERNAL_LOGBOOK.name());
                }
            });


            try {
                client.createExternalOperation(logbookOperationparamsWrongTypeProc);
                fail("this should throw an exception as the audit shouldn't be accepted as a type proc ");
            } catch (BadRequestException e) {
                // do nothing as the external logbook is rejected -> wrong type
            }
            try {
                client.createExternalOperation(logbookOperationparamsWrongType);
                fail("this should throw an exception as the External_Operation shouldn't be accepted as eventType");
            } catch (BadRequestException e) {
                // do nothing as the external logbook is rejected -> wrong event type
            }

        }



    }

    @RunWithCustomExecutor
    @Test
    public void testIngestAndFillLogbookObIdInWithManifestMessageIdentifier() throws Exception {
        prepareVitamSession();
        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        // workspace client unzip SIP in workspace
        final InputStream zipInputStreamSipObject =
            PropertiesUtils.getResourceAsStream(OK_OBIDIN_MESSAGE_IDENTIFIER);

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
        IngestInternalClientFactory.getInstance().changeServerPort(runner.PORT_SERVICE_INGEST_INTERNAL);
        final IngestInternalClient client = IngestInternalClientFactory.getInstance().getClient();
        client.uploadInitialLogbook(params);
        // init workflow before execution
        client.initWorkflow(ingestSip);
        client.upload(zipInputStreamSipObject, CommonMediaType.ZIP_TYPE, ingestSip, ProcessAction.RESUME.name());

        awaitForWorkflowTerminationWithStatus(operationGuid, StatusCode.OK);
        final AccessInternalClient accessClient = AccessInternalClientFactory.getInstance().getClient();
        JsonNode logbookOperation =
            accessClient.selectOperationById(operationGuid.getId(), new SelectMultiQuery().getFinalSelect())
                .toJsonNode();
        boolean checkServiceLevel = false;
        final JsonNode element = logbookOperation.get("$results").get(0);

        assertEquals(element.get("obIdIn").asText(), "vitam");
    }

    @RunWithCustomExecutor
    @Test
    public void testIngestWithManifestIncorrectAlgorithm() throws Exception {
        prepareVitamSession();
        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);

        final GUID objectGuid = GUIDFactory.newManifestGUID(0);
        // workspace client unzip SIP in workspace
        final InputStream zipInputStreamSipObject =
            PropertiesUtils.getResourceAsStream(SIP_ALGO_INCORRECT_IN_MANIFEST);

        final List<LogbookOperationParameters> params = new ArrayList<>();
        final LogbookOperationParameters initParameters = LogbookParametersFactory.newLogbookOperationParameters(
            operationGuid, "STP_INGEST_CONTROL_SIP", operationGuid,
            LogbookTypeProcess.INGEST, StatusCode.STARTED,
            operationGuid != null ? operationGuid.toString() : "outcomeDetailMessage",
            operationGuid);
        params.add(initParameters);
        LOGGER.error(initParameters.toString());

        // call ingest
        IngestInternalClientFactory.getInstance().changeServerPort(runner.PORT_SERVICE_INGEST_INTERNAL);
        final IngestInternalClient client = IngestInternalClientFactory.getInstance().getClient();
        client.uploadInitialLogbook(params);

        // init workflow before execution
        client.initWorkflow(ingestSip);

        client.upload(zipInputStreamSipObject, CommonMediaType.ZIP_TYPE, ingestSip, ProcessAction.RESUME.name());

        awaitForWorkflowTerminationWithStatus(operationGuid, StatusCode.KO);

        final AccessInternalClient accessClient = AccessInternalClientFactory.getInstance().getClient();
        JsonNode logbookOperation =
            accessClient.selectOperationById(operationGuid.getId(), new SelectMultiQuery().getFinalSelect())
                .toJsonNode();


        logbookOperation.get("$results").get(0).get("events").forEach(event -> {
            if (event.get("evType").asText().contains("CHECK_DATAOBJECTPACKAGE.CHECK_MANIFEST_DATAOBJECT_VERSION")) {
                assertThat(event.get("outDetail").textValue()).
                    contains("CHECK_DATAOBJECTPACKAGE.CHECK_MANIFEST_DATAOBJECT_VERSION.INVALIDE_ALGO.KO");
            }
        });



    }

    @RunWithCustomExecutor
    @Test
    public void testIngestWithCustodialHistoryFileContainingDataObjectReferenceId() throws Exception {
        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        prepareVitamSession();
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);

        // workspace client unzip SIP in workspace
        final InputStream zipInputStreamSipObject =
            PropertiesUtils.getResourceAsStream(SIP_SIP_ALL_METADATA_WITH_CUSTODIALHISTORYFILE);

        // init default logbook operation
        final List<LogbookOperationParameters> params = new ArrayList<>();
        final LogbookOperationParameters initParameters = LogbookParametersFactory.newLogbookOperationParameters(
            operationGuid, "Process_SIP_unitary", operationGuid,
            LogbookTypeProcess.INGEST, StatusCode.STARTED,
            operationGuid != null ? operationGuid.toString() : "outcomeDetailMessage",
            operationGuid);
        params.add(initParameters);

        // call ingest
        IngestInternalClientFactory.getInstance().changeServerPort(runner.PORT_SERVICE_INGEST_INTERNAL);
        final IngestInternalClient client = IngestInternalClientFactory.getInstance().getClient();
        client.uploadInitialLogbook(params);

        // init workflow before execution
        client.initWorkflow(ingestSip);

        client.upload(zipInputStreamSipObject, CommonMediaType.ZIP_TYPE, ingestSip, ProcessAction.RESUME.name());

        awaitForWorkflowTerminationWithStatus(operationGuid, StatusCode.WARNING);

        // Try to check AU and custodialHistoryModel
        final MetaDataClient metadataClient = MetaDataClientFactory.getInstance().getClient();
        SelectMultiQuery select = new SelectMultiQuery();
        select.addQueries(
            QueryHelper.eq("Title", "Les ruines de la Grande Guerre. - Belleau. - Une tranchée près de la gare."));
        final JsonNode node = metadataClient.selectUnits(select.getFinalSelect());

        final JsonNode result = node.get("$results");
        assertNotNull(result);

        CustodialHistoryModel model =
            JsonHandler.getFromJsonNode(result.get(0).get("CustodialHistory"), CustodialHistoryModel.class);
        assertNotNull(model);
        String expectedTitleOfCustodialItem = "Ce champ est obligatoire";
        assertThat(model.getCustodialHistoryItem()).isEqualTo(Arrays.asList(expectedTitleOfCustodialItem));

        DataObjectReference reference = model.getCustodialHistoryFile();
        assertNotNull(reference);
        String expectedDataObjectReference = "ID22";
        assertThat(reference.getDataObjectReferenceId()).isEqualTo(expectedDataObjectReference);
        assertNull(reference.getDataObjectGroupReferenceId());

    }

}
