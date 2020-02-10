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
package fr.gouv.vitam.storage.offers.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.client.VitamRequestIterator;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponseError;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.security.SafeFileChecker;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.server.application.VitamHttpHeader;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.common.storage.ContainerInformation;
import fr.gouv.vitam.common.storage.cas.container.api.ObjectContent;
import fr.gouv.vitam.common.storage.constants.ErrorMessage;
import fr.gouv.vitam.common.stream.ExactSizeInputStream;
import fr.gouv.vitam.common.stream.MultiplexedStreamReader;
import fr.gouv.vitam.common.stream.SizedInputStream;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.stream.VitamAsyncInputStreamResponse;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.storage.driver.model.StorageBulkPutResult;
import fr.gouv.vitam.storage.driver.model.StorageMetadataResult;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.TapeReadRequestReferentialEntity;
import fr.gouv.vitam.storage.engine.common.model.request.OfferLogRequest;
import fr.gouv.vitam.storage.offers.core.DefaultOfferService;
import fr.gouv.vitam.storage.offers.core.NonUpdatableContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.api.exception.UnavailableFileException;
import org.apache.commons.lang3.StringUtils;
import org.openstack4j.api.exceptions.ConnectionException;

import javax.validation.constraints.NotNull;
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
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static fr.gouv.vitam.storage.engine.common.utils.ContainerUtils.buildContainerName;

/**
 * Default offer REST Resource
 */
@Path("/offer/v1")
@javax.ws.rs.ApplicationPath("webresources")
public class DefaultOfferResource extends ApplicationStatusResource {

    private static final String MISSING_THE_TENANT_ID_X_TENANT_ID =
        "Missing the tenant ID (X-Tenant-Id) or wrong object Type";
    private static final String MISSING_THE_BODY = "Missing the body object";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(DefaultOfferResource.class);
    private static final String DEFAULT_OFFER_MODULE = "DEFAULT_OFFER";
    private static final String CODE_VITAM = "code_vitam";
    public static final String RE_AUTHENTICATION_CALL_STREAM_ALREADY_CONSUMED_BUT_NO_FILE_CREATED =
        "Caused by re-authentication call. Stream already consumed but no file created, storage engine must retry to re-put object";
    private static final String MISSING_THE_DATA_TYPE_PARAMETER = "Missing Data Type parameter";
    private static final String MISSING_OBJECTS_IDS_LIST_PARAMETER = "Missing Objects Ids List parameter";

    private DefaultOfferService defaultOfferService;

    /**
     * Constructor
     *
     * @param defaultOfferService
     */
    public DefaultOfferResource(DefaultOfferService defaultOfferService) {
        LOGGER.debug("DefaultOfferResource initialized");
        this.defaultOfferService = defaultOfferService;
    }

