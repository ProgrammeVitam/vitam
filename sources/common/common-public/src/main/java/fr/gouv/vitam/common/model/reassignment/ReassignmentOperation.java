/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common.model.reassignment;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Reassignment operation model
 */
public class ReassignmentOperation {

    public static final String OPERATION_ID = "OperationId";
    public static final String TARGET_ORIGINATING_AGENCY = "TargetOriginatingAgency";
    public static final String SOURCE_ORIGINATING_AGENCY = "SourceOriginatingAgency";
    public static final String REASSIGNMENT_DATE_TIME = "ReassignmentDate";

    @JsonProperty(OPERATION_ID)
    private String operationId;

    @JsonProperty(SOURCE_ORIGINATING_AGENCY)
    private String sourceOriginatingAgency;

    @JsonProperty(TARGET_ORIGINATING_AGENCY)
    private String targetOriginatingAgency;

    @JsonProperty(REASSIGNMENT_DATE_TIME)
    private String reassignmentDate;

    public String getOperationId() {
        return operationId;
    }

    public ReassignmentOperation setOperationId(String operationId) {
        this.operationId = operationId;
        return this;
    }

    public String getTargetOriginatingAgency() {
        return targetOriginatingAgency;
    }

    public ReassignmentOperation setTargetOriginatingAgency(String targetOriginatingAgency) {
        this.targetOriginatingAgency = targetOriginatingAgency;
        return this;
    }

    public String getSourceOriginatingAgency() {
        return sourceOriginatingAgency;
    }

    public ReassignmentOperation setSourceOriginatingAgency(String sourceOriginatingAgency) {
        this.sourceOriginatingAgency = sourceOriginatingAgency;
        return this;
    }

    public String getReassignmentDate() {
        return reassignmentDate;
    }

    public ReassignmentOperation setReassignmentDate(String reassignmentDate) {
        this.reassignmentDate = reassignmentDate;
        return this;
    }
}
