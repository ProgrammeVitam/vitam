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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.common.utils.DataObjectInfo;
import fr.gouv.vitam.worker.common.utils.IngestWorkflowConstants;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

/**
 * CheckConformityAction Plugin.<br>
 *
 */
public class CheckConformityActionPlugin extends ActionHandler {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CheckConformityActionPlugin.class);

    private static final String CALC_CHECK = "CALC_CHECK";
    private HandlerIO handlerIO;
    private boolean oneOrMoreMessagesDigestUpdated = false;
    private static final int ALGO_RANK = 0;

    /**
     * Constructor
     */
    public CheckConformityActionPlugin() {
        // Nothing
    }


    @Override
    public ItemStatus execute(WorkerParameters params, HandlerIO handler) throws ProcessingException {
        checkMandatoryParameters(params);
        handlerIO = handler;
        LOGGER.debug("CheckConformityActionHandler running ...");

        final ItemStatus itemStatus = new ItemStatus(CALC_CHECK);
        try {
            // Get objectGroup
            final JsonNode jsonOG = handlerIO.getJsonFromWorkspace(
                IngestWorkflowConstants.OBJECT_GROUP_FOLDER + "/" + params.getObjectName());

            final Map<String, DataObjectInfo> binaryObjects = getBinaryObjects(jsonOG);

            // checkMessageDigest
            final JsonNode qualifiers = jsonOG.get(SedaConstants.PREFIX_QUALIFIERS);
            if (qualifiers != null) {
                final List<JsonNode> versions = qualifiers.findValues(SedaConstants.TAG_VERSIONS);
                if (versions != null && !versions.isEmpty()) {
                    for (final JsonNode versionsArray : versions) {
                        for (final JsonNode version : versionsArray) {
                            if (version.get(SedaConstants.TAG_PHYSICAL_ID) == null) {
                                final String objectId = version.get(SedaConstants.PREFIX_ID).asText();
                                checkMessageDigest(params, binaryObjects.get(objectId), version, itemStatus);
                            }
                        }
                    }
                }
            }

            if (oneOrMoreMessagesDigestUpdated) {
                handlerIO.transferInputStreamToWorkspace(
                    IngestWorkflowConstants.OBJECT_GROUP_FOLDER + "/" + params.getObjectName(),
                    new ByteArrayInputStream(JsonHandler.writeAsString(jsonOG).getBytes()));
            }

        } catch (ProcessingException |
            InvalidParseOperationException e) {
            LOGGER.error(e);
            itemStatus.increment(StatusCode.FATAL);
        }

        LOGGER.debug("CheckConformityActionHandler response: " + itemStatus.getGlobalStatus());
        return new ItemStatus(CALC_CHECK).setItemsStatus(CALC_CHECK, itemStatus);
    }

    private void checkMessageDigest(WorkerParameters params,
        DataObjectInfo binaryObject, JsonNode version, ItemStatus itemStatus)
        throws ProcessingException {
        String eventDetailData;

        Response response = null;

        try {
            final DigestType digestTypeInput = DigestType.fromValue((String) handlerIO.getInput(ALGO_RANK));
            response = handlerIO.getInputStreamNoCachedFromWorkspace(
                IngestWorkflowConstants.SEDA_FOLDER + "/" + binaryObject.getUri());
            InputStream inputStream = (InputStream) response.getEntity();
            final Digest vitamDigest = new Digest(digestTypeInput);
            Digest manifestDigest;
            boolean isVitamDigest = false;
            if (!binaryObject.getAlgo().equals(digestTypeInput)) {
                // Begin calculate digest by manifest alog
                manifestDigest = new Digest(binaryObject.getAlgo());
                inputStream = manifestDigest.getDigestInputStream(inputStream);
            } else {
                manifestDigest = vitamDigest;
                isVitamDigest = true;
            }
            // calculate digest by vitam alog
            vitamDigest.update(inputStream);

            final String manifestDigestString = manifestDigest.digestHex();
            final String vitamDigestString = vitamDigest.digestHex();
            LOGGER.debug(
                "DEBUG: \n\t" + binaryObject.getAlgo().getName() + " " + binaryObject.getMessageDigest() + "\n\t" +
                    manifestDigestString + "\n\t" + vitamDigestString);
            // define eventDetailData
            eventDetailData = "{\"MessageDigest\":\"" + binaryObject.getMessageDigest() +
                "\",\"Algorithm\": \"" + binaryObject.getAlgo() +
                "\", \"SystemMessageDigest\": \"" + vitamDigestString +
                "\", \"SystemAlgorithm\": \"" + (String) handlerIO.getInput(ALGO_RANK) + "\"} ";

            // check digest
            if (manifestDigestString.equals(binaryObject.getMessageDigest())) {
                itemStatus.increment(StatusCode.OK);
                if (!isVitamDigest) {
                    // update objectGroup json
                    ((ObjectNode) version).put(SedaConstants.TAG_DIGEST, vitamDigestString);
                    ((ObjectNode) version).put(SedaConstants.ALGORITHM, (String) handlerIO.getInput(ALGO_RANK));
                    oneOrMoreMessagesDigestUpdated = true;
                }

            } else {
                itemStatus.increment(StatusCode.KO);

                // Set eventDetailData in KO case
                eventDetailData = "{\"MessageDigest\":\"" + binaryObject.getMessageDigest() + "\",\"Algorithm\": \"" +
                    binaryObject.getAlgo() +
                    "\", \"ComputedMessageDigest\": \"" + manifestDigestString + "\"} ";
            }
            itemStatus.setEvDetailData(eventDetailData);
            itemStatus.setSubTaskStatus(binaryObject.getId(), itemStatus);
        } catch (ContentAddressableStorageNotFoundException | ContentAddressableStorageServerException |
            IOException e) {
            LOGGER.error(e);
            throw new ProcessingException(e.getMessage(), e);
        } finally {
            handlerIO.consumeAnyEntityAndClose(response);
        }

    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        handler.checkHandlerIO(1, Arrays.asList(new Class[] {String.class}));
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
                    binaryObjects.put(jsonBinaryObject.get(SedaConstants.PREFIX_ID).asText(),
                        new DataObjectInfo()
                            .setSize(jsonBinaryObject.get(SedaConstants.TAG_SIZE).asLong())
                            .setId(jsonBinaryObject.get(SedaConstants.PREFIX_ID).asText())
                            .setUri(jsonBinaryObject.get(SedaConstants.TAG_URI).asText())
                            .setMessageDigest(jsonBinaryObject.get(SedaConstants.TAG_DIGEST).asText())
                            .setAlgo(DigestType.fromValue(jsonBinaryObject.get(SedaConstants.ALGORITHM).asText())));
                }
            }
        }
        // OK
        return binaryObjects;
    }
}
