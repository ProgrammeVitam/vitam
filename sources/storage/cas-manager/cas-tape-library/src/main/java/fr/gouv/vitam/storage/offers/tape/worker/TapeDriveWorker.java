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
package fr.gouv.vitam.storage.offers.tape.worker;

import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.performance.PerformanceLogger;
import fr.gouv.vitam.common.retryable.RetryableOnException;
import fr.gouv.vitam.common.retryable.RetryableParameters;
import fr.gouv.vitam.common.storage.tapelibrary.ReadWritePriority;
import fr.gouv.vitam.storage.engine.common.model.QueueMessageEntity;
import fr.gouv.vitam.storage.engine.common.model.QueueState;
import fr.gouv.vitam.storage.engine.common.model.ReadWriteOrder;
import fr.gouv.vitam.storage.engine.common.model.TapeCatalog;
import fr.gouv.vitam.storage.offers.tape.cas.AccessRequestManager;
import fr.gouv.vitam.storage.offers.tape.cas.ArchiveCacheStorage;
import fr.gouv.vitam.storage.offers.tape.cas.ArchiveReferentialRepository;
import fr.gouv.vitam.storage.offers.tape.dto.TapeDriveSpec;
import fr.gouv.vitam.storage.offers.tape.exception.QueueException;
import fr.gouv.vitam.storage.offers.tape.exception.ReadWriteErrorCode;
import fr.gouv.vitam.storage.offers.tape.exception.ReadWriteException;
import fr.gouv.vitam.storage.offers.tape.exception.TapeCatalogException;
import fr.gouv.vitam.storage.offers.tape.impl.readwrite.TapeLibraryServiceImpl;
import fr.gouv.vitam.storage.offers.tape.spec.TapeCatalogService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeDriveService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeLibraryService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeRobotPool;
import fr.gouv.vitam.storage.offers.tape.worker.tasks.ReadWriteResult;
import fr.gouv.vitam.storage.offers.tape.worker.tasks.ReadWriteTask;
import org.apache.commons.lang3.time.StopWatch;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.concurrent.TimeUnit.SECONDS;

public class TapeDriveWorker implements Runnable {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(TapeDriveWorker.class);

    private static final int MAX_ATTEMPTS = 3;
    private static final int RETRY_WAIT_SECONDS = 10;
    private static final int RANDOM_RANGE_SLEEP = 5;

    private static long sleepTime = 10_000;
    private static final long intervalDelayLogInProgressWorker =
        VitamConfiguration.getIntervalDelayLogInProgressWorker();

    private final String msgPrefix;

    private final TapeDriveOrderConsumer receiver;
    private final TapeRobotPool tapeRobotPool;
    private final TapeDriveService tapeDriveService;
    private final TapeLibraryService tapeLibraryService;

    private final TapeCatalogService tapeCatalogService;
    private final ArchiveReferentialRepository archiveReferentialRepository;
    private final AccessRequestManager accessRequestManager;
    private final AtomicBoolean stopRequested = new AtomicBoolean(false);
    private final AtomicBoolean isStopped = new AtomicBoolean(false);
    private final boolean forceOverrideNonEmptyCartridges;
    private ReadWriteResult readWriteResult;
    private final CountDownLatch shutdownSignal;
    private final String inputTarPath;
    private final ArchiveCacheStorage archiveCacheStorage;

