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
package fr.gouv.vitam.functional.administration.core.rules;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.flipkart.zjsonpatch.JsonDiff;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.SchemaValidationException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.FileRulesModel;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.server.application.configuration.DbConfigurationImpl;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.CollectionBackupModel;
import fr.gouv.vitam.functional.administration.common.FileRules;
import fr.gouv.vitam.functional.administration.common.ReportConstants;
import fr.gouv.vitam.functional.administration.common.config.ElasticsearchFunctionalAdminIndexManager;
import fr.gouv.vitam.functional.administration.common.counter.SequenceType;
import fr.gouv.vitam.functional.administration.common.counter.VitamCounterService;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesCsvException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesDeleteException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesDurationException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesReadException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialImportInProgressException;
import fr.gouv.vitam.functional.administration.common.server.ElasticsearchAccessFunctionalAdmin;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollectionsTestUtils;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.functional.administration.core.backup.FunctionalBackupService;
import fr.gouv.vitam.functional.administration.core.backup.RestoreBackupService;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.bson.Document;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static fr.gouv.vitam.common.model.administration.RuleMeasurementEnum.MONTH;
import static fr.gouv.vitam.common.model.administration.RuleMeasurementEnum.YEAR;
import static fr.gouv.vitam.common.model.administration.RuleType.AccessRule;
import static fr.gouv.vitam.common.model.administration.RuleType.AppraisalRule;
import static fr.gouv.vitam.common.model.administration.RuleType.HoldRule;
import static fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections.RULES;
import static fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminFactory.create;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Warning : To avoid error on import rules (actually we cannot update) and to be able to test each case, the tenant ID
 * is changed for each call.
 */
public class RulesManagerFileImplTest {

    public static final int RULE_AUDIT_THREAD_POOL_SIZE = 4;
    private static final String PREFIX = GUIDFactory.newGUID().getId();
    private static final String ACC_00003 = "ACC-00003";
    private static final String HOL_00001 = "HOL-00001";
    private static final String HOL_00002 = "HOL-00002";
    private static final String HOL_00003 = "HOL-00003";
    private static final String FILE_TO_TEST_OK = "jeu_ok.csv";
    private static final String FILE_DURATION_EXCEED = "regle_test_duration.csv";
    private static final String FILE_TO_TEST_KO = "jeu_donnees_KO_regles_CSV_DuplicatedReference.csv";
    private static final String FILE_TO_TEST_KO_EMPTY_LINE = "jeu_donnees_KO_regles_CSV_empty_line.csv";
    private static final String FILE_TO_TEST_RULES_DURATION_KO = "jeu_donnees_KO_regles_CSV_test.csv";
    private static final String FILE_TO_COMPARE = "jeu_donnees_OK_regles_CSV.csv";
    private static final String FILE_UPDATE_RULE_TYPE = "jeu_donnees_OK_regles_CSV_update_ruleType.csv";
    private static final String FILE_UPDATE_RULE_DURATION = "jeu_donnees_OK_regles_CSV_update_ruleDuration.csv";
    private static final String FILE_UPDATE_RULE_DESC = "jeu_donnees_OK_regles_CSV_update_ruleDesc.csv";
    private static final String FILE_DELETE_RULE = "jeu_donnees_OK_regles_CSV_delete_rule.csv";
    private static final String FILE_TO_TEST_KO_INVALID_FORMAT = "jeu_donnees_KO_regles_CSV_invalid_format.csv";
    private static final String FILE_TO_TEST_KO_MISSING_COLUMN = "jeu_donnees_KO_regles_CSV_missing_column.csv";
    private static final String FILE_TO_TEST_KO_INVALID_ENCODING = "jeu_donnees_OK_regles_CSV_regles_latin3-1.csv";
    private static final String FILE_TO_TEST_KO_HOLDRULE_DURATION_WITHOUT_MEASUREMENT =
        "jeu_donnees_KO_regles_CSV_HoldRuleWithDurationAndNoMeasure.csv";
    private static final String FILE_TO_TEST_KO_HOLDRULE_MEASUREMENT_WITHOUT_DURATION =
        "jeu_donnees_KO_regles_CSV_HoldRuleWithMeasureAndNoDuration.csv";
    private static final String STP_IMPORT_RULES = "STP_IMPORT_RULES";
    private static final Integer TENANT_ID = 0;
    private static final ElasticsearchFunctionalAdminIndexManager indexManager =
        FunctionalAdminCollectionsTestUtils.createTestIndexManager();
    private static final List<MongoDbNode> nodes = new ArrayList<>();

    @ClassRule
    public static MongoRule mongoRule = new MongoRule(MongoDbAccess.getMongoClientSettingsBuilder());

    @ClassRule
    public static ElasticsearchRule elasticsearchRule = new ElasticsearchRule();

    @ClassRule
    public static TemporaryFolder temporaryFolder = new TemporaryFolder();

    static RulesManagerFileImpl rulesFileManager;
    private static VitamCounterService vitamCounterService;
    private static VitamRuleService vitamRuleService;
    private static MongoDbAccessAdminImpl dbImpl;

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Mock
    private LogbookOperationsClientFactory logbookOperationsClientFactory;

    @Mock
    private LogbookOperationsClient logbookOperationsClient;

    @Mock
    private MetaDataClientFactory metaDataClientFactory;

    @Mock
    private MetaDataClient metaDataClient;

    @Mock
    private WorkspaceClientFactory workspaceClientFactory;

    @Mock
    private WorkspaceClient workspaceClient;

    @Mock
    private ProcessingManagementClientFactory processingManagementClientFactory;

    @Mock
    private ProcessingManagementClient processingManagementClient;

    @Mock
    private FunctionalBackupService functionalBackupService;

    @Mock
    private RestoreBackupService restoreBackupService;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        final List<ElasticsearchNode> esNodes = new ArrayList<>();
        esNodes.add(new ElasticsearchNode(ElasticsearchRule.getHost(), ElasticsearchRule.getPort()));

        FunctionalAdminCollectionsTestUtils.beforeTestClass(
            mongoRule.getMongoDatabase(),
            PREFIX,
            new ElasticsearchAccessFunctionalAdmin(ElasticsearchRule.VITAM_CLUSTER, esNodes, indexManager)
        );

        File tempFolder = temporaryFolder.newFolder();
        System.setProperty("vitam.tmp.folder", tempFolder.getAbsolutePath());
        SystemPropertyUtil.refresh();

        nodes.add(new MongoDbNode("localhost", MongoRule.getDataBasePort()));

