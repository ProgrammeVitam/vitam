/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.ServerIdentity;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.LifeCycleStatusCode;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.config.CollectionConfiguration;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.server.application.VitamHttpHeader;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.time.LogicalClockRule;
import fr.gouv.vitam.logbook.common.model.RawLifecycleByLastPersistedDateRequest;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleObjectGroupParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleUnitParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.config.DefaultCollectionConfiguration;
import fr.gouv.vitam.logbook.common.server.config.ElasticsearchLogbookIndexManager;
import fr.gouv.vitam.logbook.common.server.config.LogbookConfiguration;
import fr.gouv.vitam.logbook.common.server.config.LogbookIndexationConfiguration;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollectionsTestUtils;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookElasticsearchAccess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycleMongoDbName;
import fr.gouv.vitam.worker.core.distribution.JsonLineGenericIterator;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import net.javacrumbs.jsonunit.JsonAssert;
import org.apache.commons.collections4.IteratorUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.ws.rs.core.Response.Status;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 */
public class LogBookLifeCycleUnitTest {

    private static final String PREFIX = GUIDFactory.newGUID().getId();
    private static final TypeReference<JsonNode> JSON_NODE_TYPE_REFERENCE = new TypeReference<>() {};

    @ClassRule
    public static MongoRule mongoRule = new MongoRule(MongoDbAccess.getMongoClientSettingsBuilder());

