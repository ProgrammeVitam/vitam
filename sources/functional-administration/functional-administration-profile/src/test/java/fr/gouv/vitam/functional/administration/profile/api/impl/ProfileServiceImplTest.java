/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019) <p> contact.vitam@culture.gouv.fr <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently. <p> This software is governed by the CeCILL 2.1 license under French law and
 * abiding by the rules of distribution of free software. You can use, modify and/ or redistribute the software under
 * the terms of the CeCILL 2.1 license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info". <p> As a counterpart to the access to the source code and rights to copy, modify and
 * redistribute granted by the license, users are provided only with a limited warranty and the software's author, the
 * holder of the economic rights, and the successive licensors have only limited liability. <p> In this respect, the
 * user's attention is drawn to the risks associated with loading, using, modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software, that may mean that it is complicated to
 * manipulate, and that also therefore means that it is reserved for developers and experienced professionals having
 * in-depth computer knowledge. Users are therefore encouraged to load and test the software's suitability as regards
 * their requirements in conditions enabling the security of their systems and/or data to be ensured and, more
 * generally, to use and operate it in the same conditions as regards security. <p> The fact that you are presently
 * reading this means that you have had knowledge of the CeCILL 2.1 license and that you accept its terms.
 */
package fr.gouv.vitam.functional.administration.profile.api.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import com.fasterxml.jackson.core.type.TypeReference;
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
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.parser.request.adapter.SingleVarNameAdapter;
import fr.gouv.vitam.common.database.parser.request.single.SelectParserSingle;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.server.application.configuration.DbConfigurationImpl;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.common.server.application.junit.AsyncResponseJunitTest;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.model.ProfileModel;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminFactory;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.functional.administration.counter.VitamCounterService;
import fr.gouv.vitam.functional.administration.profile.api.ProfileService;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

