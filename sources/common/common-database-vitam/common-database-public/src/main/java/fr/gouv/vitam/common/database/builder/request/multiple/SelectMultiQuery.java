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
package fr.gouv.vitam.common.database.builder.request.multiple;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTION;
import fr.gouv.vitam.common.database.builder.request.configuration.GlobalDatas;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;

/**
 * Select: { $roots: roots, $query : query, $filter : filter, $projection : projection } or [ roots, query, filter,
 * projection ]
 */
public class SelectMultiQuery extends RequestMultiple {
    /**
     * @return this Query
     */
    public final SelectMultiQuery resetLimitFilter() {
        selectResetLimitFilter();
        return this;
    }

    /**
     * @return this Query
     */
    public final SelectMultiQuery resetOrderByFilter() {
        selectResetOrderByFilter();
        return this;
    }

    /**
     * @return this Query
     */
    public final SelectMultiQuery resetUsedProjection() {
        selectResetUsedProjection();
        return this;
    }

    /**
     * @return this Query
     */
    public final SelectMultiQuery resetUsageProjection() {
        if (projection != null) {
            projection.remove(PROJECTION.USAGE.exactToken());
        }
        return this;
    }

    /**
     * @return this Query
     */
    @Override
    public final SelectMultiQuery reset() {
        super.reset();
        resetUsageProjection();
        selectReset();
        return this;
    }

    /**
     * @param offset ignored if 0
     * @param limit  ignored if 0
     * @return this Query
     */
    public final SelectMultiQuery setLimitFilter(final long offset, final long limit) {
        selectSetLimitFilter(offset, limit);
        return this;
    }

    /**
     * @param filterContent content json
     * @return this Query
     */
    public final SelectMultiQuery setLimitFilter(final JsonNode filterContent) {
        selectSetLimitFilter(filterContent);
        return this;
    }

    /**
     * @param filter string filter
     * @return this Query
     * @throws InvalidParseOperationException when query is invalid
     */
    public final SelectMultiQuery parseLimitFilter(final String filter)
        throws InvalidParseOperationException {
        selectParseLimitFilter(filter);
        return this;
    }

    /**
     * @param variableNames list of key name
     * @return this Query
     * @throws InvalidParseOperationException when query is invalid
     */
    public final SelectMultiQuery addOrderByAscFilter(final String... variableNames)
        throws InvalidParseOperationException {
        selectAddOrderByAscFilter(variableNames);
        return this;
    }

    /**
     * @param variableNames list of key name
     * @return this Query
     * @throws InvalidParseOperationException when query is invalid
     */
    public final SelectMultiQuery addOrderByDescFilter(final String... variableNames)
        throws InvalidParseOperationException {
        selectAddOrderByDescFilter(variableNames);
        return this;
    }

    /**
     * @param filterContent json filter
     * @return this Query
     * @throws InvalidParseOperationException when query is invalid
     */
    public final SelectMultiQuery addOrderByFilter(final JsonNode filterContent)
        throws InvalidParseOperationException {
        selectAddOrderByFilter(filterContent);
        return this;
    }

    /**
     * @param filter string filter
     * @return this Query
     * @throws InvalidParseOperationException when query is invalid
     */
    public final SelectMultiQuery parseOrderByFilter(final String filter)
        throws InvalidParseOperationException {
        selectParseOrderByFilter(filter);
        return this;
    }

    /**
     * @param filterContent json filter
     * @return this Query
     * @throws InvalidParseOperationException when query is invalid
     */
    @Override
    public final SelectMultiQuery setFilter(final JsonNode filterContent)
        throws InvalidParseOperationException {
        super.setFilter(filterContent);
        selectSetFilter(filterContent);
        return this;
    }

    /**
     * @param variableNames list of key name
     * @return this Query
     * @throws InvalidParseOperationException when query is invalid
     */
    public final SelectMultiQuery addUsedProjection(final String... variableNames)
        throws InvalidParseOperationException {
        selectAddUsedProjection(variableNames);
        return this;
    }

