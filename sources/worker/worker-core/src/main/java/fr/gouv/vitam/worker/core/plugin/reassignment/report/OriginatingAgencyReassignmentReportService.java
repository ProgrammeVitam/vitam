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
package fr.gouv.vitam.worker.core.plugin.reassignment.report;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Iterators;
import fr.gouv.vitam.batch.report.model.OperationSummary;
import fr.gouv.vitam.batch.report.model.OriginatingAgencyReassignmentReportLine;
import fr.gouv.vitam.batch.report.model.ReportResults;
import fr.gouv.vitam.batch.report.model.ReportSummary;
import fr.gouv.vitam.batch.report.model.ReportType;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.jsonl.JsonLineIterator;
import fr.gouv.vitam.common.jsonl.JsonLineWriter;
import fr.gouv.vitam.common.model.OriginatingAgencyReassignmentRequest;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.logbook.LogbookEventOperation;
import fr.gouv.vitam.common.model.logbook.LogbookOperation;
import fr.gouv.vitam.common.model.processing.WorkFlowExecutionContext;
import fr.gouv.vitam.common.security.IllegalPathException;
import fr.gouv.vitam.common.security.SafeFileChecker;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.distribution.JsonLineModel;
import fr.gouv.vitam.worker.core.exception.ProcessingStatusException;
import fr.gouv.vitam.worker.core.plugin.CommonReportService;
import fr.gouv.vitam.worker.core.plugin.reassignment.ReassignmentStatistics;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import jakarta.validation.constraints.NotNull;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class OriginatingAgencyReassignmentReportService
    extends CommonReportService<OriginatingAgencyReassignmentReportLine> {

    private static final String WORKSPACE_REPORT_URI = "report.jsonl";

    private static final String UNITS_TO_UPDATE_FILE_NAME = "units_to_update.jsonl";
    private static final String OBJECT_GROUPS_TO_UPDATE_FILE_NAME = "object_groups_to_update_sp.jsonl";
    private static final String REASSIGNMENT_STATISTICS_JSON_FILE = "reassignment_statistics.json";
    public static final String OBJECT_GROUP_TYPE = "ObjectGroup";
    public static final String UNIT_TYPE = "Unit";

    public OriginatingAgencyReassignmentReportService() {
        super(ReportType.REASSIGNMENT_ORIGINATING_AGENCIES);
    }

    public void generateReassignmentReport(
        HandlerIO handler,
        WorkerParameters param,
        OriginatingAgencyReassignmentRequest originatingAgencyReassignmentRequest
    ) throws ProcessingStatusException {
        generateAndStoreReportToWorkspace(originatingAgencyReassignmentRequest, handler, param);

        storeReportToOffers(param.getContainerName());
    }

    private void generateAndStoreReportToWorkspace(
        OriginatingAgencyReassignmentRequest originatingAgencyReassignmentRequest,
        HandlerIO handler,
        WorkerParameters param
    ) throws ProcessingStatusException {
        String endDate = LocalDateUtil.nowFormatted();
        String processId = param.getContainerName();
        StatusCode workflowStatus = StatusCode.valueOf(param.getWorkflowStatusKo());
        try {
            ObjectNode context = getReportContext(originatingAgencyReassignmentRequest);

            LogbookOperation logbook = getLogbookInformation(handler, processId);

            String startDate = logbook.getEvDateTime();

            OperationSummary operationSummary = getOperationSummary(logbook, param.getContainerName());

            ReassignmentStatistics reassignmentStatistics = loadReassignmentStatistics(handler);

            ReportResults reportResults = new ReportResults(reassignmentStatistics.nbUnits(), 0, 0);
            JsonNode extendedInfo = JsonHandler.toJsonNode(reassignmentStatistics);
            ReportSummary reportSummary = new ReportSummary(
                startDate,
                endDate,
                ReportType.REASSIGNMENT_ORIGINATING_AGENCIES,
                reportResults,
                extendedInfo
            );

            File tempReport = null;
            boolean shouldGenerateReportDetail =
                StatusCode.OK.equals(workflowStatus) || StatusCode.WARNING.equals(workflowStatus);
            try {
                tempReport = createTemporaryFile(processId, WORKSPACE_REPORT_URI);

                try (
                    JsonLineWriter<OriginatingAgencyReassignmentReportLine> reportWriter = new JsonLineWriter<>(
                        new FileOutputStream(tempReport)
                    )
                ) {
                    reportWriter.addEntryObject(operationSummary);
                    if (shouldGenerateReportDetail) {
                        reportWriter.addEntryObject(reportSummary);
                    }

                    reportWriter.addEntryObject(context);

                    if (shouldGenerateReportDetail) {
                        //units
                        addUnitsToReassignmentReport(handler, originatingAgencyReassignmentRequest, reportWriter);
                        // Object groups
                        addObjectGroupsToReassignmentReport(
                            handler,
                            originatingAgencyReassignmentRequest,
                            reportWriter
                        );
                    }
                }

                storeFileToWorkspace(handler, processId, tempReport);
            } finally {
                FileUtils.deleteQuietly(tempReport);
            }
        } catch (
            IOException
            | ContentAddressableStorageNotFoundException
            | IllegalPathException
            | ContentAddressableStorageServerException
            | InvalidParseOperationException
            | LogbookClientException e
        ) {
            throw new ProcessingStatusException(StatusCode.FATAL, e.getMessage());
        }
    }

    private void addObjectGroupsToReassignmentReport(
        HandlerIO handler,
        OriginatingAgencyReassignmentRequest originatingAgencyReassignmentRequest,
        JsonLineWriter<OriginatingAgencyReassignmentReportLine> reportWriter
    ) throws IOException, ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException {
        File objectGroupsJsonlFile = handler.getFileFromWorkspace(
            WorkFlowExecutionContext.VITAM,
            OBJECT_GROUPS_TO_UPDATE_FILE_NAME
        );
        try (InputStream inputStream = new FileInputStream(objectGroupsJsonlFile)) {
            Iterator<List<JsonLineModel>> bulkLines = Iterators.partition(
                new JsonLineIterator<>(inputStream, new TypeReference<>() {}),
                VitamConfiguration.getBatchSize()
            );
            while (bulkLines.hasNext()) {
                List<JsonLineModel> objectGroupsToBatch = bulkLines.next();
                for (JsonLineModel jsonLineModel : objectGroupsToBatch) {
                    String objectGroupId = jsonLineModel.getId();

                    String initialOperation = jsonLineModel
                        .getParams()
                        .get(VitamFieldsHelper.initialOperation())
                        .asText();
                    OriginatingAgencyReassignmentReportLine originatingAgencyReassignmentObjectGroupReportLine =
                        new OriginatingAgencyReassignmentReportLine(
                            objectGroupId,
                            OBJECT_GROUP_TYPE,
                            initialOperation,
                            originatingAgencyReassignmentRequest.getSourceOriginatingAgency(),
                            originatingAgencyReassignmentRequest.getTargetOriginatingAgency()
                        );

                    reportWriter.addEntry(originatingAgencyReassignmentObjectGroupReportLine);
                }
            }
        }
    }

    private void addUnitsToReassignmentReport(
        HandlerIO handler,
        OriginatingAgencyReassignmentRequest originatingAgencyReassignmentRequest,
        JsonLineWriter<OriginatingAgencyReassignmentReportLine> reportWriter
    ) throws IOException, ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException {
        File unitsJsonlFile = handler.getFileFromWorkspace(WorkFlowExecutionContext.VITAM, UNITS_TO_UPDATE_FILE_NAME);
        try (InputStream inputStream = new FileInputStream(unitsJsonlFile)) {
            Iterator<List<JsonLineModel>> bulkLines = Iterators.partition(
                new JsonLineIterator<>(inputStream, new TypeReference<>() {}),
                VitamConfiguration.getBatchSize()
            );
            while (bulkLines.hasNext()) {
                List<JsonLineModel> unitsToBatch = bulkLines.next();
                for (JsonLineModel jsonLineModel : unitsToBatch) {
                    String unitId = jsonLineModel.getId();
                    String originatingAgency = jsonLineModel
                        .getParams()
                        .get(VitamFieldsHelper.originatingAgency())
                        .asText();
                    String initialOperation = jsonLineModel
                        .getParams()
                        .get(VitamFieldsHelper.initialOperation())
                        .asText();
                    String objectId = null;
                    if (jsonLineModel.getParams().has(VitamFieldsHelper.object())) {
                        objectId = jsonLineModel.getParams().get(VitamFieldsHelper.object()).asText();
                    }

                    OriginatingAgencyReassignmentReportLine originatingAgencyReassignmentUnitReportLine =
                        new OriginatingAgencyReassignmentReportLine(
                            unitId,
                            UNIT_TYPE,
                            initialOperation,
                            originatingAgency,
                            originatingAgencyReassignmentRequest.getTargetOriginatingAgency(),
                            objectId
                        );

                    reportWriter.addEntry(originatingAgencyReassignmentUnitReportLine);
                }
            }
        }
    }

    private void storeFileToWorkspace(HandlerIO handlerIO, String processId, File file)
        throws IOException, ContentAddressableStorageServerException {
        try (
            WorkspaceClient workspaceClient = handlerIO.getWorkspaceClient(handlerIO.getWorkFlowExecutionContext());
            InputStream inputStream = new FileInputStream(file)
        ) {
            workspaceClient.putAtomicObject(processId, WORKSPACE_REPORT_URI, inputStream, file.length());
        }
    }

    private OperationSummary getOperationSummary(LogbookOperation logbook, String processId)
        throws InvalidParseOperationException {
        List<LogbookEventOperation> events = logbook.getEvents();
        LogbookEventOperation lastEvent = events.get(events.size() - 2);

        JsonNode rSI = StringUtils.isNotBlank(logbook.getRightsStatementIdentifier())
            ? JsonHandler.getFromString(logbook.getRightsStatementIdentifier())
            : JsonHandler.createObjectNode();

        JsonNode evDetData = Objects.isNull(lastEvent.getEvDetData())
            ? JsonHandler.createObjectNode()
            : JsonHandler.getFromString(lastEvent.getEvDetData());

        return new OperationSummary(
            VitamThreadUtils.getVitamSession().getTenantId(),
            processId,
            lastEvent.getEvType(),
            lastEvent.getOutcome(),
            lastEvent.getOutDetail(),
            lastEvent.getOutMessg(),
            rSI,
            evDetData
        );
    }

    private LogbookOperation getLogbookInformation(HandlerIO handlerIO, String operationId)
        throws InvalidParseOperationException, LogbookClientException {
        try (LogbookOperationsClient client = handlerIO.getLogbookOperationsClient()) {
            JsonNode response = client.selectOperationById(operationId);
            RequestResponseOK<JsonNode> logbookResponse = RequestResponseOK.getFromJsonNode(response);
            return JsonHandler.getFromJsonNode(logbookResponse.getFirstResult(), LogbookOperation.class);
        }
    }

    @NotNull
    private File createTemporaryFile(@NotNull String processId, @NotNull String filename)
        throws IOException, IllegalPathException {
        File file = SafeFileChecker.checkSafeFileSubPaths(VitamConfiguration.getVitamTmpFolder(), processId, filename);
        FileUtils.forceMkdirParent(file);
        return file;
    }

    private ObjectNode getReportContext(OriginatingAgencyReassignmentRequest originatingAgencyReassignmentRequest)
        throws InvalidParseOperationException {
        ObjectNode context = JsonHandler.createObjectNode();
        context.set("query", JsonHandler.toJsonNode(originatingAgencyReassignmentRequest));
        return context;
    }

    private ReassignmentStatistics loadReassignmentStatistics(HandlerIO handler)
        throws InvalidParseOperationException, ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException, IOException {
        File reassignmentStatisticsFile = handler.getFileFromWorkspace(
            WorkFlowExecutionContext.VITAM,
            REASSIGNMENT_STATISTICS_JSON_FILE
        );
        return JsonHandler.getFromFile(reassignmentStatisticsFile, ReassignmentStatistics.class);
    }
}
