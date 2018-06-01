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

import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.AuthenticationLevel;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.security.rest.VitamAuthentication;
import fr.gouv.vitam.storage.engine.common.model.request.OfferSyncRequestItem;
import fr.gouv.vitam.storage.engine.common.model.response.OfferSyncResponseItem;
import fr.gouv.vitam.storage.engine.server.distribution.StorageDistribution;
import fr.gouv.vitam.storage.engine.server.exception.VitamSyncException;
import fr.gouv.vitam.storage.engine.server.offersynchronization.OfferSyncService;
import fr.gouv.vitam.storage.engine.server.offersynchronization.OfferSyncServiceImpl;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Offer synchronization resource.
 */
@Path("/storage/v1")
public class AdminOfferSyncResource {

    /**
     * Vitam Logger.
     */
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AdminOfferSyncResource.class);

    /**
     * Error/Exceptions messages.
     */
    private static final String SYNCHRONIZATION_JSON_MANDATORY_PARAMETERS_MSG =
        "the Json input of offer synchronization's parameters is mondatory.";

    private static final String SYNCHRONIZATION_EXCEPTION_MSG =
        "ERROR: Exception has been thrown when synchronizing the offers : ";
    private static final String SOURCE_OFFER_PARAMETER_IS_MONDATORY = "the source offer parameter is mondatory.";
    private static final String DESTINATION_OFFER_PARAMETER_IS_MONDATORY =
        "the destination offer parameter is mondatory.";

    private final String OFFER_SYNC_URI = "/offerSync";

    /**
     * OfferSynchronization Service.
     */
    private OfferSyncService offerSynchronizationService;

    /**
     * Constructor.
     *
     * @param distribution
     */
    public AdminOfferSyncResource(StorageDistribution distribution) {
        this(new OfferSyncServiceImpl(distribution));
    }

    /**
     * Constructor.
     *
     * @param offerSynchronizationService
     */
    @VisibleForTesting
    public AdminOfferSyncResource(
        OfferSyncService offerSynchronizationService) {
        this.offerSynchronizationService = offerSynchronizationService;
    }

    /**
     * API to access and lanch the offer synchronization service.<br/>
     *
     * @param offerSyncItems
     * @return
     */
    @Path(OFFER_SYNC_URI)
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @VitamAuthentication(authentLevel = AuthenticationLevel.BASIC_AUTHENT)
    public Response synchronizeOffer(OfferSyncRequestItem offerSyncItems) {

        ParametersChecker.checkParameter(SYNCHRONIZATION_JSON_MANDATORY_PARAMETERS_MSG, offerSyncItems);
        ParametersChecker.checkParameter(SOURCE_OFFER_PARAMETER_IS_MONDATORY, offerSyncItems.getOfferSource());
        ParametersChecker
            .checkParameter(DESTINATION_OFFER_PARAMETER_IS_MONDATORY, offerSyncItems.getOfferDestination());
        OfferSyncResponseItem response = new OfferSyncResponseItem();

        try {
            LOGGER.debug(String
                .format("Starting %s offer synchronization from the %s source offer with %d%n offset.",
                    offerSyncItems.getOfferDestination(), offerSyncItems.getOfferSource(),
                    offerSyncItems.getOffset()));

            response = offerSynchronizationService
                .synchronize(offerSyncItems.getOfferSource(), offerSyncItems.getOfferDestination(),
                    offerSyncItems.getContainerToSync(), offerSyncItems.getTenantIdToSync(),
                    offerSyncItems.getOffset());

        } catch (VitamSyncException e) {
            LOGGER.error(SYNCHRONIZATION_EXCEPTION_MSG, e);
            response = new OfferSyncResponseItem(offerSyncItems, StatusCode.KO);
        }

        return Response.ok().entity(response).build();
    }

}
