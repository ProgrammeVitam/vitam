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
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.stream.LongStream;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import com.google.common.collect.Lists;
import fr.gouv.vitam.common.VitamConfiguration;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.client.AbstractMockClient.FakeInboundResponse;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.Order;

/**
 * RestoreBackupService Test
 */
public class RestoreBackupServiceTest {

    private static final String STRATEGY_ID = "default";

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    private StorageClientFactory storageClientFactory;

    @Before
    public void setup() {
        storageClientFactory = Mockito.mock(StorageClientFactory.class);
        StorageClient storageClient = Mockito.mock(StorageClient.class);
        when(storageClientFactory.getClient()).thenReturn(storageClient);
    }

    @RunWithCustomExecutor
    @Test
    public void should_get_listing_when_listing_units_and_storage_returns_response_ok()
        throws StorageServerClientException {
        // given
        when(storageClientFactory.getClient().getOfferLogs(STRATEGY_ID, DataCategory.UNIT, 100L, 2, Order.ASC))
            .thenReturn(getListingOk(100L, 2L));
        RestoreBackupService restoreBackupService = new RestoreBackupService(storageClientFactory);
        // when
        List<OfferLog> res = restoreBackupService.getListing(STRATEGY_ID, DataCategory.UNIT, 100L, 2, Order.ASC);

        List<List<OfferLog>> listing = Lists.partition(res, VitamConfiguration.getRestoreBulkSize());

        // then
        assertThat(listing).isNotNull().isNotEmpty();
        assertThat(listing.size()).isEqualTo(1);
        assertThat(listing.get(0)).isNotNull().isNotEmpty();
        assertThat(listing.get(0).size()).isEqualTo(2);
        assertThat(listing.get(0).get(0).getFileName()).isEqualTo("100");
        assertThat(listing.get(0).get(1).getFileName()).isEqualTo("101");
    }


    @RunWithCustomExecutor
    @Test
    public void should_get_latest_listing_when_listing_units_and_storage_returns_response_ok()
        throws StorageServerClientException {
        // given
        when(storageClientFactory.getClient().getOfferLogs(STRATEGY_ID, DataCategory.UNIT, null, 1, Order.DESC))
            .thenReturn(getListingOk(0L, 1L));
        RestoreBackupService restoreBackupService = new RestoreBackupService(storageClientFactory);
        // when
        List<OfferLog> listing = restoreBackupService.getListing(STRATEGY_ID, DataCategory.UNIT, null, 1, Order.DESC);
        // then
        assertThat(listing).isNotNull().isNotEmpty();
        assertThat(listing.size()).isEqualTo(1);
        assertThat(listing.get(0)).isNotNull();
        assertThat(listing.get(0).getFileName()).isEqualTo("0");
    }

    @RunWithCustomExecutor
    @Test
    public void should_get_listing_when_listing_gots_and_storage_returns_response_ok()
        throws StorageServerClientException {
        // given
        when(storageClientFactory.getClient().getOfferLogs(STRATEGY_ID, DataCategory.OBJECTGROUP, 100L, 2,
                Order.ASC))
            .thenReturn(getListingOk(100L, 2L));
        RestoreBackupService restoreBackupService = new RestoreBackupService(storageClientFactory);
        // when
        List<OfferLog> res =
            restoreBackupService.getListing(STRATEGY_ID, DataCategory.OBJECTGROUP, 100L, 2, Order.ASC);

        List<List<OfferLog>> listing = Lists.partition(res, VitamConfiguration.getRestoreBulkSize());


        // then
        assertThat(listing).isNotNull().isNotEmpty();
        assertThat(listing.size()).isEqualTo(1);
        assertThat(listing.get(0)).isNotNull().isNotEmpty();
        assertThat(listing.get(0).size()).isEqualTo(2);
        assertThat(listing.get(0).get(0).getFileName()).isEqualTo("100");
        assertThat(listing.get(0).get(1).getFileName()).isEqualTo("101");
    }

