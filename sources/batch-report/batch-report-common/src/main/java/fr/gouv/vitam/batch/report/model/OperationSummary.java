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
import com.fasterxml.jackson.databind.JsonNode;

public class OperationSummary {

    @JsonProperty("tenant")
    private Integer tenant;

    @JsonProperty("evId")
    private String evId;

    @JsonProperty("evType")
    private String evType;

    @JsonProperty("outcome")
    private String outcome;

    @JsonProperty("outDetail")
    private String outDetail;

    @JsonProperty("outMsg")
    private String outMsg;

    @JsonProperty("rightsStatementIdentifier")
    private JsonNode rightsStatementIdentifier;

    @JsonProperty("evDetData")
    private JsonNode evDetData;

    public OperationSummary() {
        // Empty constructor for deserialization
    }

    public OperationSummary(
        Integer tenant,
        String evId,
        String evType,
        String outcome,
        String outDetail,
        String outMsg,
        JsonNode rSI,
        JsonNode evDetData
    ) {
        this.tenant = tenant;
        this.evId = evId;
        this.evType = evType;
        this.outcome = outcome;
        this.outDetail = outDetail;
        this.outMsg = outMsg;
        this.rightsStatementIdentifier = rSI;
        this.evDetData = evDetData;
    }

    public Integer getTenant() {
        return tenant;
    }

    public void setTenant(Integer tenant) {
        this.tenant = tenant;
    }

    public String getEvId() {
        return evId;
    }

    public void setEvId(String evId) {
        this.evId = evId;
    }

    public String getEvType() {
        return evType;
    }

    public void setEvType(String evType) {
        this.evType = evType;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public String getOutDetail() {
        return outDetail;
    }

    public void setOutDetail(String outDetail) {
        this.outDetail = outDetail;
    }

    public String getOutMsg() {
        return outMsg;
    }

    public void setOutMsg(String outMsg) {
        this.outMsg = outMsg;
    }

    public JsonNode getRightsStatementIdentifier() {
        return rightsStatementIdentifier;
    }

    public void setRightsStatementIdentifier(JsonNode rightsStatementIdentifier) {
        this.rightsStatementIdentifier = rightsStatementIdentifier;
    }

    public JsonNode getEvDetData() {
        return evDetData;
    }

    public void setEvDetData(JsonNode evDetData) {
        this.evDetData = evDetData;
    }
}
