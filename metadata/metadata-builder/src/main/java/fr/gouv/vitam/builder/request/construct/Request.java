/*******************************************************************************
 * This file is part of Vitam Project.
 *
 * Copyright Vitam (2012, 2016)
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL license as circulated
 * by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL license and that you
 * accept its terms.
 *******************************************************************************/
package fr.gouv.vitam.builder.request.construct;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.gouv.vitam.builder.request.construct.configuration.GlobalDatas;
import fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.GLOBAL;
import fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.SELECTFILTER;
import fr.gouv.vitam.builder.request.construct.query.Query;
import fr.gouv.vitam.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;

/**
 * Abstract Request (common part): { $roots: roots, $query : query, $filter : filter } or [ roots, query, filter ]
 *
 */
public abstract class Request {
    protected Set<String> roots = new HashSet<String>();
    protected List<Query> queries = new ArrayList<Query>();
    protected ObjectNode filter;

    /**
     *
     * @return this Request
     */
    public final Request resetHintFilter() {
        if (filter != null) {
            filter.remove(SELECTFILTER.HINT.exactToken());
        }
        return this;
    }

    /**
     *
     * @return this Request
     */
    public final Request resetFilter() {
        if (filter != null) {
            filter.removeAll();
        }
        return this;
    }

    /**
     *
     * @return this Request
     */
    public final Request resetQueries() {
        if (queries != null) {
            queries.forEach(new Consumer<Query>() {
                @Override
                public void accept(Query t) {
                    t.clean();
                }
            });
            queries.clear();
        }
        return this;
    }

    /**
     * @return this Request
     */
    public final Request resetRoots() {
        if (roots != null) {
            roots.clear();
        }
        return this;
    }

    /**
     * @return this Request
     */
    public Request reset() {
        resetRoots();
        resetFilter();
        resetQueries();
        return this;
    }

    /**
     *
     * @param hints
     * @return this Request
     * @throws InvalidParseOperationException
     */
    public final Request addHintFilter(final String... hints)
        throws InvalidParseOperationException {
        if (filter == null) {
            filter = JsonHandler.createObjectNode();
        }
        ArrayNode array = (ArrayNode) filter.get(SELECTFILTER.HINT.exactToken());
        if (array == null || array.isMissingNode()) {
            array = filter.putArray(SELECTFILTER.HINT.exactToken());
        }
        for (final String hint : hints) {
            GlobalDatas.sanityParameterCheck(hint);
            if (hint == null || hint.trim().isEmpty()) {
                continue;
            }
            array.add(hint.trim());
        }
        return this;
    }

    /**
     *
     * @param filterContent
     * @return this Request
     */
    public final Request addHintFilter(final JsonNode filterContent) {
        if (filter == null) {
            filter = JsonHandler.createObjectNode();
        }
        if (filterContent.has(SELECTFILTER.HINT.exactToken())) {
            final JsonNode node = filterContent.get(SELECTFILTER.HINT.exactToken());
            if (node.isArray()) {
                filter.putArray(SELECTFILTER.HINT.exactToken()).addAll((ArrayNode) node);
            } else {
                filter.putArray(SELECTFILTER.HINT.exactToken()).add(node.asText());
            }
        }
        return this;
    }

    /**
     *
     * @param filter
     * @return this Request
     * @throws InvalidParseOperationException
     */
    public final Request parseHintFilter(final String filter)
        throws InvalidParseOperationException {
        GlobalDatas.sanityParametersCheck(filter, GlobalDatas.NB_FILTERS);
        final JsonNode rootNode = JsonHandler.getFromString(filter);
        return addHintFilter(rootNode);
    }

    /**
     *
     * @param filterContent
     * @return this Request
     * @throws InvalidParseOperationException
     */
    public Request setFilter(final JsonNode filterContent)
        throws InvalidParseOperationException {
        resetFilter();
        return addHintFilter(filterContent);
    }

    /**
     *
     * @param filter
     * @return this Request
     * @throws InvalidParseOperationException
     */
    public final Request parseFilter(final String filter)
        throws InvalidParseOperationException {
        GlobalDatas.sanityParametersCheck(filter, GlobalDatas.NB_FILTERS);
        final JsonNode filterContent = JsonHandler.getFromString(filter);
        return setFilter(filterContent);
    }

    /**
     *
     * @param queries
     * @return this Request
     * @throws InvalidCreateOperationException
     */
    public final Request addQueries(final Query... queries)
        throws InvalidCreateOperationException {
        for (final Query query : queries) {
            if (!query.isReady()) {
                throw new InvalidCreateOperationException(
                    "Query is not ready to be added: " + query.getCurrentQuery());
            }
            this.queries.add(query);
        }
        return this;
    }

    /**
     *
     * @param roots
     * @return this Request
     * @throws InvalidParseOperationException
     */
    public final Request addRoots(final String... roots)
        throws InvalidParseOperationException {
        for (final String root : roots) {
            GlobalDatas.sanityParameterCheck(root);
            this.roots.add(root);
        }
        return this;
    }

    /**
     *
     * @param rootContent
     * @return this Request
     */
    public final Request addRoots(final ArrayNode rootContent) {
        for (final JsonNode jsonNode : rootContent) {
            roots.add(jsonNode.asText());
        }
        return this;
    }

    /**
     *
     * @param roots
     * @return this Request
     * @throws InvalidParseOperationException
     */
    public final Request parseRoots(String roots) throws InvalidParseOperationException {
        GlobalDatas.sanityParametersCheck(roots, GlobalDatas.NB_ROOTS);
        try {
            final ArrayNode rootNode = (ArrayNode) JsonHandler.getFromString(roots);
            return addRoots(rootNode);
        } catch (final Exception e) {
            throw new InvalidParseOperationException("Error while parsing Array of Roots",
                e);
        }
    }

    /**
     *
     * @return the Final containing all 3 parts: roots, queries array and filter
     */
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
    public final Set<String> getRoots() {
        return roots;
    }

    /**
     * @return the number of queries
     */
    public final int getNbQueries() {
        return queries.size();
    }

    /**
     * @return the queries list
     */
    public final List<Query> getQueries() {
        return queries;
    }

    /**
     * @param nth
     * @return the nth query
     */
    public final Query getNthQuery(int nth) {
        if (nth >= queries.size()) {
            return null;
        }
        return queries.get(nth);
    }

    /**
     * @return the filter
     */
    public final ObjectNode getFilter() {
        if (filter == null) {
            return JsonHandler.createObjectNode();
        }
        return filter;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("Requests: ");
        for (final Query subrequest : getQueries()) {
            builder.append("\n").append(subrequest);
        }
        builder.append("\n\tFilter: ").append(filter).append("\n\tRoots: ").append(roots);
        return builder.toString();
    }

}
