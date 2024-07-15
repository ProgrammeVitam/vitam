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
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.stream.ExtendedFileOutputStream;
import fr.gouv.vitam.common.stream.SizedInputStream;
import fr.gouv.vitam.storage.engine.common.model.EntryType;
import fr.gouv.vitam.storage.engine.common.model.QueueMessageType;
import fr.gouv.vitam.storage.engine.common.model.TapeArchiveReferentialEntity;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryBuildingOnDiskArchiveStorageLocation;
import fr.gouv.vitam.storage.engine.common.model.WriteOrder;
import fr.gouv.vitam.storage.offers.tape.exception.ArchiveReferentialException;
import fr.gouv.vitam.storage.offers.tape.exception.BackupWriteException;
import fr.gouv.vitam.storage.offers.tape.utils.LocalFileUtils;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static fr.gouv.vitam.storage.offers.tape.utils.LocalFileUtils.fileBuckedInputFilePath;

public class BackupFileStorage {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(BackupFileStorage.class);

    private final ArchiveReferentialRepository archiveReferentialRepository;
    private final WriteOrderCreator writeOrderCreator;
    private final String bucketId;
    private final String fileBucketId;
    private final Path fileBucketStoragePath;

    public BackupFileStorage(
        ArchiveReferentialRepository archiveReferentialRepository,
        WriteOrderCreator writeOrderCreator,
        String bucketId,
        String fileBucketId,
        String inputArchiveStorageFolder
    ) {
        this.archiveReferentialRepository = archiveReferentialRepository;
        this.writeOrderCreator = writeOrderCreator;

        this.bucketId = bucketId;
        this.fileBucketId = fileBucketId;
        this.fileBucketStoragePath = fileBuckedInputFilePath(inputArchiveStorageFolder, fileBucketId);

        // Ensure directories exists
        try {
            Files.createDirectories(fileBucketStoragePath);
        } catch (IOException e) {
            throw new VitamRuntimeException(
                "Could not initialize file bucket tar creator service " + fileBucketStoragePath,
                e
            );
        }
    }

    public void writeFile(String uniqueFileName, InputStream in) throws BackupWriteException {
        LOGGER.debug("Write backup file :" + uniqueFileName);

        final Digest digest = new Digest(VitamConfiguration.getDefaultDigestType());
        try (
            ExtendedFileOutputStream extendedFileOutputStream = createBackupFile(uniqueFileName);
            SizedInputStream sizedInputStream = new SizedInputStream(in);
            InputStream digestInputStream = digest.getDigestInputStream(sizedInputStream)
        ) {
            IOUtils.copy(digestInputStream, extendedFileOutputStream);
            extendedFileOutputStream.fsync();

            finalizeBackupFile(uniqueFileName, sizedInputStream.getSize(), digest.digestHex());
        } catch (IOException | RuntimeException ex) {
            throw new BackupWriteException("An error occurred while copying backup file to disk", ex);
        }
    }

    private ExtendedFileOutputStream createBackupFile(String uniqueFileName) throws BackupWriteException, IOException {
        Path currentArchiveFilePath = fileBucketStoragePath.resolve(uniqueFileName);
        Path currentTmpArchiveFilePath = fileBucketStoragePath.resolve(uniqueFileName + LocalFileUtils.TMP_EXTENSION);

        if (currentArchiveFilePath.toFile().exists() || currentTmpArchiveFilePath.toFile().exists()) {
            throw new IOException("Backup file with same name " + uniqueFileName + " already exists or in progress");
        }

        LOGGER.info("Creating file {}", currentTmpArchiveFilePath);

        try {
            TapeArchiveReferentialEntity tarReferentialEntity = new TapeArchiveReferentialEntity(
                uniqueFileName,
                new TapeLibraryBuildingOnDiskArchiveStorageLocation(),
                EntryType.BACKUP,
                null,
                null,
                LocalDateUtil.nowFormatted()
            );
            archiveReferentialRepository.insert(tarReferentialEntity);
        } catch (ArchiveReferentialException ex) {
            throw new BackupWriteException("Could not create a new archive file in DB", ex);
        }

        return new ExtendedFileOutputStream(currentTmpArchiveFilePath, true);
    }

    private void finalizeBackupFile(String uniqueFileName, long size, String digest) throws IOException {
        Path currentArchiveFilePath = fileBucketStoragePath.resolve(uniqueFileName);
        Path currentTmpArchiveFilePath = fileBucketStoragePath.resolve(uniqueFileName + LocalFileUtils.TMP_EXTENSION);

        if (!currentTmpArchiveFilePath.toFile().exists()) {
            throw new IOException("Backup file with name " + uniqueFileName + " not found");
        }

        // Mark file as done (remove .tmp extension)
        Files.move(currentTmpArchiveFilePath, currentArchiveFilePath, StandardCopyOption.ATOMIC_MOVE);

        // Schedule tar for copy on tape
        WriteOrder writeOrder = new WriteOrder(
            this.bucketId,
            this.fileBucketId,
            LocalFileUtils.archiveFileNameRelativeToInputArchiveStorageFolder(this.fileBucketId, uniqueFileName),
            size,
            digest,
            uniqueFileName,
            QueueMessageType.WriteBackupOrder
        );
        this.writeOrderCreator.addToQueue(writeOrder);
    }
}
