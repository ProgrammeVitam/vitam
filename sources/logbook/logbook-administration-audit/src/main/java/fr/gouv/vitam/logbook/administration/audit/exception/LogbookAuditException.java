package fr.gouv.vitam.logbook.administration.audit.exception;

import fr.gouv.vitam.common.error.VitamError;

public class LogbookAuditException extends Exception {

    private VitamError vitamError;

    private static final long serialVersionUID = 1939529482757363926L;

    public LogbookAuditException(VitamError vitamError) {
        this.vitamError = vitamError;
    }

    /**
     * @param message associated message
     */
    public LogbookAuditException(String message) {
        super(message);
    }

    /**
     * @param cause associated cause
     */
    public LogbookAuditException(Throwable cause) {
        super(cause);
    }

    /**
     * @param message associated message
     * @param cause associated cause
     */
    public LogbookAuditException(String message, Throwable cause) {
        super(message, cause);
    }

    public VitamError getVitamError() {
        return vitamError;
    }

    public void setVitamError(VitamError vitamError) {
        this.vitamError = vitamError;
    }
}
