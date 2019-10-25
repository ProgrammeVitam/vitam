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
package fr.gouv.vitam.worker.core.plugin.evidence;

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
import fr.gouv.vitam.common.model.logbook.LogbookEventOperation;
import fr.gouv.vitam.common.model.logbook.LogbookOperation;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.BackupService;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.plugin.evidence.exception.EvidenceAuditException;
import fr.gouv.vitam.worker.core.plugin.evidence.exception.EvidenceStatus;
import fr.gouv.vitam.worker.core.plugin.evidence.report.EvidenceAuditReportLine;
import fr.gouv.vitam.worker.core.plugin.evidence.report.EvidenceAuditReportService;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static fr.gouv.vitam.common.json.JsonHandler.unprettyPrint;
import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;


/**
 * EvidenceAuditFinalize class
 */
public class EvidenceAuditFinalize extends ActionHandler {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(EvidenceAuditFinalize.class);

    private static final String EVIDENCE_AUDIT_FINALIZE = "EVIDENCE_AUDIT_FINALIZE";
    BackupService backupService = new BackupService();

    private EvidenceAuditReportService evidenceAuditReportService;
    private LogbookOperationsClientFactory logbookOperationsClientFactory;

    @VisibleForTesting
    EvidenceAuditFinalize(BackupService backupService) {
        this.backupService = backupService;
    }

    @VisibleForTesting
    EvidenceAuditFinalize(EvidenceAuditReportService evidenceAuditReportService,
        LogbookOperationsClientFactory logbookOperationsClientFactory) {
        this.evidenceAuditReportService = evidenceAuditReportService;
        this.logbookOperationsClientFactory = logbookOperationsClientFactory;
    }

    public EvidenceAuditFinalize() {
        this(new EvidenceAuditReportService(), LogbookOperationsClientFactory.getInstance());
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handlerIO)
        throws ProcessingException, ContentAddressableStorageServerException {
        ItemStatus itemStatus = new ItemStatus(EVIDENCE_AUDIT_FINALIZE);
        Report evidenceReport;
        try {
            evidenceReport = generateEvidenceAuditReport(param, handlerIO);
        } catch (EvidenceAuditException e) {
            LOGGER.error(String.format("Evidence Audit finalization failed with status %s", e.getStatus()), e);
            return buildItemStatus(EVIDENCE_AUDIT_FINALIZE, StatusCode.FATAL, null);
        }

        try {

            File reportFile = handlerIO.getNewLocalFile("report.json");
            List<URI> uriListObjectsWorkspace =
                handlerIO.getUriList(handlerIO.getContainerName(), param.getObjectName());

            try (FileOutputStream fileOutputStream = new FileOutputStream(reportFile);
                OutputStreamWriter buffWriter = new OutputStreamWriter(new BufferedOutputStream(fileOutputStream),
                    StandardCharsets.UTF_8);
            ) {

                buffWriter.write(unprettyPrint(evidenceReport));
                for (URI uri : uriListObjectsWorkspace) {

                    File file = handlerIO.getFileFromWorkspace(param.getObjectName() + File.separator + uri.getPath());

                    EvidenceAuditReportLine reportLine = JsonHandler.getFromFile(file, EvidenceAuditReportLine.class);

                    switch (reportLine.getEvidenceStatus()) {
                        case OK:
                            itemStatus.increment(StatusCode.OK);
                            break;
                        case WARN:
                            itemStatus.increment(StatusCode.WARNING);
                            break;
                        case KO:
                            itemStatus.increment(StatusCode.KO);
                            break;
                        case FATAL:
                            itemStatus.increment(StatusCode.FATAL);
                            break;
                        default:
                            throw new IllegalStateException("Invalid status " + reportLine.getEvidenceStatus());
                    }
                }
                buffWriter.flush();
            }

        } catch (ContentAddressableStorageNotFoundException | IOException | InvalidParseOperationException e) {
            throw new ProcessingException(e);

        }

        if (itemStatus.getGlobalStatus().isGreaterOrEqualToFatal()) {
            itemStatus.increment(StatusCode.FATAL);
            ObjectNode infoNode = JsonHandler.createObjectNode();
            infoNode.put("Message", "There  some audits fails see the report for more details");
            itemStatus.setEvDetailData(unprettyPrint(infoNode));
            try {
                evidenceAuditReportService.storeReport(evidenceReport);
            } catch (EvidenceAuditException e) {
                throw new ProcessingException(e);
            }
            cleanup(param.getContainerName());
            return new ItemStatus(EVIDENCE_AUDIT_FINALIZE).setItemsStatus(EVIDENCE_AUDIT_FINALIZE, itemStatus);
        }

        if (itemStatus.getGlobalStatus().isGreaterOrEqualToKo()) {
            itemStatus.increment(StatusCode.KO);
            ObjectNode infoNode = JsonHandler.createObjectNode();
            infoNode.put("Message", "There some objects not securised yet see the report for more details ");
            itemStatus.setEvDetailData(unprettyPrint(infoNode));
            try {
                evidenceAuditReportService.storeReport(evidenceReport);
            } catch (EvidenceAuditException e) {
                throw new ProcessingException(e);
            }
            cleanup(param.getContainerName());
            return new ItemStatus(EVIDENCE_AUDIT_FINALIZE).setItemsStatus(EVIDENCE_AUDIT_FINALIZE, itemStatus);
        }
        if (itemStatus.getGlobalStatus().isGreaterOrEqualToWarn()) {
            itemStatus.increment(StatusCode.WARNING);
            ObjectNode infoNode = JsonHandler.createObjectNode();
            infoNode.put("Message", "There are some objects not securised yet see the report for more details ");
            itemStatus.setEvDetailData(JsonHandler.unprettyPrint(infoNode));
        } else {
            LOGGER.info("EvidenceAudit finalization terminated Successfully");
            itemStatus.increment(StatusCode.OK);
        }

        try {
            evidenceAuditReportService.storeReport(evidenceReport);
        } catch (EvidenceAuditException e) {
            throw new ProcessingException(e);
        }
        cleanup(param.getContainerName());

        return new ItemStatus(EVIDENCE_AUDIT_FINALIZE).setItemsStatus(EVIDENCE_AUDIT_FINALIZE, itemStatus);
    }

