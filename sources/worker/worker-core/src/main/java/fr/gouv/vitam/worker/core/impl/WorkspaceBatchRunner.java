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
package fr.gouv.vitam.worker.core.impl;

import com.google.common.collect.Lists;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.model.WorkspaceQueue;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.exception.WorkerspaceQueueException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

/**
 * This task is responsible of sending files to the workspace
 */
public class WorkspaceBatchRunner implements Runnable {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(WorkspaceBatchRunner.class);

    private static final int BATCH_SIZE = 10;

    private BlockingQueue<WorkspaceQueue> queue;
    private HandlerIO handlerIO;
    private Executor executor;
    /**
     * Will have a value false when the method waitEndOfTransfer is called
     * This inform the thread that he have only to execute to pending elements in the queue.
     */
    private volatile boolean doDequeue = true;

    /**
     * When join called make stopped to true
     * This monitor join method to not be called multiple times
     */
    private volatile boolean started = false;
    /**
     * When join called make stopped to true
     * This monitor join method to not be called multiple times
     */
    private volatile boolean stopped = false;

    /**
     * Will complete when exception occurs or when all element of the queue are treated
     */
    private CompletableFuture<Boolean> waitMonitor;

    /**
     * Save the last occurred exception
     */
    private Throwable exceptionOccurred = null;

    public WorkspaceBatchRunner(HandlerIO handlerAsyncIO, Executor executor, int queueSize) {
        ParametersChecker.checkParameter("Parameters mustn't be null", handlerAsyncIO, executor);
        this.waitMonitor = new CompletableFuture<>();
        this.queue = new LinkedBlockingQueue<>(queueSize);
        this.handlerIO = handlerAsyncIO;
        this.executor = executor;
    }

    @Override
    public void run() {
        List<CompletableFuture<Boolean>> completableFutures = new ArrayList<>();
        while (doDequeue || queue.size() > 0) {
            try {
                if (queue.size() > 0) {
                    int realBatchSize = BATCH_SIZE;
                    if (BATCH_SIZE * 5 < queue.size()) {
                        realBatchSize = BATCH_SIZE * 5;
                    } else if (BATCH_SIZE * 5 * 2 < queue.size()) {
                        realBatchSize = BATCH_SIZE * 5 * 2;
                    }

                    while (realBatchSize > 0) {
                        WorkspaceQueue element = queue.poll();
                        if (element != null) {
                            completableFutures.add(
                                CompletableFuture.supplyAsync(
                                    new WorkspaceTransferTask(handlerIO, element),
                                    executor
                                ).exceptionally(e -> {
                                    exceptionOccurred = e;
                                    return Boolean.FALSE;
                                })
                            );
                        }

                        realBatchSize--;
                    }

                    if (completableFutures.size() > 0) {
                        sequence(completableFutures)
                            .exceptionally(e -> {
                                exceptionOccurred = e;
                                return Lists.newArrayList();
                            })
                            .join();
                        completableFutures.clear();
                    }
                }
            } catch (Exception e) {
                LOGGER.error(e);
                queue.clear();
                waitMonitor.complete(Boolean.FALSE);
                doDequeue = false;
                break;
            }
        }
        waitMonitor.complete(Boolean.TRUE);
    }

    /**
     * Enqueue element to be transferred to the workspace
     *
     * @param workspaceQueue
     * @throws ProcessingException
     */
    public void transfer(WorkspaceQueue workspaceQueue) throws WorkerspaceQueueException {
        ParametersChecker.checkParameter("WorkspaceQueue is required", workspaceQueue);
        if (!started) {
            throw new WorkerspaceQueueException("Workspace batch runner not started");
        }

        try {
            queue.put(workspaceQueue);
        } catch (InterruptedException e) {
            LOGGER.error(e);
            throw new WorkerspaceQueueException(e);
        }
    }

    /**
     * Start the workspace batch
     * This method should be called only once
     */
    public void start() throws WorkerspaceQueueException {
        if (started) {
            throw new WorkerspaceQueueException("Workspace batch runner already started");
        }
        started = true;
        this.executor.execute(this);
    }

    /**
     * Wait end of workspace batch
     * this method may be called only once
     */
    public void join() throws WorkerspaceQueueException {
        if (!started) {
            throw new WorkerspaceQueueException("Workspace batch runner already started");
        }

        if (stopped) {
            return;
        }

        stopped = true;
        doDequeue = false;

        try {
            waitMonitor.get();
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error(e);
            throw new WorkerspaceQueueException(e);
        }

        if (null != exceptionOccurred) {
            throw new WorkerspaceQueueException(exceptionOccurred);
        }
    }

    private static <T> CompletableFuture<List<T>> sequence(List<CompletableFuture<T>> futures) {
        CompletableFuture<Void> allDoneFuture = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[futures.size()])
        );
        return allDoneFuture.thenApply(v -> futures.stream().map(CompletableFuture::join).collect(Collectors.toList()));
    }
}
