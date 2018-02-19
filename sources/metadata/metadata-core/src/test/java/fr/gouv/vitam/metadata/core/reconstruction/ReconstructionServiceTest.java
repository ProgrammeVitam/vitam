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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.Arrays;
import java.util.List;

import org.bson.Document;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import fr.gouv.vitam.common.database.api.impl.VitamElasticsearchRepository;
import fr.gouv.vitam.common.database.api.impl.VitamMongoRepository;
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
import fr.gouv.vitam.storage.engine.common.model.OfferLog;

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

    @Before
    public void setup() {
        vitamRepositoryProvider = Mockito.mock(VitamRepositoryFactory.class);
        mongoRepository = Mockito.mock(VitamMongoRepository.class);
        esRepository = Mockito.mock(VitamElasticsearchRepository.class);
        Mockito.when(vitamRepositoryProvider.getVitamMongoRepository(Mockito.any())).thenReturn(mongoRepository);
        Mockito.when(vitamRepositoryProvider.getVitamESRepository(Mockito.any())).thenReturn(esRepository);

        restoreBackupService = Mockito.mock(RestoreBackupService.class);
        logbookLifecycleClientFactory = Mockito.mock(LogbookLifeCyclesClientFactory.class);
        logbookLifecycleClient = Mockito.mock(LogbookLifeCyclesClient.class);
        Mockito.when(logbookLifecycleClientFactory.getClient()).thenReturn(logbookLifecycleClient);

        requestItem = new ReconstructionRequestItem();
        requestItem.setCollection("Unit").setTenant(10).setLimit(100).setOffset(100);
    }

    @RunWithCustomExecutor
    @Test
    public void should_return_new_offset_when_item_unit_is_ok()
        throws DatabaseException, LogbookClientBadRequestException, LogbookClientServerException {
        // given
        Mockito.when(restoreBackupService.getListing("default", MetadataCollections.UNIT, requestItem.getOffset(),
            requestItem.getLimit())).thenReturn(Arrays.asList(Arrays.asList(getOfferLog(100), getOfferLog(101))));
        Mockito.when(restoreBackupService.loadData("default", MetadataCollections.UNIT, "100", 100L))
            .thenReturn(getUnitMetadataBackupModel("100", 100L));
        Mockito.when(restoreBackupService.loadData("default", MetadataCollections.UNIT, "101", 101L))
            .thenReturn(getUnitMetadataBackupModel("101", 101L));
        Mockito.doNothing().when(logbookLifecycleClient).createRawbulkUnitlifecycles(Mockito.any());

        ReconstructionService reconstructionService =
            new ReconstructionService(vitamRepositoryProvider, restoreBackupService, logbookLifecycleClientFactory);
        // when
        ReconstructionResponseItem realResponseItem = reconstructionService.reconstruct(requestItem);
        // then
        assertThat(realResponseItem).isNotNull();
        assertThat(realResponseItem.getCollection()).isEqualTo(MetadataCollections.UNIT.name());
        assertThat(realResponseItem.getOffset()).isEqualTo(101);
        assertThat(realResponseItem.getTenant()).isEqualTo(10);
        assertThat(realResponseItem.getStatus()).isEqualTo(StatusCode.OK);
    }


    @RunWithCustomExecutor
    @Test
    public void should_return_new_offset_when_item_got_is_ok()
        throws DatabaseException, LogbookClientBadRequestException, LogbookClientServerException {
        // given
        requestItem.setCollection("ObjectGroup");
        Mockito
            .when(restoreBackupService.getListing("default", MetadataCollections.OBJECTGROUP, requestItem.getOffset(),
                requestItem.getLimit()))
            .thenReturn(Arrays.asList(Arrays.asList(getOfferLog(100), getOfferLog(101))));
        Mockito.when(restoreBackupService.loadData("default", MetadataCollections.OBJECTGROUP, "100", 100L))
            .thenReturn(getGotMetadataBackupModel("100", 100L));
        Mockito.when(restoreBackupService.loadData("default", MetadataCollections.OBJECTGROUP, "101", 101L))
            .thenReturn(getGotMetadataBackupModel("101", 101L));
        Mockito.doNothing().when(logbookLifecycleClient).createRawbulkObjectgrouplifecycles(Mockito.any());

        ReconstructionService reconstructionService =
            new ReconstructionService(vitamRepositoryProvider, restoreBackupService, logbookLifecycleClientFactory);
        // when
        ReconstructionResponseItem realResponseItem = reconstructionService.reconstruct(requestItem);
        // then
        assertThat(realResponseItem).isNotNull();
        assertThat(realResponseItem.getCollection()).isEqualTo(MetadataCollections.OBJECTGROUP.name());
        assertThat(realResponseItem.getOffset()).isEqualTo(101L);
        assertThat(realResponseItem.getTenant()).isEqualTo(10);
        assertThat(realResponseItem.getStatus()).isEqualTo(StatusCode.OK);
    }

    @RunWithCustomExecutor
    @Test
    public void should_return_request_offset_when_item_limit_zero() throws DatabaseException {
        // given
        requestItem.setLimit(0);
        Mockito.when(restoreBackupService.getListing("default", MetadataCollections.UNIT, requestItem.getOffset(),
            requestItem.getLimit())).thenReturn(Arrays.asList());

        ReconstructionService reconstructionService =
            new ReconstructionService(vitamRepositoryProvider, restoreBackupService, logbookLifecycleClientFactory);
        // when
        ReconstructionResponseItem realResponseItem = reconstructionService.reconstruct(requestItem);
        // then
        assertThat(realResponseItem).isNotNull();
        assertThat(realResponseItem.getCollection()).isEqualTo(MetadataCollections.UNIT.name());
        assertThat(realResponseItem.getOffset()).isEqualTo(100L);
        assertThat(realResponseItem.getTenant()).isEqualTo(10);
        assertThat(realResponseItem.getStatus()).isEqualTo(StatusCode.OK);
    }

    @RunWithCustomExecutor
    @Test
    public void should_throw_IllegalArgumentException_when_item_is_negative() {
        // given
        requestItem.setLimit(-5);
        ReconstructionService reconstructionService =
            new ReconstructionService(vitamRepositoryProvider, restoreBackupService, logbookLifecycleClientFactory);
        // when + then
        assertThatCode(() -> reconstructionService.reconstruct(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @RunWithCustomExecutor
    @Test
    public void should_throw_IllegalArgumentException_when_item_is_null() {
        // given
        ReconstructionService reconstructionService =
            new ReconstructionService(vitamRepositoryProvider, restoreBackupService, logbookLifecycleClientFactory);
        // when + then
        assertThatCode(() -> reconstructionService.reconstruct(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @RunWithCustomExecutor
    @Test
    public void should_throw_IllegalArgumentException_when_item_collection_is_null() {
        // given
        ReconstructionService reconstructionService =
            new ReconstructionService(vitamRepositoryProvider, restoreBackupService, logbookLifecycleClientFactory);
        // when + then
        assertThatCode(() -> reconstructionService.reconstruct(requestItem.setCollection(null)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @RunWithCustomExecutor
    @Test
    public void should_throw_IllegalArgumentException_when_item_collection_is_invalid() {
        // given
        ReconstructionService reconstructionService =
            new ReconstructionService(vitamRepositoryProvider, restoreBackupService, logbookLifecycleClientFactory);
        // when + then
        assertThatCode(() -> reconstructionService.reconstruct(requestItem.setCollection("toto")))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @RunWithCustomExecutor
    @Test
    public void should_throw_IllegalArgumentException_when_item_tenant_is_null() {
        // given
        ReconstructionService reconstructionService =
            new ReconstructionService(vitamRepositoryProvider, restoreBackupService, logbookLifecycleClientFactory);
        // when + then
        assertThatCode(() -> reconstructionService.reconstruct(requestItem.setTenant(null)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @RunWithCustomExecutor
    @Test
    public void should_return_request_offset_when_mongo_exception()
        throws DatabaseException, LogbookClientBadRequestException, LogbookClientServerException {
        // given
        Mockito.when(restoreBackupService.getListing("default", MetadataCollections.UNIT, requestItem.getOffset(),
            requestItem.getLimit())).thenReturn(Arrays.asList(Arrays.asList(getOfferLog(100), getOfferLog(101))));
        Mockito.when(restoreBackupService.loadData("default", MetadataCollections.UNIT, "100", 100L))
            .thenReturn(getUnitMetadataBackupModel("100", 100L));
        Mockito.when(restoreBackupService.loadData("default", MetadataCollections.UNIT, "101", 101L))
            .thenReturn(getUnitMetadataBackupModel("101", 101L));
        Mockito.doNothing().when(logbookLifecycleClient).createRawbulkUnitlifecycles(Mockito.any());
        Mockito.doThrow(new DatabaseException("mongo error")).when(mongoRepository).save(Mockito.any(List.class));

        ReconstructionService reconstructionService =
            new ReconstructionService(vitamRepositoryProvider, restoreBackupService, logbookLifecycleClientFactory);
        // when
        ReconstructionResponseItem realResponseItem = reconstructionService.reconstruct(requestItem);
        // then
        assertThat(realResponseItem).isNotNull();
        assertThat(realResponseItem.getCollection()).isEqualTo(MetadataCollections.UNIT.name());
        assertThat(realResponseItem.getOffset()).isEqualTo(101L);
        assertThat(realResponseItem.getTenant()).isEqualTo(10);
        assertThat(realResponseItem.getStatus()).isEqualTo(StatusCode.OK);
    }


    @RunWithCustomExecutor
    @Test
    public void should_return_request_offset_when_es_exception()
        throws DatabaseException, LogbookClientBadRequestException, LogbookClientServerException {
        // given
        Mockito.when(restoreBackupService.getListing("default", MetadataCollections.UNIT, requestItem.getOffset(),
            requestItem.getLimit())).thenReturn(Arrays.asList(Arrays.asList(getOfferLog(100), getOfferLog(101))));
        Mockito.when(restoreBackupService.loadData("default", MetadataCollections.UNIT, "100", 100L))
            .thenReturn(getUnitMetadataBackupModel("100", 100L));
        Mockito.when(restoreBackupService.loadData("default", MetadataCollections.UNIT, "101", 101L))
            .thenReturn(getUnitMetadataBackupModel("101", 101L));
        Mockito.doNothing().when(logbookLifecycleClient).createRawbulkUnitlifecycles(Mockito.any());
        Mockito.doThrow(new DatabaseException("mongo error")).when(esRepository).save(Mockito.any(List.class));

        ReconstructionService reconstructionService =
            new ReconstructionService(vitamRepositoryProvider, restoreBackupService, logbookLifecycleClientFactory);
        // when
        ReconstructionResponseItem realResponseItem = reconstructionService.reconstruct(requestItem);
        // then
        assertThat(realResponseItem).isNotNull();
        assertThat(realResponseItem.getCollection()).isEqualTo(MetadataCollections.UNIT.name());
        assertThat(realResponseItem.getOffset()).isEqualTo(100L);
        assertThat(realResponseItem.getTenant()).isEqualTo(10);
        assertThat(realResponseItem.getStatus()).isEqualTo(StatusCode.KO);
    }

    @RunWithCustomExecutor
    @Test
    public void should_return_request_offset_when_logbook_exception()
        throws DatabaseException, LogbookClientBadRequestException, LogbookClientServerException {
        // given
        Mockito.when(restoreBackupService.getListing("default", MetadataCollections.UNIT, requestItem.getOffset(),
            requestItem.getLimit())).thenReturn(Arrays.asList(Arrays.asList(getOfferLog(100), getOfferLog(101))));
        Mockito.when(restoreBackupService.loadData("default", MetadataCollections.UNIT, "100", 100L))
            .thenReturn(getUnitMetadataBackupModel("100", 100L));
        Mockito.when(restoreBackupService.loadData("default", MetadataCollections.UNIT, "101", 101L))
            .thenReturn(getUnitMetadataBackupModel("101", 101L));
        Mockito.doThrow(new LogbookClientServerException("logbook error"))
            .when(logbookLifecycleClient)
            .createRawbulkUnitlifecycles(Mockito.any());


        ReconstructionService reconstructionService =
            new ReconstructionService(vitamRepositoryProvider, restoreBackupService, logbookLifecycleClientFactory);
        // when
        ReconstructionResponseItem realResponseItem = reconstructionService.reconstruct(requestItem);
        // then
        assertThat(realResponseItem).isNotNull();
        assertThat(realResponseItem.getCollection()).isEqualTo(MetadataCollections.UNIT.name());
        assertThat(realResponseItem.getOffset()).isEqualTo(100L);
        assertThat(realResponseItem.getTenant()).isEqualTo(10);
        assertThat(realResponseItem.getStatus()).isEqualTo(StatusCode.KO);
    }

    @RunWithCustomExecutor
    @Test
    public void should_return_request_offset_when_lifecycle_null()
        throws DatabaseException, LogbookClientBadRequestException, LogbookClientServerException {
        // given
        MetadataBackupModel metadataBackupModel100 = getUnitMetadataBackupModel("100", 100L);
        metadataBackupModel100.setLifecycle(null);
        Mockito.when(restoreBackupService.getListing("default", MetadataCollections.UNIT, requestItem.getOffset(),
            requestItem.getLimit())).thenReturn(Arrays.asList(Arrays.asList(getOfferLog(100), getOfferLog(101))));
        Mockito.when(restoreBackupService.loadData("default", MetadataCollections.UNIT, "100", 100L))
            .thenReturn(metadataBackupModel100);
        Mockito.when(restoreBackupService.loadData("default", MetadataCollections.UNIT, "101", 101L))
            .thenReturn(getUnitMetadataBackupModel("101", 101L));
        Mockito.doThrow(new LogbookClientServerException("logbook error"))
            .when(logbookLifecycleClient)
            .createRawbulkUnitlifecycles(Mockito.any());


        ReconstructionService reconstructionService =
            new ReconstructionService(vitamRepositoryProvider, restoreBackupService, logbookLifecycleClientFactory);
        // when
        ReconstructionResponseItem realResponseItem = reconstructionService.reconstruct(requestItem);
        // then
        assertThat(realResponseItem).isNotNull();
        assertThat(realResponseItem.getCollection()).isEqualTo(MetadataCollections.UNIT.name());
        assertThat(realResponseItem.getOffset()).isEqualTo(100L);
        assertThat(realResponseItem.getTenant()).isEqualTo(10);
        assertThat(realResponseItem.getStatus()).isEqualTo(StatusCode.KO);
    }

    @RunWithCustomExecutor
    @Test
    public void should_return_request_offset_when_metadata_null()
        throws DatabaseException, LogbookClientBadRequestException, LogbookClientServerException {
        // given
        MetadataBackupModel metadataBackupModel100 = getUnitMetadataBackupModel("100", 100L);
        metadataBackupModel100.setUnit(null);
        Mockito.when(restoreBackupService.getListing("default", MetadataCollections.UNIT, requestItem.getOffset(),
            requestItem.getLimit())).thenReturn(Arrays.asList(Arrays.asList(getOfferLog(100), getOfferLog(101))));
        Mockito.when(restoreBackupService.loadData("default", MetadataCollections.UNIT, "100", 100L))
            .thenReturn(metadataBackupModel100);
        Mockito.when(restoreBackupService.loadData("default", MetadataCollections.UNIT, "101", 101L))
            .thenReturn(getUnitMetadataBackupModel("101", 101L));

        ReconstructionService reconstructionService =
            new ReconstructionService(vitamRepositoryProvider, restoreBackupService, logbookLifecycleClientFactory);
        // when
        ReconstructionResponseItem realResponseItem = reconstructionService.reconstruct(requestItem);
        // then
        assertThat(realResponseItem).isNotNull();
        assertThat(realResponseItem.getCollection()).isEqualTo(MetadataCollections.UNIT.name());
        assertThat(realResponseItem.getOffset()).isEqualTo(100L);
        assertThat(realResponseItem.getTenant()).isEqualTo(10);
        assertThat(realResponseItem.getStatus()).isEqualTo(StatusCode.KO);
    }

    @RunWithCustomExecutor
    @Test
    public void should_return_request_offset_when_loading_data_return_null()
        throws DatabaseException, LogbookClientBadRequestException, LogbookClientServerException {
        // given
        Mockito.when(restoreBackupService.getListing("default", MetadataCollections.UNIT, requestItem.getOffset(),
            requestItem.getLimit())).thenReturn(Arrays.asList(Arrays.asList(getOfferLog(100), getOfferLog(101))));
        Mockito.when(restoreBackupService.loadData("default", MetadataCollections.UNIT, "100", 100L))
            .thenReturn(getUnitMetadataBackupModel("100", 100L));
        Mockito.when(restoreBackupService.loadData("default", MetadataCollections.UNIT, "101", 101L)).thenReturn(null);
        Mockito.doNothing().when(logbookLifecycleClient).createRawbulkUnitlifecycles(Mockito.any());

        ReconstructionService reconstructionService =
            new ReconstructionService(vitamRepositoryProvider, restoreBackupService, logbookLifecycleClientFactory);
        // when
        ReconstructionResponseItem realResponseItem = reconstructionService.reconstruct(requestItem);
        // then
        assertThat(realResponseItem).isNotNull();
        assertThat(realResponseItem.getCollection()).isEqualTo(MetadataCollections.UNIT.name());
        assertThat(realResponseItem.getOffset()).isEqualTo(100L);
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
