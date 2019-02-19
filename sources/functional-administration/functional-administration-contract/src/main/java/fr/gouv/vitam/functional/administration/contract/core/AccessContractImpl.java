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
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static fr.gouv.vitam.common.model.RequestResponseOK.TAG_RESULTS;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.SedaConfiguration;
import fr.gouv.vitam.common.SedaVersion;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.GLOBAL;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTIONARGS;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.UPDATEACTION;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.parser.request.adapter.SingleVarNameAdapter;
import fr.gouv.vitam.common.database.parser.request.single.SelectParserSingle;
import fr.gouv.vitam.common.database.server.DbRequestResult;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.SchemaValidationException;
import fr.gouv.vitam.common.exception.VitamDBException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.guid.GUIDReader;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.AbstractContractModel;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.model.administration.ActivationStatus;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.AccessContract;
import fr.gouv.vitam.functional.administration.common.Agencies;
import fr.gouv.vitam.functional.administration.common.FunctionalBackupService;
import fr.gouv.vitam.functional.administration.common.VitamErrorUtils;
import fr.gouv.vitam.functional.administration.common.counter.SequenceType;
import fr.gouv.vitam.functional.administration.common.counter.VitamCounterService;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.functional.administration.contract.api.ContractService;
import fr.gouv.vitam.functional.administration.contract.core.GenericContractValidator.GenericRejectionCause;
import fr.gouv.vitam.functional.administration.format.model.FunctionalOperationModel;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
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
import fr.gouv.vitam.storage.engine.common.exception.StorageException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import org.assertj.core.util.VisibleForTesting;
import org.bson.conversions.Bson;

public class AccessContractImpl implements ContractService<AccessContractModel> {

    private static final String DATA_OBJECT_VERSION_INVALID = "Data object version invalid";
    private static final String ROOT_UNIT_INVALID = "RootUnits invalid, should be a list of guid";
    private static final String EXCLUDED_ROOT_UNIT_INVALID = "ExcludedRootUnits invalid, should be a list of guid";
    private static final String ORIGINATING_AGENCIES_INVALID = "OriginatingAgencies invalid, should be a list of guid";
    private static final String THE_ACCESS_CONTRACT_EVERY_DATA_OBJECT_VERSION_MUST_BE_TRUE_OR_FALSE_BUT_NOT =
        "The Access contract EveryDataObjectVersion must be true or false but not ";
    private static final String THE_ACCESS_CONTRACT_EVERY_ORIGINATING_AGENCY_MUST_BE_TRUE_OR_FALSE_BUT_NOT =
        "The Access contract EveryOriginatingAgency must be true or false but not ";
    private static final String THE_ACCESS_CONTRACT_STATUS_MUST_BE_ACTIVE_OR_INACTIVE_BUT_NOT =
        "The Access contract status must be ACTIVE or INACTIVE but not ";
    private static final String ACCESS_CONTRACT_NOT_FOUND = "Access contract not found";
    private static final String ACCESS_CONTRACT_IS_MANDATORY_PATAMETER =
        "The collection of access contracts is mandatory";
    private static final String UPDATE_ACCESS_CONTRACT_MANDATORY_PATAMETER = "access contracts is mandatory";
    private static final String CONTRACTS_IMPORT_EVENT = "STP_IMPORT_ACCESS_CONTRACT";
    private static final String CONTRACT_UPDATE_EVENT = "STP_UPDATE_ACCESS_CONTRACT";
    private static final String CONTRACT_ACCESS_REPORT = "CONTRACT_ACCESS_REPORT";
    /**
     * STP_BACKUP_ACCESS_CONTRACT
     */
    public static final String CONTRACT_BACKUP_EVENT = "STP_BACKUP_ACCESS_CONTRACT";

    private static final String EMPTY_REQUIRED_FIELD = "STP_IMPORT_ACCESS_CONTRACT.EMPTY_REQUIRED_FIELD.KO";
    private static final String WRONG_FIELD_FORMAT = "STP_IMPORT_ACCESS_CONTRACT.TO_BE_DEFINED.KO";
    private static final String DUPLICATE_IN_DATABASE = "STP_IMPORT_ACCESS_CONTRACT.IDENTIFIER_DUPLICATION.KO";
    private static final String AGENCY_NOT_FOUND_IN_DATABASE = "STP_IMPORT_ACCESS_CONTRACT.AGENCY_NOT_FOUND.KO";
    private static final String CONTRACT_VALIDATION_ERROR = "STP_IMPORT_ACCESS_CONTRACT.VALIDATION_ERROR.KO";

    private static final String UPDATE_CONTRACT_NOT_FOUND = "STP_UPDATE_ACCESS_CONTRACT.CONTRACT_NOT_FOUND.KO";
    private static final String UPDATE_CONTRACT_BAD_REQUEST = "STP_UPDATE_ACCESS_CONTRACT.BAD_REQUEST.KO";
    private static final String UPDATE_VALUE_NOT_IN_ENUM = "STP_UPDATE_ACCESS_CONTRACT.NOT_IN_ENUM.KO";
    private static final String UPDATE_AGENCY_NOT_FOUND = "STP_UPDATE_ACCESS_CONTRACT.AGENCY_NOT_FOUND.KO";
    private static final String UPDATE_KO = "STP_UPDATE_ACCESS_CONTRACT.KO";

