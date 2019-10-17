/**
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
package fr.gouv.vitam.worker.core.plugin.dip;

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.common.CompressInformation;

import java.util.Collections;

import static fr.gouv.vitam.common.model.IngestWorkflowConstants.SEDA_FILE;

/**
 * ZIP the ExportsPurge and move it from workspace to storage
 */
public class StoreExports extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(StoreExports.class);

    private static final String STORE_DIP = "STORE_DIP";
    private static final String TRANSFER_DIP = "TRANSFER_DIP";
    static String ARCHIVE_TRANSFER = "ARCHIVE_TRANSFER";
    static final String CONTENT = "Content";
    static final String DIP_CONTAINER = "DIP";
    static final String TRANSFER_CONTAINER = "TRANSFER";


    @Override
    public ItemStatus execute(WorkerParameters params, HandlerIO handler)
        throws ProcessingException, ContentAddressableStorageServerException {

        String container;
        String status_action;
        if(params.getWorkflowIdentifier().equals(ARCHIVE_TRANSFER))
        {
            container = TRANSFER_CONTAINER;
            status_action = TRANSFER_DIP;
        } else {
            container = DIP_CONTAINER;
            status_action = STORE_DIP;
        }

        final ItemStatus itemStatus = new ItemStatus(status_action);

        try {
            String dipTenantFolder = Integer.toString(VitamThreadUtils.getVitamSession().getTenantId());
            String dipZipFileName = params.getContainerName();

            zipWorkspace(handler, dipTenantFolder, dipZipFileName, container, SEDA_FILE, CONTENT);
            itemStatus.increment(StatusCode.OK);
        } catch (ContentAddressableStorageException e) {
            throw new ProcessingException(e);
        }
        return new ItemStatus(status_action).setItemsStatus(status_action, itemStatus);
    }

    private void zipWorkspace(HandlerIO handler, String outputDir, String outputFile, String container, String... inputFiles)
        throws ContentAddressableStorageException {

        LOGGER.debug("Try to compress into workspace...");
        try (WorkspaceClient workspaceClient = handler.getWorkspaceClientFactory().getClient()) {
            // Ensure target folder exists
            workspaceClient.createContainer(container);
            workspaceClient.createFolder(container, outputDir);

            // compress
            CompressInformation compressInformation = new CompressInformation();
            Collections.addAll(compressInformation.getFiles(), inputFiles);
            compressInformation.setOutputFile(outputDir + "/" + outputFile);
            compressInformation.setOutputContainer(container);
            workspaceClient.compress(handler.getContainerName(), compressInformation);
        }
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // Nothing to check
    }
}
