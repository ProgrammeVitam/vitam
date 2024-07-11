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

package fr.gouv.vitam.worker.core.plugin.traceability;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.batch.report.model.OperationSummary;
import fr.gouv.vitam.batch.report.model.Report;
import fr.gouv.vitam.batch.report.model.ReportResults;
import fr.gouv.vitam.batch.report.model.ReportSummary;
import fr.gouv.vitam.batch.report.model.ReportType;
import fr.gouv.vitam.batch.report.model.entry.TraceabilityReportEntry;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.WorkspaceConstants;
import fr.gouv.vitam.common.model.logbook.LogbookEvent;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameterName;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.distribution.JsonLineGenericIterator;
import fr.gouv.vitam.worker.core.distribution.JsonLineModel;
import fr.gouv.vitam.worker.core.exception.ProcessingStatusException;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.plugin.traceability.service.TraceabilityReportService;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static fr.gouv.vitam.worker.core.plugin.traceability.TraceabilityLinkedCheckPreparePlugin.LOGBOOK_OPERATIONS_JSONL_FILE;
import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;

public class TraceabilityFinalizationPlugin extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(TraceabilityFinalizationPlugin.class);
    private static final String PLUGIN_NAME = "TRACEABILITY_FINALIZATION";
    private static final String QUERY = "query";
    private static final String OBJECT_ID = "objectId";

    private final TraceabilityReportService traceabilityReportService;
    private final LogbookOperationsClientFactory logbookOperationsClientFactory;

    @SuppressWarnings("unused")
    public TraceabilityFinalizationPlugin() {
        this(new TraceabilityReportService(), LogbookOperationsClientFactory.getInstance());
    }

    @VisibleForTesting
    TraceabilityFinalizationPlugin(
        TraceabilityReportService traceabilityReportService,
        LogbookOperationsClientFactory logbookOperationsClientFactory
    ) {
        this.traceabilityReportService = traceabilityReportService;
        this.logbookOperationsClientFactory = logbookOperationsClientFactory;
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler) throws ProcessingException {
        try {
            storeReportEntriesToOffers(param, handler);

            generateTraceabilityReportToWorkspace(param, handler);

            storeReportToOffers(param.getContainerName());

            cleanupReport(param);

            LOGGER.info("Audit object finalization succeeded");
            return buildItemStatus(PLUGIN_NAME, StatusCode.OK, null);
        } catch (ProcessingStatusException e) {
            LOGGER.error(String.format("Audit object  finalization failed with status [%s]", e.getStatusCode()), e);
            return buildItemStatus(PLUGIN_NAME, e.getStatusCode(), null);
        }
    }

    private void storeReportEntriesToOffers(WorkerParameters param, HandlerIO handler) throws ProcessingException {
        try {
            if (handler.isExistingFileInWorkspace(LOGBOOK_OPERATIONS_JSONL_FILE)) {
                List<String> operationsId = new ArrayList<>();
                JsonLineGenericIterator<JsonLineModel> iterator = new JsonLineGenericIterator<>(
                    handler.getInputStreamFromWorkspace(LOGBOOK_OPERATIONS_JSONL_FILE),
                    new TypeReference<>() {}
                );
                iterator.forEachRemaining(lineModel -> operationsId.add(lineModel.getId()));

                List<TraceabilityReportEntry> reports = operationsId
                    .stream()
                    .map(operationId -> {
                        try {
                            return handler.getJsonFromWorkspace(
                                operationId + File.separator + WorkspaceConstants.REPORT
                            );
                        } catch (ProcessingException e) {
                            LOGGER.error(e);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .map(jsonNode -> {
                        try {
                            return JsonHandler.getFromJsonNode(jsonNode, TraceabilityReportEntry.class);
                        } catch (InvalidParseOperationException e) {
                            LOGGER.error(e);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

                traceabilityReportService.appendEntries(param.getContainerName(), reports);
            }
        } catch (
            ContentAddressableStorageServerException
            | ContentAddressableStorageNotFoundException
            | ProcessingStatusException
            | IOException e
        ) {
            throw new ProcessingException(e);
        }
    }

    private void generateTraceabilityReportToWorkspace(WorkerParameters param, HandlerIO handler)
        throws ProcessingStatusException, ProcessingException {
        if (traceabilityReportService.isReportWrittenInWorkspace(param.getContainerName())) {
            // Already stored in workspace (idempotency)
            return;
        }

        Map<WorkerParameterName, String> mapParameters = param.getMapParameters();
        JsonNode initialQuery = handler.getJsonFromWorkspace(WorkspaceConstants.QUERY);
        JsonNode logbookOperation = getLogbookOperation(param.getContainerName());

        OperationSummary operationSummary = computeLogbookInformation(param.getContainerName(), logbookOperation);
        ReportSummary reportSummary = computeReportSummary(logbookOperation);

        ObjectNode context = JsonHandler.createObjectNode();

        if (mapParameters.containsKey(WorkerParameterName.objectId)) {
            context.put(OBJECT_ID, mapParameters.get(WorkerParameterName.objectId));
        }
        context.set(QUERY, initialQuery);

        Report reportInfo = new Report(operationSummary, reportSummary, context);
        traceabilityReportService.storeReportToWorkspace(reportInfo);
    }

    private OperationSummary computeLogbookInformation(String processId, JsonNode logbookOperation)
        throws ProcessingStatusException {
        try {
            if (!(logbookOperation.has("events") && logbookOperation.get("events").isArray())) {
                throw new ProcessingStatusException(StatusCode.FATAL, "Could not generate report summary : no events");
            }

            ArrayNode events = (ArrayNode) logbookOperation.get("events");
            if (events.size() <= 2) {
                throw new ProcessingStatusException(
                    StatusCode.FATAL,
                    "Could not generate report summary : not enougth events"
                );
            }
            JsonNode lastEvent = events.get(events.size() - 2);
            Integer tenantId = logbookOperation.get(VitamDocument.TENANT_ID).asInt();
            String evType = logbookOperation.get(LogbookEvent.EV_TYPE).asText();
            String outcome = lastEvent.get(LogbookEvent.OUTCOME).asText();
            String outDetail = lastEvent.get(LogbookEvent.OUT_DETAIL).asText();
            String outMsg = lastEvent.get(LogbookEvent.OUT_MESSG).asText();
            JsonNode evDetData = JsonHandler.getFromString(lastEvent.get(LogbookEvent.EV_DET_DATA).asText());
            JsonNode rSI = JsonHandler.getFromString(
                logbookOperation.get(LogbookEvent.RIGHTS_STATEMENT_IDENTIFIER).asText()
            );
            return new OperationSummary(tenantId, processId, evType, outcome, outDetail, outMsg, rSI, evDetData);
        } catch (InvalidParseOperationException e) {
            throw new ProcessingStatusException(StatusCode.FATAL, "Could not generate report", e);
        }
    }

    private ReportSummary computeReportSummary(JsonNode logbookOperation) {
        String startDate = logbookOperation.get(LogbookEvent.EV_DATE_TIME).asText();
        String endDate = LocalDateUtil.getString(LocalDateUtil.now());
        ReportType reportType = ReportType.TRACEABILITY;
        ReportResults vitamResults = new ReportResults();
        ObjectNode extendedInfo = JsonHandler.createObjectNode();
        return new ReportSummary(startDate, endDate, reportType, vitamResults, extendedInfo);
    }

    private JsonNode getLogbookOperation(String operationId) throws ProcessingStatusException {
        try (LogbookOperationsClient client = logbookOperationsClientFactory.getClient()) {
            JsonNode logbookResponse = client.selectOperationById(operationId);
            if (
                logbookResponse.has(RequestResponseOK.TAG_RESULTS) &&
                logbookResponse.get(RequestResponseOK.TAG_RESULTS).isArray()
            ) {
                ArrayNode results = (ArrayNode) logbookResponse.get(RequestResponseOK.TAG_RESULTS);
                if (results.size() > 0) {
                    return results.get(0);
                }
            }
            throw new ProcessingStatusException(StatusCode.FATAL, "Could not find operation in logbook");
        } catch (LogbookClientException | InvalidParseOperationException e) {
            throw new ProcessingStatusException(StatusCode.FATAL, "Error while retrieving logbook operation", e);
        }
    }

    private void storeReportToOffers(String containerName) throws ProcessingStatusException {
        traceabilityReportService.storeReportToOffers(containerName);
    }

    private void cleanupReport(WorkerParameters param) throws ProcessingStatusException {
        traceabilityReportService.cleanupReport(param.getContainerName());
    }
}
