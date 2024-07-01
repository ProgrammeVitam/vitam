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
package fr.gouv.vitam.processing.integration.test;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.google.common.collect.Sets;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.ServerIdentity;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InternalServerException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.parameters.Contexts;
import fr.gouv.vitam.processing.common.model.ProcessStep;
import fr.gouv.vitam.processing.common.model.ProcessWorkflow;
import fr.gouv.vitam.processing.common.model.WorkerBean;
import fr.gouv.vitam.processing.common.model.WorkerRemoteConfiguration;
import fr.gouv.vitam.processing.engine.core.monitoring.ProcessMonitoringImpl;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.processing.management.rest.ProcessManagementMain;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import org.assertj.core.api.Assertions;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static fr.gouv.vitam.common.VitamTestHelper.waitOperation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

public class MultiFamilyWorkerProcessingIT extends VitamRuleRunner {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(MultiFamilyWorkerProcessingIT.class);

    @ClassRule
    public static VitamServerRunner runner = new VitamServerRunner(
        MultiFamilyWorkerProcessingIT.class,
        mongoRule.getMongoDatabase().getName(),
        ElasticsearchRule.getClusterName(),
        Sets.newHashSet(WorkspaceMain.class, ProcessManagementMain.class)
    );

    private static final Integer TENANT_ID = 0;
    private static final long SLEEP_TIME = 20l;
    private static final long NB_TRY = 18000;
    private final int[] elementCountPerStep = { 1, 0, 170, 1, 0, 170, 0, 170, 0, 1, 1 };

    public static final String INGEST_LEVEL_STACK_JSON = "integration-processing/ingestLevelStack.json";
    public static final String UNITS_LEVEL_STACK_PATH = "UnitsLevel/ingestLevelStack.json";

    public static final String EXISING_GOT_FILE = "integration-processing/existing_object_group.json";

    public static final String EXISTING_GOT = "UpdateObjectGroup/existing_object_group.json";

    @ClassRule
    public static WireMockClassRule workerMockRule_Family_One = new WireMockClassRule(options().dynamicPort());

    @Rule
    public WireMockClassRule workerInstanceFamilyOne = workerMockRule_Family_One;

    @ClassRule
    public static WireMockClassRule workerMockRule_Family_Two = new WireMockClassRule(options().dynamicPort());

    @Rule
    public WireMockClassRule workerInstanceFamilyTwo = workerMockRule_Family_Two;

    private WorkspaceClient workspaceClient;

    private static Path overrideDefaultIngestWorkflow = null;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        handleBeforeClass(Arrays.asList(0, 1), Collections.emptyMap());
        // set bulk size to 1 for tests
        VitamConfiguration.setWorkerBulkSize(1);
        VitamConfiguration.setDistributeurBatchSize(10);

        // Stop processing to override default ingest workflow
        runner.stopProcessManagementServer(false);

        // Override default ingest workflow
        // For more information on how this works, please refer to the hello-world-plugin docs
        //======================================
        InputStream overrideWorkflow = PropertiesUtils.getResourceAsStream(
            "integration-processing/family_worker/MultiFamilyIngestWorkflow.json"
        );

        File processingWorkflowConfig = PropertiesUtils.fileFromConfigFolder("workflows");

        if (!processingWorkflowConfig.exists()) {
            Files.createDirectories(processingWorkflowConfig.toPath());
        }

        // Copy the override workflow to the processing config folder
        overrideDefaultIngestWorkflow = processingWorkflowConfig.toPath().resolve("MultiFamilyIngestWorkflow.json");
        Files.copy(overrideWorkflow, overrideDefaultIngestWorkflow);

        // Start processing to take in account the new workflow
        runner.startProcessManagementServer();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        handleAfterClass();
        runAfter();

        // Delete override default ingest workflow. This important to ensure that other IT test do not use this configuration
        if (null != overrideDefaultIngestWorkflow) {
            Files.delete(overrideDefaultIngestWorkflow);
        }
        VitamConfiguration.setWorkerBulkSize(10);
        VitamConfiguration.setDistributeurBatchSize(100);

        ProcessingManagementClientFactory.getInstance()
            .getClient()
            .unregisterWorker("FamilyOne", String.valueOf(ServerIdentity.getInstance().getGlobalPlatformId()));

        ProcessingManagementClientFactory.getInstance()
            .getClient()
            .unregisterWorker("FamilyTwo", String.valueOf(ServerIdentity.getInstance().getGlobalPlatformId()));

        // Restart processing in order to remove override default ingest workflow
        runner.stopProcessManagementServer(false);

        try (WorkspaceClient workspaceClient = WorkspaceClientFactory.getInstance().getClient()) {
            workspaceClient.deleteContainer("process", true);
        } catch (Exception e) {
            LOGGER.error(e);
        }

        workerMockRule_Family_One.shutdownServer();
        workerMockRule_Family_Two.shutdownServer();

