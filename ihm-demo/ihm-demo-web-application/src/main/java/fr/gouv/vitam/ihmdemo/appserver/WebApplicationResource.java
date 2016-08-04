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
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.access.common.exception.AccessClientNotFoundException;
import fr.gouv.vitam.access.common.exception.AccessClientServerException;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.exception.DatabaseConflictException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.ihmdemo.core.DslQueryHelper;
import fr.gouv.vitam.ihmdemo.core.UiConstants;
import fr.gouv.vitam.ihmdemo.core.UserInterfaceTransactionManager;
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
    private static final String UPDATE_CRITERIA_MANDATORY_MSG = "Update criteria payload is mandatory";
    private static final String FIELD_ID_KEY = "fieldId";
    private static final String NEW_FIELD_VALUE_KEY = "newFieldValue";

    /**
     * @param criteria
     *            criteria search for units
     * @return Reponse
     */
    @POST
    @Path("/archivesearch/units")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getArchiveSearchResult(String criteria) {
        ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, criteria);
        try {
            Map<String, String> criteriaMap = JsonHandler.getMapStringFromString(criteria);

            String preparedQueryDsl = DslQueryHelper.createSelectDSLQuery(criteriaMap);
            JsonNode searchResult = UserInterfaceTransactionManager.searchUnits(preparedQueryDsl);
            return Response.status(Status.OK).entity(searchResult).build();

        } catch (InvalidCreateOperationException | InvalidParseOperationException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION_MSG, e);
            return Response.status(Status.BAD_REQUEST).build();
        } catch (AccessClientServerException e) {
            LOGGER.error(ACCESS_SERVER_EXCEPTION_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (AccessClientNotFoundException e) {
            LOGGER.error(ACCESS_CLIENT_NOT_FOUND_EXCEPTION_MSG, e);
            return Response.status(Status.NOT_FOUND).build();
        } catch (Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * @param unitId
     *            archive unit id
     * @return archive unit details
     */
    @GET
    @Path("/archivesearch/unit/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getArchiveUnitDetails(@PathParam("id") String unitId) {
        ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, unitId);

        // Prepare required map
        Map<String, String> selectUnitIdMap = new HashMap<String, String>();
        selectUnitIdMap.put(UiConstants.SELECT_BY_ID.toString(), unitId);

        try {
            String preparedQueryDsl = DslQueryHelper.createSelectDSLQuery(selectUnitIdMap);
            JsonNode archiveDetails = UserInterfaceTransactionManager.getArchiveUnitDetails(preparedQueryDsl, unitId);

            return Response.status(Status.OK).entity(archiveDetails).build();
        } catch (InvalidCreateOperationException | InvalidParseOperationException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION_MSG, e);
            return Response.status(Status.BAD_REQUEST).build();
        } catch (AccessClientServerException e) {
            LOGGER.error(ACCESS_SERVER_EXCEPTION_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (AccessClientNotFoundException e) {
            LOGGER.error(ACCESS_CLIENT_NOT_FOUND_EXCEPTION_MSG, e);
            return Response.status(Status.NOT_FOUND).build();
        } catch (Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * @param options
     *          the queries for searching
     * @return Response
     * @throws InvalidParseOperationException
     *              could not be transfered to Json
     */
    @POST
    @Path("/logbook/operations")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLogbookResult(String options) throws InvalidParseOperationException {
        ParametersChecker.checkParameter("Search criteria payload is mandatory", options);
        JsonNode result = JsonHandler.getFromString("{}");
        String query = "";
        try {
            Map<String, String> optionsMap = JsonHandler.getMapStringFromString(options);
            query = DslQueryHelper.createSingleQueryDSL(optionsMap);
            LogbookClient logbookClient = LogbookClientFactory.getInstance().getLogbookOperationClient();
            result = logbookClient.selectOperation(query);
        } catch (InvalidCreateOperationException | InvalidParseOperationException e) {
            LOGGER.error("Bad request Exception ", e);
            return Response.status(Status.BAD_REQUEST).build();
        } catch (LogbookClientException e) {
            LOGGER.error("Logbook Client NOT FOUND Exception ", e);
            return Response.status(Status.NOT_FOUND).build();
        } catch (Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
        return Response.status(Status.OK).entity(result).build();
    }

    /**
     * @param operationId
     *            id of operation
     * @param options
     *          the queries for searching
     * @return Response
     * @throws InvalidParseOperationException
     *              could not be transfered to Json
     */
    @POST
    @Path("/logbook/operations/{idOperation}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLogbookResultById(@PathParam("idOperation") String operationId, String options)
        throws InvalidParseOperationException {
        ParametersChecker.checkParameter("Search criteria payload is mandatory", options);
        JsonNode result = JsonHandler.getFromString("{}");
        try {
            LogbookClient logbookClient = LogbookClientFactory.getInstance().getLogbookOperationClient();
            result = logbookClient.selectOperationbyId(operationId);
        } catch (LogbookClientException e) {
            LOGGER.error("Logbook Client NOT FOUND Exception ", e);
            return Response.status(Status.NOT_FOUND).build();
        } catch (Exception e) {
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
     * @param stream, data input stream
     * @return Response
     */
    @Path("ingest/upload")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public Response upload(InputStream stream){
        ParametersChecker.checkParameter("SIP is a mandatory parameter", stream);
        try {
            IngestExternalClientFactory.getInstance().getIngestExternalClient().upload(stream);
        } catch (IngestExternalException e) {
            LOGGER.error("IngestExternalException in Upload sip", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(e.getMessage())
                .build();
        }
        return Response.status(Status.OK).build();
    }  

    /**
     * Update Archive Units
     * 
     * @param updateSet conains updated field
     * @param unitId archive unit id
     * @return archive unit details
     * @throws InvalidParseOperationException
     *              could not be transfered to Json
     */
    @PUT
    @Path("/archiveupdate/units/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateArchiveUnitDetails(@PathParam("id") String unitId, String updateSet)
        throws InvalidParseOperationException {
        ParametersChecker.checkParameter(SEARCH_CRITERIA_MANDATORY_MSG, unitId);
        ParametersChecker.checkParameter(UPDATE_CRITERIA_MANDATORY_MSG, updateSet);
        
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

        try {
            String preparedQueryDsl = DslQueryHelper.createUpdateDSLQuery(updateUnitIdMap);
            JsonNode archiveDetails = UserInterfaceTransactionManager.updateUnits(preparedQueryDsl, unitId);
            return Response.status(Status.OK).entity(archiveDetails).build();
        } catch (InvalidCreateOperationException | InvalidParseOperationException e) {
            LOGGER.error(BAD_REQUEST_EXCEPTION_MSG, e);
            return Response.status(Status.BAD_REQUEST).build();
        } catch (AccessClientServerException e) {
            LOGGER.error(ACCESS_SERVER_EXCEPTION_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } catch (AccessClientNotFoundException e) {
            LOGGER.error(ACCESS_CLIENT_NOT_FOUND_EXCEPTION_MSG, e);
            return Response.status(Status.NOT_FOUND).build();
        } catch (Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * @param options
     *          the queries for searching
     * @return Response
     * @throws InvalidParseOperationException
     *              could not be transfered to Json
     */
    @POST
    @Path("/admin/formats")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFileFormats(String options) throws InvalidParseOperationException {
        ParametersChecker.checkParameter("Search criteria payload is mandatory", options);
        JsonNode result = JsonHandler.getFromString("{}");
        String query = "";
        try {
            Map<String, String> optionsMap = JsonHandler.getMapStringFromString(options);
            query = DslQueryHelper.createSingleQueryDSL(optionsMap);
            AdminManagementClient adminClient = AdminManagementClientFactory.getInstance().getAdminManagementClient();
            result = adminClient.getFormats(JsonHandler.getFromString(query));
        } catch (InvalidCreateOperationException | InvalidParseOperationException e) {
            LOGGER.error("Bad request Exception ", e);
            return Response.status(Status.BAD_REQUEST).build();
        } catch (ReferentialException e) {
            LOGGER.error("AdminManagementClient NOT FOUND Exception ", e);
            return Response.status(Status.NOT_FOUND).build();
        } catch (Exception e) {
            LOGGER.error(INTERNAL_SERVER_ERROR_MSG, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
        return Response.status(Status.OK).entity(result).build();
    }
    
    /**
     * @param formatId
     *            id of format
     * @param options
     *          the queries for searching
     * @return Response
     * @throws InvalidParseOperationException
     *              could not be transfered to Json
     */
    @POST
    @Path("/admin/formats/{idFormat}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFormatById(@PathParam("idFormat") String formatId, String options) throws InvalidParseOperationException {
        ParametersChecker.checkParameter("Search criteria payload is mandatory", options);
        ParametersChecker.checkParameter("Format Id is mandatory", formatId);
        JsonNode result = JsonHandler.getFromString("{}");
        try {
            AdminManagementClient adminClient = AdminManagementClientFactory.getInstance().getAdminManagementClient();
            result = adminClient.getFormatByID(formatId);
        } catch (ReferentialException e) {
            LOGGER.error("AdminManagementClient NOT FOUND Exception ", e);
            return Response.status(Status.NOT_FOUND).build();
        } catch (Exception e) {
            LOGGER.error("INTERNAL SERVER ERROR", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
        return Response.status(Status.OK).entity(result).build();
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
	    AdminManagementClient client = AdminManagementClientFactory.getInstance().getAdminManagementClient();
	    try {
            client.checkFormat(input);
        } catch (ReferentialException e) {
            return Response.status(Status.FORBIDDEN).build();
        }
	    return Response.status(Status.OK).build();    
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
    public Response uploadRefFormat(InputStream input){
        AdminManagementClient client = AdminManagementClientFactory.getInstance().getAdminManagementClient();
        try {
            client.importFormat(input);
        } catch (ReferentialException e) {
            return Response.status(Status.FORBIDDEN).build();
        } catch (DatabaseConflictException e) {
            return Response.status(Status.FORBIDDEN).build();
        }
        return Response.status(Status.OK).build(); 
    }
    
    /**
     * Delete the referential format in the base
     * 
     * @return Response
     */
    @Path("/format/delete")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteFormat(){
        AdminManagementClient client = AdminManagementClientFactory.getInstance().getAdminManagementClient();
        try {
            client.deleteFormat();
        } catch (ReferentialException e) {
            return Response.status(Status.FORBIDDEN).build();
        }         
        return Response.status(Status.OK).build();
    }
}
