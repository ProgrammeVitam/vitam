package fr.gouv.vitam.common.model.administration;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import fr.gouv.vitam.common.model.ModelConstants;

/**
 * Permission model POJO
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class PermissionModel {

    @JsonProperty("tenant")
    private Integer tenant;

    @JsonProperty("AccessContracts")
    private Set<String> accessContract = new HashSet<>();

    @JsonProperty("IngestContracts")
    private Set<String> ingestContract = new HashSet<>();

    /**
     * Constructor of permission
     *
     * @param tenant
     * @param accessContract
     * @param ingestContract
     */
    public PermissionModel(int tenant, Set<String> accessContract, Set<String> ingestContract) {
        this.tenant = tenant;
        this.accessContract = accessContract;
        this.ingestContract = ingestContract;
    }

    /**
     * Default constructor
     */
    public PermissionModel() {
        // empty constructor
    }


    @JsonProperty(ModelConstants.TAG_TENANT)
    public Integer getTenant() {
        return tenant;
    }

    @JsonProperty(ModelConstants.TAG_TENANT)
    public void setTenant(Integer tenant) {
        this.tenant = tenant;
    }

    public Set<String> getAccessContract() {
        return accessContract;
    }

    public void setAccessContract(Set<String> accessContract) {
        this.accessContract = accessContract;
    }

    public Set<String> getIngestContract() {
        return ingestContract;
    }

    public void setIngestContract(Set<String> ingestContract) {
        this.ingestContract = ingestContract;
    }

}
