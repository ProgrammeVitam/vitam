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
package fr.gouv.vitam.functional.administration.core.griffin;

import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;

import static fr.gouv.vitam.common.guid.GUIDFactory.newOperationLogbookGUID;
import static fr.gouv.vitam.common.i18n.VitamLogbookMessages.getCodeOp;
import static fr.gouv.vitam.common.model.StatusCode.KO;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.common.model.StatusCode.STARTED;
import static fr.gouv.vitam.common.model.StatusCode.WARNING;
import static fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper.newLogbookOperationParameters;
import static fr.gouv.vitam.logbook.common.parameters.LogbookParameterName.outcomeDetail;
import static fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess.MASTERDATA;

/**
 * LogbookGriffinHelper class
 */
class LogbookGriffinHelper {

    LogbookGriffinHelper() {
        throw new IllegalStateException("Utility class");
    }

    static void createLogbook(LogbookOperationsClientFactory factory, GUID guid, String stepName)
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException {
        String codeOp = getCodeOp(stepName, STARTED);
        LogbookOperationParameters logbookParameters = newLogbookOperationParameters(
            guid,
            stepName,
            guid,
            MASTERDATA,
            STARTED,
            codeOp,
            guid
        );

        String parameterValue = stepName + "." + STARTED;
        logbookParameters.putParameterValue(outcomeDetail, parameterValue);

        try (LogbookOperationsClient client = factory.getClient()) {
            client.create(logbookParameters);
        }
    }

    static void createLogbookEventSuccess(LogbookOperationsClientFactory factory, GUID guid, String stepName)
        throws LogbookClientBadRequestException, LogbookClientServerException, LogbookClientNotFoundException {
        GUID guiEvent = newOperationLogbookGUID(ParameterHelper.getTenantParameter());
        String codeOp = getCodeOp(stepName, OK);

        LogbookOperationParameters logbookParameters = newLogbookOperationParameters(
            guiEvent,
            stepName,
            guid,
            MASTERDATA,
            OK,
            codeOp,
            guid
        );

        String parameterValue = stepName + "." + OK;
        logbookParameters.putParameterValue(outcomeDetail, parameterValue);

        try (LogbookOperationsClient client = factory.getClient()) {
            client.update(logbookParameters);
        }
    }

    static void createLogbookEventWarning(
        LogbookOperationsClientFactory factory,
        GUID guid,
        String stepName,
        GriffinReport warnings
    ) throws LogbookClientBadRequestException, LogbookClientServerException, LogbookClientNotFoundException {
        GUID guiEvent = newOperationLogbookGUID(ParameterHelper.getTenantParameter());
        String codeOp = getCodeOp(stepName, WARNING);

        LogbookOperationParameters logbookParameters = newLogbookOperationParameters(
            guiEvent,
            stepName,
            guid,
            MASTERDATA,
            WARNING,
            codeOp,
            guid
        );

        String parameterValue = stepName + "." + WARNING;
        logbookParameters.putParameterValue(outcomeDetail, parameterValue);

        logbookParameters.putParameterValue(LogbookParameterName.eventDetailData, JsonHandler.unprettyPrint(warnings));
        try (LogbookOperationsClient client = factory.getClient()) {
            client.update(logbookParameters);
        }
    }

    static void createLogbookEventKo(LogbookOperationsClientFactory factory, GUID guid, String stepName, String error)
        throws LogbookClientBadRequestException, LogbookClientServerException, LogbookClientNotFoundException, InvalidParseOperationException {
        GUID guiEvent = newOperationLogbookGUID(ParameterHelper.getTenantParameter());
        String codeOp = getCodeOp(stepName, KO);

        LogbookOperationParameters logbookParameters = newLogbookOperationParameters(
            guiEvent,
            stepName,
            guid,
            MASTERDATA,
            KO,
            codeOp,
            guid
        );

        String parameterValue = stepName + "." + KO;
        logbookParameters.putParameterValue(outcomeDetail, parameterValue);

        final ObjectNode object = JsonHandler.createObjectNode();
        object.put("ErrorDetail", error);
        final String wellFormedJson = SanityChecker.sanitizeJson(object);
        logbookParameters.putParameterValue(LogbookParameterName.eventDetailData, wellFormedJson);

        try (LogbookOperationsClient client = factory.getClient()) {
            client.update(logbookParameters);
        }
    }
}
