/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 *******************************************************************************/
package fr.gouv.vitam.common.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Access RequestResponseOK class contains list of results<br>
 * default results : is an empty list (immutable)
 */
public final class RequestResponseOK<T> extends RequestResponse<T> {
    
    /**
     * result detail in response
     */
    private static final String HITS = "$hits";

    /**
     * result in response
     */
    private static final String RESULTS = "$results";

    /**
     * context in response
     */
    public static final String CONTEXT = "$context";
    
    @JsonProperty(HITS)
    private DatabaseCursor hits = new DatabaseCursor(0, 0, 0);
    @JsonProperty(RESULTS)
    private final List<T> results = new ArrayList<T>();
    @JsonProperty(CONTEXT)
    private JsonNode query = JsonHandler.createObjectNode();

    /**
     * Empty RequestResponseOK constructor
     **/
    public RequestResponseOK() {
        // Empty
    }

    /**
     * Add one result
     *
     * @param result to add to request response
     * @return this
     */
    public RequestResponseOK<T> addResult(T result) {
        ParametersChecker.checkParameter("Result is a mandatory parameter", result);
        results.add(result);
        return this;
    }

    /**
     * Add list of results
     *
     * @param resultList the list of results
     * @return RequestResponseOK with mutable results list of String
     */
    @JsonSetter(RESULTS)
    public RequestResponseOK<T> addAllResults(List<T> resultList) {
        ParametersChecker.checkParameter("Result list is a mandatory parameter", resultList);
        results.addAll(resultList);
        return this;
    }


    /**
     * @return the hits of RequestResponseOK object
     */
    public DatabaseCursor getHits() {
        return hits;
    }

    /**
     * @param hits as DatabaseCursor object
     * @return RequestReponseOK with the hits are setted
     */
    @JsonSetter(HITS)
    public RequestResponseOK setHits(DatabaseCursor hits) {
        if (hits != null) {
            this.hits = hits;
        }
        return this;
    }

    /**
     * @param total of units inserted/modified as integer
     * @param offset of unit in database as integer
     * @param limit of unit per response as integer
     * @return the RequestReponseOK with the hits are setted
     */
    @JsonSetter(HITS)
    public RequestResponseOK setHits(int total, int offset, int limit) {
        hits = new DatabaseCursor(total, offset, limit, total);
        return this;
    }

    /**
     * @param total of units inserted/modified as integer
     * @param offset of unit in database as integer
     * @param limit of unit per response as integer
     * @param size of unit per response
     * @return the RequestReponseOK with the hits are setted
     */
    @JsonSetter(HITS)
    public RequestResponseOK<T> setHits(int total, int offset, int limit, int size) {
        hits = new DatabaseCursor(total, offset, limit, size);
        return this;
    }

    /**
     * @return the result of RequestResponse as a list of String
     */
    @JsonGetter(RESULTS)
    public List<T> getResults() {
        return results;
    }


    /**
     * @return the query as JsonNode of Response
     */
    public JsonNode getQuery() {
        return query;
    }

    /**
     * @param query the set to request response
     * @return this
     */
    public RequestResponseOK<T> setQuery(JsonNode query) {
        if (query != null) {
            this.query = query;
        }
        return this;
    }

    /**
     * @param node to transform
     * @return the corresponding VitamError
     * @throws InvalidParseOperationException if parse json object exception occurred
     */
    public static RequestResponseOK getFromJsonNode(JsonNode node) throws InvalidParseOperationException {
        return JsonHandler.getFromString(node.toString(), RequestResponseOK.class, JsonNode.class);
    }

    /**
     * transform a RequestResponse to a standard response
     * @return Response
     */
    @Override
    public Response toResponse() {
        final Response.ResponseBuilder resp = Response.status(getStatus()).entity(toJsonNode());
        final Map<String, String> vitamHeaders = this.getVitamHeaders();
        for (String key : vitamHeaders.keySet()) {
            resp.header(key, getHeaderString(key));
        }

        this.unSetVitamHeaders();
        return resp.build();
    }
}
