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
import fr.gouv.vitam.collect.internal.dto.CollectUnitDto;
import fr.gouv.vitam.collect.internal.dto.ObjectGroupDto;
import fr.gouv.vitam.collect.internal.exception.CollectException;
import fr.gouv.vitam.collect.internal.helpers.DbObjectGroupModelBuilder;
import fr.gouv.vitam.collect.internal.helpers.ObjectMapperBuilder;
import fr.gouv.vitam.collect.internal.helpers.QueryHandler;
import fr.gouv.vitam.collect.internal.helpers.TransactionHelper;
import fr.gouv.vitam.collect.internal.mappers.CollectUnitMapper;
import fr.gouv.vitam.collect.internal.model.UnitModel;
import fr.gouv.vitam.collect.internal.server.CollectConfiguration;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.query.action.SetAction;
import fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.InsertMultiQuery;
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

import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.FILTERARGS.OBJECTGROUPS;
import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTIONARGS.QUALIFIERS;
import static fr.gouv.vitam.common.json.JsonHandler.toJsonNode;
import static fr.gouv.vitam.common.model.RequestResponseOK.TAG_RESULTS;
import static fr.gouv.vitam.common.model.StatusCode.KO;

public class TransactionService {

    private final CollectService collectService;
    private final WorkspaceClientFactory workspaceClientFactory;
    private final MetaDataClientFactory metaDataClientFactory;
    private final FormatIdentifierFactory formatIdentifierFactory;
    private static final ObjectMapper objectMapper = ObjectMapperBuilder.buildObjectMapper();

    private static final String FOLDER_CONTENT = "Content";
    private static final String TAG_STATUS = "#status";
    private static final String FORMAT_IDENTIFIER_ID = "siegfried-local";

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(TransactionService.class);

    public TransactionService(CollectService collectService, CollectConfiguration collectConfiguration) {
        this.collectService = collectService;
        WorkspaceClientFactory.changeMode(collectConfiguration.getWorkspaceUrl());
        this.workspaceClientFactory = WorkspaceClientFactory.getInstance(WorkspaceType.COLLECT);
        this.metaDataClientFactory = MetaDataClientFactory.getInstance(MetadataType.COLLECT);
        this.formatIdentifierFactory = FormatIdentifierFactory.getInstance();
    }

    public void saveObjectGroupInMetaData(ArchiveUnitModel archiveUnitModel, String qualifier, int version,
        ObjectGroupDto objectGroupDto) throws CollectException {

        try {
            if (archiveUnitModel.getOg() == null) {
                insertNewObject(archiveUnitModel, qualifier, version, objectGroupDto);
            } else {
                updateExistingObject(archiveUnitModel, qualifier, version, objectGroupDto);
            }
        } catch (InvalidParseOperationException | CollectException e) {
            LOGGER.debug("Error : {}", e);
            throw new CollectException(e);
        }
    }

    private void updateExistingObject(ArchiveUnitModel archiveUnitModel, String qualifier, int version,
        ObjectGroupDto objectGroupDto) throws InvalidParseOperationException, CollectException {

        try (MetaDataClient client = metaDataClientFactory.getClient()) {
            RequestResponse<JsonNode> requestResponse = client.getObjectGroupByIdRaw(archiveUnitModel.getOg());
            if (!requestResponse.isOk()) {
                LOGGER.debug("Cannot found got with id({}))", archiveUnitModel.getOg());
                throw new IllegalArgumentException("Cannot found got with id(" + archiveUnitModel.getOg() + ")");
            }
            JsonNode firstResult = ((RequestResponseOK<JsonNode>) requestResponse).getFirstResult();
            DbObjectGroupModel dbObjectGroupModel = JsonHandler.getFromJsonNode(firstResult, DbObjectGroupModel.class);
            DbQualifiersModel qualifierModelToUpdate =
                TransactionHelper.findQualifier(dbObjectGroupModel.getQualifiers(), qualifier);

            if (qualifierModelToUpdate == null) {
                TransactionHelper.checkVersion(version, 1);
                addQualifierToObjectGroups(dbObjectGroupModel, qualifier, version, dbObjectGroupModel.getQualifiers(),
                    objectGroupDto);
            } else {
                DbVersionsModel dbVersionsModel =
                    TransactionHelper.getObjectVersionsModel(dbObjectGroupModel, qualifier, version);

                if (dbVersionsModel != null) {
                    LOGGER.debug("Qualifier already exist with qualifier {} and version {})", qualifier, version);
                    throw new IllegalArgumentException(
                        "Qualifier already exist with qualifier " + qualifier + " and version " +
                            version + "");
                }

                int lastVersion = TransactionHelper.getLastVersion(qualifierModelToUpdate) + 1;
                TransactionHelper.checkVersion(version, lastVersion);
                addVersionToObjectGroups(qualifierModelToUpdate, dbObjectGroupModel, qualifier, lastVersion,
                    dbObjectGroupModel.getQualifiers(),
                    objectGroupDto);
            }
        } catch (VitamClientException | InvalidCreateOperationException | MetaDataExecutionException | MetaDataClientServerException e) {
            LOGGER.debug("Error : {}", e);
            throw new CollectException(e);
        }
    }

