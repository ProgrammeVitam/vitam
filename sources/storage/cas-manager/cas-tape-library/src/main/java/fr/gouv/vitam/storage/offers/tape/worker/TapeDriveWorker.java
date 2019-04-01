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
package fr.gouv.vitam.storage.offers.tape.worker;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.performance.PerformanceLogger;
import fr.gouv.vitam.common.storage.tapelibrary.ReadWritePriority;
import fr.gouv.vitam.storage.engine.common.model.QueueMessageEntity;
import fr.gouv.vitam.storage.engine.common.model.QueueState;
import fr.gouv.vitam.storage.engine.common.model.ReadWriteOrder;
import fr.gouv.vitam.storage.engine.common.model.TapeCatalog;
import fr.gouv.vitam.storage.offers.tape.cas.TarReferentialRepository;
import fr.gouv.vitam.storage.offers.tape.exception.QueueException;
import fr.gouv.vitam.storage.offers.tape.retry.Retry;
import fr.gouv.vitam.storage.offers.tape.spec.TapeCatalogService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeDriveService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeRobotPool;
import fr.gouv.vitam.storage.offers.tape.worker.tasks.ReadWriteResult;
import fr.gouv.vitam.storage.offers.tape.worker.tasks.ReadWriteTask;
import org.apache.commons.lang3.time.StopWatch;

public class TapeDriveWorker implements Runnable {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(TapeDriveWorker.class);
    public static final int SLEEP_TIME = 1000;
    public static final int MAX_ATTEMPTS = 3;
    public static final int RETRY_WAIT_SECONDS = 1000;

    private final TapeDriveOrderConsumer receiver;
    private final TapeRobotPool tapeRobotPool;
    private final TapeDriveService tapeDriveService;
    private final TapeCatalogService tapeCatalogService;
    private final TarReferentialRepository tarReferentialRepository;
    private final AtomicBoolean stop = new AtomicBoolean(false);
    private ReadWriteResult readWriteResult;
    private final CountDownLatch shutdownSignal;
    private String inputTarPath;

    public TapeDriveWorker(
        TapeRobotPool tapeRobotPool,
        TapeDriveService tapeDriveService,
        TapeCatalogService tapeCatalogService,
        TapeDriveOrderConsumer receiver,
        TarReferentialRepository tarReferentialRepository,
        TapeCatalog currentTape,
        String inputTarPath) {
        ParametersChecker
            .checkParameter("All params is required required", tapeRobotPool, tapeDriveService,
                tarReferentialRepository, tapeCatalogService,
                receiver);

        this.tarReferentialRepository = tarReferentialRepository;
        this.tapeCatalogService = tapeCatalogService;
        this.inputTarPath = inputTarPath;
        this.tapeRobotPool = tapeRobotPool;
        this.tapeDriveService = tapeDriveService;
        this.receiver = receiver;
        this.shutdownSignal = new CountDownLatch(1);

        if (null != currentTape) {
            readWriteResult = new ReadWriteResult();
            readWriteResult.setCurrentTape(currentTape);
        }
    }

    @Override
    public void run() {
        try {
            StopWatch exceptionStopWatch = null;
            final StopWatch loopStopWatch = StopWatch.createStarted();
            while (!stop.get()) {
                LOGGER.debug("Start take readWriteOrder from queue ");

                ReadWriteOrder readWriteOrder = null;
                loopStopWatch.reset();
                loopStopWatch.start();

                try {
                    Optional<? extends ReadWriteOrder> order = receiver.consume(this);

                    if (order.isPresent()) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Process write order :" + JsonHandler.unprettyPrint(order.get()));
                        }
                        readWriteOrder = order.get();
                    }

                } catch (QueueException e) {
                    if (null == exceptionStopWatch) {
                        LOGGER.error(e);
                        exceptionStopWatch = StopWatch.createStarted();

                    } else {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Sleep " + SLEEP_TIME + " ms because of exception : ", e);
                        }
                        Thread.sleep(SLEEP_TIME);

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

                    TapeCatalog
                        currentTape =
                        (readWriteResult != null) ? readWriteResult.getCurrentTape() : null;

                    ReadWriteTask readWriteTask =
                        new ReadWriteTask(readWriteOrder, currentTape, tapeRobotPool, tapeDriveService,
                            tapeCatalogService, tarReferentialRepository, inputTarPath);
                    readWriteResult = readWriteTask.get();


                    QueueState orderState = readWriteResult.getOrderState();
                    final String orderId = readWriteOrder.getId();


                    Retry<Long> retry = new Retry<>(MAX_ATTEMPTS, RETRY_WAIT_SECONDS);
                    switch (orderState) {
                        case ERROR:
                            // Mark order as error state
                            retry.execute(() -> receiver.getQueue().markError(orderId));
                            break;

                        case READY:
                            // Re-enqueue order
                            retry.execute(() -> receiver.getQueue().markReady(orderId));
                            break;

                        case COMPLETED:
                            // Remove order from queue
                            retry.execute(() -> receiver.getQueue().remove(orderId));
                            break;

                        default:
                            throw new IllegalStateException("Order should have state Completed, Ready or Error");
                    }

                    PerformanceLogger
                        .getInstance().log("STP_Offer_Tape", ((QueueMessageEntity) readWriteOrder).getId(),
                        readWriteOrder.isWriteOrder() ? "WRITE_TO_TAPE" : "READ_FROM_TAPE",
                        loopStopWatch.getTime(TimeUnit.MILLISECONDS));
                } else {
                    Thread.sleep(10);
                }

            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            this.shutdownSignal.countDown();
        }
    }


    public void stop() {
        this.stop.compareAndSet(false, true);
        try {
            this.shutdownSignal.await();
        } catch (InterruptedException e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }
    }


    public void stop(long timeout, TimeUnit timeUnit) {
        this.stop.compareAndSet(false, true);
        try {
            this.shutdownSignal.await(timeout, timeUnit);
        } catch (InterruptedException e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }
    }

    public boolean isRunning() {
        return shutdownSignal.getCount() != 0;
    }

    public int getIndex() {
        return tapeDriveService.getTapeDriveConf().getIndex();
    }


    public ReadWritePriority getPriority() {
        return tapeDriveService.getTapeDriveConf().getReadWritePriority();
    }

    public ReadWriteResult getReadWriteResult() {
        return readWriteResult;
    }

    public TapeCatalog getCurrentTape() {
        return readWriteResult == null ? null : readWriteResult.getCurrentTape();
    }
}
