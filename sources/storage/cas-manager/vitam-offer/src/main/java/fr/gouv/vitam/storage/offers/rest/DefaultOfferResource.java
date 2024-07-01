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
package fr.gouv.vitam.storage.offers.rest;

import com.google.common.base.Strings;
import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.client.CustomVitamHttpStatusCode;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.error.DomainName;
import fr.gouv.vitam.common.error.ServiceName;
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
import fr.gouv.vitam.common.model.storage.AccessRequestStatus;
import fr.gouv.vitam.common.model.storage.ObjectEntryWriter;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.server.application.VitamHttpHeader;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.common.storage.ContainerInformation;
import fr.gouv.vitam.common.storage.cas.container.api.ObjectContent;
import fr.gouv.vitam.common.storage.constants.ErrorMessage;
import fr.gouv.vitam.common.stream.ExactSizeInputStream;
import fr.gouv.vitam.common.stream.MultiplexedStreamReader;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.storage.driver.model.StorageBulkMetadataResult;
import fr.gouv.vitam.storage.driver.model.StorageBulkPutResult;
import fr.gouv.vitam.storage.driver.model.StorageCheckObjectAvailabilityResult;
import fr.gouv.vitam.storage.driver.model.StorageMetadataResult;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.request.OfferLogRequest;
import fr.gouv.vitam.storage.offers.core.DefaultOfferService;
import fr.gouv.vitam.storage.offers.core.NonUpdatableContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageUnavailableDataFromAsyncOfferException;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.TaggedInputStream;
import org.apache.commons.io.output.CloseShieldOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.openstack4j.api.exceptions.ConnectionException;

import javax.validation.constraints.NotNull;
import javax.ws.rs.ApplicationPath;
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
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static fr.gouv.vitam.storage.engine.common.utils.ContainerUtils.buildContainerName;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/offer/v1")
@ApplicationPath("webresources")
@Tag(name = "Default-Offer")
public class DefaultOfferResource extends ApplicationStatusResource {

    private static final String MISSING_THE_TENANT_ID_X_TENANT_ID =
        "Missing the tenant ID (X-Tenant-Id) or wrong object Type";
    private static final String MISSING_THE_BODY = "Missing the body object";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(DefaultOfferResource.class);
    public static final String RE_AUTHENTICATION_CALL_STREAM_ALREADY_CONSUMED_BUT_NO_FILE_CREATED =
        "Caused by re-authentication call. Stream already consumed but no file created, storage engine must retry to re-put object";
    private static final String MISSING_THE_DATA_TYPE_PARAMETER = "Missing Data Type parameter";
    private static final String MISSING_OBJECTS_IDS_LIST_PARAMETER = "Missing Objects Ids List parameter";

