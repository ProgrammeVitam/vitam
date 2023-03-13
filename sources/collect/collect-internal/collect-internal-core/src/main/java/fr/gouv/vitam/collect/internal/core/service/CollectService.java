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
package fr.gouv.vitam.collect.internal.core.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.collect.common.dto.ObjectDto;
import fr.gouv.vitam.collect.common.exception.CollectInternalException;
import fr.gouv.vitam.collect.internal.core.common.CollectUnitModel;
import fr.gouv.vitam.collect.internal.core.helpers.CollectHelper;
import fr.gouv.vitam.collect.internal.core.helpers.builders.DbObjectGroupModelBuilder;
import fr.gouv.vitam.collect.internal.core.helpers.handlers.QueryHandler;
import fr.gouv.vitam.collect.internal.core.repository.MetadataRepository;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.query.action.SetAction;
import fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.UpdateMultiQuery;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.format.identification.FormatIdentifier;
import fr.gouv.vitam.common.format.identification.FormatIdentifierFactory;
import fr.gouv.vitam.common.format.identification.exception.FileFormatNotFoundException;
import fr.gouv.vitam.common.format.identification.exception.FormatIdentifierBadRequestException;
import fr.gouv.vitam.common.format.identification.exception.FormatIdentifierFactoryException;
import fr.gouv.vitam.common.format.identification.exception.FormatIdentifierNotFoundException;
import fr.gouv.vitam.common.format.identification.exception.FormatIdentifierTechnicalException;
import fr.gouv.vitam.common.format.identification.model.FormatIdentifierResponse;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.administration.DataObjectVersionType;
import fr.gouv.vitam.common.model.objectgroup.DbFormatIdentificationModel;
import fr.gouv.vitam.common.model.objectgroup.DbObjectGroupModel;
import fr.gouv.vitam.common.model.objectgroup.DbQualifiersModel;
import fr.gouv.vitam.common.model.objectgroup.DbVersionsModel;
import fr.gouv.vitam.common.stream.VitamAsyncInputStreamResponse;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.input.CountingInputStream;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.FILTERARGS.OBJECTGROUPS;
import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTIONARGS.QUALIFIERS;
import static fr.gouv.vitam.common.json.JsonHandler.toJsonNode;
import static fr.gouv.vitam.common.model.IngestWorkflowConstants.CONTENT_FOLDER;

public class CollectService {

