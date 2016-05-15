package fr.gouv.vitam.workspace.api.exception;
// TODO REVIEW missing licence header
/**
 * Thrown when creating a resource (Container, CasBlob, Folder) that already
 * exists.
 */
public class ContentAddressableStorageAlreadyExistException extends ContentAddressableStorageException {

    /**
     * 
     */
    // TODO REVIEW remove empty comment
    private static final long serialVersionUID = 672300958086946348L;

    // TODO REVIEW comment
    public ContentAddressableStorageAlreadyExistException(String message) {
        super(message);
    }

    /**
     * @param cause
     *            cause to associate with the exception
     */
    public ContentAddressableStorageAlreadyExistException(Throwable cause) {
        super(cause);
    }

    /**
     * @param message
     *            message to associate with the exception
     * @param cause
     *            cause to associate with the exception
     */
    public ContentAddressableStorageAlreadyExistException(String message, Throwable cause) {
        super(message, cause);
    }

}
