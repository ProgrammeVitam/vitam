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

import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.storage.tapelibrary.TapeLibraryConfiguration;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryBuildingOnDiskTarStorageLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryOnTapeTarStorageLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryReadyOnDiskTarStorageLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryTarReferentialEntity;
import fr.gouv.vitam.storage.engine.common.model.WriteOrder;
import fr.gouv.vitam.storage.offers.tape.exception.ObjectReferentialException;
import fr.gouv.vitam.storage.offers.tape.exception.QueueException;
import fr.gouv.vitam.storage.offers.tape.exception.TarReferentialException;
import fr.gouv.vitam.storage.offers.tape.inmemoryqueue.QueueProcessingException;
import fr.gouv.vitam.storage.offers.tape.inmemoryqueue.QueueProcessor;
import fr.gouv.vitam.storage.offers.tape.spec.QueueRepository;
import fr.gouv.vitam.storage.offers.tape.utils.LocalFileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.input.CountingInputStream;

import java.io.IOException;
import java.io.InputStream;
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

import static fr.gouv.vitam.storage.offers.tape.utils.LocalFileUtils.getCreationDateFromTarId;

public class WriteOrderCreator extends QueueProcessor<WriteOrder> {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(WriteOrderCreator.class);

    private final TapeLibraryConfiguration configuration;
    private final ObjectReferentialRepository objectReferentialRepository;
    private final TarReferentialRepository tarReferentialRepository;
    private final BucketTopologyHelper bucketTopologyHelper;
    private final QueueRepository readWriteQueue;

    public WriteOrderCreator(TapeLibraryConfiguration configuration,
        ObjectReferentialRepository objectReferentialRepository,
        TarReferentialRepository tarReferentialRepository,
        BucketTopologyHelper bucketTopologyHelper,
        QueueRepository readWriteQueue) {
        super("WriteOrderCreator");
        this.configuration = configuration;
        this.objectReferentialRepository = objectReferentialRepository;
        this.tarReferentialRepository = tarReferentialRepository;
        this.bucketTopologyHelper = bucketTopologyHelper;
        this.readWriteQueue = readWriteQueue;
    }

    private static class FileGroup {
        private String readyTarFileName;
        private String tmpFileName;
        private String repairFileName;
    }