    private void insertNewObject(ArchiveUnitModel archiveUnitModel, String qualifier, int version,
        ObjectGroupDto objectGroupDto) throws InvalidParseOperationException, CollectException {

        TransactionHelper.checkVersion(version, 1);
        DbObjectGroupModel dbObjectGroupModel = new DbObjectGroupModelBuilder()
            .withId(objectGroupDto.getId())
            .withOpi(archiveUnitModel.getOpi())
            .withFileInfoModel(objectGroupDto.getFileInfo().getFileName())
            .withQualifiers(collectService.createRequestId(), objectGroupDto.getFileInfo().getFileName(), qualifier,
                version)
            .build();

        final InsertMultiQuery insert = new InsertMultiQuery();
        insert.resetFilter();
        insert.addHintFilter(BuilderToken.FILTERARGS.OBJECTGROUPS.exactToken());
        insert.addData((ObjectNode) JsonHandler.toJsonNode(dbObjectGroupModel));
        final ObjectNode insertRequest = insert.getFinalInsert();

        try (MetaDataClient client = metaDataClientFactory.getClient()) {
            JsonNode jsonNode = client.insertObjectGroup(insertRequest);
            if (jsonNode == null) {
                LOGGER.debug("Error when trying to insert ObjectGroup : {})", insertRequest);
                throw new CollectException("Error when trying to insert ObjectGroup : : " + insertRequest);
            }

            UpdateMultiQuery multiQuery = new UpdateMultiQuery();
            multiQuery.addActions(UpdateActionHelper.set(VitamFieldsHelper.object(), objectGroupDto.getId()));
            multiQuery.resetRoots().addRoots(archiveUnitModel.getId());
            RequestResponse<JsonNode> requestResponse = client.updateUnitBulk(multiQuery.getFinalUpdate());
            JsonNode firstResult = ((RequestResponseOK<JsonNode>) requestResponse).getFirstResult();

            if (firstResult != null && firstResult.has(TAG_STATUS) && firstResult.get(TAG_STATUS).textValue().equals(KO.name())) {
                //TODO : Manage Object Group rollback
                LOGGER.debug("Unit update failed on id : ", archiveUnitModel.getId());
                throw new CollectException("Unit update failed on id : " + archiveUnitModel.getId());
            }
        } catch (final CollectException | MetaDataExecutionException | MetaDataNotFoundException
            | MetaDataDocumentSizeException | MetaDataClientServerException | InvalidCreateOperationException e) {
            LOGGER.error("Can't save objectGroup by unit ID : {}", e);
            throw new CollectException("Can't save objectGroup by unit ID");
        }
    }

