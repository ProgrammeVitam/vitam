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
package fr.gouv.vitam.functional.administration.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.google.common.collect.Lists;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.database.builder.query.BooleanQuery;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.action.SetAction;
import fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.builder.request.single.Update;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.server.application.configuration.DbConfigurationImpl;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.config.AdminManagementConfiguration;
import fr.gouv.vitam.functional.administration.common.config.ElasticsearchFunctionalAdminIndexManager;
import fr.gouv.vitam.functional.administration.common.server.ElasticsearchAccessFunctionalAdmin;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollectionsTestUtils;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminFactory;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessReferential;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.ws.rs.core.Response.Status;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.and;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.match;
import static fr.gouv.vitam.common.guid.GUIDFactory.newOperationLogbookGUID;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * As contract Resource call ProfileService, the full tests are done in @see AccessProfileTest
 */
public class ProfileResourceTest {

    private static final String PREFIX = GUIDFactory.newGUID().getId();

    @ClassRule
    public static MongoRule mongoRule = new MongoRule(MongoDbAccess.getMongoClientSettingsBuilder());

    @ClassRule
    public static ElasticsearchRule elasticsearchRule = new ElasticsearchRule();

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProfileResourceTest.class);
    private static final String ADMIN_MANAGEMENT_CONF = "functional-administration-test.conf";

    private static final String RESOURCE_URI = "/adminmanagement/v1";

    private static final int TENANT_ID = 0;

    static MongoDbAccessReferential mongoDbAccess;
    private static String DATABASE_HOST = "localhost";
    private static WorkspaceClient workspaceClient;
    private static WorkspaceClientFactory workspaceClientFactory;

    private static JunitHelper junitHelper = JunitHelper.getInstance();

    private static int workspacePort = junitHelper.findAvailablePort();

    private static final ElasticsearchFunctionalAdminIndexManager indexManager =
        FunctionalAdminCollectionsTestUtils.createTestIndexManager();

    @ClassRule
    public static WireMockClassRule wireMockRule = new WireMockClassRule(workspacePort);

    @Rule
    public WireMockClassRule instanceRule = wireMockRule;

    private static int serverPort;
    private static File adminConfigFile;
    private static AdminManagementMain application;

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        List<ElasticsearchNode> esNodes = Lists.newArrayList(
            new ElasticsearchNode(ElasticsearchRule.getHost(), ElasticsearchRule.getPort())
        );

        FunctionalAdminCollectionsTestUtils.beforeTestClass(
            mongoRule.getMongoDatabase(),
            PREFIX,
            new ElasticsearchAccessFunctionalAdmin(ElasticsearchRule.VITAM_CLUSTER, esNodes, indexManager)
        );

        LogbookOperationsClientFactory.changeMode(null);
        System.setProperty(VitamConfiguration.getVitamTmpProperty(), tempFolder.newFolder().getAbsolutePath());
        SystemPropertyUtil.refresh();

        final File adminConfig = PropertiesUtils.findFile(ADMIN_MANAGEMENT_CONF);
        final AdminManagementConfiguration realAdminConfig = PropertiesUtils.readYaml(
            adminConfig,
            AdminManagementConfiguration.class
        );
        realAdminConfig.getMongoDbNodes().get(0).setDbPort(mongoRule.getDataBasePort());
        realAdminConfig.setDbName(MongoRule.VITAM_DB);
        realAdminConfig.setElasticsearchNodes(esNodes);
        realAdminConfig.setClusterName(ElasticsearchRule.VITAM_CLUSTER);
        realAdminConfig.setWorkspaceUrl("http://localhost:" + workspacePort);

        adminConfigFile = File.createTempFile("test", ADMIN_MANAGEMENT_CONF, adminConfig.getParentFile());
        PropertiesUtils.writeYaml(adminConfigFile, realAdminConfig);

        final List<MongoDbNode> nodes = new ArrayList<>();
        nodes.add(new MongoDbNode(DATABASE_HOST, mongoRule.getDataBasePort()));
        mongoDbAccess = MongoDbAccessAdminFactory.create(
            new DbConfigurationImpl(nodes, mongoRule.getMongoDatabase().getName()),
            Collections::emptyList,
            indexManager
        );

        serverPort = junitHelper.findAvailablePort();

        RestAssured.port = serverPort;
        RestAssured.basePath = RESOURCE_URI;

        workspaceClientFactory = mock(WorkspaceClientFactory.class);
        workspaceClient = mock(WorkspaceClient.class);

        try {
            application = new AdminManagementMain(adminConfigFile.getAbsolutePath());
            application.start();
            JunitHelper.unsetJettyPortSystemProperty();
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
            throw new IllegalStateException("Cannot start the AdminManagement Application Server", e);
        }
        VitamConfiguration.setTenants(Arrays.asList(TENANT_ID));
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        LOGGER.debug("Ending tests");
        System.setProperty(VitamConfiguration.getVitamTmpProperty(), VitamConfiguration.getVitamTmpFolder());
        SystemPropertyUtil.refresh();
        try {
            application.stop();
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
        }
        junitHelper.releasePort(serverPort);
        FunctionalAdminCollectionsTestUtils.afterTestClass(true);
        VitamClientFactory.resetConnections();
    }

    @Before
    public void setUp() {
        instanceRule.stubFor(
            WireMock.post(urlMatching("/workspace/v1/containers/(.*)")).willReturn(
                aResponse().withStatus(201).withHeader(GlobalDataRest.X_TENANT_ID, Integer.toString(TENANT_ID))
            )
        );
        instanceRule.stubFor(
            WireMock.delete(urlMatching("/workspace/v1/containers/(.*)")).willReturn(
                aResponse().withStatus(204).withHeader(GlobalDataRest.X_TENANT_ID, Integer.toString(TENANT_ID))
            )
        );
    }

    @Test
    @RunWithCustomExecutor
    public void givenAWellFormedProfileJsonThenReturnCeated() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(TENANT_ID));

        mongoDbAccess.deleteCollectionForTesting(FunctionalAdminCollections.PROFILE).close();
        File fileProfiles = PropertiesUtils.getResourceFile("profile_ok.json");
        JsonNode json = JsonHandler.getFromFile(fileProfiles);
        // transform to json
        given()
            .contentType(ContentType.JSON)
            .body(json)
            .header(GlobalDataRest.X_TENANT_ID, 0)
            .header(GlobalDataRest.X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
            .when()
            .post(ProfileResource.PROFILE_URI)
            .then()
            .statusCode(Status.CREATED.getStatusCode());

        final Select select = new Select();
        final BooleanQuery query = and();
        query.add(match("Identifier", "PR-0000"));
        select.setQuery(query);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        List<String> result = given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(GlobalDataRest.X_TENANT_ID, 0)
            .header(GlobalDataRest.X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
            .when()
            .get(ProfileResource.PROFILE_URI)
            .then()
            .statusCode(Status.OK.getStatusCode())
            .extract()
            .body()
            .jsonPath()
            .get("$results.Identifier");

        assertThat(result).hasSize(0);
    }

    @Test
    @RunWithCustomExecutor
    public void givenProfileJsonWithAmissingIdentifierReturnBadRequest() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(TENANT_ID));
        mongoDbAccess.deleteCollectionForTesting(FunctionalAdminCollections.PROFILE).close();
        File fileProfiles = PropertiesUtils.getResourceFile("profile_missing_identifier.json");
        JsonNode json = JsonHandler.getFromFile(fileProfiles);
        // transform to json

        given()
            .contentType(ContentType.JSON)
            .body(json)
            .header(GlobalDataRest.X_TENANT_ID, VitamThreadUtils.getVitamSession().getTenantId())
            .header(GlobalDataRest.X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
            .when()
            .post(ProfileResource.PROFILE_URI)
            .then()
            .statusCode(Status.CREATED.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void givenProfilesWithDuplicateName() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(TENANT_ID));

        mongoDbAccess.deleteCollectionForTesting(FunctionalAdminCollections.PROFILE).close();
        File fileProfiles = PropertiesUtils.getResourceFile("profile_duplicate_name.json");
        JsonNode json = JsonHandler.getFromFile(fileProfiles);
        // transform to json
        given()
            .contentType(ContentType.JSON)
            .body(json)
            .header(GlobalDataRest.X_TENANT_ID, 0)
            .header(GlobalDataRest.X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
            .when()
            .post(ProfileResource.PROFILE_URI)
            .then()
            .statusCode(Status.CREATED.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void givenTestImportXSDProfileFile() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(TENANT_ID));
        mongoDbAccess.deleteCollectionForTesting(FunctionalAdminCollections.PROFILE).close();
        File fileProfiles = PropertiesUtils.getResourceFile("profile_ok.json");
        JsonNode json = JsonHandler.getFromFile(fileProfiles);
        // transform to json
        given()
            .contentType(ContentType.JSON)
            .body(json)
            .header(GlobalDataRest.X_TENANT_ID, 0)
            .header(GlobalDataRest.X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
            .when()
            .post(ProfileResource.PROFILE_URI)
            .then()
            .statusCode(Status.CREATED.getStatusCode());

        Select select = new Select().addOrderByAscFilter("Identifier");
        JsonPath result = given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(GlobalDataRest.X_TENANT_ID, 0)
            .header(GlobalDataRest.X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
            .when()
            .get(ProfileResource.PROFILE_URI)
            .then()
            .statusCode(Status.OK.getStatusCode())
            .extract()
            .body()
            .jsonPath();

        List<String> identifiers = result.get("$results.Identifier");
        assertThat(identifiers).hasSize(2);
        assertThat(identifiers.get(0)).contains("PR-0000");
        assertThat(identifiers.get(1)).contains("PR-0000");

        String identifierProfile = identifiers.iterator().next();

        InputStream xsdProfile = new FileInputStream(PropertiesUtils.getResourceFile("profile_ok.xsd"));

        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);

        doAnswer(invocation -> null).when(workspaceClient).createContainer(anyString());
        doAnswer(invocation -> xsdProfile)
            .when(workspaceClient)
            .putObject(anyString(), anyString(), any(InputStream.class));

        // transform to json
        given()
            .contentType(ContentType.BINARY)
            .body(xsdProfile)
            .header(GlobalDataRest.X_TENANT_ID, 0)
            .header(GlobalDataRest.X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
            .when()
            .put(ProfileResource.PROFILE_URI + "/" + identifierProfile)
            .then()
            .statusCode(Status.CREATED.getStatusCode());

        // we update an existing profile -> OK
        File updateProfile = PropertiesUtils.getResourceFile("updateProfile.json");
        JsonNode updateSecurityProfileJson = JsonHandler.getFromFile(updateProfile);
        given()
            .contentType(ContentType.JSON)
            .body(updateSecurityProfileJson)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
            .when()
            .put(ProfileResource.PROFILE_URI + "/" + identifierProfile)
            .then()
            .statusCode(Status.OK.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(updateSecurityProfileJson)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
            .when()
            .put(ProfileResource.PROFILE_URI + "/wrongId")
            .then()
            .statusCode(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void givenTestImportRNGProfileFile() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(TENANT_ID));
        mongoDbAccess.deleteCollectionForTesting(FunctionalAdminCollections.PROFILE).close();
        File fileProfiles = PropertiesUtils.getResourceFile("profile_ok.json");
        JsonNode json = JsonHandler.getFromFile(fileProfiles);
        // transform to json
        given()
            .contentType(ContentType.JSON)
            .body(json)
            .header(GlobalDataRest.X_TENANT_ID, 0)
            .header(GlobalDataRest.X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
            .when()
            .post(ProfileResource.PROFILE_URI)
            .then()
            .statusCode(Status.CREATED.getStatusCode());
        Select select = new Select().addOrderByAscFilter("Identifier");

        JsonPath result = given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(GlobalDataRest.X_TENANT_ID, 0)
            .when()
            .get(ProfileResource.PROFILE_URI)
            .then()
            .statusCode(Status.OK.getStatusCode())
            .extract()
            .body()
            .jsonPath();

        List<String> identifiers = result.get("$results.Identifier");
        assertThat(identifiers).hasSize(2);
        assertThat(identifiers.get(0)).contains("PR-0000");
        assertThat(identifiers.get(1)).contains("PR-0000");

        String identifierProfile0 = identifiers.get(0);
        String identifierProfile1 = identifiers.get(1);

        InputStream xsdProfile = new FileInputStream(PropertiesUtils.getResourceFile("profile_ok.rng"));

        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);

        doAnswer(invocation -> null).when(workspaceClient).createContainer(anyString());
        doAnswer(invocation -> xsdProfile)
            .when(workspaceClient)
            .putObject(anyString(), anyString(), any(InputStream.class));

        // transform to json
        given()
            .contentType(ContentType.BINARY)
            .body(xsdProfile)
            .header(GlobalDataRest.X_TENANT_ID, 0)
            .when()
            .put(ProfileResource.PROFILE_URI + "/" + identifierProfile1)
            .then()
            .statusCode(Status.CREATED.getStatusCode());

        // update identifier with existing one should fail
        Update update = new Update();
        SetAction setDescription = UpdateActionHelper.set("Identifier", identifierProfile0);
        update.addActions(setDescription);
        update.setQuery(QueryHelper.eq("Identifier", identifierProfile1));

        given()
            .contentType(ContentType.JSON)
            .body(update.getFinalUpdate())
            .header(GlobalDataRest.X_TENANT_ID, 0)
            .when()
            .put(ProfileResource.UPDATE_PROFIL_URI + "/" + identifierProfile1)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());

        // update profile
        update = new Update();
        setDescription = UpdateActionHelper.set("Description", "New Description of the profile");
        update.addActions(setDescription);
        update.setQuery(QueryHelper.eq("Name", "aName"));

        given()
            .contentType(ContentType.JSON)
            .body(update.getFinalUpdate())
            .header(GlobalDataRest.X_TENANT_ID, 0)
            .header(GlobalDataRest.X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
            .when()
            .put(ProfileResource.UPDATE_PROFIL_URI + "/" + identifierProfile1)
            .then()
            .statusCode(Status.OK.getStatusCode());
    }
}
