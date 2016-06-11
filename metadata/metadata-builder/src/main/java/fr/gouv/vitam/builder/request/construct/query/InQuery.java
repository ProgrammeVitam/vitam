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
package fr.gouv.vitam.builder.request.construct.query;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.gouv.vitam.builder.request.construct.configuration.GlobalDatas;
import fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.QUERY;
import fr.gouv.vitam.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;

/**
 * In and Nin queries
 *
 */
public class InQuery extends Query {
    protected Set<Boolean> booleanVals;
    protected Set<Long> longVals;
    protected Set<Double> doubleVals;
    protected Set<String> stringVals;

    protected InQuery() {
        super();
    }

    @Override
    public void clean() {
        super.clean();
        if (booleanVals != null) {
            booleanVals.clear();
        }
        booleanVals = null;
        if (longVals != null) {
            longVals.clear();
        }
        longVals = null;
        if (doubleVals != null) {
            doubleVals.clear();
        }
        doubleVals = null;
        if (stringVals != null) {
            stringVals.clear();
        }
        stringVals = null;
    }

    /**
     * In Query constructor
     *
     * @param inQuery in, nin
     * @param variableName
     * @param value
     * @throws InvalidCreateOperationException
     */
    public InQuery(final QUERY inQuery, final String variableName, final long value)
        throws InvalidCreateOperationException {
        super();
        switch (inQuery) {
            case IN:
            case NIN: {
                if (variableName == null || variableName.trim().isEmpty()) {
                    throw new InvalidCreateOperationException(
                        "Query " + inQuery + " cannot be created with empty variable name");
                }
                try {
                    GlobalDatas.sanityParameterCheck(variableName);
                } catch (final InvalidParseOperationException e) {
                    throw new InvalidCreateOperationException(e);
                }
                final ObjectNode sub =
                    ((ObjectNode) currentObject).putObject(inQuery.exactToken());
                final ArrayNode array = sub.putArray(variableName.trim());
                array.add(value);
                longVals = new HashSet<Long>();
                longVals.add(value);
                currentObject = array;
                currentQUERY = inQuery;
                setReady(true);
                break;
            }
            default:
                throw new InvalidCreateOperationException(
                    "Query " + inQuery + " is not an In Query");
        }
    }

    /**
     * In Query constructor
     *
     * @param inQuery in, nin
     * @param variableName
     * @param value
     * @throws InvalidCreateOperationException
     */
    public InQuery(final QUERY inQuery, final String variableName, final double value)
        throws InvalidCreateOperationException {
        super();
        switch (inQuery) {
            case IN:
            case NIN: {
                if (variableName == null || variableName.trim().isEmpty()) {
                    throw new InvalidCreateOperationException(
                        "Query " + inQuery + " cannot be created with empty variable name");
                }
                try {
                    GlobalDatas.sanityParameterCheck(variableName);
                } catch (final InvalidParseOperationException e) {
                    throw new InvalidCreateOperationException(e);
                }
                final ObjectNode sub =
                    ((ObjectNode) currentObject).putObject(inQuery.exactToken());
                final ArrayNode array = sub.putArray(variableName.trim());
                array.add(value);
                doubleVals = new HashSet<Double>();
                doubleVals.add(value);
                currentObject = array;
                currentQUERY = inQuery;
                setReady(true);
                break;
            }
            default:
                throw new InvalidCreateOperationException(
                    "Query " + inQuery + " is not an In Query");
        }
    }

    /**
     * In Query constructor
     *
     * @param inQuery in, nin
     * @param variableName
     * @param value
     * @throws InvalidCreateOperationException
     */
    public InQuery(final QUERY inQuery, final String variableName, final String value)
        throws InvalidCreateOperationException {
        super();
        switch (inQuery) {
            case IN:
            case NIN: {
                if (variableName == null || variableName.trim().isEmpty()) {
                    throw new InvalidCreateOperationException(
                        "Query " + inQuery + " cannot be created with empty variable name");
                }
                try {
                    GlobalDatas.sanityParameterCheck(variableName);
                    GlobalDatas.sanityValueCheck(value);
                } catch (final InvalidParseOperationException e) {
                    throw new InvalidCreateOperationException(e);
                }
                final ObjectNode sub =
                    ((ObjectNode) currentObject).putObject(inQuery.exactToken());
                final ArrayNode array = sub.putArray(variableName.trim());
                array.add(value);
                stringVals = new HashSet<String>();
                stringVals.add(value);
                currentObject = array;
                currentQUERY = inQuery;
                setReady(true);
                break;
            }
            default:
                throw new InvalidCreateOperationException(
                    "Query " + inQuery + " is not an In or Search Query");
        }
    }

