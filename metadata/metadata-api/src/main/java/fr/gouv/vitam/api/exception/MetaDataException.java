package fr.gouv.vitam.api.exception;

public class MetaDataException extends Exception{
	
	 /**
	 * 
	 */
	private static final long serialVersionUID = 5683718092916241947L;

	/**
     * @param message
     *            message to associate with the exception
     */
    public MetaDataException(String message) {
        super(message);
    }

    /**
     * @param cause
     *            cause to associate with the exception
     */
    public MetaDataException(Throwable cause) {
        super(cause);
    }

    /**
     * @param message
     *            message to associate with the exception
     * @param cause
     *            cause to associate with the exception
     */
    public MetaDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
