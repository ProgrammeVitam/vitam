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
package fr.gouv.vitam.storage.engine.server.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.accesslog.AccessLogInfoModel;
import fr.gouv.vitam.common.accesslog.AccessLogUtils;
import fr.gouv.vitam.common.alert.AlertServiceImpl;
import fr.gouv.vitam.common.client.CustomVitamHttpStatusCode;
import fr.gouv.vitam.common.collection.CloseableIterator;
import fr.gouv.vitam.common.error.DomainName;
import fr.gouv.vitam.common.error.ServiceName;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseError;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.VitamAutoCloseable;
import fr.gouv.vitam.common.model.storage.AccessRequestStatus;
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
import fr.gouv.vitam.logbook.common.exception.TraceabilityException;
import fr.gouv.vitam.storage.driver.model.StorageLogBackupResult;
import fr.gouv.vitam.storage.driver.model.StorageLogTraceabilityResult;
import fr.gouv.vitam.storage.engine.common.exception.StorageAlreadyExistsException;
import fr.gouv.vitam.storage.engine.common.exception.StorageDriverNotFoundException;
import fr.gouv.vitam.storage.engine.common.exception.StorageException;
import fr.gouv.vitam.storage.engine.common.exception.StorageIllegalOperationException;
import fr.gouv.vitam.storage.engine.common.exception.StorageInconsistentStateException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.exception.StorageTechnicalException;
import fr.gouv.vitam.storage.engine.common.exception.StorageUnavailableDataFromAsyncOfferException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.request.BulkObjectStoreRequest;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.storage.engine.common.model.request.OfferLogRequest;
import fr.gouv.vitam.storage.engine.common.model.response.BatchObjectInformationResponse;
import fr.gouv.vitam.storage.engine.common.model.response.BulkObjectAvailabilityResponse;
import fr.gouv.vitam.storage.engine.common.model.response.BulkObjectStoreResponse;
import fr.gouv.vitam.storage.engine.common.model.response.StoredInfoResult;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageStrategy;
import fr.gouv.vitam.storage.engine.server.distribution.StorageDistribution;
import fr.gouv.vitam.storage.engine.server.distribution.impl.DataContext;
import fr.gouv.vitam.storage.engine.server.distribution.impl.StorageDistributionFactory;
import fr.gouv.vitam.storage.engine.server.distribution.impl.StorageDistributionImpl;
import fr.gouv.vitam.storage.engine.server.distribution.impl.StreamAndInfo;
import fr.gouv.vitam.storage.engine.server.rest.writeprotection.WriteProtection;
import fr.gouv.vitam.storage.engine.server.storagelog.StorageLog;
import fr.gouv.vitam.storage.engine.server.storagelog.StorageLogAdministration;
import fr.gouv.vitam.storage.engine.server.storagelog.StorageLogException;
import fr.gouv.vitam.storage.engine.server.storagelog.StorageLogFactory;
import fr.gouv.vitam.storage.engine.server.storagetraceability.StorageTraceabilityAdministration;
import fr.gouv.vitam.storage.engine.server.storagetraceability.TraceabilityStorageService;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.output.CloseShieldOutputStream;
import org.apache.commons.lang.BooleanUtils;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static fr.gouv.vitam.storage.engine.common.model.DataCategory.ARCHIVAL_TRANSFER_REPLY;
import static fr.gouv.vitam.storage.engine.server.distribution.impl.StorageDistributionImpl.NORMAL_ORIGIN;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;

