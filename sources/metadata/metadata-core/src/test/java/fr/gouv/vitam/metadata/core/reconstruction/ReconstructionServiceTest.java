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
package fr.gouv.vitam.metadata.core.reconstruction;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.MongoBulkWriteException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.client.ClientMockResultHelper;
import fr.gouv.vitam.common.database.api.VitamRepositoryFactory;
import fr.gouv.vitam.common.database.api.VitamRepositoryProvider;
import fr.gouv.vitam.common.database.api.impl.VitamElasticsearchRepository;
import fr.gouv.vitam.common.database.api.impl.VitamMongoRepository;
import fr.gouv.vitam.common.database.offset.OffsetRepository;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.junit.FakeInputStream;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.metadata.core.config.ElasticsearchMetadataIndexManager;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollectionsTestUtils;
import fr.gouv.vitam.metadata.core.model.ReconstructionRequestItem;
import fr.gouv.vitam.metadata.core.model.ReconstructionResponseItem;
import fr.gouv.vitam.metadata.core.utils.MappingLoaderTestUtils;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.OfferLogAction;
import fr.gouv.vitam.storage.engine.common.model.Order;
import fr.gouv.vitam.storage.engine.common.referential.model.OfferReference;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageStrategy;
import org.apache.commons.collections4.IteratorUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.mongodb.client.model.Projections.include;
import static fr.gouv.vitam.common.database.utils.MetadataDocumentHelper.getComputedGraphObjectGroupFields;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ReconstructionService tests.
 */
public class ReconstructionServiceTest {

    public static final String STRATEGY_UNIT = "strategy-md-t10";

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    private VitamRepositoryProvider vitamRepositoryProvider;
    private VitamMongoRepository mongoRepository;
    private VitamElasticsearchRepository esRepository;
    private RestoreBackupService restoreBackupService;
    private LogbookLifeCyclesClientFactory logbookLifecycleClientFactory;
    private LogbookLifeCyclesClient logbookLifecycleClient;
    private StorageClientFactory storageClientFactory;
    private StorageClient storageClient;

    private ReconstructionRequestItem requestItem;

    private OffsetRepository offsetRepository;
    private final ElasticsearchMetadataIndexManager indexManager = MetadataCollectionsTestUtils
        .createTestIndexManager(Arrays.asList(1, 10), Collections.emptyMap(),
            MappingLoaderTestUtils.getTestMappingLoader());

    @Before
    public void setup() {
        vitamRepositoryProvider = mock(VitamRepositoryFactory.class);
        mongoRepository = mock(VitamMongoRepository.class);
        esRepository = mock(VitamElasticsearchRepository.class);
        offsetRepository = mock(OffsetRepository.class);
        when(vitamRepositoryProvider.getVitamMongoRepository(any())).thenReturn(mongoRepository);
        when(vitamRepositoryProvider.getVitamESRepository(any(), any())).thenReturn(esRepository);

        restoreBackupService = mock(RestoreBackupService.class);
        logbookLifecycleClientFactory = mock(LogbookLifeCyclesClientFactory.class);
        logbookLifecycleClient = mock(LogbookLifeCyclesClient.class);
        when(logbookLifecycleClientFactory.getClient()).thenReturn(logbookLifecycleClient);

        storageClientFactory = mock(StorageClientFactory.class);
        storageClient = mock(StorageClient.class);
        when(storageClientFactory.getClient()).thenReturn(storageClient);

        requestItem = new ReconstructionRequestItem();
        requestItem.setCollection("UNIT").setTenant(10).setLimit(100)/*.setStrategy(STRATEGY_UNIT)*/;
    }

    @RunWithCustomExecutor
    @Test
    public void should_return_new_offset_when_item_unit_is_ok() throws Exception {
        // given
        when(offsetRepository.findOffsetBy(10, STRATEGY_UNIT, MetadataCollections.UNIT.getName())).thenReturn(100L);
        when(restoreBackupService.getListing(STRATEGY_UNIT, DataCategory.UNIT, 100L,
            requestItem.getLimit(), Order.ASC, VitamConfiguration.getRestoreBulkSize())).thenReturn(
            IteratorUtils.arrayIterator(getOfferLog(100L), getOfferLog(101L)));
        when(restoreBackupService.loadData(STRATEGY_UNIT, MetadataCollections.UNIT, "100", 100L))
            .thenReturn(getUnitMetadataBackupModel("100", 100L));
        when(restoreBackupService.loadData(STRATEGY_UNIT, MetadataCollections.UNIT, "101", 101L))
            .thenReturn(getUnitMetadataBackupModel("101", 101L));
        when(storageClient.getStorageStrategies()).thenReturn(getStorageStrategies());
        ArgumentCaptor<List<JsonNode>> unitLfcsCaptor = ArgumentCaptor.forClass(List.class);
        doNothing().when(logbookLifecycleClient).createRawbulkUnitlifecycles(unitLfcsCaptor.capture());

        ReconstructionService reconstructionService =
            new ReconstructionService(vitamRepositoryProvider, restoreBackupService, logbookLifecycleClientFactory,
                storageClientFactory, offsetRepository, indexManager);

        FindIterable findIterable = mock(FindIterable.class);
        final MongoCursor<String> iterator = mock(MongoCursor.class);
        when(mongoRepository.findDocuments(any(), any())).thenReturn(findIterable);
        when(findIterable.iterator()).thenReturn(iterator);
        when(iterator.hasNext()).thenReturn(Boolean.FALSE);
        // when
        ReconstructionResponseItem realResponseItem = reconstructionService.reconstruct(requestItem);
        // then
        assertThat(realResponseItem).isNotNull();
        assertThat(realResponseItem.getCollection()).isEqualTo(MetadataCollections.UNIT.name());
        verify(offsetRepository).createOrUpdateOffset(10, STRATEGY_UNIT, MetadataCollections.UNIT.getName(), 101L);
        assertThat(realResponseItem.getTenant()).isEqualTo(10);
        assertThat(realResponseItem.getStatus()).isEqualTo(StatusCode.OK);
    }


