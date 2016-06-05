package fr.gouv.vitam.logbook.rest;

import static com.jayway.restassured.RestAssured.get;
import static com.jayway.restassured.RestAssured.given;

import java.io.FileNotFoundException;

import javax.ws.rs.core.Response.Status;

import org.jhades.JHades;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.ServerIdentity;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.BasicVitamServer;
import fr.gouv.vitam.common.server.VitamServer;
import fr.gouv.vitam.common.server.application.configuration.DbConfigurationImpl;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookOutcome;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.MongoDbAccess;
import fr.gouv.vitam.logbook.common.server.database.collections.MongoDbAccessFactory;

public class LogbookResourceTest {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogbookResourceTest.class);

    private static final String LOGBOOK_CONF = "logbook.conf";
    private static final String DATABASE_HOST = "localhost";
    private static final int DATABASE_PORT = 12346;
    private static MongoDbAccess mongoDbAccess;
    private static MongodExecutable mongodExecutable;
    private static MongodProcess mongod;
    private static VitamServer vitamServer;

    private static final String REST_URI = "/logbook/v1";
    private static final int SERVER_PORT = 56789;
    private static final String OPERATIONS_URI = "/operations";
    private static final String OPERATION_ID_URI = "/{id_op}";
    private static final String STATUS_URI = "/status";

    private static LogbookOperationParameters logbookParametersStart;
    private static LogbookOperationParameters logbookParametersAppend;
    private static LogbookOperationParameters logbookParametersWrongStart;
    private static LogbookOperationParameters logbookParametersWrongAppend;

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
        mongoDbAccess =
            MongoDbAccessFactory.create(
                new DbConfigurationImpl(DATABASE_HOST, DATABASE_PORT,
                    "vitam-test"));


        RestAssured.port = SERVER_PORT;
        RestAssured.basePath = REST_URI;

        try {
            vitamServer = LogbookApplication.startApplication(new String[] {
                PropertiesUtils.getResourcesFile(LOGBOOK_CONF).getAbsolutePath(),
                Integer.toString(SERVER_PORT)});
            ((BasicVitamServer) vitamServer).start();
        } catch (FileNotFoundException | VitamApplicationServerException e) {
            LOGGER.error(e);
            throw new IllegalStateException(
                "Cannot start the Logbook Application Server", e);
        }

        final GUID eip = GUIDFactory.newOperationIdGUID(0);
        logbookParametersStart = LogbookParametersFactory.newLogbookOperationParameters(
            eip.getId(),
            "eventTypeValue1", eip.getId(), LogbookTypeProcess.INGEST,
            LogbookOutcome.STARTED, "start ingest", "x-request-id");
        logbookParametersAppend = LogbookParametersFactory.newLogbookOperationParameters(
            GUIDFactory.newOperationIdGUID(0).getId(),
            "eventTypeValue1", eip.getId(), LogbookTypeProcess.INGEST,
            LogbookOutcome.OK, "end ingest", "x-request-id");
        logbookParametersWrongStart = LogbookParametersFactory.newLogbookOperationParameters(
            eip.getId(),
            "eventTypeValue2", eip.getId(), LogbookTypeProcess.INGEST,
            LogbookOutcome.STARTED, "start ingest", "x-request-id");
        logbookParametersWrongAppend = LogbookParametersFactory.newLogbookOperationParameters(
            GUIDFactory.newOperationIdGUID(0).getId(),
            "eventTypeValue2", GUIDFactory.newOperationIdGUID(0).getId(), LogbookTypeProcess.INGEST,
            LogbookOutcome.OK, "end ingest", "x-request-id");
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        LOGGER.debug("Ending tests");
        try {
            ((BasicVitamServer) vitamServer).stop();
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
        }
        mongoDbAccess.close();
        mongod.stop();
        mongodExecutable.stop();
    }


    @Test
    public final void testOperation() {
        // Creation OK
        logbookParametersStart.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());
        logbookParametersStart.putParameterValue(LogbookParameterName.agentIdentifier,
            ServerIdentity.getInstance().getJsonIdentity());
        given()
            .contentType(ContentType.JSON)
            .body(logbookParametersStart.toString())
            .when()
            .post(OPERATIONS_URI + OPERATION_ID_URI,
                logbookParametersStart.getParameterValue(
                    LogbookParameterName.eventIdentifierProcess))
            .then()
            .statusCode(Status.CREATED.getStatusCode());
        // Update OK
        logbookParametersAppend.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());
        logbookParametersAppend.putParameterValue(LogbookParameterName.agentIdentifier,
            ServerIdentity.getInstance().getJsonIdentity());
        given()
            .contentType(ContentType.JSON)
            .body(logbookParametersAppend.toString())
            .when()
            .put(OPERATIONS_URI + OPERATION_ID_URI,
                logbookParametersAppend.getParameterValue(
                    LogbookParameterName.eventIdentifierProcess))
            .then()
            .statusCode(Status.OK.getStatusCode());
        // Create KO since already exists
        logbookParametersWrongStart.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());
        logbookParametersWrongStart.putParameterValue(LogbookParameterName.agentIdentifier,
            ServerIdentity.getInstance().getJsonIdentity());
        given()
            .contentType(ContentType.JSON)
            .body(logbookParametersWrongStart.toString())
            .when()
            .post(OPERATIONS_URI + OPERATION_ID_URI,
                logbookParametersWrongStart.getParameterValue(
                    LogbookParameterName.eventIdentifierProcess))
            .then()
            .statusCode(Status.CONFLICT.getStatusCode());
        // Update KO since not found
        logbookParametersWrongAppend.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());
        logbookParametersWrongAppend.putParameterValue(LogbookParameterName.agentIdentifier,
            ServerIdentity.getInstance().getJsonIdentity());
        given()
            .contentType(ContentType.JSON)
            .body(logbookParametersWrongAppend.toString())
            .when()
            .put(OPERATIONS_URI + OPERATION_ID_URI,
                logbookParametersWrongAppend.getParameterValue(
                    LogbookParameterName.eventIdentifierProcess))
            .then()
            .statusCode(Status.NOT_FOUND.getStatusCode());
    }
    
    @Test
    public void testError() {
        // Create KO since Bad Request
        final LogbookOperationParameters empty = LogbookParametersFactory.newLogbookOperationParameters();
        String id = GUIDFactory.newOperationIdGUID(0).getId();
        empty.putParameterValue(LogbookParameterName.eventIdentifierProcess, id);
        given()
            .contentType(ContentType.JSON)
            .body(empty.toString())
            .when()
            .post(OPERATIONS_URI + OPERATION_ID_URI, id)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
        given()
            .contentType(ContentType.JSON)
            .body(empty.toString())
            .when()
            .put(OPERATIONS_URI + OPERATION_ID_URI, id)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
        given()
            .contentType(ContentType.JSON)
            .body(logbookParametersWrongStart.toString())
            .when()
            .post(OPERATIONS_URI + OPERATION_ID_URI,
                GUIDFactory.newOperationIdGUID(0).getId())
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
        given()
            .contentType(ContentType.JSON)
            .body(logbookParametersWrongAppend.toString())
            .when()
            .put(OPERATIONS_URI + OPERATION_ID_URI,
                GUIDFactory.newOperationIdGUID(0).getId())
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public final void testGetStatus() {
        get(STATUS_URI).then().statusCode(200);
    }

}
