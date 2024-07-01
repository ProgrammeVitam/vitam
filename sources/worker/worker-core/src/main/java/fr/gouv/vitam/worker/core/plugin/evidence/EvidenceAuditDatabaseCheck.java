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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.batch.report.model.EvidenceAuditReportObject;
import fr.gouv.vitam.batch.report.model.entry.EvidenceAuditReportEntry;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.MetadataType;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageStrategy;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.exception.ProcessingStatusException;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.plugin.audit.exception.AuditException;
import fr.gouv.vitam.worker.core.plugin.evidence.exception.EvidenceStatus;
import fr.gouv.vitam.worker.core.plugin.evidence.report.EvidenceAuditParameters;
import fr.gouv.vitam.worker.core.plugin.evidence.report.EvidenceAuditReportLine;
import fr.gouv.vitam.worker.core.plugin.evidence.report.EvidenceAuditReportService;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static fr.gouv.vitam.common.json.JsonHandler.unprettyPrint;

/**
 * EvidenceAuditDatabaseCheck class
 */
public class EvidenceAuditDatabaseCheck extends ActionHandler {

    private static final String EVIDENCE_AUDIT_CHECK_DATABASE = "EVIDENCE_AUDIT_CHECK_DATABASE";
    private static final String METADA_TYPE = "metadaType";
    private static final String DATA = "data";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(EvidenceAuditDatabaseCheck.class);
    private static final String REPORTS = "reports";
    private static final int STRATEGIES_IN_RANK = 0;
    private final EvidenceService evidenceService;
    private final EvidenceAuditReportService evidenceAuditReportService;

    @VisibleForTesting
    EvidenceAuditDatabaseCheck(EvidenceService evidenceService, EvidenceAuditReportService evidenceAuditReportService) {
        this.evidenceService = evidenceService;
        this.evidenceAuditReportService = evidenceAuditReportService;
    }

    public EvidenceAuditDatabaseCheck() {
        this(new EvidenceService(), new EvidenceAuditReportService());
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handlerIO) throws ProcessingException {
        ItemStatus itemStatus = new ItemStatus(EVIDENCE_AUDIT_CHECK_DATABASE);
        String objectToAuditId = param.getObjectName();

        try {
            File file = handlerIO.getFileFromWorkspace("Object/" + objectToAuditId);

            JsonNode objectIdToAudit = JsonHandler.getFromFile(file);

            MetadataType metadataType = MetadataType.valueOf(objectIdToAudit.get(METADA_TYPE).textValue());

            List<StorageStrategy> storageStrategies = loadStorageStrategies(handlerIO);
            EvidenceAuditParameters parameters = evidenceService.evidenceAuditsChecks(
                objectToAuditId,
                metadataType,
                storageStrategies
            );

            File newLocalFile = handlerIO.getNewLocalFile(objectToAuditId + ".tmp");
            JsonHandler.writeAsFile(parameters, newLocalFile);

            handlerIO.transferFileToWorkspace(DATA + "/" + objectToAuditId, newLocalFile, true, false);

            if (parameters.getEvidenceStatus().equals(EvidenceStatus.FATAL)) {
                itemStatus.increment(StatusCode.FATAL);
                ObjectNode infoNode = JsonHandler.createObjectNode();
                infoNode.put("Message", "Fatal Technical error " + parameters.getAuditMessage());
                itemStatus.setEvDetailData(unprettyPrint(infoNode));
                evidenceAuditReportService.cleanupReport(param.getContainerName());
                return new ItemStatus(EVIDENCE_AUDIT_CHECK_DATABASE).setItemsStatus(
                    EVIDENCE_AUDIT_CHECK_DATABASE,
                    itemStatus
                );
            }
            if (
                parameters.getEvidenceStatus().equals(EvidenceStatus.KO) ||
                parameters.getEvidenceStatus().equals(EvidenceStatus.WARN)
            ) {
                EvidenceAuditReportLine evidenceAuditReportLine = null;
                evidenceAuditReportLine = new EvidenceAuditReportLine(objectToAuditId);
                evidenceAuditReportLine.setEvidenceStatus(parameters.getEvidenceStatus());
                evidenceAuditReportLine.setMessage(parameters.getAuditMessage());
                evidenceAuditReportLine.setStrategyId(parameters.getMdOptimisticStorageInfo().getStrategy());
                JsonHandler.writeAsFile(evidenceAuditReportLine, file);
                handlerIO.transferFileToWorkspace(REPORTS + "/" + objectToAuditId + ".report.json", file, true, false);
                if (!param.getWorkflowIdentifier().equals("RECTIFICATION_AUDIT")) addReportEntry(
                    param.getContainerName(),
                    createEvidenceReportEntry(parameters, evidenceAuditReportLine)
                );
            }

            itemStatus.increment(StatusCode.OK);
            return new ItemStatus(EVIDENCE_AUDIT_CHECK_DATABASE).setItemsStatus(
                EVIDENCE_AUDIT_CHECK_DATABASE,
                itemStatus
            );
        } catch (VitamException | IOException e) {
            LOGGER.error(e);
            itemStatus.increment(StatusCode.FATAL);
            return new ItemStatus(EVIDENCE_AUDIT_CHECK_DATABASE).setItemsStatus(
                EVIDENCE_AUDIT_CHECK_DATABASE,
                itemStatus
            );
        } catch (ProcessingStatusException e) {
            LOGGER.error(e);
            itemStatus.increment(e.getStatusCode());
            return new ItemStatus(EVIDENCE_AUDIT_CHECK_DATABASE).setItemsStatus(
                EVIDENCE_AUDIT_CHECK_DATABASE,
                itemStatus
            );
        }
    }

