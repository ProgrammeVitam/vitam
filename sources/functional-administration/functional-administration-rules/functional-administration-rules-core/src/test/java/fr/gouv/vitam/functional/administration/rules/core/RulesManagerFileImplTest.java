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
package fr.gouv.vitam.functional.administration.rules.core;


import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.junit.JunitHelper.ElasticsearchTestConfiguration;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.FileRulesModel;
import fr.gouv.vitam.common.server.application.configuration.DbConfigurationImpl;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.ErrorReport;
import fr.gouv.vitam.functional.administration.common.FileRules;
import fr.gouv.vitam.functional.administration.common.FilesSecurisator;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesCsvException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesDeleteException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesImportInProgressException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesUpdateException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.server.AdminManagementConfiguration;
import fr.gouv.vitam.functional.administration.common.server.ElasticsearchAccessAdminFactory;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminFactory;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.functional.administration.counter.SequenceType;
import fr.gouv.vitam.functional.administration.counter.VitamCounterService;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageAlreadyExistsClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.model.response.StoredInfoResult;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.bson.Document;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;


/**
 * Warning : To avoid error on import rules (actually we cannot update) and to be able to test each case, the tenant ID
 * is changed for each call.
 */
public class RulesManagerFileImplTest {

    private static final String ACCESS_RULE = "AccessRule";
    private static final String ACC_00003 = "ACC-00003";
    private static final String APPRAISAL_RULE = "AppraisalRule";
    private static final String FILE_TO_TEST_OK = "jeu_ok.csv";
    private static final String FILE_TO_TEST_KO = "jeu_donnees_KO_regles_CSV_DuplicatedReference.csv";
    private static final String FILE_TO_TEST_RULES_DURATION_KO = "jeu_donnees_KO_regles_CSV_test.csv";
    private static final String FILE_TO_COMPARE = "jeu_donnees_OK_regles_CSV.csv";
    private static final String FILE_UPDATE_RULE_TYPE = "jeu_donnees_OK_regles_CSV_update_ruleType.csv";
    private static final String FILE_TO_TEST_KO_INVALID_FORMAT = "jeu_donnees_KO_regles_CSV_invalid_format.csv";
    private static final Integer TENANT_ID = 0;
    private static final String STP_IMPORT_RULES = "STP_IMPORT_RULES";
    private static final String USED_UPDATED_RULE_RESULT = "used_updated_rule_result.json";



    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @ClassRule
    public static TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private final static String CLUSTER_NAME = "vitam-cluster";
    private final static String HOST_NAME = "127.0.0.1";
    private static ElasticsearchTestConfiguration esConfig = null;
    private static VitamCounterService vitamCounterService;

    static MongodExecutable mongodExecutable;
    static MongodProcess mongod;
    static JunitHelper junitHelper;
    static final String DATABASE_HOST = "localhost";
    static final String DATABASE_NAME = "vitamtest";
    static final String COLLECTION_NAME = "FileRules";
    static int port;
    static RulesManagerFileImpl rulesFileManager;
    private static MongoDbAccessAdminImpl dbImpl;

    @Mock
    private LogbookOperationsClientFactory logbookOperationsClientFactory;

    @Mock
    private WorkspaceClientFactory workspaceClientFactory;

    @Mock
    private StorageClientFactory storageClientFactory;

    @Mock
    private MetaDataClientFactory metaDataClientFactory;

    @Mock
    private FilesSecurisator securisator;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private static List<MongoDbNode> nodes = new ArrayList<>();

    public final ExpectedException exception = ExpectedException.none();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        final MongodStarter starter = MongodStarter.getDefaultInstance();
        junitHelper = JunitHelper.getInstance();

        try {
            esConfig = JunitHelper.startElasticsearchForTest(temporaryFolder, CLUSTER_NAME);
        } catch (final VitamApplicationServerException e1) {
            assumeTrue(false);
        }
        File tempFolder = temporaryFolder.newFolder();
        System.setProperty("vitam.tmp.folder", tempFolder.getAbsolutePath());
        SystemPropertyUtil.refresh();

        final List<ElasticsearchNode> esNodes = new ArrayList<>();
        esNodes.add(new ElasticsearchNode(HOST_NAME, esConfig.getTcpPort()));

