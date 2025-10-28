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

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.IngestWorkflowConstants;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.common.BulkMoveEntry;
import fr.gouv.vitam.workspace.common.BulkMoveRequest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Handler that copies all content from the SIP folder to the container root.
 * This allows the SIP generation to work properly.
 */
public class StoreObjectGroupCollectActionPlugin extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(StoreObjectGroupCollectActionPlugin.class);

    private static final String HANDLER_ID = "OBJ_STORAGE_COLLECT";
    private static final String FOLDER_SIP = "SIP";
    private static final int OG_OUT_RANK = 0;

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
            LOGGER.debug("Moving SIP content to container root for container: " + containerName);

            try (WorkspaceClient workspaceClient = handler.getWorkspaceCollectClient()) {
                Set<String> allSipFiles = new HashSet<>();

                // get list of object group's objects
                for (String objectName : param.getObjectNameList()) {
                    allSipFiles.addAll(getListUris(param.getContainerName(), objectName, handler));
                }

                if (allSipFiles.isEmpty()) {
                    LOGGER.warn("No files found in SIP folder for container: " + containerName);
                    itemStatus.increment(StatusCode.WARNING);
                    return itemStatus;
                }

                // Prepare source / destination paths for bulk move
                List<BulkMoveEntry> bulkMoveEntries = new ArrayList<>();

                for (String filePath : allSipFiles) {
                    // Add to source and destination lists
                    bulkMoveEntries.add(new BulkMoveEntry(FOLDER_SIP + "/" + filePath, filePath));
                }

                // Process this batch
                LOGGER.debug("Moving " + bulkMoveEntries.size() + " files from SIP folder to container root");
                workspaceClient.bulkMove(containerName, new BulkMoveRequest(bulkMoveEntries));
                LOGGER.debug("Successfully moved of " + bulkMoveEntries.size() + " files");

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

    /**
     * Get the list of objects linked to the current object group
     *
     * @param containerId
     * @param objectName
     * @param handlerIO
     * @return the list of object guid and corresponding Json
     * @throws ProcessingException throws when error occurs while retrieving the object group file from workspace
     */
    private Set<String> getListUris(String containerId, String objectName, HandlerIO handlerIO)
        throws ProcessingException {
        Set<String> sipFiles = new HashSet<>();

        ParametersChecker.checkParameter("Container id is a mandatory parameter", containerId);
        ParametersChecker.checkParameter("ObjectName id is a mandatory parameter", objectName);
        // Get objectGroup objects ids
        handlerIO.setCurrentObjectId(objectName);
        JsonNode jsonOG = handlerIO.getJsonFromWorkspace(
            IngestWorkflowConstants.OBJECT_GROUP_FOLDER + "/" + objectName
        );
        handlerIO.addOutputResult(OG_OUT_RANK, jsonOG, true, false);

        // Filter on objectGroup objects ids to retrieve only binary objects
        // informations linked to the ObjectGroup
        final JsonNode work = jsonOG.get(SedaConstants.PREFIX_WORK);
        final JsonNode qualifiers = work.get(SedaConstants.PREFIX_QUALIFIERS);
        if (qualifiers == null) {
            return sipFiles;
        }

        final List<JsonNode> versions = qualifiers.findValues(SedaConstants.TAG_VERSIONS);
        if (versions == null || versions.isEmpty()) {
            return sipFiles;
        }
        for (final JsonNode version : versions) {
            for (final JsonNode binaryObject : version) {
                if (binaryObject.get(SedaConstants.TAG_PHYSICAL_ID) == null) {
                    sipFiles.add(binaryObject.get(SedaConstants.TAG_URI).asText());
                }
            }
        }
        return sipFiles;
    }
}
