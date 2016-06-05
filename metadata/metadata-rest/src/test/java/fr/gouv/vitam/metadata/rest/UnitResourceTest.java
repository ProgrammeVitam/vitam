package fr.gouv.vitam.metadata.rest;

import static com.jayway.restassured.RestAssured.get;
import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.RestAssured.with;
import static org.hamcrest.Matchers.equalTo;

import javax.ws.rs.core.Response.Status;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import fr.gouv.vitam.api.config.MetaDataConfiguration;
import fr.gouv.vitam.api.exception.MetaDataException;
import fr.gouv.vitam.api.model.DatabaseCursor;
import fr.gouv.vitam.api.model.RequestResponseError;
import fr.gouv.vitam.api.model.RequestResponseOK;
import fr.gouv.vitam.api.model.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.core.database.collections.MongoDbAccess;

public class UnitResourceTest {
    private static final String DATA = "{ \"_id\": \"aeaqaaaaaeaaaaakaarp4akuuf2ldmyaaaaq\", "
            + "\"data\": \"data1\" }";
    private static final String DATA2 = "{ \"_id\": \"aeaqaaaaaeaaaaakaarp4akuuf2ldmyaaaab\","
            + "\"data\": \"data2\" }";
    private static final String DATA_URI = "/metadata/v1";
    private static final String DATABASE_COLLECTIONS = "Units";
    private static final String DATABASE_NAME = "vitam-test";
    private static final int DATABASE_PORT = 12345;
    private static MongodExecutable mongodExecutable;
    static MongodProcess mongod;

    private static final String QUERY_PATH = "{ $path :  [\"aeaqaaaaaeaaaaakaarp4akuuf2ldmyaaaaq\"]  }";
    private static final String QUERY_EXISTS = "{ $exists :  \"_id\"  }";
    private static final String QUERY_TEST = 
            "{ $or : "
            + "[ "
            + "   {$exists : '_id'}, "
            + "   {$missing : 'mavar2'}, "
            + "   {$isNull : 'mavar3'}, "
            + "   { $or : [ "
            + "          {$in : { 'mavar4' : [1, 2, 'maval1'] }}, "
            + "          { $nin : { 'mavar5' : ['maval2', true] } } ] "
            + "   } "
            + "]}";

    private static final String SERVER_HOST = "localhost";

    // TODO REVIEW Magic number ?
    private static final int SERVER_PORT = 56789;

    private static final String buildDSLWithOptions(String query, String data) {
        return "{ $roots : [ '' ], $query : [ " +query+ " ], $data : "+data+" }";
    }

    private static String createJsonStringWithDepth(int depth) {
        StringBuilder obj = new StringBuilder();
        if (depth == 0) {
            return " \"b\" ";
        }
        obj.append("{ \"a\": ").append(createJsonStringWithDepth(depth - 1)).append("}");
        return obj.toString();
    }

