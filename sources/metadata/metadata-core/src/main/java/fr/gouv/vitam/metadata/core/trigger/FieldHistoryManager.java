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
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import org.apache.commons.lang3.StringUtils;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

public class FieldHistoryManager {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(FieldHistoryManager.class);

    private final Map<String, ChangesHistory> triggers = new HashMap<>();

    public FieldHistoryManager(String fileNameTriggersConfig) {
        try {
            for (HistoryTriggerConfig config : JsonHandler.getFromInputStream(
                PropertiesUtils.getResourceAsStream(fileNameTriggersConfig),
                HistoryTriggerConfig[].class
            )) {
                if (triggers.get(config.getFieldPathTriggeredForHistory()) == null) {
                    triggers.put(
                        config.getFieldPathTriggeredForHistory(),
                        new ChangesHistory(config.getObjectPathForHistory())
                    );
                }
            }
        } catch (InvalidParseOperationException | FileNotFoundException e) {
            LOGGER.error(e);
            throw new ChangesTriggerConfigFileException(e);
        }
    }

    public void trigger(JsonNode unitBeforeChanges, JsonNode unitAfterChanges) {
        for (Map.Entry<String, ChangesHistory> trigger : triggers.entrySet()) {
            if (isUpdatedFieldForHistory(trigger.getKey(), unitBeforeChanges, unitAfterChanges)) {
                trigger.getValue().addHistory(unitBeforeChanges, unitAfterChanges);
            }
        }
    }

    private boolean isUpdatedFieldForHistory(
        String fieldPathForHistory,
        JsonNode unitBeforeUpdate,
        JsonNode unitAfterUpdate
    ) {
        String valueBeforeUpdate = getValue(fieldPathForHistory, unitBeforeUpdate);
        String valueAfterUpdate = getValue(fieldPathForHistory, unitAfterUpdate);

        return !StringUtils.equals(valueBeforeUpdate, valueAfterUpdate);
    }

    private String getValue(String fieldForHistorical, JsonNode unit) {
        JsonNode node = JsonHandler.findNode(unit, fieldForHistorical);

        if (node.isMissingNode()) {
            return null;
        }

        return node.asText();
    }
}
