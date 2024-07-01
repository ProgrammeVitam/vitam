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
package fr.gouv.vitam.common.database.builder.query;

import com.fasterxml.jackson.databind.node.ArrayNode;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.QUERY;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;

import java.util.ArrayList;
import java.util.List;

/**
 * Boolean Query
 */
public class BooleanQuery extends Query {

    protected List<Query> queries = new ArrayList<>();

    protected BooleanQuery() {
        super();
    }

    /**
     * BooleanQuery constructor
     *
     * @param booleanQuery and or not
     * @throws InvalidCreateOperationException when not a boolean query or error
     */
    public BooleanQuery(final QUERY booleanQuery) throws InvalidCreateOperationException {
        super();
        switch (booleanQuery) {
            case AND:
            case NOT:
            case OR:
                createQueryArray(booleanQuery);
                currentTokenQUERY = booleanQuery;
                break;
            default:
                throw new InvalidCreateOperationException("Query " + booleanQuery + " is not a Boolean Query");
        }
    }

    @Override
    public void clean() {
        super.clean();
        for (final Query query : queries) {
            query.clean();
        }
        queries.clear();
    }

    /**
     * Add sub queries to Boolean Query
     *
     * @param queries list of query
     * @return the BooleanQuery
     * @throws InvalidCreateOperationException when not ready or error
     */
    public final BooleanQuery add(final Query... queries) throws InvalidCreateOperationException {
        if (currentTokenQUERY != null) {
            switch (currentTokenQUERY) {
                case AND:
                case NOT:
                case OR:
                    break;
                default:
                    throw new InvalidCreateOperationException(
                        "Requests cannot be added since this is not a boolean request: " + currentTokenQUERY
                    );
            }
        }
        final ArrayNode array = (ArrayNode) currentObject;
        for (final Query elt : queries) {
            if (!elt.isReady()) {
                throw new InvalidCreateOperationException(
                    "Requests cannot be added since not ready: " + elt.getCurrentQuery()
                );
            }
            // in case sub request has those element set: not allowed
            elt.cleanDepth();
            if (
                (currentTokenQUERY == QUERY.AND && elt.currentTokenQUERY == QUERY.AND) ||
                (currentTokenQUERY == QUERY.OR && elt.currentTokenQUERY == QUERY.OR)
            ) {
                final BooleanQuery subelts = (BooleanQuery) elt;
                for (final Query sub : subelts.queries) {
                    this.queries.add(sub);
                    array.add(sub.getCurrentQuery());
                }
            } else {
                this.queries.add(elt);
                array.add(elt.getCurrentQuery());
            }
        }
        setReady(true);
        return this;
    }

    /**
     * @return the list of Queries under this Boolean Query
     */
    public List<Query> getQueries() {
        return queries;
    }
}
