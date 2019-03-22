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

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.exists;
import static com.mongodb.client.model.Filters.or;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.fasterxml.jackson.databind.node.ObjectNode;
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
import fr.gouv.vitam.storage.engine.common.model.TapeLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeLocationType;
import fr.gouv.vitam.storage.engine.common.model.TapeState;
import fr.gouv.vitam.storage.engine.common.model.WriteOrder;
import fr.gouv.vitam.storage.offers.tape.dto.TapeDriveSpec;
import fr.gouv.vitam.storage.offers.tape.dto.TapeDriveStatus;
import fr.gouv.vitam.storage.offers.tape.dto.TapeResponse;
import fr.gouv.vitam.storage.offers.tape.exception.QueueException;
import fr.gouv.vitam.storage.offers.tape.exception.ReadWriteErrorCode;
import fr.gouv.vitam.storage.offers.tape.exception.ReadWriteException;
import fr.gouv.vitam.storage.offers.tape.exception.TapeCatalogException;
import fr.gouv.vitam.storage.offers.tape.retry.Retry;
import fr.gouv.vitam.storage.offers.tape.spec.TapeCatalogService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeDriveService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeLoadUnloadService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeRobotPool;
import fr.gouv.vitam.storage.offers.tape.spec.TapeRobotService;
import org.apache.logging.log4j.util.Strings;
import org.bson.conversions.Bson;

public class WriteTask implements Future<ReadWriteResult> {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(WriteTask.class);
    public static final String TAPE_MSG = " [Tape] : ";
    public static final String TAPE_LABEL = "tape-Label-";

    // WorkingDir is empty because of : writeOrder should have an absolute file path
    public static final String WORKING_DIR = "";
    public static final long SLEEP_TIME = 20l;
    public static final int CARTRIDGE_RETRY = 1;

    public final String MSG_PREFIX;
    protected boolean cancelled = false;
    protected boolean done = false;

    private TapeCatalog workerCurrentTape;
    private final TapeRobotPool tapeRobotPool;
    private final TapeDriveService tapeDriveService;
    private final TapeCatalogService tapeCatalogService;
    private final WriteOrder writeOrder;
    private boolean retryEnabled = true;

    private int cartridgeRetry = CARTRIDGE_RETRY;

    public WriteTask(WriteOrder writeOrder, TapeCatalog workerCurrentTape, TapeRobotPool tapeRobotPool,
        TapeDriveService tapeDriveService, TapeCatalogService tapeCatalogService) {
        ParametersChecker.checkParameter("WriteOrder param is required.", writeOrder);
        ParametersChecker.checkParameter("TapeRobotPool param is required.", tapeRobotPool);
        ParametersChecker.checkParameter("TapeDriveService param is required.", tapeDriveService);
        ParametersChecker.checkParameter("TapeCatalogService param is required.", tapeCatalogService);
        this.writeOrder = writeOrder;
        this.workerCurrentTape = workerCurrentTape;
        this.tapeRobotPool = tapeRobotPool;
        this.tapeDriveService = tapeDriveService;
        this.tapeCatalogService = tapeCatalogService;
        this.MSG_PREFIX = String.format("[Library] : %s, [Drive] : %s, ", tapeRobotPool.getLibraryIdentifier(),
            tapeDriveService.getTapeDriveConf().getIndex());
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

            readWriteResult.setStatus(StatusCode.OK);
            readWriteResult.setOrderState(QueueState.COMPLETED);

        } catch (ReadWriteException e) {
            LOGGER.error(e);
            // TODO: 11/03/19 complete
            switch (e.getReadWriteErrorCode()) {
                case KO_ON_STATUS:
                case KO_ON_LOAD_THEN_STATUS:
                case KO_ON_UNLOAD_THEN_STATUS:
                    // TODO: 20/03/19 worker down, security alert
                    // FIXME: 19/03/19 timeout ?!! or unreachable drive? unknown error?
                    readWriteResult.setStatus(StatusCode.FATAL);
                    readWriteResult.setOrderState(QueueState.ERROR);
                    break;
                case KO_ON_WRITE_THEN_STATUS:
                    // TODO: 22/03/19 perhaps incident close tape and mark state as conflict
                    readWriteResult.setStatus(StatusCode.FATAL);
                    readWriteResult.setOrderState(QueueState.ERROR);
                    break;
                case KO_ON_WRITE_TO_TAPE:
                    // FIXME: 19/03/19 error while write file in the tape. perhaps timeout ?!!
                    // After retry write -> status -> rewind -> goToPosition -> write
                    // Mark tape state conflict and retry with new tape
                    if (--cartridgeRetry >= 0) {
                        return get();
                    } else {
                        readWriteResult.setStatus(StatusCode.FATAL);
                        readWriteResult.setOrderState(QueueState.READY);
                    }
                    break;
                case KO_ON_LOAD_TAPE:
                    // FIXME: 19/03/19 tape is online ? timeout ? drive is busy?
                    break;
                case KO_ON_UNLOAD_TAPE:
                    // FIXME: 19/03/19 tape is open (drive empty) ? timeout ? drive is busy?
                    break;
                case KO_ON_REWIND_TAPE:
                    // FIXME: 19/03/19 cannot rewind the tape
                    break;
                case KO_ON_REWIND_FSF_BSF_TAPE:
                    // FIXME: 19/03/19 cannot forward in the tape
                    break;
                case NULL_CURRENT_TAPE:
                    // TODO: 19/03/19 should not get this error, only if somme methods called directly
                    break;
                case KO_DB_PERSIST:
                    // Can't update database
                    readWriteResult.setStatus(StatusCode.FATAL);
                    readWriteResult.setOrderState(QueueState.READY);
                    break;
                case KO_END_OF_TAPE:
                    workerCurrentTape.setTapeState(TapeState.FULL);
                    // Re-call get, to unload current tape, load new tape then do write
                    return get();
                case FILE_NOT_FOUND:
                    // Mark write order as error state
                    readWriteResult.setStatus(StatusCode.FATAL);
                    readWriteResult.setOrderState(QueueState.ERROR);
                    break;
                case NO_EMPTY_SLOT_FOUND:
                    // TODO: 19/03/19 FATAL for the current drive cannot unload tape
                    break;
                case TAPE_LOCATION_CONFLICT:
                    // TODO: 19/03/19 FATAL conflict, resolve problem then re-init the catalog
                    // TODO: 19/03/19 perhaps try to re-init the catalog ?!!
                    break;
                case TAPE_NOT_FOUND_IN_CATALOG:
                    // TODO: 19/03/19 log security error and send information to load in mailbox concerning tape
                    break;
                case INTERNAL_ERROR_SERVER:
                default:
                    // TODO: 11/03/19 FATAL stop current Drive worker
            }
        }

