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
package fr.gouv.vitam.batch.report.model.entry;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Set;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.gouv.vitam.batch.report.model.ReportType.DELETE_GOT_VERSIONS;
import static fr.gouv.vitam.batch.report.model.entry.PreservationReportEntry.OBJECT_GROUP_ID;

@JsonInclude(NON_NULL)
public class DeleteGotVersionsReportEntry extends ReportEntry {

    public static final String PROCESS_ID = "processId";
    public static final String TENANT = "_tenant";
    public static final String CREATION_DATE_TIME = "creationDateTime";
    public static final String STATUS = "status";
    public static final String OBJECT_GROUP_GLOBAL = "objectGroupGlobal";
    public static final String UNIT_IDS = "unitIds";

    private final String processId;
    private final int tenant;
    private final String creationDateTime;
    private final String objectGroupId;
    private final Set<String> unitIds;
    private final List<ObjectGroupToDeleteReportEntry> objectGroupGlobal;

    @JsonCreator
    public DeleteGotVersionsReportEntry(
        @JsonProperty(DETAIL_ID) String detailId,
        @JsonProperty(PROCESS_ID) String processId,
        @JsonProperty(TENANT) int tenant,
        @JsonProperty(CREATION_DATE_TIME) String creationDateTime,
        @JsonProperty(OBJECT_GROUP_ID) String objectGroupId,
        @JsonProperty(UNIT_IDS) Set<String> unitIds,
        @JsonProperty(OBJECT_GROUP_GLOBAL) List<ObjectGroupToDeleteReportEntry> objectGroupGlobal,
        @JsonProperty(OUTCOME) String outcome
    ) {
        super(outcome, DELETE_GOT_VERSIONS.name(), detailId);
        this.processId = processId;
        this.tenant = tenant;
        this.creationDateTime = creationDateTime;
        this.objectGroupGlobal = objectGroupGlobal;
        this.objectGroupId = objectGroupId;
        this.unitIds = unitIds;
    }

    @JsonProperty(PROCESS_ID)
    public String getProcessId() {
        return processId;
    }

    @JsonProperty(TENANT)
    public int getTenant() {
        return tenant;
    }

    @JsonProperty(CREATION_DATE_TIME)
    public String getCreationDateTime() {
        return creationDateTime;
    }

    @JsonProperty(OBJECT_GROUP_ID)
    public String getObjectGroupId() {
        return objectGroupId;
    }

    @JsonProperty(OBJECT_GROUP_GLOBAL)
    public List<ObjectGroupToDeleteReportEntry> getObjectGroupGlobal() {
        return objectGroupGlobal;
    }

    @JsonProperty(UNIT_IDS)
    public Set<String> getUnitIds() {
        return unitIds;
    }
}
