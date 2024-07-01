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

import fr.gouv.vitam.common.collection.CloseableIterator;
import fr.gouv.vitam.common.performance.PerformanceLogger;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryObjectReferentialId;
import fr.gouv.vitam.storage.offers.tape.cache.LRUCacheEvictionJudge;
import fr.gouv.vitam.storage.offers.tape.exception.AccessRequestReferentialException;
import fr.gouv.vitam.storage.offers.tape.exception.ObjectReferentialException;
import org.apache.commons.lang3.time.StopWatch;

import java.util.Set;
import java.util.concurrent.TimeUnit;

public class ArchiveCacheEvictionController {

    private final AccessRequestReferentialRepository accessRequestReferentialRepository;
    private final ObjectReferentialRepository objectReferentialRepository;
    private final BucketTopologyHelper bucketTopologyHelper;
    private final LockManager<ArchiveCacheEntry> archiveCacheEntryLockManager;

    public ArchiveCacheEvictionController(
        AccessRequestReferentialRepository accessRequestReferentialRepository,
        ObjectReferentialRepository objectReferentialRepository,
        BucketTopologyHelper bucketTopologyHelper
    ) {
        this.accessRequestReferentialRepository = accessRequestReferentialRepository;
        this.objectReferentialRepository = objectReferentialRepository;
        this.bucketTopologyHelper = bucketTopologyHelper;
        this.archiveCacheEntryLockManager = new LockManager<>();
    }

    /**
     * Computes an eviction judge that prevents eviction of in-use archives (required by active access requests) or eviction of archives that are non-expirable (to be kept forever in cache).
     *
     * @return An eviction judge that decides if an archive "file-bucket-id/tarId" can be evicted from cache or not.
     */
    public LRUCacheEvictionJudge<ArchiveCacheEntry> computeEvictionJudge() {
        Set<String> inUseArchiveIds = listArchiveIdsRequiredByActiveAccessRequests();

        return archiveCacheEntry -> {
            if (inUseArchiveIds.contains(archiveCacheEntry.getTarId())) {
                return false;
            }

            if (this.archiveCacheEntryLockManager.isLocked(archiveCacheEntry)) {
                return false;
            }

            boolean tarIdRequiredByActiveAccessRequests = bucketTopologyHelper.keepFileBucketIdForeverInCache(
                archiveCacheEntry.getFileBucketId()
            );
            return !tarIdRequiredByActiveAccessRequests;
        };
    }

    private Set<String> listArchiveIdsRequiredByActiveAccessRequests() {
        try {
            Set<String> inUseArchiveIds;

            StopWatch swListObjects = StopWatch.createStarted();
            try (
                CloseableIterator<TapeLibraryObjectReferentialId> objectIdIterator =
                    this.accessRequestReferentialRepository.listObjectIdsForActiveAccessRequests()
            ) {
                swListObjects.stop();
                PerformanceLogger.getInstance()
                    .log(
                        "TAPE_CACHE_EVICTION",
                        "LIST_OBJECT_IDS_REQUIRED_BY_ACCESS_REQUESTS",
                        swListObjects.getTime(TimeUnit.MILLISECONDS)
                    );

                // From objectIds, get archiveIds
                StopWatch swListArchives = StopWatch.createStarted();
                inUseArchiveIds = this.objectReferentialRepository.selectArchiveIdsByObjectIds(objectIdIterator);
                swListArchives.stop();
                PerformanceLogger.getInstance()
                    .log(
                        "TAPE_CACHE_EVICTION",
                        "LIST_ARCHIVE_IDS_REQUIRED_BY_ACCESS_REQUESTS",
                        swListArchives.getTime(TimeUnit.MILLISECONDS)
                    );
            }

            return inUseArchiveIds;
        } catch (AccessRequestReferentialException | ObjectReferentialException e) {
            throw new RuntimeException("Could not list objectIds in use by active Access Requests", e);
        }
    }

    public LockHandle createLock(Set<ArchiveCacheEntry> archiveCacheEntries) {
        return this.archiveCacheEntryLockManager.createLock(archiveCacheEntries);
    }
}
