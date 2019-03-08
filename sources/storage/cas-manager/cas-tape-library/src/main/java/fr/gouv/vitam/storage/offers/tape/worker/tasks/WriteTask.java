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
import static com.mongodb.client.model.Filters.gt;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.annotations.VisibleForTesting;
import com.mongodb.client.model.Updates;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.storage.engine.common.model.QueueEntity;
import fr.gouv.vitam.storage.engine.common.model.QueueState;
import fr.gouv.vitam.storage.engine.common.model.TapeCatalog;
import fr.gouv.vitam.storage.offers.tape.dto.CommandResponse;
import fr.gouv.vitam.storage.offers.tape.dto.TapeDriveState;
import fr.gouv.vitam.storage.offers.tape.exception.QueueException;
import fr.gouv.vitam.storage.offers.tape.exception.ReadWriteErrorCode;
import fr.gouv.vitam.storage.offers.tape.exception.ReadWriteException;
import fr.gouv.vitam.storage.offers.tape.exception.TapeCatalogException;
import fr.gouv.vitam.storage.offers.tape.order.WriteOrder;
import fr.gouv.vitam.storage.offers.tape.spec.TapeCatalogService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeDriveService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeRobotPool;
import org.apache.logging.log4j.util.Strings;
import org.bson.conversions.Bson;

public class WriteTask implements Future<ReadWriteResult> {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(WriteTask.class);
    public static final String TAPE_MSG = " [Tape] : ";
    public static final int TAR_SIZE_IN_BYTES = 10 * 1024 * 1024 * 1024;
    // 10Gia * 1024(Mega) * 1024 (K Bytes) * 1024 (Bytes)
    public final long TIMEOUT_IN_MILLISECONDS;

    // WorkingDir is empty because of : writeOrder should have an absolute file path
    public static final String WORKING_DIR = "";

    public final String MSG_PREFIX;
    protected boolean cancelled = false;
    protected boolean done = false;

    private TapeCatalog workerCurrentTape;
    private final TapeRobotPool tapeRobotPool;
    private final TapeDriveService tapeDriveService;
    private final TapeCatalogService tapeCatalogService;
    private final WriteOrder writeOrder;

    public WriteTask(WriteOrder writeOrder, TapeCatalog workerCurrentTape, TapeRobotPool tapeRobotPool,
        TapeDriveService tapeDriveService, TapeCatalogService tapeCatalogService) {
        this.writeOrder = writeOrder;
        this.workerCurrentTape = workerCurrentTape;
        this.tapeRobotPool = tapeRobotPool;
        this.tapeDriveService = tapeDriveService;
        this.tapeCatalogService = tapeCatalogService;
        this.TIMEOUT_IN_MILLISECONDS = tapeDriveService.getTapeDriveConf().getTimeoutInMilliseconds();
        this.MSG_PREFIX = String.format("[Library] : %s, [Drive] : %s, ", tapeRobotPool.getLibraryIdentifier(),
            tapeDriveService.getTapeDriveConf().getIndex());
    }

    @VisibleForTesting
    public WriteTask(WriteOrder writeOrder, TapeCatalog workerCurrentTape, TapeRobotPool tapeRobotPool,
        TapeDriveService tapeDriveService, TapeCatalogService tapeCatalogService, long timeoutInMilliseconds) {
        this.writeOrder = writeOrder;
        this.workerCurrentTape = workerCurrentTape;
        this.tapeRobotPool = tapeRobotPool;
        this.tapeDriveService = tapeDriveService;
        this.tapeCatalogService = tapeCatalogService;
        this.TIMEOUT_IN_MILLISECONDS = timeoutInMilliseconds;
        this.MSG_PREFIX = String.format("[Library] : %s, [Drive] : %s, ", tapeRobotPool.getLibraryIdentifier(),
            tapeDriveService.getTapeDriveConf().getIndex());
    }

