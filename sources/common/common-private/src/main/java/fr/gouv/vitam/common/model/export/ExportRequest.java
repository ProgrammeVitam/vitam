package fr.gouv.vitam.common.model.export;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.model.dip.DataObjectVersions;
import fr.gouv.vitam.common.model.export.dip.DipRequest;
import fr.gouv.vitam.common.model.export.transfer.TransferRequest;

public class ExportRequest {
    public static final String DIP_REQUEST_FILE_NAME = "export_query.json";

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


    public ExportRequest() {
    }


    public ExportRequest(JsonNode dslRequest) {
        this.dslRequest = dslRequest;
    }

    public ExportRequest(DataObjectVersions dataObjectVersionToExport, JsonNode dslRequest, boolean withLogBookLFC) {
        this.dataObjectVersionToExport = dataObjectVersionToExport;
        this.dslRequest = dslRequest;
        this.exportWithLogBookLFC = withLogBookLFC;
    }

    public static ExportRequest from(DipRequest dipRequest) {
        ExportRequest exportRequest =
            new ExportRequest(dipRequest.getDataObjectVersionToExport(), dipRequest.getDslRequest(),
                dipRequest.isExportWithLogBookLFC());
        exportRequest.setExportType(ExportType.get(dipRequest.getDipExportType()));
        exportRequest.setExportRequestParameters(ExportRequestParameters.from(dipRequest.getDipRequestParameters()));

        return exportRequest;
    }

    public static ExportRequest from(TransferRequest transferRequest) {
        ExportRequest exportRequest =
            new ExportRequest(transferRequest.getDataObjectVersionToExport(), transferRequest.getDslRequest(),
                transferRequest.isTransferWithLogBookLFC());
        exportRequest.setExportType(ExportType.ArchiveTransfer);
        exportRequest
            .setExportRequestParameters(ExportRequestParameters.from(transferRequest.getTransferRequestParameters()));

        return exportRequest;
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
