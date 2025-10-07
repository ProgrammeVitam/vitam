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
package fr.gouv.vitam.worker.core.plugin;

import fr.gouv.vitam.collect.common.enums.TransactionStatus;
import fr.gouv.vitam.collect.internal.client.CollectInternalClient;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.workspace.client.WorkspaceClient;

/**
 * Collect ingest finalization plugin.
 */
public class CollectIngestFinalizationPlugin extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CollectIngestFinalizationPlugin.class);

    private static final String COLLECT_INGEST_FINALISATION = "COLLECT_INGEST_FINALISATION";

    private static final String FOLDER_SIP = "SIP";

    public CollectIngestFinalizationPlugin() {}

    public static String getId() {
        return COLLECT_INGEST_FINALISATION;
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler) throws ProcessingException {
        StatusCode workflowStatus = StatusCode.valueOf(param.getWorkflowStatusKo());

        String containerName = param.getContainerName();
        if (workflowStatus.isGreaterOrEqualToKo()) {
            LOGGER.error("Workflow status is " + workflowStatus + ". Updating transaction status to KO");
            try (CollectInternalClient collectInternalClient = handler.getCollectInternalClient()) {
                collectInternalClient.changeTransactionStatus(containerName, TransactionStatus.KO);
            } catch (VitamClientException e) {
                LOGGER.error("An error occurred during collect ingest finalization", e);
                final ItemStatus itemStatus = new ItemStatus(COLLECT_INGEST_FINALISATION);
                itemStatus.increment(StatusCode.FATAL);
                return itemStatus;
            }
        }

        // Delete the SIP folder after moving the files
        try (WorkspaceClient workspaceClient = handler.getWorkspaceCollectClient()) {
            LOGGER.debug("Deleting SIP folder from container: " + containerName);
            workspaceClient.deleteFolder(containerName, FOLDER_SIP);
            LOGGER.debug("Successfully deleted SIP folder from container: " + containerName);
        } catch (Exception e) {
            LOGGER.error("Error deleting SIP folder from container: " + containerName, e);
            final ItemStatus itemStatus = new ItemStatus(COLLECT_INGEST_FINALISATION);
            itemStatus.increment(StatusCode.FATAL);
            return itemStatus;
        }

        final ItemStatus itemStatus = new ItemStatus(COLLECT_INGEST_FINALISATION);
        itemStatus.increment(StatusCode.OK);
        return itemStatus;
    }
}
