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
package fr.gouv.vitam.storage.offers.tape.worker.tasks;

import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.server.query.QueryCriteria;
import fr.gouv.vitam.common.database.server.query.QueryCriteriaOperator;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.security.IllegalPathException;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.storage.engine.common.model.QueueState;
import fr.gouv.vitam.storage.engine.common.model.ReadOrder;
import fr.gouv.vitam.storage.engine.common.model.TapeCatalog;
import fr.gouv.vitam.storage.engine.common.model.TapeLocationType;
import fr.gouv.vitam.storage.engine.common.model.TapeState;
import fr.gouv.vitam.storage.offers.tape.cas.AccessRequestManager;
import fr.gouv.vitam.storage.offers.tape.cas.ArchiveCacheStorage;
import fr.gouv.vitam.storage.offers.tape.exception.AccessRequestReferentialException;
import fr.gouv.vitam.storage.offers.tape.exception.QueueException;
import fr.gouv.vitam.storage.offers.tape.exception.ReadWriteErrorCode;
import fr.gouv.vitam.storage.offers.tape.exception.ReadWriteException;
import fr.gouv.vitam.storage.offers.tape.exception.TapeCatalogException;
import fr.gouv.vitam.storage.offers.tape.spec.TapeCatalogService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeLibraryService;
import io.prometheus.client.Histogram;
import org.bson.conversions.Bson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.ne;
import static fr.gouv.vitam.common.model.StatusCode.FATAL;
import static fr.gouv.vitam.common.model.StatusCode.KO;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.storage.offers.tape.metrics.ReadWriteOrderMetrics.READ_ORDER_EXECUTION_DURATION;
import static fr.gouv.vitam.storage.offers.tape.metrics.ReadWriteOrderMetrics.READ_ORDER_WAIT_TIME_BEFORE_EXECUTION;

