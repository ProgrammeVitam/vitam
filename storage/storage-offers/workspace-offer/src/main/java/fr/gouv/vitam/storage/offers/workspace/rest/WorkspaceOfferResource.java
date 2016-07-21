/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.storage.offers.workspace.rest;

import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import fr.gouv.vitam.common.ServerIdentity;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.storage.offers.workspace.core.ObjectInit;
import fr.gouv.vitam.storage.offers.workspace.core.StatusMessage;

/**
 * Workspace offer REST Resource
 */
@Path("/offer/v1")
public class WorkspaceOfferResource {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(WorkspaceOfferResource.class);

    /**
     * Constructor
     *
     * @param configuration
     */
    public WorkspaceOfferResource(WorkspaceOfferConfiguration configuration) {
        LOGGER.debug("WorkspaceOfferResource initialized");
    }

    /**
     * Get the informations on the offer objects collection (free and used capacity, etc)
     *
     * @param query the query to get objects
     * @return informations on the offer objects collection
     */
    @GET
    @Path("/objects")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getObjects() {
        return Response.status(Response.Status.NOT_IMPLEMENTED).build();
    }

    /**
     * Get the object data or digest from its id.
     * <p>
     * HEADER "X-type" (optional) : data (dfault) or digest
     * </p>
     *
     * @param objectId object id
     * @return object data or digest
     */
    @GET
    @Path("/objects/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(value = {MediaType.APPLICATION_JSON, MediaType.APPLICATION_OCTET_STREAM})
    public Response getObject(@PathParam("id") String objectId) {
        return Response.status(Response.Status.NOT_IMPLEMENTED).build();
    }

    /**
     * Initialise a new object.
     * <p>
     * HEADER X-Command (mandatory) : INIT <br>
     * HEADER X-Tenant-Id (mandatory) : tenant's identifier
     * </p>
     *
     * @param objectInit data for object creation
     * @return structured response with the object id
     */
    @POST
    @Path("/objects")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response postObject(ObjectInit objectInit) {
        return Response.status(Response.Status.NOT_IMPLEMENTED).build();
    }


    /**
     * Write a new chunk in an object or end its creation.
     * <p>
     * HEADER X-Command (mandatory) : WRITE/END
     * </p>
     *
     * @param objectId object id
     * @param input object data (on WRITE) ou digest (on END)
     * @return structured response with the object id (and new digest ?)
     */
    @PUT
    @Path("/objects/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response putObject(@PathParam("id") String objectId, InputStream input) {
        return Response.status(Response.Status.NOT_IMPLEMENTED).build();
    }

    /**
     * Delete an Object
     *
     * @param idObject the id of the object to be tested
     * @param headers headers HTTP added to the request
     * @return the response with a specific HTTP status
     */
    @DELETE
    @Path("/objects/{id}")
    public Response deleteObject(@PathParam("ido") String idObject) {
        return Response.status(Response.Status.NOT_IMPLEMENTED).build();
    }

    /**
     * Test the existence of an object
     *
     * @param idObject the id of the object to be tested
     * @param headers headers HTTP added to the request
     * @return the response with a specific HTTP status
     */
    @HEAD
    @Path("/objects/{id}")
    public Response headObject(@PathParam("ido") String idObject, @Context HttpHeaders headers) {
        return Response.status(Response.Status.NOT_IMPLEMENTED).build();
    }

    /**
     * Check the state of the offer service API
     *
     * @return an http response with OK status (200)
     */
    @GET
    @Path("/status")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStatus() {
        return Response.ok(new StatusMessage(ServerIdentity.getInstance()),
            MediaType.APPLICATION_JSON).build();
    }

}