    @ClassRule
    public static ElasticsearchRule elasticsearchRule = new ElasticsearchRule();

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogBookLifeCycleUnitTest.class);

    private static final String REST_URI = "/logbook/v1";

    private static final String JETTY_CONFIG = "jetty-config-test.xml";

    @ClassRule
    public static TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static final String LIFE_UNIT_ID_URI = "/operations/{id_op}/unitlifecycles/{id_lc}";
    private static final String LIFE_UNIT_URI = "/operations/{id_op}/unitlifecycles";
    private static final String LIFE_OG_ID_URI = "/operations/{id_op}/objectgrouplifecycles/{id_lc}";

    private static final String FAKE_UNIT_LF_ID = "1";
    private static final String FAKE_OBG_LF_ID = "1";
    private static final String SELECT_UNIT_BY_ID_URI = "/unitlifecycles/" + FAKE_UNIT_LF_ID;
    private static final String SELECT_OBG_BY_ID_URI = "/objectgrouplifecycles/" + FAKE_OBG_LF_ID;
    private static final String UNIT_LIFECYCLES_RAW_BY_ID_URL = "/raw/unitlifecycles/byid/";
    private static final String UNIT_LIFECYCLES_RAW_BY_LAST_PERSISTED_DATE_URL =
        "/raw/unitlifecycles/bylastpersisteddate";

    private static int serverPort;
    private static LogbookMain application;

    private static LogbookLifeCycleUnitParameters logbookLifeCyclesUnitParametersStart;
    private static LogbookLifeCycleUnitParameters logbookLifeCyclesUnitParametersBAD;
    private static LogbookLifeCycleUnitParameters logbookLifeCyclesUnitParametersUpdate;

    private static LogbookLifeCycleObjectGroupParameters LogbookLifeCycleObjectGroupParametersStart;

    private static JunitHelper junitHelper;

    private static final Integer TENANT_ID = 0;
    private static final List<Integer> tenantList = Collections.singletonList(0);
    private static final ElasticsearchLogbookIndexManager indexManager =
        LogbookCollectionsTestUtils.createTestIndexManager(tenantList, Collections.emptyMap());

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Rule
    public LogicalClockRule logicalClock = new LogicalClockRule();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        List<ElasticsearchNode> esNodes = Lists.newArrayList(
            new ElasticsearchNode(ElasticsearchRule.getHost(), ElasticsearchRule.getPort())
        );

        LogbookCollectionsTestUtils.beforeTestClass(
            mongoRule.getMongoDatabase(),
            PREFIX,
            new LogbookElasticsearchAccess(ElasticsearchRule.VITAM_CLUSTER, esNodes, indexManager)
        );

        junitHelper = JunitHelper.getInstance();
        serverPort = junitHelper.findAvailablePort();

        // TODO P1 verifier la compatibilité avec les tests parallèles sur jenkins

        try {
            final LogbookConfiguration logbookConf = new LogbookConfiguration();
            final List<MongoDbNode> nodes = new ArrayList<>();
            nodes.add(new MongoDbNode("localhost", MongoRule.getDataBasePort()));
            logbookConf.setDbName(mongoRule.getMongoDatabase().getName()).setMongoDbNodes(nodes);
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
            logbookConf.setOperationTraceabilityTemporizationDelay(300);
            logbookConf.setOperationTraceabilityMaxRenewalDelay(12);
            logbookConf.setOperationTraceabilityMaxRenewalDelayUnit(ChronoUnit.HOURS);
            logbookConf.setLifecycleTraceabilityTemporizationDelay(300);
            logbookConf.setLifecycleTraceabilityMaxRenewalDelay(12);
            logbookConf.setLifecycleTraceabilityMaxRenewalDelayUnit(ChronoUnit.HOURS);
            logbookConf.setOperationTraceabilityThreadPoolSize(4);
            logbookConf.setLogbookTenantIndexation(
                new LogbookIndexationConfiguration()
                    .setDefaultCollectionConfiguration(
                        new DefaultCollectionConfiguration().setLogbookoperation(new CollectionConfiguration(1, 0))
                    )
            );

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
            throw new IllegalStateException("Cannot start the Logbook Application Server", e);
        }

        final GUID eip = GUIDFactory.newWriteLogbookGUID(0);
        final GUID iop = GUIDFactory.newWriteLogbookGUID(0);
        final GUID ioL = GUIDFactory.newUnitGUID(0);

        logbookLifeCyclesUnitParametersStart = LogbookParameterHelper.newLogbookLifeCycleUnitParameters();
        logbookLifeCyclesUnitParametersStart.setStatus(StatusCode.OK);
        logbookLifeCyclesUnitParametersStart.putParameterValue(LogbookParameterName.eventIdentifier, eip.toString());
        logbookLifeCyclesUnitParametersStart.putParameterValue(
            LogbookParameterName.eventIdentifierProcess,
            iop.toString()
        );
        logbookLifeCyclesUnitParametersStart.putParameterValue(LogbookParameterName.objectIdentifier, ioL.toString());
        /*
         * Bad request
         */
        logbookLifeCyclesUnitParametersBAD = LogbookParameterHelper.newLogbookLifeCycleUnitParameters();

        logbookLifeCyclesUnitParametersBAD.putParameterValue(LogbookParameterName.eventIdentifier, eip.toString());
        logbookLifeCyclesUnitParametersBAD.putParameterValue(
            LogbookParameterName.eventIdentifierProcess,
            iop.toString()
        );
        logbookLifeCyclesUnitParametersBAD.putParameterValue(LogbookParameterName.objectIdentifier, ioL.toString());
        /*
         * update
         *
         */
        final GUID eip2 = GUIDFactory.newWriteLogbookGUID(0);
        final GUID iop2 = GUIDFactory.newWriteLogbookGUID(0);
        final GUID ioL2 = GUIDFactory.newUnitGUID(0);
        logbookLifeCyclesUnitParametersUpdate = LogbookParameterHelper.newLogbookLifeCycleUnitParameters();
        logbookLifeCyclesUnitParametersUpdate.setStatus(StatusCode.OK);
        logbookLifeCyclesUnitParametersUpdate.putParameterValue(LogbookParameterName.eventIdentifier, eip2.toString());
        logbookLifeCyclesUnitParametersUpdate.putParameterValue(
            LogbookParameterName.eventIdentifierProcess,
            iop2.toString()
        );
        logbookLifeCyclesUnitParametersUpdate.putParameterValue(LogbookParameterName.objectIdentifier, ioL2.toString());

        /*
         * Start ObjectGroup
         */

        LogbookLifeCycleObjectGroupParametersStart = LogbookParameterHelper.newLogbookLifeCycleObjectGroupParameters();

        LogbookLifeCycleObjectGroupParametersStart = LogbookParameterHelper.newLogbookLifeCycleObjectGroupParameters();
        LogbookLifeCycleObjectGroupParametersStart.setStatus(StatusCode.OK);
        LogbookLifeCycleObjectGroupParametersStart.putParameterValue(
            LogbookParameterName.eventIdentifier,
            eip.toString()
        );
        LogbookLifeCycleObjectGroupParametersStart.putParameterValue(
            LogbookParameterName.eventIdentifierProcess,
            iop.toString()
        );
        LogbookLifeCycleObjectGroupParametersStart.putParameterValue(
            LogbookParameterName.objectIdentifier,
            ioL.toString()
        );
    }

    @After
    public void after() {
        LogbookCollectionsTestUtils.afterTest(indexManager);
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

        junitHelper.releasePort(serverPort);
        VitamClientFactory.resetConnections();
    }

    @Test
    public final void given_lifeCycleUnit_when_create_update_test() throws InvalidCreateOperationException {
        // Creation OK

        logbookLifeCyclesUnitParametersStart.putParameterValue(LogbookParameterName.eventType, "event");
        logbookLifeCyclesUnitParametersStart.setTypeProcess(LogbookTypeProcess.INGEST);
        logbookLifeCyclesUnitParametersStart.putParameterValue(LogbookParameterName.outcomeDetail, "outcomeDetail");
        logbookLifeCyclesUnitParametersStart.putParameterValue(
            LogbookParameterName.outcomeDetailMessage,
            "outcomeDetailMessage"
        );
        logbookLifeCyclesUnitParametersStart.putParameterValue(
            LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString()
        );
        logbookLifeCyclesUnitParametersStart.putParameterValue(
            LogbookParameterName.agentIdentifier,
            ServerIdentity.getInstance().getJsonIdentity()
        );
        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(logbookLifeCyclesUnitParametersStart.toString())
            .when()
            .post(
                LIFE_UNIT_ID_URI,
                logbookLifeCyclesUnitParametersStart.getParameterValue(LogbookParameterName.eventIdentifierProcess),
                logbookLifeCyclesUnitParametersStart.getParameterValue(LogbookParameterName.objectIdentifier)
            )
            .then()
            .statusCode(Status.CREATED.getStatusCode());

        // already exists
        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(logbookLifeCyclesUnitParametersStart.toString())
            .when()
            .post(
                LIFE_UNIT_ID_URI,
                logbookLifeCyclesUnitParametersStart.getParameterValue(LogbookParameterName.eventIdentifierProcess),
                logbookLifeCyclesUnitParametersStart.getParameterValue(LogbookParameterName.objectIdentifier)
            )
            .then()
            .statusCode(Status.CONFLICT.getStatusCode());

        // incoherence parameters ; response bad_request
        given()
            .contentType(ContentType.JSON)
            .body(logbookLifeCyclesUnitParametersStart.toString())
            .when()
            .post(
                LIFE_UNIT_ID_URI,
                logbookLifeCyclesUnitParametersStart.getParameterValue(LogbookParameterName.eventIdentifierProcess),
                "bad_id"
            )
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());

        // update ok
        logbookLifeCyclesUnitParametersStart.putParameterValue(
            LogbookParameterName.outcomeDetailMessage,
            "ModifiedoutcomeDetailMessage"
        );
        logbookLifeCyclesUnitParametersStart.setStatus(StatusCode.OK);
        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(logbookLifeCyclesUnitParametersStart.toString())
            .when()
            .put(
                LIFE_UNIT_ID_URI,
                logbookLifeCyclesUnitParametersStart.getParameterValue(LogbookParameterName.eventIdentifierProcess),
                logbookLifeCyclesUnitParametersStart.getParameterValue(LogbookParameterName.objectIdentifier)
            )
            .then()
            .statusCode(Status.OK.getStatusCode());

        // Update illegal argument incoherence parameters ; response bad_request
        given()
            .contentType(ContentType.JSON)
            .body(logbookLifeCyclesUnitParametersStart.toString())
            .when()
            .put(
                LIFE_UNIT_ID_URI,
                logbookLifeCyclesUnitParametersStart.getParameterValue(LogbookParameterName.eventIdentifierProcess),
                "bad_id"
            )
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());

        // Commit the created unit lifeCycle
        given()
            .header(GlobalDataRest.X_EVENT_STATUS, LifeCycleStatusCode.LIFE_CYCLE_COMMITTED)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .contentType(ContentType.JSON)
            .when()
            .put(
                LIFE_UNIT_ID_URI,
                logbookLifeCyclesUnitParametersStart.getParameterValue(LogbookParameterName.eventIdentifierProcess),
                logbookLifeCyclesUnitParametersStart.getParameterValue(LogbookParameterName.objectIdentifier)
            )
            .then()
            .statusCode(Status.OK.getStatusCode());

        // Test direct access
        Select select = new Select();
        select.setQuery(
            QueryHelper.eq(
                LogbookLifeCycleMongoDbName.objectIdentifier.getDbname(),
                logbookLifeCyclesUnitParametersStart.getParameterValue(LogbookParameterName.objectIdentifier)
            )
        );

        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_EVENT_STATUS, LifeCycleStatusCode.LIFE_CYCLE_COMMITTED.toString())
            .body(select.getFinalSelect())
            .when()
            .get(
                "/unitlifecycles/" +
                logbookLifeCyclesUnitParametersStart.getParameterValue(LogbookParameterName.objectIdentifier)
            )
            .then()
            .statusCode(Status.OK.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(new Select().getFinalSelect())
            .when()
            .get(
                LIFE_UNIT_URI,
                logbookLifeCyclesUnitParametersStart.getParameterValue(LogbookParameterName.eventIdentifierProcess)
            )
            .then()
            .statusCode(Status.OK.getStatusCode());
    }

    @Test
    public final void given_lifeCycleUnitWithoutMandotoryParams_when_create_thenReturn_BAD_RESUEST() {
        final GUID guidTest = GUIDFactory.newWriteLogbookGUID(0);

        logbookLifeCyclesUnitParametersBAD.putParameterValue(
            LogbookParameterName.objectIdentifier,
            guidTest.toString()
        );
        given()
            .contentType(ContentType.JSON)
            .body(logbookLifeCyclesUnitParametersBAD.toString())
            .when()
            .post(
                LIFE_UNIT_ID_URI,
                logbookLifeCyclesUnitParametersBAD.getParameterValue(LogbookParameterName.eventIdentifierProcess),
                logbookLifeCyclesUnitParametersBAD.getParameterValue(LogbookParameterName.objectIdentifier)
            )
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public final void given_lifeCycleUnitWithoutMandotoryParams_when_Update_thenReturn_BAD_RESUEST() {
        final GUID guidTest = GUIDFactory.newWriteLogbookGUID(0);

        logbookLifeCyclesUnitParametersBAD.putParameterValue(
            LogbookParameterName.objectIdentifier,
            guidTest.toString()
        );
        given()
            .contentType(ContentType.JSON)
            .body(logbookLifeCyclesUnitParametersBAD.toString())
            .when()
            .header(GlobalDataRest.X_EVENT_STATUS, LifeCycleStatusCode.LIFE_CYCLE_IN_PROCESS.toString())
            .put(
                LIFE_UNIT_ID_URI,
                logbookLifeCyclesUnitParametersBAD.getParameterValue(LogbookParameterName.eventIdentifierProcess),
                logbookLifeCyclesUnitParametersBAD.getParameterValue(LogbookParameterName.objectIdentifier)
            )
            .then()
            .statusCode(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public final void deleteUnit_PassTheRightArgument_ResponseOK() {
        // Delete OK
        logbookLifeCyclesUnitParametersStart.putParameterValue(
            LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString()
        );
        logbookLifeCyclesUnitParametersStart.putParameterValue(
            LogbookParameterName.agentIdentifier,
            ServerIdentity.getInstance().getJsonIdentity()
        );

        logbookLifeCyclesUnitParametersStart.putParameterValue(LogbookParameterName.eventType, "event");
        logbookLifeCyclesUnitParametersStart.setTypeProcess(LogbookTypeProcess.INGEST);
        logbookLifeCyclesUnitParametersStart.putParameterValue(LogbookParameterName.outcomeDetail, "outcomeDetail");
        logbookLifeCyclesUnitParametersStart.putParameterValue(
            LogbookParameterName.outcomeDetailMessage,
            "outcomeDetailMessage"
        );
        logbookLifeCyclesUnitParametersStart.putParameterValue(
            LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString()
        );
        logbookLifeCyclesUnitParametersStart.putParameterValue(
            LogbookParameterName.agentIdentifier,
            ServerIdentity.getInstance().getJsonIdentity()
        );
        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(logbookLifeCyclesUnitParametersStart.toString())
            .when()
            .post(
                LIFE_UNIT_ID_URI,
                logbookLifeCyclesUnitParametersStart.getParameterValue(LogbookParameterName.eventIdentifierProcess),
                logbookLifeCyclesUnitParametersStart.getParameterValue(LogbookParameterName.objectIdentifier)
            )
            .then()
            .statusCode(Status.CREATED.getStatusCode());
        given()
            .when()
            .delete(
                LIFE_UNIT_ID_URI,
                logbookLifeCyclesUnitParametersStart.getParameterValue(LogbookParameterName.eventIdentifierProcess),
                logbookLifeCyclesUnitParametersStart.getParameterValue(LogbookParameterName.objectIdentifier)
            )
            .then()
            .statusCode(Status.OK.getStatusCode());
    }

    @Test
    public final void deleteObjectGroup_PassWrongAgument_thenNotFound() {
        // Delete KO
        LogbookLifeCycleObjectGroupParametersStart.putParameterValue(
            LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString()
        );
        LogbookLifeCycleObjectGroupParametersStart.putParameterValue(
            LogbookParameterName.agentIdentifier,
            ServerIdentity.getInstance().getJsonIdentity()
        );
        given()
            .contentType(ContentType.JSON)
            .when()
            .delete(
                LIFE_UNIT_ID_URI,
                LogbookLifeCycleObjectGroupParametersStart.getParameterValue(
                    LogbookParameterName.eventIdentifierProcess
                ),
                "notExists"
            )
            .then()
            .statusCode(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public final void given_lifeCycleUnit_when_update_notfound() {
        // update notFound
        logbookLifeCyclesUnitParametersUpdate.putParameterValue(LogbookParameterName.eventType, "event");
        logbookLifeCyclesUnitParametersUpdate.setTypeProcess(LogbookTypeProcess.INGEST);
        logbookLifeCyclesUnitParametersUpdate.putParameterValue(LogbookParameterName.outcomeDetail, "outcomeDetail");
        logbookLifeCyclesUnitParametersUpdate.putParameterValue(
            LogbookParameterName.outcomeDetailMessage,
            "outcomeDetailMessage"
        );
        logbookLifeCyclesUnitParametersUpdate.putParameterValue(
            LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString()
        );
        logbookLifeCyclesUnitParametersUpdate.putParameterValue(
            LogbookParameterName.agentIdentifier,
            ServerIdentity.getInstance().getJsonIdentity()
        );

        given()
            .contentType(ContentType.JSON)
            .body(logbookLifeCyclesUnitParametersUpdate.toString())
            .header(GlobalDataRest.X_EVENT_STATUS, LifeCycleStatusCode.LIFE_CYCLE_IN_PROCESS.toString())
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .put(
                LIFE_UNIT_ID_URI,
                logbookLifeCyclesUnitParametersUpdate.getParameterValue(LogbookParameterName.eventIdentifierProcess),
                logbookLifeCyclesUnitParametersUpdate.getParameterValue(LogbookParameterName.objectIdentifier)
            )
            .then()
            .statusCode(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public final void deleteObjectGroup_PassTheRightArgument_ResponseOK() {
        // Delete OK
        LogbookLifeCycleObjectGroupParametersStart.putParameterValue(
            LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString()
        );
        LogbookLifeCycleObjectGroupParametersStart.putParameterValue(
            LogbookParameterName.agentIdentifier,
            ServerIdentity.getInstance().getJsonIdentity()
        );

        LogbookLifeCycleObjectGroupParametersStart.putParameterValue(LogbookParameterName.eventType, "event");
        LogbookLifeCycleObjectGroupParametersStart.setTypeProcess(LogbookTypeProcess.INGEST);
        LogbookLifeCycleObjectGroupParametersStart.putParameterValue(
            LogbookParameterName.outcomeDetail,
            "outcomeDetail"
        );
        LogbookLifeCycleObjectGroupParametersStart.putParameterValue(
            LogbookParameterName.outcomeDetailMessage,
            "outcomeDetailMessage"
        );
        LogbookLifeCycleObjectGroupParametersStart.putParameterValue(
            LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString()
        );
        LogbookLifeCycleObjectGroupParametersStart.putParameterValue(
            LogbookParameterName.agentIdentifier,
            ServerIdentity.getInstance().getJsonIdentity()
        );
        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(LogbookLifeCycleObjectGroupParametersStart.toString())
            .when()
            .post(
                LIFE_OG_ID_URI,
                LogbookLifeCycleObjectGroupParametersStart.getParameterValue(
                    LogbookParameterName.eventIdentifierProcess
                ),
                LogbookLifeCycleObjectGroupParametersStart.getParameterValue(LogbookParameterName.objectIdentifier)
            )
            .then()
            .statusCode(Status.CREATED.getStatusCode());

        given()
            .when()
            .delete(
                LIFE_OG_ID_URI,
                LogbookLifeCycleObjectGroupParametersStart.getParameterValue(
                    LogbookParameterName.eventIdentifierProcess
                ),
                LogbookLifeCycleObjectGroupParametersStart.getParameterValue(LogbookParameterName.objectIdentifier)
            )
            .then()
            .statusCode(Status.OK.getStatusCode());
    }

    @Test
    public final void given_lifeCycleObjectGroupWithoutMandotoryParams_when_delete_thenReturn_BAD_RESUEST() {
        // Delete OK

        given().when().delete(LIFE_OG_ID_URI, "id", "f12").then().statusCode(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void testGetUnitLifeCycleByIdThenOkWhenLogbookNotFoundException() throws Exception {
        Select select = new Select();
        select.setQuery(QueryHelper.eq(LogbookLifeCycleMongoDbName.objectIdentifier.getDbname(), FAKE_UNIT_LF_ID));
        JsonNode query = select.getFinalSelect();

        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(query)
            .param("id_lc", FAKE_UNIT_LF_ID)
            .expect()
            .statusCode(Status.NOT_FOUND.getStatusCode())
            .when()
            .get(SELECT_UNIT_BY_ID_URI);
    }

    @Test
    public void testGetObjectGroupLifeCycleByIdThenOkWhenLogbookNotFoundException() {
        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(new Select().getFinalSelect())
            .param("id_lc", FAKE_OBG_LF_ID)
            .expect()
            .statusCode(Status.NOT_FOUND.getStatusCode())
            .when()
            .get(SELECT_OBG_BY_ID_URI);
    }

    @RunWithCustomExecutor
    @Test
    public final void given_lifeCycleUnit_bulk_raw_when_create_thenReturn_created()
        throws InvalidParseOperationException, FileNotFoundException {
        List<JsonNode> lfcGotList = new ArrayList<>();
        lfcGotList.add(
            JsonHandler.getFromInputStream(
                PropertiesUtils.getResourceAsStream("lfc_unit_raw_aeaqaaaaaaef6ys5absnuala7tya75iaaacq.json")
            )
        );
        lfcGotList.add(
            JsonHandler.getFromInputStream(
                PropertiesUtils.getResourceAsStream("lfc_unit_raw_aeaqaaaaaageqltuabfg2ala73ny3zaaaacq.json")
            )
        );

        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(lfcGotList)
            .when()
            .post("/raw/unitlifecycles/bulk")
            .then()
            .statusCode(Status.CREATED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(lfcGotList)
            .when()
            .post("/raw/unitlifecycles/bulk")
            .then()
            .statusCode(Status.CREATED.getStatusCode());

        lfcGotList.add(
            JsonHandler.getFromInputStream(
                PropertiesUtils.getResourceAsStream("lfc_unit_raw_aeaqaaaaaageqltuabfg2ala73ny3zaaaacq_diff.json")
            )
        );
        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(lfcGotList)
            .when()
            .post("/raw/unitlifecycles/bulk")
            .then()
            .statusCode(Status.CREATED.getStatusCode());
    }

    @Test
    public void testGetRawUnitLifecycleById_OK() throws Exception {
        // Given
        JsonNode json = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream("lfc_unit_raw_aeaqaaaaaaef6ys5absnuala7tya75iaaacq.json")
        );

        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(Collections.singletonList(json))
            .when()
            .post("/raw/unitlifecycles/bulk")
            .then()
            .statusCode(Status.CREATED.getStatusCode());

        // When / Then
        String body = given()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .get(UNIT_LIFECYCLES_RAW_BY_ID_URL + "aeaqaaaaaaef6ys5absnuala7tya75iaaacq")
            .then()
            .statusCode(Status.OK.getStatusCode())
            .extract()
            .body()
            .asString();
        RequestResponseOK requestResponse = JsonHandler.getFromString(body, RequestResponseOK.class);

        String expectedJson = JsonHandler.unprettyPrint(json);
        String actualJson = JsonHandler.unprettyPrint(JsonHandler.toJsonNode(requestResponse.getFirstResult()));

        JsonAssert.assertJsonEquals(expectedJson, actualJson);
    }

    @Test
    public void testGetRawUnitLifecycleById_NotFound() throws Exception {
        // Given : Empty DB

        // When / Then
        given()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .get(UNIT_LIFECYCLES_RAW_BY_ID_URL + "aeaqaaaaaaef6ys5absnuala7tya75iaaaaa")
            .then()
            .statusCode(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public final void testGetRawUnitLifecyclesByLastPersistedDate() throws Exception {
        // Given :

        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(PropertiesUtils.getResourceAsStream("lfc_units_raw.json"))
            .when()
            .post("/raw/unitlifecycles/bulk")
            .then()
            .statusCode(Status.CREATED.getStatusCode());

        // When
        RawLifecycleByLastPersistedDateRequest request = new RawLifecycleByLastPersistedDateRequest(
            "2020-09-07T11:01:53.112",
            "2020-09-07T11:01:53.139",
            1000
        );
        ValidatableResponse response = given()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_REQUEST_ID, GUIDFactory.newGUID().getId())
            .contentType(ContentType.JSON)
            .body(JsonHandler.toJsonNode(request))
            .post(UNIT_LIFECYCLES_RAW_BY_LAST_PERSISTED_DATE_URL)
            .then();

        // Then
        response.statusCode(Status.OK.getStatusCode());
        long size = Long.parseLong(response.extract().header(VitamHttpHeader.X_CONTENT_LENGTH.getName()));
        assertThat(size).isPositive();

        byte[] body = ByteStreams.toByteArray(response.extract().asInputStream());
        assertThat(body).hasSize((int) size);

        JsonLineGenericIterator<JsonNode> jsonLineIterator = new JsonLineGenericIterator<>(
            new ByteArrayInputStream(body),
            JSON_NODE_TYPE_REFERENCE
        );
        List<JsonNode> entries = IteratorUtils.toList(jsonLineIterator);
        assertThat(entries).hasSize(2);
        assertThat(entries.get(0).get("_id").asText()).isEqualTo("aeaqaaaaaahdjrrgaaqiwaluna5izwiaaaaq");
        assertThat(entries.get(1).get("_id").asText()).isEqualTo("aeaqaaaaaahdjrrgaaqiwaluna5iz4aaaaaq");
    }

    @Test
    public final void testGetRawUnitLifecyclesByLastPersistedDateWithLimit() throws Exception {
        // Given :

        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(PropertiesUtils.getResourceAsStream("lfc_units_raw.json"))
            .when()
            .post("/raw/unitlifecycles/bulk")
            .then()
            .statusCode(Status.CREATED.getStatusCode());

        // When
        RawLifecycleByLastPersistedDateRequest request = new RawLifecycleByLastPersistedDateRequest(
            "2020-09-07T11:01:53.112",
            "2020-09-07T11:01:53.139",
            1
        );
        ValidatableResponse response = given()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_REQUEST_ID, GUIDFactory.newGUID().getId())
            .contentType(ContentType.JSON)
            .body(JsonHandler.toJsonNode(request))
            .post(UNIT_LIFECYCLES_RAW_BY_LAST_PERSISTED_DATE_URL)
            .then();

        // Then
        response.statusCode(Status.OK.getStatusCode());
        long size = Long.parseLong(response.extract().header(VitamHttpHeader.X_CONTENT_LENGTH.getName()));
        assertThat(size).isPositive();

        byte[] body = ByteStreams.toByteArray(response.extract().asInputStream());
        assertThat(body).hasSize((int) size);

        JsonLineGenericIterator<JsonNode> jsonLineIterator = new JsonLineGenericIterator<>(
            new ByteArrayInputStream(body),
            JSON_NODE_TYPE_REFERENCE
        );
        List<JsonNode> entries = IteratorUtils.toList(jsonLineIterator);
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).get("_id").asText()).isEqualTo("aeaqaaaaaahdjrrgaaqiwaluna5izwiaaaaq");
    }

    @Test
    public final void testGetRawUnitLifecyclesByLastPersistedDateWithLimitMatchingMultipleEntries() throws Exception {
        // Given :
        given()
            .contentType(ContentType.JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .body(PropertiesUtils.getResourceAsStream("lfc_units_raw.json"))
            .when()
            .post("/raw/unitlifecycles/bulk")
            .then()
            .statusCode(Status.CREATED.getStatusCode());

        // When
        RawLifecycleByLastPersistedDateRequest request = new RawLifecycleByLastPersistedDateRequest(
            "2020-01-01T00:00:00.000",
            "2020-12-31T01:02:03.456",
            5
        );
        ValidatableResponse response = given()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_REQUEST_ID, GUIDFactory.newGUID().getId())
            .contentType(ContentType.JSON)
            .body(JsonHandler.toJsonNode(request))
            .post(UNIT_LIFECYCLES_RAW_BY_LAST_PERSISTED_DATE_URL)
            .then();

        // Then
        response.statusCode(Status.OK.getStatusCode());
        long size = Long.parseLong(response.extract().header(VitamHttpHeader.X_CONTENT_LENGTH.getName()));
        assertThat(size).isPositive();

        byte[] body = ByteStreams.toByteArray(response.extract().asInputStream());
        assertThat(body).hasSize((int) size);

        JsonLineGenericIterator<JsonNode> jsonLineIterator = new JsonLineGenericIterator<>(
            new ByteArrayInputStream(body),
            JSON_NODE_TYPE_REFERENCE
        );
        List<JsonNode> entries = IteratorUtils.toList(jsonLineIterator);
        assertThat(entries).hasSize(6);
        assertThat(entries)
            .extracting(entry -> entry.get("_id").asText())
            .containsExactlyInAnyOrder(
                "aeaqaaaaaahdjrrgaaqiwaluna5iwoyaaabq",
                "aeaqaaaaaahdjrrgaaqiwaluna5izwiaaaaq",
                "aeaqaaaaaahdjrrgaaqiwaluna5iz4aaaaaq",
                "aeaqaaaaaahdjrrgaaqiwaluna5prxiaaaba",
                "aeaqaaaaaahdjrrgaaqiwaluna5teviaaaba",
                "aeaqaaaaaahdjrrgaaqiwaluna5tfayaaaba"
            );
    }

    @Test
    public final void testGetRawUnitLifecyclesByLastPersistedDateEmpty() throws Exception {
        // Given : no data

        // When
        RawLifecycleByLastPersistedDateRequest request = new RawLifecycleByLastPersistedDateRequest(
            "2020-09-07T11:01:53.112",
            "2020-09-07T11:01:53.139",
            1000
        );
        ValidatableResponse response = given()
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .header(GlobalDataRest.X_REQUEST_ID, GUIDFactory.newGUID().getId())
            .contentType(ContentType.JSON)
            .body(JsonHandler.toJsonNode(request))
            .post(UNIT_LIFECYCLES_RAW_BY_LAST_PERSISTED_DATE_URL)
            .then();

        // Then
        response.statusCode(Status.OK.getStatusCode());
        long size = Long.parseLong(response.extract().header(VitamHttpHeader.X_CONTENT_LENGTH.getName()));
        assertThat(size).isZero();

        byte[] body = ByteStreams.toByteArray(response.extract().asInputStream());
        assertThat(body).isEmpty();
    }
}
