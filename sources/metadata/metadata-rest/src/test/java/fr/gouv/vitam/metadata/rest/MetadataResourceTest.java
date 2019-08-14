/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 *******************************************************************************/
package fr.gouv.vitam.metadata.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.database.api.VitamRepositoryFactory;
import fr.gouv.vitam.common.database.api.impl.VitamElasticsearchRepository;
import fr.gouv.vitam.common.database.api.impl.VitamMongoRepository;
import fr.gouv.vitam.common.database.parameter.IndexParameters;
import fr.gouv.vitam.common.database.parameter.SwitchIndexParameters;
import fr.gouv.vitam.common.database.parser.request.GlobalDatasParser;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.server.ElasticsearchAccessFunctionalAdmin;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.metadata.api.config.MetaDataConfiguration;
import fr.gouv.vitam.metadata.core.database.collections.ElasticsearchAccessMetadata;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.core.database.collections.MetadataDocument;
import fr.gouv.vitam.metadata.core.database.collections.MongoDbAccessMetadataImpl;
import fr.gouv.vitam.metadata.core.database.collections.ObjectGroup;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import net.javacrumbs.jsonunit.JsonAssert;
import net.javacrumbs.jsonunit.core.Option;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.MarshalException;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.with;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

public class MetadataResourceTest {
    private static final String DATA =
        "{ \"#id\": \"aeaqaaaaaaaaaaabaawkwak2ha24fdaaaaaq\", " + "\"data\": \"data1\" }";
    private static final String DATA2 =
        "{ \"#id\": \"aeaqaaaaaeaaaaakaarp4akuuf2ldmyaaaab\"," + "\"data\": \"data2\" }";
    private static final String INVALID_DATA = "{ \"INVALID\": true }";

    private static final String DATA_URI = "/metadata/v1";
    private static final String JETTY_CONFIG = "jetty-config-test.xml";

    public static final String PREFIX = GUIDFactory.newGUID().getId();


    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    @ClassRule
    public static MongoRule mongoRule = new MongoRule(MongoDbAccessMetadataImpl.getMongoClientOptions());

    @ClassRule
    public static ElasticsearchRule elasticsearchRule = new ElasticsearchRule();
    private static JunitHelper junitHelper;
    private static int serverPort;

    private static MetadataMain metadataMain;
    private static final int tenantId = 0;
    private static final List tenantList = Lists.newArrayList(tenantId);
    private static final Integer TENANT_ID = 0;
    private static ElasticsearchAccessMetadata elasticsearchAccessMetadata;
    private static ElasticsearchAccessFunctionalAdmin accessFunctionalAdmin;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        junitHelper = JunitHelper.getInstance();
        List<ElasticsearchNode> nodes = new ArrayList<>();
        nodes.add(new ElasticsearchNode("localhost", elasticsearchRule.getTcpPort()));

        final List<MongoDbNode> mongo_nodes = new ArrayList<>();
        mongo_nodes.add(new MongoDbNode("localhost", mongoRule.getDataBasePort()));
        final MetaDataConfiguration configuration =
            new MetaDataConfiguration(mongo_nodes, mongoRule.getMongoDatabase().getName(),
                elasticsearchRule.getClusterName(), nodes);

        elasticsearchAccessMetadata = new ElasticsearchAccessMetadata(ElasticsearchRule.VITAM_CLUSTER, nodes);
        MetadataCollections.beforeTestClass(mongoRule.getMongoDatabase(), PREFIX, elasticsearchAccessMetadata, 0, 1);
        accessFunctionalAdmin = new ElasticsearchAccessFunctionalAdmin(ElasticsearchRule.VITAM_CLUSTER, nodes);
        FunctionalAdminCollections.beforeTestClass(mongoRule.getMongoDatabase(), PREFIX, accessFunctionalAdmin,
            Lists.newArrayList(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL,
                FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY));
        configuration.setJettyConfig(JETTY_CONFIG);
        configuration.setUrlProcessing("http://processing.service.consul:8203/");
        VitamConfiguration.setTenants(tenantList);
        serverPort = junitHelper.findAvailablePort();

        File configurationFile = tempFolder.newFile();

        PropertiesUtils.writeYaml(configurationFile, configuration);

        metadataMain = new MetadataMain(configurationFile.getAbsolutePath());
        metadataMain.start();
        JunitHelper.unsetJettyPortSystemProperty();

