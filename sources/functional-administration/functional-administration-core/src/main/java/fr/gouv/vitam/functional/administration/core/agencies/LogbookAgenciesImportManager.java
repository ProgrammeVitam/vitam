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
package fr.gouv.vitam.functional.administration.core.agencies;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.functional.administration.common.ReferentialFileUtils;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookDocument;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbName;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.and;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.ne;

/**
 * Agency validator and logBook manager
 */
class LogbookAgenciesImportManager {

    public static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogbookAgenciesImportManager.class);

    private final LogbookOperationsClientFactory logbookOperationsClientFactory;

    public LogbookAgenciesImportManager(LogbookOperationsClientFactory logbookOperationsClientFactory) {
        this.logbookOperationsClientFactory = logbookOperationsClientFactory;
    }

    /**
     * log start process
     *
     * @param eventType the event type to be logged
     * @throws VitamException thrown if the logbook could not be created
     */
    public void logStarted(GUID eip, String eventType) throws VitamException {
        final LogbookOperationParameters logbookParameters = LogbookParameterHelper.newLogbookOperationParameters(
            eip,
            eventType,
            eip,
            LogbookTypeProcess.MASTERDATA,
            StatusCode.STARTED,
            VitamLogbookMessages.getCodeOp(eventType, StatusCode.STARTED),
            eip
        );
        createLogbookEntry(logbookParameters);
    }

    /**
     * log end success process
     *
     * @param fileName name of the file
     * @throws VitamException thrown if the logbook could not be updated
     */
    public void logFinishSuccess(GUID eip, String fileName, StatusCode statusCode) throws VitamException {
        LogbookOperationParameters logbookParameters;
        final GUID eipId = GUIDFactory.newOperationLogbookGUID(ParameterHelper.getTenantParameter());

        logbookParameters = LogbookParameterHelper.newLogbookOperationParameters(
            eipId,
            AgenciesService.AGENCIES_IMPORT_EVENT,
            eip,
            LogbookTypeProcess.MASTERDATA,
            statusCode,
            VitamLogbookMessages.getCodeOp(AgenciesService.AGENCIES_IMPORT_EVENT, statusCode),
            eip
        );
        logbookParameters.putParameterValue(LogbookParameterName.eventDetailData, JsonHandler.unprettyPrint(evDetData));

        ReferentialFileUtils.addFilenameInLogbookOperation(fileName, logbookParameters);
        updateLogbookEntry(logbookParameters);
    }

    /**
     * log end success process
     *
     * @param eventType the event type to be logged
     * @throws VitamException thrown if the logbook could not be updated
     */
    public void logEventFatal(GUID eip, String eventType) throws VitamException {
        final LogbookOperationParameters logbookParameters = LogbookParameterHelper.newLogbookOperationParameters(
            GUIDFactory.newOperationLogbookGUID(ParameterHelper.getTenantParameter()),
            eventType,
            eip,
            LogbookTypeProcess.MASTERDATA,
            StatusCode.FATAL,
            VitamLogbookMessages.getCodeOp(eventType, StatusCode.FATAL),
            eip
        );
        updateLogbookEntry(logbookParameters);
    }

    /**
     * log end success process
     *
     * @param eventType the event type to be logged
     * @throws VitamException thrown if the logbook could not be updated
     */
    public void logEventSuccess(GUID eip, String eventType) throws VitamException {
        final LogbookOperationParameters logbookParameters = LogbookParameterHelper.newLogbookOperationParameters(
            GUIDFactory.newOperationLogbookGUID(ParameterHelper.getTenantParameter()),
            eventType,
            eip,
            LogbookTypeProcess.MASTERDATA,
            StatusCode.OK,
            VitamLogbookMessages.getCodeOp(eventType, StatusCode.OK),
            eip
        );
        updateLogbookEntry(logbookParameters);
    }

    /**
     * log end warnig
     *
     * @param eventType the event type to be logged
     * @throws VitamException thrown if the logbook could not be updated
     */
    public void logEventWarning(GUID eip, String eventType) throws VitamException {
        final LogbookOperationParameters logbookParameters = LogbookParameterHelper.newLogbookOperationParameters(
            GUIDFactory.newOperationLogbookGUID(ParameterHelper.getTenantParameter()),
            eventType,
            eip,
            LogbookTypeProcess.MASTERDATA,
            StatusCode.WARNING,
            VitamLogbookMessages.getCodeOp(eventType, StatusCode.WARNING),
            eip
        );
        logbookParameters.putParameterValue(LogbookParameterName.eventDetailData, JsonHandler.unprettyPrint(evDetData));
        updateLogbookEntry(logbookParameters);
    }

    /**
     * log fatal error (system or technical error)
     *
     * @param errorsDetails the detail error
     * @param subEvenType the sub event type
     * @throws VitamException thrown if the logbook could not be updated
     */
    public void logError(GUID eip, String errorsDetails, String subEvenType) throws VitamException {
        LOGGER.error("There validation errors on the input file {}", errorsDetails);

        // create logbook parameters
        final LogbookOperationParameters logbookParameters = LogbookParameterHelper.newLogbookOperationParameters(
            eip,
            AgenciesService.AGENCIES_IMPORT_EVENT,
            eip,
            LogbookTypeProcess.MASTERDATA,
            StatusCode.KO,
            VitamLogbookMessages.getCodeOp(AgenciesService.AGENCIES_IMPORT_EVENT, StatusCode.KO),
            eip
        );
        // set outcomeDetail
        if (subEvenType != null) {
            logbookParameters.putParameterValue(
                LogbookParameterName.outcomeDetailMessage,
                VitamLogbookMessages.getCodeOp(AgenciesService.AGENCIES_IMPORT_EVENT, subEvenType, StatusCode.KO)
            );
            logbookParameters.putParameterValue(
                LogbookParameterName.outcomeDetail,
                VitamLogbookMessages.getOutcomeDetail(AgenciesService.AGENCIES_IMPORT_EVENT, subEvenType, StatusCode.KO)
            );
        }
        // set evDetData
        logbookMessageError(errorsDetails, logbookParameters);

        updateLogbookEntry(logbookParameters);
    }

    private void logbookMessageError(String errorsDetails, LogbookOperationParameters logbookParameters) {
        if (null != errorsDetails && !errorsDetails.isEmpty()) {
            try {
                final ObjectNode object = JsonHandler.createObjectNode();
                object.put("agencyCheck", errorsDetails);

                final String wellFormedJson = SanityChecker.sanitizeJson(object);
                logbookParameters.putParameterValue(LogbookParameterName.eventDetailData, wellFormedJson);
            } catch (final InvalidParseOperationException e) {
                // Do nothing
            }
        }
    }

    public void setEvDetData(ObjectNode evDetData) {
        this.evDetData = evDetData;
    }

    private ObjectNode evDetData = JsonHandler.createObjectNode();

    public boolean isImportOperationInProgress(GUID eip) throws VitamException {
        try (LogbookOperationsClient logbookClient = logbookOperationsClientFactory.getClient()) {
            final Select select = new Select();
            select.setLimitFilter(0, 1);
            select.addOrderByDescFilter(LogbookMongoDbName.eventDateTime.getDbname());
            select.setQuery(
                and()
                    .add(eq(LogbookMongoDbName.eventType.getDbname(), AgenciesService.AGENCIES_IMPORT_EVENT))
                    .add(ne(LogbookMongoDbName.eventIdentifier.getDbname(), eip.getId()))
            );
            // FIXME : #9847 Fix logbook projections - Add back projection once projection handling is fixed
            // select.addProjection(
            //     JsonHandler.createObjectNode().set(BuilderToken.PROJECTION.FIELDS.exactToken(),
            //         JsonHandler.createObjectNode()
            //             .put(BuilderToken.PROJECTIONARGS.ID.exactToken(), 1)
            //             .put(String.format("%s.%s", LogbookDocument.EVENTS, LogbookMongoDbName.eventType.getDbname()),
            //                 1)));
            JsonNode logbookResult = logbookClient.selectOperation(select.getFinalSelect());
            RequestResponseOK<JsonNode> requestResponseOK = RequestResponseOK.getFromJsonNode(logbookResult);
            // one result and last event type is STP_IMPORT_RULES -> import in progress
            if (requestResponseOK.getHits().getSize() != 0) {
                JsonNode result = requestResponseOK.getResults().get(0);
                if (result.get(LogbookDocument.EVENTS) != null && result.get(LogbookDocument.EVENTS).size() > 0) {
                    JsonNode lastEvent = result
                        .get(LogbookDocument.EVENTS)
                        .get(result.get(LogbookDocument.EVENTS).size() - 1);
                    return !AgenciesService.AGENCIES_IMPORT_EVENT.equals(
                        lastEvent.get(LogbookMongoDbName.eventType.getDbname()).asText()
                    );
                } else {
                    return true;
                }
            } else {
                return false;
            }
        } catch (InvalidCreateOperationException | InvalidParseOperationException | LogbookClientException e) {
            throw new VitamException(e);
        }
    }

    private void createLogbookEntry(LogbookOperationParameters logbookParametersStart) throws LogbookClientException {
        try (LogbookOperationsClient client = logbookOperationsClientFactory.getClient()) {
            client.create(logbookParametersStart);
        }
    }

    private void updateLogbookEntry(LogbookOperationParameters logbookParametersEnd) throws LogbookClientException {
        try (LogbookOperationsClient client = logbookOperationsClientFactory.getClient()) {
            client.update(logbookParametersEnd);
        }
    }
}
