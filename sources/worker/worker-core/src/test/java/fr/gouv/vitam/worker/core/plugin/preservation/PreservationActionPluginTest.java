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
package fr.gouv.vitam.worker.core.plugin.preservation;

import fr.gouv.vitam.batch.report.model.PreservationReportModel;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.stream.VitamAsyncInputStreamResponse;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.worker.core.plugin.preservation.service.PreservationReportService;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
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
import java.util.Collections;
import java.util.List;

import static fr.gouv.vitam.batch.report.model.AnalyseResultPreservation.NOT_VALID;
import static fr.gouv.vitam.common.accesslog.AccessLogUtils.getNoLogAccessLog;
import static fr.gouv.vitam.common.model.StatusCode.FATAL;
import static fr.gouv.vitam.common.model.StatusCode.KO;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.storage.engine.common.model.DataCategory.OBJECT;
import static fr.gouv.vitam.worker.core.plugin.preservation.PreservationActionPlugin.DEFAULT_STORAGE_STRATEGY;
import static fr.gouv.vitam.worker.core.plugin.preservation.PreservationActionPlugin.DISTRIBUTION_FILE;
import static fr.gouv.vitam.worker.core.plugin.preservation.TestWorkerParameter.TestWorkerParameterBuilder.workerParameterBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

public class PreservationActionPluginTest {

    private final String test_id = "TEST_ID";
    private final String griffinId = "griffinId-my-test";
    private final TestWorkerParameter parameter = workerParameterBuilder()
        .withContainerName("CONTAINER_NAME_TEST")
        .withRequestId("REQUEST_ID_TEST")
        .build();

    private PreservationActionPlugin plugin;

    @Rule
    public  MockitoRule rule = MockitoJUnit.rule();

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

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
    private ArgumentCaptor<List<PreservationReportModel>> captor;

    @Before
    public void setup() throws Exception {
        given(workspaceClientFactory.getClient()).willReturn(workspaceClient);
        given(storageClientFactory.getClient()).willReturn(storageClient);

        File inputFolder = tmpGriffinFolder.newFolder("input-folder");
        File execFolder = tmpGriffinFolder.newFolder("exec-folder");
        plugin = new PreservationActionPlugin(workspaceClientFactory, storageClientFactory, reportService, inputFolder.toPath().toString(), execFolder.toPath().toString());

        Path target = Files.createDirectory(execFolder.toPath().resolve(griffinId));
        String src = Object.class.getResource("/preservation/griffin").toURI().getPath();
        Files.copy(Paths.get(src), target.resolve("griffin"));
        target.resolve("griffin").toFile().setExecutable(true);
        VitamThreadUtils.getVitamSession().setTenantId(0);
    }

    @Test
    @RunWithCustomExecutor
    public void should_read_distribution_file() throws Exception {
        // Given
        given(workspaceClient.getObject(parameter.getContainerName(), DISTRIBUTION_FILE)).willReturn(createOkResponse(getDistribLine()));
        given(storageClient.getContainerAsync(DEFAULT_STORAGE_STRATEGY, test_id, OBJECT, getNoLogAccessLog()))
            .willReturn(createOkResponse("image-files-with-data"));

        // When
        plugin.execute(parameter, null);

        // Then
        verify(workspaceClient).getObject(parameter.getContainerName(), DISTRIBUTION_FILE);
    }

    @Test
    @RunWithCustomExecutor
    public void should_copy_input_files() throws Exception {
        // Given
        given(workspaceClient.getObject(parameter.getContainerName(), DISTRIBUTION_FILE)).willReturn(createOkResponse(getDistribLine()));
        given(storageClient.getContainerAsync(DEFAULT_STORAGE_STRATEGY, test_id, OBJECT, getNoLogAccessLog()))
            .willReturn(createOkResponse("image-files-with-data"));

        // When
        plugin.execute(parameter, null);

        // Then
        verify(storageClient).getContainerAsync(DEFAULT_STORAGE_STRATEGY, test_id, OBJECT, getNoLogAccessLog());
    }

