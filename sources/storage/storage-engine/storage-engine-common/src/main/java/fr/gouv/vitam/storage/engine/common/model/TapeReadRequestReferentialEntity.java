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

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.vitam.common.LocalDateUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// MongoDB doc limit is 16Mo => should define the adequate bulk size (read threshold)
public class TapeReadRequestReferentialEntity {

    public static final String ID = "_id";
    public static final String TAR_LOCATIONS = "tarLocation";
    public static final String FILES = "files";
    public static final String CONTAINER_NAME = "containerName";
    public static final String CREATE_DATE = "createDate";
    public static final String EXPIRE_DATE = "expireDate";
    public static final String IS_COMPLETED = "isCompleted";
    public static final String IS_EXPIRED = "isExpired";

    @JsonProperty(ID)
    private String requestId;

    @JsonProperty(CONTAINER_NAME)
    private String containerName;

    // Map of tar to tar location
    @JsonProperty(TAR_LOCATIONS)
    private Map<String, TarLocation> tarLocations = new HashMap<>();

    @JsonProperty(FILES)
    private List<FileInTape> files;

    @JsonProperty(CREATE_DATE)
    private String creationDate = LocalDateUtil.nowFormatted();

    @JsonProperty(EXPIRE_DATE)
    private String expireDate;

    public TapeReadRequestReferentialEntity() {
        // Empty constructor for deserialization
    }

    public TapeReadRequestReferentialEntity(
        String requestId,
        String containerName,
        Map<String, TarLocation> tarLocations,
        List<FileInTape> files
    ) {
        this.requestId = requestId;
        this.containerName = containerName;
        this.tarLocations = tarLocations;
        this.files = files;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getContainerName() {
        return containerName;
    }

    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }

    public Map<String, TarLocation> getTarLocations() {
        return tarLocations;
    }

    public void setTarLocations(Map<String, TarLocation> tarLocations) {
        this.tarLocations = tarLocations;
    }

    public List<FileInTape> getFiles() {
        return files;
    }

    public void setFiles(List<FileInTape> files) {
        this.files = files;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    public String getExpireDate() {
        return expireDate;
    }

    public void setExpireDate(String expireDate) {
        this.expireDate = expireDate;
    }

    @JsonProperty(IS_EXPIRED)
    public Boolean isExpired() {
        return expireDate == null
            ? false
            : LocalDateUtil.now().isAfter(LocalDateUtil.parseMongoFormattedDate(expireDate));
    }

    @JsonProperty(IS_COMPLETED)
    public boolean isCompleted() {
        return tarLocations.values().stream().filter(o -> TarLocation.DISK.equals(o)).count() == tarLocations.size();
    }
}
