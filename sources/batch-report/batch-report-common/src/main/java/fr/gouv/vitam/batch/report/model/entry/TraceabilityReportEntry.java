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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.vitam.batch.report.model.TraceabilityError;

import java.util.Map;
import java.util.Objects;

public class TraceabilityReportEntry {

    public static final String OPERATION_ID = "id";
    public static final String OPERATION_TYPE = "operationType";
    public static final String STATUS = "status";
    public static final String FILE_ID = "fileId";
    public static final String MESSAGE = "message";
    public static final String TRACEABILITY_ERROR = "error";
    public static final String SECURED_HASH = "securedHash";
    public static final String OFFERS_HASHES = "offersHashes";
    public static final String EXTRA_DATA = "extraData";

    @JsonProperty(OPERATION_ID)
    private String operationId;

    @JsonProperty(OPERATION_TYPE)
    private String operationType;

    @JsonProperty(STATUS)
    private String status;

    @JsonProperty(TRACEABILITY_ERROR)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private TraceabilityError error;

    @JsonProperty(MESSAGE)
    private String message;

    @JsonProperty(SECURED_HASH)
    private String securedHash;

    @JsonProperty(OFFERS_HASHES)
    private Map<String, String> offersHashes;

    @JsonProperty(FILE_ID)
    private String fileId;

    @JsonProperty(EXTRA_DATA)
    private Map<String, Object> extraData;

    public TraceabilityReportEntry(
        @JsonProperty(OPERATION_ID) String operationId,
        @JsonProperty(OPERATION_TYPE) String operationType,
        @JsonProperty(STATUS) String status,
        @JsonProperty(MESSAGE) String message,
        @JsonProperty(TRACEABILITY_ERROR) TraceabilityError error,
        @JsonProperty(SECURED_HASH) String securedHash,
        @JsonProperty(OFFERS_HASHES) Map<String, String> offersHashes,
        @JsonProperty(FILE_ID) String fileId,
        @JsonProperty(EXTRA_DATA) Map<String, Object> extraData
    ) {
        this.operationId = operationId;
        this.operationType = operationType;
        this.status = status;
        this.message = message;
        this.error = error;
        this.securedHash = securedHash;
        this.offersHashes = offersHashes;
        this.fileId = fileId;
        this.extraData = extraData;
    }

    public String getOperationId() {
        return operationId;
    }

    public TraceabilityReportEntry setOperationId(String operationId) {
        this.operationId = operationId;
        return this;
    }

    public String getOperationType() {
        return operationType;
    }

    public TraceabilityReportEntry setOperationType(String operationType) {
        this.operationType = operationType;
        return this;
    }

    public String getStatus() {
        return status;
    }

    public TraceabilityReportEntry setStatus(String status) {
        this.status = status;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public TraceabilityReportEntry setMessage(String message) {
        this.message = message;
        return this;
    }

    public String getSecuredHash() {
        return securedHash;
    }

    public TraceabilityReportEntry setSecuredHash(String securedHash) {
        this.securedHash = securedHash;
        return this;
    }

    public Map<String, String> getOffersHashes() {
        return offersHashes;
    }

    public TraceabilityReportEntry setOffersHashes(Map<String, String> offersHashes) {
        this.offersHashes = offersHashes;
        return this;
    }

    public String getFileId() {
        return fileId;
    }

    public TraceabilityReportEntry setFileId(String fileId) {
        this.fileId = fileId;
        return this;
    }

    public Map<String, Object> getExtraData() {
        return extraData;
    }

    public TraceabilityReportEntry setExtraData(Map<String, Object> extraData) {
        this.extraData = extraData;
        return this;
    }

    public TraceabilityReportEntry appendExtraData(Map<String, Object> map) {
        if (Objects.isNull(this.extraData)) {
            setExtraData(map);
        } else {
            this.extraData.putAll(map);
        }
        return this;
    }

    public TraceabilityError getError() {
        return error;
    }

    public TraceabilityReportEntry setError(TraceabilityError error) {
        this.error = error;
        return this;
    }
}
