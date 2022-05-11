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
package fr.gouv.vitam.collect.internal.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.collect.internal.dto.ObjectGroupDto;
import fr.gouv.vitam.collect.internal.exception.CollectException;
import fr.gouv.vitam.collect.internal.helpers.CollectHelper;
import fr.gouv.vitam.collect.internal.helpers.adapters.CollectVarNameAdapter;
import fr.gouv.vitam.collect.internal.helpers.builders.DbObjectGroupModelBuilder;
import fr.gouv.vitam.collect.internal.helpers.builders.ObjectMapperBuilder;
import fr.gouv.vitam.collect.internal.helpers.handlers.QueryHandler;
import fr.gouv.vitam.collect.internal.server.CollectConfiguration;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.query.action.SetAction;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.multiple.UpdateMultiQuery;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
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
import fr.gouv.vitam.common.guid.GUIDFactory;
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
import fr.gouv.vitam.common.stream.VitamAsyncInputStreamResponse;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
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
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import fr.gouv.vitam.workspace.client.WorkspaceType;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.FILTERARGS.OBJECTGROUPS;
import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTIONARGS.QUALIFIERS;
import static fr.gouv.vitam.common.json.JsonHandler.toJsonNode;
import static fr.gouv.vitam.common.model.IngestWorkflowConstants.CONTENT_FOLDER;
import static fr.gouv.vitam.common.model.RequestResponseOK.TAG_RESULTS;
import static fr.gouv.vitam.common.model.StatusCode.KO;

public class CollectService {

