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
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.WriteModel;
import fr.gouv.vitam.common.LocalDateUtil;
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
import fr.gouv.vitam.common.time.LogicalClockRule;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.metadata.api.model.ReconstructionRequestItem;
import fr.gouv.vitam.metadata.api.model.ReconstructionResponseItem;
import fr.gouv.vitam.metadata.core.config.ElasticsearchMetadataIndexManager;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollectionsTestUtils;
import fr.gouv.vitam.metadata.core.metrics.MetadataReconstructionMetricsCache;
import fr.gouv.vitam.metadata.core.utils.MappingLoaderTestUtils;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
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
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.mongodb.client.model.Projections.include;
import static fr.gouv.vitam.common.database.utils.MetadataDocumentHelper.getComputedGraphObjectGroupFields;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * ReconstructionService tests.
 */
public class ReconstructionServiceTest {

    public static final String STRATEGY_UNIT = "strategy-md-t10";
    private static final String DEFAULT_OFFER = "default";
    private static final String OFFER_1 = "offer1";

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Rule
    public LogicalClockRule logicalClock = new LogicalClockRule();

    private VitamRepositoryProvider vitamRepositoryProvider;
    private VitamMongoRepository mongoRepository;
    private VitamElasticsearchRepository esRepository;
    private RestoreBackupService restoreBackupService;
    private LogbookLifeCyclesClientFactory logbookLifecycleClientFactory;
    private LogbookLifeCyclesClient logbookLifecycleClient;
    private StorageClientFactory storageClientFactory;
    private StorageClient storageClient;
    private MetadataReconstructionMetricsCache reconstructionMetricsCache;

    private ReconstructionRequestItem requestItem;

    private OffsetRepository offsetRepository;
    private final ElasticsearchMetadataIndexManager indexManager = MetadataCollectionsTestUtils.createTestIndexManager(
        Arrays.asList(1, 10),
        Collections.emptyMap(),
        MappingLoaderTestUtils.getTestMappingLoader()
    );

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

