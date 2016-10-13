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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.model.ProcessStep;
import fr.gouv.vitam.processing.common.model.StatusCode;
import fr.gouv.vitam.processing.common.model.Step;
import fr.gouv.vitam.processing.common.model.WorkFlow;

/**
 * ProcessMonitoringImpl class implementing the ProcessMonitoring and using a concurrent HashMap to persist objects
 */
public class ProcessMonitoringImpl implements ProcessMonitoring {

    private static final Map<String, Map<String, ProcessStep>> WORKFLOWS_LIST = new ConcurrentHashMap<>();

    private static final ProcessMonitoringImpl INSTANCE = new ProcessMonitoringImpl();

    private static final String PROCESS_DOES_NOT_EXIST = "Process does not exist";
    private static final String STEP_DOES_NOT_EXIST = "Step does not exist";

    private ProcessMonitoringImpl() {
        // doNothing
    }


    /**
     * 
     * Get the Process Monitoring instance
     *
     * @return the ProcessMonitoring instance
     */
    // TODO : Probably we should use a factory
    public static ProcessMonitoringImpl getInstance() {
        return INSTANCE;
    }

    @Override
    public Map<String, ProcessStep> initOrderedWorkflow(String processId, WorkFlow workflow, String containerName)
        throws IllegalArgumentException {
        Map<String, ProcessStep> orderedWorkflow = new LinkedHashMap<>();
        String uniqueId;
        int iterator = 0;
        for (final Step step : workflow.getSteps()) {
            ProcessStep processStep = new ProcessStep(step, 0, 0);
            uniqueId = containerName + "_" + workflow.getId() + "_" + iterator++ + "_" + step.getStepName();
            orderedWorkflow.put(uniqueId, processStep);
        }
        WORKFLOWS_LIST.put(processId, orderedWorkflow);
        return orderedWorkflow;
    }

    @Override
    public void updateStep(String processId, String uniqueId, long elementToProcess, boolean elementProcessed)
        throws ProcessingException {
        if (WORKFLOWS_LIST.containsKey(processId)) {
            Map<String, ProcessStep> orderedSteps = WORKFLOWS_LIST.get(processId);
            if (orderedSteps.containsKey(uniqueId)) {
                ProcessStep step = orderedSteps.get(uniqueId);
                if (elementProcessed) {
                    step.setElementProcessed(step.getElementProcessed() + 1);
                } else {
                    step.setElementToProcess(elementToProcess);
                }
                orderedSteps.put(uniqueId, step);
                WORKFLOWS_LIST.put(processId, orderedSteps);
            } else {
                throw new ProcessingException(STEP_DOES_NOT_EXIST);
            }

        } else {
            throw new ProcessingException(PROCESS_DOES_NOT_EXIST);
        }
    }

    @Override
    public Map<String, ProcessStep> getWorkflowStatus(String processId) throws ProcessingException {
        if (WORKFLOWS_LIST.containsKey(processId)) {
            return WORKFLOWS_LIST.get(processId);
        } else {
            throw new ProcessingException(PROCESS_DOES_NOT_EXIST);
        }
    }

    @Override
    public void updateStepStatus(String processId, String uniqueId, StatusCode status) throws ProcessingException {
        if (WORKFLOWS_LIST.containsKey(processId)) {
            Map<String, ProcessStep> orderedSteps = WORKFLOWS_LIST.get(processId);
            if (orderedSteps.containsKey(uniqueId)) {
                ProcessStep step = orderedSteps.get(uniqueId);
                step.setStepStatusCode(status);
                orderedSteps.put(uniqueId, step);
                WORKFLOWS_LIST.put(processId, orderedSteps);
            } else {
                throw new ProcessingException(STEP_DOES_NOT_EXIST);
            }
        } else {
            throw new ProcessingException(PROCESS_DOES_NOT_EXIST);
        }
    }

}