    private static final String EVDETDATA_IDENTIFIER = "identifier";

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AccessContractImpl.class);
    private static final String UND_TENANT = "_tenant";
    private static final String UND_ID = "_id";
    private final MongoDbAccessAdminImpl mongoAccess;
    private final LogbookOperationsClient logbookClient;
    private final VitamCounterService vitamCounterService;
    private final MetaDataClient metaDataClient;
    private final FunctionalBackupService functionalBackupService;

    /**
     * Constructor
     *
     * @param mongoAccess         MongoDB client
     * @param vitamCounterService
     */
    public AccessContractImpl(MongoDbAccessAdminImpl mongoAccess, VitamCounterService vitamCounterService) {
        this(mongoAccess, vitamCounterService, MetaDataClientFactory.getInstance().getClient(),
            new FunctionalBackupService(vitamCounterService));
    }

    /**
     * Constructor
     *
     * @param mongoAccess
     * @param vitamCounterService
     * @param metaDataClient
     * @param functionalBackupService
     */
    @VisibleForTesting
    public AccessContractImpl(MongoDbAccessAdminImpl mongoAccess, VitamCounterService vitamCounterService,
        MetaDataClient metaDataClient,
        FunctionalBackupService functionalBackupService) {
        this.mongoAccess = mongoAccess;
        this.vitamCounterService = vitamCounterService;
        this.metaDataClient = metaDataClient;
        this.functionalBackupService = functionalBackupService;

        logbookClient = LogbookOperationsClientFactory.getInstance().getClient();
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
        String operationId = VitamThreadUtils.getVitamSession().getRequestId();
        GUID eip = GUIDReader.getGUID(operationId);

        AccessContractManager manager = new AccessContractManager(logbookClient, metaDataClient, eip);

        manager.logStarted();

        ArrayNode contractsToPersist;

        final VitamError error =
            getVitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem(), "Access contract import error", StatusCode.KO)
                .setHttpCode(Response.Status.BAD_REQUEST.getStatusCode());
        try {

            for (final AccessContractModel acm : contractModelList) {
                // if a contract have and id
                if (acm.getId() != null) {
                    error.addToErrors(
                        getVitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem(), "Access contract import error",
                            StatusCode.KO)
                            .setMessage(
                                GenericRejectionCause.rejectIdNotAllowedInCreate(acm.getName()).getReason()));
                    continue;
                }

                // validate contract
                if (manager.validateContract(acm, acm.getName(), error)) {
                    // TODO: 5/16/17 newIngestContractGUID used for access contract, should create
                    // newAccessContractGUID?
                    acm.setId(GUIDFactory.newIngestContractGUID(ParameterHelper.getTenantParameter()).getId());
                }

                if (acm.getTenant() == null) {
                    acm.setTenant(ParameterHelper.getTenantParameter());
                }

                if (slaveMode) {
                    final Optional<GenericRejectionCause> result =
                        manager.checkEmptyIdentifierSlaveModeValidator().validate(acm, acm.getIdentifier());
                    result.ifPresent(genericRejectionCause -> error
                        .addToErrors(
                            getVitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem(), result.get().getReason(),
                                StatusCode.KO).setMessage(EMPTY_REQUIRED_FIELD)));
                }

            }
            if (error.getErrors() != null && !error.getErrors().isEmpty()) {
                // log book + application log
                // stop
                final String errorsDetails =
                    error.getErrors().stream().map(VitamError::getDescription).collect(Collectors.joining(","));
                manager.logValidationError(errorsDetails, CONTRACTS_IMPORT_EVENT,
                    error.getErrors().get(0).getMessage());
                return error;
            }

            contractsToPersist = JsonHandler.createArrayNode();
            for (final AccessContractModel acm : contractModelList) {

                setIdentifier(slaveMode, acm);
                acm.initializeDefaultValue();

                final ObjectNode accessContractNode = (ObjectNode) JsonHandler.toJsonNode(acm);
                JsonNode hashId = accessContractNode.remove(VitamFieldsHelper.id());
                if (hashId != null) {
                    accessContractNode.set(UND_ID, hashId);
                }
                JsonNode hashTenant = accessContractNode.remove(VitamFieldsHelper.tenant());
                if (hashTenant != null) {
                    accessContractNode.set(UND_TENANT, hashTenant);
                }

                /* contract is valid, add it to the list to persist */
                contractsToPersist.add(accessContractNode);
            }

            // at this point no exception occurred and no validation error detected
            // persist in collection
            // contractsToPersist.values().stream().map();
            // TODO: 3/28/17 create insertDocuments method that accepts VitamDocument instead of ArrayNode, so we can
            // use AccessContract at this point
            mongoAccess.insertDocuments(contractsToPersist, FunctionalAdminCollections.ACCESS_CONTRACT).close();

            functionalBackupService.saveCollectionAndSequence(
                eip,
                CONTRACT_BACKUP_EVENT,
                FunctionalAdminCollections.ACCESS_CONTRACT,
                eip.toString());

        } catch (final Exception exp) {
            LOGGER.error(exp);
            final String err =
                new StringBuilder("Import access contracts error > ").append(exp.getMessage()).toString();
            manager.logFatalError(err);
            saveReport(eip,contractModelList, err);
            manager.logKOError(err);
            return error.setCode(VitamCode.GLOBAL_INTERNAL_SERVER_ERROR.getItem()).setDescription(err).setHttpCode(
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        }

        manager.logSuccess();

        return new RequestResponseOK<AccessContractModel>().addAllResults(contractModelList)
            .setHttpCode(Response.Status.CREATED.getStatusCode());
    }

    private void setIdentifier(boolean slaveMode, AccessContractModel acm)
        throws ReferentialException {
        if (!slaveMode) {
            final String code =
                vitamCounterService.getNextSequenceAsString(ParameterHelper.getTenantParameter(),
                    SequenceType.ACCESS_CONTRACT_SEQUENCE);
            acm.setIdentifier(code);
        }
    }

    private void saveReport(GUID eip, List<AccessContractModel> contractModelList, String errors) throws
        StorageException {

        ContractReport report = generateReport(contractModelList, errors);
        try (InputStream reportInputStream = JsonHandler.writeToInpustream(report)) {
            final String fileName = eip + ".json";
            functionalBackupService.saveFile(reportInputStream, eip, CONTRACT_ACCESS_REPORT, DataCategory.REPORT, fileName);
        } catch (IOException | VitamException e) {
            throw new StorageException(e.getMessage(), e);
        }
    }

    private ContractReport generateReport(List<AccessContractModel> accessContract, String errors) {

        ContractReport report = new ContractReport();

        FunctionalOperationModel operationModel = retrieveOperationModel();

        report.setOperation(operationModel);

        report.setStatusCode(StatusCode.KO);

        report.setAccessContract(accessContract);

        report.setErrors(errors);

        return report;
    }

    private FunctionalOperationModel retrieveOperationModel() {
        try {
            JsonNode result = logbookClient.selectOperationById(VitamThreadUtils.getVitamSession().getRequestId(), null);
            return JsonHandler.getFromJsonNode(result.get(TAG_RESULTS).get(0), FunctionalOperationModel.class);
        } catch (LogbookClientException | InvalidParseOperationException e) {
            throw new VitamRuntimeException("Could not load operation data", e);
        }
    }

    @Override
    public AccessContractModel findByIdentifier(String identifier)
        throws ReferentialException, InvalidParseOperationException {
        SanityChecker.checkParameter(identifier);
        final SelectParserSingle parser = new SelectParserSingle(new SingleVarNameAdapter());
        parser.parse(new Select().getFinalSelect());
        try {
            parser.addCondition(eq("Identifier", identifier));
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

        private Map<AccessContractValidator, String> validators;

        private final GUID eip;

        private final LogbookOperationsClient logbookClient;

        public AccessContractManager(LogbookOperationsClient logbookClient, MetaDataClient metaDataClient,
            GUID eip) {
            this.logbookClient = logbookClient;
            // Init validator
            validators = new HashMap<AccessContractValidator, String>() {
                {
                    put(createMandatoryParamsValidator(), EMPTY_REQUIRED_FIELD);
                    put(createWrongFieldFormatValidator(), EMPTY_REQUIRED_FIELD);
                    put(checkExistenceOriginatingAgenciesValidator(), AGENCY_NOT_FOUND_IN_DATABASE);
                    put(createCheckDuplicateInDatabaseValidator(), DUPLICATE_IN_DATABASE);
                    put(validateExistsArchiveUnits(metaDataClient), CONTRACT_VALIDATION_ERROR);
                }
            };
            this.eip = eip;
        }

        private boolean validateContract(AccessContractModel contract, String jsonFormat,
            VitamError error) {

            for (final AccessContractValidator validator : validators.keySet()) {
                final Optional<GenericRejectionCause> result = validator.validate(contract, jsonFormat);
                if (result.isPresent()) {
                    // there is a validation error on this contract
                    /* contract is valid, add it to the list to persist */
                    error.addToErrors(
                        VitamErrorUtils.getVitamError(
                            VitamCode.CONTRACT_VALIDATION_ERROR.getItem(),
                            result.get().getReason(),
                            "AccessContract",
                            StatusCode.KO)
                            .setMessage(validators.get(validator))
                            .setHttpCode(Response.Status.BAD_REQUEST.getStatusCode()));
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
        private void logValidationError(String errorsDetails, String eventType, String KOEventType)
            throws VitamException {
            LOGGER.error("There validation errors on the input file {}", errorsDetails);
            final GUID eipUsage = GUIDFactory.newOperationLogbookGUID(ParameterHelper.getTenantParameter());
            final LogbookOperationParameters logbookParameters = LogbookParametersFactory
                .newLogbookOperationParameters(eipUsage, eventType, eip, LogbookTypeProcess.MASTERDATA,
                    StatusCode.KO,
                    VitamLogbookMessages.getFromFullCodeKey(KOEventType), eip);
            logbookParameters.putParameterValue(LogbookParameterName.outcomeDetail, KOEventType);
            logbookMessageError(errorsDetails, logbookParameters, KOEventType);
            logbookClient.update(logbookParameters);
        }

        /**
         * Log validation error (business error)
         *
         * @param errorsDetails
         */
        private void logUpdateError(String errorsDetails, String KOEventType) throws VitamException {
            LOGGER.error("Update document error {}", errorsDetails);
            final GUID eipUsage = GUIDFactory.newOperationLogbookGUID(ParameterHelper.getTenantParameter());
            final LogbookOperationParameters logbookParameters = LogbookParametersFactory
                .newLogbookOperationParameters(eipUsage, CONTRACT_UPDATE_EVENT, eip, LogbookTypeProcess.MASTERDATA,
                    StatusCode.KO,
                    VitamLogbookMessages.getFromFullCodeKey(KOEventType), eip);
            logbookParameters.putParameterValue(LogbookParameterName.outcomeDetail, KOEventType);
            logbookMessageError(errorsDetails, logbookParameters, KOEventType);
            logbookClient.update(logbookParameters);
        }

        /**
         * log ko error (system or technical error)
         *
         * @param errorsDetails
         * @throws VitamException
         */
        private void logKOError(String errorsDetails) throws VitamException {
            LOGGER.error("There validation errors on the input file {}", errorsDetails);
            final GUID eipUsage = GUIDFactory.newOperationLogbookGUID(ParameterHelper.getTenantParameter());
            final LogbookOperationParameters logbookParameters = LogbookParametersFactory
                .newLogbookOperationParameters(eipUsage, CONTRACTS_IMPORT_EVENT, eip, LogbookTypeProcess.MASTERDATA,
                    StatusCode.KO,
                    VitamLogbookMessages.getCodeOp(CONTRACTS_IMPORT_EVENT, StatusCode.KO), eip);
            logbookParameters.putParameterValue(LogbookParameterName.outcomeDetail, CONTRACTS_IMPORT_EVENT + "." +
                StatusCode.KO);
            logbookMessageError(errorsDetails, logbookParameters);
            logbookClient.update(logbookParameters);
        }

        /**
         * log fatal error (system or technical error)
         *
         * @param errorsDetails
         * @throws VitamException
         */
        private void logFatalError(String errorsDetails) throws VitamException {
            LOGGER.error("There validation errors on the input file {}", errorsDetails);
            final GUID eipUsage = GUIDFactory.newOperationLogbookGUID(ParameterHelper.getTenantParameter());
            final LogbookOperationParameters logbookParameters = LogbookParametersFactory
                .newLogbookOperationParameters(eipUsage, CONTRACTS_IMPORT_EVENT, eip, LogbookTypeProcess.MASTERDATA,
                    StatusCode.FATAL,
                    VitamLogbookMessages.getCodeOp(CONTRACTS_IMPORT_EVENT, StatusCode.FATAL), eip);
            logbookParameters.putParameterValue(LogbookParameterName.outcomeDetail, CONTRACTS_IMPORT_EVENT + "." +
                StatusCode.FATAL);
            logbookMessageError(errorsDetails, logbookParameters);
            logbookClient.update(logbookParameters);
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

        private void logbookMessageError(String errorsDetails, LogbookOperationParameters logbookParameters,
            String KOEventType) {
            if (null != errorsDetails && !errorsDetails.isEmpty()) {
                try {
                    final ObjectNode object = JsonHandler.createObjectNode();
                    String evDetDataKey = "accessContractCheck";
                    switch (KOEventType) {
                        case EMPTY_REQUIRED_FIELD:
                            evDetDataKey = "Mandatory Fields";
                            break;
                        case WRONG_FIELD_FORMAT:
                            evDetDataKey = "Incorrect Field and value";
                            break;
                        case DUPLICATE_IN_DATABASE:
                            evDetDataKey = "Duplicate Field";
                            break;
                        case AGENCY_NOT_FOUND_IN_DATABASE:
                        case UPDATE_AGENCY_NOT_FOUND:
                            evDetDataKey = "Agency not found";
                            break;
                        case CONTRACT_VALIDATION_ERROR:
                            evDetDataKey = "Validation error";
                            break;
                        case UPDATE_CONTRACT_NOT_FOUND:
                            evDetDataKey = "Contract not found";
                            break;
                        case UPDATE_VALUE_NOT_IN_ENUM:
                            evDetDataKey = "Not in Enum";
                            break;
                    }

                    object.put(evDetDataKey, errorsDetails);

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
            final LogbookOperationParameters logbookParameters = LogbookParametersFactory
                .newLogbookOperationParameters(eip, CONTRACTS_IMPORT_EVENT, eip, LogbookTypeProcess.MASTERDATA,
                    StatusCode.STARTED,
                    VitamLogbookMessages.getCodeOp(CONTRACTS_IMPORT_EVENT, StatusCode.STARTED), eip);
            logbookParameters.putParameterValue(LogbookParameterName.outcomeDetail, CONTRACTS_IMPORT_EVENT + "." +
                StatusCode.STARTED);
            logbookClient.create(logbookParameters);
        }

        /**
         * log update start process
         *
         * @throws VitamException
         */
        private void logUpdateStarted(String id) throws VitamException {
            final LogbookOperationParameters logbookParameters = LogbookParametersFactory
                .newLogbookOperationParameters(eip, CONTRACT_UPDATE_EVENT, eip, LogbookTypeProcess.MASTERDATA,
                    StatusCode.STARTED,
                    VitamLogbookMessages.getCodeOp(CONTRACT_UPDATE_EVENT, StatusCode.STARTED), eip);
            logbookParameters.putParameterValue(LogbookParameterName.outcomeDetail, CONTRACT_UPDATE_EVENT +
                "." + StatusCode.STARTED);
            if (null != id && !id.isEmpty()) {
                logbookParameters.putParameterValue(LogbookParameterName.objectIdentifier, id);
            }
            logbookClient.create(logbookParameters);
        }

        private void logUpdateSuccess(String id, String identifier, List<String> listDiffs) throws VitamException {
            final ObjectNode evDetData = JsonHandler.createObjectNode();
            final ObjectNode evDetDataContract = JsonHandler.createObjectNode();
            final String diffs = listDiffs.stream().reduce("", String::concat);

            final ObjectNode msg = JsonHandler.createObjectNode();
            msg.put(EVDETDATA_IDENTIFIER, identifier);
            msg.put(UPDATED_DIFFS, diffs);
            evDetDataContract.set(id, msg);

            evDetData.set(ACCESS_CONTRACT, msg);
            final String wellFormedJson = SanityChecker.sanitizeJson(evDetData);
            final GUID eipUsage = GUIDFactory.newOperationLogbookGUID(ParameterHelper.getTenantParameter());
            final LogbookOperationParameters logbookParameters =
                LogbookParametersFactory
                    .newLogbookOperationParameters(
                        eipUsage,
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
            logbookClient.update(logbookParameters);
        }


        /**
         * log end success process
         *
         * @throws VitamException
         */
        private void logSuccess() throws VitamException {
            final GUID eipUsage = GUIDFactory.newOperationLogbookGUID(ParameterHelper.getTenantParameter());
            final LogbookOperationParameters logbookParameters = LogbookParametersFactory
                .newLogbookOperationParameters(eipUsage, CONTRACTS_IMPORT_EVENT, eip, LogbookTypeProcess.MASTERDATA,
                    StatusCode.OK,
                    VitamLogbookMessages.getCodeOp(CONTRACTS_IMPORT_EVENT, StatusCode.OK), eip);
            logbookParameters.putParameterValue(LogbookParameterName.outcomeDetail, CONTRACTS_IMPORT_EVENT + "." +
                StatusCode.OK);
            logbookClient.update(logbookParameters);
        }

        /**
         * Validate that contract have not a missing mandatory parameter
         *
         * @return
         */
        private AccessContractValidator createMandatoryParamsValidator() {
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
        private AccessContractValidator createWrongFieldFormatValidator() {
            return (contract, inputList) -> {
                GenericRejectionCause rejection = null;
                final String now = LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now());
                if (contract.getStatus() == null) {
                    contract.setStatus(ActivationStatus.INACTIVE);
                }

                try {
                    if (contract.getCreationdate() == null || contract.getCreationdate().trim().isEmpty()) {
                        contract.setCreationdate(now);
                    } else {
                        contract.setCreationdate(LocalDateUtil.getFormattedDateForMongo(contract.getCreationdate()));
                    }
                } catch (final Exception e) {
                    LOGGER.error("Error access contract parse dates", e);
                    rejection = GenericRejectionCause.rejectMandatoryMissing("Creationdate");
                }
                try {
                    if (contract.getActivationdate() == null || contract.getActivationdate().trim().isEmpty()) {
                        contract.setActivationdate(now);
                    } else {
                        contract
                            .setActivationdate(LocalDateUtil.getFormattedDateForMongo(contract.getActivationdate()));
                    }
                } catch (final Exception e) {
                    LOGGER.error("Error access contract parse dates", e);
                    rejection = GenericRejectionCause.rejectMandatoryMissing("ActivationDate");
                }
                try {

                    if (contract.getDeactivationdate() == null || contract.getDeactivationdate().trim().isEmpty()) {
                        contract.setDeactivationdate(null);
                    } else {
                        contract.setDeactivationdate(LocalDateUtil.getFormattedDateForMongo(contract
                            .getDeactivationdate()));
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
         * Check if the Id of the contract is empty
         *
         * @return
         */
        private AccessContractValidator checkEmptyIdentifierSlaveModeValidator() {
            return (contract, contractName) -> {
                if (contract.getIdentifier() == null || contract.getIdentifier().isEmpty()) {
                    return Optional.of(GenericRejectionCause
                        .rejectMandatoryMissing(AccessContract.IDENTIFIER));
                }
                return Optional.empty();
            };
        }

        /**
         * Check if the contract the same name already exists in database
         *
         * @return
         */
        private AccessContractValidator createCheckDuplicateInDatabaseValidator() {
            return (contract, contractName) -> {
                if (ParametersChecker.isNotEmpty(contract.getIdentifier())) {
                    final int tenant = ParameterHelper.getTenantParameter();
                    final Bson clause =
                        and(Filters.eq(VitamDocument.TENANT_ID, tenant),
                            Filters.eq(AccessContract.IDENTIFIER, contract.getIdentifier()));
                    final boolean exist = FunctionalAdminCollections.ACCESS_CONTRACT.getCollection().count(clause) > 0;
                    if (exist) {
                        return Optional.of(GenericRejectionCause.rejectDuplicatedInDatabase(contract.getIdentifier()));
                    }
                }
                return Optional.empty();
            };
        }

        /**
         * Check if OriginatingAgencies exists in database
         *
         * @return
         */
        private AccessContractValidator checkExistenceOriginatingAgenciesValidator() {
            return (contract, contractName) -> {
                if (null == contract.getOriginatingAgencies() || contract.getOriginatingAgencies().isEmpty()) {
                    return Optional.empty();
                }

                final int tenant = ParameterHelper.getTenantParameter();

                final Bson clause =
                    and(Filters.eq(VitamDocument.TENANT_ID, tenant),
                        Filters.in(Agencies.IDENTIFIER, contract.getOriginatingAgencies()));

                FindIterable find =
                    FunctionalAdminCollections.AGENCIES.getCollection()
                        .find(clause).projection(new BasicDBObject(Agencies.IDENTIFIER, 1));

                MongoCursor<VitamDocument<?>> it = find.iterator();
                Set<String> notFound = new HashSet<>(contract.getOriginatingAgencies());

                if (null != it && it.hasNext()) {
                    while (it.hasNext()) {
                        final VitamDocument<?> next = it.next();
                        notFound.remove(next.get(Agencies.IDENTIFIER));
                    }
                }

                if (!notFound.isEmpty()) {
                    return Optional.of(GenericRejectionCause
                        .rejectRootUnitsNotFound(contractName, String.join(",", notFound)));
                }
                return Optional.empty();
            };
        }


        /**
         * Check if the contract have root Units and all ArchiveUnits corresponding to the rootUnits exists in database
         *
         * @return
         */
        private AccessContractValidator validateExistsArchiveUnits(MetaDataClient metaDataClient) {

            return (contract, contractName) -> {
                Set<String> checkUnits = new HashSet<>();
                Set<String> includeUnits = contract.getRootUnits() == null ? new HashSet<>() : contract.getRootUnits();
                Set<String> excludeUnits =
                    contract.getExcludedRootUnits() == null ? new HashSet<>() : contract.getExcludedRootUnits();

                if (!includeUnits.isEmpty() && !excludeUnits.isEmpty()) {
                    checkUnits.addAll(includeUnits);
                    checkUnits.addAll(excludeUnits);
                } else if (!excludeUnits.isEmpty()) {
                    checkUnits = excludeUnits;
                } else if (!includeUnits.isEmpty()) {
                    checkUnits = includeUnits;
                } else {
                    return Optional.empty();
                }

                checkUnits.removeIf(unit -> unit.trim().isEmpty());
                if (checkUnits.isEmpty()) {
                    return Optional.empty();
                }

                String[] rootUnitArray = checkUnits.toArray(new String[checkUnits.size()]);

                final Select select = new Select();
                try {
                    select.setQuery(QueryHelper.in(PROJECTIONARGS.ID.exactToken(), rootUnitArray).setDepthLimit(0));
                    select.setProjection(JsonHandler.getFromString("{\"$fields\": { \"#id\": 1}}"));
                } catch (InvalidCreateOperationException |
                    InvalidParseOperationException e) {
                    return Optional
                        .of(GenericRejectionCause.rejectExceptionOccurred(contract.getName(), "Error parse query", e));
                }

                final JsonNode queryDsl = select.getFinalSelect();

                try {
                    JsonNode resp = metaDataClient.selectUnits(queryDsl);
                    RequestResponseOK<JsonNode> responseOK = RequestResponseOK.getFromJsonNode(resp);
                    List<JsonNode> result = responseOK.getResults();
                    if (null == result || result.isEmpty()) {
                        return Optional.of(GenericRejectionCause
                            .rejectRootUnitsNotFound(contractName, String.join(",", checkUnits)));

                    } else if (result.size() == checkUnits.size()) {
                        return Optional.empty();
                    } else {
                        Set<String> notFoundRootUnits = new HashSet<>(checkUnits);
                        result.forEach(unit -> {
                            notFoundRootUnits.remove(unit.get("#id").asText());
                        });
                        return Optional.of(GenericRejectionCause
                            .rejectRootUnitsNotFound(contractName, String.join(",", notFoundRootUnits)));
                    }
                } catch (InvalidParseOperationException |
                    MetaDataExecutionException |
                    MetaDataDocumentSizeException |
                    MetaDataClientServerException e) {
                    return Optional.of(GenericRejectionCause
                        .rejectExceptionOccurred(contract.getName(), "Error while select units", e));
                }
            };
        }
    }



    @Override
    public void close() {
        logbookClient.close();
    }



    @Override
    public RequestResponse<AccessContractModel> updateContract(String identifier, JsonNode queryDsl)
        throws VitamException {
        ParametersChecker.checkParameter(UPDATE_ACCESS_CONTRACT_MANDATORY_PATAMETER, queryDsl);
        VitamError error =
            getVitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem(), "Access contract update error", StatusCode.KO)
                .setHttpCode(Response.Status.BAD_REQUEST.getStatusCode());

        final AccessContractModel accContractModel = findByIdentifier(identifier);
        Map<String, List<String>> updateDiffs;
        if (accContractModel == null) {
            error.setHttpCode(Response.Status.NOT_FOUND.getStatusCode());
            return error.addToErrors(getVitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem(),
                ACCESS_CONTRACT_NOT_FOUND + identifier, StatusCode.KO).setMessage(UPDATE_CONTRACT_NOT_FOUND));
        }

        String operationId = VitamThreadUtils.getVitamSession().getRequestId();
        GUID eip = GUIDReader.getGUID(operationId);
        AccessContractManager manager = new AccessContractManager(logbookClient, metaDataClient, eip);
        RequestResponseOK response = new RequestResponseOK<>();

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
                    validateUpdateAction(manager, accContractModel.getName(), error, field, value);
                }

                ((ObjectNode) fieldName).remove(AbstractContractModel.TAG_CREATION_DATE);
                ((ObjectNode) fieldName).put(AbstractContractModel.TAG_LAST_UPDATE, LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()));
            }
        }

        if (error.getErrors() != null && error.getErrors().size() > 0) {
            final String errorsDetails =
                error.getErrors().stream().map(c -> c.getDescription()).collect(Collectors.joining(","));
            manager.logUpdateError(errorsDetails, error.getErrors().get(0).getMessage());

            return error;
        }

        try {

            try (DbRequestResult result = mongoAccess
                .updateData(queryDsl, FunctionalAdminCollections.ACCESS_CONTRACT)) {
                updateDiffs = result.getDiffs();
                response.addResult(new DbRequestResult(result))
                    .setTotal(result.getTotal())
                    .setQuery(queryDsl)
                    .setHttpCode(Response.Status.OK.getStatusCode());
            }

            functionalBackupService.saveCollectionAndSequence(
                eip,
                CONTRACT_BACKUP_EVENT,
                FunctionalAdminCollections.ACCESS_CONTRACT,
                accContractModel.getId());

        } catch (SchemaValidationException | BadRequestException exp) {
            LOGGER.error(exp);
            final String err =
                new StringBuilder("Import ingest contracts error > ").append(exp.getMessage()).toString();
            manager.logUpdateError(err, UPDATE_CONTRACT_BAD_REQUEST);
            return getVitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem(), exp.getMessage(),
                StatusCode.KO).setHttpCode(Response.Status.BAD_REQUEST.getStatusCode());
        } catch (Exception exp) {
            LOGGER.error(exp);
            final String err =
                new StringBuilder("Import ingest contracts error > ").append(exp.getMessage()).toString();
            manager.logFatalError(err);
            return error.setCode(VitamCode.GLOBAL_INTERNAL_SERVER_ERROR.getItem()).setDescription(err).setHttpCode(
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        }

        manager.logUpdateSuccess(accContractModel.getId(), identifier, updateDiffs.get(accContractModel.getId()));
        return response;
    }

    private void validateUpdateAction(AccessContractManager manager, String contractName, final VitamError error,
        final String field, final JsonNode value) {

        if (AccessContract.STATUS.equals(field)) {
            if (!(ActivationStatus.ACTIVE.name().equals(value.asText()) || ActivationStatus.INACTIVE
                .name().equals(value.asText()))) {
                error.addToErrors(getVitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem(),
                    THE_ACCESS_CONTRACT_STATUS_MUST_BE_ACTIVE_OR_INACTIVE_BUT_NOT + value.asText(), StatusCode.KO)
                    .setMessage(UPDATE_VALUE_NOT_IN_ENUM));
            }
        }

        if (AccessContractModel.EVERY_ORIGINATINGAGENCY.equals(field)) {
            if (!(value instanceof BooleanNode)) {
                error.addToErrors(getVitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem(),
                    THE_ACCESS_CONTRACT_EVERY_ORIGINATING_AGENCY_MUST_BE_TRUE_OR_FALSE_BUT_NOT +
                        value.asText(),
                    StatusCode.KO).setMessage(UPDATE_VALUE_NOT_IN_ENUM));
            }
        }

        if (AccessContractModel.ORIGINATING_AGENCIES.equals(field)) {
            if (!value.isArray()) {
                error.addToErrors(getVitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem(),
                    ORIGINATING_AGENCIES_INVALID +
                        value.asText(),
                    StatusCode.KO).setMessage(UPDATE_VALUE_NOT_IN_ENUM));
            } else {

                try {
                    Set<String> originatingAgencies = JsonHandler.getFromJsonNode(value, Set.class);
                    AccessContractModel toValidate = new AccessContractModel();
                    toValidate.setOriginatingAgencies(originatingAgencies);
                    Optional<GenericRejectionCause> rejection =
                        manager.checkExistenceOriginatingAgenciesValidator()
                            .validate(toValidate, contractName);

                    if (rejection.isPresent()) {
                        // Validation error
                        error.addToErrors(
                            getVitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem(), rejection
                                .get().getReason(), StatusCode.KO).setMessage(UPDATE_AGENCY_NOT_FOUND));
                    }

                } catch (InvalidParseOperationException e) {
                    error.addToErrors(getVitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem(),
                        ROOT_UNIT_INVALID +
                            value.asText(),
                        StatusCode.KO));
                }
            }
        }

        if (AccessContractModel.EVERY_DATA_OBJECT_VERSION.equals(field)) {
            if (!(value instanceof BooleanNode)) {
                error.addToErrors(getVitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem(),
                    THE_ACCESS_CONTRACT_EVERY_DATA_OBJECT_VERSION_MUST_BE_TRUE_OR_FALSE_BUT_NOT +
                        value.asText(),
                    StatusCode.KO).setMessage(UPDATE_VALUE_NOT_IN_ENUM));
            }
        }

        if (AccessContractModel.DATA_OBJECT_VERSION.equals(field)) {
            if (!validateObjectVersion(value)) {
                error.addToErrors(getVitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem(),
                    DATA_OBJECT_VERSION_INVALID +
                        value.asText(),
                    StatusCode.KO).setMessage(UPDATE_VALUE_NOT_IN_ENUM));
            }
        }

        // Validate that RootUnits if not empty exists in database
        if (AccessContractModel.ROOT_UNITS.equals(field)) {
            if (!value.isArray()) {
                error.addToErrors(getVitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem(),
                    ROOT_UNIT_INVALID +
                        value.asText(),
                    StatusCode.KO).setMessage(UPDATE_KO));
            } else {

                try {
                    Set<String> rootUnits = JsonHandler.getFromJsonNode(value, Set.class);
                    AccessContractModel toValidate = new AccessContractModel();
                    toValidate.setRootUnits(rootUnits);
                    Optional<GenericRejectionCause> rejection =
                        manager.validateExistsArchiveUnits(metaDataClient)
                            .validate(toValidate, contractName);

                    if (rejection.isPresent()) {
                        // Validation error
                        error.addToErrors(
                            getVitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem(), rejection
                                .get().getReason(), StatusCode.KO).setMessage(UPDATE_KO));
                    }

                } catch (InvalidParseOperationException e) {
                    error.addToErrors(getVitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem(),
                        ROOT_UNIT_INVALID +
                            value.asText(),
                        StatusCode.KO).setMessage(UPDATE_KO));
                }
            }
        }

        // Validate that ExcludedRootUnits, if not empty, exists in database
        if (AccessContractModel.EXCLUDED_ROOT_UNITS.equals(field)) {
            if (!value.isArray()) {
                error.addToErrors(getVitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem(),
                    EXCLUDED_ROOT_UNIT_INVALID +
                        value.asText(),
                    StatusCode.KO).setMessage(UPDATE_KO));
            } else {

                try {
                    Set<String> excludedRootUnits = JsonHandler.getFromJsonNode(value, Set.class);
                    AccessContractModel toValidate = new AccessContractModel();
                    toValidate.setExcludedRootUnits(excludedRootUnits);
                    Optional<GenericRejectionCause> rejection =
                        manager.validateExistsArchiveUnits(metaDataClient)
                            .validate(toValidate, contractName);

                    if (rejection.isPresent()) {
                        // Validation error
                        error.addToErrors(
                            getVitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem(), rejection
                                .get().getReason(), StatusCode.KO).setMessage(UPDATE_KO));
                    }

                } catch (InvalidParseOperationException e) {
                    error.addToErrors(getVitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem(),
                        ROOT_UNIT_INVALID +
                            value.asText(),
                        StatusCode.KO).setMessage(UPDATE_KO));
                }
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

    private VitamError getVitamError(String vitamCode, String error, StatusCode statusCode) {
        return VitamErrorUtils.getVitamError(vitamCode, error, "AccessContract", statusCode);
    }

}
