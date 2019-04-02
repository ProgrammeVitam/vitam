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
package fr.gouv.vitam.storage.offers.tape.cas;

import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.iterables.BulkIterator;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.storage.tapelibrary.TapeLibraryConfiguration;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryBuildingOnDiskTarStorageLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryInputFileObjectStorageLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryTarObjectStorageLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeObjectReferentialEntity;
import fr.gouv.vitam.storage.engine.common.model.TapeTarReferentialEntity;
import fr.gouv.vitam.storage.engine.common.model.TarEntryDescription;
import fr.gouv.vitam.storage.engine.common.model.WriteOrder;
import fr.gouv.vitam.storage.offers.tape.exception.ObjectReferentialException;
import fr.gouv.vitam.storage.offers.tape.exception.TarReferentialException;
import fr.gouv.vitam.storage.offers.tape.inmemoryqueue.QueueProcessingException;
import fr.gouv.vitam.storage.offers.tape.inmemoryqueue.QueueProcessor;
import fr.gouv.vitam.storage.offers.tape.utils.LocalFileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BoundedInputStream;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

public class FileBucketTarCreator extends QueueProcessor<TarCreatorMessage> {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(FileBucketTarCreator.class);

    private final TapeLibraryConfiguration tapeLibraryConfiguration;
    private final BasicFileStorage basicFileStorage;
    private final ObjectReferentialRepository objectReferentialRepository;
    private final TarReferentialRepository tarReferentialRepository;
    private final WriteOrderCreator writeOrderCreator;
    private final Set<String> containerNames;
    private final String bucketId;
    private final String fileBucketId;
    private final Path fileBucketStoragePath;
    private final int tarBufferingTimeoutInMinutes;
    private final ScheduledExecutorService scheduledExecutorService;

    private TarAppender currentTarAppender = null;
    private Path currentTempTarFilePath = null;
    private Path currentTarFilePath = null;
    private ScheduledFuture<?> tarBufferingTimoutChecker;

    public FileBucketTarCreator(TapeLibraryConfiguration tapeLibraryConfiguration,
        BasicFileStorage basicFileStorage,
        ObjectReferentialRepository objectReferentialRepository,
        TarReferentialRepository tarReferentialRepository,
        WriteOrderCreator writeOrderCreator, Set<String> containerNames, String bucketId,
        String fileBucketId, int tarBufferingTimeoutInMinutes) {
        super("FileBucketTarCreator-" + fileBucketId);

        this.tapeLibraryConfiguration = tapeLibraryConfiguration;
        this.basicFileStorage = basicFileStorage;
        this.objectReferentialRepository = objectReferentialRepository;
        this.tarReferentialRepository = tarReferentialRepository;
        this.writeOrderCreator = writeOrderCreator;
        this.containerNames = containerNames;

        this.bucketId = bucketId;
        this.fileBucketId = fileBucketId;
        this.tarBufferingTimeoutInMinutes = tarBufferingTimeoutInMinutes;
        this.fileBucketStoragePath = Paths.get(tapeLibraryConfiguration.getInputTarStorageFolder())
            .resolve(fileBucketId);
        this.scheduledExecutorService = Executors.newScheduledThreadPool(1);

    }

    @Override
    protected void processMessage(TarCreatorMessage message)
        throws QueueProcessingException {

        if (message instanceof TarBufferingTimedOutMessage) {
            checkTarBufferingTimeout((TarBufferingTimedOutMessage) message);

        } else if (message instanceof InputFileToProcessMessage) {
            writeFile((InputFileToProcessMessage) message);

        } else {
            throw new IllegalStateException("Unknown message time type " + message.getClass());
        }
    }

    private void writeFile(InputFileToProcessMessage message)
        throws QueueProcessingException {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Processing message " + JsonHandler.unprettyPrint(message));
        }

        if (currentTarAppender == null) {
            createTarFile();
        }

