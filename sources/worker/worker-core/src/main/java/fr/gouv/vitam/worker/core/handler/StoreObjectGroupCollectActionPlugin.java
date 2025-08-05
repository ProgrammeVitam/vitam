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

package fr.gouv.vitam.worker.core.handler;

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.common.BulkMoveEntry;
import fr.gouv.vitam.workspace.common.BulkMoveRequest;
import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Handler that copies all content from the SIP folder to the container root.
 * This allows the SIP generation to work properly.
 */
public class StoreObjectGroupCollectActionPlugin extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(StoreObjectGroupCollectActionPlugin.class);

    private static final String HANDLER_ID = "OBJ_STORAGE_COLLECT";
    private static final String FOLDER_SIP = "SIP";
    private static final String CONTENT = "Content/";
    private static final int BATCH_SIZE = 1000;

    /**
     * Default constructor
     */
    public StoreObjectGroupCollectActionPlugin() {}

    /**
     * @return HANDLER_ID
     */
    public static String getId() {
        return HANDLER_ID;
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler) throws ProcessingException {
        LOGGER.debug("Start StoreObjectGroupCollectActionPlugin");
        final ItemStatus itemStatus = new ItemStatus(HANDLER_ID);

        try {
            String containerName = param.getContainerName();
            LOGGER.info("Moving SIP content to container root for container: " + containerName);

            try (WorkspaceClient workspaceClient = handler.getWorkspaceCollectClient()) {
                boolean hasMoreFiles = true;
                int totalFilesProcessed = 0;

                // Process files in batches until no more files are found
                while (hasMoreFiles) {
                    // Get batch of files from SIP folder
                    RequestResponse<URI> listResponse = workspaceClient.getListUriDigitalObjectFromFolder(
                        containerName,
                        FOLDER_SIP,
                        BATCH_SIZE
                    );
                    List<URI> sipFiles = ((RequestResponseOK<URI>) listResponse).getResults();

                    if (sipFiles == null || sipFiles.isEmpty()) {
                        if (totalFilesProcessed == 0) {
                            LOGGER.warn("No files found in SIP folder for container: " + containerName);
                            itemStatus.increment(StatusCode.WARNING);
                            return itemStatus;
                        } else {
                            // No more files to process
                            hasMoreFiles = false;
                            continue;
                        }
                    }

                    LOGGER.info("Found " + sipFiles.size() + " files in current batch");
                    totalFilesProcessed += sipFiles.size();

                    // Prepare source / destination paths for bulk move
                    List<BulkMoveEntry> bulkMoveEntries = new ArrayList<>();

                    for (URI fileUri : sipFiles) {
                        String filePath = fileUri.getPath();

                        // Skip manifest.xml files
                        if (filePath.equalsIgnoreCase("manifest.xml")) {
                            LOGGER.debug("Skipping manifest.xml file");
                            continue;
                        }

                        // Ensure file is within /Content subfolder
                        if (!StringUtils.startsWithIgnoreCase(filePath, CONTENT)) {
                            LOGGER.warn(
                                "Invalid file path '" +
                                filePath +
                                "'. Only manifest.xml and Content/* files are expected."
                            );
                            itemStatus.increment(StatusCode.KO);
                            return itemStatus;
                        }

                        // Add to source and destination lists
                        bulkMoveEntries.add(new BulkMoveEntry(FOLDER_SIP + "/" + filePath, filePath));
                    }

                    if (!bulkMoveEntries.isEmpty()) {
                        // Process this batch
                        LOGGER.debug("Moving " + bulkMoveEntries.size() + " files from SIP folder to container root");

                        workspaceClient.bulkMove(containerName, new BulkMoveRequest(bulkMoveEntries));
                        LOGGER.debug("Successfully moved batch of " + bulkMoveEntries.size() + " files");
                    }

                    // If we got fewer files than the batch size, we've processed all files
                    if (sipFiles.size() < BATCH_SIZE) {
                        hasMoreFiles = false;
                    }
                }

                LOGGER.info(
                    "Successfully moved all " + totalFilesProcessed + " files from SIP folder to container root"
                );

                // Delete the SIP folder after moving all files
                try {
                    LOGGER.info("Deleting SIP folder for container: " + containerName);
                    workspaceClient.deleteFolder(containerName, FOLDER_SIP);
                    LOGGER.info("Successfully deleted SIP folder");
                } catch (ContentAddressableStorageNotFoundException e) {
                    LOGGER.warn("SIP folder not found for deletion: " + e.getMessage());
                }

                itemStatus.increment(StatusCode.OK);
            }

            return itemStatus;
        } catch (Exception e) {
            LOGGER.error("An exception occurred in StoreObjectGroupCollectActionPlugin: " + e.getMessage(), e);
            itemStatus.increment(StatusCode.FATAL);
            throw new ProcessingException(
                "An exception occurred in StoreObjectGroupCollectActionPlugin: " + e.getMessage(),
                e
            );
        }
    }
}
