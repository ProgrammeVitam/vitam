/*******************************************************************************
 * This file is part of Vitam Project.
 *
 * Copyright Vitam (2012, 2015)
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
package fr.gouv.vitam.logbook.common.dsl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.gouv.vitam.builder.request.construct.VitamFieldsHelper;
import fr.gouv.vitam.builder.request.construct.configuration.GlobalDatas;
import fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.GLOBAL;
import fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.PROJECTION;
import fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.SELECTFILTER;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;

/**
 * Select: { $query : query, $filter : filter, $projection : projection } or [ query, filter, projection ]
 *
 */
public class Select extends Request {
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
     * @param filterContent
     * @return this Query
     */
    public final Select setLimitFilter(final JsonNode filterContent) {
        long offset = 0;
        long limit = GlobalDatas.limitLoad;
        if (filterContent.has(SELECTFILTER.LIMIT.exactToken())) {
            /*
             * $limit : n $maxScan: <number> / cursor.limit(n) "filter" : { "limit" : {"value" : n} } ou "from" : start,
             * "size" : n
             */
            limit = filterContent.get(SELECTFILTER.LIMIT.exactToken())
                .asLong(GlobalDatas.limitLoad);
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
     * @param filter
     * @return this Query
     * @throws InvalidParseOperationException
     */
    public final Select parseLimitFilter(final String filter)
        throws InvalidParseOperationException {
        GlobalDatas.sanityParametersCheck(filter, GlobalDatas.nbFilters);
        final JsonNode rootNode = JsonHandler.getFromString(filter);
        return setLimitFilter(rootNode);
    }

    /**
     *
     * @param variableNames
     * @return this Query
     * @throws InvalidParseOperationException
     */
    public final Select addOrderByAscFilter(final String... variableNames)
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
            node.put(var.trim(), 1);
        }
        return this;
    }

    /**
     *
     * @param variableNames
     * @return this Query
     * @throws InvalidParseOperationException
     */
    public final Select addOrderByDescFilter(final String... variableNames)
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
            node.put(var.trim(), -1);
        }
        return this;
    }

    /**
     *
     * @param filterContent
     * @return this Query
     * @throws InvalidParseOperationException
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
     * @param filter
     * @return this Query
     * @throws InvalidParseOperationException
     */
    public final Select parseOrderByFilter(final String filter)
        throws InvalidParseOperationException {
        GlobalDatas.sanityParametersCheck(filter, GlobalDatas.nbFilters);
        final JsonNode rootNode = JsonHandler.getFromString(filter);
        return addOrderByFilter(rootNode);
    }

    @Override
    public final Select setFilter(final JsonNode filterContent)
        throws InvalidParseOperationException {
        super.setFilter(filterContent);
        return setLimitFilter(filterContent).addOrderByFilter(filterContent);
    }

    /**
     *
     * @param variableNames
     * @return this Query
     * @throws InvalidParseOperationException
     */
    public final Select addUsedProjection(final String... variableNames)
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
            node.put(var.trim(), 1);
        }
        if (node.size() == 0) {
            projection.remove(PROJECTION.FIELDS.exactToken());
        }
        return this;
    }

    /**
     *
     * @param variableNames
     * @return this Query
     * @throws InvalidParseOperationException
     */
    public final Select addUnusedProjection(final String... variableNames)
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
            node.put(var.trim(), 0);
        }
        if (node.size() == 0) {
            projection.remove(PROJECTION.FIELDS.exactToken());
        }
        return this;
    }

    /**
     *
     * @param projectionContent
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
     * @param projection
     * @return this Query
     * @throws InvalidParseOperationException
     */
    public final Select parseProjection(final String projection)
        throws InvalidParseOperationException {
        GlobalDatas.sanityParametersCheck(projection, GlobalDatas.nbProjections);
        final JsonNode rootNode = JsonHandler.getFromString(projection);
        return setProjection(rootNode);
    }

    /**
     *
     * @param projectionContent
     * @return this Query
     * @throws InvalidParseOperationException
     */
    public final Select setProjection(final JsonNode projectionContent)
        throws InvalidParseOperationException {
        resetUsedProjection();
        return addProjection(projectionContent);
    }

    /**
     *
     * @return the Final Select containing all 3 parts: query, filter and projection
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
