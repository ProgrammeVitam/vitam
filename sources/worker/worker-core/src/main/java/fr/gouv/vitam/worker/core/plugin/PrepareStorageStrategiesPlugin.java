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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageStrategy;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;

import java.io.File;

import static fr.gouv.vitam.common.json.JsonHandler.createObjectNode;
import static fr.gouv.vitam.common.model.StatusCode.FATAL;
import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;

public class PrepareStorageStrategiesPlugin extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(PrepareStorageStrategiesPlugin.class);

    private static final String PLUGIN_NAME = "PREPARE_STORAGE_STRATEGIES";
    private static final int STRATEGIES_OUT_RANK = 0;

    private final StorageClientFactory storageClientFactory;

    public PrepareStorageStrategiesPlugin() {
        this(StorageClientFactory.getInstance());
    }

    @VisibleForTesting
    PrepareStorageStrategiesPlugin(StorageClientFactory storageClientFactory) {
        this.storageClientFactory = storageClientFactory;
    }

    @Override
    public ItemStatus execute(WorkerParameters params, HandlerIO handlerIO) throws ProcessingException {
        try {
            storeStrategies(handlerIO);
            return buildItemStatus(PLUGIN_NAME, StatusCode.OK, createObjectNode());
        } catch (ProcessingException e) {
            LOGGER.error(String.format("Prepare Storage Strategies failed with status [%s]", FATAL), e);
            ObjectNode error = createObjectNode().put("error", e.getMessage());
            return buildItemStatus(PLUGIN_NAME, FATAL, error);
        }
    }

    private void storeStrategies(HandlerIO handlerIO) throws ProcessingException {
        try (final StorageClient storageClient = storageClientFactory.getClient()) {
            RequestResponse<StorageStrategy> storageStrategies = storageClient.getStorageStrategies();
            if (storageStrategies.isOk()) {
                File tempFile = handlerIO.getNewLocalFile(handlerIO.getOutput(STRATEGIES_OUT_RANK).getPath());
                JsonHandler.writeAsFile(
                    ((RequestResponseOK<StorageStrategy>) storageStrategies).getResultsAsJsonNodes(),
                    tempFile
                );
                handlerIO.addOutputResult(STRATEGIES_OUT_RANK, tempFile, true, false);
            } else {
                throw new StorageServerClientException("Exception while retrieving storage strategies");
            }
        } catch (InvalidParseOperationException | StorageServerClientException e) {
            LOGGER.error("Storage server errors : ", e);
            throw new ProcessingException(String.format("Storage server errors : %s", e));
        }
    }
}
