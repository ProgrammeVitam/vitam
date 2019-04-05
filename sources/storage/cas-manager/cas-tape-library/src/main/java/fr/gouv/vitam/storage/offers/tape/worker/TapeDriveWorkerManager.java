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

import fr.gouv.vitam.common.ParametersChecker;
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
import fr.gouv.vitam.storage.offers.tape.cas.TarReferentialRepository;
import fr.gouv.vitam.storage.offers.tape.exception.QueueException;
import fr.gouv.vitam.storage.offers.tape.spec.QueueRepository;
import fr.gouv.vitam.storage.offers.tape.spec.TapeDriveService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeLibraryPool;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.nin;

public class TapeDriveWorkerManager implements TapeDriveOrderConsumer, TapeDriveOrderProducer {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(TapeDriveWorkerManager.class);
    private static final String TAPE_DRIVE_WORKER = "TapeDriveWorker_";
    private final QueueRepository readWriteQueue;
    private final List<TapeDriveWorker> workers;

    private final Map<Integer, OptimisticDriveResourceStatus> optimisticDriveResourceStatusMap =
        new ConcurrentHashMap<>();

    public TapeDriveWorkerManager(
        QueueRepository readWriteQueue,
        TarReferentialRepository tarReferentialRepository,
        TapeLibraryPool tapeLibraryPool,
        Map<Integer, TapeCatalog> driveTape, String inputTarPath) {

        ParametersChecker
            .checkParameter("All params is required required", tapeLibraryPool, readWriteQueue,
                tarReferentialRepository, driveTape);
        this.readWriteQueue = readWriteQueue;
        this.workers = new ArrayList<>();

        for (Map.Entry<Integer, TapeDriveService> driveEntry : tapeLibraryPool.drives()) {
            final TapeDriveWorker tapeDriveWorker =
                new TapeDriveWorker(tapeLibraryPool, driveEntry.getValue(), tapeLibraryPool.getTapeCatalogService(),
                    this, tarReferentialRepository, driveTape.get(driveEntry.getKey()), inputTarPath);
            workers.add(tapeDriveWorker);

            final Thread thread =
                VitamThreadFactory.getInstance().newThread(tapeDriveWorker);
            thread.setName(TAPE_DRIVE_WORKER + driveEntry.getKey());
            thread.start();
            LOGGER.debug("Start worker :" + thread.getName());

        }
    }

