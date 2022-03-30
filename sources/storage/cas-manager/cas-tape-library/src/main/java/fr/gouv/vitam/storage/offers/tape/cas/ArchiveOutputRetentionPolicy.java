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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.thread.VitamThreadFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.nio.file.Path;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * This cache is used to purge archive files in output folder.
 * FIXME:   should purge by remaining disk space &lt; 20% for example
 * Or disk space should be large enough
 * The problem if someone read multiple tar file in same time then the risque of full disk is possible
 */
public class ArchiveOutputRetentionPolicy {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ArchiveOutputRetentionPolicy.class);
    private final Cache<String, Path> cache;
    private final long cacheTimeoutInMinutes;
    private final ReadRequestReferentialCleaner requestReferentialCleaner;
    private final Executor cleanUpExecutor = Executors.newFixedThreadPool(1, VitamThreadFactory.getInstance());

    public ArchiveOutputRetentionPolicy(long cacheTimeoutInMinutes,
        ReadRequestReferentialCleaner requestReferentialCleaner) {
        this(cacheTimeoutInMinutes, TimeUnit.MINUTES, 4, requestReferentialCleaner);
    }

    @VisibleForTesting
    public ArchiveOutputRetentionPolicy(long cacheTimeoutInMinutes, TimeUnit timeUnit, int concurrencyLevel,
        ReadRequestReferentialCleaner requestReferentialCleaner) {
        this.cacheTimeoutInMinutes = cacheTimeoutInMinutes;
        this.requestReferentialCleaner = requestReferentialCleaner;
        cache = CacheBuilder.newBuilder()
            .expireAfterAccess(cacheTimeoutInMinutes, timeUnit)
            .concurrencyLevel(concurrencyLevel)
            .removalListener((RemovalListener<String, Path>) removalNotification -> cleanUpExecutor
                .execute(() -> cleanUpExpiredEntry(removalNotification.getKey(), removalNotification.getValue())))
            .build();

        // CleanUp to force call to removalListener (This is needed in case where we have just read and no write operations to cache)
        Executors
            .newScheduledThreadPool(1, VitamThreadFactory.getInstance())
            .scheduleWithFixedDelay(() -> cleanUp(), 0, cacheTimeoutInMinutes + 1, timeUnit);
    }

    private void cleanUpExpiredEntry(String tarFileId, Path filePath) {

        FileUtils.deleteQuietly(filePath.toFile());

        try {
            String tarFileIdWithoutExtension = StringUtils.substringBeforeLast(tarFileId, ".");
            // TODO: 22/08/2019 invalidate many by tar and not by request id
            requestReferentialCleaner.invalidate(tarFileIdWithoutExtension);
        } catch (Exception e) {
            LOGGER.warn(e);
        }

        LOGGER.debug(
            String.format("Remove archive file (%s) from file system.", filePath.toFile().getAbsolutePath()));
    }

    public void put(String archiveId, Path path) {
        cache.put(archiveId, path);
        LOGGER.debug(
            String.format("Add archive file (%s) cache retention in file system.", path.toFile().getAbsolutePath()));
    }

    public Path get(String archiveId) {
        LOGGER.debug(
            String.format("Access to archive file (%s) from cache retention in file system.", archiveId));
        return cache.getIfPresent(archiveId);
    }

    public void invalidate(String archiveId) {
        LOGGER.debug(
            String.format("Remove archive file (%s) from cache retention in file system.", archiveId));
        cache.invalidate(archiveId);
    }

    public void cleanUp() {
        cache.cleanUp();
        // RequestReferentialCleaner
        try {
            requestReferentialCleaner.cleanUp();
        } catch (Exception e) {
            LOGGER.error(e);
        }
    }

    public long getCacheTimeoutInMinutes() {
        return cacheTimeoutInMinutes;
    }
}
