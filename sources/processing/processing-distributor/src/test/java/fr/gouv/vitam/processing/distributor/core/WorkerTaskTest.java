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
package fr.gouv.vitam.processing.distributor.core;

import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.processing.Action;
import fr.gouv.vitam.common.model.processing.ActionDefinition;
import fr.gouv.vitam.common.model.processing.ProcessBehavior;
import fr.gouv.vitam.common.model.processing.Step;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.tmp.TempFolderRule;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.processing.common.model.WorkerBean;
import fr.gouv.vitam.processing.common.parameter.DefaultWorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.client.WorkerClient;
import fr.gouv.vitam.worker.client.WorkerClientFactory;
import fr.gouv.vitam.worker.client.exception.WorkerExecutorException;
import fr.gouv.vitam.worker.client.exception.WorkerServerClientException;
import fr.gouv.vitam.worker.client.exception.WorkerUnreachableException;
import fr.gouv.vitam.worker.common.DescriptionStep;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.nio.channels.UnresolvedAddressException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class WorkerTaskTest {

    private static final WorkerBean WORKER_DESCRIPTION;

    static {
        try {
            WORKER_DESCRIPTION = JsonHandler.getFromString(
                "{ \"name\" : \"workername\", \"workerId\":\"workerId\", \"family\" : \"DefaultWorker1\", \"capacity\" : 2, \"storage\" : 100, \"status\" : \"Active\", \"configuration\" : {\"serverHost\" : \"localhost\", \"serverPort\" : \"12345\" } }",
                WorkerBean.class
            );
        } catch (InvalidParseOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public TempFolderRule testFolder = new TempFolderRule();

    @Mock
    private WorkerClientFactory workerClientFactory;

    @Mock
    private WorkerClient workerClient;

    @Before
    public void setup() throws Exception {
        when(workerClientFactory.getClient()).thenReturn(workerClient);

        doNothing().when(workerClient).checkStatus();
    }

    @RunWithCustomExecutor
    @Test
    public void test_worker_task_get_ok() throws Exception {
        DescriptionStep descriptionStep = getDescriptionStep();

        WorkerInformation.getWorkerThreadLocal().get().setWorkerBean(WORKER_DESCRIPTION);

        final WorkerTask task = new WorkerTask(
            descriptionStep,
            0,
            "requestId",
            "contractId",
            "contextId",
            "applicationId",
            workerClientFactory
        );

        when(workerClient.submitStep(eq(descriptionStep))).thenReturn(
            new ItemStatus("item_ok").increment(StatusCode.OK)
        );
        WorkerTaskResult workerTaskResult = task.get();

        assertThat(workerTaskResult).isNotNull();
        assertThat(workerTaskResult.getItemStatus().getItemId()).isEqualTo("item_ok");
        assertThat(workerTaskResult.getItemStatus().getGlobalStatus()).isEqualTo(StatusCode.OK);
        assertThat(workerTaskResult.isProcessed()).isTrue();
        assertThat(workerTaskResult.getWorkerTask()).isEqualTo(task);
    }

    @RunWithCustomExecutor
    @Test(expected = WorkerUnreachableException.class)
    public void test_worker_task_get_then_WorkerUnreachableException() throws Exception {
        DescriptionStep descriptionStep = getDescriptionStep();

        WorkerInformation.getWorkerThreadLocal().get().setWorkerBean(WORKER_DESCRIPTION);

        final WorkerTask task = new WorkerTask(
            descriptionStep,
            0,
            "requestId",
            "contractId",
            "contextId",
            "applicationId",
            workerClientFactory
        );

        when(workerClient.submitStep(eq(descriptionStep))).thenThrow(
            new WorkerServerClientException("Unreachable server")
        );

        doThrow(new UnresolvedAddressException(), new UnresolvedAddressException(), new UnresolvedAddressException())
            .when(workerClient)
            .checkStatus();
        task.get();
    }

    @RunWithCustomExecutor
    @Test(expected = WorkerExecutorException.class)
    public void test_worker_task_get_then_third_checkStatus_ok_thenWorkerExecutorException() throws Exception {
        DescriptionStep descriptionStep = getDescriptionStep();

        WorkerInformation.getWorkerThreadLocal().get().setWorkerBean(WORKER_DESCRIPTION);

        final WorkerTask task = new WorkerTask(
            descriptionStep,
            0,
            "requestId",
            "contractId",
            "contextId",
            "applicationId",
            workerClientFactory
        );

        when(workerClient.submitStep(eq(descriptionStep))).thenThrow(
            new WorkerServerClientException("Unreachable server")
        );

        doThrow(new UnresolvedAddressException(), new UnresolvedAddressException())
            .doNothing()
            .when(workerClient)
            .checkStatus();
        task.get();
    }

    @RunWithCustomExecutor
    @Test
    public void with_completable_feature_test_worker_task_get_then_WorkerUnreachableException() throws Exception {
        DescriptionStep descriptionStep = getDescriptionStep();

        final WorkerTask task = new WorkerTask(
            descriptionStep,
            0,
            "requestId",
            "contractId",
            "contextId",
            "applicationId",
            workerClientFactory
        );

        when(workerClient.submitStep(eq(descriptionStep))).thenThrow(
            new WorkerServerClientException("Unreachable server")
        );

        doThrow(new UnresolvedAddressException(), new UnresolvedAddressException(), new UnresolvedAddressException())
            .when(workerClient)
            .checkStatus();

        WorkerFamilyManager workerFamilyManager = new WorkerFamilyManager("family", 10);
        workerFamilyManager.registerWorker(WORKER_DESCRIPTION);

        try {
            CompletableFuture.supplyAsync(task, workerFamilyManager.getExecutor(false))
                .exceptionally(th -> {
                    assertThat(th.getCause()).isInstanceOf(WorkerUnreachableException.class);
                    workerFamilyManager.unregisterWorker(WORKER_DESCRIPTION.getWorkerId());
                    throw (CompletionException) th;
                })
                .get();

            fail("Should throw exception");
        } catch (ExecutionException e) {
            assertThat(e.getCause()).isInstanceOf(WorkerUnreachableException.class);
        }

        verify(workerClient, times(3)).checkStatus();
    }

    private DescriptionStep getDescriptionStep() {
        DefaultWorkerParameters params = WorkerParametersFactory.newWorkerParameters();
        params.setWorkerGUID(GUIDFactory.newGUID().getId());
        params.setLogbookTypeProcess(LogbookTypeProcess.INGEST);
        final Step step = new Step().setStepName("TEST").setWorkerGroupId("familyId");
        final List<Action> actions = new ArrayList<>();
        final Action action = new Action();
        actions.add(action);

        action.setActionDefinition(
            new ActionDefinition().setActionKey("DummyHandler").setBehavior(ProcessBehavior.NOBLOCKING)
        );
        step.setBehavior(ProcessBehavior.NOBLOCKING).setActions(actions);
        return new DescriptionStep(step, params);
    }
}
