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
package fr.gouv.vitam.metadata.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.model.config.CollectionConfiguration;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.metadata.api.model.BulkUnitInsertEntry;
import fr.gouv.vitam.metadata.api.model.BulkUnitInsertRequest;
import fr.gouv.vitam.metadata.core.config.DefaultCollectionConfiguration;
import fr.gouv.vitam.metadata.core.config.ElasticsearchMetadataIndexManager;
import fr.gouv.vitam.metadata.core.config.MetaDataConfiguration;
import fr.gouv.vitam.metadata.core.config.MetadataIndexationConfiguration;
import fr.gouv.vitam.metadata.core.database.collections.ElasticsearchAccessMetadata;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollectionsTestUtils;
import fr.gouv.vitam.metadata.core.database.collections.ObjectGroup;
import fr.gouv.vitam.metadata.core.database.collections.Unit;
import fr.gouv.vitam.metadata.core.mapping.MappingLoader;
import fr.gouv.vitam.metadata.rest.utils.MappingLoaderTestUtils;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.After;
import org.junit.AfterClass;
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

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.with;

/**
 *
 */
public class SelectObjectGroupResourceTest {

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    private static final String UNIT_DATA =
        "{ \"_id\": \"aeaqaaaaaeaaaaakaarp4akuuf2ldmyaaaaq\", " + "\"data\": \"data2\" }";

    private static final String OG_DATA2 =
        "{ \"_id\": \"aeaqaaaaaeaaaaakaarp4akuuf2ldmyaaaab\"," + "\"data\": \"data2\" }";

    private static final String OBJECT_GROUP_ID = "aeaqaaaaaeaaaaakaarp4akuuf2ldmyaaaab";
    private static final String DATA_URI = "/metadata/v1";
    private static final String OBJECT_GROUPS_URI = "/objectgroups";
    private static final String JETTY_CONFIG = "jetty-config-test.xml";

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    private static final String HOST_NAME = "127.0.0.1";
    private static final Integer TENANT_ID = 0;

    private static MetadataMain application;

    private static final String BAD_QUERY_TEST =
        "{ \"$or\" : " +
        "[ " +
        "   {\"$exists\" : \"#id\"}, " +
        "   {\"$missing\" : \"mavar2\"}, " +
        "   {\"$badRquest\" : \"mavar3\"}, " +
        "   {\"$or\" : [ " +
        "          {\"$in\" : { \"mavar4\" : [1, 2, \"maval1\"] }}, " +
        "]}";

    private static final String BODY_TEST =
        "{\"$query\": {\"$eq\": {\"data\" : \"data2\" }}, \"$projection\": {}, \"$filter\": {}}";
    private static JunitHelper junitHelper;
    private static int serverPort;
    static final int tenantId = 0;
    static final List<Integer> tenantList = Collections.singletonList(tenantId);
    private static final MappingLoader mappingLoader = MappingLoaderTestUtils.getTestMappingLoader();
    private static final ElasticsearchMetadataIndexManager indexManager =
        MetadataCollectionsTestUtils.createTestIndexManager(tenantList, Collections.emptyMap(), mappingLoader);

    private static ElasticsearchAccessMetadata accessMetadata;

    @ClassRule
    public static MongoRule mongoRule = new MongoRule(
        MongoDbAccess.getMongoClientSettingsBuilder(Unit.class, ObjectGroup.class)
    );

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        List<ElasticsearchNode> esNodes = Lists.newArrayList(
            new ElasticsearchNode(ElasticsearchRule.getHost(), ElasticsearchRule.getPort())
        );

        accessMetadata = new ElasticsearchAccessMetadata(ElasticsearchRule.VITAM_CLUSTER, esNodes, indexManager);
        MetadataCollectionsTestUtils.beforeTestClass(
            mongoRule.getMongoDatabase(),
            GUIDFactory.newGUID().getId(),
            accessMetadata
        );
        junitHelper = JunitHelper.getInstance();

