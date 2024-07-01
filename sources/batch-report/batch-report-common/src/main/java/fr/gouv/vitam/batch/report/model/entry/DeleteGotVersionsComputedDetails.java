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

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)
public class DeleteGotVersionsComputedDetails {

    public static final String TOTAL_OBJECTS = "totalObjects";
    public static final String TOTAL_SIZE = "totalSize";

    private final String opc;
    private final long totalSize;
    private final int totalObjects;

    @JsonCreator
    public DeleteGotVersionsComputedDetails(
        @JsonProperty("_id") String opc,
        @JsonProperty(TOTAL_SIZE) long totalSize,
        @JsonProperty(TOTAL_OBJECTS) int totalObjects
    ) {
        this.opc = opc;
        this.totalSize = totalSize;
        this.totalObjects = totalObjects;
    }

    @JsonProperty("_id")
    public String getOpc() {
        return opc;
    }

    @JsonProperty(TOTAL_SIZE)
    public long getTotalSize() {
        return totalSize;
    }

    @JsonProperty(TOTAL_OBJECTS)
    public int getTotalObjects() {
        return totalObjects;
    }
}
