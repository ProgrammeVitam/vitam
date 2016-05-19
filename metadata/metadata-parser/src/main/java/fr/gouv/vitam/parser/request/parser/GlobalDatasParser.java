/**
 * 
 */
package fr.gouv.vitam.parser.request.parser;

import java.util.Iterator;

import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.gouv.vitam.builder.request.construct.configuration.GlobalDatas;
import fr.gouv.vitam.builder.request.construct.query.Query;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;

/**
 * Configuration for Parser
 *
 */
public class GlobalDatasParser extends GlobalDatas {

    /**
     * Default limit for Request (sanity check)
     */
    public static int limitRequest = 100000000;

    /**
     * Default limit for number of projections
     */
    public static final int nbActions = 10000;

    /**
     * Default max depth: should be 20 but let a great margin
     */
    public static final int MAXDEPTH = 100;

    /**
     * Check the Request if conforms to sanity check
     * 
     * @param arg
     * @throws InvalidParseOperationException
     *             if the sanity check is in error
     */
    public static final void sanityRequestCheck(String arg)
            throws InvalidParseOperationException {
        GlobalDatas.sanityCheck(arg, GlobalDatasParser.limitRequest);
    }
    
    /**
     * calculate JsonNode depth or number of child
     * 
     * @param JsonNode
     * @return number of child of JsonNode
     */
    public static final int getJsonNodedepth(JsonNode jsonNode) {
		// FIXME REVIEW should check null
		int depth = 0;
		boolean hasArrayNode = false;
		Iterator<JsonNode> iterator = jsonNode.iterator();
		while (iterator.hasNext()) {
			JsonNode node = iterator.next();
			if (node instanceof ObjectNode || node instanceof ArrayNode) {
				int tempDepth = getJsonNodedepth(node);
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
     * 
     * @param value
     * @return the Object for Value
     * @throws InvalidParseOperationException
     */
    public static final Object getValue(final JsonNode value)
            throws InvalidParseOperationException {
        if (value == null || value.isArray()) {
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
            return DateTime.parse(value.get(Query.DATE).asText()).toDate();
        } else {
            return value.asText();
        }
    }

    protected GlobalDatasParser() {
        // empty
    }
}
