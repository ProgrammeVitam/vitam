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
package fr.gouv.vitam.function.administration.rules.core;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.mock;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.functional.administration.counter.VitamCounterService;
import fr.gouv.vitam.functional.administration.rules.core.RulesSecurisator;
import org.bson.Document;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Matchers;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

import com.fasterxml.jackson.databind.JsonNode;
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
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.junit.JunitHelper.ElasticsearchTestConfiguration;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.server.application.configuration.DbConfigurationImpl;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.FileRules;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesImportInProgressException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.server.AdminManagementConfiguration;
import fr.gouv.vitam.functional.administration.common.server.ElasticsearchAccessAdminFactory;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminFactory;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.functional.administration.counter.VitamCounterService;
import fr.gouv.vitam.functional.administration.rules.core.RulesManagerFileImpl;
import fr.gouv.vitam.functional.administration.rules.core.RulesSecurisator;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;


/**
 * Warning : To avoid error on import rules (actually we cannot update) and to be able to test each case, the tenant ID
 * is changed for each call.
 */
@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(JUnit4.class)
@PowerMockIgnore({"javax.*", "org.xml.*"})
@PrepareForTest({LogbookOperationsClientFactory.class})
public class RulesManagerFileImplTest {
    String FILE_TO_TEST_OK = "jeu_ok.csv";
    String FILE_TO_TEST_KO = "jeu_donnees_KO_regles_CSV_DuplicatedReference.csv";
    private static final String FILE_TO_COMPARE = "jeu_donnees_OK_regles_CSV.csv";
    private static final Integer TENANT_ID = 0;

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    private final static String CLUSTER_NAME = "vitam-cluster";
    private final static String HOST_NAME = "127.0.0.1";
    private static ElasticsearchTestConfiguration esConfig = null;
    private static VitamCounterService vitamCounterService;

    static MongodExecutable mongodExecutable;
    static MongodProcess mongod;
    static MongoClient mongoClient;
    static JunitHelper junitHelper;
    static final String DATABASE_HOST = "localhost";
    static final String DATABASE_NAME = "vitamtest";
    static final String COLLECTION_NAME = "FileRules";
    static int port;
    static RulesManagerFileImpl rulesFileManager;
    private static MongoDbAccessAdminImpl dbImpl;
    static Map<Integer, List<String>> externalIdentifiers;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        final MongodStarter starter = MongodStarter.getDefaultInstance();
        junitHelper = JunitHelper.getInstance();

        try {
            esConfig = JunitHelper.startElasticsearchForTest(tempFolder, CLUSTER_NAME);
        } catch (final VitamApplicationServerException e1) {
            assumeTrue(false);
        }

        final List<ElasticsearchNode> esNodes = new ArrayList<>();
        esNodes.add(new ElasticsearchNode(HOST_NAME, esConfig.getTcpPort()));

        port = junitHelper.findAvailablePort();
        mongodExecutable = starter.prepare(new MongodConfigBuilder()
            .withLaunchArgument("--enableMajorityReadConcern")
            .version(Version.Main.PRODUCTION)
            .net(new Net(port, Network.localhostIsIPv6()))
            .build());
        mongod = mongodExecutable.start();
        final List<MongoDbNode> nodes = new ArrayList<>();
        nodes.add(new MongoDbNode(DATABASE_HOST, port));

        LogbookOperationsClientFactory.changeMode(null);
        dbImpl = MongoDbAccessAdminFactory.create(new DbConfigurationImpl(nodes, DATABASE_NAME));
        List<Integer> tenants = new ArrayList<>();
        Integer tenantsList[] = {TENANT_ID, 1, 2, 3, 4, 5, 60, 70};
        tenants.addAll(Arrays.asList(tenantsList));

        vitamCounterService = new VitamCounterService(dbImpl, tenants, null);
        RulesSecurisator securisator = mock(RulesSecurisator.class);

