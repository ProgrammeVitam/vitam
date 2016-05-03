package fr.gouv.vitam.workspace.api.exception;

/**
 * Thrown when a resource (Container, CasBlob, Folder) cannot be located.
 */
public class ContentAddressableStorageNotFoundException extends ContentAddressableStorageException{

	/**
	 * 
	 */
	private static final long serialVersionUID = -8557401608602084895L;

	public ContentAddressableStorageNotFoundException(String message) {
        super(message);
    }

    /**
     * @param cause
     *            cause to associate with the exception
     */
    public ContentAddressableStorageNotFoundException(Throwable cause) {
        super(cause);
    }

    /**
     * @param message
     *            message to associate with the exception
     * @param cause
     *            cause to associate with the exception
     */
    public ContentAddressableStorageNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
