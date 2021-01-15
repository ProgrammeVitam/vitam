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
package fr.gouv.vitam.logbook.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.google.common.collect.Lists;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.ServerIdentity;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.database.parameter.IndexParameters;
import fr.gouv.vitam.common.database.parameter.SwitchIndexParameters;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.LifeCycleStatusCode;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.model.LifecycleTraceabilityStatus;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.config.ElasticsearchLogbookIndexManager;
import fr.gouv.vitam.logbook.common.server.config.LogbookConfiguration;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollectionsTestUtils;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookElasticsearchAccess;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.with;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

@RunWithCustomExecutor
public class LogbookResourceTest {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogbookResourceTest.class);

    private static final String PREFIX = GUIDFactory.newGUID().getId();

    @ClassRule
    public static MongoRule mongoRule =
        new MongoRule(VitamCollection.getMongoClientOptions());

    @ClassRule
    public static ElasticsearchRule elasticsearchRule = new ElasticsearchRule();
    private static final String LIFECYCLES_TRACEABILITY_CHECK = "/lifecycles/traceability/check/";

    @Rule
    @ClassRule
    public static RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor());


    private static final String LOGBOOK_CONF = "logbook-test.conf";
    @ClassRule
    public static TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static final String REST_URI = "/logbook/v1";
    private static final String OPERATIONS_URI = "/operations";
    private static final String OPERATION_ID_URI = "/{id_op}";
    private static final String STATUS_URI = "/status";
    private static final String UNIT_LIFECYCLES = "/unitlifecycles";
    private static final String TRACEABILITY_URI = "/operations/traceability";
    private static final String OBJECT_GROUP_LFC_TRACEABILITY_URI = "/lifecycles/units/traceability";
    private static final String UNIT_LFC_TRACEABILITY_URI = "/lifecycles/objectgroups/traceability";
    private static final String CHECK_LOGBOOK_COHERENCE_URI = "/checklogbook";

    private static int serverPort;
    private static LogbookMain application;

    private static LogbookOperationParameters logbookParametersStart;
    private static LogbookOperationParameters logbookParametersAppend;
    private static LogbookOperationParameters logbookParametersWrongStart;
    private static LogbookOperationParameters logbookParametersWrongAppend;
    private static LogbookOperationParameters logbookParametersSelect;
    private static LogbookOperationParameters logbookParametersSelectId;
    private static final String BODY_QUERY =
        "{$query: {$eq: {\"evType\" : \"eventTypeValueSelect\"}}, $projection: {}, $filter: {}}";

    private static final String BODY_QUERY_1 =
        "{$query: {$eq: {\"evType\" : \"eventTypeValueSelectId\"}}, $projection: {}, $filter: {}}";
    private static JunitHelper junitHelper = JunitHelper.getInstance();
    private static final String FILE_QUERY_OFFSET_LIMIT = "logbook_request_offset_limit.json";


    private static LogbookConfiguration realLogbook;

    private static final int TENANT_ID = 0;
    private static final int ADMIN_TENANT_ID = 1;
    private static final List<Integer> tenantList = Collections.singletonList(TENANT_ID);
    private final static ElasticsearchLogbookIndexManager indexManager = LogbookCollectionsTestUtils
        .createTestIndexManager(tenantList, Collections.emptyMap());

    private static int workspacePort = junitHelper.findAvailablePort();
    private static int processingPort = junitHelper.findAvailablePort();

    @ClassRule
    public static WireMockClassRule workspaceWireMockRule = new WireMockClassRule(workspacePort);
    @Rule
    public WireMockClassRule workspaceInstanceRule = workspaceWireMockRule;
    @ClassRule
    public static WireMockClassRule processingWireMockRule = new WireMockClassRule(processingPort);
    @Rule
    public WireMockClassRule processingInstanceRule = processingWireMockRule;

    private static LogbookElasticsearchAccess elasticsearchAccess;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {

        final List<MongoDbNode> nodes = new ArrayList<>();
        nodes.add(new MongoDbNode("localhost", mongoRule.getDataBasePort()));
        List<ElasticsearchNode> esNodes =
            Lists.newArrayList(new ElasticsearchNode(ElasticsearchRule.getHost(), ElasticsearchRule.getPort()));

        elasticsearchAccess = new LogbookElasticsearchAccess(ElasticsearchRule.VITAM_CLUSTER, esNodes, indexManager);
        LogbookCollectionsTestUtils.beforeTestClass(mongoRule.getMongoDatabase(), PREFIX, elasticsearchAccess);

        final File logbook = PropertiesUtils.findFile(LOGBOOK_CONF);
        realLogbook = PropertiesUtils.readYaml(logbook, LogbookConfiguration.class);
        realLogbook.setElasticsearchNodes(esNodes);
        realLogbook.setClusterName(ElasticsearchRule.VITAM_CLUSTER);
        realLogbook.setMongoDbNodes(nodes);
        realLogbook.setDbName(MongoRule.VITAM_DB);
        realLogbook.setWorkspaceUrl("http://localhost:" + workspacePort);
        realLogbook.setProcessingUrl("http://localhost:" + processingPort);
        VitamConfiguration.setTenants(tenantList);
        VitamConfiguration.setAdminTenant(TENANT_ID);
        serverPort = junitHelper.findAvailablePort();

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

        logbookParametersStart = LogbookParameterHelper.newLogbookOperationParameters(
            eip, "eventTypeValue1", eip, LogbookTypeProcess.INGEST,
            StatusCode.STARTED, "start ingest", eip);
        logbookParametersAppend = LogbookParameterHelper.newLogbookOperationParameters(
            GUIDFactory.newEventGUID(0),
            "eventTypeValue1", eip, LogbookTypeProcess.INGEST,
            StatusCode.OK, "end ingest", eip);
        logbookParametersWrongStart = LogbookParameterHelper.newLogbookOperationParameters(
            eip,
            "eventTypeValue2", eip, LogbookTypeProcess.INGEST,
            StatusCode.STARTED, "start ingest", eip);
        logbookParametersWrongAppend = LogbookParameterHelper.newLogbookOperationParameters(
            GUIDFactory.newEventGUID(0),
            "eventTypeValue2", GUIDFactory.newEventGUID(0), LogbookTypeProcess.INGEST,
            StatusCode.OK, "end ingest", eip);

        logbookParametersSelect = LogbookParameterHelper.newLogbookOperationParameters(
            eip, "eventTypeValueSelect", GUIDFactory.newEventGUID(0), LogbookTypeProcess.INGEST,
            StatusCode.OK, "start ingest", eip);

        logbookParametersSelectId = LogbookParameterHelper.newLogbookOperationParameters(
            eip, "eventTypeValueSelectId", GUIDFactory.newEventGUID(0), LogbookTypeProcess.INGEST,
            StatusCode.OK, "start ingest", eip);
    }

    @Before
    public void setUp() {
        workspaceInstanceRule.stubFor(WireMock.post(WireMock.urlMatching("/workspace/v1/containers/(.*)")).willReturn
            (WireMock.aResponse().withStatus(201).withHeader(GlobalDataRest.X_TENANT_ID, Integer.toString(TENANT_ID))));
        workspaceInstanceRule.stubFor(WireMock.post(WireMock.urlMatching("/workspace/v1/containers/(.*)/objects/(.*)"))
            .willReturn(WireMock.aResponse().withStatus(201)
                .withHeader(GlobalDataRest.X_TENANT_ID, Integer.toString(TENANT_ID))));
        workspaceInstanceRule.stubFor(WireMock.delete(WireMock.urlMatching("/workspace/v1/containers/(.*)")).willReturn
            (WireMock.aResponse().withStatus(204).withHeader(GlobalDataRest.X_TENANT_ID, Integer.toString(TENANT_ID))));
        processingInstanceRule.stubFor(WireMock.post(WireMock.urlMatching("/processing/v1/operations/(.*)")).willReturn
            (WireMock.aResponse().withStatus(200)));
        processingInstanceRule.stubFor(WireMock.put(WireMock.urlMatching("/processing/v1/operations/(.*)")).willReturn
            (WireMock.aResponse().withStatus(202).withBody(JsonHandler.unprettyPrint(new RequestResponseOK<>()))
                .withHeader(GlobalDataRest.X_TENANT_ID, Integer.toString(TENANT_ID)).withHeader(HttpHeaders
                    .CONTENT_TYPE, MediaType.APPLICATION_JSON)));
    }

    @AfterClass
    public static void tearDownAfterClass() {
        LOGGER.debug("Ending tests");
        try {
            if (application != null) {
                application.stop();
            }
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
        }

        LogbookCollectionsTestUtils.afterTestClass(indexManager, true);

        junitHelper.releasePort(workspacePort);
        junitHelper.releasePort(processingPort);
        VitamClientFactory.resetConnections();
    }

    @After
    public void tearDown() {
        LogbookCollectionsTestUtils.afterTest(indexManager);
    }


    @Test
    public final void testTraceability() {
        given()
            .header(GlobalDataRest.X_TENANT_ID, ADMIN_TENANT_ID)
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(JsonHandler.unprettyPrint(Collections.singletonList(TENANT_ID)))
            .post(TRACEABILITY_URI)
            .then()
            .statusCode(Status.OK.getStatusCode());
    }

    @Test
    public final void testTraceabilityUnitLfc() {
        given()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .post(UNIT_LFC_TRACEABILITY_URI)
            .then()
            .statusCode(Status.OK.getStatusCode());
    }

    @Test
    public final void testTraceabilityObjectGroupLfc() {
        given()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .post(OBJECT_GROUP_LFC_TRACEABILITY_URI)
            .then()
            .statusCode(Status.OK.getStatusCode());
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
    public void testBulk() {
        final GUID eip = GUIDFactory.newEventGUID(0);
        // Create
        final LogbookOperationParameters start = LogbookParameterHelper.newLogbookOperationParameters(
            eip, "eventTypeValue1", eip, LogbookTypeProcess.INGEST,
            StatusCode.STARTED, "start ingest", eip);
        LogbookOperationParameters append = LogbookParameterHelper.newLogbookOperationParameters(
            GUIDFactory.newEventGUID(0),
            "eventTypeValue1", eip, LogbookTypeProcess.INGEST,
            StatusCode.OK, "end ingest", eip);
        final Queue<LogbookOperationParameters> queue = new ConcurrentLinkedQueue<>();
        queue.add(start);
        queue.add(append);
        append = LogbookParameterHelper.newLogbookOperationParameters(
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
        append = LogbookParameterHelper.newLogbookOperationParameters(
            GUIDFactory.newEventGUID(0),
            "eventTypeValue1", eip, LogbookTypeProcess.INGEST,
            StatusCode.OK, "end ingest", eip);
        queue.add(append);
        append = LogbookParameterHelper.newLogbookOperationParameters(
            GUIDFactory.newEventGUID(0),
            "eventTypeValue1", eip, LogbookTypeProcess.INGEST,
            StatusCode.OK, "end ingest", eip);
        queue.add(append);
        append = LogbookParameterHelper.newLogbookOperationParameters(
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
    public void testError() {
        // Create KO since Bad Request
        final LogbookOperationParameters empty = LogbookParameterHelper.newLogbookOperationParameters();
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
    public void should_getUnitLifeCyclesByOperation_status_ok() throws Exception {
        // Given
        // Create
        final GUID eip = GUIDFactory.newEventGUID(TENANT_ID);
        final LogbookOperationParameters start = LogbookParameterHelper.newLogbookOperationParameters(
            eip, "eventTypeValue1", eip, LogbookTypeProcess.INGEST,
            StatusCode.STARTED, "start ingest", eip);
        LogbookOperationParameters append = LogbookParameterHelper.newLogbookOperationParameters(
            GUIDFactory.newEventGUID(0),
            "eventTypeValue1", eip, LogbookTypeProcess.INGEST,
            StatusCode.OK, "end ingest", eip);
        final Queue<LogbookOperationParameters> queue = new ConcurrentLinkedQueue<>();
        queue.add(start);
        queue.add(append);
        append = LogbookParameterHelper.newLogbookOperationParameters(
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
        // When
        InputStream resourceAsStream = PropertiesUtils.getResourceAsStream(FILE_QUERY_OFFSET_LIMIT);
        JsonNode queryJson = JsonHandler.getFromInputStream(resourceAsStream);
        Response response = given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_EVENT_STATUS, LifeCycleStatusCode.LIFE_CYCLE_COMMITTED.toString())
            .body(queryJson)
            .when()
            .get(OPERATIONS_URI + OPERATION_ID_URI + UNIT_LIFECYCLES,
                eip.getId());
        // Then
        response.then().statusCode(Status.OK.getStatusCode());
    }

    @Test
    public void testOperationSelect() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        logbookParametersSelect.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());
        logbookParametersSelect.putParameterValue(LogbookParameterName.agentIdentifier,
            ServerIdentity.getInstance().getJsonIdentity());
        with()
            .contentType(ContentType.JSON)
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
            .body(JsonHandler.getFromString(BODY_QUERY_1))
            .when()
            .get(OPERATIONS_URI + OPERATION_ID_URI, logbookParametersSelectId.getParameterValue(
                LogbookParameterName.eventIdentifierProcess))
            .then()
            .statusCode(Status.OK.getStatusCode());
    }

    @Test
    public void indexCollectionNoBodyPreconditionFailed() {
        given().contentType(MediaType.APPLICATION_JSON)
            .when().post("/reindex").then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void indexCollectionUnknownInternalServerError() {
        IndexParameters indexParameters = new IndexParameters();
        List<Integer> tenants = Collections.singletonList(0);
        indexParameters.setTenants(tenants);
        indexParameters.setCollectionName("fake");

        given().contentType(MediaType.APPLICATION_JSON).body(indexParameters)
            .when().post("/reindex").then().statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
            .body("collectionName", equalTo("fake"))
            .body("KO.size()", equalTo(1))
            .body("KO.get(0).tenants.size()", equalTo(1))
            .body("KO.get(0).tenants.get(0)", equalTo(0))
            .body("KO.get(0).tenantGroup", equalTo(null))
            .body("KO.get(0).message", containsString("'fake'"));
    }

    @Test
    public void aliasCollectionNoBodyPreconditionFailed() {
        given().contentType(MediaType.APPLICATION_JSON)
            .when().post("/alias").then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void aliasUnkwownCollection() {
        SwitchIndexParameters parameters = new SwitchIndexParameters();
        parameters.setAlias("alias");
        parameters.setIndexName("indexName");
        given().contentType(MediaType.APPLICATION_JSON).body(parameters)
            .when().post("/alias").then().statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void testCheckLogbookCoherenceOK() {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        // call the endpoint logbook check coherence
        given().contentType(ContentType.JSON).
            header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(CHECK_LOGBOOK_COHERENCE_URI)
            .then().statusCode(javax.ws.rs.core.Response.Status.OK.getStatusCode());

    }

    @Test
    @RunWithCustomExecutor
    public void testCheckLifecycleTraceabilityStatus_NotFound() {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        processingInstanceRule.stubFor(WireMock.head(WireMock.urlMatching("/processing/v1/operations/(.*)")).willReturn
            (WireMock.aResponse().withStatus(404)));

        // call the endpoint logbook check coherence
        given().contentType(ContentType.JSON).
            header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().get(LIFECYCLES_TRACEABILITY_CHECK + "unkownid")
            .then().statusCode(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void testCheckLifecycleTraceabilityStatus_CompletedWithWarn() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        String operationId = "mockId";

        processingInstanceRule.stubFor(WireMock.head(WireMock.urlMatching("/processing/v1/operations/(.*)")).willReturn
            (WireMock.aResponse()
                .withStatus(200)
                .withHeader(GlobalDataRest.X_GLOBAL_EXECUTION_STATE, ProcessState.COMPLETED.name())
                .withHeader(GlobalDataRest.X_GLOBAL_EXECUTION_STATUS, StatusCode.WARNING.name())));

        // call the endpoint logbook check coherence
        JsonNode responseJson = given().contentType(ContentType.JSON).
            header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().get(LIFECYCLES_TRACEABILITY_CHECK + operationId)
            .then().statusCode(Status.OK.getStatusCode())
            .extract().body().as(JsonNode.class);

        LifecycleTraceabilityStatus status =
            JsonHandler.getFromJsonNode(responseJson.get("$results").get(0), LifecycleTraceabilityStatus.class);

        assertThat(status.isCompleted()).isTrue();
        assertThat(status.getOutcome()).isEqualTo("COMPLETED.WARNING");
        assertThat(status.isMaxEntriesReached()).isFalse();
    }
}
