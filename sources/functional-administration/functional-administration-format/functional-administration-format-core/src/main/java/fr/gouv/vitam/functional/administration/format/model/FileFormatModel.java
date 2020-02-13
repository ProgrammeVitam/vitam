/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
package fr.gouv.vitam.functional.administration.format.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

import static fr.gouv.vitam.functional.administration.common.FileFormat.ALERT;
import static fr.gouv.vitam.functional.administration.common.FileFormat.COMMENT;
import static fr.gouv.vitam.functional.administration.common.FileFormat.CREATED_DATE;
import static fr.gouv.vitam.functional.administration.common.FileFormat.EXTENSION;
import static fr.gouv.vitam.functional.administration.common.FileFormat.GROUP;
import static fr.gouv.vitam.functional.administration.common.FileFormat.HAS_PRIORITY_OVER_FILE_FORMAT_ID;
import static fr.gouv.vitam.functional.administration.common.FileFormat.MIME_TYPE;
import static fr.gouv.vitam.functional.administration.common.FileFormat.NAME;
import static fr.gouv.vitam.functional.administration.common.FileFormat.PUID;
import static fr.gouv.vitam.functional.administration.common.FileFormat.UPDATE_DATE;
import static fr.gouv.vitam.functional.administration.common.FileFormat.VERSION;
import static fr.gouv.vitam.functional.administration.common.FileFormat.VERSION_PRONOM;

public class FileFormatModel {

    @JsonProperty(CREATED_DATE)
    private String createdDate;

    @JsonProperty(VERSION_PRONOM)
    private String versionPronom;

    @JsonProperty(UPDATE_DATE)
    private String updateDate;

    @JsonProperty(PUID)
    private String puid;

    @JsonProperty(VERSION)
    private String version = "";

    @JsonProperty(MIME_TYPE)
    private String mimeType = "";

    @JsonProperty(NAME)
    private String name;

    @JsonProperty(HAS_PRIORITY_OVER_FILE_FORMAT_ID)
    private List<String> hasPriorityOverFileFormatID = new ArrayList<>();

    @JsonProperty(EXTENSION)
    private List<String> extension = new ArrayList<>();

    /**
     * Never used. Reserved for "future use".
     */
    @JsonProperty(GROUP)
    private String group = "";

    /**
     * Never used. Reserved for "future use".
     */
    @JsonProperty(ALERT)
    private boolean alert = false;

    /**
     * Never used. Reserved for "future use".
     */
    @JsonProperty(COMMENT)
    private String comment = "";

    public FileFormatModel() {
        // Empty constructor for deserialization
    }

    public String getPuid() {
        return puid;
    }

    public FileFormatModel setPuid(String puid) {
        this.puid = puid;
        return this;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public FileFormatModel setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
        return this;
    }

    public String getVersionPronom() {
        return versionPronom;
    }

    public FileFormatModel setVersionPronom(String versionPronom) {
        this.versionPronom = versionPronom;
        return this;
    }

    public String getUpdateDate() {
        return updateDate;
    }

    public FileFormatModel setUpdateDate(String updateDate) {
        this.updateDate = updateDate;
        return this;
    }

    public String getVersion() {
        return version;
    }

    public FileFormatModel setVersion(String version) {
        this.version = version;
        return this;
    }

    public String getMimeType() {
        return mimeType;
    }

    public FileFormatModel setMimeType(String mimeType) {
        this.mimeType = mimeType;
        return this;
    }

    public String getName() {
        return name;
    }

    public FileFormatModel setName(String name) {
        this.name = name;
        return this;
    }

    public List<String> getExtension() {
        return extension;
    }

    public FileFormatModel setExtension(List<String> extension) {
        this.extension = extension;
        return this;
    }

    public List<String> getHasPriorityOverFileFormatID() {
        return hasPriorityOverFileFormatID;
    }

    public FileFormatModel setHasPriorityOverFileFormatID(List<String> hasPriorityOverFileFormatID) {
        this.hasPriorityOverFileFormatID = hasPriorityOverFileFormatID;
        return this;
    }

    public String getGroup() {
        return group;
    }

    public FileFormatModel setGroup(String group) {
        this.group = group;
        return this;
    }

    public boolean isAlert() {
        return alert;
    }

    public FileFormatModel setAlert(boolean alert) {
        this.alert = alert;
        return this;
    }

    public String getComment() {
        return comment;
    }

    public FileFormatModel setComment(String comment) {
        this.comment = comment;
        return this;
    }
}
