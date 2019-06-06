package fr.gouv.vitam.worker.core.plugin.probativevalue.pojo;

import fr.gouv.vitam.common.model.logbook.LogbookOperation;

public class OperationWithClosestPreviousOperation {
    private final LogbookOperation operation;
    private final LogbookOperation closestToReferenceOperation;

    public OperationWithClosestPreviousOperation(LogbookOperation operation, LogbookOperation closestToReferenceOperation) {
        this.operation = operation;
        this.closestToReferenceOperation = closestToReferenceOperation;
    }

    public LogbookOperation getOperation() {
        return operation;
    }

    public LogbookOperation getClosestToReferenceOperation() {
        return closestToReferenceOperation;
    }
}