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
package fr.gouv.vitam.access.external.rest.v2.rest;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.access.external.rest.AccessExternalConfiguration;
import fr.gouv.vitam.access.internal.client.AccessInternalClient;
import fr.gouv.vitam.access.internal.client.AccessInternalClientFactory;
import fr.gouv.vitam.access.internal.common.exception.AccessInternalClientServerException;
import fr.gouv.vitam.common.dsl.schema.validator.SelectMultipleSchemaValidator;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.export.ExportRequest;
import fr.gouv.vitam.common.model.export.dip.DipRequest;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.security.rest.EndpointInfo;
import fr.gouv.vitam.common.security.rest.SecureEndpointRegistry;
import fr.gouv.vitam.common.security.rest.Secured;
import fr.gouv.vitam.common.security.rest.Unsecured;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.common.utils.SupportedSedaVersions;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.ws.rs.Consumes;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.List;

import static fr.gouv.vitam.utils.SecurityProfilePermissions.DIPEXPORTV2_CREATE;

@Path("/access-external/v2")
@Tag(name = "Access")
public class AccessExternalResourceV2 extends ApplicationStatusResource {

    private static final String PREDICATES_FAILED_EXCEPTION = "Predicates Failed Exception ";
    private static final String ACCESS_EXTERNAL_MODULE = "ACCESS_EXTERNAL";
    private static final String CODE_VITAM = "code_vitam";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AccessExternalResourceV2.class);
    private final SecureEndpointRegistry secureEndpointRegistry;
    private final AccessInternalClientFactory accessInternalClientFactory;
    private final AccessExternalConfiguration configuration;

    /**
     * Constructor
     *
     * @param secureEndpointRegistry endpoint list registry
     */
    public AccessExternalResourceV2(
        SecureEndpointRegistry secureEndpointRegistry,
        AccessExternalConfiguration configuration
    ) {
        this(secureEndpointRegistry, AccessInternalClientFactory.getInstance(), configuration);
    }

    public AccessExternalResourceV2(
        SecureEndpointRegistry secureEndpointRegistry,
        AccessInternalClientFactory accessInternalClientFactory,
        AccessExternalConfiguration configuration
    ) {
        this.secureEndpointRegistry = secureEndpointRegistry;
        this.accessInternalClientFactory = accessInternalClientFactory;
        this.configuration = configuration;
        LOGGER.debug("AccessExternalResourceV2 initialized");
    }

    /**
     * List secured resource end points
     *
     * @return response
     */
    @Path("/")
    @OPTIONS
    @Produces(MediaType.APPLICATION_JSON)
    @Unsecured
    public Response listResourceEndpoints() {
        String resourcePath = AccessExternalResourceV2.class.getAnnotation(Path.class).value();

        List<EndpointInfo> securedEndpointList = this.secureEndpointRegistry.getEndPointsByResourcePath(resourcePath);

        return Response.status(Status.OK).entity(securedEndpointList).build();
    }

    /**
     * Get a DIP by dip request (dsl query + other export options)
     *
     * @param dipRequest the query to get units
     * @return Response
     */
    @POST
    @Path("/dipexport")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = DIPEXPORTV2_CREATE, description = "Générer le DIP à partir d'un DSL")
    public Response exportDIP(DipRequest dipRequest) {
        try (AccessInternalClient client = accessInternalClientFactory.getClient()) {
            SanityChecker.checkJsonAll(dipRequest.getDslRequest());
            // Validate DSL query & seda Version
            SelectMultipleSchemaValidator validator = new SelectMultipleSchemaValidator();
            validator.validate(dipRequest.getDslRequest());
            if (
                dipRequest.getSedaVersion() != null &&
                !SupportedSedaVersions.isSedaVersionValid(dipRequest.getSedaVersion())
            ) {
                return Response.status(Status.PRECONDITION_FAILED)
                    .entity(getErrorEntity(Status.PRECONDITION_FAILED, "The Seda version is invalid!"))
                    .build();
            }

            RequestResponse<JsonNode> response = client.exportByUsageFilter(ExportRequest.from(dipRequest));
            if (response.isOk()) {
                return Response.status(Status.ACCEPTED.getStatusCode()).entity(response).build();
            } else {
                return response.toResponse();
            }
        } catch (final AccessInternalClientServerException e) {
            LOGGER.error(PREDICATES_FAILED_EXCEPTION, e);
            return Response.status(Status.PRECONDITION_FAILED)
                .entity(getErrorEntity(Status.PRECONDITION_FAILED, e.getLocalizedMessage()))
                .build();
        } catch (final Exception e) {
            LOGGER.error("Technical Exception ", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(getErrorEntity(Status.INTERNAL_SERVER_ERROR, e.getLocalizedMessage()))
                .build();
        }
    }

    @Deprecated
    private VitamError<JsonNode> getErrorEntity(Status status, String message) {
        String aMessage = (message != null && !message.trim().isEmpty())
            ? message
            : (status.getReasonPhrase() != null ? status.getReasonPhrase() : status.name());
        return new VitamError<JsonNode>(status.name())
            .setHttpCode(status.getStatusCode())
            .setContext(ACCESS_EXTERNAL_MODULE)
            .setState(CODE_VITAM)
            .setMessage(status.getReasonPhrase())
            .setDescription(aMessage);
    }
}
