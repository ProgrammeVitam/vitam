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

package fr.gouv.vitam.storage.engine.server.rest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseError;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.VitamAutoCloseable;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.server.application.HttpHeaderHelper;
import fr.gouv.vitam.common.server.application.VitamHttpHeader;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.common.stream.VitamAsyncInputStreamResponse;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.common.timestamp.TimeStampSignature;
import fr.gouv.vitam.common.timestamp.TimeStampSignatureWithKeystore;
import fr.gouv.vitam.common.timestamp.TimestampGenerator;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.exception.TraceabilityException;
import fr.gouv.vitam.storage.engine.common.exception.StorageAlreadyExistsException;
import fr.gouv.vitam.storage.engine.common.exception.StorageException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.storage.engine.common.model.request.OfferLogRequest;
import fr.gouv.vitam.storage.engine.common.model.response.StoredInfoResult;
import fr.gouv.vitam.storage.engine.server.distribution.StorageDistribution;
import fr.gouv.vitam.storage.engine.server.distribution.impl.StorageDistributionImpl;
import fr.gouv.vitam.storage.engine.server.storagelog.StorageLogAdministration;
import fr.gouv.vitam.storage.engine.server.storagelog.StorageLogException;
import fr.gouv.vitam.storage.engine.server.storagelog.StorageLogProvider;
import fr.gouv.vitam.storage.engine.server.storagelog.StorageLogFactory;
import fr.gouv.vitam.storage.engine.server.storagetraceability.StorageTraceabilityAdministration;
import fr.gouv.vitam.storage.engine.server.storagetraceability.TraceabilityStorageService;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

/**
 * Storage Resource implementation
 */
@Path("/storage/v1")
public class StorageResource extends ApplicationStatusResource implements VitamAutoCloseable {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(StorageResource.class);
    private static final String STORAGE_MODULE = "STORAGE";
    private static final String CODE_VITAM = "code_vitam";
    private static final String MISSING_THE_TENANT_ID_X_TENANT_ID =
        "Missing the tenant ID (X-Tenant-Id) or wrong object Type";
    private static final String STRATEGY_ID_IS_REQUIRED = "Strategy ID is required";

    private final StorageDistribution distribution;
    private final TraceabilityStorageService traceabilityLogbookService;
    private final TimestampGenerator timestampGenerator;

    private StorageLogProvider storageLogService;
    private StorageLogAdministration storageLogAdministration;
    private StorageTraceabilityAdministration traceabilityLogbookAdministration;
    /**
     * Constructor
     *
     * @param configuration
     */
    public StorageResource(StorageConfiguration configuration) {
        try {
            storageLogService = StorageLogFactory.getInstance(VitamConfiguration.getTenants(),
                Paths.get(configuration.getLoggingDirectory()));
            distribution = new StorageDistributionImpl(configuration, storageLogService);
            WorkspaceClientFactory.changeMode(configuration.getUrlWorkspace());
            storageLogAdministration =
                new StorageLogAdministration(storageLogService);

            traceabilityLogbookService = new TraceabilityStorageService(distribution);

            TimeStampSignature timeStampSignature;
            try {
                final File file = PropertiesUtils.findFile(configuration.getP12LogbookFile());
                timeStampSignature =
                    new TimeStampSignatureWithKeystore(file, configuration.getP12LogbookPassword().toCharArray());
            } catch (KeyStoreException | CertificateException | IOException | UnrecoverableKeyException |
                NoSuchAlgorithmException e) {
                LOGGER.error("unable to instantiate TimeStampGenerator", e);
                throw new RuntimeException(e);
            }

            timestampGenerator = new TimestampGenerator(timeStampSignature);
            // TODO Must have conf for logOpeClient ?
            traceabilityLogbookAdministration =
                new StorageTraceabilityAdministration(traceabilityLogbookService,
                    configuration.getZippingDirecorty(), timestampGenerator,
                    configuration.getStorageTraceabilityOverlapDelay());
            LOGGER.info("init Storage Resource server");

        } catch (IOException e) {
            LOGGER.error("Cannot initialize storage resource server, error when reading configuration file");
            // FIXME: erf, not cool here
            throw new RuntimeException(e);
        }
    }

    /**
     * Constructor
     *
     * @param configuration the storage configuration to be applied
     * @param service       the logbook service
     */
    public StorageResource(StorageConfiguration configuration, StorageLogProvider service) {
        this.storageLogService = service;
        distribution = new StorageDistributionImpl(configuration, storageLogService);
        WorkspaceClientFactory.changeMode(configuration.getUrlWorkspace());
        storageLogAdministration =
            new StorageLogAdministration(storageLogService);
        traceabilityLogbookService = new TraceabilityStorageService(distribution);

        TimeStampSignature timeStampSignature;
        try {
            final File file = PropertiesUtils.findFile(configuration.getP12LogbookFile());
            timeStampSignature =
                new TimeStampSignatureWithKeystore(file, configuration.getP12LogbookPassword().toCharArray());
        } catch (KeyStoreException | CertificateException | IOException | UnrecoverableKeyException |
            NoSuchAlgorithmException e) {
            LOGGER.error("unable to instantiate TimeStampGenerator", e);
            throw new RuntimeException(e);
        }

        timestampGenerator = new TimestampGenerator(timeStampSignature);

        traceabilityLogbookAdministration =
            new StorageTraceabilityAdministration(traceabilityLogbookService,
                configuration.getZippingDirecorty(), timestampGenerator,
                configuration.getStorageTraceabilityOverlapDelay());
        LOGGER.info("init Storage Resource server");
    }

    /**
     * Constructor used for test purpose
     *
     * @param storageDistribution the storage Distribution to be applied
     */
    StorageResource(StorageDistribution storageDistribution, TimestampGenerator timestampGenerator) {
        distribution = storageDistribution;
        traceabilityLogbookService = new TraceabilityStorageService(distribution);
        this.timestampGenerator = timestampGenerator;
    }

    /**
     * @param headers http headers
     * @return null if strategy and tenant headers have values, an error response otherwise
     */
    private Response checkTenantStrategyHeader(HttpHeaders headers) {
        if (!HttpHeaderHelper.hasValuesFor(headers, VitamHttpHeader.TENANT_ID) ||
            !HttpHeaderHelper.hasValuesFor(headers, VitamHttpHeader.STRATEGY_ID)) {
            return buildErrorResponse(VitamCode.STORAGE_MISSING_HEADER);
        }
        return null;
    }

    /**
     * @param strategyId StrategyId if directly getting from Header parameter
     * @return null if strategy and tenant headers have values, an error response otherwise
     */
    private Response checkTenantStrategyHeader(String strategyId) {
        if (VitamThreadUtils.getVitamSession().getTenantId() == null) {
            return buildErrorResponse(VitamCode.STORAGE_MISSING_HEADER);
        }
        try {
            SanityChecker.checkParameter(strategyId);
        } catch (InvalidParseOperationException e) {
            LOGGER.error("Missing Strategy", e);
            return buildErrorResponse(VitamCode.STORAGE_MISSING_HEADER);
        }
        return null;
    }

    /**
     * @param headers http headers
     * @return null if strategy, tenant, digest and digest algorithm headers have values, an error response otherwise
     */
    private Response checkDigestAlgorithmHeader(HttpHeaders headers) {
        if (!HttpHeaderHelper.hasValuesFor(headers, VitamHttpHeader.TENANT_ID) ||
            !HttpHeaderHelper.hasValuesFor(headers, VitamHttpHeader.STRATEGY_ID) ||
            !HttpHeaderHelper.hasValuesFor(headers, VitamHttpHeader.X_DIGEST) ||
            !HttpHeaderHelper.hasValuesFor(headers, VitamHttpHeader.X_DIGEST_ALGORITHM)) {
            return buildErrorResponse(VitamCode.STORAGE_MISSING_HEADER);
        }
        return null;
    }

