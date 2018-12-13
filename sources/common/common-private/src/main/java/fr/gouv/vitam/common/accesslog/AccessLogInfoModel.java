package fr.gouv.vitam.common.accesslog;

import java.util.Objects;

public class AccessLogInfoModel {

    private Boolean mustLog;
    private String eventDateTime;
    private String contextId;
    private String contractId;
    private String requestId;
    private String applicationId;
    private String objectId;
    private String archiveId;
    private String qualifier;
    private Integer version;
    private Long size;

    public Boolean getMustLog() {
        return mustLog;
    }

    public void setMustLog(Boolean mustLog) {
        this.mustLog = mustLog;
    }

    public String getEventDateTime() {
        return eventDateTime;
    }

    public void setEventDateTime(String eventDateTime) {
        this.eventDateTime = eventDateTime;
    }

    public String getContextId() {
        return contextId;
    }

    public void setContextId(String contextId) {
        this.contextId = contextId;
    }

    public String getContractId() {
        return contractId;
    }

    public void setContractId(String contractId) {
        this.contractId = contractId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public String getArchiveId() {
        return archiveId;
    }

    public void setArchiveId(String archiveId) {
        this.archiveId = archiveId;
    }

    public String getQualifier() {
        return qualifier;
    }

    public void setQualifier(String qualifier) {
        this.qualifier = qualifier;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    @Override
    public boolean equals(Object o) {
        if (super.equals(o)) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (!(o instanceof AccessLogInfoModel)) {
            return false;
        }

        AccessLogInfoModel other = (AccessLogInfoModel) o;

        if (!this.mustLog && !other.mustLog) {
            return true;
        }

        return Objects.equals(this.mustLog, other.mustLog) &&
            Objects.equals(this.eventDateTime, other.eventDateTime) &&
            Objects.equals(this.contextId, other.contextId) &&
            Objects.equals(this.contractId, other.contractId) &&
            Objects.equals(this.requestId, other.requestId) &&
            Objects.equals(this.objectId, other.objectId) &&
            Objects.equals(this.archiveId, other.archiveId) &&
            Objects.equals(this.qualifier, other.qualifier) &&
            Objects.equals(this.version, other.version) &&
            Objects.equals(this.size, other.size);
    }
}