    @Override
    public void initializeOnBootstrap() {

        try {

            for (String fileBucket : bucketTopologyHelper.listFileBuckets()) {

                Path fileBucketTarStoragePath = Paths.get(configuration.getInputTarStorageFolder()).resolve(fileBucket);
                if (fileBucketTarStoragePath.toFile().exists()) {

                    // List tar file paths
                    Map<String, FileGroup> tarFileNames = new HashMap<>();
                    try (Stream<Path> tarFileStream = Files.list(fileBucketTarStoragePath)) {
                        tarFileStream
                            .map(filePath -> filePath.getFileName().toString())
                            .forEach(tarFileName -> {

                                FileGroup fileGroup = tarFileNames.computeIfAbsent(tarFileName, f -> new FileGroup());

                                if (tarFileName.endsWith(LocalFileUtils.TMP_EXTENSION)) {
                                    fileGroup.tmpFileName = tarFileName;
                                } else if (tarFileName.endsWith(LocalFileUtils.REPAIR_EXTENSION)) {
                                    fileGroup.repairFileName = tarFileName;
                                } else {
                                    fileGroup.readyTarFileName = tarFileName;
                                }
                            });
                    }

                    /* Delete incomplete files
                     * > X.tar.tmp + X.tar    : Delete .tar (failed non atomic rename from .tar.tmp -> .tar)
                     * > X.tar.tmp + X.repair : Delete .repair
                     * > X.tar.tmp            : Repair file (generate .tar.repair, delete .tmp, rename .tar.repair -> .tar)
                     * > X.tar.repair + X.tar : Delete .tar (failed non atomic rename from .tar.repair -> .tar)
                     * > X.tar.repair         : Rename to .tar
                     * > X.tar                : OK
                     */

                    List<String> tmpTarFileNames = new ArrayList<>();
                    List<String> readyTarFileNames = new ArrayList<>();

                    for (FileGroup fileGroup : tarFileNames.values()) {

                        if (fileGroup.tmpFileName != null) {

                            if (fileGroup.readyTarFileName != null) {
                                LOGGER.warn("Deleting incomplete " + LocalFileUtils.TAR_EXTENSION + " file " +
                                    fileGroup.readyTarFileName);
                                Files.delete(fileBucketTarStoragePath.resolve(fileGroup.readyTarFileName));
                                fileGroup.readyTarFileName = null;
                            }

                            if (fileGroup.repairFileName != null) {
                                LOGGER.warn("Deleting incomplete " + LocalFileUtils.REPAIR_EXTENSION + " file " +
                                    fileGroup.repairFileName);
                                Files.delete(fileBucketTarStoragePath.resolve(fileGroup.repairFileName));
                                fileGroup.repairFileName = null;
                            }

                            tmpTarFileNames.add(fileGroup.tmpFileName);

                        } else if (fileGroup.repairFileName != null) {

                            if (fileGroup.readyTarFileName != null) {

                                LOGGER.warn("Delete incomplete file " + fileGroup.readyTarFileName +
                                    " from file " + fileGroup.repairFileName);

                                Files.delete(fileBucketTarStoragePath.resolve(fileGroup.readyTarFileName));
                            }

                            String readyTarFileName = LocalFileUtils.tarFileNamePathToTarId(fileGroup.repairFileName);
                            LOGGER.info("Move repaired file " + fileGroup.repairFileName +
                                " to file " + readyTarFileName);

                            Files.move(
                                fileBucketTarStoragePath.resolve(fileGroup.repairFileName),
                                fileBucketTarStoragePath.resolve(readyTarFileName),
                                StandardCopyOption.ATOMIC_MOVE
                            );

                            readyTarFileNames.add(readyTarFileName);
                        } else {

                            LOGGER.info("Found ready file " + fileGroup.readyTarFileName);
                            readyTarFileNames.add(fileGroup.readyTarFileName);
                        }
                    }

                    // Sort files by creation date
                    readyTarFileNames.sort(this::tarFileCreationDateComparator);
                    tmpTarFileNames.sort(this::tarFileCreationDateComparator);

                    // Process ready tar files
                    for (String tarId : readyTarFileNames) {

                        Path tarFile = fileBucketTarStoragePath.resolve(tarId);

                        Optional<TapeLibraryTarReferentialEntity> tarReferentialEntity =
                            tarReferentialRepository.find(tarId);
                        if (!tarReferentialEntity.isPresent()) {
                            throw new IllegalStateException(
                                "Unknown tar file in tar referential '" + tarFile.toString() + "'");
                        }

                        if (tarReferentialEntity.get()
                            .getLocation() instanceof TapeLibraryOnTapeTarStorageLocation) {

                            LOGGER.warn("Tar file {} already written on tape. Deleting it", tarFile);
                            Files.delete(tarFile);

                        } else if (tarReferentialEntity.get().getLocation()
                            instanceof TapeLibraryReadyOnDiskTarStorageLocation) {

                            LOGGER.warn("Rescheduling tar file {} for copy on tape.", tarFile);
                            this.addToQueue(
                                new WriteOrder(
                                    bucketTopologyHelper.getBucketFromFileBucket(fileBucket),
                                    LocalFileUtils.tarFileNameRelativeToInputTarStorageFolder(fileBucket, tarId),
                                    tarReferentialEntity.get().getSize(),
                                    tarReferentialEntity.get().getDigestValue()
                                )
                            );

                        } else if (tarReferentialEntity.get().getLocation()
                            instanceof TapeLibraryBuildingOnDiskTarStorageLocation) {

                            LOGGER.warn("Check tar file & compute size & digest.", tarFile);
                            DigestWithSize digestWithSize = verifyTarArchive(tarFile);

                            // Mark file as ready
                            tarReferentialRepository.updateLocationToReadyOnDisk(
                                tarId,
                                digestWithSize.size,
                                digestWithSize.digestValue
                            );

                            // Add to queue
                            this.addToQueue(
                                new WriteOrder(
                                    bucketTopologyHelper.getBucketFromFileBucket(fileBucket),
                                    LocalFileUtils.tarFileNameRelativeToInputTarStorageFolder(fileBucket, tarId),
                                    digestWithSize.size,
                                    digestWithSize.digestValue
                                )
                            );
                        } else {
                            throw new IllegalStateException(
                                "Invalid tar location " + tarReferentialEntity.get().getLocation().getClass()
                                    + " (" + JsonHandler.unprettyPrint(tarReferentialEntity) + ")");
                        }

                    }

                    // Repair tmp tar files
                    for (String tmpTarFileName : tmpTarFileNames) {
                        repairTarArchive(fileBucketTarStoragePath.resolve(tmpTarFileName), fileBucket);
                    }
                }

            }
        } catch (Exception e) {
            throw new VitamRuntimeException("Could reschedule tar files to copy on tape", e);
        }
    }

