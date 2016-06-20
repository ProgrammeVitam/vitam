/*******************************************************************************
 * This file is part of Vitam Project.
 *
 * Copyright Vitam (2012, 2016)
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL license as circulated
 * by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL license and that you
 * accept its terms.
 *******************************************************************************/
package fr.gouv.vitam.builder.request.construct.action;

import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.gouv.vitam.builder.request.construct.configuration.GlobalDatas;
import fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.UPDATEACTION;
import fr.gouv.vitam.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;

/**
 * Set Action: $set : { name : value, name : value, ... }
 *
 */
public class SetAction extends Action {
    private static final String CANNOT_BE_UPDATED_WITH_EMPTY_VARIABLE_NAME =
        " cannot be updated with empty variable name";
    private static final String CANNOT_ADD_A_SET_ELEMENT_SINCE_THIS_IS_NOT_A_SET_ACTION =
        "Cannot add a set element since this is not a Set Action: ";
    private static final String CANNOT_BE_CREATED_WITH_EMPTY_VARIABLE_NAME2 =
        " cannot be created with empty variable name";
    private static final String ACTION = "Action ";

    protected SetAction() {
        super();
    }

    /**
     * Set Action constructor
     *
     * @param variableName
     * @param value
     * @throws InvalidCreateOperationException
     */
    public SetAction(final String variableName, final String value)
        throws InvalidCreateOperationException {
        super();
        createActionVariableValue(UPDATEACTION.SET, variableName, value);
        currentUPDATEACTION = UPDATEACTION.SET;
        setReady(true);
    }

