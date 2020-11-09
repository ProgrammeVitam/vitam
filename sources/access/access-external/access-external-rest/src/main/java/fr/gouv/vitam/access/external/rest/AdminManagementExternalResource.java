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
package fr.gouv.vitam.access.external.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.access.external.api.AccessExtAPI;
import fr.gouv.vitam.access.internal.client.AccessInternalClient;
import fr.gouv.vitam.access.internal.client.AccessInternalClientFactory;
import fr.gouv.vitam.access.internal.common.exception.AccessInternalClientNotFoundException;
import fr.gouv.vitam.access.internal.common.exception.AccessInternalClientServerException;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.alert.AlertService;
import fr.gouv.vitam.common.alert.AlertServiceImpl;
import fr.gouv.vitam.common.client.IngestCollection;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Update;
import fr.gouv.vitam.common.database.parser.request.single.UpdateParserSingle;
import fr.gouv.vitam.common.dsl.schema.Dsl;
import fr.gouv.vitam.common.dsl.schema.DslSchema;
import fr.gouv.vitam.common.dsl.schema.ValidationException;
import fr.gouv.vitam.common.dsl.schema.validator.BatchProcessingQuerySchemaValidator;
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
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.AuditOptions;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProbativeValueRequest;
import fr.gouv.vitam.common.model.ProcessPause;
import fr.gouv.vitam.common.model.ProcessQuery;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseError;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.model.administration.AgenciesModel;
import fr.gouv.vitam.common.model.administration.ArchiveUnitProfileModel;
import fr.gouv.vitam.common.model.administration.ContextModel;
import fr.gouv.vitam.common.model.administration.FileFormatModel;
import fr.gouv.vitam.common.model.administration.IngestContractModel;
import fr.gouv.vitam.common.model.administration.ManagementContractModel;
import fr.gouv.vitam.common.model.administration.OntologyModel;
import fr.gouv.vitam.common.model.administration.ProfileModel;
import fr.gouv.vitam.common.model.administration.SecurityProfileModel;
import fr.gouv.vitam.common.model.administration.preservation.GriffinModel;
import fr.gouv.vitam.common.model.administration.preservation.PreservationScenarioModel;
import fr.gouv.vitam.common.model.audit.AuditReferentialOptions;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.security.rest.EndpointInfo;
import fr.gouv.vitam.common.security.rest.SecureEndpointRegistry;
import fr.gouv.vitam.common.security.rest.Secured;
import fr.gouv.vitam.common.security.rest.Unsecured;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.common.server.application.resources.VitamStatusService;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.stream.VitamAsyncInputStreamResponse;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientBadRequestException;
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
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.HeaderParam;
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
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import static fr.gouv.vitam.access.external.api.AccessExtAPI.RECTIFICATION_AUDIT;
import static fr.gouv.vitam.common.ParametersChecker.checkParameter;
import static fr.gouv.vitam.common.dsl.schema.DslSchema.SELECT_SINGLE;
import static fr.gouv.vitam.common.error.VitamCode.ACCESS_EXTERNAL_GET_ACCESSION_REGISTER_SYMBOLIC_ERROR;
import static fr.gouv.vitam.common.error.VitamCodeHelper.getCode;
import static fr.gouv.vitam.common.json.JsonHandler.getFromStringAsTypeReference;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.*;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

@Path("/admin-external/v1")
@ApplicationPath("webresources")
@Tag(name = "Admin")
public class AdminManagementExternalResource extends ApplicationStatusResource {

