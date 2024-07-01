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
package fr.gouv.vitam.storage.engine.server.distribution.impl;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.accesslog.AccessLogInfoModel;
import fr.gouv.vitam.common.alert.AlertService;
import fr.gouv.vitam.common.collection.CloseableIterator;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogLevel;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.storage.AccessRequestStatus;
import fr.gouv.vitam.common.model.storage.ObjectEntry;
import fr.gouv.vitam.storage.engine.common.exception.StorageException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.Order;
import fr.gouv.vitam.storage.engine.common.model.request.BulkObjectStoreRequest;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.storage.engine.common.model.response.BatchObjectInformationResponse;
import fr.gouv.vitam.storage.engine.common.model.response.BulkObjectStoreResponse;
import fr.gouv.vitam.storage.engine.common.model.response.StoredInfoResult;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageStrategy;
import fr.gouv.vitam.storage.engine.server.distribution.StorageDistribution;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

public class ReadOnlyShieldStorageDistributionTest {

    private static final String STRATEGY = "default";
    private static final String OBJECT_ID = "obj1";
    private static final String OFFER1 = "offer1";
    private static final String OFFER2 = "offer2";
    private static final List<String> OFFER_IDS = List.of(OFFER1, OFFER2);
    private static final String REQUESTER = "requester";
    private static final String ORIGIN = "origin";
    private static final DataCategory DATA_CATEGORY = DataCategory.LOGBOOK;
    private static final String ACCESS_REQUEST_ID = "accessRequestId";

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private StorageDistribution innerStorageDistribution;

    @Mock
    private AlertService alertService;

    @Mock
    private CloseableIterator<ObjectEntry> objectEntryIterator;

    @Mock
    private RequestResponse<OfferLog> offerLogRequestResponse;

    private ReadOnlyShieldStorageDistribution instance;

    @Before
    public void init() {
        instance = new ReadOnlyShieldStorageDistribution(innerStorageDistribution, alertService);
    }

    @After
    public void afterTest() {
        // Ensure no more interactions
        verifyNoMoreInteractions(innerStorageDistribution, alertService);
    }

    @Test
    public void testCloseReadOnly() {
        testClose(instance);
    }

    private void testClose(ReadOnlyShieldStorageDistribution instance) {
        // Given

        // When / Then
        assertThatCode(instance::close).doesNotThrowAnyException();
        verify(innerStorageDistribution).close();
    }

    @Test
    public void testCopyObjectFromOfferToOfferReadOnly() throws StorageException {
        // Given
        DataContext dataContext = mock(DataContext.class);
        StoredInfoResult storedInfoResult = mock(StoredInfoResult.class);
        doReturn(storedInfoResult)
            .when(innerStorageDistribution)
            .copyObjectFromOfferToOffer(dataContext, OFFER1, OFFER2);

        // When / Then
        assertThatThrownBy(() -> instance.copyObjectFromOfferToOffer(dataContext, OFFER1, OFFER2)).isInstanceOf(
            IllegalStateException.class
        );
        verify(alertService).createAlert(eq(VitamLogLevel.ERROR), anyString(), any());
        verifyZeroInteractions(innerStorageDistribution);
    }

    @Test
    public void testStoreDataInAllOffersReadOnly() throws StorageException {
        // Given
        ObjectDescription objectDescription = mock(ObjectDescription.class);
        StoredInfoResult storedInfoResult = mock(StoredInfoResult.class);
        doReturn(storedInfoResult)
            .when(innerStorageDistribution)
            .storeDataInAllOffers(STRATEGY, OFFER1, objectDescription, DATA_CATEGORY, REQUESTER);

        // When / Then
        assertThatThrownBy(
            () -> instance.storeDataInAllOffers(STRATEGY, OFFER1, objectDescription, DATA_CATEGORY, REQUESTER)
        ).isInstanceOf(IllegalStateException.class);
        verify(alertService).createAlert(eq(VitamLogLevel.ERROR), anyString(), any());
        verifyZeroInteractions(innerStorageDistribution);
    }

