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
package fr.gouv.vitam.storage.offers.common.rest;

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
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;

import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.application.AsyncInputStreamHelper;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.common.stream.SizedInputStream;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.storage.engine.common.StorageConstants;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.ObjectInit;
import fr.gouv.vitam.storage.engine.common.model.response.RequestResponseError;
import fr.gouv.vitam.storage.offers.common.core.DefaultOfferServiceImpl;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

/**
 * Default offer REST Resource
 */
@Path("/offer/v1")
@javax.ws.rs.ApplicationPath("webresources")
public class DefaultOfferResource extends ApplicationStatusResource {

    private static final String MISSING_THE_TENANT_ID_X_TENANT_ID =
        "Missing the tenant ID (X-Tenant-Id) or wrong object Type";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(DefaultOfferResource.class);
    private static final String DEFAULT_OFFER_MODULE = "DEFAULT_OFFER";
    private static final String CODE_VITAM = "code_vitam";

    /**
     * Constructor
     */
    public DefaultOfferResource() {
        LOGGER.debug("DefaultOfferResource initialized");
    }

    /**
     * Get the information on the offer objects collection (free and used capacity, etc)
     *
     * @param xTenantId
     * @param type The container type
     * @return information on the offer objects collection
     *
     */
    // TODO P1 : review path and java method name
    // FIXME P1 il manque le /container/id/
    @GET
    @Path("/objects/{type}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCapacity(@HeaderParam(GlobalDataRest.X_TENANT_ID) String xTenantId,
        @PathParam("type") DataCategory type) {
        final String containerName = buildContainerName(type, xTenantId);
        if (Strings.isNullOrEmpty(xTenantId) || Strings.isNullOrEmpty(containerName)) {
            LOGGER.error(MISSING_THE_TENANT_ID_X_TENANT_ID);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        try {
            ObjectNode result = (ObjectNode) DefaultOfferServiceImpl.getInstance().getCapacity(containerName);
            // TODO: fix this, why tenantID in response (have to be a header, so why ?)
            result.put("tenantId", xTenantId);
            return Response.status(Response.Status.OK).entity(result).build();
        } catch (final ContentAddressableStorageNotFoundException exc) {
            LOGGER.error(exc);
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).build();
        } catch (final ContentAddressableStorageServerException exc) {
            LOGGER.error(exc);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }


    /**
     * Count the number of objects on the offer objects defined container (exlude directories)
     *
     * @param xTenantId
     * @param xContainerName
     * @return number of binary objects in the container
     *
     */
    @GET
    @Path("/objects/{type}/count")
    @Produces(MediaType.APPLICATION_JSON)
    public Response countObjects(@HeaderParam(GlobalDataRest.X_TENANT_ID) String xTenantId,
        @PathParam("type") DataCategory type) {
        final String containerName = buildContainerName(type, xTenantId);
        if (Strings.isNullOrEmpty(xTenantId) || Strings.isNullOrEmpty(containerName)) {
            LOGGER.error(MISSING_THE_TENANT_ID_X_TENANT_ID);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        try {
            final JsonNode result = DefaultOfferServiceImpl.getInstance().countObjects(containerName);
            return Response.status(Response.Status.OK).entity(result).build();
        } catch (final ContentAddressableStorageNotFoundException exc) {
            LOGGER.error(exc);
            return Response.status(Response.Status.NOT_FOUND).build();
        } catch (final ContentAddressableStorageServerException exc) {
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
     * @param type Object type
     * @param objectId object id :.+ in order to get all path if some '/' are provided
     * @param headers http header
     * @param asyncResponse async response
     * @throws IOException when there is an error of get object
     */
    @GET
    @Path("/objects/{type}/{id_object:.+}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_OCTET_STREAM, CommonMediaType.ZIP})
    public void getObject(@PathParam("type") DataCategory type, @PathParam("id_object") String objectId,
        @Context HttpHeaders headers,
        @Suspended final AsyncResponse asyncResponse) throws IOException {

        VitamThreadPoolExecutor.getDefaultExecutor().execute(new Runnable() {

            @Override
            public void run() {
                getObjectAsync(type, objectId, headers, asyncResponse);
            }
        });
    }

    /**
     * Check an Object
     *
     * @param type Object type
     * @param idObject the id of the object to be tested
     * @return the response with a specific HTTP status
     */
    // TODO : After analyzing US #2005 - change to @HEAD (without /check)  
    @GET
    @Path("/objects/{type}/{id:.+}/check")
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkObject(@HeaderParam(GlobalDataRest.X_TENANT_ID) String xTenantId,
        @HeaderParam(GlobalDataRest.X_DIGEST) String xDigest,
        @HeaderParam(GlobalDataRest.X_DIGEST_ALGORITHM) String xDigestAlgorithm,
        @PathParam("type") DataCategory type,
        @PathParam("id") String idObject) {
        if (Strings.isNullOrEmpty(xTenantId)) {
            LOGGER.error(MISSING_THE_TENANT_ID_X_TENANT_ID);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        if (Strings.isNullOrEmpty(xDigestAlgorithm)) {
            LOGGER.error("Missing the digest type (X-digest-algorithm)");
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        if (Strings.isNullOrEmpty(xDigest)) {
            LOGGER.error("Missing the type (X-digest)");
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(Integer.parseInt(xTenantId)));

        try {
            final String containerName = buildContainerName(type, xTenantId);
            boolean objectIsOK = DefaultOfferServiceImpl.getInstance().checkObject(containerName, idObject, xDigest,
                DigestType.fromValue(xDigestAlgorithm));
            final ObjectNode result = JsonHandler.createObjectNode();
            result.put(StorageConstants.OBJECT_VERIFICATION, objectIsOK);
            return Response.status(Response.Status.OK).entity(result).build();
        } catch (ContentAddressableStorageException exc) {
            LOGGER.error(exc);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Initialise a new object.
     * <p>
     * HEADER X-Command (mandatory) : INIT <br>
     * HEADER X-Tenant-Id (mandatory) : tenant's identifier
     * </p>
     *
     * @param type New object's type
     * @param objectGUID the GUID Of the object
     * @param headers http header
     * @param objectInit data for object creation
     * @return structured response with the object id
     */
    // TODO - us#1982 - to be changed with this story - tenantId to stay in the header but path (type unit or object) in
    // the uri
    @POST
    @Path("/objects/{type}/{guid:.+}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response postObject(@PathParam("guid") String objectGUID, @PathParam("type") DataCategory type,
        @Context HttpHeaders headers, ObjectInit objectInit) {
        final String xTenantId = headers.getHeaderString(GlobalDataRest.X_TENANT_ID);
        if (objectInit == null) {
            LOGGER.error(MISSING_THE_TENANT_ID_X_TENANT_ID);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        final String containerName = buildContainerName(type, xTenantId);
        if (Strings.isNullOrEmpty(xTenantId) || Strings.isNullOrEmpty(containerName)) {
            LOGGER.error(MISSING_THE_TENANT_ID_X_TENANT_ID);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        if (!objectInit.getType().equals(type)) {
            LOGGER.error(MISSING_THE_TENANT_ID_X_TENANT_ID);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        final String xCommandHeader = headers.getHeaderString(GlobalDataRest.X_COMMAND);
        if (xCommandHeader == null || !xCommandHeader.equals(StorageConstants.COMMAND_INIT)) {
            LOGGER.error("Missing the INIT required command (X-Command header)");
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        try {
            final ObjectInit objectInitFilled =
                DefaultOfferServiceImpl.getInstance().initCreateObject(containerName, objectInit, objectGUID);
            return Response.status(Response.Status.CREATED).entity(objectInitFilled).build();
        } catch (final ContentAddressableStorageException exc) {
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
     * @param type Object type to update
     * @param objectId object id
     * @param headers http header
     * @param input object data
     * @return structured response with the object id (and new digest ?)
     */
    // TODO - us#1982 - to be changed with this story - tenantId to stay in the header but path (type unit or object) in
    // the uri
    @PUT
    @Path("/objects/{type}/{id:.+}")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public Response putObject(@PathParam("type") DataCategory type, @PathParam("id") String objectId,
        @Context HttpHeaders headers, InputStream input) {
        final String xTenantId = headers.getHeaderString(GlobalDataRest.X_TENANT_ID);
        final String containerName = buildContainerName(type, xTenantId);
        if (Strings.isNullOrEmpty(xTenantId) || Strings.isNullOrEmpty(containerName)) {
            LOGGER.error(MISSING_THE_TENANT_ID_X_TENANT_ID);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        final String xCommandHeader = headers.getHeaderString(GlobalDataRest.X_COMMAND);
        if (xCommandHeader == null || !xCommandHeader.equals(StorageConstants.COMMAND_WRITE) && !xCommandHeader
            .equals(StorageConstants.COMMAND_END)) {
            LOGGER.error("Missing the WRITE or END required command (X-Command header), {} found",
                xCommandHeader);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        try {
            final SizedInputStream sis = new SizedInputStream(input);
            final String digest = DefaultOfferServiceImpl.getInstance().createObject(containerName, objectId, sis,
                xCommandHeader.equals(StorageConstants.COMMAND_END));
            return Response.status(Response.Status.CREATED).entity("{\"digest\":\"" + digest + "\",\"size\":\"" + sis
                .getSize() + "\"}").build();
        } catch (IOException | ContentAddressableStorageException exc) {
            LOGGER.error("Cannot create object", exc);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            StreamUtils.closeSilently(input);
        }
    }

    /**
     * Delete an Object
     *
     * @param type Object type to delete
     * @param idObject the id of the object to be tested
     * @return the response with a specific HTTP status
     */
    @DELETE
    @Path("/objects/{type}/{id:.+}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteObject(@Context HttpHeaders headers, @PathParam("type") DataCategory type,
        @PathParam("id") String idObject) {
        final String xTenantId = headers.getHeaderString(GlobalDataRest.X_TENANT_ID);
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(Integer.parseInt(xTenantId)));
        final Status status = Status.NOT_IMPLEMENTED;
        return Response.status(status).entity(getErrorEntity(status)).build();
    }

    /**
     * Test the existence of an object
     *
     * HEADER X-Tenant-Id (mandatory) : tenant's identifier
     *
     * @param type Object type to test
     * @param idObject the id of the object to be tested
     * @param xTenantId the id of the tenant
     * @param xContainerName the container Name
     * @return the response with a specific HTTP status
     */
    @HEAD
    @Path("/objects/{type}/{id:.+}")
    public Response headObject(@PathParam("type") DataCategory type, @PathParam("id") String idObject,
        @HeaderParam(GlobalDataRest.X_TENANT_ID) String xTenantId) {
        String containerName = buildContainerName(type, xTenantId);
        if (Strings.isNullOrEmpty(xTenantId) || Strings.isNullOrEmpty(containerName)) {
            LOGGER.error(MISSING_THE_TENANT_ID_X_TENANT_ID);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        try {
            if (DefaultOfferServiceImpl.getInstance().isObjectExist(containerName, idObject)) {
                return Response.status(Response.Status.NO_CONTENT).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        } catch (final ContentAddressableStorageServerException e) {
            LOGGER.error(e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    private void getObjectAsync(DataCategory type, String objectId, HttpHeaders headers, AsyncResponse asyncResponse) {
        try {
            final String xTenantId = headers.getHeaderString(GlobalDataRest.X_TENANT_ID);
            final String containerName = buildContainerName(type, xTenantId);
            if (Strings.isNullOrEmpty(xTenantId)) {
                LOGGER.error(MISSING_THE_TENANT_ID_X_TENANT_ID);
                AsyncInputStreamHelper.writeErrorAsyncResponse(asyncResponse,
                    Response.status(Status.PRECONDITION_FAILED).build());
                return;
            }
            DefaultOfferServiceImpl.getInstance().getObject(containerName, objectId, asyncResponse);
        } catch (final ContentAddressableStorageNotFoundException e) {
            LOGGER.error(e);
            buildErrorResponseAsync(VitamCode.STORAGE_NOT_FOUND, asyncResponse);
        } catch (final ContentAddressableStorageException e) {
            LOGGER.error(e);
            buildErrorResponseAsync(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR, asyncResponse);
        }

    }

    /**
     * Add error response in async response using with vitamCode
     *
     * @param vitamCode vitam error Code
     * @param asyncResponse asynchronous response
     */
    private void buildErrorResponseAsync(VitamCode vitamCode, AsyncResponse asyncResponse) {
        AsyncInputStreamHelper.writeErrorAsyncResponse(asyncResponse,
            Response.status(vitamCode.getStatus()).entity(new RequestResponseError().setError(
                new VitamError(VitamCodeHelper.getCode(vitamCode))
                    .setContext(vitamCode.getService().getName())
                    .setState(vitamCode.getDomain().getName())
                    .setMessage(vitamCode.getMessage())
                    .setDescription(vitamCode.getMessage()))
                .toString()).build());
    }

    private VitamError getErrorEntity(Status status) {
        return new VitamError(status.name()).setHttpCode(status.getStatusCode()).setContext(DEFAULT_OFFER_MODULE)
            .setState(CODE_VITAM).setMessage(status.getReasonPhrase()).setDescription(status.getReasonPhrase());
    }

    private String buildContainerName(DataCategory type, String tenantId) {
        if (type == null || Strings.isNullOrEmpty(type.getFolder()) || Strings.isNullOrEmpty(tenantId)) {
            return null;
        }
        return new StringBuilder(type.getFolder()).append('_').append(tenantId).toString();
    }
}
