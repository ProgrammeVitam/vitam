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
package fr.gouv.vitam.storage.engine.server.rest;

import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.request.OfferSyncRequest;
import fr.gouv.vitam.storage.engine.server.offersynchronization.OfferSyncService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.ws.rs.core.Response;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Offer synchronization resource tests.
 */
public class AdminOfferSyncResourceTest {

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    /**
     * Vitam Logger.
     */
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AdminOfferSyncResourceTest.class);

    private static final String OFFER_FS_1_SERVICE_CONSUL = "offer-fs-1.service.consul";
    private static final String OFFER_FS_2_SERVICE_CONSUL = "offer-fs-2.service.consul";

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private OfferSyncService offerSyncService;

    @Before
    public void setup() {
        VitamConfiguration.setTenants(Arrays.asList(0, 1, 2));
    }

    @Test
    @RunWithCustomExecutor
    public void should_return_ok_when_offer_synchronization_started() throws Exception {
        // Given
        OfferSyncRequest offerSyncRequest = createOfferSyncRequest();

        when(
            offerSyncService.startSynchronization(
                OFFER_FS_1_SERVICE_CONSUL,
                OFFER_FS_2_SERVICE_CONSUL,
                VitamConfiguration.getDefaultStrategy(),
                DataCategory.UNIT,
                null
            )
        ).thenReturn(true);

        AdminOfferSyncResource instance = new AdminOfferSyncResource(offerSyncService);

        // When
        Response response = instance.startSynchronization(offerSyncRequest);

        // Then
        LOGGER.debug("OfferSync response : ", response);
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void should_return_conflict_when_offer_synchronization_already_running() throws Exception {
        // Given
        OfferSyncRequest offerSyncRequest = createOfferSyncRequest();

        when(
            offerSyncService.startSynchronization(
                OFFER_FS_1_SERVICE_CONSUL,
                OFFER_FS_2_SERVICE_CONSUL,
                VitamConfiguration.getDefaultStrategy(),
                DataCategory.UNIT,
                null
            )
        ).thenReturn(false);

        AdminOfferSyncResource instance = new AdminOfferSyncResource(offerSyncService);

        // When
        Response response = instance.startSynchronization(offerSyncRequest);

        // Then
        LOGGER.debug("OfferSync response : ", response);
        assertThat(response.getStatus()).isEqualTo(Response.Status.CONFLICT.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void should_throw_exception_when_offer_synchronization_request_with_missing_tenant() {
        // Given
        OfferSyncRequest offerSyncRequest = createOfferSyncRequest().setTenantId(null);
        AdminOfferSyncResource instance = new AdminOfferSyncResource(offerSyncService);

        // When / Then
        assertThatThrownBy(() -> instance.startSynchronization(offerSyncRequest)).isInstanceOf(
            IllegalArgumentException.class
        );
        verifyNoMoreInteractions(offerSyncService);
    }

    @Test
    @RunWithCustomExecutor
    public void should_throw_exception_when_offer_synchronization_request_with_missing_strategy() {
        // Given
        OfferSyncRequest offerSyncRequest = createOfferSyncRequest().setStrategyId(null);
        AdminOfferSyncResource instance = new AdminOfferSyncResource(offerSyncService);

        // When / Then
        assertThatThrownBy(() -> instance.startSynchronization(offerSyncRequest)).isInstanceOf(
            IllegalArgumentException.class
        );
        verifyNoMoreInteractions(offerSyncService);
    }

    @Test
    @RunWithCustomExecutor
    public void should_throw_exception_when_offer_synchronization_request_with_invalid_tenant() {
        // Given
        OfferSyncRequest offerSyncRequest = createOfferSyncRequest().setTenantId(3);
        AdminOfferSyncResource instance = new AdminOfferSyncResource(offerSyncService);

        // When / Then
        assertThatThrownBy(() -> instance.startSynchronization(offerSyncRequest)).isInstanceOf(
            IllegalArgumentException.class
        );
        verifyNoMoreInteractions(offerSyncService);
    }

    @Test
    @RunWithCustomExecutor
    public void should_throw_exception_when_offer_synchronization_request_with_missing_source_offer() {
        // Given
        OfferSyncRequest offerSyncRequest = createOfferSyncRequest().setSourceOffer(null);
        AdminOfferSyncResource instance = new AdminOfferSyncResource(offerSyncService);

        // When / Then
        assertThatThrownBy(() -> instance.startSynchronization(offerSyncRequest)).isInstanceOf(
            IllegalArgumentException.class
        );
        verifyNoMoreInteractions(offerSyncService);
    }

    @Test
    @RunWithCustomExecutor
    public void should_throw_exception_when_offer_synchronization_request_with_missing_target_offer() {
        // Given
        OfferSyncRequest offerSyncRequest = createOfferSyncRequest().setTargetOffer(null);
        AdminOfferSyncResource instance = new AdminOfferSyncResource(offerSyncService);

        // When / Then
        assertThatThrownBy(() -> instance.startSynchronization(offerSyncRequest)).isInstanceOf(
            IllegalArgumentException.class
        );
        verifyNoMoreInteractions(offerSyncService);
    }

    @Test
    @RunWithCustomExecutor
    public void should_throw_exception_when_offer_synchronization_request_with_missing_container() {
        // Given
        OfferSyncRequest offerSyncRequest = createOfferSyncRequest().setContainer(null);
        AdminOfferSyncResource instance = new AdminOfferSyncResource(offerSyncService);

        // When / Then
        assertThatThrownBy(() -> instance.startSynchronization(offerSyncRequest)).isInstanceOf(
            IllegalArgumentException.class
        );
        verifyNoMoreInteractions(offerSyncService);
    }

    @Test
    @RunWithCustomExecutor
    public void should_throw_exception_when_offer_synchronization_request_with_invalid_container() {
        // Given
        OfferSyncRequest offerSyncRequest = createOfferSyncRequest().setContainer("BAD");
        AdminOfferSyncResource instance = new AdminOfferSyncResource(offerSyncService);

        // When / Then
        assertThatThrownBy(() -> instance.startSynchronization(offerSyncRequest)).isInstanceOf(
            IllegalArgumentException.class
        );
        verifyNoMoreInteractions(offerSyncService);
    }

    private OfferSyncRequest createOfferSyncRequest() {
        return new OfferSyncRequest()
            .setSourceOffer(OFFER_FS_1_SERVICE_CONSUL)
            .setTargetOffer(OFFER_FS_2_SERVICE_CONSUL)
            .setContainer(DataCategory.UNIT.getCollectionName())
            .setOffset(null)
            .setTenantId(0)
            .setStrategyId(VitamConfiguration.getDefaultStrategy());
    }
}
