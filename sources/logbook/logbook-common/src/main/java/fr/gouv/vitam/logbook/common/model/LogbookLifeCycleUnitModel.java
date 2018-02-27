package fr.gouv.vitam.logbook.common.model;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleUnitParameters;

/**
 * LogbookLifeCycleUnitModel
 */
public class LogbookLifeCycleUnitModel implements LogbookLifeCycleModel {

    private String id;

    private Collection<LogbookLifeCycleUnitParameters> logbookLifeCycleParameters;

    /**
     * Constructor
     * 
     * @param id lfc id
     * @param logbookLifeCycleParameters lfc parameters
     */
    @JsonCreator
    public LogbookLifeCycleUnitModel(@JsonProperty("id") String id,
        @JsonProperty("logbookLifeCycleParameters") Collection<LogbookLifeCycleUnitParameters> logbookLifeCycleParameters) {
        this.id = id;
        this.logbookLifeCycleParameters = logbookLifeCycleParameters;
    }

    /**
     * Get logbookLifeCycleParameters
     * 
     * @return logbookLifeCycleParameters
     */
    public Collection<LogbookLifeCycleUnitParameters> getLogbookLifeCycleParameters() {
        return logbookLifeCycleParameters;
    }

    /**
     * Get id
     * 
     * @return id
     */
    public String getId() {
        return id;
    }

}
