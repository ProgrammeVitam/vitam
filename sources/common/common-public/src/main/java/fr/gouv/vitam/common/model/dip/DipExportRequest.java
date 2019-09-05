package fr.gouv.vitam.common.model.dip;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

public class DipExportRequest {
    @JsonProperty("dataObjectVersionToExport")
    private DataObjectVersions  dataObjectVersionToExport;

    @JsonProperty("dslRequest")
    private JsonNode dslRequest;

    @JsonProperty("exportWithLogBookLFC")
    private boolean exportWithLogBookLFC;

    public DipExportRequest() {}

    public DipExportRequest(DataObjectVersions dataObjectVersionToExport, JsonNode dslRequest) {
        this(dataObjectVersionToExport, dslRequest, false);
    }

    public DipExportRequest(DataObjectVersions dataObjectVersionToExport, JsonNode dslRequest, boolean withLogBookLFC) {
        this.dataObjectVersionToExport = dataObjectVersionToExport;
        this.dslRequest = dslRequest;
        this.exportWithLogBookLFC = withLogBookLFC;
    }

    public DipExportRequest(JsonNode dslRequest) {
        this.dslRequest = dslRequest;
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

    public boolean isExportWithLogBookLFC() {
        return exportWithLogBookLFC;
    }

    public void setExportWithLogBookLFC(boolean exportWithLogBookLFC) {
        this.exportWithLogBookLFC = exportWithLogBookLFC;
    }
}