    @Test
    @RunWithCustomExecutor
    public void should_create_report() throws Exception {
        // Given
        given(workspaceClient.getObject(parameter.getContainerName(), DISTRIBUTION_FILE)).willReturn(createOkResponse(getDistribLine()));
        given(storageClient.getContainerAsync(DEFAULT_STORAGE_STRATEGY, test_id, OBJECT, getNoLogAccessLog()))
            .willReturn(createOkResponse("image-files-with-data"));

        plugin.execute(parameter, null);

        // When
        verify(reportService).appendPreservationEntries(eq("REQUEST_ID"), captor.capture());

        // Then
        assertThat(captor.getValue()).extracting(PreservationReportModel::getAnalyseResult).contains(NOT_VALID.name());
    }

    @Test
    @RunWithCustomExecutor
    public void should_delete_batch_files_at_the_end() throws Exception {
        // Given
        given(workspaceClient.getObject(parameter.getContainerName(), DISTRIBUTION_FILE)).willReturn(createOkResponse(getDistribLine()));
        given(storageClient.getContainerAsync(DEFAULT_STORAGE_STRATEGY, test_id, OBJECT, getNoLogAccessLog()))
            .willReturn(createOkResponse("image-files-with-data"));

        plugin.execute(parameter, null);

        // When
        String[] filesInGriffinDir = Paths.get(tmpGriffinFolder.getRoot().getPath())
            .resolve("input-folder")
            .resolve(griffinId)
            .toFile()
            .list();

        // Then
        assertThat(filesInGriffinDir).isEmpty();
    }

    @Test
    @RunWithCustomExecutor
    public void should_exec_workflow_and_return_buid_status_OK() throws Exception {
        // Given
        given(workspaceClient.getObject(parameter.getContainerName(), DISTRIBUTION_FILE)).willReturn(createOkResponse(getDistribLine()));
        given(storageClient.getContainerAsync(DEFAULT_STORAGE_STRATEGY, test_id, OBJECT, getNoLogAccessLog()))
            .willReturn(createOkResponse("image-files-with-data"));

        // When
        ItemStatus status = plugin.execute(parameter, null);

        // Then
        assertThat(status.getGlobalStatus()).isEqualTo(OK);
    }

    @Test
    @RunWithCustomExecutor
    public void should_return_build_status_FATAL_when_read_distribution_file_error_error() throws Exception {
        // Given
        given(workspaceClient.getObject(parameter.getContainerName(), DISTRIBUTION_FILE)).willThrow(new ContentAddressableStorageServerException("test"));
        given(storageClient.getContainerAsync(DEFAULT_STORAGE_STRATEGY, test_id, OBJECT, getNoLogAccessLog()))
            .willReturn(createOkResponse("image-files-with-data"));

        // When
        ItemStatus status = plugin.execute(parameter, null);

        // Then
        assertThat(status.getGlobalStatus()).isEqualTo(FATAL);
    }

    @Test
    @RunWithCustomExecutor
    public void should_return_build_status_KO_when_any_error_occurs() throws Exception {
        // Given
        given(workspaceClient.getObject(parameter.getContainerName(), DISTRIBUTION_FILE)).willReturn(createOkResponse(getDistribLine()));
        given(storageClient.getContainerAsync(DEFAULT_STORAGE_STRATEGY, test_id, OBJECT, getNoLogAccessLog())).willThrow(new IllegalStateException("test"));

        // When
        ItemStatus status = plugin.execute(parameter, null);

        // Then
        assertThat(status.getGlobalStatus()).isEqualTo(KO);
    }

    @Test
    @RunWithCustomExecutor
    public void should_check_mandatory_parameters_do_nothing() throws Exception {
        // Given / When
        plugin.checkMandatoryIOParameter(null);

        // Then
        verifyZeroInteractions(workspaceClientFactory, workspaceClient, storageClientFactory, storageClient, reportService);
    }

    private Response createOkResponse(String entity) {
        return new VitamAsyncInputStreamResponse(new ByteArrayInputStream(entity.getBytes()), Response.Status.OK, Collections.emptyMap());
    }

    private String getDistribLine() {
        return String.format(
            "{\"id\": \"%s\", \"distribGroup\":1, \"params\": {\"formatId\": \"fmt/43\", \"griffinId\": \"%s\", \"actions\":[{\"type\":\"ANALYSE\", \"values\":null}], \"unitId\":\"Bobi\", \"objectId\": \"bobiObject\", \"debug\":true, \"timeout\":45 } }",
                test_id,
            griffinId
        );
    }
}
