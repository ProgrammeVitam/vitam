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

import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.QUERY;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.QUERYARGS;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;

/**
 * Match Query
 */
public class MatchQuery extends Query {

    private static final String QUERY2 = "Query ";
    private static final String IS_NOT_A_MATCH_QUERY = " is not a Match Query";

    protected MatchQuery() {
        super();
    }

    /**
     * Match Query constructor
     *
     * @param matchQuery match, match_phrase, match_phrase_prefix
     * @param variableName variable name
     * @param value of variable
     * @throws InvalidCreateOperationException when not valid
     */
    public MatchQuery(final QUERY matchQuery, final String variableName, final String value)
        throws InvalidCreateOperationException {
        super();
        switch (matchQuery) {
            case MATCH:
            case MATCH_ALL:
            case MATCH_PHRASE:
            case MATCH_PHRASE_PREFIX:
                createQueryVariableValue(matchQuery, variableName, value);
                currentTokenQUERY = matchQuery;
                setReady(true);
                break;
            default:
                throw new InvalidCreateOperationException(QUERY2 + matchQuery + IS_NOT_A_MATCH_QUERY);
        }
    }

    /**
     * @param max max expansions for Match type request only (not regex, search)
     * @return this MatchQuery
     * @throws InvalidCreateOperationException when not valid
     */
    public final MatchQuery setMatchMaxExpansions(final int max) throws InvalidCreateOperationException {
        switch (currentTokenQUERY) {
            case MATCH:
            case MATCH_ALL:
            case MATCH_PHRASE:
            case MATCH_PHRASE_PREFIX:
                ((ObjectNode) currentObject).put(QUERYARGS.MAX_EXPANSIONS.exactToken(), max);
                break;
            default:
                throw new InvalidCreateOperationException(QUERY2 + currentTokenQUERY + IS_NOT_A_MATCH_QUERY);
        }
        return this;
    }
}
