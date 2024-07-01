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
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.request.AbstractRequest;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.FILTERARGS;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.parser.query.QueryParserHelper;
import fr.gouv.vitam.common.database.parser.request.adapter.VarNameAdapter;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;

import java.util.Iterator;

/**
 * Abstract class implementing Parser for a Request
 *
 * Common abstract for both Multiple and Single Request
 *
 * @param <E> is one of RequestMultiple or RequestSingle
 */
public abstract class AbstractParser<E extends AbstractRequest> {

    protected VarNameAdapter adapter;
    protected String sourceRequest;

    protected E request;

    /**
     * Contains queries to be computed by a full text index
     */
    protected boolean hasFullTextQuery = false;
    /**
     * Current analyzed query to be computed by a full text index
     */
    protected boolean hasFullTextCurrentQuery = false;
    protected JsonNode rootNode;

    /**
     * @return the source
     */
    public String getSource() {
        return sourceRequest;
    }

    /**
     * @return the rootNode
     */
    public JsonNode getRootNode() {
        return rootNode;
    }

    /**
     * @return the Request
     */
    public E getRequest() {
        return request;
    }

    /**
     * @return a new Request
     */
    protected abstract E getNewRequest();

    /**
     * @param jsonRequest containing a parsed JSON as { $roots: root, $query : query, $filter : filter }
     * @throws InvalidParseOperationException if jsonRequest could not parse to JSON
     */
    public abstract void parse(final JsonNode jsonRequest) throws InvalidParseOperationException;

    /**
     * @param jsonRequest containing a parsed JSON as { $roots: root, $query : query, $filter : filter }
     * @throws InvalidParseOperationException if jsonRequest could not parse to JSON
     */
    protected void parseJson(final JsonNode jsonRequest) throws InvalidParseOperationException {
        rootNode = jsonRequest;
        sourceRequest = JsonHandler.unprettyPrint(jsonRequest);
    }

    /**
     * @return the hasFullTextQuery
     */
    public final boolean hasFullTextQuery() {
        return hasFullTextQuery;
    }

    /**
     * @return the adapter
     */
    public VarNameAdapter getAdapter() {
        return adapter;
    }

    protected Query analyzeOneCommand(final String refCommand, final JsonNode command)
        throws InvalidParseOperationException, InvalidCreateOperationException {
        Query query = QueryParserHelper.query(refCommand, command, adapter);
        if (query != null) {
            hasFullTextCurrentQuery |= query.isFullText();
        }
        return query;
    }

    /**
     * @return int the depth of the query
     */
    public abstract int getLastDepth();

    /**
     * @return FILTERARGS the filter argument
     */
    public abstract FILTERARGS model();

    /**
     * @return true if not time out
     */
    public abstract boolean hintNoTimeout();

    /**
     * @return true if not cache hint
     */
    public abstract boolean hintCache();

    protected void parseOrderByFilter(JsonNode filterNode) throws InvalidParseOperationException {
        if (filterNode.has(BuilderToken.SELECTFILTER.ORDERBY.exactToken())) {
            final ObjectNode node = (ObjectNode) filterNode.get(BuilderToken.SELECTFILTER.ORDERBY.exactToken());
            ObjectNode finalNode = node.deepCopy();
            final Iterator<String> names = node.fieldNames();
            while (names.hasNext()) {
                final String name = names.next();
                final String dbName = adapter.getVariableName(name);

                // Force update rootNode with correct dbName (replace '#' by '_')
                if (null != dbName) {
                    final JsonNode value = finalNode.remove(name);
                    finalNode.set(dbName, value);
                }
            }
            node.removeAll();
            node.setAll(finalNode);
        }
    }
}
