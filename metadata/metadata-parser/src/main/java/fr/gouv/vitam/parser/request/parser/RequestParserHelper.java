/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 *******************************************************************************/
/**
 *
 */
package fr.gouv.vitam.parser.request.parser;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.GLOBAL;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

/**
 * RequestParser Helper to get the correct Request Parser from String or from Json.\n Note however that is should be
 * better to create the correct Parser based one the source of the request:\n - POST: insert\n - PATCH: update\n -
 * DELETE: delete\n - GET: select\n - PUT: delete & insert\n
 *
 */
public class RequestParserHelper {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(RequestParserHelper.class);

    private RequestParserHelper() {
        // empty
    }

    /**
     * Create one Parser according to:\n InsertParser if { $roots: root, $query : query, $filter : filter, $data :
     * data}\n UpdateParser if { $roots: root, $query : query, $filter : filter, $action : action }\n SelectParser if {
     * $roots: roots, $query : query, $filter : filter, $projection : projection }\n DeleteParser if { $roots: roots,
     * $query : query, $filter : multi }
     *
     * @param srcRequest the request to parse
     * @return the appropriate RequestParser
     * @throws InvalidParseOperationException
     */
    @Deprecated
    public static RequestParser getParser(String srcRequest) throws InvalidParseOperationException {
        return getParser(srcRequest, new VarNameAdapter());
    }

    /**
     * Create one Parser according to:\n InsertParser if { $roots: root, $query : query, $filter : filter, $data :
     * data}\n UpdateParser if { $roots: root, $query : query, $filter : filter, $action : action }\n SelectParser if {
     * $roots: roots, $query : query, $filter : filter, $projection : projection }\n DeleteParser if { $roots: roots,
     * $query : query, $filter : multi }
     *
     * @param srcRequest the request to parse
     * @param varNameAdapter the VarNameAdapter to use with the created Parser
     * @return the appropriate RequestParser
     * @throws InvalidParseOperationException
     */
    @Deprecated
    public static RequestParser getParser(String srcRequest, VarNameAdapter varNameAdapter)
        throws InvalidParseOperationException {
        final JsonNode request = JsonHandler.getFromString(srcRequest);
        return getParser(request, varNameAdapter);
    }

    /**
     * Create one Parser according to:\n InsertParser if { $roots: root, $query : query, $filter : filter, $data :
     * data}\n UpdateParser if { $roots: root, $query : query, $filter : filter, $action : action }\n SelectParser if {
     * $roots: roots, $query : query, $filter : filter, $projection : projection }\n DeleteParser if { $roots: roots,
     * $query : query, $filter : multi }
     *
     * @param jsonRequest the request to parse
     * @return the appropriate RequestParser
     * @throws InvalidParseOperationException
     */
    public static RequestParser getParser(JsonNode jsonRequest) throws InvalidParseOperationException {
        return getParser(jsonRequest, new VarNameAdapter());
    }

    /**
     * Create one Parser according to:\n InsertParser if { $roots: root, $query : query, $filter : filter, $data :
     * data}\n UpdateParser if { $roots: root, $query : query, $filter : filter, $action : action }\n SelectParser if {
     * $roots: roots, $query : query, $filter : filter, $projection : projection }\n DeleteParser if { $roots: roots,
     * $query : query, $filter : multi }\n
     *
     * @param jsonRequest the request to parse
     * @param varNameAdapter the VarNameAdapter to use with the created Parser
     * @return the appropriate RequestParser
     * @throws InvalidParseOperationException
     */
    public static RequestParser getParser(JsonNode jsonRequest, VarNameAdapter varNameAdapter)
        throws InvalidParseOperationException {
        if (jsonRequest.get(GLOBAL.PROJECTION.exactToken()) != null) {
            LOGGER.debug("SELECT");
            final SelectParser selectParser = new SelectParser(varNameAdapter);
            selectParser.parse(jsonRequest);
            return selectParser;
        } else if (jsonRequest.get(GLOBAL.DATA.exactToken()) != null) {
            LOGGER.debug("INSERT");
            final InsertParser insertParser = new InsertParser(varNameAdapter);
            insertParser.parse(jsonRequest);
            return insertParser;
        } else if (jsonRequest.get(GLOBAL.ACTION.exactToken()) != null) {
            LOGGER.debug("UPDATE");
            final UpdateParser updateParser = new UpdateParser(varNameAdapter);
            updateParser.parse(jsonRequest);
            return updateParser;
        } else {
            LOGGER.debug("DELETE");
            final DeleteParser deleteParser = new DeleteParser(varNameAdapter);
            deleteParser.parse(jsonRequest);
            return deleteParser;
        }
    }
}
