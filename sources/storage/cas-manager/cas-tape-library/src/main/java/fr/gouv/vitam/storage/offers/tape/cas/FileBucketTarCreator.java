/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "https://cecill.info".
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
 */
package fr.gouv.vitam.storage.offers.tape.cas;

import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.stream.ExtendedFileOutputStream;
import fr.gouv.vitam.storage.engine.common.model.QueueMessageType;
import fr.gouv.vitam.storage.engine.common.model.TapeArchiveReferentialEntity;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryBuildingOnDiskArchiveStorageLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryTarObjectStorageLocation;
import fr.gouv.vitam.storage.engine.common.model.TarEntryDescription;
import fr.gouv.vitam.storage.engine.common.model.WriteOrder;
import fr.gouv.vitam.storage.offers.tape.exception.ArchiveReferentialException;
import fr.gouv.vitam.storage.offers.tape.exception.ObjectReferentialException;
import fr.gouv.vitam.storage.offers.tape.inmemoryqueue.QueueProcessingException;
import fr.gouv.vitam.storage.offers.tape.inmemoryqueue.QueueProcessor;
import fr.gouv.vitam.storage.offers.tape.metrics.InputFilesMetrics;
import fr.gouv.vitam.storage.offers.tape.utils.LocalFileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BoundedInputStream;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static fr.gouv.vitam.storage.offers.tape.utils.LocalFileUtils.TMP_EXTENSION;
import static fr.gouv.vitam.storage.offers.tape.utils.LocalFileUtils.fileBuckedInputFilePath;

public class FileBucketTarCreator extends QueueProcessor<TarCreatorMessage> {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(FileBucketTarCreator.class);

    private final BasicFileStorage basicFileStorage;
    private final ObjectReferentialRepository objectReferentialRepository;
    private final ArchiveReferentialRepository archiveReferentialRepository;
    private final WriteOrderCreator writeOrderCreator;
    private final String bucketId;
    private final String fileBucketId;
    private final Path fileBucketStoragePath;
    private final int tarBufferingTimeout;
    private final TimeUnit tarBufferingTimeUnit;
    private final ScheduledExecutorService scheduledExecutorService;

    private TarAppender currentTarAppender = null;
    private ExtendedFileOutputStream currentTarOutputStream = null;
    private Path currentTempTarFilePath = null;
    private Path currentTarFilePath = null;
    private ScheduledFuture<?> tarBufferingTimoutChecker;
    private final long maxTarEntrySize;
    private final long maxTarFileSize;

    public FileBucketTarCreator(
        BasicFileStorage basicFileStorage,
        ObjectReferentialRepository objectReferentialRepository,
        ArchiveReferentialRepository archiveReferentialRepository,
        WriteOrderCreator writeOrderCreator,
        String bucketId,
        String fileBucketId,
        int tarBufferingTimeout,
        TimeUnit tarBufferingTimeUnit,
        String inputTarStorageFolder,
        long maxTarEntrySize,
        long maxTarFileSize
    ) {
        super("FileBucketTarCreator-" + fileBucketId);
        this.maxTarEntrySize = maxTarEntrySize;

        this.basicFileStorage = basicFileStorage;
        this.objectReferentialRepository = objectReferentialRepository;
        this.archiveReferentialRepository = archiveReferentialRepository;
        this.writeOrderCreator = writeOrderCreator;

        this.bucketId = bucketId;
        this.fileBucketId = fileBucketId;
        this.tarBufferingTimeout = tarBufferingTimeout;
        this.tarBufferingTimeUnit = tarBufferingTimeUnit;
        this.maxTarFileSize = maxTarFileSize;
        this.fileBucketStoragePath = fileBuckedInputFilePath(inputTarStorageFolder, fileBucketId);
        this.scheduledExecutorService = Executors.newScheduledThreadPool(1);
    }

    @Override
    protected void processMessage(TarCreatorMessage message) throws QueueProcessingException {
        if (message instanceof TarBufferingTimedOutMessage) {
            checkTarBufferingTimeout((TarBufferingTimedOutMessage) message);
        } else if (message instanceof InputFileToProcessMessage) {
            writeFile((InputFileToProcessMessage) message);
        } else {
            throw new IllegalStateException("Unknown message time type " + message.getClass());
        }
    }

