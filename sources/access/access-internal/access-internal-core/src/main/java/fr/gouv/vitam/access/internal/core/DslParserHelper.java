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
package fr.gouv.vitam.access.internal.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.database.builder.query.action.Action;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken;
import fr.gouv.vitam.common.database.parser.request.multiple.UpdateParserMultiple;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;

import java.util.List;

public class DslParserHelper {

    public static JsonNode getValueForUpdateDsl(JsonNode query, String fieldName)
        throws InvalidParseOperationException {
        UpdateParserMultiple updateParserMultiple = new UpdateParserMultiple();
        updateParserMultiple.parse(query);
        List<Action> actions = updateParserMultiple.getRequest().getActions();

        JsonNode result = null;
        for (Action action : actions) {
            ObjectNode currentAction = action.getCurrentAction();
            JsonNode setAction = currentAction.get(BuilderToken.UPDATEACTION.SET.exactToken());

            JsonNode value = getValueForSetAction(setAction, fieldName);
            if (value != null) {
                // return last non null
                result = value;
            }
        }
        return result;
    }

    private static JsonNode getValueForSetAction(JsonNode setAction, String key) {
        if (setAction == null) {
            return null;
        }

        String[] keyParts = key.split("\\.");
        for (int i = 0; i < keyParts.length; i++) {
            String partialKey = getPartialKeys(keyParts, i);

            JsonNode value = setAction.get(partialKey);
            for (int j = i + 1; j < keyParts.length; j++) {
                value = getSubValue(value, keyParts[j]);
            }
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static JsonNode getSubValue(JsonNode node, String subKey) {
        if (node == null) {
            return null;
        }
        return node.get(subKey);
    }

    private static String getPartialKeys(String[] fieldNameParts, int nbComponents) {
        StringBuilder partialFieldName = new StringBuilder(fieldNameParts[0]);
        for (int i = 1; i <= nbComponents; i++) {
            partialFieldName.append(".").append(fieldNameParts[i]);
        }
        return partialFieldName.toString();
    }
}
