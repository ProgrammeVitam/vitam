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
package fr.gouv.vitam.functional.administration.core.security.profile;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.action.AddAction;
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
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.SecurityProfileModel;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.server.application.configuration.DbConfigurationImpl;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.config.ElasticsearchFunctionalAdminIndexManager;
import fr.gouv.vitam.functional.administration.common.counter.VitamCounterService;
import fr.gouv.vitam.functional.administration.common.server.ElasticsearchAccessFunctionalAdmin;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollectionsTestUtils;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminFactory;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.functional.administration.core.backup.FunctionalBackupService;
import fr.gouv.vitam.functional.administration.core.context.ContextService;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static fr.gouv.vitam.common.guid.GUIDFactory.newOperationLogbookGUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class SecurityProfileServiceTest {

    public static final String PERMISSIONS = "Permissions";
    private static final String PREFIX = GUIDFactory.newGUID().getId();
    private static final String NAME = "Name";
    private static final String BACKUP_SECURITY_PROFILE = "STP_BACKUP_SECURITY_PROFILE";
    private static final Integer TENANT_ID = 1;
    private static final Integer EXTERNAL_TENANT = 2;
    private static final ElasticsearchFunctionalAdminIndexManager indexManager =
        FunctionalAdminCollectionsTestUtils.createTestIndexManager();

    @ClassRule
    public static MongoRule mongoRule = new MongoRule(MongoDbAccess.getMongoClientSettingsBuilder());

    @ClassRule
    public static ElasticsearchRule elasticsearchRule = new ElasticsearchRule();

    private static MongoDbAccessAdminImpl dbImpl;

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    private VitamCounterService vitamCounterService;
    private FunctionalBackupService functionalBackupService;
    private SecurityProfileService securityProfileService;

    private ContextService contextService;

    @BeforeClass
    public static void setUpBeforeClass() throws VitamException {
        FunctionalAdminCollectionsTestUtils.beforeTestClass(
            mongoRule.getMongoDatabase(),
            PREFIX,
            new ElasticsearchAccessFunctionalAdmin(
                ElasticsearchRule.VITAM_CLUSTER,
                Lists.newArrayList(new ElasticsearchNode(ElasticsearchRule.getHost(), ElasticsearchRule.getPort())),
                indexManager
            ),
            Collections.singletonList(FunctionalAdminCollections.SECURITY_PROFILE)
        );

        final List<MongoDbNode> nodes = new ArrayList<>();
        nodes.add(new MongoDbNode("localhost", MongoRule.getDataBasePort()));
        dbImpl = MongoDbAccessAdminFactory.create(
            new DbConfigurationImpl(nodes, mongoRule.getMongoDatabase().getName()),
            Collections::emptyList,
            indexManager
        );
    }

    @AfterClass
    public static void tearDownAfterClass() {
        FunctionalAdminCollectionsTestUtils.afterTestClass(true);
    }

    @Before
    public void setup() throws Exception {
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(TENANT_ID));

        final List<MongoDbNode> nodes = new ArrayList<>();
        nodes.add(new MongoDbNode("localhost", MongoRule.getDataBasePort()));

        final List<Integer> tenants = Arrays.asList(TENANT_ID, EXTERNAL_TENANT);
        VitamConfiguration.setTenants(tenants);
        VitamConfiguration.setAdminTenant(TENANT_ID);
        Map<Integer, List<String>> listEnableExternalIdentifiers = new HashMap<>();
        List<String> list_tenant = new ArrayList<>();
        list_tenant.add("SECURITY_PROFILE");
        listEnableExternalIdentifiers.put(EXTERNAL_TENANT, list_tenant);
        vitamCounterService = new VitamCounterService(dbImpl, tenants, listEnableExternalIdentifiers);
        LogbookOperationsClientFactory.changeMode(null);

        functionalBackupService = mock(FunctionalBackupService.class);

        contextService = mock(ContextService.class);
        securityProfileService = new SecurityProfileService(
            MongoDbAccessAdminFactory.create(
                new DbConfigurationImpl(nodes, mongoRule.getMongoDatabase().getName()),
                Collections::emptyList,
                indexManager
            ),
            vitamCounterService,
            functionalBackupService
        );
        securityProfileService.setContextService(contextService);
    }

    @After
    public void afterTest() {
        FunctionalAdminCollectionsTestUtils.afterTest(
            Collections.singletonList(FunctionalAdminCollections.SECURITY_PROFILE)
        );
        securityProfileService.close();
    }

    @Test
    @RunWithCustomExecutor
    public void givenSecurityProfilesTestWellFormedContractThenImportSuccessfully() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        File securityProfileFiles = PropertiesUtils.getResourceFile("security_profile_ok.json");
        List<SecurityProfileModel> securityProfileModelList = JsonHandler.getFromFileAsTypeReference(
            securityProfileFiles,
            new TypeReference<>() {}
        );
        RequestResponse<SecurityProfileModel> response = securityProfileService.createSecurityProfiles(
            securityProfileModelList
        );

        assertThat(response.isOk()).isTrue();
        RequestResponseOK<SecurityProfileModel> responseCast = (RequestResponseOK<SecurityProfileModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);
        assertThat(responseCast.getResults().get(0).getIdentifier()).contains("SEC_PROFILE-000");
        assertThat(responseCast.getResults().get(0).getPermissions().size()).isEqualTo(3);
        assertThat(responseCast.getResults().get(1).getIdentifier()).contains("SEC_PROFILE-000");

        verify(functionalBackupService).saveCollectionAndSequence(
            any(),
            eq(BACKUP_SECURITY_PROFILE),
            eq(FunctionalAdminCollections.SECURITY_PROFILE),
            any()
        );
    }

    @Test
    @RunWithCustomExecutor
    public void givenSecurityProfilesTestMissingNameReturnBadRequest() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File securityProfileFiles = PropertiesUtils.getResourceFile("security_profile_ko_missing_name.json");
        final List<SecurityProfileModel> securityProfileModelList = JsonHandler.getFromFileAsTypeReference(
            securityProfileFiles,
            new TypeReference<>() {}
        );
        final RequestResponse<SecurityProfileModel> response = securityProfileService.createSecurityProfiles(
            securityProfileModelList
        );

        assertThat(response.isOk()).isFalse();

        verifyNoMoreInteractions(functionalBackupService);
    }

    @Test
    @RunWithCustomExecutor
    public void givenSecurityProfilesTestUnknownPermission_KO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File securityProfileFiles = PropertiesUtils.getResourceFile(
            "security_profile_ko_unknown_permission.json"
        );
        final List<SecurityProfileModel> securityProfileModelList = JsonHandler.getFromFileAsTypeReference(
            securityProfileFiles,
            new TypeReference<>() {}
        );
        final RequestResponse<SecurityProfileModel> response = securityProfileService.createSecurityProfiles(
            securityProfileModelList
        );

        assertThat(response.isOk()).isFalse();
        assertThat(((VitamError<SecurityProfileModel>) response).getErrors().get(0).getMessage()).isEqualTo(
            "Au moins une permission n'existe pas."
        );

        verifyNoMoreInteractions(functionalBackupService);
    }

    @Test
    @RunWithCustomExecutor
    public void givenSecurityProfilesTestDuplicateNamesAccepted() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File securityProfileFiles = PropertiesUtils.getResourceFile(
            "security_profile_ok_duplicate_names_accepted.json"
        );
        final List<SecurityProfileModel> securityProfileModelList = JsonHandler.getFromFileAsTypeReference(
            securityProfileFiles,
            new TypeReference<>() {}
        );
        final RequestResponse<SecurityProfileModel> response = securityProfileService.createSecurityProfiles(
            securityProfileModelList
        );

        assertThat(response.isOk()).isTrue();
    }

    @Test
    @RunWithCustomExecutor
    public void givenSecurityProfilesTestIdNotAllowedInCreation() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File securityProfileFiles = PropertiesUtils.getResourceFile(
            "security_profile_ko_id_not_allowed_in_creation.json"
        );

        final List<SecurityProfileModel> securityProfileModelList = JsonHandler.getFromFileAsTypeReference(
            securityProfileFiles,
            new TypeReference<>() {}
        );
        RequestResponse<SecurityProfileModel> response = securityProfileService.createSecurityProfiles(
            securityProfileModelList
        );

        assertThat(response.isOk()).isFalse();
    }

    @Test
    @RunWithCustomExecutor
    public void givenSecurityProfilesTestExternalIdentifierIgnored() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File securityProfileFiles = PropertiesUtils.getResourceFile("security_profile_ok_identifier.json");

        final List<SecurityProfileModel> securityProfileModelList = JsonHandler.getFromFileAsTypeReference(
            securityProfileFiles,
            new TypeReference<>() {}
        );
        RequestResponse<SecurityProfileModel> response = securityProfileService.createSecurityProfiles(
            securityProfileModelList
        );

        assertThat(response.isOk()).isTrue();

        final RequestResponseOK<SecurityProfileModel> responseCast = (RequestResponseOK<SecurityProfileModel>) response;
        assertThat(responseCast.getResults().get(0).getIdentifier()).isNotEqualTo("ID1");
    }

    @Test
    @RunWithCustomExecutor
    public void givenSecurityProfilesTestIdentifierAllowedInCreation() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(EXTERNAL_TENANT);
        final File securityProfileFiles = PropertiesUtils.getResourceFile("security_profile_ok_identifier.json");

        final List<SecurityProfileModel> securityProfileModelList = JsonHandler.getFromFileAsTypeReference(
            securityProfileFiles,
            new TypeReference<>() {}
        );
        RequestResponse<SecurityProfileModel> response = securityProfileService.createSecurityProfiles(
            securityProfileModelList
        );

        assertThat(response.isOk()).isTrue();

        final RequestResponseOK<SecurityProfileModel> responseCast = (RequestResponseOK<SecurityProfileModel>) response;
        assertThat(responseCast.getResults().get(0).getIdentifier()).isEqualTo("ID1");
    }

    @Test
    @RunWithCustomExecutor
    public void givenSecurityProfilesTestDuplicateIdentifierNotAllowedInCreation() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(EXTERNAL_TENANT);
        final File securityProfileFiles = PropertiesUtils.getResourceFile(
            "security_profile_ko_duplicate_identifier_in_creation.json"
        );

        final List<SecurityProfileModel> securityProfileModelList = JsonHandler.getFromFileAsTypeReference(
            securityProfileFiles,
            new TypeReference<>() {}
        );
        RequestResponse<SecurityProfileModel> response = securityProfileService.createSecurityProfiles(
            securityProfileModelList
        );

        assertThat(response.isOk()).isFalse();
    }

    @Test
    @RunWithCustomExecutor
    public void givenSecurityProfilesTestAlreadyExistsContract() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File securityProfileFiles = PropertiesUtils.getResourceFile("security_profile_ok.json");

        List<SecurityProfileModel> securityProfileModelList = JsonHandler.getFromFileAsTypeReference(
            securityProfileFiles,
            new TypeReference<>() {}
        );
        JsonHandler.getFromFileAsTypeReference(securityProfileFiles, new TypeReference<>() {});
        RequestResponse<SecurityProfileModel> response = securityProfileService.createSecurityProfiles(
            securityProfileModelList
        );

        final RequestResponseOK<SecurityProfileModel> responseCast = (RequestResponseOK<SecurityProfileModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);

        // Try recreate
        response = securityProfileService.createSecurityProfiles(securityProfileModelList);

        assertThat(response.isOk()).isFalse();
    }

    @Test
    @RunWithCustomExecutor
    public void givenSecurityProfileWithForFullAccessReturnOK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File securityProfileFiles = PropertiesUtils.getResourceFile("security_profile_ok_full_access.json");
        final List<SecurityProfileModel> securityProfileModelList = JsonHandler.getFromFileAsTypeReference(
            securityProfileFiles,
            new TypeReference<>() {}
        );
        final RequestResponse<SecurityProfileModel> response = securityProfileService.createSecurityProfiles(
            securityProfileModelList
        );

        assertThat(response.isOk()).isTrue();
    }

    @Test
    @RunWithCustomExecutor
    public void givenSecurityProfileWithUnauthorizedPermissionSetForFullAccessReturnBadRequest() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File securityProfileFiles = PropertiesUtils.getResourceFile(
            "security_profile_ko_permissions_with_full_access_mode.json"
        );
        final List<SecurityProfileModel> securityProfileModelList = JsonHandler.getFromFileAsTypeReference(
            securityProfileFiles,
            new TypeReference<>() {}
        );
        final RequestResponse<SecurityProfileModel> response = securityProfileService.createSecurityProfiles(
            securityProfileModelList
        );

        assertThat(response.isOk()).isFalse();
    }

    @Test
    @RunWithCustomExecutor
    public void givenSecurityProfileTestFindByFakeID() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        // find securityProfile with the fake id should return Status.OK

        final SelectParserSingle parser = new SelectParserSingle(new SingleVarNameAdapter());
        final Select select = new Select();
        parser.parse(select.getFinalSelect());
        parser.addCondition(QueryHelper.eq("#id", "fakeid"));
        final JsonNode queryDsl = parser.getRequest().getFinalSelect();
        /*
         * { "$query" : [ { "$eq" : { "_id" : "fake_id" } } ] }
         */
        final RequestResponseOK<SecurityProfileModel> securityProfileModelList =
            securityProfileService.findSecurityProfiles(queryDsl);

        assertThat(securityProfileModelList.getResults()).isEmpty();
    }

    @Test
    @RunWithCustomExecutor
    public void givenSecurityProfileTestFindById() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        File securityProfileFiles = PropertiesUtils.getResourceFile("security_profile_ok.json");
        List<SecurityProfileModel> securityProfileModelList = JsonHandler.getFromFileAsTypeReference(
            securityProfileFiles,
            new TypeReference<>() {}
        );
        RequestResponse<SecurityProfileModel> createResponse = securityProfileService.createSecurityProfiles(
            securityProfileModelList
        );

        assertThat(createResponse.isOk()).isTrue();
        final RequestResponseOK<SecurityProfileModel> createResponseCast = (RequestResponseOK<
                SecurityProfileModel
            >) createResponse;
        assertThat(createResponseCast.getResults()).hasSize(2);
        assertThat(createResponseCast.getResults().get(0).getId()).isNotEmpty();

        String id = createResponseCast.getResults().get(0).getId();

        final SelectParserSingle parser = new SelectParserSingle(new SingleVarNameAdapter());
        final Select select = new Select();
        parser.parse(select.getFinalSelect());
        parser.addCondition(QueryHelper.eq("#id", id));
        final JsonNode queryDsl = parser.getRequest().getFinalSelect();
        /*
         * { "$query" : [ { "$eq" : { "_id" : id } } ] }
         */
        final RequestResponseOK<SecurityProfileModel> findResponse = securityProfileService.findSecurityProfiles(
            queryDsl
        );

        assertThat(findResponse.getResults().size()).isEqualTo(1);
        assertThat(findResponse.getResults().get(0).getId()).isEqualTo(id);
        assertThat(findResponse.getResults().get(0).getIdentifier()).isNotEmpty();
        assertThat(findResponse.getResults().get(0).getName()).isEqualTo("SEC_PROFILE_1");
        assertThat(findResponse.getResults().get(0).getPermissions().size()).isEqualTo(3);
        assertThat(findResponse.getResults().get(0).getPermissions()).contains("rules:id:read");
    }

    @Test
    @RunWithCustomExecutor
    public void givenSecurityProfileTestFindOneByIdentifier() throws Exception {
        // Create profiles
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        File securityProfileFiles = PropertiesUtils.getResourceFile("security_profile_ok.json");
        List<SecurityProfileModel> securityProfileModelList = JsonHandler.getFromFileAsTypeReference(
            securityProfileFiles,
            new TypeReference<>() {}
        );
        RequestResponse<SecurityProfileModel> createResponse = securityProfileService.createSecurityProfiles(
            securityProfileModelList
        );

        assertThat(createResponse.isOk()).isTrue();
        final RequestResponseOK<SecurityProfileModel> createResponseCast = (RequestResponseOK<
                SecurityProfileModel
            >) createResponse;
        assertThat(createResponseCast.getResults()).hasSize(2);
        assertThat(createResponseCast.getResults().get(0).getId()).isNotEmpty();

        String identifier = createResponseCast.getResults().get(0).getIdentifier();

        // Find one profile by identifier
        final Optional<SecurityProfileModel> findResponse = securityProfileService.findOneByIdentifier(identifier);

        assertThat(findResponse.isPresent()).isTrue();
        assertThat(findResponse.get().getId()).isNotEmpty();
        assertThat(findResponse.get().getIdentifier()).isEqualTo(identifier);
        assertThat(findResponse.get().getName()).isEqualTo("SEC_PROFILE_1");
        assertThat(findResponse.get().getPermissions().size()).isEqualTo(3);
        assertThat(findResponse.get().getPermissions()).contains("rules:id:read");
    }

    @Test
    @RunWithCustomExecutor
    public void givenSecurityProfileTestFindOneByIdentifierWithFakeIdentifer() throws Exception {
        // Find one profile by identifier
        final Optional<SecurityProfileModel> findResponse = securityProfileService.findOneByIdentifier(
            "FakeIdentifier"
        );

        assertThat(findResponse).isEmpty();
    }

    @Test
    @RunWithCustomExecutor
    public void givenSecurityProfileTestUpdatePermissions_OK() throws Exception {
        String NewPermission = "securityprofiles:read";

        // Create security profiles
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        File securityProfileFiles = PropertiesUtils.getResourceFile("security_profile_ok.json");
        List<SecurityProfileModel> securityProfileModelList = JsonHandler.getFromFileAsTypeReference(
            securityProfileFiles,
            new TypeReference<>() {}
        );
        RequestResponse<SecurityProfileModel> createResponse = securityProfileService.createSecurityProfiles(
            securityProfileModelList
        );

        assertThat(createResponse.isOk()).isTrue();
        final RequestResponseOK<SecurityProfileModel> responseCast = (RequestResponseOK<
                SecurityProfileModel
            >) createResponse;
        assertThat(responseCast.getResults()).hasSize(2);

        String identifier = responseCast.getResults().get(0).getIdentifier();

        // Find security profile
        final Optional<SecurityProfileModel> securityProfileModel = securityProfileService.findOneByIdentifier(
            identifier
        );

        assertThat(securityProfileModel.isPresent()).isTrue();
        assertThat(securityProfileModel.get().getName()).isEqualTo("SEC_PROFILE_1");
        assertThat(securityProfileModel.get().getPermissions().size()).isEqualTo(3);

        // Add permission
        final UpdateParserSingle updateParser = new UpdateParserSingle(new SingleVarNameAdapter());
        final Update update = new Update();
        update.setQuery(QueryHelper.eq(NAME, "SEC_PROFILE_1"));
        final AddAction setActionAddPermission = UpdateActionHelper.add(PERMISSIONS, NewPermission);
        update.addActions(setActionAddPermission);
        updateParser.parse(update.getFinalUpdate());
        final JsonNode queryDslForUpdate = updateParser.getRequest().getFinalUpdate();

        RequestResponse<SecurityProfileModel> updateContractStatus = securityProfileService.updateSecurityProfile(
            securityProfileModel.get().getIdentifier(),
            queryDslForUpdate
        );
        assertThat(updateContractStatus.isOk()).isTrue();

        // Retry finding security profiles
        final Optional<SecurityProfileModel> securityProfileModel2 = securityProfileService.findOneByIdentifier(
            identifier
        );

        assertThat(securityProfileModel2.isPresent()).isTrue();
        assertThat(securityProfileModel2.get().getName()).isEqualTo("SEC_PROFILE_1");
        assertThat(securityProfileModel2.get().getPermissions().size()).isEqualTo(4);
        assertThat(securityProfileModel2.get().getPermissions()).contains(NewPermission);
    }

    @Test
    @RunWithCustomExecutor
    public void givenSecurityProfileTestUnknownUpdatePermissions_KO() throws Exception {
        String NewPermission = "test:test";

        // Create security profiles
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        File securityProfileFiles = PropertiesUtils.getResourceFile("security_profile_ok.json");
        List<SecurityProfileModel> securityProfileModelList = JsonHandler.getFromFileAsTypeReference(
            securityProfileFiles,
            new TypeReference<>() {}
        );
        RequestResponse<SecurityProfileModel> createResponse = securityProfileService.createSecurityProfiles(
            securityProfileModelList
        );

        assertThat(createResponse.isOk()).isTrue();
        final RequestResponseOK<SecurityProfileModel> responseCast = (RequestResponseOK<
                SecurityProfileModel
            >) createResponse;
        assertThat(responseCast.getResults()).hasSize(2);

        String identifier = responseCast.getResults().get(0).getIdentifier();

        // Find security profile
        final Optional<SecurityProfileModel> securityProfileModel = securityProfileService.findOneByIdentifier(
            identifier
        );

        assertThat(securityProfileModel.isPresent()).isTrue();
        assertThat(securityProfileModel.get().getName()).isEqualTo("SEC_PROFILE_1");
        assertThat(securityProfileModel.get().getPermissions().size()).isEqualTo(3);

        // Add permission
        final UpdateParserSingle updateParser = new UpdateParserSingle(new SingleVarNameAdapter());
        final Update update = new Update();
        update.setQuery(QueryHelper.eq(NAME, "SEC_PROFILE_1"));
        final AddAction setActionAddPermission = UpdateActionHelper.add(PERMISSIONS, NewPermission);
        update.addActions(setActionAddPermission);
        updateParser.parse(update.getFinalUpdate());
        final JsonNode queryDslForUpdate = updateParser.getRequest().getFinalUpdate();

        RequestResponse<SecurityProfileModel> updatedSecurityProfileResponse =
            securityProfileService.updateSecurityProfile(securityProfileModel.get().getIdentifier(), queryDslForUpdate);
        assertThat(updatedSecurityProfileResponse.isOk()).isFalse();
        assertThat(
            ((VitamError<SecurityProfileModel>) updatedSecurityProfileResponse).getErrors().get(0).getMessage()
        ).isEqualTo("Au moins une permission n'existe pas.");
        // Retry finding security profiles
        final Optional<SecurityProfileModel> securityProfileModel2 = securityProfileService.findOneByIdentifier(
            identifier
        );

        assertThat(securityProfileModel2.isPresent()).isTrue();
        assertThat(securityProfileModel2.get().getName()).isEqualTo("SEC_PROFILE_1");
        assertThat(securityProfileModel2.get().getPermissions().size()).isEqualTo(3);
        assertThat(securityProfileModel2.get().getPermissions()).doesNotContain(NewPermission);
    }

    @Test
    @RunWithCustomExecutor
    public void givenSecurityProfilesTestFindAllThenReturnEmpty() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final RequestResponseOK<SecurityProfileModel> securityProfileModelList =
            securityProfileService.findSecurityProfiles(JsonHandler.createObjectNode());
        assertThat(securityProfileModelList.getResults()).isEmpty();
    }

    @Test
    @RunWithCustomExecutor
    public void givenSecurityProfilesTestFindAllThenReturnTwoContracts() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File securityProfileFiles = PropertiesUtils.getResourceFile("security_profile_ok.json");
        final List<SecurityProfileModel> securityProfileModelList = JsonHandler.getFromFileAsTypeReference(
            securityProfileFiles,
            new TypeReference<>() {}
        );
        final RequestResponse<SecurityProfileModel> response = securityProfileService.createSecurityProfiles(
            securityProfileModelList
        );

        final RequestResponseOK<SecurityProfileModel> responseCast = (RequestResponseOK<SecurityProfileModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);

        final RequestResponseOK<SecurityProfileModel> securityProfileModelListSearch =
            securityProfileService.findSecurityProfiles(JsonHandler.createObjectNode());
        assertThat(securityProfileModelListSearch.getResults()).hasSize(2);
    }

    @Test
    @RunWithCustomExecutor
    public void givenSecurityProfileTestDeleteByIdentifier() throws Exception {
        // Create profiles
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        File securityProfileFiles = PropertiesUtils.getResourceFile("security_profile_ok.json");
        List<SecurityProfileModel> securityProfileModelList = JsonHandler.getFromFileAsTypeReference(
            securityProfileFiles,
            new TypeReference<>() {}
        );
        RequestResponse<SecurityProfileModel> createResponse = securityProfileService.createSecurityProfiles(
            securityProfileModelList
        );

        assertThat(createResponse.isOk()).isTrue();
        final RequestResponseOK<SecurityProfileModel> createResponseCast = (RequestResponseOK<
                SecurityProfileModel
            >) createResponse;
        assertThat(createResponseCast.getResults()).hasSize(2);
        assertThat(createResponseCast.getResults().get(0).getId()).isNotEmpty();

        String identifier = createResponseCast.getResults().get(0).getIdentifier();
        when(contextService.securityProfileIsUsedInContexts(anyString())).thenReturn(false);

        RequestResponse<SecurityProfileModel> response = securityProfileService.deleteSecurityProfile(identifier);
        assertThat(response.getHttpCode()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());

        final Optional<SecurityProfileModel> findResponse = securityProfileService.findOneByIdentifier(identifier);
        assertThat(findResponse.isPresent()).isFalse();
    }
}
