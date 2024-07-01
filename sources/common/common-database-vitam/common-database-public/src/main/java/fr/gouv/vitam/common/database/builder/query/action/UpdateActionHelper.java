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

import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Update associated actions helper
 */
public class UpdateActionHelper {

    protected UpdateActionHelper() {
        // empty
    }

    /**
     * @param variableName key name
     * @param value key value
     * @return an AddAction
     * @throws InvalidCreateOperationException when query is invalid
     */
    public static final AddAction add(final String variableName, final String... value)
        throws InvalidCreateOperationException {
        return new AddAction(variableName, value);
    }

    /**
     * @param variableName key name
     * @param value key value
     * @return an AddAction
     * @throws InvalidCreateOperationException when query is invalid
     */
    public static final AddAction add(final String variableName, final boolean... value)
        throws InvalidCreateOperationException {
        return new AddAction(variableName, value);
    }

    /**
     * @param variableName key name
     * @param value key value
     * @return an AddAction
     * @throws InvalidCreateOperationException when query is invalid
     */
    public static final AddAction add(final String variableName, final long... value)
        throws InvalidCreateOperationException {
        return new AddAction(variableName, value);
    }

    /**
     * @param variableName key name
     * @param value key value
     * @return an AddAction
     * @throws InvalidCreateOperationException when query is invalid
     */
    public static final AddAction add(final String variableName, final double... value)
        throws InvalidCreateOperationException {
        return new AddAction(variableName, value);
    }

    /**
     * @param variableName key name
     * @param value key value
     * @return an AddAction
     * @throws InvalidCreateOperationException when query is invalid
     */
    public static final AddAction add(final String variableName, final Date... value)
        throws InvalidCreateOperationException {
        return new AddAction(variableName, value);
    }

    /**
     * @param variableName key name
     * @param value key value
     * @return an IncAction
     * @throws InvalidCreateOperationException when query is invalid
     */
    public static final IncAction inc(final String variableName, final long value)
        throws InvalidCreateOperationException {
        return new IncAction(variableName, value);
    }

    /**
     * @param variableName key name
     * @return an IncAction using default value 1
     * @throws InvalidCreateOperationException when query is invalid
     */
    public static final IncAction inc(final String variableName) throws InvalidCreateOperationException {
        return new IncAction(variableName);
    }

    /**
     * @param variableName key name
     * @param value key value
     * @return an MinAction
     * @throws InvalidCreateOperationException when query is invalid
     */
    public static final MinAction min(final String variableName, final long value)
        throws InvalidCreateOperationException {
        return new MinAction(variableName, value);
    }

    /**
     * @param variableName key name
     * @param value key value
     * @return an MinAction
     * @throws InvalidCreateOperationException when query is invalid
     */
    public static final MinAction min(final String variableName, final double value)
        throws InvalidCreateOperationException {
        return new MinAction(variableName, value);
    }

    /**
     * @param variableName key name
     * @param value key value
     * @return an MinAction
     * @throws InvalidCreateOperationException when query is invalid
     */
    public static final MinAction min(final String variableName, final boolean value)
        throws InvalidCreateOperationException {
        return new MinAction(variableName, value);
    }

    /**
     * @param variableName key name
     * @param value key value
     * @return an MinAction
     * @throws InvalidCreateOperationException when query is invalid
     */
    public static final MinAction min(final String variableName, final String value)
        throws InvalidCreateOperationException {
        return new MinAction(variableName, value);
    }

    /**
     * @param variableName key name
     * @param value key value
     * @return an MinAction
     * @throws InvalidCreateOperationException when query is invalid
     */
    public static final MinAction min(final String variableName, final Date value)
        throws InvalidCreateOperationException {
        return new MinAction(variableName, value);
    }

    /**
     * @param variableName key name
     * @param value key value
     * @return an MaxAction
     * @throws InvalidCreateOperationException when query is invalid
     */
    public static final MaxAction max(final String variableName, final long value)
        throws InvalidCreateOperationException {
        return new MaxAction(variableName, value);
    }

    /**
     * @param variableName key name
     * @param value key value
     * @return an MaxAction
     * @throws InvalidCreateOperationException when query is invalid
     */
    public static final MaxAction max(final String variableName, final double value)
        throws InvalidCreateOperationException {
        return new MaxAction(variableName, value);
    }

    /**
     * @param variableName key name
     * @param value key value
     * @return an MaxAction
     * @throws InvalidCreateOperationException when query is invalid
     */
    public static final MaxAction max(final String variableName, final boolean value)
        throws InvalidCreateOperationException {
        return new MaxAction(variableName, value);
    }

    /**
     * @param variableName key name
     * @param value key value
     * @return an MaxAction
     * @throws InvalidCreateOperationException when query is invalid
     */
    public static final MaxAction max(final String variableName, final String value)
        throws InvalidCreateOperationException {
        return new MaxAction(variableName, value);
    }

    /**
     * @param variableName key name
     * @param value key value
     * @return an MaxAction
     * @throws InvalidCreateOperationException when query is invalid
     */
    public static final MaxAction max(final String variableName, final Date value)
        throws InvalidCreateOperationException {
        return new MaxAction(variableName, value);
    }

    /**
     * @param variableName key name
     * @param value key value
     * @return a PullAction
     * @throws InvalidCreateOperationException when query is invalid
     */
    public static final PullAction pull(final String variableName, final String... value)
        throws InvalidCreateOperationException {
        return new PullAction(variableName, value);
    }

    /**
     * @param variableName key name
     * @param value key value
     * @return a PullAction
     * @throws InvalidCreateOperationException when query is invalid
     */
    public static final PullAction pull(final String variableName, final boolean... value)
        throws InvalidCreateOperationException {
        return new PullAction(variableName, value);
    }

