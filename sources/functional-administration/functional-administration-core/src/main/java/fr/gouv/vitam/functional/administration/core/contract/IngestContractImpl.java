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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.GLOBAL;
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
import fr.gouv.vitam.common.model.administration.ActivationStatus;
import fr.gouv.vitam.common.model.administration.IngestContractCheckState;
import fr.gouv.vitam.common.model.administration.IngestContractModel;
import fr.gouv.vitam.common.model.administration.ManagementContractModel;
import fr.gouv.vitam.common.model.administration.SignaturePolicy;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.FileFormat;
import fr.gouv.vitam.functional.administration.common.IngestContract;
import fr.gouv.vitam.functional.administration.common.Profile;
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
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.in;
import static fr.gouv.vitam.common.json.JsonHandler.getFromStringAsTypeReference;

/**
 * IngestContract implementation class
 */
public class IngestContractImpl implements ContractService<IngestContractModel> {

    public static final String CONTRACT_BACKUP_EVENT = "STP_BACKUP_INGEST_CONTRACT";
    private static final String THE_INGEST_CONTRACT_STATUS_MUST_BE_ACTIVE_OR_INACTIVE_BUT_NOT =
        "The Ingest contract status must be ACTIVE or INACTIVE but not ";
    private static final String INGEST_CONTRACT_CHECK_PARENT_LINK_STATUS_NOT_IN_ENUM =
        "the ingest contract check parent link status in not in enum";
    private static final String INGEST_CONTRACT_NOT_FOUND = "Ingest contract not found";
    private static final String CONTRACT_IS_MANDATORY_PATAMETER = "The collection of ingest contracts is mandatory";
    private static final String EVERYFORMAT_LIST_EMPTY = "formatType field must not be empty when everyFormat is false";
    private static final String EVERYFORMAT_LIST_NOT_EMPTY = "formatType field must be empty when everyFormat is true";
    private static final String DATE_MUST_BE_VALID = "must be a valid date";
    private static final String CONTRACTS_IMPORT_EVENT = "STP_IMPORT_INGEST_CONTRACT";
    private static final String CONTRACT_UPDATE_EVENT = "STP_UPDATE_INGEST_CONTRACT";
    private static final String EMPTY_REQUIRED_FIELD =
        CONTRACTS_IMPORT_EVENT + ContractLogbookService.EMPTY_REQUIRED_FIELD;
    private static final String DUPLICATE_IN_DATABASE =
        CONTRACTS_IMPORT_EVENT + ContractLogbookService.DUPLICATE_IN_DATABASE;
    private static final String PROFILE_NOT_FOUND_IN_DATABASE =
        CONTRACTS_IMPORT_EVENT + ContractLogbookService.PROFILE_NOT_FOUND_IN_DATABASE;
    private static final String FORMAT_NOT_FOUND = CONTRACTS_IMPORT_EVENT + ContractLogbookService.FORMAT_NOT_FOUND;
    private static final String FORMAT_MUST_BE_EMPTY =
        CONTRACTS_IMPORT_EVENT + ContractLogbookService.FORMAT_MUST_BE_EMPTY;
    private static final String FORMAT_MUST_NOT_BE_EMPTY =
        CONTRACTS_IMPORT_EVENT + ContractLogbookService.FORMAT_MUST_NOT_BE_EMPTY;
    private static final String MANAGEMENTCONTRACT_NOT_FOUND =
        CONTRACTS_IMPORT_EVENT + ContractLogbookService.MANAGEMENTCONTRACT_NOT_FOUND;
    private static final String CONTRACT_BAD_REQUEST =
        CONTRACTS_IMPORT_EVENT + ContractLogbookService.CONTRACT_BAD_REQUEST;

