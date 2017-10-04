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

import static com.google.common.collect.Lists.newArrayList;
import static fr.gouv.vitam.common.LocalDateUtil.now;
import static fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycleMongoDbName.eventDateTime;
import static fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbName.eventDetailData;
import static fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbName.eventIdentifier;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

import org.apache.commons.compress.archivers.ArchiveException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;

import fr.gouv.vitam.common.BaseXx;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.exception.InvalidGuidOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.TimeStampException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDReader;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.timestamp.TimeStampSignature;
import fr.gouv.vitam.common.timestamp.TimeStampSignatureWithKeystore;
import fr.gouv.vitam.common.timestamp.TimestampGenerator;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.TraceabilityException;
import fr.gouv.vitam.logbook.common.model.TraceabilityEvent;
import fr.gouv.vitam.logbook.common.model.TraceabilityType;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookDocument;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbName;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookOperation;
import fr.gouv.vitam.logbook.common.server.exception.LogbookDatabaseException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookNotFoundException;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageAlreadyExistsClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.model.StorageCollectionType;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.common.utils.TraceabilityFile;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

/**
 * FinalizeLifecycleTraceabilityAction Plugin
 */
public class FinalizeLifecycleTraceabilityActionHandler extends ActionHandler {
    private static final VitamLogger LOGGER =
        VitamLoggerFactory.getInstance(FinalizeLifecycleTraceabilityActionHandler.class);

    private static final String HANDLER_ID = "FINALIZE_LC_TRACEABILITY";
    private static final String HANDLER_SUB_ACTION_TIMESTAMP = "OP_SECURISATION_TIMESTAMP";
    private static final String HANDLER_SUB_ACTION_SECURISATION_STORAGE = "OP_SECURISATION_STORAGE";

    private HandlerIO handlerIO;
    private boolean asyncIO = false;

    private static final String LOGBOOK = "logbook";

    private static final int LAST_OPERATION_LIFECYCLES_RANK = 0;
    private static final int TRACEABILITY_INFORMATION_RANK = 1;

    private static final String EVENT_DATE_TIME = eventDateTime.getDbname();
    private static final String EVENT_ID = eventIdentifier.getDbname();
    private static final String EVENT_DETAIL_DATA = eventDetailData.getDbname();
    private final TimestampGenerator timestampGenerator;
    private final Joiner joiner;
    private static final String VERIFY_TIMESTAMP_CONF_FILE = "verify-timestamp.conf";

    private static final String DEFAULT_STRATEGY = "default";

    /**
     * Empty constructor FinalizeLifecycleTraceabilityActionPlugin
     *
     */
    public FinalizeLifecycleTraceabilityActionHandler() {
        TimeStampSignature timeStampSignature;
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        VerifyTimeStampActionConfiguration configuration = null;
        try {
            configuration =
                PropertiesUtils.readYaml(PropertiesUtils.findFile(VERIFY_TIMESTAMP_CONF_FILE),
                    VerifyTimeStampActionConfiguration.class);
        } catch (IOException e) {
            LOGGER.error("Processing exception", e);
        }
        if (configuration != null) {
            try {
                final File file = PropertiesUtils.findFile(configuration.getP12LogbookFile());
                timeStampSignature =
                    new TimeStampSignatureWithKeystore(file, configuration.getP12LogbookPassword().toCharArray());
            } catch (KeyStoreException | CertificateException | IOException | UnrecoverableKeyException |
                NoSuchAlgorithmException e) {
                LOGGER.error("unable to instanciate TimeStampGenerator", e);
                throw new RuntimeException(e);
            }
            timestampGenerator = new TimestampGenerator(timeStampSignature);
            joiner = Joiner.on("").skipNulls();
        } else {
            LOGGER.error("unable to instanciate TimeStampGenerator");
            throw new RuntimeException("Configuration is null");
        }
    }

