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
package fr.gouv.vitam.common.database.translators.elasticsearch;

import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.collections.DynamicParserTokens;
import fr.gouv.vitam.common.database.parser.request.AbstractParser;
import fr.gouv.vitam.common.database.parser.request.adapter.VarNameAdapter;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.database.parser.request.single.SelectParserSingle;
import fr.gouv.vitam.common.database.translators.RequestToAbstract;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import org.elasticsearch.index.query.QueryBuilder;

import java.util.List;
import java.util.Set;

/**
 * Request To Elasticsearch
 */
public abstract class RequestToElasticsearch extends RequestToAbstract {

    /**
     * @param requestParser AbstractParser of unknown type
     */
    public RequestToElasticsearch(AbstractParser<?> requestParser) {
        super(requestParser);
    }

    /**
     * Create the RequestToElasticsearch adapted to the RequestParser
     *
     * @param requestParser AbstractParser of unknown type
     * @return the associated RequestToElasticsearch
     * @throws VitamException the type of request is not select
     */
    public static RequestToElasticsearch getRequestToElasticsearch(AbstractParser<?> requestParser)
        throws VitamException {
        if (requestParser instanceof SelectParserMultiple || requestParser instanceof SelectParserSingle) {
            return new SelectToElasticsearch(requestParser);
        } else {
            throw new VitamException("Only Select Request is allowed on Indexation");
        }
    }

    /**
     * Additional filter to first request
     *
     * @param field Field from which the proposed values shall be found
     * @return the filter associated with the initial roots
     * @throws InvalidParseOperationException if field could not parse to JSON
     */
    public QueryBuilder getInitialRoots(final String field) throws InvalidParseOperationException {
        final Set<String> roots = requestParser.getRequest().getRoots();
        return QueryToElasticsearch.getRoots(field, roots);
    }

    /**
     * @param roots QueryBuilder
     * @param query QueryBuilder
     * @return the final request
     */
    public QueryBuilder getRequest(QueryBuilder roots, QueryBuilder query) {
        return QueryToElasticsearch.getFullCommand(query, roots);
    }

    /**
     * find(query)
     *
     * @param nth int
     * @param parserTokens DynamicParserTokens
     * @return the associated query for find (missing the source however, as initialRoots)
     * @throws IllegalAccessError if nth exceed the size of list
     * @throws InvalidParseOperationException if could not get command by query
     */
    public QueryBuilder getNthQueries(final int nth, VarNameAdapter adapter, DynamicParserTokens parserTokens)
        throws IllegalAccessError, InvalidParseOperationException {
        final List<Query> list = requestParser.getRequest().getQueries();
        if (nth >= list.size()) {
            throw new IllegalAccessError("This Query has not enough item to get the position: " + nth);
        }
        final Query query = list.get(nth);
        return QueryToElasticsearch.getCommand(query, adapter, parserTokens);
    }
}
