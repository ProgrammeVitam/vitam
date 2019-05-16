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
package fr.gouv.vitam.storage.offers.common.rest;

import com.mongodb.client.MongoDatabase;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponseError;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.storage.offers.common.core.DefaultOfferService;
import fr.gouv.vitam.storage.offers.common.migration.OfferLogR7MigrationService;
import fr.gouv.vitam.storage.offers.common.migration.OfferMigrationStatus;

import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/offer/v1")
public class OfferAdminResource {

    /**
     * Vitam Logger.
     */
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(OfferAdminResource.class);
    private final OfferLogR7MigrationService offerLogR7MigrationService;

    public OfferAdminResource(DefaultOfferService defaultOfferService, MongoDatabase mongoDatabase) {
        this.offerLogR7MigrationService = new OfferLogR7MigrationService(defaultOfferService, mongoDatabase);
    }

    @POST
    @Path("/offer/offerlog_migration_r7")
    @Produces(MediaType.APPLICATION_JSON)
    public Response startMigration(
        @QueryParam("startOffset") Long startOffset) {

        try {

            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newGUID());
            boolean started = this.offerLogR7MigrationService.startMigration(startOffset);

            if (started) {
                LOGGER.info("Offer migration started successfully");
                return Response.accepted().build();
            } else {
                LOGGER.warn("Offer migration already in progress");
                return Response.status(Response.Status.CONFLICT).build();
            }

        } catch (Exception e) {
            LOGGER.error(e);
            return buildErrorResponse(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR);
        }
    }

    @HEAD
    @Path("/offer/offerlog_migration_r7")
    public Response isMigrationRunning() {

        try {
            boolean isRunning = this.offerLogR7MigrationService.isMigrationRunning();

            if (isRunning) {
                LOGGER.info("Offer migration still in progress");
                return Response.ok("Offer migration in progress").build();
            } else {
                LOGGER.info("No active offer migration");
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        } catch (Exception e) {
            LOGGER.error("An error occurred during offer migration", e);
            return buildErrorResponse(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR);
        }
    }

    @GET
    @Path("/offer/offerlog_migration_r7")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMigrationStatus() {

        OfferMigrationStatus offerMigrationStatus = this.offerLogR7MigrationService.getMigrationStatus();

        if (offerMigrationStatus == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.ok(offerMigrationStatus).build();
    }

    private Response buildErrorResponse(VitamCode vitamCode) {
        return Response.status(vitamCode.getStatus())
            .entity(new RequestResponseError().setError(new VitamError(VitamCodeHelper.getCode(vitamCode))
                .setContext(vitamCode.getService().getName()).setState(vitamCode.getDomain().getName())
                .setMessage(vitamCode.getMessage()).setDescription(vitamCode.getMessage())).toString())
            .build();
    }
}
