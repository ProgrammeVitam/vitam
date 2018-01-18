package fr.gouv.vitam.common.database.index.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * IndexationResult pojo
 */
public class IndexationResult {

    @JsonProperty("collectionName")
    private String collectionName;

    @JsonProperty("OK")
    private List<IndexOK> OK;

    @JsonProperty("KO")
    private List<IndexKO> KO;

    public String getCollectionName() {
        return collectionName;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    @JsonGetter("OK")
    public List<IndexOK> getIndexOK() {
        return OK;
    }

    public void setIndexOK(List<IndexOK> indexesOK) {
        this.OK = indexesOK;
    }

    @JsonGetter("KO")
    public List<IndexKO> getIndexKO() {
        return KO;
    }

    public void setIndexKO(List<IndexKO> indexesKO) {
        this.KO = indexesKO;
    }
}
