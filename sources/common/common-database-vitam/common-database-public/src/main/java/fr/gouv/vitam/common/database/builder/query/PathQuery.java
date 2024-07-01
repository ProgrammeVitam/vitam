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
import fr.gouv.vitam.common.database.builder.request.configuration.GlobalDatas;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;

/**
 * Path Query
 */
public class PathQuery extends Query {

    protected PathQuery() {
        super();
    }

    /**
     * Path Query constructor
     *
     * @param pathes node's branch
     * @throws InvalidCreateOperationException when query is not valid
     */
    public PathQuery(final String... pathes) throws InvalidCreateOperationException {
        super();
        createQueryArray(QUERY.PATH);
        final ArrayNode array = (ArrayNode) currentObject;
        for (final String elt : pathes) {
            if (elt == null || elt.trim().isEmpty()) {
                continue;
            }
            try {
                GlobalDatas.sanityParameterCheck(elt);
            } catch (final InvalidParseOperationException e) {
                throw new InvalidCreateOperationException(e);
            }
            array.add(elt.trim());
        }
        if (array.size() == 0) {
            throw new InvalidCreateOperationException("No path to add");
        }
        currentTokenQUERY = QUERY.PATH;
        setReady(true);
    }

    /**
     * Add other paths (at end) to a PATH Query
     *
     * @param pathes node's branch
     * @return this PathQuery
     * @throws InvalidCreateOperationException when query is not valid
     */
    public final PathQuery add(final String... pathes) throws InvalidCreateOperationException {
        if (currentTokenQUERY != QUERY.PATH) {
            throw new InvalidCreateOperationException(
                "Path cannot be added since this is not a path request: " + currentTokenQUERY
            );
        }
        final ArrayNode array = (ArrayNode) currentObject;
        for (final String elt : pathes) {
            if (elt == null || elt.trim().isEmpty()) {
                continue;
            }
            try {
                GlobalDatas.sanityParameterCheck(elt);
            } catch (final InvalidParseOperationException e) {
                throw new InvalidCreateOperationException(e);
            }
            array.add(elt.trim());
        }
        return this;
    }
}
