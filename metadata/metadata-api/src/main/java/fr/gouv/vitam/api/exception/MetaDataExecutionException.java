package fr.gouv.vitam.api.exception;

/**
 * MetaDataExecutionException database access error
 */
public class MetaDataExecutionException  extends MetaDataException{
	// TODO REVIEW only for access ?
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -8131926677545592877L;
	// TODO REVIEW remove empty comment, specially for private

	/**
     * @param message
     *            message to associate with the exception
     */
    public MetaDataExecutionException(String message) {
        super(message);
    }

    /**
     * @param cause
     *            cause to associate with the exception
     */
    public MetaDataExecutionException(Throwable cause) {
        super(cause);
    }

    /**
     * @param message
     *            message to associate with the exception
     * @param cause
     *            cause to associate with the exception
     */
    public MetaDataExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
