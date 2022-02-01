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
package fr.gouv.vitam.ingest.internal.integration.test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import fr.gouv.culture.archivesdefrance.seda.v2.RelatedObjectReferenceType;
import fr.gouv.vitam.access.internal.client.AccessInternalClient;
import fr.gouv.vitam.access.internal.client.AccessInternalClientFactory;
import fr.gouv.vitam.access.internal.common.exception.AccessInternalClientNotFoundException;
import fr.gouv.vitam.access.internal.common.exception.AccessInternalClientServerException;
import fr.gouv.vitam.access.internal.core.AccessInternalModuleImpl;
import fr.gouv.vitam.access.internal.rest.AccessInternalMain;
import fr.gouv.vitam.common.DataLoader;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.VitamTestHelper;
import fr.gouv.vitam.common.accesslog.AccessLogUtils;
import fr.gouv.vitam.common.client.IngestCollection;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.client.VitamClientFactoryInterface;
import fr.gouv.vitam.common.configuration.ClassificationLevel;
import fr.gouv.vitam.common.database.api.VitamRepositoryFactory;
import fr.gouv.vitam.common.database.builder.query.CompareQuery;
import fr.gouv.vitam.common.database.builder.query.Query;
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
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.guid.GUIDReader;
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
import fr.gouv.vitam.common.model.administration.ProfileModel;
import fr.gouv.vitam.common.model.administration.RuleType;
import fr.gouv.vitam.common.model.elimination.EliminationRequestBody;
import fr.gouv.vitam.common.model.rules.InheritedRuleCategoryResponseModel;
import fr.gouv.vitam.common.model.rules.InheritedRuleResponseModel;
import fr.gouv.vitam.common.model.rules.UnitInheritedRulesResponseModel;
import fr.gouv.vitam.common.model.unit.CustodialHistoryModel;
import fr.gouv.vitam.common.model.unit.DataObjectReference;
import fr.gouv.vitam.common.model.unit.EventTypeModel;
import fr.gouv.vitam.common.model.unit.RuleModel;
import fr.gouv.vitam.common.stream.SizedInputStream;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClient;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClientFactory;
import fr.gouv.vitam.ingest.internal.common.exception.IngestInternalClientServerException;
import fr.gouv.vitam.ingest.internal.upload.rest.IngestInternalMain;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookDocument;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookElasticsearchAccess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbName;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookOperation;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.core.database.collections.ObjectGroup;
import fr.gouv.vitam.metadata.rest.MetadataMain;
import fr.gouv.vitam.processing.common.model.ProcessWorkflow;
import fr.gouv.vitam.processing.engine.core.monitoring.ProcessMonitoringImpl;
import fr.gouv.vitam.processing.management.rest.ProcessManagementMain;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.worker.core.plugin.elimination.model.EliminationAnalysisResult;
import fr.gouv.vitam.worker.core.plugin.elimination.model.EliminationExtendedInfoHoldRule;
import fr.gouv.vitam.worker.core.plugin.elimination.model.EliminationGlobalStatus;
import fr.gouv.vitam.worker.server.rest.WorkerMain;
import fr.gouv.vitam.workspace.api.exception.ZipFilesNameNotAllowedException;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import io.restassured.RestAssured;
import org.apache.commons.io.FileUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
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
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static fr.gouv.vitam.common.VitamServerRunner.PORT_SERVICE_LOGBOOK;
import static fr.gouv.vitam.common.VitamTestHelper.doIngest;
import static fr.gouv.vitam.common.VitamTestHelper.findLogbook;
import static fr.gouv.vitam.common.VitamTestHelper.prepareVitamSession;
import static fr.gouv.vitam.common.VitamTestHelper.verifyOperation;
import static fr.gouv.vitam.common.VitamTestHelper.verifyProcessState;
import static fr.gouv.vitam.common.VitamTestHelper.waitOperation;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static fr.gouv.vitam.common.guid.GUIDFactory.newOperationLogbookGUID;
import static fr.gouv.vitam.common.model.IngestWorkflowConstants.WORKFLOW_IDENTIFIER;
import static fr.gouv.vitam.common.model.ProcessAction.RESUME;
import static fr.gouv.vitam.common.model.ProcessState.COMPLETED;
import static fr.gouv.vitam.common.model.ProcessState.PAUSE;
import static fr.gouv.vitam.common.model.StatusCode.FATAL;
import static fr.gouv.vitam.common.model.StatusCode.KO;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.common.model.StatusCode.STARTED;
import static fr.gouv.vitam.common.model.StatusCode.WARNING;
import static fr.gouv.vitam.logbook.common.parameters.Contexts.HOLDING_SCHEME;
import static io.restassured.RestAssured.get;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Ingest Internal integration test
 */
