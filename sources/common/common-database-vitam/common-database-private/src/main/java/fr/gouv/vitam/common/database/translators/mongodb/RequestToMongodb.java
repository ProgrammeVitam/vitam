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
package fr.gouv.vitam.common.database.translators.mongodb;

import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.parser.request.AbstractParser;
import fr.gouv.vitam.common.database.parser.request.multiple.InsertParserMultiple;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.database.parser.request.multiple.UpdateParserMultiple;
import fr.gouv.vitam.common.database.parser.request.single.SelectParserSingle;
import fr.gouv.vitam.common.database.translators.RequestToAbstract;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import org.bson.conversions.Bson;

import java.util.List;
import java.util.Set;

/**
 * Request to MongoDb
 */
public abstract class RequestToMongodb extends RequestToAbstract {

    /**
     * @param requestParser AbstractParser of unknown type
     */
    public RequestToMongodb(AbstractParser<?> requestParser) {
        super(requestParser);
    }

    /**
     * Create the RequestToMongoDB adapted to the RequestParser
     *
     * @param requestParser AbstractParser of unknown type
     * @return the associated RequestToMongoDb
     */
    public static RequestToMongodb getRequestToMongoDb(AbstractParser<?> requestParser) {
        if (requestParser instanceof SelectParserMultiple) {
            return new SelectToMongodb(requestParser);
        } else if (requestParser instanceof SelectParserSingle) {
            return new SelectToMongodb(requestParser);
        } else if (requestParser instanceof InsertParserMultiple) {
            return new InsertToMongodb(requestParser);
        } else if (requestParser instanceof UpdateParserMultiple) {
            return new UpdateToMongodb(requestParser);
        } else {
            return new DeleteToMongodb(requestParser);
        }
    }

    /**
     * Additional filter to first request
     *
     * @param field Field from which the proposed values shall be found
     * @return the filter associated with the initial roots
     * @throws InvalidParseOperationException if field could not parse to JSON
     */
    public Bson getInitialRoots(final String field) throws InvalidParseOperationException {
        final Set<String> roots = requestParser.getRequest().getRoots();
        return QueryToMongodb.getRoots(field, roots);
    }

    /**
     * @param roots Bson
     * @param query Bson
     * @return the final request
     */
    public Bson getRequest(Bson roots, Bson query) {
        return QueryToMongodb.getFullCommand(query, roots);
    }

    /**
     * find(query)
     *
     * @param nth int
     * @return the associated query for find (missing the source however, as initialRoots)
     * @throws IllegalAccessException if nth exceed the size of list
     * @throws IllegalAccessError if query is full text
     * @throws InvalidParseOperationException if could not get command by query
     */
    public Bson getNthQueries(final int nth)
        throws IllegalAccessException, IllegalAccessError, InvalidParseOperationException {
        final List<Query> list = requestParser.getRequest().getQueries();
        if (nth >= list.size()) {
            throw new IllegalAccessError("This Query has not enough item to get the position: " + nth);
        }
        final Query query = list.get(nth);
        if (query.isFullText()) {
            throw new IllegalAccessException("This Query is to be computed by Elasticsearch: " + nth);
        }
        return QueryToMongodb.getCommand(query);
    }
}
