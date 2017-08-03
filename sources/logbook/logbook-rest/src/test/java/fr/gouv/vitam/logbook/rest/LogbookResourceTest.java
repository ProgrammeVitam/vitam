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

import static com.jayway.restassured.RestAssured.get;
import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.RestAssured.with;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.ws.rs.core.Response.Status;

import org.jhades.JHades;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
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
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.ServerIdentity;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.junit.JunitHelper.ElasticsearchTestConfiguration;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.LogbookConfiguration;
import fr.gouv.vitam.logbook.common.server.LogbookDbAccess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbAccessFactory;

public class LogbookResourceTest {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogbookResourceTest.class);

    private static final String LOGBOOK_CONF = "logbook-test.conf";
    private static final String DATABASE_HOST = "localhost";
    private static final String DATABASE_NAME = "vitam-test";
    private static LogbookDbAccess mongoDbAccess;
    private static MongodExecutable mongodExecutable;
    private static MongodProcess mongod;

    // ES
    @ClassRule
    public static TemporaryFolder temporaryFolder = new TemporaryFolder();
    private final static String ES_CLUSTER_NAME = "vitam-cluster";
    private final static String ES_HOST_NAME = "localhost";
    private static ElasticsearchTestConfiguration config = null;

    private static final String REST_URI = "/logbook/v1";
    private static final String OPERATIONS_URI = "/operations";
    private static final String OPERATION_ID_URI = "/{id_op}";
    private static final String STATUS_URI = "/status";
    private static final String TRACEABILITY_URI = "/operations/traceability";
    private static int databasePort;
    private static int serverPort;
    private static LogbookMain application;

    private static LogbookOperationParameters logbookParametersStart;
    private static LogbookOperationParameters logbookParametersAppend;
    private static LogbookOperationParameters logbookParametersWrongStart;
    private static LogbookOperationParameters logbookParametersWrongAppend;
    private static LogbookOperationParameters logbookParametersSelect;
    private static LogbookOperationParameters logbookParametersSelectId;
    private static final String BODY_TEST = "{$query: {$eq: {\"aa\" : \"vv\" }}, $projection: {}, $filter: {}}";
    private static final String BODY_QUERY =
        "{$query: {$eq: {\"evType\" : \"eventTypeValueSelect\"}}, $projection: {}, $filter: {}}";
    public static String X_HTTP_METHOD_OVERRIDE = "X-HTTP-Method-Override";
    private static JunitHelper junitHelper;
    private static LogbookConfiguration realLogbook;

    private static final int TENANT_ID = 0;
    private static final List<Integer> tenantList = Arrays.asList(0);

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // Identify overlapping in particular jsr311
        new JHades().overlappingJarsReport();

        junitHelper = JunitHelper.getInstance();
        databasePort = junitHelper.findAvailablePort();
        final File logbook = PropertiesUtils.findFile(LOGBOOK_CONF);
        realLogbook = PropertiesUtils.readYaml(logbook, LogbookConfiguration.class);
        realLogbook.getMongoDbNodes().get(0).setDbPort(databasePort);
        final MongodStarter starter = MongodStarter.getDefaultInstance();
        mongodExecutable = starter.prepare(new MongodConfigBuilder()
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
        realLogbook.getElasticsearchNodes().get(0).setTcpPort(config.getTcpPort());

        final List<MongoDbNode> nodes = new ArrayList<>();
        nodes.add(new MongoDbNode(DATABASE_HOST, databasePort));
        final List<ElasticsearchNode> esNodes = new ArrayList<>();
        esNodes.add(new ElasticsearchNode(ES_HOST_NAME, config.getTcpPort()));
        LogbookConfiguration logbookConfiguration =
            new LogbookConfiguration(nodes, DATABASE_NAME, ES_CLUSTER_NAME, esNodes);
        logbookConfiguration.setTenants(tenantList);
        mongoDbAccess = LogbookMongoDbAccessFactory.create(logbookConfiguration);
        serverPort = junitHelper.findAvailablePort();

        // TODO P1 verifier la compatibilité avec les tests parallèles sur jenkins

        RestAssured.port = serverPort;
        RestAssured.basePath = REST_URI;

        File file = temporaryFolder.newFile();
        String configurationFile = file.getAbsolutePath();
        PropertiesUtils.writeYaml(file, realLogbook);


        try {
            application = new LogbookMain(configurationFile);
            application.start();
            JunitHelper.unsetJettyPortSystemProperty();
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
            throw new IllegalStateException(
                "Cannot start the Logbook Application Server", e);
        }

        final GUID eip = GUIDFactory.newEventGUID(0);

        logbookParametersStart = LogbookParametersFactory.newLogbookOperationParameters(
            eip, "eventTypeValue1", eip, LogbookTypeProcess.INGEST,
            StatusCode.STARTED, "start ingest", eip);
        logbookParametersAppend = LogbookParametersFactory.newLogbookOperationParameters(
            GUIDFactory.newEventGUID(0),
            "eventTypeValue1", eip, LogbookTypeProcess.INGEST,
            StatusCode.OK, "end ingest", eip);
        logbookParametersWrongStart = LogbookParametersFactory.newLogbookOperationParameters(
            eip,
            "eventTypeValue2", eip, LogbookTypeProcess.INGEST,
            StatusCode.STARTED, "start ingest", eip);
        logbookParametersWrongAppend = LogbookParametersFactory.newLogbookOperationParameters(
            GUIDFactory.newEventGUID(0),
            "eventTypeValue2", GUIDFactory.newEventGUID(0), LogbookTypeProcess.INGEST,
            StatusCode.OK, "end ingest", eip);

        logbookParametersSelect = LogbookParametersFactory.newLogbookOperationParameters(
            eip, "eventTypeValueSelect", GUIDFactory.newEventGUID(0), LogbookTypeProcess.INGEST,
            StatusCode.OK, "start ingest", eip);

        logbookParametersSelectId = LogbookParametersFactory.newLogbookOperationParameters(
            eip, "eventTypeValueSelectId", GUIDFactory.newEventGUID(0), LogbookTypeProcess.INGEST,
            StatusCode.OK, "start ingest", eip);
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
        mongoDbAccess.close();
        if (config != null) {
            JunitHelper.stopElasticsearchForTest(config);
        }
        junitHelper.releasePort(serverPort);
        mongod.stop();
        mongodExecutable.stop();
        junitHelper.releasePort(databasePort);
    }

    @Test
    @Ignore("need a mock of workspace client factory")
    public final void testTraceability() {
        logbookParametersAppend.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());
        logbookParametersAppend.putParameterValue(LogbookParameterName.agentIdentifier,
            ServerIdentity.getInstance().getJsonIdentity());
        given()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(logbookParametersAppend)
            .post(TRACEABILITY_URI)
            .then()
            .statusCode(Status.OK.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public final void testOperation() {
        // Creation OK
        logbookParametersStart.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());
        logbookParametersStart.putParameterValue(LogbookParameterName.agentIdentifier,
            ServerIdentity.getInstance().getJsonIdentity());
        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
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
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
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
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
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
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(logbookParametersWrongAppend.toString())
            .when()
            .put(OPERATIONS_URI + OPERATION_ID_URI,
                logbookParametersWrongAppend.getParameterValue(
                    LogbookParameterName.eventIdentifierProcess))
            .then()
            .statusCode(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void testBulk() {
        final GUID eip = GUIDFactory.newEventGUID(0);
        // Create
        final LogbookOperationParameters start = LogbookParametersFactory.newLogbookOperationParameters(
            eip, "eventTypeValue1", eip, LogbookTypeProcess.INGEST,
            StatusCode.STARTED, "start ingest", eip);
        LogbookOperationParameters append = LogbookParametersFactory.newLogbookOperationParameters(
            GUIDFactory.newEventGUID(0),
            "eventTypeValue1", eip, LogbookTypeProcess.INGEST,
            StatusCode.OK, "end ingest", eip);
        final Queue<LogbookOperationParameters> queue = new ConcurrentLinkedQueue<>();
        queue.add(start);
        queue.add(append);
        append = LogbookParametersFactory.newLogbookOperationParameters(
            GUIDFactory.newEventGUID(0),
            "eventTypeValue1", eip, LogbookTypeProcess.INGEST,
            StatusCode.OK, "end ingest", eip);
        queue.add(append);
        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(JsonHandler.unprettyPrint(queue))
            .when()
            .post(OPERATIONS_URI)
            .then()
            .statusCode(Status.CREATED.getStatusCode());

        // Update
        queue.clear();
        append = LogbookParametersFactory.newLogbookOperationParameters(
            GUIDFactory.newEventGUID(0),
            "eventTypeValue1", eip, LogbookTypeProcess.INGEST,
            StatusCode.OK, "end ingest", eip);
        queue.add(append);
        append = LogbookParametersFactory.newLogbookOperationParameters(
            GUIDFactory.newEventGUID(0),
            "eventTypeValue1", eip, LogbookTypeProcess.INGEST,
            StatusCode.OK, "end ingest", eip);
        queue.add(append);
        append = LogbookParametersFactory.newLogbookOperationParameters(
            GUIDFactory.newEventGUID(0),
            "eventTypeValue1", eip, LogbookTypeProcess.INGEST,
            StatusCode.OK, "end ingest", eip);
        queue.add(append);
        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(JsonHandler.unprettyPrint(queue))
            .when()
            .put(OPERATIONS_URI)
            .then()
            .statusCode(Status.OK.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void testError() {
        // Create KO since Bad Request
        final LogbookOperationParameters empty = LogbookParametersFactory.newLogbookOperationParameters();
        final String id = GUIDFactory.newEventGUID(0).getId();
        empty.putParameterValue(LogbookParameterName.eventIdentifierProcess, id);
        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(empty.toString())
            .when()
            .post(OPERATIONS_URI + OPERATION_ID_URI, id)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(empty.toString())
            .when()
            .put(OPERATIONS_URI + OPERATION_ID_URI, id)
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(logbookParametersWrongStart.toString())
            .when()
            .post(OPERATIONS_URI + OPERATION_ID_URI,
                GUIDFactory.newEventGUID(0).getId())
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(logbookParametersWrongAppend.toString())
            .when()
            .put(OPERATIONS_URI + OPERATION_ID_URI,
                GUIDFactory.newEventGUID(0).getId())
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public final void testGetStatus() {
        get(STATUS_URI).then().statusCode(Status.NO_CONTENT.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void testOperationSelect() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        logbookParametersSelect.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());
        logbookParametersSelect.putParameterValue(LogbookParameterName.agentIdentifier,
            ServerIdentity.getInstance().getJsonIdentity());
        with()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(logbookParametersSelect.toString())
            .when()
            .post(OPERATIONS_URI + OPERATION_ID_URI,
                logbookParametersSelect.getParameterValue(
                    LogbookParameterName.eventIdentifierProcess))
            .then()
            .statusCode(Status.CREATED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(JsonHandler.getFromString(BODY_QUERY))
            .when()
            .get(OPERATIONS_URI)
            .then()
            .statusCode(Status.OK.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void testSelectOperationId() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        logbookParametersSelectId.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());
        logbookParametersSelectId.putParameterValue(LogbookParameterName.agentIdentifier,
            ServerIdentity.getInstance().getJsonIdentity());
        with()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .contentType(ContentType.JSON)
            .body(logbookParametersSelectId.toString())
            .pathParam("id_op",
                logbookParametersSelectId.getParameterValue(LogbookParameterName.eventIdentifierProcess))
            .when()
            .post(OPERATIONS_URI + OPERATION_ID_URI,
                logbookParametersSelectId.getParameterValue(
                    LogbookParameterName.eventIdentifierProcess))
            .then()
            .statusCode(Status.CREATED.getStatusCode());

        given()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .contentType(ContentType.JSON)
            .body(JsonHandler.getFromString(BODY_QUERY))
            .when()
            .get(OPERATIONS_URI + OPERATION_ID_URI, logbookParametersSelectId.getParameterValue(
                LogbookParameterName.eventIdentifierProcess))
            .then()
            .statusCode(Status.OK.getStatusCode());
    }


}
