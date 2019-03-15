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

import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.storage.engine.common.model.TapeCatalog;
import fr.gouv.vitam.storage.engine.common.model.TapeLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeLocationType;
import fr.gouv.vitam.storage.offers.tape.dto.CommandResponse;
import fr.gouv.vitam.storage.offers.tape.dto.TapeDriveState;
import fr.gouv.vitam.storage.offers.tape.exception.QueueException;
import fr.gouv.vitam.storage.offers.tape.exception.ReadWriteErrorCode;
import fr.gouv.vitam.storage.offers.tape.exception.ReadWriteException;
import fr.gouv.vitam.storage.offers.tape.exception.TapeCatalogException;
import fr.gouv.vitam.storage.offers.tape.order.ReadOrder;
import fr.gouv.vitam.storage.offers.tape.spec.TapeCatalogService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeDriveService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeRobotPool;
import org.bson.conversions.Bson;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;

import static com.mongodb.client.model.Filters.eq;

public class ReadTask implements Future<ReadWriteResult> {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ReadTask.class);
    public static final String TAPE_MSG = " [Tape] : ";
    // FIXME: 13/03/19 : add to configuration
    private static final String WORKING_DIR = "/tmp";

    private final TapeRobotPool tapeRobotPool;
    private final TapeDriveService tapeDriveService;
    private final TapeCatalogService tapeCatalogService;
    private final ReadOrder readOrder;
    private final String MSG_PREFIX;
    private final long TIMEOUT_IN_MILLISECONDS;

    private TapeCatalog workerCurrentTape;

    protected boolean cancelled = false;
    protected boolean done = false;

    public ReadTask(ReadOrder readOrder, TapeCatalog workerCurrentTape, TapeRobotPool tapeRobotPool, TapeDriveService tapeDriveService, TapeCatalogService tapeCatalogService) {
        this.readOrder = readOrder;
        this.workerCurrentTape = workerCurrentTape;
        this.tapeRobotPool = tapeRobotPool;
        this.tapeDriveService = tapeDriveService;
        this.tapeCatalogService = tapeCatalogService;
        this.TIMEOUT_IN_MILLISECONDS = tapeDriveService.getTapeDriveConf().getTimeoutInMilliseconds();
        this.MSG_PREFIX = String.format("[Library] : %s, [Drive] : %s, ", tapeRobotPool.getLibraryIdentifier(),
                tapeDriveService.getTapeDriveConf().getIndex());
    }

    @Override
    public ReadWriteResult get() {
        final ReadWriteResult readWriteResult = new ReadWriteResult();
        readWriteResult.setCurrentTape(workerCurrentTape);

        try {
            if (null != workerCurrentTape) {
                if (workerCurrentTape.getCode().equals(readOrder.getTapeCode())) {
                    doRead(readWriteResult);
                } else {
                    // Unload current tape
                    CommandResponse unloadResponse = unloadTape();
                    if (StatusCode.OK.equals(unloadResponse.getStatus())) {
                        // Find tape
                        // If tape not found WARN (return TAR to queue and continue)
                        // If tape ok load tape to drive
                        // Do status to get tape TYPE and some other information (update catalog)
                        workerCurrentTape = getTapeFromCatalog();
                        loadTape();
                        doRead(readWriteResult);
                    }
                }
            } else {
                // Find tape
                // If tape not found WARN (return TAR to queue and continue)
                // If tape ok load tape to drive
                // Do status to get tape TYPE and some other information (update catalog)
                workerCurrentTape = getTapeFromCatalog();
                loadTape();
                doRead(readWriteResult);
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

    @Override
    public ReadWriteResult get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return CompletableFuture
                .supplyAsync(() -> get(), VitamThreadPoolExecutor.getDefaultExecutor()).get(timeout, unit);
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

    private void doRead(ReadWriteResult readWriteResult) throws ReadWriteException {
        try {
            // move drive to the given position
            // FIXME: 13/03/19 voir si on peut recupérer la position courante (gestion dans le catalogue?)
            tapeDriveService.getDriveCommandService().rewind(TIMEOUT_IN_MILLISECONDS);
            if (readOrder.getFilePosition() > 0) {
                tapeDriveService.getDriveCommandService().goToPosition(TIMEOUT_IN_MILLISECONDS,
                        readOrder.getFilePosition().toString());
            }

            // read file from tape
            CommandResponse response = tapeDriveService.getReadWriteService(TapeDriveService.ReadWriteCmd.DD)
                    .readFromTape(TIMEOUT_IN_MILLISECONDS, WORKING_DIR, readOrder.getFileName());

            if (!StatusCode.OK.equals(response.getStatus())) {
                LOGGER.error(MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode() +
                        " Action : Read, Order: " + JsonHandler.unprettyPrint(readOrder) + ", ExitCode: " +
                        response.getOutput().getExitCode() + ", Error: " +
                        response.getOutput().getStderr());
            }

            readWriteResult.setStatus(StatusCode.OK);
        } catch (Exception e) {
            throw new ReadWriteException(MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode(), e);
        }
    }

    /**
     * Get eligible tape from catalog
     *
     * @return Optional of TapeCatalog
     * @throws ReadWriteException
     */
    private TapeCatalog getTapeFromCatalog() throws ReadWriteException {
        Bson query = eq(TapeCatalog.CODE, readOrder.getTapeCode());

        try {
            Optional<TapeCatalog> found = tapeCatalogService.peek(query, TapeCatalog.class);
            if (found.isPresent()) {
                TapeCatalog tape = found.get();
                if (tape.getCurrentLocation().getLocationType().getType().equals(TapeLocationType.OUTSIDE)) {
                    return tape;
                }
            }

            throw new ReadWriteException(MSG_PREFIX +
                    " Action : LoadTapeFromCatalog, Order: " + JsonHandler.unprettyPrint(readOrder) +
                    ", Error: no tape found in the catalog with expected library and/or bucket",
                    ReadWriteErrorCode.TAPE_NOT_FOUND_IN_CATALOG);

        } catch (QueueException e) {
            throw new ReadWriteException(MSG_PREFIX, e);
        }
    }

    /**
     * Load current tape to drive
     *
     * @return CommandResponse with statusCode OK if success
     * @throws ReadWriteException
     */
    private CommandResponse loadTape() throws ReadWriteException {

        Integer driveIndex = tapeDriveService.getTapeDriveConf().getIndex();
        Integer slotIndex;
        if (null != workerCurrentTape.getCurrentLocation()) {
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

            // update catalog
            updateTapeLocation(new TapeLocation(driveIndex, TapeLocationType.DIRVE));

            return response;
        } catch (InterruptedException | TapeCatalogException e) {
            throw new ReadWriteException(MSG_PREFIX + ", Error: ", e);
        }
    }

    /**
     * Unload tape from  drive
     *
     * @return
     * @throws ReadWriteException
     */
    private CommandResponse unloadTape() throws ReadWriteException {

        Integer driveIndex = workerCurrentTape.getCurrentLocation().getIndex();
        Integer slotIndex;

        switch (workerCurrentTape.getPreviousLocation().getLocationType()) {
            case SLOT:
                slotIndex = workerCurrentTape.getPreviousLocation().getIndex();
                break;
            case DIRVE:
            case OUTSIDE:
            case IMPORTEXPORT:
                throw new ReadWriteException(MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode() +
                        ", Error: previous location should no be in " +
                        workerCurrentTape.getPreviousLocation().getLocationType().getType(),
                        ReadWriteErrorCode.TAPE_LOCATION_CONFLICT);
            default:
                throw new IllegalArgumentException(
                        MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode() + ", Error: location type not implemented");
        }

        try {
            CommandResponse response = tapeRobotPool.checkoutRobotService().getLoadUnloadService()
                    .unloadTape(TIMEOUT_IN_MILLISECONDS, slotIndex, driveIndex);

            if (response.getStatus() != StatusCode.OK) {
                LOGGER.error(MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode() + ", Action : unload, ExitCode: " +
                        response.getOutput().getExitCode() + ", Error: " +
                        response.getOutput().getStderr());
            }

            // update catalog
            updateTapeLocation(new TapeLocation(slotIndex, TapeLocationType.SLOT));

            // release the tape
            tapeCatalogService.ready(workerCurrentTape.getId());

            return response;
        } catch (InterruptedException | TapeCatalogException | QueueException e) {
            LOGGER.error(MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode(), e);
            throw new ReadWriteException(MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode(), e);
        }
    }

    private void updateTapeLocation(TapeLocation currentLocation) throws TapeCatalogException {
        workerCurrentTape.setPreviousLocation(workerCurrentTape.getCurrentLocation());
        workerCurrentTape.setCurrentLocation(currentLocation);
        Map<String, Object> updates = new HashMap<>();
        updates.put(TapeCatalog.PREVIOUS_LOCATION, workerCurrentTape.getCurrentLocation());
        updates.put(TapeCatalog.CURRENT_LOCATION, currentLocation);
        tapeCatalogService.update(workerCurrentTape.getId(), updates);
    }
}
