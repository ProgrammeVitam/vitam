/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 * 
 * This software is a computer program whose purpose is to implement a digital 
 * archiving back-office system managing high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL 2.1
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL 2.1 license and that you accept its terms.
 */
package fr.gouv.vitam.common.database.builder.request;

import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.query.action.Action;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.SELECTFILTER;
import fr.gouv.vitam.common.database.builder.request.configuration.GlobalDatas;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;

/**
 * 
 */
public abstract class AbstractRequest {
    protected ObjectNode filter;

    /**
     *
     * @return this Request
     */
    public final AbstractRequest resetHintFilter() {
        if (filter != null) {
            filter.remove(SELECTFILTER.HINT.exactToken());
        }
        return this;
    }

    /**
     *
     * @return this Request
     */
    public final AbstractRequest resetFilter() {
        if (filter != null) {
            filter.removeAll();
        }
        return this;
    }



    /**
     * @return this Request
     */
    public AbstractRequest reset() {
        resetFilter();
        return this;
    }

    /**
     *
     * @param hints
     * @return this Request
     * @throws InvalidParseOperationException
     */
    public final AbstractRequest addHintFilter(final String... hints)
        throws InvalidParseOperationException {
        ParametersChecker.checkParameter("Hint filter is a mandatory parameter", hints);
        if (filter == null) {
            filter = JsonHandler.createObjectNode();
        }
        ArrayNode array = (ArrayNode) filter.get(SELECTFILTER.HINT.exactToken());
        if (array == null || array.isMissingNode()) {
            array = filter.putArray(SELECTFILTER.HINT.exactToken());
        }
        for (final String hint : hints) {
            GlobalDatas.sanityParameterCheck(hint);
            if (hint == null || hint.trim().isEmpty()) {
                continue;
            }
            array.add(hint.trim());
        }
        return this;
    }

    /**
     *
     * @param filterContent
     * @return this Request
     */
    public final AbstractRequest addHintFilter(final JsonNode filterContent) {
        ParametersChecker.checkParameter("Filter Content is a mandatory parameter", filterContent);
        if (filter == null) {
            filter = JsonHandler.createObjectNode();
        }
        if (filterContent.has(SELECTFILTER.HINT.exactToken())) {
            final JsonNode node = filterContent.get(SELECTFILTER.HINT.exactToken());
            if (node.isArray()) {
                filter.putArray(SELECTFILTER.HINT.exactToken()).addAll((ArrayNode) node);
            } else {
                filter.putArray(SELECTFILTER.HINT.exactToken()).add(node.asText());
            }
        }
        return this;
    }

    /**
     *
     * @param filter
     * @return this Request
     * @throws InvalidParseOperationException
     */
    public final AbstractRequest parseHintFilter(final String filter)
        throws InvalidParseOperationException {
        GlobalDatas.sanityParametersCheck(filter, GlobalDatas.NB_FILTERS);
        final JsonNode rootNode = JsonHandler.getFromString(filter);
        return addHintFilter(rootNode);
    }

    /**
     *
     * @param filterContent
     * @return this Request
     * @throws InvalidParseOperationException
     */
    public AbstractRequest setFilter(final JsonNode filterContent)
        throws InvalidParseOperationException {
        resetFilter();
        return addHintFilter(filterContent);
    }

    /**
     *
     * @param filter
     * @return this Request
     * @throws InvalidParseOperationException
     */
    public final AbstractRequest parseFilter(final String filter)
        throws InvalidParseOperationException {
        GlobalDatas.sanityParametersCheck(filter, GlobalDatas.NB_FILTERS);
        final JsonNode filterContent = JsonHandler.getFromString(filter);
        return setFilter(filterContent);
    }


    /**
     * @return the filter
     */
    public final ObjectNode getFilter() {
        if (filter == null) {
            return JsonHandler.createObjectNode();
        }
        return filter;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("\n\tFilter: ").append(filter);
        return builder.toString();
    }

    /**
     * Set the query of request: in case of multi-query request: re-initialize list of query 
     * 
     * @param query
     */
    public abstract AbstractRequest setQuery(Query query) throws InvalidCreateOperationException;
    
    /**
     * @return the number of queries
     */
    public abstract int getNbQueries();
    
    /**
     * @return the queries list
     */
    public abstract List<Query> getQueries();
    
    /**
     * @return the queries list
     */
    public abstract Set<String> getRoots();

    /**
     * @return
     */
    public abstract JsonNode getProjection();
    
    /**
     * @return
     */
    public abstract JsonNode getData();
    
    /**
     * @return
     */
    public abstract List<Action> getActions();
    
    
    /**
     * @return
     */
    public abstract boolean getAllProjection();
    
    

}
