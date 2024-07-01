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

import fr.gouv.vitam.common.exception.WorkflowNotFoundException;
import fr.gouv.vitam.common.model.processing.WorkFlow;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.processing.common.model.ProcessWorkflow;

import java.util.List;
import java.util.Map;

/**
 * Process Data Access Interface offers services
 */
public interface ProcessDataAccess {
    /**
     * Allows a process to be initialized
     *
     * @param workflow the workflow to init
     * @param containerName : null not allowed , the name of the container to be processed
     * @return {@link LogbookTypeProcess}
     */
    ProcessWorkflow initProcessWorkflow(WorkFlow workflow, String containerName);

    /**
     * Gets Process Workflow by ID
     *
     * @param processId the process id
     * @param tenantId the working tenant
     * @return {@link ProcessWorkflow}
     * @throws WorkflowNotFoundException thrown when process workflow not found
     */

    ProcessWorkflow findOneProcessWorkflow(String processId, Integer tenantId) throws WorkflowNotFoundException;

    /**
     * Retrieves All the workflow process for monitoring purpose The final business scope of this feature is likely to
     * be redefined, to match the future need
     *
     * @param tenantId the working tenant
     * @return All the workflow process details
     */
    List<ProcessWorkflow> findAllProcessWorkflow(Integer tenantId);

    /**
     * Add process to Workflow<br />
     * Only use on application starting to load persisted workflow (state are PAUSED or FAILED only)
     *
     * @param processWorkflow the loaded persisted process to add
     */
    void addToWorkflowList(ProcessWorkflow processWorkflow);

    /**
     * getter of WorkflowList
     *
     * @return
     */
    Map<Integer, Map<String, ProcessWorkflow>> getWorkFlowList();
}
