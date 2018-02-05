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

package fr.gouv.vitam.storage.engine.server.storagelog;

import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationsClientHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageAlreadyExistsClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Business class for Storage Log Administration (backup)
 */
public class StorageLogAdministration {

    //TODO : could be useful to create a Junit for this

    private static final String STORAGE_LOGBOOK = "storage_logbook";

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(StorageLogAdministration.class);

    private static final String STP_OP_SECURISATION = "STP_STORAGE_SECURISATION";

    private static final String STRATEGY_ID = "default";
    final StorageLogService storageLogService;
    private final File tmpFolder;

    public StorageLogAdministration(StorageLogService storageLogService,
        String tmpFolder) {
        this.storageLogService = storageLogService;
        this.tmpFolder = new File(tmpFolder);
        this.tmpFolder.mkdir();
    }

    /**
     * secure the logbook operation since last securisation.
     *
     * @return the GUID of the operation
     * @throws IOException                         if an IOException is thrown while generating the secure storage
     * @throws StorageLogException                 if a LogZipFile cannot be generated
     * @throws LogbookClientBadRequestException    if a bad request is encountered
     * @throws LogbookClientAlreadyExistsException if the logbook already exists
     * @throws LogbookClientServerException        if there's a problem connecting to the logbook functionnality
     */
    public synchronized GUID backupStorageLog()
        throws IOException, StorageLogException,
        LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException {
        // TODO: use a distributed lock to launch this function only on one server (cf consul)
        final LogbookOperationsClientHelper helper = new LogbookOperationsClientHelper();
        Integer tenantId = ParameterHelper.getTenantParameter();
        final GUID eip = GUIDFactory.newOperationLogbookGUID(tenantId);
        try {
            createLogbookOperationStarted(helper, eip);

            List<LogInformation> info = storageLogService.rotateLogFile(tenantId);

            for (LogInformation logInformation : info) {
                storeLogFile(helper, tenantId, eip, logInformation);
            }

            createLogbookOperationEvent(helper, eip, STP_OP_SECURISATION, StatusCode.OK);

        } catch (LogbookClientNotFoundException | LogbookClientAlreadyExistsException e) {
            throw new StorageLogException(e);
        } finally {
            LogbookOperationsClientFactory.getInstance().getClient()
                .bulkCreate(eip.getId(), helper.removeCreateDelegate(eip.getId()));
        }
        return eip;
    }

    private void storeLogFile(LogbookOperationsClientHelper helper, Integer tenantId, GUID eip,
        LogInformation logInformation) throws LogbookClientNotFoundException, StorageLogException {
        LOGGER.info("Storing log file " + logInformation.getPath());

        String fileName = tenantId + "_" + STORAGE_LOGBOOK + "_"
            + logInformation.getBeginTime().format(getDateTimeFormatter()) + "_"
            + logInformation.getEndTime().format(getDateTimeFormatter()) + "_"
            + eip.toString() + ".log";

        try (InputStream inputStream =
            new BufferedInputStream(new FileInputStream(logInformation.getPath().toFile()));
            WorkspaceClient workspaceClient = WorkspaceClientFactory.getInstance().getClient()) {

            String containerName = GUIDFactory.newGUID().toString();

            workspaceClient.createContainer(containerName);

            try {

                workspaceClient.putObject(containerName, fileName, inputStream);

                try (final StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {

                    final ObjectDescription description = new ObjectDescription();
                    description.setWorkspaceContainerGUID(containerName);
                    description.setWorkspaceObjectURI(fileName);

                    storageClient.storeFileFromWorkspace(
                        STRATEGY_ID, DataCategory.STORAGELOG, fileName, description);

                } catch (StorageAlreadyExistsClientException | StorageNotFoundClientException |
                    StorageServerClientException e) {
                    LOGGER.error("unable to store log file", e);
                    createLogbookOperationEvent(helper, eip, STP_OP_SECURISATION,
                        StatusCode.FATAL);
                    throw new StorageLogException(e);
                }

                if (!Files.deleteIfExists(logInformation.getPath())) {
                    LOGGER.warn("Could not delete local storage file " + logInformation.getPath().toAbsolutePath());
                }

            } finally {
                try {
                    workspaceClient.deleteContainer(containerName, true);
                } catch (ContentAddressableStorageNotFoundException | ContentAddressableStorageServerException e) {
                    LOGGER.error(
                        String.format("Unable to cleanup file from workspace %s/%s", containerName, fileName), e);
                }
            }


        } catch (ContentAddressableStorageAlreadyExistException | ContentAddressableStorageServerException |
            IOException e) {
            LOGGER.error("Unable to create container", e);
            createLogbookOperationEvent(helper, eip, STP_OP_SECURISATION, StatusCode.FATAL);
            throw new StorageLogException(e);
        }
    }

    private void createLogbookOperationStarted(LogbookOperationsClientHelper helper, GUID eip)
        throws LogbookClientAlreadyExistsException {
        final LogbookOperationParameters logbookOperationParameters = LogbookParametersFactory
            .newLogbookOperationParameters(eip, STP_OP_SECURISATION, eip, LogbookTypeProcess.TRACEABILITY,
                StatusCode.STARTED,
                VitamLogbookMessages.getCodeOp(STP_OP_SECURISATION, StatusCode.STARTED), eip);
        logbookOperationParameters.putParameterValue(LogbookParameterName.outcomeDetail, STP_OP_SECURISATION +
            "." + StatusCode.STARTED);

        LogbookOperationsClientHelper.checkLogbookParameters(logbookOperationParameters);
        helper.createDelegate(logbookOperationParameters);
    }

    private void createLogbookOperationEvent(LogbookOperationsClientHelper helper, GUID parentEventId, String eventType,
        StatusCode statusCode) throws LogbookClientNotFoundException {

        final LogbookOperationParameters logbookOperationParameters = LogbookParametersFactory
            .newLogbookOperationParameters(parentEventId, eventType, parentEventId, LogbookTypeProcess.TRACEABILITY,
                statusCode,
                VitamLogbookMessages.getCodeOp(eventType, statusCode), parentEventId);
        logbookOperationParameters.putParameterValue(LogbookParameterName.outcomeDetail, eventType +
            "." + statusCode);

        LogbookOperationsClientHelper.checkLogbookParameters(logbookOperationParameters);
        helper.updateDelegate(logbookOperationParameters);
    }

    private DateTimeFormatter getDateTimeFormatter() {
        return DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS").withZone(ZoneOffset.UTC);
    }
}
