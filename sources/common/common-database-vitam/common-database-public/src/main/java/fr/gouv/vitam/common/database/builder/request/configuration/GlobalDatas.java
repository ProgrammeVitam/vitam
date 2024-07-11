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
package fr.gouv.vitam.common.database.builder.request.configuration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * Main configuration for Query support
 */
public class GlobalDatas {

    /**
     * Default limit for loading result
     */
    public static final int LIMIT_LOAD = 10000;
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
    public static final int NB_ROOTS = 1000;
    /**
     * Default limit for number of filters
     */
    public static final int NB_FILTERS = 10;
    /**
     * Default limit for number of facets
     */
    public static final int NB_FACETS = 1000;
    /**
     * Default limit for number of projections
     */
    public static final int NB_PROJECTIONS = 1000;
    /**
     * Default max depth: should be 30 but let a great margin
     */
    public static final int MAXDEPTH = 100;

    protected GlobalDatas() {
        // empty
    }

    /**
     * Check the String if conforms to sanity check
     *
     * @param arg argument
     * @param size limit
     * @throws InvalidParseOperationException if the sanity check is in error
     */
    protected static final void sanityCheck(String arg, int size) throws InvalidParseOperationException {
        if (arg == null) {
            throw new InvalidParseOperationException("String is null but must not");
        }
        if (arg.length() > size) {
            throw new InvalidParseOperationException("String exceeds sanity check of " + size);
        }
    }

    /**
     * Check the String if conforms to sanity check
     *
     * @param arg argument
     * @throws InvalidParseOperationException if the sanity check is in error
     */
    public static final void sanityValueCheck(String arg) throws InvalidParseOperationException {
        sanityCheck(arg, limitValue);
    }

    /**
     * Check the List of arguments if conforms to sanity check
     *
     * @param arg argument as List
     * @throws InvalidParseOperationException if the sanity check is in error
     */
    public static final void sanityValueCheck(List<?> arg) throws InvalidParseOperationException {
        for (Object value : arg) {
            sanityCheck(value.toString(), limitValue);
        }
    }

    /**
     * Check the String if conforms to sanity check for small parameters
     *
     * @param arg argument
     * @throws InvalidParseOperationException if the sanity check is in error
     */
    public static final void sanityParameterCheck(String arg) throws InvalidParseOperationException {
        sanityCheck(arg, limitParameter);
        if (arg.charAt(0) == '_') {
            throw new InvalidParseOperationException("Variable name form is not allowed: " + arg);
        }
    }

    /**
     * Check the String if conforms to sanity check for small parameters
     *
     * @param arg argument
     * @param multipleParams how many parameters
     * @throws InvalidParseOperationException if the sanity check is in error
     */
    public static final void sanityParametersCheck(String arg, int multipleParams)
        throws InvalidParseOperationException {
        sanityCheck(arg, limitParameter * multipleParams);
    }

    /**
     * @param date param
     * @return the corresponding Date in Json format
     * @throws IllegalArgumentException if date is null
     */
    public static final ObjectNode getDate(final Date date) {
        ParametersChecker.checkParameter("Date cannot be null", date);
        return JsonHandler.createObjectNode().put(Query.DATE, LocalDateUtil.fromDate(date).toString());
    }

    /**
     * @param value of node
     * @return the JsonNode for Value
     * @throws InvalidCreateOperationException when object is not json
     */
    public static final JsonNode getValueJsonNode(final Object value) throws InvalidCreateOperationException {
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
        } else if (value instanceof JsonNode) {
            return (JsonNode) value;
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
     * @param arg argument
     * @throws InvalidParseOperationException if the sanity check is in error
     * @throws IllegalArgumentException if arg is null
     */
    public static final void sanityVariableNameCheck(String arg) throws InvalidParseOperationException {
        ParametersChecker.checkParameter("Arg cannot be null", arg);
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

    /**
     * Helper to set Value from a List into an ArrayNode
     *
     * @param array
     * @param list
     */
    public static void setArrayValueFromList(ArrayNode array, List<?> list) {
        for (Object object : list) {
            if (object instanceof String) {
                array.add((String) object);
            } else if (object instanceof Long) {
                array.add((Long) object);
            } else if (object instanceof Float) {
                array.add((Float) object);
            } else if (object instanceof Double) {
                array.add((Double) object);
            } else if (object instanceof Boolean) {
                array.add((Boolean) object);
            } else if (object instanceof Integer) {
                array.add((Integer) object);
            } else if (object instanceof BigDecimal) {
                array.add((BigDecimal) object);
            } else if (object instanceof Date) {
                array.add(JsonHandler.createObjectNode().put(Query.DATE, ((Date) object).toInstant().toString()));
            } else {
                array.add(object.toString());
            }
        }
    }
}