    private int tarFileCreationDateComparator(String filename1, String filename2) {

        String creationDate1 = getCreationDateFromTarId(filename1);
        String creationDate2 = getCreationDateFromTarId(filename1);

        int compare = creationDate1.compareTo(creationDate2);
        if (compare != 0)
            return compare;
        return filename1.compareTo(filename2);
    }

    private DigestWithSize verifyTarArchive(Path tarFile) throws IOException, ObjectReferentialException {

        Digest tarDigest = new Digest(VitamConfiguration.getDefaultDigestType());
        try (InputStream inputStream = Files.newInputStream(tarFile, StandardOpenOption.READ);
            CountingInputStream countingInputStream = new CountingInputStream(inputStream);
            InputStream digestInputStream = tarDigest.getDigestInputStream(countingInputStream)) {

            TarFileDigestVerifier tarFileDigestVerifier = new TarFileDigestVerifier
                (this.objectReferentialRepository, VitamConfiguration.getBatchSize());
            tarFileDigestVerifier.verifyTarArchive(digestInputStream);

            return new DigestWithSize(countingInputStream.getByteCount(), tarDigest.digestHex());
        }
    }


    private static class DigestWithSize {

        final long size;
        final String digestValue;

        DigestWithSize(long size, String digestValue) {
            this.size = size;
            this.digestValue = digestValue;
        }

    }

    private void repairTarArchive(Path corruptedTarFilePath, String fileBucket)
        throws IOException, ObjectReferentialException, TarReferentialException {

        String tarId = FilenameUtils.removeExtension(corruptedTarFilePath.getFileName().toString());
        Path repairedFilePath = corruptedTarFilePath.resolveSibling(
            tarId + LocalFileUtils.REPAIR_EXTENSION);
        Path finalFilePath = corruptedTarFilePath.resolveSibling(tarId);

        CorruptedTarFileRapairer corruptedTarFileRapairer = new CorruptedTarFileRapairer();
        corruptedTarFileRapairer.recopy(corruptedTarFilePath, repairedFilePath, tarId);

        LOGGER.info("Successfully repaired file " + corruptedTarFilePath + " to " + repairedFilePath);

        DigestWithSize digestWithSize = verifyTarArchive(repairedFilePath);

        LOGGER.info("Successfully validated repaired file " + repairedFilePath);

        Files.delete(corruptedTarFilePath);
        Files.move(repairedFilePath, finalFilePath, StandardCopyOption.ATOMIC_MOVE);


        // Mark file as ready
        tarReferentialRepository.updateLocationToReadyOnDisk(
            tarId,
            digestWithSize.size,
            digestWithSize.digestValue
        );

        // Add to queue
        this.addToQueue(
            new WriteOrder(
                fileBucket,
                LocalFileUtils.tarFileNameRelativeToInputTarStorageFolder(fileBucket, tarId),
                digestWithSize.size,
                digestWithSize.digestValue
            )
        );
    }

    @Override
    protected void processMessage(WriteOrder message) throws QueueProcessingException {
        try {

            sendMessageToQueue(message);
        } catch (QueueProcessingException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new QueueProcessingException(QueueProcessingException.RetryPolicy.RETRY,
                "Could not post message queue.", ex);
        }
    }

    private void sendMessageToQueue(WriteOrder message)
        throws QueueProcessingException, QueueException {

        markAsReady(message);

        // Schedule tar archive for copy on tape
        readWriteQueue.add(message);
    }

    private void markAsReady(WriteOrder message) throws QueueProcessingException {
        // Mark tar archive as "ready"
        try {
            this.tarReferentialRepository.updateLocationToReadyOnDisk(
                message.getId(),
                message.getSize(),
                message.getDigest()
            );
        } catch (TarReferentialException e) {
            throw new QueueProcessingException(
                QueueProcessingException.RetryPolicy.RETRY,
                "Could not mark tar file are ready", e);
        }
    }
}
