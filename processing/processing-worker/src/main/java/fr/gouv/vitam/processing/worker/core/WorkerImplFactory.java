package fr.gouv.vitam.processing.worker.core;

import fr.gouv.vitam.processing.worker.handler.ActionHandler;

/**
 * WorkerImpl Factory to create workerImpl
 */
public class WorkerImplFactory {

    /**
     * @return WorkerImpl
     */
    public WorkerImpl create() {
        return new WorkerImpl();
    }

    /**
     * @param actionName
     * @param actionHandler
     * @return WorkerImpl
     */
    public WorkerImpl create(String actionName, ActionHandler actionHandler) {
        return new WorkerImpl(actionName, actionHandler);
    }

}
