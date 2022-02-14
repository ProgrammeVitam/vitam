/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
package fr.gouv.vitam.collect.internal.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.collect.internal.dto.ObjectGroupDto;
import fr.gouv.vitam.collect.internal.exception.CollectException;
import fr.gouv.vitam.collect.internal.helpers.CollectHelper;
import fr.gouv.vitam.collect.internal.helpers.adapters.CollectVarNameAdapter;
import fr.gouv.vitam.collect.internal.helpers.builders.DbObjectGroupModelBuilder;
import fr.gouv.vitam.collect.internal.helpers.builders.ObjectMapperBuilder;
import fr.gouv.vitam.collect.internal.helpers.handlers.QueryHandler;
import fr.gouv.vitam.collect.internal.server.CollectConfiguration;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.query.action.SetAction;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.UpdateMultiQuery;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.format.identification.FormatIdentifier;
import fr.gouv.vitam.common.format.identification.FormatIdentifierFactory;
import fr.gouv.vitam.common.format.identification.exception.FileFormatNotFoundException;
import fr.gouv.vitam.common.format.identification.exception.FormatIdentifierBadRequestException;
import fr.gouv.vitam.common.format.identification.exception.FormatIdentifierFactoryException;
import fr.gouv.vitam.common.format.identification.exception.FormatIdentifierNotFoundException;
import fr.gouv.vitam.common.format.identification.exception.FormatIdentifierTechnicalException;
import fr.gouv.vitam.common.format.identification.model.FormatIdentifierResponse;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.DataObjectVersionType;
import fr.gouv.vitam.common.model.objectgroup.DbFormatIdentificationModel;
import fr.gouv.vitam.common.model.objectgroup.DbObjectGroupModel;
import fr.gouv.vitam.common.model.objectgroup.DbQualifiersModel;
import fr.gouv.vitam.common.model.objectgroup.DbVersionsModel;
import fr.gouv.vitam.common.model.unit.ArchiveUnitModel;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.metadata.api.model.BulkUnitInsertEntry;
import fr.gouv.vitam.metadata.api.model.BulkUnitInsertRequest;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.metadata.client.MetadataType;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import fr.gouv.vitam.workspace.client.WorkspaceType;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.input.CountingInputStream;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.FILTERARGS.OBJECTGROUPS;
import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTIONARGS.QUALIFIERS;
import static fr.gouv.vitam.common.json.JsonHandler.toJsonNode;
import static fr.gouv.vitam.common.model.RequestResponseOK.TAG_RESULTS;
import static fr.gouv.vitam.common.model.StatusCode.KO;

public class TransactionService {

    public static final String SHA_512 = "SHA-512";
    private static final String FOLDER_CONTENT = "Content";
    private static final String TAG_STATUS = "#status";
    private static final String FORMAT_IDENTIFIER_ID = "siegfried-local";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(TransactionService.class);

    private final CollectService collectService;
    private final WorkspaceClientFactory workspaceCollectClientFactory;
    private final MetaDataClientFactory metaDataClientFactory;
    private final FormatIdentifierFactory formatIdentifierFactory;
    private static final ObjectMapper objectMapper = ObjectMapperBuilder.buildObjectMapper();
    private final CollectVarNameAdapter collectVarNameAdapter;

    public TransactionService(CollectService collectService, CollectConfiguration collectConfiguration) {
        this.collectService = collectService;
        WorkspaceClientFactory.changeMode(collectConfiguration.getWorkspaceUrl(), WorkspaceType.COLLECT);
        this.workspaceCollectClientFactory = WorkspaceClientFactory.getInstance(WorkspaceType.COLLECT);
        this.metaDataClientFactory = MetaDataClientFactory.getInstance(MetadataType.COLLECT);
        this.formatIdentifierFactory = FormatIdentifierFactory.getInstance();
        this.collectVarNameAdapter = new CollectVarNameAdapter();
    }

    public void saveObjectGroupInMetaData(ArchiveUnitModel archiveUnitModel, DataObjectVersionType usage, int version,
        ObjectGroupDto objectGroupDto) throws CollectException {

        try {
            objectGroupDto.setId(collectService.createRequestId());
            if (archiveUnitModel.getOg() == null) {
                insertNewObject(archiveUnitModel, usage, version, objectGroupDto);
            } else {
                updateExistingObject(archiveUnitModel, usage, version, objectGroupDto);
            }
        } catch (CollectException e) {
            LOGGER.error("Error when saving Object in metadata: {}", e);
            throw new CollectException(e);
        }
    }

