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
package fr.gouv.vitam.metadata.core.trigger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.json.JsonHandler;

import java.util.List;

public class History {

    protected static final String _HISTORY = "_history";
    protected static final String _V = "_v";
    private static final String UD = "ud";
    private static final String DATA = "data";

    private final List<String> splitPathForHistory;
    private final String namePathForHistory;
    private final long version;
    private final JsonNode nodeToHistory;

    public History(String objectPathForHistory, long version, JsonNode nodeToHistory) {
        this.splitPathForHistory = JsonPath.getSplitPath(objectPathForHistory);
        this.namePathForHistory = JsonPath.getTargetOfPath(objectPathForHistory);
        this.version = version;
        this.nodeToHistory = nodeToHistory;
    }

    public JsonNode getArrayNode() {
        final ArrayNode historyArray = JsonHandler.createArrayNode();
        ObjectNode history = JsonHandler.createObjectNode();
        historyArray.add(history);
        history.put(UD, LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()));
        ObjectNode data = history.putObject(DATA);
        data.put(_V, version);
        if (nodeToHistory.isMissingNode()) {
            createBranch(data);
        } else {
            createBranch(data).set(namePathForHistory, nodeToHistory);
        }

        return historyArray;
    }

    public JsonNode getNode() {
        ObjectNode history = JsonHandler.createObjectNode();
        history.put(UD, LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()));
        ObjectNode data = history.putObject(DATA);
        data.put(_V, version);
        createBranch(data).set(namePathForHistory, nodeToHistory);
        return history;
    }

    private ObjectNode createBranch(ObjectNode root) {
        ObjectNode currentNode = root;
        for (String nodeName : splitPathForHistory) {
            currentNode = currentNode.putObject(nodeName);
        }

        return currentNode;
    }

}
