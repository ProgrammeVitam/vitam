/**
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
package fr.gouv.vitam.ihmdemo.appserver;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.xml.stream.XMLStreamException;

import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.gouv.vitam.access.common.exception.AccessClientNotFoundException;
import fr.gouv.vitam.access.common.exception.AccessClientServerException;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.server.application.HttpHeaderHelper;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.exception.DatabaseConflictException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.ihmdemo.common.api.IhmDataRest;
import fr.gouv.vitam.ihmdemo.common.api.IhmWebAppHeader;
import fr.gouv.vitam.ihmdemo.common.pagination.OffsetBasedPagination;
import fr.gouv.vitam.ihmdemo.common.pagination.PaginationHelper;
import fr.gouv.vitam.ihmdemo.core.DslQueryHelper;
import fr.gouv.vitam.ihmdemo.core.JsonTransformer;
import fr.gouv.vitam.ihmdemo.core.UiConstants;
import fr.gouv.vitam.ihmdemo.core.UserInterfaceTransactionManager;
import fr.gouv.vitam.ingest.external.client.IngestExternalClientFactory;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;

/**
 * Web Application Resource class
 */
@Path("/v1/api")
public class WebApplicationResource {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(WebApplicationResource.class);
    private static final String BAD_REQUEST_EXCEPTION_MSG = "Bad request Exception";
    private static final String ACCESS_CLIENT_NOT_FOUND_EXCEPTION_MSG = "Access client unavailable";
    private static final String ACCESS_SERVER_EXCEPTION_MSG = "Access Server exception";
    private static final String INTERNAL_SERVER_ERROR_MSG = "INTERNAL SERVER ERROR";
    private static final String SEARCH_CRITERIA_MANDATORY_MSG = "Search criteria payload is mandatory";
    private static final String FIELD_ID_KEY = "fieldId";
    private static final String NEW_FIELD_VALUE_KEY = "newFieldValue";
    private static final String INVALID_ALL_PARENTS_TYPE_ERROR_MSG = "The parameter \"allParents\" is not an array";

    private static final String LOGBOOK_CLIENT_NOT_FOUND_EXCEPTION_MSG = "Logbook Client NOT FOUND Exception";
    private static final WebApplicationConfig webApplicationConfig = ServerApplication.getWebApplicationConfig();
    private static final String FILE_NAME_KEY = "fileName";
    private static final String FILE_SIZE_KEY = "fileSize";
    private static final String ZIP_EXTENSION = ".ZIP";
    private static final String TAR_GZ_EXTENSION = ".TAR.GZ";
    private static final int TENANT_ID = 0;

    @Context
    private HttpServletRequest request;

