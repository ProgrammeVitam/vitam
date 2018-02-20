package fr.gouv.vitam.worker.model;

import static com.google.common.collect.Lists.newArrayList;
import static fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycleMongoDbName.eventDateTime;
import static fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycleMongoDbName.eventTypeProcess;
import static fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbName.eventDetailData;
import static fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbName.eventIdentifier;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.security.merkletree.MerkleTreeAlgo;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.TraceabilityException;
import fr.gouv.vitam.logbook.common.model.TraceabilityEvent;
import fr.gouv.vitam.logbook.common.model.TraceabilityFile;
import fr.gouv.vitam.logbook.common.model.TraceabilityType;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookDocument;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbName;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookOperation;
import fr.gouv.vitam.logbook.common.server.exception.LogbookDatabaseException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookNotFoundException;
import fr.gouv.vitam.logbook.common.traceability.LogbookTraceabilityHelper;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageAlreadyExistsClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.bouncycastle.util.Strings;

public class LogbookLifeCycleTraceabilityHelper implements LogbookTraceabilityHelper {
    private static final VitamLogger LOGGER =
        VitamLoggerFactory.getInstance(LogbookLifeCycleTraceabilityHelper.class);

    private static final String DEFAULT_STRATEGY = "default";
    private static final String ZIP_NAME = "LogbookLifecycles";
    private static final String LOGBOOK = "logbook";
    private static final String EVENT_DATE_TIME = eventDateTime.getDbname();
    private static final String EVENT_TYPE_PROC = eventTypeProcess.getDbname();
    private static final String EVENT_ID = eventIdentifier.getDbname();
    private static final String EVENT_DETAIL_DATA = eventDetailData.getDbname();
    private static final String HANDLER_ID = "FINALIZE_LC_TRACEABILITY";
    private static final String HANDLER_SUB_ACTION_TIMESTAMP = "OP_SECURISATION_TIMESTAMP";
    private static final String HANDLER_SUB_ACTION_SECURISATION_STORAGE = "OP_SECURISATION_STORAGE";
    private static final int LAST_OPERATION_LIFECYCLES_RANK = 0;
    private static final int TRACEABILITY_INFORMATION_RANK = 1;

    private final HandlerIO handlerIO;
    private final LogbookOperationsClient logbookOperationsClient;
    private final ItemStatus itemStatus;
    private final String operationID;

    private LogbookOperation lastTraceabilityOperation = null;
    private List<String> expectedLogbookId = null;
    private JsonNode traceabilityInformation = null;

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
     * @param handlerIO Workflow Input/Output of the traceability event
     * @param logbookOperationsClient used to search the operation to secure
     * @param itemStatus used by workflow, event must be updated here
     * @param operationID of the current traceability process
     */
    public LogbookLifeCycleTraceabilityHelper(HandlerIO handlerIO, LogbookOperationsClient logbookOperationsClient,
        ItemStatus itemStatus, String operationID) {
        this.handlerIO = handlerIO;
        this.logbookOperationsClient = logbookOperationsClient;
        this.itemStatus = itemStatus;
        this.operationID = operationID;
    }

    @Override
    public LocalDateTime getLastEvent()
        throws TraceabilityException {

        LocalDateTime resultDate = INITIAL_START_DATE;
        try {
            File operationFile = (File) handlerIO.getInput(LAST_OPERATION_LIFECYCLES_RANK);
            JsonNode operationJson = null;
            if (operationFile != null) {
                operationJson = JsonHandler.getFromFile(operationFile);
            }

            if (operationJson != null && operationJson.isObject() && operationJson.get("evId") != null) {
                lastTraceabilityOperation = new LogbookOperation(operationJson);
            }
            File traceabilityInformationFile = (File) handlerIO.getInput(TRACEABILITY_INFORMATION_RANK);
            if (traceabilityInformationFile != null) {
                traceabilityInformation = JsonHandler.getFromFile(traceabilityInformationFile);
            }
        } catch (InvalidParseOperationException e) {
            LOGGER.warn("Cannot parse logbook operation", e);
        }
        expectedLogbookId = newArrayList(operationID);
        if (lastTraceabilityOperation != null) {
            try {
                LocalDateUtil.getDate(lastTraceabilityOperation.getString(EVENT_DATE_TIME));
            } catch (ParseException e) {
                throw new TraceabilityException("Invalid date", e);
            }
            expectedLogbookId.add(lastTraceabilityOperation.getString(EVENT_ID));

            String startDate = traceabilityInformation.get("startDate").asText();
            try {
                Date date = LocalDateUtil.getDate(startDate);
                resultDate = LocalDateUtil.fromDate(date);
            } catch (ParseException e) {
                throw new TraceabilityException("Invalid date", e);
            }
        }

        return resultDate;
    }

