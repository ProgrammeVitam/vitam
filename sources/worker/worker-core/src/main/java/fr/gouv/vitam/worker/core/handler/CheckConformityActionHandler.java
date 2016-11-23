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

import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleObjectGroupParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.common.utils.BinaryObjectInfo;
import fr.gouv.vitam.worker.common.utils.IngestWorkflowConstants;
import fr.gouv.vitam.worker.common.utils.LogbookLifecycleWorkerHelper;
import fr.gouv.vitam.worker.common.utils.SedaConstants;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

/**
 * CheckConformityAction Handler.<br>
 *
 */
public class CheckConformityActionHandler extends ActionHandler {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CheckConformityActionHandler.class);

    private static final String HANDLER_ID = "CHECK_DIGEST";
    private static final String CALC_CHECK = "CALC_CHECK";
    private static final String SUBTASKID = HANDLER_ID + "." + CALC_CHECK;
    private HandlerIO handlerIO;
    private String objectID;
    private int nbOK;
    private int nbKO;
    private LogbookLifeCycleObjectGroupParameters logbookLifecycleObjectGroupParameters;
    private boolean oneOrMoreMessagesDigestUpdated = false;
    private static final int ALGO_RANK = 0;
    private static final String INCOME = "MessageIdentifier du manifest";

    /**
     * Constructor
     */
    public CheckConformityActionHandler() {
        // Nothing
    }

    /**
     * @return HANDLER_ID
     */
    public static final String getId() {
        return HANDLER_ID;
    }


    @Override
    public ItemStatus execute(WorkerParameters params, HandlerIO handler) throws ProcessingException {
        checkMandatoryParameters(params);
        handlerIO = handler;
        logbookLifecycleObjectGroupParameters = LogbookParametersFactory.newLogbookLifeCycleObjectGroupParameters();
        nbOK = 0;
        nbKO = 0;
        LOGGER.debug("CheckConformityActionHandler running ...");

        final ItemStatus itemStatus = new ItemStatus(HANDLER_ID);
        try (LogbookLifeCyclesClient logbookClient = LogbookLifeCyclesClientFactory.getInstance().getClient()) {
            try {
                // Get objectGroup
                final JsonNode jsonOG = handlerIO.getJsonFromWorkspace(
                    IngestWorkflowConstants.OBJECT_GROUP_FOLDER + "/" + params.getObjectName());

                Map<String, BinaryObjectInfo> binaryObjects = getBinaryObjects(jsonOG);

                objectID = jsonOG.findValue(SedaConstants.PREFIX_ID).asText();

                // Add Start CHECK_DIGEST TASK in ObjectGroup LifeCycle
                logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.eventIdentifier, objectID);
                logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.objectIdentifierIncome,
                    INCOME);
                logbookLifecycleObjectGroupParameters.setBeginningLog(HANDLER_ID, null, null);
                LogbookLifecycleWorkerHelper.updateLifeCycleForBegining(logbookClient,
                    logbookLifecycleObjectGroupParameters, params);

                // checkMessageDigest
                JsonNode qualifiers = jsonOG.get(SedaConstants.PREFIX_QUALIFIERS);
                if (qualifiers != null) {
                    List<JsonNode> versions = qualifiers.findValues(SedaConstants.TAG_VERSIONS);
                    if (versions != null && !versions.isEmpty()) {
                        for (JsonNode versionsArray : versions) {
                            for (JsonNode version : versionsArray) {
                                String objectId = version.get(SedaConstants.PREFIX_ID).asText();
                                checkMessageDigest(logbookClient, params, binaryObjects.get(objectId),
                                    version,
                                    itemStatus);
                            }
                        }
                    }
                }

                if (oneOrMoreMessagesDigestUpdated) {
                    handlerIO.transferInputStreamToWorkspace(
                        IngestWorkflowConstants.OBJECT_GROUP_FOLDER + "/" + params.getObjectName(),
                        new ByteArrayInputStream(JsonHandler.writeAsString(jsonOG).getBytes()));
                }

                if (nbKO > 0) {
                    logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                        VitamLogbookMessages.getCodeLfc(itemStatus.getItemId(), StatusCode.KO));
                    // TODO P0 WORKFLOW
                } else {
                    logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                        VitamLogbookMessages.getCodeLfc(itemStatus.getItemId(), StatusCode.OK));
                }

            } catch (ProcessingException |
                InvalidParseOperationException | LogbookClientNotFoundException | LogbookClientServerException |
                LogbookClientBadRequestException e) {
                LOGGER.error(e);
                itemStatus.increment(StatusCode.FATAL);
                logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                    VitamLogbookMessages.getCodeLfc(itemStatus.getItemId(), StatusCode.FATAL));
            }

            try {

                // Set Final Status for CHECK_DIGEST TASK
                logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.eventIdentifier, objectID);
                logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.eventDetailData, null);
                LogbookLifecycleWorkerHelper.setLifeCycleFinalEventStatusByStep(logbookClient,
                    logbookLifecycleObjectGroupParameters,
                    itemStatus);
            } catch (ProcessingException e) {
                LOGGER.error(e);
                itemStatus.increment(StatusCode.FATAL);
            }
        }

        LOGGER.debug("CheckConformityActionHandler response: " + itemStatus.getGlobalStatus());
        return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
    }

    private void checkMessageDigest(LogbookLifeCyclesClient logbookClient, WorkerParameters params,
        BinaryObjectInfo binaryObject, JsonNode version, ItemStatus itemStatus)
        throws ProcessingException, LogbookClientBadRequestException, LogbookClientNotFoundException,
        LogbookClientServerException {
        String containerId = params.getContainerName();

        String eventDetailData;
        StatusCode statusCode = StatusCode.OK;

        // started for binary Object
        logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.eventIdentifier,
            binaryObject.getId());
        logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.objectIdentifierIncome,
            INCOME);
        Response response = null;

        try {
            DigestType digestTypeInput = DigestType.fromValue((String) handlerIO.getInput(ALGO_RANK));
            response = handlerIO.getInputStreamNoCachedFromWorkspace(
                IngestWorkflowConstants.SEDA_FOLDER + "/" + binaryObject.getUri());
            InputStream inputStream = (InputStream) response.getEntity();
            Digest vitamDigest = new Digest(digestTypeInput);
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
            LOGGER.debug("DEBUG: \n\t" + binaryObject.getAlgo().getName()+" " +binaryObject.getMessageDigest()+"\n\t"+
               manifestDigestString+"\n\t"+vitamDigestString);
            // define eventDetailData
            eventDetailData = "{\"MessageDigest\":\"" + binaryObject.getMessageDigest() +
                "\",\"Algorithm\": \"" + binaryObject.getAlgo() +
                "\", \"SystemMessageDigest\": \"" + (String) handlerIO.getInput(ALGO_RANK) +
                "\", \"SystemAlgorithm\": \"" + vitamDigestString + "\"} ";

            // check digest
            if (manifestDigestString.equals(binaryObject.getMessageDigest())) {
                itemStatus.increment(StatusCode.OK);
                nbOK++;

                if (!isVitamDigest) {
                    // update objectGroup json
                    ((ObjectNode) version).put(SedaConstants.TAG_DIGEST, vitamDigestString);
                    ((ObjectNode) version).put(SedaConstants.ALGORITHM, (String) handlerIO.getInput(ALGO_RANK));
                    oneOrMoreMessagesDigestUpdated = true;
                }

            } else {
                nbKO++;
                itemStatus.increment(StatusCode.KO);
                statusCode = StatusCode.KO;

                // Set eventDetailData in KO case
                eventDetailData = "{\"MessageDigest\":\"" + binaryObject.getMessageDigest() + "\",\"Algorithm\": \"" +
                    binaryObject.getAlgo() +
                    "\", \"ComputedMessageDigest\": \"" + manifestDigestString + "\"} ";
            }

            // Add CHECK_DIGEST.CALC_DIGEST SubTask Result to ObjectGroup lifeCycle
            logbookLifecycleObjectGroupParameters.setFinalStatus(SUBTASKID, null, statusCode,
                null);
            logbookLifecycleObjectGroupParameters.putParameterValue(LogbookParameterName.eventDetailData,
                eventDetailData);
            logbookClient.update(logbookLifecycleObjectGroupParameters);

        } catch (ContentAddressableStorageNotFoundException | ContentAddressableStorageServerException |
            IOException | LogbookClientBadRequestException | LogbookClientNotFoundException |
            LogbookClientServerException e) {
            LOGGER.error(e);

            logbookLifecycleObjectGroupParameters.setFinalStatus(SUBTASKID, null, StatusCode.FATAL,
                null);
            logbookClient.update(logbookLifecycleObjectGroupParameters);
            throw new ProcessingException(e.getMessage(), e);
        } finally {
            handlerIO.consumeAnyEntityAndClose(response);
        }

    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        handler.checkHandlerIO(1, Arrays.asList(new Class[] {String.class}));
    }

    private Map<String, BinaryObjectInfo> getBinaryObjects(JsonNode jsonOG) throws ProcessingException {
        Map<String, BinaryObjectInfo> binaryObjects = new HashMap<>();

        JsonNode work = jsonOG.get(SedaConstants.PREFIX_WORK);
        JsonNode qualifiers = work.get(SedaConstants.PREFIX_QUALIFIERS);
        if (qualifiers == null) {
            // KO
            return binaryObjects;
        }

        List<JsonNode> versions = qualifiers.findValues(SedaConstants.TAG_VERSIONS);
        if (versions == null || versions.isEmpty()) {
            // KO
            return binaryObjects;
        }
        for (JsonNode version : versions) {
            LOGGER.debug(version.toString());
            for (JsonNode jsonBinaryObject : version) {
                binaryObjects.put(jsonBinaryObject.get(SedaConstants.PREFIX_ID).asText(),
                    new BinaryObjectInfo()
                        .setSize(jsonBinaryObject.get(SedaConstants.TAG_SIZE).asLong())
                        .setId(jsonBinaryObject.get(SedaConstants.PREFIX_ID).asText())
                        .setUri(jsonBinaryObject.get(SedaConstants.TAG_URI).asText())
                        .setMessageDigest(jsonBinaryObject.get(SedaConstants.TAG_DIGEST).asText())
                        .setAlgo(DigestType.fromValue(jsonBinaryObject.get(SedaConstants.ALGORITHM).asText())));
            }
        }
        // OK
        return binaryObjects;
    }
}
