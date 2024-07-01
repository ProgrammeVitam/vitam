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
package fr.gouv.vitam.worker.core.plugin.preservation;

import fr.gouv.vitam.batch.report.model.entry.PreservationReportEntry;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.ActionTypePreservation;
import fr.gouv.vitam.common.model.administration.preservation.ActionPreservation;
import fr.gouv.vitam.common.stream.VitamAsyncInputStreamResponse;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.exception.ProcessingStatusException;
import fr.gouv.vitam.worker.core.plugin.preservation.model.PreservationDistributionLine;
import fr.gouv.vitam.worker.core.plugin.preservation.model.WorkflowBatchResults;
import fr.gouv.vitam.worker.core.plugin.preservation.service.PreservationReportService;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static fr.gouv.vitam.common.accesslog.AccessLogUtils.getNoLogAccessLog;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.storage.engine.common.model.DataCategory.OBJECT;
import static fr.gouv.vitam.worker.core.plugin.preservation.TestWorkerParameter.TestWorkerParameterBuilder.workerParameterBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

public class PreservationActionPluginTest {

    private final String objectId = "TEST_ID";
    private final String griffinId = "griffinId-my-test";
    private final String griffinInfinteLoopId = "griffinInfinteLoopId-my-test";

    private final TestWorkerParameter parameter = workerParameterBuilder()
        .withContainerName("CONTAINER_NAME_TEST")
        .withRequestId("REQUEST_ID_TEST")
        .build();

    private PreservationActionPlugin plugin;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Mock
    private WorkspaceClientFactory workspaceClientFactory;

    @Mock
    private WorkspaceClient workspaceClient;

    @Mock
    private StorageClientFactory storageClientFactory;

    @Mock
    private StorageClient storageClient;

    @Mock
    private PreservationReportService reportService;

    @Rule
    public TemporaryFolder tmpGriffinFolder = new TemporaryFolder();

    @Captor
    private ArgumentCaptor<List<PreservationReportEntry>> captor;

    private HandlerIO handler = new TestHandlerIO();

    private static final int WORKFLOWBATCHRESULTS_IN_MEMORY = 0;

    @Before
    public void setup() throws Exception {
        given(workspaceClientFactory.getClient()).willReturn(workspaceClient);
        given(storageClientFactory.getClient()).willReturn(storageClient);

        PreservationDistributionLine preservationDistributionLine = new PreservationDistributionLine(
            "fmt/43",
            "photo.jpg",
            Collections.singletonList(new ActionPreservation(ActionTypePreservation.ANALYSE)),
            "unitId",
            griffinId,
            objectId,
            true,
            45,
            "gotId",
            "BinaryMaster",
            "BinaryMaster",
            "other_binary_strategy",
            "ScenarioId",
            "griffinIdentifier",
            new HashSet<>(Arrays.asList("unitId", "otherUnitIdBatman"))
        );
        parameter.setObjectNameList(Collections.singletonList("gotId"));
        parameter.setObjectMetadataList(
            Collections.singletonList(JsonHandler.toJsonNode(preservationDistributionLine))
        );

        File inputFolder = tmpGriffinFolder.newFolder("input-folder");
        File execFolder = tmpGriffinFolder.newFolder("exec-folder");
        plugin = new PreservationActionPlugin(
            storageClientFactory,
            reportService,
            inputFolder.toPath().toString(),
            execFolder.toPath().toString()
        );

        Path target = Files.createDirectory(execFolder.toPath().resolve(griffinId));
        String src = getClass().getResource("/preservation/griffin").toURI().getPath();
        Files.copy(Paths.get(src), target.resolve("griffin"));
        target.resolve("griffin").toFile().setExecutable(true);

        target = Files.createDirectory(execFolder.toPath().resolve(griffinInfinteLoopId));
        src = getClass().getResource("/preservation/griffin_infite_loop").toURI().getPath();
        Files.copy(Paths.get(src), target.resolve("griffin"));
        target.resolve("griffin").toFile().setExecutable(true);

        VitamThreadUtils.getVitamSession().setTenantId(0);
    }

    @Test
    @RunWithCustomExecutor
    public void should_copy_input_files() throws Exception {
        // Given
        given(
            storageClient.getContainerAsync("other_binary_strategy", objectId, OBJECT, getNoLogAccessLog())
        ).willReturn(createOkResponse("image-files-with-data"));

        // When
        plugin.executeList(parameter, handler);

        // Then
        verify(storageClient).getContainerAsync("other_binary_strategy", objectId, OBJECT, getNoLogAccessLog());
    }

    @Test
    @RunWithCustomExecutor
    public void should_create_report() throws Exception {
        // Given
        given(
            storageClient.getContainerAsync("other_binary_strategy", objectId, OBJECT, getNoLogAccessLog())
        ).willReturn(createOkResponse("image-files-with-data"));

        plugin.executeList(parameter, handler);

        // When
        verify(reportService).appendEntries(eq("REQUEST_ID_TEST"), captor.capture());

        // Then
        assertThat(captor.getValue()).extracting(PreservationReportEntry::getAnalyseResult).contains("NOT_VALID");
    }