    private final DefaultOfferService defaultOfferService;

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
    public Response getCapacity(
        @HeaderParam(GlobalDataRest.X_TENANT_ID) String xTenantId,
        @PathParam("type") DataCategory type
    ) {
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
        } catch (Exception exc) {
            LOGGER.error(exc);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get container object list.
     *
     * @param xTenantId the tenant id
     * @param type object type
     * @return an iterator with each object metadata (actually only the id)
     */
    @GET
    @Path("/objects/{type}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getContainerList(
        @HeaderParam(GlobalDataRest.X_TENANT_ID) String xTenantId,
        @PathParam("type") DataCategory type
    ) {
        if (Strings.isNullOrEmpty(xTenantId)) {
            LOGGER.error(MISSING_THE_TENANT_ID_X_TENANT_ID);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        StreamingOutput streamingOutput = output -> {
            try (
                CloseShieldOutputStream closeShieldOutputStream = new CloseShieldOutputStream(output);
                ObjectEntryWriter objectEntryWriter = new ObjectEntryWriter(closeShieldOutputStream)
            ) {
                defaultOfferService.listObjects(buildContainerName(type, xTenantId), objectEntryWriter::write);

                // No errors ==> write EOF
                objectEntryWriter.writeEof();
            } catch (Exception e) {
                LOGGER.error("Could not return object listing. Internal server error", e);
                throw new WebApplicationException("Could not return object listing", e);
            }
        };

        return Response.ok(streamingOutput).build();
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
    public Response getOfferLogs(
        @HeaderParam(GlobalDataRest.X_TENANT_ID) String xTenantId,
        @PathParam("type") DataCategory type,
        OfferLogRequest offerLogRequest
    ) {
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
                List<OfferLog> offerLogs = defaultOfferService.getOfferLogs(
                    containerName,
                    offerLogRequest.getOffset(),
                    offerLogRequest.getLimit(),
                    offerLogRequest.getOrder()
                );
                final RequestResponseOK<OfferLog> responseOK = new RequestResponseOK<>();
                responseOK.addAllResults(offerLogs).setHttpCode(Status.OK.getStatusCode());
                LOGGER.debug("Result {}", responseOK);
                return Response.status(Status.OK).entity(JsonHandler.writeAsString(responseOK)).build();
            } catch (ContentAddressableStorageException exc) {
                LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), exc);
                return VitamCodeHelper.toVitamError(
                    VitamCode.STORAGE_GET_OFFER_LOG_ERROR,
                    exc.getMessage()
                ).toResponse();
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
    @Produces({ MediaType.APPLICATION_OCTET_STREAM, CommonMediaType.ZIP })
    public Response getObject(
        @PathParam("type") DataCategory type,
        @NotNull @PathParam("id_object") String objectId,
        @Context HttpHeaders headers
    ) {
        final String xTenantId = headers.getHeaderString(GlobalDataRest.X_TENANT_ID);
        try {
            SanityChecker.checkParameter(objectId);
            if (Strings.isNullOrEmpty(xTenantId)) {
                LOGGER.error(MISSING_THE_TENANT_ID_X_TENANT_ID);
                return Response.status(Status.PRECONDITION_FAILED).build();
            }
            final String containerName = buildContainerName(type, xTenantId);
            ObjectContent objectContent = defaultOfferService.getObject(containerName, objectId);

            StreamingOutput streamingOutput = output -> {
                TaggedInputStream taggedInputStream = null;
                try {
                    taggedInputStream = new TaggedInputStream(
                        new ExactSizeInputStream(objectContent.getInputStream(), objectContent.getSize())
                    );

                    IOUtils.copy(taggedInputStream, output);
                } catch (IOException e) {
                    // 2 types on IO Exceptions :
                    // - Client-side exceptions (caused by networking errors, client closing connection...). Just let jetty handle it
                    // - Server-side exceptions (caused by inner CAS provider...). These exceptions need at least to be logged.

                    // TaggedInputStream is used to detect error cause

                    if (taggedInputStream == null || taggedInputStream.isCauseOf(e)) {
                        LOGGER.error("Server-side IOException. Could not serve object stream from CAS container", e);
                        throw new WebApplicationException(
                            "Server-side IOException. Could not serve object stream from CAS container",
                            e
                        );
                    }

                    // Client-side IOException. Let webapp container handle it
                    throw e;
                } finally {
                    objectContent.getInputStream().close();
                }
            };

            return Response.ok(streamingOutput)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM)
                .header(VitamHttpHeader.X_CONTENT_LENGTH.getName(), String.valueOf(objectContent.getSize()))
                .build();
        } catch (final ContentAddressableStorageNotFoundException e) {
            LOGGER.warn(e);
            return buildErrorResponse(VitamCode.STORAGE_NOT_FOUND, e.getMessage());
        } catch (final ContentAddressableStorageUnavailableDataFromAsyncOfferException e) {
            LOGGER.warn(e);
            return buildCustomErrorResponse(
                CustomVitamHttpStatusCode.UNAVAILABLE_DATA_FROM_ASYNC_OFFER,
                e.getMessage()
            );
        } catch (final ContentAddressableStorageException | InvalidParseOperationException e) {
            LOGGER.error(e);
            return buildErrorResponse(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR, e.getMessage());
        }
    }

    /**
     * Create access request (asynchronous read from tape to local FS) for the given @type and objects ids list.
     * <p>
     * HEADER X-Tenant-Id (mandatory) : tenant's identifier
     * </p>
     *
     * @param type Object type
     * @param objectNames : object names for which access is requested
     * @param headers http header
     * @return response
     */
    @POST
    @Path("/access-request/{type}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createAccessRequest(
        @PathParam("type") DataCategory type,
        List<String> objectNames,
        @Context HttpHeaders headers
    ) {
        final String xTenantId = headers.getHeaderString(GlobalDataRest.X_TENANT_ID);
        try {
            if (type == null) {
                LOGGER.error(MISSING_THE_DATA_TYPE_PARAMETER);
                return Response.status(Status.PRECONDITION_FAILED).build();
            }

            if (objectNames == null || objectNames.isEmpty()) {
                LOGGER.error(MISSING_OBJECTS_IDS_LIST_PARAMETER);
                return Response.status(Status.PRECONDITION_FAILED).build();
            }

            if (Strings.isNullOrEmpty(xTenantId)) {
                LOGGER.error(MISSING_THE_TENANT_ID_X_TENANT_ID);
                return Response.status(Status.PRECONDITION_FAILED).build();
            }

            final String containerName = buildContainerName(type, xTenantId);
            String accessRequestId = defaultOfferService.createAccessRequest(containerName, objectNames);

            return new RequestResponseOK<>()
                .addResult(accessRequestId)
                .setHttpCode(Status.CREATED.getStatusCode())
                .toResponse();
        } catch (final ContentAddressableStorageNotFoundException e) {
            LOGGER.warn(e);
            return buildErrorResponse(VitamCode.STORAGE_NOT_FOUND, e.getMessage());
        } catch (final ContentAddressableStorageException e) {
            LOGGER.error(e);
            return buildErrorResponse(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR, e.getMessage());
        }
    }

    /**
     * Check access request statuses by identifiers
     * <p>
     * HEADER X-Tenant-Id (mandatory) : tenant's identifier
     * </p>
     *
     * @param accessRequestIds the list of access request ids
     * @return response
     */
    @GET
    @Path("/access-request/statuses")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkAccessRequestStatuses(List<String> accessRequestIds, @Context HttpHeaders headers) {
        try {
            ParametersChecker.checkParameter("Missing accessRequestId", accessRequestIds);
            ParametersChecker.checkParameter("Missing accessRequestId", accessRequestIds.toArray(String[]::new));
            for (String accessRequestId : accessRequestIds) {
                SanityChecker.checkParameter(accessRequestId);
            }
            final String xTenantId = headers.getHeaderString(GlobalDataRest.X_TENANT_ID);
            if (Strings.isNullOrEmpty(xTenantId)) {
                LOGGER.error(MISSING_THE_TENANT_ID_X_TENANT_ID);
                return Response.status(Status.PRECONDITION_FAILED).build();
            }

            final String adminCrossTenantAccessRequestAllowedStr = headers.getHeaderString(
                GlobalDataRest.X_ADMIN_CROSS_TENANT_ACCESS_REQUEST_ALLOWED
            );
            if (Strings.isNullOrEmpty(adminCrossTenantAccessRequestAllowedStr)) {
                LOGGER.error("Required " + GlobalDataRest.X_ADMIN_CROSS_TENANT_ACCESS_REQUEST_ALLOWED + " header");
                return Response.status(Status.PRECONDITION_FAILED).build();
            }
            boolean adminCrossTenantAccessRequestAllowed = Boolean.parseBoolean(
                adminCrossTenantAccessRequestAllowedStr
            );

            Map<String, AccessRequestStatus> accessRequestStatus = defaultOfferService.checkAccessRequestStatuses(
                accessRequestIds,
                adminCrossTenantAccessRequestAllowed
            );

            return new RequestResponseOK<>()
                .addResult(accessRequestStatus)
                .setHttpCode(Status.OK.getStatusCode())
                .toResponse();
        } catch (IllegalArgumentException e) {
            LOGGER.error(e);
            return buildErrorResponse(VitamCode.STORAGE_BAD_REQUEST, e.getMessage());
        } catch (final ContentAddressableStorageException | InvalidParseOperationException e) {
            LOGGER.error(e);
            return buildErrorResponse(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR, e.getMessage());
        }
    }

    @DELETE
    @Path("/access-request/{accessRequestId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeAccessRequest(
        @PathParam("accessRequestId") String accessRequestId,
        @Context HttpHeaders headers
    ) {
        final String xTenantId = headers.getHeaderString(GlobalDataRest.X_TENANT_ID);
        try {
            SanityChecker.checkParameter(accessRequestId);
            if (Strings.isNullOrEmpty(xTenantId)) {
                LOGGER.error(MISSING_THE_TENANT_ID_X_TENANT_ID);
                return Response.status(Status.PRECONDITION_FAILED).build();
            }

            final String adminCrossTenantAccessRequestAllowedStr = headers.getHeaderString(
                GlobalDataRest.X_ADMIN_CROSS_TENANT_ACCESS_REQUEST_ALLOWED
            );
            if (Strings.isNullOrEmpty(adminCrossTenantAccessRequestAllowedStr)) {
                LOGGER.error("Required " + GlobalDataRest.X_ADMIN_CROSS_TENANT_ACCESS_REQUEST_ALLOWED + " header");
                return Response.status(Status.PRECONDITION_FAILED).build();
            }
            boolean adminCrossTenantAccessRequestAllowed = Boolean.parseBoolean(
                adminCrossTenantAccessRequestAllowedStr
            );

            defaultOfferService.removeAccessRequest(accessRequestId, adminCrossTenantAccessRequestAllowed);

            return Response.status(Status.OK).build();
        } catch (IllegalArgumentException e) {
            LOGGER.error(e);
            return buildErrorResponse(VitamCode.STORAGE_BAD_REQUEST, e.getMessage());
        } catch (final ContentAddressableStorageException | InvalidParseOperationException e) {
            LOGGER.error(e);
            return buildErrorResponse(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR, e.getMessage());
        }
    }

    /**
     * Check object availability for immediate access on async storage offers (tape storage only).
     * For tape storage, an object is immediately accessible only when it's currently stored fully on disk.
     * This API is not supported for synchronous storage offers.
     *
     * HEADER X-Tenant-Id (mandatory) : tenant's identifier
     *
     * @param type Object type
     * @param objectNames object names for which immediate availability is to be checked
     * @param headers http header
     * @return response
     */
    @GET
    @Path("/object-availability-check/{type}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkObjectAvailability(
        @PathParam("type") DataCategory type,
        List<String> objectNames,
        @Context HttpHeaders headers
    ) {
        final String xTenantId = headers.getHeaderString(GlobalDataRest.X_TENANT_ID);
        try {
            if (Strings.isNullOrEmpty(xTenantId)) {
                LOGGER.error(MISSING_THE_TENANT_ID_X_TENANT_ID);
                return Response.status(Status.PRECONDITION_FAILED).build();
            }

            if (objectNames == null || objectNames.isEmpty()) {
                LOGGER.error(MISSING_OBJECTS_IDS_LIST_PARAMETER);
                return Response.status(Status.PRECONDITION_FAILED).build();
            }

            final String containerName = buildContainerName(type, xTenantId);
            boolean areObjectsAvailable = defaultOfferService.checkObjectAvailability(containerName, objectNames);

            return new RequestResponseOK<>()
                .addResult(new StorageCheckObjectAvailabilityResult(areObjectsAvailable))
                .setHttpCode(Status.OK.getStatusCode())
                .toResponse();
        } catch (final ContentAddressableStorageException e) {
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
    public Response putObject(
        @PathParam("objectId") String objectId,
        @PathParam("type") DataCategory type,
        @Context HttpHeaders headers,
        InputStream input
    ) {
        final String xTenantId = headers.getHeaderString(GlobalDataRest.X_TENANT_ID);
        if (Strings.isNullOrEmpty(xTenantId)) {
            LOGGER.error(MISSING_THE_TENANT_ID_X_TENANT_ID);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        final String containerName = buildContainerName(type, xTenantId);

        final String size = headers.getHeaderString(GlobalDataRest.VITAM_CONTENT_LENGTH);
        long inputStreamSize;
        try {
            inputStreamSize = Long.parseLong(size);
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

        try (final InputStream sis = new ExactSizeInputStream(input, inputStreamSize)) {
            LOGGER.info("Writing object '" + objectId + "' of container " + containerName + " (size: " + size + ")");

            SanityChecker.checkParameter(objectId);

            final String digest = defaultOfferService.createObject(
                containerName,
                objectId,
                sis,
                type,
                inputStreamSize,
                digestType
            );
            return Response.status(Response.Status.CREATED)
                .entity("{\"digest\":\"" + digest + "\",\"size\":" + inputStreamSize + "}")
                .build();
        } catch (ConnectionException e) {
            LOGGER.error(RE_AUTHENTICATION_CALL_STREAM_ALREADY_CONSUMED_BUT_NO_FILE_CREATED, e);
            return Response.status(Status.SERVICE_UNAVAILABLE)
                .entity(
                    JsonHandler.createObjectNode()
                        .put("msg", RE_AUTHENTICATION_CALL_STREAM_ALREADY_CONSUMED_BUT_NO_FILE_CREATED)
                )
                .build();
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
    public Response bulkPutObjects(
        @PathParam("type") DataCategory type,
        @Context HttpHeaders headers,
        InputStream input
    ) {
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
                new ExactSizeInputStream(input, inputStreamSize)
            );

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

            StorageBulkPutResult storageBulkPutResult = defaultOfferService.bulkPutObjects(
                containerName,
                objectIds,
                multiplexedStreamReader,
                type,
                digestType
            );

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
    public Response deleteObject(
        @HeaderParam(GlobalDataRest.X_TENANT_ID) String xTenantId,
        @HeaderParam(GlobalDataRest.X_DIGEST_ALGORITHM) String xDigestAlgorithm,
        @PathParam("type") DataCategory type,
        @PathParam("id") String idObject
    ) {
        if (Strings.isNullOrEmpty(xTenantId)) {
            LOGGER.error(MISSING_THE_TENANT_ID_X_TENANT_ID);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        try {
            SanityChecker.checkParameter(idObject);
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(Integer.parseInt(xTenantId)));
            final String containerName = buildContainerName(type, xTenantId);
            defaultOfferService.deleteObject(containerName, idObject, type);
            return Response.status(Response.Status.OK)
                .entity("{\"id\":\"" + idObject + "\",\"status\":\"" + Status.OK + "\"}")
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
    public Response checkObjectExistence(
        @PathParam("type") DataCategory type,
        @PathParam("id") String idObject,
        @HeaderParam(GlobalDataRest.X_TENANT_ID) String xTenantId
    ) {
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
    public Response getObjectMetadata(
        @PathParam("type") DataCategory type,
        @PathParam("id") String idObject,
        @HeaderParam(GlobalDataRest.X_TENANT_ID) String xTenantId,
        @HeaderParam(GlobalDataRest.X_OFFER_NO_CACHE) Boolean noCache
    ) {
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
        } catch (ContentAddressableStorageException | InvalidParseOperationException e) {
            LOGGER.error(e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get bulk metadata of the objects by ids.
     *
     * @param type Object type to test
     * @param xTenantId the id of the tenant
     * @return metadata by object id
     */
    @GET
    @Path("/bulk/objects/{type}/metadata")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getBulkObjectMetadata(
        @PathParam("type") DataCategory type,
        @HeaderParam(GlobalDataRest.X_TENANT_ID) String xTenantId,
        @HeaderParam(GlobalDataRest.X_OFFER_NO_CACHE) Boolean noCache,
        List<String> objectIds
    ) {
        if (Strings.isNullOrEmpty(xTenantId) || noCache == null) {
            LOGGER.error("Missing tenant ID (X-Tenant-Id) or noCache");
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        final String containerName = buildContainerName(type, xTenantId);
        try {
            ParametersChecker.checkParameter("ObjectIds cannot be null", objectIds);
            ParametersChecker.checkParameter("ObjectIds cannot be null", objectIds.toArray());
            for (String objectID : objectIds) {
                SanityChecker.checkParameter(objectID);
            }
            StorageBulkMetadataResult result = defaultOfferService.getBulkMetadata(containerName, objectIds, noCache);
            return Response.status(Response.Status.OK).entity(result).build();
        } catch (InvalidParseOperationException | IllegalArgumentException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).build();
        } catch (Exception e) {
            LOGGER.error(e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @POST
    @Path("/compaction")
    @Consumes(APPLICATION_JSON)
    public void launchOfferLogCompaction() throws Exception {
        try {
            LOGGER.info("Starting offer compaction.");
            defaultOfferService.compactOfferLogs();
            LOGGER.info("End offer compaction.");
        } catch (Exception e) {
            LOGGER.error("An internal error occurred during offer log compaction", e);
        }
    }

    /**
     * Add error response using with vitamCode
     *
     * @param vitamCode vitam error Code
     */

    private Response buildErrorResponse(VitamCode vitamCode, String message) {
        return Response.status(vitamCode.getStatus())
            .entity(
                new RequestResponseError()
                    .setError(
                        new VitamError(VitamCodeHelper.getCode(vitamCode))
                            .setContext(vitamCode.getService().getName())
                            .setHttpCode(vitamCode.getStatus().getStatusCode())
                            .setState(vitamCode.getDomain().getName())
                            .setMessage(vitamCode.getMessage())
                            .setDescription(Strings.isNullOrEmpty(message) ? vitamCode.getMessage() : message)
                    )
                    .toString()
            )
            .build();
    }

    private Response buildCustomErrorResponse(CustomVitamHttpStatusCode customStatusCode, String message) {
        return Response.status(customStatusCode.getStatusCode())
            .entity(
                new RequestResponseError()
                    .setError(
                        new VitamError(customStatusCode.toString())
                            .setContext(ServiceName.STORAGE.getName())
                            .setHttpCode(customStatusCode.getStatusCode())
                            .setState(DomainName.STORAGE.getName())
                            .setMessage(customStatusCode.getMessage())
                            .setDescription(Strings.isNullOrEmpty(message) ? customStatusCode.getMessage() : message)
                    )
                    .toString()
            )
            .build();
    }
}
