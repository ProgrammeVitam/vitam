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
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.jhades.JHades;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

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
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleObjectGroupParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleUnitParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.exception.LogbookDatabaseException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookNotFoundException;
import fr.gouv.vitam.logbook.lifecycles.api.LogbookLifeCycles;
import fr.gouv.vitam.logbook.lifecycles.core.LogbookLifeCyclesImpl;

/**
 *
 */
public class LogBookLifeCycleUnitTest {


    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogBookLifeCycleUnitTest.class);

    private static final String REST_URI = "/logbook/v1";

    private static final String JETTY_CONFIG = "jetty-config-test.xml";
    private static final String SERVER_HOST = "localhost";
    private static MongodExecutable mongodExecutable;
    private static MongodProcess mongod;

    private static final String LIFE_UNIT_ID_URI = "/operations/{id_op}/unitlifecycles/{id_lc}";
    private static final String LIFE_UNIT_URI = "/operations/{id_op}/unitlifecycles";
    private static final String LIFE_OG_ID_URI = "/operations/{id_op}/objectgrouplifecycles/{id_lc}";
    private static final String COMMIT_OG_ID_URI = "/operations/{id_op}/objectgrouplifecycles/{id_lc}/commit";
    private static final String COMMIT_UNIT_ID_URI = "/operations/{id_op}/unitlifecycles/{id_lc}/commit";

    private static final String FAKE_UNIT_LF_ID = "1";
    private static final String FAKE_OBG_LF_ID = "1";
    private static final String SELECT_UNIT_BY_ID_URI = "/unitlifecycles/" + FAKE_UNIT_LF_ID;
    private static final String SELECT_OBG_BY_ID_URI = "/objectgrouplifecycles/" + FAKE_OBG_LF_ID;

    private static final String LIFECYCLE_SAMPLE =
        "{'_id': 'aeaaaaaaaaaam7mxabaloakxrzydxbiaaaaq', 'evId': 'aeaaaaaaaaaam7mxabaloakxrzydxbyaaaaq', " +
            "'evType': ''," + " 'events': []}";

    private static int databasePort;
    private static int serverPort;
    private static LogbookApplication application;

    private static LogbookLifeCycleUnitParameters logbookLifeCyclesUnitParametersStart;
    private static LogbookLifeCycleUnitParameters logbookLifeCyclesUnitParametersBAD;
    private static LogbookLifeCycleUnitParameters logbookLifeCyclesUnitParametersUpdate;

    private static LogbookLifeCycleObjectGroupParameters LogbookLifeCycleObjectGroupParametersStart;

    private static JunitHelper junitHelper;

    private static final Integer TENANT_ID = 0;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // Identify overlapping in particular jsr311
        new JHades().overlappingJarsReport();


        junitHelper = JunitHelper.getInstance();
        databasePort = junitHelper.findAvailablePort();

        final MongodStarter starter = MongodStarter.getDefaultInstance();
        mongodExecutable = starter.prepare(new MongodConfigBuilder()
            .version(Version.Main.PRODUCTION)
            .net(new Net(databasePort, Network.localhostIsIPv6()))
            .build());

        mongod = mongodExecutable.start();
        serverPort = junitHelper.findAvailablePort();

        // TODO P1 verifier la compatibilité avec les tests parallèles sur jenkins
        JunitHelper.setJettyPortSystemProperty(serverPort);

        try {
            final LogbookConfiguration logbookConf = new LogbookConfiguration();
            final List<MongoDbNode> nodes = new ArrayList<>();
            nodes.add(new MongoDbNode(SERVER_HOST, databasePort));
            logbookConf.setDbName("vitam-test").setMongoDbNodes(nodes);
            logbookConf.setJettyConfig(JETTY_CONFIG);
            logbookConf.setP12LogbookFile("tsa.p12");
            logbookConf.setP12LogbookPassword("1234");
            logbookConf.setWorkspaceUrl("http://localhost:8001");

            application = new LogbookApplication(logbookConf);
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


        logbookLifeCyclesUnitParametersStart = LogbookParametersFactory.newLogbookLifeCycleUnitParameters();
        logbookLifeCyclesUnitParametersStart.setStatus(StatusCode.STARTED);
        logbookLifeCyclesUnitParametersStart.putParameterValue(LogbookParameterName.eventIdentifier,
            eip.toString());
        logbookLifeCyclesUnitParametersStart.putParameterValue(LogbookParameterName.eventIdentifierProcess,
            iop.toString());
        logbookLifeCyclesUnitParametersStart.putParameterValue(LogbookParameterName.objectIdentifier,
            ioL.toString());
        /**
         * Bad request
         */
        logbookLifeCyclesUnitParametersBAD = LogbookParametersFactory.newLogbookLifeCycleUnitParameters();

        logbookLifeCyclesUnitParametersBAD.putParameterValue(LogbookParameterName.eventIdentifier,
            eip.toString());
        logbookLifeCyclesUnitParametersBAD.putParameterValue(LogbookParameterName.eventIdentifierProcess,
            iop.toString());
        logbookLifeCyclesUnitParametersBAD.putParameterValue(LogbookParameterName.objectIdentifier,
            ioL.toString());
        /**
         * update
         *
         */
        final GUID eip2 = GUIDFactory.newWriteLogbookGUID(0);
        final GUID iop2 = GUIDFactory.newWriteLogbookGUID(0);
        final GUID ioL2 = GUIDFactory.newUnitGUID(0);
        logbookLifeCyclesUnitParametersUpdate = LogbookParametersFactory.newLogbookLifeCycleUnitParameters();
        logbookLifeCyclesUnitParametersUpdate.setStatus(StatusCode.STARTED);
        logbookLifeCyclesUnitParametersUpdate.putParameterValue(LogbookParameterName.eventIdentifier,
            eip2.toString());
        logbookLifeCyclesUnitParametersUpdate.putParameterValue(LogbookParameterName.eventIdentifierProcess,
            iop2.toString());
        logbookLifeCyclesUnitParametersUpdate.putParameterValue(LogbookParameterName.objectIdentifier,
            ioL2.toString());

        /**
         * Start ObjectGroup
         */

        LogbookLifeCycleObjectGroupParametersStart =
            LogbookParametersFactory.newLogbookLifeCycleObjectGroupParameters();

        LogbookLifeCycleObjectGroupParametersStart =
            LogbookParametersFactory.newLogbookLifeCycleObjectGroupParameters();
        LogbookLifeCycleObjectGroupParametersStart.setStatus(StatusCode.STARTED);
        LogbookLifeCycleObjectGroupParametersStart.putParameterValue(LogbookParameterName.eventIdentifier,
            eip.toString());
        LogbookLifeCycleObjectGroupParametersStart.putParameterValue(LogbookParameterName.eventIdentifierProcess,
            iop.toString());
        LogbookLifeCycleObjectGroupParametersStart.putParameterValue(LogbookParameterName.objectIdentifier,
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
        mongod.stop();
        mongodExecutable.stop();
        junitHelper.releasePort(databasePort);
        junitHelper.releasePort(serverPort);
    }

    @Test
    public final void given_lifeCycleUnit_when_create_update_test() {
        // Creation OK

        logbookLifeCyclesUnitParametersStart.putParameterValue(LogbookParameterName.eventType, "event");
        logbookLifeCyclesUnitParametersStart.setTypeProcess(LogbookTypeProcess.INGEST);
        logbookLifeCyclesUnitParametersStart.putParameterValue(LogbookParameterName.outcomeDetail, "outcomeDetail");
        logbookLifeCyclesUnitParametersStart.putParameterValue(LogbookParameterName.outcomeDetailMessage,
            "outcomeDetailMessage");
        logbookLifeCyclesUnitParametersStart.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());
        logbookLifeCyclesUnitParametersStart.putParameterValue(LogbookParameterName.agentIdentifier,
            ServerIdentity.getInstance().getJsonIdentity());
        given()
            .contentType(ContentType.JSON)
            .body(logbookLifeCyclesUnitParametersStart.toString())
            .when()
            .post(LIFE_UNIT_ID_URI,
                logbookLifeCyclesUnitParametersStart.getParameterValue(LogbookParameterName.eventIdentifierProcess),
                logbookLifeCyclesUnitParametersStart.getParameterValue(LogbookParameterName.objectIdentifier))
            .then()
            .statusCode(Status.CREATED.getStatusCode());

        // already exsits
        given()
            .contentType(ContentType.JSON)
            .body(logbookLifeCyclesUnitParametersStart.toString())
            .when()
            .post(LIFE_UNIT_ID_URI,
                logbookLifeCyclesUnitParametersStart.getParameterValue(LogbookParameterName.eventIdentifierProcess),
                logbookLifeCyclesUnitParametersStart.getParameterValue(LogbookParameterName.objectIdentifier))
            .then()
            .statusCode(Status.CONFLICT.getStatusCode());

        // incoherence parameters ; response bad_request
        given()
            .contentType(ContentType.JSON)
            .body(logbookLifeCyclesUnitParametersStart.toString())
            .when()
            .post(LIFE_UNIT_ID_URI,
                logbookLifeCyclesUnitParametersStart.getParameterValue(LogbookParameterName.eventIdentifierProcess),
                "bad_id")
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());


        // update ok
        logbookLifeCyclesUnitParametersStart.putParameterValue(LogbookParameterName.outcomeDetailMessage,
            "ModifiedoutcomeDetailMessage");
        logbookLifeCyclesUnitParametersStart.setStatus(StatusCode.OK);
        given()
            .contentType(ContentType.JSON)
            .body(logbookLifeCyclesUnitParametersStart.toString())
            .when()
            .put(LIFE_UNIT_ID_URI,
                logbookLifeCyclesUnitParametersStart.getParameterValue(LogbookParameterName.eventIdentifierProcess),
                logbookLifeCyclesUnitParametersStart.getParameterValue(LogbookParameterName.objectIdentifier))
            .then()
            .statusCode(Status.OK.getStatusCode());

        // Update illegal argument incoherence parameters ; response bad_request
        given()
            .contentType(ContentType.JSON)
            .body(logbookLifeCyclesUnitParametersStart.toString())
            .when()
            .put(LIFE_UNIT_ID_URI,
                logbookLifeCyclesUnitParametersStart.getParameterValue(LogbookParameterName.eventIdentifierProcess),
                "bad_id")
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());

        // Commit the created unit lifeCycle
        given()
            .when()
            .put(COMMIT_UNIT_ID_URI,
                logbookLifeCyclesUnitParametersStart.getParameterValue(LogbookParameterName.eventIdentifierProcess),
                logbookLifeCyclesUnitParametersStart.getParameterValue(LogbookParameterName.objectIdentifier))
            .then()
            .statusCode(Status.OK.getStatusCode());

        // Test direct access
        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(new Select().getFinalSelect())
            .when()
            .get("/unitlifecycles/" +
                logbookLifeCyclesUnitParametersStart.getParameterValue(LogbookParameterName.objectIdentifier))
            .then()
            .statusCode(Status.OK.getStatusCode());

        // Test Iterator
        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(new Select().getFinalSelect()).header(GlobalDataRest.X_CURSOR, true)
            .when()
            .get(LIFE_UNIT_URI,
                logbookLifeCyclesUnitParametersStart
                    .getParameterValue(LogbookParameterName.eventIdentifierProcess))
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
    public final void given_lifeCycleUnitWithoutMandotoryParams_when_create_thenReturn_BAD_RESUEST() {
        final GUID guidTest = GUIDFactory.newWriteLogbookGUID(0);

        logbookLifeCyclesUnitParametersBAD.putParameterValue(LogbookParameterName.objectIdentifier,
            guidTest.toString());
        given()
            .contentType(ContentType.JSON)
            .body(logbookLifeCyclesUnitParametersBAD.toString())
            .when()
            .post(LIFE_UNIT_ID_URI,
                logbookLifeCyclesUnitParametersBAD.getParameterValue(LogbookParameterName.eventIdentifierProcess),
                logbookLifeCyclesUnitParametersBAD.getParameterValue(LogbookParameterName.objectIdentifier))
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
    }


    @Test
    public final void given_lifeCycleUnitWithoutMandotoryParams_when_Update_thenReturn_BAD_RESUEST() {
        final GUID guidTest = GUIDFactory.newWriteLogbookGUID(0);

        logbookLifeCyclesUnitParametersBAD.putParameterValue(LogbookParameterName.objectIdentifier,
            guidTest.toString());
        given()
            .contentType(ContentType.JSON)
            .body(logbookLifeCyclesUnitParametersBAD.toString())
            .when()
            .put(LIFE_UNIT_ID_URI,
                logbookLifeCyclesUnitParametersBAD.getParameterValue(LogbookParameterName.eventIdentifierProcess),
                logbookLifeCyclesUnitParametersBAD.getParameterValue(LogbookParameterName.objectIdentifier))
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
    }


    @Test
    public final void deleteUnit_PassTheRightArgument_ResponseOK() {
        // Delete OK
        logbookLifeCyclesUnitParametersStart.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());
        logbookLifeCyclesUnitParametersStart.putParameterValue(LogbookParameterName.agentIdentifier,
            ServerIdentity.getInstance().getJsonIdentity());

        logbookLifeCyclesUnitParametersStart.putParameterValue(LogbookParameterName.eventType, "event");
        logbookLifeCyclesUnitParametersStart.setTypeProcess(LogbookTypeProcess.INGEST);
        logbookLifeCyclesUnitParametersStart.putParameterValue(LogbookParameterName.outcomeDetail, "outcomeDetail");
        logbookLifeCyclesUnitParametersStart.putParameterValue(LogbookParameterName.outcomeDetailMessage,
            "outcomeDetailMessage");
        logbookLifeCyclesUnitParametersStart.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());
        logbookLifeCyclesUnitParametersStart.putParameterValue(LogbookParameterName.agentIdentifier,
            ServerIdentity.getInstance().getJsonIdentity());
        given()
            .contentType(ContentType.JSON)
            .body(logbookLifeCyclesUnitParametersStart.toString())
            .when()
            .post(LIFE_UNIT_ID_URI,
                logbookLifeCyclesUnitParametersStart.getParameterValue(LogbookParameterName.eventIdentifierProcess),
                logbookLifeCyclesUnitParametersStart.getParameterValue(LogbookParameterName.objectIdentifier))
            .then()
            .statusCode(Status.CREATED.getStatusCode());
        given()
            .when()
            .delete(LIFE_UNIT_ID_URI,
                logbookLifeCyclesUnitParametersStart.getParameterValue(LogbookParameterName.eventIdentifierProcess),
                logbookLifeCyclesUnitParametersStart.getParameterValue(LogbookParameterName.objectIdentifier))
            .then()
            .statusCode(Status.OK.getStatusCode());
    }

    @Test
    public final void deleteObjectGroup_PassWrongAgument_thenNotFound() {
        // Delete KO
        LogbookLifeCycleObjectGroupParametersStart.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());
        LogbookLifeCycleObjectGroupParametersStart.putParameterValue(LogbookParameterName.agentIdentifier,
            ServerIdentity.getInstance().getJsonIdentity());
        given()
            .contentType(ContentType.JSON)
            .when()
            .delete(LIFE_UNIT_ID_URI,
                LogbookLifeCycleObjectGroupParametersStart
                    .getParameterValue(LogbookParameterName.eventIdentifierProcess),
                "notExists")
            .then()
            .statusCode(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public final void given_lifeCycleUnit_when_update_notfound() {
        // update notFound
        logbookLifeCyclesUnitParametersUpdate.putParameterValue(LogbookParameterName.eventType, "event");
        logbookLifeCyclesUnitParametersUpdate.setTypeProcess(LogbookTypeProcess.INGEST);
        logbookLifeCyclesUnitParametersUpdate.putParameterValue(LogbookParameterName.outcomeDetail, "outcomeDetail");
        logbookLifeCyclesUnitParametersUpdate.putParameterValue(LogbookParameterName.outcomeDetailMessage,
            "outcomeDetailMessage");
        logbookLifeCyclesUnitParametersUpdate.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());
        logbookLifeCyclesUnitParametersUpdate.putParameterValue(LogbookParameterName.agentIdentifier,
            ServerIdentity.getInstance().getJsonIdentity());


        given()
            .contentType(ContentType.JSON)
            .body(logbookLifeCyclesUnitParametersUpdate.toString())
            .when()
            .put(LIFE_UNIT_ID_URI,
                logbookLifeCyclesUnitParametersUpdate.getParameterValue(LogbookParameterName.eventIdentifierProcess),
                logbookLifeCyclesUnitParametersUpdate.getParameterValue(LogbookParameterName.objectIdentifier))
            .then()
            .statusCode(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public final void deleteObjectGroup_PassTheRightArgument_ResponseOK() {
        // Delete OK
        LogbookLifeCycleObjectGroupParametersStart.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());
        LogbookLifeCycleObjectGroupParametersStart.putParameterValue(LogbookParameterName.agentIdentifier,
            ServerIdentity.getInstance().getJsonIdentity());

        LogbookLifeCycleObjectGroupParametersStart.putParameterValue(LogbookParameterName.eventType, "event");
        LogbookLifeCycleObjectGroupParametersStart.setTypeProcess(LogbookTypeProcess.INGEST);
        LogbookLifeCycleObjectGroupParametersStart.putParameterValue(LogbookParameterName.outcomeDetail,
            "outcomeDetail");
        LogbookLifeCycleObjectGroupParametersStart.putParameterValue(LogbookParameterName.outcomeDetailMessage,
            "outcomeDetailMessage");
        LogbookLifeCycleObjectGroupParametersStart.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());
        LogbookLifeCycleObjectGroupParametersStart.putParameterValue(LogbookParameterName.agentIdentifier,
            ServerIdentity.getInstance().getJsonIdentity());
        given()
            .contentType(ContentType.JSON)
            .body(LogbookLifeCycleObjectGroupParametersStart.toString())
            .when()
            .post(LIFE_OG_ID_URI,
                LogbookLifeCycleObjectGroupParametersStart
                    .getParameterValue(LogbookParameterName.eventIdentifierProcess),
                LogbookLifeCycleObjectGroupParametersStart.getParameterValue(LogbookParameterName.objectIdentifier))
            .then()
            .statusCode(Status.CREATED.getStatusCode());

        given()
            .when()
            .delete(LIFE_OG_ID_URI,
                LogbookLifeCycleObjectGroupParametersStart
                    .getParameterValue(LogbookParameterName.eventIdentifierProcess),
                LogbookLifeCycleObjectGroupParametersStart.getParameterValue(LogbookParameterName.objectIdentifier))
            .then()
            .statusCode(Status.OK.getStatusCode());
    }

    @Test
    public final void given_lifeCycleObjectGroupWithoutMandotoryParams_when_delete_thenReturn_BAD_RESUEST() {
        // Delete OK

        given()
            .when()
            .delete(LIFE_OG_ID_URI, "id", "f12")
            .then()
            .statusCode(Status.NOT_FOUND.getStatusCode());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetUnitLifeCycleByIdThenOkWhenLogbookNotFoundException()
        throws LogbookDatabaseException, LogbookNotFoundException, InvalidParseOperationException {
        final LogbookLifeCycles logbookLifeCycles = Mockito.mock(LogbookLifeCyclesImpl.class);
        JsonNode query = JsonHandler.getFromString(LIFECYCLE_SAMPLE);
        when(logbookLifeCycles.getUnitById(query)).thenThrow(LogbookNotFoundException.class);
        given().contentType(ContentType.JSON).body(new Select().getFinalSelect())
            .param("id_lc", FAKE_UNIT_LF_ID).expect().statusCode(Status.NOT_FOUND.getStatusCode())
            .when().get(SELECT_UNIT_BY_ID_URI);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetObjectGroupLifeCycleByIdThenOkWhenLogbookNotFoundException()
        throws LogbookDatabaseException, LogbookNotFoundException {
        final LogbookLifeCycles logbookLifeCycles = Mockito.mock(LogbookLifeCyclesImpl.class);
        when(logbookLifeCycles.getObjectGroupById(FAKE_OBG_LF_ID)).thenThrow(LogbookNotFoundException.class);
        given().contentType(ContentType.JSON).body(new Select().getFinalSelect()).param("id_lc", FAKE_OBG_LF_ID).expect().statusCode(Status.NOT_FOUND.getStatusCode()).when()
            .get(SELECT_OBG_BY_ID_URI);
    }
}
