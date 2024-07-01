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
package fr.gouv.vitam.worker.core.plugin.transfer.reply;

import com.google.common.annotations.VisibleForTesting;
import fr.gouv.culture.archivesdefrance.seda.v2.ArchiveTransferReplyType;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageAlreadyExistsClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.storage.engine.common.model.response.StoredInfoResult;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.plugin.BinaryEventData;
import fr.gouv.vitam.worker.core.utils.PluginHelper.EventDetails;

import java.util.Collections;

import static fr.gouv.vitam.common.model.StatusCode.FATAL;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.common.model.StatusCode.WARNING;
import static fr.gouv.vitam.storage.engine.common.model.DataCategory.ARCHIVAL_TRANSFER_REPLY;
import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;

public class SaveAtrPlugin extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(SaveAtrPlugin.class);
    public static final String PLUGIN_NAME = "SAVE_ARCHIVAL_TRANSFER_REPLY";

    private final StorageClientFactory storageClientFactory;

    public SaveAtrPlugin() {
        this(StorageClientFactory.getInstance());
    }

    @VisibleForTesting
    public SaveAtrPlugin(StorageClientFactory storageClientFactory) {
        this.storageClientFactory = storageClientFactory;
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler) throws ProcessingException {
        ArchiveTransferReplyType atr = (ArchiveTransferReplyType) handler.getInput(0);
        try (StorageClient storageClient = storageClientFactory.getClient()) {
            String messageIdentifier = atr.getMessageIdentifier();

            ObjectDescription description = getDescription(messageIdentifier, handler.getContainerName());

            StoredInfoResult storedInfo = storageClient.storeFileFromWorkspace(
                VitamConfiguration.getDefaultStrategy(),
                description.getType(),
                description.getObjectName(),
                description
            );

            handler.addOutputResult(0, atr);

            return buildItemStatus(
                PLUGIN_NAME,
                OK,
                Collections.singletonMap(messageIdentifier, BinaryEventData.from(storedInfo))
            );
        } catch (StorageAlreadyExistsClientException e) {
            LOGGER.warn(e);
            return buildItemStatus(
                PLUGIN_NAME,
                WARNING,
                EventDetails.of(e.getMessage(), "ATR already exists but it will continue.")
            );
        } catch (StorageNotFoundClientException | StorageServerClientException e) {
            LOGGER.error(e);
            return buildItemStatus(PLUGIN_NAME, FATAL, EventDetails.of(e.getMessage()));
        }
    }

    private ObjectDescription getDescription(String messageIdentifier, String containerName) {
        ObjectDescription description = new ObjectDescription();
        description.setWorkspaceContainerGUID(containerName);
        description.setObjectName(messageIdentifier);
        description.setWorkspaceObjectURI("ATR-for-transfer-reply-in-workspace.xml");
        description.setType(ARCHIVAL_TRANSFER_REPLY);
        return description;
    }
}
