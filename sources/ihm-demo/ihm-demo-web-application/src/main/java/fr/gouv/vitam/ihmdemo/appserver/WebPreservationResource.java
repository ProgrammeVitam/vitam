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
package fr.gouv.vitam.ihmdemo.appserver;

import fr.gouv.vitam.access.external.client.AccessExternalClient;
import fr.gouv.vitam.access.external.client.AccessExternalClientFactory;
import fr.gouv.vitam.access.external.client.AdminExternalClient;
import fr.gouv.vitam.access.external.client.AdminExternalClientFactory;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.PreservationRequest;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.PreservationScenarioModel;
import fr.gouv.vitam.ihmdemo.core.UserInterfaceTransactionManager;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static fr.gouv.vitam.common.GlobalDataRest.X_REQUEST_ID;
import static fr.gouv.vitam.common.json.JsonHandler.getFromString;
import static fr.gouv.vitam.ihmdemo.core.UserInterfaceTransactionManager.getVitamContext;

/**
 * WebPreservationResource class
 */
@Path("/v1/api")
@javax.ws.rs.ApplicationPath("webresources")
public class WebPreservationResource {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(WebPreservationResource.class);

    private final AdminExternalClientFactory adminExternalClientFactory;
    private final AccessExternalClientFactory accessExternalClientFactory;

    WebPreservationResource(
        AdminExternalClientFactory adminExternalClientFactory,
        AccessExternalClientFactory accessExternalClientFactory) {
        this.adminExternalClientFactory = adminExternalClientFactory;
        this.accessExternalClientFactory = accessExternalClientFactory;
    }

    WebPreservationResource() {
        this(AdminExternalClientFactory.getInstance(), AccessExternalClientFactory.getInstance());
    }

    @POST
    @Path("/preservation")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("preservation:update")
    public Response launchPreservation(@Context HttpServletRequest request, String preservationString) {

        try (AccessExternalClient client = accessExternalClientFactory.getClient()) {

            PreservationRequest preservationRequest = getFromString(preservationString, PreservationRequest.class);
            VitamContext vitamContext = getVitamContext(request);
            RequestResponse response = client.launchPreservation(vitamContext, preservationRequest);

            return Response.status(Response.Status.OK).entity(response).build();

        } catch (Exception e) {
            LOGGER.error(e);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

    @POST
    @Path("/preservationScenarios")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions("preservationScenarios:read")
    public Response getPreservationScenarios(@Context HttpServletRequest request) {

        VitamContext vitamContext = getVitamContext(request);
        RequestResponse<PreservationScenarioModel> response;
        try (AdminExternalClient client = adminExternalClientFactory.getClient()) {

            response = client.findPreservationScenario(vitamContext, new Select().getFinalSelect());

            if (response instanceof RequestResponseOK) {
                return Response.status(Response.Status.OK).entity(response).build();
            }
            return Response.status(response.getHttpCode()).entity(response).build();
        } catch (VitamClientException e) {
            LOGGER.error(e);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }
}
