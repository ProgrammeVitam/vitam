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
package fr.gouv.vitam.common.model.export;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.model.dip.DataObjectVersions;
import fr.gouv.vitam.common.model.export.dip.DipRequest;
import fr.gouv.vitam.common.model.export.transfer.TransferRequest;
import fr.gouv.vitam.common.utils.SupportedSedaVersions;

public class ExportRequest {

    public static final String EXPORT_QUERY_FILE_NAME = "export_query.json";

    @JsonProperty("dataObjectVersionToExport")
    private DataObjectVersions dataObjectVersionToExport;

    @JsonProperty("exportType")
    private ExportType exportType = ExportType.MinimalArchiveDeliveryRequestReply;

    @JsonProperty("exportRequestParameters")
    private ExportRequestParameters exportRequestParameters;

    @JsonProperty("exportWithLogBookLFC")
    private boolean exportWithLogBookLFC;

    @JsonProperty("maxSizeThreshold")
    private Long maxSizeThreshold;

    @JsonProperty("dslRequest")
    private JsonNode dslRequest;

    @JsonProperty("sedaVersion")
    private String sedaVersion = SupportedSedaVersions.SEDA_2_2.getVersion();

    public ExportRequest() {}

    public ExportRequest(JsonNode dslRequest) {
        this.dslRequest = dslRequest;
    }

    public ExportRequest(
        DataObjectVersions dataObjectVersionToExport,
        JsonNode dslRequest,
        boolean withLogBookLFC,
        Long maxSizeThreshold,
        String sedaVersion
    ) {
        this.dataObjectVersionToExport = dataObjectVersionToExport;
        this.dslRequest = dslRequest;
        this.exportWithLogBookLFC = withLogBookLFC;
        this.maxSizeThreshold = maxSizeThreshold;
        this.sedaVersion = sedaVersion;
    }

    /**
     * Seda version to export is setted to default value "2.2"
     * @param dataObjectVersionToExport
     * @param dslRequest
     * @param withLogBookLFC
     */
    public ExportRequest(DataObjectVersions dataObjectVersionToExport, JsonNode dslRequest, boolean withLogBookLFC) {
        this(dataObjectVersionToExport, dslRequest, withLogBookLFC, null, SupportedSedaVersions.SEDA_2_2.getVersion());
    }

    public static ExportRequest from(DipRequest dipRequest) {
        ExportRequest exportRequest = new ExportRequest(
            dipRequest.getDataObjectVersionToExport(),
            dipRequest.getDslRequest(),
            dipRequest.isExportWithLogBookLFC()
        );
        exportRequest.setExportType(ExportType.get(dipRequest.getDipExportType()));
        exportRequest.setExportRequestParameters(ExportRequestParameters.from(dipRequest.getDipRequestParameters()));
        exportRequest.setMaxSizeThreshold(dipRequest.getMaxSizeThreshold());
        exportRequest.setSedaVersion(
            dipRequest.getSedaVersion() != null
                ? dipRequest.getSedaVersion()
                : SupportedSedaVersions.SEDA_2_2.getVersion()
        );

        return exportRequest;
    }

    public static ExportRequest from(TransferRequest transferRequest) {
        ExportRequest exportRequest = new ExportRequest(
            transferRequest.getDataObjectVersionToExport(),
            transferRequest.getDslRequest(),
            transferRequest.isTransferWithLogBookLFC()
        );
        exportRequest.setExportType(ExportType.ArchiveTransfer);
        exportRequest.setExportRequestParameters(
            ExportRequestParameters.from(transferRequest.getTransferRequestParameters())
        );
        exportRequest.setMaxSizeThreshold(transferRequest.getMaxSizeThreshold());
        exportRequest.setSedaVersion(
            transferRequest.getSedaVersion() != null
                ? transferRequest.getSedaVersion()
                : SupportedSedaVersions.SEDA_2_2.getVersion()
        );

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

    public Long getMaxSizeThreshold() {
        return maxSizeThreshold;
    }

    public void setMaxSizeThreshold(Long maxSizeThreshold) {
        this.maxSizeThreshold = maxSizeThreshold;
    }

    public String getSedaVersion() {
        return sedaVersion;
    }

    public void setSedaVersion(String sedaVersion) {
        this.sedaVersion = sedaVersion;
    }
}
