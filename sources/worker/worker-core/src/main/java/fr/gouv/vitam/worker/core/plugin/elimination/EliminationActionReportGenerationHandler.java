/*******************************************************************************
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
 *******************************************************************************/
package fr.gouv.vitam.worker.core.plugin.elimination;

import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.collection.CloseableIterator;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.functional.administration.common.BackupService;
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
import fr.gouv.vitam.worker.core.exception.ProcessingStatusException;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.plugin.elimination.report.EliminationActionObjectGroupReportExportEntry;
import fr.gouv.vitam.worker.core.plugin.elimination.report.EliminationActionReportService;
import fr.gouv.vitam.worker.core.plugin.elimination.report.EliminationActionUnitReportEntry;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;


/**
 * Elimination action finalization handler.
 */
public class EliminationActionReportGenerationHandler extends ActionHandler {

    static final String WORKSPACE_REPORT_URI = "report.json";

    private static final VitamLogger LOGGER =
        VitamLoggerFactory.getInstance(EliminationActionReportGenerationHandler.class);

    private static final String ELIMINATION_ACTION_REPORT_GENERATION = "ELIMINATION_ACTION_REPORT_GENERATION";

    static final String REPORT_JSON = "report.json";
    private static final String JSON_EXTENSION = ".json";

    private final EliminationActionReportService eliminationActionReportService;
    private final StorageClientFactory storageClientFactory;

    /**
     * Default constructor
     */
    public EliminationActionReportGenerationHandler() {
        this(
            new EliminationActionReportService(),
            StorageClientFactory.getInstance(), new BackupService());
    }

    /***
     * Test only constructor
     */
    @VisibleForTesting
    EliminationActionReportGenerationHandler(
        EliminationActionReportService eliminationActionReportService,
        StorageClientFactory storageClientFactory, BackupService backupService) {
        this.eliminationActionReportService = eliminationActionReportService;
        this.storageClientFactory = storageClientFactory;
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler)
        throws ProcessingException, ContentAddressableStorageServerException {

        try {

            generateReportToWorkspace(param, handler);

            storeReportToOffers(handler.getContainerName());

            LOGGER.info("Elimination action finalization succeeded");
            return buildItemStatus(ELIMINATION_ACTION_REPORT_GENERATION, StatusCode.OK, null);
        } catch (ProcessingStatusException e) {
            LOGGER.error(
                String.format("Elimination action finalization failed with status [%s]", e.getStatusCode()), e);
            return buildItemStatus(ELIMINATION_ACTION_REPORT_GENERATION, e.getStatusCode(), e.getEventDetails());
        }
    }

    private void generateReportToWorkspace(WorkerParameters param, HandlerIO handler) throws ProcessingStatusException {

        if (isReportWrittenInWorkspace(handler)) {
            // Report already generated (idempotency)
            return;
        }

        File report = generateEliminationReport(param, handler);

        storeEliminationReportToWorkspace(handler, report);
    }

    public boolean isReportWrittenInWorkspace(HandlerIO handler) throws ProcessingStatusException {
        try {
            return handler.isExistingFileInWorkspace(WORKSPACE_REPORT_URI);
        } catch (ProcessingException e) {
            throw new ProcessingStatusException(StatusCode.FATAL, "Could not check report existence in workspace", e);
        }
    }

    private File generateEliminationReport(WorkerParameters param, HandlerIO handler) throws ProcessingStatusException {
        File report = handler.getNewLocalFile(REPORT_JSON);

        try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(report));
            JsonGenerator jsonGenerator = JsonHandler.createJsonGenerator(outputStream)) {

            jsonGenerator.writeStartObject();

            jsonGenerator.writeFieldName("units");
            jsonGenerator.writeStartArray();

            try (CloseableIterator<EliminationActionUnitReportEntry>
                unitIterator = eliminationActionReportService.exportUnits(param.getContainerName())) {

                while (unitIterator.hasNext()) {
                    EliminationActionUnitReportEntry unitEliminationExport = unitIterator.next();
                    jsonGenerator.writeObject(unitEliminationExport);
                }
            }

            jsonGenerator.writeEndArray();

            jsonGenerator.writeFieldName("objectGroups");
            jsonGenerator.writeStartArray();

            try (CloseableIterator<EliminationActionObjectGroupReportExportEntry> objectGroupIterator =
                eliminationActionReportService.exportObjectGroups(param.getContainerName())) {

                while (objectGroupIterator.hasNext()) {
                    EliminationActionObjectGroupReportExportEntry objectGroupEliminationExport =
                        objectGroupIterator.next();
                    jsonGenerator.writeObject(objectGroupEliminationExport);
                }
            }

            jsonGenerator.writeEndArray();
            jsonGenerator.writeEndObject();

        } catch (IOException e) {
            throw new ProcessingStatusException(StatusCode.FATAL, "Could not export elimination report", e);
        }
        return report;
    }

    private void storeEliminationReportToWorkspace(HandlerIO handler, File report) throws ProcessingStatusException {
        try {
            handler.transferAtomicFileToWorkspace(REPORT_JSON, report);

        } catch (ProcessingException e) {
            throw new ProcessingStatusException(StatusCode.FATAL, "Could not store elimination report to workspace", e);
        }
    }

    public void storeReportToOffers(String containerName) throws ProcessingStatusException {
        try (StorageClient storageClient = storageClientFactory.getClient()) {
            ObjectDescription description = new ObjectDescription();
            description.setWorkspaceContainerGUID(containerName);
            description.setWorkspaceObjectURI(WORKSPACE_REPORT_URI);
            storageClient.storeFileFromWorkspace(VitamConfiguration.getDefaultStrategy(),
                DataCategory.REPORT, containerName + JSON_EXTENSION, description);
        } catch (StorageAlreadyExistsClientException | StorageNotFoundClientException | StorageServerClientException e) {
            throw new ProcessingStatusException(StatusCode.FATAL, "Could not store report to offers", e);
        }
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // NOP.
    }

    public static String getId() {
        return ELIMINATION_ACTION_REPORT_GENERATION;
    }
}