    /**
     * @param variableNames list of key name
     * @return this Query
     * @throws InvalidParseOperationException when query is invalid
     */
    public final SelectMultiQuery addUnusedProjection(final String... variableNames)
        throws InvalidParseOperationException {
        selectAddUnusedProjection(variableNames);
        return this;
    }

    /**
     * @param projectionContent json projection
     * @return this Query
     */
    public final SelectMultiQuery addProjection(final JsonNode projectionContent) {
        selectAddProjection(projectionContent);
        return this;
    }

    /**
     * @param projection string projection
     * @return this Query
     * @throws InvalidParseOperationException when query is invalid
     */
    public final SelectMultiQuery parseProjection(final String projection)
        throws InvalidParseOperationException {
        selectParseProjection(projection);
        return this;
    }

    /**
     * Specific command to get the correct Qualifier and Version from ObjectGroup. By default always return "_id".
     *
     * @param additionalFields additional fields
     * @throws InvalidParseOperationException when projection parse exception occurred
     */
    public void setProjectionSliceOnQualifier(String... additionalFields)
        throws InvalidParseOperationException {
        // FIXME P1 : it would be nice to be able to handle $slice in projection via builder
        String projection =
            "{\"$fields\":{\"#qualifiers.versions\":1,\"#id\":0," + "\"#qualifiers.versions._id\":1," +
                "\"#qualifiers.qualifier\":1" + ",\"#qualifiers.versions.DataObjectVersion\":1";
        for (final String field : additionalFields) {
            projection += ",\"#qualifiers.versions." + field + "\":1";
        }
        projection += "}}";
        parseProjection(projection);
    }

    /**
     * @param usage string
     * @return this Query
     * @throws InvalidParseOperationException when query is invalid
     */
    public final SelectMultiQuery setUsageProjection(final String usage)
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
     * @param projectionContent json projection
     * @return this Query
     * @throws InvalidParseOperationException when query is invalid
     */
    public final SelectMultiQuery setUsageProjection(final JsonNode projectionContent)
        throws InvalidParseOperationException {
        resetUsageProjection();
        if (projectionContent.has(PROJECTION.USAGE.exactToken())) {
            setUsageProjection(projectionContent.get(PROJECTION.USAGE.exactToken()).asText());
        }
        return this;
    }

    @Override
    protected final SelectMultiQuery selectSetProjection(final JsonNode projectionContent)
        throws InvalidParseOperationException {
        super.selectSetProjection(projectionContent);
        setUsageProjection(projectionContent);
        return this;
    }

    /**
     * @param projectionContent json projection
     * @return this Query
     * @throws InvalidParseOperationException when query is invalid
     */
    public final SelectMultiQuery setProjection(final JsonNode projectionContent)
        throws InvalidParseOperationException {
        selectSetProjection(projectionContent);
        return this;
    }

    /**
     * @return the Final Select containing all 4 parts: roots array, queries array, filter and projection
     */
    public final ObjectNode getFinalSelect() {
        return selectGetFinalSelect();
    }

    /**
     * @return the Final Select By Id containing only one part: projection
     */
    public final ObjectNode getFinalSelectById() {
        final ObjectNode objectNode = selectGetFinalSelect();
        objectNode.remove(BuilderToken.GLOBAL.QUERY.exactToken());
        objectNode.remove(BuilderToken.GLOBAL.FILTER.exactToken());
        objectNode.remove(BuilderToken.GLOBAL.ROOTS.exactToken());
        return objectNode;
    }

    /**
     * @return True if the projection is not restricted
     */
    @Override
    public final boolean getAllProjection() {
        return selectGetAllProjection();
    }

    /**
     * @return the projection
     */
    @Override
    public final ObjectNode getProjection() {
        return selectGetProjection();
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("QUERY: ").append(super.toString())
            .append("\n\tProjection: ").append(projection);
        return builder.toString();
    }


    /**
     * @param scrollId ignored if empty or null
     * @param scrollTimeout ignored if 0
     * @param limit ignored if 0
     * @return this Query
     */
    public final SelectMultiQuery setScrollFilter(final String scrollId, final int scrollTimeout, final int limit) {
        selectSetScrollFilter(scrollId, scrollTimeout, limit);
        return this;
    }

}
