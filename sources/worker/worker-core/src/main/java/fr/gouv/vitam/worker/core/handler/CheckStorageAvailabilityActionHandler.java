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
package fr.gouv.vitam.worker.core.handler;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.model.StorageInformation;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.common.utils.SedaUtils;
import fr.gouv.vitam.worker.common.utils.SedaUtilsFactory;

/**
 * CheckStorageAvailability Handler.<br>
 */
public class CheckStorageAvailabilityActionHandler extends ActionHandler {
    private static final VitamLogger LOGGER =
        VitamLoggerFactory.getInstance(CheckStorageAvailabilityActionHandler.class);

    private static final String HANDLER_ID = "STORAGE_AVAILABILITY_CHECK";

    private static final String DEFAULT_TENANT = "0";
    private static final String DEFAULT_STRATEGY = "default";

    /**
     * Constructor with parameter SedaUtilsFactory
     *
     */
    public CheckStorageAvailabilityActionHandler() {
        // Empty
    }

    /**
     * @return HANDLER_ID
     */
    public static final String getId() {
        return HANDLER_ID;
    }


    @Override

    public ItemStatus execute(WorkerParameters params, HandlerIO handlerIO) {
        checkMandatoryParameters(params);
        final StorageClientFactory storageClientFactory = StorageClientFactory.getInstance();
        final ItemStatus itemStatus = new ItemStatus(HANDLER_ID);
        long totalSizeToBeStored;
        try {
            checkMandatoryIOParameter(handlerIO);
            // TODO P0 extract this information from first parsing
            final SedaUtils sedaUtils = SedaUtilsFactory.create(handlerIO);
            final long objectsSizeInSip = sedaUtils.computeTotalSizeOfObjectsInManifest(params);
            final long manifestSize = sedaUtils.getManifestSize(params);
            totalSizeToBeStored = objectsSizeInSip + manifestSize;
            final JsonNode storageCapacityNode;

            try (final StorageClient storageClient = storageClientFactory.getClient()) {
                storageCapacityNode = storageClient.getStorageInformation(DEFAULT_TENANT, DEFAULT_STRATEGY);
            }

            final StorageInformation information =
                JsonHandler.getFromJsonNode(storageCapacityNode, StorageInformation.class);
            final long storageCapacity = information.getUsableSpace();
            if (storageCapacity >= totalSizeToBeStored) {
                itemStatus.increment(StatusCode.OK);
                itemStatus.setData("requiredSize", storageCapacity);
            } else {
                // error - KO
                itemStatus.increment(StatusCode.KO);
                itemStatus.setData("remainingSize", totalSizeToBeStored - storageCapacity);
            }
        } catch (ProcessingException | StorageNotFoundClientException | StorageServerClientException |
            InvalidParseOperationException e) {
            LOGGER.error(e);
            itemStatus.increment(StatusCode.FATAL);
        }
        return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // TODO P0 Add Workspace:SIP/manifest.xml and check it
    }
}