    private static final String IDENTIFIER = "Identifier";
    private static final String ATTACHEMENT_FILENAME = "attachment; filename=rapport.json";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AdminManagementExternalResource.class);
    private static final String ACCESS_EXTERNAL_MODULE = "ADMIN_EXTERNAL";
    // Should be replaced in code by a real code from VitamCode
    @Deprecated
    private static final String CODE_VITAM = "code_vitam";
    private static final String HTML_CONTENT_MSG_ERROR = "document has toxic HTML content";

    private static final String DOCUMENT_IS_MANDATORY = "document is a mandatory parameter";
    private static final String JSON_SELECT_IS_MANDATORY = "Json select is a mandatory parameter";
    private static final String SUCCESSFULLY_IMPORTED = "Successfully imported";
    private static final String FORMAT_ID_MANDATORY = "formatId is a mandatory parameter";
    private static final String UNEXPECTED_ERROR = "Unexpected error was thrown : ";

    private final SecureEndpointRegistry secureEndpointRegistry;
    private static final AlertService alertService = new AlertServiceImpl();

    private final AdminManagementClientFactory adminManagementClientFactory;
    private final IngestInternalClientFactory ingestInternalClientFactory;
    private final AccessInternalClientFactory accessInternalClientFactory;

    /**
     * Constructor
     *
     * @param secureEndpointRegistry endpoint list registry
     */
    AdminManagementExternalResource(SecureEndpointRegistry secureEndpointRegistry) {
        this.secureEndpointRegistry = secureEndpointRegistry;
        LOGGER.debug("init Admin Management Resource server");
        adminManagementClientFactory = AdminManagementClientFactory.getInstance();
        this.ingestInternalClientFactory = IngestInternalClientFactory.getInstance();
        this.accessInternalClientFactory = AccessInternalClientFactory.getInstance();
    }

    @VisibleForTesting
    AdminManagementExternalResource(
        VitamStatusService statusService,
        SecureEndpointRegistry secureEndpointRegistry,
        AdminManagementClientFactory adminManagementClientFactory,
        IngestInternalClientFactory ingestInternalClientFactory,
        AccessInternalClientFactory accessInternalClientFactory) {
        super(statusService);
        this.secureEndpointRegistry = secureEndpointRegistry;
        this.adminManagementClientFactory = adminManagementClientFactory;
        this.ingestInternalClientFactory = ingestInternalClientFactory;
        this.accessInternalClientFactory = accessInternalClientFactory;
    }

    /**
     * List secured resource end points
     */
    @Path("/")
    @OPTIONS
    @Produces(APPLICATION_JSON)
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
    @Secured(permission = FORMATSFILE_CHECK, isAdminOnly = true,
        description = "Vérifier si le référentiel des formats que l'on souhaite importer est valide")
    public Response checkDocument(InputStream document) {

        checkParameter("xmlPronom is a mandatory parameter", document);
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
    @Secured(permission = RULESFILE_CHECK,
        description = "Vérifier si le référentiel de règles de gestions que l'on souhaite importer est valide")
    public Response checkRules(InputStream document) {
        try {
            return asyncDownloadErrorReportRules(document);
        } catch (InvalidParseOperationException e) {
            return Response.status(INTERNAL_SERVER_ERROR).build();
        }
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
    @Secured(permission = AGENCIESFILE_CHECK,
        description = "Vérifier si le référentiel de services producteurs que l'on souhaite importer est valide")
    public Response checkAgencies(InputStream document) {
        return asyncDownloadErrorReportAgencies(document);
    }

    /**
     * Check Format async call
     *
     * @param document inputStream to check
     */
    private Response asyncCheckFormat(InputStream document) {
        try (AdminManagementClient client = adminManagementClientFactory.getClient()) {
            Response response = client.checkFormat(document);
            return new VitamAsyncInputStreamResponse(response);
        } catch (ReferentialException e) {
            LOGGER.error(e);
            StreamUtils.closeSilently(document);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_BAD_REQUEST, e.getMessage())
                .toResponse();
        }
    }


    /**
     * Return the Error Report or close the asyncResonse
     *
     * @param document the referential to check
     */
    private Response asyncDownloadErrorReportRules(InputStream document) throws InvalidParseOperationException {
        try (AdminManagementClient client = adminManagementClientFactory.getClient()) {
            final Response response = client.checkRulesFile(document);
            Map<String, String> headers = VitamAsyncInputStreamResponse.getDefaultMapFromResponse(response);
            headers.put(HttpHeaders.CONTENT_DISPOSITION, ATTACHEMENT_FILENAME);
            return new VitamAsyncInputStreamResponse(response,
                (Status) response.getStatusInfo(), headers);
        } catch (AdminManagementClientBadRequestException e) {
            VitamError error = new VitamError(VitamCode.ADMIN_EXTERNAL_BAD_REQUEST.getItem())
                .setMessage(VitamCode.ADMIN_EXTERNAL_BAD_REQUEST.getMessage())
                .setState(StatusCode.KO.name())
                .setCode(VitamCodeHelper.getCode(VitamCode.ADMIN_EXTERNAL_BAD_REQUEST))
                .setContext(ACCESS_EXTERNAL_MODULE)
                .setDescription(VitamCode.ADMIN_EXTERNAL_BAD_REQUEST.getMessage());
            return Response.status(BAD_REQUEST)
                .entity(JsonHandler.writeToInpustream(error)).build();
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
        try (AdminManagementClient client = adminManagementClientFactory.getClient()) {
            final Response response = client.checkAgenciesFile(document);
            Map<String, String> headers = VitamAsyncInputStreamResponse.getDefaultMapFromResponse(response);
            headers.put(HttpHeaders.CONTENT_DISPOSITION, ATTACHEMENT_FILENAME);
            return new VitamAsyncInputStreamResponse(response,
                (Status) response.getStatusInfo(), headers);
        } catch (AdminManagementClientBadRequestException e) {
            VitamError error = new VitamError(VitamCode.ADMIN_EXTERNAL_BAD_REQUEST.getItem())
                .setMessage(VitamCode.ADMIN_EXTERNAL_BAD_REQUEST.getMessage())
                .setState(StatusCode.KO.name())
                .setCode(VitamCodeHelper.getCode(VitamCode.ADMIN_EXTERNAL_BAD_REQUEST))
                .setContext(ACCESS_EXTERNAL_MODULE)
                .setDescription(VitamCode.ADMIN_EXTERNAL_BAD_REQUEST.getMessage());
            return Response.status(BAD_REQUEST)
                .entity(getErrorStream(error)).build();
        } catch (Exception e) {
            return asyncResponseResume(e, document);
        }
    }

    /**
     * Resume in case of Exception
     *
     * @param e exception to handle
     * @param document the given document to import
     */
    private Response asyncResponseResume(Exception e, InputStream document) {
        LOGGER.error(e);
        StreamUtils.closeSilently(document);
        return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_INTERNAL_SERVER_ERROR, e.getMessage())
            .toStreamResponse();
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
    @Produces(APPLICATION_JSON)
    @Secured(permission = FORMATS_CREATE, description = "Importer un référentiel des formats", isAdminOnly = true)
    public Response importFormat(@Context HttpHeaders headers, @Context UriInfo uriInfo, InputStream document) {
        String filename = headers.getHeaderString(GlobalDataRest.X_FILENAME);
        try (AdminManagementClient client = adminManagementClientFactory.getClient()) {
            checkParameter(DOCUMENT_IS_MANDATORY, document);
            Status status = client.importFormat(document, filename);

            return Response.status(status).build();
        } catch (final DatabaseConflictException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_CONFLICT, e.getMessage())
                .toResponse();
        } catch (final FileRulesImportInProgressException e) {
            LOGGER.warn(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_FORBIDDEN, e.getMessage())
                .toResponse();
        } catch (IllegalArgumentException | ReferentialException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_BAD_REQUEST, e.getMessage())
                .toResponse();
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
    @Produces(APPLICATION_JSON)
    @Secured(permission = RULES_CREATE, description = "Importer un référentiel des règles de gestion")
    public Response importRulesFile(@Context HttpHeaders headers, InputStream document) {
        String filename = headers.getHeaderString(GlobalDataRest.X_FILENAME);
        File file = PropertiesUtils.fileFromTmpFolder("tmpRuleFile");
        try {
            checkParameter(DOCUMENT_IS_MANDATORY, document);

            // Check Html Pattern
            try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                StreamUtils.copy(document, fileOutputStream);
            }
            SanityChecker.checkHTMLFile(file);

            return importRuleFile(filename, file);
        } catch (IllegalArgumentException | IOException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_BAD_REQUEST, e.getMessage())
                .toResponse();
        } catch (InvalidParseOperationException e) {
            alertService.createAlert("Rules contain an HTML injection");
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_BAD_REQUEST, HTML_CONTENT_MSG_ERROR)
                .toResponse();
        } finally {
            try {
                Files.delete(file.toPath());
            } catch (IOException e) {
                LOGGER.error(e);
            }
            StreamUtils.closeSilently(document);
        }
    }

    private Response importRuleFile(String filename, File file) throws IOException {
        try (AdminManagementClient client = adminManagementClientFactory.getClient();
            InputStream fileInputStream = new FileInputStream(file)) {
            Status status = client.importRulesFile(fileInputStream, filename);

            return Response.status(status).build();
        } catch (DatabaseConflictException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_CONFLICT, e.getMessage())
                .toResponse();
        } catch (FileRulesImportInProgressException e) {
            LOGGER.warn(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_FORBIDDEN, e.getMessage())
                .toResponse();
        } catch (ReferentialException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_BAD_REQUEST, e.getMessage())
                .toResponse();
        }
    }

    private Response downloadTraceabilityOperationFile(String operationId) {
        try (AccessInternalClient client = accessInternalClientFactory.getClient()) {
            final Response response = client.downloadTraceabilityFile(operationId);
            Map<String, String> headers = VitamAsyncInputStreamResponse.getDefaultMapFromResponse(response);
            return new VitamAsyncInputStreamResponse(response, Status.OK, headers);
        } catch (InvalidParseOperationException | IllegalArgumentException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_PRECONDITION_FAILED, e.getMessage())
                .toStreamResponse();
        } catch (AccessInternalClientServerException e) {
            LOGGER.error(e.getMessage(), e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_INTERNAL_SERVER_ERROR, e.getMessage())
                .toStreamResponse();
        } catch (AccessInternalClientNotFoundException e) {
            LOGGER.error(e.getMessage(), e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_NOT_FOUND, e.getMessage())
                .toStreamResponse();
        } catch (AccessUnauthorizedException e) {
            LOGGER.error("Contract access does not allow ", e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_UNAUTHORIZED, e.getMessage())
                .toStreamResponse();
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
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Secured(permission = INGESTCONTRACTS_CREATE_JSON,
        description = "Importer des contrats d'entrées dans le référentiel")
    public Response importIngestContracts(JsonNode select) {
        checkParameter(JSON_SELECT_IS_MANDATORY, select);
        try (AdminManagementClient client = adminManagementClientFactory.getClient()) {
            Status status = client.importIngestContracts(getFromStringAsTypeReference(select.toString(),
                new TypeReference<List<IngestContractModel>>() {
                }));

            if (BAD_REQUEST.getStatusCode() == status.getStatusCode()) {
                return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_BAD_REQUEST, BAD_REQUEST.getReasonPhrase())
                    .toResponse();
            }
            return Response.status(status)
                .entity(SUCCESSFULLY_IMPORTED)
                .build();
        } catch (final ReferentialException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_BAD_REQUEST, e.getMessage())
                .toResponse();
        } catch (InvalidParseOperationException | InvalidFormatException e) {
            LOGGER.error(e);
            VitamError error = new VitamError(VitamCode.ACCESS_EXTERNAL_INVALID_JSON.getItem())
                .setMessage(VitamCode.ACCESS_EXTERNAL_INVALID_JSON.getMessage())
                .setState(StatusCode.KO.name())
                .setCode(VitamCodeHelper.getCode(VitamCode.ACCESS_EXTERNAL_INVALID_JSON))
                .setContext(ACCESS_EXTERNAL_MODULE)
                .setDescription(VitamCode.ACCESS_EXTERNAL_INVALID_JSON.getMessage());
            return Response.status(BAD_REQUEST)
                .entity(error).build();
        }
    }

    /**
     * Import a set of access contracts.
     *
     * @param contract the input set of contracts as json
     * @return Response
     */
    @Path("/accesscontracts")
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Secured(permission = ACCESSCONTRACTS_CREATE_JSON,
        description = "Importer des contrats d'accès dans le référentiel")
    public Response importAccessContracts(JsonNode contract) {

        checkParameter(JSON_SELECT_IS_MANDATORY, contract);
        try (AdminManagementClient client = adminManagementClientFactory.getClient()) {
            Status status = client.importAccessContracts(getFromStringAsTypeReference(contract.toString(),
                new TypeReference<List<AccessContractModel>>() {
                }));

            if (BAD_REQUEST.getStatusCode() == status.getStatusCode()) {
                return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_BAD_REQUEST, BAD_REQUEST.getReasonPhrase())
                    .toResponse();
            }

            return Response.status(status)
                .entity(SUCCESSFULLY_IMPORTED).build();
        } catch (final ReferentialException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_BAD_REQUEST, e.getMessage())
                .toResponse();
        } catch (InvalidParseOperationException | InvalidFormatException e) {
            LOGGER.error(e);
            VitamError error = new VitamError(VitamCode.ACCESS_EXTERNAL_INVALID_JSON.getItem())
                .setMessage(VitamCode.ACCESS_EXTERNAL_INVALID_JSON.getMessage())
                .setState(StatusCode.KO.name())
                .setCode(VitamCodeHelper.getCode(VitamCode.ACCESS_EXTERNAL_INVALID_JSON))
                .setContext(ACCESS_EXTERNAL_MODULE)
                .setDescription(VitamCode.ACCESS_EXTERNAL_INVALID_JSON.getMessage());
            return Response.status(BAD_REQUEST)
                .entity(error).build();
        }
    }

    /**
     * Import a set of management contracts.
     *
     * @param contract the input set of contracts as json
     * @return Response
     */
    @Path(AccessExtAPI.MANAGEMENT_CONTRACT_API)
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Secured(permission = MANAGEMENTCONTRACTS_CREATE_JSON,
        description = "Importer des contrats de gestion dans le référentiel")
    public Response importManagementContracts(JsonNode contract) {

        checkParameter(JSON_SELECT_IS_MANDATORY, contract);
        try (AdminManagementClient client = adminManagementClientFactory.getClient()) {
            Status status = client.importManagementContracts(getFromStringAsTypeReference(contract.toString(),
                new TypeReference<List<ManagementContractModel>>() {
                }));

            if (BAD_REQUEST.getStatusCode() == status.getStatusCode()) {
                return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_BAD_REQUEST, BAD_REQUEST.getReasonPhrase())
                    .toResponse();
            }

            return Response.status(status)
                .entity(SUCCESSFULLY_IMPORTED).build();
        } catch (final ReferentialException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_BAD_REQUEST, e.getMessage())
                .toResponse();
        } catch (InvalidParseOperationException | InvalidFormatException e) {
            LOGGER.error(e);
            VitamError error = new VitamError(VitamCode.ACCESS_EXTERNAL_INVALID_JSON.getItem())
                .setMessage(VitamCode.ACCESS_EXTERNAL_INVALID_JSON.getMessage())
                .setState(StatusCode.KO.name())
                .setCode(VitamCodeHelper.getCode(VitamCode.ACCESS_EXTERNAL_INVALID_JSON))
                .setContext(ACCESS_EXTERNAL_MODULE)
                .setDescription(VitamCode.ACCESS_EXTERNAL_INVALID_JSON.getMessage());
            return Response.status(BAD_REQUEST)
                .entity(error).build();
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
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Secured(permission = CONTEXTS_CREATE_JSON,
        description = "Importer des contextes dans le référentiel", isAdminOnly = true)
    public Response importContexts(JsonNode select) {

        checkParameter(JSON_SELECT_IS_MANDATORY, select);
        try (AdminManagementClient client = adminManagementClientFactory.getClient()) {
            Status status = client.importContexts(getFromStringAsTypeReference(select.toString(),
                new TypeReference<>() {
                }));

            return Response.status(status).entity(SUCCESSFULLY_IMPORTED).build();
        } catch (ReferentialException | InvalidParseOperationException | InvalidFormatException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_BAD_REQUEST, e.getMessage())
                .toResponse();
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
    @Produces(APPLICATION_JSON)
    @Secured(permission = PROFILES_CREATE_BINARY, description = "Importer des profils dans le référentiel")
    public Response createProfiles(InputStream document) {

        try (AdminManagementClient client = adminManagementClientFactory.getClient()) {
            checkParameter(DOCUMENT_IS_MANDATORY, document);

            JsonNode json = JsonHandler.getFromInputStream(document);
            SanityChecker.checkJsonAll(json);
            RequestResponse requestResponse =
                client.createProfiles(getFromStringAsTypeReference(json.toString(),
                    new TypeReference<List<ProfileModel>>() {
                    }));
            return Response.status(requestResponse.getStatus())
                .entity(requestResponse).build();

        } catch (IllegalArgumentException | ReferentialException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_BAD_REQUEST, e.getMessage())
                .toResponse();
        } catch (InvalidParseOperationException e) {
            LOGGER.error(e);
            VitamError error = new VitamError(VitamCode.ADMIN_EXTERNAL_PRECONDITION_FAILED.getItem())
                .setMessage(VitamCode.ADMIN_EXTERNAL_PRECONDITION_FAILED.getMessage())
                .setState(StatusCode.KO.name())
                .setCode(VitamCodeHelper.getCode(VitamCode.ADMIN_EXTERNAL_PRECONDITION_FAILED))
                .setContext(ACCESS_EXTERNAL_MODULE)
                .setDescription(VitamCode.ADMIN_EXTERNAL_PRECONDITION_FAILED.getMessage());
            return Response.status(Status.PRECONDITION_FAILED)
                .entity(error).build();
        } catch (InvalidFormatException e) {
            LOGGER.error(e);
            VitamError error = new VitamError(VitamCode.ACCESS_EXTERNAL_INVALID_JSON.getItem())
                .setMessage(VitamCode.ACCESS_EXTERNAL_INVALID_JSON.getMessage())
                .setState(StatusCode.KO.name())
                .setCode(VitamCodeHelper.getCode(VitamCode.ACCESS_EXTERNAL_INVALID_JSON))
                .setContext(ACCESS_EXTERNAL_MODULE)
                .setDescription(VitamCode.ACCESS_EXTERNAL_INVALID_JSON.getMessage());
            return Response.status(BAD_REQUEST)
                .entity(error).build();

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
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Secured(permission = PROFILES_CREATE_JSON, description = "Ecrire un profil dans le référentiel")
    public Response createProfiles(JsonNode select) {

        checkParameter(JSON_SELECT_IS_MANDATORY, select);
        try (AdminManagementClient client = adminManagementClientFactory.getClient()) {
            SanityChecker.checkJsonAll(select);
            RequestResponse requestResponse =
                client.createProfiles(getFromStringAsTypeReference(select.toString(),
                    new TypeReference<List<ProfileModel>>() {
                    }));
            return Response.status(requestResponse.getStatus())
                .entity(requestResponse).build();

        } catch (ReferentialException | InvalidParseOperationException | InvalidFormatException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_BAD_REQUEST, e.getMessage())
                .toResponse();
        }
    }


    /**
     * Import archive unit profiles
     *
     * @param document inputStream representing the data to import
     * @return The jaxRs Response
     */
    @Path("/archiveunitprofiles")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(APPLICATION_JSON)
    @Secured(permission = ARCHIVEUNITPROFILES_CREATE_BINARY,
        description = "Importer un ou plusieurs document types dans le référentiel")
    public Response createArchiveUnitProfiles(InputStream document) {

        try (AdminManagementClient client = adminManagementClientFactory.getClient()) {
            checkParameter(DOCUMENT_IS_MANDATORY, document);

            JsonNode json = JsonHandler.getFromInputStream(document);
            SanityChecker.checkJsonAll(json);
            RequestResponse requestResponse =
                client.createArchiveUnitProfiles(getFromStringAsTypeReference(json.toString(),
                    new TypeReference<List<ArchiveUnitProfileModel>>() {
                    }));
            return Response.status(requestResponse.getStatus())
                .entity(requestResponse).build();

        } catch (IllegalArgumentException | ReferentialException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_BAD_REQUEST, e.getMessage())
                .toResponse();
        } catch (InvalidParseOperationException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_PRECONDITION_FAILED, e.getMessage())
                .setState(StatusCode.KO.name())
                .setContext(ACCESS_EXTERNAL_MODULE)
                .toResponse();
        } catch (InvalidFormatException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_INVALID_JSON, e.getMessage())
                .setState(StatusCode.KO.name())
                .toResponse();
        } finally {
            StreamUtils.closeSilently(document);
        }
    }


    /**
     * Import a set of archive unit profiles
     *
     * @param select the select query to find document
     * @return Response
     */
    @Path("/archiveunitprofiles")
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Secured(permission = ARCHIVEUNITPROFILES_CREATE_JSON,
        description = "Ecrire un ou plusieurs document type dans le référentiel")
    public Response createArchiveUnitProfiles(JsonNode select) {

        checkParameter(JSON_SELECT_IS_MANDATORY, select);
        try (AdminManagementClient client = adminManagementClientFactory.getClient()) {
            SanityChecker.checkJsonAll(select);
            RequestResponse requestResponse =
                client.createArchiveUnitProfiles(getFromStringAsTypeReference(select.toString(),
                    new TypeReference<List<ArchiveUnitProfileModel>>() {
                    }));
            return Response.status(requestResponse.getStatus())
                .entity(requestResponse).build();

        } catch (ReferentialException | InvalidParseOperationException | InvalidFormatException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_BAD_REQUEST, e.getMessage())
                .toResponse();
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
    @Produces(APPLICATION_JSON)
    @Secured(permission = PROFILES_ID_UPDATE_BINAIRE, description = "Importer un fichier xsd ou rng dans un profil")
    public Response importProfileFile(@Context UriInfo uriInfo, @PathParam("id") String profileMetadataId,
        InputStream profileFile) {
        try (AdminManagementClient client = adminManagementClientFactory.getClient()) {
            checkParameter("profileFile stream is a mandatory parameter", profileFile);
            checkParameter(profileMetadataId, "The profile id is mandatory");

            RequestResponse requestResponse = client.importProfileFile(profileMetadataId, profileFile);
            return Response.status(requestResponse.getStatus())
                .entity(requestResponse)
                .build();
        } catch (final DatabaseConflictException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_CONFLICT, e.getMessage())
                .toResponse();
        } catch (final ReferentialException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_BAD_REQUEST, e.getMessage())
                .toResponse();
        } catch (final IllegalArgumentException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_PRECONDITION_FAILED, e.getMessage())
                .toResponse();
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
    @Secured(permission = PROFILES_ID_READ_BINARY,
        description = "Télecharger le fichier xsd ou rng attaché à un profil")
    public Response downloadProfileFile(
        @PathParam("id") String fileId) {

        checkParameter("Profile id should be filled", fileId);
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
    @Secured(permission = TRACEABILITY_ID_READ,
        description = "Télécharger le logbook sécurisé attaché à une opération de sécurisation")
    public Response downloadTraceabilityFile(
        @PathParam("id") String fileId) {

        try {
            checkParameter("Traceability operation should be filled", fileId);
            return downloadTraceabilityOperationFile(fileId);
        } catch (IllegalArgumentException | VitamThreadAccessException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_PRECONDITION_FAILED, e.getMessage())
                .toStreamResponse();
        }
    }

    private Response asyncDownloadProfileFile(String profileMetadataId) {
        try (AdminManagementClient client = adminManagementClientFactory.getClient()) {
            final Response response = client.downloadProfileFile(profileMetadataId);
            Map<String, String> headers = VitamAsyncInputStreamResponse.getDefaultMapFromResponse(response);
            return new VitamAsyncInputStreamResponse(response,
                Status.OK, headers);
        } catch (ProfileNotFoundException e) {
            LOGGER.error(e.getMessage(), e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_NOT_FOUND, e.getMessage())
                .toStreamResponse();
        } catch (AdminManagementClientServerException e) {
            LOGGER.error(e.getMessage(), e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_INTERNAL_SERVER_ERROR, e.getMessage())
                .toStreamResponse();
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
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Secured(permission = FORMATS_READ, description = "Lister le contenu du référentiel des formats")
    public Response getFormats(@Dsl(value = SELECT_SINGLE) JsonNode select) {

        try (AdminManagementClient client = adminManagementClientFactory.getClient()) {
            SanityChecker.checkJsonAll(select);
            final RequestResponse<FileFormatModel> result = client.getFormats(select);
            int st = result.isOk() ? Status.OK.getStatusCode() : result.getHttpCode();
            return Response.status(st).entity(result).build();
        } catch (ReferentialNotFoundException | FileRulesNotFoundException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_NOT_FOUND, e.getMessage())
                .toResponse();
        } catch (ReferentialException | IOException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_INTERNAL_SERVER_ERROR, e.getMessage())
                .toResponse();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_BAD_REQUEST, e.getMessage())
                .toResponse();
        } catch (IllegalArgumentException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_PRECONDITION_FAILED, e.getMessage())
                .toResponse();
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
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Secured(permission = RULES_READ, description = "Lister le contenu du référentiel des règles de gestion")
    public Response getRules(@Dsl(value = SELECT_SINGLE) JsonNode select) {

        try (AdminManagementClient client = adminManagementClientFactory.getClient()) {
            SanityChecker.checkJsonAll(select);
            final JsonNode result = client.getRules(select);
            return Response.status(Status.OK).entity(result).build();
        } catch (FileRulesNotFoundException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_NOT_FOUND, e.getMessage())
                .toResponse();
        } catch (ReferentialException | IOException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_INTERNAL_SERVER_ERROR, e.getMessage())
                .toResponse();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_BAD_REQUEST, e.getMessage())
                .toResponse();
        } catch (IllegalArgumentException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_PRECONDITION_FAILED, e.getMessage())
                .toResponse();
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
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Secured(permission = INGESTCONTRACTS_READ,
        description = "Lister le contenu du référentiel des contrats d'entrée")
    public Response findIngestContracts(@Dsl(value = SELECT_SINGLE) JsonNode select) {

        try (AdminManagementClient client = adminManagementClientFactory.getClient()) {
            SanityChecker.checkJsonAll(select);
            RequestResponse<IngestContractModel> result = client.findIngestContracts(select);
            int st = result.isOk() ? Status.OK.getStatusCode() : result.getHttpCode();
            return Response.status(st).entity(result).build();
        } catch (ReferentialException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_INTERNAL_SERVER_ERROR, e.getMessage())
                .toResponse();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_BAD_REQUEST, e.getMessage())
                .toResponse();
        } catch (IllegalArgumentException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_PRECONDITION_FAILED, e.getMessage())
                .toResponse();
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
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Secured(permission = ACCESSCONTRACTS_READ, description = "Lister le contenu du référentiel des contrats d'accès")
    public Response findAccessContracts(@Dsl(value = SELECT_SINGLE) JsonNode select) {

        try (AdminManagementClient client = adminManagementClientFactory.getClient()) {
            SanityChecker.checkJsonAll(select);
            RequestResponse result = client.findAccessContracts(select);
            int st = result.isOk() ? Status.OK.getStatusCode() : result.getHttpCode();
            return Response.status(st).entity(result).build();
        } catch (ReferentialException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_INTERNAL_SERVER_ERROR, e.getMessage())
                .toResponse();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_BAD_REQUEST, e.getMessage())
                .toResponse();

        } catch (IllegalArgumentException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_PRECONDITION_FAILED, e.getMessage())
                .toResponse();
        }
    }

    /**
     * findManagementContracts using get method
     *
     * @param select the select query to find document
     * @return Response
     */
    @Path(AccessExtAPI.MANAGEMENT_CONTRACT_API)
    @GET
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Secured(permission = MANAGEMENTCONTRACTS_READ, description = "Lister le contenu du référentiel des contrats de gestion")
    public Response findManagementContracts(@Dsl(value = SELECT_SINGLE) JsonNode select) {

        try (AdminManagementClient client = adminManagementClientFactory.getClient()) {
            SanityChecker.checkJsonAll(select);
            RequestResponse result = client.findManagementContracts(select);
            int st = result.isOk() ? Status.OK.getStatusCode() : result.getHttpCode();
            return Response.status(st).entity(result).build();
        } catch (ReferentialException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_INTERNAL_SERVER_ERROR, e.getMessage())
                .toResponse();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_BAD_REQUEST, e.getMessage())
                .toResponse();

        } catch (IllegalArgumentException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_PRECONDITION_FAILED, e.getMessage())
                .toResponse();
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
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Secured(permission = PROFILES_READ, description = "Lister le contenu du référentiel des profils")
    public Response findProfiles(@Dsl(value = SELECT_SINGLE) JsonNode select) {

        try (AdminManagementClient client = adminManagementClientFactory.getClient()) {
            SanityChecker.checkJsonAll(select);
            RequestResponse result = client.findProfiles(select);
            int st = result.isOk() ? Status.OK.getStatusCode() : result.getHttpCode();
            return Response.status(st).entity(result).build();
        } catch (ReferentialException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_INTERNAL_SERVER_ERROR, e.getMessage())
                .toResponse();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_BAD_REQUEST, e.getMessage())
                .toResponse();

        } catch (IllegalArgumentException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_PRECONDITION_FAILED, e.getMessage())
                .toResponse();
        }
    }

    /**
     * find archive unit Profiles using get method
     *
     * @param select the select query to find document
     * @return Response
     */
    @Path("/archiveunitprofiles")
    @GET
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Secured(permission = ARCHIVEUNITPROFILES_READ,
        description = "Lister le contenu du référentiel des document types")
    public Response findArchiveUnitProfiles(@Dsl(value = SELECT_SINGLE) JsonNode select) {

        try (AdminManagementClient client = adminManagementClientFactory.getClient()) {
            SanityChecker.checkJsonAll(select);
            RequestResponse result = client.findArchiveUnitProfiles(select);
            int st = result.isOk() ? Status.OK.getStatusCode() : result.getHttpCode();
            return Response.status(st).entity(result).build();
        } catch (ReferentialException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_INTERNAL_SERVER_ERROR, e.getMessage())
                .toResponse();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_BAD_REQUEST, e.getMessage())
                .toResponse();
        } catch (IllegalArgumentException e) {
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
    @Path("/contexts")
    @GET
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Secured(permission = CONTEXTS_READ, description = "Lister le contenu du référentiel des contextes")
    public Response findContexts(@Dsl(value = SELECT_SINGLE) JsonNode select) {

        try (AdminManagementClient client = adminManagementClientFactory.getClient()) {
            SanityChecker.checkJsonAll(select);
            RequestResponse result = client.findContexts(select);
            int st = result.isOk() ? Status.OK.getStatusCode() : result.getHttpCode();
            return Response.status(st).entity(result).build();
        } catch (ReferentialException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_INTERNAL_SERVER_ERROR, e.getMessage())
                .toResponse();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_BAD_REQUEST, e.getMessage())
                .toResponse();
        } catch (IllegalArgumentException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_PRECONDITION_FAILED, e.getMessage())
                .toResponse();
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
    @Produces(APPLICATION_JSON)
    @Secured(permission = AGENCIES_CREATE, description = "Importer un référentiel des services producteurs")
    public Response importAgenciesFile(@Context HttpHeaders headers,
        InputStream document) {
        String filename = headers.getHeaderString(GlobalDataRest.X_FILENAME);
        File file = PropertiesUtils.fileFromTmpFolder("tmpRuleFile");
        try {
            checkParameter(DOCUMENT_IS_MANDATORY, document);

            // Check Html Pattern
            try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                StreamUtils.copy(document, fileOutputStream);
            }
            SanityChecker.checkHTMLFile(file);

            return importAgenciesFile(filename, file);
        } catch (final IllegalArgumentException | IOException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_BAD_REQUEST, e.getMessage())
                .toResponse();
        } catch (InvalidParseOperationException e) {
            alertService.createAlert("Agencies contain an HTML injection");
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_BAD_REQUEST, e.getMessage())
                .toResponse();
        } finally {
            try {
                Files.delete(file.toPath());
            } catch (IOException e) {
                LOGGER.error(e);
            }
            StreamUtils.closeSilently(document);
        }
    }

    private Response importAgenciesFile(String filename, File file) throws IOException {
        try (AdminManagementClient client = adminManagementClientFactory.getClient();
            InputStream fileInputStream = new FileInputStream(file)) {
            Status status = client.importAgenciesFile(fileInputStream, filename);

            return Response.status(status).build();
        } catch (final ReferentialException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_BAD_REQUEST, e.getMessage())
                .toResponse();
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
    @Produces(APPLICATION_JSON)
    @Secured(permission = AGENCIES_ID_READ, description = "Trouver un service producteur avec son identifier")
    public Response findAgencyByID(@PathParam("id_document") String documentId) {

        try (AdminManagementClient client = adminManagementClientFactory.getClient()) {
            checkParameter("documentId is a mandatory parameter", documentId);
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
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Secured(permission = AGENCIES_READ, description = "Lister le contenu du référentiel des services producteurs")
    public Response findAgencies(@Dsl(value = SELECT_SINGLE) JsonNode select) throws IOException {

        try (AdminManagementClient client = adminManagementClientFactory.getClient()) {
            SanityChecker.checkJsonAll(select);
            JsonNode result = client.getAgencies(select);
            return Response.status(Status.OK).entity(result).build();
        } catch (ReferentialException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_INTERNAL_SERVER_ERROR, e.getMessage())
                .toResponse();
        } catch (InvalidParseOperationException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_BAD_REQUEST, e.getMessage())
                .toResponse();
        } catch (IllegalArgumentException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_PRECONDITION_FAILED, e.getMessage())
                .toResponse();
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
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Secured(permission = ACCESSIONREGISTERS_READ,
            description = "Lister le contenu du référentiel des registres des fonds")
    public Response getAccessionRegister(@Dsl(value = SELECT_SINGLE) JsonNode select) {
        try (AdminManagementClient client = adminManagementClientFactory.getClient()) {
            SanityChecker.checkJsonAll(select);
            final RequestResponse result = client.getAccessionRegister(select);
            int st = result.isOk() ? Status.OK.getStatusCode() : result.getHttpCode();
            return Response.status(st).entity(result).build();
        } catch (ReferentialNotFoundException | FileRulesNotFoundException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_NOT_FOUND, e.getMessage())
                .toResponse();
        } catch (ReferentialException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_INTERNAL_SERVER_ERROR, e.getMessage())
                .toResponse();
        } catch (InvalidParseOperationException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_BAD_REQUEST, e.getMessage())
                .toResponse();
        } catch (AccessUnauthorizedException e) {
            LOGGER.error("Access contract does not allow ", e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_UNAUTHORIZED, e.getMessage())
                .toResponse();
        } catch (IllegalArgumentException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_PRECONDITION_FAILED, e.getMessage())
                .toResponse();
        }
    }

    /**
     * Retrieve accession register symbolic
     *
     * @param select the select query to find document
     * @return an accession register symbolic
     */
    @GET
    @Path("accessionregisterssymbolic")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Secured(permission = ACCESSIONREGISTERSSYMBOLIC_READ, description = "Get accession register symbolic")
    public Response getAccessionRegisterSymbolic(@Dsl(value = SELECT_SINGLE) JsonNode select) {
        try (AdminManagementClient client = adminManagementClientFactory.getClient()) {
            SanityChecker.checkJsonAll(select);
            Integer tenant = VitamThreadUtils.getVitamSession().getTenantId();
            RequestResponse result = client.getAccessionRegisterSymbolic(tenant, select);
            return Response.status(result.getHttpCode()).entity(result).build();
        } catch (Exception e) {
            return VitamCodeHelper.toVitamError(ACCESS_EXTERNAL_GET_ACCESSION_REGISTER_SYMBOLIC_ERROR, e.getMessage())
                .setHttpCode(ACCESS_EXTERNAL_GET_ACCESSION_REGISTER_SYMBOLIC_ERROR.getStatus().getStatusCode())
                .toResponse();
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
    @Produces(APPLICATION_JSON)
    @Secured(permission = FORMATS_ID_READ, description = "Lire un format donné")
    public Response findFormatByID(@PathParam("id_document") String documentId) {

        try {
            checkParameter(FORMAT_ID_MANDATORY, documentId);
            SanityChecker.checkParameter(documentId);

        } catch (final IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_PRECONDITION_FAILED, e.getMessage())
                .toResponse();
        }

        try (AdminManagementClient client = adminManagementClientFactory.getClient()) {
            final JsonNode result = client.getFormatByID(documentId);
            return Response.status(Status.OK).entity(result).build();
        } catch (ReferentialNotFoundException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_NOT_FOUND, e.getMessage())
                .toResponse();
        } catch (ReferentialException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_INTERNAL_SERVER_ERROR, e.getMessage())
                .toResponse();
        } catch (InvalidParseOperationException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_BAD_REQUEST, e.getMessage())
                .toResponse();
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
    @Produces(APPLICATION_JSON)
    @Secured(permission = RULES_ID_READ, description = "Lire une règle de gestion donnée")
    public Response findRuleByID(@PathParam("id_document") String documentId) {

        try {
            checkParameter(FORMAT_ID_MANDATORY, documentId);
            SanityChecker.checkParameter(documentId);
        } catch (IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_PRECONDITION_FAILED, e.getMessage())
                .toResponse();
        }

        try (AdminManagementClient client = adminManagementClientFactory.getClient()) {
            final JsonNode result = client.getRuleByID(documentId);
            return Response.status(Status.OK).entity(result).build();
        } catch (FileRulesNotFoundException e) {
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_NOT_FOUND, e.getMessage())
                .toResponse();
        } catch (ReferentialException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_INTERNAL_SERVER_ERROR, e.getMessage())
                .toResponse();
        } catch (InvalidParseOperationException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_BAD_REQUEST, e.getMessage())
                .toResponse();
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
    @Produces(APPLICATION_JSON)
    @Secured(permission = INGESTCONTRACTS_ID_READ, description = "Lire un contrat d'entrée donné")
    public Response findIngestContractsByID(@PathParam("id_document") String documentId) {

        try {
            checkParameter(FORMAT_ID_MANDATORY, documentId);
            SanityChecker.checkParameter(documentId);
        } catch (final IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_PRECONDITION_FAILED, e.getMessage())
                .toResponse();
        }

        try (AdminManagementClient client = adminManagementClientFactory.getClient()) {
            RequestResponse<IngestContractModel> requestResponse = client.findIngestContractsByID(documentId);
            int st = requestResponse.isOk() ? Status.OK.getStatusCode() : requestResponse.getHttpCode();
            return Response.status(st).entity(requestResponse).build();
        } catch (ReferentialNotFoundException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_NOT_FOUND, e.getMessage())
                .toResponse();
        } catch (ReferentialException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_INTERNAL_SERVER_ERROR, e.getMessage())
                .toResponse();
        } catch (InvalidParseOperationException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_BAD_REQUEST, e.getMessage())
                .toResponse();
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
    @Produces(APPLICATION_JSON)
    @Secured(permission = ACCESSCONTRACTS_ID_READ, description = "Lire un contrat d'accès donné")
    public Response findAccessContractsByID(@PathParam("id_document") String documentId) {

        try {
            checkParameter(FORMAT_ID_MANDATORY, documentId);
            SanityChecker.checkParameter(documentId);
        } catch (final IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_PRECONDITION_FAILED, e.getMessage())
                .toResponse();
        }

        try (AdminManagementClient client = adminManagementClientFactory.getClient()) {
            RequestResponse<AccessContractModel> requestResponse = client.findAccessContractsByID(documentId);
            int st = requestResponse.isOk() ? Status.OK.getStatusCode() : requestResponse.getHttpCode();
            return Response.status(st).entity(requestResponse).build();
        } catch (ReferentialNotFoundException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_NOT_FOUND, e.getMessage())
                .toResponse();
        } catch (ReferentialException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_INTERNAL_SERVER_ERROR, e.getMessage())
                .toResponse();
        } catch (InvalidParseOperationException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_BAD_REQUEST, e.getMessage())
                .toResponse();
        }
    }

    /**
     * findManagementContractsByID
     *
     * @param documentId the document id to find
     * @return Response
     */
    @Path(AccessExtAPI.MANAGEMENT_CONTRACT_API + "/{id_document:.+}")
    @GET
    @Produces(APPLICATION_JSON)
    @Secured(permission = MANAGEMENTCONTRACTS_ID_READ, description = "Lire un contrat de gestion donné")
    public Response findManagementContractsByID(@PathParam("id_document") String documentId) {

        try {
            checkParameter(FORMAT_ID_MANDATORY, documentId);
            SanityChecker.checkParameter(documentId);
        } catch (final IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_PRECONDITION_FAILED, e.getMessage())
                .toResponse();
        }

        try (AdminManagementClient client = adminManagementClientFactory.getClient()) {
            RequestResponse<ManagementContractModel> requestResponse =
                client.findManagementContractsByID(documentId);
            int st = requestResponse.isOk() ? Status.OK.getStatusCode() : requestResponse.getHttpCode();
            return Response.status(st).entity(requestResponse).build();
        } catch (ReferentialNotFoundException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_NOT_FOUND, e.getMessage())
                .toResponse();
        } catch (ReferentialException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_INTERNAL_SERVER_ERROR, e.getMessage())
                .toResponse();
        } catch (InvalidParseOperationException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_BAD_REQUEST, e.getMessage())
                .toResponse();
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
    @Produces(APPLICATION_JSON)
    @Secured(permission = PROFILES_ID_READ_JSON, description = "Lire un profil donné")
    public Response findProfilesByID(@PathParam("id_document") String documentId) {

        try {
            checkParameter(FORMAT_ID_MANDATORY, documentId);
            SanityChecker.checkParameter(documentId);
        } catch (final IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_PRECONDITION_FAILED, e.getMessage())
                .toResponse();
        }

        try (AdminManagementClient client = adminManagementClientFactory.getClient()) {
            RequestResponse<ProfileModel> requestResponse = client.findProfilesByID(documentId);
            int st = requestResponse.isOk() ? Status.OK.getStatusCode() : requestResponse.getHttpCode();
            return Response.status(st).entity(requestResponse).build();
        } catch (ReferentialNotFoundException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_NOT_FOUND, e.getMessage())
                .toResponse();
        } catch (ReferentialException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_INTERNAL_SERVER_ERROR, e.getMessage())
                .toResponse();
        } catch (InvalidParseOperationException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_BAD_REQUEST, e.getMessage())
                .toResponse();
        }
    }

    /**
     * find archive unit profiles by id
     *
     * @param documentId the archive unit profile ID to find
     * @return Response the matching archive unit profile or an error
     */
    @Path("/archiveunitprofiles/{id_document:.+}")
    @GET
    @Produces(APPLICATION_JSON)
    @Secured(permission = ARCHIVEUNITPROFILES_ID_READ_JSON, description = "Lire un document type donné")
    public Response findArchiveUnitProfilesByID(@PathParam("id_document") String documentId) {

        try {
            checkParameter("Archive unit profile ID is a mandatory parameter", documentId);
            SanityChecker.checkParameter(documentId);
        } catch (final IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_PRECONDITION_FAILED, e.getMessage())
                .toResponse();
        }

        try (AdminManagementClient client = adminManagementClientFactory.getClient()) {
            RequestResponse<ArchiveUnitProfileModel> requestResponse =
                client.findArchiveUnitProfilesByID(documentId);
            int st = requestResponse.isOk() ? Status.OK.getStatusCode() : requestResponse.getHttpCode();
            return Response.status(st).entity(requestResponse).build();
        } catch (ReferentialNotFoundException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_NOT_FOUND, e.getMessage())
                .toResponse();
        } catch (ReferentialException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_INTERNAL_SERVER_ERROR, e.getMessage())
                .toResponse();
        } catch (InvalidParseOperationException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_BAD_REQUEST, e.getMessage())
                .toResponse();
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
    @Produces(APPLICATION_JSON)
    @Secured(permission = CONTEXTS_ID_READ, description = "Lire un contexte donné")
    public Response findContextById(@PathParam("id_document") String documentId) {

        try {
            checkParameter(FORMAT_ID_MANDATORY, documentId);
            SanityChecker.checkParameter(documentId);
        } catch (final IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_PRECONDITION_FAILED, e.getMessage())
                .toResponse();
        }

        try (AdminManagementClient client = adminManagementClientFactory.getClient()) {
            RequestResponse<ContextModel> requestResponse = client.findContextById(documentId);
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
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Secured(permission = CONTEXTS_ID_UPDATE,
        description = "Effectuer une mise à jour sur un contexte", isAdminOnly = true)
    public Response updateContext(@PathParam("identifier") String identifier,
        @Dsl(DslSchema.UPDATE_BY_ID) JsonNode queryDsl)
        throws AdminManagementClientServerException, InvalidParseOperationException {

        try (AdminManagementClient client = adminManagementClientFactory.getClient()) {
            UpdateParserSingle updateParserSingle = new UpdateParserSingle();
            updateParserSingle.parse(queryDsl);
            Update update = updateParserSingle.getRequest();
            update.setQuery(QueryHelper.eq(IDENTIFIER, identifier));
            RequestResponse response = client.updateContext(identifier, update.getFinalUpdate());
            return getResponse(response);
        } catch (ReferentialNotFoundException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_CONTEXT_NOT_FOUND, e.getMessage())
                .setHttpCode(NOT_FOUND.getStatusCode())
                .toResponse();
        } catch (InvalidCreateOperationException | InvalidParseOperationException | AdminManagementClientServerException e) {
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
     * Update profile
     *
     * @param identifier
     * @param queryDsl
     * @return Response
     * @throws AdminManagementClientServerException
     * @throws InvalidParseOperationException
     */
    @Path("/profiles/{identifier:.+}")
    @PUT
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Secured(permission = PROFILES_ID_UPDATE_JSON, description = "Effectuer une mise à jour sur un profil")
    public Response updateProfile(@PathParam("identifier") String identifier,
        @Dsl(DslSchema.UPDATE_BY_ID) JsonNode queryDsl)
        throws AdminManagementClientServerException, InvalidParseOperationException {

        try (AdminManagementClient client = adminManagementClientFactory.getClient()) {
            UpdateParserSingle updateParserSingle = new UpdateParserSingle();
            updateParserSingle.parse(queryDsl);
            Update update = updateParserSingle.getRequest();
            update.setQuery(QueryHelper.eq(IDENTIFIER, identifier));
            RequestResponse response = client.updateProfile(identifier, update.getFinalUpdate());
            return getResponse(response);
        } catch (AdminManagementClientBadRequestException | InvalidCreateOperationException | InvalidParseOperationException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_UPDATE_PROFILE_ERROR, e.getMessage())
                .toResponse();
        } catch (ReferentialNotFoundException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_PROFILE_NOT_FOUND, e.getMessage())
                .setHttpCode(NOT_FOUND.getStatusCode())
                .toResponse();
        } catch (IllegalArgumentException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_UPDATE_PROFILE_ERROR, e.getMessage())
                .setHttpCode(Status.PRECONDITION_FAILED.getStatusCode()).toResponse();
        }
    }

    /**
     * Update archive unit profile
     *
     * @param identifier
     * @param queryDsl
     * @return Response
     * @throws AdminManagementClientServerException
     * @throws InvalidParseOperationException
     */
    @Path("/archiveunitprofiles/{identifier:.+}")
    @PUT
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Secured(permission = ARCHIVEUNITPROFILES_ID_UPDATE_JSON,
        description = "Effectuer une mise à jour sur un document type")
    public Response updateArchiveUnitProfile(@PathParam("identifier") String identifier,
        @Dsl(DslSchema.UPDATE_BY_ID) JsonNode queryDsl)
        throws AdminManagementClientServerException {

        try (AdminManagementClient client = adminManagementClientFactory.getClient()) {
            UpdateParserSingle updateParserSingle = new UpdateParserSingle();
            updateParserSingle.parse(queryDsl);
            Update update = updateParserSingle.getRequest();
            update.setQuery(QueryHelper.eq(IDENTIFIER, identifier));
            RequestResponse response = client.updateArchiveUnitProfile(identifier, update.getFinalUpdate());
            return getResponse(response);
        } catch (ReferentialNotFoundException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_PROFILE_NOT_FOUND, e.getMessage())
                .setHttpCode(NOT_FOUND.getStatusCode())
                .toResponse();
        } catch (InvalidCreateOperationException | InvalidParseOperationException e) {
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
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Secured(permission = ACCESSCONTRACTS_ID_UPDATE, description = "Effectuer une mise à jour sur un contrat d'accès")
    public Response updateAccessContract(@PathParam("identifier") String identifier,
        @Dsl(DslSchema.UPDATE_BY_ID) JsonNode queryDsl)
        throws AdminManagementClientServerException, InvalidParseOperationException {

        try (AdminManagementClient client = adminManagementClientFactory.getClient()) {
            UpdateParserSingle updateParserSingle = new UpdateParserSingle();
            updateParserSingle.parse(queryDsl);
            Update update = updateParserSingle.getRequest();
            update.setQuery(QueryHelper.eq(IDENTIFIER, identifier));
            RequestResponse response = client.updateAccessContract(identifier, update.getFinalUpdate());
            return getResponse(response);
        } catch (AdminManagementClientBadRequestException | InvalidCreateOperationException | InvalidParseOperationException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_UPDATE_ACCESS_CONTRACT_ERROR, e.getMessage())
                .toResponse();
        } catch (ReferentialNotFoundException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.CONTRACT_NOT_FOUND_ERROR, e.getMessage())
                .setHttpCode(NOT_FOUND.getStatusCode())
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
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Secured(permission = INGESTCONTRACTS_ID_UPDATE,
        description = "Effectuer une mise à jour sur un contrat d'entrée")
    public Response updateIngestContract(@PathParam("identifier") String identifier,
        @Dsl(DslSchema.UPDATE_BY_ID) JsonNode queryDsl)
        throws AdminManagementClientServerException, InvalidParseOperationException {

        try (AdminManagementClient client = adminManagementClientFactory.getClient()) {
            UpdateParserSingle updateParserSingle = new UpdateParserSingle();
            updateParserSingle.parse(queryDsl);
            Update update = updateParserSingle.getRequest();
            update.setQuery(QueryHelper.eq(IDENTIFIER, identifier));
            RequestResponse response = client.updateIngestContract(identifier, update.getFinalUpdate());
            return getResponse(response);
        } catch (AdminManagementClientBadRequestException | InvalidCreateOperationException | InvalidParseOperationException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_UPDATE_INGEST_CONTRACT_ERROR, e.getMessage())
                .toResponse();
        } catch (ReferentialNotFoundException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.CONTRACT_NOT_FOUND_ERROR, e.getMessage())
                .setHttpCode(NOT_FOUND.getStatusCode())
                .toResponse();
        } catch (IllegalArgumentException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_UPDATE_INGEST_CONTRACT_ERROR, e.getMessage())
                .setHttpCode(Status.PRECONDITION_FAILED.getStatusCode())
                .toResponse();
        }
    }

    /**
     * Update management contract
     *
     * @param identifier
     * @param queryDsl
     * @return Response
     * @throws AdminManagementClientServerException
     * @throws InvalidParseOperationException
     */
    @Path(AccessExtAPI.MANAGEMENT_CONTRACT_API_UPDATE + "/{identifier:.+}")
    @PUT
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Secured(permission = MANAGEMENTCONTRACTS_ID_UPDATE, description = "Effectuer une mise à jour sur un contrat de gestion")
    public Response updateManagementContract(@PathParam("identifier") String identifier,
        @Dsl(DslSchema.UPDATE_BY_ID) JsonNode queryDsl)
        throws AdminManagementClientServerException, InvalidParseOperationException {

        try (AdminManagementClient client = adminManagementClientFactory.getClient()) {
            UpdateParserSingle updateParserSingle = new UpdateParserSingle();
            updateParserSingle.parse(queryDsl);
            Update update = updateParserSingle.getRequest();
            update.setQuery(QueryHelper.eq(IDENTIFIER, identifier));
            RequestResponse response = client.updateManagementContract(identifier, update.getFinalUpdate());
            return getResponse(response);
        } catch (ReferentialNotFoundException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.CONTRACT_NOT_FOUND_ERROR, e.getMessage())
                .setHttpCode(NOT_FOUND.getStatusCode())
                .toResponse();
        } catch (InvalidCreateOperationException | InvalidParseOperationException | AdminManagementClientServerException e) {
            LOGGER.error(e);
            return VitamCodeHelper
                .toVitamError(VitamCode.ADMIN_EXTERNAL_UPDATE_MANAGEMENT_CONTRACT_ERROR, e.getMessage())
                .toResponse();
        } catch (IllegalArgumentException e) {
            LOGGER.error(e);
            return VitamCodeHelper
                .toVitamError(VitamCode.ADMIN_EXTERNAL_UPDATE_MANAGEMENT_CONTRACT_ERROR, e.getMessage())
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
     * @param originatingAgency the document id of accession register to get
     * @param select the query to get document
     * @return Response
     */
    @GET
    @Path(AccessExtAPI.ACCESSION_REGISTERS_API + "/{id_document}/" + AccessExtAPI.ACCESSION_REGISTERS_DETAIL)
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Secured(permission = ACCESSIONREGISTERS_ID_ACCESSIONREGISTERDETAILS_READ,
        description = "Lister les détails d'un registre de fonds")
    public Response findAccessionRegisterDetail(@PathParam("id_document") String originatingAgency,
        @Dsl(value = SELECT_SINGLE) JsonNode select) {

        checkParameter("accession register id is a mandatory parameter", originatingAgency);
        try (AdminManagementClient client = adminManagementClientFactory.getClient()) {
            SanityChecker.checkJsonAll(select);
            RequestResponse result =
                client.getAccessionRegisterDetail(originatingAgency, select);
            int st = result.isOk() ? Status.OK.getStatusCode() : result.getHttpCode();

            return Response.status(st).entity(result).build();
        } catch (final ReferentialNotFoundException e) {
            LOGGER.error(e);
            return VitamCodeHelper
                .toVitamError(VitamCode.ACCESS_EXTERNAL_GET_ACCESSION_REGISTER_DETAIL_ERROR, e.getMessage())
                .setHttpCode(Status.NOT_FOUND.getStatusCode())
                .toResponse();
        } catch (InvalidParseOperationException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_BAD_REQUEST, e.getMessage())
                .toResponse();
        } catch (Exception e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_INTERNAL_SERVER_ERROR, e.getMessage())
                .toResponse();
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
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Secured(permission = TRACEABILITYCHECKS_CREATE, description = "Tester l'intégrité d'un journal sécurisé")
    public Response checkOperationTraceability(@Dsl(value = SELECT_SINGLE) JsonNode query) {

        try (AccessInternalClient client = accessInternalClientFactory.getClient()) {
            checkParameter("checks operation Logbook traceability parameters", query);
            SanityChecker.checkJsonAll(query);
            RequestResponse<JsonNode> result = client.checkTraceabilityOperation(query);
            int st = result.isOk() ? Status.OK.getStatusCode() : result.getHttpCode();
            return Response.status(st).entity(result).build();
        } catch (final IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_BAD_REQUEST, e.getMessage())
                .toResponse();
        } catch (LogbookClientServerException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_INTERNAL_SERVER_ERROR, e.getMessage())
                .toResponse();
        } catch (AccessUnauthorizedException e) {
            LOGGER.error("Contract access does not allow ", e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_UNAUTHORIZED, e.getMessage())
                .toResponse();
        }
    }

    /**
     * Checks a list of traceability operation
     *
     * @param query the DSLQuery used to find the traceability operation to validate
     * @return The verification report == the logbookOperation
     */
    @POST
    @Path(AccessExtAPI.TRACEABILITY_API + "/linkedchecks")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Secured(permission = TRACEABILITYLINKEDCHECKS_CREATE, description = "Tester l'intégrité d'un journal sécurisé")
    public Response linkedCheckOperationTraceability(JsonNode query) {

        try (AccessInternalClient client = accessInternalClientFactory.getClient()) {
            checkParameter("checks operation Logbook traceability parameters", query);
            SanityChecker.checkJsonAll(query);
            RequestResponse<JsonNode> result = client.linkedCheckTraceability(query);
            int statusCode = result.isOk() ? Status.OK.getStatusCode() : result.getHttpCode();
            return Response.status(statusCode).entity(result).build();
        } catch (final IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_BAD_REQUEST, e.getMessage())
                .toResponse();
        } catch (LogbookClientException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_INTERNAL_SERVER_ERROR, e.getMessage())
                .toResponse();
        } catch (AccessUnauthorizedException e) {
            LOGGER.error("Contract access does not allow ", e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_UNAUTHORIZED, e.getMessage())
                .toResponse();
        }
    }

    /**
     * launch Audit
     *
     * @param options
     * @return response
     */
    @POST
    @Path(AccessExtAPI.REFERENTIAL_AUDIT_API)
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Secured(permission = REFERENTIALAUDIT_CHECK, description = "Lancer un audit de référentiel")
    public Response launchReferentialAudit(AuditReferentialOptions options) {
        checkParameter("audit options", options);
        try (AdminManagementClient client = adminManagementClientFactory.getClient()) {
            RequestResponse<JsonNode> result = client.launchReferentialAudit(options);
            int st = result.isOk() ? Status.OK.getStatusCode() : result.getHttpCode();
            return Response.status(st).entity(result).build();
        } catch (AdminManagementClientServerException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_BAD_REQUEST, e.getMessage())
                .toResponse();
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
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Secured(permission = AUDITS_CREATE, description = "Lancer un audit de l'existance des objets")
    public Response launchAudit(AuditOptions options) {
        checkParameter("audit options", options);
        try (AdminManagementClient client = adminManagementClientFactory.getClient()) {
            if (options.getQuery() != null) {
                SanityChecker.checkJsonAll(options.getQuery());
                BatchProcessingQuerySchemaValidator validator = new BatchProcessingQuerySchemaValidator();
                validator.validate(options.getQuery());
            }
            RequestResponse<JsonNode> result = client.launchAuditWorkflow(options);
            int st = result.isOk() ? Status.OK.getStatusCode() : result.getHttpCode();
            return Response.status(st).entity(result).build();
        } catch (AdminManagementClientServerException | InvalidParseOperationException | ValidationException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_BAD_REQUEST, e.getMessage())
                .toResponse();
        } catch (IOException e) {
            LOGGER.error("Technical exception", e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_INTERNAL_SERVER_ERROR, e.getMessage())
                .toResponse();
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
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Secured(permission = SECURITYPROFILES_CREATE_JSON, isAdminOnly = true,
        description = "Importer des profiles de sécurité dans le référentiel")
    public Response importSecurityProfiles(JsonNode document) {

        checkParameter("Json document is a mandatory parameter", document);
        try (AdminManagementClient client = adminManagementClientFactory.getClient()) {
            SanityChecker.checkJsonAll(document);
            Status status = client.importSecurityProfiles(getFromStringAsTypeReference(document.toString(),
                new TypeReference<List<SecurityProfileModel>>() {
                }));

            return Response.status(status).build();
        } catch (InvalidParseOperationException | InvalidFormatException | ReferentialException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_BAD_REQUEST, e.getMessage())
                .toResponse();
        }
    }

    /**
     * launch probative value request
     *
     * @param probativeValueRequest the request
     * @return Response
     */
    @POST
    @Path(AccessExtAPI.EXPORT_PROBATIVE_VALUE)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = PROBATIVEVALUE_CREATE, description = "Lancer un export du relevé de valeur probante")
    public Response exportProbativeValue(ProbativeValueRequest probativeValueRequest) {
        try (AdminManagementClient client = adminManagementClientFactory.getClient()) {
            SanityChecker.checkJsonAll(probativeValueRequest.getDslQuery());

            RequestResponse<JsonNode> result = client.exportProbativeValue(probativeValueRequest);
            int st = result.isOk() ? Status.OK.getStatusCode() : result.getHttpCode();
            return Response.status(st).entity(result).build();
        } catch (AdminManagementClientServerException | InvalidParseOperationException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_BAD_REQUEST, e.getMessage())
                .toResponse();
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
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Secured(permission = SECURITYPROFILES_READ,
        description = "Lister le contenu du référentiel des profiles de sécurité")
    public Response findSecurityProfiles(@Dsl(value = SELECT_SINGLE) JsonNode select) {

        try (AdminManagementClient client = adminManagementClientFactory.getClient()) {
            SanityChecker.checkJsonAll(select);
            RequestResponse<SecurityProfileModel> result = client.findSecurityProfiles(select);
            int st = result.isOk() ? Status.OK.getStatusCode() : result.getHttpCode();
            return Response.status(st).entity(result).build();
        } catch (ReferentialException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_INTERNAL_SERVER_ERROR, e.getMessage())
                .toResponse();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_BAD_REQUEST, e.getMessage())
                .toResponse();
        } catch (IllegalArgumentException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_PRECONDITION_FAILED, e.getMessage())
                .toResponse();
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
    @Produces(APPLICATION_JSON)
    @Secured(permission = SECURITYPROFILES_ID_READ, description = "Lire un profile de sécurité donné")
    public Response findSecurityProfileByIdentifier(@PathParam("identifier") String identifier) {

        try (AdminManagementClient client = adminManagementClientFactory.getClient()) {
            checkParameter("identifier is a mandatory parameter", identifier);
            RequestResponse<SecurityProfileModel> requestResponse =
                client.findSecurityProfileByIdentifier(identifier);
            int st = requestResponse.isOk() ? Status.OK.getStatusCode() : requestResponse.getHttpCode();
            return Response.status(st).entity(requestResponse).build();
        } catch (ReferentialNotFoundException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_NOT_FOUND, e.getMessage()).toResponse();
        } catch (final ReferentialException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_INTERNAL_SERVER_ERROR, e.getMessage())
                .toResponse();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_BAD_REQUEST, e.getMessage()).toResponse();
        } catch (final IllegalArgumentException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_PRECONDITION_FAILED, e.getMessage())
                .toResponse();
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
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Secured(permission = SECURITYPROFILES_ID_UPDATE, isAdminOnly = true,
        description = "Effectuer une mise à jour sur un profil de sécurité")
    public Response updateSecurityProfile(@PathParam("identifier") String identifier,
        @Dsl(DslSchema.UPDATE_BY_ID) JsonNode queryDsl)
        throws AdminManagementClientServerException {

        try (AdminManagementClient client = adminManagementClientFactory.getClient()) {
            UpdateParserSingle updateParserSingle = new UpdateParserSingle();
            updateParserSingle.parse(queryDsl);
            Update update = updateParserSingle.getRequest();
            update.setQuery(QueryHelper.eq(IDENTIFIER, identifier));
            RequestResponse response = client.updateSecurityProfile(identifier, update.getFinalUpdateById());
            return getResponse(response);
        } catch (AdminManagementClientBadRequestException | InvalidCreateOperationException | InvalidParseOperationException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_UPDATE_SECURITY_PROFILE_ERROR, e.getMessage())
                .toResponse();
        } catch (ReferentialNotFoundException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_SECURITY_PROFILE_NOT_FOUND, e.getMessage())
                .setHttpCode(Status.PRECONDITION_FAILED.getStatusCode())
                .toResponse();
        } catch (IllegalArgumentException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_UPDATE_SECURITY_PROFILE_ERROR, e.getMessage())
                .setHttpCode(Status.PRECONDITION_FAILED.getStatusCode())
                .toResponse();
        }
    }

    /**
     * @param headers the http header of request
     * @param query the filter query
     * @return the list of Operations details
     */
    @GET
    @Path("/operations")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Secured(permission = OPERATIONS_READ, description = "Récupérer les informations sur une opération donnée")
    public Response listOperationsDetails(@Context HttpHeaders headers, ProcessQuery query) {
        try (IngestInternalClient client = ingestInternalClientFactory.getClient()) {
            return client.listOperationsDetails(query).toResponse();
        } catch (VitamClientException e) {
            LOGGER.error(UNEXPECTED_ERROR + e.getMessage(), e);
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
    @Produces(APPLICATION_JSON)
    @Secured(permission = OPERATIONS_ID_READ_STATUS, description = "Récupérer le code HTTP d'une opération donnée")
    public Response getWorkFlowExecutionStatus(@PathParam("id") String id) {
        try (IngestInternalClient ingestInternalClient = ingestInternalClientFactory.getClient()) {
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
            return Response.status(INTERNAL_SERVER_ERROR).build();
        } catch (BadRequestException e) {
            LOGGER.error(e);
            return Response.status(BAD_REQUEST).build();
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
    @Produces(APPLICATION_JSON)
    @Secured(permission = OPERATIONS_ID_READ, description = "Récupérer le statut d'une opération donnée")
    public Response getOperationProcessExecutionDetails(@PathParam("id") String id) {
        Status status;
        try (IngestInternalClient ingestInternalClient = ingestInternalClientFactory.getClient()) {
            return ingestInternalClient.getOperationProcessExecutionDetails(id).toResponse();
        } catch (final IllegalArgumentException e) {
            LOGGER.error("Illegal argument: ", e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status)
                .entity(VitamCodeHelper
                    .toVitamError(VitamCode.INGEST_EXTERNAL_GET_OPERATION_PROCESS_DETAIL_ERROR, e.getLocalizedMessage())
                    .setHttpCode(status.getStatusCode()))
                .build();
        } catch (VitamClientException e) {
            LOGGER.error(UNEXPECTED_ERROR + e.getMessage(), e);
            status = INTERNAL_SERVER_ERROR;
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
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Secured(permission = OPERATIONS_ID_UPDATE, description = "Changer le statut d'une opération donnée")
    public Response updateWorkFlowStatus(@Context HttpHeaders headers, @PathParam("id") String id) {
        checkParameter("ACTION Request must not be null",
            headers.getRequestHeader(GlobalDataRest.X_ACTION));

        final String xAction = headers.getRequestHeader(GlobalDataRest.X_ACTION).get(0);
        return updateOperationActionProcessAsync(id, xAction);
    }

    private Response updateOperationActionProcessAsync(String operationId, String action) {

        try (IngestInternalClient ingestInternalClient = ingestInternalClientFactory.getClient()) {
            VitamThreadUtils.getVitamSession().setRequestId(operationId);
            RequestResponse<ItemStatus> itemStatusRequestResponse =
                ingestInternalClient.updateOperationActionProcess(action, operationId);
            return itemStatusRequestResponse.toResponse();
        } catch (final ProcessingException e) {
            LOGGER.error("Unauthorized action for update ", e);
            return VitamCodeHelper
                .toVitamError(VitamCode.INGEST_EXTERNAL_UNAUTHORIZED, e.getLocalizedMessage()).toResponse();
        } catch (VitamClientException e) {
            LOGGER.error("Client exception while trying to update operation process ", e);
            return VitamCodeHelper
                .toVitamError(VitamCode.INGEST_EXTERNAL_INTERNAL_CLIENT_ERROR, e.getLocalizedMessage())
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
    @Produces(APPLICATION_JSON)
    @Secured(permission = OPERATIONS_ID_DELETE, description = "Annuler une opération donnée")
    public Response interruptWorkFlowExecution(@PathParam("id") String id) {

        checkParameter("operationId must not be null", id);
        VitamError vitamError;
        try (IngestInternalClient ingestInternalClient = ingestInternalClientFactory.getClient()) {
            SanityChecker.checkParameter(id);
            VitamThreadUtils.getVitamSession().setRequestId(id);

            final RequestResponse requestResponse = ingestInternalClient.cancelOperationProcessExecution(id);
            return requestResponse.toResponse();
        } catch (final IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error("Illegal argument: ", e);
            vitamError =
                VitamCodeHelper.toVitamError(VitamCode.INGEST_EXTERNAL_ILLEGAL_ARGUMENT, e.getLocalizedMessage());
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
    @Produces(APPLICATION_JSON)
    @Secured(permission = WORKFLOWS_READ, description = "Récupérer la liste des tâches des workflows")
    public Response getWorkflowDefinitions(@Context HttpHeaders headers) {
        try (IngestInternalClient client = ingestInternalClientFactory.getClient()) {
            return client.getWorkflowDefinitions().toResponse();
        } catch (VitamClientException e) {
            LOGGER.error(UNEXPECTED_ERROR + e.getMessage(), e);
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
    @Secured(permission = RULESREPORT_ID_READ,
        description = "Récupérer le rapport pour une opération d'import de règles de gestion")
    public Response downloadRulesReportAsStream(@PathParam("opId") String opId) {
        return downloadObjectAsync(opId, IngestCollection.RULES);
    }

    @GET
    @Path("/batchreport/{opId}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Secured(permission = BATCHREPORT_ID_READ,
        description = "Récupérer le rapport pour un traitement de masse (Elimination, Preservation, Audit, Mise à jour)")
    public Response downloadBatchReportAsStream(@PathParam("opId") String opId) {
        return downloadObjectAsync(opId, IngestCollection.BATCH_REPORT);
    }

    @GET
    @Path("/distributionreport/{opId}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Secured(permission = DISTRIBUTIONREPORT_ID_READ,
        description = "Récupérer le rapport pour une opération de mise à jour de masse distribuée")
    public Response downloadDistributionReportAsStream(@PathParam("opId") String opId) {
        return downloadObjectAsync(opId, IngestCollection.DISTRIBUTIONREPORTS);
    }

    @GET
    @Path("/rulesreferential/{opId}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Secured(permission = RULESREFERENTIAL_ID_READ,
        description = "Récupérer le référentiel pour une opération d'import de règles de gestion")
    public Response downloadAgenciesCsvAsStream(@PathParam("opId") String opId) {
        return downloadObjectAsync(opId, IngestCollection.REFERENTIAL_RULES_CSV);
    }

    @GET
    @Path("/agenciesreferential/{opId}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Secured(permission = AGENCIESREFERENTIAL_ID_READ,
        description = "Récupérer le référentiel pour une opération d'import des service agents")
    public Response downloadRulesCsvAsStream(@PathParam("opId") String opId) {
        return downloadObjectAsync(opId, IngestCollection.REFERENTIAL_AGENCIES_CSV);
    }

    private Response downloadObjectAsync(String objectId, IngestCollection collection) {

        try (IngestInternalClient ingestInternalClient = ingestInternalClientFactory.getClient()) {
            final Response response = ingestInternalClient.downloadObjectAsync(objectId, collection);
            return new VitamAsyncInputStreamResponse(response);
        } catch (IllegalArgumentException e) {
            LOGGER.error("IllegalArgumentException was thrown : ", e);
            return Response.status(BAD_REQUEST)
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
            return Response.status(INTERNAL_SERVER_ERROR)
                .entity(getErrorStream(
                    VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_INTERNAL_SERVER_ERROR,
                        e.getLocalizedMessage())))
                .build();
        } catch (final IngestInternalClientNotFoundException e) {
            LOGGER.error("Request resources does not exits", e);
            return Response.status(NOT_FOUND)
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

    /**
     * launch a traceability audit for the query
     *
     * @param select the query select
     * @return Response
     */
    @Path("/evidenceaudit")
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Secured(permission = EVIDENCEAUDIT_CHECK, description = "Audit de traçabilité d'unités archivistiques")
    public Response checkEvidenceAudit(@Dsl(value = DslSchema.SELECT_MULTIPLE) JsonNode select) {
        checkParameter("mandatory parameter", select);
        try (AdminManagementClient client = adminManagementClientFactory.getClient()) {
            RequestResponse<JsonNode> result = client.evidenceAudit(select);
            int st = result.isOk() ? Status.OK.getStatusCode() : result.getHttpCode();
            return Response.status(st).entity(result).build();
        } catch (AdminManagementClientServerException e) {
            LOGGER.error(UNEXPECTED_ERROR + e.getMessage(), e);
            return Response.serverError()
                .entity(VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_UNIT_TRACREABILITY_AUDIT,
                    e.getLocalizedMessage()))
                .build();
        }
    }

    /**
     * launch a rectification audit for the operation
     *
     * @param operationId the operation id
     * @return Response
     */
    @Path(RECTIFICATION_AUDIT)
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Secured(permission = RECTIFICATIONAUDIT_CHECK, description = "rectification de données suite a un audit")
    public Response rectificationAudit(String operationId) {
        checkParameter("mandatory parameter", operationId);
        try (AdminManagementClient client = adminManagementClientFactory.getClient()) {
            RequestResponse<JsonNode> result = client.rectificationAudit(operationId);
            int st = result.isOk() ? Status.OK.getStatusCode() : result.getHttpCode();
            return Response.status(st).entity(result).build();
        } catch (AdminManagementClientServerException e) {
            LOGGER.error(UNEXPECTED_ERROR + e.getMessage(), e);
            return Response.serverError()
                .entity(VitamCodeHelper.toVitamError(VitamCode.ACCESS_EXTERNAL_UNIT_TRACREABILITY_AUDIT,
                    e.getLocalizedMessage()))
                .build();
        }
    }

    /**
     * Import a set of ontologies
     *
     * @param ontologies the ontologies to create
     * @return Response
     */
    @Path("/ontologies")
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Secured(permission = ONTOLOGIES_CREATE_JSON, description = "Importer les ontologies dans le référentiel")
    public Response importOntologies(@HeaderParam(GlobalDataRest.FORCE_UPDATE) boolean forceUpdate,
        JsonNode ontologies) {

        checkParameter("Json ontologies is a mandatory parameter", ontologies);
        try (AdminManagementClient client = adminManagementClientFactory.getClient()) {
            SanityChecker.checkJsonAll(ontologies);
            RequestResponse requestResponse =
                client.importOntologies(forceUpdate, getFromStringAsTypeReference(ontologies.toString(),
                    new TypeReference<List<OntologyModel>>() {
                    }));
            return Response.status(requestResponse.getStatus())
                .entity(requestResponse).build();

        } catch (final ReferentialException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_BAD_REQUEST, e.getMessage())
                .toResponse();
        } catch (InvalidParseOperationException | InvalidFormatException e) {
            LOGGER.error(e);
            VitamError error = new VitamError(VitamCode.ACCESS_EXTERNAL_INVALID_JSON.getItem())
                .setMessage(VitamCode.ACCESS_EXTERNAL_INVALID_JSON.getMessage())
                .setState(StatusCode.KO.name())
                .setCode(VitamCodeHelper.getCode(VitamCode.ACCESS_EXTERNAL_INVALID_JSON))
                .setContext(ACCESS_EXTERNAL_MODULE)
                .setDescription(VitamCode.ACCESS_EXTERNAL_INVALID_JSON.getMessage());
            return Response.status(BAD_REQUEST)
                .entity(error).build();
        }
    }


    /**
     * find ontologies using get method
     *
     * @param select the select query to find document
     * @return Response
     */
    @Path("/ontologies")
    @GET
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Secured(permission = ONTOLOGIES_READ, description = "Lister le contenu du référentiel des ontologies")
    public Response findOntologies(@Dsl(value = SELECT_SINGLE) JsonNode select) {

        try (AdminManagementClient client = adminManagementClientFactory.getClient()) {
            SanityChecker.checkJsonAll(select);
            RequestResponse result = client.findOntologies(select);
            int st = result.isOk() ? Status.OK.getStatusCode() : result.getHttpCode();
            return Response.status(st).entity(result).build();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_BAD_REQUEST, e.getMessage())
                .toResponse();
        } catch (IllegalArgumentException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_PRECONDITION_FAILED, e.getMessage())
                .toResponse();
        }
    }

    /**
     * find an ontology by id
     *
     * @param documentId the ontology ID to find
     * @return Response the ontology or an error
     */
    @Path("/ontologies/{id_document:.+}")
    @GET
    @Produces(APPLICATION_JSON)
    @Secured(permission = ONTOLOGIES_ID_READ_JSON, description = "Lire une ontologie")
    public Response findOntologiesByID(@PathParam("id_document") String documentId) {

        try {
            checkParameter("Ontology ID is a mandatory parameter", documentId);
            SanityChecker.checkParameter(documentId);

        } catch (final IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_PRECONDITION_FAILED, e.getMessage())
                .toResponse();
        }

        try (AdminManagementClient client = adminManagementClientFactory.getClient()) {
            RequestResponse<OntologyModel> requestResponse = client.findOntologyByID(documentId);
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
        }
    }



    /**
     * Pause the processes specified by ProcessPause info
     *
     * @param info a ProcessPause object indicating the tenant and/or the type of process to pause
     * @return
     */
    @Path("/forcepause")
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Secured(permission = FORCEPAUSE_CHECK,
        description = "Force la pause sur un type d'operation et/ou sur un tenant")
    public Response forcePause(ProcessPause info) {

        checkParameter("Json ProcessPause is a mandatory parameter", info);
        try (AdminManagementClient client = adminManagementClientFactory.getClient()) {

            RequestResponse requestResponse = client.forcePause(info);
            return Response.status(requestResponse.getStatus())
                .entity(requestResponse).build();

        } catch (final AdminManagementClientServerException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_BAD_REQUEST, e.getMessage())
                .toResponse();
        }
    }

    /**
     * Remove the pause for the processes specified by ProcessPause info
     *
     * @param info a ProcessPause object indicating the tenant and/or the type of process to pause
     * @return
     */
    @Path("/removeforcepause")
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Secured(permission = REMOVEFORCEPAUSE_CHECK,
        description = "Retire la pause sur un type d'operation et/ou sur un tenant")
    public Response removeForcePause(ProcessPause info) {

        checkParameter("Json ProcessPause is a mandatory parameter", info);
        try (AdminManagementClient client = adminManagementClientFactory.getClient()) {

            RequestResponse requestResponse = client.removeForcePause(info);
            return Response.status(requestResponse.getStatus())
                .entity(requestResponse).build();

        } catch (final AdminManagementClientServerException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_BAD_REQUEST, e.getMessage())
                .toResponse();
        }
    }

    @POST
    @Path("/griffin")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = GRIFFINS_CREATE, isAdminOnly = true, description = "Import du griffon")
    public Response importGriffin(JsonNode griffins) throws AdminManagementClientServerException {

        checkParameter("Json griffin is a mandatory parameter", griffins);
        try (AdminManagementClient client = adminManagementClientFactory.getClient()) {

            SanityChecker.checkJsonAll(griffins);

            RequestResponse requestResponse =
                client.importGriffins(getFromStringAsTypeReference(griffins.toString(),
                    new TypeReference<List<GriffinModel>>() {
                    }));

            return Response.status(requestResponse.getStatus())
                .entity(requestResponse).build();

        } catch (AdminManagementClientBadRequestException | InvalidParseOperationException | InvalidFormatException e) {
            return buildErrorResponse(VitamCode.PRESERVATION_VALIDATION_ERROR, e.getMessage());
        } catch (ReferentialException e) {
            return buildErrorResponse(VitamCode.PRESERVATION_INTERNAL_ERROR, e.getMessage());
        }
    }

    @GET
    @Path("/griffin")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = GRIFFINS_READ, description = "Lister le contenu du référentiel des griffons")
    public Response findGriffin(@Dsl(value = SELECT_SINGLE) JsonNode select)
        throws AdminManagementClientServerException {

        try (AdminManagementClient client = adminManagementClientFactory.getClient()) {
            SanityChecker.checkJsonAll(select);
            RequestResponse result = client.findGriffin(select);
            int st = result.isOk() ? Status.OK.getStatusCode() : result.getHttpCode();
            return Response.status(st).entity(result).build();
        } catch (ReferentialException e) {
            LOGGER.error(e);
            return buildErrorResponse(VitamCode.PRESERVATION_INTERNAL_ERROR, e.getMessage());
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            return buildErrorResponse(VitamCode.ADMIN_EXTERNAL_BAD_REQUEST, e.getMessage());
        }
    }


    @Path("/preservationScenario")
    @GET
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Secured(permission = PRESERVATIONSCENARIOS_READ,
        description = "Lister le contenu du référentiel des préservation scénarios")
    public Response findPreservationScenarios(@Dsl(value = SELECT_SINGLE) JsonNode select) {

        try (AdminManagementClient client = adminManagementClientFactory.getClient()) {
            SanityChecker.checkJsonAll(select);
            RequestResponse result = client.findPreservation(select);
            int st = result.isOk() ? Status.OK.getStatusCode() : result.getHttpCode();
            return Response.status(st).entity(result).build();
        } catch (ReferentialException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_INTERNAL_SERVER_ERROR, e.getMessage())
                .toResponse();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_BAD_REQUEST, e.getMessage())
                .toResponse();
        } catch (IllegalArgumentException e) {
            LOGGER.error(e);
            return VitamCodeHelper.toVitamError(VitamCode.ADMIN_EXTERNAL_PRECONDITION_FAILED, e.getMessage())
                .toResponse();
        }
    }

    @POST
    @Path("/preservationScenario")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = PRESERVATIONSCENARIOS_CREATE, description = "Import des perservation scénarios")
    public Response importPreservationScenario(JsonNode preservationScenarios) {

        try (AdminManagementClient client = adminManagementClientFactory.getClient()) {

            checkParameter("Json preservationScenarios is a mandatory parameter", preservationScenarios);
            SanityChecker.checkJsonAll(preservationScenarios);

            RequestResponse requestResponse =
                client.importPreservationScenarios(getFromStringAsTypeReference(preservationScenarios.toString(),
                    new TypeReference<List<PreservationScenarioModel>>() {
                    }));

            return Response.status(requestResponse.getStatus())
                .entity(requestResponse).build();

        } catch (AdminManagementClientBadRequestException | InvalidParseOperationException | InvalidFormatException e) {
            return buildErrorResponse(VitamCode.PRESERVATION_VALIDATION_ERROR, e.getMessage());
        } catch (ReferentialException e) {
            return buildErrorResponse(VitamCode.PRESERVATION_INTERNAL_ERROR, e.getMessage());
        }
    }

    @Path("/griffin/{id_document:.+}")
    @GET
    @Produces(APPLICATION_JSON)
    @Secured(permission = GRIFFIN_READ, description = "lecture d'un griffin par identifier")
    public Response findGriffinByID(@PathParam("id_document") String documentId) {

        try {
            checkParameter("Griffin ID is a  mandatory parameter", documentId);
            SanityChecker.checkParameter(documentId);

            return findGriffin(documentId);
        } catch (IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.warn(e);
            return buildErrorResponse(VitamCode.PRESERVATION_VALIDATION_ERROR, e.getMessage());
        } catch (ReferentialNotFoundException e) {
            LOGGER.error(e);
            LOGGER.error(e);
            return buildErrorResponse(VitamCode.REFERENTIAL_NOT_FOUND, e.getMessage());
        } catch (ReferentialException e) {
            LOGGER.error(e);
            return buildErrorResponse(VitamCode.PRESERVATION_INTERNAL_ERROR, e.getMessage());
        }
    }

    private Response findGriffin(@PathParam("id_document") String documentId)
        throws InvalidParseOperationException, ReferentialNotFoundException, AdminManagementClientServerException {

        try (AdminManagementClient client = adminManagementClientFactory.getClient()) {
            RequestResponse<GriffinModel> requestResponse = client.findGriffinByID(documentId);
            int st = requestResponse.isOk() ? Status.OK.getStatusCode() : requestResponse.getHttpCode();
            return Response.status(st).entity(requestResponse).build();
        }
    }

    @Path("/preservationScenario/{id_document:.+}")
    @GET
    @Produces(APPLICATION_JSON)
    @Secured(permission = PRESERVATIONSCENARIO_READ, description = "lecture d'un scenario par identifier")
    public Response findPreservationByID(@PathParam("id_document") String documentId) {

        try {
            checkParameter("Preservation ID is a  mandatory parameter", documentId);
            SanityChecker.checkParameter(documentId);

            return findPreservation(documentId);
        } catch (IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.warn(e);
            return buildErrorResponse(VitamCode.PRESERVATION_VALIDATION_ERROR, e.getMessage());
        } catch (ReferentialNotFoundException e) {
            LOGGER.error(e);
            return buildErrorResponse(VitamCode.REFERENTIAL_NOT_FOUND, e.getMessage());
        } catch (ReferentialException e) {
            LOGGER.error(e);
            return buildErrorResponse(VitamCode.PRESERVATION_INTERNAL_ERROR, e.getMessage());
        }
    }

    private Response findPreservation(@PathParam("id_document") String documentId)
        throws InvalidParseOperationException, ReferentialNotFoundException, AdminManagementClientServerException {

        try (AdminManagementClient client = adminManagementClientFactory.getClient()) {

            RequestResponse<PreservationScenarioModel> requestResponse = client.findPreservationByID(documentId);
            int status = requestResponse.isOk() ? Status.OK.getStatusCode() : requestResponse.getHttpCode();
            return Response.status(status).entity(requestResponse).build();
        }
    }

    private Response buildErrorResponse(VitamCode vitamCode, String message) {

        return Response.status(vitamCode.getStatus())
            .entity(new RequestResponseError().setError(new VitamError(getCode(vitamCode))
                .setContext(vitamCode.getService().getName()).setState(vitamCode.getDomain().getName())
                .setMessage(vitamCode.getMessage()).setDescription(vitamCode.getMessage())).toString() + message)
            .build();
    }


    /**
     * Posts a new logbook entry to Vitam
     *
     * @param operation the LogbookOperationParameters
     * @return Response contains the list of logbook operations
     */
    @POST
    @Path(AccessExtAPI.LOGBOOK_OPERATIONS)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = LOGBOOKOPERATIONS_CREATE, description = "Créer une opération")
    public Response createExternalOperation(LogbookOperationParameters operation) {
        Status status;
        try (AdminManagementClient client = adminManagementClientFactory.getClient()) {
            if (JsonHandler.writeValueAsBytes(JsonHandler.toJsonNode(operation)).length >= VitamConfiguration
                .getOperationMaxSizeForExternal()) {
                throw new IllegalArgumentException(
                    "The size of this operation is too big");
            }
            status = client.createExternalOperation(operation);

            return Response.status(status).build();
        } catch (AdminManagementClientServerException e) {
            LOGGER.error("Admin Management Client exception while trying to create logbookoperations: ", e);
            return VitamCodeHelper
                .toVitamError(VitamCode.LOGBOOK_EXTERNAL_INTERNAL_SERVER_ERROR,
                    e.getLocalizedMessage())
                .setHttpCode(Status.INTERNAL_SERVER_ERROR.getStatusCode()).toResponse();
        } catch (LogbookClientAlreadyExistsException e) {
            LOGGER.error("Logbook Client exception while trying to create logbookoperations: ", e);
            return VitamCodeHelper
                .toVitamError(VitamCode.LOGBOOK_EXTERNAL_CONFLICT,
                    e.getLocalizedMessage())
                .setHttpCode(Status.CONFLICT.getStatusCode()).toResponse();
        } catch (BadRequestException | InvalidParseOperationException | IllegalArgumentException e) {
            LOGGER.error("Bad request while trying to create logbookoperations: ", e);
            return VitamCodeHelper
                .toVitamError(VitamCode.LOGBOOK_EXTERNAL_BAD_REQUEST,
                    e.getLocalizedMessage())
                .setHttpCode(BAD_REQUEST.getStatusCode()).toResponse();
        }
    }

}
