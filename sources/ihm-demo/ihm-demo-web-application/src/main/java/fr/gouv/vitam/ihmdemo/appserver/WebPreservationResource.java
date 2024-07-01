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
package fr.gouv.vitam.ihmdemo.appserver;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.access.external.client.AccessExternalClient;
import fr.gouv.vitam.access.external.client.AccessExternalClientFactory;
import fr.gouv.vitam.access.external.client.AdminExternalClient;
import fr.gouv.vitam.access.external.client.AdminExternalClientFactory;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.PreservationRequest;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.preservation.GriffinModel;
import fr.gouv.vitam.common.model.administration.preservation.PreservationScenarioModel;
import fr.gouv.vitam.ihmdemo.core.DslQueryHelper;
import fr.gouv.vitam.ihmdemo.core.UserInterfaceTransactionManager;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.InputStream;
import java.util.Map;

import static fr.gouv.vitam.common.json.JsonHandler.getFromString;
import static javax.ws.rs.core.Response.status;

/**
 * WebPreservationResource class
 */
@Path("/v1/api")
@javax.ws.rs.ApplicationPath("webresources")
public class WebPreservationResource {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(WebPreservationResource.class);

    private final AdminExternalClientFactory adminExternalClientFactory;
    private final AccessExternalClientFactory accessExternalClientFactory;
    private final UserInterfaceTransactionManager userInterfaceTransactionManager;
    private final DslQueryHelper dslQueryHelper;

    @VisibleForTesting
    WebPreservationResource(
        AdminExternalClientFactory adminExternalClientFactory,
        AccessExternalClientFactory accessExternalClientFactory,
        UserInterfaceTransactionManager userInterfaceTransactionManager,
        DslQueryHelper dslQueryHelper
    ) {
        this.adminExternalClientFactory = adminExternalClientFactory;
        this.accessExternalClientFactory = accessExternalClientFactory;
        this.userInterfaceTransactionManager = userInterfaceTransactionManager;
        this.dslQueryHelper = dslQueryHelper;
    }

    WebPreservationResource() {
        this(
            AdminExternalClientFactory.getInstance(),
            AccessExternalClientFactory.getInstance(),
            UserInterfaceTransactionManager.getInstance(),
            DslQueryHelper.getInstance()
        );
    }

    @POST
    @Path("/preservation")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("preservation:update")
    public Response launchPreservation(@Context HttpServletRequest request, String preservationString) {
        try (AccessExternalClient client = accessExternalClientFactory.getClient()) {
            PreservationRequest preservationRequest = getFromString(preservationString, PreservationRequest.class);
            VitamContext vitamContext = userInterfaceTransactionManager.getVitamContext(request);
            RequestResponse response = client.launchPreservation(vitamContext, preservationRequest);

            return status(Status.OK).entity(response).build();
        } catch (Exception e) {
            LOGGER.error(e);
            return status(Status.BAD_REQUEST).build();
        }
    }

    @POST
    @Path("/preservationScenarios")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("preservationScenarios:read")
    public Response getPreservationScenarios(@Context HttpServletRequest request, String select) {
        VitamContext vitamContext = userInterfaceTransactionManager.getVitamContext(request);
        RequestResponse<PreservationScenarioModel> response;

        try (AdminExternalClient client = adminExternalClientFactory.getClient()) {
            final Map<String, Object> optionsMap = JsonHandler.getMapFromString(select);

            final JsonNode query = dslQueryHelper.createSingleQueryDSL(optionsMap);

            response = client.findPreservationScenario(vitamContext, query);

            if (response instanceof RequestResponseOK) {
                return status(Status.OK).entity(response).build();
            }
            return status(response.getHttpCode()).entity(response).build();
        } catch (Exception e) {
            LOGGER.error(e);
            return status(Status.BAD_REQUEST).build();
        }
    }

    @POST
    @Path("/griffins")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("griffins:read")
    public Response getPreservationGriffins(@Context HttpServletRequest request, String select) {
        VitamContext vitamContext = userInterfaceTransactionManager.getVitamContext(request);
        RequestResponse<GriffinModel> response;

        try (AdminExternalClient client = adminExternalClientFactory.getClient()) {
            final Map<String, Object> optionsMap = JsonHandler.getMapFromString(select);

            final JsonNode query = dslQueryHelper.createSingleQueryDSL(optionsMap);

            response = client.findGriffin(vitamContext, query);
            if (response instanceof RequestResponseOK) {
                return status(Status.OK).entity(response).build();
            }
            return status(response.getHttpCode()).entity(response).build();
        } catch (final Exception e) {
            LOGGER.error(e);
            return status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @POST
    @Path("/griffins")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("griffins:create")
    public Response uploadGriffins(@Context HttpServletRequest request, InputStream input) {
        try (final AdminExternalClient adminClient = AdminExternalClientFactory.getInstance().getClient()) {
            RequestResponse response = adminClient.importGriffin(
                userInterfaceTransactionManager.getVitamContext(request),
                input,
                request.getHeader(GlobalDataRest.X_FILENAME)
            );

            if (response instanceof RequestResponseOK) {
                return status(Status.OK).build();
            }
            if (response instanceof VitamError) {
                LOGGER.error(response.toString());
                return status(response.getStatus()).entity(response).build();
            }
            return status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (final Exception e) {
            LOGGER.error(e);
            return status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @POST
    @Path("/scenarios")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("preservationScenarios:create")
    public Response uploadPreservationScenario(@Context HttpServletRequest request, InputStream input) {
        try (final AdminExternalClient adminClient = AdminExternalClientFactory.getInstance().getClient()) {
            RequestResponse response = adminClient.importPreservationScenario(
                userInterfaceTransactionManager.getVitamContext(request),
                input,
                request.getHeader(GlobalDataRest.X_FILENAME)
            );
            if (response instanceof RequestResponseOK) {
                return status(Status.OK).build();
            }
            if (response instanceof VitamError) {
                LOGGER.error(response.toString());
                return status(response.getStatus()).entity(response).build();
            }
            return status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (final Exception e) {
            LOGGER.error(e);
            return status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Path("griffin/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("griffin:read")
    public Response getGriffinById(@Context HttpServletRequest request, @PathParam("id") String id) {
        try (final AdminExternalClient adminClient = AdminExternalClientFactory.getInstance().getClient()) {
            RequestResponse<GriffinModel> response = adminClient.findGriffinById(
                userInterfaceTransactionManager.getVitamContext(request),
                id
            );
            if (response instanceof RequestResponseOK) {
                return status(Status.OK).entity(response).build();
            }
            if (response instanceof VitamError) {
                LOGGER.error(response.toString());
                return status(response.getHttpCode()).entity(response).build();
            }
            return status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (final Exception e) {
            LOGGER.error("INTERNAL SERVER ERROR", e);
            return status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Path("scenario/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("preservationScenario:read")
    public Response getPreservationScenarioById(@Context HttpServletRequest request, @PathParam("id") String id) {
        try (final AdminExternalClient adminClient = AdminExternalClientFactory.getInstance().getClient()) {
            RequestResponse<PreservationScenarioModel> response = adminClient.findPreservationScenarioById(
                userInterfaceTransactionManager.getVitamContext(request),
                id
            );
            if (response instanceof RequestResponseOK) {
                return status(Status.OK).entity(response).build();
            }
            if (response instanceof VitamError) {
                LOGGER.error(response.toString());
                return status(response.getHttpCode()).entity(response).build();
            }
            return status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (final Exception e) {
            LOGGER.error("INTERNAL SERVER ERROR", e);
            return status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}
