package fr.gouv.vitam.worker.core.plugin.preservation.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExtractedMetadata {
    @JsonProperty("MetadataToReplace")
    private Map<String, String> metadataToReplace;

    @JsonProperty("MetadataToAdd")
    private Map<String, List<String>> metadataToAdd;

    @JsonProperty("RawMetadata")
    private String rawMetadata;

    public ExtractedMetadata() {
        // Empty constructor for deserialization
    }

    public Map<String, String> getMetadataToReplace() {
        if (metadataToReplace == null) {
            return new HashMap<>();
        }
        return metadataToReplace;
    }

    public void setMetadataToReplace(Map<String, String> metadataToReplace) {
        this.metadataToReplace = metadataToReplace;
    }

    public Map<String, List<String>> getMetadataToAdd() {
        if (metadataToAdd == null) {
            return new HashMap<>();
        }
        return metadataToAdd;
    }

    public void setMetadataToAdd(Map<String, List<String>> metadataToAdd) {
        this.metadataToAdd = metadataToAdd;
    }

    public String getRawMetadata() {
        return rawMetadata;
    }

    public void setRawMetadata(String rawMetadata) {
        this.rawMetadata = rawMetadata;
    }
}
