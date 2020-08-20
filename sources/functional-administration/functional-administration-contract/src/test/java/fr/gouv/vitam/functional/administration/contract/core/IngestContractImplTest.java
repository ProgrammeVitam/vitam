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
package fr.gouv.vitam.functional.administration.contract.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.action.SetAction;
import fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.builder.request.single.Update;
import fr.gouv.vitam.common.database.parser.request.adapter.SingleVarNameAdapter;
import fr.gouv.vitam.common.database.parser.request.single.SelectParserSingle;
import fr.gouv.vitam.common.database.parser.request.single.UpdateParserSingle;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.ActivationStatus;
import fr.gouv.vitam.common.model.administration.FileFormatModel;
import fr.gouv.vitam.common.model.administration.IngestContractModel;
import fr.gouv.vitam.common.model.administration.ManagementContractModel;
import fr.gouv.vitam.common.model.administration.ProfileModel;
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
import fr.gouv.vitam.functional.administration.common.FunctionalBackupService;
import fr.gouv.vitam.functional.administration.common.config.ElasticsearchFunctionalAdminIndexManager;
import fr.gouv.vitam.functional.administration.common.counter.VitamCounterService;
import fr.gouv.vitam.functional.administration.common.server.ElasticsearchAccessFunctionalAdmin;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollectionsTestUtils;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminFactory;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.functional.administration.contract.api.ContractService;
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
import org.mockito.Mockito;

import javax.ws.rs.core.Response;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fr.gouv.vitam.common.database.collections.VitamCollection.getMongoClientOptions;
import static fr.gouv.vitam.common.guid.GUIDFactory.newOperationLogbookGUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThatCode;

