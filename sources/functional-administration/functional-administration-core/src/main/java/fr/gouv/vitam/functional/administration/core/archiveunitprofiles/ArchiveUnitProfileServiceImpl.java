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
package fr.gouv.vitam.functional.administration.core.archiveunitprofiles;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.parser.request.adapter.SingleVarNameAdapter;
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
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.json.SchemaValidationUtils;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.ArchiveUnitProfileModel;
import fr.gouv.vitam.common.model.administration.ArchiveUnitProfileStatus;
import fr.gouv.vitam.common.model.administration.OntologyModel;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.ArchiveUnitProfile;
import fr.gouv.vitam.functional.administration.common.Ontology;
import fr.gouv.vitam.functional.administration.common.VitamErrorUtils;
import fr.gouv.vitam.functional.administration.common.counter.SequenceType;
import fr.gouv.vitam.functional.administration.common.counter.VitamCounterService;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.functional.administration.core.backup.FunctionalBackupService;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;

import javax.ws.rs.core.Response;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;

/**
 * The implementation of the archive unit profile CRUD
 */
public class ArchiveUnitProfileServiceImpl implements ArchiveUnitProfileService {

    public static final String ARCHIVE_UNIT_PROFILE_IDENTIFIER_ALREADY_EXISTS_IN_DATABASE =
        "Archive unit profile identifier already exists in database ";
    public static final String ARCHIVE_UNIT_PROFILE_IDENTIFIER_MUST_BE_STRING =
        "Archive unit profile identifier must be a string ";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ArchiveUnitProfileServiceImpl.class);
    private static final String ARCHIVE_UNIT_PROFILE_IMPORT_EVENT = "IMPORT_ARCHIVEUNITPROFILE";
    private static final String ARCHIVE_UNIT_PROFILE_BACKUP_EVENT = "BACKUP_ARCHIVEUNITPROFILE";
    private static final String ARCHIVE_UNIT_PROFILES_UPDATE_EVENT = "UPDATE_ARCHIVEUNITPROFILE";
    private static final String UPDATED_DIFFS = "updatedDiffs";
    private static final String ARCHIVE_UNIT_PROFILE_IS_MANDATORY_PARAMETER =
        "archive unit profiles parameter is mandatory";
    private static final String ARCHIVE_UNIT_PROFILE_NOT_FOUND = "Update a not found archive unit profile";
    private static final String ARCHIVE_UNIT_PROFILE_STATUS_MUST_BE_ACTIVE_OR_INACTIVE =
        "The archive unit profile status must be ACTIVE or INACTIVE but not ";
    private static final String UND_TENANT = "_tenant";
    private static final String UND_ID = "_id";
    private final MongoDbAccessAdminImpl mongoAccess;
    private final LogbookOperationsClient logbookClient;
    private final MetaDataClient metaDataClient;
    private final VitamCounterService vitamCounterService;
    private final FunctionalBackupService functionalBackupService;
    private boolean checkOntology = true;

    /**
     * Constructor
     *
     * @param mongoAccess MongoDB client
     * @param vitamCounterService the vitam counter service
     * @param functionalBackupService the functional backup service
     */
    public ArchiveUnitProfileServiceImpl(
        MongoDbAccessAdminImpl mongoAccess,
        VitamCounterService vitamCounterService,
        FunctionalBackupService functionalBackupService
    ) {
        this.mongoAccess = mongoAccess;
        this.vitamCounterService = vitamCounterService;
        logbookClient = LogbookOperationsClientFactory.getInstance().getClient();
        metaDataClient = MetaDataClientFactory.getInstance().getClient();
        this.functionalBackupService = functionalBackupService;
    }

    /**
     * Constructor
     *
     * @param mongoAccess MongoDB client
     * @param vitamCounterService the vitam counter service
     * @param functionalBackupService the functional backup service
     * @param checkOntology true or false to determine if ontology check is required
     */
    @VisibleForTesting
    public ArchiveUnitProfileServiceImpl(
        MongoDbAccessAdminImpl mongoAccess,
        VitamCounterService vitamCounterService,
        FunctionalBackupService functionalBackupService,
        boolean checkOntology
    ) {
        this.mongoAccess = mongoAccess;
        this.vitamCounterService = vitamCounterService;
        logbookClient = LogbookOperationsClientFactory.getInstance().getClient();
        metaDataClient = MetaDataClientFactory.getInstance().getClient();
        this.functionalBackupService = functionalBackupService;
        this.checkOntology = checkOntology;
    }

    @Override
    public RequestResponse<ArchiveUnitProfileModel> createArchiveUnitProfiles(
        List<ArchiveUnitProfileModel> profileModelList
    ) throws VitamException {
        ParametersChecker.checkParameter(ARCHIVE_UNIT_PROFILE_IS_MANDATORY_PARAMETER, profileModelList);

        if (profileModelList.isEmpty()) {
            return new RequestResponseOK<>();
        }

        String operationId = VitamThreadUtils.getVitamSession().getRequestId();
        GUID eip = GUIDReader.getGUID(operationId);

        boolean slaveMode = vitamCounterService.isSlaveFunctionnalCollectionOnTenant(
            SequenceType.ARCHIVE_UNIT_PROFILE_SEQUENCE.getCollection(),
            ParameterHelper.getTenantParameter()
        );

        ArchiveUnitProfileManager manager = new ArchiveUnitProfileManager(logbookClient, metaDataClient, eip);
        manager.logStarted(ARCHIVE_UNIT_PROFILE_IMPORT_EVENT, null);

        final Set<String> profileIdentifiers = new HashSet<>();
        ArrayNode archiveProfilesToPersist;

        final VitamError<ArchiveUnitProfileModel> error = getVitamError(
            VitamCode.ARCHIVE_UNIT_PROFILE_FILE_IMPORT_ERROR.getItem(),
            "Global create archive unit profile error"
        ).setHttpCode(Response.Status.BAD_REQUEST.getStatusCode());

        try {
            for (final ArchiveUnitProfileModel aupm : profileModelList) {
                // if a profile have and id
                if (null != aupm.getId()) {
                    error.addToErrors(
                        getVitamError(
                            VitamCode.ARCHIVE_UNIT_PROFILE_VALIDATION_ERROR.getItem(),
                            ArchiveUnitProfileValidator.RejectionCause.rejectIdNotAllowedInCreate(
                                aupm.getName()
                            ).getReason()
                        )
                    );
                    continue;
                }

                // if a profile with the same identifier is already treated mark the current one as duplicated
                if (ParametersChecker.isNotEmpty(aupm.getIdentifier())) {
                    if (profileIdentifiers.contains(aupm.getIdentifier())) {
                        error.addToErrors(
                            getVitamError(
                                VitamCode.ARCHIVE_UNIT_PROFILE_VALIDATION_ERROR.getItem(),
                                "Duplicate archive unit profiles (ID)"
                            ).setMessage(
                                "Archive unit profile identifier " +
                                aupm.getIdentifier() +
                                " already exists in the json"
                            )
                        );
                        continue;
                    } else {
                        profileIdentifiers.add(aupm.getIdentifier());
                    }
                }

                // validate profile
                if (manager.validateArchiveUnitProfile(aupm, error)) {
                    aupm.setId(GUIDFactory.newProfileGUID(ParameterHelper.getTenantParameter()).getId());
                }
                if (aupm.getTenant() == null) {
                    aupm.setTenant(ParameterHelper.getTenantParameter());
                }

                // Json schema validation of profile ControlSchema property
                final Optional<ArchiveUnitProfileValidator.RejectionCause> checkSchema = manager
                    .createJsonSchemaValidator()
                    .validate(aupm);

                checkSchema.ifPresent(t -> {
                    VitamError<ArchiveUnitProfileModel> vitamError = new VitamError<ArchiveUnitProfileModel>(
                        VitamCode.ARCHIVE_UNIT_PROFILE_VALIDATION_ERROR.getItem()
                    )
                        .setDescription(checkSchema.get().getReason())
                        .setMessage(ArchiveUnitProfileManager.INVALID_JSON_SCHEMA)
                        .setState(StatusCode.KO.name());

                    vitamError.setContext("FunctionalModule-ArchiveUnitProfile");
                    error.addToErrors(vitamError);
                });

                if (slaveMode) {
                    final Optional<ArchiveUnitProfileValidator.RejectionCause> result = manager
                        .checkEmptyIdentifierSlaveModeValidator()
                        .validate(aupm);

                    result.ifPresent(t -> {
                        VitamError<ArchiveUnitProfileModel> vitamError = new VitamError<ArchiveUnitProfileModel>(
                            VitamCode.ARCHIVE_UNIT_PROFILE_VALIDATION_ERROR.getItem()
                        )
                            .setMessage(ArchiveUnitProfileManager.EMPTY_REQUIRED_FIELD)
                            .setDescription(result.get().getReason())
                            .setState(StatusCode.KO.name());
                        vitamError.setContext("FunctionalModule-ArchiveUnitProfile");
                        error.addToErrors(vitamError);
                    });
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
                manager.logValidationError(
                    ARCHIVE_UNIT_PROFILE_IMPORT_EVENT,
                    null,
                    errorsDetails,
                    error.getErrors().get(0).getMessage()
                );
                return error;
            }

            archiveProfilesToPersist = JsonHandler.createArrayNode();
            final Map<String, OntologyModel> ontologyModelMap = getOntologyModelMap();

            for (final ArchiveUnitProfileModel aupm : profileModelList) {
                setIdentifier(slaveMode, aupm);

                if (aupm.getControlSchema() != null) {
                    List<String> schemaFields = SchemaValidationUtils.extractFieldsFromSchema(aupm.getControlSchema());
                    if (checkOntology) {
                        schemaFields.forEach(k -> validateFieldInSchemaAgainstOntology(ontologyModelMap, k, error));
                    }
                    aupm.setFields(schemaFields);
                }

                final ObjectNode archiveProfileNode = (ObjectNode) JsonHandler.toJsonNode(aupm);
                JsonNode jsonNode = archiveProfileNode.remove(VitamFieldsHelper.id());
                /* archive unit profile is valid, add it to the list to persist */

                if (jsonNode != null) {
                    archiveProfileNode.set(UND_ID, jsonNode);
                }
                JsonNode hashTenant = archiveProfileNode.remove(VitamFieldsHelper.tenant());
                if (hashTenant != null) {
                    archiveProfileNode.set(UND_TENANT, hashTenant);
                }
                archiveProfilesToPersist.add(archiveProfileNode);
            }

            if (null != error.getErrors() && !error.getErrors().isEmpty()) {
                // log book + application log
                // stop
                final String errorsDetails = error
                    .getErrors()
                    .stream()
                    .map(VitamError::getDescription)
                    .collect(Collectors.joining(","));
                manager.logValidationError(
                    ARCHIVE_UNIT_PROFILE_IMPORT_EVENT,
                    null,
                    errorsDetails,
                    error.getErrors().get(0).getMessage()
                );
                return error;
            }
            // at this point no exception occurred and no validation error detected
            // persist in collection
            // TODO: 3/28/17 create insertDocuments method that accepts VitamDocument instead of ArrayNode, so we can
            // use ArchiveUnitProfile at this point
            mongoAccess
                .insertDocuments(archiveProfilesToPersist, FunctionalAdminCollections.ARCHIVE_UNIT_PROFILE)
                .close();

            functionalBackupService.saveCollectionAndSequence(
                eip,
                ARCHIVE_UNIT_PROFILE_BACKUP_EVENT,
                FunctionalAdminCollections.ARCHIVE_UNIT_PROFILE,
                eip.toString()
            );
        } catch (SchemaValidationException e) {
            LOGGER.error(e);
            final String err = "Import archive unit profile error > " + e.getMessage();

            // logbook error event
            manager.logValidationError(
                ARCHIVE_UNIT_PROFILE_IMPORT_EVENT,
                null,
                err,
                ArchiveUnitProfileManager.IMPORT_KO
            );

            return getVitamError(VitamCode.ARCHIVE_UNIT_PROFILE_VALIDATION_ERROR.getItem(), e.getMessage()).setHttpCode(
                Response.Status.BAD_REQUEST.getStatusCode()
            );
        } catch (final Exception e) {
            final String err = "Import archive unit profiles error : " + e.getMessage();
            manager.logFatalError(ARCHIVE_UNIT_PROFILE_IMPORT_EVENT, null, err);
            return getVitamError(VitamCode.ARCHIVE_UNIT_PROFILE_FILE_IMPORT_ERROR.getItem(), err).setHttpCode(
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()
            );
        }

        manager.logSuccess(ARCHIVE_UNIT_PROFILE_IMPORT_EVENT, null, null);

        return new RequestResponseOK<ArchiveUnitProfileModel>()
            .addAllResults(profileModelList)
            .setHttpCode(Response.Status.CREATED.getStatusCode());
    }

    @Override
    public RequestResponseOK<ArchiveUnitProfileModel> findArchiveUnitProfiles(JsonNode queryDsl)
        throws ReferentialException, InvalidParseOperationException {
        try (
            DbRequestResult result = mongoAccess.findDocuments(
                queryDsl,
                FunctionalAdminCollections.ARCHIVE_UNIT_PROFILE
            )
        ) {
            return result.getRequestResponseOK(queryDsl, ArchiveUnitProfile.class, ArchiveUnitProfileModel.class);
        }
    }

    @Override
    public RequestResponse<ArchiveUnitProfileModel> updateArchiveUnitProfile(String id, JsonNode queryDsl)
        throws VitamException {
        final ArchiveUnitProfileModel profileModel = findByIdentifier(id);
        if (profileModel == null) {
            return getVitamError(
                VitamCode.ARCHIVE_UNIT_PROFILE_VALIDATION_ERROR.getItem(),
                ARCHIVE_UNIT_PROFILE_NOT_FOUND
            )
                .setHttpCode(Response.Status.NOT_FOUND.getStatusCode())
                .setMessage(ArchiveUnitProfileManager.UPDATE_AUP_NOT_FOUND);
        }

        String operationId = VitamThreadUtils.getVitamSession().getRequestId();
        GUID eip = GUIDReader.getGUID(operationId);
        final ArchiveUnitProfileManager manager = new ArchiveUnitProfileManager(logbookClient, metaDataClient, eip);
        Map<String, List<String>> updateDiffs;
        manager.logStarted(ARCHIVE_UNIT_PROFILES_UPDATE_EVENT, profileModel.getId());

        if (queryDsl == null || !queryDsl.isObject()) {
            manager.logValidationError(
                ARCHIVE_UNIT_PROFILES_UPDATE_EVENT,
                profileModel.getId(),
                "Update query dsl must be an object and not null",
                ArchiveUnitProfileManager.UPDATE_KO
            );
            return getVitamError(
                VitamCode.ARCHIVE_UNIT_PROFILE_VALIDATION_ERROR.getItem(),
                "Update query dsl must be an object and not null : " + profileModel.getIdentifier()
            )
                .setHttpCode(Response.Status.BAD_REQUEST.getStatusCode())
                .setMessage(ArchiveUnitProfileManager.UPDATE_KO);
        }

        final VitamError<ArchiveUnitProfileModel> error = getVitamError(
            VitamCode.ARCHIVE_UNIT_PROFILE_VALIDATION_ERROR.getItem(),
            "Update archive unit profile error : " + profileModel.getIdentifier()
        ).setHttpCode(Response.Status.BAD_REQUEST.getStatusCode());

        final JsonNode actionNode = queryDsl.get(BuilderToken.GLOBAL.ACTION.exactToken());
        boolean schemaUpdate = false;
        String newSchema = null;
        for (final JsonNode fieldToSet : actionNode) {
            final JsonNode fieldName = fieldToSet.get(BuilderToken.UPDATEACTION.SET.exactToken());
            if (fieldName != null) {
                final Iterator<String> it = fieldName.fieldNames();
                while (it.hasNext()) {
                    final String field = it.next();
                    final JsonNode value = fieldName.findValue(field);
                    if (ArchiveUnitProfileModel.CONTROLSCHEMA.equals(field)) {
                        schemaUpdate = true;
                        newSchema = value.asText();
                    }
                    validateUpdateAction(profileModel, error, field, value, manager);
                }

                ((ObjectNode) fieldName).remove(ArchiveUnitProfileModel.CREATION_DATE);
                ((ObjectNode) fieldName).put(ArchiveUnitProfileModel.LAST_UPDATE, LocalDateUtil.nowFormatted());
            }
        }

        if (schemaUpdate) {
            try {
                if (newSchema != null) {
                    final Map<String, OntologyModel> ontologyModelMap = getOntologyModelMap();

                    ObjectNode fieldsObjectNode = JsonHandler.createObjectNode();
                    ArrayNode fieldsNode = JsonHandler.createArrayNode();
                    //Get the list of fields in the json schema
                    List<String> schemaFields = SchemaValidationUtils.extractFieldsFromSchema(newSchema);
                    if (checkOntology) {
                        schemaFields.forEach(k -> {
                            validateFieldInSchemaAgainstOntology(ontologyModelMap, k, error);
                            fieldsNode.add(k);
                        });
                    }
                    fieldsObjectNode.set(ArchiveUnitProfileModel.FIELDS, fieldsNode);
                    ObjectNode setFields = JsonHandler.createObjectNode();
                    setFields.set(BuilderToken.UPDATEACTION.SET.exactToken(), fieldsObjectNode);
                    ((ArrayNode) actionNode).add(setFields);
                }
            } catch (InvalidParseOperationException e) {
                LOGGER.error(e);
                error.addToErrors(
                    new VitamError<ArchiveUnitProfileModel>(VitamCode.ARCHIVE_UNIT_PROFILE_VALIDATION_ERROR.getItem())
                        .setDescription("The archive unit profile name contains bad fields in its schema")
                        .setMessage(ArchiveUnitProfileManager.UPDATE_KO)
                );
            }
        }

        if (error.getErrors() != null && error.getErrors().size() > 0) {
            final String errorsDetails = error
                .getErrors()
                .stream()
                .map(VitamError::getDescription)
                .collect(Collectors.joining(","));
            manager.logValidationError(
                ARCHIVE_UNIT_PROFILES_UPDATE_EVENT,
                profileModel.getId(),
                errorsDetails,
                error.getErrors().get(0).getMessage()
            );

            return error;
        }

        String wellFormedJson = null;
        try {
            try (
                DbRequestResult result = mongoAccess.updateData(
                    queryDsl,
                    FunctionalAdminCollections.ARCHIVE_UNIT_PROFILE
                )
            ) {
                updateDiffs = result.getDiffs();
            } catch (SchemaValidationException | BadRequestException e) {
                LOGGER.error(e);
                final String err = "Update archive unit profile error > " + e.getMessage();

                // logbook error event
                manager.logValidationError(
                    ARCHIVE_UNIT_PROFILES_UPDATE_EVENT,
                    profileModel.getId(),
                    err,
                    ArchiveUnitProfileManager.UPDATE_KO
                );

                return getVitamError(
                    VitamCode.ARCHIVE_UNIT_PROFILE_VALIDATION_ERROR.getItem(),
                    e.getMessage()
                ).setHttpCode(Response.Status.BAD_REQUEST.getStatusCode());
            }

            List<String> diff = updateDiffs.get(profileModel.getId());
            try {
                final ObjectNode object = JsonHandler.createObjectNode();
                object.put(UPDATED_DIFFS, Joiner.on(" ").join(diff));
                wellFormedJson = SanityChecker.sanitizeJson(object);
            } catch (InvalidParseOperationException e) {
                // Do nothing
                LOGGER.info("Invalid parse Exception while sanitize updated diffs");
            }

            functionalBackupService.saveCollectionAndSequence(
                eip,
                ARCHIVE_UNIT_PROFILE_BACKUP_EVENT,
                FunctionalAdminCollections.ARCHIVE_UNIT_PROFILE,
                profileModel.getId()
            );
        } catch (final Exception e) {
            LOGGER.error(e);
            final String err = "Update archive unit profile error : " + e.getMessage();
            manager.logFatalError(ARCHIVE_UNIT_PROFILES_UPDATE_EVENT, profileModel.getId(), err);
            error
                .setCode(VitamCode.GLOBAL_INTERNAL_SERVER_ERROR.getItem())
                .setDescription(err)
                .setHttpCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());

            return error;
        }

        manager.logSuccess(ARCHIVE_UNIT_PROFILES_UPDATE_EVENT, profileModel.getId(), wellFormedJson);
        return new RequestResponseOK<ArchiveUnitProfileModel>().setHttpCode(Response.Status.OK.getStatusCode());
    }

    @Override
    public void close() {
        if (null != logbookClient) {
            logbookClient.close();
        }
    }

    @VisibleForTesting
    public ArchiveUnitProfileModel findByIdentifier(String id)
        throws ReferentialException, InvalidParseOperationException {
        SanityChecker.checkParameter(id);
        final SelectParserSingle parser = new SelectParserSingle(new SingleVarNameAdapter());
        parser.parse(new Select().getFinalSelect());
        try {
            parser.addCondition(eq(ArchiveUnitProfile.IDENTIFIER, id));
        } catch (final InvalidCreateOperationException e) {
            throw new ReferentialException(e);
        }

        try (
            DbRequestResult result = mongoAccess.findDocuments(
                parser.getRequest().getFinalSelect(),
                FunctionalAdminCollections.ARCHIVE_UNIT_PROFILE
            )
        ) {
            final List<ArchiveUnitProfileModel> list = result.getDocuments(
                ArchiveUnitProfile.class,
                ArchiveUnitProfileModel.class
            );
            if (list.isEmpty()) {
                return null;
            }
            return list.get(0);
        }
    }

    /**
     * Validate dsl action
     *
     * @param profileModel
     * @param error thrown errors
     * @param field the field to check
     * @param value the calue to check
     */
    private void validateUpdateAction(
        ArchiveUnitProfileModel profileModel,
        final VitamError<ArchiveUnitProfileModel> error,
        final String field,
        final JsonNode value,
        ArchiveUnitProfileManager manager
    ) {
        if (ArchiveUnitProfile.STATUS.equals(field)) {
            if (
                !(ArchiveUnitProfileStatus.ACTIVE.name().equals(value.asText()) ||
                    ArchiveUnitProfileStatus.INACTIVE.name().equals(value.asText()))
            ) {
                error.addToErrors(
                    getVitamError(
                        VitamCode.ARCHIVE_UNIT_PROFILE_VALIDATION_ERROR.getItem(),
                        ARCHIVE_UNIT_PROFILE_STATUS_MUST_BE_ACTIVE_OR_INACTIVE + value.asText()
                    )
                        .setHttpCode(Response.Status.BAD_REQUEST.getStatusCode())
                        .setMessage(ArchiveUnitProfileManager.UPDATE_VALUE_NOT_IN_ENUM)
                );
            }
        }

        if (ArchiveUnitProfileModel.CONTROLSCHEMA.equals(field)) {
            final Optional<ArchiveUnitProfileValidator.RejectionCause> checkSchema = manager
                .createJsonSchemaValidator()
                .validate(new ArchiveUnitProfileModel().setControlSchema(value.asText()));
            checkSchema.ifPresent(
                t ->
                    error.addToErrors(
                        new VitamError<ArchiveUnitProfileModel>(
                            VitamCode.ARCHIVE_UNIT_PROFILE_VALIDATION_ERROR.getItem()
                        )
                            .setDescription(checkSchema.get().getReason())
                            .setMessage(ArchiveUnitProfileManager.UPDATE_KO)
                    )
            );
        }

        if (ArchiveUnitProfileModel.TAG_IDENTIFIER.equals(field)) {
            if (!value.isTextual()) {
                error.addToErrors(
                    getVitamError(
                        VitamCode.ARCHIVE_UNIT_PROFILE_VALIDATION_ERROR.getItem(),
                        ARCHIVE_UNIT_PROFILE_IDENTIFIER_MUST_BE_STRING + " : " + value.asText()
                    )
                        .setHttpCode(Response.Status.BAD_REQUEST.getStatusCode())
                        .setMessage(ArchiveUnitProfileManager.UPDATE_KO)
                );
            } else if (!profileModel.getIdentifier().equals(value.asText())) {
                Optional<ArchiveUnitProfileValidator.RejectionCause> validateIdentifier = manager
                    .createCheckDuplicateInDatabaseValidator()
                    .validate(new ArchiveUnitProfileModel().setIdentifier(value.asText()));
                if (validateIdentifier.isPresent()) {
                    error.addToErrors(
                        getVitamError(
                            VitamCode.ARCHIVE_UNIT_PROFILE_VALIDATION_ERROR.getItem(),
                            ARCHIVE_UNIT_PROFILE_IDENTIFIER_ALREADY_EXISTS_IN_DATABASE + " : " + value.asText()
                        )
                            .setHttpCode(Response.Status.BAD_REQUEST.getStatusCode())
                            .setMessage(ArchiveUnitProfileManager.UPDATE_DUPLICATE_IN_DATABASE)
                    );
                }
            }
        }

        if (ArchiveUnitProfileModel.CONTROLSCHEMA.equals(field)) {
            // Json schema validation of profileModel ControlSchema property
            final Optional<ArchiveUnitProfileValidator.RejectionCause> checkSchema = manager
                .createJsonSchemaValidator()
                .validate(profileModel);
            checkSchema.ifPresent(
                t ->
                    error.addToErrors(
                        new VitamError<ArchiveUnitProfileModel>(
                            VitamCode.ARCHIVE_UNIT_PROFILE_VALIDATION_ERROR.getItem()
                        )
                            .setDescription(checkSchema.get().getReason())
                            .setMessage(ArchiveUnitProfileManager.UPDATE_KO)
                    )
            );

            // check if the archiveUnitProfile is used by an archiveUnit
            final Optional<ArchiveUnitProfileValidator.RejectionCause> checkUsedJsonSchema = manager
                .createCheckUsedJsonSchema()
                .validate(profileModel);
            checkUsedJsonSchema.ifPresent(
                t ->
                    error.addToErrors(
                        new VitamError<ArchiveUnitProfileModel>(
                            VitamCode.ARCHIVE_UNIT_PROFILE_VALIDATION_ERROR.getItem()
                        )
                            .setDescription(checkUsedJsonSchema.get().getReason())
                            .setMessage(ArchiveUnitProfileManager.UPDATE_KO)
                    )
            );
        }
    }

    private void setIdentifier(boolean slaveMode, ArchiveUnitProfileModel archiveUnitProfilModel)
        throws ReferentialException {
        if (!slaveMode) {
            String code = vitamCounterService.getNextSequenceAsString(
                ParameterHelper.getTenantParameter(),
                SequenceType.ARCHIVE_UNIT_PROFILE_SEQUENCE
            );
            archiveUnitProfilModel.setIdentifier(code);
        }
    }

    private VitamError<ArchiveUnitProfileModel> getVitamError(String vitamCode, String error) {
        return VitamErrorUtils.getVitamError(
            vitamCode,
            error,
            "ArchiveUnitProfile",
            StatusCode.KO,
            ArchiveUnitProfileModel.class
        );
    }

    /**
     * Get ontology map
     *
     * @return ontology map
     */
    private Map<String, OntologyModel> getOntologyModelMap()
        throws ReferentialException, InvalidParseOperationException {
        final fr.gouv.vitam.common.database.builder.request.single.Select select =
            new fr.gouv.vitam.common.database.builder.request.single.Select();
        // TODO when it's merged -> search on archive unit fields defined in ontology
        /*
         * final BooleanQuery query = and(); select.setQuery(query);
         */
        JsonNode queryDsl = select.getFinalSelect();
        try (DbRequestResult result = mongoAccess.findDocuments(queryDsl, FunctionalAdminCollections.ONTOLOGY)) {
            final RequestResponseOK<OntologyModel> ontologyModelResponse = result.getRequestResponseOK(
                queryDsl,
                Ontology.class,
                OntologyModel.class
            );
            List<OntologyModel> ontologyModelList = ontologyModelResponse.getResults();
            return ontologyModelList.stream().collect(Collectors.toMap(OntologyModel::getIdentifier, c -> c));
        }
    }

    /**
     * Validate that the field is present in the ontology map
     *
     * @param ontologyMap
     * @param field
     * @param error
     */
    private void validateFieldInSchemaAgainstOntology(
        Map<String, OntologyModel> ontologyMap,
        final String field,
        final VitamError<ArchiveUnitProfileModel> error
    ) {
        if (!ontologyMap.containsKey(field)) {
            error.addToErrors(
                getVitamError(
                    VitamCode.ARCHIVE_UNIT_PROFILE_VALIDATION_ERROR.getItem(),
                    ArchiveUnitProfileValidator.RejectionCause.rejectMissingFieldInOntology(field).getReason()
                )
            );
        }
    }
}
