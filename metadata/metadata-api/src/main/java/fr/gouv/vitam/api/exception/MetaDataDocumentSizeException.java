package fr.gouv.vitam.api.exception;

/**
 * MetaDataDocumentSizeException max size exceeded error
 */
public class MetaDataDocumentSizeException extends MetaDataException{

	/**
	 * 
	 */
	private static final long serialVersionUID = 3476175676691016762L;
	
	/**
     * @param message
     *            message to associate with the exception
     */
    public MetaDataDocumentSizeException(String message) {
        super(message);
    }

    /**
     * @param cause
     *            cause to associate with the exception
     */
    public MetaDataDocumentSizeException(Throwable cause) {
        super(cause);
    }

    /**
     * @param message
     *            message to associate with the exception
     * @param cause
     *            cause to associate with the exception
     */
    public MetaDataDocumentSizeException(String message, Throwable cause) {
        super(message, cause);
    }
}
