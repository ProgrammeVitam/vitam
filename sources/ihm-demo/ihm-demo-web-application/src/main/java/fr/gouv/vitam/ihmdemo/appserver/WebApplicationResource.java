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
import java.net.URLDecoder;
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
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
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

import fr.gouv.vitam.access.external.api.AdminCollections;
import fr.gouv.vitam.access.external.client.AdminExternalClient;
import fr.gouv.vitam.access.external.client.AdminExternalClientFactory;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientException;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientNotFoundException;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientServerException;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.server2.application.HttpHeaderHelper;
import fr.gouv.vitam.common.server2.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.ihmdemo.common.api.IhmDataRest;
import fr.gouv.vitam.ihmdemo.common.api.IhmWebAppHeader;
import fr.gouv.vitam.ihmdemo.common.pagination.OffsetBasedPagination;
import fr.gouv.vitam.ihmdemo.common.pagination.PaginationHelper;
import fr.gouv.vitam.ihmdemo.core.DslQueryHelper;
import fr.gouv.vitam.ihmdemo.core.JsonTransformer;
import fr.gouv.vitam.ihmdemo.core.UiConstants;
import fr.gouv.vitam.ihmdemo.core.UserInterfaceTransactionManager;
import fr.gouv.vitam.ingest.external.client.IngestExternalClient;
import fr.gouv.vitam.ingest.external.client.IngestExternalClientFactory;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;

/**
 * Web Application Resource class
 */
@Path("/v1/api")
public class WebApplicationResource extends ApplicationStatusResource {

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
    private final WebApplicationConfig webApplicationConfig;
    private static final String FILE_NAME_KEY = "fileName";
    private static final String FILE_SIZE_KEY = "fileSize";
    private static final String ZIP_EXTENSION = ".ZIP";
    private static final String TAR_GZ_EXTENSION = ".TAR.GZ";
    private static final int TENANT_ID = 0;

    // FIXME P0 utile ??? utilisé nulle part
    @Context
    private HttpServletRequest request;

    /**
     * Constructor
     * 
     * @param webApplicationConfig
     */
    public WebApplicationResource(WebApplicationConfig webApplicationConfig) {
        this.webApplicationConfig = webApplicationConfig;
    }

