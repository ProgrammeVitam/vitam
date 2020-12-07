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
package fr.gouv.vitam.metadata.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.database.parser.request.GlobalDatasParser;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.model.RequestResponseOK;
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
import fr.gouv.vitam.metadata.core.database.collections.MongoDbAccessMetadataImpl;
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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.with;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for select unit (and by id and bulk) functionality
 */
public class SelectUnitResourceTest {

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    private static final Integer TENANT_ID = 1;

    private static final String GUID_0 = GUIDFactory.newUnitGUID(TENANT_ID).toString();
    private static final String GUID_1 = GUIDFactory.newUnitGUID(TENANT_ID).toString();

    private final static String AU0_MGT = "{" +
        "    \"StorageRule\" : {" +
        "      \"Rules\":[{" +
        "      \"Rule\" : \"str0\"," +
        "      \"PreventInheritance\" : \"true\"," +
        "      \"StartDate\" : \"2017-01-01\"," +
        "      \"EndDate\" : \"2019-01-01\"" +
        "    }]}," +
        "    \"AccessRule\" : {" +
        "      \"Rules\":[{" +
        "      \"Rule\" : \"acc0\"," +
        "      \"StartDate\" : \"2017-01-01\"," +
        "      \"EndDate\" : \"2019-01-01\"" +
        "    }]," +
        "      \"FinalAction\" : \"RestrictedAccess\"" +
        "    }" +
        "  }";

    private final static String AU1_MGT = "{" +
        "    \"DissiminationRule\" : {" +
        "      \"Rules\":[{" +
        "      \"Rule\" : \"dis1\"" +
        "    }]}," +
        "    \"AccessRule\" : {" +
        "      \"PreventInheritance\" : \"true\"" +
        "    }" +
        "  }";

    private static final String DATA_0 =
        "{ \"_id\": \"" + GUID_0 + "\", " + "\"data\": \"data2\", \"_mgt\": " + AU0_MGT +
            ", \"DescriptionLevel\" : \"Grp\" }";

    private static final String DATA_1 =
        "{ \"_id\": \"" + GUID_1 + "\", " + "\"data\": \"data2\", \"_mgt\": " + AU1_MGT +
            ", \"DescriptionLevel\" : \"Item\" }";

    private static final String DATA_URI = "/metadata/v1";
    private static final String JETTY_CONFIG = "jetty-config-test.xml";
    static final List<Integer> tenantList = Lists.newArrayList(TENANT_ID);
    private static final MappingLoader mappingLoader = MappingLoaderTestUtils.getTestMappingLoader();
    private static final ElasticsearchMetadataIndexManager indexManager = MetadataCollectionsTestUtils
        .createTestIndexManager(tenantList, Collections.emptyMap(), mappingLoader);

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    private static final String BAD_QUERY_TEST =
        "{ \"$or\" : " + "[ " + "   {\"$exists\" : \"#id\"}, " + "   {\"$missing\" : \"mavar2\"}, " +
            "   {\"$badRquest\" : \"mavar3\"}, " +
            "   { \"$or\" : [ " + "          {\"$in\" : { \"mavar4\" : [1, 2, \"maval1\"] }}, " + "]}";

    private static final String BAD_QUERY_DSL_TEST =
            "{\"$query\": {\"$eq\": \"data2\" }, \"$projection\": {}, \"$filter\": {}}";
    
    private static final String SERVER_HOST = "localhost";

