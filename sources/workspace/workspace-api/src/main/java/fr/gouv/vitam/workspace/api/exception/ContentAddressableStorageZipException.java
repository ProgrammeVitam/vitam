package fr.gouv.vitam.workspace.api.exception;

/**
 * Thrown when there is an error on a zip file resource (format, etc).
 */
public class ContentAddressableStorageZipException extends ContentAddressableStorageException {
    private static final long serialVersionUID = -7976465493734475323L;

    /**
     * @param message as String message to associate with the exception
     */
    public ContentAddressableStorageZipException(String message) {
        super(message);
    }

    /**
     * @param cause as String to associate with the exception
     */
    public ContentAddressableStorageZipException(Throwable cause) {
        super(cause);
    }

    /**
     * @param message : message to associate with the exception
     * @param cause : cause to associate with the exception
     */
    public ContentAddressableStorageZipException(String message, Throwable cause) {
        super(message, cause);
    }

}
