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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.UPDATEACTION;
import fr.gouv.vitam.common.database.builder.request.configuration.GlobalDatas;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;

import java.util.Date;
import java.util.List;

/**
 * Action component
 */
public class Action {

    private static final String ACTION2 = "Action ";
    private static final String CANNOT_BE_CREATED_WITH_EMPTY_VARIABLE_NAME =
        " cannot be created with empty variable name";
    protected ObjectNode currentAction;
    protected JsonNode currentObject;
    protected UPDATEACTION currentUPDATEACTION;
    protected boolean ready;

    /**
     * Empty constructor
     */
    protected Action() {
        currentAction = JsonHandler.createObjectNode();
        currentObject = currentAction;
        currentUPDATEACTION = null;
        ready = false;
    }

    protected final void createActionValueArrayVariable(final UPDATEACTION action, final String variableName)
        throws InvalidCreateOperationException {
        if (variableName == null || variableName.trim().isEmpty()) {
            throw new InvalidCreateOperationException(ACTION2 + action + CANNOT_BE_CREATED_WITH_EMPTY_VARIABLE_NAME);
        }
        try {
            GlobalDatas.sanityParameterCheck(variableName);
        } catch (final InvalidParseOperationException e) {
            throw new InvalidCreateOperationException(e);
        }
        currentObject = ((ObjectNode) currentObject).putObject(action.exactToken()).putArray(variableName.trim());
    }

    protected final void createActionVariables(final UPDATEACTION action, final String... variableNames)
        throws InvalidCreateOperationException {
        final ArrayNode node = ((ObjectNode) currentObject).putArray(action.exactToken());
        for (final String var : variableNames) {
            if (var != null && !var.trim().isEmpty()) {
                try {
                    GlobalDatas.sanityParameterCheck(var);
                } catch (final InvalidParseOperationException e) {
                    throw new InvalidCreateOperationException(e);
                }
                node.add(var.trim());
            }
        }
        currentObject = node;
    }

    protected final void createActionVariableValue(
        final UPDATEACTION action,
        final String variableName,
        final long value
    ) throws InvalidCreateOperationException {
        if (variableName == null || variableName.trim().isEmpty()) {
            throw new InvalidCreateOperationException(ACTION2 + action + CANNOT_BE_CREATED_WITH_EMPTY_VARIABLE_NAME);
        }
        try {
            GlobalDatas.sanityParameterCheck(variableName);
        } catch (final InvalidParseOperationException e) {
            throw new InvalidCreateOperationException(e);
        }
        currentObject = ((ObjectNode) currentObject).putObject(action.exactToken());
        ((ObjectNode) currentObject).put(variableName.trim(), value);
    }

    protected final void createActionVariableValue(
        final UPDATEACTION action,
        final String variableName,
        final double value
    ) throws InvalidCreateOperationException {
        if (variableName == null || variableName.trim().isEmpty()) {
            throw new InvalidCreateOperationException(ACTION2 + action + CANNOT_BE_CREATED_WITH_EMPTY_VARIABLE_NAME);
        }
        try {
            GlobalDatas.sanityParameterCheck(variableName);
        } catch (final InvalidParseOperationException e) {
            throw new InvalidCreateOperationException(e);
        }
        currentObject = ((ObjectNode) currentObject).putObject(action.exactToken());
        ((ObjectNode) currentObject).put(variableName.trim(), value);
    }

    protected final void createActionVariableValue(
        final UPDATEACTION action,
        final String variableName,
        final String value
    ) throws InvalidCreateOperationException {
        if (variableName == null || variableName.trim().isEmpty()) {
            throw new InvalidCreateOperationException(ACTION2 + action + CANNOT_BE_CREATED_WITH_EMPTY_VARIABLE_NAME);
        }
        try {
            GlobalDatas.sanityParameterCheck(variableName);
            GlobalDatas.sanityValueCheck(value);
        } catch (final InvalidParseOperationException e) {
            throw new InvalidCreateOperationException(e);
        }
        currentObject = ((ObjectNode) currentObject).putObject(action.exactToken());
        ((ObjectNode) currentObject).put(variableName.trim(), value);
    }

    protected final void createActionVariableValue(
        final UPDATEACTION action,
        final String variableName,
        final List<?> value
    ) throws InvalidCreateOperationException {
        if (variableName == null || variableName.trim().isEmpty()) {
            throw new InvalidCreateOperationException(ACTION2 + action + CANNOT_BE_CREATED_WITH_EMPTY_VARIABLE_NAME);
        }
        try {
            GlobalDatas.sanityParameterCheck(variableName);
            GlobalDatas.sanityValueCheck(value);
        } catch (final InvalidParseOperationException e) {
            throw new InvalidCreateOperationException(e);
        }
        currentObject = ((ObjectNode) currentObject).putObject(action.exactToken());

        ArrayNode array = JsonHandler.createArrayNode();
        GlobalDatas.setArrayValueFromList(array, value);
        ((ObjectNode) currentObject).set(variableName.trim(), array);
    }

    protected final void createActionVariableValue(
        final UPDATEACTION action,
        final String variableName,
        final boolean value
    ) throws InvalidCreateOperationException {
        if (variableName == null || variableName.trim().isEmpty()) {
            throw new InvalidCreateOperationException(ACTION2 + action + CANNOT_BE_CREATED_WITH_EMPTY_VARIABLE_NAME);
        }
        try {
            GlobalDatas.sanityParameterCheck(variableName);
        } catch (final InvalidParseOperationException e) {
            throw new InvalidCreateOperationException(e);
        }
        currentObject = ((ObjectNode) currentObject).putObject(action.exactToken());
        ((ObjectNode) currentObject).put(variableName.trim(), value);
    }

    protected final void createActionVariableValue(
        final UPDATEACTION action,
        final String variableName,
        final Date value
    ) throws InvalidCreateOperationException {
        if (variableName == null || variableName.trim().isEmpty()) {
            throw new InvalidCreateOperationException(ACTION2 + action + CANNOT_BE_CREATED_WITH_EMPTY_VARIABLE_NAME);
        }
        try {
            GlobalDatas.sanityParameterCheck(variableName);
        } catch (final InvalidParseOperationException e) {
            throw new InvalidCreateOperationException(e);
        }
        currentObject = ((ObjectNode) currentObject).putObject(action.exactToken());
        ((ObjectNode) currentObject).set(variableName.trim(), GlobalDatas.getDate(value));
    }

    /**
     * Clean the object
     */
    public void clean() {
        currentAction.removeAll();
        currentObject = currentAction;
        currentUPDATEACTION = null;
        ready = false;
    }

    /**
     * @return the currentAction
     */
    public ObjectNode getCurrentAction() {
        return currentAction;
    }

    /**
     * @return the currentObject
     */
    public JsonNode getCurrentObject() {
        return currentObject;
    }

    /**
     * @return the current UPDATEACTION
     */
    public UPDATEACTION getUPDATEACTION() {
        return currentUPDATEACTION;
    }

    /**
     * @return the ready
     */
    public boolean isReady() {
        return ready;
    }

    /**
     * @param ready the ready to set
     */
    protected void setReady(final boolean ready) {
        this.ready = ready;
    }

    @Override
    public String toString() {
        return JsonHandler.unprettyPrint(currentAction);
    }
}
