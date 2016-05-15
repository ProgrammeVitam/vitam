package fr.gouv.vitam.workspace.api.exception;

// TODO REVIEW missing licence header
// TODO REVIEW missing javadoc comments 

public class ContentAddressableStorageServerException extends ContentAddressableStorageException {

    /**
     * 
     */
    // TODO REVIEW remove empty comment
    private static final long serialVersionUID = 2785259930527471401L;

    // TODO REVIEW comment
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
