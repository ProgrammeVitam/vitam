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

import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.AuthenticationLevel;
import fr.gouv.vitam.common.security.rest.VitamAuthentication;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.request.OfferPartialSyncItem;
import fr.gouv.vitam.storage.engine.common.model.request.OfferPartialSyncRequest;
import fr.gouv.vitam.storage.engine.common.model.request.OfferSyncRequest;
import fr.gouv.vitam.storage.engine.server.distribution.StorageDistribution;
import fr.gouv.vitam.storage.engine.server.offersynchronization.OfferSyncService;
import fr.gouv.vitam.storage.engine.server.offersynchronization.OfferSyncStatus;
import fr.gouv.vitam.storage.engine.server.rest.writeprotection.WriteProtection;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.elasticsearch.common.Strings;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static fr.gouv.vitam.common.GlobalDataRest.X_REQUEST_ID;

@Path("/storage/v1")
@Tag(name = "Admin-Offer")
public class AdminOfferSyncResource {

    /**
     * Vitam Logger.
     */
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AdminOfferSyncResource.class);

    private static final String OFFER_SYNC_URI = "/offerSync";
    private static final String OFFER_PARTIAL_SYNC_URI = "/offerPartialSync";

    /**
     * OfferSynchronization Service.
     */
    private OfferSyncService offerSyncService;

    /**
     * Constructor.
     */
    public AdminOfferSyncResource(StorageDistribution distribution, StorageConfiguration storageConfiguration) {
        this(new OfferSyncService(distribution, storageConfiguration));
    }

    /**
     * Constructor.
     *
     * @param offerSyncService
     */
    @VisibleForTesting
    public AdminOfferSyncResource(OfferSyncService offerSyncService) {
        this.offerSyncService = offerSyncService;
    }

    @Path(OFFER_PARTIAL_SYNC_URI)
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @WriteProtection(true)
    public Response startPartialSynchronization(OfferPartialSyncRequest offerPartialSyncRequest) {
        ParametersChecker.checkParameter("source offer is mandatory.", offerPartialSyncRequest.getSourceOffer());
        ParametersChecker.checkParameter("target offer is mandatory.", offerPartialSyncRequest.getTargetOffer());
        if (offerPartialSyncRequest.getSourceOffer().equals(offerPartialSyncRequest.getTargetOffer())) {
            throw new IllegalArgumentException("Source offer cannot be the same as target offer");
        }

        VitamThreadUtils.getVitamSession().setTenantId(VitamConfiguration.getAdminTenant());
        VitamThreadUtils.getVitamSession()
            .setRequestId(GUIDFactory.newRequestIdGUID(VitamConfiguration.getAdminTenant()));

        if (
            null == offerPartialSyncRequest.getItemsToSynchronize() ||
            offerPartialSyncRequest.getItemsToSynchronize().isEmpty()
        ) {
            LOGGER.info("Items to synchronize is empty");
            return Response.status(Response.Status.BAD_REQUEST)
                .header(X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
                .entity(
                    JsonHandler.unprettyPrint(
                        JsonHandler.createObjectNode().put("Error", "ItemsToSynchronize parameter is empty")
                    )
                )
                .build();
        }

        String validateRequestMsg = validateRequest(offerPartialSyncRequest);

        if (validateRequestMsg.length() > 0) {
            LOGGER.error(validateRequestMsg);
            throw new IllegalArgumentException(validateRequestMsg);
        }

        boolean started = offerSyncService.startSynchronization(
            offerPartialSyncRequest.getSourceOffer(),
            offerPartialSyncRequest.getTargetOffer(),
            offerPartialSyncRequest.getStrategyId(),
            offerPartialSyncRequest.getItemsToSynchronize()
        );

        Response.Status status;
        if (started) {
            LOGGER.info("Offer partial synchronization started");
            status = Response.Status.OK;
        } else {
            LOGGER.warn("Another synchronization process is already running");
            status = Response.Status.CONFLICT;
        }
        return Response.status(status).header(X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId()).build();
    }

    private String validateRequest(OfferPartialSyncRequest offerPartialSyncRequest) {
        StringBuilder sb = new StringBuilder();
        for (OfferPartialSyncItem o : offerPartialSyncRequest.getItemsToSynchronize()) {
            if (!VitamConfiguration.getTenants().contains(o.getTenantId())) {
                sb.append("Invalid tenant ").append(o.getTenantId()).append(", ");
            }
            if (Strings.isNullOrEmpty(o.getContainer())) {
                sb.append("container required, ");
            }

            if (null == o.getFilenames() || o.getFilenames().isEmpty()) {
                sb.append("filenames is required; ");
            } else {
                o
                    .getFilenames()
                    .forEach(f -> {
                        if (Strings.isNullOrEmpty(f)) {
                            sb.append("File is required; ");
                        }
                    });
            }
        }

        return sb.toString();
    }

    /**
     * Start offer synchronization. At most, one synchronization process can be started.
     */
    @Path(OFFER_SYNC_URI)
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @VitamAuthentication(authentLevel = AuthenticationLevel.BASIC_AUTHENT)
    @Operation(
        summary = "Start offer synchronization",
        description = "Démarre la synchronisation des offres. Une seule synchronisation peut être démarré à la fois."
    )
    @WriteProtection(true)
    public Response startSynchronization(OfferSyncRequest offerSyncRequest) {
        ParametersChecker.checkParameter("source offer is mandatory.", offerSyncRequest.getSourceOffer());
        ParametersChecker.checkParameter("target offer is mandatory.", offerSyncRequest.getTargetOffer());
        if (offerSyncRequest.getSourceOffer().equals(offerSyncRequest.getTargetOffer())) {
            throw new IllegalArgumentException("Source offer cannot be the same as target offer");
        }
        ParametersChecker.checkParameter("strategyId is mandatory.", offerSyncRequest.getStrategyId());
        ParametersChecker.checkParameter("tenantId is mandatory.", offerSyncRequest.getTenantId());
        ParametersChecker.checkParameter("container is mandatory.", offerSyncRequest.getContainer());

        if (!VitamConfiguration.getTenants().contains(offerSyncRequest.getTenantId())) {
            throw new IllegalArgumentException("Invalid tenant " + offerSyncRequest.getTenantId());
        }
        VitamThreadUtils.getVitamSession().setTenantId(offerSyncRequest.getTenantId());
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(offerSyncRequest.getTenantId()));

        DataCategory dataCategory = DataCategory.getByCollectionName(offerSyncRequest.getContainer());

        LOGGER.info(
            String.format(
                "Starting %s offer synchronization from the %s source offer with %d%n offset.",
                offerSyncRequest.getTargetOffer(),
                offerSyncRequest.getSourceOffer(),
                offerSyncRequest.getOffset()
            )
        );

        boolean started = offerSyncService.startSynchronization(
            offerSyncRequest.getSourceOffer(),
            offerSyncRequest.getTargetOffer(),
            offerSyncRequest.getStrategyId(),
            dataCategory,
            offerSyncRequest.getOffset()
        );

        Response.Status status;
        if (started) {
            LOGGER.info("Offer synchronization started");
            status = Response.Status.OK;
        } else {
            LOGGER.warn("Another synchronization process is already running");
            status = Response.Status.CONFLICT;
        }
        return Response.status(status).header(X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId()).build();
    }

    /**
     * Returns offer synchronization process running status in a "Running" header (true/false).
     */
    @Path(OFFER_SYNC_URI)
    @HEAD
    @VitamAuthentication(authentLevel = AuthenticationLevel.BASIC_AUTHENT)
    @Operation(
        summary = "return if offer synchronization is running",
        description = "Permet de récupérer le status de la synchronisation des offres (en cours ou non)"
    )
    @WriteProtection(true)
    public Response isOfferSynchronizationRunning() {
        return Response.ok().header("Running", this.offerSyncService.isRunning()).build();
    }

    /**
     * Returns the offer synchronization status of the last synchronization (synchronization may be done, or still running)
     */
    @Path(OFFER_SYNC_URI)
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @VitamAuthentication(authentLevel = AuthenticationLevel.BASIC_AUTHENT)
    @Operation(
        summary = "return last offer synchronization status",
        description = "Permet de récupérer le status de la dernière synchronisation des offres (terminée ou en cours)"
    )
    @WriteProtection(true)
    public Response getLastOfferSynchronizationStatus() {
        OfferSyncStatus lastSynchronizationStatus = this.offerSyncService.getLastSynchronizationStatus();

        if (lastSynchronizationStatus == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.ok(lastSynchronizationStatus).build();
    }
}
