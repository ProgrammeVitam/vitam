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
package fr.gouv.vitam.functional.administration.core.profile;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.parser.request.adapter.SingleVarNameAdapter;
import fr.gouv.vitam.common.database.parser.request.single.SelectParserSingle;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.ProfileModel;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.server.application.configuration.DbConfigurationImpl;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.Profile;
import fr.gouv.vitam.functional.administration.common.config.ElasticsearchFunctionalAdminIndexManager;
import fr.gouv.vitam.functional.administration.common.counter.VitamCounterService;
import fr.gouv.vitam.functional.administration.common.server.ElasticsearchAccessFunctionalAdmin;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollectionsTestUtils;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminFactory;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.functional.administration.core.backup.FunctionalBackupService;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fr.gouv.vitam.common.guid.GUIDFactory.newOperationLogbookGUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

public class ProfileServiceImplTest {

    static final String DATABASE_HOST = "localhost";
    private static final Integer TENANT_ID = 1;
    private static final Integer EXTERNAL_TENANT = 2;
    private static final String PREFIX = GUIDFactory.newGUID().getId();
    private static final ElasticsearchFunctionalAdminIndexManager indexManager =
        FunctionalAdminCollectionsTestUtils.createTestIndexManager();

    @ClassRule
    public static MongoRule mongoRule = new MongoRule(MongoDbAccess.getMongoClientSettingsBuilder(Profile.class));

    static ProfileService profileService;
    static FunctionalBackupService functionalBackupService = Mockito.mock(FunctionalBackupService.class);
    private static VitamCounterService vitamCounterService;
    private static MongoDbAccessAdminImpl dbImpl;

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        FunctionalAdminCollectionsTestUtils.beforeTestClass(
            mongoRule.getMongoDatabase(),
            PREFIX,
            new ElasticsearchAccessFunctionalAdmin(
                ElasticsearchRule.VITAM_CLUSTER,
                Lists.newArrayList(new ElasticsearchNode(ElasticsearchRule.getHost(), ElasticsearchRule.getPort())),
                indexManager
            ),
            Collections.singletonList(FunctionalAdminCollections.PROFILE)
        );

        final List<MongoDbNode> nodes = new ArrayList<>();
        nodes.add(new MongoDbNode(DATABASE_HOST, MongoRule.getDataBasePort()));

        dbImpl = MongoDbAccessAdminFactory.create(
            new DbConfigurationImpl(nodes, mongoRule.getMongoDatabase().getName()),
            Collections::emptyList,
            indexManager
        );
        final List<Integer> tenants = Arrays.asList(TENANT_ID, EXTERNAL_TENANT);
        Map<Integer, List<String>> listEnableExternalIdentifiers = new HashMap<>();
        List<String> list_tenant = new ArrayList<>();
        list_tenant.add("PROFILE");
        listEnableExternalIdentifiers.put(EXTERNAL_TENANT, list_tenant);

        vitamCounterService = new VitamCounterService(dbImpl, tenants, listEnableExternalIdentifiers);

        LogbookOperationsClientFactory.changeMode(null);

