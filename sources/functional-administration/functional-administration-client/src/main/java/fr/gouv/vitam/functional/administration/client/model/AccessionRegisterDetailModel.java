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

import fr.gouv.vitam.functional.administration.common.AccessionRegisterStatus;

public class AccessionRegisterDetailModel {

    @JsonProperty("_id")
    private String id;
    @JsonProperty("_tenant")
    private long tenant;
    @JsonProperty("OriginatingAgency")
    private String originatingAgency;
    @JsonProperty("SubmissionAgency")
    private String submissionAgency;
    // TODO date object
    @JsonProperty("EndDate")
    private String endDate;
    // TODO date object
    @JsonProperty("StartDate")
    private String startDate;
    @JsonProperty("LastUpdate")
    private String lastUpdate;
    @JsonProperty("Status")
    private AccessionRegisterStatus status;
    @JsonProperty("TotalObjectGroups")
    private RegisterValueDetailModel totalObjectsGroups;
    @JsonProperty("TotalUnits")
    private RegisterValueDetailModel totalUnits;
    @JsonProperty("TotalObjects")
    private RegisterValueDetailModel totalObjects;
    @JsonProperty("ObjectSize")
    private RegisterValueDetailModel ObjectSize;

    
    /**
     * Constructor without fields
     * 
     * use for jackson
     */
    public AccessionRegisterDetailModel() {
    }
    
    /**
     * Constructor using fields
     * 
     * @param id
     * @param tenant
     * @param originatingAgency
     * @param submissionAgency
     * @param endDate
     * @param startDate
     * @param lastUpdate
     * @param status
     * @param totalObjectsGroups
     * @param totalUnits
     * @param totalObjects
     * @param objectSize
     */
    public AccessionRegisterDetailModel(String id, long tenant, String originatingAgency, String submissionAgency,
        String endDate, String startDate, String lastUpdate,
        AccessionRegisterStatus status,
        RegisterValueDetailModel totalObjectsGroups,
        RegisterValueDetailModel totalUnits,
        RegisterValueDetailModel totalObjects,
        RegisterValueDetailModel objectSize) {
        this.id = id;
        this.tenant = tenant;
        this.originatingAgency = originatingAgency;
        this.submissionAgency = submissionAgency;
        this.endDate = endDate;
        this.startDate = startDate;
        this.lastUpdate = lastUpdate;
        this.status = status;
        this.totalObjectsGroups = totalObjectsGroups;
        this.totalUnits = totalUnits;
        this.totalObjects = totalObjects;
        ObjectSize = objectSize;
    }
    
    /**
     * 
     * @return String
     */
    public String getId() {
        return id;
    }
    
    /**
     * 
     * @param id
     * @return AccessionRegisterDetailModel
     */
    public AccessionRegisterDetailModel setId(String id) {
        this.id = id;
        return this;
    }
    
    /**
     * 
     * @return long
     */
    public long getTenant() {
        return tenant;
    }
    
    /**
     * 
     * @param tenant
     * @return AccessionRegisterDetailModel
     */
    public AccessionRegisterDetailModel setTenant(long tenant) {
        this.tenant = tenant;
        return this ;
    }
    
    /**
     * 
     * @return String
     */
    public String getOriginatingAgency() {
        return originatingAgency;
    }
    
    /**
     * 
     * @param originatingAgency
     * @return AccessionRegisterDetailModel
     */
    public AccessionRegisterDetailModel setOriginatingAgency(String originatingAgency) {
        this.originatingAgency = originatingAgency;
        return this;
    }
    
    /**
     * 
     * @return String
     */
    public String getSubmissionAgency() {
        return submissionAgency;
    }
    
    /**
     * 
     * @param submissionAgency
     * @return AccessionRegisterDetailModel
     */
    public AccessionRegisterDetailModel setSubmissionAgency(String submissionAgency) {
        this.submissionAgency = submissionAgency;
        return this;
    }
    
    /**
     * 
     * @return String
     */
    public String getEndDate() {
        return endDate;
    }
    
    /**
     * 
     * @param endDate
     * @return AccessionRegisterDetailModel
     */
    public AccessionRegisterDetailModel setEndDate(String endDate) {
        this.endDate = endDate;
        return this;
    }
    
    /**
     * 
     * @return String
     */
    public String getStartDate() {
        return startDate;
    }
    
    /**
     * 
     * @param startDate
     * @return AccessionRegisterDetailModel
     */
    public AccessionRegisterDetailModel setStartDate(String startDate) {
        this.startDate = startDate;
        return this;
    }
    
    /**
     * 
     * @return String
     */
    public String getLastUpdate() {
        return lastUpdate;
    }
    
    /**
     * 
     * @param lastUpdate
     * @return AccessionRegisterDetailModel
     */
    public AccessionRegisterDetailModel setLastUpdate(String lastUpdate) {
        this.lastUpdate = lastUpdate;
        return this;
    }
    
    /**
     * 
     * @return AccessionRegisterStatus
     */
    public AccessionRegisterStatus getStatus() {
        return status;
    }
    /**
     * 
     * @param status
     * @return AccessionRegisterDetailModel
     */
    public AccessionRegisterDetailModel setStatus(AccessionRegisterStatus status) {
        this.status = status;
        return this;
    }
    
    /**
     * 
     * @return RegisterValueDetailModel
     */
    public RegisterValueDetailModel getTotalObjectsGroups() {
        return totalObjectsGroups;
    }
    
    /**
     * 
     * @param totalObjectsGroups
     * @return AccessionRegisterDetailModel
     */
    public AccessionRegisterDetailModel setTotalObjectsGroups(RegisterValueDetailModel totalObjectsGroups) {
        this.totalObjectsGroups = totalObjectsGroups;
        return this;
    }
    
    /**
     * 
     * @return RegisterValueDetailModel
     */
    public RegisterValueDetailModel getTotalUnits() {
        return totalUnits;
    }
    
    /**
     * 
     * @param totalUnits
     * @return AccessionRegisterDetailModel
     */
    public AccessionRegisterDetailModel setTotalUnits(RegisterValueDetailModel totalUnits) {
        this.totalUnits = totalUnits;
        return this;
    }
    
    /**
     * 
     * @return RegisterValueDetailModel
     */
    public RegisterValueDetailModel getTotalObjects() {
        return totalObjects;
    }
    
    /**
     * 
     * @param totalObjects
     * @return AccessionRegisterDetailModel
     */
    public AccessionRegisterDetailModel setTotalObjects(RegisterValueDetailModel totalObjects) {
        this.totalObjects = totalObjects;
        return this;
    }
    
    /**
     * 
     * @return RegisterValueDetailModel
     */
    public RegisterValueDetailModel getObjectSize() {
        return ObjectSize;
    }
    
    /**
     * 
     * @param objectSize
     * @return AccessionRegisterDetailModel
     */
    public AccessionRegisterDetailModel setObjectSize(RegisterValueDetailModel objectSize) {
        ObjectSize = objectSize;
        return this;
    }

}
