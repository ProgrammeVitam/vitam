package fr.gouv.vitam.logbook.common.model;

import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleObjectGroupParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleUnitParameters;

public class LogbookLifeCycleUnitModel implements LogbookLifeCycleModel {

    private String id;

    private Collection<LogbookLifeCycleUnitParameters> logbookLifeCycleParameters;

    @JsonCreator
    public LogbookLifeCycleUnitModel(@JsonProperty("id") String id,
        @JsonProperty("logbookLifeCycleParameters")
            Collection<LogbookLifeCycleUnitParameters> logbookLifeCycleParameters) {
        this.id = id;
        this.logbookLifeCycleParameters = logbookLifeCycleParameters;
    }

    public Collection<LogbookLifeCycleUnitParameters> getLogbookLifeCycleParameters() {
        return logbookLifeCycleParameters;
    }

    public String getId() {
        return id;
    }

}
