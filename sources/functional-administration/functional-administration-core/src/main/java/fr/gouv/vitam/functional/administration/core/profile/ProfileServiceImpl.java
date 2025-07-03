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
package fr.gouv.vitam.functional.administration.core.profile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.accesslog.AccessLogUtils;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.builder.request.single.Update;
import fr.gouv.vitam.common.database.parser.request.adapter.SingleVarNameAdapter;
import fr.gouv.vitam.common.database.parser.request.single.SelectParserSingle;
import fr.gouv.vitam.common.database.parser.request.single.UpdateParserSingle;
import fr.gouv.vitam.common.database.server.DbRequestResult;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.guid.GUIDReader;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.ProfileFormat;
import fr.gouv.vitam.common.model.administration.ProfileSedaVersion;
import fr.gouv.vitam.common.model.administration.ProfileStatus;
import fr.gouv.vitam.common.model.administration.profile.CreateProfileModel;
import fr.gouv.vitam.common.model.administration.profile.ProfileModel;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.stream.VitamAsyncInputStreamResponse;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.common.xml.SecureXMLFactoryUtils;
import fr.gouv.vitam.functional.administration.common.Profile;
import fr.gouv.vitam.functional.administration.common.VitamErrorUtils;
import fr.gouv.vitam.functional.administration.common.counter.SequenceType;
import fr.gouv.vitam.functional.administration.common.counter.VitamCounterService;
import fr.gouv.vitam.functional.administration.common.exception.ProfileNotFoundException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.functional.administration.core.backup.FunctionalBackupService;
import fr.gouv.vitam.functional.administration.core.profile.ProfileValidator.RejectionCause;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageUnavailableDataFromAsyncOfferClientException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.apache.commons.collections4.CollectionUtils;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.LocalDateUtil.now;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;

/**
 * The implementation of the profile servie This implementation manage creation, update, ... profiles with any given
 * format (xsd, rng)
 */
public class ProfileServiceImpl implements ProfileService {

