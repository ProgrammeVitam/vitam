package fr.gouv.vitam.metadata.core.graph;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.database.api.VitamRepositoryProvider;
import fr.gouv.vitam.common.database.api.impl.VitamMongoRepository;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.core.reconstruction.RestoreBackupService;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.OfferLogAction;
import fr.gouv.vitam.storage.engine.common.model.Order;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.assertj.core.util.Lists;
import org.bson.Document;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StoreGraphServiceTest {

    public static final String DEFAULT_STRATEGY = "default";

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

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

        given(unitRepository.findDocuments(any(), anyInt())).willReturn(findIterableUnit);
        given(gotRepository.findDocuments(any(), anyInt())).willReturn(findIterableGot);

        given(vitamRepositoryProvider.getVitamMongoRepository(MetadataCollections.UNIT.getVitamCollection())).willReturn(unitRepository);
        given(vitamRepositoryProvider.getVitamMongoRepository(MetadataCollections.OBJECTGROUP.getVitamCollection()))
            .willReturn(gotRepository);

        given(findIterableUnit.projection(any())).willReturn(findIterableUnit);
        given(findIterableUnit.iterator()).willReturn(mongoCursorUnit);
        given(findIterableGot.projection(any())).willReturn(findIterableGot);
        given(findIterableGot.iterator()).willReturn(mongoCursorGot);

        when(mongoCursorUnit.next()).thenAnswer(
            o -> Document.parse("{\"_glpd\": \"" + LocalDateUtil.getFormattedDateForMongo(LocalDateTime.now()) + "\"}")
        );
    }

    @Test
    @RunWithCustomExecutor
    public void whenNoGraphInOfferThenGetListingReturnInitialDate() throws StoreGraphException {
        // given
        when(restoreBackupService.getListing(DEFAULT_STRATEGY, DataCategory.UNIT_GRAPH, null, 1, Order.DESC))
            .thenReturn(
                Lists.newArrayList());

        LocalDateTime date = storeGraphService.getLastGraphStoreDate(MetadataCollections.UNIT);
        assertThat(date).isEqualTo(StoreGraphService.INITIAL_START_DATE);

    }

    @Test
    @RunWithCustomExecutor
    public void whenGetListingReturnLastDate() throws StoreGraphException {
        // given
        String startDate = "2018-01-01-00-00-00-000";
        String endDate = "2018-01-01-06-30-10-123";
        when(restoreBackupService.getListing(DEFAULT_STRATEGY, DataCategory.UNIT_GRAPH, null, 1, Order.DESC))
            .thenReturn(
                Lists.newArrayList(
                    new OfferLog(DataCategory.UNIT_GRAPH.getCollectionName(), startDate + "_" + endDate,
                        OfferLogAction.WRITE)));


        LocalDateTime dateTime = LocalDateTime.from(StoreGraphService.formatter.parse(endDate));
        LocalDateTime date = storeGraphService.getLastGraphStoreDate(MetadataCollections.UNIT);
        assertThat(date).isEqualTo(dateTime);
    }


    @Test(expected = StoreGraphException.class)
    @RunWithCustomExecutor
    public void whenGetListingThenExceptionOccurs() throws StoreGraphException {
        // given
        when(restoreBackupService.getListing(DEFAULT_STRATEGY, DataCategory.UNIT_GRAPH, null, 1, Order.DESC))
            .thenThrow(new RuntimeException(""));
        storeGraphService.getLastGraphStoreDate(MetadataCollections.UNIT);
    }

    @Test
    @RunWithCustomExecutor
    public void whenTryStoreGraphThenOK() throws StoreGraphException {
        // given
        String startDate = "2018-01-01-00-00-00-000";
        String endDate = "2018-01-01-06-30-10-123";
        when(restoreBackupService.getListing(DEFAULT_STRATEGY, DataCategory.UNIT_GRAPH, null, 1, Order.DESC))
            .thenReturn(
                Lists.newArrayList(
                    new OfferLog(DataCategory.UNIT_GRAPH.getCollectionName(), startDate + "_" + endDate,
                        OfferLogAction.WRITE)));

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

        Map<MetadataCollections, Integer> stored = storeGraphService.tryStoreGraph();
        assertThat(stored.get(MetadataCollections.UNIT)).isEqualTo(3);
    }


    @Test
    @RunWithCustomExecutor
    public void whenStoreGraphThenNoUnitGraphInThePeriodOK() throws StoreGraphException {
        // given
        String startDate = "2018-01-01-00-00-00-000";
        String endDate = "2018-01-01-06-30-10-123";
        when(restoreBackupService.getListing(DEFAULT_STRATEGY, DataCategory.UNIT_GRAPH, null, 1, Order.DESC))
            .thenReturn(
                Lists.newArrayList(
                    new OfferLog(DataCategory.UNIT_GRAPH.getCollectionName(), startDate + "_" + endDate,
                        OfferLogAction.WRITE)));


        when(mongoCursorUnit.hasNext()).thenAnswer(o -> false);

        LocalDateTime dateTime = LocalDateTime.from(StoreGraphService.formatter.parse(endDate));

        LocalDateTime date = storeGraphService.getLastGraphStoreDate(MetadataCollections.UNIT);
        assertThat(date).isEqualTo(dateTime);

        Map<MetadataCollections, Integer> stored = storeGraphService.tryStoreGraph();
        // Because no zip file created in the offer
        assertThat(stored.get(MetadataCollections.UNIT)).isEqualTo(0);
    }
}
