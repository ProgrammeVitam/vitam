package fr.gouv.vitam.storage.offers.tape.worker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.thread.VitamThreadFactory;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.storage.engine.common.model.QueueEntity;
import fr.gouv.vitam.storage.offers.tape.impl.queue.QueueException;
import fr.gouv.vitam.storage.offers.tape.order.Order;
import fr.gouv.vitam.storage.offers.tape.order.ReadOrder;
import fr.gouv.vitam.storage.offers.tape.spec.QueueRepository;
import fr.gouv.vitam.storage.offers.tape.spec.TapeDriveService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeLibraryPool;

public class TapeDriveWorkerManager {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(TapeDriveWorkerManager.class);
    public static final String TAPE_DRIVE_WORKER = "TapeDriveWorker_";
    private final QueueRepository readWriteQueue;
    private final List<TapeDriveWorker> workers;

    public TapeDriveWorkerManager(
        QueueRepository readWriteQueue,
        TapeLibraryPool tapeLibraryPool) {

        ParametersChecker
            .checkParameter("All params is required required", tapeLibraryPool, readWriteQueue);
        this.readWriteQueue = readWriteQueue;
        this.workers = new ArrayList<>();

        for (Map.Entry<Integer, TapeDriveService> driveEntry : tapeLibraryPool.drives()) {
            final TapeDriveWorker tapeDriveWorker =
                new TapeDriveWorker(tapeLibraryPool, driveEntry.getValue(), readWriteQueue);
            workers.add(tapeDriveWorker);

            final Thread thread =
                VitamThreadFactory.getInstance().newThread(tapeDriveWorker);
            thread.setName(TAPE_DRIVE_WORKER + driveEntry.getKey());
            thread.start();
            LOGGER.debug("Start worker :" + thread.getName());

        }
    }

    public <T> void enqueue(QueueEntity entity, Class<T> clazz) throws QueueException {
        this.readWriteQueue.add(entity, clazz);
    }


    public void shutdown() {
        List<CompletableFuture> completableFutures = new ArrayList<>();
        workers.forEach(w -> completableFutures
            .add(CompletableFuture.runAsync(() -> w.stop(), VitamThreadPoolExecutor.getDefaultExecutor())));
        CompletableFuture.allOf(completableFutures.toArray(new CompletableFuture[workers.size()])).join();
    }


    public void shutdown(long timeout, TimeUnit timeUnit) {
        List<CompletableFuture> completableFutures = new ArrayList<>();
        workers.forEach(w -> completableFutures
            .add(CompletableFuture
                .runAsync(() -> w.stop(timeout, timeUnit), VitamThreadPoolExecutor.getDefaultExecutor())));
        CompletableFuture.allOf(completableFutures.toArray(new CompletableFuture[workers.size()])).join();
    }
}
