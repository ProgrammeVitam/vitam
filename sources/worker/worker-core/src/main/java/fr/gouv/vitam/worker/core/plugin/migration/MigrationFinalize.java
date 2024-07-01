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
package fr.gouv.vitam.worker.core.plugin.migration;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.functional.administration.common.exception.BackupServiceException;
import fr.gouv.vitam.functional.administration.core.backup.BackupService;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.handler.HandlerUtils;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

import java.io.IOException;

import static fr.gouv.vitam.common.model.VitamConstants.JSON_EXTENSION;
import static fr.gouv.vitam.worker.core.plugin.migration.MigrationUnitPrepare.MIGRATION_UNITS_LIST_IDS;

/**
 * MigrationFinalize class
 */
public class MigrationFinalize extends ActionHandler {

    private static final String MIGRATION_FINALIZE = "MIGRATION_FINALIZE";
    private final BackupService backupService;

    private static final String REPORTS = "reports";

    @VisibleForTesting
    private MigrationFinalize(BackupService backupService) {
        this.backupService = backupService;
    }

    public MigrationFinalize() {
        this(new BackupService());
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handlerIO) throws ProcessingException {
        ItemStatus itemStatus = new ItemStatus(MIGRATION_FINALIZE);

        String reportFileName = handlerIO.getContainerName() + JSON_EXTENSION;

        saveReportToWorkspace(handlerIO, reportFileName);

        storeInOffers(reportFileName);

        itemStatus.increment(StatusCode.OK);

        return new ItemStatus(MIGRATION_FINALIZE).setItemsStatus(MIGRATION_FINALIZE, itemStatus);
    }

    private void saveReportToWorkspace(HandlerIO handlerIO, String reportFileName) throws ProcessingException {
        if (handlerIO.isExistingFileInWorkspace(reportFileName)) {
            // Report already exists (idempotency)
            return;
        }

        try {
            ObjectNode reportJson = JsonHandler.createObjectNode();
            if (handlerIO.isExistingFileInWorkspace(REPORTS + "/" + MIGRATION_UNITS_LIST_IDS + JSON_EXTENSION)) {
                reportJson.set(
                    "units",
                    JsonHandler.getFromInputStream(
                        handlerIO.getInputStreamFromWorkspace(REPORTS + "/" + MIGRATION_UNITS_LIST_IDS + JSON_EXTENSION)
                    )
                );
            } else {
                reportJson.set("units", JsonHandler.createArrayNode());
            }
            HandlerUtils.save(handlerIO, reportJson, reportFileName);
        } catch (
            IOException
            | ContentAddressableStorageNotFoundException
            | ContentAddressableStorageServerException
            | InvalidParseOperationException e
        ) {
            throw new ProcessingException(e);
        }
    }

    private void storeInOffers(String reportFileName) throws ProcessingException {
        try {
            backupService.backupFromWorkspace(reportFileName, DataCategory.REPORT, reportFileName);
        } catch (BackupServiceException e) {
            throw new ProcessingException(e);
        }
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {}
}
