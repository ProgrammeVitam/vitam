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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
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
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.SchemaValidationException;
import fr.gouv.vitam.common.exception.VitamException;
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
import fr.gouv.vitam.common.model.UnitType;
import fr.gouv.vitam.common.model.administration.AbstractContractModel;
import fr.gouv.vitam.common.model.administration.ActivationStatus;
import fr.gouv.vitam.common.model.administration.IngestContractModel;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.FileFormat;
import fr.gouv.vitam.functional.administration.common.FunctionalBackupService;
import fr.gouv.vitam.functional.administration.common.IngestContract;
import fr.gouv.vitam.functional.administration.common.Profile;
import fr.gouv.vitam.functional.administration.common.VitamErrorUtils;
import fr.gouv.vitam.functional.administration.common.counter.SequenceType;
import fr.gouv.vitam.functional.administration.common.counter.VitamCounterService;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.functional.administration.contract.api.ContractService;
import fr.gouv.vitam.functional.administration.contract.core.GenericContractValidator.GenericRejectionCause;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import org.bson.conversions.Bson;
import org.jclouds.json.Json;

import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.in;
import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTIONARGS.UNITTYPE;

/**
 * IngestContract implementation class
 */
public class IngestContractImpl implements ContractService<IngestContractModel> {

    private static final String THE_INGEST_CONTRACT_STATUS_MUST_BE_ACTIVE_OR_INACTIVE_BUT_NOT =
        "The Ingest contract status must be ACTIVE or INACTIVE but not ";
    private static final String INGEST_CONTRACT_NOT_FOUND = "Ingest contract not found";
    private static final String CONTRACT_IS_MANDATORY_PATAMETER = "The collection of ingest contracts is mandatory";
    private static final String EVERYFORMAT_LIST_EMPTY = "formatType field must not be empty when everyFormat is false";
    private static final String EVERYFORMAT_LIST_NOT_EMPTY = "formatType field must be empty when everyFormat is true";
    private static final String CONTRACTS_IMPORT_EVENT = "STP_IMPORT_INGEST_CONTRACT";
    private static final String CONTRACT_UPDATE_EVENT = "STP_UPDATE_INGEST_CONTRACT";
    public static final String CONTRACT_BACKUP_EVENT = "STP_BACKUP_INGEST_CONTRACT";

    private static final String EMPTY_REQUIRED_FIELD = "STP_IMPORT_INGEST_CONTRACT.EMPTY_REQUIRED_FIELD.KO";
    private static final String WRONG_FIELD_FORMAT = "STP_IMPORT_INGEST_CONTRACT.TO_BE_DEFINED.KO";
    private static final String DUPLICATE_IN_DATABASE = "STP_IMPORT_INGEST_CONTRACT.IDENTIFIER_DUPLICATION.KO";
    private static final String PROFILE_NOT_FOUND_IN_DATABASE = "STP_IMPORT_INGEST_CONTRACT.PROFILE_NOT_FOUND.KO";
    private static final String FORMAT_NOT_FOUND = "STP_IMPORT_INGEST_CONTRACT.FORMAT_NOT_FOUND.KO";
    private static final String FORMAT_MUST_BE_EMPTY = "STP_IMPORT_INGEST_CONTRACT.FORMAT_MUST_BE_EMPTY.KO";
    private static final String FORMAT_MUST_NOT_BE_EMPTY = "STP_IMPORT_INGEST_CONTRACT.FORMAT_MUST_NOT_BE_EMPTY.KO";

    private static final String UPDATE_CONTRACT_NOT_FOUND = "STP_UPDATE_INGEST_CONTRACT.CONTRACT_NOT_FOUND.KO";
    private static final String UPDATE_VALUE_NOT_IN_ENUM = "STP_UPDATE_INGEST_CONTRACT.NOT_IN_ENUM.KO";
    private static final String UPDATE_DUPLICATE_IN_DATABASE = "STP_UPDATE_INGEST_CONTRACT.IDENTIFIER_DUPLICATION.KO";
    private static final String UPDATE_PROFILE_NOT_FOUND = "STP_UPDATE_INGEST_CONTRACT.PROFILE_NOT_FOUND.KO";
    private static final String UPDATE_CONTRACT_BAD_REQUEST = "STP_UPDATE_INGEST_CONTRACT.BAD_REQUEST.KO";
    private static final String UPDATE_KO = "STP_UPDATE_INGEST_CONTRACT.KO";
    private static final String UPDATE_WRONG_FILEFORMAT = "STP_UPDATE_INGEST_CONTRACT.FILEFORMAT_NOT_FOUND.KO";

