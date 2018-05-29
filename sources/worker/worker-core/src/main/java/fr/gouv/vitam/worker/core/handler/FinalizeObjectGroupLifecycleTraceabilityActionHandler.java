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
package fr.gouv.vitam.worker.core.handler;

import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.logbook.common.exception.TraceabilityException;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.model.LogbookLifeCycleTraceabilityHelper;
import fr.gouv.vitam.worker.model.LogbookObjectGroupLifeCycleTraceabilityHelper;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

/**
 * FinalizeUnitLifecycleTraceabilityActionHandler Plugin
 */
public class FinalizeObjectGroupLifecycleTraceabilityActionHandler extends FinalizeLifecycleTraceabilityActionHandler {
    private static final VitamLogger LOGGER =
        VitamLoggerFactory.getInstance(FinalizeObjectGroupLifecycleTraceabilityActionHandler.class);

    private static final String HANDLER_ID = "FINALIZE_OG_LFC_TRACEABILITY";
    private final LogbookOperationsClientFactory logbookOperationsClientFactory;
    private final WorkspaceClientFactory workspaceClientFactory;

    /**
     * Empty constructor
     */
    public FinalizeObjectGroupLifecycleTraceabilityActionHandler() {
        this(LogbookOperationsClientFactory.getInstance(),
            WorkspaceClientFactory.getInstance());
    }

    /**
     * Constructor for testing
     */
    @VisibleForTesting
    FinalizeObjectGroupLifecycleTraceabilityActionHandler(LogbookOperationsClientFactory logbookOperationsClientFactory,
        WorkspaceClientFactory workspaceClientFactory) {
        this.logbookOperationsClientFactory = logbookOperationsClientFactory;
        this.workspaceClientFactory = workspaceClientFactory;
    }

    @Override
    public ItemStatus execute(WorkerParameters params, HandlerIO handler) {
        final ItemStatus itemStatus = new ItemStatus(HANDLER_ID);
        try (final LogbookOperationsClient logbookOperationsClient = logbookOperationsClientFactory.getClient()) {
            LogbookLifeCycleTraceabilityHelper helper =
                new LogbookObjectGroupLifeCycleTraceabilityHelper(handler, logbookOperationsClient, itemStatus,
                    params.getContainerName(), workspaceClientFactory);

            finalizeLifecycles(helper);
            itemStatus.increment(StatusCode.OK);
        } catch (TraceabilityException e) {
            LOGGER.error("Exception while finalizing", e);
            itemStatus.increment(StatusCode.FATAL);
        }
        return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // Nothing to check
    }

    /**
     * @return HANDLER_ID
     */
    public static String getId() {
        return HANDLER_ID;
    }
}