    /**
     * In Query constructor
     *
     * @param inQuery in, nin
     * @param variableName
     * @param value
     * @throws InvalidCreateOperationException
     */
    public InQuery(final QUERY inQuery, final String variableName, final Date value)
        throws InvalidCreateOperationException {
        super();
        switch (inQuery) {
            case IN:
            case NIN: {
                if (variableName == null || variableName.trim().isEmpty()) {
                    throw new InvalidCreateOperationException(
                        "Query " + inQuery + " cannot be created with empty variable name");
                }
                try {
                    GlobalDatas.sanityParameterCheck(variableName);
                } catch (final InvalidParseOperationException e) {
                    throw new InvalidCreateOperationException(e);
                }
                final ObjectNode sub =
                    ((ObjectNode) currentObject).putObject(inQuery.exactToken());
                final ArrayNode array = sub.putArray(variableName.trim());
                final String sdate = LocalDateUtil.fromDate(value).toString();
                array.add(GlobalDatas.getDate(value));
                stringVals = new HashSet<String>();
                stringVals.add(sdate);
                currentObject = array;
                currentQUERY = inQuery;
                setReady(true);
                break;
            }
            default:
                throw new InvalidCreateOperationException(
                    "Query " + inQuery + " is not an In or Search Query");
        }
    }

    /**
     * In Query constructor
     *
     * @param inQuery in, nin
     * @param variableName
     * @param value
     * @throws InvalidCreateOperationException
     */
    public InQuery(final QUERY inQuery, final String variableName, final boolean value)
        throws InvalidCreateOperationException {
        super();
        switch (inQuery) {
            case IN:
            case NIN: {
                if (variableName == null || variableName.trim().isEmpty()) {
                    throw new InvalidCreateOperationException(
                        "Query " + inQuery + " cannot be created with empty variable name");
                }
                try {
                    GlobalDatas.sanityParameterCheck(variableName);
                } catch (final InvalidParseOperationException e) {
                    throw new InvalidCreateOperationException(e);
                }
                final ObjectNode sub =
                    ((ObjectNode) currentObject).putObject(inQuery.exactToken());
                final ArrayNode array = sub.putArray(variableName.trim());
                array.add(value);
                booleanVals = new HashSet<Boolean>();
                booleanVals.add(value);
                currentObject = array;
                currentQUERY = inQuery;
                setReady(true);
                break;
            }
            default:
                throw new InvalidCreateOperationException(
                    "Query " + inQuery + " is not an In Query");
        }
    }

    /**
     * In Query constructor
     *
     * @param inQuery in, nin
     * @param variableName
     * @param values
     * @throws InvalidCreateOperationException
     */
    public InQuery(final QUERY inQuery, final String variableName, final String... values)
        throws InvalidCreateOperationException {
        super();
        if (variableName == null || variableName.trim().isEmpty()) {
            throw new InvalidCreateOperationException(
                "Query " + inQuery + " cannot be created with empty variable name");
        }
        try {
            GlobalDatas.sanityParameterCheck(variableName);
        } catch (final InvalidParseOperationException e) {
            throw new InvalidCreateOperationException(e);
        }
        switch (inQuery) {
            case IN:
            case NIN: {
                final ObjectNode sub =
                    ((ObjectNode) currentObject).putObject(inQuery.exactToken());
                final ArrayNode array = sub.putArray(variableName.trim());
                stringVals = new HashSet<String>();
                for (final String value : values) {
                    try {
                        GlobalDatas.sanityValueCheck(value);
                    } catch (final InvalidParseOperationException e) {
                        throw new InvalidCreateOperationException(e);
                    }
                    if (!stringVals.contains(value)) {
                        array.add(value);
                        stringVals.add(value);
                    }
                }
                currentObject = array;
                break;
            }
            default:
                throw new InvalidCreateOperationException(
                    "Query " + inQuery + " is not an In Query");
        }
        currentQUERY = inQuery;
        setReady(true);
    }

