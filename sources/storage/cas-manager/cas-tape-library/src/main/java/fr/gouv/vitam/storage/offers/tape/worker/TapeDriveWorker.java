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
import java.util.concurrent.TimeUnit;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.performance.PerformanceLogger;
import fr.gouv.vitam.common.storage.tapelibrary.ReadWritePriority;
import fr.gouv.vitam.storage.engine.common.model.QueueEntity;
import fr.gouv.vitam.storage.engine.common.model.TapeCatalog;
import fr.gouv.vitam.storage.offers.tape.exception.QueueException;
import fr.gouv.vitam.storage.offers.tape.order.Order;
import fr.gouv.vitam.storage.offers.tape.order.ReadOrder;
import fr.gouv.vitam.storage.offers.tape.order.WriteOrder;
import fr.gouv.vitam.storage.offers.tape.spec.QueueRepository;
import fr.gouv.vitam.storage.offers.tape.spec.TapeCatalogService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeDriveService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeRobotPool;
import fr.gouv.vitam.storage.offers.tape.worker.tasks.ReadWriteResult;
import fr.gouv.vitam.storage.offers.tape.worker.tasks.ReadWriteTask;
import org.apache.commons.lang3.time.StopWatch;

public class TapeDriveWorker implements Runnable {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(TapeDriveWorker.class);
    public static final int SLEEP_TIME = 1000;

    private final QueueRepository readWriteQueue;
    private final TapeRobotPool tapeRobotPool;
    private final TapeDriveService tapeDriveService;
    private final TapeCatalogService tapeCatalogService;
    private volatile boolean stop = false;
    private volatile boolean died = false;
    private ReadWriteResult previousReadWriteResult;

    public TapeDriveWorker(
        TapeRobotPool tapeRobotPool,
        TapeDriveService tapeDriveService,
        TapeCatalogService tapeCatalogService,
        QueueRepository readWriteQueue
    ) {
        this.tapeCatalogService = tapeCatalogService;
        ParametersChecker
            .checkParameter("All params is required required", tapeRobotPool, tapeDriveService, readWriteQueue,
                readWriteQueue);
        this.tapeRobotPool = tapeRobotPool;
        this.tapeDriveService = tapeDriveService;
        this.readWriteQueue = readWriteQueue;
    }

    @Override
    public void run() {
        try {
            final StopWatch exceptionStopWatch = StopWatch.createStarted();
            final StopWatch loopStopWatch = StopWatch.createStarted();
            while (!stop) {
                LOGGER.debug("Start take order from queue ");

                Order order = null;
                loopStopWatch.reset();
                loopStopWatch.start();

                try {
                    if (tapeDriveService.getTapeDriveConf().getReadWritePriority() == ReadWritePriority.WRITE) {
                        Optional<WriteOrder> write = readWriteQueue.peek(WriteOrder.class);
                        if (write.isPresent()) {
                            LOGGER.debug("Process write order :" + JsonHandler.unprettyPrint(write.get()));
                            order = write.get();

                        } else {
                            Optional<ReadOrder> read = readWriteQueue.peek(ReadOrder.class);
                            if (read.isPresent()) {
                                LOGGER.debug("Process read order :" + JsonHandler.unprettyPrint(read.get()));
                                order = read.get();
                            }
                        }
                    } else {
                        Optional<ReadOrder> read = readWriteQueue.peek(ReadOrder.class);
                        if (read.isPresent()) {
                            LOGGER.debug("Process read order :" + JsonHandler.unprettyPrint(read.get()));
                            order = read.get();

                        } else {
                            Optional<WriteOrder> write = readWriteQueue.peek(WriteOrder.class);
                            if (write.isPresent()) {
                                LOGGER.debug("Process write order :" + JsonHandler.unprettyPrint(write.get()));
                                order = write.get();

                            }
                        }
                    }

                } catch (QueueException e) {
                    LOGGER.debug("Sleep " + SLEEP_TIME + " ms because of exception : ", e);
                    Thread.sleep(SLEEP_TIME);

                    // Log every one minute
                    if (exceptionStopWatch.getTime(TimeUnit.MINUTES) >= 1) {
                        LOGGER.error(e);
                        exceptionStopWatch.reset();
                        exceptionStopWatch.start();
                    }
                }



                if (order != null) {

                    // Reset StopWatch
                    exceptionStopWatch.reset();

                    TapeCatalog
                        currentTape =
                        (previousReadWriteResult != null) ? previousReadWriteResult.getCurrentTape() : null;
                    ReadWriteTask readWriteTask =
                        new ReadWriteTask(order, currentTape, tapeRobotPool, tapeDriveService, tapeCatalogService);
                    previousReadWriteResult = readWriteTask.get();
                    // TODO: 12/03/19 update catalog
                    // TODO: 11/03/19 update index
                }


                if (order != null) {
                    PerformanceLogger
                        .getInstance().log("STP_Offer_Tape", ((QueueEntity) order).getId(),
                        order.isWriteOrder() ? "WRITE_TO_TAPE" : "READ_FROM_TAPE",
                        loopStopWatch.getTime(TimeUnit.MILLISECONDS));
                }

            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            died = true;
        }
    }


    public void stop() {
        this.stop = true;
        while (!died) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                SysErrLogger.FAKE_LOGGER.ignoreLog(e);
            }
        }
    }


    public void stop(long timeout, TimeUnit timeUnit) {
        StopWatch stopWatch = StopWatch.createStarted();
        this.stop = true;
        while (!died && stopWatch.getTime(timeUnit) < timeout) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                SysErrLogger.FAKE_LOGGER.ignoreLog(e);
            }
        }
    }

}
