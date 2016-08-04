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
package fr.gouv.vitam.common.database.builder.request.multiple;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.GLOBAL;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTION;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.SELECTFILTER;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.configuration.GlobalDatas;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;

/**
 * Select: { $roots: roots, $query : query, $filter : filter, $projection : projection } or [ roots, query, filter,
 * projection ]
 *
 */
public class Select extends RequestMultiple {
    protected ObjectNode projection;

    /**
     *
     * @return this Query
     */
    public final Select resetLimitFilter() {
        if (filter != null) {
            filter.remove(SELECTFILTER.OFFSET.exactToken());
            filter.remove(SELECTFILTER.LIMIT.exactToken());
        }
        return this;
    }

    /**
     *
     * @return this Query
     */
    public final Select resetOrderByFilter() {
        if (filter != null) {
            filter.remove(SELECTFILTER.ORDERBY.exactToken());
        }
        return this;
    }

    /**
     *
     * @return this Query
     */
    public final Select resetUsedProjection() {
        if (projection != null) {
            projection.remove(PROJECTION.FIELDS.exactToken());
        }
        return this;
    }

    /**
     *
     * @return this Query
     */
    public final Select resetUsageProjection() {
        if (projection != null) {
            projection.remove(PROJECTION.USAGE.exactToken());
        }
        return this;
    }

    /**
     * @return this Query
     */
    @Override
    public final Select reset() {
        super.reset();
        resetUsageProjection();
        resetUsedProjection();
        return this;
    }

    /**
     * @param offset ignored if 0
     * @param limit ignored if 0
     * @return this Query
     */
    public final Select setLimitFilter(final long offset, final long limit) {
        if (filter == null) {
            filter = JsonHandler.createObjectNode();
        }
        resetLimitFilter();
        if (offset > 0) {
            filter.put(SELECTFILTER.OFFSET.exactToken(), offset);
        }
        if (limit > 0) {
            filter.put(SELECTFILTER.LIMIT.exactToken(), limit);
        }
        return this;
    }

    /**
     *
     * @param filterContent content json
     * @return this Query
     */
    public final Select setLimitFilter(final JsonNode filterContent) {
        long offset = 0;
        long limit = GlobalDatas.LIMIT_LOAD;
        if (filterContent.has(SELECTFILTER.LIMIT.exactToken())) {
            /*
             * $limit : n $maxScan: <number> / cursor.limit(n) "filter" : { "limit" : {"value" : n} } ou "from" : start,
             * "size" : n
             */
            limit = filterContent.get(SELECTFILTER.LIMIT.exactToken())
                .asLong(GlobalDatas.LIMIT_LOAD);
        }
        if (filterContent.has(SELECTFILTER.OFFSET.exactToken())) {
            /*
             * $offset : start cursor.skip(start) "from" : start, "size" : n
             */
            offset = filterContent.get(SELECTFILTER.OFFSET.exactToken()).asLong(0);
        }
        return setLimitFilter(offset, limit);
    }

    /**
     *
     * @param filter string filter
     * @return this Query
     * @throws InvalidParseOperationException when query is invalid
     */
    public final Select parseLimitFilter(final String filter)
        throws InvalidParseOperationException {
        GlobalDatas.sanityParametersCheck(filter, GlobalDatas.NB_FILTERS);
        final JsonNode rootNode = JsonHandler.getFromString(filter);
        return setLimitFilter(rootNode);
    }

    /**
     *
     * @param variableNames list of key name
     * @return this Query
     * @throws InvalidParseOperationException when query is invalid
     */
    public final Select addOrderByAscFilter(final String... variableNames)
        throws InvalidParseOperationException {
        return addOrderByFilter(1, variableNames);
    }

    /**
     *
     * @param variableNames list of key name
     * @return this Query
     * @throws InvalidParseOperationException when query is invalid
     */
    public final Select addOrderByDescFilter(final String... variableNames)
        throws InvalidParseOperationException {
        return addOrderByFilter(-1, variableNames);
    }

    /**
     *
     * @param way the way of the operation
     * @param variableNames list of key name
     * @return this Query
     * @throws InvalidParseOperationException when query is invalid
     */
    private final Select addOrderByFilter(final int way, final String... variableNames)
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
     *
     * @param filterContent json filter
     * @return this Query
     * @throws InvalidParseOperationException when query is invalid
     */
    public final Select addOrderByFilter(final JsonNode filterContent)
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
     *
     * @param filter string filter
     * @return this Query
     * @throws InvalidParseOperationException when query is invalid
     */
    public final Select parseOrderByFilter(final String filter)
        throws InvalidParseOperationException {
        GlobalDatas.sanityParametersCheck(filter, GlobalDatas.NB_FILTERS);
        final JsonNode rootNode = JsonHandler.getFromString(filter);
        return addOrderByFilter(rootNode);
    }

