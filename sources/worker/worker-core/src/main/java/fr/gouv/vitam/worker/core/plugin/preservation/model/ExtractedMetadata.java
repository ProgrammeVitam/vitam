package fr.gouv.vitam.worker.core.plugin.preservation.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.vitam.common.model.preservation.OtherMetadata;

public class ExtractedMetadata {

    @JsonProperty("OtherMetadata")
    private OtherMetadata otherMetadata;

    @JsonProperty("RawMetadata")
    private String rawMetadata;


    public ExtractedMetadata() {
        // Empty constructor for deserialization
    }

    public OtherMetadata getOtherMetadata() {
        return otherMetadata;
    }

    public void setOtherMetadata(OtherMetadata otherMetadata) {
        this.otherMetadata = otherMetadata;
    }

    public String getRawMetadata() {
        return rawMetadata;
    }

    public void setRawMetadata(String rawMetadata) {
        this.rawMetadata = rawMetadata;
    }
}
