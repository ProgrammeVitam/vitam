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

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.and;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.builder.query.BooleanQuery;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.server.application.configuration.DbConfigurationImpl;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.common.server.application.resources.BasicVitamStatusServiceImpl;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.functional.administration.accession.register.core.ReferentialAccessionRegisterImpl;
import fr.gouv.vitam.functional.administration.common.AccessionRegisterDetail;
import fr.gouv.vitam.functional.administration.common.AccessionRegisterSummary;
import fr.gouv.vitam.functional.administration.common.FileFormat;
import fr.gouv.vitam.functional.administration.common.FileRules;
import fr.gouv.vitam.functional.administration.common.exception.DatabaseConflictException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialNotFoundException;
import fr.gouv.vitam.functional.administration.common.server.AdminManagementConfiguration;
import fr.gouv.vitam.functional.administration.common.server.ElasticsearchAccessAdminFactory;
import fr.gouv.vitam.functional.administration.common.server.ElasticsearchAccessFunctionalAdmin;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminFactory;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.functional.administration.format.core.ReferentialFormatFileImpl;
import fr.gouv.vitam.functional.administration.ingest.contract.core.IngestContractImpl;
import fr.gouv.vitam.functional.administration.rules.core.RulesManagerFileImpl;

/**
 * FormatManagementResourceImpl implements AccessResource
 */
@Path("/adminmanagement/v1")
@javax.ws.rs.ApplicationPath("webresources")
public class AdminManagementResource extends ApplicationStatusResource {

