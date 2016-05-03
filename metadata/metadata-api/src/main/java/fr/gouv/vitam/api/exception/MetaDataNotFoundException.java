package fr.gouv.vitam.api.exception;

public class MetaDataNotFoundException extends MetaDataException{

	/**
	 * 
	 */
	private static final long serialVersionUID = 6036555671433217486L;
	
	/**
     * @param message
     *            message to associate with the exception
     */
    public MetaDataNotFoundException(String message) {
        super(message);
    }

    /**
     * @param cause
     *            cause to associate with the exception
     */
    public MetaDataNotFoundException(Throwable cause) {
        super(cause);
    }

    /**
     * @param message
     *            message to associate with the exception
     * @param cause
     *            cause to associate with the exception
     */
    public MetaDataNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

}
