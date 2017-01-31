package fr.gouv.vitam.worker.core.handler;

import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.worker.common.HandlerIO;

public class CommitLifeCycleObjectGroupActionHandler extends CommitLifeCycleActionHandler {

    private static final String HANDLER_ID = "COMMIT_LIFE_CYCLE_OBJECT_GROUP";

    /**
     * @return HANDLER_ID
     */
    public static final String getId() {
        return HANDLER_ID;
    }

    @Override
    public void commitLifeCycle(HandlerIO handlerIO, String objectID, String operationId)
        throws ProcessingException, LogbookClientBadRequestException, LogbookClientNotFoundException,
        LogbookClientServerException {
        handlerIO.getLifecyclesClient().commitObjectGroup(operationId, objectID);
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
