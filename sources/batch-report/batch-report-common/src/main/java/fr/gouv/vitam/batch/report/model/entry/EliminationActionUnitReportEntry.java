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
import com.fasterxml.jackson.annotation.JsonProperty;

public class EliminationActionUnitReportEntry {

    private static final String ID = "id";
    private static final String ORIGINATING_AGENCY = "originatingAgency";
    private static final String OPI = "opi";
    private static final String OBJECT_GROUP = "objectGroupId";
    private static final String STATUS = "status";

    private final String id;
    private final String originatingAgency;
    private final String initialOperation;
    private final String objectGroupId;
    private final String status;

    @JsonCreator
    public EliminationActionUnitReportEntry(
        @JsonProperty(ID) String id,
        @JsonProperty(ORIGINATING_AGENCY) String originatingAgency,
        @JsonProperty(OPI) String initialOperation,
        @JsonProperty(OBJECT_GROUP) String objectGroupId,
        @JsonProperty(STATUS) String status
    ) {
        this.id = id;
        this.originatingAgency = originatingAgency;
        this.initialOperation = initialOperation;
        this.objectGroupId = objectGroupId;
        this.status = status;
    }

    @JsonProperty(ID)
    public String getId() {
        return id;
    }

    @JsonProperty(ORIGINATING_AGENCY)
    public String getOriginatingAgency() {
        return originatingAgency;
    }

    @JsonProperty(OPI)
    public String getInitialOperation() {
        return initialOperation;
    }

    @JsonProperty(OBJECT_GROUP)
    public String getObjectGroupId() {
        return objectGroupId;
    }

    @JsonProperty(STATUS)
    public String getStatus() {
        return status;
    }
}