    private void cleanup(String container) {
        try {
            evidenceAuditReportService.cleanupReport(container);
        } catch (EvidenceAuditException e) {
            LOGGER.error(String.format("Evidence Audit finalization failed with status [%s]", e.getStatus()), e);
        }
    }

    private Report generateEvidenceAuditReport(WorkerParameters param, HandlerIO handler)
        throws EvidenceAuditException, ProcessingException {
        JsonNode initialQuery = handler.getJsonFromWorkspace("query.json");
        JsonNode logbookOperation = getLogbookOperation(param.getContainerName());
        LogbookOperation logbookOperationClass = null;
        try {
            logbookOperationClass = getLogbookInformation(param.getContainerName());
        } catch (InvalidParseOperationException | LogbookClientException e) {
            throw new EvidenceAuditException(EvidenceStatus.FATAL, "Error while retrieving logbook operation", e);
        }

        OperationSummary operationSummary = computeLogbookInformation(param.getContainerName(), logbookOperationClass);
        ReportSummary reportSummary = null;
        reportSummary = computeReportSummary(logbookOperationClass);

        ObjectNode context = JsonHandler.createObjectNode();
        context.set("query", initialQuery);

        return new Report(operationSummary, reportSummary, context);
    }

    private OperationSummary computeLogbookInformation(String processId, LogbookOperation logbookOperation)
        throws EvidenceAuditException {
        try {
            if ((logbookOperation.getEvents().isEmpty())) {
                throw new EvidenceAuditException(EvidenceStatus.FATAL, "Could not generate report summary : no events");
            }

            List<LogbookEventOperation> events = logbookOperation.getEvents();
            if (events.size() <= 2) {
                throw new EvidenceAuditException(EvidenceStatus.FATAL,
                    "Could not generate report summary : not enougth events");
            }
            LogbookEventOperation lastEvent = events.get(events.size() - 3);
            Integer tenantId = VitamThreadUtils.getVitamSession().getTenantId();
            String evId = processId;
            String evType = logbookOperation.getEvType();
            String outcome = lastEvent.getOutcome();
            String outDetail = lastEvent.getOutDetail();
            String outMsg = lastEvent.getOutMessg();
            JsonNode evDetData = JsonHandler.createObjectNode();
            if (lastEvent.getEvDetData() != null) {
                JsonHandler.getFromString(lastEvent.getEvDetData());
            }
            JsonNode rSI = JsonHandler.getFromString(logbookOperation.getRightsStatementIdentifier());
            OperationSummary operationSummary =
                new OperationSummary(tenantId, evId, evType, outcome, outDetail, outMsg, rSI,
                    evDetData);
            return operationSummary;
        } catch (InvalidParseOperationException e) {
            throw new EvidenceAuditException(EvidenceStatus.FATAL, "Could not generate report", e);
        }
    }

    private ReportSummary computeReportSummary(LogbookOperation logbookOperation) {
        String startDate = logbookOperation.getEvDateTime();
        String endDate = LocalDateUtil.getString(LocalDateTime.now());
        ReportType reportType = ReportType.EVIDENCE_AUDIT;
        ReportResults vitamResults = new ReportResults();
        ObjectNode extendedInfo = JsonHandler.createObjectNode();
        return new ReportSummary(startDate, endDate, reportType, vitamResults, extendedInfo);
    }

    private JsonNode getLogbookOperation(String operationId) throws EvidenceAuditException {
        try (LogbookOperationsClient client = logbookOperationsClientFactory.getClient()) {
            JsonNode logbookResponse = client.selectOperationById(operationId);
            if (logbookResponse.has("$results") && logbookResponse.get("$results").isArray()) {
                ArrayNode results = (ArrayNode) logbookResponse.get("$results");
                if (results.size() > 0) {
                    return results.get(0);
                }
            }
            throw new EvidenceAuditException(EvidenceStatus.FATAL, "Could not find operation in logbook");
        } catch (LogbookClientException | InvalidParseOperationException e) {
            throw new EvidenceAuditException(EvidenceStatus.FATAL, "Error while retrieving logbook operation", e);
        }
    }

    private LogbookOperation getLogbookInformation(String operationId)
        throws InvalidParseOperationException, LogbookClientException {
        try (LogbookOperationsClient client = logbookOperationsClientFactory.getClient()) {
            JsonNode response = client.selectOperationById(operationId);
            RequestResponseOK<JsonNode> logbookResponse = RequestResponseOK.getFromJsonNode(response);
            return JsonHandler.getFromJsonNode(logbookResponse.getFirstResult(), LogbookOperation.class);
        }
    }

}