    @Test
    public void testStoreDataInOffersWithResponseReadOnly() throws StorageException {
        // Given
        Response response = mock(Response.class);
        StoredInfoResult storedInfoResult = mock(StoredInfoResult.class);
        doReturn(storedInfoResult)
            .when(innerStorageDistribution)
            .storeDataInOffers(STRATEGY, ORIGIN, OBJECT_ID, DATA_CATEGORY, REQUESTER, OFFER_IDS, response);

        // When / Then
        assertThatThrownBy(
            () -> instance.storeDataInOffers(STRATEGY, ORIGIN, OBJECT_ID, DATA_CATEGORY, REQUESTER, OFFER_IDS, response)
        ).isInstanceOf(IllegalStateException.class);
        verify(alertService).createAlert(eq(VitamLogLevel.ERROR), anyString(), any());
        verifyZeroInteractions(innerStorageDistribution);
    }

    @Test
    public void testStoreDataInOffersWithStreamAndInfoReadOnly() throws StorageException {
        // Given
        StreamAndInfo streamAndInfo = mock(StreamAndInfo.class);
        StoredInfoResult storedInfoResult = mock(StoredInfoResult.class);
        doReturn(storedInfoResult)
            .when(innerStorageDistribution)
            .storeDataInOffers(STRATEGY, ORIGIN, streamAndInfo, OBJECT_ID, DATA_CATEGORY, REQUESTER, OFFER_IDS);

        // When / Then
        assertThatThrownBy(
            () ->
                instance.storeDataInOffers(
                    STRATEGY,
                    ORIGIN,
                    streamAndInfo,
                    OBJECT_ID,
                    DATA_CATEGORY,
                    REQUESTER,
                    OFFER_IDS
                )
        ).isInstanceOf(IllegalStateException.class);
        verify(alertService).createAlert(eq(VitamLogLevel.ERROR), anyString(), any());
        verifyZeroInteractions(innerStorageDistribution);
    }

    @Test
    public void testGetOfferIdsReadonly() throws StorageException {
        // Given
        doReturn(OFFER_IDS).when(innerStorageDistribution).getOfferIds(STRATEGY);

        // When
        List<String> results = instance.getOfferIds(STRATEGY);

        // Then
        verify(innerStorageDistribution).getOfferIds(STRATEGY);
        assertThat(results).isEqualTo(OFFER_IDS);
    }

    @Test
    public void testGetContainerInformationReadOnly() throws StorageException {
        // Given
        JsonNode response = JsonHandler.createObjectNode();
        doReturn(response).when(innerStorageDistribution).getContainerInformation(STRATEGY);

        // When
        JsonNode result = instance.getContainerInformation(STRATEGY);

        // Then
        verify(innerStorageDistribution).getContainerInformation(STRATEGY);
        assertThat(result).isEqualTo(response);
    }

    @Test
    public void testGetContainerInformationOfObjectReadOnly() throws StorageException {
        // Given
        JsonNode response = JsonHandler.createObjectNode();
        doReturn(response)
            .when(innerStorageDistribution)
            .getContainerInformation(STRATEGY, DATA_CATEGORY, OBJECT_ID, OFFER_IDS, true);

        // When
        JsonNode result = instance.getContainerInformation(STRATEGY, DATA_CATEGORY, OBJECT_ID, OFFER_IDS, true);

        // Then
        verify(innerStorageDistribution).getContainerInformation(STRATEGY, DATA_CATEGORY, OBJECT_ID, OFFER_IDS, true);
        assertThat(result).isEqualTo(response);
    }

    @Test
    public void testListContainerObjectsReadOnly() throws StorageException {
        // Given
        doReturn(objectEntryIterator).when(innerStorageDistribution).listContainerObjects(STRATEGY, DATA_CATEGORY);

        // When
        CloseableIterator<ObjectEntry> result = instance.listContainerObjects(STRATEGY, DATA_CATEGORY);

        // Then
        verify(innerStorageDistribution).listContainerObjects(STRATEGY, DATA_CATEGORY);
        assertThat(result).isEqualTo(objectEntryIterator);
    }