    public static final String UNABLE_TO_FIND_ARCHIVE_UNIT_ID = "Unable to find archiveUnit Id";
    private static final String FORMAT_IDENTIFIER_ID = "siegfried-local";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CollectService.class);

    private final MetadataRepository metadataRepository;
    private final WorkspaceClientFactory workspaceCollectClientFactory;
    private final FormatIdentifierFactory formatIdentifierFactory;

    public CollectService(MetadataRepository metadataRepository, WorkspaceClientFactory workspaceCollectClientFactory,
        FormatIdentifierFactory formatIdentifierFactory) {
        this.metadataRepository = metadataRepository;
        this.workspaceCollectClientFactory = workspaceCollectClientFactory;
        this.formatIdentifierFactory = formatIdentifierFactory;
    }

    public CollectUnitModel getArchiveUnitModel(String unitId) throws CollectInternalException {

        try {
            JsonNode jsonNode = metadataRepository.selectUnitById(unitId);
            final CollectUnitModel unitModel =
                JsonHandler.getFromJsonNode(jsonNode, CollectUnitModel.class);
            if (unitModel == null) {
                LOGGER.error(UNABLE_TO_FIND_ARCHIVE_UNIT_ID);
                throw new CollectInternalException(UNABLE_TO_FIND_ARCHIVE_UNIT_ID);
            }
            return unitModel;
        } catch (CollectInternalException | InvalidParseOperationException e) {
            LOGGER.error("Error when fetching unit by id({}): {} ", unitId, e);
            throw new CollectInternalException("Error when fetching unit by id(" + unitId + ") " + e);
        }
    }

    public ObjectDto updateOrSaveObjectGroup(CollectUnitModel unitModel, DataObjectVersionType usage, int version,
        ObjectDto objectDto) throws CollectInternalException {

        try {
            objectDto.setId(GUIDFactory.newObjectGUID(VitamThreadUtils.getVitamSession().getTenantId()).getId());
            if (unitModel.getOg() == null) {
                insertNewObjectGroup(unitModel, usage, version, objectDto);
            } else {
                updateExistingObjectGroup(unitModel, usage, version, objectDto);
            }
            return objectDto;
        } catch (CollectInternalException e) {
            LOGGER.error("Error when saving Object in metadata: {}", e);
            throw new CollectInternalException(e);
        }
    }

    private void updateExistingObjectGroup(CollectUnitModel unitModel, DataObjectVersionType usage, int version,
        ObjectDto objectDto) throws CollectInternalException {

        try {
            JsonNode objectGroup = metadataRepository.selectObjectGroupById(unitModel.getOg(), true);

            DbObjectGroupModel dbObjectGroupModel = JsonHandler.getFromJsonNode(objectGroup, DbObjectGroupModel.class);
            DbQualifiersModel qualifierModelToUpdate =
                CollectHelper.findQualifier(dbObjectGroupModel.getQualifiers(), usage);

            if (qualifierModelToUpdate == null) {
                CollectHelper.checkVersion(version, 1);
                addQualifierToObjectGroups(dbObjectGroupModel, usage, version, objectDto);
            } else {
                DbVersionsModel dbVersionsModel =
                    CollectHelper.getObjectVersionsModel(dbObjectGroupModel, usage, version);

                if (dbVersionsModel != null) {
                    LOGGER.error("Qualifier already exist with usage {} and version {})", usage, version);
                    throw new CollectInternalException(
                        "Qualifier already exist with usage " + usage + " and version " + version + "");
                }

                int lastVersion = CollectHelper.getLastVersion(qualifierModelToUpdate) + 1;
                CollectHelper.checkVersion(version, lastVersion);
                addVersionToObjectGroups(qualifierModelToUpdate, dbObjectGroupModel, usage, lastVersion,
                    dbObjectGroupModel.getQualifiers(), objectDto);
            }
        } catch (InvalidParseOperationException e) {
            LOGGER.error("Error when updating existing Object : {}", e);
            throw new CollectInternalException(e);
        }
    }

    private void insertNewObjectGroup(CollectUnitModel unitModel, DataObjectVersionType usage, int version,
        ObjectDto objectDto) throws CollectInternalException {

        boolean gotCreated = false;
        String gotId = null;
        try {
            CollectHelper.checkVersion(version, 1);
            DbObjectGroupModel dbObjectGroupModel = new DbObjectGroupModelBuilder()
                .withId(objectDto.getId())
                .withOpi(unitModel.getOpi()).withFileInfoModel(objectDto.getFileInfo().getFileName())
                .withQualifiers(objectDto.getId(), objectDto.getFileInfo().getFileName(), usage, version).build();

            JsonNode jsonNode =
                metadataRepository.saveObjectGroup((ObjectNode) JsonHandler.toJsonNode(dbObjectGroupModel));
            if (jsonNode == null) {
                LOGGER.error("Error when trying to insert ObjectGroup : {})", dbObjectGroupModel);
                throw new CollectInternalException("Error when trying to insert ObjectGroup : : " + dbObjectGroupModel);
            }
            gotCreated = true;
            gotId = dbObjectGroupModel.getId();

            UpdateMultiQuery multiQuery = new UpdateMultiQuery();
            multiQuery.addActions(UpdateActionHelper.set(VitamFieldsHelper.object(), dbObjectGroupModel.getId()));
            multiQuery.resetRoots().addRoots(unitModel.getId());

            metadataRepository.updateUnitById(multiQuery, unitModel.getOpi(), unitModel.getId());
        } catch (final CollectInternalException | InvalidCreateOperationException | InvalidParseOperationException e) {
            if(gotCreated){
                metadataRepository.deleteObjectGroups(Arrays.asList(gotId));
            }
            LOGGER.error("Error when saving new objectGroup in metadata : {}", e);
            throw new CollectInternalException("Error when saving new objectGroup in metadata: " + e);
        }
    }

    private void addQualifierToObjectGroups(DbObjectGroupModel objectGroup, DataObjectVersionType usage, int version,
        ObjectDto objectDto) throws CollectInternalException {

        try {
            UpdateMultiQuery query = QueryHandler.getQualifiersAddMultiQuery(objectGroup, usage, version, objectDto);
            metadataRepository.updateObjectGroupById(query, objectGroup.getId(), objectGroup.getOpi());
        } catch (final InvalidParseOperationException | InvalidCreateOperationException e) {
            LOGGER.error("Error when adding usage/version to existing qualifier: {}", e);
            throw new CollectInternalException("Error when adding usage/version to existing qualifier: " + e);
        }
    }

    private void addVersionToObjectGroups(DbQualifiersModel qualifierModelToUpdate, DbObjectGroupModel objectGroup,
        DataObjectVersionType usage, int version, List<DbQualifiersModel> qualifiers, ObjectDto objectDto)
        throws CollectInternalException {

        try {
            UpdateMultiQuery query =
                QueryHandler.getQualifiersUpdateMultiQuery(qualifierModelToUpdate, usage, version, qualifiers,
                    objectDto, objectGroup.getNbc());

            metadataRepository.updateObjectGroupById(query, objectGroup.getId(), objectGroup.getOpi());
        } catch (InvalidParseOperationException | InvalidCreateOperationException e) {
            LOGGER.error("Error when adding version to Object: {}", e);
            throw new CollectInternalException("Error when adding version to Object: " + e);
        }
    }

    public DbObjectGroupModel getDbObjectGroup(CollectUnitModel unitModel) throws CollectInternalException {
        try {
            if (unitModel.getOg() == null) {
                LOGGER.debug("Cannot found any got attached to unit with id({}))", unitModel.getId());
                throw new IllegalArgumentException(
                    "Cannot found any object attached to unit with id(" + unitModel.getId() + ")");
            }

            JsonNode objectGroup = metadataRepository.selectObjectGroupById(unitModel.getOg(), true);
            return JsonHandler.getFromJsonNode(objectGroup, DbObjectGroupModel.class);
        } catch (InvalidParseOperationException e) {
            LOGGER.error("Error when fetching Object from metadata: {}", e);
            throw new CollectInternalException("Error when fetching Object from metadata: " + e);
        }
    }

    public void addBinaryInfoToQualifier(DbObjectGroupModel dbObjectGroupModel, DataObjectVersionType usage,
        int version, InputStream uploadedInputStream) throws CollectInternalException {

        DbQualifiersModel qualifierModelToUpdate =
            CollectHelper.findQualifier(dbObjectGroupModel.getQualifiers(), usage);

        if (qualifierModelToUpdate == null) {
            LOGGER.debug("Cannot found usage for object  with id({}))", dbObjectGroupModel.getId());
            throw new IllegalArgumentException(
                "Cannot found usage for object with id(" + dbObjectGroupModel.getId() + ")");
        }

        DbVersionsModel dbVersionsModel = CollectHelper.getObjectVersionsModel(dbObjectGroupModel, usage, version);

        if (dbVersionsModel == null) {
            LOGGER.debug("Cannot found version for object  with id({}))", dbObjectGroupModel.getId());
            throw new IllegalArgumentException(
                "Cannot found version for object with id(" + dbObjectGroupModel.getId() + ")");
        }

        String extension = FilenameUtils.getExtension(dbVersionsModel.getFileInfoModel().getFilename()).toLowerCase();
        String fileName = dbVersionsModel.getId() + (extension.equals("") ? "" : "." + extension);
        CountingInputStream countingInputStream = new CountingInputStream(uploadedInputStream);
        String digest = pushStreamToWorkspace(dbObjectGroupModel.getOpi(), countingInputStream,
            CONTENT_FOLDER.concat(File.separator).concat(fileName));
        DbFormatIdentificationModel formatIdentifierResponse =
            getFormatIdentification(dbObjectGroupModel.getOpi(), fileName);

        if (null != formatIdentifierResponse) {
            dbVersionsModel.setFormatIdentificationModel(formatIdentifierResponse);
        }

        int indexQualifier = dbObjectGroupModel.getQualifiers().indexOf(qualifierModelToUpdate);
        int indexVersionsModel = qualifierModelToUpdate.getVersions().indexOf(dbVersionsModel);
        dbVersionsModel.setOpi(dbObjectGroupModel.getOpi());
        dbVersionsModel.setUri(CONTENT_FOLDER + File.separator + fileName);
        dbVersionsModel.setMessageDigest(digest);
        dbVersionsModel.setAlgorithm(DigestType.SHA512.getName());
        dbVersionsModel.setSize(countingInputStream.getByteCount());

        qualifierModelToUpdate.getVersions().set(indexVersionsModel, dbVersionsModel);
        dbObjectGroupModel.getQualifiers().set(indexQualifier, qualifierModelToUpdate);
        try {
            Map<String, JsonNode> action = new HashMap<>();
            action.put(QUALIFIERS.exactToken(), toJsonNode(dbObjectGroupModel.getQualifiers()));
            SetAction setQualifier = new SetAction(action);
            UpdateMultiQuery query = new UpdateMultiQuery();
            query.addHintFilter(OBJECTGROUPS.exactToken());
            query.addActions(setQualifier);
            metadataRepository.updateObjectGroupById(query, dbObjectGroupModel.getId(), dbObjectGroupModel.getOpi());
        } catch (final InvalidParseOperationException | InvalidCreateOperationException e) {
            LOGGER.error("Error when updating existing qualifier: {}", e);
            throw new CollectInternalException("Error when updating existing qualifier: " + e);
        }
    }

    public String pushStreamToWorkspace(String containerName, InputStream uploadedInputStream, String fileName)
        throws CollectInternalException {
        LOGGER.debug("Try to push stream to workspace...");
        try (WorkspaceClient workspaceClient = workspaceCollectClientFactory.getClient()) {
            if (!workspaceClient.isExistingContainer(containerName)) {
                workspaceClient.createContainer(containerName);
                workspaceClient.createFolder(containerName, CONTENT_FOLDER);
            }
            Digest digest = new Digest(VitamConfiguration.getDefaultDigestType());
            InputStream digestInputStream = digest.getDigestInputStream(uploadedInputStream);
            workspaceClient.putObject(containerName, fileName, digestInputStream);
            LOGGER.debug("Push stream to workspace finished");
            return digest.digestHex();
        } catch (ContentAddressableStorageException e) {
            LOGGER.error("Error when trying to push stream to workspace: {} ", e);
            throw new CollectInternalException("Error when trying to push stream to workspace: " + e);
        }
    }

    public InputStream getInputStreamFromWorkspace(String containerName, String fileName)
        throws CollectInternalException {
        try (WorkspaceClient workspaceClient = workspaceCollectClientFactory.getClient()) {
            final Response response = workspaceClient.getObject(containerName, fileName);
            if (response.getStatus() != Response.Status.OK.getStatusCode()) {
                throw new CollectInternalException("Cannot find stream");
            }
            return response.readEntity(InputStream.class);
        } catch (ContentAddressableStorageException e) {
            LOGGER.error("Error while retrieving stream from workspace: {} ", e);
            throw new CollectInternalException("Error while retrieving stream from workspace: " + e);
        }
    }

    public DbFormatIdentificationModel getFormatIdentification(String transactionId, String fileName)
        throws CollectInternalException {
        try (WorkspaceClient workspaceClient = workspaceCollectClientFactory.getClient()) {
            FormatIdentifier formatIdentifier = formatIdentifierFactory.getFormatIdentifierFor(FORMAT_IDENTIFIER_ID);
            if (!workspaceClient.isExistingContainer(transactionId)) {
                return null;
            }
            InputStream is = workspaceClient.getObject(transactionId, CONTENT_FOLDER + File.separator + fileName)
                .readEntity(InputStream.class);
            Path path = Paths.get(VitamConfiguration.getVitamTmpFolder(), fileName);
            Files.copy(is, path);
            File tmpFile = path.toFile();
            final List<FormatIdentifierResponse> formats = formatIdentifier.analysePath(tmpFile.toPath());
            final FormatIdentifierResponse format = CollectHelper.getFirstPronomFormat(formats);
            if (format == null) {
                LOGGER.error("Can't not found format !");
                throw new CollectInternalException("Can't not found format !");
            }
            DbFormatIdentificationModel formatIdentificationModel = new DbFormatIdentificationModel();
            formatIdentificationModel.setFormatId(format.getPuid());
            formatIdentificationModel.setMimeType(format.getMimetype());
            formatIdentificationModel.setFormatLitteral(format.getFormatLiteral());
            Files.delete(path);
            return formatIdentificationModel;
        } catch (ContentAddressableStorageServerException | ContentAddressableStorageNotFoundException |
            FileFormatNotFoundException | FormatIdentifierBadRequestException | IOException |
            FormatIdentifierNotFoundException | FormatIdentifierFactoryException |
            FormatIdentifierTechnicalException e) {
            LOGGER.error("Can't detect format for the object : {}", e);
            throw new CollectInternalException("Can't detect format for the object : " + e);
        }
    }

    public FormatIdentifierResponse detectFileFormat(String transactionId, String fileName)
        throws CollectInternalException {
        try (WorkspaceClient workspaceClient = workspaceCollectClientFactory.getClient()) {
            FormatIdentifier formatIdentifier = formatIdentifierFactory.getFormatIdentifierFor(FORMAT_IDENTIFIER_ID);
            try (InputStream is = workspaceClient.getObject(transactionId, CONTENT_FOLDER + File.separator + fileName)
                .readEntity(InputStream.class)) {
                Path path = Paths.get(VitamConfiguration.getVitamTmpFolder(), fileName);
                Files.copy(is, path);
                File tmpFile = path.toFile();
                final List<FormatIdentifierResponse> formats = formatIdentifier.analysePath(tmpFile.toPath());
                Files.deleteIfExists(path);
                return CollectHelper.getFirstPronomFormat(formats);
            }
        } catch (ContentAddressableStorageNotFoundException | FormatIdentifierNotFoundException | IOException |
            FormatIdentifierBadRequestException | ContentAddressableStorageServerException |
            FormatIdentifierTechnicalException | FormatIdentifierFactoryException |
            FileFormatNotFoundException e) {
            throw new CollectInternalException("Can't detect format for the object : ", e);
        }
    }

    public Response getBinaryByUsageAndVersion(CollectUnitModel unitModel, DataObjectVersionType usage, int version)
        throws StorageNotFoundException, CollectInternalException {

        DbObjectGroupModel dbObjectGroupModel = getDbObjectGroup(unitModel);
        DbVersionsModel finalversionsResponse =
            CollectHelper.getObjectVersionsModel(dbObjectGroupModel, usage, version);

        String filename = null;
        if (finalversionsResponse != null) {
            filename = finalversionsResponse.getUri();
        }
        if (null == filename) {
            LOGGER.error("file name not found");
            throw new CollectInternalException("file name not found");
        }
        try (WorkspaceClient workspaceClient = workspaceCollectClientFactory.getClient()) {
            Response binary = workspaceClient.getObject(unitModel.getOpi(), filename);
            return new VitamAsyncInputStreamResponse(binary, Response.Status.OK,
                MediaType.APPLICATION_OCTET_STREAM_TYPE);
        } catch (ContentAddressableStorageServerException | ContentAddressableStorageNotFoundException e) {
            LOGGER.error("Cannot found got with id ");
            throw new CollectInternalException("Cannot found got with id");
        }
    }
}
