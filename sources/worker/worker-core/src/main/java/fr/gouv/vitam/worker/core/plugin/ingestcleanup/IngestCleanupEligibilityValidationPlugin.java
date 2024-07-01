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
package fr.gouv.vitam.worker.core.plugin.ingestcleanup;

import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameterName;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.exception.ProcessingStatusException;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.plugin.ingestcleanup.report.CleanupReportManager;
import fr.gouv.vitam.worker.core.plugin.ingestcleanup.service.IngestCleanupEligibilityService;

import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;

public class IngestCleanupEligibilityValidationPlugin extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(
        IngestCleanupEligibilityValidationPlugin.class
    );
    private static final String INGEST_CLEANUP_ELIGIBILITY_VALIDATION = "INGEST_CLEANUP_ELIGIBILITY_VALIDATION";

    private final IngestCleanupEligibilityService ingestCleanupEligibilityService;

    public IngestCleanupEligibilityValidationPlugin() {
        this(
            new IngestCleanupEligibilityService(
                MetaDataClientFactory.getInstance(),
                LogbookOperationsClientFactory.getInstance()
            )
        );
    }

    @VisibleForTesting
    public IngestCleanupEligibilityValidationPlugin(IngestCleanupEligibilityService ingestCleanupEligibilityService) {
        this.ingestCleanupEligibilityService = ingestCleanupEligibilityService;
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler) throws ProcessingException {
        try {
            String ingestOperationId = param.getParameterValue(WorkerParameterName.ingestOperationIdToCleanup);

            CleanupReportManager cleanupReportManager = CleanupReportManager.newReport(ingestOperationId);

            checkUnits(ingestOperationId, cleanupReportManager);
            checkObjectGroups(ingestOperationId, cleanupReportManager);

            cleanupReportManager.persistReportDataToWorkspace(handler);

            StatusCode globalStatusCode = cleanupReportManager.getGlobalStatus();
            if (globalStatusCode.isGreaterOrEqualToKo()) {
                LOGGER.error("Blocking validation errors...");
            } else if (globalStatusCode.isGreaterOrEqualToWarn()) {
                LOGGER.warn("Non blocking validation errors...");
            } else {
                LOGGER.info("Ingest cleanup eligibility validation succeeded");
            }
            return buildItemStatus(INGEST_CLEANUP_ELIGIBILITY_VALIDATION, globalStatusCode);
        } catch (ProcessingStatusException e) {
            LOGGER.error(String.format("Cleanup eligibility validation failed with status [%s]", e.getStatusCode()), e);
            return buildItemStatus(INGEST_CLEANUP_ELIGIBILITY_VALIDATION, e.getStatusCode(), e.getEventDetails());
        }
    }

    private void checkUnits(String ingestOperationId, CleanupReportManager cleanupReportManager)
        throws ProcessingStatusException {
        ingestCleanupEligibilityService.checkChildUnitsFromOtherIngests(ingestOperationId, cleanupReportManager);
        ingestCleanupEligibilityService.checkUnitUpdatesFromOtherOperations(ingestOperationId, cleanupReportManager);
    }

    private void checkObjectGroups(String ingestOperationId, CleanupReportManager cleanupReportManager)
        throws ProcessingStatusException {
        ingestCleanupEligibilityService.checkObjectGroupUpdatesFromOtherOperations(
            ingestOperationId,
            cleanupReportManager
        );
        ingestCleanupEligibilityService.checkObjectAttachmentsToExistingObjectGroups(
            ingestOperationId,
            cleanupReportManager
        );
    }
}
