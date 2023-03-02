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
package fr.gouv.vitam.logbook.common.server.reconstruction;

import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.api.VitamRepositoryFactory;
import fr.gouv.vitam.common.database.api.VitamRepositoryProvider;
import fr.gouv.vitam.common.database.api.impl.VitamElasticsearchRepository;
import fr.gouv.vitam.common.database.api.impl.VitamMongoRepository;
import fr.gouv.vitam.common.database.offset.OffsetRepository;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.logbook.common.model.reconstruction.ReconstructionRequestItem;
import fr.gouv.vitam.logbook.common.model.reconstruction.ReconstructionResponseItem;
import fr.gouv.vitam.logbook.common.server.config.ElasticsearchLogbookIndexManager;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookTransformData;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.OfferLogAction;
import org.apache.commons.collections4.IteratorUtils;
import org.bson.Document;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;

import static fr.gouv.vitam.logbook.common.server.reconstruction.ReconstructionService.LOGBOOK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    private ReconstructionRequestItem requestItem;

    private OffsetRepository offsetRepository;
    private ElasticsearchLogbookIndexManager indexManager;

    private static final long OFFSET = 100L;
    private static final long NEXT_OFFSET = OFFSET + 1L;
    private static final int TENANT = 10;

    @Before
    public void setup() {
        vitamRepositoryProvider = mock(VitamRepositoryFactory.class);
        mongoRepository = mock(VitamMongoRepository.class);
        esRepository = mock(VitamElasticsearchRepository.class);
        when(vitamRepositoryProvider.getVitamMongoRepository(any())).thenReturn(mongoRepository);
        when(vitamRepositoryProvider.getVitamESRepository(any(), any())).thenReturn(esRepository);

        restoreBackupService = mock(RestoreBackupService.class);

        requestItem = new ReconstructionRequestItem();
        requestItem.setTenant(TENANT).setLimit(100);

        offsetRepository = mock(OffsetRepository.class);
        indexManager = mock(ElasticsearchLogbookIndexManager.class);
    }

    @RunWithCustomExecutor
    @Test
    public void should_return_new_offset_when_item_unit_is_ok()
        throws Exception {
        // given
        when(offsetRepository.findOffsetBy(TENANT, VitamConfiguration.getDefaultStrategy(), LOGBOOK)).thenReturn(
            OFFSET);

        when(restoreBackupService.getListing(VitamConfiguration.getDefaultStrategy(), OFFSET,
            requestItem.getLimit()))
            .thenReturn(IteratorUtils.singletonIterator(Arrays.asList(getOfferLog(100), getOfferLog(101))));
        when(restoreBackupService.loadData(VitamConfiguration.getDefaultStrategy(), "100", OFFSET))
            .thenReturn(getLogbookBackupModel("100", OFFSET));
        when(restoreBackupService.loadData(VitamConfiguration.getDefaultStrategy(), "101", NEXT_OFFSET))
            .thenReturn(getLogbookBackupModel("101", NEXT_OFFSET));

        ReconstructionService reconstructionService =
            new ReconstructionService(vitamRepositoryProvider, restoreBackupService,
                new LogbookTransformData(), offsetRepository, indexManager);
        // when
        ReconstructionResponseItem realResponseItem = reconstructionService.reconstruct(requestItem);
        // then
        assertThat(realResponseItem).isNotNull();
        verify(offsetRepository).createOrUpdateOffset(TENANT, VitamConfiguration.getDefaultStrategy(), LOGBOOK,
            NEXT_OFFSET);
        assertThat(realResponseItem.getTenant()).isEqualTo(TENANT);
        assertThat(realResponseItem.getStatus()).isEqualTo(StatusCode.OK);
    }

    @RunWithCustomExecutor
    @Test
    public void should_fail_when_item_unit_is_missing()
        throws Exception {
        // given
        when(offsetRepository.findOffsetBy(TENANT, VitamConfiguration.getDefaultStrategy(), LOGBOOK)).thenReturn(
            OFFSET);

        when(restoreBackupService.getListing(VitamConfiguration.getDefaultStrategy(), OFFSET,
            requestItem.getLimit()))
            .thenReturn(IteratorUtils.singletonIterator(Arrays.asList(getOfferLog(100), getOfferLog(101))));
        when(restoreBackupService.loadData(VitamConfiguration.getDefaultStrategy(), "100", OFFSET))
            .thenThrow(new StorageNotFoundException(""));
        when(restoreBackupService.loadData(VitamConfiguration.getDefaultStrategy(), "101", NEXT_OFFSET))
            .thenReturn(getLogbookBackupModel("101", NEXT_OFFSET));

        ReconstructionService reconstructionService =
            new ReconstructionService(vitamRepositoryProvider, restoreBackupService,
                new LogbookTransformData(), offsetRepository, indexManager);
        // when
        ReconstructionResponseItem realResponseItem = reconstructionService.reconstruct(requestItem);
        // then
        assertThat(realResponseItem).isNotNull();
        verify(offsetRepository, times(0)).createOrUpdateOffset(eq(TENANT), eq(VitamConfiguration.getDefaultStrategy()),
            eq(LOGBOOK), anyLong());
        assertThat(realResponseItem.getTenant()).isEqualTo(TENANT);
        assertThat(realResponseItem.getStatus()).isEqualTo(StatusCode.KO);
    }

    @RunWithCustomExecutor
    @Test
    public void should_return_request_offset_when_item_limit_zero()
        throws DatabaseException, StorageServerClientException, StorageNotFoundClientException {
        // given
        when(offsetRepository.findOffsetBy(TENANT, VitamConfiguration.getDefaultStrategy(), LOGBOOK)).thenReturn(
            OFFSET);

        requestItem.setLimit(0);
        when(restoreBackupService.getListing(VitamConfiguration.getDefaultStrategy(), OFFSET,
            requestItem.getLimit())).thenReturn(IteratorUtils.emptyIterator());

        ReconstructionService reconstructionService =
            new ReconstructionService(vitamRepositoryProvider, restoreBackupService,
                new LogbookTransformData(), offsetRepository, indexManager);
        // when
        ReconstructionResponseItem realResponseItem = reconstructionService.reconstruct(requestItem);
        // then
        assertThat(realResponseItem).isNotNull();
        // we don't update offset if there no data to reconstruct
        verify(offsetRepository, times(0)).createOrUpdateOffset(eq(TENANT), eq(VitamConfiguration.getDefaultStrategy()),
            eq(LOGBOOK), anyLong());
        assertThat(realResponseItem.getTenant()).isEqualTo(TENANT);
        assertThat(realResponseItem.getStatus()).isEqualTo(StatusCode.OK);
    }

    @RunWithCustomExecutor
    @Test
    public void should_throw_IllegalArgumentException_when_item_is_negative() {
        // given
        requestItem.setLimit(-5);
        ReconstructionService reconstructionService =
            new ReconstructionService(vitamRepositoryProvider, restoreBackupService,
                new LogbookTransformData(), offsetRepository, indexManager);
        // when + then
        assertThatCode(() -> reconstructionService.reconstruct(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @RunWithCustomExecutor
    @Test
    public void should_throw_IllegalArgumentException_when_item_is_null() {
        // given
        ReconstructionService reconstructionService =
            new ReconstructionService(vitamRepositoryProvider, restoreBackupService,
                new LogbookTransformData(), offsetRepository, indexManager);
        // when + then
        assertThatCode(() -> reconstructionService.reconstruct(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @RunWithCustomExecutor
    @Test
    public void should_throw_IllegalArgumentException_when_item_tenant_is_null() {
        // given
        ReconstructionService reconstructionService =
            new ReconstructionService(vitamRepositoryProvider, restoreBackupService,
                new LogbookTransformData(), offsetRepository, indexManager);
        // when + then
        assertThatCode(() -> reconstructionService.reconstruct(requestItem.setTenant(null)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @RunWithCustomExecutor
    @Test
    public void should_stop_processing_when_mongo_exception()
        throws Exception {
        // given
        when(offsetRepository.findOffsetBy(TENANT, VitamConfiguration.getDefaultStrategy(), LOGBOOK)).thenReturn(
            OFFSET);

        when(restoreBackupService.getListing(VitamConfiguration.getDefaultStrategy(), OFFSET, requestItem.getLimit()))
            .thenReturn(IteratorUtils.singletonIterator(Arrays.asList(getOfferLog(100), getOfferLog(101))));
        when(restoreBackupService.loadData(VitamConfiguration.getDefaultStrategy(), "100", OFFSET))
            .thenReturn(getLogbookBackupModel("100", OFFSET));
        when(restoreBackupService.loadData(VitamConfiguration.getDefaultStrategy(), "101", NEXT_OFFSET))
            .thenReturn(getLogbookBackupModel("101", NEXT_OFFSET));
        doThrow(new DatabaseException("mongo error")).when(mongoRepository).saveOrUpdate(anyList());

        ReconstructionService reconstructionService =
            new ReconstructionService(vitamRepositoryProvider, restoreBackupService,
                new LogbookTransformData(), offsetRepository, indexManager);
        // when
        ReconstructionResponseItem realResponseItem = reconstructionService.reconstruct(requestItem);
        // then
        assertThat(realResponseItem).isNotNull();
        verify(offsetRepository, times(0)).createOrUpdateOffset(eq(TENANT), eq(VitamConfiguration.getDefaultStrategy()),
            eq(LOGBOOK), anyLong());
        assertThat(realResponseItem.getTenant()).isEqualTo(TENANT);
        assertThat(realResponseItem.getStatus()).isEqualTo(StatusCode.KO);
    }

    @RunWithCustomExecutor
    @Test
    public void should_stop_processing_when_es_exception()
        throws Exception {
        // given
        when(offsetRepository.findOffsetBy(TENANT, VitamConfiguration.getDefaultStrategy(), LOGBOOK)).thenReturn(
            OFFSET);
        when(restoreBackupService.getListing(VitamConfiguration.getDefaultStrategy(), OFFSET, requestItem.getLimit()))
            .thenReturn(IteratorUtils.singletonIterator(Arrays.asList(getOfferLog(100), getOfferLog(101))));
        when(restoreBackupService.loadData(VitamConfiguration.getDefaultStrategy(), "100", OFFSET))
            .thenReturn(getLogbookBackupModel("100", OFFSET));
        when(restoreBackupService.loadData(VitamConfiguration.getDefaultStrategy(), "101", NEXT_OFFSET))
            .thenReturn(getLogbookBackupModel("101", NEXT_OFFSET));
        doThrow(new DatabaseException("mongo error")).when(esRepository).save(anyList());

        ReconstructionService reconstructionService =
            new ReconstructionService(vitamRepositoryProvider, restoreBackupService,
                new LogbookTransformData(), offsetRepository, indexManager);
        // when
        ReconstructionResponseItem realResponseItem = reconstructionService.reconstruct(requestItem);
        // then
        assertThat(realResponseItem).isNotNull();
        verify(offsetRepository, times(0)).createOrUpdateOffset(eq(TENANT), eq(VitamConfiguration.getDefaultStrategy()),
            eq(LOGBOOK), anyLong());
        assertThat(realResponseItem.getTenant()).isEqualTo(TENANT);
        assertThat(realResponseItem.getStatus()).isEqualTo(StatusCode.KO);
    }


    @RunWithCustomExecutor
    @Test
    public void should_return_request_offset_when_logbook_null()
        throws Exception {
        // given
        when(offsetRepository.findOffsetBy(TENANT, VitamConfiguration.getDefaultStrategy(), LOGBOOK)).thenReturn(
            OFFSET);
        LogbookBackupModel logbookBackupModel100 = getLogbookBackupModel("100", OFFSET);
        logbookBackupModel100.setLogbookOperation(null);
        when(restoreBackupService.getListing(VitamConfiguration.getDefaultStrategy(), OFFSET,
            requestItem.getLimit()))
            .thenReturn(IteratorUtils.singletonIterator(Arrays.asList(getOfferLog(100), getOfferLog(101))));
        when(restoreBackupService.loadData(VitamConfiguration.getDefaultStrategy(), "100", OFFSET))
            .thenReturn(logbookBackupModel100);
        when(restoreBackupService.loadData(VitamConfiguration.getDefaultStrategy(), "101", NEXT_OFFSET))
            .thenReturn(getLogbookBackupModel("101", NEXT_OFFSET));
        ReconstructionService reconstructionService =
            new ReconstructionService(vitamRepositoryProvider, restoreBackupService,
                new LogbookTransformData(), offsetRepository, indexManager);
        // when
        ReconstructionResponseItem realResponseItem = reconstructionService.reconstruct(requestItem);
        // then
        assertThat(realResponseItem).isNotNull();
        // we don't update offset if there no data to reconstruct
        verify(offsetRepository, times(0)).createOrUpdateOffset(eq(TENANT), eq(VitamConfiguration.getDefaultStrategy()),
            eq(LOGBOOK), anyLong());
        assertThat(realResponseItem.getTenant()).isEqualTo(TENANT);
        assertThat(realResponseItem.getStatus()).isEqualTo(StatusCode.KO);
    }

    private LogbookBackupModel getLogbookBackupModel(String id, Long offset) {
        LogbookBackupModel model = new LogbookBackupModel();
        model.setLogbookOperation(new Document("_id", id));
        model.setLogbookId(id);
        model.setOffset(offset);
        return model;
    }

    private OfferLog getOfferLog(long sequence) {
        OfferLog offerLog = new OfferLog("container", "" + sequence, OfferLogAction.WRITE);
        offerLog.setSequence(sequence);
        return offerLog;
    }

}
