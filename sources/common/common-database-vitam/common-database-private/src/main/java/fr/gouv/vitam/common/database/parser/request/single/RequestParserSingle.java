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
import com.fasterxml.jackson.databind.node.ArrayNode;
import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.FILTERARGS;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.GLOBAL;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.QUERY;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.SELECTFILTER;
import fr.gouv.vitam.common.database.builder.request.configuration.GlobalDatas;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Delete;
import fr.gouv.vitam.common.database.builder.request.single.Insert;
import fr.gouv.vitam.common.database.builder.request.single.RequestSingle;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.builder.request.single.Update;
import fr.gouv.vitam.common.database.parser.query.ParserTokens;
import fr.gouv.vitam.common.database.parser.request.AbstractParser;
import fr.gouv.vitam.common.database.parser.request.GlobalDatasParser;
import fr.gouv.vitam.common.database.parser.request.adapter.SingleVarNameAdapterExternal;
import fr.gouv.vitam.common.database.parser.request.adapter.VarNameAdapter;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;

import java.util.Map.Entry;

import static fr.gouv.vitam.common.database.parser.query.QueryParserHelper.nop;
import static fr.gouv.vitam.common.database.parser.query.QueryParserHelper.path;

/**
 * Single Request Parser (common base) { $query : query, $filter : filter }
 */
public abstract class RequestParserSingle extends AbstractParser<RequestSingle> {

    /**
     * Constructor for Internal API
     */
    public RequestParserSingle() {
        request = getNewRequest();
        adapter = new SingleVarNameAdapterExternal();
    }

    /**
     * Constructor for Masterdata
     *
     * @param adapter VarNameAdapter
     */
    public RequestParserSingle(VarNameAdapter adapter) {
        request = getNewRequest();
        this.adapter = adapter;
    }

    private void internalParse() throws InvalidParseOperationException {
        GlobalDatasParser.sanityRequestCheck(sourceRequest);
        if (request != null) {
            request.reset();
        } else {
            request = getNewRequest();
        }
        hasFullTextQuery = false;
        if (rootNode == null || rootNode.isMissingNode()) {
            throw new InvalidParseOperationException("The current Node is missing(empty): RequestRoot");
        }

        /*
         * { $query : query, $filter : filter }
         */
        queryParse(rootNode.get(GLOBAL.QUERY.exactToken()));
        filterParse(rootNode.get(GLOBAL.FILTER.exactToken()));
    }

    /**
     * @param jsonRequest containing a parsed JSON as { $query : query, $filter : filter }
     * @throws InvalidParseOperationException if jsonRequest could not parse to JSON
     */
    @Override
    protected void parseJson(final JsonNode jsonRequest) throws InvalidParseOperationException {
        super.parseJson(jsonRequest);
        internalParse();
    }

    /**
     * @param query containing only the JSON query part (no filter)
     * @throws InvalidParseOperationException if query could not parse to JSON or sanity check to query is in error
     */
    protected void parseQueryOnly(final String query) throws InvalidParseOperationException {
        GlobalDatasParser.sanityRequestCheck(query);
        sourceRequest = query;
        if (request != null) {
            request.reset();
        } else {
            request = getNewRequest();
        }
        hasFullTextQuery = false;
        rootNode = JsonHandler.getFromString(query);
        if (rootNode.isMissingNode()) {
            throw new InvalidParseOperationException("The current Node is missing(empty): RequestRoot");
        }
        // Not as array and no filter
        queryParse(rootNode);
        filterParse(JsonHandler.createObjectNode());
    }

    /**
     * Filter part
     *
     * @param rootNode JsonNode
     * @throws InvalidParseOperationException if rootNode could not parse to JSON
     */
    protected void filterParse(final JsonNode rootNode) throws InvalidParseOperationException {
        if (rootNode == null) {
            return;
        }
        GlobalDatas.sanityParametersCheck(rootNode.toString(), GlobalDatas.NB_FILTERS);
        try {
            // Check valid variable names first
            parseOrderByFilter(rootNode);

            request.setFilter(rootNode);
        } catch (final Exception e) {
            throw new InvalidParseOperationException("Parse in error for Filter: " + rootNode, e);
        }
    }

