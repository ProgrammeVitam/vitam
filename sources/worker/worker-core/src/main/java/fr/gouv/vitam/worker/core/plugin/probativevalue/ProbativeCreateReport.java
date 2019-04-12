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
package fr.gouv.vitam.worker.core.plugin.probativevalue;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.batch.report.model.OperationSummary;
import fr.gouv.vitam.batch.report.model.ReportResults;
import fr.gouv.vitam.batch.report.model.ReportSummary;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.BackupService;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.distribution.JsonLineGenericIterator;
import fr.gouv.vitam.worker.core.distribution.JsonLineModel;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.plugin.evidence.exception.EvidenceStatus;
import fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.ProbativeReportEntry;
import fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.ProbativeReportV2;
import fr.gouv.vitam.worker.core.utils.PluginHelper.EventDetails;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static fr.gouv.vitam.batch.report.model.ReportType.PROBATIVE_VALUE;
import static fr.gouv.vitam.storage.engine.common.model.DataCategory.REPORT;
import static fr.gouv.vitam.worker.core.plugin.evidence.EvidenceService.JSON;
import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;
import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatusWithMasterData;

public class ProbativeCreateReport extends ActionHandler {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProbativeCreateReport.class);

    private static final String HANDLER_ID = "PROBATIVE_VALUE_CREATE_REPORT";
    private static final TypeReference<JsonLineModel> TYPE_REFERENCE = new TypeReference<JsonLineModel>() {};
    private static final ReportVersion2 reportVersion2 = new ReportVersion2();

    private final BackupService backupService;

    @VisibleForTesting
    public ProbativeCreateReport(BackupService backupService) {
        this.backupService = backupService;
    }

    public ProbativeCreateReport() {
        this(new BackupService());
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler) throws ProcessingException, ContentAddressableStorageServerException {

        try (InputStream inputStream = handler.getInputStreamFromWorkspace("distributionFile.jsonl");
             JsonLineGenericIterator<JsonLineModel> lines = new JsonLineGenericIterator<>(inputStream, TYPE_REFERENCE)){
             JsonNode context = JsonHandler.getFromFile(handler.getFileFromWorkspace("request"));

            List<ProbativeReportEntry> probativeEntries = lines.stream()
                .map(line -> getFromFile(handler, line))
                .collect(Collectors.toList());

            ReportResults reportResults = new ReportResults(
                new Long(probativeEntries.stream().filter(e -> e.getStatus().equals(EvidenceStatus.OK)).count()).intValue(),
                new Long(probativeEntries.stream().filter(e -> e.getStatus().equals(EvidenceStatus.KO)).count()).intValue(),
                new Long(probativeEntries.stream().filter(e -> e.getStatus().equals(EvidenceStatus.WARN)).count()).intValue(),
                probativeEntries.size()
            );

            String startDate = probativeEntries.stream()
                .min(Comparator.comparing(ProbativeReportEntry::getEvStartDateTime))
                .map(ProbativeReportEntry::getEvStartDateTime)
                .orElse("NO_START_DATE_FOUND");
            String endDate = probativeEntries.stream()
                .max(Comparator.comparing(ProbativeReportEntry::getEvStartDateTime))
                .map(ProbativeReportEntry::getEvStartDateTime)
                .orElse("NO_END_DATE_FOUND");

            ReportSummary reportSummary = new ReportSummary(
                startDate,
                endDate,
                PROBATIVE_VALUE,
                reportResults,
                null
            );

            OperationSummary operationSummary = new OperationSummary(
                VitamThreadUtils.getVitamSession().getTenantId(),
                param.getContainerName(),
                "PROBATIVE_VALUE",
                "",
                "",
                "",
                JsonHandler.createObjectNode(),
                JsonHandler.createObjectNode()
            );

            ProbativeReportV2 probativeReportV2 = new ProbativeReportV2(operationSummary, reportSummary, context, probativeEntries);
            backupService.backup(JsonHandler.writeToInpustream(probativeReportV2), REPORT, param.getContainerName() + JSON);

            return buildItemStatusWithMasterData(HANDLER_ID, StatusCode.OK, EventDetails.of("Probative value report success."), JsonHandler.unprettyPrint(reportVersion2));
        } catch (Exception e) {
            LOGGER.error(e);
            return buildItemStatus(HANDLER_ID, StatusCode.KO, EventDetails.of("Probative value report error."));
        }
    }

    private ProbativeReportEntry getFromFile(HandlerIO handler, JsonLineModel line) {
        try {
            return JsonHandler.getFromFile(handler.getFileFromWorkspace(line.getId()), ProbativeReportEntry.class);
        } catch (InvalidParseOperationException | IOException | ContentAddressableStorageNotFoundException | ContentAddressableStorageServerException e) {
            throw new VitamRuntimeException(e);
        }
    }

    public static class ReportVersion2 {
        @JsonProperty("probativeReportVersion")
        private final int probativeReportVersion = 2;
    }
}
