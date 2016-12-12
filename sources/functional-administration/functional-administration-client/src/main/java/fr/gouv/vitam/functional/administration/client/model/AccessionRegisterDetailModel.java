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

    // use for jackson
    public AccessionRegisterDetailModel() {
    }

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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getTenant() {
        return tenant;
    }

    public void setTenant(long tenant) {
        this.tenant = tenant;
    }

    public String getOriginatingAgency() {
        return originatingAgency;
    }

    public void setOriginatingAgency(String originatingAgency) {
        this.originatingAgency = originatingAgency;
    }

    public String getSubmissionAgency() {
        return submissionAgency;
    }

    public void setSubmissionAgency(String submissionAgency) {
        this.submissionAgency = submissionAgency;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(String lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public AccessionRegisterStatus getStatus() {
        return status;
    }

    public void setStatus(AccessionRegisterStatus status) {
        this.status = status;
    }

    public RegisterValueDetailModel getTotalObjectsGroups() {
        return totalObjectsGroups;
    }

    public void setTotalObjectsGroups(RegisterValueDetailModel totalObjectsGroups) {
        this.totalObjectsGroups = totalObjectsGroups;
    }

    public RegisterValueDetailModel getTotalUnits() {
        return totalUnits;
    }

    public void setTotalUnits(RegisterValueDetailModel totalUnits) {
        this.totalUnits = totalUnits;
    }

    public RegisterValueDetailModel getTotalObjects() {
        return totalObjects;
    }

    public void setTotalObjects(RegisterValueDetailModel totalObjects) {
        this.totalObjects = totalObjects;
    }

    public RegisterValueDetailModel getObjectSize() {
        return ObjectSize;
    }

    public void setObjectSize(RegisterValueDetailModel objectSize) {
        ObjectSize = objectSize;
    }

}