    @RunWithCustomExecutor
    @Test
    public void should_return_new_offsets_when_item_unit_for_two_strategies_are_ok() throws Exception {
        // given
        when(offsetRepository.findOffsetBy(eq(10), contains(STRATEGY_UNIT),
            eq(MetadataCollections.UNIT.getName()))).thenReturn(100L);
        when(restoreBackupService.getListing(contains(STRATEGY_UNIT), eq(DataCategory.UNIT), eq(100L),
            eq(requestItem.getLimit()), eq(Order.ASC), eq(VitamConfiguration.getRestoreBulkSize()))).thenReturn(
            IteratorUtils.arrayIterator(getOfferLog(100L), getOfferLog(101L)));
        when(restoreBackupService.loadData(contains(STRATEGY_UNIT), eq(MetadataCollections.UNIT), eq("100"), eq(100L)))
            .thenReturn(getUnitMetadataBackupModel("100", 100L));
        when(restoreBackupService.loadData(contains(STRATEGY_UNIT), eq(MetadataCollections.UNIT), eq("101"), eq(101L)))
            .thenReturn(getUnitMetadataBackupModel("101", 101L));
        when(storageClient.getStorageStrategies()).thenReturn(
            getStorageStrategiesWithSameReferentOfferForDifferentStrategies());
        ArgumentCaptor<List<JsonNode>> unitLfcsCaptor = ArgumentCaptor.forClass(List.class);
        doNothing().when(logbookLifecycleClient).createRawbulkUnitlifecycles(unitLfcsCaptor.capture());

        ReconstructionService reconstructionService =
            new ReconstructionService(vitamRepositoryProvider, restoreBackupService, logbookLifecycleClientFactory,
                storageClientFactory, offsetRepository, indexManager);

        FindIterable findIterable = mock(FindIterable.class);
        final MongoCursor<String> iterator = mock(MongoCursor.class);
        when(mongoRepository.findDocuments(any(), any())).thenReturn(findIterable);
        when(findIterable.iterator()).thenReturn(iterator);
        when(iterator.hasNext()).thenReturn(Boolean.FALSE);
        // when
        ReconstructionResponseItem realResponseItem = reconstructionService.reconstruct(requestItem);
        // then
        assertThat(realResponseItem).isNotNull();
        assertThat(realResponseItem.getCollection()).isEqualTo(MetadataCollections.UNIT.name());
        verify(offsetRepository).createOrUpdateOffset(eq(10), startsWith(STRATEGY_UNIT),
            eq(MetadataCollections.UNIT.getName()), eq(101L));
        assertThat(realResponseItem.getTenant()).isEqualTo(10);
        assertThat(realResponseItem.getStatus()).isEqualTo(StatusCode.OK);
    }

    @RunWithCustomExecutor
    @Test
    public void should_ignore_missing_files_and_return_new_offset_when_item_unit_is_ok() throws Exception {
        // given
        when(offsetRepository.findOffsetBy(10, STRATEGY_UNIT, MetadataCollections.UNIT.getName())).thenReturn(100L);
        when(restoreBackupService.getListing(STRATEGY_UNIT, DataCategory.UNIT, 100L,
            requestItem.getLimit(), Order.ASC, VitamConfiguration.getRestoreBulkSize())).thenReturn(
            IteratorUtils.arrayIterator(getOfferLog(100L), getOfferLog(101L)));
        when(restoreBackupService.loadData(STRATEGY_UNIT, MetadataCollections.UNIT, "100", 100L))
            .thenReturn(getUnitMetadataBackupModel("100", 100L));
        when(restoreBackupService.loadData(STRATEGY_UNIT, MetadataCollections.UNIT, "101", 101L))
            .thenThrow(new StorageNotFoundException(""));
        when(storageClient.getStorageStrategies()).thenReturn(getStorageStrategies());
        ArgumentCaptor<List<JsonNode>> unitLfcsCaptor = ArgumentCaptor.forClass(List.class);
        doNothing().when(logbookLifecycleClient).createRawbulkUnitlifecycles(unitLfcsCaptor.capture());

        ReconstructionService reconstructionService =
            new ReconstructionService(vitamRepositoryProvider, restoreBackupService, logbookLifecycleClientFactory,
                storageClientFactory, offsetRepository, indexManager);

        FindIterable findIterable = mock(FindIterable.class);
        final MongoCursor<String> iterator = mock(MongoCursor.class);
        when(mongoRepository.findDocuments(any(), any())).thenReturn(findIterable);
        when(findIterable.iterator()).thenReturn(iterator);
        when(iterator.hasNext()).thenReturn(Boolean.FALSE);
        // when
        ReconstructionResponseItem realResponseItem = reconstructionService.reconstruct(requestItem);
        // then
        assertThat(realResponseItem).isNotNull();
        assertThat(realResponseItem.getCollection()).isEqualTo(MetadataCollections.UNIT.name());
        verify(offsetRepository).createOrUpdateOffset(10, STRATEGY_UNIT, MetadataCollections.UNIT.getName(), 101L);
        assertThat(realResponseItem.getTenant()).isEqualTo(10);
        assertThat(realResponseItem.getStatus()).isEqualTo(StatusCode.OK);
    }

