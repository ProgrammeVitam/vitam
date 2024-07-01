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
package fr.gouv.vitam.storage.offers.tape.inmemoryqueue;

import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;

import java.util.concurrent.LinkedBlockingDeque;

public abstract class QueueProcessor<T> {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(QueueProcessor.class);
    private static final int RETRY_DELAY_IN_MILLISECONDS = 60_000;

    private final String workerName;
    private final LinkedBlockingDeque<T> queue = new LinkedBlockingDeque<>();

    protected QueueProcessor(String workerName) {
        this.workerName = workerName;
    }

    public void startListener() {
        VitamThreadPoolExecutor.getDefaultExecutor().execute(this::workerTask);
    }

    public void addToQueue(T message) {
        queue.add(message);
    }

    public void addFirst(T message) {
        queue.addFirst(message);
    }

    private void workerTask() {
        String initialThreadName = Thread.currentThread().getName();
        try {
            Thread.currentThread().setName(workerName);

            processMessages();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.error("Interrupted thread. Shutdown...", e);
        } finally {
            Thread.currentThread().setName(initialThreadName);
        }
    }

    private void processMessages() throws InterruptedException {
        T message = queue.take();

        do {
            try {
                processMessage(message);

                // Take next message
                message = queue.take();
            } catch (RuntimeException ex) {
                throw new IllegalStateException(
                    "Unexpected exception occurred during message processing " +
                    JsonHandler.unprettyPrint(message) +
                    ". Shutting down...",
                    ex
                );
            } catch (QueueProcessingException ex) {
                switch (ex.getRetryPolicy()) {
                    case FATAL_SHUTDOWN:
                        throw new IllegalStateException(
                            "Fatal exception occurred during message processing " +
                            JsonHandler.unprettyPrint(message) +
                            ". Shutting down...",
                            ex
                        );
                    case DROP_MESSAGE:
                        LOGGER.error(
                            "Non recoverable exception occurred during message processing " +
                            JsonHandler.unprettyPrint(message),
                            ex
                        );

                        // Take next message
                        message = queue.take();

                        break;
                    case RETRY:
                        LOGGER.error(
                            "Exception occurred during message processing " +
                            JsonHandler.unprettyPrint(message) +
                            ". Will retry right away",
                            ex
                        );
                        break;
                    default:
                        throw new IllegalStateException("Unexpected retry policy " + ex.getRetryPolicy(), ex);
                }

                Thread.sleep(RETRY_DELAY_IN_MILLISECONDS);
            }
        } while (true);
    }

    protected abstract void processMessage(T message) throws QueueProcessingException, InterruptedException;
}
