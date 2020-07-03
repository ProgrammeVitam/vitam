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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import com.mongodb.client.MongoCursor;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.ServerIdentity;
import fr.gouv.vitam.common.client.OntologyLoader;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.index.model.IndexationResult;
import fr.gouv.vitam.common.database.parameter.IndexParameters;
import fr.gouv.vitam.common.database.parameter.SwitchIndexParameters;
import fr.gouv.vitam.common.database.parser.request.single.SelectParserSingle;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamDBException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.exception.WorkflowNotFoundException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.LifeCycleStatusCode;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.common.timestamp.TimeStampSignature;
import fr.gouv.vitam.common.timestamp.TimeStampSignatureWithKeystore;
import fr.gouv.vitam.common.timestamp.TimestampGenerator;
import fr.gouv.vitam.logbook.administration.audit.core.LogbookAuditAdministration;
import fr.gouv.vitam.logbook.administration.audit.exception.LogbookAuditException;
import fr.gouv.vitam.logbook.administration.core.LfcTraceabilityType;
import fr.gouv.vitam.logbook.administration.core.LogbookAdministration;
import fr.gouv.vitam.logbook.administration.core.LogbookLFCAdministration;
import fr.gouv.vitam.logbook.common.exception.TraceabilityException;
import fr.gouv.vitam.logbook.common.model.AuditLogbookOptions;
import fr.gouv.vitam.logbook.common.model.LifecycleTraceabilityStatus;
import fr.gouv.vitam.logbook.common.model.LogbookLifeCycleObjectGroupModel;
import fr.gouv.vitam.logbook.common.model.LogbookLifeCycleUnitModel;
import fr.gouv.vitam.logbook.common.model.RawLifecycleByLastPersistedDateRequest;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleObjectGroupParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleParametersBulk;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleUnitParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.server.LogbookConfiguration;
import fr.gouv.vitam.logbook.common.server.LogbookDbAccess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookDocument;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycle;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycleObjectGroup;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycleUnit;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbAccessFactory;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbName;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookOperation;
import fr.gouv.vitam.logbook.common.server.database.collections.request.LogbookVarNameAdapter;
import fr.gouv.vitam.logbook.common.server.exception.LogbookAlreadyExistsException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookDatabaseException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookNotFoundException;
import fr.gouv.vitam.logbook.lifecycles.api.LogbookLifeCycles;
import fr.gouv.vitam.logbook.lifecycles.core.LogbookLifeCyclesImpl;
import fr.gouv.vitam.logbook.operations.api.LogbookOperations;
import fr.gouv.vitam.logbook.operations.core.AlertLogbookOperationsDecorator;
import fr.gouv.vitam.logbook.operations.core.LogbookOperationsImpl;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

import javax.annotation.PostConstruct;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import java.io.File;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;

/**
 * Logbook Resource implementation
 */
@Path("/logbook/v1")
public class LogbookResource extends ApplicationStatusResource {

