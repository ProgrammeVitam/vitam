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

import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.storage.tapelibrary.TapeLibraryConfiguration;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static fr.gouv.vitam.storage.offers.tape.utils.LocalFileUtils.fileBuckedInputFilePath;

public class FileBucketTarCreatorManager {

    private final BucketTopologyHelper bucketTopologyHelper;
    private final Map<String, FileBucketTarCreator> fileBucketTarCreatorMap;
    private final FileBucketTarCreatorBootstrapRecovery fileBucketTarCreatorBootstrapRecovery;
    private final String inputTarStorageFolder;

    public FileBucketTarCreatorManager(
        TapeLibraryConfiguration tapeLibraryConfiguration,
        BasicFileStorage basicFileStorage,
        BucketTopologyHelper bucketTopologyHelper,
        ObjectReferentialRepository objectReferentialRepository,
        ArchiveReferentialRepository archiveReferentialRepository,
        WriteOrderCreator writeOrderCreator
    ) {
        this.bucketTopologyHelper = bucketTopologyHelper;

        this.fileBucketTarCreatorMap = bucketTopologyHelper
            .listFileBuckets()
            .stream()
            .collect(
                Collectors.toMap(
                    fileBucket -> fileBucket,
                    fileBucket ->
                        new FileBucketTarCreator(
                            basicFileStorage,
                            objectReferentialRepository,
                            archiveReferentialRepository,
                            writeOrderCreator,
                            bucketTopologyHelper.getBucketFromFileBucket(fileBucket),
                            fileBucket,
                            this.bucketTopologyHelper.getTarBufferingTimeoutInMinutes(
                                    this.bucketTopologyHelper.getBucketFromFileBucket(fileBucket)
                                ),
                            TimeUnit.MINUTES,
                            tapeLibraryConfiguration.getInputTarStorageFolder(),
                            tapeLibraryConfiguration.getMaxTarEntrySize(),
                            tapeLibraryConfiguration.getMaxTarFileSize()
                        )
                )
            );
        inputTarStorageFolder = tapeLibraryConfiguration.getInputTarStorageFolder();
        fileBucketTarCreatorBootstrapRecovery = new FileBucketTarCreatorBootstrapRecovery(
            basicFileStorage,
            objectReferentialRepository
        );
    }

    public void initializeOnBootstrap() {
        // Backup files
        ensureWorkingDirectoryExists(BucketTopologyHelper.BACKUP_FILE_BUCKET);

        // Other files
        for (Map.Entry<String, FileBucketTarCreator> entry : fileBucketTarCreatorMap.entrySet()) {
            String fileBucketId = entry.getKey();
            FileBucketTarCreator fileBucketTarCreator = entry.getValue();

            ensureWorkingDirectoryExists(fileBucketId);

            fileBucketTarCreatorBootstrapRecovery.initializeOnBootstrap(
                fileBucketId,
                fileBucketTarCreator,
                this.bucketTopologyHelper
            );
        }
    }

    private void ensureWorkingDirectoryExists(String fileBucketId) {
        Path fileBucketStoragePath = fileBuckedInputFilePath(this.inputTarStorageFolder, fileBucketId);
        try {
            Files.createDirectories(fileBucketStoragePath);
        } catch (IOException e) {
            throw new VitamRuntimeException(
                "Could not initialize file bucket tar creator service " + fileBucketStoragePath,
                e
            );
        }
    }

    public void startListeners() {
        for (FileBucketTarCreator fileBucketTarCreator : fileBucketTarCreatorMap.values()) {
            fileBucketTarCreator.startListener();
        }
    }

    public void addToQueue(InputFileToProcessMessage inputFileToProcessMessage) {
        String containerName = inputFileToProcessMessage.getContainerName();
        String fileBucket = bucketTopologyHelper.getFileBucketFromContainerName(containerName);

        FileBucketTarCreator fileBucketTarCreator = fileBucketTarCreatorMap.get(fileBucket);
        fileBucketTarCreator.addToQueue(inputFileToProcessMessage);
    }

    public boolean containsTar(String fileBucketId, String tarId) {
        FileBucketTarCreator fileBucketTarCreator = fileBucketTarCreatorMap.get(fileBucketId);
        return fileBucketTarCreator.containsTar(tarId);
    }

    public Optional<FileInputStream> tryReadTar(String fileBucketId, String tarId) {
        FileBucketTarCreator fileBucketTarCreator = fileBucketTarCreatorMap.get(fileBucketId);
        return fileBucketTarCreator.tryReadTar(tarId);
    }
}
