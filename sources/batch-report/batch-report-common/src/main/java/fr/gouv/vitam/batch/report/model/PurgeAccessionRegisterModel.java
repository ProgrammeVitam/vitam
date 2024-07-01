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

public class PurgeAccessionRegisterModel {

    public static final String OPI = "opi";
    public static final String OPC = "opc";
    public static final String ORIGINATING_AGENCY = "originatingAgency";
    public static final String TOTAL_UNITS = "totalUnits";
    public static final String TOTAL_OBJECT_GROUPS = "totalObjectGroups";
    public static final String TOTAL_OBJECTS = "totalObjects";
    public static final String TOTAL_SIZE = "totalSize";

    @JsonProperty(OPI)
    private String opi;

    @JsonProperty(ORIGINATING_AGENCY)
    private String originatingAgency;

    @JsonProperty(TOTAL_UNITS)
    private long totalUnits;

    @JsonProperty(TOTAL_OBJECT_GROUPS)
    private long totalObjectGroups;

    @JsonProperty(TOTAL_OBJECTS)
    private long totalObjects;

    @JsonProperty(TOTAL_SIZE)
    private long totalSize;

    public PurgeAccessionRegisterModel() {
        // Empty constructor for deserialization
    }

    public PurgeAccessionRegisterModel(
        String opi,
        String originatingAgency,
        long totalUnits,
        long totalObjectGroups,
        long totalObjects,
        long totalSize
    ) {
        this.opi = opi;
        this.originatingAgency = originatingAgency;
        this.totalUnits = totalUnits;
        this.totalObjectGroups = totalObjectGroups;
        this.totalObjects = totalObjects;
        this.totalSize = totalSize;
    }

    public String getOpi() {
        return opi;
    }

    public void setOpi(String opi) {
        this.opi = opi;
    }

    public String getOriginatingAgency() {
        return originatingAgency;
    }

    public void setOriginatingAgency(String originatingAgency) {
        this.originatingAgency = originatingAgency;
    }

    public long getTotalUnits() {
        return totalUnits;
    }

    public void setTotalUnits(long totalUnits) {
        this.totalUnits = totalUnits;
    }

    public long getTotalObjectGroups() {
        return totalObjectGroups;
    }

    public void setTotalObjectGroups(long totalObjectGroups) {
        this.totalObjectGroups = totalObjectGroups;
    }

    public long getTotalObjects() {
        return totalObjects;
    }

    public void setTotalObjects(long totalObjects) {
        this.totalObjects = totalObjects;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }
}
