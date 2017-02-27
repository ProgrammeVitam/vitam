package fr.gouv.vitam.ingest.internal.upload.rest;

import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;

public class ProcessContext {

    /**
     * idWorkFlow properties, must be defined in JSON file(required)
     */

    private String workFlowId;


    /**
     * executionContext properties, must be defined in JSON file(required)
     */

    private String executionContext;

    private LogbookTypeProcess logbookTypeProcess;

    /**
     * @return the workFlowId
     */
    public String getWorkFlowId() {
        return workFlowId;
    }

    /**
     * @param workFlowId the workFlowId to set
     * @return
     *
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
     * @return
     *
     * @return this
     */
    public ProcessContext setExecutionContext(String executionContext) {
        this.executionContext = executionContext;
        return this;
    }

    /**
     * @return the logbookTypeProcess
     */
    public LogbookTypeProcess getLogbookTypeProcess() {
        return logbookTypeProcess;
    }

    /**
     * @param logbookTypeProcess the logbookTypeProcess
     * @return this
     */
    public ProcessContext setLogbookTypeProcess(LogbookTypeProcess logbookTypeProcess) {
        this.logbookTypeProcess = logbookTypeProcess;
        return this;
    }
}
