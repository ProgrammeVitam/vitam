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

import com.google.common.collect.Iterators;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryInputFileObjectStorageLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeObjectReferentialEntity;
import fr.gouv.vitam.storage.offers.tape.exception.ObjectReferentialException;
import fr.gouv.vitam.storage.offers.tape.utils.LocalFileUtils;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

public class FileBucketTarCreatorBootstrapRecovery {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(
        FileBucketTarCreatorBootstrapRecovery.class
    );

    private final BasicFileStorage basicFileStorage;
    private final ObjectReferentialRepository objectReferentialRepository;

    public FileBucketTarCreatorBootstrapRecovery(
        BasicFileStorage basicFileStorage,
        ObjectReferentialRepository objectReferentialRepository
    ) {
        this.basicFileStorage = basicFileStorage;
        this.objectReferentialRepository = objectReferentialRepository;
    }

    public void initializeOnBootstrap(
        String fileBucketId,
        FileBucketTarCreator fileBucketTarCreator,
        BucketTopologyHelper bucketTopologyHelper
    ) {
        Set<String> containerNames = bucketTopologyHelper.listContainerNames(fileBucketId);

        try {
            // List existing files
            for (String containerName : containerNames) {
                try (
                    Stream<String> storageIdsStream = this.basicFileStorage.listStorageIdsByContainerName(containerName)
                ) {
                    Iterator<List<String>> bulkIterator = Iterators.partition(
                        storageIdsStream.iterator(),
                        VitamConfiguration.getBatchSize()
                    );

                    while (bulkIterator.hasNext()) {
                        List<String> storageIds = bulkIterator.next();
                        try {
                            recoverBulkStorageIds(containerName, storageIds, fileBucketTarCreator);
                        } catch (ObjectReferentialException e) {
                            throw new VitamRuntimeException(
                                "Could not initialize service to container " + containerName + " files " + storageIds,
                                e
                            );
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new VitamRuntimeException(
                "Could not recover file bucket tar creator on bootstrap for fileBucket " + fileBucketId,
                e
            );
        }
    }

    private void recoverBulkStorageIds(
        String containerName,
        List<String> storageIds,
        FileBucketTarCreator fileBucketTarCreator
    ) throws ObjectReferentialException {
        if (storageIds.isEmpty()) {
            return;
        }

        // Map storage ids to object names
        Map<String, String> storageIdToObjectNameMap = storageIds
            .stream()
            .collect(toMap(storageId -> storageId, LocalFileUtils::storageIdToObjectName));
        HashSet<String> objectNames = new HashSet<>(storageIdToObjectNameMap.values());

        // Find objects in object referential (bulk)
        List<TapeObjectReferentialEntity> objectReferentialEntities =
            this.objectReferentialRepository.bulkFind(containerName, objectNames);

        Map<String, TapeObjectReferentialEntity> objectReferentialEntityByObjectNameMap = objectReferentialEntities
            .stream()
            .collect(toMap(entity -> entity.getId().getObjectName(), entity -> entity));

        // Process storage ids
        for (String storageId : storageIds) {
            String objectName = storageIdToObjectNameMap.get(storageId);

            if (!objectReferentialEntityByObjectNameMap.containsKey(objectName)) {
                // Not found in DB -> Log & delete file
                LOGGER.warn("Incomplete file " + storageId + ". Will be deleted");
                this.basicFileStorage.deleteFile(containerName, storageId);
            } else {
                TapeObjectReferentialEntity objectReferentialEntity = objectReferentialEntityByObjectNameMap.get(
                    objectName
                );

                if (!storageId.equals(objectReferentialEntity.getStorageId())) {
                    // Not found in DB -> Log & delete file
                    LOGGER.warn("Incomplete or obsolete file " + storageId + ". Will be deleted");
                    this.basicFileStorage.deleteFile(containerName, storageId);
                } else if (objectReferentialEntity.getLocation() instanceof TapeLibraryInputFileObjectStorageLocation) {
                    // Found & in file  =>  Add to queue
                    LOGGER.warn("Input file to be scheduled for archival " + containerName + "/" + storageId);
                    fileBucketTarCreator.addToQueue(
                        new InputFileToProcessMessage(
                            containerName,
                            objectName,
                            storageId,
                            objectReferentialEntity.getSize(),
                            objectReferentialEntity.getDigest(),
                            objectReferentialEntity.getDigestType()
                        )
                    );
                } else {
                    // Input file already archived to TAR => Delete it
                    LOGGER.debug("Input file already archived " + containerName + "/" + storageId);
                    this.basicFileStorage.deleteFile(containerName, storageId);
                }
            }
        }
    }
}
