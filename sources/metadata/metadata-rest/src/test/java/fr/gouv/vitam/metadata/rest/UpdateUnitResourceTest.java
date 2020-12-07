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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.client.ClientMockResultHelper;
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
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.OntologyModel;
import fr.gouv.vitam.common.model.config.CollectionConfiguration;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.ontology.OntologyTestHelper;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.metadata.core.config.DefaultCollectionConfiguration;
import fr.gouv.vitam.metadata.core.config.ElasticsearchMetadataIndexManager;
import fr.gouv.vitam.metadata.core.config.MetadataIndexationConfiguration;
import fr.gouv.vitam.metadata.core.config.MetaDataConfiguration;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollectionsTestUtils;
import fr.gouv.vitam.metadata.core.mapping.MappingLoader;
import fr.gouv.vitam.metadata.api.model.BulkUnitInsertEntry;
import fr.gouv.vitam.metadata.api.model.BulkUnitInsertRequest;
import fr.gouv.vitam.metadata.core.database.collections.ElasticsearchAccessMetadata;
import fr.gouv.vitam.metadata.core.database.collections.MongoDbAccessMetadataImpl;
import fr.gouv.vitam.metadata.core.model.UpdateUnit;
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

import static fr.gouv.vitam.metadata.core.database.collections.MetadataCollections.UNIT;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.with;
import static org.assertj.core.api.Assertions.assertThat;

public class UpdateUnitResourceTest {

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    private static final Integer TENANT_ID_0 = 0;
    private static final List<Integer> tenantList = Collections.singletonList(TENANT_ID_0);
    private static final MappingLoader mappingLoader = MappingLoaderTestUtils.getTestMappingLoader();
    private static final ElasticsearchMetadataIndexManager indexManager = MetadataCollectionsTestUtils
        .createTestIndexManager(tenantList, Collections.emptyMap(), mappingLoader);

    private static final String ID_UNIT = "aeaqaaaaaeaaaaakaarp4akuuf2ldmyaaaap";
    private static final String ID_UNIT_1 = "aeaqaaaaaeaaaaakaarp4akuuf2ldmyaaaaq";
    private static final String ID_UNIT_2 = "aeaqaaaaaeaaaaakaarp4akuuf2ldmyaaaab";

    private static final String DATA =
        "{ \"_id\": \"" + ID_UNIT + "\", \"_tenant\": 0, " + "\"data\": \"data\" }";

    private static final String DATA1 =
        "{ \"_id\": \"" + ID_UNIT_1 + "\", \"_tenant\": 0, " + "\"data\": \"data1\"," +
                "\"Title\": \"Archive1\", \"_mgt\": {\"NeedAuthorization\": true}," +
                " \"DescriptionLevel\": \"Item\" }";

    private static final String DATA2 =
        "{ \"_id\": \"" + ID_UNIT_2 + "\", \"_tenant\": 0, " + "\"data\": \"data2\", " +
            "\"Title\": \"Archive3\", \"_mgt\": {\"NeedAuthorization\": true}," +
            " \"DescriptionLevel\": \"Item\" }";

    private static final String DATA_URI = "/metadata/v1";
    private static final String JETTY_CONFIG = "jetty-config-test.xml";

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();
    private final static String HOST_NAME = "127.0.0.1";
    private static final Integer TENANT_ID = 0;

    private static final String BODY_TEST =
        "{\"$query\": [], \"$action\": [{\"$set\": {\"data\": \"data3\"}}], \"$filter\": {}}";

    private static final String BODY_TEST_BAD_REQUEST =
        "{\"$query\": [{ \"#id\": \"aeaqaaaaaeaaaaakaarp4akuuf2ldmyaaa22\"}], \"$action\": [{\"$set\": {\"data\": \"data3\"}}], \"$filter\": {}}";

    private static final String REAL_UPDATE_BODY_TEST =
        "{\"$query\": [], \"$action\": [{\"$set\": {\"data\": \"data4\"}}, {\"$push\": {\"#operations\": [\"aeaqaaaaaeaaaaakaarp4akuuf2ldmyaaaac\"]}}], \"$filter\": {}}";


    // Bulk

    private static final String BODY_BULK_TEST_SIMPLE_GOOD_REQUEST =
            "[{\"$roots\": [\""+ ID_UNIT_1 + "\"], \"$action\": [{\"$set\": {\"data\": \"data10\"}}], \"$filter\": {}}]";

