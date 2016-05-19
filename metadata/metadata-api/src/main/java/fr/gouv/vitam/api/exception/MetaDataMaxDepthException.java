package fr.gouv.vitam.api.exception;

/**
 * MetaDataMaxDepthException json depth limit exceeded error
 */
public class MetaDataMaxDepthException extends MetaDataException{
	// TODO REVIEW Do not specify json depth but query depth
	/**
	 * 
	 */
	private static final long serialVersionUID = -5718801648690145963L;
	// TODO REVIEW remove empty comment, specially for private
	
	 /**
     * @param message
     *            message to associate with the exception
     */
    public MetaDataMaxDepthException(String message) {
        super(message);
    }

    /**
     * @param cause
     *            cause to associate with the exception
     */
    public MetaDataMaxDepthException(Throwable cause) {
        super(cause);
    }

    /**
     * @param message
     *            message to associate with the exception
     * @param cause
     *            cause to associate with the exception
     */
    public MetaDataMaxDepthException(String message, Throwable cause) {
        super(message, cause);
    }

}
