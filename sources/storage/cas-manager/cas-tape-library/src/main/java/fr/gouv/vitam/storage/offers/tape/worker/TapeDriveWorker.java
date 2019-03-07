package fr.gouv.vitam.storage.offers.tape.worker;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.storage.tapelibrary.ReadWritePriority;
import fr.gouv.vitam.storage.engine.common.model.TapeCatalog;
import fr.gouv.vitam.storage.offers.tape.impl.queue.QueueException;
import fr.gouv.vitam.storage.offers.tape.order.Order;
import fr.gouv.vitam.storage.offers.tape.order.ReadOrder;
import fr.gouv.vitam.storage.offers.tape.order.WriteOrder;
import fr.gouv.vitam.storage.offers.tape.spec.QueueRepository;
import fr.gouv.vitam.storage.offers.tape.spec.TapeDriveService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeRobotPool;
import fr.gouv.vitam.storage.offers.tape.worker.tasks.ReadWriteResult;
import fr.gouv.vitam.storage.offers.tape.worker.tasks.ReadWriteTask;
import org.apache.commons.lang3.time.StopWatch;

public class TapeDriveWorker implements Runnable {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(TapeDriveWorker.class);

    private final QueueRepository readWriteQueue;
    private final TapeRobotPool tapeRobotPool;
    private final TapeDriveService tapeDriveService;
    private volatile boolean stop = false;
    private volatile boolean died = false;
    private ReadWriteResult previousReadWriteResult;

    public TapeDriveWorker(
        TapeRobotPool tapeRobotPool,
        TapeDriveService tapeDriveService,
        QueueRepository readWriteQueue
    ) {
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
            while (!stop) {
                LOGGER.debug("Start take order from queue ");

                Order order = null;

                try {
                    if (tapeDriveService.getTapeDriveConf().getReadWritePriority() == ReadWritePriority.WRITE) {
                        Optional<WriteOrder> write = readWriteQueue.peek(WriteOrder.class);
                        if (write.isPresent()) {
                            LOGGER.debug("Treat write order :" + JsonHandler.unprettyPrint(write.get()));
                            order = write.get();

                        } else {
                            Optional<ReadOrder> read = readWriteQueue.peek(ReadOrder.class);
                            if (read.isPresent()) {
                                LOGGER.debug("Treat read order :" + JsonHandler.unprettyPrint(read.get()));
                                order = read.get();
                            }
                        }
                    } else {
                        Optional<ReadOrder> read = readWriteQueue.peek(ReadOrder.class);
                        if (read.isPresent()) {
                            LOGGER.debug("Treat read order :" + JsonHandler.unprettyPrint(read.get()));
                            order = read.get();

                        } else {
                            Optional<WriteOrder> write = readWriteQueue.peek(WriteOrder.class);
                            if (write.isPresent()) {
                                LOGGER.debug("Treat write order :" + JsonHandler.unprettyPrint(write.get()));
                                order = write.get();

                            }
                        }
                    }

                } catch (QueueException e) {
                    LOGGER.error(e);
                }



                if (order == null) {
                    continue;
                }

                TapeCatalog
                    currentTape = (previousReadWriteResult != null) ? previousReadWriteResult.getCurrentTape() : null;
                ReadWriteTask readWriteTask = new ReadWriteTask(order, currentTape, tapeRobotPool, tapeDriveService);
                previousReadWriteResult = readWriteTask.get();

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
