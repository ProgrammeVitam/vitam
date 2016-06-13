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
package fr.gouv.vitam.builder.request.construct.configuration;

import java.util.Date;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.gouv.vitam.builder.request.construct.query.Query;
import fr.gouv.vitam.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;

/**
 * Main configuration for Query support
 *
 *
 */
public class GlobalDatas {

    /**
     * Default limit for loading result
     */
    public static final int limitLoad = 10000;
    /**
     * Default limit for Value (sanity check)
     */
    private static int limitValue = 10000000;
    /**
     * Default limit for small parameter (sanity check)
     */
    private static int limitParameter = 1000;
    /**
     * Default limit for number of roots
     */
    public static final int nbRoots = 1000;
    /**
     * Default limit for number of filters
     */
    public static final int nbFilters = 10;
    /**
     * Default limit for number of projections
     */
    public static final int nbProjections = 1000;
    /**
     * True means commands are to be written using '$' as prefix
     */
    public static final boolean COMMAND_DOLLAR = true;

    /**
     * Check the String if conforms to sanity check
     *
     * @param arg
     * @throws InvalidCreateOperationException if the sanity check is in error
     */
    protected static final void sanityCheck(String arg, int size)
        throws InvalidParseOperationException {
        // TODO REVIEW should check null
        if (arg != null && arg.length() > size) {
            throw new InvalidParseOperationException(
                "String exceeds sanity check of " + size);
        }
    }

    /**
     * Check the String if conforms to sanity check
     *
     * @param arg
     * @throws InvalidParseOperationException if the sanity check is in error
     */
    public static final void sanityValueCheck(String arg)
        throws InvalidParseOperationException {
        sanityCheck(arg, limitValue);
    }

    /**
     * Check the String if conforms to sanity check for small parameters
     *
     * @param arg
     * @throws InvalidParseOperationException if the sanity check is in error
     */
    public static final void sanityParameterCheck(String arg)
        throws InvalidParseOperationException {
        sanityCheck(arg, limitParameter);
        if (arg.charAt(0) == '_') {
            throw new InvalidParseOperationException(
                "Variable name form is not allowed: " + arg);
        }
    }

    /**
     * Check the String if conforms to sanity check for small parameters
     *
     * @param arg
     * @param multipleParams how many parameters
     * @throws InvalidParseOperationException if the sanity check is in error
     */
    public static final void sanityParametersCheck(String arg, int multipleParams)
        throws InvalidParseOperationException {
        sanityCheck(arg, limitParameter * multipleParams);
    }

    protected GlobalDatas() {
        // empty
    }

    /**
     * @param date
     * @return the corresponding Date in Json format
     */
    public static final ObjectNode getDate(final Date date) {
        // TODO REVIEW should check null
        return JsonHandler.createObjectNode().put(Query.DATE,
            LocalDateUtil.fromDate(date).toString());
    }

    /**
     *
     * @param value
     * @return the JsonNode for Value
     * @throws InvalidCreateOperationException
     */
    public static final JsonNode getValueJsonNode(final Object value)
        throws InvalidCreateOperationException {
        final ObjectNode node = JsonHandler.createObjectNode();
        if (value == null) {
            return node.nullNode();
        } else if (value instanceof Integer) {
            return node.numberNode((Integer) value);
        } else if (value instanceof Long) {
            return node.numberNode((Long) value);
        } else if (value instanceof Float) {
            return node.numberNode((Float) value);
        } else if (value instanceof Double) {
            return node.numberNode((Double) value);
        } else if (value instanceof Boolean) {
            return node.booleanNode((Boolean) value);
        } else if (value instanceof Date) {
            return getDate((Date) value);
        } else {
            final String val = value.toString();
            try {
                sanityValueCheck(val);
            } catch (final InvalidParseOperationException e) {
                throw new InvalidCreateOperationException(e);
            }
            return node.textNode(val);
        }
    }

    /**
     * Check the Variable name if conforms to sanity check
     * 
     * @param arg
     * @throws InvalidParseOperationException if the sanity check is in error
     */
    public static final void sanityVariableNameCheck(String arg)
        throws InvalidParseOperationException {
        // TODO REVIEW should check null
        if (arg.charAt(0) == '#') {
            throw new InvalidParseOperationException("Variable name cannot be a protected one (starting with '#'");
        }
    }

    /**
     * @return the current LimitValue (sanity check)
     */
    public static int getLimitValue() {
        return limitValue;
    }

    /**
     * @param limitValue the new limit Value to set (sanity check)
     */
    public static void setLimitValue(int limitValue) {
        GlobalDatas.limitValue = limitValue;
    }

    /**
     * @return the current LimitParameter (sanity check)
     */
    public static int getLimitParameter() {
        return limitParameter;
    }

    /**
     * @param limitParameter the new limiteParameter to set (sanity check)
     */
    public static void setLimitParameter(int limitParameter) {
        GlobalDatas.limitParameter = limitParameter;
    }
}
