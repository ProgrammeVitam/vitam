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
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.GLOBAL;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTION;
import fr.gouv.vitam.common.database.builder.request.configuration.GlobalDatas;
import fr.gouv.vitam.common.database.builder.request.single.RequestSingle;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.parser.request.adapter.VarNameAdapter;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;

import java.util.Iterator;

/**
 * Select Parser: { $query : query, $filter : filter, $projection : projection }
 */
public class SelectParserSingle extends RequestParserSingle {

    /**
     * Empty constructor
     */
    public SelectParserSingle() {
        super();
    }

    /**
     * @param adapter VarNameAdapter
     */
    public SelectParserSingle(VarNameAdapter adapter) {
        super(adapter);
    }

    @Override
    protected RequestSingle getNewRequest() {
        return new Select();
    }

    /**
     * @param request containing a parsed JSON as { $query : query, $filter : filter, $projection : projection }
     * @throws InvalidParseOperationException if request could not parse to JSON
     */
    @Override
    public void parse(final JsonNode request) throws InvalidParseOperationException {
        parseJson(request);
        internalParseSelect();
    }

    /**
     * @throws InvalidParseOperationException if rootNode could parse to projection
     */
    private void internalParseSelect() throws InvalidParseOperationException {
        // { $query : query, $filter : filter, $projection : projection }
        projectionParse(rootNode.get(GLOBAL.PROJECTION.exactToken()));
    }

    /**
     * @param query containing only the JSON request part (no filter neither projection nor roots)
     * @throws InvalidParseOperationException if query could not parse to projection
     */
    @Override
    public void parseQueryOnly(final String query) throws InvalidParseOperationException {
        super.parseQueryOnly(query);
        projectionParse(JsonHandler.createObjectNode());
    }

    /**
     * $fields : {name1 : 0/1, name2 : 0/1, ...}, $usage : contractId
     *
     * @param rootNode JsonNode
     * @throws InvalidParseOperationException if rootNode could not parse to projection or check sanity to rootNode is
     * in error
     */
    public void projectionParse(final JsonNode rootNode) throws InvalidParseOperationException {
        if (rootNode == null) {
            return;
        }
        GlobalDatas.sanityParametersCheck(rootNode.toString(), GlobalDatas.NB_PROJECTIONS);
        try {
            ((Select) request).resetUsedProjection();
            final ObjectNode node = JsonHandler.createObjectNode();
            if (rootNode.has(PROJECTION.FIELDS.exactToken())) {
                adapter.setVarsValue(node, rootNode.path(PROJECTION.FIELDS.exactToken()));
                ((ObjectNode) rootNode).set(PROJECTION.FIELDS.exactToken(), node);
            }
            ((Select) request).setProjection(rootNode);
        } catch (final Exception e) {
            throw new InvalidParseOperationException("Parse in error for Projection: " + rootNode, e);
        }
    }

    /**
     * Add the new Projection slice to the current Projection. If the existing projection is empty, the allFields is
     * added first.
     *
     * @param slice the projection to add
     * @param allFields the default fields to add if none exists yet
     * @throws InvalidParseOperationException if slice or allFields is null or check sanity to them is in error
     */
    public void addProjection(final ObjectNode slice, final ObjectNode allFields)
        throws InvalidParseOperationException {
        if (slice == null || allFields == null) {
            throw new InvalidParseOperationException("addProjection does not accept null parameters");
        }
        GlobalDatas.sanityParametersCheck(slice.toString(), GlobalDatas.NB_PROJECTIONS);
        try {
            ObjectNode node = (ObjectNode) ((Select) request).getProjection().get(PROJECTION.FIELDS.exactToken());
            if (node == null) {
                node = ((Select) request).getProjection().putObject(PROJECTION.FIELDS.exactToken());
                // Add all fields
                node.setAll(allFields);
            } else {
                final Iterator<String> names = slice.fieldNames();
                if (names.hasNext() && node.has(names.next())) {
                    // do not change anything
                    return;
                }
            }
            node.setAll(slice);
        } catch (final Exception e) {
            throw new InvalidParseOperationException("Parse in error for Projection: " + slice, e);
        }
    }

    @Override
    public Select getRequest() {
        return (Select) request;
    }
}
