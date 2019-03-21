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

import fr.gouv.vitam.common.database.server.query.QueryCriteria;
import fr.gouv.vitam.common.database.server.query.QueryCriteriaOperator;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.storage.engine.common.model.QueueMessageType;
import fr.gouv.vitam.storage.engine.common.model.QueueState;
import fr.gouv.vitam.storage.engine.common.model.ReadOrder;
import fr.gouv.vitam.storage.engine.common.model.TapeCatalog;
import fr.gouv.vitam.storage.engine.common.model.TapeLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeLocationType;
import fr.gouv.vitam.storage.offers.tape.dto.TapeResponse;
import fr.gouv.vitam.storage.offers.tape.exception.QueueException;
import fr.gouv.vitam.storage.offers.tape.exception.ReadWriteErrorCode;
import fr.gouv.vitam.storage.offers.tape.exception.TapeCatalogException;
import fr.gouv.vitam.storage.offers.tape.spec.TapeCatalogService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeDriveService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeRobotPool;
import org.bson.conversions.Bson;

import java.util.Arrays;
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

import static com.mongodb.client.model.Filters.eq;

public class ReadTask implements Future<ReadWriteResult> {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ReadTask.class);
    public static final String TAPE_MSG = " [Tape] : ";
    private static final long TIMEOUT_IN_MILLISECONDES = 30000;

    private final TapeRobotPool tapeRobotPool;
    private final TapeDriveService tapeDriveService;
    private final TapeCatalogService tapeCatalogService;
    private final ReadOrder readOrder;
    private final String MSG_PREFIX;

    private TapeCatalog workerCurrentTape;

    protected AtomicBoolean done = new AtomicBoolean(false);

    public ReadTask(ReadOrder readOrder, TapeCatalog workerCurrentTape, TapeRobotPool tapeRobotPool, TapeDriveService tapeDriveService, TapeCatalogService tapeCatalogService) {
        this.readOrder = readOrder;
        this.workerCurrentTape = workerCurrentTape;
        this.tapeRobotPool = tapeRobotPool;
        this.tapeDriveService = tapeDriveService;
        this.tapeCatalogService = tapeCatalogService;
        this.MSG_PREFIX = String.format("[Library] : %s, [Drive] : %s, ", tapeRobotPool.getLibraryIdentifier(),
                tapeDriveService.getTapeDriveConf().getIndex());
    }

    @Override
    public ReadWriteResult get() {
        try {
            if (null != workerCurrentTape && !workerCurrentTape.getCode().equals(readOrder.getTapeCode())) {
                // Unload current tape
                TapeResponse tapeResponse = unloadTape();
                switch (tapeResponse.getStatus()) {
                    case KO:
                    case WARNING: // FIXME: 21/03/19 revoir le traitement de ce cas
                        return new ReadWriteResult(QueueState.READY, workerCurrentTape);
                    case FATAL:
                        return new ReadWriteResult(QueueState.ERROR, workerCurrentTape);
                }
                workerCurrentTape = null;
            }

            if (workerCurrentTape == null) {
                // Find tape
                // If tape not found WARN (return TAR to queue and continue)
                // If tape ok load tape to drive
                // Do status to get tape TYPE and some other information (update catalog)
                CatalogResponse catalogResponse = getTapeFromCatalog();
                switch (catalogResponse.getStatus()) {
                    case KO:
                        return new ReadWriteResult(QueueState.READY, workerCurrentTape);
                    case FATAL:
                        return new ReadWriteResult(QueueState.ERROR, workerCurrentTape);
                }

                TapeResponse tapeResponse = loadTape();
                switch (tapeResponse.getStatus()) {
                    case KO:
                    case WARNING: // FIXME: 21/03/19 revoir le traitement de ce cas
                        return new ReadWriteResult(QueueState.READY, workerCurrentTape);
                    case FATAL:
                        return new ReadWriteResult(QueueState.ERROR, workerCurrentTape);
                }
            }

            return doRead();
        } finally {
            done.set(true);
        }
    }

    @Override
    public ReadWriteResult get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        // FIXME : What about background task still running after timeout
        return CompletableFuture
                .supplyAsync(() -> get(), VitamThreadPoolExecutor.getDefaultExecutor()).get(timeout, unit);
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

    private ReadWriteResult doRead() {

        boolean occuredError;
        // move drive to the given position
        int offset = readOrder.getFilePosition();
        boolean toRewind = true;
        if(workerCurrentTape.getCurrentPosition() != null) {
            toRewind = false;
            offset = readOrder.getFilePosition() - workerCurrentTape.getCurrentPosition();
        }

        TapeResponse response = moveToPosition(toRewind, offset);
        occuredError = !StatusCode.OK.equals(response.getStatus());

        // read file from tape
        if (!occuredError) {
            response = readFromTape();
            occuredError = !StatusCode.OK.equals(response.getStatus());
        }

        if (occuredError) {
            // retry
            int nbRetry = 2;
            while (nbRetry != 0 && occuredError) {
                try {
                    Thread.sleep(TIMEOUT_IN_MILLISECONDES);
                } catch (InterruptedException e) {
                    SysErrLogger.FAKE_LOGGER.ignoreLog(e);
                }

                nbRetry--;

                // Rewind
                response = moveToPosition(true, readOrder.getFilePosition());
                occuredError = !StatusCode.OK.equals(response.getStatus());
                if (occuredError) {
                    continue;
                }

                response = readFromTape();
                occuredError = !StatusCode.OK.equals(response.getStatus());
                if (occuredError) {
                    continue;
                }
            }
        }

        switch (response.getStatus()) {
            case KO:
            case WARNING: // FIXME: 21/03/19 revoir le traitement de ce cas
                return new ReadWriteResult(QueueState.READY, workerCurrentTape);
            case FATAL:
                return new ReadWriteResult(QueueState.ERROR, workerCurrentTape);
            default:
                return new ReadWriteResult(QueueState.COMPLETED, workerCurrentTape, response);
        }
    }

    private TapeResponse moveToPosition(boolean rewind, int offset) {

        TapeResponse response = new TapeResponse(StatusCode.OK);
        if (rewind) {
            response = tapeDriveService.getDriveCommandService().rewind();
            if (!StatusCode.OK.equals(response.getStatus())) {
                LOGGER.error(MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode() +
                        " Action : Rewind Tape, Order: " + JsonHandler.unprettyPrint(readOrder) + ", Entity: " +
                        JsonHandler.unprettyPrint(response.getEntity()));

                return response;
            }
        }

        if (offset != 0) {
            response = tapeDriveService.getDriveCommandService().goToPosition(Math.abs(offset) - 1, offset < 0);
            if (!StatusCode.OK.equals(response.getStatus())) {
                LOGGER.error(MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode() +
                        " Action : Go to position Tape, Order: " + JsonHandler.unprettyPrint(readOrder) + ", Entity: " +
                        JsonHandler.unprettyPrint(response.getEntity()));
            }
        }

        return response;

    }

    private TapeResponse readFromTape() {
        TapeResponse response = tapeDriveService.getReadWriteService(TapeDriveService.ReadWriteCmd.DD)
                .readFromTape(readOrder.getFileName());

        if (!StatusCode.OK.equals(response.getStatus())) {
            LOGGER.error(MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode() +
                    " Action : Read, Order: " + JsonHandler.unprettyPrint(readOrder) + ", Entity: " +
                    JsonHandler.unprettyPrint(response.getEntity()));
        }

        return response;
    }

    /**
     * Get eligible tape from catalog
     *
     * @return
     */
    private CatalogResponse getTapeFromCatalog() {
        Bson query = eq(TapeCatalog.CODE, readOrder.getTapeCode());

        try {
            Optional<TapeCatalog> found = tapeCatalogService.receive(query, QueueMessageType.TapeCatalog);
            if (!found.isPresent()) {
                List<TapeCatalog> tapes = tapeCatalogService.find(Arrays.asList(new QueryCriteria(TapeCatalog.CODE, readOrder.getTapeCode(), QueryCriteriaOperator.EQ)));
                if (tapes.size() == 0) {
                    return new CatalogResponse(StatusCode.FATAL);
                } else {
                    return new CatalogResponse(StatusCode.KO);
                }
            }
            if (found.get().getCurrentLocation().getLocationType().getType().equals(TapeLocationType.OUTSIDE)) {
               LOGGER.error(MSG_PREFIX +
                        " Action : LoadTapeFromCatalog, Order: " + JsonHandler.unprettyPrint(readOrder) +
                        ", Error: no tape found in the catalog with expected library and/or bucket",
                        ReadWriteErrorCode.TAPE_NOT_FOUND_IN_CATALOG);
                return new CatalogResponse(StatusCode.FATAL);
            }

            return new CatalogResponse(StatusCode.OK, found.get());

        } catch (QueueException | TapeCatalogException e) {
            return new CatalogResponse(StatusCode.KO);
        }
    }

    /**
     * Load current tape to drive
     */
    private TapeResponse loadTape() {

        TapeResponse response;
        Integer driveIndex = tapeDriveService.getTapeDriveConf().getIndex();
        Integer slotIndex;
        if (null == workerCurrentTape.getCurrentLocation()) {
            LOGGER.error(MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode() +
                    ", Error: tape current location is null. please update catalog",
                    ReadWriteErrorCode.TAPE_LOCATION_CONFLICT);
            return new TapeResponse(ReadWriteErrorCode.TAPE_LOCATION_CONFLICT, StatusCode.FATAL);
        }

        slotIndex = workerCurrentTape.getCurrentLocation().getIndex();

        try {
            response = tapeRobotPool.checkoutRobotService().getLoadUnloadService()
                            .loadTape(slotIndex, driveIndex);

            if (StatusCode.OK.equals(response.getStatus())) {
                // rewind
                response = moveToPosition(true, 0);
            }

            if (StatusCode.OK.equals(response.getStatus())) {
                // update catalog
                workerCurrentTape.setCurrentPosition(0);
                updateTapeLocation(new TapeLocation(driveIndex, TapeLocationType.DIRVE));
            }

        } catch (InterruptedException e) {
            LOGGER.error(MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode(), e);
            response = new TapeResponse(e.getMessage(), StatusCode.FATAL);
        } catch (TapeCatalogException e) {
            LOGGER.error(MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode(), e);
            response = new TapeResponse(e.getMessage(), StatusCode.KO);
        }

        return response;
    }

    /**
     * Unload tape from  drive
     */
    private TapeResponse unloadTape() {

        Integer driveIndex = workerCurrentTape.getCurrentLocation().getIndex();
        Integer slotIndex;
        TapeResponse response;

        switch (workerCurrentTape.getPreviousLocation().getLocationType()) {
            case SLOT:
                slotIndex = workerCurrentTape.getPreviousLocation().getIndex();
                break;
            case DIRVE:
            case OUTSIDE:
            case IMPORTEXPORT:
                LOGGER.error(MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode() +
                        ", Error: previous location should no be in " +
                        workerCurrentTape.getPreviousLocation().getLocationType().getType(),
                        ReadWriteErrorCode.TAPE_LOCATION_CONFLICT);
                return new TapeResponse(ReadWriteErrorCode.TAPE_LOCATION_CONFLICT, StatusCode.FATAL);
            default:
                LOGGER.error(
                        MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode() + ", Error: location type not implemented");
                return new TapeResponse(ReadWriteErrorCode.TAPE_LOCATION_UNKNOWN, StatusCode.FATAL);
        }

        try {
            response = tapeRobotPool.checkoutRobotService().getLoadUnloadService()
                    .unloadTape(slotIndex, driveIndex);

            // update catalog
            updateTapeLocation(new TapeLocation(slotIndex, TapeLocationType.SLOT));

            // release the tape
            tapeCatalogService.markReady(workerCurrentTape.getId());

        } catch (InterruptedException e) {
            LOGGER.error(MSG_PREFIX + TAPE_MSG + workerCurrentTape.getCode(), e);
            response = new TapeResponse(e.getMessage(), StatusCode.FATAL);
        } catch (TapeCatalogException | QueueException e) {
            response = new TapeResponse(e.getMessage(), StatusCode.KO);
        }

        return response;
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
