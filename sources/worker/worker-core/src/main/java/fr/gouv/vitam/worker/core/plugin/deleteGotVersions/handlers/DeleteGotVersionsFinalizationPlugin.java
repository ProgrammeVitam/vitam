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

package fr.gouv.vitam.worker.core.plugin.deleteGotVersions.handlers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.batch.report.client.BatchReportClient;
import fr.gouv.vitam.batch.report.client.BatchReportClientFactory;
import fr.gouv.vitam.batch.report.model.OperationSummary;
import fr.gouv.vitam.batch.report.model.Report;
import fr.gouv.vitam.batch.report.model.ReportResults;
import fr.gouv.vitam.batch.report.model.ReportSummary;
import fr.gouv.vitam.batch.report.model.ReportType;
import fr.gouv.vitam.batch.report.model.entry.DeleteGotVersionsReportEntry;
import fr.gouv.vitam.batch.report.model.entry.ObjectGroupToDeleteReportEntry;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.DeleteGotVersionsRequest;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.logbook.LogbookEventOperation;
import fr.gouv.vitam.common.model.logbook.LogbookOperation;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.exception.ProcessingStatusException;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.plugin.deleteGotVersions.services.DeleteGotVersionsReportService;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.json.JsonHandler.getFromJsonNode;
import static fr.gouv.vitam.common.model.StatusCode.FATAL;
import static fr.gouv.vitam.common.model.StatusCode.KO;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.common.model.StatusCode.WARNING;
import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;

