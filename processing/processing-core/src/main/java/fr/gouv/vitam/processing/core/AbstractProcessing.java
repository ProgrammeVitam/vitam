package fr.gouv.vitam.processing.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
*
*
 */
public abstract class AbstractProcessing {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractProcessing.class);

    public void executeProcess(Object archiveObject) {
        LOGGER.info("execution du traitement en cours...");
    }
    
    
    public abstract void executeProcess(ProcessEngineImpl processInstance, ExecutionContext executionContext);

}
