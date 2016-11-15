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

package fr.gouv.vitam.ihmrecette.appserver;


import static com.jayway.restassured.RestAssured.given;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response.Status;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server2.application.configuration.MongoDbNode;

// FIXME Think about Unit tests
public class WebApplicationResourceDeleteTest {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(WebApplicationResourceDeleteTest.class);

    // Take it from conf file
    private static final String DEFAULT_WEB_APP_CONTEXT = "/test-admin";
    private static final String CREDENTIALS = "{\"token\": {\"principal\": \"myName\", \"credentials\": \"myName\"}}";
    private static final String CREDENTIALS_NO_VALID =
        "{\"token\": {\"principal\": \"myName\", \"credentials\": \"myName\"}}";
    private static JunitHelper junitHelper;
    private static int serverPort;
    private static int databasePort;
    private static File adminConfigFile;

    static MongodExecutable mongodExecutable;
    static MongodProcess mongod;

    private static ServerApplication application;

    private static JunitHelper.ElasticsearchTestConfiguration config = null;
    private final static String CLUSTER_NAME = "vitam-cluster";
    private final static String HOST_NAME = "127.0.0.1";

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {

        junitHelper = JunitHelper.getInstance();
        // ES
        try {
            config = JunitHelper.startElasticsearchForTest(tempFolder, CLUSTER_NAME);
        } catch (VitamApplicationServerException e1) {
            assumeTrue(false);
        }

        final List<ElasticsearchNode> nodes = new ArrayList<>();
        nodes.add(new ElasticsearchNode(HOST_NAME, config.getTcpPort()));

        databasePort = junitHelper.findAvailablePort();
        serverPort = junitHelper.findAvailablePort();

        final File adminConfig = PropertiesUtils.findFile("ihm-recette.conf");
        final WebApplicationConfig realAdminConfig =
            PropertiesUtils.readYaml(adminConfig, WebApplicationConfig.class);
        realAdminConfig.getMongoDbNodes().get(0).setDbPort(databasePort);
        realAdminConfig.setBaseUrl(DEFAULT_WEB_APP_CONTEXT);
        realAdminConfig.setSecure(false);
        realAdminConfig.setClusterName(CLUSTER_NAME);
        realAdminConfig.setElasticsearchNodes(nodes);
        adminConfigFile = File.createTempFile("test", "ihm-recette.conf", adminConfig.getParentFile());
        PropertiesUtils.writeYaml(adminConfigFile, realAdminConfig);

        final MongodStarter starter = MongodStarter.getDefaultInstance();
        mongodExecutable = starter.prepare(new MongodConfigBuilder()
            .version(Version.Main.PRODUCTION)
            .net(new Net(databasePort, Network.localhostIsIPv6()))
            .build());
        mongod = mongodExecutable.start();

        List<MongoDbNode> mongoNodes = new ArrayList<>();
        mongoNodes.add(new MongoDbNode("localhost", databasePort));

        RestAssured.port = serverPort;
        RestAssured.basePath = DEFAULT_WEB_APP_CONTEXT + "/v1/api";

        try {
            application = new ServerApplication(adminConfigFile.getAbsolutePath());
            application.start();
            JunitHelper.unsetJettyPortSystemProperty();
        } catch (final VitamApplicationServerException e) {

            throw new IllegalStateException(
                "Cannot start the Logbook Application Server", e);
        }
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        LOGGER.debug("Ending tests");
        try {
            application.stop();
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
        }

        mongod.stop();
        mongodExecutable.stop();
        junitHelper.releasePort(databasePort);
        junitHelper.releasePort(serverPort);
    }

    @Test
    public void givenNoSecureServerLoginUnauthorized() {
        given().contentType(ContentType.JSON).body(CREDENTIALS).expect().statusCode(Status.UNAUTHORIZED.getStatusCode())
            .when()
            .post("login");
        given().contentType(ContentType.JSON).body(CREDENTIALS_NO_VALID).expect()
            .statusCode(Status.UNAUTHORIZED.getStatusCode()).when()
            .post("login");
    }

    @Test
    public void testSuccessStatus() {
        given().expect().statusCode(Status.NO_CONTENT.getStatusCode()).when().get("status");
    }

    @Test
    public void testDeleteFormatOK() {
        given().expect().statusCode(Status.OK.getStatusCode()).when().delete("delete/formats");
    }

    @Test
    public void testDeleteRulesFileOK() {
        given().expect().statusCode(Status.OK.getStatusCode()).when().delete("delete/rules");
    }

    @Test
    public void testAccessionRegisterOK() {
        given().expect().statusCode(Status.OK.getStatusCode()).when().delete("delete/accessionregisters");
    }

    @Test
    public void testDeleteLogbookOperationOK() {
        given().expect().statusCode(Status.OK.getStatusCode()).when().delete("delete/logbook/operation");
    }

    @Test
    public void testDeleteLogbookLifecycleOGOK() {
        given().expect().statusCode(Status.OK.getStatusCode()).when().delete("delete/logbook/lifecycle/objectgroup");
    }

    @Test
    public void testDeleteLogbookLifecycleUnitOK() {
        given().expect().statusCode(Status.OK.getStatusCode()).when().delete("delete/logbook/lifecycle/unit");
    }

    @Test
    public void testDeleteMetadataOGOK() {
        given().expect().statusCode(Status.OK.getStatusCode()).when().delete("delete/metadata/objectgroup");
    }

    @Test
    public void testDeleteMetadataUnitOK() {
        given().expect().statusCode(Status.OK.getStatusCode()).when().delete("delete/metadata/unit");
    }

    @Test
    public void testDeleteAllOk() {
        given().expect().statusCode(Status.OK.getStatusCode()).when().delete("delete");
    }


    @Test
    public void testGetAvailableFilesListWithInternalSererWhenBadSipDirectory() {
        final String currentSipDirectory = application.getConfiguration().getSipDirectory();
        application.getConfiguration().setSipDirectory("SIP_DIRECTORY_NOT_FOUND");

        given().expect().statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
            .when()
            .get("/upload/fileslist");

        // Reset WebApplicationConfiguration
        application.getConfiguration().setSipDirectory(currentSipDirectory);
    }

    @Test
    public void testGetAvailableFilesListWithInternalSererWhenNotConfiguredSipDirectory() {
        final String currentSipDirectory = application.getConfiguration().getSipDirectory();
        application.getConfiguration().setSipDirectory(null);

        given().expect().statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
            .when()
            .get("/upload/fileslist");

        // Reset WebApplicationConfiguration
        application.getConfiguration().setSipDirectory(currentSipDirectory);
    }

    @Test
    public void testUploadFileFromServerWithInternalServerWhenNotConfiguredSipDirectory() throws VitamException {
        final String currentSipDirectory = application.getConfiguration().getSipDirectory();
        application.getConfiguration().setSipDirectory(null);

        given().param("file_name", "SIP.zip").expect()
            .statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
            .when()
            .get("/upload/SIP.zip");

        // Reset WebApplicationConfiguration
        application.getConfiguration().setSipDirectory(currentSipDirectory);
    }

}
