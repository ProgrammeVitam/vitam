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
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken;
import fr.gouv.vitam.common.database.server.DbRequestResult;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.SchemaValidationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
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
import fr.gouv.vitam.common.model.administration.ActivationStatus;
import fr.gouv.vitam.common.model.administration.DataObjectVersionType;
import fr.gouv.vitam.common.model.administration.ManagementContractModel;
import fr.gouv.vitam.common.model.administration.StorageDetailModel;
import fr.gouv.vitam.common.model.administration.VersionRetentionPolicyModel;
import fr.gouv.vitam.common.model.administration.VersionUsageModel;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.ManagementContract;
import fr.gouv.vitam.functional.administration.common.VitamErrorUtils;
import fr.gouv.vitam.functional.administration.common.counter.SequenceType;
import fr.gouv.vitam.functional.administration.common.counter.VitamCounterService;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessReferential;
import fr.gouv.vitam.functional.administration.core.backup.FunctionalBackupService;
import fr.gouv.vitam.functional.administration.core.contract.GenericContractValidator.GenericRejectionCause;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.common.exception.StorageException;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageStrategy;
import fr.gouv.vitam.storage.engine.common.utils.StorageStrategyNotFoundException;
import fr.gouv.vitam.storage.engine.common.utils.StorageStrategyReferentOfferException;
import fr.gouv.vitam.storage.engine.common.utils.StorageStrategyUtils;
import org.apache.commons.lang3.EnumUtils;

import javax.ws.rs.core.Response;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.model.administration.DataObjectVersionType.BINARY_MASTER;
import static fr.gouv.vitam.common.model.administration.VersionUsageModel.IntermediaryVersionEnum;
import static fr.gouv.vitam.common.model.administration.VersionUsageModel.IntermediaryVersionEnum.ALL;
import static fr.gouv.vitam.common.model.administration.VersionUsageModel.IntermediaryVersionEnum.LAST;
import static fr.gouv.vitam.functional.administration.common.ManagementContract.DEFAULT_USAGE;
import static fr.gouv.vitam.functional.administration.common.ManagementContract.INITIAL_VERSION;
import static fr.gouv.vitam.functional.administration.common.ManagementContract.INTERMEDIARY_VERSION;
import static fr.gouv.vitam.functional.administration.common.ManagementContract.VERSION_RETENTION_POLICY;

public class ManagementContractImpl implements ContractService<ManagementContractModel> {

