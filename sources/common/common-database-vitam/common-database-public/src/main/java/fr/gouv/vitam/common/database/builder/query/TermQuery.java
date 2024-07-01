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
import fr.gouv.vitam.common.database.builder.request.configuration.GlobalDatas;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;

import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Term Query
 */
public class TermQuery extends Query {

    private static final String CANNOT_BE_UPDATED_WITH_EMPTY_VARIABLE_NAME =
        " cannot be updated with empty variable name";
    private static final String QUERY2 = "Query ";
    private static final String CANNOT_ADD_A_TERM_ELEMENT_SINCE_THIS_IS_NOT_A_TERM_QUERY =
        "Cannot add a term element since this is not a Term Query: ";

    protected TermQuery() {
        super();
    }

    /**
     * Term Query constructor
     *
     * @param variableName key name
     * @param value key value
     * @throws InvalidCreateOperationException when query is invalid
     */
    public TermQuery(final String variableName, final String value) throws InvalidCreateOperationException {
        super();
        createQueryVariableValue(QUERY.TERM, variableName, value);
        currentTokenQUERY = QUERY.TERM;
        setReady(true);
    }

    /**
     * Term Query constructor
     *
     * @param variableName key name
     * @param value key value
     * @throws InvalidCreateOperationException when query is invalid
     */
    public TermQuery(final String variableName, final long value) throws InvalidCreateOperationException {
        super();
        createQueryVariableValue(QUERY.TERM, variableName, value);
        currentTokenQUERY = QUERY.TERM;
        setReady(true);
    }

    /**
     * Term Query constructor
     *
     * @param variableName key name
     * @param value key value
     * @throws InvalidCreateOperationException when query is invalid
     */
    public TermQuery(final String variableName, final double value) throws InvalidCreateOperationException {
        super();
        createQueryVariableValue(QUERY.TERM, variableName, value);
        currentTokenQUERY = QUERY.TERM;
        setReady(true);
    }

    /**
     * Term Query constructor
     *
     * @param variableName key name
     * @param value key value
     * @throws InvalidCreateOperationException when query is invalid
     */
    public TermQuery(final String variableName, final boolean value) throws InvalidCreateOperationException {
        super();
        createQueryVariableValue(QUERY.TERM, variableName, value);
        currentTokenQUERY = QUERY.TERM;
        setReady(true);
    }

    /**
     * Term Query constructor
     *
     * @param variableName key name
     * @param value key value
     * @throws InvalidCreateOperationException when query is invalid
     */
    public TermQuery(final String variableName, final Date value) throws InvalidCreateOperationException {
        super();
        createQueryVariableValue(QUERY.TERM, variableName, value);
        currentTokenQUERY = QUERY.TERM;
        setReady(true);
    }

    /**
     * Term Query constructor from Map
     *
     * @param variableNameValue map of name and value
     * @throws InvalidCreateOperationException when query is invalid
     */
    public TermQuery(final Map<String, Object> variableNameValue) throws InvalidCreateOperationException {
        super();
        currentObject = ((ObjectNode) currentObject).putObject(QUERY.TERM.exactToken());
        final ObjectNode node = (ObjectNode) currentObject;
        for (final Entry<String, Object> entry : variableNameValue.entrySet()) {
            final String name = entry.getKey();
            if (name == null || name.trim().isEmpty()) {
                continue;
            }
            try {
                GlobalDatas.sanityParameterCheck(name);
            } catch (final InvalidParseOperationException e) {
                throw new InvalidCreateOperationException(e);
            }
            final Object val = entry.getValue();
            node.set(name.trim(), GlobalDatas.getValueJsonNode(val));
        }
        currentTokenQUERY = QUERY.TERM;
        setReady(true);
    }

    /**
     * Add other Term sub queries to Term Query
     *
     * @param variableName key name
     * @param value key value
     * @return the TermQuery
     * @throws InvalidCreateOperationException when query is invalid
     */
    public final TermQuery add(final String variableName, final String value) throws InvalidCreateOperationException {
        if (currentTokenQUERY != QUERY.TERM) {
            throw new InvalidCreateOperationException(
                CANNOT_ADD_A_TERM_ELEMENT_SINCE_THIS_IS_NOT_A_TERM_QUERY + currentTokenQUERY
            );
        }
        if (variableName == null || variableName.trim().isEmpty()) {
            throw new InvalidCreateOperationException(
                QUERY2 + currentTokenQUERY + CANNOT_BE_UPDATED_WITH_EMPTY_VARIABLE_NAME
            );
        }
        try {
            GlobalDatas.sanityParameterCheck(variableName);
            GlobalDatas.sanityValueCheck(value);
        } catch (final InvalidParseOperationException e) {
            throw new InvalidCreateOperationException(e);
        }
        ((ObjectNode) currentObject).put(variableName.trim(), value);
        return this;
    }

