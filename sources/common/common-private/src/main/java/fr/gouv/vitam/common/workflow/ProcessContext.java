package fr.gouv.vitam.common.workflow;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.vitam.common.json.JsonHandler;

/**
 * Process Context
 */
public class ProcessContext {

    /**
     * idWorkFlow properties, must be defined in JSON file(required)
     */
    @JsonProperty("WorkFlowId")
    private String workFlowId;


    /**
     * executionContext properties, must be defined in JSON file(required)
     */
    @JsonProperty("ExecutionContext")
    private String executionContext;

    @JsonProperty("LogbookTypeProcess")
    private String logbookTypeProcess;

    /**
     * @return the workFlowId
     */
    public String getWorkFlowId() {
        return workFlowId;
    }

    /**
     * @param workFlowId the workFlowId to set
     * @return this
     */
    public ProcessContext setWorkFlowId(String workFlowId) {
        this.workFlowId = workFlowId;
        return this;
    }

    /**
     * @return the executionContext
     */
    public String getExecutionContext() {
        return executionContext;
    }

    /**
     * @param executionContext the executionContext to set
     * @return this
     */
    public ProcessContext setExecutionContext(String executionContext) {
        this.executionContext = executionContext;
        return this;
    }

    /**
     * @return the logbookTypeProcess
     */
    public String getLogbookTypeProcess() {
        return logbookTypeProcess;
    }

    /**
     * @param logbookTypeProcess the logbookTypeProcess
     * @return this
     */
    public ProcessContext setLogbookTypeProcess(String logbookTypeProcess) {
        this.logbookTypeProcess = logbookTypeProcess;
        return this;
    }

    @Override
    public String toString() {
        return JsonHandler.unprettyPrint(this);
    }
}
