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

import java.io.IOException;
import java.util.ArrayList;
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
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.compress.archivers.ArchiveException;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;

import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.VitamAutoCloseable;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.server.application.AsyncInputStreamHelper;
import fr.gouv.vitam.common.server.application.HttpHeaderHelper;
import fr.gouv.vitam.common.server.application.VitamHttpHeader;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.exception.TraceabilityException;
import fr.gouv.vitam.storage.engine.common.exception.StorageAlreadyExistsException;
import fr.gouv.vitam.storage.engine.common.exception.StorageException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.storage.engine.common.model.response.RequestResponseError;
import fr.gouv.vitam.storage.engine.common.model.response.StoredInfoResult;
import fr.gouv.vitam.storage.engine.server.distribution.StorageDistribution;
import fr.gouv.vitam.storage.engine.server.distribution.impl.StorageDistributionImpl;
import fr.gouv.vitam.storage.logbook.StorageLogException;
import fr.gouv.vitam.storage.logbook.StorageLogbookAdministration;
import fr.gouv.vitam.storage.logbook.StorageLogbookService;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

/**
 * Storage Resource implementation
 */
@Path("/storage/v1")
@javax.ws.rs.ApplicationPath("webresources")
public class StorageResource extends ApplicationStatusResource implements VitamAutoCloseable {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(StorageResource.class);
    private static final String STORAGE_MODULE = "STORAGE";
    private static final String CODE_VITAM = "code_vitam";
    private static final String MISSING_THE_TENANT_ID_X_TENANT_ID =
        "Missing the tenant ID (X-Tenant-Id) or wrong object Type";

    private final StorageDistribution distribution;


    private StorageLogbookService storageLogbookService;
    private StorageLogbookAdministration storageLogbookAdministration;

    /**
     * Constructor
     *
     * @param configuration the storage configuration to be applied
     * @param service the logbook service
     */
    public StorageResource(StorageConfiguration configuration, StorageLogbookService service) {
        this.storageLogbookService = service;
        distribution = new StorageDistributionImpl(configuration, storageLogbookService);
        WorkspaceClientFactory.changeMode(configuration.getUrlWorkspace());
        storageLogbookAdministration =
            new StorageLogbookAdministration(storageLogbookService, configuration.getZippingDirecorty());
        LOGGER.info("init Storage Resource server");
    }

    /**
     * Constructor used for test purpose
     *
     * @param storageDistribution the storage Distribution to be applied
     */
    StorageResource(StorageDistribution storageDistribution) {
        distribution = storageDistribution;
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
        return response;
    }

    /**
     * Search the header value for 'X-Http-Method-Override' and return an error response id it's value is not 'GET'
     *
     * @param headers the http headers to check
     * @return OK response if no header is found, NULL if header value is correct, BAD_REQUEST if the header contain an
     *         other value than GET
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
        return Response.status(status).entity(getErrorEntity(status)).build();
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
        return Response.status(status).entity(getErrorEntity(status)).build();
    }

    /**
     * Get list of object type
     *
     * @param xcursor the X-Cursor
     * @param xcursorId the X-Cursor-Id if exists
     * @param strategyId the strategy to get offers
     * @param type the object type to list
     * @return a response with listing elements
     */
    @Path("/{type}")
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
     * Get object metadata as json Note : this is NOT to be handled in item #72.
     *
     * @param headers http header
     * @param objectId the id of the object
     * @return Response NOT_IMPLEMENTED
     */
    @Path("/objects/{id_object}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getObjectInformation(@Context HttpHeaders headers, @PathParam("id_object") String objectId) {
        Response response = checkTenantStrategyHeader(headers);
        if (response != null) {
            return response;
        }
        final Status status = Status.NOT_IMPLEMENTED;
        return Response.status(status).entity(getErrorEntity(status)).build();
    }

    /**
     * Get an object data Note : this is NOT to be handled in item #72.
     *
     * @param headers http header
     * @param objectId the id of the object
     * @param asyncResponse async response
     * @throws IOException throws an IO Exception
     */
    @Path("/objects/{id_object}")
    @GET
    @Produces({MediaType.APPLICATION_OCTET_STREAM, CommonMediaType.ZIP})
    public void getObject(@Context HttpHeaders headers, @PathParam("id_object") String objectId,
        @Suspended final AsyncResponse asyncResponse) throws IOException {

        VitamThreadPoolExecutor.getDefaultExecutor().execute(new Runnable() {

            @Override
            public void run() {
                getByCategoryAsync(objectId, headers, DataCategory.OBJECT, asyncResponse);
            }
        });

    }