    @RunWithCustomExecutor
    @Test
    public void should_return_new_offset_when_item_got_is_ok() throws Exception {
        // given
        when(offsetRepository.findOffsetBy(10, STRATEGY_UNIT, MetadataCollections.OBJECTGROUP.getName())).thenReturn(
            100L);

        requestItem.setCollection("ObjectGroup");
        when(restoreBackupService.getListing(STRATEGY_UNIT, DataCategory.OBJECTGROUP, 100l,
            requestItem.getLimit(), Order.ASC, VitamConfiguration.getRestoreBulkSize()))
            .thenReturn(IteratorUtils.arrayIterator(getOfferLog(100), getOfferLog(101)));
        when(restoreBackupService.loadData(STRATEGY_UNIT, MetadataCollections.OBJECTGROUP, "100", 100L))
            .thenReturn(getGotMetadataBackupModel("100", 100L));
        when(restoreBackupService.loadData(STRATEGY_UNIT, MetadataCollections.OBJECTGROUP, "101", 101L))
            .thenReturn(getGotMetadataBackupModel("101", 101L));
        when(storageClient.getStorageStrategies()).thenReturn(getStorageStrategies());
        doNothing().when(logbookLifecycleClient).createRawbulkObjectgrouplifecycles(any());

        ReconstructionService reconstructionService = new ReconstructionService(vitamRepositoryProvider,
            restoreBackupService, logbookLifecycleClientFactory, storageClientFactory, offsetRepository,
            indexManager);

        FindIterable findIterable = mock(FindIterable.class);
        final MongoCursor<String> iterator = mock(MongoCursor.class);
        when(mongoRepository.findDocuments(any(), any())).thenReturn(findIterable);
        when(findIterable.iterator()).thenReturn(iterator);
        when(iterator.hasNext()).thenReturn(Boolean.FALSE);
        // when
        ReconstructionResponseItem realResponseItem = reconstructionService.reconstruct(requestItem);
        // then
        assertThat(realResponseItem).isNotNull();
        assertThat(realResponseItem.getCollection()).isEqualTo(MetadataCollections.OBJECTGROUP.name());
        verify(offsetRepository).createOrUpdateOffset(10, STRATEGY_UNIT, MetadataCollections.OBJECTGROUP.getName(),
            101L);
        assertThat(realResponseItem.getTenant()).isEqualTo(10);
        assertThat(realResponseItem.getStatus()).isEqualTo(StatusCode.OK);
    }

    @RunWithCustomExecutor
    @Test
    public void should_ignore_missing_files_and_return_new_offset_when_item_got_is_ok() throws Exception {
        // given
        when(offsetRepository.findOffsetBy(10, STRATEGY_UNIT, MetadataCollections.OBJECTGROUP.getName())).thenReturn(
            100L);

        requestItem.setCollection("ObjectGroup");
        when(restoreBackupService.getListing(STRATEGY_UNIT, DataCategory.OBJECTGROUP, 100l,
            requestItem.getLimit(), Order.ASC, VitamConfiguration.getRestoreBulkSize()))
            .thenReturn(IteratorUtils.arrayIterator(getOfferLog(100), getOfferLog(101)));
        when(restoreBackupService.loadData(STRATEGY_UNIT, MetadataCollections.OBJECTGROUP, "100", 100L))
            .thenReturn(getGotMetadataBackupModel("100", 100L));
        when(restoreBackupService.loadData(STRATEGY_UNIT, MetadataCollections.OBJECTGROUP, "101", 101L))
            .thenThrow(new StorageNotFoundException(""));
        when(storageClient.getStorageStrategies()).thenReturn(getStorageStrategies());
        doNothing().when(logbookLifecycleClient).createRawbulkObjectgrouplifecycles(any());

        ReconstructionService reconstructionService = new ReconstructionService(vitamRepositoryProvider,
            restoreBackupService, logbookLifecycleClientFactory, storageClientFactory, offsetRepository,
            indexManager);

        FindIterable findIterable = mock(FindIterable.class);
        final MongoCursor<String> iterator = mock(MongoCursor.class);
        when(mongoRepository.findDocuments(any(), any())).thenReturn(findIterable);
        when(findIterable.iterator()).thenReturn(iterator);
        when(iterator.hasNext()).thenReturn(Boolean.FALSE);
        // when
        ReconstructionResponseItem realResponseItem = reconstructionService.reconstruct(requestItem);
        // then
        assertThat(realResponseItem).isNotNull();
        assertThat(realResponseItem.getCollection()).isEqualTo(MetadataCollections.OBJECTGROUP.name());
        verify(offsetRepository).createOrUpdateOffset(10, STRATEGY_UNIT, MetadataCollections.OBJECTGROUP.getName(),
            101L);
        assertThat(realResponseItem.getTenant()).isEqualTo(10);
        assertThat(realResponseItem.getStatus()).isEqualTo(StatusCode.OK);
    }

