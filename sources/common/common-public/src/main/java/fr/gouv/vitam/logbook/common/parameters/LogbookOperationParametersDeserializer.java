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
package fr.gouv.vitam.logbook.common.parameters;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * AbstractParameters Deserializer for Jackson
 */
class LogbookOperationParametersDeserializer extends JsonDeserializer<AbstractParameters> {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(
        LogbookOperationParametersDeserializer.class
    );

    /**
     * Empty constructor
     */
    public LogbookOperationParametersDeserializer() {
        // empty
    }

    @Override
    public AbstractParameters deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        final LogbookOperationParameters logbookOperationParams =
            LogbookParametersFactory.newLogbookOperationParameters();
        try {
            ObjectNode objNode = (ObjectNode) p.readValueAsTree();
            Iterator<Map.Entry<String, JsonNode>> iter = objNode.fields();
            ArrayNode eventList = null;
            final Map<String, String> parameters = new HashMap<>();
            while (iter.hasNext()) {
                Map.Entry<String, JsonNode> entry = iter.next();
                if (LogbookParameterName.events.name().equals(entry.getKey())) {
                    eventList = (ArrayNode) entry.getValue();
                } else if (entry.getValue().isTextual()) {
                    parameters.put(entry.getKey(), entry.getValue().asText());
                }
            }
            logbookOperationParams.setMap(parameters);
            Set<LogbookParameters> events = new LinkedHashSet<>();
            if (eventList != null) {
                for (JsonNode event : eventList) {
                    events.add(JsonHandler.getFromJsonNode(event, LogbookOperationParameters.class));
                }
            }
            logbookOperationParams.setEvents(events);
        } catch (InvalidParseOperationException e) {
            LOGGER.error(e);
        }
        return logbookOperationParams;
    }
}
