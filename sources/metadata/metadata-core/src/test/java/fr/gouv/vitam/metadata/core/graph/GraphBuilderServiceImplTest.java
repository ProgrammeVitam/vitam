package fr.gouv.vitam.metadata.core.graph;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Map;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.database.api.impl.VitamElasticsearchRepository;
import fr.gouv.vitam.common.database.api.impl.VitamMongoRepository;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.core.database.collections.VitamRepositoryProvider;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import org.bson.Document;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class GraphBuilderServiceImplTest {


    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @Mock
    private VitamRepositoryProvider vitamRepositoryProvider;

    @Mock
    private VitamMongoRepository unitRepository;

    @Mock
    private VitamMongoRepository gotRepository;

    @Mock
    private VitamElasticsearchRepository unitEsRepository;

    @Mock
    private VitamElasticsearchRepository gotEsRepository;

    @Mock
    private FindIterable findIterableUnit;

    @Mock
    private MongoCursor mongoCursorUnit;


    @Mock
    private FindIterable findIterableGot;

    @Mock
    private MongoCursor mongoCursorGot;

    @InjectMocks
    private GraphBuilderServiceImpl graphBuilderService;

    @Before
    public void setup() throws DatabaseException {
        WorkspaceClient workspaceClient = mock(WorkspaceClient.class);


        given(unitRepository.findDocuments(anyObject(), anyInt())).willReturn(findIterableUnit);
        given(gotRepository.findDocuments(anyObject(), anyInt())).willReturn(findIterableGot);


        given(unitRepository.findDocuments(anyObject(), anyObject())).willReturn(findIterableUnit);
        given(gotRepository.findDocuments(anyObject(), anyObject())).willReturn(findIterableGot);


        doNothing().when(unitEsRepository).update(anyObject());
        doNothing().when(gotEsRepository).update(anyObject());

        given(vitamRepositoryProvider.getVitamMongoRepository(MetadataCollections.UNIT)).willReturn(unitRepository);
        given(vitamRepositoryProvider.getVitamMongoRepository(MetadataCollections.OBJECTGROUP))
            .willReturn(gotRepository);

        given(vitamRepositoryProvider.getVitamESRepository(MetadataCollections.UNIT)).willReturn(unitEsRepository);
        given(vitamRepositoryProvider.getVitamESRepository(MetadataCollections.OBJECTGROUP))
            .willReturn(gotEsRepository);


        given(findIterableUnit.projection(anyObject())).willReturn(findIterableUnit);
        given(findIterableUnit.sort(anyObject())).willReturn(findIterableUnit);
        given(findIterableUnit.limit(anyInt())).willReturn(findIterableUnit);
        given(findIterableUnit.iterator()).willReturn(mongoCursorUnit);
        given(findIterableGot.projection(anyObject())).willReturn(findIterableGot);
        given(findIterableGot.sort(anyObject())).willReturn(findIterableGot);
        given(findIterableGot.limit(anyInt())).willReturn(findIterableGot);
        given(findIterableGot.iterator()).willReturn(mongoCursorGot);

        when(mongoCursorUnit.next()).thenAnswer(
            o -> new Document("_id", GUIDFactory.newGUID().getId())
                .append("_glpd", LocalDateUtil.getFormattedDateForMongo(LocalDateTime.now()))
        );
        when(mongoCursorGot.next()).thenAnswer(
            o -> new Document("_id", GUIDFactory.newGUID().getId())
                .append("_glpd", LocalDateUtil.getFormattedDateForMongo(LocalDateTime.now()))
        );
    }

    @Test
    @RunWithCustomExecutor
    public void whenBuildGraphThenOK() throws GraphBuilderException {
        // Given
        final int[] cpt = {0};
        when(mongoCursorUnit.hasNext()).thenAnswer(o -> {

            if (cpt[0] > 4) {
                return false;
            }
            cpt[0]++;
            return true;
        });

        final int[] cptGot = {0};
        when(mongoCursorGot.hasNext()).thenAnswer(o -> {

            if (cptGot[0] > 4) {
                return false;
            }
            cptGot[0]++;
            return true;
        });


        Map<MetadataCollections, Integer> map = graphBuilderService.computeGraph();
        assertThat(map.get(MetadataCollections.UNIT)).isEqualTo(3);
        assertThat(map.get(MetadataCollections.OBJECTGROUP)).isEqualTo(3);
    }

    @Test
    @RunWithCustomExecutor
    public void whenBuildObjectGroupGraphThenOK() throws GraphBuilderException {
        // Given
        final int[] cpt = {0};
        when(mongoCursorGot.hasNext()).thenAnswer(o -> {

            if (cpt[0] > 4) {
                return false;
            }
            cpt[0]++;
            return true;
        });


        Integer result = graphBuilderService.computeGraph(MetadataCollections.OBJECTGROUP, null);
        assertThat(result).isEqualTo(3);
    }

    @Test
    @RunWithCustomExecutor
    public void whenBuildUnitGraphThenOK() throws GraphBuilderException {
        // Given
        final int[] cpt = {0};
        when(mongoCursorUnit.hasNext()).thenAnswer(o -> {

            if (cpt[0] > 4) {
                return false;
            }
            cpt[0]++;
            return true;
        });


        Integer result = graphBuilderService.computeGraph(MetadataCollections.UNIT, null);
        assertThat(result).isEqualTo(3);
    }


    @Test
    @RunWithCustomExecutor
    public void whenBuildGraphThenQueryResultIsEmpty() throws GraphBuilderException {
        // given
        when(mongoCursorUnit.hasNext()).thenAnswer(o -> false);
        when(mongoCursorGot.hasNext()).thenAnswer(o -> false);


        Map<MetadataCollections, Integer> map = graphBuilderService.computeGraph();
        assertThat(map.get(MetadataCollections.UNIT)).isEqualTo(0);
        assertThat(map.get(MetadataCollections.OBJECTGROUP)).isEqualTo(0);
    }
}