    @RunWithCustomExecutor
    @Test
    public void should_return_new_offset_when_item_got_graph_is_ok() throws Exception {
        VitamConfiguration.setAdminTenant(1);
        // given
        when(offsetRepository.findOffsetBy(1, VitamConfiguration.getDefaultStrategy(),
            DataCategory.OBJECTGROUP_GRAPH.name())).thenReturn(100L);

        requestItem.setCollection(DataCategory.OBJECTGROUP_GRAPH.name());
        when(restoreBackupService.getListing(VitamConfiguration.getDefaultStrategy(), DataCategory.OBJECTGROUP_GRAPH,
            100l,
            requestItem.getLimit(), Order.ASC, VitamConfiguration.getRestoreBulkSize()))
            .thenReturn(IteratorUtils.arrayIterator(getOfferLog("1970-01-01-00-00-00-000_2018-04-20-17-00-01-444", 100),
                getOfferLog("2018-04-20-17-00-01-444_2018-05-20-17-00-01-445", 101)));
        when(restoreBackupService
            .loadData(VitamConfiguration.getDefaultStrategy(), DataCategory.OBJECTGROUP_GRAPH,
                "1970-01-01-00-00-00-000_2018-04-20-17-00-01-444"))
            .thenReturn(new FakeInputStream(1, false));

        when(restoreBackupService
            .loadData(VitamConfiguration.getDefaultStrategy(), DataCategory.OBJECTGROUP_GRAPH,
                "2018-04-20-17-00-01-444_2018-05-20-17-00-01-445"))
            .thenReturn(new FakeInputStream(1, true));

        when(storageClient.getStorageStrategies()).thenReturn(getStorageStrategies());

        ReconstructionService reconstructionService = new ReconstructionService(vitamRepositoryProvider,
            restoreBackupService, logbookLifecycleClientFactory, storageClientFactory, offsetRepository,
            indexManager);

        FindIterable findIterable = mock(FindIterable.class);
        final MongoCursor<String> iterator = mock(MongoCursor.class);
        final Bson projection = include(getComputedGraphObjectGroupFields());
        when(mongoRepository.findDocuments(anyList(), eq(projection))).thenReturn(findIterable);
        when(findIterable.iterator()).thenReturn(iterator);
        when(iterator.hasNext()).thenReturn(Boolean.FALSE);
        // when
        ReconstructionResponseItem realResponseItem = reconstructionService.reconstruct(requestItem);
        // then
        assertThat(realResponseItem).isNotNull();
        assertThat(realResponseItem.getCollection()).isEqualTo(DataCategory.OBJECTGROUP_GRAPH.name());
        verify(offsetRepository).createOrUpdateOffset(1, VitamConfiguration.getDefaultStrategy(),
            DataCategory.OBJECTGROUP_GRAPH.name(), 101L);
        assertThat(realResponseItem.getTenant()).isEqualTo(1);
        assertThat(realResponseItem.getStatus()).isEqualTo(StatusCode.OK);
    }

    @RunWithCustomExecutor
    @Test
    public void should_ignore_missing_files_and_return_new_offset_when_item_got_graph_is_ok() throws Exception {
        VitamConfiguration.setAdminTenant(1);
        // given
        when(offsetRepository.findOffsetBy(1, VitamConfiguration.getDefaultStrategy(),
            DataCategory.OBJECTGROUP_GRAPH.name())).thenReturn(100L);

        requestItem.setCollection(DataCategory.OBJECTGROUP_GRAPH.name());
        when(restoreBackupService.getListing(VitamConfiguration.getDefaultStrategy(), DataCategory.OBJECTGROUP_GRAPH,
            100l,
            requestItem.getLimit(), Order.ASC, VitamConfiguration.getRestoreBulkSize()))
            .thenReturn(IteratorUtils.arrayIterator(getOfferLog("1970-01-01-00-00-00-000_2018-04-20-17-00-01-444", 100),
                getOfferLog("2018-04-20-17-00-01-444_2018-05-20-17-00-01-445", 101)));
        when(restoreBackupService
            .loadData(VitamConfiguration.getDefaultStrategy(), DataCategory.OBJECTGROUP_GRAPH,
                "1970-01-01-00-00-00-000_2018-04-20-17-00-01-444"))
            .thenReturn(new FakeInputStream(1, false));

        when(restoreBackupService
            .loadData(VitamConfiguration.getDefaultStrategy(), DataCategory.OBJECTGROUP_GRAPH,
                "2018-04-20-17-00-01-444_2018-05-20-17-00-01-445"))
            .thenThrow(new StorageNotFoundException(""));

        when(storageClient.getStorageStrategies()).thenReturn(getStorageStrategies());

        ReconstructionService reconstructionService = new ReconstructionService(vitamRepositoryProvider,
            restoreBackupService, logbookLifecycleClientFactory, storageClientFactory, offsetRepository,
            indexManager);

        FindIterable findIterable = mock(FindIterable.class);
        final MongoCursor<String> iterator = mock(MongoCursor.class);
        final Bson projection = include(getComputedGraphObjectGroupFields());
        when(mongoRepository.findDocuments(anyList(), eq(projection))).thenReturn(findIterable);
        when(findIterable.iterator()).thenReturn(iterator);
        when(iterator.hasNext()).thenReturn(Boolean.FALSE);
        // when
        ReconstructionResponseItem realResponseItem = reconstructionService.reconstruct(requestItem);
        // then
        assertThat(realResponseItem).isNotNull();
        assertThat(realResponseItem.getCollection()).isEqualTo(DataCategory.OBJECTGROUP_GRAPH.name());
        verify(offsetRepository).createOrUpdateOffset(1, VitamConfiguration.getDefaultStrategy(),
            DataCategory.OBJECTGROUP_GRAPH.name(), 100L);
        assertThat(realResponseItem.getTenant()).isEqualTo(1);
        assertThat(realResponseItem.getStatus()).isEqualTo(StatusCode.KO);
    }

