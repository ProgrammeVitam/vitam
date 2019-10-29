/*
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
package fr.gouv.vitam.purge;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Iterators;
import com.google.common.collect.Sets;
import com.google.common.collect.Streams;
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
import fr.gouv.vitam.common.VitamConfiguration;
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
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.AccessUnauthorizedException;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
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
import fr.gouv.vitam.common.model.dip.DataObjectVersions;
import fr.gouv.vitam.common.model.dip.DipExportRequest;
import fr.gouv.vitam.common.model.dip.ExportRequestParameters;
import fr.gouv.vitam.common.model.dip.ExportType;
import fr.gouv.vitam.common.model.elimination.EliminationRequestBody;
import fr.gouv.vitam.common.model.objectgroup.ObjectGroupResponse;
import fr.gouv.vitam.common.model.objectgroup.QualifiersModel;
import fr.gouv.vitam.common.model.objectgroup.VersionsModel;
import fr.gouv.vitam.common.model.processing.WorkFlow;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.stream.VitamAsyncInputStream;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.AccessionRegisterDetail;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClient;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClientFactory;
import fr.gouv.vitam.ingest.internal.upload.rest.IngestInternalMain;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.rest.MetadataMain;
import fr.gouv.vitam.processing.common.model.ProcessWorkflow;
import fr.gouv.vitam.processing.data.core.ProcessDataAccessImpl;
import fr.gouv.vitam.processing.engine.core.monitoring.ProcessMonitoringImpl;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.processing.management.rest.ProcessManagementMain;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.storage.offers.rest.DefaultOfferMain;
import fr.gouv.vitam.worker.core.distribution.JsonLineGenericIterator;
import fr.gouv.vitam.worker.core.plugin.elimination.model.EliminationActionUnitStatus;
import fr.gouv.vitam.worker.core.plugin.purge.PurgeObjectGroupStatus;
import fr.gouv.vitam.worker.core.plugin.purge.PurgeUnitStatus;
import fr.gouv.vitam.worker.server.rest.WorkerMain;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import io.restassured.RestAssured;
import net.javacrumbs.jsonunit.JsonAssert;
import net.javacrumbs.jsonunit.core.Option;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.SetUtils;
import org.assertj.core.util.Lists;
import org.bson.Document;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.guid.GUIDFactory.newOperationLogbookGUID;
import static io.restassured.RestAssured.get;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class EndToEndEliminationAndTransferReplyIT extends VitamRuleRunner {

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
    private static final String SAINT_DENIS_UNIVERSITE_LIGNE_13 = "1_Saint Denis Université (ligne 13)";
    private static final String SAINT_DENIS_BASILIQUE = "Saint Denis Basilique";
    private static final String CARREFOUR_PLEYEL = "Carrefour Pleyel";
    private static final String SAINT_LAZARE = "Saint-Lazare";
    private static final String MARX_DORMOY = "Marx Dormoy";
    private static final String MONTPARNASSE = "Montparnasse.txt";
    private static final String ORIGINATING_AGENCY = "RATP";
    private static final String JSONL = ".jsonl";
    private static final String JSON = ".json";
    private static final String XML = ".xml";
    @ClassRule
    public static VitamServerRunner runner =
        new VitamServerRunner(EndToEndEliminationAndTransferReplyIT.class, mongoRule.getMongoDatabase().getName(),
            ElasticsearchRule.getClusterName(),
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
    private static String CONFIG_SIEGFRIED_PATH = "";
    private static String TEST_ELIMINATION_SIP =
        "elimination/TEST_ELIMINATION.zip";
    private static String TEST_TRANSFER_SIP =
        "integration-processing/4_UNITS_2_GOTS.zip";
    private static String ELIMINATION_ACCESSION_REGISTER_DETAIL =
        "elimination/accession_regoister_detail.json";
    private static String ELIMINATION_ACCESSION_REGISTER_SUMMARY =
        "elimination/accession_regoister_summary.json";
    private WorkFlow workflow = WorkFlow.of(WORKFLOW_ID, WORKFLOW_IDENTIFIER, "INGEST");
    private TypeReference<JsonNode> JSON_NODE_TYPE_REFERENCE =
        new TypeReference<JsonNode>() {
        };
    private TypeReference<UnitReportEntry> UNIT_REPORT_TYPE_REFERENCE =
        new TypeReference<UnitReportEntry>() {
        };
    private TypeReference<ObjectGroupReportEntry> OG_REPORT_TYPE_REFERENCE =
        new TypeReference<ObjectGroupReportEntry>() {
        };

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        handleBeforeClass(0, 1);
        CONFIG_SIEGFRIED_PATH =
            PropertiesUtils.getResourcePath("integration-ingest-internal/format-identifiers.conf").toString();

        FormatIdentifierFactory.getInstance().changeConfigurationFile(CONFIG_SIEGFRIED_PATH);


        new DataLoader("integration-ingest-internal").prepareData();

    }

    @After
    public void afterTest() {
        VitamThreadUtils.getVitamSession().setContractId("aName");
        VitamThreadUtils.getVitamSession().setContextId("Context_IT");

        ProcessDataAccessImpl.getInstance().clearWorkflow();
        runAfterMongo(Sets.newHashSet(
            MetadataCollections.UNIT.getName(),
            MetadataCollections.OBJECTGROUP.getName(),
            FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getName(),
            FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getName(),
            LogbookCollections.OPERATION.getName(),
            LogbookCollections.LIFECYCLE_UNIT.getName(),
            LogbookCollections.LIFECYCLE_OBJECTGROUP.getName(),
            LogbookCollections.LIFECYCLE_OBJECTGROUP.getName(),
            LogbookCollections.LIFECYCLE_UNIT_IN_PROCESS.getName()

        ));

        runAfterEs(Sets.newHashSet(
            MetadataCollections.UNIT.getName().toLowerCase() + "_0",
            MetadataCollections.UNIT.getName().toLowerCase() + "_1",
            MetadataCollections.OBJECTGROUP.getName().toLowerCase() + "_0",
            MetadataCollections.OBJECTGROUP.getName().toLowerCase() + "_1",
            LogbookCollections.OPERATION.getName().toLowerCase() + "_0",
            LogbookCollections.OPERATION.getName().toLowerCase() + "_1",
            FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getName().toLowerCase(),
            FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getName().toLowerCase()
        ));
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        handleAfterClass(0, 1);
        runAfter();
        VitamClientFactory.resetConnections();
    }

    public static void prepareVitamSession() {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        VitamThreadUtils.getVitamSession().setContractId("aName3");
        VitamThreadUtils.getVitamSession().setContextId("Context_IT");
    }

    @Before
    public void setUpBefore() {
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(0));
    }

    private void wait(String operationId) {
        int nbTry = 0;
        ProcessingManagementClient processingClient =
            ProcessingManagementClientFactory.getInstance().getClient();
        while (!processingClient.isNotRunning(operationId)) {
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
        final String ingestOperationGuid = doIngest(PropertiesUtils.getResourceAsStream(TEST_ELIMINATION_SIP),
            StatusCode.OK);

        // Check ingested units
        final AccessInternalClient accessInternalClient = AccessInternalClientFactory.getInstance().getClient();
        final RequestResponseOK<JsonNode> ingestedUnits = selectUnitsByOpi(ingestOperationGuid, accessInternalClient);
        final RequestResponseOK<JsonNode> ingestedGots = selectGotsByOpi(ingestOperationGuid, accessInternalClient);

        assertThat(ingestedUnits.getResults()).hasSize(6);
        assertThat(ingestedGots.getResults()).hasSize(3);

        Set<String> ingestedObjectIds = getObjectIds(ingestedGots);
        assertThat(ingestedObjectIds).hasSize(3);

        // elimination action
        final String eliminationActionOperationGuid = newOperationLogbookGUID(tenantId).toString();
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
        assertJsonEquals(ELIMINATION_ACCESSION_REGISTER_DETAIL, JsonHandler.toJsonNode(
            Lists.newArrayList(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getCollection().find())),
            excludeFields);

        // Check Accession Register Summary
        assertJsonEquals(ELIMINATION_ACCESSION_REGISTER_SUMMARY,
            JsonHandler.toJsonNode(
                Lists.newArrayList(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getCollection().find())),
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
        try (InputStream reportInputStream = readStoredReport(eliminationActionOperationGuid + JSONL)) {

            checkEliminationReport(reportInputStream, ingestedUnits, ingestedUnitsByTitle, ingestOperationGuid,
                ingestedGots,
                detachedGotId);
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testTransferReply() throws Exception {
        final String ingestOperationGuid = doIngest(PropertiesUtils.getResourceAsStream(TEST_ELIMINATION_SIP),
            StatusCode.OK);

        // Check ingested units
        final AccessInternalClient accessInternalClient = AccessInternalClientFactory.getInstance().getClient();
        final RequestResponseOK<JsonNode> ingestedUnits = selectUnitsByOpi(ingestOperationGuid, accessInternalClient);
        final RequestResponseOK<JsonNode> ingestedObjectGroups =
            selectGotsByOpi(ingestOperationGuid, accessInternalClient);
        Set<String> ingestedUnitIds = getIds(ingestedUnits);
        Set<String> ingestedObjectGroupIds = getIds(ingestedObjectGroups);

        assertThat(ingestedUnits.getResults()).hasSize(6);
        assertThat(ingestedObjectGroups.getResults()).hasSize(3);

        Set<String> ingestedObjectIds = getObjectIds(ingestedObjectGroups);
        assertThat(ingestedObjectIds).hasSize(3);

        // Export transfer archive
        String transferOperation = transfer(ingestedUnitIds, StatusCode.OK);

        // Check "opts" field update
        checkTransferOperationsInExportedUnits(accessInternalClient, transferOperation, ingestedUnitIds);

        //  Check transfer export report
        checkTransferRequestReport(transferOperation, ingestedUnitIds, Collections.emptyList());

        // Get exported SIP from transfer request and ingest it
        String transferredSipIngestOperationId;
        try (InputStream sipInputStream = getTransferSip(transferOperation)) {
            transferredSipIngestOperationId = doIngest(sipInputStream, StatusCode.OK);
        }

        // Check ingested transferred metadata match initial exported ones
        RequestResponseOK<JsonNode> ingestedTransferredUnits =
            selectUnitsByOpi(transferredSipIngestOperationId, accessInternalClient);
        RequestResponseOK<JsonNode> ingestedTransferredObjectGroups =
            selectGotsByOpi(transferredSipIngestOperationId, accessInternalClient);

        compareMetadata(ingestOperationGuid, ingestedUnits, ingestedObjectGroups, transferredSipIngestOperationId,
            ingestedTransferredUnits, ingestedTransferredObjectGroups);

        // Send ATR back for transfer reply workflow
        String transferReplyOperationId;
        try (InputStream atrInputStream = readStoredReport(transferredSipIngestOperationId + XML)) {
            transferReplyOperationId = startTransferReplyWorkflow(atrInputStream, StatusCode.OK);
        }

        List<String> transferPurgedTransferUnitIds = new ArrayList<>(ingestedUnitIds);
        List<String> transferAlreadyDeletedUnitIds = Collections.emptyList();
        List<String> transferNonDeletableUnitIds = Collections.emptyList();
        List<String> transferDeletedObjectGroups = new ArrayList<>(ingestedObjectGroupIds);
        List<String> transferDetachedObjectGroups = Collections.emptyList();

        // Check transfer result
        checkTransferResult(ingestOperationGuid, ingestedUnits, ingestedObjectGroups, ingestedUnitIds,
            ingestedObjectGroupIds,
            transferReplyOperationId, transferPurgedTransferUnitIds, transferAlreadyDeletedUnitIds,
            transferNonDeletableUnitIds, transferDeletedObjectGroups, transferDetachedObjectGroups,
            accessInternalClient);

        // Check Accession Register Detail
        List<String> excludeFields = Lists
            .newArrayList("_id", "StartDate", "LastUpdate", "EndDate", "Opc", "Opi", "CreationDate", "OperationIds");
        assertJsonEquals("transfer/reply/accession_register_detail.json", JsonHandler.toJsonNode(
            Lists.newArrayList(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getCollection().find(
                new Document(AccessionRegisterDetail.OPI, ingestOperationGuid)
            ))),
            excludeFields);

        // Check Accession Register Summary
        assertJsonEquals("transfer/reply/accession_register_summary.json",
            JsonHandler.toJsonNode(
                Lists.newArrayList(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getCollection().find())),
            excludeFields);
    }

    @RunWithCustomExecutor
    @Test
    public void testTransferReplyComplex() throws Exception {
        final String ingestOperationGuid = doIngest(PropertiesUtils.getResourceAsStream(TEST_ELIMINATION_SIP),
            StatusCode.OK);

        // Check ingested units
        final AccessInternalClient accessInternalClient = AccessInternalClientFactory.getInstance().getClient();
        final RequestResponseOK<JsonNode> ingestedUnits = selectUnitsByOpi(ingestOperationGuid, accessInternalClient);
        final RequestResponseOK<JsonNode> ingestedObjectGroups =
            selectGotsByOpi(ingestOperationGuid, accessInternalClient);
        Set<String> ingestedUnitIds = getIds(ingestedUnits);
        Set<String> ingestedObjectGroupIds = getIds(ingestedObjectGroups);

        assertThat(ingestedUnits.getResults()).hasSize(6);
        assertThat(ingestedObjectGroups.getResults()).hasSize(3);

        Set<String> ingestedObjectIds = getObjectIds(ingestedObjectGroups);
        assertThat(ingestedObjectIds).hasSize(3);

        Map<String, JsonNode> ingestedUnitsByTitle = mapByField(ingestedUnits, "Title");

        // Transfer operation 1 : 1 single unit with 1 object group
        String transfer1UnitId = getId(ingestedUnitsByTitle.get(MONTPARNASSE));
        List<String> transfer1UnitIds = Collections.singletonList(transfer1UnitId);
        String transferOperation1 = transfer(transfer1UnitIds, StatusCode.OK);

        // Check "opts" field update
        checkTransferOperationsInExportedUnits(accessInternalClient, transferOperation1, transfer1UnitIds);

        //  Check transfer export report
        checkTransferRequestReport(transferOperation1, transfer1UnitIds, Collections.emptyList());

        // Export transfer archive ==> WARNING (contains 1 unit already being transferred)
        String transfer2UnitId1 = getId(ingestedUnitsByTitle.get(MONTPARNASSE));
        String transfer2UnitId2 = getId(ingestedUnitsByTitle.get(MARX_DORMOY));
        String transfer2UnitId3 = getId(ingestedUnitsByTitle.get(SAINT_LAZARE));
        String transfer2UnitId4 = getId(ingestedUnitsByTitle.get(SAINT_DENIS_BASILIQUE));
        List<String> transfer2UnitIds =
            Arrays.asList(transfer2UnitId1, transfer2UnitId2, transfer2UnitId3, transfer2UnitId4);
        String transferOperation2 = transfer(transfer2UnitIds, StatusCode.WARNING);

        // Check "opts" field update
        checkTransferOperationsInExportedUnits(accessInternalClient, transferOperation2, transfer2UnitIds);

        //  Check transfer export report
        checkTransferRequestReport(transferOperation2,
            Arrays.asList(transfer2UnitId2, transfer2UnitId3, transfer2UnitId4),
            Collections.singletonList(transfer2UnitId1));

        // Get exported SIP from transfer request 1 and ingest it
        String transferredSip1IngestOperationId;
        try (InputStream sipInputStream = getTransferSip(transferOperation1)) {
            transferredSip1IngestOperationId = doIngest(sipInputStream, StatusCode.OK);
        }

        // Send ATR back for transfer reply workflow 1 ==> Will delete 1 unit & detach 1 object group
        String transferReplyOperationId1;
        try (InputStream atrInputStream = readStoredReport(transferredSip1IngestOperationId + XML)) {
            transferReplyOperationId1 = startTransferReplyWorkflow(atrInputStream, StatusCode.OK);
        }

        List<String> transfer1PurgedTransferUnitIds = transfer1UnitIds;
        List<String> transfer1AlreadyDeletedUnitIds = Collections.emptyList();
        List<String> transfer1NonDeletableUnitIds = Collections.emptyList();
        List<String> transfer1DeletedObjectGroups = Collections.emptyList();
        List<String> transfer1DetachedObjectGroups =
            Collections.singletonList(getObjectGroupId(getById(ingestedUnits, transfer1UnitId)));

        // Check transfer 1 result
        checkTransferResult(ingestOperationGuid, ingestedUnits, ingestedObjectGroups, ingestedUnitIds,
            ingestedObjectGroupIds,
            transferReplyOperationId1, transfer1PurgedTransferUnitIds, transfer1AlreadyDeletedUnitIds,
            transfer1NonDeletableUnitIds, transfer1DeletedObjectGroups, transfer1DetachedObjectGroups,
            accessInternalClient);

        // Get exported SIP from transfer request 2 and ingest it
        String transferredSip2IngestOperationId;
        try (InputStream sipInputStream = getTransferSip(transferOperation2)) {
            transferredSip2IngestOperationId = doIngest(sipInputStream, StatusCode.OK);
        }

        // Send ATR back for transfer reply workflow 2 ==> Will delete 2/3 units & 1 object group
        String transferReplyOperationId2;
        try (InputStream atrInputStream = readStoredReport(transferredSip2IngestOperationId + XML)) {
            // 1 units could not be deleted ==> WARNING
            transferReplyOperationId2 = startTransferReplyWorkflow(atrInputStream, StatusCode.WARNING);
        }

        List<String> transfer2PurgedTransferUnitIds = Arrays.asList(transfer2UnitId2, transfer2UnitId3);
        List<String> transfer2AlreadyDeletedUnitIds = Collections.singletonList(transfer2UnitId1);
        List<String> transfer2NonDeletableUnitIds = Collections.singletonList(transfer2UnitId4);
        List<String> transfer2DeletedObjectGroups =
            Collections.singletonList(getObjectGroupId(getById(ingestedUnits, transfer2UnitId2)));
        List<String> transfer2DetachedObjectGroups = Collections.emptyList();

        // Check transfer 2 result
        checkTransferResult(ingestOperationGuid, ingestedUnits, ingestedObjectGroups, ingestedUnitIds,
            ingestedObjectGroupIds,
            transferReplyOperationId2, transfer2PurgedTransferUnitIds, transfer2AlreadyDeletedUnitIds,
            transfer2NonDeletableUnitIds, transfer2DeletedObjectGroups, transfer2DetachedObjectGroups,
            accessInternalClient);

        // Check Accession Register Detail

        System.out.println( JsonHandler.unprettyPrint(Lists.newArrayList(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getCollection().find())));

        List<String> excludeFields = Lists
            .newArrayList("_id", "StartDate", "LastUpdate", "EndDate", "Opc", "Opi", "CreationDate", "OperationIds");
        assertJsonEquals("transfer/reply/accession_register_detail_complex_test.json", JsonHandler.toJsonNode(
            Lists.newArrayList(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getCollection().find(
                new Document(AccessionRegisterDetail.OPI, ingestOperationGuid)
            ))),
            excludeFields);
        // Check Accession Register Summary
        assertJsonEquals("transfer/reply/accession_register_summary_complex_test.json",
            JsonHandler.toJsonNode(
                Lists.newArrayList(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getCollection().find())),
            excludeFields);
    }

    private void checkTransferResult(String ingestOperationGuid, RequestResponseOK<JsonNode> ingestedUnits,
        RequestResponseOK<JsonNode> ingestedObjectGroups, Set<String> ingestedUnitIds,
        Set<String> ingestedObjectGroupIds, String transferReplyOperationId,
        List<String> expectedDeletedUnitIds, List<String> expectedAlreadyDeletedUnitIds,
        List<String> expectedNonDeletableUnitIds, List<String> expectedDeletedObjectGroups,
        List<String> expectedDetachedObjectGroups,
        AccessInternalClient accessInternalClient)
        throws StorageNotFoundClientException, StorageServerClientException, IOException, StorageNotFoundException,
        BadRequestException, InvalidParseOperationException, AccessUnauthorizedException,
        AccessInternalClientServerException, AccessInternalClientNotFoundException, InvalidCreateOperationException {

        // Check remaining units / object groups
        final RequestResponseOK<JsonNode> remainingUnits = selectUnitsByOpi(ingestOperationGuid, accessInternalClient);
        final RequestResponseOK<JsonNode> remainingObjectGroups =
            selectGotsByOpi(ingestOperationGuid, accessInternalClient);

        assertThat(getIds(remainingUnits)).containsExactlyInAnyOrderElementsOf(
            ListUtils.removeAll(getIds(ingestedUnits), ListUtils.union(expectedAlreadyDeletedUnitIds, expectedDeletedUnitIds)));
        assertThat(getIds(remainingObjectGroups)).containsExactlyInAnyOrderElementsOf(
            ListUtils.removeAll(getIds(ingestedObjectGroups), expectedDeletedObjectGroups)
        );

        // Low level check of units / object groups & objects existence

        for (String id : ingestedUnitIds) {
            boolean shouldBePurged = expectedDeletedUnitIds.contains(id)
                || expectedAlreadyDeletedUnitIds.contains(id);
            checkUnitExistence(id, !shouldBePurged);
        }

        for (String id : ingestedObjectGroupIds) {
            boolean shouldBePurged = expectedDeletedObjectGroups.contains(id);
            checkObjectGroupExistence(id, !shouldBePurged);
            for (String objectId : getObjectIds(getById(ingestedObjectGroups, id))) {
                checkObjectExistence(objectId, !shouldBePurged);
            }
        }

        // Check transfer reply report
        checkTransferReplyReport(transferReplyOperationId, ingestOperationGuid,
            expectedDeletedUnitIds, expectedAlreadyDeletedUnitIds, expectedNonDeletableUnitIds,
            expectedDeletedObjectGroups, expectedDetachedObjectGroups,
            ingestedUnits, ingestedObjectGroups);
    }

    private void checkTransferReplyReport(String transferReplyOperationId, String ingestOperationGuid,
        List<String> expectedDeletedUnitIds, List<String> expectedAlreadyDeletedUnitIds,
        List<String> expectedNonDeletableUnitIds, List<String> expectedDeletedObjectGroupIds,
        List<String> expectedPartiallyDeletedObjectGroupIds, RequestResponseOK<JsonNode> ingestedUnits,
        RequestResponseOK<JsonNode> ingestedObjectGroups)

        throws IOException, StorageNotFoundException, StorageServerClientException {

        try (InputStream reportInputStream = readStoredReport(transferReplyOperationId + JSONL);
            JsonLineGenericIterator<JsonNode> reportIterator = new JsonLineGenericIterator<>(reportInputStream,
                JSON_NODE_TYPE_REFERENCE)) {

            // Skip context headers
            // FIXME : Check headers
            reportIterator.skip();
            reportIterator.skip();
            reportIterator.skip();

            //Check report units
            int unitCount = expectedDeletedUnitIds.size() + expectedAlreadyDeletedUnitIds.size() +
                expectedNonDeletableUnitIds.size();

            Map<String, UnitReportEntry> unitReportById = Streams.stream(
                Iterators.transform(
                    Iterators.limit(reportIterator, unitCount),
                    entry -> asTypeReference(entry, UNIT_REPORT_TYPE_REFERENCE))
            ).collect(toMap(entry -> entry.id, entry -> entry));

            assertThat(unitReportById.values()).allMatch(i -> "Unit".equals(i.params.type));
            assertThat(unitReportById.keySet()).containsExactlyInAnyOrderElementsOf(
                ListUtils.union(expectedDeletedUnitIds,
                    ListUtils.union(expectedAlreadyDeletedUnitIds, expectedNonDeletableUnitIds)));
            for (Map.Entry<String, UnitReportEntry> entry : unitReportById.entrySet()) {
                assertThat(entry.getValue().params.id).isEqualTo(entry.getKey());

                String expectedStatus = expectedAlreadyDeletedUnitIds.contains(entry.getKey()) ? "ALREADY_DELETED" :
                    expectedDeletedUnitIds.contains(entry.getKey()) ? "DELETED" : "NON_DESTROYABLE_HAS_CHILD_UNITS";
                assertThat(entry.getValue().params.status).isEqualTo(expectedStatus);

                if(!expectedAlreadyDeletedUnitIds.contains(entry.getKey())) {
                    assertThat(entry.getValue().params.opi).isEqualTo(ingestOperationGuid);
                    assertThat(entry.getValue().params.originatingAgency).isEqualTo(ORIGINATING_AGENCY);
                    assertThat(entry.getValue().params.objectGroupId).isEqualTo(
                        getById(ingestedUnits, entry.getKey()).has("#object") ?
                            getById(ingestedUnits, entry.getKey()).get("#object").asText() : null
                    );
                }
            }

            // Check report object groups
            int objectGroupCount = expectedDeletedObjectGroupIds.size() + expectedPartiallyDeletedObjectGroupIds.size();
            Map<String, ObjectGroupReportEntry> objectGroupReportById = Streams.stream(
                Iterators.transform(
                    Iterators.limit(reportIterator, objectGroupCount),
                    entry -> asTypeReference(entry, OG_REPORT_TYPE_REFERENCE))
            ).collect(toMap(entry -> entry.id, entry -> entry));

            assertThat(objectGroupReportById.values()).allMatch(i -> "ObjectGroup".equals(i.params.type));
            assertThat(objectGroupReportById.keySet()).containsExactlyInAnyOrderElementsOf(
                ListUtils.union(expectedDeletedObjectGroupIds, expectedPartiallyDeletedObjectGroupIds));

            for (Map.Entry<String, ObjectGroupReportEntry> entry : objectGroupReportById.entrySet()) {
                assertThat(entry.getValue().params.id).isEqualTo(entry.getKey());

                if (expectedDeletedObjectGroupIds.contains(entry.getKey())) {
                    assertThat(entry.getValue().params.status).isEqualTo("DELETED");
                    assertThat(entry.getValue().params.objectIds).containsExactlyInAnyOrderElementsOf(
                        getObjectIds(getById(ingestedObjectGroups, entry.getKey()))
                    );
                    assertThat(entry.getValue().params.deletedParentUnitIds).isNullOrEmpty();
                } else {
                    assertThat(entry.getValue().params.status).isEqualTo("PARTIAL_DETACHMENT");
                    assertThat(entry.getValue().params.objectIds).isNullOrEmpty();
                    assertThat(entry.getValue().params.deletedParentUnitIds).containsExactlyInAnyOrderElementsOf(
                        SetUtils.intersection(
                            getParentUnitIds(getById(ingestedObjectGroups, entry.getKey())),
                            new HashSet<>(expectedDeletedUnitIds)));
                }

                assertThat(entry.getValue().params.opi).isEqualTo(ingestOperationGuid);
                assertThat(entry.getValue().params.originatingAgency).isEqualTo(ORIGINATING_AGENCY);
            }

            // Check end of report
            assertThat(reportIterator.hasNext()).isFalse();
        }
    }

    private String startTransferReplyWorkflow(InputStream atrInputStream, StatusCode expectedStatusCode) throws AccessInternalClientServerException {
        String transferReplyWorkflowGuid = GUIDFactory.newOperationLogbookGUID(tenantId).getId();
        VitamThreadUtils.getVitamSession().setRequestId(transferReplyWorkflowGuid);
        try (AccessInternalClient client = AccessInternalClientFactory.getInstance().getClient()) {
            client.startTransferReplyWorkflow(atrInputStream);
            awaitForWorkflowTerminationWithStatus(transferReplyWorkflowGuid, expectedStatusCode);
        }
        return transferReplyWorkflowGuid;
    }

    private void checkTransferOperationsInExportedUnits(AccessInternalClient accessInternalClient,
        String transferOperation, Collection<String> expectedUnitIds)
        throws InvalidCreateOperationException, InvalidParseOperationException, AccessInternalClientServerException,
        AccessInternalClientNotFoundException, AccessUnauthorizedException, BadRequestException,
        LogbookClientException {
        SelectMultiQuery selectUnitsWithTransferOperations = new SelectMultiQuery();
        selectUnitsWithTransferOperations.addQueries(
            QueryHelper.eq(VitamFieldsHelper.opts(), transferOperation));
        RequestResponseOK<JsonNode> response =
            (RequestResponseOK<JsonNode>) accessInternalClient
                .selectUnits(selectUnitsWithTransferOperations.getFinalSelect());
        assertThat(response.getResults().stream()
            .map(this::getId)
            .collect(Collectors.toList())
        ).containsExactlyInAnyOrderElementsOf(expectedUnitIds);

        // Ensure exported units LFC not modified (opts is a non persisted / secured field)
        LogbookLifeCyclesClient logbookLifeCyclesClient = LogbookLifeCyclesClientFactory.getInstance().getClient();
        List<JsonNode> unitLifeCycles =
            logbookLifeCyclesClient.getRawUnitLifeCycleByIds(new ArrayList<>(expectedUnitIds));
        for (JsonNode unitLifeCycle : unitLifeCycles) {
            assertThat(unitLifeCycle.get("events").get(unitLifeCycle.get("events").size() - 1)
                .get("outDetail").asText()).isEqualTo("LFC.UNIT_METADATA_INDEXATION.OK");
        }
    }

    private void checkTransferRequestReport(String transferOperationId, Collection<String> okUnitIds,
        Collection<String> alredyTransferedUnitIds)
        throws IOException, StorageNotFoundException, StorageServerClientException {

        try (InputStream is = readStoredReport(transferOperationId + JSON);
            JsonLineGenericIterator<JsonNode> reportIterator =
                new JsonLineGenericIterator<>(is, JSON_NODE_TYPE_REFERENCE)) {

            // Skip context headers
            // FIXME : Check headers
            reportIterator.skip();
            reportIterator.skip();

            //Check report units
            Map<String, String> exportedUnitReportStatus =
                Streams.stream(reportIterator)
                    .collect(toMap(
                        entry -> entry.get("id").asText(),
                        entry -> entry.get("status").asText()
                    ));

            assertThat(exportedUnitReportStatus).hasSize(okUnitIds.size() + alredyTransferedUnitIds.size());
            assertThat(exportedUnitReportStatus.entrySet())
                .allMatch(entry -> entry.getValue()
                    .equals(okUnitIds.contains(entry.getKey()) ? "OK" : "ALREADY_IN_TRANSFER"));
        }
    }

    private void compareMetadata(String ingestOperationGuid, RequestResponseOK<JsonNode> ingestedUnits,
        RequestResponseOK<JsonNode> ingestedGots, String transferredSipIngestOperationId,
        RequestResponseOK<JsonNode> ingestedTransferredUnits,
        RequestResponseOK<JsonNode> ingestedTransferredObjectGroups) throws InvalidParseOperationException {
        Map<String, String> initialIngestReplacements = getReplacementsForNormalization(ingestOperationGuid,
            ingestedUnits.getResults(), ingestedGots.getResults());
        List<JsonNode> initialUnits = normalize(ingestedUnits.getResults(), initialIngestReplacements);
        List<JsonNode> initialObjectGroups = normalize(ingestedGots.getResults(), initialIngestReplacements);

        Map<String, String> transferredIngestReplacements =
            getReplacementsForNormalization(transferredSipIngestOperationId,
                ingestedTransferredUnits.getResults(), ingestedTransferredObjectGroups.getResults());
        List<JsonNode> transferredUnits =
            normalize(ingestedTransferredUnits.getResults(), transferredIngestReplacements);
        List<JsonNode> transferredObjectGroups =
            normalize(ingestedTransferredObjectGroups.getResults(), transferredIngestReplacements);

        assertThat(initialUnits).hasSameSizeAs(transferredUnits);
        for (int i = 0; i < initialUnits.size(); i++) {
            JsonAssert.assertJsonEquals(initialUnits.get(i), transferredUnits.get(i),
                JsonAssert.when(Option.IGNORING_ARRAY_ORDER));
        }

        assertThat(initialObjectGroups).hasSameSizeAs(transferredObjectGroups);
        for (int i = 0; i < initialObjectGroups.size(); i++) {

            JsonAssert.assertJsonEquals(initialObjectGroups.get(i), transferredObjectGroups.get(i),
                JsonAssert.when(Option.IGNORING_ARRAY_ORDER));
        }
    }

    private Map<String, String> getReplacementsForNormalization(String opi, List<JsonNode> units,
        List<JsonNode> objectGroups)
        throws InvalidParseOperationException {
        Map<String, String> replacements = new HashMap<>();
        replacements.put(opi, "{ingest_operation}");
        for (JsonNode unit : units) {
            replacements.put(getId(unit), "{unit:" + getTitle(unit) + "}");
        }
        for (JsonNode objectGroup : objectGroups) {
            String gotLabel =
                "{og_of:" +
                    JsonHandler.getFromJsonNode(objectGroup.get("#unitups"), new TypeReference<List<String>>() {
                    }).stream()
                        .map(replacements::get)
                        .sorted()
                        .collect(Collectors.joining(",", "{og_of:", "}"));
            replacements.put(getId(objectGroup), gotLabel);

            objectGroup.get("#qualifiers").elements().forEachRemaining(
                qualifier -> qualifier.get("versions").elements().forEachRemaining(
                    version -> replacements.put(version.get("#id").asText(),
                        "{binary:" + version.get("DataObjectVersion").asText() + "_of_" + gotLabel + "}")
                )
            );
        }
        return replacements;
    }

    private List<JsonNode> normalize(List<JsonNode> resultSet, Map<String, String> replacements)
        throws InvalidParseOperationException {

        List<JsonNode> normalizedMetadata = resultSet.stream()
            .map(metadata -> (JsonNode) metadata.deepCopy())
            .collect(Collectors.toList());

        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            replaceIds(normalizedMetadata, entry.getKey(), entry.getValue());
        }

        List<String> excludedFields = Arrays.asList("#version", "_graph", "_uds", "_glpd", "#allunitups", "_us_sp",
            "Uri", "DateCreatedByApplication");
        for (JsonNode md : normalizedMetadata) {
            purgeIgnoredFields(md, excludedFields);
        }

        return normalizedMetadata.stream()
            .sorted(Comparator.comparing(this::getId))
            .collect(Collectors.toList());
    }

    private void replaceIds(List<JsonNode> metadataList, String id, String replacement)
        throws InvalidParseOperationException {
        for (int i = 0; i < metadataList.size(); i++) {
            metadataList.set(i, JsonHandler.getFromString(
                JsonHandler.unprettyPrint(metadataList.get(i))
                    .replaceAll(id, replacement))
            );
        }
    }

    private void purgeIgnoredFields(JsonNode jsonNode, List<String> ignoredFields) {
        if (jsonNode.isValueNode()) {
            return;
        }
        if (jsonNode.isArray()) {
            for (int i = 0; i < jsonNode.size(); i++) {
                purgeIgnoredFields(jsonNode.get(i), ignoredFields);
            }
            return;
        }
        if (jsonNode.isObject()) {
            ((ObjectNode) jsonNode).remove(ignoredFields);
            for (JsonNode node : jsonNode) {
                purgeIgnoredFields(node, ignoredFields);
            }
            return;
        }
        throw new IllegalStateException("Unknown type " + jsonNode);
    }

    private InputStream readStoredReport(String filename)
        throws StorageServerClientException, StorageNotFoundException {
        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {

            Response reportResponse = null;

            try {
                reportResponse = storageClient.getContainerAsync(VitamConfiguration.getDefaultStrategy(),
                    filename, DataCategory.REPORT,
                    AccessLogUtils.getNoLogAccessLog());

                assertThat(reportResponse.getStatus()).isEqualTo(Status.OK.getStatusCode());

                return new VitamAsyncInputStream(reportResponse);


            } catch (RuntimeException | StorageServerClientException | StorageNotFoundException e) {
                StreamUtils.consumeAnyEntityAndClose(reportResponse);
                throw e;
            }
        }
    }

    private String doIngest(InputStream zipInputStreamSipObject, StatusCode expectedStatusCode) throws VitamException {
        final GUID ingestOperationGuid = newOperationLogbookGUID(tenantId);
        prepareVitamSession();
        VitamThreadUtils.getVitamSession().setRequestId(ingestOperationGuid);
        // workspace client unzip SIP in workspace

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

        awaitForWorkflowTerminationWithStatus(ingestOperationGuid.getId(), expectedStatusCode);
        return ingestOperationGuid.getId();
    }

    private String transfer(Collection<String> unitIds, StatusCode expectedStatusCode) throws Exception {

        SelectMultiQuery select = new SelectMultiQuery();
        select.setQuery(QueryHelper.in(VitamFieldsHelper.id(), unitIds.toArray(new String[0])));

        DipExportRequest dipExportRequest = new DipExportRequest(
            new DataObjectVersions(),
            select.getFinalSelect(),
            true
        );

        ExportRequestParameters exportRequestParameters = new ExportRequestParameters();
        exportRequestParameters.setArchivalAgencyIdentifier("Identifier4");
        exportRequestParameters.setRequesterIdentifier("Required RequesterIdentifier");
        exportRequestParameters.setArchivalAgreement("ArchivalAgreement0");
        exportRequestParameters.setOriginatingAgencyIdentifier("RATP");
        exportRequestParameters.setSubmissionAgencyIdentifier("RATP");
        exportRequestParameters.setRelatedTransferReference(
            com.google.common.collect.Lists.newArrayList("RelatedTransferReference1", "RelatedTransferReference2"));

        dipExportRequest.setExportType(ExportType.ArchiveTransfer);
        dipExportRequest.setExportRequestParameters(exportRequestParameters);


        String transferOperation = GUIDFactory.newGUID().getId();
        prepareVitamSession();
        VitamThreadUtils.getVitamSession().setRequestId(transferOperation);

        try (AccessInternalClient client = AccessInternalClientFactory.getInstance().getClient()) {
            client.exportByUsageFilter(dipExportRequest);

            awaitForWorkflowTerminationWithStatus(transferOperation, expectedStatusCode);

            return transferOperation;
        }
    }

    private InputStream getTransferSip(String operationId)
        throws Exception {
        try (AccessInternalClient client = AccessInternalClientFactory.getInstance().getClient()) {
            return client.findExportByID(operationId).readEntity(InputStream.class);
        }
    }

    private void checkEliminationReport(InputStream reportInputStream, RequestResponseOK<JsonNode> ingestedUnits,
        Map<String, JsonNode> ingestedUnitsByTitle,
        String ingestOperationGuid, RequestResponseOK<JsonNode> ingestedGots, String detachedGotId) {

        try (JsonLineGenericIterator<JsonNode> reportIterator = new JsonLineGenericIterator<>(reportInputStream,
            JSON_NODE_TYPE_REFERENCE)) {

            // Skip context headers
            // FIXME : Check headers
            reportIterator.skip();
            reportIterator.skip();
            reportIterator.skip();

            //Check report units
            Map<String, UnitReportEntry> unitReportByTitle = Streams.stream(
                Iterators.transform(
                    Iterators.limit(reportIterator, 6),
                    entry -> asTypeReference(entry, UNIT_REPORT_TYPE_REFERENCE))
            ).collect(toMap(entry -> getTitle(getById(ingestedUnits, entry.id)), entry -> entry));

            assertThat(unitReportByTitle).hasSize(6);

            assertThat(unitReportByTitle.get(CARREFOUR_PLEYEL).params.status).isEqualTo(
                EliminationActionUnitStatus.GLOBAL_STATUS_KEEP.name());
            assertThat(unitReportByTitle.get(SAINT_DENIS_BASILIQUE).params.status).isEqualTo(
                PurgeUnitStatus.NON_DESTROYABLE_HAS_CHILD_UNITS.name());
            assertThat(unitReportByTitle.get(SAINT_DENIS_UNIVERSITE_LIGNE_13).params.status).isEqualTo(
                PurgeUnitStatus.NON_DESTROYABLE_HAS_CHILD_UNITS.name());
            assertThat(unitReportByTitle.get(SAINT_LAZARE).params.status).isEqualTo(
                PurgeUnitStatus.DELETED.name());
            assertThat(unitReportByTitle.get(MARX_DORMOY).params.status).isEqualTo(
                PurgeUnitStatus.DELETED.name());
            assertThat(unitReportByTitle.get(MONTPARNASSE).params.status).isEqualTo(
                PurgeUnitStatus.DELETED.name());

            for (UnitReportEntry unitReport : unitReportByTitle.values()) {

                JsonNode unit = getById(ingestedUnits, unitReport.id);

                assertThat(unitReport.params.opi).isEqualTo(ingestOperationGuid.toString());
                assertThat(unitReport.params.originatingAgency).isEqualTo(ORIGINATING_AGENCY);
                assertThat(unitReport.params.objectGroupId).isEqualTo(getObjectGroupId(unit));
                assertThat(unitReport.params.type).isEqualTo("Unit");
            }

            //Check report object groups
            List<ObjectGroupReportEntry> objectGroupReports = Streams.stream(
                Iterators.transform(
                    Iterators.limit(reportIterator, 2),
                    entry -> asTypeReference(entry, OG_REPORT_TYPE_REFERENCE))
            ).collect(Collectors.toList());

            assertThat(objectGroupReports).hasSize(2);
            assertThat(objectGroupReports).allMatch(i -> "ObjectGroup".equals(i.params.type));

            String deletedGotId = getObjectGroupId(ingestedUnitsByTitle.get(MARX_DORMOY));
            ObjectGroupReportEntry deletedGotReport = objectGroupReports.stream()
                .filter(got -> got.id.equals(deletedGotId))
                .findFirst().get();
            assertThat(deletedGotReport.params.status).isEqualTo(PurgeObjectGroupStatus.DELETED.name());
            assertThat(deletedGotReport.params.opi).isEqualTo(ingestOperationGuid);
            assertThat(deletedGotReport.params.originatingAgency).isEqualTo(ORIGINATING_AGENCY);
            assertThat(deletedGotReport.params.objectIds).containsExactlyInAnyOrderElementsOf(
                getObjectIds(getById(ingestedGots, deletedGotId)));
            assertThat(deletedGotReport.params.deletedParentUnitIds).isNullOrEmpty();

            ObjectGroupReportEntry detachedGotReport = objectGroupReports.stream()
                .filter(got -> got.id.equals(detachedGotId))
                .findFirst().get();
            assertThat(detachedGotReport.params.status)
                .isEqualTo(PurgeObjectGroupStatus.PARTIAL_DETACHMENT.name());
            assertThat(detachedGotReport.params.opi).isEqualTo(ingestOperationGuid);
            assertThat(detachedGotReport.params.originatingAgency).isEqualTo(ORIGINATING_AGENCY);
            assertThat(detachedGotReport.params.objectIds).isNullOrEmpty();
            assertThat(detachedGotReport.params.deletedParentUnitIds).containsExactlyInAnyOrder(
                getId(ingestedUnitsByTitle.get(MONTPARNASSE)));

            // Check end of report
            assertThat(reportIterator.hasNext()).isFalse();
        }
    }

    private <T> T asTypeReference(JsonNode entry, TypeReference<T> typeReference) {
        try {
            return JsonHandler.getFromJsonNode(entry, typeReference);
        } catch (InvalidParseOperationException e) {
            throw new RuntimeException(e);
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
            .assertJsonEquals(expected, actual, JsonAssert.whenIgnoringPaths(excludeFields.toArray(new String[] {})));
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

    private JsonNode getById(RequestResponseOK<JsonNode> metadata, String id) {
        return metadata.getResults().stream()
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

            List<String> offers = storageClient.getOffers(VitamConfiguration.getDefaultStrategy());
            JsonNode information =
                storageClient
                    .getInformation(VitamConfiguration.getDefaultStrategy(), dataCategory, filename, offers, false);
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

    private Set<String> getParentUnitIds(JsonNode gotJson) {

        try {
            ObjectGroupResponse gotResponse = JsonHandler.getFromJsonNode(gotJson, ObjectGroupResponse.class);
            return new HashSet<>(gotResponse.getUp());

        } catch (InvalidParseOperationException e) {
            throw new RuntimeException(e);
        }
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
            .collect(toMap(node -> node.get(title).asText(), node -> node));
    }

    private RequestResponseOK<JsonNode> selectGotsByOpi(String ingestOperationGuid,
        AccessInternalClient accessInternalClient)
        throws InvalidCreateOperationException, InvalidParseOperationException, AccessInternalClientServerException,
        AccessInternalClientNotFoundException, AccessUnauthorizedException, BadRequestException {
        SelectMultiQuery checkEliminationGotDslRequest = new SelectMultiQuery();
        checkEliminationGotDslRequest.addQueries(
            QueryHelper.eq(VitamFieldsHelper.initialOperation(), ingestOperationGuid));

        return (RequestResponseOK<JsonNode>) accessInternalClient
            .selectObjects(checkEliminationGotDslRequest.getFinalSelect());
    }

    private RequestResponseOK<JsonNode> selectUnitsByOpi(String ingestOperationGuid,
        AccessInternalClient accessInternalClient)
        throws InvalidCreateOperationException, InvalidParseOperationException, AccessInternalClientServerException,
        AccessInternalClientNotFoundException, AccessUnauthorizedException, BadRequestException {
        SelectMultiQuery checkEliminationDslRequest = new SelectMultiQuery();
        checkEliminationDslRequest.addQueries(
            QueryHelper.eq(VitamFieldsHelper.initialOperation(), ingestOperationGuid));

        return (RequestResponseOK<JsonNode>) accessInternalClient
            .selectUnits(checkEliminationDslRequest.getFinalSelect());
    }

    private void awaitForWorkflowTerminationWithStatus(String operationGuid, StatusCode expectedStatusCode) {

        wait(operationGuid);

        ProcessWorkflow processWorkflow =
            ProcessMonitoringImpl.getInstance().findOneProcessWorkflow(operationGuid, tenantId);

        assertNotNull(processWorkflow);
        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
        assertEquals(expectedStatusCode, processWorkflow.getStatus());
    }

    private static class UnitReportEntry {
        @JsonProperty("id")
        String id;
        @JsonProperty("params")
        UnitReportParams params;
    }


    private static class UnitReportParams {
        @JsonProperty("id")
        String id;
        @JsonProperty("status")
        String status;
        @JsonProperty("opi")
        String opi;
        @JsonProperty("originatingAgency")
        String originatingAgency;
        @JsonProperty("objectGroupId")
        String objectGroupId;
        @JsonProperty("type")
        String type;
    }


    private static class ObjectGroupReportEntry {
        @JsonProperty("id")
        String id;
        @JsonProperty("params")
        ObjectGroupReportParams params;
    }


    private static class ObjectGroupReportParams {
        @JsonProperty("id")
        String id;
        @JsonProperty("status")
        String status;
        @JsonProperty("opi")
        String opi;
        @JsonProperty("originatingAgency")
        String originatingAgency;
        @JsonProperty("objectIds")
        List<String> objectIds;
        @JsonProperty("deletedParentUnitIds")
        List<String> deletedParentUnitIds;
        @JsonProperty("type")
        String type;
    }
}
