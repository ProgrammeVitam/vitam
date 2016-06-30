/**
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
 */
package fr.gouv.vitam.metadata.rest;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.RestAssured.with;

import javax.ws.rs.core.Response.Status;

import org.jhades.JHades;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Header;
import com.jayway.restassured.response.Headers;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import fr.gouv.vitam.api.config.MetaDataConfiguration;
import fr.gouv.vitam.core.database.collections.MongoDbAccess;

/**
 * 
 */
public class SelectUnitResourceTest {


    private static final String DATA =
        "{ \"_id\": \"aeaqaaaaaeaaaaakaarp4akuuf2ldmyaaaaq\", " + "\"data\": \"data2\" }";
    private static final String DATA2 =
        "{ \"_id\": \"aeaqaaaaaeaaaaakaarp4akuuf2ldmyaaaab\"," + "\"data\": \"data2\" }";
    private static final String DATA_URI = "/metadata/v1";
    private static final String DATABASE_NAME = "vitam-test";
    private static final int DATABASE_PORT = 12345;
    private static MongodExecutable mongodExecutable;
    static MongodProcess mongod;

    private static final String BAD_QUERY_TEST =
        "{ $or : " + "[ " + "   {$exists : '_id'}, " + "   {$missing : 'mavar2'}, " + "   {$badRquest : 'mavar3'}, " +
            "   { $or : [ " + "          {$in : { 'mavar4' : [1, 2, 'maval1'] }}, " + "]}";

    private static final String SERVER_HOST = "localhost";

    private static final String X_HTTP_Method = "X-HTTP-Method-Override";
    private static final String GET = "GET";

    private static final String BODY_TEST = "{$query: {$eq: {\"data\" : \"data2\" }}, $projection: {}, $filter: {}}";

    private static final int SERVER_PORT = 8589;

    private static final String buildDSLWithOptions(String query, String data) {
        return "{ $roots : [ '' ], $query : [ " + query + " ], $data : " + data + " }";
    }


    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // Identify overlapping in particular jsr311
        new JHades().overlappingJarsReport();

        final MongodStarter starter = MongodStarter.getDefaultInstance();
        mongodExecutable = starter.prepare(new MongodConfigBuilder()
            .version(Version.Main.PRODUCTION)
            .net(new Net(DATABASE_PORT, Network.localhostIsIPv6()))
            .build());
        mongod = mongodExecutable.start();

        final MetaDataConfiguration configuration = new MetaDataConfiguration(SERVER_HOST, DATABASE_PORT, DATABASE_NAME);
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
        MongoDbAccess.VitamCollections.C_UNIT.getCollection().drop();
    }

    // select archive unit test
    @Test
    public void given_2units_insert_when_searchUnits_thenReturn_Found() throws Exception {
        with()
            .contentType(ContentType.JSON)
            .body(buildDSLWithOptions("", DATA2)).when()
            .post("/units").then()
            .statusCode(Status.CREATED.getStatusCode());

        with()
            .contentType(ContentType.JSON)
            .body(buildDSLWithOptions("", DATA)).when()
            .post("/units").then()
            .statusCode(Status.CREATED.getStatusCode());

        given().headers(Headers.headers(new Header(X_HTTP_Method, GET)))
            .contentType(ContentType.JSON)
            .body(BODY_TEST).when()
            .post("/units").then()
            .statusCode(Status.FOUND.getStatusCode());
    }


    @Test
    public void given_badRequestHHtpwhen_thenReturn_() {
        given()
            .contentType(ContentType.JSON)
            .header("X-HTTP-Method-Override", "ABC")
            .when()
            .post("/units")
            .then()
            .statusCode(Status.METHOD_NOT_ALLOWED.getStatusCode());

    }


    @Test
    public void given_Bad_Request_when_Select_thenReturn_Bad_Request() {

        given()
            .contentType(ContentType.JSON)
            .header("X-HTTP-Method-Override", "GET")
            .body(BAD_QUERY_TEST)
            .when()
            .post("/units")
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void given_emptyQuery_when_Select_thenReturn_Bad_Request() {

        given()
            .contentType(ContentType.JSON)
            .header("X-HTTP-Method-Override", "GET")
            .body("")
            .when()
            .post("/units")
            .then()
            .statusCode(Status.NOT_ACCEPTABLE.getStatusCode());
    }

}
