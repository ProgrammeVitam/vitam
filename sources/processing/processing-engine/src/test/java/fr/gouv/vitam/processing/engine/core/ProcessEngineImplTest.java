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
package fr.gouv.vitam.processing.engine.core;

import static org.junit.Assert.assertNotNull;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.processing.common.exception.WorkflowNotFoundException;
import fr.gouv.vitam.processing.common.model.ProcessStep;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.processing.distributor.api.ProcessDistributor;
import fr.gouv.vitam.processing.engine.core.monitoring.ProcessMonitoringImpl;

/**
 * Do not forget init method on test method !
 */
public class ProcessEngineImplTest {
    private ProcessEngineImpl processEngine;
    private WorkerParameters workParams;
    private ItemStatus response;
    private ProcessMonitoringImpl processMonitoring;
    private ProcessDistributor processDistributor;

    @Before
    public void init() throws WorkflowNotFoundException {
        workParams = WorkerParametersFactory.newWorkerParameters();
        workParams.setWorkerGUID(GUIDFactory.newGUID()).setUrlMetadata("http://localhost:8083").setUrlWorkspace("http://localhost:8083")
            .setContainerName(GUIDFactory.newGUID().getId());

        processDistributor = Mockito.mock(ProcessDistributor.class);
        processEngine = new ProcessEngineImplFactory().create(processDistributor);
        processEngine.setWorkflow("workflowJSONv1");
        processEngine.setWorkflow("workflowJSONFinallyStep");
        processMonitoring = ProcessMonitoringImpl.getInstance();
    }

    @Test
    public void processEngineTest() throws Exception {
        response = processEngine.startWorkflow(workParams, "workflowJSONv1");
        assertNotNull(response);
        String processId = workParams.getProcessId();
        Map<String, ProcessStep> list = processMonitoring.getWorkflowStatus(processId);
        assertNotNull(list);
    }

    @Test
    public void processEngineTestWithFinallyStep() throws Exception {
        final ItemStatus responses = new ItemStatus("stepName");
        responses.increment(StatusCode.KO);
        Mockito.when(processDistributor.distribute(Mockito.anyObject(), Mockito.anyObject(),
            Mockito.eq("workflowJSONFinallyStep"))).thenReturn(responses);

        response = processEngine.startWorkflow(workParams, "workflowJSONFinallyStep");
        assertNotNull(response);
        final String processId = workParams.getProcessId();
        Map<String, ProcessStep> map = processMonitoring.getWorkflowStatus(processId);
        assertNotNull(map);
    }

    @Test(expected = WorkflowNotFoundException.class)
    public void givenWorkFlowIdasNullThenReturnNotFoundException() throws Exception {
        processEngine.startWorkflow(workParams, "notExist");
    }

}
