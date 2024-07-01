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

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.processing.Action;
import fr.gouv.vitam.common.model.processing.ActionDefinition;
import fr.gouv.vitam.common.model.processing.ProcessBehavior;
import fr.gouv.vitam.common.model.processing.Step;
import fr.gouv.vitam.common.tmp.TempFolderRule;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.processing.common.exception.ProcessingBadRequestException;
import fr.gouv.vitam.processing.common.exception.WorkerFamilyNotFoundException;
import fr.gouv.vitam.processing.common.model.WorkerBean;
import fr.gouv.vitam.processing.common.parameter.DefaultWorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.client.WorkerClient;
import fr.gouv.vitam.worker.client.WorkerClientFactory;
import fr.gouv.vitam.worker.common.DescriptionStep;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static fr.gouv.vitam.processing.distributor.api.IWorkerManager.WORKER_DB_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

public class WorkerManagerTest {

    private static final WorkerBean WORKER_DESCRIPTION;
    private static final WorkerBean WORKER_DESCRIPTION_2;
    private static final WorkerBean BIG_WORKER_DESCRIPTION;

    static {
        try {
            WORKER_DESCRIPTION = JsonHandler.getFromString(
                "{ \"name\" : \"workername\", \"family\" : \"DefaultWorker1\", \"capacity\" : 2, \"storage\" : 100, \"status\" : \"Active\", \"configuration\" : {\"serverHost\" : \"localhost\", \"serverPort\" : \"12345\" } }",
                WorkerBean.class
            );

            WORKER_DESCRIPTION_2 = JsonHandler.getFromString(
                "{ \"name\" : \"workername\", \"family\" : \"DefaultWorker2\", \"capacity\" : 2, \"storage\" : 100, \"status\" : \"Active\", \"configuration\" : {\"serverHost\" : \"localhost\", \"serverPort\" : \"12345\" } }",
                WorkerBean.class
            );

            BIG_WORKER_DESCRIPTION = JsonHandler.getFromString(
                "{ \"name\" : \"workername2\", \"family\" : \"BigWorker\", \"capacity\" : 4, \"storage\" : 100, \"status\" : \"Active\", \"configuration\" : {\"serverHost\" : \"localhost\", \"serverPort\" : \"12345\" } }",
                WorkerBean.class
            );
        } catch (InvalidParseOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public TempFolderRule testFolder = new TempFolderRule();

    private WorkerManager workerManager;

    @Mock
    private WorkerClientFactory workerClientFactory;

    @Mock
    private WorkerClient workerClient;

    private static void cleanWorkerDb() throws IOException {
        Files.deleteIfExists(PropertiesUtils.fileFromDataFolder(WORKER_DB_PATH).toPath());
    }

    @Before
    public void setup() throws Exception {
        when(workerClientFactory.getClient()).thenReturn(workerClient);

        doNothing().when(workerClient).checkStatus();
        workerManager = new WorkerManager(workerClientFactory);
        workerManager.initialize();
    }

    @After
    public void tearDownAfter() throws IOException {
        cleanWorkerDb();
    }

    @Test
    public void givenBigWorkerFamilyAndStepOfBigWorkflowRunningOn() throws Exception {
        final String familyId = "BigWorker";
        final String workerId = "NewWorkerId2";
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
        doReturn(new ItemStatus().increment(StatusCode.OK)).when(workerClient).submitStep(descriptionStep);

        workerManager.registerWorker(familyId, workerId, BIG_WORKER_DESCRIPTION);
        WorkerFamilyManager workerFamilyManager = workerManager.findWorkerBy(familyId);
        assertNotNull(workerFamilyManager);

        WorkerTaskResult workerTaskResult = CompletableFuture.supplyAsync(
            task,
            workerFamilyManager.getExecutor(false)
        ).get();

        assertThat(workerTaskResult).isNotNull();
        assertThat(workerTaskResult.getWorkerTask()).isEqualTo(task);
        assertThat(workerTaskResult.getItemStatus().getGlobalStatus()).isEqualTo(StatusCode.OK);
        assertThat(workerTaskResult.isProcessed()).isTrue();
    }

    private DescriptionStep getDescriptionStep() {
        DefaultWorkerParameters params = WorkerParametersFactory.newWorkerParameters();
        params.setWorkerGUID(GUIDFactory.newGUID().getId());
        params.setLogbookTypeProcess(LogbookTypeProcess.INGEST);

        final Step step = new Step().setStepName("TEST").setWorkerGroupId("BigWorker");
        final List<Action> actions = new ArrayList<>();
        final Action action = new Action();
        actions.add(action);

        action.setActionDefinition(
            new ActionDefinition().setActionKey("DummyHandler").setBehavior(ProcessBehavior.NOBLOCKING)
        );
        step.setBehavior(ProcessBehavior.NOBLOCKING).setActions(actions);
        return new DescriptionStep(step, params);
    }

    @Test
    public void givenCorrectQueueAndCorrectAsyncWhenSubmitJobThenOK() throws Exception {
        DefaultWorkerParameters params = WorkerParametersFactory.newWorkerParameters();
        params.setWorkerGUID(GUIDFactory.newGUID().getId());
        params.setLogbookTypeProcess(LogbookTypeProcess.INGEST);
        final Step step = new Step().setStepName("TEST");
        final List<Action> actions = new ArrayList<>();
        final Action action = new Action();
        final String familyId = "DefaultWorker1";
        final String workerId = "NewWorkerId";

        action.setActionDefinition(
            new ActionDefinition().setActionKey("DummyHandler").setBehavior(ProcessBehavior.NOBLOCKING)
        );
        actions.add(action);
        step.setBehavior(ProcessBehavior.NOBLOCKING).setActions(actions);
        DescriptionStep descriptionStep = new DescriptionStep(step, params);
        final WorkerTask task = new WorkerTask(
            descriptionStep,
            0,
            "requestId",
            "contractId",
            "contextId",
            "applicationId",
            workerClientFactory
        );
        doReturn(new ItemStatus().increment(StatusCode.OK)).when(workerClient).submitStep(descriptionStep);
        workerManager.registerWorker(familyId, workerId, WORKER_DESCRIPTION);

        WorkerFamilyManager workerFamilyManager = workerManager.findWorkerBy(familyId);
        assertNotNull(workerFamilyManager);
        WorkerTaskResult workerTaskResult = CompletableFuture.supplyAsync(
            task,
            workerFamilyManager.getExecutor(false)
        ).get();

        assertThat(workerTaskResult).isNotNull();
        assertThat(workerTaskResult.getWorkerTask()).isEqualTo(task);
        assertThat(workerTaskResult.getItemStatus().getGlobalStatus()).isEqualTo(StatusCode.OK);
        assertThat(workerTaskResult.isProcessed()).isTrue();
    }

    @Test
    public void register_worker_ok() throws Exception {
        final String familyId = "DefaultWorker1";
        final String workerId = "NewWorkerId" + GUIDFactory.newGUID().getId();
        workerManager.registerWorker(familyId, workerId, WORKER_DESCRIPTION);
        assertNotNull(workerManager.findWorkerBy(familyId));
    }

    @Test
    public void on_initialize_register_existing_worker_ok() throws Exception {
        final String familyId = "DefaultWorker1";
        final String workerId = "NewWorkerId" + GUIDFactory.newGUID().getId();
        workerManager.registerWorker(familyId, workerId, WORKER_DESCRIPTION);
        assertNotNull(workerManager.findWorkerBy(familyId));
        workerManager.initialize();
    }

    @Test
    public void register_existing_worker_ok() throws Exception {
        final String familyId = "DefaultWorker1";
        final String workerId = "NewWorkerId1" + GUIDFactory.newGUID().getId();

        workerManager.registerWorker(familyId, workerId, WORKER_DESCRIPTION);
        workerManager.registerWorker(familyId, workerId, WORKER_DESCRIPTION);
    }

    @Test
    public void unregister_existing_worker_ok() throws Exception {
        final String familyId = "DefaultWorker2";
        final String workerId = "NewWorkerId2" + GUIDFactory.newGUID().getId();
        workerManager.registerWorker(familyId, workerId, WORKER_DESCRIPTION_2);
        WorkerFamilyManager workerfamily = workerManager.findWorkerBy(familyId);
        assertNotNull(workerfamily);
        assertEquals(workerfamily.getWorkers().size(), 1);

        workerManager.unregisterWorker(familyId, workerId);
        workerfamily = workerManager.findWorkerBy(familyId);
        assertNotNull(workerfamily);
        assertEquals(workerfamily.getWorkers().size(), 0);
    }

    @Test(expected = WorkerFamilyNotFoundException.class)
    public void unregister_non_existing_family_worker_ko() throws Exception {
        final String familyId = "NewFamilyId" + GUIDFactory.newGUID().getId();
        final String workerId = "NewWorkerId1";
        workerManager.unregisterWorker(familyId, workerId);
    }

    @Test
    public void unregister_non_existing_worker_ok() throws Exception {
        final String familyId = "DefaultWorker1";
        final String workerId = "NewWorkerId3" + GUIDFactory.newGUID().getId();
        final String workerUnknownId = "UnknownWorkerId";
        workerManager.registerWorker(familyId, workerId, WORKER_DESCRIPTION);
        workerManager.unregisterWorker(familyId, workerUnknownId);
    }

    @Test(expected = ProcessingBadRequestException.class)
    public void register_unmatched_family_then_throw_exception() throws Exception {
        final String familyId = "NewFamilyId";
        final String workerId = "NewWorkerId4" + GUIDFactory.newGUID().getId();
        workerManager.registerWorker(
            familyId,
            workerId,
            JsonHandler.getFromString(
                "{\"name\":\"worker_name\",\"status\":\"ok\", \"family\" : \"fakeValue\", \"configuration\" : {\"serverHost\" : \"localhost\", \"serverPort\" : \"12345\" }}",
                WorkerBean.class
            )
        );
    }
}