    public static final String UNABLE_TO_FIND_ARCHIVE_UNIT_ID = "Unable to find archiveUnit Id";
    private static final String TAG_STATUS = "#status";
    private static final String FORMAT_IDENTIFIER_ID = "siegfried-local";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CollectService.class);
    private static final ObjectMapper objectMapper = ObjectMapperBuilder.buildObjectMapper();
    private final TransactionService transactionService;
    private final WorkspaceClientFactory workspaceCollectClientFactory;
    private final MetaDataClientFactory metaDataClientFactory;
    private final FormatIdentifierFactory formatIdentifierFactory;
    private final CollectVarNameAdapter collectVarNameAdapter;
    private final StorageClientFactory storageClientFactory;

    public CollectService(TransactionService transactionService, CollectConfiguration collectConfiguration) {
        this.transactionService = transactionService;
        WorkspaceClientFactory.changeMode(collectConfiguration.getWorkspaceUrl(), WorkspaceType.COLLECT);
        this.workspaceCollectClientFactory = WorkspaceClientFactory.getInstance(WorkspaceType.COLLECT);
        this.metaDataClientFactory = MetaDataClientFactory.getInstance(MetadataType.COLLECT);
        this.formatIdentifierFactory = FormatIdentifierFactory.getInstance();
        this.storageClientFactory = StorageClientFactory.getInstance();
        this.collectVarNameAdapter = new CollectVarNameAdapter();
    }

    @VisibleForTesting
    public CollectService(TransactionService transactionService, MetaDataClientFactory metaDataClientFactory) {
        this.transactionService = transactionService;
        this.workspaceCollectClientFactory = WorkspaceClientFactory.getInstance(WorkspaceType.COLLECT);
        this.metaDataClientFactory = metaDataClientFactory;
        this.formatIdentifierFactory = FormatIdentifierFactory.getInstance();
        this.storageClientFactory = StorageClientFactory.getInstance();
        this.collectVarNameAdapter = new CollectVarNameAdapter();
    }

    public static String createRequestId() {
        String id = GUIDFactory.newRequestIdGUID(VitamThreadUtils.getVitamSession().getTenantId()).getId();
        LOGGER.debug("Generated Request Id : {}", id);
        return id;
    }

    public void checkParameters(String unitId, DataObjectVersionType usage, Integer version) {
        if (usage == null || unitId == null || version == null) {
            LOGGER.error("usage({}), unitId({}) or version({}) can't be null", usage, unitId, version);
            throw new IllegalArgumentException("usage, unitId or version can't be null");
        }
    }

    public ArchiveUnitModel getArchiveUnitModel(String unitId) throws CollectException {

        try (MetaDataClient metaDataClient = metaDataClientFactory.getClient()) {
            JsonNode jsonNode = metaDataClient.selectUnitbyId(new Select().getFinalSelect(), unitId);
            if (jsonNode == null || !jsonNode.has(TAG_RESULTS) || jsonNode.get(TAG_RESULTS).size() == 0) {
                LOGGER.error("Can't get unit by ID: {}" + unitId);
                throw new CollectException("Can't get unit by ID: " + unitId);
            }
            ArchiveUnitModel archiveUnitModel =
                objectMapper.convertValue(jsonNode.get(TAG_RESULTS).get(0), ArchiveUnitModel.class);
            if (archiveUnitModel == null) {
                LOGGER.error(UNABLE_TO_FIND_ARCHIVE_UNIT_ID);
                throw new CollectException(UNABLE_TO_FIND_ARCHIVE_UNIT_ID);
            }
            return archiveUnitModel;
        } catch (CollectException | MetaDataExecutionException | MetaDataDocumentSizeException
            | InvalidParseOperationException | MetaDataClientServerException e) {
            LOGGER.error("Error when fetching unit by id({}): {} ", unitId, e);
            throw new CollectException("Error when fetching unit by id(" + unitId + ") " + e);
        }
    }

    public ObjectGroupDto saveObjectGroupInMetaData(ArchiveUnitModel archiveUnitModel, DataObjectVersionType usage,
        int version,
        ObjectGroupDto objectGroupDto) throws CollectException {

        try {
            objectGroupDto.setId(createRequestId());
            if (archiveUnitModel.getOg() == null) {
                insertNewObject(archiveUnitModel, usage, version, objectGroupDto);
            } else {
                updateExistingObject(archiveUnitModel, usage, version, objectGroupDto);
            }
            return objectGroupDto;
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
                LOGGER.error("Cannot find got with id({}))", archiveUnitModel.getOg());
                throw new CollectException("Cannot found got with id(" + archiveUnitModel.getOg() + ")");
            }
            JsonNode firstResult = ((RequestResponseOK<JsonNode>) requestResponse).getFirstResult();
            DbObjectGroupModel dbObjectGroupModel = JsonHandler.getFromJsonNode(firstResult, DbObjectGroupModel.class);
            DbQualifiersModel qualifierModelToUpdate =
                CollectHelper.findQualifier(dbObjectGroupModel.getQualifiers(), usage);

            if (qualifierModelToUpdate == null) {
                CollectHelper.checkVersion(version, 1);
                addQualifierToObjectGroups(dbObjectGroupModel, usage, version,
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
                .withQualifiers(createRequestId(), objectGroupDto.getFileInfo().getFileName(), usage,
                    version)
                .build();

            final ObjectNode insertRequest = QueryHandler.insertObjectMultiQuery(dbObjectGroupModel);
            JsonNode jsonNode = client.insertObjectGroup(insertRequest);
            if (jsonNode == null) {
                LOGGER.error("Error when trying to insert ObjectGroup : {})", insertRequest);
                throw new CollectException("Error when trying to insert ObjectGroup : : " + insertRequest);
            }

            JsonNode firstResult = QueryHandler.updateUnitMultiQuery(archiveUnitModel, client, objectGroupDto.getId());

            if (firstResult != null && firstResult.has(TAG_STATUS) &&
                firstResult.get(TAG_STATUS).textValue().equals(KO.name())) {
                //TODO : Manage Object Group rollback
                LOGGER.error("Update Unit with object group id  failed on id : ", archiveUnitModel.getId());
                throw new CollectException("Update Unit with object group id : " + archiveUnitModel.getId());
            }
        } catch (final CollectException | MetaDataExecutionException | MetaDataNotFoundException
            | MetaDataDocumentSizeException | MetaDataClientServerException | InvalidCreateOperationException
            | InvalidParseOperationException e) {
            LOGGER.error("Error when saving new objectGroup in metadata : {}", e);
            throw new CollectException("Error when saving new objectGroup in metadata: " + e);
        }
    }

    public void addQualifierToObjectGroups(DbObjectGroupModel objectGroup, DataObjectVersionType usage, int version,
        ObjectGroupDto objectGroupDto) throws CollectException {

        try (MetaDataClient client = metaDataClientFactory.getClient()) {
            String versionId = createRequestId();
            UpdateMultiQuery query =
                QueryHandler.getQualifiersAddMultiQuery(usage, version, objectGroup.getQualifiers(), objectGroupDto,
                    versionId,
                    objectGroup.getNbc());
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
            String versionId = createRequestId();
            UpdateMultiQuery query = QueryHandler.getQualifiersUpdateMultiQuery(qualifierModelToUpdate,
                usage, version, qualifiers, objectGroupDto, versionId, objectGroup.getNbc());

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
        int version, InputStream uploadedInputStream, String fileUri) throws CollectException {

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
        String fileCompletePath = fileUri + "/" + fileName;
        CountingInputStream countingInputStream = new CountingInputStream(uploadedInputStream);
        String digest = pushStreamToWorkspace(dbObjectGroupModel.getOpi(), countingInputStream, fileCompletePath);
        DbFormatIdentificationModel formatIdentifierResponse =
            getFormatIdentification(dbObjectGroupModel.getOpi(), fileName, fileCompletePath);

        if (null != formatIdentifierResponse) {
            dbVersionsModel.setFormatIdentificationModel(formatIdentifierResponse);
        }

        int indexQualifier = dbObjectGroupModel.getQualifiers().indexOf(qualifierModelToUpdate);
        int indexVersionsModel = qualifierModelToUpdate.getVersions().indexOf(dbVersionsModel);
        dbVersionsModel.setOpi(dbObjectGroupModel.getOpi());
        dbVersionsModel.setUri(CONTENT_FOLDER + "/" + fileCompletePath);
        dbVersionsModel.setMessageDigest(digest);
        dbVersionsModel.setAlgorithm(DigestType.SHA512.getName());
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
        try (WorkspaceClient workspaceClient = workspaceCollectClientFactory.getClient()) {
            if (!workspaceClient.isExistingContainer(containerName)) {
                workspaceClient.createContainer(containerName);
                workspaceClient.createFolder(containerName, CONTENT_FOLDER);
            }
            Digest digest = new Digest(VitamConfiguration.getDefaultDigestType());
            InputStream digestInputStream = digest.getDigestInputStream(uploadedInputStream);
            workspaceClient.putObject(containerName, CONTENT_FOLDER.concat("/").concat(fileName), digestInputStream);
            LOGGER.debug("Push stream to workspace finished");
            return digest.digestHex();
        } catch (ContentAddressableStorageException e) {
            LOGGER.error("Error when trying to push stream to workspace: {} ", e);
            throw new CollectException("Error when trying to push stream to workspace: " + e);
        }
    }

    public JsonNode saveArchiveUnitInMetaData(JsonNode unitJsonDto) throws CollectException {
        try (MetaDataClient client = metaDataClientFactory.getClient()) {
            ObjectNode unitJson = JsonHandler.createObjectNode();
            this.collectVarNameAdapter.setVarsValue(unitJson, unitJsonDto);
            List<BulkUnitInsertEntry> units = CollectHelper.fetchBulkUnitInsertEntries(unitJson);
            return client.insertUnitBulk(new BulkUnitInsertRequest(units));
        } catch (final MetaDataException | InvalidParseOperationException e) {
            LOGGER.error("Error when saving unit in metadata: {}", e);
            throw new CollectException("Error when saving unit in metadata: " + e);
        }
    }

    public DbFormatIdentificationModel getFormatIdentification(String transactionId, String fileName, String fileUri)
        throws CollectException {
        FormatIdentifier formatIdentifier;
        try (WorkspaceClient workspaceClient = workspaceCollectClientFactory.getClient()) {
            formatIdentifier = formatIdentifierFactory.getFormatIdentifierFor(FORMAT_IDENTIFIER_ID);
            if (!workspaceClient.isExistingContainer(transactionId)) {
                return null;
            }
            InputStream is =
                workspaceClient.getObject(transactionId, CONTENT_FOLDER + "/" + fileUri)
                    .readEntity(InputStream.class);
            Path path = Paths.get(VitamConfiguration.getVitamTmpFolder(), fileName);
            Files.copy(is, path);
            File tmpFile = path.toFile();
            final List<FormatIdentifierResponse> formats = formatIdentifier.analysePath(tmpFile.toPath());
            final FormatIdentifierResponse format = CollectHelper.getFirstPronomFormat(formats);
            if (format == null) {
                LOGGER.error("Can't not found format !");
                throw new CollectException("Can't not found format !");
            }
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

    public JsonNode getUnitsByTransactionIdInMetaData(String transactionId) throws CollectException {
        try (MetaDataClient client = metaDataClientFactory.getClient()) {
            SelectMultiQuery selectUnit = new SelectMultiQuery();
            selectUnit.addQueries(QueryHelper.eq(VitamFieldsHelper.initialOperation(), transactionId));
            selectUnit.setLimitFilter(0, VitamConfiguration.getBatchSize());
            return client.selectUnits(selectUnit.getFinalSelect());
        } catch (final MetaDataException | InvalidParseOperationException | InvalidCreateOperationException e) {
            LOGGER.error("Error when getting units in metadata: {}", e);
            throw new CollectException("Error when getting units in metadata: " + e);
        }
    }

    public JsonNode getUnitByIdInMetaData(String unitId) throws CollectException {
        try (MetaDataClient client = metaDataClientFactory.getClient()) {
            return client.selectUnitbyId(new Select().getFinalSelect(), unitId);
        } catch (final MetaDataException | InvalidParseOperationException e) {
            LOGGER.error("Error when fetching unit in metadata: {}", e);
            throw new CollectException("Error when fetching unit in metadata: " + e);
        }
    }

    public JsonNode getObjectGroupByIdInMetaData(String objectGroupId) throws CollectException {
        try (MetaDataClient client = metaDataClientFactory.getClient()) {
            RequestResponse<JsonNode> objectGroupResponse = client.getObjectGroupByIdRaw(objectGroupId);
            return objectGroupResponse.toJsonNode();
        } catch (final VitamClientException e) {
            LOGGER.error("Cannot found got with id({}))", objectGroupId);
            throw new CollectException("Cannot found got with id(" + objectGroupId + ")");
        }
    }


    public Response getBinaryByUsageAndVersion(ArchiveUnitModel archiveUnitModel, DataObjectVersionType usage,
        int version)
        throws StorageNotFoundException, CollectException {

        DbObjectGroupModel dbObjectGroupModel = getDbObjectGroup(archiveUnitModel);
        DbVersionsModel finalversionsResponse =
            CollectHelper.getObjectVersionsModel(dbObjectGroupModel, usage, version);

        String filename = null;
        if (finalversionsResponse != null) {
            filename = finalversionsResponse.getUri();
        }
        if (null == filename) {
            LOGGER.error("file name not found");
            throw new CollectException("file name not found");
        }
        try (WorkspaceClient workspaceClient = workspaceCollectClientFactory.getClient()) {
            Response binary = workspaceClient.getObject(archiveUnitModel.getOpi(), filename);
            return new VitamAsyncInputStreamResponse(binary, Response.Status.OK,
                MediaType.APPLICATION_OCTET_STREAM_TYPE);
        } catch (ContentAddressableStorageServerException | ContentAddressableStorageNotFoundException e) {
            LOGGER.error("Cannot found got with id ");
            throw new CollectException("Cannot found got with id");
        }

    }
}
