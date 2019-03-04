package fr.gouv.vitam.storage.offers.tape.worker;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.storage.tapelibrary.ReadWritePriority;
import fr.gouv.vitam.storage.offers.tape.order.Order;
import fr.gouv.vitam.storage.offers.tape.spec.TapeDriveService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeRobotPool;
import fr.gouv.vitam.storage.offers.tape.worker.tasks.ReadWriteResult;
import fr.gouv.vitam.storage.offers.tape.worker.tasks.ReadWriteTask;
import org.apache.commons.lang3.time.StopWatch;

public class TapeDriveWorker implements Runnable {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(TapeDriveWorker.class);

    private final BlockingQueue<Order> writeQueue;
    private final BlockingQueue<Order> readQueue;
    private final TapeRobotPool tapeRobotPool;
    private final TapeDriveService tapeDriveService;
    private volatile boolean stop = false;
    private volatile boolean died = false;

    public TapeDriveWorker(
        TapeRobotPool tapeRobotPool,
        TapeDriveService tapeDriveService,
        BlockingQueue<Order> writeQueue,
        BlockingQueue<Order> readQueue
    ) {
        ParametersChecker
            .checkParameter("All params is required required", tapeRobotPool, tapeDriveService, writeQueue,
                readQueue);
        this.tapeRobotPool = tapeRobotPool;
        this.tapeDriveService = tapeDriveService;
        this.writeQueue = writeQueue;
        this.readQueue = readQueue;
    }

    @Override
    public void run() {
        try {
            while (!stop) {
                LOGGER.debug("Start take order from queue ");

                Order order;
                if (tapeDriveService.getTapeDriveConf().getReadWritePriority() == ReadWritePriority.WRITE) {
                    if (writeQueue.size() > 0 || readQueue.size() == 0) {
                        order = writeQueue.poll(1, TimeUnit.MINUTES);
                    } else {
                        order = readQueue.poll(1, TimeUnit.MINUTES);
                    }
                } else {
                    if (readQueue.size() > 0 || writeQueue.size() == 0) {
                        order = readQueue.poll(1, TimeUnit.MINUTES);
                    } else {
                        order = writeQueue.poll(1, TimeUnit.MINUTES);
                    }
                }

                if (order == null) {
                    continue;
                }

                ReadWriteTask readTask = new ReadWriteTask(order, tapeRobotPool, tapeDriveService);
                ReadWriteResult readResult = readTask.get();
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
