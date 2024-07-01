/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common.database.builder.request.single;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.query.action.Action;
import fr.gouv.vitam.common.database.builder.request.AbstractRequest;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.GLOBAL;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.json.JsonHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Request for Single Mode Query
 */
public abstract class RequestSingle extends AbstractRequest {

    protected Query query;

    /**
     * @return this Request
     */
    public final RequestSingle resetQuery() {
        if (query != null) {
            query.clean();
        }
        return this;
    }

    /**
     * @return this Request
     */
    @Override
    public RequestSingle reset() {
        resetFilter();
        resetQuery();
        return this;
    }

    /**
     * @param query of request
     * @return this Request
     * @throws InvalidCreateOperationException whern query is invalid
     */
    @Override
    public final RequestSingle setQuery(final Query query) throws InvalidCreateOperationException {
        ParametersChecker.checkParameter("Query is a mandatory parameter", query);
        if (query == null) {
            throw new InvalidCreateOperationException("Null Query");
        }
        if (!query.isReady()) {
            throw new InvalidCreateOperationException("Query is not ready to be set: " + query.getCurrentQuery());
        }
        this.query = query;
        return this;
    }

    /**
     * Get the json final of request
     *
     * @return the Final json containing all 2 parts: query and filter
     */
    @Override
    protected final ObjectNode getFinal() {
        final ObjectNode node = JsonHandler.createObjectNode();
        if (query != null) {
            node.set(GLOBAL.QUERY.exactToken(), query.getCurrentQuery());
        } else {
            node.putObject(GLOBAL.QUERY.exactToken());
        }
        if (filter != null && filter.size() > 0) {
            node.set(GLOBAL.FILTER.exactToken(), filter);
        } else {
            node.putObject(GLOBAL.FILTER.exactToken());
        }
        return node;
    }

    /**
     * @return the query
     */
    public final Query getQuery() {
        return query;
    }

    /**
     * @return the number of queries
     */
    @Override
    public final int getNbQueries() {
        return 1;
    }

    /**
     * default implements of getQueries
     */
    @Override
    public List<Query> getQueries() {
        final List<Query> queries = new ArrayList<>();
        queries.add(query);
        return queries;
    }

    @Override
    public Set<String> getRoots() {
        return Collections.emptySet();
    }

    @Override
    public JsonNode getData() {
        return JsonHandler.createObjectNode();
    }

    @Override
    public boolean getAllProjection() {
        return selectGetAllProjection();
    }

    @Override
    public ObjectNode getProjection() {
        return selectGetProjection();
    }

    /**
     * default implements of getActions
     */
    @Override
    public List<Action> getActions() {
        return Collections.emptyList();
    }

    @Override
    public String toString() {
        return new StringBuilder()
            .append("Requests: ")
            .append(query != null ? query : "")
            .append(super.toString())
            .toString();
    }

    /**
     * @return the multiple data to insert at once
     */
    public ArrayNode getDatas() {
        return JsonHandler.createArrayNode();
    }
}
