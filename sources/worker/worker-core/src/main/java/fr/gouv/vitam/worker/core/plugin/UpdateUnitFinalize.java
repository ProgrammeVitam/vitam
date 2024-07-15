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
package fr.gouv.vitam.worker.core.plugin;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.batch.report.client.BatchReportClient;
import fr.gouv.vitam.batch.report.client.BatchReportClientFactory;
import fr.gouv.vitam.batch.report.model.OperationSummary;
import fr.gouv.vitam.batch.report.model.Report;
import fr.gouv.vitam.batch.report.model.ReportResults;
import fr.gouv.vitam.batch.report.model.ReportSummary;
import fr.gouv.vitam.batch.report.model.ReportType;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.logbook.LogbookEvent;
import fr.gouv.vitam.common.model.logbook.LogbookEventOperation;
import fr.gouv.vitam.common.model.logbook.LogbookOperation;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageAlreadyExistsClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.utils.PluginHelper.EventDetails;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static fr.gouv.vitam.batch.report.model.ReportType.UPDATE_UNIT;
import static fr.gouv.vitam.common.model.StatusCode.FATAL;
import static fr.gouv.vitam.common.model.StatusCode.KO;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.common.model.StatusCode.WARNING;
import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;

/**
 * Generate the report :<br>
 * - compute data from batch-report<br>
 * - store the report file<br>
 * - clean the batch-report data<br>
 */
public abstract class UpdateUnitFinalize extends ActionHandler {

    static final String JSONL_EXTENSION = ".jsonl";
    static final String WORKSPACE_REPORT_URI = "report.jsonl";

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(UpdateUnitFinalize.class);

    private static final String DETAILS = " Detail= ";

    private final BatchReportClientFactory batchReportClientFactory;
    private final LogbookOperationsClientFactory logbookOperationsClientFactory;
    private final StorageClientFactory storageClientFactory;

    public UpdateUnitFinalize() {
        this(
            BatchReportClientFactory.getInstance(),
            LogbookOperationsClientFactory.getInstance(),
            StorageClientFactory.getInstance()
        );
    }

    @VisibleForTesting
    protected UpdateUnitFinalize(
        BatchReportClientFactory batchReportClientFactory,
        LogbookOperationsClientFactory logbookOperationsClientFactory,
        StorageClientFactory storageClientFactory
    ) {
        this.batchReportClientFactory = batchReportClientFactory;
        this.logbookOperationsClientFactory = logbookOperationsClientFactory;
        this.storageClientFactory = storageClientFactory;
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler) throws ProcessingException {
        try {
            ReportResults reportResult = storeReportToWorkspace(param, handler);

            storeReportToOffers(param);

            cleanupReport(param);

            if (reportResult == null) {
                return buildItemStatus(
                    getPluginId(),
                    WARNING,
                    EventDetails.of(
                        getUpdateType() +
                        " report generation WARNING. Vitam results are absent, seems to be not logbook events..."
                    )
                );
            }

            if (reportResult.getNbKo() != 0 || reportResult.getNbWarning() != 0) {
                return buildItemStatus(
                    getPluginId(),
                    WARNING,
                    EventDetails.of(
                        getUpdateType() +
                        " report generation WARNING. Some update operations have a KO or WARNING status."
                    )
                );
            }

            if (reportResult.getTotal() == 0 || reportResult.getNbOk() == 0) {
                return buildItemStatus(
                    getPluginId(),
                    KO,
                    EventDetails.of(getUpdateType() + " report generation KO. No update done.")
                );
            }

            return buildItemStatus(getPluginId(), OK, EventDetails.of(getUpdateType() + " report generation OK."));
        } catch (
            LogbookClientException
            | VitamClientInternalException
            | StorageNotFoundClientException
            | StorageServerClientException
            | StorageAlreadyExistsClientException
            | InvalidParseOperationException e
        ) {
            LOGGER.error(e);
            return buildItemStatus(getPluginId(), FATAL, EventDetails.of("Client error when generating report."));
        }
    }

    private ReportResults storeReportToWorkspace(WorkerParameters param, HandlerIO handler)
        throws ProcessingException, InvalidParseOperationException, LogbookClientException, VitamClientInternalException {
        try (BatchReportClient batchReportClient = batchReportClientFactory.getClient()) {
            if (handler.isExistingFileInWorkspace(WORKSPACE_REPORT_URI)) {
                // Report already generated (idempotency)
                JsonNode report = handler.getJsonFromWorkspace(WORKSPACE_REPORT_URI);
                return JsonHandler.getFromJsonNode(report, Report.class).getReportSummary().getVitamResults();
            }

            Report reportInfo = generateReport(param);
            batchReportClient.storeReportToWorkspace(reportInfo);
            return reportInfo.getReportSummary().getVitamResults();
        }
    }

