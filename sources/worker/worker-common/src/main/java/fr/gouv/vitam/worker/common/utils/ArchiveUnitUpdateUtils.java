/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
package fr.gouv.vitam.worker.common.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.builder.query.action.SetAction;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.UpdateMultiQuery;
import fr.gouv.vitam.common.exception.InvalidGuidOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.guid.GUIDReader;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.functional.administration.common.FileRules;
import fr.gouv.vitam.functional.administration.common.RuleMeasurementEnum;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleUnitParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;

/**
 * ArchiveUnitUpdateUtils in order to deal with common update operations for units
 *
 */
public class ArchiveUnitUpdateUtils {

    private static final String _DIFF = "$diff";
    private static final String RESULTS = "$results";
    private static final String DIFF = "#diff";
    private static final String FINAL_DIFF = "diff";
    private static final String ID = "#id";
    private static final String MANAGEMENT_KEY = "#management";
    private static final String RULES_KEY = "Rules";
    private static final String MANAGEMENT_PREFIX = MANAGEMENT_KEY + '.';
    private static final String RULES_PREFIX = '.' + RULES_KEY;

    private static final String UNIT_METADATA_UPDATE = "UPDATE_UNIT_RULES";

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ArchiveUnitUpdateUtils.class);

    /**
     * Method used to get update query for an archive unit
     * 
     * @param rulesForCategory
     * @param listRulesByType
     * @param query
     * @param key
     * @return
     * @throws ProcessingException
     */
    public boolean updateCategoryRules(ArrayNode rulesForCategory, List<JsonNode> listRulesByType,
        UpdateMultiQuery query, String key) throws ProcessingException {
        ArrayNode updatedRulesFinalForCategory = JsonHandler.createArrayNode();
        boolean updateNeeded = false;
        Iterator<JsonNode> updateRulesIterator = rulesForCategory.iterator();
        while (updateRulesIterator.hasNext()) {
            JsonNode updateRule = updateRulesIterator.next();
            String updateRuleName = updateRule.get("Rule").asText();            
            boolean findIt = false;
            for (JsonNode rule : listRulesByType) {
                // if the rule to be updated has no start date, then we dont do the maths,  
                // that means the rule is not really active - even if the query that returns
                // the list of au to be updated has been updated, just in case, the test is still present
                if (rule.get("RuleId").asText() != null && rule.get("RuleId").asText().equals(updateRuleName)
                    && updateRule.get("StartDate") != null) {
                    updateNeeded = true;
                    findIt = true;
                    updateRule = computeEndDate((ObjectNode) updateRule, rule);
                    updatedRulesFinalForCategory.add(updateRule);
                }
            }
            if (!findIt) {
                updatedRulesFinalForCategory.add(updateRule);
            }
        }
        // Put newRules in a new action
        Map<String, JsonNode> action = new HashMap<>();
        action.put(MANAGEMENT_PREFIX + key + RULES_PREFIX, updatedRulesFinalForCategory);
        try {
            query.addActions(new SetAction(action));
            return updateNeeded;
        } catch (InvalidCreateOperationException e) {
            throw new ProcessingException(e);
        }
    }


    private JsonNode computeEndDate(ObjectNode updatingRule, JsonNode ruleModel) throws ProcessingException {
        LocalDate endDate = null;

        // FIXME Start of duplicated method, need to add it in a common module
        final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        String startDateString = updatingRule.get("StartDate").asText();
        String ruleId = updatingRule.get("Rule").asText();
        String currentRuleType = ruleModel.get("RuleType").asText();

        if (ParametersChecker.isNotEmpty(startDateString) && ParametersChecker.isNotEmpty(ruleId, currentRuleType)) {
            LocalDate startDate = LocalDate.parse(startDateString, timeFormatter);
            if (startDate.getYear() >= 9000) {
                throw new ProcessingException("Wrong Start Date");
            }

            final String duration = ruleModel.get(FileRules.RULEDURATION).asText();
            final String measurement = ruleModel.get(FileRules.RULEMEASUREMENT).asText();
            if (!"unlimited".equalsIgnoreCase(duration)) {
                final RuleMeasurementEnum ruleMeasurement = RuleMeasurementEnum.getEnumFromType(measurement);
                endDate = startDate.plus(Integer.parseInt(duration), ruleMeasurement.getTemporalUnit());
            }
        }
        // End of duplicated method
        if (endDate != null) {
            updatingRule.put("EndDate", endDate.format(timeFormatter));
        }

        return updatingRule;
    }

    /**
     * Common method to get the diff message
     * 
     * @param diff
     * @param unitId
     * @return
     * @throws InvalidParseOperationException
     */
    public String getDiffMessageFor(JsonNode diff, String unitId) throws InvalidParseOperationException {
        if (diff == null) {
            return "";
        }
        final JsonNode arrayNode = diff.has(_DIFF) ? diff.get(_DIFF) : diff.get(RESULTS);
        if (arrayNode == null) {
            return "";
        }
        for (final JsonNode diffNode : arrayNode) {
            if (diffNode.get(ID) != null && unitId.equals(diffNode.get(ID).textValue())) {
                ObjectNode diffObject = JsonHandler.createObjectNode();
                diffObject.set(FINAL_DIFF, diffNode.get(DIFF));
                return JsonHandler.writeAsString(diffObject);
            }
        }
        return "";
    }

    /**
     * Method used to log lifecycles unit
     * 
     * @param params
     * @param auGuid
     * @param code
     * @param evDetData
     * @param logbookLifeCycleClient
     */
    public void logLifecycle(WorkerParameters params, String auGuid, StatusCode code,
        String evDetData, LogbookLifeCyclesClient logbookLifeCycleClient) {
        try {
            LogbookLifeCycleUnitParameters logbookLCParam =
                getLogbookLifeCycleUpdateUnitParameters(GUIDReader.getGUID(params.getContainerName()), code,
                    GUIDReader.getGUID(auGuid), UNIT_METADATA_UPDATE);
            if (evDetData != null) {
                logbookLCParam.putParameterValue(LogbookParameterName.eventDetailData, evDetData);
            }
            logbookLifeCycleClient.update(logbookLCParam);
        } catch (LogbookClientNotFoundException | LogbookClientBadRequestException | LogbookClientServerException |
            InvalidGuidOperationException e) {
            // Ignore since could be during first insert step
            LOGGER.error("Should be in First Insert step so not alreday commited", e);
        }
    }

    private LogbookLifeCycleUnitParameters getLogbookLifeCycleUpdateUnitParameters(GUID eventIdentifierProcess,
        StatusCode logbookOutcome, GUID objectIdentifier, String action) {
        final LogbookTypeProcess eventTypeProcess = LogbookTypeProcess.UPDATE;
        final GUID updateGuid = GUIDFactory.newEventGUID(ParameterHelper.getTenantParameter());
        return LogbookParametersFactory.newLogbookLifeCycleUnitParameters(updateGuid,
            VitamLogbookMessages.getEventTypeLfc(action),
            eventIdentifierProcess,
            eventTypeProcess, logbookOutcome,
            VitamLogbookMessages.getOutcomeDetailLfc(action, logbookOutcome),
            VitamLogbookMessages.getCodeLfc(action, logbookOutcome), objectIdentifier);
    }


    /**
     * Method used to commit lifecycle
     * 
     * @param processId
     * @param archiveUnitId
     * @param logbookLifeCycleClient
     */
    public void commitLifecycle(String processId, String archiveUnitId,
        LogbookLifeCyclesClient logbookLifeCycleClient) {
        try {
            logbookLifeCycleClient.commitUnit(processId, archiveUnitId);
        } catch (LogbookClientBadRequestException | LogbookClientNotFoundException |
            LogbookClientServerException e) {
            LOGGER.error("Couldn't commit lifecycles", e);
        }
    }
}
