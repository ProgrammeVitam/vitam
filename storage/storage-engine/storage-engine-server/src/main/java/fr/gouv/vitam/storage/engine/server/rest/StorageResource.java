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

package fr.gouv.vitam.storage.engine.server.rest;

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
import javax.ws.rs.core.Response.Status;

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.storage.engine.common.StorageConstants;

/**
 * Storage Resource implementation
 */
@Path("/storage/v1")
public class StorageResource {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(StorageResource.class);

    /**
     * Constructor
     *
     * @param configuration the storage configuration to be applied
     */
    public StorageResource(StorageConfiguration configuration) {
        super();
        LOGGER.info("init Storage Resource server");
    }

    /**
     * Return a response status
     *
     * @return Response
     */
    @Path("status")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response status() {
        return Response.status(Status.OK).build();
    }

    /**
     * Get a list of containers
     *
     * @param headers http header
     * @return Response NOT_IMPLEMENTED
     */
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_OCTET_STREAM, StorageConstants.APPLICATION_ZIP})
    public Response getContainer(@Context HttpHeaders headers) {
        return Response.status(Status.NOT_IMPLEMENTED).build();
    }

    /**
     * Post a new container
     * 
     * @param headers http header
     * @return Response NOT_IMPLEMENTED
     */
    // TODO : check the existence, in the headers, of the value X-Http-Method-Override, if set
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createContainer(@Context HttpHeaders headers) {
        return Response.status(Status.NOT_IMPLEMENTED).build();
    }

    /**
     * Delete a container
     *
     * @param headers http header
     * @return Response NOT_IMPLEMENTED
     */
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteContainer(@Context HttpHeaders headers) {
        return Response.status(Status.NOT_IMPLEMENTED).build();
    }

    /**
     * Check the existence of a container
     *
     * @param headers http header
     * @return Response NOT_IMPLEMENTED
     */
    @HEAD
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response checkContainer(@Context HttpHeaders headers) {
        return Response.status(Status.NOT_IMPLEMENTED).build();
    }


    /**
     * Get a list of objects
     *
     * @param headers http header
     * @return Response NOT_IMPLEMENTED
     */
    @Path("/objects")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getObjects(@Context HttpHeaders headers) {
        return Response.status(Status.NOT_IMPLEMENTED).build();
    }

    /**
     * Get an object
     *
     * @param headers http header
     * @param objectId the id of the object
     * @return Response NOT_IMPLEMENTED
     */
    @Path("/objects/{id_object}")
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_OCTET_STREAM, StorageConstants.APPLICATION_ZIP})
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getObject(@Context HttpHeaders headers, @PathParam("id_object") String objectId) {
        return Response.status(Status.NOT_IMPLEMENTED).build();
    }

    /**
     * Post a new object
     * 
     * @param object the object to be created in the container
     * @param headers http header
     * @param objectId the id of the object
     * @return Response NOT_IMPLEMENTED
     */
    // TODO : check the existence, in the headers, of the value X-Http-Method-Override, if set
    @Path("/objects/{id_object}")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createObject(InputStream object, @Context HttpHeaders headers,
        @PathParam("id_object") String objectId) {
        return Response.status(Status.NOT_IMPLEMENTED).build();
    }

    /**
     * Delete an object
     *
     * @param headers http header
     * @param objectId the id of the object
     * @return Response NOT_IMPLEMENTED
     */
    @Path("/objects/{id_object}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteObject(@Context HttpHeaders headers, @PathParam("id_object") String objectId) {
        return Response.status(Status.NOT_IMPLEMENTED).build();
    }

    /**
     * Check the existence of an object
     *
     * @param headers http header
     * @param objectId the id of the object
     * @return Response NOT_IMPLEMENTED
     */
    @Path("/objects/{id_object}")
    @HEAD
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response checkObject(@Context HttpHeaders headers, @PathParam("id_object") String objectId) {
        return Response.status(Status.NOT_IMPLEMENTED).build();
    }


    /**
     * Get a list of logbooks
     *
     * @param headers http header
     * @return Response NOT_IMPLEMENTED
     */
    @Path("/logbooks")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getLogbooks(@Context HttpHeaders headers) {
        return Response.status(Status.NOT_IMPLEMENTED).build();
    }

    /**
     * Get an object
     *
     * @param headers http header
     * @param logbookId the id of the logbook
     * @return Response NOT_IMPLEMENTED
     */
    @Path("/logbooks/{id_logbook}")
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_OCTET_STREAM, StorageConstants.APPLICATION_ZIP})
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getLogbook(@Context HttpHeaders headers, @PathParam("id_logbook") String logbookId) {
        return Response.status(Status.NOT_IMPLEMENTED).build();
    }

    /**
     * Post a new object
     * 
     * @param logbook the logbook to be created
     * @param headers http header
     * @param logbookId the id of the logbookId
     * @return Response NOT_IMPLEMENTED
     */
    // TODO specify the Logbook Object to be created
    // TODO : check the existence, in the headers, of the value X-Http-Method-Override, if set
    @Path("/logbooks/{id_logbook}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createLogbook(Object logbook, @Context HttpHeaders headers,
        @PathParam("id_logbook") String logbookId) {
        return Response.status(Status.NOT_IMPLEMENTED).build();
    }

    /**
     * Delete a logbook
     *
     * @param headers http header
     * @param logbookId the id of the logbook
     * @return Response NOT_IMPLEMENTED
     */
    @Path("/logbooks/{id_logbook}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteLogbook(@Context HttpHeaders headers, @PathParam("id_logbook") String logbookId) {
        return Response.status(Status.NOT_IMPLEMENTED).build();
    }

    /**
     * Check the existence of a logbook
     *
     * @param headers http header
     * @param logbookId the id of the logbook
     * @return Response NOT_IMPLEMENTED
     */
    @Path("/logbooks/{id_logbook}")
    @HEAD
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response checkLogbook(@Context HttpHeaders headers, @PathParam("id_logbook") String logbookId) {
        return Response.status(Status.NOT_IMPLEMENTED).build();
    }


    /**
     * Get a list of units
     *
     * @param headers http header
     * @return Response NOT_IMPLEMENTED
     */
    @Path("/units")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getUnits(@Context HttpHeaders headers) {
        return Response.status(Status.NOT_IMPLEMENTED).build();
    }

    /**
     * Get a unit
     *
     * @param headers http header
     * @param metadataId the id of the unit metadata
     * @return Response NOT_IMPLEMENTED
     */
    @Path("/units/{id_md}")
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_OCTET_STREAM, StorageConstants.APPLICATION_ZIP})
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getUnit(@Context HttpHeaders headers, @PathParam("id_md") String metadataId) {
        return Response.status(Status.NOT_IMPLEMENTED).build();
    }

    /**
     * Post a new unit metadata
     * 
     * @param headers http header
     * @param metadataId the id of the unit metadata
     * @return Response NOT_IMPLEMENTED
     */
    // TODO : check the existence, in the headers, of the value X-Http-Method-Override, if set
    @Path("/units/{id_md}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createUnitMetadata(@Context HttpHeaders headers, @PathParam("id_md") String metadataId) {
        return Response.status(Status.NOT_IMPLEMENTED).build();
    }

    /**
     * Update a unit metadata
     * 
     * @param headers http header
     * @param metadataId the id of the unit metadata
     * @return Response NOT_IMPLEMENTED
     */
    @Path("/units/{id_md}")
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateUnitMetadata(@Context HttpHeaders headers, @PathParam("id_md") String metadataId) {
        return Response.status(Status.NOT_IMPLEMENTED).build();
    }

    /**
     * Delete a unit metadata
     *
     * @param headers http header
     * @param metadataId the id of the unit metadata
     * @return Response NOT_IMPLEMENTED
     */
    @Path("/units/{id_md}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteUnit(@Context HttpHeaders headers, @PathParam("id_md") String metadataId) {
        return Response.status(Status.NOT_IMPLEMENTED).build();
    }

    /**
     * Check the existence of a unit metadata
     * 
     * @param headers http header
     * @param metadataId the id of the unit metadata
     * @return Response NOT_IMPLEMENTED
     */
    @Path("/units/{id_md}")
    @HEAD
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response checkUnit(@Context HttpHeaders headers, @PathParam("id_md") String metadataId) {
        return Response.status(Status.NOT_IMPLEMENTED).build();
    }

    /**
     * Get a list of Object Groups
     *
     * @param headers http header
     * @return Response NOT_IMPLEMENTED
     */
    @Path("/objectgroups")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getObjectGroups(@Context HttpHeaders headers) {
        return Response.status(Status.NOT_IMPLEMENTED).build();
    }

    /**
     * Get a Object Group
     *
     * @param headers http header
     * @param metadataId the id of the Object Group metadata
     * @return Response NOT_IMPLEMENTED
     */
    @Path("/objectgroups/{id_md}")
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_OCTET_STREAM, StorageConstants.APPLICATION_ZIP})
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getObjectGroup(@Context HttpHeaders headers, @PathParam("id_md") String metadataId) {
        return Response.status(Status.NOT_IMPLEMENTED).build();
    }

    /**
     * Post a new Object Group metadata
     * 
     * @param headers http header
     * @param metadataId the id of the Object Group metadata
     * @return Response NOT_IMPLEMENTED
     */
    // TODO : check the existence, in the headers, of the value X-Http-Method-Override, if set
    @Path("/objectgroups/{id_md}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createObjectGroup(@Context HttpHeaders headers, @PathParam("id_md") String metadataId) {
        return Response.status(Status.NOT_IMPLEMENTED).build();
    }

    /**
     * Update a Object Group metadata
     * 
     * @param headers http header
     * @param metadataId the id of the unit metadata
     * @return Response NOT_IMPLEMENTED
     */
    @Path("/objectgroups/{id_md}")
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateObjectGroupMetadata(@Context HttpHeaders headers, @PathParam("id_md") String metadataId) {
        return Response.status(Status.NOT_IMPLEMENTED).build();
    }

    /**
     * Delete a Object Group metadata
     *
     * @param headers http header
     * @param metadataId the id of the Object Group metadata
     * @return Response NOT_IMPLEMENTED
     */
    @Path("/objectgroups/{id_md}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteObjectGroup(@Context HttpHeaders headers, @PathParam("id_md") String metadataId) {
        return Response.status(Status.NOT_IMPLEMENTED).build();
    }

    /**
     * Check the existence of a Object Group metadata
     *
     * @param headers http header
     * @param metadataId the id of the Object Group metadata
     * @return Response NOT_IMPLEMENTED
     */
    @Path("/objectgroups/{id_md}")
    @HEAD
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response checkObjectGroup(@Context HttpHeaders headers, @PathParam("id_md") String metadataId) {
        return Response.status(Status.NOT_IMPLEMENTED).build();
    }

}