public class DeleteGotVersionsFinalizationPlugin extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(DeleteGotVersionsFinalizationPlugin.class);

    private static final String DELETE_GOT_VERSIONS_FINALIZATION = "DELETE_GOT_VERSIONS_FINALIZATION";

    private final DeleteGotVersionsReportService deleteGotVersionsReportService;
    private final LogbookOperationsClient logbookOperationsClient;
    private final BatchReportClientFactory batchReportClientFactory;

    public DeleteGotVersionsFinalizationPlugin() {
        this(new DeleteGotVersionsReportService(), LogbookOperationsClientFactory.getInstance().getClient(),
            BatchReportClientFactory.getInstance());
    }

    @VisibleForTesting
    public DeleteGotVersionsFinalizationPlugin(
        DeleteGotVersionsReportService deleteGotVersionsReportService,
        LogbookOperationsClient logbookOperationsClient, BatchReportClientFactory batchReportClientFactory) {
        this.deleteGotVersionsReportService = deleteGotVersionsReportService;
        this.logbookOperationsClient = logbookOperationsClient;
        this.batchReportClientFactory = batchReportClientFactory;
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler) throws ProcessingException {
        try {
            storeReportToWorkspace(param, handler);
            storeReportToOffers(param.getContainerName());
            cleanupReport(param.getRequestId());
            return buildItemStatus(DELETE_GOT_VERSIONS_FINALIZATION, OK);
        } catch (Exception e) {
            LOGGER.error("Error on finalization", e);
            ObjectNode eventDetails = JsonHandler.createObjectNode();
            eventDetails.put("error", e.getMessage());
            return buildItemStatus(DELETE_GOT_VERSIONS_FINALIZATION, FATAL, eventDetails);
        }
    }

    private void storeReportToWorkspace(WorkerParameters param, HandlerIO handler)
        throws IOException, InvalidParseOperationException,
        ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException, LogbookClientException,
        ProcessingStatusException {

        if (deleteGotVersionsReportService.isReportWrittenInWorkspace(param.getContainerName())) {
            // Report already generated to workspace (idempotency)
            return;
        }

        try (InputStream inputRequest = handler.getInputStreamFromWorkspace("deleteGotVersionsRequest")) {
            // create operation Summary
            JsonNode result = logbookOperationsClient.selectOperationById(param.getContainerName());
            RequestResponseOK<JsonNode> logbookOperationVersionModelResponseOK =
                RequestResponseOK.getFromJsonNode(result);
            LogbookOperation logbookOperationVersionModel =
                JsonHandler
                    .getFromJsonNode(logbookOperationVersionModelResponseOK.getFirstResult(), LogbookOperation.class);

            // create operation Summary
            OperationSummary operationSummary =
                getOperationSummary(logbookOperationVersionModel, param.getContainerName());

            // create report Summary
            String startDate = logbookOperationVersionModel.getEvDateTime();
            String endDate = LocalDateUtil.getString(LocalDateUtil.now());
            ReportType reportType = ReportType.DELETE_GOT_VERSIONS;
            ReportResults vitamResults = new ReportResults();
            List<DeleteGotVersionsReportEntry> deleteGotVersionsReportEntries =
                getDeletedObjectGroups(param.getContainerName());

            if (deleteGotVersionsReportEntries.isEmpty()) {
                vitamResults.setNbKo(1);
                vitamResults.setNbOk(0);
                vitamResults.setNbWarning(0);
            } else {
                List<ObjectGroupToDeleteReportEntry> reportEntries = deleteGotVersionsReportEntries.stream()
                    .map(DeleteGotVersionsReportEntry::getObjectGroupGlobal)
                    .flatMap(List::stream)
                    .collect(Collectors.toList());

                List<StatusCode> statusList = reportEntries.stream()
                    .map(ObjectGroupToDeleteReportEntry::getStatus)
                    .collect(Collectors.toList());

                if (!statusList.isEmpty()) {
                    vitamResults.setNbOk(computeNbrOkOperations(reportEntries));
                    vitamResults.setNbKo(Collections.frequency(statusList, KO));
                    vitamResults.setNbWarning(Collections.frequency(statusList, WARNING));
                }
            }

            ObjectNode extendedInfo = JsonHandler.createObjectNode();
            ReportSummary reportSummary =
                new ReportSummary(startDate, endDate, reportType, vitamResults, extendedInfo);

            // Create context
            DeleteGotVersionsRequest deleteGotVersionsRequest =
                JsonHandler.getFromInputStream(inputRequest, DeleteGotVersionsRequest.class);
            JsonNode context = JsonHandler.toJsonNode(deleteGotVersionsRequest);

            Report reportInfo = new Report(operationSummary, reportSummary, context);
            deleteGotVersionsReportService.storeReportToWorkspace(reportInfo);
        }
    }

    private OperationSummary getOperationSummary(LogbookOperation logbook, String processId)
        throws InvalidParseOperationException {
        List<LogbookEventOperation> events = logbook.getEvents();
        List<String> outcomes = events.stream().map(LogbookEventOperation::getOutcome).collect(Collectors.toList());
        int nbrKo = Collections.frequency(outcomes, KO.name());
        int nbrWarning = Collections.frequency(outcomes, WARNING.name());
        StatusCode globalOutcome = nbrKo > 0 ? KO : nbrWarning > 0 ? WARNING : OK;
        LogbookEventOperation referenceEvent = events.get(events.size() - 2);
        if (!globalOutcome.equals(OK)) {
            // Get the action's event that generates the status ( PS : bypass the first result because its the step's event )
            List<LogbookEventOperation> referenceEventsOfOutcome =
                events.stream().filter(elmt -> elmt.getOutcome().equals(globalOutcome.name()))
                    .collect(Collectors.toList());
            referenceEvent = referenceEventsOfOutcome.get(1);
        }
        JsonNode rSI = StringUtils.isNotBlank(logbook.getRightsStatementIdentifier())
            ? JsonHandler.getFromString(logbook.getRightsStatementIdentifier())
            : JsonHandler.createObjectNode();

        JsonNode evDetData = Objects.isNull(referenceEvent.getEvDetData())
            ? JsonHandler.createObjectNode()
            : JsonHandler.getFromString(referenceEvent.getEvDetData());

        return new OperationSummary(
            VitamThreadUtils.getVitamSession().getTenantId(),
            processId,
            referenceEvent.getEvType(),
            referenceEvent.getOutcome(),
            referenceEvent.getOutDetail(),
            referenceEvent.getOutMessg(),
            rSI,
            evDetData
        );
    }

    private int computeNbrOkOperations(List<ObjectGroupToDeleteReportEntry> reportEntries) {
        return (int) reportEntries.stream().filter(elmt -> elmt.getStatus().equals(OK))
            .map(ObjectGroupToDeleteReportEntry::getDeletedVersions).mapToLong(List::size).sum();
    }

    private void storeReportToOffers(String processId) throws ProcessingStatusException {
        deleteGotVersionsReportService.storeReportToOffers(processId);
    }

    private void cleanupReport(String processId) throws ProcessingStatusException {
        deleteGotVersionsReportService.cleanupReport(processId);
    }

    private List<DeleteGotVersionsReportEntry> getDeletedObjectGroups(String operationId)
        throws ProcessingStatusException {
        try (BatchReportClient client = batchReportClientFactory.getClient()) {
            JsonNode resultsNode = client.readDeletedGotVersionsReport(ReportType.DELETE_GOT_VERSIONS, operationId);
            return getFromJsonNode(resultsNode, new TypeReference<>() {
            });
        } catch (Exception e) {
            throw new ProcessingStatusException(StatusCode.FATAL, "Could not find entries from report!", e);
        }
    }
}