    @Test
    @RunWithCustomExecutor
    public void should_delete_batch_files_in_case_of_error() throws Exception {
        // Given
        given(
            storageClient.getContainerAsync(
                VitamConfiguration.getDefaultStrategy(),
                objectId,
                OBJECT,
                getNoLogAccessLog()
            )
        ).willReturn(createOkResponse("image-files-with-data"));
        doThrow(new ProcessingStatusException(StatusCode.FATAL, "error"))
            .when(reportService)
            .appendEntries(any(), any());

        // When
        ThrowingCallable throwingCallable = () -> plugin.executeList(parameter, handler);

        // Then
        assertThatThrownBy(throwingCallable).isInstanceOf(ProcessingException.class);

        String[] filesInGriffinDir = Paths.get(tmpGriffinFolder.getRoot().getPath(), "input-folder", griffinId)
            .toFile()
            .list();

        assertThat(filesInGriffinDir).isEmpty();
    }

    @Test
    @RunWithCustomExecutor
    public void should_exec_workflow_and_return_build_status_OK() throws Exception {
        // Given
        given(
            storageClient.getContainerAsync("other_binary_strategy", objectId, OBJECT, getNoLogAccessLog())
        ).willReturn(createOkResponse("image-files-with-data"));

        // When
        List<ItemStatus> status = plugin.executeList(parameter, handler);

        // Then
        assertThat(status).extracting(ItemStatus::getGlobalStatus).containsOnly(OK);
    }

    @Test
    @RunWithCustomExecutor
    public void should_stop_process_when_any_exception() throws Exception {
        // Given
        given(
            storageClient.getContainerAsync(
                VitamConfiguration.getDefaultStrategy(),
                objectId,
                OBJECT,
                getNoLogAccessLog()
            )
        ).willThrow(new IllegalStateException("test"));

        // When
        ThrowingCallable throwingCallable = () -> plugin.executeList(parameter, handler);

        // Then
        assertThatThrownBy(throwingCallable).isInstanceOf(ProcessingException.class);
    }

    @Test
    @RunWithCustomExecutor
    public void should_create_preservation_report_with_binary_guid() throws Exception {
        // Given
        given(
            storageClient.getContainerAsync("other_binary_strategy", objectId, OBJECT, getNoLogAccessLog())
        ).willReturn(createOkResponse("image-files-with-data"));
        doNothing().when(reportService).appendEntries(ArgumentMatchers.anyString(), captor.capture());
        // When
        List<ItemStatus> status = plugin.executeList(parameter, handler);
        // Then
        assertThat(status).extracting(ItemStatus::getGlobalStatus).containsOnly(OK);
        String binaryId = captor.getValue().get(0).getOutputObjectId();
        assertThat(binaryId).isNotNull();
        WorkflowBatchResults results = (WorkflowBatchResults) handler.getInput(WORKFLOWBATCHRESULTS_IN_MEMORY);
        assertThat(binaryId).isEqualTo(
            results.getWorkflowBatchResults().get(0).getOutputExtras().get(0).getBinaryGUID()
        );
    }

    @Test
    @RunWithCustomExecutor
    public void should_kill_griffin_process_when_timeout_expired() throws Exception {
        // Given
        PreservationDistributionLine preservationDistributionLineShortTimeout = new PreservationDistributionLine(
            "fmt/43",
            "photo.jpg",
            Collections.singletonList(new ActionPreservation(ActionTypePreservation.ANALYSE)),
            "unitId",
            griffinInfinteLoopId,
            objectId,
            true,
            2,
            "gotId",
            "BinaryMaster",
            "BinaryMaster",
            "other_binary_strategy",
            "ScenarioId",
            "griffinIdentifier",
            Collections.singleton("unitId")
        );
        parameter.setObjectMetadataList(
            Collections.singletonList(JsonHandler.toJsonNode(preservationDistributionLineShortTimeout))
        );
        given(
            storageClient.getContainerAsync("other_binary_strategy", objectId, OBJECT, getNoLogAccessLog())
        ).willReturn(createOkResponse("image-files-with-data"));

        // When
        ThrowingCallable throwingCallable = () -> plugin.executeList(parameter, handler);

        // Then
        assertThatThrownBy(throwingCallable)
            .isInstanceOf(ProcessingException.class)
            .hasMessageContaining("process hasn't exited");
    }

    @Test
    @RunWithCustomExecutor
    public void should_add_related_units_in_workflow_batch_result() throws Exception {
        // Given
        given(
            storageClient.getContainerAsync("other_binary_strategy", objectId, OBJECT, getNoLogAccessLog())
        ).willReturn(createOkResponse("image-files-with-data"));

        // When
        plugin.executeList(parameter, handler);

        // Then
        WorkflowBatchResults results = (WorkflowBatchResults) handler.getInput(WORKFLOWBATCHRESULTS_IN_MEMORY);
        assertThat(results.getWorkflowBatchResults().get(0).getUnitsForExtractionAU()).containsOnly(
            "unitId",
            "otherUnitIdBatman"
        );
    }

    private Response createOkResponse(String entity) {
        return new VitamAsyncInputStreamResponse(
            new ByteArrayInputStream(entity.getBytes()),
            Response.Status.OK,
            Collections.emptyMap()
        );
    }
}
