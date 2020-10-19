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
package fr.gouv.vitam.logbook.operations.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.google.common.collect.Lists;
import com.mongodb.client.FindIterable;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.client.configuration.ClientConfigurationImpl;
import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
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
import fr.gouv.vitam.common.model.config.CollectionConfiguration;
import fr.gouv.vitam.common.model.logbook.LogbookEvent;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.model.LogbookLifeCycleObjectGroupModel;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleObjectGroupParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.config.DefaultCollectionConfiguration;
import fr.gouv.vitam.logbook.common.server.config.ElasticsearchLogbookIndexManager;
import fr.gouv.vitam.logbook.common.server.config.LogbookIndexationConfiguration;
import fr.gouv.vitam.logbook.common.server.config.LogbookConfiguration;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollectionsTestUtils;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookDocument;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookElasticsearchAccess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycleObjectGroup;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycleUnit;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbName;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.ws.rs.core.Response;
import java.io.File;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;
import static fr.gouv.vitam.logbook.common.parameters.Contexts.IMPORT_ONTOLOGY;
import static fr.gouv.vitam.logbook.common.parameters.Contexts.INGEST_CLEANUP;
import static fr.gouv.vitam.logbook.common.parameters.Contexts.LOGBOOK_TRACEABILITY;
import static fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections.LIFECYCLE_OBJECTGROUP_IN_PROCESS;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class LogbookResourceIT {

    private static final String PREFIX = GUIDFactory.newGUID().getId();

    @ClassRule
    public static MongoRule mongoRule =
        new MongoRule(VitamCollection.getMongoClientOptions());

    @ClassRule
    public static ElasticsearchRule elasticsearchRule = new ElasticsearchRule();

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogbookResourceIT.class);

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    private static final String JETTY_CONFIG = "jetty-config-test.xml";
    private static final String ALERT_EVENT_TYPE = "STP_IMPORT_ACCESS_CONTRACT";
    private static final String ALERT_EVENT_OUTCOME = "OK";

    @ClassRule
    public static TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static final String REST_URI = "/logbook/v1";
    private static JunitHelper junitHelper = JunitHelper.getInstance();
    private static int serverPort;
    private static LogbookMain application;
    private static final int NB_TEST = 100;
    private static final Integer tenantId = 0;
    private static final Integer adminTenant = 1;
    private static final Integer secondTenant = 0;
    private static final List<Integer> tenantList = newArrayList(tenantId, secondTenant);
    private static final ElasticsearchLogbookIndexManager indexManager = LogbookCollectionsTestUtils
        .createTestIndexManager(tenantList, Collections.emptyMap());

    private static LogbookOperationParameters logbookParametersStart;
    private static LogbookOperationParameters logbookParametersAppend;
    private static LogbookOperationParameters logbookParametersWrongStart;
    private static LogbookOperationParameters logbookParametersWrongAppend;
    private static LogbookLifeCycleObjectGroupParameters logbookLcParametersStart;
    private static LogbookLifeCycleObjectGroupParameters logbookLcParametersAppend;
    private static LogbookLifeCycleObjectGroupParameters logbookLcParametersWrongStart;
    private static LogbookLifeCycleObjectGroupParameters logbookLcParametersWrongAppend;

    private static int workspacePort = junitHelper.findAvailablePort();
    @ClassRule
    public static WireMockClassRule workspaceWireMockRule = new WireMockClassRule(workspacePort);
    @Rule
    public WireMockClassRule workspaceInstanceRule = workspaceWireMockRule;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {

        List<ElasticsearchNode> esNodes =
            Lists.newArrayList(new ElasticsearchNode(ElasticsearchRule.getHost(), ElasticsearchRule.getPort()));

        LogbookCollectionsTestUtils.beforeTestClass(mongoRule.getMongoDatabase(), PREFIX,
            new LogbookElasticsearchAccess(ElasticsearchRule.VITAM_CLUSTER, esNodes, indexManager));

        serverPort = junitHelper.findAvailablePort();

        try {
            final LogbookConfiguration logbookConf = new LogbookConfiguration();
            final List<MongoDbNode> nodes = new ArrayList<>();
            nodes.add(new MongoDbNode("localhost", MongoRule.getDataBasePort()));
            logbookConf.setMongoDbNodes(nodes).setDbName(mongoRule.getMongoDatabase().getName());
            logbookConf.setJettyConfig(JETTY_CONFIG);
            logbookConf.setP12LogbookFile("tsa.p12");
            logbookConf.setP12LogbookPassword("1234");
            logbookConf.setWorkspaceUrl("http://localhost:" + workspacePort);
            logbookConf.setProcessingUrl("http://localhost:8097");
            logbookConf.setClusterName(ElasticsearchRule.VITAM_CLUSTER);
            logbookConf.setElasticsearchNodes(esNodes);
            VitamConfiguration.setTenants(tenantList);
            VitamConfiguration.setAdminTenant(adminTenant);
            final List<LogbookEvent> alertEvents = new ArrayList<>();
            LogbookEvent alertEvent = new LogbookEvent();
            alertEvent.setEvType(ALERT_EVENT_TYPE);
            alertEvent.setOutcome(ALERT_EVENT_OUTCOME);
            alertEvent.setOutDetail(ALERT_EVENT_TYPE + "." + ALERT_EVENT_OUTCOME);
            alertEvents.add(alertEvent);
            logbookConf.setAlertEvents(alertEvents);
            logbookConf.setOpLfcEventsToSkip(new ArrayList<>());
            logbookConf.setOpWithLFC(new ArrayList<>());
            logbookConf.setOpEventsNotInWf(new ArrayList<>());
            logbookConf.setOperationTraceabilityTemporizationDelay(300);
            logbookConf.setOperationTraceabilityMaxRenewalDelay(12);
            logbookConf.setOperationTraceabilityMaxRenewalDelayUnit(ChronoUnit.HOURS);
            logbookConf.setLifecycleTraceabilityTemporizationDelay(300);
            logbookConf.setLifecycleTraceabilityMaxRenewalDelay(12);
            logbookConf.setLifecycleTraceabilityMaxRenewalDelayUnit(ChronoUnit.HOURS);
            logbookConf.setOperationTraceabilityThreadPoolSize(4);
            logbookConf.setLogbookTenantIndexation(new LogbookIndexationConfiguration()
                .setDefaultCollectionConfiguration(new DefaultCollectionConfiguration().setLogbookoperation(
                    new CollectionConfiguration(2, 1))));
            File file = temporaryFolder.newFile();
            String configurationFile = file.getAbsolutePath();
            PropertiesUtils.writeYaml(file, logbookConf);


            application = new LogbookMain(configurationFile);
            application.start();
            JunitHelper.unsetJettyPortSystemProperty();

            RestAssured.port = serverPort;
            RestAssured.basePath = REST_URI;
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
            throw new IllegalStateException(
                "Cannot start the Logbook Application Server", e);
        }

        LogbookOperationsClientFactory.changeMode(new ClientConfigurationImpl("localhost", serverPort));
        LogbookLifeCyclesClientFactory.changeMode(new ClientConfigurationImpl("localhost", serverPort));
        LOGGER.debug("Initialize client: localhost" + ":" + serverPort);
    }

    @Before
    public void setUp() {
        workspaceInstanceRule.stubFor(WireMock.post(WireMock.urlMatching("/workspace/v1/containers/(.*)")).willReturn
            (WireMock.aResponse().withStatus(201).withHeader(GlobalDataRest.X_TENANT_ID, Integer.toString(0))));
        workspaceInstanceRule.stubFor(WireMock.post(WireMock.urlMatching("/workspace/v1/containers/(.*)/objects/(.*)"))
            .willReturn(WireMock.aResponse().withStatus(201)
                .withHeader(GlobalDataRest.X_TENANT_ID, Integer.toString(0))));
        workspaceInstanceRule.stubFor(WireMock.delete(WireMock.urlMatching("/workspace/v1/containers/(.*)")).willReturn
            (WireMock.aResponse().withStatus(204).withHeader(GlobalDataRest.X_TENANT_ID, Integer.toString(0))));
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

        LogbookCollectionsTestUtils.afterTestClass(indexManager, true);

        junitHelper.releasePort(serverPort);
        junitHelper.releasePort(workspacePort);
        VitamClientFactory.resetConnections();
    }

    @After
    public void cleanup() {
        LogbookCollectionsTestUtils.afterTest(indexManager);
    }

    @RunWithCustomExecutor
    @Test
    public final void testOperationSearchTraceability() throws LogbookClientException, VitamApplicationServerException {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        final GUID eip1 = GUIDFactory.newEventGUID(tenantId);
        final GUID eip2 = GUIDFactory.newEventGUID(tenantId);
        String evDetData =
            "{\"LogType\":\"OPERATION\",\"StartDate\":\"2017-02-16T23:01:03.49\",\"EndDate\":\"2017-02-20T09:46:25.816\"," +
                "\"Hash\":\"6o0DS5ukbVsPHtynv2dW48tT/D65xUMs3orIkwsYU/Ron3RjEo3nzdiO4LliyNRNT3Eg/vhbitXsT+L2MWi4BA==\"," +
                "\"TimeStampToken\":\"MIIEezAVAgEAMBAMDk9wZXJhdGlvbiBPa2F5MIIEYAYJKoZIhvcNAQcCoIIEUTCCBE0CAQMxDzANBglghkgBZQMEAgMFADCBgAYLKoZIhvcNAQkQAQSgcQRv" +
                "MG0CAQEGASkwUTANBglghkgBZQMEAgMFAARA8RT79mPtXGJf5kadV2fyLnlAgZBVr7s7ZxMFRp4qr1GGPhU7Cu6+XXVBowT1moq8BRLm6U0VphGb51g8Idlh/wIBARgPMjAxNzAyMjAwO" +
                "TQ2MjZaMYIDsjCCA64CAQEwYzBdMQswCQYDVQQGEwJGUjEMMAoGA1UECBMDaWRmMQ4wDAYDVQQHEwVwYXJpczEPMA0GA1UEChMGVml0YW0uMR8wHQYDVQQDFBZDQV9zZXJ2ZXJfaW50ZX" +
                "JtZWRpYXRlAgIAsDANBglghkgBZQMEAgMFAKCCASAwGgYJKoZIhvcNAQkDMQ0GCyqGSIb3DQEJEAEEMBwGCSqGSIb3DQEJBTEPFw0xNzAyMjAwOTQ2MjZaMC0GCSqGSIb3DQEJNDEgMB4" +
                "wDQYJYIZIAWUDBAIDBQChDQYJKoZIhvcNAQENBQAwTwYJKoZIhvcNAQkEMUIEQKxHofJ/92kGaE6GNB1cJSshBcAOimapOlA3Vdphd7E9eahvILjI26Y/gHrCuLuzwkRQVsrRoKELp57y" +
                "OKpDRSowZAYLKoZIhvcNAQkQAi8xVTBTMFEwTzALBglghkgBZQMEAgMEQDiZ3/2zK5pV1xaSWoSwn4yrbEItdSp6xal0vz8sz1DODD0cb8B+ZOeFx0kaN/a5YwBjMe7TJx0eVZYpebtTp" +
                "DYwDQYJKoZIhvcNAQENBQAEggIATm+Gj6piHPhj3w211QRuvfhYVBynQ7Wb4UR/sUE8hYV8H/lFr9UDj9tu4Js9PxH4UyOsYZcIwx1aSWG4xKlVovsS/jMk/HXLdMUvXHKIc4Givopw2j" +
                "q4WM4PbXI1jCep2qNLEFj/RgHX23SuXnqsCQvZwh9ROx42hwwuzFmkV8PjdJtVn2NdSLO20tkPTZdb86ztt2qpU1A7lkEfCPryNluYfPhPaBfyxUbmeiMM0sPsDM/dQouu3BJnqSrenxC" +
                "VfkFxAbREWJFlKrC94caG3dWuIvVlabcP2uSnd23uAZmTSabJyHUMn2w0KD+chGseMnwF6xq8/6/RlKG6kPc5n1unl/R4q8DdeLLmSb0MBU+03buk4tmRaIdtAgXzG3InCopiJLT9fvL3" +
                "6o8M2da67rvizzBMoV/Zjtm4u1S3SVCKrf10go7vGOvF0IIAzFE+Un255jsGVrvWdYQcueS7s9GJIPEMN/9huIMM6WUyPkPcj4I6Way+iOoNBZT7aGx9tQEXsZYXiQ6/VDLmiBiWJKwCs" +
                "HNuIckMGV5KQcKAbJYPgqjBNHFgVD9hf9AX0R763soZy8BMW6UOZGNlY08QXsm2eNc70D0+6kRMCAu/iARyq04Cz+L5tKwtIBzbOOqD+6h9ok8ahXQCQZKBc3SRHbc3rm9SnnUdQUsb53OOkRE=\"," +
                "\"MinusOneMonthLogbookTraceabilityDate\":\"2017-01-16T23:01:03.49\",\"MinusOneYearLogbookTraceabilityDate\":\"2016-02-16T23:01:03.49\"," +
                "\"NumberOfElements\":112,\"FileName\":\"0_LogbookOperation_20170220_094625.zip\",\"Size\":3089204}";
        LogbookOperationParameters traceabilityParametersStart = LogbookParameterHelper.newLogbookOperationParameters(
            eip1, LOGBOOK_TRACEABILITY.getEventType(), eip1, LogbookTypeProcess.TRACEABILITY,
            StatusCode.STARTED, "Début de la sécurisation des journaux", eip1);
        LogbookOperationParameters traceabilityParametersStpStart =
            LogbookParameterHelper.newLogbookOperationParameters(
                GUIDFactory.newEventGUID(0), LOGBOOK_TRACEABILITY.getEventType(), eip1, LogbookTypeProcess.TRACEABILITY,
                StatusCode.STARTED, "Début du processus de sécurisation des journaux", eip1);
        LogbookOperationParameters traceabilityParametersStpAct1End =
            LogbookParameterHelper.newLogbookOperationParameters(
                GUIDFactory.newEventGUID(0), "OP_SECURISATION_TIMESTAMP", eip1, LogbookTypeProcess.TRACEABILITY,
                StatusCode.OK, "Succès de création du tampon d'horodatage de l'ensemble des journaux", eip1);
        LogbookOperationParameters traceabilityParametersStpAct2End =
            LogbookParameterHelper.newLogbookOperationParameters(
                GUIDFactory.newEventGUID(0), "OP_SECURISATION_STORAGE", eip1, LogbookTypeProcess.TRACEABILITY,
                StatusCode.OK, "Succès du stockage des journaux", eip1);
        LogbookOperationParameters traceabilityParametersStpEnd =
            LogbookParameterHelper.newLogbookOperationParameters(
                GUIDFactory.newEventGUID(0), LOGBOOK_TRACEABILITY.getEventType(), eip1, LogbookTypeProcess.TRACEABILITY,
                StatusCode.OK, "Succès du processus de sécurisation des journaux", eip1);
        traceabilityParametersStpEnd.putParameterValue(LogbookParameterName.eventDetailData, evDetData);

        LogbookOperationParameters traceabilityParameters2Start =
            LogbookParameterHelper.newLogbookOperationParameters(
                eip2, LOGBOOK_TRACEABILITY.getEventType(), eip2, LogbookTypeProcess.TRACEABILITY,
                StatusCode.STARTED, "Début de la sécurisation des journaux", eip2);
        LogbookOperationParameters traceabilityParameters2StpStart =
            LogbookParameterHelper.newLogbookOperationParameters(
                GUIDFactory.newEventGUID(0), LOGBOOK_TRACEABILITY.getEventType(), eip2, LogbookTypeProcess.TRACEABILITY,
                StatusCode.STARTED, "Début du processus de sécurisation des journaux", eip2);
        LogbookOperationParameters traceabilityParameters2StpEndFatal =
            LogbookParameterHelper.newLogbookOperationParameters(
                GUIDFactory.newEventGUID(0), LOGBOOK_TRACEABILITY.getEventType(), eip2, LogbookTypeProcess.TRACEABILITY,
                StatusCode.FATAL, "Succès du processus de sécurisation des journaux", eip2);

        try (final LogbookOperationsClient client =
            LogbookOperationsClientFactory.getInstance().getClient()) {
            client.checkStatus();
            client.create(traceabilityParametersStart);
            client.update(traceabilityParametersStpStart);
            client.update(traceabilityParametersStpAct1End);
            client.update(traceabilityParametersStpAct2End);
            client.update(traceabilityParametersStpEnd);
            client.create(traceabilityParameters2Start);
            client.update(traceabilityParameters2StpStart);
            client.update(traceabilityParameters2StpEndFatal);

            // request with limit and evDetData
            try {
                final Select select = new Select();
                final Query eventProcType = QueryHelper.eq("evTypeProc", LogbookTypeProcess.TRACEABILITY.name());
                final Query logType = QueryHelper
                    .eq(String.format("%s.%s.%s", LogbookDocument.EVENTS,
                        LogbookMongoDbName.eventDetailData.getDbname(), "LogType"),
                        "OPERATION");
                final Query eventType = QueryHelper.eq(
                    String.format("%s.%s", LogbookDocument.EVENTS, LogbookMongoDbName.eventType.getDbname()),
                    LOGBOOK_TRACEABILITY.getEventType());
                final Query outcome = QueryHelper
                    .eq(String.format("%s.%s", LogbookDocument.EVENTS, LogbookMongoDbName.outcome.getDbname()),
                        "OK");
                select.setLimitFilter(0, 1);
                select.setQuery(QueryHelper.and().add(eventProcType, logType, eventType, outcome));
                JsonNode json = client.selectOperation(select.getFinalSelect());
                RequestResponseOK response = JsonHandler.getFromJsonNode(json, RequestResponseOK.class);
                assertEquals(1, response.getHits().getTotal());

            } catch (InvalidCreateOperationException | InvalidParseOperationException e) {
                fail("Should not have raized an exception");
            }

            // request with sort
            try {
                final Select select = new Select();
                final Query eventProcType = QueryHelper.eq("evTypeProc", LogbookTypeProcess.TRACEABILITY.name());
                select.setQuery(QueryHelper.and().add(eventProcType));
                select.addOrderByDescFilter("evDateTime");
                JsonNode json = client.selectOperation(select.getFinalSelect());
                RequestResponseOK response = JsonHandler.getFromJsonNode(json, RequestResponseOK.class);
                Iterator responseResults = response.getResults().iterator();
                Map<String, Object> firstResult = (Map<String, Object>) responseResults.next();
                String eventIdProc1 = (String) firstResult.get(LogbookMongoDbName.eventIdentifierProcess.getDbname());
                assertEquals(eip2.toString(), eventIdProc1);
                Map<String, Object> secondResult = (Map<String, Object>) responseResults.next();
                String eventIdProc2 = (String) secondResult.get(LogbookMongoDbName.eventIdentifierProcess.getDbname());
                assertEquals(eip1.toString(), eventIdProc2);
            } catch (InvalidCreateOperationException | InvalidParseOperationException e) {
                fail("Should not have raized an exception");
            }
        }
    }

    @RunWithCustomExecutor
    @Test
    public final void testSelectOperationMultiTenant() throws Exception {
        try (final LogbookOperationsClient client = LogbookOperationsClientFactory.getInstance().getClient()) {
            VitamThreadUtils.getVitamSession().setTenantId(adminTenant);
            final GUID eip1 = GUIDFactory.newOperationLogbookGUID(adminTenant);
            LogbookOperationParameters importOntologyStart = LogbookParameterHelper.newLogbookOperationParameters(
                    eip1, IMPORT_ONTOLOGY.getEventType(), eip1, IMPORT_ONTOLOGY.getLogbookTypeProcess(),
                    StatusCode.STARTED, "Début du processus d'import de l'ontologie", eip1);
            client.checkStatus();
            client.create(importOntologyStart);

            final GUID eip2 = GUIDFactory.newOperationLogbookGUID(adminTenant);
            LogbookOperationParameters ingestTenantAdmin = LogbookParameterHelper.newLogbookOperationParameters(
                    eip2, INGEST_CLEANUP.getEventType(), eip2, INGEST_CLEANUP.getLogbookTypeProcess(),
                    StatusCode.STARTED, "Début de l'ingest", eip2);
            client.create(ingestTenantAdmin);

            VitamThreadUtils.getVitamSession().setTenantId(secondTenant);
            final GUID eip3 = GUIDFactory.newOperationLogbookGUID(secondTenant);
            LogbookOperationParameters ingestStart = LogbookParameterHelper.newLogbookOperationParameters(
                    eip3, INGEST_CLEANUP.getEventType(), eip3, INGEST_CLEANUP.getLogbookTypeProcess(),
                    StatusCode.STARTED, "Début de l'ingest", eip3);
            client.create(ingestStart);

            final Select select = new Select();
            JsonNode json = client.selectOperation(select.getFinalSelect());
            RequestResponseOK response = JsonHandler.getFromJsonNode(json, RequestResponseOK.class);
            List<LinkedHashMap<String, String>> list = response.getResults();
            Map<String, LinkedHashMap<String, String>> logbooks = list.stream()
                    .collect(Collectors.toMap(l -> l.get("evId"), l -> l));

            assertEquals(2, response.getHits().getTotal());
            assertThat(logbooks).containsKeys(eip1.getId(), eip3.getId());
        }
    }

    @RunWithCustomExecutor
    @Test
    public final void testOperation() throws LogbookClientException, VitamApplicationServerException {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        // Creation OK
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

        try (final LogbookOperationsClient client =
            LogbookOperationsClientFactory.getInstance().getClient()) {
            client.checkStatus();

            client.create(logbookParametersStart);

            // Update OK
            client.update(logbookParametersAppend);

            // Create KO since already exists
            try {
                client.create(logbookParametersWrongStart);
                fail("Should raized an exception");
            } catch (final LogbookClientException e) {
                // ignore
            }

            // Update KO since not found
            try {
                client.update(logbookParametersWrongAppend);
                fail("Should raized an exception");
            } catch (final LogbookClientException e) {
                // ignore
            }
            // Create KO since Bad Request
            final LogbookOperationParameters empty = LogbookParameterHelper.newLogbookOperationParameters();
            empty.putParameterValue(LogbookParameterName.eventIdentifierProcess,
                logbookParametersWrongAppend.getParameterValue(
                    LogbookParameterName.eventIdentifierProcess));
            try {
                client.create(empty);
                fail("Should raized an exception");
            } catch (final IllegalArgumentException e) {
                // ignore
            }
            try {
                client.update(empty);
                fail("Should raized an exception");
            } catch (final IllegalArgumentException e) {
                // ignore
            }
        }
    }


    @RunWithCustomExecutor
    @Test
    public final void testOperationMultiple() throws LogbookClientException, VitamApplicationServerException {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        // Creation OK
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

        try (final LogbookOperationsClient client =
            LogbookOperationsClientFactory.getInstance().getClient()) {
            client.checkStatus();

            client.create(logbookParametersStart);

            // Update multiple OK
            final long start = System.nanoTime();
            int i = 0;
            try {
                for (i = 0; i < NB_TEST; i++) {
                    client.update(logbookParametersAppend);
                }
            } catch (final LogbookClientServerException e) {
                LOGGER.error("Issue after " + i);
                fail(e.getMessage());
            }
            final long stop = System.nanoTime();
            final long start2 = System.nanoTime();
            i = 0;
            try {
                for (i = 0; i < NB_TEST; i++) {
                    client.updateDelegate(logbookParametersAppend);
                }
                client.commitUpdateDelegate(eip.getId());
            } catch (final LogbookClientServerException e) {
                LOGGER.error("Issue after " + i);
                fail(e.getMessage());
            }
            final long stop2 = System.nanoTime();
            LOGGER.warn("Multiple updates vs bulk updates: {} ms vs {} ms", (stop - start) / 1000000,
                (stop2 - start2) / 1000000);
            client.checkStatus();
        }
    }

    @RunWithCustomExecutor
    @Test
    public final void testLifeCycle() throws LogbookClientException, VitamApplicationServerException {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        // Creation OK
        final GUID eip = GUIDFactory.newEventGUID(0);
        logbookLcParametersStart = LogbookParameterHelper.newLogbookLifeCycleObjectGroupParameters(
            eip, "eventTypeValue1", eip, LogbookTypeProcess.INGEST,
            StatusCode.STARTED, "start ingest", "detail", eip);
        logbookLcParametersAppend = LogbookParameterHelper.newLogbookLifeCycleObjectGroupParameters(
            GUIDFactory.newEventGUID(0),
            "eventTypeValue1", eip, LogbookTypeProcess.INGEST,
            StatusCode.OK, "end ingest", "detail", eip);
        logbookLcParametersWrongStart = LogbookParameterHelper.newLogbookLifeCycleObjectGroupParameters(
            eip,
            "eventTypeValue2", eip, LogbookTypeProcess.INGEST,
            StatusCode.STARTED, "start ingest", "detail", eip);
        logbookLcParametersWrongAppend = LogbookParameterHelper.newLogbookLifeCycleObjectGroupParameters(
            GUIDFactory.newEventGUID(0),
            "eventTypeValue2", eip, LogbookTypeProcess.INGEST,
            StatusCode.OK, "end ingest", "detail", GUIDFactory.newEventGUID(0));

        try (final LogbookLifeCyclesClient client =
            LogbookLifeCyclesClientFactory.getInstance().getClient()) {
            client.checkStatus();

            client.create(logbookLcParametersStart);

            // Update OK
            client.update(logbookLcParametersAppend);

            // Create KO since already exists
            try {
                client.create(logbookLcParametersWrongStart);
                fail("Should raized an exception");
            } catch (final LogbookClientException e) {
                // ignore
            }

            // Update KO since not found
            try {
                client.update(logbookLcParametersWrongAppend);
                fail("Should raized an exception");
            } catch (final LogbookClientException e) {
                // ignore
            }
            // Create KO since Bad Request
            final LogbookLifeCycleObjectGroupParameters empty =
                LogbookParameterHelper.newLogbookLifeCycleObjectGroupParameters();
            empty.putParameterValue(LogbookParameterName.eventIdentifierProcess,
                logbookLcParametersWrongAppend.getParameterValue(
                    LogbookParameterName.eventIdentifierProcess));
            try {
                client.create(empty);
                fail("Should raized an exception");
            } catch (final IllegalArgumentException e) {
                // ignore
            }
            try {
                client.update(empty);
                fail("Should raized an exception");
            } catch (final IllegalArgumentException e) {
                // ignore
            }
        }
    }

    @RunWithCustomExecutor
    @Test
    public final void testLifeCycleMultiple() throws LogbookClientException, VitamApplicationServerException {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        // Creation OK
        final GUID eip = GUIDFactory.newEventGUID(0);
        logbookLcParametersStart = LogbookParameterHelper.newLogbookLifeCycleObjectGroupParameters(
            eip, "eventTypeValue1", eip, LogbookTypeProcess.INGEST,
            StatusCode.STARTED, "start ingest", "detail", eip);
        logbookLcParametersAppend = LogbookParameterHelper.newLogbookLifeCycleObjectGroupParameters(
            GUIDFactory.newEventGUID(0),
            "eventTypeValue1", eip, LogbookTypeProcess.INGEST,
            StatusCode.OK, "end ingest", "detail", eip);
        logbookLcParametersWrongStart = LogbookParameterHelper.newLogbookLifeCycleObjectGroupParameters(
            eip,
            "eventTypeValue2", eip, LogbookTypeProcess.INGEST,
            StatusCode.STARTED, "start ingest", "detail", eip);
        logbookLcParametersWrongAppend = LogbookParameterHelper.newLogbookLifeCycleObjectGroupParameters(
            GUIDFactory.newEventGUID(0),
            "eventTypeValue2", GUIDFactory.newEventGUID(0), LogbookTypeProcess.INGEST,
            StatusCode.OK, "end ingest", "detail", eip);

        try (final LogbookLifeCyclesClient client =
            LogbookLifeCyclesClientFactory.getInstance().getClient()) {
            client.checkStatus();

            client.create(logbookLcParametersStart);

            // Update multiple OK
            int i = 0;
            try {
                for (i = 0; i < NB_TEST; i++) {
                    client.update(logbookLcParametersAppend);
                }
            } catch (final LogbookClientServerException e) {
                LOGGER.error("Issue after " + i);
                fail(e.getMessage());
            }
        }
    }


    @RunWithCustomExecutor
    @Test
    public final void testLifeCycleBulk() throws LogbookClientException, VitamApplicationServerException {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        // Creation OK
        final GUID eip = GUIDFactory.newEventGUID(0);
        final GUID eip2 = GUIDFactory.newEventGUID(0);
        logbookLcParametersStart = LogbookParameterHelper.newLogbookLifeCycleObjectGroupParameters(
            eip, "eventTypeValue1", eip, LogbookTypeProcess.INGEST,
            StatusCode.STARTED, "start ingest", "detail", eip);
        logbookLcParametersAppend = LogbookParameterHelper.newLogbookLifeCycleObjectGroupParameters(
            GUIDFactory.newEventGUID(0),
            "eventTypeValue1", eip, LogbookTypeProcess.INGEST,
            StatusCode.OK, "end ingest", "detail", eip);
        logbookLcParametersWrongStart = LogbookParameterHelper.newLogbookLifeCycleObjectGroupParameters(
            eip2,
            "eventTypeValue2", eip2, LogbookTypeProcess.INGEST,
            StatusCode.STARTED, "start ingest", "detail", eip2);
        logbookLcParametersWrongAppend = LogbookParameterHelper.newLogbookLifeCycleObjectGroupParameters(
            GUIDFactory.newEventGUID(0),
            "eventTypeValue2", GUIDFactory.newEventGUID(0), LogbookTypeProcess.INGEST,
            StatusCode.OK, "end ingest", "detail", eip2);

        List<LogbookLifeCycleObjectGroupParameters> logbookLifeCycleObjectGroupParameters = Lists
            .newArrayList(logbookLcParametersStart, logbookLcParametersAppend);

        ArrayList<LogbookLifeCycleObjectGroupParameters> logbookLifeCycleObjectGroupParameters1 =
            Lists.newArrayList(logbookLcParametersWrongStart,
                logbookLcParametersWrongAppend);

        LogbookLifeCycleObjectGroupModel logbookLifeCycleModel = new LogbookLifeCycleObjectGroupModel("10",
            logbookLifeCycleObjectGroupParameters);
        LogbookLifeCycleObjectGroupModel logbookLifeCycleModel2 = new LogbookLifeCycleObjectGroupModel("10",
            logbookLifeCycleObjectGroupParameters1);

        try (final LogbookLifeCyclesClient client =
            LogbookLifeCyclesClientFactory.getInstance().getClient()) {

            client.checkStatus();
            client.bulkObjectGroup(eip.toString(), Lists.newArrayList(logbookLifeCycleModel, logbookLifeCycleModel2));

            // Update multiple OK

            FindIterable<LogbookLifeCycleObjectGroup> objects =
                 LIFECYCLE_OBJECTGROUP_IN_PROCESS.<LogbookLifeCycleObjectGroup>getVitamCollection()
                    .getCollection().find();
            ArrayList<LogbookLifeCycleObjectGroup> objects1 = Lists.newArrayList(objects);
            assertThat(objects1).hasSize(2).extracting("_id").containsExactly(eip.toString(), eip2.toString());
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testGetRawUnitLifeCycleByIds() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(2);

        // Given
        JsonNode doc1 = JsonHandler.getFromFile(PropertiesUtils.getResourceFile("doc1.json"));
        JsonNode doc2 = JsonHandler.getFromFile(PropertiesUtils.getResourceFile("doc2.json"));
        JsonNode doc3 = JsonHandler.getFromFile(PropertiesUtils.getResourceFile("doc3.json"));

        String GUID1 = "aeaqaaaaaahoii7cab2ggalizruha2iaaabq";
        String GUID2 = "aeaqaaaaaahoii7cab2ggalizruhbziaaaaq";
        String GUID3 = "aeaqaaaaaahoii7cab2ggalizruhbzqaaaba";

        LogbookCollections.LIFECYCLE_UNIT.getCollection().insertMany(
            Arrays.asList(
                new LogbookLifeCycleUnit(doc1),
                new LogbookLifeCycleUnit(doc2),
                new LogbookLifeCycleUnit(doc3)
            )
        );


        // Test unknown id
        given()
            .header(GlobalDataRest.X_TENANT_ID, 2)
            .contentType(ContentType.JSON)
            .body(JsonHandler.toJsonNode(Arrays.asList(GUID1, "Unkown id")))
            .when().get("/raw/unitlifecycles/byids")
            .then().statusCode(Response.Status.NOT_FOUND.getStatusCode());

        // Test wrong tenant id
        given()
            .header(GlobalDataRest.X_TENANT_ID, 2)
            .contentType(ContentType.JSON)
            .body(JsonHandler.toJsonNode(Arrays.asList(GUID1, GUID2)))
            .when().get("/raw/unitlifecycles/byids")
            .then().statusCode(Response.Status.NOT_FOUND.getStatusCode());

        // Test OK
        JsonNode responseJson = given()
            .header(GlobalDataRest.X_TENANT_ID, 2)
            .contentType(ContentType.JSON)
            .body(JsonHandler.toJsonNode(Arrays.asList(GUID1, GUID3)))
            .when().get("/raw/unitlifecycles/byids")
            .then().statusCode(Response.Status.OK.getStatusCode())
            .extract().body().as(JsonNode.class);
        RequestResponseOK requestResponseOK = RequestResponseOK.getFromJsonNode(responseJson);

        List results = requestResponseOK.getResults();
        assertThat(results).hasSize(2);
        results.sort(
            Comparator.comparing(o -> ((ObjectNode) o).get("_id").asText())
        );
        JsonAssert.assertJsonEquals(results.get(0), doc1);
        JsonAssert.assertJsonEquals(results.get(1), doc3);
    }

}
