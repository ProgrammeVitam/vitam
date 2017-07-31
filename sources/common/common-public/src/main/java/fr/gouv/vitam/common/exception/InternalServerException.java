package fr.gouv.vitam.common.exception;


/**
 * InternalServerException : manage Internal Server Exception
 */
public class InternalServerException extends VitamException {
    private static final long serialVersionUID = -632934116167401414L;

    /**
     * @param message
     */
    public InternalServerException(String message) {
        super(message);
    }

    /**
     * @param message
     * @param cause
     */
    public InternalServerException(String message, Throwable cause) {
        super(message, cause);
    }

}