        final List<MongoDbNode> mongo_nodes = new ArrayList<>();
        mongo_nodes.add(new MongoDbNode(HOST_NAME, MongoRule.getDataBasePort()));
        final MetaDataConfiguration configuration = new MetaDataConfiguration(
            mongo_nodes,
            MongoRule.VITAM_DB,
            ElasticsearchRule.VITAM_CLUSTER,
            esNodes,
            mappingLoader
        );
        configuration.setJettyConfig(JETTY_CONFIG);
        configuration.setUrlProcessing("http://processing.service.consul:8203/");
        configuration.setContextPath("/metadata");
        configuration.setIndexationConfiguration(
            new MetadataIndexationConfiguration()
                .setDefaultCollectionConfiguration(
                    new DefaultCollectionConfiguration()
                        .setUnit(new CollectionConfiguration(1, 0))
                        .setObjectgroup(new CollectionConfiguration(1, 0))
                )
        );

        VitamConfiguration.setTenants(tenantList);
        serverPort = junitHelper.findAvailablePort();

        File configurationFile = tempFolder.newFile();

        PropertiesUtils.writeYaml(configurationFile, configuration);

        application = new MetadataMain(configurationFile.getAbsolutePath());
        application.start();

        JunitHelper.unsetJettyPortSystemProperty();

        RestAssured.port = serverPort;
        RestAssured.basePath = DATA_URI;
    }

    @AfterClass
    public static void tearDownAfterClass() {
        try {
            application.stop();
        } catch (Exception e) {
            SysErrLogger.FAKE_LOGGER.syserr("", e);
        } finally {
            MetadataCollectionsTestUtils.afterTestClass(indexManager, true);

            junitHelper.releasePort(serverPort);
            VitamClientFactory.resetConnections();
        }
    }

    @After
    public void tearDown() {
        MetadataCollectionsTestUtils.afterTest(indexManager);
    }

    private static final JsonNode buildDSLWithOptions(String data) throws Exception {
        return JsonHandler.getFromString("{ $roots : [], $query : [], $data : " + data + " }");
    }

    private static final JsonNode buildDSLWithOptionsRoot(String root, String data) throws Exception {
        return JsonHandler.getFromString("{ $roots : [ \"" + root + "\"], $query : [], $data : " + data + " }");
    }

    @Test
    @RunWithCustomExecutor
    public void getObjectGroupPostOK() throws Exception {
        with()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(
                new BulkUnitInsertRequest(
                    Collections.singletonList(
                        new BulkUnitInsertEntry(Collections.emptySet(), JsonHandler.getFromString(UNIT_DATA))
                    )
                )
            )
            .when()
            .post("/units/bulk")
            .then()
            .statusCode(Status.CREATED.getStatusCode());
        with()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(buildDSLWithOptionsRoot("aeaqaaaaaeaaaaakaarp4akuuf2ldmyaaaaq", OG_DATA2))
            .when()
            .post(OBJECT_GROUPS_URI)
            .then()
            .statusCode(Status.CREATED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(JsonHandler.getFromString(BODY_TEST))
            .when()
            .get(OBJECT_GROUPS_URI + "/" + OBJECT_GROUP_ID)
            .then()
            .statusCode(Status.OK.getStatusCode());
    }

    @Test
    public void getObjectGroupPRECONDITION_FAILED() {
        given()
            .contentType(ContentType.JSON)
            .when()
            .get(OBJECT_GROUPS_URI + "/" + OBJECT_GROUP_ID)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void getObjectGroupBadRequest() {
        given()
            .contentType(ContentType.JSON)
            .body(BAD_QUERY_TEST)
            .when()
            .post(OBJECT_GROUPS_URI)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void getObjectGroupEmptyRequestBadRequest() {
        given()
            .contentType(ContentType.JSON)
            .body("")
            .when()
            .post(OBJECT_GROUPS_URI)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void getObjectGroupEmptyQueryPreconditionFailed() {
        given()
            .contentType(ContentType.JSON)
            .body("")
            .when()
            .get(OBJECT_GROUPS_URI + "/" + OBJECT_GROUP_ID)
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test(expected = InvalidParseOperationException.class)
    public void shouldReturnErrorRequestBadRequest() throws Exception {
        given()
            .contentType(ContentType.JSON)
            .body(buildDSLWithOptions("lkvhvgvuyqvkvj"))
            .when()
            .get(OBJECT_GROUPS_URI + "/" + OBJECT_GROUP_ID)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
    }
}
