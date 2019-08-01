package fr.gouv.vitam.storage.engine.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.vitam.common.LocalDateUtil;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

// MongoDB doc limit is 16Mo => should define the adequate bulk size (read threshold)
public class TapeReadRequestReferentialEntity {
    public static final String ID = "_id";
    public static final String TAR_LOCATIONS = "tarLocation";
    public static final String FILES = "files";
    public static final String CONTAINER_NAME = "containerName";
    public static final String CREATE_DATE = "createDate";
    public static final String EXPIRE_IN_MINUTES = "expireInMinutes";

    @JsonProperty(ID)
    private String requestId;

    @JsonProperty(CONTAINER_NAME)
    private String containerName;

    // Map of tar to tar location
    @JsonProperty(TAR_LOCATIONS)
    private Map<String, TarLocation> tarLocations;


    @JsonProperty(FILES)
    private List<FileInTape> files;

    @JsonProperty(CREATE_DATE)
    private String creationDate = LocalDateUtil.getFormattedDateForMongo(LocalDateTime.now());

    @JsonProperty(EXPIRE_IN_MINUTES)
    private Long expireInMinutes = 0L;


    public TapeReadRequestReferentialEntity() {
        // Empty constructor for deserialization
    }

    public TapeReadRequestReferentialEntity(String requestId, String containerName,
        Map<String, TarLocation> tarLocations, List<FileInTape> files, Long expireInMinutes) {
        this.requestId = requestId;
        this.containerName = containerName;
        this.tarLocations = tarLocations;
        this.files = files;
        this.expireInMinutes = expireInMinutes;
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

    public Long getExpireInMinutes() {
        return expireInMinutes;
    }

    public void setExpireInMinutes(Long expireInMinutes) {
        this.expireInMinutes = expireInMinutes;
    }

    @JsonProperty
    public boolean isCompleted() {
        return tarLocations.values().stream().filter(o -> TarLocation.DISK.equals(o)).count() == tarLocations.size();
    }

    @JsonProperty
    public boolean isExpired() {
        return LocalDateUtil.parseMongoFormattedDate(creationDate).plusMinutes(expireInMinutes)
            .isBefore(LocalDateTime.now());
    }
}
