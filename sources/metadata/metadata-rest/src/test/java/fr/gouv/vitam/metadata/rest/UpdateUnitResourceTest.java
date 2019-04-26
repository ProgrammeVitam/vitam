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

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.RestAssured.with;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response.Status;

import fr.gouv.vitam.common.PropertiesUtils;
import org.jhades.JHades;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.parser.request.GlobalDatasParser;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.junit.JunitHelper.ElasticsearchTestConfiguration;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.metadata.api.config.MetaDataConfiguration;
import fr.gouv.vitam.metadata.core.database.collections.ElasticsearchAccessMetadata;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;

public class UpdateUnitResourceTest {

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    private static final Integer TENANT_ID_0 = new Integer(0);
    static final List tenantList = new ArrayList() {
        {
            add(TENANT_ID_0);
        }
    };
    private static final String DATA =
        "{ \"#id\": \"aeaqaaaaaeaaaaakaarp4akuuf2ldmyaaaaq\", \"#tenant\": 0, " + "\"data\": \"data2\" }";
    private static final String DATA2 =
        "{ \"#id\": \"aeaqaaaaaeaaaaakaarp4akuuf2ldmyaaaab\", \"#tenant\": 0, " + "\"data\": \"data2\", " +
            "\"Title\": \"Archive3\", \"#management\": {\"NeedAuthorization\": true}," +
            " \"DescriptionLevel\": \"Item\" }";

    private static final String ID_UNIT = "aeaqaaaaaeaaaaakaarp4akuuf2ldmyaaaab";
    private static final String DATA_URI = "/metadata/v1";
    private static final String DATABASE_NAME = "vitam-test";
    private static final String JETTY_CONFIG = "jetty-config-test.xml";
    private static MongodExecutable mongodExecutable;
    static MongodProcess mongod;

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    private final static String CLUSTER_NAME = "vitam-cluster";
    private final static String HOST_NAME = "127.0.0.1";

    private static final String SERVER_HOST = "localhost";
    private static final Integer TENANT_ID = 0;

    private static final String BODY_TEST =
        "{\"$query\": [], \"$action\": [{\"$set\": {\"data\": \"data3\"}}], \"$filter\": {}}";

    private static final String BODY_TEST_BAD_REQUEST =
        "{\"$query\": [{ \"#id\": \"aeaqaaaaaeaaaaakaarp4akuuf2ldmyaaa22\"}], \"$action\": [{\"$set\": {\"data\": \"data3\"}}], \"$filter\": {}}";

    private static final String REAL_UPDATE_BODY_TEST =
        "{\"$query\": [], \"$action\": [{\"$set\": {\"data\": \"data4\"}}, {\"$push\": {\"#operations\": {\"$each\": [\"aeaqaaaaaeaaaaakaarp4akuuf2ldmyaaaac\"]}}}], \"$filter\": {}}";
    private static JunitHelper junitHelper;
    private static int serverPort;
    private static int dataBasePort;

    private static MetadataMain application;

    private static ElasticsearchTestConfiguration config = null;
    private static ElasticsearchAccessMetadata esClient;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // Identify overlapping in particular jsr311
        new JHades().overlappingJarsReport();
        junitHelper = JunitHelper.getInstance();
        // ES
        try {
            config = JunitHelper.startElasticsearchForTest(tempFolder, CLUSTER_NAME);
        } catch (final VitamApplicationServerException e1) {
            assumeTrue(false);
        }

        final List<ElasticsearchNode> nodes = new ArrayList<>();
        nodes.add(new ElasticsearchNode(HOST_NAME, config.getTcpPort()));
        esClient = new ElasticsearchAccessMetadata(CLUSTER_NAME, nodes);

        dataBasePort = junitHelper.findAvailablePort();

        final MongodStarter starter = MongodStarter.getDefaultInstance();
        mongodExecutable = starter.prepare(new MongodConfigBuilder()
            .withLaunchArgument("--enableMajorityReadConcern")
            .version(Version.Main.PRODUCTION)
            .net(new Net(dataBasePort, Network.localhostIsIPv6()))
            .build());
        mongod = mongodExecutable.start();

