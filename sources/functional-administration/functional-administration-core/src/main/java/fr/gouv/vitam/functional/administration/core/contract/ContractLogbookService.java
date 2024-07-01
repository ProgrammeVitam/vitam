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
package fr.gouv.vitam.functional.administration.core.contract;

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
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;

import java.util.List;

public class ContractLogbookService {

    public static final String MC_GLOBAL_ERROR = "ManagementContract service error";
    public static final String IC_GLOBAL_ERROR = "IngestContract service error";
    public static final String AC_GLOBAL_ERROR = "AccessContract service error";
    public static final String EMPTY_REQUIRED_FIELD = ".EMPTY_REQUIRED_FIELD.KO";
    public static final String WRONG_FIELD_FORMAT = ".TO_BE_DEFINED.KO";
    public static final String DUPLICATE_IN_DATABASE = ".IDENTIFIER_DUPLICATION.KO";
    public static final String PROFILE_NOT_FOUND_IN_DATABASE = ".PROFILE_NOT_FOUND.KO";
    public static final String AGENCY_NOT_FOUND_IN_DATABASE = ".AGENCY_NOT_FOUND.KO";
    public static final String CONTRACT_VALIDATION_ERROR = ".VALIDATION_ERROR.KO";
    public static final String FORMAT_NOT_FOUND = ".FORMAT_NOT_FOUND.KO";
    public static final String FORMAT_MUST_BE_EMPTY = ".FORMAT_MUST_BE_EMPTY.KO";
    public static final String FORMAT_MUST_NOT_BE_EMPTY = ".FORMAT_MUST_NOT_BE_EMPTY.KO";
    public static final String MANAGEMENTCONTRACT_NOT_FOUND = ".MANAGEMENTCONTRACT_NOT_FOUND.KO";
    public static final String CONTRACT_BAD_REQUEST = ".BAD_REQUEST.KO";
    public static final String UPDATE_CONTRACT_NOT_FOUND = ".CONTRACT_NOT_FOUND.KO";
    public static final String UPDATE_VALUE_NOT_IN_ENUM = ".NOT_IN_ENUM.KO";
    public static final String UPDATE_WRONG_FILEFORMAT = ".FILEFORMAT_NOT_FOUND.KO";
    public static final String STRATEGY_VALIDATION_ERROR = ".STRATEGY_VALIDATION_ERROR.KO";
    public static final String VERSION_RETENTION_POLICY_VALIDATION_ERROR = ".VERSION_RETENTION_POLICY_ERROR.KO";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ContractLogbookService.class);
    private static final String EVDETDATA_IDENTIFIER = "identifier";
    private static final String UPDATED_DIFFS = "updatedDiffs";
    private final GUID eip;
    private final LogbookOperationsClient logbookClient;
    private final String contractsImportEventCode;
    private final String contractUpdateEventCode;
    private final String collectionType;
    private final String contractCheckKey;

    public ContractLogbookService(
        LogbookOperationsClient logbookClient,
        GUID eip,
        String contractsImportEventCode,
        String contractUpdateEventCode,
        String collectionType,
        String contractCheckKey
    ) {
        this.logbookClient = logbookClient;
        this.eip = eip;
        this.contractsImportEventCode = contractsImportEventCode;
        this.contractUpdateEventCode = contractUpdateEventCode;
        this.collectionType = collectionType;
        this.contractCheckKey = contractCheckKey;
    }

    /**
     * Log validation error (business error)
     *
     * @param errorsDetails
     */
    public void logValidationError(final String errorsDetails, final String eventType, final String KOEventType)
        throws VitamException {
        LOGGER.error("There validation errors on the input file {}", errorsDetails);
        final GUID eipUsage = GUIDFactory.newOperationLogbookGUID(ParameterHelper.getTenantParameter());
        final LogbookOperationParameters logbookParameters = LogbookParameterHelper.newLogbookOperationParameters(
            eipUsage,
            eventType,
            eip,
            LogbookTypeProcess.MASTERDATA,
            StatusCode.KO,
            VitamLogbookMessages.getFromFullCodeKey(KOEventType),
            eip
        );
        logbookParameters.putParameterValue(LogbookParameterName.outcomeDetail, KOEventType);
        logbookMessageError(errorsDetails, logbookParameters, KOEventType);
        logbookClient.update(logbookParameters);
    }

    /**
     * log fatal error (system or technical error)
     *
     * @param errorsDetails
     * @throws VitamException
     */
    public void logFatalError(final String errorsDetails, final String eventType) throws VitamException {
        LOGGER.error("There validation errors on the input file {}", errorsDetails);
        final GUID eipUsage = GUIDFactory.newOperationLogbookGUID(ParameterHelper.getTenantParameter());
        final LogbookOperationParameters logbookParameters = LogbookParameterHelper.newLogbookOperationParameters(
            eipUsage,
            eventType,
            eip,
            LogbookTypeProcess.MASTERDATA,
            StatusCode.FATAL,
            VitamLogbookMessages.getCodeOp(eventType, StatusCode.FATAL),
            eip
        );
        logbookParameters.putParameterValue(LogbookParameterName.outcomeDetail, eventType + "." + StatusCode.FATAL);
        logbookMessageError(errorsDetails, logbookParameters);
        logbookClient.update(logbookParameters);
    }

    private void logbookMessageError(String errorsDetails, LogbookOperationParameters logbookParameters) {
        if (null != errorsDetails && !errorsDetails.isEmpty()) {
            try {
                final ObjectNode object = JsonHandler.createObjectNode();
                object.put(this.contractCheckKey, errorsDetails);

                final String wellFormedJson = SanityChecker.sanitizeJson(object);
                logbookParameters.putParameterValue(LogbookParameterName.eventDetailData, wellFormedJson);
            } catch (final InvalidParseOperationException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private void logbookMessageError(
        String errorsDetails,
        LogbookOperationParameters logbookParameters,
        String KOEventType
    ) {
        if (null != errorsDetails && !errorsDetails.isEmpty()) {
            try {
                final ObjectNode object = JsonHandler.createObjectNode();
                String evDetDataKey = null;
                String detailKo = KOEventType.replaceFirst(this.contractsImportEventCode, "").replaceFirst(
                    this.contractUpdateEventCode,
                    ""
                );
                switch (detailKo) {
                    case EMPTY_REQUIRED_FIELD:
                        evDetDataKey = "Mandatory Fields";
                        break;
                    case WRONG_FIELD_FORMAT:
                    case UPDATE_WRONG_FILEFORMAT:
                        evDetDataKey = "Incorrect Field and value";
                        break;
                    case DUPLICATE_IN_DATABASE:
                        evDetDataKey = "Duplicate Field";
                        break;
                    case UPDATE_CONTRACT_NOT_FOUND:
                        evDetDataKey = "Contract not found";
                        break;
                    case UPDATE_VALUE_NOT_IN_ENUM:
                        evDetDataKey = "Not in Enum";
                        break;
                    case PROFILE_NOT_FOUND_IN_DATABASE:
                        evDetDataKey = "Profile not found";
                        break;
                    case AGENCY_NOT_FOUND_IN_DATABASE:
                        evDetDataKey = "Agency not found";
                        break;
                    case CONTRACT_VALIDATION_ERROR:
                        evDetDataKey = "Validation error";
                        break;
                    case STRATEGY_VALIDATION_ERROR:
                    case CONTRACT_BAD_REQUEST:
                    case FORMAT_NOT_FOUND:
                    case FORMAT_MUST_BE_EMPTY:
                    case FORMAT_MUST_NOT_BE_EMPTY:
                    case MANAGEMENTCONTRACT_NOT_FOUND:
                    case AC_GLOBAL_ERROR:
                    case IC_GLOBAL_ERROR:
                    case MC_GLOBAL_ERROR:
                    case VERSION_RETENTION_POLICY_VALIDATION_ERROR:
                        evDetDataKey = this.contractCheckKey;
                        break;
                    default:
                        throw new IllegalArgumentException(detailKo + " not found in detail ko values");
                }

                object.put(evDetDataKey, errorsDetails);

                final String wellFormedJson = SanityChecker.sanitizeJson(object);
                logbookParameters.putParameterValue(LogbookParameterName.eventDetailData, wellFormedJson);
            } catch (final InvalidParseOperationException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    /**
     * log start process
     *
     * @throws VitamException
     */
    public void logStarted() throws VitamException {
        final LogbookOperationParameters logbookParameters = LogbookParameterHelper.newLogbookOperationParameters(
            eip,
            contractsImportEventCode,
            eip,
            LogbookTypeProcess.MASTERDATA,
            StatusCode.STARTED,
            VitamLogbookMessages.getCodeOp(contractsImportEventCode, StatusCode.STARTED),
            eip
        );
        logbookParameters.putParameterValue(
            LogbookParameterName.outcomeDetail,
            contractsImportEventCode + "." + StatusCode.STARTED
        );
        logbookClient.create(logbookParameters);
    }

    /**
     * log update start process
     *
     * @throws VitamException
     */
    public void logUpdateStarted(String id) throws VitamException {
        final LogbookOperationParameters logbookParameters = LogbookParameterHelper.newLogbookOperationParameters(
            eip,
            contractUpdateEventCode,
            eip,
            LogbookTypeProcess.MASTERDATA,
            StatusCode.STARTED,
            VitamLogbookMessages.getCodeOp(contractUpdateEventCode, StatusCode.STARTED),
            eip
        );
        logbookParameters.putParameterValue(
            LogbookParameterName.outcomeDetail,
            contractUpdateEventCode + "." + StatusCode.STARTED
        );
        if (null != id && !id.isEmpty()) {
            logbookParameters.putParameterValue(LogbookParameterName.objectIdentifier, id);
        }
        logbookClient.create(logbookParameters);
    }

    /**
     * log end success process
     *
     * @throws VitamException
     */
    public void logSuccess() throws VitamException {
        final GUID eipUsage = GUIDFactory.newOperationLogbookGUID(ParameterHelper.getTenantParameter());
        final LogbookOperationParameters logbookParameters = LogbookParameterHelper.newLogbookOperationParameters(
            eipUsage,
            contractsImportEventCode,
            eip,
            LogbookTypeProcess.MASTERDATA,
            StatusCode.OK,
            VitamLogbookMessages.getCodeOp(contractsImportEventCode, StatusCode.OK),
            eip
        );
        logbookParameters.putParameterValue(
            LogbookParameterName.outcomeDetail,
            contractsImportEventCode + "." + StatusCode.OK
        );
        logbookClient.update(logbookParameters);
    }

    public void logUpdateSuccess(String id, String identifier, List<String> listDiffs) throws VitamException {
        final ObjectNode evDetData = JsonHandler.createObjectNode();
        final ObjectNode evDetDataContract = JsonHandler.createObjectNode();
        final String diffs = listDiffs.stream().reduce("", String::concat);

        final ObjectNode msg = JsonHandler.createObjectNode();
        msg.put(EVDETDATA_IDENTIFIER, identifier);
        msg.put(UPDATED_DIFFS, diffs);
        evDetDataContract.set(id, msg);
        evDetData.set(this.collectionType, msg);

        final String wellFormedJson = SanityChecker.sanitizeJson(evDetData);
        final GUID eipUsage = GUIDFactory.newOperationLogbookGUID(ParameterHelper.getTenantParameter());
        final LogbookOperationParameters logbookParameters = LogbookParameterHelper.newLogbookOperationParameters(
            eipUsage,
            contractUpdateEventCode,
            eip,
            LogbookTypeProcess.MASTERDATA,
            StatusCode.OK,
            VitamLogbookMessages.getCodeOp(contractUpdateEventCode, StatusCode.OK),
            eip
        );
        if (null != id && !id.isEmpty()) {
            logbookParameters.putParameterValue(LogbookParameterName.objectIdentifier, id);
        }
        logbookParameters.putParameterValue(LogbookParameterName.eventDetailData, wellFormedJson);
        logbookParameters.putParameterValue(
            LogbookParameterName.outcomeDetail,
            contractUpdateEventCode + "." + StatusCode.OK
        );
        logbookClient.update(logbookParameters);
    }
}
