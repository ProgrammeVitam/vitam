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

package fr.gouv.vitam.functional.administration.core.rules;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.functional.administration.common.ReferentialFileUtils;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.parameters.Contexts;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookDocument;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbName;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;

import java.util.Set;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static fr.gouv.vitam.common.parameter.ParameterHelper.getTenantParameter;

public class LogbookRuleImportManager {

    private static final String STP_IMPORT_RULES = "STP_IMPORT_RULES";
    private static final String CHECK_RULES = "CHECK_RULES";
    private static final String COMMIT_RULES = "COMMIT_RULES";
    private static final String USED_DELETED_RULE_IDS = "usedDeletedRuleIds";
    private static final String USED_UPDATED_RULE_IDS = "usedUpdatedRuleIds";
    private static final String USED_RULE_IDS_WITH_DURATION_MODE_UPDATE = "usedRuleIdsWithDurationModeUpdate";
    private static final String NB_DELETED = "nbDeleted";
    private static final String NB_UPDATED = "nbUpdated";
    private static final String NB_INSERTED = "nbInserted";
    private static final String DELETED_RULE_IDS = "deletedRuleIds";
    private static final String UPDATE_RULES_ARCHIVE_UNITS = Contexts.UPDATE_RULES_ARCHIVE_UNITS.name();

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogbookRuleImportManager.class);

    private final LogbookOperationsClientFactory logbookOperationsClientFactory;

    public LogbookRuleImportManager(LogbookOperationsClientFactory logbookOperationsClientFactory) {
        this.logbookOperationsClientFactory = logbookOperationsClientFactory;
    }

    public void initStpImportRulesLogbookOperation(final GUID eip) throws LogbookClientException {
        final LogbookOperationParameters logbookParametersStart = LogbookParameterHelper.newLogbookOperationParameters(
            eip,
            STP_IMPORT_RULES,
            eip,
            LogbookTypeProcess.MASTERDATA,
            StatusCode.STARTED,
            VitamLogbookMessages.getCodeOp(STP_IMPORT_RULES, StatusCode.STARTED),
            eip
        );
        createLogBookEntry(logbookParametersStart);
    }

    public void updateCommitFileRulesLogbookOperationOkOrKo(
        StatusCode statusCode,
        GUID evIdentifierProcess,
        int nbDeleted,
        int nbUpdated,
        int nbInserted
    ) throws LogbookClientException {
        final ObjectNode evDetData = JsonHandler.createObjectNode();
        evDetData.put(NB_DELETED, nbDeleted);
        evDetData.put(NB_UPDATED, nbUpdated);
        evDetData.put(NB_INSERTED, nbInserted);
        final GUID eventId = GUIDFactory.newOperationLogbookGUID(getTenantParameter());
        final LogbookOperationParameters logbookOperationParameters =
            LogbookParameterHelper.newLogbookOperationParameters(
                eventId,
                COMMIT_RULES,
                evIdentifierProcess,
                LogbookTypeProcess.MASTERDATA,
                statusCode,
                VitamLogbookMessages.getCodeOp(COMMIT_RULES, statusCode),
                evIdentifierProcess
            );
        logbookOperationParameters.putParameterValue(
            LogbookParameterName.eventDetailData,
            JsonHandler.unprettyPrint(evDetData)
        );
        logbookOperationParameters.putParameterValue(
            LogbookParameterName.outcomeDetail,
            COMMIT_RULES + "." + statusCode
        );
        updateLogBookEntry(logbookOperationParameters);
    }

    public void updateCheckFileRulesLogbookOperation(
        StatusCode statusCode,
        Set<String> usedUpdatedRuleIds,
        Set<String> notUsedDeletedRuleIds,
        Set<String> nonDeletableUsedRuleIds,
        Set<String> usedRuleIdsWithDurationModeUpdate,
        GUID evIdentifierProcess
    ) throws LogbookClientException {
        final ObjectNode evDetData = JsonHandler.createObjectNode();
        if (!notUsedDeletedRuleIds.isEmpty()) {
            evDetData.set(DELETED_RULE_IDS, serializeRuleIds(notUsedDeletedRuleIds));
        }
        if (!usedUpdatedRuleIds.isEmpty()) {
            evDetData.set(USED_UPDATED_RULE_IDS, serializeRuleIds(usedUpdatedRuleIds));
        }
        if (!nonDeletableUsedRuleIds.isEmpty()) {
            evDetData.set(USED_DELETED_RULE_IDS, serializeRuleIds(nonDeletableUsedRuleIds));
        }
        if (!usedRuleIdsWithDurationModeUpdate.isEmpty()) {
            evDetData.set(USED_RULE_IDS_WITH_DURATION_MODE_UPDATE, serializeRuleIds(usedRuleIdsWithDurationModeUpdate));
        }
        final GUID eventId = GUIDFactory.newOperationLogbookGUID(getTenantParameter());
        final LogbookOperationParameters logbookOperationParameters =
            LogbookParameterHelper.newLogbookOperationParameters(
                eventId,
                CHECK_RULES,
                evIdentifierProcess,
                LogbookTypeProcess.MASTERDATA,
                statusCode,
                VitamLogbookMessages.getCodeOp(CHECK_RULES, statusCode),
                evIdentifierProcess
            );
        logbookOperationParameters.putParameterValue(
            LogbookParameterName.eventDetailData,
            JsonHandler.unprettyPrint(evDetData)
        );
        logbookOperationParameters.putParameterValue(
            LogbookParameterName.outcomeDetail,
            CHECK_RULES + "." + statusCode
        );
        updateLogBookEntry(logbookOperationParameters);
    }

    private ArrayNode serializeRuleIds(Set<String> ruleIds) {
        final ArrayNode arrayNode = JsonHandler.createArrayNode();
        for (String fileRulesId : ruleIds) {
            arrayNode.add(fileRulesId);
        }
        return arrayNode;
    }

    public void updateCheckFileRulesLogbookOperationWhenCheckBeforeImportIsKo(
        String subEvenType,
        GUID evIdentifierProcess
    ) throws LogbookClientException {
        final GUID eventId = GUIDFactory.newOperationLogbookGUID(getTenantParameter());
        final LogbookOperationParameters logbookOperationParameters =
            LogbookParameterHelper.newLogbookOperationParameters(
                eventId,
                CHECK_RULES,
                evIdentifierProcess,
                LogbookTypeProcess.MASTERDATA,
                StatusCode.KO,
                VitamLogbookMessages.getCodeOp(CHECK_RULES, subEvenType, StatusCode.KO),
                evIdentifierProcess
            );
        logbookOperationParameters.putParameterValue(
            LogbookParameterName.outcomeDetail,
            VitamLogbookMessages.getOutcomeDetail(CHECK_RULES, subEvenType, StatusCode.KO)
        );
        updateLogBookEntry(logbookOperationParameters);
    }

    public void updateStpImportRulesLogbookOperation(final GUID eip, StatusCode status, String filename)
        throws InvalidParseOperationException, LogbookClientException {
        final GUID eip1 = GUIDFactory.newEventGUID(eip);
        final LogbookOperationParameters logbookParametersEnd = LogbookParameterHelper.newLogbookOperationParameters(
            eip1,
            STP_IMPORT_RULES,
            eip,
            LogbookTypeProcess.MASTERDATA,
            status,
            VitamLogbookMessages.getCodeOp(STP_IMPORT_RULES, status),
            eip
        );
        ReferentialFileUtils.addFilenameInLogbookOperation(filename, logbookParametersEnd);
        updateLogBookEntry(logbookParametersEnd);
    }

    public void initializeUnitRuleUpdateWorkflowLogbook(GUID updateOperationGUID, GUID reqId)
        throws LogbookClientException {
        final LogbookOperationParameters logbookUpdateParametersStart =
            LogbookParameterHelper.newLogbookOperationParameters(
                updateOperationGUID,
                UPDATE_RULES_ARCHIVE_UNITS,
                updateOperationGUID,
                LogbookTypeProcess.UPDATE,
                StatusCode.STARTED,
                VitamLogbookMessages.getCodeOp(UPDATE_RULES_ARCHIVE_UNITS, StatusCode.STARTED),
                reqId
            );
        createLogBookEntry(logbookUpdateParametersStart);
    }

    public void updateUnitRuleUpdateWorkflowLogbook(GUID updateOperationGUID, GUID reqId)
        throws LogbookClientException {
        final LogbookOperationParameters logbookUpdateParametersEnd =
            LogbookParameterHelper.newLogbookOperationParameters(
                updateOperationGUID,
                UPDATE_RULES_ARCHIVE_UNITS,
                updateOperationGUID,
                LogbookTypeProcess.UPDATE,
                StatusCode.KO,
                VitamLogbookMessages.getCodeOp(UPDATE_RULES_ARCHIVE_UNITS, StatusCode.KO),
                reqId
            );
        updateLogBookEntry(logbookUpdateParametersEnd);
    }

    private void createLogBookEntry(LogbookOperationParameters logbookParametersStart) throws LogbookClientException {
        try (LogbookOperationsClient client = logbookOperationsClientFactory.getClient()) {
            client.create(logbookParametersStart);
        }
    }

    private void updateLogBookEntry(LogbookOperationParameters logbookParametersEnd) throws LogbookClientException {
        try (LogbookOperationsClient client = logbookOperationsClientFactory.getClient()) {
            client.update(logbookParametersEnd);
        }
    }

    public boolean isImportOperationInProgress() throws FileRulesException {
        try {
            final Select select = new Select();
            select.setLimitFilter(0, 1);
            select.addOrderByDescFilter(LogbookMongoDbName.eventDateTime.getDbname());
            select.setQuery(
                eq(
                    String.format("%s.%s", LogbookDocument.EVENTS, LogbookMongoDbName.eventType.getDbname()),
                    STP_IMPORT_RULES
                )
            );
            // FIXME : #9847 Fix logbook projections - Add back projection once projection handling is fixed
            // select.addProjection(
            //    JsonHandler.createObjectNode().set(BuilderToken.PROJECTION.FIELDS.exactToken(),
            //         JsonHandler.createObjectNode()
            //             .put(BuilderToken.PROJECTIONARGS.ID.exactToken(), 1)
            //             .put(String.format("%s.%s", LogbookDocument.EVENTS, LogbookMongoDbName.eventType.getDbname()),
            //                 1)));
            JsonNode logbookResult = logbookOperationsClientFactory
                .getClient()
                .selectOperation(select.getFinalSelect());
            RequestResponseOK<JsonNode> requestResponseOK = RequestResponseOK.getFromJsonNode(logbookResult);
            // one result and last event type is STP_IMPORT_RULES -> import in progress
            if (requestResponseOK.getHits().getSize() != 0) {
                JsonNode result = requestResponseOK.getResults().get(0);
                if (result.get(LogbookDocument.EVENTS) != null && result.get(LogbookDocument.EVENTS).size() > 0) {
                    JsonNode lastEvent = result
                        .get(LogbookDocument.EVENTS)
                        .get(result.get(LogbookDocument.EVENTS).size() - 1);
                    return !STP_IMPORT_RULES.equals(lastEvent.get(LogbookMongoDbName.eventType.getDbname()).asText());
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } catch (InvalidCreateOperationException | InvalidParseOperationException | LogbookClientException e) {
            throw new FileRulesException(e);
        }
    }
}
