package fr.gouv.vitam.metadata.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * ObjectGroupPerOriginatingAgency class describing ObjectGroup
 */
public class ObjectGroupPerOriginatingAgency {

    @JsonProperty("_id")
    private ObjectGroupPerOriginatingAgencyPK id;

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
     * @param id             the aggregation id
     * @param numberOfObject the number of object
     * @param numberOfGOT    the number of GoT
     * @param size           the size
     */
    public ObjectGroupPerOriginatingAgency(ObjectGroupPerOriginatingAgencyPK id, long numberOfObject, long numberOfGOT,
        long size) {
        this.id = id;
        this.numberOfObject = numberOfObject;
        this.numberOfGOT = numberOfGOT;
        this.size = size;
    }

    /**
     * get aggregation id
     *
     * @return id
     */
    public ObjectGroupPerOriginatingAgencyPK getId() {
        return id;
    }

    /**
     * set id
     *
     * @param id
     */
    public void setId(ObjectGroupPerOriginatingAgencyPK id) {
        this.id = id;
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
