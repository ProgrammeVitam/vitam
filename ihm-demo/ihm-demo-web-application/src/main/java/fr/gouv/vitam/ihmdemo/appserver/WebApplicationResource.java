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

import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.DatatypeConverter;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.access.common.exception.AccessClientNotFoundException;
import fr.gouv.vitam.access.common.exception.AccessClientServerException;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.exception.DatabaseConflictException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.ihmdemo.core.DslQueryHelper;
import fr.gouv.vitam.ihmdemo.core.JsonTransformer;
import fr.gouv.vitam.ihmdemo.core.UiConstants;
import fr.gouv.vitam.ihmdemo.core.UserInterfaceTransactionManager;
import fr.gouv.vitam.ihmdemo.core.auth.shiro.AuthenticationService;
import fr.gouv.vitam.ihmdemo.core.auth.shiro.AuthenticationServiceFactory;
import fr.gouv.vitam.ingest.external.api.IngestExternalException;
import fr.gouv.vitam.ingest.external.client.IngestExternalClientFactory;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.operations.client.LogbookClient;
import fr.gouv.vitam.logbook.operations.client.LogbookClientFactory;

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

    @Context
    private HttpServletRequest request;
    private static AuthenticationService authenticationService =
        AuthenticationServiceFactory.getInstance().getAuthenticationService();
    // the maximum age of the cookie in seconds
    private int maxAge = 18000;
    // specifies whether the cookie will only be sent over a secure connection
    private boolean secure = false;


    /**
     * @param criteria criteria search for units
     * @return Reponse
     */
    @POST
    @Path("/archivesearch/units")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getArchiveSearchResult(@CookieParam ("sessionId") String sessionId, String criteria) {
        ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, criteria);
        if(authenticationService.getSession(sessionId)) {
            try {
                SanityChecker.checkJsonAll(JsonHandler.toJsonNode(criteria));
                Map<String, String> criteriaMap = JsonHandler.getMapStringFromString(criteria);
                String preparedQueryDsl = DslQueryHelper.createSelectElasticsearchDSLQuery(criteriaMap);
                JsonNode searchResult = UserInterfaceTransactionManager.searchUnits(preparedQueryDsl);
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
        } else{
            return Response.status(Status.UNAUTHORIZED).build();
        }
    }

    /**
     * @param unitId archive unit id
     * @return archive unit details
     */
    @GET
    @Path("/archivesearch/unit/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getArchiveUnitDetails(@CookieParam ("sessionId") String sessionId, @PathParam("id") String unitId) {
        ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, unitId);
        if(authenticationService.getSession(sessionId)) {
            try {
                SanityChecker.checkJsonAll(JsonHandler.toJsonNode(unitId));
                // Prepare required map
                Map<String, String> selectUnitIdMap = new HashMap<String, String>();
                selectUnitIdMap.put(UiConstants.SELECT_BY_ID.toString(), unitId);
                String preparedQueryDsl = DslQueryHelper.createSelectDSLQuery(selectUnitIdMap);
                JsonNode archiveDetails = UserInterfaceTransactionManager.getArchiveUnitDetails(preparedQueryDsl, unitId);

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
        } else{
            return Response.status(Status.UNAUTHORIZED).build();
        }
    }

    /**
     * @param options the queries for searching
     * @return Response
     */
    @POST
    @Path("/logbook/operations")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLogbookResult(@CookieParam ("sessionId") String sessionId, String options) {
        if(authenticationService.getSession(sessionId)) {
            JsonNode result = null;
            try {
                ParametersChecker.checkParameter("Search criteria payload is mandatory", options);
                result = JsonHandler.getFromString("{}");
                SanityChecker.checkJsonAll(JsonHandler.toJsonNode(options));
                String query = "";
                Map<String, String> optionsMap = JsonHandler.getMapStringFromString(options);
                query = DslQueryHelper.createSingleQueryDSL(optionsMap);
                LogbookClient logbookClient = LogbookClientFactory.getInstance().getLogbookOperationClient();
                result = logbookClient.selectOperation(query);
            } catch (final InvalidCreateOperationException | InvalidParseOperationException e) {
                LOGGER.error("Bad request Exception ", e);
                return Response.status(Status.BAD_REQUEST).build();
            } catch (final LogbookClientException e) {
                LOGGER.error("Logbook Client NOT FOUND Exception ", e);
                return Response.status(Status.NOT_FOUND).build();
            } catch (final Exception e) {
                LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
            return Response.status(Status.OK).entity(result).build();
        } else{
            return Response.status(Status.UNAUTHORIZED).build();
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
    public Response getLogbookResultById(@CookieParam ("sessionId") String sessionId, @PathParam("idOperation") String operationId, String options) {

        if(authenticationService.getSession(sessionId)) {
            JsonNode result = null;

            try {
                ParametersChecker.checkParameter("Search criteria payload is mandatory", options);
                SanityChecker.checkJsonAll(JsonHandler.toJsonNode(options));
                result = JsonHandler.getFromString("{}");
                LogbookClient logbookClient = LogbookClientFactory.getInstance().getLogbookOperationClient();
                result = logbookClient.selectOperationbyId(operationId);
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
        } else{
            return Response.status(Status.UNAUTHORIZED).build();
        }
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
     * upload the file TODO : add file name
     *
     * @param stream, data input stream
     * @return Response
     */
    @Path("ingest/upload")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public Response upload(@CookieParam ("sessionId") String sessionId, InputStream stream) {
        ParametersChecker.checkParameter("SIP is a mandatory parameter", stream);
        if(authenticationService.getSession(sessionId)) {
            try {
                IngestExternalClientFactory.getInstance().getIngestExternalClient().upload(stream);
            } catch (final IngestExternalException e) {
                LOGGER.error("IngestExternalException in Upload sip", e);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .build();
            }
            return Response.status(Status.OK).build();
        } else{
            return Response.status(Status.UNAUTHORIZED).build();
        }
    }

    /**
     * Update Archive Units
     *
     * @param updateSet conains updated field
     * @param unitId archive unit id
     * @return archive unit details
     */
    @PUT
    @Path("/archiveupdate/units/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateArchiveUnitDetails(@CookieParam ("sessionId") String sessionId, @PathParam("id") String unitId, String updateSet) {

        if(authenticationService.getSession(sessionId)) {
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
                Map<String, String> updateUnitIdMap = new HashMap<String, String>();
                JsonNode modifiedFields = JsonHandler.getFromString(updateSet);
                if (modifiedFields != null && modifiedFields.isArray()) {
                    for (final JsonNode modifiedField : modifiedFields) {
                        updateUnitIdMap.put(modifiedField.get(FIELD_ID_KEY).textValue(),
                            modifiedField.get(NEW_FIELD_VALUE_KEY).textValue());
                    }
                }

                // Add ID to set root part
                updateUnitIdMap.put(UiConstants.SELECT_BY_ID.toString(), unitId);

                String preparedQueryDsl = DslQueryHelper.createUpdateDSLQuery(updateUnitIdMap);
                JsonNode archiveDetails = UserInterfaceTransactionManager.updateUnits(preparedQueryDsl, unitId);
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
        } else{
            return Response.status(Status.UNAUTHORIZED).build();
        }
    }

    /**
     * @param options the queries for searching
     * @return Response
     */
    @POST
    @Path("/admin/formats")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFileFormats(@CookieParam ("sessionId") String sessionId, String options) {
        ParametersChecker.checkParameter("Search criteria payload is mandatory", options);
        if(authenticationService.getSession(sessionId)) {
            String query = "";
            JsonNode result = null;
            try {
                SanityChecker.checkJsonAll(JsonHandler.toJsonNode(options));
                result = JsonHandler.getFromString("{}");
                Map<String, String> optionsMap = JsonHandler.getMapStringFromString(options);
                query = DslQueryHelper.createSingleQueryDSL(optionsMap);
                AdminManagementClient adminClient = AdminManagementClientFactory.getInstance().getAdminManagementClient();
                result = adminClient.getFormats(JsonHandler.getFromString(query));
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
            return Response.status(Status.OK).entity(result).build();
        } else{
            return Response.status(Status.UNAUTHORIZED).build();
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
    public Response getFormatById(@CookieParam ("sessionId") String sessionId, @PathParam("idFormat") String formatId, String options) {

        if(authenticationService.getSession(sessionId)) {
            JsonNode result = null;

            try {
                ParametersChecker.checkParameter("Search criteria payload is mandatory", options);
                SanityChecker.checkJsonAll(JsonHandler.toJsonNode(options));
                ParametersChecker.checkParameter("Format Id is mandatory", formatId);
                SanityChecker.checkJsonAll(JsonHandler.toJsonNode(formatId));
                result = JsonHandler.getFromString("{}");
                AdminManagementClient adminClient = AdminManagementClientFactory.getInstance().getAdminManagementClient();
                result = adminClient.getFormatByID(formatId);
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
            return Response.status(Status.OK).entity(result).build();
        } else{
            return Response.status(Status.UNAUTHORIZED).build();
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
    public Response checkRefFormat(@CookieParam ("sessionId") String sessionId, InputStream input) {
        if(authenticationService.getSession(sessionId)) {
            AdminManagementClient client = AdminManagementClientFactory.getInstance().getAdminManagementClient();
            try {
                client.checkFormat(input);
            } catch (final ReferentialException e) {
                return Response.status(Status.FORBIDDEN).build();
            }
            return Response.status(Status.OK).build();
        } else{
            return Response.status(Status.UNAUTHORIZED).build();
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
    public Response uploadRefFormat(@CookieParam ("sessionId") String sessionId, InputStream input) {

        if(authenticationService.getSession(sessionId)) {
            AdminManagementClient client = AdminManagementClientFactory.getInstance().getAdminManagementClient();
            try {
                client.importFormat(input);
            } catch (final ReferentialException e) {
                return Response.status(Status.FORBIDDEN).build();
            } catch (final DatabaseConflictException e) {
                return Response.status(Status.FORBIDDEN).build();
            }
            return Response.status(Status.OK).build();
        } else{
            return Response.status(Status.UNAUTHORIZED).build();
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
    public Response deleteFormat(@CookieParam ("sessionId") String sessionId) {
        if(authenticationService.getSession(sessionId)) {
            AdminManagementClient client = AdminManagementClientFactory.getInstance().getAdminManagementClient();
            try {
                client.deleteFormat();
            } catch (final ReferentialException e) {
                return Response.status(Status.FORBIDDEN).build();
            }
            return Response.status(Status.OK).build();
        } else{
            return Response.status(Status.UNAUTHORIZED).build();
        }
    }



    /**
     * Decode the authorization header and use authentication service to login
     * 
     * @return a response containing the session Id in header
     */
    @POST
    @Path("/login")
    @Produces(MediaType.APPLICATION_JSON)
    public Response login() {

        String decodedAuthorization = null;
        String username = null;
        try {
            String header = request.getHeader("authorization");

            if (header == null) {
                LOGGER.error("invalid authorization header");
                return Response.status(Status.UNAUTHORIZED).build();
            }

            String codedAuthorization = header.substring(header.indexOf(" ") + 1);

            byte[] bytes = DatatypeConverter.parseBase64Binary(codedAuthorization);
            decodedAuthorization = new String(bytes);

            String[] parts = decodedAuthorization.split(":");
            username = parts[0];
            String password = parts[1];


            Serializable sessionId = authenticationService.login(username, password, true);
            LOGGER.info("User login : " + username + " logged in successfully using PASSWORD " + "sessionId : " +
                sessionId.toString());

            // using cookie
            NewCookie newCookie = new NewCookie("sessionId", sessionId.toString(),
                "/", "", 0, "", maxAge, secure);

            ResponseBuilder response = Response.ok().cookie(newCookie);

            return response.build();

        } catch (VitamException e) {
            LOGGER.info("User login failure : " + username + " using PASSWORD");
            LOGGER.debug(e.getMessage(), e);
            return Response.status(Status.UNAUTHORIZED).build();
        } catch (Exception e) {
            LOGGER.debug(e.getMessage(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Use the session id to log out 
     * 
     * @param sessionId of loggedIn user
     * @return a response with status OK or UNAUTHORIZED
     */
    @POST
    @Path("/logout")
    @Produces(MediaType.APPLICATION_JSON)
    public Response logout(@CookieParam("sessionId") String sessionId) {

        try {
            authenticationService.logout(sessionId);

        } catch (VitamException e) {
            LOGGER.info("User logout failure : " + sessionId);
            LOGGER.debug(e.getMessage(), e);
            return Response.status(Status.UNAUTHORIZED).entity(e.getMessage()).build();
        }


        LOGGER.info("User logout : " + sessionId + " logged out successfully");
        NewCookie newCookie = new NewCookie("sessionId", "",
            "/", "", 0, "", 0, secure);

        ResponseBuilder response = Response.ok().cookie(newCookie);
        return response.build();
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
    public Response getArchiveObjectGroup(@CookieParam ("sessionId") String sessionId, @PathParam("idOG") String objectGroupId) {
        ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, objectGroupId);
        if(authenticationService.getSession(sessionId)) {
            try {
                SanityChecker.checkJsonAll(JsonHandler.toJsonNode(objectGroupId));

                HashMap<String, String> qualifierProjection = new HashMap<>();
                qualifierProjection.put("projection_qualifiers", "#qualifiers");
                String preparedQueryDsl = DslQueryHelper.createSelectDSLQuery(qualifierProjection);
                JsonNode searchResult = UserInterfaceTransactionManager.selectObjectbyId(preparedQueryDsl, objectGroupId);

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
        } else {
            return Response.status(Status.UNAUTHORIZED).build();
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
    public Response getObjectAsInputStream(@CookieParam ("sessionId") String sessionId, @PathParam("idOG") String objectGroupId, String options) {
        ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, objectGroupId);
        if(authenticationService.getSession(sessionId)) {
            try {
                SanityChecker.checkJsonAll(JsonHandler.toJsonNode(objectGroupId));
                SanityChecker.checkJsonAll(JsonHandler.toJsonNode(options));
                Map<String, String> optionsMap = JsonHandler.getMapStringFromString(options);
                String usage = optionsMap.get("usage");
                String version = optionsMap.get("version");
                ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, usage);
                ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, version);
                HashMap<String, String> emptyMap = new HashMap<>();
                String preparedQueryDsl = DslQueryHelper.createSelectDSLQuery(emptyMap);
                InputStream stream =
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
        } else {
            return Response.status(Status.UNAUTHORIZED).build();
        }
    }


}
