package fr.gouv.vitam.functional.administration.common.exception;

/**
 * FileRulesException manage File Rules Exception
 * 
 */
public class FileRulesException extends ReferentialException {

    /**
     * RulesManagerException error
     */
    private static final long serialVersionUID = 1L;

    /**
     * @param message message to associate with the exception
     */
    public FileRulesException(String message) {
        super(message);
    }

    /**
     * @param cause cause to associate with the exception
     */
    public FileRulesException(Throwable cause) {
        super(cause);
    }

    /**
     * @param message message to associate with the exception
     * @param cause cause to associate with the exception
     */
    public FileRulesException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @param message associated message
     * @param cause associated cause
     * @param enableSuppression allow suppression or not
     * @param writableStackTrace allow writable stack trace or not
     */
    public FileRulesException(String message, Throwable cause, boolean enableSuppression,
        boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