    /**
     * Set Action constructor from Map
     *
     * @param variableNameValue
     * @throws InvalidCreateOperationException
     */
    public SetAction(final Map<String, ?> variableNameValue)
        throws InvalidCreateOperationException {
        super();
        currentObject =
            ((ObjectNode) currentObject).putObject(UPDATEACTION.SET.exactToken());
        final ObjectNode node = (ObjectNode) currentObject;
        for (final Entry<String, ?> entry : variableNameValue.entrySet()) {
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
        currentUPDATEACTION = UPDATEACTION.SET;
        setReady(true);
    }

    /**
     * Set Action constructor
     *
     * @param variableName
     * @param value
     * @throws InvalidCreateOperationException
     */
    public SetAction(final String variableName, final long value)
        throws InvalidCreateOperationException {
        super();
        createActionVariableValue(UPDATEACTION.SET, variableName, value);
        currentUPDATEACTION = UPDATEACTION.SET;
        setReady(true);
    }

    /**
     * Set Action constructor
     *
     * @param variableName
     * @param value
     * @throws InvalidCreateOperationException
     */
    public SetAction(final String variableName, final boolean value)
        throws InvalidCreateOperationException {
        super();
        createActionVariableValue(UPDATEACTION.SET, variableName, value);
        currentUPDATEACTION = UPDATEACTION.SET;
        setReady(true);
    }

    /**
     * Set Action constructor
     *
     * @param variableName
     * @param value
     * @throws InvalidCreateOperationException
     */
    public SetAction(final String variableName, final double value)
        throws InvalidCreateOperationException {
        super();
        createActionVariableValue(UPDATEACTION.SET, variableName, value);
        currentUPDATEACTION = UPDATEACTION.SET;
        setReady(true);
    }

    /**
     * Set Action constructor
     *
     * @param variableName
     * @param value
     * @throws InvalidCreateOperationException
     */
    public SetAction(final String variableName, final Date value)
        throws InvalidCreateOperationException {
        super();
        if (variableName == null || variableName.trim().isEmpty()) {
            throw new InvalidCreateOperationException(
                ACTION + currentUPDATEACTION + CANNOT_BE_CREATED_WITH_EMPTY_VARIABLE_NAME2);
        }
        try {
            GlobalDatas.sanityParameterCheck(variableName);
        } catch (final InvalidParseOperationException e) {
            throw new InvalidCreateOperationException(e);
        }
        currentObject =
            ((ObjectNode) currentObject).putObject(UPDATEACTION.SET.exactToken());
        final ObjectNode node = (ObjectNode) currentObject;
        node.set(variableName, GlobalDatas.getDate(value));
        currentUPDATEACTION = UPDATEACTION.SET;
        setReady(true);
    }

    /**
     * Add other Set sub actions to Set Query
     *
     * @param variableName
     * @param value
     * @return the SetAction
     * @throws InvalidCreateOperationException
     */
    public final SetAction add(final String variableName, final String value)
        throws InvalidCreateOperationException {
        if (currentUPDATEACTION != UPDATEACTION.SET) {
            throw new InvalidCreateOperationException(
                CANNOT_ADD_A_SET_ELEMENT_SINCE_THIS_IS_NOT_A_SET_ACTION + currentUPDATEACTION);
        }
        if (variableName == null || variableName.trim().isEmpty()) {
            throw new InvalidCreateOperationException(
                ACTION + currentUPDATEACTION + CANNOT_BE_UPDATED_WITH_EMPTY_VARIABLE_NAME);
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
     * Add other Set sub actions to Set Query
     *
     * @param variableName
     * @param value
     * @return the SetAction
     * @throws InvalidCreateOperationException
     */
    public final SetAction add(final String variableName, final boolean value)
        throws InvalidCreateOperationException {
        if (currentUPDATEACTION != UPDATEACTION.SET) {
            throw new InvalidCreateOperationException(
                CANNOT_ADD_A_SET_ELEMENT_SINCE_THIS_IS_NOT_A_SET_ACTION + currentUPDATEACTION);
        }
        if (variableName == null || variableName.trim().isEmpty()) {
            throw new InvalidCreateOperationException(
                ACTION + currentUPDATEACTION + CANNOT_BE_UPDATED_WITH_EMPTY_VARIABLE_NAME);
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
     * Add other Set sub actions to Set Query
     *
     * @param variableName
     * @param value
     * @return the SetAction
     * @throws InvalidCreateOperationException
     */
    public final SetAction add(final String variableName, final long value)
        throws InvalidCreateOperationException {
        if (currentUPDATEACTION != UPDATEACTION.SET) {
            throw new InvalidCreateOperationException(
                CANNOT_ADD_A_SET_ELEMENT_SINCE_THIS_IS_NOT_A_SET_ACTION + currentUPDATEACTION);
        }
        if (variableName == null || variableName.trim().isEmpty()) {
            throw new InvalidCreateOperationException(
                ACTION + currentUPDATEACTION + CANNOT_BE_UPDATED_WITH_EMPTY_VARIABLE_NAME);
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
     * Add other Set sub actions to Set Query
     *
     * @param variableName
     * @param value
     * @return the SetAction
     * @throws InvalidCreateOperationException
     */
    public final SetAction add(final String variableName, final double value)
        throws InvalidCreateOperationException {
        if (currentUPDATEACTION != UPDATEACTION.SET) {
            throw new InvalidCreateOperationException(
                CANNOT_ADD_A_SET_ELEMENT_SINCE_THIS_IS_NOT_A_SET_ACTION + currentUPDATEACTION);
        }
        if (variableName == null || variableName.trim().isEmpty()) {
            throw new InvalidCreateOperationException(
                ACTION + currentUPDATEACTION + CANNOT_BE_UPDATED_WITH_EMPTY_VARIABLE_NAME);
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
     * Add other Set sub actions to Set Query
     *
     * @param variableName
     * @param value
     * @return the SetAction
     * @throws InvalidCreateOperationException
     */
    public final SetAction add(final String variableName, final Date value)
        throws InvalidCreateOperationException {
        if (currentUPDATEACTION != UPDATEACTION.SET) {
            throw new InvalidCreateOperationException(
                CANNOT_ADD_A_SET_ELEMENT_SINCE_THIS_IS_NOT_A_SET_ACTION + currentUPDATEACTION);
        }
        if (variableName == null || variableName.trim().isEmpty()) {
            throw new InvalidCreateOperationException(
                ACTION + currentUPDATEACTION + CANNOT_BE_UPDATED_WITH_EMPTY_VARIABLE_NAME);
        }
        try {
            GlobalDatas.sanityParameterCheck(variableName);
        } catch (final InvalidParseOperationException e) {
            throw new InvalidCreateOperationException(e);
        }
        ((ObjectNode) currentObject).set(variableName, GlobalDatas.getDate(value));
        return this;
    }
}