    @Override
    public ReadWriteResult get() {

        final ReadWriteResult readWriteResult = new ReadWriteResult();
        readWriteResult.setCurrentTape(workerCurrentTape);

        try {
            if (null != workerCurrentTape) {
                if (canWriteOnTape()) {
                    doWrite(readWriteResult);
                } else {
                    // Unload current tape
                    CommandResponse unloadResponse = doUnloadTape();
                    if (StatusCode.OK.equals(unloadResponse.getStatus())) {
                        // Find tape
                        // If tape not found WARN (return TAR to queue and continue)
                        // If tape ok load tape to drive
                        // Do status to get tape TYPE and some other information (update catalog)
                        if (findAndLoadTape(readWriteResult)) {
                            // Check if new tape then doWrite(label)
                            // doWrite(TAR)
                            doWrite(readWriteResult);
                        }
                    }
                }
            } else {
                // Find tape
                // If tape not found WARN (return TAR to queue and continue)
                // If tape ok load tape to drive
                // Do status to get tape TYPE and some other information (update catalog)
                if (findAndLoadTape(readWriteResult)) {
                    // Check if new tape then doWrite(label)
                    // doWrite(TAR)
                    doWrite(readWriteResult);
                }
            }
        } catch (ReadWriteException e) {
            LOGGER.error(e);
            // TODO: 11/03/19 complete
            switch (e.getReadWriteErrorCode()) {
                case NULL_CURRENT_TAPE:
                    break;
                case NO_EMPTY_SLOT_FOUND:
                    break;
                case TAPE_LOCATION_CONFLICT:
                    break;
                case TAPE_NOT_FOUND_IN_CATALOG:
                    break;
                case INTERNAL_ERROR_SERVER:
                default:
                    // TODO: 11/03/19 treat exception
            }
        }

        return readWriteResult;
    }

    private boolean findAndLoadTape(ReadWriteResult readWriteResult) throws ReadWriteException {
        // Find tape
        // If tape not found WARN (return TAR to queue and continue)
        workerCurrentTape = loadTapeFromCatalog();

        // If tape found in catalog then load tape to drive
        CommandResponse loadResponse = doLoadTape();

        if (!StatusCode.OK.equals(loadResponse.getStatus())) {
            // TODO: retry
            // readWriteResult.setTapeNotFoundError
            return false;
        }


        // Do status to get tape TYPE and some other information (update catalog)
        TapeDriveState driveStatus = doDriveStatus();
        if (!StatusCode.OK.equals(driveStatus.getStatus())) {

            //readWriteResult.setDriveStateError
            return false;
        }


        // TODO: 11/03/19 update  readWriteResult with success and information to add
        return true;
    }

    private void doWrite(ReadWriteResult readWriteResult) throws ReadWriteException {

        // Deep copy of tape
        // TODO Update tape catalog with remaningSize
        // Check if new tape then doWrite(label)
        String label = GUIDFactory.newGUID().getId();

        CommandResponse writeLabelResonse = tryWriteLabelToTape(label);

        if (!StatusCode.OK.equals(writeLabelResonse.getStatus())) {
            // TODO: 11/03/19 get label if changed and update index
        }
        // doWrite(TAR)
        CommandResponse writeResponse = doWriteFileToTape();

        if (!StatusCode.OK.equals(writeResponse.getStatus())) {
            // TODO perhaps doDriveStatus
            // TODO Make tape IncidentClose this tape will be read only if WORM TAPE else do fsf last FileCount and retry nbtime if fail then IncidentClose
            // TODO: 11/03/19 return error
            // TODO: 11/03/19 update  readWriteResult with error information
        }

        // TODO: 11/03/19 update readWriteResult with success information



        // TODO: 12/03/19 /!\ if exceptions occurs rollback
    }


    /**
     * Check if tape and write order are for the same bucket and the tape contains sufficient space to store the file
     *
     * @return true if can write, false else
     */
    private boolean canWriteOnTape() {
        if (null == workerCurrentTape) {
            return false;
        }

        boolean bucketNotManaged = workerCurrentTape.getBucket() == null && writeOrder.getBucket() == null;

        return (bucketNotManaged || Objects.equals(workerCurrentTape.getBucket(), writeOrder.getBucket())) &&
            (workerCurrentTape.getRemainingSize() > writeOrder.getSize());
    }


