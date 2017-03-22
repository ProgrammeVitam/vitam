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
package fr.gouv.vitam.processing.management.rest;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.Status.Family;

import com.codahale.metrics.Gauge;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.WorkflowNotFoundException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.server.application.AbstractVitamApplication;
import fr.gouv.vitam.common.server.application.AsyncInputStreamHelper;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.processing.common.ProcessingEntry;
import fr.gouv.vitam.processing.common.config.ServerConfiguration;
import fr.gouv.vitam.processing.common.exception.ProcessWorkflowNotFoundException;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.model.ProcessWorkflow;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.processing.engine.core.monitoring.ProcessMonitoring;
import fr.gouv.vitam.processing.engine.core.monitoring.ProcessMonitoringImpl;
import fr.gouv.vitam.processing.management.api.ProcessManagement;
import fr.gouv.vitam.processing.management.core.ProcessManagementImpl;

/**
 * This class is resource provider of ProcessManagement
 */
@Path("/processing/v1")
@javax.ws.rs.ApplicationPath("webresources")
public class ProcessManagementResource extends ApplicationStatusResource {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProcessManagementResource.class);

    private static final String ERR_OPERATION_ID_IS_MANDATORY = "The operation identifier is mandatory";

    private static final String UNAUTHORIZED_ACTION = "Unauthorized action :";

    private static final String PROCESS_ID_FIELD = "operation_id";
    private static final String PROCESS_TYPE_FIELD = "processType";
    private static final String EXECUTION_MODE_FIELD = "executionMode";
    private static final String GLOBAL_EXECUTION_STATUS_FIELD = "globalStatus";
    private static final String PROCESS_DATE_FIELD = "processDate";

    // TODO remove if it won't be used
    private static final String STEP_EXECUTION_STATUS_FIELD = "stepStatus";
    private static final String ERR_PROCESS_INPUT_ISMANDATORY = "The process input object is mandatory";

    private final ServerConfiguration config;
    private final ProcessManagement processManagementMock;
    private final ProcessMonitoring processMonitoring;
    private final AtomicLong runningWorkflows = new AtomicLong(0L);

    /**
     * ProcessManagementResource : initiate the ProcessManagementResource resources
     *
     * @param configuration the server configuration to be applied
     */
    public ProcessManagementResource(ServerConfiguration configuration) {
        processManagementMock = null;
        config = configuration;
        processMonitoring = ProcessMonitoringImpl.getInstance();
        LOGGER.info("init Process Management Resource server");
        AbstractVitamApplication.getBusinessMetricsRegistry().register("Running workflows",
            new Gauge<Long>() {
                @Override
                public Long getValue() {
                    return runningWorkflows.get();
                }
            });
    }

    /**
     * For test purpose
     *
     * @param pManagement the processManagement to mock
     * @param configuration the configuration
     */
    ProcessManagementResource(ProcessManagement pManagement, ServerConfiguration configuration) {
        processManagementMock = pManagement;
        config = configuration;
        processMonitoring = ProcessMonitoringImpl.getInstance();
    }

    /**
     * Execute the process as a set of operations.
     *
     * @param headers
     * @param process as Json of type ProcessingEntry, indicate the container and workflowId
     * @param asyncResponse
     * @return http response
     */
    @Path("operations")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void executeVitamProcess(@Context HttpHeaders headers, ProcessingEntry process,
        @Suspended final AsyncResponse asyncResponse) {

        ParametersChecker.checkParameter("process Entry  is a mandatory parameter", process);
        ParametersChecker.checkParameter("actionId is a mandatory parameter",
            headers.getRequestHeader(GlobalDataRest.X_ACTION));

        final String xAction = headers.getRequestHeader(GlobalDataRest.X_ACTION).get(0);
        ProcessAction action = ProcessAction.getProcessAction(xAction);
        ParametersChecker.checkParameter("ProcessAction  is a mandatory parameter",
            action);

        Integer tenantId = VitamThreadUtils.getVitamSession().getTenantId();

        ItemStatus resp = null;
        ProcessManagement processManagement = processManagementMock;
        final WorkerParameters workParams = WorkerParametersFactory.newWorkerParameters().setContainerName(process
            .getContainer()).setUrlMetadata(config.getUrlMetadata()).setUrlWorkspace(config.getUrlWorkspace());

        try {
            runningWorkflows.incrementAndGet();
            if (processManagement == null) {
                processManagement = new ProcessManagementImpl(config); // NOSONAR mock management
            }
            resp = processManagement.submitWorkflow(workParams, process.getWorkflow(), action, asyncResponse, tenantId);
        } catch (WorkflowNotFoundException e) {
            // if workflow or handler not found
            LOGGER.error(e);
            this.asyncResponse(asyncResponse, Status.NOT_FOUND, null);
            return;
        } catch (final IllegalArgumentException e) {
            // if the entry argument if illegal
            LOGGER.error(e);
            this.asyncResponse(asyncResponse, Status.PRECONDITION_FAILED, null);
            return;
        } catch (final ProcessingException e) {
            // if there is an unauthorized action
            LOGGER.error(e);
            this.asyncResponse(asyncResponse, Status.UNAUTHORIZED, null);
            return;
        } finally {
            runningWorkflows.decrementAndGet();
            if (processManagementMock == null && processManagement != null) {
                processManagement.close();
            }

        }
        this.asyncResponse(asyncResponse, getStatusFrom(resp), resp);
    }

    /**
     * Update the process executions.
     *
     * @param process as Json of type ProcessingEntry, indicate the container and workflowId
     * @param headers contain X-Action and X-Context-ID
     * @return http response
     */
    @Path("operations")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateVitamProcess(@Context HttpHeaders headers, ProcessingEntry process) {
        return Response.status(Status.NOT_IMPLEMENTED).build();
    }

    private Status getStatusFrom(ItemStatus response) {
        switch (response.getGlobalStatus()) {
            case KO:
                return Status.BAD_REQUEST;
            case FATAL:
                return Status.INTERNAL_SERVER_ERROR;
            default:
                return Status.OK;
        }
    }

    private VitamError getErrorEntity(Status status) {
        return new VitamError(status.name()).setHttpCode(status.getStatusCode())
            .setContext("ingest")
            .setState("code_vitam")
            .setMessage(status.getReasonPhrase())
            .setDescription(status.getReasonPhrase());
    }

    /**
     * Resume the asynchronous response following a given status and entity
     * 
     * @param asyncResponse
     * @param status
     * @param entity
     */
    private void asyncResponse(final AsyncResponse asyncResponse, Status status, Object entity) {
        ResponseBuilder builder = Response.status(status);
        if (status.getFamily() == Family.CLIENT_ERROR || status.getFamily() == Family.SERVER_ERROR) {
            builder.entity(entity == null ? getErrorEntity(status) : entity);
        } else {
            builder.entity(entity);
        }
        AsyncInputStreamHelper.asyncResponseResume(asyncResponse, builder.build());
    }

    /**
     * Execute the process of an operation related to the id.
     *
     *
     * @param headers contain X-Action and X-Context-ID
     * @param process as Json of type ProcessingEntry, indicate the container and workflowId
     * @param id operation identifier
     * @param asyncResponse
     * @return http response
     * @throws ProcessingException
     */
    @Path("operations/{id}")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void executeWorkFlow(@Context HttpHeaders headers, @PathParam("id") String id,
        ProcessingEntry process, @Suspended final AsyncResponse asyncResponse) {

        ParametersChecker.checkParameter(ERR_OPERATION_ID_IS_MANDATORY, id);
        ParametersChecker.checkParameter(ERR_PROCESS_INPUT_ISMANDATORY, process);
        
        final WorkerParameters workParams = WorkerParametersFactory.newWorkerParameters().setContainerName(process
            .getContainer()).setUrlMetadata(config.getUrlMetadata()).setUrlWorkspace(config.getUrlWorkspace());

        ItemStatus resp = new ItemStatus(id);
        ProcessManagement processManagement = processManagementMock;
        ParametersChecker.checkParameter("actionId is a mandatory parameter",
            headers.getRequestHeader(GlobalDataRest.X_ACTION));

        Integer tenantId = VitamThreadUtils.getVitamSession().getTenantId();

        try {
            runningWorkflows.incrementAndGet();
            if (processManagement == null) {
                processManagement = new ProcessManagementImpl(config); // NOSONAR mock management
            }
            final String xAction = headers.getRequestHeader(GlobalDataRest.X_ACTION).get(0);
            final ProcessAction action = ProcessAction.getProcessAction(xAction);
            switch (action) {
                case INIT:
                    // Initialize the process to execute

                    // 1- Get contextId == LogbookTypeProcess given on the header X_CONTEXT_ID
                    final String xContextId = headers.getRequestHeader(GlobalDataRest.X_CONTEXT_ID).get(0);
                    ParametersChecker.checkParameter("X_CONTEXT_ID is a mandatory parameter",
                        xContextId);

                    LogbookTypeProcess logbookTypeProcess = LogbookTypeProcess.valueOf(xContextId);

                    processManagement.initWorkflow(workParams, process.getWorkflow(),
                        logbookTypeProcess, asyncResponse, tenantId);
                    break;

                case NEXT:
                case RESUME:
                    // Start the process
                    resp = processManagement.submitWorkflow(workParams, null, action, asyncResponse, tenantId);
                    break;

                default:
                    this.asyncResponse(asyncResponse, Status.UNAUTHORIZED, "UNAUTHORIZED_ACTION + xAction");
                    return;
            }

        } catch (final ProcessingException e) {
            // if there is an unauthorized action
            LOGGER.error(e);
            this.asyncResponse(asyncResponse, Status.UNAUTHORIZED, null);
            return;
        } catch (final IllegalArgumentException e) {
            LOGGER.error(e);
            this.asyncResponse(asyncResponse, Status.INTERNAL_SERVER_ERROR, null);
            return;
        }
    }

    /**
     * get the workflow status
     *
     * @param id operation identifier
     * @param query the query
     * @return http response
     */
    @Path("operations/{id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOperationProcessExecutionDetails(@PathParam("id") String id, JsonNode query) {

        ParametersChecker.checkParameter(ERR_OPERATION_ID_IS_MANDATORY,
            id);
        Status status;
        ProcessWorkflow pwork;
        Integer tenantId = VitamThreadUtils.getVitamSession().getTenantId();
        try {
            pwork = processMonitoring.getProcessWorkflow(id, tenantId);
        } catch (final IllegalArgumentException e) {
            // if the entry argument if illegal
            LOGGER.error(e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status)
                .entity(getErrorEntity(status))
                .build();
        } catch (WorkflowNotFoundException e) {
            // if workflow or handler not found
            LOGGER.error(e);
            status = Status.NOT_FOUND;
            return Response.status(status)
                .entity(getErrorEntity(status))
                .build();
        }
        return Response.status(Status.OK).entity(new ItemStatus(id).setGlobalExecutionStatus(pwork.getExecutionStatus())
            .increment(pwork.getGlobalStatusCode())).build();
    }

    /**
     * Update the status of an operation.
     *
     * @param headers contain X-Action and X-Context-ID
     * @param process as Json of type ProcessingEntry, indicate the container and workflowId
     * @param id operation identifier
     * @param asyncResponse
     */
    @Path("operations/{id}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void updateWorkFlowStatus(@Context HttpHeaders headers, @PathParam("id") String id,
        @Suspended final AsyncResponse asyncResponse) {

        ParametersChecker.checkParameter("actionId is a mandatory parameter",
            headers.getRequestHeader(GlobalDataRest.X_ACTION));
        ParametersChecker.checkParameter(ERR_OPERATION_ID_IS_MANDATORY,
            id);
        Integer tenantId = VitamThreadUtils.getVitamSession().getTenantId();

        final WorkerParameters workParams = WorkerParametersFactory.newWorkerParameters().setContainerName(id)
            .setUrlMetadata(config.getUrlMetadata()).setUrlWorkspace(config.getUrlWorkspace());

        ItemStatus itemStatus = new ItemStatus(id);
        ProcessManagement processManagement = processManagementMock;
        try {
            if (processManagement == null) {
                processManagement = new ProcessManagementImpl(config); // NOSONAR mock management
            }
            final String xAction = headers.getRequestHeader(GlobalDataRest.X_ACTION).get(0);
            final ProcessAction action = ProcessAction.getProcessAction(xAction);

            switch (action) {
                case NEXT:
                case RESUME:
                    // launch process workflow
                    itemStatus = processManagement.submitWorkflow(workParams, null,
                        action, asyncResponse, tenantId);
                    break;
                case PAUSE:
                    itemStatus =
                        processManagement.pauseProcessWorkFlow(workParams.getContainerName(), tenantId, asyncResponse);
                    break;
                default:
                    this.asyncResponse(asyncResponse, Status.UNAUTHORIZED, "UNAUTHORIZED_ACTION" + xAction);
                    return;
            }

        } catch (ProcessWorkflowNotFoundException e) {
            // if workflow or handler not found
            LOGGER.error(e);
            this.asyncResponse(asyncResponse, Status.NOT_FOUND, null);
            return;
        } catch (final IllegalArgumentException e) {
            // if the entry argument if illegal
            LOGGER.error(e);
            this.asyncResponse(asyncResponse, Status.PRECONDITION_FAILED, null);
            return;
        } catch (final ProcessingException e) {
            // if there is an unauthorized action
            LOGGER.error(e);
            this.asyncResponse(asyncResponse, Status.UNAUTHORIZED, null);
            return;
        }

    }

    /**
     * Interrupt the process of an operation identified by Id.
     *
     * @param id operation identifier
     * @param asyncResponse
     * @return http response
     */
    @Path("operations/{id}")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void interruptWorkFlowExecution(@PathParam("id") String id,
        @Suspended final AsyncResponse asyncResponse) {
        ParametersChecker.checkParameter(ERR_OPERATION_ID_IS_MANDATORY,
            id);
        ProcessManagement processManagement = processManagementMock;
        ItemStatus itemStatus = null;
        Integer tenantId = VitamThreadUtils.getVitamSession().getTenantId();
        try {
            if (processManagement == null) {
                processManagement = new ProcessManagementImpl(config); // NOSONAR mock management
            }
            itemStatus = processManagement.cancelProcessWorkflow(id, tenantId, asyncResponse);
        } catch (WorkflowNotFoundException | ProcessWorkflowNotFoundException e) {
            // if workflow or handler not found
            LOGGER.error(e);
            this.asyncResponse(asyncResponse, Status.NOT_FOUND, null);
            return;
        } catch (final IllegalArgumentException e) {
            // if the entry argument if illegal
            LOGGER.error(e);
            this.asyncResponse(asyncResponse, Status.PRECONDITION_FAILED, null);
            return;
        } catch (ProcessingException e) {
            LOGGER.error(e);
            this.asyncResponse(asyncResponse, Status.UNAUTHORIZED, null);
            return;
        } catch (Exception e) {
            LOGGER.error(e);
            this.asyncResponse(asyncResponse, Status.INTERNAL_SERVER_ERROR, null);
            return;
        }
        this.asyncResponse(asyncResponse, Status.OK, itemStatus);
    }

    /**
     * get the operation status
     *
     * @param id operation identifier
     * @return http response
     */
    @Path("operations/{id}")
    @HEAD
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getWorkFlowExecutionStatus(@PathParam("id") String id) {
        Status status;
        ProcessWorkflow pwork;
        Integer tenantId = VitamThreadUtils.getVitamSession().getTenantId();
        ParametersChecker.checkParameter(ERR_OPERATION_ID_IS_MANDATORY, id);
        try {
            pwork = processMonitoring.getProcessWorkflow(id, tenantId);
        } catch (final IllegalArgumentException e) {
            // if the entry argument if illegal
            LOGGER.error(e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status)
                .entity(getErrorEntity(status))
                .build();
        } catch (WorkflowNotFoundException e) {
            // if workflow or handler not found
            LOGGER.error(e);
            status = Status.NOT_FOUND;
            return Response.status(status)
                .entity(getErrorEntity(status))
                .build();
        }
        return Response.status(Status.OK).entity(new ItemStatus(id).setGlobalExecutionStatus(pwork.getExecutionStatus())
            .increment(pwork.getGlobalStatusCode())).build();
    }


    /**
     * TODO add javadoc
     * 
     * @return
     */
    @GET
    @Path("/operations")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProcessWorkflows() {
        Integer tenantId = VitamThreadUtils.getVitamSession().getTenantId();
        List<ProcessWorkflow> listWorkflows = processMonitoring.getAllProcessWorkflow(tenantId);

        if (listWorkflows != null) {
            ArrayNode result = JsonHandler.createArrayNode();
            for (ProcessWorkflow processWorkflow : listWorkflows) {
                ObjectNode workflow = JsonHandler.createObjectNode();
                workflow.put(PROCESS_ID_FIELD, processWorkflow.getOperationId());
                workflow.put(PROCESS_TYPE_FIELD, processWorkflow.getLogbookTypeProcess().toString());
                workflow.put(EXECUTION_MODE_FIELD, processWorkflow.getExecutionMode().toString());
                workflow.put(GLOBAL_EXECUTION_STATUS_FIELD, processWorkflow.getExecutionStatus().toString());
                workflow.put(STEP_EXECUTION_STATUS_FIELD, processWorkflow.getGlobalStatusCode().toString());
                workflow.put(PROCESS_DATE_FIELD, LocalDateUtil.getFormattedDate(processWorkflow.getProcessDate()));

                result.add(workflow);
            }

            return Response.status(Status.OK).entity(result).build();
        }
        return Response.status(Status.OK).build();
    }
}
