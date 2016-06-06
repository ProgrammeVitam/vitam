package fr.gouv.vitam.processing.distributor.core;

import fr.gouv.vitam.processing.worker.core.WorkerImpl;

/**
 * ProcessDistributorImpl Factory to create ProcessDistributorImpl
 */
public class ProcessDistributorImplFactory {

    /**
     * @return ProcessDistributorImpl
     */
    public ProcessDistributorImpl create() {
        return new ProcessDistributorImpl();
    }
    
    /**
     * @param workerImpl
     * @return ProcessDistributorImpl
     */
    public ProcessDistributorImpl create(WorkerImpl workerImpl) {
        return new ProcessDistributorImpl(workerImpl);
    }
}