    public static final String OP_PROFILE_STORAGE = "OP_PROFILE_STORAGE";
    public static final String PROFILE_FORMAT_SHOULD_BE_XSD_OR_RNG = "Profile Format should be XSD or RNG : ";
    public static final String PROFILE_SEDAVERSION_SHOULD_BE_VALID = "Profile SedaVersion should be a valid value : ";
    public static final String PROFILE_IDENTIFIER_ALREADY_EXISTS_IN_DATABASE =
        "Profile identifier already exists in database ";
    public static final String PROFILE_IDENTIFIER_MUST_BE_STRING = "Profile identifier shoud be a string ";
    public static final String PROFILE_BACKUP_EVENT = "BACKUP_PROFILE";
    public static final String PATH_UNUPDATABLE = "The path field is not updatable";
    public static final String PATH_SHOULD_NOT_BE_FILLED = "The profile path should not be filled manually";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProfileServiceImpl.class);
    private static final String PROFILE_IS_MANDATORY_PARAMETER = "profiles parameter is mandatory";
    private static final String PROFILES_IMPORT_EVENT = "STP_IMPORT_PROFILE_JSON";
    private static final String PROFILES_UPDATE_EVENT = "STP_UPDATE_PROFILE_JSON";
    private static final String PROFILES_FILE_IMPORT_EVENT = "STP_IMPORT_PROFILE_FILE";
    private static final String PROFILE_NOT_FOUND = "Update a not found profile";
    private static final String PROFILE_NOT_FOUND_WITH_IDENTIFIER = "No profile metadata found with identifier : ";
    private static final String MANDATORY_PROFILE_METADATA =
        ", to import the file, the metadata profile must be created first";
    private static final String UPDATED_DIFFS = "updatedDiffs";
    private static final String THE_PROFILE_STATUS_MUST_BE_ACTIVE_OR_INACTIVE_BUT_NOT =
        "The profile status must be ACTIVE or INACTIVE but not ";
    private static final String TENANT = "_tenant";
    private static final String ID = "_id";
    private final MongoDbAccessAdminImpl mongoAccess;
    private final LogbookOperationsClient logbookClient;
    private final VitamCounterService vitamCounterService;
    private final FunctionalBackupService functionalBackupService;

    /**
     * Constructor
     *
     * @param mongoAccess MongoDB client
     * @param vitamCounterService the vitam counter service
     * @param functionalBackupService the functional backup service
     */
    public ProfileServiceImpl(
        MongoDbAccessAdminImpl mongoAccess,
        VitamCounterService vitamCounterService,
        FunctionalBackupService functionalBackupService
    ) {
        this.mongoAccess = mongoAccess;
        this.vitamCounterService = vitamCounterService;
        logbookClient = LogbookOperationsClientFactory.getInstance().getClient();
        this.functionalBackupService = functionalBackupService;
    }

    @Override
    public RequestResponse<ProfileModel> createProfiles(List<CreateProfileModel> createProfileModels)
        throws VitamException {
        ParametersChecker.checkParameter(PROFILE_IS_MANDATORY_PARAMETER, createProfileModels);

        if (createProfileModels.isEmpty()) {
            return new RequestResponseOK<>();
        }

        final boolean slaveMode = vitamCounterService.isSlaveFunctionnalCollectionOnTenant(
            SequenceType.PROFILE_SEQUENCE.getCollection(),
            ParameterHelper.getTenantParameter()
        );
        final String operationId = VitamThreadUtils.getVitamSession().getRequestId();
        final GUID eip = GUIDReader.getGUID(operationId);
        final ProfileManager manager = new ProfileManager(logbookClient, eip);
        manager.logStarted(PROFILES_IMPORT_EVENT, null);

        final Set<String> profileIdentifiers = new HashSet<>();

        final VitamError<ProfileModel> error = getVitamError(
            VitamCode.PROFILE_FILE_IMPORT_ERROR.getItem(),
            "Global create profile error"
        ).setHttpCode(Response.Status.BAD_REQUEST.getStatusCode());

        try {
            for (final CreateProfileModel pm : createProfileModels) {
                // if a profile with the same identifier is already treated mark the current one as duplicated
                if (ParametersChecker.isNotEmpty(pm.getIdentifier())) {
                    if (profileIdentifiers.contains(pm.getIdentifier())) {
                        error.addToErrors(
                            getVitamError(
                                VitamCode.PROFILE_VALIDATION_ERROR.getItem(),
                                "Duplicate profiles"
                            ).setMessage("Profile identifier " + pm.getIdentifier() + " already exists in the json")
                        );
                        continue;
                    } else {
                        profileIdentifiers.add(pm.getIdentifier());
                    }
                }

                if (slaveMode && (pm.getIdentifier() == null || pm.getIdentifier().isEmpty())) {
                    error.addToErrors(
                        new VitamError<ProfileModel>(VitamCode.PROFILE_VALIDATION_ERROR.getItem())
                            .setDescription(
                                new RejectionCause(
                                    String.format("The field %s is mandatory.", CreateProfileModel.TAG_IDENTIFIER)
                                ).getReason()
                            )
                            .setMessage(ProfileManager.EMPTY_REQUIRED_FIELD)
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
                    .collect(Collectors.joining(","));
                manager.logValidationError(
                    PROFILES_IMPORT_EVENT,
                    null,
                    errorsDetails,
                    error.getErrors().get(0).getMessage()
                );
                return error;
            }

            final List<ProfileModel> profilesToCreate = createProfileModels
                .stream()
                .map(createProfileModel -> createProfileModel(createProfileModel, manager, error, slaveMode))
                .toList();
            final ArrayNode payload = JsonHandler.createArrayNode();
            final List<ObjectNode> profilesToPersist = profilesToCreate
                .stream()
                .map(this::convertToProfileNode)
                .toList();

            profilesToPersist.forEach(payload::add);

            // at this point no exception occurred and no validation error detected
            // persist in collection
            // profilesToPersist.values().stream().map();
            // TODO: 3/28/17 create insertDocuments method that accepts VitamDocument instead of ArrayNode, so we can
            // use Profile at this point
            mongoAccess.insertDocuments(payload, FunctionalAdminCollections.PROFILE).close();

            functionalBackupService.saveCollectionAndSequence(
                eip,
                PROFILE_BACKUP_EVENT,
                FunctionalAdminCollections.PROFILE,
                eip.toString()
            );

            manager.logSuccess(PROFILES_IMPORT_EVENT, null, null);

            return new RequestResponseOK<ProfileModel>()
                .addAllResults(profilesToCreate)
                .setHttpCode(Response.Status.CREATED.getStatusCode());
        } catch (final Exception exp) {
            LOGGER.error(exp);
            final String err = "Import profiles error : " + exp.getMessage();
            manager.logFatalError(PROFILES_IMPORT_EVENT, null, err);
            return getVitamError(VitamCode.PROFILE_FILE_IMPORT_ERROR.getItem(), err).setHttpCode(
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()
            );
        }
    }

    // Méthode privée pour créer et configurer un profil
    private ProfileModel createProfileModel(
        CreateProfileModel createProfileModel,
        ProfileManager manager,
        VitamError<ProfileModel> error,
        boolean slaveMode
    ) {
        ProfileModel profileModelToPersist = new ProfileModel(createProfileModel);

        validateAndSetId(profileModelToPersist, error, manager); // manager et error passés ici
        ensureTenantIsSet(profileModelToPersist);

        try {
            setIdentifier(slaveMode, profileModelToPersist);
        } catch (ReferentialException e) {
            throw new RuntimeException("Failed to set identifier for profile: " + profileModelToPersist.getId(), e);
        }

        return profileModelToPersist;
    }

    // Valide et génère un ID pour le profil
    private void validateAndSetId(
        ProfileModel profileModelToPersist,
        VitamError<ProfileModel> error,
        ProfileManager manager
    ) {
        if (manager.validateProfile(profileModelToPersist, error)) {
            profileModelToPersist.setId(GUIDFactory.newProfileGUID(ParameterHelper.getTenantParameter()).getId());
        }
    }

    // Assure que le tenant est défini
    private void ensureTenantIsSet(ProfileModel profileModelToPersist) {
        if (profileModelToPersist.getTenant() == null) {
            profileModelToPersist.setTenant(ParameterHelper.getTenantParameter());
        }
    }

    // Méthode privée pour simplifier la transformation d'un profil
    private ObjectNode convertToProfileNode(ProfileModel profileModel) {
        ObjectNode profileNode;
        try {
            profileNode = (ObjectNode) JsonHandler.toJsonNode(profileModel);
        } catch (InvalidParseOperationException e) {
            throw new RuntimeException("Error converting profileModel to JsonNode", e);
        }

        handleFieldRemoval(profileNode, VitamFieldsHelper.id(), ID);
        handleFieldRemoval(profileNode, VitamFieldsHelper.tenant(), TENANT);

        return profileNode;
    }

    // Méthode privée pour gérer la suppression et l'ajout des champs
    private void handleFieldRemoval(ObjectNode profileNode, String fieldToRemove, String fieldToAdd) {
        JsonNode removedField = profileNode.remove(fieldToRemove);
        if (removedField != null) {
            profileNode.set(fieldToAdd, removedField);
        }
    }

    private void setIdentifier(boolean slaveMode, ProfileModel pm) throws ReferentialException {
        if (!slaveMode) {
            String code = vitamCounterService.getNextSequenceAsString(
                ParameterHelper.getTenantParameter(),
                SequenceType.PROFILE_SEQUENCE
            );
            pm.setIdentifier(code);
        }
    }

    @Override
    public RequestResponse<ProfileModel> importProfileFile(String profileIdentifier, InputStream profileFile)
        throws VitamException {
        final ProfileModel profileModel = findByIdentifier(profileIdentifier);
        final GUID eip = GUIDFactory.newOperationLogbookGUID(ParameterHelper.getTenantParameter());
        final ProfileManager manager = new ProfileManager(logbookClient, eip);
        final VitamError<ProfileModel> vitamError = getVitamError(
            VitamCode.PROFILE_FILE_IMPORT_ERROR.getItem(),
            "Global import profile error"
        ).setHttpCode(Response.Status.BAD_REQUEST.getStatusCode());

        if (null == profileModel) {
            LOGGER.error(PROFILE_NOT_FOUND_WITH_IDENTIFIER + profileIdentifier + MANDATORY_PROFILE_METADATA);

            manager.logValidationError(
                PROFILES_FILE_IMPORT_EVENT,
                profileIdentifier,
                PROFILE_NOT_FOUND_WITH_IDENTIFIER + profileIdentifier + MANDATORY_PROFILE_METADATA,
                ProfileManager.IMPORT_KO
            );
            return vitamError.addToErrors(
                getVitamError(
                    VitamCode.PROFILE_FILE_IMPORT_ERROR.getItem(),
                    PROFILE_NOT_FOUND_WITH_IDENTIFIER + profileIdentifier + MANDATORY_PROFILE_METADATA
                )
            );
        }

        manager.logStarted(PROFILES_FILE_IMPORT_EVENT, null);

        File file = null;
        final ProfileSedaVersion sedaVersionToUpdate;

        try {
            ProfileSedaVersion schemaDefinitionSedaVersion = ProfileSedaVersion.DEFAULT;

            file = File.createTempFile(GUIDFactory.newGUID().getId(), "profile");
            Files.copy(profileFile, file.toPath(), StandardCopyOption.REPLACE_EXISTING);

            if (profileModel.getFormat() == ProfileFormat.RNG) {
                final FileInputStream fileInputStream = new FileInputStream(file);
                final InputSource inputSource = new InputSource(fileInputStream);
                schemaDefinitionSedaVersion = extractSedaVersion(inputSource);
            } else if (profileModel.getFormat() == ProfileFormat.XSD && profileModel.getSedaVersion() != null) {
                schemaDefinitionSedaVersion = profileModel.getSedaVersion();
            }

            final ProfileSedaVersion currentSedaVersion = profileModel.getSedaVersion();

            if (schemaDefinitionSedaVersion == null) {
                final String errorDetails = "No seda version found in schema definition file";
                LOGGER.error(errorDetails);
                manager.logValidationError(
                    PROFILES_FILE_IMPORT_EVENT,
                    profileIdentifier,
                    errorDetails,
                    ProfileManager.IMPORT_KO
                );
                return vitamError.addToErrors(
                    getVitamError(VitamCode.PROFILE_FILE_IMPORT_ERROR.getItem(), errorDetails)
                );
            }

            if (currentSedaVersion != null && !currentSedaVersion.equals(schemaDefinitionSedaVersion)) {
                final String errorDetails =
                    "Extracted seda version from schema definition file '%s', not matches profile ones '%s'".formatted(
                            schemaDefinitionSedaVersion.getVersion(),
                            currentSedaVersion.getVersion()
                        );
                LOGGER.error(errorDetails);
                manager.logValidationError(
                    PROFILES_FILE_IMPORT_EVENT,
                    profileIdentifier,
                    errorDetails,
                    ProfileManager.IMPORT_KO
                );
                return vitamError.addToErrors(
                    getVitamError(VitamCode.PROFILE_FILE_IMPORT_ERROR.getItem(), errorDetails)
                );
            }

            sedaVersionToUpdate = Optional.ofNullable(currentSedaVersion).orElse(schemaDefinitionSedaVersion);

            /*
             * Validate the stream
             */
            boolean isValid = manager.validateProfileFile(profileModel, file, vitamError);

            if (!isValid) {
                final String errorsDetails = vitamError
                    .getErrors()
                    .stream()
                    .map(VitamError::getMessage)
                    .collect(Collectors.joining(","));
                manager.logValidationError(
                    PROFILES_FILE_IMPORT_EVENT,
                    profileModel.getId(),
                    "Profile file validate error : " + errorsDetails,
                    ProfileManager.IMPORT_KO
                );
                return vitamError;
            }

            final String extension = profileModel.getFormat().getExtention();
            final Integer tenantId = ParameterHelper.getTenantParameter();
            final String fileName = String.format(
                "%d_profile_%s_%s.%s",
                tenantId,
                profileModel.getId(),
                now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")),
                extension
            );

            final String oldPath = profileModel.getPath();
            final InputStream profileIS = new FileInputStream(file);
            functionalBackupService.saveFile(profileIS, eip, OP_PROFILE_STORAGE, DataCategory.PROFILE, fileName);

            final UpdateParserSingle updateParserActive = new UpdateParserSingle(new SingleVarNameAdapter());
            final Update update = new Update();
            update.setQuery(eq(ProfileModel.TAG_IDENTIFIER, profileModel.getIdentifier()));
            update.addActions(
                UpdateActionHelper.set(ProfileModel.TAG_PATH, fileName),
                UpdateActionHelper.set(ProfileModel.TAG_SEDA_VERSION, sedaVersionToUpdate.getVersion()),
                UpdateActionHelper.set(ProfileModel.LAST_UPDATE, LocalDateUtil.nowFormatted())
            );
            updateParserActive.parse(update.getFinalUpdate());
            final JsonNode queryDsl = updateParserActive.getRequest().getFinalUpdate();
            mongoAccess.updateData(queryDsl, FunctionalAdminCollections.PROFILE).close();
            // Collection backup
            functionalBackupService.saveCollectionAndSequence(
                eip,
                PROFILE_BACKUP_EVENT,
                FunctionalAdminCollections.PROFILE,
                eip.toString()
            );

            String wellFormedJson = null;
            try {
                final ObjectNode object = JsonHandler.createObjectNode();
                final ObjectNode msg = JsonHandler.createObjectNode();
                msg.put("updateField", "Path");
                msg.put("oldPath", oldPath);
                msg.put("newPath", fileName);
                object.set("profileUpdate", msg);

                wellFormedJson = SanityChecker.sanitizeJson(object);
            } catch (InvalidParseOperationException e) {
                // Do nothing
            }

            manager.logSuccess(PROFILES_FILE_IMPORT_EVENT, null, wellFormedJson);
        } catch (Exception e) {
            String err = "Import profile error : " + e.getMessage();
            LOGGER.error(err, e);
            manager.logFatalError(PROFILES_FILE_IMPORT_EVENT, null, err);
            return getVitamError(VitamCode.GLOBAL_INTERNAL_SERVER_ERROR.getItem(), err).setHttpCode(
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()
            );
        } finally {
            try {
                Files.deleteIfExists(file.toPath());
            } catch (IOException e) {
                SysErrLogger.FAKE_LOGGER.ignoreLog(e);
            }

            if (null != profileFile) {
                try {
                    profileFile.close();
                } catch (final IOException e) {
                    LOGGER.error("Error while closing the profile file stream!");
                }
            }
        }

        return new RequestResponseOK<ProfileModel>().setHttpCode(Response.Status.CREATED.getStatusCode());
    }

    @Override
    public Response downloadProfileFile(String profileIdentifier)
        throws ProfileNotFoundException, InvalidParseOperationException, ReferentialException {
        final ProfileModel profileMetadata = findByIdentifier(profileIdentifier);
        if (null == profileMetadata) {
            LOGGER.error("No profile metadata found with id : " + profileIdentifier + MANDATORY_PROFILE_METADATA);
            throw new ProfileNotFoundException(
                "No profile metadata found with id : " + profileIdentifier + MANDATORY_PROFILE_METADATA
            );
        }

        if (Strings.isNullOrEmpty(profileMetadata.getPath()) || profileMetadata.getPath().isEmpty()) {
            LOGGER.error(
                "The profile metadata found with an id : " +
                profileIdentifier +
                ", does not have an xsd or an rng file yet"
            );
            throw new ProfileNotFoundException(
                "The profile metadata found with id : " + profileIdentifier + ", does not have a xsd or rng file yet"
            );
        }

        // A valid operation found : download the related file
        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
            final Response response = storageClient.getContainerAsync(
                VitamConfiguration.getDefaultStrategy(),
                profileMetadata.getPath(),
                DataCategory.PROFILE,
                AccessLogUtils.getNoLogAccessLog()
            );
            Map<String, String> headers = new HashMap<>();
            headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM);
            headers.put(HttpHeaders.CONTENT_DISPOSITION, "filename=" + profileMetadata.getPath());
            return new VitamAsyncInputStreamResponse(response, Status.OK, headers);
        } catch (final StorageServerClientException | StorageUnavailableDataFromAsyncOfferClientException e) {
            throw new ReferentialException(e);
        } catch (final StorageNotFoundException e) {
            throw new ProfileNotFoundException(e);
        }
    }

    @Override
    public RequestResponse<ProfileModel> updateProfile(String identifier, JsonNode jsonDsl) throws VitamException {
        final ProfileModel profileModel = findByIdentifier(identifier);
        return updateProfile(profileModel, jsonDsl);
    }

    @Override
    public RequestResponse<ProfileModel> updateProfile(ProfileModel profileModel, JsonNode jsonDsl)
        throws VitamException {
        if (profileModel == null) {
            return getVitamError(VitamCode.PROFILE_VALIDATION_ERROR.getItem(), PROFILE_NOT_FOUND)
                .setHttpCode(Response.Status.NOT_FOUND.getStatusCode())
                .setMessage(ProfileManager.UPDATE_PROFILE_NOT_FOUND);
        }

        String operationId = VitamThreadUtils.getVitamSession().getRequestId();
        GUID eip = GUIDReader.getGUID(operationId);
        final ProfileManager manager = new ProfileManager(logbookClient, eip);
        Map<String, List<String>> updateDiffs;
        RequestResponseOK<ProfileModel> response = new RequestResponseOK<>();

        manager.logStarted(PROFILES_UPDATE_EVENT, profileModel.getId());

        if (jsonDsl == null || !jsonDsl.isObject()) {
            manager.logValidationError(
                PROFILES_UPDATE_EVENT,
                profileModel.getId(),
                "Update query dsl must be an object and not null",
                ProfileManager.UPDATE_KO
            );
            return getVitamError(
                VitamCode.PROFILE_VALIDATION_ERROR.getItem(),
                "Update query dsl must be an object and not null : " + profileModel.getIdentifier()
            )
                .setHttpCode(Response.Status.BAD_REQUEST.getStatusCode())
                .setMessage(ProfileManager.UPDATE_KO);
        }

        final VitamError<ProfileModel> error = getVitamError(
            VitamCode.PROFILE_VALIDATION_ERROR.getItem(),
            "Update profile error : " + profileModel.getIdentifier()
        ).setHttpCode(Response.Status.BAD_REQUEST.getStatusCode());

        final JsonNode actionNode = jsonDsl.get(BuilderToken.GLOBAL.ACTION.exactToken());
        ProfileSedaVersion sedaVersion = ProfileSedaVersion.DEFAULT;

        if (profileModel.getSedaVersion() != null) sedaVersion = profileModel.getSedaVersion();

        for (final JsonNode fieldToSet : actionNode) {
            final JsonNode fieldName = fieldToSet.get(BuilderToken.UPDATEACTION.SET.exactToken());
            if (fieldName != null) {
                final Iterator<String> it = fieldName.fieldNames();
                while (it.hasNext()) {
                    final String field = it.next();
                    final JsonNode value = fieldName.findValue(field);
                    validateUpdateAction(profileModel, error, field, value, manager);
                    if (Profile.SEDA_VERSION.equals(field)) {
                        sedaVersion = ProfileSedaVersion.forVersion(value.asText());
                    }
                }
                ((ObjectNode) fieldName).remove(ProfileModel.CREATION_DATE);
                ((ObjectNode) fieldName).put(ProfileModel.LAST_UPDATE, LocalDateUtil.nowFormatted());
                ((ObjectNode) fieldName).put(Profile.SEDA_VERSION, sedaVersion.getVersion());
            }
        }

        if (error.getErrors() != null && CollectionUtils.isNotEmpty(error.getErrors())) {
            final String errorsDetails = error
                .getErrors()
                .stream()
                .map(VitamError::getDescription)
                .collect(Collectors.joining(","));
            manager.logValidationError(
                PROFILES_UPDATE_EVENT,
                profileModel.getId(),
                errorsDetails,
                error.getErrors().get(0).getMessage()
            );

            return error;
        }

        String wellFormedJson = null;
        try {
            try (DbRequestResult result = mongoAccess.updateData(jsonDsl, FunctionalAdminCollections.PROFILE)) {
                updateDiffs = result.getDiffs();
                response
                    .addAllResults(result.getDocuments(Profile.class, ProfileModel.class))
                    .setTotal(result.getTotal())
                    .setQuery(jsonDsl)
                    .setHttpCode(Response.Status.OK.getStatusCode());
            }

            List<String> diff = updateDiffs.get(profileModel.getId());
            try {
                final ObjectNode object = JsonHandler.createObjectNode();
                object.put(UPDATED_DIFFS, Joiner.on(" ").join(diff));
                wellFormedJson = SanityChecker.sanitizeJson(object);
            } catch (InvalidParseOperationException e) {
                // Do nothing
            }

            functionalBackupService.saveCollectionAndSequence(
                eip,
                PROFILE_BACKUP_EVENT,
                FunctionalAdminCollections.PROFILE,
                profileModel.getId()
            );
        } catch (final Exception e) {
            LOGGER.error(e);
            final String err = "Update profile error : " + e.getMessage();
            manager.logFatalError(PROFILES_UPDATE_EVENT, profileModel.getId(), err);
            error
                .setCode(VitamCode.GLOBAL_INTERNAL_SERVER_ERROR.getItem())
                .setDescription(err)
                .setHttpCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());

            return error;
        }

        manager.logSuccess(PROFILES_UPDATE_EVENT, profileModel.getId(), wellFormedJson);
        return response;
    }

    /**
     * Validate dsl
     *
     * @param profileModel
     * @param error
     * @param field
     * @param value
     */
    private void validateUpdateAction(
        ProfileModel profileModel,
        final VitamError<ProfileModel> error,
        final String field,
        final JsonNode value,
        ProfileManager manager
    ) {
        if (
            Profile.STATUS.equals(field) &&
            !(ProfileStatus.ACTIVE.name().equals(value.asText()) ||
                ProfileStatus.INACTIVE.name().equals(value.asText()))
        ) {
            error.addToErrors(
                getVitamError(
                    VitamCode.PROFILE_VALIDATION_ERROR.getItem(),
                    THE_PROFILE_STATUS_MUST_BE_ACTIVE_OR_INACTIVE_BUT_NOT + value.asText()
                )
                    .setHttpCode(Response.Status.BAD_REQUEST.getStatusCode())
                    .setMessage(ProfileManager.UPDATE_VALUE_NOT_IN_ENUM)
            );
        }

        if (
            Profile.FORMAT.equals(field) &&
            !(ProfileFormat.XSD.name().equals(value.asText()) || ProfileFormat.RNG.name().equals(value.asText()))
        ) {
            error.addToErrors(
                getVitamError(
                    VitamCode.PROFILE_VALIDATION_ERROR.getItem(),
                    PROFILE_FORMAT_SHOULD_BE_XSD_OR_RNG + value.asText()
                )
                    .setHttpCode(Response.Status.BAD_REQUEST.getStatusCode())
                    .setMessage(ProfileManager.UPDATE_KO)
            );
        }

        if (Profile.SEDA_VERSION.equals(field)) {
            try {
                ProfileSedaVersion.forVersion(value.asText()); // return value ignored, call only for validation
            } catch (IllegalArgumentException e) {
                error.addToErrors(
                    getVitamError(
                        VitamCode.PROFILE_VALIDATION_ERROR.getItem(),
                        PROFILE_SEDAVERSION_SHOULD_BE_VALID + value.asText()
                    )
                        .setHttpCode(Response.Status.BAD_REQUEST.getStatusCode())
                        .setMessage(ProfileManager.UPDATE_KO)
                );
            }

            try (final Response response = downloadProfileFile(profileModel.getIdentifier())) {
                if (response.getStatus() == 200) {
                    ProfileSedaVersion schemaDefinitionSedaVersion = ProfileSedaVersion.DEFAULT;
                    ProfileSedaVersion currentSedaVersion = profileModel.getSedaVersion();
                    if (profileModel.getFormat() == ProfileFormat.RNG) {
                        final InputStream inputStream = response.getEntity() instanceof InputStream
                            ? (InputStream) response.getEntity()
                            : response.readEntity(InputStream.class);
                        final InputSource inputSource = new InputSource(inputStream);
                        schemaDefinitionSedaVersion = extractSedaVersion(inputSource);
                    } else if (profileModel.getFormat() == ProfileFormat.XSD && profileModel.getSedaVersion() != null) {
                        schemaDefinitionSedaVersion = profileModel.getSedaVersion();
                    }
                    final ProfileSedaVersion nextSedaVersion = ProfileSedaVersion.forVersion(value.asText());

                    if (schemaDefinitionSedaVersion == null) {
                        final String errorDetails = "Schema definition file not contains seda version";

                        LOGGER.error(errorDetails);

                        error.addToErrors(
                            getVitamError(VitamCode.PROFILE_VALIDATION_ERROR.getItem(), errorDetails)
                                .setHttpCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
                                .setMessage(ProfileManager.UPDATE_KO)
                        );
                    }

                    if (schemaDefinitionSedaVersion != null && !nextSedaVersion.equals(schemaDefinitionSedaVersion)) {
                        final String errorDetails =
                            "The new SEDA version value '%s' does not match the one in the schema definition file '%s'".formatted(
                                    nextSedaVersion.getVersion(),
                                    schemaDefinitionSedaVersion.getVersion()
                                );

                        LOGGER.error(errorDetails);

                        error.addToErrors(
                            getVitamError(VitamCode.PROFILE_VALIDATION_ERROR.getItem(), errorDetails)
                                .setHttpCode(Status.BAD_REQUEST.getStatusCode())
                                .setMessage(ProfileManager.UPDATE_KO)
                        );
                    }

                    if (
                        schemaDefinitionSedaVersion != null &&
                        currentSedaVersion != null &&
                        !nextSedaVersion.equals(currentSedaVersion)
                    ) {
                        final String errorDetails =
                            "The new SEDA version value '%s' does not match the one in the profile '%s'".formatted(
                                    nextSedaVersion.getVersion(),
                                    currentSedaVersion.getVersion()
                                );

                        LOGGER.error(errorDetails);

                        error.addToErrors(
                            getVitamError(VitamCode.PROFILE_VALIDATION_ERROR.getItem(), errorDetails)
                                .setHttpCode(Status.BAD_REQUEST.getStatusCode())
                                .setMessage(ProfileManager.UPDATE_KO)
                        );
                    }
                }
            } catch (ProfileNotFoundException e) {
                LOGGER.info("Not profile found, seda version can be updated: {}", e.getMessage());
            } catch (
                ReferentialException
                | InvalidParseOperationException
                | ParserConfigurationException
                | IOException
                | SAXException e
            ) {
                final String errorDetails = "Schema definition file seda's version extraction failure";

                LOGGER.error("{}: {}", errorDetails, e.getMessage());

                error.addToErrors(
                    getVitamError(VitamCode.PROFILE_VALIDATION_ERROR.getItem(), errorDetails)
                        .setHttpCode(Status.INTERNAL_SERVER_ERROR.getStatusCode())
                        .setMessage(ProfileManager.UPDATE_KO)
                );
            }
        }

        if (ProfileModel.TAG_IDENTIFIER.equals(field)) {
            if (!value.isTextual()) {
                error.addToErrors(
                    getVitamError(
                        VitamCode.PROFILE_VALIDATION_ERROR.getItem(),
                        PROFILE_IDENTIFIER_MUST_BE_STRING + " : " + value.asText()
                    )
                        .setHttpCode(Response.Status.BAD_REQUEST.getStatusCode())
                        .setMessage(ProfileManager.UPDATE_KO)
                );
            } else if (!profileModel.getIdentifier().equals(value.asText())) {
                Optional<RejectionCause> validateIdentifier = manager
                    .createCheckDuplicateInDatabaseValidator()
                    .validate(new ProfileModel().setIdentifier(value.asText()));
                if (validateIdentifier.isPresent()) {
                    error.addToErrors(
                        getVitamError(
                            VitamCode.PROFILE_VALIDATION_ERROR.getItem(),
                            PROFILE_IDENTIFIER_ALREADY_EXISTS_IN_DATABASE + " : " + value.asText()
                        )
                            .setHttpCode(Response.Status.BAD_REQUEST.getStatusCode())
                            .setMessage(ProfileManager.UPDATE_DUPLICATE_IN_DATABASE)
                    );
                }
            }
        }

        if (ProfileModel.TAG_PATH.equals(field)) {
            error.addToErrors(
                getVitamError(VitamCode.PROFILE_VALIDATION_ERROR.getItem(), PATH_UNUPDATABLE + " : " + value.asText())
                    .setHttpCode(Response.Status.BAD_REQUEST.getStatusCode())
                    .setMessage(PATH_SHOULD_NOT_BE_FILLED)
            );
        }
    }

    @Override
    public ProfileModel findByIdentifier(String identifier)
        throws ReferentialException, InvalidParseOperationException {
        SanityChecker.checkParameter(identifier);
        final SelectParserSingle parser = new SelectParserSingle(new SingleVarNameAdapter());
        parser.parse(new Select().getFinalSelect());
        try {
            parser.addCondition(eq(Profile.IDENTIFIER, identifier));
        } catch (final InvalidCreateOperationException e) {
            throw new ReferentialException(e);
        }

        try (
            DbRequestResult result = mongoAccess.findDocuments(
                parser.getRequest().getFinalSelect(),
                FunctionalAdminCollections.PROFILE
            )
        ) {
            final List<ProfileModel> list = result.getDocuments(Profile.class, ProfileModel.class);
            if (list.isEmpty()) {
                return null;
            }
            return list.get(0);
        }
    }

    @Override
    public RequestResponseOK<ProfileModel> findProfiles(JsonNode queryDsl)
        throws ReferentialException, InvalidParseOperationException {
        try (DbRequestResult result = mongoAccess.findDocuments(queryDsl, FunctionalAdminCollections.PROFILE)) {
            return result.getRequestResponseOK(queryDsl, Profile.class, ProfileModel.class);
        }
    }

    private VitamError<ProfileModel> getVitamError(String vitamCode, String error) {
        return VitamErrorUtils.getVitamError(vitamCode, error, "Profile", StatusCode.KO, ProfileModel.class);
    }

    @Override
    public void close() {
        if (null != logbookClient) {
            logbookClient.close();
        }
    }

    ProfileSedaVersion extractSedaVersion(final InputSource inputSource)
        throws SAXException, ParserConfigurationException, IOException {
        final SAXParserFactory factory = SecureXMLFactoryUtils.createSecureSAXParserFactory();

        // Créer un SAXParser sécurisé
        final ProfileSax2Handler handler = new ProfileSax2Handler();
        final SAXParser saxParser = factory.newSAXParser();
        final XMLReader reader = saxParser.getXMLReader();
        reader.setContentHandler(handler);
        reader.parse(inputSource);

        return handler.getSedaVersion();
    }
}
