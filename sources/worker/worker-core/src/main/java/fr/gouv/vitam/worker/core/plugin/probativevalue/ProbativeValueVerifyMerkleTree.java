/**
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
 */
package fr.gouv.vitam.worker.core.plugin.probativevalue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.BaseXx;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.security.merkletree.MerkleTree;
import fr.gouv.vitam.common.security.merkletree.MerkleTreeAlgo;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.model.TraceabilityEvent;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbName;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookOperation;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import static fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbName.rightsStatementIdentifier;
import static fr.gouv.vitam.worker.core.handler.VerifyMerkleTreeActionHandler.computeMerkleTree;

/**
 * Using Merkle trees to detect inconsistencies in data
 */
public class ProbativeValueVerifyMerkleTree extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProbativeValueVerifyMerkleTree.class);


    private static final String HANDLER_ID = "PROBATIVE_VALUE_CHECK_MERKLE_TREE";
    public static final String EV_ID_APP_SESSION = "EvIdAppSession";
    public static final String EV_TYPE_PROC = "EvTypeProc";
    private LogbookOperationsClientFactory logbookOperationsClientFactory;

    private String merkleJsonRootHash = null;
    private String merkleDataRootHash = null;
    private String merkleLogbookRootHash = null;


    @VisibleForTesting
    public ProbativeValueVerifyMerkleTree(
        LogbookOperationsClientFactory logbookOperationsClientFactory) {
        this.logbookOperationsClientFactory = logbookOperationsClientFactory;
    }


    public ProbativeValueVerifyMerkleTree() {
        this(LogbookOperationsClientFactory.getInstance());
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handlerIO) {

        final ItemStatus itemStatus = new ItemStatus(HANDLER_ID);

        try {

            ObjectNode result = JsonHandler.createObjectNode();

            checkMerkleTree(param.getObjectName(),
                handlerIO.getFileFromWorkspace("dataDir" + "/" + param.getObjectName()),
                handlerIO.getFileFromWorkspace("merkleDir" + "/" + param.getObjectName()), result);


            boolean isMerkleOk =
                merkleJsonRootHash != null
                    && merkleJsonRootHash.equals(merkleDataRootHash)
                    && merkleDataRootHash.equals(merkleLogbookRootHash);

            result.put("Id", param.getObjectName());

            result.put("OperationCheckStatus", isMerkleOk ? "OK" : "KO");

            result.put("Details", String
                .format("merkleJsonRootHash is : '%s', merkleDataRootHash is :'%s', merkleLogbookRootHash is '%s' ",
                    merkleJsonRootHash, merkleDataRootHash, merkleLogbookRootHash));

            handlerIO.transferInputStreamToWorkspace("operationReport" + "/" + param.getObjectName(),
                new ByteArrayInputStream(result.toString().getBytes()), null, false);

            boolean shouldCheckOpi = Boolean.parseBoolean((String) handlerIO.getInput(0));

            if (shouldCheckOpi) {
                checkOpiInfo(param, handlerIO);
            }
        } catch (Exception e) {
            LOGGER.error(e);
            itemStatus.increment(StatusCode.FATAL);
        }
        itemStatus.increment(StatusCode.OK);
        return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
    }

    private void checkOpiInfo(WorkerParameters param,
        HandlerIO handlerIO)
        throws ContentAddressableStorageServerException, ContentAddressableStorageNotFoundException, IOException,
        LogbookClientException, InvalidParseOperationException, ProcessingException {
        File listOpiFile = handlerIO.getFileFromWorkspace("operationForOpi/" + param.getObjectName());

        Set<String> opiList = JsonHandler.getFromFileAsTypeRefence(listOpiFile, new TypeReference<HashSet<String>>() {
        });

        for (String id : opiList) {
            LogbookOperation logbookOperation = getLogbookOperation(id);
            ObjectNode node = JsonHandler.createObjectNode();

            node.put("id", id);
            String agIdApp = (String) logbookOperation.get("agIdApp");

            String evIdAppSession = (String) logbookOperation.get(EV_ID_APP_SESSION);

            JsonNode rightStatement =
                JsonHandler.getFromString((String) logbookOperation.get(rightsStatementIdentifier.getDbname()));


            String evTypeProc = (String) logbookOperation.get("evTypeProc");
            node.put(EV_TYPE_PROC, evTypeProc);
            node.put(EV_ID_APP_SESSION, evIdAppSession);
            node.put("agIdApp", agIdApp);
            node.put(EV_ID_APP_SESSION, rightStatement.get("ArchivalAgreement").textValue());
            node.put("OperationCheckStatus", "OK");

            handlerIO.transferInputStreamToWorkspace("operationReport" + "/" + id,
                new ByteArrayInputStream(node.toString().getBytes()), null, false);
        }
    }

    void checkMerkleTree(String operationId, File dataFile, File merkleFile, ObjectNode report) {


        try (InputStream dataStream = new FileInputStream(dataFile);
        ) {

            JsonNode merkleFileJson = JsonHandler.getFromFile(merkleFile);

            merkleJsonRootHash = merkleFileJson.get("Root").asText();

            LogbookOperation logbookOperation = getLogbookOperation(operationId);
            String evTypeProc = (String) logbookOperation.get("evTypeProc");
            report.put(EV_TYPE_PROC, evTypeProc);

            String evDetData = logbookOperation.getString(LogbookMongoDbName.eventDetailData.getDbname());
            JsonNode eventDetail = JsonHandler.getFromString(evDetData);

            TraceabilityEvent traceabilityEvent =
                JsonHandler.getFromJsonNode(eventDetail, TraceabilityEvent.class);

            merkleLogbookRootHash = traceabilityEvent.getHash();
            MerkleTreeAlgo merkleTreeAlgo = computeMerkleTree(dataStream);
            MerkleTree merkleTree = merkleTreeAlgo.generateMerkle();
            merkleDataRootHash = BaseXx.getBase64(merkleTree.getRoot());

        } catch (InvalidParseOperationException | LogbookClientException | IOException | ProcessingException e) {
            LOGGER.error(e);

        }
    }

    private LogbookOperation getLogbookOperation(String operationId)
        throws LogbookClientException, InvalidParseOperationException {

        try (LogbookOperationsClient client = logbookOperationsClientFactory.getClient()) {
            JsonNode node = client.selectOperationById(operationId);
            return new LogbookOperation(node.get("$results").get(0));
        }
    }

    String getMerkleJsonRootHash() {
        return merkleJsonRootHash;
    }

    String getMerkleDataRootHash() {
        return merkleDataRootHash;
    }

    public String getMerkleLogbookRootHash() {
        return merkleLogbookRootHash;
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
    }

}
