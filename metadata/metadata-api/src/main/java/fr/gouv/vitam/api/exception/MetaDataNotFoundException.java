package fr.gouv.vitam.api.exception;

/**
 * MetaDataNotFoundException document parent not found error
 */
public class MetaDataNotFoundException extends MetaDataException {
    // TODO REVIEW Not only parent! For Insert ok, but not for SELECT, UPDATE, DELETE

    /**
     * 
     */
    private static final long serialVersionUID = 6036555671433217486L;
    // TODO REVIEW remove empty comment, specially for private

    /**
     * @param message message to associate with the exception
     */
    public MetaDataNotFoundException(String message) {
        super(message);
    }

    /**
     * @param cause cause to associate with the exception
     */
    public MetaDataNotFoundException(Throwable cause) {
        super(cause);
    }

    /**
     * @param message message to associate with the exception
     * @param cause cause to associate with the exception
     */
    public MetaDataNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

}