    @RunWithCustomExecutor
    @Test
    public void should_return_new_offset_when_item_unit_graph_is_ok() throws Exception {
        VitamConfiguration.setAdminTenant(1);
        // given
        when(offsetRepository.findOffsetBy(1, VitamConfiguration.getDefaultStrategy(),
            DataCategory.UNIT_GRAPH.name())).thenReturn(100L);

        requestItem.setCollection(DataCategory.UNIT_GRAPH.name());
        when(restoreBackupService.getListing(VitamConfiguration.getDefaultStrategy(), DataCategory.UNIT_GRAPH, 100l,
            requestItem.getLimit(), Order.ASC, VitamConfiguration.getRestoreBulkSize()))
            .thenReturn(IteratorUtils.arrayIterator(getOfferLog("1970-01-01-00-00-00-000_2018-04-20-17-00-01-444", 100),
                getOfferLog("2018-04-20-17-00-01-444_2018-05-20-17-00-01-445", 101)));
        when(restoreBackupService
            .loadData(VitamConfiguration.getDefaultStrategy(), DataCategory.UNIT_GRAPH,
                "1970-01-01-00-00-00-000_2018-04-20-17-00-01-444"))
            .thenReturn(new FakeInputStream(1, false));

        when(restoreBackupService
            .loadData(VitamConfiguration.getDefaultStrategy(), DataCategory.UNIT_GRAPH,
                "2018-04-20-17-00-01-444_2018-05-20-17-00-01-445"))
            .thenReturn(new FakeInputStream(1, true));

        when(storageClient.getStorageStrategies()).thenReturn(getStorageStrategies());

        ReconstructionService reconstructionService = new ReconstructionService(vitamRepositoryProvider,
            restoreBackupService, logbookLifecycleClientFactory, storageClientFactory, offsetRepository,
            indexManager);

        FindIterable findIterable = mock(FindIterable.class);
        final MongoCursor<String> iterator = mock(MongoCursor.class);
        final Bson projection = include(getComputedGraphObjectGroupFields());
        when(mongoRepository.findDocuments(anyList(), eq(projection))).thenReturn(findIterable);
        when(findIterable.iterator()).thenReturn(iterator);
        when(iterator.hasNext()).thenReturn(Boolean.FALSE);
        // when
        ReconstructionResponseItem realResponseItem = reconstructionService.reconstruct(requestItem);
        // then
        assertThat(realResponseItem).isNotNull();
        assertThat(realResponseItem.getCollection()).isEqualTo(DataCategory.UNIT_GRAPH.name());
        verify(offsetRepository).createOrUpdateOffset(1, VitamConfiguration.getDefaultStrategy(),
            DataCategory.UNIT_GRAPH.name(), 101L);
        assertThat(realResponseItem.getTenant()).isEqualTo(1);
        assertThat(realResponseItem.getStatus()).isEqualTo(StatusCode.OK);
    }

    @RunWithCustomExecutor
    @Test
    public void should_ignore_missing_files_and_return_new_offset_when_item_unit_graph_is_ok() throws Exception {
        VitamConfiguration.setAdminTenant(1);
        // given
        when(offsetRepository.findOffsetBy(1, VitamConfiguration.getDefaultStrategy(),
            DataCategory.UNIT_GRAPH.name())).thenReturn(100L);

        requestItem.setCollection(DataCategory.UNIT_GRAPH.name());
        when(restoreBackupService.getListing(VitamConfiguration.getDefaultStrategy(), DataCategory.UNIT_GRAPH, 100l,
            requestItem.getLimit(), Order.ASC, VitamConfiguration.getRestoreBulkSize()))
            .thenReturn(IteratorUtils.arrayIterator(getOfferLog("1970-01-01-00-00-00-000_2018-04-20-17-00-01-444", 100),
                getOfferLog("2018-04-20-17-00-01-444_2018-05-20-17-00-01-445", 101)));
        when(restoreBackupService
            .loadData(VitamConfiguration.getDefaultStrategy(), DataCategory.UNIT_GRAPH,
                "1970-01-01-00-00-00-000_2018-04-20-17-00-01-444"))
            .thenThrow(new StorageNotFoundException(""));

        when(restoreBackupService
            .loadData(VitamConfiguration.getDefaultStrategy(), DataCategory.UNIT_GRAPH,
                "2018-04-20-17-00-01-444_2018-05-20-17-00-01-445"))
            .thenReturn(new FakeInputStream(1, true));

        when(storageClient.getStorageStrategies()).thenReturn(getStorageStrategies());

        ReconstructionService reconstructionService = new ReconstructionService(vitamRepositoryProvider,
            restoreBackupService, logbookLifecycleClientFactory, storageClientFactory, offsetRepository,
            indexManager);

        FindIterable findIterable = mock(FindIterable.class);
        final MongoCursor<String> iterator = mock(MongoCursor.class);
        final Bson projection = include(getComputedGraphObjectGroupFields());
        when(mongoRepository.findDocuments(anyList(), eq(projection))).thenReturn(findIterable);
        when(findIterable.iterator()).thenReturn(iterator);
        when(iterator.hasNext()).thenReturn(Boolean.FALSE);
        // when
        ReconstructionResponseItem realResponseItem = reconstructionService.reconstruct(requestItem);
        // then
        assertThat(realResponseItem).isNotNull();
        assertThat(realResponseItem.getCollection()).isEqualTo(DataCategory.UNIT_GRAPH.name());
        verify(offsetRepository).createOrUpdateOffset(1, VitamConfiguration.getDefaultStrategy(),
            DataCategory.UNIT_GRAPH.name(), 100L);
        assertThat(realResponseItem.getTenant()).isEqualTo(1);
        assertThat(realResponseItem.getStatus()).isEqualTo(StatusCode.KO);
    }


    @RunWithCustomExecutor
    @Test
    public void should_return_request_offset_when_item_limit_zero()
        throws StorageServerClientException, InvalidParseOperationException {
        // given
        requestItem.setLimit(0);
        when(offsetRepository.findOffsetBy(10, STRATEGY_UNIT, MetadataCollections.UNIT.getName())).thenReturn(100L);

        when(restoreBackupService.getListing(STRATEGY_UNIT, DataCategory.UNIT, 100l,
            requestItem.getLimit(), Order.ASC, VitamConfiguration.getRestoreBulkSize())).
            thenReturn(IteratorUtils.emptyIterator());

        when(storageClient.getStorageStrategies()).thenReturn(getStorageStrategies());

        ReconstructionService reconstructionService = new ReconstructionService(vitamRepositoryProvider,
            restoreBackupService, logbookLifecycleClientFactory, storageClientFactory, offsetRepository,
            indexManager);
        // when
        ReconstructionResponseItem realResponseItem = reconstructionService.reconstruct(requestItem);
        // then
        assertThat(realResponseItem).isNotNull();
        assertThat(realResponseItem.getCollection()).isEqualTo(MetadataCollections.UNIT.name());
        verify(offsetRepository).createOrUpdateOffset(10, STRATEGY_UNIT, MetadataCollections.UNIT.getName(), 100L);
        assertThat(realResponseItem.getTenant()).isEqualTo(10);
        assertThat(realResponseItem.getStatus()).isEqualTo(StatusCode.OK);
    }