        Optional<InputStream> inputStream = Optional.empty();
        try {
            inputStream = openInputFile(message);

            if (!inputStream.isPresent()) {
                // File deleted meanwhile. Skip quietly...
                return;
            }

            Digest digest = new Digest(DigestType.fromValue(message.getDigestAlgorithm()));
            InputStream digestInputStream = digest.getDigestInputStream(inputStream.get());
            long remainingSize = message.getSize();
            List<TarEntryDescription> tarEntryDescriptions = new ArrayList<>();

            int entryIndex = 0;
            do {

                long entrySize = Math.min(remainingSize, this.tapeLibraryConfiguration.getMaxTarEntrySize());

                if (!currentTarAppender.canAppend(entrySize)) {
                    finalizeTarFile();
                    createTarFile();
                }

                String entryName = LocalFileUtils.createTarEntryName(
                    message.getContainerName(), message.getStorageId(), entryIndex);
                BoundedInputStream entryInputStream = new BoundedInputStream(digestInputStream, entrySize);

                TarEntryDescription tarEntryDescription =
                    currentTarAppender.append(entryName, entryInputStream, entrySize);
                tarEntryDescriptions.add(tarEntryDescription);

                remainingSize -= entrySize;
                entryIndex++;
            }
            while (remainingSize > 0L);
            currentTarAppender.fsync();

            if (!digest.digestHex().equals(message.getDigestValue())) {
                throw new QueueProcessingException(
                    QueueProcessingException.RetryPolicy.FATAL_SHUTDOWN,
                    "Invalid file digest. request=" + JsonHandler.unprettyPrint(message) +
                        ". Actual digest=" +
                        digest.digestHex() + ".");
            }

            indexInObjectReferential(message, tarEntryDescriptions);

            IOUtils.closeQuietly(inputStream.get());
            basicFileStorage.deleteFile(message.getContainerName(), message.getStorageId());

        } catch (IOException | RuntimeException ex) {

            if (currentTarAppender != null) {
                this.currentTarAppender.closeQuitely();
            }

            throw new QueueProcessingException(QueueProcessingException.RetryPolicy.FATAL_SHUTDOWN,
                "An error occurred while archiving file to tar", ex);

        } finally {
            inputStream.ifPresent(IOUtils::closeQuietly);
        }
    }

    private Optional<InputStream> openInputFile(InputFileToProcessMessage message) throws IOException {
        try {
            return Optional.of(
                basicFileStorage.readFile(message.getContainerName(), message.getStorageId())
            );
        } catch (FileNotFoundException ex) {
            LOGGER.debug(ex);
            return Optional.empty();
        }
    }

    private void createTarFile() throws QueueProcessingException {

        LocalDateTime now = LocalDateUtil.now();
        String tarFileId = LocalFileUtils.createTarId(now);

        try {
            TapeTarReferentialEntity tarReferentialEntity = new TapeTarReferentialEntity(
                tarFileId, new TapeLibraryBuildingOnDiskTarStorageLocation(), null, null, now.toString());
            tarReferentialRepository.insert(tarReferentialEntity);
        } catch (TarReferentialException ex) {
            throw new QueueProcessingException(QueueProcessingException.RetryPolicy.RETRY,
                "Could not create a new tar file", ex);
        }

        this.currentTarFilePath = fileBucketStoragePath.resolve(tarFileId);
        this.currentTempTarFilePath = fileBucketStoragePath.resolve(tarFileId + LocalFileUtils.TMP_EXTENSION);
        this.currentTarAppender = new TarAppender(
            currentTempTarFilePath, tarFileId, tapeLibraryConfiguration.getMaxTarFileSize());
        this.tarBufferingTimoutChecker = this.scheduledExecutorService.schedule(
            () -> checkTarBufferingTimeout(tarFileId), tarBufferingTimeoutInMinutes, TimeUnit.MINUTES);
    }

    private void finalizeTarFile() throws IOException {

        this.currentTarAppender.close();

        // Mark file as done (remove .tmp extension)
        Files.move(this.currentTempTarFilePath, this.currentTarFilePath, StandardCopyOption.ATOMIC_MOVE);

        // Schedule tar for copy on tape
        WriteOrder writeOrder = new WriteOrder(
            this.bucketId,
            LocalFileUtils
                .tarFileNameRelativeToInputTarStorageFolder(this.fileBucketId, this.currentTarAppender.getTarId()),
            this.currentTarAppender.getBytesWritten(),
            this.currentTarAppender.getDigestValue(),
            this.currentTarAppender.getTarId());
        this.writeOrderCreator.addToQueue(writeOrder);

        this.currentTarAppender = null;
        this.tarBufferingTimoutChecker.cancel(false);
    }

    private void indexInObjectReferential(InputFileToProcessMessage inputFileToProcessMessage,
        List<TarEntryDescription> tarEntryDescriptions) throws QueueProcessingException {

        try {
            objectReferentialRepository.updateStorageLocation(
                inputFileToProcessMessage.getContainerName(),
                inputFileToProcessMessage.getObjectName(),
                inputFileToProcessMessage.getStorageId(),
                new TapeLibraryTarObjectStorageLocation(tarEntryDescriptions)
            );
        } catch (ObjectReferentialException ex) {
            throw new QueueProcessingException(
                QueueProcessingException.RetryPolicy.RETRY,
                "Could not index object referential " + inputFileToProcessMessage.getContainerName() + "/" +
                    inputFileToProcessMessage.getObjectName() + " (" +
                    inputFileToProcessMessage.getStorageId() + ") to tar files " +
                    JsonHandler.unprettyPrint(tarEntryDescriptions), ex);
        }
    }

    @Override
    public void initializeOnBootstrap() {

        try {
            // Ensure directory exists
            Files.createDirectories(fileBucketStoragePath);

            // List existing files
            for (String containerName : containerNames) {
                try (Stream<String> storageIdsStream = this.basicFileStorage
                    .listStorageIdsByContainerName(containerName)) {
                    BulkIterator<String> bulkIterator = new BulkIterator<>(
                        storageIdsStream.iterator(), VitamConfiguration.getBatchSize());

                    while (bulkIterator.hasNext()) {

                        List<String> storageIds = bulkIterator.next();
                        try {
                            processStorageIds(containerName, storageIds);
                        } catch (ObjectReferentialException e) {
                            throw new VitamRuntimeException("Could not initialize service to container " + containerName
                                + " files " + storageIds, e);
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new VitamRuntimeException(
                "Could not initialize file bucket tar creator service " + fileBucketStoragePath, e);
        }
    }

    private void processStorageIds(String containerName, List<String> storageIds) throws ObjectReferentialException {

        // Map storage ids to object names
        Map<String, String> storageIdToObjectIdMap = storageIds.stream()
            .collect(toMap(
                storageId -> storageId,
                LocalFileUtils::storageIdToObjectName
            ));
        HashSet<String> objectNames = new HashSet<>(storageIdToObjectIdMap.values());

        // Find objects in object referential (bulk)
        List<TapeObjectReferentialEntity> objectReferentialEntities =
            this.objectReferentialRepository.bulkFind(containerName,
                objectNames);

        Map<String, TapeObjectReferentialEntity> objectReferentialEntityByObjectIdMap =
            objectReferentialEntities.stream()
                .collect(toMap(entity -> entity.getId().getObjectName(), entity -> entity));

        // Process storage ids
        for (String storageId : storageIds) {
            String objectName = storageIdToObjectIdMap.get(storageId);

            if (!objectReferentialEntityByObjectIdMap.containsKey(objectName)) {
                // Not found in DB -> Log & delete file
                LOGGER.warn("Incomplete file " + storageId + ". Will be deleted");
                this.basicFileStorage.deleteFile(containerName, storageId);
            } else {

                TapeObjectReferentialEntity objectReferentialEntity =
                    objectReferentialEntityByObjectIdMap.get(objectName);

                if (!storageId.equals(objectReferentialEntity.getStorageId())) {
                    // Not found in DB -> Log & delete file
                    LOGGER.warn("Incomplete or obsolete file " + storageId + ". Will be deleted");
                    this.basicFileStorage.deleteFile(containerName, storageId);
                } else if (objectReferentialEntity.getLocation()
                    instanceof TapeLibraryInputFileObjectStorageLocation) {
                    // Found & in file  =>  Add to queue
                    LOGGER.warn("Input file to be scheduled for archival "
                        + containerName + "/" + storageId);
                    this.addToQueue(new InputFileToProcessMessage(containerName, objectName, storageId,
                        objectReferentialEntity.getSize(), objectReferentialEntity.getDigest(),
                        objectReferentialEntity.getDigestType()));
                } else {
                    // Input file already archived to TAR => Delete it
                    LOGGER.debug("Input file already archived "
                        + containerName + "/" + storageId);
                    this.basicFileStorage.deleteFile(containerName, storageId);
                }
            }
        }
    }

    private void checkTarBufferingTimeout(TarBufferingTimedOutMessage tarBufferingTimedOutMessage)
        throws QueueProcessingException {

        // Double check tar Id in case of concurrent access
        if (this.currentTarAppender == null ||
            !this.currentTarAppender.getTarId().equals(tarBufferingTimedOutMessage.getTarId())) {
            return;
        }

        try {
            finalizeTarFile();
        } catch (IOException ex) {
            throw new QueueProcessingException(QueueProcessingException.RetryPolicy.FATAL_SHUTDOWN,
                "An error occurred while archiving file to tar", ex);
        }
    }

    private void checkTarBufferingTimeout(String tarId) {
        addFirst(new TarBufferingTimedOutMessage(tarId));
    }
}
