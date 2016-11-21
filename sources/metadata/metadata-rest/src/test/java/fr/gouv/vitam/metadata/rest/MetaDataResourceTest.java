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

import static com.jayway.restassured.RestAssured.get;
import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.RestAssured.with;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response.Status;
import javax.xml.bind.MarshalException;

import org.jhades.JHades;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.database.parser.request.GlobalDatasParser;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.junit.JunitHelper.ElasticsearchTestConfiguration;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.server2.application.configuration.MongoDbNode;
import fr.gouv.vitam.metadata.api.config.MetaDataConfiguration;
import fr.gouv.vitam.metadata.api.exception.MetaDataException;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;

public class MetaDataResourceTest {
    private static final String DATA =
        "{ \"#id\": \"aeaqaaaaaeaaaaakaarp4akuuf2ldmyaaaaq\", " + "\"data\": \"data1\" }";
    private static final String DATA2 =
        "{ \"#id\": \"aeaqaaaaaeaaaaakaarp4akuuf2ldmyaaaab\"," + "\"data\": \"data2\" }";
    private static final String DATA3 =
        "{ \"#id\": \"aeaqaaaaaeaaaaakaarp4akuuf2ldmyaaaaz\"," + "\"data\": \"data3\" }";

    private static final String DATA_URI = "/metadata/v1";
    private static final String DATABASE_NAME = "vitam-test";
    private static final String JETTY_CONFIG = "jetty-config-test.xml";
    private static MongodExecutable mongodExecutable;
    static MongodProcess mongod;

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    private final static String CLUSTER_NAME = "vitam-cluster";
    private final static String HOST_NAME = "127.0.0.1";

    private static final String QUERY_PATH = "{ $path :  [\"aeaqaaaaaeaaaaakaarp4akuuf2ldmyaaaaq\"]  }";
    private static final String QUERY_EXISTS = "{ $exists :  \"#id\"  }";
    private static final String QUERY_TEST =
        "{ $or : " + "[ " + "   {$exists : '#id'}, " + "   {$missing : 'mavar2'}, " + "   {$isNull : 'mavar3'}, " +
            "   { $or : [ " + "          {$in : { 'mavar4' : [1, 2, 'maval1'] }}, " +
            "          { $nin : { 'mavar5' : ['maval2', true] } } ] " + "   } " + "]}";

    private static final String SERVER_HOST = "localhost";
    private static JunitHelper junitHelper;
    private static int dataBasePort;
    private static int serverPort;

    private static File newMetadataConf;
    private static MetaDataApplication application;
    private static ElasticsearchTestConfiguration config = null;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // Identify overlapping in particular jsr311
        new JHades().overlappingJarsReport();
        // ES
        try {
            config = JunitHelper.startElasticsearchForTest(tempFolder, CLUSTER_NAME);
        } catch (VitamApplicationServerException e1) {
            assumeTrue(false);
        }
        junitHelper = JunitHelper.getInstance();

        final List<ElasticsearchNode> nodes = new ArrayList<ElasticsearchNode>();
        nodes.add(new ElasticsearchNode(HOST_NAME, config.getTcpPort()));

        dataBasePort = junitHelper.findAvailablePort();

        final MongodStarter starter = MongodStarter.getDefaultInstance();
        mongodExecutable = starter.prepare(new MongodConfigBuilder()
            .version(Version.Main.PRODUCTION)
            .net(new Net(dataBasePort, Network.localhostIsIPv6()))
            .build());
        mongod = mongodExecutable.start();

        List<MongoDbNode> mongo_nodes = new ArrayList<MongoDbNode>();
        mongo_nodes.add(new MongoDbNode(SERVER_HOST, dataBasePort));
        // TODO: using configuration file ? Why not ?
        final MetaDataConfiguration configuration =
            new MetaDataConfiguration(mongo_nodes, DATABASE_NAME, CLUSTER_NAME, nodes);
        configuration.setJettyConfig(JETTY_CONFIG);
        serverPort = junitHelper.findAvailablePort();

        application = new MetaDataApplication(configuration);
        application.start();
        JunitHelper.unsetJettyPortSystemProperty();

