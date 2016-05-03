package fr.gouv.vitam.api.exception;


public class MetaDataMaxDepthException extends MetaDataException{

	/**
	 * 
	 */
	private static final long serialVersionUID = -5718801648690145963L;
	
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
