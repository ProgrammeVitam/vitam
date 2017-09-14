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
package fr.gouv.vitam.access.external.rest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
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
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.access.external.api.AccessExtAPI;
import fr.gouv.vitam.access.internal.client.AccessInternalClient;
import fr.gouv.vitam.access.internal.client.AccessInternalClientFactory;
import fr.gouv.vitam.access.internal.common.exception.AccessInternalClientNotFoundException;
import fr.gouv.vitam.access.internal.common.exception.AccessInternalClientServerException;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.error.ServiceName;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.AccessUnauthorizedException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamThreadAccessException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.model.administration.AccessionRegisterDetailModel;
import fr.gouv.vitam.common.model.administration.ContextModel;
import fr.gouv.vitam.common.model.administration.FileFormatModel;
import fr.gouv.vitam.common.model.administration.IngestContractModel;
import fr.gouv.vitam.common.model.administration.ProfileModel;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.security.rest.EndpointInfo;
import fr.gouv.vitam.common.security.rest.SecureEndpointRegistry;
import fr.gouv.vitam.common.security.rest.Secured;
import fr.gouv.vitam.common.security.rest.Unsecured;
import fr.gouv.vitam.common.server.application.AsyncInputStreamHelper;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.functional.administration.common.exception.DatabaseConflictException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesImportInProgressException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesNotFoundException;
import fr.gouv.vitam.functional.administration.common.exception.ProfileNotFoundException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;

/**
 * Admin Management External Resource Implement
 */