    /**
     *
     * @param filterContent json filter
     * @return this Query
     * @throws InvalidParseOperationException when query is invalid
     */
    @Override
    public final Select setFilter(final JsonNode filterContent)
        throws InvalidParseOperationException {
        super.setFilter(filterContent);
        return setLimitFilter(filterContent).addOrderByFilter(filterContent);
    }

    /**
     *
     * @param variableNames list of key name
     * @return this Query
     * @throws InvalidParseOperationException when query is invalid
     */
    public final Select addUsedProjection(final String... variableNames)
        throws InvalidParseOperationException {
        return addXxxProjection(1, variableNames);
    }

    /**
     *
     * @param variableNames list of key name
     * @return this Query
     * @throws InvalidParseOperationException when query is invalid
     */
    public final Select addUnusedProjection(final String... variableNames)
        throws InvalidParseOperationException {
        return addXxxProjection(0, variableNames);
    }

    /**
     *
     * @param way the way of the operation
     * @param variableNames list of key name
     * @return this Query
     * @throws InvalidParseOperationException when query is invalid
     */
    private final Select addXxxProjection(final int way, final String... variableNames)
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
     *
     * @param projectionContent json projection
     * @return this Query
     */
    public final Select addProjection(final JsonNode projectionContent) {
        if (projection == null) {
            projection = JsonHandler.createObjectNode();
        }
        if (projectionContent.has(PROJECTION.FIELDS.exactToken())) {
            final ObjectNode node =
                projection.putObject(PROJECTION.FIELDS.exactToken());
            node.setAll(
                (ObjectNode) projectionContent.get(PROJECTION.FIELDS.exactToken()));
        }
        return this;
    }

    /**
     *
     * @param projection string projection
     * @return this Query
     * @throws InvalidParseOperationException when query is invalid
     */
    public final Select parseProjection(final String projection)
        throws InvalidParseOperationException {
        GlobalDatas.sanityParametersCheck(projection, GlobalDatas.NB_PROJECTIONS);
        final JsonNode rootNode = JsonHandler.getFromString(projection);
        return setProjection(rootNode);
    }

    /**
     *
     * @param usage string
     * @return this Query
     * @throws InvalidParseOperationException when query is invalid
     */
    public final Select setUsageProjection(final String usage)
        throws InvalidParseOperationException {
        GlobalDatas.sanityParameterCheck(usage);
        if (projection == null) {
            projection = JsonHandler.createObjectNode();
        }
        if (usage == null || usage.trim().isEmpty()) {
            return this;
        }
        projection.put(PROJECTION.USAGE.exactToken(), usage.trim());
        return this;
    }

    /**
     *
     * @param projectionContent json projection
     * @return this Query
     * @throws InvalidParseOperationException when query is invalid
     */
    public final Select setUsageProjection(final JsonNode projectionContent)
        throws InvalidParseOperationException {
        resetUsageProjection();
        if (projectionContent.has(PROJECTION.USAGE.exactToken())) {
            setUsageProjection(
                projectionContent.get(PROJECTION.USAGE.exactToken()).asText());
        }
        return this;
    }

    /**
     *
     * @param projectionContent json projection
     * @return this Query
     * @throws InvalidParseOperationException when query is invalid
     */
    public final Select setProjection(final JsonNode projectionContent)
        throws InvalidParseOperationException {
        resetUsedProjection();
        return addProjection(projectionContent).setUsageProjection(projectionContent);
    }

    /**
     *
     * @return the Final Select containing all 4 parts: roots array, queries array, filter and projection
     */
    public final ObjectNode getFinalSelect() {
        final ObjectNode node = getFinal();
        if (projection != null && projection.size() > 0) {
            node.set(GLOBAL.PROJECTION.exactToken(), projection);
        } else {
            node.putObject(GLOBAL.PROJECTION.exactToken());
        }
        return node;
    }

    /**
     *
     * @return True if the projection is not restricted
     */
    public final boolean getAllProjection() {
        if (projection != null) {
            final ObjectNode node = (ObjectNode) projection.get(PROJECTION.FIELDS.exactToken());
            if (node == null || node.isMissingNode()) {
                return true;
            }
            final String all = VitamFieldsHelper.all();
            if (node.has(all) && node.get(all).asInt() > 0) {
                return true;
            }
            return false;
        }
        return true;
    }

    /**
     * @return the projection
     */
    public final ObjectNode getProjection() {
        if (projection == null) {
            return JsonHandler.createObjectNode();
        }
        return projection;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("QUERY: ").append(super.toString())
            .append("\n\tProjection: ").append(projection);
        return builder.toString();
    }

}
