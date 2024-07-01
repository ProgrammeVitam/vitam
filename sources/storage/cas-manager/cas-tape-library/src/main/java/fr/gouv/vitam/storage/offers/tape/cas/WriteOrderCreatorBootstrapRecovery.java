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

import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.security.IllegalPathException;
import fr.gouv.vitam.common.stream.ExtendedFileOutputStream;
import fr.gouv.vitam.common.stream.SizedInputStream;
import fr.gouv.vitam.storage.engine.common.model.QueueMessageType;
import fr.gouv.vitam.storage.engine.common.model.TapeArchiveReferentialEntity;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryBuildingOnDiskArchiveStorageLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryOnTapeArchiveStorageLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryReadyOnDiskArchiveStorageLocation;
import fr.gouv.vitam.storage.engine.common.model.WriteOrder;
import fr.gouv.vitam.storage.offers.tape.exception.ArchiveReferentialException;
import fr.gouv.vitam.storage.offers.tape.exception.ObjectReferentialException;
import fr.gouv.vitam.storage.offers.tape.exception.QueueException;
import fr.gouv.vitam.storage.offers.tape.utils.LocalFileUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static fr.gouv.vitam.storage.offers.tape.utils.LocalFileUtils.getCreationDateFromArchiveId;
import static org.apache.commons.lang.StringUtils.isEmpty;

public class WriteOrderCreatorBootstrapRecovery {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(WriteOrderCreatorBootstrapRecovery.class);

    private final String inputTarStorageFolder;
    private final ArchiveReferentialRepository archiveReferentialRepository;
    private final BucketTopologyHelper bucketTopologyHelper;
    private final WriteOrderCreator writeOrderCreator;
    private final TarFileRapairer tarFileRapairer;
    private final ArchiveCacheStorage archiveCacheStorage;

    public WriteOrderCreatorBootstrapRecovery(
        String inputTarStorageFolder,
        ArchiveReferentialRepository archiveReferentialRepository,
        BucketTopologyHelper bucketTopologyHelper,
        WriteOrderCreator writeOrderCreator,
        TarFileRapairer tarFileRapairer,
        ArchiveCacheStorage archiveCacheStorage
    ) {
        this.inputTarStorageFolder = inputTarStorageFolder;
        this.archiveReferentialRepository = archiveReferentialRepository;
        this.bucketTopologyHelper = bucketTopologyHelper;
        this.writeOrderCreator = writeOrderCreator;
        this.tarFileRapairer = tarFileRapairer;
        this.archiveCacheStorage = archiveCacheStorage;
    }

    public void initializeOnBootstrap() {
        try {
            //Backup files
            Path fileBucketTarStoragePath = Paths.get(inputTarStorageFolder).resolve(
                BucketTopologyHelper.BACKUP_FILE_BUCKET
            );
            if (fileBucketTarStoragePath.toFile().exists()) {
                recoverFileBucket(BucketTopologyHelper.BACKUP_FILE_BUCKET, fileBucketTarStoragePath);
            }

            // Other files
            for (String fileBucket : bucketTopologyHelper.listFileBuckets()) {
                fileBucketTarStoragePath = Paths.get(inputTarStorageFolder).resolve(fileBucket);
                if (fileBucketTarStoragePath.toFile().exists()) {
                    recoverFileBucket(fileBucket, fileBucketTarStoragePath);
                }
            }
        } catch (Exception e) {
            throw new VitamRuntimeException("Could not reschedule tar files to copy on tape", e);
        }
    }

    private void recoverFileBucket(String fileBucket, Path fileBucketArchiveStoragePath)
        throws IOException, ArchiveReferentialException, ObjectReferentialException, QueueException, IllegalPathException {
        Map<String, FileGroup> archiveFileGroups = getFileListGroupedByArchiveId(fileBucketArchiveStoragePath);

        List<String> archiveFileNames = cleanupIncompleteFiles(fileBucketArchiveStoragePath, archiveFileGroups);

        // Sort files by creation date
        sortFilesByCreationDate(archiveFileNames);

        // Process archives
        for (String archiveFileName : archiveFileNames) {
            if (
                archiveFileName.endsWith(LocalFileUtils.TAR_EXTENSION) ||
                archiveFileName.endsWith(LocalFileUtils.ZIP_EXTENSION)
            ) {
                processReadyArchive(fileBucket, fileBucketArchiveStoragePath, archiveFileName);
            } else if (archiveFileName.endsWith(LocalFileUtils.TMP_EXTENSION)) {
                repairArchive(fileBucketArchiveStoragePath, archiveFileName, fileBucket);
            } else {
                throw new IllegalStateException("Invalid file extension " + archiveFileName);
            }
        }
    }

