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
package fr.gouv.vitam.functional.administration.core.security.profile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Delete;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.parser.request.adapter.SingleVarNameAdapter;
import fr.gouv.vitam.common.database.parser.request.single.DeleteParserSingle;
import fr.gouv.vitam.common.database.parser.request.single.SelectParserSingle;
import fr.gouv.vitam.common.database.parser.request.single.UpdateParserSingle;
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
import fr.gouv.vitam.common.model.VitamAutoCloseable;
import fr.gouv.vitam.common.model.administration.SecurityProfileModel;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.Context;
import fr.gouv.vitam.functional.administration.common.SecurityProfile;
import fr.gouv.vitam.functional.administration.common.VitamErrorUtils;
import fr.gouv.vitam.functional.administration.common.counter.SequenceType;
import fr.gouv.vitam.functional.administration.common.counter.VitamCounterService;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.functional.administration.core.backup.FunctionalBackupService;
import fr.gouv.vitam.functional.administration.core.context.ContextService;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.utils.SecurityProfilePermissions;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import javax.ws.rs.core.Response;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class SecurityProfileService implements VitamAutoCloseable {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(SecurityProfileService.class);
    private static final String SECURITY_PROFILE_IS_MANDATORY_PARAMETER =
        "The collection of security profiles is mandatory";
    private static final String SECURITY_PROFILE_NOT_FOUND = "Security profile not found";
    private static final String ERR_ID_NOT_ALLOWED_IN_CREATE = "Id must be null when creating security profiles (%s)";

    private static final String ERR_MISSING_SECURITY_PROFILE_IDENTIFIER =
        "Identifier must not be null when creating security profiles (%s)";
    private static final String ERR_DUPLICATE_IDENTIFIER_IN_CREATE =
        "One or many security profiles in the imported list have the same identifier : %s";

    private static final String ERR_MISSING_SECURITY_PROFILE_NAME = "Security profile name is mandatory (%s)";

    private static final String ERR_UNEXPECTED_PERMISSION_SET_WITH_FULL_ACCESS =
        "Permission set cannot be set with full access mode : %s";

    private static final String SECURITY_PROFILE_IMPORT_EVENT = "STP_IMPORT_SECURITY_PROFILE";
    private static final String SECURITY_PROFILE_UPDATE_EVENT = "STP_UPDATE_SECURITY_PROFILE";
    private static final String SECURITY_PROFILE_DELETE_EVENT = "STP_DELETE_SECURITY_PROFILE";
    private static final String SECURITY_PROFILE_BACKUP_EVENT = "STP_BACKUP_SECURITY_PROFILE";
    private static final String IMPORT_SECURITY_PROFILE_UNKNOW_PERMISSION_KEY =
        "STP_IMPORT_SECURITY_PROFILE.UNKNOWN_PERMISSION";
    private static final String UPDATE_SECURITY_PROFILE_UNKNOW_PERMISSION_KEY =
        "STP_UPDATE_SECURITY_PROFILE.UNKNOWN_PERMISSION";
    private static final String UNKNOW_PERMISSION_SECURITY_PROFILE_ERROR =
        "At least, one security pofile's permission is not valid";
    private static final String _ID = "_id";
    private final MongoDbAccessAdminImpl mongoAccess;
    private final LogbookOperationsClient logbookClient;
    private final VitamCounterService vitamCounterService;
    private final FunctionalBackupService functionalBackupService;
    private ContextService contextService;

    /**
     * Constructor
     *
     * @param dbConfiguration
     * @param vitamCounterService
     * @param functionalBackupService
     */
    public SecurityProfileService(
        MongoDbAccessAdminImpl dbConfiguration,
        VitamCounterService vitamCounterService,
        FunctionalBackupService functionalBackupService
    ) {
        mongoAccess = dbConfiguration;
        this.vitamCounterService = vitamCounterService;
        this.functionalBackupService = functionalBackupService;
        logbookClient = LogbookOperationsClientFactory.getInstance().getClient();
    }

    public RequestResponse<SecurityProfileModel> createSecurityProfiles(List<SecurityProfileModel> securityProfileList)
        throws VitamException {
        ParametersChecker.checkParameter(SECURITY_PROFILE_IS_MANDATORY_PARAMETER, securityProfileList);

        if (securityProfileList.isEmpty()) {
            return new RequestResponseOK<>();
        }

        String operationId = VitamThreadUtils.getVitamSession().getRequestId();
        GUID eip = GUIDReader.getGUID(operationId);

        SecurityProfileLogbookManager manager = new SecurityProfileLogbookManager(logbookClient, eip);
        manager.logImportStarted();

        final VitamError<SecurityProfileModel> error = getVitamError(
            VitamCode.SECURITY_PROFILE_VALIDATION_ERROR.getItem(),
            "Global create security profile error"
        ).setHttpCode(Response.Status.BAD_REQUEST.getStatusCode());

        try {
            validateSecurityProfilesToInsert(securityProfileList, error);

            if (null != error.getErrors() && !error.getErrors().isEmpty()) {
                // log book + application log
                // stop
                String errorsDetails = error
                    .getErrors()
                    .stream()
                    .map(VitamError::getMessage)
                    .collect(Collectors.joining(","));
                manager.logValidationError(errorsDetails, SECURITY_PROFILE_IMPORT_EVENT);
                return error;
            }

            boolean slaveMode = isSlaveMode();
            for (SecurityProfileModel securityProfile : securityProfileList) {
                securityProfile.setId(GUIDFactory.newContextGUID().getId());

                if (!slaveMode) {
                    String code = vitamCounterService.getNextSequenceAsString(
                        ParameterHelper.getTenantParameter(),
                        SequenceType.SECURITY_PROFILE_SEQUENCE
                    );
                    securityProfile.setIdentifier(code);
                }
            }

            ArrayNode securityProfilesToPersist = JsonHandler.createArrayNode();
            for (SecurityProfileModel securityProfile : securityProfileList) {
                ObjectNode securityProfileNode = (ObjectNode) JsonHandler.toJsonNode(securityProfile);

                JsonNode jsonNode = securityProfileNode.remove(VitamFieldsHelper.id());
                if (jsonNode != null) {
                    securityProfileNode.set(_ID, jsonNode);
                }
                securityProfilesToPersist.add(securityProfileNode);
            }
            mongoAccess.insertDocuments(securityProfilesToPersist, FunctionalAdminCollections.SECURITY_PROFILE).close();

            functionalBackupService.saveCollectionAndSequence(
                eip,
                SECURITY_PROFILE_BACKUP_EVENT,
                FunctionalAdminCollections.SECURITY_PROFILE,
                eip.toString()
            );

            manager.logImportSuccess();

            return new RequestResponseOK<SecurityProfileModel>()
                .addAllResults(securityProfileList)
                .setHttpCode(Response.Status.CREATED.getStatusCode());
        } catch (final Exception exp) {
            LOGGER.error(exp);
            final String err = "Security profile import failed > " + exp.getMessage();
            manager.logFatalError(err, SECURITY_PROFILE_IMPORT_EVENT);
            return error
                .setCode(VitamCode.GLOBAL_INTERNAL_SERVER_ERROR.getItem())
                .setDescription(err)
                .setHttpCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        }
    }

    private void validateSecurityProfilesToInsert(
        List<SecurityProfileModel> securityProfileList,
        VitamError<SecurityProfileModel> error
    ) {
        boolean slaveMode = isSlaveMode();

        final Set<String> securityProfileIdentifiers = new HashSet<>();

        for (SecurityProfileModel securityProfile : securityProfileList) {
            // Security profile id should be null
            if (null != securityProfile.getId()) {
                error.addToErrors(
                    getVitamError(
                        VitamCode.SECURITY_PROFILE_VALIDATION_ERROR.getItem(),
                        String.format(ERR_ID_NOT_ALLOWED_IN_CREATE, securityProfile.getName())
                    )
                );
                continue;
            }

            // In slave mode (app provided identifier), security profile identifier should be unique and not null
            if (slaveMode) {
                // if a security profile have an identifier
                if (StringUtils.isEmpty(securityProfile.getIdentifier())) {
                    error.addToErrors(
                        getVitamError(
                            VitamCode.SECURITY_PROFILE_VALIDATION_ERROR.getItem(),
                            String.format(ERR_MISSING_SECURITY_PROFILE_IDENTIFIER, securityProfile.getName())
                        )
                    );
                    continue;
                }

                if (securityProfileIdentifiers.contains(securityProfile.getIdentifier())) {
                    error.addToErrors(
                        getVitamError(
                            VitamCode.SECURITY_PROFILE_VALIDATION_ERROR.getItem(),
                            String.format(ERR_DUPLICATE_IDENTIFIER_IN_CREATE, securityProfile.getIdentifier())
                        )
                    );
                    continue;
                }

                // mark the current security profile identifier as treated
                securityProfileIdentifiers.add(securityProfile.getIdentifier());
            }

            // Missing security profile name
            if (StringUtils.isEmpty(securityProfile.getName())) {
                error.addToErrors(
                    getVitamError(
                        VitamCode.SECURITY_PROFILE_VALIDATION_ERROR.getItem(),
                        String.format(ERR_MISSING_SECURITY_PROFILE_NAME, securityProfile.getName())
                    )
                );
                continue;
            }

            if (securityProfile.getFullAccess()) {
                // Permission set incompatible with full access mode
                if (!CollectionUtils.isEmpty(securityProfile.getPermissions())) {
                    error.addToErrors(
                        getVitamError(
                            VitamCode.SECURITY_PROFILE_VALIDATION_ERROR.getItem(),
                            String.format(ERR_UNEXPECTED_PERMISSION_SET_WITH_FULL_ACCESS, securityProfile.getName())
                        )
                    );
                    continue;
                }
            }
            // check Permissions validity
            if (securityProfile.getPermissions() != null && !securityProfile.getPermissions().isEmpty()) {
                checkSecurityProfilePermissions(
                    securityProfile.getPermissions(),
                    error,
                    IMPORT_SECURITY_PROFILE_UNKNOW_PERMISSION_KEY
                );
            }
        }
    }

    private void checkSecurityProfilePermissions(
        Set<String> permissions,
        VitamError<SecurityProfileModel> error,
        String messageKey
    ) {
        boolean isPermissionsUnvalid = permissions
            .stream()
            .map(SecurityProfilePermissions::isPermissionValid)
            .anyMatch(isPermissionValid -> isPermissionValid.equals(Boolean.FALSE));
        if (isPermissionsUnvalid) {
            error.addToErrors(
                getVitamErrorWithMessage(
                    VitamCode.SECURITY_PROFILE_VALIDATION_ERROR.getItem(),
                    VitamLogbookMessages.getCodeOp(messageKey, StatusCode.KO)
                )
            );
        }
    }

    public Optional<SecurityProfileModel> findOneByIdentifier(String identifier)
        throws ReferentialException, InvalidParseOperationException {
        final SelectParserSingle parser = new SelectParserSingle(new SingleVarNameAdapter());
        parser.parse(new Select().getFinalSelect());
        try {
            parser.addCondition(QueryHelper.eq("Identifier", identifier));
        } catch (InvalidCreateOperationException e) {
            throw new ReferentialException(e);
        }

        try (
            DbRequestResult result = mongoAccess.findDocuments(
                parser.getRequest().getFinalSelect(),
                FunctionalAdminCollections.SECURITY_PROFILE
            )
        ) {
            final List<SecurityProfileModel> list = result.getDocuments(
                SecurityProfile.class,
                SecurityProfileModel.class
            );
            if (list.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(list.get(0));
        }
    }

    public RequestResponseOK<SecurityProfileModel> findSecurityProfiles(JsonNode queryDsl)
        throws ReferentialException, InvalidParseOperationException {
        try (
            DbRequestResult result = mongoAccess.findDocuments(queryDsl, FunctionalAdminCollections.SECURITY_PROFILE)
        ) {
            return result.getRequestResponseOK(queryDsl, SecurityProfile.class, SecurityProfileModel.class);
        }
    }

    public RequestResponse<SecurityProfileModel> updateSecurityProfile(String identifier, JsonNode queryDsl)
        throws VitamException {
        VitamError<SecurityProfileModel> error = getVitamError(
            VitamCode.SECURITY_PROFILE_VALIDATION_ERROR.getItem(),
            "Update security profile error"
        ).setHttpCode(Response.Status.BAD_REQUEST.getStatusCode());

        if (queryDsl == null || !queryDsl.isObject()) {
            return error;
        }

        final Optional<SecurityProfileModel> securityProfileModelOps = findOneByIdentifier(identifier);
        if (securityProfileModelOps.isEmpty()) {
            error.setHttpCode(Response.Status.NOT_FOUND.getStatusCode());
            return error.addToErrors(
                getVitamError(
                    VitamCode.SECURITY_PROFILE_VALIDATION_ERROR.getItem(),
                    SECURITY_PROFILE_NOT_FOUND + identifier
                )
            );
        }
        SecurityProfileModel securityProfileModel = securityProfileModelOps.get();

        String operationId = VitamThreadUtils.getVitamSession().getRequestId();
        GUID eip = GUIDReader.getGUID(operationId);

        SecurityProfileLogbookManager manager = new SecurityProfileLogbookManager(logbookClient, eip);

        manager.logUpdateStarted(securityProfileModel.getId());

        try {
            // Check permission validity
            Set<String> permissions = new HashSet<>();
            ArrayNode permissionsNode = (ArrayNode) queryDsl.findValue(Context.PERMISSION);
            if (null != permissionsNode && !permissionsNode.isEmpty()) {
                permissions = JsonHandler.getFromJsonNode(permissionsNode, Set.class, String.class);
            }
            checkSecurityProfilePermissions(permissions, error, UPDATE_SECURITY_PROFILE_UNKNOW_PERMISSION_KEY);
            if (error.getErrors().size() > 0) {
                error.setHttpCode(Response.Status.BAD_REQUEST.getStatusCode());
                String errorsDetails = error
                    .getErrors()
                    .stream()
                    .map(VitamError::getMessage)
                    .collect(Collectors.joining(","));
                manager.logValidationError(errorsDetails, SECURITY_PROFILE_UPDATE_EVENT);
                return error;
            }

            JsonNode finalUpdate = enforceIdentifierInUpdateQuery(identifier, queryDsl);

            DbRequestResult result = mongoAccess.updateData(finalUpdate, FunctionalAdminCollections.SECURITY_PROFILE);
            Map<String, List<String>> updateDiffs = result.getDiffs();
            result.close();

            functionalBackupService.saveCollectionAndSequence(
                eip,
                SECURITY_PROFILE_BACKUP_EVENT,
                FunctionalAdminCollections.SECURITY_PROFILE,
                identifier
            );

            manager.logUpdateSuccess(securityProfileModel.getId(), updateDiffs.get(securityProfileModel.getId()));
            return new RequestResponseOK<>();
        } catch (SchemaValidationException | BadRequestException e) {
            LOGGER.error(e);
            final String err = "Security profile update failed > " + e.getMessage();
            manager.logValidationError(err, SECURITY_PROFILE_UPDATE_EVENT);
            error
                .setCode(VitamCode.SECURITY_PROFILE_VALIDATION_ERROR.getItem())
                .setDescription(err)
                .setHttpCode(Response.Status.BAD_REQUEST.getStatusCode());

            return error;
        } catch (VitamException | InvalidCreateOperationException e) {
            LOGGER.error(e);
            final String err = "Security profile update failed > " + e.getMessage();
            manager.logFatalError(err, SECURITY_PROFILE_UPDATE_EVENT);
            error
                .setCode(VitamCode.GLOBAL_INTERNAL_SERVER_ERROR.getItem())
                .setDescription(err)
                .setHttpCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());

            return error;
        }
    }

    private JsonNode enforceIdentifierInUpdateQuery(String identifier, JsonNode queryDsl)
        throws InvalidParseOperationException, InvalidCreateOperationException {
        final UpdateParserSingle parser = new UpdateParserSingle(
            FunctionalAdminCollections.SECURITY_PROFILE.getVarNameAdapater()
        );
        parser.parse(queryDsl);
        parser.addCondition(QueryHelper.eq(SecurityProfile.IDENTIFIER, identifier));
        return parser.getRequest().getFinalUpdate();
    }

    public RequestResponse<SecurityProfileModel> deleteSecurityProfile(String securityProfileId) throws VitamException {
        VitamError<SecurityProfileModel> error = getVitamError(
            VitamCode.SECURITY_PROFILE_VALIDATION_ERROR.getItem(),
            "Delete security profile error"
        ).setHttpCode(Response.Status.BAD_REQUEST.getStatusCode());

        String operationId = VitamThreadUtils.getVitamSession().getRequestId();
        GUID eip = GUIDReader.getGUID(operationId);

        SecurityProfileLogbookManager manager = new SecurityProfileLogbookManager(logbookClient, eip);

        manager.logDeleteStarted(securityProfileId);

        try {
            JsonNode finalDelete = enforceIdentifierInDeleteQuery(securityProfileId);

            if (!exist(finalDelete)) {
                manager.logValidationError(
                    "Security profile not found : " + securityProfileId,
                    SECURITY_PROFILE_DELETE_EVENT
                );
                return getVitamError(
                    VitamCode.SECURITY_PROFILE_VALIDATION_ERROR.getItem(),
                    "Delete context error : " + securityProfileId
                ).setHttpCode(Response.Status.NOT_FOUND.getStatusCode());
            }

            boolean isSecurityProfileUsed = contextService.securityProfileIsUsedInContexts(securityProfileId);
            if (isSecurityProfileUsed) {
                manager.logValidationError(
                    "Security profile is used : " + securityProfileId,
                    SECURITY_PROFILE_DELETE_EVENT
                );
                return getVitamError(
                    VitamCode.SECURITY_PROFILE_VALIDATION_ERROR.getItem(),
                    "Delete context error : " + securityProfileId
                ).setHttpCode(Response.Status.FORBIDDEN.getStatusCode());
            }

            DbRequestResult result = mongoAccess.deleteDocument(
                finalDelete,
                FunctionalAdminCollections.SECURITY_PROFILE
            );
            RequestResponseOK<SecurityProfileModel> response = new RequestResponseOK<>();
            response
                .addAllResults(result.getDocuments(SecurityProfile.class, SecurityProfileModel.class))
                .setTotal(result.getTotal())
                .setHttpCode(Response.Status.NO_CONTENT.getStatusCode());
            result.close();

            functionalBackupService.saveCollectionAndSequence(
                eip,
                SECURITY_PROFILE_BACKUP_EVENT,
                FunctionalAdminCollections.SECURITY_PROFILE,
                eip.toString()
            );

            manager.logDeleteSuccess(securityProfileId);

            return response;
        } catch (SchemaValidationException | BadRequestException e) {
            LOGGER.error(e);
            final String err = "Security profile delete failed > " + e.getMessage();
            manager.logValidationError(err, SECURITY_PROFILE_DELETE_EVENT);
            error
                .setCode(VitamCode.SECURITY_PROFILE_VALIDATION_ERROR.getItem())
                .setDescription(err)
                .setHttpCode(Response.Status.BAD_REQUEST.getStatusCode());

            return error;
        } catch (Exception e) {
            LOGGER.error(e);
            final String err = "Security profile delete failed > " + e.getMessage();
            manager.logFatalError(err, SECURITY_PROFILE_DELETE_EVENT);
            error
                .setCode(VitamCode.GLOBAL_INTERNAL_SERVER_ERROR.getItem())
                .setDescription(err)
                .setHttpCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());

            return error;
        }
    }

    private JsonNode enforceIdentifierInDeleteQuery(String identifier)
        throws InvalidParseOperationException, InvalidCreateOperationException {
        final DeleteParserSingle parser = new DeleteParserSingle(
            FunctionalAdminCollections.SECURITY_PROFILE.getVarNameAdapater()
        );
        parser.parse(new Delete().getFinalDelete());
        parser.addCondition(QueryHelper.eq(SecurityProfile.IDENTIFIER, identifier));
        return parser.getRequest().getFinalDelete();
    }

    private boolean exist(JsonNode finalSelect) throws InvalidParseOperationException, ReferentialException {
        DbRequestResult result = mongoAccess.findDocuments(finalSelect, FunctionalAdminCollections.SECURITY_PROFILE);
        final List<SecurityProfileModel> list = result.getDocuments(SecurityProfile.class, SecurityProfileModel.class);
        return !list.isEmpty();
    }

    @Override
    public void close() {
        logbookClient.close();
    }

    private boolean isSlaveMode() {
        return vitamCounterService.isSlaveFunctionnalCollectionOnTenant(
            SequenceType.SECURITY_PROFILE_SEQUENCE.getCollection(),
            ParameterHelper.getTenantParameter()
        );
    }

    private VitamError<SecurityProfileModel> getVitamError(String vitamCode, String error) {
        return VitamErrorUtils.getVitamError(
            vitamCode,
            error,
            "SecurityProfile",
            StatusCode.KO,
            SecurityProfileModel.class
        );
    }

    private VitamError<SecurityProfileModel> getVitamErrorWithMessage(String vitamCode, String msg) {
        return VitamErrorUtils.getVitamErrorWithMessage(
            vitamCode,
            SecurityProfileService.UNKNOW_PERMISSION_SECURITY_PROFILE_ERROR,
            "SecurityProfile",
            StatusCode.KO,
            msg,
            SecurityProfileModel.class
        );
    }

    public void setContextService(ContextService contextService) {
        this.contextService = contextService;
    }

    /**
     * Logbook manager for security profile operations
     */
    private static final class SecurityProfileLogbookManager {

        private static final String UPDATED_DIFFS = "updatedDiffs";
        private static final String SECURITY_PROFILE = "SecurityProfile";

        private final GUID eip;
        private final LogbookOperationsClient logbookClient;

        public SecurityProfileLogbookManager(LogbookOperationsClient logbookClient, GUID eip) {
            this.logbookClient = logbookClient;
            this.eip = eip;
        }

        /**
         * log start process
         *
         * @throws VitamException
         */
        private void logImportStarted() throws VitamException {
            final LogbookOperationParameters logbookParameters;
            logbookParameters = LogbookParameterHelper.newLogbookOperationParameters(
                eip,
                SECURITY_PROFILE_IMPORT_EVENT,
                eip,
                LogbookTypeProcess.MASTERDATA,
                StatusCode.STARTED,
                VitamLogbookMessages.getCodeOp(SECURITY_PROFILE_IMPORT_EVENT, StatusCode.STARTED),
                eip
            );
            logbookClient.create(logbookParameters);
        }

        /**
         * Log validation error (business error)
         *
         * @param errorsDetails
         * @param eventType
         */
        private void logValidationError(final String errorsDetails, String eventType) throws VitamException {
            LOGGER.error("There validation errors on the input file {}", errorsDetails);
            final GUID eipId = GUIDFactory.newOperationLogbookGUID(ParameterHelper.getTenantParameter());
            final LogbookOperationParameters logbookParameters = LogbookParameterHelper.newLogbookOperationParameters(
                eipId,
                eventType,
                eip,
                LogbookTypeProcess.MASTERDATA,
                StatusCode.KO,
                VitamLogbookMessages.getCodeOp(eventType, StatusCode.KO),
                eip
            );
            logbookMessageError(errorsDetails, logbookParameters);

            logbookClient.update(logbookParameters);
        }

        /**
         * log fatal error (system or technical error)
         *
         * @param errorsDetails
         * @throws VitamException
         */
        private void logFatalError(String errorsDetails, String eventCode) throws VitamException {
            LOGGER.error("There validation errors on the input file {}", errorsDetails);
            final GUID eipId = GUIDFactory.newOperationLogbookGUID(ParameterHelper.getTenantParameter());
            final LogbookOperationParameters logbookParameters = LogbookParameterHelper.newLogbookOperationParameters(
                eipId,
                eventCode,
                eip,
                LogbookTypeProcess.MASTERDATA,
                StatusCode.FATAL,
                VitamLogbookMessages.getCodeOp(eventCode, StatusCode.FATAL),
                eip
            );
            logbookMessageError(errorsDetails, logbookParameters);

            logbookClient.update(logbookParameters);
        }

        private void logbookMessageError(String errorsDetails, LogbookOperationParameters logbookParameters) {
            if (null != errorsDetails && !errorsDetails.isEmpty()) {
                try {
                    final ObjectNode object = JsonHandler.createObjectNode();
                    object.put("errors", errorsDetails);

                    final String wellFormedJson = SanityChecker.sanitizeJson(object);
                    logbookParameters.putParameterValue(LogbookParameterName.eventDetailData, wellFormedJson);
                } catch (final InvalidParseOperationException e) {
                    // Do nothing
                }
            }
        }

        /**
         * log end success process
         *
         * @throws VitamException
         */
        private void logImportSuccess() throws VitamException {
            final GUID eipId = GUIDFactory.newOperationLogbookGUID(ParameterHelper.getTenantParameter());
            final LogbookOperationParameters logbookParameters = LogbookParameterHelper.newLogbookOperationParameters(
                eipId,
                SECURITY_PROFILE_IMPORT_EVENT,
                eip,
                LogbookTypeProcess.MASTERDATA,
                StatusCode.OK,
                VitamLogbookMessages.getCodeOp(SECURITY_PROFILE_IMPORT_EVENT, StatusCode.OK),
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
                SECURITY_PROFILE_UPDATE_EVENT,
                eip,
                LogbookTypeProcess.MASTERDATA,
                StatusCode.STARTED,
                VitamLogbookMessages.getCodeOp(SECURITY_PROFILE_UPDATE_EVENT, StatusCode.STARTED),
                eip
            );
            logbookParameters.putParameterValue(
                LogbookParameterName.outcomeDetail,
                SECURITY_PROFILE_UPDATE_EVENT + "." + StatusCode.STARTED
            );
            if (null != id && !id.isEmpty()) {
                logbookParameters.putParameterValue(LogbookParameterName.objectIdentifier, id);
            }
            logbookClient.create(logbookParameters);
        }

        private void logUpdateSuccess(String id, List<String> listDiffs) throws VitamException {
            final ObjectNode evDetData = JsonHandler.createObjectNode();
            final String diffs = listDiffs.stream().reduce("", String::concat);

            final ObjectNode msg = JsonHandler.createObjectNode();
            msg.put(UPDATED_DIFFS, diffs);

            evDetData.set(SECURITY_PROFILE, msg);

            final GUID eipId = GUIDFactory.newOperationLogbookGUID(ParameterHelper.getTenantParameter());

            final String wellFormedJson = SanityChecker.sanitizeJson(evDetData);
            final LogbookOperationParameters logbookParameters = LogbookParameterHelper.newLogbookOperationParameters(
                eipId,
                SECURITY_PROFILE_UPDATE_EVENT,
                eip,
                LogbookTypeProcess.MASTERDATA,
                StatusCode.OK,
                VitamLogbookMessages.getCodeOp(SECURITY_PROFILE_UPDATE_EVENT, StatusCode.OK),
                eip
            );
            if (null != id && !id.isEmpty()) {
                logbookParameters.putParameterValue(LogbookParameterName.objectIdentifier, id);
            }
            logbookParameters.putParameterValue(LogbookParameterName.eventDetailData, wellFormedJson);
            logbookParameters.putParameterValue(
                LogbookParameterName.outcomeDetail,
                SECURITY_PROFILE_UPDATE_EVENT + "." + StatusCode.OK
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
                SECURITY_PROFILE_DELETE_EVENT,
                eip,
                LogbookTypeProcess.MASTERDATA,
                StatusCode.STARTED,
                VitamLogbookMessages.getCodeOp(SECURITY_PROFILE_DELETE_EVENT, StatusCode.STARTED),
                eip
            );
            logbookParameters.putParameterValue(
                LogbookParameterName.outcomeDetail,
                SECURITY_PROFILE_DELETE_EVENT + "." + StatusCode.STARTED
            );
            if (null != id && !id.isEmpty()) {
                logbookParameters.putParameterValue(LogbookParameterName.objectIdentifier, id);
            }
            logbookClient.create(logbookParameters);
        }

        /**
         * log delete sucess process
         *
         * @throws VitamException
         */
        private void logDeleteSuccess(String id) throws VitamException {
            final ObjectNode evDetData = JsonHandler.createObjectNode();

            final GUID eipId = GUIDFactory.newOperationLogbookGUID(ParameterHelper.getTenantParameter());

            final String wellFormedJson = SanityChecker.sanitizeJson(evDetData);
            final LogbookOperationParameters logbookParameters = LogbookParameterHelper.newLogbookOperationParameters(
                eipId,
                SECURITY_PROFILE_DELETE_EVENT,
                eip,
                LogbookTypeProcess.MASTERDATA,
                StatusCode.OK,
                VitamLogbookMessages.getCodeOp(SECURITY_PROFILE_DELETE_EVENT, StatusCode.OK),
                eip
            );
            if (null != id && !id.isEmpty()) {
                logbookParameters.putParameterValue(LogbookParameterName.objectIdentifier, id);
            }
            logbookParameters.putParameterValue(LogbookParameterName.eventDetailData, wellFormedJson);
            logbookParameters.putParameterValue(
                LogbookParameterName.outcomeDetail,
                SECURITY_PROFILE_DELETE_EVENT + "." + StatusCode.OK
            );
            logbookClient.update(logbookParameters);
        }
    }
}