    @Override
    public LocalDateTime saveDataInZip(MerkleTreeAlgo algo,
        LocalDateTime startDate, TraceabilityFile file) throws IOException,
        TraceabilityException {

        file.initStoreLog();
        try {
            List<URI> uriListLFCObjectsWorkspace =
                handlerIO.getUriList(handlerIO.getContainerName(), SedaConstants.LFC_OBJECTS_FOLDER);
            extractAppendToFinalFile(uriListLFCObjectsWorkspace, file, algo, SedaConstants.LFC_OBJECTS_FOLDER);

            List<URI> uriListLFCUnitsWorkspace =
                handlerIO.getUriList(handlerIO.getContainerName(), SedaConstants.LFC_UNITS_FOLDER);
            extractAppendToFinalFile(uriListLFCUnitsWorkspace, file, algo, SedaConstants.LFC_UNITS_FOLDER);

        } catch (ProcessingException e) {
            throw new TraceabilityException(e);
        } finally {
            file.closeStoreLog();
        }

        return startDate;
    }

    @Override
    public void createLogbookOperationStructure()
        throws TraceabilityException {
        // Nothing to do: Master event is make and updated by workflow
    }

    @Override
    public void createLogbookOperationEvent(Integer tenantId,
        String eventType, StatusCode status, TraceabilityEvent event)
        throws TraceabilityException {

        if (!getStepName().equals(eventType)) {
            final ItemStatus subItemStatusTimestamp = new ItemStatus(eventType);
            itemStatus.setItemsStatus(eventType, subItemStatusTimestamp.increment(status));
        }
    }

    @Override
    public void saveEvent(TraceabilityEvent event) {
        String evDetailData = JsonHandler.unprettyPrint(event);
        itemStatus.setEvDetailData(evDetailData);
        itemStatus.setMasterData(LogbookParameterName.eventDetailData.name(),
            evDetailData);
    }