        final List<MongoDbNode> mongo_nodes = new ArrayList<>();
        mongo_nodes.add(new MongoDbNode(SERVER_HOST, dataBasePort));
        // TODO: using configuration file ? Why not ?
        final MetaDataConfiguration configuration =
            new MetaDataConfiguration(mongo_nodes, DATABASE_NAME, CLUSTER_NAME, nodes);
        configuration.setJettyConfig(JETTY_CONFIG);
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
    public static void tearDownAfterClass() throws Exception {
        if (config == null) {
            return;
        }
        JunitHelper.stopElasticsearchForTest(config);
        application.stop();
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
        MetadataCollections.UNIT.getCollection().drop();
    }

    private static final JsonNode buildDSLWithOptions(String data) throws InvalidParseOperationException {
        return JsonHandler.getFromString("{ $roots : [], $query : [], $data : " + data + " }");
    }

    private static final JsonNode buildDSLWithOptionsRoot(String data, String root)
        throws InvalidParseOperationException {
        return JsonHandler
            .getFromString("{ $roots : [ '" + root + "' ], $query : [], $data : " + data + " }");
    }

    private static String createJsonStringWithDepth(int depth) {
        final StringBuilder obj = new StringBuilder();
        if (depth == 0) {
            return " \"b\" ";
        }
        obj.append("{ \"a\": ").append(createJsonStringWithDepth(depth - 1)).append("}");
        return obj.toString();
    }

    // Unit by ID (request and uri)

    // TODO : in order to deal with selection in the query, the code should be modified in MetaDataImpl /
    // MetadataJsonResponseUtils
    @Test
    @RunWithCustomExecutor
    public void given_2units_insert_when_UpdateUnitsByID_thenReturn_Found() throws Exception {
        with()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID_0)
            .body(buildDSLWithOptions(DATA2)).when()
            .post("/units").then()
            .statusCode(Status.CREATED.getStatusCode());

        with()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID_0)
            .body(buildDSLWithOptionsRoot(DATA, ID_UNIT)).when()
            .post("/units").then()
            .statusCode(Status.CREATED.getStatusCode());

        esClient.refreshIndex(MetadataCollections.UNIT, TENANT_ID_0);

        given()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .contentType(ContentType.JSON)
            .body(JsonHandler.getFromString(BODY_TEST)).when()
            .put("/units/" + ID_UNIT).then()
            .statusCode(Status.FOUND.getStatusCode());
        esClient.refreshIndex(MetadataCollections.UNIT, TENANT_ID_0);

        given()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .contentType(ContentType.JSON)
            .body(JsonHandler.getFromString(REAL_UPDATE_BODY_TEST)).when()
            .put("/units/" + ID_UNIT).then()
            .statusCode(Status.FOUND.getStatusCode());
    }

    @Test(expected = InvalidParseOperationException.class)
    public void given_emptyQuery_when_UpdateByID_thenReturn_Bad_Request() throws InvalidParseOperationException {

        given()
            .contentType(ContentType.JSON)
            .body(JsonHandler.getFromString(""))
            .when()
            .put("/units/" + ID_UNIT)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void given_bad_header_when_UpdateByID_thenReturn_Bad_Request() throws InvalidParseOperationException {

        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(JsonHandler.getFromString(BODY_TEST_BAD_REQUEST))
            .when()
            .put("/units/" + ID_UNIT)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void shouldReturn_REQUEST_ENTITY_TOO_LARGE_If_DocumentIsTooLarge() throws Exception {
        final int limitRequest = GlobalDatasParser.limitRequest;
        GlobalDatasParser.limitRequest = 99;
        given()
            .contentType(ContentType.JSON)
            .body(buildDSLWithOptions(createJsonStringWithDepth(101))).when()
            .put("/units/" + ID_UNIT).then()
            .statusCode(Status.PRECONDITION_FAILED.getStatusCode());
        GlobalDatasParser.limitRequest = limitRequest;
    }

    @Test(expected = InvalidParseOperationException.class)
    public void shouldReturnErrorRequestBadRequest() throws Exception {
        given()
            .contentType(ContentType.JSON)
            .body(buildDSLWithOptions("lkvhvgvuyqvkvj")).when()
            .put("/units/" + ID_UNIT).then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
    }

}
