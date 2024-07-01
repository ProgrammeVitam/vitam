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
package fr.gouv.vitam.ihmrecette.appserver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.mongodb.BasicDBObject;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.server.DbRequestResult;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.DocumentAlreadyExistsException;
import fr.gouv.vitam.common.exception.InvalidGuidOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.SchemaValidationException;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.guid.GUIDReader;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.administration.ContextModel;
import fr.gouv.vitam.common.model.config.CollectionConfiguration;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.server.application.configuration.DbConfigurationImpl;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.common.xsrf.filter.XSRFFilter;
import fr.gouv.vitam.common.xsrf.filter.XSRFHelper;
import fr.gouv.vitam.functional.administration.common.Context;
import fr.gouv.vitam.functional.administration.common.config.ElasticsearchFunctionalAdminIndexManager;
import fr.gouv.vitam.functional.administration.common.config.FunctionalAdminIndexationConfiguration;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.server.ElasticsearchAccessFunctionalAdmin;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollectionsTestUtils;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminFactory;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.ihmrecette.appserver.utils.MappingLoaderTestUtils;
import fr.gouv.vitam.logbook.common.server.config.ElasticsearchLogbookIndexManager;
import fr.gouv.vitam.logbook.common.server.config.LogbookIndexationConfiguration;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollectionsTestUtils;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookElasticsearchAccess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycleObjectGroup;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycleUnit;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookOperation;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.core.config.DefaultCollectionConfiguration;
import fr.gouv.vitam.metadata.core.config.ElasticsearchMetadataIndexManager;
import fr.gouv.vitam.metadata.core.config.MetadataIndexationConfiguration;
import fr.gouv.vitam.metadata.core.database.collections.ElasticsearchAccessMetadata;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollectionsTestUtils;
import fr.gouv.vitam.metadata.core.database.collections.MetadataDocument;
import fr.gouv.vitam.metadata.core.database.collections.ObjectGroup;
import fr.gouv.vitam.metadata.core.database.collections.Unit;
import fr.gouv.vitam.metadata.core.mapping.MappingLoader;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Cookie;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.core.Response.Status;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class WebApplicationResourceDeleteTest {

    private static final String PREFIX = GUIDFactory.newGUID().getId();

    @ClassRule
    public static MongoRule mongoRule = new MongoRule(MongoDbAccess.getMongoClientSettingsBuilder());

    @ClassRule
    public static ElasticsearchRule elasticsearchRule = new ElasticsearchRule();

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(WebApplicationResourceDeleteTest.class);

    private static final String CONTEXT_NAME = "Name";
    private static final String ADMIN_CONTEXT = "admin-context";
    private static final String SECURITY_PROFIL_NAME = "Name";
    private static final String SECURITY_PROFIL_NAME_TO_SAVE = "admin-security-profile";
    // Take it from conf file
    private static final String DEFAULT_WEB_APP_CONTEXT = "/ihm-recette";
    private static final String CREDENTIALS = "{\"token\": {\"principal\": \"myName\", \"credentials\": \"myName\"}}";
    private static final String CREDENTIALS_NO_VALID =
        "{\"token\": {\"principal\": \"myName\", \"credentials\": \"myName\"}}";
    private static final String IHM_RECETTE_CONF = "ihm-recette.conf";
    private static JunitHelper junitHelper;
    private static int serverPort;
    private static File adminConfigFile;

    private static MongoDbAccessAdminImpl mongoDbAccessAdmin;

    private static IhmRecetteMain application;

    private static final Integer TENANT_ID = 0;
    private static final Integer ADMIN_TENANT_ID = 1;
    static final List<Integer> tenantList = Arrays.asList(0, ADMIN_TENANT_ID);

    static final String tokenCSRF = XSRFHelper.generateCSRFToken();
    private static final Cookie COOKIE = new Cookie.Builder("JSESSIONID", "testId").build();

    private static final MappingLoader mappingLoader = MappingLoaderTestUtils.getTestMappingLoader();
    private static final ElasticsearchMetadataIndexManager metadataIndexManager =
        MetadataCollectionsTestUtils.createTestIndexManager(tenantList, Collections.emptyMap(), mappingLoader);
    private static final ElasticsearchLogbookIndexManager logbookIndexManager =
        LogbookCollectionsTestUtils.createTestIndexManager(tenantList, Collections.emptyMap());
    private static final ElasticsearchFunctionalAdminIndexManager functionalAdminIndexManager =
        FunctionalAdminCollectionsTestUtils.createTestIndexManager();

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        List<ElasticsearchNode> nodes = Lists.newArrayList(
            new ElasticsearchNode(ElasticsearchRule.getHost(), ElasticsearchRule.getPort())
        );
        FunctionalAdminCollectionsTestUtils.beforeTestClass(
            mongoRule.getMongoDatabase(),
            PREFIX,
            new ElasticsearchAccessFunctionalAdmin(ElasticsearchRule.VITAM_CLUSTER, nodes, functionalAdminIndexManager)
        );

        MetadataCollectionsTestUtils.beforeTestClass(
            mongoRule.getMongoDatabase(),
            PREFIX,
            new ElasticsearchAccessMetadata(ElasticsearchRule.VITAM_CLUSTER, nodes, metadataIndexManager)
        );

        LogbookCollectionsTestUtils.beforeTestClass(
            mongoRule.getMongoDatabase(),
            PREFIX,
            new LogbookElasticsearchAccess(ElasticsearchRule.VITAM_CLUSTER, nodes, logbookIndexManager)
        );

        junitHelper = JunitHelper.getInstance();
        serverPort = junitHelper.findAvailablePort();

        final File adminConfig = PropertiesUtils.findFile("ihm-recette.conf");
        final WebApplicationConfig realAdminConfig = PropertiesUtils.readYaml(adminConfig, WebApplicationConfig.class);
        realAdminConfig.getMongoDbNodes().get(0).setDbPort(MongoRule.getDataBasePort());
        realAdminConfig.setBaseUrl(DEFAULT_WEB_APP_CONTEXT);
        realAdminConfig.setAuthentication(false);
        realAdminConfig.setEnableSession(true);
        realAdminConfig.setEnableXsrFilter(true);
        realAdminConfig.setClusterName(ElasticsearchRule.VITAM_CLUSTER);
        realAdminConfig.setFunctionalAdminIndexationConfiguration(
            new FunctionalAdminIndexationConfiguration().setDefaultConfiguration(new CollectionConfiguration(1, 0))
        );
        realAdminConfig.setMetadataIndexationConfiguration(
            new MetadataIndexationConfiguration()
                .setDefaultCollectionConfiguration(
                    new DefaultCollectionConfiguration()
                        .setUnit(new CollectionConfiguration(1, 0))
                        .setObjectgroup(new CollectionConfiguration(1, 0))
                )
        );
        realAdminConfig.setLogbookIndexationConfiguration(
            new LogbookIndexationConfiguration()
                .setDefaultCollectionConfiguration(
                    new fr.gouv.vitam.logbook.common.server.config.DefaultCollectionConfiguration()
                        .setLogbookoperation(new CollectionConfiguration(1, 0))
                )
        );
        VitamConfiguration.setTenants(tenantList);

        realAdminConfig.getElasticsearchNodes().get(0).setHttpPort(ElasticsearchRule.PORT);
        realAdminConfig.setElasticsearchExternalMetadataMappings(mappingLoader.getElasticsearchExternalMappings());
        adminConfigFile = File.createTempFile("test", IHM_RECETTE_CONF, adminConfig.getParentFile());
        PropertiesUtils.writeYaml(adminConfigFile, realAdminConfig);

        RestAssured.port = serverPort;
        RestAssured.basePath = DEFAULT_WEB_APP_CONTEXT + "/v1/api";

        final DbConfigurationImpl adminConfiguration = new DbConfigurationImpl(
            realAdminConfig.getMongoDbNodes(),
            realAdminConfig.getMasterdataDbName(),
            false,
            realAdminConfig.getDbUserName(),
            realAdminConfig.getDbPassword()
        );
        mongoDbAccessAdmin = MongoDbAccessAdminFactory.create(
            adminConfiguration,
            Collections::emptyList,
            functionalAdminIndexManager
        );

        try {
            application = new IhmRecetteMain(adminConfigFile.getAbsolutePath());
            application.start();
            JunitHelper.unsetJettyPortSystemProperty();
        } catch (final VitamApplicationServerException e) {
            throw new IllegalStateException("Cannot start the Logbook Application Server", e);
        }

        XSRFFilter.addToken("testId", tokenCSRF);

        FunctionalAdminCollectionsTestUtils.afterTestClass(
            Lists.newArrayList(FunctionalAdminCollections.ONTOLOGY),
            false
        );
    }

    @AfterClass
    public static void tearDownAfterClass() {
        LOGGER.debug("Ending tests");
        try {
            application.stop();
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
        }

        FunctionalAdminCollectionsTestUtils.afterTestClass(true);

        MetadataCollectionsTestUtils.afterTestClass(metadataIndexManager, true);

        LogbookCollectionsTestUtils.afterTestClass(logbookIndexManager, true);

        mongoDbAccessAdmin.close();
        junitHelper.releasePort(serverPort);
    }

    @Before
    public void before() {
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newGUID());
    }

    @Test
    @RunWithCustomExecutor
    public void givenNoSecureServerLoginUnauthorized() {
        given()
            .contentType(ContentType.JSON)
            .body(CREDENTIALS)
            .expect()
            .statusCode(Status.UNAUTHORIZED.getStatusCode())
            .when()
            .post("login");
        given()
            .contentType(ContentType.JSON)
            .body(CREDENTIALS_NO_VALID)
            .expect()
            .statusCode(Status.UNAUTHORIZED.getStatusCode())
            .when()
            .post("login");
    }

    @Test
    @RunWithCustomExecutor
    public void testSuccessStatus() {
        given()
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .header(GlobalDataRest.X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
            .cookie(COOKIE)
            .expect()
            .statusCode(Status.NO_CONTENT.getStatusCode())
            .when()
            .get("status");
    }

    @Test
    @RunWithCustomExecutor
    public void testDeleteFormatOK() {
        try {
            final GUID idFormat = addData(FunctionalAdminCollections.FORMATS);
            assertTrue(existsData(FunctionalAdminCollections.FORMATS, idFormat.getId()));
            given()
                .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
                .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
                .header(GlobalDataRest.X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
                .cookie(COOKIE)
                .expect()
                .statusCode(Status.OK.getStatusCode())
                .when()
                .delete("delete/formats");
            assertFalse(existsData(FunctionalAdminCollections.FORMATS, idFormat.getId()));
        } catch (final Exception e) {
            LOGGER.error(e);
            fail("Exception using mongoDbAccess");
        }
    }

    @Test
    @RunWithCustomExecutor
    public void testDeleteOntologiesOK() {
        try {
            final GUID idOntology = addData(FunctionalAdminCollections.ONTOLOGY);
            assertTrue(existsData(FunctionalAdminCollections.ONTOLOGY, idOntology.getId()));
            given()
                .header(GlobalDataRest.X_TENANT_ID, ADMIN_TENANT_ID)
                .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
                .header(GlobalDataRest.X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
                .cookie(COOKIE)
                .expect()
                .statusCode(Status.OK.getStatusCode())
                .when()
                .delete("delete/masterdata/ontologies");
            assertFalse(existsData(FunctionalAdminCollections.ONTOLOGY, idOntology.getId()));
        } catch (final Exception e) {
            LOGGER.error(e);
            fail("Exception using mongoDbAccess");
        }
    }

    @Test
    @RunWithCustomExecutor
    public void testDeleteRulesFileOK() {
        try {
            VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
            final GUID idRule = addData(FunctionalAdminCollections.RULES);
            assertTrue(existsData(FunctionalAdminCollections.RULES, idRule.getId()));
            given()
                .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
                .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
                .header(GlobalDataRest.X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
                .cookie(COOKIE)
                .expect()
                .statusCode(Status.OK.getStatusCode())
                .when()
                .delete("delete/rules");
            assertFalse(existsData(FunctionalAdminCollections.RULES, idRule.getId()));
        } catch (final Exception e) {
            fail("Exception using mongoDbAccess");
        }
    }

    @Test
    @RunWithCustomExecutor
    public void testAccessionRegisterOK() {
        try {
            VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
            final GUID idRegisterSummary = addData(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY);
            final GUID idRegisterDetail = addData(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL);
            assertTrue(existsData(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY, idRegisterSummary.getId()));
            assertTrue(existsData(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL, idRegisterDetail.getId()));
            given()
                .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
                .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
                .header(GlobalDataRest.X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
                .cookie(COOKIE)
                .expect()
                .statusCode(Status.OK.getStatusCode())
                .when()
                .delete("delete/accessionregisters");
            assertFalse(existsData(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY, idRegisterSummary.getId()));
            assertFalse(existsData(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL, idRegisterDetail.getId()));
        } catch (final Exception e) {
            fail("Exception using mongoDbAccess");
        }
    }

    @Test
    @RunWithCustomExecutor
    public void testDeleteLogbookOperationOK() {
        try {
            final GUID idOperation = addData(LogbookCollections.OPERATION);
            assertTrue(existsData(LogbookCollections.OPERATION, idOperation.getId()));
            given()
                .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
                .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
                .header(GlobalDataRest.X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
                .cookie(COOKIE)
                .expect()
                .statusCode(Status.OK.getStatusCode())
                .when()
                .delete("delete/logbook/operation");
            assertFalse(existsData(LogbookCollections.OPERATION, idOperation.getId()));
        } catch (final Exception e) {
            LOGGER.error(e);
            fail("Exception using mongoDbAccess");
        }
    }

    @Test
    @RunWithCustomExecutor
    public void testDeleteLogbookLifecycleOGOK() {
        try {
            final GUID idLfcOg = addData(LogbookCollections.LIFECYCLE_OBJECTGROUP);
            assertTrue(existsData(LogbookCollections.LIFECYCLE_OBJECTGROUP, idLfcOg.getId()));
            given()
                .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
                .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
                .header(GlobalDataRest.X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
                .cookie(COOKIE)
                .expect()
                .statusCode(Status.OK.getStatusCode())
                .when()
                .delete("delete/logbook/lifecycle/objectgroup");
            assertFalse(existsData(LogbookCollections.LIFECYCLE_OBJECTGROUP, idLfcOg.getId()));
        } catch (final Exception e) {
            LOGGER.error(e);
            fail("Exception using mongoDbAccess");
        }
    }

    @Test
    @RunWithCustomExecutor
    public void testDeleteLogbookLifecycleUnitOK() {
        try {
            final GUID idLfcUnit = addData(LogbookCollections.LIFECYCLE_UNIT);
            assertTrue(existsData(LogbookCollections.LIFECYCLE_UNIT, idLfcUnit.getId()));
            given()
                .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
                .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
                .header(GlobalDataRest.X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
                .cookie(COOKIE)
                .expect()
                .statusCode(Status.OK.getStatusCode())
                .when()
                .delete("delete/logbook/lifecycle/unit");
            assertFalse(existsData(LogbookCollections.LIFECYCLE_UNIT, idLfcUnit.getId()));
        } catch (final Exception e) {
            LOGGER.error(e);
            fail("Exception using mongoDbAccess");
        }
    }

    @Test
    @RunWithCustomExecutor
    public void testDeleteMetadataOGOK() {
        try {
            final GUID idOg = addData(MetadataCollections.OBJECTGROUP);
            assertTrue(existsData(MetadataCollections.OBJECTGROUP, idOg.getId()));
            given()
                .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
                .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
                .header(GlobalDataRest.X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
                .cookie(COOKIE)
                .expect()
                .statusCode(Status.OK.getStatusCode())
                .when()
                .delete("delete/metadata/objectgroup");
            assertFalse(existsData(MetadataCollections.OBJECTGROUP, idOg.getId()));
        } catch (final Exception e) {
            LOGGER.error(e);
            fail("Exception using mongoDbAccess");
        }
    }

    @Test
    @RunWithCustomExecutor
    public void testDeleteMetadataUnitOK() {
        try {
            final GUID idUnit = addData(MetadataCollections.UNIT);
            assertTrue(existsData(MetadataCollections.UNIT, idUnit.getId()));
            given()
                .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
                .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
                .header(GlobalDataRest.X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
                .cookie(COOKIE)
                .expect()
                .statusCode(Status.OK.getStatusCode())
                .when()
                .delete("delete/metadata/unit");
            assertFalse(existsData(MetadataCollections.UNIT, idUnit.getId()));
        } catch (final Exception e) {
            LOGGER.error(e);
            fail("Exception using mongoDbAccess");
        }
    }

    @Test
    @RunWithCustomExecutor
    public void testDeleteMasterdataAccessOK() {
        try {
            VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

            final GUID idUnit = addData(FunctionalAdminCollections.ACCESS_CONTRACT);
            assertTrue(existsData(FunctionalAdminCollections.ACCESS_CONTRACT, idUnit.getId()));
            given()
                .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
                .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
                .header(GlobalDataRest.X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
                .cookie(COOKIE)
                .expect()
                .statusCode(Status.OK.getStatusCode())
                .when()
                .delete("delete/masterdata/accessContract");
            assertFalse(existsData(FunctionalAdminCollections.ACCESS_CONTRACT, idUnit.getId()));
        } catch (final Exception e) {
            LOGGER.error(e);
            fail("Exception using mongoDbAccess");
        }
    }

    @Test
    @RunWithCustomExecutor
    public void testDeleteMasterdataIngestOK() {
        try {
            VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

            final GUID idUnit = addData(FunctionalAdminCollections.INGEST_CONTRACT);
            assertTrue(existsData(FunctionalAdminCollections.INGEST_CONTRACT, idUnit.getId()));
            given()
                .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
                .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
                .header(GlobalDataRest.X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
                .cookie(COOKIE)
                .expect()
                .statusCode(Status.OK.getStatusCode())
                .when()
                .delete("delete/masterdata/ingestContract");
            assertFalse(existsData(FunctionalAdminCollections.INGEST_CONTRACT, idUnit.getId()));
        } catch (final Exception e) {
            LOGGER.error(e);
            fail("Exception using mongoDbAccess");
        }
    }

    @Test
    @RunWithCustomExecutor
    public void testDeleteMasterdataManagementOK() {
        try {
            VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

            final GUID idUnit = addData(FunctionalAdminCollections.MANAGEMENT_CONTRACT);
            assertTrue(existsData(FunctionalAdminCollections.MANAGEMENT_CONTRACT, idUnit.getId()));
            given()
                .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
                .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
                .header(GlobalDataRest.X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
                .cookie(COOKIE)
                .expect()
                .statusCode(Status.OK.getStatusCode())
                .when()
                .delete("delete/masterdata/managementContract");
            assertFalse(existsData(FunctionalAdminCollections.MANAGEMENT_CONTRACT, idUnit.getId()));
        } catch (final Exception e) {
            LOGGER.error(e);
            fail("Exception using mongoDbAccess");
        }
    }

    @Test
    @RunWithCustomExecutor
    public void testDeleteTnrOk() throws Exception {
        try {
            VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
            // insert and check data
            final GUID idFormat = addData(FunctionalAdminCollections.FORMATS);
            assertTrue(existsData(FunctionalAdminCollections.FORMATS, idFormat.getId()));
            final GUID idRule = addData(FunctionalAdminCollections.RULES);
            assertTrue(existsData(FunctionalAdminCollections.RULES, idRule.getId()));
            final GUID idAgency = addData(FunctionalAdminCollections.AGENCIES);
            assertTrue(existsData(FunctionalAdminCollections.AGENCIES, idAgency.getId()));
            final GUID idProfile = addData(FunctionalAdminCollections.PROFILE);
            assertTrue(existsData(FunctionalAdminCollections.PROFILE, idProfile.getId()));
            final GUID idAccessContract = addData(FunctionalAdminCollections.ACCESS_CONTRACT);
            assertTrue(existsData(FunctionalAdminCollections.ACCESS_CONTRACT, idAccessContract.getId()));
            final GUID idIngestContract = addData(FunctionalAdminCollections.INGEST_CONTRACT);
            assertTrue(existsData(FunctionalAdminCollections.INGEST_CONTRACT, idIngestContract.getId()));
            final GUID idRegisterSummary = addData(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY);
            final GUID idRegisterDetail = addData(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL);
            assertTrue(existsData(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY, idRegisterSummary.getId()));
            assertTrue(existsData(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL, idRegisterDetail.getId()));
            // delete all
            given()
                .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
                .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
                .header(GlobalDataRest.X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
                .cookie(COOKIE)
                .expect()
                .statusCode(Status.OK.getStatusCode())
                .when()
                .delete("delete/deleteTnr");
            // check deleted
            assertFalse(existsData(FunctionalAdminCollections.FORMATS, idFormat.getId()));
            assertFalse(existsData(FunctionalAdminCollections.AGENCIES, idAgency.getId()));
            assertFalse(existsData(FunctionalAdminCollections.PROFILE, idProfile.getId()));
            assertFalse(existsData(FunctionalAdminCollections.ACCESS_CONTRACT, idAccessContract.getId()));
            assertFalse(existsData(FunctionalAdminCollections.INGEST_CONTRACT, idIngestContract.getId()));
            assertFalse(existsData(FunctionalAdminCollections.RULES, idRule.getId()));
            assertFalse(existsData(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY, idRegisterSummary.getId()));
            assertFalse(existsData(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL, idRegisterDetail.getId()));
        } catch (final ReferentialException e) {
            LOGGER.error(e);
            fail("Exception using mongoDbAccess");
        }
    }

    @Test
    @RunWithCustomExecutor
    public void testDeleteMasterdataProfileOK() {
        try {
            VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

            final GUID idProfile = addData(FunctionalAdminCollections.PROFILE);
            assertTrue(existsData(FunctionalAdminCollections.PROFILE, idProfile.getId()));
            given()
                .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
                .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
                .header(GlobalDataRest.X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
                .cookie(COOKIE)
                .expect()
                .statusCode(Status.OK.getStatusCode())
                .when()
                .delete("delete/masterdata/profile");
            assertFalse(existsData(FunctionalAdminCollections.PROFILE, idProfile.getId()));
        } catch (final Exception e) {
            LOGGER.error(e);
            fail("Exception using mongoDbAccess");
        }
    }

    @Test
    @RunWithCustomExecutor
    public void testDeleteMasterdataAgencyOK() {
        try {
            VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

            final GUID idAgency = addData(FunctionalAdminCollections.AGENCIES);
            assertTrue(existsData(FunctionalAdminCollections.AGENCIES, idAgency.getId()));
            given()
                .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
                .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
                .header(GlobalDataRest.X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
                .cookie(COOKIE)
                .expect()
                .statusCode(Status.OK.getStatusCode())
                .when()
                .delete("delete/masterdata/agencies");
            assertFalse(existsData(FunctionalAdminCollections.AGENCIES, idAgency.getId()));
        } catch (final Exception e) {
            LOGGER.error(e);
            fail("Exception using mongoDbAccess");
        }
    }

    @Test
    @RunWithCustomExecutor
    public void testDeleteMasterdataContextOK() {
        try {
            VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

            final GUID adminContext = addAdminContextData(FunctionalAdminCollections.CONTEXT);
            // Needs two contexts for testing purposes (admin context won't be deleted)
            final GUID idContext2 = addData(FunctionalAdminCollections.CONTEXT);
            assertTrue(existsData(FunctionalAdminCollections.CONTEXT, adminContext.getId()));
            assertTrue(existsData(FunctionalAdminCollections.CONTEXT, idContext2.getId()));
            given()
                .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
                .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
                .header(GlobalDataRest.X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
                .cookie(COOKIE)
                .expect()
                .statusCode(Status.OK.getStatusCode())
                .when()
                .delete("delete/masterdata/context");
            assertTrue(existsData(FunctionalAdminCollections.CONTEXT, adminContext.getId()));
            assertFalse(existsData(FunctionalAdminCollections.CONTEXT, idContext2.getId()));
        } catch (final Exception e) {
            LOGGER.error(e);
            fail("Exception using mongoDbAccess");
        }
    }

    @Test
    @RunWithCustomExecutor
    public void testDeleteMasterdataSecuryityProfilOK() {
        try {
            VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

            final GUID adminSecurity = addAdminSecurityData(FunctionalAdminCollections.SECURITY_PROFILE);
            // Needs two contexts for testing purposes (admin context won't be deleted)
            final GUID idSecurity = addData(FunctionalAdminCollections.SECURITY_PROFILE);
            assertTrue(existsData(FunctionalAdminCollections.SECURITY_PROFILE, adminSecurity.getId()));
            assertTrue(existsData(FunctionalAdminCollections.SECURITY_PROFILE, idSecurity.getId()));
            given()
                .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
                .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
                .header(GlobalDataRest.X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
                .cookie(COOKIE)
                .expect()
                .statusCode(Status.OK.getStatusCode())
                .when()
                .delete("delete/masterdata/securityProfil");
            assertTrue(existsData(FunctionalAdminCollections.SECURITY_PROFILE, adminSecurity.getId()));
            assertFalse(existsData(FunctionalAdminCollections.SECURITY_PROFILE, idSecurity.getId()));
        } catch (final Exception e) {
            LOGGER.error(e);
            fail("Exception using mongoDbAccess");
        }
    }

    @Test
    @RunWithCustomExecutor
    public void testDeleteAllOk()
        throws InvalidCreateOperationException, InvalidGuidOperationException, SchemaValidationException {
        try {
            VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
            // insert and check data
            final GUID idFormat = addData(FunctionalAdminCollections.FORMATS);
            assertTrue(existsData(FunctionalAdminCollections.FORMATS, idFormat.getId()));
            final GUID idRule = addData(FunctionalAdminCollections.RULES);
            assertTrue(existsData(FunctionalAdminCollections.RULES, idRule.getId()));
            final GUID idAgency = addData(FunctionalAdminCollections.AGENCIES);
            assertTrue(existsData(FunctionalAdminCollections.AGENCIES, idAgency.getId()));
            final GUID idProfile = addData(FunctionalAdminCollections.PROFILE);
            assertTrue(existsData(FunctionalAdminCollections.PROFILE, idProfile.getId()));
            final GUID idAccessContract = addData(FunctionalAdminCollections.ACCESS_CONTRACT);
            assertTrue(existsData(FunctionalAdminCollections.ACCESS_CONTRACT, idAccessContract.getId()));
            final GUID idIngestContract = addData(FunctionalAdminCollections.INGEST_CONTRACT);
            assertTrue(existsData(FunctionalAdminCollections.INGEST_CONTRACT, idIngestContract.getId()));
            final GUID idRegisterSummary = addData(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY);
            final GUID idRegisterDetail = addData(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL);
            assertTrue(existsData(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY, idRegisterSummary.getId()));
            assertTrue(existsData(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL, idRegisterDetail.getId()));
            final GUID adminContext = addAdminContextData(FunctionalAdminCollections.CONTEXT);
            final GUID idContext2 = addData(FunctionalAdminCollections.CONTEXT);
            assertTrue(existsData(FunctionalAdminCollections.CONTEXT, adminContext.getId()));
            assertTrue(existsData(FunctionalAdminCollections.CONTEXT, idContext2.getId()));

            final GUID idOntology = addData(FunctionalAdminCollections.ONTOLOGY);
            assertTrue(existsData(FunctionalAdminCollections.ONTOLOGY, idOntology.getId()));
            // delete all
            given()
                .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
                .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
                .header(GlobalDataRest.X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
                .cookie(COOKIE)
                .expect()
                .statusCode(Status.OK.getStatusCode())
                .when()
                .delete("delete");
            // check not deleted
            assertTrue(existsData(FunctionalAdminCollections.FORMATS, idFormat.getId()));
            // check deleted
            assertFalse(existsData(FunctionalAdminCollections.PROFILE, idProfile.getId()));
            assertFalse(existsData(FunctionalAdminCollections.ACCESS_CONTRACT, idAccessContract.getId()));
            assertFalse(existsData(FunctionalAdminCollections.INGEST_CONTRACT, idIngestContract.getId()));
            assertFalse(existsData(FunctionalAdminCollections.RULES, idRule.getId()));
            assertFalse(existsData(FunctionalAdminCollections.AGENCIES, idAgency.getId()));
            assertFalse(existsData(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY, idRegisterSummary.getId()));
            assertFalse(existsData(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL, idRegisterDetail.getId()));
            assertFalse(existsData(FunctionalAdminCollections.PROFILE, idProfile.getId()));
            assertFalse(existsData(FunctionalAdminCollections.CONTEXT, idContext2.getId()));
            //Admin context must still exist
            assertTrue(existsData(FunctionalAdminCollections.CONTEXT, adminContext.getId()));
        } catch (final ReferentialException | InvalidParseOperationException | DocumentAlreadyExistsException e) {
            LOGGER.error(e);
            fail("Exception using mongoDbAccess");
        }
    }

    private GUID addData(FunctionalAdminCollections collection)
        throws ReferentialException, SchemaValidationException, DocumentAlreadyExistsException {
        final GUID guid = GUIDFactory.newGUID();
        ObjectNode data1 = JsonHandler.createObjectNode().put("_id", guid.getId());

        if (
            !collection.equals(FunctionalAdminCollections.CONTEXT) &&
            !collection.equals(FunctionalAdminCollections.SECURITY_PROFILE) &&
            !collection.equals(FunctionalAdminCollections.FORMATS)
        ) {
            data1.put("_tenant", 1);
        }
        data1.put("_v", "0");
        switch (collection) {
            case ONTOLOGY:
                data1.put("CreationDate", "2008-10-10");
                data1.put("Identifier", "Identifier_" + GUIDFactory.newGUID().getId());
                data1.put("LastUpdate", "2008-10-10");
                data1.put("Origin", "EXTERNAL");
                data1.put("Type", "TEXT");
                break;
            case AGENCIES:
                data1.put("Name", "aName");
                data1.put("Identifier", "Identifier_" + GUIDFactory.newGUID().getId());
                break;
            case PROFILE:
                data1.put("Name", "aName");
                data1.put("Identifier", "Identifier_" + GUIDFactory.newGUID().getId());
                data1.put("Status", "ACTIVE");
                data1.put("Format", "RNG");
                data1.put("CreationDate", "2019-02-13");
                data1.put("LastUpdate", "2019-02-13");
                break;
            case ACCESS_CONTRACT:
                data1.put("Name", "aName");
                data1.put("Identifier", "Identifier_" + GUIDFactory.newGUID().getId());
                data1.put("Status", "ACTIVE");
                data1.put("CreationDate", "2019-02-13");
                data1.put("LastUpdate", "2019-02-13");
                data1.put("EveryDataObjectVersion", false);
                data1.put("EveryOriginatingAgency", true);
                data1.put("WritingPermission", true);
                data1.put("AccessLog", "INACTIVE");
                break;
            case INGEST_CONTRACT:
                data1.put("Name", "aName");
                data1.put("Identifier", "Identifier_" + GUIDFactory.newGUID().getId());
                data1.put("Status", "ACTIVE");
                data1.put("CreationDate", "2019-02-13");
                data1.put("LastUpdate", "2019-02-13");
                data1.put("EveryDataObjectVersion", false);
                data1.put("EveryFormatType", true);
                data1.put("FormatUnidentifiedAuthorized", true);
                data1.put("MasterMandatory", true);
                break;
            case MANAGEMENT_CONTRACT:
                data1.put("Name", "aName");
                data1.put("Identifier", "Identifier_" + GUIDFactory.newGUID().getId());
                data1.put("Status", "ACTIVE");
                data1.put("CreationDate", "2019-02-13");
                data1.put("LastUpdate", "2019-02-13");
                data1.set(
                    "Storage",
                    JsonHandler.createObjectNode()
                        .put("UnitStrategy", "default")
                        .put("ObjectGroupStrategy", "default")
                        .put("ObjectStrategy", "default")
                );
                break;
            case RULES:
                data1.put("RuleId", "APP-00001");
                data1.put("RuleType", "AppraisalRule");
                data1.put("RuleValue", "Dossier individuel d’agent civil");
                data1.put("RuleDuration", "80");
                data1.put("RuleMeasurement", "Year");
                data1.put("CreationDate", "2019-02-10");
                data1.put("UpdateDate", "2019-02-14");
                break;
            case CONTEXT:
                data1.put(CONTEXT_NAME, "aName");
                data1.put("Identifier", "Identifier_" + GUIDFactory.newGUID().getId());
                data1.put("CreationDate", "2019-02-13");
                data1.put("LastUpdate", "2019-02-13");
                data1.put("EnableControl", true);
                final ObjectNode permissionNode = JsonHandler.createObjectNode();
                permissionNode.put("tenant", TENANT_ID);
                data1.set("Permissions", JsonHandler.createArrayNode().add(permissionNode));
                data1.put("SecurityProfile", "admin-security-profile");
                data1.put("Status", "ACTIVE");
                break;
            case SECURITY_PROFILE:
                data1.put("Name", "aName");
                data1.put("Identifier", "admin-security-profile_" + GUIDFactory.newGUID().getId());
                data1.set("Permissions", new ArrayNode(null));
                data1.put("FullAccess", true);
                break;
            case FORMATS:
                data1.put("Name", "Plain Text File");
                data1.put("PUID", "x-fmt/111");
                data1.put("CreatedDate", "2019-02-15");
                data1.put("VersionPronom", "94");
                break;
            case ACCESSION_REGISTER_SUMMARY:
                data1.put("OriginatingAgency", "FRAN_NP_009913");
                data1.set("TotalObjectGroups", new ObjectNode(null));
                data1.set("TotalUnits", new ObjectNode(null));
                data1.set("TotalObjects", new ObjectNode(null));
                data1.set("ObjectSize", new ObjectNode(null));
                break;
            case ACCESSION_REGISTER_DETAIL:
                data1.put("OriginatingAgency", "FRAN_NP_009913");
                data1.put("Opc", GUIDFactory.newGUID().toString());
                data1.put("Opi", GUIDFactory.newGUID().toString());
                data1.set("Events", new ArrayNode(null));
                data1.set("TotalObjectGroups", new ObjectNode(null));
                data1.set("TotalUnits", new ObjectNode(null));
                data1.set("TotalObjects", new ObjectNode(null));
                data1.set("ObjectSize", new ObjectNode(null));
                break;
            default:
                throw new IllegalArgumentException(String.format("ERROR: Invalid collection {%s}", collection));
        }

        mongoDbAccessAdmin.insertDocument(data1, collection).close();
        return guid;
    }

    @SuppressWarnings("rawtypes")
    public GUID addData(MetadataCollections collection) throws MetaDataExecutionException {
        final GUID guid = GUIDFactory.newGUID();
        final ObjectNode data1 = JsonHandler.createObjectNode().put("_id", guid.getId());
        data1.put(VitamDocument.TENANT_ID, TENANT_ID);
        MetadataDocument document;
        if (collection.equals(MetadataCollections.OBJECTGROUP)) {
            document = new ObjectGroup(data1);
        } else {
            document = new Unit(data1);
        }

        collection.getCollection().insertOne(document);

        collection.getEsClient().insertFullDocument(collection, TENANT_ID, document.getId(), document);

        return guid;
    }

    @SuppressWarnings("rawtypes")
    public GUID addData(LogbookCollections collection) {
        final GUID guid = GUIDFactory.newGUID();
        final ObjectNode data1 = JsonHandler.createObjectNode().put("_id", guid.getId());
        data1.put(VitamDocument.TENANT_ID, TENANT_ID);
        VitamDocument document;
        if (collection.equals(LogbookCollections.OPERATION)) {
            document = new LogbookOperation(data1);
        } else if (collection.equals(LogbookCollections.LIFECYCLE_UNIT)) {
            document = new LogbookLifeCycleUnit(data1);
        } else {
            document = new LogbookLifeCycleObjectGroup(data1);
        }

        collection.getCollection().insertOne(document);
        return guid;
    }

    public GUID addAdminContextData(FunctionalAdminCollections collection)
        throws ReferentialException, InvalidCreateOperationException, InvalidGuidOperationException, InvalidParseOperationException, SchemaValidationException, DocumentAlreadyExistsException {
        final Query query = QueryHelper.or().add(QueryHelper.eq(CONTEXT_NAME, ADMIN_CONTEXT));
        JsonNode select = query.getCurrentObject();
        DbRequestResult result = mongoDbAccessAdmin.findDocuments(select, FunctionalAdminCollections.CONTEXT);
        GUID adminContext;
        if (result.getCount() > 0) {
            adminContext = GUIDReader.getGUID(result.getDocuments(Context.class, ContextModel.class).get(0).getId());
        } else {
            adminContext = GUIDFactory.newGUID();
            final ObjectNode data1 = JsonHandler.createObjectNode().put("_id", adminContext.getId());
            data1.put(CONTEXT_NAME, ADMIN_CONTEXT);
            data1.put("Identifier", "Identifier");
            data1.put("CreationDate", "2019-02-13");
            data1.put("LastUpdate", "2019-02-13");
            data1.put("EnableControl", true);
            final ObjectNode permissionNode = JsonHandler.createObjectNode();
            permissionNode.put("tenant", TENANT_ID);
            data1.set("Permissions", JsonHandler.createArrayNode().add(permissionNode));
            data1.put("SecurityProfile", "admin-security-profile");
            data1.put("Status", "ACTIVE");
            mongoDbAccessAdmin.insertDocument(data1, collection).close();
        }
        return adminContext;
    }

    public GUID addAdminSecurityData(FunctionalAdminCollections collection)
        throws ReferentialException, InvalidCreateOperationException, InvalidGuidOperationException, InvalidParseOperationException, SchemaValidationException, DocumentAlreadyExistsException {
        final Query query = QueryHelper.or().add(QueryHelper.eq(SECURITY_PROFIL_NAME, SECURITY_PROFIL_NAME_TO_SAVE));
        JsonNode select = query.getCurrentObject();
        DbRequestResult result = mongoDbAccessAdmin.findDocuments(select, FunctionalAdminCollections.SECURITY_PROFILE);
        GUID adminContext;
        if (result.getCount() > 0) {
            adminContext = GUIDReader.getGUID(result.getDocuments(Context.class, ContextModel.class).get(0).getId());
        } else {
            adminContext = GUIDFactory.newGUID();
            final ObjectNode data1 = JsonHandler.createObjectNode().put("_id", adminContext.getId());
            data1.put(SECURITY_PROFIL_NAME, SECURITY_PROFIL_NAME_TO_SAVE);
            data1.put("Identifier", "admin-security-profile");
            data1.set("Permissions", new ArrayNode(null));
            data1.put("FullAccess", true);
            mongoDbAccessAdmin.insertDocument(data1, collection).close();
        }
        return adminContext;
    }

    public boolean existsData(FunctionalAdminCollections collection, String id) {
        return mongoDbAccessAdmin.getDocumentById(id, collection) != null;
    }

    public boolean existsData(LogbookCollections collection, String id) {
        final BasicDBObject bbo = new BasicDBObject("_id", id);
        return collection.getCollection().find(bbo).first() != null;
    }

    public boolean existsData(MetadataCollections collection, String id) {
        final BasicDBObject bbo = new BasicDBObject("_id", id);
        return collection.getCollection().find(bbo).first() != null;
    }
}