    public static final String CONTRACT_BACKUP_EVENT = "STP_BACKUP_MANAGEMENT_CONTRACT";
    public static final String INVALID_INTERMEDIARY_VERSION_TYPE = "%s is an invalid intermediary version type.";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ManagementContractImpl.class);
    private static final String MANAGEMENT_CONTRACT_NOT_FOUND = "Management contract not found";
    private static final String CONTRACT_IS_MANDATORY_PARAMETER = "The collection of management contracts is mandatory";
    private static final String THE_MANAGEMENT_CONTRACT_STATUS_MUST_BE_ACTIVE_OR_INACTIVE_BUT_NOT =
        "The management contract status must be ACTIVE or INACTIVE but not ";
    private static final String DATE_MUST_BE_VALID = "must be a valid date";
    private static final String CONTRACTS_IMPORT_EVENT = "STP_IMPORT_MANAGEMENT_CONTRACT";
    private static final String CONTRACT_UPDATE_EVENT = "STP_UPDATE_MANAGEMENT_CONTRACT";
    private static final String EMPTY_REQUIRED_FIELD =
        CONTRACTS_IMPORT_EVENT + ContractLogbookService.EMPTY_REQUIRED_FIELD;
    private static final String DUPLICATE_IN_DATABASE =
        CONTRACTS_IMPORT_EVENT + ContractLogbookService.DUPLICATE_IN_DATABASE;
    private static final String STRATEGY_VALIDATION_ERROR =
        CONTRACTS_IMPORT_EVENT + ContractLogbookService.STRATEGY_VALIDATION_ERROR;
    private static final String CONTRACT_BAD_REQUEST =
        CONTRACTS_IMPORT_EVENT + ContractLogbookService.CONTRACT_BAD_REQUEST;
    private static final String VERSION_RETENTION_POLICY_VALIDATION_ERROR =
        CONTRACTS_IMPORT_EVENT + ContractLogbookService.VERSION_RETENTION_POLICY_VALIDATION_ERROR;
    private static final String UPDATE_CONTRACT_NOT_FOUND =
        CONTRACT_UPDATE_EVENT + ContractLogbookService.UPDATE_CONTRACT_NOT_FOUND;
    private static final String UPDATE_CONTRACT_BAD_REQUEST =
        CONTRACT_UPDATE_EVENT + ContractLogbookService.CONTRACT_BAD_REQUEST;
    private static final String UPDATE_VALUE_NOT_IN_ENUM =
        CONTRACT_UPDATE_EVENT + ContractLogbookService.UPDATE_VALUE_NOT_IN_ENUM;
    private static final String UPDATE_STRATEGY_VALIDATION_ERROR =
        CONTRACT_UPDATE_EVENT + ContractLogbookService.STRATEGY_VALIDATION_ERROR;
    private static final String UPDATE_VERSION_RETENTION_POLICY_VALIDATION_ERROR =
        CONTRACT_UPDATE_EVENT + ContractLogbookService.VERSION_RETENTION_POLICY_VALIDATION_ERROR;
    private static final String UND_TENANT = "_tenant";
    private static final String UND_ID = "_id";
    private static final String CONTRACT_KEY = "ManagementContract";
    private static final String CONTRACT_CHECK_KEY = "managementContractCheck";
    private final MongoDbAccessReferential mongoAccess;

    private final VitamCounterService vitamCounterService;
    private final FunctionalBackupService functionalBackupService;

    private final StorageClient storageClient;
    private final LogbookOperationsClient logbookClient;

    public ManagementContractImpl(MongoDbAccessAdminImpl mongoAccess, VitamCounterService vitamCounterService) {
        this(
            mongoAccess,
            vitamCounterService,
            StorageClientFactory.getInstance().getClient(),
            LogbookOperationsClientFactory.getInstance().getClient(),
            new FunctionalBackupService(vitamCounterService)
        );
    }

    public ManagementContractImpl(
        MongoDbAccessAdminImpl mongoAccess,
        VitamCounterService vitamCounterService,
        StorageClient storageClient,
        LogbookOperationsClient logbookOperationsClient,
        FunctionalBackupService functionalBackupService
    ) {
        this.mongoAccess = mongoAccess;
        this.vitamCounterService = vitamCounterService;
        this.functionalBackupService = functionalBackupService;
        this.storageClient = storageClient;
        this.logbookClient = logbookOperationsClient;
    }

    private static VitamError<ManagementContractModel> getVitamError(String vitamCode, String error) {
        return VitamErrorUtils.getVitamError(
            vitamCode,
            error,
            CONTRACT_KEY,
            StatusCode.KO,
            ManagementContractModel.class
        );
    }

    @Override
    public RequestResponse<ManagementContractModel> createContracts(List<ManagementContractModel> contractModelList)
        throws VitamException {
        ParametersChecker.checkParameter(CONTRACT_IS_MANDATORY_PARAMETER, contractModelList);

        if (contractModelList.isEmpty()) {
            return new RequestResponseOK<>();
        }

        boolean slaveMode = vitamCounterService.isSlaveFunctionnalCollectionOnTenant(
            SequenceType.MANAGEMENT_CONTRACT_SEQUENCE.getCollection(),
            ParameterHelper.getTenantParameter()
        );
        String operationId = VitamThreadUtils.getVitamSession().getRequestId();
        GUID eip = GUIDReader.getGUID(operationId);

        ManagementContractValidationService validationService = new ManagementContractValidationService(
            storageClient,
            mongoAccess
        );
        ContractLogbookService logbookService = new ContractLogbookService(
            logbookClient,
            eip,
            CONTRACTS_IMPORT_EVENT,
            CONTRACT_UPDATE_EVENT,
            CONTRACT_KEY,
            CONTRACT_CHECK_KEY
        );

        logbookService.logStarted();

        final VitamError<ManagementContractModel> error = getVitamError(
            VitamCode.CONTRACT_VALIDATION_ERROR.getItem(),
            "Management contract import error"
        ).setHttpCode(Response.Status.BAD_REQUEST.getStatusCode());

        try {
            for (final ManagementContractModel mcm : contractModelList) {
                // if a contract have and id
                if (null != mcm.getId()) {
                    error.addToErrors(
                        getVitamError(
                            VitamCode.CONTRACT_VALIDATION_ERROR.getItem(),
                            GenericContractValidator.GenericRejectionCause.rejectIdNotAllowedInCreate(
                                mcm.getName()
                            ).getReason()
                        )
                    );
                    continue;
                }

                // Set default versionPolicyRetention if not present
                if (mcm.getVersionRetentionPolicy() == null) {
                    mcm.setVersionRetentionPolicy(new VersionRetentionPolicyModel());
                    mcm.getVersionRetentionPolicy().setInitialVersion(true);
                    mcm.getVersionRetentionPolicy().setIntermediaryVersion(LAST);
                }

                // validate contract
                if (validationService.validateContract(mcm, mcm.getName(), error)) {
                    mcm.setId(GUIDFactory.newContractGUID(ParameterHelper.getTenantParameter()).getId());
                }
                if (mcm.getTenant() == null) {
                    mcm.setTenant(ParameterHelper.getTenantParameter());
                }

                if (slaveMode) {
                    final Optional<GenericContractValidator.GenericRejectionCause> result = validationService
                        .checkEmptyIdentifierSlaveModeValidator()
                        .validate(mcm, mcm.getIdentifier());
                    result.ifPresent(
                        t ->
                            error.addToErrors(
                                getVitamError(
                                    VitamCode.CONTRACT_VALIDATION_ERROR.getItem(),
                                    result.get().getReason()
                                ).setMessage(EMPTY_REQUIRED_FIELD)
                            )
                    );
                }
            }

            if (null != error.getErrors() && !error.getErrors().isEmpty()) {
                // log book + application log
                // stop
                final String errorsDetails = error
                    .getErrors()
                    .stream()
                    .map(VitamError::getDescription)
                    .distinct()
                    .collect(Collectors.joining(","));

                logbookService.logValidationError(
                    errorsDetails,
                    CONTRACTS_IMPORT_EVENT,
                    error.getErrors().get(0).getMessage()
                );
                return error;
            }

            ArrayNode contractsToPersist = JsonHandler.createArrayNode();
            for (final ManagementContractModel mcm : contractModelList) {
                ContractHelper.setIdentifier(
                    slaveMode,
                    mcm,
                    vitamCounterService,
                    SequenceType.MANAGEMENT_CONTRACT_SEQUENCE
                );
                final ObjectNode managementContractNode = (ObjectNode) JsonHandler.toJsonNode(mcm);
                JsonNode hashId = managementContractNode.remove(VitamFieldsHelper.id());
                if (hashId != null) {
                    managementContractNode.set(UND_ID, hashId);
                }

                JsonNode hashTenant = managementContractNode.remove(VitamFieldsHelper.tenant());
                if (hashTenant != null) {
                    managementContractNode.set(UND_TENANT, hashTenant);
                }
                /* contract is valid, add it to the list to persist */
                contractsToPersist.add(managementContractNode);
            }

            // at this point no exception occurred and no validation error detected
            // persist in collection
            // TODO: 3/28/17 create insertDocuments method that accepts VitamDocument
            // instead of ArrayNode, so we can
            // use ManagementContract at this point
            mongoAccess.insertDocuments(contractsToPersist, FunctionalAdminCollections.MANAGEMENT_CONTRACT).close();

            functionalBackupService.saveCollectionAndSequence(
                eip,
                CONTRACT_BACKUP_EVENT,
                FunctionalAdminCollections.MANAGEMENT_CONTRACT,
                eip.toString()
            );

            logbookService.logSuccess();

            return new RequestResponseOK<ManagementContractModel>()
                .addAllResults(contractModelList)
                .setHttpCode(Response.Status.CREATED.getStatusCode());
        } catch (SchemaValidationException | BadRequestException exp) {
            LOGGER.error(exp);
            final String err = "Import management contracts error > " + exp.getMessage();
            logbookService.logValidationError(err, CONTRACTS_IMPORT_EVENT, CONTRACT_BAD_REQUEST);
            return getVitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem(), exp.getMessage())
                .setDescription(err)
                .setHttpCode(Response.Status.BAD_REQUEST.getStatusCode());
        } catch (final Exception exp) {
            LOGGER.error(exp);
            final String err = "Import management contracts error > " + exp.getMessage();
            logbookService.logFatalError(err, CONTRACTS_IMPORT_EVENT);
            return error
                .setCode(VitamCode.GLOBAL_INTERNAL_SERVER_ERROR.getItem())
                .setDescription(err)
                .setHttpCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        }
    }

    @Override
    public RequestResponse<ManagementContractModel> updateContract(String identifier, JsonNode queryDsl)
        throws VitamException {
        VitamError<ManagementContractModel> error = getVitamError(
            VitamCode.CONTRACT_VALIDATION_ERROR.getItem(),
            "Management contract update error"
        ).setHttpCode(Response.Status.BAD_REQUEST.getStatusCode());

        if (queryDsl == null || !queryDsl.isObject()) {
            return error;
        }

        final ManagementContractModel managementContractModel = findByIdentifier(identifier);
        if (managementContractModel == null) {
            error.setHttpCode(Response.Status.NOT_FOUND.getStatusCode());
            return error.addToErrors(
                getVitamError(
                    VitamCode.CONTRACT_VALIDATION_ERROR.getItem(),
                    MANAGEMENT_CONTRACT_NOT_FOUND + identifier
                ).setMessage(UPDATE_CONTRACT_NOT_FOUND)
            );
        }

        String operationId = VitamThreadUtils.getVitamSession().getRequestId();
        GUID eip = GUIDReader.getGUID(operationId);
        RequestResponseOK<ManagementContractModel> response = new RequestResponseOK<>();

        ManagementContractValidationService validationService = new ManagementContractValidationService(
            storageClient,
            mongoAccess
        );
        ContractLogbookService logbookService = new ContractLogbookService(
            logbookClient,
            eip,
            CONTRACTS_IMPORT_EVENT,
            CONTRACT_UPDATE_EVENT,
            CONTRACT_KEY,
            CONTRACT_CHECK_KEY
        );

        logbookService.logUpdateStarted(managementContractModel.getId());

        boolean isVersionRetentionPolicyUpdated = false;
        final JsonNode actionNode = queryDsl.get(BuilderToken.GLOBAL.ACTION.exactToken());
        for (final JsonNode fieldToSet : actionNode) {
            final JsonNode fieldName = fieldToSet.get(BuilderToken.UPDATEACTION.SET.exactToken());
            if (fieldName != null) {
                final Iterator<String> it = fieldName.fieldNames();
                while (it.hasNext()) {
                    final String field = it.next();
                    final JsonNode value = fieldName.findValue(field);
                    if (field.contains(VERSION_RETENTION_POLICY)) {
                        isVersionRetentionPolicyUpdated = true;
                    }
                    validateUpdateAction(
                        validationService,
                        managementContractModel.getName(),
                        error,
                        field,
                        value,
                        managementContractModel
                    );
                }
                ((ObjectNode) fieldName).remove(AbstractContractModel.TAG_CREATION_DATE);
                ((ObjectNode) fieldName).put(
                        AbstractContractModel.TAG_LAST_UPDATE,
                        LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now())
                    );
            }
        }

        // Add Version retention policy if does not exists
        if (managementContractModel.getVersionRetentionPolicy() == null && !isVersionRetentionPolicyUpdated) {
            VersionRetentionPolicyModel versionRetentionPolicyModel = new VersionRetentionPolicyModel();
            versionRetentionPolicyModel.setInitialVersion(true);
            versionRetentionPolicyModel.setIntermediaryVersion(LAST);
            final ObjectNode versionRetentionPolicyNode = JsonHandler.createObjectNode();
            versionRetentionPolicyNode.set(
                VERSION_RETENTION_POLICY,
                JsonHandler.toJsonNode(versionRetentionPolicyModel)
            );
            ObjectNode setFields = JsonHandler.createObjectNode();
            setFields.set(BuilderToken.UPDATEACTION.SET.exactToken(), versionRetentionPolicyNode);
            ((ArrayNode) actionNode).add(setFields);
        }

        Map<String, List<String>> updateDiffs;
        try {
            if (error.getErrors() != null && !error.getErrors().isEmpty()) {
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

            DbRequestResult result = mongoAccess.updateData(queryDsl, FunctionalAdminCollections.MANAGEMENT_CONTRACT);
            updateDiffs = result.getDiffs();
            response
                .addAllResults(result.getDocuments(ManagementContract.class, ManagementContractModel.class))
                .setTotal(result.getTotal())
                .setQuery(queryDsl)
                .setHttpCode(Response.Status.OK.getStatusCode());

            result.close();

            functionalBackupService.saveCollectionAndSequence(
                eip,
                CONTRACT_BACKUP_EVENT,
                FunctionalAdminCollections.MANAGEMENT_CONTRACT,
                managementContractModel.getId()
            );

            logbookService.logUpdateSuccess(
                managementContractModel.getId(),
                identifier,
                updateDiffs.get(managementContractModel.getId())
            );
            return response;
        } catch (SchemaValidationException | BadRequestException exp) {
            LOGGER.error(exp);
            final String err = "Update management contract error > " + exp.getMessage();
            logbookService.logValidationError(err, CONTRACT_UPDATE_EVENT, UPDATE_CONTRACT_BAD_REQUEST);
            return getVitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem(), exp.getMessage())
                .setDescription(err)
                .setHttpCode(Response.Status.BAD_REQUEST.getStatusCode());
        } catch (Exception e) {
            LOGGER.error(e);
            final String err = "Update management contract error > " + e.getMessage();
            logbookService.logFatalError(err, CONTRACT_UPDATE_EVENT);
            error
                .setCode(VitamCode.GLOBAL_INTERNAL_SERVER_ERROR.getItem())
                .setDescription(err)
                .setHttpCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());

            return error;
        }
    }

    @Override
    public ManagementContractModel findByIdentifier(String identifier)
        throws ReferentialException, InvalidParseOperationException {
        SanityChecker.checkParameter(identifier);
        try (
            DbRequestResult result = ContractHelper.findByIdentifier(
                identifier,
                FunctionalAdminCollections.MANAGEMENT_CONTRACT,
                mongoAccess
            )
        ) {
            final List<ManagementContractModel> list = result.getDocuments(
                ManagementContract.class,
                ManagementContractModel.class
            );
            if (list.isEmpty()) {
                return null;
            }
            return list.get(0);
        }
    }

    @Override
    public RequestResponseOK<ManagementContractModel> findContracts(JsonNode queryDsl)
        throws ReferentialException, InvalidParseOperationException {
        SanityChecker.checkJsonAll(queryDsl);
        try (
            DbRequestResult result = mongoAccess.findDocuments(queryDsl, FunctionalAdminCollections.MANAGEMENT_CONTRACT)
        ) {
            return result.getRequestResponseOK(queryDsl, ManagementContract.class, ManagementContractModel.class);
        }
    }

    @Override
    public void close() {
        logbookClient.close();
        storageClient.close();
    }

    private void validateUpdateAction(
        ManagementContractValidationService validationService,
        String contractName,
        final VitamError<ManagementContractModel> error,
        final String field,
        final JsonNode value,
        ManagementContractModel managementContractModel
    ) {
        switch (field) {
            case ManagementContract.STATUS: {
                if (
                    !(ActivationStatus.ACTIVE.name().equals(value.asText()) ||
                        ActivationStatus.INACTIVE.name().equals(value.asText()))
                ) {
                    error.addToErrors(
                        getVitamError(
                            VitamCode.CONTRACT_VALIDATION_ERROR.getItem(),
                            THE_MANAGEMENT_CONTRACT_STATUS_MUST_BE_ACTIVE_OR_INACTIVE_BUT_NOT + value.asText()
                        ).setMessage(UPDATE_VALUE_NOT_IN_ENUM)
                    );
                }
                break;
            }
            case ManagementContract.LAST_UPDATE:
            case ManagementContract.CREATIONDATE:
            case ManagementContract.ACTIVATIONDATE:
            case ManagementContract.DEACTIVATIONDATE:
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
        }

        validateStorage(validationService, contractName, error, field, value);
        validateVersionRetentionPolicy(validationService, contractName, error, field, value, managementContractModel);
    }

    private void validateStorage(
        ManagementContractValidationService validationService,
        String contractName,
        VitamError<ManagementContractModel> error,
        String field,
        JsonNode value
    ) {
        if (ManagementContract.STORAGE.equals(field)) {
            final Iterator<String> it = value.fieldNames();
            while (it.hasNext()) {
                final String subField = it.next();
                final JsonNode subValue = value.findValue(subField);
                validateUpdateAction(validationService, contractName, error, subField, subValue, null);
            }
        }

        if (
            ManagementContract.OBJECTGROUP_STRATEGY.equals(field) ||
            StorageDetailModel.TAG_OBJECT_GROUP_STRATEGY.equals(field)
        ) {
            ManagementContractModel toValidate = new ManagementContractModel();
            toValidate.setStorage(new StorageDetailModel().setObjectGroupStrategy(value.asText()));
            Optional<GenericRejectionCause> rejection = validationService
                .checkStorageStrategies()
                .validate(toValidate, contractName);
            rejection.ifPresent(
                genericRejectionCause ->
                    error.addToErrors(
                        getVitamError(
                            VitamCode.CONTRACT_VALIDATION_ERROR.getItem(),
                            genericRejectionCause.getReason()
                        ).setMessage(UPDATE_STRATEGY_VALIDATION_ERROR)
                    )
            );
        }

        if (ManagementContract.UNIT_STRATEGY.equals(field) || StorageDetailModel.TAG_UNIT_STRATEGY.equals(field)) {
            ManagementContractModel toValidate = new ManagementContractModel();
            toValidate.setStorage(new StorageDetailModel().setUnitStrategy(value.asText()));
            Optional<GenericRejectionCause> rejection = validationService
                .checkStorageStrategies()
                .validate(toValidate, contractName);
            rejection.ifPresent(
                genericRejectionCause ->
                    error.addToErrors(
                        getVitamError(
                            VitamCode.CONTRACT_VALIDATION_ERROR.getItem(),
                            genericRejectionCause.getReason()
                        ).setMessage(UPDATE_STRATEGY_VALIDATION_ERROR)
                    )
            );
        }

        if (ManagementContract.OBJECT_STRATEGY.equals(field) || StorageDetailModel.TAG_OBJECT_STRATEGY.equals(field)) {
            ManagementContractModel toValidate = new ManagementContractModel();
            toValidate.setStorage(new StorageDetailModel().setObjectStrategy(value.asText()));
            Optional<GenericRejectionCause> rejection = validationService
                .checkStorageStrategies()
                .validate(toValidate, contractName);
            rejection.ifPresent(
                genericRejectionCause ->
                    error.addToErrors(
                        getVitamError(
                            VitamCode.CONTRACT_VALIDATION_ERROR.getItem(),
                            genericRejectionCause.getReason()
                        ).setMessage(UPDATE_STRATEGY_VALIDATION_ERROR)
                    )
            );
        }
    }

    private void validateVersionRetentionPolicy(
        ManagementContractValidationService validationService,
        String contractName,
        VitamError<ManagementContractModel> error,
        String field,
        JsonNode value,
        ManagementContractModel managementContractModel
    ) {
        if (ManagementContract.VERSION_RETENTION_POLICY.equals(field)) {
            final Iterator<String> it = value.fieldNames();
            while (it.hasNext()) {
                final String subField = it.next();
                final JsonNode subValue = value.findValue(subField);
                validateUpdateAction(
                    validationService,
                    contractName,
                    error,
                    subField,
                    subValue,
                    managementContractModel
                );
            }
        }

        ManagementContractModel managementContractModeltoValidate = new ManagementContractModel()
            .setVersionRetentionPolicy(new VersionRetentionPolicyModel());
        if (managementContractModel != null) {
            if (managementContractModel.getVersionRetentionPolicy() == null) {
                managementContractModeltoValidate.setVersionRetentionPolicy(new VersionRetentionPolicyModel());
                managementContractModeltoValidate.getVersionRetentionPolicy().setInitialVersion(true);
                managementContractModeltoValidate.getVersionRetentionPolicy().setIntermediaryVersion(LAST);
            } else {
                managementContractModeltoValidate
                    .getVersionRetentionPolicy()
                    .setInitialVersion(managementContractModel.getVersionRetentionPolicy().getInitialVersion());
                managementContractModeltoValidate
                    .getVersionRetentionPolicy()
                    .setIntermediaryVersion(
                        managementContractModel.getVersionRetentionPolicy().getIntermediaryVersion()
                    );
            }
        }

        if (
            List.of(
                ManagementContract.INITIAL_VERSION,
                VersionRetentionPolicyModel.TAG_VERSION_RETENTION_POLICY_INITIAL_VERSION
            ).contains(field)
        ) {
            managementContractModeltoValidate.getVersionRetentionPolicy().setInitialVersion(value.asBoolean());
            Optional<GenericRejectionCause> rejection = validationService
                .checkVersionRetentionPolicy()
                .validate(managementContractModeltoValidate, contractName);
            rejection.ifPresent(
                genericRejectionCause ->
                    error.addToErrors(
                        getVitamError(
                            VitamCode.CONTRACT_VALIDATION_ERROR.getItem(),
                            genericRejectionCause.getReason()
                        ).setMessage(UPDATE_VERSION_RETENTION_POLICY_VALIDATION_ERROR)
                    )
            );
        }

        if (
            List.of(
                ManagementContract.INTERMEDIARY_VERSION,
                VersionRetentionPolicyModel.TAG_VERSION_RETENTION_POLICY_INTERMEDIARY_VERSION
            ).contains(field)
        ) {
            if (EnumUtils.isValidEnum(IntermediaryVersionEnum.class, value.textValue())) {
                managementContractModeltoValidate
                    .getVersionRetentionPolicy()
                    .setIntermediaryVersion(IntermediaryVersionEnum.valueOf(value.textValue()));
                Optional<GenericRejectionCause> rejection = validationService
                    .checkVersionRetentionPolicy()
                    .validate(managementContractModeltoValidate, contractName);
                rejection.ifPresent(
                    genericRejectionCause ->
                        error.addToErrors(
                            getVitamError(
                                VitamCode.CONTRACT_VALIDATION_ERROR.getItem(),
                                genericRejectionCause.getReason()
                            ).setMessage(ManagementContractImpl.UPDATE_VERSION_RETENTION_POLICY_VALIDATION_ERROR)
                        )
                );
            } else {
                error.addToErrors(
                    getVitamError(
                        VitamCode.CONTRACT_VALIDATION_ERROR.getItem(),
                        String.format(INVALID_INTERMEDIARY_VERSION_TYPE, value.textValue())
                    ).setMessage(ManagementContractImpl.UPDATE_VERSION_RETENTION_POLICY_VALIDATION_ERROR)
                );
            }
        }

        if (
            List.of(
                ManagementContract.USAGES,
                VersionRetentionPolicyModel.TAG_VERSION_RETENTION_POLICY_USAGES
            ).contains(field)
        ) {
            try {
                Set<VersionUsageModel> usages = JsonHandler.getFromJsonNode(value, new TypeReference<>() {});
                managementContractModeltoValidate.getVersionRetentionPolicy().setUsages(usages);
            } catch (InvalidParseOperationException e) {
                LOGGER.error(e.getMessage());
                error.addToErrors(
                    getVitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem(), e.getMessage()).setMessage(
                        UPDATE_VERSION_RETENTION_POLICY_VALIDATION_ERROR
                    )
                );
            }
            Optional<GenericRejectionCause> rejection = validationService
                .checkVersionRetentionPolicy()
                .validate(managementContractModeltoValidate, contractName);
            rejection.ifPresent(
                genericRejectionCause ->
                    error.addToErrors(
                        getVitamError(
                            VitamCode.CONTRACT_VALIDATION_ERROR.getItem(),
                            genericRejectionCause.getReason()
                        ).setMessage(UPDATE_VERSION_RETENTION_POLICY_VALIDATION_ERROR)
                    )
            );
        }
    }

    /**
     * Contract validator
     */
    protected static final class ManagementContractValidationService {

        private final Map<ManagementContractValidator, String> validators;

        private final StorageClient storageClient;
        private final MongoDbAccessReferential mongoAccess;

        public ManagementContractValidationService(StorageClient storageClient, MongoDbAccessReferential mongoAccess) {
            this.storageClient = storageClient;
            this.mongoAccess = mongoAccess;
            // Init validator
            validators = new HashMap<>() {
                {
                    put(createMandatoryParamsValidator(), EMPTY_REQUIRED_FIELD);
                    put(createWrongFieldFormatValidator(), EMPTY_REQUIRED_FIELD);
                    put(createCheckDuplicateInDatabaseValidator(), DUPLICATE_IN_DATABASE);
                    put(checkStorageStrategies(), STRATEGY_VALIDATION_ERROR);
                    put(checkVersionRetentionPolicy(), VERSION_RETENTION_POLICY_VALIDATION_ERROR);
                }
            };
        }

        private boolean validateContract(
            ManagementContractModel contract,
            String jsonFormat,
            VitamError<ManagementContractModel> error
        ) {
            for (final ManagementContractValidator validator : validators.keySet()) {
                final Optional<GenericContractValidator.GenericRejectionCause> result = validator.validate(
                    contract,
                    jsonFormat
                );
                if (result.isPresent()) {
                    // there is a validation error on this contract
                    /* contract is valid, add it to the list to persist */
                    error.addToErrors(
                        getVitamError(
                            VitamCode.CONTRACT_VALIDATION_ERROR.getItem(),
                            result.get().getReason()
                        ).setMessage(validators.get(validator))
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
         * @return ManagementContractValidator
         */
        private ManagementContractValidator createMandatoryParamsValidator() {
            return (contract, jsonFormat) -> {
                GenericContractValidator.GenericRejectionCause rejection = null;
                if (contract.getName() == null || contract.getName().trim().isEmpty()) {
                    rejection = GenericContractValidator.GenericRejectionCause.rejectMandatoryMissing(
                        ManagementContract.NAME
                    );
                }

                return rejection == null ? Optional.empty() : Optional.of(rejection);
            };
        }

        /**
         * Set a default value if null
         *
         * @return ManagementContractValidator
         */
        private ManagementContractValidator createWrongFieldFormatValidator() {
            return (contract, inputList) -> {
                GenericContractValidator.GenericRejectionCause rejection = null;
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
                    LOGGER.error("Error management contract parse dates", e);
                    rejection = GenericContractValidator.GenericRejectionCause.rejectMandatoryMissing("CreationDate");
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
                    LOGGER.error("Error management contract parse dates", e);
                    rejection = GenericContractValidator.GenericRejectionCause.rejectMandatoryMissing("ActivationDate");
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
                    LOGGER.error("Error management contract parse dates", e);
                    rejection = GenericContractValidator.GenericRejectionCause.rejectMandatoryMissing(
                        "DeactivationDate"
                    );
                }

                contract.setLastupdate(now);

                return rejection == null ? Optional.empty() : Optional.of(rejection);
            };
        }

        /**
         * Check if the contract the same name already exists in database
         *
         * @return ManagementContractValidator
         */
        private ManagementContractValidator createCheckDuplicateInDatabaseValidator() {
            return (contract, contractName) -> {
                if (ParametersChecker.isNotEmpty(contract.getIdentifier())) {
                    try (
                        DbRequestResult result = ContractHelper.findByIdentifier(
                            contract.getIdentifier(),
                            FunctionalAdminCollections.MANAGEMENT_CONTRACT,
                            mongoAccess
                        )
                    ) {
                        final boolean exist = result.getCount() > 0;
                        if (exist) {
                            return Optional.of(
                                GenericContractValidator.GenericRejectionCause.rejectDuplicatedInDatabase(
                                    contract.getIdentifier()
                                )
                            );
                        }
                    } catch (ReferentialException | InvalidParseOperationException e) {
                        throw new VitamRuntimeException(e);
                    }
                }
                return Optional.empty();
            };
        }

        /**
         * Check if the Id of the contract is empty
         *
         * @return ManagementContractValidator
         */
        private ManagementContractValidator checkEmptyIdentifierSlaveModeValidator() {
            return (contract, contractIdentifier) -> {
                if (contractIdentifier == null || contractIdentifier.isEmpty()) {
                    return Optional.of(
                        GenericContractValidator.GenericRejectionCause.rejectMandatoryMissing(
                            ManagementContract.IDENTIFIER
                        )
                    );
                }
                return Optional.empty();
            };
        }

        /**
         * Check if the contract declares valid storage strategies.
         *
         * @return ManagementContractValidator
         */
        private ManagementContractValidator checkStorageStrategies() {
            return (contract, contractName) -> {
                try {
                    StorageDetailModel storage = contract.getStorage();
                    if (
                        storage == null ||
                        (storage.getObjectGroupStrategy() == null &&
                            storage.getUnitStrategy() == null &&
                            storage.getObjectStrategy() == null)
                    ) {
                        return Optional.empty();
                    }

                    RequestResponse<StorageStrategy> strategiesResponse = storageClient.getStorageStrategies();
                    if (!strategiesResponse.isOk()) {
                        LOGGER.error(strategiesResponse.toString());
                        throw new StorageException("Exception while retrieving storage strategies");
                    }
                    List<StorageStrategy> strategies =
                        ((RequestResponseOK<StorageStrategy>) strategiesResponse).getResults();

                    try {
                        if (storage.getObjectGroupStrategy() != null) {
                            StorageStrategyUtils.checkStrategy(
                                storage.getObjectGroupStrategy(),
                                strategies,
                                ManagementContract.OBJECTGROUP_STRATEGY,
                                true
                            );
                        }
                        if (storage.getUnitStrategy() != null) {
                            StorageStrategyUtils.checkStrategy(
                                storage.getUnitStrategy(),
                                strategies,
                                ManagementContract.UNIT_STRATEGY,
                                true
                            );
                        }

                        if (storage.getObjectStrategy() != null) {
                            StorageStrategyUtils.checkStrategy(
                                storage.getObjectStrategy(),
                                strategies,
                                ManagementContract.OBJECT_STRATEGY,
                                false
                            );
                        }
                    } catch (StorageStrategyNotFoundException storageStrategyNotFoundException) {
                        return Optional.of(
                            GenericContractValidator.GenericRejectionCause.rejectStorageStrategyMissing(
                                storageStrategyNotFoundException.getStrategyId(),
                                storageStrategyNotFoundException.getVariableName()
                            )
                        );
                    } catch (StorageStrategyReferentOfferException storageStrategyReferentOfferException) {
                        return Optional.of(
                            GenericContractValidator.GenericRejectionCause.rejectStorageStrategyDoesNotContainsOneReferent(
                                storageStrategyReferentOfferException.getStrategyId(),
                                storageStrategyReferentOfferException.getVariableName()
                            )
                        );
                    }

                    return Optional.empty();
                } catch (Exception e) {
                    return Optional.of(
                        GenericRejectionCause.rejectExceptionOccurred(contract.getName(), "Error checking storage", e)
                    );
                }
            };
        }

        private ManagementContractValidator checkVersionRetentionPolicy() {
            return (contract, contractName) -> {
                try {
                    VersionRetentionPolicyModel versionRetentionPolicy = contract.getVersionRetentionPolicy();

                    if (
                        versionRetentionPolicy == null ||
                        (!versionRetentionPolicy.getInitialVersion() &&
                            versionRetentionPolicy.getIntermediaryVersion() == null)
                    ) {
                        return Optional.of(
                            GenericContractValidator.GenericRejectionCause.rejectInvalidVersionRetentionPolicyParamOfUsage(
                                INITIAL_VERSION,
                                ManagementContract.VERSION_RETENTION_POLICY,
                                contractName
                            )
                        );
                    }

                    // Check default policy
                    if (!versionRetentionPolicy.getInitialVersion()) {
                        return Optional.of(
                            GenericContractValidator.GenericRejectionCause.rejectInvalidVersionRetentionPolicyParamOfUsage(
                                INITIAL_VERSION,
                                DEFAULT_USAGE,
                                contractName
                            )
                        );
                    }
                    if (
                        versionRetentionPolicy.getIntermediaryVersion() == null ||
                        !List.of(LAST, ALL).contains(versionRetentionPolicy.getIntermediaryVersion())
                    ) {
                        return Optional.of(
                            GenericContractValidator.GenericRejectionCause.rejectInvalidVersionRetentionPolicyParamOfUsage(
                                INTERMEDIARY_VERSION,
                                DEFAULT_USAGE,
                                contractName
                            )
                        );
                    }

                    // Check Usage
                    Set<VersionUsageModel> versionsUsages = versionRetentionPolicy.getUsages();
                    if (versionsUsages != null && !versionsUsages.isEmpty()) {
                        for (VersionUsageModel versionUsage : versionsUsages) {
                            if (
                                Arrays.stream(DataObjectVersionType.values()).noneMatch(
                                    elmt -> elmt.getName().equals(versionUsage.getUsageName())
                                )
                            ) {
                                return Optional.of(
                                    GenericContractValidator.GenericRejectionCause.rejectInvalidVersionRetentionPolicyUsage(
                                        versionUsage.getUsageName(),
                                        contractName
                                    )
                                );
                            }

                            if (versionUsage.getUsageName().equals(BINARY_MASTER.getName())) {
                                if (!versionUsage.getInitialVersion()) {
                                    return Optional.of(
                                        GenericContractValidator.GenericRejectionCause.rejectInvalidVersionRetentionPolicyParamOfUsage(
                                            INITIAL_VERSION,
                                            BINARY_MASTER.getName(),
                                            contractName
                                        )
                                    );
                                }
                                if (
                                    versionUsage.getIntermediaryVersion() == null ||
                                    !List.of(LAST, ALL).contains(versionUsage.getIntermediaryVersion())
                                ) {
                                    return Optional.of(
                                        GenericContractValidator.GenericRejectionCause.rejectInvalidVersionRetentionPolicyParamOfUsage(
                                            INTERMEDIARY_VERSION,
                                            BINARY_MASTER.getName(),
                                            contractName
                                        )
                                    );
                                }
                            } else {
                                if (
                                    versionUsage.getIntermediaryVersion() == null ||
                                    !EnumUtils.isValidEnum(
                                        VersionUsageModel.IntermediaryVersionEnum.class,
                                        versionUsage.getIntermediaryVersion().toString()
                                    )
                                ) {
                                    return Optional.of(
                                        GenericContractValidator.GenericRejectionCause.rejectInvalidVersionRetentionPolicyParamOfUsage(
                                            INTERMEDIARY_VERSION,
                                            versionUsage.getUsageName(),
                                            contractName
                                        )
                                    );
                                }
                            }
                        }
                    }

                    return Optional.empty();
                } catch (Exception e) {
                    return Optional.of(
                        GenericRejectionCause.rejectExceptionOccurred(
                            contract.getName(),
                            "Error checking version retention policy",
                            e
                        )
                    );
                }
            };
        }
    }
}
