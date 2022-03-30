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
import fr.gouv.vitam.batch.report.model.entry.PurgeObjectGroupReportEntry;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * PurgeObjectGroup
 */
public class PurgeObjectGroupModel {

    public static final String PROCESS_ID = "processId";
    public static final String TENANT = "_tenant";
    public static final String METADATA = "_metadata";
    public static final String CREATION_DATE_TIME = "creationDateTime";

    @JsonProperty(PROCESS_ID)
    private String processId;

    @JsonProperty(CREATION_DATE_TIME)
    private String creationDateTime;

    @JsonProperty(METADATA)
    private PurgeObjectGroupReportEntry metadata;

    @JsonProperty(TENANT)
    private int tenant;

    public PurgeObjectGroupModel() {
        // Empty constructor for deserialization
    }

    public PurgeObjectGroupModel(String processId, String creationDateTime,
        PurgeObjectGroupReportEntry metadata, int tenant) {
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

    public PurgeObjectGroupReportEntry getMetadata() {
        return metadata;
    }

    public void setMetadata(PurgeObjectGroupReportEntry metadata) {
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
        if (o == this)
            return true;
        if (!(o instanceof PurgeObjectGroupModel)) {
            return false;
        }
        PurgeObjectGroupModel eliminationActionObjectGroupModel = (PurgeObjectGroupModel) o;

        return Objects.equals(getTenant(), eliminationActionObjectGroupModel.getTenant())
            && Objects.equals(getProcessId(), eliminationActionObjectGroupModel.getProcessId())
            && Objects.equals(getMetadataId(this), getMetadataId(eliminationActionObjectGroupModel));
    }

    private static String getMetadataId(PurgeObjectGroupModel eliminationActionUnitModel) {
        if (eliminationActionUnitModel.getMetadata() == null
            || StringUtils.isEmpty(eliminationActionUnitModel.getMetadata().getId())) {
            throw new IllegalArgumentException("Invalid metadata");
        }

        return eliminationActionUnitModel.getMetadata().getId();
    }

    @Override
    public int hashCode() {
        return Objects.hash(metadata.getId(), processId, tenant);
    }
}
