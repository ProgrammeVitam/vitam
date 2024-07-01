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

import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.worker.common.HandlerIO;

/**
 * CommitLifeCycleObjectGroup Handler
 */
public class CommitLifeCycleObjectGroupActionHandler extends CommitLifeCycleActionHandler {

    private static final String HANDLER_ID = "COMMIT_LIFE_CYCLE_OBJECT_GROUP";

    private static final int SHOULD_WRITE_RANK = 0;

    /**
     * @return HANDLER_ID
     */
    public static final String getId() {
        return HANDLER_ID;
    }

    @Override
    public void commitLifeCycle(HandlerIO handlerIO, String objectID, String operationId)
        throws LogbookClientBadRequestException, LogbookClientNotFoundException, LogbookClientServerException {
        if (handlerIO.getInput() != null && handlerIO.getInput().size() > 0) {
            boolean shouldWriteLFC = (boolean) handlerIO.getInput(SHOULD_WRITE_RANK);
            if (shouldWriteLFC) {
                handlerIO.getLifecyclesClient().commitObjectGroup(operationId, objectID);
            }
        } else {
            handlerIO.getLifecyclesClient().commitObjectGroup(operationId, objectID);
        }
    }

    @Override
    public ItemStatus getItemStatus() {
        return new ItemStatus(HANDLER_ID);
    }

    @Override
    public ItemStatus buildFinalItemStatus(ItemStatus itemStatus) {
        return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
    }
}