    @Override
    public void storeAndDeleteZip(Integer tenant, File zipFile,
        String fileName, String uri, TraceabilityEvent event)
        throws TraceabilityException {

        final ItemStatus subItemStatusSecurisationStorage = new ItemStatus(HANDLER_SUB_ACTION_SECURISATION_STORAGE);
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
                    DEFAULT_STRATEGY, DataCategory.LOGBOOK, fileName, description);
                workspaceClient.deleteContainer(fileName, true);
                subItemStatusSecurisationStorage.setEvDetailData(JsonHandler.unprettyPrint(event));
                itemStatus.setItemsStatus(HANDLER_SUB_ACTION_SECURISATION_STORAGE,
                    subItemStatusSecurisationStorage.increment(StatusCode.OK));
            } catch (StorageAlreadyExistsClientException | StorageNotFoundClientException |
                StorageServerClientException | ContentAddressableStorageNotFoundException e) {
                itemStatus.setItemsStatus(HANDLER_SUB_ACTION_SECURISATION_STORAGE,
                    subItemStatusSecurisationStorage.increment(StatusCode.FATAL));
                throw new TraceabilityException("unable to store zip file", e);
            }
        } catch (ContentAddressableStorageAlreadyExistException | ContentAddressableStorageServerException |
            IOException e) {
            itemStatus.setItemsStatus(HANDLER_SUB_ACTION_SECURISATION_STORAGE,
                subItemStatusSecurisationStorage.increment(StatusCode.FATAL));
            throw new TraceabilityException("unable to create container", e);
        } finally {
            zipFile.delete();
        }
    }

    @Override
    public TraceabilityType getTraceabilityType() {
        return TraceabilityType.LIFECYCLE;
    }

    @Override
    public String getStepName() {
        return HANDLER_ID;
    }

    @Override
    public String getTimestampStepName() {
        return HANDLER_SUB_ACTION_TIMESTAMP;
    }

    @Override
    public String getZipName() {
        return ZIP_NAME;
    }

    @Override
    public Object getUriName() {
        return LOGBOOK;
    }

    @Override
    public String getEndDate() throws TraceabilityException {
        if (traceabilityInformation != null && traceabilityInformation.get("endDate") != null) {
            return traceabilityInformation.get("endDate").asText();
        }
        return "";
    }

    @Override
    public Long getDataSize() throws TraceabilityException {
        if (traceabilityInformation != null &&
            traceabilityInformation.get("numberUnitLifecycles") != null &&
            traceabilityInformation.get("numberObjectLifecycles") != null) {

            return traceabilityInformation.get("numberUnitLifecycles").asLong() +
                traceabilityInformation.get("numberObjectLifecycles").asLong();
        }
        return 0L;
    }

    @Override
    public byte[] getPreviousTimestampToken()
        throws InvalidParseOperationException {
        if (!isLastEventInit) {
            extractPreviousEvent();
        }
        return previousTimestampToken;
    }

    @Override
    public byte[] getPreviousMonthTimestampToken(LocalDateTime currentDate)
        throws InvalidParseOperationException, TraceabilityException {
        if (!isLastMonthEventInit) {
            extractPreviousEvent(currentDate.minusMonths(1), true);
        }
        return previousMonthTimestampToken;
    }

    @Override
    public byte[] getPreviousYearTimestampToken(LocalDateTime currentDate)
        throws InvalidParseOperationException, TraceabilityException {
        if (!isLastYearEventInit) {
            extractPreviousEvent(currentDate.minusYears(1), false);
        }
        return previousYearTimestampToken;
    }

    @Override
    public String getPreviousStartDate() throws InvalidParseOperationException {
        if (!isLastEventInit) {
            extractPreviousEvent();
        }
        return previousStartDate;
    }

    @Override
    public String getPreviousMonthStartDate(LocalDateTime currentDate)
        throws InvalidParseOperationException, TraceabilityException {
        if (!isLastMonthEventInit) {
            extractPreviousEvent(currentDate.minusMonths(1), true);
        }
        return previousMonthStartDate;
    }

    @Override
    public String getPreviousYearStartDate(LocalDateTime currentDate)
        throws InvalidParseOperationException, TraceabilityException {
        if (!isLastYearEventInit) {
            extractPreviousEvent(currentDate.minusYears(1), false);
        }
        return previousYearStartDate;
    }

    /**
     * Reduce part of the process : lets merge in one only file the disting lifecycles ones
     *
     * @param listOfFiles
     * @param traceabilityFile
     * @param algo
     * @param rootFolder
     * @return
     * @throws ContentAddressableStorageNotFoundException
     * @throws ContentAddressableStorageServerException
     * @throws IOException
     * @throws TraceabilityException
     */
    private void extractAppendToFinalFile(List<URI> listOfFiles, TraceabilityFile traceabilityFile, MerkleTreeAlgo algo,
        String rootFolder)
        throws TraceabilityException {
        try {
            for (final URI uriWorkspace : listOfFiles) {
                try (InputStream lifecycleIn =
                    handlerIO.getInputStreamFromWorkspace(rootFolder + "/" + uriWorkspace.getPath());) {

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    org.apache.commons.io.IOUtils.copy(lifecycleIn, baos);
                    byte[] bytes = baos.toByteArray();
                    String lifecycleDataStr = Strings.fromByteArray(bytes);

                    traceabilityFile.storeLog(bytes);
                    algo.addLeaf(lifecycleDataStr);
                }
            }

        } catch (ContentAddressableStorageNotFoundException | ContentAddressableStorageServerException | IOException e) {
            LOGGER.error("Error while storing files in ZIP");
            throw new TraceabilityException("Error while storing files in ZIP", e);
        }
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

    private void extractPreviousEvent(LocalDateTime specificDate, Boolean isMonth)
        throws InvalidParseOperationException, TraceabilityException {
        try {
            previousMonthTimestampToken =
                findHashByTraceabilityEventExpect(specificDate);
            final LogbookOperation oneMounthBeforeTraceabilityOperation =
                findFirstTraceabilityOperationOKAfterDate(specificDate);
            if (oneMounthBeforeTraceabilityOperation != null) {
                TraceabilityEvent oneMonthBeforeTraceabilityEvent =
                    extractEventDetData(oneMounthBeforeTraceabilityOperation);
                if (oneMonthBeforeTraceabilityEvent != null) {
                    previousMonthStartDate = oneMonthBeforeTraceabilityEvent.getStartDate();
                }
            }
        } catch (LogbookNotFoundException | LogbookDatabaseException | InvalidCreateOperationException | LogbookClientException e) {
            throw new TraceabilityException(e);
        }

        if (isMonth) {
            isLastMonthEventInit = true;
        } else {
            isLastYearEventInit = true;
        }
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
        return JsonHandler.getFromString((String) logbookOperation.get(EVENT_DETAIL_DATA), TraceabilityEvent.class);
    }

    private byte[] findHashByTraceabilityEventExpect(LocalDateTime date)
        throws InvalidCreateOperationException, InvalidParseOperationException, LogbookClientException {
        try {
            RequestResponseOK<JsonNode> requestResponseOK =
                RequestResponseOK.getFromJsonNode(
                    logbookOperationsClient.selectOperation(generateSelectLogbookOperation(date).getFinalSelect()));
            List<JsonNode> foundOperation = requestResponseOK.getResults();
            if (foundOperation != null && !foundOperation.isEmpty()) {
                LogbookOperation lastOperationTraceabilityLifecycle = new LogbookOperation(foundOperation.get(0));
                if (!expectedLogbookId.contains(lastOperationTraceabilityLifecycle.getString(EVENT_ID))) {
                    expectedLogbookId.add(lastOperationTraceabilityLifecycle.getString(EVENT_ID));
                    return extractTimestampToken(lastOperationTraceabilityLifecycle);
                }
            }
            return null;
        } catch (LogbookClientNotFoundException e) {
            LOGGER.warn("Logbook operation not found, there is no Operation");
        }
        return null;
    }

    private LogbookOperation findFirstTraceabilityOperationOKAfterDate(LocalDateTime date)
        throws InvalidCreateOperationException, LogbookNotFoundException, LogbookDatabaseException,
        InvalidParseOperationException, LogbookClientException {
        try {
            RequestResponseOK<JsonNode> requestResponseOK =
                RequestResponseOK.getFromJsonNode(
                    logbookOperationsClient.selectOperation(generateSelectLogbookOperation(date).getFinalSelect()));
            List<JsonNode> foundOperation = requestResponseOK.getResults();
            if (foundOperation != null && foundOperation.size() == 1) {
                return new LogbookOperation(foundOperation.get(0));
            }
        } catch (LogbookClientNotFoundException e) {
            LOGGER.warn("Logbook operation not found, there is no Operation");
        }
        return null;
    }

    private Select generateSelectLogbookOperation(LocalDateTime date) throws InvalidCreateOperationException {
        final Select select = new Select();
        final Query query = QueryHelper.gt(EVENT_DATE_TIME, date.toString());
        final Query type = QueryHelper.eq(EVENT_TYPE_PROC, LogbookTypeProcess.TRACEABILITY.name());
        final Query findEvent = QueryHelper
            .eq(String.format("%s.%s", LogbookDocument.EVENTS, LogbookMongoDbName.outcomeDetail.getDbname()),
                "LOGBOOK_LC_SECURISATION.OK");
        select.setQuery(QueryHelper.and().add(query, type, findEvent));
        select.setLimitFilter(0, 1);
        return select;
    }

}