        RestAssured.port = serverPort;
        RestAssured.basePath = DATA_URI;
    }

    @AfterClass
    public static void tearDownAfterClass() {
        if (config == null) {
            return;
        }
        JunitHelper.stopElasticsearchForTest(config);
        try {
            application.stop();
        } catch (final Exception e) {
            // ignore
        }
        mongod.stop();
        mongodExecutable.stop();
        junitHelper.releasePort(dataBasePort);
        junitHelper.releasePort(serverPort);
    }
    
    @Before
    public void before() {
        Assume.assumeTrue("Elasticsearch not started but should", config != null);
    }

    @After
    public void tearDown() {
        MetadataCollections.C_UNIT.getCollection().drop();
    }

    private static final JsonNode buildDSLWithOptions(String query, String data) throws Exception {
        return JsonHandler.getFromString("{ $roots : [ '' ], $query : [ " + query + " ], $data : " + data + " }");
    }

    private static String createJsonStringWithDepth(int depth) {
        final StringBuilder obj = new StringBuilder();
        if (depth == 0) {
            return " \"b\" ";
        }
        obj.append("{ \"a\": ").append(createJsonStringWithDepth(depth - 1)).append("}");
        return obj.toString();
    }

    private static String generateResponseErrorFromStatus(Status status) throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(new VitamError(status.name()).setHttpCode(status.getStatusCode()).setContext("ingest").setState("code_vitam")
                .setMessage(status.getReasonPhrase()).setDescription(status.getReasonPhrase()));
    }


    /**
     * Test status endpointgivenInsertObjectGroupWithBodyIsNotCorrectThenReturnErrorBadRequest
     */
    @Test
    public void shouldGetStatusOK() throws MetaDataException, InvalidParseOperationException {
        get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());
    }

    /**
     * Test insert Unit endpoint
     */

    @Test(expected = InvalidParseOperationException.class)
    public void shouldRaiseExceptionIfBodyIsNotCorrect() throws Exception {
        given()
            .contentType(ContentType.JSON)
            .body(buildDSLWithOptions("invalid", DATA)).when()
            .post("/units").then()
            .body(equalTo(generateResponseErrorFromStatus(Status.BAD_REQUEST)))
            .statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void shouldReturnErrorConflictIfIdDuplicated() throws Exception {
        with()
            .contentType(ContentType.JSON)
            .body(buildDSLWithOptions("", DATA)).when()
            .post("/units").then()
            .statusCode(Status.CREATED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(buildDSLWithOptions("", DATA)).when()
            .post("/units").then()
            .body(equalTo(generateResponseErrorFromStatus(Status.CONFLICT)))
            .statusCode(Status.CONFLICT.getStatusCode());
    }

    @Test
    public void givenInsertUnitWithQueryPathWhenParentFoundThenReturnCreated() throws Exception {
        with()
            .contentType(ContentType.JSON)
            .body(buildDSLWithOptions("", DATA)).when()
            .post("/units").then()
            .statusCode(Status.CREATED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(buildDSLWithOptions(QUERY_PATH, DATA2)).when()
            .post("/units").then()
            .statusCode(Status.CREATED.getStatusCode());
    }

    @Test
    public void givenInsertUnitWithQueryExistsWhenParentFoundThenReturnCreated() throws Exception {
        with()
            .contentType(ContentType.JSON)
            .body(buildDSLWithOptions("", DATA)).when()
            .post("/units").then()
            .statusCode(Status.CREATED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(buildDSLWithOptions(QUERY_EXISTS, DATA2)).when()
            .post("/units").then()
            .statusCode(Status.CREATED.getStatusCode());
    }

    @Test
    public void givenInsertUnitWithQueryWhenParentFoundThenReturnCreated() throws Exception {
        with()
            .contentType(ContentType.JSON)
            .body(buildDSLWithOptions("", DATA)).when()
            .post("/units").then()
            .statusCode(Status.CREATED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(buildDSLWithOptions(QUERY_TEST, DATA2)).when()
            .post("/units").then()
            .statusCode(Status.CREATED.getStatusCode());
    }

    @Test(expected = MarshalException.class)
    public void shouldReturnErrorIfContentTypeIsNotJson() throws Exception {
        given()
            .contentType("application/xml")
            .body(buildDSLWithOptions("", DATA)).when()
            .post("/units").then()
            .statusCode(Status.UNSUPPORTED_MEDIA_TYPE.getStatusCode());
    }

    @Test
    public void shouldReturnErrorNotFoundIfParentNotFound() throws Exception {
        given()
            .contentType(ContentType.JSON)
            .body(buildDSLWithOptions(QUERY_EXISTS, DATA)).when()
            .post("/units").then()
            .body(equalTo(generateResponseErrorFromStatus(Status.NOT_FOUND)))
            .statusCode(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void shouldReturnErrorRequestBadRequestIfDocumentIsTooLarge() throws Exception {
        final int limitRequest = GlobalDatasParser.limitRequest;
        GlobalDatasParser.limitRequest = 99;
        given()
            .contentType(ContentType.JSON)
            .body(buildDSLWithOptions("", createJsonStringWithDepth(60))).when()
            .post("/units").then()
            .body(equalTo(generateResponseErrorFromStatus(Status.REQUEST_ENTITY_TOO_LARGE)))
            .statusCode(Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode());
        GlobalDatasParser.limitRequest = limitRequest;
    }

    @Test
    public void shouldReturnResponseOKIfDocumentCreated() throws Exception {
        given()
            .contentType(ContentType.JSON)
            .body(buildDSLWithOptions("", DATA)).when()
            .post("/units").then()
            .body(equalTo(
                new RequestResponseOK().setHits(1,0,1).setQuery(buildDSLWithOptions("", DATA)).toString()))
            .statusCode(Status.CREATED.getStatusCode());
    }

    // Test object group
    @Test(expected = InvalidParseOperationException.class)
    public void givenInsertObjectGroupWithBodyIsNotCorrectThenReturnErrorBadRequest() throws Exception {
        given()
            .contentType(ContentType.JSON)
            .body(buildDSLWithOptions("invalid", DATA)).when()
            .post("/objectgroups").then()
            .body(equalTo(generateResponseErrorFromStatus(Status.BAD_REQUEST)))
            .statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void givenInsertObjectGroupWithIdDuplicatedThenReturnErrorConflict() throws Exception {
        with()
            .contentType(ContentType.JSON)
            .body(buildDSLWithOptions("", DATA)).when()
            .post("/units").then()
            .statusCode(Status.CREATED.getStatusCode());
        with()
            .contentType(ContentType.JSON)
            .body(buildDSLWithOptions(QUERY_PATH, DATA2)).when()
            .post("/objectgroups").then()
            .statusCode(Status.CREATED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(buildDSLWithOptions(QUERY_PATH, DATA2)).when()
            .post("/objectgroups").then()
            .body(equalTo(generateResponseErrorFromStatus(Status.CONFLICT)))
            .statusCode(Status.CONFLICT.getStatusCode());
    }

    @Test
    public void givenInsertObjectGroupWithNoParentThenReturnErrorNotFound() throws Exception {
        given()
            .contentType(ContentType.JSON)
            .body(buildDSLWithOptions("", DATA3)).when()
            .post("/objectgroups").then()
            .body(equalTo(generateResponseErrorFromStatus(Status.NOT_FOUND)))
            .statusCode(Status.NOT_FOUND.getStatusCode());
    }


    @Test
    public void shouldReturnErrorRequestBadRequestWhenInsertGOIfDocumentIsTooLarge() throws Exception {
        final int limitRequest = GlobalDatasParser.limitRequest;
        GlobalDatasParser.limitRequest = 99;
        given()
            .contentType(ContentType.JSON)
            .body(buildDSLWithOptions("", createJsonStringWithDepth(60))).when()
            .post("/objectgroups").then()
            .body(equalTo(generateResponseErrorFromStatus(Status.REQUEST_ENTITY_TOO_LARGE)))
            .statusCode(Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode());
        GlobalDatasParser.limitRequest = limitRequest;
    }

}
