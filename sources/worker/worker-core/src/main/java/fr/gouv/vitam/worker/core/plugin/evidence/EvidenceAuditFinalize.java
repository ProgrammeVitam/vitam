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
package fr.gouv.vitam.worker.core.plugin.evidence;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.exception.ProcessingStatusException;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.plugin.evidence.report.EvidenceAuditReportService;

import static fr.gouv.vitam.worker.core.plugin.CommonReportService.WORKSPACE_REPORT_URI;

/**
 * EvidenceAuditFinalize class
 */
public class EvidenceAuditFinalize extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(EvidenceAuditFinalize.class);

    private static final String EVIDENCE_AUDIT_FINALIZE = "EVIDENCE_AUDIT_FINALIZE";

    private final EvidenceAuditReportService evidenceAuditReportService;

    @VisibleForTesting
    EvidenceAuditFinalize(EvidenceAuditReportService evidenceAuditReportService) {
        this.evidenceAuditReportService = evidenceAuditReportService;
    }

    public EvidenceAuditFinalize() {
        this(new EvidenceAuditReportService());
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handlerIO) throws ProcessingException {
        ItemStatus itemStatus = new ItemStatus(EVIDENCE_AUDIT_FINALIZE);

        try {
            String containerName = param.getContainerName();
            if (handlerIO.isExistingFileInWorkspace(WORKSPACE_REPORT_URI)) {
                evidenceAuditReportService.storeReportToOffers(containerName);
            } else {
                LOGGER.warn("No report generated");

                ObjectNode eventDetails = JsonHandler.createObjectNode();
                eventDetails.put("error", "No report generated");

                itemStatus.setEvDetailData(JsonHandler.unprettyPrint(eventDetails));
                itemStatus.increment(StatusCode.WARNING);
            }

            evidenceAuditReportService.cleanupReport(containerName);

            itemStatus.increment(StatusCode.OK);
            return new ItemStatus(EVIDENCE_AUDIT_FINALIZE).setItemsStatus(EVIDENCE_AUDIT_FINALIZE, itemStatus);
        } catch (ProcessingStatusException e) {
            LOGGER.error(e);
            itemStatus.increment(e.getStatusCode());
            return new ItemStatus(EVIDENCE_AUDIT_FINALIZE).setItemsStatus(EVIDENCE_AUDIT_FINALIZE, itemStatus);
        }
    }
}
