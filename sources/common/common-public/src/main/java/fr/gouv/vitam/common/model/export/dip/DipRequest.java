/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common.model.export.dip;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.model.dip.DataObjectVersions;

public class DipRequest {

    @JsonProperty("dataObjectVersionToExport")
    private DataObjectVersions dataObjectVersionToExport;

    @JsonProperty("dipExportType")
    private DipExportType dipExportType = DipExportType.MINIMAL;

    @JsonProperty("dipRequestParameters")
    private DipRequestParameters dipRequestParameters;

    @JsonProperty("exportWithLogBookLFC")
    private boolean exportWithLogBookLFC;

    @JsonProperty("maxSizeThreshold")
    private Long maxSizeThreshold;

    @JsonProperty("dslRequest")
    private JsonNode dslRequest;

    @JsonProperty("sedaVersion")
    private String sedaVersion;

    public DipRequest() {}

    public DipRequest(JsonNode dslRequest) {
        this.dslRequest = dslRequest;
    }

    public DipRequest(
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

    public DipRequest(
        DataObjectVersions dataObjectVersionToExport,
        JsonNode dslRequest,
        boolean withLogBookLFC,
        String sedaVersion
    ) {
        this(dataObjectVersionToExport, dslRequest, withLogBookLFC, null, sedaVersion);
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
