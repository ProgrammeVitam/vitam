package fr.gouv.vitam.common.model.dip;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

public class TransferRequest {
    @JsonProperty("dataObjectVersionToExport")
    private DataObjectVersions dataObjectVersionToExport;

    @JsonProperty("transferRequestParameters")
    private TransferRequestParameters transferRequestParameters;

    @JsonProperty("transferWithLogBookLFC")
    private boolean transferWithLogBookLFC;

    @JsonProperty("dslRequest")
    private JsonNode dslRequest;


    public TransferRequest() {
    }


    public TransferRequest(JsonNode dslRequest) {
        this.dslRequest = dslRequest;
    }

    public TransferRequest(DataObjectVersions dataObjectVersionToExport, JsonNode dslRequest, boolean withLogBookLFC) {
        this.dataObjectVersionToExport = dataObjectVersionToExport;
        this.dslRequest = dslRequest;
        this.transferWithLogBookLFC = withLogBookLFC;
    }

    public DataObjectVersions getDataObjectVersionToExport() {
        return dataObjectVersionToExport;
    }

    public void setDataObjectVersionToExport(DataObjectVersions dataObjectVersionToExport) {
        this.dataObjectVersionToExport = dataObjectVersionToExport;
    }

    public TransferRequestParameters getTransferRequestParameters() {
        return transferRequestParameters;
    }

    public void setTransferRequestParameters(TransferRequestParameters transferRequestParameters) {
        this.transferRequestParameters = transferRequestParameters;
    }

    public boolean isTransferWithLogBookLFC() {
        return transferWithLogBookLFC;
    }

    public void setTransferWithLogBookLFC(boolean transferWithLogBookLFC) {
        this.transferWithLogBookLFC = transferWithLogBookLFC;
    }

    public JsonNode getDslRequest() {
        return dslRequest;
    }

    public void setDslRequest(JsonNode dslRequest) {
        this.dslRequest = dslRequest;
    }
}