    private Map<String, FileGroup> getFileListGroupedByArchiveId(Path fileBucketArchiveStoragePath) throws IOException {
        // List tar file paths
        Map<String, FileGroup> archiveFileNames = new HashMap<>();
        try (Stream<Path> archiveFileStream = Files.list(fileBucketArchiveStoragePath)) {
            archiveFileStream
                .map(filePath -> filePath.getFileName().toString())
                .forEach(archiveFileName -> {
                    // Group files by archive id
                    String archiveId = LocalFileUtils.archiveFileNamePathToArchiveId(archiveFileName);
                    FileGroup fileGroup = archiveFileNames.computeIfAbsent(archiveId, f -> new FileGroup());

                    if (archiveFileName.endsWith(LocalFileUtils.TMP_EXTENSION)) {
                        fileGroup.tmpFileName = archiveFileName;
                    } else {
                        fileGroup.readyFileName = archiveFileName;
                    }
                });
        }
        return archiveFileNames;
    }

    private List<String> cleanupIncompleteFiles(
        Path fileBucketTarStoragePath,
        Map<String, FileGroup> archiveFileGroups
    ) throws IOException {
        /* Delete incomplete files
         * > X.tar.tmp + X.tar          : Delete incomplete .tar
         * > X.tar.tmp                  : NOP
         * > X.tar                      : NOP
         */

        /* Delete incomplete files
         * > X.zip.tmp + X.zip          : Delete incomplete .zip
         * > X.zip.tmp                  : Delete incomplete .tmp (no guarantee  that file completely received)
         * > X.zip                      : NOP
         */

        List<String> tarFileNames = new ArrayList<>();
        for (FileGroup fileGroup : archiveFileGroups.values()) {
            if (fileGroup.tmpFileName != null) {
                boolean backupFiles = fileGroup.tmpFileName.endsWith(LocalFileUtils.ZIP_EXTENSION);
                if (fileGroup.readyFileName == null && backupFiles) {
                    LOGGER.warn("Deleting potential incomplete file " + fileGroup.tmpFileName);
                    Files.delete(fileBucketTarStoragePath.resolve(fileGroup.tmpFileName));
                    fileGroup.tmpFileName = null;
                    continue;
                }

                if (fileGroup.readyFileName != null) {
                    LOGGER.warn("Deleting incomplete file " + fileGroup.readyFileName);
                    Files.delete(fileBucketTarStoragePath.resolve(fileGroup.readyFileName));
                    fileGroup.readyFileName = null;
                }

                // As we have ready file name, the tmp file should be completely received
                tarFileNames.add(fileGroup.tmpFileName);
            } else {
                LOGGER.info("Found ready file " + fileGroup.readyFileName);
                tarFileNames.add(fileGroup.readyFileName);
            }
        }
        return tarFileNames;
    }

    private void sortFilesByCreationDate(List<String> archiveFileNames) {
        archiveFileNames.sort((filename1, filename2) -> {
            String archiveId1 = LocalFileUtils.archiveFileNamePathToArchiveId(filename1);
            String archiveId2 = LocalFileUtils.archiveFileNamePathToArchiveId(filename1);

            String creationDate1 = getCreationDateFromArchiveId(archiveId1);
            String creationDate2 = getCreationDateFromArchiveId(archiveId2);

            int compare = creationDate1.compareTo(creationDate2);
            if (compare != 0) return compare;
            return filename1.compareTo(filename2);
        });
    }

    private void processReadyArchive(String fileBucket, Path fileBucketArchiveStoragePath, String archiveId)
        throws ArchiveReferentialException, IOException, ObjectReferentialException, QueueException, IllegalPathException {
        Path archiveFile = fileBucketArchiveStoragePath.resolve(archiveId);

        Optional<TapeArchiveReferentialEntity> tarReferentialEntity = archiveReferentialRepository.find(archiveId);
        if (tarReferentialEntity.isEmpty()) {
            throw new IllegalStateException("Unknown archive file in Archive referential '" + archiveFile + "'");
        }

        if (tarReferentialEntity.get().getLocation() instanceof TapeLibraryOnTapeArchiveStorageLocation) {
            if (archiveCacheStorage.containsArchive(fileBucket, archiveId)) {
                LOGGER.warn(
                    "Archive file {}/{} already written on tape, and in cache. Deleting it",
                    fileBucket,
                    archiveFile
                );
                Files.delete(archiveFile);
            } else {
                LOGGER.warn("Archive file {}/{} already written on tape, moving it to cache", fileBucket, archiveFile);

                // Reserve cache space and move archive to cache
                archiveCacheStorage.reserveArchiveStorageSpace(
                    fileBucket,
                    archiveId,
                    tarReferentialEntity.get().getSize()
                );
                try {
                    archiveCacheStorage.moveArchiveToCache(archiveFile, fileBucket, archiveId);
                } catch (Exception e) {
                    archiveCacheStorage.cancelReservedArchive(fileBucket, archiveId);
                    throw e;
                }
            }
        } else if (tarReferentialEntity.get().getLocation() instanceof TapeLibraryReadyOnDiskArchiveStorageLocation) {
            // TODO: 10/06/19 check that no write order in the queue else do not enqueue a new WriteOrder
            LOGGER.warn("Rescheduling archive file {} for copy on tape.", archiveFile);
            enqueue(
                bucketTopologyHelper.getBucketFromFileBucket(fileBucket),
                fileBucket,
                archiveId,
                tarReferentialEntity.get().getSize(),
                tarReferentialEntity.get().getDigestValue()
            );
        } else if (
            tarReferentialEntity.get().getLocation() instanceof TapeLibraryBuildingOnDiskArchiveStorageLocation
        ) {
            LOGGER.warn("Check archive file & compute size & digest.", archiveFile);
            if (!BucketTopologyHelper.BACKUP_FILE_BUCKET.equals(fileBucket)) {
                TarFileRapairer.DigestWithSize digestWithSize = verifyTarArchive(archiveFile);

                // Add to queue
                enqueue(
                    bucketTopologyHelper.getBucketFromFileBucket(fileBucket),
                    fileBucket,
                    archiveId,
                    digestWithSize.getSize(),
                    digestWithSize.getDigestValue()
                );
            } else {
                reScheduleArchive(
                    fileBucketArchiveStoragePath,
                    BucketTopologyHelper.BACKUP_BUCKET,
                    BucketTopologyHelper.BACKUP_FILE_BUCKET,
                    archiveId,
                    0,
                    null
                );
            }
        } else {
            throw new IllegalStateException(
                "Invalid tar location " +
                tarReferentialEntity.get().getLocation().getClass() +
                " (" +
                JsonHandler.unprettyPrint(tarReferentialEntity) +
                ")"
            );
        }
    }

