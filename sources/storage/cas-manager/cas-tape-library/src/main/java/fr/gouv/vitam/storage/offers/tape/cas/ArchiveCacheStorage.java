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
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.alert.AlertService;
import fr.gouv.vitam.common.alert.AlertServiceImpl;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.security.IllegalPathException;
import fr.gouv.vitam.common.security.SafeFileChecker;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.storage.offers.tape.cache.LRUCache;
import fr.gouv.vitam.storage.offers.tape.cache.LRUCacheEntry;
import fr.gouv.vitam.storage.offers.tape.cache.LRUCacheEvictionJudge;

import javax.annotation.concurrent.ThreadSafe;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Cache for archive storage on disk with Least Recently Used (LRU) eviction policy.
 *
 * Archives are stored as files on a cache directory in the following topology {cacheDirectory}/{fileBucketId}/{tarId}.
 * Cache is configured with storage capacity thresholds :
 * - Max storage space : max capacity that cannot be exceeded by archive cache.
 * - An eviction storage space threshold : triggers background delete of old unused archive files.
 * - Safe storage capacity threshold : causes background delete process to stop when enough disk space is available.
 *
 * Typical use would be :
 * - {@code reserveArchiveStorageSpace()} to ensure enough disk space is available
 * - Write data to temporary archive in a dedicated folder (different from cache directory which is managed by the cache), but on same file system partition (to ensure atomic file move is available)
 * - {@code moveArchiveToCache()} to move atomically temporary archive to cache directory and add it to cache OR {@code cancelReservedArchive()} on failure
 * - At any time, try read an archive from disk using the {@code tryReadFile()} method.
 *
 * When initialized, the cache loads all existing archive files from storage directory.
 *
 * This class is Thread-Safe.
 */
