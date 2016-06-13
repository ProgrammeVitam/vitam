package fr.gouv.vitam.api.model;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Meta-data RequestResponse class contains request query
 *
 */
// TODO REVIEW Fix comment with a correct vision (either adding <br> either adding ':'
// TODO REVIEW should be abstract
public class RequestResponse {
    private JsonNode query;

    /**
     * @return the query as JsonNode of Response
     */
    public JsonNode getQuery() {
        // TODO REVIEW not return null but empty Json
        return query;
    }

    /**
     * RequestResponse constructor
     * 
     * @param query the query of type JsonNode which will be setted for RequestResponse
     */
    public RequestResponse setQuery(JsonNode query) {
        this.query = query;
        return this;
    }
}