    private TarFileRapairer.DigestWithSize verifyTarArchive(Path archiveFile)
        throws IOException, ObjectReferentialException {
        try (InputStream inputStream = Files.newInputStream(archiveFile, StandardOpenOption.READ)) {
            return tarFileRapairer.verifyTarArchive(inputStream);
        }
    }

    private void repairArchive(Path fileBucketArchiveStoragePath, String tmpFileName, String fileBucket)
        throws IOException, ObjectReferentialException, QueueException, ArchiveReferentialException {
        String archiveId = LocalFileUtils.archiveFileNamePathToArchiveId(tmpFileName);

        Path tmpFilePath = fileBucketArchiveStoragePath.resolve(tmpFileName);
        Path finalFilePath = fileBucketArchiveStoragePath.resolve(archiveId);

        LOGGER.info("Repairing & verifying file " + tmpFilePath);

        String bucket = BucketTopologyHelper.BACKUP_BUCKET;
        String digestValue = null;
        long size = 0;
        if (!BucketTopologyHelper.BACKUP_FILE_BUCKET.equals(fileBucket)) {
            bucket = bucketTopologyHelper.getBucketFromFileBucket(fileBucket);

            TarFileRapairer.DigestWithSize digestWithSize;
            try (
                InputStream inputStream = Files.newInputStream(tmpFilePath, StandardOpenOption.READ);
                OutputStream outputStream = new ExtendedFileOutputStream(finalFilePath, true)
            ) {
                digestWithSize = tarFileRapairer.repairAndVerifyTarArchive(inputStream, outputStream, archiveId);
                digestValue = digestWithSize.getDigestValue();
                size = digestWithSize.getSize();
            }
        } else {
            Files.move(tmpFilePath, finalFilePath, StandardCopyOption.ATOMIC_MOVE);
        }

        Files.delete(tmpFilePath);

        LOGGER.info("Successfully repaired & verified file " + tmpFilePath + " to " + finalFilePath);
        reScheduleArchive(fileBucketArchiveStoragePath, bucket, fileBucket, archiveId, size, digestValue);
    }

    private void enqueue(String bucket, String fileBucket, String archiveId, long size, String digest)
        throws ArchiveReferentialException, QueueException {
        // Add to queue
        WriteOrder message = new WriteOrder(
            bucket,
            fileBucket,
            LocalFileUtils.archiveFileNameRelativeToInputArchiveStorageFolder(fileBucket, archiveId),
            size,
            digest,
            archiveId,
            QueueMessageType.WriteOrder
        );

        // Write Backup order
        if (BucketTopologyHelper.BACKUP_BUCKET.equals(bucket)) {
            message.setMessageType(QueueMessageType.WriteBackupOrder);
        }

        writeOrderCreator.sendMessageToQueue(message);
    }

    private void reScheduleArchive(
        Path fileBucketArchiveStoragePath,
        String bucket,
        String fileBucket,
        String archiveId,
        long size,
        String digestValue
    ) throws IOException, ArchiveReferentialException, QueueException {
        Path finalFilePath = fileBucketArchiveStoragePath.resolve(archiveId);

        if (isEmpty(digestValue)) {
            // Compute digest
            final Digest digest = new Digest(VitamConfiguration.getDefaultDigestType());
            try (
                SizedInputStream is = new SizedInputStream(Files.newInputStream(finalFilePath, StandardOpenOption.READ))
            ) {
                digest.update(is);
                digestValue = digest.digestHex();
                size = is.getSize();
            }
        }

        // Add to queue
        enqueue(bucket, fileBucket, archiveId, size, digestValue);
    }

    private static class FileGroup {

        private String readyFileName;
        private String tmpFileName;
    }
}