    private static final String LOGBOOK = "logbook";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogbookResource.class);
    private static final String CODE_VITAM = "code_vitam";
    private final LogbookOperations logbookOperation;
    private final LogbookLifeCycles logbookLifeCycle;
    private final LogbookConfiguration logbookConfiguration;
    private final LogbookDbAccess mongoDbAccess;
    private final LogbookAdministration logbookAdministration;
    private final LogbookLFCAdministration logbookLFCAdministration;
    private final LogbookAuditAdministration logbookAuditAdministration;
    private static final String MISSING_THE_TENANT_ID_X_TENANT_ID =
        "Missing the tenant ID (X-Tenant-Id) or wrong object Type";

    private static final String AUDIT_TRACEABILITY_URI = "/auditTraceability";

    /**
     * Constructor
     *
     * @param configuration of type LogbookConfiguration
     * @param ontologyLoader
     */
    public LogbookResource(LogbookConfiguration configuration, OntologyLoader ontologyLoader) {
        if (configuration.isDbAuthentication()) {
            logbookConfiguration =
                new LogbookConfiguration(configuration.getMongoDbNodes(), configuration.getDbName(),
                    configuration.getClusterName(), configuration.getElasticsearchNodes(), true,
                    configuration.getDbUserName(), configuration.getDbPassword());

        } else {
            logbookConfiguration =
                new LogbookConfiguration(configuration.getMongoDbNodes(), configuration.getDbName(),
                    configuration.getClusterName(), configuration.getElasticsearchNodes());
        }
        mongoDbAccess = LogbookMongoDbAccessFactory.create(logbookConfiguration, ontologyLoader);

        logbookOperation = new AlertLogbookOperationsDecorator(new LogbookOperationsImpl(mongoDbAccess),
            configuration.getAlertEvents());

        TimeStampSignature timeStampSignature;
        try {
            final File file = PropertiesUtils.findFile(configuration.getP12LogbookFile());
            timeStampSignature =
                new TimeStampSignatureWithKeystore(file, configuration.getP12LogbookPassword().toCharArray());
        } catch (KeyStoreException | CertificateException | IOException | UnrecoverableKeyException |
            NoSuchAlgorithmException e) {
            LOGGER.error("unable to instantiate TimeStampGenerator", e);
            throw new RuntimeException(e);
        }
        final TimestampGenerator timestampGenerator = new TimestampGenerator(timeStampSignature);
        final WorkspaceClientFactory clientFactory = WorkspaceClientFactory.getInstance();
        WorkspaceClientFactory.changeMode(configuration.getWorkspaceUrl());

        logbookAdministration = new LogbookAdministration(logbookOperation, timestampGenerator,
            configuration.getOperationTraceabilityTemporizationDelay());

        final ProcessingManagementClientFactory processClientFactory = ProcessingManagementClientFactory.getInstance();
        ProcessingManagementClientFactory.changeConfigurationUrl(configuration.getProcessingUrl());

        logbookLFCAdministration = new LogbookLFCAdministration(logbookOperation, processClientFactory,
            clientFactory, configuration.getLifecycleTraceabilityTemporizationDelay(),
            configuration.getLifecycleTraceabilityMaxEntries());

        logbookAuditAdministration = new LogbookAuditAdministration(logbookOperation);

        LOGGER.debug("LogbookResource operation initialized");

        logbookLifeCycle = new LogbookLifeCyclesImpl(mongoDbAccess);
        LOGGER.debug("LogbookResource lifecycles initialized");
    }

    /**
     * Selects an operation only by Id
     *
     * @param id operation ID
     * @return the response with a specific HTTP status
     */
    @GET
    @Path("/operations/{id_op}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOperationOnlyById(@PathParam("id_op") String id) {
        Status status;
        try {
            final LogbookOperation result = logbookOperation.getById(id);
            return Response.status(Status.OK)
                .entity(
                    new RequestResponseOK<LogbookOperation>(new Select().getFinalSelect()).addResult(result)
                        .setHttpCode(Status.OK.getStatusCode()))
                .build();
        } catch (final LogbookNotFoundException exc) {
            LOGGER.debug(exc);
            status = Status.NOT_FOUND;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(LOGBOOK)
                    .setState("code_vitam")
                    .setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        } catch (final IllegalArgumentException | LogbookException exc) {
            LOGGER.error(exc);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(LOGBOOK)
                    .setState("code_vitam")
                    .setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        }
    }

    /**
     * Selects an operation
     *
     * @param id operation ID
     * @param queryDsl the query containing the ID
     * @return the response with a specific HTTP status
     */
    @GET
    @Path("/operations/{id_op}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOperation(@PathParam("id_op") String id, JsonNode queryDsl) throws VitamException {
        Status status;
        try {
            // With resteasy, queryDsl couldnt be null
            if (queryDsl == null ||
                (queryDsl.get("$query") != null && queryDsl.get("$query").size() == 0)) {
                final LogbookOperation result = logbookOperation.getById(id);
                return Response.status(Status.OK)
                    .entity(
                        new RequestResponseOK<LogbookOperation>(queryDsl).addResult(result)
                            .setHttpCode(Status.OK.getStatusCode()))
                    .build();
            } else {

                final List<LogbookOperation> result;
                result = logbookOperation.select(queryDsl, false);
                if (result.size() != 1) {
                    // TODO: Seriously ? Slice is false, select may return a list of operations. Why is this an error ?
                    throw new LogbookDatabaseException("Result size different than 1.");
                }
                return Response.status(Status.OK)
                    .entity(new RequestResponseOK<LogbookOperation>(queryDsl)
                        .addResult(result.iterator().next())
                        .setHttpCode(Status.OK.getStatusCode()))
                    .build();
            }
        } catch (final LogbookNotFoundException exc) {
            LOGGER.debug(exc);
            status = Status.NOT_FOUND;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(LOGBOOK)
                    .setState("code_vitam")
                    .setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        } catch (final InvalidParseOperationException | IllegalArgumentException | LogbookException exc) {
            LOGGER.error(exc);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(LOGBOOK)
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
     * @return the response with a specific HTTP status
     */
    @POST
    @Path("/operations/{id_op}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createOperation(@PathParam("id_op") String operationId,
        LogbookOperationParameters operation) {
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
            LOGGER.debug(exc);
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
     * Run traceability secure operation for logbook
     *
     * @param xTenantId the tenant id
     * @return the response with a specific HTTP status
     */
    @POST
    @Path("/operations/traceability")
    @Produces(MediaType.APPLICATION_JSON)
    public Response traceability(@HeaderParam(GlobalDataRest.X_TENANT_ID) String xTenantId) {
        if (Strings.isNullOrEmpty(xTenantId)) {
            LOGGER.error(MISSING_THE_TENANT_ID_X_TENANT_ID);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        try {
            Integer tenantId = Integer.parseInt(xTenantId);
            GUID guid = GUIDFactory.newOperationLogbookGUID(tenantId);

            VitamThreadUtils.getVitamSession().setTenantId(tenantId);
            VitamThreadUtils.getVitamSession().setRequestId(guid);

            logbookAdministration.generateSecureLogbook(guid);
            final List<String> resultAsJson = new ArrayList<>();

            resultAsJson.add(guid.toString());
            return Response.status(Status.OK)
                .entity(new RequestResponseOK<String>()
                    .addAllResults(resultAsJson)
                    .setHits(1, 0, 1)
                    .setHttpCode(Status.OK.getStatusCode()))
                .build();

        } catch (TraceabilityException e) {
            LOGGER.error("unable to generate traceability log", e);
            // FIXME: Why put a responseOK when exception catch ?
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(new RequestResponseOK()
                    .setHttpCode(Status.INTERNAL_SERVER_ERROR.getStatusCode()))
                .build();
        }
    }

    /**
     * Bulk Create Operation
     *
     * @param query as JsonNode or Operations Logbooks as ArrayNode
     * @return Response of SELECT query with POST method or CREATED
     */
    @POST
    @Path("/operations")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response bulkCreateOperation(JsonNode query) {
        // query is in fact a bulk LogbookOperationsParameter
        try {
            ParametersChecker.checkParameter("Logbook parameters", query);
        } catch (final IllegalArgumentException e) {
            LOGGER.error("Operations is incorrect", e);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        try {
            final LogbookOperationParameters[] arrayOperations =
                JsonHandler.getFromJsonNode(query, LogbookOperationParameters[].class);
            logbookOperation.createBulkLogbookOperation(arrayOperations);
        } catch (final LogbookDatabaseException e) {
            LOGGER.error(e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } catch (final LogbookAlreadyExistsException e) {
            LOGGER.error(e);
            return Response.status(Response.Status.CONFLICT).build();
        } catch (InvalidParseOperationException | IllegalArgumentException e) {
            LOGGER.error(e);
            final Status status = Status.PRECONDITION_FAILED;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(LOGBOOK)
                    .setState("code_vitam")
                    .setMessage(status.getReasonPhrase())
                    .setDescription(e.getMessage()))
                .build();
        }
        return Response.status(Response.Status.CREATED).build();

    }


    /**
     * Select a list of operations
     *
     * @param query DSL as JsonNode
     * @return Response containt the list of loglook operation
     */
    @GET
    @Path("/operations")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectOperation(JsonNode query) {
        return selectOperation(query, false);
    }

    @GET
    @Path("/slicedOperations")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectOperationSliced(JsonNode query) {
        return selectOperation(query, true);
    }

    private Response selectOperation(JsonNode query, boolean sliced) {
        Status status;
        try {
            final RequestResponse<LogbookOperation> result = logbookOperation.selectOperations(query, sliced);

            return Response.status(Status.OK)
                    .entity(result
                            .setHttpCode(Status.OK.getStatusCode()))
                    .build();
        } catch (final VitamDBException ve) {
            LOGGER.error(ve);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                    .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                            .setContext(LOGBOOK)
                            .setState(CODE_VITAM)
                            .setMessage(ve.getMessage())
                            .setDescription(status.getReasonPhrase()))
                    .build();
        } catch (final LogbookNotFoundException exc) {
            LOGGER.debug(exc);
            status = Status.NOT_FOUND;
            return Response.status(status)
                    .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                            .setContext(LOGBOOK)
                            .setState("code_vitam")
                            .setMessage(status.getReasonPhrase())
                            .setDescription(exc.getMessage()))
                    .build();
        } catch (final IllegalArgumentException | LogbookException exc) {
            LOGGER.error(exc);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status)
                    .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                            .setContext(LOGBOOK)
                            .setState("code_vitam")
                            .setMessage(status.getReasonPhrase())
                            .setDescription(exc.getMessage()))
                    .build();
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
            final LogbookOperationParameters[] arrayOperations =
                JsonHandler.getFromString(arrayNodeOperations, LogbookOperationParameters[].class);
            logbookOperation.updateBulkLogbookOperation(arrayOperations);
        } catch (final LogbookNotFoundException exc) {
            LOGGER.debug(exc);
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
     * @param evtStatus the evenement status (commited / not_commited)
     * @param query as JsonNode
     * @return the response with a specific HTTP status
     */
    @GET
    @Path("/operations/{id_op}/unitlifecycles")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUnitLifeCyclesByOperation(@PathParam("id_op") String operationId,
        @HeaderParam(GlobalDataRest.X_EVENT_STATUS) String evtStatus, JsonNode query) {
        Status status;
        List<JsonNode> objects = new ArrayList<>();
        try {
            final Select newQuery = addConditionToQuery(operationId, query);
            try (MongoCursor<LogbookLifeCycleUnit> iterator = mongoDbAccess
                .getLogbookLifeCycleUnitsFull(
                    fromLifeCycleStatusToUnitCollection(getSelectLifeCycleStatusCode(evtStatus)),
                    newQuery)) {
                while (iterator.hasNext()) {
                    objects.add(JsonHandler.toJsonNode(iterator.next()));
                }
                status = Status.OK;
                final ResponseBuilder builder =
                    Response.status(status)
                        .entity(new RequestResponseOK<JsonNode>().addAllResults(objects));
                return builder.build();
            }
        } catch (final LogbookDatabaseException | InvalidParseOperationException |
            InvalidCreateOperationException exc) {
            LOGGER.error(exc);
            status = Status.INTERNAL_SERVER_ERROR;
            final ResponseBuilder builder = Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(ServerIdentity.getInstance().getRole()).setState("code_vitam")
                    .setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()));
            return builder.build();
        } catch (final IllegalArgumentException exc) {
            LOGGER.error(exc);
            status = Status.BAD_REQUEST;
            final ResponseBuilder builder = Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(ServerIdentity.getInstance().getRole()).setState("code_vitam")
                    .setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()));
            return builder.build();
        }
    }

    private Select addConditionToQuery(String operationId, JsonNode query)
        throws InvalidParseOperationException, InvalidCreateOperationException {
        final SelectParserSingle parser = new SelectParserSingle(new LogbookVarNameAdapter());
        parser.parse(query);
        parser.addCondition(QueryHelper.or()
            .add(QueryHelper.eq(LogbookMongoDbName.eventIdentifierProcess.getDbname(), operationId))
            .add(QueryHelper.eq(
                LogbookDocument.EVENTS + '.' + LogbookMongoDbName.eventIdentifierProcess.getDbname(),
                operationId)));
        return parser.getRequest();
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
                        .setContext(LOGBOOK).setState("code_vitam").setMessage(status.getReasonPhrase())
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
                    .setContext(LOGBOOK).setState("code_vitam").setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        } catch (final LogbookDatabaseException exc) {
            LOGGER.error(exc);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(new VitamError(status.name())
                    .setHttpCode(status.getStatusCode())
                    .setContext(LOGBOOK).setState("code_vitam").setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        } catch (final IllegalArgumentException exc) {
            LOGGER.error(exc);
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(LOGBOOK).setState("code_vitam").setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        }
        return Response.status(Response.Status.CREATED).build();

    }

    @POST
    @Path("/operations/{id_op}/bulklifecycles/unit/temporary")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateUnitLifeCyclesUnitTemporaryByOperation(@PathParam("id_op") String operationId,
                                                                 List<LogbookLifeCycleParametersBulk> logbookLifeCycleParametersBulk) {

        logbookLifeCycle.updateLogbookLifeCycleBulk(LogbookCollections.LIFECYCLE_UNIT_IN_PROCESS, logbookLifeCycleParametersBulk);

        return Response.status(Response.Status.CREATED).build();
    }
    @POST
    @Path("/operations/{id_op}/bulklifecycles/got/temporary")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateUnitLifeCyclesGOTTemporaryByOperation(@PathParam("id_op") String operationId,
                                                                List<LogbookLifeCycleParametersBulk> logbookLifeCycleParametersBulk) {

        logbookLifeCycle.updateLogbookLifeCycleBulk(LogbookCollections.LIFECYCLE_OBJECTGROUP_IN_PROCESS, logbookLifeCycleParametersBulk);

        return Response.status(Response.Status.CREATED).build();
    }
    @POST
    @Path("/operations/{id_op}/bulklifecycles/unit")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateUnitLifeCyclesUnitByOperation(@PathParam("id_op") String operationId,
                                                                 List<LogbookLifeCycleParametersBulk> logbookLifeCycleParametersBulk) {

        logbookLifeCycle.updateLogbookLifeCycleBulk(LogbookCollections.LIFECYCLE_UNIT, logbookLifeCycleParametersBulk);

        return Response.status(Response.Status.CREATED).build();
    }
    @POST
    @Path("/operations/{id_op}/bulklifecycles/got")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateUnitLifeCyclesGOTByOperation(@PathParam("id_op") String operationId,
                                                                List<LogbookLifeCycleParametersBulk> logbookLifeCycleParametersBulk) {

        logbookLifeCycle.updateLogbookLifeCycleBulk(LogbookCollections.LIFECYCLE_OBJECTGROUP, logbookLifeCycleParametersBulk);

        return Response.status(Response.Status.CREATED).build();
    }
        /**
         * Update Unit Life Cycle
         *
         * @param operationId the operation id
         * @param unitLcId the life cycle id
         * @param evtStatus the operation type : Update or Commit the lifeCycle
         * @param parameters the json serialized as a LogbookLifeCycleUnitParameters.
         * @return the response with a specific HTTP status
         */
    @PUT
    @Path("/operations/{id_op}/unitlifecycles/{id_lc}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateUnitLifeCyclesUnitTemporaryByOperation(@PathParam("id_op") String operationId,
                                                                 @PathParam("id_lc") String unitLcId, @HeaderParam(GlobalDataRest.X_EVENT_STATUS) String evtStatus,
                                                                 LogbookLifeCycleUnitParameters parameters) {
        Status status;
        try {

            // Decide which operation to execute : Update a Temporary LifeCycle or Commit the unit lifeCycle based on
            // X-EVENT-STATUS header value
            // Note : By default, the operation is an update process if the header wasn't given
            LifeCycleStatusCode lifeCycleStatus = getUpdateOrCommitLifeCycleStatusCode(evtStatus);

            // It is an update on a temporary lifeCycle
            if (LifeCycleStatusCode.LIFE_CYCLE_IN_PROCESS.equals(lifeCycleStatus)) {
                try {
                    // check null or empty parameters
                    ParameterHelper.checkNullOrEmptyParameters(parameters);
                } catch (final IllegalArgumentException e) {
                    LOGGER.error("unit lifecycles is incorrect", e);
                    status = Status.BAD_REQUEST;
                    return Response.status(status)
                        .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                            .setContext(LOGBOOK).setState("code_vitam").setMessage(status.getReasonPhrase())
                            .setDescription(e.getMessage()))
                        .build();
                }
                logbookLifeCycle.updateUnit(operationId, unitLcId, parameters);
            } else {


                if (null == parameters || parameters.getMapParameters().isEmpty()) {
                    // Commit the given Unit lifeCycle
                    logbookLifeCycle.commitUnit(operationId, unitLcId);
                } else {
                    // Update the already committed lifeCycle
                    logbookLifeCycle.updateUnit(operationId, unitLcId, parameters, true);
                }

            }
        } catch (final LogbookNotFoundException exc) {
            LOGGER.debug(exc);
            status = Status.NOT_FOUND;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(LOGBOOK).setState("code_vitam").setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        } catch (final LogbookDatabaseException exc) {
            LOGGER.error(exc);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(LOGBOOK).setState("code_vitam").setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        } catch (final IllegalArgumentException exc) {
            LOGGER.error(exc);
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(LOGBOOK).setState("code_vitam").setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        } catch (LogbookAlreadyExistsException exc) {
            LOGGER.error(exc);
            status = Status.CONFLICT;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(LOGBOOK).setState("code_vitam").setMessage(status.getReasonPhrase())
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
            LOGGER.debug(exc);
            status = Status.NOT_FOUND;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(LOGBOOK).setState("code_vitam").setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        } catch (final LogbookDatabaseException exc) {
            LOGGER.error(exc);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(LOGBOOK).setState("code_vitam").setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        } catch (final IllegalArgumentException exc) {
            LOGGER.error(exc);
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(LOGBOOK).setState("code_vitam").setMessage(status.getReasonPhrase())
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
    @Deprecated
    @PUT
    @Path("/operations/{id_op}/unitlifecycles/{id_lc}/commit")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response commitUnitLifeCyclesByOperation(@PathParam("id_op") String operationId,
        @PathParam("id_lc") String unitLcId) {
        LOGGER.debug("UnitLifeCycle commited: " + unitLcId);
        try {
            logbookLifeCycle.commitUnit(operationId, unitLcId);
        } catch (LogbookDatabaseException e) {
            LOGGER.error(e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } catch (LogbookNotFoundException e) {
            LOGGER.debug(e);
            return Response.status(Response.Status.NOT_FOUND).build();
        } catch (LogbookAlreadyExistsException e) {
            LOGGER.error(e);
            return Response.status(Response.Status.CONFLICT).build();
        }

        return Response.status(Response.Status.OK).build();
    }

    /**
     * Lifecycle Unit Bulk Create
     *
     * @param idOp the operation id
     * @param array Lifecycle Unit Logbooks as ArrayNode
     * @return Response of CREATED
     */
    @POST
    @Path("/operations/{id_op}/unitlifecycles")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response bulkCreateUnit(@PathParam("id_op") String idOp, String array) {
        // array as a bulk LogbookLifeCycleParameters
        try {
            ParametersChecker.checkParameter("Logbook parameters", array);
        } catch (final IllegalArgumentException e) {
            LOGGER.error("Lifecycles is incorrect", e);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        try {
            final LogbookLifeCycleUnitParameters[] arrayLifecycles =
                JsonHandler.getFromString(array, LogbookLifeCycleUnitParameters[].class);
            logbookLifeCycle.createBulkLogbookLifecycle(idOp, arrayLifecycles);
        } catch (final LogbookDatabaseException e) {
            LOGGER.error(e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } catch (final LogbookAlreadyExistsException e) {
            LOGGER.error(e);
            return Response.status(Response.Status.CONFLICT).build();
        } catch (InvalidParseOperationException | IllegalArgumentException e) {
            LOGGER.error(e);
            final Status status = Status.PRECONDITION_FAILED;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(LOGBOOK)
                    .setState("code_vitam")
                    .setMessage(status.getReasonPhrase())
                    .setDescription(e.getMessage()))
                .build();
        }
        return Response.status(Response.Status.CREATED).build();
    }

    /**
     * Lifecycle Unit Bulk Create
     *
     * @param idOp the operation id
     * @param logbookLifeCycleModels Lifecycle Unit Logbooks as ArrayNode
     * @return Response of CREATED
     */
    @PUT
    @Path("/operations/{id_op}/lifecycles/objectgroup/bulk")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createLifeCycleObjectGroupBulk(@PathParam("id_op") String idOp,
        List<LogbookLifeCycleObjectGroupModel> logbookLifeCycleModels) {
        // array as a bulk LogbookLifeCycleParameters
        ParametersChecker.checkParameter("Logbook parameters", logbookLifeCycleModels);
        try {
            logbookLifeCycle.bulk(LogbookCollections.LIFECYCLE_OBJECTGROUP_IN_PROCESS, idOp, logbookLifeCycleModels);
        } catch (final IllegalArgumentException e) {
            LOGGER.error("Lifecycles is incorrect", e);
            return Response.status(Response.Status.BAD_REQUEST).build();
        } catch (DatabaseException e) {
            LOGGER.error(e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        return Response.status(Response.Status.CREATED).build();
    }

    /**
     * Lifecycle Unit Bulk Create
     *
     * @param idOp the operation id
     * @param logbookLifeCycleModels Lifecycle Unit Logbooks as ArrayNode
     * @return Response of CREATED
     */
    @PUT
    @Path("/operations/{id_op}/lifecycles/unit/bulk")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createLifeCycleUnitBulk(@PathParam("id_op") String idOp,
        List<LogbookLifeCycleUnitModel> logbookLifeCycleModels) {
        // array as a bulk LogbookLifeCycleParameters
        ParametersChecker.checkParameter("Logbook parameters", logbookLifeCycleModels);
        try {
            logbookLifeCycle.bulk(LogbookCollections.LIFECYCLE_UNIT_IN_PROCESS, idOp, logbookLifeCycleModels);
        } catch (final IllegalArgumentException e) {
            LOGGER.error("Lifecycles is incorrect", e);
            return Response.status(Response.Status.BAD_REQUEST).build();
        } catch (DatabaseException e) {
            LOGGER.error(e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        return Response.status(Response.Status.CREATED).build();
    }

    /**
     * Update Lifecycle With Bulk Mode
     *
     * @param idOp the operation id
     * @param arrayNodeLifecycle as ArrayNode of operations to add to existing Lifecycle Logbook entry
     * @return Response with a status of OK if updated
     */
    @PUT
    @Path("/operations/{id_op}/unitlifecycles")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    // Note: here let String since we need JsonHandler to parser the object
    public Response updateBulkUnit(@PathParam("id_op") String idOp, String arrayNodeLifecycle) {
        try {
            final LogbookLifeCycleUnitParameters[] arrayLifecycles =
                JsonHandler.getFromString(arrayNodeLifecycle, LogbookLifeCycleUnitParameters[].class);
            logbookLifeCycle.updateBulkLogbookLifecycle(idOp, arrayLifecycles);
        } catch (final LogbookNotFoundException exc) {
            LOGGER.debug(exc);
            return Response.status(Response.Status.NOT_FOUND).build();
        } catch (final LogbookDatabaseException exc) {
            LOGGER.error(exc);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } catch (final IllegalArgumentException | InvalidParseOperationException exc) {
            LOGGER.error(exc);
            return Response.status(Response.Status.BAD_REQUEST).build();
        } catch (LogbookAlreadyExistsException exc) {
            LOGGER.error(exc);
            return Response.status(Response.Status.CONFLICT).build();
        }
        return Response.status(Response.Status.OK).build();
    }

    /**
     * gets the unit life cycle based on its id
     *
     * @param unitLifeCycleId the unit life cycle id
     * @param evtStatus the lifeCycle Status that we are looking for : COMMITTED or IN_PROCESS
     * @param queryDsl the query to get unit lfc
     * @return the unit life cycle
     */
    @GET
    @Path("/unitlifecycles/{id_lc}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUnitLifeCycleById(@PathParam("id_lc") String unitLifeCycleId,
        @HeaderParam(GlobalDataRest.X_EVENT_STATUS) String evtStatus, JsonNode queryDsl) {
        Status status;
        try {
            LifeCycleStatusCode lifeCycleStatusCode = getSelectLifeCycleStatusCode(evtStatus);
            final SelectParserSingle parser = new SelectParserSingle(new LogbookVarNameAdapter());
            if (queryDsl != null) {
                parser.parse(queryDsl);
            }
            Select select = parser.getRequest();
            select.setQuery(QueryHelper.eq(LogbookMongoDbName.objectIdentifier.getDbname(), unitLifeCycleId));
            final LogbookLifeCycle result =
                logbookLifeCycle.getUnitById(select.getFinalSelect(), fromLifeCycleStatusToUnitCollection(lifeCycleStatusCode));

            return Response.status(Status.OK)
                .entity(new RequestResponseOK<LogbookLifeCycle>(queryDsl)
                    .addResult(result)
                    .setHttpCode(Status.OK.getStatusCode()))
                .build();
        } catch (final LogbookNotFoundException exc) {
            LOGGER.debug(exc);
            return Response.status(Status.NOT_FOUND)
                .entity(new RequestResponseOK()
                    .addResult(JsonHandler.createArrayNode())
                    .setHits(0, 0, 1)
                    .setHttpCode(Status.NOT_FOUND.getStatusCode()))
                .build();
        } catch (final LogbookException exc) {
            LOGGER.error(exc);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(LOGBOOK)
                    .setState("code_vitam")
                    .setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        } catch (final IllegalArgumentException | InvalidParseOperationException | InvalidCreateOperationException exc) {
            LOGGER.error(exc);
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(LOGBOOK)
                    .setState("code_vitam")
                    .setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        }
    }

    /**
     * Gets the unit life cycle status based on its id
     *
     * @param unitLifeCycleId the unit life cycle id
     * @return the unit life cycle status : Committed or In process
     */
    @HEAD
    @Path("/unitlifecycles/{id_lc}")
    public Response getUnitLifeCycleStatus(@PathParam("id_lc") String unitLifeCycleId) {
        Status status;
        try {
            LifeCycleStatusCode lifeCycleStatusCode = logbookLifeCycle.getUnitLifeCycleStatus(unitLifeCycleId);
            if (lifeCycleStatusCode != null) {
                // Build the response
                return Response.status(Status.OK)
                    .header(GlobalDataRest.X_EVENT_STATUS, lifeCycleStatusCode.toString())
                    .build();
            } else {
                throw new LogbookNotFoundException(
                    String.format("No lifeCycle found for the given id (%s).", unitLifeCycleId));
            }


        } catch (final LogbookNotFoundException exc) {
            LOGGER.debug(exc);
            return Response.status(Status.NOT_FOUND)
                .entity(new RequestResponseOK()
                    .addResult(JsonHandler.createArrayNode())
                    .setHits(0, 0, 1)
                    .setHttpCode(Status.NOT_FOUND.getStatusCode()))
                .build();
        } catch (final LogbookDatabaseException exc) {
            LOGGER.error(exc);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(LOGBOOK)
                    .setState("code_vitam")
                    .setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        }
    }

    /**
     * Gets a list of unit lifeCycles using a queryDsl
     *
     * @param queryDsl a DSL query
     * @param evtStatus the lifeCycle Status that we are looking for : COMMITTED or IN_PROCESS
     * @return a list of unit lifeCycles
     */
    @GET
    @Path("/unitlifecycles")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getUnitLifeCycle(JsonNode queryDsl, @HeaderParam(GlobalDataRest.X_EVENT_STATUS) String evtStatus)
        throws VitamDBException {
        Status status;
        try {
            LifeCycleStatusCode lifeCycleStatusCode = getSelectLifeCycleStatusCode(evtStatus);
            final List<LogbookLifeCycle> result =
                logbookLifeCycle.selectUnit(queryDsl, true, fromLifeCycleStatusToUnitCollection(lifeCycleStatusCode));

            return Response.status(Status.OK)
                .entity(new RequestResponseOK<LogbookLifeCycle>(queryDsl)
                    .addAllResults(result)
                    .setHttpCode(Status.OK.getStatusCode()))
                .build();

        } catch (final LogbookNotFoundException exc) {
            LOGGER.debug(exc);
            return Response.status(Status.NOT_FOUND)
                .entity(new RequestResponseOK()
                    .addResult(JsonHandler.createArrayNode())
                    .setHits(0, 0, 1)
                    .setHttpCode(Status.NOT_FOUND.getStatusCode()))
                .build();
        } catch (final LogbookException | InvalidParseOperationException exc) {
            LOGGER.error(exc);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(LOGBOOK)
                    .setState("code_vitam")
                    .setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        } catch (final IllegalArgumentException exc) {
            LOGGER.error(exc);
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(LOGBOOK)
                    .setState("code_vitam")
                    .setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        }
    }

    /**
     * Gets a list of raw unit life cycles by request
     *
     * @param selectionJsonNode the request
     * @return a list of unit lifeCycles
     */
    @GET
    @Path("/raw/unitlifecycles/bylastpersisteddate")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getRawUnitLifecyclesByLastPersistedDate(JsonNode selectionJsonNode) {
        Status status;
        try {

            RawLifecycleByLastPersistedDateRequest request =
                JsonHandler.getFromJsonNode(selectionJsonNode, RawLifecycleByLastPersistedDateRequest.class);

            final List<JsonNode> result =
                logbookLifeCycle
                    .getRawUnitLifecyclesByLastPersistedDate(request.getStartDate(), request.getEndDate(),
                        request.getLimit());

            return Response.status(Status.OK)
                .entity(new RequestResponseOK<JsonNode>()
                    .addAllResults(result)
                    .setHttpCode(Status.OK.getStatusCode()))
                .build();

        } catch (final InvalidParseOperationException exc) {
            LOGGER.error(exc);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(LOGBOOK)
                    .setState("code_vitam")
                    .setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        } catch (final IllegalArgumentException exc) {
            LOGGER.error(exc);
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(LOGBOOK)
                    .setState("code_vitam")
                    .setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        }
    }

    /**
     * Gets a list of raw unit lifeCycles by id
     *
     * @param id the id to retrieve
     * @return a the unit lifecycle in raw format
     */
    @GET
    @Path("/raw/unitlifecycles/byid/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getRawUnitLifeCycleById(@PathParam("id") String id) {
        Status status;
        try {

            final JsonNode result =
                logbookLifeCycle.getRawUnitLifeCycleById(id);

            return Response.status(Status.OK)
                .entity(new RequestResponseOK<JsonNode>()
                    .addResult(result)
                    .setHttpCode(Status.OK.getStatusCode()))
                .build();

        } catch (final LogbookNotFoundException exc) {
            LOGGER.debug(exc);
            return Response.status(Status.NOT_FOUND)
                .entity(new RequestResponseOK()
                    .addResult(JsonHandler.createArrayNode())
                    .setHits(0, 0, 1)
                    .setHttpCode(Status.NOT_FOUND.getStatusCode()))
                .build();
        } catch (final InvalidParseOperationException exc) {
            LOGGER.error(exc);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(LOGBOOK)
                    .setState("code_vitam")
                    .setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        } catch (final IllegalArgumentException exc) {
            LOGGER.error(exc);
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(LOGBOOK)
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
     * @param evtStatus the evenement status (commited / not_commited)
     * @param query as JsonNode
     * @return the response with a specific HTTP status
     */
    @GET
    @Path("/operations/{id_op}/objectgrouplifecycles")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getObjectGroupLifeCyclesByOperation(@PathParam("id_op") String operationId,
        @HeaderParam(GlobalDataRest.X_EVENT_STATUS) String evtStatus, JsonNode query) {
        Status status;
        List<JsonNode> objects = new ArrayList<>();
        try {
            final Select newQuery = addConditionToQuery(operationId, query);
            try (MongoCursor<LogbookLifeCycleObjectGroup> ogCursor =
                mongoDbAccess.getLogbookLifeCycleObjectGroupsFull(
                    fromLifeCycleStatusToObjectGroupCollection(getSelectLifeCycleStatusCode(evtStatus)),
                    newQuery)) {
                while (ogCursor.hasNext()) {
                    objects.add(JsonHandler.toJsonNode(ogCursor.next()));
                }
                final ResponseBuilder builder =
                    Response.status(Status.OK)
                        .entity(new RequestResponseOK<JsonNode>().addAllResults(objects));
                return builder.build();
            }
        } catch (final LogbookDatabaseException | InvalidParseOperationException |
            InvalidCreateOperationException exc) {
            LOGGER.error(exc);
            status = Status.INTERNAL_SERVER_ERROR;
            final ResponseBuilder builder = Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(LOGBOOK).setState("code_vitam").setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()));
            return builder.build();
        } catch (final IllegalArgumentException exc) {
            LOGGER.error(exc);
            status = Status.BAD_REQUEST;
            final ResponseBuilder builder = Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(LOGBOOK).setState("code_vitam").setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()));
            return builder.build();
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
                        .setContext(LOGBOOK).setState("code_vitam").setMessage(status.getReasonPhrase())
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
                    .setContext(LOGBOOK).setState("code_vitam").setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        } catch (final LogbookDatabaseException exc) {
            LOGGER.error(exc);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(LOGBOOK).setState("code_vitam").setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        } catch (final IllegalArgumentException exc) {
            LOGGER.error(exc);
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(LOGBOOK).setState("code_vitam").setMessage(status.getReasonPhrase())
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
     * @param evtStatus the operation type : Update or Commit the lifeCycle
     * @param parameters the json serialized as a LogbookLifeCycleObjectGroupParameters.
     * @return the response with a specific HTTP status
     */
    @PUT
    @Path("/operations/{id_op}/objectgrouplifecycles/{id_lc}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateObjectGroupLifeCyclesByOperation(@PathParam("id_op") String operationId,
        @PathParam("id_lc") String objGrpId,
        @HeaderParam(GlobalDataRest.X_EVENT_STATUS) String evtStatus,
        LogbookLifeCycleObjectGroupParameters parameters) {
        Status status;
        try {

            // Decide which operation to execute : Update a Temporary LifeCycle or Commit the objectGroup lifeCycle
            // based on X-EVENT-STATUS header value
            // Note : By default, the operation is an update process if the header wasn't given
            LifeCycleStatusCode lifeCycleStatus = getUpdateOrCommitLifeCycleStatusCode(evtStatus);

            // It is an update on a temporary lifeCycle
            if (LifeCycleStatusCode.LIFE_CYCLE_IN_PROCESS.equals(lifeCycleStatus)) {
                try {
                    // check null or empty parameters
                    ParameterHelper.checkNullOrEmptyParameters(parameters);
                } catch (final IllegalArgumentException e) {
                    LOGGER.error("objectgrouplifecycles is incorrect", e);
                    status = Status.BAD_REQUEST;
                    return Response.status(status)
                        .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                            .setContext(LOGBOOK).setState("code_vitam").setMessage(status.getReasonPhrase())
                            .setDescription(e.getMessage()))
                        .build();
                }
                /**
                 * update object group logbook Life cycle
                 */
                logbookLifeCycle.updateObjectGroup(operationId, objGrpId, parameters);
            } else {
                if (null == parameters || parameters.getMapParameters().isEmpty()) {
                    // Commit the given objectGroup lifeCycle
                    logbookLifeCycle.commitObjectGroup(operationId, objGrpId);
                } else {
                    // Update the already committed lifeCycle
                    logbookLifeCycle.updateObjectGroup(operationId, objGrpId, parameters, true);
                }
            }
        } catch (final LogbookNotFoundException exc) {
            LOGGER.debug(exc);
            status = Status.NOT_FOUND;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(LOGBOOK).setState("code_vitam").setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        } catch (final LogbookDatabaseException exc) {
            LOGGER.error(exc);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(LOGBOOK).setState("code_vitam").setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        } catch (final IllegalArgumentException exc) {
            LOGGER.error(exc);
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(LOGBOOK).setState("code_vitam").setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        } catch (LogbookAlreadyExistsException exc) {
            LOGGER.error(exc);
            status = Status.CONFLICT;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(LOGBOOK).setState("code_vitam").setMessage(status.getReasonPhrase())
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
            LOGGER.debug(exc);
            status = Status.NOT_FOUND;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(LOGBOOK).setState("code_vitam").setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        } catch (final LogbookDatabaseException exc) {
            LOGGER.error(exc);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(LOGBOOK).setState("code_vitam").setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        } catch (final IllegalArgumentException exc) {
            LOGGER.error(exc);
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(LOGBOOK).setState("code_vitam").setMessage(status.getReasonPhrase())
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
     * @return the response with a specific HTTP status
     */
    @Deprecated
    @PUT
    @Path("/operations/{id_op}/objectgrouplifecycles/{id_lc}/commit")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response commitObjectGroupLifeCyclesByOperation(@PathParam("id_op") String operationId,
        @PathParam("id_lc") String objGrpId) {
        LOGGER.debug("ObjectGroup commited: " + objGrpId);
        try {
            logbookLifeCycle.commitObjectGroup(operationId, objGrpId);
        } catch (LogbookDatabaseException e) {
            LOGGER.error(e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } catch (LogbookNotFoundException e) {
            LOGGER.debug(e);
            return Response.status(Response.Status.NOT_FOUND).build();
        } catch (LogbookAlreadyExistsException e) {
            LOGGER.error(e);
            return Response.status(Response.Status.CONFLICT).build();
        }

        return Response.status(Response.Status.OK).build();
    }

    /**
     * Lifecycle ObjectGroup Bulk Create
     *
     * @param idOp the operation id
     * @param array Lifecycle ObjectGroup Logbooks as ArrayNode
     * @return Response of CREATED
     */
    @POST
    @Path("/operations/{id_op}/objectgrouplifecycles")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response bulkCreateObjectGroup(@PathParam("id_op") String idOp, String array) {
        // array as a bulk LogbookLifeCycleParameters
        try {
            ParametersChecker.checkParameter("Logbook parameters", array);
        } catch (final IllegalArgumentException e) {
            LOGGER.error("Lifecycles is incorrect", e);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        try {
            final LogbookLifeCycleObjectGroupParameters[] arrayLifecycles =
                JsonHandler.getFromString(array, LogbookLifeCycleObjectGroupParameters[].class);
            logbookLifeCycle.createBulkLogbookLifecycle(idOp, arrayLifecycles);
        } catch (final LogbookDatabaseException e) {
            LOGGER.error(e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } catch (final LogbookAlreadyExistsException e) {
            LOGGER.error(e);
            return Response.status(Response.Status.CONFLICT).build();
        } catch (InvalidParseOperationException | IllegalArgumentException e) {
            LOGGER.error(e);
            final Status status = Status.PRECONDITION_FAILED;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(LOGBOOK)
                    .setState("code_vitam")
                    .setMessage(status.getReasonPhrase())
                    .setDescription(e.getMessage()))
                .build();
        }
        return Response.status(Response.Status.CREATED).build();
    }

    /**
     * Update Lifecycle ObjectGroup With Bulk Mode
     *
     * @param idOp the operation id
     * @param arrayNodeLifecycle as ArrayNode of operations to add to existing Lifecycle Logbook entry
     * @return Response with a status of OK if updated
     */
    @PUT
    @Path("/operations/{id_op}/objectgrouplifecycles")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    // Note: here let String since we need JsonHandler to parser the object
    public Response updateBulkObjectGroup(@PathParam("id_op") String idOp, String arrayNodeLifecycle) {
        try {
            final LogbookLifeCycleObjectGroupParameters[] arrayLifecycles =
                JsonHandler.getFromString(arrayNodeLifecycle, LogbookLifeCycleObjectGroupParameters[].class);
            logbookLifeCycle.updateBulkLogbookLifecycle(idOp, arrayLifecycles);
        } catch (final LogbookNotFoundException exc) {
            LOGGER.debug(exc);
            return Response.status(Response.Status.NOT_FOUND).build();
        } catch (final LogbookDatabaseException exc) {
            LOGGER.error(exc);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } catch (final IllegalArgumentException | InvalidParseOperationException exc) {
            LOGGER.error(exc);
            return Response.status(Response.Status.BAD_REQUEST).build();
        } catch (LogbookAlreadyExistsException exc) {
            LOGGER.error(exc);
            return Response.status(Response.Status.CONFLICT).build();
        }
        return Response.status(Response.Status.OK).build();
    }

    /**
     * Gets the object group life cycle based on its id and using the passed DSL query
     *
     * @param objectGroupLifeCycleId the object group life cycle id
     * @param evtStatus the lifeCycle Status that we are looking for : COMMITTED or IN_PROCESS
     * @param queryDsl the DSL query
     * @return a Response that contains the object group life cycle
     */
    @GET
    @Path("/objectgrouplifecycles/{id_lc}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getObjectGroupLifeCycleById(@PathParam("id_lc") String objectGroupLifeCycleId,
        @HeaderParam(GlobalDataRest.X_EVENT_STATUS) String evtStatus, JsonNode queryDsl)
        throws VitamDBException {
        Status status;
        try {
            LifeCycleStatusCode requiredLifeCycleStatus = getSelectLifeCycleStatusCode(evtStatus);

            List<LogbookLifeCycle> result = new ArrayList<>();
            result = logbookLifeCycle.selectObjectGroup(queryDsl, false,
                fromLifeCycleStatusToObjectGroupCollection(requiredLifeCycleStatus));

            return Response.status(Status.OK)
                .entity(new RequestResponseOK<LogbookLifeCycle>(queryDsl)
                    .addResult(result.iterator().next())
                    .setHttpCode(Status.OK.getStatusCode()))
                .build();
        } catch (final LogbookNotFoundException exc) {
            LOGGER.debug(exc);
            return Response.status(Status.NOT_FOUND)
                .entity(new RequestResponseOK(queryDsl)
                    .setHttpCode(Status.NOT_FOUND.getStatusCode()))
                .build();
        } catch (final LogbookException | InvalidParseOperationException exc) {
            LOGGER.error(exc);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(LOGBOOK)
                    .setState("code_vitam")
                    .setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        } catch (final IllegalArgumentException exc) {
            LOGGER.error(exc);
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(LOGBOOK)
                    .setState("code_vitam")
                    .setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        }
    }


    /**
     * Gets the object group life cycles based on the passed DSL query
     *
     * @param evtStatus the lifeCycle Status that we are looking for : COMMITTED or IN_PROCESS
     * @param queryDsl the DSL query
     * @return a Response that contains the object group life cycle
     */
    @GET
    @Path("/objectgrouplifecycles")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getObjectGroupLifeCycle(@HeaderParam(GlobalDataRest.X_EVENT_STATUS) String evtStatus,
        JsonNode queryDsl)
        throws VitamDBException {
        Status status;
        try {
            LifeCycleStatusCode requiredLifeCycleStatus = getSelectLifeCycleStatusCode(evtStatus);

            final List<LogbookLifeCycle> result;
            result = logbookLifeCycle.selectObjectGroup(queryDsl, false,
                fromLifeCycleStatusToObjectGroupCollection(requiredLifeCycleStatus));
            return Response.status(Status.OK)
                .entity(new RequestResponseOK<LogbookLifeCycle>(queryDsl)
                    .addAllResults(result)
                    .setHttpCode(Status.OK.getStatusCode()))
                .build();
        } catch (final LogbookNotFoundException exc) {
            LOGGER.debug(exc);
            return Response.status(Status.NOT_FOUND)
                .entity(new RequestResponseOK(queryDsl)
                    .setHttpCode(Status.NOT_FOUND.getStatusCode()))
                .build();
        } catch (final LogbookException | InvalidParseOperationException exc) {
            LOGGER.error(exc);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(LOGBOOK)
                    .setState("code_vitam")
                    .setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        } catch (final IllegalArgumentException exc) {
            LOGGER.error(exc);
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(LOGBOOK)
                    .setState("code_vitam")
                    .setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        }
    }

    /**
     * Gets the objectGroup life cycle status based on its id
     *
     * @param objectGroupLifeCycleId the object group cycle id
     * @return the object group cycle status : Committed or In process
     */
    @HEAD
    @Path("/objectgrouplifecycles/{id_lc}")
    public Response getObjectGroupLifeCycleStatus(@PathParam("id_lc") String objectGroupLifeCycleId) {
        Status status;
        try {
            LifeCycleStatusCode lifeCycleStatusCode =
                logbookLifeCycle.getObjectGroupLifeCycleStatus(objectGroupLifeCycleId);
            if (lifeCycleStatusCode != null) {
                // Build the response
                return Response.status(Status.OK)
                    .header(GlobalDataRest.X_EVENT_STATUS, lifeCycleStatusCode.toString())
                    .build();
            } else {
                throw new LogbookNotFoundException(
                    String.format("No lifeCycle found for the given id %s.", objectGroupLifeCycleId));
            }
        } catch (final LogbookNotFoundException exc) {
            LOGGER.debug(exc);
            return Response.status(Status.NOT_FOUND)
                .entity(new RequestResponseOK()
                    .addResult(JsonHandler.createArrayNode())
                    .setHits(0, 0, 1)
                    .setHttpCode(Status.NOT_FOUND.getStatusCode()))
                .build();
        } catch (final LogbookDatabaseException exc) {
            LOGGER.error(exc);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(LOGBOOK)
                    .setState("code_vitam")
                    .setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        }
    }

    /**
     * Gets a list of raw unit life cycles by request
     *
     * @param selectionJsonNode the request
     * @return a list of unit lifeCycles
     */
    @GET
    @Path("/raw/objectgrouplifecycles/bylastpersisteddate")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getRawObjectGroupLifecyclesByLastPersistedDate(JsonNode selectionJsonNode) {
        Status status;
        try {

            RawLifecycleByLastPersistedDateRequest request =
                JsonHandler.getFromJsonNode(selectionJsonNode, RawLifecycleByLastPersistedDateRequest.class);

            final List<JsonNode> result =
                logbookLifeCycle
                    .getRawObjectGroupLifecyclesByLastPersistedDate(request.getStartDate(), request.getEndDate(),
                        request.getLimit());

            return Response.status(Status.OK)
                .entity(new RequestResponseOK<JsonNode>()
                    .addAllResults(result)
                    .setHttpCode(Status.OK.getStatusCode()))
                .build();

        } catch (final InvalidParseOperationException exc) {
            LOGGER.error(exc);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(LOGBOOK)
                    .setState("code_vitam")
                    .setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        } catch (final IllegalArgumentException exc) {
            LOGGER.error(exc);
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(LOGBOOK)
                    .setState("code_vitam")
                    .setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        }
    }

    /**
     * Gets a list of raw object group lifeCycles by id
     *
     * @param id the id to retrieve
     * @return a the object group lifecycle in raw format
     */
    @GET
    @Path("/raw/objectgrouplifecycles/byid/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getRawObjectGroupLifeCycleById(@PathParam("id") String id) {
        Status status;
        try {

            final JsonNode result =
                logbookLifeCycle.getRawObjectGroupLifeCycleById(id);

            return Response.status(Status.OK)
                .entity(new RequestResponseOK<JsonNode>()
                    .addResult(result)
                    .setHttpCode(Status.OK.getStatusCode()))
                .build();

        } catch (final LogbookNotFoundException exc) {
            LOGGER.debug(exc);
            return Response.status(Status.NOT_FOUND)
                .entity(new RequestResponseOK()
                    .addResult(JsonHandler.createArrayNode())
                    .setHits(0, 0, 1)
                    .setHttpCode(Status.NOT_FOUND.getStatusCode()))
                .build();
        } catch (final InvalidParseOperationException exc) {
            LOGGER.error(exc);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(LOGBOOK)
                    .setState("code_vitam")
                    .setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        } catch (final IllegalArgumentException exc) {
            LOGGER.error(exc);
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(LOGBOOK)
                    .setState("code_vitam")
                    .setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        }
    }

    /***** LIFE CYCLES OBJECT GROUP - END *****/

    /**
     * Deletes all temporary Unit lifeCycles created during a given operation
     *
     * @param operationId the operation id
     * @return a Response that contains the result of deletion operation
     */
    @DELETE
    @Path("/operations/{id_op}/unitlifecycles")
    public Response rollBackUnitLifeCyclesByOperation(@PathParam("id_op") String operationId) {
        Status status;
        try {
            logbookLifeCycle.rollBackUnitsByOperation(operationId);
        } catch (final LogbookNotFoundException exc) {
            LOGGER.debug(exc);
            status = Status.NOT_FOUND;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(LOGBOOK).setState("code_vitam").setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        } catch (final LogbookDatabaseException exc) {
            LOGGER.error(exc);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(LOGBOOK).setState("code_vitam").setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        } catch (final IllegalArgumentException exc) {
            LOGGER.error(exc);
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(LOGBOOK).setState("code_vitam").setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        }
        return Response.status(Response.Status.OK).build();
    }

    /**
     * Deletes all temporary ObjectGroup lifeCycles created during a given operation
     *
     * @param operationId the operation id
     * @return a Response that contains the result of deletion operation
     */
    @DELETE
    @Path("/operations/{id_op}/objectgrouplifecycles")
    public Response rollBackObjectGroupLifeCyclesByOperation(@PathParam("id_op") String operationId) {
        Status status;
        try {
            logbookLifeCycle.rollBackObjectGroupsByOperation(operationId);
        } catch (final LogbookNotFoundException exc) {
            LOGGER.debug(exc);
            status = Status.NOT_FOUND;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(LOGBOOK).setState("code_vitam").setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        } catch (final LogbookDatabaseException exc) {
            LOGGER.error(exc);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(LOGBOOK).setState("code_vitam").setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        } catch (final IllegalArgumentException exc) {
            LOGGER.error(exc);
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(LOGBOOK).setState("code_vitam").setMessage(status.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        }
        return Response.status(Response.Status.OK).build();
    }

    private LogbookCollections fromLifeCycleStatusToUnitCollection(LifeCycleStatusCode lifeCycleStatusCode) {
        switch (lifeCycleStatusCode) {
            case LIFE_CYCLE_COMMITTED:
                return LogbookCollections.LIFECYCLE_UNIT;
            case LIFE_CYCLE_IN_PROCESS:
                return LogbookCollections.LIFECYCLE_UNIT_IN_PROCESS;
            default:
                return LogbookCollections.LIFECYCLE_UNIT;
        }
    }

    private LogbookCollections fromLifeCycleStatusToObjectGroupCollection(LifeCycleStatusCode lifeCycleStatusCode) {
        switch (lifeCycleStatusCode) {
            case LIFE_CYCLE_COMMITTED:
                return LogbookCollections.LIFECYCLE_OBJECTGROUP;
            case LIFE_CYCLE_IN_PROCESS:
                return LogbookCollections.LIFECYCLE_OBJECTGROUP_IN_PROCESS;
            default:
                return LogbookCollections.LIFECYCLE_OBJECTGROUP;
        }
    }

    private LifeCycleStatusCode getUpdateOrCommitLifeCycleStatusCode(String evtStatusHeader)
        throws IllegalArgumentException {
        if (evtStatusHeader == null) {
            return LifeCycleStatusCode.LIFE_CYCLE_IN_PROCESS;
        } else {
            return LifeCycleStatusCode.valueOf(evtStatusHeader);
        }
    }

    private LifeCycleStatusCode getSelectLifeCycleStatusCode(String evtStatusHeader) throws IllegalArgumentException {
        if (evtStatusHeader == null) {
            return LifeCycleStatusCode.LIFE_CYCLE_COMMITTED;
        } else {
            return LifeCycleStatusCode.valueOf(evtStatusHeader);
        }
    }

    /**
     * Runs unit lifecycle traceability
     *
     * @param xTenantId the tenant id
     * @return the response with a specific HTTP status
     */
    @POST
    @Path("/lifecycles/units/traceability")
    @Produces(MediaType.APPLICATION_JSON)
    public Response traceabilityLfcUnit(@HeaderParam(GlobalDataRest.X_TENANT_ID) String xTenantId) {
        return traceabilityLFC(xTenantId, LfcTraceabilityType.Unit);
    }

    /**
     * Runs object group lifecycle traceability
     *
     * @param xTenantId the tenant id
     * @return the response with a specific HTTP status
     */
    @POST
    @Path("/lifecycles/objectgroups/traceability")
    @Produces(MediaType.APPLICATION_JSON)
    public Response traceabilityLfcObjectGroup(@HeaderParam(GlobalDataRest.X_TENANT_ID) String xTenantId) {
        return traceabilityLFC(xTenantId, LfcTraceabilityType.ObjectGroup);
    }

    private Response traceabilityLFC(String xTenantId,
        LfcTraceabilityType lfcTraceabilityType) {

        if (Strings.isNullOrEmpty(xTenantId)) {
            LOGGER.error(MISSING_THE_TENANT_ID_X_TENANT_ID);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        try {
            Integer tenantId = Integer.parseInt(xTenantId);
            VitamThreadUtils.getVitamSession().setTenantId(tenantId);

            final GUID guid = logbookLFCAdministration.generateSecureLogbookLFC(lfcTraceabilityType);
            final List<String> resultAsJson = new ArrayList<>();

            resultAsJson.add(guid.toString());
            return Response.status(Status.OK)
                .entity(new RequestResponseOK<String>()
                    .addAllResults(resultAsJson)
                    .setHits(1, 0, 1)
                    .setHttpCode(Status.OK.getStatusCode()))
                .build();

        } catch (VitamException e) {
            LOGGER.error("unable to generate traceability log", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(new RequestResponseOK()
                    .setHttpCode(Status.INTERNAL_SERVER_ERROR.getStatusCode()))
                .build();
        }
    }

    /**
     * Runs unit lifecycle traceability
     *
     * @param operationId the process id
     * @return the response with a specific HTTP status
     */
    @GET
    @Path("/lifecycles/traceability/check/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkLifecycleTraceabilityStatus(@PathParam("id") String operationId) {

        try {
            VitamThreadUtils.getVitamSession().setRequestId(operationId);
            LifecycleTraceabilityStatus status = logbookLFCAdministration.checkLifecycleTraceabilityStatus(operationId);

            return Response.status(Status.OK)
                .entity(new RequestResponseOK<LifecycleTraceabilityStatus>()
                    .addResult(status)
                    .setHits(1, 0, 1)
                    .setHttpCode(Status.OK.getStatusCode()))
                .build();
        } catch (final LogbookNotFoundException | WorkflowNotFoundException exc) {
            LOGGER.debug(exc);
            return Response.status(Status.NOT_FOUND)
                .entity(new VitamError(Status.NOT_FOUND.name()).setHttpCode(Status.NOT_FOUND.getStatusCode())
                    .setContext(LOGBOOK)
                    .setState("code_vitam")
                    .setMessage(Status.NOT_FOUND.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        } catch (VitamException | InvalidCreateOperationException e) {
            LOGGER.error("unable to check lifecycle traceability status", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(new RequestResponseOK()
                    .setHttpCode(Status.INTERNAL_SERVER_ERROR.getStatusCode()))
                .build();
        }
    }

    /**
     * Reindex a collection
     *
     * @param indexParameters parameters specifying what to reindex
     * @return Response
     */
    @Path("/reindex")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response reindex(IndexParameters indexParameters) {
        try {
            ParametersChecker.checkParameter("Parameters are mandatory", indexParameters);
        } catch (final IllegalArgumentException exc) {
            LOGGER.error(exc);
            return Response.status(Status.PRECONDITION_FAILED).entity(
                new VitamError(Status.PRECONDITION_FAILED.name())
                    .setHttpCode(Status.PRECONDITION_FAILED.getStatusCode())
                    .setContext(LOGBOOK)
                    .setState("code_vitam")
                    .setMessage(Status.PRECONDITION_FAILED.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        }
        IndexationResult result = logbookOperation.reindex(indexParameters);
        Response response = null;
        if (result.getIndexKO() == null || result.getIndexKO().size() == 0) {
            // No KO -> 201
            response = Response.status(Status.CREATED).entity(new RequestResponseOK()
                .setHttpCode(Status.CREATED.getStatusCode())).entity(result).build();
        } else {
            // OK and at least one KO -> 202
            if (result.getIndexOK() != null && result.getIndexOK().size() > 0) {
                Response.status(Status.ACCEPTED).entity(new RequestResponseOK()
                    .setHttpCode(Status.ACCEPTED.getStatusCode())).entity(result).build();
            } else {
                // All KO -> 500
                Status status = Status.INTERNAL_SERVER_ERROR;
                VitamError error = VitamCodeHelper.toVitamError(VitamCode.METADATA_INDEXATION_ERROR,
                    status.getReasonPhrase());
                response = Response.status(status).entity(error).entity(result).build();
            }
        }
        return response;
    }

    /**
     * Switch indexes
     *
     * @param switchIndexParameters
     * @return Response
     */
    @Path("/alias")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response changeIndexes(SwitchIndexParameters switchIndexParameters) {
        try {
            ParametersChecker.checkParameter("parameter is mandatory", switchIndexParameters);
            ParametersChecker.checkParameter("alias parameter is mandatory", switchIndexParameters.getAlias());
            ParametersChecker.checkParameter("indexName parameter is mandatory", switchIndexParameters.getIndexName());
        } catch (final IllegalArgumentException exc) {
            LOGGER.error(exc);
            return Response.status(Status.PRECONDITION_FAILED).entity(
                new VitamError(Status.PRECONDITION_FAILED.name())
                    .setHttpCode(Status.PRECONDITION_FAILED.getStatusCode())
                    .setContext(LOGBOOK)
                    .setState("code_vitam")
                    .setMessage(Status.PRECONDITION_FAILED.getReasonPhrase())
                    .setDescription(exc.getMessage()))
                .build();
        }
        try {
            logbookOperation.switchIndex(switchIndexParameters.getAlias(), switchIndexParameters.getIndexName());
            return Response.status(Status.OK).entity(new RequestResponseOK().setHttpCode(Status.OK.getStatusCode()))
                .build();
        } catch (DatabaseException exc) {
            Status status = Status.INTERNAL_SERVER_ERROR;
            VitamError error = VitamCodeHelper.toVitamError(VitamCode.METADATA_SWITCH_INDEX_ERROR,
                exc.getMessage());
            return Response.status(status).entity(error).build();
        }
    }

    @Path(AUDIT_TRACEABILITY_URI)
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response launchTraceabilityAudit(AuditLogbookOptions options) {
        try {
            logbookAuditAdministration
                .auditTraceability(options.getType(), options.getNbDay(), options.getTimesEachDay());

            return Response.status(Status.ACCEPTED).entity(new RequestResponseOK()
                .setHttpCode(Status.ACCEPTED.getStatusCode())).build();

        } catch (LogbookAuditException e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(
                new VitamError(Status.INTERNAL_SERVER_ERROR.name())
                    .setHttpCode(Status.INTERNAL_SERVER_ERROR.getStatusCode()))
                .build();
        }

    }

    @DELETE
    @Path("/objectgrouplifecycles/bulkDelete")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteObjectGroups(List<String> objectGroupIds) {
        Status status;
        JsonNode jsonNode;

        try {
            jsonNode = JsonHandler.toJsonNode(objectGroupIds);

            logbookLifeCycle.deleteLifeCycleObjectGroups(objectGroupIds);

        } catch (InvalidParseOperationException e) {
            LOGGER.error(e);
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(LOGBOOK)
                    .setState(CODE_VITAM)
                    .setMessage(status.getReasonPhrase())
                    .setDescription(e.getMessage()))
                .build();
        } catch (DatabaseException e) {
         return    Response.status(Status.INTERNAL_SERVER_ERROR).entity(
                new VitamError(Status.INTERNAL_SERVER_ERROR.name())
                    .setHttpCode(Status.INTERNAL_SERVER_ERROR.getStatusCode()))
                .build();
        }
        return Response.status(Status.OK)
            .entity(new RequestResponseOK<String>(jsonNode)
                .setHits(objectGroupIds.size(), 0, 1)
                .setHttpCode(Status.OK.getStatusCode()))
            .build();

    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/lifeCycleUnits/bulkDelete")
    public Response deleteUnits(List<String> unitsIdentifier) {
        Status status;
        JsonNode jsonNode;
        try {
            jsonNode = JsonHandler.toJsonNode(unitsIdentifier);
            logbookLifeCycle.deleteLifeCycleUnits(unitsIdentifier);

        }
        catch (DatabaseException e) {
           return Response.status(Status.INTERNAL_SERVER_ERROR).entity(
                new VitamError(Status.INTERNAL_SERVER_ERROR.name())
                    .setHttpCode(Status.INTERNAL_SERVER_ERROR.getStatusCode()))
                .build();
        }
        catch (InvalidParseOperationException e) {
            LOGGER.error(e);
            status = Status.BAD_REQUEST;
            return Response.status(status)
                .entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                    .setContext(LOGBOOK)
                    .setState(CODE_VITAM)
                    .setMessage(status.getReasonPhrase())
                    .setDescription(e.getMessage()))
                .build();
        }

        return Response.status(Status.OK)
            .entity(new RequestResponseOK<String>(jsonNode)
                .setHits(unitsIdentifier.size(), 0, 1)
                .setHttpCode(Status.OK.getStatusCode()))
            .build();
    }
}
