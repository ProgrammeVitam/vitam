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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.IngestWorkflowConstants;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.common.utils.DataObjectInfo;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CheckConformityAction Plugin.<br>
 */
public class CheckConformityActionPlugin extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CheckConformityActionPlugin.class);

    public static final String CALC_CHECK = "CALC_CHECK";
    private static final String EMPTY = "EMPTY";
    private static final String INVALID = "INVALID";
    private static final int ALGO_RANK = 0;
    private static final int OG_OUT_RANK = 0;

    /**
     * Constructor
     */
    public CheckConformityActionPlugin() {
        // Nothing
    }

    @Override
    public ItemStatus execute(WorkerParameters params, HandlerIO handlerIO) throws ProcessingException {
        checkMandatoryParameters(params);
        LOGGER.debug("CheckConformityActionHandler running ...");

        // Set default status code to OK
        final ItemStatus itemStatus = new ItemStatus(CALC_CHECK);
        try {
            // Get objectGroup
            final JsonNode jsonOG = handlerIO.getJsonFromWorkspace(
                IngestWorkflowConstants.OBJECT_GROUP_FOLDER + "/" + params.getObjectName()
            );

            handlerIO.addOutputResult(OG_OUT_RANK, jsonOG, true, false);

            final Map<String, DataObjectInfo> binaryObjects = getBinaryObjects(jsonOG);

            boolean oneOrMoreMessagesDigestUpdated = false;
            // checkMessageDigest
            final JsonNode qualifiers = jsonOG.get(SedaConstants.PREFIX_QUALIFIERS);
            if (qualifiers != null) {
                final List<JsonNode> versions = qualifiers.findValues(SedaConstants.TAG_VERSIONS);
                if (versions != null && !versions.isEmpty()) {
                    for (final JsonNode versionsArray : versions) {
                        for (final JsonNode version : versionsArray) {
                            if (version.get(SedaConstants.TAG_PHYSICAL_ID) == null) {
                                final String objectId = version.get(SedaConstants.PREFIX_ID).asText();
                                boolean messagesDigestUpdated = checkMessageDigest(
                                    binaryObjects.get(objectId),
                                    version,
                                    itemStatus,
                                    handlerIO
                                );
                                if (messagesDigestUpdated) {
                                    oneOrMoreMessagesDigestUpdated = true;
                                }
                            }
                        }
                    }
                }
            }

            if (oneOrMoreMessagesDigestUpdated) {
                handlerIO.transferJsonToWorkspace(
                    IngestWorkflowConstants.OBJECT_GROUP_FOLDER,
                    params.getObjectName(),
                    jsonOG,
                    false,
                    false
                );
            }
        } catch (ProcessingException e) {
            LOGGER.error(e);
            itemStatus.increment(StatusCode.FATAL);
        }

        // Occurs only when GOT does not have qualifiers or versions or all versions are Physical
        if (itemStatus.getGlobalStatus().getStatusLevel() == StatusCode.UNKNOWN.getStatusLevel()) {
            itemStatus.increment(StatusCode.OK);
        }
        LOGGER.debug("CheckConformityActionHandler response: " + itemStatus.getGlobalStatus());
        return new ItemStatus(CALC_CHECK).setItemsStatus(CALC_CHECK, itemStatus);
    }

    private boolean checkMessageDigest(
        DataObjectInfo binaryObject,
        JsonNode version,
        ItemStatus itemStatus,
        HandlerIO handlerIO
    ) throws ProcessingException {
        InputStream inputStream = null;
        try {
            final DigestType digestTypeInput = DigestType.fromValue((String) handlerIO.getInput(ALGO_RANK));
            inputStream = handlerIO.getInputStreamFromWorkspace(
                IngestWorkflowConstants.SEDA_FOLDER + File.separator + binaryObject.getUri()
            );
            final Digest vitamDigest = new Digest(digestTypeInput);
            Digest manifestDigest;
            boolean isVitamDigest = false;
            if (!binaryObject.getAlgo().equals(digestTypeInput)) {
                // Begin calculate digest by manifest algo
                manifestDigest = new Digest(binaryObject.getAlgo());
                inputStream = manifestDigest.getDigestInputStream(inputStream);
            } else {
                manifestDigest = vitamDigest;
                isVitamDigest = true;
            }
            // calculate digest by vitam algo
            vitamDigest.update(inputStream);

            final String manifestDigestString = manifestDigest.digestHex();
            final String vitamDigestString = vitamDigest.digestHex();
            String binaryObjectMessageDigest = binaryObject.getMessageDigest();

            boolean messagesDigestUpdated = false;

            LOGGER.debug(
                "DEBUG: \n\t" +
                binaryObject.getAlgo().getName() +
                " " +
                binaryObjectMessageDigest +
                "\n\t" +
                manifestDigestString +
                "\n\t" +
                vitamDigestString
            );

            // create ItemStatus for subtask
            ItemStatus subTaskItemStatus = new ItemStatus(CALC_CHECK);

            // check digest
            if (binaryObjectMessageDigest.isEmpty() || binaryObjectMessageDigest.equals(null)) {
                subTaskItemStatus.increment(StatusCode.KO);
                subTaskItemStatus.setGlobalOutcomeDetailSubcode(EMPTY);
                itemStatus.increment(StatusCode.KO);
            } else if (manifestDigestString.equals(binaryObjectMessageDigest)) {
                subTaskItemStatus.increment(StatusCode.OK);
                itemStatus.increment(StatusCode.OK);
                if (!isVitamDigest) {
                    // update objectGroup json
                    ((ObjectNode) version).put(SedaConstants.TAG_DIGEST, vitamDigestString);
                    ((ObjectNode) version).put(SedaConstants.ALGORITHM, (String) handlerIO.getInput(ALGO_RANK));
                    messagesDigestUpdated = true;
                }

                // define eventDetailData
                ObjectNode jsonNode = JsonHandler.createObjectNode();
                jsonNode.put("MessageDigest", binaryObjectMessageDigest);
                jsonNode.put("Algorithm", binaryObject.getAlgo().getName());
                jsonNode.put("SystemMessageDigest", vitamDigestString);
                jsonNode.put("SystemAlgorithm", (String) handlerIO.getInput(ALGO_RANK));

                subTaskItemStatus.setEvDetailData(JsonHandler.unprettyPrint(jsonNode));
            } else {
                subTaskItemStatus.increment(StatusCode.KO);
                subTaskItemStatus.setGlobalOutcomeDetailSubcode(INVALID);
                itemStatus.increment(StatusCode.KO);
                // Set eventDetailData in KO case
                ObjectNode jsonNode = JsonHandler.createObjectNode();
                jsonNode.put("MessageDigest", binaryObject.getMessageDigest());
                jsonNode.put("Algorithm", binaryObject.getAlgo().getName());
                jsonNode.put("ComputedMessageDigest", manifestDigestString);

                subTaskItemStatus.setEvDetailData(JsonHandler.unprettyPrint(jsonNode));
            }

            itemStatus.setSubTaskStatus(binaryObject.getId(), subTaskItemStatus);

            return messagesDigestUpdated;
        } catch (
            ContentAddressableStorageNotFoundException | ContentAddressableStorageServerException | IOException e
        ) {
            LOGGER.error(e);
            throw new ProcessingException(e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        handler.checkHandlerIO(1, Arrays.asList(new Class[] { String.class }));
    }

    private Map<String, DataObjectInfo> getBinaryObjects(JsonNode jsonOG) throws ProcessingException {
        final Map<String, DataObjectInfo> binaryObjects = new HashMap<>();

        final JsonNode work = jsonOG.get(SedaConstants.PREFIX_WORK);
        final JsonNode qualifiers = work.get(SedaConstants.PREFIX_QUALIFIERS);

        if (qualifiers == null) {
            // KO
            return binaryObjects;
        }

        final List<JsonNode> versions = qualifiers.findValues(SedaConstants.TAG_VERSIONS);
        if (versions == null || versions.isEmpty()) {
            // KO
            return binaryObjects;
        }
        for (final JsonNode version : versions) {
            LOGGER.debug(version.toString());
            for (final JsonNode jsonBinaryObject : version) {
                if (jsonBinaryObject.get(SedaConstants.TAG_PHYSICAL_ID) == null) {
                    binaryObjects.put(
                        jsonBinaryObject.get(SedaConstants.PREFIX_ID).asText(),
                        new DataObjectInfo()
                            .setSize(jsonBinaryObject.get(SedaConstants.TAG_SIZE).asLong())
                            .setId(jsonBinaryObject.get(SedaConstants.PREFIX_ID).asText())
                            .setUri(jsonBinaryObject.get(SedaConstants.TAG_URI).asText())
                            .setMessageDigest(jsonBinaryObject.get(SedaConstants.TAG_DIGEST).asText())
                            .setAlgo(DigestType.fromValue(jsonBinaryObject.get(SedaConstants.ALGORITHM).asText()))
                    );
                }
            }
        }
        // OK
        return binaryObjects;
    }
}
