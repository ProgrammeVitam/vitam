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
package fr.gouv.vitam.worker.core.utils;

import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.model.ProcessQuery;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.processing.ProcessDetail;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Helper class for checking concurrent non completed (running / paused) workflows.
 *
 * Provides lightweight lock implementation (at most one process can run in parallel).
 */
public class LightweightWorkflowLock {

    private final ProcessingManagementClientFactory processingManagementClientFactory;

    /**
     * Default constructor
     */
    public LightweightWorkflowLock() {
        this(ProcessingManagementClientFactory.getInstance());
    }

    /***
     * Test only constructor
     */
    @VisibleForTesting
    LightweightWorkflowLock(ProcessingManagementClientFactory processingManagementClientFactory) {
        this.processingManagementClientFactory = processingManagementClientFactory;
    }

    /**
     * Returns all concurrent non completed (running / paused) workflows.
     *
     * @param workflowIds the workflow Ids to check
     * @param currentProcessId the current process id (filtred from result)
     * @return the list of concurrent workflows if any, or an empty list if no concurrent workflow is found.
     */
    public List<ProcessDetail> listConcurrentWorkflows(List<String> workflowIds, String currentProcessId)
        throws VitamClientException {
        try (ProcessingManagementClient client = processingManagementClientFactory.getClient()) {
            ProcessQuery query = new ProcessQuery();
            // Non completed processes
            query.setStates(
                Arrays.stream(ProcessState.values())
                    .filter(state -> state.compareTo(ProcessState.COMPLETED) < 0)
                    .map(Enum::name)
                    .collect(Collectors.toList())
            );
            // Workflow id
            query.setWorkflows(workflowIds);

            RequestResponse<ProcessDetail> processDetailRequestResponse = client.listOperationsDetails(query);
            if (!processDetailRequestResponse.isOk()) {
                VitamError error = (VitamError) processDetailRequestResponse;
                throw new VitamClientException(
                    "Could not check concurrent workflows " + error.getDescription() + " - " + error.getMessage()
                );
            }

            List<ProcessDetail> processDetails =
                ((RequestResponseOK<ProcessDetail>) processDetailRequestResponse).getResults();

            return processDetails
                .stream()
                .filter(processDetail -> !currentProcessId.equals(processDetail.getOperationId()))
                .collect(Collectors.toList());
        }
    }
}
