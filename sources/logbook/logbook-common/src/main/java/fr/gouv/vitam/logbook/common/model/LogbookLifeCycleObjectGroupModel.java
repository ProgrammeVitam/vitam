package fr.gouv.vitam.logbook.common.model;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleObjectGroupParameters;

/**
 * LogbookLifeCycleObjectGroupModel
 */
public class LogbookLifeCycleObjectGroupModel implements LogbookLifeCycleModel {

    private String id;

    private Collection<LogbookLifeCycleObjectGroupParameters> logbookLifeCycleParameters;

    /**
     * Constructor
     * 
     * @param id lfc id
     * @param logbookLifeCycleParameters lfc parameters
     */
    @JsonCreator
    public LogbookLifeCycleObjectGroupModel(@JsonProperty("id") String id,
        @JsonProperty("logbookLifeCycleParameters") Collection<LogbookLifeCycleObjectGroupParameters> logbookLifeCycleParameters) {
        this.id = id;
        this.logbookLifeCycleParameters = logbookLifeCycleParameters;
    }

    @Override
    public Collection<LogbookLifeCycleObjectGroupParameters> getLogbookLifeCycleParameters() {
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