        rulesFileManager = new RulesManagerFileImpl(
            MongoDbAccessAdminFactory.create(
                new DbConfigurationImpl(nodes, DATABASE_NAME)),
            vitamCounterService, securisator);
        ElasticsearchAccessAdminFactory.create(
            new AdminManagementConfiguration(nodes, DATABASE_NAME, CLUSTER_NAME, esNodes));

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
        try {
            rulesFileManager.checkFile(new FileInputStream(PropertiesUtils.findFile(FILE_TO_TEST_OK)));
            // Nothing there
        } catch (final Exception e) {
            fail("Check file with FILE_TO_TEST_OK should not throw exception");
        }

        try {
            rulesFileManager.checkFile(new FileInputStream(PropertiesUtils.findFile(FILE_TO_TEST_KO)));
            fail("Check file with FILE_TO_TEST_KO should throw exception");
        } catch (final ReferentialException e) {
            // Nothing there
        } catch (final Exception e) {
            fail("Check file with FILE_TO_TEST_KO should not throw this exception");
        }
        rulesFileManager.importFile(new FileInputStream(PropertiesUtils.findFile(FILE_TO_TEST_OK)));
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

        assertEquals(file.getVersion(), 1);
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
        PowerMockito.mockStatic(LogbookOperationsClientFactory.class);
        LogbookOperationsClientFactory logbookOperationsClientFactory =
            PowerMockito.mock(LogbookOperationsClientFactory.class);
        LogbookOperationsClient client = PowerMockito.mock(LogbookOperationsClient.class);
        PowerMockito.when(LogbookOperationsClientFactory.getInstance()).thenReturn(logbookOperationsClientFactory);
        PowerMockito.when(logbookOperationsClientFactory.getClient()).thenReturn(client);

        PowerMockito.when(client.selectOperation(Matchers.anyObject()))
            .thenReturn(getJsonResult(StatusCode.OK.name(), tenantId));
        rulesFileManager.importFile(new FileInputStream(PropertiesUtils.findFile(FILE_TO_TEST_OK)));

        VitamThreadUtils.getVitamSession().setTenantId(++tenantId);
        PowerMockito.when(client.selectOperation(Matchers.anyObject()))
            .thenReturn(getJsonResult(StatusCode.KO.name(), tenantId));
        rulesFileManager.importFile(new FileInputStream(PropertiesUtils.findFile(FILE_TO_TEST_OK)));

        VitamThreadUtils.getVitamSession().setTenantId(++tenantId);
        PowerMockito.when(client.selectOperation(Matchers.anyObject())).thenReturn(getJsonResult(StatusCode.WARNING
            .name(), tenantId));
        rulesFileManager.importFile(new FileInputStream(PropertiesUtils.findFile(FILE_TO_TEST_OK)));

