package fr.gouv.vitam.common.model.dip;

public enum DipExportType {
    MINIMAL(ExportType.MinimalArchiveDeliveryRequestReply),
    FULL(ExportType.ArchiveDeliveryRequestReply);

    private final ExportType exportType;

    DipExportType(ExportType exportType) {
        this.exportType = exportType;
    }

    public ExportType getExportType() {
        return exportType;
    }
}
