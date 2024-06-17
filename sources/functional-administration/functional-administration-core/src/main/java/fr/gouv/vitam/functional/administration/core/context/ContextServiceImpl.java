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
package fr.gouv.vitam.functional.administration.core.context;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Delete;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.parser.request.adapter.SingleVarNameAdapter;
import fr.gouv.vitam.common.database.parser.request.single.DeleteParserSingle;
import fr.gouv.vitam.common.database.parser.request.single.SelectParserSingle;
import fr.gouv.vitam.common.database.server.DbRequestResult;
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
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.model.administration.ContextModel;
import fr.gouv.vitam.common.model.administration.ContextStatus;
import fr.gouv.vitam.common.model.administration.IngestContractModel;
import fr.gouv.vitam.common.model.administration.PermissionModel;
import fr.gouv.vitam.common.model.administration.SecurityProfileModel;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.Context;
import fr.gouv.vitam.functional.administration.common.VitamErrorUtils;
import fr.gouv.vitam.functional.administration.common.counter.SequenceType;
import fr.gouv.vitam.functional.administration.common.counter.VitamCounterService;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialNotFoundException;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.functional.administration.core.backup.FunctionalBackupService;
import fr.gouv.vitam.functional.administration.core.context.ContextValidator.ContextRejectionCause;
import fr.gouv.vitam.functional.administration.core.contract.AccessContractImpl;
import fr.gouv.vitam.functional.administration.core.contract.ContractService;
import fr.gouv.vitam.functional.administration.core.contract.IngestContractImpl;
import fr.gouv.vitam.functional.administration.core.security.profile.SecurityProfileService;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.security.internal.client.InternalSecurityClient;
import fr.gouv.vitam.security.internal.client.InternalSecurityClientFactory;
import org.apache.commons.lang.StringUtils;
import org.bson.conversions.Bson;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.eq;
import static fr.gouv.vitam.common.database.parser.request.adapter.SimpleVarNameAdapter.change;
import static fr.gouv.vitam.common.database.server.mongodb.VitamDocument.TENANT_ID;

public class ContextServiceImpl implements ContextService {

