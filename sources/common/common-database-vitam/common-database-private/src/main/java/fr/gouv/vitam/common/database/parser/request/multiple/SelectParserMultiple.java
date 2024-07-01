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
package fr.gouv.vitam.common.database.parser.request.multiple;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.database.builder.facet.Facet;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.FACET;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.FACETARGS;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.GLOBAL;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTION;
import fr.gouv.vitam.common.database.builder.request.configuration.GlobalDatas;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.RequestMultiple;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.parser.facet.FacetParserHelper;
import fr.gouv.vitam.common.database.parser.request.adapter.VarNameAdapter;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;

/**
 * Select Parser: { $roots: roots, $query : query, $filter : filter, $projection : projection }
 */
public class SelectParserMultiple extends RequestParserMultiple {

    /**
     * Empty constructor
     */
    public SelectParserMultiple() {
        super();
    }

    /**
     * @param adapter VarNameAdapter
     */
    public SelectParserMultiple(VarNameAdapter adapter) {
        super(adapter);
    }

    @Override
    protected RequestMultiple getNewRequest() {
        return new SelectMultiQuery();
    }

    /**
     * @param request containing a parsed JSON as { $roots: root, $query : query, $filter : filter, $projection :
     * projection }
     * @throws InvalidParseOperationException if request could not parse to JSON
     */
    @Override
    public void parse(final JsonNode request) throws InvalidParseOperationException {
        parseJson(request);
        internalParseSelect();
    }

    /**
     * @throws InvalidParseOperationException
     */
    private void internalParseSelect() throws InvalidParseOperationException {
        // not as array but composite as { $roots: root, $query : query,
        // $filter : filter, $projection : projection }
        projectionParse(rootNode.get(GLOBAL.PROJECTION.exactToken()));
        facetsParse(rootNode.get(GLOBAL.FACETS.exactToken()));
        thresholdParse(rootNode.get(GLOBAL.THRESOLD.exactToken()));
        parseTrackTotalHits(rootNode);
    }

    /**
     * @param query containing only the JSON request part (no filter neither projection nor roots)
     * @throws InvalidParseOperationException if request could not parse to JSON
     */
    @Override
    public void parseQueryOnly(final String query) throws InvalidParseOperationException {
        super.parseQueryOnly(query);
        projectionParse(JsonHandler.createObjectNode());
    }

    /**
     * Parse facets
     *
     * @param rootNode JsonNode
     * @throws InvalidParseOperationException if rootNode could not parse to JSON
     */
    protected void facetsParse(final JsonNode rootNode) throws InvalidParseOperationException {
        if (rootNode == null) {
            return;
        }
        if (!rootNode.isArray()) {
            throw new InvalidParseOperationException("Parse in error for Field: should be an array");
        }
        GlobalDatas.sanityParametersCheck(rootNode.toString(), GlobalDatas.NB_FACETS);
        try {
            ((SelectMultiQuery) request).resetFacets();
            for (final JsonNode facet : rootNode) {
                if (!facet.has(FACETARGS.NAME.exactToken())) {
                    throw new InvalidParseOperationException("Invalid parse: name is mandatory");
                }
                FACET facetCommand = getFacetCommand(facet);
                ((SelectMultiQuery) request).addFacets(analyzeOneFacet(facet, facetCommand));
            }
        } catch (final Exception e) {
            throw new InvalidParseOperationException("Parse in error for Field: " + rootNode, e);
        }
    }

    /**
     * Get the facet command
     *
     * @param facet facet
     * @return FACET command
     * @throws InvalidParseOperationException when valid command could not be found
     */
    public static final FACET getFacetCommand(final JsonNode facet) throws InvalidParseOperationException {
        for (FACET facetCommand : FACET.values()) {
            if (facet.has(facetCommand.exactToken())) {
                return facetCommand;
            }
        }
        throw new InvalidParseOperationException("Invalid parse : facet command not found");
    }

    /**
     * Generate a Facet from a Json + command
     *
     * @param facet facet as json
     * @param facetCommand facet command
     * @return Facet
     * @throws InvalidCreateOperationException parsing error
     * @throws InvalidParseOperationException invalid command type
     */
    protected Facet analyzeOneFacet(final JsonNode facet, FACET facetCommand)
        throws InvalidCreateOperationException, InvalidParseOperationException {
        switch (facetCommand) {
            case TERMS:
                return FacetParserHelper.terms(facet, adapter);
            case DATE_RANGE:
                return FacetParserHelper.dateRange(facet, adapter);
            case FILTERS:
                return FacetParserHelper.filters(facet, adapter);
            default:
                throw new InvalidParseOperationException(
                    "Invalid parse: command not a facet " + facetCommand.exactToken()
                );
        }
    }

    /**
     * $fields : {name1 : 0/1, name2 : 0/1, ...}, $usage : contractId
     *
     * @param rootNode JsonNode
     * @throws InvalidParseOperationException if rootNode could not parse to JSON
     */
    protected void projectionParse(final JsonNode rootNode) throws InvalidParseOperationException {
        if (rootNode == null) {
            return;
        }
        GlobalDatas.sanityParametersCheck(rootNode.toString(), GlobalDatas.NB_PROJECTIONS);
        try {
            ((SelectMultiQuery) request).resetUsageProjection().resetUsedProjection();
            final ObjectNode node = JsonHandler.createObjectNode();
            if (rootNode.has(PROJECTION.FIELDS.exactToken())) {
                adapter.setVarsValue(node, rootNode.path(PROJECTION.FIELDS.exactToken()));
                ((ObjectNode) rootNode).set(PROJECTION.FIELDS.exactToken(), node);
            }
            ((SelectMultiQuery) request).setProjection(rootNode);
        } catch (final Exception e) {
            throw new InvalidParseOperationException("Parse in error for Projection: " + rootNode, e);
        }
    }

    /**
     * {$"threshold" : arg}
     *
     * @param rootNode JsonNode
     * @throws InvalidParseOperationException if rootNode could not parse to JSON
     */
    protected void thresholdParse(final JsonNode rootNode) throws InvalidParseOperationException {
        if (rootNode == null) {
            return;
        }

        try {
            ((SelectMultiQuery) request).setThreshold(rootNode.asLong());
        } catch (final Exception e) {
            throw new InvalidParseOperationException("Parse in error for Action: " + rootNode, e);
        }
    }

    protected void parseTrackTotalHits(JsonNode rootNode) throws InvalidParseOperationException {
        if (!rootNode.has(GLOBAL.FILTER.exactToken())) {
            return;
        }

        JsonNode filterNode = rootNode.get(GLOBAL.FILTER.exactToken());
        if (!filterNode.has(BuilderToken.SELECTFILTER.TRACK_TOTAL_HITS.exactToken())) {
            return;
        }

        JsonNode trackTotalHits = filterNode.get(BuilderToken.SELECTFILTER.TRACK_TOTAL_HITS.exactToken());

        if (!trackTotalHits.isBoolean()) {
            throw new InvalidParseOperationException("Invalid trackTotalHits node format");
        }

        ((SelectMultiQuery) request).trackTotalHits(trackTotalHits.asBoolean());
    }

    @Override
    public SelectMultiQuery getRequest() {
        return (SelectMultiQuery) request;
    }
}
