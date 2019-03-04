package fr.gouv.vitam.storage.offers.tape.worker.tasks;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.ws.rs.core.Feature;

import fr.gouv.vitam.storage.offers.tape.impl.TapeDriveManager;
import fr.gouv.vitam.storage.offers.tape.impl.TapeRobotManager;
import fr.gouv.vitam.storage.offers.tape.order.Order;
import fr.gouv.vitam.storage.offers.tape.order.ReadOrder;
import fr.gouv.vitam.storage.offers.tape.order.WriteOrder;
import fr.gouv.vitam.storage.offers.tape.spec.TapeDriveService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeRobotPool;

public class ReadWriteTask implements Future<ReadWriteResult> {

    private final Future<ReadWriteResult> readWriteTask;

    public ReadWriteTask(Order order, TapeRobotPool tapeRobotPool, TapeDriveService tapeDriveService) {
        if (order.isReadOrder()) {
            readWriteTask = new ReadTask((ReadOrder) order, tapeRobotPool, tapeDriveService);
        } else {
            readWriteTask = new WriteTask((WriteOrder) order, tapeRobotPool, tapeDriveService);

        }
    }

    @Override
    public ReadWriteResult get() throws InterruptedException, ExecutionException {
        return readWriteTask.get();
    }

    @Override
    public ReadWriteResult get(long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException {
        return readWriteTask.get(timeout, unit);
    }


    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return readWriteTask.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return readWriteTask.isCancelled();
    }

    @Override
    public boolean isDone() {
        return readWriteTask.isDone();
    }
}
