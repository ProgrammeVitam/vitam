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

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Object mapping FileInfoResponse
 */
public class FileInfoModel {

    public static final String FILENAME = "Filename";
    public static final String CREATING_APPLICATION_NAME = "CreatingApplicationName";
    public static final String CREATING_APPLICATION_VERSION = "CreatingApplicationVersion";
    public static final String CREATING_OS = "CreatingOs";
    public static final String CREATING_OS_VERSION = "CreatingOsVersion";
    public static final String LAST_MODIFIED = "LastModified";
    public static final String DATE_CREATED_BY_APPLICATION = "DateCreatedByApplication";

    @JsonProperty(FILENAME)
    private String filename;

    @JsonProperty(CREATING_APPLICATION_NAME)
    private String creatingApplicationName;

    @JsonProperty(CREATING_APPLICATION_VERSION)
    private String creatingApplicationVersion;

    @JsonProperty(CREATING_OS)
    private String creatingOs;

    @JsonProperty(CREATING_OS_VERSION)
    private String creatingOsVersion;

    @JsonProperty(LAST_MODIFIED)
    private String lastModified;

    @JsonProperty(DATE_CREATED_BY_APPLICATION)
    private String dateCreatedByApplication;

    public String getDateCreatedByApplication() {
        return dateCreatedByApplication;
    }

    public void setDateCreatedByApplication(String dateCreatedByApplication) {
        this.dateCreatedByApplication = dateCreatedByApplication;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getCreatingApplicationName() {
        return creatingApplicationName;
    }

    public void setCreatingApplicationName(String creatingApplicationName) {
        this.creatingApplicationName = creatingApplicationName;
    }

    public String getCreatingApplicationVersion() {
        return creatingApplicationVersion;
    }

    public void setCreatingApplicationVersion(String creatingApplicationVersion) {
        this.creatingApplicationVersion = creatingApplicationVersion;
    }

    public String getCreatingOs() {
        return creatingOs;
    }

    public void setCreatingOs(String creatingOs) {
        this.creatingOs = creatingOs;
    }

    public String getCreatingOsVersion() {
        return creatingOsVersion;
    }

    public void setCreatingOsVersion(String creatingOsVersion) {
        this.creatingOsVersion = creatingOsVersion;
    }

    public String getLastModified() {
        return lastModified;
    }

    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }
}
