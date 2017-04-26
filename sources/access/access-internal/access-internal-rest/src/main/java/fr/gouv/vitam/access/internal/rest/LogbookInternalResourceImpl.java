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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.bson.Document;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Iterables;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.client.DefaultClient;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.parser.request.adapter.VarNameAdapter;
import fr.gouv.vitam.common.database.parser.request.single.SelectParserSingle;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InternalServerException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.WorkflowNotFoundException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.server.application.AsyncInputStreamHelper;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.model.TraceabilityEvent;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationsClientHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookDocument;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbName;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookOperation;
import fr.gouv.vitam.logbook.common.server.database.collections.request.LogbookVarNameAdapter;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.model.StorageCollectionType;

/**
 * AccessResourceImpl implements AccessResource
 */
@Path("/access-internal/v1")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@javax.ws.rs.ApplicationPath("webresources")
public class LogbookInternalResourceImpl {

    /**
     * 
     */
    private static final String CHECK_LOGBOOK_OP_SECURISATION = "CHECK_LOGBOOK_OP_SECURISATION";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogbookInternalResourceImpl.class);
    private static final String LOGBOOK_MODULE = "LOGBOOK";
    private static final String CODE_VITAM = "code_vitam";

    // TODO Extract values from DSLQueryHelper
    private static final String EVENT_ID_PROCESS = "evIdProc";
    private static final String OB_ID = "obId";
    private static final String DSLQUERY_TO_CHECK_TRACEABILITY_OPERATION_NOT_FOUND =
        "DSL Query to start traceability check was not found.";

    // TODO Add Enumeration of all possible WORKFLOWS
    private static final String DEFAULT_CHECK_TRACEABILITY_WORKFLOW = "DefaultCheckTraceability";
    private static final String DEFAULT_STORAGE_STRATEGY = "default";

    /**
     * Default Constructor
     */
    public LogbookInternalResourceImpl() {
        LOGGER.debug("LogbookExternalResource initialized");
    }

    /***** LOGBOOK OPERATION - START *****/
    /**
     * @param operationId the operation id
     * @param queryDsl the query
     * @return the response with a specific HTTP status
     */
    @GET
    @Path("/operations/{id_op}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOperationById(@PathParam("id_op") String operationId, JsonNode queryDsl) {
        Status status;
        try (LogbookOperationsClient client = LogbookOperationsClientFactory.getInstance().getClient()) {
            SanityChecker.checkJsonAll(queryDsl);
            SanityChecker.checkParameter(operationId);
            final SelectParserSingle parser = new SelectParserSingle(new VarNameAdapter());
            Select select = new Select();
            parser.parse(select.getFinalSelect());
            parser.addCondition(QueryHelper.eq(EVENT_ID_PROCESS, operationId));
            queryDsl = parser.getRequest().getFinalSelect();            
            final JsonNode result = client.selectOperationById(operationId, addProdServicesToQuery(queryDsl));
            return Response.status(Status.OK).entity(result).build();
        } catch (final LogbookClientException e) {
            LOGGER.error(e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status).entity(getErrorEntity(status)).build();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status).entity(getErrorEntity(status)).build();
        } catch (InvalidCreateOperationException e) {
            LOGGER.error(e);
            status = Status.BAD_REQUEST;
            return Response.status(status).entity(getErrorEntity(status)).build();
        }
    }


    /**
     * GET with request in body
     *
     * @param query DSL as String
     * @return Response contains a list of logbook operation
     */
    @GET
    @Path("/operations")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectOperation(JsonNode query) {
        Status status;
        try (LogbookOperationsClient client = LogbookOperationsClientFactory.getInstance().getClient()) {
            // Check correctness of request
            final SelectParserSingle parser = new SelectParserSingle();
            parser.parse(query);
            parser.getRequest().reset();
            final JsonNode result = client.selectOperation(addProdServicesToQuery(query));
            return Response.status(Status.OK).entity(result).build();
        } catch (final LogbookClientNotFoundException e) {
            return Response.status(Status.OK).entity(new RequestResponseOK().toJsonNode()).build();
        } catch (final LogbookClientException e) {
            LOGGER.error(e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status).entity(getErrorEntity(status)).build();
        } catch (final InvalidParseOperationException | InvalidCreateOperationException e) {
            LOGGER.error(e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status).entity(getErrorEntity(status)).build();
        }
    }


    /*****
     * LOGBOOK LIFE CYCLES
     *****/

    /**
     * gets the unit life cycle based on its id
     *
     * @param unitLifeCycleId the unit life cycle id
     * @param queryDsl the query
     * @return the unit life cycle
     */
    @GET
    @Path("/unitlifecycles/{id_lc}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUnitLifeCycleById(@PathParam("id_lc") String unitLifeCycleId, JsonNode queryDsl) {
        Status status;
        try (LogbookLifeCyclesClient client = LogbookLifeCyclesClientFactory.getInstance().getClient()) {
            SanityChecker.checkParameter(unitLifeCycleId);
            SanityChecker.checkJsonAll(queryDsl);
            final JsonNode result = client.selectUnitLifeCycleById(unitLifeCycleId, queryDsl);
            return Response.status(Status.OK).entity(result).build();
        } catch (final LogbookClientException e) {
            LOGGER.error(e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status).entity(getErrorEntity(status)).build();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status).entity(getErrorEntity(status)).build();
        }
    }

    /**
     * gets the unit life cycle based on its id
     *
     * @param queryDsl dsl query containing obId
     * @return the unit life cycle
     */
    @GET
    @Path("/unitlifecycles")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUnitLifeCycle(JsonNode queryDsl) {
        Status status;
        try (LogbookLifeCyclesClient client = LogbookLifeCyclesClientFactory.getInstance().getClient()) {
            SanityChecker.checkJsonAll(queryDsl);
            final SelectParserSingle parser = new SelectParserSingle(new VarNameAdapter());
            Select select = new Select();
            parser.parse(select.getFinalSelect());
            parser.addCondition(QueryHelper.eq(OB_ID, queryDsl.findValue(OB_ID).asText()));
            queryDsl = parser.getRequest().getFinalSelect();
            final JsonNode result = client.selectUnitLifeCycle(queryDsl);
            return Response.status(Status.OK).entity(result).build();
        } catch (final LogbookClientException e) {
            LOGGER.error(e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status).entity(getErrorEntity(status)).build();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status).entity(getErrorEntity(status)).build();
        } catch (InvalidCreateOperationException e) {
            LOGGER.error(e);
            status = Status.BAD_REQUEST;
            return Response.status(status).entity(getErrorEntity(status)).build();
        }
    }


    /**
     * gets the object group life cycle based on its id
     *
     * @param objectGroupLifeCycleId the object group life cycle id
     * @param queryDsl the query
     * @return the object group life cycle
     */
    @GET
    @Path("/objectgrouplifecycles/{id_lc}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getObjectGroupLifeCycle(@PathParam("id_lc") String objectGroupLifeCycleId, JsonNode queryDsl) {
        Status status;
        try (LogbookLifeCyclesClient client = LogbookLifeCyclesClientFactory.getInstance().getClient()) {
            SanityChecker.checkParameter(objectGroupLifeCycleId);
            SanityChecker.checkJsonAll(queryDsl);
            final JsonNode result = client.selectObjectGroupLifeCycleById(objectGroupLifeCycleId, queryDsl);
            return Response.status(Status.OK).entity(result).build();
        } catch (final LogbookClientException e) {
            LOGGER.error(e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status).entity(getErrorEntity(status)).build();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status).entity(getErrorEntity(status)).build();
        }
    }

    /***** LIFE CYCLES - END *****/


    private VitamError getErrorEntity(Status status) {
        return new VitamError(status.name()).setContext(LOGBOOK_MODULE)

            .setHttpCode(status.getStatusCode()).setState(CODE_VITAM).setMessage(status.getReasonPhrase())
            .setDescription(status.getReasonPhrase());
    }

    /**
     * Checks a traceability operation based on a given DSLQuery
     * 
     * @param query the DSLQuery used to find the traceability operation to validate
     * @return The verification report == the logbookOperation
     * @throws LogbookClientNotFoundException
     */
    @POST
    @Path("/traceability/check")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkOperationTraceability(JsonNode query) throws LogbookClientNotFoundException {
        ParametersChecker.checkParameter(DSLQUERY_TO_CHECK_TRACEABILITY_OPERATION_NOT_FOUND, query);
        
        // Get TenantID
        Integer tenantId = VitamThreadUtils.getVitamSession().getTenantId();
        Response response = null;
        try (LogbookOperationsClient logbookOperationsClient =
            LogbookOperationsClientFactory.getInstance().getClient();
            ProcessingManagementClient processManagementClient =
                ProcessingManagementClientFactory.getInstance().getClient()) {

            LogbookOperationsClientHelper helper = new LogbookOperationsClientHelper();

            // Initialize a new process
            GUID checkOperationGUID = GUIDFactory.newOperationLogbookGUID(tenantId);
            processManagementClient.initVitamProcess(LogbookTypeProcess.CHECK.toString(), checkOperationGUID.getId(),
                DEFAULT_CHECK_TRACEABILITY_WORKFLOW);

            // Create logbookOperation for check TRACEABILITY process
            createOrUpdateLogbookOperation(helper, true, checkOperationGUID, StatusCode.STARTED);
            logbookOperationsClient.bulkCreate(checkOperationGUID.getId(),
                helper.removeCreateDelegate(checkOperationGUID.getId()));

            // Run the WORKFLOW
            response =
                processManagementClient.executeCheckTraceabilityWorkFlow(checkOperationGUID.getId(), query,
                    DEFAULT_CHECK_TRACEABILITY_WORKFLOW, LogbookTypeProcess.CHECK.toString(),
                    ProcessAction.RESUME.getValue());
            
            // Add final event to logbookOperation
            createOrUpdateLogbookOperation(helper, false, checkOperationGUID,
                StatusCode.parseFromHttpStatus(response.getStatus()));
            logbookOperationsClient.bulkUpdate(checkOperationGUID.getId(),
                helper.removeUpdateDelegate(checkOperationGUID.getId()));

            // Get the created logbookOperation and return the response
            final JsonNode result = logbookOperationsClient.selectOperationById(checkOperationGUID.getId(), null);
            return Response.ok().entity(RequestResponseOK.getFromJsonNode(result)).build();

        } catch (BadRequestException | IllegalArgumentException | LogbookClientBadRequestException e) {
            LOGGER.error(e);
            final Status status = Status.BAD_REQUEST;
            return Response.status(status).entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                .setContext("logbook")
                .setState("code_vitam")
                .setMessage(status.getReasonPhrase())
                .setDescription(e.getMessage())).build();

        } catch (InternalServerException | VitamClientException | LogbookClientException |
            InvalidParseOperationException e) {
            LOGGER.error(e);
            final Status status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status).entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                .setContext("logbook")
                .setState("code_vitam")
                .setMessage(status.getReasonPhrase())
                .setDescription(e.getMessage())).build();

        } catch (WorkflowNotFoundException e) {
            LOGGER.error(e);
            final Status status = Status.NOT_FOUND;
            return Response.status(status).entity(new VitamError(status.name()).setHttpCode(status.getStatusCode())
                .setContext("logbook")
                .setState("code_vitam")
                .setMessage(status.getReasonPhrase())
                .setDescription(e.getMessage())).build();
        } finally {
            DefaultClient.staticConsumeAnyEntityAndClose(response);
        }
    }

    @GET
    @Path("/traceability/{idOperation}/content")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public void downloadTraceabilityOperation(@PathParam("idOperation") String operationId,
        @Suspended final AsyncResponse asyncResponse) {

        VitamThreadPoolExecutor.getDefaultExecutor().execute(() -> downloadObjectAsync(asyncResponse, operationId));
    }

    private void downloadObjectAsync(final AsyncResponse asyncResponse, String operationId) {

        // Get the TRACEABILITY operation
        LogbookOperation operationToCheck = null;
        try (LogbookOperationsClient logbookOperationsClient =
            LogbookOperationsClientFactory.getInstance().getClient()) {

            RequestResponseOK requestResponseOK =
                RequestResponseOK.getFromJsonNode(logbookOperationsClient.selectOperationById(operationId, null));

            List<ObjectNode> foundOperation = requestResponseOK.getResults();
            if (foundOperation == null || foundOperation.isEmpty() || foundOperation.size() > 1) {
                // More than operation found return BAD_REQUEST response
                AsyncInputStreamHelper.asyncResponseResume(asyncResponse, Response.status(Status.BAD_REQUEST).build());
                return;
            }

            operationToCheck = new LogbookOperation(foundOperation.get(0));
            String operationType = (String) operationToCheck.get(LogbookMongoDbName.eventTypeProcess.getDbname());

            // Check if it a traceability operation
            if (!LogbookTypeProcess.TRACEABILITY.equals(LogbookTypeProcess.valueOf(operationType))) {
                // It wasn't a traceability operation
                AsyncInputStreamHelper.asyncResponseResume(asyncResponse, Response.status(Status.BAD_REQUEST).build());
                return;
            }
        } catch (InvalidParseOperationException | LogbookClientException | IllegalArgumentException e) {
            LOGGER.error(e.getMessage(), e);
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse,
                Response.status(Status.INTERNAL_SERVER_ERROR).build());
            return;
        }

        // A valid operation found : download the related file
        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {

            // Get last event to extract eventDetailData field
            ArrayList events = (ArrayList) operationToCheck.get(LogbookDocument.EVENTS);
            Document lastEvent = (Document) Iterables.getLast(events);

            // Create TraceabilityEvent instance
            String evDetData = lastEvent.getString(LogbookMongoDbName.eventDetailData.getDbname());
            JsonNode eventDetail = JsonHandler.getFromString(evDetData);

            TraceabilityEvent traceabilityEvent =
                JsonHandler.getFromJsonNode(eventDetail, TraceabilityEvent.class);
            String fileName = traceabilityEvent.getFileName();

            // Get zip file
            final Response response =
                storageClient.getContainerAsync(DEFAULT_STORAGE_STRATEGY, fileName, StorageCollectionType.LOGBOOKS);

            final AsyncInputStreamHelper helper = new AsyncInputStreamHelper(asyncResponse, response);
            if (response.getStatus() == Status.OK.getStatusCode()) {
                helper.writeResponse(Response
                    .ok()
                    .header("Content-Disposition", "filename=" + fileName)
                    .header("Content-Type", "application/octet-stream"));
            } else {
                helper.writeResponse(Response.status(response.getStatus()));
            }

        } catch (StorageServerClientException | StorageNotFoundException e) {
            LOGGER.error(e.getMessage(), e);
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse,
                Response.status(Status.INTERNAL_SERVER_ERROR).build());
            return;
        } catch (InvalidParseOperationException e) {
            LOGGER.error(e.getMessage(), e);
            AsyncInputStreamHelper.asyncResponseResume(asyncResponse,
                Response.status(Status.INTERNAL_SERVER_ERROR).build());
            return;
        }
    }

    private void createOrUpdateLogbookOperation(LogbookOperationsClientHelper helper,
        boolean isCreationMode, GUID eventIdentifier, StatusCode outcome)
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException,
        LogbookClientNotFoundException {

        final LogbookOperationParameters parameters =
            LogbookParametersFactory.newLogbookOperationParameters(eventIdentifier,
                CHECK_LOGBOOK_OP_SECURISATION, eventIdentifier, LogbookTypeProcess.CHECK, outcome,
                VitamLogbookMessages.getCodeOp(CHECK_LOGBOOK_OP_SECURISATION, outcome), eventIdentifier);

        if (isCreationMode) {
            helper.createDelegate(parameters);
        } else {
            helper.updateDelegate(parameters);
        }
    }

    private JsonNode addProdServicesToQuery(JsonNode queryDsl) throws InvalidParseOperationException, InvalidCreateOperationException{        
        Set<String> prodServices = VitamThreadUtils.getVitamSession().getProdServices();

        if (prodServices == null || prodServices.isEmpty()){
            return queryDsl; 
        } else {
            final SelectParserSingle parser = new SelectParserSingle(new LogbookVarNameAdapter());
            parser.parse(queryDsl);
            parser.addCondition(QueryHelper.in("events.agIdOrig", prodServices.stream().toArray(String[]::new)));
            return parser.getRequest().getFinalSelect();
        }
    }    

    
}
