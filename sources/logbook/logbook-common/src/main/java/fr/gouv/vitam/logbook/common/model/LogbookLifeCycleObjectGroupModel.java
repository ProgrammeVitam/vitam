package fr.gouv.vitam.logbook.common.model;

import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleObjectGroupParameters;

public class LogbookLifeCycleObjectGroupModel implements LogbookLifeCycleModel {

    private String id;

    private Collection<LogbookLifeCycleObjectGroupParameters> logbookLifeCycleParameters;

    @JsonCreator
    public LogbookLifeCycleObjectGroupModel(@JsonProperty("id") String id,
        @JsonProperty("logbookLifeCycleParameters")
            Collection<LogbookLifeCycleObjectGroupParameters> logbookLifeCycleParameters) {
        this.id = id;
        this.logbookLifeCycleParameters = logbookLifeCycleParameters;
    }

    @Override
    public Collection<LogbookLifeCycleObjectGroupParameters> getLogbookLifeCycleParameters() {
        return logbookLifeCycleParameters;
    }

    public String getId() {
        return id;
    }

}
