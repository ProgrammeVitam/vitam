package fr.gouv.vitam.common.database.index.model;

import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * IndexOK
 */
public class IndexOK {

    @JsonProperty("indexName")
    private String indexName;

    @JsonProperty("tenant")
    private Integer tenant;

    public IndexOK() {}

    public IndexOK(String indexName, Integer tenant) {
        this.indexName = indexName;
        this.tenant = tenant;
    }

    public IndexOK(String indexName) {
        this.indexName = indexName;
    }

    public String getIndexName() {
        return indexName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    public Integer getTenant() {
        return tenant;
    }

    public void setTenant(Integer tenant) {
        this.tenant = tenant;
    }
}
