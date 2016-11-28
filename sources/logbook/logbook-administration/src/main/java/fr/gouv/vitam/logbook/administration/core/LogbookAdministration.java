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
import static fr.gouv.vitam.common.LocalDateUtil.getString;
import static fr.gouv.vitam.common.LocalDateUtil.now;
import static fr.gouv.vitam.common.json.JsonHandler.unprettyPrint;
import static fr.gouv.vitam.common.model.StatusCode.FATAL;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.common.model.StatusCode.STARTED;
import static fr.gouv.vitam.logbook.common.parameters.LogbookParameterName.eventIdentifier;
import static fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory.newLogbookOperationParameters;
import static fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess.TRACEABILITY;
import static fr.gouv.vitam.logbook.common.server.database.collections.LogbookDocument.EVENTS;
import static fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycleMongoDbName.eventDateTime;
import static fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbName.eventDetailData;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

import org.apache.commons.compress.archivers.ArchiveException;
import org.bson.Document;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.mongodb.client.MongoCursor;

import fr.gouv.vitam.common.BaseXx;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.TimeStampException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.security.merkletree.MerkleTree;
import fr.gouv.vitam.common.security.merkletree.MerkleTreeAlgo;
import fr.gouv.vitam.common.timestamp.TimestampGenerator;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationsClientHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookOperation;
import fr.gouv.vitam.logbook.common.server.exception.LogbookAlreadyExistsException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookDatabaseException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookNotFoundException;
import fr.gouv.vitam.logbook.operations.api.LogbookOperations;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.StorageCollectionType;
import fr.gouv.vitam.storage.engine.client.exception.StorageAlreadyExistsClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.model.request.CreateObjectDescription;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

/**
 * Business class for Logbook Administration (traceability)
 */
