package fr.gouv.vitam.common.model.tape;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

// MongoDB doc limit is 16Mo => should define the adequate bulk size (read threshold)
public class TapeReadRequestReferentialEntity {
    public static final String ID = "_id";
    public static final String TAR_LOCATIONS = "tarLocation";
    public static final String FILES = "files";
    public static final String CONTAINER_NAME = "containerName";

    @JsonProperty(ID)
    private String requestId;

    @JsonProperty(CONTAINER_NAME)
    private String containerName;

    // Map of tar to tar location
    @JsonProperty(TAR_LOCATIONS)
    private Map<String, TarLocation> tarLocations;


    @JsonProperty(FILES)
    private List<FileInTape> files;

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

    @JsonIgnore
    public boolean isCompleted() {
        return tarLocations.values().stream().filter(o -> TarLocation.DISK.equals(o)).count() == tarLocations.size();
    }
}
