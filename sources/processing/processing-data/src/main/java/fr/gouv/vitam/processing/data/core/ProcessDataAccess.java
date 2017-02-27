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
package fr.gouv.vitam.processing.data.core;

import java.util.List;
import java.util.Map;

import fr.gouv.vitam.common.exception.WorkflowNotFoundException;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.ProcessExecutionStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.exception.StepsNotFoundException;
import fr.gouv.vitam.processing.common.model.ProcessStep;
import fr.gouv.vitam.processing.common.model.ProcessWorkflow;
import fr.gouv.vitam.processing.common.model.WorkFlow;

/**
 * Process Data Access Interface offers services
 */
public interface ProcessDataAccess {

    /**
     * Allows a process to be initialized
     * 
     * @param workflow the workflow to init
     * @param containerName : null not allowed , the name of the container to be processed
     * @param executionMode {@link ProcessAction}
     */
    ProcessWorkflow initProcessWorkflow(WorkFlow workflow, String containerName, ProcessAction executionMode,
        LogbookTypeProcess logbookTypeProcess, Integer tenantId);

    /**
     * Updates a step status in a workflow, knowing its unique id
     *
     * @param processId the id of the process to be updated
     * @param uniqueId the step with unique Id
     * @param status the Code of the status
     * @throws ProcessingException if the step does not exist
     */
    void updateStepStatus(String processId, String uniqueId, StatusCode status, Integer tenantId)
        throws ProcessingException;

    /**
     * Update a step in a workflow, knowing its unique id
     *
     * @param processId the id of the process to be updated
     * @param uniqueId the unique Id of the step
     * @param elementToProcess the number of element to be processed
     * @param elementProcessed if a new element has been processed
     * @throws ProcessingException if the step does not exist
     */
    void updateStep(String processId, String uniqueId, long elementToProcess, boolean elementProcessed,
        Integer tenantId)
        throws ProcessingException;



    /**
     * Gets process steps by processId
     * 
     * @param processId is operation id
     * @return map of process step
     * @throws StepsNotFoundException will be thrown when steps not found
     * @throws WorkflowNotFoundException
     */

    public Map<String, ProcessStep> getWorkflowProcessSteps(String processId, Integer tenantId)
        throws StepsNotFoundException, WorkflowNotFoundException;

    /**
     * Returns true if at least one of the step status is KO or FATAL.
     *
     * @param operationId the id of the workflow
     * @return true if at least one of the step status is KO or FATAL, else false
     * @throws ProcessingException if the process does not exist
     */
    StatusCode getFinalWorkflowStatus(String operationId, Integer tenantId) throws ProcessingException;

    /**
     * Cancels Process Workflow by operation identifier
     * 
     * @param operationId the id of the process to delete
     * @throws WorkflowNotFoundException thrown when process workflow not found
     * @throws ProcessingException thrown when process workflow not found
     */
    ProcessWorkflow cancelProcessWorkflow(String operationId, Integer tenantId)
        throws WorkflowNotFoundException, ProcessingException;

    /**
     * clears map of Process Workflows by status and number of object will be cleared
     * 
     * @param statusCode
     * @param number of the objects will be cleared
     */
    void clear(StatusCode statusCode, int number, Integer tenantId);


    /**
     * 
     * Returns workflow id (workflow identifier)
     * 
     * @param operationId is a container name in workparams (and operation id)
     * @return workflow id
     * @throws WorkflowNotFoundException thrown when process workflow not found
     */
    String getWorkflowIdByProcessId(String operationId, Integer tenantId) throws WorkflowNotFoundException;


    /**
     * Gets nextStep will be executed in process engine (after check workflow execution status) <br>
     * if the process workflow is finished or is failed then will return null
     * 
     * @param operationId
     * @return Process step or null if workflow finished, paused, failed or canceled {@link ProcessExecutionStatus}
     * @throws StepsNotFoundException
     * @throws WorkflowNotFoundException thrown when process workflow not found
     */
    ProcessStep nextStep(String operationId, Integer tenantId)
        throws StepsNotFoundException, WorkflowNotFoundException;

    /**
     * Updates Process workflow by Process id
     * 
     * @param operationId is an operation or a containerName parameter
     * @param executionStatus
     * @throws WorkflowNotFoundException thrown when process workflow not found
     */
    void updateProcessExecutionStatus(String operationId, ProcessExecutionStatus executionStatus, Integer tenantId)
        throws WorkflowNotFoundException;

    /**
     * Gets Process Workflow by ID
     * 
     * @param operationId : the process identifier
     * @return {@link ProcessWorkflow}
     * @throws WorkflowNotFoundException thrown when process workflow not found
     */

    ProcessWorkflow getProcessWorkflow(String processId, Integer tenantId) throws WorkflowNotFoundException;

    /**
     * Returns finally step
     * 
     * @param operationId : the process identifier
     * @return {@link ProcessStep}
     * @throws WorkflowNotFoundException thrown when process workflow not found
     * @throws StepsNotFoundException
     */

    ProcessStep getFinallyStep(String operationId, Integer tenantId)
        throws StepsNotFoundException, WorkflowNotFoundException;


    /**
     * Gets current Process Workflow execution status by operation Id
     * 
     * @param operationId : the process identifier <br>
     *        null not allowed
     * @return {@link ProcessExecutionStatus} :
     * @throws WorkflowNotFoundException thrown when process workflow not found
     */
    ProcessExecutionStatus getProcessExecutionStatus(String operationId, Integer tenantId)
        throws WorkflowNotFoundException;

    /**
     * Retrieves All the workflow process for monitoring purpose The final business scope of this feature is likely to
     * be redefined, to match the future need
     * 
     * @return All the workflow process details
     */
    List<ProcessWorkflow> getAllWorkflowProcess(Integer tenantId);

    /**
     * Prepares process workflow will be launching
     * 
     * @param operationId
     * @param executionMode
     * @throws ProcessingException
     */
    void prepareToRelaunch(String operationId, ProcessAction executionMode, Integer tenantId)
        throws ProcessingException;

    /**
     * updates message identifier by operation id
     * 
     * @param operationId
     * @param messageIdentifier
     */
    void updateMessageIdentifier(String operationId, String messageIdentifier, Integer tenantId);


    /**
     * Returns messageIdentifier by operation id
     * 
     * @param operationId
     * @return messageIdentifier
     */
    String getMessageIdentifierByOperationId(String operationId, Integer tenantId);

}