public class LogbookAdministration {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogbookAdministration.class);

    private static final String STP_SECURISATION = "LOGBOOK_OP_SECURISATION";
    private static final String TIMESTAMP = "OP_SECURISATION_TIMESTAMP";
    private static final String OP_SECURISATION_STORAGE = "OP_SECURISATION_STORAGE";
    private static final String STP_OP_SECURISATION = "STP_OP_SECURISATION";

    private static final String EVENT_DATE_TIME = eventDateTime.getDbname();

    private static final String STRATEGY_ID = "default";
    private static final int TENANT_ID = 0;

    private final LogbookOperations logbookOperations;
    private final TimestampGenerator timestampGenerator;
    private final WorkspaceClientFactory workspaceClientFactory;
    private final DateTimeFormatter formatter;

    private final File tmpFolder;

    private final Joiner joiner;

    @VisibleForTesting //
    LogbookAdministration(LogbookOperations logbookOperations,
        TimestampGenerator timestampGenerator, WorkspaceClientFactory workspaceClientFactory, File tmpFolder) {
        this.logbookOperations = logbookOperations;
        this.timestampGenerator = timestampGenerator;
        this.workspaceClientFactory = workspaceClientFactory;
        this.tmpFolder = tmpFolder;
        formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

        joiner = Joiner.on("").skipNulls();

        tmpFolder.mkdir();
    }

    /**
     * @param logbookOperations
     * @param timestampGenerator
     * @param workspaceClientFactory
     */
    public LogbookAdministration(LogbookOperations logbookOperations, TimestampGenerator timestampGenerator,
        WorkspaceClientFactory workspaceClientFactory) {
        this(logbookOperations, timestampGenerator, workspaceClientFactory,
            PropertiesUtils.fileFromTmpFolder("secure"));
    }

    /**
     * secure the logbook operation since last securisation.
     *
     * @return the GUID of the operation
     * @throws TraceabilityException
     * @throws LogbookNotFoundException
     * @throws InvalidParseOperationException
     * @throws LogbookDatabaseException
     * @throws InvalidCreateOperationException
     */
    // TODO: use a distributed lock to launch this function only on one server (cf consul)
    public synchronized GUID generateSecureLogbook()
        throws TraceabilityException, LogbookNotFoundException, InvalidParseOperationException,
        LogbookDatabaseException, InvalidCreateOperationException {

        final LogbookOperation lastTraceabilityOperation = logbookOperations.findLastTraceabilityOperationOK();

        final GUID eip = GUIDFactory.newOperationLogbookGUID(TENANT_ID);

        final List<String> expectedLogbookId = newArrayList(eip.getId());
        LocalDateTime startDate;

        if (lastTraceabilityOperation == null) {
            startDate = LocalDateTime.MIN;
        } else {
            final Date date = LocalDateUtil.getDate(lastTraceabilityOperation.getString(EVENT_DATE_TIME));
            startDate = LocalDateUtil.fromDate(date);
            expectedLogbookId.add(lastTraceabilityOperation.getString(eventIdentifier));
        }

        final LocalDateTime currentDate = now();

        final String fileName = String.format("%d_LogbookOperation_%s.zip", TENANT_ID, currentDate.format(formatter));
        createLogbookOperationStructure(eip);

        final File zipFile = new File(tmpFolder, fileName);
        final String uri = String.format("%s/%s", "logbook", fileName);
        TraceabilityEvent traceabilityEvent;

        try (TraceabilityFile traceabilityFile = new TraceabilityFile(zipFile)) {

            final MongoCursor<LogbookOperation> mongoCursor = logbookOperations.selectAfterDate(startDate);
            final TraceabilityIterator traceabilityIterator = new TraceabilityIterator(mongoCursor);

            final MerkleTreeAlgo merkleTreeAlgo = new MerkleTreeAlgo(VitamConfiguration.getDefaultDigestType());

            traceabilityFile.initStoreOperationLog();

            while (traceabilityIterator.hasNext()) {

                final LogbookOperation logbookOperation = traceabilityIterator.next();
                final String logbookOperationStr = JsonHandler.unprettyPrint(logbookOperation);
                traceabilityFile.storeOperationLog(logbookOperation);
                merkleTreeAlgo.addSheet(logbookOperationStr);
            }

            traceabilityFile.closeStoreOperationLog();

            final MerkleTree merkleTree = merkleTreeAlgo.generateMerkle();
            traceabilityFile.storeMerkleTree(merkleTree);

            final String rootHash = BaseXx.getBase64(merkleTree.getRoot());

            final String hash1 = extractHash(lastTraceabilityOperation);
            final String hash2 = findHashByTraceabilityEventExpect(expectedLogbookId, currentDate.minusMonths(1));
            final String hash3 = findHashByTraceabilityEventExpect(expectedLogbookId, currentDate.minusYears(1));

            final byte[] timeStampToken = generateTimeStampToken(eip, rootHash, hash1, hash2, hash3);
            traceabilityFile.storeTimeStampToken(timeStampToken);

            final long numberOfLine = traceabilityIterator.getNumberOfLine();
            final String endDate = traceabilityIterator.endDate();

            traceabilityFile.storeAdditionalInformation(numberOfLine, getString(startDate), endDate);
            traceabilityFile.storeHashCalculationInformation(rootHash, hash1, hash2, hash3);

            traceabilityEvent = new TraceabilityEvent(getString(startDate), endDate, rootHash, numberOfLine, uri);

        } catch (LogbookDatabaseException | LogbookNotFoundException | IOException | InvalidCreateOperationException |
            ArchiveException | InvalidParseOperationException e) {
            createLogbookOperationEvent(eip, STP_OP_SECURISATION, FATAL, null);

            zipFile.delete();
            throw new TraceabilityException(e);
        }

        try (InputStream inputStream = new BufferedInputStream(new FileInputStream(zipFile));
            WorkspaceClient workspaceClient = workspaceClientFactory.getClient()) {

            createLogbookOperationEvent(eip, OP_SECURISATION_STORAGE, STARTED, null);
            workspaceClient.createContainer(fileName);

            workspaceClient.putObject(fileName, uri, inputStream);

            final StorageClientFactory storageClientFactory = StorageClientFactory.getInstance();

            final CreateObjectDescription description = new CreateObjectDescription();
            description.setWorkspaceContainerGUID(fileName);
            description.setWorkspaceObjectURI(uri);

            try (final StorageClient storageClient = storageClientFactory.getClient()) {

                storageClient.storeFileFromWorkspace(Integer.toString(TENANT_ID),
                    STRATEGY_ID, StorageCollectionType.LOGBOOKS, fileName, description);
                workspaceClient.deleteObject(fileName, uri);

                createLogbookOperationEvent(eip, OP_SECURISATION_STORAGE, OK, null);

            } catch (StorageAlreadyExistsClientException | StorageNotFoundClientException |
                StorageServerClientException | ContentAddressableStorageNotFoundException e) {
                createLogbookOperationEvent(eip, OP_SECURISATION_STORAGE, StatusCode.FATAL, null);
                LOGGER.error("unable to store zip file", e);
                throw new TraceabilityException(e);
            }
        } catch (ContentAddressableStorageAlreadyExistException | ContentAddressableStorageServerException |
            IOException e) {
            LOGGER.error("unable to create container", e);
            createLogbookOperationEvent(eip, OP_SECURISATION_STORAGE, StatusCode.FATAL, null);
            throw new TraceabilityException(e);
        } finally {
            zipFile.delete();
        }
        createLogbookOperationEvent(eip, STP_OP_SECURISATION, OK, traceabilityEvent);
        return eip;
    }

    @VisibleForTesting
    String findHashByTraceabilityEventExpect(List<String> expectIds, LocalDateTime date)
        throws InvalidCreateOperationException, LogbookNotFoundException, LogbookDatabaseException,
        InvalidParseOperationException {

        final LogbookOperation logbookOperation = logbookOperations.findFirstTraceabilityOperationOKAfterDate(date);

        if (logbookOperation == null || expectIds.contains(logbookOperation.getString(eventIdentifier))) {
            return null;
        }
        expectIds.add(logbookOperation.getString(eventIdentifier));
        return extractHash(logbookOperation);
    }

    private String extractHash(LogbookOperation logbookOperation) throws InvalidParseOperationException {
        if (logbookOperation == null) {
            return null;
        }

        final List<Document> events = (List<Document>) logbookOperation.get(EVENTS);
        final Document lastEvent = Iterables.getLast(events);

        final String evDetData = (String) lastEvent.get(eventDetailData.getDbname());

        final TraceabilityEvent traceabilityEvent = JsonHandler.getFromString(evDetData, TraceabilityEvent.class);
        return traceabilityEvent.getHash();
    }

    @VisibleForTesting
    byte[] generateTimeStampToken(GUID eip, String rootHash, String hash1, String hash2, String hash3)
        throws IOException, TraceabilityException {

        try {
            final String hash = joiner.join(rootHash, hash1, hash2, hash3);
            createLogbookOperationEvent(eip, TIMESTAMP, STARTED, null);

            final DigestType digestType = VitamConfiguration.getDefaultTimestampDigestType();

            final Digest digest = new Digest(digestType);
            digest.update(hash);
            final byte[] hashDigest = digest.digest();

            final byte[] timeStampToken = timestampGenerator.generateToken(hashDigest, digestType, null);

            createLogbookOperationEvent(eip, TIMESTAMP, OK, null);
            return timeStampToken;
        } catch (final TimeStampException e) {
            LOGGER.error("unable to generate timestamp", e);
            createLogbookOperationEvent(eip, TIMESTAMP, StatusCode.FATAL, null);
            throw new TraceabilityException(e);
        }
    }

    private void createLogbookOperationStructure(GUID eip) throws TraceabilityException {
        try {
            final LogbookOperationParameters logbookParameters =
                newLogbookOperationParameters(eip, STP_SECURISATION, eip, TRACEABILITY, STARTED, null, null, eip);

            LogbookOperationsClientHelper.checkLogbookParameters(logbookParameters);
            logbookOperations.create(logbookParameters);
            createLogbookOperationEvent(eip, STP_OP_SECURISATION, STARTED, null);
        } catch (LogbookAlreadyExistsException | LogbookDatabaseException e) {
            LOGGER.error("unable to create traceability logbook", e);
            throw new TraceabilityException(e);
        }
    }

    private void createLogbookOperationEvent(GUID parentEventId, String eventType, StatusCode statusCode,
        TraceabilityEvent traceabilityEvent) throws TraceabilityException {
        final GUID eventId = GUIDFactory.newEventGUID(TENANT_ID);
        final LogbookOperationParameters logbookOperationParameters =
            newLogbookOperationParameters(eventId, eventType, parentEventId, TRACEABILITY, statusCode, null, null,
                parentEventId);

        LogbookOperationsClientHelper.checkLogbookParameters(logbookOperationParameters);

        if (traceabilityEvent != null) {
            logbookOperationParameters
                .putParameterValue(LogbookParameterName.eventDetailData, unprettyPrint(traceabilityEvent));
        }
        try {
            logbookOperations.update(logbookOperationParameters);
        } catch (LogbookNotFoundException | LogbookDatabaseException e) {
            LOGGER.error("unable to update traceability logbook", e);
            throw new TraceabilityException(e);
        }
    }

}
