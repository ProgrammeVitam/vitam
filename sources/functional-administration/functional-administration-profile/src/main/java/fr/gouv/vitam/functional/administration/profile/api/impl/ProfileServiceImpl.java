/*
 *  Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *  <p>
 *  contact.vitam@culture.gouv.fr
 *  <p>
 *  This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 *  high volumetry securely and efficiently.
 *  <p>
 *  This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 *  software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 *  circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 *  <p>
 *  As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 *  users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 *  successive licensors have only limited liability.
 *  <p>
 *  In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 *  developing or reproducing the software by the user in light of its specific status of free software, that may mean
 *  that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 *  experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 *  software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 *  to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *  <p>
 *  The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 *  accept its terms.
 */

package fr.gouv.vitam.functional.administration.profile.api.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.mongodb.client.MongoCursor;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.parser.request.adapter.VarNameAdapter;
import fr.gouv.vitam.common.database.parser.request.single.SelectParserSingle;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.guid.GUIDObjectType;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.server.application.AsyncInputStreamHelper;
import fr.gouv.vitam.common.server.application.VitamStreamingOutput;
import fr.gouv.vitam.functional.administration.client.model.ProfileModel;
import fr.gouv.vitam.functional.administration.common.Profile;
import fr.gouv.vitam.functional.administration.common.embed.ProfileFormat;
import fr.gouv.vitam.functional.administration.common.exception.ProfileNotFoundException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.functional.administration.profile.api.ProfileService;
import fr.gouv.vitam.functional.administration.profile.core.ProfileManager;
import fr.gouv.vitam.functional.administration.profile.core.ProfileValidator.RejectionCause;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageAlreadyExistsClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.model.StorageCollectionType;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.apache.commons.io.IOUtils;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The implementation of the profile servie
 * This implementation manage creation, update, ... profiles with any given format (xsd, rng)
 *
 */
