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

import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTION;
import fr.gouv.vitam.common.database.builder.request.configuration.GlobalDatas;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;

/**
 * Select: { $roots: roots, $query : query, $filter : filter, $projection : projection } or [ roots, query, filter,
 * projection ]
 *
 */
public class Select extends RequestMultiple {
    /**
     *
     * @return this Query
     */
    public final Select resetLimitFilter() {
        selectResetLimitFilter();
        return this;
    }

    /**
     *
     * @return this Query
     */
    public final Select resetOrderByFilter() {
        selectResetOrderByFilter();
        return this;
    }

    /**
     *
     * @return this Query
     */
    public final Select resetUsedProjection() {
        selectResetUsedProjection();
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
        selectReset();
        return this;
    }

    /**
     * @param offset ignored if 0
     * @param limit ignored if 0
     * @return this Query
     */
    public final Select setLimitFilter(final long offset, final long limit) {
        selectSetLimitFilter(offset, limit);
        return this;
    }

    /**
     *
     * @param filterContent content json
     * @return this Query
     */
    public final Select setLimitFilter(final JsonNode filterContent) {
        selectSetLimitFilter(filterContent);
        return this;
    }

    /**
     *
     * @param filter string filter
     * @return this Query
     * @throws InvalidParseOperationException when query is invalid
     */
    public final Select parseLimitFilter(final String filter)
        throws InvalidParseOperationException {
        selectParseLimitFilter(filter);
        return this;
    }

    /**
     *
     * @param variableNames list of key name
     * @return this Query
     * @throws InvalidParseOperationException when query is invalid
     */
    public final Select addOrderByAscFilter(final String... variableNames)
        throws InvalidParseOperationException {
        selectAddOrderByAscFilter(variableNames);
        return this;
    }

    /**
     *
     * @param variableNames list of key name
     * @return this Query
     * @throws InvalidParseOperationException when query is invalid
     */
    public final Select addOrderByDescFilter(final String... variableNames)
        throws InvalidParseOperationException {
        selectAddOrderByDescFilter(variableNames);
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
        selectAddOrderByFilter(filterContent);
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
        selectParseOrderByFilter(filter);
        return this;
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
        selectSetFilter(filterContent);
        return this;
    }

    /**
     *
     * @param variableNames list of key name
     * @return this Query
     * @throws InvalidParseOperationException when query is invalid
     */
    public final Select addUsedProjection(final String... variableNames)
        throws InvalidParseOperationException {
        selectAddUsedProjection(variableNames);
        return this;
    }

    /**
     *
     * @param variableNames list of key name
     * @return this Query
     * @throws InvalidParseOperationException when query is invalid
     */
    public final Select addUnusedProjection(final String... variableNames)
        throws InvalidParseOperationException {
        selectAddUnusedProjection(variableNames);
        return this;
    }

    /**
     *
     * @param projectionContent json projection
     * @return this Query
     */
    public final Select addProjection(final JsonNode projectionContent) {
        selectAddProjection(projectionContent);
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
        selectParseProjection(projection);
        return this;
    }

    /**
     * Specific command to get the correct Qualifier and Version from ObjectGroup. By default always return "_id".
     *
     * @param qualifier might be either Xxx or Xxx_n
     * @param version
     * @param additionalFields additional fields
     * @throws InvalidParseOperationException
     */
    public void setProjectionSliceOnQualifier(String qualifier, int version, String... additionalFields)
        throws InvalidParseOperationException {
        // FIXME P1 : it would be nice to be able to handle $slice in projection via builder
        String projection =
            "{\"$fields\":{\"#qualifiers." + qualifier.trim().split("_")[0] + ".versions\": { $slice: [" + version +
                ",1]},\"#id\":0," + "\"#qualifiers." + qualifier.trim().split("_")[0] + ".versions._id\":1";
        for (final String field : additionalFields) {
            projection += ",\"#qualifiers." + qualifier.trim().split("_")[0] + ".versions." + field + "\":1";
        }
        projection += "}}";


        parseProjection(projection);
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

    @Override
    protected final Select selectSetProjection(final JsonNode projectionContent)
        throws InvalidParseOperationException {
        super.selectSetProjection(projectionContent);
        setUsageProjection(projectionContent);
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
        selectSetProjection(projectionContent);
        return this;
    }

    /**
     *
     * @return the Final Select containing all 4 parts: roots array, queries array, filter and projection
     */
    public final ObjectNode getFinalSelect() {
        return selectGetFinalSelect();
    }

    /**
     *
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

}
