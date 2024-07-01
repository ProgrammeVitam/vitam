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

package fr.gouv.vitam.worker.core.plugin.traceability;

import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.WorkspaceConstants;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import static fr.gouv.vitam.common.model.StatusCode.KO;
import static fr.gouv.vitam.common.model.WorkspaceConstants.ERROR_FLAG;
import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;

public class ExtractSecureTraceabilityDataFilePlugin extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(
        ExtractSecureTraceabilityDataFilePlugin.class
    );
    private static final String PLUGIN_NAME = "EXTRACT_SECURE_TRACEABILITY_DATA_FILE";
    private static final int TRACEABILITY_FILE_IN_RANK = 0;

    private final WorkspaceClientFactory workspaceClientFactory;

    @SuppressWarnings("unused")
    public ExtractSecureTraceabilityDataFilePlugin() {
        this(WorkspaceClientFactory.getInstance());
    }

    public ExtractSecureTraceabilityDataFilePlugin(WorkspaceClientFactory workspaceClientFactory) {
        this.workspaceClientFactory = workspaceClientFactory;
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler) throws ProcessingException {
        if (handler.isExistingFileInWorkspace(param.getObjectName() + File.separator + ERROR_FLAG)) {
            return buildItemStatus(PLUGIN_NAME, KO);
        }
        try (WorkspaceClient workspaceClient = workspaceClientFactory.getClient()) {
            File traceabilityFile = (File) handler.getInput(TRACEABILITY_FILE_IN_RANK);
            String zipContainer =
                WorkspaceConstants.TRACEABILITY_OPERATION_DIRECTORY + File.separator + param.getObjectName();

            // Idempotency - we check if a folder exist
            if (workspaceClient.isExistingFolder(param.getContainerName(), zipContainer)) {
                workspaceClient.deleteObject(param.getContainerName(), zipContainer);
                LOGGER.warn("folder was already existing, re-extracting audit zip");
            }

            // 2- unzip file
            workspaceClient.uncompressObject(
                param.getContainerName(),
                zipContainer,
                CommonMediaType.ZIP,
                new FileInputStream(traceabilityFile)
            );
        } catch (ContentAddressableStorageException | IOException e) {
            LOGGER.error(e);
            return buildItemStatus(PLUGIN_NAME, StatusCode.FATAL, null);
        }
        return buildItemStatus(PLUGIN_NAME, StatusCode.OK, null);
    }
}
