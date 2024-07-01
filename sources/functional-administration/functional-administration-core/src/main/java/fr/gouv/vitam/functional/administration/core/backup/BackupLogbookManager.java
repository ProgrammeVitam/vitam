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
package fr.gouv.vitam.functional.administration.core.backup;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;

import static fr.gouv.vitam.common.i18n.VitamLogbookMessages.getCodeOp;

/**
 * Class manage logbook operations logging
 */
public class BackupLogbookManager {

    public static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(BackupLogbookManager.class);
    public static final String ERROR_MESSAGE = "Message";
    public static final String FILE_NAME = "FileName";
    public static final String DIGEST = "Digest";
    public static final String DIGESTTYPE = "DigestType";
    private final LogbookOperationsClientFactory logbookClientFactory;

    public BackupLogbookManager() {
        this.logbookClientFactory = LogbookOperationsClientFactory.getInstance();
    }

    @VisibleForTesting
    public BackupLogbookManager(LogbookOperationsClientFactory logbookClientFactory) {
        this.logbookClientFactory = logbookClientFactory;
    }

    /**
     * log end success process
     *
     * @param eventType the event type to be logged
     * @throws VitamException thrown if the logbook could not be updated
     */
    public void logEventSuccess(
        GUID logbookOperationMasterId,
        String eventType,
        String digestStr,
        String fileName,
        String objectIdentifier
    ) throws VitamException {
        final GUID eipId = GUIDFactory.newOperationLogbookGUID(ParameterHelper.getTenantParameter());

        final LogbookOperationParameters logbookParameters = LogbookParameterHelper.newLogbookOperationParameters(
            eipId,
            eventType,
            logbookOperationMasterId,
            LogbookTypeProcess.MASTERDATA,
            StatusCode.OK,
            getCodeOp(eventType, StatusCode.OK),
            logbookOperationMasterId
        );

        if (objectIdentifier != null && !objectIdentifier.isEmpty()) {
            logbookParameters.putParameterValue(LogbookParameterName.objectIdentifier, objectIdentifier);
        }

        ObjectNode evDetData = JsonHandler.createObjectNode();
        evDetData.put(FILE_NAME, fileName);
        evDetData.put(DIGEST, digestStr);
        evDetData.put(DIGESTTYPE, VitamConfiguration.getDefaultDigestType().getName());

        logbookParameters.putParameterValue(LogbookParameterName.eventDetailData, JsonHandler.unprettyPrint(evDetData));

        LogbookOperationsClient logbookClient = logbookClientFactory.getClient();
        logbookClient.update(logbookParameters);
    }

    /**
     * log error (system or technical error)
     *
     * @param logbookOperationMasterId
     * @param eventType eventType
     * @param errorsDetails the detail error
     * @throws VitamException
     */
    public void logError(GUID logbookOperationMasterId, String eventType, String errorsDetails) throws VitamException {
        LOGGER.error("There validation errors on the input file {}", errorsDetails);

        final GUID eipId = GUIDFactory.newOperationLogbookGUID(ParameterHelper.getTenantParameter());
        final LogbookOperationParameters logbookParameters = LogbookParameterHelper.newLogbookOperationParameters(
            eipId,
            eventType,
            logbookOperationMasterId,
            LogbookTypeProcess.MASTERDATA,
            StatusCode.KO,
            getCodeOp(eventType, StatusCode.KO),
            logbookOperationMasterId
        );
        logbookMessageError(errorsDetails, logbookParameters);

        LogbookOperationsClient logbookClient = logbookClientFactory.getClient();
        logbookClient.update(logbookParameters);
    }

    private void logbookMessageError(String errorsDetails, LogbookOperationParameters logbookParameters) {
        ParametersChecker.checkParameter(errorsDetails);

        final ObjectNode object = JsonHandler.createObjectNode();
        object.put(ERROR_MESSAGE, errorsDetails);
        final String wellFormedJson;
        try {
            wellFormedJson = SanityChecker.sanitizeJson(object);
            logbookParameters.putParameterValue(LogbookParameterName.eventDetailData, wellFormedJson);
        } catch (InvalidParseOperationException e) {
            throw new IllegalStateException("Could not sanitize json message: " + errorsDetails, e);
        }
    }
}
