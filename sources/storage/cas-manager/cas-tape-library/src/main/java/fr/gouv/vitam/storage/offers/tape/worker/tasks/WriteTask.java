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
package fr.gouv.vitam.storage.offers.tape.worker.tasks;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.storage.engine.common.model.QueueMessageEntity;
import fr.gouv.vitam.storage.engine.common.model.QueueMessageType;
import fr.gouv.vitam.storage.engine.common.model.QueueState;
import fr.gouv.vitam.storage.engine.common.model.TapeCatalog;
import fr.gouv.vitam.storage.engine.common.model.TapeCatalogLabel;
import fr.gouv.vitam.storage.engine.common.model.TapeLibraryOnTapeTarStorageLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeLocationType;
import fr.gouv.vitam.storage.engine.common.model.TapeState;
import fr.gouv.vitam.storage.engine.common.model.WriteOrder;
import fr.gouv.vitam.storage.offers.tape.cas.TarReferentialRepository;
import fr.gouv.vitam.storage.offers.tape.dto.TapeDriveSpec;
import fr.gouv.vitam.storage.offers.tape.dto.TapeDriveStatus;
import fr.gouv.vitam.storage.offers.tape.dto.TapeResponse;
import fr.gouv.vitam.storage.offers.tape.exception.QueueException;
import fr.gouv.vitam.storage.offers.tape.exception.ReadWriteErrorCode;
import fr.gouv.vitam.storage.offers.tape.exception.ReadWriteException;
import fr.gouv.vitam.storage.offers.tape.exception.TapeCatalogException;
import fr.gouv.vitam.storage.offers.tape.exception.TarReferentialException;
import fr.gouv.vitam.storage.offers.tape.retry.Retry;
import fr.gouv.vitam.storage.offers.tape.spec.TapeCatalogService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeDriveService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeLoadUnloadService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeRobotPool;
import fr.gouv.vitam.storage.offers.tape.spec.TapeRobotService;
import fr.gouv.vitam.storage.offers.tape.utils.LocalFileUtils;
import org.apache.commons.io.FileUtils;
import org.bson.conversions.Bson;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

public class WriteTask implements Future<ReadWriteResult> {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(WriteTask.class);
    public static final String TAPE_MSG = " [Tape] : ";
    public static final String TAPE_LABEL = "tape-Label-";

    public static final long SLEEP_TIME = 20l;
    public static final int CARTRIDGE_RETRY = 1;
    public static final int MAX_ATTEMPTS = 3;
    public static final int ATTEMPTS_SLEEP_IN_SECONDS = 10;

    public final String MSG_PREFIX;
    private final String inputTarPath;
    protected boolean cancelled = false;
    protected boolean done = false;

    private TapeCatalog workerCurrentTape;
    private final TarReferentialRepository tarReferentialRepository;
    private final TapeRobotPool tapeRobotPool;
    private final TapeDriveService tapeDriveService;
    private final TapeCatalogService tapeCatalogService;
    private final WriteOrder writeOrder;
    private boolean retryEnabled = true;
    private int cartridgeRetry = CARTRIDGE_RETRY;
    private final boolean forceOverrideNonEmptyCartridges;

    public WriteTask(
        WriteOrder writeOrder, TapeCatalog workerCurrentTape, TapeRobotPool tapeRobotPool,
        TapeDriveService tapeDriveService, TapeCatalogService tapeCatalogService,
        TarReferentialRepository tarReferentialRepository, String inputTarPath,
        boolean forceOverrideNonEmptyCartridges) {
        ParametersChecker.checkParameter("WriteOrder param is required.", writeOrder);
        ParametersChecker.checkParameter("TapeRobotPool param is required.", tapeRobotPool);
        ParametersChecker.checkParameter("TapeDriveService param is required.", tapeDriveService);
        ParametersChecker.checkParameter("TapeCatalogService param is required.", tapeCatalogService);
        ParametersChecker.checkParameter("TarReferentialRepository param is required.", tarReferentialRepository);
        this.writeOrder = writeOrder;
        this.workerCurrentTape = workerCurrentTape;
        this.tapeRobotPool = tapeRobotPool;
        this.tapeDriveService = tapeDriveService;
        this.tapeCatalogService = tapeCatalogService;
        this.tarReferentialRepository = tarReferentialRepository;
        this.inputTarPath = inputTarPath;
        this.MSG_PREFIX = String.format("[Library] : %s, [Drive] : %s, ", tapeRobotPool.getLibraryIdentifier(),
            tapeDriveService.getTapeDriveConf().getIndex());
        this.forceOverrideNonEmptyCartridges = forceOverrideNonEmptyCartridges;
    }

