package fr.gouv.vitam.common.model.tape;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class TapeReadRequestReferentialEntity {
    public static final String ID = "_id";
    public static final String EXPECTED_TARS_COUNT = "expectedTarsCount";
    public static final String CURRENT_READ_TARS_COUNT = "currentReadTarsCount";
    public static final String FILES = "files";

    @JsonProperty(ID)
    private String requestId;

    @JsonProperty(EXPECTED_TARS_COUNT)
    private int expectedTarsCount;

    @JsonProperty(CURRENT_READ_TARS_COUNT)
    private int currentReadTarsCount = 0;

    // MongoDB doc limit is 16Mo => should define the adequate bulk size (read threshold)
    @JsonProperty(FILES)
    private List<FileInTape> files;

    public TapeReadRequestReferentialEntity() {
        // Empty constructor for deserialization
    }

    public TapeReadRequestReferentialEntity(String requestId, int expectedTarsCount, List<FileInTape> files) {
        this.requestId = requestId;
        this.expectedTarsCount = expectedTarsCount;
        this.files = files;
    }

    public String getRequestId() {
        return requestId;
    }

    public List<FileInTape> getFiles() {
        return files;
    }

    @JsonIgnore
    public boolean isCompleted() {
        return expectedTarsCount == currentReadTarsCount;
    }
}
