package fr.gouv.vitam.metadata.core.graph;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import fr.gouv.vitam.common.database.api.VitamRepositoryProvider;
import fr.gouv.vitam.common.database.api.impl.VitamMongoRepository;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.worker.core.distribution.ChainedFileModel;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWithCustomExecutor
public class ReclassificationDistributionServiceTest {

    @ClassRule
    public static RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private WorkspaceClientFactory workspaceClientFactory;
    @Mock
    private WorkspaceClient workspaceClient;

    @Mock
    private VitamRepositoryProvider vitamRepositoryProvider;
    @Mock
    private VitamMongoRepository vitamMongoRepository;

    @InjectMocks
    ReclassificationDistributionService instance;

    @Before
    public void init() throws Exception {
        doReturn(workspaceClient).when(workspaceClientFactory).getClient();
        doReturn(vitamMongoRepository).when(vitamRepositoryProvider).getVitamMongoRepository(MetadataCollections.UNIT.getVitamCollection());

        int tenant = 0;
        VitamThreadUtils.getVitamSession().setTenantId(tenant);
        String operationId = GUIDFactory.newRequestIdGUID(tenant).toString();
        VitamThreadUtils.getVitamSession().setRequestId(operationId);
    }

    @Test
    public void testExportReclassificationChildNodes() throws Exception {

        // Given
        String unitsToUpdateChainedFileName = "unitChainedFileName.json";
        String objectGroupsToUpdateChainedFileName = "objectGroupChainedFileName.json";

        FindIterable<Document> iterable = mock(FindIterable.class);
        doReturn(iterable).when(vitamMongoRepository).findDocuments(any(Bson.class), anyInt());
        doReturn(iterable).when(iterable).projection(any());
        MongoCursor<Document> iterator = mock(MongoCursor.class);
        doReturn(iterator).when(iterable).iterator();
        when(iterator.hasNext()).thenReturn(true, true, true, true, false);
        when(iterator.next()).thenReturn(
            new Document().append("_id", "id1").append("_og", "og1"),
            new Document().append("_id", "id2"),
            new Document().append("_id", "id3").append("_og", "og3"),
            new Document().append("_id", "id4").append("_og", "og4"));


        // When
        Set<String> ids = new HashSet<>(Arrays.asList("id1", "id2", "id3"));
        instance
            .exportReclassificationChildNodes(ids, unitsToUpdateChainedFileName, objectGroupsToUpdateChainedFileName);

        // Then
        ArgumentCaptor<InputStream> unitsInputStream = ArgumentCaptor.forClass(InputStream.class);
        verify(workspaceClient)
            .putObject(eq(VitamThreadUtils.getVitamSession().getRequestId()), eq(unitsToUpdateChainedFileName),
                unitsInputStream.capture());
        ChainedFileModel unitChainedFileModel =
            JsonHandler.getFromInputStream(unitsInputStream.getValue(), ChainedFileModel.class);
        assertThat(unitChainedFileModel.getElements()).containsExactlyInAnyOrder("id1", "id2", "id3", "id4");

        ArgumentCaptor<InputStream> objectGroupsInputStream = ArgumentCaptor.forClass(InputStream.class);
        verify(workspaceClient)
            .putObject(eq(VitamThreadUtils.getVitamSession().getRequestId()), eq(objectGroupsToUpdateChainedFileName),
                objectGroupsInputStream.capture());
        ChainedFileModel objectGroupChainedFileModel =
            JsonHandler.getFromInputStream(objectGroupsInputStream.getValue(), ChainedFileModel.class);
        assertThat(objectGroupChainedFileModel.getElements()).containsExactlyInAnyOrder("og1", "og3", "og4");
    }
}
