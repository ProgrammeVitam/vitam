/*******************************************************************************
 * This file is part of Vitam Project.
 * 
 * Copyright Vitam (2012, 2015)
 *
 * This software is governed by the CeCILL 2.1 license under French law and
 * abiding by the rules of distribution of free software. You can use, modify
 * and/ or redistribute the software under the terms of the CeCILL license as
 * circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify
 * and redistribute granted by the license, users are provided only with a
 * limited warranty and the software's author, the holder of the economic
 * rights, and the successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with
 * loading, using, modifying and/or developing or reproducing the software by
 * the user in light of its specific status of free software, that may mean that
 * it is complicated to manipulate, and that also therefore means that it is
 * reserved for developers and experienced professionals having in-depth
 * computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling
 * the security of their systems and/or data to be ensured and, more generally,
 * to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 *******************************************************************************/
package fr.gouv.vitam.builder.request.construct.action;

import java.util.Date;

import com.fasterxml.jackson.databind.node.ArrayNode;

import fr.gouv.vitam.builder.request.construct.configuration.GlobalDatas;
import fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.UPDATEACTION;
import fr.gouv.vitam.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;

/**
 * Pull Action: $pull : { name : { $each : [ value, value, ... ] } }
 *
 */
public class PullAction extends Action {
    protected PullAction() {
        super();
    }

    /**
     * Pull Action constructor
     *
     * @param variableName
     * @param value
     * @throws InvalidCreateOperationException
     */
    public PullAction(final String variableName, final String... value)
            throws InvalidCreateOperationException {
        super();
        createActionVariableEach(UPDATEACTION.PULL, variableName);
        for (final String val : value) {
            if (val != null && !val.trim().isEmpty()) {
                try {
                    GlobalDatas.sanityValueCheck(val);
                } catch (InvalidParseOperationException e) {
                    throw new InvalidCreateOperationException(e);
                }
                ((ArrayNode) currentObject).add(val.trim());
            }
        }
        currentUPDATEACTION = UPDATEACTION.PULL;
        setReady(true);
    }

    /**
     * Pull Action constructor
     *
     * @param variableName
     * @param value
     * @throws InvalidCreateOperationException
     */
    public PullAction(final String variableName, final long... value)
            throws InvalidCreateOperationException {
        super();
        createActionVariableEach(UPDATEACTION.PULL, variableName);
        for (final long val : value) {
            ((ArrayNode) currentObject).add(val);
        }
        currentUPDATEACTION = UPDATEACTION.PULL;
        setReady(true);
    }

    /**
     * Pull Action constructor
     *
     * @param variableName
     * @param value
     * @throws InvalidCreateOperationException
     */
    public PullAction(final String variableName, final boolean... value)
            throws InvalidCreateOperationException {
        super();
        createActionVariableEach(UPDATEACTION.PULL, variableName);
        for (final boolean val : value) {
            ((ArrayNode) currentObject).add(val);
        }
        currentUPDATEACTION = UPDATEACTION.PULL;
        setReady(true);
    }

    /**
     * Pull Action constructor
     *
     * @param variableName
     * @param value
     * @throws InvalidCreateOperationException
     */
    public PullAction(final String variableName, final double... value)
            throws InvalidCreateOperationException {
        super();
        createActionVariableEach(UPDATEACTION.PULL, variableName);
        for (final double val : value) {
            ((ArrayNode) currentObject).add(val);
        }
        currentUPDATEACTION = UPDATEACTION.PULL;
        setReady(true);
    }

    /**
     * Pull Action constructor
     *
     * @param variableName
     * @param value
     * @throws InvalidCreateOperationException
     */
    public PullAction(final String variableName, final Date... value)
            throws InvalidCreateOperationException {
        super();
        createActionVariableEach(UPDATEACTION.PULL, variableName);
        for (final Date val : value) {
            ((ArrayNode) currentObject).add(GlobalDatas.getDate(val));
        }
        currentUPDATEACTION = UPDATEACTION.PULL;
        setReady(true);
    }

    /**
     * Add other Pull sub actions to Pull Query
     *
     * @param value
     * @return the PullAction
     * @throws InvalidCreateOperationException
     */
    public final PullAction add(final String... value)
            throws InvalidCreateOperationException {
        if (currentUPDATEACTION != UPDATEACTION.PULL) {
            throw new InvalidCreateOperationException(
                    "Cannot add a set element since this is not a Pull Action: "
                            + currentUPDATEACTION);
        }
        for (final String val : value) {
            if (val != null && !val.trim().isEmpty()) {
                try {
                    GlobalDatas.sanityValueCheck(val);
                } catch (InvalidParseOperationException e) {
                    throw new InvalidCreateOperationException(e);
                }
                ((ArrayNode) currentObject).add(val.trim());
            }
        }
        return this;
    }

    /**
     * Add other Pull sub actions to Pull Query
     *
     * @param value
     * @return the PullAction
     * @throws InvalidCreateOperationException
     */
    public final PullAction add(final boolean... value)
            throws InvalidCreateOperationException {
        if (currentUPDATEACTION != UPDATEACTION.PULL) {
            throw new InvalidCreateOperationException(
                    "Cannot add a set element since this is not a Pull Action: "
                            + currentUPDATEACTION);
        }
        for (final boolean val : value) {
            ((ArrayNode) currentObject).add(val);
        }
        return this;
    }

    /**
     * Add other Pull sub actions to Pull Query
     *
     * @param value
     * @return the PullAction
     * @throws InvalidCreateOperationException
     */
    public final PullAction add(final long... value)
            throws InvalidCreateOperationException {
        if (currentUPDATEACTION != UPDATEACTION.PULL) {
            throw new InvalidCreateOperationException(
                    "Cannot add a set element since this is not a Pull Action: "
                            + currentUPDATEACTION);
        }
        for (final long val : value) {
            ((ArrayNode) currentObject).add(val);
        }
        return this;
    }

    /**
     * Add other Pull sub actions to Pull Query
     *
     * @param value
     * @return the PullAction
     * @throws InvalidCreateOperationException
     */
    public final PullAction add(final double... value)
            throws InvalidCreateOperationException {
        if (currentUPDATEACTION != UPDATEACTION.PULL) {
            throw new InvalidCreateOperationException(
                    "Cannot add a set element since this is not a Pull Action: "
                            + currentUPDATEACTION);
        }
        for (final double val : value) {
            ((ArrayNode) currentObject).add(val);
        }
        return this;
    }

    /**
     * Add other Pull sub actions to Pull Query
     *
     * @param value
     * @return the PullAction
     * @throws InvalidCreateOperationException
     */
    public final PullAction add(final Date... value)
            throws InvalidCreateOperationException {
        if (currentUPDATEACTION != UPDATEACTION.PULL) {
            throw new InvalidCreateOperationException(
                    "Cannot add a set element since this is not a Pull Action: "
                            + currentUPDATEACTION);
        }
        for (final Date val : value) {
            ((ArrayNode) currentObject).add(GlobalDatas.getDate(val));
        }
        return this;
    }

}
