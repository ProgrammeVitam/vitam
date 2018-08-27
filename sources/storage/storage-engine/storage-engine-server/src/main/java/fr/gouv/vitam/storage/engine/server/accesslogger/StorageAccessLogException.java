package fr.gouv.vitam.storage.engine.server.accesslogger;

import fr.gouv.vitam.common.exception.VitamException;

public class StorageAccessLogException extends VitamException {

    private static final long serialVersionUID = -1138768439172966496L;

    /**
     * @param message associated message
     */
    public StorageAccessLogException(String message) {
        super(message);
    }

    /**
     * @param cause associated cause
     */
    public StorageAccessLogException(Throwable cause) {
        super(cause);
    }

    /**
     * @param message associated message
     * @param cause   associated cause
     */
    public StorageAccessLogException(String message, Throwable cause) {
        super(message, cause);
    }
}
