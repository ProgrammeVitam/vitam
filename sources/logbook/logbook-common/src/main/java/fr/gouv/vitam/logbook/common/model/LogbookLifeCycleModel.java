package fr.gouv.vitam.logbook.common.model;

import java.util.Collection;

import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleParameters;

public interface LogbookLifeCycleModel {
    Collection<? extends LogbookLifeCycleParameters> getLogbookLifeCycleParameters();
}
