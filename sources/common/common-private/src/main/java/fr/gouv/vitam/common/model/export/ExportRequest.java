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

    @JsonProperty("exportWithoutObjects")
    private boolean exportWithoutObjects;

    @JsonProperty("maxSizeThreshold")
    private Long maxSizeThreshold;

    @JsonProperty("dslRequest")
    private JsonNode dslRequest;

    @JsonProperty("sedaVersion")
    private String sedaVersion = SupportedSedaVersions.SEDA_2_2.getVersion();

    @JsonProperty("useOriginalFilenames")
    private boolean useOriginalFilenames;

    @JsonProperty("exportWithTree")
    private boolean exportWithTree;

    public ExportRequest() {}

    private ExportRequest(
        DataObjectVersions dataObjectVersionToExport,
        JsonNode dslRequest,
        boolean withLogBookLFC,
        boolean withoutObjects,
        Long maxSizeThreshold,
        String sedaVersion,
        boolean useOriginalFilenames,
        boolean exportWithTree,
        ExportType exportType,
        ExportRequestParameters exportRequestParameters
    ) {
        this.dataObjectVersionToExport = dataObjectVersionToExport;
        this.dslRequest = dslRequest;
        this.exportWithLogBookLFC = withLogBookLFC;
        this.exportWithoutObjects = withoutObjects;
        this.maxSizeThreshold = maxSizeThreshold;
        this.sedaVersion = sedaVersion;
        this.useOriginalFilenames = useOriginalFilenames;
        this.exportWithTree = exportWithTree;
        this.exportType = exportType;
        this.exportRequestParameters = exportRequestParameters;
    }

    public static ExportRequest from(DipRequest dipRequest) {
        return new ExportRequest(
            dipRequest.getDataObjectVersionToExport(),
            dipRequest.getDslRequest(),
            dipRequest.isExportWithLogBookLFC(),
            dipRequest.isExportWithoutObjects(),
            dipRequest.getMaxSizeThreshold(),
            dipRequest.getSedaVersion() != null
                ? dipRequest.getSedaVersion()
                : SupportedSedaVersions.SEDA_2_3.getVersion(),
            dipRequest.isUseOriginalFilenames(),
            dipRequest.isExportWithTree(),
            ExportType.get(dipRequest.getDipExportType()),
            ExportRequestParameters.from(dipRequest.getDipRequestParameters())
        );
    }

    public static ExportRequest from(TransferRequest transferRequest) {
        return new ExportRequest(
            transferRequest.getDataObjectVersionToExport(),
            transferRequest.getDslRequest(),
            transferRequest.isTransferWithLogBookLFC(),
            transferRequest.isTransferWithoutObjects(),
            transferRequest.getMaxSizeThreshold(),
            transferRequest.getSedaVersion() != null
                ? transferRequest.getSedaVersion()
                : SupportedSedaVersions.SEDA_2_3.getVersion(),
            false,
            false,
            ExportType.ArchiveTransfer,
            ExportRequestParameters.from(transferRequest.getTransferRequestParameters())
        );
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

    public boolean isExportWithoutObjects() {
        return exportWithoutObjects;
    }

    public void setExportWithoutObjects(boolean exportWithoutObjects) {
        this.exportWithoutObjects = exportWithoutObjects;
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

    public boolean isUseOriginalFilenames() {
        return useOriginalFilenames;
    }

    public void setUseOriginalFilenames(boolean useOriginalFilenames) {
        this.useOriginalFilenames = useOriginalFilenames;
    }

    public boolean isExportWithTree() {
        return exportWithTree;
    }

    public void setExportWithTree(boolean exportWithTree) {
        this.exportWithTree = exportWithTree;
    }
}
