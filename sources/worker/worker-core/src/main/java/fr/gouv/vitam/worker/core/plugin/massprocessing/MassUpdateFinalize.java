/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.worker.core.plugin.massprocessing;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.batch.report.client.BatchReportClient;
import fr.gouv.vitam.batch.report.client.BatchReportClientFactory;
import fr.gouv.vitam.batch.report.model.OperationSummary;
import fr.gouv.vitam.batch.report.model.Report;
import fr.gouv.vitam.batch.report.model.ReportResults;
import fr.gouv.vitam.batch.report.model.ReportSummary;
import fr.gouv.vitam.common.LocalDateUtil;
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
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.utils.PluginHelper.EventDetails;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
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
import static fr.gouv.vitam.processing.engine.core.ProcessEngineImpl.DETAILS;
import static fr.gouv.vitam.worker.core.plugin.massprocessing.description.MassUpdateUnitsProcess.MASS_UPDATE_UNITS;
import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;

public class MassUpdateFinalize extends ActionHandler {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(MassUpdateFinalize.class);

    private static final String MASS_UPDATE_FINALIZE = "MASS_UPDATE_FINALIZE";

    private final BatchReportClientFactory batchReportClientFactory;
    private final LogbookOperationsClientFactory logbookOperationsClientFactory;

    public MassUpdateFinalize() {
        this(BatchReportClientFactory.getInstance(), LogbookOperationsClientFactory.getInstance());
    }

    @VisibleForTesting
    public MassUpdateFinalize(BatchReportClientFactory batchReportClientFactory, LogbookOperationsClientFactory logbookOperationsClientFactory) {
        this.batchReportClientFactory = batchReportClientFactory;
        this.logbookOperationsClientFactory = logbookOperationsClientFactory;
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler) throws ProcessingException, ContentAddressableStorageServerException {
        try (BatchReportClient batchReportClient = batchReportClientFactory.getClient();
             LogbookOperationsClient logbookClient = logbookOperationsClientFactory.getClient()) {

            LogbookOperation logbook = getLogbookInformation(param, logbookClient);
            OperationSummary operationSummary = getOperationSummary(logbook, param.getContainerName());
            ReportSummary reportSummary = getReport(logbook);
            JsonNode context = handler.getJsonFromWorkspace("query.json");

            Report reportInfo = new Report(operationSummary, reportSummary, context);
            batchReportClient.storeReport(reportInfo);
            batchReportClient.cleanupReport(param.getContainerName(), UPDATE_UNIT);

            return buildItemStatus(MASS_UPDATE_FINALIZE, OK, EventDetails.of("MassUpdate report generation OK."));
        } catch (LogbookClientException | VitamClientInternalException e) {
            LOGGER.error(e);
            return buildItemStatus(MASS_UPDATE_FINALIZE, FATAL, EventDetails.of("Client error when generating report."));
        }  catch (InvalidParseOperationException e) {
            LOGGER.error(e);
            return buildItemStatus(MASS_UPDATE_FINALIZE, KO, EventDetails.of("Generic error when generating report."));
        }
    }

    private LogbookOperation getLogbookInformation(WorkerParameters params, LogbookOperationsClient logbookClient) throws InvalidParseOperationException, LogbookClientException {
        JsonNode response = logbookClient.selectOperationById(params.getContainerName());
        RequestResponseOK<JsonNode> logbookResponse = RequestResponseOK.getFromJsonNode(response);
        return JsonHandler.getFromJsonNode(logbookResponse.getFirstResult(), LogbookOperation.class);
    }

    private OperationSummary getOperationSummary(LogbookOperation logbook, String processId) throws InvalidParseOperationException {
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

    private ReportSummary getReport(LogbookOperation logbook) {
        Optional<LogbookEventOperation> logbookEvent = logbook.getEvents().stream()
            .filter(e -> e.getEvType().equals(MASS_UPDATE_UNITS))
            .findFirst();

        String startDate = logbook.getEvDateTime();
        String endDate = LocalDateUtil.getString(LocalDateTime.now());

        if (!logbookEvent.isPresent()) {
            return new ReportSummary(startDate, endDate, UPDATE_UNIT, null, null);
        }

        Map<StatusCode, Integer> codesNumber = getStatusStatistic(logbookEvent.get());
        int total = codesNumber.values().stream().mapToInt(i -> i).sum();

        ReportResults results = new ReportResults(codesNumber.get(OK), codesNumber.get(KO), codesNumber.get(WARNING), total);
        return new ReportSummary(startDate, endDate, UPDATE_UNIT, results, null);
    }

    private Map<StatusCode, Integer> getStatusStatistic(LogbookEvent logbookEvent) {
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
            .map(last -> Stream.of(last.split("\\s"))
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toMap(s -> StatusCode.valueOf(s.split(":")[0]), s -> Integer.valueOf(s.split(":")[1]))))
            .orElse(Collections.emptyMap());
    }
}
