package fr.gouv.vitam.logbook.common.model;

import java.util.Collection;

import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleParameters;

/**
 * LogbookLifeCycleModel interface
 */
public interface LogbookLifeCycleModel {

    /**
     * getLogbookLifeCycleParameters
     * 
     * @return Lifecycle parameters
     */
    Collection<? extends LogbookLifeCycleParameters> getLogbookLifeCycleParameters();
}
