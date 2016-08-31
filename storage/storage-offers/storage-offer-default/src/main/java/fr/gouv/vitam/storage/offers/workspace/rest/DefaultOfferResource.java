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

import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.HeaderParam;
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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ServerIdentity;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.StatusMessage;
import fr.gouv.vitam.storage.engine.common.StorageConstants;
import fr.gouv.vitam.storage.engine.common.model.ObjectInit;
import fr.gouv.vitam.storage.offers.workspace.core.DefaultOfferServiceImpl;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

/**
 * Default offer REST Resource
 */
@Path("/offer/v1")
public class DefaultOfferResource {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(DefaultOfferResource.class);

    /**
     * Constructor
     *
     * @param configuration the workspace offer configuration to be applied
     */
    public DefaultOfferResource(DefaultOfferConfiguration configuration) {
        LOGGER.debug("DefaultOfferResource initialized");
    }

    /**
     * Get the information on the offer objects collection (free and used capacity, etc)
     *
     * @return information on the offer objects collection
     *
     * TODO: review path and java method name
     */
    // FIXME il manque le /container/id/
    @GET
    @Path("/objects")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCapacity(@HeaderParam(GlobalDataRest.X_TENANT_ID) String xTenantId) {
        if (Strings.isNullOrEmpty(xTenantId)) {
            LOGGER.error("Missing the tenant ID (X-Tenant-Id)");
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        try {
            JsonNode result = DefaultOfferServiceImpl.getInstance().getCapacity(xTenantId);
            return Response.status(Response.Status.OK).entity(result).build();
        } catch (ContentAddressableStorageNotFoundException exc) {
            LOGGER.error(exc);
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).build();
        } catch (ContentAddressableStorageServerException exc) {
            LOGGER.error(exc);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get the object data or digest from its id.
     * <p>
     * HEADER X-Tenant-Id (mandatory) : tenant's identifier HEADER "X-type" (optional) : data (dfault) or digest
     * </p>
     *
     * @param objectId object id
     * @param headers http header
     * @return object data or digest
     * @throws IOException when there is an error of get object
     */
    @GET
    @Path("/objects/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(value = {MediaType.APPLICATION_JSON, MediaType.APPLICATION_OCTET_STREAM})
    public Response getObject(@PathParam("id") String objectId, @Context HttpHeaders headers) throws IOException {
        String xTenantId = headers.getHeaderString(GlobalDataRest.X_TENANT_ID);
        if (Strings.isNullOrEmpty(xTenantId)) {
            LOGGER.error("Missing the tenant ID (X-Tenant-Id)");
            return Response.status(Response.Status.PRECONDITION_FAILED).build();
        }
        InputStream stream = null;
        try {
            stream = DefaultOfferServiceImpl.getInstance().getObject(xTenantId, objectId);
            return Response.ok(stream, MediaType.APPLICATION_OCTET_STREAM).header("Content-Length", stream.available())
                .build();
        } catch (ContentAddressableStorageNotFoundException e) {
            LOGGER.error(e);
            return Response.status(Status.NOT_FOUND).entity(objectId).build();
        } catch (ContentAddressableStorageException e) {
            LOGGER.error(e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(objectId).build();
        }
    }

    /**
     * Initialise a new object.
     * <p>
     * HEADER X-Command (mandatory) : INIT <br>
     * HEADER X-Tenant-Id (mandatory) : tenant's identifier
     * </p>
     *
     * @param objectGUID the GUID Of the object
     * @param headers http header
     * @param objectInit data for object creation
     * @return structured response with the object id
     */
    @POST
    @Path("/objects/{guid}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response postObject(@PathParam("guid") String objectGUID, @Context HttpHeaders headers,
        ObjectInit objectInit) {
        String xTenantId = headers.getHeaderString(GlobalDataRest.X_TENANT_ID);
        if (Strings.isNullOrEmpty(xTenantId)) {
            LOGGER.error("Missing the tenant ID (X-Tenant-Id)");
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        String xCommandHeader = headers.getHeaderString(GlobalDataRest.X_COMMAND);
        if (xCommandHeader == null || !xCommandHeader.equals(StorageConstants.COMMAND_INIT)) {
            LOGGER.error("Missing the INIT required command (X-Command header)");
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        if (objectInit == null) {
            LOGGER.error("objectInit cannot be null");
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        try {
            ObjectInit objectInitFilled =
                DefaultOfferServiceImpl.getInstance().createContainer(xTenantId, objectInit, objectGUID);
            return Response.status(Response.Status.CREATED).entity(objectInitFilled).build();
        } catch (ContentAddressableStorageException exc) {
            LOGGER.error(exc);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }


    /**
     * Write a new chunk in an object or end its creation.
     * <p>
     * HEADER X-Command (mandatory) : WRITE/END HEADER X-Tenant-Id (mandatory) : tenant's identifier
     * </p>
     *
     * @param objectId object id
     * @param headers http header
     * @param input object data
     * @return structured response with the object id (and new digest ?)
     */
    @PUT
    @Path("/objects/{id}")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public Response putObject(@PathParam("id") String objectId, @Context HttpHeaders headers, InputStream input) {
        String xTenantId = headers.getHeaderString(GlobalDataRest.X_TENANT_ID);
        if (Strings.isNullOrEmpty(xTenantId)) {
            LOGGER.error("Missing the tenant ID (X-Tenant-Id)");
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        String xCommandHeader = headers.getHeaderString(GlobalDataRest.X_COMMAND);
        if (xCommandHeader == null || (!xCommandHeader.equals(StorageConstants.COMMAND_WRITE) && !xCommandHeader
            .equals(StorageConstants.COMMAND_END))) {
            LOGGER.error(String.format("Missing the WRITE or END required command (X-Command header), %s found",
                xCommandHeader));
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        try {
            String digest = DefaultOfferServiceImpl.getInstance().createObject(xTenantId, objectId, input,
                xCommandHeader.equals(StorageConstants.COMMAND_END));
            return Response.status(Response.Status.CREATED).entity("{\"digest\":\"" + digest + "\"}").build();
        } catch (IOException | ContentAddressableStorageException exc) {
            LOGGER.error("Cannot create object", exc);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete an Object
     *
     * @param idObject the id of the object to be tested
     * @return the response with a specific HTTP status
     */
    @DELETE
    @Path("/objects/{id}")
    public Response deleteObject(@PathParam("id") String idObject) {
        return Response.status(Response.Status.NOT_IMPLEMENTED).build();
    }

    /**
     * Test the existence of an object
     *
     * HEADER X-Tenant-Id (mandatory) : tenant's identifier
     *
     * @param idObject the id of the object to be tested
     * @param xTenantId the id of the tenant
     * @return the response with a specific HTTP status
     */
    @HEAD
    @Path("/objects/{id}")
    public Response headObject(@PathParam("id") String idObject,
        @HeaderParam(GlobalDataRest.X_TENANT_ID) String xTenantId) {
        if (Strings.isNullOrEmpty(xTenantId)) {
            LOGGER.error("Missing the tenant ID (X-Tenant-Id)");
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        if (DefaultOfferServiceImpl.getInstance().isObjectExist(xTenantId, idObject)) {
            return Response.status(Response.Status.NO_CONTENT).build();
        }
        return Response.status(Response.Status.NOT_FOUND).build();
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
