/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common.database.parser.request;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.request.configuration.GlobalDatas;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Configuration for Parser
 */
public class GlobalDatasParser extends GlobalDatas {

    private static final String DATE_INVALID = "Date invalid";
    /**
     * Default limit for Request (sanity check)
     */
    public static final int DEFAULT_LIMIT_REQUEST = 100000000;
    /**
     * Default limit for Request (sanity check)
     */
    public static int limitRequest = DEFAULT_LIMIT_REQUEST; // NOSONAR Change can be done

    /**
     * Default limit for number of projections
     */
    public static final int NB_ACTIONS = 10000;

    /**
     * default limit scroll timeout
     */
    public static final int DEFAULT_SCROLL_TIMEOUT = 60000;

    protected GlobalDatasParser() {
        // empty
    }

    /**
     * Check the Request if conforms to sanity check
     *
     * @param arg String
     * @throws InvalidParseOperationException if the sanity check is in error
     */
    public static final void sanityRequestCheck(String arg) throws InvalidParseOperationException {
        GlobalDatas.sanityCheck(arg, GlobalDatasParser.limitRequest);
    }

    /**
     * calculate JsonNode depth
     *
     * @param jsonNode JsonNode
     * @return number of child of JsonNode
     */
    public static final int getJsonNodedepth(JsonNode jsonNode) {
        ParametersChecker.checkParameter("jsonNode is a mandatory parameter", jsonNode);

        int depth = 0;
        boolean hasArrayNode = false;
        final Iterator<JsonNode> iterator = jsonNode.iterator();
        while (iterator.hasNext()) {
            final JsonNode node = iterator.next();
            if (node instanceof ObjectNode || node instanceof ArrayNode) {
                final int tempDepth = getJsonNodedepth(node);
                if (tempDepth > depth) {
                    depth = tempDepth;
                }
            }
            if (node instanceof ArrayNode) {
                hasArrayNode = true;
            }
        }

        if (hasArrayNode) {
            return depth;
        } else {
            return 1 + depth;
        }
    }

    /**
     * @param value JsonNode
     * @return the Object for Value
     * @throws InvalidParseOperationException if value could not parse to JSON
     */
    public static final Object getValue(final JsonNode value) throws InvalidParseOperationException {
        if (value == null) {
            throw new InvalidParseOperationException("Not correctly parsed");
        }
        if (value.isBoolean()) {
            return value.asBoolean();
        } else if (value.isFloat()) {
            return value.floatValue();
        } else if (value.isInt()) {
            return value.asInt();
        } else if (value.isDouble()) {
            return value.asDouble();
        } else if (value.canConvertToLong()) {
            return value.asLong();
        } else if (value.has(Query.DATE)) {
            try {
                return LocalDateUtil.getDate(value.get(Query.DATE).asText());
            } catch (ParseException e) {
                throw new InvalidParseOperationException(DATE_INVALID);
            }
        } else if (value.isArray()) {
            List<Object> list = new ArrayList<>();
            for (JsonNode item : (ArrayNode) value) {
                list.add(getValue(item));
            }
            return list;
        } else {
            return value.asText();
        }
    }

    /**
     * @param value
     * @return the ArrayNode for value
     * @throws InvalidParseOperationException if value could not parse to JSON
     */
    public static final ArrayNode getArray(final JsonNode value) throws InvalidParseOperationException {
        if (value == null) {
            throw new InvalidParseOperationException("Not correctly parsed");
        }
        if (value instanceof ArrayNode) {
            return (ArrayNode) value;
        }
        ArrayNode node = JsonHandler.createArrayNode();
        if (value.isBoolean()) {
            return node.add(value.asBoolean());
        } else if (value.isFloat()) {
            return node.add(value.floatValue());
        } else if (value.isInt()) {
            return node.add(value.asInt());
        } else if (value.isDouble()) {
            return node.add(value.asDouble());
        } else if (value.canConvertToLong()) {
            return node.add(value.asLong());
        } else if (value.has(Query.DATE)) {
            try {
                return node.add(LocalDateUtil.getFormattedDate(LocalDateUtil.getDate(value.get(Query.DATE).asText())));
            } catch (ParseException e) {
                throw new InvalidParseOperationException(DATE_INVALID);
            }
        } else {
            return node.add(value.asText());
        }
    }
}
