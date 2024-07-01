/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common.database.parser.request.multiple;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import fr.gouv.vitam.common.database.builder.query.action.Action;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.GLOBAL;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.UPDATEACTION;
import fr.gouv.vitam.common.database.builder.request.configuration.GlobalDatas;
import fr.gouv.vitam.common.database.builder.request.multiple.RequestMultiple;
import fr.gouv.vitam.common.database.builder.request.multiple.UpdateMultiQuery;
import fr.gouv.vitam.common.database.parser.request.GlobalDatasParser;
import fr.gouv.vitam.common.database.parser.request.adapter.VarNameAdapter;
import fr.gouv.vitam.common.database.parser.request.adapter.VarNameUpdateAdapter;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;

import java.util.Iterator;
import java.util.Map.Entry;

import static fr.gouv.vitam.common.database.parser.query.action.UpdateActionParserHelper.add;
import static fr.gouv.vitam.common.database.parser.query.action.UpdateActionParserHelper.inc;
import static fr.gouv.vitam.common.database.parser.query.action.UpdateActionParserHelper.max;
import static fr.gouv.vitam.common.database.parser.query.action.UpdateActionParserHelper.min;
import static fr.gouv.vitam.common.database.parser.query.action.UpdateActionParserHelper.pop;
import static fr.gouv.vitam.common.database.parser.query.action.UpdateActionParserHelper.pull;
import static fr.gouv.vitam.common.database.parser.query.action.UpdateActionParserHelper.push;
import static fr.gouv.vitam.common.database.parser.query.action.UpdateActionParserHelper.rename;
import static fr.gouv.vitam.common.database.parser.query.action.UpdateActionParserHelper.set;
import static fr.gouv.vitam.common.database.parser.query.action.UpdateActionParserHelper.setregex;
import static fr.gouv.vitam.common.database.parser.query.action.UpdateActionParserHelper.unset;

/**
 * Update Parser: { $roots: root, $query : query, $filter : filter, $action : action }
 */
public class UpdateParserMultiple extends RequestParserMultiple {

    VarNameUpdateAdapter updateAdapter;

    /**
     * Empty constructor
     */
    public UpdateParserMultiple() {
        super();
        updateAdapter = new VarNameUpdateAdapter(adapter);
    }

    /**
     * @param adapter VarNameAdapter
     */
    public UpdateParserMultiple(VarNameAdapter adapter) {
        super(adapter);
        updateAdapter = new VarNameUpdateAdapter(adapter);
    }

    @Override
    protected RequestMultiple getNewRequest() {
        return new UpdateMultiQuery();
    }

    /**
     * @param request containing a parsed JSON as { $roots: root, $query : query, $filter : filter, $action : action }
     * @throws InvalidParseOperationException if request could not parse to JSON
     */
    @Override
    public void parse(final JsonNode request) throws InvalidParseOperationException {
        parseJson(request);
        internalParseUpdate();
    }

    /**
     * @throws InvalidParseOperationException if could not parse to JSON
     */
    private void internalParseUpdate() throws InvalidParseOperationException {
        // { $roots: root, $query : query, $filter : filter, $action : action }
        actionParse(rootNode.get(GLOBAL.ACTION.exactToken()));
        thresholdParse(rootNode.get(GLOBAL.THRESOLD.exactToken()));
    }

    /**
     * {$"action" : args, ...}
     *
     * @param rootNode JsonNode
     * @throws InvalidParseOperationException if rootNode could not parse to JSON
     */
    protected void actionParse(final JsonNode rootNode) throws InvalidParseOperationException {
        if (rootNode == null) {
            return;
        }
        GlobalDatas.sanityParametersCheck(rootNode.toString(), GlobalDatasParser.NB_ACTIONS);
        try {
            for (final JsonNode node : (ArrayNode) rootNode) {
                Iterator<Entry<String, JsonNode>> iterator = node.fields();
                while (iterator.hasNext()) {
                    final Entry<String, JsonNode> entry = iterator.next();
                    final Action updateAction = analyseOneAction(entry.getKey(), entry.getValue());
                    ((UpdateMultiQuery) request).addActions(updateAction);
                }
                iterator = null;
            }
        } catch (final Exception e) {
            throw new InvalidParseOperationException("Parse in error for Action: " + rootNode, e);
        }
    }

    /**
     * {$"threshold" : arg}
     *
     * @param rootNode JsonNode
     * @throws InvalidParseOperationException if rootNode could not parse to JSON
     */
    protected void thresholdParse(final JsonNode rootNode) throws InvalidParseOperationException {
        if (rootNode == null) {
            return;
        }

        try {
            ((UpdateMultiQuery) request).setThreshold(rootNode.asLong());
        } catch (final Exception e) {
            throw new InvalidParseOperationException("Parse in error for Action: " + rootNode, e);
        }
    }

    protected void setregexParse(final JsonNode rootNode) throws InvalidParseOperationException {
        if (rootNode == null) {
            return;
        }

        try {
            ((UpdateMultiQuery) request).setThreshold(rootNode.asLong());
        } catch (final Exception e) {
            throw new InvalidParseOperationException("Parse in error for Action: " + rootNode, e);
        }
    }

    /**
     * Compute the QUERY from command
     *
     * @param actionroot String
     * @return the QUERY
     * @throws InvalidParseOperationException if actionroot could not parse to JSON
     */
    protected static final UPDATEACTION getUpdateActionId(final String actionroot)
        throws InvalidParseOperationException {
        if (!actionroot.startsWith(BuilderToken.DEFAULT_PREFIX)) {
            throw new InvalidParseOperationException("Incorrect action $command: " + actionroot);
        }
        final String command = actionroot.substring(1).toUpperCase();
        UPDATEACTION action = null;
        try {
            action = UPDATEACTION.valueOf(command);
        } catch (final IllegalArgumentException e) {
            throw new InvalidParseOperationException("Invalid action command: " + command, e);
        }
        return action;
    }

    protected Action analyseOneAction(final String refCommand, final JsonNode command)
        throws InvalidParseOperationException {
        GlobalDatas.sanityValueCheck(command.toString());
        final UPDATEACTION action = getUpdateActionId(refCommand);
        switch (action) {
            case ADD:
                return add(command, updateAdapter);
            case INC:
                return inc(command, updateAdapter);
            case MIN:
                return min(command, updateAdapter);
            case MAX:
                return max(command, updateAdapter);
            case POP:
                return pop(command, updateAdapter);
            case PULL:
                return pull(command, updateAdapter);
            case PUSH:
                return push(command, updateAdapter);
            case RENAME:
                return rename(command, updateAdapter);
            case SET:
                return set(command, updateAdapter);
            case UNSET:
                return unset(command, updateAdapter);
            case SETREGEX:
                return setregex(command, updateAdapter);
            default:
                throw new InvalidParseOperationException("Invalid command: " + refCommand);
        }
    }

    @Override
    public String toString() {
        return new StringBuilder().append(request.toString()).append("\n\tLastLevel: ").append(lastDepth).toString();
    }

    @Override
    public UpdateMultiQuery getRequest() {
        return (UpdateMultiQuery) request;
    }
}
