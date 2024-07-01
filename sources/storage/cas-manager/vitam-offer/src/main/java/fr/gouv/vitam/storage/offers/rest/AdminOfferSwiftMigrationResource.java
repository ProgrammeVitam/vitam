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
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.AuthenticationLevel;
import fr.gouv.vitam.common.security.rest.VitamAuthentication;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.common.storage.swift.Swift;
import fr.gouv.vitam.storage.offers.migration.SwiftMigrationRequest;
import fr.gouv.vitam.storage.offers.migration.SwiftMigrationService;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/offer/v1/swift-migration")
@Tag(name = "Admin-Offer")
public class AdminOfferSwiftMigrationResource extends ApplicationStatusResource {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AdminOfferSwiftMigrationResource.class);

    private final SwiftMigrationService swiftMigrationService;

    public AdminOfferSwiftMigrationResource(Swift swiftContentAddressableStorage) {
        this.swiftMigrationService = new SwiftMigrationService(swiftContentAddressableStorage.getOsClient());
    }

    @POST
    @Path("")
    @Consumes(APPLICATION_JSON)
    @VitamAuthentication(authentLevel = AuthenticationLevel.BASIC_AUTHENT)
    public Response launchSwiftMigration(SwiftMigrationRequest swiftMigrationRequest) {
        ParametersChecker.checkParameter("Request required", swiftMigrationRequest);
        ParametersChecker.checkParameter(
            "Swift migration mode required",
            swiftMigrationRequest.getSwiftMigrationMode()
        );
        LOGGER.info("Starting swift offer migration - " + swiftMigrationRequest.getSwiftMigrationMode());

        boolean migrationStarted =
            this.swiftMigrationService.tryStartMigration(swiftMigrationRequest.getSwiftMigrationMode());

        if (!migrationStarted) {
            LOGGER.info("Swift offer migration is already running.");
            return Response.status(Response.Status.CONFLICT).build();
        }

        LOGGER.info("Swift offer migration started successfully.");
        return Response.status(Response.Status.ACCEPTED).build();
    }

    @HEAD
    @Path("")
    @Consumes(APPLICATION_JSON)
    @VitamAuthentication(authentLevel = AuthenticationLevel.BASIC_AUTHENT)
    public Response checkSwiftMigrationInProgress() {
        boolean migrationInProgress = this.swiftMigrationService.isMigrationInProgress();

        if (!migrationInProgress) {
            LOGGER.info("Swift migration still in progress");
            return Response.ok().build();
        } else {
            LOGGER.info("No active swift migration");
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @GET
    @Path("")
    @Consumes(APPLICATION_JSON)
    @VitamAuthentication(authentLevel = AuthenticationLevel.BASIC_AUTHENT)
    public Response getSwiftMigrationResult() {
        boolean hasMigrationSucceeded = this.swiftMigrationService.hasMigrationSucceeded();

        if (hasMigrationSucceeded) {
            LOGGER.info("Swift migration succeeded");
            return Response.ok().build();
        } else {
            LOGGER.error("Swift migration failed");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}