    public void addQualifierToObjectGroups(DbObjectGroupModel objectGroup, String qualifier, int version,
        List<DbQualifiersModel> qualifiers, ObjectGroupDto objectGroupDto) throws InvalidCreateOperationException {

        try (MetaDataClient client = metaDataClientFactory.getClient()) {
            String versionId = collectService.createRequestId();
            UpdateMultiQuery query =
                QueryHandler.getQualifiersAddMultiQuery(qualifier, version, qualifiers, objectGroupDto, versionId);
            client.updateObjectGroupById(query.getFinalUpdate(), objectGroup.getId());
        } catch (final MetaDataException | InvalidParseOperationException e) {
            LOGGER.error(e);
            throw new IllegalArgumentException("Can't save objectGroup by unit ID: " + e.getMessage());
        }
    }

    public void addVersionToObjectGroups(DbQualifiersModel qualifierModelToUpdate, DbObjectGroupModel objectGroup,
        String qualifier, int version, List<DbQualifiersModel> qualifiers, ObjectGroupDto objectGroupDto)
        throws MetaDataExecutionException, MetaDataClientServerException, InvalidCreateOperationException,
        CollectException {

        try (MetaDataClient client = metaDataClientFactory.getClient()) {
            String versionId = collectService.createRequestId();
            UpdateMultiQuery query = QueryHandler.getQualifiersUpdateMultiQuery(qualifierModelToUpdate, objectGroup,
                qualifier, version, qualifiers, objectGroupDto, versionId);

            client.updateObjectGroupById(query.getFinalUpdate(), objectGroup.getId());
        } catch (final MetaDataException | InvalidParseOperationException e) {
            LOGGER.error(e);
            throw new CollectException("Can't save objectGroup by unit ID: {}" + e.getMessage());
        }
    }

