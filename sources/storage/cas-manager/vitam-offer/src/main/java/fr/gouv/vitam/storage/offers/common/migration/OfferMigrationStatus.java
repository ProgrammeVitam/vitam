/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 *******************************************************************************/
package fr.gouv.vitam.storage.offers.common.migration;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.vitam.common.model.StatusCode;

public class OfferMigrationStatus {

    @JsonProperty("requestId")
    private String requestId;
    @JsonProperty("statusCode")
    private StatusCode statusCode;
    @JsonProperty("startDate")
    private String startDate;
    @JsonProperty("endDate")
    private String endDate;
    @JsonProperty("startOffset")
    private Long startOffset;
    @JsonProperty("currentOffset")
    private Long currentOffset;

    public OfferMigrationStatus(String requestId, StatusCode statusCode, String startDate, String endDate,
        Long startOffset, Long currentOffset) {
        this.requestId = requestId;
        this.statusCode = statusCode;
        this.startDate = startDate;
        this.endDate = endDate;
        this.startOffset = startOffset;
        this.currentOffset = currentOffset;
    }

    public String getRequestId() {
        return requestId;
    }

    public OfferMigrationStatus setRequestId(String requestId) {
        this.requestId = requestId;
        return this;
    }

    public StatusCode getStatusCode() {
        return statusCode;
    }

    public OfferMigrationStatus setStatusCode(StatusCode statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    public String getStartDate() {
        return startDate;
    }

    public OfferMigrationStatus setStartDate(String startDate) {
        this.startDate = startDate;
        return this;
    }

    public String getEndDate() {
        return endDate;
    }

    public OfferMigrationStatus setEndDate(String endDate) {
        this.endDate = endDate;
        return this;
    }

    public Long getStartOffset() {
        return startOffset;
    }

    public OfferMigrationStatus setStartOffset(Long startOffset) {
        this.startOffset = startOffset;
        return this;
    }

    public Long getCurrentOffset() {
        return currentOffset;
    }

    public OfferMigrationStatus setCurrentOffset(Long currentOffset) {
        this.currentOffset = currentOffset;
        return this;
    }
}
