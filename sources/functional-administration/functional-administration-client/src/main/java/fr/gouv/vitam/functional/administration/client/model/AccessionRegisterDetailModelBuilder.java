package fr.gouv.vitam.functional.administration.client.model;

import fr.gouv.vitam.functional.administration.common.AccessionRegisterStatus;

public class AccessionRegisterDetailModelBuilder {
    private String id;
    private long tenant;
    private String originatingAgency;
    private String submissionAgency;
    private String endDate;
    private String startDate;
    private String lastUpdate;
    private AccessionRegisterStatus status;
    private RegisterValueDetailModel totalObjectsGroups;
    private RegisterValueDetailModel totalUnits;
    private RegisterValueDetailModel totalObjects;
    private RegisterValueDetailModel objectSize;

    public AccessionRegisterDetailModelBuilder setId(String id) {
        this.id = id;
        return this;
    }

    public AccessionRegisterDetailModelBuilder setTenant(long tenant) {
        this.tenant = tenant;
        return this;
    }

    public AccessionRegisterDetailModelBuilder setOriginatingAgency(String originatingAgency) {
        this.originatingAgency = originatingAgency;
        return this;
    }

    public AccessionRegisterDetailModelBuilder setSubmissionAgency(String submissionAgency) {
        this.submissionAgency = submissionAgency;
        return this;
    }

    public AccessionRegisterDetailModelBuilder setEndDate(String endDate) {
        this.endDate = endDate;
        return this;
    }

    public AccessionRegisterDetailModelBuilder setStartDate(String startDate) {
        this.startDate = startDate;
        return this;
    }

    public AccessionRegisterDetailModelBuilder setLastUpdate(String lastUpdate) {
        this.lastUpdate = lastUpdate;
        return this;
    }

    public AccessionRegisterDetailModelBuilder setStatus(AccessionRegisterStatus status) {
        this.status = status;
        return this;
    }

    public AccessionRegisterDetailModelBuilder setTotalObjectsGroups(RegisterValueDetailModel totalObjectsGroups) {
        this.totalObjectsGroups = totalObjectsGroups;
        return this;
    }

    public AccessionRegisterDetailModelBuilder setTotalUnits(RegisterValueDetailModel totalUnits) {
        this.totalUnits = totalUnits;
        return this;
    }

    public AccessionRegisterDetailModelBuilder setTotalObjects(RegisterValueDetailModel totalObjects) {
        this.totalObjects = totalObjects;
        return this;
    }

    public AccessionRegisterDetailModelBuilder setObjectSize(RegisterValueDetailModel objectSize) {
        this.objectSize = objectSize;
        return this;
    }

    public AccessionRegisterDetailModel createAccessionRegisterDetailModel() {
        return new AccessionRegisterDetailModel(id, tenant, originatingAgency, submissionAgency, endDate, startDate,
            lastUpdate, status, totalObjectsGroups, totalUnits, totalObjects, objectSize);
    }
}
