package fr.gouv.vitam.workspace.api.exception;

public class ContentAddressableStorageServerException extends ContentAddressableStorageException {

    /**
     * 
     */
    private static final long serialVersionUID = 2785259930527471401L;

    public ContentAddressableStorageServerException(String message) {
        super(message);
    }

    /**
     * @param cause
     *            cause to associate with the exception
     */
    public ContentAddressableStorageServerException(Throwable cause) {
        super(cause);
    }

    /**
     * @param message
     *            message to associate with the exception
     * @param cause
     *            cause to associate with the exception
     */
    public ContentAddressableStorageServerException(String message, Throwable cause) {
        super(message, cause);
    }
}