    @Override
    public ReadWriteResult get() {

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

            withRetryDoUpdateTarReferential(file);

            readWriteResult.setStatus(StatusCode.OK);
            readWriteResult.setOrderState(QueueState.COMPLETED);

        } catch (ReadWriteException e) {
            LOGGER.error(e);
            readWriteResult.setCode(e.getReadWriteErrorCode());
            switch (e.getReadWriteErrorCode()) {
                case KO_LABEL_DISCORDING:
                case KO_LABEL_DISCORDING_NOT_EMPTY_TAPE:
                case KO_UNKNOWN_CURRENT_POSITION:
                    // TODO: 28/03/19 perhaps just rewind and retry
                    // TODO: rewind, goto position file count, retry write file
                    // TODO: ugly fix
                    workerCurrentTape.setTapeState(TapeState.CONFLICT);
                    LOGGER.warn(MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode() +
                        " is marked as conflict (incident close tape)");
                    try {
                        withRetryDoUpdateTapeCatalog(workerCurrentTape);
                    } catch (ReadWriteException ex) {
                        LOGGER.error(ex);
                    }

                    readWriteResult.setStatus(StatusCode.FATAL);
                    readWriteResult.setOrderState(QueueState.READY);
                    break;
                case KO_ON_WRITE_TO_TAPE:
                    // FIXME: 19/03/19 error while write file in the tape. perhaps timeout ?!!
                    // After retry write -> status -> rewind -> goToPosition -> write
                    // Mark tape state conflict and retry with new tape
                    if (--cartridgeRetry >= 0) {
                        workerCurrentTape.setTapeState(TapeState.CONFLICT);
                        LOGGER.warn(MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode() +
                            " is marked as conflict (incident close tape)");
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
                case TAPE_LOCATION_CONFLICT:
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
                case INTERNAL_ERROR_SERVER:
                default:
                    readWriteResult.setStatus(StatusCode.FATAL);
                    readWriteResult.setOrderState(QueueState.ERROR);
            }
        }

        readWriteResult.setCurrentTape(workerCurrentTape);
        return readWriteResult;
    }

    private void withRetryDoUpdateTarReferential(File file) throws ReadWriteException {

        Retry.Delegate delegate = () -> {
            updateTarReferential(file);
            return true;
        };

        withRetry(delegate);
    }


    private void updateTarReferential(File file) throws ReadWriteException {
        try {
            TapeLibraryOnTapeTarStorageLocation onTapeTarStorageLocation =
                new TapeLibraryOnTapeTarStorageLocation(workerCurrentTape.getCode(),
                    workerCurrentTape.getFileCount() - 1);

            tarReferentialRepository.updateLocationToOnTape(writeOrder.getTarId(), onTapeTarStorageLocation);

            FileUtils.deleteQuietly(file);

        } catch (TarReferentialException e) {
            throw new ReadWriteException(
                MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode() + ", Error: while update tar referential", e,
                ReadWriteErrorCode.KO_DB_PERSIST);
        }
    }

    private void unloadThenLoadTapeAndWrite(File file) throws ReadWriteException {
        // Unload current tape
        doUnloadTape();

        withRetryTapeBackToCatalog();

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
        if (tryFindTapeCatalogAndLoadIntoDrive()) {
            // Check if new tape then doWrite(label)
            // doWrite(TAR)
            doWrite(file);
        }
    }

