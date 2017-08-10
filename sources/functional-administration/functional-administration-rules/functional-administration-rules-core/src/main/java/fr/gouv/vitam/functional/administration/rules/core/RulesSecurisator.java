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
package fr.gouv.vitam.functional.administration.rules.core;


import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.UpdateWorkflowConstants;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationsClientHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageAlreadyExistsClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.exception.StorageException;
import fr.gouv.vitam.storage.engine.common.model.StorageCollectionType;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

public class RulesSecurisator {


    private static final String STORAGE_RULE_NAME = "RULES-";
    private static final String STORAGE_RULE_WORKSPACE = "RULES";

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(RulesSecurisator.class);

    private static final String RULE_SECURISATION = "RULES_SECURISATION";
    private static final String FLE_NAME = "FileName";
    private static final String DIGEST = "Digest";


    private static final String STRATEGY_ID = "default";
    public static final String STORAGE_LOGBOOK_RULE = "StorageRule";
    private static String STP_IMPORT_RULES = "STP_IMPORT_RULES";
    private LogbookOperationsClient client = LogbookOperationsClientFactory.getInstance().getClient();

    /**
     * Create a LogBook Entry related to object's update
     *
     * @param logbookParametersEnd
     */
    private void updateLogBookEntry(LogbookOperationParameters logbookParametersEnd) {
        try {
            client.update(logbookParametersEnd);
        } catch (LogbookClientBadRequestException | LogbookClientNotFoundException | LogbookClientServerException e) {
            LOGGER.error(e.getMessage());
        }
    }



    /**
     * secure File rules
     */
    public void secureFileRules(int version, InputStream stream, String extension, GUID eipMaster, String digest)

        throws StorageException, LogbookClientServerException, LogbookClientBadRequestException,
        LogbookClientAlreadyExistsException {
        final LogbookOperationsClientHelper helper = new LogbookOperationsClientHelper();
        Integer tenantId = ParameterHelper.getTenantParameter();
        try (
            WorkspaceClient workspaceClient = WorkspaceClientFactory.getInstance().getClient();
            StorageClient storageClient = StorageClientFactory.getInstance().getClient();) {

            final GUID eip1 = GUIDFactory.newOperationLogbookGUID(tenantId);
            final String fileName =
                String.format("%d_" + STORAGE_RULE_NAME + "%s_%s." + extension, tenantId, version,
                    LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern("yyyyMMddhhmmss")));
            final LogbookOperationParameters logbookParametersStart = LogbookParametersFactory
                .newLogbookOperationParameters(eip1, RULE_SECURISATION + "_" + extension.toUpperCase(), eipMaster,
                    LogbookTypeProcess.STORAGE_RULE,
                    StatusCode.STARTED, VitamLogbookMessages
                        .getCodeOp(RULE_SECURISATION + "_" + extension.toUpperCase(), StatusCode.STARTED),
                    eip1);
            updateLogBookEntry(logbookParametersStart);


            final String uri = String.format("%s/%s", STORAGE_RULE_WORKSPACE, fileName);

            try {
                workspaceClient.createContainer(fileName);
                workspaceClient.putObject(fileName, uri, stream);
                final ObjectDescription description = new ObjectDescription();
                description.setWorkspaceContainerGUID(fileName);
                description.setWorkspaceObjectURI(uri);

                try {
                    storageClient.storeFileFromWorkspace(
                        STRATEGY_ID, StorageCollectionType.RULES, fileName, description);
                    workspaceClient.deleteContainer(fileName, true);
                } catch (StorageAlreadyExistsClientException | StorageNotFoundClientException |
                    StorageServerClientException | ContentAddressableStorageNotFoundException e) {

                    final LogbookOperationParameters logbookParametersEnd = LogbookParametersFactory
                        .newLogbookOperationParameters(eip1, RULE_SECURISATION + "_" + extension.toUpperCase(),
                            eipMaster,
                            LogbookTypeProcess.STORAGE_RULE,
                            StatusCode.KO, VitamLogbookMessages
                                .getCodeOp(RULE_SECURISATION + "_" + extension.toUpperCase(), StatusCode.KO),
                            eip1);
                    updateLogBookEntry(logbookParametersEnd);

                    LOGGER.error("unable to store file", e);
                    throw new StorageException(e);
                }


                final LogbookOperationParameters logbookParametersEnd = LogbookParametersFactory
                    .newLogbookOperationParameters(eip1, RULE_SECURISATION + "_" + extension.toUpperCase(), eipMaster,
                        LogbookTypeProcess.STORAGE_RULE,
                        StatusCode.OK, VitamLogbookMessages.getCodeOp(RULE_SECURISATION + "_" + extension.toUpperCase(),
                            StatusCode.OK),
                        eip1);
                final ObjectNode evDetData = JsonHandler.createObjectNode();
                evDetData.put(FLE_NAME, fileName);
                evDetData.put(DIGEST, digest);
                logbookParametersEnd.putParameterValue(LogbookParameterName.eventDetailData,
                    JsonHandler.unprettyPrint(evDetData));
                updateLogBookEntry(logbookParametersEnd);
            } catch (ContentAddressableStorageAlreadyExistException | ContentAddressableStorageServerException e) {
                LOGGER.error("unable to create container or store file in workspace", e);
                final LogbookOperationParameters logbookParametersEnd = LogbookParametersFactory
                    .newLogbookOperationParameters(eip1, RULE_SECURISATION + "_" + extension.toUpperCase(), eipMaster,
                        LogbookTypeProcess.STORAGE_RULE,
                        StatusCode.KO, VitamLogbookMessages.getCodeOp(RULE_SECURISATION + "_" + extension.toUpperCase(),
                            StatusCode.KO),
                        eip1);
                updateLogBookEntry(logbookParametersEnd);
                throw new StorageException(e);
            } finally {
                StreamUtils.closeSilently(stream);
            }

        }
    }


    public void copyFilesOnWorkspaceUpdateWorkflow(InputStream stream, String containerName)
        throws ContentAddressableStorageAlreadyExistException, ContentAddressableStorageServerException {
        try (
            WorkspaceClient workspaceClient = WorkspaceClientFactory.getInstance().getClient();) {
            workspaceClient.createContainer(containerName);
            workspaceClient.putObject(containerName,
                UpdateWorkflowConstants.PROCESSING_FOLDER + "/" + UpdateWorkflowConstants.UPDATED_RULES_JSON,
                stream);
        }

    }

}
