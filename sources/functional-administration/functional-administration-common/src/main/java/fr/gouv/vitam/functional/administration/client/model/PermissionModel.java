package fr.gouv.vitam.functional.administration.client.model;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PermissionModel {

    @JsonProperty("_tenant")
    private long tenant;

    @JsonProperty("AccessContracts")
    private Set<String> accessContract;
    
    @JsonProperty("IngestContracts")
    private Set<String> ingestContract;
    
    /**
     * Constructor of permission
     * 
     * @param tenant
     * @param accessContract
     * @param ingestContract
     */
    public PermissionModel(long tenant, Set<String> accessContract, Set<String> ingestContract) {
        this.tenant = tenant;
        this.accessContract = accessContract;
        this.ingestContract = ingestContract;
    }
    public PermissionModel() {
    }
    
    public long getTenant() {
        return tenant;
    }

    public void setTenant(long tenant) {
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
