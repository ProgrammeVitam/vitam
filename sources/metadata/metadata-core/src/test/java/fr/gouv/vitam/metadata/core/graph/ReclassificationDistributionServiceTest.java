package fr.gouv.vitam.metadata.core.graph;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.metadata.core.MetaDataImpl;
import fr.gouv.vitam.worker.core.distribution.JsonLineGenericIterator;
import fr.gouv.vitam.worker.core.distribution.JsonLineModel;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@RunWithCustomExecutor
public class ReclassificationDistributionServiceTest {

    private static final TypeReference<JsonLineModel> TYPE_REFERENCE = new TypeReference<JsonLineModel>() {
    };

    @ClassRule
    public static RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private WorkspaceClientFactory workspaceClientFactory;
    @Mock
    private WorkspaceClient workspaceClient;
    @Mock
    private MetaDataImpl metaData;

    @InjectMocks
    ReclassificationDistributionService instance;

    private String operationId;

    @Before
    public void init() throws Exception {
        doReturn(workspaceClient).when(workspaceClientFactory).getClient();

        int tenant = 0;
        VitamThreadUtils.getVitamSession().setTenantId(tenant);
        operationId = GUIDFactory.newRequestIdGUID(tenant).toString();
        VitamThreadUtils.getVitamSession().setRequestId(operationId);
    }

    @Test
    public void testExportReclassificationChildNodes() throws Exception {

        // Given
        String unitsToUpdateJsonLineFileName = "UnitsToUpdate.jsonl";
        String objectGroupsToUpdateJsonLineFileName = "ObjectGroupsToUpdate.jsonl";

        RequestResponseOK<JsonNode> results = new RequestResponseOK<JsonNode>()
            .addAllResults(Arrays.asList(
                JsonHandler.createObjectNode().put("#id", "id1").put("#object", "og1"),
                JsonHandler.createObjectNode().put("#id", "id2"),
                JsonHandler.createObjectNode().put("#id", "id3-1").put("#object", "og3"),
                JsonHandler.createObjectNode().put("#id", "id3-2").put("#object", "og3"),
                JsonHandler.createObjectNode().put("#id", "id4").put("#object", "og4")
            ));

        doReturn(results).when(metaData).selectUnitsByQuery(any());

        Map<String, File> savedFiles = new HashMap<>();
        doAnswer((args) -> {
            File file = this.folder.newFile();
            FileUtils.copyInputStreamToFile(args.getArgument(2), file);
            savedFiles.put(args.getArgument(1), file);
            return null;
        }).when(workspaceClient).putObject(eq(operationId), anyString(), any(InputStream.class));

        // When
        Set<String> ids = new HashSet<>(Arrays.asList("id1", "id2", "id3"));
        instance
            .exportReclassificationChildNodes(ids, unitsToUpdateJsonLineFileName, objectGroupsToUpdateJsonLineFileName);

        // Then
        verify(workspaceClient).putObject(eq(operationId), eq(unitsToUpdateJsonLineFileName), any(InputStream.class));
        verify(workspaceClient)
            .putObject(eq(operationId), eq(objectGroupsToUpdateJsonLineFileName), any(InputStream.class));

        List<String> unitIds = readDistributionFile(savedFiles.get(unitsToUpdateJsonLineFileName));
        List<String> objectGroupIds = readDistributionFile(savedFiles.get(objectGroupsToUpdateJsonLineFileName));

        assertThat(unitIds).containsExactly("id1", "id2", "id3-1", "id3-2", "id4");
        assertThat(objectGroupIds).containsExactly("og1", "og3", "og4");
    }

    private List<String> readDistributionFile(File file) throws IOException {
        try (InputStream is = new FileInputStream(file);
            JsonLineGenericIterator<JsonLineModel> jsonLineGenericIterator = new JsonLineGenericIterator<>(is,
                TYPE_REFERENCE)) {
            return jsonLineGenericIterator
                .stream()
                .map(JsonLineModel::getId)
                .collect(Collectors.toList());
        }
    }
}
