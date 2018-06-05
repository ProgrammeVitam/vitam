package fr.gouv.vitam.access.internal.common.exception;

public class AccessInternalRuleExecutionException extends
		AccessInternalException {

	private static final long serialVersionUID = 3507862590422500648L;

	/**
     * constructor with message
     *
     * @param message message to associate with the exception
     */
    public AccessInternalRuleExecutionException(String message) {
        super(message);
    }

    /**
     * constructor with throwable
     *
     * @param cause cause to associate with the exception
     */
    public AccessInternalRuleExecutionException(Throwable cause) {
        super(cause);
    }

    /**
     * constructor with message and throwable
     *
     * @param message message to associate with the exception
     * @param cause cause to associate with the exception
     */
    public AccessInternalRuleExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * constructor with message throwable enableSuppression and writableStackTrace
     *
     * @param message associated message
     * @param cause associated cause
     * @param enableSuppression allow suppression or not
     * @param writableStackTrace allow writable stack trace or not
     */
    public AccessInternalRuleExecutionException(String message, Throwable cause, boolean enableSuppression,
        boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
