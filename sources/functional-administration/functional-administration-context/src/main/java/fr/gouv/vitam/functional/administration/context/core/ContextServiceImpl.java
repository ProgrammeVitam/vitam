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
package fr.gouv.vitam.functional.administration.context.core;

import static com.mongodb.client.model.Filters.eq;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response;

import org.bson.conversions.Bson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.parser.request.adapter.SingleVarNameAdapter;
import fr.gouv.vitam.common.database.parser.request.single.SelectParserSingle;
import fr.gouv.vitam.common.database.server.DbRequestResult;
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
import fr.gouv.vitam.common.model.AccessContractModel;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.model.ContextModel;
import fr.gouv.vitam.functional.administration.client.model.IngestContractModel;
import fr.gouv.vitam.functional.administration.client.model.PermissionModel;
import fr.gouv.vitam.functional.administration.common.Context;
import fr.gouv.vitam.functional.administration.common.IngestContract;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.functional.administration.context.api.ContextService;
import fr.gouv.vitam.functional.administration.context.core.ContextValidator.ContextRejectionCause;
import fr.gouv.vitam.functional.administration.contract.api.ContractService;
import fr.gouv.vitam.functional.administration.contract.core.AccessContractImpl;
import fr.gouv.vitam.functional.administration.contract.core.IngestContractImpl;
import fr.gouv.vitam.functional.administration.counter.SequenceType;
import fr.gouv.vitam.functional.administration.counter.VitamCounterService;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationsClientHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;

public class ContextServiceImpl implements ContextService {
    private static final String INVALID_IDENTIFIER_OF_THE_ACCESS_CONTRACT = "Invalid identifier of the access contract:";