        runner.startProcessManagementServer();
        VitamClientFactory.resetConnections();
    }

    @Before
    public void setUp() throws Exception {
        // Ensure processing is started
        runner.startProcessManagementServer();
        WorkerBean workerBeanOne = new WorkerBean(
            "FamilyOneWorker",
            "FamilyOne",
            1,
            "status",
            new WorkerRemoteConfiguration("localhost", workerMockRule_Family_One.port())
        );
        workerBeanOne.setWorkerId(ServerIdentity.getInstance().getGlobalPlatformId() + "_family_one");

        WorkerBean workerBeanTwo = new WorkerBean(
            "FamilyTwoWorker",
            "FamilyTwo",
            1,
            "status",
            new WorkerRemoteConfiguration("localhost", workerMockRule_Family_Two.port())
        );
        workerBeanTwo.setWorkerId(ServerIdentity.getInstance().getGlobalPlatformId() + "_family_two");

        ProcessingManagementClientFactory processingManagementClientFactory =
            ProcessingManagementClientFactory.getInstance();
        ProcessingManagementClient processingManagementClient = processingManagementClientFactory.getClient();

        processingManagementClient.registerWorker(
            workerBeanOne.getFamily(),
            workerBeanOne.getWorkerId(),
            workerBeanOne
        );

        processingManagementClient.registerWorker(
            workerBeanTwo.getFamily(),
            workerBeanTwo.getWorkerId(),
            workerBeanTwo
        );

        workerInstanceFamilyOne.stubFor(
            WireMock.get(urlMatching("/worker/v1/status")).willReturn(
                aResponse().withStatus(200).withHeader(GlobalDataRest.X_TENANT_ID, Integer.toString(TENANT_ID))
            )
        );

        workerInstanceFamilyOne.stubFor(
            WireMock.post(urlMatching("/worker/v1/tasks")).willReturn(
                aResponse()
                    .withStatus(200)
                    .withBody(JsonHandler.unprettyPrint(getMockedItemStatus(StatusCode.OK, "FamilyOne")))
                    .withHeader(GlobalDataRest.X_TENANT_ID, Integer.toString(TENANT_ID))
                    .withHeader("Content-Type", "application/json")
            )
        );

        workerInstanceFamilyTwo.stubFor(
            WireMock.get(urlMatching("/worker/v1/status")).willReturn(
                aResponse().withStatus(200).withHeader(GlobalDataRest.X_TENANT_ID, Integer.toString(TENANT_ID))
            )
        );

        workerInstanceFamilyTwo.stubFor(
            WireMock.post(urlMatching("/worker/v1/tasks")).willReturn(
                aResponse()
                    .withStatus(200)
                    .withBody(JsonHandler.unprettyPrint(getMockedItemStatus(StatusCode.OK, "FamilyTwo")))
                    .withHeader(GlobalDataRest.X_TENANT_ID, Integer.toString(TENANT_ID))
                    .withHeader("X_FAMILY_HEADER", "FamilyTwo")
                    .withHeader("Content-Type", "application/json")
            )
        );
    }

    ItemStatus getMockedItemStatus(StatusCode statusCode, String family) {
        return new ItemStatus(family).setItemsStatus(
            family,
            new ItemStatus(family).setMessage(family).increment(statusCode, VitamConfiguration.getWorkerBulkSize())
        );
    }

    @Test
    @RunWithCustomExecutor
    public void test_multi_family_workers_by_overriding_default_ingest() throws Exception {
        ProcessingIT.prepareVitamSession();

        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(TENANT_ID);
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        final String operationId = operationGuid.toString();
        simulateIngest(operationId);

        ProcessWorkflow processWorkflow = ProcessMonitoringImpl.getInstance()
            .findOneProcessWorkflow(operationId, TENANT_ID);

        RequestResponse<ItemStatus> resp = ProcessingManagementClientFactory.getInstance()
            .getClient()
            .executeOperationProcess(operationId, Contexts.DEFAULT_WORKFLOW.name(), ProcessAction.NEXT.getValue());

        assertThat(resp).isNotNull();
        assertThat(resp.isOk()).isTrue();
        assertThat(resp.getStatus()).isEqualTo(Response.Status.ACCEPTED.getStatusCode());

        waitOperation(operationId, ProcessState.PAUSE);

        // Get first step and assert that it is executed by FamilyOne worker
        ProcessStep firstStep = processWorkflow.getSteps().get(0);
        assertThat(firstStep.getWorkerGroupId()).isEqualTo("FamilyOne");
        assertThat(firstStep.getStepResponses().getItemsStatus()).containsKey("FamilyOne");

        // Worker of the family two not invoked
        assertThat(workerInstanceFamilyTwo.getServeEvents().getMeta().total).isEqualTo(0);
        // Worker of the family one invoked once
        assertThat(workerInstanceFamilyOne.getServeEvents().getMeta().total).isEqualTo(1);

        workerInstanceFamilyOne.resetRequests();

        resp = ProcessingManagementClientFactory.getInstance()
            .getClient()
            .executeOperationProcess(operationId, Contexts.DEFAULT_WORKFLOW.name(), ProcessAction.NEXT.getValue());

        assertThat(resp).isNotNull();
        assertThat(resp.isOk()).isTrue();
        assertThat(resp.getStatus()).isEqualTo(Response.Status.ACCEPTED.getStatusCode());

        waitOperation(operationId, ProcessState.PAUSE);

        // Worker of the family one not invoked
        assertThat(workerInstanceFamilyOne.getServeEvents().getMeta().total).isEqualTo(0);
        // Worker of the family two not invoked. Because distribution is empty
        assertThat(workerInstanceFamilyTwo.getServeEvents().getMeta().total).isEqualTo(0);

        resp = ProcessingManagementClientFactory.getInstance()
            .getClient()
            .executeOperationProcess(operationId, Contexts.DEFAULT_WORKFLOW.name(), ProcessAction.NEXT.getValue());

        assertThat(resp).isNotNull();
        assertThat(resp.isOk()).isTrue();
        assertThat(resp.getStatus()).isEqualTo(Response.Status.ACCEPTED.getStatusCode());

        waitOperation(operationId, ProcessState.PAUSE);

        // Worker of the family one not invoked
        assertThat(workerInstanceFamilyOne.getServeEvents().getMeta().total).isEqualTo(0);
        // Worker of the family two is invoked.
        assertThat(workerInstanceFamilyTwo.getServeEvents().getMeta().total).isEqualTo(170);

        // Reset counting events
        workerInstanceFamilyOne.resetRequests();
        workerInstanceFamilyTwo.resetRequests();

        // Get third step and assert that it is executed by FamilyTwo worker
        ProcessStep secondThree = processWorkflow.getSteps().get(2);
        assertThat(secondThree.getWorkerGroupId()).isEqualTo("FamilyTwo");
        assertThat(secondThree.getStepResponses().getItemsStatus()).containsKey("FamilyTwo");

        resp = ProcessingManagementClientFactory.getInstance()
            .getClient()
            .executeOperationProcess(operationId, Contexts.DEFAULT_WORKFLOW.name(), ProcessAction.RESUME.getValue());

        assertThat(resp).isNotNull();
        assertThat(resp.isOk()).isTrue();
        assertThat(resp.getStatus()).isEqualTo(Response.Status.ACCEPTED.getStatusCode());

        waitOperation(operationId);

        // Number of invocations from the step 3 to the last one
        // All ref steps are executed by worker FamilyOne
        // All Distributed steps are executed by worker FamilyTwo
        // Following elementCountPerStep = {1, 0, 170 [Step Three], 1, 0, 170, 0, 170, 0, 1, 1};
        // Step 5 "bulkSize": 1000  (1 call) => 0
        // Step 6 "bulkSize": 1000  (1 call level_0, 1 call level_1, 1 call level_2) => 3 calls
        // Step 7 "bulkSize": 128   (2 call) => 0
        // Step 8 "bulkSize": 128   (2 call level_0, 1 call level_1, 1 call level_2) => 4 calls
        // Step 9 "bulkSize": 128   (2 call) => 0
        // In total 7 calls
        // Worker of the family one
        assertThat(workerInstanceFamilyOne.getServeEvents().getMeta().total).isEqualTo(1 + 1 + 1);
        // Worker of the family two
        assertThat(workerInstanceFamilyTwo.getServeEvents().getMeta().total).isEqualTo(7);

        workerInstanceFamilyOne.resetRequests();
        workerInstanceFamilyTwo.resetRequests();

        processWorkflow = ProcessMonitoringImpl.getInstance().findOneProcessWorkflow(operationId, TENANT_ID);
        assertThat(processWorkflow).isNotNull();
        assertThat(processWorkflow.getStatus()).isEqualTo(StatusCode.WARNING);
        assertThat(processWorkflow.getState()).isEqualTo(ProcessState.COMPLETED);

        checkAllSteps(processWorkflow);
    }

    private void simulateIngest(String containerName)
        throws ContentAddressableStorageServerException, BadRequestException, InternalServerException, IOException {
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);
        ProcessingManagementClientFactory.getInstance()
            .getClient()
            .initVitamProcess(containerName, Contexts.DEFAULT_WORKFLOW.name());

        final File resourceFile = PropertiesUtils.getResourceFile(INGEST_LEVEL_STACK_JSON);
        workspaceClient.putObject(containerName, UNITS_LEVEL_STACK_PATH, Files.newInputStream(resourceFile.toPath()));

        final File existing_got = PropertiesUtils.getResourceFile(EXISING_GOT_FILE);
        workspaceClient.putObject(containerName, EXISTING_GOT, Files.newInputStream(existing_got.toPath()));
    }

    private void checkAllSteps(ProcessWorkflow processWorkflow) {
        int stepIndex = 0;
        for (ProcessStep step : processWorkflow.getSteps()) {
            // check status
            assertTrue(
                step.getStepStatusCode().equals(StatusCode.OK) || step.getStepStatusCode().equals(StatusCode.WARNING)
            );

            // check processed elements
            Assertions.assertThat(step.getElementProcessed().get())
                .as("Compare elementProcessed with elementToProcess > step :" + step.getStepName())
                .isEqualTo(step.getElementToProcess().get());
            Assertions.assertThat(step.getElementProcessed().get())
                .as("Compare elementProcessed with expected number > step :" + step.getStepName())
                .isEqualTo(elementCountPerStep[stepIndex++]);
        }
    }
}