        profileService = new ProfileServiceImpl(
            MongoDbAccessAdminFactory.create(
                new DbConfigurationImpl(nodes, mongoRule.getMongoDatabase().getName()),
                Collections::emptyList,
                indexManager
            ),
            vitamCounterService,
            functionalBackupService
        );
    }

    @AfterClass
    public static void tearDownAfterClass() {
        FunctionalAdminCollectionsTestUtils.afterTestClass(true);
        profileService.close();
    }

    @Before
    public void setUp() {
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(TENANT_ID));
    }

    @After
    public void afterTest() {
        FunctionalAdminCollectionsTestUtils.afterTest();
        reset(functionalBackupService);
    }

    @Test
    @RunWithCustomExecutor
    public void givenTestImportSXDProfileFile() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileMetadataProfile = PropertiesUtils.getResourceFile("profile_ok.json");
        final List<ProfileModel> profileModelList = JsonHandler.getFromFileAsTypeReference(
            fileMetadataProfile,
            new TypeReference<>() {}
        );
        final RequestResponse<ProfileModel> response = profileService.createProfiles(profileModelList);

        assertThat(response.isOk()).isTrue();
        final RequestResponseOK<ProfileModel> responseCast = (RequestResponseOK<ProfileModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);

        final ProfileModel profileModel = responseCast.getResults().iterator().next();
        final InputStream xsdProfile = new FileInputStream(PropertiesUtils.getResourceFile("profile_ok.xsd"));

        RequestResponse<ProfileModel> requestResponse = profileService.importProfileFile(
            profileModel.getIdentifier(),
            xsdProfile
        );
        assertThat(requestResponse.isOk()).isTrue();
    }

    @Test
    @RunWithCustomExecutor
    public void givenTestImportRNGProfileFile() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileMetadataProfile = PropertiesUtils.getResourceFile("profile_ok.json");
        final List<ProfileModel> profileModelList = JsonHandler.getFromFileAsTypeReference(
            fileMetadataProfile,
            new TypeReference<>() {}
        );
        final RequestResponse<ProfileModel> response = profileService.createProfiles(profileModelList);

        assertThat(response.isOk()).isTrue();

        verify(functionalBackupService).saveCollectionAndSequence(
            any(),
            eq(ProfileServiceImpl.PROFILE_BACKUP_EVENT),
            eq(FunctionalAdminCollections.PROFILE),
            any()
        );
        verifyNoMoreInteractions(functionalBackupService);
        reset(functionalBackupService);

        final RequestResponseOK<ProfileModel> responseCast = (RequestResponseOK<ProfileModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);
        assertThat(responseCast.getResults().get(0).getIdentifier()).contains("PR-000");
        final ProfileModel profileModel = responseCast.getResults().get(1);
        InputStream xsdProfile = new FileInputStream(PropertiesUtils.getResourceFile("profile_ok.rng"));

        RequestResponse<ProfileModel> requestResponse = profileService.importProfileFile(
            profileModel.getIdentifier(),
            xsdProfile
        );
        assertThat(requestResponse.isOk()).isTrue();

        verify(functionalBackupService).saveFile(
            any(),
            any(),
            eq(ProfileServiceImpl.OP_PROFILE_STORAGE),
            eq(DataCategory.PROFILE),
            anyString()
        );
        verify(functionalBackupService).saveCollectionAndSequence(
            any(),
            eq(ProfileServiceImpl.PROFILE_BACKUP_EVENT),
            eq(FunctionalAdminCollections.PROFILE),
            any()
        );
        verifyNoMoreInteractions(functionalBackupService);
    }

    @Test
    @RunWithCustomExecutor
    public void givenTestImportNotValideRNGProfileFile() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileMetadataProfile = PropertiesUtils.getResourceFile("profile_ok.json");
        final List<ProfileModel> profileModelList = JsonHandler.getFromFileAsTypeReference(
            fileMetadataProfile,
            new TypeReference<>() {}
        );
        final RequestResponse<ProfileModel> response = profileService.createProfiles(profileModelList);

        assertThat(response.isOk()).isTrue();
        final RequestResponseOK<ProfileModel> responseCast = (RequestResponseOK<ProfileModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);

        final ProfileModel profileModel = responseCast.getResults().get(1);
        final InputStream xsdProfile = new FileInputStream(PropertiesUtils.getResourceFile("profile_ok.xsd"));

        RequestResponse<ProfileModel> requestResponse = profileService.importProfileFile(
            profileModel.getIdentifier(),
            xsdProfile
        );
        assertThat(requestResponse.isOk()).isFalse();
    }

    @Test
    @RunWithCustomExecutor
    public void givenTestDownloadProfileFile() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileMetadataProfile = PropertiesUtils.getResourceFile("profile_ok.json");
        final List<ProfileModel> profileModelList = JsonHandler.getFromFileAsTypeReference(
            fileMetadataProfile,
            new TypeReference<>() {}
        );
        final RequestResponse<ProfileModel> response = profileService.createProfiles(profileModelList);

        assertThat(response.isOk()).isTrue();
        final RequestResponseOK<ProfileModel> responseCast = (RequestResponseOK<ProfileModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);

        final ProfileModel profileModel = responseCast.getResults().iterator().next();
        final InputStream xsdProfile = new FileInputStream(PropertiesUtils.getResourceFile("profile_ok.xsd"));

        RequestResponse<ProfileModel> requestResponse = profileService.importProfileFile(
            profileModel.getIdentifier(),
            xsdProfile
        );
        assertThat(requestResponse.isOk()).isTrue();

        javax.ws.rs.core.Response responseDown = profileService.downloadProfileFile(profileModel.getIdentifier());
        assertThat(responseDown.hasEntity()).isTrue();
    }

    @Test
    @RunWithCustomExecutor
    public void givenWellFormedProfileMetadataThenImportSuccessfully() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileMetadataProfile = PropertiesUtils.getResourceFile("profile_ok.json");
        final List<ProfileModel> profileModelList = JsonHandler.getFromFileAsTypeReference(
            fileMetadataProfile,
            new TypeReference<>() {}
        );
        final RequestResponse<ProfileModel> response = profileService.createProfiles(profileModelList);

        assertThat(response.isOk()).isTrue();
        final RequestResponseOK<ProfileModel> responseCast = (RequestResponseOK<ProfileModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);
    }

    @Test
    @RunWithCustomExecutor
    public void givenATestMissingIdentifierReturnBadRequest() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(EXTERNAL_TENANT);
        final File fileMetadataProfile = PropertiesUtils.getResourceFile("profile_missing_identifier.json");
        final List<ProfileModel> profileModelList = JsonHandler.getFromFileAsTypeReference(
            fileMetadataProfile,
            new TypeReference<>() {}
        );
        final RequestResponse<ProfileModel> response = profileService.createProfiles(profileModelList);

        assertThat(response.isOk()).isFalse();
        verifyZeroInteractions(functionalBackupService);
    }

    @Test
    @RunWithCustomExecutor
    public void givenTestIdentifiers() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileMetadataProfile = PropertiesUtils.getResourceFile("profile_duplicate_identifier.json");
        final List<ProfileModel> profileModelList = JsonHandler.getFromFileAsTypeReference(
            fileMetadataProfile,
            new TypeReference<>() {}
        );
        final RequestResponse<ProfileModel> response = profileService.createProfiles(profileModelList);

        assertThat(response.isOk()).isFalse();
    }

    @Test
    @RunWithCustomExecutor
    public void givenTestDuplicateNames() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileMetadataProfile = PropertiesUtils.getResourceFile("profile_duplicate_name.json");
        final List<ProfileModel> profileModelList = JsonHandler.getFromFileAsTypeReference(
            fileMetadataProfile,
            new TypeReference<>() {}
        );
        final RequestResponse<ProfileModel> response = profileService.createProfiles(profileModelList);

        assertThat(response.isOk()).isTrue();
    }

    @Test
    @RunWithCustomExecutor
    public void givenTestFilledPath() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileMetadataProfile = PropertiesUtils.getResourceFile("profile_filled_path.json");
        final List<ProfileModel> profileModelList = JsonHandler.getFromFileAsTypeReference(
            fileMetadataProfile,
            new TypeReference<>() {}
        );
        final RequestResponse<ProfileModel> response = profileService.createProfiles(profileModelList);

        assertThat(response.isOk()).isFalse();
        assertThat(((VitamError<ProfileModel>) response).getErrors().size()).isEqualTo(1);
        assertThat(((VitamError<ProfileModel>) response).getErrors().get(0).getMessage()).isEqualTo(
            ProfileServiceImpl.PATH_SHOULD_NOT_BE_FILLED
        );
    }

    @Test
    @RunWithCustomExecutor
    public void givenTestNotAllowedNotNullIdInCreation() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileMetadataProfile = PropertiesUtils.getResourceFile("profile_ok.json");

        final List<ProfileModel> profileModelList = JsonHandler.getFromFileAsTypeReference(
            fileMetadataProfile,
            new TypeReference<>() {}
        );
        RequestResponse<ProfileModel> response = profileService.createProfiles(profileModelList);

        final RequestResponseOK<ProfileModel> responseCast = (RequestResponseOK<ProfileModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);

        // Try to recreate the same profile but with id
        response = profileService.createProfiles(responseCast.getResults());

        assertThat(response.isOk()).isFalse();
    }

    @Test
    @RunWithCustomExecutor
    public void givenTestFindByFakeID() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        // find profile with the fake id should return Status.OK

        final SelectParserSingle parser = new SelectParserSingle(new SingleVarNameAdapter());
        final Select select = new Select();
        parser.parse(select.getFinalSelect());
        parser.addCondition(QueryHelper.eq("#id", "fakeid"));
        final JsonNode queryDsl = parser.getRequest().getFinalSelect();
        /*
         * String q = "{ \"$query\" : [ { \"$eq\" : { \"_id\" : \"fake_id\" } } ] }"; JsonNode queryDsl =
         * JsonHandler.getFromString(q);
         */
        final RequestResponseOK<ProfileModel> profileModelList = profileService.findProfiles(queryDsl);

        assertThat(profileModelList.getResults()).isEmpty();
    }

    /**
     * Check that the created access conrtact have the tenant owner after persisted to database
     *
     * @throws Exception
     */
    @Test
    @RunWithCustomExecutor
    public void givenTestTenantOwner() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileMetadataProfile = PropertiesUtils.getResourceFile("profile_ok.json");
        final List<ProfileModel> profileModelList = JsonHandler.getFromFileAsTypeReference(
            fileMetadataProfile,
            new TypeReference<>() {}
        );
        final RequestResponse<ProfileModel> response = profileService.createProfiles(profileModelList);

        final RequestResponseOK<ProfileModel> responseCast = (RequestResponseOK<ProfileModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);

        // We juste test the first profile
        final ProfileModel acm = responseCast.getResults().iterator().next();
        assertThat(acm).isNotNull();

        String id1 = acm.getIdentifier();
        assertThat(id1).isNotNull();

        ProfileModel one = profileService.findByIdentifier(id1);
        assertThat(one).isNotNull();
        assertThat(one.getName()).isEqualTo(acm.getName());
        assertThat(one.getTenant()).isNotNull();
        assertThat(one.getTenant()).isEqualTo(TENANT_ID);
    }

    /**
     * Profile of tenant 1, try to get the same profile with id mongo but with tenant 2 This sgould not return the
     * profile as tenant 2 is not the owner of the profile
     *
     * @throws Exception
     */
    @Test
    @RunWithCustomExecutor
    public void givenTestNotTenantOwner() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileMetadataProfile = PropertiesUtils.getResourceFile("profile_ok.json");
        final List<ProfileModel> profileModelList = JsonHandler.getFromFileAsTypeReference(
            fileMetadataProfile,
            new TypeReference<>() {}
        );
        final RequestResponse<ProfileModel> response = profileService.createProfiles(profileModelList);

        final RequestResponseOK<ProfileModel> responseCast = (RequestResponseOK<ProfileModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);

        // We juste test the first profile
        final ProfileModel acm = responseCast.getResults().iterator().next();
        assertThat(acm).isNotNull();

        final String id1 = acm.getId();
        assertThat(id1).isNotNull();

        VitamThreadUtils.getVitamSession().setTenantId(2);
        final ProfileModel one = profileService.findByIdentifier(id1);
        assertThat(one).isNull();
    }

    @Test
    @RunWithCustomExecutor
    public void givenTestfindByID() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(EXTERNAL_TENANT);
        final File fileMetadataProfile = PropertiesUtils.getResourceFile("profile_ok.json");
        final List<ProfileModel> profileModelList = JsonHandler.getFromFileAsTypeReference(
            fileMetadataProfile,
            new TypeReference<>() {}
        );
        final RequestResponse<ProfileModel> response = profileService.createProfiles(profileModelList);
        assertThat(response.isOk()).isFalse();
    }

    @Test
    @RunWithCustomExecutor
    public void givenTestImportExternalIdentifier_KO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(EXTERNAL_TENANT);
        final File fileMetadataProfile = PropertiesUtils.getResourceFile("profile_ok_id.json");
        final List<ProfileModel> profileModelList = JsonHandler.getFromFileAsTypeReference(
            fileMetadataProfile,
            new TypeReference<>() {}
        );
        final RequestResponse<ProfileModel> response = profileService.createProfiles(profileModelList);

        final RequestResponseOK<ProfileModel> responseCast = (RequestResponseOK<ProfileModel>) response;
        assertThat(responseCast.getResults()).hasSize(1);

        // We juste test the first profile
        final ProfileModel acm = responseCast.getResults().iterator().next();
        assertThat(acm).isNotNull();

        String id1 = acm.getIdentifier();
        assertThat(id1).isNotNull();

        ProfileModel one = profileService.findByIdentifier(id1);

        assertThat(one).isNotNull();

        assertThat(one.getName()).isEqualTo(acm.getName());
    }

    @Test
    @RunWithCustomExecutor
    public void givenTestImportExternalIdentifier() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileMetadataProfile = PropertiesUtils.getResourceFile("profile_ok.json");
        final List<ProfileModel> profileModelList = JsonHandler.getFromFileAsTypeReference(
            fileMetadataProfile,
            new TypeReference<>() {}
        );
        final RequestResponse<ProfileModel> response = profileService.createProfiles(profileModelList);

        final RequestResponseOK<ProfileModel> responseCast = (RequestResponseOK<ProfileModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);

        // We juste test the first profile
        final ProfileModel acm = responseCast.getResults().iterator().next();
        assertThat(acm).isNotNull();

        String id1 = acm.getIdentifier();
        assertThat(id1).isNotNull();

        ProfileModel one = profileService.findByIdentifier(id1);

        assertThat(one).isNotNull();

        assertThat(one.getName()).isEqualTo(acm.getName());
    }

    @Test
    @RunWithCustomExecutor
    public void givenTestFindAllThenReturnEmpty() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final RequestResponseOK<ProfileModel> profileModelList = profileService.findProfiles(
            JsonHandler.createObjectNode()
        );
        assertThat(profileModelList.getResults()).isEmpty();
    }

    @Test
    @RunWithCustomExecutor
    public void givenTestFindAllThenReturnTwoProfiles() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileMetadataProfile = PropertiesUtils.getResourceFile("profile_ok.json");
        final List<ProfileModel> profileModelList = JsonHandler.getFromFileAsTypeReference(
            fileMetadataProfile,
            new TypeReference<>() {}
        );
        final RequestResponse<ProfileModel> response = profileService.createProfiles(profileModelList);

        final RequestResponseOK<ProfileModel> responseCast = (RequestResponseOK<ProfileModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);

        final RequestResponseOK<ProfileModel> profileModelListSearch = profileService.findProfiles(
            JsonHandler.createObjectNode()
        );
        assertThat(profileModelListSearch.getResults()).hasSize(2);
    }

    @Test
    @RunWithCustomExecutor
    public void givenTestFindByIdentifier() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileMetadataProfile = PropertiesUtils.getResourceFile("profile_ok.json");
        final List<ProfileModel> profileModelList = JsonHandler.getFromFileAsTypeReference(
            fileMetadataProfile,
            new TypeReference<>() {}
        );
        final RequestResponse<ProfileModel> response = profileService.createProfiles(profileModelList);

        final RequestResponseOK<ProfileModel> responseCast = (RequestResponseOK<ProfileModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);

        final ProfileModel acm = profileModelList.iterator().next();
        assertThat(acm).isNotNull();

        final String id1 = acm.getId();
        assertThat(id1).isNotNull();

        final String identifier = acm.getIdentifier();
        assertThat(identifier).isNotNull();

        final SelectParserSingle parser = new SelectParserSingle(new SingleVarNameAdapter());
        final Select select = new Select();
        parser.parse(select.getFinalSelect());
        parser.addCondition(QueryHelper.eq("Identifier", identifier));
        final JsonNode queryDsl = parser.getRequest().getFinalSelect();

        final RequestResponseOK<ProfileModel> profileModelListFound = profileService.findProfiles(queryDsl);
        assertThat(profileModelListFound.getResults()).hasSize(1);

        final ProfileModel acmFound = profileModelListFound.getResults().iterator().next();
        assertThat(acmFound).isNotNull();
        assertThat(acmFound.getIdentifier()).isEqualTo(identifier);
    }

    @Test
    @RunWithCustomExecutor
    public void should_update_profil() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileMetadataProfile = PropertiesUtils.getResourceFile("profile_ok_1.json");
        final List<ProfileModel> profileModelList = JsonHandler.getFromFileAsTypeReference(
            fileMetadataProfile,
            new TypeReference<>() {}
        );
        RequestResponse<ProfileModel> response = profileService.createProfiles(profileModelList);

        assertThat(response.isOk()).isTrue();

        verify(functionalBackupService).saveCollectionAndSequence(
            any(),
            eq(ProfileServiceImpl.PROFILE_BACKUP_EVENT),
            eq(FunctionalAdminCollections.PROFILE),
            any()
        );
        verifyNoMoreInteractions(functionalBackupService);
        reset(functionalBackupService);

        Select select = new Select();
        select.setQuery(QueryHelper.eq(ProfileModel.TAG_NAME, "PToUpdate"));
        RequestResponseOK<ProfileModel> result = profileService.findProfiles(select.getFinalSelect());
        ProfileModel profil = result.getFirstResult();
        assertThat(profil).isNotNull();
        assertThat(profil.getName()).isEqualTo("PToUpdate");

        //profileService.
        String query =
            "{\"$query\":{\"$eq\":{\"Identifier\":\"" +
            profil.getIdentifier() +
            "\"}},\"$filter\":{},\"$action\":[{\"$set\":{\"Name\":\"profil test \"}}]}";
        response = profileService.updateProfile(profil, JsonHandler.getFromString(query));

        assertThat(response.isOk()).isTrue();

        verify(functionalBackupService).saveCollectionAndSequence(
            any(),
            eq(ProfileServiceImpl.PROFILE_BACKUP_EVENT),
            eq(FunctionalAdminCollections.PROFILE),
            any()
        );
        verifyNoMoreInteractions(functionalBackupService);
    }

    @Test
    @RunWithCustomExecutor
    public void should_create_profile_with_same_name() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileMetadataProfile = PropertiesUtils.getResourceFile("profile_ok_same_name.json");
        final List<ProfileModel> profileModelList = JsonHandler.getFromFileAsTypeReference(
            fileMetadataProfile,
            new TypeReference<>() {}
        );
        // When
        RequestResponse<ProfileModel> response = profileService.createProfiles(profileModelList);
        // Then
        assertThat(response.isOk()).isTrue();
    }

    @Test
    @RunWithCustomExecutor
    public void should_not_update_profil_due_to_path_update() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileMetadataProfile = PropertiesUtils.getResourceFile("profile_ok_1.json");
        final List<ProfileModel> profileModelList = JsonHandler.getFromFileAsTypeReference(
            fileMetadataProfile,
            new TypeReference<>() {}
        );
        RequestResponse<ProfileModel> response = profileService.createProfiles(profileModelList);

        assertThat(response.isOk()).isTrue();

        verify(functionalBackupService).saveCollectionAndSequence(
            any(),
            eq(ProfileServiceImpl.PROFILE_BACKUP_EVENT),
            eq(FunctionalAdminCollections.PROFILE),
            any()
        );
        verifyNoMoreInteractions(functionalBackupService);
        reset(functionalBackupService);

        Select select = new Select();
        select.setQuery(QueryHelper.eq(ProfileModel.TAG_NAME, "PToUpdate"));
        RequestResponseOK<ProfileModel> result = profileService.findProfiles(select.getFinalSelect());
        ProfileModel profil = result.getFirstResult();
        assertThat(profil).isNotNull();
        assertThat(profil.getName()).isEqualTo("PToUpdate");

        //profileService.
        String query =
            "{\"$query\":{\"$eq\":{\"Identifier\":\"" +
            profil.getIdentifier() +
            "\"}},\"$filter\":{},\"$action\":[{\"$set\":{\"Path\":\"updated Path \"}}]}";
        response = profileService.updateProfile(profil, JsonHandler.getFromString(query));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
        assertThat(((VitamError<ProfileModel>) response).getErrors().size()).isEqualTo(1);
        assertThat(((VitamError<ProfileModel>) response).getErrors().get(0).getMessage()).isEqualTo(
            ProfileServiceImpl.PATH_SHOULD_NOT_BE_FILLED
        );
    }
}
