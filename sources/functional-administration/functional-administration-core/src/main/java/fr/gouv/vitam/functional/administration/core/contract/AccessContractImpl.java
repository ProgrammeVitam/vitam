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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
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
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTION;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTIONARGS;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.UPDATEACTION;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
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
import fr.gouv.vitam.functional.administration.common.VitamErrorUtils;
import fr.gouv.vitam.functional.administration.common.counter.SequenceType;
import fr.gouv.vitam.functional.administration.common.counter.VitamCounterService;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.functional.administration.core.backup.FunctionalBackupService;
import fr.gouv.vitam.functional.administration.core.contract.GenericContractValidator.GenericRejectionCause;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import org.bson.conversions.Bson;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.and;

public class AccessContractImpl implements ContractService<AccessContractModel> {

    public static final String CONTRACT_BACKUP_EVENT = "STP_BACKUP_ACCESS_CONTRACT";
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
    private static final String DATE_MUST_BE_VALID = "must be a valid date";
    private static final String ACCESS_CONTRACT_NOT_FOUND = "Access contract not found";
    private static final String ACCESS_CONTRACT_IS_MANDATORY_PATAMETER =
        "The collection of access contracts is mandatory";
    private static final String UPDATE_ACCESS_CONTRACT_MANDATORY_PATAMETER = "access contracts is mandatory";
    private static final String CONTRACTS_IMPORT_EVENT = "STP_IMPORT_ACCESS_CONTRACT";
    private static final String CONTRACT_UPDATE_EVENT = "STP_UPDATE_ACCESS_CONTRACT";
    private static final String EMPTY_REQUIRED_FIELD =
        CONTRACTS_IMPORT_EVENT + ContractLogbookService.EMPTY_REQUIRED_FIELD;
    private static final String DUPLICATE_IN_DATABASE =
        CONTRACTS_IMPORT_EVENT + ContractLogbookService.DUPLICATE_IN_DATABASE;
    private static final String AGENCY_NOT_FOUND_IN_DATABASE =
        CONTRACTS_IMPORT_EVENT + ContractLogbookService.AGENCY_NOT_FOUND_IN_DATABASE;
    private static final String CONTRACT_VALIDATION_ERROR =
        CONTRACTS_IMPORT_EVENT + ContractLogbookService.CONTRACT_VALIDATION_ERROR;
    private static final String CONTRACT_BAD_REQUEST =
        CONTRACTS_IMPORT_EVENT + ContractLogbookService.CONTRACT_BAD_REQUEST;

