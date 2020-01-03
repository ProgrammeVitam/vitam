/*
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
 */
package fr.gouv.vitam.functional.administration.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Iterables;
import fr.gouv.vitam.common.CharsetUtils;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.client.OntologyLoader;
import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.parser.request.adapter.SingleVarNameAdapter;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.database.parser.request.single.SelectParserSingle;
import fr.gouv.vitam.common.database.server.DbRequestSingle;
import fr.gouv.vitam.common.database.utils.AccessContractRestrictionHelper;
import fr.gouv.vitam.common.error.ServiceName;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.AccessUnauthorizedException;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InvalidGuidOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDReader;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.AuditOptions;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.ProcessPause;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.VitamConstants;
import fr.gouv.vitam.common.model.VitamSession;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.model.administration.AccessionRegisterDetailModel;
import fr.gouv.vitam.common.model.administration.ActivationStatus;
import fr.gouv.vitam.common.model.administration.FileRulesModel;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.server.application.configuration.DbConfigurationImpl;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.common.server.application.resources.BasicVitamStatusServiceImpl;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.stream.VitamAsyncInputStreamResponse;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.accession.register.core.ReferentialAccessionRegisterImpl;
import fr.gouv.vitam.functional.administration.common.AccessContract;
import fr.gouv.vitam.functional.administration.common.AccessionRegisterDetail;
import fr.gouv.vitam.functional.administration.common.AccessionRegisterSummary;
import fr.gouv.vitam.functional.administration.common.ErrorReport;
import fr.gouv.vitam.functional.administration.common.FileFormat;
import fr.gouv.vitam.functional.administration.common.FileRules;
import fr.gouv.vitam.functional.administration.common.counter.VitamCounterService;
import fr.gouv.vitam.functional.administration.common.exception.DatabaseConflictException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesCsvException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesImportInProgressException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesUpdateException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.server.AccessionRegisterSymbolic;
import fr.gouv.vitam.functional.administration.common.server.AdminManagementConfiguration;
import fr.gouv.vitam.functional.administration.common.server.ElasticsearchAccessAdminFactory;
import fr.gouv.vitam.functional.administration.common.server.ElasticsearchAccessFunctionalAdmin;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminFactory;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.functional.administration.contract.api.ContractService;
import fr.gouv.vitam.functional.administration.contract.core.AccessContractImpl;
import fr.gouv.vitam.functional.administration.format.core.ReferentialFormatFileImpl;
import fr.gouv.vitam.functional.administration.rules.core.RulesManagerFileImpl;
import fr.gouv.vitam.functional.administration.rules.core.VitamRuleService;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.Contexts;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.ProcessingEntry;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;