    @Test
    public void testListContainerObjectsForOfferReadOnly() throws StorageException {
        // Given
        doReturn(objectEntryIterator)
            .when(innerStorageDistribution)
            .listContainerObjectsForOffer(DATA_CATEGORY, OFFER1, true);

        // When
        CloseableIterator<ObjectEntry> result = instance.listContainerObjectsForOffer(DATA_CATEGORY, OFFER1, true);

        // Then
        verify(innerStorageDistribution).listContainerObjectsForOffer(DATA_CATEGORY, OFFER1, true);
        assertThat(result).isEqualTo(objectEntryIterator);
    }

    @Test
    public void testGetOfferLogsReadOnly() throws StorageException {
        // Given
        doReturn(offerLogRequestResponse)
            .when(innerStorageDistribution)
            .getOfferLogs(STRATEGY, DATA_CATEGORY, 132L, 321, Order.ASC);

        // When
        RequestResponse<OfferLog> result = instance.getOfferLogs(STRATEGY, DATA_CATEGORY, 132L, 321, Order.ASC);

        // Then
        verify(innerStorageDistribution).getOfferLogs(STRATEGY, DATA_CATEGORY, 132L, 321, Order.ASC);
        assertThat(result).isEqualTo(offerLogRequestResponse);
    }

    @Test
    public void testGetOfferLogsByOfferIdReadOnly() throws StorageException {
        // Given
        doReturn(offerLogRequestResponse)
            .when(innerStorageDistribution)
            .getOfferLogsByOfferId(STRATEGY, OFFER1, DATA_CATEGORY, 132L, 321, Order.ASC);

        // When
        RequestResponse<OfferLog> result = instance.getOfferLogsByOfferId(
            STRATEGY,
            OFFER1,
            DATA_CATEGORY,
            132L,
            321,
            Order.ASC
        );

        // Then
        verify(innerStorageDistribution).getOfferLogsByOfferId(STRATEGY, OFFER1, DATA_CATEGORY, 132L, 321, Order.ASC);
        assertThat(result).isEqualTo(offerLogRequestResponse);
    }

    @Test
    public void testGetContainerByCategoryReadOnly() throws StorageException {
        // Given
        AccessLogInfoModel loginInformation = mock(AccessLogInfoModel.class);
        Response response = mock(Response.class);
        doReturn(response)
            .when(innerStorageDistribution)
            .getContainerByCategory(STRATEGY, ORIGIN, OBJECT_ID, DATA_CATEGORY, loginInformation);

        // When
        Response result = instance.getContainerByCategory(STRATEGY, ORIGIN, OBJECT_ID, DATA_CATEGORY, loginInformation);

        // Then
        verify(innerStorageDistribution).getContainerByCategory(
            STRATEGY,
            ORIGIN,
            OBJECT_ID,
            DATA_CATEGORY,
            loginInformation
        );
        assertThat(result).isEqualTo(response);
    }

    @Test
    public void testGetContainerByCategoryForOfferReadOnly() throws StorageException {
        // Given
        Response response = mock(Response.class);
        doReturn(response)
            .when(innerStorageDistribution)
            .getContainerByCategory(STRATEGY, ORIGIN, OBJECT_ID, DATA_CATEGORY, OFFER1);

        // When
        Response result = instance.getContainerByCategory(STRATEGY, ORIGIN, OBJECT_ID, DATA_CATEGORY, OFFER1);

        // Then
        verify(innerStorageDistribution).getContainerByCategory(STRATEGY, ORIGIN, OBJECT_ID, DATA_CATEGORY, OFFER1);
        assertThat(result).isEqualTo(response);
    }