    public void enqueue(QueueMessageEntity entity) throws QueueException {
        // FIXME : Unused
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
    public synchronized Optional<? extends ReadWriteOrder> produce(TapeDriveWorker driveWorker) throws QueueException {

        OptimisticDriveResourceStatus optimisticDriveResourceStatus =
            optimisticDriveResourceStatusMap
                .computeIfAbsent(driveWorker.getIndex(), i -> new OptimisticDriveResourceStatus());

        optimisticDriveResourceStatus.lastBucket =
            driveWorker.getCurrentTape() != null ? driveWorker.getCurrentTape().getBucket() : null;

        optimisticDriveResourceStatus.lastTapeCode =
            driveWorker.getCurrentTape() != null ? driveWorker.getCurrentTape().getCode() : null;

        optimisticDriveResourceStatus.targetBucket = null;
        optimisticDriveResourceStatus.targetTapeCode = null;

        ReadWritePriority readWritePriority = driveWorker.getPriority();

        Optional<? extends ReadWriteOrder> readWriteOrder;
        switch (readWritePriority) {
            case WRITE:
                readWriteOrder = selectReadWriteOrderWithWritePriority(driveWorker);
                break;
            case READ:
                readWriteOrder = selectReadWriteOrderByReadPriority(driveWorker);
                break;
            default:
                throw new IllegalStateException("Unsupported priority " + readWritePriority);
        }

        if (readWriteOrder.isPresent()) {

            if (readWriteOrder.get().isWriteOrder()) {
                WriteOrder writeOrder = (WriteOrder) readWriteOrder.get();
                optimisticDriveResourceStatus.targetBucket = writeOrder.getBucket();
                // We don't know the target tape that will be used
                optimisticDriveResourceStatus.targetTapeCode = null;
            } else {
                ReadOrder readOrder = (ReadOrder) readWriteOrder.get();
                optimisticDriveResourceStatus.targetBucket = readOrder.getBucket();
                optimisticDriveResourceStatus.targetTapeCode = readOrder.getTapeCode();
            }
        }

        return readWriteOrder;
    }

    private Optional<? extends ReadWriteOrder> selectReadWriteOrderWithWritePriority(TapeDriveWorker driveWorker)
        throws QueueException {

        Optional<? extends ReadWriteOrder> order = Optional.empty();

        if (driveWorker.getCurrentTape() != null) {

            order = selectWriteOrderByBucket(driveWorker.getCurrentTape().getBucket());

            if (!order.isPresent()) {
                order = selectReadOrderByTapeCode(driveWorker.getCurrentTape().getCode());
            }
        }

        if (!order.isPresent()) {
            order = selectWriteOrderExcludingActiveBuckets();
        }

        if (!order.isPresent()) {
            order = selectReadOrderExcludingTapeCodes();
        }

        return order;
    }

    private Optional<? extends ReadWriteOrder> selectReadWriteOrderByReadPriority(TapeDriveWorker driveWorker)
        throws QueueException {
        Optional<? extends ReadWriteOrder> order = Optional.empty();

        if (driveWorker.getCurrentTape() != null) {

            order = selectReadOrderByTapeCode(driveWorker.getCurrentTape().getCode());

            if (!order.isPresent()) {
                order = selectWriteOrderByBucket(driveWorker.getCurrentTape().getBucket());
            }
        }

        if (!order.isPresent()) {
            order = selectReadOrderExcludingTapeCodes();
        }

        if (!order.isPresent()) {
            order = selectWriteOrder();
        }

        if (!order.isPresent()) {
            order = selectWriteOrderExcludingActiveBuckets();
        }

        return order;
    }

    private Optional<? extends ReadWriteOrder> selectWriteOrderByBucket(String bucket) throws QueueException {
        return readWriteQueue.receive(eq(WriteOrder.BUCKET, bucket), QueueMessageType.WriteOrder);
    }

    private Optional<? extends ReadWriteOrder> selectWriteOrder() throws QueueException {
        return readWriteQueue.receive(QueueMessageType.WriteOrder);
    }

    private Optional<? extends ReadWriteOrder> selectReadOrderByTapeCode(String tapeCode) throws QueueException {
        return readWriteQueue.receive(
            eq(ReadOrder.TAPE_CODE, tapeCode),
            QueueMessageType.ReadOrder
        );
    }

    private Optional<? extends ReadWriteOrder> selectWriteOrderExcludingActiveBuckets() throws QueueException {

        // TODO: 28/03/19 parallelism (parallel drive by bucket)
        Set<String> activeBuckets =
            Stream.concat(
                this.optimisticDriveResourceStatusMap.values().stream()
                    .map(optimisticDriveResourceStatus -> optimisticDriveResourceStatus.targetBucket),

                this.optimisticDriveResourceStatusMap.values().stream()
                    .map(optimisticDriveResourceStatus -> optimisticDriveResourceStatus.lastBucket)
            )
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return readWriteQueue.receive(
            nin(WriteOrder.BUCKET, activeBuckets),
            QueueMessageType.WriteOrder
        );
    }

    private Optional<? extends ReadWriteOrder> selectReadOrderExcludingTapeCodes() throws QueueException {

        Set<String> activeTapeCodes =
            Stream.concat(
                this.optimisticDriveResourceStatusMap.values().stream()
                    .map(optimisticDriveResourceStatus -> optimisticDriveResourceStatus.targetTapeCode),
                this.optimisticDriveResourceStatusMap.values().stream()
                    .map(optimisticDriveResourceStatus -> optimisticDriveResourceStatus.lastTapeCode)
            )
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return readWriteQueue.receive(
            nin(ReadOrder.TAPE_CODE, activeTapeCodes),
            QueueMessageType.ReadOrder
        );
    }

    private static class OptimisticDriveResourceStatus {

        private String lastTapeCode;
        private String lastBucket;

        private String targetTapeCode;
        private String targetBucket;
    }
}
