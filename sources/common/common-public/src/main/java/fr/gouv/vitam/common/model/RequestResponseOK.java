/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
 * circulated by CEA, CNRS and INRIA at the following URL "https://cecill.info".
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.SysErrLogger;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Access RequestResponseOK class contains list of results<br>
 * default results : is an empty list (immutable)
 *
 * @param <T> Type of results
 */
public final class RequestResponseOK<T> extends RequestResponse<T> {

    /**
     * result detail in response
     */
    private static final String TAG_HITS = "$hits";

    /**
     * result in response
     */
    public static final String TAG_RESULTS = "$results";

    /**
     * facet results in response
     */
    public static final String TAG_FACET_RESULTS = "$facetResults";

    /**
     * context in response
     */
    public static final String TAG_CONTEXT = "$context";

    @JsonProperty(TAG_HITS)
    private DatabaseCursor hits;

    @JsonProperty(TAG_RESULTS)
    private final List<T> results;

    @JsonProperty(TAG_FACET_RESULTS)
    private final List<FacetResult> facetResults;

    @JsonProperty(TAG_CONTEXT)
    private JsonNode query;

    /**
     * Empty RequestResponseOK constructor
     **/
    public RequestResponseOK() {
        this(JsonHandler.createObjectNode());
    }

    /**
     * Initialize from a query
     *
     * @param query
     */
    public RequestResponseOK(JsonNode query) {
        this.query = query;
        int offset = 0;
        int limit = 0;

        try {
            QueryDTO queryDTO = JsonHandler.getFromJsonNode(query, QueryDTO.class);
            if (null != queryDTO) {
                QueryFilter queryFilter = queryDTO.getFilter();
                if (null != queryFilter) {
                    offset = queryFilter.getOffset();
                    limit = queryFilter.getLimit();
                }
            }
        } catch (Exception e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }
        hits = new DatabaseCursor(0, offset, limit);
        results = new ArrayList<>();
        facetResults = new ArrayList<>();
    }

    /**
     * Initialize from a query and results
     *
     * @param query
     */
    public RequestResponseOK(JsonNode query, List<T> results, int total) {
        this(query);
        this.results.addAll(results);
        this.hits = new DatabaseCursor(total, hits.getOffset(), hits.getLimit(), results.size());
    }

    /**
     * Initialize from a query and results
     *
     * @param query
     */
    public RequestResponseOK(JsonNode query, List<T> results, int total, String scrollId) {
        this(query, results, total);
        hits = new DatabaseCursor(total, hits.getOffset(), hits.getLimit(), results.size(), scrollId);
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
        hits.setSize(hits.getSize() + 1);
        hits.setTotal(hits.getSize());
        return this;
    }

    /**
     * Add list of results
     *
     * @param resultList the list of results
     * @return RequestResponseOK with mutable results list of String
     */
    public RequestResponseOK<T> addAllResults(List<T> resultList) {
        ParametersChecker.checkParameter("Result list is a mandatory parameter", resultList);
        results.addAll(resultList);
        hits.setSize(hits.getSize() + resultList.size());
        hits.setTotal(hits.getSize());
        return this;
    }

    /**
     * Add one facetResult
     *
     * @param facetResult to add to request response
     * @return this
     */
    public RequestResponseOK<T> addFacetResult(FacetResult facetResult) {
        ParametersChecker.checkParameter("facetResult is a mandatory parameter", facetResult);
        facetResults.add(facetResult);
        return this;
    }

