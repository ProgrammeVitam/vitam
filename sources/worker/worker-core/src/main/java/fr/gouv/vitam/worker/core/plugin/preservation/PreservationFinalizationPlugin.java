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
package fr.gouv.vitam.worker.core.plugin.preservation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.batch.report.model.OperationSummary;
import fr.gouv.vitam.batch.report.model.Report;
import fr.gouv.vitam.batch.report.model.ReportResults;
import fr.gouv.vitam.batch.report.model.ReportSummary;
import fr.gouv.vitam.batch.report.model.ReportType;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.plugin.preservation.service.PreservationReportService;
import fr.gouv.vitam.worker.core.utils.PluginHelper.EventDetails;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

import java.time.LocalDateTime;

import static fr.gouv.vitam.common.model.StatusCode.FATAL;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;

public class PreservationFinalizationPlugin extends ActionHandler {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(PreservationFinalizationPlugin.class);

    private static final String PLUGIN_NAME = "PRESERVATION_FINALIZATION";

    private PreservationReportService preservationReportService;

    public PreservationFinalizationPlugin() {
        this(new PreservationReportService());
    }

    @VisibleForTesting
    PreservationFinalizationPlugin(PreservationReportService preservationReportService) {
        this.preservationReportService = preservationReportService;
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler) throws ProcessingException, ContentAddressableStorageServerException {

        Integer tenant = VitamThreadUtils.getVitamSession().getTenantId();
        String evId = param.getRequestId();
        String evType = ""; // FIXME To be Fill in a post commit
        String outcome = ""; // FIXME To be Fill in a post commit
        String outMsg = ""; // FIXME To be Fill in a post commit
        // VitamThreadUtils.getVitamSession().getContractId();
        // VitamThreadUtils.getVitamSession().getContextId();
        // rSI = {AccessContract: contractId, Context: contextId }
        // FIXME: What should we put in rightsStatementIdentifier for Preservation ?
        JsonNode rSI = JsonHandler.createObjectNode(); // FIXME To be Fill in a post commit
        JsonNode evDetData = JsonHandler.createObjectNode(); // Will be set later by appended status data
        OperationSummary operationSummary = new OperationSummary(tenant, evId, evType, outcome, outMsg, rSI, evDetData);

        String startDate = null; // FIXME To be Fill in a post commit
        String endDate = LocalDateUtil.getString(LocalDateTime.now());
        ReportType reportType = ReportType.PRESERVATION;
        ReportResults vitamResults = new ReportResults(); // FIXME To be Fill in a post commit
        JsonNode extendedInfo = JsonHandler.createObjectNode(); // FIXME To be Fill in a post commit
        ReportSummary reportSummary = new ReportSummary(startDate, endDate, reportType, vitamResults, extendedInfo);

        JsonNode context = JsonHandler.createObjectNode();

        Report reportInfo = new Report(operationSummary, reportSummary, context);

        try {
            preservationReportService.storeReport(reportInfo);
        } catch (Exception e) {
            LOGGER.error("Error on finalization", e);
            ObjectNode eventDetails = JsonHandler.createObjectNode();
            eventDetails.put("error", e.getMessage());
            return buildItemStatus(PLUGIN_NAME, FATAL, eventDetails);
        }

        return buildItemStatus(PLUGIN_NAME, OK, EventDetails.of("Finalization ok."));
    }
}
