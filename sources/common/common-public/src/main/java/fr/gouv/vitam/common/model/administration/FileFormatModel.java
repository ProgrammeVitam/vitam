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
package fr.gouv.vitam.common.model.administration;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.vitam.common.model.ModelConstants;

import java.util.List;

/**
 * POJO java use for mapping FileFormat
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class FileFormatModel {

    @JsonProperty(ModelConstants.HASH + ModelConstants.TAG_ID)
    private String id;

    @JsonProperty(ModelConstants.HASH + ModelConstants.TAG_VERSION)
    private Integer documentVersion;

    @JsonProperty("CreatedDate")
    private String createdDate;

    @JsonProperty("VersionPronom")
    private String versionPronom;

    @JsonProperty("UpdateDate")
    private String updateDate;

    @JsonProperty("Version")
    private String version;

    @JsonProperty("HasPriorityOverFileFormatID")
    private List<String> hasPriorityOverFileFormatIDs;

    @JsonProperty("MimeType")
    private String mimeType;

    @JsonProperty("Name")
    private String name;

    /**
     * @deprecated Unused / yagni / reserved for future use
     */
    @JsonProperty("Group")
    private String group;

    /**
     * @deprecated Unused / yagni / reserved for future use
     */
    @JsonProperty("Alert")
    private Boolean alert;

    /**
     * @deprecated Unused / yagni / reserved for future use
     */
    @JsonProperty("Comment")
    private String comment;

    @JsonProperty("Extension")
    private List<String> extensions;

    @JsonProperty("PUID")
    private String puid;

    /**
     * Constructor without fields
     */
    public FileFormatModel() {}

    /**
     * @return id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id value to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return document version (_v)
     */
    public Integer getDocumentVersion() {
        return documentVersion;
    }

    /**
     * @param documentVersion document version (_v)
     */
    public void setDocumentVersion(Integer documentVersion) {
        this.documentVersion = documentVersion;
    }

    /**
     * @return createdDate
     */
    public String getCreatedDate() {
        return createdDate;
    }

    /**
     * @param createdDate value to set
     * @return this
     */
    public FileFormatModel setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
        return this;
    }

    /**
     * @return versionPronom
     */
    public String getVersionPronom() {
        return versionPronom;
    }

    /**
     * @param versionPronom value to set
     * @return this
     */
    public FileFormatModel setVersionPronom(String versionPronom) {
        this.versionPronom = versionPronom;
        return this;
    }

    /**
     * @return version
     */
    public String getVersion() {
        return version;
    }

    /**
     * @param version value to set
     * @return this
     */
    public FileFormatModel setVersion(String version) {
        this.version = version;
        return this;
    }

    /**
     * @return hasPriorityOverFileFormatIDs
     */
    public List<String> getHasPriorityOverFileFormatIDs() {
        return hasPriorityOverFileFormatIDs;
    }

    /**
     * @param hasPriorityOverFileFormatIDs value to set
     * @return this
     */
    public FileFormatModel setHasPriorityOverFileFormatIDs(List<String> hasPriorityOverFileFormatIDs) {
        this.hasPriorityOverFileFormatIDs = hasPriorityOverFileFormatIDs;
        return this;
    }

    /**
     * @return mimeType
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * @param mimeType value to set
     * @return this
     */
    public FileFormatModel setMimeType(String mimeType) {
        this.mimeType = mimeType;
        return this;
    }

    /**
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name value to set
     * @return this
     */
    public FileFormatModel setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * @return group
     */
    public String getGroup() {
        return group;
    }

    /**
     * @param group value to set
     * @return this
     */
    public FileFormatModel setGroup(String group) {
        this.group = group;
        return this;
    }

    /**
     * @return alert
     */
    public Boolean isAlert() {
        return alert;
    }

    /**
     * @param alert value to set
     * @return this
     */
    public FileFormatModel setAlert(Boolean alert) {
        this.alert = alert;
        return this;
    }

    /**
     * @return String
     */
    public String getComment() {
        return comment;
    }

    /**
     * @param comment value to set
     * @return this
     */
    public FileFormatModel setComment(String comment) {
        this.comment = comment;
        return this;
    }

    /**
     * @return List of String
     */
    public List<String> getExtensions() {
        return extensions;
    }

    /**
     * @param extensions value to set
     * @return this
     */
    public FileFormatModel setExtensions(List<String> extensions) {
        this.extensions = extensions;
        return this;
    }

    /**
     * @return puid
     */
    public String getPuid() {
        return puid;
    }

    /**
     * @param puid value to set
     * @return this
     */
    public FileFormatModel setPuid(String puid) {
        this.puid = puid;
        return this;
    }

    public String getUpdateDate() {
        return updateDate;
    }

    public FileFormatModel setUpdateDate(String updateDate) {
        this.updateDate = updateDate;
        return this;
    }
}