    @RunWithCustomExecutor
    @Test
    public void should_get_empty_listing_when_listing_units_and_storage_returns_empty_response_ok()
        throws StorageServerClientException {
        // given
        when(storageClientFactory.getClient().getOfferLogs(STRATEGY_ID, DataCategory.UNIT, 100L, 2, Order.ASC))
            .thenReturn(getListingOk(100L, -1L));
        RestoreBackupService restoreBackupService = new RestoreBackupService(storageClientFactory);
        // when
        List<OfferLog> res = restoreBackupService.getListing(STRATEGY_ID, DataCategory.UNIT, 100L, 2, Order.ASC);
        List<List<OfferLog>> listing = Lists.partition(res, VitamConfiguration.getRestoreBulkSize());
        // then
        assertThat(listing).isNotNull().isEmpty();
    }

    @RunWithCustomExecutor
    @Test
    public void should_throw_VitamRuntimeException_when_listing_and_storage_returns_VitamError()
        throws StorageServerClientException {
        // given
        when(storageClientFactory.getClient().getOfferLogs(STRATEGY_ID, DataCategory.UNIT, 100L, 2, Order.ASC))
            .thenReturn(new VitamError("test"));
        RestoreBackupService restoreBackupService = new RestoreBackupService(storageClientFactory);
        // when + then
        assertThatCode(() -> restoreBackupService.getListing(STRATEGY_ID, DataCategory.UNIT, 100L, 2, Order.ASC))
            .isInstanceOf(VitamRuntimeException.class);
    }

    @RunWithCustomExecutor
    @Test
    public void should_throw_VitamRuntimeException_when_listing_and_storage_throws_StorageServerClientException()
        throws StorageServerClientException {
        // given
        when(storageClientFactory.getClient().getOfferLogs(STRATEGY_ID, DataCategory.UNIT, 100L, 2, Order.ASC))
            .thenThrow(new StorageServerClientException("storage error"));
        RestoreBackupService restoreBackupService = new RestoreBackupService(storageClientFactory);
        // when + then
        assertThatCode(() -> restoreBackupService.getListing(STRATEGY_ID, DataCategory.UNIT, 100L, 2, Order.ASC))
            .isInstanceOf(VitamRuntimeException.class);
    }

    @RunWithCustomExecutor
    @Test
    public void should_get_unit_model_when_loading_unit_and_storage_returns_file()
        throws StorageServerClientException, StorageNotFoundException, FileNotFoundException {
        // given
        when(storageClientFactory.getClient().getContainerAsync(STRATEGY_ID, "100.json", DataCategory.UNIT))
            .thenReturn(
                new FakeInboundResponse(Status.OK, PropertiesUtils.getResourceAsStream("reconstruction_unit.json"),
                    MediaType.APPLICATION_OCTET_STREAM_TYPE, null));
        RestoreBackupService restoreBackupService = new RestoreBackupService(storageClientFactory);
        // when
        MetadataBackupModel model =
            restoreBackupService.loadData(STRATEGY_ID, MetadataCollections.UNIT, "100.json", 100L);
        // then
        assertThat(model).isNotNull();
        assertThat(model.getMetadatas()).isNotNull();
        assertThat(model.getMetadatas().get("_id")).isEqualTo("aeaqaaaaaaft45swaaxg2albfwxhlfiaaaba");
        assertThat(model.getLifecycle()).isNotNull();
        assertThat(model.getMetadatas().get("_id")).isEqualTo("aeaqaaaaaaft45swaaxg2albfwxhlfiaaaba");
        assertThat(model.getOffset()).isEqualTo(100L);
    }

    @RunWithCustomExecutor
    @Test
    public void should_get_got_model_when_loading_got_and_storage_returns_file()
        throws StorageServerClientException, StorageNotFoundException, FileNotFoundException {
        // given
        when(storageClientFactory.getClient().getContainerAsync(STRATEGY_ID, "100.json", DataCategory.OBJECTGROUP))
            .thenReturn(
                new FakeInboundResponse(Status.OK, PropertiesUtils.getResourceAsStream("reconstruction_got.json"),
                    MediaType.APPLICATION_OCTET_STREAM_TYPE, null));
        RestoreBackupService restoreBackupService = new RestoreBackupService(storageClientFactory);
        // when
        MetadataBackupModel model =
            restoreBackupService.loadData(STRATEGY_ID, MetadataCollections.OBJECTGROUP, "100.json", 100L);
        // then
        assertThat(model).isNotNull();
        assertThat(model.getMetadatas()).isNotNull();
        assertThat(model.getMetadatas().get("_id")).isEqualTo("aebaaaaaaaft45swaaxg2albfzawqoaaaaaq");
        assertThat(model.getLifecycle()).isNotNull();
        assertThat(model.getMetadatas().get("_id")).isEqualTo("aebaaaaaaaft45swaaxg2albfzawqoaaaaaq");
        assertThat(model.getOffset()).isEqualTo(100L);
    }

