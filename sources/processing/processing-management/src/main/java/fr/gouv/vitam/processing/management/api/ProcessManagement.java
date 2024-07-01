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
package fr.gouv.vitam.processing.management.api;

import fr.gouv.vitam.common.exception.StateNotAllowedException;
import fr.gouv.vitam.common.lifecycle.ProcessLifeCycle;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessPause;
import fr.gouv.vitam.common.model.ProcessQuery;
import fr.gouv.vitam.common.model.VitamAutoCloseable;
import fr.gouv.vitam.common.model.processing.ProcessDetail;
import fr.gouv.vitam.common.model.processing.WorkFlow;
import fr.gouv.vitam.processing.common.automation.IEventsState;
import fr.gouv.vitam.processing.common.config.ServerConfiguration;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.model.ProcessWorkflow;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;

import java.util.List;
import java.util.Map;

/**
 * ProcessManagement interface
 *
 * This service will be invoked by Ingest Module
 */
public interface ProcessManagement extends ProcessLifeCycle, VitamAutoCloseable {
    /**
     * Init a new process workflow
     *
     * @param workerParameters parameters to be passed to ProcessEngine
     * @param workflowId the workflow identifier
     * @return ProcessWorkflow
     * @throws ProcessingException if the process could not be initialized
     */
    ProcessWorkflow init(WorkerParameters workerParameters, String workflowId) throws ProcessingException;

    /**
     * Handle a next action for the corresponding process workflow
     *
     * @param workerParameters parameters to be passed to ProcessEngine
     * @param tenantId the tenant identifier
     * @return the status
     * @throws ProcessingException if next could not be applied
     * @throws StateNotAllowedException if the process state is incorrect
     */
    ItemStatus next(WorkerParameters workerParameters, Integer tenantId)
        throws ProcessingException, StateNotAllowedException;

    /**
     * Handle a replay action for the corresponding process workflow
     *
     * @param workerParameters parameters to be passed to ProcessEngine
     * @param tenantId the tenant identifier
     * @return the status
     * @throws ProcessingException if replay could not be applied
     * @throws StateNotAllowedException if the process state is incorrect
     */
    ItemStatus replay(WorkerParameters workerParameters, Integer tenantId)
        throws ProcessingException, StateNotAllowedException;

    /**
     * Handle a resume action for the corresponding process workflow
     *
     * @param workerParameters parameters to be passed to ProcessEngine
     * @param tenantId the tenant identifier
     * @param useForcedPause if the forced pause must be applied
     * @return the status
     * @throws ProcessingException if resume could not be applied
     * @throws StateNotAllowedException if the process state is incorrect
     */
    ItemStatus resume(WorkerParameters workerParameters, Integer tenantId, boolean useForcedPause)
        throws ProcessingException, StateNotAllowedException;

    /**
     * Handle a pause action for the corresponding process workflow
     *
     * @param operationId the operation identifier
     * @param tenantId the tenant identifier
     * @return the status
     * @throws ProcessingException if pause could not be applied
     * @throws StateNotAllowedException if the process state is incorrect
     */
    ItemStatus pause(String operationId, Integer tenantId) throws ProcessingException, StateNotAllowedException;

    /**
     * Handle a cancel action for the corresponding process workflow
     *
     * @param operationId the operation identifier
     * @param tenantId the tenant identifier
     * @return the status
     * @throws ProcessingException if cancel could not be applied
     * @throws StateNotAllowedException if the process state is incorrect
     */
    ItemStatus cancel(String operationId, Integer tenantId) throws ProcessingException, StateNotAllowedException;

    /**
     * Retrieve All the workflow process for monitoring purpose The final business scope of this feature is likely to be
     * redefined, to match the future need
     *
     * @param tenantId the tenant identifier
     * @return All the workflow process details
     */
    List<ProcessWorkflow> findAllProcessWorkflow(Integer tenantId);

    /**
     * find the workflow process according to the operation_id and the tenant_id
     *
     * @param operationId the operation identifier
     * @param tenantId the tenant identifier
     * @return the workFlow process
     */
    ProcessWorkflow findOneProcessWorkflow(String operationId, Integer tenantId);

    /**
     * Retrieve the loaded workflow definitions
     *
     * @return the workflow definitions by ID
     */
    Map<String, WorkFlow> getWorkflowDefinitions();

    /**
     * Reload workflow definitions
     */
    void reloadWorkflowDefinitions();

    /**
     * Get filtered process workflow
     *
     * @param query to filter
     * @param tenantId the tenandId
     * @return filtered process list
     */
    List<ProcessDetail> getFilteredProcess(ProcessQuery query, Integer tenantId);

    /**
     * @return WorkFlow List
     */
    Map<Integer, Map<String, ProcessWorkflow>> getWorkFlowList();

    /**
     * sProcessMonitorList
     *
     * @return
     */
    Map<String, IEventsState> getProcessMonitorList();

    /**
     * server configuration
     *
     * @return
     */
    ServerConfiguration getConfiguration();

    /**
     * Removed the forced pause on the tenant and/or the type of process
     *
     * @param pause
     */
    void removeForcePause(ProcessPause pause) throws ProcessingException;

    /**
     * Add a forced pause on the tenant and/or the type of process
     *
     * @param pause
     */
    void forcePause(ProcessPause pause) throws ProcessingException;
}