    /**
     * Get file from the write order file path and check that exists
     *
     * @return File
     * @throws ReadWriteException
     */
    private File getWriteOrderFile() throws ReadWriteException {
        File file = new File(inputTarPath, writeOrder.getFilePath());

        if (!file.exists()) {
            throw new ReadWriteException(
                MSG_PREFIX + TAPE_MSG + (workerCurrentTape == null ? "null" : workerCurrentTape.getCode()) +
                    " Action : Write, Order: " + JsonHandler.unprettyPrint(writeOrder) + ", Error: File not found",
                ReadWriteErrorCode.FILE_NOT_FOUND);
        }
        return file;
    }

    /**
     * @return true if tape loaded into drive, false else
     * @throws ReadWriteException
     */
    private boolean tryFindTapeCatalogAndLoadIntoDrive()
        throws ReadWriteException {
        // Find tape
        // If tape not found WARN (return TAR to queue and continue)
        workerCurrentTape = loadTapeFromCatalog();

        // If tape found in catalog then load tape into drive
        LOGGER.debug(MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode() + ", Action: load tape");

        doLoadTape();

        withRetryDoUpdateTapeCatalog(workerCurrentTape);

        doRewindTape(ReadWriteErrorCode.KO_ON_REWIND_TAPE);

        doCheckTapeLabel();


        return true;
    }

    /**
     * Check if label of tape catalog match label of loaded tape
     *
     * @throws ReadWriteException
     */
    private void doCheckTapeLabel() throws ReadWriteException {

        // If no label then cartridge is unknown
        if (null == workerCurrentTape.getLabel()) {

            // Check empty tape
            TapeResponse moveResponse = tapeDriveService.getDriveCommandService().goToPosition(1);
            if (moveResponse.isOK()) {

                if (this.forceOverrideNonEmptyCartridges) {

                    LOGGER.warn("OVERRIDING NON EMPTY CARTRIDGE " + workerCurrentTape.getCode());
                    TapeResponse rewindResponse = tapeDriveService.getDriveCommandService().rewind();

                    if (!rewindResponse.isOK()) {
                        throw new ReadWriteException(MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode() +
                            " Action : Force override non empty tape, Order: " + JsonHandler.unprettyPrint(writeOrder) +
                            ", Error: Could not rewind for force empty cartridge overriding",
                            ReadWriteErrorCode.KO_REWIND_BEFORE_FORCE_OVERRIDE_NON_EMPTY_TAPE, rewindResponse);
                    }

                } else {

                    workerCurrentTape.setCurrentPosition(workerCurrentTape.getCurrentPosition() + 1);
                    throw new ReadWriteException(MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode() +
                        " Action : Is Tape Empty, Order: " + JsonHandler.unprettyPrint(writeOrder) +
                        ", Error: Tape not empty but tape catalog is empty",
                        ReadWriteErrorCode.KO_LABEL_DISCORDING_NOT_EMPTY_TAPE, moveResponse);
                }
            }

            // Do status to get tape TYPE and some other information (update catalog)
            LOGGER.debug(
                MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode() + ", Action: drive status");
            TapeDriveSpec driveStatus = doDriveStatus(ReadWriteErrorCode.KO_ON_STATUS);

            workerCurrentTape.setType(driveStatus.getCartridge());
            workerCurrentTape.setWorm(driveStatus.getDriveStatuses().contains(TapeDriveStatus.WR_PROT));
            workerCurrentTape.setBucket(writeOrder.getBucket());

            withRetryDoUpdateTapeCatalog(workerCurrentTape);

        } else {

            // Read Label from tape
            File labelFile = null;
            try {
                labelFile = File.createTempFile(TAPE_LABEL, GUIDFactory.newGUID().getId());

                TapeResponse readStatus =
                    tapeDriveService.getReadWriteService(TapeDriveService.ReadWriteCmd.DD)
                        .readFromTape(labelFile.getAbsolutePath());

                if (!readStatus.isOK()) {
                    throw new ReadWriteException(MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode() +
                        " Action : Read from tape, Order: " + JsonHandler.unprettyPrint(writeOrder) +
                        ", Entity: " + JsonHandler.unprettyPrint(readStatus.getEntity()),
                        ReadWriteErrorCode.KO_ON_READ_LABEL, readStatus);
                }

                final TapeCatalogLabel tapeLabel = JsonHandler.getFromFile(labelFile, TapeCatalogLabel.class);
                final TapeCatalogLabel tapeCatalogLabel = workerCurrentTape.getLabel();

                if (tapeLabel == null || !Objects.equals(tapeLabel.getId(), tapeCatalogLabel.getId())) {
                    throw new ReadWriteException(MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode() +
                        " Action : Check tape label, Order: " + JsonHandler.unprettyPrint(writeOrder) +
                        ", Entity: " + JsonHandler.unprettyPrint(readStatus.getEntity()),
                        ReadWriteErrorCode.KO_LABEL_DISCORDING, readStatus);
                }

                workerCurrentTape.setCurrentPosition(1);
            } catch (Exception e) {
                if (e instanceof ReadWriteException) {
                    throw (ReadWriteException) e;
                }
                throw new ReadWriteException(MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode(), e);
            } finally {
                labelFile.delete();
            }

        }
    }