    @RunWithCustomExecutor
    @Test
    public void should_get_null_when_loading_and_storage_returns_file_unit_without_metadata()
        throws StorageServerClientException, StorageNotFoundException, FileNotFoundException {
        // given
        when(storageClientFactory.getClient().getContainerAsync(STRATEGY_ID, "100.json", DataCategory.UNIT))
            .thenReturn(
                new FakeInboundResponse(Status.OK,
                    PropertiesUtils.getResourceAsStream("reconstruction_unit_no_metadata.json"),
                    MediaType.APPLICATION_OCTET_STREAM_TYPE, null));
        RestoreBackupService restoreBackupService = new RestoreBackupService(storageClientFactory);
        // when
        MetadataBackupModel model =
            restoreBackupService.loadData(STRATEGY_ID, MetadataCollections.UNIT, "100.json", 100L);
        // then
        assertThat(model).isNull();
    }

    @RunWithCustomExecutor
    @Test
    public void should_get_null_when_loading_and_storage_returns_file_unit_without_lfc()
        throws StorageServerClientException, StorageNotFoundException, FileNotFoundException {
        // given
        when(storageClientFactory.getClient().getContainerAsync(STRATEGY_ID, "100.json", DataCategory.UNIT))
            .thenReturn(
                new FakeInboundResponse(Status.OK,
                    PropertiesUtils.getResourceAsStream("reconstruction_unit_no_lfc.json"),
                    MediaType.APPLICATION_OCTET_STREAM_TYPE, null));
        RestoreBackupService restoreBackupService = new RestoreBackupService(storageClientFactory);
        // when
        MetadataBackupModel model =
            restoreBackupService.loadData(STRATEGY_ID, MetadataCollections.UNIT, "100.json", 100L);
        // then
        assertThat(model).isNull();
    }

    @RunWithCustomExecutor
    @Test
    public void should_throw_VitamRuntimeException_when_loading_and_storage_throws_StorageServerClientException()
        throws StorageServerClientException, StorageNotFoundException {
        // given
        when(storageClientFactory.getClient().getContainerAsync(STRATEGY_ID, "100.json", DataCategory.UNIT))
            .thenThrow(new StorageServerClientException("storage error"));
        RestoreBackupService restoreBackupService = new RestoreBackupService(storageClientFactory);
        // when + then
        assertThatCode(() -> restoreBackupService.loadData(STRATEGY_ID, MetadataCollections.UNIT, "100.json", 100L))
            .isInstanceOf(VitamRuntimeException.class);
    }

    @RunWithCustomExecutor
    @Test
    public void should_throw_VitamRuntimeException_when_loading_and_storage_returns_invalid_file()
        throws StorageServerClientException, StorageNotFoundException, FileNotFoundException {
        // given
        when(storageClientFactory.getClient().getContainerAsync(STRATEGY_ID, "100.json", DataCategory.UNIT))
            .thenReturn(
                new FakeInboundResponse(Status.OK, new ByteArrayInputStream("test".getBytes()),
                    MediaType.APPLICATION_OCTET_STREAM_TYPE, null));
        RestoreBackupService restoreBackupService = new RestoreBackupService(storageClientFactory);
        // when + then
        assertThatCode(() -> restoreBackupService.loadData(STRATEGY_ID, MetadataCollections.UNIT, "100.json", 100L))
            .isInstanceOf(VitamRuntimeException.class);
    }

    private RequestResponseOK<OfferLog> getListingOk(long offset, long limit) {
        RequestResponseOK<OfferLog> listing = new RequestResponseOK<>();
        listing.setHttpCode(Status.OK.getStatusCode());
        LongStream.range(offset, offset + limit).forEach(i -> {
            OfferLog offerLog = new OfferLog("container", String.valueOf(i), "write");
            listing.addResult(offerLog);
        });
        return listing;
    }
}
