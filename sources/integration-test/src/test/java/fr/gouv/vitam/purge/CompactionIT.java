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
import com.mongodb.client.MongoIterable;
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
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.storage.offers.rest.DefaultOfferMain;
import fr.gouv.vitam.storage.offers.rest.OfferLogCompactionConfiguration;
import fr.gouv.vitam.worker.server.rest.WorkerMain;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import okhttp3.OkHttpClient;
import org.apache.commons.io.IOUtils;
import org.bson.Document;
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
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;
import static fr.gouv.vitam.common.guid.GUIDFactory.newOperationLogbookGUID;
import static fr.gouv.vitam.storage.engine.common.collection.OfferCollections.COMPACTED_OFFER_LOG;
import static fr.gouv.vitam.storage.engine.common.collection.OfferCollections.OFFER_LOG;
import static fr.gouv.vitam.storage.engine.common.model.CompactedOfferLog.CONTAINER;
import static fr.gouv.vitam.storage.engine.common.model.DataCategory.UNIT;
import static fr.gouv.vitam.storage.engine.common.model.Order.ASC;
import static fr.gouv.vitam.storage.engine.common.model.Order.DESC;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CompactionIT extends VitamRuleRunner {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CompactionIT.class);

    private static final Integer tenantId = 0;
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
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private WorkFlow workflow = WorkFlow.of(WORKFLOW_ID, WORKFLOW_IDENTIFIER, "INGEST");

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        handleBeforeClass(0, 1);

        FormatIdentifierFactory.getInstance()
            .changeConfigurationFile(PropertiesUtils.getResourcePath("integration-ingest-internal/format-identifiers.conf").toString());

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
    }

    @Before
    public void setUpBefore() {
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(0));
    }

    @RunWithCustomExecutor
    @Test
    public void should_compact_offer_log() throws Exception {
        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {

            doIngest(PropertiesUtils.getResourceAsStream("compaction/sip_compaction.zip"), StatusCode.OK);

            RequestResponse<OfferLog> offerLogsDescResponseBeforeCompaction = storageClient.getOfferLogs(VitamConfiguration.getDefaultStrategy(), UNIT, null, 6, DESC);
            RequestResponse<OfferLog> offerLogsAscResponseBeforeCompaction = storageClient.getOfferLogs(VitamConfiguration.getDefaultStrategy(), UNIT, null, 6, ASC);

            SECONDS.sleep(3);

            retrofit2.Response<Void> compactOfferLog = compactionRestService.launchOfferLogCompaction()
                .execute();

            assertThat(compactOfferLog.isSuccessful()).as("check request compaction successfully execute").isTrue();

            Iterable<CompactedOfferLog> offerLogsCompaction = getOfferLogsCompaction();
            Iterable<OfferLog> offerLogsFromCompaction = offerLogsCompaction.iterator().next().getLogs();
            Iterable<OfferLog> offerLogs = getOfferLogs();
            assertThat(offerLogsCompaction).as("check offer logs compaction collection is not empty").isNotEmpty();
            assertThat(offerLogs).as("check offer logs compaction collection is empty").isEmpty();

            RequestResponse<OfferLog> offerLogsDescResponse = storageClient.getOfferLogs(VitamConfiguration.getDefaultStrategy(), UNIT, null, 6, DESC);
            RequestResponse<OfferLog> offerLogsAscResponse = storageClient.getOfferLogs(VitamConfiguration.getDefaultStrategy(), UNIT, null, 6, ASC);

            assertThat(offerLogsDescResponse.isOk()).as("check request search offer logs successfully execute").isTrue();
            assertThat(offerLogsAscResponse.isOk()).as("check request search offer logs successfully execute").isTrue();

            List<OfferLog> offerLogsDesc = ((RequestResponseOK<OfferLog>) offerLogsDescResponse).getResults();
            List<OfferLog> offerLogsAsc = ((RequestResponseOK<OfferLog>) offerLogsAscResponse).getResults();

            assertThat(offerLogsDesc).as("check offer logs retrieve by descending query is not empty").isNotEmpty();
            assertThat(offerLogsAsc).as("check offer logs retrieve by ascending query is not empty").isNotEmpty();

            assertThat(offerLogsDesc).as("check offer logs retrieve by descending query size").hasSize(6);
            assertThat(offerLogsAsc).as("check offer logs retrieve by ascending query size").hasSize(6);

            assertThat(offerLogsFromCompaction).containsExactlyInAnyOrderElementsOf(offerLogsAsc);
            assertThat(offerLogsFromCompaction).containsExactlyInAnyOrderElementsOf(offerLogsDesc);

            assertThat(offerLogsDesc).containsExactlyElementsOf(((RequestResponseOK<OfferLog>) offerLogsDescResponseBeforeCompaction).getResults());
            assertThat(offerLogsAsc).containsExactlyElementsOf(((RequestResponseOK<OfferLog>) offerLogsAscResponseBeforeCompaction).getResults());
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

    public MongoIterable<OfferLog> getOfferLogs() {
        return mongoRule.getMongoDatabase()
            .getCollection(OFFER_LOG.getName())
            .find(eq(CONTAINER, "0_unit"))
            .map(this::getOfferLog);
    }

    public OfferLog getOfferLog(Document document) {
        try {
            return JsonHandler.getFromString(BsonHelper.stringify(document), OfferLog.class);
        } catch (InvalidParseOperationException e) {
            throw new VitamRuntimeException(e);
        }
    }

    public MongoIterable<CompactedOfferLog> getOfferLogsCompaction() {
        return mongoRule.getMongoDatabase()
            .getCollection(COMPACTED_OFFER_LOG.getName())
            .find(eq(CONTAINER, "0_unit"))
            .map(this::getOfferLogCompaction);
    }

    public CompactedOfferLog getOfferLogCompaction(Document document) {
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
