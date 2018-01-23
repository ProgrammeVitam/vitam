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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.Produces;
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
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.alert.AlertService;
import fr.gouv.vitam.common.alert.AlertServiceImpl;
import fr.gouv.vitam.common.client.IngestCollection;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Update;
import fr.gouv.vitam.common.database.index.model.IndexationResult;
import fr.gouv.vitam.common.database.parameter.IndexParameters;
import fr.gouv.vitam.common.database.parameter.SwitchIndexParameters;
import fr.gouv.vitam.common.database.parser.request.single.UpdateParserSingle;
import fr.gouv.vitam.common.dsl.schema.Dsl;
import fr.gouv.vitam.common.dsl.schema.DslSchema;
import fr.gouv.vitam.common.error.ServiceName;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.AccessUnauthorizedException;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InternalServerException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamThreadAccessException;
import fr.gouv.vitam.common.exception.WorkflowNotFoundException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessQuery;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.model.administration.AgenciesModel;
import fr.gouv.vitam.common.model.administration.ContextModel;
import fr.gouv.vitam.common.model.administration.FileFormatModel;
import fr.gouv.vitam.common.model.administration.IngestContractModel;
import fr.gouv.vitam.common.model.administration.ProfileModel;
import fr.gouv.vitam.common.model.administration.SecurityProfileModel;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.security.rest.EndpointInfo;
import fr.gouv.vitam.common.security.rest.SecureEndpointRegistry;
import fr.gouv.vitam.common.security.rest.Secured;
import fr.gouv.vitam.common.security.rest.Unsecured;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.stream.VitamAsyncInputStreamResponse;
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
import fr.gouv.vitam.ingest.internal.client.IngestInternalClient;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClientFactory;
import fr.gouv.vitam.ingest.internal.common.exception.IngestInternalClientNotFoundException;
import fr.gouv.vitam.ingest.internal.common.exception.IngestInternalClientServerException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;

/**
 * Admin Management External Resource
 */
@Path("/admin-external/v1")
@javax.ws.rs.ApplicationPath("webresources")
public class AdminManagementExternalResource extends ApplicationStatusResource {

