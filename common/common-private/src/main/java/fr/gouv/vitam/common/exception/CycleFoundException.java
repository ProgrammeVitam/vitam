package fr.gouv.vitam.common.exception;

/**
 * 
 * Exception used in DFS when a cycle is found
 * 
 */
public class CycleFoundException extends VitamException {

    /**
     * 
     */
    private static final long serialVersionUID = -2684023194234768369L;

    /**
     * Basic constructor to indicate a simple error message without stacktrace
     *
     * @param message message to log
     */
    public CycleFoundException(String message) {
        super(message);
    }

    /**
     * Constructor used to encapsulate a previously thrown exception. A generic message is used.
     *
     * @param throwable the originating exception
     */
    public CycleFoundException(Throwable throwable) {
        super("An error occurred while creating, configuring or starting the application server", throwable);
    }

    /**
     * Constructor used to encapsulate a previously thrown exception with but with a custom meaningful message
     *
     * @param message the message to log throw threw
     * @param throwable the originating exception
     */
    public CycleFoundException(String message, Throwable throwable) {
        super(message, throwable);
    }

}
