package fr.gouv.vitam.storage.offers.tape.worker.tasks;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.storage.offers.tape.order.ReadOrder;
import fr.gouv.vitam.storage.offers.tape.spec.TapeDriveService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeRobotPool;

public class ReadTask implements Future<ReadWriteResult> {
    protected boolean cancelled = false;
    protected boolean done = false;

    private final TapeRobotPool tapeRobotPool;
    private final TapeDriveService tapeDriveService;
    private final ReadOrder readOrder;

    public ReadTask(ReadOrder readOrder, TapeRobotPool tapeRobotPool, TapeDriveService tapeDriveService) {
        this.readOrder = readOrder;
        this.tapeRobotPool = tapeRobotPool;
        this.tapeDriveService = tapeDriveService;
    }

    @Override
    public ReadWriteResult get() {
        // TODO: 05/03/19 implement read logic

        return null;
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
}
