/**
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
package fr.gouv.vitam.functional.administration.contract.core;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response;

import fr.gouv.vitam.functional.administration.counter.SequenceType;
import org.bson.conversions.Bson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.SedaConfiguration;
import fr.gouv.vitam.common.SedaVersion;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.GLOBAL;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.UPDATEACTION;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.parser.request.adapter.SingleVarNameAdapter;
import fr.gouv.vitam.common.database.parser.request.single.SelectParserSingle;
import fr.gouv.vitam.common.database.server.DbRequestResult;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.model.administration.ContractStatus;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.functional.administration.common.AccessContract;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.functional.administration.contract.api.ContractService;
import fr.gouv.vitam.functional.administration.contract.core.GenericContractValidator.GenericRejectionCause;
import fr.gouv.vitam.functional.administration.counter.VitamCounterService;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationsClientHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;

public class AccessContractImpl implements ContractService<AccessContractModel> {

    private static final String DATA_OBJECT_VERSION_INVALID = "Data object version invalid";
    private static final String THE_ACCESS_CONTRACT_EVERY_DATA_OBJECT_VERSION_MUST_BE_TRUE_OR_FALSE_BUT_NOT =
        "The Access contract EveryDataObjectVersion must be true or false but not ";
    private static final String THE_ACCESS_CONTRACT_EVERY_ORIGINATING_AGENCY_MUST_BE_TRUE_OR_FALSE_BUT_NOT =
        "The Access contract EveryOriginatingAgency must be true or false but not ";
    private static final String THE_ACCESS_CONTRACT_STATUS_MUST_BE_ACTIVE_OR_INACTIVE_BUT_NOT =
        "The Access contract status must be ACTIVE or INACTIVE but not ";
    private static final String ACCESS_CONTRACT_NOT_FIND = "Access contract not find";
    private static final String ACCESS_CONTRACT_IS_MANDATORY_PATAMETER =
        "The collection of access contracts is mandatory";
    private static final String UPDATE_ACCESS_CONTRACT_MANDATORY_PATAMETER = "access contracts is mandatory";
    private static final String CONTRACTS_IMPORT_EVENT = "STP_IMPORT_ACCESS_CONTRACT";
    private static final String CONTRACT_UPDATE_EVENT = "STP_UPDATE_ACCESS_CONTRACT";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AccessContractImpl.class);
    private final MongoDbAccessAdminImpl mongoAccess;
    private final LogbookOperationsClient logBookclient;
    private final VitamCounterService vitamCounterService;


    /**
     * Constructor
     *
     * @param mongoAccess         MongoDB client
     * @param vitamCounterService
     */
    public AccessContractImpl(MongoDbAccessAdminImpl mongoAccess, VitamCounterService vitamCounterService) {
        this.mongoAccess = mongoAccess;
        this.vitamCounterService = vitamCounterService;
        logBookclient = LogbookOperationsClientFactory.getInstance().getClient();
    }

    @Override
    public RequestResponse<AccessContractModel> createContracts(List<AccessContractModel> contractModelList)
        throws VitamException {
        ParametersChecker.checkParameter(ACCESS_CONTRACT_IS_MANDATORY_PATAMETER, contractModelList);

        if (contractModelList.isEmpty()) {
            return new RequestResponseOK<>();
        }
        boolean slaveMode = vitamCounterService
            .isSlaveFunctionnalCollectionOnTenant(SequenceType.ACCESS_CONTRACT_SEQUENCE.getCollection(),
                ParameterHelper.getTenantParameter());
        final AccessContractManager manager = new AccessContractManager(logBookclient);

        manager.logStarted();

        final Set<String> contractNames = new HashSet<>();
        ArrayNode contractsToPersist = null;

        final VitamError error = new VitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem())
            .setHttpCode(Response.Status.BAD_REQUEST.getStatusCode());
        try {

            for (final AccessContractModel acm : contractModelList) {
                // if a contract have and id
                if (acm.getId() != null) {
                    error.addToErrors(new VitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem())
                        .setMessage(GenericRejectionCause.rejectIdNotAllowedInCreate(acm.getName()).getReason()));
                    continue;
                }
                // if a contract with the same name is already treated mark the current one as duplicated
                if (contractNames.contains(acm.getName())) {
                    error.addToErrors(new VitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem())
                        .setMessage(GenericRejectionCause.rejectDuplicatedEntry(acm.getName()).getReason()));
                    continue;
                }
                // mark the current contract as treated
                contractNames.add(acm.getName());
                // validate contract
                if (manager.validateContract(acm, acm.getName(), error)) {
                    // TODO: 5/16/17 newIngestContractGUID used for access contract, should create
                    // newAccessContractGUID?
                    acm.setId(GUIDFactory.newIngestContractGUID(ParameterHelper.getTenantParameter()).getId());
                }
                
                if(acm.getTenant() == null) {
                    acm.setTenant(ParameterHelper.getTenantParameter());
                }

                if (slaveMode) {
                    final Optional<GenericContractValidator.GenericRejectionCause> result =
                        manager.checkDuplicateInIdentifierSlaveModeValidator().validate(acm, acm.getIdentifier());
                    result.ifPresent(genericRejectionCause -> error
                        .addToErrors(new VitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem())
                            .setMessage(genericRejectionCause.getReason())));
                }

            }
            if (error.getErrors() != null && !error.getErrors().isEmpty()) {
                // log book + application log
                // stop
                final String errorsDetails =
                    error.getErrors().stream().map(VitamError::getMessage).collect(Collectors.joining(","));
                manager.logValidationError(errorsDetails);
                return error;
            }

            contractsToPersist = JsonHandler.createArrayNode();
            for (final AccessContractModel acm : contractModelList) {

                if (!slaveMode) {
                    final String code =
                        vitamCounterService.getNextSequenceAsString(ParameterHelper.getTenantParameter(),
                            SequenceType.ACCESS_CONTRACT_SEQUENCE.getName());
                    acm.setIdentifier(code);
                }

                final JsonNode accessContractNode = JsonHandler.toJsonNode(acm);

                /* contract is valid, add it to the list to persist */
                contractsToPersist.add(accessContractNode);
            }

            // at this point no exception occurred and no validation error detected
            // persist in collection
            // contractsToPersist.values().stream().map();
            // TODO: 3/28/17 create insertDocuments method that accepts VitamDocument instead of ArrayNode, so we can
            // use AccessContract at this point
            mongoAccess.insertDocuments(contractsToPersist, FunctionalAdminCollections.ACCESS_CONTRACT).close();
        } catch (final Exception exp) {
            final String err =
                new StringBuilder("Import access contracts error > ").append(exp.getMessage()).toString();
            manager.logFatalError(err);
            return error.setCode(VitamCode.GLOBAL_INTERNAL_SERVER_ERROR.getItem()).setDescription(err).setHttpCode(
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        }

        manager.logSuccess();


        return new RequestResponseOK<AccessContractModel>().addAllResults(contractModelList)
            .setHttpCode(Response.Status.CREATED.getStatusCode());
    }

    @Override
    public AccessContractModel findOne(String id) throws ReferentialException, InvalidParseOperationException {
        SanityChecker.checkParameter(id);
        final SelectParserSingle parser = new SelectParserSingle(new SingleVarNameAdapter());
        parser.parse(new Select().getFinalSelect());
        try {
            parser.addCondition(QueryHelper.eq("Identifier", id));
        } catch (InvalidCreateOperationException e) {
            throw new ReferentialException(e);
        }
        try (DbRequestResult result =
            mongoAccess.findDocuments(parser.getRequest().getFinalSelect(),
                FunctionalAdminCollections.ACCESS_CONTRACT)) {
            final List<AccessContractModel> list = result.getDocuments(AccessContract.class, AccessContractModel.class);
            if (list.isEmpty()) {
                return null;
            }
            return list.get(0);
        }
    }

    @Override
    public RequestResponseOK<AccessContractModel> findContracts(JsonNode queryDsl)
        throws ReferentialException, InvalidParseOperationException {
        SanityChecker.checkJsonAll(queryDsl);
        try (DbRequestResult result =
            mongoAccess.findDocuments(queryDsl, FunctionalAdminCollections.ACCESS_CONTRACT)) {
            return result.getRequestResponseOK(queryDsl, AccessContract.class, AccessContractModel.class);
        }
    }

    /**
     * Contract validator and logBook manager
     */
    protected final static class AccessContractManager {

        private static final String UPDATED_DIFFS = "updatedDiffs";

        private static final String ACCESS_CONTRACT = "AccessContract";

        private static List<GenericContractValidator<AccessContractModel>> validators = Arrays.asList(
            createMandatoryParamsValidator(), createWrongFieldFormatValidator(),
            createCheckDuplicateInDatabaseValidator());

        final LogbookOperationsClientHelper helper = new LogbookOperationsClientHelper();
        private GUID eip = null;

        private final LogbookOperationsClient logBookclient;

        public AccessContractManager(LogbookOperationsClient logBookclient) {
            this.logBookclient = logBookclient;
        }

        private boolean validateContract(AccessContractModel contract, String jsonFormat,
            VitamError error) {

            for (final GenericContractValidator validator : validators) {
                final Optional<GenericRejectionCause> result = validator.validate(contract, jsonFormat);
                if (result.isPresent()) {
                    // there is a validation error on this contract
                    /* contract is valid, add it to the list to persist */
                    error.addToErrors(new VitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem()).setMessage(result
                        .get().getReason()));
                    // once a validation error is detected on a contract, jump to next contract
                    return false;
                }
            }
            return true;
        }


        /**
         * Log validation error (business error)
         *
         * @param errorsDetails
         */
        private void logValidationError(String errorsDetails) throws VitamException {
            LOGGER.error("There validation errors on the input file {}", errorsDetails);
            final LogbookOperationParameters logbookParameters = LogbookParametersFactory
                .newLogbookOperationParameters(eip, CONTRACTS_IMPORT_EVENT, eip, LogbookTypeProcess.MASTERDATA,
                    StatusCode.KO,
                    VitamLogbookMessages.getCodeOp(CONTRACTS_IMPORT_EVENT, StatusCode.KO), eip);
            logbookMessageError(errorsDetails, logbookParameters);
            helper.updateDelegate(logbookParameters);
            logBookclient.bulkCreate(eip.getId(), helper.removeCreateDelegate(eip.getId()));

        }

        /**
         * Log validation error (business error)
         *
         * @param errorsDetails
         */
        private void logUpdateError(String errorsDetails) throws VitamException {
            LOGGER.error("Update document error {}", errorsDetails);
            final LogbookOperationParameters logbookParameters = LogbookParametersFactory
                .newLogbookOperationParameters(eip, CONTRACT_UPDATE_EVENT, eip, LogbookTypeProcess.MASTERDATA,
                    StatusCode.KO,
                    VitamLogbookMessages.getCodeOp(CONTRACT_UPDATE_EVENT, StatusCode.KO), eip);
            logbookMessageError(errorsDetails, logbookParameters);
            helper.updateDelegate(logbookParameters);
            logBookclient.bulkCreate(eip.getId(), helper.removeCreateDelegate(eip.getId()));

        }

        /**
         * log fatal error (system or technical error)
         *
         * @param errorsDetails
         * @throws VitamException
         */
        private void logFatalError(String errorsDetails) throws VitamException {
            LOGGER.error("There validation errors on the input file {}", errorsDetails);
            final LogbookOperationParameters logbookParameters = LogbookParametersFactory
                .newLogbookOperationParameters(eip, CONTRACTS_IMPORT_EVENT, eip, LogbookTypeProcess.MASTERDATA,
                    StatusCode.FATAL,
                    VitamLogbookMessages.getCodeOp(CONTRACTS_IMPORT_EVENT, StatusCode.FATAL), eip);
            logbookParameters.putParameterValue(LogbookParameterName.outcomeDetail, CONTRACTS_IMPORT_EVENT + "." +
                StatusCode.FATAL);
            logbookMessageError(errorsDetails, logbookParameters);
            helper.updateDelegate(logbookParameters);
            logBookclient.bulkCreate(eip.getId(), helper.removeCreateDelegate(eip.getId()));
        }

        private void logbookMessageError(String errorsDetails, LogbookOperationParameters logbookParameters) {
            if (null != errorsDetails && !errorsDetails.isEmpty()) {
                try {
                    final ObjectNode object = JsonHandler.createObjectNode();
                    object.put("accessContractCheck", errorsDetails);

                    final String wellFormedJson = SanityChecker.sanitizeJson(object);
                    logbookParameters.putParameterValue(LogbookParameterName.eventDetailData, wellFormedJson);
                } catch (final InvalidParseOperationException e) {
                    // Do nothing
                }
            }
        }

        /**
         * log start process
         *
         * @throws VitamException
         */
        private void logStarted() throws VitamException {
            eip = GUIDFactory.newOperationLogbookGUID(ParameterHelper.getTenantParameter());
            final LogbookOperationParameters logbookParameters = LogbookParametersFactory
                .newLogbookOperationParameters(eip, CONTRACTS_IMPORT_EVENT, eip, LogbookTypeProcess.MASTERDATA,
                    StatusCode.STARTED,
                    VitamLogbookMessages.getCodeOp(CONTRACTS_IMPORT_EVENT, StatusCode.STARTED), eip);
            logbookParameters.putParameterValue(LogbookParameterName.outcomeDetail, CONTRACTS_IMPORT_EVENT + "." +
                StatusCode.STARTED);
            helper.createDelegate(logbookParameters);

        }

        /**
         * log update start process
         *
         * @throws VitamException
         */
        private void logUpdateStarted(String id) throws VitamException {
            eip = GUIDFactory.newOperationLogbookGUID(ParameterHelper.getTenantParameter());
            final LogbookOperationParameters logbookParameters = LogbookParametersFactory
                .newLogbookOperationParameters(eip, CONTRACT_UPDATE_EVENT, eip, LogbookTypeProcess.MASTERDATA,
                    StatusCode.STARTED,
                    VitamLogbookMessages.getCodeOp(CONTRACT_UPDATE_EVENT, StatusCode.STARTED), eip);
            logbookParameters.putParameterValue(LogbookParameterName.outcomeDetail, CONTRACT_UPDATE_EVENT +
                "." + StatusCode.STARTED);
            if (null != id && !id.isEmpty()) {
                logbookParameters.putParameterValue(LogbookParameterName.objectIdentifier, id);
            }
            helper.createDelegate(logbookParameters);

        }

        private void logUpdateSuccess(String id, List<String> listDiffs) throws VitamException {
            final ObjectNode evDetData = JsonHandler.createObjectNode();
            final ObjectNode evDetDataContract = JsonHandler.createObjectNode();
            final String diffs = listDiffs.stream().reduce("", String::concat);

            final ObjectNode msg = JsonHandler.createObjectNode();
            msg.put(UPDATED_DIFFS, diffs);
            evDetDataContract.set(id, msg);

            evDetData.set(ACCESS_CONTRACT, msg);
            final String wellFormedJson = SanityChecker.sanitizeJson(evDetData);
            final LogbookOperationParameters logbookParameters =
                LogbookParametersFactory
                    .newLogbookOperationParameters(
                        eip,
                        CONTRACT_UPDATE_EVENT,
                        eip,
                        LogbookTypeProcess.MASTERDATA,
                        StatusCode.OK,
                        VitamLogbookMessages.getCodeOp(CONTRACT_UPDATE_EVENT, StatusCode.OK),
                        eip);

            if (null != id && !id.isEmpty()) {
                logbookParameters.putParameterValue(LogbookParameterName.objectIdentifier, id);
            }
            logbookParameters.putParameterValue(LogbookParameterName.eventDetailData,
                wellFormedJson);
            logbookParameters.putParameterValue(LogbookParameterName.outcomeDetail, CONTRACT_UPDATE_EVENT +
                "." + StatusCode.OK);
            helper.updateDelegate(logbookParameters);
            logBookclient.bulkCreate(eip.getId(), helper.removeCreateDelegate(eip.getId()));
        }


        /**
         * log end success process
         *
         * @throws VitamException
         */
        private void logSuccess() throws VitamException {
            final LogbookOperationParameters logbookParameters = LogbookParametersFactory
                .newLogbookOperationParameters(eip, CONTRACTS_IMPORT_EVENT, eip, LogbookTypeProcess.MASTERDATA,
                    StatusCode.OK,
                    VitamLogbookMessages.getCodeOp(CONTRACTS_IMPORT_EVENT, StatusCode.OK), eip);
            logbookParameters.putParameterValue(LogbookParameterName.outcomeDetail, CONTRACTS_IMPORT_EVENT + "." +
                StatusCode.OK);
            helper.updateDelegate(logbookParameters);
            logBookclient.bulkCreate(eip.getId(), helper.removeCreateDelegate(eip.getId()));
        }

        /**
         * Validate that contract have not a missing mandatory parameter
         *
         * @return
         */
        private static GenericContractValidator<AccessContractModel> createMandatoryParamsValidator() {
            return (contract, jsonFormat) -> {
                GenericRejectionCause rejection = null;
                if (contract.getName() == null || contract.getName().trim().isEmpty()) {
                    rejection = GenericRejectionCause.rejectMandatoryMissing(AccessContract.NAME);
                }

                return rejection == null ? Optional.empty() : Optional.of(rejection);
            };
        }

        /**
         * Set a default value if null
         *
         * @return
         */
        private static GenericContractValidator createWrongFieldFormatValidator() {
            return (contract, inputList) -> {
                GenericRejectionCause rejection = null;
                final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");


                final String now = LocalDateUtil.now().toString();
                if (contract.getStatus() == null || contract.getStatus().isEmpty()) {
                    contract.setStatus(ContractStatus.INACTIVE.name());
                }


                if (!contract.getStatus().equals(ContractStatus.ACTIVE.name()) &&
                    !contract.getStatus().equals(ContractStatus.INACTIVE.name())) {
                    LOGGER.error("Error access contract status not valide (must be ACTIVE or INACTIVE");
                    rejection =
                        GenericRejectionCause.rejectMandatoryMissing("Status " + contract.getStatus() +
                            " not valide must be ACTIVE or INACTIVE");
                }


                try {
                    if (contract.getCreationdate() == null || contract.getCreationdate().trim().isEmpty()) {
                        contract.setCreationdate(now);
                    } else {
                        contract.setCreationdate(
                            LocalDate.parse(contract.getCreationdate(), formatter).atStartOfDay().toString());
                    }

                } catch (final Exception e) {
                    LOGGER.error("Error access contract parse dates", e);
                    rejection = GenericRejectionCause.rejectMandatoryMissing("Creationdate");
                }
                try {
                    if (contract.getActivationdate() == null || contract.getActivationdate().trim().isEmpty()) {
                        contract.setActivationdate(now);
                    } else {
                        contract.setActivationdate(
                            LocalDate.parse(contract.getActivationdate(), formatter).atStartOfDay().toString());

                    }
                } catch (final Exception e) {
                    LOGGER.error("Error access contract parse dates", e);
                    rejection = GenericRejectionCause.rejectMandatoryMissing("ActivationDate");
                }
                try {

                    if (contract.getDeactivationdate() == null || contract.getDeactivationdate().trim().isEmpty()) {
                        contract.setDeactivationdate(null);
                    } else {
                        contract.setDeactivationdate(
                            LocalDate.parse(contract.getDeactivationdate(), formatter).atStartOfDay().toString());
                    }
                } catch (final Exception e) {
                    LOGGER.error("Error access contract parse dates", e);
                    rejection = GenericRejectionCause.rejectMandatoryMissing("deactivationdate");
                }

                contract.setLastupdate(now);

                return rejection == null ? Optional.empty() : Optional.of(rejection);
            };
        }

        /**
         * Check if the Id of the  contract  already exists in database
         *
         * @return
         */
        private static GenericContractValidator checkDuplicateInIdentifierSlaveModeValidator() {
            return (contract, contractName) -> {
                if (contract.getIdentifier() == null || contract.getIdentifier().isEmpty()) {
                    return Optional.of(GenericContractValidator.GenericRejectionCause
                        .rejectMandatoryMissing(AccessContract.IDENTIFIER));
                }
                GenericRejectionCause rejection = null;
                final int tenant = ParameterHelper.getTenantParameter();
                final Bson clause =
                    and(eq(VitamDocument.TENANT_ID, tenant), eq(AccessContract.IDENTIFIER, contract.getIdentifier()));
                final boolean exist = FunctionalAdminCollections.ACCESS_CONTRACT.getCollection().count(clause) > 0;
                if (exist) {
                    rejection = GenericRejectionCause.rejectDuplicatedInDatabase(contractName);
                }
                return rejection == null ? Optional.empty() : Optional.of(rejection);
            };
        }

        /**
         * Check if the contract the same name already exists in database
         *
         * @return
         */
        private static GenericContractValidator createCheckDuplicateInDatabaseValidator() {
            return (contract, contractName) -> {
                GenericRejectionCause rejection = null;
                final int tenant = ParameterHelper.getTenantParameter();
                final Bson clause =
                    and(eq(VitamDocument.TENANT_ID, tenant), eq(AccessContract.NAME, contract.getName()));
                final boolean exist = FunctionalAdminCollections.ACCESS_CONTRACT.getCollection().count(clause) > 0;
                if (exist) {
                    rejection = GenericRejectionCause.rejectDuplicatedInDatabase(contractName);
                }
                return rejection == null ? Optional.empty() : Optional.of(rejection);
            };
        }
    }


    @Override
    public void close() {
        logBookclient.close();
    }



    @Override
    public RequestResponse<AccessContractModel> updateContract(String id, JsonNode queryDsl)
        throws VitamException {
        ParametersChecker.checkParameter(UPDATE_ACCESS_CONTRACT_MANDATORY_PATAMETER, queryDsl);
        final VitamError error = new VitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem())
            .setHttpCode(Response.Status.BAD_REQUEST.getStatusCode());

        final AccessContractModel accContractModel = findOne(id);
        Map<String, List<String>> updateDiffs = new HashMap<>();
        if (accContractModel == null) {
            return error.addToErrors(new VitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem()).setMessage(
                ACCESS_CONTRACT_NOT_FIND + id));
        }
        final AccessContractManager manager = new AccessContractManager(logBookclient);
        manager.logUpdateStarted(accContractModel.getId());
        if (queryDsl == null || !queryDsl.isObject()) {
            return error;
        }

        final JsonNode actionNode = queryDsl.get(GLOBAL.ACTION.exactToken());

        for (final JsonNode fieldToSet : actionNode) {
            final JsonNode fieldName = fieldToSet.get(UPDATEACTION.SET.exactToken());
            if (fieldName != null) {
                final Iterator<String> it = fieldName.fieldNames();
                while (it.hasNext()) {
                    final String field = it.next();
                    final JsonNode value = fieldName.findValue(field);
                    validateUpdateAction(error, field, value);
                }
            }
        }

        if (error.getErrors() != null && error.getErrors().size() > 0) {
            final String errorsDetails =
                error.getErrors().stream().map(c -> c.getMessage()).collect(Collectors.joining(","));
            manager.logUpdateError(errorsDetails);

            return error;
        }

        try (DbRequestResult result = mongoAccess.updateData(queryDsl, FunctionalAdminCollections.ACCESS_CONTRACT)) {
            updateDiffs = result.getDiffs();
        } catch (final ReferentialException e) {
            final String err = new StringBuilder("Update access contracts error > ").append(e.getMessage()).toString();
            manager.logFatalError(err);
            error.setCode(VitamCode.GLOBAL_INTERNAL_SERVER_ERROR.getItem())
                .setDescription(err)
                .setHttpCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());

            return error;
        }
        manager.logUpdateSuccess(accContractModel.getId(), updateDiffs.get(accContractModel.getId()));
        return new RequestResponseOK<AccessContractModel>();
    }

    private void validateUpdateAction(final VitamError error, final String field, final JsonNode value) {
        if (AccessContract.STATUS.equals(field)) {
            if (!(ContractStatus.ACTIVE.name().equals(value.asText()) || ContractStatus.INACTIVE
                .name().equals(value.asText()))) {
                error.addToErrors(new VitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem()).setMessage(
                    THE_ACCESS_CONTRACT_STATUS_MUST_BE_ACTIVE_OR_INACTIVE_BUT_NOT + value.asText()));
            }
        }

        if (AccessContractModel.EVERY_ORIGINATINGAGENCY.equals(field)) {
            if (!(value instanceof BooleanNode)) {
                error.addToErrors(new VitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem()).setMessage(
                    THE_ACCESS_CONTRACT_EVERY_ORIGINATING_AGENCY_MUST_BE_TRUE_OR_FALSE_BUT_NOT +
                        value.asText()));
            }
        }

        if (AccessContractModel.EVERY_DATA_OBJECT_VERSION.equals(field)) {
            if (!(value instanceof BooleanNode)) {
                error.addToErrors(new VitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem()).setMessage(
                    THE_ACCESS_CONTRACT_EVERY_DATA_OBJECT_VERSION_MUST_BE_TRUE_OR_FALSE_BUT_NOT +
                        value.asText()));
            }
        }

        if (AccessContractModel.DATA_OBJECT_VERSION.equals(field)) {
            if (!validateObjectVersion(value)) {
                error.addToErrors(new VitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem()).setMessage(
                    DATA_OBJECT_VERSION_INVALID +
                        value.asText()));
            }
        }
    }



    private boolean validateObjectVersion(JsonNode value) {
        if (!value.isArray()) {
            return false;
        }

        SedaVersion sedaVersion = new SedaVersion();
        try {
            sedaVersion = SedaConfiguration.getSupportedVerion();
        } catch (IOException e) {
            LOGGER.error(e);
        }
        for (JsonNode node : value) {
            if (!sedaVersion.isSupportedVesion(node.asText())) {
                return false;
            }
        }
        return true;


    }

}
