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

package fr.gouv.vitam.logbook.administration.core;

import static com.google.common.collect.Lists.newArrayList;
import static fr.gouv.vitam.common.json.JsonHandler.unprettyPrint;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.common.model.StatusCode.STARTED;
import static fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory.newLogbookOperationParameters;
import static fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess.TRACEABILITY;
import static fr.gouv.vitam.logbook.common.server.database.collections.LogbookDocument.EVENTS;
import static fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycleMongoDbName.eventDateTime;
import static fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbName.eventDetailData;
import static fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbName.eventIdentifier;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import org.bson.Document;

import com.google.common.collect.Iterables;
import com.mongodb.client.MongoCursor;

import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.security.merkletree.MerkleTreeAlgo;
import fr.gouv.vitam.logbook.common.exception.TraceabilityException;
import fr.gouv.vitam.logbook.common.model.TraceabilityEvent;
import fr.gouv.vitam.logbook.common.model.TraceabilityFile;
import fr.gouv.vitam.logbook.common.model.TraceabilityIterator;
import fr.gouv.vitam.logbook.common.model.TraceabilityType;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationsClientHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookOperation;
import fr.gouv.vitam.logbook.common.server.exception.LogbookAlreadyExistsException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookDatabaseException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookNotFoundException;
import fr.gouv.vitam.logbook.common.traceability.LogbookTraceabilityHelper;
import fr.gouv.vitam.logbook.operations.api.LogbookOperations;
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

