/*******************************************************************************
 * This file is part of Vitam Project.
 * 
 * Copyright Vitam (2012, 2015)
 *
 * This software is governed by the CeCILL 2.1 license under French law and
 * abiding by the rules of distribution of free software. You can use, modify
 * and/ or redistribute the software under the terms of the CeCILL license as
 * circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify
 * and redistribute granted by the license, users are provided only with a
 * limited warranty and the software's author, the holder of the economic
 * rights, and the successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with
 * loading, using, modifying and/or developing or reproducing the software by
 * the user in light of its specific status of free software, that may mean that
 * it is complicated to manipulate, and that also therefore means that it is
 * reserved for developers and experienced professionals having in-depth
 * computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling
 * the security of their systems and/or data to be ensured and, more generally,
 * to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 *******************************************************************************/
package fr.gouv.vitam.builder.request.construct.query;

import java.util.Date;

import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.gouv.vitam.builder.request.construct.configuration.GlobalDatas;
import fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.QUERY;
import fr.gouv.vitam.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;

/**
 * Range Query
 *
 */
public class RangeQuery extends Query {
    protected RangeQuery() {
        super();
    }

    /**
     * Range Query constructor
     *
     * @param variableName
     * @param from
     *            gt, gte
     * @param valueFrom
     * @param to
     *            lt, lte
     * @param valueTo
     * @throws InvalidCreateOperationException
     *
     */
    public RangeQuery(final String variableName, final QUERY from, final long valueFrom,
            final QUERY to, final long valueTo)
            throws InvalidCreateOperationException {
        super();
        if (variableName == null || variableName.trim().isEmpty()) {
            throw new InvalidCreateOperationException("Query " + currentQUERY
                    + " cannot be updated with empty variable name");
        }
        try {
            GlobalDatas.sanityParameterCheck(variableName);
        } catch (InvalidParseOperationException e) {
            throw new InvalidCreateOperationException(e);
        }
        switch (from) {
            case gt:
            case gte:
                break;
            default:
                throw new InvalidCreateOperationException(
                        "Query " + from + " is not a valid Compare Query");
        }
        switch (to) {
            case lt:
            case lte:
                break;
            default:
                throw new InvalidCreateOperationException(
                        "Query " + to + " is not a valid Compare Query");
        }
        final ObjectNode sub = ((ObjectNode) currentObject)
                .putObject(QUERY.range.exactToken()).putObject(variableName.trim());
        sub.put(from.exactToken(), valueFrom);
        sub.put(to.exactToken(), valueTo);
        currentQUERY = QUERY.range;
        setReady(true);
    }

    /**
     * Range Query constructor
     *
     * @param variableName
     * @param from
     *            gt, gte
     * @param valueFrom
     * @param to
     *            lt, lte
     * @param valueTo
     * @throws InvalidCreateOperationException
     */
    public RangeQuery(final String variableName, final QUERY from, final double valueFrom,
            final QUERY to,
            final double valueTo) throws InvalidCreateOperationException {
        super();
        if (variableName == null || variableName.trim().isEmpty()) {
            throw new InvalidCreateOperationException("Query " + currentQUERY
                    + " cannot be updated with empty variable name");
        }
        try {
            GlobalDatas.sanityParameterCheck(variableName);
        } catch (InvalidParseOperationException e) {
            throw new InvalidCreateOperationException(e);
        }
        switch (from) {
            case gt:
            case gte:
                break;
            default:
                throw new InvalidCreateOperationException(
                        "Query " + from + " is not a valid Compare Query");
        }
        switch (to) {
            case lt:
            case lte:
                break;
            default:
                throw new InvalidCreateOperationException(
                        "Query " + to + " is not a valid Compare Query");
        }
        final ObjectNode sub = ((ObjectNode) currentObject)
                .putObject(QUERY.range.exactToken()).putObject(variableName.trim());
        sub.put(from.exactToken(), valueFrom);
        sub.put(to.exactToken(), valueTo);
        currentQUERY = QUERY.range;
        setReady(true);
    }

    /**
     * Range Query constructor
     *
     * @param variableName
     * @param from
     *            gt, gte
     * @param valueFrom
     * @param to
     *            lt, lte
     * @param valueTo
     * @throws InvalidCreateOperationException
     */
    public RangeQuery(final String variableName, final QUERY from, final String valueFrom,
            final QUERY to,
            final String valueTo) throws InvalidCreateOperationException {
        super();
        if (variableName == null || variableName.trim().isEmpty()) {
            throw new InvalidCreateOperationException("Query " + currentQUERY
                    + " cannot be updated with empty variable name");
        }
        try {
            GlobalDatas.sanityParameterCheck(variableName);
            GlobalDatas.sanityValueCheck(valueFrom);
            GlobalDatas.sanityValueCheck(valueTo);
        } catch (InvalidParseOperationException e) {
            throw new InvalidCreateOperationException(e);
        }
        switch (from) {
            case gt:
            case gte:
                break;
            default:
                throw new InvalidCreateOperationException(
                        "Query " + from + " is not a valid Compare Query");
        }
        switch (to) {
            case lt:
            case lte:
                break;
            default:
                throw new InvalidCreateOperationException(
                        "Query " + to + " is not a valid Compare Query");
        }
        final ObjectNode sub = ((ObjectNode) currentObject)
                .putObject(QUERY.range.exactToken()).putObject(variableName.trim());
        sub.put(from.exactToken(), valueFrom);
        sub.put(to.exactToken(), valueTo);
        currentQUERY = QUERY.range;
        setReady(true);
    }

    /**
     * Range Query constructor
     *
     * @param variableName
     * @param from
     *            gt, gte
     * @param valueFrom
     * @param to
     *            lt, lte
     * @param valueTo
     * @throws InvalidCreateOperationException
     */
    public RangeQuery(final String variableName, final QUERY from, final Date valueFrom,
            final QUERY to, final Date valueTo)
            throws InvalidCreateOperationException {
        super();
        if (variableName == null || variableName.trim().isEmpty()) {
            throw new InvalidCreateOperationException("Query " + currentQUERY
                    + " cannot be updated with empty variable name");
        }
        try {
            GlobalDatas.sanityParameterCheck(variableName);
        } catch (InvalidParseOperationException e) {
            throw new InvalidCreateOperationException(e);
        }
        switch (from) {
            case gt:
            case gte:
                break;
            default:
                throw new InvalidCreateOperationException(
                        "Query " + from + " is not a valid Compare Query");
        }
        switch (to) {
            case lt:
            case lte:
                break;
            default:
                throw new InvalidCreateOperationException(
                        "Query " + to + " is not a valid Compare Query");
        }
        final ObjectNode sub = ((ObjectNode) currentObject)
                .putObject(QUERY.range.exactToken()).putObject(variableName.trim());
        sub.set(from.exactToken(), GlobalDatas.getDate(valueFrom));
        sub.set(to.exactToken(), GlobalDatas.getDate(valueTo));
        currentQUERY = QUERY.range;
        setReady(true);
    }
}
