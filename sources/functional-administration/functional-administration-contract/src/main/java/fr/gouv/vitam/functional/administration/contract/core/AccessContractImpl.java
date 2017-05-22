/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
package fr.gouv.vitam.functional.administration.contract.core;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import fr.gouv.vitam.common.security.SanityChecker;

import org.apache.commons.lang3.BooleanUtils;
import org.bson.conversions.Bson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.client.MongoCursor;

import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.GLOBAL;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.UPDATEACTION;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.parser.request.adapter.VarNameAdapter;
import fr.gouv.vitam.common.database.parser.request.single.SelectParserSingle;
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
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.functional.administration.client.model.AccessContractModel;
import fr.gouv.vitam.functional.administration.common.AccessContract;
import fr.gouv.vitam.functional.administration.common.ContractStatus;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.functional.administration.contract.api.ContractService;
import fr.gouv.vitam.functional.administration.contract.core.GenericContractValidator.GenericRejectionCause;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationsClientHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;

import javax.ws.rs.core.Response;

public class AccessContractImpl implements ContractService<AccessContractModel> {

    private static final String ACCESS_CONTRACT_IS_MANDATORY_PATAMETER =
        "The collection of access contracts is mandatory";
    private static final String UPDATE_ACCESS_CONTRACT_MANDATORY_PATAMETER = "access contracts is mandatory";
    private static final String CONTRACTS_IMPORT_EVENT = "STP_IMPORT_ACCESS_CONTRACT";
    private static final String CONTRACT_UPDATE_EVENT = "STP_UPDATE_ACCESS_CONTRACT";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AccessContractImpl.class);
    private final MongoDbAccessAdminImpl mongoAccess;
    private LogbookOperationsClient logBookclient;

    /**
     * Constructor
     *
     * @param mongoAccess MongoDB client
     */
    public AccessContractImpl(MongoDbAccessAdminImpl mongoAccess) {
        this.mongoAccess = mongoAccess;
        this.logBookclient = LogbookOperationsClientFactory.getInstance().getClient();
    }



    @Override
    public RequestResponse<AccessContractModel> createContracts(List<AccessContractModel> contractModelList)
        throws VitamException {
        ParametersChecker.checkParameter(ACCESS_CONTRACT_IS_MANDATORY_PATAMETER, contractModelList);

        if (contractModelList.isEmpty()) {
            return new RequestResponseOK<>();
        }

        AccessContractManager manager = new AccessContractManager(logBookclient);

        manager.logStarted();

        final Set<String> contractNames = new HashSet<>();
        ArrayNode contractsToPersist = null;

        final VitamError error = new VitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem())
            .setHttpCode(Response.Status.BAD_REQUEST.getStatusCode());

        try {

            for (final AccessContractModel acm : contractModelList) {


                // if a contract have and id
                if (null != acm.getId()) {
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

                    final JsonNode accessContractNode = JsonHandler.toJsonNode(acm);


                    /* contract is valid, add it to the list to persist */
                    if (contractsToPersist == null) {
                        contractsToPersist = JsonHandler.createArrayNode();
                    }

                    contractsToPersist.add(accessContractNode);
                }


            }

            if (null != error.getErrors() && !error.getErrors().isEmpty()) {
                // log book + application log
                // stop
                String errorsDetails =
                    error.getErrors().stream().map(c -> c.getMessage()).collect(Collectors.joining(","));
                manager.logValidationError(errorsDetails);
                return error;
            }

            // at this point no exception occurred and no validation error detected
            // persist in collection
            // contractsToPersist.values().stream().map();
            // TODO: 3/28/17 create insertDocuments method that accepts VitamDocument instead of ArrayNode, so we can
            // use AccessContract at this point
            mongoAccess.insertDocuments(contractsToPersist, FunctionalAdminCollections.ACCESS_CONTRACT);
        } catch (Exception exp) {
            String err = new StringBuilder("Import access contracts error > ").append(exp.getMessage()).toString();
            manager.logFatalError(err);
            return error.setCode(VitamCode.GLOBAL_INTERNAL_SERVER_ERROR.getItem()).setDescription(err).setHttpCode(
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        }

        manager.logSuccess();


        return new RequestResponseOK<AccessContractModel>().addAllResults(contractModelList).setHits(
            contractModelList.size(), 0, contractModelList.size()).setHttpCode(Response.Status.CREATED.getStatusCode());
    }

    @Override
    public AccessContractModel findOne(String id) throws ReferentialException, InvalidParseOperationException {

        final SelectParserSingle parser = new SelectParserSingle(new VarNameAdapter());
        parser.parse(new Select().getFinalSelect());
        try {
            parser.addCondition(QueryHelper.eq("#id", id));
        } catch (InvalidCreateOperationException e) {
            throw new ReferentialException(e);
        }
        JsonNode queryDsl = parser.getRequest().getFinalSelect();

        MongoCursor<VitamDocument<?>> cursor =
            mongoAccess.findDocuments(queryDsl, FunctionalAdminCollections.ACCESS_CONTRACT);
        if (null == cursor)
            return null;

        while (cursor.hasNext()) {
            final AccessContract accessContract = (AccessContract) cursor.next();
            return JsonHandler.getFromString(accessContract.toJson(), AccessContractModel.class);
        }

        return null;
    }

    @Override
    public List<AccessContractModel> findContracts(JsonNode queryDsl)
        throws ReferentialException, InvalidParseOperationException {
        final List<AccessContractModel> accessContractModelCollection = new ArrayList<>();
        MongoCursor<VitamDocument<?>> cursor =
            mongoAccess.findDocuments(queryDsl, FunctionalAdminCollections.ACCESS_CONTRACT);

        if (null == cursor)
            return accessContractModelCollection;

        while (cursor.hasNext()) {
            final AccessContract accessContract = (AccessContract) cursor.next();
            accessContractModelCollection.add(JsonHandler.getFromString(accessContract.toJson(),
                AccessContractModel.class));
        }

        return accessContractModelCollection;
    }

    /**
     * Contract validator and logBook manager
     */
    protected final static class AccessContractManager {

        private static List<GenericContractValidator<AccessContractModel>> validators = Arrays.asList(
            createMandatoryParamsValidator(), createWrongFieldFormatValidator(),
            createCheckDuplicateInDatabaseValidator());

        final LogbookOperationsClientHelper helper = new LogbookOperationsClientHelper();
        private GUID eip = null;

        private LogbookOperationsClient logBookclient;

        public AccessContractManager(LogbookOperationsClient logBookclient) {
            this.logBookclient = logBookclient;
        }

        private boolean validateContract(AccessContractModel contract, String jsonFormat,
            VitamError error) {

            for (GenericContractValidator validator : validators) {
                Optional<GenericRejectionCause> result = validator.validate(contract, jsonFormat);
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
         *
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
                } catch (InvalidParseOperationException e) {
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

        private void logUpdateSuccess(String id, String updateEventDetailData, String oldValue) throws VitamException {
            final ObjectNode evDetData = JsonHandler.createObjectNode();
            final ObjectNode msg = JsonHandler.createObjectNode();
            msg.put("updateField", "Status");
            msg.put("oldValue", oldValue);
            msg.put("newValue", updateEventDetailData);
            evDetData.put("AccessContract", msg);
            String wellFormedJson = SanityChecker.sanitizeJson(evDetData);
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

                if (contract.getOriginatingAgencies() == null || contract.getOriginatingAgencies().isEmpty()) {
                    rejection = GenericRejectionCause.rejectMandatoryMissing(AccessContract.ORIGINATINGAGENCIES);
                }

                return (rejection == null) ? Optional.empty() : Optional.of(rejection);
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
                DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE.ofPattern("dd/MM/yyyy");


                String now = LocalDateUtil.now().toString();
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
                    if (contract.getCreationdate() == null || contract.getCreationdate().isEmpty()) {
                        contract.setCreationdate(now);
                    } else {
                        contract.setCreationdate(
                            LocalDate.parse(contract.getCreationdate(), formatter).atStartOfDay().toString());
                    }

                } catch (Exception e) {
                    LOGGER.error("Error access contract parse dates", e);
                    rejection = GenericRejectionCause.rejectMandatoryMissing("Creationdate");
                }
                try {
                    if (contract.getActivationdate() == null || contract.getActivationdate().isEmpty()) {
                        contract.setActivationdate(now);
                    } else {
                        contract.setActivationdate(
                            LocalDate.parse(contract.getActivationdate(), formatter).atStartOfDay().toString());

                    }
                } catch (Exception e) {
                    LOGGER.error("Error access contract parse dates", e);
                    rejection = GenericRejectionCause.rejectMandatoryMissing("ActivationDate");
                }
                try {

                    if (contract.getDeactivationdate() == null || contract.getDeactivationdate().isEmpty()) {

                    } else {

                        contract.setDeactivationdate(
                            LocalDate.parse(contract.getDeactivationdate(), formatter).atStartOfDay().toString());
                    }
                } catch (Exception e) {
                    LOGGER.error("Error access contract parse dates", e);
                    rejection = GenericRejectionCause.rejectMandatoryMissing("deactivationdate");
                }

                contract.setLastupdate(now);

                return (rejection == null) ? Optional.empty() : Optional.of(rejection);
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
                int tenant = ParameterHelper.getTenantParameter();
                Bson clause = and(eq(VitamDocument.TENANT_ID, tenant), eq(AccessContract.NAME, contract.getName()));
                boolean exist = FunctionalAdminCollections.ACCESS_CONTRACT.getCollection().count(clause) > 0;
                if (exist) {
                    rejection = GenericRejectionCause.rejectDuplicatedInDatabase(contractName);
                }
                return (rejection == null) ? Optional.empty() : Optional.of(rejection);
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

        AccessContractModel accContractModel = findOne(id);
        if (accContractModel == null) {
            return error.addToErrors(new VitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem()).setMessage(
                "Access contract not find" + id));
        }
        AccessContractManager manager = new AccessContractManager(logBookclient);
        manager.logUpdateStarted(accContractModel.getId());
        if (queryDsl == null || !queryDsl.isObject()) {
            return error;
        }

        JsonNode actionNode = queryDsl.get(GLOBAL.ACTION.exactToken());

        String updateStatus = null;
        for (JsonNode fieldToSet : actionNode) {
            JsonNode fieldName = fieldToSet.get(UPDATEACTION.SET.exactToken());
            if (fieldName != null) {
                Iterator<String> it = fieldName.fieldNames();
                while (it.hasNext()) {
                    String field = it.next();
                    JsonNode status = fieldName.findValue(field);
                    if ("Status".equals(field)) {
                        if (!(ContractStatus.ACTIVE.name().equals(status.asText()) || ContractStatus.INACTIVE
                            .name().equals(status.asText()))) {
                            error.addToErrors(new VitamError(VitamCode.CONTRACT_VALIDATION_ERROR.getItem()).setMessage(
                                "The Access contract status must be ACTIVE or INACTIVE but not " + status.asText()));
                        }
                        updateStatus = status.asText();
                    }
                }
            }
        }

        if (error.getErrors() != null && error.getErrors().size() > 0) {
            String errorsDetails =
                error.getErrors().stream().map(c -> c.getMessage()).collect(Collectors.joining(","));
            manager.logValidationError(errorsDetails);

            return error;
        }

        try {
            mongoAccess.updateData(queryDsl, FunctionalAdminCollections.ACCESS_CONTRACT);
        } catch (ReferentialException e) {
            String err = new StringBuilder("Update access contracts error > ").append(e.getMessage()).toString();
            manager.logFatalError(err);
            error.setCode(VitamCode.GLOBAL_INTERNAL_SERVER_ERROR.getItem())
                .setDescription(err)
                .setHttpCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());

            return error;
        }
        manager.logUpdateSuccess(id, updateStatus, accContractModel.getStatus());
        return new RequestResponseOK<AccessContractModel>();
    }

    /**
     * FIXME : How to update the access contract status (?) with queryDsl or with internal DTO
     * 
     * @return map with the parameters to update
     */
    private Map<String, Object> createMapForUpdate(Boolean status) {
        final Map<String, Object> mapForUpdate = new HashMap<String, Object>();
        String now = LocalDateUtil.now().toString();
        if (BooleanUtils.isTrue(status)) {
            mapForUpdate.put(AccessContract.STATUS, true);
            mapForUpdate.put(AccessContract.ACTIVATIONDATE, now);
            mapForUpdate.put(AccessContract.LAST_UPDATE, now);
        } else {
            mapForUpdate.put(AccessContract.STATUS, false);
            mapForUpdate.put(AccessContract.DEACTIVATIONDATE, now);
            mapForUpdate.put(AccessContract.LAST_UPDATE, now);
        }
        return mapForUpdate;
    }

}