    @VisibleForTesting
    public TapeDriveWorker(
        TapeRobotPool tapeRobotPool,
        TapeDriveService tapeDriveService,
        TapeCatalogService tapeCatalogService,
        TapeDriveOrderConsumer receiver,
        ArchiveReferentialRepository archiveReferentialRepository,
        AccessRequestManager accessRequestManager,
        TapeCatalog currentTape,
        String inputTarPath,
        long sleepTime,
        boolean forceOverrideNonEmptyCartridges,
        ArchiveCacheStorage archiveCacheStorage,
        int fullCartridgeDetectionThresholdInMB
    ) {
        ParametersChecker.checkParameter(
            "All params is required required",
            tapeRobotPool,
            tapeDriveService,
            archiveReferentialRepository,
            accessRequestManager,
            tapeCatalogService,
            receiver,
            archiveCacheStorage
        );

        this.archiveReferentialRepository = archiveReferentialRepository;
        this.accessRequestManager = accessRequestManager;
        this.tapeCatalogService = tapeCatalogService;
        this.inputTarPath = inputTarPath;
        this.tapeRobotPool = tapeRobotPool;
        this.tapeDriveService = tapeDriveService;
        this.receiver = receiver;
        this.tapeLibraryService = new TapeLibraryServiceImpl(
            tapeDriveService,
            tapeRobotPool,
            fullCartridgeDetectionThresholdInMB
        );

        this.forceOverrideNonEmptyCartridges = forceOverrideNonEmptyCartridges;
        this.archiveCacheStorage = archiveCacheStorage;

        this.shutdownSignal = new CountDownLatch(1);
        TapeDriveWorker.sleepTime = sleepTime;

        if (null != currentTape) {
            readWriteResult = new ReadWriteResult();
            readWriteResult.setCurrentTape(currentTape);
        }

        this.msgPrefix = String.format(
            "[Library] : %s, [Drive] : %s, ",
            tapeRobotPool.getLibraryIdentifier(),
            tapeDriveService.getTapeDriveConf().getIndex()
        );
    }

    public TapeDriveWorker(
        TapeRobotPool tapeRobotPool,
        TapeDriveService tapeDriveService,
        TapeCatalogService tapeCatalogService,
        TapeDriveOrderConsumer receiver,
        ArchiveReferentialRepository archiveReferentialRepository,
        AccessRequestManager accessRequestManager,
        TapeCatalog currentTape,
        String inputTarPath,
        boolean forceOverrideNonEmptyCartridges,
        ArchiveCacheStorage archiveCacheStorage,
        int fullCartridgeDetectionThresholdInMB
    ) {
        this(
            tapeRobotPool,
            tapeDriveService,
            tapeCatalogService,
            receiver,
            archiveReferentialRepository,
            accessRequestManager,
            currentTape,
            inputTarPath,
            sleepTime,
            forceOverrideNonEmptyCartridges,
            archiveCacheStorage,
            fullCartridgeDetectionThresholdInMB
        );
    }

    @Override
    public void run() {
        try {
            StopWatch exceptionStopWatch = null;
            final StopWatch loopStopWatch = StopWatch.createStarted();
            final StopWatch inProgressWorkerStopWatch = StopWatch.createStarted();
            while (!stopRequested.get()) {
                LOGGER.debug(msgPrefix + "Start take readWriteOrder from queue ");

                ReadWriteOrder readWriteOrder = null;
                loopStopWatch.reset();
                loopStopWatch.start();

                try {
                    Optional<? extends ReadWriteOrder> order = receiver.consume(this);

                    if (order.isPresent()) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug(msgPrefix + "Process write order :" + JsonHandler.unprettyPrint(order.get()));
                        }
                        readWriteOrder = order.get();
                    }
                } catch (QueueException e) {
                    if (null == exceptionStopWatch) {
                        LOGGER.error(e);
                        exceptionStopWatch = StopWatch.createStarted();
                    } else {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug(msgPrefix + "Sleep " + sleepTime + " ms because of exception : ", e);
                        }
                        TimeUnit.MILLISECONDS.sleep(sleepTime);

                        // Log every one minute
                        if (exceptionStopWatch.getTime(TimeUnit.MINUTES) >= 1) {
                            LOGGER.error(e);
                            exceptionStopWatch.reset();
                            exceptionStopWatch.start();
                        }
                    }
                }