    /**
     * { query } if one level only
     *
     * @param rootNode JsonNode
     * @throws InvalidParseOperationException if rootNode could not parse to JSON
     */
    protected void queryParse(final JsonNode rootNode) throws InvalidParseOperationException {
        if (rootNode == null) {
            return;
        }
        try {
            // 1 level only: (request)
            analyzeRootQuery(rootNode);
        } catch (final Exception e) {
            throw new InvalidParseOperationException("Parse in error for Query: " + rootNode, e);
        }
    }

    /**
     * { expression }
     *
     * @param command JsonNode
     * @throws InvalidParseOperationException if command is null or command could not parse to JSON
     * @throws InvalidCreateOperationException if could not set query to request or analyzeOneCommand is in error
     */
    protected void analyzeRootQuery(final JsonNode command)
        throws InvalidParseOperationException, InvalidCreateOperationException {
        if (command == null) {
            throw new InvalidParseOperationException("Not correctly parsed");
        }
        // new Query to analyze, so reset to false (only one there)
        hasFullTextCurrentQuery = false;
        hasFullTextQuery = false;
        // Root may be empty: ok since it means get all
        if (command.size() == 0) {
            request.setQuery(nop());
            return;
        }
        // now single element
        final Entry<String, JsonNode> queryItem = JsonHandler.checkUnicity("RootRequest", command);
        Query query;
        if (queryItem.getKey().equalsIgnoreCase(QUERY.PATH.exactToken())) {
            final ArrayNode array = (ArrayNode) queryItem.getValue();
            query = path(array, adapter);
        } else {
            query = analyzeOneCommand(queryItem.getKey(), queryItem.getValue());
            if (query == null) {
                // NOP
                request.setQuery(nop());
                return;
            }
        }
        hasFullTextQuery = hasFullTextCurrentQuery;
        request.setQuery(query.setFullText(hasFullTextQuery));
    }

    /**
     * Allow to add one condition to the current parsed Request</br>
     * </br>
     * Example:</br>
     *
     * <pre>
     * <code>
     *   SelectParserSingle parser = new SelectParserSingle(...);
     *   parser.parse(jsonQuery);
     *   parser.addCondition(eq(FieldName, value));
     *   JsonNode newJsonQuery = parser.getRootNode();
     * </code>
     * </pre>
     *
     * @param condition the condition to add
     * @throws InvalidCreateOperationException when invalid create query exception occurred
     * @throws InvalidParseOperationException hen invalid parse data to create query
     */
    public void addCondition(Query condition) throws InvalidCreateOperationException, InvalidParseOperationException {
        final RequestParserSingle newOne = RequestParserHelperSingle.getParser(rootNode.deepCopy(), adapter);
        final Query query = request.getQuery();
        Query newQuery = null;
        if (query == null) {
            newQuery = condition;
            getRequest().setQuery(newQuery);
        } else {
            newQuery = QueryHelper.and().add(query, condition);
            getRequest().setQuery(newQuery);
        }
        if (newOne instanceof SelectParserSingle) {
            parse(((Select) getRequest()).getFinalSelect().deepCopy());
        } else if (newOne instanceof InsertParserSingle) {
            parse(((Insert) getRequest()).getFinalInsert().deepCopy());
        } else if (newOne instanceof UpdateParserSingle) {
            parse(((Update) getRequest()).getFinalUpdate().deepCopy());
        } else {
            parse(((Delete) getRequest()).getFinalDelete().deepCopy());
        }
        newOne.request = null;
        newOne.rootNode = null;
        newOne.sourceRequest = null;
    }

    @Override
    public String toString() {
        return request.toString();
    }

    /**
     * @return True if the hint contains notimeout
     */
    @Override
    public boolean hintNoTimeout() {
        final JsonNode jsonNode = request.getFilter().get(SELECTFILTER.HINT.exactToken());
        if (jsonNode != null) {
            final ArrayNode array = (ArrayNode) jsonNode;
            for (final JsonNode node : array) {
                if (ParserTokens.FILTERARGS.NOTIMEOUT.exactToken().equals(node.asText())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public int getLastDepth() {
        return 0;
    }

    @Override
    public FILTERARGS model() {
        return FILTERARGS.OTHERS;
    }

    @Override
    public boolean hintCache() {
        return false;
    }
}
