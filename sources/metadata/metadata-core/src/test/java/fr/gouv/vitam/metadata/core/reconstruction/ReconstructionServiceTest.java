/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 *******************************************************************************/
package fr.gouv.vitam.metadata.core.reconstruction;

import static com.mongodb.client.model.Projections.include;
import static fr.gouv.vitam.common.database.utils.MetadataDocumentHelper.getComputedGraphObjectGroupFields;
import static fr.gouv.vitam.common.database.utils.MetadataDocumentHelper.getComputedGraphUnitFields;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import fr.gouv.vitam.common.database.api.impl.VitamElasticsearchRepository;
import fr.gouv.vitam.common.database.api.impl.VitamMongoRepository;
import fr.gouv.vitam.common.database.offset.OffsetRepository;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.core.database.collections.VitamRepositoryFactory;
import fr.gouv.vitam.metadata.core.database.collections.VitamRepositoryProvider;
import fr.gouv.vitam.metadata.core.model.ReconstructionRequestItem;
import fr.gouv.vitam.metadata.core.model.ReconstructionResponseItem;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.Order;
import org.assertj.core.util.Lists;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * ReconstructionService tests.
 */
public class ReconstructionServiceTest {

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    private VitamRepositoryProvider vitamRepositoryProvider;
    private VitamMongoRepository mongoRepository;
    private VitamElasticsearchRepository esRepository;
    private RestoreBackupService restoreBackupService;
    private LogbookLifeCyclesClientFactory logbookLifecycleClientFactory;
    private LogbookLifeCyclesClient logbookLifecycleClient;

    private ReconstructionRequestItem requestItem;

    private OffsetRepository offsetRepository;

    @Before
    public void setup() {
        vitamRepositoryProvider = mock(VitamRepositoryFactory.class);
        mongoRepository = mock(VitamMongoRepository.class);
        esRepository = mock(VitamElasticsearchRepository.class);
        offsetRepository = mock(OffsetRepository.class);
        when(vitamRepositoryProvider.getVitamMongoRepository(any())).thenReturn(mongoRepository);
        when(vitamRepositoryProvider.getVitamESRepository(any())).thenReturn(esRepository);

        restoreBackupService = mock(RestoreBackupService.class);
        logbookLifecycleClientFactory = mock(LogbookLifeCyclesClientFactory.class);
        logbookLifecycleClient = mock(LogbookLifeCyclesClient.class);
        when(logbookLifecycleClientFactory.getClient()).thenReturn(logbookLifecycleClient);

        requestItem = new ReconstructionRequestItem();
        requestItem.setCollection("UNIT").setTenant(10).setLimit(100);
    }

    @RunWithCustomExecutor
    @Test
    public void should_return_new_offset_when_item_unit_is_ok() throws Exception {
        // given
        when(offsetRepository.findOffsetBy(10, MetadataCollections.UNIT.getName())).thenReturn(100L);
        when(restoreBackupService.getListing("default", DataCategory.UNIT, 100l,
            requestItem.getLimit(), Order.ASC)).thenReturn(
            Lists.newArrayList(getOfferLog(100), getOfferLog(101)));
        when(restoreBackupService.loadData("default", MetadataCollections.UNIT, "100", 100L))
            .thenReturn(getUnitMetadataBackupModel("100", 100L));
        when(restoreBackupService.loadData("default", MetadataCollections.UNIT, "101", 101L))
            .thenReturn(getUnitMetadataBackupModel("101", 101L));
        Mockito.doNothing().when(logbookLifecycleClient).createRawbulkUnitlifecycles(any());

        ReconstructionService reconstructionService =
            new ReconstructionService(vitamRepositoryProvider, restoreBackupService, logbookLifecycleClientFactory,
                offsetRepository);

        FindIterable findIterable = mock(FindIterable.class);
        final MongoCursor<String> iterator = mock(MongoCursor.class);
        final Bson projection = include(getComputedGraphUnitFields());

        when(mongoRepository.findDocuments(anyList(), eq(projection))).thenReturn(findIterable);
        when(findIterable.iterator()).thenReturn(iterator);
        when(iterator.hasNext()).thenReturn(Boolean.FALSE);
        // when
        ReconstructionResponseItem realResponseItem = reconstructionService.reconstruct(requestItem);
        // then
        assertThat(realResponseItem).isNotNull();
        assertThat(realResponseItem.getCollection()).isEqualTo(MetadataCollections.UNIT.name());
        verify(offsetRepository).createOrUpdateOffset(10, MetadataCollections.UNIT.getName(), 101L);
        assertThat(realResponseItem.getTenant()).isEqualTo(10);
        assertThat(realResponseItem.getStatus()).isEqualTo(StatusCode.OK);
    }