public class IngestContractImplTest {

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor());

    private static final Integer TENANT_ID = 1;
    private static final Integer EXTERNAL_TENANT = 2;
    private static final String DATABASE_HOST = "localhost";
    private static final String DATABASE_NAME = "vitam-test";
    private static final String FORMAT_FILE = "file-format-light.json";

    public static final String PREFIX = GUIDFactory.newGUID().getId();

    @ClassRule
    public static MongoRule mongoRule =
        new MongoRule(getMongoClientOptions(Lists.newArrayList(AccessContract.class, Agencies.class)));

    @ClassRule
    public static ElasticsearchRule elasticsearchRule = new ElasticsearchRule();


    static VitamCounterService vitamCounterService;
    static MetaDataClient metaDataClientMock;
    static LogbookOperationsClient logbookOperationsClientMock;
    static FunctionalBackupService functionalBackupService;
    static ContractService<ManagementContractModel> managementContractService;

    static ContractService<IngestContractModel> ingestContractService;
    private static MongoDbAccessAdminImpl dbImpl;

    private static final ElasticsearchFunctionalAdminIndexManager indexManager =
        FunctionalAdminCollectionsTestUtils.createTestIndexManager();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        FunctionalAdminCollectionsTestUtils.beforeTestClass(mongoRule.getMongoDatabase(), PREFIX,
            new ElasticsearchAccessFunctionalAdmin(ElasticsearchRule.VITAM_CLUSTER,
                Arrays.asList(new ElasticsearchNode(ElasticsearchRule.getHost(), ElasticsearchRule.getPort())),
                indexManager),
            Arrays.asList(FunctionalAdminCollections.INGEST_CONTRACT, FunctionalAdminCollections.AGENCIES));
        final List<MongoDbNode> nodes = new ArrayList<>();
        nodes.add(new MongoDbNode(DATABASE_HOST, mongoRule.getDataBasePort()));
        dbImpl =
            MongoDbAccessAdminFactory.create(new DbConfigurationImpl(nodes, DATABASE_NAME), Collections::emptyList, indexManager);
        final List<Integer> tenants = Arrays.asList(TENANT_ID, EXTERNAL_TENANT);
        Map<Integer, List<String>> listEnableExternalIdentifiers = new HashMap<>();
        List<String> list_tenant = new ArrayList<>();
        list_tenant.add("INGEST_CONTRACT");
        listEnableExternalIdentifiers.put(EXTERNAL_TENANT, list_tenant);


        vitamCounterService = new VitamCounterService(dbImpl, tenants, listEnableExternalIdentifiers);
        LogbookOperationsClientFactory.changeMode(null);

        metaDataClientMock = mock(MetaDataClient.class);

        logbookOperationsClientMock = mock(LogbookOperationsClient.class);

        functionalBackupService = mock(FunctionalBackupService.class);

        managementContractService = mock(ManagementContractImpl.class);

        ingestContractService =
            new IngestContractImpl(dbImpl, vitamCounterService, metaDataClientMock, logbookOperationsClientMock,
                functionalBackupService, managementContractService);

    }

    @AfterClass
    public static void tearDownAfterClass() {
        FunctionalAdminCollectionsTestUtils.afterTestClass(true);
        ingestContractService.close();
        VitamClientFactory.resetConnections();
    }

    @Before
    public void setUp() {
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(TENANT_ID));
        reset(logbookOperationsClientMock);
        reset(metaDataClientMock);
    }

    @After
    public void afterTest() {
        FunctionalAdminCollectionsTestUtils.afterTest();
        Mockito.reset(functionalBackupService);
    }


    @Test
    @RunWithCustomExecutor
    public void givenIngestContractsTestWellFormedContractThenImportSuccessfully() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileContracts = PropertiesUtils.getResourceFile("referential_contracts_ok.json");
        final List<IngestContractModel> IngestContractModelList =
            JsonHandler.getFromFileAsTypeReference(fileContracts, new TypeReference<List<IngestContractModel>>() {
            });
        final RequestResponse<IngestContractModel> response =
            ingestContractService.createContracts(IngestContractModelList);

        assertThat(response.isOk()).isTrue();
        final RequestResponseOK<IngestContractModel> responseCast = (RequestResponseOK<IngestContractModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);
        assertThat(responseCast.getResults().get(0).getIdentifier()).contains("IC-000");
        assertThat(responseCast.getResults().get(1).getIdentifier()).contains("IC-000");

        verify(functionalBackupService).saveCollectionAndSequence(any(), eq(IngestContractImpl.CONTRACT_BACKUP_EVENT),
            eq(FunctionalAdminCollections.INGEST_CONTRACT), any());

        ArgumentCaptor<LogbookOperationParameters> logbookOperationParametersCaptor =
            ArgumentCaptor.forClass(LogbookOperationParameters.class);
        verify(logbookOperationsClientMock, times(1)).create(logbookOperationParametersCaptor.capture());
        verify(logbookOperationsClientMock, times(1)).update(logbookOperationParametersCaptor.capture());
        List<LogbookOperationParameters> allLogbookOperationParameters = logbookOperationParametersCaptor
            .getAllValues();
        assertThat(allLogbookOperationParameters.size()).isEqualTo(2);
        assertThat(allLogbookOperationParameters.get(0).getStatus()).isEqualTo(StatusCode.STARTED);
        assertThat(allLogbookOperationParameters.get(0).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_IMPORT_INGEST_CONTRACT");
        assertThat(allLogbookOperationParameters.get(1).getStatus()).isEqualTo(StatusCode.OK);
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_IMPORT_INGEST_CONTRACT");
    }

    @Test
    @RunWithCustomExecutor
    public void givenIngestContractsTestComputeInheritedRulesAtIngestThenImportSuccessfully() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileContracts = PropertiesUtils.getResourceFile(
            "referential_contracts_ok_ComputeInheritedRulesAtIngest.json");
        final List<IngestContractModel> IngestContractModelList =
            JsonHandler.getFromFileAsTypeReference(fileContracts, new TypeReference<List<IngestContractModel>>() {
            });
        final RequestResponse<IngestContractModel> response =
            ingestContractService.createContracts(IngestContractModelList);

        assertThat(response.isOk()).isTrue();
        final RequestResponseOK<IngestContractModel> responseCast = (RequestResponseOK<IngestContractModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);
        assertThat(responseCast.getResults().get(0).getIdentifier()).contains("IC-000");
        assertThat(responseCast.getResults().get(1).getIdentifier()).contains("IC-000");

        verify(functionalBackupService).saveCollectionAndSequence(any(), eq(IngestContractImpl.CONTRACT_BACKUP_EVENT),
            eq(FunctionalAdminCollections.INGEST_CONTRACT), any());

        ArgumentCaptor<LogbookOperationParameters> logbookOperationParametersCaptor =
            ArgumentCaptor.forClass(LogbookOperationParameters.class);
        verify(logbookOperationsClientMock, times(1)).create(logbookOperationParametersCaptor.capture());
        verify(logbookOperationsClientMock, times(1)).update(logbookOperationParametersCaptor.capture());
        List<LogbookOperationParameters> allLogbookOperationParameters = logbookOperationParametersCaptor
            .getAllValues();
        assertThat(allLogbookOperationParameters.size()).isEqualTo(2);
        assertThat(allLogbookOperationParameters.get(0).getStatus()).isEqualTo(StatusCode.STARTED);
        assertThat(allLogbookOperationParameters.get(0).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_IMPORT_INGEST_CONTRACT");
        assertThat(allLogbookOperationParameters.get(1).getStatus()).isEqualTo(StatusCode.OK);
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_IMPORT_INGEST_CONTRACT");
    }

    @Test
    @RunWithCustomExecutor
    public void givenIngestContractsTestMissingNameReturnBadRequest() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileContracts = PropertiesUtils.getResourceFile("referential_contracts_missingName.json");
        final List<IngestContractModel> IngestContractModelList =
            JsonHandler.getFromFileAsTypeReference(fileContracts, new TypeReference<List<IngestContractModel>>() {
            });
        final RequestResponse<IngestContractModel> response =
            ingestContractService.createContracts(IngestContractModelList);

        assertThat(response.isOk()).isFalse();

        verifyNoMoreInteractions(functionalBackupService);

        ArgumentCaptor<LogbookOperationParameters> logbookOperationParametersCaptor =
            ArgumentCaptor.forClass(LogbookOperationParameters.class);
        verify(logbookOperationsClientMock, times(1)).create(logbookOperationParametersCaptor.capture());
        verify(logbookOperationsClientMock, times(1)).update(logbookOperationParametersCaptor.capture());
        List<LogbookOperationParameters> allLogbookOperationParameters = logbookOperationParametersCaptor
            .getAllValues();
        assertThat(allLogbookOperationParameters.size()).isEqualTo(2);
        assertThat(allLogbookOperationParameters.get(0).getStatus()).isEqualTo(StatusCode.STARTED);
        assertThat(allLogbookOperationParameters.get(0).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_IMPORT_INGEST_CONTRACT");
        assertThat(allLogbookOperationParameters.get(1).getStatus()).isEqualTo(StatusCode.KO);
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_IMPORT_INGEST_CONTRACT");
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.outcomeDetail))
            .isEqualTo("STP_IMPORT_INGEST_CONTRACT.EMPTY_REQUIRED_FIELD.KO");

    }

    @Test
    @RunWithCustomExecutor
    public void givenIngestContractsTestProfileNotInDBReturnBadRequest() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileContracts = PropertiesUtils.getResourceFile("referential_contracts_profile_not_indb.json");
        final List<IngestContractModel> IngestContractModelList =
            JsonHandler.getFromFileAsTypeReference(fileContracts, new TypeReference<List<IngestContractModel>>() {
            });
        final RequestResponse<IngestContractModel> response =
            ingestContractService.createContracts(IngestContractModelList);

        assertThat(response.isOk()).isFalse();

        ArgumentCaptor<LogbookOperationParameters> logbookOperationParametersCaptor =
            ArgumentCaptor.forClass(LogbookOperationParameters.class);
        verify(logbookOperationsClientMock, times(1)).create(logbookOperationParametersCaptor.capture());
        verify(logbookOperationsClientMock, times(1)).update(logbookOperationParametersCaptor.capture());
        List<LogbookOperationParameters> allLogbookOperationParameters = logbookOperationParametersCaptor
            .getAllValues();
        assertThat(allLogbookOperationParameters.size()).isEqualTo(2);
        assertThat(allLogbookOperationParameters.get(0).getStatus()).isEqualTo(StatusCode.STARTED);
        assertThat(allLogbookOperationParameters.get(0).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_IMPORT_INGEST_CONTRACT");
        assertThat(allLogbookOperationParameters.get(1).getStatus()).isEqualTo(StatusCode.KO);
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_IMPORT_INGEST_CONTRACT");
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.outcomeDetail))
            .isEqualTo("STP_IMPORT_INGEST_CONTRACT.PROFILE_NOT_FOUND.KO");
    }

    @Test
    @RunWithCustomExecutor
    public void testObjectNode() throws InvalidParseOperationException {
        assertThatCode(() -> {
            final ArrayNode object = JsonHandler.createArrayNode();
            final ObjectNode msg = JsonHandler.createObjectNode();
            msg.put("Status", "update");
            msg.put("oldStatus", "INACTIF");
            msg.put("newStatus", "ACTIF");
            final ObjectNode msg2 = JsonHandler.createObjectNode();
            msg2.put("LinkParentId", "update");
            msg2.put("oldLinkParentId", "lqskdfjh");
            msg2.put("newLinkParentId", "lqskdfjh");
            object.add(msg);
            object.add(msg2);
            SanityChecker.sanitizeJson(object);
        }).doesNotThrowAnyException();
    }


    @Test
    @RunWithCustomExecutor
    public void givenIngestContractsTestProfileInDBReturnOK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileMetadataProfile = PropertiesUtils.getResourceFile("profile_ok.json");

        final List<ProfileModel> profileModelList =
            JsonHandler.getFromFileAsTypeReference(fileMetadataProfile, new TypeReference<List<ProfileModel>>() {
            });

        dbImpl.insertDocuments(
            JsonHandler.createArrayNode().add(JsonHandler.toJsonNode(profileModelList.iterator().next())),
            FunctionalAdminCollections.PROFILE).close();

        final File fileContracts = PropertiesUtils.getResourceFile("referential_contracts_profile_indb.json");
        final List<IngestContractModel> IngestContractModelList =
            JsonHandler.getFromFileAsTypeReference(fileContracts, new TypeReference<List<IngestContractModel>>() {
            });
        final RequestResponse<IngestContractModel> response =
            ingestContractService.createContracts(IngestContractModelList);

        assertThat(response.isOk()).isTrue();
        final RequestResponseOK<IngestContractModel> responseCast = (RequestResponseOK<IngestContractModel>) response;
        assertThat(responseCast.getResults()).hasSize(1);
    }

    @Test
    @RunWithCustomExecutor
    public void givenIngestContractsTestNotAllowedNotNullIdInCreation() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileContracts = PropertiesUtils.getResourceFile("referential_contracts_ok.json");

        final List<IngestContractModel> ingestContractModelList =
            JsonHandler.getFromFileAsTypeReference(fileContracts, new TypeReference<List<IngestContractModel>>() {
            });
        ingestContractModelList.get(0).setId(GUIDFactory.newGUID().getId());
        RequestResponse<IngestContractModel> response = ingestContractService.createContracts(ingestContractModelList);
        assertThat(response.isOk()).isFalse();

        ArgumentCaptor<LogbookOperationParameters> logbookOperationParametersCaptor =
            ArgumentCaptor.forClass(LogbookOperationParameters.class);
        verify(logbookOperationsClientMock, times(1)).create(logbookOperationParametersCaptor.capture());
        verify(logbookOperationsClientMock, times(1)).update(logbookOperationParametersCaptor.capture());
        List<LogbookOperationParameters> allLogbookOperationParameters = logbookOperationParametersCaptor
            .getAllValues();
        assertThat(allLogbookOperationParameters.size()).isEqualTo(2);
        assertThat(allLogbookOperationParameters.get(0).getStatus()).isEqualTo(StatusCode.STARTED);
        assertThat(allLogbookOperationParameters.get(0).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_IMPORT_INGEST_CONTRACT");
        assertThat(allLogbookOperationParameters.get(1).getStatus()).isEqualTo(StatusCode.KO);
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_IMPORT_INGEST_CONTRACT");
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.outcomeDetail))
            .isEqualTo("IngestContract service error");
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.eventDetailData))
            .contains("ingestContractCheck").contains("Id must be null when creating contracts (aName)");
    }


    @Test
    @RunWithCustomExecutor
    public void givenIngestContractsTestSameName() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileContracts = PropertiesUtils.getResourceFile("referential_contracts_ok.json");

        List<IngestContractModel> IngestContractModelList =
            JsonHandler.getFromFileAsTypeReference(fileContracts, new TypeReference<List<IngestContractModel>>() {
            });
        RequestResponse<IngestContractModel> response = ingestContractService.createContracts(IngestContractModelList);

        final RequestResponseOK<IngestContractModel> responseCast = (RequestResponseOK<IngestContractModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);


        // unset ids
        IngestContractModelList =
            JsonHandler.getFromFileAsTypeReference(fileContracts, new TypeReference<List<IngestContractModel>>() {
            });
        response = ingestContractService.createContracts(IngestContractModelList);

        assertThat(response.isOk()).isTrue();
    }


    @Test
    @RunWithCustomExecutor
    public void givenIngestContractTestFindByFakeID() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        // find ingestContract with the fake id should return Status.OK

        final SelectParserSingle parser = new SelectParserSingle(new SingleVarNameAdapter());
        final Select select = new Select();
        parser.parse(select.getFinalSelect());
        parser.addCondition(QueryHelper.eq("#id", "fakeid"));
        final JsonNode queryDsl = parser.getRequest().getFinalSelect();
        final RequestResponseOK<IngestContractModel> IngestContractModelList =
            ingestContractService.findContracts(queryDsl);

        assertThat(IngestContractModelList.getResults()).isEmpty();
    }


    /**
     * Check that the created ingest conrtact have the tenant owner after persisted to database
     *
     * @throws Exception
     */
    @Test
    @RunWithCustomExecutor
    public void givenIngestContractsTestTenantOwner() throws Exception {

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileContracts = PropertiesUtils.getResourceFile("referential_contracts_ok.json");
        final List<IngestContractModel> IngestContractModelList =
            JsonHandler.getFromFileAsTypeReference(fileContracts, new TypeReference<List<IngestContractModel>>() {
            });
        final RequestResponse<IngestContractModel> response =
            ingestContractService.createContracts(IngestContractModelList);

        final RequestResponseOK<IngestContractModel> responseCast = (RequestResponseOK<IngestContractModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);

        // We juste test the first contract
        final IngestContractModel acm = responseCast.getResults().iterator().next();
        assertThat(acm).isNotNull();

        String id1 = acm.getIdentifier();
        assertThat(id1).isNotNull();


        final IngestContractModel one = ingestContractService.findByIdentifier(id1);

        assertThat(one).isNotNull();

        assertThat(one.getName()).isEqualTo(acm.getName());

        assertThat(one.getTenant()).isNotNull();
        assertThat(one.getTenant()).isEqualTo(Integer.valueOf(TENANT_ID));

    }


    /**
     * Ingest contract of tenant 1, try to get the same contract with id mongo but with tenant 2 This sgould not return
     * the contract as tenant 2 is not the owner of the Ingest contract
     *
     * @throws Exception
     */
    @Test
    @RunWithCustomExecutor
    public void givenIngestContractsTestNotTenantOwner() throws Exception {

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileContracts = PropertiesUtils.getResourceFile("referential_contracts_ok.json");
        final List<IngestContractModel> IngestContractModelList =
            JsonHandler.getFromFileAsTypeReference(fileContracts, new TypeReference<List<IngestContractModel>>() {
            });
        final RequestResponse<IngestContractModel> response =
            ingestContractService.createContracts(IngestContractModelList);

        final RequestResponseOK<IngestContractModel> responseCast = (RequestResponseOK<IngestContractModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);

        // We juste test the first contract
        final IngestContractModel acm = responseCast.getResults().iterator().next();
        assertThat(acm).isNotNull();

        final String id1 = acm.getId();
        assertThat(id1).isNotNull();


        VitamThreadUtils.getVitamSession().setTenantId(2);

        final IngestContractModel one = ingestContractService.findByIdentifier(id1);

        assertThat(one).isNull();

    }

    @Test
    @RunWithCustomExecutor
    public void givenIngestContractsTestfindByIdentifier() throws Exception {

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileContracts = PropertiesUtils.getResourceFile("referential_contracts_ok.json");
        final List<IngestContractModel> IngestContractModelList =
            JsonHandler.getFromFileAsTypeReference(fileContracts, new TypeReference<List<IngestContractModel>>() {
            });
        final RequestResponse<IngestContractModel> response =
            ingestContractService.createContracts(IngestContractModelList);

        final RequestResponseOK<IngestContractModel> responseCast = (RequestResponseOK<IngestContractModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);

        // We juste test the first contract
        final IngestContractModel acm = responseCast.getResults().iterator().next();
        assertThat(acm).isNotNull();

        String id1 = acm.getIdentifier();
        assertThat(id1).isNotNull();


        final IngestContractModel one = ingestContractService.findByIdentifier(id1);

        assertThat(one).isNotNull();

        assertThat(one.getName()).isEqualTo(acm.getName());
    }


    @Test
    @RunWithCustomExecutor
    public void givenIngestContractsTestImportExternalIdentifier() throws Exception {

        VitamThreadUtils.getVitamSession().setTenantId(EXTERNAL_TENANT);
        final File fileContracts = PropertiesUtils.getResourceFile("referential_contracts_ok_identifier.json");
        final List<IngestContractModel> IngestContractModelList =
            JsonHandler.getFromFileAsTypeReference(fileContracts, new TypeReference<List<IngestContractModel>>() {
            });
        RequestResponse<IngestContractModel> response = ingestContractService.createContracts(IngestContractModelList);
        assertThat(response.isOk()).isTrue();

        final RequestResponseOK<IngestContractModel> responseCast = (RequestResponseOK<IngestContractModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);

        // We juste test the first contract
        final IngestContractModel acm = responseCast.getResults().iterator().next();
        assertThat(acm).isNotNull();

        String id1 = acm.getIdentifier();
        assertThat(id1).isNotNull();


        final IngestContractModel one = ingestContractService.findByIdentifier(id1);

        assertThat(one).isNotNull();

        assertThat(one.getName()).isEqualTo(acm.getName());

        // test duplication
        response = ingestContractService.createContracts(IngestContractModelList);
        assertThat(response.isOk()).isFalse();
    }


    @Test
    @RunWithCustomExecutor
    public void givenIngesContractsTestImportExternalIdentifierKO() throws Exception {

        VitamThreadUtils.getVitamSession().setTenantId(EXTERNAL_TENANT);
        final File fileContracts = PropertiesUtils.getResourceFile("referential_contracts_ok.json");
        final List<IngestContractModel> IngestContractModelList =
            JsonHandler.getFromFileAsTypeReference(fileContracts, new TypeReference<List<IngestContractModel>>() {
            });
        final RequestResponse<IngestContractModel> response =
            ingestContractService.createContracts(IngestContractModelList);
        assertThat(response.isOk()).isFalse();


        ArgumentCaptor<LogbookOperationParameters> logbookOperationParametersCaptor =
            ArgumentCaptor.forClass(LogbookOperationParameters.class);
        verify(logbookOperationsClientMock, times(1)).create(logbookOperationParametersCaptor.capture());
        verify(logbookOperationsClientMock, times(1)).update(logbookOperationParametersCaptor.capture());
        List<LogbookOperationParameters> allLogbookOperationParameters = logbookOperationParametersCaptor
            .getAllValues();
        assertThat(allLogbookOperationParameters.size()).isEqualTo(2);
        assertThat(allLogbookOperationParameters.get(0).getStatus()).isEqualTo(StatusCode.STARTED);
        assertThat(allLogbookOperationParameters.get(0).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_IMPORT_INGEST_CONTRACT");
        assertThat(allLogbookOperationParameters.get(1).getStatus()).isEqualTo(StatusCode.KO);
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_IMPORT_INGEST_CONTRACT");
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.outcomeDetail))
            .isEqualTo("STP_IMPORT_INGEST_CONTRACT.EMPTY_REQUIRED_FIELD.KO");

    }


    @Test
    @RunWithCustomExecutor
    public void givenIngestContractsTestInvalidVersionThenImportKO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileContracts = PropertiesUtils.getResourceFile("referential_contracts_ok.json");
        final List<IngestContractModel> ingestContractModelList =
            JsonHandler.getFromFileAsTypeReference(fileContracts, new TypeReference<List<IngestContractModel>>() {
            });
        ingestContractModelList.get(0).setDataObjectVersion(Collections.singleton("toto"));
        final RequestResponse<IngestContractModel> response =
            ingestContractService.createContracts(ingestContractModelList);

        assertThat(response.isOk()).isFalse();
        assertThat(((VitamError) response).getErrors()).isNotNull();
        assertThat(((VitamError) response).getMessage()).isEqualTo("IngestContract service error");
        assertThat(((VitamError) response).getDescription()).contains("Import ingest contracts error > ")
            .contains("Document schema validation failed");
        assertThat(((VitamError) response).getState()).isEqualTo("KO");
        assertThat(((VitamError) response).getHttpCode()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());

        verifyNoMoreInteractions(functionalBackupService);

        ArgumentCaptor<LogbookOperationParameters> logbookOperationParametersCaptor =
            ArgumentCaptor.forClass(LogbookOperationParameters.class);
        verify(logbookOperationsClientMock, times(1)).create(logbookOperationParametersCaptor.capture());
        verify(logbookOperationsClientMock, times(1)).update(logbookOperationParametersCaptor.capture());
        List<LogbookOperationParameters> allLogbookOperationParameters = logbookOperationParametersCaptor
            .getAllValues();
        assertThat(allLogbookOperationParameters.size()).isEqualTo(2);
        assertThat(allLogbookOperationParameters.get(0).getStatus()).isEqualTo(StatusCode.STARTED);
        assertThat(allLogbookOperationParameters.get(0).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_IMPORT_INGEST_CONTRACT");
        assertThat(allLogbookOperationParameters.get(1).getStatus()).isEqualTo(StatusCode.KO);
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_IMPORT_INGEST_CONTRACT");
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.outcomeDetail))
            .isEqualTo("STP_IMPORT_INGEST_CONTRACT.BAD_REQUEST.KO");
    }

    @Test
    @RunWithCustomExecutor
    public void givenIngestContractsTestFindAllThenReturnEmpty() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final RequestResponseOK<IngestContractModel> IngestContractModelList =
            ingestContractService.findContracts(JsonHandler.createObjectNode());
        assertThat(IngestContractModelList.getResults()).isEmpty();
    }

    @Test
    @RunWithCustomExecutor
    public void givenIngestContractsTestFindAllThenReturnTwoContracts() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileContracts = PropertiesUtils.getResourceFile("referential_contracts_ok.json");
        final List<IngestContractModel> IngestContractModelList =
            JsonHandler.getFromFileAsTypeReference(fileContracts, new TypeReference<List<IngestContractModel>>() {
            });
        final RequestResponse<IngestContractModel> response =
            ingestContractService.createContracts(IngestContractModelList);

        final RequestResponseOK<IngestContractModel> responseCast = (RequestResponseOK<IngestContractModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);

        final RequestResponseOK<IngestContractModel> IngestContractModelListSearch =
            ingestContractService.findContracts(JsonHandler.createObjectNode());
        assertThat(IngestContractModelListSearch.getResults()).hasSize(2);
    }

    @Test
    @RunWithCustomExecutor
    public void givenIngestContractTestUpdate() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final String documentName = "aName";
        // Create document
        final File fileContracts = PropertiesUtils.getResourceFile("referential_contracts_ok.json");

        final List<IngestContractModel> ingestModelList =
            JsonHandler.getFromFileAsTypeReference(fileContracts, new TypeReference<List<IngestContractModel>>() {
            });
        final RequestResponse<IngestContractModel> response = ingestContractService.createContracts(ingestModelList);

        RequestResponseOK<IngestContractModel> responseCast = (RequestResponseOK<IngestContractModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);


        final SelectParserSingle parser = new SelectParserSingle(new SingleVarNameAdapter());
        final Select select = new Select();
        parser.parse(select.getFinalSelect());
        parser.addCondition(QueryHelper.eq("Name", documentName));
        final JsonNode queryDsl = parser.getRequest().getFinalSelect();
        responseCast = ingestContractService.findContracts(queryDsl);
        assertThat(responseCast.getResults()).isNotEmpty();
        final SelectParserSingle parserbName2 = new SelectParserSingle(new SingleVarNameAdapter());
        final Select selectbName2 = new Select();
        parserbName2.parse(selectbName2.getFinalSelect());
        parserbName2.addCondition(QueryHelper.eq("Name", "bName2"));
        final JsonNode queryDslbName2 = parserbName2.getRequest().getFinalSelect();
        RequestResponseOK<IngestContractModel> responseFindbName2 = ingestContractService.findContracts(queryDslbName2);
        for (final IngestContractModel ingestContractModel : responseFindbName2.getResults()) {
            assertThat(ingestContractModel.isMasterMandatory()).isTrue();
            assertThat(ingestContractModel.isEveryDataObjectVersion()).isFalse();
        }
        for (final IngestContractModel ingestContractModel : responseFindbName2.getResults()) {
            assertThat(ingestContractModel.isFormatUnidentifiedAuthorized()).isFalse();
            assertThat(ingestContractModel.isEveryFormatType()).isTrue();
        }
        for (final IngestContractModel ingestContractModel : responseCast.getResults()) {
            assertThat(ActivationStatus.ACTIVE.equals(ingestContractModel.getStatus())).isTrue();
        }
        for (final IngestContractModel ingestContractModel : responseCast.getResults()) {
            assertThat(ingestContractModel.isMasterMandatory()).isFalse();
            assertThat(ingestContractModel.isEveryDataObjectVersion()).isTrue();
        }
        for (final IngestContractModel ingestContractModel : responseCast.getResults()) {
            assertThat(ingestContractModel.isFormatUnidentifiedAuthorized()).isFalse();
            assertThat(ingestContractModel.isEveryFormatType()).isTrue();
        }

        final String identifier = responseCast.getResults().get(0).getIdentifier();
        // Test update for ingest contract Status => inactive
        final String now = LocalDateUtil.now().toString();
        final UpdateParserSingle updateParser = new UpdateParserSingle(new SingleVarNameAdapter());
        final SetAction setActionStatusInactive =
            UpdateActionHelper.set("Status", ActivationStatus.INACTIVE.toString());
        final SetAction setActionDesactivationDateInactive = UpdateActionHelper.set("DeactivationDate", now);
        final SetAction setActionLastUpdateInactive = UpdateActionHelper.set("LastUpdate", now);

        final Update update = new Update();
        update.setQuery(QueryHelper.eq("Identifier", identifier));
        update.addActions(setActionStatusInactive, setActionDesactivationDateInactive, setActionLastUpdateInactive);
        updateParser.parse(update.getFinalUpdate());
        JsonNode queryDslForUpdate = updateParser.getRequest().getFinalUpdate();

        RequestResponse<IngestContractModel> updateContractStatus =
            ingestContractService.updateContract(ingestModelList.get(0).getIdentifier(), queryDslForUpdate);
        assertThat(updateContractStatus).isNotExactlyInstanceOf(VitamError.class);

        parser.parse(new Select().getFinalSelect());
        parser.addCondition(QueryHelper.eq("Identifier", identifier));
        final JsonNode queryDsl2 = parser.getRequest().getFinalSelect();
        final RequestResponseOK<IngestContractModel> ingestContractModelListForassert =
            ingestContractService.findContracts(queryDsl2);
        assertThat(ingestContractModelListForassert.getResults()).isNotEmpty();
        for (final IngestContractModel ingestContractModel : ingestContractModelListForassert.getResults()) {
            assertThat(ActivationStatus.INACTIVE.equals(ingestContractModel.getStatus())).isTrue();
            assertThat(ingestContractModel.getDeactivationdate()).isNotEmpty();
            assertThat(ingestContractModel.getLastupdate()).isNotEmpty();
        }

        // Test update for ingest contract Status => Active
        final UpdateParserSingle updateParserActive = new UpdateParserSingle(new SingleVarNameAdapter());
        final SetAction setActionStatusActive = UpdateActionHelper.set("Status", ActivationStatus.ACTIVE.toString());
        final SetAction setActionDesactivationDateActive = UpdateActionHelper.set("ActivationDate", now);
        final SetAction setActionLastUpdateActive = UpdateActionHelper.set("LastUpdate", now);
        final SetAction setLinkParentId = UpdateActionHelper.set(IngestContractModel.LINK_PARENT_ID, "");
        final Update updateStatusActive = new Update();
        updateStatusActive.setQuery(QueryHelper.eq("Identifier", identifier));
        updateStatusActive.addActions(setActionStatusActive, setActionDesactivationDateActive,
            setActionLastUpdateActive, setLinkParentId);
        updateParserActive.parse(updateStatusActive.getFinalUpdate());
        JsonNode queryDslStatusActive = updateParserActive.getRequest().getFinalUpdate();
        ingestContractService.updateContract(ingestModelList.get(0).getIdentifier(), queryDslStatusActive);

        final RequestResponseOK<IngestContractModel> ingestContractModelListForassert2 =
            ingestContractService.findContracts(queryDsl2);
        assertThat(ingestContractModelListForassert2.getResults()).isNotEmpty();
        for (final IngestContractModel ingestContractModel : ingestContractModelListForassert2.getResults()) {
            assertThat(ActivationStatus.ACTIVE.equals(ingestContractModel.getStatus())).isTrue();
            assertThat(ingestContractModel.getActivationdate()).isNotEmpty();
            assertThat(ingestContractModel.getLastupdate()).isNotEmpty();
            assertThat(ingestContractModel.getLinkParentId()).isEqualTo("");
            assertThat(ActivationStatus.INACTIVE.equals(ingestContractModel.getCheckParentLink()));
        }

        // we try to update ingest contract with same value -> Bad Request
        RequestResponse<IngestContractModel> responseUpdate =
            ingestContractService.updateContract(ingestModelList.get(0).getIdentifier(), queryDslStatusActive);
        assertThat(!responseUpdate.isOk());
        assertEquals(200, responseUpdate.getStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void givenIngestContractWhenUpdateComputeInheritedRulesAtIngestThenValueIstrue() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final String documentName = "aName";
        // Create document
        final File fileContracts =
            PropertiesUtils.getResourceFile("referential_contracts_ok_ComputeInheritedRulesAtIngest.json");

        final List<IngestContractModel> ingestModelList =
            JsonHandler.getFromFileAsTypeReference(fileContracts, new TypeReference<List<IngestContractModel>>() {
            });
        final RequestResponse<IngestContractModel> response = ingestContractService.createContracts(ingestModelList);

        RequestResponseOK<IngestContractModel> responseCast = (RequestResponseOK<IngestContractModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);

        final SelectParserSingle parser = new SelectParserSingle(new SingleVarNameAdapter());
        final Select select = new Select();
        parser.parse(select.getFinalSelect());
        parser.addCondition(QueryHelper.eq("Name", documentName));
        final JsonNode queryDsl = parser.getRequest().getFinalSelect();
        responseCast = ingestContractService.findContracts(queryDsl);
        assertThat(responseCast.getResults()).isNotEmpty();

        final SelectParserSingle parserbName2 = new SelectParserSingle(new SingleVarNameAdapter());
        final Select selectbName2 = new Select();
        parserbName2.parse(selectbName2.getFinalSelect());
        parserbName2.addCondition(QueryHelper.eq("Name", "bName2"));
        final JsonNode queryDslbName2 = parserbName2.getRequest().getFinalSelect();
        RequestResponseOK<IngestContractModel> responseFindbName2 = ingestContractService.findContracts(queryDslbName2);
        for (final IngestContractModel ingestContractModel : responseFindbName2.getResults()) {
            assertThat(ingestContractModel.isComputeInheritedRulesAtIngest()).isFalse();
        }

        final String identifier = responseCast.getResults().get(0).getIdentifier();
        final String now = LocalDateUtil.now().toString();
        final UpdateParserSingle updateParser = new UpdateParserSingle(new SingleVarNameAdapter());

        final SetAction setActionLastUpdateInactive = UpdateActionHelper.set("LastUpdate", now);
        final SetAction setComputeInheritedRulesAtIngest = UpdateActionHelper.set("ComputeInheritedRulesAtIngest", true);

        final Update update = new Update();
        update.setQuery(QueryHelper.eq("Identifier", identifier));
        update.addActions(setActionLastUpdateInactive, setComputeInheritedRulesAtIngest);
        updateParser.parse(update.getFinalUpdate());
        JsonNode queryDslForUpdate = updateParser.getRequest().getFinalUpdate();

        RequestResponse<IngestContractModel> updateContractStatus =
            ingestContractService.updateContract(ingestModelList.get(0).getIdentifier(), queryDslForUpdate);
        assertThat(updateContractStatus).isNotExactlyInstanceOf(VitamError.class);

        parser.parse(new Select().getFinalSelect());
        parser.addCondition(QueryHelper.eq("Identifier", identifier));
        final JsonNode queryDsl2 = parser.getRequest().getFinalSelect();
        final RequestResponseOK<IngestContractModel> ingestContractModelListForassert =
            ingestContractService.findContracts(queryDsl2);
        for (final IngestContractModel ingestContractModel : ingestContractModelListForassert.getResults()) {
            assertThat(ingestContractModelListForassert.getResults()).isNotEmpty();
            assertThat(ingestContractModel.isComputeInheritedRulesAtIngest()).isTrue();
        }
    }

    @Test
    @RunWithCustomExecutor
    public void givenIngestContractsTestFormatInDB() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileFormat = PropertiesUtils.getResourceFile(FORMAT_FILE);

        final List<FileFormatModel> fileFormatModelList =
            JsonHandler.getFromFileAsTypeReference(fileFormat, new TypeReference<List<FileFormatModel>>() {
            });

        ArrayNode allFormatNodeArray = JsonHandler.createArrayNode();
        for (FileFormatModel format : fileFormatModelList) {
            allFormatNodeArray.add(JsonHandler.toJsonNode(format));
        }
        dbImpl.insertDocuments(
            allFormatNodeArray,
            FunctionalAdminCollections.FORMATS).close();
        //Contract format OK
        final File fileContracts = PropertiesUtils.getResourceFile("referential_contracts_with_formattype_ok.json");
        final List<IngestContractModel> ingestContractModelList =
            JsonHandler.getFromFileAsTypeReference(fileContracts, new TypeReference<List<IngestContractModel>>() {
            });

        RequestResponse<IngestContractModel> response = ingestContractService.createContracts(ingestContractModelList);

        assertTrue(response.isOk());

        final SelectParserSingle parser = new SelectParserSingle(new SingleVarNameAdapter());
        final Select select = new Select();
        parser.parse(select.getFinalSelect());

        JsonNode formatQueryDsl = parser.getRequest().getFinalSelect();
        RequestResponseOK<IngestContractModel> responseCast = ingestContractService.findContracts(formatQueryDsl);

        assertThat(responseCast.getResults()).isNotEmpty();
        assertTrue(responseCast.getResults().size() == 2);

        //KO
        final File fileContractsKO = PropertiesUtils.getResourceFile("referential_contracts_with_formattype_ko.json");
        final List<IngestContractModel> ingestContractModelKOList =
            JsonHandler.getFromFileAsTypeReference(fileContractsKO, new TypeReference<List<IngestContractModel>>() {
            });

        response = ingestContractService.createContracts(ingestContractModelKOList);

        assertTrue(!response.isOk());

        RequestResponseOK<IngestContractModel> responseContrat = ingestContractService.findContracts(formatQueryDsl);

        assertThat(responseContrat.getResults()).isNotEmpty();
        assertTrue(responseCast.getResults().size() == 2);
    }

    @Test
    @RunWithCustomExecutor
    public void givenIngestContractsTestFindByName() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileContracts = PropertiesUtils.getResourceFile("referential_contracts_ok.json");
        final List<IngestContractModel> IngestContractModelList =
            JsonHandler.getFromFileAsTypeReference(fileContracts, new TypeReference<List<IngestContractModel>>() {
            });
        final RequestResponse<IngestContractModel> response =
            ingestContractService.createContracts(IngestContractModelList);

        final RequestResponseOK<IngestContractModel> responseCast = (RequestResponseOK<IngestContractModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);


        final IngestContractModel acm = IngestContractModelList.iterator().next();
        assertThat(acm).isNotNull();

        final String id1 = acm.getId();
        assertThat(id1).isNotNull();

        final String name = acm.getName();
        assertThat(name).isNotNull();


        final SelectParserSingle parser = new SelectParserSingle(new SingleVarNameAdapter());
        final Select select = new Select();
        parser.parse(select.getFinalSelect());
        parser.addCondition(QueryHelper.eq("Name", name));
        final JsonNode queryDsl = parser.getRequest().getFinalSelect();


        final RequestResponseOK<IngestContractModel> IngestContractModelListFound =
            ingestContractService.findContracts(queryDsl);
        assertThat(IngestContractModelListFound.getResults()).hasSize(1);

        final IngestContractModel acmFound = IngestContractModelListFound.getResults().iterator().next();
        assertThat(acmFound).isNotNull();

        assertThat(acmFound.getId()).isEqualTo(id1);
        assertThat(acmFound.getName()).isEqualTo(name);

    }

    @Test
    @RunWithCustomExecutor
    public void givenIngestContractsTestLinkParentIdKo() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(metaDataClientMock.selectUnitbyId(any(), any())).thenReturn(new RequestResponseOK<>().toJsonNode());
        when(metaDataClientMock.selectUnits(any())).thenReturn(new RequestResponseOK<>().toJsonNode());

        final File fileContracts = PropertiesUtils.getResourceFile("referential_contracts_link_parentId.json");
        final List<IngestContractModel> IngestContractModelList =
            JsonHandler.getFromFileAsTypeReference(fileContracts, new TypeReference<List<IngestContractModel>>() {
            });
        final RequestResponse<IngestContractModel> response =
            ingestContractService.createContracts(IngestContractModelList);

        assertThat(response).isInstanceOf(VitamError.class);

        final VitamError vitamError = (VitamError) response;
        assertThat(vitamError.toString()).contains("At least one AU id holding_guid not found");
        assertThat(vitamError.getErrors()).isNotNull();
        assertThat(vitamError.getErrors().size()).isEqualTo(2);
        assertThat(vitamError.getErrors().get(0).getMessage()).isEqualTo("IngestContract service error");
        assertThat(vitamError.getErrors().get(0).getDescription())
            .isEqualTo("At least one AU id holding_guid not found");
        assertThat(vitamError.getState()).isEqualTo("KO");
        assertThat(vitamError.getHttpCode()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());


        ArgumentCaptor<LogbookOperationParameters> logbookOperationParametersCaptor =
            ArgumentCaptor.forClass(LogbookOperationParameters.class);
        verify(logbookOperationsClientMock, times(1)).create(logbookOperationParametersCaptor.capture());
        verify(logbookOperationsClientMock, times(1)).update(logbookOperationParametersCaptor.capture());
        List<LogbookOperationParameters> allLogbookOperationParameters = logbookOperationParametersCaptor
            .getAllValues();
        assertThat(allLogbookOperationParameters.size()).isEqualTo(2);
        assertThat(allLogbookOperationParameters.get(0).getStatus()).isEqualTo(StatusCode.STARTED);
        assertThat(allLogbookOperationParameters.get(0).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_IMPORT_INGEST_CONTRACT");
        assertThat(allLogbookOperationParameters.get(1).getStatus()).isEqualTo(StatusCode.KO);
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_IMPORT_INGEST_CONTRACT");
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.outcomeDetail))
            .isEqualTo("IngestContract service error");
    }

    @Test
    @RunWithCustomExecutor
    public void givenIngestContractsTestCheckParentIdKo() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(metaDataClientMock.selectUnitbyId(any(), any())).thenReturn(new RequestResponseOK<>().toJsonNode());
        when(metaDataClientMock.selectUnits(any())).thenReturn(new RequestResponseOK<>().toJsonNode());

        final File fileContracts = PropertiesUtils.getResourceFile("referential_contracts_link_parentId.json");
        final List<IngestContractModel> IngestContractModelList =
            JsonHandler.getFromFileAsTypeReference(fileContracts, new TypeReference<List<IngestContractModel>>() {
            });
        final RequestResponse<IngestContractModel> response =
            ingestContractService.createContracts(IngestContractModelList);

        assertThat(response).isInstanceOf(VitamError.class);

        final VitamError vitamError = (VitamError) response;
        assertThat(vitamError.getErrors()).isNotNull();
        assertThat(vitamError.getErrors().size()).isEqualTo(2);
        assertThat(vitamError.getErrors().get(1).getMessage()).isEqualTo("IngestContract service error");
        assertThat(vitamError.getErrors().get(1).getDescription())
            .isEqualTo("At least one AU id unitId2 unitId not found");
        assertThat(vitamError.getState()).isEqualTo("KO");
        assertThat(vitamError.getHttpCode()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());


        ArgumentCaptor<LogbookOperationParameters> logbookOperationParametersCaptor =
            ArgumentCaptor.forClass(LogbookOperationParameters.class);
        verify(logbookOperationsClientMock, times(1)).create(logbookOperationParametersCaptor.capture());
        verify(logbookOperationsClientMock, times(1)).update(logbookOperationParametersCaptor.capture());
        List<LogbookOperationParameters> allLogbookOperationParameters = logbookOperationParametersCaptor
            .getAllValues();
        assertThat(allLogbookOperationParameters.size()).isEqualTo(2);
        assertThat(allLogbookOperationParameters.get(0).getStatus()).isEqualTo(StatusCode.STARTED);
        assertThat(allLogbookOperationParameters.get(0).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_IMPORT_INGEST_CONTRACT");
        assertThat(allLogbookOperationParameters.get(1).getStatus()).isEqualTo(StatusCode.KO);
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_IMPORT_INGEST_CONTRACT");
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.outcomeDetail))
            .isEqualTo("IngestContract service error");
    }

    @Test
    @RunWithCustomExecutor
    public void givenIngestContractsTestLinkParentIdOK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        RequestResponseOK ok = new RequestResponseOK<>();
        ok.setHits(1, 0, 1, 1);// simulate returning result when query for filing or holding unit
        when(metaDataClientMock.selectUnitbyId(any(), any())).thenReturn(ok.toJsonNode());
        when(metaDataClientMock.selectUnits(any())).thenReturn(ok.toJsonNode());

        final File fileContracts = PropertiesUtils.getResourceFile("referential_contracts_link_parentId.json");
        final List<IngestContractModel> IngestContractModelList =
            JsonHandler.getFromFileAsTypeReference(fileContracts, new TypeReference<List<IngestContractModel>>() {
            });
        final RequestResponse<IngestContractModel> response =
            ingestContractService.createContracts(IngestContractModelList);

        final RequestResponseOK<IngestContractModel> responseCast = (RequestResponseOK<IngestContractModel>) response;
        assertThat(responseCast.getResults()).hasSize(5);

        final RequestResponseOK<IngestContractModel> IngestContractModelListSearch =
            ingestContractService.findContracts(JsonHandler.createObjectNode());
        assertThat(IngestContractModelListSearch.getResults()).hasSize(5);
    }

    @Test
    @RunWithCustomExecutor
    public void givenIngestContractTestValidationWhenUpdate() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final String documentName = "aName";
        // Create document
        final File fileContracts = PropertiesUtils.getResourceFile("referential_contracts_ok.json");

        final List<IngestContractModel> ingestModelList =
            JsonHandler.getFromFileAsTypeReference(fileContracts, new TypeReference<List<IngestContractModel>>() {
            });
        final RequestResponse<IngestContractModel> response = ingestContractService.createContracts(ingestModelList);

        RequestResponseOK<IngestContractModel> responseCast = (RequestResponseOK<IngestContractModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);


        final SelectParserSingle parser = new SelectParserSingle(new SingleVarNameAdapter());
        final Select select = new Select();
        parser.parse(select.getFinalSelect());
        parser.addCondition(QueryHelper.eq("Name", documentName));
        final JsonNode queryDsl = parser.getRequest().getFinalSelect();
        responseCast = ingestContractService.findContracts(queryDsl);
        assertThat(responseCast.getResults()).isNotEmpty();
        for (final IngestContractModel ingestContractModel : responseCast.getResults()) {
            assertThat(ActivationStatus.ACTIVE.equals(ingestContractModel.getStatus())).isTrue();
        }
        final String identifier = responseCast.getResults().get(0).getIdentifier();
        // Test update for ingest contract Status => inactive
        final String now = LocalDateUtil.now().toString();
        final UpdateParserSingle updateParser = new UpdateParserSingle(new SingleVarNameAdapter());
        final SetAction setActionStatusInactive = UpdateActionHelper.set("Status", "INVALID_STATUS");
        final SetAction setActionDesactivationDateInactive = UpdateActionHelper.set("DeactivationDate", now);
        final SetAction setActionLastUpdateInactive = UpdateActionHelper.set("LastUpdate", now);

        final Update update = new Update();
        update.setQuery(QueryHelper.eq("Identifier", identifier));
        update.addActions(setActionStatusInactive, setActionDesactivationDateInactive, setActionLastUpdateInactive);
        updateParser.parse(update.getFinalUpdate());
        JsonNode queryDslForUpdate = updateParser.getRequest().getFinalUpdate();

        RequestResponse<IngestContractModel> updateContractStatus =
            ingestContractService.updateContract(ingestModelList.get(0).getIdentifier(), queryDslForUpdate);
        assertEquals(updateContractStatus.getStatus(), Response.Status.BAD_REQUEST.getStatusCode());
        assertThat(updateContractStatus).isInstanceOf(VitamError.class);
        List<VitamError> errors = ((VitamError) updateContractStatus).getErrors();
        assertThat(VitamLogbookMessages.getFromFullCodeKey(errors.get(0).getMessage()).equals(
            "Échec du processus de mise à jour du contrat d'entrée : une valeur ne correspond pas aux valeurs attendues")
        ).isTrue();

        final UpdateParserSingle updateParser2 = new UpdateParserSingle(new SingleVarNameAdapter());
        final SetAction setCheckParentLinkStatusInactive = UpdateActionHelper.set("CheckParentLink", "INVALID_STATUS");
        final Update update2 = new Update();
        update2.setQuery(QueryHelper.eq("Identifier", identifier));
        update2.addActions(setCheckParentLinkStatusInactive);
        updateParser2.parse(update2.getFinalUpdate());
        JsonNode queryDslForUpdate2 = updateParser2.getRequest().getFinalUpdate();

        RequestResponse<IngestContractModel> updateCheckParentLinkStatus =
            ingestContractService.updateContract(ingestModelList.get(0).getIdentifier(), queryDslForUpdate2);
        assertEquals(updateCheckParentLinkStatus.getStatus(), Response.Status.BAD_REQUEST.getStatusCode());
        assertThat(updateCheckParentLinkStatus).isInstanceOf(VitamError.class);

        when(managementContractService.findByIdentifier(any())).thenReturn(null);
        final UpdateParserSingle updateParserMc = new UpdateParserSingle(new SingleVarNameAdapter());
        final SetAction setManagementContractNotExists = UpdateActionHelper.set("ManagementContractId", "UNKNOW_MC");
        final Update updateMc = new Update();
        updateMc.setQuery(QueryHelper.eq("Identifier", identifier));
        updateMc.addActions(setManagementContractNotExists);
        updateParserMc.parse(updateMc.getFinalUpdate());
        JsonNode queryDslForUpdateMc = updateParserMc.getRequest().getFinalUpdate();

        RequestResponse<IngestContractModel> updateCheckMc =
            ingestContractService.updateContract(ingestModelList.get(0).getIdentifier(), queryDslForUpdateMc);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), updateCheckMc.getStatus());
        assertThat(updateCheckMc).isInstanceOf(VitamError.class);
    }

    @Test
    @RunWithCustomExecutor
    public void should_retrieve_vitam_error_when_everyformattype_is_false_and_blank_formattype() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final String documentName = "aName";
        // Create document
        final File fileContracts = PropertiesUtils.getResourceFile("referential_contracts_ok.json");

        final List<IngestContractModel> ingestModelList =
            JsonHandler.getFromFileAsTypeReference(fileContracts, new TypeReference<List<IngestContractModel>>() {
            });
        final RequestResponse<IngestContractModel> response = ingestContractService.createContracts(ingestModelList);
        RequestResponseOK<IngestContractModel> responseCast = (RequestResponseOK<IngestContractModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);


        final SelectParserSingle parser = new SelectParserSingle(new SingleVarNameAdapter());
        final Select select = new Select();
        parser.parse(select.getFinalSelect());
        parser.addCondition(QueryHelper.eq("Name", documentName));
        final JsonNode queryDsl = parser.getRequest().getFinalSelect();
        responseCast = ingestContractService.findContracts(queryDsl);
        assertThat(responseCast.getResults()).isNotEmpty();
        for (final IngestContractModel ingestContractModel : responseCast.getResults()) {
            assertThat(ActivationStatus.ACTIVE.equals(ingestContractModel.getStatus())).isTrue();
        }
        final String identifier = responseCast.getResults().get(0).getIdentifier();
        // When
        // Test update for ingest contract With Empty List
        final UpdateParserSingle updateParserActive = new UpdateParserSingle(new SingleVarNameAdapter());
        final SetAction setEveryFormatType = UpdateActionHelper.set(IngestContractModel.EVERY_FORMAT_TYPE, false);
        final Update updateStatusActive = new Update();
        updateStatusActive.setQuery(QueryHelper.eq("Identifier", identifier));
        updateStatusActive.addActions(setEveryFormatType);
        updateParserActive.parse(updateStatusActive.getFinalUpdate());
        JsonNode queryDslStatusActive = updateParserActive.getRequest().getFinalUpdate();
        // Then
        RequestResponse<IngestContractModel> ingestContractModelRequestResponse =
            ingestContractService.updateContract(ingestModelList.get(0).getIdentifier(), queryDslStatusActive);
        assertThat(ingestContractModelRequestResponse.getHttpCode()).isEqualTo(400);
        VitamError vitamError =
            JsonHandler.getFromJsonNode(ingestContractModelRequestResponse.toJsonNode(), VitamError.class);
        assertThat(vitamError.getCode()).isEqualTo("08");
        assertThat(vitamError.getDescription()).isEqualTo("Ingest contract update error");

    }

    @Test
    @RunWithCustomExecutor
    public void givenIngestContractsTestManagementContractIdOK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(managementContractService.findByIdentifier(any())).thenReturn(new ManagementContractModel());

        final File fileContracts = PropertiesUtils.getResourceFile("referential_contracts_managementContractId.json");
        final List<IngestContractModel> IngestContractModelList =
            JsonHandler.getFromFileAsTypeReference(fileContracts, new TypeReference<List<IngestContractModel>>() {
            });
        final RequestResponse<IngestContractModel> response =
            ingestContractService.createContracts(IngestContractModelList);

        final RequestResponseOK<IngestContractModel> responseCast = (RequestResponseOK<IngestContractModel>) response;
        assertThat(responseCast.getResults()).hasSize(5);

        final RequestResponseOK<IngestContractModel> IngestContractModelListSearch =
            ingestContractService.findContracts(JsonHandler.createObjectNode());
        assertThat(IngestContractModelListSearch.getResults()).hasSize(5);
    }

    @Test
    @RunWithCustomExecutor
    public void givenIngestContractsTestManagementContractIdKO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(managementContractService.findByIdentifier(any())).thenReturn(null);

        final File fileContracts = PropertiesUtils.getResourceFile("referential_contracts_managementContractId.json");
        final List<IngestContractModel> IngestContractModelList =
            JsonHandler.getFromFileAsTypeReference(fileContracts, new TypeReference<List<IngestContractModel>>() {
            });
        final RequestResponse<IngestContractModel> response =
            ingestContractService.createContracts(IngestContractModelList);

        assertThat(response).isInstanceOf(VitamError.class);

        final VitamError vitamError = (VitamError) response;
        assertThat(vitamError.toString()).contains("At least one Management Contract with Id MC-00002 not found");
        assertThat(vitamError.getErrors()).isNotNull();
        assertThat(vitamError.getErrors().size()).isEqualTo(2);
        assertThat(vitamError.getErrors().get(0).getMessage())
            .isEqualTo("STP_IMPORT_INGEST_CONTRACT.MANAGEMENTCONTRACT_NOT_FOUND.KO");
        assertThat(vitamError.getErrors().get(0).getDescription())
            .isEqualTo("At least one Management Contract with Id MC-00001 not found");
        assertThat(vitamError.getState()).isEqualTo("KO");
        assertThat(vitamError.getHttpCode()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());


        ArgumentCaptor<LogbookOperationParameters> logbookOperationParametersCaptor =
            ArgumentCaptor.forClass(LogbookOperationParameters.class);
        verify(logbookOperationsClientMock, times(1)).create(logbookOperationParametersCaptor.capture());
        verify(logbookOperationsClientMock, times(1)).update(logbookOperationParametersCaptor.capture());
        List<LogbookOperationParameters> allLogbookOperationParameters = logbookOperationParametersCaptor
            .getAllValues();
        assertThat(allLogbookOperationParameters.size()).isEqualTo(2);
        assertThat(allLogbookOperationParameters.get(0).getStatus()).isEqualTo(StatusCode.STARTED);
        assertThat(allLogbookOperationParameters.get(0).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_IMPORT_INGEST_CONTRACT");
        assertThat(allLogbookOperationParameters.get(1).getStatus()).isEqualTo(StatusCode.KO);
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.eventType))
            .isEqualTo("STP_IMPORT_INGEST_CONTRACT");
        assertThat(allLogbookOperationParameters.get(1).getParameterValue(LogbookParameterName.outcomeDetail))
            .isEqualTo("STP_IMPORT_INGEST_CONTRACT.MANAGEMENTCONTRACT_NOT_FOUND.KO");
    }
}
