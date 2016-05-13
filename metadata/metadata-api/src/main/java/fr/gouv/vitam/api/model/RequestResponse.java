package fr.gouv.vitam.api.model;

import com.fasterxml.jackson.databind.JsonNode;
/**
 * Meta-data RequestResponse class
 * contains request query 
 *
 */
public class RequestResponse {
    private JsonNode query;

    /**
     * @return the query as JsonNode of Response
     */ 
    public JsonNode getQuery() {
        return query;
    }
    /**
     * RequestResponse constructor
     * @param query
     *          the query of type JsonNode which will be setted for RequestResponse
     */
    public RequestResponse setQuery(JsonNode query) {
        this.query = query;
        return this;
    }
}
