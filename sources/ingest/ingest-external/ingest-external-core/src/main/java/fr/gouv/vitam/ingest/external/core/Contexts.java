package fr.gouv.vitam.ingest.external.core;

import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;

public enum Contexts {
    BLANK_TEST(LogbookTypeProcess.INGEST_TEST),
    DEFAULT_WORKFLOW(LogbookTypeProcess.INGEST);

    private LogbookTypeProcess logbookTypeProcess;

    Contexts(LogbookTypeProcess logbookTypeProcess) {
        this.logbookTypeProcess = logbookTypeProcess;
    }

    LogbookTypeProcess getLogbookTypeProcess() {
        return logbookTypeProcess;
    }

}
