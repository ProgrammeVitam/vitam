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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.storage.tapelibrary.ReadWritePriority;
import fr.gouv.vitam.common.thread.VitamThreadFactory;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.storage.engine.common.model.QueueMessageEntity;
import fr.gouv.vitam.storage.engine.common.model.QueueMessageType;
import fr.gouv.vitam.storage.engine.common.model.ReadOrder;
import fr.gouv.vitam.storage.engine.common.model.ReadWriteOrder;
import fr.gouv.vitam.storage.engine.common.model.TapeCatalog;
import fr.gouv.vitam.storage.engine.common.model.WriteOrder;
import fr.gouv.vitam.storage.offers.tape.exception.QueueException;
import fr.gouv.vitam.storage.offers.tape.spec.QueueRepository;
import fr.gouv.vitam.storage.offers.tape.spec.TapeDriveService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeLibraryPool;
import org.bson.conversions.Bson;

public class TapeDriveWorkerManager implements TapeDriveOrderConsumer, TapeDriveOrderProducer {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(TapeDriveWorkerManager.class);
    public static final String TAPE_DRIVE_WORKER = "TapeDriveWorker_";
    private final QueueRepository readWriteQueue;
    private final List<TapeDriveWorker> workers;

    // Drive index with current tape
    private final Map<Integer, TapeCatalog> driveWorkerCurrentCatalog = new ConcurrentHashMap<>();


    public TapeDriveWorkerManager(
        QueueRepository readWriteQueue,
        TapeLibraryPool tapeLibraryPool) {

        ParametersChecker
            .checkParameter("All params is required required", tapeLibraryPool, readWriteQueue);
        this.readWriteQueue = readWriteQueue;
        this.workers = new ArrayList<>();

        for (Map.Entry<Integer, TapeDriveService> driveEntry : tapeLibraryPool.drives()) {
            final TapeDriveWorker tapeDriveWorker =
                new TapeDriveWorker(tapeLibraryPool, driveEntry.getValue(), tapeLibraryPool.getTapeCatalogService(),
                    this);
            workers.add(tapeDriveWorker);

            final Thread thread =
                VitamThreadFactory.getInstance().newThread(tapeDriveWorker);
            thread.setName(TAPE_DRIVE_WORKER + driveEntry.getKey());
            thread.start();
            LOGGER.debug("Start worker :" + thread.getName());

        }
    }

    public void enqueue(QueueMessageEntity entity) throws QueueException {
        this.readWriteQueue.add(entity);
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

    @Override
    public QueueRepository getQueue() {
        return readWriteQueue;
    }

    @Override
    public Optional<? extends ReadWriteOrder> consume(TapeDriveWorker driveWorker) throws QueueException {
        return produce(driveWorker);
    }

    @Override
    public Optional<? extends ReadWriteOrder> produce(TapeDriveWorker driveWorker) throws QueueException {

        Integer driveIndex = driveWorker.getIndex();
        ReadWritePriority readWritePriority = driveWorker.getPriority();

        driveWorkerCurrentCatalog.put(driveIndex, driveWorker.getCurrentTape());


        // TODO: 21/03/19 construct optimized query to get order
        Bson query = null;

        if (ReadWritePriority.WRITE.equals(readWritePriority)) {
            Optional<? extends ReadWriteOrder> write = readWriteQueue.receive(query, QueueMessageType.WriteOrder);
            if (write.isPresent()) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Process write order :" + JsonHandler.unprettyPrint(write.get()));
                }
                return write;

            } else {
                Optional<ReadOrder> read = readWriteQueue.receive(query, QueueMessageType.ReadOrder);
                if (read.isPresent()) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Process read order :" + JsonHandler.unprettyPrint(read.get()));
                    }
                    return read;
                }
            }
        } else {
            Optional<ReadOrder> read = readWriteQueue.receive(query, QueueMessageType.ReadOrder);
            if (read.isPresent()) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Process read order :" + JsonHandler.unprettyPrint(read.get()));
                }
                return read;

            } else {
                Optional<WriteOrder> write = readWriteQueue.receive(query, QueueMessageType.WriteOrder);
                if (write.isPresent()) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Process write order :" + JsonHandler.unprettyPrint(write.get()));
                    }
                    return write;
                }
            }
        }

        // Each drive have a current tape
        // Order that should go to the given tape should be produced to the drive that have the tape on


        return null;
    }
}