    @Override
    public ItemStatus execute(WorkerParameters params, HandlerIO handler) {
        handlerIO = handler;
        final ItemStatus itemStatus = new ItemStatus(HANDLER_ID);
        try {
            finalizeLifecycles(params, itemStatus);
            itemStatus.increment(StatusCode.OK);
        } catch (InvalidParseOperationException | TraceabilityException | LogbookException | ProcessingException e) {
            LOGGER.error("Exception while finalizing", e);
            itemStatus.increment(StatusCode.FATAL);
        }
        return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID,
            itemStatus);
    }


    /**
     * Generation and storage of the secure file for lifecycles
     * 
     * @param params
     * @param itemStatus
     * @throws InvalidParseOperationException
     * @throws TraceabilityException
     * @throws LogbookException
     * @throws ProcessingException
     */
    private void finalizeLifecycles(WorkerParameters params, ItemStatus itemStatus)
        throws InvalidParseOperationException, TraceabilityException, LogbookException, ProcessingException {
        LogbookOperation lastTraceabilityOperation = null;
        JsonNode traceabilityInformation = null;
        try {
            JsonNode operationJson = JsonHandler.getFromFile((File) handlerIO.getInput(LAST_OPERATION_LIFECYCLES_RANK));

            if (operationJson != null && operationJson.isObject() && operationJson.get("evId") != null) {
                lastTraceabilityOperation = new LogbookOperation(operationJson);
            }
            traceabilityInformation =
                JsonHandler.getFromFile((File) handlerIO.getInput(TRACEABILITY_INFORMATION_RANK));
        } catch (InvalidParseOperationException e) {
            LOGGER.warn("Cannot parse logbook operation", e);
        }
        final List<String> expectedLogbookId = newArrayList(params.getProcessId());
        if (lastTraceabilityOperation != null) {
            Date date;
            try {
                date = LocalDateUtil.getDate(lastTraceabilityOperation.getString(EVENT_DATE_TIME));
            } catch (ParseException e) {
                throw new InvalidParseOperationException("Invalid date");
            }
            expectedLogbookId.add(lastTraceabilityOperation.getString(EVENT_ID));
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        final LocalDateTime currentDate = now();
        Integer tenantId = ParameterHelper.getTenantParameter();
        final String fileName = String.format("%d_LogbookLifecycles_%s.zip", tenantId, currentDate.format(formatter));
        File tmpFolder = PropertiesUtils.fileFromTmpFolder("secure");
        tmpFolder.mkdir();
        final File zipFile = new File(tmpFolder, fileName);
        final String uri = String.format("%s/%s", LOGBOOK, fileName);
        TraceabilityEvent traceabilityEvent = null;

        try (TraceabilityFile traceabilityFile = new TraceabilityFile(zipFile)) {
            String rootHash = null;
            traceabilityFile.initStoreLifecycleLog();
            List<URI> uriListLFCObjectsWorkspace =
                handlerIO.getUriList(handlerIO.getContainerName(), SedaConstants.LFC_OBJECTS_FOLDER);
            rootHash = extractAppendToFinalFile(uriListLFCObjectsWorkspace, traceabilityFile, rootHash,
                SedaConstants.LFC_OBJECTS_FOLDER);

            List<URI> uriListLFCUnitsWorkspace =
                handlerIO.getUriList(handlerIO.getContainerName(), SedaConstants.LFC_UNITS_FOLDER);
            rootHash = extractAppendToFinalFile(uriListLFCUnitsWorkspace, traceabilityFile, rootHash,
                SedaConstants.LFC_UNITS_FOLDER);
            traceabilityFile.closeStoreLifecycleLog();

            if (uriListLFCUnitsWorkspace.size() > 0 || uriListLFCObjectsWorkspace.size() > 0) {
                final String timestampToken1 = extractTimestampToken(lastTraceabilityOperation);
                try (final LogbookOperationsClient logbookOperationsClient =
                    LogbookOperationsClientFactory.getInstance().getClient();) {
                    final String timestampToken2 =
                        findHashByTraceabilityEventExpect(logbookOperationsClient, expectedLogbookId,
                            currentDate.minusMonths(1));
                    final String timestampToken3 =
                        findHashByTraceabilityEventExpect(logbookOperationsClient, expectedLogbookId,
                            currentDate.minusYears(1));
                    final String timestampToken1Base64 =
                        (timestampToken1 == null) ? null : BaseXx.getBase64(timestampToken1.getBytes());
                    final String timestampToken2Base64 =
                        (timestampToken2 == null) ? null : BaseXx.getBase64(timestampToken2.getBytes());
                    final String timestampToken3Base64 =
                        (timestampToken3 == null) ? null : BaseXx.getBase64(timestampToken3.getBytes());

                    final byte[] timeStampToken =
                        generateTimeStampToken(GUIDReader.getGUID(params.getProcessId()), tenantId, rootHash,
                            timestampToken1, timestampToken2, timestampToken3, itemStatus);
                    traceabilityFile.storeTimeStampToken(timeStampToken);

                    long numberOfLifecycles = 0;
                    String startDate = null;
                    String endDate = null;
                    if (traceabilityInformation != null &&
                        traceabilityInformation.get("numberUnitLifecycles") != null &&
                        traceabilityInformation.get("numberObjectLifecycles") != null &&
                        traceabilityInformation.get("endDate") != null &&
                        traceabilityInformation.get("startDate") != null) {
                        numberOfLifecycles = traceabilityInformation.get("numberUnitLifecycles").asLong() +
                            traceabilityInformation.get("numberObjectLifecycles").asLong();
                        startDate = traceabilityInformation.get("startDate").asText();
                        endDate = traceabilityInformation.get("endDate").asText();
                    }

                    traceabilityFile.storeAdditionalInformation(numberOfLifecycles, startDate, endDate);
                    traceabilityFile.storeHashCalculationInformation(rootHash, timestampToken1Base64,
                        timestampToken2Base64,
                        timestampToken3Base64);

                    String previousDate = null;
                    String previousMonthDate = null;
                    String previousYearDate = null;
                    if (lastTraceabilityOperation != null) {
                        TraceabilityEvent lastTraceabilityEvent = extractEventDetData(lastTraceabilityOperation);
                        if (lastTraceabilityEvent != null) {
                            previousDate = lastTraceabilityEvent.getStartDate();
                        }
                    }

                    final LogbookOperation oneMounthBeforeTraceabilityOperation =
                        findFirstTraceabilityOperationOKAfterDate(logbookOperationsClient, currentDate.minusMonths(1));
                    if (oneMounthBeforeTraceabilityOperation != null) {
                        TraceabilityEvent oneMonthBeforeTraceabilityEvent =
                            extractEventDetData(oneMounthBeforeTraceabilityOperation);
                        if (oneMonthBeforeTraceabilityEvent != null) {
                            previousMonthDate = oneMonthBeforeTraceabilityEvent.getStartDate();
                        }
                    }

                    final LogbookOperation oneYearBeforeTraceabilityOperation =
                        findFirstTraceabilityOperationOKAfterDate(logbookOperationsClient, currentDate.minusYears(1));
                    if (oneYearBeforeTraceabilityOperation != null) {
                        TraceabilityEvent oneYearBeforeTraceabilityEvent =
                            extractEventDetData(oneYearBeforeTraceabilityOperation);
                        if (oneYearBeforeTraceabilityEvent != null) {
                            previousYearDate = oneYearBeforeTraceabilityEvent.getStartDate();
                        }
                    }

                    long size = zipFile.length();

                    traceabilityEvent = new TraceabilityEvent(TraceabilityType.LIFECYCLE, startDate, endDate,
                        rootHash, timeStampToken, previousDate, previousMonthDate, previousYearDate, numberOfLifecycles,
                        fileName, size, VitamConfiguration.getDefaultDigestType());

                    itemStatus.setEvDetailData(JsonHandler.unprettyPrint(traceabilityEvent));
                    itemStatus.setMasterData(LogbookParameterName.eventDetailData.name(),
                        JsonHandler.unprettyPrint(traceabilityEvent));
                } catch (LogbookNotFoundException | LogbookDatabaseException | LogbookClientException |
                    InvalidCreateOperationException | InvalidGuidOperationException e) {
                    zipFile.delete();
                    LOGGER.error("error with logbook ", e);
                    throw new LogbookException(e);
                }
            } else {
                // do nothing, nothing to be handled
                LOGGER.warn("No lifecycle to be processed");
                return;
            }

        } catch (IOException | ArchiveException e) {
            zipFile.delete();
            LOGGER.error("unable to generate zip file", e);
            throw new TraceabilityException(e);
        } catch (ProcessingException e) {
            zipFile.delete();
            LOGGER.error("processing exception", e);
            throw e;
        } catch (ContentAddressableStorageNotFoundException | ContentAddressableStorageServerException e) {
            zipFile.delete();
            LOGGER.error("Workspace exception", e);
            throw new ProcessingException(e);
        }

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
                    DEFAULT_STRATEGY, StorageCollectionType.LOGBOOKS, fileName, description);
                workspaceClient.deleteContainer(fileName, true);
                subItemStatusSecurisationStorage.setEvDetailData(JsonHandler.unprettyPrint(traceabilityEvent));
                itemStatus.setItemsStatus(HANDLER_SUB_ACTION_SECURISATION_STORAGE,
                    subItemStatusSecurisationStorage.increment(StatusCode.OK));
            } catch (StorageAlreadyExistsClientException | StorageNotFoundClientException |
                StorageServerClientException | ContentAddressableStorageNotFoundException e) {
                itemStatus.setItemsStatus(HANDLER_SUB_ACTION_SECURISATION_STORAGE,
                    subItemStatusSecurisationStorage.increment(StatusCode.FATAL));
                LOGGER.error("unable to store zip file", e);
                throw new TraceabilityException(e);
            }
        } catch (ContentAddressableStorageAlreadyExistException | ContentAddressableStorageServerException |
            IOException e) {
            LOGGER.error("unable to create container", e);
            itemStatus.setItemsStatus(HANDLER_SUB_ACTION_SECURISATION_STORAGE,
                subItemStatusSecurisationStorage.increment(StatusCode.FATAL));
            throw new TraceabilityException(e);
        } finally {
            zipFile.delete();
        }

    }

    /**
     * Reduce part of the process : lets merge in one only file the disting lifecycles ones
     * 
     * @param listOfFiles
     * @param traceabilityFile
     * @param rootHash
     * @param rootFolder
     * @return
     * @throws ContentAddressableStorageNotFoundException
     * @throws ContentAddressableStorageServerException
     * @throws IOException
     */
    private String extractAppendToFinalFile(List<URI> listOfFiles, TraceabilityFile traceabilityFile, String rootHash,
        String rootFolder)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException, IOException {
        for (final URI uriWorkspace : listOfFiles) {
            final InputStream lifecycleIn =
                handlerIO.getInputStreamFromWorkspace(rootFolder + "/" + uriWorkspace.getPath());
            if (rootHash == null) {
                // no choice but to duplicate in order to get the root hash (executed only once)
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                org.apache.commons.io.IOUtils.copy(lifecycleIn, baos);
                byte[] bytes = baos.toByteArray();
                traceabilityFile.storeLifecycleLog(new ByteArrayInputStream(bytes));
                rootHash = BaseXx.getBase64(bytes);
            } else {
                traceabilityFile.storeLifecycleLog(lifecycleIn);
            }

        }
        return rootHash;
    }

    /**
     * Extract the timestamp from a logbook operation
     * 
     * @param logbookOperation
     * @return
     * @throws InvalidParseOperationException
     */
    private String extractTimestampToken(LogbookOperation logbookOperation) throws InvalidParseOperationException {
        TraceabilityEvent traceabilityEvent = extractEventDetData(logbookOperation);
        if (traceabilityEvent == null) {
            return null;
        }
        return new String(traceabilityEvent.getTimeStampToken());
    }

    /**
     * Extract the eventDetData from a logbook operation
     * 
     * @param logbookOperation
     * @return
     * @throws InvalidParseOperationException
     */
    private TraceabilityEvent extractEventDetData(LogbookOperation logbookOperation)
        throws InvalidParseOperationException {
        if (logbookOperation == null) {
            return null;
        }
        return JsonHandler.getFromString((String) logbookOperation.get(EVENT_DETAIL_DATA), TraceabilityEvent.class);
    }

    /**
     * Find a hash in a logbook operation
     * 
     * @param logbookOperationsClient
     * @param expectIds
     * @param date
     * @return
     * @throws InvalidCreateOperationException
     * @throws InvalidParseOperationException
     * @throws LogbookClientException
     */
    private String findHashByTraceabilityEventExpect(LogbookOperationsClient logbookOperationsClient,
        List<String> expectIds, LocalDateTime date)
        throws InvalidCreateOperationException, InvalidParseOperationException, LogbookClientException {
        try {
            RequestResponseOK requestResponseOK =
                RequestResponseOK.getFromJsonNode(
                    logbookOperationsClient.selectOperation(generateSelectLogbookOperation(date).getFinalSelect()));
            List<ObjectNode> foundOperation = requestResponseOK.getResults();
            if (foundOperation != null && foundOperation.size() >= 1) {
                LogbookOperation lastOperationTraceabilityLifecycle = new LogbookOperation(foundOperation.get(0));
                if (!expectIds.contains(lastOperationTraceabilityLifecycle.getString(EVENT_ID))) {
                    expectIds.add(lastOperationTraceabilityLifecycle.getString(EVENT_ID));
                    return extractTimestampToken(lastOperationTraceabilityLifecycle);
                }
            } else {
                return null;
            }
        } catch (LogbookClientNotFoundException e) {
            LOGGER.warn("Logbook operation not found, there is no Operation");
        }
        return null;
    }

    /**
     * Find the latest traceability after a specific date
     * 
     * @param logbookOperationsClient
     * @param date
     * @return the logbook operation
     * @throws InvalidCreateOperationException
     * @throws LogbookNotFoundException
     * @throws LogbookDatabaseException
     * @throws InvalidParseOperationException
     * @throws LogbookClientException
     */
    private LogbookOperation findFirstTraceabilityOperationOKAfterDate(LogbookOperationsClient logbookOperationsClient,
        LocalDateTime date)
        throws InvalidCreateOperationException, LogbookNotFoundException, LogbookDatabaseException,
        InvalidParseOperationException, LogbookClientException {
        try {
            RequestResponseOK requestResponseOK =
                RequestResponseOK.getFromJsonNode(
                    logbookOperationsClient.selectOperation(generateSelectLogbookOperation(date).getFinalSelect()));
            List<ObjectNode> foundOperation = requestResponseOK.getResults();
            if (foundOperation != null && foundOperation.size() == 1) {
                return new LogbookOperation(foundOperation.get(0));
            }
        } catch (LogbookClientNotFoundException e) {
            LOGGER.warn("Logbook operation not found, there is no Operation");
        }
        return null;
    }

    /**
     * Generate a select query with a dateTime
     * 
     * @param date
     * @return a query
     * @throws InvalidCreateOperationException
     */
    private Select generateSelectLogbookOperation(LocalDateTime date) throws InvalidCreateOperationException {
        final Select select = new Select();
        final Query query = QueryHelper.gt("evDateTime", date.toString());
        final Query type = QueryHelper.eq("evTypeProc", LogbookTypeProcess.TRACEABILITY.name());
        final Query findEvent = QueryHelper
            .eq(String.format("%s.%s", LogbookDocument.EVENTS, LogbookMongoDbName.outcomeDetail.getDbname()),
                "LOGBOOK_LC_SECURISATION.OK");
        select.setQuery(QueryHelper.and().add(query, type, findEvent));
        select.setLimitFilter(0, 1);
        return select;
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // Nothing to check
    }

    @VisibleForTesting
    byte[] generateTimeStampToken(GUID eip, Integer tenantId, String rootHash, String hash1, String hash2, String hash3,
        ItemStatus itemStatus)
        throws IOException, TraceabilityException {
        final ItemStatus subItemStatusTimestamp = new ItemStatus(HANDLER_SUB_ACTION_TIMESTAMP);
        try {
            final String hash = joiner.join(rootHash, hash1, hash2, hash3);
            final DigestType digestType = VitamConfiguration.getDefaultTimestampDigestType();
            final Digest digest = new Digest(digestType);
            digest.update(hash);
            final byte[] hashDigest = digest.digest();
            // TODO maybe nonce could be different than null ? If so, think about changing VerifyTimeStampActionHandler
            final byte[] timeStampToken = timestampGenerator.generateToken(hashDigest, digestType, null);
            itemStatus.setItemsStatus(HANDLER_SUB_ACTION_TIMESTAMP, subItemStatusTimestamp.increment(StatusCode.OK));
            return timeStampToken;
        } catch (final TimeStampException e) {
            LOGGER.error("unable to generate timestamp", e);
            itemStatus.setItemsStatus(HANDLER_SUB_ACTION_TIMESTAMP, subItemStatusTimestamp.increment(StatusCode.KO));
            throw new TraceabilityException(e);
        }
    }

    /**
     * @return HANDLER_ID
     */
    public static final String getId() {
        return HANDLER_ID;
    }
}
