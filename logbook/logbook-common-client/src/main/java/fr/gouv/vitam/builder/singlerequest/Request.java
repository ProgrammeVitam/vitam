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
package fr.gouv.vitam.builder.singlerequest;


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
 * Logbook Abstract Request (common part): { $query : query, $filter : filter } or [ query, filter ]
 *
 */
public abstract class Request {
    protected Query query;
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
    public final Request resetQuery() {
        if (query != null) {
            query.clean();
        }
        return this;
    }

    /**
     * @return this Request
     */
    public Request reset() {
        resetFilter();
        resetQuery();
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
     * @param query
     * @return this Request
     * @throws InvalidCreateOperationException
     */
    public final Request setQuery(final Query query)
        throws InvalidCreateOperationException {
        if (query == null) {
            throw new InvalidCreateOperationException("Null Query");
        }
        if (!query.isReady()) {
            throw new InvalidCreateOperationException(
                "Query is not ready to be set: " + query.getCurrentQuery());
        }
        resetQuery();
        this.query = query;
        return this;
    }

    /**
     *
     * @return the Final containing all 2 parts: queries array and filter
     */
    protected final ObjectNode getFinal() {
        final ObjectNode node = JsonHandler.createObjectNode();
        if (query != null) {
            node.set(GLOBAL.QUERY.exactToken(), query.getCurrentQuery());
        } else {
            node.putObject(GLOBAL.QUERY.exactToken());
        }
        if (filter != null && filter.size() > 0) {
            node.set(GLOBAL.FILTER.exactToken(), filter);
        } else {
            node.putObject(GLOBAL.FILTER.exactToken());
        }
        return node;
    }

    /**
     * @return the query
     */
    public final Query getQuery() {
        return query;
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
        return new StringBuilder()
            .append("Requests: ").append(query != null ? query : "").append("\n\tFilter: ").append(filter)
            .toString();
    }

}
