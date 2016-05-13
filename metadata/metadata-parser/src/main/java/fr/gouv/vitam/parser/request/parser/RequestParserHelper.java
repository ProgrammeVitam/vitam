/**
 * 
 */
package fr.gouv.vitam.parser.request.parser;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.GLOBAL;

/**
 * RequestParser Helper to get the correct Request Parser from String or from Json.\n
 * Note however that is should be better to create the correct Parser based one the source of the request:\n
 * - POST: insert\n
 * - PATCH: update\n
 * - DELETE: delete\n
 * - GET: select\n
 * - PUT: delete & insert\n
 *
 */
public class RequestParserHelper {
	private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(RequestParserHelper.class);

	private RequestParserHelper() {
		// empty
	}
	
	/**
	 * Create one Parser according to:\n
	 * InsertParser if { $roots: root, $query : query, $filter : filter, $data : data}\n
	 * UpdateParser if { $roots: root, $query : query, $filter : filter, $action : action }\n
	 * SelectParser if { $roots: roots, $query : query, $filter : filter, $projection : projection }\n
	 * DeleteParser if { $roots: roots, $query : query, $filter : multi }
	 * 
	 * @param jsonRequest the request to parse
	 * @return the appropriate RequestParser
	 * @throws InvalidParseOperationException
	 */
	public static RequestParser getParser(String srcRequest) throws InvalidParseOperationException {
		return getParser(srcRequest, new VarNameAdapter());
	}
	
	/**
	 * Create one Parser according to:\n
	 * InsertParser if { $roots: root, $query : query, $filter : filter, $data : data}\n
	 * UpdateParser if { $roots: root, $query : query, $filter : filter, $action : action }\n
	 * SelectParser if { $roots: roots, $query : query, $filter : filter, $projection : projection }\n
	 * DeleteParser if { $roots: roots, $query : query, $filter : multi }
	 * 
	 * @param jsonRequest the request to parse
	 * @param varNameAdapter the VarNameAdapter to use with the created Parser
	 * @return the appropriate RequestParser
	 * @throws InvalidParseOperationException
	 */
	public static RequestParser getParser(String srcRequest, VarNameAdapter varNameAdapter) throws InvalidParseOperationException {
		JsonNode request = JsonHandler.getFromString(srcRequest);
		return getParser(request, varNameAdapter);
	}
	
	/**
	 * Create one Parser according to:\n
	 * InsertParser if { $roots: root, $query : query, $filter : filter, $data : data}\n
	 * UpdateParser if { $roots: root, $query : query, $filter : filter, $action : action }\n
	 * SelectParser if { $roots: roots, $query : query, $filter : filter, $projection : projection }\n
	 * DeleteParser if { $roots: roots, $query : query, $filter : multi }
	 * 
	 * @param jsonRequest the request to parse
	 * @return the appropriate RequestParser
	 * @throws InvalidParseOperationException
	 */
	public static RequestParser getParser(JsonNode jsonRequest) throws InvalidParseOperationException {
		return getParser(jsonRequest, new VarNameAdapter());
	}
	/**
	 * Create one Parser according to:\n
	 * InsertParser if { $roots: root, $query : query, $filter : filter, $data : data}\n
	 * UpdateParser if { $roots: root, $query : query, $filter : filter, $action : action }\n
	 * SelectParser if { $roots: roots, $query : query, $filter : filter, $projection : projection }\n
	 * DeleteParser if { $roots: roots, $query : query, $filter : multi }\n
	 * 
	 * @param jsonRequest the request to parse
	 * @param varNameAdapter the VarNameAdapter to use with the created Parser
	 * @return the appropriate RequestParser
	 * @throws InvalidParseOperationException 
	 */
	public static RequestParser getParser(JsonNode jsonRequest, VarNameAdapter varNameAdapter) throws InvalidParseOperationException {
		if (jsonRequest.get(GLOBAL.PROJECTION.exactToken()) != null) {
			LOGGER.debug("SELECT");
			SelectParser selectParser = new SelectParser(varNameAdapter);
			selectParser.parse(jsonRequest);
			return selectParser;
		} else if (jsonRequest.get(GLOBAL.DATA.exactToken()) != null) {
			LOGGER.debug("INSERT");
			InsertParser insertParser = new InsertParser(varNameAdapter);
			insertParser.parse(jsonRequest);
			return insertParser;
		} else if (jsonRequest.get(GLOBAL.ACTION.exactToken()) != null) {
			LOGGER.debug("UPDATE");
			UpdateParser updateParser = new UpdateParser(varNameAdapter);
			updateParser.parse(jsonRequest);
			return updateParser;
		} else {
			LOGGER.debug("DELETE");
			DeleteParser deleteParser = new DeleteParser(varNameAdapter);
			deleteParser.parse(jsonRequest);
			return deleteParser;
		}
	}
}
