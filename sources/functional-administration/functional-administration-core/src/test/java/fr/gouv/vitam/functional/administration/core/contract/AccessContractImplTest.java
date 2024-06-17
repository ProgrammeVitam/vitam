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
package fr.gouv.vitam.functional.administration.core.contract;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Lists;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.action.SetAction;
import fr.gouv.vitam.common.database.builder.query.action.UnsetAction;
import fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.builder.request.single.Update;
import fr.gouv.vitam.common.database.parser.request.adapter.SingleVarNameAdapter;
import fr.gouv.vitam.common.database.parser.request.single.SelectParserSingle;
import fr.gouv.vitam.common.database.parser.request.single.UpdateParserSingle;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.DocumentAlreadyExistsException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.SchemaValidationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.model.administration.ActivationStatus;
import fr.gouv.vitam.common.model.administration.AgenciesModel;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.server.application.configuration.DbConfigurationImpl;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadFactory;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.AccessContract;
import fr.gouv.vitam.functional.administration.common.Agencies;
import fr.gouv.vitam.functional.administration.common.AgenciesParser;
import fr.gouv.vitam.functional.administration.common.config.ElasticsearchFunctionalAdminIndexManager;
import fr.gouv.vitam.functional.administration.common.counter.VitamCounterService;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.server.ElasticsearchAccessFunctionalAdmin;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollectionsTestUtils;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminFactory;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.functional.administration.core.backup.FunctionalBackupService;
import fr.gouv.vitam.functional.administration.core.format.model.FunctionalOperationModel;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fr.gouv.vitam.common.guid.GUIDFactory.newOperationLogbookGUID;
import static fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections.AGENCIES;
import static fr.gouv.vitam.functional.administration.core.contract.AccessContractImpl.CONTRACT_BACKUP_EVENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class AccessContractImplTest {

    private static final String NEW_NAME = "New Name";

    private static final String NAME = "Name";

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    public static final String PREFIX = GUIDFactory.newGUID().getId();

    @ClassRule
    public static MongoRule mongoRule = new MongoRule(
        MongoDbAccess.getMongoClientSettingsBuilder(AccessContract.class, Agencies.class)
    );

    @ClassRule
    public static ElasticsearchRule elasticsearchRule = new ElasticsearchRule();

    private static final int TENANT_ID = 1;
    private static final int EXTERNAL_TENANT = 2;
    private static MongoDbAccessAdminImpl dbImpl;

    static final String DATABASE_HOST = "localhost";
    static MetaDataClient metaDataClientMock;
    static LogbookOperationsClient logbookOperationsClientMock;
    static FunctionalBackupService functionalBackupService;
    static VitamCounterService vitamCounterService;

    static ContractService<AccessContractModel> accessContractService;

    private static final ElasticsearchFunctionalAdminIndexManager indexManager =
        FunctionalAdminCollectionsTestUtils.createTestIndexManager();

    @Before
    public void setUp() throws Exception {
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(TENANT_ID));
    }

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        FunctionalAdminCollectionsTestUtils.beforeTestClass(
            mongoRule.getMongoDatabase(),
            PREFIX,
            new ElasticsearchAccessFunctionalAdmin(
                ElasticsearchRule.VITAM_CLUSTER,
                Collections.singletonList(
                    new ElasticsearchNode(ElasticsearchRule.getHost(), ElasticsearchRule.getPort())
                ),
                indexManager
            ),
            Arrays.asList(FunctionalAdminCollections.ACCESS_CONTRACT, FunctionalAdminCollections.AGENCIES)
        );

        final List<MongoDbNode> nodes = new ArrayList<>();
        nodes.add(new MongoDbNode(DATABASE_HOST, MongoRule.getDataBasePort()));

        dbImpl = MongoDbAccessAdminFactory.create(
            new DbConfigurationImpl(nodes, mongoRule.getMongoDatabase().getName()),
            Collections::emptyList,
            indexManager
        );
        final List<Integer> tenants = new ArrayList<>();
        tenants.add(TENANT_ID);
        tenants.add(EXTERNAL_TENANT);
        Map<Integer, List<String>> listEnableExternalIdentifiers = new HashMap<>();
        List<String> list_tenant = new ArrayList<>();
        list_tenant.add("ACCESS_CONTRACT");
        listEnableExternalIdentifiers.put(EXTERNAL_TENANT, list_tenant);
        vitamCounterService = new VitamCounterService(dbImpl, tenants, listEnableExternalIdentifiers);
        LogbookOperationsClientFactory.changeMode(null);

        metaDataClientMock = mock(MetaDataClient.class);

        logbookOperationsClientMock = mock(LogbookOperationsClient.class);

        functionalBackupService = mock(FunctionalBackupService.class);

        accessContractService = new AccessContractImpl(
            MongoDbAccessAdminFactory.create(
                new DbConfigurationImpl(nodes, mongoRule.getMongoDatabase().getName()),
                Collections::emptyList,
                indexManager
            ),
            vitamCounterService,
            metaDataClientMock,
            logbookOperationsClientMock,
            functionalBackupService
        );
        final File fileAgencies = PropertiesUtils.getResourceFile("agencies-contract.csv");

        final Thread thread = VitamThreadFactory.getInstance()
            .newThread(() -> {
                try {
                    VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

                    insertDocuments(AgenciesParser.readFromCsv(new FileInputStream(fileAgencies)));

                    VitamThreadUtils.getVitamSession().setTenantId(EXTERNAL_TENANT);
                    insertDocuments(AgenciesParser.readFromCsv(new FileInputStream(fileAgencies)));
                } catch (VitamException | IOException e) {
                    throw new RuntimeException(e);
                }
            });

        thread.start();
        thread.join();
    }

    private static void insertDocuments(List<AgenciesModel> agenciesToInsert)
        throws InvalidParseOperationException, ReferentialException, SchemaValidationException, DocumentAlreadyExistsException {
        ArrayNode agenciesNodeToPersist = JsonHandler.createArrayNode();

        for (final AgenciesModel agency : agenciesToInsert) {
            agenciesNodeToPersist.add(JsonHandler.toJsonNode(agency));
        }
        if (!agenciesToInsert.isEmpty()) {
            dbImpl.insertDocuments(agenciesNodeToPersist, AGENCIES, 0).close();
        }
    }

    @AfterClass
    public static void tearDownAfterClass() {
        FunctionalAdminCollectionsTestUtils.afterTestClass(true);
        accessContractService.close();
    }

    @After
    public void afterTest() {
        FunctionalAdminCollectionsTestUtils.afterTest(Lists.newArrayList(FunctionalAdminCollections.ACCESS_CONTRACT));
        reset(functionalBackupService);
        reset(logbookOperationsClientMock);
        reset(metaDataClientMock);
    }

    @Test
    @RunWithCustomExecutor
    public void givenAccessContractsTestWellFormedContractThenImportSuccessfully() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileContracts = PropertiesUtils.getResourceFile("contracts_access_ok.json");
        final List<AccessContractModel> accessContractModelList = JsonHandler.getFromFileAsTypeReference(
            fileContracts,
            new TypeReference<>() {}
        );
        final RequestResponse<AccessContractModel> response = accessContractService.createContracts(
            accessContractModelList
        );

        assertThat(response.isOk()).isTrue();
        final RequestResponseOK<AccessContractModel> responseCast = (RequestResponseOK<AccessContractModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);
        assertThat(responseCast.getResults().get(0).getIdentifier()).contains("AC-000");
        assertThat(responseCast.getResults().get(1).getIdentifier()).contains("AC-000");

        verify(functionalBackupService).saveCollectionAndSequence(
            any(),
            eq(CONTRACT_BACKUP_EVENT),
            eq(FunctionalAdminCollections.ACCESS_CONTRACT),
            any()
        );

        ArgumentCaptor<LogbookOperationParameters> logbookOperationParametersCaptor = ArgumentCaptor.forClass(
            LogbookOperationParameters.class
        );
        verify(logbookOperationsClientMock, times(1)).create(logbookOperationParametersCaptor.capture());
        verify(logbookOperationsClientMock, times(1)).update(logbookOperationParametersCaptor.capture());
        List<LogbookOperationParameters> allLogbookOperationParameters =
            logbookOperationParametersCaptor.getAllValues();
        assertThat(allLogbookOperationParameters.size()).isEqualTo(2);
        assertThat(allLogbookOperationParameters.get(0).getStatus()).isEqualTo(StatusCode.STARTED);
        assertThat(allLogbookOperationParameters.get(0).getParameterValue(LogbookParameterName.eventType)).isEqualTo(
            "STP_IMPORT_ACCESS_CONTRACT"
        );
        assertThat(allLogbookOperationParameters.get(1).getStatus()).isEqualTo(StatusCode.OK);
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.eventType)).isEqualTo(
            "STP_IMPORT_ACCESS_CONTRACT"
        );
    }

    @Test
    @RunWithCustomExecutor
    public void givenAccessContractsTestMissingNameReturnBadRequest() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileContracts = PropertiesUtils.getResourceFile("contracts_access_missingName.json");
        final List<AccessContractModel> accessContractModelList = JsonHandler.getFromFileAsTypeReference(
            fileContracts,
            new TypeReference<>() {}
        );
        final RequestResponse<AccessContractModel> response = accessContractService.createContracts(
            accessContractModelList
        );

        assertThat(response.isOk()).isFalse();

        verifyNoMoreInteractions(functionalBackupService);

        ArgumentCaptor<LogbookOperationParameters> logbookOperationParametersCaptor = ArgumentCaptor.forClass(
            LogbookOperationParameters.class
        );
        verify(logbookOperationsClientMock, times(1)).create(logbookOperationParametersCaptor.capture());
        verify(logbookOperationsClientMock, times(1)).update(logbookOperationParametersCaptor.capture());
        List<LogbookOperationParameters> allLogbookOperationParameters =
            logbookOperationParametersCaptor.getAllValues();
        assertThat(allLogbookOperationParameters.size()).isEqualTo(2);
        assertThat(allLogbookOperationParameters.get(0).getStatus()).isEqualTo(StatusCode.STARTED);
        assertThat(allLogbookOperationParameters.get(0).getParameterValue(LogbookParameterName.eventType)).isEqualTo(
            "STP_IMPORT_ACCESS_CONTRACT"
        );
        assertThat(allLogbookOperationParameters.get(1).getStatus()).isEqualTo(StatusCode.KO);
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.eventType)).isEqualTo(
            "STP_IMPORT_ACCESS_CONTRACT"
        );
        assertThat(
            allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.outcomeDetail)
        ).isEqualTo("STP_IMPORT_ACCESS_CONTRACT.EMPTY_REQUIRED_FIELD.KO");
    }

    @Test
    @RunWithCustomExecutor
    public void givenAccessContractsTestNotAllowedNotNullIdInCreation() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileContracts = PropertiesUtils.getResourceFile("contracts_access_ok.json");

        final List<AccessContractModel> accessContractModelList = JsonHandler.getFromFileAsTypeReference(
            fileContracts,
            new TypeReference<>() {}
        );
        accessContractModelList.get(0).setId(GUIDFactory.newGUID().getId());
        RequestResponse<AccessContractModel> response = accessContractService.createContracts(accessContractModelList);

        assertThat(response.isOk()).isFalse();

        ArgumentCaptor<LogbookOperationParameters> logbookOperationParametersCaptor = ArgumentCaptor.forClass(
            LogbookOperationParameters.class
        );
        verify(logbookOperationsClientMock, times(1)).create(logbookOperationParametersCaptor.capture());
        verify(logbookOperationsClientMock, times(1)).update(logbookOperationParametersCaptor.capture());
        List<LogbookOperationParameters> allLogbookOperationParameters =
            logbookOperationParametersCaptor.getAllValues();
        assertThat(allLogbookOperationParameters.size()).isEqualTo(2);
        assertThat(allLogbookOperationParameters.get(0).getStatus()).isEqualTo(StatusCode.STARTED);
        assertThat(allLogbookOperationParameters.get(0).getParameterValue(LogbookParameterName.eventType)).isEqualTo(
            "STP_IMPORT_ACCESS_CONTRACT"
        );
        assertThat(allLogbookOperationParameters.get(1).getStatus()).isEqualTo(StatusCode.KO);
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.eventType)).isEqualTo(
            "STP_IMPORT_ACCESS_CONTRACT"
        );
        assertThat(
            allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.outcomeDetail)
        ).isEqualTo("AccessContract service error");
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.eventDetailData))
            .contains("accessContractCheck")
            .contains("Id must be null when creating contracts (aName)");
    }

    @Test
    @RunWithCustomExecutor
    public void givenAccessContractsTestSameName() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileContracts = PropertiesUtils.getResourceFile("contracts_access_ok.json");

        List<AccessContractModel> accessContractModelList = JsonHandler.getFromFileAsTypeReference(
            fileContracts,
            new TypeReference<>() {}
        );
        RequestResponse<AccessContractModel> response = accessContractService.createContracts(accessContractModelList);

        final RequestResponseOK<AccessContractModel> responseCast = (RequestResponseOK<AccessContractModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);

        // unset ids
        accessContractModelList = JsonHandler.getFromFileAsTypeReference(fileContracts, new TypeReference<>() {});
        response = accessContractService.createContracts(accessContractModelList);

        assertThat(response.isOk()).isTrue();
    }

    @Test
    @RunWithCustomExecutor
    public void givenAccessContractTestFindByFakeID() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        // find accessContract with the fake id should return Status.OK

        final SelectParserSingle parser = new SelectParserSingle(new SingleVarNameAdapter());
        final Select select = new Select();
        parser.parse(select.getFinalSelect());
        parser.addCondition(QueryHelper.eq("#id", "fakeid"));
        final JsonNode queryDsl = parser.getRequest().getFinalSelect();
        /*
         * String q = "{ \"$query\" : [ { \"$eq\" : { \"_id\" : \"fake_id\" } } ] }"; JsonNode queryDsl =
         * JsonHandler.getFromString(q);
         */
        final RequestResponseOK<AccessContractModel> accessContractModelList = accessContractService.findContracts(
            queryDsl
        );

        assertThat(accessContractModelList.getResults()).isEmpty();
    }

    @Test
    @RunWithCustomExecutor
    public void givenAccessContractTestUpdateAccessContractStatus() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final String documentName = "aName";
        // Create document
        final File fileContracts = PropertiesUtils.getResourceFile("contracts_access_ok.json");

        final List<AccessContractModel> accessContractModelList = JsonHandler.getFromFileAsTypeReference(
            fileContracts,
            new TypeReference<>() {}
        );
        final RequestResponse<AccessContractModel> response = accessContractService.createContracts(
            accessContractModelList
        );

        final RequestResponseOK<AccessContractModel> responseCast = (RequestResponseOK<AccessContractModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);

        final SelectParserSingle parser = new SelectParserSingle(new SingleVarNameAdapter());
        final Select select = new Select();
        parser.parse(select.getFinalSelect());
        parser.addCondition(QueryHelper.eq(NAME, documentName));
        final JsonNode queryDsl = parser.getRequest().getFinalSelect();
        final RequestResponseOK<AccessContractModel> accessContractModelList2 = accessContractService.findContracts(
            queryDsl
        );
        assertThat(accessContractModelList2.getResults()).isNotEmpty();
        for (final AccessContractModel accessContractModel : accessContractModelList2.getResults()) {
            assertEquals(ActivationStatus.ACTIVE, accessContractModel.getStatus());
        }

        // Test update for access contract Status => inactive
        final String now = LocalDateUtil.nowFormatted();
        final UpdateParserSingle updateParser = new UpdateParserSingle(new SingleVarNameAdapter());
        final SetAction setActionStatusInactive = UpdateActionHelper.set(
            "Status",
            ActivationStatus.INACTIVE.toString()
        );
        final SetAction setActionDesactivationDateInactive = UpdateActionHelper.set("DeactivationDate", now);
        final SetAction setActionLastUpdateInactive = UpdateActionHelper.set("LastUpdate", now);

        final Update update = new Update();
        update.setQuery(QueryHelper.eq(NAME, documentName));
        update.addActions(setActionStatusInactive, setActionDesactivationDateInactive, setActionLastUpdateInactive);
        updateParser.parse(update.getFinalUpdate());
        final JsonNode queryDslForUpdate = updateParser.getRequest().getFinalUpdate();

        RequestResponse<AccessContractModel> updateContractStatus = accessContractService.updateContract(
            accessContractModelList.get(0).getIdentifier(),
            queryDslForUpdate
        );
        assertThat(updateContractStatus).isNotExactlyInstanceOf(VitamError.class);

        final RequestResponseOK<AccessContractModel> accessContractModelListForassert =
            accessContractService.findContracts(queryDsl);
        assertThat(accessContractModelListForassert.getResults()).isNotEmpty();
        for (final AccessContractModel accessContractModel : accessContractModelListForassert.getResults()) {
            assertThat(ActivationStatus.INACTIVE.equals(accessContractModel.getStatus())).isTrue();
            assertThat(accessContractModel.getDeactivationdate()).isNotEmpty();
            assertThat(accessContractModel.getLastupdate()).isNotEmpty();
        }

        // Test update for access contract Status => Active
        final UpdateParserSingle updateParserActive = new UpdateParserSingle(new SingleVarNameAdapter());
        final SetAction setActionStatusActive = UpdateActionHelper.set("Status", ActivationStatus.ACTIVE.toString());
        final SetAction setActionDesactivationDateActive = UpdateActionHelper.set("ActivationDate", now);
        final SetAction setActionLastUpdateActive = UpdateActionHelper.set("LastUpdate", now);
        final Update updateStatusActive = new Update();
        updateStatusActive.setQuery(QueryHelper.eq(NAME, documentName));
        updateStatusActive.addActions(
            setActionStatusActive,
            setActionDesactivationDateActive,
            setActionLastUpdateActive
        );
        updateParserActive.parse(updateStatusActive.getFinalUpdate());
        final JsonNode queryDslStatusActive = updateParserActive.getRequest().getFinalUpdate();

        accessContractService.updateContract(accessContractModelList.get(0).getIdentifier(), queryDslStatusActive);

        final RequestResponseOK<AccessContractModel> accessContractModelListForassert2 =
            accessContractService.findContracts(queryDsl);
        assertThat(accessContractModelListForassert2.getResults()).isNotEmpty();
        for (final AccessContractModel accessContractModel : accessContractModelListForassert2.getResults()) {
            assertThat(ActivationStatus.ACTIVE.equals(accessContractModel.getStatus())).isTrue();
            assertThat(accessContractModel.getActivationdate()).isNotEmpty();
            assertThat(accessContractModel.getLastupdate()).isNotEmpty();
        }

        // we try to update access contract with same value -> Bad Request
        RequestResponse<AccessContractModel> responseUpdate = accessContractService.updateContract(
            accessContractModelList.get(0).getIdentifier(),
            queryDslStatusActive
        );
        assertThat(responseUpdate.isOk()).isTrue();
        assertEquals(200, responseUpdate.getStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void givenAccessContractTestUpdateAccessContractOriginatingAgency() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final String documentName = "aName";

        // Create document
        final File fileContracts = PropertiesUtils.getResourceFile("contracts_access_ok.json");

        final List<AccessContractModel> accessContractModelList = JsonHandler.getFromFileAsTypeReference(
            fileContracts,
            new TypeReference<>() {}
        );
        final RequestResponse<AccessContractModel> response = accessContractService.createContracts(
            accessContractModelList
        );

        RequestResponseOK<AccessContractModel> responseCast = (RequestResponseOK<AccessContractModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);

        final SelectParserSingle parser = new SelectParserSingle(new SingleVarNameAdapter());
        final Select select = new Select();
        parser.parse(select.getFinalSelect());
        parser.addCondition(QueryHelper.eq(NAME, documentName));
        final JsonNode queryDsl = parser.getRequest().getFinalSelect();
        responseCast = accessContractService.findContracts(queryDsl);
        assertThat(responseCast.getResults()).isNotEmpty();
        for (final AccessContractModel accessContractModel : responseCast.getResults()) {
            assertEquals(ActivationStatus.ACTIVE, accessContractModel.getStatus());
        }

        // Test update for access contract Status => inactive
        final String now = LocalDateUtil.nowFormatted();
        final UpdateParserSingle updateParser = new UpdateParserSingle(new SingleVarNameAdapter());
        final SetAction setActionStatusInactive = UpdateActionHelper.set(
            "Status",
            ActivationStatus.INACTIVE.toString()
        );
        final SetAction setActionName = UpdateActionHelper.set(NAME, NEW_NAME);
        final SetAction setActionDesactivationDateInactive = UpdateActionHelper.set("DeactivationDate", now);
        final SetAction setActionLastUpdateInactive = UpdateActionHelper.set("LastUpdate", now);
        final Update update = new Update();
        update.setQuery(QueryHelper.eq(NAME, documentName));
        update.addActions(
            setActionName,
            setActionStatusInactive,
            setActionDesactivationDateInactive,
            setActionLastUpdateInactive
        );
        updateParser.parse(update.getFinalUpdate());
        JsonNode queryDslForUpdate = updateParser.getRequest().getFinalUpdate();
        RequestResponse<AccessContractModel> updateContractStatus = accessContractService.updateContract(
            accessContractModelList.get(0).getIdentifier(),
            queryDslForUpdate
        );
        assertThat(updateContractStatus).isNotExactlyInstanceOf(VitamError.class);

        final Select newSelect = new Select();
        newSelect.setQuery(QueryHelper.eq(NAME, NEW_NAME));
        final RequestResponseOK<AccessContractModel> accessContractModelListForassert =
            accessContractService.findContracts(newSelect.getFinalSelect());
        assertThat(accessContractModelListForassert.getResults()).isNotEmpty();
        for (final AccessContractModel accessContractModel : accessContractModelListForassert.getResults()) {
            assertThat(ActivationStatus.INACTIVE.equals(accessContractModel.getStatus())).isTrue();
            assertThat(accessContractModel.getEveryOriginatingAgency()).isFalse();
            assertThat(accessContractModel.getDeactivationdate()).isNotEmpty();
            assertThat(accessContractModel.getLastupdate()).isNotEmpty();
        }

        // Test update for access contract Status => Active
        final UpdateParserSingle updateParserActive = new UpdateParserSingle(new SingleVarNameAdapter());
        final SetAction setActionEveryOriginatingAgency = UpdateActionHelper.set("EveryOriginatingAgency", true);
        final SetAction setActionLastUpdateActive = UpdateActionHelper.set("LastUpdate", now);
        final Update updateStatusActive = new Update();
        updateStatusActive.setQuery(QueryHelper.eq(NAME, NEW_NAME));
        updateStatusActive.addActions(setActionEveryOriginatingAgency, setActionLastUpdateActive);
        updateParserActive.parse(updateStatusActive.getFinalUpdate());
        JsonNode queryDslStatusActive = updateParserActive.getRequest().getFinalUpdate();
        accessContractService.updateContract(accessContractModelList.get(0).getIdentifier(), queryDslStatusActive);

        final RequestResponseOK<AccessContractModel> accessContractModelListForassert2 =
            accessContractService.findContracts(newSelect.getFinalSelect());
        assertThat(accessContractModelListForassert2.getResults()).isNotEmpty();
        for (final AccessContractModel accessContractModel : accessContractModelListForassert2.getResults()) {
            assertThat(ActivationStatus.INACTIVE.equals(accessContractModel.getStatus())).isTrue();
            assertThat(accessContractModel.getEveryOriginatingAgency()).isTrue();
            assertThat(accessContractModel.getActivationdate()).isNotEmpty();
            assertThat(accessContractModel.getLastupdate()).isNotEmpty();
        }
    }

    /**
     * Check that the created access conrtact have the tenant owner after persisted to database
     *
     * @throws Exception
     */
    @Test
    @RunWithCustomExecutor
    public void givenAccessContractsTestTenantOwner() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileContracts = PropertiesUtils.getResourceFile("contracts_access_ok.json");
        final List<AccessContractModel> accessContractModelList = JsonHandler.getFromFileAsTypeReference(
            fileContracts,
            new TypeReference<>() {}
        );
        final RequestResponse<AccessContractModel> response = accessContractService.createContracts(
            accessContractModelList
        );

        final RequestResponseOK<AccessContractModel> responseCast = (RequestResponseOK<AccessContractModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);

        // We juste test the first contract
        final AccessContractModel acm = responseCast.getResults().iterator().next();
        assertThat(acm).isNotNull();

        String id1 = acm.getIdentifier();
        assertThat(id1).isNotNull();

        final AccessContractModel one = accessContractService.findByIdentifier(id1);

        assertThat(one).isNotNull();

        assertThat(one.getName()).isEqualTo(acm.getName());

        assertThat(one.getTenant()).isNotNull();
        assertThat(one.getTenant()).isEqualTo(Integer.valueOf(TENANT_ID));
    }

    /**
     * Access contract of tenant 1, try to get the same contract with id mongo but with tenant 2 This sgould not return
     * the contract as tenant 2 is not the owner of the access contract
     *
     * @throws Exception
     */
    @Test
    @RunWithCustomExecutor
    public void givenAccessContractsTestNotTenantOwner() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileContracts = PropertiesUtils.getResourceFile("contracts_access_ok.json");
        final List<AccessContractModel> accessContractModelList = JsonHandler.getFromFileAsTypeReference(
            fileContracts,
            new TypeReference<>() {}
        );
        final RequestResponse<AccessContractModel> response = accessContractService.createContracts(
            accessContractModelList
        );

        final RequestResponseOK<AccessContractModel> responseCast = (RequestResponseOK<AccessContractModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);

        // We juste test the first contract
        final AccessContractModel acm = responseCast.getResults().iterator().next();
        assertThat(acm).isNotNull();

        final String id1 = acm.getId();
        assertThat(id1).isNotNull();

        VitamThreadUtils.getVitamSession().setTenantId(2);

        final AccessContractModel one = accessContractService.findByIdentifier(id1);

        assertThat(one).isNull();
    }

    @Test
    @RunWithCustomExecutor
    public void givenAccessContractsTestfindByIdentifier() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileContracts = PropertiesUtils.getResourceFile("contracts_access_ok.json");
        final List<AccessContractModel> accessContractModelList = JsonHandler.getFromFileAsTypeReference(
            fileContracts,
            new TypeReference<>() {}
        );
        final RequestResponse<AccessContractModel> response = accessContractService.createContracts(
            accessContractModelList
        );

        final RequestResponseOK<AccessContractModel> responseCast = (RequestResponseOK<AccessContractModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);

        // We juste test the first contract
        final AccessContractModel acm = responseCast.getResults().iterator().next();
        assertThat(acm).isNotNull();

        String id1 = acm.getIdentifier();
        assertThat(id1).isNotNull();

        final AccessContractModel one = accessContractService.findByIdentifier(id1);

        assertThat(one).isNotNull();

        assertThat(one.getName()).isEqualTo(acm.getName());
    }

    @Test
    @RunWithCustomExecutor
    public void givenAccessContractsTestImportExternalIdentifierKO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(EXTERNAL_TENANT);
        final File fileContracts = PropertiesUtils.getResourceFile("contracts_access_ok.json");
        final List<AccessContractModel> accessContractModelList = JsonHandler.getFromFileAsTypeReference(
            fileContracts,
            new TypeReference<>() {}
        );
        final RequestResponse<AccessContractModel> response = accessContractService.createContracts(
            accessContractModelList
        );
        assertThat(response.isOk()).isFalse();

        ArgumentCaptor<LogbookOperationParameters> logbookOperationParametersCaptor = ArgumentCaptor.forClass(
            LogbookOperationParameters.class
        );
        verify(logbookOperationsClientMock, times(1)).create(logbookOperationParametersCaptor.capture());
        verify(logbookOperationsClientMock, times(1)).update(logbookOperationParametersCaptor.capture());
        List<LogbookOperationParameters> allLogbookOperationParameters =
            logbookOperationParametersCaptor.getAllValues();
        assertThat(allLogbookOperationParameters.size()).isEqualTo(2);
        assertThat(allLogbookOperationParameters.get(0).getStatus()).isEqualTo(StatusCode.STARTED);
        assertThat(allLogbookOperationParameters.get(0).getParameterValue(LogbookParameterName.eventType)).isEqualTo(
            "STP_IMPORT_ACCESS_CONTRACT"
        );
        assertThat(allLogbookOperationParameters.get(1).getStatus()).isEqualTo(StatusCode.KO);
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.eventType)).isEqualTo(
            "STP_IMPORT_ACCESS_CONTRACT"
        );
        assertThat(
            allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.outcomeDetail)
        ).isEqualTo("STP_IMPORT_ACCESS_CONTRACT.EMPTY_REQUIRED_FIELD.KO");
    }

    @Test
    @RunWithCustomExecutor
    public void givenAccessContractsTestImportExternalIdentifier() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(EXTERNAL_TENANT);
        final File fileContracts = PropertiesUtils.getResourceFile("contracts_access_ok_Identifier.json");
        final List<AccessContractModel> accessContractModelList = JsonHandler.getFromFileAsTypeReference(
            fileContracts,
            new TypeReference<>() {}
        );
        final RequestResponse<AccessContractModel> response = accessContractService.createContracts(
            accessContractModelList
        );
        assertThat(response.isOk()).isTrue();
    }

    @Test
    @RunWithCustomExecutor
    public void givenAccessContractsTestFindAllThenReturnEmpty() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final RequestResponseOK<AccessContractModel> accessContractModelList = accessContractService.findContracts(
            JsonHandler.createObjectNode()
        );
        assertThat(accessContractModelList.getResults()).isEmpty();
    }

    @Test
    @RunWithCustomExecutor
    public void givenAccessContractsTestFindAllThenReturnTwoContracts() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileContracts = PropertiesUtils.getResourceFile("contracts_access_ok.json");
        final List<AccessContractModel> accessContractModelList = JsonHandler.getFromFileAsTypeReference(
            fileContracts,
            new TypeReference<>() {}
        );
        final RequestResponse<AccessContractModel> response = accessContractService.createContracts(
            accessContractModelList
        );

        final RequestResponseOK<AccessContractModel> responseCast = (RequestResponseOK<AccessContractModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);

        final RequestResponseOK<AccessContractModel> accessContractModelListSearch =
            accessContractService.findContracts(JsonHandler.createObjectNode());
        assertThat(accessContractModelListSearch.getResults()).hasSize(2);
    }

    @Test
    @RunWithCustomExecutor
    public void givenAccessContractsTestFindByName() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileContracts = PropertiesUtils.getResourceFile("contracts_access_ok.json");
        final List<AccessContractModel> accessContractModelList = JsonHandler.getFromFileAsTypeReference(
            fileContracts,
            new TypeReference<>() {}
        );
        final RequestResponse<AccessContractModel> response = accessContractService.createContracts(
            accessContractModelList
        );

        final RequestResponseOK<AccessContractModel> responseCast = (RequestResponseOK<AccessContractModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);

        final AccessContractModel acm = accessContractModelList.iterator().next();
        assertThat(acm).isNotNull();

        final String id1 = acm.getId();
        assertThat(id1).isNotNull();

        final String name = acm.getName();
        assertThat(name).isNotNull();

        final SelectParserSingle parser = new SelectParserSingle(new SingleVarNameAdapter());
        final Select select = new Select();
        parser.parse(select.getFinalSelect());
        parser.addCondition(QueryHelper.eq("Identifier", acm.getIdentifier()));
        final JsonNode queryDsl = parser.getRequest().getFinalSelect();

        final RequestResponseOK<AccessContractModel> accessContractModelListFound = accessContractService.findContracts(
            queryDsl
        );
        assertThat(accessContractModelListFound.getResults()).hasSize(1);

        final AccessContractModel acmFound = accessContractModelListFound.getResults().iterator().next();
        assertThat(acmFound).isNotNull();

        assertThat(acmFound.getId()).isEqualTo(id1);
        assertThat(acmFound.getName()).isEqualTo(name);
    }

    @Test
    @RunWithCustomExecutor
    public void givenAccessContractsTestNotExistingRootUnits() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileContracts = PropertiesUtils.getResourceFile("contracts_access_not_exists_root_units.json");

        when(metaDataClientMock.selectUnits(any())).thenReturn(new RequestResponseOK<>().toJsonNode());
        final List<AccessContractModel> accessContractModelList = JsonHandler.getFromFileAsTypeReference(
            fileContracts,
            new TypeReference<>() {}
        );
        final RequestResponse<AccessContractModel> response = accessContractService.createContracts(
            accessContractModelList
        );

        assertThat(response.isOk()).isFalse();
        assertThat(response.toString()).contains("RootUnits (GUID1,GUID2,GUID3) not found in database");

        ArgumentCaptor<LogbookOperationParameters> logbookOperationParametersCaptor = ArgumentCaptor.forClass(
            LogbookOperationParameters.class
        );
        verify(logbookOperationsClientMock, times(1)).create(logbookOperationParametersCaptor.capture());
        verify(logbookOperationsClientMock, times(1)).update(logbookOperationParametersCaptor.capture());
        List<LogbookOperationParameters> allLogbookOperationParameters =
            logbookOperationParametersCaptor.getAllValues();
        assertThat(allLogbookOperationParameters.size()).isEqualTo(2);
        assertThat(allLogbookOperationParameters.get(0).getStatus()).isEqualTo(StatusCode.STARTED);
        assertThat(allLogbookOperationParameters.get(0).getParameterValue(LogbookParameterName.eventType)).isEqualTo(
            "STP_IMPORT_ACCESS_CONTRACT"
        );
        assertThat(allLogbookOperationParameters.get(1).getStatus()).isEqualTo(StatusCode.KO);
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.eventType)).isEqualTo(
            "STP_IMPORT_ACCESS_CONTRACT"
        );
        assertThat(
            allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.outcomeDetail)
        ).isEqualTo("STP_IMPORT_ACCESS_CONTRACT.VALIDATION_ERROR.KO");
    }

    @Test
    @RunWithCustomExecutor
    public void givenAccessContractsTestNotAllExistingRootUnits() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileContracts = PropertiesUtils.getResourceFile("contracts_access_not_exists_root_units.json");

        RequestResponseOK<JsonNode> res = new RequestResponseOK<>();
        res.addResult(JsonHandler.createObjectNode().put("#id", "GUID1"));
        res.addResult(JsonHandler.createObjectNode().put("#id", "GUID3"));

        when(metaDataClientMock.selectUnits(any())).thenReturn(res.toJsonNode());

        final List<AccessContractModel> accessContractModelList = JsonHandler.getFromFileAsTypeReference(
            fileContracts,
            new TypeReference<>() {}
        );
        final RequestResponse<AccessContractModel> response = accessContractService.createContracts(
            accessContractModelList
        );

        assertThat(response.isOk()).isFalse();
        assertThat(response.toString()).contains("RootUnits (GUID2) not found in database");

        ArgumentCaptor<LogbookOperationParameters> logbookOperationParametersCaptor = ArgumentCaptor.forClass(
            LogbookOperationParameters.class
        );
        verify(logbookOperationsClientMock, times(1)).create(logbookOperationParametersCaptor.capture());
        verify(logbookOperationsClientMock, times(1)).update(logbookOperationParametersCaptor.capture());
        List<LogbookOperationParameters> allLogbookOperationParameters =
            logbookOperationParametersCaptor.getAllValues();
        assertThat(allLogbookOperationParameters.size()).isEqualTo(2);
        assertThat(allLogbookOperationParameters.get(0).getStatus()).isEqualTo(StatusCode.STARTED);
        assertThat(allLogbookOperationParameters.get(0).getParameterValue(LogbookParameterName.eventType)).isEqualTo(
            "STP_IMPORT_ACCESS_CONTRACT"
        );
        assertThat(allLogbookOperationParameters.get(1).getStatus()).isEqualTo(StatusCode.KO);
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.eventType)).isEqualTo(
            "STP_IMPORT_ACCESS_CONTRACT"
        );
        assertThat(
            allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.outcomeDetail)
        ).isEqualTo("STP_IMPORT_ACCESS_CONTRACT.VALIDATION_ERROR.KO");
    }

    @Test
    @RunWithCustomExecutor
    public void givenAccessContractsTestExistingRootUnitsOK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileContracts = PropertiesUtils.getResourceFile("contracts_access_ok_root_units.json");

        RequestResponseOK<JsonNode> res = new RequestResponseOK<>();
        res.addResult(JsonHandler.createObjectNode().put("#id", "GUID1"));
        res.addResult(JsonHandler.createObjectNode().put("#id", "GUID2"));
        res.addResult(JsonHandler.createObjectNode().put("#id", "GUID3"));

        when(metaDataClientMock.selectUnits(any())).thenReturn(res.toJsonNode());
        final List<AccessContractModel> accessContractModelList = JsonHandler.getFromFileAsTypeReference(
            fileContracts,
            new TypeReference<>() {}
        );
        final RequestResponse<AccessContractModel> response = accessContractService.createContracts(
            accessContractModelList
        );

        assertThat(response.isOk()).isTrue();
        assertThat(((RequestResponseOK<AccessContractModel>) response).getResults()).hasSize(2);
        assertThat(((RequestResponseOK<AccessContractModel>) response).getResults().get(0).getName()).contains("aName");
        assertThat(((RequestResponseOK<AccessContractModel>) response).getResults().get(1).getName()).contains(
            "aName1"
        );
    }

    @Test
    @RunWithCustomExecutor
    public void givenAccessContractsTestEmptyRootUnitsAndExcludedRootUnitsOK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileContracts = PropertiesUtils.getResourceFile("contracts_access_empty_root_units.json");

        final List<AccessContractModel> accessContractModelList = JsonHandler.getFromFileAsTypeReference(
            fileContracts,
            new TypeReference<>() {}
        );
        final RequestResponse<AccessContractModel> response = accessContractService.createContracts(
            accessContractModelList
        );

        assertThat(response.isOk()).isTrue();
        assertThat(((RequestResponseOK<AccessContractModel>) response).getResults()).hasSize(2);
        assertThat(((RequestResponseOK<AccessContractModel>) response).getResults().get(0).getName()).contains("aName");
        assertThat(((RequestResponseOK<AccessContractModel>) response).getResults().get(1).getName()).contains(
            "aName1"
        );
    }

    @Test
    @RunWithCustomExecutor
    public void givenAccessContractsTestNotExistingExcludedRootUnits() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileContracts = PropertiesUtils.getResourceFile(
            "contracts_access_not_exists_excluded_root_units.json"
        );

        when(metaDataClientMock.selectUnits(any())).thenReturn(new RequestResponseOK<>().toJsonNode());
        final List<AccessContractModel> accessContractModelList = JsonHandler.getFromFileAsTypeReference(
            fileContracts,
            new TypeReference<>() {}
        );
        final RequestResponse<AccessContractModel> response = accessContractService.createContracts(
            accessContractModelList
        );

        assertThat(response.isOk()).isFalse();
        assertThat(response.toString()).contains("RootUnits (GUID1,GUID2,GUID3) not found in database");

        ArgumentCaptor<LogbookOperationParameters> logbookOperationParametersCaptor = ArgumentCaptor.forClass(
            LogbookOperationParameters.class
        );
        verify(logbookOperationsClientMock, times(1)).create(logbookOperationParametersCaptor.capture());
        verify(logbookOperationsClientMock, times(1)).update(logbookOperationParametersCaptor.capture());
        List<LogbookOperationParameters> allLogbookOperationParameters =
            logbookOperationParametersCaptor.getAllValues();
        assertThat(allLogbookOperationParameters.size()).isEqualTo(2);
        assertThat(allLogbookOperationParameters.get(0).getStatus()).isEqualTo(StatusCode.STARTED);
        assertThat(allLogbookOperationParameters.get(0).getParameterValue(LogbookParameterName.eventType)).isEqualTo(
            "STP_IMPORT_ACCESS_CONTRACT"
        );
        assertThat(allLogbookOperationParameters.get(1).getStatus()).isEqualTo(StatusCode.KO);
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.eventType)).isEqualTo(
            "STP_IMPORT_ACCESS_CONTRACT"
        );
        assertThat(
            allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.outcomeDetail)
        ).isEqualTo("STP_IMPORT_ACCESS_CONTRACT.VALIDATION_ERROR.KO");
    }

    @Test
    @RunWithCustomExecutor
    public void givenAccessContractsTestNotExistingBothRootUnitsAndExcludedRootUnits() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileContracts = PropertiesUtils.getResourceFile(
            "contracts_access_not_existing_rootunits_and_excludedrootunits.json"
        );

        when(metaDataClientMock.selectUnits(any())).thenReturn(new RequestResponseOK<>().toJsonNode());
        final List<AccessContractModel> accessContractModelList = JsonHandler.getFromFileAsTypeReference(
            fileContracts,
            new TypeReference<>() {}
        );
        final RequestResponse<AccessContractModel> response = accessContractService.createContracts(
            accessContractModelList
        );

        assertThat(response.isOk()).isFalse();
        assertEquals(
            ((VitamError<AccessContractModel>) response).getErrors().get(0).getMessage(),
            "STP_IMPORT_ACCESS_CONTRACT.VALIDATION_ERROR.KO"
        );
        assertThat(response.toString()).contains(
            "ExcludedRootUnits and RootUnits (GUID11,GUID22,GUID33,GUID1,GUID2,GUID3) not found in database"
        );

        ArgumentCaptor<LogbookOperationParameters> logbookOperationParametersCaptor = ArgumentCaptor.forClass(
            LogbookOperationParameters.class
        );
        verify(logbookOperationsClientMock, times(1)).create(logbookOperationParametersCaptor.capture());
        verify(logbookOperationsClientMock, times(1)).update(logbookOperationParametersCaptor.capture());
        List<LogbookOperationParameters> allLogbookOperationParameters =
            logbookOperationParametersCaptor.getAllValues();
        assertThat(allLogbookOperationParameters.size()).isEqualTo(2);
        assertThat(allLogbookOperationParameters.get(0).getStatus()).isEqualTo(StatusCode.STARTED);
        assertThat(allLogbookOperationParameters.get(0).getParameterValue(LogbookParameterName.eventType)).isEqualTo(
            "STP_IMPORT_ACCESS_CONTRACT"
        );
        assertThat(allLogbookOperationParameters.get(1).getStatus()).isEqualTo(StatusCode.KO);
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.eventType)).isEqualTo(
            "STP_IMPORT_ACCESS_CONTRACT"
        );
        assertThat(
            allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.outcomeDetail)
        ).isEqualTo("STP_IMPORT_ACCESS_CONTRACT.VALIDATION_ERROR.KO");
    }

    @Test
    @RunWithCustomExecutor
    public void givenAccessContractsTestExistingExcludedRootUnitsOK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileContracts = PropertiesUtils.getResourceFile("contracts_access_ok_excluded_root_units.json");

        RequestResponseOK<JsonNode> res = new RequestResponseOK<>();
        res.addResult(JsonHandler.createObjectNode().put("#id", "GUID1"));
        res.addResult(JsonHandler.createObjectNode().put("#id", "GUID2"));
        res.addResult(JsonHandler.createObjectNode().put("#id", "GUID3"));

        when(metaDataClientMock.selectUnits(any())).thenReturn(res.toJsonNode());
        final List<AccessContractModel> accessContractModelList = JsonHandler.getFromFileAsTypeReference(
            fileContracts,
            new TypeReference<>() {}
        );
        final RequestResponse<AccessContractModel> response = accessContractService.createContracts(
            accessContractModelList
        );

        assertThat(response.isOk()).isTrue();
        assertThat(((RequestResponseOK<AccessContractModel>) response).getResults()).hasSize(2);
        assertThat(((RequestResponseOK<AccessContractModel>) response).getResults().get(0).getName()).contains("aName");
        assertThat(((RequestResponseOK<AccessContractModel>) response).getResults().get(1).getName()).contains(
            "aName1"
        );
    }

    @Test
    @RunWithCustomExecutor
    public void givenAccessContractsTestOriginatingAgenciesNotExistsThenKO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(EXTERNAL_TENANT);
        final File fileContracts = PropertiesUtils.getResourceFile("contracts_access_not_exists_agencies.json");
        final List<AccessContractModel> accessContractModelList = JsonHandler.getFromFileAsTypeReference(
            fileContracts,
            new TypeReference<>() {}
        );
        final RequestResponse<AccessContractModel> response = accessContractService.createContracts(
            accessContractModelList
        );
        assertThat(response.isOk()).isFalse();

        ArgumentCaptor<LogbookOperationParameters> logbookOperationParametersCaptor = ArgumentCaptor.forClass(
            LogbookOperationParameters.class
        );
        verify(logbookOperationsClientMock, times(1)).create(logbookOperationParametersCaptor.capture());
        verify(logbookOperationsClientMock, times(1)).update(logbookOperationParametersCaptor.capture());
        List<LogbookOperationParameters> allLogbookOperationParameters =
            logbookOperationParametersCaptor.getAllValues();
        assertThat(allLogbookOperationParameters.size()).isEqualTo(2);
        assertThat(allLogbookOperationParameters.get(0).getStatus()).isEqualTo(StatusCode.STARTED);
        assertThat(allLogbookOperationParameters.get(0).getParameterValue(LogbookParameterName.eventType)).isEqualTo(
            "STP_IMPORT_ACCESS_CONTRACT"
        );
        assertThat(allLogbookOperationParameters.get(1).getStatus()).isEqualTo(StatusCode.KO);
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.eventType)).isEqualTo(
            "STP_IMPORT_ACCESS_CONTRACT"
        );
        assertThat(
            allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.outcomeDetail)
        ).isEqualTo("STP_IMPORT_ACCESS_CONTRACT.AGENCY_NOT_FOUND.KO");
    }

    @Test
    @RunWithCustomExecutor
    public void givenAccessContractTestUpdateAccessContractOriginatingAgencyNotExistsThenKO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final String documentName = "aName";
        final String activeStatus = "ACTIVE";

        // Create document
        final File fileContracts = PropertiesUtils.getResourceFile("contracts_access_no_agencies.json");

        final List<AccessContractModel> accessContractModelList = JsonHandler.getFromFileAsTypeReference(
            fileContracts,
            new TypeReference<>() {}
        );
        final RequestResponse<AccessContractModel> response = accessContractService.createContracts(
            accessContractModelList
        );

        RequestResponseOK<AccessContractModel> responseCast = (RequestResponseOK<AccessContractModel>) response;
        assertThat(responseCast.getResults()).hasSize(1);

        final SelectParserSingle parser = new SelectParserSingle(new SingleVarNameAdapter());
        final Select select = new Select();
        parser.parse(select.getFinalSelect());
        parser.addCondition(QueryHelper.eq(NAME, documentName));
        final JsonNode queryDsl = parser.getRequest().getFinalSelect();
        responseCast = accessContractService.findContracts(queryDsl);
        assertThat(responseCast.getResults()).isNotEmpty();
        for (final AccessContractModel accessContractModel : responseCast.getResults()) {
            assertEquals(activeStatus, accessContractModel.getStatus().name());
        }

        // Test update existing originatingAgencies
        final String now = LocalDateUtil.nowFormatted();
        UpdateParserSingle updateParser = new UpdateParserSingle(new SingleVarNameAdapter());
        List<String> agencies = new ArrayList<>();
        agencies.add("FR_ORG_AGEN");
        final SetAction setActionStatusInactive = UpdateActionHelper.set(
            AccessContractModel.ORIGINATING_AGENCIES,
            agencies
        );
        final SetAction setActionLastUpdateInactive = UpdateActionHelper.set("LastUpdate", now);
        Update update = new Update();
        update.setQuery(QueryHelper.eq(NAME, documentName));
        update.addActions(setActionStatusInactive, setActionLastUpdateInactive);
        updateParser.parse(update.getFinalUpdate());
        JsonNode queryDslForUpdate = updateParser.getRequest().getFinalUpdate();
        RequestResponse<AccessContractModel> updateContractStatus = accessContractService.updateContract(
            accessContractModelList.get(0).getIdentifier(),
            queryDslForUpdate
        );
        assertThat(updateContractStatus.isOk()).isTrue();

        updateParser = new UpdateParserSingle(new SingleVarNameAdapter());
        agencies = new ArrayList<>();
        agencies.add("NotExistingOriginatingAgencies");
        update = new Update();
        update.setQuery(QueryHelper.eq(NAME, documentName));
        update.addActions(
            UpdateActionHelper.set(AccessContractModel.ORIGINATING_AGENCIES, agencies),
            UpdateActionHelper.set("LastUpdate", now)
        );
        updateParser.parse(update.getFinalUpdate());
        updateContractStatus = accessContractService.updateContract(
            accessContractModelList.get(0).getIdentifier(),
            updateParser.getRequest().getFinalUpdate()
        );
        assertThat(updateContractStatus.isOk()).isFalse();
    }

    @Test
    @RunWithCustomExecutor
    public void givenAccessContractTestValidateSchemaBeforeUpdateAccessContract() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final String documentName = "aName";
        // Create document
        final File fileContracts = PropertiesUtils.getResourceFile("contracts_access_ok.json");

        final List<AccessContractModel> accessContractModelList = JsonHandler.getFromFileAsTypeReference(
            fileContracts,
            new TypeReference<>() {}
        );
        final RequestResponse<AccessContractModel> response = accessContractService.createContracts(
            accessContractModelList
        );

        final RequestResponseOK<AccessContractModel> responseCast = (RequestResponseOK<AccessContractModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);

        final SelectParserSingle parser = new SelectParserSingle(new SingleVarNameAdapter());
        final Select select = new Select();
        parser.parse(select.getFinalSelect());
        parser.addCondition(QueryHelper.eq(NAME, documentName));
        final JsonNode queryDsl = parser.getRequest().getFinalSelect();
        final RequestResponseOK<AccessContractModel> accessContractModelList2 = accessContractService.findContracts(
            queryDsl
        );
        assertThat(accessContractModelList2.getResults()).isNotEmpty();
        for (final AccessContractModel accessContractModel : accessContractModelList2.getResults()) {
            assertEquals(ActivationStatus.ACTIVE, accessContractModel.getStatus());
        }

        // Test invalid status
        final UpdateParserSingle updateParser = new UpdateParserSingle(new SingleVarNameAdapter());
        final SetAction setActionStatusInactive = UpdateActionHelper.set("Status", "INVALID_STATUS");
        final Update update = new Update();
        update.setQuery(QueryHelper.eq(NAME, documentName));
        update.addActions(setActionStatusInactive);
        updateParser.parse(update.getFinalUpdate());
        JsonNode queryDslForUpdate = updateParser.getRequest().getFinalUpdate();

        RequestResponse<AccessContractModel> updateContractStatus = accessContractService.updateContract(
            accessContractModelList.get(0).getIdentifier(),
            queryDslForUpdate
        );
        assertEquals(updateContractStatus.getStatus(), Response.Status.BAD_REQUEST.getStatusCode());
        assertThat(updateContractStatus).isInstanceOf(VitamError.class);
        List<VitamError<AccessContractModel>> errors =
            ((VitamError<AccessContractModel>) updateContractStatus).getErrors();
        assertThat(
            errors
                .get(0)
                .getDescription()
                .equals("The Access contract status must be ACTIVE or INACTIVE but not INVALID_STATUS")
        ).isTrue();

        // Test unset lastUpdate
        final UnsetAction unsetActionLastUpdate = UpdateActionHelper.unset("LastUpdate");
        final UpdateParserSingle updateParser2 = new UpdateParserSingle(new SingleVarNameAdapter());
        final Update update2 = new Update();
        update2.setQuery(QueryHelper.eq(NAME, documentName));
        update2.addActions(unsetActionLastUpdate);
        updateParser2.parse(update2.getFinalUpdate());
        final JsonNode queryDslForUpdate2 = updateParser2.getRequest().getFinalUpdate();

        updateContractStatus = accessContractService.updateContract(
            accessContractModelList.get(0).getIdentifier(),
            queryDslForUpdate2
        );
        assertEquals(updateContractStatus.getStatus(), Response.Status.BAD_REQUEST.getStatusCode());
        assertThat(updateContractStatus).isInstanceOf(VitamError.class);
        String errorDescription = ((VitamError<AccessContractModel>) updateContractStatus).getDescription();
        assertThat(errorDescription.contains("object has missing required properties ([\\\"LastUpdate\\\"])")).isTrue();

        // Test invalid property
        final SetAction setActionInvalidProperty = UpdateActionHelper.set("InvalidProp", "prop value");
        final UpdateParserSingle updateParser3 = new UpdateParserSingle(new SingleVarNameAdapter());
        final Update update3 = new Update();
        update3.setQuery(QueryHelper.eq(NAME, documentName));
        update3.addActions(setActionInvalidProperty);
        updateParser3.parse(update3.getFinalUpdate());
        final JsonNode queryDslForUpdate3 = updateParser3.getRequest().getFinalUpdate();

        updateContractStatus = accessContractService.updateContract(
            accessContractModelList.get(0).getIdentifier(),
            queryDslForUpdate3
        );
        assertEquals(updateContractStatus.getStatus(), Response.Status.BAD_REQUEST.getStatusCode());
        assertThat(updateContractStatus).isInstanceOf(VitamError.class);
        errorDescription = ((VitamError<AccessContractModel>) updateContractStatus).getDescription();
        assertThat(
            errorDescription.contains(
                "object instance has properties which are not allowed by the schema: [\\\"InvalidProp\\\"]"
            )
        ).isTrue();
    }

    @Test
    @RunWithCustomExecutor
    public void givenAccessContractsWithInexistantUsageThenKO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        // Given
        File fileContractsKO = PropertiesUtils.getResourceFile("KO_contract_access_usage_inexistant.json");
        List<AccessContractModel> accessContractModelListKO = JsonHandler.getFromFileAsTypeReference(
            fileContractsKO,
            new TypeReference<>() {}
        );
        RequestResponseOK<FunctionalOperationModel> tempsResult = new RequestResponseOK<
            FunctionalOperationModel
        >().addResult(new FunctionalOperationModel().setEvId("evId").setEvDateTime("evDateTime").setEvType("evType"));
        when(logbookOperationsClientMock.selectOperationById(any())).thenReturn(tempsResult.toJsonNode());
        // When
        RequestResponse<AccessContractModel> response = accessContractService.createContracts(
            accessContractModelListKO
        );

        // Then
        assertThat(response).isInstanceOf(VitamError.class);
        assertThat(response.toString()).contains("Import access contracts error");
        assertThat(response.toString()).contains("Document schema validation failed");
        assertThat(response.toString()).contains("toto");

        ArgumentCaptor<LogbookOperationParameters> logbookOperationParametersCaptor = ArgumentCaptor.forClass(
            LogbookOperationParameters.class
        );
        verify(logbookOperationsClientMock, times(1)).create(logbookOperationParametersCaptor.capture());
        verify(logbookOperationsClientMock, times(1)).update(logbookOperationParametersCaptor.capture());
        List<LogbookOperationParameters> allLogbookOperationParameters =
            logbookOperationParametersCaptor.getAllValues();
        assertThat(allLogbookOperationParameters.size()).isEqualTo(2);
        assertThat(allLogbookOperationParameters.get(0).getStatus()).isEqualTo(StatusCode.STARTED);
        assertThat(allLogbookOperationParameters.get(0).getParameterValue(LogbookParameterName.eventType)).isEqualTo(
            "STP_IMPORT_ACCESS_CONTRACT"
        );
        assertThat(allLogbookOperationParameters.get(1).getStatus()).isEqualTo(StatusCode.KO);
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.eventType)).isEqualTo(
            "STP_IMPORT_ACCESS_CONTRACT"
        );
        assertThat(
            allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.outcomeDetail)
        ).isEqualTo("STP_IMPORT_ACCESS_CONTRACT.BAD_REQUEST.KO");
    }
}