        port = junitHelper.findAvailablePort();
        mongodExecutable = starter.prepare(new MongodConfigBuilder()
            .withLaunchArgument("--enableMajorityReadConcern")
            .version(Version.Main.PRODUCTION)
            .net(new Net(port, Network.localhostIsIPv6()))
            .build());
        mongod = mongodExecutable.start();
        nodes.add(new MongoDbNode(DATABASE_HOST, port));

        LogbookOperationsClientFactory.changeMode(null);
        dbImpl = MongoDbAccessAdminFactory.create(new DbConfigurationImpl(nodes, DATABASE_NAME));
        List<Integer> tenants = new ArrayList<>();
        Integer tenantsList[] = {TENANT_ID, 1, 2, 3, 4, 5, 60, 70};
        tenants.addAll(Arrays.asList(tenantsList));

        vitamCounterService = new VitamCounterService(dbImpl, tenants, null);

        ElasticsearchAccessAdminFactory.create(
            new AdminManagementConfiguration(nodes, DATABASE_NAME, CLUSTER_NAME, esNodes));

    }

    @Before
    public void setUp() throws Exception {
        rulesFileManager =
            new RulesManagerFileImpl(
                MongoDbAccessAdminFactory.create(
                    new DbConfigurationImpl(nodes, DATABASE_NAME)),
                vitamCounterService, securisator,
                logbookOperationsClientFactory, storageClientFactory, workspaceClientFactory, metaDataClientFactory);

    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        if (esConfig != null) {
            JunitHelper.stopElasticsearchForTest(esConfig);
        }
        mongod.stop();
        mongodExecutable.stop();
        junitHelper.releasePort(port);
    }

    /**
     * Warning : To avoid error on import rules (actually we cannot update) and to be able to test each case, the tenant
     * ID is changed for each call.
     */
    @Test
    @RunWithCustomExecutor
    public void testimportRulesFile() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(70);
        LogbookOperationsClient logbookOperationsclient = mock(LogbookOperationsClient.class);
        when(logbookOperationsClientFactory.getClient()).thenReturn(logbookOperationsclient);
        when(logbookOperationsclient.selectOperation(Matchers.anyObject()))
            .thenReturn(getJsonResult(STP_IMPORT_RULES, TENANT_ID));

        WorkspaceClient workspaceClient = mock(WorkspaceClient.class);
        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);

        StorageClient storageClient = mock(StorageClient.class);
        when(storageClientFactory.getClient()).thenReturn(storageClient);


        Map<Integer, List<ErrorReport>> errors = new HashMap<>();
        List<FileRulesModel> usedDeletedRules = new ArrayList<>();
        List<FileRulesModel> usedUpdatedRules = new ArrayList<>();
        Set<String> notUsedDeletedRules = new HashSet<>();
        Set<String> notUsedUpdatedRules = new HashSet<>();
        try {
            rulesFileManager.checkFile(new FileInputStream(PropertiesUtils.findFile(FILE_TO_TEST_OK)),
                errors, usedDeletedRules, usedUpdatedRules, notUsedDeletedRules, notUsedUpdatedRules);
        } catch (final FileRulesException e) {
            fail("Check file with FILE_TO_TEST_OK should not throw exception");
        } catch (FileRulesDeleteException e) {
            fail("Check file with FILE_TO_TEST_OK should not throw exception");
        } catch (FileRulesUpdateException e) {
            fail("Check file with FILE_TO_TEST_OK should not throw exception");
        }

        try {
            rulesFileManager.checkFile(new FileInputStream(PropertiesUtils.findFile(FILE_TO_TEST_KO)),
                errors, usedDeletedRules, usedUpdatedRules, notUsedDeletedRules, notUsedUpdatedRules);
            fail("Check file with FILE_TO_TEST_KO should throw exception");
        } catch (final FileRulesCsvException e) {
            exception.expect(FileRulesCsvException.class);
        } catch (FileRulesDeleteException e) {
            fail("Check file with FILE_TO_TEST_OK should not throw exception");
        } catch (FileRulesUpdateException e) {
            fail("Check file with FILE_TO_TEST_OK should not throw exception");
        }
        rulesFileManager.importFile(new FileInputStream(PropertiesUtils.findFile(FILE_TO_TEST_OK)), FILE_TO_TEST_OK);
        final MongoClient client = new MongoClient(new ServerAddress(DATABASE_HOST, port));
        final MongoCollection<Document> collection = client.getDatabase(DATABASE_NAME).getCollection(COLLECTION_NAME);

        // There are 22 rules in the file but, we don't now the order of test execution, so use the modulo to check
        // if the number of rules in mongo is correct.
        assertEquals(0, collection.count() % 22);

        final Select select = new Select();
        select.setQuery(eq("RuleId", "APP-00005"));
        final RequestResponseOK<FileRules> fileList = rulesFileManager.findDocuments(select.getFinalSelect());
        final String id = fileList.getResults().get(0).getString("RuleId");
        final FileRules file = rulesFileManager.findDocumentById(id);


        assertEquals(file.getRuleid(), fileList.getResults().get(0).getRuleid());

        assertThat(file.getVersion()).isEqualTo(1);
        client.close();
    }

    /**
     * Warning : To avoid error on import rules (actually we cannot update) and to be able to test each case, the tenant
     * ID is changed for each call.
     */
    @Test
    @RunWithCustomExecutor
    public void testNoImportInProgess() throws Exception {
        int tenantId = 1;
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        WorkspaceClient workspaceClient = mock(WorkspaceClient.class);
        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);

        StorageClient storageClient = mock(StorageClient.class);
        when(storageClientFactory.getClient()).thenReturn(storageClient);

        LogbookOperationsClient client = mock(LogbookOperationsClient.class);
        when(logbookOperationsClientFactory.getClient()).thenReturn(client);

        when(client.selectOperation(Matchers.anyObject())).thenReturn(getJsonResult(STP_IMPORT_RULES, tenantId));
        rulesFileManager.importFile(new FileInputStream(PropertiesUtils.findFile(FILE_TO_TEST_OK)), FILE_TO_TEST_OK);

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(client.selectOperation(Matchers.anyObject())).thenReturn(getEmptyJsonResponse());
        rulesFileManager.importFile(new FileInputStream(PropertiesUtils.findFile(FILE_TO_TEST_OK)), FILE_TO_TEST_OK);
    }

    /**
     * Warning : To avoid error on import rules (actually cannot update) and to be able to test each case, the tenant ID
     * is changed for each call.
     */
    @Test(expected = FileRulesImportInProgressException.class)
    @RunWithCustomExecutor
    public void testImportInProgess() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(60);
        LogbookOperationsClient client = mock(LogbookOperationsClient.class);
        when(logbookOperationsClientFactory.getClient()).thenReturn(client);
        when(client.selectOperation(Matchers.anyObject())).thenReturn(
            getJsonResult("COMMIT_RULES", 60));
        rulesFileManager.importFile(new FileInputStream(PropertiesUtils.findFile(FILE_TO_TEST_OK)), FILE_TO_TEST_OK);
    }

    private JsonNode getJsonResult(String evType, int tenantId) throws Exception {
        return JsonHandler.getFromString(String.format("{\n" +
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
            "}", evType, tenantId));
    }

    private JsonNode getEmptyJsonResponse() throws Exception {
        return JsonHandler.getFromString("{\n" +
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
            "}");
    }

    @Test
    @RunWithCustomExecutor
    public void shouldRetrieveAllRules() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        WorkspaceClient workspaceClient = mock(WorkspaceClient.class);
        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);

        StorageClient storageClient = mock(StorageClient.class);
        when(storageClientFactory.getClient()).thenReturn(storageClient);

        final Select select = new Select();
        select.setQuery(eq("#tenant", TENANT_ID));
        List<FileRules> fileRules =
            convertResponseResultToFileRules(rulesFileManager.findDocuments(select.getFinalSelect()));
        if (fileRules.size() == 0) {
            rulesFileManager.importFile(new FileInputStream(PropertiesUtils.findFile(FILE_TO_TEST_OK)),
                FILE_TO_TEST_OK);
        }
        List<FileRules> fileRulesAfter =
            convertResponseResultToFileRules(rulesFileManager.findDocuments(select.getFinalSelect()));
        assertEquals(fileRulesAfter.size(), 22);

    }

    @Test
    @RunWithCustomExecutor
    public void shouldInsertUpdateAndDeleteNewFileRules() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final Select select = new Select();
        WorkspaceClient workspaceClient = mock(WorkspaceClient.class);
        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);

        StorageClient storageClient = mock(StorageClient.class);
        when(storageClientFactory.getClient()).thenReturn(storageClient);

        LogbookOperationsClient client = mock(LogbookOperationsClient.class);
        when(logbookOperationsClientFactory.getClient()).thenReturn(client);
        when(client.selectOperation(Matchers.anyObject())).thenReturn(getJsonResult(STP_IMPORT_RULES, TENANT_ID));

        MetaDataClient metaDataClient = mock(MetaDataClient.class);
        when(metaDataClientFactory.getClient()).thenReturn(metaDataClient);
        when(metaDataClient.selectUnits(any())).thenReturn(JsonHandler.createArrayNode());


        try {
            List<FileRules> fileRules = new ArrayList<FileRules>();
            try {
                select.setQuery(eq("#tenant", TENANT_ID));
                fileRules = convertResponseResultToFileRules(rulesFileManager.findDocuments(select.getFinalSelect()));
            } catch (ReferentialException e) {
            }
            if (fileRules.size() == 0) {
                rulesFileManager.importFile(new FileInputStream(PropertiesUtils.findFile(FILE_TO_TEST_OK)),
                    FILE_TO_TEST_OK);
            }
            List<FileRules> fileRulesAfterImport =
                convertResponseResultToFileRules(rulesFileManager.findDocuments(select.getFinalSelect()));
            assertEquals(22, fileRulesAfterImport.size());
            // FILE_TO_COMPARE => insert 1 rule, delete 1 rule, update 1 rule
            rulesFileManager.importFile(new FileInputStream(PropertiesUtils.findFile(FILE_TO_COMPARE)),
                FILE_TO_COMPARE);
            List<FileRules> fileRulesAfterInsert =
                convertResponseResultToFileRules(rulesFileManager.findDocuments(select.getFinalSelect()));
            assertEquals(22, fileRulesAfterInsert.size());

        } catch (ReferentialException | InvalidParseOperationException | IOException |
            InvalidCreateOperationException e) {
            fail("ReferentialException " + e.getCause());
        }
    }


    @Test
    @RunWithCustomExecutor
    public void shouldUpdateRulesType() {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final Select select = new Select();
        try {
            LogbookOperationsClient logbookOperationsclient = mock(LogbookOperationsClient.class);
            when(logbookOperationsClientFactory.getClient()).thenReturn(logbookOperationsclient);
            when(logbookOperationsclient.selectOperation(Matchers.anyObject()))
                .thenReturn(getJsonResult(STP_IMPORT_RULES, TENANT_ID));

            WorkspaceClient workspaceClient = mock(WorkspaceClient.class);
            when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);

            StorageClient storageClient = mock(StorageClient.class);
            when(storageClientFactory.getClient()).thenReturn(storageClient);


            List<FileRules> fileRules = new ArrayList<>();
            try {
                select.setQuery(eq("#tenant", TENANT_ID));
                fileRules = convertResponseResultToFileRules(rulesFileManager.findDocuments(select.getFinalSelect()));
            } catch (ReferentialException e) {
            }
            if (fileRules.size() == 0) {
                rulesFileManager.importFile(new FileInputStream(PropertiesUtils.findFile(FILE_TO_TEST_OK)),
                    FILE_TO_TEST_OK);
            }
            List<FileRules> fileRulesAfterImport =
                convertResponseResultToFileRules(rulesFileManager.findDocuments(select.getFinalSelect()));
            assertEquals(22, fileRulesAfterImport.size());
            for (FileRules fileRulesAfter : fileRulesAfterImport) {
                if (ACC_00003.equals(fileRulesAfter.getRuleid())) {
                    assertEquals(APPRAISAL_RULE, fileRulesAfter.getRuletype());
                }
            }
            // FILE_TO_COMPARE => insert 1 rule, delete 1 rule, update 1 rule
            rulesFileManager.importFile(new FileInputStream(PropertiesUtils.findFile(FILE_UPDATE_RULE_TYPE)),
                FILE_UPDATE_RULE_TYPE);
            List<FileRules> fileRulesAfterInsert =
                convertResponseResultToFileRules(rulesFileManager.findDocuments(select.getFinalSelect()));
            assertEquals(22, fileRulesAfterInsert.size());
            for (FileRules fileRulesUpdated : fileRulesAfterInsert) {
                if (ACC_00003.equals(fileRulesUpdated.getRuleid())) {
                    assertEquals(APPRAISAL_RULE, fileRulesUpdated.getRuletype());
                }
            }


        } catch (ReferentialException | InvalidParseOperationException | IOException |
            InvalidCreateOperationException | LogbookClientException e) {
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
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final Select select = new Select();
        WorkspaceClient workspaceClient = mock(WorkspaceClient.class);
        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);

        StorageClient storageClient = mock(StorageClient.class);
        when(storageClientFactory.getClient()).thenReturn(storageClient);

        LogbookOperationsClient client = mock(LogbookOperationsClient.class);
        when(logbookOperationsClientFactory.getClient()).thenReturn(client);
        when(client.selectOperation(Matchers.anyObject())).thenReturn(getJsonResult(STP_IMPORT_RULES, TENANT_ID));

        MetaDataClient metaDataClient = mock(MetaDataClient.class);
        when(metaDataClientFactory.getClient()).thenReturn(metaDataClient);
        when(metaDataClient.selectUnits(any())).thenReturn(JsonHandler.createArrayNode());

        try {
            select.setQuery(eq("#tenant", TENANT_ID));
            List<FileRules> fileRules =
                convertResponseResultToFileRules(rulesFileManager.findDocuments(select.getFinalSelect()));

            if (fileRules.size() == 0) {
                rulesFileManager.importFile(new FileInputStream(PropertiesUtils.findFile(FILE_TO_TEST_OK)),
                    FILE_TO_TEST_OK);
            }
            List<FileRules> fileRulesAfterImport =
                convertResponseResultToFileRules(rulesFileManager.findDocuments(select.getFinalSelect()));
            assertEquals(22, fileRulesAfterImport.size());
            rulesFileManager.importFile(new FileInputStream(PropertiesUtils.findFile(FILE_TO_TEST_OK)),
                FILE_TO_TEST_OK);
            List<FileRules> fileRulesAfterInsert =
                convertResponseResultToFileRules(rulesFileManager.findDocuments(select.getFinalSelect()));
            assertEquals(22, fileRulesAfterInsert.size());
            assertEquals(fileRulesAfterInsert.stream().findAny().get().get(VitamFieldsHelper.version()),
                vitamCounterService.getSequence(TENANT_ID, SequenceType.RULES_SEQUENCE));

        } catch (ReferentialException | InvalidParseOperationException | IOException |
            InvalidCreateOperationException e) {
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
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        // mock Storage
        StorageClient storageClient = mock(StorageClient.class);
        when(storageClientFactory.getClient()).thenReturn(storageClient);
        when(storageClient.storeFileFromWorkspace(Matchers.anyObject(), Matchers.anyObject(), Matchers.anyObject(),
            Matchers.anyObject())).thenReturn(new StoredInfoResult());
        // mock workspace
        WorkspaceClient workspaceClient = mock(WorkspaceClient.class);
        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);

        MetaDataClient metaDataClient = mock(MetaDataClient.class);
        when(metaDataClientFactory.getClient()).thenReturn(metaDataClient);
        when(metaDataClient.selectUnits(any())).thenReturn(JsonHandler.createArrayNode());


        LogbookOperationsClient client = mock(LogbookOperationsClient.class);
        when(logbookOperationsClientFactory.getClient()).thenReturn(client);
        when(client.selectOperation(Matchers.anyObject())).thenReturn(getJsonResult(STP_IMPORT_RULES, TENANT_ID));

        assertThatThrownBy(
            () -> rulesFileManager.importFile(new FileInputStream(PropertiesUtils.findFile(FILE_TO_TEST_KO)),
                FILE_TO_TEST_KO)).isInstanceOf(FileRulesCsvException.class).hasMessageContaining("Invalid CSV File");

    }

    @Test
    @RunWithCustomExecutor
    public void should_not_duplicate_error_on_each_line_in_report_when_error_append()
        throws Exception {
        final InputStream inputStream = getInputStreamAndInitialiseMockWhenCheckRulesFile(FILE_TO_TEST_RULES_DURATION_KO);

        // Then
        final JsonNode errorReportAtJson = JsonHandler.getFromInputStream(inputStream);
        final JsonNode errorNode = errorReportAtJson.get("error");
        assertThat(errorNode.get("line 10").isArray());
        if (errorNode.get("line 10").isArray()) {

            final ArrayNode line10ArrayNode = (ArrayNode) errorNode.get("line 10");
            assertThat(line10ArrayNode.size()).isNotEqualTo(23);
            assertThat(line10ArrayNode.size()).isEqualTo(2);
            assertThat(line10ArrayNode.get(0).get("Code").asText())
                .contains("STP_IMPORT_RULES_RULEID_DUPLICATION.KO");
            assertThat(line10ArrayNode.get(0).get("Message").asText()).contains(
                "Il existe plusieurs fois le même RuleId. Ce RuleId doit être unique dans l'ensemble du référentiel");
            assertThat(line10ArrayNode.get(0).get("Information additionnelle").asText()).contains(
                "APP-00001");
        }
    }

    @Test
    @RunWithCustomExecutor
    public void should_contains_outMessg_in_error_report_when_csv_with_bad_format_is_upload()
        throws Exception {
        //Given
        // mock Storage
        final InputStream inputStream = getInputStreamAndInitialiseMockWhenCheckRulesFile(FILE_TO_TEST_RULES_DURATION_KO);
        // Then
        final JsonNode errorReportAtJson = JsonHandler.getFromInputStream(inputStream);
        final JsonNode errorNode = errorReportAtJson.get("JDO");

        assertThat(errorNode.get("outMessg").asText())
            .isEqualTo("Échec du processus d'import du référentiel des règles de gestion");
    }

    @Test
    @RunWithCustomExecutor
    public void should_contains_outMessg_in_error_report_when_csv_with_no_rule_type_is_upload()
        throws Exception {
        //Given
        // mock Storage
        final InputStream inputStream = getInputStreamAndInitialiseMockWhenCheckRulesFile(FILE_TO_TEST_KO_INVALID_FORMAT);
        // Then
        final JsonNode errorReportAtJson = JsonHandler.getFromInputStream(inputStream);
        final JsonNode errorNode = errorReportAtJson.get("JDO");

        assertThat(errorNode.get("outMessg").asText())
            .isEqualTo("Échec du processus d'import du référentiel des règles de gestion");
    }

    private InputStream getInputStreamAndInitialiseMockWhenCheckRulesFile(String filename)
        throws StorageAlreadyExistsClientException, StorageNotFoundClientException, StorageServerClientException,
        MetaDataDocumentSizeException, MetaDataExecutionException, InvalidParseOperationException,
        MetaDataClientServerException, FileRulesException {
        //Given
        // mock Storage
        StorageClient storageClient = mock(StorageClient.class);
        when(storageClientFactory.getClient()).thenReturn(storageClient);
        when(storageClient.storeFileFromWorkspace(Matchers.anyObject(), Matchers.anyObject(), Matchers.anyObject(),
            Matchers.anyObject())).thenReturn(new StoredInfoResult());
        // mock workspace
        WorkspaceClient workspaceClient = mock(WorkspaceClient.class);
        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);

        // mock logbook client
        LogbookOperationsClient client = mock(LogbookOperationsClient.class);
        when(logbookOperationsClientFactory.getClient()).thenReturn(client);

        MetaDataClient metaDataClient = mock(MetaDataClient.class);
        when(metaDataClientFactory.getClient()).thenReturn(metaDataClient);
        when(metaDataClient.selectUnits(any())).thenReturn(JsonHandler.createArrayNode());



        assertThatCode(() -> when(client.selectOperation(Matchers.anyObject()))
            .thenReturn(getJsonResult(STP_IMPORT_RULES, TENANT_ID))).doesNotThrowAnyException();

        Map<Integer, List<ErrorReport>> errorsMap = new HashMap<>();
        List<FileRulesModel> usedDeletedRules = new ArrayList<>();
        List<FileRulesModel> usedUpdatedRules = new ArrayList<>();
        Set<String> notUsedDeletedRules = new HashSet<>();
        Set<String> notUsedUpdatedRules = new HashSet<>();


        // When
        assertThatThrownBy(() -> rulesFileManager
            .checkFile(new FileInputStream(PropertiesUtils.findFile(filename)), errorsMap,
                usedDeletedRules, usedUpdatedRules, notUsedDeletedRules, notUsedUpdatedRules))
            .isInstanceOf(ReferentialException.class);

        return rulesFileManager.generateErrorReport(errorsMap, usedDeletedRules, usedUpdatedRules, StatusCode.KO,
            null);
    }



    @Test
    @RunWithCustomExecutor
    public void should_contains_evid_and_outMessg_in_report_when_csv_with_notUsedUpdatedRules_is_upload()
        throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final Select select = new Select();
        // given
        LogbookOperationsClient logbookOperationsclient = mock(LogbookOperationsClient.class);
        when(logbookOperationsClientFactory.getClient()).thenReturn(logbookOperationsclient);
        when(logbookOperationsclient.selectOperation(Matchers.anyObject()))
            .thenReturn(getJsonResult(STP_IMPORT_RULES, TENANT_ID));

        MetaDataClient metaDataClient = mock(MetaDataClient.class);
        when(metaDataClientFactory.getClient()).thenReturn(metaDataClient);
        when(metaDataClient.selectUnits(any())).thenReturn(JsonHandler.createArrayNode());

        WorkspaceClient workspaceClient = mock(WorkspaceClient.class);
        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);

        StorageClient storageClient = mock(StorageClient.class);
        when(storageClientFactory.getClient()).thenReturn(storageClient);

        File file = folder.newFolder();
        final Path report = Paths.get(file.getAbsolutePath(), "report.json");
        getInputStreamAndInitialiseMockWhenImportFileRules(workspaceClient, report);
        // when
        rulesFileManager.importFile(new FileInputStream(PropertiesUtils.findFile(FILE_TO_TEST_OK)),
            FILE_TO_TEST_OK);
        // then
        verify(workspaceClient, times(1)).putObject(anyString(), anyString(), any(InputStream.class));
        final JsonNode fromFile = JsonHandler.getFromFile(report.toFile());
        final JsonNode jdoNode = fromFile.get("JDO");
        final String evId = jdoNode.get("evId").asText();
        final String outMessg = jdoNode.get("outMessg").asText();
        assertThat(evId).isNotEmpty();
        assertThat(outMessg)
            .contains("Succès du processus d'enregistrement de la copie du référentiel des règles de gestion");

        // when
        List<FileRules> fileRulesAfterImport =
            convertResponseResultToFileRules(rulesFileManager.findDocuments(select.getFinalSelect()));
        assertEquals(22, fileRulesAfterImport.size());

        for (FileRules fileRulesAfter : fileRulesAfterImport) {
            if (ACC_00003.equals(fileRulesAfter.getRuleid())) {
                assertEquals(ACCESS_RULE, fileRulesAfter.getRuletype());
            }
        }


        final Path reportAfterUpdate = Paths.get(file.getAbsolutePath(), "reportAfterUpdate.json");
        getInputStreamAndInitialiseMockWhenImportFileRules(workspaceClient, reportAfterUpdate);

        // FILE_TO_COMPARE => insert 1 rule, delete 1 rule, update 1 rule
        rulesFileManager.importFile(new FileInputStream(PropertiesUtils.findFile(FILE_UPDATE_RULE_TYPE)),
            FILE_UPDATE_RULE_TYPE);
        List<FileRules> fileRulesAfterInsert =
            convertResponseResultToFileRules(rulesFileManager.findDocuments(select.getFinalSelect()));
        assertEquals(22, fileRulesAfterInsert.size());

        final JsonNode reportAfterUpdateNode = JsonHandler.getFromFile(reportAfterUpdate.toFile());
        final JsonNode jdoAfterUpdateNode = reportAfterUpdateNode.get("JDO");
        final String evIdAfterUpdate = jdoAfterUpdateNode.get("evId").asText();
        final String outMessgAfterUpdate = jdoNode.get("outMessg").asText();
        assertThat(evIdAfterUpdate).isNotEmpty();
        assertThat(outMessgAfterUpdate)
            .contains("Succès du processus d'enregistrement de la copie du référentiel des règles de gestion");

        for (FileRules fileRulesUpdated : fileRulesAfterInsert) {
            if (ACC_00003.equals(fileRulesUpdated.getRuleid())) {
                assertEquals(APPRAISAL_RULE, fileRulesUpdated.getRuletype());
            }
        }
    }

    @Test
    @RunWithCustomExecutor
    public void should_assert_consistency_of_report_when_update_Rule_linked_to_au_append() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final Select select = new Select();
        // given
        LogbookOperationsClient logbookOperationsclient = mock(LogbookOperationsClient.class);
        when(logbookOperationsClientFactory.getClient()).thenReturn(logbookOperationsclient);
        when(logbookOperationsclient.selectOperation(Matchers.anyObject()))
            .thenReturn(getJsonResult(STP_IMPORT_RULES, TENANT_ID));

        MetaDataClient metaDataClient = mock(MetaDataClient.class);
        when(metaDataClientFactory.getClient()).thenReturn(metaDataClient);
        when(metaDataClient.selectUnits(any()))
            .thenReturn(JsonHandler.getFromFile(PropertiesUtils.findFile(USED_UPDATED_RULE_RESULT)));

        WorkspaceClient workspaceClient = mock(WorkspaceClient.class);
        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);

        StorageClient storageClient = mock(StorageClient.class);
        when(storageClientFactory.getClient()).thenReturn(storageClient);

        File file = folder.newFolder();
        final Path report = Paths.get(file.getAbsolutePath(), "report.json");
        getInputStreamAndInitialiseMockWhenImportFileRules(workspaceClient, report);
        // when
        rulesFileManager.importFile(new FileInputStream(PropertiesUtils.findFile(FILE_TO_TEST_OK)),
            FILE_TO_TEST_OK);
        // then
        verify(workspaceClient, times(1)).putObject(anyString(), anyString(), any(InputStream.class));
        final JsonNode fromFile = JsonHandler.getFromFile(report.toFile());
        final JsonNode jdoNode = fromFile.get("JDO");
        final String evId = jdoNode.get("evId").asText();
        final String outMessg = jdoNode.get("outMessg").asText();
        assertThat(evId).isNotEmpty();
        assertThat(outMessg)
            .contains("Succès du processus d'enregistrement de la copie du référentiel des règles de gestion");


        // when
        List<FileRules> fileRulesAfterImport =
            convertResponseResultToFileRules(rulesFileManager.findDocuments(select.getFinalSelect()));
        assertEquals(22, fileRulesAfterImport.size());

        for (FileRules fileRulesAfter : fileRulesAfterImport) {
            if (ACC_00003.equals(fileRulesAfter.getRuleid())) {
                assertEquals(ACCESS_RULE, fileRulesAfter.getRuletype());
            }
        }


        final Path reportAfterUpdate = Paths.get(file.getAbsolutePath(), "reportAfterUpdate.json");
        getInputStreamAndInitialiseMockWhenImportFileRules(workspaceClient, reportAfterUpdate);

        // FILE_TO_COMPARE => insert 1 rule, delete 1 rule, update 1 rule
        rulesFileManager.importFile(new FileInputStream(PropertiesUtils.findFile(FILE_UPDATE_RULE_TYPE)),
            FILE_UPDATE_RULE_TYPE);
        List<FileRules> fileRulesAfterInsert =
            convertResponseResultToFileRules(rulesFileManager.findDocuments(select.getFinalSelect()));
        assertEquals(22, fileRulesAfterInsert.size());
        final JsonNode reportAfterUpdateNode = JsonHandler.getFromFile(reportAfterUpdate.toFile());
        final JsonNode jdoAfterUpdateNode = reportAfterUpdateNode.get("JDO");
        final String evIdAfterUpdate = jdoAfterUpdateNode.get("evId").asText();
        final String outMessgAfterUpdate = jdoAfterUpdateNode.get("outMessg").asText();
        assertThat(evIdAfterUpdate).isNotEmpty();
        assertThat(outMessgAfterUpdate)
            .contains("Avertissement lors du processus d'import du référentiel des règles de gestion");

        for (FileRules fileRulesUpdated : fileRulesAfterInsert) {
            if (ACC_00003.equals(fileRulesUpdated.getRuleid())) {
                assertEquals(APPRAISAL_RULE, fileRulesUpdated.getRuletype());
            }
        }

    }

    private void getInputStreamAndInitialiseMockWhenImportFileRules(WorkspaceClient workspaceClient, Path report)
        throws Exception {

        doAnswer(invocation -> {
            InputStream argumentAt = invocation.getArgumentAt(2, InputStream.class);
            Files.copy(argumentAt, report);
            return null;
        }).when(workspaceClient).putObject(anyString(), anyString(), any(InputStream.class));


    }

}