    public static final String CONTEXTS_BACKUP_EVENT = "STP_BACKUP_CONTEXT";
    private static final String INVALID_IDENTIFIER_OF_THE_ACCESS_CONTRACT =
        "Invalid identifier of the access contract:";
    private static final String INVALID_IDENTIFIER_OF_THE_INGEST_CONTRACT =
        "Invalid identifier of the ingest contract:";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ContextServiceImpl.class);
    private static final String CONTEXT_IS_MANDATORY_PARAMETER = "contexts parameter is mandatory";
    private static final String CONTEXTS_IMPORT_EVENT = "STP_IMPORT_CONTEXT";
    private static final String CONTEXTS_UPDATE_EVENT = "STP_UPDATE_CONTEXT";
    private static final String CONTEXTS_DELETE_EVENT = "STP_DELETE_CONTEXT";
    private static final String EMPTY_REQUIRED_FIELD = "STP_IMPORT_CONTEXT.EMPTY_REQUIRED_FIELD.KO";
    private static final String SECURITY_PROFILE_NOT_FOUND = "STP_IMPORT_CONTEXT.SECURITY_PROFILE_NOT_FOUND.KO";
    private static final String DUPLICATE_IN_DATABASE = "STP_IMPORT_CONTEXT.IDENTIFIER_DUPLICATION.KO";
    private static final String UNKNOWN_VALUE = "STP_IMPORT_CONTEXT.UNKNOWN_VALUE.KO";

    private static final String UPDATE_UNKNOWN_VALUE = "STP_UPDATE_CONTEXT.UNKNOWN_VALUE.KO";
    private static final String UPDATE_KO = "STP_UPDATE_CONTEXT.KO";
    private static final String DELETE_KO = "STP_DELETE_CONTEXT.KO";

    private static final String UPDATE_CONTEXT_MANDATORY_PARAMETER = "context is mandatory";

    private final MongoDbAccessAdminImpl mongoAccess;
    private final LogbookOperationsClient logbookClient;
    private final InternalSecurityClient internalSecurityClient;
    private final VitamCounterService vitamCounterService;
    private final FunctionalBackupService functionalBackupService;
    private final ContractService<IngestContractModel> ingestContract;
    private final ContractService<AccessContractModel> accessContract;
    private SecurityProfileService securityProfileService;

    /**
     * Constructor
     *
     * @param mongoAccess MongoDB client
     */
    public ContextServiceImpl(MongoDbAccessAdminImpl mongoAccess, VitamCounterService vitamCounterService) {
        this.mongoAccess = mongoAccess;
        this.vitamCounterService = vitamCounterService;
        this.logbookClient = LogbookOperationsClientFactory.getInstance().getClient();
        this.internalSecurityClient = InternalSecurityClientFactory.getInstance().getClient();
        this.ingestContract = new IngestContractImpl(mongoAccess, vitamCounterService);
        this.accessContract = new AccessContractImpl(mongoAccess, vitamCounterService);
        this.functionalBackupService = new FunctionalBackupService(vitamCounterService);
    }

    /**
     * Constructor
     *
     * @param mongoAccess MongoDB client
     */
    @VisibleForTesting
    public ContextServiceImpl(
        MongoDbAccessAdminImpl mongoAccess,
        VitamCounterService vitamCounterService,
        ContractService<IngestContractModel> ingestContract,
        ContractService<AccessContractModel> accessContract,
        SecurityProfileService securityProfileService,
        FunctionalBackupService functionalBackupService
    ) {
        this.mongoAccess = mongoAccess;
        this.vitamCounterService = vitamCounterService;
        this.logbookClient = LogbookOperationsClientFactory.getInstance().getClient();
        this.internalSecurityClient = InternalSecurityClientFactory.getInstance().getClient();
        this.ingestContract = ingestContract;
        this.accessContract = accessContract;
        this.securityProfileService = securityProfileService;
        this.functionalBackupService = functionalBackupService;
    }

    @Override
    public RequestResponse<ContextModel> createContexts(List<ContextModel> contextModelList) throws VitamException {
        ParametersChecker.checkParameter(CONTEXT_IS_MANDATORY_PARAMETER, contextModelList);

        if (contextModelList.isEmpty()) {
            return new RequestResponseOK<>();
        }
        boolean slaveMode = vitamCounterService.isSlaveFunctionnalCollectionOnTenant(
            SequenceType.CONTEXT_SEQUENCE.getCollection(),
            ParameterHelper.getTenantParameter()
        );

        String operationId = VitamThreadUtils.getVitamSession().getRequestId();
        GUID eip = GUIDReader.getGUID(operationId);

        ContextManager manager = new ContextManager(
            logbookClient,
            accessContract,
            ingestContract,
            securityProfileService,
            eip
        );

        manager.logStarted();

        final List<ContextModel> contextsListToPersist = new ArrayList<>();
        final VitamError<ContextModel> error = new VitamError<ContextModel>(
            VitamCode.CONTEXT_VALIDATION_ERROR.getItem()
        ).setHttpCode(Response.Status.BAD_REQUEST.getStatusCode());

        ArrayNode contextsToPersist = JsonHandler.createArrayNode();

        try {
            for (final ContextModel cm : contextModelList) {
                if (!slaveMode) {
                    final String code = vitamCounterService.getNextSequenceAsString(
                        ParameterHelper.getTenantParameter(),
                        SequenceType.CONTEXT_SEQUENCE
                    );
                    cm.setIdentifier(code);
                }
                // if a contract have an id
                if (cm.getId() != null) {
                    error.addToErrors(
                        new VitamError<ContextModel>(VitamCode.CONTEXT_VALIDATION_ERROR.getItem()).setMessage(
                            ContextRejectionCause.rejectIdNotAllowedInCreate(cm.getName()).getReason()
                        )
                    );
                    continue;
                }

                // validate context
                if (manager.validateContext(cm, error)) {
                    cm.setId(GUIDFactory.newContextGUID().getId());
                    cm.setCreationdate(LocalDateUtil.getString(LocalDateUtil.now()));
                    cm.setLastupdate(LocalDateUtil.getString(LocalDateUtil.now()));

                    final ObjectNode contextNode = (ObjectNode) JsonHandler.toJsonNode(cm);
                    JsonNode jsonNode = contextNode.remove(VitamFieldsHelper.id());

                    if (jsonNode != null) {
                        contextNode.set("_id", jsonNode);
                    }

                    // change field permission.#tenantId by permission._tenantId
                    change(contextNode, VitamFieldsHelper.tenant(), TENANT_ID);

                    contextsToPersist.add(contextNode);
                    final ContextModel ctxt = JsonHandler.getFromJsonNode(contextNode, ContextModel.class);
                    contextsListToPersist.add(ctxt);
                }
                if (slaveMode) {
                    List<ContextRejectionCause> results = manager.checkEmptyIdentifierSlaveModeValidator().validate(cm);
                    for (ContextRejectionCause result : results) {
                        error.addToErrors(
                            new VitamError<ContextModel>(VitamCode.CONTEXT_VALIDATION_ERROR.getItem())
                                .setMessage(EMPTY_REQUIRED_FIELD)
                                .setDescription(result.getReason())
                                .setState(StatusCode.KO.name())
                        );
                    }
                }
            }

            if (null != error.getErrors() && !error.getErrors().isEmpty()) {
                // log book + application log
                // stop
                final String errorsDetails = error
                    .getErrors()
                    .stream()
                    .map(VitamError::getDescription)
                    .collect(Collectors.joining(","));
                manager.logValidationError(errorsDetails, CONTEXTS_IMPORT_EVENT, error.getErrors().get(0).getMessage());
                return error;
            }

            mongoAccess.insertDocuments(contextsToPersist, FunctionalAdminCollections.CONTEXT).close();

            functionalBackupService.saveCollectionAndSequence(
                eip,
                CONTEXTS_BACKUP_EVENT,
                FunctionalAdminCollections.CONTEXT,
                eip.toString()
            );
        } catch (final Exception exp) {
            final String err = "Import contexts error > " + exp.getMessage();
            manager.logFatalError(err);
            return error
                .setCode(VitamCode.GLOBAL_INTERNAL_SERVER_ERROR.getItem())
                .setDescription(err)
                .setHttpCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        }

        manager.logSuccess();

        return new RequestResponseOK<ContextModel>()
            .addAllResults(contextsListToPersist)
            .setHttpCode(Response.Status.CREATED.getStatusCode());
    }

    @Override
    public DbRequestResult findContexts(JsonNode queryDsl) throws ReferentialException {
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
        try (
            DbRequestResult result = mongoAccess.findDocuments(
                parser.getRequest().getFinalSelect(),
                FunctionalAdminCollections.CONTEXT
            )
        ) {
            final List<ContextModel> list = result.getDocuments(Context.class, ContextModel.class);
            if (list.isEmpty()) {
                throw new ReferentialNotFoundException("Context not found");
            }
            return list.get(0);
        }
    }

    @Override
    public RequestResponse<ContextModel> deleteContext(String contextId, boolean forceDelete) throws VitamException {
        SanityChecker.checkParameter(contextId);

        final DeleteParserSingle parser = new DeleteParserSingle(new SingleVarNameAdapter());
        parser.parse(new Delete().getFinalDelete());
        try {
            parser.addCondition(QueryHelper.eq(Context.IDENTIFIER, contextId));
        } catch (InvalidCreateOperationException e) {
            throw new ReferentialException(e);
        }

        JsonNode finalDelete = parser.getRequest().getFinalDelete();

        String operationId = VitamThreadUtils.getVitamSession().getRequestId();
        GUID eip = GUIDReader.getGUID(operationId);

        ContextManager manager = new ContextManager(
            logbookClient,
            accessContract,
            ingestContract,
            securityProfileService,
            eip
        );

        try {
            manager.logDeleteStarted(contextId);

            if (!exist(finalDelete)) {
                manager.logValidationError("Context not found : " + contextId, CONTEXTS_DELETE_EVENT, DELETE_KO);
                return getVitamError(
                    VitamCode.CONTEXT_VALIDATION_ERROR.getItem(),
                    "Delete context error : " + contextId
                ).setHttpCode(Response.Status.NOT_FOUND.getStatusCode());
            }

            if (internalSecurityClient.contextIsUsed(contextId) && !forceDelete) {
                manager.logValidationError("Delete context error : " + contextId, CONTEXTS_DELETE_EVENT, DELETE_KO);
                return getVitamError(
                    VitamCode.CONTEXT_VALIDATION_ERROR.getItem(),
                    "Delete context error : " + contextId
                )
                    .setHttpCode(Response.Status.FORBIDDEN.getStatusCode())
                    .setMessage(DELETE_KO);
            }

            RequestResponseOK<ContextModel> response = new RequestResponseOK<>();

            DbRequestResult result = mongoAccess.deleteDocument(finalDelete, FunctionalAdminCollections.CONTEXT);

            response
                .addAllResults(result.getDocuments(Context.class, ContextModel.class))
                .setTotal(result.getTotal())
                .setHttpCode(Response.Status.NO_CONTENT.getStatusCode());

            // close result
            result.close();

            functionalBackupService.saveCollectionAndSequence(
                eip,
                CONTEXTS_BACKUP_EVENT,
                FunctionalAdminCollections.CONTEXT,
                eip.toString()
            );

            manager.logDeleteSuccess(contextId);

            return response;
        } catch (SchemaValidationException | BadRequestException e) {
            LOGGER.error(e);
            final String err = "Delete context error > " + e.getMessage();

            // logbook error event
            manager.logValidationError(err, CONTEXTS_DELETE_EVENT, DELETE_KO);

            return getVitamError(VitamCode.CONTEXT_VALIDATION_ERROR.getItem(), e.getMessage())
                .setHttpCode(Response.Status.BAD_REQUEST.getStatusCode())
                .setMessage(DELETE_KO);
        } catch (final Exception e) {
            LOGGER.error(e);
            final VitamError<ContextModel> error = getVitamError(
                VitamCode.CONTEXT_VALIDATION_ERROR.getItem(),
                "Context delete error"
            ).setHttpCode(Response.Status.BAD_REQUEST.getStatusCode());
            final String err = "Delete context error > " + e.getMessage();
            error
                .setCode(VitamCode.GLOBAL_INTERNAL_SERVER_ERROR.getItem())
                .setDescription(err)
                .setHttpCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());

            // logbook error event
            manager.logFatalError(err);
            return error;
        }
    }

    @Override
    public boolean securityProfileIsUsedInContexts(String securityProfileId)
        throws InvalidCreateOperationException, ReferentialException, InvalidParseOperationException {
        final Select select = new Select();
        select.setQuery(QueryHelper.eq(Context.SECURITY_PROFILE, securityProfileId));
        return exist(select.getFinalSelect());
    }

    private boolean exist(JsonNode finalSelect) throws InvalidParseOperationException, ReferentialException {
        DbRequestResult result = mongoAccess.findDocuments(finalSelect, FunctionalAdminCollections.CONTEXT);
        final List<ContextModel> list = result.getDocuments(Context.class, ContextModel.class);
        return !list.isEmpty();
    }

    @Override
    public RequestResponse<ContextModel> updateContext(String id, JsonNode queryDsl) throws VitamException {
        ParametersChecker.checkParameter(UPDATE_CONTEXT_MANDATORY_PARAMETER, queryDsl);
        SanityChecker.checkJsonAll(queryDsl);
        final VitamError<ContextModel> error = getVitamError(
            VitamCode.CONTEXT_VALIDATION_ERROR.getItem(),
            "Context update error"
        ).setHttpCode(Response.Status.BAD_REQUEST.getStatusCode());

        final ContextModel contextModel = findOneContextById(id);

        String operationId = VitamThreadUtils.getVitamSession().getRequestId();
        GUID eip = GUIDReader.getGUID(operationId);

        ContextManager manager = new ContextManager(
            logbookClient,
            accessContract,
            ingestContract,
            securityProfileService,
            eip
        );

        manager.logUpdateStarted(contextModel.getId());
        final JsonNode permissionsNode = queryDsl.findValue(ContextModel.TAG_PERMISSIONS);
        if (permissionsNode != null && permissionsNode.isArray()) {
            for (JsonNode permission : permissionsNode) {
                PermissionModel permissionModel = JsonHandler.getFromJsonNode(permission, PermissionModel.class);
                final int tenantId = permissionModel.getTenant();
                for (String accessContractId : permissionModel.getAccessContract()) {
                    if (!manager.checkIdentifierOfAccessContract(accessContractId, tenantId)) {
                        error.addToErrors(
                            new VitamError<ContextModel>(VitamCode.CONTEXT_VALIDATION_ERROR.getItem())
                                .setDescription(INVALID_IDENTIFIER_OF_THE_ACCESS_CONTRACT + accessContractId)
                                .setMessage(UPDATE_UNKNOWN_VALUE)
                        );
                    }
                }

                for (String ingestContractId : permissionModel.getIngestContract()) {
                    if (!manager.checkIdentifierOfIngestContract(ingestContractId, tenantId)) {
                        error.addToErrors(
                            new VitamError<ContextModel>(VitamCode.CONTEXT_VALIDATION_ERROR.getItem())
                                .setDescription(INVALID_IDENTIFIER_OF_THE_INGEST_CONTRACT + ingestContractId)
                                .setMessage(UPDATE_UNKNOWN_VALUE)
                        );
                    }
                }
            }
        }

        if (error.getErrors() != null && error.getErrors().size() > 0) {
            final String errorsDetails = error
                .getErrors()
                .stream()
                .map(VitamError::getDescription)
                .collect(Collectors.joining(","));
            manager.logValidationError(errorsDetails, CONTEXTS_UPDATE_EVENT, error.getErrors().get(0).getMessage());

            return error.setState(StatusCode.KO.name());
        }

        String diff = null;
        RequestResponseOK<ContextModel> response = new RequestResponseOK<>();

        final JsonNode actionNode = queryDsl.get(BuilderToken.GLOBAL.ACTION.exactToken());
        for (final JsonNode fieldToSet : actionNode) {
            final JsonNode fieldName = fieldToSet.get(BuilderToken.UPDATEACTION.SET.exactToken());
            if (fieldName != null) {
                ((ObjectNode) fieldName).remove(ContextModel.TAG_CREATION_DATE);
                ((ObjectNode) fieldName).put(ContextModel.TAG_LAST_UPDATE, LocalDateUtil.nowFormatted());
            }
        }

        try {
            DbRequestResult result = mongoAccess.updateData(queryDsl, FunctionalAdminCollections.CONTEXT);

            response
                .addAllResults(result.getDocuments(Context.class, ContextModel.class))
                .setTotal(result.getTotal())
                .setQuery(queryDsl)
                .setHttpCode(Response.Status.OK.getStatusCode());

            List<String> updates = null;
            // if at least one change was applied
            if (result.getCount() > 0) {
                // get first list of changes as we updated only one context
                updates = result.getDiffs().values().stream().findFirst().orElseThrow(NoSuchElementException::new);
            }

            // close result
            result.close();

            // create diff for evDetData
            if (updates != null && updates.size() > 0) {
                // concat changes
                String modifs = String.join("\n", updates);

                // create diff as json string
                final ObjectNode diffObject = JsonHandler.createObjectNode();
                diffObject.put("diff", modifs);
                diffObject.put("version", VitamConfiguration.getDiffVersion());
                diff = SanityChecker.sanitizeJson(diffObject);
            }

            functionalBackupService.saveCollectionAndSequence(
                eip,
                CONTEXTS_BACKUP_EVENT,
                FunctionalAdminCollections.CONTEXT,
                contextModel.getId()
            );
        } catch (SchemaValidationException | BadRequestException e) {
            LOGGER.error(e);
            final String err = "Update context error > " + e.getMessage();

            // logbook error event
            manager.logValidationError(err, CONTEXTS_UPDATE_EVENT, UPDATE_KO);

            return getVitamError(VitamCode.CONTEXT_VALIDATION_ERROR.getItem(), e.getMessage())
                .setHttpCode(Response.Status.BAD_REQUEST.getStatusCode())
                .setMessage(UPDATE_KO);
        } catch (final Exception e) {
            LOGGER.error(e);
            final String err = "Update context error > " + e.getMessage();
            error
                .setCode(VitamCode.GLOBAL_INTERNAL_SERVER_ERROR.getItem())
                .setDescription(err)
                .setHttpCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());

            // logbook error event
            manager.logFatalError(err);
            return error;
        }

        // logbook success event
        manager.logUpdateSuccess(contextModel.getId(), diff);

        return response;
    }

    private VitamError<ContextModel> getVitamError(String vitamCode, String error) {
        return VitamErrorUtils.getVitamError(vitamCode, error, "Context", StatusCode.KO, ContextModel.class);
    }

    @Override
    public void close() {
        logbookClient.close();
    }

    @Override
    public void setSecurityProfileService(SecurityProfileService securityProfileService) {
        this.securityProfileService = securityProfileService;
    }

    /**
     * Context validator and logBook manager
     */
    private static final class ContextManager {

        private final GUID eip;
        private final LogbookOperationsClient logbookClient;
        private final ContractService<AccessContractModel> accessContract;
        private final ContractService<IngestContractModel> ingestContract;
        private final SecurityProfileService securityProfileService;
        private final Map<ContextValidator, String> validators;

        public ContextManager(
            LogbookOperationsClient logbookClient,
            ContractService<AccessContractModel> accessContract,
            ContractService<IngestContractModel> ingestContract,
            SecurityProfileService securityProfileService,
            GUID eip
        ) {
            this.eip = eip;
            this.logbookClient = logbookClient;
            this.accessContract = accessContract;
            this.ingestContract = ingestContract;
            this.securityProfileService = securityProfileService;
            // Init validator
            validators = new HashMap<>() {
                {
                    put(createMandatoryParamsValidator(), EMPTY_REQUIRED_FIELD);
                    put(securityProfileIdentifierValidator(), SECURITY_PROFILE_NOT_FOUND);
                    put(createCheckDuplicateInDatabaseValidator(), DUPLICATE_IN_DATABASE);
                    put(checkTenant(), UNKNOWN_VALUE);
                    put(checkContract(), UNKNOWN_VALUE);
                }
            };
        }

        public boolean validateContext(ContextModel context, VitamError<ContextModel> error)
            throws ReferentialException, InvalidParseOperationException {
            for (final ContextValidator validator : validators.keySet()) {
                final List<ContextRejectionCause> validatorErrors = validator.validate(context);
                if (!validatorErrors.isEmpty()) {
                    for (ContextRejectionCause validatorError : validatorErrors) {
                        // there is a validation error on this context
                        /* context is valid, add it to the list to persist */
                        error.addToErrors(
                            new VitamError<ContextModel>(VitamCode.CONTEXT_VALIDATION_ERROR.getItem())
                                .setMessage(validators.get(validator))
                                .setDescription(validatorError.getReason())
                                .setState(StatusCode.KO.name())
                        );
                        // once a validation error is detected on a context, jump to next context
                    }
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
            final LogbookOperationParameters logbookParameters = LogbookParameterHelper.newLogbookOperationParameters(
                eip,
                CONTEXTS_IMPORT_EVENT,
                eip,
                LogbookTypeProcess.MASTERDATA,
                StatusCode.STARTED,
                VitamLogbookMessages.getCodeOp(CONTEXTS_IMPORT_EVENT, StatusCode.STARTED),
                eip
            );

            logbookClient.create(logbookParameters);
        }

        /**
         * log end success process
         *
         * @throws VitamException
         */
        private void logSuccess() throws VitamException {
            final GUID eipUsage = GUIDFactory.newOperationLogbookGUID(ParameterHelper.getTenantParameter());
            final LogbookOperationParameters logbookParameters = LogbookParameterHelper.newLogbookOperationParameters(
                eipUsage,
                CONTEXTS_IMPORT_EVENT,
                eip,
                LogbookTypeProcess.MASTERDATA,
                StatusCode.OK,
                VitamLogbookMessages.getCodeOp(CONTEXTS_IMPORT_EVENT, StatusCode.OK),
                eip
            );
            logbookClient.update(logbookParameters);
        }

        /**
         * log update start process
         *
         * @throws VitamException
         */
        private void logUpdateStarted(String id) throws VitamException {
            final LogbookOperationParameters logbookParameters = LogbookParameterHelper.newLogbookOperationParameters(
                eip,
                CONTEXTS_UPDATE_EVENT,
                eip,
                LogbookTypeProcess.MASTERDATA,
                StatusCode.STARTED,
                VitamLogbookMessages.getCodeOp(CONTEXTS_UPDATE_EVENT, StatusCode.STARTED),
                eip
            );
            logbookParameters.putParameterValue(
                LogbookParameterName.outcomeDetail,
                CONTEXTS_UPDATE_EVENT + "." + StatusCode.STARTED
            );
            if (null != id && !id.isEmpty()) {
                logbookParameters.putParameterValue(LogbookParameterName.objectIdentifier, id);
            }
            logbookClient.create(logbookParameters);
        }

        /**
         * log update success process
         *
         * @throws VitamException
         */
        private void logUpdateSuccess(String id, String evDetData) throws VitamException {
            final GUID eipUsage = GUIDFactory.newOperationLogbookGUID(ParameterHelper.getTenantParameter());
            final LogbookOperationParameters logbookParameters = LogbookParameterHelper.newLogbookOperationParameters(
                eipUsage,
                CONTEXTS_UPDATE_EVENT,
                eip,
                LogbookTypeProcess.MASTERDATA,
                StatusCode.OK,
                VitamLogbookMessages.getCodeOp(CONTEXTS_UPDATE_EVENT, StatusCode.OK),
                eip
            );

            if (null != id && !id.isEmpty()) {
                logbookParameters.putParameterValue(LogbookParameterName.objectIdentifier, id);
            }
            logbookParameters.putParameterValue(LogbookParameterName.eventDetailData, evDetData);
            logbookParameters.putParameterValue(
                LogbookParameterName.outcomeDetail,
                CONTEXTS_UPDATE_EVENT + "." + StatusCode.OK
            );
            logbookClient.update(logbookParameters);
        }

        /**
         * log delete start process
         *
         * @throws VitamException
         */
        private void logDeleteStarted(String id) throws VitamException {
            final LogbookOperationParameters logbookParameters = LogbookParameterHelper.newLogbookOperationParameters(
                eip,
                CONTEXTS_DELETE_EVENT,
                eip,
                LogbookTypeProcess.MASTERDATA,
                StatusCode.STARTED,
                VitamLogbookMessages.getCodeOp(CONTEXTS_DELETE_EVENT, StatusCode.STARTED),
                eip
            );
            logbookParameters.putParameterValue(
                LogbookParameterName.outcomeDetail,
                CONTEXTS_DELETE_EVENT + "." + StatusCode.STARTED
            );
            if (null != id && !id.isEmpty()) {
                logbookParameters.putParameterValue(LogbookParameterName.objectIdentifier, id);
            }
            logbookClient.create(logbookParameters);
        }

        /**
         * log delete success process
         *
         * @throws VitamException
         */
        private void logDeleteSuccess(String id) throws VitamException {
            final GUID eipUsage = GUIDFactory.newOperationLogbookGUID(ParameterHelper.getTenantParameter());
            final LogbookOperationParameters logbookParameters = LogbookParameterHelper.newLogbookOperationParameters(
                eipUsage,
                CONTEXTS_DELETE_EVENT,
                eip,
                LogbookTypeProcess.MASTERDATA,
                StatusCode.OK,
                VitamLogbookMessages.getCodeOp(CONTEXTS_DELETE_EVENT, StatusCode.OK),
                eip
            );

            if (null != id && !id.isEmpty()) {
                logbookParameters.putParameterValue(LogbookParameterName.objectIdentifier, id);
            }
            logbookParameters.putParameterValue(
                LogbookParameterName.outcomeDetail,
                CONTEXTS_DELETE_EVENT + "." + StatusCode.OK
            );
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
            final LogbookOperationParameters logbookParameters = LogbookParameterHelper.newLogbookOperationParameters(
                eipUsage,
                CONTEXTS_IMPORT_EVENT,
                eip,
                LogbookTypeProcess.MASTERDATA,
                StatusCode.FATAL,
                VitamLogbookMessages.getCodeOp(CONTEXTS_IMPORT_EVENT, StatusCode.FATAL),
                eip
            );
            logbookMessageError(errorsDetails, logbookParameters);
            logbookClient.update(logbookParameters);
        }

        private void logValidationError(String errorsDetails, String action, String KOEventType) throws VitamException {
            LOGGER.error("There validation errors on the input file {}", errorsDetails);
            final GUID eipUsage = GUIDFactory.newOperationLogbookGUID(ParameterHelper.getTenantParameter());
            final LogbookOperationParameters logbookParameters = LogbookParameterHelper.newLogbookOperationParameters(
                eipUsage,
                action,
                eip,
                LogbookTypeProcess.MASTERDATA,
                StatusCode.KO,
                VitamLogbookMessages.getFromFullCodeKey(KOEventType),
                eip
            );
            logbookParameters.putParameterValue(LogbookParameterName.outcomeDetail, KOEventType);
            logbookMessageError(errorsDetails, logbookParameters, KOEventType);
            logbookClient.update(logbookParameters);
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

        private void logbookMessageError(
            String errorsDetails,
            LogbookOperationParameters logbookParameters,
            String KOEventType
        ) {
            if (null != errorsDetails && !errorsDetails.isEmpty()) {
                try {
                    final ObjectNode object = JsonHandler.createObjectNode();
                    String evDetDataKey = "contextCheck";
                    switch (KOEventType) {
                        case EMPTY_REQUIRED_FIELD:
                            evDetDataKey = "Mandatory fields";
                            break;
                        case SECURITY_PROFILE_NOT_FOUND:
                            evDetDataKey = "Security profile not found";
                            break;
                        case DUPLICATE_IN_DATABASE:
                            evDetDataKey = "Duplicate field";
                            break;
                        case UNKNOWN_VALUE:
                            evDetDataKey = "Incorrect field";
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
         * Validate that context have not a missing mandatory parameter
         *
         * @return
         */
        private ContextValidator createMandatoryParamsValidator() {
            return context -> {
                List<ContextRejectionCause> validationErrors = new ArrayList<>();
                if (StringUtils.isBlank(context.getName())) {
                    validationErrors.add(ContextValidator.ContextRejectionCause.rejectMandatoryMissing(Context.NAME));
                }

                if (StringUtils.isBlank(context.getSecurityProfileIdentifier())) {
                    validationErrors.add(
                        ContextValidator.ContextRejectionCause.rejectMandatoryMissing(Context.SECURITY_PROFILE)
                    );
                }

                if (context.getStatus() == null) {
                    context.setStatus(ContextStatus.INACTIVE);
                }

                return validationErrors;
            };
        }

        /**
         * Validate that context have not a missing mandatory parameter
         *
         * @return
         */
        private ContextValidator securityProfileIdentifierValidator() {
            return context -> {
                Optional<SecurityProfileModel> securityProfileModel = securityProfileService.findOneByIdentifier(
                    context.getSecurityProfileIdentifier()
                );

                if (securityProfileModel.isEmpty()) {
                    return Collections.singletonList(
                        ContextValidator.ContextRejectionCause.invalidSecurityProfile(
                            context.getSecurityProfileIdentifier()
                        )
                    );
                } else {
                    // OK
                    return Collections.emptyList();
                }
            };
        }

        /**
         * Check if the context the same name already exists in database
         *
         * @return
         */
        private ContextValidator createCheckDuplicateInDatabaseValidator() {
            return context -> {
                if (ParametersChecker.isNotEmpty(context.getIdentifier())) {
                    final Bson clause = eq(Context.IDENTIFIER, context.getIdentifier());
                    final boolean exist = FunctionalAdminCollections.CONTEXT.getCollection().countDocuments(clause) > 0;
                    if (exist) {
                        return Collections.singletonList(
                            ContextValidator.ContextRejectionCause.rejectDuplicatedInDatabase(context.getIdentifier())
                        );
                    }
                }
                return Collections.emptyList();
            };
        }

        /**
         * Check if the ingest contract and access contract exist
         *
         * @return
         */
        private ContextValidator checkContract() {
            return context -> {
                List<ContextRejectionCause> validationErrors = new ArrayList<>();
                final List<PermissionModel> pmList = context.getPermissions();
                for (final PermissionModel pm : pmList) {
                    if (pm.getTenant() == null) {
                        validationErrors.add(ContextValidator.ContextRejectionCause.rejectNullTenant());
                    } else {
                        final int tenant = pm.getTenant();
                        final Set<String> icList = pm.getIngestContract();
                        for (final String ic : icList) {
                            if (!checkIdentifierOfIngestContract(ic, tenant)) {
                                validationErrors.add(
                                    ContextValidator.ContextRejectionCause.rejectNoExistanceOfIngestContract(ic, tenant)
                                );
                            }
                        }

                        final Set<String> acList = pm.getAccessContract();
                        for (final String ac : acList) {
                            if (!checkIdentifierOfAccessContract(ac, tenant)) {
                                validationErrors.add(
                                    ContextValidator.ContextRejectionCause.rejectNoExistanceOfAccessContract(ac, tenant)
                                );
                            }
                        }
                    }
                }

                return validationErrors;
            };
        }

        /**
         * Check if the tenant exist
         *
         * @return
         */
        private ContextValidator checkTenant() {
            return context -> {
                List<ContextRejectionCause> validationErrors = new ArrayList<>();
                final List<PermissionModel> pmList = context.getPermissions();
                for (final PermissionModel pm : pmList) {
                    if (pm.getTenant() == null) {
                        validationErrors.add(ContextValidator.ContextRejectionCause.rejectNullTenant());
                    } else {
                        final int tenant = pm.getTenant();
                        List<Integer> tenants = VitamConfiguration.getTenants();
                        if (!tenants.contains(tenant)) {
                            validationErrors.add(
                                ContextValidator.ContextRejectionCause.rejectNoExistanceOfTenant(tenant)
                            );
                        }
                    }
                }

                return validationErrors;
            };
        }

        public boolean checkIdentifierOfIngestContract(String ic, int tenant)
            throws ReferentialException, InvalidParseOperationException {
            int initialTenant = VitamThreadUtils.getVitamSession().getTenantId();
            try {
                VitamThreadUtils.getVitamSession().setTenantId(tenant);
                return (null != ingestContract.findByIdentifier(ic));
            } finally {
                VitamThreadUtils.getVitamSession().setTenantId(initialTenant);
            }
        }

        public boolean checkIdentifierOfAccessContract(String ac, int tenant)
            throws ReferentialException, InvalidParseOperationException {
            int initialTenant = VitamThreadUtils.getVitamSession().getTenantId();
            try {
                VitamThreadUtils.getVitamSession().setTenantId(tenant);
                return (null != accessContract.findByIdentifier(ac));
            } finally {
                VitamThreadUtils.getVitamSession().setTenantId(initialTenant);
            }
        }

        /**
         * Check if the Id of the context already exists in database
         *
         * @return
         */
        private ContextValidator checkEmptyIdentifierSlaveModeValidator() {
            return context -> {
                if (context.getIdentifier() == null || context.getIdentifier().isEmpty()) {
                    return Collections.singletonList(
                        ContextValidator.ContextRejectionCause.rejectMandatoryMissing(Context.IDENTIFIER)
                    );
                }
                return Collections.emptyList();
            };
        }
    }
}
