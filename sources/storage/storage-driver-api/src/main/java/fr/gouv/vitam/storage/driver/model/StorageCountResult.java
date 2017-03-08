package fr.gouv.vitam.storage.driver.model;

/**
 * Holds result data that come as a result of a request to count the objects on a container of the distant storage offer
 */
public class StorageCountResult extends StorageResult {
    private final long numberObjects;

    /**
     * Initialize the needed parameters for count results
     *
     * @param tenantId The request tenantId
     * @param type the type The request type
     * @param numberObjects Response of the number of objects in the container of the offer
     */
    public StorageCountResult(Integer tenantId, String type, long numberObjects) {
        super(tenantId, type);
        this.numberObjects = numberObjects;
    }

    /**
     * 
     * @return the numberObjects
     */
    public long getNumberObjects() {
        return numberObjects;
    }

}
