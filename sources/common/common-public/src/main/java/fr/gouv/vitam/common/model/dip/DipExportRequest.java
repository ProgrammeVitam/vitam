package fr.gouv.vitam.common.model.dip;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

public class DipExportRequest {
    public static final String DIP_REQUEST_FILE_NAME = "dip_export_query.json";

    @JsonProperty("dataObjectVersionToExport")
    private DataObjectVersions dataObjectVersionToExport;

    @JsonProperty("exportType")
    private ExportType exportType = ExportType.MinimalArchiveDeliveryRequestReply;

    @JsonProperty("exportRequestParameters")
    private ExportRequestParameters exportRequestParameters;


    @JsonProperty("exportWithLogBookLFC")
    private boolean exportWithLogBookLFC;

    @JsonProperty("dslRequest")
    private JsonNode dslRequest;


    public DipExportRequest() {
    }


    public DipExportRequest(JsonNode dslRequest) {
        this.dslRequest = dslRequest;
    }

    public DipExportRequest(DataObjectVersions dataObjectVersionToExport, JsonNode dslRequest, boolean withLogBookLFC) {
        this.dataObjectVersionToExport = dataObjectVersionToExport;
        this.dslRequest = dslRequest;
        this.exportWithLogBookLFC = withLogBookLFC;
    }

    public DataObjectVersions getDataObjectVersionToExport() {
        return dataObjectVersionToExport;
    }

    public void setDataObjectVersionToExport(DataObjectVersions dataObjectVersionToExport) {
        this.dataObjectVersionToExport = dataObjectVersionToExport;
    }

    public JsonNode getDslRequest() {
        return dslRequest;
    }

    public void setDslRequest(JsonNode dslRequest) {
        this.dslRequest = dslRequest;
    }

    public ExportType getExportType() {
        return exportType;
    }

    public void setExportType(ExportType exportType) {
        this.exportType = exportType;
    }

    public ExportRequestParameters getExportRequestParameters() {
        return exportRequestParameters;
    }

    public void setExportRequestParameters(ExportRequestParameters exportRequestParameters) {
        this.exportRequestParameters = exportRequestParameters;
    }

    public boolean isExportWithLogBookLFC() {
        return exportWithLogBookLFC;
    }

    public void setExportWithLogBookLFC(boolean exportWithLogBookLFC) {
        this.exportWithLogBookLFC = exportWithLogBookLFC;
    }
}
