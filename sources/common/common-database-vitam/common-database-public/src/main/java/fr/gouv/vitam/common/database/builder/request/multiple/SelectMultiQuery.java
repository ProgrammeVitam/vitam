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
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.GLOBAL;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTION;
import fr.gouv.vitam.common.database.builder.request.configuration.GlobalDatas;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Select: { $roots: roots, $query : query, $filter : filter, $projection : projection, $facets : facet }
 */
public class SelectMultiQuery extends RequestMultiple {

    /**
     * Facets
     */
    protected List<Facet> facets = new ArrayList<>();

    protected Long threshold;

    /**
     * @return this Request
     */
    public final SelectMultiQuery resetFacets() {
        if (facets != null) {
            facets.clear();
        }
        return this;
    }

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
        resetFacets();
        resetUsageProjection();
        selectReset();
        return this;
    }

    /**
     * @param offset ignored if 0
     * @param limit ignored if 0
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
     * @param trackTotalHits to set. false (default) to return approximative total hits, true to return exact total hits
     * @return this Query
     */
    public final SelectMultiQuery trackTotalHits(boolean trackTotalHits) {
        if (filter == null) {
            filter = JsonHandler.createObjectNode();
        }
        if (trackTotalHits) {
            filter.put(BuilderToken.SELECTFILTER.TRACK_TOTAL_HITS.exactToken(), true);
        } else {
            filter.remove(BuilderToken.SELECTFILTER.TRACK_TOTAL_HITS.exactToken());
        }
        return this;
    }

    /**
     * @param filter string filter
     * @return this Query
     * @throws InvalidParseOperationException when query is invalid
     */
    public final SelectMultiQuery parseLimitFilter(final String filter) throws InvalidParseOperationException {
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
    public final SelectMultiQuery addOrderByFilter(final JsonNode filterContent) throws InvalidParseOperationException {
        selectAddOrderByFilter(filterContent);
        return this;
    }

    /**
     * @param filter string filter
     * @return this Query
     * @throws InvalidParseOperationException when query is invalid
     */
    public final SelectMultiQuery parseOrderByFilter(final String filter) throws InvalidParseOperationException {
        selectParseOrderByFilter(filter);
        return this;
    }

    /**
     * @param filterContent json filter
     * @return this Query
     * @throws InvalidParseOperationException when query is invalid
     */
    @Override
    public final SelectMultiQuery setFilter(final JsonNode filterContent) throws InvalidParseOperationException {
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
    public final SelectMultiQuery parseProjection(final String projection) throws InvalidParseOperationException {
        selectParseProjection(projection);
        return this;
    }

    /**
     * @param usage string
     * @return this Query
     * @throws InvalidParseOperationException when query is invalid
     */
    public final SelectMultiQuery setUsageProjection(final String usage) throws InvalidParseOperationException {
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
     * @return the Final Select containing all 5 parts: roots array, queries array, facets array, filter and projection
     */
    public final ObjectNode getFinalSelect() {
        final ObjectNode node = selectGetFinalSelect();
        addFacetsToNode(node);
        addThresholdToNode(node);
        return node;
    }

    /**
     * @return the Final Select By Id containing only one part: projection
     */
    public final ObjectNode getFinalSelectById() {
        final ObjectNode objectNode = selectGetFinalSelect();
        objectNode.remove(BuilderToken.GLOBAL.ROOTS.exactToken());
        objectNode.remove(BuilderToken.GLOBAL.QUERY.exactToken());
        objectNode.remove(BuilderToken.GLOBAL.FILTER.exactToken());
        objectNode.remove(BuilderToken.GLOBAL.FACETS.exactToken());
        return objectNode;
    }

    /**
     * Add facets to given node
     *
     * @param node with facets
     */
    protected void addFacetsToNode(ObjectNode node) {
        if (facets != null && !facets.isEmpty()) {
            final ArrayNode array = JsonHandler.createArrayNode();
            for (final Facet facet : facets) {
                array.add(facet.getCurrentFacet());
            }
            node.set(GLOBAL.FACETS.exactToken(), array);
        } else {
            node.putArray(GLOBAL.FACETS.exactToken());
        }
    }

    private void addThresholdToNode(ObjectNode node) {
        if (threshold != null) {
            node.put(GLOBAL.THRESOLD.exactToken(), threshold);
        }
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

    /**
     * @return the facets
     */
    public final List<Facet> getFacets() {
        return facets;
    }

    /**
     * @param facets list of facet
     * @return this Request
     * @throws IllegalArgumentException when facet is invalid
     */
    public final SelectMultiQuery addFacets(final Facet... facets) {
        for (final Facet facet : facets) {
            ParametersChecker.checkParameter("Facet is a mandatory parameter", facet);
            this.facets.add(facet);
        }
        return this;
    }

    /**
     * @param facet facet
     * @return this Request
     * @throws IllegalArgumentException when facet is invalid
     */
    public SelectMultiQuery setFacet(Facet facet) {
        ParametersChecker.checkParameter("Facet is a mandatory parameter", facet);
        facets = new ArrayList<>();
        facets.add(facet);
        return this;
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

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("QUERY: ").append(super.toString());
        builder.append("\n\tProjection: ").append(projection);
        builder.append("\n\tFacets: ");
        for (final Facet subrequest : getFacets()) {
            builder.append("\n").append(subrequest);
        }
        builder.append("\n\tThreshold: ").append(threshold);
        return builder.toString();
    }
}
