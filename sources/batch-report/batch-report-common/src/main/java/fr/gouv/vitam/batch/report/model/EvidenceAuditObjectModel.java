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
package fr.gouv.vitam.batch.report.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.vitam.batch.report.model.entry.EvidenceAuditReportEntry;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * EvidenceAuditObjectModel
 */
public class EvidenceAuditObjectModel {

    public static final String PROCESS_ID = "processId";
    public static final String TENANT = "_tenant";
    public static final String METADATA = "_metadata";
    public static final String CREATION_DATE_TIME = "creationDateTime";

    @JsonProperty(PROCESS_ID)
    private String processId;

    @JsonProperty(CREATION_DATE_TIME)
    private String creationDateTime;

    @JsonProperty(METADATA)
    private EvidenceAuditReportEntry metadata;

    @JsonProperty(TENANT)
    private int tenant;

    public EvidenceAuditObjectModel() {
        // Empty constructor for deserialization
    }

    public EvidenceAuditObjectModel(
        String processId,
        int tenant,
        String creationDateTime,
        EvidenceAuditReportEntry metadata
    ) {
        this.processId = processId;
        this.creationDateTime = creationDateTime;
        this.metadata = metadata;
        this.tenant = tenant;
    }

    public String getProcessId() {
        return processId;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }

    public String getCreationDateTime() {
        return creationDateTime;
    }

    public void setCreationDateTime(String creationDateTime) {
        this.creationDateTime = creationDateTime;
    }

    public EvidenceAuditReportEntry getMetadata() {
        return metadata;
    }

    public void setMetadata(EvidenceAuditReportEntry metadata) {
        this.metadata = metadata;
    }

    public int getTenant() {
        return tenant;
    }

    public void setTenant(int tenant) {
        this.tenant = tenant;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof EvidenceAuditObjectModel)) {
            return false;
        }
        EvidenceAuditObjectModel evidenceAuditObjectModel = (EvidenceAuditObjectModel) o;

        return (
            Objects.equals(getTenant(), evidenceAuditObjectModel.getTenant()) &&
            Objects.equals(getProcessId(), evidenceAuditObjectModel.getProcessId()) &&
            Objects.equals(getMetadataId(this), getMetadataId(evidenceAuditObjectModel))
        );
    }

    private static String getMetadataId(EvidenceAuditObjectModel evidenceAuditObjectModel) {
        if (
            evidenceAuditObjectModel.getMetadata() == null ||
            StringUtils.isEmpty(evidenceAuditObjectModel.getMetadata().getDetailId())
        ) {
            throw new IllegalArgumentException("Invalid metadata");
        }

        return evidenceAuditObjectModel.getMetadata().getDetailId();
    }

    @Override
    public int hashCode() {
        return Objects.hash(metadata.getObjectType(), processId, tenant);
    }
}
