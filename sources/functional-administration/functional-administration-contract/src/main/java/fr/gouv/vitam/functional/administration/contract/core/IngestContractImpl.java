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
import static com.mongodb.client.model.Filters.in;
import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTIONARGS.UNITTYPE;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response;

import fr.gouv.vitam.common.StringUtils;
import fr.gouv.vitam.functional.administration.common.AccessContract;
import org.bson.conversions.Bson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ParametersChecker;
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
import fr.gouv.vitam.common.model.AbstractContractModel;
import fr.gouv.vitam.common.model.ContractStatus;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.functional.administration.client.model.IngestContractModel;
import fr.gouv.vitam.functional.administration.common.IngestContract;
import fr.gouv.vitam.functional.administration.common.Profile;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.functional.administration.contract.api.ContractService;
import fr.gouv.vitam.functional.administration.counter.SequenceType;
import fr.gouv.vitam.functional.administration.counter.VitamCounterService;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationsClientHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;

public class IngestContractImpl implements ContractService<IngestContractModel> {

    private static final String THE_INGEST_CONTRACT_STATUS_MUST_BE_ACTIVE_OR_INACTIVE_BUT_NOT =
        "The Ingest contract status must be ACTIVE or INACTIVE but not ";
    private static final String INGEST_CONTRACT_NOT_FIND = "Ingest contract not find";
    private static final String CONTRACT_IS_MANDATORY_PATAMETER = "The collection of ingest contracts is mandatory";
    private static final String CONTRACTS_IMPORT_EVENT = "STP_IMPORT_INGEST_CONTRACT";
    private static final String CONTRACT_UPDATE_EVENT = "STP_UPDATE_INGEST_CONTRACT";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IngestContractImpl.class);
    private final MongoDbAccessAdminImpl mongoAccess;
    private final LogbookOperationsClient logBookclient;

    private static final String FILING_UNIT = "FILING_UNIT";
    private final VitamCounterService vitamCounterService;

    /**
     * Constructor
     *
     * @param dbConfiguration
     * @param vitamCounterService
     */
    public IngestContractImpl(MongoDbAccessAdminImpl dbConfiguration, VitamCounterService vitamCounterService) {
        mongoAccess = dbConfiguration;
        this.vitamCounterService = vitamCounterService;
        logBookclient = LogbookOperationsClientFactory.getInstance().getClient();
    }



    @Override
    public RequestResponse<IngestContractModel> createContracts(List<IngestContractModel> contractModelList)
        throws VitamException {
        ParametersChecker.checkParameter(CONTRACT_IS_MANDATORY_PATAMETER, contractModelList);

        if (contractModelList.isEmpty()) {
            return new RequestResponseOK<>();
        }
        boolean slaveMode = vitamCounterService
            .isSlaveFunctionnalCollectionOnTenant(SequenceType.INGEST_CONTRACT_SEQUENCE.getCollection(),
                ParameterHelper.getTenantParameter());
        final IngestContractImpl.IngestContractManager manager =
            new IngestContractImpl.IngestContractManager(logBookclient);

        manager.logStarted();

        final Set<String> contractNames = new HashSet<>();
        ArrayNode contractsToPersist = null;

        final VitamError error =
            new VitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem()).setHttpCode(Response.Status.BAD_REQUEST
                .getStatusCode());

        try (MetaDataClient metadataClient = MetaDataClientFactory.getInstance().getClient()) {

            for (final IngestContractModel acm : contractModelList) {


                // if a contract have and id
                if (null != acm.getId()) {
                    error.addToErrors(new VitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem()).setMessage(
                        GenericContractValidator.GenericRejectionCause.rejectIdNotAllowedInCreate(acm.getName())
                            .getReason()));
                    continue;
                }

                // if a contract with the same name is already treated mark the current one as duplicated
                if (contractNames.contains(acm.getName())) {
                    error
                        .addToErrors(new VitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem()).setMessage(
                            GenericContractValidator.GenericRejectionCause.rejectDuplicatedEntry(acm.getName())
                                .getReason()));
                    continue;
                }

                final String filingParentId = acm.getFilingParentId();
                if (filingParentId != null) {
                    if (!checkIfAUInFilingSchema(metadataClient, filingParentId)) {
                        error
                            .addToErrors(new VitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem()).setMessage(
                                GenericContractValidator.GenericRejectionCause
                                    .rejectWrongFilingParentId(filingParentId)
                                    .getReason()));
                        continue;
                    }
                }

                // mark the current contract as treated
                contractNames.add(acm.getName());

                // validate contract
                if (manager.validateContract(acm, acm.getName(), error)) {

                    acm.setId(GUIDFactory.newIngestContractGUID(ParameterHelper.getTenantParameter()).getId());
                    final JsonNode ingestContractModel = JsonHandler.toJsonNode(acm);

                }
                if (slaveMode) {
                    final Optional<GenericContractValidator.GenericRejectionCause> result =
                        manager.checkDuplicateInIdentifierSlaveModeValidator().validate(acm, acm.getIdentifier());
                    result.ifPresent(t -> error
                        .addToErrors(new VitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem()).setMessage(result
                            .get().getReason())));
                }

            }

            if (null != error.getErrors() && !error.getErrors().isEmpty()) {
                // log book + application log
                // stop
                final String errorsDetails =
                    error.getErrors().stream().map(c -> c.getMessage()).collect(Collectors.joining(","));
                manager.logValidationError(errorsDetails, CONTRACTS_IMPORT_EVENT);
                return error;
            }
            contractsToPersist = JsonHandler.createArrayNode();
            for (final IngestContractModel acm : contractModelList) {
                if (!slaveMode) {
                    final String code = vitamCounterService
                        .getNextSequenceAsString(ParameterHelper.getTenantParameter(),
                            SequenceType.INGEST_CONTRACT_SEQUENCE.getName());
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
            // use IngestContract at this point
            mongoAccess.insertDocuments(contractsToPersist, FunctionalAdminCollections.INGEST_CONTRACT).close();

        } catch (final Exception exp) {
            final String err =
                new StringBuilder("Import ingest contracts error > ").append(exp.getMessage()).toString();
            manager.logFatalError(err);
            return error.setCode(VitamCode.GLOBAL_INTERNAL_SERVER_ERROR.getItem()).setDescription(err).setHttpCode(
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        }

        manager.logSuccess();


        return new RequestResponseOK<IngestContractModel>().addAllResults(contractModelList)
            .setHttpCode(Response.Status.CREATED.getStatusCode());
    }

    @Override
    public IngestContractModel findOne(String id)
        throws ReferentialException, InvalidParseOperationException {
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
                FunctionalAdminCollections.INGEST_CONTRACT)) {
            final List<IngestContractModel> list = result.getDocuments(IngestContract.class, IngestContractModel.class);
            if (list.isEmpty()) {
                return null;
            }
            return list.get(0);
        }
    }

    @Override
    public RequestResponseOK<IngestContractModel> findContracts(JsonNode queryDsl)
        throws ReferentialException, InvalidParseOperationException {
        SanityChecker.checkJsonAll(queryDsl);
        try (DbRequestResult result =
            mongoAccess.findDocuments(queryDsl, FunctionalAdminCollections.INGEST_CONTRACT)) {
            return result.getRequestResponseOK(queryDsl, IngestContract.class, IngestContractModel.class);
        }
    }

    /**
     * Contract validator and logBook manager
     */
    protected final static class IngestContractManager {

        private static List<GenericContractValidator<IngestContractModel>> validators = Arrays.asList(
            createMandatoryParamsValidator(), createWrongFieldFormatValidator(),
            createCheckDuplicateInDatabaseValidator(), createCheckProfilesExistsInDatabaseValidator());

        final LogbookOperationsClientHelper helper = new LogbookOperationsClientHelper();
        private GUID eip = null;

        private final LogbookOperationsClient logBookclient;

        public IngestContractManager(LogbookOperationsClient logBookclient) {
            this.logBookclient = logBookclient;
        }

        private boolean validateContract(IngestContractModel contract, String jsonFormat,
            VitamError error) {

            for (final GenericContractValidator validator : validators) {
                final Optional<GenericContractValidator.GenericRejectionCause> result =
                    validator.validate(contract, jsonFormat);
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
        private void logValidationError(final String errorsDetails, final String eventType) throws VitamException {
            LOGGER.error("There validation errors on the input file {}", errorsDetails);
            final LogbookOperationParameters logbookParameters = LogbookParametersFactory
                .newLogbookOperationParameters(eip, eventType, eip, LogbookTypeProcess.MASTERDATA,
                    StatusCode.KO,
                    VitamLogbookMessages.getCodeOp(eventType, StatusCode.KO), eip);
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
                    object.put("ingestContractCheck", errorsDetails);

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

        private void logUpdateSuccess(String id, String updateEventDetailData, String oldValue) throws VitamException {
            final ObjectNode evDetData = JsonHandler.createObjectNode();
            final ObjectNode msg = JsonHandler.createObjectNode();
            msg.put("updateField", "Status");
            msg.put("oldValue", oldValue);
            msg.put("newValue", updateEventDetailData);
            evDetData.put("IngestContract", msg);
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
         * Validate that contract have not a missing mandatory parameter
         *
         * @return GenericContractValidator
         */
        private static GenericContractValidator<IngestContractModel> createMandatoryParamsValidator() {
            return (contract, jsonFormat) -> {
                GenericContractValidator.GenericRejectionCause rejection = null;
                if (contract.getName() == null || contract.getName().trim().isEmpty()) {
                    rejection =
                        GenericContractValidator.GenericRejectionCause.rejectMandatoryMissing(IngestContract.NAME);
                }

                return rejection == null ? Optional.empty() : Optional.of(rejection);
            };
        }

        /**
         * Set a default value if null
         *
         * @return GenericContractValidator
         */
        private static GenericContractValidator createWrongFieldFormatValidator() {
            return (contract, inputList) -> {
                GenericContractValidator.GenericRejectionCause rejection = null;
                final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");


                final String now = LocalDateUtil.now().toString();
                if (contract.getStatus() == null || contract.getStatus().isEmpty()) {
                    contract.setStatus(ContractStatus.INACTIVE.name());
                }

                if (!contract.getStatus().equals(ContractStatus.ACTIVE.name()) &&
                    !contract.getStatus().equals(ContractStatus.INACTIVE.name())) {
                    LOGGER.error("Error ingest contract status not valide (must be ACTIVE or INACTIVE");
                    rejection =
                        GenericContractValidator.GenericRejectionCause
                            .rejectMandatoryMissing("Status " + contract.getStatus() +
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
                    LOGGER.error("Error ingest contract parse dates", e);
                    rejection = GenericContractValidator.GenericRejectionCause.rejectMandatoryMissing("Creationdate");
                }
                try {
                    if (contract.getActivationdate() == null || contract.getActivationdate().trim().isEmpty()) {
                        contract.setActivationdate(now);
                    } else {
                        contract.setActivationdate(
                            LocalDate.parse(contract.getActivationdate(), formatter).atStartOfDay().toString());

                    }
                } catch (final Exception e) {
                    LOGGER.error("Error ingest contract parse dates", e);
                    rejection = GenericContractValidator.GenericRejectionCause.rejectMandatoryMissing("ActivationDate");
                }
                try {

                    if (contract.getDeactivationdate() == null || contract.getDeactivationdate().trim().isEmpty()) {
                        contract.setDeactivationdate(null);
                    } else {

                        contract.setDeactivationdate(
                            LocalDate.parse(contract.getDeactivationdate(), formatter).atStartOfDay().toString());
                    }
                } catch (final Exception e) {
                    LOGGER.error("Error ingest contract parse dates", e);
                    rejection =
                        GenericContractValidator.GenericRejectionCause.rejectMandatoryMissing("deactivationdate");
                }

                contract.setLastupdate(now);

                return rejection == null ? Optional.empty() : Optional.of(rejection);
            };
        }


        /**
         * Check if the contract the same name already exists in database
         *
         * @return GenericContractValidator
         */
        private static GenericContractValidator createCheckDuplicateInDatabaseValidator() {
            return (contract, contractName) -> {
                GenericContractValidator.GenericRejectionCause rejection = null;
                final int tenant = ParameterHelper.getTenantParameter();
                final Bson clause =
                    and(eq(VitamDocument.TENANT_ID, tenant), eq(IngestContract.NAME, contract.getName()));
                final boolean exist = FunctionalAdminCollections.INGEST_CONTRACT.getCollection().count(clause) > 0;
                if (exist) {
                    rejection = GenericContractValidator.GenericRejectionCause.rejectDuplicatedInDatabase(contractName);
                }
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
                if (contractName == null || contractName.isEmpty()) {
                    return Optional
                        .of(GenericContractValidator.GenericRejectionCause.rejectMandatoryMissing(contractName));
                }
                GenericContractValidator.GenericRejectionCause rejection = null;
                final int tenant = ParameterHelper.getTenantParameter();
                final Bson clause =
                    and(eq(VitamDocument.TENANT_ID, tenant), eq(AccessContract.IDENTIFIER, contract.getIdentifier()));
                final boolean exist = FunctionalAdminCollections.INGEST_CONTRACT.getCollection().count(clause) > 0;
                if (exist) {
                    rejection = GenericContractValidator.GenericRejectionCause.rejectDuplicatedInDatabase(contractName);
                }
                return rejection == null ? Optional.empty() : Optional.of(rejection);
            };
        }

        /**
         * Check if the profiles exists bien dans la base de données
         *
         * @return GenericContractValidator
         */
        public static GenericContractValidator<IngestContractModel> createCheckProfilesExistsInDatabaseValidator() {
            return (contract, contractName) -> {
                if (null == contract.getArchiveProfiles()) {
                    return Optional.empty();
                }
                GenericContractValidator.GenericRejectionCause rejection = null;
                final int tenant = ParameterHelper.getTenantParameter();
                final Bson clause =
                    and(eq(VitamDocument.TENANT_ID, tenant), in(Profile.IDENTIFIER, contract.getArchiveProfiles()));
                final long count = FunctionalAdminCollections.PROFILE.getCollection().count(clause);
                if (count != contract.getArchiveProfiles().size()) {
                    rejection =
                        GenericContractValidator.GenericRejectionCause
                            .rejectArchiveProfileNotFoundInDatabase(contractName);
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
    public RequestResponse<IngestContractModel> updateContract(String id, JsonNode queryDsl)
        throws VitamException {
        final VitamError error =
            new VitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem()).setHttpCode(Response.Status.BAD_REQUEST
                .getStatusCode());

        if (queryDsl == null || !queryDsl.isObject()) {
            return error;
        }

        final IngestContractModel ingestContractModel = findOne(id);
        if (ingestContractModel == null) {
            return error.addToErrors(new VitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem()).setMessage(
                INGEST_CONTRACT_NOT_FIND + id));
        }
        final IngestContractManager manager = new IngestContractManager(logBookclient);
        manager.logUpdateStarted(ingestContractModel.getId());

        final JsonNode actionNode = queryDsl.get(GLOBAL.ACTION.exactToken());
        String updateStatus = null;
        for (final JsonNode fieldToSet : actionNode) {
            final JsonNode fieldName = fieldToSet.get(UPDATEACTION.SET.exactToken());
            if (fieldName != null) {
                final Iterator<String> it = fieldName.fieldNames();
                while (it.hasNext()) {
                    final String field = it.next();
                    final JsonNode value = fieldName.findValue(field);
                    if (AbstractContractModel.STATUS.equals(field)) {
                        if (!(ContractStatus.ACTIVE.name().equals(value.asText()) || ContractStatus.INACTIVE
                            .name().equals(value.asText()))) {
                            error.addToErrors(new VitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem())
                                .setMessage(THE_INGEST_CONTRACT_STATUS_MUST_BE_ACTIVE_OR_INACTIVE_BUT_NOT +
                                    value.asText()));
                        }
                        updateStatus = value.asText();
                    }
                }
            }
        }

        try (MetaDataClient metadataClient = MetaDataClientFactory.getInstance().getClient()) {
            if (queryDsl.findValue(IngestContractModel.FILING_PARENT_ID) != null) {
                final String filingParentId = queryDsl.findValue(IngestContractModel.FILING_PARENT_ID).asText();
                if (filingParentId != null) {
                    if (!checkIfAUInFilingSchema(metadataClient, filingParentId)) {
                        error
                            .addToErrors(new VitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem()).setMessage(
                                GenericContractValidator.GenericRejectionCause
                                    .rejectWrongFilingParentId(filingParentId)
                                    .getReason()));

                        final String errorsDetails =
                            error.getErrors().stream().map(c -> c.getMessage()).collect(Collectors.joining(","));
                        manager.logValidationError(errorsDetails, CONTRACT_UPDATE_EVENT);
                    }
                }
            }

            final JsonNode archiveProfilesNode = queryDsl.findValue(IngestContractModel.ARCHIVE_PROFILES);
            if (archiveProfilesNode != null) {
                final Set<String> archiveProfiles =
                    JsonHandler.getFromString(archiveProfilesNode.toString(), Set.class, String.class);
                final GenericContractValidator<IngestContractModel> validator =
                    IngestContractManager.createCheckProfilesExistsInDatabaseValidator();
                final Optional<GenericContractValidator.GenericRejectionCause> result =
                    validator.validate(new IngestContractModel().setArchiveProfiles(archiveProfiles),
                        "update contract ..");
                if (result.isPresent()) {
                    // there is a validation error on this contract
                    /* contract is valid, add it to the list to persist */
                    error.addToErrors(new VitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem()).setMessage(result
                        .get().getReason()));
                }
            }

            if (error.getErrors() != null && error.getErrors().size() > 0) {
                final String errorsDetails =
                    error.getErrors().stream().map(c -> c.getMessage()).collect(Collectors.joining(","));
                manager.logValidationError(errorsDetails, CONTRACT_UPDATE_EVENT);

                return error;
            }
            mongoAccess.updateData(queryDsl, FunctionalAdminCollections.INGEST_CONTRACT).close();
        } catch (ReferentialException | InvalidCreateOperationException e) {
            final String err = new StringBuilder("Update ingest contracts error > ").append(e.getMessage()).toString();
            manager.logFatalError(err);
            error.setCode(VitamCode.GLOBAL_INTERNAL_SERVER_ERROR.getItem())
                .setDescription(err)
                .setHttpCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());

            return error;
        }
        manager.logUpdateSuccess(ingestContractModel.getId(), updateStatus, ingestContractModel.getStatus());
        return new RequestResponseOK<>();
    }

    private boolean checkIfAUInFilingSchema(MetaDataClient metadataClient, String id)
        throws InvalidCreateOperationException, MetaDataExecutionException, MetaDataDocumentSizeException,
        MetaDataClientServerException, InvalidParseOperationException {
        final Select select = new Select();
        select.setQuery(QueryHelper.eq(UNITTYPE.exactToken(), FILING_UNIT).setDepthLimit(0));
        final JsonNode queryDsl = select.getFinalSelect();
        // if the filing id is in the filing schema
        if (metadataClient.selectUnitbyId(queryDsl, id).get("$hits").get("size").asInt() == 0) {
                return false;
            } else {
                return true;
            }
    }
}
