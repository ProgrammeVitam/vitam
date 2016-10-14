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
package fr.gouv.vitam.common.server.application;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.gouv.vitam.common.ServerIdentity;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;


/**
 *
 * AdminStatusResource : Manage Admin Functionality through Admin URI
 *
 */
@Path("/admin/v1")
@Consumes("application/json")
@Produces("application/json")
public class AdminStatusResource {
    private final VitamStatusService statusService;

    /**
     * Constructor AdminStatusResource
     *
     * @param statusService
     */
    public AdminStatusResource(VitamStatusService statusService) {
        this.statusService = statusService;
    }

    /**
     * Return a response status
     *
     * @return Response containing the status of the service
     */
    @Path("status")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response status() {
        try {
            final ObjectNode objectNode = JsonHandler.createObjectNode();
            final JsonNode node = JsonHandler.toJsonNode(ServerIdentity.getInstance());
            objectNode.set("serverIdentity", node);
            objectNode.put("status", statusService.getResourcesStatus());
            final ObjectNode detail = statusService.getAdminStatus();
            if (detail.size() != 0) {
                objectNode.set("detail", statusService.getAdminStatus());
            }
            if (statusService.getResourcesStatus()) {
                return Response.ok(objectNode,
                    MediaType.APPLICATION_JSON).build();
            } else {
                return Response.status(Status.SERVICE_UNAVAILABLE).entity(objectNode).build();
            }
        } catch (final InvalidParseOperationException e) {
            return Response.status(Status.SERVICE_UNAVAILABLE).build();
        }
    }
}
