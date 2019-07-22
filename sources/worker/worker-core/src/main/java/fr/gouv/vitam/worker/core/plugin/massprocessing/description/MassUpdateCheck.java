/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.worker.core.plugin.massprocessing.description;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.utils.PluginHelper.EventDetails;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static fr.gouv.vitam.common.model.StatusCode.KO;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;
import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatusWithMessage;

public class MassUpdateCheck extends ActionHandler {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(MassUpdateCheck.class);
    private static final String PLUGIN_NAME = "MASS_UPDATE_CHECK";
    private static final String FORBIDDEN_PREFIX = "#";
    private static final String FORBIDDEN_PREFIX_INTERNAL = "_";

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler) throws ProcessingException {
        try {
            List<String> internalKeyFields = getInternalActionKeyFields(handler.getJsonFromWorkspace("query.json"));
            if (!internalKeyFields.isEmpty()) {
                String message = String.format("Invalid DSL query: cannot contains '%s' internal field(s).", String.join(", ", internalKeyFields));
                return buildItemStatusWithMessage(PLUGIN_NAME, KO, message);
            }

            return buildItemStatus(PLUGIN_NAME, OK, EventDetails.of("Check OK."));
        } catch (Exception e) {
            LOGGER.error(e);
            return buildItemStatus(PLUGIN_NAME, KO, EventDetails.of("Check KO unexpected error."));
        }
    }

    private List<String> getInternalActionKeyFields(JsonNode query) {
        JsonNode action = query.get("$action");
        if (action == null || action.isMissingNode() || action.isNull()) {
            return Collections.emptyList();
        }
        return getInternalKeyFields(action);
    }

    private List<String> getInternalKeyFields(JsonNode action) {
        List<String> internalKeyFields = new ArrayList<>();

        if (action.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = action.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                JsonNode value = field.getValue();
                if (value.isObject() || value.isArray()) {
                    return getInternalKeyFields(value);
                }

                String key = field.getKey().toLowerCase();
                addInternalFieldFromSetRegex(internalKeyFields, value, key);

                if (key.startsWith(FORBIDDEN_PREFIX) || key.startsWith(FORBIDDEN_PREFIX_INTERNAL)) {
                    internalKeyFields.add(key);
                }
            }
        }
        if (action.isArray()) {
            Iterator<JsonNode> elements = action.elements();
            while (elements.hasNext()) {
                JsonNode element = elements.next();
                if (element.isObject() || element.isArray()) {
                    return getInternalKeyFields(element);
                }
            }
        }

        return internalKeyFields;
    }

    private void addInternalFieldFromSetRegex(List<String> internalKeyFields, JsonNode value, String key) {
        // When we use '$setregex' DSL operator we must check if a forbidden field is use, so we check the value of '$target'.
        if (key.equalsIgnoreCase("$target") && (value.asText().startsWith(FORBIDDEN_PREFIX) || value.asText().startsWith(FORBIDDEN_PREFIX_INTERNAL))) {
            internalKeyFields.add(value.asText());
        }
    }
}
