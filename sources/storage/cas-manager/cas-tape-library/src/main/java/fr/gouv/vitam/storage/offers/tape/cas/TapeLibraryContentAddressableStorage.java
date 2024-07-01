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

import com.google.common.util.concurrent.Uninterruptibles;
import com.mongodb.MongoException;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.collection.CloseableIterator;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.MetadatasObject;
import fr.gouv.vitam.common.model.storage.AccessRequestStatus;
import fr.gouv.vitam.common.model.storage.ObjectEntry;
import fr.gouv.vitam.common.security.IllegalPathException;
import fr.gouv.vitam.common.storage.ContainerInformation;
import fr.gouv.vitam.common.storage.cas.container.api.ContentAddressableStorage;
import fr.gouv.vitam.common.storage.cas.container.api.MetadatasStorageObject;
import fr.gouv.vitam.common.storage.cas.container.api.ObjectContent;
import fr.gouv.vitam.common.storage.cas.container.api.ObjectListingListener;
import fr.gouv.vitam.common.storage.constants.ErrorMessage;
import fr.gouv.vitam.common.stream.ExactDigestValidatorInputStream;
import fr.gouv.vitam.common.stream.ExactSizeInputStream;
import fr.gouv.vitam.common.stream.LazySequenceInputStream;
import fr.gouv.vitam.storage.engine.common.model.TapeArchiveReferentialEntity;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryInputFileObjectStorageLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryObjectReferentialId;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryObjectStorageLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryTarObjectStorageLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeObjectReferentialEntity;
import fr.gouv.vitam.storage.engine.common.model.TarEntryDescription;
import fr.gouv.vitam.storage.engine.common.utils.ContainerUtils;
import fr.gouv.vitam.storage.offers.tape.exception.ArchiveReferentialException;
import fr.gouv.vitam.storage.offers.tape.exception.ObjectReferentialException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageUnavailableDataFromAsyncOfferException;
import org.apache.commons.collections4.SetUtils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.NoSuchFileException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class TapeLibraryContentAddressableStorage implements ContentAddressableStorage {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(
        TapeLibraryContentAddressableStorage.class
    );
    private static final int NB_GET_OBJECT_RETRIES_ON_CONCURRENT_UPDATE = 5;
    private static final int RETRY_GET_OBJECT_SLEEP_MIN_DELAY = 10;
    private static final int RETRY_GET_OBJECT_SLEEP_MAX_DELAY = 3000;

    private final BasicFileStorage basicFileStorage;
    private final ObjectReferentialRepository objectReferentialRepository;
    private final FileBucketTarCreatorManager fileBucketTarCreatorManager;
    private final ArchiveReferentialRepository archiveReferentialRepository;
    private final AccessRequestManager accessRequestManager;
    private final ArchiveCacheStorage archiveCacheStorage;
    private final ArchiveCacheEvictionController archiveCacheEvictionController;
    private final BucketTopologyHelper bucketTopologyHelper;

    public TapeLibraryContentAddressableStorage(
        BasicFileStorage basicFileStorage,
        ObjectReferentialRepository objectReferentialRepository,
        ArchiveReferentialRepository archiveReferentialRepository,
        AccessRequestManager accessRequestManager,
        FileBucketTarCreatorManager fileBucketTarCreatorManager,
        ArchiveCacheStorage archiveCacheStorage,
        ArchiveCacheEvictionController archiveCacheEvictionController,
        BucketTopologyHelper bucketTopologyHelper
    ) {
        this.basicFileStorage = basicFileStorage;
        this.objectReferentialRepository = objectReferentialRepository;
        this.archiveReferentialRepository = archiveReferentialRepository;
        this.accessRequestManager = accessRequestManager;
        this.fileBucketTarCreatorManager = fileBucketTarCreatorManager;
        this.archiveCacheStorage = archiveCacheStorage;
        this.archiveCacheEvictionController = archiveCacheEvictionController;
        this.bucketTopologyHelper = bucketTopologyHelper;
    }

    @Override
    public void createContainer(String containerName) {
        // NOP
    }

    @Override
    public boolean isExistingContainer(String containerName) {
        return true;
    }

    @Override
    public void writeObject(
        String containerName,
        String objectName,
        InputStream inputStream,
        DigestType digestType,
        long size
    ) throws ContentAddressableStorageException {
        LOGGER.debug(String.format("Upload object %s in container %s", objectName, containerName));

        Digest digest = new Digest(digestType);
        InputStream digestInputStream = digest.getDigestInputStream(inputStream);

        // Persist to disk
        String storageId;
        try {
            storageId = this.basicFileStorage.writeFile(containerName, objectName, digestInputStream, size);
        } catch (IOException e) {
            throw new ContentAddressableStorageException(
                "Could not persist file to disk " + containerName + "/" + objectName,
                e
            );
        }
        String digestValue = digest.digestHex();

        // Commit to mongo
        try {
            String now = LocalDateUtil.now().toString();
            objectReferentialRepository.insertOrUpdate(
                new TapeObjectReferentialEntity(
                    new TapeLibraryObjectReferentialId(containerName, objectName),
                    size,
                    digestType.getName(),
                    digestValue,
                    storageId,
                    new TapeLibraryInputFileObjectStorageLocation(),
                    now,
                    now
                )
            );
        } catch (ObjectReferentialException ex) {
            throw new ContentAddressableStorageServerException(
                String.format("Could not index the object %s in container %s in database", objectName, containerName),
                ex
            );
        } finally {
            // Add message to queue even on DB exception :
            // - If DB insertion succeeded, but got exception due to Network failure ==> This ensures that the object didn't get lost
            // - If DB insertion failed ==> object will be added to TAR, but will be orphan (that's OK)

            // Notify tar file builder queue
            fileBucketTarCreatorManager.addToQueue(
                new InputFileToProcessMessage(
                    containerName,
                    objectName,
                    storageId,
                    size,
                    digestValue,
                    digestType.getName()
                )
            );
        }
    }

    @Override
    public void checkObjectDigestAndStoreDigest(
        String containerName,
        String objectName,
        String objectDigest,
        DigestType digestType,
        long size
    ) {
        // Digest check is done while copying file into TARs
        // Object digest already persisted
    }

    @Override
    public ObjectContent getObject(String containerName, String objectName) throws ContentAddressableStorageException {
        LOGGER.debug(String.format("Download object %s from container %s", objectName, containerName));

        for (int nbTry = 0; nbTry < NB_GET_OBJECT_RETRIES_ON_CONCURRENT_UPDATE; nbTry++) {
            Optional<ObjectContent> objectContent = tryReadObject(containerName, objectName);
            if (objectContent.isPresent()) {
                return objectContent.get();
            }

            LOGGER.warn("Could not read object " + containerName + "/" + objectName + " due to concurrent update");
            int sleepDelay = ThreadLocalRandom.current()
                .nextInt(RETRY_GET_OBJECT_SLEEP_MIN_DELAY, RETRY_GET_OBJECT_SLEEP_MAX_DELAY);
            Uninterruptibles.sleepUninterruptibly(sleepDelay, TimeUnit.MILLISECONDS);
        }

        throw new ContentAddressableStorageServerException("Could not read object " + containerName + "/" + objectName);
    }

    private Optional<ObjectContent> tryReadObject(String containerName, String objectName)
        throws ContentAddressableStorageServerException, ContentAddressableStorageNotFoundException, ContentAddressableStorageUnavailableDataFromAsyncOfferException {
        TapeObjectReferentialEntity objectReferentialEntity = getTapeObjectReferentialEntity(containerName, objectName);

        TapeLibraryObjectStorageLocation location = objectReferentialEntity.getLocation();
        if (location instanceof TapeLibraryInputFileObjectStorageLocation) {
            Optional<InputStream> inputStream = tryReadObjectFromInputFile(containerName, objectReferentialEntity);
            if (inputStream.isPresent()) {
                return Optional.of(toObjectContent(inputStream.get(), objectReferentialEntity));
            }
            return Optional.empty();
        }

        if (location instanceof TapeLibraryTarObjectStorageLocation) {
            InputStream inputStream = readFromTarFiles(
                containerName,
                objectName,
                ((TapeLibraryTarObjectStorageLocation) objectReferentialEntity.getLocation()).getTarEntries()
            );
            return Optional.of(toObjectContent(inputStream, objectReferentialEntity));
        }

        throw new IllegalStateException("Unknown object storage location: " + location.getClass());
    }

    private TapeObjectReferentialEntity getTapeObjectReferentialEntity(String containerName, String objectName)
        throws ContentAddressableStorageServerException, ContentAddressableStorageNotFoundException {
        Optional<TapeObjectReferentialEntity> objectReferentialEntity;
        try {
            objectReferentialEntity = objectReferentialRepository.find(containerName, objectName);
        } catch (ObjectReferentialException e) {
            throw new ContentAddressableStorageServerException(e);
        }

        if (objectReferentialEntity.isEmpty()) {
            throw new ContentAddressableStorageNotFoundException(
                ErrorMessage.OBJECT_NOT_FOUND + containerName + "/" + objectName
            );
        }
        return objectReferentialEntity.get();
    }

    private Optional<InputStream> tryReadObjectFromInputFile(
        String containerName,
        TapeObjectReferentialEntity tapeObjectReferentialEntity
    ) throws ContentAddressableStorageServerException {
        try {
            InputStream inputStream =
                this.basicFileStorage.readFile(containerName, tapeObjectReferentialEntity.getStorageId());

            return Optional.of(inputStream);
        } catch (FileNotFoundException | NoSuchFileException ex) {
            LOGGER.warn(
                "Could not open inputFile '" +
                containerName +
                "/" +
                tapeObjectReferentialEntity.getStorageId() +
                "'. " +
                "Concurrent delete or move to TAR archive?",
                ex
            );
            return Optional.empty();
        } catch (IOException ex) {
            throw new ContentAddressableStorageServerException(
                "An error occurred during reading inputFile '" +
                containerName +
                "/" +
                tapeObjectReferentialEntity.getStorageId() +
                "'",
                ex
            );
        }
    }

    private InputStream readFromTarFiles(String containerName, String objectName, List<TarEntryDescription> tarEntries)
        throws ContentAddressableStorageServerException, ContentAddressableStorageUnavailableDataFromAsyncOfferException {
        if (tarEntries == null || tarEntries.isEmpty()) {
            throw new IllegalStateException("empty TAR description for object : " + containerName + "/" + objectName);
        }
        String fileBucketId = this.bucketTopologyHelper.getFileBucketFromContainerName(containerName);

        Set<String> tarIds = getTarIds(tarEntries);
        Map<String, TapeArchiveReferentialEntity> tapeArchiveReferentialEntityMap = getTapeArchiveEntities(tarIds);

        // Most objects are stored in a single TAR entry.
        if (tarEntries.size() == 1) {
            // Just load / return TAR entry content.
            TarEntryDescription tarEntry = tarEntries.get(0);
            return loadTarFileInputStream(containerName, objectName, tarEntry);
        }

        return loadLargeObjectInputStream(
            containerName,
            objectName,
            tarEntries,
            fileBucketId,
            tarIds,
            tapeArchiveReferentialEntityMap
        );
    }

    private Set<String> getTarIds(List<TarEntryDescription> tarEntryDescriptions) {
        return tarEntryDescriptions.stream().map(TarEntryDescription::getTarFileId).collect(Collectors.toSet());
    }

    private Map<String, TapeArchiveReferentialEntity> getTapeArchiveEntities(Set<String> tarFileIds)
        throws ContentAddressableStorageServerException {
        List<TapeArchiveReferentialEntity> tapeArchiveReferentialEntityList;
        try {
            tapeArchiveReferentialEntityList = archiveReferentialRepository.bulkFind(tarFileIds);
        } catch (ArchiveReferentialException e) {
            throw new ContentAddressableStorageServerException("Could not load archive info from DB", e);
        }
        Map<String, TapeArchiveReferentialEntity> tapeArchiveReferentialEntityMap = tapeArchiveReferentialEntityList
            .stream()
            .collect(
                Collectors.toMap(
                    TapeArchiveReferentialEntity::getArchiveId,
                    tapeArchiveReferentialEntity -> tapeArchiveReferentialEntity
                )
            );

        if (tapeArchiveReferentialEntityMap.size() != tarFileIds.size()) {
            // Should never occur. Tape archives are never deleted
            Set<String> missingTarIds = SetUtils.difference(tarFileIds, tapeArchiveReferentialEntityMap.keySet());
            throw new IllegalStateException("Could not locate TAR(s) archives " + missingTarIds);
        }
        return tapeArchiveReferentialEntityMap;
    }

    private LockHandle lockTarEntryEviction(String fileBucketId, Set<String> tarIds) {
        return archiveCacheEvictionController.createLock(
            tarIds.stream().map(tarId -> new ArchiveCacheEntry(fileBucketId, tarId)).collect(Collectors.toSet())
        );
    }

    private LazySequenceInputStream loadLargeObjectInputStream(
        String containerName,
        String objectName,
        List<TarEntryDescription> tarEntries,
        String fileBucketId,
        Set<String> tarIds,
        Map<String, TapeArchiveReferentialEntity> tapeArchiveReferentialEntityMap
    ) throws ContentAddressableStorageUnavailableDataFromAsyncOfferException {
        // For large objects which span multiple TAR Entries :
        // - Lock TAR files to prevent there eviction from cache
        // - Pre-check existence of all TAR files (as a building_on_disk, ready_on_disk or in cache TAR)
        // - Return a lazy-loaded sequence of tar entry streams to avoid "too many open files"-like errors
        // - Finally, unlock TAR files (remove temp lock) on error or response stream closed ended.
        LockHandle evictionLock = lockTarEntryEviction(fileBucketId, tarIds);
        try {
            checkTarExistence(fileBucketId, tapeArchiveReferentialEntityMap.values());

            // Lazy loading of TarEntry input streams
            Iterator<InputStream> lazyInputStreamIterator = tarEntries
                .stream()
                .map(tarEntry -> {
                    try {
                        return loadTarFileInputStream(containerName, objectName, tarEntry);
                    } catch (
                        ContentAddressableStorageUnavailableDataFromAsyncOfferException
                        | ContentAddressableStorageServerException e
                    ) {
                        throw new RuntimeException(
                            "Could not load entry " +
                            fileBucketId +
                            "/" +
                            tarEntry.getTarFileId() +
                            " @" +
                            tarEntry.getEntryName(),
                            e
                        );
                    }
                })
                .iterator();

            return new LazySequenceInputStream(lazyInputStreamIterator) {
                @Override
                public void close() throws IOException {
                    // Release any eviction locks
                    evictionLock.release();
                    super.close();
                }
            };
        } catch (Exception e) {
            // On error, release any eviction locks & rethrow the exception
            evictionLock.release();
            throw e;
        }
    }

    private void checkTarExistence(
        String fileBucketId,
        Collection<TapeArchiveReferentialEntity> tapeArchiveReferentialEntities
    ) throws ContentAddressableStorageUnavailableDataFromAsyncOfferException {
        for (TapeArchiveReferentialEntity tapeArchiveReferentialEntity : tapeArchiveReferentialEntities) {
            checkTarExistence(fileBucketId, tapeArchiveReferentialEntity.getArchiveId());
        }
    }

    private void checkTarExistence(String fileBucketId, String archiveId)
        throws ContentAddressableStorageUnavailableDataFromAsyncOfferException {
        if (this.fileBucketTarCreatorManager.containsTar(fileBucketId, archiveId)) {
            LOGGER.debug("{}/{} found." + fileBucketId);
            return;
        }

        if (archiveCacheStorage.containsArchive(fileBucketId, archiveId)) {
            LOGGER.debug("{}/{} found." + fileBucketId);
            return;
        }

        throw new ContentAddressableStorageUnavailableDataFromAsyncOfferException(
            "Could not find archive " + fileBucketId + "/" + archiveId
        );
    }

    private InputStream loadTarFileInputStream(String containerName, String objectName, TarEntryDescription tarEntry)
        throws ContentAddressableStorageUnavailableDataFromAsyncOfferException, ContentAddressableStorageServerException {
        try {
            FileInputStream fileInputStream = locateAndOpenTarFileInputStream(containerName, objectName, tarEntry);
            return TarHelper.readEntryAtPos(fileInputStream, tarEntry);
        } catch (IOException e) {
            throw new ContentAddressableStorageServerException("Could not load tar file", e);
        }
    }

    private FileInputStream locateAndOpenTarFileInputStream(
        String containerName,
        String objectName,
        TarEntryDescription tarEntry
    ) throws ContentAddressableStorageUnavailableDataFromAsyncOfferException {
        String fileBucketId = this.bucketTopologyHelper.getFileBucketFromContainerName(containerName);

        Optional<FileInputStream> fileInputStream =
            this.fileBucketTarCreatorManager.tryReadTar(fileBucketId, tarEntry.getTarFileId());

        if (fileInputStream.isPresent()) return fileInputStream.get();

        try {
            Optional<FileInputStream> cachedFileInputStream = archiveCacheStorage.tryReadArchive(
                fileBucketId,
                tarEntry.getTarFileId()
            );

            if (cachedFileInputStream.isEmpty()) {
                throw new ContentAddressableStorageUnavailableDataFromAsyncOfferException(
                    "Could not locate archive " +
                    tarEntry.getTarFileId() +
                    " for object " +
                    containerName +
                    "/" +
                    objectName
                );
            }

            return cachedFileInputStream.get();
        } catch (IllegalPathException e) {
            throw new IllegalStateException(
                "Illegal fileBucketId/tarId : " + fileBucketId + "/" + tarEntry.getTarFileId(),
                e
            );
        }
    }

    private ObjectContent toObjectContent(
        InputStream inputStream,
        TapeObjectReferentialEntity tapeObjectReferentialEntity
    ) throws ContentAddressableStorageServerException {
        try {
            return new ObjectContent(
                new ExactDigestValidatorInputStream(
                    new ExactSizeInputStream(inputStream, tapeObjectReferentialEntity.getSize()),
                    VitamConfiguration.getDefaultDigestType(),
                    tapeObjectReferentialEntity.getDigest()
                ),
                tapeObjectReferentialEntity.getSize()
            );
        } catch (IOException e) {
            throw new ContentAddressableStorageServerException(e);
        }
    }

    @Override
    public String createAccessRequest(String containerName, List<String> objectsNames)
        throws ContentAddressableStorageException {
        return this.accessRequestManager.createAccessRequest(containerName, objectsNames);
    }

    @Override
    public Map<String, AccessRequestStatus> checkAccessRequestStatuses(
        List<String> accessRequestIds,
        boolean adminCrossTenantAccessRequestAllowed
    ) throws ContentAddressableStorageException {
        return this.accessRequestManager.checkAccessRequestStatuses(
                accessRequestIds,
                adminCrossTenantAccessRequestAllowed
            );
    }

    @Override
    public void removeAccessRequest(String accessRequestId, boolean adminCrossTenantAccessRequestAllowed)
        throws ContentAddressableStorageException {
        this.accessRequestManager.removeAccessRequest(accessRequestId, adminCrossTenantAccessRequestAllowed);
    }

    @Override
    public boolean checkObjectAvailability(String containerName, List<String> objectNames)
        throws ContentAddressableStorageException {
        return this.accessRequestManager.checkObjectAvailability(containerName, objectNames);
    }

    @Override
    public void deleteObject(String containerName, String objectName)
        throws ContentAddressableStorageServerException, ContentAddressableStorageNotFoundException {
        LOGGER.debug(String.format("Delete object %s from container %s", objectName, containerName));

        try {
            boolean objectDeleted =
                this.objectReferentialRepository.delete(new TapeLibraryObjectReferentialId(containerName, objectName));

            if (!objectDeleted) {
                throw new ContentAddressableStorageNotFoundException(ErrorMessage.OBJECT_NOT_FOUND + objectName);
            }
        } catch (ObjectReferentialException e) {
            throw new ContentAddressableStorageServerException(
                "Error on deleting object " + containerName + "/" + objectName
            );
        }
    }

    @Override
    public boolean isExistingObject(String containerName, String objectName)
        throws ContentAddressableStorageServerException {
        LOGGER.debug(String.format("Check existence of object %s in container %s", objectName, containerName));
        try {
            Optional<TapeObjectReferentialEntity> objectReferentialEntity = objectReferentialRepository.find(
                containerName,
                objectName
            );
            return objectReferentialEntity.isPresent();
        } catch (ObjectReferentialException ex) {
            throw new ContentAddressableStorageServerException(
                String.format("Could not check existence of object %s in container %s", objectName, containerName),
                ex
            );
        }
    }

    @Override
    public String getObjectDigest(String containerName, String objectName, DigestType algo, boolean noCache)
        throws ContentAddressableStorageException {
        LOGGER.debug(String.format("Get digest of object %s in container %s", objectName, containerName));
        try {
            Optional<TapeObjectReferentialEntity> objectReferentialEntity = objectReferentialRepository.find(
                containerName,
                objectName
            );

            if (objectReferentialEntity.isEmpty()) {
                throw new ContentAddressableStorageNotFoundException(
                    String.format("No such object %s in container %s", objectName, containerName)
                );
            }

            if (!algo.getName().equals(objectReferentialEntity.get().getDigestType())) {
                throw new ContentAddressableStorageNotFoundException(
                    String.format(
                        "Digest algorithm mismatch for object %s in container %s. Expected %s, found %s",
                        objectName,
                        containerName,
                        algo.getName(),
                        objectReferentialEntity.get().getDigestType()
                    )
                );
            }

            return objectReferentialEntity.get().getDigest();
        } catch (ObjectReferentialException ex) {
            throw new ContentAddressableStorageServerException(
                String.format("Could not get digest of object %s in container %s", objectName, containerName),
                ex
            );
        }
    }

    @Override
    public ContainerInformation getContainerInformation(String containerName) {
        LOGGER.debug(String.format("Get information of container %s", containerName));
        ParametersChecker.checkParameter(
            ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(),
            containerName
        );
        // we do not call the storage since it is not pertinent in tape library storage
        final ContainerInformation containerInformation = new ContainerInformation();
        containerInformation.setUsableSpace(-1);
        return containerInformation;
    }

    @Override
    public MetadatasObject getObjectMetadata(String containerName, String objectName, boolean noCache)
        throws ContentAddressableStorageException {
        LOGGER.debug(String.format("Get metadata of object %s in container %s", objectName, containerName));
        ParametersChecker.checkParameter(
            ErrorMessage.CONTAINER_OBJECT_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName,
            objectName
        );

        try {
            Optional<TapeObjectReferentialEntity> objectReferentialEntity = objectReferentialRepository.find(
                containerName,
                objectName
            );

            if (objectReferentialEntity.isEmpty()) {
                throw new ContentAddressableStorageNotFoundException(
                    String.format("No such object %s in container %s", objectName, containerName)
                );
            }

            MetadatasStorageObject result = new MetadatasStorageObject();
            result.setType(ContainerUtils.parseDataCategoryFromContainerName(containerName).getFolder());
            result.setObjectName(objectName);
            result.setDigest(objectReferentialEntity.get().getDigest());
            result.setFileSize(objectReferentialEntity.get().getSize());
            result.setLastModifiedDate(objectReferentialEntity.get().getLastObjectModifiedDate());
            return result;
        } catch (ObjectReferentialException ex) {
            throw new ContentAddressableStorageServerException(
                String.format("Could not get metadata of object %s in container %s", objectName, containerName),
                ex
            );
        }
    }

    @Override
    public void listContainer(String containerName, ObjectListingListener objectListingListener)
        throws ContentAddressableStorageServerException, IOException {
        LOGGER.info("Listing of objects of container {}", containerName);

        try (
            CloseableIterator<ObjectEntry> entryIterator = objectReferentialRepository.listContainerObjectEntries(
                containerName
            )
        ) {
            while (entryIterator.hasNext()) {
                objectListingListener.handleObjectEntry(entryIterator.next());
            }
        } catch (ObjectReferentialException | MongoException e) {
            throw new ContentAddressableStorageServerException(
                "Could not list objects of container " + containerName,
                e
            );
        }
        LOGGER.info("Done listing objects of container {}", containerName);
    }

    @Override
    public void close() {
        // NOP
    }
}
