package fr.gouv.vitam.workspace.api.exception;

/**
 * Top level Exception for all Workspace Exceptions
 */
public class ContentAddressableStorageException extends Exception{

	 /**
	 * 
	 */
	private static final long serialVersionUID = -4421004141804361330L;

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
