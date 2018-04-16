package fr.gouv.vitam.metadata.core.graph;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Map;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import fr.gouv.vitam.common.database.api.impl.VitamMongoRepository;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.core.database.collections.VitamRepositoryProvider;
import fr.gouv.vitam.metadata.core.reconstruction.RestoreBackupService;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class StoreGraphServiceTest {

    public static final String DEFAULT_STRATEGY = "default";

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @Mock
    private VitamRepositoryProvider vitamRepositoryProvider;

    @Mock
    private RestoreBackupService restoreBackupService;

    @Mock
    private VitamMongoRepository unitRepository;

    @Mock
    private VitamMongoRepository gotRepository;

    @Mock
    private WorkspaceClientFactory workspaceClientFactory;

    @Mock
    private StorageClientFactory storageClientFactory;


    @Mock
    private StorageClient storageClient;

    @Mock
    private FindIterable findIterableUnit;

    @Mock
    private MongoCursor mongoCursorUnit;


    @Mock
    private FindIterable findIterableGot;

    @Mock
    private MongoCursor mongoCursorGot;

    @InjectMocks
    private StoreGraphService storeGraphService;

    @Before
    public void setup() {
        WorkspaceClient workspaceClient = mock(WorkspaceClient.class);

        given(workspaceClientFactory.getClient()).willReturn(workspaceClient);
        given(storageClientFactory.getClient()).willReturn(storageClient);

        given(unitRepository.findDocuments(anyObject(), anyInt())).willReturn(findIterableUnit);
        given(gotRepository.findDocuments(anyObject(), anyInt())).willReturn(findIterableGot);

        given(vitamRepositoryProvider.getVitamMongoRepository(MetadataCollections.UNIT)).willReturn(unitRepository);
        given(vitamRepositoryProvider.getVitamMongoRepository(MetadataCollections.OBJECTGROUP))
            .willReturn(gotRepository);

        given(findIterableUnit.projection(anyObject())).willReturn(findIterableUnit);
        given(findIterableUnit.iterator()).willReturn(mongoCursorUnit);
        given(findIterableGot.projection(anyObject())).willReturn(findIterableGot);
        given(findIterableGot.iterator()).willReturn(mongoCursorGot);

    }

    @After
    public void tearDown() {
    }

    @Test
    @RunWithCustomExecutor
    public void whenNoGraphInOfferThengetLastShippingReturnInitialDate() throws StoreGraphException {
        // given
        when(restoreBackupService.getLatestLogs(DEFAULT_STRATEGY, DataCategory.UNIT_GRAPH, 1)).thenReturn(
            Lists.newArrayList());

        LocalDateTime date = storeGraphService.getLastGraphStoreDate(MetadataCollections.UNIT);
        assertThat(date).isEqualTo(StoreGraphService.INITIAL_START_DATE);

    }

    @Test
    @RunWithCustomExecutor
    public void whenGetLastShippingReturnLastShippingDate() throws StoreGraphException {
        // given
        String startDate = "2018-01-01-00-00-00-000";
        String endDate = "2018-01-01-06-30-10-123";
        when(restoreBackupService.getLatestLogs(DEFAULT_STRATEGY, DataCategory.UNIT_GRAPH, 1)).thenReturn(
            Lists.newArrayList(
                new OfferLog(DataCategory.UNIT_GRAPH.getCollectionName(), startDate + "_" + endDate,
                    "write")));


        LocalDateTime dateTime = LocalDateTime.from(StoreGraphService.formatter.parse(endDate));
        LocalDateTime date = storeGraphService.getLastGraphStoreDate(MetadataCollections.UNIT);
        assertThat(date).isEqualTo(dateTime);
    }


    @Test(expected = StoreGraphException.class)
    @RunWithCustomExecutor
    public void whenGetLastShippingThenExceptionOccurs() throws StoreGraphException {
        // given
        when(restoreBackupService.getLatestLogs(DEFAULT_STRATEGY, DataCategory.UNIT_GRAPH, 1))
            .thenThrow(new RuntimeException(""));
        storeGraphService.getLastGraphStoreDate(MetadataCollections.UNIT);
    }

    @Test
    @RunWithCustomExecutor
    public void whenShipGraphThenOK() throws StoreGraphException {
        // given
        String startDate = "2018-01-01-00-00-00-000";
        String endDate = "2018-01-01-06-30-10-123";
        when(restoreBackupService.getLatestLogs(DEFAULT_STRATEGY, DataCategory.UNIT_GRAPH, 1)).thenReturn(
            Lists.newArrayList(
                new OfferLog(DataCategory.UNIT_GRAPH.getCollectionName(), startDate + "_" + endDate,
                    "write")));

        final int[] cpt = {0};
        when(mongoCursorUnit.hasNext()).thenAnswer(o -> {

            if (cpt[0] > 4) {
                return false;
            }
            cpt[0]++;
            return true;
        });

        LocalDateTime dateTime = LocalDateTime.from(StoreGraphService.formatter.parse(endDate));

        LocalDateTime date = storeGraphService.getLastGraphStoreDate(MetadataCollections.UNIT);
        assertThat(date).isEqualTo(dateTime);

        Map<MetadataCollections, Integer> shipped = storeGraphService.tryStoreGraph();
        assertThat(shipped.get(MetadataCollections.UNIT)).isEqualTo(3);
    }


    @Test
    @RunWithCustomExecutor
    public void whenStoreGraphThenNoUnitGraphInThePeriodOK() throws StoreGraphException {
        // given
        String startDate = "2018-01-01-00-00-00-000";
        String endDate = "2018-01-01-06-30-10-123";
        when(restoreBackupService.getLatestLogs(DEFAULT_STRATEGY, DataCategory.UNIT_GRAPH, 1)).thenReturn(
            Lists.newArrayList(
                new OfferLog(DataCategory.UNIT_GRAPH.getCollectionName(), startDate + "_" + endDate,
                    "write")));


        when(mongoCursorUnit.hasNext()).thenAnswer(o -> false);

        LocalDateTime dateTime = LocalDateTime.from(StoreGraphService.formatter.parse(endDate));

        LocalDateTime date = storeGraphService.getLastGraphStoreDate(MetadataCollections.UNIT);
        assertThat(date).isEqualTo(dateTime);

        Map<MetadataCollections, Integer> shipped = storeGraphService.tryStoreGraph();
        // Because no zip file created in the offer
        assertThat(shipped.get(MetadataCollections.UNIT)).isEqualTo(0);
    }
}
