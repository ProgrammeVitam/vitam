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
package fr.gouv.vitam.common.database.builder.query.action;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken;
import fr.gouv.vitam.common.database.builder.request.configuration.GlobalDatas;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Setregex Action: $setregex : { name : value, name : value, ... }
 */
public class SetregexAction extends Action {

    private static final String CANNOT_ADD_AN_ELEMENT_SINCE_THIS_IS_NOT_A_SETREGEX_ACTION =
        "Cannot add an element since this is not a Setregex Action: ";
    private static final String CANNOT_BE_UPDATED_WITH_EMPTY_VARIABLE_NAME =
        " cannot be updated with empty variable name";
    private static final String ACTION = "Action ";

    /**
     * SetregexAction constructor.
     */
    protected SetregexAction() {
        super();
    }

    /**
     * Setregex Action constructor
     *
     * @param variableName key name
     * @param value key value
     * @throws InvalidCreateOperationException when query is invalid
     */
    public SetregexAction(final String variableName, final String value) throws InvalidCreateOperationException {
        super();
        createActionVariableValue(BuilderToken.UPDATEACTION.SETREGEX, variableName, value);
        currentUPDATEACTION = BuilderToken.UPDATEACTION.SETREGEX;
        setReady(true);
    }

    /**
     * Setregex Action constructor
     *
     * @param variableName key name
     * @param value key value as a list of values
     * @throws InvalidCreateOperationException when query is invalid
     */
    public SetregexAction(final String variableName, final List<?> value) throws InvalidCreateOperationException {
        super();
        createActionVariableValue(BuilderToken.UPDATEACTION.SETREGEX, variableName, value);
        currentUPDATEACTION = BuilderToken.UPDATEACTION.SETREGEX;
        setReady(true);
    }

    /**
     * Setregex Action constructor from Map
     *
     * @param variableNameValue map of key and Value
     * @throws InvalidCreateOperationException when query is invalid
     */
    public SetregexAction(final Map<String, ?> variableNameValue) throws InvalidCreateOperationException {
        super();
        currentObject = ((ObjectNode) currentObject).putObject(BuilderToken.UPDATEACTION.SETREGEX.exactToken());
        final ObjectNode node = (ObjectNode) currentObject;
        for (final Map.Entry<String, ?> entry : variableNameValue.entrySet()) {
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
        currentUPDATEACTION = BuilderToken.UPDATEACTION.SETREGEX;
        setReady(true);
    }

    /**
     * Setregex Action constructor from ObjectNode
     *
     * @param updateData ObjectNode natively
     * @throws InvalidCreateOperationException when query is invalid
     */
    public SetregexAction(final ObjectNode updateData) throws InvalidCreateOperationException {
        super();
        currentObject = ((ObjectNode) currentObject).putObject(BuilderToken.UPDATEACTION.SETREGEX.exactToken());
        final ObjectNode node = (ObjectNode) currentObject;

        Iterator<String> iterator = updateData.fieldNames();
        while (iterator.hasNext()) {
            final String key = iterator.next();
            try {
                GlobalDatas.sanityParameterCheck(key);
            } catch (final InvalidParseOperationException e) {
                throw new InvalidCreateOperationException(e);
            }
            node.set(key, updateData.get(key));
        }
        currentUPDATEACTION = BuilderToken.UPDATEACTION.SETREGEX;
        setReady(true);
    }

    /**
     * Setregex Action constructor
     *
     * @param variableName key name
     * @param value key value
     * @throws InvalidCreateOperationException when query is invalid
     */
    public SetregexAction(final String variableName, final long value) throws InvalidCreateOperationException {
        super();
        createActionVariableValue(BuilderToken.UPDATEACTION.SETREGEX, variableName, value);
        currentUPDATEACTION = BuilderToken.UPDATEACTION.SETREGEX;
        setReady(true);
    }

    /**
     * Setregex Action constructor
     *
     * @param variableName key name
     * @param value key value
     * @throws InvalidCreateOperationException when query is invalid
     */
    public SetregexAction(final String variableName, final boolean value) throws InvalidCreateOperationException {
        super();
        createActionVariableValue(BuilderToken.UPDATEACTION.SETREGEX, variableName, value);
        currentUPDATEACTION = BuilderToken.UPDATEACTION.SETREGEX;
        setReady(true);
    }

    /**
     * Setregex Action constructor
     *
     * @param variableName key name
     * @param value key value
     * @throws InvalidCreateOperationException when query is invalid
     */
    public SetregexAction(final String variableName, final double value) throws InvalidCreateOperationException {
        super();
        createActionVariableValue(BuilderToken.UPDATEACTION.SETREGEX, variableName, value);
        currentUPDATEACTION = BuilderToken.UPDATEACTION.SETREGEX;
        setReady(true);
    }

    /**
     * Add other Set sub actions to Query
     *
     * @param variableName key name
     * @param value key value
     * @return the SetregexAction
     * @throws InvalidCreateOperationException when query is invalid
     */
    public final SetregexAction add(final String variableName, final String value)
        throws InvalidCreateOperationException {
        if (currentUPDATEACTION != BuilderToken.UPDATEACTION.SETREGEX) {
            throw new InvalidCreateOperationException(
                CANNOT_ADD_AN_ELEMENT_SINCE_THIS_IS_NOT_A_SETREGEX_ACTION + currentUPDATEACTION
            );
        }
        if (variableName == null || variableName.trim().isEmpty()) {
            throw new InvalidCreateOperationException(
                ACTION + currentUPDATEACTION + CANNOT_BE_UPDATED_WITH_EMPTY_VARIABLE_NAME
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
     * Add other Set sub actions to Query
     *
     * @param variableName key name
     * @param values values as list
     * @return the SetregexAction
     * @throws InvalidCreateOperationException when query is invalid
     */
    public final SetregexAction add(final String variableName, final List<?> values)
        throws InvalidCreateOperationException {
        if (currentUPDATEACTION != BuilderToken.UPDATEACTION.SETREGEX) {
            throw new InvalidCreateOperationException(
                CANNOT_ADD_AN_ELEMENT_SINCE_THIS_IS_NOT_A_SETREGEX_ACTION + currentUPDATEACTION
            );
        }
        if (variableName == null || variableName.trim().isEmpty()) {
            throw new InvalidCreateOperationException(
                ACTION + currentUPDATEACTION + CANNOT_BE_UPDATED_WITH_EMPTY_VARIABLE_NAME
            );
        }
        if (values == null) {
            throw new InvalidCreateOperationException(ACTION + currentUPDATEACTION + " cannot update with null list");
        }
        try {
            GlobalDatas.sanityParameterCheck(variableName);
            if (!values.isEmpty()) {
                GlobalDatas.sanityValueCheck(values);
            }
        } catch (final InvalidParseOperationException e) {
            throw new InvalidCreateOperationException(e);
        }
        ArrayNode array = JsonHandler.createArrayNode();
        GlobalDatas.setArrayValueFromList(array, values);
        ((ObjectNode) currentObject).set(variableName.trim(), array);
        return this;
    }
}
