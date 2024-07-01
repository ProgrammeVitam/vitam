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
package fr.gouv.vitam.worker.core.plugin.ingestcleanup;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessQuery;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.logbook.LogbookEvent;
import fr.gouv.vitam.common.model.logbook.LogbookOperation;
import fr.gouv.vitam.common.model.processing.ProcessDetail;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameterName;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.exception.ProcessingStatusException;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

import static fr.gouv.vitam.common.model.RequestResponseOK.TAG_RESULTS;
import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;

public class IngestCleanupRequestValidationPlugin extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(
        IngestCleanupRequestValidationPlugin.class
    );
    String INGEST_CLEANUP_REQUEST_VALIDATION = "INGEST_CLEANUP_REQUEST_VALIDATION";

    private final ProcessingManagementClientFactory processingManagementClientFactory;
    private final LogbookOperationsClientFactory logbookOperationsClientFactory;

    public IngestCleanupRequestValidationPlugin() {
        this(ProcessingManagementClientFactory.getInstance(), LogbookOperationsClientFactory.getInstance());
    }

    @VisibleForTesting
    IngestCleanupRequestValidationPlugin(
        ProcessingManagementClientFactory processingManagementClientFactory,
        LogbookOperationsClientFactory logbookOperationsClientFactory
    ) {
        this.processingManagementClientFactory = processingManagementClientFactory;
        this.logbookOperationsClientFactory = logbookOperationsClientFactory;
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler) throws ProcessingException {
        try {
            String ingestOperationId = param.getParameterValue(WorkerParameterName.ingestOperationIdToCleanup);
            checkIngestOperationId(ingestOperationId);

            LOGGER.info("Ingest cleanup request validation succeeded");
            return buildItemStatus(INGEST_CLEANUP_REQUEST_VALIDATION, StatusCode.OK);
        } catch (ProcessingStatusException e) {
            LOGGER.error(
                String.format("Ingest cleanup request validation failed with status [%s]", e.getStatusCode()),
                e
            );
            return buildItemStatus(INGEST_CLEANUP_REQUEST_VALIDATION, e.getStatusCode(), e.getEventDetails());
        }
    }

    private void checkIngestOperationId(String ingestOperationId) throws ProcessingStatusException {
        LogbookOperation logbookOperation = getLogbookOperation(ingestOperationId);

        checkOperationType(ingestOperationId, logbookOperation);

        ProcessDetail processDetails = getProcessDetails(ingestOperationId);

        checkProcessCompletion(processDetails, ingestOperationId);

        StatusCode statusCode = getStatusCode(logbookOperation, processDetails);

        ensureWorkflowFailed(ingestOperationId, statusCode);
    }

    private LogbookOperation getLogbookOperation(String ingestOperationId) throws ProcessingStatusException {
        try (LogbookOperationsClient logbookOperationsClient = logbookOperationsClientFactory.getClient()) {
            JsonNode jsonNode = logbookOperationsClient.selectOperationById(ingestOperationId);
            return JsonHandler.getFromJsonNode(jsonNode.get(TAG_RESULTS).get(0), LogbookOperation.class);
        } catch (LogbookClientNotFoundException e) {
            throw new ProcessingStatusException(StatusCode.KO, "Logbook operation not found " + ingestOperationId);
        } catch (InvalidParseOperationException | LogbookClientException e) {
            throw new ProcessingStatusException(
                StatusCode.FATAL,
                "Could load logbook operation " + ingestOperationId,
                e
            );
        }
    }

    private void checkOperationType(String ingestOperationId, LogbookOperation logbookOperation)
        throws ProcessingStatusException {
        if (!logbookOperation.getEvTypeProc().equals(LogbookTypeProcess.INGEST.name())) {
            throw new ProcessingStatusException(
                StatusCode.KO,
                "Expected INGEST operation, found " +
                logbookOperation.getEvTypeProc() +
                " for operation " +
                ingestOperationId
            );
        }
    }

    private ProcessDetail getProcessDetails(String ingestOperationId) throws ProcessingStatusException {
        try (ProcessingManagementClient processingManagementClient = processingManagementClientFactory.getClient()) {
            ProcessQuery query = new ProcessQuery();
            query.setId(ingestOperationId);
            RequestResponse<ProcessDetail> processDetailRequestResponse =
                processingManagementClient.listOperationsDetails(query);
            if (!processDetailRequestResponse.isOk()) {
                VitamError error = (VitamError) processDetailRequestResponse;
                throw new ProcessingStatusException(
                    StatusCode.FATAL,
                    "Could not check active processes " + error.getDescription() + " - " + error.getMessage()
                );
            }

            List<ProcessDetail> processDetails =
                ((RequestResponseOK<ProcessDetail>) processDetailRequestResponse).getResults();
            if (processDetails.isEmpty()) {
                LOGGER.info("No active workflow... Operation is COMPLETED");
                return null;
            }

            return processDetails.get(0);
        } catch (VitamClientException | IllegalArgumentException e) {
            throw new ProcessingStatusException(StatusCode.FATAL, "Could not check active processes", e);
        }
    }

    private void checkProcessCompletion(ProcessDetail processDetails, String ingestOperationId)
        throws ProcessingStatusException {
        if (processDetails == null) {
            LOGGER.info("Process not found --> already COMPLETED & purged from processing manager");
            return;
        }

        ProcessState processState = ProcessState.valueOf(processDetails.getGlobalState());
        if (processState != ProcessState.COMPLETED) {
            throw new ProcessingStatusException(
                StatusCode.KO,
                "Process " + ingestOperationId + " is not yet COMPLETED"
            );
        }
    }

    private StatusCode getStatusCode(LogbookOperation logbookOperation, ProcessDetail processDetails) {
        // Get status code from process details, otherwise parse it from logbook operation
        if (processDetails != null) {
            return StatusCode.valueOf(processDetails.getStepStatus());
        }
        if (CollectionUtils.isEmpty(logbookOperation.getEvents())) {
            return null;
        }
        return Lists.reverse(logbookOperation.getEvents())
            .stream()
            .filter(e -> e.getEvType().equals(logbookOperation.getEvType()))
            .findFirst()
            .map(LogbookEvent::getOutcome)
            .map(StatusCode::valueOf)
            .orElse(null);
    }

    private void ensureWorkflowFailed(String ingestOperationId, StatusCode statusCode)
        throws ProcessingStatusException {
        if (statusCode == null) {
            LOGGER.info("Process " + ingestOperationId + " completed without final result (aborted?)");
            return;
        }
        if (statusCode.isGreaterOrEqualToKo()) {
            LOGGER.info("Process " + ingestOperationId + " completed with errors");
            return;
        }
        throw new ProcessingStatusException(
            StatusCode.KO,
            "Process " + ingestOperationId + " did not fail (" + statusCode + ")"
        );
    }
}
