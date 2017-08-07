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
package fr.gouv.vitam.logbook.rest;

import static com.jayway.restassured.RestAssured.given;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.core.Response.Status;

import fr.gouv.vitam.common.PropertiesUtils;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.jhades.JHades;
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
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ServerIdentity;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.junit.JunitHelper.ElasticsearchTestConfiguration;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleObjectGroupParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.LogbookConfiguration;

/**
 *
 */
public class LogBookLifeCycleObjectGroupTest {


    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogBookLifeCycleObjectGroupTest.class);

    private static final String REST_URI = "/logbook/v1";

    private static final String JETTY_CONFIG = "jetty-config-test.xml";
    private static final String SERVER_HOST = "localhost";
    private static final String DATABASE_NAME = "vitam-test";
    private static MongodExecutable mongodExecutable;
    private static MongodProcess mongod;

    private static final Integer tenantId = 0;
    private static final List<Integer> tenantList = Arrays.asList(0);

    // ES
    @ClassRule
    public static TemporaryFolder temporaryFolder = new TemporaryFolder();
    private final static String ES_CLUSTER_NAME = "vitam-cluster";
    private final static String ES_HOST_NAME = "localhost";
    private static ElasticsearchTestConfiguration config = null;

    private static final String LIFE_OBJECT_GROUP_ID_URI = "/operations/{id_op}/objectgrouplifecycles/{id_lc}";
    private static final String LIFE_OBJECT_GROUP_URI = "/operations/{id_op}/objectgrouplifecycles";

    private static int databasePort;
    private static int serverPort;
    private static LogbookMain application;
    private static LogbookLifeCycleObjectGroupParameters logbookLifeCyclesObjectGroupParametersStart;
    private static LogbookLifeCycleObjectGroupParameters logbookLCObjectGroupParametersAppend;
    private static LogbookLifeCycleObjectGroupParameters logbookLifeCyclesObjectGroupParametersBAD;

    private static LogbookLifeCycleObjectGroupParameters logbookLifeCyclesObjectGroupParametersUpdate;

    private static JunitHelper junitHelper;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        new JHades().overlappingJarsReport();

        junitHelper = JunitHelper.getInstance();
        databasePort = junitHelper.findAvailablePort();

        final MongodStarter starter = MongodStarter.getDefaultInstance();
        mongodExecutable = starter.prepare(new MongodConfigBuilder()
            .withLaunchArgument("--enableMajorityReadConcern")
            .version(Version.Main.PRODUCTION)
            .net(new Net(databasePort, Network.localhostIsIPv6()))
            .build());

        mongod = mongodExecutable.start();
        // ES
        try {
            config = JunitHelper.startElasticsearchForTest(temporaryFolder, ES_CLUSTER_NAME);
        } catch (final VitamApplicationServerException e1) {
            assumeTrue(false);
        }
        serverPort = junitHelper.findAvailablePort();

        // TODO P1 verifier la compatibilité avec les tests parallèles sur jenkins


        try {
            final LogbookConfiguration logbookConf = new LogbookConfiguration();
            final List<MongoDbNode> nodes = new ArrayList<>();
            nodes.add(new MongoDbNode(SERVER_HOST, databasePort));
            logbookConf.setDbName(DATABASE_NAME).setMongoDbNodes(nodes);
            final List<ElasticsearchNode> esNodes = new ArrayList<>();
            esNodes.add(new ElasticsearchNode(ES_HOST_NAME, config.getTcpPort()));
            logbookConf.setJettyConfig(JETTY_CONFIG);
            logbookConf.setP12LogbookFile("tsa.p12");
            logbookConf.setP12LogbookPassword("1234");
            logbookConf.setWorkspaceUrl("http://localhost:8001");
            logbookConf.setClusterName(ES_CLUSTER_NAME);
            logbookConf.setElasticsearchNodes(esNodes);
            logbookConf.setTenants(tenantList);

            File file = temporaryFolder.newFile();
            String configurationFile = file.getAbsolutePath();
            PropertiesUtils.writeYaml(file, logbookConf);


            application = new LogbookMain(configurationFile);
            application.start();

            RestAssured.port = serverPort;
            RestAssured.basePath = REST_URI;
            JunitHelper.unsetJettyPortSystemProperty();

        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
            throw new IllegalStateException(
                "Cannot start the Logbook Application Server", e);
        }

        final GUID eip = GUIDFactory.newWriteLogbookGUID(0);
        final GUID iop = GUIDFactory.newWriteLogbookGUID(0);
        final GUID ioL = GUIDFactory.newUnitGUID(0);
        final GUID iog = GUIDFactory.newObjectGroupGUID(0);


        logbookLifeCyclesObjectGroupParametersStart =
            LogbookParametersFactory.newLogbookLifeCycleObjectGroupParameters();
        logbookLifeCyclesObjectGroupParametersStart.setStatus(StatusCode.STARTED);
        logbookLifeCyclesObjectGroupParametersStart.putParameterValue(LogbookParameterName.eventIdentifier,
            eip.toString());
        logbookLifeCyclesObjectGroupParametersStart.putParameterValue(LogbookParameterName.eventIdentifierProcess,
            iop.toString());
        logbookLifeCyclesObjectGroupParametersStart.putParameterValue(LogbookParameterName.objectIdentifier,
            ioL.toString());
        /**
         * Bad request
         */
        logbookLifeCyclesObjectGroupParametersBAD = LogbookParametersFactory.newLogbookLifeCycleObjectGroupParameters();

        logbookLifeCyclesObjectGroupParametersBAD.putParameterValue(LogbookParameterName.eventIdentifier,
            eip.toString());
        logbookLifeCyclesObjectGroupParametersStart.putParameterValue(LogbookParameterName.eventIdentifierProcess,
            iop.toString());


        /**
         * update
         *
         */
        final GUID eip2 = GUIDFactory.newWriteLogbookGUID(0);
        final GUID iop2 = GUIDFactory.newWriteLogbookGUID(0);
        final GUID ioL2 = GUIDFactory.newUnitGUID(0);
        logbookLifeCyclesObjectGroupParametersUpdate =
            LogbookParametersFactory.newLogbookLifeCycleObjectGroupParameters();
        logbookLifeCyclesObjectGroupParametersUpdate.setStatus(StatusCode.STARTED);
        logbookLifeCyclesObjectGroupParametersUpdate.putParameterValue(LogbookParameterName.eventIdentifier,
            eip2.toString());
        logbookLifeCyclesObjectGroupParametersUpdate.putParameterValue(LogbookParameterName.eventIdentifierProcess,
            iop2.toString());
        logbookLifeCyclesObjectGroupParametersUpdate.putParameterValue(LogbookParameterName.objectIdentifier,
            ioL2.toString());
        /**
         *
         * update
         */

        logbookLCObjectGroupParametersAppend = LogbookParametersFactory.newLogbookLifeCycleObjectGroupParameters();
        logbookLCObjectGroupParametersAppend.setStatus(StatusCode.OK);
        logbookLCObjectGroupParametersAppend.putParameterValue(LogbookParameterName.eventIdentifierProcess,
            iop.toString());
        logbookLCObjectGroupParametersAppend.putParameterValue(LogbookParameterName.objectIdentifier,
            ioL.toString());

    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        LOGGER.debug("Ending tests");
        try {
            if (application != null) {
                application.stop();
            }
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
        }
        if (config != null) {
            JunitHelper.stopElasticsearchForTest(config);
        }
        mongod.stop();
        mongodExecutable.stop();
        junitHelper.releasePort(databasePort);
        junitHelper.releasePort(serverPort);
    }

    @Test
    public final void given_lifeCycleObjectGroup_when_create_thenReturn_created() {
        // Creation OK

        logbookLifeCyclesObjectGroupParametersStart.putParameterValue(LogbookParameterName.eventType, "event");
        logbookLifeCyclesObjectGroupParametersStart.setTypeProcess(LogbookTypeProcess.INGEST);
        logbookLifeCyclesObjectGroupParametersStart.putParameterValue(LogbookParameterName.outcomeDetail,
            "outcomeDetail");
        logbookLifeCyclesObjectGroupParametersStart.putParameterValue(LogbookParameterName.outcomeDetailMessage,
            "outcomeDetailMessage");
        logbookLifeCyclesObjectGroupParametersStart.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());
        logbookLifeCyclesObjectGroupParametersStart.putParameterValue(LogbookParameterName.agentIdentifier,
            ServerIdentity.getInstance().getJsonIdentity());

        String objectId =
            logbookLifeCyclesObjectGroupParametersStart.getParameterValue(LogbookParameterName.objectIdentifier);
        String operationId = logbookLifeCyclesObjectGroupParametersStart
            .getParameterValue(LogbookParameterName.eventIdentifierProcess);

        given()
            .contentType(ContentType.JSON)
            .body(logbookLifeCyclesObjectGroupParametersStart.toString())
            .when()
            .post(LIFE_OBJECT_GROUP_ID_URI,
                operationId,
                objectId)
            .then()
            .statusCode(Status.CREATED.getStatusCode());



        // already exsits
        given()
            .contentType(ContentType.JSON)
            .body(logbookLifeCyclesObjectGroupParametersStart.toString())
            .when()
            .post(LIFE_OBJECT_GROUP_ID_URI, operationId,
                objectId)
            .then()
            .statusCode(Status.CONFLICT.getStatusCode());

        // incoherence parameters ; response bad_request
        given()
            .contentType(ContentType.JSON)
            .body(logbookLifeCyclesObjectGroupParametersStart.toString())
            .when()
            .post(LIFE_OBJECT_GROUP_ID_URI, operationId,
                "bad_id")
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());


        // update ok
        logbookLifeCyclesObjectGroupParametersStart.putParameterValue(LogbookParameterName.outcomeDetailMessage,
            "ModifiedoutcomeDetailMessage");
        logbookLifeCyclesObjectGroupParametersStart.setStatus(StatusCode.OK);
        given()
            .contentType(ContentType.JSON)
            .body(logbookLifeCyclesObjectGroupParametersStart.toString())
            .when()
            .put(LIFE_OBJECT_GROUP_ID_URI, operationId,
                objectId)
            .then()
            .statusCode(Status.OK.getStatusCode());


        // Update illegal argument incoherence parameters ; response bad_request
        given()
            .contentType(ContentType.JSON)
            .body(logbookLifeCyclesObjectGroupParametersStart.toString())
            .when()
            .put(LIFE_OBJECT_GROUP_ID_URI, operationId,
                "bad_id")
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());


        // Commit
        given().contentType(ContentType.JSON)
            .when()
            .put("/operations/" + operationId + "/objectgrouplifecycles/" +
                objectId +
                "/commit")
            .then()
            .statusCode(Status.OK.getStatusCode());

        // Test direct access
        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, 0)
            .body(new Select().getFinalSelect())
            .when()
            .get("/objectgrouplifecycles/" + objectId)
            .then()
            .statusCode(Status.OK.getStatusCode());

        // Test Iterator
        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, 0)
            .body(new Select().getFinalSelect()).header(GlobalDataRest.X_CURSOR, true)
            .when()
            .get(LIFE_OBJECT_GROUP_URI, operationId)
            .then()
            .statusCode(Status.OK.getStatusCode()).header(GlobalDataRest.X_CURSOR_ID, new BaseMatcher() {

                @Override
                public boolean matches(Object item) {
                    return item != null && item instanceof String && !((String) item).isEmpty();
                }

                @Override
                public void describeTo(Description description) {}
            });
    }

    @Test
    public final void given_lifeCycleObjectGroup_Without_MandotoryParams_when_create_thenReturn_BAD_RESUEST() {
        final GUID guidTest = GUIDFactory.newWriteLogbookGUID(0);

        logbookLifeCyclesObjectGroupParametersBAD.putParameterValue(LogbookParameterName.objectIdentifier,
            guidTest.toString());
        given()
            .contentType(ContentType.JSON)
            .body(logbookLifeCyclesObjectGroupParametersBAD.toString())
            .when()
            .post(LIFE_OBJECT_GROUP_ID_URI,
                logbookLifeCyclesObjectGroupParametersStart
                    .getParameterValue(LogbookParameterName.eventIdentifierProcess),
                logbookLifeCyclesObjectGroupParametersStart.getParameterValue(LogbookParameterName.objectIdentifier))
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public final void given_lifeCycleGO_when_update_thenReturn_notfound() {
        // update notFound
        logbookLifeCyclesObjectGroupParametersUpdate.putParameterValue(LogbookParameterName.eventType, "event");
        logbookLifeCyclesObjectGroupParametersUpdate.setTypeProcess(LogbookTypeProcess.INGEST);
        logbookLifeCyclesObjectGroupParametersUpdate.putParameterValue(LogbookParameterName.outcomeDetail,
            "outcomeDetail");
        logbookLifeCyclesObjectGroupParametersUpdate.putParameterValue(LogbookParameterName.outcomeDetailMessage,
            "outcomeDetailMessage");
        logbookLifeCyclesObjectGroupParametersUpdate.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());
        logbookLifeCyclesObjectGroupParametersUpdate.putParameterValue(LogbookParameterName.agentIdentifier,
            ServerIdentity.getInstance().getJsonIdentity());


        given()
            .contentType(ContentType.JSON)
            .body(logbookLifeCyclesObjectGroupParametersUpdate.toString())
            .when()
            .put(LIFE_OBJECT_GROUP_ID_URI,
                logbookLifeCyclesObjectGroupParametersUpdate
                    .getParameterValue(LogbookParameterName.eventIdentifierProcess),
                logbookLifeCyclesObjectGroupParametersUpdate.getParameterValue(LogbookParameterName.objectIdentifier))
            .then()
            .statusCode(Status.NOT_FOUND.getStatusCode());
    }


    @Test
    public final void given_lifeCycleGO_Without_MandotoryParams_when_update_thenReturn_BAD_RESUEST() {
        final GUID guidTest = GUIDFactory.newWriteLogbookGUID(0);
        logbookLifeCyclesObjectGroupParametersBAD.putParameterValue(LogbookParameterName.objectIdentifier,
            guidTest.toString());
        logbookLifeCyclesObjectGroupParametersBAD.putParameterValue(LogbookParameterName.eventIdentifierProcess,
            guidTest.toString());

        given()
            .contentType(ContentType.JSON)
            .body(logbookLifeCyclesObjectGroupParametersBAD.toString())
            .when()
            .put(LIFE_OBJECT_GROUP_ID_URI,
                logbookLifeCyclesObjectGroupParametersBAD
                    .getParameterValue(LogbookParameterName.eventIdentifierProcess),
                logbookLifeCyclesObjectGroupParametersBAD.getParameterValue(LogbookParameterName.objectIdentifier))
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
    }

}
