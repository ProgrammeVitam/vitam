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
package fr.gouv.vitam.functional.administration.rest;

import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.configuration.PublicConfiguration;
import fr.gouv.vitam.functional.administration.common.config.AdminManagementConfiguration;
import fr.gouv.vitam.functional.administration.core.configuration.PublicConfigurationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/adminmanagement/v1")
@Tag(name = "Functional-Administration")
public class AdminPublicConfigurationResource {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AdminPublicConfigurationResource.class);

    private static final String FUNCTIONAL_ADMINISTRATION_MODULE = "FUNCTIONAL_ADMINISTRATION_MODULE";
    private final PublicConfigurationService publicConfigurationService;

    public AdminPublicConfigurationResource(AdminManagementConfiguration adminManagementConfiguration) {
        this.publicConfigurationService = new PublicConfigurationService(adminManagementConfiguration);
    }

    @Path("/configuration")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPublicConfiguration() {
        try {
            PublicConfiguration publicConfiguration = publicConfigurationService.getPublicConfiguration();
            return new RequestResponseOK<PublicConfiguration>()
                .addResult(publicConfiguration)
                .setHttpCode(Response.Status.OK.getStatusCode())
                .toResponse();
        } catch (Exception e) {
            LOGGER.error("Unexpected server error", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(getErrorEntity(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage(), null))
                .build();
        }
    }

    private <T> VitamError<T> getErrorEntity(Response.Status status, String message, String code) {
        String aMessage = messageFromReason(status, message);
        String aCode = (code != null) ? code : String.valueOf(status.getStatusCode());
        return new VitamError<T>(aCode)
            .setHttpCode(status.getStatusCode())
            .setContext(FUNCTIONAL_ADMINISTRATION_MODULE)
            .setState("code_vitam")
            .setMessage(status.getReasonPhrase())
            .setDescription(aMessage);
    }

    private String messageFromReason(Response.Status status, String message) {
        return (message != null && !message.trim().isEmpty())
            ? message
            : (status.getReasonPhrase() != null ? status.getReasonPhrase() : status.name());
    }
}