    private void updateExistingObject(ArchiveUnitModel archiveUnitModel, DataObjectVersionType usage, int version,
        ObjectGroupDto objectGroupDto) throws CollectException {

        try (MetaDataClient client = metaDataClientFactory.getClient()) {
            RequestResponse<JsonNode> requestResponse = client.getObjectGroupByIdRaw(archiveUnitModel.getOg());
            if (!requestResponse.isOk()) {
                LOGGER.error("Cannot found got with id({}))", archiveUnitModel.getOg());
                throw new CollectException("Cannot found got with id(" + archiveUnitModel.getOg() + ")");
            }
            JsonNode firstResult = ((RequestResponseOK<JsonNode>) requestResponse).getFirstResult();
            DbObjectGroupModel dbObjectGroupModel = JsonHandler.getFromJsonNode(firstResult, DbObjectGroupModel.class);
            DbQualifiersModel qualifierModelToUpdate =
                CollectHelper.findQualifier(dbObjectGroupModel.getQualifiers(), usage);

            if (qualifierModelToUpdate == null) {
                CollectHelper.checkVersion(version, 1);
                addQualifierToObjectGroups(dbObjectGroupModel, usage, version, dbObjectGroupModel.getQualifiers(),
                    objectGroupDto);
            } else {
                DbVersionsModel dbVersionsModel =
                    CollectHelper.getObjectVersionsModel(dbObjectGroupModel, usage, version);

                if (dbVersionsModel != null) {
                    LOGGER.error("Qualifier already exist with usage {} and version {})", usage, version);
                    throw new CollectException("Qualifier already exist with usage " + usage + " and version " +
                        version + "");
                }

                int lastVersion = CollectHelper.getLastVersion(qualifierModelToUpdate) + 1;
                CollectHelper.checkVersion(version, lastVersion);
                addVersionToObjectGroups(qualifierModelToUpdate, dbObjectGroupModel, usage, lastVersion,
                    dbObjectGroupModel.getQualifiers(), objectGroupDto);
            }
        } catch (VitamClientException | InvalidParseOperationException e) {
            LOGGER.error("Error when updating existing Object : {}", e);
            throw new CollectException(e);
        }
    }

    private void insertNewObject(ArchiveUnitModel archiveUnitModel, DataObjectVersionType usage, int version,
        ObjectGroupDto objectGroupDto) throws CollectException {

        try (MetaDataClient client = metaDataClientFactory.getClient()) {
            CollectHelper.checkVersion(version, 1);
            DbObjectGroupModel dbObjectGroupModel = new DbObjectGroupModelBuilder()
                .withId(objectGroupDto.getId())
                .withOpi(archiveUnitModel.getOpi())
                .withFileInfoModel(objectGroupDto.getFileInfo().getFileName())
                .withQualifiers(collectService.createRequestId(), objectGroupDto.getFileInfo().getFileName(), usage,
                    version)
                .build();

            final ObjectNode insertRequest = QueryHandler.insertObjectMultiQuery(dbObjectGroupModel);
            JsonNode jsonNode = client.insertObjectGroup(insertRequest);
            if (jsonNode == null) {
                LOGGER.error("Error when trying to insert ObjectGroup : {})", insertRequest);
                throw new CollectException("Error when trying to insert ObjectGroup : : " + insertRequest);
            }

            JsonNode firstResult = QueryHandler.updateUnitMultiQuery(archiveUnitModel, objectGroupDto, client);

            if (firstResult != null && firstResult.has(TAG_STATUS) &&
                firstResult.get(TAG_STATUS).textValue().equals(KO.name())) {
                //TODO : Manage Object Group rollback
                LOGGER.error("Unit update failed on id : ", archiveUnitModel.getId());
                throw new CollectException("Unit update failed on id : " + archiveUnitModel.getId());
            }
        } catch (final CollectException | MetaDataExecutionException | MetaDataNotFoundException
            | MetaDataDocumentSizeException | MetaDataClientServerException | InvalidCreateOperationException
            | InvalidParseOperationException e) {
            LOGGER.error("Error when saving new objectGroup in metadata : {}", e);
            throw new CollectException("Error when saving new objectGroup in metadata: " + e);
        }
    }

    public void addQualifierToObjectGroups(DbObjectGroupModel objectGroup, DataObjectVersionType usage, int version,
        List<DbQualifiersModel> qualifiers, ObjectGroupDto objectGroupDto) throws CollectException {

        try (MetaDataClient client = metaDataClientFactory.getClient()) {
            String versionId = collectService.createRequestId();
            UpdateMultiQuery query =
                QueryHandler.getQualifiersAddMultiQuery(usage, version, qualifiers, objectGroupDto, versionId,
                    objectGroup);
            client.updateObjectGroupById(query.getFinalUpdate(), objectGroup.getId());
        } catch (final MetaDataException | InvalidParseOperationException | InvalidCreateOperationException e) {
            LOGGER.error("Error when adding usage/version to existing qualifier: {}", e);
            throw new CollectException("Error when adding usage/version to existing qualifier: " + e);
        }
    }

