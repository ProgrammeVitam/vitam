package fr.gouv.vitam.common.client;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InternalServerException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.exception.WorkflowNotFoundException;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessQuery;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.processing.ProcessDetail;
import fr.gouv.vitam.common.model.processing.WorkFlow;


/**
 * 
 * OperationManagementClient include all common method between ProcessManagement and Ingest Internal
 */
public interface OperationManagementClient extends MockOrRestClient {

    /**
     * getOperationProcessStatus:
     * 
     * get operation process status**
     * 
     * @param id : operation identifier*
     * @return ItemStatus response containing message and status*
     * @throws VitamClientException
     * @throws InternalServerException
     * @throws BadRequestException
     */

    ItemStatus getOperationProcessStatus(String id)
        throws VitamClientException, InternalServerException, BadRequestException;

    /**
     * 
     * getOperationProcessExecutionDetails : get operation processing execution details
     * 
     * @param id : operation identifier
     * @return Engine response containing message and status
     * @throws VitamClientException
     * @throws InternalServerException
     * @throws BadRequestException
     */

    ItemStatus getOperationProcessExecutionDetails(String id)
        throws VitamClientException, InternalServerException, BadRequestException;

    /**
     * cancelOperationProcessExecution : cancel processing operation
     * 
     * @param id : operation identifier
     * @return ItemStatus response containing message and status
     * @throws VitamClientException
     * @throws InternalServerException
     * @throws BadRequestException
     */
    ItemStatus cancelOperationProcessExecution(String id)
        throws InternalServerException, BadRequestException, VitamClientException;

    /**
     * updateOperationActionProcess : update operation processing status
     * 
     * 
     * @param actionId : identify the action to be executed by the workflow(next , pause,resume)
     * @param operationId : operation identifier
     * @return Response response containing message and status
     * @throws InternalServerException
     * @throws BadRequestException
     * @throws VitamClientException
     */
    RequestResponse<ItemStatus> updateOperationActionProcess(String actionId, String operationId)
        throws InternalServerException, BadRequestException, VitamClientException;


    /**
     * executeOperationProcess : execute an operation processing
     *
     * @param operationId id of the operation
     * @param workflow id of the workflow
     * @param contextId define the execution context of workflow
     * @param actionId identify the action to be executed by the workflow(next , pause,resume)
     * @return RequestResponse
     * @throws InternalServerException
     * @throws BadRequestException
     * @throws VitamClientException
     * @throws WorkflowNotFoundException
     */
    RequestResponse<JsonNode> executeOperationProcess(String operationId, String workflow, String contextId,
        String actionId)
        throws InternalServerException, BadRequestException, VitamClientException, WorkflowNotFoundException;

    /**
     * initWorkFlow : init workFlow Process
     * 
     * 
     * @param contextId :define the execution context of workflow
     * @throws VitamClientException
     * @throws VitamException
     */
    void initWorkFlow(String contextId) throws VitamException;

    /**
     * updateVitamProcess : update vitam process status
     * 
     * 
     * @param contextId
     * @param actionId
     * @param container
     * @param workflow
     * @return ItemStatus
     * @throws InternalServerException
     * @throws BadRequestException
     * @throws VitamClientException
     */

    @Deprecated // FIXME clean lors de la 2745
    ItemStatus updateVitamProcess(String contextId, String actionId, String container, String workflow)
        throws InternalServerException, BadRequestException, VitamClientException;

    /**
     * initVitamProcess
     * 
     * @param contextId
     * @param container
     * @param workflow
     * @throws InternalServerException
     * @throws VitamClientException
     * @throws BadRequestException
     */
    void initVitamProcess(String contextId, String container, String workflow)
        throws InternalServerException, VitamClientException, BadRequestException;

    /**
     * Retrieve all the workflow operations
     * 
     * @param query Query model
     * 
     * @return All details of the operations
     * @throws VitamClientException
     */
    RequestResponse<ProcessDetail> listOperationsDetails(ProcessQuery query) throws VitamClientException;


    /**
     * Retrieve all the workflow definitions.
     * 
     * @return workflow definitions
     * @throws VitamClientException
     */
    RequestResponse<WorkFlow> getWorkflowDefinitions() throws VitamClientException;

}