@ThreadSafe
public class ArchiveCacheStorage {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ArchiveCacheStorage.class);
    private static final long MB_TO_BYTES = 1_000_000L;

    private final Path cacheDirectory;
    private final BucketTopologyHelper bucketTopologyHelper;
    private final LRUCache<ArchiveCacheEntry> lruCache;
    private final ArchiveCacheEvictionController archiveCacheEvictionController;

    /**
     * @param cacheDirectory the cache storage directory
     * @param bucketTopologyHelper bucket topology helper
     * @param archiveCacheEvictionController controller of archive cache eviction.
     * @param maxStorageSpace max capacity (in bytes) that cannot be exceeded by archive cache.
     * @param evictionStorageSpaceThreshold : storage space capacity (in bytes) that triggers background delete of old unused archive files
     * @param safeStorageSpaceThreshold safe storage space capacity level (in bytes). When enough storage space is available, background cache delete process ends.
     * @throws IllegalPathException if provided cache directory contains unsafe or illegal archive names.
     * @throws IOException if an I/O error is thrown when accessing disk.
     */
    public ArchiveCacheStorage(
        String cacheDirectory,
        BucketTopologyHelper bucketTopologyHelper,
        ArchiveCacheEvictionController archiveCacheEvictionController,
        long maxStorageSpace,
        long evictionStorageSpaceThreshold,
        long safeStorageSpaceThreshold
    ) throws IllegalPathException, IOException {
        this(
            cacheDirectory,
            bucketTopologyHelper,
            archiveCacheEvictionController,
            maxStorageSpace,
            evictionStorageSpaceThreshold,
            safeStorageSpaceThreshold,
            VitamThreadPoolExecutor.getDefaultExecutor(),
            new AlertServiceImpl()
        );
    }

    @VisibleForTesting
    ArchiveCacheStorage(
        String cacheDirectory,
        BucketTopologyHelper bucketTopologyHelper,
        ArchiveCacheEvictionController archiveCacheEvictionController,
        long maxStorageSpace,
        long evictionStorageSpaceThreshold,
        long safeStorageSpaceThreshold,
        Executor executor,
        AlertService alertService
    ) throws IllegalPathException, IOException {
        this.archiveCacheEvictionController = archiveCacheEvictionController;

        // Sanity check
        this.cacheDirectory = SafeFileChecker.checkSafeDirPath(cacheDirectory).toPath();
        this.bucketTopologyHelper = bucketTopologyHelper;

        // Create / initialize LRU cache using current directory file listing
        this.lruCache = createLRUCache(
            maxStorageSpace,
            evictionStorageSpaceThreshold,
            safeStorageSpaceThreshold,
            this::fileEvictionJudgeFactory,
            executor,
            alertService
        );
    }

    private LRUCacheEvictionJudge<ArchiveCacheEntry> fileEvictionJudgeFactory() {
        LOGGER.warn(
            "Preparing archive cache eviction. Max capacity {}MB, Current usage: {}MB",
            this.lruCache.getMaxCapacity() / MB_TO_BYTES,
            this.lruCache.getCurrentCapacity() / MB_TO_BYTES
        );

        LRUCacheEvictionJudge<ArchiveCacheEntry> entryLRUCacheEvictionJudge =
            archiveCacheEvictionController.computeEvictionJudge();

        LOGGER.info("Done preparing archive cache eviction...");

        return entryLRUCacheEvictionJudge;
    }

    private LRUCache<ArchiveCacheEntry> createLRUCache(
        long maxCapacity,
        long evictionCapacity,
        long safeCapacity,
        Supplier<LRUCacheEvictionJudge<ArchiveCacheEntry>> archiveEvictionJudgeFactory,
        Executor evictionExecutor,
        AlertService alertService
    ) throws IllegalPathException, IOException {
        // Directory structure is {cacheDirectory}/{fileBucketId}/{tarId}
        // Initialize cache with maxDepth = 2 for sub-directory/file listing
        try (Stream<Path> pathStream = Files.walk(this.cacheDirectory, 2)) {
            Stream<LRUCacheEntry<ArchiveCacheEntry>> initialFileCacheEntries = pathStream
                .filter(this::filterNonRegularFiles)
                .peek(this::checkNonRootFile)
                .peek(this::checkFileSafety)
                .peek(this::checkFileBucketId)
                .map(this::createFileCacheEntry);

            return new LRUCache<>(
                maxCapacity,
                evictionCapacity,
                safeCapacity,
                archiveEvictionJudgeFactory,
                this::evictFileListener,
                initialFileCacheEntries,
                evictionExecutor,
                alertService
            );
        } catch (RuntimeException e) {
            // Unwrap runtime exceptions
            Throwable cause = e.getCause();
            if (cause instanceof IllegalPathException) {
                throw (IllegalPathException) cause;
            }
            if (cause instanceof IOException) {
                throw (IOException) cause;
            }
            throw e;
        }
    }

    public void reserveArchiveStorageSpace(String fileBucketId, String tarId, long fileSize)
        throws IllegalPathException {
        // Check file name
        Path filePath = checkSafeDirPath(fileBucketId, tarId);

        // Check file existence
        ArchiveCacheEntry archiveCacheEntry = new ArchiveCacheEntry(fileBucketId, tarId);
        if (this.lruCache.containsEntry(archiveCacheEntry)) {
            throw new IllegalArgumentException("Archive '" + archiveCacheEntry + "' already exists in cache.");
        }
        if (this.lruCache.isReservedEntry(archiveCacheEntry)) {
            throw new IllegalArgumentException("Reservation for file '" + archiveCacheEntry + "' already exists.");
        }
        if (Files.exists(filePath)) {
            throw new IllegalArgumentException(
                "File '" + archiveCacheEntry + "' already exists on storage directory " + this.cacheDirectory
            );
        }

        // Append file to cache
        Instant currentTimestamp = getCurrentInstant();
        this.lruCache.reserveEntry(new LRUCacheEntry<>(archiveCacheEntry, fileSize, currentTimestamp));
    }

    public void moveArchiveToCache(Path initialFilePath, String fileBucketId, String tarId)
        throws IllegalPathException, IOException {
        // Check file name
        Path cachedFilePath = checkSafeDirPath(fileBucketId, tarId);

        // Ensure initial file exists
        if (!Files.exists(initialFilePath)) {
            throw new FileNotFoundException("File not exists" + initialFilePath);
        }
        if (!Files.isRegularFile(initialFilePath)) {
            throw new IOException("Not a file" + initialFilePath);
        }

        // Ensure target file does not exist
        if (Files.exists(cachedFilePath)) {
            throw new IOException("File already exists" + cachedFilePath);
        }

        // Ensure file is reserved in cache
        ArchiveCacheEntry archiveCacheEntry = new ArchiveCacheEntry(fileBucketId, tarId);
        if (!this.lruCache.isReservedEntry(archiveCacheEntry)) {
            throw new IllegalArgumentException("File " + archiveCacheEntry + " is not reserved in cache");
        }

        // Check file size
        LRUCacheEntry<ArchiveCacheEntry> reservedEntry = this.lruCache.getReservedEntry(archiveCacheEntry);
        long fileSize = Files.size(initialFilePath);
        if (reservedEntry.getWeight() != fileSize) {
            throw new IllegalArgumentException(
                "File '" +
                archiveCacheEntry +
                "' size " +
                fileSize +
                " does not match reserved file capacity " +
                reservedEntry.getWeight()
            );
        }

        // Atomic move file to cache
        Files.createDirectories(cachedFilePath.getParent());
        Files.move(initialFilePath, cachedFilePath, StandardCopyOption.ATOMIC_MOVE);

        // Append file to cache with last timestamp
        Instant currentTimestamp = getCurrentInstant();
        this.lruCache.updateEntryAccessTimestamp(archiveCacheEntry, currentTimestamp);
        this.lruCache.confirmReservation(archiveCacheEntry);
    }

    public void cancelReservedArchive(String fileBucketId, String tarId) {
        // Check parameters
        ParametersChecker.checkParameter("Missing fileBucketId", fileBucketId);
        ParametersChecker.checkParameter("Missing tarId", tarId);

        // Check reservation
        ArchiveCacheEntry archiveCacheEntry = new ArchiveCacheEntry(fileBucketId, tarId);
        if (!this.lruCache.isReservedEntry(archiveCacheEntry)) {
            throw new IllegalArgumentException("File " + archiveCacheEntry + " is not reserved in cache");
        }

        // Cancel reservation
        this.lruCache.cancelReservation(archiveCacheEntry);
    }

    public Optional<FileInputStream> tryReadArchive(String fileBucketId, String tarId) throws IllegalPathException {
        // Check file name
        Path cachedFilePath = checkSafeDirPath(fileBucketId, tarId);

        // Check file existence
        if (!containsArchive(fileBucketId, tarId)) {
            return Optional.empty();
        }

        try {
            Optional<FileInputStream> inputStream = Optional.of(new FileInputStream(cachedFilePath.toFile()));

            // Update access timestamp
            ArchiveCacheEntry archiveCacheEntry = new ArchiveCacheEntry(fileBucketId, tarId);
            LOGGER.debug("Access to file " + archiveCacheEntry);
            this.lruCache.updateEntryAccessTimestamp(archiveCacheEntry, getCurrentInstant());

            return inputStream;
        } catch (FileNotFoundException e) {
            LOGGER.warn("Could not open file for read. Concurrent purge?", e);
            return Optional.empty();
        }
    }

    public boolean containsArchive(String fileBucketId, String tarId) {
        // Check parameters
        ParametersChecker.checkParameter("Missing fileBucketId", fileBucketId);
        ParametersChecker.checkParameter("Missing tarId", tarId);

        ArchiveCacheEntry archiveCacheEntry = new ArchiveCacheEntry(fileBucketId, tarId);

        return this.lruCache.containsEntry(archiveCacheEntry);
    }

    public boolean isArchiveReserved(String fileBucketId, String tarId) {
        // Check parameters
        ParametersChecker.checkParameter("Missing fileBucketId", fileBucketId);
        ParametersChecker.checkParameter("Missing tarId", tarId);

        ArchiveCacheEntry archiveCacheEntry = new ArchiveCacheEntry(fileBucketId, tarId);

        return this.lruCache.isReservedEntry(archiveCacheEntry);
    }

    public long getMaxStorageSpace() {
        return this.lruCache.getMaxCapacity();
    }

    public long getEvictionStorageSpaceThreshold() {
        return this.lruCache.getEvictionCapacity();
    }

    public long getSafeStorageSpaceThreshold() {
        return this.lruCache.getSafeCapacity();
    }

    public long getCurrentStorageSpaceUsage() {
        return this.lruCache.getCurrentCapacity();
    }

    private Path checkSafeDirPath(String fileBucketId, String tarId) throws IllegalPathException {
        ParametersChecker.checkParameter("Missing fileBucketId", fileBucketId);
        ParametersChecker.checkParameter("Missing tarId", tarId);
        if (!this.bucketTopologyHelper.isValidFileBucketId(fileBucketId)) {
            throw new IllegalArgumentException("Invalid fileBucketId '" + fileBucketId + "'");
        }

        return SafeFileChecker.checkSafeDirPath(this.cacheDirectory.toString(), fileBucketId, tarId).toPath();
    }

    private boolean filterNonRegularFiles(Path filePath) {
        if (!Files.isRegularFile(filePath)) {
            LOGGER.debug("Ignoring non regular file " + filePath);
            return false;
        }
        return true;
    }

    private void checkNonRootFile(Path filePath) {
        Path parentDir = filePath.getParent().toAbsolutePath();
        try {
            if (Files.isSameFile(parentDir, this.cacheDirectory)) {
                throw new IllegalStateException(
                    "Invalid file '" +
                    filePath +
                    "'  at root of cache directory " +
                    this.cacheDirectory +
                    ". Expected {fileBucketId}/{tarId} format"
                );
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void checkFileSafety(Path filePath) {
        try {
            String parentDir = filePath.getParent().getFileName().toString();
            String filename = filePath.getFileName().toString();

            // Check file path safety
            Path safeFilePath = SafeFileChecker.checkSafeFilePath(
                this.cacheDirectory.toString(),
                parentDir,
                filename
            ).toPath();
            if (!Files.isSameFile(safeFilePath, filePath)) {
                throw new IllegalPathException(
                    "Illegal file '" +
                    filePath +
                    "' path. Expected " +
                    this.cacheDirectory +
                    " folder to be its parent folder"
                );
            }
        } catch (IllegalPathException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void checkFileBucketId(Path filePath) {
        String fileBucketId = filePath.getParent().getFileName().toString();

        if (!this.bucketTopologyHelper.isValidFileBucketId(fileBucketId)) {
            throw new IllegalStateException("Unknown fileBucketId " + fileBucketId);
        }
    }

    private LRUCacheEntry<ArchiveCacheEntry> createFileCacheEntry(Path filePath) {
        try {
            String fileBucketId = filePath.getParent().getFileName().toString();
            String tarId = filePath.getFileName().toString();
            BasicFileAttributes attr = Files.readAttributes(filePath, BasicFileAttributes.class);
            Instant lastAccessTime = attr.lastAccessTime().toInstant();
            long size = attr.size();
            return new LRUCacheEntry<>(new ArchiveCacheEntry(fileBucketId, tarId), size, lastAccessTime);
        } catch (IOException e) {
            // Wrap as RuntimeException
            throw new UncheckedIOException(e);
        }
    }

    private void evictFileListener(ArchiveCacheEntry archiveCacheEntry) {
        try {
            // No need for SafeFileChecker checks since filename have been checked when added to cache.

            // Delete file
            LOGGER.info(
                "Deleting unused archive {}/{}",
                archiveCacheEntry.getFileBucketId(),
                archiveCacheEntry.getTarId()
            );
            Path filePath =
                this.cacheDirectory.resolve(archiveCacheEntry.getFileBucketId()).resolve(archiveCacheEntry.getTarId());
            Files.delete(filePath);
        } catch (IOException e) {
            LOGGER.warn(
                "Could not delete file {}/{}" +
                archiveCacheEntry.getFileBucketId() +
                "/" +
                archiveCacheEntry.getTarId() +
                " from " +
                this.cacheDirectory,
                e
            );
        }
    }

    public boolean isCacheEvictionRunning() {
        return this.lruCache.isCacheEvictionRunning();
    }

    private Instant getCurrentInstant() {
        return LocalDateUtil.now().toInstant(ZoneOffset.UTC);
    }
}
