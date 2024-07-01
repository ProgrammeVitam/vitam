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
package fr.gouv.vitam.storage.engine.common.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

// MongoDB doc limit is 16Mo => should define the adequate bulk size (read threshold)
public class TapeAccessRequestReferentialEntity {

    public static final String ID = "_id";
    public static final String CONTAINER_NAME = "containerName";
    public static final String OBJECT_NAMES = "objectNames";
    public static final String CREATION_DATE = "creationDate";
    public static final String READY_DATE = "readyDate";
    public static final String EXPIRATION_DATE = "expirationDate";
    public static final String PURGE_DATE = "purgeDate";
    public static final String UNAVAILABLE_ARCHIVE_IDS = "unavailableArchiveIds";
    public static final String TENANT = "_tenant";
    public static final String VERSION = "_v";

    @JsonProperty(ID)
    private final String requestId;

    @JsonProperty(CONTAINER_NAME)
    private final String containerName;

    @JsonProperty(OBJECT_NAMES)
    private final List<String> objectNames;

    @JsonProperty(CREATION_DATE)
    @JsonInclude
    private final String creationDate;

    @JsonProperty(READY_DATE)
    @JsonInclude
    private final String readyDate;

    @JsonProperty(EXPIRATION_DATE)
    @JsonInclude
    private final String expirationDate;

    @JsonProperty(PURGE_DATE)
    @JsonInclude
    private final String purgeDate;

    // List of TarIds not yet available on disk
    @JsonProperty(UNAVAILABLE_ARCHIVE_IDS)
    @JsonInclude
    private final List<String> unavailableArchiveIds;

    @JsonProperty(TENANT)
    private final int tenant;

    @JsonProperty(VERSION)
    private final int version;

    @JsonCreator
    public TapeAccessRequestReferentialEntity(
        @JsonProperty(ID) String requestId,
        @JsonProperty(CONTAINER_NAME) String containerName,
        @JsonProperty(OBJECT_NAMES) List<String> objectNames,
        @JsonProperty(CREATION_DATE) String creationDate,
        @JsonProperty(READY_DATE) String readyDate,
        @JsonProperty(EXPIRATION_DATE) String expirationDate,
        @JsonProperty(PURGE_DATE) String purgeDate,
        @JsonProperty(UNAVAILABLE_ARCHIVE_IDS) List<String> unavailableArchiveIds,
        @JsonProperty(TENANT) int tenant,
        @JsonProperty(VERSION) int version
    ) {
        this.requestId = requestId;
        this.containerName = containerName;
        this.objectNames = objectNames;
        this.creationDate = creationDate;
        this.readyDate = readyDate;
        this.expirationDate = expirationDate;
        this.purgeDate = purgeDate;
        this.unavailableArchiveIds = unavailableArchiveIds;
        this.tenant = tenant;
        this.version = version;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getContainerName() {
        return containerName;
    }

    public List<String> getObjectNames() {
        return objectNames;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public String getReadyDate() {
        return readyDate;
    }

    public String getExpirationDate() {
        return expirationDate;
    }

    public String getPurgeDate() {
        return purgeDate;
    }

    public List<String> getUnavailableArchiveIds() {
        return unavailableArchiveIds;
    }

    public int getTenant() {
        return tenant;
    }

    public int getVersion() {
        return version;
    }
}