    public DbObjectGroupModel getDbObjectGroup(ArchiveUnitModel archiveUnitModel)
        throws CollectException {

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
            LOGGER.error(e);
            throw new CollectException("Can't save objectGroup by unit ID: {}" + e.getMessage());
        }
    }

    public void addBinaryInfoToQualifier(DbObjectGroupModel dbObjectGroupModel, String usage, int version,
        String fileName, String digest, int sizeInputStream,
        DbFormatIdentificationModel formatIdentifierResponse) throws CollectException {

        DbQualifiersModel qualifierModelToUpdate =
            TransactionHelper.findQualifier(dbObjectGroupModel.getQualifiers(), usage);

        if (qualifierModelToUpdate == null) {
            LOGGER.debug("Cannot found usage for object  with id({}))", dbObjectGroupModel.getId());
            throw new IllegalArgumentException(
                "Cannot found usage for object with id(" + dbObjectGroupModel.getId() + ")");
        }

        DbVersionsModel dbVersionsModel = TransactionHelper.getObjectVersionsModel(dbObjectGroupModel, usage, version);

        if (dbVersionsModel == null) {
            LOGGER.debug("Cannot found version for object  with id({}))", dbObjectGroupModel.getId());
            throw new IllegalArgumentException(
                "Cannot found version for object with id(" + dbObjectGroupModel.getId() + ")");
        }

        int indexQualifier = dbObjectGroupModel.getQualifiers().indexOf(qualifierModelToUpdate);
        int indexVersionsModel = qualifierModelToUpdate.getVersions().indexOf(dbVersionsModel);
        dbVersionsModel.setOpi(dbObjectGroupModel.getOpi());
        dbVersionsModel.setUri("Content/" + fileName);
        dbVersionsModel.setMessageDigest(digest);
        dbVersionsModel.setAlgorithm("SHA-512");
        dbVersionsModel.setSize(sizeInputStream);

        if (null != formatIdentifierResponse) {
            dbVersionsModel.setFormatIdentificationModel(formatIdentifierResponse);
        }

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
            LOGGER.error(e);
            throw new CollectException("Can't save objectGroup " + e.getMessage());
        }
    }

    public String pushStreamToWorkspace(String containerName, InputStream uploadedInputStream, String fileName) {
        LOGGER.debug("Try to push stream to workspace...");
        String digest;
        try (WorkspaceClient workspaceClient = workspaceClientFactory.getClient()) {
            if (!workspaceClient.isExistingContainer(containerName)) {
                workspaceClient.createContainer(containerName);
                workspaceClient.createFolder(containerName, FOLDER_CONTENT);
            }
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-512");
            uploadedInputStream = new DigestInputStream(uploadedInputStream, messageDigest);
            workspaceClient.putObject(containerName, FOLDER_CONTENT.concat("/").concat(fileName), uploadedInputStream);
            digest = TransactionHelper.readMessageDigestReturn(messageDigest.digest());
        } catch (ContentAddressableStorageException | NoSuchAlgorithmException e) {
            LOGGER.error("Error when trying to push stream to workspace {} ", e);
            throw new IllegalArgumentException("Error when trying to push stream to workspace {} " + e.getMessage());
        }
        LOGGER.debug(" -> push stream to workspace finished");
        return digest;
    }

    public JsonNode saveArchiveUnitInMetaData(CollectUnitDto collectUnitDto) throws CollectException {
        try (MetaDataClient client = metaDataClientFactory.getClient()) {
            UnitModel unitModel = CollectUnitMapper.toModel(collectUnitDto);
            JsonNode unitJsonNode = objectMapper.convertValue(unitModel, JsonNode.class);
            List<BulkUnitInsertEntry> units =
                Collections.singletonList(new BulkUnitInsertEntry(unitModel.getUp(), unitJsonNode));
            return client.insertUnitBulk(new BulkUnitInsertRequest(units));
        } catch (final MetaDataException | InvalidParseOperationException e) {
            LOGGER.error(e);
            throw new CollectException(e);
        }
    }

    public ArchiveUnitModel getArchiveUnitById(String unitId) throws CollectException {
        try (MetaDataClient client = metaDataClientFactory.getClient()) {
            JsonNode jsonNode = client.selectUnitbyId(new Select().getFinalSelect(), unitId);
            if (jsonNode == null || !jsonNode.has(TAG_RESULTS) || jsonNode.get(TAG_RESULTS).size() == 0) {
                throw new CollectException("Can't get unit by ID: " + unitId);
            }
            return objectMapper.convertValue(jsonNode.get(TAG_RESULTS).get(0), ArchiveUnitModel.class);
        } catch (CollectException | MetaDataExecutionException | MetaDataDocumentSizeException
            | InvalidParseOperationException | MetaDataClientServerException e) {
            LOGGER.error(e);
            throw new CollectException(e);
        }
    }

    public DbFormatIdentificationModel getFormatIdentification(String transactionId, String objectName) {
        FormatIdentifier formatIdentifier;
        try (WorkspaceClient workspaceClient = workspaceClientFactory.getClient()) {
            formatIdentifier = formatIdentifierFactory.getFormatIdentifierFor(FORMAT_IDENTIFIER_ID);
            if (workspaceClient.isExistingContainer(transactionId)) {
                InputStream is =
                    workspaceClient.getObject(transactionId, "Content/" + objectName).readEntity(InputStream.class);
                Path path = Paths.get(VitamConfiguration.getVitamTmpFolder(), objectName);
                Files.copy(is, path);
                File tmpFile = path.toFile();
                final List<FormatIdentifierResponse> formats = formatIdentifier.analysePath(tmpFile.toPath());
                final FormatIdentifierResponse format = TransactionHelper.getFirstPronomFormat(formats);
                DbFormatIdentificationModel formatIdentificationModel = new DbFormatIdentificationModel();
                formatIdentificationModel.setFormatId(format.getPuid());
                formatIdentificationModel.setMimeType(format.getMimetype());
                formatIdentificationModel.setFormatLitteral(format.getFormatLiteral());
                Files.delete(path);
                return formatIdentificationModel;
            }
            return null;
        } catch (ContentAddressableStorageServerException | ContentAddressableStorageNotFoundException | FileFormatNotFoundException | FormatIdentifierBadRequestException | IOException | FormatIdentifierNotFoundException | FormatIdentifierFactoryException | FormatIdentifierTechnicalException e) {
            LOGGER.error(e);
            throw new IllegalArgumentException("Can't detect format for the object : " + e.getMessage());
        }
    }

}