    public void addVersionToObjectGroups(DbQualifiersModel qualifierModelToUpdate, DbObjectGroupModel objectGroup,
        DataObjectVersionType usage, int version, List<DbQualifiersModel> qualifiers, ObjectGroupDto objectGroupDto)
        throws CollectException {

        try (MetaDataClient client = metaDataClientFactory.getClient()) {
            String versionId = collectService.createRequestId();
            UpdateMultiQuery query = QueryHandler.getQualifiersUpdateMultiQuery(qualifierModelToUpdate, objectGroup,
                usage, version, qualifiers, objectGroupDto, versionId);

            client.updateObjectGroupById(query.getFinalUpdate(), objectGroup.getId());
        } catch (final MetaDataException | InvalidParseOperationException | InvalidCreateOperationException e) {
            LOGGER.error("Error when adding version to Object: {}", e);
            throw new CollectException("Error when adding version to Object: " + e);
        }
    }

    public DbObjectGroupModel getDbObjectGroup(ArchiveUnitModel archiveUnitModel) throws CollectException {
        try (MetaDataClient client = metaDataClientFactory.getClient()) {
            if (archiveUnitModel.getOg() == null) {
                LOGGER.debug("Cannot found any got attached to unit with id({}))", archiveUnitModel.getId());
                throw new IllegalArgumentException(
                    "Cannot found any object attached to unit with id(" + archiveUnitModel.getId() + ")");
            }
            RequestResponse<JsonNode> requestResponse = client.getObjectGroupByIdRaw(archiveUnitModel.getOg());
            if (!requestResponse.isOk()) {
                LOGGER.debug("Cannot found object with id({}))", archiveUnitModel.getOg());
                throw new IllegalArgumentException("Cannot found object with id(" + archiveUnitModel.getOg() + ")");
            }
            JsonNode firstResult = ((RequestResponseOK<JsonNode>) requestResponse).getFirstResult();
            return objectMapper.convertValue(firstResult, DbObjectGroupModel.class);
        } catch (VitamClientException e) {
            LOGGER.error("Error when fetching Object from metadata: {}", e);
            throw new CollectException("Error when fetching Object from metadata: " + e);
        }
    }

    public void addBinaryInfoToQualifier(DbObjectGroupModel dbObjectGroupModel, DataObjectVersionType usage,
        int version, InputStream uploadedInputStream) throws CollectException {

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
        String digest = pushStreamToWorkspace(dbObjectGroupModel.getOpi(), countingInputStream, fileName);
        DbFormatIdentificationModel formatIdentifierResponse =
            getFormatIdentification(dbObjectGroupModel.getOpi(), fileName);

        if (null != formatIdentifierResponse) {
            dbVersionsModel.setFormatIdentificationModel(formatIdentifierResponse);
        }

        int indexQualifier = dbObjectGroupModel.getQualifiers().indexOf(qualifierModelToUpdate);
        int indexVersionsModel = qualifierModelToUpdate.getVersions().indexOf(dbVersionsModel);
        dbVersionsModel.setOpi(dbObjectGroupModel.getOpi());
        dbVersionsModel.setUri(FOLDER_CONTENT + "/" + fileName);
        dbVersionsModel.setMessageDigest(digest);
        dbVersionsModel.setAlgorithm(SHA_512);
        dbVersionsModel.setSize(countingInputStream.getByteCount());

        qualifierModelToUpdate.getVersions().set(indexVersionsModel, dbVersionsModel);
        dbObjectGroupModel.getQualifiers().set(indexQualifier, qualifierModelToUpdate);
        try (MetaDataClient client = metaDataClientFactory.getClient()) {
            Map<String, JsonNode> action = new HashMap<>();
            action.put(QUALIFIERS.exactToken(), toJsonNode(dbObjectGroupModel.getQualifiers()));
            SetAction setQualifier = new SetAction(action);
            UpdateMultiQuery query = new UpdateMultiQuery();
            query.addHintFilter(OBJECTGROUPS.exactToken());
            query.addActions(setQualifier);

            client.updateObjectGroupById(query.getFinalUpdate(), dbObjectGroupModel.getId());
        } catch (final MetaDataException | InvalidParseOperationException | InvalidCreateOperationException e) {
            LOGGER.error("Error when updating existing qualifier: {}", e);
            throw new CollectException("Error when updating existing qualifier: " + e);
        }
    }

