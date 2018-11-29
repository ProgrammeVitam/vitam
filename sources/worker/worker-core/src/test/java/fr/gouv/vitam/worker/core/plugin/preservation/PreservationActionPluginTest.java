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

import static fr.gouv.vitam.batch.report.model.AnalyseResultPreservation.NOT_VALID;
import static fr.gouv.vitam.common.accesslog.AccessLogUtils.getNoLogAccessLog;
import static fr.gouv.vitam.common.model.StatusCode.KO;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.storage.engine.common.model.DataCategory.OBJECT;
import static fr.gouv.vitam.worker.core.plugin.preservation.PreservationActionPlugin.DEFAULT_STORAGE_STRATEGY;
import static fr.gouv.vitam.worker.core.plugin.preservation.TestWorkerParameter.TestWorkerParameterBuilder.workerParameterBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.vitam.batch.report.model.PreservationReportModel;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.stream.VitamAsyncInputStreamResponse;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.worker.core.plugin.preservation.service.PreservationReportService;
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

public class PreservationActionPluginTest {

    private final String test_id = "TEST_ID";
    private final String griffinId = "griffinId-my-test";
    private final TestWorkerParameter parameter =
        workerParameterBuilder()
            .withContainerName("CONTAINER_NAME_TEST")
            .withRequestId("REQUEST_ID_TEST")
            .build();


    private PreservationActionPlugin plugin;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

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
        parameter.setObjectNameList(Collections.singletonList(test_id));


        ParamsPreservationDistributionFile paramsPreservationDistributionFile =
            new ParamsPreservationDistributionFile("fmt/43", "photo.jpg",
                Collections.singletonList(new Action("ANALYSE", null)), "unitId",
                griffinId,
                "objectId", true, 45);
        parameter.setObjectMetadataList(
            Collections.singletonList(JsonHandler.toJsonNode(paramsPreservationDistributionFile)));

        File inputFolder = tmpGriffinFolder.newFolder("input-folder");
        File execFolder = tmpGriffinFolder.newFolder("exec-folder");
        plugin = new PreservationActionPlugin(storageClientFactory, reportService, inputFolder.toPath().toString(),
            execFolder.toPath().toString());

        Path target = Files.createDirectory(execFolder.toPath().resolve(griffinId));
        String src = Object.class.getResource("/preservation/griffin").toURI().getPath();
        Files.copy(Paths.get(src), target.resolve("griffin"));
        target.resolve("griffin").toFile().setExecutable(true);
        VitamThreadUtils.getVitamSession().setTenantId(0);
    }

    @Test
    @RunWithCustomExecutor
    public void should_copy_input_files() throws Exception {
        // Given
        given(storageClient.getContainerAsync(DEFAULT_STORAGE_STRATEGY, test_id, OBJECT, getNoLogAccessLog()))
            .willReturn(createOkResponse("image-files-with-data"));

        // When
        plugin.executeList(parameter, null);

        // Then
        verify(storageClient).getContainerAsync(DEFAULT_STORAGE_STRATEGY, test_id, OBJECT, getNoLogAccessLog());
    }

    @Test
    @RunWithCustomExecutor
    public void should_create_report() throws Exception {
        // Given
        given(storageClient.getContainerAsync(DEFAULT_STORAGE_STRATEGY, test_id, OBJECT, getNoLogAccessLog()))
            .willReturn(createOkResponse("image-files-with-data"));

        plugin.executeList(parameter, null);

        // When
        verify(reportService).appendPreservationEntries(eq("REQUEST_ID"), captor.capture());

        // Then
        assertThat(captor.getValue()).extracting(PreservationReportModel::getAnalyseResult).contains(NOT_VALID.name());
    }

    @Test
    @RunWithCustomExecutor
    public void should_delete_batch_files_at_the_end() throws Exception {
        // Given
        given(storageClient.getContainerAsync(DEFAULT_STORAGE_STRATEGY, test_id, OBJECT, getNoLogAccessLog()))
            .willReturn(createOkResponse("image-files-with-data"));

        plugin.executeList(parameter, null);

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
        given(storageClient.getContainerAsync(DEFAULT_STORAGE_STRATEGY, test_id, OBJECT, getNoLogAccessLog()))
            .willReturn(createOkResponse("image-files-with-data"));

        // When
        List<ItemStatus> status = plugin.executeList(parameter, null);

        // Then
        for (ItemStatus itemStatus : status) {
            assertThat(itemStatus.getGlobalStatus()).isEqualTo(OK);
        }
    }

    @Test
    @RunWithCustomExecutor
    public void should_return_build_status_KO_when_any_error_occurs() throws Exception {
        // Given
        given(storageClient.getContainerAsync(DEFAULT_STORAGE_STRATEGY, test_id, OBJECT, getNoLogAccessLog()))
            .willThrow(new IllegalStateException("test"));

        // When
        List<ItemStatus> status = plugin.executeList(parameter, null);

        // Then
        for (ItemStatus itemStatus : status) {
            assertThat(itemStatus.getGlobalStatus()).isEqualTo(KO);
        }
    }

    private Response createOkResponse(String entity) {
        return new VitamAsyncInputStreamResponse(new ByteArrayInputStream(entity.getBytes()), Response.Status.OK,
            Collections.emptyMap());
    }

    private class ParamsPreservationDistributionFile {
        @JsonProperty("formatId")
        private String formatId;
        @JsonProperty("filename")
        private String filename;
        @JsonProperty("actions")
        private List<Action> actions;
        @JsonProperty("unitId")
        private String unitId;
        @JsonProperty("griffinId")
        private String griffinId;
        @JsonProperty("objectId")
        private String objectId;
        @JsonProperty("debug")
        private boolean debug;
        @JsonProperty("timeout")
        private int timeout;

        public ParamsPreservationDistributionFile(String formatId, String filename,
            List<Action> actions, String unitId, String griffinId, String objectId, boolean debug, int timeout) {
            this.formatId = formatId;
            this.filename = filename;
            this.actions = actions;
            this.unitId = unitId;
            this.griffinId = griffinId;
            this.objectId = objectId;
            this.debug = debug;
            this.timeout = timeout;
        }
    }


    private class Action {
        @JsonProperty("type")
        private String type;
        @JsonProperty("values")
        private String values;

        public Action(String type, String values) {
            this.type = type;
            this.values = values;
        }
    }
}