@Path("/adminmanagement/v1")
@ApplicationPath("webresources")
public class AdminManagementResource extends ApplicationStatusResource {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AdminManagementResource.class);

    private static final SingleVarNameAdapter DEFAULT_VARNAME_ADAPTER = new SingleVarNameAdapter();
    private static final String FUNCTIONAL_ADMINISTRATION_MODULE = "FUNCTIONAL_ADMINISTRATION_MODULE";

    private static final String ATTACHMENT_FILENAME = "attachment; filename=ErrorReport.json";
    private static final String SELECT_IS_A_MANDATORY_PARAMETER = "select is a mandatory parameter";
    private static final String AUDIT_URI = "/audit";
    private static final String AUDIT_RULE_URI = "/auditRule";
    private static final String OPERATIONS_PATH = "logbookoperations";
    private static final String OPTIONS_IS_MANDATORY_PARAMETER = "The json option is mandatory";
    private static final String ORIGINATING_AGENCY = "OriginatingAgency";
    private static final String ACCESS_CONTRACT = "AccessContract";
    private static final String AUDIT_TYPE = "auditType";
    private static final String OBJECT_ID = "objectId";
    private static final String AUDIT_ACTIONS = "auditActions";

    private final MongoDbAccessAdminImpl mongoAccess;
    private final ElasticsearchAccessFunctionalAdmin elasticsearchAccess;
    private final OntologyLoader rulesOntologyLoader;
    private VitamCounterService vitamCounterService;
    private VitamRuleService vitamRuleService;

    public AdminManagementResource(AdminManagementConfiguration configuration, OntologyLoader ontologyLoader, OntologyLoader rulesOntologyLoader) {
        super(new BasicVitamStatusServiceImpl());
        this.rulesOntologyLoader = rulesOntologyLoader;
        DbConfigurationImpl adminConfiguration;
        if (configuration.isDbAuthentication()) {
            adminConfiguration =
                new DbConfigurationImpl(configuration.getMongoDbNodes(), configuration.getDbName(),
                    true, configuration.getDbUserName(), configuration.getDbPassword());
        } else {
            adminConfiguration =
                new DbConfigurationImpl(configuration.getMongoDbNodes(),
                    configuration.getDbName());
        }
        // / FIXME: 3/31/17 Factories mustn't be created here !!!
        elasticsearchAccess = ElasticsearchAccessAdminFactory.create(configuration);
        mongoAccess = MongoDbAccessAdminFactory.create(adminConfiguration, ontologyLoader);
        WorkspaceClientFactory.changeMode(configuration.getWorkspaceUrl());
        ProcessingManagementClientFactory.changeConfigurationUrl(configuration.getProcessingUrl());
        vitamRuleService = new VitamRuleService(configuration.getListMinimumRuleDuration());
        LOGGER.debug("init Admin Management Resource server");
    }


    MongoDbAccessAdminImpl getLogbookDbAccess() {
        return mongoAccess;
    }

    /**
     * @return the elasticsearchAccess
     */
    ElasticsearchAccessFunctionalAdmin getElasticsearchAccess() {
        return elasticsearchAccess;
    }

    /**
     * check the file format
     *
     * @param xmlPronom as InputStream
     * @return Response
     */
    @Path("format/check")
    @POST
    @Consumes(APPLICATION_OCTET_STREAM)
    @Produces(APPLICATION_JSON)
    public Response checkFormat(InputStream xmlPronom) {
        ParametersChecker.checkParameter("xmlPronom is a mandatory parameter", xmlPronom);
        try (ReferentialFormatFileImpl formatManagement =
            new ReferentialFormatFileImpl(mongoAccess, vitamCounterService)) {
            formatManagement.checkFile(xmlPronom);
            return Response.status(Status.OK).build();
        } catch (final ReferentialException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (final Exception e) {
            LOGGER.error(e);
            return Response.status(INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        } finally {
            StreamUtils.closeSilently(xmlPronom);
        }
    }


    /**
     * import the file format
     *
     * @param headers http headers
     * @param xmlPronom as InputStream
     * @return response
     */
    @Path("format/import")
    @POST
    @Consumes(APPLICATION_OCTET_STREAM)
    @Produces(APPLICATION_JSON)
    public Response importFormat(@Context HttpHeaders headers, InputStream xmlPronom) {
        ParametersChecker.checkParameter("xmlPronom is a mandatory parameter", xmlPronom);
        String filename = headers.getHeaderString(GlobalDataRest.X_FILENAME);
        try (ReferentialFormatFileImpl formatManagement =
            new ReferentialFormatFileImpl(mongoAccess, vitamCounterService)) {
            formatManagement.importFile(xmlPronom, filename);
            return Response.status(Status.CREATED).entity(Status.CREATED.getReasonPhrase()).build();
        } catch (final ReferentialException e) {
            LOGGER.error(e);
            final Status status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(e.getMessage())
                .build();
        } catch (final DatabaseConflictException e) {
            LOGGER.error(e);
            return Response.status(Status.CONFLICT).entity(e.getMessage()).build();
        } catch (final Exception e) {
            LOGGER.error(e);
            final Status status = INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(status)
                .build();
        } finally {
            StreamUtils.closeSilently(xmlPronom);
        }

    }

    /**
     * Find the file format detail related to a specified Id
     *
     * @param formatId path param as String
     * @param request
     * @return Response
     */
    @GET
    @Path("format/{id_format:.+}")
    @Produces(APPLICATION_JSON)
    public Response findFileFormatByID(@PathParam("id_format") String formatId, @Context Request request) {
        ParametersChecker.checkParameter("formatId is a mandatory parameter", formatId);
        FileFormat fileFormat = null;
        try (ReferentialFormatFileImpl formatManagement =
            new ReferentialFormatFileImpl(mongoAccess, vitamCounterService)) {
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(formatId));
            fileFormat = formatManagement.findDocumentById(formatId);
            if (fileFormat == null) {
                LOGGER.error("NO DATA for the specified formatId '" + formatId + "'");
                return Response.status(Status.NOT_FOUND)
                    .entity(getErrorEntity(Status.NOT_FOUND, "Format not found")).build();
            }

            CacheControl cacheControl = new CacheControl();
            cacheControl.setMaxAge(VitamConfiguration.getCacheControlDelay());
            cacheControl.setPrivate(false);

            EntityTag etag = new EntityTag(Integer.toString(fileFormat.hashCode()));
            // determine the current version has the same "ETag" value,
            // the browser’s cached copy "Etag" value passed by If-None-Match header
            ResponseBuilder builder = request.evaluatePreconditions(etag);
            // did cached resource change?
            if (builder == null) {
                // resource is modified so server new content
                // 200 OK status code is returned with new content
                return Response.status(Status.OK).entity(new RequestResponseOK()
                    .addResult(JsonHandler.toJsonNode(fileFormat)).setHttpCode(Status.OK.getStatusCode())).tag(etag)
                    .cacheControl(cacheControl).build();
            }

            return builder.cacheControl(cacheControl).tag(etag).build();
        } catch (final Exception e) {
            LOGGER.error(e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(getErrorEntity(Status.INTERNAL_SERVER_ERROR, e.getMessage())).build();
        }
    }

    /**
     * retrieve all the file format inserted in the collection fileFormat
     *
     * @param select as String the query to get format
     * @return Response
     */
    @Path("format/document")
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response findFormats(JsonNode select) {
        ParametersChecker.checkParameter(SELECT_IS_A_MANDATORY_PARAMETER, select);
        RequestResponseOK<FileFormat> fileFormatList;
        try (ReferentialFormatFileImpl formatManagement =
            new ReferentialFormatFileImpl(mongoAccess, vitamCounterService)) {
            SanityChecker.checkJsonAll(select);
            fileFormatList = formatManagement.findDocuments(select).setQuery(select);
            return Response.status(Status.OK)
                .entity(fileFormatList).build();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST)
                .entity(getErrorEntity(Status.BAD_REQUEST, e.getMessage())).build();
        } catch (final Exception e) {
            LOGGER.error(e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(getErrorEntity(Status.INTERNAL_SERVER_ERROR, e.getMessage())).build();
        }
    }

    /**
     * check the rules file
     *
     * @param rulesStream as InputStream
     * @return Response
     */
    @Path("rules/check")
    @POST
    @Consumes(APPLICATION_OCTET_STREAM)
    @Produces(APPLICATION_OCTET_STREAM)
    public Response checkRulesFile(InputStream rulesStream) {
        ParametersChecker.checkParameter("rulesStream is a mandatory parameter", rulesStream);
        return downloadErrorReport(rulesStream);
    }


    /**
     * async Download Report
     *
     * @param document the document to check
     */
    private Response downloadErrorReport(InputStream document) {
        Map<Integer, List<ErrorReport>> errors = new HashMap<Integer, List<ErrorReport>>();
        List<FileRulesModel> usedDeletedRules = new ArrayList<>();
        List<FileRulesModel> usedUpdatedRules = new ArrayList<>();
        List<FileRulesModel> usedUpdateRulesForUpdateUnit = new ArrayList<>();
        List<FileRulesModel> insertRules = new ArrayList<>();
        Set<String> notUsedDeletedRules = new HashSet<>();
        Set<String> notUsedUpdatedRules = new HashSet<>();
        try {
            RulesManagerFileImpl rulesManagerFileImpl = new RulesManagerFileImpl(mongoAccess, vitamCounterService, this.rulesOntologyLoader);

            try {
                rulesManagerFileImpl
                    .checkFile(document, errors, usedDeletedRules, usedUpdatedRules, usedUpdateRulesForUpdateUnit,
                        insertRules,
                        notUsedDeletedRules, notUsedUpdatedRules);
            } catch (FileRulesUpdateException exc) {
                LOGGER.warn("used Rules ({}) want to be updated",
                    usedUpdatedRules != null ? usedUpdatedRules.toString() : "");
            }
            InputStream errorReportInputStream =
                rulesManagerFileImpl.generateErrorReport(errors, usedDeletedRules, usedUpdatedRules, StatusCode.OK,
                    null);
            Map<String, String> headers = new HashMap<>();
            headers.put(HttpHeaders.CONTENT_TYPE, APPLICATION_OCTET_STREAM);
            headers.put(HttpHeaders.CONTENT_DISPOSITION, ATTACHMENT_FILENAME);
            return new VitamAsyncInputStreamResponse(errorReportInputStream,
                Status.OK, headers);
        } catch (Exception e) {
            LOGGER.error("Error while checking file ", e);
            return handleGenerateReport(errors, usedDeletedRules, usedUpdatedRules, rulesOntologyLoader);
        }
    }

    /**
     * Handle Generation of the report in case of exception
     *
     * @param errors
     * @param usedDeletedRules
     * @param usedDeletedRules
     * @param usedUpdatedRules
     * @param ontologyLoader
     * @return response
     */
    private Response handleGenerateReport(Map<Integer, List<ErrorReport>> errors,
        List<FileRulesModel> usedDeletedRules, List<FileRulesModel> usedUpdatedRules, OntologyLoader ontologyLoader) {
        InputStream errorReportInputStream;
        RulesManagerFileImpl rulesManagerFileImpl = new RulesManagerFileImpl(mongoAccess, vitamCounterService, ontologyLoader);
        errorReportInputStream =
            rulesManagerFileImpl.generateErrorReport(errors, usedDeletedRules, usedUpdatedRules, StatusCode.KO,
                null);
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.CONTENT_TYPE, APPLICATION_OCTET_STREAM);
        headers.put(HttpHeaders.CONTENT_DISPOSITION, ATTACHMENT_FILENAME);
        return new VitamAsyncInputStreamResponse(errorReportInputStream,
            Status.BAD_REQUEST, headers);
    }

    /**
     * import the rules file
     *
     * @param headers http headers
     * @param rulesStream as InputStream
     * @return Response
     */
    @Path("rules/import")
    @POST
    @Consumes(APPLICATION_OCTET_STREAM)
    @Produces(APPLICATION_JSON)
    public Response importRulesFile(@Context HttpHeaders headers, InputStream rulesStream) {
        ParametersChecker.checkParameter("rulesStream is a mandatory parameter", rulesStream);
        String filename = headers.getHeaderString(GlobalDataRest.X_FILENAME);
        try {
            RulesManagerFileImpl rulesFileManagement = new RulesManagerFileImpl(mongoAccess, vitamCounterService, this.rulesOntologyLoader);

            rulesFileManagement.importFile(rulesStream, filename);
            return Response.status(Status.CREATED).entity(Status.CREATED.getReasonPhrase()).build();
        } catch (final FileRulesImportInProgressException e) {
            LOGGER.warn(e);
            return Response.status(Status.FORBIDDEN).entity(e.getMessage()).build();
        } catch (final FileRulesException | FileRulesCsvException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage())
                .build();
        } catch (final ReferentialException e) {
            LOGGER.error(e);
            return Response.status(Status.CONFLICT).entity(e.getMessage())
                .build();
        } catch (final Exception e) {
            LOGGER.error(e);
            final Status status = INTERNAL_SERVER_ERROR;
            return Response.status(status).entity(status.getReasonPhrase()).build();
        } finally {
            StreamUtils.closeSilently(rulesStream);
        }

    }

    /**
     * findRuleByID : find the rules details based on a given Id
     *
     * @param ruleId path param as String
     * @param request the request
     * @return Response
     */
    @GET
    @Path("rules/{id_rule}")
    @Produces(APPLICATION_JSON)
    public Response findRuleByID(@PathParam("id_rule") String ruleId, @Context Request request) {
        ParametersChecker.checkParameter("ruleId is a mandatory parameter", ruleId);
        FileRules fileRules;
        try {
            RulesManagerFileImpl rulesFileManagement = new RulesManagerFileImpl(mongoAccess, vitamCounterService, this.rulesOntologyLoader);

            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(ruleId));
            fileRules = rulesFileManagement.findDocumentById(ruleId);
            if (fileRules == null) {
                LOGGER.error("NO DATA for the specified rule Value or More than one records exists '" + ruleId + "'");
                return Response.status(Status.NOT_FOUND)
                    .entity(getErrorEntity(Status.NOT_FOUND, "Rule not found")).build();
            }

            CacheControl cacheControl = new CacheControl();
            cacheControl.setMaxAge(VitamConfiguration.getCacheControlDelay());
            cacheControl.setPrivate(false);

            EntityTag etag = new EntityTag(Integer.toString(fileRules.hashCode()));
            // determine the current version has the same "ETag" value,
            // the browser’s cached copy "Etag" value passed by If-None-Match header
            ResponseBuilder builder = request.evaluatePreconditions(etag);
            // did cached resource change?
            if (builder == null) {
                // resource is modified so server new content
                // 200 OK status code is returned with new content
                return Response.status(Status.OK).entity(new RequestResponseOK()
                    .addResult(JsonHandler.toJsonNode(fileRules)).setHttpCode(Status.OK.getStatusCode())).tag(etag)
                    .cacheControl(cacheControl).build();
            }

            return builder.cacheControl(cacheControl).tag(etag).build();
        } catch (final Exception e) {
            LOGGER.error(e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(getErrorEntity(Status.INTERNAL_SERVER_ERROR, e.getMessage())).build();
        }
    }

    /**
     * show all file rules inserted in the collection fileRules
     *
     * @param select as String
     * @return Response
     */
    @Path("rules/document")
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response findDocumentRules(JsonNode select) {
        ParametersChecker.checkParameter(SELECT_IS_A_MANDATORY_PARAMETER, select);
        RequestResponseOK<FileRules> filerulesList;
        try {
            RulesManagerFileImpl rulesFileManagement = new RulesManagerFileImpl(mongoAccess, vitamCounterService, this.rulesOntologyLoader);
            SanityChecker.checkJsonAll(select);
            filerulesList = rulesFileManagement.findDocuments(select).setQuery(select);
            return Response.status(Status.OK)
                .entity(filerulesList)
                .build();

        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST)
                .entity(getErrorEntity(Status.BAD_REQUEST, e.getMessage())).build();
        } catch (final Exception e) {
            LOGGER.error(e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(getErrorEntity(Status.INTERNAL_SERVER_ERROR, e.getMessage())).build();
        }
    }

    /**
     * create or update an accession register
     *
     * @param accessionRegister AccessionRegisterDetail object
     * @return Response
     */
    @Path("accession-register")
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response createAccessionRegister(AccessionRegisterDetailModel accessionRegister) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("register ID / Originating Agency: " + accessionRegister.getId() + " / " +
                accessionRegister.getOriginatingAgency());
        }
        ParametersChecker.checkParameter("Accession Register is a mandatory parameter", accessionRegister);
        try (ReferentialAccessionRegisterImpl accessionRegisterManagement =
            new ReferentialAccessionRegisterImpl(mongoAccess, vitamCounterService)) {
            accessionRegisterManagement.createOrUpdateAccessionRegister(accessionRegister);
            return Response.status(Status.CREATED).build();
        } catch (final BadRequestException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST)
                .entity(getErrorEntity(Status.BAD_REQUEST, e.getMessage())).build();
        } catch (final ReferentialException e) {
            LOGGER.error(e);
            if (DbRequestSingle.checkInsertOrUpdate(e)) {
                // Accession register detail already exists in database
                VitamError ve = new VitamError(Status.CONFLICT.name()).setHttpCode(Status.CONFLICT.getStatusCode())
                    .setContext(ServiceName.EXTERNAL_ACCESS.getName())
                    .setState("code_vitam")
                    .setMessage(Status.CONFLICT.getReasonPhrase())
                    .setDescription("Document already exists in database");

                return Response.status(Status.CONFLICT).entity(ve).build();
            }
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(getErrorEntity(Status.INTERNAL_SERVER_ERROR, e.getMessage())).build();
        } catch (final Exception e) {
            LOGGER.error(e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(getErrorEntity(Status.INTERNAL_SERVER_ERROR, e.getMessage())).build();
        }
    }

    /**
     * retrieve all accession summary from accession summary collection
     *
     * @param select as String the query to find accession register
     * @return Response
     */
    @Path("accession-register/document")
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response findDocumentFundsRegister(JsonNode select) {
        ParametersChecker.checkParameter(SELECT_IS_A_MANDATORY_PARAMETER, select);
        RequestResponseOK<AccessionRegisterSummary> fileFundRegisters;
        try {
            fileFundRegisters = findFundRegisters(select);
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST)
                .entity(getErrorEntity(Status.BAD_REQUEST, e.getMessage())).build();
        } catch (AccessUnauthorizedException e) {
            LOGGER.error("Access contract does not allow ", e);
            return Response.status(Status.UNAUTHORIZED)
                .entity(getErrorEntity(Status.UNAUTHORIZED, e.getMessage())).build();
        } catch (final Exception e) {
            LOGGER.error(e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(getErrorEntity(Status.INTERNAL_SERVER_ERROR, e.getMessage())).build();
        }

        return Response.status(Status.OK)
            .entity(fileFundRegisters)
            .build();
    }

    private RequestResponseOK<AccessionRegisterSummary> findFundRegisters(JsonNode select)
        throws InvalidParseOperationException, AccessUnauthorizedException, InvalidCreateOperationException,
        ReferentialException {
        try (ReferentialAccessionRegisterImpl accessionRegisterManagement =
            new ReferentialAccessionRegisterImpl(mongoAccess, vitamCounterService)) {

            RequestResponseOK<AccessionRegisterSummary> fileFundRegisters;
            SanityChecker.checkJsonAll(select);

            if (StringUtils.isBlank(VitamThreadUtils.getVitamSession().getContractId())) {
                throw new AccessUnauthorizedException("No contract chosen");
            }
            AccessContractModel contract = getContractDetails(VitamThreadUtils.getVitamSession().getContractId());
            if (contract == null) {
                throw new AccessUnauthorizedException("Contract Not Found");
            }
            boolean isEveryOriginatingAgency = contract.getEveryOriginatingAgency();
            Set<String> prodServices = contract.getOriginatingAgencies();

            SelectParserSingle parser = new SelectParserSingle(DEFAULT_VARNAME_ADAPTER);
            parser.parse(select);

            if (!isEveryOriginatingAgency) {
                parser.addCondition(QueryHelper.in(ORIGINATING_AGENCY,
                    prodServices.toArray(new String[0])));
            }

            fileFundRegisters = accessionRegisterManagement.findDocuments(parser.getRequest().getFinalSelect())
                .setQuery(select);
            return fileFundRegisters;
        }
    }

    /**
     * retrieve accession register detail based on a given dsl query
     *
     * @param originatingAgency
     * @param select as String the query to find the accession register
     * @return Response
     */
    @Path("accession-register/detail/{id}")
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response findDetailAccessionRegister(@PathParam("id") String originatingAgency, JsonNode select) {
        ParametersChecker.checkParameter(SELECT_IS_A_MANDATORY_PARAMETER, select);
        RequestResponseOK<AccessionRegisterDetail> accessionRegisterDetails;
        try (ReferentialAccessionRegisterImpl accessionRegisterManagement =
            new ReferentialAccessionRegisterImpl(mongoAccess, vitamCounterService)) {
            SanityChecker.checkJsonAll(select);
            SanityChecker.checkParameter(originatingAgency);

            SelectParserSingle parser = new SelectParserSingle(DEFAULT_VARNAME_ADAPTER);
            parser.parse(select);
            AccessContractModel contract = getContractDetails(VitamThreadUtils.getVitamSession().getContractId());
            if (contract == null) {
                throw new AccessUnauthorizedException("Contract Not Found");
            }
            Boolean isEveryOriginatingAgency = contract.getEveryOriginatingAgency();
            Set<String> prodServices = contract.getOriginatingAgencies();

            if (!isEveryOriginatingAgency && !prodServices.contains(originatingAgency)) {
                return Response.status(Status.UNAUTHORIZED)
                    .entity(getErrorEntity(Status.UNAUTHORIZED, "Unauthorized")).build();
            }
            if (!isEveryOriginatingAgency) {
                parser.addCondition(QueryHelper.in(ORIGINATING_AGENCY,
                    prodServices.toArray(new String[0])).setDepthLimit(0));
            }
            parser.addCondition(
                eq(ORIGINATING_AGENCY, URLDecoder.decode(originatingAgency, CharsetUtils.UTF_8)));

            accessionRegisterDetails =
                accessionRegisterManagement.findDetail(parser.getRequest().getFinalSelect()).setQuery(select);

        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST)
                .entity(getErrorEntity(Status.BAD_REQUEST, e.getMessage())).build();
        } catch (final Exception e) {
            LOGGER.error(e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(getErrorEntity(Status.INTERNAL_SERVER_ERROR, e.getMessage())).build();
        }

        return Response.status(Status.OK)
            .entity(accessionRegisterDetails)
            .build();
    }

    /**
     * retrieve accession register detail based on a given dsl query
     *
     * @param select as String the query to find the accession register
     * @return Response
     */
    @Path("accession-register/detail")
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response findDetailAccessionRegister(JsonNode select) {
        ParametersChecker.checkParameter(SELECT_IS_A_MANDATORY_PARAMETER, select);
        RequestResponseOK<AccessionRegisterDetail> accessionRegisterDetails;
        try (ReferentialAccessionRegisterImpl accessionRegisterManagement =
            new ReferentialAccessionRegisterImpl(mongoAccess, vitamCounterService)) {
            SanityChecker.checkJsonAll(select);

            SelectParserSingle parser = new SelectParserSingle(DEFAULT_VARNAME_ADAPTER);
            parser.parse(select);

            accessionRegisterDetails =
                accessionRegisterManagement.findDetail(parser.getRequest().getFinalSelect()).setQuery(select);

        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST)
                .entity(getErrorEntity(Status.BAD_REQUEST, e.getMessage())).build();
        } catch (final Exception e) {
            LOGGER.error(e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(getErrorEntity(Status.INTERNAL_SERVER_ERROR, e.getMessage())).build();
        }

        return Response.status(Status.OK)
            .entity(accessionRegisterDetails)
            .build();
    }

    @Path(AUDIT_RULE_URI)
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response launchRuleAudit() {
        RulesManagerFileImpl rulesManagerFileImpl = new RulesManagerFileImpl(mongoAccess, vitamCounterService, rulesOntologyLoader);
        int tenant = VitamThreadUtils.getVitamSession().getTenantId();
        try {
            rulesManagerFileImpl.checkRuleConformity(rulesManagerFileImpl.getRuleFromCollection(tenant),
                rulesManagerFileImpl.getRuleFromOffer(tenant), tenant);
        } catch (InvalidParseOperationException e) {
            LOGGER.error(e);
            final Status status = INTERNAL_SERVER_ERROR;
            return Response.status(status).entity(getErrorEntity(status, e.getLocalizedMessage())).build();
        }
        return Response.status(Status.ACCEPTED).entity(new RequestResponseOK()
            .setHttpCode(Status.ACCEPTED.getStatusCode())).build();
    }

    /**
     * Launch audit with options
     *
     * @param options
     * @return Response
     */
    @Path(AUDIT_URI)
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response launchAudit(AuditOptions options) {
        ParametersChecker.checkParameter(OPTIONS_IS_MANDATORY_PARAMETER, options);
        try (ProcessingManagementClient processingClient = ProcessingManagementClientFactory.getInstance().getClient();
            WorkspaceClient workspaceClient = WorkspaceClientFactory.getInstance().getClient()) {
            final int tenantId = VitamThreadUtils.getVitamSession().getTenantId();
            final String operationId = VitamThreadUtils.getVitamSession().getRequestId();

            AccessContractModel contract = getContractDetails(VitamThreadUtils.getVitamSession().getContractId());
            if (contract == null) {
                throw new AccessUnauthorizedException("Contract Not Found");
            }

            validateAuditOptions(options, tenantId);

            final ProcessingEntry entry = new ProcessingEntry(operationId, Contexts.AUDIT_WORKFLOW.name());

            entry.getExtraParams().put(AUDIT_TYPE, options.getAuditType());
            entry.getExtraParams().put(OBJECT_ID, options.getObjectId());
            entry.getExtraParams().put(AUDIT_ACTIONS, options.getAuditActions());

            workspaceClient.createContainer(operationId);

            createAuditLogbookOperation(operationId, contract);

            SelectMultiQuery selectQuery = new SelectMultiQuery();
            if ("tenant".equals(options.getAuditType())) {
                selectQuery.setQuery(
                    QueryHelper.eq(BuilderToken.PROJECTIONARGS.TENANT.exactToken(), options.getObjectId()));
            } else if ("originatingagency".equals(options.getAuditType())) {
                selectQuery.setQuery(QueryHelper.in(BuilderToken.PROJECTIONARGS.ORIGINATING_AGENCY.exactToken(),
                    options.getObjectId()));
            } else if ("dsl".equals(options.getAuditType())) {
                final SelectParserMultiple parser = new SelectParserMultiple();
                parser.parse(options.getQuery().deepCopy());
                selectQuery = parser.getRequest();
            }

            JsonNode restrictedQuery = AccessContractRestrictionHelper
                .applyAccessContractRestrictionForObjectGroupForSelect(selectQuery.getFinalSelect(), contract);
            // for CheckThresholdHandler
            workspaceClient.putObject(operationId, "query.json", JsonHandler.writeToInpustream(restrictedQuery));

            processingClient.initVitamProcess(entry);
            processingClient.updateOperationActionProcess(ProcessAction.RESUME.getValue(), operationId);

            return Response.status(Status.ACCEPTED)
                .entity(new RequestResponseOK().setHttpCode(Status.ACCEPTED.getStatusCode())).build();
        } catch (BadRequestException exp) {
            LOGGER.error(exp);
            final Status status = Status.PRECONDITION_FAILED;
            return Response.status(status).entity(getErrorEntity(status, exp.getMessage())).build();
        } catch (Exception exp) {
            LOGGER.error(exp);
            final Status status = INTERNAL_SERVER_ERROR;
            return Response.status(status).entity(getErrorEntity(status, exp.getLocalizedMessage())).build();
        }
    }

    @POST
    @Path("accession-register/symbolic")
    @Produces(APPLICATION_JSON)
    public Response createAccessionRegisterSymbolic() {
        try (ReferentialAccessionRegisterImpl service = new ReferentialAccessionRegisterImpl(
            mongoAccess, vitamCounterService)) {

            MetaDataClient client = MetaDataClientFactory.getInstance().getClient();
            ArrayNode accessionRegisterSymbolic = (ArrayNode) client.createAccessionRegisterSymbolic()
                .get("$results");

            List<AccessionRegisterSymbolic> accessionRegisterSymbolicsToInsert =
                StreamSupport.stream(accessionRegisterSymbolic.spliterator(), false)
                    .map(AccessionRegisterSymbolic::new)
                    .collect(Collectors.toList());

            if (accessionRegisterSymbolicsToInsert.isEmpty()) {
                return Response.status(NO_CONTENT).build();
            }

            service.insertAccessionRegisterSymbolic(accessionRegisterSymbolicsToInsert);

            return Response.status(OK).build();
        } catch (Exception e) {
            LOGGER.error(e);
            return Response.status(INTERNAL_SERVER_ERROR)
                .entity(getErrorEntity(INTERNAL_SERVER_ERROR, e.getLocalizedMessage()))
                .build();
        }
    }

    @GET
    @Path("accession-register/symbolic")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response getAccessionRegisterSymbolic(JsonNode queryDsl) {
        try (ReferentialAccessionRegisterImpl service = new ReferentialAccessionRegisterImpl(mongoAccess,
            vitamCounterService)) {

            SanityChecker.checkJsonAll(queryDsl);

            SelectParserSingle parser = new SelectParserSingle(DEFAULT_VARNAME_ADAPTER);
            parser.parse(queryDsl);
            AccessContractModel contract = getContractDetails(VitamThreadUtils.getVitamSession().getContractId());
            if (contract == null || contract.getEveryOriginatingAgency() == null) {
                throw new AccessUnauthorizedException("Contract Not Found or EveryOriginatingAgency flag is null");
            }

            if (!contract.getEveryOriginatingAgency()) {
                parser.addCondition(QueryHelper.in(ORIGINATING_AGENCY,
                    contract.getOriginatingAgencies().stream().toArray(String[]::new)).setDepthLimit(0));
            }

            List<AccessionRegisterSymbolic> accessionRegisterSymbolic = service.findAccessionRegisterSymbolic(queryDsl);
            RequestResponseOK<AccessionRegisterSymbolic> entity = new RequestResponseOK<AccessionRegisterSymbolic>()
                .addAllResults(accessionRegisterSymbolic)
                .setQuery(queryDsl);
            return Response.status(OK)
                .entity(entity)
                .build();
        } catch (Exception e) {
            LOGGER.error(e);
            return Response.status(INTERNAL_SERVER_ERROR)
                .entity(getErrorEntity(INTERNAL_SERVER_ERROR, e.getLocalizedMessage()))
                .build();
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
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response forcePause(ProcessPause info) {

        ParametersChecker.checkParameter("Json ProcessPause is a mandatory parameter", info);
        try (ProcessingManagementClient processingClient = ProcessingManagementClientFactory.getInstance()
            .getClient()) {
            RequestResponse requestResponse = processingClient.forcePause(info);
            return Response.status(requestResponse.getStatus())
                .entity(requestResponse).build();

        } catch (final ProcessingException e) {
            LOGGER.error(e);
            final Status status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status).entity(getErrorEntity(status, e.getLocalizedMessage())).build();
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
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeForcePause(ProcessPause info) {

        ParametersChecker.checkParameter("Json ProcessPause is a mandatory parameter", info);
        try (ProcessingManagementClient processingClient = ProcessingManagementClientFactory.getInstance()
            .getClient()) {
            RequestResponse requestResponse = processingClient.removeForcePause(info);
            return Response.status(requestResponse.getStatus())
                .entity(requestResponse).build();

        } catch (final ProcessingException e) {
            LOGGER.error(e);
            final Status status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status).entity(getErrorEntity(status, e.getLocalizedMessage())).build();
        }
    }

    private void validateAuditOptions(AuditOptions options, final int tenantId)
        throws InvalidCreateOperationException,
        InvalidParseOperationException, AccessUnauthorizedException, ReferentialException, BadRequestException {
        String errorMessage = null;
        if (options.getAuditType() == null) {
            errorMessage = "The field auditType is mandatory";
        } else if (options.getAuditActions() == null) {
            errorMessage = "The field auditActions is mandatory";
        } else {
            switch (options.getAuditType()) {
                case "tenant":
                    if (StringUtils.isBlank(options.getObjectId())) {
                        errorMessage = "The field objectId is mandatory with auditType " + options.getAuditType();
                    } else if (!options.getObjectId().equals(String.valueOf(tenantId))) {
                        errorMessage = "Invalid tenant parameter";
                    }
                    break;
                case "originatingagency":
                    if (StringUtils.isBlank(options.getObjectId())) {
                        errorMessage = "The field objectId is mandatory with auditType " + options.getAuditType();
                    } else {
                        RequestResponseOK<AccessionRegisterSummary> fileFundRegisters;
                        Select selectRequest = new Select();
                        selectRequest.setQuery(QueryHelper.eq(ORIGINATING_AGENCY, options.getObjectId()));
                        fileFundRegisters = findFundRegisters(selectRequest.getFinalSelect());
                        if (fileFundRegisters.getResults().size() == 0) {
                            errorMessage = "Invalid originating agency parameter";
                        }
                    }
                    break;
                case "dsl":
                    if (options.getQuery() == null) {
                        errorMessage = "The field query is mandatory with auditType " + options.getAuditType();
                    }
                    break;
                default:
                    errorMessage = "Invalid audit type parameter";
                    break;
            }
        }
        if (errorMessage != null) {
            throw new BadRequestException(errorMessage);
        }
    }

    private AccessContractModel getContractDetails(String contractId)
        throws InvalidCreateOperationException, ReferentialException, InvalidParseOperationException {

        try (ContractService<AccessContractModel> accessContract = new AccessContractImpl(mongoAccess,
            vitamCounterService)) {

            final RequestResponseOK<AccessContractModel> accessContractModelList =
                accessContract.findContracts(getQueryDsl(contractId)).setQuery(JsonHandler.createObjectNode());
            return Iterables.getOnlyElement(accessContractModelList.getResults(), null);

        }
    }

    private static JsonNode getQueryDsl(String headerAccessContratId) throws InvalidCreateOperationException {
        Select select = new Select();
        Query query = QueryHelper.and().add(QueryHelper.eq(AccessContract.IDENTIFIER, headerAccessContratId),
            QueryHelper.eq(AccessContract.STATUS, ActivationStatus.ACTIVE.name()));
        select.setQuery(query);
        return select.getFinalSelect();
    }

    /**
     * @param vitamCounterService
     */
    public void setVitamCounterService(VitamCounterService vitamCounterService) {
        this.vitamCounterService = vitamCounterService;
    }


    private void createAuditLogbookOperation(String operationId, AccessContractModel contract)
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException,
        InvalidGuidOperationException, InvalidCreateOperationException, ReferentialException,
        InvalidParseOperationException {
        final GUID objectId = GUIDReader.getGUID(operationId);

        try (final LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient()) {
            final LogbookOperationParameters initParameters = LogbookParametersFactory.newLogbookOperationParameters(
                objectId, Contexts.AUDIT_WORKFLOW.getEventType(), objectId, LogbookTypeProcess.AUDIT,
                StatusCode.STARTED,
                VitamLogbookMessages.getCodeOp(Contexts.AUDIT_WORKFLOW.getEventType(), StatusCode.STARTED),
                objectId);

            ObjectNode rightsStatementIdentifier = JsonHandler.createObjectNode();
            rightsStatementIdentifier.put(ACCESS_CONTRACT, contract.getIdentifier());
            initParameters.putParameterValue(LogbookParameterName.rightsStatementIdentifier,
                rightsStatementIdentifier.toString());

            logbookClient.create(initParameters);
        }
    }

    /**
     * Create a new logbook operation entry in Vitam
     *
     * @param operation the LogbookOperationParameters
     * @return Response contains the list of logbook operations
     */
    @POST
    @Path(OPERATIONS_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addExternalOperation(LogbookOperationParameters operation) {
        try (final LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient()) {

            if (!LogbookTypeProcess.EXTERNAL_LOGBOOK.equals(operation.getTypeProcess())) {
                throw new IllegalArgumentException(
                    "This type of process is not correct :" + operation.getTypeProcess());
            } else if (operation.getParameterValue(LogbookParameterName.eventType) == null ||
                !operation.getParameterValue(LogbookParameterName.eventType).startsWith("EXT_")) {
                throw new IllegalArgumentException(
                    "This type of event is not correct :" +
                        operation.getParameterValue(LogbookParameterName.eventType));
            }

            final VitamSession vitamSession = VitamThreadUtils.getVitamSession();
            String contextId = vitamSession.getContextId();
            String personalCertificate = vitamSession.getPersonalCertificate();
            final GUID eip = GUIDReader.getGUID(vitamSession.getRequestId());

            LogbookOperationParameters masterOperation = LogbookParametersFactory.newLogbookOperationParameters();

            masterOperation.setTypeProcess(LogbookTypeProcess.EXTERNAL_LOGBOOK)
                .setStatus(StatusCode.OK)
                .putParameterValue(LogbookParameterName.eventIdentifier, eip.getId())
                .putParameterValue(LogbookParameterName.eventIdentifierProcess, eip.getId())
                .putParameterValue(LogbookParameterName.eventIdentifierRequest, eip.getId())
                .putParameterValue(LogbookParameterName.objectIdentifier, eip.getId())
                .putParameterValue(LogbookParameterName.eventType,
                    operation.getParameterValue(LogbookParameterName.eventType))
                .putParameterValue(LogbookParameterName.outcomeDetailMessage,
                    VitamLogbookMessages.getCodeOp(LogbookTypeProcess.EXTERNAL_LOGBOOK.name(), StatusCode.OK))
                .putParameterValue(LogbookParameterName.outcomeDetail, VitamLogbookMessages
                    .getOutcomeDetail(operation.getParameterValue(LogbookParameterName.eventType), StatusCode.STARTED))
                .putParameterValue(LogbookParameterName.agentIdentifierApplication, contextId)
                .putParameterValue(LogbookParameterName.agentIdentifierPersonae, personalCertificate)
                .putParameterValue(LogbookParameterName.eventDateTime, LocalDateUtil.now().toString());

            if (operation.getParameterValue(LogbookParameterName.agentIdentifierApplicationSession) != null) {
                masterOperation.putParameterValue(LogbookParameterName.agentIdentifierApplicationSession,
                    operation.getParameterValue(LogbookParameterName.agentIdentifierApplicationSession));
            }
            if (operation.getParameterValue(LogbookParameterName.agIdExt) != null) {
                JsonHandler.validate(operation.getParameterValue(LogbookParameterName.agIdExt));
                masterOperation.putParameterValue(LogbookParameterName.agIdExt,
                    operation.getParameterValue(LogbookParameterName.agIdExt));
            }
            if (operation.getParameterValue(LogbookParameterName.objectIdentifierIncome) != null) {
                masterOperation.putParameterValue(LogbookParameterName.objectIdentifierIncome,
                    operation.getParameterValue(LogbookParameterName.objectIdentifierIncome));
            }

            masterOperation.setEvents(operation.getEvents());

            // split here so we can do things as multiple calls : one creation, then, multiple updates
            logbookClient.create(masterOperation);
            if (masterOperation.getEvents() != null && masterOperation.getEvents().size() > 0) {
                for (final LogbookParameters param : masterOperation.getEvents()) {
                    param.putParameterValue(LogbookParameterName.eventIdentifier, eip.getId());
                    param.putParameterValue(LogbookParameterName.eventIdentifierProcess, eip.getId());
                    logbookClient.update((LogbookOperationParameters) param);
                }
            }
            return Response.status(Status.CREATED).entity(new RequestResponseOK()
                .setHttpCode(Status.CREATED.getStatusCode())).build();
        } catch (LogbookClientBadRequestException | IllegalArgumentException | InvalidParseOperationException | InvalidGuidOperationException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST)
                .entity(getErrorEntity(Status.BAD_REQUEST, e.getMessage())).build();
        } catch (LogbookClientAlreadyExistsException e) {
            LOGGER.error(e);
            return Response.status(Status.CONFLICT)
                .entity(getErrorEntity(Status.CONFLICT, e.getMessage())).build();
        } catch (LogbookClientServerException e) {
            LOGGER.error(e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(getErrorEntity(Status.INTERNAL_SERVER_ERROR, e.getMessage())).build();
        } catch (LogbookClientNotFoundException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST)
                .entity(getErrorEntity(Status.BAD_REQUEST, e.getMessage())).build();
        }
    }

    private VitamError getErrorEntity(Status status, String message) {
        String aMessage =
            (message != null && !message.trim().isEmpty()) ? message
                : (status.getReasonPhrase() != null ? status.getReasonPhrase() : status.name());
        return new VitamError(status.name()).setHttpCode(status.getStatusCode())
            .setContext(FUNCTIONAL_ADMINISTRATION_MODULE)
            .setState(status.name())
            .setMessage(status.getReasonPhrase()).setDescription(aMessage);
    }
}
