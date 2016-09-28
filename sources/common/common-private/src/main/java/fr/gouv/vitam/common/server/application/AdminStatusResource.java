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
    private VitamStatusService statusService;

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
            ObjectNode objectNode = JsonHandler.createObjectNode();
            JsonNode node = JsonHandler.toJsonNode(ServerIdentity.getInstance());
            objectNode.set("serverIdentity", node);
            objectNode.put("status", statusService.getResourcesStatus());
            ObjectNode detail = statusService.getAdminStatus();
            if (detail.size() != 0) {
                objectNode.set("detail", statusService.getAdminStatus());
            }
            if (statusService.getResourcesStatus()) {
                return Response.ok(objectNode,
                    MediaType.APPLICATION_JSON).build();
            } else {
                return Response.status(Status.SERVICE_UNAVAILABLE).entity(objectNode).build();
            }
        } catch (InvalidParseOperationException e) {
            return Response.status(Status.SERVICE_UNAVAILABLE).build();
        }
    }
}