    private static final String BODY_BULK_TEST_BAD_BODY_REQUEST =
            "[{\"$roots\": [\""+ ID_UNIT_1 + "\"], \"$action\": [{\"$test\": {\"data\": \"data10\"}}], \"$filter\": {}}]";

    private static final String BODY_BULK_TEST_TWO_GOOD_REQUESTS =
            "[{\"$roots\": [\""+ ID_UNIT_1 + "\"], \"$action\": [{\"$set\": {\"data\": \"data10\"}}], \"$filter\": {}}," +
                    "{\"$roots\": [\"" + ID_UNIT_2 + "\"], \"$action\": [{\"$set\": {\"Title\": \"Title5\"}}, {\"$set\": {\"data\": \"data5\"}}], \"$filter\": {}}]";

    private static final String BODY_BULK_TEST_ONE_BAD_REQUEST_ON_TWO =
            "[{\"$roots\": [\""+ ID_UNIT + "\"], \"$action\": [{\"$set\": {\"data\": \"data10\"}}], \"$filter\": {}}," +
                    "{\"$roots\": [\"" + ID_UNIT_2 + "\"], \"$action\": [{\"$set\": {\"Title\": \"Title5\"}}, {\"$set\": {\"data\": \"data5\"}}], \"$filter\": {}}]";

    private static final String BODY_BULK_TEST_TWO_BAD_REQUEST_AND_AN_EMPTY_ONE =
            "[{\"$roots\": [\""+ ID_UNIT + "\"], \"$action\": [{\"$set\": {\"data\": \"data10\"}}], \"$filter\": {}}," +
                    "{\"$roots\": [], \"$action\": [{\"$set\": {\"Title\": \"Title5\"}}, {\"$set\": {\"data\": \"data5\"}}], \"$filter\": {}}," +
                    "{\"$roots\": \"test\", \"$action\": [{\"$set\": {\"Title\": \"Title5\"}}, {\"$set\": {\"data\": \"data5\"}}], \"$filter\": {}}]";

    private static JunitHelper junitHelper;
    private static int serverPort;
    @ClassRule
    public static MongoRule mongoRule =
        new MongoRule(MongoDbAccessMetadataImpl.getMongoClientOptions());

    private static MetadataMain application;
    private static ElasticsearchAccessMetadata esClient;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        junitHelper = JunitHelper.getInstance();

        List<ElasticsearchNode> esNodes =
            Lists.newArrayList(new ElasticsearchNode(ElasticsearchRule.getHost(), ElasticsearchRule.getPort()));

        esClient = new ElasticsearchAccessMetadata(ElasticsearchRule.VITAM_CLUSTER, esNodes,
            indexManager);

        MetadataCollectionsTestUtils.beforeTestClass(mongoRule.getMongoDatabase(), GUIDFactory.newGUID().getId(),
            esClient);
        final List<MongoDbNode> mongo_nodes = new ArrayList<>();
        mongo_nodes.add(new MongoDbNode(HOST_NAME, mongoRule.getDataBasePort()));
        final MetaDataConfiguration configuration =
            new MetaDataConfiguration(mongo_nodes, MongoRule.VITAM_DB, ElasticsearchRule.VITAM_CLUSTER, esNodes, mappingLoader);
        configuration.setJettyConfig(JETTY_CONFIG);
        configuration.setUrlProcessing("http://processing.service.consul:8203/");

        configuration.setIndexationConfiguration(new MetadataIndexationConfiguration()
            .setDefaultCollectionConfiguration(new DefaultCollectionConfiguration()
                .setUnit(new CollectionConfiguration(2, 1))
                .setObjectgroup(new CollectionConfiguration(2, 1))));

        VitamConfiguration.setTenants(tenantList);
        serverPort = junitHelper.findAvailablePort();
        File configurationFile = tempFolder.newFile();

        PropertiesUtils.writeYaml(configurationFile, configuration);

        application = new MetadataMain(configurationFile.getAbsolutePath());

        application.start();
        JunitHelper.unsetJettyPortSystemProperty();

        RestAssured.port = serverPort;
        RestAssured.basePath = DATA_URI;

        ClientMockResultHelper.setOntologies(JsonHandler.getFromInputStreamAsTypeReference(
            OntologyTestHelper.loadOntologies(), new TypeReference<List<OntologyModel>>() {
            }));
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