    private void getByCategoryAsync(String objectId, HttpHeaders headers, DataCategory category,
        AsyncResponse asyncResponse) {
        VitamCode vitamCode = checkTenantStrategyHeaderAsync(headers);
        if (vitamCode == null) {
            final String strategyId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.STRATEGY_ID).get(0);
            try {
                distribution.getContainerByCategory(strategyId, objectId, category, asyncResponse);
                return;
            } catch (final StorageNotFoundException exc) {
                LOGGER.error(exc);
                vitamCode = VitamCode.STORAGE_NOT_FOUND;
            } catch (final StorageException exc) {
                LOGGER.error(exc);
                vitamCode = VitamCode.STORAGE_TECHNICAL_INTERNAL_ERROR;
            }
        }
        if (vitamCode != null) {
            buildErrorResponseAsync(vitamCode, asyncResponse);
        }

    }

    /**
     * Post a new object
     *
     * @param httpServletRequest http servlet request to get requester
     * @param headers http header
     * @param objectId the id of the object
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

    private Response getObjectInformationWithPost(HttpHeaders headers, String objectId) {
        final Response response = checkTenantStrategyHeader(headers);
        if (response != null) {
            return response;
        }
        final Response responsePost = checkPostHeader(headers);
        if (responsePost == null) {
            return getObjectInformation(headers, objectId);
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
            return Response.status(Status.UNAUTHORIZED).entity(getErrorEntity(Status.UNAUTHORIZED)).build();
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
     * @param headers http header
     * @param objectId the id of the object
     * @return Response NOT_IMPLEMENTED
     */
    @Path("/objects/{id_object}")
    @HEAD
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response checkObject(@Context HttpHeaders headers, @PathParam("id_object") String objectId) {
        String strategyId;
        final Response response = checkTenantStrategyHeader(headers);
        if (response == null) {
            strategyId = HttpHeaderHelper.getHeaderValues(headers, VitamHttpHeader.STRATEGY_ID).get(0);
            try {
                distribution.getContainerObjectInformations(strategyId, objectId);
                return Response.status(Status.OK).build();
            } catch (final StorageNotFoundException e) {
                LOGGER.error(e);
                return buildErrorResponse(VitamCode.STORAGE_NOT_FOUND);
            }
        }
        return response;
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
        return Response.status(status).entity(getErrorEntity(status)).build();
    }

    /**
     * Get an object
     * <p>
     * Note : this is NOT to be handled in item #72.
     *
     * @param headers http header
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
        return Response.status(status).entity(getErrorEntity(status)).build();
    }

    /**
     * @param headers http header
     * @param objectId the id of the object
     * @param asyncResponse async response
     * @throws IOException exception
     */
    @Path("/logbooks/{id_logbook}")
    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public void getLogbook(@Context HttpHeaders headers, @PathParam("id_logbook") String objectId,
        @Suspended final AsyncResponse asyncResponse) throws IOException {
        VitamThreadPoolExecutor.getDefaultExecutor().execute(new Runnable() {

            @Override
            public void run() {
                getByCategoryAsync(objectId, headers, DataCategory.LOGBOOK, asyncResponse);
            }
        });

    }

    /**
     * Post a new object
     *
     * @param httpServletRequest http servlet request to get requester
     * @param headers http header
     * @param logbookId the id of the logbookId
     * @param createObjectDescription the workspace information about logbook to be created
     * @return Response NOT_IMPLEMENTED
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
     * @param headers http header
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
        return Response.status(status).entity(getErrorEntity(status)).build();
    }

    /**
     * Check the existence of a logbook Note : this is NOT to be handled in item #72.
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
        Response response = checkTenantStrategyHeader(headers);
        if (response != null) {
            return response;
        }
        final Status status = Status.NOT_IMPLEMENTED;
        return Response.status(status).entity(getErrorEntity(status)).build();
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
        return Response.status(status).entity(getErrorEntity(status)).build();
    }

    /**
     * Get a unit
     * <p>
     * Note : this is NOT to be handled in item #72.
     *
     * @param headers http header
     * @param metadataId the id of the unit metadata
     * @return Response NOT_IMPLEMENTED
     */
    @Path("/units/{id_md}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getUnit(@Context HttpHeaders headers, @PathParam("id_md") String metadataId) {
        Response response = checkTenantStrategyHeader(headers);
        if (response != null) {
            return response;
        }
        final Status status = Status.NOT_IMPLEMENTED;
        return Response.status(status).entity(getErrorEntity(status)).build();
    }

    /**
     * Post a new unit metadata
     *
     * @param httpServletRequest http servlet request to get requester
     * @param headers http header
     * @param metadataId the id of the unit metadata
     * @param createObjectDescription the workspace description of the unit to be created
     * @return Response NOT_IMPLEMENTED
     */
    // TODO P1: remove httpServletRequest when requester information sent by
    // header (X-Requester)
    @Path("/units/{id_md}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createUnitMetadata(@Context HttpServletRequest httpServletRequest, @Context HttpHeaders headers,
        @PathParam("id_md") String metadataId, ObjectDescription createObjectDescription) {
        if (createObjectDescription == null) {
            return getUnit(headers, metadataId);
        } else {
            // TODO P1: actually no X-Requester header, so send the getRemoteAdr
            // from HttpServletRequest
            return createObjectByType(headers, metadataId, createObjectDescription, DataCategory.UNIT,
                httpServletRequest.getRemoteAddr());
        }
    }

    /**
     * Update a unit metadata
     * <p>
     * Note : this is NOT to be handled in item #72.
     *
     * @param headers http header
     * @param metadataId the id of the unit metadata
     * @param query the query as a JsonNode
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
        return Response.status(status).entity(getErrorEntity(status)).build();
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
        Response response = checkTenantStrategyHeader(headers);
        if (response != null) {
            return response;
        }
        Status status = Status.NOT_IMPLEMENTED;
        if (!DataCategory.UNIT.canDelete()) {
            status = Status.UNAUTHORIZED;
        }
        return Response.status(status).entity(getErrorEntity(status)).build();
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
        Response response = checkTenantStrategyHeader(headers);
        if (response != null) {
            return response;
        }
        final Status status = Status.NOT_IMPLEMENTED;
        return Response.status(status).entity(getErrorEntity(status)).build();
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
        return Response.status(status).entity(getErrorEntity(status)).build();
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
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_OCTET_STREAM, CommonMediaType.ZIP})
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getObjectGroup(@Context HttpHeaders headers, @PathParam("id_md") String metadataId) {
        Response response = checkTenantStrategyHeader(headers);
        if (response != null) {
            return response;
        }
        final Status status = Status.NOT_IMPLEMENTED;
        return Response.status(status).entity(getErrorEntity(status)).build();
    }

    /**
     * Post a new Object Group metadata
     * <p>
     * Note : this is NOT to be handled in item #72.
     *
     * @param httpServletRequest http servlet request to get requester
     * @param headers http header
     * @param metadataId the id of the Object Group metadata
     * @param createObjectDescription the workspace description of the unit to be created
     * @return Response Created, not found or internal server error
     */
    // TODO P1: remove httpServletRequest when requester information sent by
    // header (X-Requester)
    // TODO P1 : check the existence, in the headers, of the value
    // X-Http-Method-Override, if set
    @Path("/objectgroups/{id_md}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createObjectGroup(@Context HttpServletRequest httpServletRequest, @Context HttpHeaders headers,
        @PathParam("id_md") String metadataId, ObjectDescription createObjectDescription) {
        if (createObjectDescription == null) {
            return getObjectGroup(headers, metadataId);
        } else {
            // TODO P1: actually no X-Requester header, so send the getRemoteAdr
            // from HttpServletRequest
            return createObjectByType(headers, metadataId, createObjectDescription, DataCategory.OBJECT_GROUP,
                httpServletRequest.getRemoteAddr());
        }
    }

    /**
     * Update a Object Group metadata
     * <p>
     * Note : this is NOT to be handled in item #72.
     *
     * @param headers http header
     * @param metadataId the id of the unit metadata
     * @param query the query as a JsonNode
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
        if (!DataCategory.OBJECT_GROUP.canUpdate()) {
            status = Status.UNAUTHORIZED;
        }
        return Response.status(status).entity(getErrorEntity(status)).build();
    }

    /**
     * Delete a Object Group metadata
     * <p>
     * Note : this is NOT to be handled in item #72.
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
        Response response = checkTenantStrategyHeader(headers);
        if (response != null) {
            return response;
        }
        Status status = Status.NOT_IMPLEMENTED;
        if (!DataCategory.OBJECT_GROUP.canDelete()) {
            status = Status.UNAUTHORIZED;
        }
        return Response.status(status).entity(getErrorEntity(status)).build();
    }

    /**
     * Check the existence of a Object Group metadata
     * <p>
     * Note : this is NOT to be handled in item #72.
     *
     * @param headers http header
     * @param metadataId the id of the Object Group metadata
     * @return Response OK if the object exists, NOT_FOUND otherwise (or BAD_REQUEST in cas of bad request format)
     */
    @Path("/objectgroups/{id_md}")
    @HEAD
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response checkObjectGroup(@Context HttpHeaders headers, @PathParam("id_md") String metadataId) {
        Response response = checkTenantStrategyHeader(headers);
        if (response != null) {
            return response;
        }
        final Status status = Status.NOT_IMPLEMENTED;
        return Response.status(status).entity(getErrorEntity(status)).build();
    }

    private Response buildErrorResponse(VitamCode vitamCode) {
        return Response.status(vitamCode.getStatus())
            .entity(new RequestResponseError().setError(new VitamError(VitamCodeHelper.getCode(vitamCode))
                .setContext(vitamCode.getService().getName()).setState(vitamCode.getDomain().getName())
                .setMessage(vitamCode.getMessage()).setDescription(vitamCode.getMessage())).toString())
            .build();
    }

    private Response badRequestResponse(String message) {
        return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\":\"" + message + "\"}").build();
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
     * @param asyncResponse
     * @throws IOException throws an IO Exception
     */
    @Path("/reports/{id_report}")
    @GET
    @Produces({MediaType.APPLICATION_OCTET_STREAM, CommonMediaType.ZIP})
    public void getReport(@Context HttpHeaders headers, @PathParam("id_report") String objectId,
        @Suspended final AsyncResponse asyncResponse) throws IOException {
        VitamThreadPoolExecutor.getDefaultExecutor().execute(new Runnable() {

            @Override
            public void run() {
                getByCategoryAsync(objectId, headers, DataCategory.REPORT, asyncResponse);
            }
        });

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
     * @param asyncResponse
     * @throws IOException
     */
    @Path("/manifests/{id_manifest}")
    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public void getManifest(@Context HttpHeaders headers, @PathParam("id_manifest") String objectId,
        @Suspended final AsyncResponse asyncResponse) throws IOException {
        VitamThreadPoolExecutor.getDefaultExecutor().execute(new Runnable() {

            @Override
            public void run() {
                getByCategoryAsync(objectId, headers, DataCategory.MANIFEST, asyncResponse);
            }
        });

    }

    /**
     * Run storage logbook secure operation
     *
     * @param xTenantId the tenant id
     * @return the response with a specific HTTP status
     */
    @POST
    @Path("/storage/secure")
    @Produces(MediaType.APPLICATION_JSON)
    public Response secureStorageLogbook(@HeaderParam(GlobalDataRest.X_TENANT_ID) String xTenantId) {
        if (Strings.isNullOrEmpty(xTenantId)) {
            LOGGER.error(MISSING_THE_TENANT_ID_X_TENANT_ID);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        try {
            Integer tenantId = Integer.parseInt(xTenantId);
            VitamThreadUtils.getVitamSession().setTenantId(tenantId);
            final GUID guid = storageLogbookAdministration.generateSecureStorageLogbook();
            final List<String> resultAsJson = new ArrayList<>();
            resultAsJson.add(guid.toString());
            return Response.status(Status.OK)
                .entity(new RequestResponseOK<String>()
                    .addAllResults(resultAsJson))
                .build();

        } catch (LogbookClientServerException | TraceabilityException | IOException |
            StorageLogException | LogbookClientAlreadyExistsException | LogbookClientBadRequestException e) {
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
     * @param storageLogname         the id of the object
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
     * Get a report
     *
     * @param headers http header
     * @param profileFileName the id of the object
     * @param asyncResponse
     * @throws IOException throws an IO Exception
     */
    @Path("/profiles/{profile_file_name}")
    @GET
    @Produces({MediaType.APPLICATION_OCTET_STREAM, CommonMediaType.ZIP})
    public void downloadProfile(@Context HttpHeaders headers, @PathParam("profile_file_name") String profileFileName,
        @Suspended final AsyncResponse asyncResponse) throws IOException {
        VitamThreadPoolExecutor.getDefaultExecutor().execute(
            () -> getByCategoryAsync(profileFileName, headers, DataCategory.PROFILE, asyncResponse));

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

    /**
     * Add error response in async response using with vitamCode
     *
     * @param vitamCode vitam error Code
     * @param asyncResponse asynchronous response
     */
    private void buildErrorResponseAsync(VitamCode vitamCode, AsyncResponse asyncResponse) {
        AsyncInputStreamHelper.asyncResponseResume(asyncResponse,
            Response.status(vitamCode.getStatus()).entity(new RequestResponseError().setError(
                new VitamError(VitamCodeHelper.getCode(vitamCode))
                    .setContext(vitamCode.getService().getName())
                    .setState(vitamCode.getDomain().getName())
                    .setMessage(vitamCode.getMessage())
                    .setDescription(vitamCode.getMessage()))
                .toString()).build());
    }

    private VitamError getErrorEntity(Status status) {
        return new VitamError(status.name()).setHttpCode(status.getStatusCode()).setContext(STORAGE_MODULE)
            .setState(CODE_VITAM)
            .setMessage(status.getReasonPhrase()).setDescription(status.getReasonPhrase());
    }

    @Override
    public void close() {
        distribution.close();
    }

    /**
     * Getter of Storage service
     * 
     * @return
     */
    public StorageLogbookService getStorageLogbookService() {
        return storageLogbookService;
    }

}
