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
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.retryable.RetryableOnException;
import fr.gouv.vitam.common.retryable.RetryableParameters;
import fr.gouv.vitam.common.security.IllegalPathException;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.storage.engine.common.model.QueueMessageEntity;
import fr.gouv.vitam.storage.engine.common.model.QueueMessageType;
import fr.gouv.vitam.storage.engine.common.model.QueueState;
import fr.gouv.vitam.storage.engine.common.model.TapeCatalog;
import fr.gouv.vitam.storage.engine.common.model.TapeCatalogLabel;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryOnTapeArchiveStorageLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeState;
import fr.gouv.vitam.storage.engine.common.model.WriteOrder;
import fr.gouv.vitam.storage.offers.tape.cas.ArchiveCacheStorage;
import fr.gouv.vitam.storage.offers.tape.cas.ArchiveReferentialRepository;
import fr.gouv.vitam.storage.offers.tape.exception.ArchiveReferentialException;
import fr.gouv.vitam.storage.offers.tape.exception.QueueException;
import fr.gouv.vitam.storage.offers.tape.exception.ReadWriteErrorCode;
import fr.gouv.vitam.storage.offers.tape.exception.ReadWriteException;
import fr.gouv.vitam.storage.offers.tape.exception.TapeCatalogException;
import fr.gouv.vitam.storage.offers.tape.spec.TapeCatalogService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeLibraryService;
import fr.gouv.vitam.storage.offers.tape.utils.LocalFileUtils;
import io.prometheus.client.Histogram;
import org.apache.commons.io.FileUtils;
import org.bson.conversions.Bson;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.exists;
import static com.mongodb.client.model.Filters.or;
import static fr.gouv.vitam.storage.offers.tape.metrics.ReadWriteOrderMetrics.WRITE_ORDER_EXECUTION_DURATION;
import static fr.gouv.vitam.storage.offers.tape.metrics.ReadWriteOrderMetrics.WRITE_ORDER_WAIT_TIME_BEFORE_EXECUTION;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class WriteTask implements Future<ReadWriteResult> {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(WriteTask.class);
    public static final String TAPE_MSG = " [Tape] : ";
    public static final String TAPE_LABEL = "tape-Label-";

    public static final int SLEEP_TIME = 20;
    public static final int CARTRIDGE_RETRY = 1;
    private static final int NB_RETRY = 3;
    private static final int RANDOM_RANGE_SLEEP = 5;

    public final String MSG_PREFIX;
    private final String inputTarPath;
    protected boolean cancelled = false;
    protected boolean done = false;

    private TapeCatalog workerCurrentTape;
    private final ArchiveReferentialRepository archiveReferentialRepository;
    private final TapeLibraryService tapeLibraryService;
    private final TapeCatalogService tapeCatalogService;
    private final ArchiveCacheStorage archiveCacheStorage;
    private final WriteOrder writeOrder;
    private int cartridgeRetry = CARTRIDGE_RETRY;
    private final boolean forceOverrideNonEmptyCartridges;

    public WriteTask(
        WriteOrder writeOrder,
        TapeCatalog workerCurrentTape,
        TapeLibraryService tapeLibraryService,
        TapeCatalogService tapeCatalogService,
        ArchiveReferentialRepository archiveReferentialRepository,
        ArchiveCacheStorage archiveCacheStorage,
        String inputTarPath,
        boolean forceOverrideNonEmptyCartridges
    ) {
        ParametersChecker.checkParameter("WriteOrder param is required.", writeOrder);
        ParametersChecker.checkParameter("TapeLibraryService param is required.", tapeLibraryService);
        ParametersChecker.checkParameter("TapeCatalogService param is required.", tapeCatalogService);
        ParametersChecker.checkParameter(
            "ArchiveReferentialRepository param is required.",
            archiveReferentialRepository
        );
        ParametersChecker.checkParameter("ArchiveCacheStorage param is required.", archiveCacheStorage);
        ParametersChecker.checkParameter("InputTarPath param is required.", inputTarPath);
        this.writeOrder = writeOrder;
        this.workerCurrentTape = workerCurrentTape;
        this.tapeLibraryService = tapeLibraryService;
        this.tapeCatalogService = tapeCatalogService;
        this.archiveReferentialRepository = archiveReferentialRepository;
        this.archiveCacheStorage = archiveCacheStorage;
        this.inputTarPath = inputTarPath;
        this.MSG_PREFIX = String.format(
            "[Library] : %s, [Drive] : %s, ",
            tapeLibraryService.getLibraryIdentifier(),
            tapeLibraryService.getDriveIndex()
        );
        this.forceOverrideNonEmptyCartridges = forceOverrideNonEmptyCartridges;
    }

    @Override
    public ReadWriteResult get() {
        reportWaitTime();

        Histogram.Timer executionTimer = WRITE_ORDER_EXECUTION_DURATION.labels(writeOrder.getBucket()).startTimer();

        final ReadWriteResult readWriteResult = new ReadWriteResult();
        try {
            File file = getWriteOrderFile();

            if (null != workerCurrentTape) {
                if (canWriteOnTape()) {
                    doWrite(file);
                } else {
                    unloadThenLoadTapeAndWrite(file);
                }
            } else {
                loadTapeAndWrite(file);
            }

            retryable().execute(this::updateTarReferential);

            if (writeOrder.getMessageType() == QueueMessageType.WriteBackupOrder) {
                LOGGER.warn(
                    "Backup archive '" +
                    writeOrder.getArchiveId() +
                    " archived to a backup tape with code '" +
                    workerCurrentTape.getCode() +
                    "'"
                );

                // Backup archives are not persisted on cache
                if (!file.delete()) {
                    throw new ReadWriteException(
                        "Could not delete backup archive " + writeOrder.getArchiveId() + " (" + file + ")",
                        ReadWriteErrorCode.KO_ON_DELETE_ARCHIVED_BACKUP
                    );
                }
            } else {
                // Regular (data) archive. Move it to cache.
                moveArchiveToCache(file);
            }

            readWriteResult.setStatus(StatusCode.OK);
            readWriteResult.setOrderState(QueueState.COMPLETED);
        } catch (ReadWriteException e) {
            LOGGER.error("Write task failed", e);
            readWriteResult.setCode(e.getReadWriteErrorCode());
            switch (e.getReadWriteErrorCode()) {
                case KO_LABEL_DISCORDING:
                case KO_LABEL_DISCORDING_NOT_EMPTY_TAPE:
                case KO_UNKNOWN_CURRENT_POSITION:
                    // TODO: 28/03/19 perhaps just rewind and retry
                    // TODO: rewind, goto position file count, retry write file
                    // TODO: ugly fix
                    workerCurrentTape.setTapeState(TapeState.CONFLICT);
                    LOGGER.warn(
                        MSG_PREFIX +
                        TAPE_MSG +
                        workerCurrentTape.getCode() +
                        " is marked as conflict (incident close tape)"
                    );
                    try {
                        retryable().execute(() -> doUpdateTapeCatalog(workerCurrentTape));
                    } catch (ReadWriteException ex) {
                        LOGGER.error(ex);
                    }

                    readWriteResult.setStatus(StatusCode.FATAL);
                    readWriteResult.setOrderState(QueueState.READY);
                    break;
                case KO_ON_WRITE_TO_TAPE:
                    // FIXME: 19/03/19 error while write file in the tape. perhaps timeout ?!!
                    // Mark tape state conflict and retry with new tape
                    if (--cartridgeRetry >= 0) {
                        workerCurrentTape.setTapeState(TapeState.CONFLICT);
                        LOGGER.warn(
                            MSG_PREFIX +
                            TAPE_MSG +
                            workerCurrentTape.getCode() +
                            " is marked as conflict (incident close tape)"
                        );
                        return get();
                    } else {
                        readWriteResult.setStatus(StatusCode.FATAL);
                        readWriteResult.setOrderState(QueueState.READY);
                    }
                    break;
                case KO_ON_END_OF_TAPE:
                    workerCurrentTape.setTapeState(TapeState.FULL);
                    // Re-call get, to unload current tape, load new tape then do write
                    // TODO: 28/03/19 best is to get read orders on this tape before unload
                    // TODO: read may needs rewind that can take a lot of time. unload load tape maybe better and faster than taking read orders
                    return get();
                case KO_ON_GOTO_FILE_COUNT:
                case KO_ON_REWIND_FSF_BSF_TAPE:
                case KO_REWIND_BEFORE_UNLOAD_TAPE:
                // Error maybe IO exception or tape corrupted or timeout
                case TAPE_LOCATION_CONFLICT_ON_LOAD:
                case TAPE_LOCATION_CONFLICT_ON_UNLOAD:
                case TAPE_LOCATION_UNKNOWN:
                // TODO: should a re-init of tape catalog
                case TAPE_NOT_FOUND_IN_CATALOG:
                // demands to load a new tape from external
                case NO_EMPTY_SLOT_FOUND:
                // Someone have manually modified library
                case NULL_CURRENT_TAPE:
                case KO_ON_READ_FROM_TAPE:
                case KO_ON_REWIND_TAPE:
                // drive is open (drive empty) , tape is corrupted ? timeout ? drive is busy?
                case KO_ON_LOAD_TAPE:
                // drive is online (already have a tape loaded)? timeout ? drive is busy?
                case KO_ON_UNLOAD_TAPE:
                // drive is open (drive empty) ? timeout ? drive is busy?
                case KO_ON_READ_LABEL:
                case KO_DB_PERSIST:
                case KO_TAPE_CURRENT_POSITION_GREATER_THAN_FILE_COUNT:
                case KO_ON_STATUS:
                case KO_ON_LOAD_THEN_STATUS:
                case KO_ON_UNLOAD_THEN_STATUS:
                    readWriteResult.setStatus(StatusCode.FATAL);
                    readWriteResult.setOrderState(QueueState.READY);
                    break;
                case FILE_NOT_FOUND:
                // File delete or not generated
                // Mark write order as error state
                case KO_ON_MOVE_TO_CACHE:
                case KO_ON_DELETE_ARCHIVED_BACKUP:
                case INTERNAL_ERROR_SERVER:
                case KO_DRIVE_STATUS_KO_AFTER_WRITE_ERROR:
                default:
                    readWriteResult.setStatus(StatusCode.FATAL);
                    readWriteResult.setOrderState(QueueState.ERROR);
            }
        } finally {
            executionTimer.observeDuration();
        }

        readWriteResult.setCurrentTape(workerCurrentTape);
        return readWriteResult;
    }

    private void moveArchiveToCache(File archiveFile) throws ReadWriteException {
        try {
            // Reserve cache storage space
            archiveCacheStorage.reserveArchiveStorageSpace(
                writeOrder.getFileBucketId(),
                writeOrder.getArchiveId(),
                writeOrder.getSize()
            );

            try {
                // Move archive to cache
                archiveCacheStorage.moveArchiveToCache(
                    archiveFile.toPath(),
                    writeOrder.getFileBucketId(),
                    writeOrder.getArchiveId()
                );
            } catch (Exception e) {
                // On error, cancel reservation
                archiveCacheStorage.cancelReservedArchive(writeOrder.getFileBucketId(), writeOrder.getArchiveId());
                throw e;
            }
        } catch (IllegalPathException | IOException | IllegalStateException e) {
            throw new ReadWriteException(
                MSG_PREFIX +
                TAPE_MSG +
                workerCurrentTape.getCode() +
                ", Error: while moving archive file '" +
                archiveFile +
                "' to archive cache",
                e,
                ReadWriteErrorCode.KO_ON_MOVE_TO_CACHE
            );
        }
    }

    private void updateTarReferential() throws ReadWriteException {
        try {
            TapeLibraryOnTapeArchiveStorageLocation onTapeTarStorageLocation =
                new TapeLibraryOnTapeArchiveStorageLocation(
                    workerCurrentTape.getCode(),
                    workerCurrentTape.getFileCount() - 1
                );

            archiveReferentialRepository.updateLocationToOnTape(writeOrder.getArchiveId(), onTapeTarStorageLocation);
        } catch (ArchiveReferentialException e) {
            throw new ReadWriteException(
                MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode() + ", Error: while update archive referential",
                e,
                ReadWriteErrorCode.KO_DB_PERSIST
            );
        }
    }

    private void unloadThenLoadTapeAndWrite(File file) throws ReadWriteException {
        // Unload current tape
        tapeLibraryService.unloadTape(workerCurrentTape);

        retryable().execute(this::tapeBackToCatalog);

        loadTapeAndWrite(file);
    }

    /**
     * Method helper
     *
     * @param file
     * @throws ReadWriteException
     */
    private void loadTapeAndWrite(File file) throws ReadWriteException {
        // Find tape
        // If tape not found WARN (return TAR to queue and continue)
        // If tape ok load tape to drive
        // Do status to get tape TYPE and some other information (update catalog)
        tryFindTapeCatalogAndLoadIntoDrive();
        // Check if new tape then doWrite(label)
        // doWrite(TAR)
        doWrite(file);
    }

    /**
     * Get file from the write order file path and check that exists
     *
     * @return File
     * @throws ReadWriteException
     */
    private File getWriteOrderFile() throws ReadWriteException {
        File file = new File(inputTarPath, writeOrder.getFilePath());

        if (!file.exists() || !file.isFile()) {
            throw new ReadWriteException(
                MSG_PREFIX +
                TAPE_MSG +
                (workerCurrentTape == null ? "null" : workerCurrentTape.getCode()) +
                " Action : Write, Order: " +
                JsonHandler.unprettyPrint(writeOrder) +
                ", Error: File not found",
                ReadWriteErrorCode.FILE_NOT_FOUND
            );
        }
        return file;
    }

    private void tryFindTapeCatalogAndLoadIntoDrive() throws ReadWriteException {
        // Find tape
        // If tape not found WARN (return TAR to queue and continue)
        workerCurrentTape = loadTapeFromCatalog();

        // If tape found in catalog then load tape into drive
        LOGGER.debug(MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode() + ", Action: load tape");

        tapeLibraryService.loadTape(workerCurrentTape);

        retryable().execute(() -> doUpdateTapeCatalog(workerCurrentTape));

        doCheckTapeLabel();
    }

    /**
     * Load tape from catalog
     *
     * @return Optional of TapeCatalog
     * @throws ReadWriteException
     */
    private TapeCatalog loadTapeFromCatalog() throws ReadWriteException {
        String bucket = writeOrder.getBucket();

        // Find tape catalog with state open (have data)
        Bson query = and(
            eq(TapeCatalog.LIBRARY, tapeLibraryService.getLibraryIdentifier()),
            eq(TapeCatalog.TAPE_STATE, TapeState.OPEN.name()),
            eq(TapeCatalog.BUCKET, bucket)
        );

        try {
            Optional<TapeCatalog> found = tapeCatalogService.receive(query);
            if (found.isPresent()) {
                return found.get();
            } else {
                // Find tape catalog with state empty (new tape)
                query = and(
                    eq(TapeCatalog.LIBRARY, tapeLibraryService.getLibraryIdentifier()),
                    eq(TapeCatalog.TAPE_STATE, TapeState.EMPTY.name()),
                    or(eq(TapeCatalog.BUCKET, bucket), exists(TapeCatalog.BUCKET, false))
                );

                found = tapeCatalogService.receive(query);
                if (found.isPresent()) {
                    return found.get();
                } else {
                    throw new ReadWriteException(
                        MSG_PREFIX +
                        " Action : Load Tape From Catalog, Order: " +
                        JsonHandler.unprettyPrint(writeOrder) +
                        ", Error: no markReady tape found in the catalog with expected bucket and/or remainingSize",
                        ReadWriteErrorCode.TAPE_NOT_FOUND_IN_CATALOG
                    );
                }
            }
        } catch (QueueException e) {
            throw new ReadWriteException(MSG_PREFIX, e);
        }
    }

    /**
     * Check if label of tape catalog match label of loaded tape
     *
     * @throws ReadWriteException
     */
    private void doCheckTapeLabel() throws ReadWriteException {
        // If no label then cartridge is unknown
        if (null == workerCurrentTape.getLabel()) {
            tapeLibraryService.ensureTapeIsEmpty(workerCurrentTape, forceOverrideNonEmptyCartridges);

            workerCurrentTape.setBucket(writeOrder.getBucket());
            retryable().execute(() -> doUpdateTapeCatalog(workerCurrentTape));
        } else {
            tapeLibraryService.checkNonEmptyTapeLabel(workerCurrentTape);
        }
    }

    /**
     * Check if tape and write order are for the same bucket and the drive is not at the end ot tape
     *
     * @return true if can write, false else
     */
    private boolean canWriteOnTape() {
        if (null == workerCurrentTape) {
            return false;
        }

        return (
            TapeState.EMPTY.equals(workerCurrentTape.getTapeState()) ||
            (Objects.equals(workerCurrentTape.getBucket(), writeOrder.getBucket()) &&
                TapeState.OPEN.equals(workerCurrentTape.getTapeState()))
        );
    }

    /**
     * Write writeOrder to tape if tape label is empty
     *
     * @throws ReadWriteException if not success ReadWriteException will be thrown
     */
    private void tryWriteLabelToTape() throws ReadWriteException {
        workerCurrentTape.setBucket(writeOrder.getBucket());

        TapeCatalogLabel objLabel = new TapeCatalogLabel();
        objLabel.setId(workerCurrentTape.getId());
        objLabel.setCode(workerCurrentTape.getCode());
        objLabel.setAlternativeCode(workerCurrentTape.getAlternativeCode());
        objLabel.setBucket(workerCurrentTape.getBucket());
        objLabel.setType(workerCurrentTape.getType());

        File labelFile = null;
        try {
            Path tmpPath = Paths.get(inputTarPath, LocalFileUtils.INPUT_TAR_TMP_FOLDER);
            Files.createDirectories(tmpPath);

            String fileName = TAPE_LABEL + GUIDFactory.newGUID().getId();
            labelFile = tmpPath.resolve(fileName).toFile();

            JsonHandler.writeAsFile(objLabel, labelFile);

            doWriteFileToTape(LocalFileUtils.INPUT_TAR_TMP_FOLDER + "/" + fileName, labelFile.length());

            workerCurrentTape.setLabel(objLabel);

            retryable().execute(() -> doUpdateTapeCatalog(workerCurrentTape));
        } catch (ReadWriteException e) {
            throw e;
        } catch (Exception e) {
            throw new ReadWriteException(MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode(), e);
        } finally {
            FileUtils.deleteQuietly(labelFile);
        }
    }

    private void doWriteFileToTape(String filePath, long writtenBytes) throws ReadWriteException {
        try {
            tapeLibraryService.write(filePath, writtenBytes, workerCurrentTape);
        } catch (ReadWriteException e) {
            if (e.getReadWriteErrorCode() == ReadWriteErrorCode.KO_ON_WRITE_TO_TAPE) {
                // Update TapeCatalog in db to set Conflict state
                retryable().execute(this::tapeBackToCatalog);
            }
            throw e;
        }
    }

    /**
     * Update TapeCatalog with new information
     *
     * @param tapeCatalog
     * @throws ReadWriteException
     */
    private void doUpdateTapeCatalog(TapeCatalog tapeCatalog) throws ReadWriteException {
        try {
            tapeCatalogService.replace(tapeCatalog);
        } catch (TapeCatalogException e) {
            throw new ReadWriteException(
                MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode() + ", Error: while update tape catalog",
                e,
                ReadWriteErrorCode.KO_DB_PERSIST
            );
        }
    }

    private void tapeBackToCatalog() throws ReadWriteException {
        ParametersChecker.checkParameter(
            MSG_PREFIX + ", Error: tape to update in the catalog is null.",
            workerCurrentTape
        );
        try {
            final Map<String, Object> updates = new HashMap<>();

            updates.put(TapeCatalog.CURRENT_LOCATION, workerCurrentTape.getPreviousLocation());
            updates.put(TapeCatalog.TAPE_STATE, workerCurrentTape.getTapeState().name());
            updates.put(QueueMessageEntity.STATE, QueueState.READY.name());

            tapeCatalogService.update(workerCurrentTape.getId(), updates);
        } catch (TapeCatalogException e) {
            throw new ReadWriteException(
                MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode() + ", Error: while update tape catalog",
                e,
                ReadWriteErrorCode.KO_DB_PERSIST
            );
        }
    }

    private RetryableOnException<Void, ReadWriteException> retryable() {
        RetryableParameters parameters = new RetryableParameters(
            NB_RETRY,
            SLEEP_TIME,
            SLEEP_TIME,
            RANDOM_RANGE_SLEEP,
            MILLISECONDS
        );
        return new RetryableOnException<>(parameters);
    }

    /**
     * Write label if possible and the given file to tape
     *
     * @param file
     * @throws ReadWriteException
     */
    private void doWrite(File file) throws ReadWriteException {
        if (null == workerCurrentTape.getLabel()) {
            // Check if new tape then write label
            tryWriteLabelToTape();
        }

        // doWrite(TAR)
        doWriteFileToTape(writeOrder.getFilePath(), file.length());

        retryable().execute(() -> doUpdateTapeCatalog(workerCurrentTape));
    }

    @Override
    public ReadWriteResult get(long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException {
        return CompletableFuture.supplyAsync(this::get, VitamThreadPoolExecutor.getDefaultExecutor()).get(
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
        return cancelled;
    }

    @Override
    public boolean isDone() {
        return done;
    }

    private void reportWaitTime() {
        long waitTimeInSeconds = Duration.between(
            LocalDateUtil.now(),
            LocalDateUtil.parseMongoFormattedDate(writeOrder.getCreated())
        ).get(ChronoUnit.SECONDS);

        WRITE_ORDER_WAIT_TIME_BEFORE_EXECUTION.labels(writeOrder.getBucket()).observe(waitTimeInSeconds);
    }
}