    /**
     * In Query constructor
     *
     * @param inQuery in, nin
     * @param variableName
     * @param values
     * @throws InvalidCreateOperationException
     */
    public InQuery(final QUERY inQuery, final String variableName, final Date... values)
        throws InvalidCreateOperationException {
        super();
        if (variableName == null || variableName.trim().isEmpty()) {
            throw new InvalidCreateOperationException(
                "Query " + inQuery + " cannot be created with empty variable name");
        }
        try {
            GlobalDatas.sanityParameterCheck(variableName);
        } catch (final InvalidParseOperationException e) {
            throw new InvalidCreateOperationException(e);
        }
        switch (inQuery) {
            case IN:
            case NIN: {
                final ObjectNode sub =
                    ((ObjectNode) currentObject).putObject(inQuery.exactToken());
                final ArrayNode array = sub.putArray(variableName.trim());
                stringVals = new HashSet<String>();
                for (final Date value : values) {
                    final String sdate = LocalDateUtil.fromDate(value).toString();
                    if (!stringVals.contains(sdate)) {
                        array.add(GlobalDatas.getDate(value));
                        stringVals.add(sdate);
                    }
                }
                currentObject = array;
                break;
            }
            default:
                throw new InvalidCreateOperationException(
                    "Query " + inQuery + " is not an In Query");
        }
        currentQUERY = inQuery;
        setReady(true);
    }

    /**
     * In Query constructor
     *
     * @param inQuery in, nin
     * @param variableName
     * @param values
     * @throws InvalidCreateOperationException
     */
    public InQuery(final QUERY inQuery, final String variableName, final long... values)
        throws InvalidCreateOperationException {
        super();
        if (inQuery != QUERY.IN && inQuery != QUERY.NIN) {
            throw new InvalidCreateOperationException(
                "Query " + inQuery + " is not an In Query");
        }
        if (variableName == null || variableName.trim().isEmpty()) {
            throw new InvalidCreateOperationException(
                "Query " + currentQUERY + " cannot be created with empty variable name");
        }
        try {
            GlobalDatas.sanityParameterCheck(variableName);
        } catch (final InvalidParseOperationException e) {
            throw new InvalidCreateOperationException(e);
        }
        final ObjectNode sub =
            ((ObjectNode) currentObject).putObject(inQuery.exactToken());
        final ArrayNode array = sub.putArray(variableName.trim());
        longVals = new HashSet<Long>();
        for (final long value : values) {
            if (!longVals.contains(value)) {
                array.add(value);
                longVals.add(value);
            }
        }
        currentObject = array;
        currentQUERY = inQuery;
        setReady(true);
    }

    /**
     * In Query constructor
     *
     * @param inQuery in, nin
     * @param variableName
     * @param values
     * @throws InvalidCreateOperationException
     */
    public InQuery(final QUERY inQuery, final String variableName, final double... values)
        throws InvalidCreateOperationException {
        super();
        if (inQuery != QUERY.IN && inQuery != QUERY.NIN) {
            throw new InvalidCreateOperationException(
                "Query " + inQuery + " is not an In Query");
        }
        if (variableName == null || variableName.trim().isEmpty()) {
            throw new InvalidCreateOperationException(
                "Query " + currentQUERY + " cannot be created with empty variable name");
        }
        try {
            GlobalDatas.sanityParameterCheck(variableName);
        } catch (final InvalidParseOperationException e) {
            throw new InvalidCreateOperationException(e);
        }
        final ObjectNode sub =
            ((ObjectNode) currentObject).putObject(inQuery.exactToken());
        final ArrayNode array = sub.putArray(variableName.trim());
        doubleVals = new HashSet<Double>();
        for (final double value : values) {
            if (!doubleVals.contains(value)) {
                array.add(value);
                doubleVals.add(value);
            }
        }
        currentObject = array;
        currentQUERY = inQuery;
        setReady(true);
    }

    /**
     * In Query constructor
     *
     * @param inQuery in, nin
     * @param variableName
     * @param values
     * @throws InvalidCreateOperationException
     */
    public InQuery(final QUERY inQuery, final String variableName,
        final boolean... values)
        throws InvalidCreateOperationException {
        super();
        if (inQuery != QUERY.IN && inQuery != QUERY.NIN) {
            throw new InvalidCreateOperationException(
                "Query " + inQuery + " is not an In Query");
        }
        if (variableName == null || variableName.trim().isEmpty()) {
            throw new InvalidCreateOperationException(
                "Query " + currentQUERY + " cannot be created with empty variable name");
        }
        try {
            GlobalDatas.sanityParameterCheck(variableName);
        } catch (final InvalidParseOperationException e) {
            throw new InvalidCreateOperationException(e);
        }
        final ObjectNode sub =
            ((ObjectNode) currentObject).putObject(inQuery.exactToken());
        final ArrayNode array = sub.putArray(variableName.trim());
        booleanVals = new HashSet<Boolean>();
        for (final boolean value : values) {
            if (!booleanVals.contains(value)) {
                array.add(value);
                booleanVals.add(value);
            }
        }
        currentObject = array;
        currentQUERY = inQuery;
        setReady(true);
    }