    @RunWithCustomExecutor
    @Test
    public void should_return_new_offset_when_item_got_is_ok() throws Exception {
        // given
        when(offsetRepository.findOffsetBy(10, MetadataCollections.OBJECTGROUP.getName())).thenReturn(100L);

        requestItem.setCollection("ObjectGroup");
        when(restoreBackupService.getListing("default", DataCategory.OBJECTGROUP, 100l,
            requestItem.getLimit(), Order.ASC))
            .thenReturn(Arrays.asList(getOfferLog(100), getOfferLog(101)));
        when(restoreBackupService.loadData("default", MetadataCollections.OBJECTGROUP, "100", 100L))
            .thenReturn(getGotMetadataBackupModel("100", 100L));
        when(restoreBackupService.loadData("default", MetadataCollections.OBJECTGROUP, "101", 101L))
            .thenReturn(getGotMetadataBackupModel("101", 101L));
        Mockito.doNothing().when(logbookLifecycleClient).createRawbulkObjectgrouplifecycles(any());

        ReconstructionService reconstructionService =
            new ReconstructionService(vitamRepositoryProvider, restoreBackupService, logbookLifecycleClientFactory,
                offsetRepository);
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
        assertThat(realResponseItem.getCollection()).isEqualTo(MetadataCollections.OBJECTGROUP.name());
        verify(offsetRepository).createOrUpdateOffset(10, MetadataCollections.OBJECTGROUP.getName(), 101L);
        assertThat(realResponseItem.getTenant()).isEqualTo(10);
        assertThat(realResponseItem.getStatus()).isEqualTo(StatusCode.OK);
    }

    @RunWithCustomExecutor
    @Test
    public void should_return_request_offset_when_item_limit_zero() {
        // given
        requestItem.setLimit(0);
        when(offsetRepository.findOffsetBy(10, MetadataCollections.UNIT.getName())).thenReturn(100L);

        when(restoreBackupService.getListing("default", DataCategory.UNIT, 100l,
            requestItem.getLimit(), Order.ASC)).thenReturn(Arrays.asList());

        ReconstructionService reconstructionService =
            new ReconstructionService(vitamRepositoryProvider, restoreBackupService, logbookLifecycleClientFactory,
                offsetRepository);
        // when
        ReconstructionResponseItem realResponseItem = reconstructionService.reconstruct(requestItem);
        // then
        assertThat(realResponseItem).isNotNull();
        assertThat(realResponseItem.getCollection()).isEqualTo(MetadataCollections.UNIT.name());
        verify(offsetRepository).createOrUpdateOffset(10, MetadataCollections.UNIT.getName(), 100L);
        assertThat(realResponseItem.getTenant()).isEqualTo(10);
        assertThat(realResponseItem.getStatus()).isEqualTo(StatusCode.OK);
    }