    /**
     * Load tape from catalog
     *
     * @return Optional of TapeCatalog
     * @throws ReadWriteException
     */
    private TapeCatalog loadTapeFromCatalog() throws ReadWriteException {

        String bucket = writeOrder.getBucket();

        Bson libraryFilter = eq(TapeCatalog.LIBRARY, tapeRobotPool.getLibraryIdentifier());
        Bson remainingSize = gt(TapeCatalog.REMAINING_SIZE, writeOrder.getSize());

        Bson query = null != bucket ?
            and(libraryFilter, eq(TapeCatalog.BUCKET, bucket), remainingSize) :
            and(libraryFilter, remainingSize);
        Bson update = Updates.inc(TapeCatalog.REMAINING_SIZE, 0 - writeOrder.getSize());

        try {
            Optional<TapeCatalog> found = tapeCatalogService.peek(query, update, TapeCatalog.class);
            if (found.isPresent()) {
                return found.get();
            } else {
                throw new ReadWriteException(MSG_PREFIX +
                    " Action : LoadTapeFromCatalog, Order: " + JsonHandler.unprettyPrint(writeOrder) +
                    ", Error: no ready tape found in the catalog with expected bucket and/or remainingSize",
                    ReadWriteErrorCode.TAPE_NOT_FOUND_IN_CATALOG);
            }
        } catch (QueueException e) {
            throw new ReadWriteException(MSG_PREFIX, e);
        }
    }

    /**
     * @return
     */
    private TapeDriveState doDriveStatus() {
        return tapeDriveService.getDriveCommandService().status(TIMEOUT_IN_MILLISECONDS);
    }


    /**
     * Write writeOrder to tape if tape label is empty
     *
     * @return true if write success, false else
     * @throws ReadWriteException
     */
    private CommandResponse tryWriteLabelToTape(String label) throws ReadWriteException {
        if (null == workerCurrentTape) {
            throw new ReadWriteException(
                MSG_PREFIX + ", Error: can't write label current tape is null.", ReadWriteErrorCode.NULL_CURRENT_TAPE);
        }

        if (Strings.isNotEmpty(workerCurrentTape.getLabel())) {
            CommandResponse response = new CommandResponse();
            response.setStatus(StatusCode.OK);
            return response;
        }

        // TODO: 08/03/19 write label
        try {
            CommandResponse response = tapeDriveService.getReadWriteService(TapeDriveService.ReadWriteCmd.DD)
                .writeToTape(TIMEOUT_IN_MILLISECONDS, WORKING_DIR, writeOrder.getFilePath());

            if (response.getStatus() != StatusCode.OK) {
                LOGGER.error(MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode() +
                    " Action : Write, Order: " + JsonHandler.unprettyPrint(writeOrder) + ", ExitCode: " +
                    response.getOutput().getExitCode() + ", Error: " +
                    response.getOutput().getStderr());
            } else {
                workerCurrentTape.setLabel(label);
            }

            return response;
        } catch (Exception e) {
            throw new ReadWriteException(MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode(), e);
        }
    }


    /**
     * Write writeOrder to tape
     *
     * @return true if write success, false else
     * @throws ReadWriteException
     */
    private CommandResponse doWriteFileToTape() throws ReadWriteException {
        if (null == workerCurrentTape) {
            throw new ReadWriteException(
                MSG_PREFIX + ", Error: can't write file current tape is null.", ReadWriteErrorCode.NULL_CURRENT_TAPE);
        }


        // TODO: 08/03/19 write tar check  WORKING_DIR
        try {
            CommandResponse response = tapeDriveService.getReadWriteService(TapeDriveService.ReadWriteCmd.DD)
                .writeToTape(TIMEOUT_IN_MILLISECONDS, WORKING_DIR, writeOrder.getFilePath());

            if (!StatusCode.OK.equals(response.getStatus())) {
                LOGGER.error(MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode() +
                    " Action : Write, Order: " + JsonHandler.unprettyPrint(writeOrder) + ", ExitCode: " +
                    response.getOutput().getExitCode() + ", Error: " +
                    response.getOutput().getStderr());
            }

            return response;
        } catch (Exception e) {
            throw new ReadWriteException(MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode(), e);
        }
    }