    /**
     * Add an In Value to an existing In Query
     *
     * @param inValue
     * @return the InQuery
     * @throws InvalidCreateOperationException
     */
    public final InQuery add(final String... inValue)
        throws InvalidCreateOperationException {
        if (currentQUERY != QUERY.IN && currentQUERY != QUERY.NIN) {
            throw new InvalidCreateOperationException(
                "Cannot add an InValue since this is not an In Query: " + currentQUERY);
        }
        final ArrayNode array = (ArrayNode) currentObject;
        if (stringVals == null) {
            stringVals = new HashSet<String>();
        }
        for (final String val : inValue) {
            try {
                GlobalDatas.sanityValueCheck(val);
            } catch (final InvalidParseOperationException e) {
                throw new InvalidCreateOperationException(e);
            }
            if (!stringVals.contains(val)) {
                array.add(val);
                stringVals.add(val);
            }
        }
        return this;
    }

    /**
     * Add an In Value to an existing In Query
     *
     * @param inValue
     * @return the InQuery
     * @throws InvalidCreateOperationException
     */
    public final InQuery add(final Date... inValue)
        throws InvalidCreateOperationException {
        if (currentQUERY != QUERY.IN && currentQUERY != QUERY.NIN) {
            throw new InvalidCreateOperationException(
                "Cannot add an InValue since this is not an In Query: " + currentQUERY);
        }
        final ArrayNode array = (ArrayNode) currentObject;
        if (stringVals == null) {
            stringVals = new HashSet<String>();
        }
        for (final Date val : inValue) {
            final String sdate = LocalDateUtil.fromDate(val).toString();
            if (!stringVals.contains(sdate)) {
                array.add(GlobalDatas.getDate(val));
                stringVals.add(sdate);
            }
        }
        return this;
    }

    /**
     * Add an In Value to an existing In Query
     *
     * @param inValue
     * @return the InQuery
     * @throws InvalidCreateOperationException
     */
    public final InQuery add(final long... inValue)
        throws InvalidCreateOperationException {
        if (currentQUERY != QUERY.IN && currentQUERY != QUERY.NIN) {
            throw new InvalidCreateOperationException(
                "Cannot add an InValue since this is not an In Query: " + currentQUERY);
        }
        final ArrayNode array = (ArrayNode) currentObject;
        if (longVals == null) {
            longVals = new HashSet<Long>();
        }
        for (final long l : inValue) {
            if (!longVals.contains(l)) {
                array.add(l);
                longVals.add(l);
            }
        }
        return this;
    }

    /**
     * Add an In Value to an existing In Query
     *
     * @param inValue
     * @return the InQuery
     * @throws InvalidCreateOperationException
     */
    public final InQuery add(final double... inValue)
        throws InvalidCreateOperationException {
        if (currentQUERY != QUERY.IN && currentQUERY != QUERY.NIN) {
            throw new InvalidCreateOperationException(
                "Cannot add an InValue since this is not an In Query: " + currentQUERY);
        }
        final ArrayNode array = (ArrayNode) currentObject;
        if (doubleVals == null) {
            doubleVals = new HashSet<Double>();
        }
        for (final double d : inValue) {
            if (!doubleVals.contains(d)) {
                array.add(d);
                doubleVals.add(d);
            }
        }
        return this;
    }

    /**
     * Add an In Value to an existing In Query
     *
     * @param inValue
     * @return the InQuery
     * @throws InvalidCreateOperationException
     */
    public final InQuery add(final boolean... inValue)
        throws InvalidCreateOperationException {
        if (currentQUERY != QUERY.IN && currentQUERY != QUERY.NIN) {
            throw new InvalidCreateOperationException(
                "Cannot add an InValue since this is not an In Query: " + currentQUERY);
        }
        final ArrayNode array = (ArrayNode) currentObject;
        if (booleanVals == null) {
            booleanVals = new HashSet<Boolean>();
        }
        for (final boolean b : inValue) {
            if (!booleanVals.contains(b)) {
                array.add(b);
                booleanVals.add(b);
            }
        }
        return this;
    }
}