    private static final String UPDATE_CONTRACT_NOT_FOUND =
        CONTRACT_UPDATE_EVENT + ContractLogbookService.UPDATE_CONTRACT_NOT_FOUND;
    private static final String UPDATE_CONTRACT_BAD_REQUEST =
        CONTRACT_UPDATE_EVENT + ContractLogbookService.CONTRACT_BAD_REQUEST;
    private static final String UPDATE_VALUE_NOT_IN_ENUM =
        CONTRACT_UPDATE_EVENT + ContractLogbookService.UPDATE_VALUE_NOT_IN_ENUM;
    private static final String UPDATE_PROFILE_NOT_FOUND =
        CONTRACT_UPDATE_EVENT + ContractLogbookService.PROFILE_NOT_FOUND_IN_DATABASE;
    private static final String UPDATE_WRONG_FILEFORMAT =
        CONTRACT_UPDATE_EVENT + ContractLogbookService.UPDATE_WRONG_FILEFORMAT;
    private static final String UPDATE_MANAGEMENTCONTRACT_NOT_FOUND =
        CONTRACT_UPDATE_EVENT + ContractLogbookService.MANAGEMENTCONTRACT_NOT_FOUND;
    private static final String UPDATE_KO = CONTRACT_UPDATE_EVENT + ".KO";


    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IngestContractImpl.class);
    private static final String UND_TENANT = "_tenant";
    private static final String UND_ID = "_id";
    private static final String RESULT_HITS = "$hits";
    private static final String HITS_SIZE = "size";
    private static final String CONTRACT_KEY = "IngestContract";
    private static final String CONTRACT_CHECK_KEY = "ingestContractCheck";
    private final MongoDbAccessAdminImpl mongoAccess;
    private final LogbookOperationsClient logbookClient;
    private final VitamCounterService vitamCounterService;
    private final MetaDataClient metaDataClient;
    private final FunctionalBackupService functionalBackupService;
    private final ContractService<ManagementContractModel> managementContractService;



    /**
     * Constructor
     *
     * @param mongoAccess the mongo access service
     * @param vitamCounterService the vitam counter service
     */
    public IngestContractImpl(MongoDbAccessAdminImpl mongoAccess, VitamCounterService vitamCounterService) {
        this(mongoAccess, vitamCounterService, MetaDataClientFactory.getInstance().getClient(),
            LogbookOperationsClientFactory.getInstance().getClient(),
            new FunctionalBackupService(vitamCounterService),
            new ManagementContractImpl(mongoAccess, vitamCounterService));
    }

    /**
     * Constructor
     *
     * @param mongoAccess the mongo access service
     * @param vitamCounterService the vitam counter service
     * @param metaDataClient the metadata client
     * @param logbookClient the logbook client
     * @param functionalBackupService
     * @param managementContractService
     */
    @VisibleForTesting
    public IngestContractImpl(MongoDbAccessAdminImpl mongoAccess, VitamCounterService vitamCounterService,
        MetaDataClient metaDataClient,
        LogbookOperationsClient logbookClient,
        FunctionalBackupService functionalBackupService,
        ContractService<ManagementContractModel> managementContractService) {
        this.mongoAccess = mongoAccess;
        this.vitamCounterService = vitamCounterService;
        this.metaDataClient = metaDataClient;
        this.functionalBackupService = functionalBackupService;
        this.logbookClient = logbookClient;
        this.managementContractService = managementContractService;
    }

    private static VitamError<IngestContractModel> getVitamError(String vitamCode, String error) {
        return VitamErrorUtils.getVitamError(vitamCode, error, CONTRACT_KEY, StatusCode.KO, IngestContractModel.class);
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


        IngestContractValidationService validationService =
            new IngestContractValidationService(metaDataClient, managementContractService);
        ContractLogbookService logbookService = new ContractLogbookService(logbookClient, eip, CONTRACTS_IMPORT_EVENT,
            CONTRACT_UPDATE_EVENT, CONTRACT_KEY, CONTRACT_CHECK_KEY);

        logbookService.logStarted();

        ArrayNode contractsToPersist;

        final VitamError<IngestContractModel> error =
            getVitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem(), "Ingest contract import error")
                .setHttpCode(Response.Status.BAD_REQUEST
                    .getStatusCode());

        try {

            for (final IngestContractModel acm : contractModelList) {

                final String linkParentId = acm.getLinkParentId();
                if (linkParentId != null && !validationService.checkIfUnitExist(linkParentId)) {
                    error.addToErrors(getVitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem(),
                        GenericRejectionCause
                            .rejectAuNotFoundInDatabase(linkParentId)
                            .getReason()));
                    continue;
                }

                final String managementContractId = acm.getManagementContractId();
                if (managementContractId != null &&
                    !validationService.checkIfManagementContractExists(managementContractId)) {
                    error.addToErrors(getVitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem(),
                        GenericRejectionCause
                            .rejectMCNotFoundInDatabase(managementContractId)
                            .getReason()).setMessage(MANAGEMENTCONTRACT_NOT_FOUND));
                    continue;
                }

                final SignaturePolicy signaturePolicy = acm.getSignaturePolicy();
                if (signaturePolicy != null) {
                    if (validationService.isInvalidSignaturePolicy(signaturePolicy)) {
                        error.addToErrors(getVitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem(),
                            GenericRejectionCause
                                .rejectInconsistentContract(acm.getName(),
                                    "The fields needSignature, needTimestamp, or needAdditionalProof are not authorized due to the signature policy")
                                .getReason()).setMessage(UPDATE_CONTRACT_BAD_REQUEST));
                        continue;
                    }
                    validationService.validSignatureObject(signaturePolicy);
                }

                final Set<String> checkParentId = acm.getCheckParentId();
                if (checkParentId != null) {
                    if (!checkParentId.isEmpty()
                        && IngestContractCheckState.UNAUTHORIZED.equals(acm.getCheckParentLink())) {
                        error.addToErrors(getVitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem(),
                            GenericRejectionCause
                                .rejectInconsistentContract(acm.getName(),
                                    "attachments not authorized but checkParentId field is not empty")
                                .getReason()));
                        continue;
                    }
                    if (!validationService.checkIfAllUnitExist(checkParentId)) {
                        error.addToErrors(getVitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem(),
                            GenericRejectionCause
                                .rejectAuNotFoundInDatabase(String.join(" ", checkParentId))
                                .getReason()));
                        continue;
                    }
                }

                // if a contract have and id
                if (null != acm.getId()) {
                    error.addToErrors(getVitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem(),
                        GenericRejectionCause.rejectIdNotAllowedInCreate(acm.getName())
                            .getReason()));
                    continue;
                }

                //when everyformattype is false, formattype must not be empty
                if (!acm.isEveryFormatType() && (acm.getFormatType() == null || acm.getFormatType().isEmpty())) {
                    error.addToErrors(getVitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem(),
                        EVERYFORMAT_LIST_EMPTY).setMessage(FORMAT_MUST_NOT_BE_EMPTY));
                    continue;
                }

                //when everyformattype is true, formattype must  be empty
                if (acm.isEveryFormatType() && acm.getFormatType() != null) {
                    error.addToErrors(getVitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem(),
                        EVERYFORMAT_LIST_NOT_EMPTY).setMessage(FORMAT_MUST_BE_EMPTY));
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
                    final Optional<GenericRejectionCause> result =
                        validationService.checkEmptyIdentifierSlaveModeValidator().validate(acm, acm.getIdentifier());
                    result.ifPresent(t -> error
                        .addToErrors(getVitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem(),
                            result.get().getReason()).setMessage(EMPTY_REQUIRED_FIELD)
                        )
                    );
                }

            }

            if (null != error.getErrors() && !error.getErrors().isEmpty()) {
                // log book + application log
                // stop
                final String errorsDetails =
                    error.getErrors().stream().map(VitamError::getDescription).distinct()
                        .collect(Collectors.joining(","));

                logbookService
                    .logValidationError(errorsDetails, CONTRACTS_IMPORT_EVENT, error.getErrors().get(0).getMessage());
                return error;
            }
            contractsToPersist = JsonHandler.createArrayNode();
            for (final IngestContractModel acm : contractModelList) {
                ContractHelper.setIdentifier(slaveMode, acm, vitamCounterService,
                    SequenceType.INGEST_CONTRACT_SEQUENCE);
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
            // TODO: 3/28/17 create insertDocuments method that accepts VitamDocument instead of ArrayNode, so we can
            // use IngestContract at this point
            mongoAccess.insertDocuments(contractsToPersist, FunctionalAdminCollections.INGEST_CONTRACT).close();

            functionalBackupService.saveCollectionAndSequence(
                eip,
                CONTRACT_BACKUP_EVENT,
                FunctionalAdminCollections.INGEST_CONTRACT,
                eip.toString()
            );

            logbookService.logSuccess();

            return new RequestResponseOK<IngestContractModel>().addAllResults(contractModelList)
                .setHttpCode(Response.Status.CREATED.getStatusCode());

        } catch (SchemaValidationException | BadRequestException exp) {
            LOGGER.error(exp);
            final String err = "Import ingest contracts error > " + exp.getMessage();
            logbookService.logValidationError(err, CONTRACTS_IMPORT_EVENT, CONTRACT_BAD_REQUEST);
            return getVitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem(), exp.getMessage()
            ).setDescription(err).setHttpCode(Response.Status.BAD_REQUEST.getStatusCode());
        } catch (final Exception exp) {
            LOGGER.error(exp);
            final String err = "Import ingest contracts error > " + exp.getMessage();
            logbookService.logFatalError(err, CONTRACTS_IMPORT_EVENT);
            return error.setCode(VitamCode.GLOBAL_INTERNAL_SERVER_ERROR.getItem()).setDescription(err).setHttpCode(
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        }
    }

    @Override
    public IngestContractModel findByIdentifier(String identifier)
        throws ReferentialException, InvalidParseOperationException {
        SanityChecker.checkParameter(identifier);
        try (DbRequestResult result = ContractHelper.findByIdentifier(identifier,
            FunctionalAdminCollections.INGEST_CONTRACT, mongoAccess)) {
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

    @Override
    public void close() {
        logbookClient.close();
    }

    @Override
    public RequestResponse<IngestContractModel> updateContract(String identifier, JsonNode queryDsl)
        throws VitamException {
        VitamError<IngestContractModel> error =
            getVitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem(), "Ingest contract update error")
                .setHttpCode(Response.Status.BAD_REQUEST
                    .getStatusCode());

        if (queryDsl == null || !queryDsl.isObject()) {
            return error;
        }

        final IngestContractModel ingestContractModel = findByIdentifier(identifier);
        if (ingestContractModel == null) {
            error.setHttpCode(Response.Status.NOT_FOUND.getStatusCode());
            return error.addToErrors(
                getVitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem(), INGEST_CONTRACT_NOT_FOUND + identifier
                ).setMessage(UPDATE_CONTRACT_NOT_FOUND));
        }



        String operationId = VitamThreadUtils.getVitamSession().getRequestId();
        GUID eip = GUIDReader.getGUID(operationId);
        RequestResponseOK<IngestContractModel> response = new RequestResponseOK<>();

        IngestContractValidationService validationService =
            new IngestContractValidationService(metaDataClient, managementContractService);
        ContractLogbookService logbookService = new ContractLogbookService(logbookClient, eip, CONTRACTS_IMPORT_EVENT,
            CONTRACT_UPDATE_EVENT, CONTRACT_KEY, CONTRACT_CHECK_KEY);

        logbookService.logUpdateStarted(ingestContractModel.getId());

        final JsonNode actionNode = queryDsl.get(GLOBAL.ACTION.exactToken());
        for (final JsonNode fieldToSet : actionNode) {
            final JsonNode fieldName = fieldToSet.get(UPDATEACTION.SET.exactToken());
            if (fieldName != null) {
                final Iterator<String> it = fieldName.fieldNames();
                while (it.hasNext()) {
                    final String field = it.next();
                    final JsonNode value = fieldName.findValue(field);
                    validateUpdateAction(validationService, ingestContractModel.getName(), error, field, value,
                        ingestContractModel);
                }

                ((ObjectNode) fieldName).remove(AbstractContractModel.TAG_CREATION_DATE);
                ((ObjectNode) fieldName).put(AbstractContractModel.TAG_LAST_UPDATE,
                    LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()));
            }
        }

        Map<String, List<String>> updateDiffs;

        try {
            JsonNode linkParentNode = queryDsl.findValue(IngestContractModel.LINK_PARENT_ID);
            if (linkParentNode != null) {
                final String linkParentId = linkParentNode.asText();
                if (!linkParentId.equals("")) {
                    if (!validationService.checkIfUnitExist(linkParentId)) {
                        error.addToErrors(getVitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem(),
                            GenericRejectionCause.rejectAuNotFoundInDatabase(linkParentId).getReason())
                            .setMessage(UPDATE_KO));
                    }
                }
            }

            JsonNode managementContractNode = queryDsl.findValue(IngestContractModel.TAG_MANAGEMENT_CONTRACT_ID);
            if (managementContractNode != null) {
                final String managementContractId = managementContractNode.asText();
                if (!validationService.checkIfManagementContractExists(managementContractId)) {
                    error.addToErrors(getVitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem(),
                        GenericRejectionCause.rejectMCNotFoundInDatabase(managementContractId).getReason()
                    ).setMessage(UPDATE_MANAGEMENTCONTRACT_NOT_FOUND));
                }
            }

            boolean isAttachmentAuthorized = true;
            JsonNode checkParentLink = queryDsl.findValue(IngestContractModel.TAG_CHECK_PARENT_LINK);
            IngestContractCheckState checkState = ingestContractModel.getCheckParentLink();

            if (checkParentLink != null) {
                if (IngestContractCheckState.contains(checkParentLink.asText())) {
                    checkState = IngestContractCheckState.valueOf(checkParentLink.asText());
                } else {
                    error.addToErrors(getVitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem(),
                        INGEST_CONTRACT_CHECK_PARENT_LINK_STATUS_NOT_IN_ENUM + checkParentLink.asText()
                    ).setMessage(UPDATE_VALUE_NOT_IN_ENUM));
                }
            }

            if (IngestContractCheckState.UNAUTHORIZED.equals(checkState)) {
                isAttachmentAuthorized = false;
            }

            JsonNode checkParentIdsNode = queryDsl.findValue(IngestContractModel.TAG_CHECK_PARENT_ID);
            Set<String> checkParentIds = new HashSet<>();
            if (checkParentIdsNode != null) {
                if (checkParentIdsNode.isArray()) {

                    for (JsonNode checkParentId : checkParentIdsNode) {
                        checkParentIds.add(checkParentId.asText());
                    }

                    if (!checkParentIds.isEmpty()) {
                        if (!validationService.checkIfAllUnitExist(checkParentIds)) {
                            error.addToErrors(getVitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem(),
                                GenericRejectionCause.rejectAuNotFoundInDatabase(String.join(" ", checkParentIds))
                                    .getReason())
                                .setMessage(UPDATE_KO));
                        }
                    }
                }
            } else if (ingestContractModel.getCheckParentId() != null) {
                checkParentIds.addAll(ingestContractModel.getCheckParentId());
            }


            if (!isAttachmentAuthorized && !checkParentIds.isEmpty()) {
                error.addToErrors(getVitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem(),
                    GenericRejectionCause
                        .rejectInconsistentContract(ingestContractModel.getName(),
                            "attachments not authorized but checkParentId field is not empty")
                        .getReason()));
            }

            final JsonNode archiveProfilesNode = queryDsl.findValue(IngestContractModel.ARCHIVE_PROFILES);
            if (archiveProfilesNode != null) {
                final Set<String> archiveProfiles =
                    getFromStringAsTypeReference(archiveProfilesNode.toString(), new TypeReference<>() {
                    });
                final IngestContractValidator validator =
                    validationService.createCheckProfilesExistsInDatabaseValidator();
                final Optional<GenericRejectionCause> result =
                    validator.validate(new IngestContractModel().setArchiveProfiles(archiveProfiles),
                        identifier);
                // there is a validation error on this contract
                /* contract is valid, add it to the list to persist */
                result.ifPresent(genericRejectionCause -> error.addToErrors(
                    getVitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem(), genericRejectionCause.getReason()
                    ).setMessage(UPDATE_PROFILE_NOT_FOUND)));
            }
            final JsonNode fileFormatTypeNode = queryDsl.findValue(IngestContractModel.FORMAT_TYPE);
            Set<String> fileFormatTypes =
                getFromJsonNodeOrFromIngestContractModel(ingestContractModel, fileFormatTypeNode);
            checkFormatsNotEmptyWhenEveryFormatTypeIsFalse(queryDsl, fileFormatTypes, fileFormatTypeNode, error);

            if (fileFormatTypes != null) {

                final IngestContractValidator validator =
                    validationService.createCheckFormatFileExistsInDatabaseValidator();
                final Optional<GenericRejectionCause> result =
                    validator.validate(new IngestContractModel().setFormatType(fileFormatTypes),
                        identifier);
                // there is a validation error on this contract
                /* contract is valid, add it to the list to persist */
                result.ifPresent(genericRejectionCause -> error.addToErrors(
                    getVitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem(), genericRejectionCause.getReason()
                    ).setMessage(UPDATE_WRONG_FILEFORMAT)));
            }

            final JsonNode signaturePolicyNode = queryDsl.findValue(IngestContractModel.TAG_SIGNATURE_POLICY);
            if (signaturePolicyNode != null) {
                ObjectMapper objectMapper = new ObjectMapper();
                SignaturePolicy signaturePolicy = JsonHandler.getFromJsonNode(signaturePolicyNode, SignaturePolicy.class);
                if (validationService.isInvalidSignaturePolicy(signaturePolicy)) {
                    error.addToErrors(getVitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem(),
                        GenericRejectionCause
                            .rejectInconsistentContract(ingestContractModel.getName(),
                                "The fields needSignature, needTimestamp, or needAdditionalProof are not authorized due to the signature policy")
                            .getReason()).setMessage(UPDATE_CONTRACT_BAD_REQUEST));
                }
                validationService.validSignatureObject(signaturePolicy);
            }


            if (error.getErrors() != null && !error.getErrors().isEmpty()) {
                final String errorsDetails =
                    error.getErrors().stream().map(VitamError::getDescription).collect(Collectors.joining(","));
                logbookService.logValidationError(errorsDetails, CONTRACT_UPDATE_EVENT,
                    error.getErrors().get(0).getMessage());

                return error;
            }

            DbRequestResult result = mongoAccess.updateData(queryDsl, FunctionalAdminCollections.INGEST_CONTRACT);
            updateDiffs = result.getDiffs();
            response.addAllResults(result.getDocuments(IngestContract.class, IngestContractModel.class))
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

            logbookService.logUpdateSuccess(ingestContractModel.getId(), identifier,
                updateDiffs.get(ingestContractModel.getId()));
            return response;

        } catch (SchemaValidationException | BadRequestException exp) {
            LOGGER.error(exp);
            final String err = "Update ingest contract error > " + exp.getMessage();
            logbookService.logValidationError(err, CONTRACT_UPDATE_EVENT, UPDATE_CONTRACT_BAD_REQUEST);
            return getVitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem(), exp.getMessage()
            ).setDescription(err).setHttpCode(Response.Status.BAD_REQUEST.getStatusCode());
        } catch (Exception e) {
            LOGGER.error(e);
            final String err = "Update ingest contract error > " + e.getMessage();
            logbookService.logFatalError(err, CONTRACT_UPDATE_EVENT);
            error.setCode(VitamCode.GLOBAL_INTERNAL_SERVER_ERROR.getItem())
                .setDescription(err)
                .setHttpCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());

            return error;
        }
    }

    private void validateUpdateAction(IngestContractValidationService validationService, String name,
        VitamError<IngestContractModel> error, String field, JsonNode value, IngestContractModel ingestContractModel) {
        switch (field) {
            case IngestContract.STATUS:
                if (!(ActivationStatus.ACTIVE.name().equals(value.asText()) || ActivationStatus.INACTIVE
                    .name().equals(value.asText()))) {
                    error.addToErrors(getVitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem(),
                        THE_INGEST_CONTRACT_STATUS_MUST_BE_ACTIVE_OR_INACTIVE_BUT_NOT + value.asText()
                    ).setMessage(UPDATE_VALUE_NOT_IN_ENUM));
                }
                break;
            case IngestContract.LAST_UPDATE:
            case IngestContract.CREATIONDATE:
            case IngestContract.ACTIVATIONDATE:
            case IngestContract.DEACTIVATIONDATE:
                try {
                    LocalDateUtil.getFormattedDateForMongo(value.asText());
                } catch (DateTimeParseException e) {
                    error.addToErrors(getVitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem(),
                        String.format("%s %s", field, DATE_MUST_BE_VALID))
                        .setMessage(UPDATE_CONTRACT_BAD_REQUEST));
                }
                break;
        }

    }

    private Set<String> getFromJsonNodeOrFromIngestContractModel(IngestContractModel ingestContractModel,
        JsonNode fileFormatTypeNode) throws InvalidParseOperationException {

        if (fileFormatTypeNode != null && fileFormatTypeNode.isArray()) {
            return JsonHandler.getFromString(fileFormatTypeNode.toString(), Set.class, String.class);
        } else if (ingestContractModel.getFormatType() != null) {
            return ingestContractModel.getFormatType();
        }

        return null;
    }

    private void checkFormatsNotEmptyWhenEveryFormatTypeIsFalse(JsonNode queryDsl, Set<String> fileFormatTypes,
        JsonNode fileFormatTypeNode, VitamError<IngestContractModel> error) {
        final JsonNode everyFileformatFlagNode = queryDsl.findValue(IngestContractModel.EVERY_FORMAT_TYPE);

        if (everyFileformatFlagNode != null || fileFormatTypeNode != null) {
            final boolean everyFileFormat = everyFileformatFlagNode != null && everyFileformatFlagNode.asBoolean();
            boolean formatsEmpty = fileFormatTypes == null || fileFormatTypes.isEmpty();
            final boolean canBeUpdate = !formatsEmpty || everyFileFormat;
            if (!canBeUpdate) {
                error.addToErrors(
                    getVitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem(), EVERYFORMAT_LIST_EMPTY
                    ).setMessage(UPDATE_CONTRACT_BAD_REQUEST));
            }
        }
    }


    /**
     * Contract validator
     */
    protected final static class IngestContractValidationService {

        private final Map<IngestContractValidator, String> validators;

        private final MetaDataClient metaDataClient;

        private final ContractService<ManagementContractModel> managementContractService;


        public IngestContractValidationService(MetaDataClient metaDataClient,
            ContractService<ManagementContractModel> managementContractService) {
            this.metaDataClient = metaDataClient;
            this.managementContractService = managementContractService;
            // Init validator
            validators = new HashMap<>() {{
                put(createMandatoryParamsValidator(), EMPTY_REQUIRED_FIELD);
                put(createWrongFieldFormatValidator(), EMPTY_REQUIRED_FIELD);
                put(createCheckDuplicateInDatabaseValidator(), DUPLICATE_IN_DATABASE);
                put(createCheckProfilesExistsInDatabaseValidator(), PROFILE_NOT_FOUND_IN_DATABASE);
                put(createCheckFormatFileExistsInDatabaseValidator(), FORMAT_NOT_FOUND);
            }};
        }

        private boolean validateContract(IngestContractModel contract, String jsonFormat,
            VitamError<IngestContractModel> error) {

            for (final IngestContractValidator validator : validators.keySet()) {
                final Optional<GenericRejectionCause> result =
                    validator.validate(contract, jsonFormat);
                if (result.isPresent()) {
                    // there is a validation error on this contract
                    /* contract is valid, add it to the list to persist */
                    error.addToErrors(getVitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem(), result
                        .get().getReason()).setMessage(validators.get(validator)));
                    // once a validation error is detected on a contract, jump to next contract
                    return false;
                }
            }
            return true;
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
         * Validate that contract have not an invalid object Signature
         *
         * @return boolean
         */
        protected boolean isInvalidSignaturePolicy(SignaturePolicy signaturePolicy) {
            return signaturePolicy.getSignedDocument() == null ||
                (signaturePolicy.getSignedDocument() == SignaturePolicy.SignedDocumentPolicyEnum.FORBIDDEN &&
                (signaturePolicy.isNeedSignature() != null || signaturePolicy.isNeedTimestamp() != null ||
                    signaturePolicy.isNeedAdditionalProof() != null));
        }

        protected void validSignatureObject(SignaturePolicy signaturePolicy) {
            if (!signaturePolicy.getSignedDocument().equals(SignaturePolicy.SignedDocumentPolicyEnum.FORBIDDEN)) {
                signaturePolicy.setNeedSignature(
                    signaturePolicy.isNeedSignature() != null ? signaturePolicy.isNeedSignature() : false);
                signaturePolicy.setNeedAdditionalProof(
                    signaturePolicy.isNeedAdditionalProof() != null ? signaturePolicy.isNeedAdditionalProof() : false);
                signaturePolicy.setNeedTimestamp(
                    signaturePolicy.isNeedTimestamp() != null ? signaturePolicy.isNeedTimestamp() : false);
            }
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
                    contract.setCheckParentLink(IngestContractCheckState.AUTHORIZED);
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
                    final boolean exist =
                        FunctionalAdminCollections.INGEST_CONTRACT.getCollection().countDocuments(clause) > 0;
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
                final long count = FunctionalAdminCollections.PROFILE.getCollection().countDocuments(clause);
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

                final Bson clause =
                    in(FileFormat.PUID, contract.getFormatType());

                final long count = FunctionalAdminCollections.FORMATS.getCollection().countDocuments(clause);

                if (count != contract.getFormatType().size()) {
                    rejection =
                        GenericRejectionCause
                            .rejectFormatFileTypeNotFoundInDatabase(contractName);
                }
                return rejection == null ? Optional.empty() : Optional.of(rejection);
            };
        }

        private boolean checkIfUnitExist(String unitId)
            throws MetaDataExecutionException, MetaDataDocumentSizeException, MetaDataClientServerException,
            InvalidParseOperationException {
            final Select select = new Select();
            JsonNode jsonNode = metaDataClient.selectUnitbyId(select.getFinalSelect(), unitId);
            return (jsonNode != null && jsonNode.get(RESULT_HITS) != null
                && jsonNode.get(RESULT_HITS).get(HITS_SIZE).asInt() > 0);
        }

        private boolean checkIfManagementContractExists(String managementContractId)
            throws ReferentialException, InvalidParseOperationException {
            ManagementContractModel mc = managementContractService.findByIdentifier(managementContractId);
            return mc != null;
        }

        private boolean checkIfAllUnitExist(Set<String> unitIds)
            throws MetaDataExecutionException, MetaDataDocumentSizeException, MetaDataClientServerException,
            InvalidParseOperationException, InvalidCreateOperationException {
            final Select select = new Select();
            select.setQuery(QueryHelper.in(VitamFieldsHelper.id(), unitIds.toArray(new String[0])));
            JsonNode jsonNode = metaDataClient.selectUnits(select.getFinalSelect());
            return (jsonNode != null && jsonNode.get(RESULT_HITS) != null
                && jsonNode.get(RESULT_HITS).get(HITS_SIZE).asInt() > 0);
        }

    }
}
