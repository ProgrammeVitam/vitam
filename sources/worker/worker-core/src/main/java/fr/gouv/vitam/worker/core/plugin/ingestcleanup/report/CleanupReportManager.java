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
package fr.gouv.vitam.worker.core.plugin.ingestcleanup.report;

import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.exception.StorageAlreadyExistsClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.distribution.JsonLineModel;
import fr.gouv.vitam.worker.core.distribution.JsonLineWriter;
import fr.gouv.vitam.worker.core.exception.ProcessingStatusException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

/**
 * In memory report management for ingest cleanup workflow.
 * Assumes that ingest operations to not exceed 100 000 items.
 */
public class CleanupReportManager {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CleanupReportManager.class);

    public static final String CLEANUP_REPORT_BACKUP_FILE_NAME = "cleanupReportBackup.json";
    private static final String JSONL_EXTENSION = ".jsonl";

    private final CleanupReport cleanupReport;

    private CleanupReportManager(CleanupReport cleanupReport) {
        this.cleanupReport = cleanupReport;
    }

    public void reportUnitError(String id, String message) {
        IngestCleanupUnitReportEntry unitReportEntry = cleanupReport
            .getUnits()
            .computeIfAbsent(id, _id -> new IngestCleanupUnitReportEntry().setId(_id));
        unitReportEntry.addError(message);
        unitReportEntry.updateStatus(StatusCode.KO);
    }

    public void reportUnitWarning(String id, String message) {
        IngestCleanupUnitReportEntry unitReportEntry = cleanupReport
            .getUnits()
            .computeIfAbsent(id, _id -> new IngestCleanupUnitReportEntry().setId(_id));
        unitReportEntry.addWarning(message);
        unitReportEntry.updateStatus(StatusCode.WARNING);
    }

    public void reportObjectGroupError(String id, String message) {
        IngestCleanupObjectGroupReportEntry objectGroupReportEntry = cleanupReport
            .getObjectGroups()
            .computeIfAbsent(id, _id -> new IngestCleanupObjectGroupReportEntry().setId(_id));
        objectGroupReportEntry.addError(message);
        objectGroupReportEntry.updateStatus(StatusCode.KO);
    }

    public void reportObjectGroupWarning(String id, String message) {
        IngestCleanupObjectGroupReportEntry objectGroupReportEntry = cleanupReport
            .getObjectGroups()
            .computeIfAbsent(id, _id -> new IngestCleanupObjectGroupReportEntry().setId(_id));
        objectGroupReportEntry.addWarning(message);
        objectGroupReportEntry.updateStatus(StatusCode.WARNING);
    }

    public void reportDeletedUnit(String id) {
        IngestCleanupUnitReportEntry unitReportEntry = cleanupReport
            .getUnits()
            .computeIfAbsent(id, _id -> new IngestCleanupUnitReportEntry().setId(_id));
        unitReportEntry.updateStatus(StatusCode.OK);
    }

    public void reportDeletedObjectGroup(String id, List<String> objects) {
        IngestCleanupObjectGroupReportEntry objectGroupReportEntry = cleanupReport
            .getObjectGroups()
            .computeIfAbsent(id, _id -> new IngestCleanupObjectGroupReportEntry().setId(_id));
        objectGroupReportEntry.updateStatus(StatusCode.OK);
        objectGroupReportEntry.setObjects(objects);
    }

    public StatusCode getGlobalStatus() {
        StatusCode statusCode = StatusCode.OK;
        for (IngestCleanupUnitReportEntry unitReportEntry : this.cleanupReport.getUnits().values()) {
            if (statusCode.compareTo(unitReportEntry.getStatus()) < 0) {
                statusCode = unitReportEntry.getStatus();
            }
        }
        for (IngestCleanupObjectGroupReportEntry objectGroupReportEntry : this.cleanupReport.getObjectGroups()
            .values()) {
            if (statusCode.compareTo(objectGroupReportEntry.getStatus()) < 0) {
                statusCode = objectGroupReportEntry.getStatus();
            }
        }
        return statusCode;
    }

    public void persistReportDataToWorkspace(HandlerIO handlerIO) throws ProcessingStatusException {
        File tmpFile = handlerIO.getNewLocalFile(GUIDFactory.newGUID().getId());
        try {
            JsonHandler.writeAsFile(this.cleanupReport, tmpFile);
            handlerIO.transferFileToWorkspace(CLEANUP_REPORT_BACKUP_FILE_NAME, tmpFile, true, false);
        } catch (InvalidParseOperationException | ProcessingException e) {
            throw new ProcessingStatusException(StatusCode.FATAL, "Could not persist cleanup report", e);
        }
    }

    public void exportReport(HandlerIO handlerIO, StorageClient storageClient) throws ProcessingStatusException {
        File tmpFile = generateReport(handlerIO);
        saveReportToOffers(handlerIO, storageClient, tmpFile);
    }

    private File generateReport(HandlerIO handlerIO) throws ProcessingStatusException {
        File tmpFile = handlerIO.getNewLocalFile(GUIDFactory.newGUID().getId());
        try (
            FileOutputStream fileOutputStream = new FileOutputStream(tmpFile);
            JsonLineWriter writer = new JsonLineWriter(fileOutputStream)
        ) {
            writer.addEntry(
                JsonHandler.createObjectNode().put("ingestOperationId", this.cleanupReport.getIngestOperationId())
            );
            for (IngestCleanupUnitReportEntry unit : this.cleanupReport.getUnits().values()) {
                writer.addEntry(new JsonLineModel(unit.getId(), null, JsonHandler.toJsonNode(unit)));
            }
            for (IngestCleanupObjectGroupReportEntry objectGroup : this.cleanupReport.getObjectGroups().values()) {
                writer.addEntry(new JsonLineModel(objectGroup.getId(), null, JsonHandler.toJsonNode(objectGroup)));
            }
        } catch (IOException | InvalidParseOperationException e) {
            throw new ProcessingStatusException(StatusCode.FATAL, "Could not generate report", e);
        }
        return tmpFile;
    }

    private void saveReportToOffers(HandlerIO handlerIO, StorageClient storageClient, File tmpFile)
        throws ProcessingStatusException {
        try {
            String reportFileName = handlerIO.getContainerName() + JSONL_EXTENSION;
            handlerIO.transferFileToWorkspace(reportFileName, tmpFile, true, false);
            ObjectDescription description = new ObjectDescription();
            description.setWorkspaceContainerGUID(handlerIO.getContainerName());
            description.setWorkspaceObjectURI(reportFileName);

            storageClient.storeFileFromWorkspace(
                VitamConfiguration.getDefaultStrategy(),
                DataCategory.REPORT,
                reportFileName,
                description
            );
        } catch (
            StorageAlreadyExistsClientException
            | StorageNotFoundClientException
            | StorageServerClientException
            | ProcessingException e
        ) {
            throw new ProcessingStatusException(StatusCode.FATAL, "Could not export report", e);
        }
    }

    @VisibleForTesting
    public CleanupReport getCleanupReport() {
        return cleanupReport;
    }

    public static CleanupReportManager newReport(String ingestOperationId) {
        return new CleanupReportManager(new CleanupReport().setIngestOperationId(ingestOperationId));
    }

    public static Optional<CleanupReportManager> loadReportDataFromWorkspace(HandlerIO handlerIO)
        throws ProcessingStatusException {
        try (InputStream inputStream = handlerIO.getInputStreamFromWorkspace(CLEANUP_REPORT_BACKUP_FILE_NAME)) {
            CleanupReport cleanupReport = JsonHandler.getFromInputStream(inputStream, CleanupReport.class);
            return Optional.of(new CleanupReportManager(cleanupReport));
        } catch (ContentAddressableStorageServerException | InvalidParseOperationException | IOException e) {
            throw new ProcessingStatusException(StatusCode.FATAL, "Could not load cleanup report", e);
        } catch (ContentAddressableStorageNotFoundException e) {
            LOGGER.warn("Report not found", e);
            return Optional.empty();
        }
    }
}
