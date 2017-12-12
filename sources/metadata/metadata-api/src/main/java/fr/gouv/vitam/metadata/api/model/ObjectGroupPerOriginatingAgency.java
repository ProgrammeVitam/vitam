package fr.gouv.vitam.metadata.api.model;

/**
 * ObjectGroupPerOriginatingAgency class describing ObjectGroup
 */
public class ObjectGroupPerOriginatingAgency {

    private String originatingAgency;

    private long numberOfObject;

    private long numberOfGOT;

    private long size;

    /**
     * Constructor
     */
    public ObjectGroupPerOriginatingAgency() {
        // empty constructor
    }

    /**
     * Constructor
     *
     * @param originatingAgency the originating agency
     * @param numberOfObject the number of object
     * @param numberOfGOT the number of GoT
     * @param size the size
     */
    public ObjectGroupPerOriginatingAgency(String originatingAgency, long numberOfObject, long numberOfGOT, long size) {
        this.originatingAgency = originatingAgency;
        this.numberOfObject = numberOfObject;
        this.numberOfGOT = numberOfGOT;
        this.size = size;
    }

    /**
     * getOriginatingAgency
     *
     * @return originatingAgency
     */
    public String getOriginatingAgency() {
        return originatingAgency;
    }

    /**
     * setOriginatingAgency
     *
     * @param originatingAgency
     */
    public void setOriginatingAgency(String originatingAgency) {
        this.originatingAgency = originatingAgency;
    }

    /**
     * getNumberOfObject
     *
     * @return numberOfObject
     */
    public long getNumberOfObject() {
        return numberOfObject;
    }

    /**
     * setNumberOfObject
     *
     * @param numberOfObject
     */
    public void setNumberOfObject(long numberOfObject) {
        this.numberOfObject = numberOfObject;
    }

    /**
     * getNumberOfGOT
     *
     * @return numberOfGOT
     */
    public long getNumberOfGOT() {
        return numberOfGOT;
    }

    /**
     * setNumberOfGOT
     *
     * @param numberOfGOT
     */
    public void setNumberOfGOT(long numberOfGOT) {
        this.numberOfGOT = numberOfGOT;
    }

    /**
     * getSize
     *
     * @return size
     */
    public long getSize() {
        return size;
    }

    /**
     * setSize
     *
     * @param size
     */
    public void setSize(long size) {
        this.size = size;
    }

}