    /**
     * Add other Term sub queries to Term Query
     *
     * @param variableName key name
     * @param value key value
     * @return the TermQuery
     * @throws InvalidCreateOperationException when query is invalid
     */
    public final TermQuery add(final String variableName, final long value) throws InvalidCreateOperationException {
        if (currentTokenQUERY != QUERY.TERM) {
            throw new InvalidCreateOperationException(
                CANNOT_ADD_A_TERM_ELEMENT_SINCE_THIS_IS_NOT_A_TERM_QUERY + currentTokenQUERY
            );
        }
        if (variableName == null || variableName.trim().isEmpty()) {
            throw new InvalidCreateOperationException(
                QUERY2 + currentTokenQUERY + CANNOT_BE_UPDATED_WITH_EMPTY_VARIABLE_NAME
            );
        }
        try {
            GlobalDatas.sanityParameterCheck(variableName);
        } catch (final InvalidParseOperationException e) {
            throw new InvalidCreateOperationException(e);
        }
        ((ObjectNode) currentObject).put(variableName.trim(), value);
        return this;
    }

    /**
     * Add other Term sub queries to Term Query
     *
     * @param variableName key name
     * @param value key value
     * @return the TermQuery
     * @throws InvalidCreateOperationException when query is invalid
     */
    public final TermQuery add(final String variableName, final double value) throws InvalidCreateOperationException {
        if (currentTokenQUERY != QUERY.TERM) {
            throw new InvalidCreateOperationException(
                CANNOT_ADD_A_TERM_ELEMENT_SINCE_THIS_IS_NOT_A_TERM_QUERY + currentTokenQUERY
            );
        }
        if (variableName == null || variableName.trim().isEmpty()) {
            throw new InvalidCreateOperationException(
                QUERY2 + currentTokenQUERY + CANNOT_BE_UPDATED_WITH_EMPTY_VARIABLE_NAME
            );
        }
        try {
            GlobalDatas.sanityParameterCheck(variableName);
        } catch (final InvalidParseOperationException e) {
            throw new InvalidCreateOperationException(e);
        }
        ((ObjectNode) currentObject).put(variableName.trim(), value);
        return this;
    }

    /**
     * Add other Term sub queries to Term Query
     *
     * @param variableName key name
     * @param value key value
     * @return the TermQuery
     * @throws InvalidCreateOperationException when query is invalid
     */
    public final TermQuery add(final String variableName, final boolean value) throws InvalidCreateOperationException {
        if (currentTokenQUERY != QUERY.TERM) {
            throw new InvalidCreateOperationException(
                CANNOT_ADD_A_TERM_ELEMENT_SINCE_THIS_IS_NOT_A_TERM_QUERY + currentTokenQUERY
            );
        }
        if (variableName == null || variableName.trim().isEmpty()) {
            throw new InvalidCreateOperationException(
                QUERY2 + currentTokenQUERY + CANNOT_BE_UPDATED_WITH_EMPTY_VARIABLE_NAME
            );
        }
        try {
            GlobalDatas.sanityParameterCheck(variableName);
        } catch (final InvalidParseOperationException e) {
            throw new InvalidCreateOperationException(e);
        }
        ((ObjectNode) currentObject).put(variableName.trim(), value);
        return this;
    }

    /**
     * Add other Term sub queries to Term Query
     *
     * @param variableName key name
     * @param value key value
     * @return the TermQuery
     * @throws InvalidCreateOperationException when query is invalid
     */
    public final TermQuery add(final String variableName, final Date value) throws InvalidCreateOperationException {
        if (currentTokenQUERY != QUERY.TERM) {
            throw new InvalidCreateOperationException(
                CANNOT_ADD_A_TERM_ELEMENT_SINCE_THIS_IS_NOT_A_TERM_QUERY + currentTokenQUERY
            );
        }
        if (variableName == null || variableName.trim().isEmpty()) {
            throw new InvalidCreateOperationException(
                QUERY2 + currentTokenQUERY + CANNOT_BE_UPDATED_WITH_EMPTY_VARIABLE_NAME
            );
        }
        try {
            GlobalDatas.sanityParameterCheck(variableName);
        } catch (final InvalidParseOperationException e) {
            throw new InvalidCreateOperationException(e);
        }
        ((ObjectNode) currentObject).set(variableName.trim(), GlobalDatas.getDate(value));
        return this;
    }
}
