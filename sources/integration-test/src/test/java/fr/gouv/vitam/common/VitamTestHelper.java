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

package fr.gouv.vitam.common;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.accesslog.AccessLogUtils;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InternalServerException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.ProcessQuery;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.processing.ProcessDetail;
import fr.gouv.vitam.common.model.processing.WorkFlow;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClient;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClientFactory;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.parameters.Contexts;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.model.ProcessWorkflow;
import fr.gouv.vitam.processing.engine.core.monitoring.ProcessMonitoringImpl;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageUnavailableDataFromAsyncOfferClientException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.apache.commons.collections4.iterators.PeekingIterator;
import org.bson.Document;

import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.mongodb.client.model.Filters.eq;
import static fr.gouv.vitam.common.VitamServerRunner.NB_TRY;
import static fr.gouv.vitam.common.model.IngestWorkflowConstants.SANITY_CHECK_RESULT_FILE;
import static fr.gouv.vitam.common.model.IngestWorkflowConstants.STP_UPLOAD_RESULT_JSON;
import static fr.gouv.vitam.common.model.MetadataStorageHelper.GOT_KEY;
import static fr.gouv.vitam.common.model.MetadataStorageHelper.LFC_KEY;
import static fr.gouv.vitam.common.model.MetadataStorageHelper.UNIT_KEY;
import static fr.gouv.vitam.common.model.ProcessState.COMPLETED;
import static fr.gouv.vitam.common.model.ProcessState.RUNNING;
import static fr.gouv.vitam.common.model.VitamConstants.JSON_EXTENSION;
import static fr.gouv.vitam.common.model.logbook.LogbookEvent.OB_ID;
import static fr.gouv.vitam.logbook.common.parameters.Contexts.DEFAULT_WORKFLOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class VitamTestHelper {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(VitamTestHelper.class);
    private static final long SLEEP_TIME = 20L;

    private VitamTestHelper() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void prepareVitamSession(Integer tenantId, String contractId, String contextId) {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        VitamThreadUtils.getVitamSession().setContractId(contractId);
        VitamThreadUtils.getVitamSession().setContextId(contextId);
    }

    public static void runStepByStepUntilStepReached(String operationGuid, String targetStepName)
        throws VitamClientException, InternalServerException, InterruptedException, ProcessingException {
        try (
            ProcessingManagementClient processingClient = ProcessingManagementClientFactory.getInstance().getClient()
        ) {
            while (true) {
                ProcessQuery processQuery = new ProcessQuery();
                processQuery.setId(operationGuid);
                RequestResponse<ProcessDetail> response = processingClient.listOperationsDetails(processQuery);
                if (response.isOk()) {
                    ProcessDetail processDetail = ((RequestResponseOK<ProcessDetail>) response).getResults().get(0);
                    switch (ProcessState.valueOf(processDetail.getGlobalState())) {
                        case PAUSE:
                            if (processDetail.getPreviousStep().equals(targetStepName)) {
                                LOGGER.info(operationGuid + " finished step " + targetStepName);
                                return;
                            }
                            processingClient.executeOperationProcess(
                                operationGuid,
                                DEFAULT_WORKFLOW.name(),
                                ProcessAction.NEXT.getValue()
                            );
                            break;
                        case RUNNING:
                            // Sleep and retry
                            TimeUnit.MILLISECONDS.sleep(SLEEP_TIME);
                            break;
                        case COMPLETED:
                            throw new VitamRuntimeException(
                                "Process completion unexpected " + JsonHandler.unprettyPrint(processDetail)
                            );
                    }
                } else {
                    throw new ProcessingException("listOperationsDetails does not response ...");
                }
            }
        }
    }

    public static void verifyLogbook(String operationId, String actionKey, String statusCode) {
        Document operation = LogbookCollections.OPERATION.getCollection().find(eq("_id", operationId)).first();
        assertThat(operation).isNotNull();
        assertTrue(operation.toString().contains(String.format("%s.%s", actionKey, statusCode)));
    }

    public static JsonNode findLogbook(String opId) {
        try (LogbookOperationsClient client = LogbookOperationsClientFactory.getInstance().getClient()) {
            return client.selectOperationById(opId);
        } catch (LogbookClientException | InvalidParseOperationException e) {
            return fail("cannot find logbook with id = " + opId, e);
        }
    }

    public static String printLogbook(String opId) {
        try {
            JsonNode logbook = findLogbook(opId);
            RequestResponseOK<JsonNode> result = RequestResponseOK.getFromJsonNode(logbook);
            assertThat(result.getResults()).isNotEmpty();
            return JsonHandler.prettyPrint(result.getFirstResult());
        } catch (InvalidParseOperationException e) {
            return fail("Error - Cannot read LogbookOperation " + opId, e);
        }
    }

    public static String readReportFile(String fileName) {
        try (StorageClient client = StorageClientFactory.getInstance().getClient()) {
            Response containerAsync = client.getContainerAsync(
                VitamConfiguration.getDefaultStrategy(),
                fileName,
                DataCategory.REPORT,
                AccessLogUtils.getNoLogAccessLog()
            );
            return containerAsync.readEntity(String.class);
        } catch (StorageNotFoundException e) {
            return "No " + fileName + " found";
        } catch (Exception e) {
            return fail("Cannot read " + fileName, e);
        }
    }

    /**
     * Extract lines of jsonl report to list of JsonNode
     *
     * @param reportResponse
     * @return
     * @throws IOException
     * @throws InvalidParseOperationException
     */
    public static List<JsonNode> getReport(Response reportResponse) throws IOException, InvalidParseOperationException {
        List<JsonNode> reportLines = new ArrayList<>();
        try (InputStream is = reportResponse.readEntity(InputStream.class)) {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            PeekingIterator<String> linesPeekIterator = new PeekingIterator<>(bufferedReader.lines().iterator());
            while (linesPeekIterator.hasNext()) {
                reportLines.add(JsonHandler.getFromString(linesPeekIterator.next()));
            }
        }
        return reportLines;
    }

    public static List<JsonNode> getReports(String operationGuid) {
        try (
            StorageClient storageClient = StorageClientFactory.getInstance().getClient();
            Response reportResponse = storageClient.getContainerAsync(
                VitamConfiguration.getDefaultStrategy(),
                operationGuid + ".jsonl",
                DataCategory.REPORT,
                AccessLogUtils.getNoLogAccessLog()
            )
        ) {
            assertThat(reportResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
            return getReport(reportResponse);
        } catch (InvalidParseOperationException | IOException e) {
            fail("error while retrieving report for operation ", operationGuid, e);
        } catch (
            StorageServerClientException
            | StorageNotFoundException
            | StorageUnavailableDataFromAsyncOfferClientException e
        ) {
            fail("error while retrieving container from storage ", e);
        }
        return null;
    }

    public static String doIngest(
        int tenantId,
        String zip,
        Contexts context,
        ProcessAction processAction,
        StatusCode logbookStatus
    ) throws VitamException {
        final InputStream zipStream;
        try {
            zipStream = PropertiesUtils.getResourceAsStream(zip);
            return doIngest(tenantId, zipStream, context, processAction, logbookStatus);
        } catch (FileNotFoundException e) {
            fail("cannot find file", zip, e);
            return null;
        }
    }

    public static String doIngest(
        int tenantId,
        InputStream zipStream,
        Contexts context,
        ProcessAction processAction,
        StatusCode logbookStatus
    ) throws VitamException {
        final WorkFlow workflow = WorkFlow.of(
            context.name(),
            context.getEventType(),
            context.getLogbookTypeProcess().name()
        );
        final GUID ingestOperationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);

        VitamThreadUtils.getVitamSession().setRequestId(ingestOperationGuid);

        // init default logbook operation
        final List<LogbookOperationParameters> params = new ArrayList<>();
        final LogbookOperationParameters initParameters = LogbookParameterHelper.newLogbookOperationParameters(
            ingestOperationGuid,
            context.getEventType(),
            ingestOperationGuid,
            LogbookTypeProcess.INGEST,
            logbookStatus,
            ingestOperationGuid.toString(),
            ingestOperationGuid
        );
        params.add(initParameters);

        // call ingest
        final IngestInternalClient client = IngestInternalClientFactory.getInstance().getClient();
        client.uploadInitialLogbook(params);

        // init workflow before execution
        client.initWorkflow(workflow);

        try (final WorkspaceClient workspaceClient = WorkspaceClientFactory.getInstance().getClient()) {
            InputStream sanityCheckStream = PropertiesUtils.getResourceAsStream(SANITY_CHECK_RESULT_FILE);
            workspaceClient.putObject(ingestOperationGuid.getId(), SANITY_CHECK_RESULT_FILE, sanityCheckStream);
        } catch (FileNotFoundException e) {
            fail("SanityCheck File not found", e);
        }

        client.updateOperationActionProcess(processAction.getValue(), ingestOperationGuid.getId());

        client.upload(zipStream, CommonMediaType.ZIP_TYPE, workflow, processAction.name());

        waitOperation(NB_TRY, SLEEP_TIME, ingestOperationGuid.getId());
        return ingestOperationGuid.toString();
    }

    public static String doIngestWithLogbookStatus(int tenantId, String zip, StatusCode logbokStatus)
        throws VitamException {
        return doIngest(tenantId, zip, DEFAULT_WORKFLOW, ProcessAction.RESUME, logbokStatus);
    }

    public static String doIngest(int tenantId, String zip) throws VitamException {
        return doIngest(tenantId, zip, DEFAULT_WORKFLOW, ProcessAction.RESUME, StatusCode.STARTED);
    }

    public static String doIngest(int tenantId, InputStream is) throws VitamException {
        return doIngest(tenantId, is, DEFAULT_WORKFLOW, ProcessAction.RESUME, StatusCode.STARTED);
    }

    public static String doIngestNext(int tenantId, String zip) throws VitamException {
        return doIngest(tenantId, zip, DEFAULT_WORKFLOW, ProcessAction.NEXT, StatusCode.STARTED);
    }

    public static String doIngestNext(int tenantId, InputStream is) throws VitamException {
        return doIngest(tenantId, is, DEFAULT_WORKFLOW, ProcessAction.NEXT, StatusCode.STARTED);
    }

    public static void verifyOperation(String opId, StatusCode statusCode) {
        try (ProcessingManagementClient client = ProcessingManagementClientFactory.getInstance().getClient()) {
            ItemStatus itemStatus = client.getOperationProcessStatus(opId);
            try {
                assertThat(itemStatus.getGlobalStatus()).isEqualTo(statusCode);
            } catch (AssertionError e) {
                System.err.println(VitamTestHelper.printLogbook(opId));
                throw e;
            }
        } catch (VitamClientException | InternalServerException | BadRequestException e) {
            fail("cannot find process with id = ", opId, e);
        }
    }

    public static void verifyOperation(String opId, StatusCode... statusCodes) {
        try (ProcessingManagementClient client = ProcessingManagementClientFactory.getInstance().getClient()) {
            ItemStatus itemStatus = client.getOperationProcessStatus(opId);
            try {
                assertThat(itemStatus.getGlobalStatus()).isIn(statusCodes);
            } catch (AssertionError e) {
                System.err.println(VitamTestHelper.printLogbook(opId));
                throw e;
            }
        } catch (VitamClientException | InternalServerException | BadRequestException e) {
            fail("cannot find process with id = ", opId, e);
        }
    }

    public static void verifyProcessState(String operationId, Integer tenantId, ProcessState processState) {
        ProcessWorkflow processWorkflow = ProcessMonitoringImpl.getInstance()
            .findOneProcessWorkflow(operationId, tenantId);
        assertThat(processWorkflow).isNotNull();
        assertThat(processWorkflow.getState()).isEqualTo(processState);
    }

    public static void waitOperation(long nbTry, long timeToSleep, String operationId, ProcessState processState) {
        try (
            ProcessingManagementClient processingManagementClient = ProcessingManagementClientFactory.getInstance()
                .getClient()
        ) {
            for (int nbtimes = 0; nbtimes <= nbTry; nbtimes++) {
                final ItemStatus opStatus = processingManagementClient.getOperationProcessStatus(operationId);
                final ProcessState state = opStatus.getGlobalState();
                if ((processState == null && !state.equals(RUNNING)) || state.equals(processState)) {
                    break;
                } else if (nbtimes == nbTry) {
                    LOGGER.error(
                        "Process with id={} not finished state={}, status={}",
                        operationId,
                        state,
                        opStatus.getGlobalStatus()
                    );
                    fail("Process did not finished on time");
                }
                try {
                    TimeUnit.MILLISECONDS.sleep(timeToSleep);
                } catch (InterruptedException e) {
                    SysErrLogger.FAKE_LOGGER.ignoreLog(e);
                }
            }
        } catch (VitamClientException | InternalServerException | BadRequestException e) {
            fail("An error occured while retreiving process status", e);
        }
    }

    public static void waitOperation(long nbTry, long timeToSleep, String operationId) {
        waitOperation(nbTry, timeToSleep, operationId, null);
    }

    public static void waitOperation(String operationId, ProcessState processState) {
        waitOperation(VitamServerRunner.NB_TRY, VitamServerRunner.SLEEP_TIME, operationId, processState);
    }

    public static void waitOperation(String operationId) {
        waitOperation(VitamServerRunner.NB_TRY, VitamServerRunner.SLEEP_TIME, operationId);
    }

    public static void insertWaitForStepEssentialFiles(String containerName) {
        try (final WorkspaceClient workspaceClient = WorkspaceClientFactory.getInstance().getClient()) {
            InputStream sanityCheckStream = PropertiesUtils.getResourceAsStream(SANITY_CHECK_RESULT_FILE);
            workspaceClient.putObject(containerName, SANITY_CHECK_RESULT_FILE, sanityCheckStream);

            InputStream stpUploadStream = PropertiesUtils.getResourceAsStream(STP_UPLOAD_RESULT_JSON);
            workspaceClient.putObject(containerName, STP_UPLOAD_RESULT_JSON, stpUploadStream);
        } catch (FileNotFoundException | ContentAddressableStorageServerException e) {
            fail("File not found", e);
        }
    }

    public static void awaitForWorkflowTerminationWithStatus(GUID operationGuid, StatusCode status) {
        awaitForWorkflowTerminationWithStatus(operationGuid, status, COMPLETED);
    }

    private static void awaitForWorkflowTerminationWithStatus(
        GUID operationGuid,
        StatusCode status,
        ProcessState processState
    ) {
        waitOperation(operationGuid.toString());

        ProcessWorkflow processWorkflow = ProcessMonitoringImpl.getInstance()
            .findOneProcessWorkflow(operationGuid.toString(), VitamThreadUtils.getVitamSession().getTenantId());

        assertNotNull(processWorkflow);
        assertEquals(processState, processWorkflow.getState());
        assertEquals(status, processWorkflow.getStatus());
    }

    public static JsonNode getObjectfromOffer(String obId, DataCategory dataCategory)
        throws StorageServerClientException, StorageNotFoundException, InvalidParseOperationException, StorageUnavailableDataFromAsyncOfferClientException {
        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
            Response object = storageClient.getContainerAsync(
                VitamConfiguration.getDefaultStrategy(),
                obId + JSON_EXTENSION,
                dataCategory,
                AccessLogUtils.getNoLogAccessLog()
            );
            InputStream inputStream = object.readEntity(InputStream.class);
            JsonNode resultOffer = JsonHandler.getFromInputStream(inputStream);
            assertNotNull(resultOffer);
            return resultOffer;
        }
    }

    public static JsonNode getUnitLFCfromOffer(String obId)
        throws StorageServerClientException, StorageNotFoundException, InvalidParseOperationException, StorageUnavailableDataFromAsyncOfferClientException {
        final JsonNode resultOffer = getObjectfromOffer(obId, DataCategory.UNIT);
        assertTrue(resultOffer.has(LFC_KEY));
        return resultOffer.get(LFC_KEY);
    }

    public static JsonNode getObjectGroupLFCfromOffer(String obId)
        throws StorageServerClientException, StorageNotFoundException, InvalidParseOperationException, StorageUnavailableDataFromAsyncOfferClientException {
        final JsonNode resultOffer = getObjectfromOffer(obId, DataCategory.OBJECTGROUP);
        assertTrue(resultOffer.has(LFC_KEY));
        return resultOffer.get(LFC_KEY);
    }

    public static JsonNode getUnitfromOffer(String obId)
        throws StorageServerClientException, StorageNotFoundException, InvalidParseOperationException, StorageUnavailableDataFromAsyncOfferClientException {
        final JsonNode resultOffer = getObjectfromOffer(obId, DataCategory.UNIT);
        assertTrue(resultOffer.has(UNIT_KEY));
        return resultOffer.get(UNIT_KEY);
    }

    public static JsonNode getObjectGroupfromOffer(String obId)
        throws StorageServerClientException, StorageNotFoundException, InvalidParseOperationException, StorageUnavailableDataFromAsyncOfferClientException {
        final JsonNode resultOffer = getObjectfromOffer(obId, DataCategory.OBJECTGROUP);
        assertTrue(resultOffer.has(GOT_KEY));
        return resultOffer.get(GOT_KEY);
    }

    public static JsonNode getUnitLFC(String obId)
        throws InvalidCreateOperationException, LogbookClientException, InvalidParseOperationException {
        try (
            LogbookLifeCyclesClient logbookLifeCyclesClient = LogbookLifeCyclesClientFactory.getInstance().getClient()
        ) {
            final Select select = new Select();
            select.setQuery(QueryHelper.eq(OB_ID, obId));
            JsonNode unitLFCresponse = logbookLifeCyclesClient.selectUnitLifeCycle(select.getFinalSelect());
            JsonNode results = unitLFCresponse.get(RequestResponseOK.TAG_RESULTS);
            assertNotNull(results);
            assertFalse(results.isEmpty());
            return results.get(0);
        }
    }

    public static JsonNode getObjectGroupLFC(String obId)
        throws InvalidCreateOperationException, LogbookClientException, InvalidParseOperationException {
        try (
            LogbookLifeCyclesClient logbookLifeCyclesClient = LogbookLifeCyclesClientFactory.getInstance().getClient()
        ) {
            final Select select = new Select();
            select.setQuery(QueryHelper.eq(OB_ID, obId));
            JsonNode ogLFCresponse = logbookLifeCyclesClient.selectObjectGroupLifeCycle(select.getFinalSelect());
            JsonNode results = ogLFCresponse.get(RequestResponseOK.TAG_RESULTS);
            assertNotNull(results);
            assertFalse(results.isEmpty());
            return results.get(0);
        }
    }

    public static void doTraceabilityGots() throws VitamException {
        try (
            LogbookOperationsClient logbookOperationsClient = LogbookOperationsClientFactory.getInstance().getClient()
        ) {
            RequestResponseOK<?> traceabilityObjectGroupResponse = logbookOperationsClient.traceabilityLfcObjectGroup();
            String traceabilityGotOperationId = traceabilityObjectGroupResponse.getHeaderString(
                GlobalDataRest.X_REQUEST_ID
            );
            waitOperation(traceabilityGotOperationId);
        }
    }

    public static void doTraceabilityUnits() throws VitamException {
        try (
            LogbookOperationsClient logbookOperationsClient = LogbookOperationsClientFactory.getInstance().getClient()
        ) {
            RequestResponseOK<?> traceabilityUnitResponse = logbookOperationsClient.traceabilityLfcUnit();
            String traceabilityUnitOperationId = traceabilityUnitResponse.getHeaderString(GlobalDataRest.X_REQUEST_ID);
            waitOperation(traceabilityUnitOperationId);
        }
    }

    public static void doTraceabilityOperations() throws VitamException {
        Integer tenantId = VitamThreadUtils.getVitamSession().getTenantId();

        try (LogbookOperationsClient client = LogbookOperationsClientFactory.getInstance().getClient()) {
            VitamThreadUtils.getVitamSession().setTenantId(VitamConfiguration.getAdminTenant());

            client.traceability(List.of(tenantId));
        } finally {
            VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        }
    }
}