public class ProfileServiceImplTest {

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor());

    private static final Integer TENANT_ID = 1;

    static JunitHelper junitHelper;
    static final String COLLECTION_NAME = "Profile";
    static final String DATABASE_HOST = "localhost";
    static final String DATABASE_NAME = "vitam-test";
    static MongodExecutable mongodExecutable;
    static MongodProcess mongod;
    static MongoClient client;
    private static VitamCounterService vitamCounterService;
    private static MongoDbAccessAdminImpl dbImpl;

    static ProfileService profileService;
    static int mongoPort;
    static WorkspaceClient workspaceClient;
    static WorkspaceClientFactory workspaceClientFactory;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        final MongodStarter starter = MongodStarter.getDefaultInstance();
        junitHelper = JunitHelper.getInstance();
        mongoPort = junitHelper.findAvailablePort();
        mongodExecutable = starter.prepare(new MongodConfigBuilder()
            .withLaunchArgument("--enableMajorityReadConcern")
            .version(Version.Main.PRODUCTION)
            .net(new Net(mongoPort, Network.localhostIsIPv6()))
            .build());
        mongod = mongodExecutable.start();
        client = new MongoClient(new ServerAddress(DATABASE_HOST, mongoPort));

        final List<MongoDbNode> nodes = new ArrayList<>();
        nodes.add(new MongoDbNode(DATABASE_HOST, mongoPort));

        dbImpl = MongoDbAccessAdminFactory.create(new DbConfigurationImpl(nodes, DATABASE_NAME));
        final List tenants = new ArrayList<>();
        tenants.add(new Integer(TENANT_ID));
        vitamCounterService = new VitamCounterService(dbImpl, tenants);

        workspaceClientFactory = mock(WorkspaceClientFactory.class);
        workspaceClient = mock(WorkspaceClient.class);
        LogbookOperationsClientFactory.changeMode(null);

        profileService =
            new ProfileServiceImpl(MongoDbAccessAdminFactory.create(new DbConfigurationImpl(nodes, DATABASE_NAME)),
                workspaceClientFactory, vitamCounterService);

    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        mongod.stop();
        mongodExecutable.stop();
        junitHelper.releasePort(mongoPort);
        client.close();
        profileService.close();
    }

    @After
    public void afterTest() {
        final MongoCollection<Document> collection = client.getDatabase(DATABASE_NAME).getCollection(COLLECTION_NAME);
        collection.deleteMany(new Document());
    }


    @Test
    @RunWithCustomExecutor
    public void givenTestImportSXDProfileFile() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileMetadataProfile = PropertiesUtils.getResourceFile("profile_ok.json");
        final List<ProfileModel> profileModelList =
            JsonHandler.getFromFileAsTypeRefence(fileMetadataProfile, new TypeReference<List<ProfileModel>>() {});
        final RequestResponse response = profileService.createProfiles(profileModelList);

        assertThat(response.isOk());
        final RequestResponseOK<ProfileModel> responseCast = (RequestResponseOK<ProfileModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);

        final ProfileModel profileModel = responseCast.getResults().iterator().next();
        final InputStream xsdProfile = new FileInputStream(PropertiesUtils.getResourceFile("profile_ok.xsd"));

        given(workspaceClientFactory.getClient()).willReturn(workspaceClient);
        doAnswer(invocation -> xsdProfile).when(workspaceClient).createContainer(anyString());
        doAnswer(invocation -> xsdProfile).when(workspaceClient).putObject(anyString(), anyString(),
            any(InputStream.class));


        RequestResponse requestResponse = profileService.importProfileFile(profileModel.getIdentifier(), xsdProfile);
        assertThat(requestResponse.isOk()).isTrue();

    }

    @Test
    @RunWithCustomExecutor
    public void givenTestImportRNGProfileFile() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileMetadataProfile = PropertiesUtils.getResourceFile("profile_ok.json");
        final List<ProfileModel> profileModelList =
            JsonHandler.getFromFileAsTypeRefence(fileMetadataProfile, new TypeReference<List<ProfileModel>>() {});
        final RequestResponse response = profileService.createProfiles(profileModelList);

        assertThat(response.isOk());
        final RequestResponseOK<ProfileModel> responseCast = (RequestResponseOK<ProfileModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);
        assertThat(responseCast.getResults().get(0).getIdentifier()).contains("PR-000");
        assertThat(responseCast.getResults().get(1).getIdentifier()).contains("aIdentifier");
        final ProfileModel profileModel = responseCast.getResults().get(1);
        InputStream xsdProfile = new FileInputStream(PropertiesUtils.getResourceFile("profile_ok.rng"));


        doAnswer(invocation -> null).when(workspaceClient).createContainer(anyString());
        doAnswer(invocation -> xsdProfile).when(workspaceClient).putObject(anyString(), anyString(),
            any(InputStream.class));
        given(workspaceClientFactory.getClient()).willReturn(workspaceClient);


        RequestResponse requestResponse = profileService.importProfileFile(profileModel.getIdentifier(), xsdProfile);
        assertThat(requestResponse.isOk()).isTrue();

    }

    @Test
    @RunWithCustomExecutor
    public void givenTestImportNotValideRNGProfileFile() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileMetadataProfile = PropertiesUtils.getResourceFile("profile_ok.json");
        final List<ProfileModel> profileModelList =
            JsonHandler.getFromFileAsTypeRefence(fileMetadataProfile, new TypeReference<List<ProfileModel>>() {});
        final RequestResponse response = profileService.createProfiles(profileModelList);

        assertThat(response.isOk());
        final RequestResponseOK<ProfileModel> responseCast = (RequestResponseOK<ProfileModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);

        final ProfileModel profileModel = responseCast.getResults().get(1);
        final InputStream xsdProfile = new FileInputStream(PropertiesUtils.getResourceFile("profile_ok.xsd"));


        doAnswer(invocation -> xsdProfile).when(workspaceClient).putObject(anyString(), anyString(),
            any(InputStream.class));
        given(workspaceClientFactory.getClient()).willReturn(workspaceClient);


        RequestResponse requestResponse = profileService.importProfileFile(profileModel.getIdentifier(), xsdProfile);
        assertThat(requestResponse.isOk()).isFalse();

    }

    @Test
    @RunWithCustomExecutor
    public void givenTestDownloadProfileFile() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileMetadataProfile = PropertiesUtils.getResourceFile("profile_ok.json");
        final List<ProfileModel> profileModelList =
            JsonHandler.getFromFileAsTypeRefence(fileMetadataProfile, new TypeReference<List<ProfileModel>>() {});
        final RequestResponse response = profileService.createProfiles(profileModelList);

        assertThat(response.isOk());
        final RequestResponseOK<ProfileModel> responseCast = (RequestResponseOK<ProfileModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);

        final ProfileModel profileModel = responseCast.getResults().iterator().next();
        final InputStream xsdProfile = new FileInputStream(PropertiesUtils.getResourceFile("profile_ok.xsd"));


        doAnswer(invocation -> xsdProfile).when(workspaceClient).putObject(anyString(), anyString(),
            any(InputStream.class));
        given(workspaceClientFactory.getClient()).willReturn(workspaceClient);
        RequestResponse requestResponse = profileService.importProfileFile(profileModel.getIdentifier(), xsdProfile);
        assertThat(requestResponse.isOk()).isTrue();

        final AsyncResponseJunitTest responseAsync = new AsyncResponseJunitTest();
        profileService.downloadProfileFile(profileModel.getIdentifier(), responseAsync);

    }

    @Test
    @RunWithCustomExecutor
    public void givenWellFormedProfileMetadataThenImportSuccessfully() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileMetadataProfile = PropertiesUtils.getResourceFile("profile_ok.json");
        final List<ProfileModel> profileModelList =
            JsonHandler.getFromFileAsTypeRefence(fileMetadataProfile, new TypeReference<List<ProfileModel>>() {});
        final RequestResponse response = profileService.createProfiles(profileModelList);

        assertThat(response.isOk());
        final RequestResponseOK<ProfileModel> responseCast = (RequestResponseOK<ProfileModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);
    }

    @Test
    @RunWithCustomExecutor
    public void givenATestMissingIdentifierReturnBadRequest() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileMetadataProfile = PropertiesUtils.getResourceFile("profile_missing_identifier.json");
        final List<ProfileModel> profileModelList =
            JsonHandler.getFromFileAsTypeRefence(fileMetadataProfile, new TypeReference<List<ProfileModel>>() {});
        final RequestResponse response = profileService.createProfiles(profileModelList);

        assertThat(!response.isOk());

    }

    @Test
    @RunWithCustomExecutor
    public void givenTestIdentifiers() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileMetadataProfile = PropertiesUtils.getResourceFile("profile_duplicate_identifier.json");
        final List<ProfileModel> profileModelList =
            JsonHandler.getFromFileAsTypeRefence(fileMetadataProfile, new TypeReference<List<ProfileModel>>() {});
        final RequestResponse response = profileService.createProfiles(profileModelList);

        assertThat(!response.isOk());
    }

    @Test
    @RunWithCustomExecutor
    public void givenTestDuplicateNames() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileMetadataProfile = PropertiesUtils.getResourceFile("profile_duplicate_name.json");
        final List<ProfileModel> profileModelList =
            JsonHandler.getFromFileAsTypeRefence(fileMetadataProfile, new TypeReference<List<ProfileModel>>() {});
        final RequestResponse response = profileService.createProfiles(profileModelList);

        assertThat(!response.isOk());
    }

    @Test
    @RunWithCustomExecutor
    public void givenTestNotAllowedNotNullIdInCreation() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileMetadataProfile = PropertiesUtils.getResourceFile("profile_ok.json");

        final List<ProfileModel> profileModelList =
            JsonHandler.getFromFileAsTypeRefence(fileMetadataProfile, new TypeReference<List<ProfileModel>>() {});
        RequestResponse response = profileService.createProfiles(profileModelList);

        final RequestResponseOK<ProfileModel> responseCast = (RequestResponseOK<ProfileModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);

        // Try to recreate the same profile but with id
        response = profileService.createProfiles(responseCast.getResults());

        assertThat(!response.isOk());
    }


    @Test
    @RunWithCustomExecutor
    public void givenTestAlreadyExistsProfile() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileMetadataProfile = PropertiesUtils.getResourceFile("profile_ok.json");

        List<ProfileModel> profileModelList =
            JsonHandler.getFromFileAsTypeRefence(fileMetadataProfile, new TypeReference<List<ProfileModel>>() {});
        RequestResponse response = profileService.createProfiles(profileModelList);

        final RequestResponseOK<ProfileModel> responseCast = (RequestResponseOK<ProfileModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);


        // unset ids
        profileModelList =
            JsonHandler.getFromFileAsTypeRefence(fileMetadataProfile, new TypeReference<List<ProfileModel>>() {});
        response = profileService.createProfiles(profileModelList);

        assertThat(!response.isOk());
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
        final List<ProfileModel> profileModelList =
            JsonHandler.getFromFileAsTypeRefence(fileMetadataProfile, new TypeReference<List<ProfileModel>>() {});
        final RequestResponse response = profileService.createProfiles(profileModelList);

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
        assertThat(one.getTenant()).isEqualTo(Integer.valueOf(TENANT_ID));
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
        final List<ProfileModel> profileModelList =
            JsonHandler.getFromFileAsTypeRefence(fileMetadataProfile, new TypeReference<List<ProfileModel>>() {});
        final RequestResponse response = profileService.createProfiles(profileModelList);

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

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileMetadataProfile = PropertiesUtils.getResourceFile("profile_ok.json");
        final List<ProfileModel> profileModelList =
            JsonHandler.getFromFileAsTypeRefence(fileMetadataProfile, new TypeReference<List<ProfileModel>>() {});
        final RequestResponse response = profileService.createProfiles(profileModelList);

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
        final RequestResponseOK<ProfileModel> profileModelList =
            profileService.findProfiles(JsonHandler.createObjectNode());
        assertThat(profileModelList.getResults()).isEmpty();
    }

    @Test
    @RunWithCustomExecutor
    public void givenTestFindAllThenReturnTwoProfiles() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileMetadataProfile = PropertiesUtils.getResourceFile("profile_ok.json");
        final List<ProfileModel> profileModelList =
            JsonHandler.getFromFileAsTypeRefence(fileMetadataProfile, new TypeReference<List<ProfileModel>>() {});
        final RequestResponse response = profileService.createProfiles(profileModelList);

        final RequestResponseOK<ProfileModel> responseCast = (RequestResponseOK<ProfileModel>) response;
        assertThat(responseCast.getResults()).hasSize(2);

        final RequestResponseOK<ProfileModel> profileModelListSearch =
            profileService.findProfiles(JsonHandler.createObjectNode());
        assertThat(profileModelListSearch.getResults()).hasSize(2);
    }

    @Test
    @RunWithCustomExecutor
    public void givenTestFindByIdentifier() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        final File fileMetadataProfile = PropertiesUtils.getResourceFile("profile_ok.json");
        final List<ProfileModel> profileModelList =
            JsonHandler.getFromFileAsTypeRefence(fileMetadataProfile, new TypeReference<List<ProfileModel>>() {});
        final RequestResponse response = profileService.createProfiles(profileModelList);

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

        assertThat(acmFound.getId()).isEqualTo(id1);
        assertThat(acmFound.getIdentifier()).isEqualTo(identifier);
    }

}