@Path("/admin-external/v1")
@javax.ws.rs.ApplicationPath("webresources")
public class AdminManagementExternalResourceImpl {
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String CONTENT_DISPOSITION = "Content-Disposition";
    private static final String ATTACHEMENT_FILENAME = "attachment; filename=ErrorReport.json";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AdminManagementExternalResourceImpl.class);
    private static final String ACCESS_EXTERNAL_MODULE = "ADMIN_EXTERNAL";
    private static final String CODE_VITAM = "code_vitam";

    private final SecureEndpointRegistry secureEndpointRegistry;

    /**
     * Constructor
     * 
     * @param secureEndpointRegistry endpoint list registry
     */
    public AdminManagementExternalResourceImpl(SecureEndpointRegistry secureEndpointRegistry) {
        this.secureEndpointRegistry = secureEndpointRegistry;
        LOGGER.debug("init Admin Management Resource server");
    }

    /**
     * List secured resource end points
     */
    @Path("/")
    @OPTIONS
    @Produces(MediaType.APPLICATION_JSON)
    @Unsecured()
    public Response listResourceEndpoints() {

        String resourcePath = AdminManagementExternalResourceImpl.class.getAnnotation(Path.class).value();

        List<EndpointInfo> securedEndpointList = this.secureEndpointRegistry.getEndPointsByResourcePath(resourcePath);

        return Response.status(Status.OK).entity(securedEndpointList).build();
    }

    /**
     * checkFormat
     *
     * @param document the document to check
     * @param asyncResponse
     * @return Response
     */
    @Path("/formats")
    @PUT
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Secured(permission = "formats:check",
        description = "Vérifier si le référentiel des formats que l'on souhaite importer est valide")
    public void checkFormat(InputStream document, @Suspended final AsyncResponse asyncResponse) {
        Integer tenantId = ParameterHelper.getTenantParameter();
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(tenantId));
        addRequestId();

        ParametersChecker.checkParameter("xmlPronom is a mandatory parameter", document);
        VitamThreadPoolExecutor.getDefaultExecutor()
            .execute(() -> asyncCheckFormat(document, asyncResponse));
    }

    /**
     * checkRules
     *
     * @param document the document to check
     * @return Response
     */
    @Path("/rules")
    @PUT
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Secured(permission = "rules:check",
        description = "Vérifier si le référentiel de règles de gestions que l'on souhaite importer est valide")
    public void checkRules(InputStream document,
        @Suspended final AsyncResponse asyncResponse) {
        Integer tenantId = ParameterHelper.getTenantParameter();
        LOGGER.debug(String.format("tenant Id %d", tenantId));
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(tenantId));
        addRequestId();

        VitamThreadPoolExecutor.getDefaultExecutor()
            .execute(() -> asyncDownloadErrorReport(document, asyncResponse));
    }

    /**
     * Check Format async call
     * 
     * @param document inputStream to check
     * @param asyncResponse asyncResponse
     */
    private void asyncCheckFormat(InputStream document, AsyncResponse asyncResponse) {
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            Response response = client.checkFormat(document);
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse, response);
        } catch (ReferentialException ex) {
            asyncResponseResume(asyncResponse, VitamCode.ADMIN_EXTERNAL_CHECK_DOCUMENT_BAD_REQUEST,
                ex.getLocalizedMessage(), document);
        }
    }


    /**
     * Return the Error Report or close the asyncResonse
     * 
     * @param document the referential to check
     * @param asyncResponse asyncResponse
     */
    private void asyncDownloadErrorReport(InputStream document, final AsyncResponse asyncResponse) {
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            final Response response = client.checkRulesFile(document);
            final AsyncInputStreamHelper helper = new AsyncInputStreamHelper(asyncResponse, response);
            final ResponseBuilder responseBuilder =
                Response.status(response.getStatus())
                    .header(CONTENT_DISPOSITION, ATTACHEMENT_FILENAME)
                    .header(CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM);
            helper.writeResponse(responseBuilder);
        } catch (Exception e) {
            asyncResponseResume(asyncResponse, VitamCode.ADMIN_EXTERNAL_CHECK_DOCUMENT_ERROR, e.getLocalizedMessage(),
                document);
        }
    }

    /**
     * Resume the asyncResponse in case of Exception
     * 
     * @param asyncResponse the given asyncResponse
     * @param vitamCode the vitam code
     * @param message message
     * @param document the given document to import
     */
    private void asyncResponseResume(AsyncResponse asyncResponse, VitamCode vitamCode, String message,
        InputStream document) {
        LOGGER.error(message);
        AsyncInputStreamHelper
            .asyncResponseResume(
                asyncResponse,
                Response.status(vitamCode.getStatus())
                    .entity(getErrorStream(VitamCodeHelper
                        .toVitamError(vitamCode, message)).toString())
                    .build(),
                document);
    }

    /**
     * Import a format
     *
     * @param headers http headers
     * @param uriInfo used to construct the created resource and send it back as location in the response
     * @param document inputStream representing the data to import
     * @return The jaxRs Response
     */
    @Path("/formats")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "formats:create", description = "Importer un référentiel des formats")
    public Response importFormat(@Context HttpHeaders headers, @Context UriInfo uriInfo,
        InputStream document) {
        Integer tenantId = ParameterHelper.getTenantParameter();
        String filename = headers.getHeaderString(GlobalDataRest.X_FILENAME);
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(tenantId));
        try {
            ParametersChecker.checkParameter("document is a mandatory parameter", document);
            try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
                Status status = client.importFormat(document, filename);
                // Send the http response with no entity and the status got from internalService;
                ResponseBuilder ResponseBuilder = Response.status(status);
                return ResponseBuilder.build();
            } catch (final DatabaseConflictException e) {
                LOGGER.error(e);
                final Status status = Status.CONFLICT;
                return Response.status(status).entity(getErrorEntity(status, e.getMessage(), null)).build();
            } catch (final FileRulesImportInProgressException e) {
                LOGGER.warn(e);
                return Response.status(Status.FORBIDDEN)
                    .entity(getErrorEntity(Status.FORBIDDEN, e.getMessage(), null)).build();
            } catch (final ReferentialException e) {
                LOGGER.error(e);
                return Response.status(Status.BAD_REQUEST)
                    .entity(getErrorEntity(Status.BAD_REQUEST, e.getMessage(), null)).build();
            }
        } catch (final IllegalArgumentException e) {
            LOGGER.error(e);
            final Status status = Status.BAD_REQUEST;
            return Response.status(status).entity(getErrorEntity(status, e.getMessage(), null)).build();
        } finally {
            StreamUtils.closeSilently(document);
        }
    }


    /**
     * Import a rules document
     *
     * @param headers http headers
     * @param document inputStream representing the data to import
     * @return The jaxRs Response
     */
    @Path("/rules")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "rules:create", description = "Importer un référentiel des règles de gestion")
    public Response importRulesFile(@Context HttpHeaders headers,
        InputStream document) {
        Integer tenantId = ParameterHelper.getTenantParameter();
        String filename = headers.getHeaderString(GlobalDataRest.X_FILENAME);
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(tenantId));
        try {
            ParametersChecker.checkParameter("document is a mandatory parameter", document);
            try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
                Status status = client.importRulesFile(document, filename);

                // Send the http response with no entity and the status got from internalService;
                ResponseBuilder ResponseBuilder = Response.status(status);
                return ResponseBuilder.build();
            } catch (final DatabaseConflictException e) {
                LOGGER.error(e);
                final Status status = Status.CONFLICT;
                return Response.status(status).entity(getErrorEntity(status, e.getMessage(), null)).build();
            } catch (final FileRulesImportInProgressException e) {
                LOGGER.warn(e);
                return Response.status(Status.FORBIDDEN)
                    .entity(getErrorEntity(Status.FORBIDDEN, e.getMessage(), null)).build();
            } catch (final ReferentialException e) {
                LOGGER.error(e);
                return Response.status(Status.BAD_REQUEST)
                    .entity(getErrorEntity(Status.BAD_REQUEST, e.getMessage(), null)).build();
            }
        } catch (final IllegalArgumentException e) {
            LOGGER.error(e);
            final Status status = Status.BAD_REQUEST;
            return Response.status(status).entity(getErrorEntity(status, e.getMessage(), null)).build();
        } finally {
            StreamUtils.closeSilently(document);
        }
    }

    /**
     * Import an entry contract
     *
     * @param document inputStream representing the data to import
     * @return The jaxRs Response
     */
    @Path("/entrycontracts")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "entrycontracts:create:binary",
        description = "Importer des contrats d'entrées dans le référentiel")
    public Response importIngestContracts(InputStream document) {
        Integer tenantId = ParameterHelper.getTenantParameter();
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(tenantId));
        try {
            ParametersChecker.checkParameter("document is a mandatory parameter", document);
            try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
                JsonNode json = JsonHandler.getFromInputStream(document);
                SanityChecker.checkJsonAll(json);
                Status status =
                    client.importIngestContracts(JsonHandler.getFromStringAsTypeRefence(json.toString(),
                        new TypeReference<List<IngestContractModel>>() {}));
                // Send the http response with no entity and the status got from internalService;
                ResponseBuilder ResponseBuilder = Response.status(status);
                return ResponseBuilder.build();
            } catch (final ReferentialException e) {
                LOGGER.error(e);
                return Response.status(Status.BAD_REQUEST)
                    .entity(getErrorEntity(Status.BAD_REQUEST, e.getMessage(), null)).build();
            } catch (InvalidParseOperationException e) {
                LOGGER.error(e);
                return Response.status(Status.BAD_REQUEST)
                    .entity(getErrorEntity(Status.BAD_REQUEST, e.getMessage(), null)).build();
            }
        } catch (final IllegalArgumentException e) {
            LOGGER.error(e);
            final Status status = Status.BAD_REQUEST;
            return Response.status(status).entity(getErrorEntity(status, e.getMessage(), null)).build();
        } finally {
            StreamUtils.closeSilently(document);
        }
    }

    private void downloadTraceabilityOperationFile(String operationId, final AsyncResponse asyncResponse) {
        AsyncInputStreamHelper helper;
        try (AccessInternalClient client = AccessInternalClientFactory.getInstance().getClient()) {
            final Response response = client.downloadTraceabilityFile(operationId);
            helper = new AsyncInputStreamHelper(asyncResponse, response);
            final ResponseBuilder responseBuilder = Response.status(Status.OK)
                .header("Content-Disposition", response.getHeaderString("Content-Disposition"))
                .type(response.getMediaType());
            helper.writeResponse(responseBuilder);
        } catch (final InvalidParseOperationException | IllegalArgumentException exc) {
            LOGGER.error(exc);
            final Response errorResponse = Response.status(Status.PRECONDITION_FAILED)
                .entity(getErrorStream(Status.PRECONDITION_FAILED, exc.getMessage(), null)).build();
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse, errorResponse);
        } catch (final AccessInternalClientServerException exc) {
            LOGGER.error(exc.getMessage(), exc);
            final Response errorResponse = Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(getErrorStream(Status.INTERNAL_SERVER_ERROR, exc.getMessage(), null)).build();
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse, errorResponse);
        } catch (final AccessInternalClientNotFoundException exc) {
            LOGGER.error(exc.getMessage(), exc);
            final Response errorResponse = Response.status(Status.NOT_FOUND)
                .entity(getErrorStream(Status.NOT_FOUND, exc.getMessage(), null)).build();
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse, errorResponse);
        } catch (AccessUnauthorizedException e) {
            LOGGER.error("Contract access does not allow ", e);
            final Response errorResponse = Response.status(Status.UNAUTHORIZED)
                .entity(getErrorStream(Status.UNAUTHORIZED, e.getMessage(), null)).build();
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse, errorResponse);
        }
    }


    /**
     * Import a set of ingest contracts.
     *
     * @param select the select query to find document
     * @return Response
     */
    @Path("/entrycontracts")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "entrycontracts:create:json",
        description = "Importer des contrats d'entrées dans le référentiel")
    public Response importIngestContracts(JsonNode select)
        throws DatabaseConflictException {

        addRequestId();
        ParametersChecker.checkParameter("Json select is a mandatory parameter", select);
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            SanityChecker.checkJsonAll(select);
            Status status = client.importIngestContracts(JsonHandler.getFromStringAsTypeRefence(select.toString(),
                new TypeReference<List<IngestContractModel>>() {}));

            // Send the http response with the entity and the status got from internalService;
            ResponseBuilder ResponseBuilder = Response.status(status)
                .entity("Successfully imported");
            return ResponseBuilder.build();
        } catch (final ReferentialException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST)
                .entity(getErrorEntity(Status.BAD_REQUEST, e.getMessage(), null)).build();
        } catch (InvalidParseOperationException e) {
            return Response.status(Status.BAD_REQUEST)
                .entity(getErrorEntity(Status.BAD_REQUEST, e.getMessage(), null)).build();
        }
    }

    /**
     * Import an access contract document
     *
     * @param document inputStream representing the data to import
     * @return The jaxRs Response
     */
    @Path("/accesscontracts")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "accesscontracts:create:binary",
        description = "Importer des contrats d'accès dans le référentiel")
    public Response importAccessContracts(InputStream document) {
        Integer tenantId = ParameterHelper.getTenantParameter();
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(tenantId));
        try {
            ParametersChecker.checkParameter("document is a mandatory parameter", document);
            try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {

                JsonNode json = JsonHandler.getFromInputStream(document);
                SanityChecker.checkJsonAll(json);
                Status status = client.importAccessContracts(JsonHandler.getFromStringAsTypeRefence(json.toString(),
                    new TypeReference<List<AccessContractModel>>() {}));
                // Send the http response with no entity and the status got from internalService;
                ResponseBuilder ResponseBuilder = Response.status(status);
                return ResponseBuilder.build();
            } catch (final ReferentialException e) {
                LOGGER.error(e);
                return Response.status(Status.BAD_REQUEST)
                    .entity(getErrorEntity(Status.BAD_REQUEST, e.getMessage(), null)).build();
            } catch (InvalidParseOperationException e) {
                return Response.status(Status.BAD_REQUEST)
                    .entity(getErrorEntity(Status.BAD_REQUEST, e.getMessage(), null)).build();
            }
        } catch (final IllegalArgumentException e) {
            LOGGER.error(e);
            final Status status = Status.BAD_REQUEST;
            return Response.status(status).entity(getErrorEntity(status, e.getMessage(), null)).build();
        } finally {
            StreamUtils.closeSilently(document);
        }
    }

    /**
     * Import a set of access contracts.
     *
     * @param select the select query to find document
     * @return Response
     */
    @Path("/accesscontracts")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "accesscontracts:create:json",
        description = "Importer des contrats d'accès dans le référentiel")
    public Response importAccessContracts(JsonNode select)
        throws DatabaseConflictException {

        addRequestId();
        ParametersChecker.checkParameter("Json select is a mandatory parameter", select);
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            SanityChecker.checkJsonAll(select);
            Status status = client.importAccessContracts(JsonHandler.getFromStringAsTypeRefence(select.toString(),
                new TypeReference<List<AccessContractModel>>() {}));

            // Send the http response with the entity and the status got from internalService;
            ResponseBuilder ResponseBuilder = Response.status(status)
                .entity("Successfully imported");
            return ResponseBuilder.build();
        } catch (final ReferentialException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST)
                .entity(getErrorEntity(Status.BAD_REQUEST, e.getMessage(), null)).build();
        } catch (InvalidParseOperationException e) {
            return Response.status(Status.BAD_REQUEST)
                .entity(getErrorEntity(Status.BAD_REQUEST, e.getMessage(), null)).build();
        }
    }

    /**
     * Import contexts
     *
     * @param document inputStream representing the data to import
     * @return The jaxRs Response
     */
    @Path("/contexts")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "contexts:create:binary", description = "Importer des contextes dans le référentiel")
    public Response importContexts(InputStream document) {
        Integer tenantId = ParameterHelper.getTenantParameter();
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(tenantId));
        try {
            ParametersChecker.checkParameter("document is a mandatory parameter", document);
            try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {

                JsonNode json = JsonHandler.getFromInputStream(document);
                SanityChecker.checkJsonAll(json);
                Status status = client.importContexts(JsonHandler.getFromStringAsTypeRefence(json.toString(),
                    new TypeReference<List<ContextModel>>() {}));


                // Send the http response with no entity and the status got from internalService;
                ResponseBuilder ResponseBuilder = Response.status(status);
                return ResponseBuilder.build();
            } catch (final FileRulesImportInProgressException e) {
                LOGGER.warn(e);
                return Response.status(Status.FORBIDDEN)
                    .entity(getErrorEntity(Status.FORBIDDEN, e.getMessage(), null)).build();
            } catch (final ReferentialException e) {
                LOGGER.error(e);
                return Response.status(Status.BAD_REQUEST)
                    .entity(getErrorEntity(Status.BAD_REQUEST, e.getMessage(), null)).build();
            } catch (InvalidParseOperationException e) {
                return Response.status(Status.BAD_REQUEST)
                    .entity(getErrorEntity(Status.BAD_REQUEST, e.getMessage(), null)).build();
            }
        } catch (final IllegalArgumentException e) {
            LOGGER.error(e);
            final Status status = Status.BAD_REQUEST;
            return Response.status(status).entity(getErrorEntity(status, e.getMessage(), null)).build();
        } finally {
            StreamUtils.closeSilently(document);
        }
    }

    /**
     * Import a set of contexts
     *
     * @param select the select query to find document
     * @return Response
     */
    @Path("/contexts")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "contexts:create:json", description = "Importer des contextes dans le référentiel")
    public Response importContexts(JsonNode select)
        throws DatabaseConflictException {

        addRequestId();
        ParametersChecker.checkParameter("Json select is a mandatory parameter", select);
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            SanityChecker.checkJsonAll(select);
            Status status = client.importContexts(JsonHandler.getFromStringAsTypeRefence(select.toString(),
                new TypeReference<List<ContextModel>>() {}));

            // Send the http response with the entity and the status got from internalService;
            ResponseBuilder ResponseBuilder = Response.status(status)
                .entity("Successfully imported");
            return ResponseBuilder.build();
        } catch (final ReferentialException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST)
                .entity(getErrorEntity(Status.BAD_REQUEST, e.getMessage(), null)).build();
        } catch (InvalidParseOperationException e) {
            return Response.status(Status.BAD_REQUEST)
                .entity(getErrorEntity(Status.BAD_REQUEST, e.getMessage(), null)).build();
        }
    }


    /**
     * Import a profiles document
     *
     * @param document inputStream representing the data to import
     * @return The jaxRs Response
     */
    @Path("/profiles")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "profiles:create:binary", description = "Importer des profils dans le référentiel")
    public Response createProfiles(InputStream document) {
        Integer tenantId = ParameterHelper.getTenantParameter();
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(tenantId));
        try {
            ParametersChecker.checkParameter("document is a mandatory parameter", document);
            try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {

                JsonNode json = JsonHandler.getFromInputStream(document);
                SanityChecker.checkJsonAll(json);
                RequestResponse requestResponse =
                    client.createProfiles(JsonHandler.getFromStringAsTypeRefence(json.toString(),
                        new TypeReference<List<ProfileModel>>() {}));
                return Response.status(requestResponse.getStatus())
                    .entity(requestResponse).build();

            } catch (final ReferentialException e) {
                LOGGER.error(e);
                return Response.status(Status.BAD_REQUEST)
                    .entity(getErrorEntity(Status.BAD_REQUEST, e.getMessage(), null)).build();
            } catch (InvalidParseOperationException e) {
                return Response.status(Status.BAD_REQUEST)
                    .entity(getErrorEntity(Status.BAD_REQUEST, e.getMessage(), null)).build();
            }
        } catch (final IllegalArgumentException e) {
            LOGGER.error(e);
            final Status status = Status.BAD_REQUEST;
            return Response.status(status).entity(getErrorEntity(status, e.getMessage(), null)).build();
        } finally {
            StreamUtils.closeSilently(document);
        }
    }

    /**
     * Import a set of profiles
     *
     * @param select the select query to find document
     * @return Response
     */
    @Path("/profiles")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "profiles:create:json", description = "Ecrire un profil dans le référentiel")
    public Response createProfiles(JsonNode select)
        throws DatabaseConflictException {

        addRequestId();
        ParametersChecker.checkParameter("Json select is a mandatory parameter", select);
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            SanityChecker.checkJsonAll(select);
            RequestResponse requestResponse =
                client.createProfiles(JsonHandler.getFromStringAsTypeRefence(select.toString(),
                    new TypeReference<List<ProfileModel>>() {}));
            return Response.status(requestResponse.getStatus())
                .entity(requestResponse).build();

        } catch (final ReferentialException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST)
                .entity(getErrorEntity(Status.BAD_REQUEST, e.getMessage(), null)).build();
        } catch (InvalidParseOperationException e) {
            return Response.status(Status.BAD_REQUEST)
                .entity(getErrorEntity(Status.BAD_REQUEST, e.getMessage(), null)).build();
        }
    }

    /**
     * Import a Profile file document (xsd or rng, ...)
     *
     * @param uriInfo used to construct the created resource and send it back as location in the response
     * @param profileMetadataId id of the profile metadata
     * @param profileFile inputStream representing the data to import
     * @return The jaxRs Response
     */
    @Path("/profiles/{id:.+}")
    @PUT
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "profiles:id:update", description = "Importer un fichier xsd ou rng dans un profil")
    public Response importProfileFile(@Context UriInfo uriInfo,
        @PathParam("id") String profileMetadataId,
        InputStream profileFile) {
        addRequestId();
        try {
            ParametersChecker.checkParameter("profileFile stream is a mandatory parameter", profileFile);
            ParametersChecker.checkParameter(profileMetadataId, "The profile id is mandatory");
            try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
                RequestResponse requestResponse = client.importProfileFile(profileMetadataId, profileFile);
                ResponseBuilder ResponseBuilder = Response.status(requestResponse.getStatus())
                    .entity(requestResponse);
                return ResponseBuilder.build();
            } catch (final DatabaseConflictException e) {
                LOGGER.error(e);
                final Status status = Status.CONFLICT;
                return Response.status(status).entity(getErrorEntity(status, e.getMessage(), null)).build();
            } catch (final ReferentialException e) {
                LOGGER.error(e);
                return Response.status(Status.BAD_REQUEST)
                    .entity(getErrorEntity(Status.BAD_REQUEST, e.getMessage(), null)).build();
            }
        } catch (final IllegalArgumentException e) {
            LOGGER.error(e);
            final Status status = Status.BAD_REQUEST;
            return Response.status(status).entity(getErrorEntity(status, e.getMessage(), null)).build();
        } finally {
            StreamUtils.closeSilently(profileFile);
        }
    }

    /**
     * Download the profile file<br/>
     * <br/>
     * <b>The caller is responsible to close the Response after consuming the inputStream.</b>
     *
     * @param fileId
     * @param asyncResponse
     */
    @GET
    @Path("/profiles/{id:.+}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Secured(permission = "profiles:id:read:binary",
        description = "Télecharger le fichier xsd ou rng attaché à un profil")
    public void downloadProfileFile(
        @PathParam("id") String fileId,
        @Suspended final AsyncResponse asyncResponse) {

        ParametersChecker.checkParameter("Profile id should be filled", fileId);
        addRequestId();
        VitamThreadPoolExecutor.getDefaultExecutor()
            .execute(() -> asyncDownloadProfileFile(fileId, asyncResponse));
    }

    /**
     * Download the traceability file<br/>
     * <br/>
     * <b>The caller is responsible to close the Response after consuming the inputStream.</b>
     *
     * @param fileId
     * @param asyncResponse
     */
    @GET
    @Path("/traceability/{id:.+}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Secured(permission = "traceability:id:read",
        description = "Télécharger le logbook sécurisé attaché à une opération de sécurisation")
    public void downloadTraceabilityFile(
        @PathParam("id") String fileId,
        @Suspended final AsyncResponse asyncResponse) {

        try {
            ParametersChecker.checkParameter("Traceability operation should be filled", fileId);

            addRequestId();

            VitamThreadPoolExecutor.getDefaultExecutor()
                .execute(() -> downloadTraceabilityOperationFile(fileId, asyncResponse));
        } catch (IllegalArgumentException | VitamThreadAccessException e) {
            LOGGER.error(e);
            final Response errorResponse = Response.status(Status.PRECONDITION_FAILED)
                .entity(getErrorStream(Status.PRECONDITION_FAILED, e.getMessage(), null))
                .build();
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse, errorResponse);
        }
    }

    private void asyncDownloadProfileFile(String profileMetadataId, final AsyncResponse asyncResponse) {
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            final Response response = client.downloadProfileFile(profileMetadataId);
            final AsyncInputStreamHelper helper = new AsyncInputStreamHelper(asyncResponse, response);
            final ResponseBuilder responseBuilder =
                Response.status(Status.OK)
                    .header(CONTENT_DISPOSITION, response.getHeaderString(CONTENT_DISPOSITION))
                    .type(response.getMediaType());
            helper.writeResponse(responseBuilder);
        } catch (final ProfileNotFoundException exc) {
            LOGGER.error(exc.getMessage(), exc);
            AsyncInputStreamHelper
                .asyncResponseResume(
                    asyncResponse,
                    Response.status(Status.NOT_FOUND)
                        .entity(getErrorStream(Status.NOT_FOUND, exc.getMessage(), null).toString()).build());
        } catch (final AdminManagementClientServerException exc) {
            LOGGER.error(exc.getMessage(), exc);
            AsyncInputStreamHelper
                .asyncResponseResume(
                    asyncResponse,
                    Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity(getErrorStream(Status.INTERNAL_SERVER_ERROR, exc.getMessage(), null).toString())
                        .build());
        }
    }

    /**
     * getFormats using get method
     *
     * @param select the select query to find document
     * @return Response
     */
    @Path("/formats")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "formats:read", description = "Lister le contenu du référentiel des formats")
    public Response getFormats(JsonNode select) {

        addRequestId();
        try {
            try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
                final RequestResponse<FileFormatModel> result = client.getFormats(select);
                int st = result.isOk() ? Status.OK.getStatusCode() : result.getHttpCode();
                return Response.status(st).entity(result).build();
            } catch (ReferentialNotFoundException | FileRulesNotFoundException e) {
                final Status status = Status.NOT_FOUND;
                return Response.status(status).entity(getErrorEntity(status, e.getMessage(), null)).build();
            } catch (ReferentialException | IOException e) {
                LOGGER.error(e);
                final Status status = Status.INTERNAL_SERVER_ERROR;
                return Response.status(status).entity(getErrorEntity(status, e.getMessage(), null)).build();
            } catch (final InvalidParseOperationException e) {
                LOGGER.error(e);
                final Status status = Status.BAD_REQUEST;
                return Response.status(status).entity(getErrorEntity(status, e.getMessage(), null)).build();
            }
        } catch (IllegalArgumentException e) {
            LOGGER.error(e);
            final Status status = Status.PRECONDITION_FAILED;
            return Response.status(status).entity(getErrorEntity(status, e.getMessage(), null)).build();
        }
    }

    /**
     * getRules using get method
     *
     * @param select the select query to find document
     * @return Response
     */
    @Path("/rules")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "rules:read", description = "Lister le contenu du référentiel des règles de gestion")
    public Response getRules(JsonNode select) {

        addRequestId();
        try {
            try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
                final JsonNode result = client.getRules(select);
                return Response.status(Status.OK).entity(result).build();
            } catch (FileRulesNotFoundException e) {
                final Status status = Status.NOT_FOUND;
                return Response.status(status).entity(getErrorEntity(status, e.getMessage(), null)).build();
            } catch (ReferentialException | IOException e) {
                LOGGER.error(e);
                final Status status = Status.INTERNAL_SERVER_ERROR;
                return Response.status(status).entity(getErrorEntity(status, e.getMessage(), null)).build();
            } catch (final InvalidParseOperationException e) {
                LOGGER.error(e);
                final Status status = Status.BAD_REQUEST;
                return Response.status(status).entity(getErrorEntity(status, e.getMessage(), null)).build();
            }
        } catch (IllegalArgumentException e) {
            LOGGER.error(e);
            final Status status = Status.PRECONDITION_FAILED;
            return Response.status(status).entity(getErrorEntity(status, e.getMessage(), null)).build();
        }
    }

    /**
     * findIngestContracts using get method
     *
     * @param select the select query to find document
     * @return Response
     */
    @Path("/entrycontracts")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "entrycontracts:read", description = "Lister le contenu du référentiel des contrats d'entrée")
    public Response findIngestContracts(JsonNode select) {

        addRequestId();
        try {
            try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
                RequestResponse<IngestContractModel> result = client.findIngestContracts(select);
                int st = result.isOk() ? Status.OK.getStatusCode() : result.getHttpCode();
                return Response.status(st).entity(result).build();
            } catch (ReferentialException e) {
                LOGGER.error(e);
                final Status status = Status.INTERNAL_SERVER_ERROR;
                return Response.status(status).entity(getErrorEntity(status, e.getMessage(), null)).build();
            } catch (final InvalidParseOperationException e) {
                LOGGER.error(e);
                final Status status = Status.BAD_REQUEST;
                return Response.status(status).entity(getErrorEntity(status, e.getMessage(), null)).build();
            }
        } catch (IllegalArgumentException e) {
            LOGGER.error(e);
            final Status status = Status.PRECONDITION_FAILED;
            return Response.status(status).entity(getErrorEntity(status, e.getMessage(), null)).build();
        }
    }

    /**
     * findAccessContracts using get method
     *
     * @param select the select query to find document
     * @return Response
     */
    @Path("/accesscontracts")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "accesscontracts:read", description = "Lister le contenu du référentiel des contrats d'accès")
    public Response findAccessContracts(JsonNode select) {

        addRequestId();
        try {
            try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
                RequestResponse result = client.findAccessContracts(select);
                int st = result.isOk() ? Status.OK.getStatusCode() : result.getHttpCode();
                return Response.status(st).entity(result).build();
            } catch (ReferentialException e) {
                LOGGER.error(e);
                final Status status = Status.INTERNAL_SERVER_ERROR;
                return Response.status(status).entity(getErrorEntity(status, e.getMessage(), null)).build();
            } catch (final InvalidParseOperationException e) {
                LOGGER.error(e);
                final Status status = Status.BAD_REQUEST;
                return Response.status(status).entity(getErrorEntity(status, e.getMessage(), null)).build();
            }
        } catch (IllegalArgumentException e) {
            LOGGER.error(e);
            final Status status = Status.PRECONDITION_FAILED;
            return Response.status(status).entity(getErrorEntity(status, e.getMessage(), null)).build();
        }
    }

    /**
     * findProfiles using get method
     *
     * @param select the select query to find document
     * @return Response
     */
    @Path("/profiles")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "profiles:read", description = "Lister le contenu du référentiel des profils")
    public Response findProfiles(JsonNode select) {

        addRequestId();
        try {
            try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
                RequestResponse result = client.findProfiles(select);
                int st = result.isOk() ? Status.OK.getStatusCode() : result.getHttpCode();
                return Response.status(st).entity(result).build();
            } catch (ReferentialException e) {
                LOGGER.error(e);
                final Status status = Status.INTERNAL_SERVER_ERROR;
                return Response.status(status).entity(getErrorEntity(status, e.getMessage(), null)).build();
            } catch (final InvalidParseOperationException e) {
                LOGGER.error(e);
                final Status status = Status.BAD_REQUEST;
                return Response.status(status).entity(getErrorEntity(status, e.getMessage(), null)).build();
            }
        } catch (IllegalArgumentException e) {
            LOGGER.error(e);
            final Status status = Status.PRECONDITION_FAILED;
            return Response.status(status).entity(getErrorEntity(status, e.getMessage(), null)).build();
        }
    }

    /**
     * findContexts using get method
     *
     * @param select the select query to find document
     * @return Response
     */
    @Path("/contexts")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "contexts:read", description = "Lister le contenu du référentiel des contextes")
    public Response findContexts(JsonNode select) {

        addRequestId();
        try {
            try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
                RequestResponse result = client.findContexts(select);
                int st = result.isOk() ? Status.OK.getStatusCode() : result.getHttpCode();
                return Response.status(st).entity(result).build();
            } catch (ReferentialException e) {
                LOGGER.error(e);
                final Status status = Status.INTERNAL_SERVER_ERROR;
                return Response.status(status).entity(getErrorEntity(status, e.getMessage(), null)).build();
            } catch (final InvalidParseOperationException e) {
                LOGGER.error(e);
                final Status status = Status.BAD_REQUEST;
                return Response.status(status).entity(getErrorEntity(status, e.getMessage(), null)).build();
            }
        } catch (IllegalArgumentException e) {
            LOGGER.error(e);
            final Status status = Status.PRECONDITION_FAILED;
            return Response.status(status).entity(getErrorEntity(status, e.getMessage(), null)).build();
        }
    }

    /**
     * getAccessionRegister using get method
     *
     * @param select the select query to find document
     * @return Response
     */
    @Path("/accession-registers")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "accession-registers:read",
        description = "Lister le contenu du référentiel des registres des fonds")
    public Response getAccessionRegister(JsonNode select) {

        addRequestId();
        try {
            try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {

                final RequestResponse result = client.getAccessionRegister(select);
                int st = result.isOk() ? Status.OK.getStatusCode() : result.getHttpCode();
                return Response.status(st).entity(result).build();
            } catch (ReferentialNotFoundException | FileRulesNotFoundException e) {
                final Status status = Status.NOT_FOUND;
                return Response.status(status).entity(getErrorEntity(status, e.getMessage(), null)).build();
            } catch (ReferentialException e) {
                LOGGER.error(e);
                final Status status = Status.INTERNAL_SERVER_ERROR;
                return Response.status(status).entity(getErrorEntity(status, e.getMessage(), null)).build();
            } catch (final InvalidParseOperationException e) {
                LOGGER.error(e);
                final Status status = Status.BAD_REQUEST;
                return Response.status(status).entity(getErrorEntity(status, e.getMessage(), null)).build();
            } catch (final AccessUnauthorizedException e) {
                LOGGER.error("Access contract does not allow ", e);
                final Status status = Status.UNAUTHORIZED;
                return Response.status(status).entity(getErrorEntity(status, e.getMessage(), null)).build();
            }
        } catch (IllegalArgumentException e) {
            LOGGER.error(e);
            final Status status = Status.PRECONDITION_FAILED;
            return Response.status(status).entity(getErrorEntity(status, e.getMessage(), null)).build();
        }
    }

    /**
     * Create or update an accession register
     *
     * @param select the select query to find document
     * @return Response
     */
    @Path("/accession-registers")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "accession-registers:create",
        description = "Permet de créer ou de modifier le registre des fonds.")
    // FIXME : EST-CE VRAIMENT UNE BONNE IDEE d'exposer une API external pour modifier le registre des fonds?!!!
    public Response createOrUpdateAccessionRegister(JsonNode select)
        throws DatabaseConflictException {

        addRequestId();
        ParametersChecker.checkParameter("Json select is a mandatory parameter", select);
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {

            SanityChecker.checkJsonAll(select);
            RequestResponse requestResponse =
                client.createorUpdateAccessionRegister(JsonHandler.getFromStringAsTypeRefence(select.toString(),
                    new TypeReference<AccessionRegisterDetailModel>() {}));

            return Response.status(requestResponse.getStatus())
                .entity(requestResponse).build();
        } catch (final ReferentialException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST)
                .entity(getErrorEntity(Status.BAD_REQUEST, e.getMessage(), null)).build();
        } catch (InvalidParseOperationException e) {
            return Response.status(Status.BAD_REQUEST)
                .entity(getErrorEntity(Status.BAD_REQUEST, e.getMessage(), null)).build();
        }
    }

    /**
     * With Document By Id
     *
     * @param collection
     * @param documentId
     * @return Response
     */
    @Path("/{collection}/{id_document:.+}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Unsecured
    public Response findDocumentByID(@PathParam("collection") String collection,
        @PathParam("id_document") String documentId) {
        // FIXME: Change @Unsecured with @Secured once implemented
        addRequestId();
        return Response.status(Status.BAD_REQUEST)
            .entity(getErrorEntity(Status.BAD_REQUEST, "Method not yet implemented", null)).build();
    }

    /**
     * findFormatByID
     *
     * @param documentId the document id to find
     * @return Response
     */
    @Path("/formats/{id_document:.+}")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "formats:id:read", description = "Lire un format donné")
    public Response findFormatByID(@PathParam("id_document") String documentId, JsonNode select) {
        addRequestId();
        try {
            ParametersChecker.checkParameter("formatId is a mandatory parameter", documentId);
            SanityChecker.checkParameter(documentId);
            try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
                final JsonNode result = client.getFormatByID(documentId);
                return Response.status(Status.OK).entity(result).build();
            } catch (ReferentialNotFoundException e) {
                LOGGER.error(e);
                final Status status = Status.NOT_FOUND;
                return Response.status(status).entity(getErrorEntity(status, e.getMessage(), null)).build();
            } catch (final ReferentialException e) {
                LOGGER.error(e);
                final Status status = Status.INTERNAL_SERVER_ERROR;
                return Response.status(status).entity(getErrorEntity(status, e.getMessage(), null)).build();
            } catch (final InvalidParseOperationException e) {
                LOGGER.error(e);
                final Status status = Status.BAD_REQUEST;
                return Response.status(status).entity(getErrorEntity(status, e.getMessage(), null)).build();
            }
        } catch (final IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error(e);
            return Response.status(Status.PRECONDITION_FAILED)
                .entity(getErrorEntity(Status.PRECONDITION_FAILED, e.getMessage(), null)).build();
        }
    }

    /**
     * findRuleByID
     *
     * @param documentId the document id to find
     * @return Response
     */
    @Path("/rules/{id_document:.+}")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "rules:id:read", description = "Lire une règle de gestion donnée")
    public Response findRuleByID(@PathParam("id_document") String documentId, JsonNode select) {
        addRequestId();
        try {
            ParametersChecker.checkParameter("formatId is a mandatory parameter", documentId);
            SanityChecker.checkParameter(documentId);
            try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
                final JsonNode result = client.getRuleByID(documentId);
                return Response.status(Status.OK).entity(result).build();
            } catch (final ReferentialException e) {
                LOGGER.error(e);
                final Status status = Status.INTERNAL_SERVER_ERROR;
                return Response.status(status).entity(getErrorEntity(status, e.getMessage(), null)).build();
            } catch (final InvalidParseOperationException e) {
                LOGGER.error(e);
                final Status status = Status.BAD_REQUEST;
                return Response.status(status).entity(getErrorEntity(status, e.getMessage(), null)).build();
            }
        } catch (final IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error(e);
            return Response.status(Status.PRECONDITION_FAILED)
                .entity(getErrorEntity(Status.PRECONDITION_FAILED, e.getMessage(), null)).build();
        }
    }

    /**
     * findIngestContractsByID
     *
     * @param documentId the document id to find
     * @return Response
     */
    @Path("/entrycontracts/{id_document:.+}")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "entrycontracts:id:read", description = "Lire un contrat d'entrée donné")
    public Response findIngestContractsByID(@PathParam("id_document") String documentId, JsonNode select) {
        addRequestId();
        try {
            ParametersChecker.checkParameter("formatId is a mandatory parameter", documentId);
            SanityChecker.checkParameter(documentId);
            try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
                RequestResponse<IngestContractModel> requestResponse = client.findIngestContractsByID(documentId);
                int st = requestResponse.isOk() ? Status.OK.getStatusCode() : requestResponse.getHttpCode();
                return Response.status(st).entity(requestResponse).build();
            } catch (ReferentialNotFoundException e) {
                LOGGER.error(e);
                final Status status = Status.NOT_FOUND;
                return Response.status(status).entity(getErrorEntity(status, e.getMessage(), null)).build();
            } catch (final ReferentialException e) {
                LOGGER.error(e);
                final Status status = Status.INTERNAL_SERVER_ERROR;
                return Response.status(status).entity(getErrorEntity(status, e.getMessage(), null)).build();
            } catch (final InvalidParseOperationException e) {
                LOGGER.error(e);
                final Status status = Status.BAD_REQUEST;
                return Response.status(status).entity(getErrorEntity(status, e.getMessage(), null)).build();
            }
        } catch (final IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error(e);
            return Response.status(Status.PRECONDITION_FAILED)
                .entity(getErrorEntity(Status.PRECONDITION_FAILED, e.getMessage(), null)).build();
        }
    }

    /**
     * findAccessContractsByID
     *
     * @param documentId the document id to find
     * @return Response
     */
    @Path("/accesscontracts/{id_document:.+}")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "accesscontracts:id:read", description = "Lire un contrat d'accès donné")
    public Response findAccessContractsByID(@PathParam("id_document") String documentId, JsonNode select) {
        addRequestId();
        try {
            ParametersChecker.checkParameter("formatId is a mandatory parameter", documentId);
            SanityChecker.checkParameter(documentId);
            try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
                RequestResponse<AccessContractModel> requestResponse = client.findAccessContractsByID(documentId);
                int st = requestResponse.isOk() ? Status.OK.getStatusCode() : requestResponse.getHttpCode();
                return Response.status(st).entity(requestResponse).build();
            } catch (ReferentialNotFoundException e) {
                LOGGER.error(e);
                final Status status = Status.NOT_FOUND;
                return Response.status(status).entity(getErrorEntity(status, e.getMessage(), null)).build();
            } catch (final ReferentialException e) {
                LOGGER.error(e);
                final Status status = Status.INTERNAL_SERVER_ERROR;
                return Response.status(status).entity(getErrorEntity(status, e.getMessage(), null)).build();
            } catch (final InvalidParseOperationException e) {
                LOGGER.error(e);
                final Status status = Status.BAD_REQUEST;
                return Response.status(status).entity(getErrorEntity(status, e.getMessage(), null)).build();
            }
        } catch (final IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error(e);
            return Response.status(Status.PRECONDITION_FAILED)
                .entity(getErrorEntity(Status.PRECONDITION_FAILED, e.getMessage(), null)).build();
        }
    }

    /**
     * findProfilesByID
     *
     * @param documentId the document id to find
     * @return Response
     */
    @Path("/profiles/{id_document:.+}")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "profiles:id:read:json", description = "Lire un profil donné")
    public Response findProfilesByID(@PathParam("id_document") String documentId, JsonNode select) {
        addRequestId();
        try {
            ParametersChecker.checkParameter("formatId is a mandatory parameter", documentId);
            SanityChecker.checkParameter(documentId);
            try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
                RequestResponse<ProfileModel> requestResponse = client.findProfilesByID(documentId);
                int st = requestResponse.isOk() ? Status.OK.getStatusCode() : requestResponse.getHttpCode();
                return Response.status(st).entity(requestResponse).build();
            } catch (ReferentialNotFoundException e) {
                LOGGER.error(e);
                final Status status = Status.NOT_FOUND;
                return Response.status(status).entity(getErrorEntity(status, e.getMessage(), null)).build();
            } catch (final ReferentialException e) {
                LOGGER.error(e);
                final Status status = Status.INTERNAL_SERVER_ERROR;
                return Response.status(status).entity(getErrorEntity(status, e.getMessage(), null)).build();
            } catch (final InvalidParseOperationException e) {
                LOGGER.error(e);
                final Status status = Status.BAD_REQUEST;
                return Response.status(status).entity(getErrorEntity(status, e.getMessage(), null)).build();
            }
        } catch (final IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error(e);
            return Response.status(Status.PRECONDITION_FAILED)
                .entity(getErrorEntity(Status.PRECONDITION_FAILED, e.getMessage(), null)).build();
        }
    }

    /**
     * findContextById
     *
     * @param documentId the document id to find
     * @return Response
     */
    @Path("/contexts/{id_document:.+}")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "contexts:id:read", description = "Lire un contexte donné")
    public Response findContextById(@PathParam("id_document") String documentId, JsonNode select) {
        addRequestId();
        try {
            ParametersChecker.checkParameter("formatId is a mandatory parameter", documentId);
            SanityChecker.checkParameter(documentId);
            try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
                RequestResponse<ContextModel> requestResponse = client.findContextById(documentId);
                int st = requestResponse.isOk() ? Status.OK.getStatusCode() : requestResponse.getHttpCode();
                return Response.status(st).entity(requestResponse).build();
            } catch (ReferentialNotFoundException e) {
                LOGGER.error(e);
                final Status status = Status.NOT_FOUND;
                return Response.status(status).entity(getErrorEntity(status, e.getMessage(), null)).build();
            } catch (final ReferentialException e) {
                LOGGER.error(e);
                final Status status = Status.INTERNAL_SERVER_ERROR;
                return Response.status(status).entity(getErrorEntity(status, e.getMessage(), null)).build();
            } catch (final InvalidParseOperationException e) {
                LOGGER.error(e);
                final Status status = Status.BAD_REQUEST;
                return Response.status(status).entity(getErrorEntity(status, e.getMessage(), null)).build();
            }
        } catch (final IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error(e);
            return Response.status(Status.PRECONDITION_FAILED)
                .entity(getErrorEntity(Status.PRECONDITION_FAILED, e.getMessage(), null)).build();
        }
    }

    /**
     * Update context
     *
     * @param id
     * @param queryDsl
     * @return Response
     * @throws AdminManagementClientServerException
     * @throws InvalidParseOperationException
     */
    @Path("/context/{id:.+}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "contexts:id:update", description = "Effectuer une mise à jour sur un contexte")
    public Response updateContext(@PathParam("id") String id, JsonNode queryDsl)
        throws AdminManagementClientServerException, InvalidParseOperationException {
        addRequestId();
        try {
            try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
                RequestResponse response = client.updateContext(id, queryDsl);
                return getResponse(response);
            }
        } catch (IllegalArgumentException e) {
            LOGGER.error(e);
            return Response.status(Status.PRECONDITION_FAILED)
                .entity(getErrorEntity(Status.PRECONDITION_FAILED, e.getMessage(), null)).build();
        }
    }

    /**
     * Update access contract
     *
     * @param id
     * @param queryDsl
     * @return Response
     * @throws AdminManagementClientServerException
     * @throws InvalidParseOperationException
     */
    @Path("/accesscontracts/{id:.+}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "accesscontracts:id:update", description = "Effectuer une mise à jour sur un contrat d'accès")
    public Response updateAccessContract(@PathParam("id") String id, JsonNode queryDsl)
        throws AdminManagementClientServerException, InvalidParseOperationException {
        addRequestId();
        try {
            try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
                RequestResponse response = client.updateAccessContract(id, queryDsl);
                return getResponse(response);
            }
        } catch (IllegalArgumentException e) {
            LOGGER.error(e);
            return Response.status(Status.PRECONDITION_FAILED)
                .entity(getErrorEntity(Status.PRECONDITION_FAILED, e.getMessage(), null)).build();
        }
    }

    /**
     * Update ingest contract
     *
     * @param id
     * @param queryDsl
     * @return Response
     * @throws AdminManagementClientServerException
     * @throws InvalidParseOperationException
     */
    @Path("/entrycontracts/{id:.+}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "entrycontracts:id:update", description = "Effectuer une mise à jour sur un contrat d'entrée")
    public Response updateIngestContract(@PathParam("id") String id, JsonNode queryDsl)
        throws AdminManagementClientServerException, InvalidParseOperationException {
        addRequestId();
        try {
            try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
                RequestResponse response = client.updateIngestContract(id, queryDsl);
                return getResponse(response);
            }
        } catch (IllegalArgumentException e) {
            LOGGER.error(e);
            return Response.status(Status.PRECONDITION_FAILED)
                .entity(getErrorEntity(Status.PRECONDITION_FAILED, e.getMessage(), null)).build();
        }
    }

    private Response getResponse(RequestResponse response) {
        if (response.isOk()) {
            return Response.status(Status.OK).entity(response).build();
        } else {
            final VitamError error = (VitamError) response;
            return Response.status(error.getHttpCode()).entity(response).build();
        }
    }

    /**
     * findDocumentByID
     *
     * @param documentId the document id to get
     * @return Response
     */
    @POST
    @Path(AccessExtAPI.ACCESSION_REGISTERS_API + "/{id_document}")
    @Produces(MediaType.APPLICATION_JSON)
    @Unsecured()
    public Response findAccessionRegisterById(@PathParam("id_document") String documentId) {
        // FIXME : Change @Unsecured to @Secured once implemented
        addRequestId();
        final Status status = Status.NOT_IMPLEMENTED;
        return Response.status(status).entity(getErrorEntity(status, status.getReasonPhrase(), null)).build();
    }


    /**
     * findAccessionRegisterDetail
     *
     * @param documentId the document id of accession register to get
     * @param select the query to get document
     * @return Response
     */
    @GET
    @Path(AccessExtAPI.ACCESSION_REGISTERS_API + "/{id_document}/accession-register-detail")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "accession-registers:id:accession-register-detail:read",
        description = "Lister les détails d'un registre de fonds")
    public Response findAccessionRegisterDetail(@PathParam("id_document") String documentId, JsonNode select) {
        addRequestId();

        ParametersChecker.checkParameter("accession register id is a mandatory parameter", documentId);
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            RequestResponse result =
                client.getAccessionRegisterDetail(documentId, select);
            int st = result.isOk() ? Status.OK.getStatusCode() : result.getHttpCode();

            return Response.status(st).entity(result).build();
        } catch (final ReferentialNotFoundException e) {
            return Response.status(Status.OK).entity(new RequestResponseOK().setHttpCode(Status.OK.getStatusCode()))
                .build();
        } catch (InvalidParseOperationException e) {
            LOGGER.error(e);
            final Status status = Status.BAD_REQUEST;
            return Response.status(status).entity(getErrorEntity(status, e.getMessage(), null)).build();
        } catch (Exception e) {
            LOGGER.error(e);
            final Status status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status).entity(getErrorEntity(status, e.getMessage(), null)).build();
        }
    }

    /**
     * Checks a traceability operation
     *
     * @param query the DSLQuery used to find the traceability operation to validate
     * @return The verification report == the logbookOperation
     */
    @POST
    @Path(AccessExtAPI.TRACEABILITY_API + "/check")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "traceability:check", description = "Tester l'intégrité d'un journal sécurisé")
    public Response checkOperationTraceability(JsonNode query) {

        try (AccessInternalClient client = AccessInternalClientFactory.getInstance().getClient()) {
            ParametersChecker.checkParameter("checks operation Logbook traceability parameters", query);

            addRequestId();
            RequestResponse<JsonNode> result = client.checkTraceabilityOperation(query);
            int st = result.isOk() ? Status.OK.getStatusCode() : result.getHttpCode();
            return Response.status(st).entity(result).build();
        } catch (final IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error(e);
            final Status status = Status.BAD_REQUEST;
            return Response.status(status).entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                .setContext(ServiceName.EXTERNAL_ACCESS.getName())
                .setState("code_vitam")
                .setMessage(status.getReasonPhrase())
                .setDescription(e.getMessage())).build();
        } catch (LogbookClientServerException e) {
            LOGGER.error(e);
            final Status status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status).entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                .setContext(ServiceName.EXTERNAL_ACCESS.getName())
                .setState("code_vitam")
                .setMessage(status.getReasonPhrase())
                .setDescription(e.getMessage())).build();
        } catch (AccessUnauthorizedException e) {
            LOGGER.error("Contract access does not allow ", e);
            final Status status = Status.UNAUTHORIZED;
            return Response.status(status).entity(getErrorEntity(status, e.getLocalizedMessage())).build();
        }
    }

    /**
     * launch Audit
     *
     * @param options
     * @return
     */
    @POST
    @Path(AccessExtAPI.AUDITS_API)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "audits:check", description = "Lancer un audit de l'existance des objets")
    public Response launchAudit(JsonNode options) {
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            addRequestId();
            client.launchAuditWorkflow(options);
        } catch (AdminManagementClientServerException e) {
            LOGGER.error(e);
            final Status status = Status.BAD_REQUEST;
            return Response.status(status).entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                .setContext(ServiceName.EXTERNAL_ACCESS.getName())
                .setState("code_vitam")
                .setMessage(status.getReasonPhrase())
                .setDescription(e.getMessage())).build();
        }
        return Response.status(Status.ACCEPTED).build();
    }

    private void addRequestId() {
        Integer tenantId = ParameterHelper.getTenantParameter();
        LOGGER.debug(String.format("tenant Id %d", tenantId));
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(tenantId));
    }

    /**
     * Construct the error following input
     *
     * @param status Http error status
     * @param message The functional error message, if absent the http reason phrase will be used instead
     * @param code The functional error code, if absent the http code will be used instead
     * @return
     */
    private VitamError getErrorEntity(Status status, String message, String code) {
        String aMessage =
            (message != null && !message.trim().isEmpty()) ? message
                : (status.getReasonPhrase() != null ? status.getReasonPhrase() : status.name());
        String aCode = (code != null) ? code : String.valueOf(status.getStatusCode());
        return new VitamError(aCode).setHttpCode(status.getStatusCode()).setContext(ACCESS_EXTERNAL_MODULE)
            .setState(CODE_VITAM).setMessage(status.getReasonPhrase()).setDescription(aMessage);
    }


    private InputStream getErrorStream(VitamError vitamError) {
        try {
            return JsonHandler.writeToInpustream(vitamError);
        } catch (InvalidParseOperationException e) {
            return new ByteArrayInputStream("{ 'message' : 'Invalid VitamError message' }".getBytes());
        }
    }

    private InputStream getErrorStream(Status status, String message, String code) {
        String aMessage =
            (message != null && !message.trim().isEmpty()) ? message
                : (status.getReasonPhrase() != null ? status.getReasonPhrase() : status.name());
        String aCode = (code != null) ? code : String.valueOf(status.getStatusCode());
        try {
            return JsonHandler.writeToInpustream(new VitamError(aCode)
                .setHttpCode(status.getStatusCode()).setContext(ACCESS_EXTERNAL_MODULE)
                .setState(CODE_VITAM).setMessage(status.getReasonPhrase()).setDescription(aMessage));
        } catch (InvalidParseOperationException e) {
            return new ByteArrayInputStream("{ 'message' : 'Invalid VitamError message' }".getBytes());
        }
    }

    private VitamError getErrorEntity(Status status, String message) {
        String aMessage =
            (message != null && !message.trim().isEmpty()) ? message
                : (status.getReasonPhrase() != null ? status.getReasonPhrase() : status.name());

        return new VitamError(status.name()).setHttpCode(status.getStatusCode()).setContext(ACCESS_EXTERNAL_MODULE)
            .setState(CODE_VITAM).setMessage(status.getReasonPhrase()).setDescription(aMessage);
    }
}
