package fr.gouv.vitam.worker.core.plugin;

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
import fr.gouv.vitam.storage.engine.client.exception.StorageClientException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

import java.util.ArrayList;
import java.util.List;

/**
 * Check Integrity of object
 */
public class CheckIntegrityObjectPlugin extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CheckIntegrityObjectPlugin.class);

    private static final String CHECK_INTEGRITY_ID = "AUDIT_FILE_INTEGRITY";
    public static final String QUALIFIERS = "#qualifiers";
    public static final String UNITS_UPS = "#unitups";
    private static final int OG_NODE_RANK = 0;

    private StorageClientFactory storageClientFactory;

    /**
     * Empty constructor CheckIntegrityObjectPlugin
     */
    public CheckIntegrityObjectPlugin() {
        this(StorageClientFactory.getInstance());
    }

    @VisibleForTesting
    public CheckIntegrityObjectPlugin(StorageClientFactory storageClientFactory) {
        this.storageClientFactory = storageClientFactory;
    }


    @Override
    public ItemStatus execute(WorkerParameters params, HandlerIO handler)
        throws ProcessingException, ContentAddressableStorageServerException {
        LOGGER.debug(CHECK_INTEGRITY_ID + " in execute");
        ObjectNode evDetData = JsonHandler.createObjectNode();

        final ItemStatus itemStatus = new ItemStatus(CHECK_INTEGRITY_ID);
        int nbObjectOK = 0;
        int nbObjectKO = 0;
        try (final StorageClient storageClient = storageClientFactory.getClient()) {
            JsonNode ogNode = (JsonNode) handler.getInput(OG_NODE_RANK);
            JsonNode qualifiersList = ogNode.get(QUALIFIERS);
            JsonNode unitsUpsList = ogNode.get(UNITS_UPS);
            evDetData.set("OriginatingAgency", ogNode.get("#originating_agency"));

            for (JsonNode qualifier : qualifiersList) {
                if (qualifier.get("qualifier").asText().equals("PhysicalMaster")) {
                    continue;
                }
                JsonNode versions = qualifier.get("versions");
                ArrayNode errors = JsonHandler.createArrayNode();
                for (JsonNode version : versions) {
                    boolean checkDigest = true;
                    String messageDigest = null;
                    if (version.has("MessageDigest")) {
                        messageDigest = version.get("MessageDigest").asText();
                    } else {
                        nbObjectKO += 1;
                        continue;
                    }

                    JsonNode storageInformation = version.get("#storage");
                    final String strategy = storageInformation.get("strategyId").textValue();
                    final List<String> offerIds = new ArrayList<>();
                    for (JsonNode offerId : storageInformation.get("offerIds")) {
                        offerIds.add(offerId.textValue());
                    }

                    JsonNode offerToMetadata = storageClient.getInformation(strategy, DataCategory.OBJECT,
                        version.get("#id").asText(), offerIds, true);
                    for (String offerId : offerIds) {
                        String digest = null;
                        JsonNode metadata = offerToMetadata.findValue(offerId);
                        if (metadata != null) {
                            digest = metadata.get("digest").asText();
                        } else {
                            checkDigest = false;
                            continue;
                        }

                        checkDigest = checkDigest && messageDigest.equals(digest);
                    }

                    if (checkDigest) {
                        nbObjectOK += 1;
                    } else {
                        nbObjectKO += 1;
                        ObjectNode objectError = JsonHandler.createObjectNode();
                        objectError.put("IdObj", version.get("#id").textValue());
                        objectError.put("Usage", version.get("DataObjectVersion").textValue());
                        objectError.putArray("IdAU").addAll((ArrayNode) unitsUpsList);
                        errors.add(objectError);
                    }
                }
                evDetData.set("errors", errors);
            }
        } catch (StorageClientException e) {
            LOGGER.error("Storage server errors : ", e);
            itemStatus.increment(StatusCode.FATAL);
            return new ItemStatus(CHECK_INTEGRITY_ID).setItemsStatus(CHECK_INTEGRITY_ID, itemStatus);
        }

        if (nbObjectKO > 0) {
            itemStatus.increment(StatusCode.KO);
        }

        if (itemStatus.getGlobalStatus().equals(StatusCode.UNKNOWN)) {
            itemStatus.increment(StatusCode.OK);
        }

        itemStatus.setData("Detail", "Detail = OK : " + nbObjectOK + " KO : " + nbObjectKO);
        try {
            evDetData.set("nbKO", JsonHandler.getFromString(String.valueOf(nbObjectKO)));
            itemStatus.setEvDetailData(JsonHandler.unprettyPrint(evDetData));
        } catch (InvalidParseOperationException e) {
            LOGGER.error(e);
        }

        return new ItemStatus(CHECK_INTEGRITY_ID).setItemsStatus(CHECK_INTEGRITY_ID, itemStatus);
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // TODO Auto-generated method stub

    }

    /**
     * @return CHECK_INTEGRITY_ID
     */
    public static final String getId() {
        return CHECK_INTEGRITY_ID;
    }

}
