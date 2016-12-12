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
package fr.gouv.vitam.functional.administration.client.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FileFormatModel {

    @JsonProperty("_id")
    private String id;

    // TODO: P3 use a date object
    @JsonProperty("CreatedDate")
    private String createdDate = "";

    // integer ?
    @JsonProperty("VersionPronom")
    private String versionPronom = "";

    @JsonProperty("Version")
    private String version = "";

    @JsonProperty("HasPriorityOverFileFormatID")
    private List<String> hasPriorityOverFileFormatIDs = new ArrayList<>();

    @JsonProperty("MIMEType")
    private String mimeType = "";

    @JsonProperty("Name")
    private String name = "";

    @JsonProperty("Group")
    private String group = "";

    @JsonProperty("Alert")
    private boolean alert;

    @JsonProperty("Comment")
    private String comment = "";

    @JsonProperty("Extension")
    private List<String> extensions = new ArrayList<>();

    @JsonProperty("PUID")
    private String puid = "";

    public FileFormatModel() {
    }

    public FileFormatModel(String id, String createdDate, String versionPronom, String version,
        List<String> hasPriorityOverFileFormatIDs, String mimeType, String name, String group, boolean alert,
        String comment, List<String> extensions, String puid) {
        this.id = id;
        this.createdDate = createdDate;
        this.versionPronom = versionPronom;
        this.version = version;
        this.hasPriorityOverFileFormatIDs = hasPriorityOverFileFormatIDs;
        this.mimeType = mimeType;
        this.name = name;
        this.group = group;
        this.alert = alert;
        this.comment = comment;
        this.extensions = extensions;
        this.puid = puid;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

    public String getVersionPronom() {
        return versionPronom;
    }

    public void setVersionPronom(String versionPronom) {
        this.versionPronom = versionPronom;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<String> getHasPriorityOverFileFormatIDs() {
        return hasPriorityOverFileFormatIDs;
    }

    public void setHasPriorityOverFileFormatIDs(List<String> hasPriorityOverFileFormatIDs) {
        this.hasPriorityOverFileFormatIDs = hasPriorityOverFileFormatIDs;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public boolean isAlert() {
        return alert;
    }

    public void setAlert(boolean alert) {
        this.alert = alert;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public List<String> getExtensions() {
        return extensions;
    }

    public void setExtensions(List<String> extensions) {
        this.extensions = extensions;
    }

    public String getPuid() {
        return puid;
    }

    public void setPuid(String puid) {
        this.puid = puid;
    }

}
