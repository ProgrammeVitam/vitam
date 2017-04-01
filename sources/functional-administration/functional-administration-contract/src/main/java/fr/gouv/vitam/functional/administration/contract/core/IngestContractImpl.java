package fr.gouv.vitam.functional.administration.contract.core;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.bson.conversions.Bson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.VitamAutoCloseable;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.functional.administration.common.IngestContract;
import fr.gouv.vitam.functional.administration.common.ContractStatus;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.functional.administration.contract.core.ContractValidator.RejectionCause;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;

public class IngestContractImpl implements VitamAutoCloseable {

    private static final String CONTRACTS_IMPORT_EVENT = "STP_IMPORT_INGEST_CONTRACT";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IngestContractImpl.class);
    private static final String ERR_INVALID_CONTRACT_FILE = "The input file structure is invalid";
    private final MongoDbAccessAdminImpl mongoAccess;
    private LogbookOperationsClient logBookclient;

    private static List<ContractValidator> validators = Arrays.asList(
        createMandatoryParamsValidator(), createWrongFieldFormatValidator(),
        createDuplicateInFileValidator(), createCheckDuplicateInDatabaseValidator());

    /**
     * Constructor
     *
     * @param dbConfiguration
     */
    public IngestContractImpl(MongoDbAccessAdminImpl dbConfiguration) {
        mongoAccess = dbConfiguration;
        this.logBookclient = LogbookOperationsClientFactory.getInstance().getClient();
    }


    /**
     * Import a set of contracts after passing the validation steps @see {@link ContractValidator} and </BR>
     * {@link IngestContractImpl#validators}
     * 
     * @param contractsToImport
     * @return 
     * @throws InvalidParseOperationException
     * @throws ReferentialException
     */
    public List<JsonNode> importContracts(ArrayNode contractsToImport)
        throws InvalidParseOperationException, ReferentialException {

        SanityChecker.checkJsonAll(contractsToImport);
        Map<String, JsonNode> contractsToPersist = new HashMap<>();
        Map<IngestContract, RejectionCause> wrongContracts = new HashMap<>(0);

        GUID logbookOperationId = logStarted();

        for (JsonNode contractAsJson : contractsToImport) {
            try {
                IngestContract contractToValidate = JsonHandler.getFromJsonNode(contractAsJson, IngestContract.class);
                if (validateContract(contractsToPersist, wrongContracts, contractToValidate)) {
                    /* contract is valid, add it to the list to persist */
                    if (contractsToPersist == null) {
                        contractsToPersist = new HashMap<>();
                    }
                    contractToValidate
                        .setId(GUIDFactory.newIngestContractGUID(ParameterHelper.getTenantParameter()).getId());
                    contractsToPersist.put(contractToValidate.getName(),
                        JsonHandler.getFromString(contractToValidate.toJson()));
                }
            } catch (InvalidParseOperationException poe) {
                // invalid contract json at parse
                logValidationError(logbookOperationId, poe.getMessage());
                throw new ReferentialException(ERR_INVALID_CONTRACT_FILE);

            }
        }
        if (!wrongContracts.isEmpty()) {
            // log book + application log
            // stop
            String errorsDetails =
                wrongContracts.values().stream().map(c -> c.getReason()).collect(Collectors.joining(","));
            logValidationError(logbookOperationId, errorsDetails);
            throw new ReferentialException(errorsDetails);
        }
        // store if all contracts are are valid only
        if (contractsToPersist.size() == contractsToImport.size()) {
            // persist in collection
            // contractsToPersist.values().stream().map();
            mongoAccess.insertDocuments(JsonHandler.createArrayNode().addAll(contractsToPersist.values()),
                FunctionalAdminCollections.INGEST_CONTRACT);
            logSuccess(logbookOperationId);
            return new ArrayList<>(contractsToPersist.values());
        }
        return Collections.emptyList();
    }


    private boolean validateContract(Map<String, JsonNode> contractsToPersist,
        Map<IngestContract, RejectionCause> wrongContracts, IngestContract contract) {
        for (ContractValidator validator : validators) {
            Optional<RejectionCause> result = validator.validate(contract, contractsToPersist);
            if (result.isPresent()) {
                // there is a validation error on this contract
                /* contract is valid, add it to the list to persist */
                wrongContracts.put(contract, result.get());
                // once a validation error is detected on a contract, jump to next contract
                return false;
            }
        }
        return true;
    }


    /**
     * @param errorsDetails
     */
    private void logValidationError(GUID logbookOperationId, String errorsDetails) {
        LOGGER.error("There validation errors on the input file {}", errorsDetails);
        final GUID eventId = GUIDFactory.newOperationLogbookGUID(ParameterHelper.getTenantParameter());
        final LogbookOperationParameters logbookParametersStart = LogbookParametersFactory
            .newLogbookOperationParameters(eventId, CONTRACTS_IMPORT_EVENT, logbookOperationId,
                LogbookTypeProcess.MASTERDATA,
                StatusCode.KO,
                VitamLogbookMessages.getCodeOp(CONTRACTS_IMPORT_EVENT, StatusCode.KO), logbookOperationId);
        updateLogBookEntry(logbookParametersStart);
    }

    private GUID logStarted() {
        final GUID eip = GUIDFactory.newOperationLogbookGUID(ParameterHelper.getTenantParameter());
        final LogbookOperationParameters logbookParametersStart = LogbookParametersFactory
            .newLogbookOperationParameters(eip, CONTRACTS_IMPORT_EVENT, eip, LogbookTypeProcess.MASTERDATA,
                StatusCode.STARTED,
                VitamLogbookMessages.getCodeOp(CONTRACTS_IMPORT_EVENT, StatusCode.STARTED), eip);
        createLogBookEntry(logbookParametersStart);

        return eip;
    }

    private void logSuccess(GUID logbookOperationId) {
        final GUID eventId = GUIDFactory.newOperationLogbookGUID(ParameterHelper.getTenantParameter());
        final LogbookOperationParameters logbookParametersStart = LogbookParametersFactory
            .newLogbookOperationParameters(eventId, CONTRACTS_IMPORT_EVENT, logbookOperationId,
                LogbookTypeProcess.MASTERDATA,
                StatusCode.OK, VitamLogbookMessages.getCodeOp(CONTRACTS_IMPORT_EVENT, StatusCode.OK),
                logbookOperationId);
        updateLogBookEntry(logbookParametersStart);
    }

    /**
     * Create a LogBook Entry related to object's creation
     *
     * @param logbookParametersStart
     */
    private void createLogBookEntry(LogbookOperationParameters logbookParametersStart) {
        try {
            this.logBookclient.create(logbookParametersStart);
        } catch (LogbookClientBadRequestException | LogbookClientAlreadyExistsException |
            LogbookClientServerException e) {
            LOGGER.error(e.getMessage());
        }
    }

    private void updateLogBookEntry(LogbookOperationParameters logbookParametersStart) {
        try {
            this.logBookclient.update(logbookParametersStart);
        } catch (LogbookClientBadRequestException |
            LogbookClientServerException | LogbookClientNotFoundException e) {
            LOGGER.error(e.getMessage());
        }
    }

    private static ContractValidator createMandatoryParamsValidator() {
        return (contract, inputList) -> {
            RejectionCause rejection = null;
            if (contract.getName() == null || contract.getName().trim().isEmpty()) {
                rejection = RejectionCause.rejectMandatoryMissing(contract, IngestContract.NAME);
            }
            if (contract.getStatus() == null) {
                contract.setStatus(ContractStatus.INACTIVE);
                // FIXME
                String now = new Date().toString();
                contract.setCreationdate(now);
                contract.setDeactivationdate(now);
                contract.setActivationdate(null);
            }
            return (rejection == null) ? Optional.empty() : Optional.of(rejection);
        };
    }

    private static ContractValidator createWrongFieldFormatValidator() {
        return (contract, inputList) -> {
            RejectionCause rejection = null;
            String now = LocalDateUtil.now().toString();
            if (contract.getStatus() == null) {
                contract.setStatus(ContractStatus.INACTIVE);
                contract.setCreationdate(now);
                contract.setDeactivationdate(now);
                contract.setActivationdate(null);
            } else {
                contract.setCreationdate(now);
                contract.setDeactivationdate(null);
                contract.setActivationdate((contract.getStatus() == ContractStatus.ACTIVE) ? now : null);
                contract.setDeactivationdate((contract.getStatus() == ContractStatus.INACTIVE) ? now : null);
            }
            return (rejection == null) ? Optional.empty() : Optional.of(rejection);
        };
    }


    private static ContractValidator createDuplicateInFileValidator() {
        return (contract, inputList) -> {
            RejectionCause rejection = null;
            if (inputList != null && inputList.containsKey(contract.getName())) {
                rejection = RejectionCause.rejectDuplicatedEntry(contract);
            }
            return (rejection == null) ? Optional.empty() : Optional.of(rejection);
        };
    }


    private static ContractValidator createCheckDuplicateInDatabaseValidator() {
        return (contract, inputList) -> {
            RejectionCause rejection = null;
            int tenant = ParameterHelper.getTenantParameter();
            Bson clause = and(eq(VitamDocument.TENANT_ID, tenant), eq(IngestContract.NAME, contract.getName()));
            boolean exist = FunctionalAdminCollections.INGEST_CONTRACT.getCollection().count(clause) > 0;
            if (exist) {
                rejection = RejectionCause.rejectDuplicatedInDatabase(contract);
            }
            return (rejection == null) ? Optional.empty() : Optional.of(rejection);
        };
    }

    @Override
    public void close() {
        logBookclient.close();
    }


}
