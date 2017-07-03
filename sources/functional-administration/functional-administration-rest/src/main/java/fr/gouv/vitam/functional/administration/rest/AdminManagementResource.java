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
package fr.gouv.vitam.functional.administration.rest;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterables;

import fr.gouv.vitam.common.CharsetUtils;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.parser.request.adapter.SingleVarNameAdapter;
import fr.gouv.vitam.common.database.parser.request.single.SelectParserSingle;
import fr.gouv.vitam.common.exception.AccessUnauthorizedException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.AccessContractModel;
import fr.gouv.vitam.common.model.ContractStatus;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.server.application.configuration.DbConfigurationImpl;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.common.server.application.resources.BasicVitamStatusServiceImpl;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.accession.register.core.ReferentialAccessionRegisterImpl;
import fr.gouv.vitam.functional.administration.common.AccessContract;
import fr.gouv.vitam.functional.administration.common.AccessionRegisterDetail;
import fr.gouv.vitam.functional.administration.common.AccessionRegisterSummary;
import fr.gouv.vitam.functional.administration.common.FileFormat;
import fr.gouv.vitam.functional.administration.common.FileRules;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.functional.administration.common.exception.DatabaseConflictException;
import fr.gouv.vitam.functional.administration.common.exception.FileFormatNotFoundException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesImportInProgressException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialNotFoundException;
import fr.gouv.vitam.functional.administration.common.server.AdminManagementConfiguration;
import fr.gouv.vitam.functional.administration.common.server.ElasticsearchAccessAdminFactory;
import fr.gouv.vitam.functional.administration.common.server.ElasticsearchAccessFunctionalAdmin;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminFactory;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.functional.administration.contract.api.ContractService;
import fr.gouv.vitam.functional.administration.contract.core.AccessContractImpl;
import fr.gouv.vitam.functional.administration.counter.VitamCounterService;
import fr.gouv.vitam.functional.administration.format.core.ReferentialFormatFileImpl;
import fr.gouv.vitam.functional.administration.rules.core.RulesManagerFileImpl;
import fr.gouv.vitam.functional.administration.rules.core.RulesSecurisator;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

/**
 * FormatManagementResourceImpl implements AccessResource
 */
@Path("/adminmanagement/v1")
@javax.ws.rs.ApplicationPath("webresources")
public class AdminManagementResource extends ApplicationStatusResource {

