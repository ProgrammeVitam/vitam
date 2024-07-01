/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "https://cecill.info".
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
package fr.gouv.vitam.processing.management.rest;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.StateNotAllowedException;
import fr.gouv.vitam.common.exception.WorkflowNotFoundException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.lifecycle.ProcessLifeCycle;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.ProcessPause;
import fr.gouv.vitam.common.model.ProcessQuery;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.processing.ProcessDetail;
import fr.gouv.vitam.common.model.processing.WorkFlow;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.processing.common.ProcessingEntry;
import fr.gouv.vitam.processing.common.config.ServerConfiguration;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.exception.ProcessingStorageWorkspaceException;
import fr.gouv.vitam.processing.common.model.ProcessWorkflow;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.processing.distributor.api.ProcessDistributor;
import fr.gouv.vitam.processing.engine.core.monitoring.ProcessMonitoring;
import fr.gouv.vitam.processing.engine.core.monitoring.ProcessMonitoringImpl;
import fr.gouv.vitam.processing.management.api.ProcessManagement;
import fr.gouv.vitam.processing.management.core.ProcessManagementImpl;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Path("/processing/v1")
@ApplicationPath("webresources")
@Tag(name = "Processing")
public class ProcessManagementResource extends ApplicationStatusResource {

    private static final String WORKFLOW = "workflow";

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProcessManagementResource.class);

    private static final String ERR_OPERATION_ID_IS_MANDATORY = "The operation identifier is mandatory";

    private static final String ERR_PROCESS_INPUT_ISMANDATORY = "The process input object is mandatory";

    private final ServerConfiguration config;
    private final ProcessManagement processManagement;
    private final ProcessMonitoring processMonitoring;
    private final AtomicLong runningWorkflows = new AtomicLong(0L);

    public ProcessLifeCycle getProcessLifeCycle() {
        return processManagement;
    }

    /**
     * ProcessManagementResource : initiate the ProcessManagementResource resources
     *
     * @param configuration the server configuration to be applied
     */
    public ProcessManagementResource(ServerConfiguration configuration, ProcessDistributor processDistributor) {
        config = configuration;
        try {
            processManagement = new ProcessManagementImpl(config, processDistributor);
        } catch (ProcessingStorageWorkspaceException e) {
            throw new RuntimeException(e);
        }
        processMonitoring = ProcessMonitoringImpl.getInstance();
        LOGGER.info("init Process Management Resource server");
    }

    /**
     * For test purpose
     *
     * @param pManagement the processManagement to mock
     * @param configuration the configuration
     */
    ProcessManagementResource(ProcessManagement pManagement, ServerConfiguration configuration) {
        processManagement = pManagement;
        config = configuration;
        processMonitoring = ProcessMonitoringImpl.getInstance();
    }

    private VitamError getErrorEntity(Status status, String msg, String description) {
        return new VitamError(status.name())
            .setHttpCode(status.getStatusCode())
            .setContext(WORKFLOW)
            .setState("code_vitam")
            .setMessage(msg)
            .setDescription(description);
    }

    /**
     * @param status
     * @param entity
     * @return
     */
    private Response buildResponse(Status status, Object entity) {
        return Response.status(status).entity(entity).build();
    }

    /**
     * Resume the asynchronous response following a given status and entity
     */
    private Response buildResponse(ItemStatus itemStatus) {
        RequestResponseOK<ItemStatus> responseOK = new RequestResponseOK<>();
        responseOK.addResult(itemStatus);
        responseOK.setHttpCode(itemStatus.getGlobalState().getEquivalentHttpStatus().getStatusCode());

        return Response.status(itemStatus.getGlobalState().getEquivalentHttpStatus())
            .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATE, itemStatus.getGlobalState())
            .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATUS, itemStatus.getGlobalStatus())
            .header(GlobalDataRest.X_CONTEXT_ID, itemStatus.getLogbookTypeProcess())
            .entity(responseOK)
            .build();
    }

    @Path("workflows")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getWorkflowDefinitions() {
        try {
            List<WorkFlow> workflowDefinitions = new ArrayList<>(processManagement.getWorkflowDefinitions().values());
            RequestResponseOK<WorkFlow> response = new RequestResponseOK<>();
            response
                .addAllResults(workflowDefinitions)
                .setHits(workflowDefinitions.size(), 0, workflowDefinitions.size(), workflowDefinitions.size())
                .setHttpCode(Status.OK.getStatusCode());
            return Response.status(Status.OK).entity(response).build();
        } catch (Exception e) {
            LOGGER.error("Error while retrieving workflow definitions : ", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(VitamCodeHelper.toVitamError(VitamCode.WORKFLOW_DEFINITION_ERROR, e.getLocalizedMessage()))
                .build();
        }
    }

    @Path("workflows/{workfowId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getWorkflowDetails(@PathParam("workfowId") String workfowId) {
        try {
            Optional<WorkFlow> optionalWorkflow = processManagement
                .getWorkflowDefinitions()
                .values()
                .stream()
                .filter(workFlow -> StringUtils.equals(workFlow.getId(), workfowId))
                .findFirst();
            if (optionalWorkflow.isPresent()) {
                return Response.status(Status.OK)
                    .header(GlobalDataRest.X_TYPE_PROCESS, optionalWorkflow.get().getTypeProc())
                    .entity(optionalWorkflow.get())
                    .build();
            }

            return Response.status(Status.NOT_FOUND).build();
        } catch (Exception e) {
            LOGGER.error("Error while retrieving workflow definitions : ", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity("Internal Server Error while getting workflow (" + workfowId + ") :" + e.getMessage())
                .build();
        }
    }

    /**
     * Execute the process of an operation related to the id.
     *
     * @param headers contain X-Action and X-Context-ID
     * @param process as Json of type ProcessingEntry, indicate the container and workflowId
     * @param id operation identifier
     * @throws ProcessingException if error in start a workflow
     */
    @Path("operations/{id}")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response executeWorkFlow(@Context HttpHeaders headers, @PathParam("id") String id, ProcessingEntry process) {
        ParametersChecker.checkParameter(ERR_OPERATION_ID_IS_MANDATORY, id);
        ParametersChecker.checkParameter(ERR_PROCESS_INPUT_ISMANDATORY, process);
        final String reqId = VitamThreadUtils.getVitamSession().getRequestId();

        final WorkerParameters workParams = WorkerParametersFactory.newWorkerParameters()
            .setContainerName(process.getContainer())
            .setRequestId(reqId)
            .setProcessId(process.getContainer())
            .setUrlMetadata(config.getUrlMetadata())
            .setUrlWorkspace(config.getUrlWorkspace());
        if (process.getExtraParams().size() > 0) {
            workParams.setMap(process.getExtraParams());
        }

        ParametersChecker.checkParameter(
            "actionId is a mandatory parameter",
            headers.getRequestHeader(GlobalDataRest.X_ACTION)
        );

        Integer tenantId = VitamThreadUtils.getVitamSession().getTenantId();
        final String xAction = headers.getRequestHeader(GlobalDataRest.X_ACTION).get(0);

        try {
            runningWorkflows.incrementAndGet();

            final ProcessAction action = ProcessAction.getProcessAction(xAction);

            ItemStatus itemStatus = null;
            switch (action) {
                case INIT:
                    // Initialize the process to start
                    ProcessWorkflow pw = processManagement.init(workParams, process.getWorkflow());
                    return buildResponse(Status.CREATED, pw);
                case NEXT:
                    // Start process in step by step mode
                    itemStatus = processManagement.next(workParams, tenantId);
                    break;
                case RESUME:
                    // Start process in continue mode
                    itemStatus = processManagement.resume(workParams, tenantId, true);
                    break;
                default:
                    return this.buildResponse(
                            Status.CONFLICT,
                            getErrorEntity(
                                Status.CONFLICT,
                                "NOT_ALLOWED_ACTION " + xAction,
                                "The action " +
                                xAction +
                                " is not allowed! Only INIT, NEXT and RESUME are allowed for this endpoint"
                            )
                        );
            }

            return this.buildResponse(itemStatus);
        } catch (StateNotAllowedException e) {
            LOGGER.error(e);
            return this.buildResponse(
                    Status.CONFLICT,
                    getErrorEntity(
                        Status.CONFLICT,
                        "NOT_ALLOWED_ACTION " + xAction,
                        "The action " + xAction + " is not allowed! The engine exception is :" + e.getMessage()
                    )
                );
        } catch (final ProcessingException e) {
            LOGGER.error(e);
            return this.buildResponse(
                    Status.PRECONDITION_FAILED,
                    getErrorEntity(
                        Status.PRECONDITION_FAILED,
                        "Error processing the action :" + xAction,
                        "The action " + xAction + " cause an error :" + e.getMessage()
                    )
                );
        } catch (final Exception e) {
            LOGGER.error(e);
            return this.buildResponse(
                    Status.INTERNAL_SERVER_ERROR,
                    getErrorEntity(
                        Status.INTERNAL_SERVER_ERROR,
                        "Internal error while processing the action :" + xAction,
                        "The action " + xAction + " cause an internal error :" + e.getMessage()
                    )
                );
        }
    }

    /**
     * get the workflow status
     *
     * @param id operation identifier
     * @return http response
     */
    @Path("operations/{id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOperationProcessExecutionDetails(@PathParam("id") String id) {
        Integer tenantId = VitamThreadUtils.getVitamSession().getTenantId();
        try {
            final ProcessWorkflow processWorkflow = processMonitoring.findOneProcessWorkflow(id, tenantId);
            ItemStatus itemStatus = new ItemStatus(id)
                .setGlobalState(processWorkflow.getState())
                .increment(processWorkflow.getStatus());

            RequestResponseOK<ItemStatus> responseOK = new RequestResponseOK<>();
            responseOK.addResult(itemStatus);
            responseOK.setHttpCode(itemStatus.getGlobalState().getEquivalentHttpStatus().getStatusCode());

            return Response.status(Status.OK)
                .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATE, processWorkflow.getState())
                .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATUS, processWorkflow.getStatus())
                .header(GlobalDataRest.X_CONTEXT_ID, processWorkflow.getLogbookTypeProcess())
                .entity(responseOK)
                .build();
        } catch (final IllegalArgumentException e) {
            LOGGER.error(e);
            return Response.status(Status.PRECONDITION_FAILED)
                .entity(
                    getErrorEntity(
                        Status.PRECONDITION_FAILED,
                        "Error while find ProcessWorkflow",
                        "The parameter id expected not null, id :" + id + " >> Error : " + e.getMessage()
                    )
                )
                .build();
        } catch (WorkflowNotFoundException e) {
            LOGGER.error(e);
            return Response.status(Status.NOT_FOUND)
                .entity(
                    getErrorEntity(
                        Status.NOT_FOUND,
                        "Error while find ProcessWorkflow",
                        "ProcessWorkflow not found with tenant :" +
                        tenantId +
                        " and with id:" +
                        id +
                        " >> Error : " +
                        e.getMessage()
                    )
                )
                .build();
        } catch (Exception e) {
            LOGGER.error(e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(
                    getErrorEntity(
                        Status.INTERNAL_SERVER_ERROR,
                        "Error while find ProcessWorkflow",
                        "ProcessWorkflow with tenant :" +
                        tenantId +
                        " and with id:" +
                        id +
                        " >> Error : " +
                        e.getMessage()
                    )
                )
                .build();
        }
    }

    /**
     * Update the status of an operation.
     *
     * @param headers contain X-Action and X-Context-ID
     * @param id operation identifier
     */
    @Path("operations/{id}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateWorkFlowStatus(@Context HttpHeaders headers, @PathParam("id") String id) {
        ParametersChecker.checkParameter(
            "actionId is a mandatory parameter",
            headers.getRequestHeader(GlobalDataRest.X_ACTION)
        );
        ParametersChecker.checkParameter(ERR_OPERATION_ID_IS_MANDATORY, id);
        Integer tenantId = VitamThreadUtils.getVitamSession().getTenantId();
        final String reqId = VitamThreadUtils.getVitamSession().getRequestId();

        final WorkerParameters workParams = WorkerParametersFactory.newWorkerParameters()
            .setContainerName(id)
            .setRequestId(reqId)
            .setProcessId(id)
            .setUrlMetadata(config.getUrlMetadata())
            .setUrlWorkspace(config.getUrlWorkspace());

        final String xAction = headers.getRequestHeader(GlobalDataRest.X_ACTION).get(0);

        try {
            final ProcessAction action = ProcessAction.getProcessAction(xAction);

            ItemStatus itemStatus;
            switch (action) {
                case NEXT:
                    itemStatus = processManagement.next(workParams, tenantId);
                    break;
                case RESUME:
                    itemStatus = processManagement.resume(workParams, tenantId, false);
                    break;
                case REPLAY:
                    itemStatus = processManagement.replay(workParams, tenantId);
                    break;
                case PAUSE:
                    itemStatus = processManagement.pause(workParams.getContainerName(), tenantId);

                    break;
                default:
                    return this.buildResponse(
                            Status.CONFLICT,
                            getErrorEntity(
                                Status.CONFLICT,
                                "NOT_ALLOWED_ACTION " + xAction,
                                "The action " +
                                xAction +
                                " is not allowed! Only INIT, NEXT, REPLAY and RESUME are allowed for this endpoint"
                            )
                        );
            }

            return this.buildResponse(itemStatus);
        } catch (StateNotAllowedException e) {
            LOGGER.error(e);
            return this.buildResponse(
                    Status.CONFLICT,
                    getErrorEntity(
                        Status.CONFLICT,
                        "NOT_ALLOWED_ACTION " + xAction,
                        "The action " + xAction + " is not allowed! The engine exception is :" + e.getMessage()
                    )
                );
        } catch (final ProcessingException e) {
            LOGGER.error(e);
            return this.buildResponse(
                    Status.PRECONDITION_FAILED,
                    getErrorEntity(
                        Status.PRECONDITION_FAILED,
                        "Error processing the action :" + xAction,
                        "The action " + xAction + " cause an error :" + e.getMessage()
                    )
                );
        } catch (final Exception e) {
            LOGGER.error(e);
            return this.buildResponse(
                    Status.INTERNAL_SERVER_ERROR,
                    getErrorEntity(
                        Status.INTERNAL_SERVER_ERROR,
                        "Internal error while processing the action :" + xAction,
                        "The action " + xAction + " cause an internal error :" + e.getMessage()
                    )
                );
        }
    }

    /**
     * Interrupt the process of an operation identified by Id.
     *
     * @param id operation identifier
     */
    @Path("operations/{id}")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response cancelOperationProcessExecution(@PathParam("id") String id) {
        ParametersChecker.checkParameter(ERR_OPERATION_ID_IS_MANDATORY, id);

        Integer tenantId = VitamThreadUtils.getVitamSession().getTenantId();
        try {
            final ItemStatus itemStatus = processManagement.cancel(id, tenantId);
            return this.buildResponse(itemStatus);
        } catch (StateNotAllowedException e) {
            LOGGER.error(e);
            return this.buildResponse(
                    Status.CONFLICT,
                    getErrorEntity(
                        Status.CONFLICT,
                        "NOT_ALLOWED_ACTION  CANCEL",
                        "The action cancel is not allowed! The engine exception is :" + e.getMessage()
                    )
                );
        } catch (final ProcessingException e) {
            LOGGER.error(e);
            return this.buildResponse(
                    Status.NOT_FOUND,
                    getErrorEntity(
                        Status.NOT_FOUND,
                        "Error processing the action : CANCEL",
                        "The action cancel cause an error :" + e.getMessage()
                    )
                );
        } catch (final Exception e) {
            LOGGER.error(e);
            return this.buildResponse(
                    Status.INTERNAL_SERVER_ERROR,
                    getErrorEntity(
                        Status.INTERNAL_SERVER_ERROR,
                        "Internal error while processing the action : CANCEL",
                        "The action cancel cause an internal error :" + e.getMessage()
                    )
                );
        }
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
    public Response getWorkFlowState(@PathParam("id") String id) {
        Integer tenantId = VitamThreadUtils.getVitamSession().getTenantId();
        try {
            final ProcessWorkflow processWorkflow = processMonitoring.findOneProcessWorkflow(id, tenantId);
            Response.ResponseBuilder builder = Response.status(Status.ACCEPTED);
            if (ProcessState.COMPLETED.equals(processWorkflow.getState())) {
                builder.status(Status.OK);
            } else {
                builder.status(Status.ACCEPTED);
            }

            return builder
                .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATE, processWorkflow.getState())
                .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATUS, processWorkflow.getStatus())
                .header(GlobalDataRest.X_CONTEXT_ID, processWorkflow.getLogbookTypeProcess())
                .build();
        } catch (final IllegalArgumentException e) {
            LOGGER.error(e);
            return Response.status(Status.PRECONDITION_FAILED).build();
        } catch (WorkflowNotFoundException e) {
            LOGGER.error(e);
            return Response.status(Status.NOT_FOUND).build();
        } catch (Exception e) {
            LOGGER.error(e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * get the process workflow
     *
     * @param query the filter query
     * @return the workflow in response
     */
    @GET
    @Path("/operations")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response findProcessWorkflow(ProcessQuery query) {
        try {
            Integer tenantId = VitamThreadUtils.getVitamSession().getTenantId();
            List<ProcessDetail> processDetails = processManagement.getFilteredProcess(query, tenantId);
            RequestResponseOK<ProcessDetail> response = new RequestResponseOK<>(JsonHandler.toJsonNode(query));
            response
                .addAllResults(processDetails)
                .setHits(processDetails.size(), 0, processDetails.size(), processDetails.size())
                .setHttpCode(Status.OK.getStatusCode());
            return Response.status(Status.OK).entity(response).build();
        } catch (Exception e) {
            LOGGER.error("Error while finding existing workflow process: ", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(VitamCodeHelper.toVitamError(VitamCode.WORKFLOW_PROCESSES_ERROR, e.getMessage()))
                .build();
        }
    }

    /**
     * Pause the processes specified by ProcessPause info
     *
     * @param info a ProcessPause object indicating the tenant and/or the type of process to pause
     * @return
     */
    @Path("/forcepause")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response forcePause(ProcessPause info) {
        try {
            processManagement.forcePause(info);

            RequestResponseOK<ProcessPause> response = new RequestResponseOK<>();
            response.addResult(info).setHttpCode(Status.OK.getStatusCode());
            return Response.status(Status.OK).entity(response).build();
        } catch (ProcessingException e) {
            LOGGER.error(e);
            return this.buildResponse(
                    Status.BAD_REQUEST,
                    getErrorEntity(Status.BAD_REQUEST, "Error while set force pause", e.getMessage())
                );
        }
    }

    /**
     * Remove the pause for the processes specified by ProcessPause info
     *
     * @param info a ProcessPause object indicating the tenant and/or the type of process to pause
     * @return
     */
    @Path("/removeforcepause")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeForcePause(ProcessPause info) {
        try {
            processManagement.removeForcePause(info);
            RequestResponseOK<ProcessPause> response = new RequestResponseOK<>();
            response.addResult(info).setHttpCode(Status.OK.getStatusCode());
            return Response.status(Status.OK).entity(response).build();
        } catch (ProcessingException e) {
            LOGGER.error(e);
            return this.buildResponse(
                    Status.BAD_REQUEST,
                    getErrorEntity(Status.BAD_REQUEST, "Error while remove force pause", e.getMessage())
                );
        }
    }
}
