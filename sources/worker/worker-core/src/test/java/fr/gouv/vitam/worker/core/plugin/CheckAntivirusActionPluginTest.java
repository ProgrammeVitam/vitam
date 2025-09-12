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
package fr.gouv.vitam.worker.core.plugin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.antivirus.client.AntivirusApi;
import fr.gouv.vitam.antivirus.client.AntivirusClientFactory;
import fr.gouv.vitam.antivirus.client.invoker.ApiException;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.processing.WorkFlowExecutionContext;
import fr.gouv.vitam.processing.common.parameter.DefaultWorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.common.HandlerIO;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class CheckAntivirusActionPluginTest {

    private AntivirusClientFactory antivirusClientFactory;
    private AntivirusApi antivirusApi;
    private HandlerIO handlerIO;
    private CheckAntivirusActionPlugin plugin;

    @Before
    public void setUp() {
        antivirusClientFactory = mock(AntivirusClientFactory.class);
        antivirusApi = mock(AntivirusApi.class);
        when(antivirusClientFactory.getAntivirusApi()).thenReturn(antivirusApi);
        handlerIO = mock(HandlerIO.class);
        plugin = new CheckAntivirusActionPlugin(antivirusClientFactory);
    }

    @After
    public void tearDown() {
        // Reset configuration flag potentially modified in tests
        VitamConfiguration.setIgnoreAntivirusCheckForWorker(false);
    }

    @Test
    public void whenAntivirusDisabled_thenSkipAndReturnOk() {
        // Given
        VitamConfiguration.setIgnoreAntivirusCheckForWorker(true);
        DefaultWorkerParameters params = WorkerParametersFactory.newWorkerParameters(
            WorkFlowExecutionContext.COLLECT,
            "pId",
            "stepId",
            "ogId",
            "currentStep",
            new ArrayList<>(),
            "metadataURL",
            "workspaceURL"
        );

        // When
        ItemStatus result = plugin.execute(params, handlerIO);

        // Then
        assertEquals(StatusCode.OK, result.getGlobalStatus());

        // Ensure antivirus client was never requested
        verify(antivirusClientFactory, never()).getAntivirusApi();
        verifyNoInteractions(antivirusApi);
    }

    @Test
    public void whenAntivirusScanOk_thenReturnOk() throws Exception {
        // Given
        VitamConfiguration.setIgnoreAntivirusCheckForWorker(false);
        DefaultWorkerParameters params = WorkerParametersFactory.newWorkerParameters(
            WorkFlowExecutionContext.COLLECT,
            "pId",
            "stepId",
            "ogId",
            "currentStep",
            new ArrayList<>(),
            "metadataURL",
            "workspaceURL"
        );

        // Build OG JSON with two binary objects
        JsonNode og = buildOgJson(
            new String[] { "obj1", "obj2" },
            new String[] { "Content/file1.bin", "Content/file2.bin" }
        );
        when(handlerIO.getInput(0)).thenReturn(og);

        // Mock workspace file retrieval to create real temp files that should be deleted after scan
        List<File> createdFiles = new ArrayList<>();
        when(handlerIO.getFileFromWorkspace(any(WorkFlowExecutionContext.class), anyString())).thenAnswer(
            invocation -> {
                String path = invocation.getArgument(1);
                File f = File.createTempFile("antivirus_test_", "_" + new File(path).getName());
                // ensure contains some bytes
                if (!f.exists()) throw new IOException("Temp file not created");
                createdFiles.add(f);
                return f;
            }
        );

        // Antivirus API does not throw -> treated as Response.Status.OK
        doNothing().when(antivirusApi).scanByPath(anyString());

        // When
        ItemStatus result = plugin.execute(params, handlerIO);

        // Then
        assertEquals(StatusCode.OK, result.getGlobalStatus());
        assertThat(result.getItemsStatus()).containsKey("OG_OBJECTS_ANTIVIRUS_CHECK");
        ItemStatus task = result.getItemsStatus().get("OG_OBJECTS_ANTIVIRUS_CHECK");
        assertEquals(StatusCode.OK, task.getGlobalStatus());
        // two subtasks for two objects
        assertThat(task.getSubTaskStatus().keySet()).containsExactlyInAnyOrder("obj1", "obj2");
        task.getSubTaskStatus().values().forEach(st -> assertEquals(StatusCode.OK, st.getGlobalStatus()));

        // Verify API called twice with correct execution context
        verify(handlerIO, times(2)).getFileFromWorkspace(eq(WorkFlowExecutionContext.COLLECT), anyString());
        verify(antivirusClientFactory, times(1)).getAntivirusApi();
        verify(antivirusApi, times(2)).scanByPath(anyString());
    }

    @Test
    public void whenAntivirusDetectsVirus_thenReturnKo() throws Exception {
        // Given
        VitamConfiguration.setIgnoreAntivirusCheckForWorker(false);
        DefaultWorkerParameters params = WorkerParametersFactory.newWorkerParameters(
            WorkFlowExecutionContext.COLLECT,
            "pId",
            "stepId",
            "ogId",
            "currentStep",
            new ArrayList<>(),
            "metadataURL",
            "workspaceURL"
        );

        // Build OG JSON with two binary objects
        JsonNode og = buildOgJson(
            new String[] { "obj1", "obj2" },
            new String[] { "Content/file1.bin", "Content/file2.bin" }
        );
        when(handlerIO.getInput(0)).thenReturn(og);

        // Mock workspace to return temp files
        List<File> createdFiles = new ArrayList<>();
        when(handlerIO.getFileFromWorkspace(any(WorkFlowExecutionContext.class), anyString())).thenAnswer(
            invocation -> {
                String path = invocation.getArgument(1);
                File f = File.createTempFile("antivirus_test_", "_" + new File(path).getName());
                createdFiles.add(f);
                return f;
            }
        );

        // First scan throws BAD_REQUEST (virus detected), second scan OK
        doThrow(new ApiException(400, "infected")).doNothing().when(antivirusApi).scanByPath(anyString());

        // When
        ItemStatus result = plugin.execute(params, handlerIO);

        // Then
        assertEquals(StatusCode.KO, result.getGlobalStatus());
        assertThat(result.getItemsStatus()).containsKey("OG_OBJECTS_ANTIVIRUS_CHECK");
        ItemStatus task = result.getItemsStatus().get("OG_OBJECTS_ANTIVIRUS_CHECK");
        assertEquals(StatusCode.KO, task.getGlobalStatus());
        assertThat(task.getSubTaskStatus().keySet()).containsExactlyInAnyOrder("obj1", "obj2");
        assertEquals(StatusCode.KO, task.getSubTaskStatus().get("obj1").getGlobalStatus());
        assertEquals(StatusCode.OK, task.getSubTaskStatus().get("obj2").getGlobalStatus());

        // Verify API called twice
        verify(antivirusClientFactory, times(1)).getAntivirusApi();
        verify(antivirusApi, times(2)).scanByPath(anyString());
    }

    private static JsonNode buildOgJson(String[] ids, String[] uris) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        // _work._qualifiers.versions
        ObjectNode work = mapper.createObjectNode();
        ArrayNode workQualifiers = mapper.createArrayNode();
        ObjectNode workQualifier = mapper.createObjectNode();
        ArrayNode workVersions = mapper.createArrayNode();
        // top-level _qualifiers.versions
        ArrayNode topQualifiers = mapper.createArrayNode();
        ObjectNode topQualifier = mapper.createObjectNode();
        ArrayNode topVersions = mapper.createArrayNode();

        for (int i = 0; i < ids.length; i++) {
            ObjectNode versionForMap = mapper.createObjectNode();
            versionForMap.put("_id", ids[i]);
            versionForMap.put("Uri", uris[i]);
            workVersions.add(versionForMap);

            ObjectNode versionForIter = mapper.createObjectNode();
            versionForIter.put("_id", ids[i]);
            versionForIter.put("Uri", uris[i]);
            // no PhysicalId -> indicates a BinaryDataObject to be scanned
            topVersions.add(versionForIter);
        }
        workQualifier.set("versions", workVersions);
        workQualifiers.add(workQualifier);
        work.set("_qualifiers", workQualifiers);
        root.set("_work", work);

        topQualifier.set("versions", topVersions);
        topQualifiers.add(topQualifier);
        root.set("_qualifiers", topQualifiers);

        return root;
    }
}
