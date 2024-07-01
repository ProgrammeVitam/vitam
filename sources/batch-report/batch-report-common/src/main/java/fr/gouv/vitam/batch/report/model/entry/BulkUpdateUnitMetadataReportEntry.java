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
import fr.gouv.vitam.common.model.StatusCode;

import java.util.Objects;

import static fr.gouv.vitam.batch.report.model.ReportType.BULK_UPDATE_UNIT;

public class BulkUpdateUnitMetadataReportEntry extends ReportEntry {

    public static final String RESULT_KEY = "resultKey";
    public static final String PROCESS_ID = "processId";
    public static final String TENANT_ID = "_tenant";
    public static final String STATUS = "status";
    public static final String QUERY = "query";
    public static final String UNIT_ID = "unitId";
    public static final String MESSAGE = "message";
    public static final String STATUS_ID = "statusId";

    private final String processId;
    private final Integer tenantId;
    private final String resultKey;
    private final StatusCode status;
    private final String query;
    private final String unitId;
    private final String message;
    private final Integer statusId;

    @JsonCreator
    public BulkUpdateUnitMetadataReportEntry(
        @JsonProperty(TENANT_ID) Integer tenantId,
        @JsonProperty(PROCESS_ID) String processId,
        @JsonProperty(DETAIL_ID) String detailId,
        @JsonProperty(QUERY) String query,
        @JsonProperty(UNIT_ID) String unitId,
        @JsonProperty(RESULT_KEY) String resultKey,
        @JsonProperty(STATUS) StatusCode status,
        @JsonProperty(OUTCOME) String outcome,
        @JsonProperty(MESSAGE) String message
    ) {
        super(outcome, BULK_UPDATE_UNIT.name(), detailId);
        this.processId = processId;
        this.tenantId = tenantId;
        this.resultKey = resultKey;
        this.status = status;
        this.query = query;
        this.unitId = unitId;
        this.message = message;
        this.statusId = status.ordinal();
    }

    @JsonProperty(RESULT_KEY)
    public String getResultKey() {
        return resultKey;
    }

    @JsonProperty(STATUS)
    public StatusCode getStatus() {
        return status;
    }

    @JsonProperty(MESSAGE)
    public String getMessage() {
        return message;
    }

    @JsonProperty(TENANT_ID)
    public Integer getTenantId() {
        return tenantId;
    }

    @JsonProperty(PROCESS_ID)
    public String getProcessId() {
        return processId;
    }

    @JsonProperty(STATUS_ID)
    public Integer getStatusId() {
        return statusId;
    }

    @JsonProperty(QUERY)
    public String getQuery() {
        return query;
    }

    @JsonProperty(UNIT_ID)
    public String getUnitId() {
        return unitId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BulkUpdateUnitMetadataReportEntry that = (BulkUpdateUnitMetadataReportEntry) o;
        return (
            processId.equals(that.processId) &&
            tenantId.equals(that.tenantId) &&
            getDetailId().equals(that.getDetailId()) &&
            query.equals(that.query) &&
            unitId.equals(that.unitId) &&
            resultKey.equals(that.resultKey) &&
            status == that.status &&
            message.equals(that.message)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(processId, tenantId, getDetailId(), query, unitId, resultKey, status, message);
    }
}
