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
package fr.gouv.vitam.storage.engine.server.storagetraceability;

import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.CanonicalJsonFormatter;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.security.merkletree.MerkleTreeAlgo;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.exception.TraceabilityException;
import fr.gouv.vitam.logbook.common.model.TraceabilityEvent;
import fr.gouv.vitam.logbook.common.model.TraceabilityFile;
import fr.gouv.vitam.logbook.common.model.TraceabilityIterator;
import fr.gouv.vitam.logbook.common.model.TraceabilityStatistics;
import fr.gouv.vitam.logbook.common.model.TraceabilityType;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationsClientHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.traceability.LogbookTraceabilityHelper;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageAlreadyExistsClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.exception.StorageException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.common.compress.VitamArchiveStreamFactory;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.io.IOUtils;

import javax.ws.rs.core.Response;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;

import static fr.gouv.vitam.common.json.JsonHandler.unprettyPrint;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.common.model.StatusCode.STARTED;
import static fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory.newLogbookOperationParameters;
import static fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess.TRACEABILITY;

/**
 * Handle specific steps of the traceability process for Storage
 */
public class LogbookStorageTraceabilityHelper implements LogbookTraceabilityHelper {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogbookStorageTraceabilityHelper.class);

    private static final String STRATEGY_ID = "default";
    private static final String STORAGE_SECURISATION_STORAGE = "STORAGE_SECURISATION_STORAGE";
    private static final String STP_STORAGE_SECURISATION = "STP_STORAGE_SECURISATION";
    private static final String TIMESTAMP = "STORAGE_SECURISATION_TIMESTAMP";
    private static final String ZIP_NAME = "StorageTraceability";
    private static final String CONTAINER = DataCategory.STORAGETRACEABILITY.getFolder();

    private final LogbookOperationsClient logbookOperationsClient;
    private final TraceabilityStorageService traceabilityLogbookService;
    private final WorkspaceClient workspaceClient;
    private final GUID operationID;
    private final int delay;

    private StorageTraceabilityData lastTraceabilityData = null;
    private LocalDateTime traceabilityStartDate;
    private LocalDateTime traceabilityEndDate;
    private TraceabilityIterator<OfferLog> traceabilityIterator = null;

    private Boolean isLastEventInit = false;
    private String previousStartDate = null;
    private byte[] previousTimestampToken = null;

    /**
     * @param logbookOperations     used to search the operation to secure
     * @param operationID           guid of the traceability operation
     * @param overlapDelayInSeconds the overlap delay in second used to avoid to forgot logbook operation for traceability
     */
    public LogbookStorageTraceabilityHelper(LogbookOperationsClient logbookOperations, WorkspaceClient workspaceClient,
        TraceabilityStorageService traceabilityLogbookService, GUID operationID, int overlapDelayInSeconds) {
        this.logbookOperationsClient = logbookOperations;
        this.workspaceClient = workspaceClient;
        this.traceabilityLogbookService = traceabilityLogbookService;
        this.operationID = operationID;
        this.delay = overlapDelayInSeconds;
    }

    @Override
    public void initialize() throws TraceabilityException {
        this.traceabilityEndDate = LocalDateUtil.now();
        String fileName;
        try {
            fileName = traceabilityLogbookService.getLastTraceability(STRATEGY_ID);
            if (fileName == null) {
                lastTraceabilityData = null;
                this.traceabilityStartDate = INITIAL_START_DATE;
                return;
            }
        } catch (StorageException e) {
            throw new TraceabilityException("Unable to get last traceability in database", e);
        }

        Response response = null;
        try {
            response = traceabilityLogbookService.getObject(STRATEGY_ID, fileName, DataCategory.STORAGETRACEABILITY);
            try (
                InputStream stream = response.readEntity(InputStream.class);
                ArchiveInputStream archiveInputStream = new VitamArchiveStreamFactory()
                    .createArchiveInputStream(CommonMediaType.ZIP_TYPE, stream)) {

                ArchiveEntry entry = null;
                while (entry == null || !"token.tsp".equals(entry.getName())) {
                    entry = archiveInputStream.getNextEntry();
                    if (entry == null) {
                        throw new TraceabilityException("Can't find token.tsp file in ZIP");
                    }
                }

                LocalDateTime date = StorageFileNameHelper.parseDateFromStorageTraceabilityFileName(fileName);
                lastTraceabilityData =
                    new StorageTraceabilityData(IOUtils.toByteArray(archiveInputStream), date.minusSeconds(delay));
                this.traceabilityStartDate = lastTraceabilityData.startDate;
            }
        } catch (IOException e) {
            throw new TraceabilityException("Unable to read ZIP", e);
        } catch (ArchiveException e) {
            throw new TraceabilityException("Unable to create Archive Stream", e);
        } catch (StorageException e) {
            throw new TraceabilityException("Unable to get last traceability information", e);
        } finally {
            StreamUtils.consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public void saveDataInZip(MerkleTreeAlgo algo, TraceabilityFile file)
        throws IOException, TraceabilityException {

        try {
            traceabilityIterator =
                traceabilityLogbookService.getLastSavedStorageLogs(STRATEGY_ID, this.traceabilityStartDate);
        } catch (StorageException e) {
            throw new TraceabilityException("Unable to get last backup in database", e);
        }

        file.initStoreLog();
        while (traceabilityIterator.hasNext()) {
            final OfferLog storageFile = traceabilityIterator.next();
            String fileName = storageFile.getFileName();
            Digest digest = new Digest(VitamConfiguration.getDefaultDigestType());
            Response response = null;
            InputStream stream = null;
            try {
                response = traceabilityLogbookService.getObject(STRATEGY_ID, fileName, DataCategory.STORAGELOG);
                stream = response.readEntity(InputStream.class);
                byte[] hash = digest.update(stream).digest();

                ObjectNode fileInfo = JsonHandler.createObjectNode();
                fileInfo.put("FileName", fileName);
                fileInfo.put("Hash", hash);
                byte[] bytes = CanonicalJsonFormatter.serializeToByteArray(fileInfo);

                file.storeLog(bytes);
                algo.addLeaf(bytes);
            } catch (StorageException e) {
                throw new TraceabilityException("Unable to get the given object " + fileName, e);
            }  finally {
                StreamUtils.closeSilently(stream);
                StreamUtils.consumeAnyEntityAndClose(response);
            }
        }

        file.closeStoreLog();
    }

    @Override
    public String getPreviousStartDate() throws InvalidParseOperationException {
        if (!isLastEventInit) {
            extractPreviousEvent();
        }
        return previousStartDate;
    }

    @Override
    public byte[] getPreviousTimestampToken() throws InvalidParseOperationException {
        if (!isLastEventInit) {
            extractPreviousEvent();
        }
        return previousTimestampToken;
    }

    @Override
    public String getPreviousMonthStartDate() {
        // Never link to previous Year/Month token for storage
        return null;
    }

    @Override
    public byte[] getPreviousMonthTimestampToken() {
        // Never link to previous Year/Month token for storage
        return null;
    }

    @Override
    public String getPreviousYearStartDate() {
        // Never link to previous Year/Month token for storage
        return null;
    }

    @Override
    public byte[] getPreviousYearTimestampToken() {
        // Never link to previous Year/Month token for storage
        return null;
    }

    @Override
    public void createLogbookOperationStructure() throws TraceabilityException {
        final LogbookOperationParameters logbookParameters =
            newLogbookOperationParameters(operationID, STP_STORAGE_SECURISATION, operationID, TRACEABILITY, STARTED,
                null,
                null, operationID);

        LogbookOperationsClientHelper.checkLogbookParameters(logbookParameters);
        try {
            logbookOperationsClient.create(logbookParameters);
        } catch (LogbookClientBadRequestException | LogbookClientAlreadyExistsException | LogbookClientServerException e) {
            throw new TraceabilityException("unable to create traceability logbook", e);
        }
    }

    @Override
    public void createLogbookOperationEvent(Integer tenantId, String eventType, StatusCode status,
        TraceabilityEvent event) throws TraceabilityException {
        final GUID eventId = GUIDFactory.newEventGUID(tenantId);
        final LogbookOperationParameters logbookOperationParameters =
            newLogbookOperationParameters(eventId, eventType, operationID, TRACEABILITY, status, null, null,
                operationID);

        LogbookOperationsClientHelper.checkLogbookParameters(logbookOperationParameters);

        if (event != null) {
            String eventData = unprettyPrint(event);
            logbookOperationParameters
                .putParameterValue(LogbookParameterName.eventDetailData, eventData);
            logbookOperationParameters.putParameterValue(LogbookParameterName.masterData, eventData);
        }
        try {
            logbookOperationsClient.update(logbookOperationParameters);
        } catch (LogbookClientBadRequestException | LogbookClientServerException | LogbookClientNotFoundException e) {
            throw new TraceabilityException("unable to update traceability logbook", e);
        }
    }

    @Override
    public void saveEvent(TraceabilityEvent event) {
        // Nothing to do, event is saved in logbook after with 'createLogbookOperationEvent'
    }

    @Override
    public void saveEmpty(Integer tenantId) throws TraceabilityException {
        createLogbookOperationEvent(tenantId, STP_STORAGE_SECURISATION, StatusCode.WARNING, null);
    }

    @Override
    public void storeAndDeleteZip(Integer tenant, File zipFile, String fileName, String uri, TraceabilityEvent event)
        throws TraceabilityException {
        try (InputStream inputStream = new BufferedInputStream(new FileInputStream(zipFile))) {

            String containerName = VitamThreadUtils.getVitamSession().getRequestId() + "-Traceability";
            try {
                workspaceClient.createContainer(containerName);
            } catch (ContentAddressableStorageAlreadyExistException e) {
                // Already exists
                SysErrLogger.FAKE_LOGGER.ignoreLog(e);
            }
            workspaceClient.putObject(containerName, uri, inputStream);

            final StorageClientFactory storageClientFactory = StorageClientFactory.getInstance();

            final ObjectDescription description = new ObjectDescription();
            description.setWorkspaceContainerGUID(containerName);
            description.setWorkspaceObjectURI(uri);

            try (final StorageClient storageClient = storageClientFactory.getClient()) {

                storageClient.storeFileFromWorkspace(
                    STRATEGY_ID, DataCategory.STORAGETRACEABILITY, fileName, description);
                workspaceClient.deleteContainer(containerName, true);

                createLogbookOperationEvent(tenant, STORAGE_SECURISATION_STORAGE, OK, event);

            } catch (StorageAlreadyExistsClientException | StorageNotFoundClientException |
                StorageServerClientException | ContentAddressableStorageNotFoundException e) {
                LOGGER.error("unable to store zip file", e);
                createLogbookOperationEvent(tenant, STORAGE_SECURISATION_STORAGE, StatusCode.FATAL, event);
                throw new TraceabilityException(e);
            }
        } catch (ContentAddressableStorageServerException | IOException e) {
            LOGGER.error("unable to create container", e);
            createLogbookOperationEvent(tenant, STORAGE_SECURISATION_STORAGE, StatusCode.FATAL, event);
            throw new TraceabilityException(e);
        } finally {
            if (!zipFile.delete()) {
                LOGGER.error("Unable to delete zipFile");
            }
        }
    }

    @Override
    public String getStepName() {
        return STP_STORAGE_SECURISATION;
    }

    @Override
    public String getTimestampStepName() {
        return TIMESTAMP;
    }

    @Override
    public String getZipName() {
        return ZIP_NAME;
    }

    @Override
    public String getUriName() {
        return CONTAINER;
    }

    @Override
    public TraceabilityType getTraceabilityType() {
        return TraceabilityType.STORAGE;
    }

    @Override
    public boolean getMaxEntriesReached() {
        return false;
    }

    @Override
    public TraceabilityStatistics getTraceabilityStatistics() {
        return null;
    }

    @Override
    public long getDataSize() throws TraceabilityException {
        if (traceabilityIterator != null) {
            return traceabilityIterator.getNumberOfLines();
        }
        throw new TraceabilityException("Iterator is not yet initialized");
    }

    @Override
    public String getTraceabilityStartDate() {
        return LocalDateUtil.getFormattedDateForMongo(traceabilityStartDate);
    }

    @Override
    public String getTraceabilityEndDate() {
        return LocalDateUtil.getFormattedDateForMongo(traceabilityEndDate);
    }

    private void extractPreviousEvent() throws InvalidParseOperationException {
        if (lastTraceabilityData != null) {
            previousTimestampToken = lastTraceabilityData.token;
            previousStartDate = LocalDateUtil.getString(lastTraceabilityData.startDate);
        }
        isLastEventInit = true;
    }
}
