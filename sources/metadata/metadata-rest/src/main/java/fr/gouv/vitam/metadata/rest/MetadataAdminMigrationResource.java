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

package fr.gouv.vitam.metadata.rest;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.error.ServiceName;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.AuthenticationLevel;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.security.rest.VitamAuthentication;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.metadata.core.migration.UnitsWithTransferRequestsMigrationService;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/metadata/v1")
@Tag(name = "Metadata")
public class MetadataAdminMigrationResource {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(MetadataAdminMigrationResource.class);
    private static final String X_THRESHOLD_PARAMETER = "X-Threshold";

    private final UnitsWithTransferRequestsMigrationService migrationService;

    public MetadataAdminMigrationResource(UnitsWithTransferRequestsMigrationService migrationService) {
        this.migrationService = migrationService;
    }

    @POST
    @Path("/migrateUnitsWithTransferRequests")
    @Produces(MediaType.APPLICATION_JSON)
    @VitamAuthentication(authentLevel = AuthenticationLevel.BASIC_AUTHENT)
    public Response migrateUnitsWithTransferRequests(
        @HeaderParam(GlobalDataRest.X_TENANT_ID) Integer xTenantId,
        @QueryParam(X_THRESHOLD_PARAMETER) @DefaultValue("100000") Integer threshold
    ) {
        try {
            ParametersChecker.checkParameter("X_TENANT_ID header is required and mustn't be null", xTenantId);
            VitamThreadUtils.getVitamSession().setTenantId(xTenantId);
            String operationId = GUIDFactory.newRequestIdGUID(xTenantId).toString();
            VitamThreadUtils.getVitamSession().setRequestId(operationId);
            VitamThreadUtils.getVitamSession()
                .setApplicationSessionId("internal-units-with-transfer-requests-migration");

            boolean migrationProceeded = migrationService.migrateUnits(threshold);

            if (migrationProceeded) {
                return new RequestResponseOK<String>()
                    .setHttpCode(Response.Status.OK.getStatusCode())
                    .addResult(operationId)
                    .toResponse();
            }

            return new RequestResponseOK<String>().setHttpCode(Response.Status.ACCEPTED.getStatusCode()).toResponse();
        } catch (IllegalArgumentException e) {
            LOGGER.error(e);
            return getResponse(
                Response.Status.BAD_REQUEST,
                e,
                "Migration failed for tenant " + xTenantId + ". Illegal parameter"
            );
        } catch (Exception e) {
            LOGGER.error(e);
            return getResponse(
                Response.Status.INTERNAL_SERVER_ERROR,
                e,
                "Migration failed for tenant " + xTenantId + ". Internal server error"
            );
        } finally {
            VitamThreadUtils.getVitamSession().setApplicationSessionId(null);
        }
    }

    private static Response getResponse(Response.Status status, Exception e, String message) {
        return new VitamError<>(status.name())
            .setHttpCode(status.getStatusCode())
            .setContext(ServiceName.METADATA.getName())
            .setMessage(message)
            .setDescription(e.getMessage())
            .toResponse();
    }
}
