package fr.gouv.vitam.workspace.common;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * TODO : @gafou : unused in server resource. It probably should be ? Or maybe the code should change.
 * 
 */
public class RequestResponse {
    private JsonNode query;
    private JsonNode results;

    
    /**
     * @return the Result part
     */
    public JsonNode getResult() {
        return results;
    }

    /**
     * RequestResponse constructor
     * 
     * @param result the result of type JsonNode which will be setted for RequestResponse
     * @return this
     */
    public RequestResponse setResult(JsonNode result) {
        this.results = result;
        return this;
    }
    
    /**
     * @return the query as JsonNode of Response
     */
    public JsonNode getQuery() {
        return query;
    }

    /**
     * RequestResponse constructor
     * 
     * @param query the query of type JsonNode which will be setted for RequestResponse
     * @return this
     */
    public RequestResponse setQuery(JsonNode query) {
        this.query = query;
        return this;
    }
}
