package fr.gouv.vitam.api.exception;

/**
 * MetaDataException metadata error
 */
public class MetaDataException extends RuntimeException {
    // TODO REVIEW specify that this exception is the general one (father of all exception)
    // TODO REVIEW Should not be a RuntimeException but an Exception since it must be catch (checked)
    /**
    * 
    */
    private static final long serialVersionUID = 5683718092916241947L;
    // TODO REVIEW remove empty comment, specially for private

    /**
     * @param message message to associate with the exception
     */
    public MetaDataException(String message) {
        super(message);
    }

    /**
     * @param cause cause to associate with the exception
     */
    public MetaDataException(Throwable cause) {
        super(cause);
    }

    /**
     * @param message message to associate with the exception
     * @param cause cause to associate with the exception
     */
    public MetaDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