    private void addReportEntry(String processId, EvidenceAuditReportEntry entry) throws ProcessingStatusException {
        evidenceAuditReportService.appendEntries(processId, Arrays.asList(entry));
    }

    private EvidenceAuditReportEntry createEvidenceReportEntry(
        EvidenceAuditParameters parameters,
        EvidenceAuditReportLine evidenceAuditReportLine
    ) {
        ArrayList<EvidenceAuditReportObject> ListvidEvidenceAuditBatchReport = createEvidenceBatchFromEvidenceWorker(
            evidenceAuditReportLine
        );

        return new EvidenceAuditReportEntry(
            parameters.getId(),
            parameters.getEvidenceStatus().name(),
            parameters.getAuditMessage(),
            parameters.getMetadataType().name(),
            ListvidEvidenceAuditBatchReport,
            evidenceAuditReportLine.getSecuredHash(),
            evidenceAuditReportLine.getStrategyId(),
            evidenceAuditReportLine.getOffersHashes(),
            parameters.getAuditMessage()
        );
    }

    private ArrayList<EvidenceAuditReportObject> createEvidenceBatchFromEvidenceWorker(
        EvidenceAuditReportLine evidenceAuditReportLine
    ) {
        ArrayList<EvidenceAuditReportObject> list = new ArrayList<>();

        if (evidenceAuditReportLine.getObjectsReports() != null) {
            for (fr.gouv.vitam.worker.core.plugin.evidence.report.EvidenceAuditReportObject objects : evidenceAuditReportLine.getObjectsReports()) {
                list.add(
                    new EvidenceAuditReportObject(
                        objects.getIdentifier(),
                        objects.getEvidenceStatus().name(),
                        objects.getMessage(),
                        objects.getObjectType(),
                        objects.getSecuredHash(),
                        objects.getStrategyId(),
                        objects.getOffersHashes()
                    )
                );
            }
            return list;
        }
        return list;
    }

    private List<StorageStrategy> loadStorageStrategies(HandlerIO handler) throws AuditException {
        try {
            return JsonHandler.getFromFileAsTypeReference(
                (File) handler.getInput(STRATEGIES_IN_RANK),
                new TypeReference<List<StorageStrategy>>() {}
            );
        } catch (InvalidParseOperationException e) {
            throw new AuditException(StatusCode.FATAL, "Could not load storage strategies datas", e);
        }
    }
}
