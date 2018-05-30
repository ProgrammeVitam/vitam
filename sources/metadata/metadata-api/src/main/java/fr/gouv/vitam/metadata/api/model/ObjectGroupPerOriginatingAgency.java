package fr.gouv.vitam.metadata.api.model;

/**
 * ObjectGroupPerOriginatingAgency class describing ObjectGroup
 */
public class ObjectGroupPerOriginatingAgency {

    private String operation;

    private boolean symbolic;

    private String agency;

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
     * @param operation      the operation id
     * @param numberOfObject the number of object
     * @param numberOfGOT    the number of GoT
     * @param size           the size
     */
    public ObjectGroupPerOriginatingAgency(String operation, boolean symbolic, String agency, long numberOfObject, long numberOfGOT,
        long size) {
        this.operation = operation;
        this.agency = agency;
        this.numberOfObject = numberOfObject;
        this.numberOfGOT = numberOfGOT;
        this.size = size;
        this.symbolic = symbolic;
    }


    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
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

    public boolean isSymbolic() {
        return symbolic;
    }

    public void setSymbolic(boolean symbolic) {
        this.symbolic = symbolic;
    }

    public String getAgency() {
        return agency;
    }

    public void setAgency(String agency) {
        this.agency = agency;
    }
}
