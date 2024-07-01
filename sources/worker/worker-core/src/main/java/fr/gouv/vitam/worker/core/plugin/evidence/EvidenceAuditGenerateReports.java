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
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.batch.report.model.EvidenceAuditReportObject;
import fr.gouv.vitam.batch.report.model.entry.EvidenceAuditReportEntry;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
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
import fr.gouv.vitam.worker.core.plugin.evidence.exception.EvidenceStatus;
import fr.gouv.vitam.worker.core.plugin.evidence.report.EvidenceAuditParameters;
import fr.gouv.vitam.worker.core.plugin.evidence.report.EvidenceAuditReportLine;
import fr.gouv.vitam.worker.core.plugin.evidence.report.EvidenceAuditReportService;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * EvidenceAuditGenerateReports class
 */
public class EvidenceAuditGenerateReports extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(EvidenceAuditGenerateReports.class);

    private static final String EVIDENCE_AUDIT_PREPARE_GENERATE_REPORTS = "EVIDENCE_AUDIT_PREPARE_GENERATE_REPORTS";
    private static final String DATA = "data";
    private static final String FILE_NAMES = "fileNames";
    public static final String ZIP = "zip";
    private static final String REPORTS = "reports";
    private static final String ALTER = "alter";
    private final EvidenceAuditReportService evidenceAuditReportService;

    @VisibleForTesting
    EvidenceAuditGenerateReports(EvidenceAuditReportService evidenceAuditReportService) {
        this.evidenceAuditReportService = evidenceAuditReportService;
    }

    public EvidenceAuditGenerateReports() {
        this(new EvidenceAuditReportService());
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handlerIO) throws ProcessingException {
        ItemStatus itemStatus = new ItemStatus(EVIDENCE_AUDIT_PREPARE_GENERATE_REPORTS);

        try {
            JsonNode options = handlerIO.getJsonFromWorkspace("evidenceOptions");
            boolean correctiveAudit = options.get("correctiveOption").booleanValue();

            //Fixme verify if  file is not too big for memory
            File securedDataFile = handlerIO.getFileFromWorkspace(ZIP + "/" + param.getObjectName());
            File listOfObjectByFile = handlerIO.getFileFromWorkspace(FILE_NAMES + "/" + param.getObjectName());

            List<String> securedLines = Files.readAllLines(securedDataFile.toPath(), Charset.defaultCharset());

            ArrayList<String> listIds = JsonHandler.getFromFileAsTypeReference(
                listOfObjectByFile,
                new TypeReference<ArrayList<String>>() {}
            );

            EvidenceService evidenceService = new EvidenceService();

            for (String objectToAuditId : listIds) {
                File infoFromDatabase = handlerIO.getFileFromWorkspace(DATA + "/" + objectToAuditId);

                EvidenceAuditParameters parameters = JsonHandler.getFromFile(
                    infoFromDatabase,
                    EvidenceAuditParameters.class
                );

                EvidenceAuditReportLine evidenceAuditReportLine;

                File file = handlerIO.getNewLocalFile(objectToAuditId);

                if (parameters.getEvidenceStatus().equals(EvidenceStatus.OK)) {
                    evidenceAuditReportLine = evidenceService.auditAndGenerateReportIfKo(
                        parameters,
                        securedLines,
                        objectToAuditId
                    );
                } else {
                    evidenceAuditReportLine = new EvidenceAuditReportLine(objectToAuditId);
                    evidenceAuditReportLine.setEvidenceStatus(parameters.getEvidenceStatus());
                    evidenceAuditReportLine.setMessage(parameters.getAuditMessage());
                }
                JsonHandler.writeAsFile(evidenceAuditReportLine, file);

                if (!correctiveAudit) addReportEntry(
                    param.getContainerName(),
                    createEvidenceReportEntry(evidenceAuditReportLine)
                );

                handlerIO.transferFileToWorkspace(
                    REPORTS + "/" + objectToAuditId + ".report.json",
                    file,
                    !correctiveAudit,
                    false
                );

                // corrective audit
                if (correctiveAudit) {
                    handlerIO.transferFileToWorkspace(ALTER + "/" + objectToAuditId + ".json", file, true, false);
                }
            }

            itemStatus.increment(StatusCode.OK);
        } catch (
            IOException
            | ContentAddressableStorageNotFoundException
            | InvalidParseOperationException
            | ProcessingStatusException
            | ContentAddressableStorageServerException e
        ) {
            LOGGER.error(e);

            return itemStatus.increment(StatusCode.FATAL);
        }
        return new ItemStatus(EVIDENCE_AUDIT_PREPARE_GENERATE_REPORTS).setItemsStatus(
            EVIDENCE_AUDIT_PREPARE_GENERATE_REPORTS,
            itemStatus
        );
    }

    private void addReportEntry(String processId, EvidenceAuditReportEntry entry) throws ProcessingStatusException {
        evidenceAuditReportService.appendEntries(processId, Arrays.asList(entry));
    }

    private EvidenceAuditReportEntry createEvidenceReportEntry(EvidenceAuditReportLine evidenceAuditReportLine) {
        ArrayList<EvidenceAuditReportObject> ListvidEvidenceAuditBatchReport = createEvidenceBatchFromEvidenceWorker(
            evidenceAuditReportLine
        );

        String message = evidenceAuditReportLine.getMessage() != null
            ? evidenceAuditReportLine.getMessage()
            : "audit " +
            evidenceAuditReportLine.getEvidenceStatus().name() +
            " for " +
            evidenceAuditReportLine.getObjectType().getName();
        return new EvidenceAuditReportEntry(
            evidenceAuditReportLine.getIdentifier(),
            evidenceAuditReportLine.getEvidenceStatus().name(),
            message,
            evidenceAuditReportLine.getObjectType().name(),
            ListvidEvidenceAuditBatchReport,
            evidenceAuditReportLine.getSecuredHash(),
            evidenceAuditReportLine.getStrategyId(),
            evidenceAuditReportLine.getOffersHashes(),
            evidenceAuditReportLine.getEvidenceStatus().name()
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
}
