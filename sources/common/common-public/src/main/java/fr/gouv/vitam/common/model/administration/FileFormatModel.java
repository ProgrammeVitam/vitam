/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common.model.administration;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * POJO java use for mapping @{@link fr.gouv.vitam.functional.administration.common.FileFormat}
 */
public class FileFormatModel {

    /**
     * unique id
     */
    @JsonProperty("_id")
    private String id;

    // TODO: P3 use a date object
    /**
     * creation date
     */
    @JsonProperty("CreatedDate")
    private String createdDate = "";

    /**
     * version pronom
     */
    @JsonProperty("VersionPronom")
    private String versionPronom = "";

    /**
     * version
     */
    @JsonProperty("Version")
    private String version = "";

    /**
     * list of FileFormat with lower priority
     */
    @JsonProperty("HasPriorityOverFileFormatID")
    private List<String> hasPriorityOverFileFormatIDs = new ArrayList<>();

    /**
     * mime type
     */
    @JsonProperty("MIMEType")
    private String mimeType = "";

    /**
     * name
     */
    @JsonProperty("Name")
    private String name = "";

    /**
     * group
     */
    @JsonProperty("Group")
    private String group = "";

    @JsonProperty("Alert")
    private boolean alert;

    /**
     * comment
     */
    @JsonProperty("Comment")
    private String comment = "";

    /**
     * extensions
     */
    @JsonProperty("Extension")
    private List<String> extensions = new ArrayList<>();

    /**
     * puid
     */
    @JsonProperty("PUID")
    private String puid = "";

    /**
     * Constructor without fields
     *
     */
    public FileFormatModel() {
    }

    /**
     * @return id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id value to set
     * @return this
     */
    public FileFormatModel setId(String id) {
        this.id = id;
        return this;
    }

    /**
     *
     * @return createdDate
     */
    public String getCreatedDate() {
        return createdDate;
    }

    /**
     *
     * @param createdDate value to set
     * @return this
     */
    public FileFormatModel setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
        return this;
    }

    /**
     *
     * @return versionPronom
     */
    public String getVersionPronom() {
        return versionPronom;
    }

    /**
     *
     * @param versionPronom value to set
     * @return this
     */
    public FileFormatModel setVersionPronom(String versionPronom) {
        this.versionPronom = versionPronom;
        return this;
    }

    /**
     *
     * @return version
     */
    public String getVersion() {
        return version;
    }

    /**
     *
     * @param version value to set
     * @return this
     */
    public FileFormatModel setVersion(String version) {
        this.version = version;
        return this;
    }

    /**
     *
     * @return hasPriorityOverFileFormatIDs
     */
    public List<String> getHasPriorityOverFileFormatIDs() {
        return hasPriorityOverFileFormatIDs;
    }

    /**
     *
     * @param hasPriorityOverFileFormatIDs value to set
     * @return this
     */
    public FileFormatModel setHasPriorityOverFileFormatIDs(List<String> hasPriorityOverFileFormatIDs) {
        this.hasPriorityOverFileFormatIDs = hasPriorityOverFileFormatIDs;
        return this;
    }

    /**
     *
     * @return mimeType
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     *
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
     *
     * @param name value to set
     * @return this
     */
    public FileFormatModel setName(String name) {
        this.name = name;
        return this;
    }

    /**
     *
     * @return group
     */
    public String getGroup() {
        return group;
    }

    /**
     *
     * @param group value to set
     * @return this
     */
    public FileFormatModel setGroup(String group) {
        this.group = group;
        return this;
    }

    /**
     *
     * @return alert
     */
    public boolean isAlert() {
        return alert;
    }

    /**
     *
     * @param alert value to set
     * @return this
     */
    public FileFormatModel setAlert(boolean alert) {
        this.alert = alert;
        return this;
    }

    /**
     *
     * @return String
     */
    public String getComment() {
        return comment;
    }

    /**
     *
     * @param comment value to set
     * @return this
     */
    public FileFormatModel setComment(String comment) {
        this.comment = comment;
        return this;
    }

    /**
     *
     * @return List of String
     */
    public List<String> getExtensions() {
        return extensions;
    }

    /**
     *
     * @param extensions value to set
     * @return this
     */
    public FileFormatModel setExtensions(List<String> extensions) {
        this.extensions = extensions;
        return this;
    }

    /**
     *
     * @return puid
     */
    public String getPuid() {
        return puid;
    }

    /**
     *
     * @param puid value to set
     * @return this
     */
    public FileFormatModel setPuid(String puid) {
        this.puid = puid;
        return this;
    }

}
