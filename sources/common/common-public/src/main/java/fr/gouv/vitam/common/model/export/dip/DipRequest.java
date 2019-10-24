package fr.gouv.vitam.common.model.export.dip;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.model.dip.DataObjectVersions;
import fr.gouv.vitam.common.model.dip.DipExportRequest;

public class DipRequest {
    @JsonProperty("dataObjectVersionToExport")
    private DataObjectVersions dataObjectVersionToExport;

    @JsonProperty("dipExportType")
    private DipExportType dipExportType = DipExportType.MINIMAL;

    @JsonProperty("dipRequestParameters")
    private DipRequestParameters dipRequestParameters;


    @JsonProperty("exportWithLogBookLFC")
    private boolean exportWithLogBookLFC;

    @JsonProperty("dslRequest")
    private JsonNode dslRequest;


    public DipRequest() {
    }


    public DipRequest(JsonNode dslRequest) {
        this.dslRequest = dslRequest;
    }

    public DipRequest(DataObjectVersions dataObjectVersionToExport, JsonNode dslRequest, boolean withLogBookLFC) {
        this.dataObjectVersionToExport = dataObjectVersionToExport;
        this.dslRequest = dslRequest;
        this.exportWithLogBookLFC = withLogBookLFC;
    }

    public DipRequest(DipExportRequest dipExportRequest) {
        this(dipExportRequest.getDataObjectVersionToExport(), dipExportRequest.getDslRequest(),
            dipExportRequest.isExportWithLogBookLFC());
        this.dipExportType = DipExportType.MINIMAL;
    }

    public DataObjectVersions getDataObjectVersionToExport() {
        return dataObjectVersionToExport;
    }

    public void setDataObjectVersionToExport(DataObjectVersions dataObjectVersionToExport) {
        this.dataObjectVersionToExport = dataObjectVersionToExport;
    }

    public DipExportType getDipExportType() {
        return dipExportType;
    }

    public void setDipExportType(DipExportType dipExportType) {
        this.dipExportType = dipExportType;
    }

    public DipRequestParameters getDipRequestParameters() {
        return dipRequestParameters;
    }

    public void setDipRequestParameters(DipRequestParameters dipRequestParameters) {
        this.dipRequestParameters = dipRequestParameters;
    }

    public boolean isExportWithLogBookLFC() {
        return exportWithLogBookLFC;
    }

    public void setExportWithLogBookLFC(boolean exportWithLogBookLFC) {
        this.exportWithLogBookLFC = exportWithLogBookLFC;
    }

    public JsonNode getDslRequest() {
        return dslRequest;
    }

    public void setDslRequest(JsonNode dslRequest) {
        this.dslRequest = dslRequest;
    }
}
