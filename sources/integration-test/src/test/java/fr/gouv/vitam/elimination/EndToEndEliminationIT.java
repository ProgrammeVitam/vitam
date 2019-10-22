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
package fr.gouv.vitam.elimination;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import com.mongodb.client.model.Filters;
import fr.gouv.vitam.access.internal.client.AccessInternalClient;
import fr.gouv.vitam.access.internal.client.AccessInternalClientFactory;
import fr.gouv.vitam.access.internal.common.exception.AccessInternalClientNotFoundException;
import fr.gouv.vitam.access.internal.common.exception.AccessInternalClientServerException;
import fr.gouv.vitam.access.internal.rest.AccessInternalMain;
import fr.gouv.vitam.batch.report.rest.BatchReportMain;
import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.DataLoader;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.accesslog.AccessLogUtils;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.exception.AccessUnauthorizedException;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.format.identification.FormatIdentifierFactory;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.elimination.EliminationRequestBody;
import fr.gouv.vitam.common.model.objectgroup.ObjectGroupResponse;
import fr.gouv.vitam.common.model.objectgroup.QualifiersModel;
import fr.gouv.vitam.common.model.objectgroup.VersionsModel;
import fr.gouv.vitam.common.model.processing.WorkFlow;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClient;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClientFactory;
import fr.gouv.vitam.ingest.internal.upload.rest.IngestInternalMain;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.rest.MetadataMain;
import fr.gouv.vitam.processing.common.model.ProcessWorkflow;
import fr.gouv.vitam.processing.engine.core.monitoring.ProcessMonitoringImpl;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.processing.management.rest.ProcessManagementMain;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.storage.offers.common.rest.DefaultOfferMain;
import fr.gouv.vitam.worker.core.plugin.elimination.model.EliminationActionObjectGroupStatus;
import fr.gouv.vitam.worker.core.plugin.elimination.model.EliminationActionUnitStatus;
import fr.gouv.vitam.worker.core.plugin.elimination.report.EliminationActionObjectGroupReportEntry;
import fr.gouv.vitam.worker.core.plugin.elimination.report.EliminationActionUnitReportEntry;
import fr.gouv.vitam.worker.server.rest.WorkerMain;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import io.restassured.RestAssured;
import net.javacrumbs.jsonunit.JsonAssert;
import org.apache.commons.collections4.SetUtils;
import org.assertj.core.util.Lists;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.guid.GUIDFactory.newOperationLogbookGUID;
import static fr.gouv.vitam.common.stream.StreamUtils.consumeAnyEntityAndClose;
import static io.restassured.RestAssured.get;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Ingest Internal integration test
 */
public class EndToEndEliminationIT extends VitamRuleRunner {
    private static final String DEFAULT_STRATEGY = "default";

    @ClassRule
    public static VitamServerRunner runner =
            new VitamServerRunner(EndToEndEliminationIT.class, mongoRule.getMongoDatabase().getName(),
                    elasticsearchRule.getClusterName(),
                    Sets.newHashSet(
                            MetadataMain.class,
                            WorkerMain.class,
                            AdminManagementMain.class,
                            LogbookMain.class,
                            WorkspaceMain.class,
                            ProcessManagementMain.class,
                            AccessInternalMain.class,
                            IngestInternalMain.class,
                            StorageMain.class,
                            DefaultOfferMain.class,
                            BatchReportMain.class
                    ));

    private static final Integer tenantId = 0;
    private static final long SLEEP_TIME = 20L;
    private static final long NB_TRY = 18000; // equivalent to 16 minute


    private static final String METADATA_PATH = "/metadata/v1";
    private static final String PROCESSING_PATH = "/processing/v1";
    private static final String WORKER_PATH = "/worker/v1";
    private static final String WORKSPACE_PATH = "/workspace/v1";
    private static final String LOGBOOK_PATH = "/logbook/v1";
    private static final String INGEST_INTERNAL_PATH = "/ingest/v1";
    private static final String ACCESS_INTERNAL_PATH = "/access-internal/v1";
    private static final String STORAGE_PATH = "/storage/v1";
    private static final String OFFER_PATH = "/offer/v1";
    private static final String BATCH_REPORT_PATH = "/batchreport/v1";
    private static final String WORKFLOW_ID = "DEFAULT_WORKFLOW";
    private static final String WORKFLOW_IDENTIFIER = "PROCESS_SIP_UNITARY";
    private WorkFlow workflow = WorkFlow.of(WORKFLOW_ID, WORKFLOW_IDENTIFIER, "INGEST");

