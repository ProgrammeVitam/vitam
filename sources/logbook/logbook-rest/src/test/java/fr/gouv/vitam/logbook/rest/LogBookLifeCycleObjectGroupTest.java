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
import com.google.common.collect.Lists;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.ServerIdentity;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleObjectGroupParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.LogbookConfiguration;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookElasticsearchAccess;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.ws.rs.core.Response.Status;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static io.restassured.RestAssured.given;

/**
 *
 */
public class LogBookLifeCycleObjectGroupTest {

    private static final String PREFIX = GUIDFactory.newGUID().getId();

    @ClassRule
    public static MongoRule mongoRule =
            new MongoRule(VitamCollection.getMongoClientOptions());

    @ClassRule
    public static ElasticsearchRule elasticsearchRule = new ElasticsearchRule();

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogBookLifeCycleObjectGroupTest.class);

    private static final String REST_URI = "/logbook/v1";

    private static final String JETTY_CONFIG = "jetty-config-test.xml";

    private static final Integer tenantId = 0;
    private static final List<Integer> tenantList = Arrays.asList(0);

    // ES
    @ClassRule
    public static TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static final String LIFE_OBJECT_GROUP_ID_URI = "/operations/{id_op}/objectgrouplifecycles/{id_lc}";
    private static final String LIFE_OBJECT_GROUP_URI = "/operations/{id_op}/objectgrouplifecycles";
    private static final String OBJECT_GROUP_LIFECYCLES_RAW_BY_ID_URL = "/raw/objectgrouplifecycles/byid/";

    private static int serverPort;
    private static LogbookMain application;
    private static LogbookLifeCycleObjectGroupParameters logbookLifeCyclesObjectGroupParametersStart;
    private static LogbookLifeCycleObjectGroupParameters logbookLCObjectGroupParametersAppend;
    private static LogbookLifeCycleObjectGroupParameters logbookLifeCyclesObjectGroupParametersBAD;

    private static LogbookLifeCycleObjectGroupParameters logbookLifeCyclesObjectGroupParametersUpdate;

    private static JunitHelper junitHelper;

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor());

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {

        LogbookCollections.beforeTestClass(mongoRule.getMongoDatabase(), PREFIX,
                new LogbookElasticsearchAccess(ElasticsearchRule.VITAM_CLUSTER,
                        Lists.newArrayList(new ElasticsearchNode("localhost", ElasticsearchRule.TCP_PORT))), tenantId);

        junitHelper = JunitHelper.getInstance();
        serverPort = junitHelper.findAvailablePort();

        // TODO P1 verifier la compatibilité avec les tests parallèles sur jenkins


        try {
            final LogbookConfiguration logbookConf = new LogbookConfiguration();
            final List<MongoDbNode> nodes = new ArrayList<>();
            nodes.add(new MongoDbNode("localhost", mongoRule.getDataBasePort()));
            logbookConf.setDbName(mongoRule.getMongoDatabase().getName()).setMongoDbNodes(nodes);
            final List<ElasticsearchNode> esNodes = new ArrayList<>();
            esNodes.add(new ElasticsearchNode("localhost", ElasticsearchRule.TCP_PORT));
            logbookConf.setJettyConfig(JETTY_CONFIG);
            logbookConf.setP12LogbookFile("tsa.p12");
            logbookConf.setP12LogbookPassword("1234");
            logbookConf.setWorkspaceUrl("http://localhost:8001");
            logbookConf.setProcessingUrl("http://localhost:8002");
            logbookConf.setClusterName(ElasticsearchRule.VITAM_CLUSTER);
            logbookConf.setElasticsearchNodes(esNodes);
            VitamConfiguration.setTenants(tenantList);
            logbookConf.setOpLfcEventsToSkip(new ArrayList<>());
            logbookConf.setOpEventsNotInWf(new ArrayList<>());
            logbookConf.setOpWithLFC(new ArrayList<>());

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

        logbookLifeCyclesObjectGroupParametersStart =
            LogbookParameterHelper.newLogbookLifeCycleObjectGroupParameters();
        logbookLifeCyclesObjectGroupParametersStart.setStatus(StatusCode.OK);
        logbookLifeCyclesObjectGroupParametersStart.putParameterValue(LogbookParameterName.eventIdentifier,
            eip.toString());
        logbookLifeCyclesObjectGroupParametersStart.putParameterValue(LogbookParameterName.eventIdentifierProcess,
            iop.toString());
        logbookLifeCyclesObjectGroupParametersStart.putParameterValue(LogbookParameterName.objectIdentifier,
            ioL.toString());
        /**
         * Bad request
         */
        logbookLifeCyclesObjectGroupParametersBAD = LogbookParameterHelper.newLogbookLifeCycleObjectGroupParameters();

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
            LogbookParameterHelper.newLogbookLifeCycleObjectGroupParameters();
        logbookLifeCyclesObjectGroupParametersUpdate.setStatus(StatusCode.OK);
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

        logbookLCObjectGroupParametersAppend = LogbookParameterHelper.newLogbookLifeCycleObjectGroupParameters();
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

        LogbookCollections.afterTestClass(true, tenantId);

        junitHelper.releasePort(serverPort);
        VitamClientFactory.resetConnections();
    }

    @Test
    public final void given_lifeCycleObjectGroup_when_create_thenReturn_created()
        throws InvalidParseOperationException {
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
            .header(GlobalDataRest.X_TENANT_ID, tenantId)
            .body(logbookLifeCyclesObjectGroupParametersStart.toString())
            .when()
            .post(LIFE_OBJECT_GROUP_ID_URI,
                operationId,
                objectId)
            .then()
            .statusCode(Status.CREATED.getStatusCode());



        // already exists
        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, tenantId)
            .body(logbookLifeCyclesObjectGroupParametersStart.toString())
            .when()
            .post(LIFE_OBJECT_GROUP_ID_URI, operationId,
                objectId)
            .then()
            .statusCode(Status.CONFLICT.getStatusCode());

        // incoherence parameters ; response bad_request
        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, tenantId)
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
        JsonNode jsonNode =
            JsonHandler.getFromString("{\"$query\":{\"$eq\":{\"_id\":\"" + objectId + "\"}}},  {\"$projection\":{}}\"");
        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, 0)
            .body(jsonNode)
            .when()
            .get("/objectgrouplifecycles/" + objectId)
            .then()
            .statusCode(Status.OK.getStatusCode());

        // Test Iterator
        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, 0)
            .body(new Select().getFinalSelect())
            .when()
            .get(LIFE_OBJECT_GROUP_URI, operationId)
            .then()
            .statusCode(Status.OK.getStatusCode());
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

    @RunWithCustomExecutor
    @Test
    public final void given_lifeCycleGO_bulk_raw_when_create_thenReturn_created()
        throws InvalidParseOperationException, FileNotFoundException {
        List<JsonNode> lfcGotList = new ArrayList<>();
        lfcGotList.add(JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream("lfc_got_raw_aebaaaaaaaef6ys5absnuala7t4lfmiaaabq.json")));
        lfcGotList.add(JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream("lfc_got_raw_aebaaaaaaageqltuabfg2ala73rnaqiaaaba.json")));

        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, tenantId)
            .body(lfcGotList)
            .when()
            .post("/raw/objectgrouplifecycles/bulk")
            .then()
            .statusCode(Status.CREATED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, tenantId)
            .body(lfcGotList)
            .when()
            .post("/raw/objectgrouplifecycles/bulk")
            .then()
            .statusCode(Status.CREATED.getStatusCode());

        lfcGotList.add(JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream("lfc_got_raw_aebaaaaaaageqltuabfg2ala73rnaqiaaaba_diff.json")));
        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, tenantId)
            .body(lfcGotList)
            .when()
            .post("/raw/objectgrouplifecycles/bulk")
            .then()
            .statusCode(Status.CREATED.getStatusCode());
    }

    @Test
    public void testGetRawObjectGroupLifecycleById_OK() throws Exception {

        // Given
        JsonNode json = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream("lfc_got_raw_aebaaaaaaaef6ys5absnuala7t4lfmiaaabq.json"));

        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, tenantId)
            .body(Collections.singletonList(json))
            .when()
            .post("/raw/objectgrouplifecycles/bulk")
            .then()
            .statusCode(Status.CREATED.getStatusCode());


        // When / Then
        InputStream stream = given()
            .header(GlobalDataRest.X_TENANT_ID, tenantId)
            .get(OBJECT_GROUP_LIFECYCLES_RAW_BY_ID_URL + "aebaaaaaaaef6ys5absnuala7t4lfmiaaabq")
            .then()
            .statusCode(Status.OK.getStatusCode())
            .extract().body().asInputStream();

        RequestResponseOK response = JsonHandler.getFromInputStream(stream, RequestResponseOK.class);

        String expectedJson = JsonHandler.unprettyPrint(json);
        String actualJson = JsonHandler.unprettyPrint(JsonHandler.toJsonNode(response.getFirstResult()));

        JsonAssert.assertJsonEquals(expectedJson, actualJson);
    }

    @Test
    public void testGetRawObjectGroupLifecycleById_NotFound() throws Exception {

        // Given : Empty DB

        // When / Then
        given()
            .header(GlobalDataRest.X_TENANT_ID, tenantId)
            .get(OBJECT_GROUP_LIFECYCLES_RAW_BY_ID_URL + "aebaaaaaaaef6ys5absnuala7t4lfmiaaaaa")
            .then()
            .statusCode(Status.NOT_FOUND.getStatusCode());
    }
}
