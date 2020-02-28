/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.administration.ContextModel;
import fr.gouv.vitam.common.thread.VitamThreadUtils;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.List;

/**
 * Wrapper around ContextResource to expose many services.
 */
@Path("/v1/admin")
public class AdminContextResource {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AdminContextResource.class);

    private ContextResource contextResource;

    public static final int ADMIN_TENANT = VitamConfiguration.getAdminTenant();

    /**
     * @param contextResource
     */
    public AdminContextResource(ContextResource contextResource) {
        LOGGER.debug("init Admin Management Resource server");
        this.contextResource = contextResource;
    }

    @Path("/contexts")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response importContexts(List<ContextModel> contextModelList, @Context UriInfo uri) {
        // TODO: report this as a vitam event
        LOGGER.info("create context with admin interface");
        LOGGER.info("using of admin tenant: 1");

        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);
        return contextResource.importContexts(contextModelList, uri);
    }

    @Path("/contexts")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response findContexts(JsonNode queryDsl) {
        // TODO: report this as a vitam event
        LOGGER.info("find context with admin interface");
        LOGGER.info("using of admin tenant: 1");

        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);
        return contextResource.findContexts(queryDsl);
    }

    @Path("/context/{contextId}")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteContext(@PathParam("contextId") String contextId) {
        // TODO: report this as a vitam event
        LOGGER.info("delete context with admin interface");
        LOGGER.info("using of admin tenant: 1");

        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);
        return contextResource.deleteContext(contextId, true);
    }
}
