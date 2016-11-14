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

package fr.gouv.vitam.logbook.rest;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.ServerIdentity;
import fr.gouv.vitam.common.client2.VitamRequestIterator;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.server2.application.configuration.DbConfiguration;
import fr.gouv.vitam.common.server2.application.configuration.DbConfigurationImpl;
import fr.gouv.vitam.common.server2.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleObjectGroupParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleUnitParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.server.LogbookDbAccess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycleObjectGroup;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycleUnit;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbAccessFactory;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookOperation;
import fr.gouv.vitam.logbook.common.server.exception.LogbookAlreadyExistsException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookDatabaseException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookNotFoundException;
import fr.gouv.vitam.logbook.lifecycles.api.LogbookLifeCycles;
import fr.gouv.vitam.logbook.lifecycles.core.LogbookLifeCyclesImpl;
import fr.gouv.vitam.logbook.operations.api.LogbookOperations;
import fr.gouv.vitam.logbook.operations.core.LogbookOperationsImpl;

/**
 * Logbook Resource implementation
 */
@Path("/logbook/v1")
@javax.ws.rs.ApplicationPath("webresources")
public class LogbookResource extends ApplicationStatusResource {
    private static final int MAX_NB_PART_ITERATOR = 100;
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogbookResource.class);
    private final LogbookOperations logbookOperation;
    private final LogbookLifeCycles logbookLifeCycle;
    private final DbConfiguration logbookConfiguration;
    private final LogbookDbAccess mongoDbAccess;

    /**
     * Constructor
     *
     * @param configuration
     */
    public LogbookResource(LogbookConfiguration configuration) {
        if (configuration.isDbAuthentication()) {
            logbookConfiguration =
                new DbConfigurationImpl(configuration.getMongoDbNodes(), configuration.getDbName(),
                    true, configuration.getDbUserName(), configuration.getDbPassword());

        } else {
            logbookConfiguration =
                new DbConfigurationImpl(configuration.getMongoDbNodes(),
                    configuration.getDbName());
        }
        mongoDbAccess = LogbookMongoDbAccessFactory.create(logbookConfiguration);
        logbookOperation = new LogbookOperationsImpl(mongoDbAccess);
        LOGGER.debug("LogbookResource operation initialized");

        logbookLifeCycle = new LogbookLifeCyclesImpl(mongoDbAccess);
        LOGGER.debug("LogbookResource lifecycles initialized");
    }

    LogbookDbAccess getLogbookDbAccess() {
        return mongoDbAccess;
    }

    /**
     * Select an operation
     *
     * @param operationId the operation id
     * @return the response with a specific HTTP status
     */
    @GET
    @Path("/operations/{id_op}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOperation(@PathParam("id_op") String operationId) {
        Status status;
        try {
            final LogbookOperation result = logbookOperation.getById(operationId);
            return Response.status(Status.OK)
                .entity(new RequestResponseOK()
                    .setHits(1, 0, 1)
                    .addResult(JsonHandler.getFromString(result.toJson())))
                .build();
        } catch (final LogbookNotFoundException exc) {
            LOGGER.error(exc);
            status = Status.NOT_FOUND;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext("logbook")
                    .setState("code_vitam")
                    .setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        } catch (final InvalidParseOperationException | IllegalArgumentException | LogbookException exc) {
            LOGGER.error(exc);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext("logbook")
                    .setState("code_vitam")
                    .setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        }
    }

    /**
     * Create or Select a new operation
     *
     * @param operationId path param, the operation id
     * @param operation the json serialized as a LogbookOperationParameters.
     * @param xhttpOverride header param as String indicate the use of POST method as GET
     * @return the response with a specific HTTP status
     */
    @POST
    @Path("/operations/{id_op}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createOrSelectOperation(@PathParam("id_op") String operationId,
        LogbookOperationParameters operation,
        @HeaderParam(GlobalDataRest.X_HTTP_METHOD_OVERRIDE) String xhttpOverride) {
        if (xhttpOverride != null && "GET".equals(xhttpOverride)) {
            ParametersChecker.checkParameter("Operation id is required", operationId);
            return getOperation(operationId);
        } else {
            Response finalResponse;
            finalResponse = Response.status(Response.Status.CREATED).build();
            try {
                LOGGER.debug(
                    operation.getParameterValue(LogbookOperation.getIdParameterName()).equals(operationId) + " " +
                        operation.getParameterValue(LogbookOperation.getIdParameterName()) + " =? " + operationId);
                try {
                    ParameterHelper.checkNullOrEmptyParameters(operation);
                } catch (final IllegalArgumentException e) {
                    LOGGER.error("Operations is incorrect", e);
                    return Response.status(Response.Status.BAD_REQUEST).build();
                }
                if (!operation.getParameterValue(LogbookOperation.getIdParameterName()).equals(operationId)) {
                    LOGGER.error("OperationId is not the same as in the operation parameter");
                    return Response.status(Response.Status.BAD_REQUEST).build();
                }

                logbookOperation.create(operation);
            } catch (final LogbookAlreadyExistsException exc) {
                LOGGER.error(exc);
                finalResponse = Response.status(Response.Status.CONFLICT).build();
            } catch (final LogbookDatabaseException exc) {
                LOGGER.error(exc);
                finalResponse = Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            } catch (final IllegalArgumentException exc) {
                LOGGER.error(exc);
                finalResponse = Response.status(Response.Status.BAD_REQUEST).build();
            }
            return finalResponse;
        }
    }


    /**
     * Append a new item on the given operation
     *
     * @param operationId the operation id
     * @param operation the json serialized as a LogbookOperationParameters.
     * @return the response with a specific HTTP status
     */
    @PUT
    @Path("/operations/{id_op}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateOperation(@PathParam("id_op") String operationId, LogbookOperationParameters operation) {
        Response finalResponse = Response.status(Response.Status.OK).build();
        try {
            ParameterHelper.checkNullOrEmptyParameters(operation);
        } catch (final IllegalArgumentException e) {
            LOGGER.error("Operations is incorrect", e);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        if (!operation.getParameterValue(LogbookOperation.getIdParameterName()).equals(operationId)) {
            LOGGER.error("OperationId is not the same as in the operation parameter");
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        try {
            logbookOperation.update(operation);
        } catch (final LogbookNotFoundException exc) {
            LOGGER.error(exc);
            finalResponse = Response.status(Response.Status.NOT_FOUND).build();
        } catch (final LogbookDatabaseException exc) {
            LOGGER.error(exc);
            finalResponse = Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } catch (final IllegalArgumentException exc) {
            LOGGER.error(exc);
            finalResponse = Response.status(Response.Status.BAD_REQUEST).build();
        }
        return finalResponse;
    }

    /**
     * Select a list of operations
     * 
     * @param query DSL as String
     * @return Response containt the list of loglook operation
     */
    @GET
    @Path("/operations")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    // FIXME P0 changer String en JsonNode pour toutes les Query
    public Response selectOperation(String query) {
        Status status;
        try {
            final List<LogbookOperation> result = logbookOperation.select(JsonHandler.getFromString(query));
            final ArrayNode resultAsJson = JsonHandler.createArrayNode();
            for (LogbookOperation logbook : result) {
                resultAsJson.add(JsonHandler.toJsonNode(logbook));
            }
            return Response.status(Status.OK)
                .entity(new RequestResponseOK()
                    .setHits(result.size(), 0, 1)
                    .setQuery(JsonHandler.getFromString(query))
                    .addAllResults(resultAsJson))
                .build();
        } catch (final LogbookNotFoundException exc) {
            LOGGER.error(exc);
            status = Status.NOT_FOUND;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext("logbook")
                    .setState("code_vitam")
                    .setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        } catch (final InvalidParseOperationException | IllegalArgumentException | LogbookException exc) {
            LOGGER.error(exc);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext("logbook")
                    .setState("code_vitam")
                    .setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        }
    }


    /**
     * select Operation With Post Override Or Bulk Create
     * 
     * @param query as JsonNode or Operations Logbooks as ArrayNode
     * @param xhttpOverride header parameter indicate that we use POST with X-Http-Method-Override,
     * @return Response of SELECT query with POST method or CREATED for not GET Overriden method
     */
    @POST
    @Path("/operations")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectOperationWithPostOverrideOrBulkCreate(String query,
        @HeaderParam(GlobalDataRest.X_HTTP_METHOD_OVERRIDE) String xhttpOverride) {
        if (xhttpOverride != null && HttpMethod.GET.equals(xhttpOverride)) {
            return selectOperation(query);
        } else {
            // query is in fact a bulk LogbookOperationsParameter
            try {
                ParametersChecker.checkParameter("Logbook parameters", query);
            } catch (final IllegalArgumentException e) {
                LOGGER.error("Operations is incorrect", e);
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            try {
                LogbookOperationParameters[] arrayOperations =
                    JsonHandler.getFromString(query, LogbookOperationParameters[].class);
                logbookOperation.createBulkLogbookOperation(arrayOperations);
            } catch (LogbookDatabaseException e) {
                LOGGER.error(e);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            } catch (LogbookAlreadyExistsException e) {
                LOGGER.error(e);
                return Response.status(Response.Status.CONFLICT).build();
            } catch (InvalidParseOperationException | IllegalArgumentException e) {
                LOGGER.error(e);
                Status status = Status.PRECONDITION_FAILED;
                return Response.status(status)
                    .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                        .setContext("logbook")
                        .setState("code_vitam")
                        .setMessage(status.getReasonPhrase())
                        .setDescription(e.getMessage()))
                    .build();
            }
            return Response.status(Response.Status.CREATED).build();
        }

    }

    /**
     * Update Operation With Bulk Mode
     * 
     * @param arrayNodeOperations as ArrayNode of operations to add to existing Operation Logbook entry
     * @return Response with a status of OK if updated
     */
    @PUT
    @Path("/operations")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    // Note: here let String since we need JsonHandler to parser the object
    public Response updateOperationBulk(String arrayNodeOperations) {
        try {
            LogbookOperationParameters[] arrayOperations =
                JsonHandler.getFromString(arrayNodeOperations, LogbookOperationParameters[].class);
            logbookOperation.updateBulkLogbookOperation(arrayOperations);
        } catch (final LogbookNotFoundException exc) {
            LOGGER.error(exc);
            return Response.status(Response.Status.NOT_FOUND).build();
        } catch (final LogbookDatabaseException exc) {
            LOGGER.error(exc);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } catch (final IllegalArgumentException | InvalidParseOperationException exc) {
            LOGGER.error(exc);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        return Response.status(Response.Status.OK).build();
    }

    /***** LIFE CYCLES UNIT - START *****/

    /**
     * GET multiple Unit Life Cycles through VitamRequestIterator
     *
     * @param operationId the operation id
     * @param xcursor if True means new query, if False means end of query from client side
     * @param xcursorId if present, means continue on Cursor
     * @param query as JsonNode
     * @return the response with a specific HTTP status
     */
    @GET
    @Path("/operations/{id_op}/unitlifecycles")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    // FIXME P0 changer String en JsonNode pour toutes les Query
    public Response getUnitLifeCyclesByOperation(@PathParam("id_op") String operationId,
        @HeaderParam(GlobalDataRest.X_CURSOR) boolean xcursor,
        @HeaderParam(GlobalDataRest.X_CURSOR_ID) String xcursorId, String query) {
        Status status;
        try {
            String cursorId = xcursorId;
            if (VitamRequestIterator.isEndOfCursor(xcursor, xcursorId)) {
                // terminate the cursor
                logbookLifeCycle.finalizeCursor(cursorId);
                ResponseBuilder builder = Response.status(Status.NO_CONTENT);
                return VitamRequestIterator.setHeaders(builder, xcursor, null).build();
            }
            JsonNode nodeQuery = JsonHandler.createObjectNode();
            if (VitamRequestIterator.isNewCursor(xcursor, xcursorId)) {
                // check null or empty parameters
                ParametersChecker.checkParameter("Arguments must not be null", operationId, query);
                // create the cursor
                nodeQuery = JsonHandler.getFromString(query);
                cursorId = logbookLifeCycle.createCursorUnit(operationId, nodeQuery);
            }
            RequestResponseOK responseOK =
                new RequestResponseOK().setQuery(nodeQuery);
            int nb = 0;
            try {
                for (; nb < MAX_NB_PART_ITERATOR; nb++) {
                    LogbookLifeCycleUnit lcUnit = logbookLifeCycle.getCursorUnitNext(cursorId);
                    responseOK.addResult(JsonHandler.toJsonNode(lcUnit));
                }
            } catch (LogbookNotFoundException e) {
                // Ignore
                SysErrLogger.FAKE_LOGGER.ignoreLog(e);
            }
            ResponseBuilder builder = Response.status(nb < MAX_NB_PART_ITERATOR ? Status.OK : Status.PARTIAL_CONTENT)
                .entity(new RequestResponseOK()
                    .setHits(nb, 0, nb).setQuery(nodeQuery));
            return VitamRequestIterator.setHeaders(builder, xcursor, cursorId).build();
        } catch (final LogbookDatabaseException exc) {
            LOGGER.error(exc);
            status = Status.INTERNAL_SERVER_ERROR;
            ResponseBuilder builder = Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(ServerIdentity.getInstance().getRole()).setState("code_vitam")
                    .setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()));
            return VitamRequestIterator.setHeaders(builder, xcursor, null).build();
        } catch (final IllegalArgumentException | InvalidParseOperationException exc) {
            LOGGER.error(exc);
            status = Status.BAD_REQUEST;
            ResponseBuilder builder = Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(ServerIdentity.getInstance().getRole()).setState("code_vitam")
                    .setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()));
            return VitamRequestIterator.setHeaders(builder, xcursor, null).build();
        }
    }

    /**
     * Create Unit Life Cycle
     *
     * @param operationId the operation id
     * @param unitLcId the life cycle id
     * @param parameters the json serialized as a LogbookLifeCycleUnitParameters.
     * @return the response with a specific HTTP status
     */
    @POST
    @Path("/operations/{id_op}/unitlifecycles/{id_lc}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createUnitLifeCyclesByOperation(@PathParam("id_op") String operationId,
        @PathParam("id_lc") String unitLcId, LogbookLifeCycleUnitParameters parameters) {
        Status status;
        try {
            try {
                // check null or empty parameters
                ParameterHelper.checkNullOrEmptyParameters(parameters);
            } catch (final IllegalArgumentException e) {
                LOGGER.error("unit lifecycles is incorrect", e);
                status = Status.BAD_REQUEST;
                return Response.status(status)
                    .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                        .setContext("logbook").setState("code_vitam").setMessage(status.getReasonPhrase())
                        .setDescription(e.getMessage()))
                    .build();
            }
            /**
             * create unit logbook Life cycle
             */
            logbookLifeCycle.createUnit(operationId, unitLcId, parameters);

        } catch (final LogbookAlreadyExistsException exc) {
            LOGGER.error(exc);
            status = Status.CONFLICT;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext("logbook").setState("code_vitam").setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        } catch (final LogbookDatabaseException exc) {
            LOGGER.error(exc);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext("logbook").setState("code_vitam").setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        } catch (final IllegalArgumentException exc) {
            LOGGER.error(exc);
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext("logbook").setState("code_vitam").setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        }
        return Response.status(Response.Status.CREATED).build();

    }

    /**
     * Update Unit Life Cycle
     *
     * @param operationId the operation id
     * @param unitLcId the life cycle id
     * @param parameters the json serialized as a LogbookLifeCycleUnitParameters.
     * @return the response with a specific HTTP status
     */
    @PUT
    @Path("/operations/{id_op}/unitlifecycles/{id_lc}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateUnitLifeCyclesByOperation(@PathParam("id_op") String operationId,
        @PathParam("id_lc") String unitLcId, LogbookLifeCycleUnitParameters parameters) {
        Status status;
        try {
            try {
                // check null or empty parameters
                ParameterHelper.checkNullOrEmptyParameters(parameters);
            } catch (final IllegalArgumentException e) {
                LOGGER.error("unit lifecycles is incorrect", e);
                status = Status.BAD_REQUEST;
                return Response.status(status)
                    .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                        .setContext("logbook").setState("code_vitam").setMessage(status.getReasonPhrase())
                        .setDescription(e.getMessage()))
                    .build();
            }
            /**
             * update unit logbook Life cycle
             */
            logbookLifeCycle.updateUnit(operationId, unitLcId, parameters);

        } catch (final LogbookNotFoundException exc) {
            LOGGER.error(exc);
            status = Status.NOT_FOUND;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext("logbook").setState("code_vitam").setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        } catch (final LogbookDatabaseException exc) {
            LOGGER.error(exc);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext("logbook").setState("code_vitam").setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        } catch (final IllegalArgumentException exc) {
            LOGGER.error(exc);
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext("logbook").setState("code_vitam").setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        }
        return Response.status(Response.Status.OK).build();
    }

    /**
     * Delete Unit Life Cycle
     *
     * @param operationId the operation id
     * @param unitLcId the life cycle id
     * @return the response with a specific HTTP status
     */
    @DELETE
    @Path("/operations/{id_op}/unitlifecycles/{id_lc}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteUnitLifeCyclesByOperation(@PathParam("id_op") String operationId,
        @PathParam("id_lc") String unitLcId) {
        Status status;
        try {
            logbookLifeCycle.rollbackUnit(operationId, unitLcId);
        } catch (final LogbookNotFoundException exc) {
            LOGGER.error(exc);
            status = Status.NOT_FOUND;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext("logbook").setState("code_vitam").setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        } catch (final LogbookDatabaseException exc) {
            LOGGER.error(exc);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext("logbook").setState("code_vitam").setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        } catch (final IllegalArgumentException exc) {
            LOGGER.error(exc);
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext("logbook").setState("code_vitam").setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        }
        return Response.status(Response.Status.OK).build();
    }

    /**
     * Commit Unit Life Cycle
     *
     * @param operationId the operation id
     * @param unitLcId the life cycle id
     * @return the response with a specific HTTP status
     */
    @PUT
    @Path("/operations/{id_op}/unitlifecycles/{id_lc}/commit")
    @Produces(MediaType.APPLICATION_JSON)
    public Response commitUnitLifeCyclesByOperation(@PathParam("id_op") String operationId,
        @PathParam("id_lc") String unitLcId) {
        LOGGER.debug("UnitLifeCycle commited: " + unitLcId);
        return Response.status(Response.Status.OK).build();
    }

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
        try {
            final LogbookLifeCycleUnit result = logbookLifeCycle.getUnitById(unitLifeCycleId);
            return Response.status(Status.OK)
                .entity(new RequestResponseOK()
                    .setHits(1, 0, 1)
                    .addResult(JsonHandler.getFromString(result.toJson())))
                .build();
        } catch (final LogbookNotFoundException exc) {
            return Response.status(Status.OK)
                .entity(new RequestResponseOK()
                    .setHits(0, 0, 1)
                    .addResult(JsonHandler.createArrayNode()))
                .build();
        } catch (final LogbookException | IllegalArgumentException exc) {
            LOGGER.error(exc);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext("logbook")
                    .setState("code_vitam")
                    .setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        }
    }

    /***** LIFE CYCLES UNIT - END *****/

    /***** LIFE CYCLES OBJECT GROUP - START *****/
    /**
     * GET multiple Unit Life Cycles through VitamRequestIterator
     *
     * @param operationId the operation id
     * @param xcursor if True means new query, if False means end of query from client side
     * @param xcursorId if present, means continue on Cursor
     * @param query as JsonNode
     * @return the response with a specific HTTP status
     */
    @GET
    @Path("/operations/{id_op}/objectgrouplifecycles")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    // FIXME P0 changer String en JsonNode pour toutes les Query
    public Response getObjectGroupLifeCyclesByOperation(@PathParam("id_op") String operationId,
        @HeaderParam(GlobalDataRest.X_CURSOR) boolean xcursor,
        @HeaderParam(GlobalDataRest.X_CURSOR_ID) String xcursorId, String query) {
        Status status;
        try {
            String cursorId = xcursorId;
            if (VitamRequestIterator.isEndOfCursor(xcursor, xcursorId)) {
                // terminate the cursor
                logbookLifeCycle.finalizeCursor(cursorId);
                ResponseBuilder builder = Response.status(Status.NO_CONTENT);
                return VitamRequestIterator.setHeaders(builder, xcursor, null).build();
            }
            JsonNode nodeQuery = JsonHandler.createObjectNode();
            if (VitamRequestIterator.isNewCursor(xcursor, xcursorId)) {
                // check null or empty parameters
                ParametersChecker.checkParameter("Arguments must not be null", operationId, query);
                // create the cursor
                nodeQuery = JsonHandler.getFromString(query);
                cursorId = logbookLifeCycle.createCursorObjectGroup(operationId, nodeQuery);
            }
            fr.gouv.vitam.common.model.RequestResponseOK responseOK =
                new fr.gouv.vitam.common.model.RequestResponseOK().setQuery(nodeQuery);
            int nb = 0;
            try {
                for (; nb < MAX_NB_PART_ITERATOR; nb++) {
                    LogbookLifeCycleObjectGroup lcObjectGroup = logbookLifeCycle.getCursorObjectGroupNext(cursorId);
                    responseOK.addResult(JsonHandler.toJsonNode(lcObjectGroup));
                }
            } catch (LogbookNotFoundException e) {
                // Ignore
                SysErrLogger.FAKE_LOGGER.ignoreLog(e);
            }
            ResponseBuilder builder = Response.status(nb < MAX_NB_PART_ITERATOR ? Status.OK : Status.PARTIAL_CONTENT)
                .entity(new fr.gouv.vitam.common.model.RequestResponseOK()
                    .setHits(nb, 0, nb).setQuery(nodeQuery));
            return VitamRequestIterator.setHeaders(builder, xcursor, cursorId).build();
        } catch (final LogbookDatabaseException exc) {
            LOGGER.error(exc);
            status = Status.INTERNAL_SERVER_ERROR;
            ResponseBuilder builder = Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext("logbook").setState("code_vitam").setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()));
            return VitamRequestIterator.setHeaders(builder, xcursor, null).build();
        } catch (final IllegalArgumentException | InvalidParseOperationException exc) {
            LOGGER.error(exc);
            status = Status.BAD_REQUEST;
            ResponseBuilder builder = Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext("logbook").setState("code_vitam").setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()));
            return VitamRequestIterator.setHeaders(builder, xcursor, null).build();
        }
    }

    /**
     * Create object Group Life Cycle
     *
     * @param operationId the operation id
     * @param objGrpId the life cycle id
     * @param parameters the json serialized as a LogbookLifeCycleObjectGroupParameters.
     * @return the response with a specific HTTP status
     */
    @POST
    @Path("/operations/{id_op}/objectgrouplifecycles/{id_lc}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createObjectGroupLifeCyclesByOperation(@PathParam("id_op") String operationId,
        @PathParam("id_lc") String objGrpId, LogbookLifeCycleObjectGroupParameters parameters) {
        Status status;

        try {
            try {
                // check null or empty parameters
                ParameterHelper.checkNullOrEmptyParameters(parameters);
            } catch (final IllegalArgumentException e) {
                LOGGER.error("objectgrouplifecycles is incorrect", e);
                status = Status.BAD_REQUEST;
                return Response.status(status)
                    .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                        .setContext("logbook").setState("code_vitam").setMessage(status.getReasonPhrase())
                        .setDescription(e.getMessage()))
                    .build();
            }
            /**
             * create objectgroup logbook Life cycle
             */
            logbookLifeCycle.createObjectGroup(operationId, objGrpId, parameters);

        } catch (final LogbookAlreadyExistsException exc) {
            LOGGER.error(exc);
            status = Status.CONFLICT;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext("logbook").setState("code_vitam").setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        } catch (final LogbookDatabaseException exc) {
            LOGGER.error(exc);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext("logbook").setState("code_vitam").setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        } catch (final IllegalArgumentException exc) {
            LOGGER.error(exc);
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext("logbook").setState("code_vitam").setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        }
        return Response.status(Response.Status.CREATED).build();
    }

    /**
     * Update object Group Life Cycle
     *
     * @param operationId the operation id
     * @param objGrpId the life cycle id
     * @param parameters the json serialized as a LogbookLifeCycleObjectGroupParameters.
     * @return the response with a specific HTTP status
     */
    @PUT
    @Path("/operations/{id_op}/objectgrouplifecycles/{id_lc}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateObjectGroupLifeCyclesByOperation(@PathParam("id_op") String operationId,
        @PathParam("id_lc") String objGrpId, LogbookLifeCycleObjectGroupParameters parameters) {
        Status status;
        try {
            try {
                // check null or empty parameters
                ParameterHelper.checkNullOrEmptyParameters(parameters);
            } catch (final IllegalArgumentException e) {
                LOGGER.error("objectgrouplifecycles is incorrect", e);
                status = Status.BAD_REQUEST;
                return Response.status(status)
                    .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                        .setContext("logbook").setState("code_vitam").setMessage(status.getReasonPhrase())
                        .setDescription(e.getMessage()))
                    .build();
            }
            /**
             * update object group logbook Life cycle
             */
            logbookLifeCycle.updateObjectGroup(operationId, objGrpId, parameters);
        } catch (final LogbookNotFoundException exc) {
            LOGGER.error(exc);
            status = Status.NOT_FOUND;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext("logbook").setState("code_vitam").setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        } catch (final LogbookDatabaseException exc) {
            LOGGER.error(exc);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext("logbook").setState("code_vitam").setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        } catch (final IllegalArgumentException exc) {
            LOGGER.error(exc);
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext("logbook").setState("code_vitam").setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        }
        return Response.status(Response.Status.OK).build();
    }

    /**
     * Delete object Group Life Cycle
     *
     * @param operationId the operation id
     * @param objGrpId the life cycle id
     * @return the response with a specific HTTP status
     */
    @DELETE
    @Path("/operations/{id_op}/objectgrouplifecycles/{id_lc}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteObjectGroupLifeCyclesByOperation(@PathParam("id_op") String operationId,
        @PathParam("id_lc") String objGrpId) {
        Status status;
        try {
            logbookLifeCycle.rollbackObjectGroup(operationId, objGrpId);
        } catch (final LogbookNotFoundException exc) {
            LOGGER.error(exc);
            status = Status.NOT_FOUND;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext("logbook").setState("code_vitam").setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        } catch (final LogbookDatabaseException exc) {
            LOGGER.error(exc);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext("logbook").setState("code_vitam").setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        } catch (final IllegalArgumentException exc) {
            LOGGER.error(exc);
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext("logbook").setState("code_vitam").setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        }
        return Response.status(Response.Status.OK).build();
    }

    /**
     * Commit object Group Life Cycle
     *
     * @param operationId the operation id
     * @param objGrpId the life cycle id
     * @param parameters the json serialized as a LogbookLifeCycleObjectGroupParameters.
     * @return the response with a specific HTTP status
     */
    @PUT
    @Path("/operations/{id_op}/objectgrouplifecycles/{id_lc}/commit")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response commitObjectGroupLifeCyclesByOperation(@PathParam("id_op") String operationId,
        @PathParam("id_lc") String objGrpId) {
        LOGGER.debug("ObjectGroupLifeCycle commited: " + objGrpId);
        return Response.status(Response.Status.OK).build();
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
        try {
            final LogbookLifeCycleObjectGroup result = logbookLifeCycle.getObjectGroupById(objectGroupLifeCycleId);
            return Response.status(Status.OK)
                .entity(new RequestResponseOK()
                    .setHits(1, 0, 1)
                    .addResult(JsonHandler.getFromString(result.toJson())))
                .build();
        } catch (final LogbookNotFoundException exc) {
            return Response.status(Status.OK)
                .entity(new RequestResponseOK()
                    .setHits(0, 0, 1)
                    .addResult(JsonHandler.createArrayNode()))
                .build();
        } catch (final LogbookException exc) {
            LOGGER.error(exc);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext("logbook")
                    .setState("code_vitam")
                    .setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        }
    }

    /***** LIFE CYCLES OBJECT GROUP - END *****/

}