    private static String generateResponseErrorFromStatus(Status status) throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(new RequestResponseError()
                .setError(new VitamError(status.getStatusCode()).setContext("ingest").setState("code_vitam")
                        .setMessage(status.getReasonPhrase()).setDescription(status.getReasonPhrase())));
    }

    private static String generateResponseOK(DatabaseCursor cursor, JsonNode query) throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(new RequestResponseOK().setHits(cursor).setQuery(query));
    }

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        final MongodStarter starter = MongodStarter.getDefaultInstance();
        mongodExecutable = starter.prepare(new MongodConfigBuilder()
            .version(Version.Main.PRODUCTION)
            .net(new Net(DATABASE_PORT, Network.localhostIsIPv6()))
            .build());
        mongod = mongodExecutable.start();

        MetaDataConfiguration configuration = new MetaDataConfiguration(SERVER_HOST, DATABASE_PORT, DATABASE_NAME,
                DATABASE_COLLECTIONS);
        MetaDataApplication.run(configuration, SERVER_PORT);
        RestAssured.port = SERVER_PORT;
        RestAssured.basePath = DATA_URI;
    }

    @AfterClass
    public static void tearDownAfterClass() {
        mongod.stop();
        mongodExecutable.stop();
    }

    @After
    public void tearDown() {
        MongoDbAccess.VitamCollections.Cunit.getCollection().drop();
    }

    /**
     * Test status endpoint
     * 
     */
    @Test
    public void shouldGetStatusOK() throws MetaDataException, InvalidParseOperationException {
        get("/status").then().statusCode(200);
    }

    /**
     * Test insert Unit endpoint
     * 
     */

    @Test
    public void shouldReturnErrorBadRequestIfBodyIsNotCorrect() throws Exception {
        given()
            .contentType(ContentType.JSON)
            .body(buildDSLWithOptions("invalid", DATA)).
        when()
            .post("/units").
        then()
            .body(equalTo(generateResponseErrorFromStatus(Status.BAD_REQUEST)))
            .statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void shouldReturnErrorConflictIfIdDuplicated() throws Exception {
        with()
            .contentType(ContentType.JSON)
            .body(buildDSLWithOptions("", DATA)).
        when()
            .post("/units").
        then()
            .statusCode(Status.CREATED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(buildDSLWithOptions("", DATA)).
        when()
            .post("/units").
        then()
            .body(equalTo(generateResponseErrorFromStatus(Status.CONFLICT)))
            .statusCode(Status.CONFLICT.getStatusCode());
    }

    @Test
    public void givenInsertUnitWithQueryPATHWhenParentFoundThenReturnCreated() throws Exception {
        with()
            .contentType(ContentType.JSON)
            .body(buildDSLWithOptions("", DATA)).
        when()
            .post("/units").
        then()
            .statusCode(Status.CREATED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(buildDSLWithOptions(QUERY_PATH, DATA2)).
        when()
            .post("/units").
        then()
            .statusCode(Status.CREATED.getStatusCode());
    }

    @Test
    public void givenInsertUnitWithQueryExistsWhenParentFoundThenReturnCreated() throws Exception {
        with()
            .contentType(ContentType.JSON)
            .body(buildDSLWithOptions("", DATA)).
        when()
            .post("/units").
        then()
            .statusCode(Status.CREATED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(buildDSLWithOptions(QUERY_EXISTS, DATA2)).
        when()
            .post("/units").
        then()
            .statusCode(Status.CREATED.getStatusCode());
    }
    
    @Test
    public void givenInsertUnitWithQueryWhenParentFoundThenReturnCreated() throws Exception {
        with()
            .contentType(ContentType.JSON)
            .body(buildDSLWithOptions("", DATA)).
        when()
            .post("/units").
        then()
            .statusCode(Status.CREATED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(buildDSLWithOptions(QUERY_TEST, DATA2)).
        when()
            .post("/units").
        then()
            .statusCode(Status.CREATED.getStatusCode());
    }

    @Test
    public void shouldReturnErrorIfContentTypeIsNotJson() throws Exception {
        given()
            .contentType("application/xml")
            .body(buildDSLWithOptions("", DATA)).
        when()
            .post("/units").
        then()
            .statusCode(Status.UNSUPPORTED_MEDIA_TYPE.getStatusCode());
    }

    @Test
    public void shouldReturnErrorNotFoundIfParentNotFound() throws Exception {
        given()
            .contentType(ContentType.JSON)
            .body(buildDSLWithOptions(QUERY_EXISTS, DATA)).
        when()
            .post("/units").
        then()
            .body(equalTo(generateResponseErrorFromStatus(Status.NOT_FOUND)))
            .statusCode(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void shouldReturnErrorRequestEntityTooLargeIfDocumentIsTooLarge() throws Exception {
        given()
            .contentType(ContentType.JSON)
            .body(buildDSLWithOptions("", createJsonStringWithDepth(100))).
        when()
            .post("/units").
        then()
            .body(equalTo(generateResponseErrorFromStatus(Status.REQUEST_ENTITY_TOO_LARGE)))
            .statusCode(Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode());
    }

    @Test
    public void shouldReturnResponseOKIfDocumentCreated() throws Exception {
        given()
            .contentType(ContentType.JSON)
            .body(buildDSLWithOptions("", DATA)).
        when()
            .post("/units").
        then()
            .body(equalTo(generateResponseOK(new DatabaseCursor(1, 0, 1), JsonHandler.getFromString(buildDSLWithOptions("", DATA)))))
            .statusCode(Status.CREATED.getStatusCode());
    }

}
