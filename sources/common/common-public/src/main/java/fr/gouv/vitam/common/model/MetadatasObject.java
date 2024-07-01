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
package fr.gouv.vitam.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Metadata object
 */
public abstract class MetadatasObject {

    @JsonProperty("objectName")
    private String objectName;

    @JsonProperty("type")
    private String type;

    @JsonProperty("digest")
    private String digest;

    @JsonProperty("fileSize")
    private long fileSize;

    @JsonProperty("lastAccessDate")
    private String lastAccessDate;

    @JsonProperty("lastModifiedDate")
    private String lastModifiedDate;

    /**
     * empty constructor
     */
    public MetadatasObject() {
        this.objectName = null;
        this.type = null;
        this.digest = null;
        this.fileSize = 0;
        this.lastAccessDate = null;
        this.lastModifiedDate = null;
    }

    /**
     * Constructor to initialize the needed parameters for get metadata results
     *
     * @param objectName the object name
     * @param type the type of metadata object
     * @param digest of metadata object
     * @param fileSize of metadata object
     * @param lastAccessDate of metadata object
     * @param lastModifiedDate of metadata object
     */
    public MetadatasObject(
        String objectName,
        String type,
        String digest,
        long fileSize,
        String lastAccessDate,
        String lastModifiedDate
    ) {
        this.objectName = objectName;
        this.type = type;
        this.digest = digest;
        this.fileSize = fileSize;
        this.lastAccessDate = lastAccessDate;
        this.lastModifiedDate = lastModifiedDate;
    }

    /**
     * @return object name
     */
    public String getObjectName() {
        return objectName;
    }

    /**
     * @param objectName of metadata object to set
     * @return MetadatasObjectResult
     */
    public MetadatasObject setObjectName(String objectName) {
        this.objectName = objectName;
        return this;
    }

    /**
     * @return type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type of metadata object to set
     * @return MetadatasObjectResult
     */
    public MetadatasObject setType(String type) {
        this.type = type;
        return this;
    }

    /**
     * @return digest
     */
    public String getDigest() {
        return digest;
    }

    /**
     * @param digest of metadata object to set
     * @return MetadatasObjectResult
     */
    public MetadatasObject setDigest(String digest) {
        this.digest = digest;
        return this;
    }

    /**
     * @return the file size of metadata object
     */
    public long getFileSize() {
        return fileSize;
    }

    /**
     * @param fileSize of metadata object to set
     * @return MetadatasObjectResult
     */
    public MetadatasObject setFileSize(long fileSize) {
        this.fileSize = fileSize;
        return this;
    }

    /**
     * @return file's last access date
     */
    public String getLastAccessDate() {
        return lastAccessDate;
    }

    /**
     * @param lastAccessDate of metadata object to set
     * @return MetadatasObjectResult
     */
    public MetadatasObject setLastAccessDate(String lastAccessDate) {
        this.lastAccessDate = lastAccessDate;
        return this;
    }

    /**
     * @return file's last modifiedDate
     */
    public String getLastModifiedDate() {
        return lastModifiedDate;
    }

    /**
     * @param lastModifiedDate of metadata object to set
     * @return MetadatasObjectResult
     */
    public MetadatasObject setLastModifiedDate(String lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
        return this;
    }
}
