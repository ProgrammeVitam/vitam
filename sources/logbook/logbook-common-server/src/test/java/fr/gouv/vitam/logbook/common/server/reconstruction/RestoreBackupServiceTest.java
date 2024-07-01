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

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.accesslog.AccessLogUtils;
import fr.gouv.vitam.common.client.AbstractMockClient.FakeInboundResponse;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.OfferLogAction;
import fr.gouv.vitam.storage.engine.common.model.Order;
import org.apache.commons.collections4.IteratorUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;
import java.io.ByteArrayInputStream;
import java.util.Iterator;
import java.util.List;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * RestoreBackupService Test
 */
public class RestoreBackupServiceTest {

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    private StorageClientFactory storageClientFactory;
    private static final String DEFAULT_OFFER = "default";

    @Before
    public void setup() {
        storageClientFactory = Mockito.mock(StorageClientFactory.class);
        StorageClient storageClient = Mockito.mock(StorageClient.class);
        when(storageClientFactory.getClient()).thenReturn(storageClient);
    }

    @RunWithCustomExecutor
    @Test
    public void should_get_listing_when_listing_units_and_storage_returns_response_ok()
        throws StorageServerClientException, StorageNotFoundClientException {
        // given
        when(
            storageClientFactory
                .getClient()
                .getOfferLogs(
                    eq(VitamConfiguration.getDefaultStrategy()),
                    eq(DEFAULT_OFFER),
                    eq(DataCategory.BACKUP_OPERATION),
                    eq(100L),
                    eq(2),
                    eq(Order.ASC)
                )
        ).thenReturn(getListingOk(100L, 2L));
        when(storageClientFactory.getClient().getReferentOffer(eq(VitamConfiguration.getDefaultStrategy()))).thenReturn(
            DEFAULT_OFFER
        );

        RestoreBackupService restoreBackupService = new RestoreBackupService(storageClientFactory);
        // when
        Iterator<List<OfferLog>> listingIterator = restoreBackupService.getListing(
            VitamConfiguration.getDefaultStrategy(),
            100L,
            2
        );
        // then
        List<List<OfferLog>> listing = IteratorUtils.toList(listingIterator);
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
        throws StorageServerClientException, StorageNotFoundClientException {
        // given
        when(
            storageClientFactory
                .getClient()
                .getOfferLogs(
                    eq(VitamConfiguration.getDefaultStrategy()),
                    eq(DEFAULT_OFFER),
                    eq(DataCategory.BACKUP_OPERATION),
                    eq(100L),
                    eq(2),
                    eq(Order.ASC)
                )
        ).thenReturn(getListingOk(100L, -1L));
        when(storageClientFactory.getClient().getReferentOffer(eq(VitamConfiguration.getDefaultStrategy()))).thenReturn(
            DEFAULT_OFFER
        );
        RestoreBackupService restoreBackupService = new RestoreBackupService(storageClientFactory);
        // when
        Iterator<List<OfferLog>> listing = restoreBackupService.getListing(
            VitamConfiguration.getDefaultStrategy(),
            100L,
            2
        );
        // then
        assertThat(listing).isNotNull().isEmpty();
    }

    @RunWithCustomExecutor
    @Test
    public void should_throw_VitamRuntimeException_when_listing_and_storage_returns_VitamError()
        throws StorageServerClientException, StorageNotFoundClientException {
        // given
        when(storageClientFactory.getClient().getReferentOffer(eq(VitamConfiguration.getDefaultStrategy()))).thenReturn(
            DEFAULT_OFFER
        );
        when(
            storageClientFactory
                .getClient()
                .getOfferLogs(
                    eq(VitamConfiguration.getDefaultStrategy()),
                    eq(DEFAULT_OFFER),
                    eq(DataCategory.BACKUP_OPERATION),
                    eq(100L),
                    eq(2),
                    eq(Order.ASC)
                )
        ).thenReturn(new VitamError("test"));
        RestoreBackupService restoreBackupService = new RestoreBackupService(storageClientFactory);
        // when + then
        Iterator<List<OfferLog>> listing = restoreBackupService.getListing(
            VitamConfiguration.getDefaultStrategy(),
            100L,
            2
        );
        assertThatCode(() -> IteratorUtils.toList(listing)).isInstanceOf(VitamRuntimeException.class);
    }

    @RunWithCustomExecutor
    @Test
    public void should_throw_VitamRuntimeException_when_listing_and_storage_throws_StorageServerClientException()
        throws StorageServerClientException, StorageNotFoundClientException {
        // given
        when(storageClientFactory.getClient().getReferentOffer(eq(VitamConfiguration.getDefaultStrategy()))).thenReturn(
            DEFAULT_OFFER
        );
        when(
            storageClientFactory
                .getClient()
                .getOfferLogs(
                    eq(VitamConfiguration.getDefaultStrategy()),
                    eq(DEFAULT_OFFER),
                    eq(DataCategory.BACKUP_OPERATION),
                    eq(100L),
                    eq(2),
                    eq(Order.ASC)
                )
        ).thenThrow(new StorageServerClientException("storage error"));
        RestoreBackupService restoreBackupService = new RestoreBackupService(storageClientFactory);
        // when + then
        Iterator<List<OfferLog>> listing = restoreBackupService.getListing(
            VitamConfiguration.getDefaultStrategy(),
            100L,
            2
        );
        assertThatCode(() -> IteratorUtils.toList(listing)).isInstanceOf(VitamRuntimeException.class);
    }

    @RunWithCustomExecutor
    @Test
    public void should_get_unit_model_when_loading_unit_and_storage_returns_file() throws Exception {
        // given
        when(storageClientFactory.getClient().getReferentOffer(eq(VitamConfiguration.getDefaultStrategy()))).thenReturn(
            DEFAULT_OFFER
        );
        when(
            storageClientFactory
                .getClient()
                .getContainerAsync(
                    VitamConfiguration.getDefaultStrategy(),
                    DEFAULT_OFFER,
                    "100.json",
                    DataCategory.BACKUP_OPERATION,
                    AccessLogUtils.getNoLogAccessLog()
                )
        ).thenReturn(
            new FakeInboundResponse(
                Status.OK,
                PropertiesUtils.getResourceAsStream("reconstruction_operation.json"),
                MediaType.APPLICATION_OCTET_STREAM_TYPE,
                null
            )
        );
        RestoreBackupService restoreBackupService = new RestoreBackupService(storageClientFactory);
        // when
        LogbookBackupModel model = restoreBackupService.loadData(
            VitamConfiguration.getDefaultStrategy(),
            "100.json",
            100L
        );
        // then
        assertThat(model).isNotNull();
        assertThat(model.getLogbookOperation()).isNotNull();
        assertThat(model.getLogbookOperation().get("_id")).isEqualTo("aeeaaaaaacgegs3taawbcalbwplatwaaaaaq");
        assertThat(model.getOffset()).isEqualTo(100L);
    }

    @RunWithCustomExecutor
    @Test
    public void should_get_null_when_loading_and_storage_returns_file_invalid() throws Exception {
        // given
        when(storageClientFactory.getClient().getReferentOffer(eq(VitamConfiguration.getDefaultStrategy()))).thenReturn(
            DEFAULT_OFFER
        );
        when(
            storageClientFactory
                .getClient()
                .getContainerAsync(
                    VitamConfiguration.getDefaultStrategy(),
                    DEFAULT_OFFER,
                    "100.json",
                    DataCategory.BACKUP_OPERATION,
                    AccessLogUtils.getNoLogAccessLog()
                )
        ).thenReturn(
            new FakeInboundResponse(
                Status.OK,
                PropertiesUtils.getResourceAsStream("reconstruction_operation_invalid.json"),
                MediaType.APPLICATION_OCTET_STREAM_TYPE,
                null
            )
        );
        RestoreBackupService restoreBackupService = new RestoreBackupService(storageClientFactory);

        // when + then
        assertThatCode(
            () -> restoreBackupService.loadData(VitamConfiguration.getDefaultStrategy(), "100.json", 100L)
        ).isInstanceOf(VitamRuntimeException.class);
    }

    @RunWithCustomExecutor
    @Test
    public void should_throw_VitamRuntimeException_when_loading_and_storage_throws_StorageServerClientException()
        throws Exception {
        // given
        when(storageClientFactory.getClient().getReferentOffer(eq(VitamConfiguration.getDefaultStrategy()))).thenReturn(
            DEFAULT_OFFER
        );
        when(
            storageClientFactory
                .getClient()
                .getContainerAsync(
                    VitamConfiguration.getDefaultStrategy(),
                    DEFAULT_OFFER,
                    "100.json",
                    DataCategory.BACKUP_OPERATION,
                    AccessLogUtils.getNoLogAccessLog()
                )
        ).thenThrow(new StorageServerClientException("storage error"));
        RestoreBackupService restoreBackupService = new RestoreBackupService(storageClientFactory);
        // when + then
        assertThatCode(
            () -> restoreBackupService.loadData(VitamConfiguration.getDefaultStrategy(), "100.json", 100L)
        ).isInstanceOf(VitamRuntimeException.class);
    }

    @RunWithCustomExecutor
    @Test
    public void should_throw_VitamRuntimeException_when_loading_and_storage_returns_invalid_file() throws Exception {
        // given
        when(storageClientFactory.getClient().getReferentOffer(eq(VitamConfiguration.getDefaultStrategy()))).thenReturn(
            DEFAULT_OFFER
        );
        when(
            storageClientFactory
                .getClient()
                .getContainerAsync(
                    VitamConfiguration.getDefaultStrategy(),
                    DEFAULT_OFFER,
                    "100.json",
                    DataCategory.BACKUP_OPERATION,
                    AccessLogUtils.getNoLogAccessLog()
                )
        ).thenReturn(
            new FakeInboundResponse(
                Status.OK,
                new ByteArrayInputStream("test".getBytes()),
                MediaType.APPLICATION_OCTET_STREAM_TYPE,
                null
            )
        );
        RestoreBackupService restoreBackupService = new RestoreBackupService(storageClientFactory);
        // when + then
        assertThatCode(
            () -> restoreBackupService.loadData(VitamConfiguration.getDefaultStrategy(), "100.json", 100L)
        ).isInstanceOf(VitamRuntimeException.class);
    }

    private RequestResponseOK<OfferLog> getListingOk(long offset, long limit) {
        RequestResponseOK<OfferLog> listing = new RequestResponseOK<>();
        listing.setHttpCode(Status.OK.getStatusCode());
        LongStream.range(offset, offset + limit).forEach(i -> {
            OfferLog offerLog = new OfferLog("container", String.valueOf(i), OfferLogAction.WRITE);
            listing.addResult(offerLog);
        });
        return listing;
    }
}