    /**
     * Get storage information for a specific tenant/strategy For example the usable space
     *
     * @param headers http headers
     * @return Response containing the storage information as json, or an error (404, 500)
     */
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStorageInformation(@Context HttpHeaders headers) {
        final Response response = checkTenantStrategyHeader(headers);
        if (response == null) {
            VitamCode vitamCode;
            final String strategyId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.STRATEGY_ID).get(0);
            try {
                final JsonNode result = distribution.getContainerInformations(strategyId);
                return Response.status(Status.OK).entity(result).build();
            } catch (final StorageNotFoundException exc) {
                LOGGER.error(exc);
                vitamCode = VitamCode.STORAGE_NOT_FOUND;
            } catch (final StorageException exc) {
                LOGGER.error(exc);
                vitamCode = VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR;
            } catch (final IllegalArgumentException exc) {
                LOGGER.error(exc);
                vitamCode = VitamCode.STORAGE_BAD_REQUEST;
            }
            return buildErrorResponse(vitamCode);
        }
        return response;
    }

    /**
     * Search the header value for 'X-Http-Method-Override' and return an error response id it's value is not 'GET'
     *
     * @param headers the http headers to check
     * @return OK response if no header is found, NULL if header value is correct, BAD_REQUEST if the header contain an
     * other value than GET
     */
    public Response checkPostHeader(HttpHeaders headers) {
        if (HttpHeaderHelper.hasValuesFor(headers, VitamHttpHeader.METHOD_OVERRIDE)) {
            final MultivaluedHashMap<String, String> wanted = new MultivaluedHashMap<>();
            wanted.add(VitamHttpHeader.METHOD_OVERRIDE.getName(), HttpMethod.GET);
            try {
                HttpHeaderHelper.validateHeaderValue(headers, wanted);
                return null;
            } catch (IllegalArgumentException | IllegalStateException exc) {
                LOGGER.error(exc);
                return badRequestResponse(exc.getMessage());
            }
        } else {
            return Response.status(Status.OK).build();
        }
    }

    /**
     * Create a container
     * <p>
     *
     * @param headers http header
     * @return Response NOT_IMPLEMENTED
     */
    // TODO P1 : container creation possibility needs to be re-think then
    // deleted or implemented. Vitam Architects are
    // aware of this
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createContainer(@Context HttpHeaders headers) {
        Response response = checkTenantStrategyHeader(headers);
        if (response != null) {
            return response;
        }
        final Status status = Status.NOT_IMPLEMENTED;
        return Response.status(status).entity(getErrorEntity(status, status.getReasonPhrase())).build();
    }

    /**
     * Delete a container
     * <p>
     * Note : this is NOT to be handled in item #72.
     *
     * @param headers http header
     * @return Response NOT_IMPLEMENTED
     */
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteContainer(@Context HttpHeaders headers) {
        Response response = checkTenantStrategyHeader(headers);
        if (response != null) {
            return response;
        }
        final Status status = Status.NOT_IMPLEMENTED;
        return Response.status(status).entity(getErrorEntity(status, status.getReasonPhrase())).build();
    }

    /**
     * Get list of object type
     *
     * @param xcursor    the X-Cursor
     * @param xcursorId  the X-Cursor-Id if exists
     * @param strategyId the strategy to get offers
     * @param type       the object type to list
     * @return a response with listing elements
     */
    @Path(
        "/{type:UNIT|OBJECT|OBJECTGROUP|LOGBOOK|REPORT|MANIFEST|PROFILE|STORAGELOG|STORAGETRACEABILITY|RULES|DIP|AGENCIES|BACKUP" +
            "|BACKUP_OPERATION|CHECKLOGBOOKREPORTS|OBJECTGROUP_GRAPH|UNIT_GRAPH}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response listObjects(@HeaderParam(GlobalDataRest.X_CURSOR) boolean xcursor,
        @HeaderParam(GlobalDataRest.X_CURSOR_ID) String xcursorId,
        @HeaderParam(GlobalDataRest.X_STRATEGY_ID) String strategyId, @PathParam("type") DataCategory type) {
        final Response response = checkTenantStrategyHeader(strategyId);
        if (response != null) {
            return response;
        }
        try {
            ParametersChecker.checkParameter("X-Cursor is required", xcursor);
            ParametersChecker.checkParameter("Strategy ID is required", strategyId);
            RequestResponse<JsonNode> jsonNodeRequestResponse =
                distribution.listContainerObjects(strategyId, type, xcursorId);

            return jsonNodeRequestResponse.toResponse();
        } catch (IllegalArgumentException exc) {
            LOGGER.error(exc);
            return buildErrorResponse(VitamCode.STORAGE_MISSING_HEADER);
        } catch (Exception exc) {
            LOGGER.error(exc);
            return buildErrorResponse(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR);
        }
    }

    /**
     * Get offer log from referent offer
     *
     * @param strategyId      the strategy to get offers
     * @param type            the object type to list
     * @param offerLogRequest offer log request params
     * @return list of offer log
     */
    @Path(
        "/{type:UNIT|OBJECT|OBJECTGROUP|LOGBOOK|REPORT|MANIFEST|PROFILE|STORAGELOG|STORAGETRACEABILITY|RULES|DIP|AGENCIES|BACKUP" +
            "|BACKUP_OPERATION|CHECKLOGBOOKREPORTS|OBJECTGROUP_GRAPH|UNIT_GRAPH}/logs")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getOfferLogs(@HeaderParam(GlobalDataRest.X_STRATEGY_ID) String strategyId,
        @PathParam("type") DataCategory type, OfferLogRequest offerLogRequest) {

        final Response response = checkTenantStrategyHeader(strategyId);
        if (response != null) {
            return response;
        }
        try {
            ParametersChecker.checkParameter(STRATEGY_ID_IS_REQUIRED, strategyId);
            RequestResponse<OfferLog> jsonNodeRequestResponse =
                distribution.getOfferLogs(strategyId, type, offerLogRequest.getOffset(), offerLogRequest.getLimit(),
                    offerLogRequest.getOrder());

            return jsonNodeRequestResponse.toResponse();
        } catch (IllegalArgumentException exc) {
            LOGGER.error(exc);
            return VitamCodeHelper.toVitamError(VitamCode.STORAGE_BAD_REQUEST, null).toResponse();
        } catch (Exception exc) {
            LOGGER.error(exc);
            return VitamCodeHelper.toVitamError(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR, null).toResponse();
        }
    }

    /**
     * Get object metadata as json Note : this is NOT to be handled in item #72.
     *
     * @param headers  http header
     * @param objectId the id of the object
     * @return Response NOT_IMPLEMENTED
     */
    @Path("/info/{type}/{id_object}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getInformation(@Context HttpHeaders headers,
        @PathParam("type") String typeStr, @PathParam("id_object") String objectId) {

        DataCategory type = DataCategory.getByCollectionName(typeStr);

        String strategyId;
        final Response response = checkTenantStrategyHeader(headers);
        if (response == null) {
            strategyId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.STRATEGY_ID).get(0);
            if (!HttpHeaderHelper.hasValuesFor(headers, VitamHttpHeader.OFFERS_IDS)) {
                return buildErrorResponse(VitamCode.STORAGE_MISSING_HEADER);
            }
            String listOffer = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.OFFERS_IDS).get(0);
            List<String> offerIds = Arrays.asList(listOffer.split(","));

            JsonNode offerMetadataInfo;
            try {
                offerMetadataInfo = distribution.getContainerInformations(strategyId, type, objectId, offerIds);
            } catch (StorageException e) {
                LOGGER.error(e);
                return buildErrorResponse(VitamCode.STORAGE_NOT_FOUND);
            }
            return Response.status(Status.OK).entity(offerMetadataInfo).build();
        }
        return response;
    }

    /**
     * Get an object data
     *
     * @param headers  http header
     * @param objectId the id of the object
     * @return the stream
     * @throws IOException throws an IO Exception
     */
    @Path("/objects/{id_object}")
    @GET
    @Produces({MediaType.APPLICATION_OCTET_STREAM, CommonMediaType.ZIP})
    public Response getObject(@Context HttpHeaders headers, @PathParam("id_object") String objectId)
        throws IOException {
        VitamCode vitamCode = checkTenantStrategyHeaderAsync(headers);
        if (vitamCode != null) {
            return buildErrorResponse(vitamCode);
        }
        String strategyId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.STRATEGY_ID).get(0);

        try {
            return new VitamAsyncInputStreamResponse(
                getByCategory(objectId, DataCategory.OBJECT, strategyId, vitamCode),
                Status.OK, MediaType.APPLICATION_OCTET_STREAM_TYPE);
        } catch (final StorageNotFoundException exc) {
            LOGGER.error(exc);
            vitamCode = VitamCode.STORAGE_NOT_FOUND;
        } catch (final StorageException exc) {
            LOGGER.error(exc);
            vitamCode = VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR;
        }
        return buildErrorResponse(vitamCode);
    }

    /**
     * Get colection data.
     *
     * @param headers
     * @param backupfile
     * @return
     * @throws IOException
     */
    @Path("/backup/{backupfile}")
    @GET
    @Produces({MediaType.APPLICATION_OCTET_STREAM})
    public Response getBackupFile(@Context HttpHeaders headers, @PathParam("backupfile") String backupfile)
        throws IOException {
        VitamCode vitamCode = checkTenantStrategyHeaderAsync(headers);
        if (vitamCode != null) {
            return buildErrorResponse(vitamCode);
        }
        String strategyId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.STRATEGY_ID).get(0);

        try {
            return new VitamAsyncInputStreamResponse(
                getByCategory(backupfile, DataCategory.BACKUP, strategyId, vitamCode),
                Status.OK, MediaType.APPLICATION_OCTET_STREAM_TYPE);
        } catch (final StorageNotFoundException exc) {
            LOGGER.error(exc);
            vitamCode = VitamCode.STORAGE_NOT_FOUND;
        } catch (final StorageException exc) {
            LOGGER.error(exc);
            vitamCode = VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR;
        }
        return buildErrorResponse(vitamCode);
    }

    private Response getByCategory(String objectId, DataCategory category,
        String strategyId, VitamCode vitamCode)
        throws StorageException {
        if (vitamCode == null) {
            return distribution.getContainerByCategory(strategyId, objectId, category);
        }
        return buildErrorResponse(vitamCode);
    }

    /**
     * Post a new object
     *
     * @param httpServletRequest      http servlet request to get requester
     * @param headers                 http header
     * @param objectId                the id of the object
     * @param createObjectDescription the object description
     * @return Response response
     */
    // TODO P1 : remove httpServletRequest when requester information sent by
    // header (X-Requester)
    @Path("/objects/{id_object}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createObjectOrGetInformation(@Context HttpServletRequest httpServletRequest,
        @Context HttpHeaders headers,
        @PathParam("id_object") String objectId, ObjectDescription createObjectDescription) {
        // If the POST is a creation request
        if (createObjectDescription != null) {
            // TODO P1 : actually no X-Requester header, so send the
            // getRemoteAdr from HttpServletRequest
            return createObjectByType(headers, objectId, createObjectDescription, DataCategory.OBJECT,
                httpServletRequest.getRemoteAddr());
        } else {
            return getObjectInformationWithPost(headers, objectId);
        }
    }

    /**
     * Post a new backup operation
     *
     * @param httpServletRequest      http servlet request to get requester
     * @param headers                 http header
     * @param operationId             the id of the operation
     * @param createObjectDescription the object description for storage
     * @return
     */
    @Path("/backupoperations/{id_operation}")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createOrUpdateBackupOperation(@Context HttpServletRequest httpServletRequest,
        @Context HttpHeaders headers,
        @PathParam("id_operation") String operationId, ObjectDescription createObjectDescription) {
        return createObjectByType(headers, operationId, createObjectDescription, DataCategory.BACKUP_OPERATION,
            httpServletRequest.getRemoteAddr());
    }


    /**
     * @param strategyId
     * @return
     */
    @Path("/offers")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOffers(@HeaderParam(GlobalDataRest.X_STRATEGY_ID) String strategyId) {

        try {
            List<String> offerIds = distribution.getOfferIds(strategyId);

            return Response.status(Status.OK)
                .entity(JsonHandler.toJsonNode(offerIds))
                .build();
        } catch (InvalidParseOperationException | StorageException e) {
            return buildErrorResponse(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR);
        }
    }



    /**
     * Get a backup operation
     *
     * @param headers     http header
     * @param operationId the id of the operation
     * @return the stream
     */
    @Path("/backupoperations/{id_operation}")
    @GET
    @Produces({MediaType.APPLICATION_OCTET_STREAM, CommonMediaType.ZIP})
    public Response getBackupOperation(@Context HttpHeaders headers, @PathParam("id_operation") String operationId) {
        VitamCode vitamCode = checkTenantStrategyHeaderAsync(headers);
        if (vitamCode != null) {
            return buildErrorResponse(vitamCode);
        }
        String strategyId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.STRATEGY_ID).get(0);
        try {
            return new VitamAsyncInputStreamResponse(
                getByCategory(operationId, DataCategory.BACKUP_OPERATION, strategyId, vitamCode),
                Status.OK, MediaType.APPLICATION_OCTET_STREAM_TYPE);
        } catch (final StorageNotFoundException exc) {
            LOGGER.error(exc);
            vitamCode = VitamCode.STORAGE_NOT_FOUND;
        } catch (final StorageException exc) {
            LOGGER.error(exc);
            vitamCode = VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR;
        }
        return buildErrorResponse(vitamCode);
    }

    private Response getObjectInformationWithPost(HttpHeaders headers, String objectId) {
        // FIXME : What is this used for? Do we really need to support X_HTTP_METHOD_OVERRIDE for internal APIs?
        final Response response = checkTenantStrategyHeader(headers);
        if (response != null) {
            return response;
        }
        final Response responsePost = checkPostHeader(headers);
        if (responsePost == null) {
            return getInformation(headers, DataCategory.OBJECT.getCollectionName(), objectId);
        } else if (responsePost.getStatus() == Status.OK.getStatusCode()) {
            return Response.status(Status.PRECONDITION_FAILED).build();
        } else {
            return responsePost;
        }
    }

    /**
     * Delete an object
     *
     * @param headers  http header
     * @param objectId the id of the object
     * @return Response NOT_IMPLEMENTED
     */
    @Path("/objects/{id_object}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteObject(@Context HttpHeaders headers, @PathParam("id_object") String objectId) {
        String strategyId, digestAlgorithm, digest;
        Response response = checkTenantStrategyHeader(headers);
        if (response != null) {
            return response;
        }
        if (!DataCategory.OBJECT.canDelete()) {
            return Response.status(Status.UNAUTHORIZED).entity(getErrorEntity(Status.UNAUTHORIZED, "Cannot be deleted"))
                .build();
        }
        response = checkDigestAlgorithmHeader(headers);
        if (response == null) {
            strategyId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.STRATEGY_ID).get(0);
            digest = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.X_DIGEST).get(0);
            digestAlgorithm = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.X_DIGEST_ALGORITHM).get(0);
            try {
                distribution.deleteObject(strategyId, objectId, digest, DigestType.fromValue(digestAlgorithm));
                return Response.status(Status.NO_CONTENT).build();
            } catch (final StorageNotFoundException exc) {
                LOGGER.error(exc);
                return buildErrorResponse(VitamCode.STORAGE_NOT_FOUND);
            } catch (final StorageException exc) {
                LOGGER.error(exc);
                return buildErrorResponse(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR);
            } catch (final IllegalArgumentException exc) {
                LOGGER.error(exc);
                return buildErrorResponse(VitamCode.STORAGE_BAD_REQUEST);
            }
        }
        return response;
    }

    /**
     * Check the existence of an object
     *
     * @param headers  http header
     * @param objectId the id of the object
     * @return Response NOT_IMPLEMENTED
     */
    @Path(
        "/{type:UNIT|OBJECT|OBJECTGROUP|LOGBOOK|REPORT|MANIFEST|PROFILE|STORAGELOG|STORAGETRACEABILITY|RULES|DIP|AGENCIES|BACKUP" +
            "|BACKUP_OPERATION|CHECKLOGBOOKREPORTS|OBJECTGROUP_GRAPH|UNIT_GRAPH|DISTRIBUTIONREPORTS|ACCESSION_REGISTER_DETAIL|ACCESSION_REGISTER_SYMBOLIC}/{id_object}")
    @HEAD
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response checkObject(@Context HttpHeaders headers, @PathParam("type") DataCategory type,
        @PathParam("id_object") String objectId) {
        String strategyId;
        final Response response = checkTenantStrategyHeader(headers);
        if (response == null) {
            strategyId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.STRATEGY_ID).get(0);
            if (!HttpHeaderHelper.hasValuesFor(headers, VitamHttpHeader.OFFERS_IDS)) {
                return buildErrorResponse(VitamCode.STORAGE_MISSING_HEADER);
            }
            String listOffer = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.OFFERS_IDS).get(0);
            List<String> offerIds = Arrays.asList(listOffer.split(","));
            try {
                if (!distribution.checkObjectExisting(strategyId, objectId, type, offerIds)) {
                    return Response.status(Status.NOT_FOUND).build();
                }
            } catch (final StorageException e) {
                LOGGER.error(e);
                return buildErrorResponse(VitamCode.STORAGE_NOT_FOUND);
            }
        }
        return Response.status(Status.NO_CONTENT).build();
    }

    /**
     * Get a list of logbooks
     * <p>
     * Note : this is NOT to be handled in item #72.
     *
     * @param headers http header
     * @return Response NOT_IMPLEMENTED
     */
    @Path("/logbooks")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getLogbooks(@Context HttpHeaders headers) {
        Response response = checkTenantStrategyHeader(headers);
        if (response != null) {
            return response;
        }
        final Status status = Status.NOT_IMPLEMENTED;
        return Response.status(status).entity(getErrorEntity(status, status.getReasonPhrase())).build();
    }

    /**
     * Get an object
     * <p>
     * Note : this is NOT to be handled in item #72.
     *
     * @param headers   http header
     * @param logbookId the id of the logbook
     * @return Response NOT_IMPLEMENTED
     */
    @Path("/logbooks/{id_logbook}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getLogbook(@Context HttpHeaders headers, @PathParam("id_logbook") String logbookId) {
        Response response = checkTenantStrategyHeader(headers);
        if (response != null) {
            return response;
        }
        final Status status = Status.NOT_IMPLEMENTED;
        return Response.status(status).entity(getErrorEntity(status, status.getReasonPhrase())).build();
    }

    /**
     * @param headers  http header
     * @param objectId the id of the object
     * @return the stream
     * @throws IOException exception
     */
    @Path("/logbooks/{id_logbook}")
    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getLogbookStream(@Context HttpHeaders headers, @PathParam("id_logbook") String objectId)
        throws IOException {
        VitamCode vitamCode = checkTenantStrategyHeaderAsync(headers);
        if (vitamCode != null) {
            return buildErrorResponse(vitamCode);
        }
        String strategyId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.STRATEGY_ID).get(0);
        try {
            return new VitamAsyncInputStreamResponse(
                getByCategory(objectId, DataCategory.LOGBOOK, strategyId, vitamCode),
                Status.OK, MediaType.APPLICATION_OCTET_STREAM_TYPE);
        } catch (final StorageNotFoundException exc) {
            LOGGER.error(exc);
            vitamCode = VitamCode.STORAGE_NOT_FOUND;
        } catch (final StorageException exc) {
            LOGGER.error(exc);
            vitamCode = VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR;
        }
        return buildErrorResponse(vitamCode);
    }

    /**
     * Post a new object
     *
     * @param httpServletRequest      http servlet request to get requester
     * @param headers                 http header
     * @param logbookId               the id of the logbookId
     * @param createObjectDescription the workspace information about logbook to be created
     * @return the stream
     */
    // TODO P1: remove httpServletRequest when requester information sent by
    // header (X-Requester)
    @Path("/logbooks/{id_logbook}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createLogbook(@Context HttpServletRequest httpServletRequest, @Context HttpHeaders headers,
        @PathParam("id_logbook") String logbookId, ObjectDescription createObjectDescription) {
        if (createObjectDescription == null) {
            return getLogbook(headers, logbookId);
        } else {
            // TODO P1: actually no X-Requester header, so send the getRemoteAdr
            // from HttpServletRequest
            return createObjectByType(headers, logbookId, createObjectDescription, DataCategory.LOGBOOK,
                httpServletRequest.getRemoteAddr());
        }
    }

    /**
     * Delete a logbook Note : this is NOT to be handled in item #72.
     *
     * @param headers   http header
     * @param logbookId the id of the logbook
     * @return Response UNAUTHORIZED
     */
    @Path("/logbooks/{id_logbook}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteLogbook(@Context HttpHeaders headers, @PathParam("id_logbook") String logbookId) {
        Response response = checkTenantStrategyHeader(headers);
        if (response != null) {
            return response;
        }
        Status status = Status.NOT_IMPLEMENTED;
        if (!DataCategory.LOGBOOK.canDelete()) {
            status = Status.UNAUTHORIZED;
        }
        return Response.status(status).entity(getErrorEntity(status, status.getReasonPhrase())).build();
    }

    /**
     * Get a list of units
     * <p>
     * Note : this is NOT to be handled in item #72.
     *
     * @param headers http header
     * @return Response NOT_IMPLEMENTED
     */
    @Path("/units")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getUnits(@Context HttpHeaders headers) {
        Response response = checkTenantStrategyHeader(headers);
        if (response != null) {
            return response;
        }
        final Status status = Status.NOT_IMPLEMENTED;
        return Response.status(status).entity(getErrorEntity(status, status.getReasonPhrase())).build();
    }

    /**
     * Get a unit
     *
     * @param headers  http header
     * @param metadataId the id of the unit
     * @return the stream
     */
    @Path("/units/{id_md}")
    @GET
    @Produces({MediaType.APPLICATION_OCTET_STREAM, CommonMediaType.ZIP})
    public Response getUnit(@Context HttpHeaders headers, @PathParam("id_md") String metadataId) {
        VitamCode vitamCode = checkTenantStrategyHeaderAsync(headers);
        if (vitamCode != null) {
            return buildErrorResponse(vitamCode);
        }
        String strategyId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.STRATEGY_ID).get(0);
        try {
            return new VitamAsyncInputStreamResponse(
                getByCategory(metadataId, DataCategory.UNIT, strategyId, vitamCode),
                Status.OK, MediaType.APPLICATION_OCTET_STREAM_TYPE);
        } catch (final StorageNotFoundException exc) {
            LOGGER.error(exc);
            vitamCode = VitamCode.STORAGE_NOT_FOUND;
        } catch (final StorageException exc) {
            LOGGER.error(exc);
            vitamCode = VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR;
        }
        return buildErrorResponse(vitamCode);
    }

    /**
     * Post a new unit metadata
     *
     * @param httpServletRequest      http servlet request to get requester
     * @param headers                 http header
     * @param metadataId              the id of the unit metadata
     * @param createObjectDescription the workspace description of the unit to be created
     * @return Response containing result infos
     */
    // TODO P1: remove httpServletRequest when requester information sent by
    // header (X-Requester)
    @Path("/units/{id_md}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createUnitMetadata(@Context HttpServletRequest httpServletRequest, @Context HttpHeaders headers,
        @PathParam("id_md") String metadataId, ObjectDescription createObjectDescription) {
        return createObjectByType(headers, metadataId, createObjectDescription, DataCategory.UNIT,
            httpServletRequest.getRemoteAddr());
    }

    /**
     * Update a unit metadata
     * <p>
     * Note : this is NOT to be handled in item #72.
     *
     * @param headers    http header
     * @param metadataId the id of the unit metadata
     * @param query      the query as a JsonNode
     * @return Response NOT_IMPLEMENTED
     */
    @Path("/units/{id_md}")
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateUnitMetadata(@Context HttpHeaders headers, @PathParam("id_md") String metadataId,
        JsonNode query) {
        Response response = checkTenantStrategyHeader(headers);
        if (response != null) {
            return response;
        }
        Status status = Status.NOT_IMPLEMENTED;
        if (!DataCategory.UNIT.canUpdate()) {
            status = Status.UNAUTHORIZED;
        }
        return Response.status(status).entity(getErrorEntity(status, status.getReasonPhrase())).build();
    }

    /**
     * Delete a unit metadata
     *
     * @param headers    http header
     * @param metadataId the id of the unit metadata
     * @return Response NOT_IMPLEMENTED
     */
    @Path("/units/{id_md}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteUnit(@Context HttpHeaders headers, @PathParam("id_md") String metadataId) {
        Response response = checkTenantStrategyHeader(headers);
        if (response != null) {
            return response;
        }
        Status status = Status.NOT_IMPLEMENTED;
        if (!DataCategory.UNIT.canDelete()) {
            status = Status.UNAUTHORIZED;
        }
        return Response.status(status).entity(getErrorEntity(status, status.getReasonPhrase())).build();
    }

    /**
     * Get a list of Object Groups
     * <p>
     * Note : this is NOT to be handled in item #72.
     *
     * @param headers http header
     * @return Response NOT_IMPLEMENTED
     */
    @Path("/objectgroups")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getObjectGroups(@Context HttpHeaders headers) {
        Response response = checkTenantStrategyHeader(headers);
        if (response != null) {
            return response;
        }
        final Status status = Status.NOT_IMPLEMENTED;
        return Response.status(status).entity(getErrorEntity(status, status.getReasonPhrase())).build();
    }

    /**
     * Get a Object Group
     * <p>
     * Note : this is NOT to be handled in item #72.
     *
     * @param headers    http header
     * @param metadataId the id of the Object Group metadata
     * @return Response NOT_IMPLEMENTED
     */
    @Path("/objectgroups/{id_md}")
    @GET
    @Produces({MediaType.APPLICATION_OCTET_STREAM, CommonMediaType.ZIP})
    public Response getObjectGroup(@Context HttpHeaders headers, @PathParam("id_md") String metadataId) {
        VitamCode vitamCode = checkTenantStrategyHeaderAsync(headers);
        if (vitamCode != null) {
            return buildErrorResponse(vitamCode);
        }
        String strategyId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.STRATEGY_ID).get(0);
        try {
            return new VitamAsyncInputStreamResponse(
                getByCategory(metadataId, DataCategory.OBJECTGROUP, strategyId, vitamCode),
                Status.OK, MediaType.APPLICATION_OCTET_STREAM_TYPE);
        } catch (final StorageNotFoundException exc) {
            LOGGER.error(exc);
            vitamCode = VitamCode.STORAGE_NOT_FOUND;
        } catch (final StorageException exc) {
            LOGGER.error(exc);
            vitamCode = VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR;
        }
        return buildErrorResponse(vitamCode);
    }

    /**
     * Post a new Object Group metadata
     *
     * @param httpServletRequest      http servlet request to get requester
     * @param headers                 http header
     * @param metadataId              the id of the Object Group metadata
     * @param createObjectDescription the workspace description of the unit to be created
     * @return Response Created with informations
     */
    // TODO P1: remove httpServletRequest when requester information sent by
    // header (X-Requester)
    @Path("/objectgroups/{id_md}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createObjectGroup(@Context HttpServletRequest httpServletRequest, @Context HttpHeaders headers,
        @PathParam("id_md") String metadataId, ObjectDescription createObjectDescription) {
        // TODO P1: actually no X-Requester header, so send the getRemoteAdr
        // from HttpServletRequest
        return createObjectByType(headers, metadataId, createObjectDescription, DataCategory.OBJECTGROUP,
            httpServletRequest.getRemoteAddr());
    }

    /**
     * Update a Object Group metadata
     * <p>
     * Note : this is NOT to be handled in item #72.
     *
     * @param headers    http header
     * @param metadataId the id of the unit metadata
     * @param query      the query as a JsonNode
     * @return Response NOT_IMPLEMENTED
     */
    @Path("/objectgroups/{id_md}")
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateObjectGroupMetadata(@Context HttpHeaders headers, @PathParam("id_md") String metadataId,
        JsonNode query) {
        Response response = checkTenantStrategyHeader(headers);
        if (response != null) {
            return response;
        }
        Status status = Status.NOT_IMPLEMENTED;
        if (!DataCategory.OBJECTGROUP.canUpdate()) {
            status = Status.UNAUTHORIZED;
        }
        return Response.status(status).entity(getErrorEntity(status, status.getReasonPhrase())).build();
    }

    /**
     * Delete a Object Group metadata
     * <p>
     * Note : this is NOT to be handled in item #72.
     *
     * @param headers    http header
     * @param metadataId the id of the Object Group metadata
     * @return Response NOT_IMPLEMENTED
     */
    @Path("/objectgroups/{id_md}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteObjectGroup(@Context HttpHeaders headers, @PathParam("id_md") String metadataId) {
        Response response = checkTenantStrategyHeader(headers);
        if (response != null) {
            return response;
        }
        Status status = Status.NOT_IMPLEMENTED;
        if (!DataCategory.OBJECTGROUP.canDelete()) {
            status = Status.UNAUTHORIZED;
        }
        return Response.status(status).entity(getErrorEntity(status, status.getReasonPhrase())).build();
    }

    private Response buildErrorResponse(VitamCode vitamCode) {
        return Response.status(vitamCode.getStatus())
            .entity(new RequestResponseError().setError(new VitamError(VitamCodeHelper.getCode(vitamCode))
                .setContext(vitamCode.getService().getName()).setState(vitamCode.getDomain().getName())
                .setMessage(vitamCode.getMessage()).setDescription(vitamCode.getMessage())).toString())
            .build();
    }

    private Response badRequestResponse(String message) {
        return Response.status(Status.BAD_REQUEST).entity("{\"error\":\"" + message + "\"}").build();
    }

    /**
     * Post a new object
     *
     * @param httpServletRequest      http servlet request to get requester
     * @param headers                 http header
     * @param reportId                the id of the object
     * @param createObjectDescription the object description
     * @return Response
     */
    // TODO P1: remove httpServletRequest when requester information sent by
    // header (X-Requester)
    @Path("/reports/{id_report}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createReportOrGetInformation(@Context HttpServletRequest httpServletRequest,
        @Context HttpHeaders headers,
        @PathParam("id_report") String reportId, ObjectDescription createObjectDescription) {
        // If the POST is a creation request
        if (createObjectDescription != null) {
            // TODO P1: actually no X-Requester header, so send the
            // getRemoteAddr from HttpServletRequest
            return createObjectByType(headers, reportId, createObjectDescription, DataCategory.REPORT,
                httpServletRequest.getRemoteAddr());
        } else {
            return getObjectInformationWithPost(headers, reportId);
        }
    }

    /**
     * Get a report
     *
     * @param headers  http header
     * @param objectId the id of the object
     * @return the stream
     * @throws IOException throws an IO Exception
     */
    @Path("/reports/{id_report}")
    @GET
    @Produces({MediaType.APPLICATION_OCTET_STREAM, CommonMediaType.ZIP})
    public Response getReport(@Context HttpHeaders headers, @PathParam("id_report") String objectId)
        throws IOException {
        VitamCode vitamCode = checkTenantStrategyHeaderAsync(headers);
        if (vitamCode != null) {
            return buildErrorResponse(vitamCode);
        }
        String strategyId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.STRATEGY_ID).get(0);
        try {
            return new VitamAsyncInputStreamResponse(
                getByCategory(objectId, DataCategory.REPORT, strategyId, vitamCode),
                Status.OK, MediaType.APPLICATION_OCTET_STREAM_TYPE);
        } catch (final StorageNotFoundException exc) {
            LOGGER.error(exc);
            vitamCode = VitamCode.STORAGE_NOT_FOUND;
        } catch (final StorageException exc) {
            LOGGER.error(exc);
            vitamCode = VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR;
        }
        return buildErrorResponse(vitamCode);
    }

    // TODO P1: requester have to come from vitam headers (X-Requester), but
    // does not exist actually, so use
    // getRemoteAdr from HttpServletRequest passed as parameter (requester)
    // Change it when the good header is sent
    private Response createObjectByType(HttpHeaders headers, String objectId, ObjectDescription createObjectDescription,
        DataCategory category, String requester) {
        final Response response = checkTenantStrategyHeader(headers);
        if (response == null) {
            VitamCode vitamCode;
            final String strategyId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.STRATEGY_ID).get(0);
            try {
                final StoredInfoResult result =
                    distribution.storeData(strategyId, objectId, createObjectDescription, category,
                        requester);
                return Response.status(Status.CREATED).entity(result).build();
            } catch (final StorageNotFoundException exc) {
                LOGGER.error(exc);
                vitamCode = VitamCode.STORAGE_NOT_FOUND;
            } catch (final StorageAlreadyExistsException exc) {
                LOGGER.error(exc);
                vitamCode = VitamCode.STORAGE_DRIVER_OBJECT_ALREADY_EXISTS;
            } catch (final StorageException exc) {
                LOGGER.error(exc);
                vitamCode = VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR;
            } catch (final UnsupportedOperationException exc) {
                LOGGER.error(exc);
                vitamCode = VitamCode.STORAGE_BAD_REQUEST;
            }
            // If here, an error occurred
            return buildErrorResponse(vitamCode);
        }
        return response;
    }

    /**
     * Post a new object manifest
     *
     * @param httpServletRequest      http servlet request to get requester
     * @param headers                 http header
     * @param manifestId              the id of the object
     * @param createObjectDescription the object description
     * @return Response
     */
    // TODO P1: remove httpServletRequest when requester information sent by
    // header (X-Requester)
    @Path("/manifests/{id_manifest}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createManifestOrGetInformation(@Context HttpServletRequest httpServletRequest,
        @Context HttpHeaders headers,
        @PathParam("id_manifest") String manifestId, ObjectDescription createObjectDescription) {
        // If the POST is a creation request
        if (createObjectDescription != null) {
            // TODO P1: actually no X-Requester header, so send the
            // getRemoteAddr from HttpServletRequest
            return createObjectByType(headers, manifestId, createObjectDescription, DataCategory.MANIFEST,
                httpServletRequest.getRemoteAddr());
        } else {
            return getObjectInformationWithPost(headers, manifestId);
        }
    }

    /**
     * getManifest stored by ingest operation
     *
     * @param headers
     * @param objectId
     * @return the stream
     * @throws IOException
     */
    @Path("/manifests/{id_manifest}")
    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getManifest(@Context HttpHeaders headers, @PathParam("id_manifest") String objectId)
        throws IOException {
        VitamCode vitamCode = checkTenantStrategyHeaderAsync(headers);
        if (vitamCode != null) {
            return buildErrorResponse(vitamCode);
        }
        String strategyId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.STRATEGY_ID).get(0);
        try {
            return new VitamAsyncInputStreamResponse(
                getByCategory(objectId, DataCategory.MANIFEST, strategyId, vitamCode),
                Status.OK, MediaType.APPLICATION_OCTET_STREAM_TYPE);
        } catch (final StorageNotFoundException exc) {
            LOGGER.error(exc);
            vitamCode = VitamCode.STORAGE_NOT_FOUND;
        } catch (final StorageException exc) {
            LOGGER.error(exc);
            vitamCode = VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR;
        }
        return buildErrorResponse(vitamCode);

    }

    /**
     * Backup storage log
     *
     * @param xTenantId the tenant id
     * @return the response with a specific HTTP status
     */
    @POST
    @Path("/storage/backup")
    @Produces(MediaType.APPLICATION_JSON)
    public Response backupStorageLog(@HeaderParam(GlobalDataRest.X_TENANT_ID) String xTenantId) {
        if (Strings.isNullOrEmpty(xTenantId)) {
            LOGGER.error(MISSING_THE_TENANT_ID_X_TENANT_ID);
            return Response.status(Status.BAD_REQUEST).build();
        }
        try {
            Integer tenantId = Integer.parseInt(xTenantId);
            VitamThreadUtils.getVitamSession().setTenantId(tenantId);
            final GUID guid = storageLogAdministration.backupStorageLog();
            final List<String> resultAsJson = new ArrayList<>();
            resultAsJson.add(guid.toString());
            return Response.status(Status.OK)
                .entity(new RequestResponseOK<String>()
                    .addAllResults(resultAsJson))
                .build();

        } catch (LogbookClientServerException | IOException |
            StorageLogException | LogbookClientAlreadyExistsException | LogbookClientBadRequestException e) {
            LOGGER.error("unable to generate backup log", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(new RequestResponseOK())
                .build();
        }
    }

    /**
     * Run storage logbook secure operation
     *
     * @param xTenantId the tenant id
     * @return the response with a specific HTTP status
     */
    @POST
    @Path("/storage/traceability")
    @Produces(MediaType.APPLICATION_JSON)
    public Response traceabilityStorageLogbook(@HeaderParam(GlobalDataRest.X_TENANT_ID) String xTenantId) {
        if (Strings.isNullOrEmpty(xTenantId)) {
            LOGGER.error(MISSING_THE_TENANT_ID_X_TENANT_ID);
            return Response.status(Status.BAD_REQUEST).build();
        }
        try {
            Integer tenantId = Integer.parseInt(xTenantId);
            final GUID guid = GUIDFactory.newOperationLogbookGUID(tenantId);

            VitamThreadUtils.getVitamSession().setTenantId(tenantId);
            VitamThreadUtils.getVitamSession().setRequestId(guid);

            traceabilityLogbookAdministration.generateTraceabilityStorageLogbook(guid);
            return Response.status(Status.OK)
                .entity(new RequestResponseOK<GUID>()
                    .addResult(guid))
                .build();

        } catch (TraceabilityException e) {
            LOGGER.error("unable to generate secure  log", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(new RequestResponseOK())
                .build();
        }
    }

    /**
     * Post a new object
     *
     * @param httpServletRequest      http servlet request to get requester
     * @param headers                 http header
     * @param storageLogname          the id of the object
     * @param createObjectDescription the object description
     * @return Response
     */
    // header (X-Requester)
    @Path("/storagelog/{storagelogname}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createStorageLog(@Context HttpServletRequest httpServletRequest,
        @Context HttpHeaders headers,
        @PathParam("storagelogname") String storageLogname, ObjectDescription createObjectDescription) {
        // If the POST is a creation request
        if (createObjectDescription != null) {
            return createObjectByType(headers, storageLogname, createObjectDescription, DataCategory.STORAGELOG,
                httpServletRequest.getRemoteAddr());
        } else {
            return getObjectInformationWithPost(headers, storageLogname);
        }
    }

    /**
     * Post a new object
     *
     * @param httpServletRequest      http servlet request to get requester
     * @param headers                 http header
     * @param storageLogname          storage log name
     * @param createObjectDescription the object description
     * @return Response
     */
    // header (X-Requester)
    @Path("/storagetraceability/{storagetraceabilityname}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createStorageTraceability(@Context HttpServletRequest httpServletRequest,
        @Context HttpHeaders headers,
        @PathParam("storagetraceabilityname") String storageLogname, ObjectDescription createObjectDescription) {
        // If the POST is a creation request
        if (createObjectDescription != null) {
            return createObjectByType(headers, storageLogname, createObjectDescription,
                DataCategory.STORAGETRACEABILITY,
                httpServletRequest.getRemoteAddr());
        } else {
            return getObjectInformationWithPost(headers, storageLogname);
        }
    }

    /**
     * Get a storage traceability file
     *
     * @param headers  http header
     * @param filename the id of the object
     * @return the stream
     * @throws IOException throws an IO Exception
     */
    @Path("/storagetraceability/{storagetraceability_name}")
    @GET
    @Produces({MediaType.APPLICATION_OCTET_STREAM, CommonMediaType.ZIP})
    public Response downloadStorageTraceability(@Context HttpHeaders headers,
        @PathParam("storagetraceability_name") String filename) {
        VitamCode vitamCode = checkTenantStrategyHeaderAsync(headers);
        if (vitamCode != null) {
            return buildErrorResponse(vitamCode);
        }
        String strategyId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.STRATEGY_ID).get(0);
        try {
            return new VitamAsyncInputStreamResponse(
                getByCategory(filename, DataCategory.STORAGETRACEABILITY, strategyId, vitamCode),
                Status.OK, MediaType.APPLICATION_OCTET_STREAM_TYPE);
        } catch (final StorageNotFoundException exc) {
            LOGGER.error(exc);
            vitamCode = VitamCode.STORAGE_NOT_FOUND;
        } catch (final StorageException exc) {
            LOGGER.error(exc);
            vitamCode = VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR;
        }
        return buildErrorResponse(vitamCode);
    }

    /**
     * Post a new object
     *
     * @param httpServletRequest      http servlet request to get requester
     * @param headers                 http header
     * @param backupfile              the id of the object
     * @param createObjectDescription the object description
     * @return Response
     */
    // header (X-Requester)
    @Path("/backup/{backupfile}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createBackupFile(@Context HttpServletRequest httpServletRequest,
        @Context HttpHeaders headers,
        @PathParam("backupfile") String backupfile, ObjectDescription createObjectDescription) {
        // If the POST is a creation request
        if (createObjectDescription != null) {
            return createObjectByType(headers, backupfile, createObjectDescription, DataCategory.BACKUP,
                httpServletRequest.getRemoteAddr());
        } else {
            return getObjectInformationWithPost(headers, backupfile);
        }
    }

    /**
     * Post a new object
     *
     * @param httpServletRequest      http servlet request to get requester
     * @param headers                 http header
     * @param ruleFile                the id of the object
     * @param createObjectDescription the object description
     * @return Response
     */
    // header (X-Requester)
    @Path("/rules/{rulefile}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createRuleFile(@Context HttpServletRequest httpServletRequest,
        @Context HttpHeaders headers,
        @PathParam("rulefile") String ruleFile, ObjectDescription createObjectDescription) {
        // If the POST is a creation request
        if (createObjectDescription != null) {
            return createObjectByType(headers, ruleFile, createObjectDescription, DataCategory.RULES,
                httpServletRequest.getRemoteAddr());
        } else {
            return getObjectInformationWithPost(headers, ruleFile);
        }
    }

    @Path("/rules/{id_object}")
    @GET
    @Produces({MediaType.APPLICATION_OCTET_STREAM})
    public Response getRuleFile(@Context HttpHeaders headers,
        @PathParam("id_object") String objectId)
        throws IOException {
        VitamCode vitamCode = checkTenantStrategyHeaderAsync(headers);
        if (vitamCode != null) {
            return buildErrorResponse(vitamCode);
        }
        String strategyId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.STRATEGY_ID).get(0);
        try {
            return new VitamAsyncInputStreamResponse(
                getByCategory(objectId, DataCategory.RULES, strategyId, vitamCode),
                Status.OK, MediaType.APPLICATION_OCTET_STREAM_TYPE);
        } catch (final StorageNotFoundException exc) {
            LOGGER.error(exc);
            vitamCode = VitamCode.STORAGE_NOT_FOUND;
        } catch (final StorageException exc) {
            LOGGER.error(exc);
            vitamCode = VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR;
        }
        return buildErrorResponse(vitamCode);
    }

    /**
     * Post a new ckeck logbook report file
     *
     * @param httpServletRequest      http servlet request to get requester
     * @param headers                 http header
     * @param logbookreportfile       the id of the object
     * @param createObjectDescription the object description
     * @return Response
     */
    @Path("/checklogbookreports/{logbookreportfile}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createlogbookreportFile(@Context HttpServletRequest httpServletRequest,
        @Context HttpHeaders headers,
        @PathParam("logbookreportfile") String logbookreportfile, ObjectDescription createObjectDescription) {
        // If the POST is a creation request
        if (createObjectDescription != null) {
            return createObjectByType(headers, logbookreportfile, createObjectDescription,
                DataCategory.CHECKLOGBOOKREPORTS,
                httpServletRequest.getRemoteAddr());
        } else {
            return getObjectInformationWithPost(headers, logbookreportfile);
        }
    }

    /**
     * Get colection data.
     *
     * @param headers
     * @param logbookreportfile
     * @return
     * @throws IOException
     */
    @Path("/checklogbookreports/{logbookreportfile}")
    @GET
    @Produces({MediaType.APPLICATION_OCTET_STREAM})
    public Response getlogbookreportFile(@Context HttpHeaders headers,
        @PathParam("logbookreportfile") String logbookreportfile)
        throws IOException {
        VitamCode vitamCode = checkTenantStrategyHeaderAsync(headers);
        if (vitamCode != null) {
            return buildErrorResponse(vitamCode);
        }
        String strategyId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.STRATEGY_ID).get(0);

        try {
            return new VitamAsyncInputStreamResponse(
                getByCategory(logbookreportfile, DataCategory.CHECKLOGBOOKREPORTS, strategyId, vitamCode),
                Status.OK, MediaType.APPLICATION_OCTET_STREAM_TYPE);
        } catch (final StorageNotFoundException exc) {
            LOGGER.error(exc);
            vitamCode = VitamCode.STORAGE_NOT_FOUND;
        } catch (final StorageException exc) {
            LOGGER.error(exc);
            vitamCode = VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR;
        }
        return buildErrorResponse(vitamCode);
    }


    /**
     * Create a new graph zip file
     *
     * @param httpServletRequest      http servlet request to get requester
     * @param headers                 http header
     * @param graph_file_name         the id of the object
     * @param createObjectDescription the object description
     * @return Response
     */
    @Path("/unitgraph/{graph_file_name}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createUnitGraphFile(@Context HttpServletRequest httpServletRequest,
        @Context HttpHeaders headers,
        @PathParam("graph_file_name") String graph_file_name, ObjectDescription createObjectDescription) {
        // If the POST is a creation request
        if (createObjectDescription != null) {
            return createObjectByType(headers, graph_file_name, createObjectDescription,
                DataCategory.UNIT_GRAPH,
                httpServletRequest.getRemoteAddr());
        } else {
            return getObjectInformationWithPost(headers, graph_file_name);
        }
    }

    /**
     * Get graph zip file
     *
     * @param headers
     * @param graph_file_name
     * @return
     * @throws IOException
     */
    @Path("/unitgraph/{graph_file_name}")
    @GET
    @Produces({MediaType.APPLICATION_OCTET_STREAM})
    public Response getUnitGraphFile(@Context HttpHeaders headers,
        @PathParam("graph_file_name") String graph_file_name) {
        VitamCode vitamCode = checkTenantStrategyHeaderAsync(headers);
        if (vitamCode != null) {
            return buildErrorResponse(vitamCode);
        }
        String strategyId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.STRATEGY_ID).get(0);

        try {
            return new VitamAsyncInputStreamResponse(
                getByCategory(graph_file_name, DataCategory.UNIT_GRAPH, strategyId, vitamCode),
                Status.OK, MediaType.APPLICATION_OCTET_STREAM_TYPE);
        } catch (final StorageNotFoundException exc) {
            LOGGER.error(exc);
            vitamCode = VitamCode.STORAGE_NOT_FOUND;
        } catch (final StorageException exc) {
            LOGGER.error(exc);
            vitamCode = VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR;
        }
        return buildErrorResponse(vitamCode);
    }


    /**
     * Create a new graph zip file
     *
     * @param httpServletRequest      http servlet request to get requester
     * @param headers                 http header
     * @param graph_file_name         the id of the object
     * @param createObjectDescription the object description
     * @return Response
     */
    @Path("/objectgroupgraph/{graph_file_name}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createObjectGroupGraphFile(@Context HttpServletRequest httpServletRequest,
        @Context HttpHeaders headers,
        @PathParam("graph_file_name") String graph_file_name, ObjectDescription createObjectDescription) {
        // If the POST is a creation request
        if (createObjectDescription != null) {
            return createObjectByType(headers, graph_file_name, createObjectDescription,
                DataCategory.OBJECTGROUP_GRAPH,
                httpServletRequest.getRemoteAddr());
        } else {
            return getObjectInformationWithPost(headers, graph_file_name);
        }
    }

    /**
     * Get graph zip file
     *
     * @param headers
     * @param graph_file_name
     * @return
     * @throws IOException
     */
    @Path("/objectgroupgraph/{graph_file_name}")
    @GET
    @Produces({MediaType.APPLICATION_OCTET_STREAM})
    public Response getObjectGroupGraphFile(@Context HttpHeaders headers,
        @PathParam("graph_file_name") String graph_file_name) {
        VitamCode vitamCode = checkTenantStrategyHeaderAsync(headers);
        if (vitamCode != null) {
            return buildErrorResponse(vitamCode);
        }
        String strategyId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.STRATEGY_ID).get(0);

        try {
            return new VitamAsyncInputStreamResponse(
                getByCategory(graph_file_name, DataCategory.OBJECTGROUP_GRAPH, strategyId, vitamCode),
                Status.OK, MediaType.APPLICATION_OCTET_STREAM_TYPE);
        } catch (final StorageNotFoundException exc) {
            LOGGER.error(exc);
            vitamCode = VitamCode.STORAGE_NOT_FOUND;
        } catch (final StorageException exc) {
            LOGGER.error(exc);
            vitamCode = VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR;
        }
        return buildErrorResponse(vitamCode);
    }


    /**
     * Post a new object
     *
     * @param httpServletRequest      http servlet request to get requester
     * @param headers                 http header
     * @param agencyfile              the id of the object
     * @param createObjectDescription the object description
     * @return Response
     */
    // header (X-Requester)
    @Path("/agencies/{agencyfile}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response creatAgencyfileFile(@Context HttpServletRequest httpServletRequest,
        @Context HttpHeaders headers,
        @PathParam("agencyfile") String agencyfile, ObjectDescription createObjectDescription) {
        // If the POST is a creation request
        if (createObjectDescription != null) {
            return createObjectByType(headers, agencyfile, createObjectDescription, DataCategory.AGENCIES,
                httpServletRequest.getRemoteAddr());
        } else {
            return getObjectInformationWithPost(headers, agencyfile);
        }
    }

    /**
     * Post a new object
     *
     * @param httpServletRequest      http servlet request to get requester
     * @param headers                 http header
     * @param guid                    the id of the object
     * @param createObjectDescription the object description
     * @return Response
     */
    @Path("/dip/{guid}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createDIP(@Context HttpServletRequest httpServletRequest,
        @Context HttpHeaders headers,
        @PathParam("guid") String guid, ObjectDescription createObjectDescription) {
        // If the POST is a creation request
        if (createObjectDescription != null) {
            return createObjectByType(headers, guid, createObjectDescription, DataCategory.DIP,
                httpServletRequest.getRemoteAddr());
        } else {
            return getObjectInformationWithPost(headers, guid);
        }
    }

    /**
     * read a dip
     *
     * @param httpServletRequest http servlet request to get requester
     * @param headers            http header
     * @param guid               the id of the object
     * @return Response
     */
    @Path("/dip/{guid}")
    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response readDIP(@Context HttpServletRequest httpServletRequest,
        @Context HttpHeaders headers,
        @PathParam("guid") String guid) {
        // If the POST is a creation request
        VitamCode vitamCode = checkTenantStrategyHeaderAsync(headers);
        if (vitamCode != null) {
            return buildErrorResponse(vitamCode);
        }
        String strategyId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.STRATEGY_ID).get(0);
        try {
            return new VitamAsyncInputStreamResponse(
                getByCategory(guid, DataCategory.DIP, strategyId, vitamCode),
                Status.OK, MediaType.APPLICATION_OCTET_STREAM_TYPE);
        } catch (final StorageNotFoundException exc) {
            LOGGER.error(exc);
            vitamCode = VitamCode.STORAGE_NOT_FOUND;
        } catch (final StorageException exc) {
            LOGGER.error(exc);
            vitamCode = VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR;
        }
        return buildErrorResponse(vitamCode);
    }

    /**
     * Post a new object
     *
     * @param httpServletRequest      http servlet request to get requester
     * @param headers                 http header
     * @param profileFileName         the id of the object
     * @param createObjectDescription the object description
     * @return Response
     */
    // header (X-Requester)
    @Path("/profiles/{profile_file_name}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createProfileOrGetInformation(@Context HttpServletRequest httpServletRequest,
        @Context HttpHeaders headers,
        @PathParam("profile_file_name") String profileFileName, ObjectDescription createObjectDescription) {
        // If the POST is a creation request
        if (createObjectDescription != null) {
            return createObjectByType(headers, profileFileName, createObjectDescription, DataCategory.PROFILE,
                httpServletRequest.getRemoteAddr());
        } else {
            return getObjectInformationWithPost(headers, profileFileName);
        }
    }

    /**
     * Get a report
     *
     * @param headers         http header
     * @param profileFileName the id of the object
     * @return the stream
     * @throws IOException throws an IO Exception
     */
    @Path("/profiles/{profile_file_name}")
    @GET
    @Produces({MediaType.APPLICATION_OCTET_STREAM, CommonMediaType.ZIP})
    public Response downloadProfile(@Context HttpHeaders headers,
        @PathParam("profile_file_name") String profileFileName)
        throws IOException {
        VitamCode vitamCode = checkTenantStrategyHeaderAsync(headers);
        if (vitamCode != null) {
            return buildErrorResponse(vitamCode);
        }
        String strategyId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.STRATEGY_ID).get(0);
        try {
            return new VitamAsyncInputStreamResponse(
                getByCategory(profileFileName, DataCategory.PROFILE, strategyId, vitamCode),
                Status.OK, MediaType.APPLICATION_OCTET_STREAM_TYPE);
        } catch (final StorageNotFoundException exc) {
            LOGGER.error(exc);
            vitamCode = VitamCode.STORAGE_NOT_FOUND;
        } catch (final StorageException exc) {
            LOGGER.error(exc);
            vitamCode = VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR;
        }
        return buildErrorResponse(vitamCode);
    }

    /**
     * @param headers http headers
     * @return null if strategy and tenant headers have values, a VitamCode response otherwise
     */
    private VitamCode checkTenantStrategyHeaderAsync(HttpHeaders headers) {
        if (!HttpHeaderHelper.hasValuesFor(headers, VitamHttpHeader.TENANT_ID) ||
            !HttpHeaderHelper.hasValuesFor(headers, VitamHttpHeader.STRATEGY_ID)) {
            return VitamCode.STORAGE_MISSING_HEADER;
        }
        return null;
    }

    private VitamError getErrorEntity(Status status, String message) {
        String aMessage =
            (message != null && !message.trim().isEmpty()) ? message
                : (status.getReasonPhrase() != null ? status.getReasonPhrase() : status.name());

        return new VitamError(status.name()).setHttpCode(status.getStatusCode()).setContext(STORAGE_MODULE)
            .setState(CODE_VITAM)
            .setMessage(status.getReasonPhrase()).setDescription(aMessage);
    }

    @Override
    public void close() {
        storageLogService.close();
        distribution.close();
    }
}
