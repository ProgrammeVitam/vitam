package fr.gouv.vitam.common.database.index.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * IndexKO
 */
public class IndexKO extends IndexOK {

    @JsonProperty("message")
    private String message;

    public IndexKO() {}

    public IndexKO(String indexName, Integer tenant, String message) {
        super(indexName, tenant);
        this.message = message;
    }

    public IndexKO(String indexName, String message) {
        super(indexName);
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
