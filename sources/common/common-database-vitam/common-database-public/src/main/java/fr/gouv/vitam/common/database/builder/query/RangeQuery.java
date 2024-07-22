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
package fr.gouv.vitam.common.database.builder.query;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.QUERY;
import fr.gouv.vitam.common.database.builder.request.configuration.GlobalDatas;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;

import java.util.Date;

/**
 * Range Query
 */
public class RangeQuery extends Query {

    private static final String IS_NOT_A_VALID_COMPARE_QUERY = " is not a valid Compare Query";
    private static final String CANNOT_BE_UPDATED_WITH_EMPTY_VARIABLE_NAME =
        " cannot be updated with empty variable name";
    private static final String QUERY2 = "Query ";

    protected RangeQuery() {
        super();
    }

    /**
     * Range Query constructor
     *
     * @param variableName key name
     * @param from gt, gte
     * @param valueFrom start value
     * @param to lt, lte
     * @param valueTo end value
     * @throws InvalidCreateOperationException when can not create query
     */
    public RangeQuery(
        final String variableName,
        final QUERY from,
        final long valueFrom,
        final QUERY to,
        final long valueTo
    ) throws InvalidCreateOperationException {
        super();
        if (variableName == null || variableName.trim().isEmpty()) {
            throw new InvalidCreateOperationException(
                QUERY2 + currentTokenQUERY + CANNOT_BE_UPDATED_WITH_EMPTY_VARIABLE_NAME
            );
        }
        try {
            GlobalDatas.sanityParameterCheck(variableName);
        } catch (final InvalidParseOperationException e) {
            throw new InvalidCreateOperationException(e);
        }
        switch (from) {
            case GT:
            case GTE:
                break;
            default:
                throw new InvalidCreateOperationException(QUERY2 + from + IS_NOT_A_VALID_COMPARE_QUERY);
        }
        switch (to) {
            case LT:
            case LTE:
                break;
            default:
                throw new InvalidCreateOperationException(QUERY2 + to + IS_NOT_A_VALID_COMPARE_QUERY);
        }
        final ObjectNode sub =
            ((ObjectNode) currentObject).putObject(QUERY.RANGE.exactToken()).putObject(variableName.trim());
        sub.put(from.exactToken(), valueFrom);
        sub.put(to.exactToken(), valueTo);
        currentTokenQUERY = QUERY.RANGE;
        setReady(true);
    }

    /**
     * Range Query constructor
     *
     * @param variableName key name
     * @param from gt, gte
     * @param valueFrom start value
     * @param to lt, lte
     * @param valueTo end value
     * @throws InvalidCreateOperationException when can not create query
     */
    public RangeQuery(
        final String variableName,
        final QUERY from,
        final double valueFrom,
        final QUERY to,
        final double valueTo
    ) throws InvalidCreateOperationException {
        super();
        if (variableName == null || variableName.trim().isEmpty()) {
            throw new InvalidCreateOperationException(
                QUERY2 + currentTokenQUERY + CANNOT_BE_UPDATED_WITH_EMPTY_VARIABLE_NAME
            );
        }
        try {
            GlobalDatas.sanityParameterCheck(variableName);
        } catch (final InvalidParseOperationException e) {
            throw new InvalidCreateOperationException(e);
        }
        switch (from) {
            case GT:
            case GTE:
                break;
            default:
                throw new InvalidCreateOperationException(QUERY2 + from + IS_NOT_A_VALID_COMPARE_QUERY);
        }
        switch (to) {
            case LT:
            case LTE:
                break;
            default:
                throw new InvalidCreateOperationException(QUERY2 + to + IS_NOT_A_VALID_COMPARE_QUERY);
        }
        final ObjectNode sub =
            ((ObjectNode) currentObject).putObject(QUERY.RANGE.exactToken()).putObject(variableName.trim());
        sub.put(from.exactToken(), valueFrom);
        sub.put(to.exactToken(), valueTo);
        currentTokenQUERY = QUERY.RANGE;
        setReady(true);
    }

    /**
     * Range Query constructor
     *
     * @param variableName key name
     * @param from gt, gte
     * @param valueFrom start value
     * @param to lt, lte
     * @param valueTo end value
     * @throws InvalidCreateOperationException when can not create query
     */
    public RangeQuery(
        final String variableName,
        final QUERY from,
        final String valueFrom,
        final QUERY to,
        final String valueTo
    ) throws InvalidCreateOperationException {
        super();
        if (variableName == null || variableName.trim().isEmpty()) {
            throw new InvalidCreateOperationException(
                QUERY2 + currentTokenQUERY + CANNOT_BE_UPDATED_WITH_EMPTY_VARIABLE_NAME
            );
        }
        try {
            GlobalDatas.sanityParameterCheck(variableName);
            GlobalDatas.sanityValueCheck(valueFrom);
            GlobalDatas.sanityValueCheck(valueTo);
        } catch (final InvalidParseOperationException e) {
            throw new InvalidCreateOperationException(e);
        }
        switch (from) {
            case GT:
            case GTE:
                break;
            default:
                throw new InvalidCreateOperationException(QUERY2 + from + IS_NOT_A_VALID_COMPARE_QUERY);
        }
        switch (to) {
            case LT:
            case LTE:
                break;
            default:
                throw new InvalidCreateOperationException(QUERY2 + to + IS_NOT_A_VALID_COMPARE_QUERY);
        }
        final ObjectNode sub =
            ((ObjectNode) currentObject).putObject(QUERY.RANGE.exactToken()).putObject(variableName.trim());
        sub.put(from.exactToken(), valueFrom);
        sub.put(to.exactToken(), valueTo);
        currentTokenQUERY = QUERY.RANGE;
        setReady(true);
    }

    /**
     * Range Query constructor
     *
     * @param variableName key name
     * @param from gt, gte
     * @param valueFrom start value
     * @param to lt, lte
     * @param valueTo end value
     * @throws InvalidCreateOperationException when can not create query
     */
    public RangeQuery(
        final String variableName,
        final QUERY from,
        final Date valueFrom,
        final QUERY to,
        final Date valueTo
    ) throws InvalidCreateOperationException {
        super();
        if (variableName == null || variableName.trim().isEmpty()) {
            throw new InvalidCreateOperationException(
                QUERY2 + currentTokenQUERY + CANNOT_BE_UPDATED_WITH_EMPTY_VARIABLE_NAME
            );
        }
        try {
            GlobalDatas.sanityParameterCheck(variableName);
        } catch (final InvalidParseOperationException e) {
            throw new InvalidCreateOperationException(e);
        }
        switch (from) {
            case GT:
            case GTE:
                break;
            default:
                throw new InvalidCreateOperationException(QUERY2 + from + IS_NOT_A_VALID_COMPARE_QUERY);
        }
        switch (to) {
            case LT:
            case LTE:
                break;
            default:
                throw new InvalidCreateOperationException(QUERY2 + to + IS_NOT_A_VALID_COMPARE_QUERY);
        }
        final ObjectNode sub =
            ((ObjectNode) currentObject).putObject(QUERY.RANGE.exactToken()).putObject(variableName.trim());
        sub.set(
            from.exactToken(),
            new TextNode(LocalDateUtil.getFormattedDateTimeForMongo(LocalDateUtil.fromDate(valueFrom)))
        );
        sub.set(
            to.exactToken(),
            new TextNode(LocalDateUtil.getFormattedDateTimeForMongo(LocalDateUtil.fromDate(valueTo)))
        );
        currentTokenQUERY = QUERY.RANGE;
        setReady(true);
    }
}