        readWriteResult.setCurrentTape(workerCurrentTape);
        return readWriteResult;
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
        File file = new File(WORKING_DIR, writeOrder.getFilePath());

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

        // If no label then cartridge is unknown
        if (Strings.isEmpty(workerCurrentTape.getLabel())) {

            // Do status to get tape TYPE and some other information (update catalog)
            LOGGER.debug(
                MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode() + ", Action: drive status");
            TapeDriveSpec driveStatus = doDriveStatus(ReadWriteErrorCode.KO_ON_STATUS);

            workerCurrentTape.setType(driveStatus.getCartridge());
            workerCurrentTape.setWorm(driveStatus.getDriveStatuses().contains(TapeDriveStatus.WR_PROT));
            workerCurrentTape.setBucket(writeOrder.getBucket());

            withRetryDoUpdateTapeCatalog(workerCurrentTape);
        }

        return true;
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

        if (Strings.isEmpty(workerCurrentTape.getLabel())) {
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

        return Objects.equals(workerCurrentTape.getBucket(), writeOrder.getBucket()) &&
            (TapeState.OPEN.equals(workerCurrentTape.getTapeState()) ||
                TapeState.EMPTY.equals(workerCurrentTape.getTapeState()));
    }

    /**
     * Load tape from catalog
     *
     * @return Optional of TapeCatalog
     * @throws ReadWriteException
     */
    private TapeCatalog loadTapeFromCatalog() throws ReadWriteException {

        String bucket = writeOrder.getBucket();

        // TODO: 20/03/19 prefer tape that already contains data
        Bson query = and(
            eq(TapeCatalog.LIBRARY, tapeRobotPool.getLibraryIdentifier()),
            or(
                eq(TapeCatalog.BUCKET, bucket),
                exists(TapeCatalog.BUCKET, false)
            )
        );

        try {
            Optional<TapeCatalog> found = tapeCatalogService.receive(query, QueueMessageType.TapeCatalog);
            if (found.isPresent()) {
                return found.get();
            } else {
                throw new ReadWriteException(MSG_PREFIX +
                    " Action : Load Tape From Catalog, Order: " + JsonHandler.unprettyPrint(writeOrder) +
                    ", Error: no markReady tape found in the catalog with expected bucket and/or remainingSize",
                    ReadWriteErrorCode.TAPE_NOT_FOUND_IN_CATALOG);
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

        ObjectNode objLabel = JsonHandler.createObjectNode();
        objLabel.put(TapeCatalog.CODE, workerCurrentTape.getCode());
        objLabel.put(TapeCatalog.ALTERNATIVE_CODE, workerCurrentTape.getAlternativeCode());
        objLabel.put(TapeCatalog.BUCKET, workerCurrentTape.getBucket());
        objLabel.put(TapeCatalog.TYPE, workerCurrentTape.getType());
        objLabel.put(TapeCatalog.ID, workerCurrentTape.getId());

        File labelFile = null;
        try {
            labelFile = File.createTempFile(TAPE_LABEL, GUIDFactory.newGUID().getId());
            JsonHandler.writeAsFile(objLabel, labelFile);

            long fileSize = labelFile.length();

            TapeResponse response = tapeDriveService.getReadWriteService(TapeDriveService.ReadWriteCmd.DD)
                .writeToTape(labelFile.getAbsolutePath());

            if (!response.isOK()) {
                LOGGER.error(MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode() +
                    " Action : Write, Order: " + JsonHandler.unprettyPrint(writeOrder) + ", Entity: " +
                    JsonHandler.unprettyPrint(response.getEntity()));

                retryWriteToTape(labelFile, 2, response);
            }

            workerCurrentTape.setFileCount(1);
            workerCurrentTape.setCurrentPosition(0);
            workerCurrentTape.setLabel(JsonHandler.unprettyPrint(objLabel));
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
                .writeToTape(file.getAbsolutePath());

            if (!response.isOK()) {
                LOGGER.error(MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode() +
                    " Action : Write, Order: " + JsonHandler.unprettyPrint(writeOrder) + ", Entity: " +
                    JsonHandler.unprettyPrint(response.getEntity()));

                // Do status and check if end of tape
                TapeDriveSpec status = doDriveStatus(ReadWriteErrorCode.KO_ON_WRITE_THEN_STATUS);

                if (status.isEndOfTape()) {
                    throw new ReadWriteException(MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode() +
                        " Action : Write, Order: " + JsonHandler.unprettyPrint(writeOrder) + ", Drive Status: " +
                        JsonHandler.unprettyPrint(status.getEntity()) + ", Error: End of tape",
                        ReadWriteErrorCode.KO_END_OF_TAPE, response);
                }

                if (!workerCurrentTape.isWorm() || retryEnabled) {
                    retryWriteToTape(file, 2, response);
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
            workerCurrentTape.setCurrentPosition(workerCurrentTape.getFileCount() - 1);
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
     * @param file
     * @param nbRetry
     * @param response
     * @return TapeResponse
     * @throws ReadWriteException
     */
    private void retryWriteToTape(File file, int nbRetry, TapeResponse response)
        throws ReadWriteException {

        while (nbRetry != 0 && !response.isOK()) {

            nbRetry--;

            // Rewind
            doRewindTape();

            // FSF
            goToPosition(ReadWriteErrorCode.KO_ON_REWIND_FSF_BSF_TAPE);


            try {
                Thread.sleep(SLEEP_TIME);
            } catch (InterruptedException e) {
                SysErrLogger.FAKE_LOGGER.ignoreLog(e);
            }

            response = tapeDriveService.getReadWriteService(TapeDriveService.ReadWriteCmd.DD)
                .writeToTape(file.getAbsolutePath());

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
    private void goToPosition(ReadWriteErrorCode readWriteErrorCode) throws ReadWriteException {
        TapeResponse fsfResponse = tapeDriveService.getDriveCommandService()
            .goToPosition(workerCurrentTape.getFileCount() - 1);
        workerCurrentTape.setCurrentPosition(workerCurrentTape.getFileCount() - 1);


        if (!fsfResponse.isOK()) {
            throw new ReadWriteException(MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode() +
                " Action : FSF goto position, Order: " +
                JsonHandler.unprettyPrint(writeOrder) + ", Entity: " +
                JsonHandler.unprettyPrint(fsfResponse.getEntity()),
                readWriteErrorCode,
                fsfResponse);
        }
    }

    /**
     * rewind command
     *
     * @throws ReadWriteException
     */
    private void doRewindTape() throws ReadWriteException {
        // Rewind
        TapeResponse rewindResponse =
            tapeDriveService.getDriveCommandService().rewind();
        workerCurrentTape.setCurrentPosition(0);

        if (!rewindResponse.isOK()) {
            throw new ReadWriteException(MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode() +
                " Action : Rewind Tape, Order: " + JsonHandler.unprettyPrint(writeOrder) +
                ", Entity: " +
                JsonHandler.unprettyPrint(rewindResponse.getEntity()), ReadWriteErrorCode.KO_ON_REWIND_TAPE,
                rewindResponse);
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
            new Retry(3, SLEEP_TIME).execute(delegate);
        } catch (Exception e) {
            if (e instanceof ReadWriteException) {
                throw (ReadWriteException) e;
            }
            throw new ReadWriteException(e);
        }
    }

    /**
     * Load current tape to drive
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

                    if (status.isEmptyDrive()) {

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

                // FIXME: 20/03/19 read label and check that there is no tape conflict . flag configurable
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
        Integer slotIndex;

        if (null != workerCurrentTape.getPreviousLocation()) {
            switch (workerCurrentTape.getPreviousLocation().getLocationType()) {
                case SLOT:
                    slotIndex = workerCurrentTape.getPreviousLocation().getIndex();
                    break;
                case DRIVE:
                    throw new ReadWriteException(MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode() +
                        ", Error: previous location should no be in drive",
                        ReadWriteErrorCode.TAPE_LOCATION_CONFLICT);
                case OUTSIDE:
                case IMPORTEXPORT:
                    slotIndex = findEmptySlot();
                    break;
                default:
                    throw new IllegalArgumentException(
                        MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode() + ", Error: location type not implemented");
            }

        } else {
            slotIndex = findEmptySlot();
        }

        if (null == slotIndex) {
            throw new ReadWriteException(MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode() +
                ", Error : no empty slot found => cannot unload tape", ReadWriteErrorCode.NO_EMPTY_SLOT_FOUND);
        }

        try {
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

    private Integer findEmptySlot() {
        return null;
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