    private static final String IDENTIFIER = "Identifier";
    private static final String ATTACHEMENT_FILENAME = "attachment; filename=rapport.json";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AdminManagementExternalResource.class);
    private static final String ACCESS_EXTERNAL_MODULE = "ADMIN_EXTERNAL";
    // Should be replaced in code by a real code from VitamCode
    @Deprecated
    private static final String CODE_VITAM = "code_vitam";
    private static final String HTML_CONTENT_MSG_ERROR = "document has toxic HTML content";

    private final SecureEndpointRegistry secureEndpointRegistry;
    private static final AlertService alertService = new AlertServiceImpl();

    /**
     * Constructor
     * 
     * @param secureEndpointRegistry endpoint list registry
     */
    public AdminManagementExternalResource(SecureEndpointRegistry secureEndpointRegistry) {
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

        String resourcePath = AdminManagementExternalResource.class.getAnnotation(Path.class).value();

        List<EndpointInfo> securedEndpointList = this.secureEndpointRegistry.getEndPointsByResourcePath(resourcePath);

        return Response.status(Status.OK).entity(securedEndpointList).build();
    }

    /**
     * checkFormat
     *
     * @param document the document to check
     * @return Response
     */
    @Path("/formatsfilecheck")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Secured(permission = "formatsfile:check",
        description = "Vérifier si le référentiel des formats que l'on souhaite importer est valide")
    public Response checkDocument(InputStream document) {
        Integer tenantId = ParameterHelper.getTenantParameter();
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(tenantId));
        addRequestId();

        ParametersChecker.checkParameter("xmlPronom is a mandatory parameter", document);
        return asyncCheckFormat(document);
    }

    /**
     * checkRules
     *
     * @param document the document to check
     * @return Response
     */
    @Path("/rulesfilecheck")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Secured(permission = "rulesfile:check",
        description = "Vérifier si le référentiel de règles de gestions que l'on souhaite importer est valide")
    public Response checkRules(InputStream document) {
        Integer tenantId = ParameterHelper.getTenantParameter();
        LOGGER.debug(String.format("tenant Id %d", tenantId));
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(tenantId));
        addRequestId();

        return asyncDownloadErrorReportRules(document);
    }


    /**
     * check agencies
     * 
     * @param document
     * @return
     */
    @Path("/agenciesfilecheck")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Secured(permission = "agenciesfile:check",
        description = "Vérifier si le référentiel de services producteurs que l'on souhaite importer est valide")
    public Response checkAgencies(InputStream document) {
        Integer tenantId = ParameterHelper.getTenantParameter();
        LOGGER.debug(String.format("tenant Id %d", tenantId));
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(tenantId));
        addRequestId();

        return asyncDownloadErrorReportAgencies(document);
    }

    /**
     * Check Format async call
     * 
     * @param document inputStream to check
     */
    private Response asyncCheckFormat(InputStream document) {
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            Response response = client.checkFormat(document);
            return new VitamAsyncInputStreamResponse(response);
        } catch (ReferentialException ex) {
            LOGGER.error(ex);
            StreamUtils.closeSilently(document);
            return Response.status(Status.BAD_REQUEST)
                .entity(getErrorStream(Status.BAD_REQUEST, ex.getMessage(), null).toString())
                .build();
        }
    }


    /**
     * Return the Error Report or close the asyncResonse
     * 
     * @param document the referential to check
     */
    private Response asyncDownloadErrorReportRules(InputStream document) {
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            final Response response = client.checkRulesFile(document);
            Map<String, String> headers = VitamAsyncInputStreamResponse.getDefaultMapFromResponse(response);
            headers.put(HttpHeaders.CONTENT_DISPOSITION, ATTACHEMENT_FILENAME);
            return new VitamAsyncInputStreamResponse(response,
                (Status) response.getStatusInfo(), headers);
        } catch (Exception e) {
            return asyncResponseResume(e, document);
        }
    }

    /**
     * Return the Error Report or close the asyncResonse
     *
     * @param document the referential to check
     */
    private Response asyncDownloadErrorReportAgencies(InputStream document) {
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            final Response response = client.checkAgenciesFile(document);
            Map<String, String> headers = VitamAsyncInputStreamResponse.getDefaultMapFromResponse(response);
            headers.put(HttpHeaders.CONTENT_DISPOSITION, ATTACHEMENT_FILENAME);
            return new VitamAsyncInputStreamResponse(response,
                (Status) response.getStatusInfo(), headers);
        } catch (Exception e) {
            return asyncResponseResume(e, document);
        }
    }

    /**
     * Resume in case of Exception
     * 
     * @param ex exception to handle
     * @param document the given document to import
     */
    private Response asyncResponseResume(Exception ex, InputStream document) {
        LOGGER.error(ex);
        StreamUtils.closeSilently(document);
        return Response.status(Status.INTERNAL_SERVER_ERROR)
            .entity(getErrorStream(Status.INTERNAL_SERVER_ERROR, ex.getMessage(), null).toString())
            .build();
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
        File file = PropertiesUtils.fileFromTmpFolder("tmpRuleFile");
        try {
            ParametersChecker.checkParameter("document is a mandatory parameter", document);

            // Check Html Pattern
            try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                StreamUtils.copy(document, fileOutputStream);
            }
            SanityChecker.checkHTMLFile(file);

            try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
                Status status = client.importRulesFile((InputStream) new FileInputStream(file), filename);

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
        } catch (final IllegalArgumentException | IOException e) {
            LOGGER.error(e);
            final Status status = Status.BAD_REQUEST;
            return Response.status(status).entity(getErrorEntity(status, e.getMessage(), null)).build();
        } catch (InvalidParseOperationException e) {
            alertService.createAlert("Rules contain an HTML injection");
            LOGGER.error(e);
            final Status status = Status.BAD_REQUEST;
            return Response.status(status).entity(getErrorEntity(status, HTML_CONTENT_MSG_ERROR, null)).build();
        } finally {
            file.delete();
            StreamUtils.closeSilently(document);
        }
    }

    private Response downloadTraceabilityOperationFile(String operationId) {
        try (AccessInternalClient client = AccessInternalClientFactory.getInstance().getClient()) {
            final Response response = client.downloadTraceabilityFile(operationId);
            Map<String, String> headers = VitamAsyncInputStreamResponse.getDefaultMapFromResponse(response);
            return new VitamAsyncInputStreamResponse(response,
                Status.OK, headers);
        } catch (final InvalidParseOperationException | IllegalArgumentException exc) {
            LOGGER.error(exc);
            return Response.status(Status.PRECONDITION_FAILED)
                .entity(getErrorStream(Status.PRECONDITION_FAILED, exc.getMessage(), null)).build();
        } catch (final AccessInternalClientServerException exc) {
            LOGGER.error(exc.getMessage(), exc);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(getErrorStream(Status.INTERNAL_SERVER_ERROR, exc.getMessage(), null)).build();
        } catch (final AccessInternalClientNotFoundException exc) {
            LOGGER.error(exc.getMessage(), exc);
            return Response.status(Status.NOT_FOUND)
                .entity(getErrorStream(Status.NOT_FOUND, exc.getMessage(), null)).build();
        } catch (AccessUnauthorizedException e) {
            LOGGER.error("Contract access does not allow ", e);
            return Response.status(Status.UNAUTHORIZED)
                .entity(getErrorStream(Status.UNAUTHORIZED, e.getMessage(), null)).build();
        }
    }


    /**
     * Import a set of ingest contracts.
     *
     * @param select the select query to find document
     * @return Response
     */
    @Path("/ingestcontracts")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "ingestcontracts:create:json",
        description = "Importer des contrats d'entrées dans le référentiel")
    public Response importIngestContracts(JsonNode select)
        throws DatabaseConflictException {

        addRequestId();
        ParametersChecker.checkParameter("Json select is a mandatory parameter", select);
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            Status status = client.importIngestContracts(JsonHandler.getFromStringAsTypeRefence(select.toString(),
                new TypeReference<List<IngestContractModel>>() {}));

            if (Status.BAD_REQUEST.getStatusCode() == status.getStatusCode()) {
                return Response.status(Status.BAD_REQUEST)
                    .entity(getErrorEntity(Status.BAD_REQUEST, Status.BAD_REQUEST.getReasonPhrase(), null)).build();
            }

            // Send the http response with the entity and the status got from internalService;
            ResponseBuilder ResponseBuilder = Response.status(status)
                .entity("Successfully imported");
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
            Status status = client.importAccessContracts(JsonHandler.getFromStringAsTypeRefence(select.toString(),
                new TypeReference<List<AccessContractModel>>() {}));

            if (Status.BAD_REQUEST.getStatusCode() == status.getStatusCode()) {
                return Response.status(Status.BAD_REQUEST)
                    .entity(getErrorEntity(Status.BAD_REQUEST, Status.BAD_REQUEST.getReasonPhrase(), null)).build();
            }

            // Send the http response with the entity and the status got from internalService;
            ResponseBuilder ResponseBuilder = Response.status(status)
                .entity("Successfully imported");
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
            LOGGER.error(e);
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
            LOGGER.error(e);
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
    @Secured(permission = "profiles:id:update:binaire", description = "Importer un fichier xsd ou rng dans un profil")
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
     */
    @GET
    @Path("/profiles/{id:.+}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Secured(permission = "profiles:id:read:binary",
        description = "Télecharger le fichier xsd ou rng attaché à un profil")
    public Response downloadProfileFile(
        @PathParam("id") String fileId) {

        ParametersChecker.checkParameter("Profile id should be filled", fileId);
        addRequestId();
        return asyncDownloadProfileFile(fileId);
    }

    /**
     * Download the traceability file<br/>
     * <br/>
     * <b>The caller is responsible to close the Response after consuming the inputStream.</b>
     *
     * @param fileId
     */
    @GET
    @Path("/traceability/{id}/datafiles")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Secured(permission = "traceability:id:read",
        description = "Télécharger le logbook sécurisé attaché à une opération de sécurisation")
    public Response downloadTraceabilityFile(
        @PathParam("id") String fileId) {

        try {
            ParametersChecker.checkParameter("Traceability operation should be filled", fileId);

            addRequestId();

            return downloadTraceabilityOperationFile(fileId);
        } catch (IllegalArgumentException | VitamThreadAccessException e) {
            LOGGER.error(e);
            return Response.status(Status.PRECONDITION_FAILED)
                .entity(getErrorStream(Status.PRECONDITION_FAILED, e.getMessage(), null))
                .build();
        }
    }

    private Response asyncDownloadProfileFile(String profileMetadataId) {
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            final Response response = client.downloadProfileFile(profileMetadataId);
            Map<String, String> headers = VitamAsyncInputStreamResponse.getDefaultMapFromResponse(response);
            return new VitamAsyncInputStreamResponse(response,
                Status.OK, headers);
        } catch (final ProfileNotFoundException exc) {
            LOGGER.error(exc.getMessage(), exc);
            return Response.status(Status.NOT_FOUND)
                .entity(getErrorStream(Status.NOT_FOUND, exc.getMessage(), null).toString()).build();
        } catch (final AdminManagementClientServerException exc) {
            LOGGER.error(exc.getMessage(), exc);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(getErrorStream(Status.INTERNAL_SERVER_ERROR, exc.getMessage(), null).toString())
                .build();
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
    public Response getFormats(@Dsl(value = DslSchema.SELECT_SINGLE) JsonNode select) {

        addRequestId();
        try {
            try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
                SanityChecker.checkJsonAll(select);
                final RequestResponse<FileFormatModel> result = client.getFormats(select);
                int st = result.isOk() ? Status.OK.getStatusCode() : result.getHttpCode();
                return Response.status(st).entity(result).build();
            } catch (ReferentialNotFoundException | FileRulesNotFoundException e) {
                LOGGER.error(e);
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
    public Response getRules(@Dsl(value = DslSchema.SELECT_SINGLE) JsonNode select) {

        addRequestId();
        try {
            try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
                SanityChecker.checkJsonAll(select);
                final JsonNode result = client.getRules(select);
                return Response.status(Status.OK).entity(result).build();
            } catch (FileRulesNotFoundException e) {
                LOGGER.error(e);
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
    @Path("/ingestcontracts")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "ingestcontracts:read",
        description = "Lister le contenu du référentiel des contrats d'entrée")
    public Response findIngestContracts(@Dsl(value = DslSchema.SELECT_SINGLE) JsonNode select) {

        addRequestId();
        try {
            try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
                SanityChecker.checkJsonAll(select);
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
    public Response findAccessContracts(@Dsl(value = DslSchema.SELECT_SINGLE) JsonNode select) {

        addRequestId();
        try {
            try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
                SanityChecker.checkJsonAll(select);
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
    public Response findProfiles(@Dsl(value = DslSchema.SELECT_SINGLE) JsonNode select) {

        addRequestId();
        try {
            try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
                SanityChecker.checkJsonAll(select);
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
    public Response findContexts(@Dsl(value = DslSchema.SELECT_SINGLE) JsonNode select) {

        addRequestId();
        try {
            try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
                SanityChecker.checkJsonAll(select);
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
     * Import a agencies document
     *
     * @param headers http headers
     * @param document inputStream representing the data to import
     * @return The jaxRs Response
     * @throws InvalidParseOperationException
     */
    @Path("/agencies")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "agencies:create", description = "Importer un référentiel des services producteurs")
    public Response importAgenciesFile(@Context HttpHeaders headers,
        InputStream document) {
        Integer tenantId = ParameterHelper.getTenantParameter();
        String filename = headers.getHeaderString(GlobalDataRest.X_FILENAME);
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(tenantId));
        File file = PropertiesUtils.fileFromTmpFolder("tmpRuleFile");
        try {
            ParametersChecker.checkParameter("document is a mandatory parameter", document);

            // Check Html Pattern
            try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                StreamUtils.copy(document, fileOutputStream);
            }
            SanityChecker.checkHTMLFile(file);

            try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
                Status status = client.importAgenciesFile((InputStream) new FileInputStream(file), filename);

                // Send the http response with no entity and the status got from internalService;
                ResponseBuilder ResponseBuilder = Response.status(status);
                return ResponseBuilder.build();
            } catch (final ReferentialException e) {
                LOGGER.error(e);
                return Response.status(Status.BAD_REQUEST)
                    .entity(getErrorEntity(Status.BAD_REQUEST, e.getMessage(), null)).build();
            }
        } catch (final IllegalArgumentException | IOException e) {
            LOGGER.error(e);
            final Status status = Status.BAD_REQUEST;
            return Response.status(status).entity(getErrorEntity(status, e.getMessage(), null)).build();
        } catch (InvalidParseOperationException e) {
            alertService.createAlert("Agencies contain an HTML injection");
            LOGGER.error(e);
            final Status status = Status.BAD_REQUEST;
            return Response.status(status).entity(getErrorEntity(status, HTML_CONTENT_MSG_ERROR, null)).build();
        } finally {
            file.delete();
            StreamUtils.closeSilently(document);
        }
    }

    /**
     * findRuleByID
     *
     * @param documentId the document id to find
     * @return Response
     */
    @Path("/agencies/{id_document:.+}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "agencies:id:read", description = "Trouver un service producteur avec son identifier")
    public Response findAgencyByID(@PathParam("id_document") String documentId) {
        addRequestId();

        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            ParametersChecker.checkParameter("documentId is a mandatory parameter", documentId);
            SanityChecker.checkParameter(documentId);
            final RequestResponse<AgenciesModel> requestResponse = client.getAgencyById(documentId);
            int st = requestResponse.isOk() ? Status.OK.getStatusCode() : requestResponse.getHttpCode();
            return Response.status(st).entity(requestResponse).build();
        } catch (ReferentialNotFoundException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_NOT_FOUND, e.getMessage())
                .toResponse();
        } catch (final ReferentialException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_INTERNAL_SERVER_ERROR, e.getMessage())
                .toResponse();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_BAD_REQUEST, e.getMessage())
                .toResponse();
        } catch (final IllegalArgumentException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_PRECONDITION_FAILED, e.getMessage())
                .toResponse();
        }

    }


    /**
     * findContexts using get method
     *
     * @param select the select query to find document
     * @return Response
     */
    @Path("/agencies")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "agencies:read", description = "Lister le contenu du référentiel des services producteurs")
    public Response findAgencies(@Dsl(value = DslSchema.SELECT_SINGLE) JsonNode select) throws IOException {

        addRequestId();
        try {
            try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
                SanityChecker.checkJsonAll(select);
                JsonNode result = client.getAgencies(select);
                return Response.status(Status.OK).entity(result).build();
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
    @Path(AccessExtAPI.ACCESSION_REGISTERS_API)
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = AccessExtAPI.ACCESSION_REGISTERS + ":read",
        description = "Lister le contenu du référentiel des registres des fonds")
    public Response getAccessionRegister(@Dsl(value = DslSchema.SELECT_SINGLE) JsonNode select) {

        addRequestId();
        try {
            try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
                SanityChecker.checkJsonAll(select);
                final RequestResponse result = client.getAccessionRegister(select);
                int st = result.isOk() ? Status.OK.getStatusCode() : result.getHttpCode();
                return Response.status(st).entity(result).build();
            } catch (ReferentialNotFoundException | FileRulesNotFoundException e) {
                LOGGER.error(e);
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
     * findFormatByID
     *
     * @param documentId the document id to find
     * @return Response
     */
    @Path("/formats/{id_document:.+}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "formats:id:read", description = "Lire un format donné")
    public Response findFormatByID(@PathParam("id_document") String documentId) {
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
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "rules:id:read", description = "Lire une règle de gestion donnée")
    public Response findRuleByID(@PathParam("id_document") String documentId) {
        addRequestId();
        try {
            ParametersChecker.checkParameter("formatId is a mandatory parameter", documentId);
            SanityChecker.checkParameter(documentId);
            try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
                final JsonNode result = client.getRuleByID(documentId);
                return Response.status(Status.OK).entity(result).build();
            } catch (final FileRulesNotFoundException e) {
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
     * findIngestContractsByID
     *
     * @param documentId the document id to find
     * @return Response
     */
    @Path("/ingestcontracts/{id_document:.+}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "ingestcontracts:id:read", description = "Lire un contrat d'entrée donné")
    public Response findIngestContractsByID(@PathParam("id_document") String documentId) {
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
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "accesscontracts:id:read", description = "Lire un contrat d'accès donné")
    public Response findAccessContractsByID(@PathParam("id_document") String documentId) {
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
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "profiles:id:read:json", description = "Lire un profil donné")
    public Response findProfilesByID(@PathParam("id_document") String documentId) {
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
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "contexts:id:read", description = "Lire un contexte donné")
    public Response findContextById(@PathParam("id_document") String documentId) {
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
     * @param identifier
     * @param queryDsl
     * @return Response
     * @throws AdminManagementClientServerException
     * @throws InvalidParseOperationException
     */
    @Path("/contexts/{identifier:.+}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "contexts:id:update", description = "Effectuer une mise à jour sur un contexte")
    public Response updateContext(@PathParam("identifier") String identifier,
        @Dsl(DslSchema.UPDATE_BY_ID) JsonNode queryDsl)
        throws AdminManagementClientServerException, InvalidParseOperationException {
        addRequestId();
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            UpdateParserSingle updateParserSingle = new UpdateParserSingle();
            updateParserSingle.parse(queryDsl);
            Update update = updateParserSingle.getRequest();
            update.setQuery(QueryHelper.eq(IDENTIFIER, identifier));
            RequestResponse response = client.updateContext(identifier, update.getFinalUpdate());
            return getResponse(response);
        } catch (ReferentialNotFoundException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_CONTEXT_NOT_FOUND, e.getMessage())
                .setHttpCode(Status.NOT_FOUND.getStatusCode())
                .toResponse();
        } catch (InvalidCreateOperationException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_UPDATE_CONTEXT_ERROR, e.getMessage())
                .toResponse();
        } catch (IllegalArgumentException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_UPDATE_CONTEXT_ERROR, e.getMessage())
                .setHttpCode(Status.PRECONDITION_FAILED.getStatusCode()).toResponse();
        }
    }

    /**
     * Update context
     *
     * @param identifier
     * @param queryDsl
     * @return Response
     * @throws AdminManagementClientServerException
     * @throws InvalidParseOperationException
     */
    @Path("/profiles/{identifier:.+}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "profiles:id:update:json", description = "Effectuer une mise à jour sur un profil")
    public Response updateProfile(@PathParam("identifier") String identifier,
        @Dsl(DslSchema.UPDATE_BY_ID) JsonNode queryDsl)
        throws AdminManagementClientServerException, InvalidParseOperationException {
        addRequestId();
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            UpdateParserSingle updateParserSingle = new UpdateParserSingle();
            updateParserSingle.parse(queryDsl);
            Update update = updateParserSingle.getRequest();
            update.setQuery(QueryHelper.eq(IDENTIFIER, identifier));
            RequestResponse response = client.updateProfile(identifier, update.getFinalUpdate());
            return getResponse(response);
        } catch (ReferentialNotFoundException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_PROFILE_NOT_FOUND, e.getMessage())
                .setHttpCode(Status.NOT_FOUND.getStatusCode())
                .toResponse();
        } catch (InvalidCreateOperationException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_UPDATE_PROFILE_ERROR, e.getMessage())
                .toResponse();
        } catch (IllegalArgumentException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_UPDATE_PROFILE_ERROR, e.getMessage())
                .setHttpCode(Status.PRECONDITION_FAILED.getStatusCode()).toResponse();
        }
    }

    /**
     * Update access contract
     *
     * @param identifier
     * @param queryDsl
     * @return Response
     * @throws AdminManagementClientServerException
     * @throws InvalidParseOperationException
     */
    @Path("/accesscontracts/{identifier:.+}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "accesscontracts:id:update", description = "Effectuer une mise à jour sur un contrat d'accès")
    public Response updateAccessContract(@PathParam("identifier") String identifier,
        @Dsl(DslSchema.UPDATE_BY_ID) JsonNode queryDsl)
        throws AdminManagementClientServerException, InvalidParseOperationException {
        addRequestId();
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            UpdateParserSingle updateParserSingle = new UpdateParserSingle();
            updateParserSingle.parse(queryDsl);
            Update update = updateParserSingle.getRequest();
            update.setQuery(QueryHelper.eq(IDENTIFIER, identifier));
            RequestResponse response = client.updateAccessContract(identifier, update.getFinalUpdate());
            return getResponse(response);
        } catch (ReferentialNotFoundException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.CONTRACT_NOT_FOUND_ERROR, e.getMessage())
                .setHttpCode(Status.NOT_FOUND.getStatusCode())
                .toResponse();
        } catch (InvalidCreateOperationException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_UPDATE_ACCESS_CONTRACT_ERROR, e.getMessage())
                .toResponse();
        } catch (IllegalArgumentException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_UPDATE_ACCESS_CONTRACT_ERROR, e.getMessage())
                .setHttpCode(Status.PRECONDITION_FAILED.getStatusCode())
                .toResponse();
        }
    }

    /**
     * Update ingest contract
     *
     * @param identifier
     * @param queryDsl
     * @return Response
     * @throws AdminManagementClientServerException
     * @throws InvalidParseOperationException
     */
    @Path("/ingestcontracts/{identifier:.+}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "ingestcontracts:id:update",
        description = "Effectuer une mise à jour sur un contrat d'entrée")
    public Response updateIngestContract(@PathParam("identifier") String identifier,
        @Dsl(DslSchema.UPDATE_BY_ID) JsonNode queryDsl)
        throws AdminManagementClientServerException, InvalidParseOperationException {
        addRequestId();
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            UpdateParserSingle updateParserSingle = new UpdateParserSingle();
            updateParserSingle.parse(queryDsl);
            Update update = updateParserSingle.getRequest();
            update.setQuery(QueryHelper.eq(IDENTIFIER, identifier));
            RequestResponse response = client.updateIngestContract(identifier, update.getFinalUpdate());
            return getResponse(response);
        } catch (ReferentialNotFoundException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.CONTRACT_NOT_FOUND_ERROR, e.getMessage())
                .setHttpCode(Status.NOT_FOUND.getStatusCode())
                .toResponse();
        } catch (InvalidCreateOperationException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_UPDATE_INGEST_CONTRACT_ERROR, e.getMessage())
                .toResponse();
        } catch (IllegalArgumentException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_UPDATE_INGEST_CONTRACT_ERROR, e.getMessage())
                .setHttpCode(Status.PRECONDITION_FAILED.getStatusCode())
                .toResponse();
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
     * findAccessionRegisterDetail
     *
     * @param documentId the document id of accession register to get
     * @param select the query to get document
     * @return Response
     */
    @GET
    @Path(AccessExtAPI.ACCESSION_REGISTERS_API + "/{id_document}/" + AccessExtAPI.ACCESSION_REGISTERS_DETAIL)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = AccessExtAPI.ACCESSION_REGISTERS + ":id:" + AccessExtAPI.ACCESSION_REGISTERS_DETAIL + ":read",
        description = "Lister les détails d'un registre de fonds")
    public Response findAccessionRegisterDetail(@PathParam("id_document") String documentId,
        @Dsl(value = DslSchema.SELECT_SINGLE) JsonNode select) {
        addRequestId();

        ParametersChecker.checkParameter("accession register id is a mandatory parameter", documentId);
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            SanityChecker.checkJsonAll(select);
            RequestResponse result =
                client.getAccessionRegisterDetail(documentId, select);
            int st = result.isOk() ? Status.OK.getStatusCode() : result.getHttpCode();

            return Response.status(st).entity(result).build();
        } catch (final ReferentialNotFoundException e) {
            LOGGER.warn(e);
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
    @Path(AccessExtAPI.TRACEABILITY_API + "checks")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "traceabilitychecks:create", description = "Tester l'intégrité d'un journal sécurisé")
    public Response checkOperationTraceability(@Dsl(value = DslSchema.SELECT_SINGLE) JsonNode query) {

        try (AccessInternalClient client = AccessInternalClientFactory.getInstance().getClient()) {
            ParametersChecker.checkParameter("checks operation Logbook traceability parameters", query);
            SanityChecker.checkJsonAll(query);
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
     * @return response
     */
    @POST
    @Path(AccessExtAPI.AUDITS_API)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "audits:create", description = "Lancer un audit de l'existance des objets")
    public Response launchAudit(JsonNode options) {
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            SanityChecker.checkJsonAll(options);
            addRequestId();
            RequestResponse<JsonNode> result = client.launchAuditWorkflow(options);
            int st = result.isOk() ? Status.OK.getStatusCode() : result.getHttpCode();
            return Response.status(st).entity(result).build();
        } catch (AdminManagementClientServerException | InvalidParseOperationException e) {
            LOGGER.error(e);
            final Status status = Status.BAD_REQUEST;
            return Response.status(status).entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                .setContext(ServiceName.EXTERNAL_ACCESS.getName())
                .setState("code_vitam")
                .setMessage(status.getReasonPhrase())
                .setDescription(e.getMessage())).build();
        }
    }

    /**
     * Import security profile documents
     *
     * @param document the security profile to import
     * @return Response
     */
    @Path("/securityprofiles")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "securityprofiles:create:json",
        description = "Importer des profiles de sécurité dans le référentiel")
    public Response importSecurityProfiles(JsonNode document)
        throws DatabaseConflictException {

        addRequestId();
        ParametersChecker.checkParameter("Json document is a mandatory parameter", document);
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            SanityChecker.checkJsonAll(document);
            Status status = client.importSecurityProfiles(JsonHandler.getFromStringAsTypeRefence(document.toString(),
                new TypeReference<List<SecurityProfileModel>>() {}));

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
    }

    /**
     * Find security profiles using get method
     *
     * @param select the select query to find document
     * @return Response
     */
    @Path("/securityprofiles")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "securityprofiles:read",
        description = "Lister le contenu du référentiel des profiles de sécurité")
    public Response findSecurityProfiles(@Dsl(value = DslSchema.SELECT_SINGLE) JsonNode select) {

        addRequestId();
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            SanityChecker.checkJsonAll(select);
            RequestResponse<SecurityProfileModel> result = client.findSecurityProfiles(select);
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
        } catch (IllegalArgumentException e) {
            LOGGER.error(e);
            final Status status = Status.PRECONDITION_FAILED;
            return Response.status(status).entity(getErrorEntity(status, e.getMessage(), null)).build();
        }
    }

    /**
     * Find security profile by identifier
     *
     * @param identifier the identifier of the security profile to find
     * @return Response
     */
    @Path("/securityprofiles/{identifier}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "securityprofiles:id:read", description = "Lire un profile de sécurité donné")
    public Response findSecurityProfileByIdentifier(@PathParam("identifier") String identifier) {
        addRequestId();
        try {
            ParametersChecker.checkParameter("identifier is a mandatory parameter", identifier);
            try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
                RequestResponse<SecurityProfileModel> requestResponse =
                    client.findSecurityProfileByIdentifier(identifier);
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
        } catch (final IllegalArgumentException e) {
            LOGGER.error(e);
            return Response.status(Status.PRECONDITION_FAILED)
                .entity(getErrorEntity(Status.PRECONDITION_FAILED, e.getMessage(), null)).build();
        }
    }

    /**
     * Update a security profile
     *
     * @param identifier the identifier of the security profile to update
     * @param queryDsl query to execute
     * @return Response
     * @throws AdminManagementClientServerException
     * @throws InvalidParseOperationException
     */
    @Path("/securityprofiles/{identifier}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "securityprofiles:id:update",
        description = "Effectuer une mise à jour sur un profil de sécurité")
    public Response updateSecurityProfile(@PathParam("identifier") String identifier,
        @Dsl(DslSchema.UPDATE_BY_ID) JsonNode queryDsl)
        throws AdminManagementClientServerException, InvalidParseOperationException {
        addRequestId();
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            UpdateParserSingle updateParserSingle = new UpdateParserSingle();
            updateParserSingle.parse(queryDsl);
            Update update = updateParserSingle.getRequest();
            update.setQuery(QueryHelper.eq(IDENTIFIER, identifier));
            RequestResponse response = client.updateSecurityProfile(identifier, update.getFinalUpdateById());
            return getResponse(response);
        } catch (ReferentialNotFoundException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_SECURITY_PROFILE_NOT_FOUND, e.getMessage())
                .setHttpCode(Status.PRECONDITION_FAILED.getStatusCode())
                .toResponse();
        } catch (InvalidCreateOperationException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_UPDATE_SECURITY_PROFILE_ERROR, e.getMessage())
                .toResponse();
        } catch (IllegalArgumentException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_UPDATE_SECURITY_PROFILE_ERROR, e.getMessage())
                .setHttpCode(Status.PRECONDITION_FAILED.getStatusCode())
                .toResponse();
        }
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
    @Deprecated
    private VitamError getErrorEntity(Status status, String message, String code) {
        String aMessage =
            (message != null && !message.trim().isEmpty()) ? message
                : (status.getReasonPhrase() != null ? status.getReasonPhrase() : status.name());
        String aCode = (code != null) ? code : String.valueOf(status.getStatusCode());
        return new VitamError(aCode).setHttpCode(status.getStatusCode()).setContext(ACCESS_EXTERNAL_MODULE)
            .setState(CODE_VITAM).setMessage(status.getReasonPhrase()).setDescription(aMessage);
    }

    @Deprecated
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
            LOGGER.error(e);
            return new ByteArrayInputStream("{ 'message' : 'Invalid VitamError message' }".getBytes());
        }
    }

    @Deprecated
    private VitamError getErrorEntity(Status status, String message) {
        String aMessage =
            (message != null && !message.trim().isEmpty()) ? message
                : (status.getReasonPhrase() != null ? status.getReasonPhrase() : status.name());

        return new VitamError(status.name()).setHttpCode(status.getStatusCode()).setContext(ACCESS_EXTERNAL_MODULE)
            .setState(CODE_VITAM).setMessage(status.getReasonPhrase()).setDescription(aMessage);
    }



    /**
     * @param headers the http header of request
     * @param query the filter query
     * @return the list of Operations details
     */
    @GET
    @Path("/operations")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "operations:read", description = "Récupérer les informations sur une opération donnée")
    public Response listOperationsDetails(@Context HttpHeaders headers, ProcessQuery query) {
        try (IngestInternalClient client = IngestInternalClientFactory.getInstance().getClient()) {
            return client.listOperationsDetails(query).toResponse();
        } catch (VitamClientException e) {
            LOGGER.error("Unexpected error was thrown : " + e.getMessage(), e);
            return Response.serverError()
                .entity(
                    VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_CLIENT_ERROR,
                        e.getLocalizedMessage()))
                .build();
        }
    }

    /**
     * @param id operation identifier
     * @return http response
     */
    @Path("operations/{id}")
    @HEAD
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "operations:id:read:status", description = "Récupérer le code HTTP d'une opération donnée")
    public Response getWorkFlowExecutionStatus(@PathParam("id") String id) {
        try (IngestInternalClient ingestInternalClient = IngestInternalClientFactory.getInstance().getClient()) {
            SanityChecker.checkParameter(id);
            final ItemStatus itemStatus = ingestInternalClient.getOperationProcessStatus(id);

            Response.ResponseBuilder builder = Response.status(Status.ACCEPTED);
            if (ProcessState.COMPLETED.equals(itemStatus.getGlobalState())) {
                builder.status(Status.OK);
            } else {
                builder.status(Status.ACCEPTED);
            }

            return builder
                .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATE, itemStatus.getGlobalState())
                .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATUS, itemStatus.getGlobalStatus())
                .header(GlobalDataRest.X_CONTEXT_ID, itemStatus.getLogbookTypeProcess())
                .build();

        } catch (final IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error(e);
            return Response.status(Status.PRECONDITION_FAILED).build();
        } catch (final WorkflowNotFoundException e) {
            LOGGER.error(e);
            return Response.status(Status.NO_CONTENT).build();
        } catch (VitamClientException | InternalServerException e) {
            LOGGER.error(e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (BadRequestException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).build();
        }
    }

    /**
     * get the workflow status
     *
     * @param id operation identifier
     * @return http response
     */
    @Path("operations/{id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "operations:id:read", description = "Récupérer le statut d'une opération donnée")
    public Response getOperationProcessExecutionDetails(@PathParam("id") String id) {
        Status status;
        ItemStatus itemStatus = null;
        try (IngestInternalClient ingestInternalClient = IngestInternalClientFactory.getInstance().getClient()) {
            itemStatus = ingestInternalClient.getOperationProcessExecutionDetails(id);
            return new RequestResponseOK<ItemStatus>().addResult(itemStatus).setHttpCode(Status.OK.getStatusCode())
                .toResponse();
        } catch (final WorkflowNotFoundException e) {
            LOGGER.error("Workflow not found exception: ", e);
            status = Status.NOT_FOUND;
            return Response.status(status)
                .entity(VitamCodeHelper
                    .toVitamError(VitamCode.INGEST_EXTERNAL_NOT_FOUND, e.getLocalizedMessage())
                    .setHttpCode(status.getStatusCode()))
                .build();
        } catch (final IllegalArgumentException e) {
            LOGGER.error("Illegal argument: ", e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status)
                .entity(VitamCodeHelper
                    .toVitamError(VitamCode.INGEST_EXTERNAL_GET_OPERATION_PROCESS_DETAIL_ERROR, e.getLocalizedMessage())
                    .setHttpCode(status.getStatusCode()))
                .build();
        } catch (InternalServerException e) {
            LOGGER.error("Could get operation detail: ", e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(VitamCodeHelper
                    .toVitamError(VitamCode.INGEST_EXTERNAL_GET_OPERATION_PROCESS_DETAIL_ERROR, e.getLocalizedMessage())
                    .setHttpCode(status.getStatusCode()))
                .build();
        } catch (BadRequestException e) {
            LOGGER.error("Request invalid while trying to get operation detail: ", e);
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(VitamCodeHelper
                    .toVitamError(VitamCode.INGEST_EXTERNAL_GET_OPERATION_PROCESS_DETAIL_ERROR, e.getLocalizedMessage())
                    .setHttpCode(status.getStatusCode()))
                .build();
        } catch (VitamClientException e) {
            LOGGER.error("Unexpected error was thrown : " + e.getMessage(), e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(VitamCodeHelper
                    .toVitamError(VitamCode.INGEST_EXTERNAL_GET_OPERATION_PROCESS_DETAIL_ERROR, e.getLocalizedMessage())
                    .setHttpCode(status.getStatusCode()))
                .build();
        }
    }

    /**
     * Update the status of an operation.
     *
     * @param headers contain X-Action and X-Context-ID
     * @param id operation identifier
     * @return http response
     */
    @Path("operations/{id}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "operations:id:update", description = "Changer le statut d'une opération donnée")
    public Response updateWorkFlowStatus(@Context HttpHeaders headers, @PathParam("id") String id) {
        ParametersChecker.checkParameter("ACTION Request must not be null",
            headers.getRequestHeader(GlobalDataRest.X_ACTION));

        final String xAction = headers.getRequestHeader(GlobalDataRest.X_ACTION).get(0);
        return updateOperationActionProcessAsync(id, xAction);
    }

    private Response updateOperationActionProcessAsync(String operationId, String action) {

        try (IngestInternalClient ingestInternalClient = IngestInternalClientFactory.getInstance().getClient()) {
            VitamThreadUtils.getVitamSession().setRequestId(operationId);
            RequestResponse<ItemStatus> itemStatusRequestResponse =
                ingestInternalClient.updateOperationActionProcess(action, operationId);
            return itemStatusRequestResponse.toResponse();
        } catch (final ProcessingException e) {
            LOGGER.error("Unauthorized action for update ", e);
            return VitamCodeHelper
                .toVitamError(VitamCode.INGEST_EXTERNAL_UNAUTHORIZED, e.getLocalizedMessage()).toResponse();
        } catch (InternalServerException e) {
            LOGGER.error("Could not update operation process ", e);
            return VitamCodeHelper
                .toVitamError(VitamCode.INGEST_EXTERNAL_INTERNAL_SERVER_ERROR, e.getLocalizedMessage()).toResponse();
        } catch (VitamClientException e) {
            LOGGER.error("Client exception while trying to update operation process ", e);
            return VitamCodeHelper
                .toVitamError(VitamCode.INGEST_EXTERNAL_INTERNAL_CLIENT_ERROR, e.getLocalizedMessage())
                .toResponse();
        } catch (BadRequestException e) {
            LOGGER.error("Request invalid while trying to update operation process ", e);
            return VitamCodeHelper.toVitamError(VitamCode.INGEST_EXTERNAL_BAD_REQUEST, e.getLocalizedMessage())
                .toResponse();
        }
    }

    /**
     * Interrupt the process of an operation identified by Id.
     *
     * @param id operation identifier
     * @return http response
     */
    @Path("operations/{id}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "operations:id:delete", description = "Annuler une opération donnée")
    public Response interruptWorkFlowExecution(@PathParam("id") String id) {

        ParametersChecker.checkParameter("operationId must not be null", id);
        VitamError vitamError;
        try (IngestInternalClient ingestInternalClient = IngestInternalClientFactory.getInstance().getClient()) {
            SanityChecker.checkParameter(id);
            VitamThreadUtils.getVitamSession().setRequestId(id);

            final ItemStatus itemStatus = ingestInternalClient.cancelOperationProcessExecution(id);
            return new RequestResponseOK<ItemStatus>().addResult(itemStatus).setHttpCode(Status.OK.getStatusCode())
                .toResponse();
        } catch (final IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error("Illegal argument: ", e);
            vitamError =
                VitamCodeHelper.toVitamError(VitamCode.INGEST_EXTERNAL_ILLEGAL_ARGUMENT, e.getLocalizedMessage());
        } catch (WorkflowNotFoundException e) {
            LOGGER.error("Cound not find workflow: ", e);
            vitamError = VitamCodeHelper.toVitamError(VitamCode.INGEST_EXTERNAL_NOT_FOUND, e.getLocalizedMessage());
        } catch (InternalServerException e) {
            LOGGER.error("Cound not cancel operation: ", e);
            vitamError =
                VitamCodeHelper.toVitamError(VitamCode.INGEST_EXTERNAL_INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        } catch (BadRequestException e) {
            LOGGER.error("Request invalid while trying to cancel operation: ", e);
            vitamError = VitamCodeHelper.toVitamError(VitamCode.INGEST_EXTERNAL_UNAUTHORIZED, e.getLocalizedMessage());
        } catch (VitamClientException e) {
            LOGGER.error("Client exception while trying to cancel operation: ", e);
            vitamError =
                VitamCodeHelper.toVitamError(VitamCode.INGEST_EXTERNAL_INTERNAL_CLIENT_ERROR, e.getLocalizedMessage());
        }

        return Response.status(vitamError.getHttpCode()).entity(vitamError).build();
    }

    /**
     * @param headers the http header of request
     * @return Response
     */
    @GET
    @Path("/workflows")
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "workflows:read", description = "Récupérer la liste des tâches des workflows")
    public Response getWorkflowDefinitions(@Context HttpHeaders headers) {
        try (IngestInternalClient client = IngestInternalClientFactory.getInstance().getClient()) {
            return client.getWorkflowDefinitions().toResponse();
        } catch (VitamClientException e) {
            LOGGER.error("Unexpected error was thrown : " + e.getMessage(), e);
            return Response.serverError()
                .entity(VitamCodeHelper.toVitamError(VitamCode.INGEST_EXTERNAL_INTERNAL_CLIENT_ERROR,
                    e.getLocalizedMessage()))
                .build();
        }
    }

    /**
     * Download report stored by Administration operation (currently administration reports )
     * <p>
     * Return the report as stream asynchronously<br/>
     * <br/>
     * <b>The caller is responsible to close the Response after consuming the inputStream.</b>
     *
     * @param opId the id of logbook operation
     * @return the given response with the report
     */
    @GET
    @Path("/rulesreport/{opId}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Secured(permission = "rulesreport:id:read",
        description = "Récupérer le rapport pour une opération d'import de règles de gestion")
    public Response downloadRulesReportAsStream(@PathParam("opId") String opId) {
        return downloadObjectAsync(opId, IngestCollection.RULES);
    }

    private Response downloadObjectAsync(String objectId, IngestCollection collection) {
        try (IngestInternalClient ingestInternalClient = IngestInternalClientFactory.getInstance().getClient()) {
            final Response response = ingestInternalClient.downloadObjectAsync(objectId, collection);
            return new VitamAsyncInputStreamResponse(response);
        } catch (IllegalArgumentException e) {
            LOGGER.error("IllegalArgumentException was thrown : ", e);
            return Response.status(Status.BAD_REQUEST)
                .entity(getErrorStream(
                    VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_BAD_REQUEST, e.getLocalizedMessage())))
                .build();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error("Predicates Failed Exception", e);
            return Response.status(Status.PRECONDITION_FAILED)
                .entity(getErrorStream(
                    VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_PRECONDITION_FAILED,
                        e.getLocalizedMessage())))
                .build();
        } catch (final IngestInternalClientServerException e) {
            LOGGER.error("Internal Server Exception ", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(getErrorStream(
                    VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_INTERNAL_SERVER_ERROR,
                        e.getLocalizedMessage())))
                .build();
        } catch (final IngestInternalClientNotFoundException e) {
            LOGGER.error("Request resources does not exits", e);
            return Response.status(Status.NOT_FOUND)
                .entity(getErrorStream(
                    VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_NOT_FOUND, e.getLocalizedMessage())))
                .build();
        }
    }

    private InputStream getErrorStream(VitamError vitamError) {
        try {
            return JsonHandler.writeToInpustream(vitamError);
        } catch (InvalidParseOperationException e) {
            return new ByteArrayInputStream("{ 'message' : 'Invalid VitamError message' }".getBytes());
        }
    }

    private static final String REINDEX_URI = "/reindex";
    private static final String ALIASES_URI = "/alias";

    /**
     * Reindex a collection
     *
     * @param indexParameters parameters specifying what to reindex
     * @return Response response
     */
    @Path(REINDEX_URI)
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "reindex:create", description = "Reindexer une collection")
    public Response reindex(@Valid List<IndexParameters> indexParameters) {
        ParametersChecker.checkParameter("mandatory parameter", indexParameters);
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            RequestResponse<IndexationResult> result =
                client.launchReindexation(JsonHandler.toJsonNode(indexParameters));
            int st = result.isOk() ? Status.OK.getStatusCode() : result.getHttpCode();
            return Response.status(st).entity(result).build();
        } catch (AdminManagementClientServerException | InvalidParseOperationException e) {
            LOGGER.error(e);
            final Status status = Status.BAD_REQUEST;
            return Response.status(status).entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                .setContext(ServiceName.EXTERNAL_ACCESS.getName())
                .setState("code_vitam")
                .setMessage(status.getReasonPhrase())
                .setDescription(e.getMessage())).build();
        }

    }

    /**
     * Reindex a collection
     *
     * @param indexParameters parameters specifying what to reindex
     * @return Response response
     */
    @Path(ALIASES_URI)
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = "switchindex:create", description = "Switch indexes")
    public Response switchIndexes(@Valid List<SwitchIndexParameters> indexParameters) {
        ParametersChecker.checkParameter("mandatory parameter", indexParameters);
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            RequestResponse<IndexationResult> result = client.switchIndexes(JsonHandler.toJsonNode(indexParameters));
            int st = result.isOk() ? Status.OK.getStatusCode() : result.getHttpCode();
            return Response.status(st).entity(result).build();
        } catch (AdminManagementClientServerException | InvalidParseOperationException e) {
            LOGGER.error(e);
            final Status status = Status.BAD_REQUEST;
            return Response.status(status).entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                .setContext(ServiceName.EXTERNAL_ACCESS.getName())
                .setState("code_vitam")
                .setMessage(status.getReasonPhrase())
                .setDescription(e.getMessage())).build();
        }

    }
}
