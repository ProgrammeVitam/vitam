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
package fr.gouv.vitam.storage.engine.server.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.accesslog.AccessLogInfoModel;
import fr.gouv.vitam.common.accesslog.AccessLogUtils;
import fr.gouv.vitam.common.collection.CloseableIterator;
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
import fr.gouv.vitam.common.model.storage.ObjectEntry;
import fr.gouv.vitam.common.model.storage.ObjectEntryWriter;
import fr.gouv.vitam.common.parameter.ParameterHelper;
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
import fr.gouv.vitam.storage.engine.common.exception.StorageInconsistentStateException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.request.BulkObjectStoreRequest;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.storage.engine.common.model.request.OfferLogRequest;
import fr.gouv.vitam.storage.engine.common.model.response.BatchObjectInformationResponse;
import fr.gouv.vitam.storage.engine.common.model.response.BulkObjectStoreResponse;
import fr.gouv.vitam.storage.engine.common.model.response.StoredInfoResult;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageStrategy;
import fr.gouv.vitam.storage.engine.server.distribution.StorageDistribution;
import fr.gouv.vitam.storage.engine.server.distribution.impl.DataContext;
import fr.gouv.vitam.storage.engine.server.distribution.impl.StorageDistributionImpl;
import fr.gouv.vitam.storage.engine.server.distribution.impl.StreamAndInfo;
import fr.gouv.vitam.storage.engine.server.storagelog.StorageLog;
import fr.gouv.vitam.storage.engine.server.storagelog.StorageLogAdministration;
import fr.gouv.vitam.storage.engine.server.storagelog.StorageLogException;
import fr.gouv.vitam.storage.engine.server.storagelog.StorageLogFactory;
import fr.gouv.vitam.storage.engine.server.storagetraceability.StorageTraceabilityAdministration;
import fr.gouv.vitam.storage.engine.server.storagetraceability.TraceabilityStorageService;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.apache.commons.io.output.CloseShieldOutputStream;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang.BooleanUtils;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static fr.gouv.vitam.storage.engine.common.model.DataCategory.ARCHIVAL_TRANSFER_REPLY;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;

@Path("/storage/v1")
@Tag(name="Storage")
public class StorageResource extends ApplicationStatusResource implements VitamAutoCloseable {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(StorageResource.class);
    private static final String STORAGE_MODULE = "STORAGE";
    private static final String CODE_VITAM = "code_vitam";

    private static final String ERROR_WHEN_COPING_CONTEXT = "Error when coping context: ";
    private final StorageDistribution distribution;
    private final TraceabilityStorageService traceabilityLogbookService;
    private final TimestampGenerator timestampGenerator;

    private StorageLog storageLogService;
    private StorageLogAdministration storageLogAdministration;
    private StorageTraceabilityAdministration traceabilityLogbookAdministration;

