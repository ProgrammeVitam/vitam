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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.ContractsDetailsModel;
import fr.gouv.vitam.common.model.administration.ManagementContractModel;
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
import org.apache.commons.lang3.StringUtils;

import java.io.File;

/**
 * CheckStorageAvailability Handler.<br>
 */
public class CheckStorageAvailabilityActionHandler extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(
        CheckStorageAvailabilityActionHandler.class
    );

    private static final String HANDLER_ID = "STORAGE_AVAILABILITY_CHECK";
    private static final int REFERENTIAL_INGEST_CONTRACT_IN_RANK = 0;

    private final StorageClientFactory storageClientFactory;
    private final SedaUtilsFactory sedaUtilsFactory;

    /**
     * Constructor with parameter SedaUtilsFactory
     */
    public CheckStorageAvailabilityActionHandler() {
        this(StorageClientFactory.getInstance(), SedaUtilsFactory.getInstance());
    }

    @VisibleForTesting
    public CheckStorageAvailabilityActionHandler(
        StorageClientFactory storageClientFactory,
        SedaUtilsFactory sedaUtilsFactory
    ) {
        this.storageClientFactory = storageClientFactory;
        this.sedaUtilsFactory = sedaUtilsFactory;
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
        final ItemStatus itemStatus = new ItemStatus(HANDLER_ID);
        try {
            checkMandatoryIOParameter(handlerIO);
            ManagementContractModel managementContract = loadManagementContractFromWorkspace(handlerIO);

            // TODO P0 extract this information from first parsing
            final SedaUtils sedaUtils = sedaUtilsFactory.createSedaUtils(handlerIO);
            final long objectsSizeInSip = sedaUtils.computeTotalSizeOfObjectsInManifest(params);

            String strategyId = VitamConfiguration.getDefaultStrategy();
            if (
                managementContract != null &&
                managementContract.getStorage() != null &&
                StringUtils.isNotBlank(managementContract.getStorage().getObjectStrategy())
            ) {
                strategyId = managementContract.getStorage().getObjectStrategy();
            }

            final JsonNode storageCapacityNode;
            try (final StorageClient storageClient = storageClientFactory.getClient()) {
                storageCapacityNode = storageClient.getStorageInformation(strategyId);
            }

            // TODO P1 fix getcontainerInformation in storage
            if (storageCapacityNode == null) {
                LOGGER.warn("storage capacity account information not found for object strategy");
                itemStatus.increment(StatusCode.OK);
                return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
            }
            final StorageInformation[] informations = JsonHandler.getFromJsonNode(
                storageCapacityNode.get("capacities"),
                StorageInformation[].class
            );
            if (informations.length > 0) {
                for (StorageInformation information : informations) {
                    ItemStatus is = new ItemStatus(HANDLER_ID);
                    ObjectNode info = JsonHandler.createObjectNode();
                    // if usable space not specified getUsableSpace() return -1
                    if (information.getUsableSpace() >= objectsSizeInSip || information.getUsableSpace() == -1) {
                        info.put(information.getOfferId(), StatusCode.OK.name());
                        is.increment(StatusCode.OK);
                    } else {
                        LOGGER.error(
                            "storage capacity invalid on offer {} of object strategy : usableSpace={}, totalSizeToBeStored={}",
                            information.getOfferId(),
                            information.getUsableSpace(),
                            objectsSizeInSip
                        );
                        info.put(information.getOfferId(), StatusCode.KO.name());
                        info.put(information.getOfferId() + "_usableSpace", information.getUsableSpace());
                        info.put(information.getOfferId() + "_totalSizeToBeStored", objectsSizeInSip);
                        is.increment(StatusCode.KO);
                    }
                    is.setEvDetailData(info.toString());
                    itemStatus.setItemsStatus(HANDLER_ID, is);
                }
            } else {
                LOGGER.warn("No information found for offers");
                itemStatus.increment(StatusCode.OK);
                return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
            }
        } catch (
            ProcessingException
            | StorageNotFoundClientException
            | StorageServerClientException
            | InvalidParseOperationException e
        ) {
            LOGGER.error(e);
            itemStatus.increment(StatusCode.FATAL);
        }
        return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
    }

    private ManagementContractModel loadManagementContractFromWorkspace(HandlerIO handlerIO)
        throws InvalidParseOperationException {
        ContractsDetailsModel contractsDetailsModel = JsonHandler.getFromFile(
            (File) handlerIO.getInput(REFERENTIAL_INGEST_CONTRACT_IN_RANK),
            ContractsDetailsModel.class
        );
        return contractsDetailsModel.getManagementContractModel();
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // TODO P0 Add Workspace:SIP/manifest.xml and check it
    }
}
