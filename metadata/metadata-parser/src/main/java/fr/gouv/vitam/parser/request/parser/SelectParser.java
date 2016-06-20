/*******************************************************************************
 * This file is part of Vitam Project.
 *
 * Copyright Vitam (2012, 2016)
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
package fr.gouv.vitam.parser.request.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.gouv.vitam.builder.request.construct.Request;
import fr.gouv.vitam.builder.request.construct.Select;
import fr.gouv.vitam.builder.request.construct.configuration.GlobalDatas;
import fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.GLOBAL;
import fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.PROJECTION;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;

/**
 * Select Parser: { $roots: roots, $query : query, $filter : filter, $projection : projection } or [ roots, query,
 * filter, projection ]
 *
 */
public class SelectParser extends RequestParser {
    protected static final int PROJECTION_POS = 3;

    /**
     * Empty constructor
     */
    public SelectParser() {
        super();
    }

    /**
     * @param adapter
     *
     */
    public SelectParser(VarNameAdapter adapter) {
        super(adapter);
    }

    @Override
    protected Request getNewRequest() {
        return new Select();
    }

    /**
     *
     * @param request containing a parsed JSON as [ {root}, {query}, {filter}, {projection} ] or { $roots: root, $query
     *        : query, $filter : filter, $projection : projection }
     * @throws InvalidParseOperationException
     */
    @Override
    public void parse(final JsonNode request) throws InvalidParseOperationException {
        parseJson(request);
        internalParseSelect();
    }

    /**
     *
     * @param request containing a JSON as [ {root}, {query}, {filter}, {projection} ] or { $roots: root, $query :
     *        query, $filter : filter, $projection : projection }
     * @throws InvalidParseOperationException
     */
    @Override
    @Deprecated
    public void parse(final String request) throws InvalidParseOperationException {
        parseString(request);
        internalParseSelect();
    }

    /**
     * @throws InvalidParseOperationException
     */
    private void internalParseSelect() throws InvalidParseOperationException {
        if (rootNode.isArray()) {
            // should be 4, but each could be empty ( '{}' )
            if (rootNode.size() > PROJECTION_POS) {
                projectionParse(rootNode.get(PROJECTION_POS));
            }
        } else {
            // not as array but composite as { $roots: root, $query : query,
            // $filter : filter, $projection : projection }
            projectionParse(rootNode.get(GLOBAL.PROJECTION.exactToken()));
        }
    }

    /**
     *
     * @param query containing only the JSON request part (no filter neither projection nor roots)
     * @throws InvalidParseOperationException
     */
    @Override
    public void parseQueryOnly(final String query) throws InvalidParseOperationException {
        super.parseQueryOnly(query);
        projectionParse(JsonHandler.createObjectNode());
    }

    /**
     * $fields : {name1 : 0/1, name2 : 0/1, ...}, $usage : contractId
     *
     * @param rootNode
     * @throws InvalidParseOperationException
     */
    protected void projectionParse(final JsonNode rootNode)
        throws InvalidParseOperationException {
        if (rootNode == null) {
            return;
        }
        GlobalDatas.sanityParametersCheck(rootNode.toString(), GlobalDatas.NB_PROJECTIONS);
        try {
            ((Select) request).resetUsageProjection().resetUsedProjection();
            final ObjectNode node = JsonHandler.createObjectNode();
            if (rootNode.has(PROJECTION.FIELDS.exactToken())) {
                adapter.setVarsValue(node, rootNode.path(PROJECTION.FIELDS.exactToken()));
                ((ObjectNode) rootNode).set(PROJECTION.FIELDS.exactToken(), node);
            }
            ((Select) request).setProjection(rootNode);
        } catch (final Exception e) {
            throw new InvalidParseOperationException(
                "Parse in error for Projection: " + rootNode, e);
        }
    }

    @Override
    public Select getRequest() {
        return (Select) request;
    }
}