        VitamThreadUtils.getVitamSession().setTenantId(++tenantId);
        PowerMockito.when(client.selectOperation(Matchers.anyObject())).thenReturn(getJsonResult(StatusCode.FATAL
            .name(), tenantId));
        rulesFileManager.importFile(new FileInputStream(PropertiesUtils.findFile(FILE_TO_TEST_OK)));

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        PowerMockito.when(client.selectOperation(Matchers.anyObject())).thenReturn(getEmptyJsonResponse());
        rulesFileManager.importFile(new FileInputStream(PropertiesUtils.findFile(FILE_TO_TEST_OK)));
    }

    /**
     * Warning : To avoid error on import rules (actually cannot update) and to be able to test each case, the tenant ID
     * is changed for each call.
     */
    @Test(expected = FileRulesImportInProgressException.class)
    @RunWithCustomExecutor
    public void testImportInProgess() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(60);
        PowerMockito.mockStatic(LogbookOperationsClientFactory.class);
        LogbookOperationsClientFactory logbookOperationsClientFactory =
            PowerMockito.mock(LogbookOperationsClientFactory.class);
        LogbookOperationsClient client = PowerMockito.mock(LogbookOperationsClient.class);
        PowerMockito.when(LogbookOperationsClientFactory.getInstance()).thenReturn(logbookOperationsClientFactory);
        PowerMockito.when(logbookOperationsClientFactory.getClient()).thenReturn(client);

        PowerMockito.when(client.selectOperation(Matchers.anyObject())).thenReturn(
            getJsonResult(StatusCode.STARTED.name(), 60));
        rulesFileManager.importFile(new FileInputStream(PropertiesUtils.findFile(FILE_TO_TEST_OK)));
    }

    private JsonNode getJsonResult(String outcome, int tenantId) throws Exception {
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
            "                         \"outcome\": \"%s\"\n" +
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
            "                    \"events.outcome\": 1\n" +
            "               }\n" +
            "          }\n" +
            "     }\n" +
            "}", outcome, tenantId));
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
    public void shouldRetrieveAllRules() {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final Select select = new Select();
        try {
            select.setQuery(eq("#tenant", TENANT_ID));
            List<FileRules> fileRules =
                convertResponseResultToFileRules(rulesFileManager.findDocuments(select.getFinalSelect()));
            if (fileRules.size() == 0) {
                rulesFileManager.importFile(new FileInputStream(PropertiesUtils.findFile(FILE_TO_TEST_OK)));
            }
            List<FileRules> fileRulesAfter =
                convertResponseResultToFileRules(rulesFileManager.findDocuments(select.getFinalSelect()));
            assertEquals(fileRulesAfter.size(), 22);
        } catch (ReferentialException | InvalidParseOperationException | IOException |
            InvalidCreateOperationException e) {
            e.printStackTrace();
            fail("ReferentialException " + e.getCause());
        }
    }

    @Test
    @RunWithCustomExecutor
    public void shouldInsertUpdateAndDeleteNewFileRules() {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final Select select = new Select();
        try {
            List<FileRules> fileRules = new ArrayList<FileRules>();
            try {
                select.setQuery(eq("#tenant", TENANT_ID));
                fileRules = convertResponseResultToFileRules(rulesFileManager.findDocuments(select.getFinalSelect()));
            } catch (ReferentialException e) {}
            if (fileRules.size() == 0) {
                rulesFileManager.importFile(new FileInputStream(PropertiesUtils.findFile(FILE_TO_TEST_OK)));
            }
            List<FileRules> fileRulesAfterImport =
                convertResponseResultToFileRules(rulesFileManager.findDocuments(select.getFinalSelect()));
            assertEquals(22, fileRulesAfterImport.size());
            // FILE_TO_COMPARE => insert 1 rule, delete 1 rule, update 1 rule
            rulesFileManager.importFile(new FileInputStream(PropertiesUtils.findFile(FILE_TO_COMPARE)));
            List<FileRules> fileRulesAfterInsert =
                convertResponseResultToFileRules(rulesFileManager.findDocuments(select.getFinalSelect()));
            assertEquals(22, fileRulesAfterInsert.size());

        } catch (ReferentialException | InvalidParseOperationException | IOException |
            InvalidCreateOperationException e) {
            fail("ReferentialException " + e.getCause());
            e.printStackTrace();
        }
    }

    @Test
    @RunWithCustomExecutor
    public void shouldDoNothingOnFileRulesReferential() {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final Select select = new Select();
        try {
            List<FileRules> fileRules = new ArrayList<FileRules>();
            select.setQuery(eq("#tenant", TENANT_ID));
            fileRules = convertResponseResultToFileRules(rulesFileManager.findDocuments(select.getFinalSelect()));

            if (fileRules.size() == 0) {
                rulesFileManager.importFile(new FileInputStream(PropertiesUtils.findFile(FILE_TO_TEST_OK)));
            }
            List<FileRules> fileRulesAfterImport =
                convertResponseResultToFileRules(rulesFileManager.findDocuments(select.getFinalSelect()));
            assertEquals(22, fileRulesAfterImport.size());
            rulesFileManager.importFile(new FileInputStream(PropertiesUtils.findFile(FILE_TO_TEST_OK)));
            List<FileRules> fileRulesAfterInsert =
                convertResponseResultToFileRules(rulesFileManager.findDocuments(select.getFinalSelect()));
            assertEquals(22, fileRulesAfterInsert.size());
            assertEquals(fileRulesAfterInsert.stream().findAny().get().get(VitamDocument.VERSION),
                vitamCounterService.getSequence(TENANT_ID, "RULE"));

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
            return new ArrayList<FileRules>();
        }
    }
}
