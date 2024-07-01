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

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.common.utils.LogbookLifecycleWorkerHelper;

/**
 * CommitLifeCycle Handler
 */
public abstract class CommitLifeCycleActionHandler extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CommitLifeCycleActionHandler.class);

    /**
     * Default Constructor
     */
    public CommitLifeCycleActionHandler() {
        // Empty constructor
    }

    @Override
    public ItemStatus execute(WorkerParameters params, HandlerIO handlerIO) {
        checkMandatoryParameters(params);
        final ItemStatus itemStatus = getItemStatus();
        final String objectID = LogbookLifecycleWorkerHelper.getObjectID(params);
        final String operationId = params.getContainerName();
        try {
            checkMandatoryIOParameter(handlerIO);
            commitLifeCycle(handlerIO, objectID, operationId);
            itemStatus.increment(StatusCode.OK);
        } catch (
            final ProcessingException
            | LogbookClientServerException
            | LogbookClientNotFoundException
            | LogbookClientBadRequestException e
        ) {
            LOGGER.error(e);
            itemStatus.increment(StatusCode.FATAL);
        }

        return buildFinalItemStatus(itemStatus);
    }

    /**
     * Returns an ItemStatus
     *
     * @return an ItemStatus
     */
    public abstract ItemStatus getItemStatus();

    /**
     * Returns the final ItemStatus related to the current actionHandler execution based on a given one
     *
     * @param itemStatus a given ItemStatus
     * @return the final ActionHandler ItemStatus
     */
    public abstract ItemStatus buildFinalItemStatus(ItemStatus itemStatus);

    /**
     * Runs a commit process for the given object (Unit or ObjectGroup) and a given operation
     *
     * @param handlerIO a HandlerIO instance
     * @param objectID the object id to commit
     * @param operationId the operation id
     * @throws ProcessingException if processing exception occurred when commit unit lifecycle
     * @throws LogbookClientBadRequestException if the argument is incorrect when commit unit lifecycle
     * @throws LogbookClientNotFoundException if the element was not created before when commit unit lifecycle
     * @throws LogbookClientServerException if the Server got an internal error when commit unit lifecycle
     */
    public abstract void commitLifeCycle(HandlerIO handlerIO, String objectID, String operationId)
        throws ProcessingException, LogbookClientBadRequestException, LogbookClientNotFoundException, LogbookClientServerException;

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {}
}
