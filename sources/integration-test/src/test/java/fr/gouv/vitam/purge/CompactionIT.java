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
package fr.gouv.vitam.purge;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import com.mongodb.client.model.Sorts;
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
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.format.identification.FormatIdentifierFactory;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.json.BsonHelper;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.processing.WorkFlow;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.stream.VitamAsyncInputStream;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.common.time.LogicalClockRule;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClient;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClientFactory;
import fr.gouv.vitam.ingest.internal.upload.rest.IngestInternalMain;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
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
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.model.CompactedOfferLog;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.OfferLogAction;
import fr.gouv.vitam.storage.engine.common.model.Order;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.storage.offers.rest.DefaultOfferMain;
import fr.gouv.vitam.worker.server.rest.WorkerMain;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import okhttp3.OkHttpClient;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.NullInputStream;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.http.Headers;
import retrofit2.http.POST;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.mongodb.client.model.Filters.eq;
import static fr.gouv.vitam.common.guid.GUIDFactory.newOperationLogbookGUID;
import static fr.gouv.vitam.storage.engine.common.collection.OfferCollections.COMPACTED_OFFER_LOG;
import static fr.gouv.vitam.storage.engine.common.collection.OfferCollections.OFFER_LOG;
import static fr.gouv.vitam.storage.engine.common.collection.OfferCollections.OFFER_SEQUENCE;
import static fr.gouv.vitam.storage.engine.common.model.CompactedOfferLog.CONTAINER;
import static fr.gouv.vitam.storage.engine.common.model.DataCategory.OBJECT;
import static fr.gouv.vitam.storage.engine.common.model.DataCategory.UNIT;
import static fr.gouv.vitam.storage.engine.common.model.Order.ASC;
import static fr.gouv.vitam.storage.engine.common.model.Order.DESC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CompactionIT extends VitamRuleRunner {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CompactionIT.class);

    private static final Integer tenantId = 0;

    private static final String STRATEGY = "default";
    private static final String OFFER_ID = "default";

    private static final long SLEEP_TIME = 20L;
    private static final long NB_TRY = 18000;

    private static final String WORKFLOW_ID = "DEFAULT_WORKFLOW";
    private static final String WORKFLOW_IDENTIFIER = "PROCESS_SIP_UNITARY";

    private static final String XML = ".xml";

    private static final String OFFER_URL = "http://localhost:" + VitamServerRunner.PORT_SERVICE_OFFER_ADMIN;

    @ClassRule
    public static VitamServerRunner runner = new VitamServerRunner(
        CompactionIT.class,
        mongoRule.getMongoDatabase().getName(),
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

    private static CompactionRestService compactionRestService;

    @Rule
    public LogicalClockRule logicalClock = new LogicalClockRule();

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private WorkFlow workflow = WorkFlow.of(WORKFLOW_ID, WORKFLOW_IDENTIFIER, "INGEST");

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        handleBeforeClass(0, 1);

        FormatIdentifierFactory.getInstance()
            .changeConfigurationFile(
                PropertiesUtils.getResourcePath("integration-ingest-internal/format-identifiers.conf").toString());

        new DataLoader("integration-ingest-internal").prepareData();

        final OkHttpClient okHttpClient = new OkHttpClient.Builder().build();

        Retrofit retrofit =
            new Retrofit.Builder().client(okHttpClient).baseUrl(OFFER_URL)
                .addConverterFactory(JacksonConverterFactory.create()).build();

        compactionRestService = retrofit.create(CompactionRestService.class);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        handleAfterClass(0, 1);
        runAfter();
        VitamClientFactory.resetConnections();
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


        clearOfferLogs();
    }

    @Before
    public void setUpBefore() {
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(0));
        clearOfferLogs();
    }

    private void clearOfferLogs() {
        runAfterMongo(Sets.newHashSet(
            OFFER_LOG.getName(),
            COMPACTED_OFFER_LOG.getName(),
            OFFER_SEQUENCE.getName()
        ));
    }

    @RunWithCustomExecutor
    @Test
    public void should_compact_offer_log() throws Exception {
        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {

            doIngest(PropertiesUtils.getResourceAsStream("compaction/sip_compaction.zip"), StatusCode.OK);


            List<OfferLog> offerLogsDescResponseBeforeCompaction =
                getOfferLogsFromStorage(storageClient, UNIT, null, 6, DESC);
            List<OfferLog> offerLogsAscResponseBeforeCompaction =
                getOfferLogsFromStorage(storageClient, UNIT, null, 6, ASC);

            logicalClock.logicalSleep(22, ChronoUnit.DAYS);

            compactOfferLogs();

            List<CompactedOfferLog> offerLogsCompaction = getDbCompactedOfferLogs("0_unit");
            List<OfferLog> offerLogsFromCompaction = offerLogsCompaction.iterator().next().getLogs();
            List<OfferLog> offerLogs = getDbOfferLogs("0_unit");
            assertThat(offerLogsCompaction).as("check offer logs compaction collection is not empty").isNotEmpty();
            assertThat(offerLogs).as("check offer logs compaction collection is empty").isEmpty();

            List<OfferLog> offerLogsDesc =
                getOfferLogsFromStorage(storageClient, UNIT, null, 6, DESC);
            List<OfferLog> offerLogsAsc =
                getOfferLogsFromStorage(storageClient, UNIT, null, 6, ASC);

            assertThat(offerLogsDesc).as("check offer logs retrieve by descending query is not empty").isNotEmpty();
            assertThat(offerLogsAsc).as("check offer logs retrieve by ascending query is not empty").isNotEmpty();

            assertThat(offerLogsDesc).as("check offer logs retrieve by descending query size").hasSize(6);
            assertThat(offerLogsAsc).as("check offer logs retrieve by ascending query size").hasSize(6);

            assertThat(offerLogsFromCompaction).containsExactlyInAnyOrderElementsOf(offerLogsAsc);
            assertThat(offerLogsFromCompaction).containsExactlyInAnyOrderElementsOf(offerLogsDesc);

            assertThat(offerLogsDesc).containsExactlyElementsOf(
                offerLogsDescResponseBeforeCompaction);
            assertThat(offerLogsAsc).containsExactlyElementsOf(
                offerLogsAscResponseBeforeCompaction);
        }
    }

    @RunWithCustomExecutor
    @Test
    public void should_retrieve_complex_offer_log_from_OfferLog_and_CompactedOfferLog() throws Exception {

        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        VitamThreadUtils.getVitamSession().setContractId("aName3");
        VitamThreadUtils.getVitamSession().setContextId("Context_IT");

        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {

            // When
            for (int i = 1; i <= 20; i++) {
                storageClient.create(STRATEGY, "unit" + i,
                    UNIT, new NullInputStream(i), (long) i, Collections.singletonList(OFFER_ID));
            }

            logicalClock.logicalSleep(25, ChronoUnit.DAYS);

            compactOfferLogs();

            for (int i = 21; i <= 40; i++) {
                storageClient.create(STRATEGY, "unit" + i,
                    UNIT, new NullInputStream(i), (long) i, Collections.singletonList(OFFER_ID));
            }
            for (int i = 41; i <= 60; i++) {
                storageClient.create(STRATEGY, "object" + i,
                    OBJECT, new NullInputStream(i), (long) i, Collections.singletonList(OFFER_ID));
            }
            for (int i = 61; i <= 80; i++) {
                storageClient.create(STRATEGY, "unit" + i,
                    UNIT, new NullInputStream(i), (long) i, Collections.singletonList(OFFER_ID));
            }

            logicalClock.logicalSleep(15, ChronoUnit.DAYS);

            for (int i = 81; i <= 90; i++) {
                storageClient.delete(STRATEGY, UNIT, "unit" + i, Collections.singletonList(OFFER_ID));
            }
            for (int i = 91; i <= 100; i++) {
                storageClient.delete(STRATEGY, OBJECT, "object" + i, Collections.singletonList(OFFER_ID));
            }

            logicalClock.logicalSleep(10, ChronoUnit.DAYS);

            compactOfferLogs();

            // Then
            // Technical checks
            List<CompactedOfferLog> offerLogsCompaction = getDbCompactedOfferLogs();
            assertThat(offerLogsCompaction).hasSize(3);

            assertThat(offerLogsCompaction.get(0).getContainer()).isEqualTo("0_unit");
            assertThat(offerLogsCompaction.get(0).getSequenceStart()).isEqualTo(1L);
            assertThat(offerLogsCompaction.get(0).getSequenceEnd()).isEqualTo(20L);
            assertThat(offerLogsCompaction.get(0).getLogs()).hasSize(20);
            assertThat(offerLogsCompaction.get(0).getLogs().get(0).getFileName()).isEqualTo("unit1");
            assertThat(offerLogsCompaction.get(0).getLogs().get(0).getSequence()).isEqualTo(1L);
            assertThat(offerLogsCompaction.get(0).getLogs().get(0).getAction()).isEqualTo(OfferLogAction.WRITE);
            assertThat(offerLogsCompaction.get(0).getLogs().get(19).getFileName()).isEqualTo("unit20");
            assertThat(offerLogsCompaction.get(0).getLogs().get(19).getSequence()).isEqualTo(20L);
            assertThat(offerLogsCompaction.get(0).getLogs().get(19).getAction()).isEqualTo(OfferLogAction.WRITE);

            assertThat(offerLogsCompaction.get(1).getContainer()).isEqualTo("0_unit");
            assertThat(offerLogsCompaction.get(1).getSequenceStart()).isEqualTo(21L);
            assertThat(offerLogsCompaction.get(1).getSequenceEnd()).isEqualTo(80L);
            assertThat(offerLogsCompaction.get(1).getLogs()).hasSize(40);
            assertThat(offerLogsCompaction.get(1).getLogs().get(0).getFileName()).isEqualTo("unit21");
            assertThat(offerLogsCompaction.get(1).getLogs().get(0).getSequence()).isEqualTo(21L);
            assertThat(offerLogsCompaction.get(1).getLogs().get(0).getAction()).isEqualTo(OfferLogAction.WRITE);
            assertThat(offerLogsCompaction.get(1).getLogs().get(39).getFileName()).isEqualTo("unit80");
            assertThat(offerLogsCompaction.get(1).getLogs().get(39).getSequence()).isEqualTo(80L);
            assertThat(offerLogsCompaction.get(1).getLogs().get(39).getAction()).isEqualTo(OfferLogAction.WRITE);

            assertThat(offerLogsCompaction.get(2).getContainer()).isEqualTo("0_object");
            assertThat(offerLogsCompaction.get(2).getSequenceStart()).isEqualTo(41L);
            assertThat(offerLogsCompaction.get(2).getSequenceEnd()).isEqualTo(60L);
            assertThat(offerLogsCompaction.get(2).getLogs()).hasSize(20);
            assertThat(offerLogsCompaction.get(2).getLogs().get(4).getFileName()).isEqualTo("object45");
            assertThat(offerLogsCompaction.get(2).getLogs().get(4).getSequence()).isEqualTo(45L);
            assertThat(offerLogsCompaction.get(2).getLogs().get(4).getAction()).isEqualTo(OfferLogAction.WRITE);

            List<OfferLog> offerLogs = getDbOfferLogs();
            assertThat(offerLogs).hasSize(20);
            for (int i = 0; i < 10; i++) {
                OfferLog offerLog = offerLogs.get(i);
                assertThat(offerLog.getContainer()).isEqualTo("0_unit");
                assertThat(offerLog.getFileName()).isEqualTo("unit" + (i + 81));
                assertThat(offerLog.getSequence()).isEqualTo(i + 81);
                assertThat(offerLog.getAction()).isEqualTo(OfferLogAction.DELETE);
            }
            for (int i = 0; i < 10; i++) {
                OfferLog offerLog = offerLogs.get(i + 10);
                assertThat(offerLog.getContainer()).isEqualTo("0_object");
                assertThat(offerLog.getFileName()).isEqualTo("object" + (i + 91));
                assertThat(offerLog.getSequence()).isEqualTo(i + 91);
                assertThat(offerLog.getAction()).isEqualTo(OfferLogAction.DELETE);
            }

            // Functional checks
            List<OfferLog> expectedUnitOfferLogs = Stream.concat(Stream.concat(
                IntStream.rangeClosed(1, 40).mapToObj(
                    i -> new OfferLog(i, null, "0_unit", "unit" + i, OfferLogAction.WRITE)),
                IntStream.rangeClosed(61, 80).mapToObj(
                    i -> new OfferLog(i, null, "0_unit", "unit" + i, OfferLogAction.WRITE))),
                IntStream.rangeClosed(81, 90).mapToObj(
                    i -> new OfferLog(i, null, "0_unit", "unit" + i, OfferLogAction.DELETE))
            ).collect(Collectors.toList());

            checkOfferLogs(storageClient, expectedUnitOfferLogs, null, 1000, Order.ASC);
            checkOfferLogs(storageClient, expectedUnitOfferLogs, null, 13, Order.ASC);
            checkOfferLogs(storageClient, expectedUnitOfferLogs, 0L, 1000, Order.ASC);
            checkOfferLogs(storageClient, expectedUnitOfferLogs, 0L, 1000, Order.ASC);
            checkOfferLogs(storageClient, expectedUnitOfferLogs, 15L, 1000, Order.ASC);
            checkOfferLogs(storageClient, expectedUnitOfferLogs, 15L, 13, Order.ASC);
            checkOfferLogs(storageClient, expectedUnitOfferLogs, 90L, 1000, Order.ASC);
            checkOfferLogs(storageClient, expectedUnitOfferLogs, 90L, 13, Order.ASC);
            checkOfferLogs(storageClient, expectedUnitOfferLogs, 101L, 1000, Order.DESC);
            checkOfferLogs(storageClient, expectedUnitOfferLogs, 101L, 13, Order.DESC);

            checkOfferLogs(storageClient, expectedUnitOfferLogs, null, 1000, Order.DESC);
            checkOfferLogs(storageClient, expectedUnitOfferLogs, null, 13, Order.DESC);
            checkOfferLogs(storageClient, expectedUnitOfferLogs, 0L, 1000, Order.DESC);
            checkOfferLogs(storageClient, expectedUnitOfferLogs, 0L, 1000, Order.DESC);
            checkOfferLogs(storageClient, expectedUnitOfferLogs, 15L, 1000, Order.DESC);
            checkOfferLogs(storageClient, expectedUnitOfferLogs, 15L, 13, Order.DESC);
            checkOfferLogs(storageClient, expectedUnitOfferLogs, 90L, 1000, Order.DESC);
            checkOfferLogs(storageClient, expectedUnitOfferLogs, 90L, 13, Order.DESC);
            checkOfferLogs(storageClient, expectedUnitOfferLogs, 101L, 1000, Order.DESC);
            checkOfferLogs(storageClient, expectedUnitOfferLogs, 101L, 13, Order.DESC);

        }
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
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        VitamThreadUtils.getVitamSession().setContractId("aName3");
        VitamThreadUtils.getVitamSession().setContextId("Context_IT");
        VitamThreadUtils.getVitamSession().setRequestId(ingestOperationGuid);
        // workspace client unzip SIP in workspace

        // init default logbook operation
        final List<LogbookOperationParameters> params = new ArrayList<>();
        final LogbookOperationParameters initParameters = LogbookParameterHelper.newLogbookOperationParameters(
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

    private void awaitForWorkflowTerminationWithStatus(String operationGuid, StatusCode expectedStatusCode) {

        wait(operationGuid);

        ProcessWorkflow processWorkflow =
            ProcessMonitoringImpl.getInstance().findOneProcessWorkflow(operationGuid, tenantId);

        try {
            assertNotNull(processWorkflow);
            assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
            assertEquals(expectedStatusCode, processWorkflow.getStatus());
        } catch (AssertionError e) {
            tryLogLogbookOperation(operationGuid);
            tryLogATR(operationGuid);
            throw e;
        }
    }

    private void tryLogLogbookOperation(String operationId) {
        try (LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient()) {
            JsonNode logbookOperation = logbookClient.selectOperationById(operationId);
            LOGGER.error("Operation logbook status : \n" + JsonHandler.prettyPrint(logbookOperation) + "\n\n\n");
        } catch (Exception e) {
            LOGGER.error("Could not retrieve logbook operation for operation " + operationId, e);
        }
    }

    private void tryLogATR(String operationId) {
        try (InputStream atr = readStoredReport(operationId + XML)) {
            LOGGER.error("Operation ATR : \n" + IOUtils.toString(atr, StandardCharsets.UTF_8) + "\n\n\n");
        } catch (StorageNotFoundException ignored) {
        } catch (Exception e) {
            LOGGER.error("Could not retrieve ATR for operation " + operationId, e);
        }
    }

    private void compactOfferLogs() throws java.io.IOException {
        retrofit2.Response<Void> response = compactionRestService.launchOfferLogCompaction()
            .execute();

        assertThat(response.isSuccessful()).as("check request compaction successfully execute")
            .isTrue();
    }

    private void checkOfferLogs(StorageClient storageClient, List<OfferLog> unitOfferLogs,
        Long offset, int limit, Order order)
        throws StorageServerClientException {
        List<OfferLog> computedOfferLogs =
            getOfferLogsFromStorage(storageClient, DataCategory.UNIT, offset, limit, order);
        List<OfferLog> expectedOfferLogs = unitOfferLogs.stream()
            .filter(log -> offset == null ||
                (order == Order.ASC ? offset <= log.getSequence() : offset >= log.getSequence()))
            .sorted(order == Order.ASC ? Comparator.naturalOrder() : Comparator.reverseOrder())
            .limit(limit)
            .collect(Collectors.toList());

        assertThat(computedOfferLogs).hasSameSizeAs(expectedOfferLogs);
        assertThat(computedOfferLogs)
            .usingElementComparatorOnFields("container", "fileName", "action", "sequence")
            .containsExactlyElementsOf(expectedOfferLogs);
    }

    private List<OfferLog> getOfferLogsFromStorage(StorageClient storageClient, DataCategory dataCategory, Long offset,
        int limit, Order order) throws StorageServerClientException {
        RequestResponse<OfferLog> response = storageClient.getOfferLogs(STRATEGY, dataCategory, offset, limit, order);
        if (!response.isOk()) {
            throw new VitamRuntimeException("Could not list offer log");
        }
        return ((RequestResponseOK<OfferLog>) response).getResults();
    }

    private List<OfferLog> getDbOfferLogs(String container) {
        return getDbOfferLogs(eq(CONTAINER, container));
    }

    private List<OfferLog> getDbOfferLogs() {
        return getDbOfferLogs(new BsonDocument());
    }

    private List<OfferLog> getDbOfferLogs(Bson query) {
        return IteratorUtils.toList(
            mongoRule.getMongoDatabase()
                .getCollection(OFFER_LOG.getName())
                .find(query)
                .sort(Sorts.ascending(CompactedOfferLog.SEQUENCE_START))
                .map(this::mapToOfferLog)
                .iterator()
        );
    }

    private OfferLog mapToOfferLog(Document document) {
        try {
            return JsonHandler.getFromString(BsonHelper.stringify(document), OfferLog.class);
        } catch (InvalidParseOperationException e) {
            throw new VitamRuntimeException(e);
        }
    }

    private List<CompactedOfferLog> getDbCompactedOfferLogs(String container) {
        return getDbCompactedOfferLogs(eq(CONTAINER, container));
    }

    private List<CompactedOfferLog> getDbCompactedOfferLogs() {
        return getDbCompactedOfferLogs(new BsonDocument());
    }

    private List<CompactedOfferLog> getDbCompactedOfferLogs(Bson query) {
        return IteratorUtils.toList(
            mongoRule.getMongoDatabase()
                .getCollection(COMPACTED_OFFER_LOG.getName())
                .find(query)
                .sort(Sorts.ascending(CompactedOfferLog.SEQUENCE_START))
                .map(this::mapToOfferLogCompaction)
                .iterator()
        );
    }

    private CompactedOfferLog mapToOfferLogCompaction(Document document) {
        try {
            return JsonHandler.getFromString(BsonHelper.stringify(document), CompactedOfferLog.class);
        } catch (InvalidParseOperationException e) {
            throw new VitamRuntimeException(e);
        }
    }

    public interface CompactionRestService {
        @POST("/offer/v1/compaction")
        @Headers({
            "Accept: application/json"
        })
        Call<Void> launchOfferLogCompaction();
    }
}
