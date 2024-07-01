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
package fr.gouv.vitam.processing.management.core;

import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.thread.VitamThreadFactory;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.processing.common.exception.ProcessingStorageWorkspaceException;
import fr.gouv.vitam.processing.common.model.ProcessWorkflow;
import fr.gouv.vitam.processing.data.core.management.ProcessDataManagement;
import fr.gouv.vitam.processing.data.core.management.WorkspaceProcessDataManagement;
import fr.gouv.vitam.processing.management.api.ProcessManagement;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * ProcessManagementImpl implementation of ProcessManagement API
 */
public class ProcessWorkFlowsCleaner implements Runnable {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProcessWorkFlowsCleaner.class);

    private final Integer period = VitamConfiguration.getVitamCleanPeriod();
    private LocalDateTime timeLimit;
    private final ProcessManagement processManagement;
    private final WorkspaceClientFactory workspaceClientFactory;

    private final ProcessDataManagement processDataManagement;

    public ProcessWorkFlowsCleaner(ProcessManagement processManagement, TimeUnit timeunit) {
        this(
            processManagement,
            WorkspaceProcessDataManagement.getInstance(),
            WorkspaceClientFactory.getInstance(),
            timeunit
        );
    }

    @VisibleForTesting
    public ProcessWorkFlowsCleaner(
        ProcessManagement processManagement,
        ProcessDataManagement processDataManagement,
        WorkspaceClientFactory workspaceClientFactory,
        TimeUnit timeunit
    ) {
        this.processManagement = processManagement;
        this.processDataManagement = processDataManagement;
        this.workspaceClientFactory = workspaceClientFactory;
        Executors.newScheduledThreadPool(1, VitamThreadFactory.getInstance()).scheduleAtFixedRate(
            this,
            period,
            period,
            timeunit
        );
    }

    @Override
    public void run() {
        timeLimit = LocalDateUtil.now().minusHours(period);
        // One RequestId for all tenant
        VitamThreadUtils.getVitamSession()
            .setRequestId(GUIDFactory.newRequestIdGUID(VitamConfiguration.getAdminTenant()));
        this.cleanProcessingByTenants();
    }

    // clean workflow by tenant
    private void cleanProcessingByTenants() {
        for (Map.Entry<Integer, Map<String, ProcessWorkflow>> entry : this.processManagement.getWorkFlowList()
            .entrySet()) {
            Map<String, ProcessWorkflow> map = entry.getValue();
            if (null != map && map.size() > 0) {
                VitamThreadUtils.getVitamSession().setTenantId(entry.getKey());
                cleanCompletedProcess(entry.getValue());
            }
        }
    }

    //clean workflow list
    private void cleanCompletedProcess(Map<String, ProcessWorkflow> map) {
        for (Map.Entry<String, ProcessWorkflow> element : map.entrySet()) {
            if (isCleanable(element.getValue())) {
                try {
                    processDataManagement.removeOperationContainer(element.getValue(), workspaceClientFactory);
                    processDataManagement.removeProcessWorkflow(
                        VitamConfiguration.getWorkspaceWorkflowsFolder(),
                        element.getKey()
                    );

                    // remove from workFlowList
                    map.remove(element.getKey());

                    // remove from state machine
                    processManagement.getProcessMonitorList().remove(element.getKey());
                } catch (ProcessingStorageWorkspaceException e) {
                    LOGGER.error(
                        "cannot delete workflow file for serverID {} and asyncID {}",
                        VitamConfiguration.getWorkspaceWorkflowsFolder(),
                        element.getKey(),
                        e
                    );
                }
            }
        }
    }

    // Check if the workflow is cleanable
    private boolean isCleanable(ProcessWorkflow workflow) {
        return (
            workflow.getState().equals(ProcessState.COMPLETED) &&
            workflow.getProcessCompletedDate() != null &&
            workflow.getProcessCompletedDate().isBefore(timeLimit)
        );
    }
}
