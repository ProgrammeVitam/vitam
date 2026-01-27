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
package fr.gouv.vitam.storage.offers.rest;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.AuthenticationLevel;
import fr.gouv.vitam.common.security.rest.VitamAuthentication;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.storage.engine.common.model.request.OfferDiagRequest;
import fr.gouv.vitam.storage.offers.core.diag.OfferDiagService;
import fr.gouv.vitam.storage.offers.core.diag.OfferDiagStatus;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import static fr.gouv.vitam.common.GlobalDataRest.X_REQUEST_ID;

/**
 * Offer diagnostic resource.
 */

@Path("/offer/v1")
@ApplicationPath("webresources")
@Tag(name = "Default-Offer")
public class AdminOfferDiagResource {

    /**
     * Vitam Logger.
     */
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AdminOfferDiagResource.class);

    private final OfferDiagService offerDiagService;

    /**
     * Constructor.
     */
    public AdminOfferDiagResource(OfferDiagService offerDiagService) {
        this.offerDiagService = offerDiagService;
    }

    @Path("/diag")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @VitamAuthentication(authentLevel = AuthenticationLevel.BASIC_AUTHENT)
    public Response startOfferDiag(OfferDiagRequest offerDiagRequest) {
        ParametersChecker.checkParameter("Container is mandatory.", offerDiagRequest.getContainer());
        ParametersChecker.checkParameter("TenantId is mandatory.", offerDiagRequest.getTenantId());

        if (!VitamConfiguration.getTenants().contains(offerDiagRequest.getTenantId())) {
            throw new IllegalArgumentException("Invalid tenant " + offerDiagRequest.getTenantId());
        }

        VitamThreadUtils.getVitamSession().setTenantId(offerDiagRequest.getTenantId());
        VitamThreadUtils.getVitamSession()
            .setRequestId(GUIDFactory.newRequestIdGUID(VitamConfiguration.getAdminTenant()));

        boolean started = offerDiagService.startOfferDiag(offerDiagRequest.getContainer());

        if (!started) {
            LOGGER.warn("Another offer diagnostic process is already running");
            return Response.status(Response.Status.CONFLICT)
                .header(X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
                .build();
        }

        LOGGER.info("Offer diagnostic started");
        return Response.status(Response.Status.OK)
            .header(X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId())
            .build();
    }

    /**
     * Returns offer diagnostic process running status in a "Running" header (true/false).
     */
    @Path("/diag")
    @HEAD
    @VitamAuthentication(authentLevel = AuthenticationLevel.BASIC_AUTHENT)
    public Response isOfferDiagRunning() {
        return Response.ok().header("Running", this.offerDiagService.isRunning()).build();
    }

    /**
     * Returns the offer diagnostic status of the last diagnostic process (diagnostic process may be done, or still running)
     */
    @Path("/diag")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @VitamAuthentication(authentLevel = AuthenticationLevel.BASIC_AUTHENT)
    public Response getLastOfferDiagStatus() {
        OfferDiagStatus lastStatus = this.offerDiagService.getLastOfferDiagStatus();

        if (lastStatus == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.ok(lastStatus).build();
    }
}