    @Test
    public void testCheckObjectExistingReadOnly() throws StorageException {
        // Given
        Map<String, Boolean> response = Map.of(OFFER1, true, OFFER2, false);
        doReturn(response)
            .when(innerStorageDistribution)
            .checkObjectExisting(STRATEGY, OBJECT_ID, DATA_CATEGORY, OFFER_IDS);

        // When
        Map<String, Boolean> result = instance.checkObjectExisting(STRATEGY, OBJECT_ID, DATA_CATEGORY, OFFER_IDS);

        // Then
        verify(innerStorageDistribution).checkObjectExisting(STRATEGY, OBJECT_ID, DATA_CATEGORY, OFFER_IDS);
        assertThat(result).isEqualTo(response);
    }

    @Test
    public void testDeleteObjectInAllOffersReadOnly() throws StorageException {
        // Given
        DataContext dataContext = mock(DataContext.class);
        doNothing().when(innerStorageDistribution).deleteObjectInAllOffers(STRATEGY, dataContext);

        // When / Then
        assertThatThrownBy(() -> instance.deleteObjectInAllOffers(STRATEGY, dataContext)).isInstanceOf(
            IllegalStateException.class
        );
        verify(alertService).createAlert(eq(VitamLogLevel.ERROR), anyString(), any());
        verifyZeroInteractions(innerStorageDistribution);
    }

    @Test
    public void testDeleteObjectInOffersReadOnly() throws StorageException {
        // Given
        DataContext dataContext = mock(DataContext.class);
        doNothing().when(innerStorageDistribution).deleteObjectInOffers(STRATEGY, dataContext, OFFER_IDS);

        // When / Then
        assertThatThrownBy(() -> instance.deleteObjectInOffers(STRATEGY, dataContext, OFFER_IDS)).isInstanceOf(
            IllegalStateException.class
        );
        verify(alertService).createAlert(eq(VitamLogLevel.ERROR), anyString(), any());
        verifyZeroInteractions(innerStorageDistribution);
    }

    @Test
    public void testGetBatchObjectInformationReadOnly() throws StorageException {
        // Given
        List<BatchObjectInformationResponse> response = List.of(mock(BatchObjectInformationResponse.class));
        doReturn(response)
            .when(innerStorageDistribution)
            .getBatchObjectInformation(STRATEGY, DATA_CATEGORY, List.of(OBJECT_ID), OFFER_IDS);

        // When
        List<BatchObjectInformationResponse> result = instance.getBatchObjectInformation(
            STRATEGY,
            DATA_CATEGORY,
            List.of(OBJECT_ID),
            OFFER_IDS
        );

        // Then
        verify(innerStorageDistribution).getBatchObjectInformation(
            STRATEGY,
            DATA_CATEGORY,
            List.of(OBJECT_ID),
            OFFER_IDS
        );
        assertThat(result).isEqualTo(response);
    }

    @Test
    public void testBulkCreateFromWorkspaceReadOnly() throws StorageException {
        // Given
        BulkObjectStoreRequest bulkObjectStoreRequest = mock(BulkObjectStoreRequest.class);
        BulkObjectStoreResponse response = mock(BulkObjectStoreResponse.class);
        doReturn(response)
            .when(innerStorageDistribution)
            .bulkCreateFromWorkspace(STRATEGY, bulkObjectStoreRequest, REQUESTER);

        // When / Then
        assertThatThrownBy(
            () -> instance.bulkCreateFromWorkspace(STRATEGY, bulkObjectStoreRequest, REQUESTER)
        ).isInstanceOf(IllegalStateException.class);
        verify(alertService).createAlert(eq(VitamLogLevel.ERROR), anyString(), any());
        verifyZeroInteractions(innerStorageDistribution);
    }

    @Test
    public void testGetStrategiesReadOnly() throws StorageException {
        testGetStrategies(instance);
    }

    private void testGetStrategies(ReadOnlyShieldStorageDistribution instance) throws StorageException {
        // Given
        Map<String, StorageStrategy> response = Map.of(STRATEGY, mock(StorageStrategy.class));
        doReturn(response).when(innerStorageDistribution).getStrategies();

        // When
        Map<String, StorageStrategy> result = instance.getStrategies();

        // Then
        verify(innerStorageDistribution).getStrategies();
        assertThat(result).isEqualTo(response);
    }

