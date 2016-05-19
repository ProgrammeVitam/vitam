package fr.gouv.vitam.workspace.api.exception;

// TODO REVIEW missing licence header

/**
 * Top level Exception for all Workspace Exceptions
 */
// FIXME REVIEW Exception should be checked so extending Exception, not RuntimeException
public class ContentAddressableStorageException extends RuntimeException {

    /**
    * 
    */
    // TODO REVIEW remove empty comment
    private static final long serialVersionUID = -4421004141804361330L;

    // TODO REVIEW comment
    public ContentAddressableStorageException(String message) {
        super(message);
    }

    /**
     * @param cause
     *            cause to associate with the exception
     */
    public ContentAddressableStorageException(Throwable cause) {
        super(cause);
    }

    /**
     * @param message
     *            message to associate with the exception
     * @param cause
     *            cause to associate with the exception
     */
    public ContentAddressableStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
