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
package fr.gouv.vitam.common.model.objectgroup;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.Integer.parseInt;
import static java.util.Arrays.asList;

/**
 * Object mapping VersionsResponse
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VersionsModel {

    @JsonProperty("#rank")
    private int rank;

    @JsonProperty("#id")
    private String id;

    @JsonProperty("DataObjectVersion")
    private String dataObjectVersion;

    @JsonIgnore
    private final Map<String, Object> any = new HashMap<>();

    @JsonProperty("DataObjectGroupId")
    private String dataObjectGroupId;

    @JsonProperty("FormatIdentification")
    private FormatIdentificationModel formatIdentification;

    @JsonProperty("FileInfo")
    private FileInfoModel fileInfoModel;

    @JsonProperty("Metadata")
    private MetadataModel metadata;

    @JsonProperty("Size")
    private long size;

    @JsonProperty("Uri")
    private String uri;

    @JsonProperty("MessageDigest")
    private String messageDigest;

    @JsonProperty("Algorithm")
    private String algorithm;

    @JsonProperty("#storage")
    private StorageJson storage;

    @JsonProperty("PhysicalDimensions")
    private PhysicalDimensionsModel physicalDimensionsModel;

    @JsonProperty("PhysicalId")
    private String physicalId;

    @JsonProperty("OtherMetadata")
    private Map<String, Object> otherMetadata = new HashMap<>();

    @JsonProperty("#opi")
    private String opi;
    @JsonProperty("DataObjectProfile")
    private String dataObjectProfile;

    @JsonAnyGetter
    public Map<String, Object> getAny() {
        return any;
    }

    @JsonAnySetter
    public void setAny(String key, Object value) {
        this.any.put(key, value);
    }

    public Map<String, Object> getOtherMetadata() {
        return otherMetadata;
    }

    public void setOtherMetadata(Map<String, Object> otherMetadata) {
        this.otherMetadata = otherMetadata;
    }

    public String getPhysicalId() {
        return physicalId;
    }

    public void setPhysicalId(String physicalId) {
        this.physicalId = physicalId;
    }

    public PhysicalDimensionsModel getPhysicalDimensionsModel() {
        return physicalDimensionsModel;
    }

    public void setPhysicalDimensionsModel(
        PhysicalDimensionsModel physicalDimensionsModel) {
        this.physicalDimensionsModel = physicalDimensionsModel;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDataObjectVersion() {
        return dataObjectVersion;
    }

    public void setDataObjectVersion(String dataObjectVersion) {
        this.dataObjectVersion = dataObjectVersion;
    }

    public String getDataObjectProfile() {
        return dataObjectProfile;
    }

    public void setDataObjectProfile(String dataObjectProfile) {
        this.dataObjectProfile = dataObjectProfile;
    }

    public FormatIdentificationModel getFormatIdentification() {
        return formatIdentification;
    }

    public void setFormatIdentification(FormatIdentificationModel formatIdentification) {
        this.formatIdentification = formatIdentification;
    }

    public FileInfoModel getFileInfoModel() {
        return fileInfoModel;
    }

    public void setFileInfoModel(FileInfoModel fileInfoModel) {
        this.fileInfoModel = fileInfoModel;
    }

    public String getDataObjectGroupId() {
        return dataObjectGroupId;
    }

    public void setDataObjectGroupId(String dataObjectGroupId) {
        this.dataObjectGroupId = dataObjectGroupId;
    }

    public MetadataModel getMetadata() {
        return metadata;
    }

    public void setMetadata(MetadataModel metadata) {
        this.metadata = metadata;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getMessageDigest() {
        return messageDigest;
    }

    public void setMessageDigest(String messageDigest) {
        this.messageDigest = messageDigest;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public StorageJson getStorage() {
        return storage;
    }

    public void setStorage(StorageJson storage) {
        this.storage = storage;
    }

    public String getOpi() {
        return opi;
    }

    public void setOpi(String opi) {
        this.opi = opi;
    }

    @JsonIgnore
    public int getDataVersion() {
        List<String> split = asList(dataObjectVersion.split("_"));
        return parseInt(split.get(1));
    }

    @Override
    public String toString() {
        return "VersionsModel{" +
            "rank=" + rank +
            ", id='" + id + '\'' +
            ", dataObjectVersion='" + dataObjectVersion + '\'' +
            ", dataObjectProfile='" + dataObjectProfile + '\'' +
            ", dataObjectGroupId='" + dataObjectGroupId + '\'' +
            ", formatIdentification=" + formatIdentification +
            ", fileInfoModel=" + fileInfoModel +
            ", metadata=" + metadata +
            ", size=" + size +
            ", uri='" + uri + '\'' +
            ", messageDigest='" + messageDigest + '\'' +
            ", algorithm='" + algorithm + '\'' +
            ", storage=" + storage +
            ", physicalDimensionsModel=" + physicalDimensionsModel +
            ", physicalId='" + physicalId + '\'' +
            ", otherMetadata=" + otherMetadata +
            ", opi='" + opi + '\'' +
            ", any=" + any +
            '}';
    }
}
