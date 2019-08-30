package fr.gouv.vitam.storage.engine.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.vitam.common.LocalDateUtil;

import java.time.LocalDateTime;
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
    private String creationDate = LocalDateUtil.getFormattedDateForMongo(LocalDateTime.now());

    @JsonProperty(EXPIRE_DATE)
    private String expireDate;


    public TapeReadRequestReferentialEntity() {
        // Empty constructor for deserialization
    }

    public TapeReadRequestReferentialEntity(String requestId, String containerName,
        Map<String, TarLocation> tarLocations, List<FileInTape> files) {
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
        return expireDate == null ?
            false :
            LocalDateTime.now().isAfter(LocalDateUtil.parseMongoFormattedDate(expireDate));
    }

    @JsonProperty(IS_COMPLETED)
    public boolean isCompleted() {
        return tarLocations.values().stream().filter(o -> TarLocation.DISK.equals(o)).count() == tarLocations.size();
    }

}