    static final String CONTRACTS_URI = "contracts";
    private static final String SELECT_IS_A_MANDATORY_PARAMETER = "select is a mandatory parameter";

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AdminManagementResource.class);
    private static final String CONTRACT_JSON_IS_MANDATORY_PATAMETER = "The json input of contracts is mandatory";

    private final MongoDbAccessAdminImpl mongoAccess;
    private final ElasticsearchAccessFunctionalAdmin elasticsearchAccess;

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
        elasticsearchAccess = ElasticsearchAccessAdminFactory.create(configuration);
        mongoAccess = MongoDbAccessAdminFactory.create(adminConfiguration);
        LOGGER.debug("init Admin Management Resource server");
    }

    MongoDbAccess getLogbookDbAccess() {
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
     *
     * @param formatId path param as String
     * @return Response jersey response
     * @throws InvalidParseOperationException
     * @throws IOException when error json occurs
     */
    @POST
    @Path("format/{id_format}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response findFileFormatByID(@PathParam("id_format") String formatId)
        throws InvalidParseOperationException, IOException {
        ParametersChecker.checkParameter("formatId is a mandatory parameter", formatId);
        FileFormat fileFormat = null;
        try (ReferentialFormatFileImpl formatManagement = new ReferentialFormatFileImpl(mongoAccess)) {
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(formatId));
            fileFormat = formatManagement.findDocumentById(formatId);
            if (fileFormat == null) {
                throw new ReferentialException("NO DATA for the specified formatId");
            }

            return Response.status(Status.OK).entity(new RequestResponseOK()
                .setHits(1, 0, 1)
                .addResult(JsonHandler.toJsonNode(fileFormat))).build();
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
     * @param select as String
     * @return Response jersay Response
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
        List<FileFormat> fileFormatList = new ArrayList<>();
        try (ReferentialFormatFileImpl formatManagement = new ReferentialFormatFileImpl(mongoAccess)) {
            SanityChecker.checkJsonAll(select);
            fileFormatList = formatManagement.findDocuments(select);
            final RequestResponseOK responseEntity = new RequestResponseOK()
                .setHits(fileFormatList.size(), 0, fileFormatList.size())
                .setQuery(select);
            for (final FileFormat format : fileFormatList) {
                responseEntity.addResult(JsonHandler.toJsonNode(format));
            }
            return Response.status(Status.OK)
                .entity(responseEntity).build();
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
     * check the rules file
     *
     *
     *
     * @param rulesStream as InputStream
     * @return Response response jersey
     * @throws IOException
     * @throws InvalidCreateOperationException
     * @throws InvalidParseOperationException
     * @throws ReferentialException
     */
    @Path("rules/check")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkRulesFile(InputStream rulesStream)
        throws IOException, ReferentialException, InvalidParseOperationException, InvalidCreateOperationException {
        ParametersChecker.checkParameter("rulesStream is a mandatory parameter", rulesStream);

        try (RulesManagerFileImpl rulesManagerFileImpl = new RulesManagerFileImpl(mongoAccess)) {
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
     * @throws ReferentialException
     */
    @Path("rules/import")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public Response importRulesFile(InputStream rulesStream)
        throws InvalidParseOperationException, ReferentialException, IOException {
        ParametersChecker.checkParameter("rulesStream is a mandatory parameter", rulesStream);
        try (RulesManagerFileImpl rulesFileManagement = new RulesManagerFileImpl(mongoAccess)) {
            rulesFileManagement.importFile(rulesStream);
            return Response.status(Status.CREATED).entity(Status.CREATED.getReasonPhrase()).build();
        } catch (final FileRulesException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage())
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
     * @return Response jersey response
     * @throws InvalidParseOperationException
     * @throws IOException when error json occurs
     * @throws ReferentialException
     * @throws InvalidCreateOperationException
     */
    @POST
    @Path("rules/{id_rule}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response findRuleByID(@PathParam("id_rule") String ruleId)
        throws InvalidParseOperationException, IOException,
        ReferentialException, InvalidCreateOperationException {
        ParametersChecker.checkParameter("ruleId is a mandatory parameter", ruleId);
        List<FileRules> fileRules = null;
        JsonNode result = null;
        try (RulesManagerFileImpl rulesFileManagement = new RulesManagerFileImpl(mongoAccess)) {
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(ruleId));
            result = findRulesByRuleValueQueryBuilder(ruleId);
            fileRules = rulesFileManagement.findDocuments(result);
            if (fileRules == null || fileRules.size() > 1) {
                throw new FileRulesException("NO DATA for the specified rule Value or More than one records exists");
            }
            return Response.status(Status.OK).entity(new RequestResponseOK()
                .setHits(1, 0, 1)
                .addResult(JsonHandler.toJsonNode(fileRules.get(0)))).build();

        } catch (final FileRulesException e) {
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
     * findRulesByRuleValueQueryBuilder: build a dsl query based on a RuleId and order the result
     *
     * @param rulesValue
     * @return
     * @throws InvalidCreateOperationException
     * @throws InvalidParseOperationException
     */
    private JsonNode findRulesByRuleValueQueryBuilder(String rulesId)
        throws InvalidCreateOperationException, InvalidParseOperationException {
        JsonNode result;
        final Select select =
            new Select();
        select.addOrderByDescFilter(rulesId);
        final BooleanQuery query = and();
        query.add(eq("RuleId", rulesId));
        select.setQuery(query);
        result = select.getFinalSelect();
        return result;
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
        List<FileRules> filerulesList = new ArrayList<>();
        try (RulesManagerFileImpl rulesFileManagement = new RulesManagerFileImpl(mongoAccess)) {
            SanityChecker.checkJsonAll(select);
            filerulesList = rulesFileManagement.findDocuments(select);
            final RequestResponseOK responseEntity = new RequestResponseOK()
                .setHits(filerulesList.size(), 0, filerulesList.size())
                .setQuery(select);
            for (final FileRules rule : filerulesList) {
                responseEntity.addResult(JsonHandler.toJsonNode(rule));
            }
            return Response.status(Status.OK)
                .entity(responseEntity)
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
     * @param select as String
     * @return Response jersay Response
     * @throws IOException when error json occurs
     * @throws InvalidParseOperationException when error json occurs
     * @throws ReferentialException
     */
    @Path("accession-register/document")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response findDocumentFundsRegister(JsonNode select)
        throws InvalidParseOperationException, IOException, ReferentialException {
        ParametersChecker.checkParameter(SELECT_IS_A_MANDATORY_PARAMETER, select);
        List<AccessionRegisterSummary> fileFundRegisters = new ArrayList<>();
        try (ReferentialAccessionRegisterImpl accessionRegisterManagement =
            new ReferentialAccessionRegisterImpl(mongoAccess)) {
            SanityChecker.checkJsonAll(select);
            fileFundRegisters = accessionRegisterManagement.findDocuments(select);
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
            .entity(new RequestResponseOK()
                .setHits(fileFundRegisters.size(), 0, fileFundRegisters.size())
                .setQuery(select)
                .addAllResults(fileFundRegisters))
            .build();
    }

    /**
     * retrieve accession register detail based on a given dsl query
     *
     *
     * @param select as String
     * @return Response jersay Response
     * @throws IOException when error json occurs
     * @throws InvalidParseOperationException when error json occurs
     * @throws ReferentialException
     */
    @Path("accession-register/detail")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response findDetailAccessionRegister(JsonNode select)
        throws InvalidParseOperationException, IOException, ReferentialException {
        ParametersChecker.checkParameter(SELECT_IS_A_MANDATORY_PARAMETER, select);
        List<AccessionRegisterDetail> fileAccessionRegistersDetail = new ArrayList<>();
        try (ReferentialAccessionRegisterImpl accessionRegisterManagement =
            new ReferentialAccessionRegisterImpl(mongoAccess)) {
            SanityChecker.checkJsonAll(select);
            fileAccessionRegistersDetail = accessionRegisterManagement.findDetail(select);
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
            .entity(new RequestResponseOK()
                .setHits(fileAccessionRegistersDetail.size(), 0, fileAccessionRegistersDetail.size())
                .setQuery(select)
                .addAllResults(fileAccessionRegistersDetail))
            .build();
    }



    /**
     * Import a set of contracts after passing the validation steps. If all the contracts are valid, they are stored in
     * the collection and indexed. </BR>
     * The input is invalid in the following situations : </BR>
     * <ul>
     * <li>The json is invalid</li>
     * <li>The json contains 2 ore many contracts having the same name</li>
     * <li>One or more mandatory field is missing</li>
     * <li>A field has an invalid format</li>
     * <li>One or many contracts elready exist in the database</li>
     * </ul>
     * 
     * @param xmlPronom as InputStream
     * @param uri
     * @return Response jersey response
     */
    @Path(CONTRACTS_URI)
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response importContracts(ArrayNode contractsToImport, @Context UriInfo uri) {
        ParametersChecker.checkParameter(CONTRACT_JSON_IS_MANDATORY_PATAMETER, contractsToImport);
        List<JsonNode> created = null;
        try (IngestContractImpl ingestContract = new IngestContractImpl(mongoAccess)) {
            SanityChecker.checkJsonAll(contractsToImport);
            created = ingestContract.importContracts(contractsToImport);
        } catch (InvalidParseOperationException e) {
            return Response.status(Status.BAD_REQUEST).entity(getErrorEntity(Status.BAD_REQUEST, e.getMessage(), null))
                .build();
        } catch (ReferentialException refExp) {
            LOGGER.error(refExp);
            return Response.status(Status.BAD_REQUEST)
                .entity(getErrorEntity(Status.BAD_REQUEST, refExp.getMessage(), null)).build();
        } catch (Exception exp) {
            LOGGER.error("Unexpected server error {}", exp);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(getErrorEntity(Status.INTERNAL_SERVER_ERROR, exp.getMessage(), null)).build();
        }
        RequestResponseOK resp = new RequestResponseOK<>();
        if(created != null){
        	resp.addAllResults(created);
        }
        return Response.created(uri.getRequestUri().normalize()).entity(resp).build();
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
        return new VitamError(aCode).setHttpCode(status.getStatusCode()).setContext("ADMIN_MODULE")
            .setState("code_vitam").setMessage(status.getReasonPhrase()).setDescription(aMessage);
    }

}