    private static final String SEARCH_QUERY =
        "{\"$query\": [], \"$projection\": {}, \"$filter\": {}}";
    private static final String SEARCH_QUERY_WITH_FACET_MGT =
        "{\"$query\": [{\"$exists\" : \"#id\"}], \"$projection\": {}, \"$filter\": {}, \"$facets\": [{\"$name\":\"mgt_facet\",\"$terms\":{\"$field\":\"#management.StorageRule.Rules.Rule\", \"$size\": 5, \"$order\": \"ASC\"}}]}";
    private static final String SEARCH_QUERY_WITH_FACET_DESC_LEVEL =
        "{\"$query\": [{\"$exists\" : \"#id\"}], \"$projection\": {}, \"$filter\": {}, \"$facets\": [{\"$name\":\"desc_level_facet\",\"$terms\":{\"$field\":\"DescriptionLevel\", \"$size\": 5, \"$order\": \"ASC\"}}]}";
    private static final String SEARCH_QUERY_WITH_FACET_TERMS_INVALID_SIZE =
        "{\"$query\": [{\"$exists\" : \"#id\"}], \"$projection\": {}, \"$filter\": {}, \"$facets\": [{\"$name\":\"desc_level_facet\",\"$terms\":{\"$field\":\"DescriptionLevel\", \"$order\": \"ASC\"}}]}";
    private static final String SEARCH_QUERY_WITH_FACET_TERMS_INVALID_ORDER =
        "{\"$query\": [{\"$exists\" : \"#id\"}], \"$projection\": {}, \"$filter\": {}, \"$facets\": [{\"$name\":\"desc_level_facet\",\"$terms\":{\"$field\":\"DescriptionLevel\", \"$size\": 5}}]}";

    private static final String SEARCH_QUERY_BY_GUID_1 =
        "{\"$query\": [ { \"$eq\": { \"#id\": \"" + GUID_1 + "\"} }], \"$projection\": {}, \"$filter\": {}}";
    private static final String SEARCH_QUERY_BY_GUID_0 =
            "{\"$query\": [ { \"$eq\": { \"#id\": \"" + GUID_0 + "\"} }], \"$projection\": {}, \"$filter\": {}}";
    /**
     * @deprecated : obsolete $rules projection. Use /unitsWithInheritedRules API
     */
    private static final String SEARCH_QUERY_WITH_RULE =
        "{\"$query\": [], \"$projection\": {\"$fields\" : {\"$rules\" : 1}}, \"$filter\": {}}";
    private static final String SEARCH_QUERY_WITH_FACET_FILTERS =
        "{" +
            "    \"$query\": [ { \"$exists\": \"#id\" } ]," +
            "    \"$projection\": {}," +
            "    \"$filter\": {}," +
            "    \"$facets\": [" +
            "        {" +
            "            \"$name\": \"filters_facet\"," +
            "            \"$filters\": {" +
            "                \"$query_filters\": [" +
            "                    {" +
            "                        \"$name\": \"StorageRules\"," +
            "                        \"$query\": { \"$exists\": \"#management.StorageRule.Rules.Rule\" }" +
            "                    },{ " +
            "                        \"$name\": \"AccessRules\"," +
            "                        \"$query\": { \"$exists\": \"#management.AccessRule.Rules.Rule\" }\n" +
            "                    }" +
            "                ]" +
            "            }" +
            "        }" +
            "    ]" +
            "}";

    private static JunitHelper junitHelper;
    private static int serverPort;

    private static MetadataMain application;

    private static ElasticsearchAccessMetadata accessMetadata;
    @ClassRule
    public static MongoRule mongoRule =
        new MongoRule(MongoDbAccessMetadataImpl.getMongoClientOptions());

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        List<ElasticsearchNode> esNodes =
            Lists.newArrayList(new ElasticsearchNode(ElasticsearchRule.getHost(), ElasticsearchRule.getPort()));

        MappingLoader mappingLoader = MappingLoaderTestUtils.getTestMappingLoader();

        accessMetadata = new ElasticsearchAccessMetadata(ElasticsearchRule.VITAM_CLUSTER, esNodes,
            indexManager);
        MetadataCollectionsTestUtils.beforeTestClass(mongoRule.getMongoDatabase(), GUIDFactory.newGUID().getId(),
            accessMetadata);
        junitHelper = JunitHelper.getInstance();

        final List<MongoDbNode> mongo_nodes = new ArrayList<>();
        mongo_nodes.add(new MongoDbNode(SERVER_HOST, mongoRule.getDataBasePort()));
        final MetaDataConfiguration configuration =
            new MetaDataConfiguration(mongo_nodes, MongoRule.VITAM_DB, ElasticsearchRule.VITAM_CLUSTER, esNodes,
                mappingLoader);
        VitamConfiguration.setTenants(tenantList);
        configuration.setJettyConfig(JETTY_CONFIG);
        configuration.setUrlProcessing("http://processing.service.consul:8203/");

