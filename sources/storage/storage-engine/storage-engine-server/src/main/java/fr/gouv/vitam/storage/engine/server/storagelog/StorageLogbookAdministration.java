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

import static fr.gouv.vitam.common.LocalDateUtil.getString;
import static fr.gouv.vitam.common.LocalDateUtil.now;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.commons.compress.archivers.ArchiveException;

import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
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
import fr.gouv.vitam.logbook.common.exception.TraceabilityException;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationsClientHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.exception.LogbookDatabaseException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookNotFoundException;
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

/**
 * Business class for Logbook Administration (traceability)
 */
public class StorageLogbookAdministration {

    //TODO : could be usefull to create a Junit for this

    private static final String STORAGE_LOGBOOK = "storage_logbook";

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(StorageLogbookAdministration.class);

    private static final String STP_OP_SECURISATION = "STP_STORAGE_SECURISATION";


    private static final String STRATEGY_ID = "default";
    public static final String STORAGE_LOGBOOK_OPERATION_ZIP = "StorageLogbookOperation";
    final StorageLogbookService storageLogbookService;
    private final File tmpFolder;
    private final static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HH-mm-ss");



    public StorageLogbookAdministration(StorageLogbookService storageLogbookService,
        String tmpFolder) {
        this.storageLogbookService = storageLogbookService;
        this.tmpFolder = new File(tmpFolder);
        this.tmpFolder.mkdir();

    }



    /**
     * secure the logbook operation since last securisation.
     *
     * @return the GUID of the operation
     * @throws TraceabilityException               if error on generating secure logbook
     * @throws IOException                         if an IOException is thrown while generating the secure storage
     * @throws StorageLogException                 if a LogZipFile cannot be generated
     * @throws LogbookClientBadRequestException    if a bad request is encountered
     * @throws LogbookClientAlreadyExistsException if the logbook already exists
     * @throws LogbookClientServerException        if there's a problem connecting to the logbook functionnality
     * @throws LogbookNotFoundException            if not found on selecting logbook operation
     * @throws InvalidParseOperationException      if json data is not well-formed
     * @throws LogbookDatabaseException            if error on query logbook collection
     * @throws InvalidCreateOperationException     if error on creating query
     */
    public synchronized GUID generateSecureStorageLogbook()
        throws TraceabilityException, IOException, StorageLogException,
        LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException {
        // TODO: use a distributed lock to launch this function only on one server (cf consul)
        final LogbookOperationsClientHelper helper = new LogbookOperationsClientHelper();
        Integer tenantId = ParameterHelper.getTenantParameter();
        final GUID eip = GUIDFactory.newOperationLogbookGUID(tenantId);
        try {
            LocalDateTime time = now();
            final String fileName =
                String
                    .format("%d_" + STORAGE_LOGBOOK_OPERATION_ZIP + "_%s_%s.zip", tenantId, time.format(formatter),
                        eip.toString());
            createLogbookOperationStarted(helper, eip);

            final File zipFile = new File(tmpFolder, fileName);
            final String uri = String.format("%s/%s", STORAGE_LOGBOOK, fileName);
            LogInformation info = storageLogbookService.generateSecureStorage(tenantId);
            try (LogZipFile logZipFile = new LogZipFile(zipFile)) {
                logZipFile.initStoreLog();
                final DigestType digestType = VitamConfiguration.getDefaultTimestampDigestType();
                final Digest digest = new Digest(digestType);
                FileInputStream stream = new FileInputStream(info.getPath().toFile());
                logZipFile.storeLogFile(digest.getDigestInputStream(stream));
                logZipFile.storeAdditionalInformation(getString(info.getBeginTime()), getString(info.getEndTime()),
                    digest.toString(),
                    getString(time), tenantId);
                logZipFile.close();
                try {
                    info.getPath().toFile().delete();
                } catch (Exception e) {
                    LOGGER.error("unable to delete log file ", e);
                }
            } catch (IOException |
                ArchiveException e) {
                createLogbookOperationEvent(helper, eip, STP_OP_SECURISATION, StatusCode.FATAL);
                zipFile.delete();
                throw new StorageLogException(e);
            }

            try (InputStream inputStream = new BufferedInputStream(new FileInputStream(zipFile));

                WorkspaceClient workspaceClient = WorkspaceClientFactory.getInstance().getClient()) {



                workspaceClient.createContainer(fileName);
                workspaceClient.putObject(fileName, uri, inputStream);

                final StorageClientFactory storageClientFactory = StorageClientFactory.getInstance();

                final ObjectDescription description = new ObjectDescription();
                description.setWorkspaceContainerGUID(fileName);
                description.setWorkspaceObjectURI(uri);

                try (final StorageClient storageClient = storageClientFactory.getClient()) {
                    storageClient.storeFileFromWorkspace(
                        STRATEGY_ID, DataCategory.STORAGELOG, fileName, description);
                    workspaceClient.deleteContainer(fileName, true);
                } catch (StorageAlreadyExistsClientException | StorageNotFoundClientException |
                    StorageServerClientException | ContentAddressableStorageNotFoundException e) {
                    createLogbookOperationEvent(helper, eip, STP_OP_SECURISATION,
                        StatusCode.FATAL);
                    LOGGER.error("unable to store zip file", e);
                    throw new StorageLogException(e);
                }
            } catch (ContentAddressableStorageAlreadyExistException | ContentAddressableStorageServerException |
                IOException e) {
                LOGGER.error("unable to create container", e);
                createLogbookOperationEvent(helper, eip, STP_OP_SECURISATION, StatusCode.FATAL);
                throw new StorageLogException(e);



            } finally {
                zipFile.delete();
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

    private void createLogbookOperationStarted(LogbookOperationsClientHelper helper, GUID eip)
        throws LogbookClientNotFoundException, LogbookClientAlreadyExistsException {
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

}