    /**
     * @param variableName key name
     * @param value key value
     * @return a PullAction
     * @throws InvalidCreateOperationException when query is invalid
     */
    public static final PullAction pull(final String variableName, final long... value)
        throws InvalidCreateOperationException {
        return new PullAction(variableName, value);
    }

    /**
     * @param variableName key name
     * @param value key value
     * @return a PullAction
     * @throws InvalidCreateOperationException when query is invalid
     */
    public static final PullAction pull(final String variableName, final double... value)
        throws InvalidCreateOperationException {
        return new PullAction(variableName, value);
    }

    /**
     * @param variableName key name
     * @param value key value
     * @return a PullAction
     * @throws InvalidCreateOperationException when query is invalid
     */
    public static final PullAction pull(final String variableName, final Date... value)
        throws InvalidCreateOperationException {
        return new PullAction(variableName, value);
    }

    /**
     * @param variableName key name
     * @param way -1 for first, 1 for last
     * @return a PopAction
     * @throws InvalidCreateOperationException when query is invalid
     */
    public static final PopAction pop(final String variableName, final int way) throws InvalidCreateOperationException {
        return new PopAction(variableName, way);
    }

    /**
     * @param variableName key name
     * @return a PopAction with default last position
     * @throws InvalidCreateOperationException when query is invalid
     */
    public static final PopAction pop(final String variableName) throws InvalidCreateOperationException {
        return new PopAction(variableName, 1);
    }

    /**
     * @param variableName key name
     * @param value key value
     * @return a PushAction
     * @throws InvalidCreateOperationException when query is invalid
     */
    public static final PushAction push(final String variableName, final String... value)
        throws InvalidCreateOperationException {
        return new PushAction(variableName, value);
    }

    /**
     * @param variableName key name
     * @param value key value
     * @return a PushAction
     * @throws InvalidCreateOperationException when query is invalid
     */
    public static final PushAction push(final String variableName, final boolean... value)
        throws InvalidCreateOperationException {
        return new PushAction(variableName, value);
    }

    /**
     * @param variableName key name
     * @param value key value
     * @return a PushAction
     * @throws InvalidCreateOperationException when query is invalid
     */
    public static final PushAction push(final String variableName, final long... value)
        throws InvalidCreateOperationException {
        return new PushAction(variableName, value);
    }

    /**
     * @param variableName key name
     * @param value key value
     * @return a PushAction
     * @throws InvalidCreateOperationException when query is invalid
     */
    public static final PushAction push(final String variableName, final double... value)
        throws InvalidCreateOperationException {
        return new PushAction(variableName, value);
    }

    /**
     * @param variableName key name
     * @param value key value
     * @return a PushAction
     * @throws InvalidCreateOperationException when query is invalid
     */
    public static final PushAction push(final String variableName, final Date... value)
        throws InvalidCreateOperationException {
        return new PushAction(variableName, value);
    }

    /**
     * @param variableName key name
     * @param newName new key name
     * @return a RenameAction
     * @throws InvalidCreateOperationException when query is invalid
     */
    public static final RenameAction rename(final String variableName, final String newName)
        throws InvalidCreateOperationException {
        return new RenameAction(variableName, newName);
    }

    /**
     * @param variableName key name
     * @param value key value
     * @return a SetAction
     * @throws InvalidCreateOperationException when query is invalid
     */
    public static final SetAction set(final String variableName, final String value)
        throws InvalidCreateOperationException {
        return new SetAction(variableName, value);
    }

    /**
     * @param variableName key name
     * @param value key value as List of values
     * @return a SetAction
     * @throws InvalidCreateOperationException when query is invalid
     */
    public static final SetAction set(final String variableName, final List<?> value)
        throws InvalidCreateOperationException {
        return new SetAction(variableName, value);
    }

    /**
     * @param variableName key name
     * @param value key value
     * @return a SetAction
     * @throws InvalidCreateOperationException when query is invalid
     */
    public static final SetAction set(final String variableName, final boolean value)
        throws InvalidCreateOperationException {
        return new SetAction(variableName, value);
    }

    /**
     * @param variableName key name
     * @param value key value
     * @return a SetAction
     * @throws InvalidCreateOperationException when query is invalid
     */
    public static final SetAction set(final String variableName, final long value)
        throws InvalidCreateOperationException {
        return new SetAction(variableName, value);
    }

    /**
     * @param variableName key name
     * @param value key value
     * @return a SetAction
     * @throws InvalidCreateOperationException when query is invalid
     */
    public static final SetAction set(final String variableName, final double value)
        throws InvalidCreateOperationException {
        return new SetAction(variableName, value);
    }

    /**
     * @param variableName key name
     * @param value key value
     * @return a SetAction
     * @throws InvalidCreateOperationException when query is invalid
     */
    public static final SetAction set(final String variableName, final Date value)
        throws InvalidCreateOperationException {
        return new SetAction(variableName, value);
    }

    /**
     * @param map map of variableName for values
     * @return a SectAction
     * @throws InvalidCreateOperationException when query is invalid
     */
    public static final SetAction set(final Map<String, ?> map) throws InvalidCreateOperationException {
        return new SetAction(map);
    }

    /**
     * @param node of variableName and values
     * @return a SetAction
     * @throws InvalidCreateOperationException when query is invalid
     */
    public static final SetAction set(final ObjectNode node) throws InvalidCreateOperationException {
        return new SetAction(node);
    }

    /**
     * @param variableName key name
     * @return an UnsetAction
     * @throws InvalidCreateOperationException when query is invalid
     */
    public static final UnsetAction unset(final String... variableName) throws InvalidCreateOperationException {
        return new UnsetAction(variableName);
    }
}
