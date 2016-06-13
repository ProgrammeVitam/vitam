package fr.gouv.vitam.api.exception;

/**
 * MetaDataAlreadyExistException duplicate error
 */
public class MetaDataAlreadyExistException extends MetaDataException {

    /**
     * 
     */
    private static final long serialVersionUID = 130421172214167262L;
    // TODO REVIEW remove empty comment, specially for private

    /**
     * @param message message to associate with the exception
     */
    public MetaDataAlreadyExistException(String message) {
        super(message);
    }

    /**
     * @param cause cause to associate with the exception
     */
    public MetaDataAlreadyExistException(Throwable cause) {
        super(cause);
    }

    /**
     * @param message message to associate with the exception
     * @param cause cause to associate with the exception
     */
    public MetaDataAlreadyExistException(String message, Throwable cause) {
        super(message, cause);
    }
}
