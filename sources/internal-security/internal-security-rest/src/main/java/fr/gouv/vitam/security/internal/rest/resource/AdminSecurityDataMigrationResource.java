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
package fr.gouv.vitam.security.internal.rest.resource;

import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.AuthenticationLevel;
import fr.gouv.vitam.common.security.rest.VitamAuthentication;
import fr.gouv.vitam.security.internal.rest.service.migration.SecurityDataMigrationService;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/v1/api/security")
@Tag(name = "Security")
public class AdminSecurityDataMigrationResource {

    /**
     * Vitam Logger.
     */
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AdminSecurityDataMigrationResource.class);

    private final String MIGRATION_URI = "/migration";
    private final String MIGRATION_STATUS_URI = "/migration/status";

    /**
     * Security data migration service.
     */
    private final SecurityDataMigrationService securityDataMigrationService;

    /**
     * Constructor
     */
    @VisibleForTesting
    public AdminSecurityDataMigrationResource(SecurityDataMigrationService securityDataMigrationService) {
        this.securityDataMigrationService = securityDataMigrationService;
    }

    /**
     * API for security data migration
     *
     * @return the response
     */
    @Path(MIGRATION_URI)
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @VitamAuthentication(authentLevel = AuthenticationLevel.BASIC_AUTHENT)
    public Response startSecurityDataMigration() {
        try {
            boolean started = this.securityDataMigrationService.tryStartMongoDataUpdate();

            if (started) {
                LOGGER.info("Security migration started successfully");
                return Response.accepted(new ResponseMessage("OK")).build();
            } else {
                LOGGER.warn("Security migration already in progress");
                return Response.status(Response.Status.CONFLICT)
                    .entity(new ResponseMessage("Security data migration already in progress"))
                    .build();
            }
        } catch (Exception e) {
            LOGGER.error("An error occurred during security data migration", e);
            return Response.serverError().entity(new ResponseMessage(e.getMessage())).build();
        }
    }

    /**
     * API for security data migration status check
     *
     * @return the response
     */
    @Path(MIGRATION_STATUS_URI)
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response isMigrationInProgress() {
        try {
            boolean started = this.securityDataMigrationService.isMongoDataUpdateInProgress();

            if (started) {
                LOGGER.info("Security migration still in progress");
                return Response.ok(new ResponseMessage("Security migration in progress")).build();
            } else {
                LOGGER.info("No active security migration");
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ResponseMessage("No active security migration"))
                    .build();
            }
        } catch (Exception e) {
            LOGGER.error("An error occurred during security data migration", e);
            return Response.serverError().entity(new ResponseMessage(e.getMessage())).build();
        }
    }

    public static class ResponseMessage {

        private String message;

        public ResponseMessage(String message) {
            this.message = message;
        }

        public ResponseMessage() {}

        public void setMessage(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}