public class ProfileServiceImpl implements ProfileService {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProfileServiceImpl.class);

    private static final String PROFILE_IS_MANDATORY_PATAMETER = "profiles parameter is mandatory";
    private static final String PROFILES_IMPORT_EVENT = "STP_IMPORT_PROFILE_JSON";
    private static final String PROFILES_FILE_IMPORT_EVENT = "STP_IMPORT_PROFILE_FILE";
    private static final String  OP_PROFILE_STORAGE= "OP_PROFILE_STORAGE";
    private final MongoDbAccessAdminImpl mongoAccess;
    private LogbookOperationsClient logBookclient;
    private final WorkspaceClientFactory workspaceClientFactory;
    private static final String DEFAULT_STORAGE_STRATEGY = "default";

    private ProfileManager manager;

    /**
     * Constructor
     *
     * @param mongoAccess MongoDB client
     */
    public ProfileServiceImpl(MongoDbAccessAdminImpl mongoAccess, WorkspaceClientFactory workspaceClientFactory) {
        this.mongoAccess = mongoAccess;
        this.workspaceClientFactory = workspaceClientFactory;
        this.logBookclient = LogbookOperationsClientFactory.getInstance().getClient();
        this.manager = new ProfileManager(logBookclient);
    }


    @Override
    public RequestResponse<ProfileModel> createProfiles(List<ProfileModel> profileModelList)
        throws VitamException {
        ParametersChecker.checkParameter(PROFILE_IS_MANDATORY_PATAMETER, profileModelList);

        if (profileModelList.isEmpty()) {
            return new RequestResponseOK<>();
        }


        manager.logStarted(PROFILES_IMPORT_EVENT);

        final Set<String> profileIdentifiers = new HashSet<>();
        final Set<String> profileNames = new HashSet<>();
        ArrayNode profilesToPersist = null;

        final VitamError error = new VitamError(VitamCode.PROFILE_VALIDATION_ERROR.getItem()).setHttpCode(
            Response.Status.BAD_REQUEST.getStatusCode());

        try {

            for (final ProfileModel pm : profileModelList) {


                // if a profile have and id
                if (null != pm.getId()) {
                    error.addToErrors(new VitamError(VitamCode.PROFILE_VALIDATION_ERROR.getItem())
                        .setMessage(RejectionCause.rejectIdNotAllowedInCreate(pm.getName()).getReason()));
                    continue;
                }

                // if a profile with the same identifier is already treated mark the current one as duplicated
                if (profileIdentifiers.contains(pm.getIdentifier())) {
                    error.addToErrors(new VitamError(VitamCode.PROFILE_VALIDATION_ERROR.getItem())
                        .setMessage(RejectionCause.rejectDuplicatedEntry(pm.getIdentifier()).getReason()));
                    continue;
                }


                // if a profile with the same name is already treated mark the current one as duplicated
                if (profileNames.contains(pm.getName())) {
                    error.addToErrors(new VitamError(VitamCode.PROFILE_VALIDATION_ERROR.getItem())
                        .setMessage(RejectionCause.rejectDuplicatedEntry(pm.getName()).getReason()));
                    continue;
                }


                // mark the current profile as treated
                profileIdentifiers.add(pm.getIdentifier());
                profileNames.add(pm.getName());

                // validate profile
                if (manager.validateProfile(pm, error)) {

                    pm.setId(GUIDFactory.newProfileGUID(ParameterHelper.getTenantParameter()).getId());

                    final JsonNode profileNode = JsonHandler.toJsonNode(pm);


                    /* profile is valid, add it to the list to persist */
                    if (profilesToPersist == null) {
                        profilesToPersist = JsonHandler.createArrayNode();
                    }

                    profilesToPersist.add(profileNode);
                }


            }

            if (null != error.getErrors() && !error.getErrors().isEmpty()) {
                // log book + application log
                // stop
                String errorsDetails =
                    error.getErrors().stream().map(c -> c.getMessage()).collect(Collectors.joining(","));
                manager.logValidationError(PROFILES_IMPORT_EVENT, errorsDetails);
                return error;
            }

            // at this point no exception occurred and no validation error detected
            // persist in collection
            // profilesToPersist.values().stream().map();
            // TODO: 3/28/17 create insertDocuments method that accepts VitamDocument instead of ArrayNode, so we can
            // use Profile at this point
            mongoAccess.insertDocuments(profilesToPersist, FunctionalAdminCollections.PROFILE);
        } catch (Exception exp) {
            String err = new StringBuilder("Import profiles error > ").append(exp.getMessage()).toString();
            manager.logFatalError(PROFILES_IMPORT_EVENT,           err);
            return error.setCode(VitamCode.GLOBAL_INTERNAL_SERVER_ERROR.getItem()).setDescription(err).setHttpCode(
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        }

        manager.logSuccess(PROFILES_IMPORT_EVENT);


        return new RequestResponseOK<ProfileModel>().addAllResults(profileModelList).setHits(
            profileModelList.size(), 0, profileModelList.size()).setHttpCode(Response.Status.CREATED.getStatusCode());
    }


    @Override
    public RequestResponse importProfileFile(String profileMetadataId,
        InputStream profileFile)
        throws VitamException {

        manager.logStarted(PROFILES_FILE_IMPORT_EVENT);

        final ProfileModel profileMetadata = findOne(profileMetadataId);

        final VitamError vitamError = new VitamError(VitamCode.PROFILE_FILE_IMPORT_ERROR.getItem()).setHttpCode(
            Response.Status.BAD_REQUEST.getStatusCode());
        if (null == profileMetadata) {
            LOGGER.error("No profile metadata found with id : "+profileMetadataId+", to import the file, the metadata profile must be created first");

            manager.logValidationError(PROFILES_FILE_IMPORT_EVENT,
                "No profile metadata found with id : "+profileMetadataId+", to import the file, the metadata profile must be created first");
            return  vitamError. addToErrors(new VitamError(VitamCode.PROFILE_FILE_IMPORT_ERROR.getItem())
                .setMessage("No profile metadata found with id : "+profileMetadataId+", to import the file, the metadata profile must be created first"));
        }


        boolean cannotCopy = false;
        try {
            // To create a copy of inputstream, validateProfileFile use DocumentDuilder wich in some case close the stream.
            byte[] byteArray = IOUtils.toByteArray(profileFile);

            final InputStream profileFileCopy = new ByteArrayInputStream(byteArray);

            /*
             * Validate the stream
              */
            boolean isValide = manager.validateProfileFile(profileMetadata, profileFileCopy, vitamError);

            if (!isValide) {
                String errorsDetails =
                    vitamError.getErrors().stream().map(c -> c.getMessage()).collect(Collectors.joining(","));
                manager.logValidationError(PROFILES_FILE_IMPORT_EVENT,"Profile file validate error >> "+errorsDetails);
                return vitamError;
            }

            profileFile = new ByteArrayInputStream(byteArray);

            byteArray = null;
        } catch (IOException e) {
            cannotCopy = true;
        }



        String extention = "xsd";
        if (profileMetadata.getFormat().equals(ProfileFormat.RNG)) extention = "rng";

        Integer tenantId = ParameterHelper.getTenantParameter();
        final String containerName = String.format("%d_profiles", tenantId);
        final String fileName = String.format("%d_profile_%s.%s", tenantId, profileMetadata.getId(), extention);
        final String uri = String.format("%s/%s", extention, fileName);

        //Final path in the workspace : tenant_profiles/format(xsd|rng)/tenant_profile_id.format(xsd|rng)

        try (WorkspaceClient workspaceClient = workspaceClientFactory.getClient()) {
            manager.logInProgress(OP_PROFILE_STORAGE);

            workspaceClient.createContainer(containerName);
            workspaceClient.putObject(containerName, uri, profileFile);

            // If the copy of the stream is not processed then, we use workspace to save the file then read it to validate it
            if (cannotCopy) {
                final InputStream fileFromWorkSpace = workspaceClient.getObject(containerName, uri).readEntity(InputStream.class);
                boolean isValide = manager.validateProfileFile(profileMetadata, fileFromWorkSpace, vitamError);

                if (!isValide) {
                    String errorsDetails =
                        vitamError.getErrors().stream().map(c -> c.getMessage()).collect(Collectors.joining(","));
                    manager.logValidationError(PROFILES_FILE_IMPORT_EVENT,"Profile file validate error >> "+errorsDetails);
                    workspaceClient.deleteContainer(containerName, true);
                    return vitamError;
                }
            }


            final StorageClientFactory storageClientFactory = StorageClientFactory.getInstance();

            final ObjectDescription description = new ObjectDescription();
            description.setWorkspaceContainerGUID(containerName);
            description.setWorkspaceObjectURI(uri);

            try (final StorageClient storageClient = storageClientFactory.getClient()) {

                storageClient.storeFileFromWorkspace(DEFAULT_STORAGE_STRATEGY, StorageCollectionType.PROFILES, fileName, description);
                workspaceClient.deleteContainer(containerName, true);
                manager.logInProgress(OP_PROFILE_STORAGE);

            } catch (StorageAlreadyExistsClientException | StorageNotFoundClientException |
                StorageServerClientException | ContentAddressableStorageNotFoundException e) {
                String err = new StringBuilder("Import profiles storage error > ").append(e.getMessage()).toString();
                LOGGER.error(err, e);
                manager.logFatalError(OP_PROFILE_STORAGE,           err);
                return vitamError.setCode(VitamCode.GLOBAL_INTERNAL_SERVER_ERROR.getItem()).setDescription(err).setHttpCode(
                    Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
            }
        } catch (ContentAddressableStorageAlreadyExistException | ContentAddressableStorageServerException e) {
            String err = new StringBuilder("Import profiles storage workspace error > ").append(e.getMessage()).toString();
            LOGGER.error(err, e);
            manager.logFatalError(OP_PROFILE_STORAGE,           err);
            return vitamError.setCode(VitamCode.GLOBAL_INTERNAL_SERVER_ERROR.getItem()).setDescription(err).setHttpCode(
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());

        } finally {
            if (null != profileFile)
                try {
                    profileFile.close();
                } catch (IOException e) {
                    LOGGER.error("Error while closing the profile file stream!");
                }
        }

        manager.logSuccess(PROFILES_FILE_IMPORT_EVENT);

        return new RequestResponseOK().setHttpCode(Response.Status.CREATED.getStatusCode());
    }

    @Override
    public void downloadProfileFile(String profileMetadataId, AsyncResponse asyncResponse) throws
        ProfileNotFoundException, InvalidParseOperationException, ReferentialException {

        final ProfileModel profileMetadata = findOne(profileMetadataId);
        final VitamError vitamError = new VitamError(VitamCode.PROFILE_FILE_IMPORT_ERROR.getItem());
        if (null == profileMetadata) {
            LOGGER.error("No profile metadata found with id : "+profileMetadataId+", to import the file, the metadata profile must be created first");
            throw new ProfileNotFoundException("No profile metadata found with id : "+profileMetadataId+", to import the file, the metadata profile must be created first");
        }

        // A valid operation found : download the related file
        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {

            String extention = "xsd";
            if (profileMetadata.getFormat().equals(ProfileFormat.RNG)) extention = "rng";

            Integer tenantId = ParameterHelper.getTenantParameter();
            final String fileName = String.format("%d_profile_%s.%s", tenantId, profileMetadataId, extention);


            final Response response = storageClient.getContainerAsync(DEFAULT_STORAGE_STRATEGY, fileName, StorageCollectionType.PROFILES);

            final AsyncInputStreamHelper helper = new AsyncInputStreamHelper(asyncResponse, response);
            helper.writeResponse(Response
                .ok()
                .header("Content-Disposition", "filename=" + fileName)
                .header("Content-Type", "application/octet-stream"));

        } catch (StorageServerClientException | StorageNotFoundException e) {
            LOGGER.error(e.getMessage(), e);
            throw new ProfileNotFoundException(e);
        }
    }

    @Override
    public RequestResponse<ProfileModel> updateProfiles(ProfileModel profileModel) throws VitamException {
        throw new VitamException("The method updateProfile is not yet implemented !");
    }

    @Override
    public ProfileModel findOne(String id) throws ReferentialException, InvalidParseOperationException {
        final SelectParserSingle parser = new SelectParserSingle(new VarNameAdapter());
        parser.parse(new Select().getFinalSelect());
        try {
            parser.addCondition(QueryHelper.eq("#id", id));
        } catch (InvalidCreateOperationException e) {
            throw new ReferentialException(e);
        }

        MongoCursor<VitamDocument<?>> cursor =
            mongoAccess.findDocuments(parser.getRequest().getFinalSelect(), FunctionalAdminCollections.PROFILE);
        if (null == cursor)
            return null;

        while (cursor.hasNext()) {
            final Profile profile = (Profile) cursor.next();
            return JsonHandler.getFromString(profile.toJson(), ProfileModel.class);
        }

        return null;
    }

    @Override
    public ProfileModel findByIdentifier(String identifier)
        throws ReferentialException, InvalidParseOperationException {
        final SelectParserSingle parser = new SelectParserSingle(new VarNameAdapter());
        parser.parse(new Select().getFinalSelect());
        try {
            parser.addCondition(QueryHelper.eq(Profile.IDENTIFIER, identifier));
        } catch (InvalidCreateOperationException e) {
            throw new ReferentialException(e);
        }

        MongoCursor<VitamDocument<?>> cursor =
            mongoAccess.findDocuments(parser.getRequest().getFinalSelect(), FunctionalAdminCollections.PROFILE);
        if (null == cursor)
            return null;

        while (cursor.hasNext()) {
            final Profile profile = (Profile) cursor.next();
            return JsonHandler.getFromString(profile.toJson(), ProfileModel.class);
        }

        return null;
    }

    @Override
    public List<ProfileModel> findProfiles(JsonNode queryDsl)
        throws ReferentialException, InvalidParseOperationException {
        final List<ProfileModel> profileModelCollection = new ArrayList<>();
        MongoCursor<VitamDocument<?>> cursor =
            mongoAccess.findDocuments(queryDsl, FunctionalAdminCollections.PROFILE);

        if (null == cursor)
            return profileModelCollection;

        while (cursor.hasNext()) {
            final Profile profile = (Profile) cursor.next();
            profileModelCollection.add(JsonHandler.getFromString(profile.toJson(),
                ProfileModel.class));
        }

        return profileModelCollection;
    }

    @Override
    public void close() {
        if (null != logBookclient) logBookclient.close();
    }
}
