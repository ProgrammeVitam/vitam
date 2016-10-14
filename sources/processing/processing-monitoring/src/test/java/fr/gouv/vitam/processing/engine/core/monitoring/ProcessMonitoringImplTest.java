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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.model.ProcessStep;
import fr.gouv.vitam.processing.common.model.Step;
import fr.gouv.vitam.processing.common.model.WorkFlow;

public class ProcessMonitoringImplTest {

    @Test
    public void processMonitoringGetInstanceOK() {
        final ProcessMonitoringImpl processMonitoring = ProcessMonitoringImpl.getInstance();
        assertNotNull(processMonitoring);
        final ProcessMonitoringImpl processMonitoring2 = ProcessMonitoringImpl.getInstance();
        assertTrue(processMonitoring.equals(processMonitoring2));
    }

    @Test(expected = ProcessingException.class)
    public void processMonitoringGetWorkflowStatusUnknownThrowsException() throws Exception {
        final ProcessMonitoringImpl processMonitoring = ProcessMonitoringImpl.getInstance();
        processMonitoring.getWorkflowStatus("UNKNOWN_PROCESS_ID");
    }

    @Test
    public void processMonitoringInitEmptyWorkflowOK() {
        final ProcessMonitoringImpl processMonitoring = ProcessMonitoringImpl.getInstance();
        assertNotNull(processMonitoring.initOrderedWorkflow("processId", new WorkFlow(), "containerName"));
    }

    @Test
    public void processMonitoringGetWorkflowStatusKnownThenOK() throws Exception {
        final ProcessMonitoringImpl processMonitoring = ProcessMonitoringImpl.getInstance();
        assertNotNull(processMonitoring.initOrderedWorkflow("EXISTING_PROCESS_ID", new WorkFlow(), "containerName"));
        assertNotNull(processMonitoring.getWorkflowStatus("EXISTING_PROCESS_ID"));
    }

    @Test
    public void processMonitoringInitSimpleWorkflowOK() {
        final ProcessMonitoringImpl processMonitoring = ProcessMonitoringImpl.getInstance();
        final Map<String, ProcessStep> initMap =
            processMonitoring.initOrderedWorkflow("processId", initSimpleWorkflow(), "containerName");
        assertEquals(initMap.size(), 1);
        assertNotNull(initMap.get("containerName_wf1_0_step1"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void processMonitoringInitSimpleWorkflowException() {
        final ProcessMonitoringImpl processMonitoring = ProcessMonitoringImpl.getInstance();
        final Map<String, ProcessStep> initMap =
            processMonitoring.initOrderedWorkflow("processId", initSimpleWorkflowWithNullStep(), "containerName");
    }

    @Test(expected = ProcessingException.class)
    public void processMonitoringUpdateSimpleWorkflowUnknownProcessThrowsException() throws Exception {
        final ProcessMonitoringImpl processMonitoring = ProcessMonitoringImpl.getInstance();
        processMonitoring.updateStep("UNKNOWN_PROCESS_ID", "UNIQUE_ID", 10, false);
    }

    @Test(expected = ProcessingException.class)
    public void processMonitoringUpdateSimpleWorkflowUnknownStepThrowsException() throws Exception {
        final ProcessMonitoringImpl processMonitoring = ProcessMonitoringImpl.getInstance();
        processMonitoring.initOrderedWorkflow("EXISTING_PROCESS_ID", new WorkFlow(), "containerName");
        processMonitoring.updateStep("EXISTING_PROCESS_ID", "UNKNOWN_UNIQUE_STEP_ID", 10, false);
    }

    @Test(expected = ProcessingException.class)
    public void processMonitoringUpdateStatusSimpleWorkflowUnknownProcessThrowsException() throws Exception {
        final ProcessMonitoringImpl processMonitoring = ProcessMonitoringImpl.getInstance();
        processMonitoring.updateStepStatus("UNKNOWN_PROCESS_ID", "UNIQUE_ID", StatusCode.OK);
    }

    @Test(expected = ProcessingException.class)
    public void processMonitoringUpdateStatusSimpleWorkflowUnknownStepThrowsException() throws Exception {
        final ProcessMonitoringImpl processMonitoring = ProcessMonitoringImpl.getInstance();
        processMonitoring.updateStepStatus("EXISTING_PROCESS_ID", "UNKNOWN_UNIQUE_STEP_ID", StatusCode.OK);
    }

    @Test
    public void processMonitoringUpdateSimpleWorkflowKnownOK() throws Exception {
        final ProcessMonitoringImpl processMonitoring = ProcessMonitoringImpl.getInstance();
        assertNotNull(processMonitoring.initOrderedWorkflow("EXISTING_PROCESS_ID_FOR_UPDATE", initSimpleWorkflow(),
            "containerName"));
        processMonitoring.updateStep("EXISTING_PROCESS_ID_FOR_UPDATE", "containerName_wf1_0_step1", 10, false);
        Map<String, ProcessStep> orderedSteps = processMonitoring.getWorkflowStatus("EXISTING_PROCESS_ID_FOR_UPDATE");
        ProcessStep pStep = orderedSteps.get("containerName_wf1_0_step1");
        assertEquals(10, pStep.getElementToProcess());

        processMonitoring.updateStep("EXISTING_PROCESS_ID_FOR_UPDATE", "containerName_wf1_0_step1", 0, true);
        orderedSteps = processMonitoring.getWorkflowStatus("EXISTING_PROCESS_ID_FOR_UPDATE");
        pStep = orderedSteps.get("containerName_wf1_0_step1");
        assertEquals(10, pStep.getElementToProcess());
        assertEquals(1, pStep.getElementProcessed());

        processMonitoring.updateStepStatus("EXISTING_PROCESS_ID_FOR_UPDATE", "containerName_wf1_0_step1",
            StatusCode.OK);
        orderedSteps = processMonitoring.getWorkflowStatus("EXISTING_PROCESS_ID_FOR_UPDATE");
        pStep = orderedSteps.get("containerName_wf1_0_step1");
        assertEquals(StatusCode.OK, pStep.getStepStatusCode());
    }

    private WorkFlow initSimpleWorkflow() {
        final WorkFlow simpleWorkflow = new WorkFlow();
        simpleWorkflow.setId("wf1");
        final Step step1 = new Step();
        step1.setStepName("step1");
        final List<Step> steps = new ArrayList();
        steps.add(step1);
        simpleWorkflow.setSteps(steps);
        return simpleWorkflow;
    }

    private WorkFlow initSimpleWorkflowWithNullStep() {
        final WorkFlow simpleWorkflow = new WorkFlow();
        simpleWorkflow.setId("wf1");
        final List<Step> steps = new ArrayList();
        steps.add(null);
        simpleWorkflow.setSteps(steps);
        return simpleWorkflow;
    }

}
