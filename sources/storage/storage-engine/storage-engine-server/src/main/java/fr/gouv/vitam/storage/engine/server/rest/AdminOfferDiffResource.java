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

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.AuthenticationLevel;
import fr.gouv.vitam.common.security.rest.VitamAuthentication;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.request.OfferDiffRequest;
import fr.gouv.vitam.storage.engine.server.offerdiff.OfferDiffService;
import fr.gouv.vitam.storage.engine.server.offerdiff.OfferDiffStatus;
import fr.gouv.vitam.storage.engine.server.rest.writeprotection.WriteProtection;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static fr.gouv.vitam.common.GlobalDataRest.X_REQUEST_ID;

/**
 * Offer diff resource.
 */
@Path("/storage/v1")
@Tag(name = "Admin-Offer")
public class AdminOfferDiffResource {

    /**
     * Vitam Logger.
     */
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AdminOfferDiffResource.class);

    private final OfferDiffService offerDiffService;

    /**
     * Constructor.
     */
    public AdminOfferDiffResource(OfferDiffService offerDiffService) {
        this.offerDiffService = offerDiffService;
    }

    @Path("/diff")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @VitamAuthentication(authentLevel = AuthenticationLevel.BASIC_AUTHENT)
    @WriteProtection(true)
    public Response startOfferDiff(OfferDiffRequest offerDiffRequest) {
        ParametersChecker.checkParameter("Offer 1 is mandatory.", offerDiffRequest.getOffer1());
        ParametersChecker.checkParameter("Offer 2 is mandatory.", offerDiffRequest.getOffer2());
        ParametersChecker.checkParameter("Container is mandatory.", offerDiffRequest.getContainer());
        ParametersChecker.checkParameter("TenantId is mandatory.", offerDiffRequest.getTenantId());

        if (!VitamConfiguration.getTenants().contains(offerDiffRequest.getTenantId())) {
            throw new IllegalArgumentException("Invalid tenant " + offerDiffRequest.getTenantId());
        }

        VitamThreadUtils.getVitamSession().setTenantId(offerDiffRequest.getTenantId());
        VitamThreadUtils.getVitamSession()
            .setRequestId(GUIDFactory.newRequestIdGUID(VitamConfiguration.getAdminTenant()));

        DataCategory dataCategory = DataCategory.getByCollectionName(offerDiffRequest.getContainer());

        if (offerDiffRequest.getOffer1().equals(offerDiffRequest.getOffer2())) {
            throw new IllegalArgumentException("Offer ids to compare must not be the same");
        }

        boolean started = offerDiffService.startOfferDiff(
            offerDiffRequest.getOffer1(),
            offerDiffRequest.getOffer2(),
            dataCategory
        );

        Response.Status status;
        if (started) {
            LOGGER.info("Offer diff started");
            status = Response.Status.OK;
        } else {
            LOGGER.warn("Another offer diff process is already running");
            status = Response.Status.CONFLICT;
        }
        return Response.status(status).header(X_REQUEST_ID, VitamThreadUtils.getVitamSession().getRequestId()).build();
    }

    /**
     * Returns offer diff process running status in a "Running" header (true/false).
     */
    @Path("/diff")
    @HEAD
    @VitamAuthentication(authentLevel = AuthenticationLevel.BASIC_AUTHENT)
    @WriteProtection(true)
    public Response isOfferDiffRunning() {
        return Response.ok().header("Running", this.offerDiffService.isRunning()).build();
    }

    /**
     * Returns the offer diff status of the last diff process (diff process may be done, or still running)
     */
    @Path("/diff")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @VitamAuthentication(authentLevel = AuthenticationLevel.BASIC_AUTHENT)
    @WriteProtection(true)
    public Response getLastOfferDiffStatus() {
        OfferDiffStatus lastOfferDiffStatus = this.offerDiffService.getLastOfferDiffStatus();

        if (lastOfferDiffStatus == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.ok(lastOfferDiffStatus).build();
    }
}