@Path("/storage/v1")
@Tag(name = "Storage")
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
            storageLogService = StorageLogFactory.getInstance(configuration);

            // Wrap storage distribution service by a ReadOnlyShieldStorageDistribution wrapper to enforce ReadOnly checks
            distribution = StorageDistributionFactory.createStorageDistribution(
                configuration,
                storageLogService,
                new AlertServiceImpl()
            );

            WorkspaceClientFactory.changeMode(configuration.getUrlWorkspace());
            storageLogAdministration = new StorageLogAdministration(storageLogService, distribution, configuration);

            traceabilityLogbookService = new TraceabilityStorageService(distribution);

            TimeStampSignature timeStampSignature;
            try {
                final File file = PropertiesUtils.findFile(configuration.getP12LogbookFile());
                timeStampSignature = new TimeStampSignatureWithKeystore(
                    file,
                    configuration.getP12LogbookPassword().toCharArray()
                );
            } catch (
                KeyStoreException
                | CertificateException
                | IOException
                | UnrecoverableKeyException
                | NoSuchAlgorithmException e
            ) {
                LOGGER.error("unable to instantiate TimeStampGenerator", e);
                throw new RuntimeException(e);
            }

            timestampGenerator = new TimestampGenerator(timeStampSignature);
            // TODO Must have conf for logOpeClient ?
            traceabilityLogbookAdministration = new StorageTraceabilityAdministration(
                traceabilityLogbookService,
                distribution,
                configuration.getZippingDirecorty(),
                timestampGenerator,
                configuration.getStorageTraceabilityOverlapDelay(),
                configuration.getStorageLogTraceabilityThreadPoolSize()
            );
            LOGGER.info("init Storage Resource server");
        } catch (IOException e) {
            LOGGER.error("Cannot initialize storage resource server, error when reading configuration file");
            // FIXME: erf, not cool here
            throw new RuntimeException(e);
        } catch (StorageTechnicalException e) {
            LOGGER.error("A problem occured when checking strategy offers in Storage");
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
    @Produces(MediaType.APPLICATION_JSON)
    @WriteProtection(true)
    public Response copy(
        @Context HttpServletRequest httpServletRequest,
        @Context HttpHeaders headers,
        @PathParam("id_object") String objectId
    ) {
        VitamCode vitamCode = checkTenantAndHeaders(
            headers,
            VitamHttpHeader.TENANT_ID,
            VitamHttpHeader.STRATEGY_ID,
            VitamHttpHeader.X_CONTENT_SOURCE,
            VitamHttpHeader.X_CONTENT_DESTINATION,
            VitamHttpHeader.X_DATA_CATEGORY
        );
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
        } catch (final StorageUnavailableDataFromAsyncOfferException exc) {
            LOGGER.error(exc);
            return buildCustomErrorResponse(
                CustomVitamHttpStatusCode.UNAVAILABLE_DATA_FROM_ASYNC_OFFER,
                exc.getMessage()
            );
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
    @WriteProtection(true)
    public Response create(
        @Context HttpServletRequest httpServletRequest,
        @Context HttpHeaders headers,
        @PathParam("id_operation") String operationId,
        InputStream inputStream
    ) {
        String remoteAddress = httpServletRequest.getRemoteAddr();

        VitamCode vitamCode = checkTenantAndHeaders(
            headers,
            VitamHttpHeader.TENANT_ID,
            VitamHttpHeader.X_CONTENT_LENGTH,
            VitamHttpHeader.X_DATA_CATEGORY,
            VitamHttpHeader.STRATEGY_ID,
            VitamHttpHeader.OFFERS_IDS
        );
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

            StoredInfoResult storedInfoResult = distribution.storeDataInOffers(
                strategyId,
                NORMAL_ORIGIN,
                streamAndInfo,
                operationId,
                category,
                remoteAddress,
                offerIds
            );
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
    @WriteProtection(false)
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
     * Get list of object type
     *
     * @param headers X-Strategy-Id header
     * @param type the object type to list
     * @return a response with listing elements
     */
    @Path(
        "/{type:UNIT|OBJECT|OBJECTGROUP|LOGBOOK|REPORT|MANIFEST|PROFILE|STORAGELOG|STORAGEACCESSLOG|STORAGETRACEABILITY|RULES|DIP|AGENCIES|BACKUP" +
        "|BACKUP_OPERATION|CHECKLOGBOOKREPORTS|OBJECTGROUP_GRAPH|UNIT_GRAPH|DISTRIBUTIONREPORTS|ACCESSION_REGISTER_DETAIL|ACCESSION_REGISTER_SYMBOLIC}"
    )
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @WriteProtection(false)
    public Response listObjects(@Context HttpHeaders headers, @PathParam("type") DataCategory type) {
        VitamCode vitamCode = checkTenantAndHeaders(headers, VitamHttpHeader.STRATEGY_ID);
        if (vitamCode != null) {
            return buildErrorResponse(vitamCode);
        }
        String strategyId;
        try {
            strategyId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.STRATEGY_ID).get(0);
            List<String> offerIdHeaders = Optional.of(
                HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.OFFER)
            ).orElse(Collections.emptyList());
            CloseableIterator<ObjectEntry> objectEntryIterator;
            if (offerIdHeaders.isEmpty()) {
                objectEntryIterator = distribution.listContainerObjects(strategyId, type);
            } else {
                objectEntryIterator = distribution.listContainerObjectsForOffer(type, offerIdHeaders.get(0), false);
            }

            StreamingOutput streamingOutput = output -> {
                try (
                    CloseShieldOutputStream closeShieldOutputStream = CloseShieldOutputStream.wrap(output);
                    ObjectEntryWriter objectEntryWriter = new ObjectEntryWriter(closeShieldOutputStream)
                ) {
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

            return Response.ok(streamingOutput).build();
        } catch (StorageDriverNotFoundException exc) {
            LOGGER.error(exc);
            return buildErrorResponse(VitamCode.STORAGE_CONTAINER_NOT_FOUND);
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
        "|BACKUP_OPERATION|CHECKLOGBOOKREPORTS|OBJECTGROUP_GRAPH|UNIT_GRAPH|DISTRIBUTIONREPORTS|ACCESSION_REGISTER_DETAIL|ACCESSION_REGISTER_SYMBOLIC}/logs"
    )
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @WriteProtection(false)
    public Response getOfferLogs(
        @Context HttpHeaders headers,
        @PathParam("type") DataCategory type,
        OfferLogRequest offerLogRequest
    ) {
        VitamCode vitamCode = checkTenantAndHeaders(headers, VitamHttpHeader.STRATEGY_ID);
        if (vitamCode != null) {
            return buildErrorResponse(vitamCode);
        }
        try {
            String strategyId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.STRATEGY_ID).get(0);
            List<String> offerIdHeaders = Optional.of(
                HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.OFFER)
            ).orElse(Collections.emptyList());
            RequestResponse<OfferLog> jsonNodeRequestResponse;
            if (CollectionUtils.isNotEmpty(offerIdHeaders)) {
                jsonNodeRequestResponse = distribution.getOfferLogsByOfferId(
                    strategyId,
                    offerIdHeaders.get(0),
                    type,
                    offerLogRequest.getOffset(),
                    offerLogRequest.getLimit(),
                    offerLogRequest.getOrder()
                );
            } else {
                jsonNodeRequestResponse = distribution.getOfferLogs(
                    strategyId,
                    type,
                    offerLogRequest.getOffset(),
                    offerLogRequest.getLimit(),
                    offerLogRequest.getOrder()
                );
            }
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
    @WriteProtection(false)
    public Response getInformation(
        @Context HttpHeaders headers,
        @PathParam("type") String typeStr,
        @PathParam("id_object") String objectId
    ) {
        VitamCode vitamCode = checkTenantAndHeaders(
            headers,
            VitamHttpHeader.STRATEGY_ID,
            VitamHttpHeader.OFFERS_IDS,
            VitamHttpHeader.OFFER_NO_CACHE
        );
        if (vitamCode != null) {
            return buildErrorResponse(vitamCode);
        }

        DataCategory type = DataCategory.getByCollectionName(typeStr);

        String strategyId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.STRATEGY_ID).get(0);
        String listOffer = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.OFFERS_IDS).get(0);
        List<String> offerIds = Arrays.asList(listOffer.split(","));
        boolean noCache = Boolean.parseBoolean(
            HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.OFFER_NO_CACHE).get(0)
        );

        try {
            JsonNode offerMetadataInfo = distribution.getContainerInformation(
                strategyId,
                type,
                objectId,
                offerIds,
                noCache
            );
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
    @WriteProtection(false)
    public Response getBatchObjectInformation(
        @Context HttpHeaders headers,
        @PathParam("type") String typeStr,
        List<String> objectIds
    ) {
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
        return Response.status(Status.OK)
            .entity(new RequestResponseOK<BatchObjectInformationResponse>().addAllResults(objectInformationResponses))
            .build();
    }

    /**
     * Get an object data
     *
     * @param headers http header
     * @param objectId the id of the object
     * @return the stream
     */
    @Path("/objects/{id_object}")
    @GET
    @Produces({ MediaType.APPLICATION_OCTET_STREAM, CommonMediaType.ZIP })
    @WriteProtection(false)
    public Response getObject(
        @Context HttpHeaders headers,
        @PathParam("id_object") String objectId,
        AccessLogInfoModel logInfo
    ) {
        return getObject(headers, objectId, DataCategory.OBJECT, logInfo);
    }

    private Response getObject(
        HttpHeaders headers,
        String objectId,
        DataCategory dataCategory,
        AccessLogInfoModel accessLogInfoModel
    ) {
        VitamCode vitamCode = checkTenantAndHeaders(headers, VitamHttpHeader.STRATEGY_ID);
        if (vitamCode != null) {
            return buildErrorResponse(vitamCode);
        }
        String strategyId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.STRATEGY_ID).get(0);
        List<String> offerIdHeaders = Optional.of(
            HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.OFFER)
        ).orElse(Collections.emptyList());
        try {
            if (CollectionUtils.isEmpty(offerIdHeaders)) {
                return new VitamAsyncInputStreamResponse(
                    getByCategory(objectId, dataCategory, strategyId, vitamCode, accessLogInfoModel),
                    Status.OK,
                    MediaType.APPLICATION_OCTET_STREAM_TYPE
                );
            } else {
                return new VitamAsyncInputStreamResponse(
                    getByCategory(objectId, dataCategory, strategyId, vitamCode, offerIdHeaders.get(0)),
                    Status.OK,
                    MediaType.APPLICATION_OCTET_STREAM_TYPE
                );
            }
        } catch (final StorageNotFoundException exc) {
            LOGGER.error(exc);
            vitamCode = VitamCode.STORAGE_NOT_FOUND;
        } catch (final StorageUnavailableDataFromAsyncOfferException exc) {
            LOGGER.error(exc);
            return buildCustomErrorResponse(
                CustomVitamHttpStatusCode.UNAVAILABLE_DATA_FROM_ASYNC_OFFER,
                exc.getMessage()
            );
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
     */
    @Path("/backup/{backupfile}")
    @GET
    @Produces({ MediaType.APPLICATION_OCTET_STREAM })
    @WriteProtection(false)
    public Response getBackupFile(@Context HttpHeaders headers, @PathParam("backupfile") String backupfile) {
        VitamCode vitamCode = checkTenantAndHeaders(headers, VitamHttpHeader.STRATEGY_ID);
        if (vitamCode != null) {
            return buildErrorResponse(vitamCode);
        }
        String strategyId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.STRATEGY_ID).get(0);

        try {
            Response response;
            if (HttpHeaderHelper.hasValuesFor(headers, VitamHttpHeader.OFFER)) {
                String offerId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.OFFER).get(0);
                response = getByCategory(backupfile, DataCategory.BACKUP, strategyId, vitamCode, offerId);
            } else {
                response = getByCategory(
                    backupfile,
                    DataCategory.BACKUP,
                    strategyId,
                    vitamCode,
                    AccessLogUtils.getNoLogAccessLog()
                );
            }
            return new VitamAsyncInputStreamResponse(response, Status.OK, MediaType.APPLICATION_OCTET_STREAM_TYPE);
        } catch (final StorageNotFoundException exc) {
            LOGGER.error(exc);
            vitamCode = VitamCode.STORAGE_NOT_FOUND;
        } catch (final StorageUnavailableDataFromAsyncOfferException exc) {
            LOGGER.error(exc);
            return buildCustomErrorResponse(
                CustomVitamHttpStatusCode.UNAVAILABLE_DATA_FROM_ASYNC_OFFER,
                exc.getMessage()
            );
        } catch (final StorageException exc) {
            LOGGER.error(exc);
            vitamCode = VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR;
        }
        return buildErrorResponse(vitamCode);
    }

    private Response getByCategory(
        String objectId,
        DataCategory category,
        String strategyId,
        VitamCode vitamCode,
        AccessLogInfoModel logInformation
    ) throws StorageException {
        if (vitamCode == null) {
            return distribution.getContainerByCategory(
                strategyId,
                StorageDistributionImpl.NORMAL_ORIGIN,
                objectId,
                category,
                logInformation
            );
        }
        return buildErrorResponse(vitamCode);
    }

    private Response getByCategory(
        String objectId,
        DataCategory category,
        String strategyId,
        VitamCode vitamCode,
        String offerId
    ) throws StorageException {
        if (vitamCode == null) {
            return distribution.getContainerByCategory(
                strategyId,
                StorageDistributionImpl.NORMAL_ORIGIN,
                objectId,
                category,
                offerId
            );
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
    @WriteProtection(true)
    public Response createObject(
        @Context HttpServletRequest httpServletRequest,
        @Context HttpHeaders headers,
        @PathParam("id_object") String objectId,
        ObjectDescription createObjectDescription
    ) {
        return createObjectByType(
            headers,
            objectId,
            createObjectDescription,
            DataCategory.OBJECT,
            httpServletRequest.getRemoteAddr()
        );
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
    @WriteProtection(true)
    public Response createOrUpdateBackupOperation(
        @Context HttpServletRequest httpServletRequest,
        @Context HttpHeaders headers,
        @PathParam("id_operation") String operationId,
        ObjectDescription createObjectDescription
    ) {
        return createObjectByType(
            headers,
            operationId,
            createObjectDescription,
            DataCategory.BACKUP_OPERATION,
            httpServletRequest.getRemoteAddr()
        );
    }

    /**
     * Get access log data.
     *
     * @param headers headers
     * @param storageAccessLogFile backupfile
     * @return the file as stream
     */
    @Path("/storageaccesslog/{storageaccesslogfile}")
    @GET
    @Produces({ MediaType.APPLICATION_OCTET_STREAM })
    @WriteProtection(false)
    public Response getAccessLogFile(
        @Context HttpHeaders headers,
        @PathParam("storageaccesslogfile") String storageAccessLogFile
    ) {
        return getObject(
            headers,
            storageAccessLogFile,
            DataCategory.STORAGEACCESSLOG,
            AccessLogUtils.getNoLogAccessLog()
        );
    }

    /**
     * Get access log data.
     *
     * @param headers headers
     * @param storageAccessLogFile backupfile
     * @return the file as stream
     */
    @Path("/storagelog/{storagelogfile}")
    @GET
    @Produces({ MediaType.APPLICATION_OCTET_STREAM })
    @WriteProtection(false)
    public Response getStorageLogFile(
        @Context HttpHeaders headers,
        @PathParam("storagelogfile") String storageAccessLogFile
    ) {
        return getObject(headers, storageAccessLogFile, DataCategory.STORAGELOG, AccessLogUtils.getNoLogAccessLog());
    }

    /**
     * @param strategyId the strategy to get offers
     * @return
     */
    @Path("/offers")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @WriteProtection(false)
    public Response getOffers(@HeaderParam(GlobalDataRest.X_STRATEGY_ID) String strategyId) {
        try {
            List<String> offerIds = distribution.getOfferIds(strategyId);

            return Response.status(Status.OK).entity(JsonHandler.toJsonNode(offerIds)).build();
        } catch (InvalidParseOperationException | StorageException e) {
            return buildErrorResponse(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR);
        }
    }

    /**
     * Get referent Offer in strategy
     *
     * @param strategyId the strategy to get offers
     * @return
     */
    @Path("/referentOffer")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @WriteProtection(false)
    public Response getReferentOffer(@HeaderParam(GlobalDataRest.X_STRATEGY_ID) String strategyId) {
        try {
            RequestResponse<String> entity = new RequestResponseOK<String>().addResult(
                distribution.getReferentOffer(strategyId)
            );
            return Response.status(Status.OK).entity(entity).build();
        } catch (StorageException e) {
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
    @Produces({ MediaType.APPLICATION_OCTET_STREAM, CommonMediaType.ZIP })
    @WriteProtection(false)
    public Response getBackupOperation(@Context HttpHeaders headers, @PathParam("id_operation") String operationId) {
        return getObject(headers, operationId, DataCategory.BACKUP_OPERATION, AccessLogUtils.getNoLogAccessLog());
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
    @WriteProtection(true)
    public Response deleteObject(
        @Context HttpServletRequest httpServletRequest,
        @Context HttpHeaders headers,
        @PathParam("id_object") String objectId
    ) {
        VitamCode vitamCode = checkTenantAndHeaders(
            headers,
            VitamHttpHeader.TENANT_ID,
            VitamHttpHeader.STRATEGY_ID,
            VitamHttpHeader.X_DATA_CATEGORY
        );
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
            return Response.status(UNAUTHORIZED)
                .entity(getErrorEntity(UNAUTHORIZED, UNAUTHORIZED.getReasonPhrase()))
                .build();
        }

        String strategyId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.STRATEGY_ID).get(0);

        try {
            DataContext context = new DataContext(
                objectId,
                category,
                httpServletRequest.getRemoteHost(),
                ParameterHelper.getTenantParameter(),
                strategyId
            );

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
        "|BACKUP_OPERATION|CHECKLOGBOOKREPORTS|OBJECTGROUP_GRAPH|UNIT_GRAPH|DISTRIBUTIONREPORTS|ACCESSION_REGISTER_DETAIL|ACCESSION_REGISTER_SYMBOLIC}/{id_object}"
    )
    @HEAD
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @WriteProtection(false)
    public Response checkObject(
        @Context HttpHeaders headers,
        @PathParam("type") DataCategory type,
        @PathParam("id_object") String objectId
    ) {
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
            resultByOffer
                .entrySet()
                .forEach(entry -> {
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
     */
    @Path("/logbooks/{id_logbook}")
    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @WriteProtection(false)
    public Response getLogbookStream(@Context HttpHeaders headers, @PathParam("id_logbook") String objectId) {
        return getObject(headers, objectId, DataCategory.LOGBOOK, AccessLogUtils.getNoLogAccessLog());
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
    @WriteProtection(true)
    public Response createLogbook(
        @Context HttpServletRequest httpServletRequest,
        @Context HttpHeaders headers,
        @PathParam("id_logbook") String logbookId,
        ObjectDescription createObjectDescription
    ) {
        // TODO P1: actually no X-Requester header, so send the getRemoteAdr
        // from HttpServletRequest
        return createObjectByType(
            headers,
            logbookId,
            createObjectDescription,
            DataCategory.LOGBOOK,
            httpServletRequest.getRemoteAddr()
        );
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
    @Produces({ MediaType.APPLICATION_OCTET_STREAM, CommonMediaType.ZIP })
    @WriteProtection(false)
    public Response getUnit(@Context HttpHeaders headers, @PathParam("id_md") String unitId) {
        return getObject(headers, unitId, DataCategory.UNIT, AccessLogUtils.getNoLogAccessLog());
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
    @WriteProtection(true)
    public Response createUnitMetadata(
        @Context HttpServletRequest httpServletRequest,
        @Context HttpHeaders headers,
        @PathParam("id_md") String metadataId,
        ObjectDescription createObjectDescription
    ) {
        return createObjectByType(
            headers,
            metadataId,
            createObjectDescription,
            DataCategory.UNIT,
            httpServletRequest.getRemoteAddr()
        );
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
    @Produces({ MediaType.APPLICATION_OCTET_STREAM, CommonMediaType.ZIP })
    @WriteProtection(false)
    public Response getObjectGroup(@Context HttpHeaders headers, @PathParam("id_md") String metadataId) {
        return getObject(headers, metadataId, DataCategory.OBJECTGROUP, AccessLogUtils.getNoLogAccessLog());
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
    @WriteProtection(true)
    public Response createObjectGroup(
        @Context HttpServletRequest httpServletRequest,
        @Context HttpHeaders headers,
        @PathParam("id_md") String metadataId,
        ObjectDescription createObjectDescription
    ) {
        // TODO P1: actually no X-Requester header, so send the getRemoteAdr
        // from HttpServletRequest
        return createObjectByType(
            headers,
            metadataId,
            createObjectDescription,
            DataCategory.OBJECTGROUP,
            httpServletRequest.getRemoteAddr()
        );
    }

    @POST
    @Path("/archivaltransferreply/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @WriteProtection(true)
    public Response storeArchivalTransferReply(
        @Context HttpServletRequest httpServletRequest,
        @Context HttpHeaders headers,
        @PathParam("id") String id,
        ObjectDescription description
    ) {
        return createObjectByType(
            headers,
            id,
            description,
            ARCHIVAL_TRANSFER_REPLY,
            httpServletRequest.getRemoteAddr()
        );
    }

    @POST
    @Path("/tmp/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @WriteProtection(true)
    public Response storeTemporaryFile(
        @Context HttpServletRequest httpServletRequest,
        @Context HttpHeaders headers,
        @PathParam("id") String id,
        ObjectDescription description
    ) {
        return createObjectByType(headers, id, description, DataCategory.TMP, httpServletRequest.getRemoteAddr());
    }

    @Path("/tmp/{file_name}")
    @GET
    @Produces({ MediaType.APPLICATION_OCTET_STREAM })
    @WriteProtection(false)
    public Response getTemporaryFile(@Context HttpHeaders headers, @PathParam("file_name") String file_name) {
        return getObject(headers, file_name, DataCategory.TMP, AccessLogUtils.getNoLogAccessLog());
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
    @WriteProtection(true)
    public Response createReport(
        @Context HttpServletRequest httpServletRequest,
        @Context HttpHeaders headers,
        @PathParam("id_report") String reportId,
        ObjectDescription createObjectDescription
    ) {
        return createObjectByType(
            headers,
            reportId,
            createObjectDescription,
            DataCategory.REPORT,
            httpServletRequest.getRemoteAddr()
        );
    }

    /**
     * Get a report
     *
     * @param headers http header
     * @param objectId the id of the object
     * @return the stream
     */
    @Path("/reports/{id_report}")
    @GET
    @Produces({ MediaType.APPLICATION_OCTET_STREAM, CommonMediaType.ZIP })
    @WriteProtection(false)
    public Response getReport(@Context HttpHeaders headers, @PathParam("id_report") String objectId) {
        return getObject(headers, objectId, DataCategory.REPORT, AccessLogUtils.getNoLogAccessLog());
    }

    /**
     * Get a report
     *
     * @param headers http header
     * @param objectId the id of the object
     * @return the stream
     */
    @Path("/distributionreports/{id_report}")
    @GET
    @Produces({ MediaType.APPLICATION_OCTET_STREAM, CommonMediaType.ZIP })
    @WriteProtection(false)
    public Response getDistributionReport(@Context HttpHeaders headers, @PathParam("id_report") String objectId) {
        return getObject(headers, objectId, DataCategory.DISTRIBUTIONREPORTS, AccessLogUtils.getNoLogAccessLog());
    }

    // TODO P1: requester have to come from vitam headers (X-Requester), but
    // does not exist actually, so use
    // getRemoteAdr from HttpServletRequest passed as parameter (requester)
    // Change it when the good header is sent
    private Response createObjectByType(
        HttpHeaders headers,
        String objectId,
        ObjectDescription createObjectDescription,
        DataCategory category,
        String requester
    ) {
        VitamCode vitamCode = checkTenantAndHeaders(headers, VitamHttpHeader.STRATEGY_ID);
        if (vitamCode != null) {
            return buildErrorResponse(vitamCode);
        }
        if (createObjectDescription == null) {
            LOGGER.error("Missing body. Cannot create object " + category + "/" + objectId);
            return Response.status(Status.PRECONDITION_FAILED).build();
        }
        final String strategyId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.STRATEGY_ID).get(0);
        try {
            final StoredInfoResult result = distribution.storeDataInAllOffers(
                strategyId,
                objectId,
                createObjectDescription,
                category,
                requester
            );
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
    @WriteProtection(true)
    public Response createManifest(
        @Context HttpServletRequest httpServletRequest,
        @Context HttpHeaders headers,
        @PathParam("id_manifest") String manifestId,
        ObjectDescription createObjectDescription
    ) {
        return createObjectByType(
            headers,
            manifestId,
            createObjectDescription,
            DataCategory.MANIFEST,
            httpServletRequest.getRemoteAddr()
        );
    }

    /**
     * getManifest stored by ingest operation
     *
     * @param headers
     * @param objectId
     * @return the stream
     */
    @Path("/manifests/{id_manifest}")
    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @WriteProtection(false)
    public Response getManifest(@Context HttpHeaders headers, @PathParam("id_manifest") String objectId) {
        return getObject(headers, objectId, DataCategory.MANIFEST, AccessLogUtils.getNoLogAccessLog());
    }

    /**
     * Backup access log
     *
     * @param tenants tenant list to backup
     * @return the response with a specific HTTP status
     */
    @POST
    @Path("/storage/backup/accesslog")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @WriteProtection(true)
    public Response backupStorageAccessLog(List<Integer> tenants) {
        VitamCode vitamCode = checkMultiTenantRequest(tenants);
        if (vitamCode != null) {
            return buildErrorResponse(vitamCode);
        }

        try {
            List<StorageLogBackupResult> backupResultList = storageLogAdministration.backupStorageAccessLog(
                VitamConfiguration.getDefaultStrategy(),
                tenants
            );

            return Response.status(Status.OK)
                .entity(new RequestResponseOK<StorageLogBackupResult>().addAllResults(backupResultList))
                .build();
        } catch (StorageLogException e) {
            LOGGER.error("Unable to generate backup log", e);
            return buildErrorResponse(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR);
        }
    }

    /**
     * Backup storage log
     *
     * @param tenants tenant list to backup
     * @return the response with a specific HTTP status
     */
    @POST
    @Path("/storage/backup")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @WriteProtection(true)
    public Response backupStorageLog(List<Integer> tenants) {
        VitamCode vitamCode = checkMultiTenantRequest(tenants);
        if (vitamCode != null) {
            return buildErrorResponse(vitamCode);
        }

        try {
            List<StorageLogBackupResult> backupResultList = storageLogAdministration.backupStorageWriteLog(
                VitamConfiguration.getDefaultStrategy(),
                tenants
            );

            return Response.status(Status.OK)
                .entity(new RequestResponseOK<StorageLogBackupResult>().addAllResults(backupResultList))
                .build();
        } catch (StorageLogException e) {
            LOGGER.error("Unable to generate backup log", e);
            return buildErrorResponse(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR);
        }
    }

    /**
     * Run storage logbook secure operation
     *
     * @param tenants tenant list to secure
     * @return the response with a specific HTTP status
     */
    @POST
    @Path("/storage/traceability")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @WriteProtection(true)
    public Response traceabilityStorageLogbook(List<Integer> tenants) {
        VitamCode vitamCode = checkMultiTenantRequest(tenants);
        if (vitamCode != null) {
            return buildErrorResponse(vitamCode);
        }

        try {
            List<StorageLogTraceabilityResult> resultList =
                traceabilityLogbookAdministration.generateStorageLogTraceabilityOperations(
                    VitamConfiguration.getDefaultStrategy(),
                    tenants
                );

            return Response.status(Status.OK)
                .entity(new RequestResponseOK<StorageLogTraceabilityResult>().addAllResults(resultList))
                .build();
        } catch (TraceabilityException e) {
            LOGGER.error("unable to generate secure  log", e);
            return buildErrorResponse(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR);
        }
    }

    private VitamCode checkMultiTenantRequest(List<Integer> tenants) {
        Integer tenantId = VitamThreadUtils.getVitamSession().getTenantId();
        if (VitamThreadUtils.getVitamSession().getTenantId() == null) {
            LOGGER.error("Missing tenantId");
            return VitamCode.STORAGE_MISSING_HEADER;
        }

        if (!tenantId.equals(VitamConfiguration.getAdminTenant())) {
            LOGGER.error("Expecting Admin tenant " + VitamConfiguration.getAdminTenant() + " found " + tenantId);
            return VitamCode.STORAGE_INVALID_ADMIN_TENANT;
        }

        if (CollectionUtils.isEmpty(tenants)) {
            LOGGER.error("Expecting non empty list of tenants");
            return VitamCode.STORAGE_INVALID_TENANT_LIST;
        }

        if (tenants.contains(null)) {
            LOGGER.error("Null tenant");
            return VitamCode.STORAGE_INVALID_TENANT_LIST;
        }

        if (new HashSet<>(tenants).size() != tenants.size()) {
            LOGGER.error("Duplicate tenants");
            return VitamCode.STORAGE_INVALID_TENANT_LIST;
        }
        return null;
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
    @WriteProtection(true)
    public Response createStorageLog(
        @Context HttpServletRequest httpServletRequest,
        @Context HttpHeaders headers,
        @PathParam("storagelogname") String storageLogname,
        ObjectDescription createObjectDescription
    ) {
        return createObjectByType(
            headers,
            storageLogname,
            createObjectDescription,
            DataCategory.STORAGELOG,
            httpServletRequest.getRemoteAddr()
        );
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
    @WriteProtection(true)
    public Response createStorageAccessLog(
        @Context HttpServletRequest httpServletRequest,
        @Context HttpHeaders headers,
        @PathParam("storageaccesslogname") String storageAccessLogName,
        ObjectDescription createObjectDescription
    ) {
        return createObjectByType(
            headers,
            storageAccessLogName,
            createObjectDescription,
            DataCategory.STORAGEACCESSLOG,
            httpServletRequest.getRemoteAddr()
        );
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
    @WriteProtection(true)
    public Response createStorageTraceability(
        @Context HttpServletRequest httpServletRequest,
        @Context HttpHeaders headers,
        @PathParam("storagetraceabilityname") String storagetraceabilityname,
        ObjectDescription createObjectDescription
    ) {
        return createObjectByType(
            headers,
            storagetraceabilityname,
            createObjectDescription,
            DataCategory.STORAGETRACEABILITY,
            httpServletRequest.getRemoteAddr()
        );
    }

    /**
     * Get a storage traceability file
     *
     * @param headers http header
     * @param filename the id of the object
     * @return the stream
     */
    @Path("/storagetraceability/{storagetraceability_name}")
    @GET
    @Produces({ MediaType.APPLICATION_OCTET_STREAM, CommonMediaType.ZIP })
    @WriteProtection(false)
    public Response downloadStorageTraceability(
        @Context HttpHeaders headers,
        @PathParam("storagetraceability_name") String filename
    ) {
        return getObject(headers, filename, DataCategory.STORAGETRACEABILITY, AccessLogUtils.getNoLogAccessLog());
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
    @WriteProtection(true)
    public Response createBackupFile(
        @Context HttpServletRequest httpServletRequest,
        @Context HttpHeaders headers,
        @PathParam("backupfile") String backupfile,
        ObjectDescription createObjectDescription
    ) {
        return createObjectByType(
            headers,
            backupfile,
            createObjectDescription,
            DataCategory.BACKUP,
            httpServletRequest.getRemoteAddr()
        );
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
    @WriteProtection(true)
    public Response createRuleFile(
        @Context HttpServletRequest httpServletRequest,
        @Context HttpHeaders headers,
        @PathParam("rulefile") String ruleFile,
        ObjectDescription createObjectDescription
    ) {
        return createObjectByType(
            headers,
            ruleFile,
            createObjectDescription,
            DataCategory.RULES,
            httpServletRequest.getRemoteAddr()
        );
    }

    @Path("/rules/{id_object}")
    @GET
    @Produces({ MediaType.APPLICATION_OCTET_STREAM })
    @WriteProtection(false)
    public Response getRuleFile(@Context HttpHeaders headers, @PathParam("id_object") String objectId) {
        return getObject(headers, objectId, DataCategory.RULES, AccessLogUtils.getNoLogAccessLog());
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
    @WriteProtection(true)
    public Response createUnitGraphFile(
        @Context HttpServletRequest httpServletRequest,
        @Context HttpHeaders headers,
        @PathParam("graph_file_name") String graph_file_name,
        ObjectDescription createObjectDescription
    ) {
        return createObjectByType(
            headers,
            graph_file_name,
            createObjectDescription,
            DataCategory.UNIT_GRAPH,
            httpServletRequest.getRemoteAddr()
        );
    }

    /**
     * Get graph zip file
     *
     * @param headers
     * @param graph_file_name
     * @return
     */
    @Path("/unitgraph/{graph_file_name}")
    @GET
    @Produces({ MediaType.APPLICATION_OCTET_STREAM })
    @WriteProtection(false)
    public Response getUnitGraphFile(
        @Context HttpHeaders headers,
        @PathParam("graph_file_name") String graph_file_name
    ) {
        return getObject(headers, graph_file_name, DataCategory.UNIT_GRAPH, AccessLogUtils.getNoLogAccessLog());
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
    @WriteProtection(true)
    public Response createObjectGroupGraphFile(
        @Context HttpServletRequest httpServletRequest,
        @Context HttpHeaders headers,
        @PathParam("graph_file_name") String graph_file_name,
        ObjectDescription createObjectDescription
    ) {
        return createObjectByType(
            headers,
            graph_file_name,
            createObjectDescription,
            DataCategory.OBJECTGROUP_GRAPH,
            httpServletRequest.getRemoteAddr()
        );
    }

    /**
     * Get graph zip file
     *
     * @param headers
     * @param graph_file_name
     * @return
     */
    @Path("/objectgroupgraph/{graph_file_name}")
    @GET
    @Produces({ MediaType.APPLICATION_OCTET_STREAM })
    @WriteProtection(false)
    public Response getObjectGroupGraphFile(
        @Context HttpHeaders headers,
        @PathParam("graph_file_name") String graph_file_name
    ) {
        return getObject(headers, graph_file_name, DataCategory.OBJECTGROUP_GRAPH, AccessLogUtils.getNoLogAccessLog());
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
    @WriteProtection(true)
    public Response createAgencyFile(
        @Context HttpServletRequest httpServletRequest,
        @Context HttpHeaders headers,
        @PathParam("agencyfile") String agencyfile,
        ObjectDescription createObjectDescription
    ) {
        return createObjectByType(
            headers,
            agencyfile,
            createObjectDescription,
            DataCategory.AGENCIES,
            httpServletRequest.getRemoteAddr()
        );
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
    @WriteProtection(true)
    public Response createProfile(
        @Context HttpServletRequest httpServletRequest,
        @Context HttpHeaders headers,
        @PathParam("profile_file_name") String profileFileName,
        ObjectDescription createObjectDescription
    ) {
        return createObjectByType(
            headers,
            profileFileName,
            createObjectDescription,
            DataCategory.PROFILE,
            httpServletRequest.getRemoteAddr()
        );
    }

    /**
     * Get a profile
     *
     * @param headers http header
     * @param profileFileName the id of the object
     * @return the stream
     */
    @Path("/profiles/{profile_file_name}")
    @GET
    @Produces({ MediaType.APPLICATION_OCTET_STREAM, CommonMediaType.ZIP })
    @WriteProtection(false)
    public Response downloadProfile(
        @Context HttpHeaders headers,
        @PathParam("profile_file_name") String profileFileName
    ) {
        return getObject(headers, profileFileName, DataCategory.PROFILE, AccessLogUtils.getNoLogAccessLog());
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
    @WriteProtection(true)
    public Response createDistributionReportFile(
        @Context HttpServletRequest httpServletRequest,
        @Context HttpHeaders headers,
        @PathParam("distributionreportfile") String distributionreportfile,
        ObjectDescription createObjectDescription
    ) {
        return createObjectByType(
            headers,
            distributionreportfile,
            createObjectDescription,
            DataCategory.DISTRIBUTIONREPORTS,
            httpServletRequest.getRemoteAddr()
        );
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
    @WriteProtection(true)
    public Response createAccessionRegisterDetail(
        @Context HttpServletRequest httpServletRequest,
        @Context HttpHeaders headers,
        @PathParam("fileName") String fileName,
        ObjectDescription createObjectDescription
    ) {
        return createObjectByType(
            headers,
            fileName,
            createObjectDescription,
            DataCategory.ACCESSION_REGISTER_DETAIL,
            httpServletRequest.getRemoteAddr()
        );
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
    @Produces({ MediaType.APPLICATION_OCTET_STREAM, CommonMediaType.ZIP })
    @WriteProtection(false)
    public Response getAccessionRegisterDetail(@Context HttpHeaders headers, @PathParam("fileName") String fileName) {
        return getObject(headers, fileName, DataCategory.ACCESSION_REGISTER_DETAIL, AccessLogUtils.getNoLogAccessLog());
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
    @WriteProtection(true)
    public Response createAccessionRegisterSymbolic(
        @Context HttpServletRequest httpServletRequest,
        @Context HttpHeaders headers,
        @PathParam("fileName") String fileName,
        ObjectDescription createObjectDescription
    ) {
        return createObjectByType(
            headers,
            fileName,
            createObjectDescription,
            DataCategory.ACCESSION_REGISTER_SYMBOLIC,
            httpServletRequest.getRemoteAddr()
        );
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
    @Produces({ MediaType.APPLICATION_OCTET_STREAM, CommonMediaType.ZIP })
    @WriteProtection(false)
    public Response getAccessionRegisterSymbolic(@Context HttpHeaders headers, @PathParam("fileName") String fileName) {
        return getObject(
            headers,
            fileName,
            DataCategory.ACCESSION_REGISTER_SYMBOLIC,
            AccessLogUtils.getNoLogAccessLog()
        );
    }

    @Path("/bulk/{folder}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @WriteProtection(true)
    public Response bulkCreateFromWorkspace(
        @Context HttpServletRequest httpServletRequest,
        @Context HttpHeaders headers,
        @PathParam("folder") String folder,
        BulkObjectStoreRequest bulkObjectStoreRequest
    ) {
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
            ParametersChecker.checkParameter("Object names are mandatory", bulkObjectStoreRequest.getObjectNames());
            ParametersChecker.checkParameter(
                "Object names are mandatory",
                bulkObjectStoreRequest.getObjectNames().toArray()
            );
            ParametersChecker.checkParameter(
                "Workspace container is mandatory",
                bulkObjectStoreRequest.getWorkspaceContainerGUID()
            );
            ParametersChecker.checkParameter(
                "Workspace object URIs are mandatory",
                bulkObjectStoreRequest.getWorkspaceObjectURIs()
            );
            ParametersChecker.checkParameter(
                "Workspace object URIs are mandatory",
                bulkObjectStoreRequest.getWorkspaceObjectURIs().toArray()
            );
            if (bulkObjectStoreRequest.getObjectNames().isEmpty()) {
                throw new IllegalArgumentException("Empty object ids set");
            }
            if (
                bulkObjectStoreRequest.getObjectNames().size() != bulkObjectStoreRequest.getWorkspaceObjectURIs().size()
            ) {
                throw new IllegalArgumentException("Object ids must match workspace URIs");
            }
            if (!folder.equals(bulkObjectStoreRequest.getType().getCollectionName())) {
                throw new IllegalArgumentException("Folder do not match collection name");
            }

            BulkObjectStoreResponse result = distribution.bulkCreateFromWorkspace(
                strategyId,
                bulkObjectStoreRequest,
                requester
            );
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
    @WriteProtection(false)
    public Response getStrategies() {
        try {
            Map<String, StorageStrategy> strategies = distribution.getStrategies();
            RequestResponse<StorageStrategy> entity = new RequestResponseOK<StorageStrategy>().addAllResults(
                new ArrayList<>(strategies.values())
            );
            return Response.status(Status.OK).entity(entity).build();
        } catch (final StorageException exc) {
            LOGGER.error(exc);
            return buildErrorResponse(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR);
        }
    }

    /**
     * Create access request if target offer does not support synchronous read (tape library storage). If target offer
     * supports synchronous read requests, then no access request is created.
     *
     * HEADER X-Tenant-Id (mandatory) : tenant's identifier
     * HEADER X-Strategy-Id (mandatory) : storage strategy
     * HEADER X-Offer (optional) : offer id. If not specified, referent offer of the strategy is used.
     *
     * @param objectsNames objects for which access is requested
     * @param headers http header
     * @return HTTP 20O-OK with accessRequestId is access request created, HTTP 204-NO_CONTENT if no access request required
     */
    @POST
    @Path("/access-request/{dataCategory}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @WriteProtection(false)
    public Response createAccessRequestIfRequired(
        @PathParam("dataCategory") DataCategory dataCategory,
        List<String> objectsNames,
        @Context HttpHeaders headers
    ) {
        try {
            VitamCode vitamCode = checkTenantAndHeaders(
                headers,
                VitamHttpHeader.TENANT_ID,
                VitamHttpHeader.STRATEGY_ID
            );
            if (vitamCode != null) {
                return buildErrorResponse(vitamCode);
            }
            try {
                ParametersChecker.checkParameter("Data category is required", dataCategory);
                ParametersChecker.checkParameter("ObjectsNames are required", objectsNames);
                ParametersChecker.checkParameter("ObjectsNames are required", objectsNames.toArray(String[]::new));
            } catch (IllegalArgumentException e) {
                LOGGER.error(e);
                return buildErrorResponse(VitamCode.STORAGE_MISSING_HEADER);
            }

            if (objectsNames.isEmpty()) {
                LOGGER.error("Empty objectsNames");
                return buildErrorResponse(VitamCode.STORAGE_BAD_REQUEST);
            }

            String strategyId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.STRATEGY_ID).get(0);
            String offerId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.OFFER)
                .stream()
                .findFirst()
                .orElse(null);

            Optional<String> accessRequestId = distribution.createAccessRequestIfRequired(
                strategyId,
                offerId,
                dataCategory,
                objectsNames
            );

            if (accessRequestId.isEmpty()) {
                LOGGER.debug("No access request required. Offer is not Async");
                return Response.noContent().build();
            }
            LOGGER.info("Access request created " + accessRequestId.get());
            return new RequestResponseOK<String>()
                .addResult(accessRequestId.get())
                .setHttpCode(Status.OK.getStatusCode())
                .toResponse();
        } catch (Exception e) {
            LOGGER.error(e);
            return buildErrorResponse(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR);
        }
    }

    /**
     * Check access request statuses of asynchronous offer.
     *
     * HEADER X-Tenant-Id (mandatory) : tenant's identifier
     * HEADER X-Strategy-Id (mandatory) : storage strategy
     * HEADER X-Offer (optional) : offer id. If not specified, referent offer of the strategy is used.
     * HEADER X-Admin-Cross-Tenant-Access-Request-Allowed (mandatory) : when {@code true}, removal of access requests of other tenants is allowed from Admin tenant
     *
     * @param accessRequestIds accessRequestIds whose status is to be checked
     * @param headers http header
     * @return HTTP 20O-OK with the AccessRequestStatus for each access request id
     */
    @GET
    @Path("/access-request/statuses")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @WriteProtection(false)
    public Response checkAccessRequestStatuses(List<String> accessRequestIds, @Context HttpHeaders headers) {
        try {
            VitamCode vitamCode = checkTenantAndHeaders(
                headers,
                VitamHttpHeader.TENANT_ID,
                VitamHttpHeader.STRATEGY_ID,
                VitamHttpHeader.X_ADMIN_CROSS_TENANT_ACCESS_REQUEST_ALLOWED
            );
            if (vitamCode != null) {
                return buildErrorResponse(vitamCode);
            }

            try {
                ParametersChecker.checkParameter("AccessRequestIds are required", accessRequestIds);
                ParametersChecker.checkParameter(
                    "AccessRequestIds are required",
                    accessRequestIds.toArray(String[]::new)
                );
            } catch (IllegalArgumentException e) {
                LOGGER.error(e);
                return buildErrorResponse(VitamCode.STORAGE_MISSING_HEADER);
            }
            if (accessRequestIds.isEmpty()) {
                LOGGER.error("Missing access request Ids");
                return buildErrorResponse(VitamCode.STORAGE_BAD_REQUEST);
            }

            String strategyId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.STRATEGY_ID).get(0);
            String offerId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.OFFER)
                .stream()
                .findFirst()
                .orElse(null);

            String adminCrossTenantAccessRequestAllowedStr = HttpHeaderHelper.getHeaderValues(
                headers,
                VitamHttpHeader.X_ADMIN_CROSS_TENANT_ACCESS_REQUEST_ALLOWED
            ).get(0);
            boolean adminCrossTenantAccessRequestAllowed = Boolean.parseBoolean(
                adminCrossTenantAccessRequestAllowedStr
            );

            Map<String, AccessRequestStatus> accessRequestStatuses = distribution.checkAccessRequestStatuses(
                strategyId,
                offerId,
                accessRequestIds,
                adminCrossTenantAccessRequestAllowed
            );

            LOGGER.debug("Access request statuses " + accessRequestStatuses);
            return new RequestResponseOK<Map<String, AccessRequestStatus>>()
                .addResult(accessRequestStatuses)
                .setHttpCode(Status.OK.getStatusCode())
                .toResponse();
        } catch (StorageIllegalOperationException e) {
            LOGGER.error(e);
            return buildErrorResponse(VitamCode.STORAGE_ILLEGAL_OPERATION);
        } catch (Exception e) {
            LOGGER.error(e);
            return buildErrorResponse(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR);
        }
    }

    /**
     * Remove access request from asynchronous offer.
     *
     * HEADER X-Tenant-Id (mandatory) : tenant's identifier
     * HEADER X-Strategy-Id (mandatory) : storage strategy
     * HEADER X-Offer (optional) : offer id. If not specified, referent offer of the strategy is used.
     * HEADER X-Admin-Cross-Tenant-Access-Request-Allowed (mandatory) : when {@code true}, access to access requests of other tenants is allowed from Admin tenant
     *
     * @param accessRequestId the accessRequestId to be removed
     * @param headers http header
     * @return HTTP 20O-OK with the AccessRequestStatus for each access request id
     */
    @DELETE
    @Path("/access-request/{accessRequestId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @WriteProtection(false)
    public Response removeAccessRequest(
        @PathParam("accessRequestId") String accessRequestId,
        @Context HttpHeaders headers
    ) {
        try {
            VitamCode vitamCode = checkTenantAndHeaders(
                headers,
                VitamHttpHeader.TENANT_ID,
                VitamHttpHeader.STRATEGY_ID,
                VitamHttpHeader.X_ADMIN_CROSS_TENANT_ACCESS_REQUEST_ALLOWED
            );
            if (vitamCode != null) {
                return buildErrorResponse(vitamCode);
            }

            String strategyId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.STRATEGY_ID).get(0);
            String offerId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.OFFER)
                .stream()
                .findFirst()
                .orElse(null);
            try {
                ParametersChecker.checkParameter("AccessRequestId is required", accessRequestId);
            } catch (IllegalArgumentException e) {
                LOGGER.error(e);
                return buildErrorResponse(VitamCode.STORAGE_MISSING_HEADER);
            }

            String adminCrossTenantAccessRequestAllowedStr = HttpHeaderHelper.getHeaderValues(
                headers,
                VitamHttpHeader.X_ADMIN_CROSS_TENANT_ACCESS_REQUEST_ALLOWED
            ).get(0);
            boolean adminCrossTenantAccessRequestAllowed = Boolean.parseBoolean(
                adminCrossTenantAccessRequestAllowedStr
            );

            distribution.removeAccessRequest(
                strategyId,
                offerId,
                accessRequestId,
                adminCrossTenantAccessRequestAllowed
            );

            LOGGER.info("Access request removed successfully " + accessRequestId);
            return Response.accepted().build();
        } catch (StorageIllegalOperationException e) {
            LOGGER.error(e);
            return buildErrorResponse(VitamCode.STORAGE_ILLEGAL_OPERATION);
        } catch (Exception e) {
            LOGGER.error(e);
            return buildErrorResponse(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR);
        }
    }

    /**
     * Bulk check of immediate object availability in offer
     * If target offer supports synchronous read requests, objects can be read immediately.
     * If target offer does not support synchronous read requests (tape library storage), request is forwarded to the offer for effective check.
     *
     * HEADER X-Tenant-Id (mandatory) : tenant's identifier
     * HEADER X-Strategy-Id (mandatory) : storage strategy
     * HEADER X-Offer (optional) : offer id. If not specified, referent offer of the strategy is used.
     *
     * @param objectsNames objects for which access is requested
     * @param headers http header
     * @return HTTP 20O-OK with availability status
     */
    @GET
    @Path("/object-availability-check/{dataCategory}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @WriteProtection(false)
    public Response checkObjectAvailability(
        @PathParam("dataCategory") DataCategory dataCategory,
        List<String> objectsNames,
        @Context HttpHeaders headers
    ) {
        try {
            VitamCode vitamCode = checkTenantAndHeaders(
                headers,
                VitamHttpHeader.TENANT_ID,
                VitamHttpHeader.STRATEGY_ID
            );
            if (vitamCode != null) {
                return buildErrorResponse(vitamCode);
            }
            try {
                ParametersChecker.checkParameter("Data category is required", dataCategory);
                ParametersChecker.checkParameter("ObjectsNames are required", objectsNames);
                ParametersChecker.checkParameter("ObjectsNames are required", objectsNames.toArray(String[]::new));
            } catch (IllegalArgumentException e) {
                LOGGER.error(e);
                return buildErrorResponse(VitamCode.STORAGE_MISSING_HEADER);
            }

            if (objectsNames.isEmpty()) {
                LOGGER.error("Empty objectsNames");
                return buildErrorResponse(VitamCode.STORAGE_BAD_REQUEST);
            }

            String strategyId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.STRATEGY_ID).get(0);
            String offerId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.OFFER)
                .stream()
                .findFirst()
                .orElse(null);

            boolean areObjectsAvailable = distribution.checkObjectAvailability(
                strategyId,
                offerId,
                dataCategory,
                objectsNames
            );

            LOGGER.debug(
                "Availability status for objects {} of type {} is {}",
                objectsNames,
                dataCategory,
                areObjectsAvailable
            );

            return new RequestResponseOK<BulkObjectAvailabilityResponse>()
                .addResult(new BulkObjectAvailabilityResponse(areObjectsAvailable))
                .setHttpCode(Status.OK.getStatusCode())
                .toResponse();
        } catch (Exception e) {
            LOGGER.error(e);
            return buildErrorResponse(VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR);
        }
    }

    @POST
    @Path("/compaction")
    @Consumes(MediaType.APPLICATION_JSON)
    @WriteProtection(false)
    public void launchOfferLogCompaction(String offerId, @Context HttpHeaders headers) throws StorageException {
        LOGGER.info("Starting offer compaction for offer " + offerId);
        Integer tenantId = VitamThreadUtils.getVitamSession().getTenantId();
        distribution.launchOfferLogCompaction(offerId, tenantId);
        LOGGER.info("End offer compaction for offer " + offerId);
    }

    private Response buildErrorResponse(VitamCode vitamCode) {
        return Response.status(vitamCode.getStatus())
            .entity(
                new RequestResponseError()
                    .setError(
                        new VitamError(VitamCodeHelper.getCode(vitamCode))
                            .setContext(vitamCode.getService().getName())
                            .setState(vitamCode.getDomain().getName())
                            .setMessage(vitamCode.getMessage())
                            .setDescription(vitamCode.getMessage())
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
        String aMessage = ((message != null) && !message.trim().isEmpty())
            ? message
            : ((status.getReasonPhrase() != null) ? status.getReasonPhrase() : status.name());

        return new VitamError(status.name())
            .setHttpCode(status.getStatusCode())
            .setContext(STORAGE_MODULE)
            .setState(CODE_VITAM)
            .setMessage(status.getReasonPhrase())
            .setDescription(aMessage);
    }

    @Override
    public void close() {
        storageLogService.close();
        distribution.close();
    }
}
