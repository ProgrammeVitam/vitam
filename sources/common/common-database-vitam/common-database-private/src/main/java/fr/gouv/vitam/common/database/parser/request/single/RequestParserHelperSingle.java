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
package fr.gouv.vitam.common.database.parser.request.single;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.GLOBAL;
import fr.gouv.vitam.common.database.parser.request.adapter.SingleVarNameAdapterExternal;
import fr.gouv.vitam.common.database.parser.request.adapter.VarNameAdapter;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

/**
 * RequestParser Helper to get the correct Request Parser from String or from Json. <br>
 * Note however that is should be better to create the correct Parser based one the source of the request: <br>
 * - POST: insert <br>
 * - PATCH: update <br>
 * - DELETE: delete <br>
 * - GET: select <br>
 * - PUT: delete and insert
 */
public class RequestParserHelperSingle {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(RequestParserHelperSingle.class);

    private RequestParserHelperSingle() {
        // empty
    }

    /**
     * Create one Parser for Internal API according to: <br>
     * InsertParser if { $query : query, $filter : filter, $data : data} <br>
     * UpdateParser if { $query : query, $filter : filter, $action : action } <br>
     * SelectParser if { $query : query, $filter : filter, $projection : projection } <br>
     * DeleteParser if { $query : query, $filter : multi }
     *
     * @param jsonRequest the request to parse
     * @return the appropriate RequestParser
     * @throws InvalidParseOperationException if jsonRequest could not parse to JSON
     */
    public static RequestParserSingle getParser(JsonNode jsonRequest) throws InvalidParseOperationException {
        return getParser(jsonRequest, new SingleVarNameAdapterExternal());
    }

    /**
     * Create one Parser for Masterdata according to: <br>
     * InsertParser if { $query : query, $filter : filter, $data : data} <br>
     * UpdateParser if { $query : query, $filter : filter, $action : action } <br>
     * SelectParser if { $query : query, $filter : filter, $projection : projection } <br>
     * DeleteParser if { $query : query, $filter : multi } <br>
     *
     * @param jsonRequest the request to parse
     * @param varNameAdapter the VarNameAdapter to use with the created Parser
     * @return the appropriate RequestParser
     * @throws InvalidParseOperationException if jsonRequest could not parse to JSON
     */
    public static RequestParserSingle getParser(JsonNode jsonRequest, VarNameAdapter varNameAdapter)
        throws InvalidParseOperationException {
        if (jsonRequest.get(GLOBAL.PROJECTION.exactToken()) != null) {
            LOGGER.debug("SELECT");
            final SelectParserSingle selectParser = new SelectParserSingle(varNameAdapter);
            selectParser.parse(jsonRequest);
            return selectParser;
        } else if (jsonRequest.get(GLOBAL.DATA.exactToken()) != null) {
            LOGGER.debug("INSERT");
            final InsertParserSingle insertParser = new InsertParserSingle(varNameAdapter);
            insertParser.parse(jsonRequest);
            return insertParser;
        } else if (jsonRequest.get(GLOBAL.ACTION.exactToken()) != null) {
            LOGGER.debug("UPDATE");
            final UpdateParserSingle updateParser = new UpdateParserSingle(varNameAdapter);
            updateParser.parse(jsonRequest);
            return updateParser;
        } else {
            LOGGER.debug("DELETE");
            final DeleteParserSingle deleteParser = new DeleteParserSingle(varNameAdapter);
            deleteParser.parse(jsonRequest);
            return deleteParser;
        }
    }
}
