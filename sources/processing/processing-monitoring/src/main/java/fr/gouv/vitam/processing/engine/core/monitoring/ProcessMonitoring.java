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
package fr.gouv.vitam.processing.engine.core.monitoring;

import java.util.Map;

import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.model.ProcessStep;
import fr.gouv.vitam.processing.common.model.WorkFlow;

/**
 * Process Monitoring Interface offers services in order to monitor workflows
 */
// TODO : propose a method that could purge the workflows
public interface ProcessMonitoring {

    /**
     * Allows a process to be initiated
     *
     * @param processId the id of the process to be initiated
     * @param workflow the workflow to init
     * @param containerName the name of the container to be processed
     * @return a Map, a generated unique Id as a key, and as a value the step
     * @throws IllegalArgumentException if a step is null
     */
    Map<String, ProcessStep> initOrderedWorkflow(String processId, WorkFlow workflow, String containerName)
        throws IllegalArgumentException;

    /**
     * Update a step status in a workflow, knowing its unique id
     *
     * @param processId the id of the process to be updated
     * @param uniqueId the unique Id of the step
     * @param status the Code of the status
     * @throws ProcessingException if the step does not exist
     */
    void updateStepStatus(String processId, String uniqueId, StatusCode status) throws ProcessingException;

    /**
     * Update a step in a workflow, knowing its unique id
     *
     * @param processId the id of the process to be updated
     * @param uniqueId the unique Id of the step
     * @param elementToProcess the number of element to be processed
     * @param elementProcessed if a new element has been processed
     * @throws ProcessingException if the step does not exist
     */
    void updateStep(String processId, String uniqueId, long elementToProcess, boolean elementProcessed)
        throws ProcessingException;

    /**
     * Get workflow status with its workflow id If the workflow id does not exist, an empty Map is returned
     *
     * @param processId the id of the process
     * @return a Map, containerName as a key, a map of steps as the value (the value is the map created in the
     *         initOrderedWorkflow method)
     * @throws ProcessingException if the process does not exist
     */
    Map<String, ProcessStep> getWorkflowStatus(String processId) throws ProcessingException;

}