public class ReadTask implements Future<ReadWriteResult> {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ReadTask.class);
    public static final String TAPE_MSG = " [Tape] : ";
    public static final String TEMP_EXT = ".TMP";

    private final TapeLibraryService tapeLibraryService;
    private final TapeCatalogService tapeCatalogService;
    private final AccessRequestManager accessRequestManager;
    private final ReadOrder readOrder;
    private final String MSG_PREFIX;

    private final ArchiveCacheStorage archiveCacheStorage;

    private TapeCatalog workerCurrentTape;

    protected AtomicBoolean done = new AtomicBoolean(false);

    public ReadTask(
        ReadOrder readOrder,
        TapeCatalog workerCurrentTape,
        TapeLibraryService tapeLibraryService,
        TapeCatalogService tapeCatalogService,
        AccessRequestManager accessRequestManager,
        ArchiveCacheStorage archiveCacheStorage
    ) {
        ParametersChecker.checkParameter("WriteOrder param is required.", readOrder);
        ParametersChecker.checkParameter("TapeLibraryService param is required.", tapeLibraryService);
        ParametersChecker.checkParameter("TapeCatalogService param is required.", tapeCatalogService);
        ParametersChecker.checkParameter("AccessRequestManager param is required.", accessRequestManager);
        ParametersChecker.checkParameter("archiveCacheStorage param is required.", archiveCacheStorage);
        this.readOrder = readOrder;
        this.workerCurrentTape = workerCurrentTape;
        this.tapeLibraryService = tapeLibraryService;
        this.tapeCatalogService = tapeCatalogService;
        this.accessRequestManager = accessRequestManager;
        this.archiveCacheStorage = archiveCacheStorage;
        this.MSG_PREFIX = String.format(
            "[Library] : %s, [Drive] : %s, ",
            tapeLibraryService.getLibraryIdentifier(),
            tapeLibraryService.getDriveIndex()
        );
    }

    @Override
    public ReadWriteResult get() {
        reportWaitTime();

        Histogram.Timer executionTimer = READ_ORDER_EXECUTION_DURATION.labels(readOrder.getBucket()).startTimer();

        try {
            if (null != workerCurrentTape && !workerCurrentTape.getCode().equals(readOrder.getTapeCode())) {
                // Unload current tape
                unloadTape();
            }

            if (workerCurrentTape == null) {
                // Find tape
                // If tape not found WARN (return TAR to queue and continue)
                // If tape ok load tape to drive
                // Do status to get tape TYPE and some other information (update catalog)
                CatalogResponse catalogResponse = getTapeFromCatalog();
                if (!catalogResponse.isOK()) {
                    return new ReadWriteResult(
                        catalogResponse.getStatus(),
                        catalogResponse.getStatus() == StatusCode.FATAL ? QueueState.ERROR : QueueState.READY,
                        workerCurrentTape
                    );
                }

                workerCurrentTape = catalogResponse.getCurrentTape();
                loadTape();

                // Check label
                this.tapeLibraryService.checkNonEmptyTapeLabel(workerCurrentTape);
            }

            readFromTape();
        } catch (TapeCatalogException | QueueException e) {
            LOGGER.error("Read task failed", e);
            // Drive UP, Order Ready
            return new ReadWriteResult(KO, QueueState.READY, workerCurrentTape);
        } catch (ReadWriteException e) {
            LOGGER.error("Read task failed", e);
            switch (e.getReadWriteErrorCode()) {
                // Drive DOWN (FATAL), Order Ready
                case TAPE_LOCATION_CONFLICT_ON_UNLOAD:
                case NO_EMPTY_SLOT_FOUND:
                case KO_REWIND_BEFORE_UNLOAD_TAPE:
                case KO_ON_UNLOAD_THEN_STATUS:
                case KO_ON_UNLOAD_TAPE:
                    return new ReadWriteResult(FATAL, QueueState.READY, workerCurrentTape);
                case KO_TAPE_IS_BUSY:
                    // Drive UP, Order ERROR
                    return new ReadWriteResult(KO, QueueState.READY, workerCurrentTape);
                // Drive DOWN (FATAL), Order ERROR
                case KO_ON_LOAD_THEN_STATUS:
                case KO_ON_LOAD_TAPE:
                    workerCurrentTape = null;
                case KO_ON_GO_TO_POSITION:
                case KO_ON_READ_FROM_TAPE:
                case KO_ON_REWIND_TAPE:
                // Drive UP, Order Ready
                case KO_ON_WRITE_TO_FS:
                case KO_ON_MOVE_TO_CACHE:
                case KO_ON_DELETE_ARCHIVED_BACKUP:
                case TAPE_LOCATION_CONFLICT_ON_LOAD:
                case TAPE_NOT_FOUND_IN_CATALOG:
                case KO_TAPE_IS_OUTSIDE:
                case KO_TAPE_CONFLICT_STATE:
                case KO_DRIVE_STATUS_KO_AFTER_WRITE_ERROR:
                default:
                    return new ReadWriteResult(FATAL, QueueState.ERROR, workerCurrentTape);
            }
        } finally {
            done.set(true);
            executionTimer.observeDuration();
        }

        return new ReadWriteResult(OK, QueueState.COMPLETED, workerCurrentTape);
    }

    @Override
    public ReadWriteResult get(long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException {
        // FIXME : What about background task still running after timeout
        return CompletableFuture.supplyAsync(() -> get(), VitamThreadPoolExecutor.getDefaultExecutor()).get(
            timeout,
            unit
        );
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return done.get();
    }

    private void readFromTape() throws ReadWriteException {
        try {
            if (!archiveCacheStorage.containsArchive(readOrder.getFileBucketId(), readOrder.getFileName())) {
                copyFileFromTapeToCache();
            }

            updateAccessRequestStatus();
        } catch (IOException | IllegalPathException | IllegalStateException e) {
            LOGGER.error(
                MSG_PREFIX +
                TAPE_MSG +
                workerCurrentTape.getCode() +
                " Action : Read, Order: " +
                JsonHandler.unprettyPrint(readOrder) +
                ", Entity: " +
                e
            );
            throw new ReadWriteException(
                MSG_PREFIX +
                TAPE_MSG +
                workerCurrentTape.getCode() +
                " Action : Read, Order: " +
                JsonHandler.unprettyPrint(readOrder) +
                " : Error when writing TAR on file system ",
                e,
                ReadWriteErrorCode.KO_ON_WRITE_TO_FS
            );
        }
    }

    private void copyFileFromTapeToCache() throws IOException, IllegalPathException, ReadWriteException {
        Path tmpArchiveFile = Paths.get(tapeLibraryService.getTmpOutputDirectory())
            .resolve(readOrder.getFileName() + TEMP_EXT)
            .toAbsolutePath();
        Files.deleteIfExists(tmpArchiveFile);

        // Ensure enough disk space is available
        archiveCacheStorage.reserveArchiveStorageSpace(
            readOrder.getFileBucketId(),
            readOrder.getFileName(),
            readOrder.getSize()
        );

        try {
            // Read file from tape
            tapeLibraryService.read(workerCurrentTape, readOrder.getFilePosition(), readOrder.getFileName() + TEMP_EXT);

            // Move file to cache
            archiveCacheStorage.moveArchiveToCache(
                tmpArchiveFile,
                readOrder.getFileBucketId(),
                readOrder.getFileName()
            );
        } catch (Exception e) {
            // Cleanup tmp file & cancel reserved cache space
            Files.deleteIfExists(tmpArchiveFile);
            archiveCacheStorage.cancelReservedArchive(readOrder.getFileBucketId(), readOrder.getFileName());
            throw e;
        }
    }

    /**
     * Get eligible tape from catalog
     *
     * @return
     */
    private CatalogResponse getTapeFromCatalog() throws ReadWriteException, QueueException, TapeCatalogException {
        Bson query = and(
            eq(TapeCatalog.LIBRARY, tapeLibraryService.getLibraryIdentifier()),
            eq(TapeCatalog.CODE, readOrder.getTapeCode()),
            ne(TapeCatalog.TAPE_STATE, TapeState.CONFLICT.name())
        );
        Optional<TapeCatalog> found = tapeCatalogService.receive(query);
        if (found.isEmpty()) {
            List<TapeCatalog> tapes = tapeCatalogService.find(
                List.of(
                    new QueryCriteria(
                        TapeCatalog.LIBRARY,
                        tapeLibraryService.getLibraryIdentifier(),
                        QueryCriteriaOperator.EQ
                    ),
                    new QueryCriteria(TapeCatalog.CODE, readOrder.getTapeCode(), QueryCriteriaOperator.EQ)
                )
            );
            if (tapes.size() == 0) {
                LOGGER.error(
                    MSG_PREFIX +
                    TAPE_MSG +
                    " Action : LoadTapeFromCatalog, Order: " +
                    JsonHandler.unprettyPrint(readOrder) +
                    ", Error: no tape found in the catalog with expected library and/or bucket",
                    ReadWriteErrorCode.TAPE_NOT_FOUND_IN_CATALOG
                );
                throw new ReadWriteException(
                    MSG_PREFIX +
                    TAPE_MSG +
                    " Action : Read, Order: " +
                    JsonHandler.unprettyPrint(readOrder) +
                    " : Unknown tape in catalog ",
                    ReadWriteErrorCode.TAPE_NOT_FOUND_IN_CATALOG
                );
            } else if (tapes.get(0).getTapeState().equals(TapeState.CONFLICT)) {
                LOGGER.error(
                    MSG_PREFIX +
                    TAPE_MSG +
                    " Action : LoadTapeFromCatalog, Order: " +
                    JsonHandler.unprettyPrint(readOrder) +
                    ", Warn: tape is in conflict state",
                    ReadWriteErrorCode.KO_TAPE_CONFLICT_STATE
                );
                throw new ReadWriteException(
                    MSG_PREFIX +
                    TAPE_MSG +
                    " Action : Read, Order: " +
                    JsonHandler.unprettyPrint(readOrder) +
                    " : tape is in conflict state ",
                    ReadWriteErrorCode.KO_TAPE_CONFLICT_STATE
                );
            } else {
                LOGGER.error(
                    MSG_PREFIX +
                    TAPE_MSG +
                    " Action : LoadTapeFromCatalog, Order: " +
                    JsonHandler.unprettyPrint(readOrder) +
                    ", Warn: tape is busy",
                    ReadWriteErrorCode.KO_TAPE_IS_BUSY
                );
                throw new ReadWriteException(
                    MSG_PREFIX +
                    TAPE_MSG +
                    " Action : Read, Order: " +
                    JsonHandler.unprettyPrint(readOrder) +
                    " : tape is busy ",
                    ReadWriteErrorCode.KO_TAPE_IS_BUSY
                );
            }
        }
        if (found.get().getCurrentLocation().getLocationType().equals(TapeLocationType.OUTSIDE)) {
            LOGGER.error(
                MSG_PREFIX +
                TAPE_MSG +
                found.get().getCode() +
                " Action : LoadTapeFromCatalog, Order: " +
                JsonHandler.unprettyPrint(readOrder) +
                ", Error: tape is outside",
                ReadWriteErrorCode.KO_TAPE_IS_OUTSIDE
            );
            throw new ReadWriteException(
                MSG_PREFIX +
                TAPE_MSG +
                found.get().getCode() +
                " Action : Read, Order: " +
                JsonHandler.unprettyPrint(readOrder) +
                " : tape is busy ",
                ReadWriteErrorCode.KO_TAPE_IS_OUTSIDE
            );
        }

        return new CatalogResponse(OK, found.get());
    }

    /**
     * Load current tape to drive
     */
    private void loadTape() throws ReadWriteException, TapeCatalogException {
        tapeLibraryService.loadTape(workerCurrentTape);
        updateCurrentTapeLocation();
    }

    /**
     * Unload tape from  drive
     */
    private void unloadTape() throws TapeCatalogException, ReadWriteException, QueueException {
        tapeLibraryService.unloadTape(workerCurrentTape);
        // update catalog
        updateCurrentTapeLocation();
        // release the tape
        tapeCatalogService.markReady(workerCurrentTape.getId());
        workerCurrentTape = null;
    }

    private void updateCurrentTapeLocation() throws TapeCatalogException {
        Map<String, Object> updates = new HashMap<>();
        updates.put(TapeCatalog.PREVIOUS_LOCATION, workerCurrentTape.getPreviousLocation());
        updates.put(TapeCatalog.CURRENT_LOCATION, workerCurrentTape.getCurrentLocation());
        tapeCatalogService.update(workerCurrentTape.getId(), updates);
    }

    private void updateAccessRequestStatus() {
        try {
            this.accessRequestManager.updateAccessRequestWhenArchiveReady(readOrder.getFileName());
        } catch (AccessRequestReferentialException e) {
            // LOG and continue (non-blocking error, AccessRequestManager will fixup access request statuses periodically if needed).
            LOGGER.warn(
                MSG_PREFIX +
                TAPE_MSG +
                workerCurrentTape.getCode() +
                " Failed to update access requests in DB for archiveId" +
                readOrder.getFileName(),
                e
            );
        }
    }

    private void reportWaitTime() {
        long waitTimeInSeconds = Duration.between(
            LocalDateUtil.now(),
            LocalDateUtil.parseMongoFormattedDate(readOrder.getCreated())
        ).get(ChronoUnit.SECONDS);

        READ_ORDER_WAIT_TIME_BEFORE_EXECUTION.labels(readOrder.getBucket()).observe(waitTimeInSeconds);
    }
}
