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
package fr.gouv.vitam.metadata.core.graph;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.api.VitamRepositoryProvider;
import fr.gouv.vitam.common.database.api.impl.VitamMongoRepository;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.core.reconstruction.RestoreBackupService;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.OfferLogAction;
import fr.gouv.vitam.storage.engine.common.model.Order;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.apache.commons.collections4.IteratorUtils;
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

import static fr.gouv.vitam.metadata.core.graph.StoreGraphService.LAST_GRAPHSTORE_OFFERLOG_BATCH_SIZE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StoreGraphServiceTest {

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

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

        given(
            vitamRepositoryProvider.getVitamMongoRepository(MetadataCollections.UNIT.getVitamCollection())
        ).willReturn(unitRepository);
        given(
            vitamRepositoryProvider.getVitamMongoRepository(MetadataCollections.OBJECTGROUP.getVitamCollection())
        ).willReturn(gotRepository);

        given(findIterableUnit.projection(any())).willReturn(findIterableUnit);
        given(findIterableUnit.iterator()).willReturn(mongoCursorUnit);
        given(findIterableGot.projection(any())).willReturn(findIterableGot);
        given(findIterableGot.iterator()).willReturn(mongoCursorGot);

        when(mongoCursorUnit.next()).thenAnswer(
            o -> Document.parse("{\"_glpd\": \"" + LocalDateUtil.nowFormatted() + "\"}")
        );
    }

    @Test
    @RunWithCustomExecutor
    public void whenNoGraphInOfferThenGetListingReturnInitialDate()
        throws StoreGraphException, StorageServerClientException, StorageNotFoundClientException {
        // given
        when(
            restoreBackupService.getListing(
                VitamConfiguration.getDefaultStrategy(),
                null,
                DataCategory.UNIT_GRAPH,
                null,
                null,
                Order.DESC,
                LAST_GRAPHSTORE_OFFERLOG_BATCH_SIZE
            )
        ).thenReturn(IteratorUtils.emptyIterator());

        LocalDateTime date = storeGraphService.getLastGraphStoreDate(MetadataCollections.UNIT);
        assertThat(date).isEqualTo(StoreGraphService.INITIAL_START_DATE);
    }

    @Test
    @RunWithCustomExecutor
    public void whenGetListingReturnLastDate()
        throws StoreGraphException, StorageServerClientException, StorageNotFoundClientException {
        // given
        String startDate = "2018-01-01-00-00-00-000";
        String endDate = "2018-01-01-06-30-10-123";
        when(
            restoreBackupService.getListing(
                VitamConfiguration.getDefaultStrategy(),
                null,
                DataCategory.UNIT_GRAPH,
                null,
                null,
                Order.DESC,
                LAST_GRAPHSTORE_OFFERLOG_BATCH_SIZE
            )
        ).thenReturn(
            IteratorUtils.singletonIterator(
                new OfferLog(
                    DataCategory.UNIT_GRAPH.getCollectionName(),
                    startDate + "_" + endDate,
                    OfferLogAction.WRITE
                )
            )
        );
        LocalDateTime dateTime = LocalDateUtil.parse(endDate, StoreGraphService.formatter);
        LocalDateTime date = storeGraphService.getLastGraphStoreDate(MetadataCollections.UNIT);
        assertThat(date).isEqualTo(dateTime);
    }

    @Test(expected = StoreGraphException.class)
    @RunWithCustomExecutor
    public void whenGetListingThenExceptionOccurs()
        throws StoreGraphException, StorageServerClientException, StorageNotFoundClientException {
        // given
        when(
            restoreBackupService.getListing(
                VitamConfiguration.getDefaultStrategy(),
                null,
                DataCategory.UNIT_GRAPH,
                null,
                null,
                Order.DESC,
                LAST_GRAPHSTORE_OFFERLOG_BATCH_SIZE
            )
        ).thenThrow(new RuntimeException(""));
        storeGraphService.getLastGraphStoreDate(MetadataCollections.UNIT);
    }

    @Test
    @RunWithCustomExecutor
    public void whenTryStoreGraphThenOK()
        throws StoreGraphException, StorageServerClientException, StorageNotFoundClientException {
        // given
        String startDate = "2018-01-01-00-00-00-000";
        String endDate = "2018-01-01-06-30-10-123";
        when(
            restoreBackupService.getListing(
                VitamConfiguration.getDefaultStrategy(),
                null,
                DataCategory.UNIT_GRAPH,
                null,
                null,
                Order.DESC,
                LAST_GRAPHSTORE_OFFERLOG_BATCH_SIZE
            )
        ).thenReturn(
            IteratorUtils.singletonIterator(
                new OfferLog(
                    DataCategory.UNIT_GRAPH.getCollectionName(),
                    startDate + "_" + endDate,
                    OfferLogAction.WRITE
                )
            )
        );
        when(
            restoreBackupService.getListing(
                VitamConfiguration.getDefaultStrategy(),
                null,
                DataCategory.OBJECTGROUP_GRAPH,
                null,
                null,
                Order.DESC,
                LAST_GRAPHSTORE_OFFERLOG_BATCH_SIZE
            )
        ).thenReturn(IteratorUtils.emptyIterator());

        final int[] cpt = { 0 };
        when(mongoCursorUnit.hasNext()).thenAnswer(o -> {
            if (cpt[0] > 4) {
                return false;
            }
            cpt[0]++;
            return true;
        });

        LocalDateTime dateTime = LocalDateUtil.parse(endDate, StoreGraphService.formatter);

        LocalDateTime date = storeGraphService.getLastGraphStoreDate(MetadataCollections.UNIT);
        assertThat(date).isEqualTo(dateTime);

        Map<MetadataCollections, Integer> stored = storeGraphService.tryStoreGraph();
        assertThat(stored.get(MetadataCollections.UNIT)).isEqualTo(3);
    }

    @Test
    @RunWithCustomExecutor
    public void whenStoreGraphThenNoUnitGraphInThePeriodOK()
        throws StoreGraphException, StorageServerClientException, StorageNotFoundClientException {
        // given
        String startDate = "2018-01-01-00-00-00-000";
        String endDate = "2018-01-01-06-30-10-123";
        when(
            restoreBackupService.getListing(
                VitamConfiguration.getDefaultStrategy(),
                null,
                DataCategory.UNIT_GRAPH,
                null,
                null,
                Order.DESC,
                LAST_GRAPHSTORE_OFFERLOG_BATCH_SIZE
            )
        ).thenReturn(
            IteratorUtils.singletonIterator(
                new OfferLog(
                    DataCategory.UNIT_GRAPH.getCollectionName(),
                    startDate + "_" + endDate,
                    OfferLogAction.WRITE
                )
            )
        );
        when(
            restoreBackupService.getListing(
                VitamConfiguration.getDefaultStrategy(),
                null,
                DataCategory.OBJECTGROUP_GRAPH,
                null,
                null,
                Order.DESC,
                LAST_GRAPHSTORE_OFFERLOG_BATCH_SIZE
            )
        ).thenReturn(IteratorUtils.emptyIterator());

        when(mongoCursorUnit.hasNext()).thenAnswer(o -> false);

        LocalDateTime dateTime = LocalDateUtil.parse(endDate, StoreGraphService.formatter);

        LocalDateTime date = storeGraphService.getLastGraphStoreDate(MetadataCollections.UNIT);
        assertThat(date).isEqualTo(dateTime);

        Map<MetadataCollections, Integer> stored = storeGraphService.tryStoreGraph();
        // Because no zip file created in the offer
        assertThat(stored.get(MetadataCollections.UNIT)).isEqualTo(0);
    }
}
