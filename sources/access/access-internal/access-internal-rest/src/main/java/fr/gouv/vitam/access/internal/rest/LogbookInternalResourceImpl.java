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
package fr.gouv.vitam.access.internal.rest;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.parser.request.single.SelectParserSingle;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;

/**
 * AccessResourceImpl implements AccessResource
 */
@Path("/access-internal/v1")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@javax.ws.rs.ApplicationPath("webresources")
public class LogbookInternalResourceImpl {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogbookInternalResourceImpl.class);

    /**
     * Constructor
     *
     * @param configuration
     */
    public LogbookInternalResourceImpl() {
        LOGGER.debug("LogbookExternalResource initialized");
    }

    /***** LOGBOOK OPERATION - START *****/
    /**
     * @param operationId the operation id
     * @return the response with a specific HTTP status
     * @throws InvalidParseOperationException
     */
    @GET
    @Path("/operations/{id_op}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOperationById(@PathParam("id_op") String operationId) throws InvalidParseOperationException {
        Status status;
        try (LogbookOperationsClient client = LogbookOperationsClientFactory.getInstance().getClient()) {
            JsonNode result = client.selectOperationbyId(operationId);
            return Response.status(Status.OK)
                .entity(result)
                .build();
        } catch (LogbookClientException e) {
            LOGGER.error(e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(
                    new VitamError(status.name()).setHttpCode(status.getStatusCode())
                        .setContext("logbook")
                        .setState("code_vitam")
                        .setMessage(status.getReasonPhrase())
                        .setDescription(status.getReasonPhrase()))
                .build();
        }
    }

    /**
     *
     * @param operationId path param, the operation id
     * @param operation the json serialized as a LogbookOperationParameters.
     * @param xhttpOverride header param as String indicate the use of POST method as GET
     * @return the response with a specific HTTP status
     * @throws InvalidParseOperationException
     */
    @POST
    @Path("/operations/{id_op}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectOperationByPost(@PathParam("id_op") String operationId,
        @HeaderParam("X-HTTP-Method-Override") String xhttpOverride)
        throws InvalidParseOperationException {
        Status status;
        if (xhttpOverride != null && "GET".equals(xhttpOverride)) {
            ParametersChecker.checkParameter("Operation id is required", operationId);
            return getOperationById(operationId);
        } else {
            status = Status.PRECONDITION_FAILED;
            return Response.status(status)
                .entity(
                    new VitamError(status.name()).setHttpCode(status.getStatusCode())
                        .setContext("logbook")
                        .setState("code_vitam")
                        .setMessage(status.getReasonPhrase())
                        .setDescription(status.getReasonPhrase()))
                .build();
        }
    }

    /**
     * GET with request in body
     *
     * @param query DSL as String
     * @return Response containt the list of loglook operation
     * @throws InvalidParseOperationException
     */
    @GET
    @Path("/operations")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    // FIXME P0 JsonNode en argument pour toutes les "query"
    public Response selectOperation(String query)
        throws InvalidParseOperationException {
        Status status;
        try (LogbookOperationsClient client = LogbookOperationsClientFactory.getInstance().getClient()) {
            // Check correctness of request
            SelectParserSingle parser = new SelectParserSingle();
            parser.parse(JsonHandler.getFromString(query));
            parser.getRequest().reset();
            if (! (parser instanceof SelectParserSingle)) {
                throw new InvalidParseOperationException("Not a Select operation");
            }
            JsonNode result = client.selectOperation(query);
            return Response.status(Status.OK)
                .entity(result)
                .build();
        } catch (LogbookClientException e) {
            LOGGER.error(e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(
                    new VitamError(status.name()).setHttpCode(status.getStatusCode())
                        .setContext("logbook")
                        .setState("code_vitam")
                        .setMessage(status.getReasonPhrase())
                        .setDescription(status.getReasonPhrase()))
                .build();
        }
    }

    /**
     * @param query as JsonNode
     * @param xhttpOverride header parameter indicate that we use POST with X-Http-Method-Override,
     * @return Response of SELECT query with POST method
     * @throws LogbookException
     * @throws InvalidParseOperationException
     */
    @POST
    @Path("/operations")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectOperationWithPostOverride(String query,
        @HeaderParam("X-HTTP-Method-Override") String xhttpOverride)
        throws InvalidParseOperationException {
        Status status;
        if (xhttpOverride != null && ("GET").equals(xhttpOverride)) {
            return selectOperation(query);
        } else {
            status = Status.PRECONDITION_FAILED;
            return Response.status(status)
                .entity(
                    new VitamError(status.name()).setHttpCode(status.getStatusCode())
                        .setContext("logbook")
                        .setState("code_vitam")
                        .setMessage(status.getReasonPhrase())
                        .setDescription(status.getReasonPhrase()))
                .build();
        }

    }

    /*****
     * OPERATION - END *****
     * 
     * 
     * /***** LOGBOOK LIFE CYCLES
     *****/

    /**
     * gets the unit life cycle based on its id
     * 
     * @param unitLifeCycleId the unit life cycle id
     * @return the unit life cycle
     * @throws InvalidParseOperationException
     */
    @GET
    @Path("/unitlifecycles/{id_lc}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUnitLifeCycle(@PathParam("id_lc") String unitLifeCycleId) throws InvalidParseOperationException {
        Status status;
        try (LogbookLifeCyclesClient client = LogbookLifeCyclesClientFactory.getInstance().getClient()) {
            JsonNode result = client.selectUnitLifeCycleById(unitLifeCycleId);
            return Response.status(Status.OK)
                .entity(result)
                .build();
        } catch (LogbookClientException e) {
            LOGGER.error(e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status)
                .entity(
                    new VitamError(status.name()).setHttpCode(status.getStatusCode())
                        .setContext("logbook")
                        .setState("code_vitam")
                        .setMessage(status.getReasonPhrase())
                        .setDescription(status.getReasonPhrase()))
                .build();
        }
    }


    /**
     * gets the object group life cycle based on its id
     * 
     * @param objectGroupLifeCycleId the object group life cycle id
     * @return the object group life cycle
     * @throws InvalidParseOperationException
     */
    @GET
    @Path("/objectgrouplifecycles/{id_lc}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getObjectGroupLifeCycle(@PathParam("id_lc") String objectGroupLifeCycleId)
        throws InvalidParseOperationException {
        Status status;
        try (LogbookLifeCyclesClient client = LogbookLifeCyclesClientFactory.getInstance().getClient()) {
            final JsonNode result = client.selectObjectGroupLifeCycleById(objectGroupLifeCycleId);
            return Response.status(Status.OK)
                .entity(result)
                .build();
        } catch (LogbookClientException e) {
            LOGGER.error(e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status)
                .entity(
                    new VitamError(status.name()).setHttpCode(status.getStatusCode())
                        .setContext("logbook")
                        .setState("code_vitam")
                        .setMessage(status.getReasonPhrase())
                        .setDescription(status.getReasonPhrase()))
                .build();
        }
    }

    /***** LIFE CYCLES - END *****/
}