public class LogbookOperationTraceabilityHelper implements LogbookTraceabilityHelper {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogbookOperationTraceabilityHelper.class);

    private static final String STRATEGY_ID = "default";
    private static final String OP_SECURISATION_STORAGE = "OP_SECURISATION_STORAGE";
    private static final String STP_OP_SECURISATION = "STP_OP_SECURISATION";
    private static final String TIMESTAMP = "OP_SECURISATION_TIMESTAMP";
    private static final String EVENT_DATE_TIME = eventDateTime.getDbname();
    private static final String EVENT_ID = eventIdentifier.getDbname();
    private static final String EVENT_DETAIL_DATA = eventDetailData.getDbname();
    private static final String ZIP_NAME = "LogbookOperation";
    private static final String LOGBOOK = "logbook";

    private final LogbookOperations logbookOperations;
    private final GUID operationID;
    private final int delay;

    private List<String> expectedLogbookId = null;
    private LogbookOperation lastTraceabilityOperation = null;
    private TraceabilityIterator<LogbookOperation> traceabilityIterator = null;

    private Boolean isLastEventInit = false;
    private Boolean isLastMonthEventInit = false;
    private Boolean isLastYearEventInit = false;
    private String previousStartDate = null;
    private String previousMonthStartDate = null;
    private String previousYearStartDate = null;
    private byte[] previousTimestampToken = null;
    private byte[] previousMonthTimestampToken = null;
    private byte[] previousYearTimestampToken = null;

    /**
     * @param logbookOperations used to search the operation to secure
     * @param operationID guid of the traceability operation
     * @param overlapDelayInSeconds the overlap delay in second used to avoid to forgot logbook operation for traceability
     */
    public LogbookOperationTraceabilityHelper(LogbookOperations logbookOperations,
        GUID operationID, int overlapDelayInSeconds) {
        this.logbookOperations = logbookOperations;
        this.operationID = operationID;
        this.delay = overlapDelayInSeconds;
    }

    @Override
    public LocalDateTime getLastEvent() throws TraceabilityException {
        expectedLogbookId = newArrayList(operationID.getId());
        try {
            lastTraceabilityOperation = logbookOperations.findLastTraceabilityOperationOK();
        } catch (LogbookNotFoundException | LogbookDatabaseException | InvalidParseOperationException
            | InvalidCreateOperationException e) {
            throw new TraceabilityException(e);
        }
        LocalDateTime startDate;

        if (lastTraceabilityOperation == null) {
            startDate = INITIAL_START_DATE;
        } else {
            Date date;
            try {
                date = LocalDateUtil.getDate(lastTraceabilityOperation.getString(EVENT_DATE_TIME));
            } catch (ParseException e) {
                throw new TraceabilityException("Invalid date");
            }
            LocalDateTime lastStartDate = LocalDateUtil.fromDate(date);
            startDate = lastStartDate.minusSeconds(delay);
            expectedLogbookId.add(lastTraceabilityOperation.getString(EVENT_ID));
        }

        return startDate;
    }

    @Override
    public LocalDateTime saveDataInZip(MerkleTreeAlgo algo, LocalDateTime startDate, TraceabilityFile file)
        throws IOException, TraceabilityException {
        MongoCursor<LogbookOperation> mongoCursor;
        try {
            mongoCursor = logbookOperations.selectOperationsPersistedAfterDate(startDate);
        } catch (LogbookDatabaseException | LogbookNotFoundException | InvalidParseOperationException | InvalidCreateOperationException e) {
            throw new TraceabilityException(e);
        }
        traceabilityIterator = new LogbookTraceabilityIterator(mongoCursor);

        file.initStoreLog();

        while (traceabilityIterator.hasNext()) {

            final LogbookOperation logbookOperation = traceabilityIterator.next();
            logbookOperation.remove(VitamDocument.SCORE);
            final String logbookOperationStr = JsonHandler.unprettyPrint(logbookOperation);

            file.storeLog(logbookOperationStr.getBytes(StandardCharsets.UTF_8));
            algo.addLeaf(logbookOperationStr);

            if (INITIAL_START_DATE.equals(startDate) &&
                logbookOperation.getString(EVENT_DATE_TIME) != null) {
                startDate = LocalDateTime.parse(logbookOperation.getString(EVENT_DATE_TIME));
            }
        }

        file.closeStoreLog();
        return startDate;
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
    public String getPreviousMonthStartDate(LocalDateTime currentDate) throws InvalidParseOperationException, TraceabilityException {
        if (!isLastMonthEventInit) {
            extractMonthPreviousEvent(currentDate);
        }
        return previousMonthStartDate;
    }

    @Override
    public byte[] getPreviousMonthTimestampToken(LocalDateTime currentDate) throws InvalidParseOperationException, TraceabilityException {
        if (!isLastMonthEventInit) {
            extractMonthPreviousEvent(currentDate);
        }
        return previousMonthTimestampToken;
    }

    @Override
    public String getPreviousYearStartDate(LocalDateTime currentDate) throws InvalidParseOperationException, TraceabilityException {
        if (!isLastYearEventInit) {
            extractYearPreviousEvent(currentDate);
        }
        return previousYearStartDate;
    }

    @Override
    public byte[] getPreviousYearTimestampToken(LocalDateTime currentDate) throws InvalidParseOperationException, TraceabilityException {
        if (!isLastYearEventInit) {
            extractYearPreviousEvent(currentDate);
        }
        return previousYearTimestampToken;
    }

    @Override
    public void createLogbookOperationStructure() throws TraceabilityException {
        final LogbookOperationParameters logbookParameters =
            newLogbookOperationParameters(operationID, STP_OP_SECURISATION, operationID, TRACEABILITY, STARTED, null,
                null, operationID);

        LogbookOperationsClientHelper.checkLogbookParameters(logbookParameters);
        try {
            logbookOperations.create(logbookParameters);
        } catch (LogbookDatabaseException | LogbookAlreadyExistsException e) {
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
            logbookOperations.update(logbookOperationParameters);
        } catch (LogbookNotFoundException | LogbookDatabaseException e) {
            throw new TraceabilityException("unable to update traceability logbook", e);
        }
    }

    @Override
    public void saveEvent(TraceabilityEvent event) {
        // Nothing to do, event is saved in logbook after with 'createLogbookOperationEvent'
    }
    
    @Override
    public void saveEmpty(Integer tenantId) throws TraceabilityException {
        createLogbookOperationEvent(tenantId, STP_OP_SECURISATION, StatusCode.WARNING, null);
    }

    @Override
    public void storeAndDeleteZip(Integer tenant, File zipFile, String fileName, String uri, TraceabilityEvent event)
        throws TraceabilityException {
        try (InputStream inputStream = new BufferedInputStream(new FileInputStream(zipFile));
            final WorkspaceClient workspaceClient = WorkspaceClientFactory.getInstance().getClient()) {

            workspaceClient.createContainer(fileName);
            workspaceClient.putObject(fileName, uri, inputStream);

            final StorageClientFactory storageClientFactory = StorageClientFactory.getInstance();

            final ObjectDescription description = new ObjectDescription();
            description.setWorkspaceContainerGUID(fileName);
            description.setWorkspaceObjectURI(uri);

            try (final StorageClient storageClient = storageClientFactory.getClient()) {

                storageClient.storeFileFromWorkspace(
                    STRATEGY_ID, DataCategory.LOGBOOK, fileName, description);
                workspaceClient.deleteContainer(fileName, true);

                createLogbookOperationEvent(tenant, OP_SECURISATION_STORAGE, OK, event);

            } catch (StorageAlreadyExistsClientException | StorageNotFoundClientException |
                StorageServerClientException | ContentAddressableStorageNotFoundException e) {
                createLogbookOperationEvent(tenant, OP_SECURISATION_STORAGE, StatusCode.FATAL, event);
                LOGGER.error("unable to store zip file", e);
                throw new TraceabilityException(e);
            }
        } catch (ContentAddressableStorageAlreadyExistException | ContentAddressableStorageServerException |
            IOException e) {
            LOGGER.error("unable to create container", e);
            createLogbookOperationEvent(tenant, OP_SECURISATION_STORAGE, StatusCode.FATAL, event);
            throw new TraceabilityException(e);
        } finally {
            if (!zipFile.delete()) {
                LOGGER.error("Unable to delete zipFile");
            }
        }
    }

    @Override
    public String getStepName() {
        return STP_OP_SECURISATION;
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
        return LOGBOOK;
    }

    @Override
    public TraceabilityType getTraceabilityType() {
        return TraceabilityType.OPERATION;
    }

    @Override
    public Long getDataSize() throws TraceabilityException {
        if (traceabilityIterator != null) {
            return traceabilityIterator.getNumberOfLines();
        }
        throw new TraceabilityException("Iterator is not yet initialized");
    }

    @Override
    public String getEndDate() throws TraceabilityException {
        if (traceabilityIterator != null) {
            return traceabilityIterator.endDate();
        }
        throw new TraceabilityException("Iterator is not yet initialized");
    }

    private void extractPreviousEvent() throws InvalidParseOperationException {
        previousTimestampToken = extractTimestampToken(lastTraceabilityOperation);
        if (lastTraceabilityOperation != null) {
            TraceabilityEvent lastTraceabilityEvent = extractEventDetData(lastTraceabilityOperation);
            if (lastTraceabilityEvent != null) {
                previousStartDate = lastTraceabilityEvent.getStartDate();
            }
        }
        isLastEventInit = true;
    }

    private void extractMonthPreviousEvent(LocalDateTime currentDate)
        throws InvalidParseOperationException, TraceabilityException {
        try {
            previousMonthTimestampToken =
                findHashByTraceabilityEventExpect(expectedLogbookId, currentDate.minusMonths(1));
            final LogbookOperation oneMounthBeforeTraceabilityOperation =
                logbookOperations.findFirstTraceabilityOperationOKAfterDate(currentDate.minusMonths(1));
            if (oneMounthBeforeTraceabilityOperation != null) {
                TraceabilityEvent oneMonthBeforeTraceabilityEvent =
                    extractEventDetData(oneMounthBeforeTraceabilityOperation);
                if (oneMonthBeforeTraceabilityEvent != null) {
                    previousMonthStartDate = oneMonthBeforeTraceabilityEvent.getStartDate();
                }
            }
        } catch (LogbookNotFoundException | LogbookDatabaseException | InvalidCreateOperationException e) {
            throw new TraceabilityException(e);
        }
        isLastMonthEventInit = true;
    }

    private void extractYearPreviousEvent(LocalDateTime currentDate)
        throws InvalidParseOperationException, TraceabilityException {
        try {
            previousYearTimestampToken =
                findHashByTraceabilityEventExpect(expectedLogbookId, currentDate.minusYears(1));
            final LogbookOperation oneMounthBeforeTraceabilityOperation =
                logbookOperations.findFirstTraceabilityOperationOKAfterDate(currentDate.minusYears(1));
            if (oneMounthBeforeTraceabilityOperation != null) {
                TraceabilityEvent oneMonthBeforeTraceabilityEvent =
                    extractEventDetData(oneMounthBeforeTraceabilityOperation);
                if (oneMonthBeforeTraceabilityEvent != null) {
                    previousYearStartDate = oneMonthBeforeTraceabilityEvent.getStartDate();
                }
            }
        } catch (LogbookNotFoundException | LogbookDatabaseException | InvalidCreateOperationException e) {
            throw new TraceabilityException(e);
        }
        isLastYearEventInit = true;
    }

    private byte[] findHashByTraceabilityEventExpect(List<String> expectIds, LocalDateTime date)
        throws InvalidCreateOperationException, LogbookNotFoundException, LogbookDatabaseException,
        InvalidParseOperationException {

        final LogbookOperation logbookOperation = logbookOperations.findFirstTraceabilityOperationOKAfterDate(date);

        if (logbookOperation == null || expectIds.contains(logbookOperation.getString(EVENT_ID))) {
            return null;
        }
        expectIds.add(logbookOperation.getString(EVENT_ID));
        return extractTimestampToken(logbookOperation);
    }

    byte[] extractTimestampToken(LogbookOperation logbookOperation) throws InvalidParseOperationException {
        TraceabilityEvent traceabilityEvent = extractEventDetData(logbookOperation);
        if (traceabilityEvent == null) {
            return null;
        }
        return traceabilityEvent.getTimeStampToken();
    }

    private TraceabilityEvent extractEventDetData(LogbookOperation logbookOperation)
        throws InvalidParseOperationException {
        if (logbookOperation == null) {
            return null;
        }

        final List<Document> events = (List<Document>) logbookOperation.get(EVENTS);
        final Document lastEvent = Iterables.getLast(events);

        final String evDetData = (String) lastEvent.get(EVENT_DETAIL_DATA);

        return JsonHandler.getFromString(evDetData, TraceabilityEvent.class);
    }
}