    /**
     * Constructor
     *
     * @param configuration
     */
    StorageResource(StorageConfiguration configuration) {
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
     * Constructor used for test purpose
     *
     * @param storageDistribution the storage Distribution to be applied
     */
    @VisibleForTesting
    StorageResource(StorageDistribution storageDistribution, TimestampGenerator timestampGenerator) {
        distribution = storageDistribution;
        traceabilityLogbookService = new TraceabilityStorageService(distribution);
        this.timestampGenerator = timestampGenerator;
    }

    @Path("/copy/{id_object}")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response copy(@Context HttpServletRequest httpServletRequest, @Context HttpHeaders headers,
        @PathParam("id_object") String objectId) {
        VitamCode vitamCode = checkTenantAndHeaders(headers, VitamHttpHeader.TENANT_ID, VitamHttpHeader.STRATEGY_ID,
            VitamHttpHeader.X_CONTENT_SOURCE, VitamHttpHeader.X_CONTENT_DESTINATION, VitamHttpHeader.X_DATA_CATEGORY);
        if (vitamCode != null) {
            return buildErrorResponse(vitamCode);
        }

        String remoteAddress = httpServletRequest.getRemoteAddr();
        int tenantId = VitamThreadUtils.getVitamSession().getTenantId();
        DataCategory category;
        String source = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.X_CONTENT_SOURCE).get(0);
        String destination = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.X_CONTENT_DESTINATION).get(0);
        String strategyId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.STRATEGY_ID).get(0);
        try {
            category = getDataCategory(headers);
        } catch (IllegalArgumentException e) {

            return buildErrorResponse(VitamCode.STORAGE_MISSING_HEADER);
        }


        DataContext context = new DataContext(objectId, category, remoteAddress, tenantId, strategyId);
        try {
            StoredInfoResult storedInfoResult = distribution.copyObjectFromOfferToOffer(context, source, destination);
            return Response.ok().entity(storedInfoResult).build();
        } catch (Exception e) {
            LOGGER.error(ERROR_WHEN_COPING_CONTEXT + context, e);

            return Response.serverError().build();
        }
    }

    /**
     * Post a new backup operation
     *
     * @param httpServletRequest http servlet request to get requester
     * @param headers http header
     * @param operationId the id of the operation
     * @param inputStream inputStream
     * @return Response
     */
    @Path("/create/{id_operation}")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(@Context HttpServletRequest httpServletRequest,
        @Context HttpHeaders headers,
        @PathParam("id_operation") String operationId, InputStream inputStream) {

        String remoteAddress = httpServletRequest.getRemoteAddr();

        VitamCode vitamCode = checkTenantAndHeaders(headers, VitamHttpHeader.TENANT_ID,
            VitamHttpHeader.X_CONTENT_LENGTH, VitamHttpHeader.X_DATA_CATEGORY, VitamHttpHeader.STRATEGY_ID,
            VitamHttpHeader.OFFERS_IDS);
        if (vitamCode != null) {
            return buildErrorResponse(vitamCode);
        }

        DataCategory category;
        Long size = Long.valueOf(HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.X_CONTENT_LENGTH).get(0));
        final String strategyId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.STRATEGY_ID).get(0);

        try {
            category = getDataCategory(headers);
        } catch (IllegalArgumentException e) {

            return buildErrorResponse(VitamCode.STORAGE_MISSING_HEADER);
        }

        try (StreamAndInfo streamAndInfo = new StreamAndInfo(inputStream, size)) {

            String listOffer = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.OFFERS_IDS).get(0);
            List<String> offerIds = Arrays.asList(listOffer.split(","));

            StoredInfoResult storedInfoResult = distribution.storeDataInOffers(strategyId, streamAndInfo, operationId,
                category, remoteAddress, offerIds);
            return Response.ok().entity(storedInfoResult).build();
        } catch (final StorageException e) {
            LOGGER.error(e);
            return buildErrorResponse(VitamCode.STORAGE_NOT_FOUND);
        }

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
        VitamCode vitamCode = checkTenantAndHeaders(headers, VitamHttpHeader.STRATEGY_ID);
        if (vitamCode != null) {
            return buildErrorResponse(vitamCode);
        }
        final String strategyId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.STRATEGY_ID).get(0);
        try {
            final JsonNode result = distribution.getContainerInformation(strategyId);
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

    /**
     * Search the header value for 'X-Http-Method-Override' and return an error response id it's value is not 'GET'
     *
     * @param headers the http headers to check
     * @return OK response if no header is found, NULL if header value is correct, BAD_REQUEST if the header contain an
     * other value than GET
     */
    private Response checkPostHeader(HttpHeaders headers) {
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
     * Get list of object type
     *
     * @param headers X-Strategy-Id header
     * @param type the object type to list
     * @return a response with listing elements
     */
    @Path(
        "/{type:UNIT|OBJECT|OBJECTGROUP|LOGBOOK|REPORT|MANIFEST|PROFILE|STORAGELOG|STORAGEACCESSLOG|STORAGETRACEABILITY|RULES|DIP|AGENCIES|BACKUP" +
            "|BACKUP_OPERATION|CHECKLOGBOOKREPORTS|OBJECTGROUP_GRAPH|UNIT_GRAPH|DISTRIBUTIONREPORTS|ACCESSION_REGISTER_DETAIL|ACCESSION_REGISTER_SYMBOLIC}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response listObjects(@Context HttpHeaders headers, @PathParam("type") DataCategory type) {
        VitamCode vitamCode = checkTenantAndHeaders(headers, VitamHttpHeader.STRATEGY_ID);
        if (vitamCode != null) {
            return buildErrorResponse(vitamCode);
        }
        String strategyId;
        try {
            strategyId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.STRATEGY_ID).get(0);
            CloseableIterator<ObjectEntry> objectEntryIterator = distribution.listContainerObjects(strategyId, type);

            StreamingOutput streamingOutput = output -> {
                try (
                    CloseShieldOutputStream closeShieldOutputStream = new CloseShieldOutputStream(output);
                    ObjectEntryWriter objectEntryWriter = new ObjectEntryWriter(closeShieldOutputStream)) {

                    while (objectEntryIterator.hasNext()) {
                        objectEntryWriter.write(objectEntryIterator.next());
                    }

                    // No errors ==> write EOF
                    objectEntryWriter.writeEof();

                } catch (Exception e) {
                    String msg = "Could not read object listing";
                    LOGGER.error(msg, e);
                    throw new WebApplicationException(msg, e);
                } finally {
                    objectEntryIterator.close();
                }
            };

            return Response
                .ok(streamingOutput)
                .build();
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
     * @param headers
     * @param type the object type to list
     * @param offerLogRequest offer log request params
     * @return list of offer log
     */
    @Path(
        "/{type:UNIT|OBJECT|OBJECTGROUP|LOGBOOK|REPORT|MANIFEST|PROFILE|STORAGELOG|STORAGETRACEABILITY|RULES|DIP|AGENCIES|BACKUP" +
            "|BACKUP_OPERATION|CHECKLOGBOOKREPORTS|OBJECTGROUP_GRAPH|UNIT_GRAPH|DISTRIBUTIONREPORTS|ACCESSION_REGISTER_DETAIL|ACCESSION_REGISTER_SYMBOLIC}/logs")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getOfferLogs(@Context HttpHeaders headers,
        @PathParam("type") DataCategory type, OfferLogRequest offerLogRequest) {
        VitamCode vitamCode = checkTenantAndHeaders(headers, VitamHttpHeader.STRATEGY_ID);
        if (vitamCode != null) {
            return buildErrorResponse(vitamCode);
        }
        try {
            String strategyId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.STRATEGY_ID).get(0);
            RequestResponse<OfferLog> jsonNodeRequestResponse = distribution.getOfferLogs(strategyId, type,
                offerLogRequest.getOffset(), offerLogRequest.getLimit(), offerLogRequest.getOrder());
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
     * @param headers http header
     * @param objectId the id of the object
     * @return Response NOT_IMPLEMENTED
     */
    @Path("/info/{type}/{id_object}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getInformation(@Context HttpHeaders headers,
        @PathParam("type") String typeStr, @PathParam("id_object") String objectId) {
        VitamCode vitamCode = checkTenantAndHeaders(headers, VitamHttpHeader.STRATEGY_ID, VitamHttpHeader.OFFERS_IDS,
            VitamHttpHeader.OFFER_NO_CACHE);
        if (vitamCode != null) {
            return buildErrorResponse(vitamCode);
        }

        DataCategory type = DataCategory.getByCollectionName(typeStr);

        String strategyId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.STRATEGY_ID).get(0);
        String listOffer = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.OFFERS_IDS).get(0);
        List<String> offerIds = Arrays.asList(listOffer.split(","));
        boolean noCache = Boolean
            .parseBoolean(HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.OFFER_NO_CACHE).get(0));

        try {
            JsonNode offerMetadataInfo = distribution.getContainerInformation(strategyId, type, objectId, offerIds,
                noCache);
            return Response.status(Status.OK).entity(offerMetadataInfo).build();
        } catch (StorageException e) {
            LOGGER.error(e);
            return buildErrorResponse(VitamCode.STORAGE_NOT_FOUND);
        }
    }

    /**
     * Get object metadata as json Note : this is NOT to be handled in item #72.
     *
     * @param headers http header
     * @param objectIds the id of the object
     */
    @Path("/batch_info/{type}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getBatchObjectInformation(@Context HttpHeaders headers, @PathParam("type") String typeStr,
        List<String> objectIds) {
        VitamCode vitamCode = checkTenantAndHeaders(headers, VitamHttpHeader.STRATEGY_ID, VitamHttpHeader.OFFERS_IDS);
        if (vitamCode != null) {
            return buildErrorResponse(vitamCode);
        }

        DataCategory type = DataCategory.getByCollectionName(typeStr);
        String strategyId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.STRATEGY_ID).get(0);
        String listOffer = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.OFFERS_IDS).get(0);
        List<String> offerIds = Arrays.asList(listOffer.split(","));

        List<BatchObjectInformationResponse> objectInformationResponses;
        try {
            objectInformationResponses = distribution.getBatchObjectInformation(strategyId, type, objectIds, offerIds);
        } catch (StorageException e) {
            LOGGER.error(e);
            return buildErrorResponse(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR);
        }
        return Response.status(Status.OK).entity(
            new RequestResponseOK<BatchObjectInformationResponse>().addAllResults(objectInformationResponses))
            .build();
    }

    /**
     * Get an object data
     *
     * @param headers http header
     * @param objectId the id of the object
     * @return the stream
     * @throws IOException throws an IO Exception
     */
    @Path("/objects/{id_object}")
    @GET
    @Produces({MediaType.APPLICATION_OCTET_STREAM, CommonMediaType.ZIP})
    public Response getObject(@Context HttpHeaders headers, @PathParam("id_object") String objectId,
        AccessLogInfoModel logInfo)
        throws IOException {
        VitamCode vitamCode = checkTenantAndHeaders(headers, VitamHttpHeader.STRATEGY_ID);
        if (vitamCode != null) {
            return buildErrorResponse(vitamCode);
        }
        String strategyId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.STRATEGY_ID).get(0);

        try {
            return new VitamAsyncInputStreamResponse(
                getByCategory(objectId, DataCategory.OBJECT, strategyId, vitamCode, logInfo),
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
     * @param headers headers
     * @param backupfile backupfile
     * @return
     * @throws IOException
     */
    @Path("/backup/{backupfile}")
    @GET
    @Produces({MediaType.APPLICATION_OCTET_STREAM})
    public Response getBackupFile(@Context HttpHeaders headers, @PathParam("backupfile") String backupfile)
        throws IOException {
        VitamCode vitamCode = checkTenantAndHeaders(headers, VitamHttpHeader.STRATEGY_ID);
        if (vitamCode != null) {
            return buildErrorResponse(vitamCode);
        }
        String strategyId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.STRATEGY_ID).get(0);

        try {
            return new VitamAsyncInputStreamResponse(
                getByCategory(backupfile, DataCategory.BACKUP, strategyId, vitamCode, null),
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
        String strategyId, VitamCode vitamCode, AccessLogInfoModel logInformation)
        throws StorageException {
        if (vitamCode == null) {
            return distribution.getContainerByCategory(strategyId, objectId, category, logInformation);
        }
        return buildErrorResponse(vitamCode);
    }

    /**
     * Post a new object
     *
     * @param httpServletRequest http servlet request to get requester
     * @param headers http header
     * @param objectId the id of the object
     * @param createObjectDescription the object description
     * @return Response
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
     * @param httpServletRequest http servlet request to get requester
     * @param headers http header
     * @param operationId the id of the operation
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
     * Get access log data.
     *
     * @param headers headers
     * @param storageAccessLogFile backupfile
     * @return the file as stream
     * @throws IOException
     */
    @Path("/storageaccesslog/{storageaccesslogfile}")
    @GET
    @Produces({MediaType.APPLICATION_OCTET_STREAM})
    public Response getAccessLogFile(@Context HttpHeaders headers,
        @PathParam("storageaccesslogfile") String storageAccessLogFile) {
        VitamCode vitamCode = checkTenantAndHeaders(headers, VitamHttpHeader.STRATEGY_ID);
        if (vitamCode != null) {
            return buildErrorResponse(vitamCode);
        }
        String strategyId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.STRATEGY_ID).get(0);

        try {
            return new VitamAsyncInputStreamResponse(
                getByCategory(storageAccessLogFile, DataCategory.STORAGEACCESSLOG, strategyId, vitamCode, null),
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
     * @param strategyId the strategy to get offers
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
     * @param headers http header
     * @param operationId the id of the operation
     * @return the stream
     */
    @Path("/backupoperations/{id_operation}")
    @GET
    @Produces({MediaType.APPLICATION_OCTET_STREAM, CommonMediaType.ZIP})
    public Response getBackupOperation(@Context HttpHeaders headers, @PathParam("id_operation") String operationId) {
        VitamCode vitamCode = checkTenantAndHeaders(headers, VitamHttpHeader.STRATEGY_ID);
        if (vitamCode != null) {
            return buildErrorResponse(vitamCode);
        }
        String strategyId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.STRATEGY_ID).get(0);
        try {
            return new VitamAsyncInputStreamResponse(
                getByCategory(operationId, DataCategory.BACKUP_OPERATION, strategyId, vitamCode, null),
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
        VitamCode vitamCode = checkTenantAndHeaders(headers, VitamHttpHeader.STRATEGY_ID);
        if (vitamCode != null) {
            return buildErrorResponse(vitamCode);
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
     * @param headers http header
     * @param objectId the id of the object
     * @return Response
     */
    @Path("/delete/{id_object}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteObject(@Context HttpServletRequest httpServletRequest, @Context HttpHeaders headers,
        @PathParam("id_object") String objectId) {

        VitamCode vitamCode = checkTenantAndHeaders(headers, VitamHttpHeader.TENANT_ID, VitamHttpHeader.STRATEGY_ID,
            VitamHttpHeader.X_DATA_CATEGORY);
        if (vitamCode != null) {
            return buildErrorResponse(vitamCode);
        }

        DataCategory category;
        try {
            category = getDataCategory(headers);
        } catch (IllegalArgumentException e) {

            return buildErrorResponse(VitamCode.STORAGE_MISSING_HEADER);
        }

        if (!category.canDelete()) {
            return Response.status(UNAUTHORIZED).entity(getErrorEntity(UNAUTHORIZED, UNAUTHORIZED.getReasonPhrase()))
                .build();
        }

        String strategyId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.STRATEGY_ID).get(0);

        try {
            DataContext context = new DataContext(objectId, category, httpServletRequest.getRemoteHost(),
                ParameterHelper.getTenantParameter(), strategyId);

            List<String> headerValues = headers.getRequestHeader(GlobalDataRest.X_OFFER_IDS);

            if (headerValues == null || headerValues.isEmpty()) {
                distribution.deleteObjectInAllOffers(strategyId, context);
            } else {
                distribution.deleteObjectInOffers(strategyId, context, Arrays.asList(headerValues.get(0).split(",")));
            }
            return Response.status(Status.NO_CONTENT).build();
        } catch (final StorageNotFoundException e) {
            LOGGER.error(e);
            return buildErrorResponse(VitamCode.STORAGE_NOT_FOUND);
        } catch (final StorageException exc) {
            LOGGER.error(exc);
            return buildErrorResponse(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR);
        } catch (final IllegalArgumentException e) {
            LOGGER.error(e);
            return buildErrorResponse(VitamCode.STORAGE_BAD_REQUEST);
        }

    }

    private DataCategory getDataCategory(@Context HttpHeaders headers) {
        DataCategory category;
        String dataCategoryString = headers.getHeaderString(GlobalDataRest.X_DATA_CATEGORY);

        if (dataCategoryString == null || dataCategoryString.isEmpty()) {
            throw new IllegalArgumentException("category connot be empty or null ");

        }
        category = DataCategory.valueOf(dataCategoryString);
        return category;
    }

    /**
     * Check the existence of an object
     *
     * @param headers http header
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
        VitamCode vitamCode = checkTenantAndHeaders(headers, VitamHttpHeader.STRATEGY_ID, VitamHttpHeader.OFFERS_IDS);
        if (vitamCode != null) {
            return buildErrorResponse(vitamCode);
        }

        String strategyId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.STRATEGY_ID).get(0);
        String listOffer = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.OFFERS_IDS).get(0);
        List<String> offerIds = Arrays.asList(listOffer.split(","));

        try {
            Map<String, Boolean> resultByOffer = distribution.checkObjectExisting(strategyId, objectId, type, offerIds);
            final ResponseBuilder responseBuilder;
            if (resultByOffer.containsValue(Boolean.FALSE)) {
                responseBuilder = Response.status(Status.NOT_FOUND);
            } else {
                responseBuilder = Response.status(Status.NO_CONTENT);
            }
            resultByOffer.entrySet().forEach(entry -> {
                responseBuilder.header(entry.getKey(), BooleanUtils.toStringTrueFalse(entry.getValue()));
            });
            return responseBuilder.build();

        } catch (final StorageException e) {
            LOGGER.error(e);
            return buildErrorResponse(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR);
        }

    }

    /**
     * @param headers http header
     * @param objectId the id of the object
     * @return the stream
     * @throws IOException exception
     */
    @Path("/logbooks/{id_logbook}")
    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getLogbookStream(@Context HttpHeaders headers, @PathParam("id_logbook") String objectId)
        throws IOException {
        VitamCode vitamCode = checkTenantAndHeaders(headers, VitamHttpHeader.STRATEGY_ID);
        if (vitamCode != null) {
            return buildErrorResponse(vitamCode);
        }
        String strategyId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.STRATEGY_ID).get(0);
        try {
            return new VitamAsyncInputStreamResponse(
                getByCategory(objectId, DataCategory.LOGBOOK, strategyId, vitamCode, null),
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
     * @param httpServletRequest http servlet request to get requester
     * @param headers http header
     * @param logbookId the id of the logbookId
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
        // TODO P1: actually no X-Requester header, so send the getRemoteAdr
        // from HttpServletRequest
        return createObjectByType(headers, logbookId, createObjectDescription, DataCategory.LOGBOOK,
            httpServletRequest.getRemoteAddr());
    }

    /**
     * Get a unit
     *
     * @param headers http header
     * @param unitId the id of the unit
     * @return the stream
     */
    @Path("/units/{id_md}")
    @GET
    @Produces({MediaType.APPLICATION_OCTET_STREAM, CommonMediaType.ZIP})
    public Response getUnit(@Context HttpHeaders headers, @PathParam("id_md") String unitId) {
        VitamCode vitamCode = checkTenantAndHeaders(headers, VitamHttpHeader.STRATEGY_ID);
        if (vitamCode != null) {
            return buildErrorResponse(vitamCode);
        }
        String strategyId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.STRATEGY_ID).get(0);
        try {
            return new VitamAsyncInputStreamResponse(
                getByCategory(unitId, DataCategory.UNIT, strategyId, vitamCode, null),
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
     * @param httpServletRequest http servlet request to get requester
     * @param headers http header
     * @param metadataId the id of the unit metadata
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
     * Get a Object Group
     * <p>
     * Note : this is NOT to be handled in item #72.
     *
     * @param headers http header
     * @param metadataId the id of the Object Group metadata
     * @return Response NOT_IMPLEMENTED
     */
    @Path("/objectgroups/{id_md}")
    @GET
    @Produces({MediaType.APPLICATION_OCTET_STREAM, CommonMediaType.ZIP})
    public Response getObjectGroup(@Context HttpHeaders headers, @PathParam("id_md") String metadataId) {
        VitamCode vitamCode = checkTenantAndHeaders(headers, VitamHttpHeader.STRATEGY_ID);
        if (vitamCode != null) {
            return buildErrorResponse(vitamCode);
        }
        String strategyId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.STRATEGY_ID).get(0);
        try {
            return new VitamAsyncInputStreamResponse(
                getByCategory(metadataId, DataCategory.OBJECTGROUP, strategyId, vitamCode, null),
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
     * @param httpServletRequest http servlet request to get requester
     * @param headers http header
     * @param metadataId the id of the Object Group metadata
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

    @POST
    @Path("/archivaltransferreply/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response storeArchivalTransferReply(@Context HttpServletRequest httpServletRequest,
        @Context HttpHeaders headers, @PathParam("id") String id, ObjectDescription description) {
        return createObjectByType(headers, id, description, ARCHIVAL_TRANSFER_REPLY,
            httpServletRequest.getRemoteAddr());
    }


    @POST
    @Path("/tmp/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response storeTemporaryFile(@Context HttpServletRequest httpServletRequest, @Context HttpHeaders headers,
        @PathParam("id") String id, ObjectDescription description) {
        return createObjectByType(headers, id, description, DataCategory.TMP, httpServletRequest.getRemoteAddr());
    }


    @Path("/tmp/{file_name}")
    @GET
    @Produces({MediaType.APPLICATION_OCTET_STREAM})
    public Response getTemporaryFile(@Context HttpHeaders headers,
        @PathParam("file_name") String file_name) {
        VitamCode vitamCode = checkTenantAndHeaders(headers, VitamHttpHeader.STRATEGY_ID);
        if (vitamCode != null) {
            return buildErrorResponse(vitamCode);
        }
        String strategyId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.STRATEGY_ID).get(0);

        try {
            return new VitamAsyncInputStreamResponse(
                getByCategory(file_name, DataCategory.TMP, strategyId, vitamCode,
                    AccessLogUtils.getNoLogAccessLog()),
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
     * @param httpServletRequest http servlet request to get requester
     * @param headers http header
     * @param reportId the id of the object
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
     * @param headers http header
     * @param objectId the id of the object
     * @return the stream
     * @throws IOException throws an IO Exception
     */
    @Path("/reports/{id_report}")
    @GET
    @Produces({MediaType.APPLICATION_OCTET_STREAM, CommonMediaType.ZIP})
    public Response getReport(@Context HttpHeaders headers, @PathParam("id_report") String objectId)
        throws IOException {
        VitamCode vitamCode = checkTenantAndHeaders(headers, VitamHttpHeader.STRATEGY_ID);
        if (vitamCode != null) {
            return buildErrorResponse(vitamCode);
        }
        String strategyId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.STRATEGY_ID).get(0);
        try {
            return new VitamAsyncInputStreamResponse(
                getByCategory(objectId, DataCategory.REPORT, strategyId, vitamCode, null),
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
     * Get a report
     *
     * @param headers http header
     * @param objectId the id of the object
     * @return the stream
     * @throws IOException throws an IO Exception
     */
    @Path("/distributionreports/{id_report}")
    @GET
    @Produces({MediaType.APPLICATION_OCTET_STREAM, CommonMediaType.ZIP})
    public Response getDistributionReport(@Context HttpHeaders headers, @PathParam("id_report") String objectId)
        throws IOException {
        VitamCode vitamCode = checkTenantAndHeaders(headers, VitamHttpHeader.STRATEGY_ID);
        if (vitamCode != null) {
            return buildErrorResponse(vitamCode);
        }
        String strategyId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.STRATEGY_ID).get(0);
        try {
            return new VitamAsyncInputStreamResponse(
                getByCategory(objectId, DataCategory.DISTRIBUTIONREPORTS, strategyId, vitamCode, null),
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
        VitamCode vitamCode = checkTenantAndHeaders(headers, VitamHttpHeader.STRATEGY_ID);
        if (vitamCode != null) {
            return buildErrorResponse(vitamCode);
        }
        final String strategyId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.STRATEGY_ID).get(0);
        try {
            final StoredInfoResult result = distribution.storeDataInAllOffers(strategyId, objectId,
                createObjectDescription, category, requester);
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

    /**
     * Post a new object manifest
     *
     * @param httpServletRequest http servlet request to get requester
     * @param headers http header
     * @param manifestId the id of the object
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
        VitamCode vitamCode = checkTenantAndHeaders(headers, VitamHttpHeader.STRATEGY_ID);
        if (vitamCode != null) {
            return buildErrorResponse(vitamCode);
        }
        String strategyId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.STRATEGY_ID).get(0);
        try {
            return new VitamAsyncInputStreamResponse(
                getByCategory(objectId, DataCategory.MANIFEST, strategyId, vitamCode,
                    AccessLogUtils.getNoLogAccessLog()),
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
     * Backup access log
     *
     * @param headers http header
     * @return the response with a specific HTTP status
     */
    @POST
    @Path("/storage/backup/accesslog")
    @Produces(MediaType.APPLICATION_JSON)
    public Response backupStorageAccessLog(@Context HttpHeaders headers) {
        VitamCode vitamCode = checkTenantAndHeaders(headers);
        if (vitamCode != null) {
            return buildErrorResponse(vitamCode);
        }

        try {
            Integer tenantId = VitamThreadUtils.getVitamSession().getTenantId();

            final GUID guid = GUIDFactory.newOperationLogbookGUID(tenantId);
            VitamThreadUtils.getVitamSession().setRequestId(guid);

            storageLogAdministration.backupStorageLog(VitamConfiguration.getDefaultStrategy(), false, guid);
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
     * Backup storage log
     *
     * @param headers http header
     * @return the response with a specific HTTP status
     */
    @POST
    @Path("/storage/backup")
    @Produces(MediaType.APPLICATION_JSON)
    public Response backupStorageLog(@Context HttpHeaders headers) {
        VitamCode vitamCode = checkTenantAndHeaders(headers);
        if (vitamCode != null) {
            return buildErrorResponse(vitamCode);
        }

        try {
            Integer tenantId = VitamThreadUtils.getVitamSession().getTenantId();

            final GUID guid = GUIDFactory.newOperationLogbookGUID(tenantId);
            VitamThreadUtils.getVitamSession().setRequestId(guid);

            storageLogAdministration.backupStorageLog(VitamConfiguration.getDefaultStrategy(), true, guid);
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
     * @param headers http header
     * @return the response with a specific HTTP status
     */
    @POST
    @Path("/storage/traceability")
    @Produces(MediaType.APPLICATION_JSON)
    public Response traceabilityStorageLogbook(@Context HttpHeaders headers) {
        VitamCode vitamCode = checkTenantAndHeaders(headers);
        if (vitamCode != null) {
            return buildErrorResponse(vitamCode);
        }

        try {
            Integer tenantId = VitamThreadUtils.getVitamSession().getTenantId();

            final GUID guid = GUIDFactory.newOperationLogbookGUID(tenantId);

            VitamThreadUtils.getVitamSession().setRequestId(guid);

            traceabilityLogbookAdministration
                .generateTraceabilityStorageLogbook(guid, VitamConfiguration.getDefaultStrategy());
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
     * @param httpServletRequest http servlet request to get requester
     * @param headers http header
     * @param storageLogname the id of the object
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
     * Post a new accesslog object
     *
     * @param httpServletRequest http servlet request to get requester
     * @param headers http header
     * @param createObjectDescription the object description
     * @return Response
     */
    // header (X-Requester)
    @Path("/storageaccesslog/{storageaccesslogname}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createStorageAccessLog(@Context HttpServletRequest httpServletRequest,
        @Context HttpHeaders headers,
        @PathParam("storageaccesslogname") String storageAccessLogName, ObjectDescription createObjectDescription) {
        // If the POST is a creation request
        if (createObjectDescription != null) {
            return createObjectByType(headers, storageAccessLogName, createObjectDescription,
                DataCategory.STORAGEACCESSLOG,
                httpServletRequest.getRemoteAddr());
        } else {
            return getObjectInformationWithPost(headers, storageAccessLogName);
        }
    }

    /**
     * Post a new object
     *
     * @param httpServletRequest http servlet request to get requester
     * @param headers http header
     * @param storagetraceabilityname storage traceability name
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
        @PathParam("storagetraceabilityname") String storagetraceabilityname,
        ObjectDescription createObjectDescription) {
        // If the POST is a creation request
        if (createObjectDescription != null) {
            return createObjectByType(headers, storagetraceabilityname, createObjectDescription,
                DataCategory.STORAGETRACEABILITY,
                httpServletRequest.getRemoteAddr());
        } else {
            return getObjectInformationWithPost(headers, storagetraceabilityname);
        }
    }

    /**
     * Get a storage traceability file
     *
     * @param headers http header
     * @param filename the id of the object
     * @return the stream
     * @throws IOException throws an IO Exception
     */
    @Path("/storagetraceability/{storagetraceability_name}")
    @GET
    @Produces({MediaType.APPLICATION_OCTET_STREAM, CommonMediaType.ZIP})
    public Response downloadStorageTraceability(@Context HttpHeaders headers,
        @PathParam("storagetraceability_name") String filename) {
        VitamCode vitamCode = checkTenantAndHeaders(headers, VitamHttpHeader.STRATEGY_ID);
        if (vitamCode != null) {
            return buildErrorResponse(vitamCode);
        }
        String strategyId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.STRATEGY_ID).get(0);
        try {
            return new VitamAsyncInputStreamResponse(
                getByCategory(filename, DataCategory.STORAGETRACEABILITY, strategyId, vitamCode, null),
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
     * @param httpServletRequest http servlet request to get requester
     * @param headers http header
     * @param backupfile the id of the object
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
     * @param httpServletRequest http servlet request to get requester
     * @param headers http header
     * @param ruleFile the id of the object
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
        VitamCode vitamCode = checkTenantAndHeaders(headers, VitamHttpHeader.STRATEGY_ID);
        if (vitamCode != null) {
            return buildErrorResponse(vitamCode);
        }
        String strategyId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.STRATEGY_ID).get(0);
        try {
            return new VitamAsyncInputStreamResponse(
                getByCategory(objectId, DataCategory.RULES, strategyId, vitamCode, AccessLogUtils.getNoLogAccessLog()),
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
     * @param httpServletRequest http servlet request to get requester
     * @param headers http header
     * @param graph_file_name the id of the object
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
        VitamCode vitamCode = checkTenantAndHeaders(headers, VitamHttpHeader.STRATEGY_ID);
        if (vitamCode != null) {
            return buildErrorResponse(vitamCode);
        }
        String strategyId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.STRATEGY_ID).get(0);

        try {
            return new VitamAsyncInputStreamResponse(
                getByCategory(graph_file_name, DataCategory.UNIT_GRAPH, strategyId, vitamCode,
                    AccessLogUtils.getNoLogAccessLog()),
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
     * @param httpServletRequest http servlet request to get requester
     * @param headers http header
     * @param graph_file_name the id of the object
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
        VitamCode vitamCode = checkTenantAndHeaders(headers, VitamHttpHeader.STRATEGY_ID);
        if (vitamCode != null) {
            return buildErrorResponse(vitamCode);
        }
        String strategyId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.STRATEGY_ID).get(0);

        try {
            return new VitamAsyncInputStreamResponse(
                getByCategory(graph_file_name, DataCategory.OBJECTGROUP_GRAPH, strategyId, vitamCode,
                    AccessLogUtils.getNoLogAccessLog()),
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
     * @param httpServletRequest http servlet request to get requester
     * @param headers http header
     * @param agencyfile the id of the object
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
     * @param httpServletRequest http servlet request to get requester
     * @param headers http header
     * @param profileFileName the id of the object
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
     * Get a profile
     *
     * @param headers http header
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
        VitamCode vitamCode = checkTenantAndHeaders(headers, VitamHttpHeader.STRATEGY_ID);
        if (vitamCode != null) {
            return buildErrorResponse(vitamCode);
        }
        String strategyId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.STRATEGY_ID).get(0);
        try {
            return new VitamAsyncInputStreamResponse(
                getByCategory(profileFileName, DataCategory.PROFILE, strategyId, vitamCode, null),
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
     * Post a new distribution report file
     *
     * @param httpServletRequest http servlet request to get requester
     * @param headers http header
     * @param distributionreportfile the id of the object
     * @param createObjectDescription the object description
     * @return Response
     */
    @Path("/distributionreports/{distributionreportfile}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createDistributionReportFile(@Context HttpServletRequest httpServletRequest,
        @Context HttpHeaders headers,
        @PathParam("distributionreportfile") String distributionreportfile, ObjectDescription createObjectDescription) {
        if (createObjectDescription != null) {
            return createObjectByType(headers, distributionreportfile, createObjectDescription,
                DataCategory.DISTRIBUTIONREPORTS,
                httpServletRequest.getRemoteAddr());
        } else {
            return getObjectInformationWithPost(headers, distributionreportfile);
        }
    }

    /**
     * Post a new unit metadata
     *
     * @param httpServletRequest http servlet request to get requester
     * @param headers http header
     * @param fileName the file name of the Accession Register Detail
     * @param createObjectDescription the workspace description of the unit to be created
     * @return Response containing result infos
     */
    // TODO P1: remove httpServletRequest when requester information sent by
    // header (X-Requester)
    @Path("/accessionregistersdetail/{fileName}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createAccessionRegisterDetail(@Context HttpServletRequest httpServletRequest,
        @Context HttpHeaders headers,
        @PathParam("fileName") String fileName, ObjectDescription createObjectDescription) {
        return createObjectByType(headers, fileName, createObjectDescription, DataCategory.ACCESSION_REGISTER_DETAIL,
            httpServletRequest.getRemoteAddr());
    }

    /**
     * Get an accessionregistersdetail
     *
     * @param headers http header
     * @param fileName the file name of the Accession Register Detail
     * @return the stream
     */
    @Path("/accessionregistersdetail/{fileName}")
    @GET
    @Produces({MediaType.APPLICATION_OCTET_STREAM, CommonMediaType.ZIP})
    public Response getAccessionRegisterDetail(@Context HttpHeaders headers, @PathParam("fileName") String fileName) {
        VitamCode vitamCode = checkTenantAndHeaders(headers, VitamHttpHeader.STRATEGY_ID);
        if (vitamCode != null) {
            return buildErrorResponse(vitamCode);
        }
        String strategyId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.STRATEGY_ID).get(0);
        try {
            return new VitamAsyncInputStreamResponse(
                getByCategory(fileName, DataCategory.ACCESSION_REGISTER_DETAIL, strategyId, vitamCode, null),
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
     * @param httpServletRequest http servlet request to get requester
     * @param headers http header
     * @param fileName the file name of the Accession Register Symbolic
     * @param createObjectDescription the workspace description of the unit to be created
     * @return Response containing result infos
     */
    // TODO P1: remove httpServletRequest when requester information sent by
    // header (X-Requester)
    @Path("/accessionregisterssymbolic/{fileName}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createAccessionRegisterSymbolic(@Context HttpServletRequest httpServletRequest,
        @Context HttpHeaders headers,
        @PathParam("fileName") String fileName, ObjectDescription createObjectDescription) {
        return createObjectByType(headers, fileName, createObjectDescription, DataCategory.ACCESSION_REGISTER_SYMBOLIC,
            httpServletRequest.getRemoteAddr());
    }

    /**
     * Get an accessionregisterssymbolic
     *
     * @param headers http header
     * @param fileName the file name of the Accession Register Symbolic
     * @return the stream
     */
    @Path("/accessionregisterssymbolic/{fileName}")
    @GET
    @Produces({MediaType.APPLICATION_OCTET_STREAM, CommonMediaType.ZIP})
    public Response getAccessionRegisterSymbolic(@Context HttpHeaders headers, @PathParam("fileName") String fileName) {
        VitamCode vitamCode = checkTenantAndHeaders(headers, VitamHttpHeader.STRATEGY_ID);
        if (vitamCode != null) {
            return buildErrorResponse(vitamCode);
        }
        String strategyId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.STRATEGY_ID).get(0);
        try {
            return new VitamAsyncInputStreamResponse(
                getByCategory(fileName, DataCategory.ACCESSION_REGISTER_SYMBOLIC, strategyId, vitamCode, null),
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

    @Path("/bulk/{folder}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response bulkCreateFromWorkspace(@Context HttpServletRequest httpServletRequest,
        @Context HttpHeaders headers,
        @PathParam("folder") String folder, BulkObjectStoreRequest bulkObjectStoreRequest) {

        try {
            // check headers
            VitamCode vitamCode = checkTenantAndHeaders(headers, VitamHttpHeader.STRATEGY_ID);
            if (vitamCode != null) {
                return buildErrorResponse(vitamCode);
            }

            String requester = httpServletRequest.getRemoteAddr();
            String strategyId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.STRATEGY_ID).get(0);

            // Basic checks
            ParametersChecker.checkParameter("Strategy id is mandatory", strategyId);
            ParametersChecker.checkParameter("Request is mandatory", bulkObjectStoreRequest);
            ParametersChecker.checkParameter("DataCategory is mandatory", bulkObjectStoreRequest.getType());
            ParametersChecker.checkParameter("Object names are mandatory",
                bulkObjectStoreRequest.getObjectNames());
            ParametersChecker.checkParameter("Object names are mandatory",
                bulkObjectStoreRequest.getObjectNames().toArray());
            ParametersChecker.checkParameter("Workspace container is mandatory",
                bulkObjectStoreRequest.getWorkspaceContainerGUID());
            ParametersChecker.checkParameter("Workspace object URIs are mandatory",
                bulkObjectStoreRequest.getWorkspaceObjectURIs());
            ParametersChecker.checkParameter("Workspace object URIs are mandatory",
                bulkObjectStoreRequest.getWorkspaceObjectURIs().toArray());
            if (bulkObjectStoreRequest.getObjectNames().isEmpty()) {
                throw new IllegalArgumentException("Empty object ids set");
            }
            if (bulkObjectStoreRequest.getObjectNames().size() !=
                bulkObjectStoreRequest.getWorkspaceObjectURIs().size()) {
                throw new IllegalArgumentException("Object ids must match workspace URIs");
            }
            if (!folder.equals(bulkObjectStoreRequest.getType().getCollectionName())) {
                throw new IllegalArgumentException("Folder do not match collection name");
            }

            BulkObjectStoreResponse result =
                distribution.bulkCreateFromWorkspace(strategyId, bulkObjectStoreRequest, requester);
            return Response.status(Status.CREATED).entity(result).build();
        } catch (final IllegalArgumentException e) {
            LOGGER.error(e);
            return buildErrorResponse(VitamCode.STORAGE_BAD_REQUEST);
        } catch (StorageInconsistentStateException exc) {
            LOGGER.error(exc);
            return buildErrorResponse(VitamCode.STORAGE_INCONSISTENT_STATE);
        } catch (final Exception exc) {
            LOGGER.error(exc);
            return buildErrorResponse(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR);
        }
    }

    /**
     * Get the strategies available in the module
     *
     * @return the strategies
     */
    @Path("/strategies")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStrategies() {
        try {
            Map<String, StorageStrategy> strategies = distribution.getStrategies();
            RequestResponse<StorageStrategy> entity = new RequestResponseOK<StorageStrategy>()
                .addAllResults(new ArrayList<>(strategies.values()));
            return Response.status(Status.OK).entity(entity).build();
        } catch (final StorageException exc) {
            LOGGER.error(exc);
            return buildErrorResponse(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR);
        }
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

    private VitamCode checkTenantAndHeaders(HttpHeaders headers, VitamHttpHeader... vitamHeaders) {
        if (VitamThreadUtils.getVitamSession().getTenantId() == null) {
            LOGGER.error("Missing tenantId");
            return VitamCode.STORAGE_MISSING_HEADER;
        }
        try {
            HttpHeaderHelper.checkVitamHeaders(headers);
        } catch (IllegalStateException e) {
            LOGGER.error("Header validation failed", e);
            return VitamCode.STORAGE_MISSING_HEADER;
        }
        for (VitamHttpHeader vitamHeader : vitamHeaders) {
            if (!HttpHeaderHelper.hasValuesFor(headers, vitamHeader)) {
                LOGGER.error("Required header missing " + vitamHeader.getName());
                return VitamCode.STORAGE_MISSING_HEADER;
            }
        }
        return null;
    }

    private VitamError getErrorEntity(Status status, String message) {
        String aMessage =
            ((message != null) && !message.trim().isEmpty()) ? message
                : ((status.getReasonPhrase() != null) ? status.getReasonPhrase() : status.name());

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
