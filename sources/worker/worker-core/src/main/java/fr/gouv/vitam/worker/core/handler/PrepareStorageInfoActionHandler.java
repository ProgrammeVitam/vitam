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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.SedaConstants;
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

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * PrepareStorageInfoActionHandler Handler.<br>
 */
public class PrepareStorageInfoActionHandler extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(PrepareStorageInfoActionHandler.class);

    private static final String HANDLER_ID = "PREPARE_STORAGE_INFO";

    private static final int STORAGE_INFO_OUT_RANK = 0;
    private static final int REFERENTIAL_INGEST_CONTRACT_IN_RANK = 0;

    private final StorageClientFactory storageClientFactory;

    /**
     * Constructor with parameter SedaUtilsFactory
     */
    public PrepareStorageInfoActionHandler() {
        this(StorageClientFactory.getInstance());
    }

    /**
     * Useful for inject mock in test class
     *
     * @param storageClientFactory instance of storageClientFactory or mock
     */
    @VisibleForTesting
    public PrepareStorageInfoActionHandler(StorageClientFactory storageClientFactory) {
        this.storageClientFactory = storageClientFactory;
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

            Map<String, JsonNode> storageCapacityNodeByStrategyId = new HashMap<>();
            try (final StorageClient storageClient = storageClientFactory.getClient()) {
                storageCapacityNodeByStrategyId.put(
                    VitamConfiguration.getDefaultStrategy(),
                    storageClient.getStorageInformation(VitamConfiguration.getDefaultStrategy())
                );
                if (managementContract != null && managementContract.getStorage() != null) {
                    if (
                        managementContract.getStorage().getUnitStrategy() != null &&
                        !storageCapacityNodeByStrategyId.containsKey(managementContract.getStorage().getUnitStrategy())
                    ) {
                        storageCapacityNodeByStrategyId.put(
                            managementContract.getStorage().getUnitStrategy(),
                            storageClient.getStorageInformation(managementContract.getStorage().getUnitStrategy())
                        );
                    }
                    if (
                        managementContract.getStorage().getObjectGroupStrategy() != null &&
                        !storageCapacityNodeByStrategyId.containsKey(
                            managementContract.getStorage().getObjectGroupStrategy()
                        )
                    ) {
                        storageCapacityNodeByStrategyId.put(
                            managementContract.getStorage().getObjectGroupStrategy(),
                            storageClient.getStorageInformation(
                                managementContract.getStorage().getObjectGroupStrategy()
                            )
                        );
                    }
                    if (
                        managementContract.getStorage().getObjectStrategy() != null &&
                        !storageCapacityNodeByStrategyId.containsKey(
                            managementContract.getStorage().getObjectStrategy()
                        )
                    ) {
                        storageCapacityNodeByStrategyId.put(
                            managementContract.getStorage().getObjectStrategy(),
                            storageClient.getStorageInformation(managementContract.getStorage().getObjectStrategy())
                        );
                    }
                }
            }

            ObjectNode strategiesInformation = JsonHandler.createObjectNode();
            for (Entry<String, JsonNode> stategyStorageCapacities : storageCapacityNodeByStrategyId.entrySet()) {
                final StorageInformation[] storageInformation = JsonHandler.getFromJsonNode(
                    stategyStorageCapacities.getValue().get("capacities"),
                    StorageInformation[].class
                );
                strategiesInformation.set(
                    stategyStorageCapacities.getKey(),
                    generateStorageInfoNode(stategyStorageCapacities.getKey(), storageInformation)
                );
            }

            storeStrategiesStorageInfo(handlerIO, strategiesInformation);

            itemStatus.increment(StatusCode.OK);
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

    private JsonNode generateStorageInfoNode(String strategyId, StorageInformation[] storageInformation)
        throws InvalidParseOperationException, ProcessingException {
        ObjectNode storageInfo = JsonHandler.createObjectNode();

        int nbCopies = storageInformation.length > 0 ? storageInformation[0].getNbCopies() : 0;

        storageInfo.put(SedaConstants.TAG_NB, nbCopies);
        ArrayNode offerIds = JsonHandler.createArrayNode();
        for (StorageInformation information : storageInformation) {
            offerIds.add(information.getOfferId());
        }
        storageInfo.set(SedaConstants.OFFER_IDS, offerIds);
        storageInfo.put(SedaConstants.STRATEGY_ID, strategyId);
        return storageInfo;
    }

    private void storeStrategiesStorageInfo(HandlerIO handlerIO, JsonNode strategiesInformation)
        throws InvalidParseOperationException, ProcessingException {
        File tempFile = handlerIO.getNewLocalFile(handlerIO.getOutput(STORAGE_INFO_OUT_RANK).getPath());
        JsonHandler.writeAsFile(strategiesInformation, tempFile);
        handlerIO.addOutputResult(STORAGE_INFO_OUT_RANK, tempFile, true, false);
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
        // NoOp
    }
}
