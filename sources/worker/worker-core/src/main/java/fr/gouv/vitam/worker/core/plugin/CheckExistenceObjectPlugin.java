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
package fr.gouv.vitam.worker.core.plugin;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;

/**
 * CheckExistenceObject Plugin.<br>
 */

public class CheckExistenceObjectPlugin extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CheckExistenceObjectPlugin.class);

    private static final String CHECK_EXISTENCE_ID = "AUDIT_FILE_EXISTING";
    private static final String CHECK_PHYSICAL_EXISTING = "AUDIT_FILE_EXISTING.PHYSICAL_OBJECT";
    private static final int OG_NODE_RANK = 0;
    private static final String QUALIFIERS = "#qualifiers";
    public static final String UNITS_UPS = "#unitups";
    private static final String PHYSICAL_MASTER = "PhysicalMaster";

    private final StorageClientFactory storageClientFactory;

    public CheckExistenceObjectPlugin() {
       this(StorageClientFactory.getInstance());
    }

    @VisibleForTesting
    public CheckExistenceObjectPlugin(StorageClientFactory storageClientFactory) {
        this.storageClientFactory = storageClientFactory;
    }

    @Override
    public ItemStatus execute(WorkerParameters params, HandlerIO handler) throws ProcessingException {
        LOGGER.debug(CHECK_EXISTENCE_ID + " in execute");
        ObjectNode evDetData = JsonHandler.createObjectNode();

        final ItemStatus itemStatus = new ItemStatus(CHECK_EXISTENCE_ID);
        final ItemStatus itemStatusCheckPhysical = new ItemStatus(CHECK_PHYSICAL_EXISTING);
        int nbObjectOK = 0;
        int nbObjectKO = 0;
        int nbObjectPhysicalKO = 0;
        try (final StorageClient storageClient = storageClientFactory.getClient()) {
            JsonNode ogNode = (JsonNode) handler.getInput(OG_NODE_RANK);
            JsonNode qualifiersList = ogNode.get(QUALIFIERS);
            JsonNode unitsUpsList = ogNode.get(UNITS_UPS);
            evDetData.set("OriginatingAgency", ogNode.get("#originating_agency"));

            for (JsonNode qualifier : qualifiersList) {
                final String usageName = qualifier.get("qualifier").asText();

                JsonNode versions = qualifier.get("versions");
                ArrayNode errors = JsonHandler.createArrayNode();
                for (JsonNode version : versions) {
                    if (PHYSICAL_MASTER.equals(usageName)) {
                        // Get global information for physical master
                        JsonNode storageInformation = ogNode.get("#storage");
                        final String strategy = storageInformation.get("strategyId").textValue();
                        final List<String> offerIds = new ArrayList<>();
                        for (JsonNode offerId : storageInformation.get("offerIds")) {
                            offerIds.add(offerId.textValue());
                        }
                        if (storageClient.exists(strategy, DataCategory.OBJECT,
                            version.get("#id").asText(), offerIds)) {
                            nbObjectPhysicalKO += 1;
                            ObjectNode objectError = JsonHandler.createObjectNode();
                            objectError.put("IdObj", version.get("#id").textValue());
                            objectError.put("Usage", version.get("DataObjectVersion").textValue());
                            objectError.putArray("IdAU").addAll((ArrayNode) unitsUpsList);
                            errors.add(objectError);
                            evDetData.set("errorsPhysical", errors);
                        } else {
                            nbObjectOK += 1;
                        }
                        continue;

                    }
                    JsonNode storageInformation = version.get("#storage");
                    final String strategy = storageInformation.get("strategyId").textValue();
                    final List<String> offerIds = new ArrayList<>();
                    for (JsonNode offerId : storageInformation.get("offerIds")) {
                        offerIds.add(offerId.textValue());
                    }

                    if (!storageClient.exists(strategy, DataCategory.OBJECT,
                        version.get("#id").asText(), offerIds)) {
                        nbObjectKO += 1;
                        ObjectNode objectError = JsonHandler.createObjectNode();
                        objectError.put("IdObj", version.get("#id").textValue());
                        objectError.put("Usage", version.get("DataObjectVersion").textValue());
                        objectError.putArray("IdAU").addAll((ArrayNode) unitsUpsList);
                        errors.add(objectError);                        
                    } else {
                        nbObjectOK += 1;
                    }
                }
                evDetData.set("errors", errors);
            }
        } catch (StorageServerClientException e) {
            LOGGER.error("Storage server errors : ", e);
            itemStatus.increment(StatusCode.FATAL);
        } catch (NullPointerException e) {
            LOGGER.error("Object does not exist : ", e);
            itemStatus.increment(StatusCode.WARNING);
        }

        if (nbObjectKO > 0) {
            itemStatus.increment(StatusCode.KO);
        }
        if (nbObjectPhysicalKO > 0) {
            itemStatusCheckPhysical.increment(StatusCode.KO);
        }
        if (itemStatus.getGlobalStatus().equals(StatusCode.UNKNOWN)) {
            itemStatus.increment(StatusCode.OK);
        }

        itemStatus.setData("Detail", "Detail = OK : " + nbObjectOK + " KO : " + (nbObjectKO + nbObjectPhysicalKO));
        try {
            evDetData.set("nbKO", JsonHandler.getFromString(String.valueOf(nbObjectKO)));
            evDetData.set("nbPhysicalKO", JsonHandler.getFromString(String.valueOf(nbObjectPhysicalKO)));
            itemStatus.setEvDetailData(JsonHandler.unprettyPrint(evDetData));
            itemStatusCheckPhysical.setEvDetailData(JsonHandler.unprettyPrint(evDetData));
        } catch (InvalidParseOperationException e) {
            LOGGER.error(e);
        }
        
        if (nbObjectPhysicalKO > 0 && nbObjectKO <= 0) {
            return new ItemStatus(CHECK_PHYSICAL_EXISTING).setItemsStatus(CHECK_PHYSICAL_EXISTING, itemStatusCheckPhysical);
        } else {
            return new ItemStatus(CHECK_EXISTENCE_ID).setItemsStatus(CHECK_EXISTENCE_ID, itemStatus);
        }
    }


    /**
     * @return CHECK_EXISTENCE_ID
     */
    public static final String getId() {
        return CHECK_EXISTENCE_ID;
    }

}
