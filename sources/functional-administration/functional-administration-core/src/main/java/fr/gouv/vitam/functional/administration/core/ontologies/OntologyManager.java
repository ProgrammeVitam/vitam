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
package fr.gouv.vitam.functional.administration.core.ontologies;

import com.fasterxml.jackson.databind.node.ObjectNode;
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
import fr.gouv.vitam.functional.administration.common.ErrorReportOntologies;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This class manage validation and log operation of Ontology service
 */
public class OntologyManager {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(OntologyManager.class);

    private final Map<String, List<ErrorReportOntologies>> errors;

    private final GUID eip;

    private final LogbookOperationsClientFactory logbookOperationsClientFactory;

    public OntologyManager(
        LogbookOperationsClientFactory logbookOperationsClientFactory,
        GUID eip,
        Map<String, List<ErrorReportOntologies>> errors
    ) {
        this.logbookOperationsClientFactory = logbookOperationsClientFactory;
        this.eip = eip;
        this.errors = errors;
    }

    /**
     * Add an error to the report
     *
     * @param identifier
     * @param error
     */
    public void addError(
        String identifier,
        ErrorReportOntologies error,
        Map<String, List<ErrorReportOntologies>> errors
    ) {
        List<ErrorReportOntologies> lineErrors = this.errors.get(identifier);
        if (lineErrors == null) {
            lineErrors = new ArrayList<>();
        }
        lineErrors.add(error);
        errors.put(identifier, lineErrors);
    }

    /**
     * Log validation error (business error)
     *
     * @param errorsDetails
     */
    public void logValidationError(String eventType, String objectId, String errorsDetails) throws VitamException {
        LOGGER.error("There validation errors on the input file {}", errorsDetails);
        final GUID eipId = GUIDFactory.newOperationLogbookGUID(ParameterHelper.getTenantParameter());
        final LogbookOperationParameters logbookParameters = LogbookParameterHelper.newLogbookOperationParameters(
            eipId,
            eventType,
            eip,
            LogbookTypeProcess.MASTERDATA,
            StatusCode.KO,
            VitamLogbookMessages.getCodeOp(eventType, StatusCode.KO),
            eip
        );
        logbookMessageError(objectId, errorsDetails, logbookParameters);
        try (LogbookOperationsClient logbookOperationsClient = logbookOperationsClientFactory.getClient()) {
            logbookOperationsClient.update(logbookParameters);
        }
    }

    private void logbookMessageError(
        String objectId,
        String errorsDetails,
        LogbookOperationParameters logbookParameters
    ) {
        if (null != errorsDetails && !errorsDetails.isEmpty()) {
            try {
                final ObjectNode object = JsonHandler.createObjectNode();
                object.put("ontologyCheck", errorsDetails);

                final String wellFormedJson = SanityChecker.sanitizeJson(object);
                logbookParameters.putParameterValue(LogbookParameterName.eventDetailData, wellFormedJson);
            } catch (InvalidParseOperationException e) {
                // Do nothing
            }
        }
        if (null != objectId && !objectId.isEmpty()) {
            logbookParameters.putParameterValue(LogbookParameterName.objectIdentifier, objectId);
        }
    }

    /**
     * log fatal error (system or technical error)
     *
     * @param errorsDetails
     * @throws VitamException
     */
    public void logFatalError(String eventType, String objectId, String errorsDetails) throws VitamException {
        LOGGER.error("There validation errors on the input file {}", errorsDetails);
        final GUID eipId = GUIDFactory.newOperationLogbookGUID(ParameterHelper.getTenantParameter());
        final LogbookOperationParameters logbookParameters = LogbookParameterHelper.newLogbookOperationParameters(
            eipId,
            eventType,
            eip,
            LogbookTypeProcess.MASTERDATA,
            StatusCode.FATAL,
            VitamLogbookMessages.getCodeOp(eventType, StatusCode.FATAL),
            eip
        );

        logbookMessageError(objectId, errorsDetails, logbookParameters);
        try (LogbookOperationsClient logbookOperationsClient = logbookOperationsClientFactory.getClient()) {
            logbookOperationsClient.update(logbookParameters);
        }
    }

    /**
     * log start process
     *
     * @throws VitamException
     */
    public void logStarted(String eventType, String objectId) throws VitamException {
        final LogbookOperationParameters logbookParameters = LogbookParameterHelper.newLogbookOperationParameters(
            eip,
            eventType,
            eip,
            LogbookTypeProcess.MASTERDATA,
            StatusCode.STARTED,
            VitamLogbookMessages.getCodeOp(eventType, StatusCode.STARTED),
            eip
        );

        logbookMessageError(objectId, null, logbookParameters);
        try (LogbookOperationsClient logbookOperationsClient = logbookOperationsClientFactory.getClient()) {
            logbookOperationsClient.create(logbookParameters);
        }
    }

    /**
     * log end success process
     *
     * @throws VitamException
     */
    public void logSuccess(String eventType, String objectId, String message) throws VitamException {
        final GUID eipId = GUIDFactory.newOperationLogbookGUID(ParameterHelper.getTenantParameter());
        final LogbookOperationParameters logbookParameters = LogbookParameterHelper.newLogbookOperationParameters(
            eipId,
            eventType,
            eip,
            LogbookTypeProcess.MASTERDATA,
            StatusCode.OK,
            VitamLogbookMessages.getCodeOp(eventType, StatusCode.OK),
            eip
        );

        if (null != objectId && !objectId.isEmpty()) {
            logbookParameters.putParameterValue(LogbookParameterName.objectIdentifier, objectId);
        }

        if (null != message && !message.isEmpty()) {
            logbookParameters.putParameterValue(LogbookParameterName.eventDetailData, message);
        }
        try (LogbookOperationsClient logbookOperationsClient = logbookOperationsClientFactory.getClient()) {
            logbookOperationsClient.update(logbookParameters);
        }
    }
}