    /**
     * @param criteria criteria search for units
     * @return Reponse
     */
    @POST
    @Path("/archivesearch/units")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getArchiveSearchResult(String criteria) {
        ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, criteria);
        try {
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(criteria));
            final Map<String, String> criteriaMap = JsonHandler.getMapStringFromString(criteria);
            final String preparedQueryDsl = DslQueryHelper.createSelectElasticsearchDSLQuery(criteriaMap);
            final JsonNode searchResult = UserInterfaceTransactionManager.searchUnits(preparedQueryDsl);
            return Response.status(Status.OK).entity(searchResult).build();

        } catch (final InvalidCreateOperationException | InvalidParseOperationException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION_MSG, e);
            return Response.status(Status.BAD_REQUEST).build();
        } catch (final AccessClientServerException e) {
            LOGGER.error(ACCESS_SERVER_EXCEPTION_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (final AccessClientNotFoundException e) {
            LOGGER.error(ACCESS_CLIENT_NOT_FOUND_EXCEPTION_MSG, e);
            return Response.status(Status.NOT_FOUND).build();
        } catch (final Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * @param unitId archive unit id
     * @return archive unit details
     */
    @GET
    @Path("/archivesearch/unit/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getArchiveUnitDetails(@PathParam("id") String unitId) {
        ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, unitId);
        try {
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(unitId));
            // Prepare required map
            final Map<String, String> selectUnitIdMap = new HashMap<String, String>();
            selectUnitIdMap.put(UiConstants.SELECT_BY_ID.toString(), unitId);
            final String preparedQueryDsl = DslQueryHelper.createSelectDSLQuery(selectUnitIdMap);
            final JsonNode archiveDetails =
                UserInterfaceTransactionManager.getArchiveUnitDetails(preparedQueryDsl, unitId);

            return Response.status(Status.OK).entity(archiveDetails).build();
        } catch (final InvalidCreateOperationException | InvalidParseOperationException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION_MSG, e);
            return Response.status(Status.BAD_REQUEST).build();
        } catch (final AccessClientServerException e) {
            LOGGER.error(ACCESS_SERVER_EXCEPTION_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (final AccessClientNotFoundException e) {
            LOGGER.error(ACCESS_CLIENT_NOT_FOUND_EXCEPTION_MSG, e);
            return Response.status(Status.NOT_FOUND).build();
        } catch (final Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * @param headers 
     * @param sessionId 
     * @param options the queries for searching
     * @return Response
     */
    @POST
    @Path("/logbook/operations")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLogbookResult(@Context HttpHeaders headers, @CookieParam("JSESSIONID") String sessionId,
        String options) {

        ParametersChecker.checkParameter("cookie is mandatory", sessionId);
        String requestId = null;
        JsonNode result = null;
        OffsetBasedPagination pagination = null;

        try {
            pagination = new OffsetBasedPagination(headers);
        } catch (final VitamException e) {
            LOGGER.error("Bad request Exception ", e);
            return Response.status(Status.BAD_REQUEST).build();
        }
        final List<String> requestIds = HttpHeaderHelper.getHeaderValues(headers, IhmWebAppHeader.REQUEST_ID.name());
        if (requestIds != null) {
            requestId = requestIds.get(0);
            // get result from shiro session
            try {
                result = PaginationHelper.getResult(sessionId, pagination);

                return Response.status(Status.OK).entity(result)
                    .header(GlobalDataRest.X_REQUEST_ID, requestId)
                    .header(IhmDataRest.X_OFFSET, pagination.getOffset())
                    .header(IhmDataRest.X_LIMIT, pagination.getLimit())
                    .build();
            } catch (final VitamException e) {
                LOGGER.error("Bad request Exception ", e);
                return Response.status(Status.BAD_REQUEST).header(GlobalDataRest.X_REQUEST_ID, requestId)
                    .build();
            }
        } else {
            requestId = GUIDFactory.newRequestIdGUID(TENANT_ID).toString();

            try (final LogbookOperationsClient logbookOperationsClient =
                LogbookOperationsClientFactory.getInstance().getClient()) {
                ParametersChecker.checkParameter("Search criteria payload is mandatory", options);
                SanityChecker.checkJsonAll(JsonHandler.toJsonNode(options));
                String query = "";
                final Map<String, String> optionsMap = JsonHandler.getMapStringFromString(options);
                query = DslQueryHelper.createSingleQueryDSL(optionsMap);

                result = logbookOperationsClient.selectOperation(query);

                // save result
                PaginationHelper.setResult(sessionId, result);
                // pagination
                result = PaginationHelper.getResult(result, pagination);

            } catch (final InvalidCreateOperationException | InvalidParseOperationException e) {
                LOGGER.error("Bad request Exception ", e);
                return Response.status(Status.BAD_REQUEST).header(GlobalDataRest.X_REQUEST_ID, requestId)
                    .build();
            } catch (final LogbookClientException e) {
                LOGGER.error("Logbook Client NOT FOUND Exception ", e);
                return Response.status(Status.NOT_FOUND).header(GlobalDataRest.X_REQUEST_ID, requestId)
                    .build();
            } catch (final Exception e) {
                LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
                return Response.status(Status.INTERNAL_SERVER_ERROR).header(GlobalDataRest.X_REQUEST_ID, requestId)
                    .build();
            }
            return Response.status(Status.OK).entity(result)
                .header(GlobalDataRest.X_REQUEST_ID, requestId)
                .header(IhmDataRest.X_OFFSET, pagination.getOffset())
                .header(IhmDataRest.X_LIMIT, pagination.getLimit())
                .build();
        }
    }

    /**
     * @param operationId id of operation
     * @param options the queries for searching
     * @return Response
     */
    @POST
    @Path("/logbook/operations/{idOperation}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLogbookResultById(@PathParam("idOperation") String operationId, String options) {

        JsonNode result = null;
        try (final LogbookOperationsClient logbookOperationsClient =
            LogbookOperationsClientFactory.getInstance().getClient()) {
            ParametersChecker.checkParameter("Search criteria payload is mandatory", options);
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(options));
            result = logbookOperationsClient.selectOperationbyId(operationId);
        } catch (final IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (final LogbookClientException e) {
            LOGGER.error("Logbook Client NOT FOUND Exception ", e);
            return Response.status(Status.NOT_FOUND).build();
        } catch (final Exception e) {
            LOGGER.error("INTERNAL SERVER ERROR", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
        return Response.status(Status.OK).entity(result).build();
    }

    /**
     * Return a response status
     *
     * @return Response
     */
    @Path("status")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response status() {
        return Response.status(Status.OK).build();
    }

    /**
     * upload the file
     *
     * @param stream data input stream
     * @return Response
     * @throws XMLStreamException
     * @throws IOException
     */
    //TODO : add file name
    @Path("ingest/upload")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response upload(InputStream stream) throws XMLStreamException, IOException {
        Response response = null;
        String responseXml = "";
        String guid = "guid";
        ParametersChecker.checkParameter("SIP is a mandatory parameter", stream);
        try {
            response = IngestExternalClientFactory.getInstance().getIngestExternalClient().upload(stream);
            // TODO: utiliser InputStream avec AsyncResponse pour ne pas charger en mémoire l'XML
            responseXml = response.readEntity(String.class);
            guid = response.getHeaderString(GlobalDataRest.X_REQUEST_ID);

        } catch (final VitamException e) {
            LOGGER.error("IngestExternalException in Upload sip", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .build();
        }

        return Response.status(Status.OK).entity(responseXml)
            .header("Content-Disposition", "attachment; filename=" + guid + ".xml")
            .header(GlobalDataRest.X_REQUEST_ID, guid)
            .build();
    }

    /**
     * Update Archive Units
     *
     * @param updateSet contains updated field
     * @param unitId archive unit id
     * @return archive unit details
     */
    @PUT
    @Path("/archiveupdate/units/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateArchiveUnitDetails(@PathParam("id") String unitId,
        String updateSet) {

        try {
            ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, unitId);
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(unitId));
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(updateSet));

        } catch (final IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        }

        try {
            // Parse updateSet
            final Map<String, String> updateUnitIdMap = new HashMap<String, String>();
            final JsonNode modifiedFields = JsonHandler.getFromString(updateSet);
            if (modifiedFields != null && modifiedFields.isArray()) {
                for (final JsonNode modifiedField : modifiedFields) {
                    updateUnitIdMap.put(modifiedField.get(FIELD_ID_KEY).textValue(),
                        modifiedField.get(NEW_FIELD_VALUE_KEY).textValue());
                }
            }

            // Add ID to set root part
            updateUnitIdMap.put(UiConstants.SELECT_BY_ID.toString(), unitId);
            final String preparedQueryDsl = DslQueryHelper.createUpdateDSLQuery(updateUnitIdMap);
            final JsonNode archiveDetails = UserInterfaceTransactionManager.updateUnits(preparedQueryDsl, unitId);
            return Response.status(Status.OK).entity(archiveDetails).build();
        } catch (final InvalidCreateOperationException | InvalidParseOperationException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION_MSG, e);
            return Response.status(Status.BAD_REQUEST).build();
        } catch (final AccessClientServerException e) {
            LOGGER.error(ACCESS_SERVER_EXCEPTION_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (final AccessClientNotFoundException e) {
            LOGGER.error(ACCESS_CLIENT_NOT_FOUND_EXCEPTION_MSG, e);
            return Response.status(Status.NOT_FOUND).build();
        } catch (final Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * @param options the queries for searching
     * @return Response
     */
    @POST
    @Path("/admin/formats")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFileFormats(String options) {
        ParametersChecker.checkParameter("Search criteria payload is mandatory", options);
        String query = "";
        JsonNode result = null;
        try (final AdminManagementClient adminClient =
            AdminManagementClientFactory.getInstance().getClient()) {
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(options));
            result = JsonHandler.createObjectNode();
            final Map<String, String> optionsMap = JsonHandler.getMapStringFromString(options);
            query = DslQueryHelper.createSingleQueryDSL(optionsMap);
            result = adminClient.getFormats(JsonHandler.getFromString(query));
            return Response.status(Status.OK).entity(result).build();            
        } catch (final InvalidCreateOperationException | InvalidParseOperationException e) {
            LOGGER.error("Bad request Exception ", e);
            return Response.status(Status.BAD_REQUEST).build();
        } catch (final ReferentialException e) {
            LOGGER.error("AdminManagementClient NOT FOUND Exception ", e);
            return Response.status(Status.NOT_FOUND).build();
        } catch (final Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * @param formatId id of format
     * @param options the queries for searching
     * @return Response
     */
    @POST
    @Path("/admin/formats/{idFormat}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFormatById(@PathParam("idFormat") String formatId,
        String options) {
        JsonNode result = null;

        try (final AdminManagementClient adminClient =
            AdminManagementClientFactory.getInstance().getClient()) {
            ParametersChecker.checkParameter("Search criteria payload is mandatory", options);
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(options));
            ParametersChecker.checkParameter("Format Id is mandatory", formatId);
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(formatId));
            result = adminClient.getFormatByID(formatId);
            return Response.status(Status.OK).entity(result).build();            
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION_MSG, e);
            return Response.status(Status.BAD_REQUEST).build();
        } catch (final ReferentialException e) {
            LOGGER.error("AdminManagementClient NOT FOUND Exception ", e);
            return Response.status(Status.NOT_FOUND).build();
        } catch (final Exception e) {
            LOGGER.error("INTERNAL SERVER ERROR", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }


    /***
     * check the referential format
     *
     * @param input the format file xml
     * @return If the formet is valid, return ok. If not, return the list of errors
     */
    @POST
    @Path("/format/check")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkRefFormat(InputStream input) {
       try (final AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
               client.checkFormat(input);
               return Response.status(Status.OK).build();
        } catch (final ReferentialException e) {
            return Response.status(Status.FORBIDDEN).build();
        } catch (Exception e) {
        LOGGER.error(e);
        return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Upload the referential format in the base
     *
     * @param input the format file xml
     * @return Response
     */
    @POST
    @Path("/format/upload")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadRefFormat(InputStream input) {
        try (final AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
                client.importFormat(input);
                return Response.status(Status.OK).build();
        } catch (final ReferentialException e) {
            return Response.status(Status.FORBIDDEN).build();
        } catch (final DatabaseConflictException e) {
            return Response.status(Status.FORBIDDEN).build();
        } catch (Exception e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete the referential format in the base
     *
     * @return Response
     */
    @Path("/format/delete")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteFormat() {
        try (final AdminManagementClient client =
            AdminManagementClientFactory.getInstance().getClient()) {
            client.deleteFormat();
            return Response.status(Status.OK).build();
        } catch (final ReferentialException e) {
            return Response.status(Status.FORBIDDEN).build();
        }catch(Exception e){
            LOGGER.error(e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }

    }

    /**
     * Retrieve an ObjectGroup as Json data based on the provided ObjectGroup id
     *
     * @param objectGroupId the object group Id
     * @return a response containing a json with informations about usages and versions for an object group
     */
    @GET
    @Path("/archiveunit/objects/{idOG}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getArchiveObjectGroup(@PathParam("idOG") String objectGroupId) {
        ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, objectGroupId);
        try {
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(objectGroupId));

            final HashMap<String, String> qualifierProjection = new HashMap<>();
            qualifierProjection.put("projection_qualifiers", "#qualifiers");
            final String preparedQueryDsl = DslQueryHelper.createSelectDSLQuery(qualifierProjection);
            final JsonNode searchResult =
                UserInterfaceTransactionManager.selectObjectbyId(preparedQueryDsl, objectGroupId);

            return Response.status(Status.OK).entity(JsonTransformer.transformResultObjects(searchResult)).build();

        } catch (final InvalidCreateOperationException | InvalidParseOperationException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION_MSG, e);
            return Response.status(Status.BAD_REQUEST).build();
        } catch (final AccessClientServerException e) {
            LOGGER.error(ACCESS_SERVER_EXCEPTION_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (final AccessClientNotFoundException e) {
            LOGGER.error(ACCESS_CLIENT_NOT_FOUND_EXCEPTION_MSG, e);
            return Response.status(Status.NOT_FOUND).build();
        } catch (final Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }

    }

    /**
     * Retrieve an Object data as an input stream
     *
     * @param objectGroupId the object group Id
     * @param options additional parameters like usage and version
     * @return a response containing the input stream
     */
    @POST
    @Path("/archiveunit/objects/download/{idOG}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getObjectAsInputStream(@PathParam("idOG") String objectGroupId, String options) {
        ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, objectGroupId);
        try {
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(objectGroupId));
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(options));
            final Map<String, String> optionsMap = JsonHandler.getMapStringFromString(options);
            final String usage = optionsMap.get("usage");
            final String version = optionsMap.get("version");
            ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, usage);
            ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, version);
            final HashMap<String, String> emptyMap = new HashMap<>();
            final String preparedQueryDsl = DslQueryHelper.createSelectDSLQuery(emptyMap);
            final InputStream stream =
                UserInterfaceTransactionManager.getObjectAsInputStream(preparedQueryDsl, objectGroupId, usage,
                    Integer.parseInt(version));
            return Response.status(Status.OK).entity(stream).build();

        } catch (final InvalidCreateOperationException | InvalidParseOperationException | NumberFormatException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION_MSG, e);
            return Response.status(Status.BAD_REQUEST).build();
        } catch (final AccessClientServerException e) {
            LOGGER.error(ACCESS_SERVER_EXCEPTION_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (final AccessClientNotFoundException e) {
            LOGGER.error(ACCESS_CLIENT_NOT_FOUND_EXCEPTION_MSG, e);
            return Response.status(Status.NOT_FOUND).build();
        } catch (final Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /***** rules Management ************/

    /**
     * @param options the queries for searching
     * @return Response
     */
    @POST
    @Path("/admin/rules")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFileRules(String options) {
        ParametersChecker.checkParameter("Search criteria payload is mandatory", options);
        String query = "";
        JsonNode result = null;
        try (final AdminManagementClient adminClient =
            AdminManagementClientFactory.getInstance().getClient()) {
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(options));
            result = JsonHandler.createObjectNode();
            final Map<String, String> optionsMap = JsonHandler.getMapStringFromString(options);
            query = DslQueryHelper.createSingleQueryDSL(optionsMap);
            result = adminClient.getRule(JsonHandler.getFromString(query));
            return Response.status(Status.OK).entity(result).build();
        } catch (final InvalidCreateOperationException | InvalidParseOperationException e) {
            LOGGER.error("Bad request Exception ", e);
            return Response.status(Status.BAD_REQUEST).build();
        } catch (final ReferentialException e) {
            LOGGER.error("AdminManagementClient NOT FOUND Exception ", e);
            return Response.status(Status.NOT_FOUND).build();
        } catch (Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * @param ruleId id of rule
     * @param options the queries for searching
     * @return Response
     */
    @POST
    @Path("/admin/rules/{id_rule}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRuleById(@PathParam("id_rule") String ruleId,
        String options) {

        JsonNode result = null;

        try (final AdminManagementClient adminClient =
            AdminManagementClientFactory.getInstance().getClient()) {
            ParametersChecker.checkParameter("Search criteria payload is mandatory", options);
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(options));
            ParametersChecker.checkParameter("rule Id is mandatory", ruleId);
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(ruleId));
            result = JsonHandler.createObjectNode();
               result = adminClient.getRuleByID(ruleId);
            return Response.status(Status.OK).entity(result).build();               
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION_MSG, e);
            return Response.status(Status.BAD_REQUEST).build();
        } catch (final ReferentialException e) {
            LOGGER.error("AdminManagementClient NOT FOUND Exception ", e);
            return Response.status(Status.NOT_FOUND).build();
        } catch (final Exception e) {
            LOGGER.error("INTERNAL SERVER ERROR", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

 
    /***
     * check the referential rules
     *
     * @param input the rules file csv
     * @return If the rules file is valid, return ok. If not, return the list of errors
     */
    @POST
    @Path("/rules/check")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkRefRule(InputStream input) {
        try (final AdminManagementClient client =
            AdminManagementClientFactory.getInstance().getClient()) {
            client.checkRulesFile(input);
            return Response.status(Status.OK).build();
        } catch (final ReferentialException e) {
            return Response.status(Status.FORBIDDEN).build();            
        } catch (Exception e) {
            LOGGER.error(e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();            
        } 
    }

    /**
     * Upload the referential rules in the base
     *
     * @param input the format file CSV
     * @return Response
     */
    @POST
    @Path("/rules/upload")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadRefRule(InputStream input) {

        try (final AdminManagementClient client =
            AdminManagementClientFactory.getInstance().getClient()) {
            client.importRulesFile(input);
            return Response.status(Status.OK).build();
        } catch (final ReferentialException e) {
            return Response.status(Status.FORBIDDEN).build();
        } catch (final DatabaseConflictException e) {
            return Response.status(Status.FORBIDDEN).build();
        } catch (Exception e) {
            LOGGER.error("INTERNAL SERVER ERROR", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }

    }

    /**
     * Delete the referential rules in the base
     *
     * @return Response
     */
    @Path("/rules/delete")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteRulesFile() {
        try (final AdminManagementClient client =
            AdminManagementClientFactory.getInstance().getClient()) {
            client.deleteRulesFile();            
            return Response.status(Status.OK).build();            
        } catch (final ReferentialException e) {
            return Response.status(Status.FORBIDDEN).build();
        } catch (Exception e) {
            LOGGER.error(e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();            
        }
    }



    /**
     * This resource returns all paths relative to a unit
     *
     * @param sessionId current session
     * @param unitId the unit id
     * @param allParents all parents unit
     * @return all paths relative to a unit
     */
    @POST
    @Path("/archiveunit/tree/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUnitTree(@PathParam("id") String unitId,
        String allParents) {

        ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, unitId);
        ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, allParents);

        try {
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(unitId));
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(allParents));

            if (!JsonHandler.getFromString(allParents).isArray()) {
                throw new VitamException(INVALID_ALL_PARENTS_TYPE_ERROR_MSG);
            }

            // 1- Build DSL Query
            final ArrayNode allParentsArray = (ArrayNode) JsonHandler.getFromString(allParents);
            final List<String> allParentsList =
                StreamSupport.stream(allParentsArray.spliterator(), false).map(p -> new String(p.asText()))
                    .collect(Collectors.toList());
            final String preparedDslQuery = DslQueryHelper.createSelectUnitTreeDSLQuery(unitId, allParentsList);

            // 2- Execute Select Query
            final JsonNode parentsDetails = UserInterfaceTransactionManager.searchUnits(preparedDslQuery);

            // 3- Build Unit tree (all paths)
            final JsonNode unitTree = UserInterfaceTransactionManager.buildUnitTree(unitId,
                parentsDetails.get(UiConstants.RESULT.getConstantValue()));

            return Response.status(Status.OK).entity(unitTree).build();
        } catch (InvalidParseOperationException | InvalidCreateOperationException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION_MSG, e);
            return Response.status(Status.BAD_REQUEST).build();
        } catch (final AccessClientServerException e) {
            LOGGER.error(ACCESS_SERVER_EXCEPTION_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (final AccessClientNotFoundException e) {
            LOGGER.error(ACCESS_CLIENT_NOT_FOUND_EXCEPTION_MSG, e);
            return Response.status(Status.NOT_FOUND).build();
        } catch (final Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * @param object user credentials
     * @return Response OK if login success
     */
    @POST
    @Path("login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(JsonNode object) {
        final Subject subject = ThreadContext.getSubject();
        final String username = object.get("token").get("principal").textValue();
        final String password = object.get("token").get("credentials").textValue();

        if (username == null || password == null) {
            return Response.status(Status.UNAUTHORIZED).build();
        }

        final UsernamePasswordToken token = new UsernamePasswordToken(username, password);

        try {
            subject.login(token);
            // TODO add access log
            LOGGER.info("Login success: " + username);
        } catch (final Exception uae) {
            LOGGER.debug("Login fail: " + username);
            return Response.status(Status.UNAUTHORIZED).build();
        }

        return Response.status(Status.OK).build();
    }

    /**
     * returns the unit life cycle based on its id
     *
     * @param unitLifeCycleId the unit id (== unit life cycle id)
     * @return the unit life cycle
     */
    @GET
    @Path("/unitlifecycles/{id_lc}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUnitLifeCycleById(@PathParam("id_lc") String unitLifeCycleId) {
        ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, unitLifeCycleId);
        JsonNode result = null;
        try {
            final LogbookLifeCyclesClient logbookLifeCycleClient =
                LogbookLifeCyclesClientFactory.getInstance().getClient();
            result = logbookLifeCycleClient.selectUnitLifeCycleById(unitLifeCycleId);
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (final LogbookClientException e) {
            LOGGER.error(LOGBOOK_CLIENT_NOT_FOUND_EXCEPTION_MSG, e);
            return Response.status(Status.NOT_FOUND).build();
        } catch (final Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
        return Response.status(Status.OK).entity(result).build();
    }

    /**
     * returns the object group life cycle based on its id
     *
     * @param objectGroupLifeCycleId the object group id (== object group life cycle id)
     * @return the object group life cycle
     */
    @GET
    @Path("/objectgrouplifecycles/{id_lc}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getObjectGroupLifeCycleById(@PathParam("id_lc") String objectGroupLifeCycleId) {

        ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, objectGroupLifeCycleId);
        JsonNode result = null;

        try {
            final LogbookLifeCyclesClient logbookLifeCycleClient =
                LogbookLifeCyclesClientFactory.getInstance().getClient();
            result = logbookLifeCycleClient.selectObjectGroupLifeCycleById(objectGroupLifeCycleId);
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (final LogbookClientException e) {
            LOGGER.error(LOGBOOK_CLIENT_NOT_FOUND_EXCEPTION_MSG, e);
            return Response.status(Status.NOT_FOUND).build();
        } catch (final Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
        return Response.status(Status.OK).entity(result).build();
    }


    /**
     * Generates the logbook operation statistics file (cvs format) relative to the operation parameter
     *
     * @param operationId logbook oeration id
     * @return the statistics file (csv format)
     */
    @GET
    @Path("/stat/{id_op}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getLogbookStatistics(@PathParam("id_op") String operationId) {
        try (final LogbookOperationsClient logbookOperationsClient =
            LogbookOperationsClientFactory.getInstance().getClient()) {
            final JsonNode logbookOperationResult = logbookOperationsClient.selectOperationbyId(operationId);
            if (logbookOperationResult != null && logbookOperationResult.has("result")) {
                final JsonNode logbookOperation = logbookOperationResult.get("result");
                // Create csv file
                final ByteArrayOutputStream csvOutputStream =
                    JsonTransformer.buildLogbookStatCsvFile(logbookOperation, operationId);
                final byte[] csvOutArray = csvOutputStream.toByteArray();
                final ResponseBuilder response = Response.ok(csvOutArray);
                response.header("Content-Disposition", "attachment;filename=rapport.csv");
                response.header("Content-Length", csvOutArray.length);

                return response.build();
            }

            return Response.status(Status.NOT_FOUND).build();
        } catch (final LogbookClientException e) {
            LOGGER.error("Logbook Client NOT FOUND Exception ", e);
            return Response.status(Status.NOT_FOUND).build();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error("INTERNAL SERVER ERROR", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (final Exception e) {
            LOGGER.error("INTERNAL SERVER ERROR", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Returns the list of available files
     *
     * @return the list of available files
     */
    @GET
    @Path("/upload/fileslist")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAvailableFilesList() {

        if (webApplicationConfig == null || webApplicationConfig.getSipDirectory() == null) {
            LOGGER.error("SIP directory not configured");
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("SIP directory not configured")
                .build();
        }

        final File fileDirectory = new File(webApplicationConfig.getSipDirectory());

        if (!fileDirectory.isDirectory()) {
            LOGGER.error("SIP directory <{}> is not a directory.",
                webApplicationConfig.getSipDirectory());
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(
                "SIP directory [" + webApplicationConfig.getSipDirectory() + "] is not a directory")
                .build();
        }
        final File[] sipFiles = fileDirectory.listFiles(new SipFilenameFilterImpl());
        final ArrayNode filesListDetails = JsonHandler.createArrayNode();

        if (sipFiles != null) {
            for (final File currentFile : sipFiles) {
                final ObjectNode fileDetails = JsonHandler.createObjectNode();
                fileDetails.put(FILE_NAME_KEY, currentFile.getName());
                fileDetails.put(FILE_SIZE_KEY, currentFile.length());
                filesListDetails.add(fileDetails);
            }
        }

        return Response.status(Status.OK).entity(filesListDetails).build();
    }

    private class SipFilenameFilterImpl implements FilenameFilter {
        @Override
        public boolean accept(File dir, String fileName) {
            return fileName.toUpperCase().endsWith(ZIP_EXTENSION) || fileName.toUpperCase().endsWith(TAR_GZ_EXTENSION);
        }
    }

    /**
     * Uploads the given file and returns the logbook operation id
     *
     * @param fileName the file name
     * @return the logbook operation id
     */
    @GET
    @Path("/upload/{file_name}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response uploadFileFromServer(@PathParam("file_name") String fileName) {
        ParametersChecker.checkParameter("SIP path is a mandatory parameter", fileName);
        if (webApplicationConfig == null || webApplicationConfig.getSipDirectory() == null) {
            LOGGER.error("SIP directory not configured");
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("SIP directory not configured")
                .build();
        }

        // Read the selected file into an InputStream
        try (
            InputStream sipInputStream = new FileInputStream(webApplicationConfig.getSipDirectory() + "/" + fileName)) {
            final Response response =
                IngestExternalClientFactory.getInstance().getIngestExternalClient().upload(sipInputStream);
            final String ingestOperationId = response.getHeaderString(GlobalDataRest.X_REQUEST_ID);

            return Response.status(response.getStatus()).entity(ingestOperationId).build();
        } catch (final VitamException e) {
            LOGGER.error("IngestExternalException in Upload sip", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e)
                .build();
        } catch (FileNotFoundException | XMLStreamException e) {
            LOGGER.error("The selected file is not found", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .build();
        } catch (IOException e) {
            LOGGER.error("Error occured when trying to close the stream", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .build();
        }
    }
}
