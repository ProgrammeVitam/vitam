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
package fr.gouv.vitam.processing.data.core;

import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.exception.WorkflowNotFoundException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.processing.Step;
import fr.gouv.vitam.common.model.processing.WorkFlow;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.processing.common.model.ProcessStep;
import fr.gouv.vitam.processing.common.model.ProcessWorkflow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ProcessMonitoringImpl class implementing the ProcessMonitoring Persists processWorkflow object (to json) at each step
 * in process/<serverId> name as <operationID>.json Remove this file at the end (completed, failed state)
 */
public class ProcessDataAccessImpl implements ProcessDataAccess {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProcessDataAccessImpl.class);

    private static final Map<Integer, Map<String, ProcessWorkflow>> WORKFLOWS_LIST = new ConcurrentHashMap<>();

    private static final ProcessDataAccessImpl INSTANCE = new ProcessDataAccessImpl();

    private static final String PROCESS_DOES_NOT_EXIST = "Process does not exist";

    private ProcessDataAccessImpl() {
        // doNothing
    }

    /**
     * Get the Process Monitoring instance
     *
     * @return the ProcessMonitoring instance
     */
    public static ProcessDataAccessImpl getInstance() {
        return INSTANCE;
    }

    @Override
    public ProcessWorkflow initProcessWorkflow(WorkFlow workflow, String containerName) {
        ParametersChecker.checkParameter("containerName is a mandatory parameter", containerName);
        ParametersChecker.checkParameter("workflow is a mandatory parameter", workflow);
        Integer tenantId = VitamThreadUtils.getVitamSession().getTenantId();
        String contextId = VitamThreadUtils.getVitamSession().getContextId();
        String applicationId = VitamThreadUtils.getVitamSession().getApplicationSessionId();

        final ProcessWorkflow pwkf = new ProcessWorkflow();
        pwkf.setLogbookTypeProcess(LogbookTypeProcess.valueOf(workflow.getTypeProc()));
        pwkf.setOperationId(containerName);
        pwkf.setTenantId(tenantId);
        pwkf.setContextId(contextId);
        pwkf.setApplicationId(applicationId);

        int cpt = 0;
        for (final Step step : workflow.getSteps()) {
            final String uniqueId = containerName + "_" + workflow.getId() + "_" + cpt + "_" + step.getStepName();
            step.setId(uniqueId);
            pwkf
                .getSteps()
                .add(new ProcessStep(step, containerName, workflow.getId(), cpt, new AtomicLong(0), new AtomicLong(0)));
            cpt++;
        }

        addToWorkflowList(pwkf);
        return pwkf;
    }

    @Override
    public ProcessWorkflow findOneProcessWorkflow(String processId, Integer tenantId) throws WorkflowNotFoundException {
        ParametersChecker.checkParameter("processId is a mandatory parameter", processId);
        ParametersChecker.checkParameter("tenantId is a mandatory parameter", tenantId);

        if (
            !WORKFLOWS_LIST.containsKey(tenantId) ||
            WORKFLOWS_LIST.get(tenantId) == null ||
            !WORKFLOWS_LIST.get(tenantId).containsKey(processId)
        ) {
            throw new WorkflowNotFoundException(
                PROCESS_DOES_NOT_EXIST +
                " > Tenant (" +
                tenantId +
                ")" +
                ". Process (" +
                processId +
                ") map = " +
                WORKFLOWS_LIST.keySet()
            );
        } else {
            return WORKFLOWS_LIST.get(tenantId).get(processId);
        }
    }

    @VisibleForTesting
    public void clearWorkflow() {
        WORKFLOWS_LIST.clear();
    }

    @Override
    public List<ProcessWorkflow> findAllProcessWorkflow(Integer tenantId) {
        return new ArrayList<>(WORKFLOWS_LIST.getOrDefault(tenantId, new HashMap<>()).values());
    }

    @Override
    public void addToWorkflowList(ProcessWorkflow processWorkflow) {
        LOGGER.info(
            "add workflow with processId: {} and tenant {}",
            processWorkflow.getOperationId(),
            processWorkflow.getTenantId()
        );

        WORKFLOWS_LIST.computeIfAbsent(processWorkflow.getTenantId(), key -> new ConcurrentHashMap<>()).put(
            processWorkflow.getOperationId(),
            processWorkflow
        );
    }

    @Override
    public Map<Integer, Map<String, ProcessWorkflow>> getWorkFlowList() {
        return WORKFLOWS_LIST;
    }
}
