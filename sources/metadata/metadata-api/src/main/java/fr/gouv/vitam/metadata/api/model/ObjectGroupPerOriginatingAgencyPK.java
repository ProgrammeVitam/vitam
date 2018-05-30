package fr.gouv.vitam.metadata.api.model;

/**
 * ObjectGroupPerOriginatingAgency class describing ObjectGroup
 */
public class ObjectGroupPerOriginatingAgencyPK {

    private String originatingAgency;
    private String opi;
    private String qualifierVersionOpi;

    /**
     * Constructor
     */
    public ObjectGroupPerOriginatingAgencyPK() {
        // empty constructor
    }

    /**
     * Constructor
     *
     * @param originatingAgency   the originating agency
     * @param opi                 object group creation operation id
     * @param qualifierVersionOpi qualifier version operation id
     */
    public ObjectGroupPerOriginatingAgencyPK(String originatingAgency, String opi, String qualifierVersionOpi) {
        this.originatingAgency = originatingAgency;
        this.opi = opi;
        this.qualifierVersionOpi = qualifierVersionOpi;
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

    public String getOpi() {
        return opi;
    }

    public void setOpi(String opi) {
        this.opi = opi;
    }

    public String getQualifierVersionOpi() {
        return qualifierVersionOpi;
    }

    public void setQualifierVersionOpi(String qualifierVersionOpi) {
        this.qualifierVersionOpi = qualifierVersionOpi;
    }
}
