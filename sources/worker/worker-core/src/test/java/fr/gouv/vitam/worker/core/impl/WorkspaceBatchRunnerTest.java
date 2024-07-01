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

import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.model.WorkspaceAction;
import fr.gouv.vitam.processing.common.model.WorkspaceQueue;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.exception.WorkerspaceQueueException;
import org.junit.Test;

import java.util.concurrent.Executor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
public class WorkspaceBatchRunnerTest {

    @Test
    public void whenConstructorThenOK() {
        HandlerIO handlerIO = mock(HandlerIOImpl.class);
        Executor executor = mock(Executor.class);
        new WorkspaceBatchRunner(handlerIO, executor, 10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void whenConstructorParamRequriedThenKO() {
        new WorkspaceBatchRunner(null, null, 10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void whenConstructorHandlerIORequriedThenKO() {
        Executor executor = mock(Executor.class);
        new WorkspaceBatchRunner(null, executor, 10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void whenConstructorExecutorRequriedThenKO() {
        HandlerIO handlerIO = mock(HandlerIOImpl.class);
        new WorkspaceBatchRunner(handlerIO, null, 10);
    }

    @Test(expected = WorkerspaceQueueException.class)
    public void whenTransferThenRunnerNotStartedKO() throws WorkerspaceQueueException {
        WorkspaceQueue workspaceQueue = mock(WorkspaceQueue.class);
        HandlerIO handlerIO = mock(HandlerIOImpl.class);
        Executor executor = mock(Executor.class);
        WorkspaceBatchRunner workspaceBatchRunner = new WorkspaceBatchRunner(handlerIO, executor, 10);
        workspaceBatchRunner.transfer(workspaceQueue);
    }

    @Test
    public void whenTransferThenOK() throws ProcessingException, WorkerspaceQueueException {
        HandlerIO handlerIO = mock(HandlerIOImpl.class);
        doAnswer(o -> o).when(handlerIO).transferInputStreamToWorkspace(any(), any(), any(), anyBoolean());
        WorkspaceQueue workspaceQueue = mock(WorkspaceQueue.class);
        when(workspaceQueue.getAction()).thenReturn(WorkspaceAction.TRANSFER);

        WorkspaceBatchRunner workspaceBatchRunner = new WorkspaceBatchRunner(
            handlerIO,
            VitamThreadPoolExecutor.getDefaultExecutor(),
            10
        );

        workspaceBatchRunner.start();
        workspaceBatchRunner.transfer(workspaceQueue);
        workspaceBatchRunner.join();
    }

    @Test(expected = WorkerspaceQueueException.class)
    public void whenJoinThenKO() throws WorkerspaceQueueException {
        HandlerIO handlerIO = mock(HandlerIOImpl.class);
        WorkspaceBatchRunner workspaceBatchRunner = new WorkspaceBatchRunner(
            handlerIO,
            VitamThreadPoolExecutor.getDefaultExecutor(),
            10
        );

        workspaceBatchRunner.join();
    }

    @Test(expected = WorkerspaceQueueException.class)
    public void whenDoubleStartThenOK() throws WorkerspaceQueueException {
        HandlerIO handlerIO = mock(HandlerIOImpl.class);
        WorkspaceBatchRunner workspaceBatchRunner = new WorkspaceBatchRunner(
            handlerIO,
            VitamThreadPoolExecutor.getDefaultExecutor(),
            10
        );
        workspaceBatchRunner.start();
        workspaceBatchRunner.start();
    }
}