    /**
     * Write label if possible and the given file to tape
     *
     * @param file
     * @throws ReadWriteException
     */
    private void doWrite(File file) throws ReadWriteException {
        if (null == workerCurrentTape) {
            throw new ReadWriteException(
                MSG_PREFIX + ", Error: can't write, current tape is null.", ReadWriteErrorCode.NULL_CURRENT_TAPE);
        }
        if (!workerCurrentTape.getFileCount().equals(workerCurrentTape.getCurrentPosition())) {
            if (workerCurrentTape.getFileCount() < workerCurrentTape.getCurrentPosition()) {
                throw new ReadWriteException(
                    MSG_PREFIX + ", Error: current position must be <= to fileCount.",
                    ReadWriteErrorCode.KO_TAPE_CURRENT_POSITION_GREATER_THAN_FILE_COUNT);
            }

            Integer positionSeek = workerCurrentTape.getFileCount() - workerCurrentTape.getCurrentPosition();

            goToPosition(positionSeek, ReadWriteErrorCode.KO_ON_GOTO_FILE_COUNT);
        }

        if (null == workerCurrentTape.getLabel()) {
            // Check if new tape then write label
            tryWriteLabelToTape();
        }

        // doWrite(TAR)
        doWriteFileToTape(file);
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

        return TapeState.EMPTY.equals(workerCurrentTape.getTapeState()) ||
            (Objects.equals(workerCurrentTape.getBucket(), writeOrder.getBucket()) &&
                TapeState.OPEN.equals(workerCurrentTape.getTapeState()));
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
            eq(TapeCatalog.LIBRARY, tapeRobotPool.getLibraryIdentifier()),
            eq(TapeCatalog.TAPE_STATE, TapeState.OPEN.name()),
            eq(TapeCatalog.BUCKET, bucket)
        );

