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
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.server.query.QueryCriteria;
import fr.gouv.vitam.common.database.server.query.QueryCriteriaOperator;
import fr.gouv.vitam.common.exception.InvalidGuidOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.guid.GUIDReader;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.storage.AccessRequestStatus;
import fr.gouv.vitam.common.thread.VitamThreadFactory;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.storage.engine.common.model.QueueMessageType;
import fr.gouv.vitam.storage.engine.common.model.ReadOrder;
import fr.gouv.vitam.storage.engine.common.model.TapeAccessRequestReferentialEntity;
import fr.gouv.vitam.storage.engine.common.model.TapeArchiveReferentialEntity;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryOnTapeArchiveStorageLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryTarObjectStorageLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeObjectReferentialEntity;
import fr.gouv.vitam.storage.engine.common.model.TarEntryDescription;
import fr.gouv.vitam.storage.offers.tape.exception.AccessRequestReferentialException;
import fr.gouv.vitam.storage.offers.tape.exception.ArchiveReferentialException;
import fr.gouv.vitam.storage.offers.tape.exception.ObjectReferentialException;
import fr.gouv.vitam.storage.offers.tape.exception.QueueException;
import fr.gouv.vitam.storage.offers.tape.spec.QueueRepository;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageBadRequestException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class AccessRequestManager {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AccessRequestManager.class);
    private static final int MAX_ACCESS_REQUEST_SIZE = 100_000;
    private static final int MAX_RETRIES = 3;

    private final ObjectReferentialRepository objectReferentialRepository;
    private final ArchiveReferentialRepository archiveReferentialRepository;
    private final AccessRequestReferentialRepository accessRequestReferentialRepository;
    private final ArchiveCacheStorage archiveCacheStorage;
    private final BucketTopologyHelper bucketTopologyHelper;
    private final QueueRepository readWriteQueue;
    private final int maxAccessRequestSize;
    private final int accessRequestExpirationDelay;
    private final TimeUnit accessRequestExpirationUnit;
    private final int accessRequestPurgeDelay;
    private final TimeUnit accessRequestPurgeUnit;
    private final int accessRequestCleanupTaskIntervalDelay;
    private final TimeUnit accessRequestCleanupTaskIntervalUnit;
    private final ScheduledExecutorService cleanupScheduler;

    public AccessRequestManager(
        ObjectReferentialRepository objectReferentialRepository,
        ArchiveReferentialRepository archiveReferentialRepository,
        AccessRequestReferentialRepository accessRequestReferentialRepository,
        ArchiveCacheStorage archiveCacheStorage,
        BucketTopologyHelper bucketTopologyHelper,
        QueueRepository readWriteQueue,
        int maxAccessRequestSize,
        int accessRequestExpirationDelay,
        TimeUnit accessRequestExpirationUnit,
        int accessRequestPurgeDelay,
        TimeUnit accessRequestPurgeUnit,
        int accessRequestCleanupTaskIntervalDelay,
        TimeUnit accessRequestCleanupTaskIntervalUnit
    ) {
        ParametersChecker.checkParameter(
            "Required parameters",
            objectReferentialRepository,
            archiveReferentialRepository,
            accessRequestReferentialRepository,
            archiveCacheStorage,
            bucketTopologyHelper
        );
        ParametersChecker.checkValue("Invalid maxAccessRequestSize", maxAccessRequestSize, 1);
        ParametersChecker.checkValue("Invalid maxAccessRequestSize", MAX_ACCESS_REQUEST_SIZE, maxAccessRequestSize);
        ParametersChecker.checkValue("Invalid accessRequestExpirationDelay", accessRequestExpirationDelay, 1);
        ParametersChecker.checkParameter("Invalid accessRequestExpirationUnit", accessRequestExpirationUnit);
        ParametersChecker.checkValue("Invalid accessRequestPurgeDelay", accessRequestPurgeDelay, 1);
        ParametersChecker.checkParameter("Invalid accessRequestPurgeUnit", accessRequestPurgeUnit);
        if (
            accessRequestPurgeUnit.convert(accessRequestPurgeDelay, TimeUnit.NANOSECONDS) <
            accessRequestExpirationUnit.convert(accessRequestExpirationDelay, TimeUnit.NANOSECONDS)
        ) {
            throw new IllegalArgumentException("Access request purge cannot occur before access request expiration");
        }
        ParametersChecker.checkValue(
            "Invalid accessRequestCleanupTaskIntervalDelay",
            accessRequestCleanupTaskIntervalDelay,
            1
        );
        ParametersChecker.checkParameter(
            "Invalid accessRequestCleanupTaskIntervalUnit",
            accessRequestCleanupTaskIntervalUnit
        );

        this.objectReferentialRepository = objectReferentialRepository;
        this.archiveReferentialRepository = archiveReferentialRepository;
        this.accessRequestReferentialRepository = accessRequestReferentialRepository;
        this.archiveCacheStorage = archiveCacheStorage;
        this.bucketTopologyHelper = bucketTopologyHelper;
        this.readWriteQueue = readWriteQueue;
        this.maxAccessRequestSize = maxAccessRequestSize;
        this.accessRequestExpirationDelay = accessRequestExpirationDelay;
        this.accessRequestExpirationUnit = accessRequestExpirationUnit;
        this.accessRequestPurgeDelay = accessRequestPurgeDelay;
        this.accessRequestPurgeUnit = accessRequestPurgeUnit;
        this.accessRequestCleanupTaskIntervalDelay = accessRequestCleanupTaskIntervalDelay;
        this.accessRequestCleanupTaskIntervalUnit = accessRequestCleanupTaskIntervalUnit;
        this.cleanupScheduler = Executors.newScheduledThreadPool(1, VitamThreadFactory.getInstance());
    }

    public void startExpirationHandler() {
        // Schedule access request cleaning task
        this.cleanupScheduler.scheduleWithFixedDelay(
                this::accessRequestCleanupTask,
                this.accessRequestCleanupTaskIntervalDelay,
                this.accessRequestCleanupTaskIntervalDelay,
                this.accessRequestCleanupTaskIntervalUnit
            );
    }

    public String createAccessRequest(String containerName, List<String> objectNames)
        throws ContentAddressableStorageException {
        // Check params
        ParametersChecker.checkParameter("Required containerName", containerName);
        ParametersChecker.checkParameter("Required objectNames", objectNames);
        ParametersChecker.checkParameter("Required objectNames", objectNames.toArray(String[]::new));
        checkRequestSize(objectNames);
        checkDuplicateObjectNames(objectNames);

        try {
            // Select objects stored in TARs, whose TARs are "on_tape", and not present in cache
            List<TapeArchiveReferentialEntity> unavailableArchivesOnDisk = getUnavailableArchivesOnDiskForObjects(
                containerName,
                objectNames
            );

            // Commit Access Request
            String accessRequestId = generateAccessRequestId();
            List<String> unavailableArchiveIds = unavailableArchivesOnDisk
                .stream()
                .map(TapeArchiveReferentialEntity::getArchiveId)
                .collect(Collectors.toList());
            LocalDateTime now = LocalDateUtil.now();
            String creationDate = LocalDateUtil.getFormattedDateTimeForMongo(now);
            String readyDate = unavailableArchiveIds.isEmpty() ? computeReadyDate(now) : null;
            String expirationDate = unavailableArchiveIds.isEmpty() ? computeExpirationDate(now) : null;
            String purgeDate = unavailableArchiveIds.isEmpty() ? computePurgeDate(now) : null;

            TapeAccessRequestReferentialEntity accessRequest = new TapeAccessRequestReferentialEntity(
                accessRequestId,
                containerName,
                objectNames,
                creationDate,
                readyDate,
                expirationDate,
                purgeDate,
                unavailableArchiveIds,
                VitamThreadUtils.getVitamSession().getTenantId(),
                0
            );
            this.accessRequestReferentialRepository.insert(accessRequest);

            // Create & schedule read orders
            bucketTopologyHelper.getFileBucketFromContainerName(containerName);
            List<ReadOrder> readOrders = createReadOrders(containerName, unavailableArchivesOnDisk);
            addReadOrdersToQueue(readOrders);

            return accessRequestId;
        } catch (
            AccessRequestReferentialException
            | QueueException
            | ArchiveReferentialException
            | ObjectReferentialException e
        ) {
            throw new ContentAddressableStorageServerException("An error occurred during access request creation.", e);
        }
    }

    public Map<String, AccessRequestStatus> checkAccessRequestStatuses(
        List<String> accessRequestIds,
        boolean adminCrossTenantAccessRequestAllowed
    ) throws ContentAddressableStorageException {
        for (String accessRequestId : accessRequestIds) {
            checkAccessRequestIdFormat(accessRequestId);
        }

        try {
            Set<String> accessRequestIdSet = new HashSet<>();
            accessRequestIds.forEach(accessRequestId -> {
                if (!accessRequestIdSet.add(accessRequestId)) {
                    throw new IllegalArgumentException("Duplicate access Request Id '" + accessRequestId + "'");
                }
            });

            List<TapeAccessRequestReferentialEntity> accessRequestEntities =
                accessRequestReferentialRepository.findByRequestIds(accessRequestIdSet);

            String now = LocalDateUtil.nowFormatted();
            boolean skipTenantCheck = skipTenantCheck(adminCrossTenantAccessRequestAllowed);
            int tenantId = VitamThreadUtils.getVitamSession().getTenantId();

            Map<String, AccessRequestStatus> results = new HashMap<>();
            for (TapeAccessRequestReferentialEntity accessRequestEntity : accessRequestEntities) {
                if (accessRequestEntity.getTenant() != tenantId && !skipTenantCheck) {
                    LOGGER.warn(
                        "Illegal access to AccessRequestId " +
                        accessRequestEntity.getRequestId() +
                        " of tenant " +
                        accessRequestEntity.getTenant() +
                        " from tenant " +
                        tenantId
                    );
                    results.putIfAbsent(accessRequestEntity.getRequestId(), AccessRequestStatus.NOT_FOUND);
                } else if (
                    accessRequestEntity.getExpirationDate() != null &&
                    accessRequestEntity.getExpirationDate().compareTo(now) < 0
                ) {
                    results.put(accessRequestEntity.getRequestId(), AccessRequestStatus.EXPIRED);
                } else if (CollectionUtils.isEmpty(accessRequestEntity.getUnavailableArchiveIds())) {
                    results.put(accessRequestEntity.getRequestId(), AccessRequestStatus.READY);
                } else {
                    results.put(accessRequestEntity.getRequestId(), AccessRequestStatus.NOT_READY);
                }
            }
            for (String accessRequestId : accessRequestIds) {
                results.putIfAbsent(accessRequestId, AccessRequestStatus.NOT_FOUND);
            }

            return results;
        } catch (AccessRequestReferentialException e) {
            throw new ContentAddressableStorageServerException(
                "An error occurred during access request status check by ids",
                e
            );
        }
    }

    private boolean skipTenantCheck(boolean adminCrossTenantAccessRequestAllowed) {
        int tenantId = VitamThreadUtils.getVitamSession().getTenantId();
        return adminCrossTenantAccessRequestAllowed && tenantId == VitamConfiguration.getAdminTenant();
    }

    public void removeAccessRequest(String accessRequestId, boolean adminCrossTenantAccessRequestAllowed)
        throws ContentAddressableStorageException {
        checkAccessRequestIdFormat(accessRequestId);

        try {
            Optional<TapeAccessRequestReferentialEntity> accessRequestEntity =
                accessRequestReferentialRepository.findByRequestId(accessRequestId);

            if (accessRequestEntity.isEmpty()) {
                // Log & continue (idempotency)
                LOGGER.warn("No such access request " + accessRequestId + ". Already deleted ?");
                return;
            }

            // Check tenant
            boolean skipTenantCheck = skipTenantCheck(adminCrossTenantAccessRequestAllowed);
            int tenantId = VitamThreadUtils.getVitamSession().getTenantId();
            if (accessRequestEntity.get().getTenant() != tenantId && !skipTenantCheck) {
                LOGGER.warn(
                    "Illegal access to AccessRequestId " +
                    accessRequestEntity.get().getRequestId() +
                    " of tenant " +
                    accessRequestEntity.get().getTenant() +
                    " from tenant " +
                    tenantId
                );
                return;
            }

            boolean deleted = accessRequestReferentialRepository.deleteAccessRequestById(accessRequestId);
            if (deleted) {
                cancelReadOrder(accessRequestEntity.get());
            }
        } catch (AccessRequestReferentialException | QueueException | ArchiveReferentialException e) {
            throw new ContentAddressableStorageServerException(
                "An error occurred during access request delete by id '" + accessRequestId + "'"
            );
        }
    }

    /**
     * Check immediate availability of objects for access.
     * An object is available if it is fully stored on disk. Not found objects are available for immediate access (immediate 404).
     *
     * @param containerName container name
     * @param objectNames list of object names whose immediate availability is to be checked
     * @return {@code true} if ALL objects are available, otherwise {@code false}.
     * @throws ContentAddressableStorageException on technical exception
     */
    public boolean checkObjectAvailability(String containerName, List<String> objectNames)
        throws ContentAddressableStorageException {
        // Check params
        ParametersChecker.checkParameter("Required containerName", containerName);
        ParametersChecker.checkParameter("Required objectNames", objectNames);
        ParametersChecker.checkParameter("Required objectNames", objectNames.toArray(String[]::new));
        checkRequestSize(objectNames);
        checkDuplicateObjectNames(objectNames);

        try {
            // Check if there are objects stored in TARs, whose TARs are "on_tape", and not present in cache
            List<TapeArchiveReferentialEntity> unavailableArchivesOnDisk = getUnavailableArchivesOnDiskForObjects(
                containerName,
                objectNames
            );

            if (unavailableArchivesOnDisk.isEmpty()) {
                LOGGER.debug(
                    "Immediate access is available for objects {} of container {}",
                    objectNames,
                    containerName
                );
                return true;
            }

            LOGGER.warn(
                "One or more objects of container {} are not available for on disk. " + "Object names: {}",
                containerName,
                objectNames
            );
            return false;
        } catch (ArchiveReferentialException | ObjectReferentialException e) {
            throw new ContentAddressableStorageServerException(
                "An error occurred while checking object availability.",
                e
            );
        }
    }

    private void checkRequestSize(List<String> objectsNames) throws ContentAddressableStorageBadRequestException {
        if (objectsNames.isEmpty()) {
            throw new ContentAddressableStorageBadRequestException("Empty request");
        }
        if (objectsNames.size() > maxAccessRequestSize) {
            throw new ContentAddressableStorageBadRequestException(
                "Request too large. Object count: " + objectsNames.size() + ", max: " + maxAccessRequestSize
            );
        }
    }

    private void checkDuplicateObjectNames(List<String> objectsNames)
        throws ContentAddressableStorageBadRequestException {
        Set<String> uniqueObjectNames = new HashSet<>();
        Set<String> duplicateNames = objectsNames
            .stream()
            .filter(objectName -> !uniqueObjectNames.add(objectName))
            .limit(10)
            .collect(Collectors.toSet());

        if (!duplicateNames.isEmpty()) {
            throw new ContentAddressableStorageBadRequestException(
                "Invalid request. Duplicate object names " + duplicateNames + " in access request"
            );
        }
    }

    private List<TapeArchiveReferentialEntity> getUnavailableArchivesOnDiskForObjects(
        String containerName,
        List<String> objectsIds
    ) throws ObjectReferentialException, ArchiveReferentialException {
        // Object access by object storage location :
        // - not found                 ==> Object can be accessed immediately (NOT_FOUND)
        // - input_files               ==> Object can be accessed immediately from disk (inputFiles/container/*)
        // - tar
        //   - building_on_disk        ==> Object can be accessed immediately from disk (inputTars/*.tar.tmp)
        //   - ready_on_disk           ==> Object can be accessed immediately from disk (inputTars/*.tar)
        //   - on_tape
        //     - existing in cache     ==> Object can be accessed immediately from disk (inputTars/*.tar)
        //     - non existing in cache ==> A ReadOrder is required

        // Select unique TarIds for objects stored in tars
        Set<String> tarIds = getTarIds(containerName, objectsIds);

        // Select archives stored on tape
        List<TapeArchiveReferentialEntity> onTapeArchives = selectArchivesStoredOnTape(tarIds);

        // Only retain archives that not present in disk cache
        return filterArchivesPresentInCache(containerName, onTapeArchives);
    }

    private Set<String> getTarIds(String containerName, List<String> objectNames) throws ObjectReferentialException {
        List<TapeObjectReferentialEntity> objectReferentialEntities = objectReferentialRepository.bulkFind(
            containerName,
            new HashSet<>(objectNames)
        );

        return objectReferentialEntities
            .stream()
            .filter(obj -> obj.getLocation() instanceof TapeLibraryTarObjectStorageLocation)
            .flatMap(obj -> ((TapeLibraryTarObjectStorageLocation) obj.getLocation()).getTarEntries().stream())
            .map(TarEntryDescription::getTarFileId)
            .collect(Collectors.toSet());
    }

    private List<TapeArchiveReferentialEntity> selectArchivesStoredOnTape(Collection<String> archiveIds)
        throws ArchiveReferentialException {
        HashSet<String> archiveIdSet = new HashSet<>(archiveIds);

        List<TapeArchiveReferentialEntity> archiveReferentialEntities =
            this.archiveReferentialRepository.bulkFind(archiveIdSet);

        if (archiveReferentialEntities.size() != archiveIds.size()) {
            Set<String> foundArchiveIds = archiveReferentialEntities
                .stream()
                .map(TapeArchiveReferentialEntity::getArchiveId)
                .collect(Collectors.toSet());
            throw new IllegalStateException(
                "Unknown archive ids: " + SetUtils.difference(archiveIdSet, foundArchiveIds)
            );
        }

        // Only return archives stored on tape
        return archiveReferentialEntities
            .stream()
            .filter(tarArchive -> tarArchive.getLocation() instanceof TapeLibraryOnTapeArchiveStorageLocation)
            .collect(Collectors.toList());
    }

    private List<TapeArchiveReferentialEntity> filterArchivesPresentInCache(
        String containerName,
        List<TapeArchiveReferentialEntity> onTapeArchives
    ) {
        String fileBucketId = this.bucketTopologyHelper.getFileBucketFromContainerName(containerName);
        return onTapeArchives
            .stream()
            .filter(
                archiveEntity -> !this.archiveCacheStorage.containsArchive(fileBucketId, archiveEntity.getArchiveId())
            )
            .collect(Collectors.toList());
    }

    private List<ReadOrder> createReadOrders(
        String containerName,
        List<TapeArchiveReferentialEntity> unavailableArchivesOnDisk
    ) {
        List<ReadOrder> readOrders = new ArrayList<>();
        String fileBucketId = bucketTopologyHelper.getFileBucketFromContainerName(containerName);
        String bucketId = bucketTopologyHelper.getBucketFromFileBucket(fileBucketId);

        for (TapeArchiveReferentialEntity archiveEntity : unavailableArchivesOnDisk) {
            // Create read orders
            TapeLibraryOnTapeArchiveStorageLocation onTapeLocation =
                (TapeLibraryOnTapeArchiveStorageLocation) archiveEntity.getLocation();

            ReadOrder readOrder = new ReadOrder(
                onTapeLocation.getTapeCode(),
                onTapeLocation.getFilePosition(),
                archiveEntity.getArchiveId(),
                bucketId,
                fileBucketId,
                archiveEntity.getSize()
            );
            readOrders.add(readOrder);
        }
        return readOrders;
    }

    private void addReadOrdersToQueue(List<ReadOrder> readOrders) throws QueueException {
        for (ReadOrder readOrder : readOrders) {
            // add read orders to worker queue
            readWriteQueue.addIfAbsent(
                Arrays.asList(
                    new QueryCriteria(ReadOrder.FILE_NAME, readOrder.getFileName(), QueryCriteriaOperator.EQ),
                    new QueryCriteria(
                        ReadOrder.MESSAGE_TYPE,
                        QueueMessageType.ReadOrder.name(),
                        QueryCriteriaOperator.EQ
                    )
                ),
                readOrder
            );
        }
    }

    public void updateAccessRequestWhenArchiveReady(String readyArchiveId) throws AccessRequestReferentialException {
        List<TapeAccessRequestReferentialEntity> accessRequestEntities =
            this.accessRequestReferentialRepository.findByUnavailableArchiveId(readyArchiveId);
        for (TapeAccessRequestReferentialEntity accessRequestEntity : accessRequestEntities) {
            updateAccessRequestWithReadyArchiveId(readyArchiveId, accessRequestEntity);
        }
    }

    private void updateAccessRequestWithReadyArchiveId(
        String readyArchiveId,
        TapeAccessRequestReferentialEntity accessRequestEntity
    ) throws AccessRequestReferentialException {
        for (int nbTry = 0; nbTry < MAX_RETRIES; nbTry++) {
            boolean updateSucceeded = tryUpdateAccessRequestWithReadyArchiveId(readyArchiveId, accessRequestEntity);
            if (updateSucceeded) {
                return;
            }

            LOGGER.warn("Concurrent update for " + accessRequestEntity.getRequestId() + ". Retry later...");
            Uninterruptibles.sleepUninterruptibly(RandomUtils.nextInt(10, 1000), TimeUnit.MILLISECONDS);

            // Retry update last access request
            Optional<TapeAccessRequestReferentialEntity> refreshedAccessRequestEntity =
                this.accessRequestReferentialRepository.findByRequestId(accessRequestEntity.getRequestId());
            if (refreshedAccessRequestEntity.isEmpty()) {
                LOGGER.info("Request id " + accessRequestEntity.getRequestId() + " deleted meanwhile.");
                return;
            }

            accessRequestEntity = refreshedAccessRequestEntity.get();
        }

        throw new AccessRequestReferentialException(
            "Could not update accessRequest " +
            accessRequestEntity.getRequestId() +
            ". Aborting after " +
            MAX_RETRIES +
            " unsuccessful retries"
        );
    }

    private boolean tryUpdateAccessRequestWithReadyArchiveId(
        String readyArchiveId,
        TapeAccessRequestReferentialEntity accessRequestEntity
    ) throws AccessRequestReferentialException {
        List<String> updatedUnavailableArchiveIds = accessRequestEntity
            .getUnavailableArchiveIds()
            .stream()
            .filter(archiveId -> !archiveId.equals(readyArchiveId))
            .collect(Collectors.toList());
        int updatedVersion = accessRequestEntity.getVersion() + 1;

        String updatedReadyDate = accessRequestEntity.getReadyDate();
        String updatedExpirationDate = accessRequestEntity.getExpirationDate();
        String updatedPurgeDate = accessRequestEntity.getPurgeDate();

        if (accessRequestEntity.getReadyDate() == null && updatedUnavailableArchiveIds.isEmpty()) {
            LocalDateTime now = LocalDateUtil.now();
            updatedReadyDate = computeReadyDate(now);
            updatedExpirationDate = computeExpirationDate(now);
            updatedPurgeDate = computePurgeDate(now);
        }

        TapeAccessRequestReferentialEntity updatedAccessRequestEntity = new TapeAccessRequestReferentialEntity(
            accessRequestEntity.getRequestId(),
            accessRequestEntity.getContainerName(),
            accessRequestEntity.getObjectNames(),
            accessRequestEntity.getCreationDate(),
            updatedReadyDate,
            updatedExpirationDate,
            updatedPurgeDate,
            updatedUnavailableArchiveIds,
            accessRequestEntity.getTenant(),
            updatedVersion
        );

        return this.accessRequestReferentialRepository.updateAccessRequest(
                updatedAccessRequestEntity,
                accessRequestEntity.getVersion()
            );
    }

    private void accessRequestCleanupTask() {
        String initialThreadName = Thread.currentThread().getName();
        try {
            Thread.currentThread().setName(initialThreadName + "-AccessRequestCleanupThread");

            // Update access requests readiness
            fixAccessRequestReadyStatus();

            // Delete expired access requests
            deleteExpiredAccessRequests();
        } catch (Exception e) {
            LOGGER.error("An error occurred during access request cleanup", e);
        } finally {
            Thread.currentThread().setName(initialThreadName);
        }
    }

    private void fixAccessRequestReadyStatus() throws AccessRequestReferentialException {
        LOGGER.info("Fixing ready status for access requests");

        // Fetch all "non-ready" access requests
        List<TapeAccessRequestReferentialEntity> nonReadyAccessRequests =
            this.accessRequestReferentialRepository.findNonReadyAccessRequests();

        // Deduplicate archiveIds
        Set<Pair<String, String>> fileBucketIdArchiveIdPairs = new HashSet<>();
        for (TapeAccessRequestReferentialEntity accessRequest : nonReadyAccessRequests) {
            String fileBucketId =
                this.bucketTopologyHelper.getFileBucketFromContainerName(accessRequest.getContainerName());
            for (String archiveId : accessRequest.getUnavailableArchiveIds()) {
                fileBucketIdArchiveIdPairs.add(ImmutablePair.of(fileBucketId, archiveId));
            }
        }

        List<String> actuallyReadyArchiveIds = fileBucketIdArchiveIdPairs
            .stream()
            .filter(i -> this.archiveCacheStorage.containsArchive(i.getLeft(), i.getRight()))
            .map(Pair::getRight)
            .collect(Collectors.toList());

        // Update access requests
        for (String actuallyReadyArchiveId : actuallyReadyArchiveIds) {
            LOGGER.warn(
                " ArchiveId: " +
                actuallyReadyArchiveId +
                "is actually ready. Fixing non updated access requests statuses"
            );
            updateAccessRequestWhenArchiveReady(actuallyReadyArchiveId);
        }
    }

    private void deleteExpiredAccessRequests() throws AccessRequestReferentialException {
        LOGGER.info("Cleaning-up expired access requests");
        List<TapeAccessRequestReferentialEntity> deletedAccessRequests =
            accessRequestReferentialRepository.cleanupAndGetExpiredAccessRequests();
        for (TapeAccessRequestReferentialEntity deletedAccessRequest : deletedAccessRequests) {
            LOGGER.warn("Expired access request " + deletedAccessRequest.getRequestId() + " deleted");
        }
    }

    private void cancelReadOrder(TapeAccessRequestReferentialEntity deletedAccessRequest)
        throws QueueException, ArchiveReferentialException, AccessRequestReferentialException {
        Set<String> archiveIdsToCheck = new HashSet<>(deletedAccessRequest.getUnavailableArchiveIds());

        if (archiveIdsToCheck.isEmpty()) {
            return;
        }

        Set<String> archiveIdsToCancel =
            accessRequestReferentialRepository.excludeArchiveIdsStillRequiredByAccessRequests(archiveIdsToCheck);

        for (String archiveId : archiveIdsToCancel) {
            readWriteQueue.tryCancelIfNotStarted(
                Arrays.asList(
                    new QueryCriteria(ReadOrder.FILE_NAME, archiveId, QueryCriteriaOperator.EQ),
                    new QueryCriteria(
                        ReadOrder.MESSAGE_TYPE,
                        QueueMessageType.ReadOrder.name(),
                        QueryCriteriaOperator.EQ
                    )
                )
            );
        }

        // Double check concurrent access request creation
        for (String archiveId : archiveIdsToCancel) {
            createReadOrderIfConcurrentAccessRequestCreated(archiveId);
        }
    }

    private void createReadOrderIfConcurrentAccessRequestCreated(String archiveId)
        throws AccessRequestReferentialException, ArchiveReferentialException, QueueException {
        List<TapeAccessRequestReferentialEntity> accessRequests =
            this.accessRequestReferentialRepository.findByUnavailableArchiveId(archiveId);
        if (accessRequests.isEmpty()) {
            LOGGER.debug("No concurrent access request created.");
            return;
        }

        LOGGER.warn("Concurrent access request created for " + archiveId + ". Re-append read order to queue");

        String containerName = accessRequests.get(0).getContainerName();
        String fileBucketId = this.bucketTopologyHelper.getFileBucketFromContainerName(containerName);
        String bucket = this.bucketTopologyHelper.getBucketFromFileBucket(fileBucketId);

        Optional<TapeArchiveReferentialEntity> tapeArchiveReferentialEntity =
            this.archiveReferentialRepository.find(archiveId);

        if (tapeArchiveReferentialEntity.isEmpty()) {
            throw new IllegalStateException("Archive id not found: " + archiveId);
        }

        if (!(tapeArchiveReferentialEntity.get().getLocation() instanceof TapeLibraryOnTapeArchiveStorageLocation)) {
            throw new IllegalStateException("Archive " + archiveId + " expected to be on tape");
        }

        TapeLibraryOnTapeArchiveStorageLocation tapeLocation =
            (TapeLibraryOnTapeArchiveStorageLocation) tapeArchiveReferentialEntity.get().getLocation();

        ReadOrder readOrder = new ReadOrder(
            tapeLocation.getTapeCode(),
            tapeLocation.getFilePosition(),
            archiveId,
            bucket,
            fileBucketId,
            tapeArchiveReferentialEntity.get().getSize()
        );
        addReadOrdersToQueue(List.of(readOrder));
    }

    public void shutdown() {
        cleanupScheduler.shutdown();
        try {
            if (!cleanupScheduler.awaitTermination(1, TimeUnit.MINUTES)) {
                throw new IllegalStateException("Could not shutdown access request manager");
            }
        } catch (InterruptedException e) {
            LOGGER.error("Thread interrupted", e);
            Thread.currentThread().interrupt();
        }
    }

    private String computeReadyDate(LocalDateTime now) {
        return LocalDateUtil.getFormattedDateTimeForMongo(now);
    }

    private String computeExpirationDate(LocalDateTime now) {
        return LocalDateUtil.getFormattedDateTimeForMongo(
            now.plus(this.accessRequestExpirationDelay, this.accessRequestExpirationUnit.toChronoUnit())
        );
    }

    private String computePurgeDate(LocalDateTime now) {
        return LocalDateUtil.getFormattedDateTimeForMongo(
            now.plus(this.accessRequestPurgeDelay, this.accessRequestPurgeUnit.toChronoUnit())
        );
    }

    /**
     * Generates a new random Access Request Id
     * Access Request Id format is {GUID}
     *
     * @return the Access Request Id
     */
    public static String generateAccessRequestId() {
        return GUIDFactory.newGUID().getId();
    }

    /***
     * Validates an Access Request Id format
     *
     * @param accessRequestId the Access Request Id to check
     * @throws IllegalArgumentException if Access Request format is invalid
     */
    private static void checkAccessRequestIdFormat(String accessRequestId) throws IllegalArgumentException {
        if (StringUtils.isEmpty(accessRequestId)) {
            throw new IllegalArgumentException("Invalid accessRequestId '" + accessRequestId + "'. Null or empty.");
        }

        // Parse / validate GUID format
        try {
            GUIDReader.getGUID(accessRequestId);
        } catch (InvalidGuidOperationException e) {
            throw new IllegalArgumentException("Invalid accessRequestId '" + accessRequestId + "'", e);
        }
    }
}
