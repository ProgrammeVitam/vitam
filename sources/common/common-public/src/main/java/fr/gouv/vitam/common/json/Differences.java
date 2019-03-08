package fr.gouv.vitam.common.json;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Differences {
    @JsonProperty("diff")
    public final List<Difference> diff;

    public Differences(List<Difference> diff) {
        this.diff = diff;
    }

    public List<Difference> getDiff() {
        return diff;
    }
}
