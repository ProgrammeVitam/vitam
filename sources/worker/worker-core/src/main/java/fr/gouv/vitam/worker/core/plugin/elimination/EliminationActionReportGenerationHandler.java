/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
package fr.gouv.vitam.worker.core.plugin.elimination;

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
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.logbook.LogbookEvent;
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
import fr.gouv.vitam.worker.core.plugin.GenericReportGenerationHandler;
import fr.gouv.vitam.worker.core.plugin.elimination.report.EliminationActionReportService;

import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;


/**
 * Elimination action finalization handler.
 */
public class EliminationActionReportGenerationHandler extends GenericReportGenerationHandler {

    private static final VitamLogger LOGGER =
        VitamLoggerFactory.getInstance(EliminationActionReportGenerationHandler.class);

    private static final String ELIMINATION_ACTION_REPORT_GENERATION = "ELIMINATION_ACTION_REPORT_GENERATION";
    private static final String LOGBOOK_ACTION_KEY = "ELIMINATION_ACTION_DELETE_UNIT";

    private final EliminationActionReportService eliminationActionReportService;
    private final LogbookOperationsClientFactory logbookOperationsClientFactory;


    /**
     * Default constructor
     */
    public EliminationActionReportGenerationHandler() {
        this(new EliminationActionReportService(), LogbookOperationsClientFactory.getInstance());
    }

    /***
     * Test only constructor
     */
    @VisibleForTesting
    EliminationActionReportGenerationHandler(
        EliminationActionReportService eliminationActionReportService,
        LogbookOperationsClientFactory logbookOperationsClientFactory) {
        this.eliminationActionReportService = eliminationActionReportService;
        this.logbookOperationsClientFactory = logbookOperationsClientFactory;
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler)
        throws ProcessingException {

        try {

            storeReportToWorkspace(param);

            storeReportToOffers(param.getContainerName());

            cleanupReport(param.getContainerName());

            LOGGER.info("Elimination action finalization succeeded");
            return buildItemStatus(ELIMINATION_ACTION_REPORT_GENERATION, StatusCode.OK, null);
        } catch (ProcessingStatusException e) {
            LOGGER.error(
                String.format("Elimination action finalization failed with status [%s]", e.getStatusCode()), e);
            return buildItemStatus(ELIMINATION_ACTION_REPORT_GENERATION, e.getStatusCode(), e.getEventDetails());
        }
    }

    private void storeReportToWorkspace(WorkerParameters param)
        throws ProcessingStatusException, ProcessingException {
        try {
            if (eliminationActionReportService.isReportWrittenInWorkspace(param.getContainerName())) {
                // Already stored in workspace (idempotency)
                return;
            }

            Report reportInfo = generateReport(param, getLogbookInformation(param));
            eliminationActionReportService.storeReportToWorkspace(reportInfo);
        } catch (InvalidParseOperationException e) {
            throw new ProcessingException(e);
        }
    }

    private LogbookOperation getLogbookInformation(WorkerParameters param) throws ProcessingException {
        try (LogbookOperationsClient logbookClient = logbookOperationsClientFactory.getClient()) {
            JsonNode response = logbookClient.selectOperationById(param.getContainerName());
            RequestResponseOK<JsonNode> logbookResponse = RequestResponseOK.getFromJsonNode(response);
            return JsonHandler.getFromJsonNode(logbookResponse.getFirstResult(), LogbookOperation.class);
        } catch (InvalidParseOperationException | LogbookClientException e) {
             throw new ProcessingException(e);
        }
    }


    private void storeReportToOffers(String containerName) throws ProcessingStatusException {
        eliminationActionReportService.storeReportToOffers(containerName);
    }

    private void cleanupReport(String containerName) throws ProcessingStatusException {
        eliminationActionReportService.cleanupReport(containerName);
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // NOP.
    }

    public static String getId() {
        return ELIMINATION_ACTION_REPORT_GENERATION;
    }

    @Override
    public ReportType getReportType() {
        return ReportType.ELIMINATION_ACTION;
    }

    @Override
    public String getLogbookActionKey() {
        return LOGBOOK_ACTION_KEY;
    }
}
