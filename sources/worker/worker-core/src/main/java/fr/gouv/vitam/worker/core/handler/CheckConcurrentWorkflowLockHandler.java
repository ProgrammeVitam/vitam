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
package fr.gouv.vitam.worker.core.handler;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.processing.ProcessDetail;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.utils.LightweightWorkflowLock;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;

/**
 * Elimination lock check handler.
 */
public class CheckConcurrentWorkflowLockHandler extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CheckConcurrentWorkflowLockHandler.class);

    private static final String CHECK_CONCURRENT_WORKFLOW_LOCK = "CHECK_CONCURRENT_WORKFLOW_LOCK";
    private static final int WORKFLOW_IDS_RANK = 0;
    static final String CONCURRENT_PROCESSES_FOUND = "Concurrent process(es) found";
    private final LightweightWorkflowLock lightweightWorkflowLock;

    /**
     * Default constructor
     */
    public CheckConcurrentWorkflowLockHandler() {
        this(new LightweightWorkflowLock());
    }

    /***
     * Test only constructor
     */
    @VisibleForTesting
    CheckConcurrentWorkflowLockHandler(LightweightWorkflowLock lightweightWorkflowLock) {
        this.lightweightWorkflowLock = lightweightWorkflowLock;
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler) throws ProcessingException {
        try {
            String workflowIdsStr = (String) handler.getInput(WORKFLOW_IDS_RANK);
            List<String> workflowIds = Arrays.stream(workflowIdsStr.split(","))
                .map(String::trim)
                .collect(Collectors.toList());

            List<ProcessDetail> concurrentWorkflows = lightweightWorkflowLock.listConcurrentWorkflows(
                workflowIds,
                param.getContainerName()
            );

            if (!concurrentWorkflows.isEmpty()) {
                LOGGER.error(
                    "Concurrent process(es) found " +
                    concurrentWorkflows
                        .stream()
                        .map(
                            i ->
                                i.getProcessType() +
                                " " +
                                i.getOperationId() +
                                "(" +
                                i.getGlobalState() +
                                "/" +
                                i.getStepStatus() +
                                ")"
                        )
                        .collect(Collectors.joining(", ", "[", "]"))
                );

                ObjectNode eventDetails = JsonHandler.createObjectNode();
                eventDetails.put("error", CONCURRENT_PROCESSES_FOUND);
                return buildItemStatus(CHECK_CONCURRENT_WORKFLOW_LOCK, StatusCode.KO, eventDetails);
            }
        } catch (VitamClientException e) {
            LOGGER.error("Concurrent workflow lock check failed", e);
            return buildItemStatus(CHECK_CONCURRENT_WORKFLOW_LOCK, StatusCode.FATAL, null);
        }

        LOGGER.info("Concurrent workflow lock check succeeded");
        return buildItemStatus(CHECK_CONCURRENT_WORKFLOW_LOCK, StatusCode.OK, null);
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // NOP.
    }

    public static String getId() {
        return CHECK_CONCURRENT_WORKFLOW_LOCK;
    }
}
