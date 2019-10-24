package fr.gouv.vitam.common.model.export;

import fr.gouv.vitam.common.model.export.dip.DipExportType;

public enum ExportType {
    MinimalArchiveDeliveryRequestReply,
    ArchiveDeliveryRequestReply,
    ArchiveTransfer;

    public static ExportType get(DipExportType dipExportType) {
        switch (dipExportType) {
            case MINIMAL:
                return MinimalArchiveDeliveryRequestReply;
            case FULL:
            default:
                return ArchiveDeliveryRequestReply;
        }
    }
}