        RestAssured.port = serverPort;
        RestAssured.basePath = DATA_URI;
    }

    @AfterClass
    public static void tearDownAfterClass() {
        MetadataCollections.afterTestClass(true, 0, 1);
        FunctionalAdminCollections.afterTestClass(Lists.newArrayList(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL,
                FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY), true);
        try {
            metadataMain.stop();
        } catch (final Exception e) {
            SysErrLogger.FAKE_LOGGER.syserr("", e);
        }
        junitHelper.releasePort(serverPort);
        VitamClientFactory.resetConnections();
    }


    @After
    public void tearDown() {
        MetadataCollections.afterTest(0, 1);
        FunctionalAdminCollections.afterTest(Lists.newArrayList(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL,
            FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY));
    }

    private static final JsonNode buildDSLWithOptions(String data) throws Exception {
        return JsonHandler.getFromString("{ $roots : [], $query : [], $data : " + data + " }");
    }

    private static final JsonNode buildDSLWithOptionsRoots(String data, String... roots) throws Exception {
        return JsonHandler.getFromString("{ $roots : [ " + joinIds(roots) + "], $query : [], $data : " + data + " }");
    }

    private static final String joinIds(String... ids) {
        return (ids.length == 0) ? "" : "\"" + StringUtils.join(ids, "\",\"") + "\"";
    }

    private static String createJsonStringWithDepth(int depth) {
        final StringBuilder obj = new StringBuilder();
        if (depth == 0) {
            return " \"b\" ";
        }
        obj.append("{ \"a\": ").append(createJsonStringWithDepth(depth - 1)).append("}");
        return obj.toString();
    }

    private static String generateResponseErrorFromStatus(Status status, String message)
        throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(new VitamError(status.name()).setHttpCode(status.getStatusCode())
            .setContext("ingest").setState("code_vitam")
            .setMessage(status.getReasonPhrase()).setDescription(message));
    }


    /**
     * Test status endpoint
     */
    @Test
    public void shouldGetStatusOK() {
        get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());
    }

    /**
     * Test insert Unit endpoint
     */
    @Test
    public void shouldRaiseExceptionIfBodyIsNotCorrect() throws Exception {
        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(INVALID_DATA).when()
            .post("/units").then()
            .body(equalTo(generateResponseErrorFromStatus(Status.BAD_REQUEST, "Parse in error for Insert: empty data")))
            .statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void shouldReturnErrorConflictIfIdDuplicated() throws Exception {
        with()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(buildDSLWithOptions(DATA)).when()
            .post("/units").then()
            .statusCode(Status.CREATED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(buildDSLWithOptions(DATA)).when()
            .post("/units").then()
            .body(equalTo(generateResponseErrorFromStatus(Status.CONFLICT,
                "Metadata already exists: [aeaqaaaaaaaaaaabaawkwak2ha24fdaaaaaq]")))
            .statusCode(Status.CONFLICT.getStatusCode());
    }

    @Test
    public void givenInsertUnitWithRootsExistsWhenParentFoundThenReturnCreated() throws Exception {
        with()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(buildDSLWithOptions(DATA)).when()
            .post("/units").then()
            .statusCode(Status.CREATED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(buildDSLWithOptionsRoots(DATA2, "aeaqaaaaaaaaaaabaawkwak2ha24fdaaaaaq")).when()
            .post("/units").then()
            .statusCode(Status.CREATED.getStatusCode());
    }

    @Test
    public void givenInsertComplexUnitGraphThenCheckGraph() throws Exception {

        /*
         * Test case :
         * 1(sp1)  2(sp2)  3(sp3)
         * |     / |       |
         * |   /   |       |
         * | /     |       |
         * 4(sp1)  5(sp2)  6(sp4)
         * |     /
         * |   /
         * | /
         * 7(sp1)
         * |
         * |
         * |
         * 8(sp1)
         */

        // Given
        String guid1 = "1";
        String guid2 = "2";
        String guid3 = "3";
        String guid4 = "4";
        String guid5 = "5";
        String guid6 = "6";
        String guid7 = "7";
        String guid8 = "8";

        String SERVICE_PRODUCER_1 = "sp1";
        String SERVICE_PRODUCER_2 = "sp2";
        String SERVICE_PRODUCER_3 = "sp3";
        String SERVICE_PRODUCER_4 = "sp4";

        createUnit(guid1, SERVICE_PRODUCER_1);
        createUnit(guid2, SERVICE_PRODUCER_2);
        createUnit(guid3, SERVICE_PRODUCER_3);
        createUnit(guid4, SERVICE_PRODUCER_1, guid1, guid2);
        createUnit(guid5, SERVICE_PRODUCER_2, guid2);
        createUnit(guid6, SERVICE_PRODUCER_4, guid3);
        createUnit(guid7, SERVICE_PRODUCER_1, guid4, guid5);
        createUnit(guid8, SERVICE_PRODUCER_1, guid7);

        // Check graph...
        checkUnitGraph(guid1, "test_complex_unit_graph_unit1.json");
        checkUnitGraph(guid6, "test_complex_unit_graph_unit6.json");
        checkUnitGraph(guid8, "test_complex_unit_graph_unit8.json");
    }

    private void createUnit(String id, String sp, String... directParents) throws Exception {

        String unitData = "{" + "\"#id\": \"" + id + "\"," + "\"#originating_agency\": \"" + sp + "\"" + "}";

        with()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(buildDSLWithOptionsRoots(unitData, directParents)).when()
            .post("/units").then()
            .statusCode(Status.CREATED.getStatusCode());
    }

    private void checkUnitGraph(String id, String expectedContentFileName)
        throws InvalidParseOperationException, IOException {
        String responseJson = given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .get("/raw/units/" + id).then()
            .statusCode(Status.OK.getStatusCode())
            .extract().body().asString();

        ObjectNode actualUnitJson = (ObjectNode) JsonHandler.getFromString(responseJson).get("$results").get(0);
        ObjectNode expectedUnitJson = (ObjectNode) JsonHandler.getFromFile(
            PropertiesUtils.getResourceFile(expectedContentFileName));

        // Check last persisted date
        LocalDateTime graphLastPersistedDate = LocalDateUtil.parseMongoFormattedDate(
            actualUnitJson.get(MetadataDocument.GRAPH_LAST_PERSISTED_DATE).asText());
        assertThat(graphLastPersistedDate)
            .isAfter(LocalDateUtil.now().minusMinutes(1)).isBefore(LocalDateUtil.now().plusSeconds(1));

        // Compare jsons (excluding graph last persisted date)
        actualUnitJson.remove(MetadataDocument.GRAPH_LAST_PERSISTED_DATE);
        expectedUnitJson.remove(MetadataDocument.GRAPH_LAST_PERSISTED_DATE);

        JsonAssert.assertJsonEquals(expectedUnitJson.toString(), actualUnitJson.toString(),
            JsonAssert.when(Option.IGNORING_ARRAY_ORDER));
    }

    @Test(expected = MarshalException.class)
    public void shouldReturnErrorIfContentTypeIsNotJson() throws Exception {
        given()
            .contentType("metadataMain/xml")
            .body(buildDSLWithOptions(DATA)).when()
            .post("/units").then()
            .statusCode(Status.UNSUPPORTED_MEDIA_TYPE.getStatusCode());
    }

    @Test
    public void shouldReturnErrorNotFoundIfParentNotFound() throws Exception {
        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(buildDSLWithOptionsRoots(DATA, "aeaqaaaaaeaaaaakaarp4akuuf2ldmyaaaab")).when()
            .post("/units")
            .then()
            .body(equalTo(generateResponseErrorFromStatus(Status.NOT_FOUND,
                "Cannot find parents: [aeaqaaaaaeaaaaakaarp4akuuf2ldmyaaaab]")))
            .statusCode(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void shouldReturnErrorRequestBadRequestIfDocumentIsTooLarge() throws Exception {
        final int limitRequest = GlobalDatasParser.limitRequest;
        GlobalDatasParser.limitRequest = 99;
        try {
            given()
                .contentType(ContentType.JSON)
                .body(buildDSLWithOptions(createJsonStringWithDepth(60))).when()
                .post("/units").then()
                .body(equalTo(generateResponseErrorFromStatus(Status.BAD_REQUEST, "String exceeds sanity check of 99")))
                .statusCode(Status.BAD_REQUEST.getStatusCode());
        } finally {
            GlobalDatasParser.limitRequest = limitRequest;
        }
    }

    @Test
    public void shouldReturnResponseOKIfDocumentCreated() throws Exception {
        String responseOK = JsonHandler.getFromFile(PropertiesUtils.findFile("reponseCreated.json")).toString();
        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(buildDSLWithOptions(DATA)).when()
            .post("/units").then()
            .body(equalTo(responseOK))
            .statusCode(Status.CREATED.getStatusCode());
    }

    // Test object group
    @Test
    public void givenInsertObjectGroupWithBodyIsNotCorrectThenReturnErrorBadRequest() throws Exception {
        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(INVALID_DATA).when()
            .post("/objectgroups").then()
            .body(equalTo(generateResponseErrorFromStatus(Status.BAD_REQUEST, "Parse in error for Insert: empty data")))
            .statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void givenInsertObjectGroupWithIdDuplicatedThenReturnErrorConflict() throws Exception {
        with()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(buildDSLWithOptions(DATA)).when()
            .post("/units").then()
            .statusCode(Status.CREATED.getStatusCode());
        with()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(buildDSLWithOptionsRoots(DATA2, "aeaqaaaaaaaaaaabaawkwak2ha24fdaaaaaq")).when()
            .post("/objectgroups").then()
            .statusCode(Status.CREATED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(buildDSLWithOptionsRoots(DATA2, "aeaqaaaaaaaaaaabaawkwak2ha24fdaaaaaq")).when()
            .post("/objectgroups").then()
            .body(equalTo(generateResponseErrorFromStatus(Status.CONFLICT,
                "Metadata already exists: [aeaqaaaaaeaaaaakaarp4akuuf2ldmyaaaab]")))
            .statusCode(Status.CONFLICT.getStatusCode());
    }

    @Test
    public void shouldReturnErrorRequestBadRequestWhenInsertGOIfDocumentIsTooLarge() throws Exception {
        final int limitRequest = GlobalDatasParser.limitRequest;
        GlobalDatasParser.limitRequest = 99;
        try {
            given()
                .contentType(ContentType.JSON)
                .body(buildDSLWithOptions(createJsonStringWithDepth(60))).when()
                .post("/objectgroups").then()
                .body(equalTo(generateResponseErrorFromStatus(Status.BAD_REQUEST, "String exceeds sanity check of 99")))
                .statusCode(Status.BAD_REQUEST.getStatusCode());
        } finally {
            GlobalDatasParser.limitRequest = limitRequest;
        }
    }

    @Test
    @RunWithCustomExecutor
    public void should_find_accession_register_on_unit() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        String operationId = "1234";

        VitamRepositoryFactory factory = VitamRepositoryFactory.get();
        VitamMongoRepository mongo =
            factory.getVitamMongoRepository(MetadataCollections.UNIT.getVitamCollection());
        Document doc = new Document("_id", "1")
            .append("_ops", singletonList(operationId))
            .append("_tenant", 0)
            .append("_max", 1)
            .append("_sp", "sp1")
            .append("_opi", operationId)
            .append("_sps", Arrays.asList("sp1", "sp2"));
        mongo.save(doc);

        VitamElasticsearchRepository es = factory.getVitamESRepository(MetadataCollections.UNIT.getVitamCollection());
        es.save(doc);

        given()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get("/accession-registers/units/" + operationId).then()
            .body("$results.size()", equalTo(1))
            .statusCode(Status.OK.getStatusCode());

    }

    @Test
    @RunWithCustomExecutor
    public void should_find_accession_register_on_object_group() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        Document doc = (Document) (new ObjectGroup(
                JsonHandler.getFromInputStream(getClass().getResourceAsStream("/object_sp1_1.json"))));
        VitamRepositoryFactory factory = VitamRepositoryFactory.get();
        VitamMongoRepository mongo =
            factory.getVitamMongoRepository(MetadataCollections.OBJECTGROUP.getVitamCollection());
        mongo.save(doc);
        VitamElasticsearchRepository es = factory.getVitamESRepository(MetadataCollections.OBJECTGROUP.getVitamCollection());
        es.save(doc);
        String operationId = "aedqaaaaacgbcaacaar3kak4tr2o3wqaaaaq";
        
        given()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get("/accession-registers/objects/" + operationId).then()
            .body("$results.size()", equalTo(1))
            .statusCode(Status.OK.getStatusCode());
    }

    @Test
    public void indexCollectionNoBodyPreconditionFailed() {
        given().contentType(MediaType.APPLICATION_JSON)
            .when().post("/reindex").then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void indexCollectionUnknownInternalServerError() {
        IndexParameters indexParameters = new IndexParameters();
        List<Integer> tenants = new ArrayList<>();
        tenants.add(0);
        indexParameters.setTenants(tenants);
        indexParameters.setCollectionName("fake");

        given().contentType(MediaType.APPLICATION_JSON).body(indexParameters)
            .when().post("/reindex").then().statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
            .body("collectionName", equalTo("fake"))
            .body("KO.size()", equalTo(1))
            .body("KO.get(0).indexName", equalTo("fake_0_*"))
            .body("KO.get(0).message", containsString("'fake'"))
            .body("KO.get(0).tenant", equalTo(0));
    }

    @Test
    public void aliasCollectionNoBodyPreconditionFailed() {
        given().contentType(MediaType.APPLICATION_JSON)
            .when().post("/alias").then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void aliasUnkwownCollection() {
        SwitchIndexParameters parameters = new SwitchIndexParameters();
        parameters.setAlias("alias");
        parameters.setIndexName("indexName");
        given().contentType(MediaType.APPLICATION_JSON).body(parameters)
            .when().post("/alias").then().statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }
}