    private static final String EVDETDATA_IDENTIFIER = "identifier";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IngestContractImpl.class);
    private final MongoDbAccessAdminImpl mongoAccess;
    private final LogbookOperationsClient logbookClient;
    private final VitamCounterService vitamCounterService;
    private final MetaDataClient metaDataClient;
    private final FunctionalBackupService functionalBackupService;
    private static final String UND_TENANT = "_tenant";
    private static final String UND_ID = "_id";
    private static final String RESULT_HITS = "$hits";
    private static final String HITS_SIZE = "size";

    /**
     * Constructor
     *
     * @param dbConfiguration the Database configuration
     * @param vitamCounterService the vitam counter service
     */
    public IngestContractImpl(MongoDbAccessAdminImpl dbConfiguration, VitamCounterService vitamCounterService) {
        this(dbConfiguration, vitamCounterService, MetaDataClientFactory.getInstance().getClient(),
            new FunctionalBackupService(vitamCounterService));
    }

    /**
     * Constructor
     *
     * @param dbConfiguration the Database configuration
     * @param vitamCounterService the vitam counter service
     * @param metaDataClient the metadata client
     * @param functionalBackupService
     */
    public IngestContractImpl(MongoDbAccessAdminImpl dbConfiguration, VitamCounterService vitamCounterService,
        MetaDataClient metaDataClient,
        FunctionalBackupService functionalBackupService) {
        mongoAccess = dbConfiguration;
        this.vitamCounterService = vitamCounterService;
        this.metaDataClient = metaDataClient;
        this.functionalBackupService = functionalBackupService;
        logbookClient = LogbookOperationsClientFactory.getInstance().getClient();
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
        String operationId = VitamThreadUtils.getVitamSession().getRequestId();
        GUID eip = GUIDReader.getGUID(operationId);


        IngestContractManager manager = new IngestContractManager(logbookClient, metaDataClient, eip);
        manager.logStarted();

        ArrayNode contractsToPersist = null;

        final VitamError error =
            getVitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem(), "Ingest contract import error", StatusCode.KO)
                .setHttpCode(Response.Status.BAD_REQUEST
                    .getStatusCode());

        try {

            for (final IngestContractModel acm : contractModelList) {


                // if a contract have and id
                if (null != acm.getId()) {
                    error.addToErrors(getVitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem(),
                        GenericRejectionCause.rejectIdNotAllowedInCreate(acm.getName())
                            .getReason(), StatusCode.KO));
                    continue;
                }

                final String linkParentId = acm.getLinkParentId();
                if (linkParentId != null) {
                    if (!manager.checkIfAUInFilingOrHoldingSchema(linkParentId)) {
                        error
                            .addToErrors(getVitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem(),
                                GenericRejectionCause
                                    .rejectWrongLinkParentId(linkParentId)
                                    .getReason(), StatusCode.KO));
                        continue;
                    }
                }

                //when everyformattype is false, formattype must not be empty
                if (!acm.isEveryFormatType() && (acm.getFormatType() == null || acm.getFormatType().isEmpty())) {
                    error.addToErrors(getVitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem(),
                            EVERYFORMAT_LIST_EMPTY, StatusCode.KO).setMessage(FORMAT_MUST_NOT_BE_EMPTY));
                    continue;
                }

                //when everyformattype is true, formattype must  be empty
                if (acm.isEveryFormatType() && acm.getFormatType() != null) {
                    error.addToErrors(getVitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem(),
                        EVERYFORMAT_LIST_NOT_EMPTY, StatusCode.KO).setMessage(FORMAT_MUST_BE_EMPTY));
                    continue;
                }

                // validate contract
                if (manager.validateContract(acm, acm.getName(), error)) {
                    acm.setId(GUIDFactory.newIngestContractGUID(ParameterHelper.getTenantParameter()).getId());
                }
                if (acm.getTenant() == null) {
                    acm.setTenant(ParameterHelper.getTenantParameter());
                }


                if (slaveMode) {
                    final Optional<GenericRejectionCause> result =
                        manager.checkEmptyIdentifierSlaveModeValidator().validate(acm, acm.getIdentifier());
                    result.ifPresent(t -> error
                        .addToErrors(getVitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem(),
                            result.get().getReason(), StatusCode.KO).setMessage(EMPTY_REQUIRED_FIELD)
                        )
                    );
                }

            }

            if (null != error.getErrors() && !error.getErrors().isEmpty()) {
                // log book + application log
                // stop
                final String errorsDetails =
                    error.getErrors().stream().map(c -> c.getDescription()).distinct().collect(Collectors.joining(","));

                manager
                    .logValidationError(errorsDetails, CONTRACTS_IMPORT_EVENT, error.getErrors().get(0).getMessage());
                return error;
            }
            contractsToPersist = JsonHandler.createArrayNode();
            for (final IngestContractModel acm : contractModelList) {
                setIdentifier(slaveMode, acm);
                final ObjectNode ingestContractNode = (ObjectNode) JsonHandler.toJsonNode(acm);
                JsonNode hashId = ingestContractNode.remove(VitamFieldsHelper.id());
                if (hashId != null) {
                    ingestContractNode.set(UND_ID, hashId);
                }

                JsonNode hashTenant = ingestContractNode.remove(VitamFieldsHelper.tenant());
                if (hashTenant != null) {
                    ingestContractNode.set(UND_TENANT, hashTenant);
                }
                /* contract is valid, add it to the list to persist */
                contractsToPersist.add(ingestContractNode);
            }

            // at this point no exception occurred and no validation error detected
            // persist in collection
            // contractsToPersist.values().stream().map();
            // TODO: 3/28/17 create insertDocuments method that accepts VitamDocument instead of ArrayNode, so we can
            // use IngestContract at this point
            mongoAccess.insertDocuments(contractsToPersist, FunctionalAdminCollections.INGEST_CONTRACT).close();

            functionalBackupService.saveCollectionAndSequence(
                eip,
                CONTRACT_BACKUP_EVENT,
                FunctionalAdminCollections.INGEST_CONTRACT,
                eip.toString()
            );

        } catch (final Exception exp) {
            LOGGER.error(exp);
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

    private void setIdentifier(boolean slaveMode, IngestContractModel acm)
        throws ReferentialException {
        if (!slaveMode) {
            final String code = vitamCounterService
                .getNextSequenceAsString(ParameterHelper.getTenantParameter(),
                    SequenceType.INGEST_CONTRACT_SEQUENCE);
            acm.setIdentifier(code);
        }
    }

    @Override
    public IngestContractModel findByIdentifier(String identifier)
        throws ReferentialException, InvalidParseOperationException {
        SanityChecker.checkParameter(identifier);
        final SelectParserSingle parser = new SelectParserSingle(new SingleVarNameAdapter());
        parser.parse(new Select().getFinalSelect());
        try {
            parser.addCondition(QueryHelper.eq("Identifier", identifier));
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

        private static final String UPDATED_DIFFS = "updatedDiffs";
        private static final String INGEST_CONTRACT = "IngestContract";

        private Map<IngestContractValidator, String> validators;

        private final GUID eip;

        private final LogbookOperationsClient logbookClient;
        private final MetaDataClient metaDataClient;


        public IngestContractManager(LogbookOperationsClient logbookClient, MetaDataClient metaDataClient,
            GUID eip) {
            this.logbookClient = logbookClient;
            this.metaDataClient = metaDataClient;
            this.eip = eip;
            // Init validator
            validators = new HashMap<IngestContractValidator, String>() {{
                put(createMandatoryParamsValidator(), EMPTY_REQUIRED_FIELD);
                put(createWrongFieldFormatValidator(), EMPTY_REQUIRED_FIELD);
                put(createCheckDuplicateInDatabaseValidator(), DUPLICATE_IN_DATABASE);
                put(createCheckProfilesExistsInDatabaseValidator(), PROFILE_NOT_FOUND_IN_DATABASE);
                put(createCheckFormatFileExistsInDatabaseValidator(), FORMAT_NOT_FOUND);
            }};
        }

        private boolean validateContract(IngestContractModel contract, String jsonFormat,
            VitamError error) {

            for (final IngestContractValidator validator : validators.keySet()) {
                final Optional<GenericRejectionCause> result =
                    validator.validate(contract, jsonFormat);
                if (result.isPresent()) {
                    // there is a validation error on this contract
                    /* contract is valid, add it to the list to persist */
                    error.addToErrors(getVitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem(), result
                        .get().getReason(), StatusCode.KO).setMessage(validators.get(validator)));
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
        private void logValidationError(final String errorsDetails, final String eventType, final String KOEventType)
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
                    object.put("ingestContractCheck", errorsDetails);

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
                    String evDetDataKey = "ingestContractCheck";
                    switch (KOEventType) {
                        case EMPTY_REQUIRED_FIELD:
                            evDetDataKey = "Mandatory Fields";
                            break;
                        case WRONG_FIELD_FORMAT:
                        case UPDATE_WRONG_FILEFORMAT:
                            evDetDataKey = "Incorrect Field and value";
                            break;
                        case DUPLICATE_IN_DATABASE:
                        case UPDATE_DUPLICATE_IN_DATABASE:
                            evDetDataKey = "Duplicate Field";
                            break;
                        case PROFILE_NOT_FOUND_IN_DATABASE:
                            evDetDataKey = "Profile not found";
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
            evDetData.set(INGEST_CONTRACT, msg);

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
         * Validate that contract have not a missing mandatory parameter
         *
         * @return IngestContractValidator
         */
        private IngestContractValidator createMandatoryParamsValidator() {
            return (contract, jsonFormat) -> {
                GenericRejectionCause rejection = null;
                if (contract.getName() == null || contract.getName().trim().isEmpty()) {
                    rejection =
                        GenericRejectionCause.rejectMandatoryMissing(IngestContract.NAME);
                }

                return rejection == null ? Optional.empty() : Optional.of(rejection);
            };
        }

        /**
         * Set a default value if null
         *
         * @return IngestContractValidator
         */
        private IngestContractValidator createWrongFieldFormatValidator() {
            return (contract, inputList) -> {
                GenericRejectionCause rejection = null;
                final String now = LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now());
                if (contract.getStatus() == null) {
                    contract.setStatus(ActivationStatus.INACTIVE);
                }
                if (contract.getCheckParentLink() == null) {
                    contract.setCheckParentLink(ActivationStatus.INACTIVE);
                }

                try {
                    if (contract.getCreationdate() == null || contract.getCreationdate().trim().isEmpty()) {
                        contract.setCreationdate(now);
                    } else {
                        contract.setCreationdate(LocalDateUtil.getFormattedDateForMongo(contract.getCreationdate()));
                    }

                } catch (final Exception e) {
                    LOGGER.error("Error ingest contract parse dates", e);
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
                    LOGGER.error("Error ingest contract parse dates", e);
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
                    LOGGER.error("Error ingest contract parse dates", e);
                    rejection =
                        GenericRejectionCause.rejectMandatoryMissing("deactivationdate");
                }

                contract.setLastupdate(now);

                return rejection == null ? Optional.empty() : Optional.of(rejection);
            };
        }


        /**
         * Check if the contract the same name already exists in database
         *
         * @return IngestContractValidator
         */
        private IngestContractValidator createCheckDuplicateInDatabaseValidator() {
            return (contract, contractName) -> {
                if (ParametersChecker.isNotEmpty(contract.getIdentifier())) {
                    final int tenant = ParameterHelper.getTenantParameter();
                    final Bson clause =
                        and(eq(VitamDocument.TENANT_ID, tenant),
                            eq(IngestContract.IDENTIFIER, contract.getIdentifier()));
                    final boolean exist = FunctionalAdminCollections.INGEST_CONTRACT.getCollection().count(clause) > 0;
                    if (exist) {
                        return Optional
                            .of(GenericRejectionCause.rejectDuplicatedInDatabase(contract.getIdentifier()));
                    }
                }
                return Optional.empty();
            };
        }


        /**
         * Check if the Id of the contract is empty
         *
         * @return
         */
        private IngestContractValidator checkEmptyIdentifierSlaveModeValidator() {
            return (contract, contractIdentifier) -> {
                if (contractIdentifier == null || contractIdentifier.isEmpty()) {
                    return Optional
                        .of(GenericRejectionCause
                            .rejectMandatoryMissing(IngestContract.IDENTIFIER));
                }
                return Optional.empty();
            };
        }

        /**
         * Check if the profiles exist in database
         *
         * @return IngestContractValidator
         */
        private IngestContractValidator createCheckProfilesExistsInDatabaseValidator() {
            return (contract, contractName) -> {
                if (null == contract.getArchiveProfiles() || contract.getArchiveProfiles().size() == 0) {
                    return Optional.empty();
                }
                GenericRejectionCause rejection = null;
                final int tenant = ParameterHelper.getTenantParameter();
                final Bson clause =
                    and(eq(VitamDocument.TENANT_ID, tenant), in(Profile.IDENTIFIER, contract.getArchiveProfiles()));
                final long count = FunctionalAdminCollections.PROFILE.getCollection().count(clause);
                if (count != contract.getArchiveProfiles().size()) {
                    rejection =
                        GenericRejectionCause
                            .rejectArchiveProfileNotFoundInDatabase(contractName);
                }
                return rejection == null ? Optional.empty() : Optional.of(rejection);
            };
        }

        /**
         * Check if the fileFormat submitted exist in database
         *
         * @return IngestContractValidator
         */
        private IngestContractValidator createCheckFormatFileExistsInDatabaseValidator() {
            return (contract, contractName) -> {
                if (null == contract.getFormatType() || contract.getFormatType().size() == 0) {
                    return Optional.empty();
                }
                GenericRejectionCause rejection = null;
                final int tenant = ParameterHelper.getTenantParameter();

                final Bson clause =
                    in(FileFormat.PUID, contract.getFormatType());

                final long count = FunctionalAdminCollections.FORMATS.getCollection().count(clause);

                if (count != contract.getFormatType().size()) {
                    rejection =
                        GenericRejectionCause
                            .rejectFormatFileTypeNotFoundInDatabase(contractName);
                }
                return rejection == null ? Optional.empty() : Optional.of(rejection);
            };
        }

        /**
         * Check if the linkParentId is a valid existing FILING or HOLDING Unit identifier
         *
         * @param linkParentId GUID as String
         * @return boolean true if valid identifier passed
         * @throws InvalidCreateOperationException invalidCreateOperationException
         * @throws MetaDataExecutionException      metaDataExecutionException
         * @throws MetaDataDocumentSizeException   metaDataDocumentSizeException
         * @throws MetaDataClientServerException   metaDataClientServerException
         * @throws InvalidParseOperationException  invalidParseOperationException
         */
        private boolean checkIfAUInFilingOrHoldingSchema(String linkParentId)
            throws InvalidCreateOperationException, MetaDataExecutionException, MetaDataDocumentSizeException,
            MetaDataClientServerException, InvalidParseOperationException {
            final Select select = new Select();
            String[] schemaArray = new String[] {UnitType.FILING_UNIT.name(), UnitType.HOLDING_UNIT.name()};
            select.setQuery(QueryHelper.in(UNITTYPE.exactToken(), schemaArray).setDepthLimit(0));
            final JsonNode queryDsl = select.getFinalSelect();

            JsonNode jsonNode = metaDataClient.selectUnitbyId(queryDsl, linkParentId);
            return (jsonNode != null && jsonNode.get(RESULT_HITS) != null
                && jsonNode.get(RESULT_HITS).get(HITS_SIZE).asInt() > 0);
        }

    }

    @Override
    public void close() {
        logbookClient.close();
    }

    @Override
    public RequestResponse<IngestContractModel> updateContract(String identifier, JsonNode queryDsl)
        throws VitamException {
        VitamError error =
            getVitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem(), "Ingest contract update error", StatusCode.KO)
                .setHttpCode(Response.Status.BAD_REQUEST
                    .getStatusCode());

        if (queryDsl == null || !queryDsl.isObject()) {
            return error;
        }

        final IngestContractModel ingestContractModel = findByIdentifier(identifier);
        if (ingestContractModel == null) {
            error.setHttpCode(Response.Status.NOT_FOUND.getStatusCode());
            return error.addToErrors(
                getVitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem(), INGEST_CONTRACT_NOT_FOUND + identifier,
                    StatusCode.KO).setMessage(UPDATE_CONTRACT_NOT_FOUND));
        }



        String operationId = VitamThreadUtils.getVitamSession().getRequestId();
        GUID eip = GUIDReader.getGUID(operationId);
        RequestResponseOK response = new RequestResponseOK<>();

        IngestContractManager manager = new IngestContractManager(logbookClient, metaDataClient, eip);

        manager.logUpdateStarted(ingestContractModel.getId());

        final JsonNode actionNode = queryDsl.get(GLOBAL.ACTION.exactToken());
        for (final JsonNode fieldToSet : actionNode) {
            final JsonNode fieldName = fieldToSet.get(UPDATEACTION.SET.exactToken());
            if (fieldName != null) {
                final Iterator<String> it = fieldName.fieldNames();
                while (it.hasNext()) {
                    final String field = it.next();
                    final JsonNode value = fieldName.findValue(field);
                    if (AbstractContractModel.TAG_STATUS.equals(field)) {
                        if (!(ActivationStatus.ACTIVE.name().equals(value.asText()) || ActivationStatus.INACTIVE
                            .name().equals(value.asText()))) {
                            error.addToErrors(getVitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem(),
                                THE_INGEST_CONTRACT_STATUS_MUST_BE_ACTIVE_OR_INACTIVE_BUT_NOT + value.asText(),
                                StatusCode.KO).setMessage(UPDATE_VALUE_NOT_IN_ENUM));
                        }
                    }
                }

                ((ObjectNode) fieldName).remove(AbstractContractModel.TAG_CREATION_DATE);
                ((ObjectNode) fieldName).put(AbstractContractModel.TAG_LAST_UPDATE, LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()));
            }
        }

        Map<String, List<String>> updateDiffs;

        try {
            JsonNode linkParentNode = queryDsl.findValue(IngestContractModel.LINK_PARENT_ID);
            if (linkParentNode != null) {
                final String linkParentId = linkParentNode.asText();
                if (!linkParentId.equals("")) {
                    if (!manager.checkIfAUInFilingOrHoldingSchema(linkParentId)) {
                        error
                            .addToErrors(getVitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem(),
                                GenericRejectionCause.rejectWrongLinkParentId(linkParentId).getReason(), StatusCode.KO)
                                .setMessage(UPDATE_KO));
                    }
                }
            }



            final JsonNode archiveProfilesNode = queryDsl.findValue(IngestContractModel.ARCHIVE_PROFILES);
            if (archiveProfilesNode != null) {
                final Set<String> archiveProfiles =
                    JsonHandler.getFromString(archiveProfilesNode.toString(), Set.class, String.class);
                final IngestContractValidator validator =
                    manager.createCheckProfilesExistsInDatabaseValidator();
                final Optional<GenericRejectionCause> result =
                    validator.validate(new IngestContractModel().setArchiveProfiles(archiveProfiles),
                        identifier);
                if (result.isPresent()) {
                    // there is a validation error on this contract
                    /* contract is valid, add it to the list to persist */
                    error.addToErrors(
                        getVitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem(), result.get().getReason(),
                            StatusCode.KO).setMessage(UPDATE_PROFILE_NOT_FOUND));
                }
            }
            final JsonNode fileFormatTypeNode = queryDsl.findValue(IngestContractModel.FORMAT_TYPE);
            Set<String> fileFormatTypes =
                getFromJsonNodeOrFromIngestContractModel(ingestContractModel, fileFormatTypeNode);
            checkFormatsNotEmptyWhenEveryFormatTypeIsFalse(queryDsl, fileFormatTypes, fileFormatTypeNode, error);

            if (fileFormatTypes != null) {

                final IngestContractValidator validator =
                    manager.createCheckFormatFileExistsInDatabaseValidator();
                final Optional<GenericRejectionCause> result =
                    validator.validate(new IngestContractModel().setFormatType(fileFormatTypes),
                        identifier);
                // there is a validation error on this contract
                /* contract is valid, add it to the list to persist */
                result.ifPresent(genericRejectionCause -> error.addToErrors(
                    getVitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem(), genericRejectionCause.getReason(),
                        StatusCode.KO).setMessage(UPDATE_WRONG_FILEFORMAT)));
            }

            if (error.getErrors() != null && error.getErrors().size() > 0) {
                final String errorsDetails =
                    error.getErrors().stream().map(VitamError::getDescription).collect(Collectors.joining(","));
                manager.logValidationError(errorsDetails, CONTRACT_UPDATE_EVENT, error.getErrors().get(0).getMessage());

                return error;
            }

            DbRequestResult result = mongoAccess.updateData(queryDsl, FunctionalAdminCollections.INGEST_CONTRACT);
            updateDiffs = result.getDiffs();
            response.addResult(new DbRequestResult(result))
                .setTotal(result.getTotal())
                .setQuery(queryDsl)
                .setHttpCode(Response.Status.OK.getStatusCode());

            result.close();

            functionalBackupService.saveCollectionAndSequence(
                eip,
                CONTRACT_BACKUP_EVENT,
                FunctionalAdminCollections.INGEST_CONTRACT,
                ingestContractModel.getId()
            );

        } catch (SchemaValidationException | BadRequestException exp) {
            LOGGER.error(exp);
            final String err =
                new StringBuilder("Import ingest contracts error > ").append(exp.getMessage()).toString();
            manager.logValidationError(err, CONTRACT_UPDATE_EVENT, UPDATE_CONTRACT_BAD_REQUEST);
            return getVitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem(), exp.getMessage(),
                StatusCode.KO).setHttpCode(Response.Status.BAD_REQUEST.getStatusCode());
        } catch (Exception e) {
            LOGGER.error(e);
            final String err = new StringBuilder("Update ingest contracts error > ").append(e.getMessage()).toString();
            manager.logFatalError(err);
            error.setCode(VitamCode.GLOBAL_INTERNAL_SERVER_ERROR.getItem())
                .setDescription(err)
                .setHttpCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());

            return error;
        }

        manager.logUpdateSuccess(ingestContractModel.getId(), identifier, updateDiffs.get(ingestContractModel.getId()));
        return response;
    }

    private Set<String> getFromJsonNodeOrFromIngestContractModel(IngestContractModel ingestContractModel,
        JsonNode fileFormatTypeNode)throws InvalidParseOperationException {

        if (fileFormatTypeNode != null && fileFormatTypeNode.isArray()) {
            return JsonHandler.getFromString(fileFormatTypeNode.toString(), Set.class, String.class);
        } else if (ingestContractModel.getFormatType() != null) {
            return ingestContractModel.getFormatType();
        }

        return null;
    }

    private void checkFormatsNotEmptyWhenEveryFormatTypeIsFalse(JsonNode queryDsl, Set<String> fileFormatTypes,
        JsonNode fileFormatTypeNode, VitamError error) {
        final JsonNode everyFileformatFlagNode = queryDsl.findValue(IngestContractModel.EVERY_FORMAT_TYPE);

        if (everyFileformatFlagNode != null || fileFormatTypeNode != null) {
            final boolean everyFileFormat = everyFileformatFlagNode != null && everyFileformatFlagNode.asBoolean();
            boolean formatsEmpty = fileFormatTypes == null || fileFormatTypes.isEmpty();
            final boolean canBeUpdate = !formatsEmpty || everyFileFormat;
            if (!canBeUpdate) {
                error.addToErrors(
                    getVitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem(), EVERYFORMAT_LIST_EMPTY,
                        StatusCode.KO).setMessage(UPDATE_CONTRACT_BAD_REQUEST));
            }
        }
    }

    private static VitamError getVitamError(String vitamCode, String error, StatusCode statusCode) {
        return VitamErrorUtils.getVitamError(vitamCode, error, "IngestContract", statusCode);
    }
}
