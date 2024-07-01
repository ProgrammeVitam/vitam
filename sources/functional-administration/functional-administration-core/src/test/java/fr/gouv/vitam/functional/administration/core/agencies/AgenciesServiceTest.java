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
package fr.gouv.vitam.functional.administration.core.agencies;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.parser.request.adapter.SingleVarNameAdapter;
import fr.gouv.vitam.common.database.parser.request.single.SelectParserSingle;
import fr.gouv.vitam.common.database.server.DbRequestResult;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.SchemaValidationException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.model.administration.AgenciesModel;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.server.application.configuration.DbConfigurationImpl;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.AccessContract;
import fr.gouv.vitam.functional.administration.common.Agencies;
import fr.gouv.vitam.functional.administration.common.config.ElasticsearchFunctionalAdminIndexManager;
import fr.gouv.vitam.functional.administration.common.counter.VitamCounterService;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.server.ElasticsearchAccessFunctionalAdmin;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollectionsTestUtils;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminFactory;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.functional.administration.core.backup.FunctionalBackupService;
import fr.gouv.vitam.functional.administration.core.contract.ContractService;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.in;
import static fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections.AGENCIES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AgenciesServiceTest {

    private static final String AGENCIES_REPORT = "AGENCIES_REPORT";
    public static final String PREFIX = GUIDFactory.newGUID().getId();
    private static final Integer TENANT_ID = 1;

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    @ClassRule
    public static MongoRule mongoRule = new MongoRule(
        MongoDbAccess.getMongoClientSettingsBuilder(Agencies.class, AccessContract.class),
        PREFIX + Agencies.class.getSimpleName(),
        PREFIX + AccessContract.class.getSimpleName()
    );

    @ClassRule
    public static ElasticsearchRule elasticsearchRule = new ElasticsearchRule();

    private static final String ACCESS_CONTRACT_ID = GUIDFactory.newGUID().toString();
    private static final String ACCESS_CONTRACT =
        "{ \"_tenant\": 1,\n" +
        "    \"_id\": \"" +
        ACCESS_CONTRACT_ID +
        "\", \n " +
        "    \"Name\": \"contract_with_field_EveryDataObjectVersion\",\n" +
        "    \"Identifier\": \"AC-000018\",\n" +
        "    \"Description\": \"aDescription of the contract\",\n" +
        "    \"Status\": \"ACTIVE\",\n" +
        "    \"CreationDate\": \"2016-12-10T00:00:00.000\",\n" +
        "    \"LastUpdate\": \"2017-10-06T01:53:22.544\",\n" +
        "    \"ActivationDate\": \"2016-12-10T00:00:00.000\",\n" +
        "    \"DeactivationDate\": \"2016-12-10T00:00:00.000\",\n" +
        "    \"DataObjectVersion\": [],\n" +
        "    \"OriginatingAgencies\": [\n" +
        "        \"FRAN_NP_005568\",\n" +
        "        \"AG-000001\"\n" +
        "    ],\n" +
        "    \"WritingPermission\": true,\n" +
        "    \"EveryOriginatingAgency\": true,\n" +
        "    \"EveryDataObjectVersion\": false,\n" +
        "     \"AccessLog\": \"ACTIVE\", \n" +
        "    \"_v\": 0\n" +
        "}";
    private static VitamCounterService vitamCounterService;
    private static MongoDbAccessAdminImpl dbImpl;

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private FunctionalBackupService functionalBackupService;

    @Mock
    private LogbookAgenciesImportManager manager;

    @Mock
    private LogbookOperationsClientFactory logbookOperationsClientFactory;

    @Mock
    private ContractService<AccessContractModel> accessContractService;

    private AgenciesService agencyService;

    private static final ElasticsearchFunctionalAdminIndexManager indexManager =
        FunctionalAdminCollectionsTestUtils.createTestIndexManager();

    @RunWithCustomExecutor
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        final List<MongoDbNode> nodes = new ArrayList<>();
        nodes.add(new MongoDbNode(MongoRule.MONGO_HOST, MongoRule.getDataBasePort()));

        dbImpl = MongoDbAccessAdminFactory.create(
            new DbConfigurationImpl(nodes, mongoRule.getMongoDatabase().getName()),
            Collections::emptyList,
            indexManager
        );

        final List<ElasticsearchNode> esNodes = new ArrayList<>();
        esNodes.add(new ElasticsearchNode(ElasticsearchRule.getHost(), ElasticsearchRule.getPort()));
        FunctionalAdminCollectionsTestUtils.beforeTestClass(
            mongoRule.getMongoDatabase(),
            PREFIX,
            new ElasticsearchAccessFunctionalAdmin(ElasticsearchRule.VITAM_CLUSTER, esNodes, indexManager),
            Lists.newArrayList(
                FunctionalAdminCollections.AGENCIES,
                FunctionalAdminCollections.ACCESS_CONTRACT,
                FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL,
                FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY
            )
        );

        final List<Integer> tenants = new ArrayList<>();
        tenants.add(TENANT_ID);

        vitamCounterService = new VitamCounterService(dbImpl, tenants, new HashMap<>());
    }

    @AfterClass
    public static void tearDownAfterClass() {
        FunctionalAdminCollectionsTestUtils.afterTestClass(true);
        VitamClientFactory.resetConnections();
    }

    @Before
    public void setUp() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(TENANT_ID));
        resetAgencies();
        File agencyFile = PropertiesUtils.findFile("agency.json");
        dbImpl.insertDocument(JsonHandler.getFromFile(agencyFile), AGENCIES).close();
        VitamDocument<?> contrat = dbImpl.getDocumentById(
            ACCESS_CONTRACT_ID,
            FunctionalAdminCollections.ACCESS_CONTRACT
        );
        if (contrat == null) {
            JsonNode contractToPersist = JsonHandler.getFromString(ACCESS_CONTRACT);
            dbImpl.insertDocument(contractToPersist, FunctionalAdminCollections.ACCESS_CONTRACT).close();
        }
        LogbookOperationsClient logbookOperationsclient = mock(LogbookOperationsClient.class);
        when(logbookOperationsClientFactory.getClient()).thenReturn(logbookOperationsclient);
        when(logbookOperationsclient.selectOperation(any())).thenReturn(getJsonResult(StatusCode.OK.name()));

        when(accessContractService.findContracts(any(JsonNode.class))).thenReturn(new RequestResponseOK<>());
    }

    @After
    public void afterTest() {
        FunctionalAdminCollectionsTestUtils.afterTest(
            com.google.common.collect.Lists.newArrayList(
                FunctionalAdminCollections.AGENCIES,
                FunctionalAdminCollections.ACCESS_CONTRACT
            )
        );
        reset(functionalBackupService);
    }

    @Test
    @RunWithCustomExecutor
    public void should_import_correctly_agencies() throws Exception {
        // Given
        Path reportPath = Paths.get(tempFolder.newFolder().getAbsolutePath(), "report_agencies.json");
        doAnswer(arguments -> {
            Files.copy(arguments.<InputStream>getArgument(0), reportPath);
            return null;
        })
            .when(functionalBackupService)
            .saveFile(
                any(InputStream.class),
                any(GUID.class),
                eq(AGENCIES_REPORT),
                eq(DataCategory.REPORT),
                endsWith(".json")
            );

        // When
        RequestResponse<AgenciesModel> response = agencyService.importAgencies(
            new FileInputStream(PropertiesUtils.getResourceFile("agencies.csv")),
            "MY-FILE-NAME"
        );
        // Then
        assertThat(response.isOk()).isTrue();

        AgenciesReport report = JsonHandler.getFromFile(reportPath.toFile(), AgenciesReport.class);

        assertThat(report.getInsertedAgencies()).containsOnly("AG-000001", "AG-000002", "AG-000003");
    }

    @Test
    @RunWithCustomExecutor
    public void should_report_error_when_deleting_used_agency() throws Exception {
        // Given
        Path reportPath = Paths.get(tempFolder.newFolder().getAbsolutePath(), "report_agencies.json");
        doAnswer(arguments -> {
            Files.copy(arguments.<InputStream>getArgument(0), reportPath);
            return null;
        })
            .when(functionalBackupService)
            .saveFile(
                any(InputStream.class),
                any(GUID.class),
                eq(AGENCIES_REPORT),
                eq(DataCategory.REPORT),
                endsWith(".json")
            );

        // When
        RequestResponse<AgenciesModel> response = agencyService.importAgencies(
            new FileInputStream(PropertiesUtils.getResourceFile("agencies_delete.csv")),
            "MY-FILE-NAME"
        );
        // Then
        assertThat(response.isOk()).isTrue();

        AgenciesReport report = JsonHandler.getFromFile(reportPath.toFile(), AgenciesReport.class);
        assertThat(report.getAgenciesToDelete()).containsOnly("AG-000000");
    }

    @Test
    @RunWithCustomExecutor
    public void should_update_correctly_agency() throws Exception {
        // Given
        Path reportPath = Paths.get(tempFolder.newFolder().getAbsolutePath(), "report_agencies.json");
        doAnswer(arguments -> {
            Files.copy(arguments.<InputStream>getArgument(0), reportPath);
            return null;
        })
            .when(functionalBackupService)
            .saveFile(
                any(InputStream.class),
                any(GUID.class),
                eq(AGENCIES_REPORT),
                eq(DataCategory.REPORT),
                endsWith(".json")
            );

        // When
        RequestResponse<AgenciesModel> response = agencyService.importAgencies(
            new FileInputStream(PropertiesUtils.getResourceFile("agencies2.csv")),
            "MY-FILE-NAME"
        );
        // Then
        assertThat(response.isOk()).isTrue();

        JsonNode report = JsonHandler.getFromFile(reportPath.toFile());
        AgenciesReport agenciesReport = JsonHandler.getFromJsonNode(report, AgenciesReport.class);
        assertThat(agenciesReport.getInsertedAgencies()).containsOnly("AG-000001", "AG-000002", "AG-000003");
        assertThat(agenciesReport.getUpdatedAgencies()).containsOnly("AG-000000");

        Agencies updatedAgency = findDocumentById("AG-000000");
        assertNotNull(updatedAgency);
        assertThat(updatedAgency.getName()).isEqualTo("agency 0");
        assertThat(updatedAgency.getDescription()).isEqualTo("un service agent déjà présent. Il a toujours été là");
    }

    @Test
    @RunWithCustomExecutor
    public void should_not_update_when_update_with_no_changes() throws Exception {
        // Given
        Path reportPath = Paths.get(tempFolder.newFolder().getAbsolutePath(), "report_agencies.json");
        doAnswer(arguments -> {
            Files.copy(arguments.<InputStream>getArgument(0), reportPath);
            return null;
        })
            .when(functionalBackupService)
            .saveFile(
                any(InputStream.class),
                any(GUID.class),
                eq(AGENCIES_REPORT),
                eq(DataCategory.REPORT),
                endsWith(".json")
            );

        // When
        RequestResponse<AgenciesModel> response = agencyService.importAgencies(
            new FileInputStream(PropertiesUtils.getResourceFile("agencies_no_changes.csv")),
            "MY-FILE-NAME"
        );

        // Then
        assertTrue(response.isOk());
        AgenciesReport report = JsonHandler.getFromFile(reportPath.toFile(), AgenciesReport.class);
        assertNotNull(report);
        assertThat(report.getInsertedAgencies()).isNullOrEmpty();
        assertThat(report.getUpdatedAgencies()).isNullOrEmpty();
    }

    @Test
    @RunWithCustomExecutor
    public void should_report_error_when_csv_malformed() throws Exception {
        // Given
        Path reportPath = Paths.get(tempFolder.newFolder().getAbsolutePath(), "report_agencies.json");
        doAnswer(arguments -> {
            Files.copy(arguments.<InputStream>getArgument(0), reportPath);
            return null;
        })
            .when(functionalBackupService)
            .saveFile(
                any(InputStream.class),
                any(GUID.class),
                eq(AGENCIES_REPORT),
                eq(DataCategory.REPORT),
                endsWith(".json")
            );

        // When
        RequestResponse<AgenciesModel> response = agencyService.importAgencies(
            new FileInputStream(PropertiesUtils.getResourceFile("agencies_empty_line.csv")),
            "MY-FILE-NAME"
        );
        // Then
        assertTrue(response instanceof VitamError);
        assertThat(reportPath.toFile()).doesNotExist();
    }

    @Test
    @RunWithCustomExecutor
    public void should_log_warning_when_used_agency_updated() throws Exception {
        // Given
        Path reportPath = Paths.get(tempFolder.newFolder().getAbsolutePath(), "report_agencies.json");
        doAnswer(arguments -> {
            Files.copy(arguments.<InputStream>getArgument(0), reportPath);
            return null;
        })
            .when(functionalBackupService)
            .saveFile(
                any(InputStream.class),
                any(GUID.class),
                eq(AGENCIES_REPORT),
                eq(DataCategory.REPORT),
                endsWith(".json")
            );

        when(accessContractService.findContracts(any(JsonNode.class))).thenReturn(
            new RequestResponseOK<AccessContractModel>().addResult(new AccessContractModel())
        );

        // When
        RequestResponse<AgenciesModel> response = agencyService.importAgencies(
            new FileInputStream(PropertiesUtils.getResourceFile("agencies2.csv")),
            "MY-FILE-NAME"
        );
        // Then
        assertTrue(response.isOk());
        verify(manager).logEventWarning(any(GUID.class), eq("IMPORT_AGENCIES.USED_CONTRACT"));
    }

    @Test
    @RunWithCustomExecutor
    public void should_not_throw_exception_check_file() throws Exception {
        LogbookOperationsClient logbookOperationsclient = mock(LogbookOperationsClient.class);
        when(logbookOperationsClientFactory.getClient()).thenReturn(logbookOperationsclient);
        when(logbookOperationsclient.selectOperation(any())).thenReturn(getJsonResult(StatusCode.OK.name()));

        File file = PropertiesUtils.getResourceFile("agencies_delete.csv");
        agencyService.parseFile(file);
    }

    @Test
    @RunWithCustomExecutor
    public void should_remove_duplicates_from_file() throws Exception {
        // Given
        Path reportPath = Paths.get(tempFolder.newFolder().getAbsolutePath(), "report_agencies.json");
        doAnswer(arguments -> {
            Files.copy(arguments.<InputStream>getArgument(0), reportPath);
            return null;
        })
            .when(functionalBackupService)
            .saveFile(
                any(InputStream.class),
                any(GUID.class),
                eq(AGENCIES_REPORT),
                eq(DataCategory.REPORT),
                endsWith(".json")
            );

        // When
        RequestResponse<AgenciesModel> response = agencyService.importAgencies(
            new FileInputStream(PropertiesUtils.getResourceFile("agenciesDUPLICATE.csv")),
            "MY-FILE-NAME"
        );
        // Then
        assertThat(response.isOk()).isTrue();

        JsonNode report = JsonHandler.getFromFile(reportPath.toFile());
        AgenciesReport agenciesReport = JsonHandler.getFromJsonNode(report, AgenciesReport.class);

        assertThat(agenciesReport.getUpdatedAgencies()).containsOnly("AG-000000");
        assertThat(agenciesReport.getInsertedAgencies()).containsOnly(
            "AG-000006",
            "AG-000005",
            "AG-000004",
            "AG-000003",
            "AG-000002",
            "AG-000001"
        );
        assertThat(agenciesReport.getAgenciesToImport()).containsOnly(
            "AG-000006",
            "AG-000005",
            "AG-000004",
            "AG-000003",
            "AG-000002",
            "AG-000001",
            "AG-000000"
        );
    }

    private JsonNode getJsonResult(String outcome) throws Exception {
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
                "                    \"events.evType\": \"STP_IMPORT_AGENCIES\"\n" +
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
                "}",
                outcome,
                AgenciesServiceTest.TENANT_ID
            )
        );
    }

    private void resetAgencies()
        throws InvalidParseOperationException, ReferentialException, InvalidCreateOperationException, BadRequestException, SchemaValidationException {
        List<Agencies> agencies = getAllAgencies();
        if (!agencies.isEmpty()) {
            String[] agenciesId = agencies.stream().map(Agencies::getIdentifier).toArray(String[]::new);
            Select select = new Select();
            select.setQuery(in(Agencies.IDENTIFIER, agenciesId));
            dbImpl.deleteDocument(select.getFinalSelect(), AGENCIES);
        }
        agencyService = new AgenciesService(
            dbImpl,
            vitamCounterService,
            functionalBackupService,
            manager,
            accessContractService
        );
    }

    private Agencies findDocumentById(String id)
        throws ReferentialException, InvalidParseOperationException, InvalidCreateOperationException {
        SanityChecker.checkParameter(id);

        final SelectParserSingle parser = new SelectParserSingle(new SingleVarNameAdapter());
        parser.parse(parser.getRequest().getFinalSelect());
        parser.addCondition(QueryHelper.eq(AgenciesModel.TAG_IDENTIFIER, id));
        DbRequestResult result = dbImpl.findDocuments(parser.getRequest().getFinalSelect(), AGENCIES);
        parser.parse(new Select().getFinalSelect());

        final List<Agencies> list = result.getDocuments(Agencies.class);
        if (list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    private List<Agencies> getAllAgencies() throws InvalidParseOperationException, ReferentialException {
        SelectParserSingle parser = new SelectParserSingle(new SingleVarNameAdapter());
        parser.parse(parser.getRequest().getFinalSelect());

        DbRequestResult result = dbImpl.findDocuments(parser.getRequest().getFinalSelect(), AGENCIES);
        parser.parse(new Select().getFinalSelect());
        return result.getDocuments(Agencies.class);
    }
}
