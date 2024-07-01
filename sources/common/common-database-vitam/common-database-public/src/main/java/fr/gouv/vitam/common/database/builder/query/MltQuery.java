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
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.QUERY;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.QUERYARGS;
import fr.gouv.vitam.common.database.builder.request.configuration.GlobalDatas;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;

import java.util.HashSet;
import java.util.Set;

/**
 * MoreLikeThis Query
 */
public class MltQuery extends Query {

    private static final String CANNOT_BE_CREATED_WITH_EMPTY_VARIABLE_NAME =
        " cannot be created with empty variable name";
    private static final String QUERY2 = "Query ";
    protected Set<String> stringVals;

    protected MltQuery() {
        super();
    }

    @Override
    public void clean() {
        super.clean();
        if (stringVals != null) {
            stringVals.clear();
        }
        stringVals = null;
    }

    /**
     * MoreLikeThis Query constructor
     *
     * @param mltQuery flt, mlt
     * @param value to compare
     * @param variableNames criteria of query
     * @throws InvalidCreateOperationException when query is not valid
     */
    public MltQuery(final QUERY mltQuery, final String value, final String... variableNames)
        throws InvalidCreateOperationException {
        super();
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidCreateOperationException(QUERY2 + mltQuery + CANNOT_BE_CREATED_WITH_EMPTY_VARIABLE_NAME);
        }
        try {
            GlobalDatas.sanityValueCheck(value);
        } catch (final InvalidParseOperationException e) {
            throw new InvalidCreateOperationException(e);
        }
        switch (mltQuery) {
            case FLT:
            case MLT:
                final ObjectNode sub = ((ObjectNode) currentObject).putObject(mltQuery.exactToken());
                final ArrayNode array = sub.putArray(QUERYARGS.FIELDS.exactToken());
                stringVals = new HashSet<>();
                for (final String varName : variableNames) {
                    if (varName == null || varName.trim().isEmpty()) {
                        continue;
                    }
                    try {
                        GlobalDatas.sanityParameterCheck(varName);
                    } catch (final InvalidParseOperationException e) {
                        throw new InvalidCreateOperationException(e);
                    }
                    final String var = varName.trim();
                    if (!stringVals.contains(var)) {
                        array.add(var);
                        stringVals.add(var);
                    }
                }
                currentObject = array;
                sub.put(QUERYARGS.LIKE.exactToken(), value);
                break;
            default:
                throw new InvalidCreateOperationException(QUERY2 + mltQuery + " is not an MoreLikeThis or In Query");
        }
        currentTokenQUERY = mltQuery;
        setReady(true);
    }

    /**
     * Add a variable into the Mlt Query
     *
     * @param variableName variable name
     * @return the MltQuery
     * @throws InvalidCreateOperationException when query is not valid
     */
    public final MltQuery add(final String... variableName) throws InvalidCreateOperationException {
        if (currentTokenQUERY != QUERY.FLT && currentTokenQUERY != QUERY.MLT) {
            throw new InvalidCreateOperationException(
                "Cannot add a variableName since this is not an Mlt Query: " + currentTokenQUERY
            );
        }
        final ArrayNode array = (ArrayNode) currentObject;
        if (stringVals == null) {
            stringVals = new HashSet<>();
        }
        for (String val : variableName) {
            if (val == null || val.trim().isEmpty()) {
                throw new InvalidCreateOperationException(
                    QUERY2 + currentTokenQUERY + " cannot be updated with empty variable name"
                );
            }
            try {
                GlobalDatas.sanityParameterCheck(val);
            } catch (final InvalidParseOperationException e) {
                throw new InvalidCreateOperationException(e);
            }
            val = val.trim();
            if (!stringVals.contains(val)) {
                array.add(val);
                stringVals.add(val);
            }
        }
        return this;
    }
}