        LogbookOperationsClientFactory.changeMode(null);
        dbImpl = create(
            new DbConfigurationImpl(nodes, mongoRule.getMongoDatabase().getName()),
            Collections::emptyList,
            indexManager
        );
        Integer[] tenantsList = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14 };
        List<Integer> tenants = new ArrayList<>(Arrays.asList(tenantsList));
        vitamRuleService = getRuleDurationConfigration(tenants);
        vitamCounterService = new VitamCounterService(dbImpl, tenants, null);
    }

    @AfterClass
    public static void tearDownAfterClass() {
        FunctionalAdminCollectionsTestUtils.afterTestClass(true);
    }

    private static VitamRuleService getRuleDurationConfigration(List<Integer> tenants) {
        Map<Integer, Map<String, String>> durationList = new HashMap<>();
        Map<String, String> duration = new HashMap<>();
        duration.put(AppraisalRule.name(), "6 day");
        duration.put(AccessRule.name(), "5");
        for (Integer tenant : tenants) {
            durationList.put(tenant, duration);
        }
        return new VitamRuleService(durationList);
    }

    @Before
    public void setUp() {
        MongoDbAccessAdminImpl dbAccess = create(
            new DbConfigurationImpl(nodes, mongoRule.getMongoDatabase().getName()),
            Collections::emptyList,
            indexManager
        );

        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);
        when(metaDataClientFactory.getClient()).thenReturn(metaDataClient);
        when(logbookOperationsClientFactory.getClient()).thenReturn(logbookOperationsClient);
        when(processingManagementClientFactory.getClient()).thenReturn(processingManagementClient);

        rulesFileManager = new RulesManagerFileImpl(
            dbAccess,
            vitamCounterService,
            functionalBackupService,
            logbookOperationsClientFactory,
            metaDataClientFactory,
            processingManagementClientFactory,
            workspaceClientFactory,
            vitamRuleService,
            RULE_AUDIT_THREAD_POOL_SIZE,
            restoreBackupService
        );
        FunctionalAdminCollectionsTestUtils.afterTestClass(false);
        FunctionalAdminCollectionsTestUtils.resetVitamSequenceCounter();
    }

    /**
     * Warning : To avoid error on import rules (actually we cannot update) and to be able to test each case, the tenant
     * ID is changed for each call.
     */
    @Test
    @RunWithCustomExecutor
    public void testimportRulesFile() throws Exception {
        int tenantId = 0;
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        when(logbookOperationsClient.selectOperation(any())).thenReturn(getJsonResult(STP_IMPORT_RULES, tenantId));
        try {
            Map<String, FileRulesModel> rulesToImport = rulesFileManager.getRulesFromCSV(
                new FileInputStream(PropertiesUtils.findFile(FILE_TO_TEST_OK))
            );
            rulesFileManager.checkFile(rulesToImport);
        } catch (final FileRulesException | FileRulesDeleteException e) {
            fail("Check file with FILE_TO_TEST_OK should not throw exception");
        }

        assertThrows(FileRulesCsvException.class, () -> {
            Map<String, FileRulesModel> rulesToImport = rulesFileManager.getRulesFromCSV(
                new FileInputStream(PropertiesUtils.findFile(FILE_TO_TEST_KO))
            );
            rulesFileManager.checkFile(rulesToImport);
        });

        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(tenantId));
        rulesFileManager.importFile(new FileInputStream(PropertiesUtils.findFile(FILE_TO_TEST_OK)), FILE_TO_TEST_OK);

        Select selectTenant = new Select();
        selectTenant.setQuery(eq("#tenant", tenantId));
        List<FileRules> fileRules = convertResponseResultToFileRules(
            rulesFileManager.findDocuments(selectTenant.getFinalSelect())
        );
        assertEquals(25, fileRules.size());

        final Select select = new Select();
        select.setQuery(eq("RuleId", "APP-00005"));
        final RequestResponseOK<FileRules> fileList = rulesFileManager.findDocuments(select.getFinalSelect());
        final String id = fileList.getResults().get(0).getString("RuleId");
        final FileRules file = rulesFileManager.findDocumentById(id);

        assertEquals(file.getRuleid(), fileList.getResults().get(0).getRuleid());

        assertThat(file.getVersion()).isEqualTo(1);

        final FileRules holdRule1 = rulesFileManager.findDocumentById("HOL-00001");
        assertThat(holdRule1.getRuletype()).isEqualTo(HoldRule);
        assertThat(holdRule1.getRuleduration()).isNull();
        assertThat(holdRule1.getRulemeasurement()).isNull();

        final FileRules holdRule2 = rulesFileManager.findDocumentById("HOL-00002");
        assertThat(holdRule2.getRuletype()).isEqualTo(HoldRule);
        assertThat(holdRule2.getRuleduration()).isEqualTo("5");
        assertThat(holdRule2.getRulemeasurement()).isEqualTo(YEAR);
    }

    @Test
    @RunWithCustomExecutor
    public void testimportRulesFileWithEmptyLine() throws Exception {
        int tenantId = 12;
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        when(logbookOperationsClient.selectOperation(any())).thenReturn(getJsonResult(STP_IMPORT_RULES, tenantId));

        assertThatThrownBy(
            () ->
                rulesFileManager.getRulesFromCSV(
                    new FileInputStream(PropertiesUtils.findFile(FILE_TO_TEST_KO_EMPTY_LINE))
                )
        )
            .isInstanceOf(FileRulesCsvException.class)
            .hasMessageContaining("Invalid CSV File");
    }

    @Test(expected = FileRulesDurationException.class)
    @RunWithCustomExecutor
    public void testImportRuleDurationExceed() throws Exception {
        int tenantId = 1;
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        when(logbookOperationsClient.selectOperation(any())).thenReturn(getJsonResult(STP_IMPORT_RULES, tenantId));

        try {
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(tenantId));
            rulesFileManager.importFile(
                new FileInputStream(PropertiesUtils.findFile(FILE_DURATION_EXCEED)),
                FILE_DURATION_EXCEED
            );
        } catch (final FileRulesException | FileRulesDeleteException e) {
            fail("Check file with FILE_TO_TEST_OK should not throw exception");
        }
    }

    /**
     * Warning : To avoid error on import rules (actually we cannot update) and to be able to test each case, the tenant
     * ID is changed for each call.
     */
    @Test
    @RunWithCustomExecutor
    public void testNoImportInProgess() throws Exception {
        int tenantId = 2;
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);

        when(metaDataClient.selectUnits(any())).thenReturn(new RequestResponseOK<JsonNode>().toJsonNode());

        when(logbookOperationsClient.selectOperation(any())).thenReturn(getJsonResult(STP_IMPORT_RULES, tenantId));
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(tenantId));
        rulesFileManager.importFile(new FileInputStream(PropertiesUtils.findFile(FILE_TO_TEST_OK)), FILE_TO_TEST_OK);

        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        when(logbookOperationsClient.selectOperation(any())).thenReturn(getEmptyJsonResponse());
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(tenantId));

        assertThatCode(
            () ->
                rulesFileManager.importFile(
                    new FileInputStream(PropertiesUtils.findFile(FILE_TO_TEST_OK)),
                    FILE_TO_TEST_OK
                )
        ).doesNotThrowAnyException();
    }

    /**
     * Warning : To avoid error on import rules (actually cannot update) and to be able to test each case, the tenant ID
     * is changed for each call.
     */
    @Test(expected = ReferentialImportInProgressException.class)
    @RunWithCustomExecutor
    public void testImportInProgess() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(3);
        when(logbookOperationsClient.selectOperation(any())).thenReturn(getJsonResult("COMMIT_RULES", 60));
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(TENANT_ID));
        rulesFileManager.importFile(new FileInputStream(PropertiesUtils.findFile(FILE_TO_TEST_OK)), FILE_TO_TEST_OK);
    }

    private JsonNode getJsonResult(String evType, int tenantId) throws Exception {
        return JsonHandler.getFromString(
            String.format(
                "{\n" +
                "     \"httpCode\": 200,\n" +
                "     \"$hits\": {\n" +
                "          \"total\": 1,\n" +
                "          \"offset\": 0,\n" +
                "          \"limit\": 1,\n" +
                "          \"size\": 1\n" +
                "     },\n" +
                "     \"$results\": [\n" +
                "          {\n" +
                "               \"_id\": \"aecaaaaaacgbcaacaa76eak44s3of6iaaaaq\",\n" +
                "               \"events\": [\n" +
                "                    {\n" +
                "                         \"evType\": \"%s\"\n" +
                "                    }\n" +
                "               ],\n" +
                "               \"_v\": 0,\n" +
                "               \"_tenant\": %d\n" +
                "          }\n" +
                "     ],\n" +
                "     \"$context\": {\n" +
                "          \"$query\": {\n" +
                "               \"$eq\": {\n" +
                "                    \"events.evType\": \"STP_IMPORT_RULES\"\n" +
                "               }\n" +
                "          },\n" +
                "          \"$filter\": {\n" +
                "               \"$limit\": 1,\n" +
                "               \"$orderby\": {\n" +
                "                    \"evDateTime\": -1\n" +
                "               }\n" +
                "          },\n" +
                "          \"$projection\": {\n" +
                "               \"$fields\": {\n" +
                "                    \"#id\": 1,\n" +
                "                    \"events.evType\": 1\n" +
                "               }\n" +
                "          }\n" +
                "     }\n" +
                "}",
                evType,
                tenantId
            )
        );
    }

    private JsonNode getEmptyJsonResponse() throws Exception {
        return JsonHandler.getFromString(
            "{\n" +
            "     \"httpCode\": 200,\n" +
            "     \"$hits\": {\n" +
            "          \"total\": 0,\n" +
            "          \"offset\": 0,\n" +
            "          \"limit\": 1,\n" +
            "          \"size\": 0\n" +
            "     },\n" +
            "     \"$results\": [],\n" +
            "     \"$context\": {\n" +
            "          \"$query\": {\n" +
            "               \"$eq\": {\n" +
            "                    \"events.evType\": \"STP_IMPORT_RULES\"\n" +
            "               }\n" +
            "          },\n" +
            "          \"$filter\": {\n" +
            "               \"$limit\": 1,\n" +
            "               \"$orderby\": {\n" +
            "                    \"evDateTime\": -1\n" +
            "               }\n" +
            "          },\n" +
            "          \"$projection\": {\n" +
            "               \"$fields\": {\n" +
            "                    \"#id\": 1,\n" +
            "                    \"events.outcome\": 1\n" +
            "               }\n" +
            "          }\n" +
            "     }\n" +
            "}"
        );
    }

    @Test
    @RunWithCustomExecutor
    public void shouldImportHoldRulesWithoutPeriodDurationAndPeriodMeasurement() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(13);
        final InputStream inputStream = getInputStreamAndInitialiseMockWhenCheckRulesFile(
            FILE_TO_TEST_KO_HOLDRULE_MEASUREMENT_WITHOUT_DURATION
        );

        // Then
        final JsonNode errorReportAtJson = JsonHandler.getFromInputStream(inputStream);
        final JsonNode errorNode = errorReportAtJson.get("error");
        ArrayNode errorLine = (ArrayNode) errorNode.get("line 2");

        assertThat(errorLine.size()).isNotEqualTo(25);
        assertThat(errorLine.size()).isEqualTo(1);
        assertThat(errorLine.get(0).get("Code").asText()).contains("STP_IMPORT_RULES_NOT_CSV_FORMAT.KO");
        assertThat(errorLine.get(0).get("Message").asText()).contains("Le fichier importé n'est pas au format CSV");
        assertThat(errorLine.get(0).get("Information additionnelle").asText()).contains("RuleDuration");
    }

    @Test
    @RunWithCustomExecutor
    public void shouldRejectHoldRulesWithPeriodDurationWithoutPeriodMeasurement() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(14);
        final InputStream inputStream = getInputStreamAndInitialiseMockWhenCheckRulesFile(
            FILE_TO_TEST_KO_HOLDRULE_DURATION_WITHOUT_MEASUREMENT
        );

        // Then
        final JsonNode errorReportAtJson = JsonHandler.getFromInputStream(inputStream);
        final JsonNode errorNode = errorReportAtJson.get("error");
        ArrayNode errorLine = (ArrayNode) errorNode.get("line 2");

        assertThat(errorLine.size()).isNotEqualTo(25);
        assertThat(errorLine.size()).isEqualTo(1);
        assertThat(errorLine.get(0).get("Code").asText()).contains("STP_IMPORT_RULES_NOT_CSV_FORMAT.KO");
        assertThat(errorLine.get(0).get("Message").asText()).contains("Le fichier importé n'est pas au format CSV");
        assertThat(errorLine.get(0).get("Information additionnelle").asText()).contains("RuleMeasurement");
    }

    @Test
    @RunWithCustomExecutor
    public void shouldInsertUpdateAndDeleteNewFileRules() throws Exception {
        int tenantId = 4;
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        final Select select = new Select();

        when(logbookOperationsClient.selectOperation(any())).thenReturn(getJsonResult(STP_IMPORT_RULES, tenantId));
        when(metaDataClient.selectUnits(any())).thenReturn(new RequestResponseOK<JsonNode>().toJsonNode());

        try {
            List<FileRules> fileRules = new ArrayList<>();
            try {
                select.setQuery(eq("#tenant", tenantId));
                fileRules = convertResponseResultToFileRules(rulesFileManager.findDocuments(select.getFinalSelect()));
            } catch (ReferentialException e) {}
            if (fileRules.size() == 0) {
                VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(tenantId));
                rulesFileManager.importFile(
                    new FileInputStream(PropertiesUtils.findFile(FILE_TO_TEST_OK)),
                    FILE_TO_TEST_OK
                );
            }
            List<FileRules> fileRulesAfterImport = convertResponseResultToFileRules(
                rulesFileManager.findDocuments(select.getFinalSelect())
            );
            assertEquals(25, fileRulesAfterImport.size());
            // FILE_TO_COMPARE => insert 1 rule, delete 1 rule, update 1 rule
            rulesFileManager.importFile(
                new FileInputStream(PropertiesUtils.findFile(FILE_TO_COMPARE)),
                FILE_TO_COMPARE
            );
            List<FileRules> fileRulesAfterInsert = convertResponseResultToFileRules(
                rulesFileManager.findDocuments(select.getFinalSelect())
            );
            assertEquals(25, fileRulesAfterInsert.size());
        } catch (
            ReferentialException | InvalidParseOperationException | IOException | InvalidCreateOperationException e
        ) {
            fail("ReferentialException " + e.getCause());
        }
    }

    @Test
    @RunWithCustomExecutor
    public void shouldUpdateRulesType() throws DatabaseException, ReferentialException, SchemaValidationException {
        int tenantId = 5;
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        MongoDbAccessAdminImpl dbAccess = create(
            new DbConfigurationImpl(nodes, mongoRule.getMongoDatabase().getName()),
            Collections::emptyList,
            indexManager
        );
        dbAccess.deleteCollectionForTesting(FunctionalAdminCollections.RULES);
        final Select select = new Select();
        try {
            when(logbookOperationsClient.selectOperation(any())).thenReturn(getJsonResult(STP_IMPORT_RULES, tenantId));
            when(metaDataClient.selectUnits(any())).thenReturn(new RequestResponseOK<JsonNode>().toJsonNode());

            List<FileRules> fileRules = new ArrayList<>();
            try {
                select.setQuery(eq("#tenant", tenantId));
                fileRules = convertResponseResultToFileRules(rulesFileManager.findDocuments(select.getFinalSelect()));
            } catch (ReferentialException e) {}
            if (fileRules.size() == 0) {
                VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(TENANT_ID));
                rulesFileManager.importFile(
                    new FileInputStream(PropertiesUtils.findFile(FILE_TO_TEST_OK)),
                    FILE_TO_TEST_OK
                );
            }
            List<FileRules> fileRulesAfterImport = convertResponseResultToFileRules(
                rulesFileManager.findDocuments(select.getFinalSelect())
            );
            assertEquals(25, fileRulesAfterImport.size());
            for (FileRules fileRulesAfter : fileRulesAfterImport) {
                if (ACC_00003.equals(fileRulesAfter.getRuleid())) {
                    assertEquals(AccessRule, fileRulesAfter.getRuletype());
                }
            }
            // FILE_TO_COMPARE => insert 1 rule, delete 1 rule, update 1 rule
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(TENANT_ID));
            rulesFileManager.importFile(
                new FileInputStream(PropertiesUtils.findFile(FILE_UPDATE_RULE_TYPE)),
                FILE_UPDATE_RULE_TYPE
            );
            List<FileRules> fileRulesAfterInsert = convertResponseResultToFileRules(
                rulesFileManager.findDocuments(select.getFinalSelect())
            );
            assertEquals(26, fileRulesAfterInsert.size());
            for (FileRules fileRulesUpdated : fileRulesAfterInsert) {
                if (ACC_00003.equals(fileRulesUpdated.getRuleid())) {
                    assertEquals(AppraisalRule, fileRulesUpdated.getRuletype());
                }
                if ("APP-00006".equals(fileRulesUpdated.getRuleid())) {
                    assertEquals(AppraisalRule, fileRulesUpdated.getRuletype());
                }
            }
        } catch (
            ReferentialException
            | InvalidParseOperationException
            | IOException
            | InvalidCreateOperationException
            | LogbookClientException e
        ) {
            fail("ReferentialException " + e.getCause());
            e.printStackTrace();
        } catch (Exception e) {
            fail("Exception " + e.getCause());
            e.printStackTrace();
        }
    }

    @Test
    @RunWithCustomExecutor
    public void shouldDoNothingOnFileRulesReferential() throws Exception {
        int tenantId = 6;
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);

        final Select select = new Select();

        when(logbookOperationsClient.selectOperation(any())).thenReturn(getJsonResult(STP_IMPORT_RULES, tenantId));
        when(metaDataClient.selectUnits(any())).thenReturn(new RequestResponseOK<JsonNode>().toJsonNode());

        try {
            select.setQuery(eq("#tenant", tenantId));
            List<FileRules> fileRules = convertResponseResultToFileRules(
                rulesFileManager.findDocuments(select.getFinalSelect())
            );

            if (fileRules.size() == 0) {
                VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(tenantId));
                rulesFileManager.importFile(
                    new FileInputStream(PropertiesUtils.findFile(FILE_TO_TEST_OK)),
                    FILE_TO_TEST_OK
                );
            }
            List<FileRules> fileRulesAfterImport = convertResponseResultToFileRules(
                rulesFileManager.findDocuments(select.getFinalSelect())
            );
            assertEquals(25, fileRulesAfterImport.size());
            rulesFileManager.importFile(
                new FileInputStream(PropertiesUtils.findFile(FILE_TO_TEST_OK)),
                FILE_TO_TEST_OK
            );
            List<FileRules> fileRulesAfterInsert = convertResponseResultToFileRules(
                rulesFileManager.findDocuments(select.getFinalSelect())
            );
            assertEquals(25, fileRulesAfterInsert.size());
            assertEquals(
                fileRulesAfterInsert.stream().findAny().get().get(VitamFieldsHelper.version()),
                vitamCounterService.getSequence(tenantId, SequenceType.RULES_SEQUENCE)
            );
        } catch (
            ReferentialException | InvalidParseOperationException | IOException | InvalidCreateOperationException e
        ) {
            e.printStackTrace();
            fail("ReferentialException " + e.getCause());
        }
    }

    private List<FileRules> convertResponseResultToFileRules(RequestResponseOK<FileRules> response) {
        if (response != null) {
            return response.getResults();
        } else {
            return new ArrayList<>();
        }
    }

    @Test
    @RunWithCustomExecutor
    public void should_retrieve_FileRulesCsvException_when_csv_with_bad_format_is_upload() throws Exception {
        int tenantId = 7;
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(tenantId));

        // mock Storage
        when(metaDataClient.selectUnits(any())).thenReturn(new RequestResponseOK<JsonNode>().toJsonNode());
        when(logbookOperationsClient.selectOperation(any())).thenReturn(getJsonResult(STP_IMPORT_RULES, tenantId));

        assertThatThrownBy(
            () ->
                rulesFileManager.importFile(
                    new FileInputStream(PropertiesUtils.findFile(FILE_TO_TEST_KO)),
                    FILE_TO_TEST_KO
                )
        )
            .isInstanceOf(FileRulesCsvException.class)
            .hasMessageContaining("Invalid CSV File");
    }

    @Test
    @RunWithCustomExecutor
    public void shouldRetrieveIOExceptionWhenImportCsvWithBadEncodingFormatNotUTF8() throws Exception {
        int tenantId = 0;
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(tenantId));

        // mock Storage
        when(metaDataClient.selectUnits(any())).thenReturn(new RequestResponseOK<JsonNode>().toJsonNode());
        when(logbookOperationsClient.selectOperation(any())).thenReturn(getJsonResult(STP_IMPORT_RULES, tenantId));

        // prepare logbook operation capture
        final ArgumentCaptor<LogbookOperationParameters> logOpParamsCaptor = ArgumentCaptor.forClass(
            LogbookOperationParameters.class
        );
        doNothing().when(logbookOperationsClient).update(logOpParamsCaptor.capture());

        // when
        assertThatThrownBy(
            () ->
                rulesFileManager.importFile(
                    new FileInputStream(PropertiesUtils.findFile(FILE_TO_TEST_KO_INVALID_ENCODING)),
                    FILE_TO_TEST_KO_INVALID_ENCODING
                )
        ).isInstanceOf(IOException.class);

        List<LogbookOperationParameters> logbooks = logOpParamsCaptor.getAllValues();
        assertThat(logbooks).hasSize(2);
        assertThat(logbooks.get(0).getTypeProcess()).isEqualTo(LogbookTypeProcess.MASTERDATA);
        assertThat(logbooks.get(0).getStatus()).isEqualTo(StatusCode.KO);
        assertThat(logbooks.get(0).getParameterValue(LogbookParameterName.eventType)).isEqualTo("CHECK_RULES");
        assertThat(logbooks.get(0).getParameterValue(LogbookParameterName.outcomeDetail)).isEqualTo(
            "CHECK_RULES.INVALID_CSV_ENCODING_NOT_UTF_EIGHT.KO"
        );
        assertThat(logbooks.get(0).getParameterValue(LogbookParameterName.outcomeDetailMessage)).isEqualTo(
            "Échec du contrôle de la conformité du fichier des règles de gestion : fichier CSV n'est pas encodé en UTF8"
        );
        assertThat(logbooks.get(1).getTypeProcess()).isEqualTo(LogbookTypeProcess.MASTERDATA);
        assertThat(logbooks.get(1).getStatus()).isEqualTo(StatusCode.KO);
        assertThat(logbooks.get(1).getParameterValue(LogbookParameterName.eventType)).isEqualTo("STP_IMPORT_RULES");
        assertThat(logbooks.get(1).getParameterValue(LogbookParameterName.outcomeDetail)).isEqualTo(
            "STP_IMPORT_RULES.KO"
        );
        assertThat(logbooks.get(1).getParameterValue(LogbookParameterName.outcomeDetailMessage)).isEqualTo(
            "Échec du processus d'import du référentiel des règles de gestion"
        );

        // Given
        final InputStream reportInputStream =
            getInputStreamAndInitialiseMockWhenCheckRuleFileFailsInCharConverionException(
                FILE_TO_TEST_KO_INVALID_ENCODING
            );
        // When
        final JsonNode errorReportAtJson = JsonHandler.getFromInputStream(reportInputStream);
        final JsonNode errorNode = errorReportAtJson.get(ReportConstants.JDO_DISPLAY);

        // Then
        assertThat(errorNode.get("outMessg").asText()).isEqualTo(
            "Échec du processus d'import du référentiel des règles de gestion"
        );
        assertThat(errorNode.get("usedDeletedRules")).isNullOrEmpty();
        assertThat(errorNode.get("usedUpdatedRules")).isNullOrEmpty();
    }

    private InputStream getInputStreamAndInitialiseMockWhenCheckRuleFileFailsInCharConverionException(String filename)
        throws MetaDataDocumentSizeException, MetaDataExecutionException, InvalidParseOperationException, MetaDataClientServerException {
        // mock logbook client
        when(metaDataClient.selectUnits(any())).thenReturn(JsonHandler.createArrayNode());
        assertThatCode(
            () -> when(logbookOperationsClient.selectOperation(any())).thenReturn(getJsonResult(STP_IMPORT_RULES, 0))
        ).doesNotThrowAnyException();

        // When
        assertThatThrownBy(
            () -> rulesFileManager.getRulesFromCSV(new FileInputStream(PropertiesUtils.findFile(filename)))
        ).isInstanceOf(IOException.class);

        return rulesFileManager.generateReportContent(
            Collections.emptyMap(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            StatusCode.KO,
            null
        );
    }

    @Test
    @RunWithCustomExecutor
    public void should_not_duplicate_error_on_each_line_in_report_when_error_append() throws Exception {
        int tenantId = 8;
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        final InputStream inputStream = getInputStreamAndInitialiseMockWhenCheckRulesFile(
            FILE_TO_TEST_RULES_DURATION_KO
        );

        // Then
        final JsonNode errorReportAtJson = JsonHandler.getFromInputStream(inputStream);
        final JsonNode errorNode = errorReportAtJson.get("error");
        assertThat(errorNode.get("line 10").isArray());
        if (errorNode.get("line 10").isArray()) {
            final ArrayNode line10ArrayNode = (ArrayNode) errorNode.get("line 10");
            assertThat(line10ArrayNode.size()).isNotEqualTo(23);
            assertThat(line10ArrayNode.size()).isEqualTo(2);
            assertThat(line10ArrayNode.get(0).get("Code").asText()).contains("STP_IMPORT_RULES_RULEID_DUPLICATION.KO");
            assertThat(line10ArrayNode.get(0).get("Message").asText()).contains(
                "Il existe plusieurs fois le même RuleId. Ce RuleId doit être unique dans l'ensemble du référentiel"
            );
            assertThat(line10ArrayNode.get(0).get("Information additionnelle").asText()).contains("APP-00001");
        }
    }

    @Test
    @RunWithCustomExecutor
    public void should_contains_outMessg_in_error_report_when_csv_with_bad_format_is_upload() throws Exception {
        // Given
        // mock Storage
        final InputStream inputStream = getInputStreamAndInitialiseMockWhenCheckRulesFile(
            FILE_TO_TEST_RULES_DURATION_KO
        );
        // Then
        final JsonNode errorReportAtJson = JsonHandler.getFromInputStream(inputStream);
        final JsonNode errorNode = errorReportAtJson.get(ReportConstants.JDO_DISPLAY);

        assertThat(errorNode.get("outMessg").asText()).isEqualTo(
            "Échec du processus d'import du référentiel des règles de gestion"
        );
    }

    @Test
    @RunWithCustomExecutor
    public void should_contains_outMessg_in_error_report_when_csv_with_no_rule_type_is_upload() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(0);
        // mock Storage
        final InputStream inputStream = getInputStreamAndInitialiseMockWhenCheckRulesFile(
            FILE_TO_TEST_KO_INVALID_FORMAT
        );
        // Then
        final JsonNode errorReportAtJson = JsonHandler.getFromInputStream(inputStream);
        final JsonNode errorNode = errorReportAtJson.get(ReportConstants.JDO_DISPLAY);

        assertThat(errorNode.get("outMessg").asText()).isEqualTo(
            "Échec du processus d'import du référentiel des règles de gestion"
        );
    }

    @Test
    @RunWithCustomExecutor
    public void should_contains_report_in_error_report_when_csv_invalid() throws Exception {
        // Given
        // mock Storage
        final InputStream inputStream = getInputStreamAndInitialiseMockWhenCheckRulesFile(
            FILE_TO_TEST_KO_MISSING_COLUMN
        );
        // Then
        final JsonNode errorReportAtJson = JsonHandler.getFromInputStream(inputStream);
        final JsonNode operationNode = errorReportAtJson.get(ReportConstants.JDO_DISPLAY);
        final JsonNode errorNode = errorReportAtJson.get(ReportConstants.ERROR);
        final ArrayNode line2ArrayNode = (ArrayNode) errorNode.get("line 2");

        assertThat(operationNode.get("outMessg").asText()).isEqualTo(
            "Échec du processus d'import du référentiel des règles de gestion"
        );

        assertThat(line2ArrayNode.get(0).get("Code").asText()).contains("STP_IMPORT_RULES_NOT_CSV_FORMAT.KO");

        assertThat(line2ArrayNode.get(0).get("Message").asText()).contains(
            "Le fichier importé n'est pas au format CSV"
        );
    }

    @Test
    @RunWithCustomExecutor
    public void should_contains_outMessg_in_error_report_when_csv_with_missing_Column() throws Exception {
        // Given
        // mock Storage
        final InputStream inputStream = getInputStreamAndInitialiseMockWhenCheckRulesFile(
            FILE_TO_TEST_KO_MISSING_COLUMN
        );
        // Then
        final JsonNode errorReportAtJson = JsonHandler.getFromInputStream(inputStream);
        final JsonNode errorNode = errorReportAtJson.get(ReportConstants.JDO_DISPLAY);

        assertThat(errorNode.get("outMessg").asText()).isEqualTo(
            "Échec du processus d'import du référentiel des règles de gestion"
        );
    }

    private InputStream getInputStreamAndInitialiseMockWhenCheckRulesFile(String filename) throws Exception {
        // mock logbook client
        when(metaDataClient.selectUnits(any())).thenReturn(JsonHandler.createArrayNode());

        assertThatCode(
            () -> when(logbookOperationsClient.selectOperation(any())).thenReturn(getJsonResult(STP_IMPORT_RULES, 0))
        ).doesNotThrowAnyException();

        // When
        Throwable thrown = catchThrowable(() -> {
            Map<String, FileRulesModel> rulesFromCSV = rulesFileManager.getRulesFromCSV(
                new FileInputStream(PropertiesUtils.findFile(filename))
            );
            rulesFileManager.checkFile(rulesFromCSV);
        });
        assertThat(thrown).isInstanceOf(FileRulesReadException.class);

        return rulesFileManager.generateReportContent(
            ((FileRulesReadException) thrown).getErrorsMap(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            StatusCode.KO,
            null
        );
    }

    @Test
    @RunWithCustomExecutor
    public void should_contains_evid_and_outMessg_in_report_when_csv_with_notUsedUpdatedRules_is_upload()
        throws Exception {
        int tenantId = 9;
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        final Select select = new Select();
        // given
        when(logbookOperationsClient.selectOperation(any())).thenReturn(getJsonResult(STP_IMPORT_RULES, tenantId));

        when(metaDataClient.selectUnits(any())).thenReturn(new RequestResponseOK<JsonNode>().toJsonNode());

        File file = folder.newFolder();
        final Path report = Paths.get(file.getAbsolutePath(), "report.json");
        getInputStreamAndInitialiseMockWhenImportFileRules(report);
        // when
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(tenantId));
        rulesFileManager.importFile(new FileInputStream(PropertiesUtils.findFile(FILE_TO_TEST_OK)), FILE_TO_TEST_OK);
        // then
        verify(functionalBackupService, times(1)).saveFile(
            any(InputStream.class),
            any(GUID.class),
            anyString(),
            eq(DataCategory.REPORT),
            anyString()
        );
        final JsonNode fromFile = JsonHandler.getFromFile(report.toFile());
        final JsonNode jdoNode = fromFile.get(ReportConstants.JDO_DISPLAY);
        final String evId = jdoNode.get("evId").asText();
        final String outMessg = jdoNode.get("outMessg").asText();
        assertThat(evId).isNotEmpty();
        assertThat(outMessg).contains("Succès du processus d'import du référentiel des règles de gestion");

        // when
        List<FileRules> fileRulesAfterImport = convertResponseResultToFileRules(
            rulesFileManager.findDocuments(select.getFinalSelect())
        );
        assertEquals(25, fileRulesAfterImport.size());

        for (FileRules fileRulesAfter : fileRulesAfterImport) {
            if (ACC_00003.equals(fileRulesAfter.getRuleid())) {
                assertEquals(AccessRule, fileRulesAfter.getRuletype());
            }
        }

        final Path reportAfterUpdate = Paths.get(file.getAbsolutePath(), "reportAfterUpdate.json");
        getInputStreamAndInitialiseMockWhenImportFileRules(reportAfterUpdate);

        // FILE_TO_COMPARE => insert 1 rule, delete 1 rule, update 1 rule
        rulesFileManager.importFile(
            new FileInputStream(PropertiesUtils.findFile(FILE_UPDATE_RULE_TYPE)),
            FILE_UPDATE_RULE_TYPE
        );
        List<FileRules> fileRulesAfterInsert = convertResponseResultToFileRules(
            rulesFileManager.findDocuments(select.getFinalSelect())
        );
        assertEquals(26, fileRulesAfterInsert.size());

        final JsonNode reportAfterUpdateNode = JsonHandler.getFromFile(reportAfterUpdate.toFile());
        final JsonNode jdoAfterUpdateNode = reportAfterUpdateNode.get(ReportConstants.JDO_DISPLAY);
        final String evIdAfterUpdate = jdoAfterUpdateNode.get("evId").asText();
        final String outMessgAfterUpdate = jdoNode.get("outMessg").asText();
        assertThat(evIdAfterUpdate).isNotEmpty();
        assertThat(outMessgAfterUpdate).contains("Succès du processus d'import du référentiel des règles de gestion");

        for (FileRules fileRulesUpdated : fileRulesAfterInsert) {
            if (ACC_00003.equals(fileRulesUpdated.getRuleid())) {
                assertEquals(AppraisalRule, fileRulesUpdated.getRuletype());
            }
            if ("APP-00006".equals(fileRulesUpdated.getRuleid())) {
                assertEquals(AppraisalRule, fileRulesUpdated.getRuletype());
            }
        }
    }

    @Test
    @RunWithCustomExecutor
    public void should_assert_consistency_of_report_when_update_Rule_linked_to_au_append() throws Exception {
        int tenantId = 10;
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        final Select select = new Select();
        // given
        when(logbookOperationsClient.selectOperation(any())).thenReturn(getJsonResult(STP_IMPORT_RULES, tenantId));
        when(metaDataClient.selectUnits(any())).thenReturn(new RequestResponseOK<JsonNode>().toJsonNode());

        File file = folder.newFolder();
        final Path report = Paths.get(file.getAbsolutePath(), "report.json");
        getInputStreamAndInitialiseMockWhenImportFileRules(report);
        // when
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(tenantId));
        rulesFileManager.importFile(new FileInputStream(PropertiesUtils.findFile(FILE_TO_TEST_OK)), FILE_TO_TEST_OK);
        // then
        verify(functionalBackupService, times(1)).saveFile(
            any(InputStream.class),
            any(GUID.class),
            anyString(),
            eq(DataCategory.REPORT),
            anyString()
        );
        final JsonNode fromFile = JsonHandler.getFromFile(report.toFile());
        final JsonNode jdoNode = fromFile.get(ReportConstants.JDO_DISPLAY);
        final String evId = jdoNode.get("evId").asText();
        final String outMessg = jdoNode.get("outMessg").asText();
        assertThat(evId).isNotEmpty();
        assertThat(outMessg).contains("Succès du processus d'import du référentiel des règles de gestion");

        // Given units referencing rule ACC-00003
        doAnswer(args -> {
            RequestResponseOK<JsonNode> response = new RequestResponseOK<>();
            if (args.getArgument(0).toString().contains(ACC_00003)) {
                // Return some dummy unit if rule ACC-00003 queried
                response.addResult(JsonHandler.createObjectNode().put("_id", "some_unit"));
            }
            return response.toJsonNode();
        })
            .when(metaDataClient)
            .selectUnits(any());

        // when
        List<FileRules> fileRulesAfterImport = convertResponseResultToFileRules(
            rulesFileManager.findDocuments(select.getFinalSelect())
        );
        assertEquals(25, fileRulesAfterImport.size());

        for (FileRules fileRulesAfter : fileRulesAfterImport) {
            switch (fileRulesAfter.getRuleid()) {
                case ACC_00003:
                    assertEquals(AccessRule, fileRulesAfter.getRuletype());
                    assertEquals("25", fileRulesAfter.getRuleduration());
                    break;
                case HOL_00001:
                    assertEquals(HoldRule, fileRulesAfter.getRuletype());
                    assertNull(fileRulesAfter.getRuleduration());
                    assertNull(fileRulesAfter.getRulemeasurement());
                    break;
                case HOL_00002:
                    assertEquals(HoldRule, fileRulesAfter.getRuletype());
                    assertEquals("5", fileRulesAfter.getRuleduration());
                    assertEquals(YEAR, fileRulesAfter.getRulemeasurement());
                    break;
                case HOL_00003:
                    assertEquals(HoldRule, fileRulesAfter.getRuletype());
                    assertEquals("1", fileRulesAfter.getRuleduration());
                    assertEquals(YEAR, fileRulesAfter.getRulemeasurement());
                    break;
            }
        }

        final Path reportAfterUpdate = Paths.get(file.getAbsolutePath(), "reportAfterUpdate.json");
        getInputStreamAndInitialiseMockWhenImportFileRules(reportAfterUpdate);

        RequestResponseOK<ItemStatus> value = new RequestResponseOK<>();
        value.setHttpCode(Response.Status.ACCEPTED.getStatusCode());
        when(processingManagementClient.updateOperationActionProcess(any(), any())).thenReturn(value);
        // FILE_TO_COMPARE => insert 1 rule, delete 1 rule, update 1 rule
        rulesFileManager.importFile(
            new FileInputStream(PropertiesUtils.findFile(FILE_UPDATE_RULE_DURATION)),
            FILE_UPDATE_RULE_DURATION
        );
        List<FileRules> fileRulesAfterInsert = convertResponseResultToFileRules(
            rulesFileManager.findDocuments(select.getFinalSelect())
        );
        assertEquals(26, fileRulesAfterInsert.size());
        final JsonNode reportAfterUpdateNode = JsonHandler.getFromFile(reportAfterUpdate.toFile());
        final JsonNode jdoAfterUpdateNode = reportAfterUpdateNode.get(ReportConstants.JDO_DISPLAY);
        final String evIdAfterUpdate = jdoAfterUpdateNode.get("evId").asText();
        final String outMessgAfterUpdate = jdoAfterUpdateNode.get("outMessg").asText();
        assertThat(evIdAfterUpdate).isNotEmpty();
        assertThat(outMessgAfterUpdate).contains(
            "Avertissement lors du processus d'import du référentiel des règles de gestion"
        );

        for (FileRules fileRulesUpdated : fileRulesAfterInsert) {
            switch (fileRulesUpdated.getRuleid()) {
                case ACC_00003:
                    assertEquals(AccessRule, fileRulesUpdated.getRuletype());
                    assertEquals("26", fileRulesUpdated.getRuleduration());
                    break;
                case HOL_00001:
                    assertEquals(HoldRule, fileRulesUpdated.getRuletype());
                    assertEquals("1", fileRulesUpdated.getRuleduration());
                    assertEquals(MONTH, fileRulesUpdated.getRulemeasurement());
                    break;
                case HOL_00002:
                    assertEquals(HoldRule, fileRulesUpdated.getRuletype());
                    assertNull(fileRulesUpdated.getRuleduration());
                    assertNull(fileRulesUpdated.getRulemeasurement());
                    break;
                case HOL_00003:
                    assertEquals(HoldRule, fileRulesUpdated.getRuletype());
                    assertEquals("2", fileRulesUpdated.getRuleduration());
                    assertEquals(MONTH, fileRulesUpdated.getRulemeasurement());
                    break;
            }
        }

        File file2 = folder.newFolder();
        final Path reportAfterUpdate2 = Paths.get(file2.getAbsolutePath(), "reportAfterUpdate.json");
        getInputStreamAndInitialiseMockWhenImportFileRules(reportAfterUpdate2);

        // FILE_TO_COMPARE => update 1 rule, but only one descritption
        rulesFileManager.importFile(
            new FileInputStream(PropertiesUtils.findFile(FILE_UPDATE_RULE_DESC)),
            FILE_UPDATE_RULE_DESC
        );
        List<FileRules> fileRulesAfterInsert2 = convertResponseResultToFileRules(
            rulesFileManager.findDocuments(select.getFinalSelect())
        );
        assertEquals(26, fileRulesAfterInsert2.size());
        final JsonNode reportAfterUpdateNode2 = JsonHandler.getFromFile(reportAfterUpdate2.toFile());
        final JsonNode jdoAfterUpdateNode2 = reportAfterUpdateNode2.get(ReportConstants.JDO_DISPLAY);
        final String evIdAfterUpdate2 = jdoAfterUpdateNode2.get("evId").asText();
        final String outMessgAfterUpdate2 = jdoAfterUpdateNode2.get("outMessg").asText();
        assertThat(evIdAfterUpdate2).isNotEmpty();
        assertThat(outMessgAfterUpdate2).contains("Succès du processus d'import du référentiel des règles de gestion");

        for (FileRules fileRulesUpdated : fileRulesAfterInsert2) {
            if (ACC_00003.equals(fileRulesUpdated.getRuleid())) {
                assertEquals(AccessRule, fileRulesUpdated.getRuletype());
                assertEquals("26", fileRulesUpdated.getRuleduration());
            }
        }
    }

    @Test
    @RunWithCustomExecutor
    public void should_throw_exception_when_csv_with_usedDeletedRuleIds_is_upload() throws Exception {
        int tenantId = 11;
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        final Select select = new Select();
        // given

        // init data with first import
        when(logbookOperationsClient.selectOperation(any())).thenReturn(getJsonResult(STP_IMPORT_RULES, tenantId));
        when(metaDataClient.selectUnits(any())).thenReturn(new RequestResponseOK<JsonNode>().toJsonNode());

        File file = folder.newFolder();
        final Path report = Paths.get(file.getAbsolutePath(), "report.json");
        getInputStreamAndInitialiseMockWhenImportFileRules(report);

        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(tenantId));
        rulesFileManager.importFile(new FileInputStream(PropertiesUtils.findFile(FILE_TO_TEST_OK)), FILE_TO_TEST_OK);

        List<FileRules> fileRulesAfterImport = convertResponseResultToFileRules(
            rulesFileManager.findDocuments(select.getFinalSelect())
        );
        assertEquals(25, fileRulesAfterImport.size());
        final Path reportAfterUpdate = Paths.get(file.getAbsolutePath(), "reportAfterUpdate.json");
        getInputStreamAndInitialiseMockWhenImportFileRules(reportAfterUpdate);

        // prepare for import ko with linked deleted rule
        when(metaDataClient.selectUnits(any())).thenReturn(
            new RequestResponseOK<JsonNode>()
                .addResult(JsonHandler.createObjectNode().put("_id", "some_unit"))
                .toJsonNode()
        );
        final ArgumentCaptor<LogbookOperationParameters> logOpParamsCaptor = ArgumentCaptor.forClass(
            LogbookOperationParameters.class
        );
        doNothing().when(logbookOperationsClient).update(logOpParamsCaptor.capture());

        // when
        assertThatThrownBy(
            () ->
                rulesFileManager.importFile(
                    new FileInputStream(PropertiesUtils.findFile(FILE_DELETE_RULE)),
                    FILE_DELETE_RULE
                )
        ).isInstanceOf(FileRulesException.class);
        List<LogbookOperationParameters> logbooks = logOpParamsCaptor.getAllValues();
        assertThat(logbooks).hasSize(2);
        assertThat(logbooks.get(0).getTypeProcess()).isEqualTo(LogbookTypeProcess.MASTERDATA);
        assertThat(logbooks.get(0).getStatus()).isEqualTo(StatusCode.KO);
        assertThat(logbooks.get(0).getParameterValue(LogbookParameterName.eventType)).isEqualTo("CHECK_RULES");
        assertThat(logbooks.get(1).getTypeProcess()).isEqualTo(LogbookTypeProcess.MASTERDATA);
        assertThat(logbooks.get(1).getStatus()).isEqualTo(StatusCode.KO);
        assertThat(logbooks.get(1).getParameterValue(LogbookParameterName.eventType)).isEqualTo("STP_IMPORT_RULES");
    }

    @Test
    @RunWithCustomExecutor
    public void should_throw_exception_when_csv_with_used_hold_rule_without_duration_updated_to_rule_with_duration()
        throws Exception {
        int tenantId = 13;
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        final Select select = new Select();
        // given

        // init data with first import
        when(logbookOperationsClient.selectOperation(any())).thenReturn(getJsonResult(STP_IMPORT_RULES, tenantId));
        when(metaDataClient.selectUnits(any())).thenReturn(new RequestResponseOK<JsonNode>().toJsonNode());

        File file = folder.newFolder();
        final Path report = Paths.get(file.getAbsolutePath(), "report.json");
        getInputStreamAndInitialiseMockWhenImportFileRules(report);

        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(tenantId));
        rulesFileManager.importFile(new FileInputStream(PropertiesUtils.findFile(FILE_TO_TEST_OK)), FILE_TO_TEST_OK);

        List<FileRules> fileRulesAfterImport = convertResponseResultToFileRules(
            rulesFileManager.findDocuments(select.getFinalSelect())
        );
        assertEquals(25, fileRulesAfterImport.size());
        final Path reportAfterUpdate = Paths.get(file.getAbsolutePath(), "reportAfterUpdate.json");
        getInputStreamAndInitialiseMockWhenImportFileRules(reportAfterUpdate);

        // Given units referencing rule HOL-00001
        doAnswer(args -> {
            RequestResponseOK<JsonNode> response = new RequestResponseOK<>();
            if (args.getArgument(0).toString().contains(HOL_00001)) {
                // Return some dummy unit if rule HOL-00001 queried
                response.addResult(JsonHandler.createObjectNode().put("_id", "some_unit"));
            }
            return response.toJsonNode();
        })
            .when(metaDataClient)
            .selectUnits(any());

        final ArgumentCaptor<LogbookOperationParameters> logOpParamsCaptor = ArgumentCaptor.forClass(
            LogbookOperationParameters.class
        );
        doNothing().when(logbookOperationsClient).update(logOpParamsCaptor.capture());

        // when
        assertThatThrownBy(
            () ->
                rulesFileManager.importFile(
                    new FileInputStream(PropertiesUtils.findFile(FILE_UPDATE_RULE_DURATION)),
                    FILE_UPDATE_RULE_DURATION
                )
        ).isInstanceOf(FileRulesException.class);
        List<LogbookOperationParameters> logbooks = logOpParamsCaptor.getAllValues();
        assertThat(logbooks).hasSize(2);
        assertThat(logbooks.get(0).getTypeProcess()).isEqualTo(LogbookTypeProcess.MASTERDATA);
        assertThat(logbooks.get(0).getStatus()).isEqualTo(StatusCode.KO);
        assertThat(logbooks.get(0).getParameterValue(LogbookParameterName.eventType)).isEqualTo("CHECK_RULES");
        assertThat(logbooks.get(1).getTypeProcess()).isEqualTo(LogbookTypeProcess.MASTERDATA);
        assertThat(logbooks.get(1).getStatus()).isEqualTo(StatusCode.KO);
        assertThat(logbooks.get(1).getParameterValue(LogbookParameterName.eventType)).isEqualTo("STP_IMPORT_RULES");
    }

    @Test
    @RunWithCustomExecutor
    public void should_throw_exception_when_csv_with_used_hold_rule_with_duration_updated_to_rule_without_duration()
        throws Exception {
        int tenantId = 13;
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        final Select select = new Select();
        // given

        // init data with first import
        when(logbookOperationsClient.selectOperation(any())).thenReturn(getJsonResult(STP_IMPORT_RULES, tenantId));
        when(metaDataClient.selectUnits(any())).thenReturn(new RequestResponseOK<JsonNode>().toJsonNode());

        File file = folder.newFolder();
        final Path report = Paths.get(file.getAbsolutePath(), "report.json");
        getInputStreamAndInitialiseMockWhenImportFileRules(report);

        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(tenantId));
        rulesFileManager.importFile(new FileInputStream(PropertiesUtils.findFile(FILE_TO_TEST_OK)), FILE_TO_TEST_OK);

        List<FileRules> fileRulesAfterImport = convertResponseResultToFileRules(
            rulesFileManager.findDocuments(select.getFinalSelect())
        );
        assertEquals(25, fileRulesAfterImport.size());
        final Path reportAfterUpdate = Paths.get(file.getAbsolutePath(), "reportAfterUpdate.json");
        getInputStreamAndInitialiseMockWhenImportFileRules(reportAfterUpdate);

        // Given units referencing rule HOL-00002
        doAnswer(args -> {
            RequestResponseOK<JsonNode> response = new RequestResponseOK<>();
            if (args.getArgument(0).toString().contains(HOL_00002)) {
                // Return some dummy unit if rule HOL-00002 queried
                response.addResult(JsonHandler.createObjectNode().put("_id", "some_unit"));
            }
            return response.toJsonNode();
        })
            .when(metaDataClient)
            .selectUnits(any());

        final ArgumentCaptor<LogbookOperationParameters> logOpParamsCaptor = ArgumentCaptor.forClass(
            LogbookOperationParameters.class
        );
        doNothing().when(logbookOperationsClient).update(logOpParamsCaptor.capture());

        // when
        assertThatThrownBy(
            () ->
                rulesFileManager.importFile(
                    new FileInputStream(PropertiesUtils.findFile(FILE_UPDATE_RULE_DURATION)),
                    FILE_UPDATE_RULE_DURATION
                )
        ).isInstanceOf(FileRulesException.class);
        List<LogbookOperationParameters> logbooks = logOpParamsCaptor.getAllValues();
        assertThat(logbooks).hasSize(2);
        assertThat(logbooks.get(0).getTypeProcess()).isEqualTo(LogbookTypeProcess.MASTERDATA);
        assertThat(logbooks.get(0).getStatus()).isEqualTo(StatusCode.KO);
        assertThat(logbooks.get(0).getParameterValue(LogbookParameterName.eventType)).isEqualTo("CHECK_RULES");
        assertThat(logbooks.get(1).getTypeProcess()).isEqualTo(LogbookTypeProcess.MASTERDATA);
        assertThat(logbooks.get(1).getStatus()).isEqualTo(StatusCode.KO);
        assertThat(logbooks.get(1).getParameterValue(LogbookParameterName.eventType)).isEqualTo("STP_IMPORT_RULES");
    }

    private void getInputStreamAndInitialiseMockWhenImportFileRules(Path report) throws Exception {
        doAnswer(invocation -> {
            InputStream argumentAt = invocation.getArgument(0);
            Files.copy(argumentAt, report);
            return null;
        })
            .when(functionalBackupService)
            .saveFile(any(InputStream.class), any(GUID.class), anyString(), eq(DataCategory.REPORT), anyString());
    }

    @Test
    public void testJsonDiff() throws Exception {
        String s1 = "{\"a\":1,\"b\":2,\"c\":[{\"a\":1,\"b\":2},{\"a\":3,\"b\":4}]}";
        String s2 =
            "{\"a\":1,\"b\":2,\"c\":[{\"xa\":199,\"b\":20},{\"a\":1,\"b\":2}],\"e\":{\"alpha\":123,\"bravo\":1234}}";

        JsonNode json1 = JsonHandler.getFromString(s1);
        JsonNode json2 = JsonHandler.getFromString(s2);

        JsonNode target = JsonDiff.asJson(json1, json2);
        assertEquals(
            target.toString(),
            "[{\"op\":\"add\",\"path\":\"/c/0\",\"value\":{\"xa\":199,\"b\":20}},{\"op\":\"remove\",\"path\":\"/c/2\"},{\"op\":\"add\",\"path\":\"/e\",\"value\":{\"alpha\":123,\"bravo\":1234}}]"
        );
        target = JsonDiff.asJson(json1, json1);
        assertEquals(target.toString(), "[]");

        assertTrue(
            rulesFileManager.checkRuleConformity(JsonHandler.createArrayNode(), JsonHandler.createArrayNode(), 0)
        );
    }

    @Test
    public void testCheckRuleConformityWithEmptyDbThenOK() throws Exception {
        // Given : empty db
        List<Integer> tenants = Arrays.asList(0, 1, 2);
        doReturn(Optional.empty())
            .when(restoreBackupService)
            .readLatestSavedFile(VitamConfiguration.getDefaultStrategy(), null, RULES);
        doReturn(JsonHandler.createArrayNode()).when(functionalBackupService).getCollectionInJson(eq(RULES), anyInt());

        // Whe / then
        assertThatCode(() -> rulesFileManager.checkRuleConformity(tenants)).doesNotThrowAnyException();

        verify(restoreBackupService, times(3)).readLatestSavedFile(
            VitamConfiguration.getDefaultStrategy(),
            null,
            RULES
        );
        verify(functionalBackupService, times(3)).getCollectionInJson(any(), anyInt());
    }

    @Test
    @RunWithCustomExecutor
    public void testCheckRuleConformityThenOK() throws Exception {
        // Given : empty db
        List<Integer> tenants = Arrays.asList(0, 1, 2);

        doAnswer(args -> {
            Integer tenantId = VitamThreadUtils.getVitamSession().getTenantId();
            Document doc = new Document();
            doc.put("_id", "doc" + tenantId);
            CollectionBackupModel collectionBackupModel = new CollectionBackupModel();
            collectionBackupModel.setDocuments(Collections.singletonList(doc));
            return Optional.of(collectionBackupModel);
        })
            .when(restoreBackupService)
            .readLatestSavedFile(VitamConfiguration.getDefaultStrategy(), null, RULES);

        doAnswer(args -> {
            Integer tenantId = VitamThreadUtils.getVitamSession().getTenantId();
            assertThat(tenantId).isEqualTo(args.getArgument(1));
            return JsonHandler.createArrayNode().add(JsonHandler.createObjectNode().put("_id", "doc" + tenantId));
        })
            .when(functionalBackupService)
            .getCollectionInJson(eq(RULES), anyInt());

        // When / then
        assertThatCode(() -> rulesFileManager.checkRuleConformity(tenants)).doesNotThrowAnyException();

        verify(restoreBackupService, times(3)).readLatestSavedFile(
            VitamConfiguration.getDefaultStrategy(),
            null,
            RULES
        );
        verify(functionalBackupService, times(3)).getCollectionInJson(any(), anyInt());
    }

    @Test
    @RunWithCustomExecutor
    public void testCheckRuleConformityWithMismatchThenOK() throws Exception {
        // Given :
        List<Integer> tenants = Arrays.asList(0, 1, 2);

        doAnswer(args -> {
            Integer tenantId = VitamThreadUtils.getVitamSession().getTenantId();

            // Tenant 0 is empty (mismatch)
            if (tenantId == 0) {
                return Optional.empty();
            }

            Document doc = new Document();
            doc.put("_id", "doc" + tenantId);
            CollectionBackupModel collectionBackupModel = new CollectionBackupModel();
            collectionBackupModel.setDocuments(Collections.singletonList(doc));
            return Optional.of(collectionBackupModel);
        })
            .when(restoreBackupService)
            .readLatestSavedFile(VitamConfiguration.getDefaultStrategy(), null, RULES);

        doAnswer(args -> {
            Integer tenantId = VitamThreadUtils.getVitamSession().getTenantId();
            assertThat(tenantId).isEqualTo(args.getArgument(1));
            return JsonHandler.createArrayNode().add(JsonHandler.createObjectNode().put("_id", "doc" + tenantId));
        })
            .when(functionalBackupService)
            .getCollectionInJson(eq(RULES), anyInt());

        assertThatThrownBy(() -> rulesFileManager.checkRuleConformity(tenants)).isInstanceOf(
            ReferentialException.class
        );

        verify(restoreBackupService, times(3)).readLatestSavedFile(
            VitamConfiguration.getDefaultStrategy(),
            null,
            RULES
        );
        verify(functionalBackupService, times(3)).getCollectionInJson(any(), anyInt());
    }
}