    private void writeFile(InputFileToProcessMessage message) throws QueueProcessingException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Processing message " + JsonHandler.unprettyPrint(message));
        }

        Optional<InputStream> inputStream = Optional.empty();
        try {
            inputStream = openInputFile(message);

            if (inputStream.isEmpty()) {
                // File deleted meanwhile. Skip quietly...
                LOGGER.info(
                    "File {} ({}/{}) not found. Deleted meanwhile?",
                    message.getStorageId(),
                    message.getContainerName(),
                    message.getObjectName()
                );
                return;
            }

            if (currentTarAppender == null) {
                createTarFile();
            }

            Digest digest = new Digest(DigestType.fromValue(message.getDigestAlgorithm()));
            InputStream digestInputStream = digest.getDigestInputStream(inputStream.get());
            long remainingSize = message.getSize();
            List<TarEntryDescription> tarEntryDescriptions = new ArrayList<>();

            int entryIndex = 0;
            do {
                long entrySize = Math.min(remainingSize, maxTarEntrySize);

                if (!currentTarAppender.canAppend(entrySize)) {
                    LOGGER.info("Finalizing full tar file {}", this.currentTempTarFilePath);
                    finalizeTarFile();
                    createTarFile();
                }

                String entryName = LocalFileUtils.createTarEntryName(
                    message.getContainerName(),
                    message.getStorageId(),
                    entryIndex
                );
                BoundedInputStream entryInputStream = new BoundedInputStream(digestInputStream, entrySize);

                TarEntryDescription tarEntryDescription = currentTarAppender.append(
                    entryName,
                    entryInputStream,
                    entrySize
                );
                tarEntryDescriptions.add(tarEntryDescription);

                remainingSize -= entrySize;
                entryIndex++;
            } while (remainingSize > 0L);
            this.currentTarAppender.flush();
            this.currentTarOutputStream.fsync();

            if (!digest.digestHex().equals(message.getDigestValue())) {
                throw new QueueProcessingException(
                    QueueProcessingException.RetryPolicy.FATAL_SHUTDOWN,
                    "Invalid file digest. request=" +
                    JsonHandler.unprettyPrint(message) +
                    ". Actual digest=" +
                    digest.digestHex() +
                    "."
                );
            }

            indexInObjectReferential(message, tarEntryDescriptions);

            IOUtils.closeQuietly(inputStream.get());
            basicFileStorage.deleteFile(message.getContainerName(), message.getStorageId());
        } catch (IOException | RuntimeException ex) {
            if (this.currentTarOutputStream != null) {
                IOUtils.closeQuietly(this.currentTarOutputStream);
            }

            throw new QueueProcessingException(
                QueueProcessingException.RetryPolicy.FATAL_SHUTDOWN,
                "An error occurred while archiving file to tar",
                ex
            );
        } finally {
            inputStream.ifPresent(IOUtils::closeQuietly);

            InputFilesMetrics.QUEUED_INPUT_FILES_COUNT.labels(bucketId).dec();

            InputFilesMetrics.QUEUED_INPUT_FILES_SIZE.labels(bucketId).dec(message.getSize());
        }
    }

    private Optional<InputStream> openInputFile(InputFileToProcessMessage message) throws IOException {
        try {
            return Optional.of(basicFileStorage.readFile(message.getContainerName(), message.getStorageId()));
        } catch (FileNotFoundException ex) {
            LOGGER.debug(ex);
            return Optional.empty();
        }
    }

    private void createTarFile() throws QueueProcessingException, IOException {
        LocalDateTime now = LocalDateUtil.now();
        String tarFileId = LocalFileUtils.createTarId(now);

        this.currentTarFilePath = fileBucketStoragePath.resolve(tarFileId);
        this.currentTempTarFilePath = fileBucketStoragePath.resolve(tarFileId + LocalFileUtils.TMP_EXTENSION);

        LOGGER.info("Creating file {}", this.currentTempTarFilePath);

        try {
            TapeArchiveReferentialEntity tarReferentialEntity = new TapeArchiveReferentialEntity(
                tarFileId,
                new TapeLibraryBuildingOnDiskArchiveStorageLocation(),
                null,
                null,
                LocalDateUtil.getFormattedDateTimeForMongo(now)
            );
            archiveReferentialRepository.insert(tarReferentialEntity);
        } catch (ArchiveReferentialException ex) {
            throw new QueueProcessingException(
                QueueProcessingException.RetryPolicy.RETRY,
                "Could not create a new tar file",
                ex
            );
        }

        this.currentTarOutputStream = new ExtendedFileOutputStream(currentTempTarFilePath, true);
        this.currentTarAppender = new TarAppender(currentTarOutputStream, tarFileId, this.maxTarFileSize);
        this.tarBufferingTimoutChecker = this.scheduledExecutorService.schedule(
                () -> checkTarBufferingTimeout(tarFileId),
                tarBufferingTimeout,
                tarBufferingTimeUnit
            );
    }

    private void finalizeTarFile() throws IOException {
        this.currentTarAppender.close();

        // Mark file as done (remove .tmp extension)
        Files.move(this.currentTempTarFilePath, this.currentTarFilePath, StandardCopyOption.ATOMIC_MOVE);

        // Schedule tar for copy on tape
        WriteOrder writeOrder = new WriteOrder(
            this.bucketId,
            this.fileBucketId,
            LocalFileUtils.archiveFileNameRelativeToInputArchiveStorageFolder(
                this.fileBucketId,
                this.currentTarAppender.getTarId()
            ),
            this.currentTarAppender.getBytesWritten(),
            this.currentTarAppender.getDigestValue(),
            this.currentTarAppender.getTarId(),
            QueueMessageType.WriteOrder
        );
        this.writeOrderCreator.addToQueue(writeOrder);

        this.currentTarAppender = null;
        this.currentTarOutputStream = null;
        this.tarBufferingTimoutChecker.cancel(false);
    }

    private void indexInObjectReferential(
        InputFileToProcessMessage inputFileToProcessMessage,
        List<TarEntryDescription> tarEntryDescriptions
    ) throws QueueProcessingException {
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
                "Could not index object referential " +
                inputFileToProcessMessage.getContainerName() +
                "/" +
                inputFileToProcessMessage.getObjectName() +
                " (" +
                inputFileToProcessMessage.getStorageId() +
                ") to tar files " +
                JsonHandler.unprettyPrint(tarEntryDescriptions),
                ex
            );
        }
    }

    private void checkTarBufferingTimeout(TarBufferingTimedOutMessage tarBufferingTimedOutMessage)
        throws QueueProcessingException {
        // Double check tar Id in case of concurrent access
        if (
            this.currentTarAppender == null ||
            !this.currentTarAppender.getTarId().equals(tarBufferingTimedOutMessage.getTarId())
        ) {
            return;
        }

        try {
            LOGGER.info(
                "Finalizing tar file {} after timeout {} {}",
                this.currentTempTarFilePath,
                this.tarBufferingTimeout,
                this.tarBufferingTimeUnit
            );

            finalizeTarFile();
        } catch (IOException ex) {
            throw new QueueProcessingException(
                QueueProcessingException.RetryPolicy.FATAL_SHUTDOWN,
                "An error occurred while archiving file to tar",
                ex
            );
        }
    }

    private void checkTarBufferingTimeout(String tarId) {
        addFirst(new TarBufferingTimedOutMessage(tarId));
    }

    public boolean containsTar(String tarId) {
        Path tempTarPath = fileBucketStoragePath.resolve(tarId + TMP_EXTENSION);
        if (Files.exists(tempTarPath)) {
            LOGGER.debug(tempTarPath + " found");
            return true;
        }

        Path readyTarPath = fileBucketStoragePath.resolve(tarId);
        if (Files.exists(readyTarPath)) {
            LOGGER.debug(readyTarPath + " found");
            return true;
        }

        LOGGER.info("Tar file " + tarId + " could not be located. Concurrent delete?");
        return false;
    }

    public Optional<FileInputStream> tryReadTar(String tarId) {
        Path tempTarPath = fileBucketStoragePath.resolve(tarId + TMP_EXTENSION);
        if (Files.exists(tempTarPath)) {
            try {
                FileInputStream fileInputStream = new FileInputStream(tempTarPath.toFile());
                return Optional.of(fileInputStream);
            } catch (FileNotFoundException e) {
                LOGGER.info("Could not open " + tempTarPath + ". Concurrent rename?", e);
            }
        }

        Path readyTarPath = fileBucketStoragePath.resolve(tarId);
        if (Files.exists(readyTarPath)) {
            try {
                FileInputStream fileInputStream = new FileInputStream(readyTarPath.toFile());
                return Optional.of(fileInputStream);
            } catch (FileNotFoundException e) {
                LOGGER.info("Could not open " + readyTarPath + ". Concurrent delete?", e);
            }
        }

        LOGGER.info("Tar file " + tarId + " could not be located. Concurrent delete?");
        return Optional.empty();
    }

    public void addToQueue(TarCreatorMessage message) {
        super.addToQueue(message);
        if (message instanceof InputFileToProcessMessage) {
            InputFilesMetrics.QUEUED_INPUT_FILES_COUNT.labels(bucketId).inc();

            InputFilesMetrics.QUEUED_INPUT_FILES_SIZE.labels(bucketId).inc(
                ((InputFileToProcessMessage) message).getSize()
            );
        }
    }
}
