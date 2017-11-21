package fr.gouv.vitam.metadata.api.model;

/**
 * ObjectGroupPerOriginatingAgency class describing ObjectGroup
 */
public class ObjectGroupPerOriginatingAgency {

    private String originatingAgency;

    private int numberOfObject;

    private int numberOfGOT;

    private int size;

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
    public ObjectGroupPerOriginatingAgency(String originatingAgency, int numberOfObject, int numberOfGOT, int size) {
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
    public int getNumberOfObject() {
        return numberOfObject;
    }

    /**
     * setNumberOfObject
     * 
     * @param numberOfObject
     */
    public void setNumberOfObject(int numberOfObject) {
        this.numberOfObject = numberOfObject;
    }

    /**
     * getNumberOfGOT
     * 
     * @return numberOfGOT
     */
    public int getNumberOfGOT() {
        return numberOfGOT;
    }

    /**
     * setNumberOfGOT
     * 
     * @param numberOfGOT
     */
    public void setNumberOfGOT(int numberOfGOT) {
        this.numberOfGOT = numberOfGOT;
    }

    /**
     * getSize
     * 
     * @return size
     */
    public int getSize() {
        return size;
    }

    /**
     * setSize
     * 
     * @param size
     */
    public void setSize(int size) {
        this.size = size;
    }

}
