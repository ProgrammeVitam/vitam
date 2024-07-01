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
package fr.gouv.vitam.common.database.builder.request.single;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;

/**
 * SELECT for Single Mode Query
 */
public class Select extends RequestSingle {

    /**
     * @return this Query
     */
    public final Select resetLimitFilter() {
        selectResetLimitFilter();
        return this;
    }

    /**
     * @return this Query
     */
    public final Select resetOrderByFilter() {
        selectResetOrderByFilter();
        return this;
    }

    /**
     * @return this Query
     */
    public final Select resetUsedProjection() {
        selectResetUsedProjection();
        return this;
    }

    @Override
    public final Select reset() {
        super.reset();
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
     * @param filterContent json filter
     * @return this Query
     */
    public final Select setLimitFilter(final JsonNode filterContent) {
        selectSetLimitFilter(filterContent);
        return this;
    }

    /**
     * @param filter string filter
     * @return this Query
     * @throws InvalidParseOperationException when query is invalid
     */
    public final Select parseLimitFilter(final String filter) throws InvalidParseOperationException {
        selectParseLimitFilter(filter);
        return this;
    }

    /**
     * @param variableNames list of key name
     * @return this Query
     * @throws InvalidParseOperationException when query is invalid
     */
    public final Select addOrderByAscFilter(final String... variableNames) throws InvalidParseOperationException {
        selectAddOrderByAscFilter(variableNames);
        return this;
    }

    /**
     * @param variableNames list of key name
     * @return this Query
     * @throws InvalidParseOperationException when query is invalid
     */
    public final Select addOrderByDescFilter(final String... variableNames) throws InvalidParseOperationException {
        selectAddOrderByDescFilter(variableNames);
        return this;
    }

    /**
     * @param filterContent json filter
     * @return this Query
     * @throws InvalidParseOperationException when query is invalid
     */
    public final Select addOrderByFilter(final JsonNode filterContent) throws InvalidParseOperationException {
        selectAddOrderByFilter(filterContent);
        return this;
    }

    /**
     * @param filter string filter
     * @return this Query
     * @throws InvalidParseOperationException when query is invalid
     */
    public final Select parseOrderByFilter(final String filter) throws InvalidParseOperationException {
        selectParseOrderByFilter(filter);
        return this;
    }

    @Override
    public final Select setFilter(final JsonNode filterContent) throws InvalidParseOperationException {
        super.setFilter(filterContent);
        selectSetFilter(filterContent);
        return this;
    }

    /**
     * @param variableNames list of key name
     * @return this Query
     * @throws InvalidParseOperationException when query is invalid
     */
    public final Select addUsedProjection(final String... variableNames) throws InvalidParseOperationException {
        selectAddUsedProjection(variableNames);
        return this;
    }

    /**
     * @param variableNames list of key name
     * @return this Query
     * @throws InvalidParseOperationException when query is invalid
     */
    public final Select addUnusedProjection(final String... variableNames) throws InvalidParseOperationException {
        selectAddUnusedProjection(variableNames);
        return this;
    }

    /**
     * @param projectionContent json projection
     * @return this Query
     */
    public final Select addProjection(final JsonNode projectionContent) {
        selectAddProjection(projectionContent);
        return this;
    }

    /**
     * @param projection string projection
     * @return this Query
     * @throws InvalidParseOperationException when query is invalid
     */
    public final Select parseProjection(final String projection) throws InvalidParseOperationException {
        selectParseProjection(projection);
        return this;
    }

    /**
     * @param projectionContent json projection
     * @return this Query
     * @throws InvalidParseOperationException when query is invalid
     */
    public final Select setProjection(final JsonNode projectionContent) throws InvalidParseOperationException {
        selectSetProjection(projectionContent);
        return this;
    }

    /**
     * @return the Final Select containing all 3 parts: query, filter and projection
     */
    public final ObjectNode getFinalSelect() {
        return selectGetFinalSelect();
    }

    /**
     * @return the Final Select By Id containing only one parts: projection
     */
    public final ObjectNode getFinalSelectById() {
        final ObjectNode objectNode = selectGetFinalSelect();
        objectNode.remove(BuilderToken.GLOBAL.FILTER.exactToken());
        objectNode.remove(BuilderToken.GLOBAL.QUERY.exactToken());
        return objectNode;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("QUERY: ").append(super.toString()).append("\n\tProjection: ").append(projection);
        return builder.toString();
    }
}
