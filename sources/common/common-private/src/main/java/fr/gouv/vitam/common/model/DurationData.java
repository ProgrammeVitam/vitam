package fr.gouv.vitam.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.temporal.ChronoUnit;

public class DurationData {
    @JsonProperty("DurationValue")
    private Integer durationValue;

    @JsonProperty("DurationUnit")
    private ChronoUnit durationUnit;

    public DurationData(Integer durationValue, ChronoUnit durationUnit) {
        this.durationValue = durationValue;
        this.durationUnit = durationUnit;
    }

    public DurationData() {
        // Empty constructor for Jackson
    }

    public Integer getDurationValue() {
        return durationValue;
    }

    public ChronoUnit getDurationUnit() {
        return durationUnit;
    }

    public void setDurationValue(Integer durationValue) {
        this.durationValue = durationValue;
    }

    public void setDurationUnit(ChronoUnit durationUnit) {
        this.durationUnit = durationUnit;
    }
}
