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
package fr.gouv.vitam.storage.engine.server.rest;

import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.storage.engine.common.model.request.OfferSyncRequest;
import fr.gouv.vitam.storage.engine.common.model.response.OfferSyncResponseItem;
import fr.gouv.vitam.storage.engine.server.exception.VitamSyncException;
import fr.gouv.vitam.storage.engine.server.offersynchronization.OfferSyncService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.when;

/**
 * Offer synchronization resource tests.
 */
@RunWith(MockitoJUnitRunner.class)
public class AdminOfferSyncResourceTest {

    /**
     * Vitam Logger.
     */
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AdminOfferSyncResourceTest.class);

    private static final String OFFER_FS_1_SERVICE_CONSUL = "offer-fs-1.service.consul";
    private static final String OFFER_FS_2_SERVICE_CONSUL = "offer-fs-2.service.consul";

    private OfferSyncRequest offerSyncRequest;
    private OfferSyncResponseItem offerSyncResponseItem;

    @Mock
    private OfferSyncService offerSyncService;

    private AdminOfferSyncResource adminOfferSyncResource;

    @Before
    public void setup() {
        offerSyncRequest = new OfferSyncRequest();
    }

    @Test
    public void should_return_ok_when_request_item_full() throws VitamSyncException {

        // Given
        offerSyncRequest.setOfferSource(OFFER_FS_1_SERVICE_CONSUL).setOfferDestination(OFFER_FS_2_SERVICE_CONSUL)
            .setOffset(null);
        offerSyncResponseItem = new OfferSyncResponseItem(offerSyncRequest, StatusCode.OK);

        when(offerSyncService.synchronize(OFFER_FS_1_SERVICE_CONSUL, OFFER_FS_2_SERVICE_CONSUL, null, null, null))
            .thenReturn(offerSyncResponseItem);

        adminOfferSyncResource = new AdminOfferSyncResource(offerSyncService);

        // When
        Response response = adminOfferSyncResource.synchronizeOffer(offerSyncRequest);
        LOGGER.debug(String.format(
            "calling OfferSync service with the following parameters : %s source offer, %s destination offer, %d offset.",
            offerSyncRequest.getOfferSource(), offerSyncRequest.getOfferDestination(),
            offerSyncRequest.getOffset()));

        // Then
        LOGGER.debug("OfferSync response : ", response);
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        OfferSyncResponseItem responseEntity = (OfferSyncResponseItem) response.getEntity();
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getOfferSource()).isEqualTo(OFFER_FS_1_SERVICE_CONSUL);
        assertThat(responseEntity.getOfferDestination()).isEqualTo(OFFER_FS_2_SERVICE_CONSUL);
        assertThat(responseEntity.getStatus()).isEqualTo(StatusCode.OK);

    }

    @Test
    public void should_return_empty_response_when_that_request_empty() {

        // Given
        adminOfferSyncResource = new AdminOfferSyncResource(offerSyncService);
        offerSyncRequest =
            new OfferSyncRequest().setOfferSource("").setOfferDestination(OFFER_FS_2_SERVICE_CONSUL);

        // When / Then
        assertThatCode(() -> adminOfferSyncResource.synchronizeOffer(offerSyncRequest))
            .isInstanceOf(IllegalArgumentException.class);

        // Given
        offerSyncRequest =
            new OfferSyncRequest().setOfferSource(OFFER_FS_1_SERVICE_CONSUL).setOfferDestination(null);

        // When / Then
        assertThatCode(() -> adminOfferSyncResource.synchronizeOffer(offerSyncRequest))
            .isInstanceOf(IllegalArgumentException.class);

    }

    @Test
    public void should_return_ok_when_throws_sync_exception() throws VitamSyncException {

        // Given
        offerSyncRequest = new OfferSyncRequest();
        offerSyncRequest.setOfferSource(OFFER_FS_1_SERVICE_CONSUL).setOfferDestination(OFFER_FS_2_SERVICE_CONSUL)
            .setOffset(100L);

        when(offerSyncService.synchronize(OFFER_FS_1_SERVICE_CONSUL, OFFER_FS_2_SERVICE_CONSUL, null, null, null))
            .thenThrow(new VitamSyncException("ERROR: Exception has been throw when calling offerSync service."));

        adminOfferSyncResource = new AdminOfferSyncResource(offerSyncService);

        // When
        Response response = adminOfferSyncResource.synchronizeOffer(offerSyncRequest);

        // Then
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

    }

    @Test
    public void should_throws_IllegalArgument_exception_when_request_item_null() throws DatabaseException {

        // Given
        adminOfferSyncResource = new AdminOfferSyncResource(offerSyncService);

        // When / Then
        assertThatCode(() -> adminOfferSyncResource.synchronizeOffer(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

}