        ClientMockResultHelper.resetOntologies();
    }

    @After
    public void tearDown() {
        MetadataCollectionsTestUtils.afterTest(indexManager);
    }

    private static final BulkUnitInsertRequest bulkInsertRequest(String data) throws InvalidParseOperationException {
        return new BulkUnitInsertRequest(Collections.singletonList(
            new BulkUnitInsertEntry(Collections.emptySet(), JsonHandler.getFromString(data))
        ));
    }

    private static final BulkUnitInsertRequest bulkInsertRequest(String data, String root)
        throws InvalidParseOperationException {
        return new BulkUnitInsertRequest(Collections.singletonList(
            new BulkUnitInsertEntry(Collections.singleton(root), JsonHandler.getFromString(data))
        ));
    }

    private static String createJsonStringWithDepth(int depth) {
        if (depth == 0) {
            return " \"b\" ";
        }
        return "{ \"a\": " + createJsonStringWithDepth(depth - 1) + "}";
    }

    // Unit by ID (request and uri)

    @Test
    @RunWithCustomExecutor
    public void given_2units_insert_when_UpdateUnitsByID_thenReturn_Found() throws Exception {
        with()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID_0)
            .header(GlobalDataRest.X_REQUEST_ID, GUIDFactory.newRequestIdGUID(TENANT_ID).toString())
            .body(bulkInsertRequest(DATA2)).when()
            .post("/units/bulk").then()
            .statusCode(Status.CREATED.getStatusCode());

        with()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID_0)
            .header(GlobalDataRest.X_REQUEST_ID, GUIDFactory.newRequestIdGUID(TENANT_ID).toString())
            .body(bulkInsertRequest(DATA, ID_UNIT_2)).when()
            .post("/units/bulk").then()
            .statusCode(Status.CREATED.getStatusCode());

        esClient.refreshIndex(UNIT, TENANT_ID_0);

        given()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_REQUEST_ID, GUIDFactory.newRequestIdGUID(TENANT_ID).toString())
            .contentType(ContentType.JSON)
            .body(JsonHandler.getFromString(BODY_TEST)).when()
            .put("/units/" + ID_UNIT_2).then()
            .statusCode(Status.OK.getStatusCode());
        esClient.refreshIndex(UNIT, TENANT_ID_0);

        given()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_REQUEST_ID, GUIDFactory.newRequestIdGUID(TENANT_ID).toString())
            .contentType(ContentType.JSON)
            .body(JsonHandler.getFromString(REAL_UPDATE_BODY_TEST)).when()
            .put("/units/" + ID_UNIT_2).then()
            .statusCode(Status.OK.getStatusCode());
    }

    @Test(expected = InvalidParseOperationException.class)
    public void given_emptyQuery_when_UpdateByID_thenReturn_Bad_Request() throws InvalidParseOperationException {

        given()
            .contentType(ContentType.JSON)
            .body(JsonHandler.getFromString(""))
            .when()
            .put("/units/" + ID_UNIT_2)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void given_bad_header_when_UpdateByID_thenReturn_Bad_Request() throws InvalidParseOperationException {

        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_REQUEST_ID, GUIDFactory.newRequestIdGUID(TENANT_ID).toString())
            .body(JsonHandler.getFromString(BODY_TEST_BAD_REQUEST))
            .when()
            .put("/units/" + ID_UNIT_2)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void shouldReturn_REQUEST_ENTITY_TOO_LARGE_If_DocumentIsTooLarge() throws Exception {
        final int limitRequest = GlobalDatasParser.limitRequest;
        GlobalDatasParser.limitRequest = 99;
        given()
            .contentType(ContentType.JSON)
            .body(bulkInsertRequest(createJsonStringWithDepth(101))).when()
            .put("/units/" + ID_UNIT_2).then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
        GlobalDatasParser.limitRequest = limitRequest;
    }

    @Test(expected = InvalidParseOperationException.class)
    public void shouldReturnErrorRequestBadRequest() throws Exception {
        given()
            .contentType(ContentType.JSON)
            .body(bulkInsertRequest("lkvhvgvuyqvkvj")).when()
            .put("/units/" + ID_UNIT_2).then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    // Bulk update
    @Test
    @RunWithCustomExecutor
    public void given_correct_units_insert_when_twoUpdateBulkByIDValid_thenReturn_OK() throws Exception {
        with()
                .contentType(ContentType.JSON)
                .header(GlobalDataRest.X_TENANT_ID, TENANT_ID_0)
                .header(GlobalDataRest.X_REQUEST_ID, GUIDFactory.newRequestIdGUID(TENANT_ID).toString())
                .body(bulkInsertRequest(DATA2)).when()
                .post("/units/bulk").then()
                .statusCode(Status.CREATED.getStatusCode());

        with()
                .contentType(ContentType.JSON)
                .header(GlobalDataRest.X_TENANT_ID, TENANT_ID_0)
                .header(GlobalDataRest.X_REQUEST_ID, GUIDFactory.newRequestIdGUID(TENANT_ID).toString())
                .body(bulkInsertRequest(DATA1, ID_UNIT_2)).when()
                .post("/units/bulk").then()
                .statusCode(Status.CREATED.getStatusCode());

        esClient.refreshIndex(UNIT, TENANT_ID_0);

        InputStream stream = given()
                .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
                .header(GlobalDataRest.X_REQUEST_ID, GUIDFactory.newRequestIdGUID(TENANT_ID).toString())
                .contentType(ContentType.JSON)
                .body(JsonHandler.getFromString(BODY_BULK_TEST_TWO_GOOD_REQUESTS)).when()
                .post("/units/atomicupdatebulk").then()
                .statusCode(Status.OK.getStatusCode()).extract().asInputStream();

        // We get a RequestResponseOK object, either updates fail or succeed
        RequestResponseOK<JsonNode> responseOK = JsonHandler.getFromInputStream(stream, RequestResponseOK.class, JsonNode.class);
        List<JsonNode> results = responseOK.getResults();

        // We get one result per request
        assertThat(results.size()).isEqualTo(2);

        // We get one hit for each request which succeeded
        RequestResponseOK<UpdateUnit> firstResponse = RequestResponseOK.getFromJsonNode(results.get(0), UpdateUnit.class);
        assertThat(firstResponse.getHits().getSize()).isEqualTo(1);

        // The response contains the id of the updated object
        List<UpdateUnit> resultsFirstResponse = firstResponse.getResults();
        assertThat(resultsFirstResponse.size()).isEqualTo(1);
        assertThat(resultsFirstResponse.get(0).getStatus()).isEqualTo(StatusCode.OK);
        assertThat(resultsFirstResponse.get(0).getUnitId()).isEqualTo(ID_UNIT_1);

        // Same for the second request
        RequestResponseOK secondResponse = RequestResponseOK.getFromJsonNode(results.get(1), UpdateUnit.class);
        assertThat(secondResponse.getHits().getSize()).isEqualTo(1);

        List<UpdateUnit> resultsSecondResponse = secondResponse.getResults();
        assertThat(resultsSecondResponse.size()).isEqualTo(1);
        assertThat(resultsSecondResponse.get(0).getStatus()).isEqualTo(StatusCode.OK);
        assertThat(resultsSecondResponse.get(0).getUnitId()).isEqualTo(ID_UNIT_2);
    }

    @Test
    @RunWithCustomExecutor
    public void given_correct_units_insert_when_twoUpdateBulkByID_with_oneInvalid_thenReturn_OK() throws Exception {
        with()
                .contentType(ContentType.JSON)
                .header(GlobalDataRest.X_TENANT_ID, TENANT_ID_0)
                .header(GlobalDataRest.X_REQUEST_ID, GUIDFactory.newRequestIdGUID(TENANT_ID).toString())
                .body(bulkInsertRequest(DATA2)).when()
                .post("/units/bulk").then()
                .statusCode(Status.CREATED.getStatusCode());

        with()
                .contentType(ContentType.JSON)
                .header(GlobalDataRest.X_TENANT_ID, TENANT_ID_0)
                .header(GlobalDataRest.X_REQUEST_ID, GUIDFactory.newRequestIdGUID(TENANT_ID).toString())
                .body(bulkInsertRequest(DATA, ID_UNIT_2)).when()
                .post("/units/bulk").then()
                .statusCode(Status.CREATED.getStatusCode());

        esClient.refreshIndex(UNIT, TENANT_ID_0);

        InputStream stream = given()
                .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
                .header(GlobalDataRest.X_REQUEST_ID, GUIDFactory.newRequestIdGUID(TENANT_ID).toString())
                .contentType(ContentType.JSON)
                .body(JsonHandler.getFromString(BODY_BULK_TEST_ONE_BAD_REQUEST_ON_TWO)).when()
                .post("/units/atomicupdatebulk").then()
                .statusCode(Status.OK.getStatusCode()).extract().asInputStream();

        // Global response is OK
        RequestResponseOK<JsonNode> responseOK = JsonHandler.getFromInputStream(stream, RequestResponseOK.class, JsonNode.class);
        List<JsonNode> results = responseOK.getResults();

        assertThat(results.size()).isEqualTo(2);

        // First update returns RequestResponseOK
        RequestResponseOK<UpdateUnit> firstResponse = RequestResponseOK.getFromJsonNode(results.get(0), UpdateUnit.class);
        assertThat(firstResponse.getHits().getSize()).isEqualTo(1);

        // First update is KO (in UpdateUnit) with a schema Error
        List<UpdateUnit> resultsFirstResponse = firstResponse.getResults();
        assertThat(resultsFirstResponse.size()).isEqualTo(1);
        assertThat(resultsFirstResponse.get(0).getStatus()).isEqualTo(StatusCode.KO);
        assertThat(resultsFirstResponse.get(0).getUnitId()).isEqualTo(ID_UNIT);

        // Second update returns RequestResponseOK
        RequestResponseOK secondResponse = RequestResponseOK.getFromJsonNode(results.get(1), UpdateUnit.class);
        assertThat(secondResponse.getHits().getSize()).isEqualTo(1);

        // First update is OK (in UpdateUnit)
        List<UpdateUnit> resultsSecondResponse = secondResponse.getResults();
        assertThat(resultsSecondResponse.size()).isEqualTo(1);
        assertThat(resultsSecondResponse.get(0).getStatus()).isEqualTo(StatusCode.OK);
        assertThat(resultsSecondResponse.get(0).getUnitId()).isEqualTo(ID_UNIT_2);
    }

    @Test
    @RunWithCustomExecutor
    public void given_correct_units_insert_when_threeUpdateBulkByID_with_twoInvalid_andOneEmpty_thenReturn_OK() throws Exception {
        with()
                .contentType(ContentType.JSON)
                .header(GlobalDataRest.X_TENANT_ID, TENANT_ID_0)
                .header(GlobalDataRest.X_REQUEST_ID, GUIDFactory.newRequestIdGUID(TENANT_ID).toString())
                .body(bulkInsertRequest(DATA2)).when()
                .post("/units/bulk").then()
                .statusCode(Status.CREATED.getStatusCode());

        with()
                .contentType(ContentType.JSON)
                .header(GlobalDataRest.X_TENANT_ID, TENANT_ID_0)
                .header(GlobalDataRest.X_REQUEST_ID, GUIDFactory.newRequestIdGUID(TENANT_ID).toString())
                .body(bulkInsertRequest(DATA, ID_UNIT_2)).when()
                .post("/units/bulk").then()
                .statusCode(Status.CREATED.getStatusCode());

        esClient.refreshIndex(UNIT, TENANT_ID_0);

        InputStream stream = given()
                .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
                .header(GlobalDataRest.X_REQUEST_ID, GUIDFactory.newRequestIdGUID(TENANT_ID).toString())
                .contentType(ContentType.JSON)
                .body(JsonHandler.getFromString(BODY_BULK_TEST_TWO_BAD_REQUEST_AND_AN_EMPTY_ONE)).when()
                .post("/units/atomicupdatebulk").then()
                .statusCode(Status.OK.getStatusCode()).extract().asInputStream();

        // Global response is OK
        RequestResponseOK<JsonNode> responseOK = JsonHandler.getFromInputStream(stream, RequestResponseOK.class, JsonNode.class);
        List<JsonNode> results = responseOK.getResults();

        assertThat(results.size()).isEqualTo(3);

        // First update returns RequestResponseOK
        RequestResponseOK<UpdateUnit> firstResponse = RequestResponseOK.getFromJsonNode(results.get(0), UpdateUnit.class);
        assertThat(firstResponse.getHits().getSize()).isEqualTo(1);

        // First update is KO (in UpdateUnit), with a schema Error
        List<UpdateUnit> resultsFirstResponse = firstResponse.getResults();
        assertThat(resultsFirstResponse.size()).isEqualTo(1);
        assertThat(resultsFirstResponse.get(0).getStatus()).isEqualTo(StatusCode.KO);
        assertThat(resultsFirstResponse.get(0).getUnitId()).isEqualTo(ID_UNIT);

        // Second update returns RequestResponseOK
        RequestResponseOK secondResponse = RequestResponseOK.getFromJsonNode(results.get(1), UpdateUnit.class);
        assertThat(secondResponse.getHits().getSize()).isEqualTo(0);

        // Here, there is no update (no roots)
        List<UpdateUnit> resultsSecondResponse = secondResponse.getResults();
        assertThat(resultsSecondResponse.size()).isEqualTo(0);

        // Third update returns a Vitam Error, with a Bad Request error code (bad body request)
        VitamError thirdResponse = VitamError.getFromJsonNode(results.get(2));
        assertThat(thirdResponse.getStatus()).isEqualTo(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void given_no_insert_when_badBodyBulkUpdate_thenReturn_Error() throws Exception {
        InputStream stream = given()
                .contentType(ContentType.JSON)
                .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
                .body(BODY_BULK_TEST_BAD_BODY_REQUEST).when()
                .post("/units/atomicupdatebulk").then()
                .statusCode(Status.OK.getStatusCode()).extract().asInputStream();

        // Global response is OK
        RequestResponseOK<JsonNode> responseOK = JsonHandler.getFromInputStream(stream, RequestResponseOK.class, JsonNode.class);

        List<JsonNode> results = responseOK.getResults();

        assertThat(results.size()).isEqualTo(1);

        // Single update returns a Vitam Error, with a Bad Request error code (bad body request)
        VitamError firstResponse = VitamError.getFromJsonNode(results.get(0));
        assertThat(firstResponse.getStatus()).isEqualTo(Status.BAD_REQUEST.getStatusCode());

        stream = given()
                .contentType(ContentType.JSON)
                .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
                .body(BODY_BULK_TEST_SIMPLE_GOOD_REQUEST).when()
                .post("/units/atomicupdatebulk").then()
                .statusCode(Status.OK.getStatusCode()).extract().asInputStream();

        // Global response is OK
        RequestResponseOK<JsonNode> responseOK2 = JsonHandler.getFromInputStream(stream, RequestResponseOK.class, JsonNode.class);
        List<JsonNode> results2 = responseOK2.getResults();

        assertThat(results2.size()).isEqualTo(1);

        // First update returns RequestResponseOK
        RequestResponseOK<UpdateUnit> firstResponse2 = RequestResponseOK.getFromJsonNode(results2.get(0), UpdateUnit.class);
        assertThat(firstResponse2.getHits().getSize()).isEqualTo(1);

        // First update is KO (in UpdateUnit), with root not found
        List<UpdateUnit> resultsFirstResponse2 = firstResponse2.getResults();
        assertThat(resultsFirstResponse2.size()).isEqualTo(1);
        assertThat(resultsFirstResponse2.get(0).getStatus()).isEqualTo(StatusCode.KO);
        assertThat(resultsFirstResponse2.get(0).getUnitId()).isEqualTo(ID_UNIT_1);
    }


    @Test
    @RunWithCustomExecutor
    public void given_no_insert_when_badHeaderBulkUpdate_thenReturn_Error() throws Exception {
        InputStream stream = given()
                .contentType(ContentType.JSON)
                .body(JsonHandler.getFromString(BODY_BULK_TEST_SIMPLE_GOOD_REQUEST)).when()
                .post("/units/atomicupdatebulk").then()
                .statusCode(Status.OK.getStatusCode()).extract().asInputStream();

        // Global response is OK
        RequestResponseOK<JsonNode> responseOK = JsonHandler.getFromInputStream(stream, RequestResponseOK.class, JsonNode.class);

        List<JsonNode> results = responseOK.getResults();

        assertThat(results.size()).isEqualTo(1);

        // Single update returns RequestResponseOK
        RequestResponseOK<UpdateUnit> firstResponse = RequestResponseOK.getFromJsonNode(results.get(0), UpdateUnit.class);
        assertThat(firstResponse.getHits().getSize()).isEqualTo(1);

        // Single update is FATAL (in UpdateUnit), with no tenant found
        List<UpdateUnit> resultsFirstResponse = firstResponse.getResults();
        assertThat(resultsFirstResponse.size()).isEqualTo(1);
        assertThat(resultsFirstResponse.get(0).getStatus()).isEqualTo(StatusCode.FATAL);
        assertThat(resultsFirstResponse.get(0).getUnitId()).isEqualTo(ID_UNIT_1);

        // An internal error is returned if tenant is invalid
        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, "TOTO")
            .body(JsonHandler.getFromString(BODY_BULK_TEST_SIMPLE_GOOD_REQUEST)).when()
            .post("/units/atomicupdatebulk").then()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode()).extract().asInputStream();
    }

}