    /**
     * Retrieve all the messages for logbook
     * 
     * @return Response
     */
    @GET
    @Path("/messages/logbook")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLogbookMessages() {
        // TODO P0 : If translation key could be the same in different .properties file, MUST add an unique prefix per
        // file
        return Response.status(Status.OK).entity(VitamLogbookMessages.getAllMessages()).build();
    }

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
        } catch (final AccessExternalClientServerException e) {
            LOGGER.error(ACCESS_SERVER_EXCEPTION_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (final AccessExternalClientNotFoundException e) {
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
        } catch (final AccessExternalClientServerException e) {
            LOGGER.error(ACCESS_SERVER_EXCEPTION_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (final AccessExternalClientNotFoundException e) {
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

            try {
                ParametersChecker.checkParameter("Search criteria payload is mandatory", options);
                SanityChecker.checkJsonAll(JsonHandler.toJsonNode(options));
                String query = "";
                final Map<String, String> optionsMap = JsonHandler.getMapStringFromString(options);
                query = DslQueryHelper.createSingleQueryDSL(optionsMap);

                result = UserInterfaceTransactionManager.selectOperation(query);

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
        try {
            ParametersChecker.checkParameter("Search criteria payload is mandatory", options);
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(options));
            result = UserInterfaceTransactionManager.selectOperationbyId(operationId);
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
    // TODO P1 : add file name
    @Path("ingest/upload")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response upload(InputStream stream) throws XMLStreamException, IOException {
        Response response = null;
        String responseXml = "";
        String guid = "guid";
        ParametersChecker.checkParameter("SIP is a mandatory parameter", stream);
        try (IngestExternalClient client = IngestExternalClientFactory.getInstance().getClient()) {
            response = client.upload(stream);

            // FIXME P0 utiliser InputStream avec AsyncResponse pour ne pas charger en mémoire l'XML
            responseXml = response.readEntity(String.class);
            guid = response.getHeaderString(GlobalDataRest.X_REQUEST_ID);

        } catch (final VitamException e) {
            LOGGER.error("IngestExternalException in Upload sip", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .build();
        }
        return Response.status(Status.fromStatusCode(response.getStatus())).entity(responseXml)
            .header("Content-Disposition", "attachment; filename=" + guid + ".xml")
            .header(GlobalDataRest.X_REQUEST_ID, guid)
            .build();
    }

    // FIXME P0 - This endpoint is front test only and should not be used. A chunk compatible endpoint MUST be made
    /**
     * upload 2 : API Endpoint that can Handle chunk mode. Chunks information are given in header (Fast catch of these
     * header are present in the code) <br />
     * The front should give some information
     * <ul>
     * <li>Flow-Chunk-Number => The index of the current chunk</li>
     * <li>Flow-Chunk-Size => The configured maximal size of a chunk</li>
     * <li>Flow-Current-Chunk-Size => The size of the current chunk</li>
     * <li>Flow-Total-Size => The total size of the file (All chunks)</li>
     * <li>Flow-Identifier => The identifier of the flow</li>
     * <li>Flow-Filename => The file name</li>
     * <li>Flow-Relative-Path => (?)The relative path (or the file name only)</li>
     * <li>Flow-Total-Chunks => The number of chunks</li>
     * </ul>
     * 
     * @param stream data input stream for the current chunk
     * @param headers HTTP Headers containing chunk information
     * @return Response
     */
    @Path("ingest/upload2")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response upload2(@Context HttpHeaders headers, InputStream stream) {
        LOGGER.error("This method should not be used !!");
        LOGGER.error("Headers number: " + headers.getLength());
        MultivaluedMap<String, String> headersMap = headers.getRequestHeaders();
        for (Map.Entry<String, List<String>> entry : headersMap.entrySet()) {
            if (entry.getKey().startsWith("Flow-")) {
                LOGGER.error("Header: " + entry.getKey() + ", value: " + entry.getValue());
            }
        }

        return Response.status(Status.OK).build();
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
        } catch (final AccessExternalClientServerException e) {
            LOGGER.error(ACCESS_SERVER_EXCEPTION_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (final AccessExternalClientNotFoundException e) {
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
        try (final AdminExternalClient adminClient =
            AdminExternalClientFactory.getInstance().getClient()) {
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(options));
            result = JsonHandler.createObjectNode();
            final Map<String, String> optionsMap = JsonHandler.getMapStringFromString(options);
            query = DslQueryHelper.createSingleQueryDSL(optionsMap);
            result = adminClient.findDocuments(AdminCollections.FORMATS, JsonHandler.getFromString(query));
            return Response.status(Status.OK).entity(result).build();
        } catch (final InvalidCreateOperationException | InvalidParseOperationException e) {
            LOGGER.error("Bad request Exception ", e);
            return Response.status(Status.BAD_REQUEST).build();
        } catch (final AccessExternalClientNotFoundException e) {
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

        try (final AdminExternalClient adminClient =
            AdminExternalClientFactory.getInstance().getClient()) {
            ParametersChecker.checkParameter("Search criteria payload is mandatory", options);
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(options));
            ParametersChecker.checkParameter("Format Id is mandatory", formatId);
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(formatId));
            result = adminClient.findDocumentById(AdminCollections.FORMATS, formatId);
            return Response.status(Status.OK).entity(result).build();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION_MSG, e);
            return Response.status(Status.BAD_REQUEST).build();
        } catch (final AccessExternalClientNotFoundException e) {
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
        try (final AdminExternalClient adminClient =
            AdminExternalClientFactory.getInstance().getClient()) {
            adminClient.checkDocuments(AdminCollections.FORMATS, input);
            return Response.status(Status.OK).build();
        } catch (final AccessExternalClientNotFoundException e) {
            LOGGER.error("AdminManagementClient NOT FOUND Exception ", e);
            return Response.status(Status.NOT_FOUND).build();
        } catch (Exception e) {
            LOGGER.error(e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            StreamUtils.closeSilently(input);
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
        try (final AdminExternalClient adminClient =
            AdminExternalClientFactory.getInstance().getClient()) {
            adminClient.createDocuments(AdminCollections.FORMATS, input);
            return Response.status(Status.OK).build();
        } catch (final AccessExternalClientException e) {
            LOGGER.error("AdminManagementClient NOT FOUND Exception ", e);
            return Response.status(Status.FORBIDDEN).build();
        } catch (Exception e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            StreamUtils.closeSilently(input);
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
        try (final AdminExternalClient adminClient =
            AdminExternalClientFactory.getInstance().getClient()) {
            adminClient.deleteDocuments(AdminCollections.FORMATS);
            return Response.status(Status.OK).build();
        } catch (final AccessExternalClientException e) {
            LOGGER.error("AdminManagementClient NOT FOUND Exception ", e);
            return Response.status(Status.FORBIDDEN).build();
        } catch (Exception e) {
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
        } catch (final AccessExternalClientServerException e) {
            LOGGER.error(ACCESS_SERVER_EXCEPTION_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (final AccessExternalClientNotFoundException e) {
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
     * @param usage additional mandatory parameters usage
     * @param version additional mandatory parameters version
     * @param filename additional mandatory parameters filename
     * @return a response containing the input stream
     */
    @GET
    @Path("/archiveunit/objects/download/{idOG}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getObjectAsInputStream(@PathParam("idOG") String objectGroupId, @QueryParam("usage") String usage,
        @QueryParam("version") String version, @QueryParam("filename") String filename) {
        ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, objectGroupId);
        try {
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(objectGroupId));
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(version));
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(usage));
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(filename));
            ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, usage);
            ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, version);
            ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, filename);
            final HashMap<String, String> emptyMap = new HashMap<>();
            final String preparedQueryDsl = DslQueryHelper.createSelectDSLQuery(emptyMap);
            final InputStream stream =
                UserInterfaceTransactionManager.getObjectAsInputStream(preparedQueryDsl, objectGroupId, usage,
                    Integer.parseInt(version));
            return Response.status(Status.OK)
                .header("Content-Disposition", "filename=\"" + URLDecoder.decode(filename, "UTF-8") + "\"")
                .entity(stream).build();

        } catch (final InvalidCreateOperationException | InvalidParseOperationException | NumberFormatException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION_MSG, e);
            return Response.status(Status.BAD_REQUEST).build();
        } catch (final AccessExternalClientServerException e) {
            LOGGER.error(ACCESS_SERVER_EXCEPTION_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (final AccessExternalClientNotFoundException e) {
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
        try (final AdminExternalClient adminClient =
            AdminExternalClientFactory.getInstance().getClient()) {
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(options));
            result = JsonHandler.createObjectNode();
            final Map<String, String> optionsMap = JsonHandler.getMapStringFromString(options);
            query = DslQueryHelper.createSingleQueryDSL(optionsMap);
            result = adminClient.findDocuments(AdminCollections.RULES, JsonHandler.getFromString(query));
            return Response.status(Status.OK).entity(result).build();
        } catch (final InvalidCreateOperationException | InvalidParseOperationException e) {
            LOGGER.error("Bad request Exception ", e);
            return Response.status(Status.BAD_REQUEST).build();
        } catch (final AccessExternalClientNotFoundException e) {
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

        try (final AdminExternalClient adminClient =
            AdminExternalClientFactory.getInstance().getClient()) {
            ParametersChecker.checkParameter("Search criteria payload is mandatory", options);
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(options));
            ParametersChecker.checkParameter("rule Id is mandatory", ruleId);
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(ruleId));
            result = JsonHandler.createObjectNode();
            result = adminClient.findDocumentById(AdminCollections.RULES, ruleId);
            return Response.status(Status.OK).entity(result).build();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION_MSG, e);
            return Response.status(Status.BAD_REQUEST).build();
        } catch (final AccessExternalClientNotFoundException e) {
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
        try (final AdminExternalClient adminClient =
            AdminExternalClientFactory.getInstance().getClient()) {
            adminClient.checkDocuments(AdminCollections.RULES, input);
            return Response.status(Status.OK).build();
        } catch (final AccessExternalClientException e) {
            return Response.status(Status.FORBIDDEN).build();
        } catch (Exception e) {
            LOGGER.error(e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            StreamUtils.closeSilently(input);
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

        try (final AdminExternalClient adminClient =
            AdminExternalClientFactory.getInstance().getClient()) {
            adminClient.createDocuments(AdminCollections.RULES, input);
            return Response.status(Status.OK).build();
        } catch (final AccessExternalClientException e) {
            return Response.status(Status.FORBIDDEN).build();
        } catch (Exception e) {
            LOGGER.error("INTERNAL SERVER ERROR", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            StreamUtils.closeSilently(input);
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
        try (final AdminExternalClient adminClient =
            AdminExternalClientFactory.getInstance().getClient()) {
            adminClient.deleteDocuments(AdminCollections.RULES);
            return Response.status(Status.OK).build();
        } catch (final AccessExternalClientException e) {
            return Response.status(Status.FORBIDDEN).build();
        } catch (Exception e) {
            LOGGER.error(e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get the action registers filtered with option query
     * 
     * @param options the queries for searching
     * @return Response
     */
    @POST
    @Path("/admin/accession-register")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAccessionRegister(String options) {
        ParametersChecker.checkParameter("Search criteria payload is mandatory", options);
        JsonNode result = JsonHandler.createObjectNode();
        try {
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(options));
            result = UserInterfaceTransactionManager.findAccessionRegisterSummary(options);
        } catch (final InvalidCreateOperationException | InvalidParseOperationException e) {
            LOGGER.error("Bad request Exception ", e);
            return Response.status(Status.BAD_REQUEST).build();
        } catch (final AccessExternalClientNotFoundException e) {
            LOGGER.error("AdminManagementClient NOT FOUND Exception ", e);
            return Response.status(Status.NOT_FOUND).build();
        } catch (final Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
        return Response.status(Status.OK).entity(result).build();
    }

    /**
     * Get the detail of an accessionregister matching options query
     * 
     * @param id
     * @param options query criteria
     * @return accession register details
     */
    @POST
    @Path("/admin/accession-register/{id}/accession-register-detail")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAccessionRegisterDetail(@PathParam("id") String id, String options) {
        ParametersChecker.checkParameter("Search criteria payload is mandatory", options);
        JsonNode result = JsonHandler.createObjectNode();
        try {
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(options));
            result = UserInterfaceTransactionManager.findAccessionRegisterDetail(id, options);
        } catch (final InvalidCreateOperationException | InvalidParseOperationException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION_MSG, e);
            return Response.status(Status.BAD_REQUEST).build();
        } catch (final AccessExternalClientNotFoundException e) {
            LOGGER.error("AdminManagementClient NOT FOUND Exception ", e);
            return Response.status(Status.NOT_FOUND).build();
        } catch (final Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }

        return Response.status(Status.OK).entity(result).build();
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
        } catch (final AccessExternalClientServerException e) {
            LOGGER.error(ACCESS_SERVER_EXCEPTION_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (final AccessExternalClientNotFoundException e) {
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
            // TODO P1 add access log
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
            result = UserInterfaceTransactionManager.selectUnitLifeCycleById(unitLifeCycleId);
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
            result = UserInterfaceTransactionManager.selectObjectGroupLifeCycleById(objectGroupLifeCycleId);
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
        try {
            final JsonNode logbookOperationResult = UserInterfaceTransactionManager.selectOperationbyId(operationId);
            if (logbookOperationResult != null && logbookOperationResult.has("$results")) {
                final JsonNode logbookOperation = logbookOperationResult.get("$results");
                // Create csv file
                final ByteArrayOutputStream csvOutputStream =
                    JsonTransformer.buildLogbookStatCsvFile(logbookOperation);
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
            InputStream sipInputStream = new FileInputStream(webApplicationConfig.getSipDirectory() + "/" + fileName);
            IngestExternalClient client = IngestExternalClientFactory.getInstance().getClient()) {
            final Response response = client.upload(sipInputStream);
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
