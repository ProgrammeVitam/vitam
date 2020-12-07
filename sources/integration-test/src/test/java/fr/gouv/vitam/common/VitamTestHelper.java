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

package fr.gouv.vitam.common;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.accesslog.AccessLogUtils;
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
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
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
import static fr.gouv.vitam.common.model.RequestResponseOK.TAG_RESULTS;
import static fr.gouv.vitam.logbook.common.parameters.Contexts.DEFAULT_WORKFLOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.Assert.assertEquals;
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

        try (ProcessingManagementClient processingClient =
            ProcessingManagementClientFactory.getInstance().getClient()) {

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
                            processingClient
                                .executeOperationProcess(operationGuid, DEFAULT_WORKFLOW.name(),
                                    ProcessAction.NEXT.getValue());
                            break;

                        case RUNNING:

                            // Sleep and retry
                            TimeUnit.MILLISECONDS.sleep(SLEEP_TIME);
                            break;

                        case COMPLETED:
                            throw new VitamRuntimeException(
                                "Process completion unexpected " + JsonHandler.unprettyPrint(processDetail));
                    }
                } else {
                    throw new ProcessingException("listOperationsDetails does not response ...");
                }
            }
        }
    }

    public static void verifyLogbook(String operationId, String actionKey, String statusCode) {
        Document operation =
            LogbookCollections.OPERATION.getCollection().find(eq("_id", operationId)).first();
        assertThat(operation).isNotNull();
        assertTrue(operation.toString().contains(String.format("%s.%s", actionKey, statusCode)));
    }

    public static JsonNode findLogbook(String opId) {
        try (LogbookOperationsClient client = LogbookOperationsClientFactory.getInstance().getClient()) {
            return client.selectOperationById(opId);
        } catch (LogbookClientException | InvalidParseOperationException e) {
            fail("cannot find logbook with id = ", opId, e);
        }
        return null;
    }

    public static void printLogbook(String opId) {
        JsonNode result = findLogbook(opId);
        System.out.println(JsonHandler.prettyPrint(result.get(TAG_RESULTS).get(0)));
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
        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient();
            Response reportResponse = storageClient.getContainerAsync(VitamConfiguration.getDefaultStrategy(),
                operationGuid + ".jsonl", DataCategory.REPORT,
                AccessLogUtils.getNoLogAccessLog())) {
            assertThat(reportResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
            return getReport(reportResponse);
        } catch (InvalidParseOperationException | IOException e) {
            fail("error while retrieving report for operation ", operationGuid, e);
        } catch (StorageServerClientException | StorageNotFoundException e) {
            fail("error while retrieving container from storage ", e);
        }
        return null;
    }

    public static String doIngest(int tenantId, String zip) throws VitamException {
        final InputStream zipStream;
        final WorkFlow workflow =
            WorkFlow.of(DEFAULT_WORKFLOW.name(), DEFAULT_WORKFLOW.getEventType(),
                DEFAULT_WORKFLOW.getLogbookTypeProcess().name());
        final GUID ingestOperationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);

        VitamThreadUtils.getVitamSession().setRequestId(ingestOperationGuid);

        try {
            zipStream = PropertiesUtils.getResourceAsStream(zip);
        } catch (FileNotFoundException e) {
            fail("cannot find file", zip, e);
            return null;
        }

        // init default logbook operation
        final List<LogbookOperationParameters> params = new ArrayList<>();
        final LogbookOperationParameters initParameters = LogbookParameterHelper.newLogbookOperationParameters(
            ingestOperationGuid, DEFAULT_WORKFLOW.getEventType(), ingestOperationGuid, LogbookTypeProcess.INGEST,
            StatusCode.STARTED, ingestOperationGuid.toString(), ingestOperationGuid);
        params.add(initParameters);

        // call ingest
        final IngestInternalClient client = IngestInternalClientFactory.getInstance().getClient();
        client.uploadInitialLogbook(params);

        // init workflow before execution
        client.initWorkflow(workflow);

        client.upload(zipStream, CommonMediaType.ZIP_TYPE, workflow, ProcessAction.RESUME.name());

        waitOperation(NB_TRY, SLEEP_TIME, ingestOperationGuid.getId());
        return ingestOperationGuid.toString();
    }

    public static void verifyOperation(String opId, StatusCode statusCode) {
        try (ProcessingManagementClient client = ProcessingManagementClientFactory.getInstance().getClient()) {
            ItemStatus itemStatus =
                client.getOperationProcessStatus(opId);
            if (!statusCode.equals(itemStatus.getGlobalStatus()))
                VitamTestHelper.printLogbook(opId);
            assertEquals(statusCode, itemStatus.getGlobalStatus());
        } catch (VitamClientException | InternalServerException | BadRequestException e) {
            fail("cannot find process with id = ", opId, e);
        }
    }


    public static void waitOperation(long nbTry, long timeToSleep, String operationId) {
        ProcessingManagementClient processingClient = ProcessingManagementClientFactory.getInstance().getClient();
        for (int nbtimes = 0; (nbtimes <= nbTry && !processingClient.isNotRunning(operationId)); nbtimes++) {
            try {
                TimeUnit.MILLISECONDS.sleep(timeToSleep);
            } catch (InterruptedException e) {
                SysErrLogger.FAKE_LOGGER.ignoreLog(e);
            }
        }
    }

    public static void waitOperation(String operationId) {
        waitOperation(VitamServerRunner.NB_TRY, VitamServerRunner.SLEEP_TIME, operationId);
    }
}
