/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.functional.administration.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data Transfer Object Model of contract (DTO).
 */

public class IngestContractModel {
	
    /**
     * unique identifier
     */
    @JsonProperty("_id")
    private String id;

    /**
     * tenant id
     */
    @JsonProperty("_tenant")
    private long tenant;
    
    @JsonProperty("Name")
    private String name;
    
    @JsonProperty("Description")
    private String description;
    
    @JsonProperty("Status")
    private String status;
    
    @JsonProperty("CreationDate")
    private String creationdate;
    
    @JsonProperty("LastUpdate")
    private String lastupdate;
    
    @JsonProperty("ActivationDate")
    private String activationdate;
    
    @JsonProperty("DeactivationDate")
	private String deactivationdate;

    
   
    /**
     * Constructor without fields
     * use for jackson
     */
    public IngestContractModel() {
    }

    /**
     * @return id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id
     * @return this
     */
    public IngestContractModel setId(String id) {
        this.id = id;
        return this;
    }

    /**
     * @return tenant
     */
    public long getTenant() {
        return tenant;
    }

    /**
     * @param tenant
     * @return this
     */
    public IngestContractModel setTenant(long tenant) {
        this.tenant = tenant;
        return this;
    }

    /**
     * Name of the contract
     * @return
     */
    public String getName() {
    	return this.name;
    }

    /**
     * Set or change the contract name
     * @param name
     * @return
     */
    public IngestContractModel setName(String name) {
        this.name = name;
    	return this;
    }

    /**
     * Get the contract description
     * @return
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * Set or change the contract description
     * @param description
     * @return
     */
    public IngestContractModel setDescription(String description) {
        this.description = description;
    	return this;
    }

        
    /**
     * Get the contract status
     * @return
     */
    public String getStatus() {
        return this.status;
    }
    

    /**
     * Set or change the contract status
     * @param status
     * @return
     */
    public IngestContractModel setStatus(String status) {
        this.status = status;
    	return this;
    }

    public String getCreationdate() {
        return this.creationdate;
    }

    public IngestContractModel setCreationdate(String creationdate) {
        this.creationdate = creationdate;
    	return this;
    }

    public String getLastupdate() {
    	return this.lastupdate;
    }

    public IngestContractModel setLastupdate(String lastupdate) {
        this.lastupdate = lastupdate;
    	return this;
    }

    public String getActivationdate() {
    	return this.activationdate;
    }

    public IngestContractModel setActivationdate(String activationdate) {
    	this.activationdate = activationdate;
    	return this;
    }

    public String getDeactivationdate() {
    	return this.deactivationdate;
    }

    public IngestContractModel setDeactivationdate(String deactivationdate) {
    	this.deactivationdate = activationdate;
    	return this;
    }

}
