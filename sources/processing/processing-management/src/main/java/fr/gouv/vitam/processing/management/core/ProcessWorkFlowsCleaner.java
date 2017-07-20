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
package fr.gouv.vitam.processing.management.core;

import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.ServerIdentity;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.processing.common.model.ProcessWorkflow;
import fr.gouv.vitam.processing.data.core.management.ProcessDataManagement;
import fr.gouv.vitam.processing.data.core.management.WorkspaceProcessDataManagement;
import fr.gouv.vitam.processing.management.api.ProcessManagement;

import java.time.LocalDateTime;
import java.time.Period;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * ProcessManagementImpl implementation of ProcessManagement API
 */
public class ProcessWorkFlowsCleaner implements Runnable {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProcessWorkFlowsCleaner.class);

    private Integer period = VitamConfiguration.getVitamCleanPeriod();
    private LocalDateTime timeLimit;
    final private ProcessManagement processManagement;

    private TimeUnit timeUnit;

    public ProcessWorkFlowsCleaner(ProcessManagement processManagement, TimeUnit timeunit) {
        this.timeUnit = timeunit;
        this.processManagement = processManagement;
        processDataManagement = WorkspaceProcessDataManagement.getInstance();
        Executors
            .newScheduledThreadPool(1).scheduleAtFixedRate(this, period, period, timeUnit);
    }


    @Override public void run() {
        timeLimit = LocalDateTime.now().minusHours(period);
        this.cleanProcessingByTenants();
    }

    ProcessDataManagement processDataManagement;

    // clean workflow by teneant
    private void cleanProcessingByTenants() {
        for (Map.Entry<Integer, Map<String, ProcessWorkflow>> entry : this.processManagement.getWorkFlowList()
            .entrySet()) {
            Map<String, ProcessWorkflow> map = entry.getValue();
            if (null != map && map.size() > 0) {
                cleanCompletedProcess(entry.getValue());
            }
        }
    }

    //clean workflow list
    private void cleanCompletedProcess(Map<String, ProcessWorkflow> map) {
        for (Map.Entry<String, ProcessWorkflow> element : map.entrySet()) {
            if (isCleaneable(element.getValue())) {
                try {
                    processDataManagement
                        .removeProcessWorkflow(String.valueOf(ServerIdentity.getInstance().getServerId()),
                            element.getKey().toString());
                } catch (Exception e) {
                    LOGGER.error("cannot delete workflow file for serverID {} and asyncID {}", String.valueOf
                        (ServerIdentity.getInstance().getServerId()), element.getKey().toString(), e);
                }
                /**
                 *remove from workFlowList
                 */
                map.remove(element.getKey());
                //remove from state machine
                processManagement.getProcessMonitorList().remove(element.getKey());
            }
        }


    }

    // check if the workflow is clennable
    private boolean isCleaneable(ProcessWorkflow workflow) {
        return workflow.getState().equals(ProcessState.COMPLETED)
            && workflow.getProcessCompletedDate() != null &&
            workflow.getProcessCompletedDate().isBefore(timeLimit);

    }

}