        try {
            Optional<TapeCatalog> found = tapeCatalogService.receive(query, QueueMessageType.TapeCatalog);
            if (found.isPresent()) {
                return found.get();
            } else {
                // Find tape catalog with state empty (new tape)
                query = and(
                    eq(TapeCatalog.LIBRARY, tapeRobotPool.getLibraryIdentifier()),
                    eq(TapeCatalog.TAPE_STATE, TapeState.EMPTY.name()),
                    or(
                        eq(TapeCatalog.BUCKET, bucket),
                        exists(TapeCatalog.BUCKET, false)
                    )
                );

                found = tapeCatalogService.receive(query, QueueMessageType.TapeCatalog);
                if (found.isPresent()) {
                    return found.get();
                } else {
                    throw new ReadWriteException(MSG_PREFIX +
                        " Action : Load Tape From Catalog, Order: " + JsonHandler.unprettyPrint(writeOrder) +
                        ", Error: no markReady tape found in the catalog with expected bucket and/or remainingSize",
                        ReadWriteErrorCode.TAPE_NOT_FOUND_IN_CATALOG);
                }
            }
        } catch (QueueException e) {
            throw new ReadWriteException(MSG_PREFIX, e);
        }
    }

    /**
     * @return TapeDriveState
     * @throws ReadWriteException
     */
    private TapeDriveSpec doDriveStatus(ReadWriteErrorCode readWriteErrorCode) throws ReadWriteException {
        int retry = 3;

        TapeDriveSpec driveStatus = tapeDriveService.getDriveCommandService().status();

        retry--;

        while (retry != 0 && !driveStatus.isOK()) {
            LOGGER.error(MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode() +
                    " Action : drive status, Entity: " + JsonHandler.unprettyPrint(driveStatus.getEntity()),
                readWriteErrorCode, driveStatus);

            try {
                Thread.sleep(SLEEP_TIME);
            } catch (InterruptedException e) {
                SysErrLogger.FAKE_LOGGER.ignoreLog(e);
            }

            retry--;

            driveStatus = tapeDriveService.getDriveCommandService().status();
        }

        if (!driveStatus.isOK()) {
            throw new ReadWriteException(MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode() +
                " Action : drive status, Entity: " + JsonHandler.unprettyPrint(driveStatus.getEntity()),
                readWriteErrorCode, (TapeResponse) driveStatus);
        }

        return driveStatus;
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

            long fileSize = labelFile.length();

            String relativeFilePath = LocalFileUtils.INPUT_TAR_TMP_FOLDER + "/" + fileName;
            TapeResponse response = tapeDriveService.getReadWriteService(TapeDriveService.ReadWriteCmd.DD)
                .writeToTape(relativeFilePath);

            if (!response.isOK()) {
                LOGGER.error(MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode() +
                    " Action : Write, Order: " + JsonHandler.unprettyPrint(writeOrder) + ", Entity: " +
                    JsonHandler.unprettyPrint(response.getEntity()));

                retryWriteToTape(relativeFilePath, 2, response);
            }

            workerCurrentTape.setFileCount(1);
            workerCurrentTape.setCurrentPosition(1);
            workerCurrentTape.setLabel(objLabel);
            workerCurrentTape.setWrittenBytes(workerCurrentTape.getWrittenBytes() + fileSize);
            workerCurrentTape.setTapeState(TapeState.OPEN);

            withRetryDoUpdateTapeCatalog(workerCurrentTape);

        } catch (Exception e) {
            if (e instanceof ReadWriteException) {
                throw (ReadWriteException) e;
            }
            throw new ReadWriteException(MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode(), e);
        } finally {
            labelFile.delete();
        }
    }

    /**
     * Write writeOrder to tape and update currentTape information
     *
     * @throws ReadWriteException if not success ReadWriteException will be thrown
     */
    private void doWriteFileToTape(File file) throws ReadWriteException {
        try {
            // TODO: 20/03/19 make tape write lock to true
            TapeResponse response = tapeDriveService.getReadWriteService(TapeDriveService.ReadWriteCmd.DD)
                .writeToTape(writeOrder.getFilePath());

            if (!response.isOK()) {
                LOGGER.error(MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode() +
                    " Action : Write, Order: " + JsonHandler.unprettyPrint(writeOrder) + ", Entity: " +
                    JsonHandler.unprettyPrint(response.getEntity()));

                // Do status and check if end of tape
                TapeDriveSpec status = doDriveStatus(ReadWriteErrorCode.KO_UNKNOWN_CURRENT_POSITION);

                if (status.isEndOfTape()) {
                    throw new ReadWriteException(MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode() +
                        " Action : Write, Order: " + JsonHandler.unprettyPrint(writeOrder) + ", Drive Status: " +
                        JsonHandler.unprettyPrint(status.getEntity()) + ", Error: End of tape",
                        ReadWriteErrorCode.KO_ON_END_OF_TAPE, response);
                }

                if (!workerCurrentTape.isWorm() || retryEnabled) {
                    retryWriteToTape(writeOrder.getFilePath(), 2, response);
                } else {
                    workerCurrentTape.setTapeState(TapeState.CONFLICT);
                    withRetryTapeBackToCatalog();

                    throw new ReadWriteException(MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode() +
                        " Action : Write, Order: " + JsonHandler.unprettyPrint(writeOrder) + ", Entity: " +
                        JsonHandler.unprettyPrint(response.getEntity()), ReadWriteErrorCode.KO_ON_WRITE_TO_TAPE,
                        response);
                }
            }

            cartridgeRetry = CARTRIDGE_RETRY;
            workerCurrentTape.setFileCount(workerCurrentTape.getFileCount() + 1);
            workerCurrentTape.setCurrentPosition(workerCurrentTape.getFileCount());
            workerCurrentTape.setWrittenBytes(workerCurrentTape.getWrittenBytes() + file.length());
            workerCurrentTape.setTapeState(TapeState.OPEN);

            withRetryDoUpdateTapeCatalog(workerCurrentTape);

        } catch (Exception e) {
            if (e instanceof ReadWriteException) {
                throw (ReadWriteException) e;
            }
            throw new ReadWriteException(MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode(), e);
        }
    }

    /**
     * Retry write to tape
     *
     * @param filePath
     * @param nbRetry
     * @param response
     * @return TapeResponse
     * @throws ReadWriteException
     */
    private void retryWriteToTape(String filePath, int nbRetry, TapeResponse response)
        throws ReadWriteException {

        while (nbRetry != 0 && !response.isOK()) {

            nbRetry--;

            // Rewind
            doRewindTape(ReadWriteErrorCode.KO_UNKNOWN_CURRENT_POSITION);

            // FSF
            goToPosition(workerCurrentTape.getFileCount(), ReadWriteErrorCode.KO_UNKNOWN_CURRENT_POSITION);


            try {
                Thread.sleep(SLEEP_TIME);
            } catch (InterruptedException e) {
                SysErrLogger.FAKE_LOGGER.ignoreLog(e);
            }

            response = tapeDriveService.getReadWriteService(TapeDriveService.ReadWriteCmd.DD)
                .writeToTape(filePath);

            if (!response.isOK()) {
                LOGGER.error(MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode() +
                    " Action : Write, Order: " + JsonHandler.unprettyPrint(writeOrder) + ", Entity: " +
                    JsonHandler.unprettyPrint(response.getEntity()));
            }
        }

        if (!response.isOK()) {
            workerCurrentTape.setTapeState(TapeState.CONFLICT);
            withRetryTapeBackToCatalog();
            throw new ReadWriteException(MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode() +
                " Action : Write, Order: " + JsonHandler.unprettyPrint(writeOrder) + ", Entity: " +
                JsonHandler.unprettyPrint(response.getEntity()), ReadWriteErrorCode.KO_ON_WRITE_TO_TAPE, response);
        }
    }

    /**
     * fsf command
     *
     * @throws ReadWriteException
     */
    private void goToPosition(Integer position, ReadWriteErrorCode readWriteErrorCode) throws ReadWriteException {
        TapeResponse fsfResponse = tapeDriveService.getDriveCommandService()
            .goToPosition(position);

        if (!fsfResponse.isOK()) {
            throw new ReadWriteException(MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode() +
                " Action : FSF goto position Error " + fsfResponse.getErrorCode() + ", Order: " +
                JsonHandler.unprettyPrint(writeOrder) + ", Entity: " +
                JsonHandler.unprettyPrint(fsfResponse.getEntity()),
                readWriteErrorCode,
                fsfResponse);
        }
        // Update current position only if fsf command success
        workerCurrentTape.setCurrentPosition(workerCurrentTape.getFileCount());
    }

    /**
     * rewind command
     *
     * @throws ReadWriteException
     */
    private void doRewindTape(ReadWriteErrorCode readWriteErrorCode) throws ReadWriteException {
        // Rewind
        TapeResponse rewindResponse =
            tapeDriveService.getDriveCommandService().rewind();

        if (!rewindResponse.isOK()) {
            throw new ReadWriteException(MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode() +
                " Action : Rewind Tape, Order: " + JsonHandler.unprettyPrint(writeOrder) +
                ", Entity: " +
                JsonHandler.unprettyPrint(rewindResponse.getEntity()), readWriteErrorCode,
                rewindResponse);
        }
        // if rewind success then set current position to 0
        workerCurrentTape.setCurrentPosition(0);

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
                MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode() + ", Error: while update tape catalog", e,
                ReadWriteErrorCode.KO_DB_PERSIST);
        }
    }

    private void withRetryDoUpdateTapeCatalog(TapeCatalog tapeCatalog) throws ReadWriteException {

        Retry.Delegate delegate = () -> {
            doUpdateTapeCatalog(tapeCatalog);
            return true;
        };

        withRetry(delegate);
    }

    private void tapeBackToCatalog() throws ReadWriteException {
        ParametersChecker
            .checkParameter(MSG_PREFIX + ", Error: tape to update in the catalog is null.", workerCurrentTape);
        try {
            final Map<String, Object> updates = new HashMap<>();

            updates.put(TapeCatalog.CURRENT_LOCATION, workerCurrentTape.getPreviousLocation());
            updates.put(TapeCatalog.TAPE_STATE, workerCurrentTape.getTapeState().name());
            updates.put(QueueMessageEntity.STATE, QueueState.READY.name());

            tapeCatalogService.update(workerCurrentTape.getId(), updates);
        } catch (TapeCatalogException e) {
            throw new ReadWriteException(
                MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode() + ", Error: while update tape catalog", e,
                ReadWriteErrorCode.KO_DB_PERSIST);
        }

    }

    private void withRetryTapeBackToCatalog() throws ReadWriteException {

        Retry.Delegate delegate = () -> {
            tapeBackToCatalog();
            return true;
        };

        withRetry(delegate);
    }


    private void withRetry(Retry.Delegate<?> delegate) throws ReadWriteException {
        try {
            new Retry(MAX_ATTEMPTS, ATTEMPTS_SLEEP_IN_SECONDS).execute(delegate);
        } catch (Exception e) {
            if (e instanceof ReadWriteException) {
                throw (ReadWriteException) e;
            }
            throw new ReadWriteException(e);
        }
    }

    /**
     * Load current tape to drive
     * This method should be called after unloadTape success
     * At this point drive should not have any tape on
     *
     * @throws ReadWriteException if not success ReadWriteException will be thrown
     */
    private void doLoadTape() throws ReadWriteException {
        ParametersChecker
            .checkParameter(
                MSG_PREFIX + ", Error: tape to load is null. please get markReady tape from catalog",
                workerCurrentTape);

        Integer driveIndex = tapeDriveService.getTapeDriveConf().getIndex();
        Integer slotIndex;
        if (null != workerCurrentTape.getPreviousLocation()) {
            slotIndex = workerCurrentTape.getPreviousLocation().getIndex();
        } else {
            throw new ReadWriteException(MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode() +
                ", Error: tape current location is null. please update catalog",
                ReadWriteErrorCode.TAPE_LOCATION_CONFLICT);
        }

        try {
            TapeRobotService tapeRobotService = tapeRobotPool.checkoutRobotService();

            try {
                TapeLoadUnloadService loadUnloadService = tapeRobotService.getLoadUnloadService();

                TapeResponse response = loadUnloadService.loadTape(slotIndex, driveIndex);

                if (!response.isOK()) {

                    TapeDriveSpec status = doDriveStatus(ReadWriteErrorCode.KO_ON_LOAD_THEN_STATUS);

                    if (!status.driveHasTape()) {

                        // Retry once
                        response = loadUnloadService.loadTape(slotIndex, driveIndex);

                        if (!response.isOK()) {
                            throw new ReadWriteException(
                                MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode() + ", Action : load, Entity: " +
                                    JsonHandler.unprettyPrint(response.getEntity()), ReadWriteErrorCode.KO_ON_LOAD_TAPE,
                                response);
                        }
                    }
                }

                workerCurrentTape.setCurrentLocation(
                    new TapeLocation(tapeDriveService.getTapeDriveConf().getIndex(), TapeLocationType.DRIVE));

            } finally {
                tapeRobotPool.pushRobotService(tapeRobotService);
            }
        } catch (InterruptedException e) {
            throw new ReadWriteException(MSG_PREFIX + ", Error: ", e);
        }
    }

    /**
     * Unload tape from  drive
     *
     * @throws ReadWriteException if not success ReadWriteException will be thrown
     */
    private void doUnloadTape() throws ReadWriteException {
        ParametersChecker.checkParameter(MSG_PREFIX + ", Error: tape to unload is null.", workerCurrentTape);

        Integer driveIndex = tapeDriveService.getTapeDriveConf().getIndex();
        Integer slotIndex = null;

        if (null != workerCurrentTape.getPreviousLocation()) {
            switch (workerCurrentTape.getPreviousLocation().getLocationType()) {
                case SLOT:
                case IMPORTEXPORT:
                    slotIndex = workerCurrentTape.getPreviousLocation().getIndex();
                    break;
                case DRIVE:
                case OUTSIDE:
                    throw new ReadWriteException(MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode() +
                        ", Error: previous location should no be in drive",
                        ReadWriteErrorCode.TAPE_LOCATION_CONFLICT);
                default:
                    throw new IllegalArgumentException(
                        MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode() + ", Error: location type not implemented");
            }

        } else {
            // TODO: 28/03/19   slotIndex = findEmptySlot();
        }

        if (null == slotIndex) {
            throw new ReadWriteException(MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode() +
                ", Error : no empty slot found => cannot unload tape", ReadWriteErrorCode.NO_EMPTY_SLOT_FOUND);
        }

        try {
            TapeResponse ejectResponse = tapeDriveService.getDriveCommandService().eject();
            if (!ejectResponse.isOK()) {
                throw new ReadWriteException(MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode() +
                        " Action : Eject tape with forced rewind, Order: " + JsonHandler.unprettyPrint(writeOrder) +
                        ", Error: Could not rewind or unload tape",
                        ReadWriteErrorCode.KO_REWIND_BEFORE_UNLOAD_TAPE, ejectResponse);
            }

            workerCurrentTape.setCurrentPosition(0);

            final TapeRobotService tapeRobotService = tapeRobotPool.checkoutRobotService();

            try {
                TapeLoadUnloadService loadUnloadService = tapeRobotService.getLoadUnloadService();

                TapeResponse response = loadUnloadService.unloadTape(slotIndex, driveIndex);

                if (!response.isOK()) {
                    TapeDriveSpec status = doDriveStatus(ReadWriteErrorCode.KO_ON_UNLOAD_THEN_STATUS);

                    if (status.driveHasTape()) {
                        // Retry once
                        response = loadUnloadService.unloadTape(slotIndex, driveIndex);

                        if (!response.isOK()) {
                            throw new ReadWriteException(
                                MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode() + ", Action : unload, Entity: " +
                                    JsonHandler.unprettyPrint(response.getEntity()),
                                ReadWriteErrorCode.KO_ON_UNLOAD_TAPE,
                                response);
                        }
                    }
                }

                workerCurrentTape.setCurrentLocation(workerCurrentTape.getPreviousLocation());

            } finally {
                tapeRobotPool.pushRobotService(tapeRobotService);
            }

        } catch (InterruptedException e) {
            LOGGER.error(MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode(), e);
            throw new ReadWriteException(MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode(), e);
        }

    }

    @Override
    public ReadWriteResult get(long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException {

        return CompletableFuture.supplyAsync(() -> get(), VitamThreadPoolExecutor.getDefaultExecutor())
            .get(timeout, unit);

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
}