    @RunWithCustomExecutor
    @Test
    public void should_throw_IllegalArgumentException_when_item_is_negative() {
        // given
        requestItem.setLimit(-5);
        ReconstructionService reconstructionService =
            new ReconstructionService(vitamRepositoryProvider, restoreBackupService, logbookLifecycleClientFactory,
                offsetRepository);
        // when + then
        assertThatCode(() -> reconstructionService.reconstruct(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @RunWithCustomExecutor
    @Test
    public void should_throw_IllegalArgumentException_when_item_is_null() {
        // given
        ReconstructionService reconstructionService =
            new ReconstructionService(vitamRepositoryProvider, restoreBackupService, logbookLifecycleClientFactory,
                offsetRepository);
        // when + then
        assertThatCode(() -> reconstructionService.reconstruct(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @RunWithCustomExecutor
    @Test
    public void should_throw_IllegalArgumentException_when_item_collection_is_null() {
        // given
        ReconstructionService reconstructionService =
            new ReconstructionService(vitamRepositoryProvider, restoreBackupService, logbookLifecycleClientFactory,
                offsetRepository);
        // when + then
        assertThatCode(() -> reconstructionService.reconstruct(requestItem.setCollection(null)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @RunWithCustomExecutor
    @Test
    public void should_throw_IllegalArgumentException_when_item_collection_is_invalid() {
        // given
        ReconstructionService reconstructionService =
            new ReconstructionService(vitamRepositoryProvider, restoreBackupService, logbookLifecycleClientFactory,
                offsetRepository);
        // when + then
        assertThatCode(() -> reconstructionService.reconstruct(requestItem.setCollection("toto")))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @RunWithCustomExecutor
    @Test
    public void should_throw_IllegalArgumentException_when_item_tenant_is_null() {
        // given
        ReconstructionService reconstructionService =
            new ReconstructionService(vitamRepositoryProvider, restoreBackupService, logbookLifecycleClientFactory,
                offsetRepository);
        // when + then
        assertThatCode(() -> reconstructionService.reconstruct(requestItem.setTenant(null)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @RunWithCustomExecutor
    @Test
    public void should_return_request_offset_when_mongo_exception()
        throws DatabaseException, LogbookClientBadRequestException, LogbookClientServerException {
        // Given
        when(offsetRepository.findOffsetBy(10, MetadataCollections.UNIT.getName())).thenReturn(100L);
        when(restoreBackupService.getListing("default", DataCategory.UNIT, 100l,
            requestItem.getLimit(), Order.ASC)).thenReturn(
            Arrays.asList(getOfferLog(100), getOfferLog(101)));
        when(restoreBackupService.loadData("default", MetadataCollections.UNIT, "100", 100L))
            .thenReturn(getUnitMetadataBackupModel("100", 100L));
        when(restoreBackupService.loadData("default", MetadataCollections.UNIT, "101", 101L))
            .thenReturn(getUnitMetadataBackupModel("101", 101L));
        Mockito.doNothing().when(logbookLifecycleClient).createRawbulkUnitlifecycles(any());
        Mockito.doThrow(new DatabaseException("mongo error")).when(mongoRepository).save(any(List.class));

        ReconstructionService reconstructionService =
            new ReconstructionService(vitamRepositoryProvider, restoreBackupService, logbookLifecycleClientFactory,
                offsetRepository);
        FindIterable<Document> findIterable = mock(FindIterable.class);
        final MongoCursor<Document> iterator = mock(MongoCursor.class);
        final Bson projection = include(getComputedGraphUnitFields());
        when(mongoRepository.findDocuments(anyList(), any(Bson.class))).thenReturn(findIterable);
        when(findIterable.iterator()).thenReturn(iterator);
        when(iterator.hasNext()).thenReturn(Boolean.FALSE);

        // When
        ReconstructionResponseItem realResponseItem = reconstructionService.reconstruct(requestItem);

        // Then
        assertThat(realResponseItem).isNotNull();
        assertThat(realResponseItem.getCollection()).isEqualTo(MetadataCollections.UNIT.name());
        verify(offsetRepository).createOrUpdateOffset(10, MetadataCollections.UNIT.getName(), 101L);
        assertThat(realResponseItem.getTenant()).isEqualTo(10);
        assertThat(realResponseItem.getStatus()).isEqualTo(StatusCode.OK);
    }


    @RunWithCustomExecutor
    @Test
    public void should_return_request_offset_when_es_exception()
        throws DatabaseException, LogbookClientBadRequestException, LogbookClientServerException {
        // Given
        when(offsetRepository.findOffsetBy(10, MetadataCollections.UNIT.getName())).thenReturn(100L);
        when(restoreBackupService.getListing("default", DataCategory.UNIT, 100l, requestItem.getLimit(), Order.ASC))
            .thenReturn(Arrays.asList(getOfferLog(100), getOfferLog(101)));
        when(restoreBackupService.loadData("default", MetadataCollections.UNIT, "100", 100L))
            .thenReturn(getUnitMetadataBackupModel("100", 100L));
        when(restoreBackupService.loadData("default", MetadataCollections.UNIT, "101", 101L))
            .thenReturn(getUnitMetadataBackupModel("101", 101L));
        Mockito.doNothing().when(logbookLifecycleClient).createRawbulkUnitlifecycles(any());
        Mockito.doThrow(new DatabaseException("mongo error")).when(esRepository).save(any(List.class));

        ReconstructionService reconstructionService =
            new ReconstructionService(vitamRepositoryProvider, restoreBackupService, logbookLifecycleClientFactory,
                offsetRepository);

        FindIterable findIterable = mock(FindIterable.class);
        final MongoCursor<String> iterator = mock(MongoCursor.class);
        final Bson projection = include(getComputedGraphUnitFields());

        when(mongoRepository.findDocuments(anyList(), eq(projection))).thenReturn(findIterable);
        when(findIterable.iterator()).thenReturn(iterator);
        when(iterator.hasNext()).thenReturn(Boolean.FALSE);
        // When
        ReconstructionResponseItem realResponseItem = reconstructionService.reconstruct(requestItem);

        // Then
        assertThat(realResponseItem).isNotNull();
        assertThat(realResponseItem.getCollection()).isEqualTo(MetadataCollections.UNIT.name());
        verify(offsetRepository).createOrUpdateOffset(10, MetadataCollections.UNIT.getName(), 100L);
        assertThat(realResponseItem.getTenant()).isEqualTo(10);
        assertThat(realResponseItem.getStatus()).isEqualTo(StatusCode.KO);
    }

    @RunWithCustomExecutor
    @Test
    public void should_return_request_offset_when_logbook_exception() throws LogbookClientBadRequestException,
        LogbookClientServerException {
        // Given
        when(offsetRepository.findOffsetBy(10, MetadataCollections.UNIT.getName())).thenReturn(100L);
        when(restoreBackupService.getListing("default", DataCategory.UNIT, 100l,
            requestItem.getLimit(), Order.ASC)).thenReturn(Arrays.asList(getOfferLog(100), getOfferLog(101)));
        when(restoreBackupService.loadData("default", MetadataCollections.UNIT, "100", 100L))
            .thenReturn(getUnitMetadataBackupModel("100", 100L));
        when(restoreBackupService.loadData("default", MetadataCollections.UNIT, "101", 101L))
            .thenReturn(getUnitMetadataBackupModel("101", 101L));
        Mockito.doThrow(new LogbookClientServerException("logbook error"))
            .when(logbookLifecycleClient)
            .createRawbulkUnitlifecycles(any());

        ReconstructionService reconstructionService =
            new ReconstructionService(vitamRepositoryProvider, restoreBackupService, logbookLifecycleClientFactory,
                offsetRepository);

        FindIterable findIterable = mock(FindIterable.class);
        final MongoCursor<String> iterator = mock(MongoCursor.class);
        final Bson projection = include(getComputedGraphUnitFields());

        when(mongoRepository.findDocuments(anyList(), eq(projection))).thenReturn(findIterable);

        when(findIterable.iterator()).thenReturn(iterator);
        when(iterator.hasNext()).thenReturn(Boolean.FALSE);
        // When
        ReconstructionResponseItem realResponseItem = reconstructionService.reconstruct(requestItem);

        // Then
        assertThat(realResponseItem).isNotNull();
        assertThat(realResponseItem.getCollection()).isEqualTo(MetadataCollections.UNIT.name());
        verify(offsetRepository).createOrUpdateOffset(10, MetadataCollections.UNIT.getName(), 100L);
        assertThat(realResponseItem.getTenant()).isEqualTo(10);
        assertThat(realResponseItem.getStatus()).isEqualTo(StatusCode.KO);
    }

    @RunWithCustomExecutor
    @Test
    public void should_return_request_offset_when_lifecycle_null()
        throws LogbookClientBadRequestException, LogbookClientServerException {
        // Given
        when(offsetRepository.findOffsetBy(10, MetadataCollections.UNIT.getName())).thenReturn(100L);
        MetadataBackupModel metadataBackupModel100 = getUnitMetadataBackupModel("100", 100L);
        metadataBackupModel100.setLifecycle(null);
        when(restoreBackupService.getListing("default", DataCategory.UNIT, 100l,
            requestItem.getLimit(), Order.ASC)).thenReturn(Arrays.asList(getOfferLog(100), getOfferLog(101)));
        when(restoreBackupService.loadData("default", MetadataCollections.UNIT, "100", 100L))
            .thenReturn(metadataBackupModel100);
        when(restoreBackupService.loadData("default", MetadataCollections.UNIT, "101", 101L))
            .thenReturn(getUnitMetadataBackupModel("101", 101L));
        Mockito.doThrow(new LogbookClientServerException("logbook error"))
            .when(logbookLifecycleClient)
            .createRawbulkUnitlifecycles(any());

        ReconstructionService reconstructionService =
            new ReconstructionService(vitamRepositoryProvider, restoreBackupService, logbookLifecycleClientFactory,
                offsetRepository);
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
    public void should_return_request_offset_when_metadata_null() {
        // Given
        when(offsetRepository.findOffsetBy(10, MetadataCollections.UNIT.getName())).thenReturn(100L);
        MetadataBackupModel metadataBackupModel100 = getUnitMetadataBackupModel("100", 100L);
        metadataBackupModel100.setUnit(null);
        when(restoreBackupService.getListing("default", DataCategory.UNIT, 100l,
            requestItem.getLimit(), Order.ASC)).thenReturn(Arrays.asList(getOfferLog(100), getOfferLog(101)));
        when(restoreBackupService.loadData("default", MetadataCollections.UNIT, "100", 100L))
            .thenReturn(metadataBackupModel100);
        when(restoreBackupService.loadData("default", MetadataCollections.UNIT, "101", 101L))
            .thenReturn(getUnitMetadataBackupModel("101", 101L));

        ReconstructionService reconstructionService =
            new ReconstructionService(vitamRepositoryProvider, restoreBackupService, logbookLifecycleClientFactory,
                offsetRepository);
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
    public void should_return_request_offset_when_loading_data_return_null()
        throws DatabaseException, LogbookClientBadRequestException, LogbookClientServerException {
        // given
        when(offsetRepository.findOffsetBy(10, MetadataCollections.UNIT.getName())).thenReturn(100L);
        when(restoreBackupService.getListing("default", DataCategory.UNIT, 100l,
            requestItem.getLimit(), Order.ASC)).thenReturn(Arrays.asList(getOfferLog(100), getOfferLog(101)));
        when(restoreBackupService.loadData("default", MetadataCollections.UNIT, "100", 100L))
            .thenReturn(getUnitMetadataBackupModel("100", 100L));
        when(restoreBackupService.loadData("default", MetadataCollections.UNIT, "101", 101L)).thenReturn(null);
        Mockito.doNothing().when(logbookLifecycleClient).createRawbulkUnitlifecycles(any());

        ReconstructionService reconstructionService =
            new ReconstructionService(vitamRepositoryProvider, restoreBackupService, logbookLifecycleClientFactory,
                offsetRepository);
        // when
        ReconstructionResponseItem realResponseItem = reconstructionService.reconstruct(requestItem);
        // then
        assertThat(realResponseItem).isNotNull();
        assertThat(realResponseItem.getCollection()).isEqualTo(MetadataCollections.UNIT.name());
        verify(offsetRepository).createOrUpdateOffset(10, MetadataCollections.UNIT.getName(), 100L);
        assertThat(realResponseItem.getTenant()).isEqualTo(10);
        assertThat(realResponseItem.getStatus()).isEqualTo(StatusCode.KO);
    }

    private MetadataBackupModel getUnitMetadataBackupModel(String id, Long offset) {
        MetadataBackupModel model = new MetadataBackupModel();
        model.setUnit(new Document("_id", id));
        model.setLifecycle(new Document("_id", id));
        model.setOffset(offset);
        return model;
    }

    private OfferLog getOfferLog(long sequence) {
        OfferLog offerLog = new OfferLog("container", "" + sequence, "write");
        offerLog.setSequence(sequence);
        return offerLog;
    }

    private MetadataBackupModel getGotMetadataBackupModel(String id, Long offset) {
        MetadataBackupModel model = new MetadataBackupModel();
        model.setGot(new Document("_id", id));
        model.setLifecycle(new Document("_id", id));
        model.setOffset(offset);
        return model;
    }

}
