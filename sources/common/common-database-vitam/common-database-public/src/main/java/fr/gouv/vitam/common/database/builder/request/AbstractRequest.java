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
package fr.gouv.vitam.common.database.builder.request;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.query.action.Action;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.GLOBAL;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTION;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.SELECTFILTER;
import fr.gouv.vitam.common.database.builder.request.configuration.GlobalDatas;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;

import java.util.List;
import java.util.Set;

/**
 * Common Abstract Request
 */
public abstract class AbstractRequest {

    protected ObjectNode filter;
    protected ObjectNode projection;

    /**
     * @return this Request
     */
    public final AbstractRequest resetHintFilter() {
        if (filter != null) {
            filter.remove(SELECTFILTER.HINT.exactToken());
        }
        return this;
    }

    /**
     * @return this Request
     */
    public final AbstractRequest resetFilter() {
        if (filter != null) {
            filter.removeAll();
        }
        return this;
    }

    /**
     * @return this Request
     */
    public AbstractRequest reset() {
        resetFilter();
        return this;
    }

    /**
     * @param hints list of hint
     * @return this Request
     * @throws InvalidParseOperationException when query is invalid
     */
    public final AbstractRequest addHintFilter(final String... hints) throws InvalidParseOperationException {
        ParametersChecker.checkParameter("Hint filter is a mandatory parameter", hints);
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
     * @param filterContent json filter
     * @return this Request
     */
    public final AbstractRequest addHintFilter(final JsonNode filterContent) {
        ParametersChecker.checkParameter("Filter Content is a mandatory parameter", filterContent);
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
     * @param filterContent json filter
     * @return this Request
     * @throws InvalidParseOperationException when query is invalid
     */
    public AbstractRequest setFilter(final JsonNode filterContent) throws InvalidParseOperationException {
        resetFilter();
        return addHintFilter(filterContent);
    }

    /**
     * @param filter String filter
     * @return this Request
     * @throws InvalidParseOperationException when query is invalid
     */
    public final AbstractRequest parseFilter(final String filter) throws InvalidParseOperationException {
        GlobalDatas.sanityParametersCheck(filter, GlobalDatas.NB_FILTERS);
        final JsonNode filterContent = JsonHandler.getFromString(filter);
        return setFilter(filterContent);
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
        builder.append("\n\tFilter: ").append(filter);
        return builder.toString();
    }

    /**
     * Set the query of request: in case of multi-query request: re-initialize list of query
     *
     * @param query of request
     * @return this request
     * @throws InvalidCreateOperationException when query is invalid
     */
    public abstract AbstractRequest setQuery(Query query) throws InvalidCreateOperationException;

    /**
     * @return the number of queries
     */
    public abstract int getNbQueries();

    /**
     * @return the queries list
     */
    public abstract List<Query> getQueries();

    /**
     * @return the queries list
     */
    public abstract Set<String> getRoots();

    /**
     * @return the data
     */
    public abstract JsonNode getData();

    /**
     * @return list of actions
     */
    public abstract List<Action> getActions();

    /**
     * @return True if the projection is not restricted
     */
    public abstract boolean getAllProjection();

    /**
     * @return the projection
     */
    public abstract ObjectNode getProjection();

    /******************************************************/
    /** Refactoring for SELECT part in protected methods **/
    /******************************************************/

    /**
     * @return this Query
     */
    protected final AbstractRequest selectResetLimitFilter() {
        if (filter != null) {
            filter.remove(SELECTFILTER.OFFSET.exactToken());
            filter.remove(SELECTFILTER.LIMIT.exactToken());
        }
        return this;
    }

    /**
     * @return this Query
     */
    protected final AbstractRequest selectResetOrderByFilter() {
        if (filter != null) {
            filter.remove(SELECTFILTER.ORDERBY.exactToken());
        }
        return this;
    }

    /**
     * @return this Query
     */
    protected final AbstractRequest selectResetUsedProjection() {
        if (projection != null) {
            projection.remove(PROJECTION.FIELDS.exactToken());
        }
        return this;
    }

    protected final AbstractRequest selectReset() {
        selectResetUsedProjection();
        return this;
    }

    /**
     * @param scrollId ignored if empty or null
     * @param scrollTimeout ignored if 0
     * @param limit ignored if 0
     * @return this Query
     */
    protected final AbstractRequest selectSetScrollFilter(
        final String scrollId,
        final int scrollTimeout,
        final int limit
    ) {
        if (filter == null) {
            filter = JsonHandler.createObjectNode();
        }
        selectResetLimitFilter();
        if (scrollId != null && !scrollId.isEmpty()) {
            filter.put(SELECTFILTER.SCROLL_ID.exactToken(), scrollId);
        }
        if (limit > 0) {
            filter.put(SELECTFILTER.LIMIT.exactToken(), limit);
        }
        if (scrollTimeout > 0) {
            filter.put(SELECTFILTER.SCROLL_TIMEOUT.exactToken(), scrollTimeout);
        }
        return this;
    }

    /**
     * @param offset ignored if 0
     * @param limit ignored if 0
     * @return this Query
     */
    protected final AbstractRequest selectSetLimitFilter(final long offset, final long limit) {
        if (filter == null) {
            filter = JsonHandler.createObjectNode();
        }
        selectResetLimitFilter();
        if (offset > 0) {
            filter.put(SELECTFILTER.OFFSET.exactToken(), offset);
        }
        if (limit > 0) {
            filter.put(SELECTFILTER.LIMIT.exactToken(), limit);
        }
        return this;
    }

    /**
     * @param filterContent json filter
     * @return this Query
     */
    protected final AbstractRequest selectSetLimitFilter(final JsonNode filterContent) {
        long offset = 0;
        int limit = GlobalDatas.LIMIT_LOAD;
        String scrollId = null;
        int timeout = 0;
        if (filterContent.has(SELECTFILTER.LIMIT.exactToken())) {
            /*
             * $limit : n $maxScan: <number> / cursor.limit(n) "filter" : { "limit" : {"value" : n} } ou "from" : start,
             * "size" : n
             */
            limit = filterContent.get(SELECTFILTER.LIMIT.exactToken()).asInt(GlobalDatas.LIMIT_LOAD);
        }
        if (filterContent.has(SELECTFILTER.OFFSET.exactToken())) {
            /*
             * $offset : start cursor.skip(start) "from" : start, "size" : n
             */
            offset = filterContent.get(SELECTFILTER.OFFSET.exactToken()).asLong(0);
        }
        if (filterContent.has(SELECTFILTER.SCROLL_ID.exactToken())) {
            /*
             * $offset : start cursor.skip(start) "from" : start, "size" : n
             */
            scrollId = filterContent.get(SELECTFILTER.SCROLL_ID.exactToken()).asText();
            if (filterContent.has(SELECTFILTER.SCROLL_TIMEOUT.exactToken())) {
                /*
                 * $offset : start cursor.skip(start) "from" : start, "size" : n
                 */
                timeout = filterContent.get(SELECTFILTER.SCROLL_TIMEOUT.exactToken()).asInt(0);
            }
            selectSetScrollFilter(scrollId, timeout, limit);
        }

        return selectSetLimitFilter(offset, limit);
    }

    /**
     * @param filter string filter
     * @return this Query
     * @throws InvalidParseOperationException when query is invalid
     */
    protected final AbstractRequest selectParseLimitFilter(final String filter) throws InvalidParseOperationException {
        GlobalDatas.sanityParametersCheck(filter, GlobalDatas.NB_FILTERS);
        final JsonNode rootNode = JsonHandler.getFromString(filter);
        return selectSetLimitFilter(rootNode);
    }

    /**
     * @param variableNames list of key name
     * @return this Query
     * @throws InvalidParseOperationException when query is invalid
     */
    protected final AbstractRequest selectAddOrderByAscFilter(final String... variableNames)
        throws InvalidParseOperationException {
        return addOrderByFilter(1, variableNames);
    }

    /**
     * @param variableNames list of key name
     * @return this Query
     * @throws InvalidParseOperationException when query is invalid
     */
    protected final AbstractRequest selectAddOrderByDescFilter(final String... variableNames)
        throws InvalidParseOperationException {
        return addOrderByFilter(-1, variableNames);
    }

    /**
     * @param way the way of the operation
     * @param variableNames list of key name
     * @return this Query
     * @throws InvalidParseOperationException when query is invalid
     */
    private final AbstractRequest addOrderByFilter(final int way, final String... variableNames)
        throws InvalidParseOperationException {
        if (filter == null) {
            filter = JsonHandler.createObjectNode();
        }
        ObjectNode node = (ObjectNode) filter.get(SELECTFILTER.ORDERBY.exactToken());
        if (node == null || node.isMissingNode()) {
            node = filter.putObject(SELECTFILTER.ORDERBY.exactToken());
        }
        for (final String var : variableNames) {
            if (var == null || var.trim().isEmpty()) {
                continue;
            }
            GlobalDatas.sanityParameterCheck(var);
            node.put(var.trim(), way);
        }
        return this;
    }

    /**
     * @param filterContent json filter
     * @return this Query
     * @throws InvalidParseOperationException when query is invalid
     */
    protected final AbstractRequest selectAddOrderByFilter(final JsonNode filterContent)
        throws InvalidParseOperationException {
        if (filter == null) {
            filter = JsonHandler.createObjectNode();
        }
        if (filterContent.has(SELECTFILTER.ORDERBY.exactToken())) {
            /*
             * $orderby : { key : +/-1, ... } $orderby: { key : +/-1, ... } "sort" : [ { "key" : "asc/desc"}, ...,
             * "_score" ]
             */
            final JsonNode node = filterContent.get(SELECTFILTER.ORDERBY.exactToken());
            filter.putObject(SELECTFILTER.ORDERBY.exactToken()).setAll((ObjectNode) node);
        }
        return this;
    }

    /**
     * @param filter string filter
     * @return this Query
     * @throws InvalidParseOperationException when query is invalid
     */
    protected final AbstractRequest selectParseOrderByFilter(final String filter)
        throws InvalidParseOperationException {
        GlobalDatas.sanityParametersCheck(filter, GlobalDatas.NB_FILTERS);
        final JsonNode rootNode = JsonHandler.getFromString(filter);
        return selectAddOrderByFilter(rootNode);
    }

    protected final AbstractRequest selectSetFilter(final JsonNode filterContent)
        throws InvalidParseOperationException {
        return selectSetLimitFilter(filterContent).selectAddOrderByFilter(filterContent);
    }

    /**
     * @param variableNames list of key name
     * @return this Query
     * @throws InvalidParseOperationException when query is invalid
     */
    protected final AbstractRequest selectAddUsedProjection(final String... variableNames)
        throws InvalidParseOperationException {
        return addXxxProjection(1, variableNames);
    }

    /**
     * @param variableNames list of key name
     * @return this Query
     * @throws InvalidParseOperationException when query is invalid
     */
    protected final AbstractRequest selectAddUnusedProjection(final String... variableNames)
        throws InvalidParseOperationException {
        return addXxxProjection(0, variableNames);
    }

    /**
     * @param way the way of the operation
     * @param variableNames list of key name
     * @return this Query
     * @throws InvalidParseOperationException when query is invalid
     */
    private final AbstractRequest addXxxProjection(final int way, final String... variableNames)
        throws InvalidParseOperationException {
        if (projection == null) {
            projection = JsonHandler.createObjectNode();
        }
        ObjectNode node = (ObjectNode) projection.get(PROJECTION.FIELDS.exactToken());
        if (node == null || node.isMissingNode()) {
            node = projection.putObject(PROJECTION.FIELDS.exactToken());
        }
        for (final String var : variableNames) {
            if (var == null || var.trim().isEmpty()) {
                continue;
            }
            GlobalDatas.sanityParameterCheck(var);
            node.put(var.trim(), way);
        }
        if (node.size() == 0) {
            projection.remove(PROJECTION.FIELDS.exactToken());
        }
        return this;
    }

    /**
     * @param projectionContent json projection
     * @return this Query
     */
    protected final AbstractRequest selectAddProjection(final JsonNode projectionContent) {
        if (projection == null) {
            projection = JsonHandler.createObjectNode();
        }
        if (projectionContent.has(PROJECTION.FIELDS.exactToken())) {
            final ObjectNode node = projection.putObject(PROJECTION.FIELDS.exactToken());
            node.setAll((ObjectNode) projectionContent.get(PROJECTION.FIELDS.exactToken()));
        }
        return this;
    }

    /**
     * @param projection string projection
     * @return this Query
     * @throws InvalidParseOperationException when query is invalid
     */
    protected final AbstractRequest selectParseProjection(final String projection)
        throws InvalidParseOperationException {
        GlobalDatas.sanityParametersCheck(projection, GlobalDatas.NB_PROJECTIONS);
        final JsonNode rootNode = JsonHandler.getFromString(projection);
        return selectSetProjection(rootNode);
    }

    /**
     * @param projectionContent json projection
     * @return this Query
     * @throws InvalidParseOperationException when query is invalid
     */
    protected AbstractRequest selectSetProjection(final JsonNode projectionContent)
        throws InvalidParseOperationException {
        selectResetUsedProjection();
        return selectAddProjection(projectionContent);
    }

    /**
     * Get the json final of request
     *
     * @return the Final json containing all 2 parts: query and filter
     */
    protected abstract ObjectNode getFinal();

    /**
     * @return the Final Select containing all 3 parts: query, filter and projection
     */
    protected final ObjectNode selectGetFinalSelect() {
        final ObjectNode node = getFinal();
        if (projection != null && projection.size() > 0) {
            node.set(GLOBAL.PROJECTION.exactToken(), projection);
        } else {
            node.putObject(GLOBAL.PROJECTION.exactToken());
        }
        return node;
    }

    /**
     * @return True if the projection is not restricted
     */
    protected boolean selectGetAllProjection() {
        if (projection != null) {
            final ObjectNode node = (ObjectNode) projection.get(PROJECTION.FIELDS.exactToken());
            if (node == null || node.isMissingNode()) {
                return true;
            }
            final String all = VitamFieldsHelper.all();
            if (node.has(all) && node.get(all).asInt() > 0) {
                return true;
            }
            if (!node.fieldNames().hasNext()) {
                return true;
            }
            return false;
        }
        return true;
    }

    /**
     * @return the projection
     */
    protected ObjectNode selectGetProjection() {
        if (projection == null) {
            return JsonHandler.createObjectNode();
        }
        return projection;
    }
}