                if (readWriteOrder != null) {
                    // Reset StopWatch
                    exceptionStopWatch = null;

                    TapeCatalog currentTape = (readWriteResult != null) ? readWriteResult.getCurrentTape() : null;

                    ReadWriteTask readWriteTask = new ReadWriteTask(
                        readWriteOrder,
                        currentTape,
                        tapeLibraryService,
                        tapeCatalogService,
                        archiveReferentialRepository,
                        accessRequestManager,
                        inputTarPath,
                        forceOverrideNonEmptyCartridges,
                        archiveCacheStorage
                    );
                    readWriteResult = readWriteTask.get();

                    currentTape = readWriteResult.getCurrentTape();

                    QueueState orderState = readWriteResult.getOrderState();
                    final String orderId = readWriteOrder.getId();

                    switch (orderState) {
                        case ERROR:
                            // Mark order as error state
                            retryable().exec(() -> receiver.getQueue().markError(orderId));
                            break;
                        case READY:
                            // Re-enqueue order
                            retryable().exec(() -> receiver.getQueue().markReady(orderId));
                            break;
                        case COMPLETED:
                            // Remove order from queue
                            retryable().exec(() -> receiver.getQueue().remove(orderId));
                            break;
                        default:
                            throw new IllegalStateException(
                                msgPrefix + "Order should have state Completed, Ready or Error"
                            );
                    }

                    PerformanceLogger.getInstance()
                        .log(
                            "STP_Offer_Tape",
                            ((QueueMessageEntity) readWriteOrder).getId(),
                            readWriteOrder.isWriteOrder() ? "WRITE_TO_TAPE" : "READ_FROM_TAPE",
                            loopStopWatch.getTime(TimeUnit.MILLISECONDS)
                        );

                    if (StatusCode.FATAL.equals(readWriteResult.getStatus())) {
                        throw new VitamRuntimeException(
                            String.format(
                                "[Library] : %s, [Drive] : %s, [Tape]: %s, is stopped because of FATAL status when executing order: %s",
                                tapeRobotPool.getLibraryIdentifier(),
                                tapeDriveService.getTapeDriveConf().getIndex(),
                                currentTape == null ? "No active tape" : currentTape.getCode(),
                                JsonHandler.unprettyPrint(readWriteOrder)
                            )
                        );
                    }
                } else {
                    // Log every
                    String msg = msgPrefix + "No read/write to tape order found. waiting (" + sleepTime + ") ms ...";
                    if (inProgressWorkerStopWatch.getTime(TimeUnit.MILLISECONDS) >= intervalDelayLogInProgressWorker) {
                        inProgressWorkerStopWatch.reset();
                        inProgressWorkerStopWatch.start();
                        LOGGER.warn(msg);
                    } else {
                        LOGGER.debug(msg);
                    }

                    TimeUnit.MILLISECONDS.sleep(sleepTime);
                }
            }
        } catch (Throwable e) {
            LOGGER.error(msgPrefix + " Worker FAILED with error", e);
        } finally {
            this.isStopped.set(true);
            this.shutdownSignal.countDown();
        }
    }

    private RetryableOnException<Long, QueueException> retryable() {
        return new RetryableOnException<>(
            new RetryableParameters(MAX_ATTEMPTS, RETRY_WAIT_SECONDS, RETRY_WAIT_SECONDS, RANDOM_RANGE_SLEEP, SECONDS)
        );
    }

    public void stop() {
        LOGGER.warn(
            String.format(
                "[Library] : %s, [Drive] : %s, stopping ....",
                tapeRobotPool.getLibraryIdentifier(),
                tapeDriveService.getTapeDriveConf().getIndex()
            )
        );
        this.stopRequested.compareAndSet(false, true);
        try {
            this.shutdownSignal.await();
        } catch (InterruptedException e) {
            LOGGER.error(e);
            Thread.currentThread().interrupt();
        }
        LOGGER.warn(
            String.format(
                "[Library] : %s, [Drive] : %s, stopped ....",
                tapeRobotPool.getLibraryIdentifier(),
                tapeDriveService.getTapeDriveConf().getIndex()
            )
        );
    }

    public void stop(long timeout, TimeUnit timeUnit) throws TimeoutException {
        LOGGER.warn(
            String.format(
                "[Library] : %s, [Drive] : %s, stopping ....",
                tapeRobotPool.getLibraryIdentifier(),
                tapeDriveService.getTapeDriveConf().getIndex()
            )
        );

        this.stopRequested.compareAndSet(false, true);
        try {
            if (!this.shutdownSignal.await(timeout, timeUnit)) {
                throw new TimeoutException(
                    String.format(
                        "[Library] : %s, [Drive] : %s, Stopping drive worker took too long (timeout %d %s)....",
                        tapeRobotPool.getLibraryIdentifier(),
                        tapeDriveService.getTapeDriveConf().getIndex(),
                        timeout,
                        timeUnit
                    )
                );
            }
        } catch (InterruptedException e) {
            LOGGER.error(e);
            Thread.currentThread().interrupt();
        }

        LOGGER.warn(
            String.format(
                "[Library] : %s, [Drive] : %s, stopped ....",
                tapeRobotPool.getLibraryIdentifier(),
                tapeDriveService.getTapeDriveConf().getIndex()
            )
        );
    }

    public boolean isRunning() {
        return !isStopped.get();
    }

    public int getIndex() {
        return tapeDriveService.getTapeDriveConf().getIndex();
    }

    ReadWritePriority getPriority() {
        return tapeDriveService.getTapeDriveConf().getReadWritePriority();
    }

    ReadWriteResult getReadWriteResult() {
        return readWriteResult;
    }

    TapeCatalog getCurrentTape() {
        return readWriteResult == null ? null : readWriteResult.getCurrentTape();
    }

    public void initializeOnBootstrap() throws ReadWriteException, TapeCatalogException {
        TapeCatalog currentTape = getCurrentTape();

        LOGGER.info(this.msgPrefix + "Checking drive " + tapeLibraryService.getDriveIndex() + " on bootstrap");

        // Checking access to drive using a status command
        TapeDriveSpec driveStatus = tapeLibraryService.getDriveStatus(ReadWriteErrorCode.KO_ON_STATUS);

        // Check for drives having an ejected tape
        if (currentTape != null && !driveStatus.driveHasTape()) {
            throw new IllegalStateException(
                this.msgPrefix +
                "Drive " +
                getIndex() +
                " should contain a tape " +
                currentTape.getCode() +
                " but no tape found in drive. Possible drive ejected but not unloaded?"
            );
        }

        if (currentTape == null) {
            LOGGER.info(this.msgPrefix + "Drive " + tapeLibraryService.getDriveIndex() + " is empty on bootstrap");
            return;
        }

        LOGGER.warn(
            this.msgPrefix +
            "Found a tape " +
            currentTape.getCode() +
            " with " +
            currentTape.getTapeState() +
            " state on drive " +
            tapeLibraryService.getDriveIndex() +
            " on bootstrap"
        );

        // Rewind tape
        tapeLibraryService.rewindTape(currentTape);

        switch (currentTape.getTapeState()) {
            case EMPTY:
                LOGGER.info(this.msgPrefix + "Ensuring tape is empty...");
                tapeLibraryService.ensureTapeIsEmpty(currentTape, forceOverrideNonEmptyCartridges);
                break;
            case OPEN:
            case FULL:
                LOGGER.info(this.msgPrefix + "Read and validate tape label...");
                tapeLibraryService.checkNonEmptyTapeLabel(currentTape);
                break;
            case CONFLICT:
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + currentTape.getTapeState());
        }

        this.tapeCatalogService.replace(currentTape);
    }

    @VisibleForTesting
    public static void updateInactivitySleepDelayForTesting() {
        sleepTime = 100;
    }
}