    public String pushStreamToWorkspace(String containerName, InputStream uploadedInputStream, String fileName)
        throws CollectException {
        LOGGER.debug("Try to push stream to workspace...");
        String digest;
        try (WorkspaceClient workspaceClient = workspaceCollectClientFactory.getClient()) {
            if (!workspaceClient.isExistingContainer(containerName)) {
                workspaceClient.createContainer(containerName);
                workspaceClient.createFolder(containerName, FOLDER_CONTENT);
            }
            MessageDigest messageDigest = MessageDigest.getInstance(SHA_512);
            uploadedInputStream = new DigestInputStream(uploadedInputStream, messageDigest);
            workspaceClient.putObject(containerName, FOLDER_CONTENT.concat("/").concat(fileName), uploadedInputStream);
            digest = CollectHelper.readMessageDigestReturn(messageDigest.digest());
        } catch (ContentAddressableStorageException | NoSuchAlgorithmException e) {
            LOGGER.error("Error when trying to push stream to workspace: {} ", e);
            throw new CollectException("Error when trying to push stream to workspace: " + e);
        }
        LOGGER.debug("Push stream to workspace finished");
        return digest;
    }

    public JsonNode saveArchiveUnitInMetaData(JsonNode unitJsonDto) throws CollectException {
        try (MetaDataClient client = metaDataClientFactory.getClient()) {
            ObjectNode unitJson = JsonHandler.createObjectNode();
            this.collectVarNameAdapter.setVarsValue(unitJson, unitJsonDto);
            List<BulkUnitInsertEntry> units;
            if (null != unitJson.get("_up") && unitJson.get("_up").size() != 0) {
                Set<String> parentUnitIds = StreamSupport
                    .stream(unitJson.get("_up").spliterator(), false)
                    .map(JsonNode::asText)
                    .collect(Collectors.toSet());
                units = Collections.singletonList(new BulkUnitInsertEntry(parentUnitIds, unitJson));
            } else {
                units = Collections.singletonList(new BulkUnitInsertEntry(Collections.emptySet(), unitJson));
            }
            return client.insertUnitBulk(new BulkUnitInsertRequest(units));
        } catch (final MetaDataException | InvalidParseOperationException e) {
            LOGGER.error("Error when saving unit in metadata: {}", e);
            throw new CollectException("Error when saving unit in metadata: " + e);
        }
    }

    public ArchiveUnitModel getArchiveUnitById(String unitId) throws CollectException {
        try (MetaDataClient client = metaDataClientFactory.getClient()) {
            JsonNode jsonNode = client.selectUnitbyId(new Select().getFinalSelect(), unitId);
            if (jsonNode == null || !jsonNode.has(TAG_RESULTS) || jsonNode.get(TAG_RESULTS).size() == 0) {
                LOGGER.error("Can't get unit by ID: {}" + unitId);
                throw new CollectException("Can't get unit by ID: " + unitId);
            }
            return objectMapper.convertValue(jsonNode.get(TAG_RESULTS).get(0), ArchiveUnitModel.class);
        } catch (CollectException | MetaDataExecutionException | MetaDataDocumentSizeException
            | InvalidParseOperationException | MetaDataClientServerException e) {
            LOGGER.error("Error when fetching unit by id({}): {} ", unitId, e);
            throw new CollectException("Error when fetching unit by id("+ unitId +") " + e);
        }
    }

    public DbFormatIdentificationModel getFormatIdentification(String transactionId, String objectName)
        throws CollectException {
        FormatIdentifier formatIdentifier;
        try (WorkspaceClient workspaceClient = workspaceCollectClientFactory.getClient()) {
            formatIdentifier = formatIdentifierFactory.getFormatIdentifierFor(FORMAT_IDENTIFIER_ID);
            if (!workspaceClient.isExistingContainer(transactionId)) {
                return null;
            }
            InputStream is =
                workspaceClient.getObject(transactionId, "Content/" + objectName).readEntity(InputStream.class);
            Path path = Paths.get(VitamConfiguration.getVitamTmpFolder(), objectName);
            Files.copy(is, path);
            File tmpFile = path.toFile();
            final List<FormatIdentifierResponse> formats = formatIdentifier.analysePath(tmpFile.toPath());
            final FormatIdentifierResponse format = CollectHelper.getFirstPronomFormat(formats);
            DbFormatIdentificationModel formatIdentificationModel = new DbFormatIdentificationModel();
            formatIdentificationModel.setFormatId(format.getPuid());
            formatIdentificationModel.setMimeType(format.getMimetype());
            formatIdentificationModel.setFormatLitteral(format.getFormatLiteral());
            Files.delete(path);
            return formatIdentificationModel;

        } catch (ContentAddressableStorageServerException | ContentAddressableStorageNotFoundException
            | FileFormatNotFoundException | FormatIdentifierBadRequestException | IOException
            | FormatIdentifierNotFoundException | FormatIdentifierFactoryException | FormatIdentifierTechnicalException e) {
            LOGGER.error("Can't detect format for the object : {}", e);
            throw new CollectException("Can't detect format for the object : " + e);
        }
    }

}