    private void tapeBackToCatalog() throws ReadWriteException {
        if (null == workerCurrentTape) {
            throw new ReadWriteException(
                MSG_PREFIX + ", Error: tape to update in the catalog is null.", ReadWriteErrorCode.NULL_CURRENT_TAPE);
        }

        try {
            if (workerCurrentTape.getRemainingSize() < TAR_SIZE_IN_BYTES) {
                tapeCatalogService.complete(workerCurrentTape.getId());
            } else {
                Map<String, Object> updates = new HashMap<>();
                updates.put(QueueEntity.STATE, QueueState.READY);
                updates.put(TapeCatalog.CURRENT_LOCATION, workerCurrentTape.getPreviousLocation());
                tapeCatalogService.update(workerCurrentTape.getId(), updates);
            }
        } catch (TapeCatalogException |
            QueueException e) {
            throw new ReadWriteException(
                MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode() + ", Error: while update tape catalog", e);
        }

    }

    /**
     * Load current tape to drive
     *
     * @return CommandResponse with statusCode OK if success
     * @throws ReadWriteException
     */
    private CommandResponse doLoadTape() throws ReadWriteException {
        if (null == workerCurrentTape) {
            throw new ReadWriteException(
                MSG_PREFIX + ", Error: tape to load is null. please get ready tape from catalog",
                ReadWriteErrorCode.NULL_CURRENT_TAPE);
        }

        Integer driveIndex = tapeDriveService.getTapeDriveConf().getIndex();
        Integer slotIndex;
        if (null != workerCurrentTape.getPreviousLocation()) {
            slotIndex = workerCurrentTape.getCurrentLocation().getIndex();
        } else {
            throw new ReadWriteException(MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode() +
                ", Error: tape current location is null. please update catalog",
                ReadWriteErrorCode.TAPE_LOCATION_CONFLICT);
        }

        try {
            CommandResponse response =
                tapeRobotPool.checkoutRobotService().getLoadUnloadService()
                    .loadTape(TIMEOUT_IN_MILLISECONDS, slotIndex, driveIndex);

            if (response.getStatus() != StatusCode.OK) {
                LOGGER.error(MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode() + ", Action : load,  ExitCode: " +
                    response.getOutput().getExitCode() + ", Error: " +
                    response.getOutput().getStderr());
            }

            return response;
        } catch (InterruptedException e) {
            throw new ReadWriteException(MSG_PREFIX + ", Error: ", e);
        }
    }

    /**
     * Unload tape from  drive
     *
     * @return true if success, false else
     * @throws ReadWriteException
     */
    private CommandResponse doUnloadTape() throws ReadWriteException {
        if (null == workerCurrentTape) {
            throw new ReadWriteException(MSG_PREFIX + ", Error: tape to unload is null.",
                ReadWriteErrorCode.NULL_CURRENT_TAPE);
        }

        Integer driveIndex = workerCurrentTape.getCurrentLocation().getIndex();
        Integer slotIndex;

        if (null != workerCurrentTape.getPreviousLocation()) {
            switch (workerCurrentTape.getPreviousLocation().getType()) {
                case SLOT:
                    slotIndex = workerCurrentTape.getPreviousLocation().getIndex();
                    break;
                case DIRVE:
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
            CommandResponse response = tapeRobotPool.checkoutRobotService().getLoadUnloadService()
                .unloadTape(TIMEOUT_IN_MILLISECONDS, slotIndex, driveIndex);

            if (response.getStatus() != StatusCode.OK) {
                LOGGER.error(MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode() + ", Action : unload, ExitCode: " +
                    response.getOutput().getExitCode() + ", Error: " +
                    response.getOutput().getStderr());
            }

            return response;
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
