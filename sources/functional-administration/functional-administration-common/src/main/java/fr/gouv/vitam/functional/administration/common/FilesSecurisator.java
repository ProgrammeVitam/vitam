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
package fr.gouv.vitam.functional.administration.common;


import java.io.InputStream;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.databind.node.ObjectNode;

import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.stream.StreamUtils;

import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
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

import static fr.gouv.vitam.common.i18n.VitamLogbookMessages.getCodeOp;
import static fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory.*;

public class FilesSecurisator {

    public FilesSecurisator() {

    }

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(FilesSecurisator.class);

    private final String FLE_NAME = "FileName";
    private final String DIGEST = "Digest";


    private static final String STRATEGY_ID = "default";
    private LogbookOperationsClient client = LogbookOperationsClientFactory.getInstance().getClient();
    private final DateTimeFormatter formater = DateTimeFormatter.ofPattern("yyyyMMddhhmmss");
    private WorkspaceClientFactory workspaceClientFactory;
    private StorageClientFactory storageClientFactory;
    private ObjectNode evDetData = JsonHandler.createObjectNode();

    @VisibleForTesting
    public FilesSecurisator(String storage_name, String logBook_securisation,
        WorkspaceClientFactory workspaceClientFactory, StorageClientFactory storageClientFactory) {
        this.storageClientFactory = storageClientFactory;
        this.workspaceClientFactory = workspaceClientFactory;
    }



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


    public void secureFiles(int version, InputStream stream, String extension, GUID eipMaster,
        String digest, LogbookTypeProcess process, StorageCollectionType storageCollectionType, String logbook_event,
        String prefix_name)
        throws LogbookClientServerException, StorageException, LogbookClientBadRequestException,
        LogbookClientAlreadyExistsException {


        evDetData.put(DIGEST, digest);
        String name = getName(version, extension, prefix_name, ParameterHelper.getTenantParameter());

        secureFiles(stream, extension, eipMaster, process, storageCollectionType, logbook_event, prefix_name, name);
    }

    /**
     * secure File
     */
    public void secureFiles(InputStream stream, String extension, GUID eipMaster,
        LogbookTypeProcess process, StorageCollectionType storageCollectionType,
        String logbook_event, String prefix_name, String fileName)

        throws StorageException, LogbookClientServerException, LogbookClientBadRequestException,
        LogbookClientAlreadyExistsException {

        Integer tenantId = ParameterHelper.getTenantParameter();

        try (
            WorkspaceClient workspaceClient = workspaceClientFactory.getInstance().getClient();
            StorageClient storageClient = storageClientFactory.getInstance().getClient()
        ) {

            final GUID eip = GUIDFactory.newOperationLogbookGUID(tenantId);
            final String eventCode = logbook_event + "_" + extension.toUpperCase();
            final String uri = String.format("%s/%s", prefix_name, fileName);
            final ObjectDescription description = new ObjectDescription();

            final LogbookOperationParameters logbookParametersStart =
                newLogbookOperationParameters(eip, eventCode, eipMaster, process,
                    StatusCode.STARTED, getCodeOp(eventCode, StatusCode.STARTED), eip);

            updateLogBookEntry(logbookParametersStart);

            try {

                storeInWorkSpace(stream, eipMaster, process, storageCollectionType, workspaceClient, storageClient, eip,
                    fileName, eventCode, uri, description);

                final LogbookOperationParameters logbookEnd = newLogbookOperationParameters(eip,
                    eventCode, eipMaster, process, StatusCode.OK, getCodeOp(eventCode, StatusCode.OK), eip);

                evDetData.put(FLE_NAME, fileName);
                logbookEnd
                    .putParameterValue(LogbookParameterName.eventDetailData, JsonHandler.unprettyPrint(evDetData));

                updateLogBookEntry(logbookEnd);

            } catch (ContentAddressableStorageAlreadyExistException | ContentAddressableStorageServerException e) {

                LOGGER.error("unable to create container or store file in workspace", e);
                final LogbookOperationParameters logbookParametersEnd =
                    newLogbookOperationParameters(eip, eventCode, eipMaster, process,
                        StatusCode.KO, getCodeOp(eventCode, StatusCode.KO), eip);

                updateLogBookEntry(logbookParametersEnd);

                throw new StorageException(e);

            } finally {

                StreamUtils.closeSilently(stream);
            }
        }
    }

    private void storeInWorkSpace(InputStream stream, GUID eipMaster, LogbookTypeProcess process,
        StorageCollectionType storageCollectionType, WorkspaceClient workspaceClient, StorageClient storageClient,
        GUID eip, String fileName, String eventCode, String uri, ObjectDescription description)
        throws ContentAddressableStorageAlreadyExistException, ContentAddressableStorageServerException,
        StorageException {
        workspaceClient.createContainer(fileName);
        workspaceClient.putObject(fileName, uri, stream);
        description.setWorkspaceContainerGUID(fileName);
        description.setWorkspaceObjectURI(uri);

        try {

            storageClient.storeFileFromWorkspace(STRATEGY_ID, storageCollectionType, fileName, description);

            workspaceClient.deleteContainer(fileName, true);

        } catch (StorageAlreadyExistsClientException | StorageNotFoundClientException |
            StorageServerClientException | ContentAddressableStorageNotFoundException e) {

            final LogbookOperationParameters logbookParametersEnd =
                newLogbookOperationParameters(eip, eventCode,
                    eipMaster, process, StatusCode.KO, getCodeOp(eventCode, StatusCode.KO), eip);

            updateLogBookEntry(logbookParametersEnd);

            LOGGER.error("unable to store file", e);
            throw new StorageException(e);
        }
    }

    private String getName(int version, String extension, String prefix_name, Integer tenantId) {
        return String.format("%d_" + prefix_name + "-%s_%s." + extension,
            tenantId, version, LocalDateTime.now().format(formater)
        );
    }


}
