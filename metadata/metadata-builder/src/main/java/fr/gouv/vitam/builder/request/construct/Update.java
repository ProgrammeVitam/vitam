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
package fr.gouv.vitam.builder.request.construct;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.gouv.vitam.builder.request.construct.action.Action;
import fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.GLOBAL;
import fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.MULTIFILTER;
import fr.gouv.vitam.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;

/**
 * Update: { $roots: roots, $query : query, $filter : multi, $action : action }
 * or [ roots, query, multi, action ]
 *
 */
public class Update extends Request {
    protected List<Action> actions = new ArrayList<Action>();

    /**
     *
     * @return this Update
     */
    public final Update resetActions() {
        if (actions != null) {
            actions.forEach(new Consumer<Action>() {
                @Override
                public void accept(Action t) {
                    t.clean();
                }
            });
            actions.clear();
        }
        return this;
    }

    /**
     * @return this Update
     */
    public final Update reset() {
        super.reset();
        resetActions();
        return this;
    }

    /**
     * @param mult
     *            True to act on multiple elements, False to act only on 1
     *            element
     * @return this Update
     */
    public final Update setMult(final boolean mult) {
        if (filter == null) {
            filter = JsonHandler.createObjectNode();
        }
        filter.put(MULTIFILTER.mult.exactToken(), mult);
        return this;
    }

    /**
     * @param filterContent
     * @return this Update
     */
    public final Update setMult(final JsonNode filterContent) {
        if (filter == null) {
            filter = JsonHandler.createObjectNode();
        }
        if (filterContent.has(MULTIFILTER.mult.exactToken())) {
            filter.setAll((ObjectNode) filterContent);
        }
        return this;
    }

    /**
     * 
     * @param filterContent
     * @return this Update
     * @throws InvalidParseOperationException
     */
    public final Update setFilter(final JsonNode filterContent)
            throws InvalidParseOperationException {
        super.setFilter(filterContent);
        return setMult(filterContent);
    }

    /**
     *
     * @param action
     * @return this Update
     * @throws InvalidCreateOperationException
     */
    public final Update addActions(final Action... action)
            throws InvalidCreateOperationException {
        for (final Action act : action) {
            if (!act.isReady()) {
                throw new InvalidCreateOperationException(
                        "Action is not ready to be added: " + act.getCurrentAction());
            }
            actions.add(act);
        }
        return this;
    }

    /**
     *
     * @return the Final Update containing all 4 parts: roots, queries array,
     *         filter and actions
     */
    public final ObjectNode getFinalUpdate() {
        final ObjectNode node = getFinal();
        if (actions != null && actions.size() > 0) {
            ArrayNode array = JsonHandler.createArrayNode();
            for (Action action : actions) {
                array.add(action.getCurrentAction());
            }
            node.set(GLOBAL.action.exactToken(), array);
        } else {
            node.putArray(GLOBAL.action.exactToken());
        }
        return node;
    }

    /**
     * @return the actions list
     */
    public final List<Action> getActions() {
        return actions;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("UPDATEACTION: ").append(super.toString())
                .append("\n\tActions: ");
        for (Action subaction : getActions()) {
            builder.append("\n").append(subaction);
        }
        return builder.toString();
    }

}