    @RunWithCustomExecutor
    @Test
    public void should_throw_IllegalArgumentException_when_item_is_negative() {
        // given
        requestItem.setLimit(-5);
        ReconstructionService reconstructionService = new ReconstructionService(vitamRepositoryProvider,
            restoreBackupService, logbookLifecycleClientFactory, storageClientFactory, offsetRepository,
            indexManager);
        // when + then
        assertThatCode(() -> reconstructionService.reconstruct(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @RunWithCustomExecutor
    @Test
    public void should_throw_IllegalArgumentException_when_item_is_null() {
        // given
        ReconstructionService reconstructionService = new ReconstructionService(vitamRepositoryProvider,
            restoreBackupService, logbookLifecycleClientFactory, storageClientFactory, offsetRepository,
            indexManager);
        // when + then
        assertThatCode(() -> reconstructionService.reconstruct(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @RunWithCustomExecutor
    @Test
    public void should_throw_IllegalArgumentException_when_item_collection_is_null() {
        // given
        ReconstructionService reconstructionService = new ReconstructionService(vitamRepositoryProvider,
            restoreBackupService, logbookLifecycleClientFactory, storageClientFactory, offsetRepository,
            indexManager);
        // when + then
        assertThatCode(() -> reconstructionService.reconstruct(requestItem.setCollection(null)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @RunWithCustomExecutor
    @Test
    public void should_throw_IllegalArgumentException_when_item_collection_is_invalid() {
        // given
        ReconstructionService reconstructionService = new ReconstructionService(vitamRepositoryProvider,
            restoreBackupService, logbookLifecycleClientFactory, storageClientFactory, offsetRepository,
            indexManager);
        // when + then
        assertThatCode(() -> reconstructionService.reconstruct(requestItem.setCollection("toto")))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @RunWithCustomExecutor
    @Test
    public void should_throw_IllegalArgumentException_when_item_tenant_is_null() {
        // given
        ReconstructionService reconstructionService = new ReconstructionService(vitamRepositoryProvider,
            restoreBackupService, logbookLifecycleClientFactory, storageClientFactory, offsetRepository,
            indexManager);
        // when + then
        assertThatCode(() -> reconstructionService.reconstruct(requestItem.setTenant(null)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @RunWithCustomExecutor
    @Test
    public void should_return_ko_when_mongo_exception()
        throws Exception {
        // Given
        when(offsetRepository.findOffsetBy(10, STRATEGY_UNIT, MetadataCollections.UNIT.getName())).thenReturn(100L);
        when(restoreBackupService.getListing(STRATEGY_UNIT, DataCategory.UNIT, 100l,
            requestItem.getLimit(), Order.ASC, VitamConfiguration.getRestoreBulkSize())).thenReturn(
            IteratorUtils.arrayIterator(getOfferLog(100), getOfferLog(101)));
        when(restoreBackupService.loadData(STRATEGY_UNIT, MetadataCollections.UNIT, "100", 100L))
            .thenReturn(getUnitMetadataBackupModel("100", 100L));
        when(restoreBackupService.loadData(STRATEGY_UNIT, MetadataCollections.UNIT, "101", 101L))
            .thenReturn(getUnitMetadataBackupModel("101", 101L));
        when(storageClient.getStorageStrategies()).thenReturn(getStorageStrategies());
        doNothing().when(logbookLifecycleClient).createRawbulkUnitlifecycles(any());
        // doThrow(new DatabaseException("mongo error")).when(mongoRepository).update(any(List.class));
        final int[] cpt = {0};
        Mockito.doAnswer(i -> {
            cpt[0]++;
            if (cpt[0] == 1) {

                throw new DatabaseException(new MongoBulkWriteException(null, new ArrayList<>(), null, null));
            } else {
                throw new DatabaseException("mongo error");
            }
        }).when(mongoRepository).update(any(List.class));

        ReconstructionService reconstructionService = new ReconstructionService(vitamRepositoryProvider,
            restoreBackupService, logbookLifecycleClientFactory, storageClientFactory, offsetRepository,
            indexManager);

        FindIterable<Document> findIterable = mock(FindIterable.class);
        final MongoCursor<Document> iterator = mock(MongoCursor.class);
        when(mongoRepository.findDocuments(any(), any())).thenReturn(findIterable);
        when(findIterable.iterator()).thenReturn(iterator);
        when(iterator.hasNext()).thenReturn(Boolean.FALSE);

        // When
        ReconstructionResponseItem realResponseItem = reconstructionService.reconstruct(requestItem);

        // Then
        assertThat(realResponseItem).isNotNull();
        assertThat(realResponseItem.getCollection()).isEqualTo(MetadataCollections.UNIT.name());
        verify(mongoRepository, times(2)).update(anyList());
        assertThat(realResponseItem.getTenant()).isEqualTo(10);
        assertThat(realResponseItem.getStatus()).isEqualTo(StatusCode.KO);
    }


    @RunWithCustomExecutor
    @Test
    public void should_return_ko_when_es_exception()
        throws Exception {
        // Given
        when(offsetRepository.findOffsetBy(10, STRATEGY_UNIT, MetadataCollections.UNIT.getName())).thenReturn(100L);
        when(restoreBackupService.getListing(STRATEGY_UNIT, DataCategory.UNIT, 100l, requestItem.getLimit(),
            Order.ASC, VitamConfiguration.getRestoreBulkSize()))
            .thenReturn(IteratorUtils.arrayIterator(getOfferLog(100), getOfferLog(101)));
        when(restoreBackupService.loadData(STRATEGY_UNIT, MetadataCollections.UNIT, "100", 100L))
            .thenReturn(getUnitMetadataBackupModel("100", 100L));
        when(restoreBackupService.loadData(STRATEGY_UNIT, MetadataCollections.UNIT, "101", 101L))
            .thenReturn(getUnitMetadataBackupModel("101", 101L));
        when(storageClient.getStorageStrategies()).thenReturn(getStorageStrategies());
        doNothing().when(logbookLifecycleClient).createRawbulkUnitlifecycles(any());
        doThrow(new DatabaseException("Elasticsearch error")).when(esRepository).save(any(List.class));

        ReconstructionService reconstructionService = new ReconstructionService(vitamRepositoryProvider,
            restoreBackupService, logbookLifecycleClientFactory, storageClientFactory, offsetRepository,
            indexManager);

        FindIterable findIterable = mock(FindIterable.class);
        final MongoCursor<String> iterator = mock(MongoCursor.class);
        when(mongoRepository.findDocuments(any(), any())).thenReturn(findIterable);
        when(findIterable.iterator()).thenReturn(iterator);
        when(iterator.hasNext()).thenReturn(Boolean.FALSE);
        // When
        ReconstructionResponseItem realResponseItem = reconstructionService.reconstruct(requestItem);

        // Then
        assertThat(realResponseItem).isNotNull();
        assertThat(realResponseItem.getCollection()).isEqualTo(MetadataCollections.UNIT.name());
        assertThat(realResponseItem.getTenant()).isEqualTo(10);
        assertThat(realResponseItem.getStatus()).isEqualTo(StatusCode.KO);
    }

    @RunWithCustomExecutor
    @Test
    public void should_return_ko_when_logbook_exception() throws Exception {
        // Given
        when(offsetRepository.findOffsetBy(10, STRATEGY_UNIT, MetadataCollections.UNIT.getName())).thenReturn(100L);
        when(restoreBackupService.getListing(STRATEGY_UNIT, DataCategory.UNIT, 100l,
            requestItem.getLimit(), Order.ASC, VitamConfiguration.getRestoreBulkSize()))
            .thenReturn(IteratorUtils.arrayIterator(getOfferLog(100), getOfferLog(101)));
        when(restoreBackupService.loadData(STRATEGY_UNIT, MetadataCollections.UNIT, "100", 100L))
            .thenReturn(getUnitMetadataBackupModel("100", 100L));
        when(restoreBackupService.loadData(STRATEGY_UNIT, MetadataCollections.UNIT, "101", 101L))
            .thenReturn(getUnitMetadataBackupModel("101", 101L));
        when(storageClient.getStorageStrategies()).thenReturn(getStorageStrategies());
        doThrow(new LogbookClientServerException("logbook error"))
            .when(logbookLifecycleClient)
            .createRawbulkUnitlifecycles(any());

        ReconstructionService reconstructionService = new ReconstructionService(vitamRepositoryProvider,
            restoreBackupService, logbookLifecycleClientFactory, storageClientFactory, offsetRepository,
            indexManager);

        FindIterable findIterable = mock(FindIterable.class);
        final MongoCursor<String> iterator = mock(MongoCursor.class);

        when(mongoRepository.findDocuments(any(), any())).thenReturn(findIterable);

        when(findIterable.iterator()).thenReturn(iterator);
        when(iterator.hasNext()).thenReturn(Boolean.FALSE);
        // When
        ReconstructionResponseItem realResponseItem = reconstructionService.reconstruct(requestItem);

        // Then
        assertThat(realResponseItem).isNotNull();
        assertThat(realResponseItem.getCollection()).isEqualTo(MetadataCollections.UNIT.name());
        assertThat(realResponseItem.getTenant()).isEqualTo(10);
        assertThat(realResponseItem.getStatus()).isEqualTo(StatusCode.KO);
    }

    @RunWithCustomExecutor
    @Test
    public void should_return_request_offset_when_lifecycle_null()
        throws Exception {
        // Given
        when(offsetRepository.findOffsetBy(10, STRATEGY_UNIT, MetadataCollections.UNIT.getName())).thenReturn(100L);
        MetadataBackupModel metadataBackupModel100 = getUnitMetadataBackupModel("100", 100L);
        metadataBackupModel100.setLifecycle(null);
        when(restoreBackupService.getListing(STRATEGY_UNIT, DataCategory.UNIT, 100l,
            requestItem.getLimit(), Order.ASC, VitamConfiguration.getRestoreBulkSize()))
            .thenReturn(IteratorUtils.arrayIterator(getOfferLog(100), getOfferLog(101)));
        when(restoreBackupService.loadData(STRATEGY_UNIT, MetadataCollections.UNIT, "100", 100L))
            .thenReturn(metadataBackupModel100);
        when(restoreBackupService.loadData(STRATEGY_UNIT, MetadataCollections.UNIT, "101", 101L))
            .thenReturn(getUnitMetadataBackupModel("101", 101L));
        when(storageClient.getStorageStrategies()).thenReturn(getStorageStrategies());
        doThrow(new LogbookClientServerException("logbook error"))
            .when(logbookLifecycleClient)
            .createRawbulkUnitlifecycles(any());

        ReconstructionService reconstructionService = new ReconstructionService(vitamRepositoryProvider,
            restoreBackupService, logbookLifecycleClientFactory, storageClientFactory, offsetRepository,
            indexManager);
        // When
        ReconstructionResponseItem realResponseItem = reconstructionService.reconstruct(requestItem);

        // Then
        assertThat(realResponseItem).isNotNull();
        assertThat(realResponseItem.getCollection()).isEqualTo(MetadataCollections.UNIT.name());
        assertThat(realResponseItem.getTenant()).isEqualTo(10);
        assertThat(realResponseItem.getStatus()).isEqualTo(StatusCode.KO);
    }

    @RunWithCustomExecutor
    @Test
    public void should_return_request_offset_when_metadata_null() throws Exception {
        // Given
        when(offsetRepository.findOffsetBy(10, STRATEGY_UNIT, MetadataCollections.UNIT.getName())).thenReturn(100L);
        MetadataBackupModel metadataBackupModel100 = getUnitMetadataBackupModel("100", 100L);
        metadataBackupModel100.setUnit(null);
        when(restoreBackupService.getListing(STRATEGY_UNIT, DataCategory.UNIT, 100l,
            requestItem.getLimit(), Order.ASC, VitamConfiguration.getRestoreBulkSize()))
            .thenReturn(IteratorUtils.arrayIterator(getOfferLog(100), getOfferLog(101)));
        when(restoreBackupService.loadData(STRATEGY_UNIT, MetadataCollections.UNIT, "100", 100L))
            .thenReturn(metadataBackupModel100);
        when(restoreBackupService.loadData(STRATEGY_UNIT, MetadataCollections.UNIT, "101", 101L))
            .thenReturn(getUnitMetadataBackupModel("101", 101L));
        when(storageClient.getStorageStrategies()).thenReturn(getStorageStrategies());

        ReconstructionService reconstructionService = new ReconstructionService(vitamRepositoryProvider,
            restoreBackupService, logbookLifecycleClientFactory, storageClientFactory, offsetRepository,
            indexManager);
        // When
        ReconstructionResponseItem realResponseItem = reconstructionService.reconstruct(requestItem);
        // Then
        assertThat(realResponseItem).isNotNull();
        assertThat(realResponseItem.getCollection()).isEqualTo(MetadataCollections.UNIT.name());
        assertThat(realResponseItem.getTenant()).isEqualTo(10);
        assertThat(realResponseItem.getStatus()).isEqualTo(StatusCode.KO);
    }

    @RunWithCustomExecutor
    @Test
    public void should_return_new_offset_when_loading_missing_data()
        throws Exception {
        // given
        when(offsetRepository.findOffsetBy(10, STRATEGY_UNIT, MetadataCollections.UNIT.getName())).thenReturn(100L);
        when(restoreBackupService.getListing(STRATEGY_UNIT, DataCategory.UNIT, 100l,
            requestItem.getLimit(), Order.ASC, VitamConfiguration.getRestoreBulkSize())).thenReturn(
            IteratorUtils.singletonIterator(getOfferLog(100)));
        when(restoreBackupService.loadData(STRATEGY_UNIT, MetadataCollections.UNIT, "100", 100L))
            .thenThrow(new StorageNotFoundException(""));
        when(storageClient.getStorageStrategies()).thenReturn(getStorageStrategies());
        doNothing().when(logbookLifecycleClient).createRawbulkUnitlifecycles(any());

        FindIterable findIterable = mock(FindIterable.class);
        final MongoCursor<String> iterator = mock(MongoCursor.class);
        when(mongoRepository.findDocuments(any(), any())).thenReturn(findIterable);
        when(findIterable.iterator()).thenReturn(iterator);
        when(iterator.hasNext()).thenReturn(Boolean.FALSE);

        ReconstructionService reconstructionService = new ReconstructionService(vitamRepositoryProvider,
            restoreBackupService, logbookLifecycleClientFactory, storageClientFactory, offsetRepository,
            indexManager);

        // when
        ReconstructionResponseItem realResponseItem = reconstructionService.reconstruct(requestItem);
        // then
        assertThat(realResponseItem).isNotNull();
        assertThat(realResponseItem.getCollection()).isEqualTo(MetadataCollections.UNIT.name());
        verify(offsetRepository).createOrUpdateOffset(10, STRATEGY_UNIT, MetadataCollections.UNIT.getName(), 100L);
        assertThat(realResponseItem.getTenant()).isEqualTo(10);
        assertThat(realResponseItem.getStatus()).isEqualTo(StatusCode.OK);
    }

    private MetadataBackupModel getUnitMetadataBackupModel(String id, Long offset) {
        MetadataBackupModel model = new MetadataBackupModel();
        model.setUnit(new Document("_id", id).append("_v", 0));
        model.setLifecycle(new Document("_id", id));
        model.setOffset(offset);
        return model;
    }

    private OfferLog getOfferLog(long sequence) {
        return getOfferLog(null, sequence);
    }

    private OfferLog getOfferLog(String fileName, long sequence) {
        if (null == fileName) {
            fileName = "" + sequence;
        }
        OfferLog offerLog = new OfferLog("container", fileName, OfferLogAction.WRITE);
        offerLog.setSequence(sequence);
        return offerLog;
    }

    private MetadataBackupModel getGotMetadataBackupModel(String id, Long offset) {
        MetadataBackupModel model = new MetadataBackupModel();
        model.setGot(new Document("_id", id).append("_v", 0));
        model.setLifecycle(new Document("_id", id));
        model.setOffset(offset);
        return model;
    }

    private RequestResponse<StorageStrategy> getStorageStrategies() throws InvalidParseOperationException {
        OfferReference offerReference = new OfferReference();
        offerReference.setId("offer1");
        offerReference.setReferent(true);
        StorageStrategy strategy = new StorageStrategy();
        strategy.setId(STRATEGY_UNIT);
        strategy.getOffers().add(offerReference);

        return ClientMockResultHelper
            .createResponse(Arrays.asList(strategy));
    }

    private RequestResponse<StorageStrategy> getStorageStrategiesWithSameReferentOfferForDifferentStrategies()
        throws InvalidParseOperationException {
        OfferReference offerReference = new OfferReference();
        offerReference.setId("offer1");
        offerReference.setReferent(true);
        StorageStrategy strategy1 = new StorageStrategy();
        strategy1.setId(STRATEGY_UNIT);
        strategy1.getOffers().add(offerReference);
        StorageStrategy strategy2 = new StorageStrategy();
        strategy2.setId(STRATEGY_UNIT + "-bis");
        strategy2.getOffers().add(offerReference);

        return ClientMockResultHelper
            .createResponse(Arrays.asList(strategy1, strategy2));
    }

}
