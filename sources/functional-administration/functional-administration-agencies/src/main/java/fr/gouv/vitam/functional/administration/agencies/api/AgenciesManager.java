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
package fr.gouv.vitam.functional.administration.agencies.api;


import static fr.gouv.vitam.functional.administration.agencies.api.AgenciesService.AGENCIES_IMPORT_EVENT;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;

import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.functional.administration.common.ReferentialFileUtils;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;

/**
 * Agency validator and logBook manager
 */
class AgenciesManager {
    public static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AgenciesManager.class);

    private final GUID eip;
    private final LogbookOperationsClient logbookClient;
    private boolean warning = false;



    public AgenciesManager(LogbookOperationsClient logbookClient, GUID eip) {
        this.logbookClient = logbookClient;
        this.eip = eip;
    }

    @VisibleForTesting
    AgenciesManager(LogbookOperationsClient logBookclient, GUID eip, boolean warning) {
        this.logbookClient = logBookclient;
        this.eip = eip;
        this.warning = warning;
    }

    /**
     * log start process
     *
     * @throws VitamException thrown if the logbook could not be created
     * @param eventType the event type to be logged
     */
    public void logStarted(String eventType) throws VitamException {

        final LogbookOperationParameters logbookParameters = LogbookParametersFactory
            .newLogbookOperationParameters(eip, eventType, eip,
                LogbookTypeProcess.MASTERDATA,
                StatusCode.STARTED,
                VitamLogbookMessages.getCodeOp(eventType, StatusCode.STARTED), eip);
        logbookClient.create(logbookParameters);
    }

    /**
     * log end success process
     *
     * @param fileName name of the file
     * @throws VitamException thrown if the logbook could not be updated
     */
    public void logFinish(String fileName) throws VitamException {

        LogbookOperationParameters logbookParameters;
        final GUID eipId = GUIDFactory.newOperationLogbookGUID(ParameterHelper.getTenantParameter());

        if (warning) {
            logbookParameters = LogbookParametersFactory
                .newLogbookOperationParameters(eipId, AGENCIES_IMPORT_EVENT, eip, LogbookTypeProcess.MASTERDATA,
                    StatusCode.WARNING,
                    VitamLogbookMessages.getCodeOp(AGENCIES_IMPORT_EVENT, StatusCode.WARNING), eip);
            logbookParameters.putParameterValue(LogbookParameterName.eventDetailData,
                JsonHandler.unprettyPrint(evDetData));
        } else {

            logbookParameters = LogbookParametersFactory
                .newLogbookOperationParameters(eipId, AGENCIES_IMPORT_EVENT, eip, LogbookTypeProcess.MASTERDATA,
                    StatusCode.OK,
                    VitamLogbookMessages.getCodeOp(AGENCIES_IMPORT_EVENT, StatusCode.OK), eip);
        }

        ReferentialFileUtils.addFilenameInLogbookOperation(fileName, logbookParameters);
        logbookClient.update(logbookParameters);

    }


    /**
     * log end success process
     *
     * @param eventType the event type to be logged
     * @throws VitamException thrown if the logbook could not be updated
     */
    public void logEventSuccess(String eventType) throws VitamException {
        final LogbookOperationParameters logbookParameters = LogbookParametersFactory
            .newLogbookOperationParameters(GUIDFactory.newOperationLogbookGUID(ParameterHelper.getTenantParameter()),
                eventType, eip, LogbookTypeProcess.MASTERDATA, StatusCode.OK,
                VitamLogbookMessages.getCodeOp(eventType, StatusCode.OK), eip);
        logbookClient.update(logbookParameters);
    }

    /**
     * log end warnig
     *
     * @param eventType the event type to be logged
     * @throws VitamException thrown if the logbook could not be updated
     */
    public void logEventWarning(String eventType) throws VitamException {
        if (!warning) {
            warning = true;
        }
        final LogbookOperationParameters logbookParameters = LogbookParametersFactory
            .newLogbookOperationParameters(GUIDFactory.newOperationLogbookGUID(ParameterHelper.getTenantParameter()),
                eventType, eip, LogbookTypeProcess.MASTERDATA, StatusCode.WARNING,
                VitamLogbookMessages.getCodeOp(eventType, StatusCode.WARNING), eip);
        logbookParameters.putParameterValue(LogbookParameterName.eventDetailData,
            JsonHandler.unprettyPrint(evDetData));
        logbookClient.update(logbookParameters);

    }



    /**
     * log fatal error (system or technical error)
     *
     * @param errorsDetails the detail error
     * @param subEvenType the sub event type
     * @throws VitamException thrown if the logbook could not be updated
     */
    public void logError(String errorsDetails, String subEvenType) throws VitamException {
        LOGGER.error("There validation errors on the input file {}", errorsDetails);
        
        // create logbook parameters
        final LogbookOperationParameters logbookParameters = LogbookParametersFactory
            .newLogbookOperationParameters(eip, AGENCIES_IMPORT_EVENT, eip, LogbookTypeProcess.MASTERDATA,
                StatusCode.KO,
                VitamLogbookMessages.getCodeOp(AGENCIES_IMPORT_EVENT, StatusCode.KO), eip);
        // set outcomeDetail
        if (subEvenType != null) {
            logbookParameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
                VitamLogbookMessages.getCodeOp(AGENCIES_IMPORT_EVENT, subEvenType, StatusCode.KO));
            logbookParameters.putParameterValue(LogbookParameterName.outcomeDetail,
                VitamLogbookMessages.getOutcomeDetail(AGENCIES_IMPORT_EVENT, subEvenType, StatusCode.KO));
        }
        // set evDetData
        logbookMessageError(errorsDetails, logbookParameters);

        logbookClient.update(logbookParameters);
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


}