        configuration.setIndexationConfiguration(new MetadataIndexationConfiguration()
            .setDefaultCollectionConfiguration(new DefaultCollectionConfiguration()
                .setUnit(new CollectionConfiguration(2, 1))
                .setObjectgroup(new CollectionConfiguration(2, 1))));

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
            MetadataCollectionsTestUtils.afterTestClass(indexManager, true);
            application.stop();
        } catch (Exception e) {
            SysErrLogger.FAKE_LOGGER.syserr("", e);
        } finally {
            junitHelper.releasePort(serverPort);
            VitamClientFactory.resetConnections();
        }
    }

    @After
    public void tearDown() {
        MetadataCollectionsTestUtils.afterTest(indexManager);
    }

    private static BulkUnitInsertRequest bulkInsertRequest(String data) throws InvalidParseOperationException {
        return new BulkUnitInsertRequest(Collections.singletonList(
            new BulkUnitInsertEntry(Collections.emptySet(), JsonHandler.getFromString(data))
        ));
    }

    private static BulkUnitInsertRequest bulkInsertRequest(String roots, String data)
        throws InvalidParseOperationException {
        return new BulkUnitInsertRequest(Collections.singletonList(
            new BulkUnitInsertEntry(Collections.singleton(roots), JsonHandler.getFromString(data))
        ));
    }


    private static String createJsonStringWithDepth(int depth) {
        final StringBuilder obj = new StringBuilder();
        if (depth == 0) {
            return " \"b\" ";
        }
        obj.append("{ \"a\": ").append(createJsonStringWithDepth(depth - 1)).append("}");
        return obj.toString();
    }

    // select archive unit test

    /**
     * @deprecated to be replaced with /unitsWithInheritedRules
     */
    @Test
    public void given_2units_insert_when_searchUnits_thenReturn_Found() throws Exception {
        with()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(bulkInsertRequest(DATA_1)).when()
            .post("/units/bulk").then()
            .statusCode(Status.CREATED.getStatusCode());

        with()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(bulkInsertRequest(DATA_0)).when()
            .post("/units/bulk").then()
            .statusCode(Status.CREATED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(JsonHandler.getFromString(DATA_1)).when()
            .post("/units/bulk").then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
    }


    @Test
    public void given_badRequestHHtp_when_selectUnit_thenReturn_BAD_REQUEST() {
        given()
            .contentType(ContentType.JSON)
            .when()
            .get("/units")
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }


    @Test
    public void given_Bad_Request_when_Select_thenReturn_Bad_Request() {

        given()
            .contentType(ContentType.JSON)
            .body(BAD_QUERY_TEST)
            .when()
            .get("/units")
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void given_emptyQuery_when_Select_thenReturn_BadRequest() {

        given()
            .contentType(ContentType.JSON)
            .body("")
            .when()
            .get("/units")
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    // select Unit by ID (request and uri)

    @Test
    @RunWithCustomExecutor
    public void given_2units_insert_when_searchUnitsByID_thenReturn_Found() throws Exception {
        with()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(bulkInsertRequest(DATA_1)).when()
            .post("/units/bulk").then()
            .statusCode(Status.CREATED.getStatusCode());

        with()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(bulkInsertRequest(DATA_0)).when()
            .post("/units/bulk").then()
            .statusCode(Status.CREATED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(JsonHandler.getFromString(SEARCH_QUERY)).when()
            .get("/units/" + GUID_0).then()
            .statusCode(Status.FOUND.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void given_2units_insert_when_searchUnitsWithFacet_thenReturn_Facet() throws Exception {

        with()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(bulkInsertRequest(DATA_1)).when()
            .post("/units/bulk").then()
            .statusCode(Status.CREATED.getStatusCode());

        with()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(bulkInsertRequest(DATA_0)).when()
            .post("/units/bulk").then()
            .statusCode(Status.CREATED.getStatusCode());

        InputStream stream = given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(JsonHandler.getFromString(SEARCH_QUERY_WITH_FACET_MGT)).when()
            .get("/units").then()
            .statusCode(Status.FOUND.getStatusCode()).extract().asInputStream();

        RequestResponseOK<JsonNode> responseOK1 = JsonHandler.getFromInputStream(stream, RequestResponseOK.class);
        assertThat(responseOK1.getFacetResults().size()).isEqualTo(1);
        assertThat(responseOK1.getFacetResults().get(0).getName()).isEqualTo("mgt_facet");
        assertThat(responseOK1.getFacetResults().get(0).getBuckets().size()).isEqualTo(1);
        assertThat(responseOK1.getFacetResults().get(0).getBuckets().get(0).getValue()).isEqualTo("str0");
        assertThat(responseOK1.getFacetResults().get(0).getBuckets().get(0).getCount()).isEqualTo(1);

        stream = given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(JsonHandler.getFromString(SEARCH_QUERY_WITH_FACET_DESC_LEVEL)).when()
            .get("/units").then()
            .statusCode(Status.FOUND.getStatusCode()).extract().asInputStream();

        RequestResponseOK<JsonNode> responseOK2 = JsonHandler.getFromInputStream(stream, RequestResponseOK.class);
        assertThat(responseOK2.getFacetResults().size()).isEqualTo(1);
        assertThat(responseOK2.getFacetResults().get(0).getName()).isEqualTo("desc_level_facet");
        assertThat(responseOK2.getFacetResults().get(0).getBuckets().size()).isEqualTo(2);
        assertThat(responseOK2.getFacetResults().get(0).getBuckets().get(0).getValue()).isEqualTo("Grp");
        assertThat(responseOK2.getFacetResults().get(0).getBuckets().get(0).getCount()).isEqualTo(1);
        assertThat(responseOK2.getFacetResults().get(0).getBuckets().get(1).getValue()).isEqualTo("Item");
        assertThat(responseOK2.getFacetResults().get(0).getBuckets().get(1).getCount()).isEqualTo(1);

        stream = given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(JsonHandler.getFromString(SEARCH_QUERY_WITH_FACET_FILTERS)).when()
            .get("/units").then()
            .statusCode(Status.FOUND.getStatusCode()).extract().asInputStream();

        RequestResponseOK<JsonNode> responseOK3 = JsonHandler.getFromInputStream(stream, RequestResponseOK.class);
        assertThat(responseOK3.getFacetResults().size()).isEqualTo(1);
        assertThat(responseOK3.getFacetResults().get(0).getName()).isEqualTo("filters_facet");
        assertThat(responseOK3.getFacetResults().get(0).getBuckets().size()).isEqualTo(2);
        assertThat(responseOK3.getFacetResults().get(0).getBuckets().get(0).getValue()).isEqualTo("AccessRules");
        assertThat(responseOK3.getFacetResults().get(0).getBuckets().get(0).getCount()).isEqualTo(1);
        assertThat(responseOK3.getFacetResults().get(0).getBuckets().get(1).getValue()).isEqualTo("StorageRules");
        assertThat(responseOK3.getFacetResults().get(0).getBuckets().get(1).getCount()).isEqualTo(1);

        stream = given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(JsonHandler.getFromString(SEARCH_QUERY_WITH_FACET_TERMS_INVALID_ORDER)).when()
            .get("/units").then()
            .statusCode(Status.BAD_REQUEST.getStatusCode()).extract().asInputStream();

        VitamError responseKO1 = JsonHandler.getFromInputStream(stream, VitamError.class);
        assertThat(responseKO1.getHttpCode()).isEqualTo(Status.BAD_REQUEST.getStatusCode());

        stream = given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(JsonHandler.getFromString(SEARCH_QUERY_WITH_FACET_TERMS_INVALID_SIZE)).when()
            .get("/units").then()
            .statusCode(Status.BAD_REQUEST.getStatusCode()).extract().asInputStream();

        VitamError responseKO2 = JsonHandler.getFromInputStream(stream, VitamError.class);
        assertThat(responseKO2.getHttpCode()).isEqualTo(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void given_2units_insert_when_searchUnitsByIDWithRule_thenReturn_Found() throws Exception {

        with()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(bulkInsertRequest(DATA_0)).when()
            .post("/units/bulk").then()
            .statusCode(Status.CREATED.getStatusCode());

        with()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(bulkInsertRequest(GUID_0, DATA_1)).when()
            .post("/units/bulk").then()
            .statusCode(Status.CREATED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(JsonHandler.getFromString(SEARCH_QUERY_WITH_RULE)).when()
            .get("/units/" + GUID_1).then()
            .statusCode(Status.FOUND.getStatusCode());
    }


    @Test(expected = InvalidParseOperationException.class)
    public void given_emptyQuery_when_SelectByID_thenReturn_Bad_Request() throws InvalidParseOperationException {

        given()
            .contentType(ContentType.JSON)
            .body(JsonHandler.getFromString(""))
            .when()
            .get("/units/" + GUID_0)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void given_bad_header_when_SelectByID_thenReturn_Not_allowed() throws InvalidParseOperationException {

        given()
            .contentType(ContentType.JSON)
            .body(JsonHandler.getFromString(SEARCH_QUERY))
            .when()
            .post("/units/" + GUID_0)
            .then()
            .statusCode(Status.METHOD_NOT_ALLOWED.getStatusCode());
    }

    @Test
    public void shouldReturn_Bad_Request_If_DocumentIsTooLarge() throws Exception {
        final int limitRequest = GlobalDatasParser.limitRequest;
        GlobalDatasParser.limitRequest = 99;
        given()
            .contentType(ContentType.JSON)
            .body(bulkInsertRequest(createJsonStringWithDepth(101))).when()
            .post("/units/" + GUID_0).then()
            .statusCode(Status.METHOD_NOT_ALLOWED.getStatusCode());
        GlobalDatasParser.limitRequest = limitRequest;
    }

    @Test
    public void shouldReturnErrorRequestBadRequestIfDocumentIsTooLarge() throws Exception {
        final int limitRequest = GlobalDatasParser.limitRequest;
        GlobalDatasParser.limitRequest = 99;
        given()
            .contentType(ContentType.JSON)
            .body(bulkInsertRequest(createJsonStringWithDepth(101))).when()
            .post("/units/" + GUID_0).then()
            .statusCode(Status.METHOD_NOT_ALLOWED.getStatusCode());
        GlobalDatasParser.limitRequest = limitRequest;
    }


    @Test(expected = InvalidParseOperationException.class)
    public void shouldRaiseErrorOnBadRequest() throws Exception {
        given()
            .contentType(ContentType.JSON)
            .body(bulkInsertRequest("lkvhvgvuyqvkvj")).when()
            .get("/units/" + GUID_0).then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void given_badRequestHHtp_when_selectUnitsWithInheritedRules_thenReturn_BAD_REQUEST() {
        given()
            .contentType(ContentType.JSON)
            .when()
            .get("/unitsWithInheritedRules")
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void given_Bad_Request_when_SelectUnitsWithInheritedRules_thenReturn_Bad_Request() {

        given()
            .contentType(ContentType.JSON)
            .body(BAD_QUERY_TEST)
            .when()
            .get("/unitsWithInheritedRules")
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void given_emptyQuery_when_SelectUnitsWithInheritedRules_thenReturn_BadRequest() {

        given()
            .contentType(ContentType.JSON)
            .body("")
            .when()
            .get("/unitsWithInheritedRules")
            .then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void given_2units_insert_when_searchUnitsWithInheritedRules_thenReturn_Found() throws Exception {

        with()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(bulkInsertRequest(DATA_0)).when()
            .post("/units/bulk").then()
            .statusCode(Status.CREATED.getStatusCode());

        with()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(bulkInsertRequest(GUID_0, DATA_1)).when()
            .post("/units/bulk").then()
            .statusCode(Status.CREATED.getStatusCode());

        InputStream stream = given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(JsonHandler.getFromString(SEARCH_QUERY_BY_GUID_1)).when()
            .get("/unitsWithInheritedRules").then()
            .statusCode(Status.FOUND.getStatusCode())
            .extract().asInputStream();
        RequestResponseOK<JsonNode> responseOK =
            JsonHandler.getFromInputStream(stream, RequestResponseOK.class, JsonNode.class);

        assertThat(responseOK.getResults()).hasSize(1);
        JsonNode unit1 = responseOK.getResults().get(0);
        // Ensure inherited rules are exported
        assertThat(unit1.get("InheritedRules").get("StorageRule").size()).isGreaterThan(0);
    }

    @Test
    @RunWithCustomExecutor
    public void given_2units_insert_when_searchUnitsWithInheritedRulesWithFacet_thenReturn_Facet() throws Exception {

        with()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(bulkInsertRequest(DATA_1)).when()
            .post("/units/bulk").then()
            .statusCode(Status.CREATED.getStatusCode());

        with()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(bulkInsertRequest(DATA_0)).when()
            .post("/units/bulk").then()
            .statusCode(Status.CREATED.getStatusCode());

        InputStream stream = given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(JsonHandler.getFromString(SEARCH_QUERY_WITH_FACET_MGT)).when()
            .get("/unitsWithInheritedRules").then()
            .statusCode(Status.FOUND.getStatusCode()).extract().asInputStream();

        RequestResponseOK<JsonNode> responseOK1 = JsonHandler.getFromInputStream(stream, RequestResponseOK.class);
        assertThat(responseOK1.getFacetResults().size()).isEqualTo(1);
        assertThat(responseOK1.getFacetResults().get(0).getName()).isEqualTo("mgt_facet");
        assertThat(responseOK1.getFacetResults().get(0).getBuckets().size()).isEqualTo(1);
        assertThat(responseOK1.getFacetResults().get(0).getBuckets().get(0).getValue()).isEqualTo("str0");
        assertThat(responseOK1.getFacetResults().get(0).getBuckets().get(0).getCount()).isEqualTo(1);
    }
    
    @Test
    @RunWithCustomExecutor
    public void given_2units_insert_when_twoBulkSearchValid_thenReturn_TwoResults() throws Exception {


        with()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(bulkInsertRequest(DATA_0)).when()
            .post("/units/bulk").then()
            .statusCode(Status.CREATED.getStatusCode());
        
        with()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(bulkInsertRequest(DATA_1)).when()
            .post("/units/bulk").then()
            .statusCode(Status.CREATED.getStatusCode());

        List<JsonNode> bulkSearch = new ArrayList<JsonNode>();
        bulkSearch.add(JsonHandler.getFromString(SEARCH_QUERY_BY_GUID_0));
        bulkSearch.add(JsonHandler.getFromString(SEARCH_QUERY_BY_GUID_1));
        
        InputStream stream = given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(bulkSearch).when()
            .get("/units/bulk").then()
            .statusCode(Status.FOUND.getStatusCode()).extract().asInputStream();

        RequestResponseOK<JsonNode> responseVitam = JsonHandler.getFromInputStream(stream, RequestResponseOK.class, JsonNode.class);
        assertThat(responseVitam.isOk()).isTrue();
        assertThat(responseVitam.getHttpCode()).isEqualTo(Status.FOUND.getStatusCode());
        assertThat(responseVitam.getResults()).hasSize(2);
        RequestResponseOK<JsonNode> firstResultVitam = RequestResponseOK.getFromJsonNode(responseVitam.getResults().get(0));
        assertThat(firstResultVitam.getFirstResult().get("#id").asText()).isEqualTo(GUID_0);
        RequestResponseOK<JsonNode> secondResultVitam = RequestResponseOK.getFromJsonNode(responseVitam.getResults().get(1));
        assertThat(secondResultVitam.getFirstResult().get("#id").asText()).isEqualTo(GUID_1);
        
    }
    
    @Test
    @RunWithCustomExecutor
    public void given_1unit_insert_when_twoValidQueriesBulkSelect_thenReturn_OneResultEmpty() throws Exception {

        with()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(bulkInsertRequest(DATA_0)).when()
            .post("/units/bulk").then()
            .statusCode(Status.CREATED.getStatusCode());

        List<JsonNode> bulkSearch = new ArrayList<JsonNode>();
        bulkSearch.add(JsonHandler.getFromString(SEARCH_QUERY_BY_GUID_0));
        bulkSearch.add(JsonHandler.getFromString(SEARCH_QUERY_BY_GUID_1));
        
        InputStream stream = given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(bulkSearch).when()
            .get("/units/bulk").then()
            .statusCode(Status.FOUND.getStatusCode()).extract().asInputStream();

        RequestResponseOK<JsonNode> responseVitam = JsonHandler.getFromInputStream(stream, RequestResponseOK.class, JsonNode.class);
        assertThat(responseVitam.isOk()).isTrue();
        assertThat(responseVitam.getHttpCode()).isEqualTo(Status.FOUND.getStatusCode());
        assertThat(responseVitam.getResults()).hasSize(2);
        RequestResponseOK<JsonNode> firstResultVitam = RequestResponseOK.getFromJsonNode(responseVitam.getResults().get(0));
        assertThat(firstResultVitam.getFirstResult().get("#id").asText()).isEqualTo(GUID_0);
        RequestResponseOK<JsonNode> secondResultVitam = RequestResponseOK.getFromJsonNode(responseVitam.getResults().get(1));
        assertThat(secondResultVitam.getResults().size()).isEqualTo(0);
        
    }
    
    @Test
    @RunWithCustomExecutor
    public void given_1unit_insert_when_noQueryBulkSelect_thenReturn_ZeroResults() throws Exception {

        with()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(bulkInsertRequest(DATA_0)).when()
            .post("/units/bulk").then()
            .statusCode(Status.CREATED.getStatusCode());

        List<JsonNode> bulkSearch = new ArrayList<JsonNode>();
        
        InputStream stream = given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(bulkSearch).when()
            .get("/units/bulk").then()
            .statusCode(Status.FOUND.getStatusCode()).extract().asInputStream();

        RequestResponseOK<JsonNode> responseVitam = JsonHandler.getFromInputStream(stream, RequestResponseOK.class, JsonNode.class);
        assertThat(responseVitam.isOk()).isTrue();
        assertThat(responseVitam.getHttpCode()).isEqualTo(Status.FOUND.getStatusCode());
        assertThat(responseVitam.getResults()).hasSize(0);
        
    }
    
    @Test
    @RunWithCustomExecutor
    public void given_1unit_insert_when_oneBadQueryDSLOneValidDSLBulkSelect_thenReturn_BadQueryResultAndValidResult() throws Exception {
        with()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(bulkInsertRequest(DATA_0)).when()
            .post("/units/bulk").then()
            .statusCode(Status.CREATED.getStatusCode());

        List<JsonNode> bulkSearch = new ArrayList<JsonNode>();
        bulkSearch.add(JsonHandler.getFromString(BAD_QUERY_DSL_TEST));
        bulkSearch.add(JsonHandler.getFromString(SEARCH_QUERY_BY_GUID_0));
        
        InputStream stream = given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(bulkSearch).when()
            .get("/units/bulk").then()
            .statusCode(Status.FOUND.getStatusCode()).extract().asInputStream();

        RequestResponseOK<JsonNode> responseVitam = JsonHandler.getFromInputStream(stream, RequestResponseOK.class, JsonNode.class);
        assertThat(responseVitam.isOk()).isTrue();
        assertThat(responseVitam.getHttpCode()).isEqualTo(Status.FOUND.getStatusCode());
        assertThat(responseVitam.getResults()).hasSize(2);
        VitamError firstResultVitam = VitamError.getFromJsonNode(responseVitam.getResults().get(0));
        assertThat(firstResultVitam.getHttpCode()).isEqualTo(Status.BAD_REQUEST.getStatusCode());
        RequestResponseOK<JsonNode> secondResultVitam = RequestResponseOK.getFromJsonNode(responseVitam.getResults().get(1));
        assertThat(secondResultVitam.getFirstResult().get("#id").asText()).isEqualTo(GUID_0);
        
    }
    

    @Test
    @RunWithCustomExecutor
    public void given_no_insert_when_badBodyBulkSelect_thenReturn_Error() throws Exception {

        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(BAD_QUERY_TEST).when()
            .get("/units/bulk").then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
        
        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(SEARCH_QUERY_BY_GUID_0).when()
            .get("/units/bulk").then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
        
    }
    

    @Test
    @RunWithCustomExecutor
    public void given_no_insert_when_badHeaderBulkSelect_thenReturn_Error() throws Exception {

        List<JsonNode> bulkSearch = new ArrayList<JsonNode>();
        bulkSearch.add(JsonHandler.getFromString(SEARCH_QUERY_BY_GUID_0));
        
        given()
            .contentType(ContentType.JSON)
            .body(bulkSearch).when()
            .get("/units/bulk").then()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());
        
        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, "TOTO")
            .body(bulkSearch).when()
            .get("/units/bulk").then()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());
        
    }
}