    @Test
    public void testCreateAccessRequestIfRequiredReadOnly() throws StorageException {
        // Given
        doReturn(Optional.of(ACCESS_REQUEST_ID))
            .when(innerStorageDistribution)
            .createAccessRequestIfRequired(STRATEGY, OFFER1, DATA_CATEGORY, List.of(OBJECT_ID));

        // When
        Optional<String> result = instance.createAccessRequestIfRequired(
            STRATEGY,
            OFFER1,
            DATA_CATEGORY,
            List.of(OBJECT_ID)
        );

        // Then
        verify(innerStorageDistribution).createAccessRequestIfRequired(
            STRATEGY,
            OFFER1,
            DATA_CATEGORY,
            List.of(OBJECT_ID)
        );
        assertThat(result).isEqualTo(Optional.of(ACCESS_REQUEST_ID));
    }

    @Test
    public void testCheckAccessRequestStatusesReadOnly() throws StorageException {
        // Given
        doReturn(Map.of(ACCESS_REQUEST_ID, AccessRequestStatus.READY))
            .when(innerStorageDistribution)
            .checkAccessRequestStatuses(STRATEGY, OFFER1, List.of(ACCESS_REQUEST_ID), true);

        // When
        Map<String, AccessRequestStatus> result = instance.checkAccessRequestStatuses(
            STRATEGY,
            OFFER1,
            List.of(ACCESS_REQUEST_ID),
            true
        );

        // Then
        verify(innerStorageDistribution).checkAccessRequestStatuses(STRATEGY, OFFER1, List.of(ACCESS_REQUEST_ID), true);
        assertThat(result).isEqualTo(Map.of(ACCESS_REQUEST_ID, AccessRequestStatus.READY));
    }

    @Test
    public void testRemoveAccessRequestReadOnly() throws StorageException {
        // Given
        doNothing().when(innerStorageDistribution).removeAccessRequest(STRATEGY, OFFER1, ACCESS_REQUEST_ID, true);

        // When
        instance.removeAccessRequest(STRATEGY, OFFER1, ACCESS_REQUEST_ID, true);

        // Then
        verify(innerStorageDistribution).removeAccessRequest(STRATEGY, OFFER1, ACCESS_REQUEST_ID, true);
    }

    @Test
    public void testCheckObjectAvailabilityReadOnly() throws StorageException {
        // Given
        doReturn(true)
            .when(innerStorageDistribution)
            .checkObjectAvailability(STRATEGY, OFFER1, DATA_CATEGORY, List.of(OBJECT_ID));

        // When
        boolean result = instance.checkObjectAvailability(STRATEGY, OFFER1, DATA_CATEGORY, List.of(OBJECT_ID));

        // Then
        verify(innerStorageDistribution).checkObjectAvailability(STRATEGY, OFFER1, DATA_CATEGORY, List.of(OBJECT_ID));
        assertThat(result).isEqualTo(true);
    }

    @Test
    public void testGetReferentOfferReadOnly() throws StorageException {
        // Given
        doReturn(OFFER1).when(innerStorageDistribution).getReferentOffer(STRATEGY);

        // When
        String result = instance.getReferentOffer(STRATEGY);

        // Then
        verify(innerStorageDistribution).getReferentOffer(STRATEGY);
        assertThat(result).isEqualTo(OFFER1);
    }

    @Test
    public void testLaunchOfferLogCompaction() throws StorageException {
        // Given
        Response response = mock(Response.class);
        doReturn(response).when(innerStorageDistribution).launchOfferLogCompaction(OFFER1, 1);

        // When
        Response result = instance.launchOfferLogCompaction(OFFER1, 1);

        // Then
        verify(innerStorageDistribution).launchOfferLogCompaction(OFFER1, 1);
        assertThat(result).isEqualTo(response);
    }
}