    /**
     * Get the information on the offer objects collection (free and used capacity, etc)
     *
     * @param xTenantId XtenantId
     * @param type The container type
     * @return information on the offer objects collection
     */
    // TODO P1 : review java method name
    // FIXME P1 il manque le /container/id/
    @HEAD
    @Path("/objects/{type}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCapacity(@HeaderParam(GlobalDataRest.X_TENANT_ID) String xTenantId,
        @PathParam("type") DataCategory type) {
        if (Strings.isNullOrEmpty(xTenantId)) {
            LOGGER.error(MISSING_THE_TENANT_ID_X_TENANT_ID);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        final String containerName = buildContainerName(type, xTenantId);
        try {
            ContainerInformation capacity = defaultOfferService.getCapacity(containerName);
            Response.ResponseBuilder response = Response.status(Status.OK);
            response.header("X-Usable-Space", capacity.getUsableSpace());
            response.header(GlobalDataRest.X_TENANT_ID, xTenantId);
            return response.build();
        } catch (final ContentAddressableStorageNotFoundException exc) {
            LOGGER.error(ErrorMessage.CONTAINER_NOT_FOUND.getMessage() + containerName, exc);
            return Response.status(Response.Status.NOT_FOUND).build();
        } catch (final ContentAddressableStorageServerException exc) {
            LOGGER.error(exc);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get container object list
     *
     * @param xcursor if true means new query, if false means end of query from client side
     * @param xcursorId if present, means continue on cursor
     * @param xTenantId the tenant id
     * @param type object type
     * @return an iterator with each object metadata (actually only the id)
     */
    @GET
    @Path("/objects/{type}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getContainerList(@HeaderParam(GlobalDataRest.X_CURSOR) boolean xcursor,
        @HeaderParam(GlobalDataRest.X_CURSOR_ID) String xcursorId,
        @HeaderParam(GlobalDataRest.X_TENANT_ID) String xTenantId,
        @PathParam("type") DataCategory type) {
        try {
            if (Strings.isNullOrEmpty(xTenantId)) {
                LOGGER.error(MISSING_THE_TENANT_ID_X_TENANT_ID);
                final Response.ResponseBuilder builder = Response.status(Status.BAD_REQUEST);
                return VitamRequestIterator.setHeaders(builder, xcursor, null).build();
            }
            Status status;
            String cursorId = xcursorId;
            if (VitamRequestIterator.isEndOfCursor(xcursor, xcursorId)) {
                defaultOfferService.finalizeCursor(buildContainerName(type, xTenantId), xcursorId);
                final Response.ResponseBuilder builder = Response.status(Status.NO_CONTENT);
                return VitamRequestIterator.setHeaders(builder, xcursor, null).build();
            }

            if (VitamRequestIterator.isNewCursor(xcursor, xcursorId)) {
                try {
                    cursorId = defaultOfferService.createCursor(buildContainerName(type, xTenantId));
                } catch (ContentAddressableStorageNotFoundException | ContentAddressableStorageServerException exc) {
                    LOGGER.error(exc);
                    status = Status.INTERNAL_SERVER_ERROR;
                    final Response.ResponseBuilder builder = Response.status(status)
                        .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                            .setContext("default-offer")
                            .setState("code_vitam").setMessage(status.getReasonPhrase())
                            .setDescription(exc.getMessage()));
                    return VitamRequestIterator.setHeaders(builder, xcursor, null).build();
                }
            }

            final RequestResponseOK<JsonNode> responseOK = new RequestResponseOK<JsonNode>();

            if (defaultOfferService.hasNext(buildContainerName(type, xTenantId), cursorId)) {
                try {
                    List<JsonNode> list = defaultOfferService.next(buildContainerName(type, xTenantId), cursorId);
                    responseOK.addAllResults(list);
                    LOGGER.debug("Result {}", responseOK);
                    final Response.ResponseBuilder builder = Response
                        .status(defaultOfferService.hasNext(buildContainerName(type, xTenantId), cursorId)
                            ? Status.PARTIAL_CONTENT
                            : Status.OK).entity(responseOK);
                    return VitamRequestIterator.setHeaders(builder, xcursor, cursorId).build();
                } catch (ContentAddressableStorageNotFoundException exc) {
                    LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), exc);
                    status = Status.INTERNAL_SERVER_ERROR;
                    final Response.ResponseBuilder builder = Response.status(status)
                        .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                            .setContext(DEFAULT_OFFER_MODULE)
                            .setState(CODE_VITAM).setMessage(status.getReasonPhrase())
                            .setDescription(exc.getMessage()));
                    return VitamRequestIterator.setHeaders(builder, xcursor, null).build();
                }
            } else {
                defaultOfferService.finalizeCursor(buildContainerName(type, xTenantId), xcursorId);
                final Response.ResponseBuilder builder = Response.status(Status.NO_CONTENT);
                return VitamRequestIterator.setHeaders(builder, xcursor, null).build();
            }
        } catch (Exception e) {
            LOGGER.error(e);
            final Response.ResponseBuilder builder = Response.status(Status.BAD_REQUEST);
            return VitamRequestIterator.setHeaders(builder, xcursor, null).build();
        }
    }

    /**
     * Get log of objects from container
     *
     * @param xTenantId the tenant id
     * @param type object type
     * @param offerLogRequest request params
     * @return list of objects infos
     */
    @GET
    @Path("/objects/{type}/logs")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOfferLogs(@HeaderParam(GlobalDataRest.X_TENANT_ID) String xTenantId,
        @PathParam("type") DataCategory type, OfferLogRequest offerLogRequest) {
        try {
            if (offerLogRequest == null) {
                LOGGER.error(MISSING_THE_BODY);
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            if (Strings.isNullOrEmpty(xTenantId)) {
                LOGGER.error(MISSING_THE_TENANT_ID_X_TENANT_ID);
                return Response.status(Response.Status.BAD_REQUEST).build();
            }

            try {
                final String containerName = buildContainerName(type, xTenantId);
                List<OfferLog> offerLogs =
                    defaultOfferService.getOfferLogs(containerName, offerLogRequest.getOffset(),
                        offerLogRequest.getLimit(), offerLogRequest.getOrder());
                final RequestResponseOK<OfferLog> responseOK = new RequestResponseOK<>();
                responseOK.addAllResults(offerLogs).setHttpCode(Status.OK.getStatusCode());
                LOGGER.debug("Result {}", responseOK);
                return Response.status(Status.OK).entity(JsonHandler.writeAsString(responseOK)).build();
            } catch (ContentAddressableStorageException exc) {
                LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), exc);
                return VitamCodeHelper.toVitamError(VitamCode.STORAGE_GET_OFFER_LOG_ERROR, exc.getMessage())
                    .toResponse();
            }
        } catch (Exception e) {
            LOGGER.error("An internal error occurred during offer log listing", e);
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
     * @return response
     * @throws IOException when there is an error of get object
     */
    @GET
    @Path("/objects/{type}/{id_object}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_OCTET_STREAM, CommonMediaType.ZIP})
    public Response getObject(@PathParam("type") DataCategory type, @NotNull @PathParam("id_object") String objectId,
        @Context HttpHeaders headers) {
        final String xTenantId = headers.getHeaderString(GlobalDataRest.X_TENANT_ID);
        try {
            SanityChecker.checkParameter(objectId);
            if (Strings.isNullOrEmpty(xTenantId)) {
                LOGGER.error(MISSING_THE_TENANT_ID_X_TENANT_ID);
                return Response.status(Status.PRECONDITION_FAILED).build();
            }
            final String containerName = buildContainerName(type, xTenantId);
            ObjectContent objectContent = defaultOfferService.getObject(containerName, objectId);

            Map<String, String> responseHeader = new HashMap<>();
            responseHeader.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM);
            responseHeader.put(VitamHttpHeader.X_CONTENT_LENGTH.getName(), String.valueOf(objectContent.getSize()));

            return new VitamAsyncInputStreamResponse(objectContent.getInputStream(),
                Status.OK, responseHeader);
        } catch (final ContentAddressableStorageNotFoundException | UnavailableFileException e) {
            LOGGER.warn(e);
            return buildErrorResponse(VitamCode.STORAGE_NOT_FOUND, e.getMessage());
        } catch (final ContentAddressableStorageException | InvalidParseOperationException e) {
            LOGGER.error(e);
            return buildErrorResponse(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR, e.getMessage());
        }
    }

    /**
     * Create read order (asynchronous read from tape to local FS) for the given @type and objects ids list.
     * <p>
     * HEADER X-Tenant-Id (mandatory) : tenant's identifier HEADER "X-type" (optional) : data (dfault) or digest
     * </p>
     *
     * @param type Object type
     * @param objectsIds objects ids :.+ in order to get all path if some '/' are provided
     * @param headers http header
     * @return response
     */
    @POST
    @Path("/readorder/{type}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createReadOrderRequest(@PathParam("type") DataCategory type, List<String> objectsIds,
        @Context HttpHeaders headers) {
        final String xTenantId = headers.getHeaderString(GlobalDataRest.X_TENANT_ID);
        try {
            if (type == null) {
                LOGGER.error(MISSING_THE_DATA_TYPE_PARAMETER);
                return Response.status(Status.PRECONDITION_FAILED).build();
            }

            if (objectsIds == null || objectsIds.isEmpty()) {
                LOGGER.error(MISSING_OBJECTS_IDS_LIST_PARAMETER);
                return Response.status(Status.PRECONDITION_FAILED).build();
            }

            if (Strings.isNullOrEmpty(xTenantId)) {
                LOGGER.error(MISSING_THE_TENANT_ID_X_TENANT_ID);
                return Response.status(Status.PRECONDITION_FAILED).build();
            }

            final String containerName = buildContainerName(type, xTenantId);
            Optional<TapeReadRequestReferentialEntity>
                createReadOrderRequest = defaultOfferService.createReadOrderRequest(containerName, objectsIds);

            if (createReadOrderRequest.isPresent()) {
                final RequestResponseOK<TapeReadRequestReferentialEntity> responseOK = new RequestResponseOK<>();
                responseOK.addResult(createReadOrderRequest.get())
                    .addHeader(GlobalDataRest.READ_REQUEST_ID, createReadOrderRequest.get().getRequestId())
                    .setHttpCode(Status.CREATED.getStatusCode());

                return responseOK.toResponse();
            } else {
                return buildErrorResponse(VitamCode.STORAGE_CREATE_READ_ORDER_ERROR, "Not read order request created");
            }

        } catch (final ContentAddressableStorageNotFoundException e) {
            LOGGER.warn(e);
            return buildErrorResponse(VitamCode.STORAGE_NOT_FOUND, e.getMessage());
        } catch (final ContentAddressableStorageException e) {
            LOGGER.error(e);
            return buildErrorResponse(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR, e.getMessage());
        }
    }

    /**
     * Get read order request
     * <p>
     * HEADER X-Tenant-Id (mandatory) : tenant's identifier HEADER "X-type" (optional) : data (dfault) or digest
     * </p>
     *
     * @param readOrderRequestId the read request ID
     * @return response
     */
    @GET
    @Path("/readorder/{readOrderRequestId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getReadOrderRequest(@PathParam("readOrderRequestId") String readOrderRequestId,
        @Context HttpHeaders headers) {
        final String xTenantId = headers.getHeaderString(GlobalDataRest.X_TENANT_ID);
        try {
            SanityChecker.checkParameter(readOrderRequestId);
            if (Strings.isNullOrEmpty(xTenantId)) {
                LOGGER.error(MISSING_THE_TENANT_ID_X_TENANT_ID);
                return Response.status(Status.PRECONDITION_FAILED).build();
            }

            Optional<TapeReadRequestReferentialEntity>
                createReadOrderRequest = defaultOfferService.getReadOrderRequest(readOrderRequestId);

            if (createReadOrderRequest.isPresent()) {
                final RequestResponseOK<TapeReadRequestReferentialEntity> responseOK = new RequestResponseOK<>();
                responseOK.addResult(createReadOrderRequest.get())
                    .addHeader(GlobalDataRest.READ_REQUEST_ID, createReadOrderRequest.get().getRequestId())
                    .setHttpCode(Status.OK.getStatusCode());

                return responseOK.toResponse();
            } else {
                return buildErrorResponse(VitamCode.STORAGE_NOT_FOUND,
                    "Read order request (" + readOrderRequestId + ") not found");
            }

        } catch (final ContentAddressableStorageNotFoundException e) {
            LOGGER.warn(e);
            return buildErrorResponse(VitamCode.STORAGE_NOT_FOUND, e.getMessage());
        } catch (final ContentAddressableStorageException | InvalidParseOperationException e) {
            LOGGER.error(e);
            return buildErrorResponse(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR, e.getMessage());
        }
    }


    @DELETE
    @Path("/readorder/{readOrderRequestId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeReadOrderRequest(@PathParam("readOrderRequestId") String readOrderRequestId,
        @Context HttpHeaders headers) {
        final String xTenantId = headers.getHeaderString(GlobalDataRest.X_TENANT_ID);
        try {
            SanityChecker.checkParameter(readOrderRequestId);
            if (Strings.isNullOrEmpty(xTenantId)) {
                LOGGER.error(MISSING_THE_TENANT_ID_X_TENANT_ID);
                return Response.status(Status.PRECONDITION_FAILED).build();
            }

            defaultOfferService.removeReadOrderRequest(readOrderRequestId);

            return Response.status(Status.ACCEPTED).header(GlobalDataRest.READ_REQUEST_ID, readOrderRequestId).build();

        } catch (final ContentAddressableStorageNotFoundException e) {
            LOGGER.warn(e);
            return buildErrorResponse(VitamCode.STORAGE_NOT_FOUND, e.getMessage());
        } catch (final ContentAddressableStorageException | InvalidParseOperationException e) {
            LOGGER.error(e);
            return buildErrorResponse(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR, e.getMessage());
        }
    }

    /**
     * Creates or updates an object.
     *
     * @param type Object's type
     * @param objectId the object id
     * @param headers http header
     * @return structured response with the object id
     */
    @PUT
    @Path("/objects/{type}/{objectId:.+}")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public Response putObject(@PathParam("objectId") String objectId, @PathParam("type") DataCategory type,
        @Context HttpHeaders headers, InputStream input) {

        final String xTenantId = headers.getHeaderString(GlobalDataRest.X_TENANT_ID);
        if (Strings.isNullOrEmpty(xTenantId)) {
            LOGGER.error(MISSING_THE_TENANT_ID_X_TENANT_ID);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        final String containerName = buildContainerName(type, xTenantId);

        final String size = headers.getHeaderString(GlobalDataRest.VITAM_CONTENT_LENGTH);
        Long inputStreamSize;
        try {
            inputStreamSize = Long.valueOf(size);
        } catch (NumberFormatException e) {
            LOGGER.error("Bad or missing size '" + size + "'");
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        String xDigestAlgorithm = headers.getHeaderString(GlobalDataRest.X_DIGEST_ALGORITHM);
        if (StringUtils.isEmpty(xDigestAlgorithm)) {
            LOGGER.error("Missing digest");
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        DigestType digestType = DigestType.fromValue(xDigestAlgorithm);

        try (final SizedInputStream sis = new SizedInputStream(input)) {
            LOGGER.info("Writing object '" + objectId + "' of container " + containerName + " (size: " + size + ")");

            SanityChecker.checkParameter(objectId);
            defaultOfferService.checkOfferPath(containerName, objectId);

            final String digest =
                defaultOfferService.createObject(containerName, objectId, sis,
                    type, inputStreamSize, digestType);
            return Response.status(Response.Status.CREATED)
                .entity("{\"digest\":\"" + digest + "\",\"size\":" + sis.getSize() + "}").build();
        } catch (ConnectionException e) {
            LOGGER.error(RE_AUTHENTICATION_CALL_STREAM_ALREADY_CONSUMED_BUT_NO_FILE_CREATED, e);
            return Response.status(Status.SERVICE_UNAVAILABLE).entity(JsonHandler.createObjectNode()
                .put("msg", RE_AUTHENTICATION_CALL_STREAM_ALREADY_CONSUMED_BUT_NO_FILE_CREATED)).build();

        } catch (NonUpdatableContentAddressableStorageException e) {
            LOGGER.error("Object overriding forbidden", e);
            return Response.status(Status.CONFLICT).build();
        } catch (Exception exc) {
            LOGGER.error("Cannot create object", exc);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            StreamUtils.closeSilently(input);
        }
    }

    /**
     * Bulk create or update objects.
     *
     * @param type Object's type
     * @param headers http header
     * @return structured response with the object id
     */
    @PUT
    @Path("/bulk/objects/{type}")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public Response bulkPutObjects(@PathParam("type") DataCategory type,
        @Context HttpHeaders headers, InputStream input) {

        final String xTenantId = headers.getHeaderString(GlobalDataRest.X_TENANT_ID);
        if (Strings.isNullOrEmpty(xTenantId)) {
            LOGGER.error(MISSING_THE_TENANT_ID_X_TENANT_ID);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        final String containerName = buildContainerName(type, xTenantId);

        final String size = headers.getHeaderString(GlobalDataRest.VITAM_CONTENT_LENGTH);
        Long inputStreamSize;
        try {
            inputStreamSize = Long.valueOf(size);
        } catch (NumberFormatException e) {
            LOGGER.error("Bad or missing size '" + size + "'");
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        try {
            MultiplexedStreamReader multiplexedStreamReader = new MultiplexedStreamReader(
                new ExactSizeInputStream(input, inputStreamSize));

            Optional<ExactSizeInputStream> headerEntry = multiplexedStreamReader.readNextEntry();
            if (!headerEntry.isPresent()) {
                throw new IllegalStateException("Header entry not found");
            }
            List<String> objectIds = JsonHandler.getFromInputStream(headerEntry.get(), List.class);

            String xDigestAlgorithm = headers.getHeaderString(GlobalDataRest.X_DIGEST_ALGORITHM);
            if (StringUtils.isEmpty(xDigestAlgorithm)) {
                LOGGER.error("Missing digest");
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            DigestType digestType = DigestType.fromValue(xDigestAlgorithm);

            StorageBulkPutResult storageBulkPutResult = defaultOfferService
                .bulkPutObjects(containerName, objectIds, multiplexedStreamReader, type, digestType);

            return Response.status(Status.CREATED).entity(storageBulkPutResult).build();

        } catch (NonUpdatableContentAddressableStorageException e) {
            LOGGER.error("Object overriding forbidden", e);
            return Response.status(Status.CONFLICT).build();
        } catch (Exception exc) {
            LOGGER.error("Cannot create object", exc);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            StreamUtils.closeSilently(input);
        }
    }

    /**
     * Delete an Object
     *
     * @param xTenantId the tenantId
     * @param xDigestAlgorithm the digest algorithm
     * @param type Object type to delete
     * @param idObject the id of the object to be tested
     * @return the response with a specific HTTP status
     */
    @DELETE
    @Path("/objects/{type}/{id:.+}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteObject(@HeaderParam(GlobalDataRest.X_TENANT_ID) String xTenantId,
        @HeaderParam(GlobalDataRest.X_DIGEST_ALGORITHM) String xDigestAlgorithm, @PathParam("type") DataCategory
        type,
        @PathParam("id") String idObject) {
        if (Strings.isNullOrEmpty(xTenantId)) {
            LOGGER.error(MISSING_THE_TENANT_ID_X_TENANT_ID);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        try {
            SanityChecker.checkParameter(idObject);
            VitamThreadUtils.getVitamSession()
                .setRequestId(GUIDFactory.newRequestIdGUID(Integer.parseInt(xTenantId)));
            final String containerName = buildContainerName(type, xTenantId);
            defaultOfferService.deleteObject(containerName, idObject, type);
            return Response.status(Response.Status.OK)
                .entity("{\"id\":\"" + idObject + "\",\"status\":\"" + Response.Status.OK.toString() + "\"}")
                .build();
        } catch (ContentAddressableStorageNotFoundException e) {
            LOGGER.info(e);
            return Response.status(Response.Status.NOT_FOUND).build();
        } catch (ContentAddressableStorageException | InvalidParseOperationException e) {
            LOGGER.error(e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }

    }

    /**
     * Test the existence of an object
     * <p>
     * HEADER X-Tenant-Id (mandatory) : tenant's identifier
     *
     * @param type Object type to test
     * @param idObject the id of the object to be tested
     * @param xTenantId the id of the tenant
     * @return the response with a specific HTTP status. If none of DIGEST or
     * DIGEST_ALGORITHM headers is given, an existence test is done and
     * can return 204/404 as response. If only DIGEST or only
     * DIGEST_ALGORITHM header is given, a not implemented exception is
     * thrown. Later, this should respond with 200/409. If both DIGEST
     * and DIGEST_ALGORITHM header are given, a full digest check is
     * done and can return 200/409 as response
     */
    @HEAD
    @Path("/objects/{type}/{id:.+}")
    public Response checkObjectExistence(@PathParam("type") DataCategory type, @PathParam("id") String idObject,
        @HeaderParam(GlobalDataRest.X_TENANT_ID) String xTenantId) {
        if (Strings.isNullOrEmpty(xTenantId)) {
            LOGGER.error(MISSING_THE_TENANT_ID_X_TENANT_ID);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        String containerName = buildContainerName(type, xTenantId);

        try {
            SanityChecker.checkParameter(idObject);
            if (defaultOfferService.isObjectExist(containerName, idObject)) {
                return Response.status(Response.Status.NO_CONTENT).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        } catch (final ContentAddressableStorageException | InvalidParseOperationException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get metadata of the object type.
     *
     * @param type Object type to test
     * @param idObject the id of the object to be tested
     * @param xTenantId the id of the tenant
     * @return metadatas
     */
    @GET
    @Path("/objects/{type}/{id:.+}/metadatas")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getObjectMetadata(@PathParam("type") DataCategory type, @PathParam("id") String idObject,
        @HeaderParam(GlobalDataRest.X_TENANT_ID) String xTenantId,
        @HeaderParam(GlobalDataRest.X_OFFER_NO_CACHE) Boolean noCache) {

        if (Strings.isNullOrEmpty(xTenantId) || noCache == null) {
            LOGGER.error("Missing tenant ID (X-Tenant-Id) or noCache");
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        final String containerName = buildContainerName(type, xTenantId);
        try {
            SanityChecker.checkParameter(idObject);
            StorageMetadataResult result = defaultOfferService.getMetadata(containerName, idObject, noCache);
            return Response.status(Response.Status.OK).entity(result).build();
        } catch (ContentAddressableStorageNotFoundException e) {
            LOGGER.warn(e);
            return Response.status(Response.Status.NOT_FOUND).build();
        } catch (ContentAddressableStorageException | IOException | InvalidParseOperationException e) {
            LOGGER.error(e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Add error response using with vitamCode
     *
     * @param vitamCode vitam error Code
     */

    private Response buildErrorResponse(VitamCode vitamCode, String message) {
        return Response.status(vitamCode.getStatus()).entity(new RequestResponseError().setError(
            new VitamError(VitamCodeHelper.getCode(vitamCode))
                .setContext(vitamCode.getService().getName())
                .setHttpCode(vitamCode.getStatus().getStatusCode())
                .setState(vitamCode.getDomain().getName())
                .setMessage(vitamCode.getMessage())
                .setDescription(Strings.isNullOrEmpty(message) ? vitamCode.getMessage() : message))
            .toString()).build();
    }
}