    private static final String UPDATE_CONTRACT_NOT_FOUND =
        CONTRACT_UPDATE_EVENT + ContractLogbookService.UPDATE_CONTRACT_NOT_FOUND;
    private static final String UPDATE_CONTRACT_BAD_REQUEST =
        CONTRACT_UPDATE_EVENT + ContractLogbookService.CONTRACT_BAD_REQUEST;
    private static final String UPDATE_VALUE_NOT_IN_ENUM =
        CONTRACT_UPDATE_EVENT + ContractLogbookService.UPDATE_VALUE_NOT_IN_ENUM;
    private static final String UPDATE_AGENCY_NOT_FOUND =
        CONTRACT_UPDATE_EVENT + ContractLogbookService.AGENCY_NOT_FOUND_IN_DATABASE;
    private static final String UPDATE_KO = CONTRACT_UPDATE_EVENT + ".KO";

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AccessContractImpl.class);
    private static final String UND_TENANT = "_tenant";
    private static final String UND_ID = "_id";

    private static final String CONTRACT_KEY = "AccessContract";
    private static final String CONTRACT_CHECK_KEY = "accessContractCheck";

    private final MongoDbAccessAdminImpl mongoAccess;
    private final LogbookOperationsClient logbookClient;
    private final VitamCounterService vitamCounterService;
    private final MetaDataClient metaDataClient;
    private final FunctionalBackupService functionalBackupService;

    /**
     * Constructor
     *
     * @param mongoAccess MongoDB client
     * @param vitamCounterService
     */
    public AccessContractImpl(MongoDbAccessAdminImpl mongoAccess, VitamCounterService vitamCounterService) {
        this(
            mongoAccess,
            vitamCounterService,
            MetaDataClientFactory.getInstance().getClient(),
            LogbookOperationsClientFactory.getInstance().getClient(),
            new FunctionalBackupService(vitamCounterService)
        );
    }

    /**
     * Constructor
     *
     * @param mongoAccess
     * @param vitamCounterService
     * @param metaDataClient
     * @param logbookClient
     * @param functionalBackupService
     */
    @VisibleForTesting
    public AccessContractImpl(
        MongoDbAccessAdminImpl mongoAccess,
        VitamCounterService vitamCounterService,
        MetaDataClient metaDataClient,
        LogbookOperationsClient logbookClient,
        FunctionalBackupService functionalBackupService
    ) {
        this.mongoAccess = mongoAccess;
        this.vitamCounterService = vitamCounterService;
        this.metaDataClient = metaDataClient;
        this.functionalBackupService = functionalBackupService;
        this.logbookClient = logbookClient;
    }

    private static Optional<GenericRejectionCause> selectUnits(
        MetaDataClient metaDataClient,
        AccessContractModel contract,
        String contractName,
        Set<String> checkUnits,
        String unitType
    ) {
        String[] rootUnitArray = checkUnits.toArray(new String[0]);

        final Select select = new Select();
        try {
            select.setQuery(QueryHelper.in(PROJECTIONARGS.ID.exactToken(), rootUnitArray).setDepthLimit(0));
            select.setProjection(
                JsonHandler.createObjectNode()
                    .set(
                        PROJECTION.FIELDS.exactToken(),
                        JsonHandler.createObjectNode().put(PROJECTIONARGS.ID.exactToken(), 1)
                    )
            );
        } catch (InvalidCreateOperationException | InvalidParseOperationException e) {
            return Optional.of(
                GenericRejectionCause.rejectExceptionOccurred(contract.getName(), "Error parse query", e)
            );
        }

        final JsonNode queryDsl = select.getFinalSelect();

        try {
            JsonNode resp = metaDataClient.selectUnits(queryDsl);
            RequestResponseOK<JsonNode> responseOK = RequestResponseOK.getFromJsonNode(resp);
            List<JsonNode> result = responseOK.getResults();
            if (null == result || result.isEmpty()) {
                String guidArrayString = String.join(",", checkUnits);
                switch (unitType) {
                    case "AllUnits":
                        return Optional.of(
                            GenericRejectionCause.rejectExcludedAndRootUnitsNotFound(contractName, guidArrayString)
                        );
                    case AccessContractModel.EXCLUDED_ROOT_UNITS:
                        return Optional.of(
                            GenericRejectionCause.rejectExcludedRootUnitsNotFound(contractName, guidArrayString)
                        );
                    case AccessContractModel.ROOT_UNITS:
                    default:
                        return Optional.of(
                            GenericRejectionCause.rejectRootUnitsNotFound(contractName, guidArrayString)
                        );
                }
            } else if (result.size() == checkUnits.size()) {
                return Optional.empty();
            } else {
                Set<String> notFoundRootUnits = new HashSet<>(checkUnits);
                result.forEach(unit -> notFoundRootUnits.remove(unit.get(VitamFieldsHelper.id()).asText()));
                return Optional.of(
                    GenericRejectionCause.rejectRootUnitsNotFound(contractName, String.join(",", notFoundRootUnits))
                );
            }
        } catch (
            InvalidParseOperationException
            | MetaDataExecutionException
            | MetaDataDocumentSizeException
            | MetaDataClientServerException e
        ) {
            return Optional.of(
                GenericRejectionCause.rejectExceptionOccurred(contract.getName(), "Error while select units", e)
            );
        }
    }

    @Override
    public RequestResponse<AccessContractModel> createContracts(List<AccessContractModel> contractModelList)
        throws VitamException {
        ParametersChecker.checkParameter(ACCESS_CONTRACT_IS_MANDATORY_PATAMETER, contractModelList);

        if (contractModelList.isEmpty()) {
            return new RequestResponseOK<>();
        }
        boolean slaveMode = vitamCounterService.isSlaveFunctionnalCollectionOnTenant(
            SequenceType.ACCESS_CONTRACT_SEQUENCE.getCollection(),
            ParameterHelper.getTenantParameter()
        );
        String operationId = VitamThreadUtils.getVitamSession().getRequestId();
        GUID eip = GUIDReader.getGUID(operationId);

        AccessContractValidationService validationService = new AccessContractValidationService(metaDataClient);
        ContractLogbookService logbookService = new ContractLogbookService(
            logbookClient,
            eip,
            CONTRACTS_IMPORT_EVENT,
            CONTRACT_UPDATE_EVENT,
            CONTRACT_KEY,
            CONTRACT_CHECK_KEY
        );

        logbookService.logStarted();

        ArrayNode contractsToPersist;

        final VitamError<AccessContractModel> error = getVitamError(
            VitamCode.CONTRACT_VALIDATION_ERROR.getItem(),
            "Access contract import error"
        ).setHttpCode(Response.Status.BAD_REQUEST.getStatusCode());
        try {
            for (final AccessContractModel acm : contractModelList) {
                // if a contract have and id
                if (acm.getId() != null) {
                    error.addToErrors(
                        getVitamError(
                            VitamCode.CONTRACT_VALIDATION_ERROR.getItem(),
                            GenericRejectionCause.rejectIdNotAllowedInCreate(acm.getName()).getReason()
                        )
                    );
                    continue;
                }

                // validate contract
                if (validationService.validateContract(acm, acm.getName(), error)) {
                    acm.setId(GUIDFactory.newContractGUID(ParameterHelper.getTenantParameter()).getId());
                }

                if (acm.getTenant() == null) {
                    acm.setTenant(ParameterHelper.getTenantParameter());
                }

                if (slaveMode) {
                    final Optional<GenericRejectionCause> result = validationService
                        .checkEmptyIdentifierSlaveModeValidator()
                        .validate(acm, acm.getIdentifier());
                    result.ifPresent(
                        genericRejectionCause ->
                            error.addToErrors(
                                getVitamError(
                                    VitamCode.CONTRACT_VALIDATION_ERROR.getItem(),
                                    result.get().getReason()
                                ).setMessage(EMPTY_REQUIRED_FIELD)
                            )
                    );
                }
            }
            if (error.getErrors() != null && !error.getErrors().isEmpty()) {
                // log book + application log
                // stop
                final String errorsDetails = error
                    .getErrors()
                    .stream()
                    .map(VitamError::getDescription)
                    .collect(Collectors.joining(","));
                logbookService.logValidationError(
                    errorsDetails,
                    CONTRACTS_IMPORT_EVENT,
                    error.getErrors().get(0).getMessage()
                );
                return error;
            }

            contractsToPersist = JsonHandler.createArrayNode();
            for (final AccessContractModel acm : contractModelList) {
                ContractHelper.setIdentifier(
                    slaveMode,
                    acm,
                    vitamCounterService,
                    SequenceType.ACCESS_CONTRACT_SEQUENCE
                );
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
                eip.toString()
            );

            logbookService.logSuccess();

            return new RequestResponseOK<AccessContractModel>()
                .addAllResults(contractModelList)
                .setHttpCode(Response.Status.CREATED.getStatusCode());
        } catch (SchemaValidationException | BadRequestException exp) {
            LOGGER.error(exp);
            final String err = "Import access contracts error > " + exp.getMessage();
            logbookService.logValidationError(err, CONTRACTS_IMPORT_EVENT, CONTRACT_BAD_REQUEST);
            return getVitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem(), exp.getMessage())
                .setDescription(err)
                .setHttpCode(Response.Status.BAD_REQUEST.getStatusCode());
        } catch (final Exception exp) {
            LOGGER.error(exp);
            final String err = "Import access contracts error > " + exp.getMessage();
            logbookService.logFatalError(err, CONTRACTS_IMPORT_EVENT);
            return error
                .setCode(VitamCode.GLOBAL_INTERNAL_SERVER_ERROR.getItem())
                .setDescription(err)
                .setHttpCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        }
    }

    @Override
    public AccessContractModel findByIdentifier(String identifier)
        throws ReferentialException, InvalidParseOperationException {
        SanityChecker.checkParameter(identifier);
        try (
            DbRequestResult result = ContractHelper.findByIdentifier(
                identifier,
                FunctionalAdminCollections.ACCESS_CONTRACT,
                mongoAccess
            )
        ) {
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
        try (DbRequestResult result = mongoAccess.findDocuments(queryDsl, FunctionalAdminCollections.ACCESS_CONTRACT)) {
            return result.getRequestResponseOK(queryDsl, AccessContract.class, AccessContractModel.class);
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
        VitamError<AccessContractModel> error = getVitamError(
            VitamCode.CONTRACT_VALIDATION_ERROR.getItem(),
            "Access contract update error"
        ).setHttpCode(Response.Status.BAD_REQUEST.getStatusCode());

        final AccessContractModel accContractModel = findByIdentifier(identifier);
        Map<String, List<String>> updateDiffs;
        if (accContractModel == null) {
            error.setHttpCode(Response.Status.NOT_FOUND.getStatusCode());
            return error.addToErrors(
                getVitamError(
                    VitamCode.CONTRACT_VALIDATION_ERROR.getItem(),
                    ACCESS_CONTRACT_NOT_FOUND + identifier
                ).setMessage(UPDATE_CONTRACT_NOT_FOUND)
            );
        }

        String operationId = VitamThreadUtils.getVitamSession().getRequestId();
        GUID eip = GUIDReader.getGUID(operationId);
        AccessContractValidationService validationService = new AccessContractValidationService(metaDataClient);
        ContractLogbookService logbookService = new ContractLogbookService(
            logbookClient,
            eip,
            CONTRACTS_IMPORT_EVENT,
            CONTRACT_UPDATE_EVENT,
            CONTRACT_KEY,
            CONTRACT_CHECK_KEY
        );

        RequestResponseOK<AccessContractModel> response = new RequestResponseOK<>();

        logbookService.logUpdateStarted(accContractModel.getId());
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
                    validateUpdateAction(validationService, accContractModel.getName(), error, field, value);
                }

                ((ObjectNode) fieldName).remove(AbstractContractModel.TAG_CREATION_DATE);
                ((ObjectNode) fieldName).put(
                        AbstractContractModel.TAG_LAST_UPDATE,
                        LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now())
                    );
            }
        }

        if (error.getErrors() != null && error.getErrors().size() > 0) {
            final String errorsDetails = error
                .getErrors()
                .stream()
                .map(VitamError::getDescription)
                .collect(Collectors.joining(","));
            logbookService.logValidationError(
                errorsDetails,
                CONTRACT_UPDATE_EVENT,
                error.getErrors().get(0).getMessage()
            );

            return error;
        }

        try {
            try (
                DbRequestResult result = mongoAccess.updateData(queryDsl, FunctionalAdminCollections.ACCESS_CONTRACT)
            ) {
                updateDiffs = result.getDiffs();
                response
                    .addAllResults(result.getDocuments(AccessContract.class, AccessContractModel.class))
                    .setTotal(result.getTotal())
                    .setQuery(queryDsl)
                    .setHttpCode(Response.Status.OK.getStatusCode());
            }

            functionalBackupService.saveCollectionAndSequence(
                eip,
                CONTRACT_BACKUP_EVENT,
                FunctionalAdminCollections.ACCESS_CONTRACT,
                accContractModel.getId()
            );

            logbookService.logUpdateSuccess(
                accContractModel.getId(),
                identifier,
                updateDiffs.get(accContractModel.getId())
            );
            return response;
        } catch (SchemaValidationException | BadRequestException exp) {
            LOGGER.error(exp);
            final String err = "Update access contract error > " + exp.getMessage();
            logbookService.logValidationError(err, CONTRACT_UPDATE_EVENT, UPDATE_CONTRACT_BAD_REQUEST);
            return getVitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem(), exp.getMessage())
                .setDescription(err)
                .setHttpCode(Response.Status.BAD_REQUEST.getStatusCode());
        } catch (Exception exp) {
            LOGGER.error(exp);
            final String err = "Update access contract error > " + exp.getMessage();
            logbookService.logFatalError(err, CONTRACT_UPDATE_EVENT);
            return error
                .setCode(VitamCode.GLOBAL_INTERNAL_SERVER_ERROR.getItem())
                .setDescription(err)
                .setHttpCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        }
    }

    private void validateUpdateAction(
        AccessContractValidationService validationService,
        String contractName,
        final VitamError<AccessContractModel> error,
        final String field,
        final JsonNode value
    ) {
        switch (field) {
            case AccessContract.STATUS:
                if (
                    !(ActivationStatus.ACTIVE.name().equals(value.asText()) ||
                        ActivationStatus.INACTIVE.name().equals(value.asText()))
                ) {
                    error.addToErrors(
                        getVitamError(
                            VitamCode.CONTRACT_VALIDATION_ERROR.getItem(),
                            THE_ACCESS_CONTRACT_STATUS_MUST_BE_ACTIVE_OR_INACTIVE_BUT_NOT + value.asText()
                        ).setMessage(UPDATE_VALUE_NOT_IN_ENUM)
                    );
                }
                break;
            case AccessContract.LAST_UPDATE:
            case AccessContract.CREATIONDATE:
            case AccessContract.ACTIVATIONDATE:
            case AccessContract.DEACTIVATIONDATE:
                try {
                    LocalDateUtil.getFormattedDateForMongo(value.asText());
                } catch (DateTimeParseException e) {
                    error.addToErrors(
                        getVitamError(
                            VitamCode.CONTRACT_VALIDATION_ERROR.getItem(),
                            String.format("%s %s", field, DATE_MUST_BE_VALID)
                        ).setMessage(UPDATE_CONTRACT_BAD_REQUEST)
                    );
                }
                break;
            case AccessContractModel.EVERY_ORIGINATINGAGENCY:
                if (!(value instanceof BooleanNode)) {
                    error.addToErrors(
                        getVitamError(
                            VitamCode.CONTRACT_VALIDATION_ERROR.getItem(),
                            THE_ACCESS_CONTRACT_EVERY_ORIGINATING_AGENCY_MUST_BE_TRUE_OR_FALSE_BUT_NOT + value.asText()
                        ).setMessage(UPDATE_VALUE_NOT_IN_ENUM)
                    );
                }
                break;
            case AccessContractModel.ORIGINATING_AGENCIES:
                if (!value.isArray()) {
                    error.addToErrors(
                        getVitamError(
                            VitamCode.CONTRACT_VALIDATION_ERROR.getItem(),
                            ORIGINATING_AGENCIES_INVALID + value.asText()
                        ).setMessage(UPDATE_VALUE_NOT_IN_ENUM)
                    );
                } else {
                    try {
                        Set<String> originatingAgencies = JsonHandler.getFromJsonNode(value, new TypeReference<>() {});
                        AccessContractModel toValidate = new AccessContractModel();
                        toValidate.setOriginatingAgencies(originatingAgencies);
                        Optional<GenericRejectionCause> rejection = validationService
                            .checkExistenceOriginatingAgenciesValidator()
                            .validate(toValidate, contractName);

                        // Validation error
                        rejection.ifPresent(
                            genericRejectionCause ->
                                error.addToErrors(
                                    getVitamError(
                                        VitamCode.CONTRACT_VALIDATION_ERROR.getItem(),
                                        genericRejectionCause.getReason()
                                    ).setMessage(UPDATE_AGENCY_NOT_FOUND)
                                )
                        );
                    } catch (InvalidParseOperationException e) {
                        error.addToErrors(
                            getVitamError(
                                VitamCode.CONTRACT_VALIDATION_ERROR.getItem(),
                                ROOT_UNIT_INVALID + value.asText()
                            )
                        );
                    }
                }
                break;
            case AccessContractModel.EVERY_DATA_OBJECT_VERSION:
                if (!(value instanceof BooleanNode)) {
                    error.addToErrors(
                        getVitamError(
                            VitamCode.CONTRACT_VALIDATION_ERROR.getItem(),
                            THE_ACCESS_CONTRACT_EVERY_DATA_OBJECT_VERSION_MUST_BE_TRUE_OR_FALSE_BUT_NOT + value.asText()
                        ).setMessage(UPDATE_VALUE_NOT_IN_ENUM)
                    );
                }
                break;
            case AccessContractModel.DATA_OBJECT_VERSION:
                if (!validateObjectVersion(value)) {
                    error.addToErrors(
                        getVitamError(
                            VitamCode.CONTRACT_VALIDATION_ERROR.getItem(),
                            DATA_OBJECT_VERSION_INVALID + value.asText()
                        ).setMessage(UPDATE_VALUE_NOT_IN_ENUM)
                    );
                }
                break;
            case AccessContractModel.ROOT_UNITS:
                // Validate that RootUnits if not empty exists in database
                if (!value.isArray()) {
                    error.addToErrors(
                        getVitamError(
                            VitamCode.CONTRACT_VALIDATION_ERROR.getItem(),
                            ROOT_UNIT_INVALID + value.asText()
                        ).setMessage(UPDATE_KO)
                    );
                } else {
                    try {
                        Set<String> rootUnits = JsonHandler.getFromJsonNode(value, Set.class, String.class);
                        AccessContractModel toValidate = new AccessContractModel();
                        toValidate.setRootUnits(rootUnits);
                        Optional<GenericRejectionCause> rejection = validationService
                            .validateExistsArchiveUnits(metaDataClient)
                            .validate(toValidate, contractName);

                        // Validation error
                        rejection.ifPresent(
                            genericRejectionCause ->
                                error.addToErrors(
                                    getVitamError(
                                        VitamCode.CONTRACT_VALIDATION_ERROR.getItem(),
                                        genericRejectionCause.getReason()
                                    ).setMessage(UPDATE_KO)
                                )
                        );
                    } catch (InvalidParseOperationException e) {
                        error.addToErrors(
                            getVitamError(
                                VitamCode.CONTRACT_VALIDATION_ERROR.getItem(),
                                ROOT_UNIT_INVALID + value.asText()
                            ).setMessage(UPDATE_KO)
                        );
                    }
                }
                break;
            case AccessContractModel.EXCLUDED_ROOT_UNITS:
                // Validate that ExcludedRootUnits, if not empty, exists in database
                if (!value.isArray()) {
                    error.addToErrors(
                        getVitamError(
                            VitamCode.CONTRACT_VALIDATION_ERROR.getItem(),
                            EXCLUDED_ROOT_UNIT_INVALID + value.asText()
                        ).setMessage(UPDATE_KO)
                    );
                } else {
                    try {
                        Set<String> excludedRootUnits = JsonHandler.getFromJsonNode(value, Set.class, String.class);
                        AccessContractModel toValidate = new AccessContractModel();
                        toValidate.setExcludedRootUnits(excludedRootUnits);
                        Optional<GenericRejectionCause> rejection = validationService
                            .validateExistsArchiveUnits(metaDataClient)
                            .validate(toValidate, contractName);

                        // Validation error
                        rejection.ifPresent(
                            genericRejectionCause ->
                                error.addToErrors(
                                    getVitamError(
                                        VitamCode.CONTRACT_VALIDATION_ERROR.getItem(),
                                        genericRejectionCause.getReason()
                                    ).setMessage(UPDATE_KO)
                                )
                        );
                    } catch (InvalidParseOperationException e) {
                        error.addToErrors(
                            getVitamError(
                                VitamCode.CONTRACT_VALIDATION_ERROR.getItem(),
                                ROOT_UNIT_INVALID + value.asText()
                            ).setMessage(UPDATE_KO)
                        );
                    }
                }
                break;
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

    private VitamError<AccessContractModel> getVitamError(String vitamCode, String error) {
        return VitamErrorUtils.getVitamError(
            vitamCode,
            error,
            "AccessContract",
            StatusCode.KO,
            AccessContractModel.class
        );
    }

    /**
     * Contract validator and logBook manager
     */
    protected static final class AccessContractValidationService {

        private final Map<AccessContractValidator, String> validators;

        public AccessContractValidationService(MetaDataClient metaDataClient) {
            // Init validator
            validators = new HashMap<>() {
                {
                    put(createMandatoryParamsValidator(), EMPTY_REQUIRED_FIELD);
                    put(createWrongFieldFormatValidator(), EMPTY_REQUIRED_FIELD);
                    put(checkExistenceOriginatingAgenciesValidator(), AGENCY_NOT_FOUND_IN_DATABASE);
                    put(createCheckDuplicateInDatabaseValidator(), DUPLICATE_IN_DATABASE);
                    put(validateExistsArchiveUnits(metaDataClient), CONTRACT_VALIDATION_ERROR);
                }
            };
        }

        private boolean validateContract(
            AccessContractModel contract,
            String jsonFormat,
            VitamError<AccessContractModel> error
        ) {
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
                            StatusCode.KO,
                            AccessContractModel.class
                        )
                            .setMessage(validators.get(validator))
                            .setHttpCode(Response.Status.BAD_REQUEST.getStatusCode())
                    );
                    // once a validation error is detected on a contract, jump to next contract
                    return false;
                }
            }
            return true;
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
                        contract.setActivationdate(
                            LocalDateUtil.getFormattedDateForMongo(contract.getActivationdate())
                        );
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
                            LocalDateUtil.getFormattedDateForMongo(contract.getDeactivationdate())
                        );
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
                    return Optional.of(GenericRejectionCause.rejectMandatoryMissing(AccessContract.IDENTIFIER));
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
                    final Bson clause = and(
                        Filters.eq(VitamDocument.TENANT_ID, tenant),
                        Filters.eq(AccessContract.IDENTIFIER, contract.getIdentifier())
                    );
                    final boolean exist =
                        FunctionalAdminCollections.ACCESS_CONTRACT.getCollection().countDocuments(clause) > 0;
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

                final Bson clause = and(
                    Filters.eq(VitamDocument.TENANT_ID, tenant),
                    Filters.in(Agencies.IDENTIFIER, contract.getOriginatingAgencies())
                );

                FindIterable<Agencies> find = FunctionalAdminCollections.AGENCIES.<Agencies>getCollection()
                    .find(clause)
                    .projection(new BasicDBObject(Agencies.IDENTIFIER, 1));

                MongoCursor<Agencies> it = find.iterator();
                Set<String> notFound = new HashSet<>(contract.getOriginatingAgencies());

                while (it.hasNext()) {
                    final Agencies next = it.next();
                    notFound.remove(next.getIdentifier());
                }

                if (!notFound.isEmpty()) {
                    return Optional.of(
                        GenericRejectionCause.rejectRootUnitsNotFound(contractName, String.join(",", notFound))
                    );
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
                Set<String> excludeUnits = contract.getExcludedRootUnits() == null
                    ? new HashSet<>()
                    : contract.getExcludedRootUnits();

                if (!includeUnits.isEmpty() && !excludeUnits.isEmpty()) {
                    checkUnits.addAll(includeUnits);
                    checkUnits.addAll(excludeUnits);

                    checkUnits.removeIf(unit -> unit.trim().isEmpty());
                    if (checkUnits.isEmpty()) {
                        return Optional.empty();
                    }

                    return selectUnits(metaDataClient, contract, contractName, checkUnits, "AllUnits");
                } else if (!excludeUnits.isEmpty()) {
                    checkUnits = excludeUnits;
                    return selectUnits(
                        metaDataClient,
                        contract,
                        contractName,
                        checkUnits,
                        AccessContractModel.EXCLUDED_ROOT_UNITS
                    );
                } else if (!includeUnits.isEmpty()) {
                    checkUnits = includeUnits;
                    return selectUnits(
                        metaDataClient,
                        contract,
                        contractName,
                        checkUnits,
                        AccessContractModel.ROOT_UNITS
                    );
                } else {
                    return Optional.empty();
                }
            };
        }
    }
}
