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
package fr.gouv.vitam.common.database.builder.request.multiple;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.builder.facet.Facet;
import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.query.action.Action;
import fr.gouv.vitam.common.database.builder.request.AbstractRequest;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.GLOBAL;
import fr.gouv.vitam.common.database.builder.request.configuration.GlobalDatas;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * abstract class for multi query request
 */
public abstract class RequestMultiple extends AbstractRequest {

    protected Set<String> roots = new HashSet<>();
    protected List<Query> queries = new ArrayList<>();
    protected List<Facet> facets = new ArrayList<>();
    protected Long threshold;

    /**
     * @return this Request
     */
    public final RequestMultiple resetQueries() {
        if (queries != null) {
            queries.forEach(
                new Consumer<Query>() {
                    @Override
                    public void accept(Query t) {
                        t.clean();
                    }
                }
            );
            queries.clear();
        }
        return this;
    }

    /**
     * @return this Request
     */
    public final RequestMultiple resetRoots() {
        if (roots != null) {
            roots.clear();
        }
        return this;
    }

    /**
     * @return this Request
     */
    @Override
    public RequestMultiple reset() {
        resetRoots();
        resetFilter();
        resetQueries();
        return this;
    }

    /**
     * @param queries list of query
     * @return this Request
     * @throws InvalidCreateOperationException when query is invalid
     */
    public final RequestMultiple addQueries(final Query... queries) throws InvalidCreateOperationException {
        for (final Query query : queries) {
            ParametersChecker.checkParameter("Query is a mandatory parameter", query);
            if (!query.isReady()) {
                throw new InvalidCreateOperationException("Query is not ready to be added: " + query.getCurrentQuery());
            }
            this.queries.add(query);
        }
        return this;
    }

    @Override
    public RequestMultiple setQuery(Query query) throws InvalidCreateOperationException {
        ParametersChecker.checkParameter("Query is a mandatory parameter", query);
        if (!query.isReady()) {
            throw new InvalidCreateOperationException("Query is not ready to be added: " + query.getCurrentQuery());
        }
        queries = new ArrayList<>();
        queries.add(query);
        return this;
    }

    /**
     * @param roots string root
     * @return this Request
     * @throws InvalidParseOperationException when query is invalid
     */
    public final RequestMultiple addRoots(final String... roots) throws InvalidParseOperationException {
        for (final String root : roots) {
            GlobalDatas.sanityParameterCheck(root);
            this.roots.add(root);
        }
        return this;
    }

    /**
     * @param rootContent array of root
     * @return this Request
     */
    public final RequestMultiple addRoots(final ArrayNode rootContent) {
        for (final JsonNode jsonNode : rootContent) {
            roots.add(jsonNode.asText());
        }
        return this;
    }

    /**
     * @param roots string of array root
     * @return this Request
     * @throws InvalidParseOperationException when query is invalid
     */
    public final RequestMultiple parseRoots(String roots) throws InvalidParseOperationException {
        GlobalDatas.sanityParametersCheck(roots, GlobalDatas.NB_ROOTS);
        try {
            final ArrayNode rootNode = (ArrayNode) JsonHandler.getFromString(roots);
            return addRoots(rootNode);
        } catch (final Exception e) {
            throw new InvalidParseOperationException("Error while parsing Array of Roots", e);
        }
    }

    /**
     * Get the json final of request
     *
     * @return the Final containing all 3 parts: roots, queries array and filter
     */
    @Override
    protected final ObjectNode getFinal() {
        final ObjectNode node = JsonHandler.createObjectNode();
        if (roots != null && !roots.isEmpty()) {
            final ArrayNode array = node.putArray(GLOBAL.ROOTS.exactToken());
            for (final String val : roots) {
                array.add(val);
            }
        } else {
            node.putArray(GLOBAL.ROOTS.exactToken());
        }
        if (queries != null && !queries.isEmpty()) {
            final ArrayNode array = JsonHandler.createArrayNode();
            for (final Query query : queries) {
                array.add(query.getCurrentQuery());
            }
            node.set(GLOBAL.QUERY.exactToken(), array);
        } else {
            node.putArray(GLOBAL.QUERY.exactToken());
        }
        if (filter != null && filter.size() > 0) {
            node.set(GLOBAL.FILTER.exactToken(), filter);
        } else {
            node.putObject(GLOBAL.FILTER.exactToken());
        }
        return node;
    }

    /**
     * @return the roots array
     */
    @Override
    public final Set<String> getRoots() {
        return roots;
    }

    /**
     * @return the number of queries
     */
    @Override
    public final int getNbQueries() {
        return queries.size();
    }

    /**
     * @return the queries list
     */
    @Override
    public final List<Query> getQueries() {
        return queries;
    }

    /**
     * @param nth query position
     * @return the nth query
     */
    public final Query getNthQuery(int nth) {
        if (nth >= queries.size()) {
            return null;
        }
        return queries.get(nth);
    }

    /**
     * getFacets
     *
     * @return
     */
    public List<Facet> getFacets() {
        return facets;
    }

    /**
     * setFacets
     *
     * @param facets
     */
    public void setFacets(List<Facet> facets) {
        this.facets = facets;
    }

    /**
     * default implements of getData
     */
    @Override
    public JsonNode getData() {
        return JsonHandler.createObjectNode();
    }

    /**
     * default implements of getAllProjection
     */
    @Override
    public boolean getAllProjection() {
        return false;
    }

    /**
     * default implements of getProjection
     */
    @Override
    public ObjectNode getProjection() {
        return JsonHandler.createObjectNode();
    }

    /**
     * Getter for threshold
     *
     * @return the threshold
     */
    public Long getThreshold() {
        return threshold;
    }

    /**
     * Setter for threshold
     *
     * @param threshold the value to set for the threshold
     */
    public void setThreshold(Long threshold) {
        this.threshold = threshold;
    }

    /**
     * default implements of getActions
     */
    @Override
    public List<Action> getActions() {
        return Collections.emptyList();
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("Requests: ");
        for (final Query subrequest : getQueries()) {
            builder.append("\n").append(subrequest);
        }
        builder.append(super.toString()).append("\n\tRoots: ").append(roots);
        return builder.toString();
    }
}
