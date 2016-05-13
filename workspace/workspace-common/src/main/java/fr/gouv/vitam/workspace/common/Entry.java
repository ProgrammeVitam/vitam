package fr.gouv.vitam.workspace.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The Entry class.
 *
 */
public class Entry {

    @JsonProperty("name")
    private String name;

    @JsonCreator
    public Entry(@JsonProperty("name") String name) {
        super();
        this.name = name;
    }

    @JsonIgnore
    public String getName() {
        return name;
    }

}