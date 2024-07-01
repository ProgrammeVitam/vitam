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
import fr.gouv.vitam.common.client.VitamClientFactory;
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
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollectionsTestUtils;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminFactory;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessReferential;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.ws.rs.core.Response.Status;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static fr.gouv.vitam.common.guid.GUIDFactory.newOperationLogbookGUID;
import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;

/**
 * As context Resource call ContextService, the full tests are done in @see AccessContextTest
 */
public class ContextResourceTest {

    private static final String PREFIX = GUIDFactory.newGUID().getId();

    @ClassRule
    public static MongoRule mongoRule = new MongoRule(MongoDbAccess.getMongoClientSettingsBuilder());

    @ClassRule
    public static ElasticsearchRule elasticsearchRule = new ElasticsearchRule();

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ContextResourceTest.class);
    private static final String ADMIN_MANAGEMENT_CONF = "functional-administration-test.conf";

    private static final String RESOURCE_URI = "/adminmanagement/v1";
    private static final String STATUS_URI = "/status";

    private static final int TENANT_ID = 0;
    static MongoDbAccessReferential mongoDbAccess;
    private static String DATABASE_HOST = "localhost";

    private static JunitHelper junitHelper = JunitHelper.getInstance();
    private static int serverPort;
    private static File adminConfigFile;
    private static AdminManagementMain application;

    private static final ElasticsearchFunctionalAdminIndexManager indexManager =
        FunctionalAdminCollectionsTestUtils.createTestIndexManager();

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    private static int workspacePort = junitHelper.findAvailablePort();

    @ClassRule
    public static WireMockClassRule workspaceWireMock = new WireMockClassRule(workspacePort);

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

        File tmpFolder = tempFolder.newFolder();
        System.setProperty("vitam.tmp.folder", tmpFolder.getAbsolutePath());
        SystemPropertyUtil.refresh();

        LogbookOperationsClientFactory.changeMode(null);

        final File adminConfig = PropertiesUtils.findFile(ADMIN_MANAGEMENT_CONF);
        final AdminManagementConfiguration realAdminConfig = PropertiesUtils.readYaml(
            adminConfig,
            AdminManagementConfiguration.class
        );
        realAdminConfig.getMongoDbNodes().get(0).setDbPort(mongoRule.getDataBasePort());
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

        try {
            application = new AdminManagementMain(adminConfigFile.getAbsolutePath());
            application.start();
            JunitHelper.unsetJettyPortSystemProperty();
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
            throw new IllegalStateException("Cannot start the AdminManagement Application Server", e);
        }

        // Mock workspace API
        workspaceWireMock.stubFor(
            WireMock.post(urlMatching("/workspace/v1/containers/(.*)")).willReturn(
                aResponse().withStatus(201).withHeader(GlobalDataRest.X_TENANT_ID, Integer.toString(TENANT_ID))
            )
        );
        workspaceWireMock.stubFor(
            WireMock.delete(urlMatching("/workspace/v1/containers/(.*)")).willReturn(
                aResponse().withStatus(204).withHeader(GlobalDataRest.X_TENANT_ID, Integer.toString(TENANT_ID))
            )
        );
        // Create security profile
    }

    private static void createSecurityProfile() throws Exception {
        // Create initial security context
        File securityProfileFile = PropertiesUtils.getResourceFile("security_profile_ok.json");
        JsonNode secProfileJson = JsonHandler.getFromFile(securityProfileFile);
        given()
            .contentType(ContentType.JSON)
            .body(secProfileJson)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
            .when()
            .post(SecurityProfileResource.SECURITY_PROFILE_URI)
            .then()
            .statusCode(Status.CREATED.getStatusCode());
    }

    @Before
    public void setUp() {
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(TENANT_ID));
    }

    @AfterClass
    public static void tearDownAfterClass() {
        LOGGER.debug("Ending tests");
        try {
            application.stop();
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
        }

        junitHelper.releasePort(serverPort);
        FunctionalAdminCollectionsTestUtils.afterTestClass(true);
        VitamClientFactory.resetConnections();
    }

    @After
    public void tearDown() {
        FunctionalAdminCollectionsTestUtils.afterTest();
    }

    @Test
    @RunWithCustomExecutor
    public final void testGetStatus() {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        get(STATUS_URI).then().statusCode(Status.NO_CONTENT.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void givenAWellFormedContextJsonThenReturnCeated() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        createSecurityProfile();

        File fileContexts = PropertiesUtils.getResourceFile("contexts_ok.json");
        JsonNode json = JsonHandler.getFromFile(fileContexts);

        MetaDataClientFactory.changeMode(null);

        // transform to json
        given()
            .contentType(ContentType.JSON)
            .body(json)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
            .when()
            .post(ContextResource.CONTEXTS_URI)
            .then()
            .statusCode(Status.CREATED.getStatusCode());

        // we try to update an unexisting id
        given()
            .contentType(ContentType.JSON)
            .body(JsonHandler.createArrayNode())
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
            .when()
            .put(ContextResource.UPDATE_CONTEXT_URI + "/wrongId")
            .then()
            .statusCode(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void givenAContextJsonWithInvalidSecurityProfileThenReturnBadRequest() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        File fileContexts = PropertiesUtils.getResourceFile("contexts_ko_invalid_security_profile.json");
        JsonNode json = JsonHandler.getFromFile(fileContexts);

        MetaDataClientFactory.changeMode(null);

        // transform to json
        given()
            .contentType(ContentType.JSON)
            .body(json)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
            .when()
            .post(ContextResource.CONTEXTS_URI)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
    }
}
