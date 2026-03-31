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
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.objectgroup.VersionsModelCustomized;
import fr.gouv.vitam.common.model.reassignment.ReassignmentOperation;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)
public class ObjectGroupToDeleteReportEntry {

    private static final String ORIGINATING_AGENCIES_REASSIGNMENTS = "originatingAgenciesReassignments";
    public static final String STATUS = "status";
    public static final String DELETED_VERSIONS = "deletedVersions";
    public static final String OUTCOME = "outCome";
    public static final String ORIGINATING_AGENCY = "OriginatingAgency";

    private final StatusCode status;
    private final String outcome;
    private final List<VersionsModelCustomized> deletedVersions;
    private final String originatingAgency;
    private final List<ReassignmentOperation> originatingAgenciesReassignments;

    @JsonCreator
    public ObjectGroupToDeleteReportEntry(
        @JsonProperty(STATUS) StatusCode status,
        @JsonProperty(OUTCOME) String outcome,
        @JsonProperty(DELETED_VERSIONS) List<VersionsModelCustomized> deletedVersions,
        @JsonProperty(ORIGINATING_AGENCY) String originatingAgency,
        @JsonProperty(ORIGINATING_AGENCIES_REASSIGNMENTS) List<ReassignmentOperation> originatingAgenciesReassignments
    ) {
        this.deletedVersions = deletedVersions;
        this.status = status;
        this.outcome = outcome;
        this.originatingAgency = originatingAgency;
        this.originatingAgenciesReassignments = originatingAgenciesReassignments;
    }

    @JsonProperty(DELETED_VERSIONS)
    public List<VersionsModelCustomized> getDeletedVersions() {
        return deletedVersions;
    }

    @JsonProperty(STATUS)
    public StatusCode getStatus() {
        return status;
    }

    @JsonProperty(OUTCOME)
    public String getOutcome() {
        return outcome;
    }

    @JsonProperty(ORIGINATING_AGENCY)
    public String getOriginatingAgency() {
        return originatingAgency;
    }

    @JsonProperty(ORIGINATING_AGENCIES_REASSIGNMENTS)
    public List<ReassignmentOperation> getOriginatingAgenciesReassignments() {
        return originatingAgenciesReassignments;
    }
}