    private static final String SELECT_IS_A_MANDATORY_PARAMETER = "select is a mandatory parameter";

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AdminManagementResource.class);

    private static final SingleVarNameAdapter DEFAULT_VARNAME_ADAPTER = new SingleVarNameAdapter();
    private final MongoDbAccessAdminImpl mongoAccess;
    private final ElasticsearchAccessFunctionalAdmin elasticsearchAccess;
    private VitamCounterService vitamCounterService;
    private RulesSecurisator securisator = new RulesSecurisator();

    /**
     * Constructor
     *
     * @param configuration config for constructing AdminManagement
     */
    public AdminManagementResource(AdminManagementConfiguration configuration) {
        super(new BasicVitamStatusServiceImpl(), configuration.getTenants());
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
        /// FIXME: 3/31/17 Factories mustn't be created here !!!
        elasticsearchAccess = ElasticsearchAccessAdminFactory.create(configuration);
        mongoAccess = MongoDbAccessAdminFactory.create(adminConfiguration);
        WorkspaceClientFactory.changeMode(configuration.getWorkspaceUrl());

        LOGGER.debug("init Admin Management Resource server");
    }

    @VisibleForTesting
    AdminManagementResource(AdminManagementConfiguration configuration,
        RulesSecurisator securisator) {
        this(configuration);
        this.securisator = securisator;
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
     * @return Response response jersey
     */
    @Path("format/check")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkFormat(InputStream xmlPronom) {
        ParametersChecker.checkParameter("xmlPronom is a mandatory parameter", xmlPronom);
        try (ReferentialFormatFileImpl formatManagement = new ReferentialFormatFileImpl(mongoAccess)) {
            formatManagement.checkFile(xmlPronom);
            return Response.status(Status.OK).build();
        } catch (final ReferentialException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (final Exception e) {
            LOGGER.error(e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        } finally {
            StreamUtils.closeSilently(xmlPronom);
        }
    }


    /**
     * import the file format
     *
     * @param xmlPronom as InputStream
     * @return Response jersey response
     */
    @Path("format/import")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public Response importFormat(InputStream xmlPronom) {
        ParametersChecker.checkParameter("xmlPronom is a mandatory parameter", xmlPronom);
        try (ReferentialFormatFileImpl formatManagement = new ReferentialFormatFileImpl(mongoAccess)) {
            formatManagement.importFile(xmlPronom);
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
            final Status status = Status.INTERNAL_SERVER_ERROR;
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
     * @return Response jersey response
     * @throws InvalidParseOperationException when transform result to json exception occurred
     * @throws IOException when error json occurs
     */
    @GET
    @Path("format/{id_format:.+}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response findFileFormatByID(@PathParam("id_format") String formatId, @Context Request request)
        throws InvalidParseOperationException, IOException {
        ParametersChecker.checkParameter("formatId is a mandatory parameter", formatId);
        FileFormat fileFormat = null;
        try (ReferentialFormatFileImpl formatManagement = new ReferentialFormatFileImpl(mongoAccess)) {
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(formatId));
            fileFormat = formatManagement.findDocumentById(formatId);
            if (fileFormat == null) {
                throw new ReferentialException("NO DATA for the specified formatId");
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
                    .addResult(JsonHandler.toJsonNode(fileFormat))).tag(etag).cacheControl(cacheControl).build();
            }

            return builder.cacheControl(cacheControl).tag(etag).build();
        } catch (final ReferentialException e) {
            LOGGER.error(e);
            final Status status = Status.NOT_FOUND;
            return Response.status(status).build();
        } catch (final Exception e) {
            LOGGER.error(e);
            final Status status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status).entity(status).build();
        }
    }

    /**
     * retrieve all the file format inserted in the collection fileFormat
     *
     * @param select as String the query to get format
     * @return Response jersey Response
     * @throws IOException when error json occurs
     * @throws InvalidParseOperationException when error json occurs
     */
    @Path("format/document")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response findFormats(JsonNode select)
        throws InvalidParseOperationException, IOException {
        ParametersChecker.checkParameter(SELECT_IS_A_MANDATORY_PARAMETER, select);
        RequestResponseOK<FileFormat> fileFormatList;
        try (ReferentialFormatFileImpl formatManagement = new ReferentialFormatFileImpl(mongoAccess)) {
            SanityChecker.checkJsonAll(select);
            fileFormatList = formatManagement.findDocuments(select).setQuery(select);
            return Response.status(Status.OK)
                .entity(fileFormatList).build();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (FileFormatNotFoundException e) {
            return Response.status(Status.OK).entity(new RequestResponseOK(select)).build();
        } catch (final ReferentialException e) {
            LOGGER.error(e);
            final Status status = Status.NOT_FOUND;
            return Response.status(status).build();
        } catch (final Exception e) {
            LOGGER.error(e);
            final Status status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status).entity(status).build();
        }
    }

    /**
     * check the rules file
     *
     * @param rulesStream as InputStream
     * @return Response response jersey
     * @throws IOException convert inputstream rule to File exception occurred
     * @throws InvalidCreateOperationException if exception occurred when create query
     * @throws InvalidParseOperationException if parsing json data exception occurred
     * @throws ReferentialException if exception occurred when create rule file manager
     */
    @Path("rules/check")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkRulesFile(InputStream rulesStream)
        throws IOException, ReferentialException, InvalidParseOperationException, InvalidCreateOperationException {
        ParametersChecker.checkParameter("rulesStream is a mandatory parameter", rulesStream);

        try (RulesManagerFileImpl rulesManagerFileImpl = new RulesManagerFileImpl(mongoAccess, vitamCounterService,
            securisator)) {
            rulesManagerFileImpl.checkFile(rulesStream);
            return Response.status(Status.OK).build();
        } catch (final FileRulesException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (final Exception e) {
            LOGGER.error(e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        } finally {
            StreamUtils.closeSilently(rulesStream);
        }


    }


    /**
     * import the rules file
     *
     * @param rulesStream as InputStream
     * @return Response jersey response
     * @throws IOException when error json occurs
     * @throws InvalidParseOperationException when error json occurs
     * @throws ReferentialException when the mongo insert throw error
     */
    @Path("rules/import")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public Response importRulesFile(InputStream rulesStream)
        throws InvalidParseOperationException, ReferentialException, IOException {
        ParametersChecker.checkParameter("rulesStream is a mandatory parameter", rulesStream);
        try (RulesManagerFileImpl rulesFileManagement = new RulesManagerFileImpl(mongoAccess, vitamCounterService,
            securisator)) {

            rulesFileManagement.importFile(rulesStream);
            return Response.status(Status.CREATED).entity(Status.CREATED.getReasonPhrase()).build();
        } catch (final FileRulesImportInProgressException e) {
            LOGGER.warn(e);
            return Response.status(Status.FORBIDDEN).entity(e.getMessage()).build();
        } catch (final FileRulesException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage())
                .build();
        } catch (final ReferentialException e) {
            LOGGER.error(e);
            return Response.status(Status.CONFLICT).entity(e.getMessage())
                .build();
        } catch (final Exception e) {
            LOGGER.error(e);
            final Status status = Status.INTERNAL_SERVER_ERROR;
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
     * @return Response jersey response
     * @throws InvalidParseOperationException if exception occurred when transform json rule id
     * @throws IOException when error json occurs
     * @throws ReferentialException when the mongo search throw error or search result is null
     * @throws InvalidCreateOperationException if exception occurred when create query
     */
    @GET
    @Path("rules/{id_rule}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response findRuleByID(@PathParam("id_rule") String ruleId, @Context Request request)
        throws InvalidParseOperationException, IOException,
        ReferentialException, InvalidCreateOperationException {
        ParametersChecker.checkParameter("ruleId is a mandatory parameter", ruleId);
        FileRules fileRules = null;
        JsonNode result = null;
        try (RulesManagerFileImpl rulesFileManagement = new RulesManagerFileImpl(mongoAccess, vitamCounterService,
            securisator)) {
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(ruleId));
            fileRules = rulesFileManagement.findDocumentById(ruleId);
            if (fileRules == null) {
                throw new FileRulesException("NO DATA for the specified rule Value or More than one records exists");
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
                    .addResult(JsonHandler.toJsonNode(fileRules))).tag(etag).cacheControl(cacheControl).build();
            }

            return builder.cacheControl(cacheControl).tag(etag).build();
        } catch (final FileRulesException e) {
            LOGGER.error(e);
            final Status status = Status.NOT_FOUND;
            return Response.status(status).entity(e.getMessage()).build();
        } catch (final Exception e) {
            LOGGER.error(e);
            final Status status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status).entity(e.getMessage()).build();
        }
    }

    /**
     * show all file rules inserted in the collection fileRules
     *
     * @param select as String
     * @return Response jersey Response
     * @throws IOException when error json occurs
     * @throws InvalidParseOperationException when error json occurs
     */
    @Path("rules/document")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response findDocumentRules(JsonNode select)
        throws InvalidParseOperationException, IOException {
        ParametersChecker.checkParameter(SELECT_IS_A_MANDATORY_PARAMETER, select);
        RequestResponseOK<FileRules> filerulesList;
        try (RulesManagerFileImpl rulesFileManagement = new RulesManagerFileImpl(mongoAccess, vitamCounterService,
            securisator)) {
            SanityChecker.checkJsonAll(select);
            filerulesList = rulesFileManagement.findDocuments(select).setQuery(select);
            return Response.status(Status.OK)
                .entity(filerulesList)
                .build();

        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (final ReferentialException e) {
            LOGGER.error(e);
            final Status status = Status.NOT_FOUND;
            return Response.status(status).build();
        } catch (final Exception e) {
            LOGGER.error(e);
            final Status status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status).entity(status).build();
        }
    }

    /**
     * create or update an accession register
     *
     * @param accessionRegister AccessionRegisterDetail object
     * @return Response jersey response
     */
    @Path("accession-register")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createAccessionRegister(AccessionRegisterDetail accessionRegister) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("register ID / Originating Agency: " + accessionRegister.getId() + " / " +
                accessionRegister.getOriginatingAgency());
        }
        ParametersChecker.checkParameter("Accession Register is a mandatory parameter", accessionRegister);
        try (ReferentialAccessionRegisterImpl accessionRegisterManagement =
            new ReferentialAccessionRegisterImpl(mongoAccess)) {
            accessionRegisterManagement.createOrUpdateAccessionRegister(accessionRegister);
            return Response.status(Status.CREATED).build();
        } catch (final ReferentialException e) {
            LOGGER.error(e);
            return Response.status(Status.PRECONDITION_FAILED).entity(Status.PRECONDITION_FAILED).build();
        } catch (final Exception e) {
            LOGGER.error(e);
            final Status status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status).entity(status).build();
        }
    }

    /**
     * retrieve all accession summary from accession summary collection
     *
     * @param select as String the query to find accession register
     * @return Response jersey Response
     * @throws IOException when error json occurs
     * @throws InvalidParseOperationException when error json occurs
     * @throws ReferentialException when the mongo search throw error or search result is null
     */
    @Path("accession-register/document")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response findDocumentFundsRegister(JsonNode select)
        throws InvalidParseOperationException, IOException, ReferentialException {
        ParametersChecker.checkParameter(SELECT_IS_A_MANDATORY_PARAMETER, select);
        RequestResponseOK<AccessionRegisterSummary> fileFundRegisters;
        try (ReferentialAccessionRegisterImpl accessionRegisterManagement =
            new ReferentialAccessionRegisterImpl(mongoAccess)) {
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
                parser.addCondition(QueryHelper.in("OriginatingAgency",
                    prodServices.toArray(new String[0])));
            }

            fileFundRegisters = accessionRegisterManagement.findDocuments(parser.getRequest().getFinalSelect())
                .setQuery(select);
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (final ReferentialNotFoundException e) {
            LOGGER.error(e);
            final Status status = Status.NOT_FOUND;
            return Response.status(status).entity(status).build();
        } catch (AccessUnauthorizedException e) {
            LOGGER.error("Access contract does not allow ", e);
            final Status status = Status.UNAUTHORIZED;
            return Response.status(status).entity(status).build();
        } catch (final Exception e) {
            LOGGER.error(e);
            final Status status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status).entity(status).build();
        }

        return Response.status(Status.OK)
            .entity(fileFundRegisters)
            .build();
    }

    /**
     * retrieve accession register detail based on a given dsl query
     *
     * @param documentId
     * @param select as String the query to find the accession register
     * @return Response jersey Response
     * @throws IOException when error json occurs
     * @throws InvalidParseOperationException when error json occurs
     * @throws ReferentialException when the mongo search throw error or search result is null
     */
    @Path("accession-register/detail/{id}")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response findDetailAccessionRegister(@PathParam("id") String documentId, JsonNode select)
        throws InvalidParseOperationException, IOException, ReferentialException {
        ParametersChecker.checkParameter(SELECT_IS_A_MANDATORY_PARAMETER, select);
        RequestResponseOK<AccessionRegisterDetail> accessionRegisterDetails;
        try (ReferentialAccessionRegisterImpl accessionRegisterManagement =
            new ReferentialAccessionRegisterImpl(mongoAccess)) {
            SanityChecker.checkJsonAll(select);
            SanityChecker.checkParameter(documentId);

            AccessContractModel contract = getContractDetails(VitamThreadUtils.getVitamSession().getContractId());
            if (contract == null) {
                throw new AccessUnauthorizedException("Contract Not Found");
            }
            boolean isEveryOriginatingAgency = contract.getEveryOriginatingAgency();
            Set<String> prodServices = contract.getOriginatingAgencies();

            SelectParserSingle parser = new SelectParserSingle(DEFAULT_VARNAME_ADAPTER);
            parser.parse(select);

            if (!isEveryOriginatingAgency && !prodServices.contains(documentId)) {
                return Response.status(Status.UNAUTHORIZED).entity(Status.UNAUTHORIZED).build();
            }
            if (!isEveryOriginatingAgency) {
                parser.addCondition(QueryHelper.in("OriginatingAgency",
                    prodServices.stream().toArray(String[]::new)).setDepthLimit(0));
            }
            parser.addCondition(eq("OriginatingAgency", URLDecoder.decode(documentId, CharsetUtils.UTF_8)));

            accessionRegisterDetails =
                accessionRegisterManagement.findDetail(parser.getRequest().getFinalSelect()).setQuery(select);
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (final ReferentialNotFoundException e) {
            LOGGER.error(e);
            final Status status = Status.NOT_FOUND;
            return Response.status(status).entity(status).build();
        } catch (final Exception e) {
            LOGGER.error(e);
            final Status status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status).entity(status).build();
        }

        return Response.status(Status.OK)
            .entity(accessionRegisterDetails)
            .build();
    }

    private AccessContractModel getContractDetails(String contratId) throws InvalidParseOperationException,
        InvalidCreateOperationException, AdminManagementClientServerException {

        try (ContractService<AccessContractModel> accessContract = new AccessContractImpl(mongoAccess,
            vitamCounterService)) {

            final RequestResponseOK<AccessContractModel> accessContractModelList =
                accessContract.findContracts(getQueryDsl(contratId)).setQuery(JsonHandler.createObjectNode());
            return Iterables.getOnlyElement(accessContractModelList.getResults(), null);

        } catch (ReferentialException | InvalidParseOperationException e) {
            LOGGER.error(e);
            return null;
        }
    }

    private static JsonNode getQueryDsl(String headerAccessContratId)
        throws InvalidParseOperationException, InvalidCreateOperationException {

        Select select = new Select();
        Query query = QueryHelper.and().add(QueryHelper.eq(AccessContract.NAME, headerAccessContratId),
            QueryHelper.eq(AccessContract.STATUS, ContractStatus.ACTIVE.name()));
        select.setQuery(query);

        return select.getFinalSelect();
    }

    public void setVitamCounterService(VitamCounterService vitamCounterService) {
        this.vitamCounterService = vitamCounterService;
    }
}