public class IngestInternalIT extends VitamRuleRunner {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IngestInternalIT.class);
    private static final String LINE_3 = "line 3";
    private static final String LINE_2 = "line 2";
    private static final String TITLE = "Title";
    private static final String TITLE_FR = "Title_.fr";
    private static final String JEU_DONNEES_OK_REGLES_CSV_CSV = "jeu_donnees_OK_regles_CSV.csv";
    private static final Integer tenantId = 0;
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
    private static final String FILE_RULES_KO_WRONG_RULETYPE =
        "functional-admin/file-rules/jeu_donnees_KO_regles_CSV_WrongRuleType.csv";
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
    private static final TypeReference<List<EventTypeModel>> LIST_TYPE_REFERENCE =
        new TypeReference<>() {
        };
    public static final String RESULTS = "$results";

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
    private static String SIP_TREE = "integration-ingest-internal/test_arbre.zip";
    private static String SIP_TREE_WITHOUT_INGEST_CONTRACT = "integration-ingest-internal/SIP_arbre_without_ingest_contract.zip";
    private static String SIP_FILE_OK_NAME = "integration-ingest-internal/SIP-ingest-internal-ok.zip";
    private static String SIP_FILE_KO_FORMAT = "integration-ingest-internal/SIP_mauvais_format.pdf";
    private static String SIP_CONTENT_KO_FORMAT = "integration-ingest-internal/SIP-ingest-internal-Content-KO.zip";
    private static String SIP_WITH_LOGBOOK = "integration-ingest-internal/sip_with_logbook.zip";
    private static String SIP_WITH_MALFORMED_LOGBOOK = "integration-ingest-internal/sip_with_malformed_logbook.zip";
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
    private static String OK_RULES_COMPLEX_COMPLETE_V2_SIP =
        "integration-ingest-internal/1069_OK_RULES_COMPLEXE_COMPLETE_V2.zip";
    private static String OK_OBIDIN_MESSAGE_IDENTIFIER =
        "integration-ingest-internal/SIP-ingest-internal-ok.zip";
    private static String SIP_ALGO_INCORRECT_IN_MANIFEST = "integration-ingest-internal/SIP_INCORRECT_ALGORITHM.zip";
    private static String SIP_WITH_ORGANIZATION_METADATA_DESCRIPTION_FREE_TAGS =
        "integration-ingest-internal/sip_with_organization_descriptive_metadata_free_tags.zip";
    private static String SIP_ATTACHMENT_WITH_OBJECT =
        "integration-ingest-internal/OK_OBJECT.zip";
    private static String SIP_WITH_ARCHIVE_PROFILE =
        "integration-ingest-internal/OK_SIPwithProfilRNG.zip";

    private static LogbookElasticsearchAccess esClient;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        handleBeforeClass(Arrays.asList(0, 1), Collections.emptyMap());
        // ES client
        List<ElasticsearchNode> esNodes =
            Lists.newArrayList(new ElasticsearchNode(ElasticsearchRule.getHost(), ElasticsearchRule.getPort()));
        esClient = new LogbookElasticsearchAccess(ElasticsearchRule.getClusterName(), esNodes, logbookIndexManager);

        StorageClientFactory storageClientFactory = StorageClientFactory.getInstance();
        storageClientFactory.setVitamClientType(VitamClientFactoryInterface.VitamClientType.MOCK);
        new DataLoader("integration-ingest-internal").prepareData();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        handleAfterClass();
        StorageClientFactory storageClientFactory = StorageClientFactory.getInstance();
        storageClientFactory.setVitamClientType(VitamClientFactoryInterface.VitamClientType.PRODUCTION);
        runAfter();
        VitamClientFactory.resetConnections();
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

    @Test
    @RunWithCustomExecutor
    public void should_finish_in_COMPLETED_FATAL_when_ATR_step_fails() throws Exception {
        // Given
        prepareVitamSession(tenantId, "OUR_FAILING_CONTRACT", "Context_IT");

        String operationId = VitamTestHelper.doIngest(tenantId, SIP_FILE_OK_NAME);
        verifyOperation(operationId, FATAL);

        VitamThreadUtils.getVitamSession().setContractId("SHHHH_ITS_ALL_FINE_CONTRACT_UNICORN");
        IngestInternalClient client = IngestInternalClientFactory.getInstance().getClient();
        client.updateOperationActionProcess(ProcessAction.RESUME.getValue(), operationId);

        waitOperation(operationId);
        verifyOperation(operationId, OK);

        // Check Logbook Closure Event
        fr.gouv.vitam.common.model.logbook.LogbookOperation ingestLogbook =
            JsonHandler.getFromJsonNode(findLogbook(operationId).get(RESULTS),
                fr.gouv.vitam.common.model.logbook.LogbookOperation.class);
        assertNotNull(ingestLogbook);
        assertFalse(ingestLogbook.getEvents().isEmpty());
        assertEquals(WORKFLOW_IDENTIFIER + "." + OK,
            ingestLogbook.getEvents().get(ingestLogbook.getEvents().size() - 1).getOutDetail());
    }

    @RunWithCustomExecutor
    @Test
    public void testIngestInternalUploadSipWithBadFormatThenFATAL() {
        prepareVitamSession(tenantId, "aName3", "Context_IT");
        assertThrows(IngestInternalClientServerException.class, () -> {
            String operationId = VitamTestHelper.doIngest(tenantId, SIP_FILE_KO_FORMAT);
            verifyOperation(operationId, FATAL);
        });

    }

    @RunWithCustomExecutor
    @Test
    public void testIngestInternalUploadSipWithBadContentFormatThenKO() {

        prepareVitamSession(tenantId, "aName3", "Context_IT");
        assertThrows(ZipFilesNameNotAllowedException.class, () -> {
            String operationId = VitamTestHelper.doIngest(tenantId, SIP_CONTENT_KO_FORMAT);
            verifyOperation(operationId, KO);
        });
    }

    @RunWithCustomExecutor
    @Test
    public void testIngestInternal() throws Exception {
        GUID operationGuid = null;
        try {
            prepareVitamSession(tenantId, "aName3", "Context_IT");
            String operationId = VitamTestHelper.doIngest(tenantId, SIP_FILE_OK_NAME);
            operationGuid = GUIDReader.getGUID(operationId);
            verifyOperation(operationId, OK);
            // Try to check AU
            final MetaDataClient metadataClient = MetaDataClientFactory.getInstance().getClient();
            final JsonNode node = getArchiveUnitWithTitle(metadataClient, "Sensibilisation API", TITLE);
            SelectMultiQuery select;
            LOGGER.debug(JsonHandler.prettyPrint(node));
            final JsonNode result = node.get(RESULTS);
            assertNotNull(result);
            final JsonNode unit = result.get(0);
            assertNotNull(unit);
            final String og = unit.get("#object").asText();
            assertThat(unit.get("#management").get("NeedAuthorization").asBoolean()).isFalse();
            assertThat(unit.get("#storage").get("strategyId").asText())
                .isEqualTo(VitamConfiguration.getDefaultStrategy());
            assertThat(unit.get("#storage").has("offerIds")).isFalse();
            // Try to check OG
            select = new SelectMultiQuery();
            select.addRoots(og);
            final JsonNode jsonResponse = metadataClient.selectObjectGrouptbyId(select.getFinalSelect(), og);
            LOGGER.warn("Result: " + jsonResponse);
            RequestResponseOK<ObjectGroup> objectGroupResponse =
                JsonHandler.getFromJsonNode(jsonResponse, new TypeReference<>() {});
            assertThat(objectGroupResponse).isNotNull();
            List<ObjectGroup> objectGroupList = objectGroupResponse.getResults();
            assertThat(objectGroupList).hasSize(1);
            ObjectGroup objectGroup = objectGroupList.iterator().next();
            // Bug 5159: check that all ObjectGroup _up are in _us
            assertThat(objectGroup.getList(VitamFieldsHelper.allunitups(), String.class))
                .containsAll(objectGroup.getList(VitamFieldsHelper.unitups(), String.class));
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
                (RequestResponseOK<JsonNode>) accessClient.selectUnitbyId(new SelectMultiQuery().getFinalSelect(), unitId);
            assertNotNull(responseUnitBeforeUpdate.getFirstResult());
            assertNotNull(responseUnitBeforeUpdate.getFirstResult().get("#management").get("AccessRule"));

            // execute update -> rules to be 'unset'
            Map<String, JsonNode> action = new HashMap<>();
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(tenantId));
            action.put("#management.AccessRule.Rules", JsonHandler.createArrayNode());
            UpdateMultiQuery updateQuery = new UpdateMultiQuery().addActions(new SetAction(action));
            updateQuery.addRoots(unitId);
            RequestResponse<JsonNode> response = accessClient
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
                (RequestResponseOK<JsonNode> ) accessClient.selectUnitbyId(new SelectMultiQuery().getFinalSelect(), unitId);

            // check version incremented in lfc
            assertEquals(6, checkAndRetrieveLfcVersionForUnit(unitId, accessClient));
            assertNotNull(responseUnitAfterUpdate.getFirstResult());
            assertEquals(responseUnitBeforeUpdate.getFirstResult().get("#opi"),
                responseUnitAfterUpdate.getFirstResult().get("#opi"));

            // execute update -> classification rules without classification owner
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(tenantId));

            UpdateMultiQuery updateQueryClassification = new UpdateMultiQuery()
                .addActions(new UnsetAction("#management.ClassificationRule.ClassificationOwner"));
            updateQueryClassification.addRoots(unitId);
            RequestResponse responseClassification = accessClient
                .updateUnitbyId(updateQueryClassification.getFinalUpdate(), unitId);
            assertFalse(responseClassification.isOk());
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
                (RequestResponseOK<JsonNode>) accessClient.selectUnitbyId(new SelectMultiQuery().getFinalSelect(), unitId);
            assertEquals(0,
                responseUnitAfterUpdatePreventInheritance.getFirstResult().get("#management").get("AccessRule")
                    .get("Inheritance").get("PreventRulesId").size());
            assertFalse(responseUnitAfterUpdatePreventInheritance.getFirstResult().get("#management").get("AccessRule")
                .get("Inheritance").get("PreventInheritance").asBoolean());

            sizedInputStream = new SizedInputStream(inputStream);
            final long size2 = StreamUtils.closeSilently(sizedInputStream);
            LOGGER.warn("read: " + size2);
            assertEquals(size2, size);

            JsonNode logbookOperation =
                accessClient.selectOperationById(operationGuid.getId())
                    .toJsonNode();
            assertThat(logbookOperation.get(RESULTS).get(0).get("evParentId")).isExactlyInstanceOf(NullNode.class);
            Set<String> eventIds = new HashSet<>();
            eventIds.add(logbookOperation.get(RESULTS).get(0).get("evId").asText());
            logbookOperation.get(RESULTS).get(0).get("events").forEach(event -> {
                if (event.get("evType").asText().contains("STP_UPLOAD_SIP")) {
                    assertThat(event.get("outDetail").asText()).contains("STP_UPLOAD_SIP");
                }
                eventIds.add(event.get("evId").asText());
            });

            // check evIds
            assertThat(eventIds.size()).isEqualTo(logbookOperation.get(RESULTS).get(0).get("events").size() + 1);

            QueryBuilder query = QueryBuilders.matchQuery("_id", operationGuid.getId());
            SearchResponse elasticSearchResponse =
                esClient.search(LogbookCollections.OPERATION, tenantId, query, null, null, 0, 25);
            assertEquals(1, elasticSearchResponse.getHits().getTotalHits().value);
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
                assertNotNull(operationGuid);
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
        final JsonNode result = lfcResponse.get(RESULTS);
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
        final JsonNode result = lfcResponse.get(RESULTS);
        assertNotNull(result);
        final JsonNode lfc = result.get(0);
        assertNotNull(lfc);

        return lfc;
    }


    @RunWithCustomExecutor
    @Test
    public void testPhysicalArchiveIngestInternal() throws Exception {
        GUID operationGuid = null;
        try {
            prepareVitamSession(tenantId, "aName3", "Context_IT");
            String operationId = VitamTestHelper.doIngest(tenantId, SIP_OK_PHYSICAL_ARCHIVE);
            operationGuid = GUIDReader.getGUID(operationId);
            verifyOperation(operationId, WARNING);

            // Try to check AU
            final MetaDataClient metadataClient = MetaDataClientFactory.getInstance().getClient();
            final JsonNode node = getArchiveUnitWithTitle(metadataClient, "Sed blandit mi dolor", TITLE);
            SelectMultiQuery select;
            LOGGER.debug(JsonHandler.prettyPrint(node));
            final JsonNode result = node.get(RESULTS);
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
            final List<String> valuesAsText = jsonResponse.get(RESULTS).findValuesAsText("#id");
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
        prepareVitamSession(tenantId, "aName3", "Context_IT");

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
        assertThat(logbookResult.get(RESULTS).get(0).get("evParentId")).isExactlyInstanceOf(NullNode.class);
        operationId = logbookResult.get(RESULTS).get(0).get("evId").asText();


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
        GUID operationGuid = null;
        try {
            prepareVitamSession(tenantId, "aName3", "Context_IT");
            IngestInternalClientFactory.getInstance().changeServerPort(runner.PORT_SERVICE_INGEST_INTERNAL);
            String operationId = VitamTestHelper.doIngest(tenantId, SIP_OK_PHYSICAL_ARCHIVE_WITH_ATTACHMENT_FROM_CONTARCT);
            operationGuid = GUIDReader.getGUID(operationId);
            verifyOperation(operationId, WARNING);

            // Try to check AU
            final MetaDataClient metadataClient = MetaDataClientFactory.getInstance().getClient();
            final JsonNode node = getArchiveUnitWithTitle(metadataClient, "Root AU ATTACHED", TITLE);
            LOGGER.debug(JsonHandler.prettyPrint(node));
            final JsonNode result = node.get(RESULTS);
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
            prepareVitamSession(tenantId, "aName3", "Context_IT");
            final String operationGuid = doIngest(tenantId,SIP_ARBRE, HOLDING_SCHEME,
                    ProcessAction.RESUME, StatusCode.STARTED);
            waitOperation(operationGuid);
            verifyOperation(operationGuid, OK);

            // Try to check AU - arborescence and parents stuff, without roots
            final MetaDataClient metadataClient = MetaDataClientFactory.getInstance().getClient();
            final JsonNode node = getArchiveUnitWithTitle(metadataClient, "Arbre simple", TITLE);
            LOGGER.debug(JsonHandler.prettyPrint(node));
            final JsonNode result = node.get(RESULTS);
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
        GUID operationGuid = null;
        try {
            prepareVitamSession(tenantId, "aName3", "Context_IT");
            IngestInternalClientFactory.getInstance().changeServerPort(runner.PORT_SERVICE_INGEST_INTERNAL);
            String operationId = VitamTestHelper.doIngest(tenantId, SIP_KO_PHYSICAL_ARCHIVE_BINARY_IN_PHYSICAL);
            operationGuid = GUIDReader.getGUID(operationId);
            verifyOperation(operationId, KO);
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
        GUID operationGuid = null;
        try {
            prepareVitamSession(tenantId, "aName3", "Context_IT");
            IngestInternalClientFactory.getInstance().changeServerPort(runner.PORT_SERVICE_INGEST_INTERNAL);
            String operationId = VitamTestHelper.doIngest(tenantId, SIP_KO_PHYSICAL_ARCHIVE_PHYSICAL_IN_BINARY);
            operationGuid = GUIDReader.getGUID(operationId);
            verifyOperation(operationId, KO);
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
        GUID operationGuid = null;
        try {
            prepareVitamSession(tenantId, "aName3", "Context_IT");
            IngestInternalClientFactory.getInstance().changeServerPort(runner.PORT_SERVICE_INGEST_INTERNAL);
            String operationId = VitamTestHelper.doIngest(tenantId, SIP_KO_PHYSICAL_ARCHIVE_PHYSICAL_ID_EMPTY);
            operationGuid = GUIDReader.getGUID(operationId);
            verifyOperation(operationId, KO);
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
        GUID operationGuid;
        prepareVitamSession(tenantId, "aName3", "Context_IT");
        IngestInternalClientFactory.getInstance().changeServerPort(runner.PORT_SERVICE_INGEST_INTERNAL);
        String operationId = VitamTestHelper.doIngest(tenantId, SIP_KO_WITH_SPECIAL_CHARS);
        operationGuid = GUIDReader.getGUID(operationId);
        verifyOperation(operationId, KO);

        final AccessInternalClient accessClient = AccessInternalClientFactory.getInstance().getClient();
        JsonNode logbookOperation =
            accessClient.selectOperationById(operationGuid.getId())
                .toJsonNode();
        boolean checkDataObject = true;
        final JsonNode elmt = logbookOperation.get(RESULTS).get(0);
        assertThat(elmt.get("evParentId")).isExactlyInstanceOf(NullNode.class);
        final List<Document> logbookOperationEvents =
            (List<Document>) new LogbookOperation(elmt).get(LogbookDocument.EVENTS);
        for (final Document event : logbookOperationEvents) {
            if (KO.toString()
                .equals(event.get(LogbookMongoDbName.outcome.getDbname()).toString()) &&
                event.get(LogbookMongoDbName.eventType.getDbname())
                    .equals("CHECK_UNIT_SCHEMA")) {
                checkDataObject = false;
                break;
            }
        }

        assertFalse(checkDataObject);
    }


    @RunWithCustomExecutor
    @Test
    public void testIngestInternal1791CA1() throws Exception {
        GUID operationGuid;
        prepareVitamSession(tenantId, "aName3", "Context_IT");
        IngestInternalClientFactory.getInstance().changeServerPort(runner.PORT_SERVICE_INGEST_INTERNAL);
        String operationId = VitamTestHelper.doIngest(tenantId, SIP_KO_WITH_EMPTY_TITLE);
        operationGuid = GUIDReader.getGUID(operationId);
        verifyOperation(operationId, KO);

        final AccessInternalClient accessClient = AccessInternalClientFactory.getInstance().getClient();
        JsonNode logbookOperation =
            accessClient.selectOperationById(operationGuid.getId())
                .toJsonNode();
        boolean checkUnitSuccess = true;
        final JsonNode elmt = logbookOperation.get(RESULTS).get(0);
        assertThat(elmt.get("evParentId")).isExactlyInstanceOf(NullNode.class);
        final List<Document> logbookOperationEvents =
            (List<Document>) new LogbookOperation(elmt).get(LogbookDocument.EVENTS.toString());
        for (final Document event : logbookOperationEvents) {
            if (KO.toString()
                .equals(event.get(LogbookMongoDbName.outcome.getDbname()).toString()) &&
                event.get(LogbookMongoDbName.eventType.getDbname()).equals("CHECK_UNIT_SCHEMA")) {
                checkUnitSuccess = false;
                break;
            }
        }

        assertFalse(checkUnitSuccess);
    }

    @RunWithCustomExecutor
    @Test
    public void testIngestInternal1791CA2() throws Exception {
        GUID operationGuid;
        prepareVitamSession(tenantId, "aName3", "Context_IT");
        IngestInternalClientFactory.getInstance().changeServerPort(runner.PORT_SERVICE_INGEST_INTERNAL);
        String operationId = VitamTestHelper.doIngest(tenantId, SIP_KO_WITH_INCORRECT_DATE);
        operationGuid = GUIDReader.getGUID(operationId);
        verifyOperation(operationId, KO);

        final AccessInternalClient accessClient = AccessInternalClientFactory.getInstance().getClient();
        JsonNode logbookOperation =
            accessClient.selectOperationById(operationGuid.getId())
                .toJsonNode();
        boolean checkUnitSuccess = true;
        final JsonNode elmt = logbookOperation.get(RESULTS).get(0);
        assertThat(elmt.get("evParentId")).isExactlyInstanceOf(NullNode.class);
        final List<Document> logbookOperationEvents =
            (List<Document>) new LogbookOperation(elmt).get(LogbookDocument.EVENTS.toString());
        for (final Document event : logbookOperationEvents) {
            if (KO.toString()
                .equals(event.get(LogbookMongoDbName.outcome.getDbname()).toString()) &&
                event.get(LogbookMongoDbName.eventType.getDbname()).equals("CHECK_UNIT_SCHEMA")) {
                checkUnitSuccess = false;
                break;
            }
        }

        assertFalse(checkUnitSuccess);
    }

    @RunWithCustomExecutor
    @Test
    public void testIngestTreeWhenIngestContractTagNotFound() throws Exception {
        prepareVitamSession(tenantId, "aName3", "Context_IT");
        final String operationGuid = doIngest(tenantId, SIP_TREE_WITHOUT_INGEST_CONTRACT, HOLDING_SCHEME,
            ProcessAction.RESUME, StatusCode.STARTED);
        waitOperation(operationGuid);
        verifyOperation(operationGuid, KO);
        JsonNode logbook = findLogbook(operationGuid);
        assertThat(logbook).isNotNull();
        assertThat(logbook.toString()).contains("CHECK_HEADER.CHECK_CONTRACT_INGEST.CONTRACT_NOT_IN_MANIFEST.KO");
    }

    @RunWithCustomExecutor
    @Test
    public void testIngestWithManifestIncorrectObjectNumber() throws Exception {
        prepareVitamSession(tenantId, "aName3", "Context_IT");
        IngestInternalClientFactory.getInstance().changeServerPort(runner.PORT_SERVICE_INGEST_INTERNAL);
        String operationId = VitamTestHelper.doIngest(tenantId, SIP_NB_OBJ_INCORRECT_IN_MANIFEST);
        verifyOperation(operationId, KO);

    }

    @RunWithCustomExecutor
    @Test
    public void testIngestWithManifestHavingMgtRules() throws Exception {
        prepareVitamSession(tenantId, "aName3", "Context_IT");
        IngestInternalClientFactory.getInstance().changeServerPort(runner.PORT_SERVICE_INGEST_INTERNAL);
        String operationId = VitamTestHelper.doIngest(tenantId, SIP_OK_WITH_MGT_META_DATA_ONLY_RULES);
        verifyOperation(operationId, WARNING);

        // Try to check AU
        final MetaDataClient metadataClient = MetaDataClientFactory.getInstance().getClient();
        final JsonNode node = getArchiveUnitWithTitle(metadataClient, "Unit with Management META DATA rules", TITLE);
        LOGGER.debug(JsonHandler.prettyPrint(node));
        final JsonNode result = node.get(RESULTS);
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
        prepareVitamSession(tenantId, "aName3", "Context_IT");
        IngestInternalClientFactory.getInstance().changeServerPort(runner.PORT_SERVICE_INGEST_INTERNAL);
        String operationId = VitamTestHelper.doIngest(tenantId, SIP_OK_WITH_BOTH_UNITMGT_MGTMETADATA_RULES);
        GUID operationGuid = GUIDReader.getGUID(operationId);
        verifyOperation(operationId, WARNING);
        verifyProcessState(operationGuid.toString(),tenantId, COMPLETED);

        // Try to check AU
        final MetaDataClient metadataClient = MetaDataClientFactory.getInstance().getClient();
        final JsonNode node = getArchiveUnitWithTitle(metadataClient, "UNIT with both rules", TITLE);
        LOGGER.debug(JsonHandler.prettyPrint(node));
        final JsonNode result = node.get(RESULTS);
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
        prepareVitamSession(tenantId, "aName3", "Context_IT");
        IngestInternalClientFactory.getInstance().changeServerPort(runner.PORT_SERVICE_INGEST_INTERNAL);
        String operationId = VitamTestHelper.doIngest(tenantId, SIP_OK_WITH_BOTH_UNITMGT_MGTMETADATA_RULES_WiTHOUT_OBJECTS);
        verifyOperation(operationId, WARNING);

        // Try to check AU
        final MetaDataClient metadataClient = MetaDataClientFactory.getInstance().getClient();
        final JsonNode node = getArchiveUnitWithTitle(metadataClient, "LEVANT", TITLE);
        LOGGER.debug(JsonHandler.prettyPrint(node));
        final JsonNode result = node.get(RESULTS);
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
        prepareVitamSession(tenantId, "aName3", "Context_IT");
        IngestInternalClientFactory.getInstance().changeServerPort(runner.PORT_SERVICE_INGEST_INTERNAL);
        String operationId = VitamTestHelper.doIngest(tenantId, SIP_OK_WITH_ADDRESSEE);
        GUID operationGuid = GUIDReader.getGUID(operationId);
        verifyOperation(operationId, KO);

        final AccessInternalClient accessClient = AccessInternalClientFactory.getInstance().getClient();
        JsonNode logbookOperation =
            accessClient.selectOperationById(operationGuid.getId())
                .toJsonNode();
        boolean checkUnitSuccess = false;
        final JsonNode elmt = logbookOperation.get(RESULTS).get(0);
        assertThat(elmt.get("evParentId")).isExactlyInstanceOf(NullNode.class);
        final List<Document> logbookOperationEvents =
            (List<Document>) new LogbookOperation(elmt).get(LogbookDocument.EVENTS.toString());
        for (final Document event : logbookOperationEvents) {
            if (KO.toString()
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
        prepareVitamSession(tenantId, "aName3", "Context_IT");
        IngestInternalClientFactory.getInstance().changeServerPort(runner.PORT_SERVICE_INGEST_INTERNAL);
        String operationId = VitamTestHelper.doIngest(tenantId, SIP_OK_WITH_SERVICE_LEVEL);
        GUID operationGuid = GUIDReader.getGUID(operationId);
        verifyOperation(operationId, WARNING);

        final AccessInternalClient accessClient = AccessInternalClientFactory.getInstance().getClient();
        JsonNode logbookOperation =
            accessClient.selectOperationById(operationGuid.getId())
                .toJsonNode();
        boolean checkServiceLevel = false;
        final JsonNode elmt = logbookOperation.get(RESULTS).get(0);
        assertThat(elmt.get("evParentId")).isExactlyInstanceOf(NullNode.class);
        final List<Document> logbookOperationEvents =
            (List<Document>) new LogbookOperation(elmt).get(LogbookDocument.EVENTS.toString());
        for (final Document event : logbookOperationEvents) {
            if (OK.toString()
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
        prepareVitamSession(tenantId, "aName3", "Context_IT");
        IngestInternalClientFactory.getInstance().changeServerPort(runner.PORT_SERVICE_INGEST_INTERNAL);
        String operationId = VitamTestHelper.doIngest(tenantId, SIP_OK_WITHOUT_SERVICE_LEVEL);
        GUID operationGuid = GUIDReader.getGUID(operationId);
        verifyOperation(operationId, WARNING);

        final AccessInternalClient accessClient = AccessInternalClientFactory.getInstance().getClient();
        JsonNode logbookOperation =
            accessClient.selectOperationById(operationGuid.getId())
                .toJsonNode();
        boolean checkServiceLevel = false;
        final JsonNode elmt = logbookOperation.get(RESULTS).get(0);
        assertThat(elmt.get("evParentId")).isExactlyInstanceOf(NullNode.class);
        final List<Document> logbookOperationEvents =
            (List<Document>) new LogbookOperation(elmt).get(LogbookDocument.EVENTS.toString());
        for (final Document event : logbookOperationEvents) {
            if (OK.toString()
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
        GUID operationGuid = null;
        try {
            prepareVitamSession(tenantId, "aName3", "Context_IT");
            IngestInternalClientFactory.getInstance().changeServerPort(runner.PORT_SERVICE_INGEST_INTERNAL);
            String operationId = VitamTestHelper.doIngest(tenantId, SIP_FILE_OK_NAME);
            operationGuid = GUIDReader.getGUID(operationId);
            verifyOperation(operationId, OK);
            verifyProcessState(operationId, tenantId, COMPLETED);

            SelectMultiQuery select = new SelectMultiQuery();
            select.addQueries(QueryHelper.match("Title", "Sensibilisation API"));
            // Get AU
            final AccessInternalClient accessClient = AccessInternalClientFactory.getInstance().getClient();
            RequestResponse<JsonNode> response = accessClient.selectUnits(select.getFinalSelect());
            assertTrue(response.isOk());

            // Get GOT
            LOGGER.warn(response.toString());
            final JsonNode node = response.toJsonNode().get(RESULTS).get(0);
            LOGGER.warn(node.toString());
            final String unitId = node.get("#object").asText();


            SelectMultiQuery select2 = new SelectMultiQuery();
            response = accessClient.selectObjectbyId(select2.getFinalSelect(), unitId);
            assertTrue(response.isOk());

            // Get logbook
            SelectMultiQuery select3 = new SelectMultiQuery();
            select.addQueries(QueryHelper.eq("evType", "Process_SIP_unitary"));
            response = accessClient.selectOperation(select3.getFinalSelect(), false, false);
            assertTrue(response.isOk());




            final InputStream zipInputStreamSipObject2 =
                PropertiesUtils.getResourceAsStream(SIP_TREE);

            String operationGuid2 = VitamTestHelper.doIngest(tenantId, zipInputStreamSipObject2, HOLDING_SCHEME, RESUME, STARTED);
            verifyOperation(operationGuid2, OK);

            VitamThreadUtils.getVitamSession().setContractId("aName4");

            SelectMultiQuery selectTree = new SelectMultiQuery();
            selectTree.addQueries(QueryHelper.eq("Title", "testArbre2").setDepthLimit(5));
            // Get AU
            RequestResponse<JsonNode> responseTree = accessClient.selectUnits(selectTree.getFinalSelect());
            assertTrue(responseTree.isOk());
            assertEquals(1, responseTree.toJsonNode().get("$hits").get("total").asInt());
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
        prepareVitamSession(tenantId, "aName3", "Context_IT");
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
        prepareVitamSession(tenantId, "aName3", "Context_IT");

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
        assertThat(logbookResult.get(RESULTS).get(0).get("evParentId")).isExactlyInstanceOf(NullNode.class);


        stream = new FileInputStream(PropertiesUtils.findFile(FILE_AGENCIES_AU_update));
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(tenantId));

        status = client.importAgenciesFile(stream, FILE_AGENCIES_AU_update);
        ResponseBuilder = Response.status(status);
        response = ResponseBuilder.build();
        assertEquals(response.getStatus(), Status.CREATED.getStatusCode());
    }


    @Test
    @RunWithCustomExecutor
    public void shouldRetrieveReportWhenCheckFileRules() throws Exception {
        prepareVitamSession(tenantId, "aName3", "Context_IT");
        FileInputStream stream = new FileInputStream(PropertiesUtils.findFile(FILE_RULES_KO_DUPLICATED_REFERENCE));
        FileInputStream streamErrorReport = new FileInputStream(PropertiesUtils.findFile(ERROR_REPORT_CONTENT));
        checkFileRulesWithCustomReferential(stream, streamErrorReport, LINE_3);
    }


    @Test
    @RunWithCustomExecutor
    public void shouldRetrieveReportWhencheckFileRulesError6000Day() throws Exception {
        prepareVitamSession(tenantId, "aName3", "Context_IT");
        FileInputStream stream = new FileInputStream(PropertiesUtils.findFile(FILE_RULES_KO_600000_DAY));
        FileInputStream streamErrorReport = new FileInputStream(PropertiesUtils.findFile(ERROR_REPORT_6000_DAYS));
        checkFileRulesWithCustomReferential(stream, streamErrorReport, LINE_2);
    }

    @Test
    @RunWithCustomExecutor
    public void shouldRetrieveReportWhencheckFileRulesError9000Years() throws Exception {
        prepareVitamSession(tenantId, "aName3", "Context_IT");
        FileInputStream stream = new FileInputStream(PropertiesUtils.findFile(FILE_RULES_KO_90000_YEAR));
        FileInputStream streamErrorReport = new FileInputStream(PropertiesUtils.findFile(ERROR_REPORT_9000_YEARS));
        checkFileRulesWithCustomReferential(stream, streamErrorReport, LINE_2);
    }

    @Test
    @RunWithCustomExecutor
    public void shouldRetrieveReportWhenCheckFileRulesErrorAnarchyRules() throws Exception {
        prepareVitamSession(tenantId, "aName3", "Context_IT");
        FileInputStream stream = new FileInputStream(PropertiesUtils.findFile(FILE_RULES_KO_ANARCHY_RULE));
        FileInputStream streamErrorReport =
            new FileInputStream(PropertiesUtils.findFile(ERROR_REPORT_ANARCHY_RULE));
        checkFileRulesWithCustomReferential(stream, streamErrorReport, LINE_2);
    }

    @Test
    @RunWithCustomExecutor
    public void shouldRetrieveReportWhenCheckFileRulesDecadeMeasurement() throws Exception {
        prepareVitamSession(tenantId, "aName3", "Context_IT");
        FileInputStream stream = new FileInputStream(PropertiesUtils.findFile(FILE_RULES_KO_DECADE_MEASURE));
        FileInputStream streamErrorReport =
            new FileInputStream(PropertiesUtils.findFile(ERROR_REPORT_DECADE_MEASURE));
        checkFileRulesWithCustomReferential(stream, streamErrorReport, LINE_2);
    }

    @Test
    @RunWithCustomExecutor
    public void shouldRetrieveReportWhenCheckFileRulesErrorNegativeDuration() throws Exception {
        prepareVitamSession(tenantId, "aName3", "Context_IT");
        FileInputStream stream = new FileInputStream(PropertiesUtils.findFile(FILE_RULES_KO_NEGATIVE_DURATION));
        FileInputStream streamErrorReport =
            new FileInputStream(PropertiesUtils.findFile(ERROR_REPORT_NEGATIVE_DURATION));
        checkFileRulesWithCustomReferential(stream, streamErrorReport, LINE_2);
    }

    @Test
    @RunWithCustomExecutor
    public void shouldRetrieveReportWhenCheckFileRulesErrorWrongComa() throws Exception {
        prepareVitamSession(tenantId, "aName3", "Context_IT");
        FileInputStream stream =
            new FileInputStream(PropertiesUtils.findFile(FILE_RULES_KO_REFERENCE_WITH_WRONG_COMMA));
        AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient();

        assertThatThrownBy(() -> client.checkRulesFile(stream))
                .isInstanceOf(AdminManagementClientServerException.class)
                .hasMessageContaining("\\u00C9chec du processus d'import du r\\u00E9f\\u00E9rentiel des r\\u00E8gles de gestion");
    }

    @Test
    @RunWithCustomExecutor
    public void shouldRetrieveReportWhenCheckFileRulesErrorWrongRuleType() throws Exception {
        prepareVitamSession(tenantId, "aName3", "Context_IT");
        FileInputStream stream =
            new FileInputStream(PropertiesUtils.findFile(FILE_RULES_KO_WRONG_RULETYPE));
        AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient();

        assertThatThrownBy(() -> client.checkRulesFile(stream))
            .isInstanceOf(AdminManagementClientServerException.class)
            .hasMessageContaining("\\u00C9chec du processus d'import du r\\u00E9f\\u00E9rentiel des r\\u00E8gles de gestion");
    }

    @Test
    @RunWithCustomExecutor
    public void shouldRetrieveReportWhenCheckFileRulesErrorUnknowDuration() throws Exception {
        prepareVitamSession(tenantId, "aName3", "Context_IT");
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
            final JsonNode expectedNode = JsonHandler.getFromInputStream(expectedStreamErrorReport);
            final JsonNode expectedError = expectedNode.get("error").get(lineNumber).get(0).get("Code");
            assertThatThrownBy(() -> client.checkRulesFile(fileInputStreamToImport))
                .isInstanceOf(AdminManagementClientServerException.class)
                .hasMessageContaining(expectedError.asText());
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testIngestInternalMultipleActions() throws Exception {
        prepareVitamSession(tenantId, "aName3", "Context_IT");
        final IngestInternalClient client2 = IngestInternalClientFactory.getInstance().getClient();
        IngestInternalClientFactory.getInstance().changeServerPort(runner.PORT_SERVICE_INGEST_INTERNAL);
        String operationId = VitamTestHelper.doIngestNext(tenantId, SIP_OK_PHYSICAL_ARCHIVE);
        GUID operationGuid = GUIDReader.getGUID(operationId);
        verifyOperation(operationId, OK);
        verifyProcessState(operationId, tenantId, PAUSE);

        RequestResponse<ItemStatus> requestResponse =
                client2.getOperationProcessExecutionDetails(operationGuid.toString());
        assertThat(requestResponse.isOk()).isTrue();
        RequestResponseOK<ItemStatus> responseOK = (RequestResponseOK<ItemStatus>) requestResponse;
        assertThat(responseOK.getResults().iterator().next().getGlobalStatus()).isEqualTo(OK);

        assertNotNull(client2.getWorkflowDefinitions());

        // then finally we cancel the ingest
        requestResponse = client2.cancelOperationProcessExecution(operationGuid.toString());
        assertThat(requestResponse.isOk()).isTrue();
        assertThat(requestResponse.getHttpCode()).isEqualTo(Response.Status.ACCEPTED.getStatusCode());
        responseOK = (RequestResponseOK<ItemStatus>) requestResponse;
        assertThat(responseOK.getResults().iterator().hasNext()).isTrue();
        assertThat(responseOK.getResults().iterator().next().getGlobalStatus()).isEqualTo(KO);

        awaitForWorkflowTerminationWithStatus(operationGuid, KO);

        verifyOperation(operationGuid.getId(), KO);
        verifyProcessState(operationGuid.getId(), tenantId, COMPLETED);
    }

    @RunWithCustomExecutor
    @Test
    public void testIngestInternal4396() throws Exception {
        prepareVitamSession(tenantId, "aName3", "Context_IT");
        IngestInternalClientFactory.getInstance().changeServerPort(runner.PORT_SERVICE_INGEST_INTERNAL);
        String operationId = VitamTestHelper.doIngest(tenantId, SIP_4396);
        GUID operationGuid = GUIDReader.getGUID(operationId);
        verifyOperation(operationId, OK);

        SelectMultiQuery select = new SelectMultiQuery();
        select.addQueries(QueryHelper.and().add(QueryHelper.match("Title", "monSIP"))
            .add(QueryHelper.in("#operations", operationGuid.toString())));
        // Get AU
        final MetaDataClient metadataClient = MetaDataClientFactory.getInstance().getClient();
        final JsonNode node = metadataClient.selectUnits(select.getFinalSelect());

        // management for this unit
        LOGGER.warn(node.toString());
        assertNotNull(node);
        assertNotNull(node.get(RESULTS));
        assertEquals(1, node.get(RESULTS).size());
        assertNotNull(node.get(RESULTS).get(0).get("Title"));
        assertNotNull(node.get(RESULTS).get(0).get("#management"));
        assertEquals("Secret Défense",
            node.get(RESULTS).get(0).get("#management").get("ClassificationRule").get("ClassificationLevel")
                .asText());
        assertEquals("ClassOWn",
            node.get(RESULTS).get(0).get("#management").get("ClassificationRule").get("ClassificationOwner")
                .asText());

    }


    @RunWithCustomExecutor
    @Test
    public void testApplyAccessContractSecurityFilter()
        throws FileNotFoundException, InvalidParseOperationException, DatabaseException {

        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        prepareVitamSession(tenantId, "aName3", "Context_IT");
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        // UniqueTitleParent : aeaqaaaaaahmtusqabktwaldc34sm5yaaaaq
        // UniqueTitleChild : aeaqaaaaaahmtusqabktwaldc34sm5iaaabq
        final List<Document> unitList =
            JsonHandler.getFromFileAsTypeReference(PropertiesUtils
                    .getResourceFile("integration-ingest-internal/data/units_tree_access_contract_test.json"),
                new TypeReference<>() {
                });

        // Save units in Mongo
        VitamRepositoryFactory.get().getVitamMongoRepository(MetadataCollections.UNIT.getVitamCollection())
            .save(unitList);

        // Save units in Elasticsearch
        VitamRepositoryFactory.get().getVitamESRepository(MetadataCollections.UNIT.getVitamCollection(),
            metadataIndexManager.getElasticsearchIndexAliasResolver(MetadataCollections.UNIT))
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

            // check rule filter
            Query query3 = QueryHelper.exists("Title");
            selectMultiple.getRoots().clear();
            selectMultiple.getQueries().clear();
            selectMultiple.addQueries(query3);
            accessContractModel.getRootUnits().clear();
            accessContractModel.getOriginatingAgencies().clear();
            accessContractModel.setEveryOriginatingAgency(true);
            accessContractModel.getRuleCategoryToFilter().add(RuleType.DisseminationRule);
            newJson = AccessContractRestrictionHelper
                .applyAccessContractRestrictionForUnitForSelect(selectMultiple.getFinalSelect(), accessContractModel);
            assertThat(newJson).isNotNull();
            result = accessInternalModule.selectUnit(newJson);
            assertThat(result).isNotNull();
            res = JsonHandler.getFromString(result.toString(), RequestResponseOK.class, JsonNode.class);
            assertThat(res.getResults()).hasSize(1);

            //////////////////////////
            /// Test Depth negative
            /////////////////////////
            query_1 = QueryHelper.eq("Title", "UniqueTitleChild");

            query_2 = QueryHelper.eq("Title", "UniqueTitleChild");
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
        prepareVitamSession(tenantId, "aName3", "Context_IT");
        String operationId = VitamTestHelper.doIngest(tenantId, OK_RULES_COMPLEX_COMPLETE_V2_SIP);
        final GUID operationGuid = GUIDReader.getGUID(operationId);
        verifyOperation(operationId, OK);

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

        InheritedRuleCategoryResponseModel holdRuleCategory =
            unitInheritedRules.getRuleCategories().get(VitamConstants.TAG_RULE_HOLD);

        assertThat(holdRuleCategory.getRules()).hasSize(4);

        InheritedRuleResponseModel hol00001 = holdRuleCategory.getRules().stream()
            .filter(r -> r.getRuleId().equals("HOL-00001")).findFirst().orElseThrow();

        InheritedRuleResponseModel hol00002 = holdRuleCategory.getRules().stream()
            .filter(r -> r.getRuleId().equals("HOL-00002")).findFirst().orElseThrow();

        InheritedRuleResponseModel hol00004_form_root = holdRuleCategory.getRules().stream()
            .filter(r -> r.getRuleId().equals("HOL-00004"))
            .filter(r -> r.getPaths().get(0).size() == 4)
            .findFirst().orElseThrow();

        InheritedRuleResponseModel hol00004_form_other_parent = holdRuleCategory.getRules().stream()
            .filter(r -> r.getRuleId().equals("HOL-00004"))
            .filter(r -> r.getPaths().get(0).size() == 3)
            .findFirst().orElseThrow();

        assertThat(hol00001.getRuleId()).isEqualTo("HOL-00001");
        assertThat(hol00001.getPaths()).hasSize(2);
        assertThat(hol00001.getPaths().get(0)).hasSize(4);
        assertThat(hol00001.getPaths().get(1)).hasSize(4);
        assertThat(hol00001.getExtendedRuleAttributes()).containsOnlyKeys(
            RuleModel.START_DATE, RuleModel.END_DATE, RuleModel.HOLD_OWNER, RuleModel.PREVENT_REARRANGEMENT);
        assertThat(hol00001.getStartDate()).isEqualTo("2000-01-01");
        assertThat(hol00001.getEndDate()).isEqualTo("2001-01-01");
        assertThat(hol00001.getExtendedRuleAttributes().get(RuleModel.HOLD_OWNER)).isEqualTo("Owner");
        assertThat(hol00001.getExtendedRuleAttributes().get(RuleModel.PREVENT_REARRANGEMENT)).isEqualTo(false);

        assertThat(hol00002.getRuleId()).isEqualTo("HOL-00002");
        assertThat(hol00002.getPaths()).hasSize(1);
        assertThat(hol00002.getPaths().get(0)).hasSize(4);
        assertThat(hol00002.getExtendedRuleAttributes()).containsOnlyKeys(
            RuleModel.END_DATE, RuleModel.HOLD_END_DATE, RuleModel.HOLD_REASON, RuleModel.HOLD_REASSESSING_DATE);
        assertThat(hol00002.getStartDate()).isNull();
        assertThat(hol00002.getEndDate()).isEqualTo("2010-01-01");
        assertThat(hol00002.getExtendedRuleAttributes().get(RuleModel.HOLD_END_DATE)).isEqualTo("2010-01-01");
        assertThat(hol00002.getExtendedRuleAttributes().get(RuleModel.HOLD_REASON)).isEqualTo("Reason");
        assertThat(hol00002.getExtendedRuleAttributes().get(RuleModel.HOLD_REASSESSING_DATE)).isEqualTo("2005-01-01");

        assertThat(hol00004_form_root.getRuleId()).isEqualTo("HOL-00004");
        assertThat(hol00004_form_root.getPaths()).hasSize(1);
        assertThat(hol00004_form_root.getPaths().get(0)).hasSize(4);
        assertThat(hol00004_form_root.getExtendedRuleAttributes()).isEmpty();
        assertThat(hol00004_form_root.getStartDate()).isNull();
        assertThat(hol00004_form_root.getEndDate()).isNull();

        assertThat(hol00004_form_other_parent.getRuleId()).isEqualTo("HOL-00004");
        assertThat(hol00004_form_other_parent.getPaths()).hasSize(1);
        assertThat(hol00004_form_other_parent.getPaths().get(0)).hasSize(3);
        assertThat(hol00004_form_other_parent.getExtendedRuleAttributes()).containsOnlyKeys(RuleModel.HOLD_OWNER);
        assertThat(hol00004_form_other_parent.getStartDate()).isNull();
        assertThat(hol00004_form_other_parent.getEndDate()).isNull();
        assertThat(hol00004_form_other_parent.getExtendedRuleAttributes().get(RuleModel.HOLD_OWNER))
            .isEqualTo("Owner HOL-00004");
    }

    @RunWithCustomExecutor
    @Test
    public void testEliminationAnalysisOverSipWithComplexRules() throws Exception {
        prepareVitamSession(tenantId, "aName3", "Context_IT");
        IngestInternalClientFactory.getInstance().changeServerPort(runner.PORT_SERVICE_INGEST_INTERNAL);
        String operationId = VitamTestHelper.doIngest(tenantId, OK_RULES_COMPLEX_COMPLETE_V2_SIP);
        final GUID ingestOperationGuid = GUIDReader.getGUID(operationId);
        verifyOperation(operationId, OK);

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

        awaitForWorkflowTerminationWithStatus(eliminationAnalysisOperationGuid, OK);

        // Check indexation querying
        final RequestResponseOK<JsonNode> destroyableIndexedUnitsResponse =
            queryIndexedEliminationAnalysis(ingestOperationGuid, eliminationAnalysisOperationGuid,
                accessInternalClient, EliminationGlobalStatus.DESTROY);

        List<String> indexedUnitTitles = destroyableIndexedUnitsResponse.getResults()
            .stream()
            .map(node -> node.get("Title").asText())
            .collect(Collectors.toList());

        assertThat(indexedUnitTitles)
            .containsExactlyInAnyOrder("Porte de Pantin", "Eglise de Pantin");

        // Check access to #elimination field
        assertThat(destroyableIndexedUnitsResponse.getFirstResult()
            .get(VitamFieldsHelper.elimination())
            .get(0)
            .get("OperationId").asText()).isEqualTo(eliminationAnalysisOperationGuid.toString());

        // Ensure document version has not been updated during indexation process
        assertThat(destroyableIndexedUnitsResponse.getFirstResult().get(VitamFieldsHelper.version()).asInt())
            .isEqualTo(0);

        // Check indexation of elimination "CONFLICT"
        final RequestResponseOK<JsonNode> conflictIndexedUnitsResponse =
            queryIndexedEliminationAnalysis(ingestOperationGuid, eliminationAnalysisOperationGuid,
                accessInternalClient, EliminationGlobalStatus.CONFLICT);
        assertThat(conflictIndexedUnitsResponse.getResults()).hasSize(1);
        assertThat(conflictIndexedUnitsResponse.getResults().get(0).get("Title").asText()).isEqualTo("Stalingrad.txt");

        EliminationAnalysisResult conflictEliminationAnalysisResult =
            JsonHandler.getFromJsonNode(
                conflictIndexedUnitsResponse.getResults().get(0).get(VitamFieldsHelper.elimination()),
                EliminationAnalysisResult.class);

        assertThat(conflictEliminationAnalysisResult.getDestroyableOriginatingAgencies()).
            containsExactlyInAnyOrder("RATP");
        assertThat(conflictEliminationAnalysisResult.getNonDestroyableOriginatingAgencies()).isEmpty();
        assertThat(conflictEliminationAnalysisResult.getOperationId()).isEqualTo(eliminationAnalysisOperationGuid.getId());
        assertThat(conflictEliminationAnalysisResult.getGlobalStatus()).isEqualTo(EliminationGlobalStatus.CONFLICT);
        assertThat(conflictEliminationAnalysisResult.getExtendedInfo()).hasSize(1);
        assertThat(conflictEliminationAnalysisResult.getExtendedInfo().get(0)).isInstanceOf(
            EliminationExtendedInfoHoldRule.class);
        assertThat(((EliminationExtendedInfoHoldRule) conflictEliminationAnalysisResult.getExtendedInfo().get(0))
            .getDetails().getHoldRuleIds()).containsExactlyInAnyOrder("HOL-00004");
    }

    private RequestResponseOK<JsonNode> queryIndexedEliminationAnalysis(GUID ingestOperationGuid,
        GUID eliminationAnalysisOperationGuid, AccessInternalClient accessInternalClient,
        EliminationGlobalStatus status)
        throws InvalidCreateOperationException, InvalidParseOperationException, AccessInternalClientServerException,
        AccessInternalClientNotFoundException, AccessUnauthorizedException, BadRequestException {
        SelectMultiQuery checkAnalysisDslRequest = new SelectMultiQuery();
        checkAnalysisDslRequest.addQueries(
            QueryHelper.and().add(QueryHelper.eq(VitamFieldsHelper.initialOperation(), ingestOperationGuid.toString()))
                .add(QueryHelper
                    .eq(VitamFieldsHelper.elimination() + ".OperationId", eliminationAnalysisOperationGuid.toString()))
                .add(QueryHelper.eq(VitamFieldsHelper.elimination() + ".GlobalStatus", status.name())));

        checkAnalysisDslRequest.addUsedProjection(
            "Title", VitamFieldsHelper.elimination(), VitamFieldsHelper.version());

        final RequestResponseOK<JsonNode> indexedUnitsResponse =
            (RequestResponseOK<JsonNode>) accessInternalClient.selectUnits(checkAnalysisDslRequest.getFinalSelect());

        assertThat(indexedUnitsResponse.isOk()).isTrue();
        return indexedUnitsResponse;
    }

    @RunWithCustomExecutor
    @Test
    public void testLFCAccessForUnitAndGot() throws Exception {
        GUID operationGuid = null;
        try {
            prepareVitamSession(tenantId, "aName3", "Context_IT");
            VitamThreadUtils.getVitamSession().setContractId("aName4");
            IngestInternalClientFactory.getInstance().changeServerPort(runner.PORT_SERVICE_INGEST_INTERNAL);
            String operationId = VitamTestHelper.doIngest(tenantId, SIP_OK_PHYSICAL_ARCHIVE_FOR_LFC);
            operationGuid = GUIDReader.getGUID(operationId);
            verifyOperation(operationId, WARNING);

            // Try to check AU
            final MetaDataClient metadataClient = MetaDataClientFactory.getInstance().getClient();
            final JsonNode node = getArchiveUnitWithTitle(metadataClient, "Sed blandit mi dolor", TITLE);
            SelectMultiQuery select;
            LOGGER.debug(JsonHandler.prettyPrint(node));
            final JsonNode result = node.get(RESULTS);
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
            final List<String> valuesAsText = jsonResponse.get(RESULTS).findValuesAsText("#id");
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
                assertEquals("Access by Contract Exception", e.getMessage());
            }

            try {
                retrieveLfcForGot(objectId, accessClient);
            } catch (AccessUnauthorizedException e) {
                assertEquals("Access by Contract Exception", e.getMessage());
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
        awaitForWorkflowTerminationWithStatus(operationGuid, status, COMPLETED);
    }

    private void awaitForWorkflowTerminationWithStatus(GUID operationGuid, StatusCode status,
        ProcessState processState) {

        waitOperation(operationGuid.toString());

        ProcessWorkflow processWorkflow =
            ProcessMonitoringImpl.getInstance().findOneProcessWorkflow(operationGuid.toString(), tenantId);

        assertNotNull(processWorkflow);
        assertEquals(processState, processWorkflow.getState());
        assertEquals(status, processWorkflow.getStatus());
    }



    @RunWithCustomExecutor
    @Test
    public void testExternalLogbook() throws Exception {
        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        final GUID operationGuidForRequestId = GUIDFactory.newOperationLogbookGUID(tenantId);
        prepareVitamSession(tenantId, "aName3", "Context_IT");
        VitamThreadUtils.getVitamSession().setRequestId(operationGuidForRequestId);

        // external logbook creation

        // root logbook
        final LogbookOperationParameters logbookOperationparams =
            LogbookParameterHelper.newLogbookOperationParameters(
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
        final LogbookOperationParameters eventParameters = LogbookParameterHelper
            .newLogbookOperationParameters(
                eventGuid,
                "EXT_External_Operation",
                operationGuid,
                LogbookTypeProcess.EXTERNAL_LOGBOOK,
                OK,
                "outcomeDetailMessage",
                operationGuid);
        Set<LogbookParameters> events = new LinkedHashSet<>();
        events.add(eventParameters);
        logbookOperationparams.setEvents(events);

        final LogbookOperationParameters logbookOperationparamsWrongType =
            LogbookParameterHelper.newLogbookOperationParameters(
                operationGuid, "External_Operation", operationGuid,
                LogbookTypeProcess.EXTERNAL_LOGBOOK, StatusCode.STARTED,
                operationGuid != null ? operationGuid.toString() : "outcomeDetailMessage",
                operationGuid);
        final LogbookOperationParameters logbookOperationparamsWrongTypeProc =
            LogbookParameterHelper.newLogbookOperationParameters(
                operationGuid, "EXT_External_Operation", operationGuid,
                LogbookTypeProcess.AUDIT, StatusCode.STARTED,
                operationGuid != null ? operationGuid.toString() : "outcomeDetailMessage",
                operationGuid);

        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient();
            AccessInternalClient accessClient = AccessInternalClientFactory.getInstance().getClient()) {

            assertEquals(Status.CREATED, client.createExternalOperation(logbookOperationparams));

            JsonNode logbookOperation =
                accessClient
                    .selectOperationById(operationGuidForRequestId.getId())
                    .toJsonNode();
            // assert certain parameters in the master are overloaded
            assertEquals(OK.name(), logbookOperation.get(RESULTS).get(0)
                .get(LogbookMongoDbName.getLogbookMongoDbName(LogbookParameterName.outcome).getDbname()).asText());

            assertThat(logbookOperation.get(RESULTS).get(0).get("evParentId")).isExactlyInstanceOf(NullNode.class);

            // assert certain parameters in the master are not overloaded
            assertEquals(operationGuid.getId(),
                logbookOperation.get(RESULTS).get(0)
                    .get(LogbookMongoDbName
                        .getLogbookMongoDbName(LogbookParameterName.agentIdentifierApplicationSession).getDbname())
                    .asText());
            assertThat(logbookOperation.get(RESULTS).get(0)
                .get(LogbookMongoDbName.getLogbookMongoDbName(LogbookParameterName.agIdExt).getDbname()).asText()
                .contains(operationGuid.getId()));
            assertEquals(operationGuid.getId(),
                logbookOperation.get(RESULTS).get(0).get(
                    LogbookMongoDbName.getLogbookMongoDbName(LogbookParameterName.objectIdentifierIncome).getDbname())
                    .asText());
            assertEquals(1, logbookOperation.get(RESULTS).get(0).get("events").size());
            logbookOperation.get(RESULTS).get(0).get("events").forEach(event -> {
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
        prepareVitamSession(tenantId, "aName3", "Context_IT");
        IngestInternalClientFactory.getInstance().changeServerPort(runner.PORT_SERVICE_INGEST_INTERNAL);
        String operationId = VitamTestHelper.doIngest(tenantId, OK_OBIDIN_MESSAGE_IDENTIFIER);
        final GUID operationGuid = GUIDReader.getGUID(operationId);
        verifyOperation(operationId, OK);

        final AccessInternalClient accessClient = AccessInternalClientFactory.getInstance().getClient();
        JsonNode logbookOperation =
            accessClient.selectOperationById(operationGuid.getId())
                .toJsonNode();
        final JsonNode element = logbookOperation.get(RESULTS).get(0);

        assertEquals(element.get("obIdIn").asText(), "vitam");
        assertThat(element.get("evParentId")).isExactlyInstanceOf(NullNode.class);
    }

    @RunWithCustomExecutor
    @Test
    public void testIngestWithManifestIncorrectAlgorithm() throws Exception {
        prepareVitamSession(tenantId, "aName3", "Context_IT");
        IngestInternalClientFactory.getInstance().changeServerPort(runner.PORT_SERVICE_INGEST_INTERNAL);
        String operationId = VitamTestHelper.doIngest(tenantId, SIP_ALGO_INCORRECT_IN_MANIFEST);
        final GUID operationGuid = GUIDReader.getGUID(operationId);
        verifyOperation(operationId, KO);

        final AccessInternalClient accessClient = AccessInternalClientFactory.getInstance().getClient();
        JsonNode logbookOperation =
            accessClient.selectOperationById(operationGuid.getId())
                .toJsonNode();

        assertThat(logbookOperation.get(RESULTS).get(0).get("evParentId")).isExactlyInstanceOf(NullNode.class);
        logbookOperation.get(RESULTS).get(0).get("events").forEach(event -> {
            if (event.get("evType").asText().contains("CHECK_DATAOBJECTPACKAGE.CHECK_MANIFEST_DATAOBJECT_VERSION")) {
                assertThat(event.get("outDetail").textValue()).
                    contains("CHECK_DATAOBJECTPACKAGE.CHECK_MANIFEST_DATAOBJECT_VERSION.INVALIDE_ALGO.KO");
            }
        });



    }

    @RunWithCustomExecutor
    @Test
    public void testIngestWithCustodialHistoryFileContainingDataObjectReferenceId() throws Exception {
        prepareVitamSession(tenantId, "aName3", "Context_IT");
        IngestInternalClientFactory.getInstance().changeServerPort(runner.PORT_SERVICE_INGEST_INTERNAL);
        String operationId = VitamTestHelper.doIngest(tenantId, SIP_SIP_ALL_METADATA_WITH_CUSTODIALHISTORYFILE);
        verifyOperation(operationId, WARNING);

        // Try to check AU and custodialHistoryModel
        final MetaDataClient metadataClient = MetaDataClientFactory.getInstance().getClient();
        final JsonNode node = getArchiveUnitWithTitle(metadataClient,
            "Les ruines de la Grande Guerre. - Belleau. - Une tranchée près de la gare.", TITLE);

        final JsonNode result = node.get(RESULTS);
        assertNotNull(result);

        CustodialHistoryModel model =
            JsonHandler.getFromJsonNode(result.get(0).get("CustodialHistory"), CustodialHistoryModel.class);
        assertNotNull(model);
        String expectedTitleOfCustodialItem = "Ce champ est obligatoire";
        assertThat(model.getCustodialHistoryItem()).isEqualTo(Collections.singletonList(expectedTitleOfCustodialItem));

        RelatedObjectReferenceType relatedObjectReferenceType =
            JsonHandler.getFromJsonNode(result.get(0).get("RelatedObjectReference"), RelatedObjectReferenceType.class);
        assertNotNull(relatedObjectReferenceType);
        assertThat(relatedObjectReferenceType.getRequires().get(0).getRepositoryArchiveUnitPID()).isNotEqualTo("ID04");

        final String referenceGUID = model.getCustodialHistoryFile().getDataObjectReferenceId();
        // Check reference of custodialHistory
        SelectMultiQuery select;
        select = new SelectMultiQuery();
        select.addRoots(referenceGUID);
        final JsonNode jsonResponse = metadataClient.selectObjectGrouptbyId(select.getFinalSelect(), referenceGUID);
        RequestResponseOK<ObjectGroup> objectGroupResponse =
            JsonHandler.getFromJsonNode(jsonResponse, RequestResponseOK.class, ObjectGroup.class);
        assertThat(objectGroupResponse).isNotNull();

        DataObjectReference reference = model.getCustodialHistoryFile();
        assertNotNull(reference);
        String oldExpectedDataObjectReference = "ID22";
        assertThat(reference.getDataObjectReferenceId()).isNotEqualTo(oldExpectedDataObjectReference);
        assertNull(reference.getDataObjectGroupReferenceId());

    }

    @RunWithCustomExecutor
    @Test
    public void should_ingest_SIP_with_logbook() throws Exception {
        prepareVitamSession(tenantId, "aName3", "Context_IT");
        IngestInternalClientFactory.getInstance().changeServerPort(runner.PORT_SERVICE_INGEST_INTERNAL);
        String operationId = VitamTestHelper.doIngest(tenantId, SIP_WITH_LOGBOOK);
        verifyOperation(operationId, WARNING);

        try (MetaDataClient metadataClient = MetaDataClientFactory.getInstance().getClient();
            LogbookLifeCyclesClient logbookLifeCyclesClient = LogbookLifeCyclesClientFactory.getInstance()
                .getClient()) {
            JsonNode nodeForLogbookOG =
                getArchiveUnitWithTitle(metadataClient, "AU-with-logbook-for-sip-logbook-test", TITLE);
            JsonNode nodeForLogbookAU =
                getArchiveUnitWithTitle(metadataClient, "AU-for-logbook-test-with-logbook-au", TITLE);

            JsonNode objectGroupLifeCycle = logbookLifeCyclesClient
                .getRawObjectGroupLifeCycleById(nodeForLogbookOG.get(RESULTS).get(0).get("#object").asText());
            JsonNode unitLifeCycle = logbookLifeCyclesClient
                .getRawUnitLifeCycleById(nodeForLogbookAU.get(RESULTS).get(0).get("#id").asText());

            List<EventTypeModel> eventsOG =
                JsonHandler.getFromJsonNode(objectGroupLifeCycle.get("events"), LIST_TYPE_REFERENCE);
            List<EventTypeModel> eventsAU =
                JsonHandler.getFromJsonNode(unitLifeCycle.get("events"), LIST_TYPE_REFERENCE);

            assertThat(eventsOG.stream().filter(e -> e.getEventType().equals("LFC.EXTERNAL_LOGBOOK")).findFirst())
                .isNotEmpty();
            assertThat(eventsAU.stream().filter(e -> e.getEventType().equals("LFC.EXTERNAL_LOGBOOK")).findFirst())
                .isNotEmpty();
        }
    }

    @RunWithCustomExecutor
    @Test
    public void should_NOT_ingest_SIP_with_malformed_logbook() throws Exception {
        prepareVitamSession(tenantId, "aName3", "Context_IT");
        IngestInternalClientFactory.getInstance().changeServerPort(runner.PORT_SERVICE_INGEST_INTERNAL);
        String operationId = VitamTestHelper.doIngest(tenantId, SIP_WITH_MALFORMED_LOGBOOK);
        verifyOperation(operationId, KO);
    }

    @RunWithCustomExecutor
    @Test
    public void testIngestWithOrganizationDescriptiveMetadataFreeTagsThenOK() throws Exception {
        prepareVitamSession(tenantId, "aName", "Context_IT");
        IngestInternalClientFactory.getInstance().changeServerPort(runner.PORT_SERVICE_INGEST_INTERNAL);
        String operationId = VitamTestHelper.doIngest(tenantId, SIP_WITH_ORGANIZATION_METADATA_DESCRIPTION_FREE_TAGS);
        verifyOperation(operationId, WARNING);

        // Try to check AU and custodialHistoryModel
        final MetaDataClient metadataClient = MetaDataClientFactory.getInstance().getClient();
        final JsonNode node = getArchiveUnitWithTitle(metadataClient,
            "Chemin des Dames - Ce que fut le Monument d'Hurtebise", TITLE_FR);

        final JsonNode result = node.get(RESULTS);
        assertNotNull(result);

        String identifierType = result.get(0).get("OriginatingAgency").get("Identifier").asText();
        assertEquals(identifierType, "RATP");
        assertThat(
            result.get(0).get("OriginatingAgency").get("OrganizationDescriptiveMetadata").get("DescriptionOA").get(0)
                .asText()).isEqualTo("La RATP est un établissement public");
    }

    @RunWithCustomExecutor
    @Test
    public void testIngestWithObjectAttachementWithDifferentOriginatingThenKO() throws Exception {
        prepareVitamSession(tenantId, "aName", "Context_IT");
        IngestInternalClientFactory.getInstance().changeServerPort(runner.PORT_SERVICE_INGEST_INTERNAL);
        String operationId = VitamTestHelper.doIngest(tenantId, SIP_ATTACHMENT_WITH_OBJECT);
        verifyOperation(operationId, WARNING);

        // Try to check AU
        final MetaDataClient metadataClient = MetaDataClientFactory.getInstance().getClient();
        final JsonNode node = getArchiveUnitWithTitle(metadataClient,
            "Annuaire_projet.pdf", TITLE);

        final JsonNode result = node.get(RESULTS);
        assertNotNull(result);
        final JsonNode unit = result.get(0);
        assertNotNull(unit);
        String unitId = unit.get("#id").asText();


        String zipName = GUIDFactory.newGUID().toString() + ".zip";
        String LINK_AU_TO_GOT_KO_SP = "integration-ingest-internal/LINK_AU_TO_GOT_KO_SP";
        String ZIP_LINK_AU_TO_GOT_KO_SP = "integration-ingest-internal/data";
        // prepare zip
        putSystemIdInManifest(LINK_AU_TO_GOT_KO_SP + "/manifest.xml",
            "(?<=<SystemId>).*?(?=</SystemId>)",
            unitId);
        zipFolder(PropertiesUtils.getResourcePath(LINK_AU_TO_GOT_KO_SP),
            PropertiesUtils.getResourcePath(ZIP_LINK_AU_TO_GOT_KO_SP).toAbsolutePath().toString() +
                "/" + zipName);

        String SIP_TO_ATTACH = ZIP_LINK_AU_TO_GOT_KO_SP + "/" + zipName;


        prepareVitamSession(tenantId, "aName", "Context_IT");
        IngestInternalClientFactory.getInstance().changeServerPort(runner.PORT_SERVICE_INGEST_INTERNAL);
        String operationIdAttachement = VitamTestHelper.doIngest(tenantId, SIP_TO_ATTACH);
        GUID operationGuidAttachement = GUIDReader.getGUID(operationIdAttachement);
        verifyOperation(operationIdAttachement, KO);

        final AccessInternalClient accessClient = AccessInternalClientFactory.getInstance().getClient();
        JsonNode logbookOperation =
            accessClient.selectOperationById(operationGuidAttachement.getId())
                .toJsonNode();
        boolean checkKOLinkingSuccess = true;
        final JsonNode elmt = logbookOperation.get(RESULTS).get(0);
        assertThat(elmt.get("evParentId")).isExactlyInstanceOf(NullNode.class);
        final List<Document> logbookOperationEvents =
            (List<Document>) new LogbookOperation(elmt).get(LogbookDocument.EVENTS.toString());
        for (final Document event : logbookOperationEvents) {
            if (KO.toString()
                .equals(event.get(LogbookMongoDbName.outcome.getDbname()).toString()) &&
                event.get(LogbookMongoDbName.outcomeDetail.getDbname())
                    .equals("CHECK_DATAOBJECTPACKAGE.CHECK_MANIFEST.SUBTASK_UNAUTHORIZED_ATTACHMENT_BY_BAD_SP.KO")) {
                checkKOLinkingSuccess = false;
                break;
            }
        }
        assertFalse(checkKOLinkingSuccess);

        try {
            Files.delete(new File(
                PropertiesUtils.getResourcePath(ZIP_LINK_AU_TO_GOT_KO_SP).toAbsolutePath().toString() + "/" + zipName)
                .toPath());
        } catch (Exception e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testIngestWithProfileRNGWithoutPathThenKO() throws Exception {
        prepareVitamSession(tenantId, "aName", "Context_IT");

        AdminManagementClient mgtClient = AdminManagementClientFactory.getInstance().getClient();
        RequestResponseOK<ProfileModel> response =
            (RequestResponseOK<ProfileModel>) mgtClient.findProfiles(new Select().getFinalSelect());
        ProfileModel profile = response.getResults().get(0);

        // update the existing profile path to provoke a KO ingest
        Bson filter = Filters.eq(ProfileModel.TAG_IDENTIFIER, profile.getIdentifier());
        Bson update = Updates.unset(ProfileModel.TAG_PATH);
        UpdateResult updateResult = FunctionalAdminCollections.PROFILE.getCollection().updateOne(filter, update);
        assertEquals(updateResult.getModifiedCount(), 1);

        IngestInternalClientFactory.getInstance().changeServerPort(runner.PORT_SERVICE_INGEST_INTERNAL);
        String operationId = VitamTestHelper.doIngest(tenantId, SIP_WITH_ARCHIVE_PROFILE);
        final GUID operationGuid= GUIDReader.getGUID(operationId);
        verifyOperation(operationId, KO);

        final AccessInternalClient accessClient = AccessInternalClientFactory.getInstance().getClient();
        JsonNode logbookOperation =
            accessClient.selectOperationById(operationGuid.getId())
                .toJsonNode();
        assertThat(logbookOperation.get(RESULTS).get(0).get("evParentId")).isExactlyInstanceOf(NullNode.class);
        logbookOperation.get(RESULTS).get(0).get("events").forEach(event -> {
            if (event.get("evType").asText().contains("CHECK_HEADER.CHECK_ARCHIVEPROFILE")) {
                assertThat(event.get("outDetail").textValue()).
                    contains("CHECK_HEADER.CHECK_ARCHIVEPROFILE.KO");
            }
        });

        Bson filterAfterUpdate = Filters.eq(ProfileModel.TAG_IDENTIFIER, profile.getIdentifier());
        Bson updateAfterTest = Updates.set(ProfileModel.TAG_PATH, profile.getPath());
        UpdateResult updateResultAfter =
            FunctionalAdminCollections.PROFILE.getCollection().updateOne(filterAfterUpdate, updateAfterTest);
        assertEquals(updateResultAfter.getModifiedCount(), 1);
    }

    private void putSystemIdInManifest(String targetFilename, String textToReplace, String replacementText)
        throws IOException {
        Path path = PropertiesUtils.getResourcePath(targetFilename);

        String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        content = content.replaceAll(textToReplace, replacementText);
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
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

    private JsonNode getArchiveUnitWithTitle(MetaDataClient metadataClient, String name, String titleType)
        throws Exception {
        SelectMultiQuery select = new SelectMultiQuery();
        select.addQueries(QueryHelper.eq(titleType, name));
        return metadataClient.selectUnits(select.getFinalSelect());
    }
}