    private static final String INVALID_IDENTIFIER_OF_THE_INGEST_CONTRACT = "Invalid identifier of the ingest contract:";

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ContextServiceImpl.class);

    private static final String CONTEXT_IS_MANDATORY_PATAMETER = "contexts parameter is mandatory";
    private static final String CONTEXTS_IMPORT_EVENT = "STP_IMPORT_CONTEXT";
    private static final String CONTEXTS_UPDATE_EVENT = "STP_UPDATE_CONTEXT";
    private static final String IDENTIFIER = "Identifier";
    private static final String UPDATE_CONTEXT_MANDATORY_PATAMETER = "context is mandatory";

    private final MongoDbAccessAdminImpl mongoAccess;
    private final LogbookOperationsClient logBookclient;
    private final VitamCounterService vitamCounterService;


    /**
     * Constructor
     *
     * @param mongoAccess MongoDB client
     */
    public ContextServiceImpl(MongoDbAccessAdminImpl mongoAccess, VitamCounterService vitamCounterService) {
        this.mongoAccess = mongoAccess;
        this.vitamCounterService = vitamCounterService;
        logBookclient = LogbookOperationsClientFactory.getInstance().getClient();
    }

    @Override
    public RequestResponse<ContextModel> createContexts(List<ContextModel> contextModelList) throws VitamException {
        ParametersChecker.checkParameter(CONTEXT_IS_MANDATORY_PATAMETER, contextModelList);

        if (contextModelList.isEmpty()) {
            return new RequestResponseOK<>();
        }

        final ContextServiceImpl.ContextManager manager =
            new ContextServiceImpl.ContextManager(logBookclient, mongoAccess, vitamCounterService);

        manager.logStarted();

        final List<ContextModel> contextsListToPersist = new ArrayList<>();
        ArrayNode contextsToPersist = null;
        final Set<String> contextNames = new HashSet<>();
        final VitamError error = new VitamError(VitamCode.CONTEXT_VALIDATION_ERROR.getItem())
            .setHttpCode(Response.Status.BAD_REQUEST.getStatusCode());

        try {            
            for (final ContextModel cm : contextModelList){
               final  String code = vitamCounterService.getNextSequenceAsString(ParameterHelper.getTenantParameter(), SequenceType.CONTEXT_SEQUENCE.getName());
                cm.setIdentifier(code);

                // if a contract have an id
                if (null != cm.getId()) {
                    error.addToErrors(new VitamError(VitamCode.CONTEXT_VALIDATION_ERROR.getItem()).setMessage(
                        ContextRejectionCause.rejectIdNotAllowedInCreate(cm.getName()).getReason()));
                    continue;
                }

                // if a contract with the same name is already treated mark the current one as duplicated
                if (contextNames.contains(cm.getName())) {
                    error.addToErrors(new VitamError(VitamCode.CONTEXT_VALIDATION_ERROR.getItem()).setMessage(
                        ContextRejectionCause.rejectDuplicatedEntry(cm.getName()).getReason()));
                    continue;
                }

                // mark the current contract as treated
                contextNames.add(cm.getName());

                // validate context
                if (manager.validateContext(cm, error)) {

                    cm.setId(GUIDFactory.newContextGUID().getId());
                    cm.setCreationdate(LocalDateUtil.getString(LocalDateUtil.now()));
                    cm.setLastupdate(LocalDateUtil.getString(LocalDateUtil.now()));

                    final JsonNode contextNode = JsonHandler.toJsonNode(cm);


                    /* context is valid, add it to the list to persist */
                    if (contextsToPersist == null) {
                        contextsToPersist = JsonHandler.createArrayNode();
                    }

                    contextsToPersist.add(contextNode);
                    final ContextModel ctxt = JsonHandler.getFromJsonNode(contextNode, ContextModel.class);
                    contextsListToPersist.add(ctxt);
                }
            }

            if (null != error.getErrors() && !error.getErrors().isEmpty()) {
                // log book + application log
                // stop
                final String errorsDetails =
                    error.getErrors().stream().map(c -> c.getMessage()).collect(Collectors.joining(","));
                manager.logValidationError(errorsDetails, CONTEXTS_IMPORT_EVENT);
                return error;
            }

            mongoAccess.insertDocuments(contextsToPersist, FunctionalAdminCollections.CONTEXT).close();
        } catch (final Exception exp) {
            final String err = new StringBuilder("Import contexts error > ").append(exp.getMessage()).toString();
            manager.logFatalError(err);
            return error.setCode(VitamCode.GLOBAL_INTERNAL_SERVER_ERROR.getItem()).setDescription(err).setHttpCode(
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        }

        manager.logSuccess();

        return new RequestResponseOK<ContextModel>().addAllResults(contextsListToPersist)
            .setHttpCode(Response.Status.CREATED.getStatusCode());
    }

    @Override
    public DbRequestResult findContexts(JsonNode queryDsl)
        throws ReferentialException, InvalidParseOperationException {
        return mongoAccess.findDocuments(queryDsl, FunctionalAdminCollections.CONTEXT);
    }

    @Override
    public ContextModel findOneContextById(String id) throws ReferentialException, InvalidParseOperationException {
        SanityChecker.checkParameter(id);
        final SelectParserSingle parser = new SelectParserSingle(new SingleVarNameAdapter());
        parser.parse(new Select().getFinalSelect());
        try {
            parser.addCondition(QueryHelper.eq("Identifier", id));
        } catch (InvalidCreateOperationException e) {
            throw new ReferentialException(e);
        }
        try (DbRequestResult result =
            mongoAccess.findDocuments(parser.getRequest().getFinalSelect(), FunctionalAdminCollections.CONTEXT)) {
            final List<ContextModel> list = result.getDocuments(Context.class, ContextModel.class);
            if (list.isEmpty()) {
                throw new ReferentialException("Context not found");
            }
            return list.get(0);
        }
    }

    @Override
    public RequestResponse<ContextModel> updateContext(String id, JsonNode queryDsl)
        throws VitamException {
        ParametersChecker.checkParameter(UPDATE_CONTEXT_MANDATORY_PATAMETER, queryDsl);
        SanityChecker.checkJsonAll(queryDsl);
        final VitamError error = new VitamError(VitamCode.CONTEXT_VALIDATION_ERROR.getItem())
            .setHttpCode(Response.Status.BAD_REQUEST.getStatusCode());

        final ContextModel contextModel = findOneContextById(id);
        final ContextServiceImpl.ContextManager manager =
            new ContextServiceImpl.ContextManager(logBookclient, mongoAccess, vitamCounterService);
        manager.logUpdateStarted(contextModel.getId());
        final JsonNode permissionsNode = queryDsl.findValue(ContextModel.PERMISSIONS);
        if (permissionsNode != null && permissionsNode.isArray()) {
            for (JsonNode permission : permissionsNode) {
                PermissionModel permissionModel = JsonHandler.getFromJsonNode(permission, PermissionModel.class);
                final int tenantId = permissionModel.getTenant();
                for (String accessContractId : permissionModel.getAccessContract()) {
                    if (!ContextManager.checkIdentifierOfAccessContract(accessContractId, tenantId)) {
                        error.addToErrors(
                            new VitamError(VitamCode.CONTEXT_VALIDATION_ERROR.getItem())
                                .setMessage(INVALID_IDENTIFIER_OF_THE_INGEST_CONTRACT + accessContractId));
                    }
                }

                for (String ingestContractId : permissionModel.getIngestContract()) {
                    if (!ContextManager.checkIdentifierOfIngestContract(ingestContractId, tenantId)) {
                        error.addToErrors(
                            new VitamError(VitamCode.CONTEXT_VALIDATION_ERROR.getItem())
                                .setMessage(INVALID_IDENTIFIER_OF_THE_ACCESS_CONTRACT + ingestContractId));
                    }
                }
            }

        }

        if (error.getErrors() != null && error.getErrors().size() > 0) {
            final String errorsDetails =
                error.getErrors().stream().map(c -> c.getMessage()).collect(Collectors.joining(","));
            manager.logValidationError(errorsDetails, CONTEXTS_UPDATE_EVENT);

            return error;
        }

        try {
            mongoAccess.updateData(queryDsl, FunctionalAdminCollections.CONTEXT).close();
        } catch (final ReferentialException e) {
            final String err = new StringBuilder("Update context error > ").append(e.getMessage()).toString();
            error.setCode(VitamCode.GLOBAL_INTERNAL_SERVER_ERROR.getItem())
                .setDescription(err)
                .setHttpCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
            manager.logFatalError(err);
            return error;
        }

        manager.logUpdateSuccess(id, queryDsl.toString(), JsonHandler.unprettyPrint(contextModel));
        return new RequestResponseOK<>();
    }

    /**
     * Context validator and logBook manager
     */
    protected final static class ContextManager {
        final LogbookOperationsClientHelper helper = new LogbookOperationsClientHelper();
        private GUID eip = null;
        private final LogbookOperationsClient logBookclient;
        private static ContractService<AccessContractModel> accessContract;
        private static ContractService<IngestContractModel> ingestContract;
        private static List<ContextValidator> validators = Arrays.asList(
            createMandatoryParamsValidator(), createCheckDuplicateInDatabaseValidator(), checkContract());

        public ContextManager(LogbookOperationsClient logBookclient,
            MongoDbAccessAdminImpl mongoAccess, VitamCounterService vitamCounterService) {
            this.logBookclient = logBookclient;
            ingestContract = new IngestContractImpl(mongoAccess, vitamCounterService);
            accessContract = new AccessContractImpl(mongoAccess, vitamCounterService);
        }

        public boolean validateContext(ContextModel context, VitamError error) {
            for (final ContextValidator validator : validators) {
                final Optional<ContextRejectionCause> result = validator.validate(context);
                if (result.isPresent()) {
                    // there is a validation error on this context
                    /* context is valid, add it to the list to persist */
                    error.addToErrors(new VitamError(VitamCode.CONTEXT_VALIDATION_ERROR.getItem())
                        .setMessage(result.get().getReason()));
                    // once a validation error is detected on a context, jump to next context
                    return false;
                }
            }
            return true;
        }

        /**
         * log start process
         *
         * @throws VitamException
         */
        private void logStarted() throws VitamException {
            eip = GUIDFactory.newOperationLogbookGUID(ParameterHelper.getTenantParameter());
            final LogbookOperationParameters logbookParameters = LogbookParametersFactory
                .newLogbookOperationParameters(eip, CONTEXTS_IMPORT_EVENT, eip, LogbookTypeProcess.MASTERDATA,
                    StatusCode.STARTED,
                    VitamLogbookMessages.getCodeOp(CONTEXTS_IMPORT_EVENT, StatusCode.STARTED), eip);

            helper.createDelegate(logbookParameters);

        }

        /**
         * log end success process
         *
         * @throws VitamException
         */
        private void logSuccess() throws VitamException {
            final LogbookOperationParameters logbookParameters = LogbookParametersFactory
                .newLogbookOperationParameters(eip, CONTEXTS_IMPORT_EVENT, eip, LogbookTypeProcess.MASTERDATA,
                    StatusCode.OK,
                    VitamLogbookMessages.getCodeOp(CONTEXTS_IMPORT_EVENT, StatusCode.OK), eip);
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
                .newLogbookOperationParameters(eip, CONTEXTS_UPDATE_EVENT, eip, LogbookTypeProcess.MASTERDATA,
                    StatusCode.STARTED,
                    VitamLogbookMessages.getCodeOp(CONTEXTS_UPDATE_EVENT, StatusCode.STARTED), eip);
            logbookParameters.putParameterValue(LogbookParameterName.outcomeDetail, CONTEXTS_UPDATE_EVENT +
                "." + StatusCode.STARTED);
            if (null != id && !id.isEmpty()) {
                logbookParameters.putParameterValue(LogbookParameterName.objectIdentifier, id);
            }
            helper.createDelegate(logbookParameters);

        }

        /**
         * log update success process
         *
         * @throws VitamException
         */
        private void logUpdateSuccess(String id, String query, String oldValue) throws VitamException {
            final ObjectNode evDetData = JsonHandler.createObjectNode();
            final ObjectNode msg = JsonHandler.createObjectNode();
            msg.put("oldValue", oldValue);
            msg.put("request", query);
            evDetData.set("Context", msg);
            final String wellFormedJson = SanityChecker.sanitizeJson(evDetData);
            final LogbookOperationParameters logbookParameters =
                LogbookParametersFactory
                    .newLogbookOperationParameters(
                        eip,
                        CONTEXTS_UPDATE_EVENT,
                        eip,
                        LogbookTypeProcess.MASTERDATA,
                        StatusCode.OK,
                        VitamLogbookMessages.getCodeOp(CONTEXTS_UPDATE_EVENT, StatusCode.OK),
                        eip);

            if (null != id && !id.isEmpty()) {
                logbookParameters.putParameterValue(LogbookParameterName.objectIdentifier, id);
            }
            logbookParameters.putParameterValue(LogbookParameterName.eventDetailData,
                wellFormedJson);
            logbookParameters.putParameterValue(LogbookParameterName.outcomeDetail, CONTEXTS_UPDATE_EVENT +
                "." + StatusCode.OK);
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
                .newLogbookOperationParameters(eip, CONTEXTS_IMPORT_EVENT, eip, LogbookTypeProcess.MASTERDATA,
                    StatusCode.FATAL,
                    VitamLogbookMessages.getCodeOp(CONTEXTS_IMPORT_EVENT, StatusCode.FATAL), eip);
            logbookMessageError(errorsDetails, logbookParameters);
            helper.updateDelegate(logbookParameters);
            logBookclient.bulkCreate(eip.getId(), helper.removeCreateDelegate(eip.getId()));
        }

        private void logValidationError(String errorsDetails, String action) throws VitamException {
            LOGGER.error("There validation errors on the input file {}", errorsDetails);
            final LogbookOperationParameters logbookParameters = LogbookParametersFactory
                .newLogbookOperationParameters(eip, action, eip, LogbookTypeProcess.MASTERDATA,
                    StatusCode.KO,
                    VitamLogbookMessages.getCodeOp(action, StatusCode.KO), eip);
            logbookMessageError(errorsDetails, logbookParameters);
            helper.updateDelegate(logbookParameters);
            logBookclient.bulkCreate(eip.getId(), helper.removeCreateDelegate(eip.getId()));

        }

        private void logbookMessageError(String errorsDetails, LogbookOperationParameters logbookParameters) {
            if (null != errorsDetails && !errorsDetails.isEmpty()) {
                try {
                    final ObjectNode object = JsonHandler.createObjectNode();
                    object.put("contextCheck", errorsDetails);

                    final String wellFormedJson = SanityChecker.sanitizeJson(object);
                    logbookParameters.putParameterValue(LogbookParameterName.eventDetailData, wellFormedJson);
                } catch (final InvalidParseOperationException e) {
                    // Do nothing
                }
            }
        }

        /**
         * Validate that context have not a missing mandatory parameter
         *
         * @return
         */
        private static ContextValidator createMandatoryParamsValidator() {
            return (context) -> {
                ContextValidator.ContextRejectionCause rejection = null;
                if (context.getName() == null || context.getName().trim().isEmpty()) {
                    rejection =
                        ContextValidator.ContextRejectionCause.rejectMandatoryMissing(Context.NAME);
                }

                return rejection == null ? Optional.empty() : Optional.of(rejection);
            };
        }


        /**
         * Check if the context the same name already exists in database
         *
         * @return
         */
        private static ContextValidator createCheckDuplicateInDatabaseValidator() {
            return (context) -> {
                ContextValidator.ContextRejectionCause rejection = null;
                final Bson clause = eq(IngestContract.NAME, context.getName());
                final boolean exist = FunctionalAdminCollections.CONTEXT.getCollection().count(clause) > 0;
                if (exist) {
                    rejection = ContextValidator.ContextRejectionCause.rejectDuplicatedInDatabase(context.getName());
                }
                return rejection == null ? Optional.empty() : Optional.of(rejection);
            };
        }

        /**
         * Check if the ingest contract and access contract exist
         *
         * @return
         */
        private static ContextValidator checkContract() {
            return (context) -> {
                ContextValidator.ContextRejectionCause rejection = null;

                final List<PermissionModel> pmList = context.getPermissions();
                for (final PermissionModel pm : pmList) {
                    final int tenant = pm.getTenant();

                    final Set<String> icList = pm.getIngestContract();
                    for (final String ic : icList) {
                        if (!checkIdentifierOfIngestContract(ic, tenant)) {
                            rejection = ContextValidator.ContextRejectionCause.rejectNoExistanceOfIngestContract(ic);
                            return Optional.of(rejection);
                        }
                    }

                    final Set<String> acList = pm.getAccessContract();
                    for (final String ac : acList) {
                        if (!checkIdentifierOfAccessContract(ac, tenant)) {
                            rejection = ContextValidator.ContextRejectionCause.rejectNoExistanceOfAccessContract(ac);
                            return Optional.of(rejection);
                        }
                    }
                }

                return Optional.empty();
            };
        }


        public static boolean checkIdentifierOfIngestContract(String ic, int tenant) {
            final Select select = new Select();
            try {
                VitamThreadUtils.getVitamSession().setTenantId(tenant);
                final Query query = QueryHelper.and().add(QueryHelper.eq(IDENTIFIER, ic));
                select.setQuery(query);
                final JsonNode queryDsl = select.getFinalSelect();
                if (ingestContract.findContracts(queryDsl).isEmpty()) {
                    return false;
                }
            } catch (InvalidCreateOperationException | ReferentialException | InvalidParseOperationException e) {
                return false;
            }

            return true;
        }

        public static boolean checkIdentifierOfAccessContract(String ac, int tenant) {

            final Select select = new Select();
            try {
                VitamThreadUtils.getVitamSession().setTenantId(tenant);
                final Query query = QueryHelper.and().add(QueryHelper.eq(IDENTIFIER, ac));
                select.setQuery(query);
                final JsonNode queryDsl = select.getFinalSelect();
                if (accessContract.findContracts(queryDsl).isEmpty()) {
                    return false;
                }
            } catch (InvalidCreateOperationException | ReferentialException | InvalidParseOperationException e) {
                return false;
            }

            return true;
        }
    }

    @Override
    public void close() {
        logBookclient.close();
    }
}
