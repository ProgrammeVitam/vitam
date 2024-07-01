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
package fr.gouv.vitam.storage.engine.server.rest.writeprotection;

import fr.gouv.vitam.common.ServerIdentity;
import fr.gouv.vitam.common.alert.AlertService;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.logging.VitamLogLevel;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

/**
 * Filter to prevent access to WriteProtected APIs (reserved for primary site only)
 */
public class WriteProtectionForbiddenFilter implements ContainerRequestFilter {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(WriteProtectionForbiddenFilter.class);

    private final AlertService alertService;

    public WriteProtectionForbiddenFilter(AlertService alertService) {
        this.alertService = alertService;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String message =
            "SECURITY ALERT. Illegal Write API invocation on secondary site : " +
            requestContext.getMethod() +
            " " +
            requestContext.getUriInfo().getRequestUri();

        LOGGER.error(message);
        alertService.createAlert(VitamLogLevel.ERROR, message);

        final VitamError vitamError = new VitamError(
            VitamCodeHelper.getCode(VitamCode.STORAGE_ILLEGAL_WRITE_ON_SECONDARY_SITE)
        );

        vitamError
            .setContext(ServerIdentity.getInstance().getJsonIdentity())
            .setMessage(VitamCode.STORAGE_ILLEGAL_WRITE_ON_SECONDARY_SITE.getMessage())
            .setDescription(VitamCode.STORAGE_ILLEGAL_WRITE_ON_SECONDARY_SITE.getMessage())
            .setState(VitamCode.STORAGE_ILLEGAL_WRITE_ON_SECONDARY_SITE.name())
            .setHttpCode(VitamCode.STORAGE_ILLEGAL_WRITE_ON_SECONDARY_SITE.getStatus().getStatusCode());

        requestContext.abortWith(
            Response.status(vitamError.getHttpCode()).entity(vitamError).type(MediaType.APPLICATION_JSON_TYPE).build()
        );
    }
}