    private static final String SAINT_DENIS_UNIVERSITE_LIGNE_13 = "1_Saint Denis Université (ligne 13)";
    private static final String SAINT_DENIS_BASILIQUE = "Saint Denis Basilique";
    private static final String CARREFOUR_PLEYEL = "Carrefour Pleyel";
    private static final String SAINT_LAZARE = "Saint-Lazare";
    private static final String MARX_DORMOY = "Marx Dormoy";
    private static final String MONTPARNASSE = "Montparnasse.txt";
    private static final String ORIGINATING_AGENCY = "RATP";


    private static String CONFIG_SIEGFRIED_PATH = "";

    private static String TEST_ELIMINATION_SIP =
            "elimination/TEST_ELIMINATION.zip";


    private static String ACCESSION_REGISTER_DETAIL =
            "elimination/accession_regoister_detail.json";

    private static String ACCESSION_REGISTER_SUMMARY =
            "elimination/accession_regoister_summary.json";



    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        handleBeforeClass(0, 1);
        CONFIG_SIEGFRIED_PATH =
                PropertiesUtils.getResourcePath("integration-ingest-internal/format-identifiers.conf").toString();

        FormatIdentifierFactory.getInstance().changeConfigurationFile(CONFIG_SIEGFRIED_PATH);


        new DataLoader("integration-ingest-internal").prepareData();

    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        handleAfterClass(0, 1);
        runAfter();
        VitamClientFactory.resetConnections();
    }

    @Before
    public void setUpBefore() {
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(0));
    }

    private void wait(String operationId) {
        int nbTry = 0;
        ProcessingManagementClient processingClient =
                ProcessingManagementClientFactory.getInstance().getClient();
        while (!processingClient.isOperationCompleted(operationId)) {
            try {
                Thread.sleep(SLEEP_TIME);
            } catch (InterruptedException e) {
                SysErrLogger.FAKE_LOGGER.ignoreLog(e);
            }
            if (nbTry == NB_TRY)
                break;
            nbTry++;
        }
    }


    public static void prepareVitamSession() {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        VitamThreadUtils.getVitamSession().setContractId("aName3");
        VitamThreadUtils.getVitamSession().setContextId("Context_IT");
    }

    @RunWithCustomExecutor
    @Test
    public void testServersStatus() {
        RestAssured.port = VitamServerRunner.PORT_SERVICE_PROCESSING;
        RestAssured.basePath = PROCESSING_PATH;
        get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());

        RestAssured.port = VitamServerRunner.PORT_SERVICE_WORKSPACE;
        RestAssured.basePath = WORKSPACE_PATH;
        get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());

        RestAssured.port = VitamServerRunner.PORT_SERVICE_METADATA;
        RestAssured.basePath = METADATA_PATH;
        get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());

        RestAssured.port = VitamServerRunner.PORT_SERVICE_WORKER;
        RestAssured.basePath = WORKER_PATH;
        get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());

        RestAssured.port = VitamServerRunner.PORT_SERVICE_LOGBOOK;
        RestAssured.basePath = LOGBOOK_PATH;
        get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());

        RestAssured.port = VitamServerRunner.PORT_SERVICE_INGEST_INTERNAL;
        RestAssured.basePath = INGEST_INTERNAL_PATH;
        get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());

        RestAssured.port = VitamServerRunner.PORT_SERVICE_ACCESS_INTERNAL;
        RestAssured.basePath = ACCESS_INTERNAL_PATH;
        get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());

        RestAssured.port = VitamServerRunner.PORT_SERVICE_STORAGE;
        RestAssured.basePath = STORAGE_PATH;
        get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());

        RestAssured.port = VitamServerRunner.PORT_SERVICE_OFFER;
        RestAssured.basePath = OFFER_PATH;
        get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());

        RestAssured.port = VitamServerRunner.PORT_SERVICE_BATCH_REPORT;
        RestAssured.basePath = BATCH_REPORT_PATH;
        get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());
    }

    @RunWithCustomExecutor
    @Test
    public void testEliminationAction() throws Exception {
        final GUID ingestOperationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        prepareVitamSession();
        VitamThreadUtils.getVitamSession().setRequestId(ingestOperationGuid);
        // workspace client unzip SIP in workspace
        final InputStream zipInputStreamSipObject =
                PropertiesUtils.getResourceAsStream(TEST_ELIMINATION_SIP);

        // init default logbook operation
        final List<LogbookOperationParameters> params = new ArrayList<>();
        final LogbookOperationParameters initParameters = LogbookParametersFactory.newLogbookOperationParameters(
                ingestOperationGuid, "Process_SIP_unitary", ingestOperationGuid,
                LogbookTypeProcess.INGEST, StatusCode.STARTED,
                ingestOperationGuid.toString(), ingestOperationGuid);
        params.add(initParameters);

        // call ingest
        IngestInternalClientFactory.getInstance().changeServerPort(VitamServerRunner.PORT_SERVICE_INGEST_INTERNAL);
        final IngestInternalClient client = IngestInternalClientFactory.getInstance().getClient();
        client.uploadInitialLogbook(params);

        // init workflow before execution
        client.initWorkflow(workflow);

        client.upload(zipInputStreamSipObject, CommonMediaType.ZIP_TYPE, workflow, ProcessAction.RESUME.name());

        awaitForWorkflowTerminationWithStatus(ingestOperationGuid, StatusCode.OK);

        // Check ingested units
        final AccessInternalClient accessInternalClient = AccessInternalClientFactory.getInstance().getClient();
        final RequestResponseOK<JsonNode> ingestedUnits = selectUnitsByOpi(ingestOperationGuid, accessInternalClient);
        final RequestResponseOK<JsonNode> ingestedGots = selectGotsByOpi(ingestOperationGuid, accessInternalClient);

        assertThat(ingestedUnits.getResults()).hasSize(6);
        assertThat(ingestedGots.getResults()).hasSize(3);

        Set<String> ingestedObjectIds = getObjectIds(ingestedGots);
        assertThat(ingestedObjectIds).hasSize(3);

        // elimination action
        final GUID eliminationActionOperationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        VitamThreadUtils.getVitamSession().setRequestId(eliminationActionOperationGuid);

        SelectMultiQuery analysisDslRequest = new SelectMultiQuery();
        analysisDslRequest
                .addQueries(QueryHelper.eq(VitamFieldsHelper.initialOperation(), ingestOperationGuid.toString()));

        EliminationRequestBody eliminationRequestBody = new EliminationRequestBody(
                "2018-01-01", analysisDslRequest.getFinalSelect());

        final RequestResponse<JsonNode> actionResult =
                accessInternalClient.startEliminationAction(eliminationRequestBody);

        assertThat(actionResult.isOk()).isTrue();

        awaitForWorkflowTerminationWithStatus(eliminationActionOperationGuid, StatusCode.WARNING);

        // DSL check
        final RequestResponseOK<JsonNode> remainingUnits = selectUnitsByOpi(ingestOperationGuid, accessInternalClient);
        final RequestResponseOK<JsonNode> remainingGots = selectGotsByOpi(ingestOperationGuid, accessInternalClient);

        assertThat(remainingUnits.getResults()).hasSize(3);
        assertThat(remainingGots.getResults()).hasSize(2);

        Set<String> remainingObjectIds = getObjectIds(remainingGots);
        assertThat(remainingObjectIds).hasSize(2);

        // Check Accession Register Detail
        List<String> excludeFields = Lists
                .newArrayList("_id", "StartDate", "LastUpdate", "EndDate", "Opc", "Opi", "CreationDate", "OperationIds");
        assertJsonEquals(ACCESSION_REGISTER_DETAIL, JsonHandler.toJsonNode(
                FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getCollection().find()), excludeFields);

        // Check Accession Register Summary
        assertJsonEquals(ACCESSION_REGISTER_SUMMARY,
                JsonHandler.toJsonNode(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getCollection().find()),
                excludeFields);

        // Check remaining units / gots & objects ids (low level)

        Set<String> ingestedUnitIds = getIds(ingestedUnits);
        Map<String, JsonNode> ingestedUnitsByTitle = mapByField(ingestedUnits, "Title");

        Set<String> expectedRemainingUnitIds = new HashSet<>(Arrays.asList(
            getId(ingestedUnitsByTitle.get(SAINT_DENIS_UNIVERSITE_LIGNE_13)),
            getId(ingestedUnitsByTitle.get(SAINT_DENIS_BASILIQUE)),
            getId(ingestedUnitsByTitle.get(CARREFOUR_PLEYEL))
        ));
        Set<String> expectedDeletedUnitIds = SetUtils.difference(ingestedUnitIds, expectedRemainingUnitIds);

        for (String id : expectedRemainingUnitIds) {
            checkUnitExistence(id, true);
        }

        for (String id : expectedDeletedUnitIds) {
            checkUnitExistence(id, false);
        }

        Set<String> ingestedGotIds = getIds(ingestedGots);
        Set<String> expectedRemainingGotIds = new HashSet<>(Arrays.asList(
            getObjectGroupId(ingestedUnitsByTitle.get(SAINT_DENIS_UNIVERSITE_LIGNE_13)),
            getObjectGroupId(ingestedUnitsByTitle.get(SAINT_DENIS_BASILIQUE))
        ));
        Set<String> expectedDeletedGotIds = SetUtils.difference(ingestedGotIds, expectedRemainingGotIds);

        for (String id : expectedRemainingGotIds) {
            checkObjectGroupExistence(id, true);
        }

        for (String id : expectedDeletedGotIds) {
            checkObjectGroupExistence(id, false);
        }


        Set<String> expectedRemainingObjectIds = ingestedGots.getResults().stream()
                .filter(got -> expectedRemainingGotIds.contains(getId(got)))
                .flatMap(got -> getObjectIds(got).stream())
                .collect(Collectors.toSet());
        Set<String> expectedDeletedObjectIds = SetUtils.difference(ingestedObjectIds, expectedRemainingObjectIds);


        for (String id : expectedRemainingObjectIds) {
            checkObjectExistence(id, true);
        }

        for (String id : expectedDeletedObjectIds) {
            checkObjectExistence(id, false);
        }

        // Check detached GOT

        String detachedGotId = getObjectGroupId(ingestedUnitsByTitle.get(SAINT_DENIS_BASILIQUE));

        JsonNode detachedGotBeforeElimination = getById(ingestedGots, detachedGotId);
        assertThat(detachedGotBeforeElimination.get(VitamFieldsHelper.unitups())).hasSize(2);
        assertThat(detachedGotBeforeElimination.get(VitamFieldsHelper.allunitups())).hasSize(6);

        JsonNode detachedGotAfterElimination = getById(remainingGots, detachedGotId);
        assertThat(detachedGotAfterElimination.get(VitamFieldsHelper.version()).asInt()).isEqualTo(
            detachedGotBeforeElimination.get(VitamFieldsHelper.version()).asInt() + 1);
        assertThat(detachedGotAfterElimination.get(VitamFieldsHelper.unitups())).hasSize(1);
        assertThat(detachedGotAfterElimination.get(VitamFieldsHelper.allunitups())).hasSize(2);

        assertThat(detachedGotAfterElimination.get(VitamFieldsHelper.unitups()).get(0).asText()).isEqualTo(
                getId(ingestedUnitsByTitle.get(SAINT_DENIS_BASILIQUE)));

        // Check report
        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {

            Response reportResponse = null;

            try {
                reportResponse = storageClient.getContainerAsync(DEFAULT_STRATEGY,
                        eliminationActionOperationGuid.toString() + ".json", DataCategory.REPORT,
                        AccessLogUtils.getNoLogAccessLog());

                assertThat(reportResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

                try (InputStream is = reportResponse.readEntity(InputStream.class)) {
                    ReportModel report = JsonHandler.getFromInputStream(is, ReportModel.class);

                    //Check report units
                    assertThat(report.units).hasSize(6);

                    Map<String, EliminationActionUnitReportEntry> unitReportByTitle = report.units.stream()
                            .collect(Collectors.toMap(
                                    entry -> ingestedUnits.getResults().stream()
                                            .filter(unit -> unit.get(VitamFieldsHelper.id()).asText().equals(entry.getUnitId()))
                                            .map(this::getTitle)
                                            .findFirst().get()
                                    , entry -> entry));

                    assertThat(unitReportByTitle.get(CARREFOUR_PLEYEL).getStatus()).isEqualTo(
                            EliminationActionUnitStatus.GLOBAL_STATUS_KEEP);
                    assertThat(unitReportByTitle.get(SAINT_DENIS_BASILIQUE).getStatus()).isEqualTo(
                            EliminationActionUnitStatus.NON_DESTROYABLE_HAS_CHILD_UNITS);
                    assertThat(unitReportByTitle.get(SAINT_DENIS_UNIVERSITE_LIGNE_13).getStatus()).isEqualTo(
                            EliminationActionUnitStatus.NON_DESTROYABLE_HAS_CHILD_UNITS);
                    assertThat(unitReportByTitle.get(SAINT_LAZARE).getStatus()).isEqualTo(
                            EliminationActionUnitStatus.DELETED);
                    assertThat(unitReportByTitle.get(MARX_DORMOY).getStatus()).isEqualTo(
                            EliminationActionUnitStatus.DELETED);
                    assertThat(unitReportByTitle.get(MONTPARNASSE).getStatus()).isEqualTo(
                            EliminationActionUnitStatus.DELETED);

                    for (EliminationActionUnitReportEntry unitReport : report.units) {

                        JsonNode unit = ingestedUnits.getResults().stream()
                                .filter(ingestedUnit -> getId(ingestedUnit)
                                        .equals(unitReport.getUnitId()))
                                .findFirst().get();

                        assertThat(unitReport.getInitialOperation()).isEqualTo(ingestOperationGuid.toString());
                        assertThat(unitReport.getOriginatingAgency()).isEqualTo(ORIGINATING_AGENCY);
                        assertThat(unitReport.getObjectGroupId())
                                .isEqualTo(getObjectGroupId(unit));
                    }

                    //Check report gots
                    assertThat(report.objectGroups).hasSize(2);

                    String deletedGotId = getObjectGroupId(ingestedUnitsByTitle.get(MARX_DORMOY));
                    EliminationActionObjectGroupReportEntry deletedGotReport = report.objectGroups.stream()
                            .filter(got -> got.getObjectGroupId().equals(deletedGotId))
                            .findFirst().get();
                    assertThat(deletedGotReport.getStatus()).isEqualTo(EliminationActionObjectGroupStatus.DELETED);
                    assertThat(deletedGotReport.getInitialOperation()).isEqualTo(ingestOperationGuid.toString());
                    assertThat(deletedGotReport.getOriginatingAgency()).isEqualTo(ORIGINATING_AGENCY);
                    assertThat(deletedGotReport.getObjectIds()).containsExactlyInAnyOrderElementsOf(
                            getObjectIds(ingestedGots.getResults().stream()
                                    .filter(got -> getId(got).equals(deletedGotId))
                                    .findFirst().get()));
                    assertThat(deletedGotReport.getDeletedParentUnitIds()).isNullOrEmpty();

                    EliminationActionObjectGroupReportEntry detachedGotReport = report.objectGroups.stream()
                            .filter(got -> got.getObjectGroupId().equals(detachedGotId))
                            .findFirst().get();
                    assertThat(detachedGotReport.getStatus())
                            .isEqualTo(EliminationActionObjectGroupStatus.PARTIAL_DETACHMENT);
                    assertThat(detachedGotReport.getInitialOperation()).isEqualTo(ingestOperationGuid.toString());
                    assertThat(detachedGotReport.getOriginatingAgency()).isEqualTo(ORIGINATING_AGENCY);
                    assertThat(detachedGotReport.getObjectIds()).isNullOrEmpty();
                    assertThat(detachedGotReport.getDeletedParentUnitIds()).containsExactlyInAnyOrder(
                            getId(ingestedUnitsByTitle.get(MONTPARNASSE)));
                }

            } finally {
                consumeAnyEntityAndClose(reportResponse);
            }
        }
    }

    private void assertJsonEquals(String resourcesFile, JsonNode actual, List<String> excludeFields)
            throws FileNotFoundException, InvalidParseOperationException {
        JsonNode expected = JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(resourcesFile));
        if (excludeFields != null) {
            expected.forEach(e -> {
                ObjectNode ee = (ObjectNode) e;
                ee.remove(excludeFields);
                if (ee.has("Events")) {
                    ee.get("Events").forEach(a -> ((ObjectNode) a).remove(excludeFields));
                }
            });
            actual.forEach(e -> {
                ObjectNode ee = (ObjectNode) e;
                ee.remove(excludeFields);
                if (ee.has("Events")) {
                    ee.get("Events").forEach(a -> ((ObjectNode) a).remove(excludeFields));
                }

            });
        }

        JsonAssert
                .assertJsonEquals(expected, actual, JsonAssert.whenIgnoringPaths(excludeFields.toArray(new String[]{})));
    }

    private String getId(JsonNode unit) {
        return unit.get(VitamFieldsHelper.id()).asText();
    }

    private String getObjectGroupId(JsonNode unit) {
        return unit.has(VitamFieldsHelper.object()) ? unit.get(VitamFieldsHelper.object()).asText() : null;
    }

    private String getTitle(JsonNode unit) {
        return unit.get("Title").asText();
    }

    private static class ReportModel {
        @JsonProperty("units")
        private List<EliminationActionUnitReportEntry> units;

        @JsonProperty("objectGroups")
        private List<EliminationActionObjectGroupReportEntry> objectGroups;
    }

    private JsonNode getById(RequestResponseOK<JsonNode> ingestedGots, String id) {
        return ingestedGots.getResults().stream()
                .filter(got -> id.equals(got.get(VitamFieldsHelper.id()).asText()))
                .findFirst().get();
    }

    private void checkUnitExistence(String unitId, boolean shouldExist)
            throws StorageNotFoundClientException, StorageServerClientException {
        checkDocumentExistence(MetadataCollections.UNIT.getVitamCollection(), unitId, shouldExist);
        checkDocumentExistence(LogbookCollections.LIFECYCLE_UNIT.getVitamCollection(), unitId, shouldExist);
        checkFileInStorage(DataCategory.UNIT, unitId + ".json", shouldExist);
    }

    private void checkObjectGroupExistence(String gotId, boolean shouldExist)
            throws StorageNotFoundClientException, StorageServerClientException {
        checkDocumentExistence(MetadataCollections.OBJECTGROUP.getVitamCollection(), gotId, shouldExist);
        checkDocumentExistence(LogbookCollections.LIFECYCLE_OBJECTGROUP.getVitamCollection(), gotId, shouldExist);
        checkFileInStorage(DataCategory.OBJECTGROUP, gotId + ".json", shouldExist);
    }

    private void checkObjectExistence(String objectId, boolean shouldExist)
            throws StorageNotFoundClientException, StorageServerClientException {
        checkFileInStorage(DataCategory.OBJECT, objectId, shouldExist);
    }

    private void checkFileInStorage(DataCategory dataCategory, String filename, boolean shouldExist)
            throws StorageNotFoundClientException, StorageServerClientException {
        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {

            List<String> offers = storageClient.getOffers(DEFAULT_STRATEGY);
            JsonNode information = storageClient.getInformation(DEFAULT_STRATEGY, dataCategory, filename, offers, false);
            boolean fileFound = information.size() > 0;
            assertThat(fileFound).isEqualTo((boolean) shouldExist);
        }
    }

    private void checkDocumentExistence(VitamCollection collection, String documentId,
                                        boolean shouldExist) {

        int expectedHits = shouldExist ? 1 : 0;

        // Logbook LFCs are not persisted in ES
        if (collection.getEsClient() != null) {
            long totalHits = collection.getEsClient().getClient()
                    .prepareSearch(getAliasName(collection, VitamThreadUtils.getVitamSession().getTenantId()))
                    .setTypes(VitamCollection.getTypeunique())
                    .setQuery(QueryBuilders.termQuery(VitamDocument.ID, documentId))
                    .get()
                    .getHits()
                    .getTotalHits();
            assertThat(totalHits).isEqualTo(expectedHits);
        }

        assertThat(collection.getCollection().find(Filters.eq(VitamDocument.ID, documentId))
                .iterator()).hasSize(expectedHits);
    }

    private Set<String> getObjectIds(RequestResponseOK<JsonNode> gots) {
        Set<String> objectIds = new HashSet<>();
        for (JsonNode gotJson : gots.getResults()) {
            objectIds.addAll(getObjectIds(gotJson));
        }
        return objectIds;
    }

    private Set<String> getObjectIds(JsonNode gotJson) {
        Set<String> objectIds = new HashSet<>();

        try {
            ObjectGroupResponse gotResponse = JsonHandler.getFromJsonNode(gotJson, ObjectGroupResponse.class);

            for (QualifiersModel qualifier : gotResponse.getQualifiers()) {
                for (VersionsModel version : qualifier.getVersions()) {
                    if (version.getPhysicalId() == null) {
                        objectIds.add(version.getId());
                    }
                }
            }
        } catch (InvalidParseOperationException e) {
            throw new RuntimeException(e);
        }
        return objectIds;
    }

    private Set<String> getIds(RequestResponseOK<JsonNode> ingestedGots) {
        return mapByField(ingestedGots, VitamFieldsHelper.id()).keySet();
    }

    private String getAliasName(final VitamCollection collection, Integer tenantId) {
        return collection.getName().toLowerCase() + "_" + tenantId.toString();
    }

    private Map<String, JsonNode> mapByField(RequestResponseOK<JsonNode> requestResponseOK, String title) {
        return requestResponseOK.getResults()
                .stream()
                .collect(Collectors.toMap(node -> node.get(title).asText(), node -> node));
    }

    private RequestResponseOK<JsonNode> selectGotsByOpi(GUID ingestOperationGuid,
                                                        AccessInternalClient accessInternalClient)
            throws InvalidCreateOperationException, InvalidParseOperationException, AccessInternalClientServerException,
            AccessInternalClientNotFoundException, AccessUnauthorizedException, BadRequestException {
        SelectMultiQuery checkEliminationGotDslRequest = new SelectMultiQuery();
        checkEliminationGotDslRequest.addQueries(
                QueryHelper.eq(VitamFieldsHelper.initialOperation(), ingestOperationGuid.toString()));

        return (RequestResponseOK<JsonNode>) accessInternalClient
                .selectObjects(checkEliminationGotDslRequest.getFinalSelect());
    }

    private RequestResponseOK<JsonNode> selectUnitsByOpi(GUID ingestOperationGuid,
                                                         AccessInternalClient accessInternalClient)
            throws InvalidCreateOperationException, InvalidParseOperationException, AccessInternalClientServerException,
            AccessInternalClientNotFoundException, AccessUnauthorizedException, BadRequestException {
        SelectMultiQuery checkEliminationDslRequest = new SelectMultiQuery();
        checkEliminationDslRequest.addQueries(
                QueryHelper.eq(VitamFieldsHelper.initialOperation(), ingestOperationGuid.toString()));

        return (RequestResponseOK<JsonNode>) accessInternalClient
                .selectUnits(checkEliminationDslRequest.getFinalSelect());
    }

    private void awaitForWorkflowTerminationWithStatus(GUID operationGuid, StatusCode ok) {

        wait(operationGuid.toString());

        ProcessWorkflow processWorkflow =
                ProcessMonitoringImpl.getInstance().findOneProcessWorkflow(operationGuid.toString(), tenantId);

        assertNotNull(processWorkflow);
        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
        assertEquals(ok, processWorkflow.getStatus());
    }

}
