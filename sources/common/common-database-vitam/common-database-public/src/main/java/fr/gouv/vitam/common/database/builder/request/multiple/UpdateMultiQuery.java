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
package fr.gouv.vitam.common.database.builder.request.multiple;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.database.builder.query.action.Action;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.GLOBAL;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.MULTIFILTER;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Update: { $roots: roots, $query : query, $filter : multi, $action : action } or [ roots, query, multi, action ]
 */
public class UpdateMultiQuery extends RequestMultiple {

    protected List<Action> actions = new ArrayList<>();

    /**
     * @return this Update
     */
    public final UpdateMultiQuery resetActions() {
        if (actions != null) {
            actions.forEach(
                new Consumer<Action>() {
                    @Override
                    public void accept(Action t) {
                        t.clean();
                    }
                }
            );
            actions.clear();
        }
        return this;
    }

    /**
     * @return this Update
     */
    @Override
    public final UpdateMultiQuery reset() {
        super.reset();
        resetActions();
        return this;
    }

    /**
     * @param mult True to act on multiple elements, False to act only on 1 element
     * @return this Update
     */
    public final UpdateMultiQuery setMult(final boolean mult) {
        if (filter == null) {
            filter = JsonHandler.createObjectNode();
        }
        filter.put(MULTIFILTER.MULT.exactToken(), mult);
        return this;
    }

    /**
     * @param filterContent json filter
     * @return this Update
     */
    public final UpdateMultiQuery setMult(final JsonNode filterContent) {
        if (filter == null) {
            filter = JsonHandler.createObjectNode();
        }
        if (filterContent.has(MULTIFILTER.MULT.exactToken())) {
            filter.setAll((ObjectNode) filterContent);
        }
        return this;
    }

    /**
     * @param filterContent json filter
     * @return this Update
     * @throws InvalidParseOperationException when query is not valid
     */
    @Override
    public final UpdateMultiQuery setFilter(final JsonNode filterContent) throws InvalidParseOperationException {
        super.setFilter(filterContent);
        return setMult(filterContent);
    }

    /**
     * @param action list
     * @return this Update
     * @throws InvalidCreateOperationException when action is not valid
     */
    public final UpdateMultiQuery addActions(final Action... action) throws InvalidCreateOperationException {
        for (final Action act : action) {
            if (!act.isReady()) {
                throw new InvalidCreateOperationException("Action is not ready to be added: " + act.getCurrentAction());
            }
            actions.add(act);
        }
        return this;
    }

    /**
     * @return the Final Update for update one object (by id) containing only 1 part: actions
     */
    public final ObjectNode getFinalUpdateById() {
        final ObjectNode node = getFinalUpdate();
        node.remove(GLOBAL.QUERY.exactToken());
        node.remove(GLOBAL.ROOTS.exactToken());
        node.remove(GLOBAL.FILTER.exactToken());
        node.remove(GLOBAL.PROJECTION.exactToken());
        return node;
    }

    /**
     * @return the Final Update containing all 4 parts: roots, queries array, filter and actions
     */
    public final ObjectNode getFinalUpdate() {
        final ObjectNode node = getFinal();
        if (actions != null && !actions.isEmpty()) {
            final ArrayNode array = JsonHandler.createArrayNode();
            for (final Action action : actions) {
                array.add(action.getCurrentAction());
            }
            node.set(GLOBAL.ACTION.exactToken(), array);
        } else {
            node.putArray(GLOBAL.ACTION.exactToken());
        }
        if (threshold != null) {
            node.put(GLOBAL.THRESOLD.exactToken(), threshold);
        }

        return node;
    }

    /**
     * @return the actions list
     */
    @Override
    public final List<Action> getActions() {
        return actions;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("UPDATEACTION: ").append(super.toString()).append("\n\tActions: ");
        for (final Action subaction : getActions()) {
            builder.append("\n").append(subaction);
        }
        return builder.toString();
    }
}
