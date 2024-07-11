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
package fr.gouv.vitam.worker.core.plugin.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.batch.report.model.OperationSummary;
import fr.gouv.vitam.batch.report.model.Report;
import fr.gouv.vitam.batch.report.model.ReportResults;
import fr.gouv.vitam.batch.report.model.ReportSummary;
import fr.gouv.vitam.batch.report.model.ReportType;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.WorkspaceConstants;
import fr.gouv.vitam.common.model.logbook.LogbookEvent;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameterName;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.exception.ProcessingStatusException;
import fr.gouv.vitam.worker.core.handler.ActionHandler;

import java.util.Map;

import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;

/**
 * AuditFinalizePlugin.
 */
public class AuditFinalizePlugin extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AuditFinalizePlugin.class);
    private static final String OBJECT_AUDIT_FINALIZE = "REPORT_AUDIT";

    private final AuditReportService auditReportService;
    private final LogbookOperationsClientFactory logbookOperationsClientFactory;

    /**
     * Default constructor
     */
    public AuditFinalizePlugin() {
        this(new AuditReportService(), LogbookOperationsClientFactory.getInstance());
    }

    /***
     * Test only constructor
     */
    @VisibleForTesting
    AuditFinalizePlugin(
        AuditReportService auditReportService,
        LogbookOperationsClientFactory logbookOperationsClientFactory
    ) {
        this.auditReportService = auditReportService;
        this.logbookOperationsClientFactory = logbookOperationsClientFactory;
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler) throws ProcessingException {
        try {
            generateAuditReportToWorkspace(param, handler);

            storeReportToOffers(param.getContainerName());

            cleanupReport(param);

            LOGGER.info("Audit object finalization succeeded");
            return buildItemStatus(OBJECT_AUDIT_FINALIZE, StatusCode.OK, null);
        } catch (ProcessingStatusException e) {
            LOGGER.error(String.format("Audit object  finalization failed with status [%s]", e.getStatusCode()), e);
            return buildItemStatus(OBJECT_AUDIT_FINALIZE, e.getStatusCode(), null);
        }
    }

    private void generateAuditReportToWorkspace(WorkerParameters param, HandlerIO handler)
        throws ProcessingStatusException, ProcessingException {
        if (auditReportService.isReportWrittenInWorkspace(param.getContainerName())) {
            // Already stored in workspace (idempotency)
            return;
        }

        Map<WorkerParameterName, String> mapParameters = param.getMapParameters();
        JsonNode initialQuery = handler.getJsonFromWorkspace(WorkspaceConstants.QUERY);
        JsonNode logbookOperation = getLogbookOperation(param.getContainerName());

        OperationSummary operationSummary = computeLogbookInformation(param.getContainerName(), logbookOperation);
        ReportSummary reportSummary = computeReportSummary(logbookOperation);

        ObjectNode context = JsonHandler.createObjectNode();
        context.put(WorkerParameterName.auditActions.name(), mapParameters.get(WorkerParameterName.auditActions));
        context.put("auditType", mapParameters.get(WorkerParameterName.auditType));
        if (mapParameters.containsKey(WorkerParameterName.objectId)) {
            context.put("objectId", mapParameters.get(WorkerParameterName.objectId));
        }
        context.set("query", initialQuery);

        Report reportInfo = new Report(operationSummary, reportSummary, context);
        auditReportService.storeReportToWorkspace(reportInfo);
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
            Integer tenantId = VitamThreadUtils.getVitamSession().getTenantId();
            String evId = processId;
            String evType = logbookOperation.get(LogbookEvent.EV_TYPE).asText();
            String outcome = lastEvent.get(LogbookEvent.OUTCOME).asText();
            String outDetail = lastEvent.get(LogbookEvent.OUT_DETAIL).asText();
            String outMsg = lastEvent.get(LogbookEvent.OUT_MESSG).asText();
            JsonNode evDetData = JsonHandler.getFromString(lastEvent.get(LogbookEvent.EV_DET_DATA).asText());
            JsonNode rSI = JsonHandler.getFromString(
                logbookOperation.get(LogbookEvent.RIGHTS_STATEMENT_IDENTIFIER).asText()
            );
            OperationSummary operationSummary = new OperationSummary(
                tenantId,
                evId,
                evType,
                outcome,
                outDetail,
                outMsg,
                rSI,
                evDetData
            );
            return operationSummary;
        } catch (InvalidParseOperationException e) {
            throw new ProcessingStatusException(StatusCode.FATAL, "Could not generate report", e);
        }
    }

    private ReportSummary computeReportSummary(JsonNode logbookOperation) {
        String startDate = logbookOperation.get(LogbookEvent.EV_DATE_TIME).asText();
        String endDate = LocalDateUtil.getString(LocalDateUtil.now());
        ReportType reportType = ReportType.AUDIT;
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
        auditReportService.storeReportToOffers(containerName);
    }

    private void cleanupReport(WorkerParameters param) throws ProcessingStatusException {
        auditReportService.cleanupReport(param.getContainerName());
    }
}
