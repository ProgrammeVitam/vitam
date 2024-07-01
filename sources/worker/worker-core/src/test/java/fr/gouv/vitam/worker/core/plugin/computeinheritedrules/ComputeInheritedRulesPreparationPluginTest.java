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
package fr.gouv.vitam.worker.core.plugin.computeinheritedrules;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.distribution.JsonLineGenericIterator;
import fr.gouv.vitam.worker.core.distribution.JsonLineModel;
import fr.gouv.vitam.worker.core.plugin.preservation.TestHandlerIO;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

import static fr.gouv.vitam.worker.core.plugin.computeinheritedrules.ComputeInheritedRulesPreparationPlugin.UNITS_JSONL_FILE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ComputeInheritedRulesPreparationPluginTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private AdminManagementClientFactory adminManagementClientFactory;

    @Mock
    private AdminManagementClient adminManagementClient;

    @Mock
    private MetaDataClientFactory metaDataClientFactory;

    @Mock
    private MetaDataClient metaDataClient;

    @Mock
    private WorkspaceClientFactory workspaceClientFactory;

    @Mock
    private WorkspaceClient workspaceClient;

    private ComputeInheritedRulesPreparationPlugin computeInheritedRulesPreparationPlugin;

    private static final TypeReference<JsonLineModel> jsonLineModelTypeReference = new TypeReference<>() {};

    @Before
    public void setUp() throws Exception {
        when(metaDataClientFactory.getClient()).thenReturn(metaDataClient);
        when(adminManagementClientFactory.getClient()).thenReturn(adminManagementClient);
        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);

        computeInheritedRulesPreparationPlugin = new ComputeInheritedRulesPreparationPlugin(metaDataClientFactory);
    }

    @Test
    public void should_generate_distribution_file_with_requested_ids() throws Exception {
        // Given
        InputStream query = PropertiesUtils.getResourceAsStream("computeInheritedRules/query.json");
        File distributionFile = temporaryFolder.newFile();
        WorkerParameters workerParameters = mock(WorkerParameters.class);
        JsonNode unitResponse = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream("computeInheritedRules/unit.json")
        );
        when(metaDataClient.selectUnits(any())).thenReturn(unitResponse);
        HandlerIO handlerIO = mock(HandlerIO.class);
        when(handlerIO.getJsonFromWorkspace(anyString())).thenReturn(JsonHandler.getFromInputStream(query));
        when(handlerIO.getNewLocalFile(anyString())).thenReturn(distributionFile);
        File resultFile = temporaryFolder.newFile();
        doAnswer(invocation -> {
            File distributionFileCaptured = invocation.getArgument(1);

            try (FileOutputStream fileOutputStream = new FileOutputStream(resultFile)) {
                Files.copy(distributionFileCaptured.toPath(), fileOutputStream);
            }
            return null;
        })
            .when(handlerIO)
            .transferFileToWorkspace(
                ArgumentMatchers.eq(UNITS_JSONL_FILE),
                any(),
                ArgumentMatchers.eq(true),
                ArgumentMatchers.eq(false)
            );
        // When
        ItemStatus itemStatus = computeInheritedRulesPreparationPlugin.execute(workerParameters, handlerIO);
        // Then
        StatusCode globalStatus = itemStatus.getGlobalStatus();
        assertThat(globalStatus).isEqualTo(StatusCode.OK);
        JsonLineGenericIterator<JsonLineModel> lines = new JsonLineGenericIterator<>(
            new FileInputStream(resultFile),
            jsonLineModelTypeReference
        );

        List<String> unitIds = lines.stream().map(JsonLineModel::getId).collect(Collectors.toList());
        assertThat(unitIds.size()).isEqualTo(4);
        assertThat(unitIds).contains("id_unit_1", "id_unit_2", "id_unit_3", "id_unit_4");
    }

    @Test
    public void should_delete_distribution_file_after_transfer() throws Exception {
        // Given
        InputStream query = PropertiesUtils.getResourceAsStream("computeInheritedRules/query.json");
        File distributionFile = temporaryFolder.newFile();
        WorkerParameters workerParameters = mock(WorkerParameters.class);
        JsonNode unitResponse = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream("computeInheritedRules/unit.json")
        );
        when(metaDataClient.selectUnits(any())).thenReturn(unitResponse);
        TestHandlerIO handler = new TestHandlerIO();
        handler.setJsonFromWorkspace("query.json", JsonHandler.getFromInputStream(query));
        handler.setNewLocalFile(distributionFile);
        // When
        ItemStatus itemStatus = computeInheritedRulesPreparationPlugin.execute(workerParameters, handler);
        // Then
        StatusCode globalStatus = itemStatus.getGlobalStatus();
        assertThat(globalStatus).isEqualTo(StatusCode.OK);
        File transferredFileToWorkspace = handler.getTransferedFileToWorkspace(UNITS_JSONL_FILE);
        assertThat(Files.exists(transferredFileToWorkspace.toPath())).isEqualTo(false);
    }
}
