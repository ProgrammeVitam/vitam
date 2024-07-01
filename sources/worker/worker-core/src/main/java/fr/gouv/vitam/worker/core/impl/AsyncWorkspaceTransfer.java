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

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.processing.common.model.WorkspaceQueue;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.exception.WorkerspaceQueueException;

import java.util.concurrent.Executor;

/**
 * Send asynchronously files to the workspace
 * Manage double collection,
 * one for managing completable waitMonitor as batch
 * The scond collection is for WorkspaceQueue, information of files to be sent to the workspace
 * <p>
 * If an exception occurs when a completable waitMonitor try to execute a task,
 * this exception will be saved and propagated when the method waitEndOfTransfer is called
 */
public class AsyncWorkspaceTransfer {

    private HandlerIO handlerIO;
    private WorkspaceBatchRunner runner = null;

    public AsyncWorkspaceTransfer(HandlerIO handlerAsyncIO) {
        ParametersChecker.checkParameter("The parameter handlerIO musn't be null", handlerAsyncIO);

        this.handlerIO = handlerAsyncIO;
    }

    public void transfer(WorkspaceQueue workspaceQueue) throws WorkerspaceQueueException {
        if (null == runner) {
            throw new WorkerspaceQueueException(
                "Workspace batch runner is not started, call startTransfer to start it"
            );
        }
        this.runner.transfer(workspaceQueue);
    }

    public void startTransfer(int queueSize) throws WorkerspaceQueueException {
        if (null == runner) {
            if (queueSize < 1) {
                throw new WorkerspaceQueueException("The batch size must be greater than 1");
            }
            final Executor executor = VitamThreadPoolExecutor.getDefaultExecutor();
            this.runner = new WorkspaceBatchRunner(handlerIO, executor, queueSize);
            this.runner.start();
        }
    }

    /**
     * Wait end of transfer. All Completable waitMonitor should be completed
     */
    public void waitEndOfTransfer() throws WorkerspaceQueueException {
        if (null != runner) {
            this.runner.join();
            this.runner = null;
        }
    }
}
