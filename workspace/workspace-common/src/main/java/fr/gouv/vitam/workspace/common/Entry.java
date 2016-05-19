package fr.gouv.vitam.workspace.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
// TODO Missing licence file 
// TODO Explicit the role of the class in the comment (eg : used for the serialisation/unserialisation of the json object) . It is currently used both for container and folder 
/**
 * The Entry class.
 *
 */
public class Entry {

    @JsonProperty("name")
    private String name;

    @JsonCreator
    public Entry(@JsonProperty("name") String name) {
        // TODO REVIEW useful super() ?
        super();
        // FIXME REVIEW check null ?
        this.name = name;
    }

    @JsonIgnore
    public String getName() {
        return name;
    }

}
