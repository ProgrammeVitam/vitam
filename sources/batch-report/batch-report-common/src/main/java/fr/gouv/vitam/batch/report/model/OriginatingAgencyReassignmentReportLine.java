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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)
public class OriginatingAgencyReassignmentReportLine {

    private static final String ID = "id";
    private static final String TYPE = "type";
    private static final String OPI = "opi";
    private static final String SOURCE_ORIGINATING_AGENCY = "sourceOriginatingAgency";
    private static final String TARGET_ORIGINATING_AGENCY = "targetOriginatingAgency";
    private static final String OBJECT_GROUP_ID = "objectGroupId";

    @JsonProperty(ID)
    private String id;

    @JsonProperty(TYPE)
    private String type;

    @JsonProperty(OPI)
    private String opi;

    @JsonProperty(SOURCE_ORIGINATING_AGENCY)
    private String sourceOriginatingAgency;

    @JsonProperty(TARGET_ORIGINATING_AGENCY)
    private String targetOriginatingAgency;

    @JsonProperty(OBJECT_GROUP_ID)
    private String objectGroupId;

    public OriginatingAgencyReassignmentReportLine() {
        // Empty constructor
    }

    public OriginatingAgencyReassignmentReportLine(
        String id,
        String type,
        String opi,
        String sourceOriginatingAgency,
        String targetOriginatingAgency,
        String objectGroupId
    ) {
        this.id = id;
        this.type = type;
        this.opi = opi;
        this.sourceOriginatingAgency = sourceOriginatingAgency;
        this.targetOriginatingAgency = targetOriginatingAgency;
        this.objectGroupId = objectGroupId;
    }

    public OriginatingAgencyReassignmentReportLine(
        String id,
        String type,
        String opi,
        String sourceOriginatingAgency,
        String targetOriginatingAgency
    ) {
        this.id = id;
        this.type = type;
        this.opi = opi;
        this.sourceOriginatingAgency = sourceOriginatingAgency;
        this.targetOriginatingAgency = targetOriginatingAgency;
    }

    public String getId() {
        return id;
    }

    public OriginatingAgencyReassignmentReportLine setId(String id) {
        this.id = id;
        return this;
    }

    public String getType() {
        return type;
    }

    public OriginatingAgencyReassignmentReportLine setType(String type) {
        this.type = type;
        return this;
    }

    public String getOpi() {
        return opi;
    }

    public OriginatingAgencyReassignmentReportLine setOpi(String opi) {
        this.opi = opi;
        return this;
    }

    public String getSourceOriginatingAgency() {
        return sourceOriginatingAgency;
    }

    public OriginatingAgencyReassignmentReportLine setSourceOriginatingAgency(String sourceOriginatingAgency) {
        this.sourceOriginatingAgency = sourceOriginatingAgency;
        return this;
    }

    public String getTargetOriginatingAgency() {
        return targetOriginatingAgency;
    }

    public OriginatingAgencyReassignmentReportLine setTargetOriginatingAgency(String targetOriginatingAgency) {
        this.targetOriginatingAgency = targetOriginatingAgency;
        return this;
    }

    public String getObjectGroupId() {
        return objectGroupId;
    }

    public OriginatingAgencyReassignmentReportLine setObjectGroupId(String objectGroupId) {
        this.objectGroupId = objectGroupId;
        return this;
    }
}
