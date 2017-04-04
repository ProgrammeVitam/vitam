package fr.gouv.vitam.ingest.external.core;

import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;

/**
 * Context enum
 */
public enum Contexts {
    
    BLANK_TEST(LogbookTypeProcess.INGEST_TEST),
    HOLDING_SCHEME(LogbookTypeProcess.MASTERDATA),
    FILING_SCHEME(LogbookTypeProcess.INGEST),
    DEFAULT_WORKFLOW(LogbookTypeProcess.INGEST);

    private static final String FILINGSCHEME = "FILINGSCHEME";
    private static final String HOLDINGSCHEME = "HOLDINGSCHEME";
    private static final String PROCESS_SIP_UNITARY = "PROCESS_SIP_UNITARY";
    private LogbookTypeProcess logbookTypeProcess;

    Contexts(LogbookTypeProcess logbookTypeProcess) {
        this.logbookTypeProcess = logbookTypeProcess;
    }

    LogbookTypeProcess getLogbookTypeProcess() {
        return logbookTypeProcess;
    }
    
    /**
     * @return eventType string
     */
    public String getEventType() {
        switch (this) {
            case BLANK_TEST:
            case DEFAULT_WORKFLOW:
                return PROCESS_SIP_UNITARY;
            case FILING_SCHEME:
                return FILINGSCHEME;
            case HOLDING_SCHEME:
                return HOLDINGSCHEME;
            default:
                return PROCESS_SIP_UNITARY;
        }

    }

}
