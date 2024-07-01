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
package fr.gouv.vitam.common.model.administration;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class RegisterValueEventModel {

    public static final String OPERATION = "Opc";
    public static final String OPERATION_TYPE = "OpType";
    public static final String TOTAL_GOTS = "Gots";
    public static final String TOTAL_UNITS = "Units";
    public static final String TOTAL_OBJECTS = "Objects";
    public static final String TOTAL_OBJECTS_SIZE = "ObjSize";
    public static final String CREATION_DATE = "CreationDate";

    @JsonProperty(OPERATION)
    private String operation;

    @JsonProperty(OPERATION_TYPE)
    private String operationType;

    /**
     * archive number
     */
    @JsonProperty(TOTAL_GOTS)
    private long totalGots = 0l;

    /**
     * archive unit number
     */
    @JsonProperty(TOTAL_UNITS)
    private long totalUnits = 0l;

    /**
     * archive object number
     */
    @JsonProperty(TOTAL_OBJECTS)
    private long totalObjects = 0l;

    /**
     * archive object size
     */
    @JsonProperty(TOTAL_OBJECTS_SIZE)
    private long objectSize = 0l;

    @JsonProperty(CREATION_DATE)
    private String creationdate;

    /**
     * Constructor without fields
     *
     * use for jackson
     */
    public RegisterValueEventModel() {}

    /**
     * Get operation id
     *
     * @return operation
     */
    public String getOperation() {
        return operation;
    }

    /**
     * Set operation id
     *
     * @param operation
     */
    public RegisterValueEventModel setOperation(String operation) {
        this.operation = operation;
        return this;
    }

    /**
     * Get the operation type (INGEST, ELIMINATION, ...)
     *
     * @return total
     */
    public String getOperationType() {
        return operationType;
    }

    /**
     * @param operationType
     * @return RegisterValueEventModel
     */
    public RegisterValueEventModel setOperationType(String operationType) {
        this.operationType = operationType;
        return this;
    }

    public long getTotalGots() {
        return totalGots;
    }

    public RegisterValueEventModel setTotalGots(long totalGots) {
        this.totalGots = totalGots;
        return this;
    }

    public long getTotalUnits() {
        return totalUnits;
    }

    public RegisterValueEventModel setTotalUnits(long totalUnits) {
        this.totalUnits = totalUnits;
        return this;
    }

    public long getTotalObjects() {
        return totalObjects;
    }

    public RegisterValueEventModel setTotalObjects(long totalObjects) {
        this.totalObjects = totalObjects;
        return this;
    }

    public long getObjectSize() {
        return objectSize;
    }

    public RegisterValueEventModel setObjectSize(long objectSize) {
        this.objectSize = objectSize;
        return this;
    }

    /**
     * Get create date of this instance
     *
     * @return creationdate
     */
    public String getCreationdate() {
        return creationdate;
    }

    /**
     * Set creationDate
     *
     * @param creationdate
     */
    public RegisterValueEventModel setCreationdate(String creationdate) {
        this.creationdate = creationdate;
        return this;
    }
}