        reconstructionMetricsCache = mock(MetadataReconstructionMetricsCache.class);
    }

    @RunWithCustomExecutor
    @Test
    public void should_return_new_offset_when_item_unit_is_ok() throws Exception {
        // given
        when(offsetRepository.findOffsetBy(10, STRATEGY_UNIT, MetadataCollections.UNIT.getName())).thenReturn(99L);
        when(
            restoreBackupService.getListing(
                STRATEGY_UNIT,
                DEFAULT_OFFER,
                DataCategory.UNIT,
                100L,
                requestItem.getLimit(),
                Order.ASC,
                VitamConfiguration.getBatchSize()
            )
        ).thenReturn(IteratorUtils.arrayIterator(getOfferLog(100L), getOfferLog(101L)));
        when(
            restoreBackupService.loadData(STRATEGY_UNIT, DEFAULT_OFFER, MetadataCollections.UNIT, "100", 100L)
        ).thenReturn(getUnitMetadataBackupModel("100", 100L));
        when(
            restoreBackupService.loadData(STRATEGY_UNIT, DEFAULT_OFFER, MetadataCollections.UNIT, "101", 101L)
        ).thenReturn(getUnitMetadataBackupModel("101", 101L));
        when(storageClient.getStorageStrategies()).thenReturn(getStorageStrategies());
        ArgumentCaptor<List<JsonNode>> unitLfcsCaptor = ArgumentCaptor.forClass(List.class);
        doNothing().when(logbookLifecycleClient).createRawbulkUnitlifecycles(unitLfcsCaptor.capture());
        when(storageClient.getReferentOffer(STRATEGY_UNIT)).thenReturn(DEFAULT_OFFER);

        ReconstructionService reconstructionService = new ReconstructionService(
            vitamRepositoryProvider,
            restoreBackupService,
            logbookLifecycleClientFactory,
            storageClientFactory,
            offsetRepository,
            indexManager,
            reconstructionMetricsCache
        );

        FindIterable findIterable = mock(FindIterable.class);
        final MongoCursor<String> iterator = mock(MongoCursor.class);
        when(mongoRepository.findDocuments(any(), any())).thenReturn(findIterable);
        when(findIterable.iterator()).thenReturn(iterator);
        when(iterator.hasNext()).thenReturn(Boolean.FALSE);

        logicalClock.freezeTime();
        LocalDateTime reconstructionDateTime = LocalDateUtil.now();
        // when
        ReconstructionResponseItem realResponseItem = reconstructionService.reconstruct(requestItem);
        // then
        assertThat(realResponseItem).isNotNull();
        assertThat(realResponseItem.getCollection()).isEqualTo(MetadataCollections.UNIT.name());
        verify(offsetRepository).createOrUpdateOffset(10, STRATEGY_UNIT, MetadataCollections.UNIT.getName(), 101L);
        assertThat(realResponseItem.getTenant()).isEqualTo(10);
        assertThat(realResponseItem.getStatus()).isEqualTo(StatusCode.OK);
        verify(reconstructionMetricsCache).registerLastDocumentReconstructionDate(
            MetadataCollections.UNIT,
            10,
            STRATEGY_UNIT,
            reconstructionDateTime
        );
        verifyNoMoreInteractions(reconstructionMetricsCache);
    }

    @RunWithCustomExecutor
    @Test
    public void should_return_new_offset_when_too_many_units_ok() throws Exception {
        // given
        when(offsetRepository.findOffsetBy(10, STRATEGY_UNIT, MetadataCollections.UNIT.getName())).thenReturn(99L);
        logicalClock.freezeTime();
        List<OfferLog> offerLogs = IntStream.rangeClosed(100, 199)
            .mapToObj(sequence -> {
                logicalClock.logicalSleep(10, ChronoUnit.MINUTES);
                return getOfferLog(sequence);
            })
            .collect(Collectors.toList());
        requestItem.setLimit(100);
        when(
            restoreBackupService.getListing(
                STRATEGY_UNIT,
                DEFAULT_OFFER,
                DataCategory.UNIT,
                100L,
                requestItem.getLimit(),
                Order.ASC,
                VitamConfiguration.getBatchSize()
            )
        ).thenReturn(offerLogs.iterator());
        for (int i = 100; i < 200; i++) {
            when(
                restoreBackupService.loadData(
                    STRATEGY_UNIT,
                    DEFAULT_OFFER,
                    MetadataCollections.UNIT,
                    String.valueOf(i),
                    i
                )
            ).thenReturn(getUnitMetadataBackupModel(String.valueOf(i), (long) i));
        }
        when(storageClient.getStorageStrategies()).thenReturn(getStorageStrategies());
        ArgumentCaptor<List<JsonNode>> unitLfcsCaptor = ArgumentCaptor.forClass(List.class);
        doNothing().when(logbookLifecycleClient).createRawbulkUnitlifecycles(unitLfcsCaptor.capture());
        when(storageClient.getReferentOffer(STRATEGY_UNIT)).thenReturn(DEFAULT_OFFER);

        ReconstructionService reconstructionService = new ReconstructionService(
            vitamRepositoryProvider,
            restoreBackupService,
            logbookLifecycleClientFactory,
            storageClientFactory,
            offsetRepository,
            indexManager,
            reconstructionMetricsCache
        );

        FindIterable findIterable = mock(FindIterable.class);
        final MongoCursor<String> iterator = mock(MongoCursor.class);
        when(mongoRepository.findDocuments(any(), any())).thenReturn(findIterable);
        when(findIterable.iterator()).thenReturn(iterator);
        when(iterator.hasNext()).thenReturn(Boolean.FALSE);

        logicalClock.logicalSleep(10, ChronoUnit.MINUTES);
        // when
        ReconstructionResponseItem realResponseItem = reconstructionService.reconstruct(requestItem);
        // then
        assertThat(realResponseItem).isNotNull();
        assertThat(realResponseItem.getCollection()).isEqualTo(MetadataCollections.UNIT.name());
        verify(offsetRepository).createOrUpdateOffset(10, STRATEGY_UNIT, MetadataCollections.UNIT.getName(), 199L);
        assertThat(realResponseItem.getTenant()).isEqualTo(10);
        assertThat(realResponseItem.getStatus()).isEqualTo(StatusCode.OK);
        verify(reconstructionMetricsCache).registerLastDocumentReconstructionDate(
            MetadataCollections.UNIT,
            10,
            STRATEGY_UNIT,
            offerLogs.get(99).getTime()
        );
        verifyNoMoreInteractions(reconstructionMetricsCache);
    }

    @RunWithCustomExecutor
    @Test
    public void should_return_new_offsets_when_item_unit_for_two_strategies_are_ok() throws Exception {
        // given
        when(
            offsetRepository.findOffsetBy(eq(10), contains(STRATEGY_UNIT), eq(MetadataCollections.UNIT.getName()))
        ).thenReturn(99L);
        when(
            restoreBackupService.getListing(
                contains(STRATEGY_UNIT),
                eq(DEFAULT_OFFER),
                eq(DataCategory.UNIT),
                eq(100L),
                eq(requestItem.getLimit()),
                eq(Order.ASC),
                eq(VitamConfiguration.getBatchSize())
            )
        ).thenReturn(IteratorUtils.arrayIterator(getOfferLog(100L), getOfferLog(101L)));
        when(
            restoreBackupService.loadData(
                contains(STRATEGY_UNIT),
                eq(DEFAULT_OFFER),
                eq(MetadataCollections.UNIT),
                eq("100"),
                eq(100L)
            )
        ).thenReturn(getUnitMetadataBackupModel("100", 100L));
        when(
            restoreBackupService.loadData(
                contains(STRATEGY_UNIT),
                eq(DEFAULT_OFFER),
                eq(MetadataCollections.UNIT),
                eq("101"),
                eq(101L)
            )
        ).thenReturn(getUnitMetadataBackupModel("101", 101L));
        when(storageClient.getStorageStrategies()).thenReturn(
            getStorageStrategiesWithSameReferentOfferForDifferentStrategies()
        );
        ArgumentCaptor<List<JsonNode>> unitLfcsCaptor = ArgumentCaptor.forClass(List.class);
        doNothing().when(logbookLifecycleClient).createRawbulkUnitlifecycles(unitLfcsCaptor.capture());
        when(storageClient.getReferentOffer(any())).thenReturn(DEFAULT_OFFER);

        ReconstructionService reconstructionService = new ReconstructionService(
            vitamRepositoryProvider,
            restoreBackupService,
            logbookLifecycleClientFactory,
            storageClientFactory,
            offsetRepository,
            indexManager,
            reconstructionMetricsCache
        );

        FindIterable findIterable = mock(FindIterable.class);
        final MongoCursor<String> iterator = mock(MongoCursor.class);
        when(mongoRepository.findDocuments(any(), any())).thenReturn(findIterable);
        when(findIterable.iterator()).thenReturn(iterator);
        when(iterator.hasNext()).thenReturn(Boolean.FALSE);

        logicalClock.freezeTime();
        LocalDateTime reconstructionDateTime = LocalDateUtil.now();
        // when
        ReconstructionResponseItem realResponseItem = reconstructionService.reconstruct(requestItem);
        // then
        assertThat(realResponseItem).isNotNull();
        assertThat(realResponseItem.getCollection()).isEqualTo(MetadataCollections.UNIT.name());
        verify(offsetRepository).createOrUpdateOffset(
            eq(10),
            startsWith(STRATEGY_UNIT),
            eq(MetadataCollections.UNIT.getName()),
            eq(101L)
        );
        assertThat(realResponseItem.getTenant()).isEqualTo(10);
        assertThat(realResponseItem.getStatus()).isEqualTo(StatusCode.OK);
        verify(reconstructionMetricsCache).registerLastDocumentReconstructionDate(
            MetadataCollections.UNIT,
            10,
            STRATEGY_UNIT,
            reconstructionDateTime
        );
        verify(reconstructionMetricsCache).registerLastDocumentReconstructionDate(
            MetadataCollections.UNIT,
            10,
            STRATEGY_UNIT + "-bis",
            reconstructionDateTime
        );
        verifyNoMoreInteractions(reconstructionMetricsCache);
    }

    @RunWithCustomExecutor
    @Test
    public void should_ignore_missing_files_and_return_new_offset_when_item_unit_is_ok() throws Exception {
        // given
        when(offsetRepository.findOffsetBy(10, STRATEGY_UNIT, MetadataCollections.UNIT.getName())).thenReturn(99L);
        when(
            restoreBackupService.getListing(
                STRATEGY_UNIT,
                DEFAULT_OFFER,
                DataCategory.UNIT,
                100L,
                requestItem.getLimit(),
                Order.ASC,
                VitamConfiguration.getBatchSize()
            )
        ).thenReturn(IteratorUtils.arrayIterator(getOfferLog(100L), getOfferLog(101L)));
        when(
            restoreBackupService.loadData(STRATEGY_UNIT, DEFAULT_OFFER, MetadataCollections.UNIT, "100", 100L)
        ).thenReturn(getUnitMetadataBackupModel("100", 100L));
        when(
            restoreBackupService.loadData(STRATEGY_UNIT, DEFAULT_OFFER, MetadataCollections.UNIT, "101", 101L)
        ).thenThrow(new StorageNotFoundException(""));
        when(storageClient.getStorageStrategies()).thenReturn(getStorageStrategies());
        ArgumentCaptor<List<JsonNode>> unitLfcsCaptor = ArgumentCaptor.forClass(List.class);
        doNothing().when(logbookLifecycleClient).createRawbulkUnitlifecycles(unitLfcsCaptor.capture());
        when(storageClient.getReferentOffer(STRATEGY_UNIT)).thenReturn(DEFAULT_OFFER);

        ReconstructionService reconstructionService = new ReconstructionService(
            vitamRepositoryProvider,
            restoreBackupService,
            logbookLifecycleClientFactory,
            storageClientFactory,
            offsetRepository,
            indexManager,
            reconstructionMetricsCache
        );

        FindIterable findIterable = mock(FindIterable.class);
        final MongoCursor<String> iterator = mock(MongoCursor.class);
        when(mongoRepository.findDocuments(any(), any())).thenReturn(findIterable);
        when(findIterable.iterator()).thenReturn(iterator);
        when(iterator.hasNext()).thenReturn(Boolean.FALSE);

        logicalClock.freezeTime();
        LocalDateTime reconstructionDateTime = LocalDateUtil.now();
        // when
        ReconstructionResponseItem realResponseItem = reconstructionService.reconstruct(requestItem);
        // then
        assertThat(realResponseItem).isNotNull();
        assertThat(realResponseItem.getCollection()).isEqualTo(MetadataCollections.UNIT.name());
        verify(offsetRepository).createOrUpdateOffset(10, STRATEGY_UNIT, MetadataCollections.UNIT.getName(), 101L);
        assertThat(realResponseItem.getTenant()).isEqualTo(10);
        assertThat(realResponseItem.getStatus()).isEqualTo(StatusCode.OK);
        verify(reconstructionMetricsCache).registerLastDocumentReconstructionDate(
            MetadataCollections.UNIT,
            10,
            STRATEGY_UNIT,
            reconstructionDateTime
        );
        verifyNoMoreInteractions(reconstructionMetricsCache);
    }

    @RunWithCustomExecutor
    @Test
    public void should_return_new_offset_when_item_got_is_ok() throws Exception {
        // given
        when(offsetRepository.findOffsetBy(10, STRATEGY_UNIT, MetadataCollections.OBJECTGROUP.getName())).thenReturn(
            99L
        );

        requestItem.setCollection("ObjectGroup");
        when(
            restoreBackupService.getListing(
                STRATEGY_UNIT,
                DEFAULT_OFFER,
                DataCategory.OBJECTGROUP,
                100L,
                requestItem.getLimit(),
                Order.ASC,
                VitamConfiguration.getBatchSize()
            )
        ).thenReturn(IteratorUtils.arrayIterator(getOfferLog(100), getOfferLog(101)));
        when(
            restoreBackupService.loadData(STRATEGY_UNIT, DEFAULT_OFFER, MetadataCollections.OBJECTGROUP, "100", 100L)
        ).thenReturn(getGotMetadataBackupModel("100", 100L));
        when(
            restoreBackupService.loadData(STRATEGY_UNIT, DEFAULT_OFFER, MetadataCollections.OBJECTGROUP, "101", 101L)
        ).thenReturn(getGotMetadataBackupModel("101", 101L));
        when(storageClient.getStorageStrategies()).thenReturn(getStorageStrategies());
        doNothing().when(logbookLifecycleClient).createRawbulkObjectgrouplifecycles(any());
        when(storageClient.getReferentOffer(STRATEGY_UNIT)).thenReturn(DEFAULT_OFFER);

        ReconstructionService reconstructionService = new ReconstructionService(
            vitamRepositoryProvider,
            restoreBackupService,
            logbookLifecycleClientFactory,
            storageClientFactory,
            offsetRepository,
            indexManager,
            reconstructionMetricsCache
        );

        FindIterable findIterable = mock(FindIterable.class);
        final MongoCursor<String> iterator = mock(MongoCursor.class);
        when(mongoRepository.findDocuments(any(), any())).thenReturn(findIterable);
        when(findIterable.iterator()).thenReturn(iterator);
        when(iterator.hasNext()).thenReturn(Boolean.FALSE);

        logicalClock.freezeTime();
        LocalDateTime reconstructionDateTime = LocalDateUtil.now();
        // when
        ReconstructionResponseItem realResponseItem = reconstructionService.reconstruct(requestItem);
        // then
        assertThat(realResponseItem).isNotNull();
        assertThat(realResponseItem.getCollection()).isEqualTo(MetadataCollections.OBJECTGROUP.name());
        verify(offsetRepository).createOrUpdateOffset(
            10,
            STRATEGY_UNIT,
            MetadataCollections.OBJECTGROUP.getName(),
            101L
        );
        assertThat(realResponseItem.getTenant()).isEqualTo(10);
        assertThat(realResponseItem.getStatus()).isEqualTo(StatusCode.OK);
        verify(reconstructionMetricsCache).registerLastDocumentReconstructionDate(
            MetadataCollections.OBJECTGROUP,
            10,
            STRATEGY_UNIT,
            reconstructionDateTime
        );
        verifyNoMoreInteractions(reconstructionMetricsCache);
    }

    @RunWithCustomExecutor
    @Test
    public void should_fix_date_time_format_for_aud_and_acd_fields_when_reconstructing_truncated_lfc_evDateTime()
        throws Exception {
        // given
        when(offsetRepository.findOffsetBy(10, STRATEGY_UNIT, MetadataCollections.OBJECTGROUP.getName())).thenReturn(
            99L
        );

        requestItem.setCollection("ObjectGroup");
        when(
            restoreBackupService.getListing(
                STRATEGY_UNIT,
                DEFAULT_OFFER,
                DataCategory.OBJECTGROUP,
                100L,
                requestItem.getLimit(),
                Order.ASC,
                VitamConfiguration.getBatchSize()
            )
        ).thenReturn(IteratorUtils.singletonIterator(getOfferLog(100)));
        when(
            restoreBackupService.loadData(STRATEGY_UNIT, DEFAULT_OFFER, MetadataCollections.OBJECTGROUP, "100", 100L)
        ).thenReturn(getGotMetadataBackupModelWithTruncatedDateTime("100", 100L));
        when(storageClient.getStorageStrategies()).thenReturn(getStorageStrategies());
        doNothing().when(logbookLifecycleClient).createRawbulkObjectgrouplifecycles(any());
        when(storageClient.getReferentOffer(STRATEGY_UNIT)).thenReturn(DEFAULT_OFFER);

        ReconstructionService reconstructionService = new ReconstructionService(
            vitamRepositoryProvider,
            restoreBackupService,
            logbookLifecycleClientFactory,
            storageClientFactory,
            offsetRepository,
            indexManager,
            reconstructionMetricsCache
        );

        FindIterable findIterable = mock(FindIterable.class);
        final MongoCursor<String> iterator = mock(MongoCursor.class);
        when(mongoRepository.findDocuments(any(), any())).thenReturn(findIterable);
        when(findIterable.iterator()).thenReturn(iterator);
        when(iterator.hasNext()).thenReturn(Boolean.FALSE);

        logicalClock.freezeTime();
        LocalDateTime reconstructionDateTime = LocalDateUtil.now();
        // when
        ReconstructionResponseItem realResponseItem = reconstructionService.reconstruct(requestItem);
        // then
        assertThat(realResponseItem).isNotNull();
        assertThat(realResponseItem.getCollection()).isEqualTo(MetadataCollections.OBJECTGROUP.name());
        verify(offsetRepository).createOrUpdateOffset(
            10,
            STRATEGY_UNIT,
            MetadataCollections.OBJECTGROUP.getName(),
            100L
        );
        assertThat(realResponseItem.getTenant()).isEqualTo(10);
        assertThat(realResponseItem.getStatus()).isEqualTo(StatusCode.OK);
        ArgumentCaptor<List<WriteModel<Document>>> documentArgCaptor = ArgumentCaptor.forClass(List.class);
        verify(mongoRepository).update(documentArgCaptor.capture());
        assertThat(documentArgCaptor.getValue()).hasSize(1);
        assertThat(documentArgCaptor.getValue().get(0)).isInstanceOf(ReplaceOneModel.class);
        ReplaceOneModel<Document> replaceOneModel = (ReplaceOneModel<Document>) documentArgCaptor.getValue().get(0);
        assertThat(replaceOneModel.getReplacement().getString("_id")).isEqualTo("100");
        assertThat(replaceOneModel.getReplacement().getString("_acd")).isEqualTo("2000-05-01T11:22:00.000");
        assertThat(replaceOneModel.getReplacement().getString("_aud")).isEqualTo("2000-01-01T00:22:33.000");
    }

    @RunWithCustomExecutor
    @Test
    public void should_ignore_missing_files_and_return_new_offset_when_item_got_is_ok() throws Exception {
        // given
        when(offsetRepository.findOffsetBy(10, STRATEGY_UNIT, MetadataCollections.OBJECTGROUP.getName())).thenReturn(
            99L
        );

        requestItem.setCollection("ObjectGroup");
        when(
            restoreBackupService.getListing(
                STRATEGY_UNIT,
                DEFAULT_OFFER,
                DataCategory.OBJECTGROUP,
                100L,
                requestItem.getLimit(),
                Order.ASC,
                VitamConfiguration.getBatchSize()
            )
        ).thenReturn(IteratorUtils.arrayIterator(getOfferLog(100), getOfferLog(101)));
        when(
            restoreBackupService.loadData(STRATEGY_UNIT, DEFAULT_OFFER, MetadataCollections.OBJECTGROUP, "100", 100L)
        ).thenReturn(getGotMetadataBackupModel("100", 100L));
        when(
            restoreBackupService.loadData(STRATEGY_UNIT, DEFAULT_OFFER, MetadataCollections.OBJECTGROUP, "101", 101L)
        ).thenThrow(new StorageNotFoundException(""));
        when(storageClient.getStorageStrategies()).thenReturn(getStorageStrategies());
        doNothing().when(logbookLifecycleClient).createRawbulkObjectgrouplifecycles(any());
        when(storageClient.getReferentOffer(STRATEGY_UNIT)).thenReturn(DEFAULT_OFFER);

        ReconstructionService reconstructionService = new ReconstructionService(
            vitamRepositoryProvider,
            restoreBackupService,
            logbookLifecycleClientFactory,
            storageClientFactory,
            offsetRepository,
            indexManager,
            reconstructionMetricsCache
        );

        FindIterable findIterable = mock(FindIterable.class);
        final MongoCursor<String> iterator = mock(MongoCursor.class);
        when(mongoRepository.findDocuments(any(), any())).thenReturn(findIterable);
        when(findIterable.iterator()).thenReturn(iterator);
        when(iterator.hasNext()).thenReturn(Boolean.FALSE);

        logicalClock.freezeTime();
        LocalDateTime reconstructionDateTime = LocalDateUtil.now();
        // when
        ReconstructionResponseItem realResponseItem = reconstructionService.reconstruct(requestItem);
        // then
        assertThat(realResponseItem).isNotNull();
        assertThat(realResponseItem.getCollection()).isEqualTo(MetadataCollections.OBJECTGROUP.name());
        verify(offsetRepository).createOrUpdateOffset(
            10,
            STRATEGY_UNIT,
            MetadataCollections.OBJECTGROUP.getName(),
            101L
        );
        assertThat(realResponseItem.getTenant()).isEqualTo(10);
        assertThat(realResponseItem.getStatus()).isEqualTo(StatusCode.OK);
        verify(reconstructionMetricsCache).registerLastDocumentReconstructionDate(
            MetadataCollections.OBJECTGROUP,
            10,
            STRATEGY_UNIT,
            reconstructionDateTime
        );
        verifyNoMoreInteractions(reconstructionMetricsCache);
    }

    @RunWithCustomExecutor
    @Test
    public void should_return_new_offset_when_item_got_graph_is_ok() throws Exception {
        VitamConfiguration.setAdminTenant(1);
        // given
        when(
            offsetRepository.findOffsetBy(
                1,
                VitamConfiguration.getDefaultStrategy(),
                DataCategory.OBJECTGROUP_GRAPH.name()
            )
        ).thenReturn(99L);

        requestItem.setCollection(DataCategory.OBJECTGROUP_GRAPH.name());
        when(
            restoreBackupService.getListing(
                VitamConfiguration.getDefaultStrategy(),
                DEFAULT_OFFER,
                DataCategory.OBJECTGROUP_GRAPH,
                100L,
                requestItem.getLimit(),
                Order.ASC,
                VitamConfiguration.getBatchSize()
            )
        ).thenReturn(
            IteratorUtils.arrayIterator(
                getOfferLog("1970-01-01-00-00-00-000_2018-04-20-17-00-01-444", 100),
                getOfferLog("2018-04-20-17-00-01-444_2018-05-20-17-00-01-445", 101)
            )
        );
        when(
            restoreBackupService.loadData(
                VitamConfiguration.getDefaultStrategy(),
                DEFAULT_OFFER,
                DataCategory.OBJECTGROUP_GRAPH,
                "1970-01-01-00-00-00-000_2018-04-20-17-00-01-444"
            )
        ).thenReturn(new FakeInputStream(1, false));

        when(
            restoreBackupService.loadData(
                VitamConfiguration.getDefaultStrategy(),
                DEFAULT_OFFER,
                DataCategory.OBJECTGROUP_GRAPH,
                "2018-04-20-17-00-01-444_2018-05-20-17-00-01-445"
            )
        ).thenReturn(new FakeInputStream(1, true));

        when(storageClient.getStorageStrategies()).thenReturn(getStorageStrategies());
        when(storageClient.getReferentOffer(VitamConfiguration.getDefaultStrategy())).thenReturn(DEFAULT_OFFER);

        ReconstructionService reconstructionService = new ReconstructionService(
            vitamRepositoryProvider,
            restoreBackupService,
            logbookLifecycleClientFactory,
            storageClientFactory,
            offsetRepository,
            indexManager,
            reconstructionMetricsCache
        );

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
        verify(offsetRepository).createOrUpdateOffset(
            1,
            VitamConfiguration.getDefaultStrategy(),
            DataCategory.OBJECTGROUP_GRAPH.name(),
            101L
        );
        assertThat(realResponseItem.getTenant()).isEqualTo(1);
        assertThat(realResponseItem.getStatus()).isEqualTo(StatusCode.OK);
        InOrder inOrder = inOrder(reconstructionMetricsCache);
        inOrder
            .verify(reconstructionMetricsCache)
            .registerLastGraphReconstructionDate(
                MetadataCollections.OBJECTGROUP,
                LocalDateTime.of(1970, 1, 1, 0, 0, 0, 0)
            );
        inOrder
            .verify(reconstructionMetricsCache, times(2))
            .registerLastGraphReconstructionDate(
                MetadataCollections.OBJECTGROUP,
                LocalDateTime.of(2018, 4, 20, 17, 0, 1, 444000000)
            );
        inOrder
            .verify(reconstructionMetricsCache)
            .registerLastGraphReconstructionDate(
                MetadataCollections.OBJECTGROUP,
                LocalDateTime.of(2018, 5, 20, 17, 0, 1, 445000000)
            );
        inOrder.verifyNoMoreInteractions();
    }

    @RunWithCustomExecutor
    @Test
    public void should_ignore_missing_files_and_return_new_offset_when_item_got_graph_is_ok() throws Exception {
        VitamConfiguration.setAdminTenant(1);
        // given
        when(
            offsetRepository.findOffsetBy(
                1,
                VitamConfiguration.getDefaultStrategy(),
                DataCategory.OBJECTGROUP_GRAPH.name()
            )
        ).thenReturn(99L);

        requestItem.setCollection(DataCategory.OBJECTGROUP_GRAPH.name());
        when(
            restoreBackupService.getListing(
                VitamConfiguration.getDefaultStrategy(),
                DEFAULT_OFFER,
                DataCategory.OBJECTGROUP_GRAPH,
                100L,
                requestItem.getLimit(),
                Order.ASC,
                VitamConfiguration.getBatchSize()
            )
        ).thenReturn(
            IteratorUtils.arrayIterator(
                getOfferLog("1970-01-01-00-00-00-000_2018-04-20-17-00-01-444", 100),
                getOfferLog("2018-04-20-17-00-01-444_2018-05-20-17-00-01-445", 101)
            )
        );
        when(
            restoreBackupService.loadData(
                VitamConfiguration.getDefaultStrategy(),
                DEFAULT_OFFER,
                DataCategory.OBJECTGROUP_GRAPH,
                "1970-01-01-00-00-00-000_2018-04-20-17-00-01-444"
            )
        ).thenReturn(new FakeInputStream(1, false));

        when(
            restoreBackupService.loadData(
                VitamConfiguration.getDefaultStrategy(),
                DEFAULT_OFFER,
                DataCategory.OBJECTGROUP_GRAPH,
                "2018-04-20-17-00-01-444_2018-05-20-17-00-01-445"
            )
        ).thenThrow(new StorageNotFoundException(""));

        when(storageClient.getStorageStrategies()).thenReturn(getStorageStrategies());
        when(storageClient.getReferentOffer(VitamConfiguration.getDefaultStrategy())).thenReturn(DEFAULT_OFFER);

        ReconstructionService reconstructionService = new ReconstructionService(
            vitamRepositoryProvider,
            restoreBackupService,
            logbookLifecycleClientFactory,
            storageClientFactory,
            offsetRepository,
            indexManager,
            reconstructionMetricsCache
        );

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
        verify(offsetRepository).createOrUpdateOffset(
            1,
            VitamConfiguration.getDefaultStrategy(),
            DataCategory.OBJECTGROUP_GRAPH.name(),
            100L
        );
        assertThat(realResponseItem.getTenant()).isEqualTo(1);
        assertThat(realResponseItem.getStatus()).isEqualTo(StatusCode.KO);
        InOrder inOrder = inOrder(reconstructionMetricsCache);
        inOrder
            .verify(reconstructionMetricsCache)
            .registerLastGraphReconstructionDate(
                MetadataCollections.OBJECTGROUP,
                LocalDateTime.of(1970, 1, 1, 0, 0, 0, 0)
            );
        inOrder
            .verify(reconstructionMetricsCache, times(2))
            .registerLastGraphReconstructionDate(
                MetadataCollections.OBJECTGROUP,
                LocalDateTime.of(2018, 4, 20, 17, 0, 1, 444000000)
            );
        inOrder.verifyNoMoreInteractions();
    }

    @RunWithCustomExecutor
    @Test
    public void should_return_new_offset_when_item_unit_graph_is_ok() throws Exception {
        VitamConfiguration.setAdminTenant(1);
        // given
        when(
            offsetRepository.findOffsetBy(1, VitamConfiguration.getDefaultStrategy(), DataCategory.UNIT_GRAPH.name())
        ).thenReturn(99L);

        requestItem.setCollection(DataCategory.UNIT_GRAPH.name());
        when(
            restoreBackupService.getListing(
                VitamConfiguration.getDefaultStrategy(),
                DEFAULT_OFFER,
                DataCategory.UNIT_GRAPH,
                100L,
                requestItem.getLimit(),
                Order.ASC,
                VitamConfiguration.getBatchSize()
            )
        ).thenReturn(
            IteratorUtils.arrayIterator(
                getOfferLog("1970-01-01-00-00-00-000_2018-04-20-17-00-01-444", 100),
                getOfferLog("2018-04-20-17-00-01-444_2018-05-20-17-00-01-445", 101)
            )
        );
        when(
            restoreBackupService.loadData(
                VitamConfiguration.getDefaultStrategy(),
                DEFAULT_OFFER,
                DataCategory.UNIT_GRAPH,
                "1970-01-01-00-00-00-000_2018-04-20-17-00-01-444"
            )
        ).thenReturn(new FakeInputStream(1, false));

        when(
            restoreBackupService.loadData(
                VitamConfiguration.getDefaultStrategy(),
                DEFAULT_OFFER,
                DataCategory.UNIT_GRAPH,
                "2018-04-20-17-00-01-444_2018-05-20-17-00-01-445"
            )
        ).thenReturn(new FakeInputStream(1, true));

        when(storageClient.getStorageStrategies()).thenReturn(getStorageStrategies());
        when(storageClient.getReferentOffer(VitamConfiguration.getDefaultStrategy())).thenReturn(DEFAULT_OFFER);

        ReconstructionService reconstructionService = new ReconstructionService(
            vitamRepositoryProvider,
            restoreBackupService,
            logbookLifecycleClientFactory,
            storageClientFactory,
            offsetRepository,
            indexManager,
            reconstructionMetricsCache
        );

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
        verify(offsetRepository).createOrUpdateOffset(
            1,
            VitamConfiguration.getDefaultStrategy(),
            DataCategory.UNIT_GRAPH.name(),
            101L
        );
        assertThat(realResponseItem.getTenant()).isEqualTo(1);
        assertThat(realResponseItem.getStatus()).isEqualTo(StatusCode.OK);
        InOrder inOrder = inOrder(reconstructionMetricsCache);
        inOrder
            .verify(reconstructionMetricsCache)
            .registerLastGraphReconstructionDate(MetadataCollections.UNIT, LocalDateTime.of(1970, 1, 1, 0, 0, 0, 0));
        inOrder
            .verify(reconstructionMetricsCache, times(2))
            .registerLastGraphReconstructionDate(
                MetadataCollections.UNIT,
                LocalDateTime.of(2018, 4, 20, 17, 0, 1, 444000000)
            );
        inOrder
            .verify(reconstructionMetricsCache)
            .registerLastGraphReconstructionDate(
                MetadataCollections.UNIT,
                LocalDateTime.of(2018, 5, 20, 17, 0, 1, 445000000)
            );
        inOrder.verifyNoMoreInteractions();
    }

    @RunWithCustomExecutor
    @Test
    public void should_ignore_missing_files_and_return_new_offset_when_item_unit_graph_is_ok() throws Exception {
        VitamConfiguration.setAdminTenant(1);
        // given
        when(
            offsetRepository.findOffsetBy(1, VitamConfiguration.getDefaultStrategy(), DataCategory.UNIT_GRAPH.name())
        ).thenReturn(99L);

        requestItem.setCollection(DataCategory.UNIT_GRAPH.name());
        when(
            restoreBackupService.getListing(
                VitamConfiguration.getDefaultStrategy(),
                DEFAULT_OFFER,
                DataCategory.UNIT_GRAPH,
                100L,
                requestItem.getLimit(),
                Order.ASC,
                VitamConfiguration.getBatchSize()
            )
        ).thenReturn(
            IteratorUtils.arrayIterator(
                getOfferLog("1970-01-01-00-00-00-000_2018-04-20-17-00-01-444", 100),
                getOfferLog("2018-04-20-17-00-01-444_2018-05-20-17-00-01-445", 101)
            )
        );
        when(
            restoreBackupService.loadData(
                VitamConfiguration.getDefaultStrategy(),
                DEFAULT_OFFER,
                DataCategory.UNIT_GRAPH,
                "1970-01-01-00-00-00-000_2018-04-20-17-00-01-444"
            )
        ).thenThrow(new StorageNotFoundException(""));

        when(
            restoreBackupService.loadData(
                VitamConfiguration.getDefaultStrategy(),
                DEFAULT_OFFER,
                DataCategory.UNIT_GRAPH,
                "2018-04-20-17-00-01-444_2018-05-20-17-00-01-445"
            )
        ).thenReturn(new FakeInputStream(1, true));

        when(storageClient.getStorageStrategies()).thenReturn(getStorageStrategies());
        when(storageClient.getReferentOffer(VitamConfiguration.getDefaultStrategy())).thenReturn(DEFAULT_OFFER);

        ReconstructionService reconstructionService = new ReconstructionService(
            vitamRepositoryProvider,
            restoreBackupService,
            logbookLifecycleClientFactory,
            storageClientFactory,
            offsetRepository,
            indexManager,
            reconstructionMetricsCache
        );

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
        verify(offsetRepository).findOffsetBy(
            1,
            VitamConfiguration.getDefaultStrategy(),
            DataCategory.UNIT_GRAPH.name()
        );
        verifyNoMoreInteractions(offsetRepository);
        verifyNoMoreInteractions(esRepository);
        assertThat(realResponseItem.getTenant()).isEqualTo(1);
        assertThat(realResponseItem.getStatus()).isEqualTo(StatusCode.KO);
        verify(reconstructionMetricsCache).registerLastGraphReconstructionDate(
            MetadataCollections.UNIT,
            LocalDateTime.of(1970, 1, 1, 0, 0, 0, 0)
        );
        verifyNoMoreInteractions(reconstructionMetricsCache);
    }

    @RunWithCustomExecutor
    @Test
    public void should_return_request_offset_when_item_limit_zero()
        throws StorageServerClientException, InvalidParseOperationException, StorageNotFoundClientException {
        // given
        requestItem.setLimit(0);
        when(offsetRepository.findOffsetBy(10, STRATEGY_UNIT, MetadataCollections.UNIT.getName())).thenReturn(99L);

        when(
            restoreBackupService.getListing(
                STRATEGY_UNIT,
                DEFAULT_OFFER,
                DataCategory.UNIT,
                100L,
                requestItem.getLimit(),
                Order.ASC,
                VitamConfiguration.getBatchSize()
            )
        ).thenReturn(IteratorUtils.emptyIterator());

        when(storageClient.getStorageStrategies()).thenReturn(getStorageStrategies());
        when(storageClient.getReferentOffer(STRATEGY_UNIT)).thenReturn(DEFAULT_OFFER);

        ReconstructionService reconstructionService = new ReconstructionService(
            vitamRepositoryProvider,
            restoreBackupService,
            logbookLifecycleClientFactory,
            storageClientFactory,
            offsetRepository,
            indexManager,
            reconstructionMetricsCache
        );
        // when
        ReconstructionResponseItem realResponseItem = reconstructionService.reconstruct(requestItem);
        // then
        assertThat(realResponseItem).isNotNull();
        assertThat(realResponseItem.getCollection()).isEqualTo(MetadataCollections.UNIT.name());
        verify(offsetRepository, never()).createOrUpdateOffset(anyInt(), anyString(), anyString(), anyLong());
        assertThat(realResponseItem.getTenant()).isEqualTo(10);
        assertThat(realResponseItem.getStatus()).isEqualTo(StatusCode.OK);
    }

    @RunWithCustomExecutor
    @Test
    public void should_do_nothing_when_no_new_unit_graph_to_reconstruct() throws Exception {
        VitamConfiguration.setAdminTenant(1);
        // given
        when(
            offsetRepository.findOffsetBy(1, VitamConfiguration.getDefaultStrategy(), DataCategory.UNIT_GRAPH.name())
        ).thenReturn(99L);

        requestItem.setCollection(DataCategory.UNIT_GRAPH.name());
        when(
            restoreBackupService.getListing(
                VitamConfiguration.getDefaultStrategy(),
                DEFAULT_OFFER,
                DataCategory.UNIT_GRAPH,
                100L,
                requestItem.getLimit(),
                Order.ASC,
                VitamConfiguration.getBatchSize()
            )
        ).thenReturn(IteratorUtils.emptyIterator());

        when(storageClient.getStorageStrategies()).thenReturn(getStorageStrategies());
        when(storageClient.getReferentOffer(VitamConfiguration.getDefaultStrategy())).thenReturn(DEFAULT_OFFER);

        ReconstructionService reconstructionService = new ReconstructionService(
            vitamRepositoryProvider,
            restoreBackupService,
            logbookLifecycleClientFactory,
            storageClientFactory,
            offsetRepository,
            indexManager,
            reconstructionMetricsCache
        );

        logicalClock.freezeTime();
        LocalDateTime reconstructionDate = LocalDateUtil.now();
        // when
        ReconstructionResponseItem realResponseItem = reconstructionService.reconstruct(requestItem);
        // then
        assertThat(realResponseItem).isNotNull();
        assertThat(realResponseItem.getCollection()).isEqualTo(DataCategory.UNIT_GRAPH.name());
        assertThat(realResponseItem.getTenant()).isEqualTo(1);
        assertThat(realResponseItem.getStatus()).isEqualTo(StatusCode.OK);

        verify(offsetRepository).findOffsetBy(
            1,
            VitamConfiguration.getDefaultStrategy(),
            DataCategory.UNIT_GRAPH.name()
        );
        verifyNoMoreInteractions(offsetRepository);
        verifyNoMoreInteractions(mongoRepository);
        verifyNoMoreInteractions(esRepository);
        verify(reconstructionMetricsCache).registerLastGraphReconstructionDate(
            MetadataCollections.UNIT,
            reconstructionDate
        );
        verifyNoMoreInteractions(reconstructionMetricsCache);
    }

    @RunWithCustomExecutor
    @Test
    public void should_throw_IllegalArgumentException_when_item_is_negative() {
        // given
        requestItem.setLimit(-5);
        ReconstructionService reconstructionService = new ReconstructionService(
            vitamRepositoryProvider,
            restoreBackupService,
            logbookLifecycleClientFactory,
            storageClientFactory,
            offsetRepository,
            indexManager,
            reconstructionMetricsCache
        );
        // when + then
        assertThatCode(() -> reconstructionService.reconstruct(null)).isInstanceOf(IllegalArgumentException.class);
        verifyNoMoreInteractions(reconstructionMetricsCache);
    }

    @RunWithCustomExecutor
    @Test
    public void should_throw_IllegalArgumentException_when_item_is_null() {
        // given
        ReconstructionService reconstructionService = new ReconstructionService(
            vitamRepositoryProvider,
            restoreBackupService,
            logbookLifecycleClientFactory,
            storageClientFactory,
            offsetRepository,
            indexManager,
            reconstructionMetricsCache
        );
        // when + then
        assertThatCode(() -> reconstructionService.reconstruct(null)).isInstanceOf(IllegalArgumentException.class);
        verifyNoMoreInteractions(reconstructionMetricsCache);
    }

    @RunWithCustomExecutor
    @Test
    public void should_throw_IllegalArgumentException_when_item_collection_is_null() {
        // given
        ReconstructionService reconstructionService = new ReconstructionService(
            vitamRepositoryProvider,
            restoreBackupService,
            logbookLifecycleClientFactory,
            storageClientFactory,
            offsetRepository,
            indexManager,
            reconstructionMetricsCache
        );
        // when + then
        assertThatCode(() -> reconstructionService.reconstruct(requestItem.setCollection(null))).isInstanceOf(
            IllegalArgumentException.class
        );
        verifyNoMoreInteractions(reconstructionMetricsCache);
    }

    @RunWithCustomExecutor
    @Test
    public void should_throw_IllegalArgumentException_when_item_collection_is_invalid() {
        // given
        ReconstructionService reconstructionService = new ReconstructionService(
            vitamRepositoryProvider,
            restoreBackupService,
            logbookLifecycleClientFactory,
            storageClientFactory,
            offsetRepository,
            indexManager,
            reconstructionMetricsCache
        );
        // when + then
        assertThatCode(() -> reconstructionService.reconstruct(requestItem.setCollection("toto"))).isInstanceOf(
            IllegalArgumentException.class
        );
        verifyNoMoreInteractions(reconstructionMetricsCache);
    }

    @RunWithCustomExecutor
    @Test
    public void should_throw_IllegalArgumentException_when_item_tenant_is_null() {
        // given
        ReconstructionService reconstructionService = new ReconstructionService(
            vitamRepositoryProvider,
            restoreBackupService,
            logbookLifecycleClientFactory,
            storageClientFactory,
            offsetRepository,
            indexManager,
            reconstructionMetricsCache
        );
        // when + then
        assertThatCode(() -> reconstructionService.reconstruct(requestItem.setTenant(null))).isInstanceOf(
            IllegalArgumentException.class
        );
        verifyNoMoreInteractions(reconstructionMetricsCache);
    }

    @RunWithCustomExecutor
    @Test
    public void should_return_ko_when_mongo_exception() throws Exception {
        // Given
        when(offsetRepository.findOffsetBy(10, STRATEGY_UNIT, MetadataCollections.UNIT.getName())).thenReturn(99L);
        when(
            restoreBackupService.getListing(
                STRATEGY_UNIT,
                DEFAULT_OFFER,
                DataCategory.UNIT,
                100L,
                requestItem.getLimit(),
                Order.ASC,
                VitamConfiguration.getBatchSize()
            )
        ).thenReturn(IteratorUtils.arrayIterator(getOfferLog(100), getOfferLog(101)));
        when(
            restoreBackupService.loadData(STRATEGY_UNIT, DEFAULT_OFFER, MetadataCollections.UNIT, "100", 100L)
        ).thenReturn(getUnitMetadataBackupModel("100", 100L));
        when(
            restoreBackupService.loadData(STRATEGY_UNIT, DEFAULT_OFFER, MetadataCollections.UNIT, "101", 101L)
        ).thenReturn(getUnitMetadataBackupModel("101", 101L));
        when(storageClient.getStorageStrategies()).thenReturn(getStorageStrategies());
        doNothing().when(logbookLifecycleClient).createRawbulkUnitlifecycles(any());
        when(storageClient.getReferentOffer(STRATEGY_UNIT)).thenReturn(DEFAULT_OFFER);
        // doThrow(new DatabaseException("mongo error")).when(mongoRepository).update(any(List.class));
        final int[] cpt = { 0 };
        Mockito.doAnswer(i -> {
            cpt[0]++;
            if (cpt[0] == 1) {
                throw new DatabaseException(mock(MongoBulkWriteException.class));
            } else {
                throw new DatabaseException("mongo error");
            }
        })
            .when(mongoRepository)
            .update(anyList());

        ReconstructionService reconstructionService = new ReconstructionService(
            vitamRepositoryProvider,
            restoreBackupService,
            logbookLifecycleClientFactory,
            storageClientFactory,
            offsetRepository,
            indexManager,
            reconstructionMetricsCache
        );

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
        verifyNoMoreInteractions(reconstructionMetricsCache);
    }

    @RunWithCustomExecutor
    @Test
    public void should_return_ko_when_es_exception() throws Exception {
        // Given
        when(offsetRepository.findOffsetBy(10, STRATEGY_UNIT, MetadataCollections.UNIT.getName())).thenReturn(99L);
        when(
            restoreBackupService.getListing(
                STRATEGY_UNIT,
                DEFAULT_OFFER,
                DataCategory.UNIT,
                100L,
                requestItem.getLimit(),
                Order.ASC,
                VitamConfiguration.getBatchSize()
            )
        ).thenReturn(IteratorUtils.arrayIterator(getOfferLog(100), getOfferLog(101)));
        when(
            restoreBackupService.loadData(STRATEGY_UNIT, DEFAULT_OFFER, MetadataCollections.UNIT, "100", 100L)
        ).thenReturn(getUnitMetadataBackupModel("100", 100L));
        when(
            restoreBackupService.loadData(STRATEGY_UNIT, DEFAULT_OFFER, MetadataCollections.UNIT, "101", 101L)
        ).thenReturn(getUnitMetadataBackupModel("101", 101L));
        when(storageClient.getStorageStrategies()).thenReturn(getStorageStrategies());
        doNothing().when(logbookLifecycleClient).createRawbulkUnitlifecycles(any());
        doThrow(new DatabaseException("Elasticsearch error")).when(esRepository).save(anyList());
        when(storageClient.getReferentOffer(STRATEGY_UNIT)).thenReturn(DEFAULT_OFFER);

        ReconstructionService reconstructionService = new ReconstructionService(
            vitamRepositoryProvider,
            restoreBackupService,
            logbookLifecycleClientFactory,
            storageClientFactory,
            offsetRepository,
            indexManager,
            reconstructionMetricsCache
        );

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
        verifyNoMoreInteractions(reconstructionMetricsCache);
    }

    @RunWithCustomExecutor
    @Test
    public void should_return_ko_when_logbook_exception() throws Exception {
        // Given
        when(offsetRepository.findOffsetBy(10, STRATEGY_UNIT, MetadataCollections.UNIT.getName())).thenReturn(99L);
        when(
            restoreBackupService.getListing(
                STRATEGY_UNIT,
                DEFAULT_OFFER,
                DataCategory.UNIT,
                100L,
                requestItem.getLimit(),
                Order.ASC,
                VitamConfiguration.getBatchSize()
            )
        ).thenReturn(IteratorUtils.arrayIterator(getOfferLog(100), getOfferLog(101)));
        when(
            restoreBackupService.loadData(STRATEGY_UNIT, DEFAULT_OFFER, MetadataCollections.UNIT, "100", 100L)
        ).thenReturn(getUnitMetadataBackupModel("100", 100L));
        when(
            restoreBackupService.loadData(STRATEGY_UNIT, DEFAULT_OFFER, MetadataCollections.UNIT, "101", 101L)
        ).thenReturn(getUnitMetadataBackupModel("101", 101L));
        when(storageClient.getStorageStrategies()).thenReturn(getStorageStrategies());
        doThrow(new LogbookClientServerException("logbook error"))
            .when(logbookLifecycleClient)
            .createRawbulkUnitlifecycles(any());
        when(storageClient.getReferentOffer(STRATEGY_UNIT)).thenReturn(DEFAULT_OFFER);

        ReconstructionService reconstructionService = new ReconstructionService(
            vitamRepositoryProvider,
            restoreBackupService,
            logbookLifecycleClientFactory,
            storageClientFactory,
            offsetRepository,
            indexManager,
            reconstructionMetricsCache
        );

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
        verifyNoMoreInteractions(reconstructionMetricsCache);
    }

    @RunWithCustomExecutor
    @Test
    public void should_return_request_offset_when_lifecycle_null() throws Exception {
        // Given
        when(offsetRepository.findOffsetBy(10, STRATEGY_UNIT, MetadataCollections.UNIT.getName())).thenReturn(99L);
        MetadataBackupModel metadataBackupModel100 = getUnitMetadataBackupModel("100", 100L);
        metadataBackupModel100.setLifecycle(null);
        when(
            restoreBackupService.getListing(
                STRATEGY_UNIT,
                DEFAULT_OFFER,
                DataCategory.UNIT,
                100L,
                requestItem.getLimit(),
                Order.ASC,
                VitamConfiguration.getBatchSize()
            )
        ).thenReturn(IteratorUtils.arrayIterator(getOfferLog(100), getOfferLog(101)));
        when(
            restoreBackupService.loadData(STRATEGY_UNIT, DEFAULT_OFFER, MetadataCollections.UNIT, "100", 100L)
        ).thenReturn(metadataBackupModel100);
        when(
            restoreBackupService.loadData(STRATEGY_UNIT, DEFAULT_OFFER, MetadataCollections.UNIT, "101", 101L)
        ).thenReturn(getUnitMetadataBackupModel("101", 101L));
        when(storageClient.getStorageStrategies()).thenReturn(getStorageStrategies());
        doThrow(new LogbookClientServerException("logbook error"))
            .when(logbookLifecycleClient)
            .createRawbulkUnitlifecycles(any());
        when(storageClient.getReferentOffer(STRATEGY_UNIT)).thenReturn(DEFAULT_OFFER);

        ReconstructionService reconstructionService = new ReconstructionService(
            vitamRepositoryProvider,
            restoreBackupService,
            logbookLifecycleClientFactory,
            storageClientFactory,
            offsetRepository,
            indexManager,
            reconstructionMetricsCache
        );
        // When
        ReconstructionResponseItem realResponseItem = reconstructionService.reconstruct(requestItem);

        // Then
        assertThat(realResponseItem).isNotNull();
        assertThat(realResponseItem.getCollection()).isEqualTo(MetadataCollections.UNIT.name());
        assertThat(realResponseItem.getTenant()).isEqualTo(10);
        assertThat(realResponseItem.getStatus()).isEqualTo(StatusCode.KO);
        verifyNoMoreInteractions(reconstructionMetricsCache);
    }

    @RunWithCustomExecutor
    @Test
    public void should_return_request_offset_when_metadata_null() throws Exception {
        // Given
        when(offsetRepository.findOffsetBy(10, STRATEGY_UNIT, MetadataCollections.UNIT.getName())).thenReturn(99L);
        MetadataBackupModel metadataBackupModel100 = getUnitMetadataBackupModel("100", 100L);
        metadataBackupModel100.setUnit(null);
        when(
            restoreBackupService.getListing(
                STRATEGY_UNIT,
                DEFAULT_OFFER,
                DataCategory.UNIT,
                100L,
                requestItem.getLimit(),
                Order.ASC,
                VitamConfiguration.getBatchSize()
            )
        ).thenReturn(IteratorUtils.arrayIterator(getOfferLog(100), getOfferLog(101)));
        when(
            restoreBackupService.loadData(STRATEGY_UNIT, DEFAULT_OFFER, MetadataCollections.UNIT, "100", 100L)
        ).thenReturn(metadataBackupModel100);
        when(
            restoreBackupService.loadData(STRATEGY_UNIT, DEFAULT_OFFER, MetadataCollections.UNIT, "101", 101L)
        ).thenReturn(getUnitMetadataBackupModel("101", 101L));
        when(storageClient.getStorageStrategies()).thenReturn(getStorageStrategies());
        when(storageClient.getReferentOffer(STRATEGY_UNIT)).thenReturn(DEFAULT_OFFER);

        ReconstructionService reconstructionService = new ReconstructionService(
            vitamRepositoryProvider,
            restoreBackupService,
            logbookLifecycleClientFactory,
            storageClientFactory,
            offsetRepository,
            indexManager,
            reconstructionMetricsCache
        );
        // When
        ReconstructionResponseItem realResponseItem = reconstructionService.reconstruct(requestItem);
        // Then
        assertThat(realResponseItem).isNotNull();
        assertThat(realResponseItem.getCollection()).isEqualTo(MetadataCollections.UNIT.name());
        assertThat(realResponseItem.getTenant()).isEqualTo(10);
        assertThat(realResponseItem.getStatus()).isEqualTo(StatusCode.KO);
        verifyNoMoreInteractions(reconstructionMetricsCache);
    }

    @RunWithCustomExecutor
    @Test
    public void should_return_new_offset_when_loading_missing_data() throws Exception {
        // given
        when(offsetRepository.findOffsetBy(10, STRATEGY_UNIT, MetadataCollections.UNIT.getName())).thenReturn(99L);
        when(
            restoreBackupService.getListing(
                STRATEGY_UNIT,
                DEFAULT_OFFER,
                DataCategory.UNIT,
                100L,
                requestItem.getLimit(),
                Order.ASC,
                VitamConfiguration.getBatchSize()
            )
        ).thenReturn(IteratorUtils.singletonIterator(getOfferLog(100)));
        when(
            restoreBackupService.loadData(STRATEGY_UNIT, DEFAULT_OFFER, MetadataCollections.UNIT, "100", 100L)
        ).thenThrow(new StorageNotFoundException(""));
        when(storageClient.getStorageStrategies()).thenReturn(getStorageStrategies());
        doNothing().when(logbookLifecycleClient).createRawbulkUnitlifecycles(any());
        when(storageClient.getReferentOffer(STRATEGY_UNIT)).thenReturn(DEFAULT_OFFER);

        FindIterable findIterable = mock(FindIterable.class);
        final MongoCursor<String> iterator = mock(MongoCursor.class);
        when(mongoRepository.findDocuments(any(), any())).thenReturn(findIterable);
        when(findIterable.iterator()).thenReturn(iterator);
        when(iterator.hasNext()).thenReturn(Boolean.FALSE);

        ReconstructionService reconstructionService = new ReconstructionService(
            vitamRepositoryProvider,
            restoreBackupService,
            logbookLifecycleClientFactory,
            storageClientFactory,
            offsetRepository,
            indexManager,
            reconstructionMetricsCache
        );

        logicalClock.freezeTime();
        LocalDateTime reconstructionDateTime = LocalDateUtil.now();
        // when
        ReconstructionResponseItem realResponseItem = reconstructionService.reconstruct(requestItem);

        // then
        assertThat(realResponseItem).isNotNull();
        assertThat(realResponseItem.getCollection()).isEqualTo(MetadataCollections.UNIT.name());
        verify(offsetRepository).createOrUpdateOffset(10, STRATEGY_UNIT, MetadataCollections.UNIT.getName(), 100L);
        assertThat(realResponseItem.getTenant()).isEqualTo(10);
        assertThat(realResponseItem.getStatus()).isEqualTo(StatusCode.OK);
        verify(reconstructionMetricsCache).registerLastDocumentReconstructionDate(
            MetadataCollections.UNIT,
            10,
            STRATEGY_UNIT,
            reconstructionDateTime
        );
        verifyNoMoreInteractions(reconstructionMetricsCache);
    }

    private MetadataBackupModel getUnitMetadataBackupModel(String id, Long offset) {
        MetadataBackupModel model = new MetadataBackupModel();
        model.setUnit(new Document("_id", id).append("_v", 0));
        model.setLifecycle(
            new Document("_id", id)
                .append("evDateTime", "2000-01-01T00:11:22.333")
                .append("_lastPersistedDate", "2000-01-01T11:22:33.444")
                .append("evTypeProc", "INGEST")
                .append(
                    "events",
                    List.of(
                        new Document()
                            .append("_lastPersistedDate", "2000-01-01T11:22:33.444")
                            .append("evDateTime", "2000-01-01T11:22:33.444")
                            .append("outDetail", "LFC.CHECK_MANIFEST.OK"),
                        new Document()
                            .append("_lastPersistedDate", "2000-01-01T11:22:33.444")
                            .append("evDateTime", "2000-01-01T11:22:33.444")
                            .append("outDetail", "LFC.CHECK_MANIFEST.LFC_CREATION.OK")
                    )
                )
        );
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

    private MetadataBackupModel getGotMetadataBackupModelWithTruncatedDateTime(String id, Long offset) {
        MetadataBackupModel gotMetadataBackupModel = getGotMetadataBackupModel(id, offset);
        gotMetadataBackupModel.getLifecycle().put("evDateTime", "2000-05-01T11:22");
        List<Document> events = (List<Document>) gotMetadataBackupModel.getLifecycle().get("events");
        events.get(events.size() - 1).put("evDateTime", "2000-01-01T00:22:33");
        return gotMetadataBackupModel;
    }

    private MetadataBackupModel getGotMetadataBackupModel(String id, Long offset) {
        MetadataBackupModel model = new MetadataBackupModel();
        model.setGot(new Document("_id", id).append("_v", 0));
        model.setLifecycle(
            new Document("_id", id)
                .append("evDateTime", "2000-01-01T00:11:22.333")
                .append("_lastPersistedDate", "2000-01-01T11:22:33.444")
                .append("evTypeProc", "INGEST")
                .append(
                    "events",
                    List.of(
                        new Document()
                            .append("_lastPersistedDate", "2000-01-01T11:22:33.444")
                            .append("evDateTime", "2000-01-01T11:22:33.444")
                            .append("outDetail", "LFC.CHECK_MANIFEST.OK"),
                        new Document()
                            .append("_lastPersistedDate", "2000-01-01T11:22:33.444")
                            .append("evDateTime", "2000-01-01T11:22:33.444")
                            .append("outDetail", "LFC.CHECK_MANIFEST.LFC_CREATION.OK")
                    )
                )
        );
        model.setOffset(offset);
        return model;
    }

    private RequestResponse<StorageStrategy> getStorageStrategies() throws InvalidParseOperationException {
        OfferReference offerReference = new OfferReference();
        offerReference.setId(OFFER_1);
        offerReference.setReferent(true);
        StorageStrategy strategy = new StorageStrategy();
        strategy.setId(STRATEGY_UNIT);
        strategy.getOffers().add(offerReference);

        return ClientMockResultHelper.createResponse(List.of(strategy));
    }

    private RequestResponse<StorageStrategy> getStorageStrategiesWithSameReferentOfferForDifferentStrategies()
        throws InvalidParseOperationException {
        OfferReference offerReference = new OfferReference();
        offerReference.setId(OFFER_1);
        offerReference.setReferent(true);
        StorageStrategy strategy1 = new StorageStrategy();
        strategy1.setId(STRATEGY_UNIT);
        strategy1.getOffers().add(offerReference);
        StorageStrategy strategy2 = new StorageStrategy();
        strategy2.setId(STRATEGY_UNIT + "-bis");
        strategy2.getOffers().add(offerReference);

        return ClientMockResultHelper.createResponse(Arrays.asList(strategy1, strategy2));
    }
}
