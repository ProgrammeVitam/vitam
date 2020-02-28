/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.vitam.common.model.preservation.OtherMetadata;

import java.util.Objects;

/**
 * DbVersionsModel
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class DbVersionsModel {

    @JsonProperty("_id")
    private String id;

    @JsonProperty("DataObjectVersion")
    private String dataObjectVersion;

    @JsonProperty("DataObjectGroupId")
    private String dataObjectGroupId;

    @JsonProperty("FormatIdentification")
    private DbFormatIdentificationModel formatIdentificationModel;

    @JsonProperty("FileInfo")
    private DbFileInfoModel fileInfoModel;

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

    @JsonProperty("_storage")
    private DbStorageModel storage;

    @JsonProperty("PhysicalDimensions")
    private PhysicalDimensionsModel physicalDimensionsModel;

    @JsonProperty("PhysicalId")
    private String physicalId;

    @JsonProperty("OtherMetadata")
    private OtherMetadata otherMetadata = new OtherMetadata();

    @JsonProperty("_opi")
    private String opi;

    public DbVersionsModel() {
        // empty constructor for deserialization
    }

    public DbVersionsModel(
        String id,
        String dataObjectVersion,
        String dataObjectGroupId,
        DbFormatIdentificationModel formatIdentificationModel,
        DbFileInfoModel fileInfoModel,
        MetadataModel metadata,
        long size,
        String uri,
        String messageDigest,
        String algorithm,
        DbStorageModel storage,
        PhysicalDimensionsModel physicalDimensionsModel,
        String physicalId,
        OtherMetadata otherMetadata,
        String opi) {

        this.id = id;
        this.dataObjectVersion = dataObjectVersion;
        this.dataObjectGroupId = dataObjectGroupId;
        this.formatIdentificationModel = formatIdentificationModel;
        this.fileInfoModel = fileInfoModel;
        this.metadata = metadata;
        this.size = size;
        this.uri = uri;
        this.messageDigest = messageDigest;
        this.algorithm = algorithm;
        this.storage = storage;
        this.physicalDimensionsModel = physicalDimensionsModel;
        this.physicalId = physicalId;
        this.otherMetadata = otherMetadata;
        this.opi = opi;
    }

    @JsonIgnore
    public static DbVersionsModel newVersionsFrom(DbVersionsModel that, OtherMetadata otherMetadata) {
        return new DbVersionsModel(
            that.id,
            that.dataObjectVersion,
            that.dataObjectGroupId,
            that.formatIdentificationModel,
            that.fileInfoModel,
            that.metadata,
            that.size,
            that.uri,
            that.messageDigest,
            that.algorithm,
            that.storage,
            that.physicalDimensionsModel,
            that.physicalId,
            otherMetadata,
            that.opi
        );
    }

    @JsonIgnore
    public static DbVersionsModel newVersionsFrom(DbVersionsModel that, DbFormatIdentificationModel format) {
        return new DbVersionsModel(
            that.id,
            that.dataObjectVersion,
            that.dataObjectGroupId,
            format,
            that.fileInfoModel,
            that.metadata,
            that.size,
            that.uri,
            that.messageDigest,
            that.algorithm,
            that.storage,
            that.physicalDimensionsModel,
            that.physicalId,
            that.otherMetadata,
            that.opi
        );
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

    public String getDataObjectGroupId() {
        return dataObjectGroupId;
    }

    public void setDataObjectGroupId(String dataObjectGroupId) {
        this.dataObjectGroupId = dataObjectGroupId;
    }

    public DbFormatIdentificationModel getFormatIdentificationModel() {
        return formatIdentificationModel;
    }

    public void setFormatIdentificationModel(DbFormatIdentificationModel formatIdentificationModel) {
        this.formatIdentificationModel = formatIdentificationModel;
    }

    public DbFileInfoModel getFileInfoModel() {
        return fileInfoModel;
    }

    public void setFileInfoModel(DbFileInfoModel fileInfoModel) {
        this.fileInfoModel = fileInfoModel;
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

    public DbStorageModel getStorage() {
        return storage;
    }

    public void setStorage(DbStorageModel storage) {
        this.storage = storage;
    }

    public PhysicalDimensionsModel getPhysicalDimensionsModel() {
        return physicalDimensionsModel;
    }

    public void setPhysicalDimensionsModel(PhysicalDimensionsModel physicalDimensionsModel) {
        this.physicalDimensionsModel = physicalDimensionsModel;
    }

    public String getPhysicalId() {
        return physicalId;
    }

    public void setPhysicalId(String physicalId) {
        this.physicalId = physicalId;
    }

    public OtherMetadata getOtherMetadata() {
        return otherMetadata;
    }

    public void setOtherMetadata(OtherMetadata otherMetadata) {
        this.otherMetadata = otherMetadata;
    }

    public String getOpi() {
        return opi;
    }

    public void setOpi(String opi) {
        this.opi = opi;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        DbVersionsModel that = (DbVersionsModel) o;
        return id.equals(that.id)
            && dataObjectVersion.equals(that.dataObjectVersion)
            && dataObjectGroupId.equals(that.dataObjectGroupId)
            && otherMetadata.equals(that.otherMetadata)
            && formatIdentificationModel.equals(that.formatIdentificationModel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, dataObjectVersion, dataObjectGroupId, formatIdentificationModel);
    }
}
