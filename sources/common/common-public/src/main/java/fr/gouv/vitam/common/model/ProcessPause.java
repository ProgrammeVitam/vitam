package fr.gouv.vitam.common.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Model to pause processes.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProcessPause {

    @JsonProperty("type")
    private String type;
    @JsonProperty("tenant")
    private Integer tenant;
    @JsonProperty("pauseAll")
    private Boolean pauseAll;

    /**
     * Constructor without fields use for jackson
     */
    public ProcessPause() {
    }

    /**
     * @param type
     * @param tenant
     */
    public ProcessPause(String type, int tenant, Boolean pauseAll) {
        this.type = type;
        this.tenant = tenant;
        this.pauseAll = pauseAll;
    }

    /**
     * Gets the type
     *
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the type
     *
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }


    /**
     * Gets the tenant
     *
     * @return the tenant
     */
    public Integer getTenant() {
        return tenant;
    }

    /**
     * Sets the tenant
     *
     * @param tenant the tenant to set
     */
    public void setTenant(Integer tenant) {
        this.tenant = tenant;
    }


    /**
     * Gets the pauseAll param
     *
     * @return the pauseAll
     */
    public Boolean getPauseAll() {
        return pauseAll;
    }

    /**
     * Sets the pauseAll
     *
     * @param pauseAll the pauseAll to set
     */
    public void setPauseAll(Boolean pauseAll) {
        this.pauseAll = pauseAll;
    }
}
