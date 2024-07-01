/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "https://cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
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

        return (
            Objects.equals(this.mustLog, other.mustLog) &&
            Objects.equals(this.eventDateTime, other.eventDateTime) &&
            Objects.equals(this.contextId, other.contextId) &&
            Objects.equals(this.contractId, other.contractId) &&
            Objects.equals(this.requestId, other.requestId) &&
            Objects.equals(this.objectId, other.objectId) &&
            Objects.equals(this.archiveId, other.archiveId) &&
            Objects.equals(this.qualifier, other.qualifier) &&
            Objects.equals(this.version, other.version) &&
            Objects.equals(this.size, other.size)
        );
    }
}