    /**
     * Add list of facetResult
     *
     * @param facetResultList the list of facetResults
     * @return RequestResponseOK with mutable results list of String
     */
    public RequestResponseOK<T> addAllFacetResults(List<FacetResult> facetResultList) {
        ParametersChecker.checkParameter("facetResult list is a mandatory parameter", facetResults);
        facetResults.addAll(facetResultList);
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
    public RequestResponseOK<T> setHits(DatabaseCursor hits) {
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
    public RequestResponseOK<T> setHits(long total, int offset, int limit) {
        hits = new DatabaseCursor(total, offset, limit, total);
        return this;
    }

    /**
     * @param total of units inserted/modified as integer
     * @param offset of unit in database as integer
     * @param limit of unit per response as integer
     * @param size of unit per response
     * @param scrollId of response
     * @return the RequestReponseOK with the hits are setted
     */
    public RequestResponseOK<T> setHits(long total, int offset, int limit, int size, String scrollId) {
        hits = new DatabaseCursor(total, offset, limit, size, scrollId);
        return this;
    }

    /**
     * @param total of units inserted/modified as integer
     * @param offset of unit in database as integer
     * @param limit of unit per response as integer
     * @param size of unit per response
     * @return the RequestReponseOK with the hits are setted
     */
    public RequestResponseOK<T> setHits(long total, int offset, int limit, int size) {
        hits = new DatabaseCursor(total, offset, limit, size);
        return this;
    }

    /**
     * Should be used only with hints of elasticsearch
     *
     * @param total
     * @return
     */
    public RequestResponseOK<T> setTotal(long total) {
        if (hits != null) {
            hits.setTotal(total);
        }
        return this;
    }

    /**
     * When activate scroll
     *
     * @param scrollId
     * @return
     */
    public RequestResponseOK<T> setScrollId(String scrollId) {
        if (hits != null) {
            hits.setScrollId(scrollId);
        }
        return this;
    }

    /**
     * @return the facetResults
     */
    public List<FacetResult> getFacetResults() {
        return facetResults;
    }

    /**
     * @return the result of RequestResponse as a list of <T>
     */
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
     * @return the first result of RequestResponse as a <T>
     */
    @JsonIgnore
    public T getFirstResult() {
        if (results != null && !results.isEmpty()) {
            return results.get(0);
        }
        return null;
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
     * @return True if the result is empty
     */
    @JsonIgnore
    public boolean isEmpty() {
        return results.isEmpty();
    }

    /**
     * @param node to transform
     * @return the corresponding VitamError
     * @throws InvalidParseOperationException if parse json object exception occurred
     */
    public static RequestResponseOK<JsonNode> getFromJsonNode(JsonNode node) throws InvalidParseOperationException {
        return JsonHandler.getFromString(node.toString(), RequestResponseOK.class, JsonNode.class);
    }

    /**
     * @param node to transform
     * @return the corresponding VitamError
     * @throws InvalidParseOperationException if parse json object exception occurred
     */
    public static <T> RequestResponseOK<T> getFromJsonNode(JsonNode node, Class<T> clazz)
        throws InvalidParseOperationException {
        return JsonHandler.getFromString(node.toString(), RequestResponseOK.class, clazz);
    }

    /**
     * @return the result of RequestResponse as a list of jsonNode
     * @throws InvalidParseOperationException
     */
    @JsonIgnore
    public List<JsonNode> getResultsAsJsonNodes() throws InvalidParseOperationException {
        if (results != null) {
            List<JsonNode> jsonNodeResults = new ArrayList<>();
            for (T result : results) {
                jsonNodeResults.add(JsonHandler.toJsonNode(result));
            }
            return jsonNodeResults;
        }
        return null;
    }

    public RequestResponseOK<T> setHttpCode(int httpCode) {
        super.setHttpCode(httpCode);
        return this;
    }

    /**
     * transform a RequestResponse to a standard response
     *
     * @return Response
     */
    @Override
    public Response toResponse() {
        final Response.ResponseBuilder resp = Response.status(getStatus()).entity(JsonHandler.unprettyPrint(this));
        final Map<String, String> vitamHeaders = getVitamHeaders();
        for (final String key : vitamHeaders.keySet()) {
            resp.header(key, getHeaderString(key));
        }

        unSetVitamHeaders();
        return resp.build();
    }
}
