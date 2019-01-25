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


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.access.internal.api.AccessInternalModule;
import fr.gouv.vitam.access.internal.common.exception.AccessInternalExecutionException;
import fr.gouv.vitam.access.internal.core.AccessInternalModuleImpl;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.accesslog.AccessLogInfoModel;
import fr.gouv.vitam.common.accesslog.AccessLogUtils;
import fr.gouv.vitam.common.client.DefaultClient;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.parser.request.single.SelectParserSingle;
import fr.gouv.vitam.common.database.utils.AccessContractRestrictionHelper;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InternalServerException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamDBException;
import fr.gouv.vitam.common.exception.WorkflowNotFoundException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.stream.VitamAsyncInputStreamResponse;
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
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbName;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookOperation;
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
import fr.gouv.vitam.storage.engine.common.model.DataCategory;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbName.eventDetailData;

/**
 * AccessResourceImpl implements AccessResource
 */
@Path("/access-internal/v1")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@javax.ws.rs.ApplicationPath("webresources")
public class LogbookInternalResourceImpl {

    private static final String CHECK_LOGBOOK_OP_SECURISATION = "CHECK_LOGBOOK_OP_SECURISATION";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogbookInternalResourceImpl.class);
    private static final String LOGBOOK_MODULE = "LOGBOOK";
    private static final String CODE_VITAM = "code_vitam";

    // TODO Extract values from DSLQueryHelper
    private static final String EVENT_ID_PROCESS = "evIdProc";
    private static final String OB_ID = "obId";
    private static final String DSLQUERY_TO_CHECK_TRACEABILITY_OPERATION_NOT_FOUND =
        "DSL Query to start traceability check was not found.";

    private static final String DEFAULT_STORAGE_STRATEGY = "default";

    private static final long SLEEP_TIME = 20l;
    private static final long NB_TRY = 18000;

    private final AccessInternalModule accessModule;

    /**
     * Default Constructor
     */
    public LogbookInternalResourceImpl() {
        accessModule = new AccessInternalModuleImpl();
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
            final SelectParserSingle parser = new SelectParserSingle();
            Select select = new Select();
            parser.parse(select.getFinalSelect());
            parser.addCondition(QueryHelper.eq(EVENT_ID_PROCESS, operationId));
            final JsonNode result = client.selectOperationById(operationId);
            return Response.status(Status.OK).entity(result).build();
        } catch (final LogbookClientNotFoundException e) {
            LOGGER.error(e);
            status = Status.NOT_FOUND;
            return Response.status(status).entity(getErrorEntity(status, e.getMessage())).build();
        } catch (final LogbookClientException e) {
            LOGGER.error(e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status).entity(getErrorEntity(status, e.getMessage())).build();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status).entity(getErrorEntity(status, e.getMessage())).build();
        } catch (InvalidCreateOperationException e) {
            LOGGER.error(e);
            status = Status.BAD_REQUEST;
            return Response.status(status).entity(getErrorEntity(status, e.getMessage())).build();
        }
    }


    /**
     * GET with request in body
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
            final JsonNode result = client.selectOperation(query);
            return Response.status(Status.OK).entity(result).build();
        } catch (final LogbookClientException e) {
            LOGGER.error(e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status).entity(getErrorEntity(status, e.getMessage())).build();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status).entity(getErrorEntity(status, e.getMessage())).build();
        }
    }


    /*****
     * LOGBOOK LIFE CYCLES
     *****/

    /**
     * gets the unit life cycle based on its id
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
            Select unitQuery = new Select();
            unitQuery.setQuery(QueryHelper.eq(VitamFieldsHelper.id(), unitLifeCycleId));
            JsonNode unitResult =
                    accessModule.selectUnit(AccessContractRestrictionHelper.applyAccessContractRestrictionForUnitForSelect(unitQuery.getFinalSelect(),
                            VitamThreadUtils.getVitamSession().getContract()));
            if(unitResult.get("$hits").get("total").toString().equals("0")) {
                return Response.status(Status.UNAUTHORIZED.getStatusCode()).entity(getErrorEntity(Status.UNAUTHORIZED, "Accès refusé")).build();
            }

            final JsonNode result = client.selectUnitLifeCycleById(unitLifeCycleId, queryDsl);
            return Response.status(Status.OK).entity(result).build();

        } catch (final LogbookClientNotFoundException e) {
            LOGGER.error(e);
            status = Status.NOT_FOUND;
            return Response.status(status).entity(getErrorEntity(status, e.getMessage())).build();
        } catch (final LogbookClientException e) {
            LOGGER.error(e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status).entity(getErrorEntity(status, e.getMessage())).build();
        } catch (final InvalidParseOperationException | InvalidCreateOperationException e) {
            LOGGER.error(e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status).entity(getErrorEntity(status, e.getMessage())).build();
        } catch (final VitamDBException | AccessInternalExecutionException e) {
            LOGGER.error(e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status).entity(getErrorEntity(status, e.getMessage())).build();
        }
    }

    /**
     * gets the unit life cycle based on its id
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
            final SelectParserSingle parser = new SelectParserSingle();
            Select select = new Select();
            parser.parse(select.getFinalSelect());
            parser.addCondition(QueryHelper.eq(OB_ID, queryDsl.findValue(OB_ID).asText()));
            queryDsl = parser.getRequest().getFinalSelect();
            final JsonNode result = client.selectUnitLifeCycle(queryDsl);
            return Response.status(Status.OK).entity(result).build();
        } catch (final LogbookClientException e) {
            LOGGER.error(e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status).entity(getErrorEntity(status, e.getMessage())).build();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status).entity(getErrorEntity(status, e.getMessage())).build();
        } catch (InvalidCreateOperationException e) {
            LOGGER.error(e);
            status = Status.BAD_REQUEST;
            return Response.status(status).entity(getErrorEntity(status, e.getMessage())).build();
        }
    }


    /**
     * gets the object group life cycle based on its id
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
            Select gotQuery = new Select();
            gotQuery.setQuery(QueryHelper.eq(VitamFieldsHelper.id(), objectGroupLifeCycleId));
            final JsonNode gotResult = accessModule
                    .selectObjects(AccessContractRestrictionHelper
                            .applyAccessContractRestrictionForObjectGroupForSelect(gotQuery.getFinalSelect(),
                                    VitamThreadUtils.getVitamSession().getContract()));
            if(gotResult.get("$hits").get("total").toString().equals("0")) {
                return Response.status(Status.UNAUTHORIZED.getStatusCode()).entity(getErrorEntity(Status.UNAUTHORIZED, "Accès refusé")).build();
            }

            final JsonNode result = client.selectObjectGroupLifeCycleById(objectGroupLifeCycleId, queryDsl);
            return Response.status(Status.OK).entity(result).build();
        } catch (final LogbookClientNotFoundException e) {
            LOGGER.error(e);
            status = Status.NOT_FOUND;
            return Response.status(status).entity(getErrorEntity(status, e.getMessage())).build();
        } catch (final LogbookClientException e) {
            LOGGER.error(e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status).entity(getErrorEntity(status, e.getMessage())).build();
        } catch (final InvalidParseOperationException | InvalidCreateOperationException e) {
            LOGGER.error(e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status).entity(getErrorEntity(status, e.getMessage())).build();
        } catch (final VitamDBException | AccessInternalExecutionException e) {
            LOGGER.error(e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status).entity(getErrorEntity(status, e.getMessage())).build();
        }
    }

    /***** LIFE CYCLES - END *****/

    private InputStream getErrorStream(Status status, String message) {
        String aMessage =
            (message != null && !message.trim().isEmpty()) ? message
                : (status.getReasonPhrase() != null ? status.getReasonPhrase() : status.name());
        try {
            return JsonHandler.writeToInpustream(new VitamError(status.name())
                .setHttpCode(status.getStatusCode()).setContext(LOGBOOK_MODULE)
                .setState(CODE_VITAM).setMessage(status.getReasonPhrase()).setDescription(aMessage));
        } catch (InvalidParseOperationException e) {
            return new ByteArrayInputStream("{ 'message' : 'Invalid VitamError message' }".getBytes());
        }
    }

    private VitamError getErrorEntity(Status status, String message) {
        String aMessage =
            (message != null && !message.trim().isEmpty()) ? message
                : (status.getReasonPhrase() != null ? status.getReasonPhrase() : status.name());

        return new VitamError(status.name()).setHttpCode(status.getStatusCode()).setContext(LOGBOOK_MODULE)
            .setState(CODE_VITAM).setMessage(status.getReasonPhrase()).setDescription(aMessage);
    }

    /**
     * Checks a traceability operation based on a given DSLQuery
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
        GUID checkOperationGUID = null;
        LOGGER.debug("Start Check in Resource");
        try (LogbookOperationsClient logbookOperationsClient =
            LogbookOperationsClientFactory.getInstance().getClient();
            ProcessingManagementClient processingClient =
                ProcessingManagementClientFactory.getInstance().getClient()) {

            LogbookOperationsClientHelper helper = new LogbookOperationsClientHelper();

            // Initialize a new process
            checkOperationGUID = GUIDFactory.newOperationLogbookGUID(tenantId);
            processingClient.initVitamProcess(checkOperationGUID.getId(), LogbookTypeProcess.CHECK.name());

            // Create logbookOperation for check TRACEABILITY process
            createOrUpdateLogbookOperation(helper, true, checkOperationGUID, StatusCode.STARTED);
            logbookOperationsClient.bulkCreate(checkOperationGUID.getId(),
                helper.removeCreateDelegate(checkOperationGUID.getId()));

            LOGGER.debug("Started Check in Resource");
            // Run the WORKFLOW
            response =
                processingClient.executeCheckTraceabilityWorkFlow(checkOperationGUID.getId(), query,
                        LogbookTypeProcess.CHECK.name(), ProcessAction.RESUME.getValue());
            LOGGER.debug("Check in Resource launched");


            int nbTry = 0;
            boolean done = processingClient.isOperationCompleted(checkOperationGUID.getId());

            while (!done) {
                try {
                    Thread.sleep(SLEEP_TIME);
                } catch (InterruptedException e) {
                    SysErrLogger.FAKE_LOGGER.ignoreLog(e);
                }
                if (nbTry == NB_TRY)
                    break;
                nbTry++;
                done = processingClient.isOperationCompleted(checkOperationGUID.getId());
            }
            LOGGER.debug("End of Check in Resource: {} nbTry {}", done, nbTry);
            if (done) {
                // Get the created logbookOperation and return the response
                final JsonNode result = logbookOperationsClient.selectOperationById(checkOperationGUID.getId());
                return Response.ok().entity(RequestResponseOK.getFromJsonNode(result)).build();
            } else {
                ItemStatus itemStatus = processingClient.getOperationProcessStatus(checkOperationGUID.getId());
                Status status = Status.EXPECTATION_FAILED;
                if (itemStatus == null) {
                    itemStatus =
                        new ItemStatus(checkOperationGUID.getId()).setMessage("Unknown status of the workflow");
                    status = Status.INTERNAL_SERVER_ERROR;
                }
                return Response.status(status).entity(getErrorEntity(status, JsonHandler.unprettyPrint(itemStatus)))
                    .build();
            }
        } catch (BadRequestException | LogbookClientBadRequestException e) {
            LOGGER.error(e);
            final Status status = Status.BAD_REQUEST;
            return Response.status(status).entity(getErrorEntity(status, e.getMessage())).build();

        } catch (InternalServerException | VitamClientException | LogbookClientException |
            InvalidParseOperationException e) {
            LOGGER.error(e);
            final Status status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status).entity(getErrorEntity(status, e.getMessage())).build();

        } catch (WorkflowNotFoundException e) {
            LOGGER.error(e);
            final Status status = Status.NOT_FOUND;
            return Response.status(status).entity(getErrorEntity(status, e.getMessage())).build();
        } finally {
            DefaultClient.staticConsumeAnyEntityAndClose(response);
        }
    }

    /**
     * @param operationId
     * @return traceability operation stream
     */
    @GET
    @Path("/traceability/{idOperation}/content")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadTraceabilityOperation(@PathParam("idOperation") String operationId) {

        // Get the TRACEABILITY operation
        LogbookOperation operationToCheck = null;
        try (LogbookOperationsClient logbookOperationsClient =
            LogbookOperationsClientFactory.getInstance().getClient()) {

            final SelectParserSingle parser = new SelectParserSingle();
            Select select = new Select();
            parser.parse(select.getFinalSelect());
            parser.addCondition(QueryHelper.eq(EVENT_ID_PROCESS, operationId));

            RequestResponseOK requestResponseOK =
                RequestResponseOK.getFromJsonNode(
                    logbookOperationsClient.selectOperationById(operationId));

            List<ObjectNode> foundOperation = requestResponseOK.getResults();
            if (foundOperation == null || foundOperation.isEmpty() || foundOperation.size() > 1) {
                // More than operation found return BAD_REQUEST response
                return Response.status(Status.BAD_REQUEST)
                    .entity(getErrorStream(Status.BAD_REQUEST, "Operation not found")).build();
            }

            operationToCheck = new LogbookOperation(foundOperation.get(0));
            String operationType = (String) operationToCheck.get(LogbookMongoDbName.eventTypeProcess.getDbname());

            // Check if it a traceability operation
            if (!LogbookTypeProcess.TRACEABILITY.equals(LogbookTypeProcess.valueOf(operationType))) {
                // It wasn't a traceability operation
                return Response.status(Status.BAD_REQUEST)
                    .entity(getErrorStream(Status.BAD_REQUEST, "Not a traceability operation")).build();
            }
        } catch (final LogbookClientNotFoundException e) {
            LOGGER.error(e.getMessage(), e);
            // More than operation found return BAD_REQUEST response
            return Response.status(Status.BAD_REQUEST)
                .entity(getErrorStream(Status.BAD_REQUEST, "Operation not found")).build();
        } catch (InvalidParseOperationException | InvalidCreateOperationException | LogbookClientException |
            IllegalArgumentException e) {
            LOGGER.error(e.getMessage(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(getErrorStream(Status.INTERNAL_SERVER_ERROR, e.getMessage())).build();
        }

        // A valid operation found : download the related file
        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {

            TraceabilityEvent traceabilityEvent =
                JsonHandler.getFromString((String) operationToCheck.get(eventDetailData.getDbname()),
                    TraceabilityEvent.class);
            String fileName = traceabilityEvent.getFileName();

            // Get zip file
            DataCategory dataCategory = getDataCategory(traceabilityEvent);

            AccessLogInfoModel logInfo = AccessLogUtils.getNoLogAccessLog();
            final Response response =
                storageClient.getContainerAsync(DEFAULT_STORAGE_STRATEGY, fileName, dataCategory, logInfo);
            if (response.getStatus() == Status.OK.getStatusCode()) {
                Map<String, String> headers = new HashMap<>();
                headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM);
                headers.put(HttpHeaders.CONTENT_DISPOSITION, "filename=" + fileName);
                return new VitamAsyncInputStreamResponse(response,
                    Status.OK, headers);
            } else {
                Status status = (Status) response.getStatusInfo();
                storageClient.consumeAnyEntityAndClose(response);
                return Response.status(status).build();
            }

        } catch (StorageServerClientException | StorageNotFoundException e) {
            LOGGER.error(e.getMessage(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(getErrorStream(Status.INTERNAL_SERVER_ERROR, e.getMessage())).build();
        } catch (InvalidParseOperationException e) {
            LOGGER.error(e.getMessage(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(getErrorStream(Status.INTERNAL_SERVER_ERROR, e.getMessage())).build();
        }
    }

    private DataCategory getDataCategory(TraceabilityEvent traceabilityEvent) {

        if (traceabilityEvent.getLogType() == null) {
            throw new IllegalStateException("Missing traceability event type");
        }

        switch (traceabilityEvent.getLogType()) {
            case OPERATION:
            case UNIT_LIFECYCLE:
            case OBJECTGROUP_LIFECYCLE:
                return DataCategory.LOGBOOK;
            case STORAGE:
                return DataCategory.STORAGETRACEABILITY;
            default:
                throw new IllegalStateException("Invalid traceability event type " + traceabilityEvent.getLogType());
        }
    }

    private void createOrUpdateLogbookOperation(LogbookOperationsClientHelper helper,
        boolean isCreationMode, GUID eventIdentifier, StatusCode outcome)
        throws LogbookClientAlreadyExistsException, LogbookClientNotFoundException {

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


}