    protected ReportSummary getReport(LogbookOperation logbook) {
        Optional<LogbookEventOperation> logbookEvent = logbook
            .getEvents()
            .stream()
            .filter(e -> e.getEvType().startsWith(getUpdateActionKey()))
            .reduce((a, b) -> b);

        String startDate = logbook.getEvDateTime();
        String endDate = LocalDateUtil.nowFormatted();

        if (logbookEvent.isEmpty()) {
            return new ReportSummary(startDate, endDate, UPDATE_UNIT, null, null);
        }

        Map<StatusCode, Integer> codesNumber = getStatusStatistic(logbookEvent.get());
        int nbOk = codesNumber.get(OK) == null ? 0 : codesNumber.get(OK);
        int nbKo = codesNumber.get(KO) == null ? 0 : codesNumber.get(KO);
        int nbWarning = codesNumber.get(WARNING) == null ? 0 : codesNumber.get(WARNING);

        ReportResults results = new ReportResults(nbOk, nbKo, nbWarning);
        return new ReportSummary(startDate, endDate, UPDATE_UNIT, results, null);
    }

    private Report generateReport(WorkerParameters param)
        throws InvalidParseOperationException, LogbookClientException {
        LogbookOperation logbook = getLogbookInformation(param);
        OperationSummary operationSummary = getOperationSummary(logbook, param.getContainerName());
        ReportSummary reportSummary = getReport(logbook);
        // Agregate status of logbook operations when ko occurs
        if (
            reportSummary.getVitamResults() != null &&
            reportSummary.getVitamResults().getNbKo() > 0 &&
            WARNING.name().equals(param.getWorkflowStatusKo())
        ) {
            operationSummary.setOutcome(operationSummary.getOutcome().replace(KO.name(), param.getWorkflowStatusKo()));
            operationSummary.setOutDetail(
                operationSummary.getOutDetail().replace(KO.name(), param.getWorkflowStatusKo())
            );
        }
        JsonNode context = JsonHandler.createObjectNode();
        return new Report(operationSummary, reportSummary, context);
    }

    private void storeReportToOffers(WorkerParameters param)
        throws StorageNotFoundClientException, StorageServerClientException, StorageAlreadyExistsClientException {
        try (StorageClient storageClient = storageClientFactory.getClient()) {
            ObjectDescription description = new ObjectDescription();
            description.setWorkspaceContainerGUID(param.getContainerName());
            description.setWorkspaceObjectURI(WORKSPACE_REPORT_URI);
            storageClient.storeFileFromWorkspace(
                VitamConfiguration.getDefaultStrategy(),
                DataCategory.REPORT,
                param.getContainerName() + JSONL_EXTENSION,
                description
            );
        }
    }

    private void cleanupReport(WorkerParameters param) throws VitamClientInternalException {
        try (BatchReportClient batchReportClient = batchReportClientFactory.getClient()) {
            batchReportClient.cleanupReport(param.getContainerName(), getReportType());
        }
    }

    private LogbookOperation getLogbookInformation(WorkerParameters params)
        throws InvalidParseOperationException, LogbookClientException {
        try (LogbookOperationsClient logbookClient = logbookOperationsClientFactory.getClient()) {
            JsonNode response = logbookClient.selectOperationById(params.getContainerName());
            RequestResponseOK<JsonNode> logbookResponse = RequestResponseOK.getFromJsonNode(response);
            return JsonHandler.getFromJsonNode(logbookResponse.getFirstResult(), LogbookOperation.class);
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

    protected Map<StatusCode, Integer> getStatusStatistic(LogbookEvent logbookEvent) {
        String outMessg = logbookEvent.getOutMessg();
        if (StringUtils.isBlank(outMessg)) {
            return Collections.emptyMap();
        }
        String[] splitedMessage = outMessg.split(DETAILS);
        if (splitedMessage.length != 2) {
            return Collections.emptyMap();
        }
        return Stream.of(splitedMessage)
            .reduce((first, second) -> second)
            .map(
                last ->
                    Stream.of(last.split("\\s"))
                        .filter(StringUtils::isNotBlank)
                        .collect(
                            Collectors.toMap(
                                s -> StatusCode.valueOf(s.split(":")[0]),
                                s -> Integer.valueOf(s.split(":")[1])
                            )
                        )
            )
            .orElse(Collections.emptyMap());
    }

    protected abstract String getPluginId();

    protected abstract String getUpdateType();

    protected abstract String getUpdateActionKey();

    protected abstract ReportType getReportType();
}
