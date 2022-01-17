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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.culture.archivesdefrance.seda.v2.IdentifierType;
import fr.gouv.culture.archivesdefrance.seda.v2.LevelType;
import fr.gouv.culture.archivesdefrance.seda.v2.OrganizationDescriptiveMetadataType;
import fr.gouv.vitam.collect.internal.dto.ArchiveUnitDto;
import fr.gouv.vitam.collect.internal.dto.ObjectGroupDto;
import fr.gouv.vitam.collect.internal.exception.CollectException;
import fr.gouv.vitam.collect.internal.model.CollectModel;
import fr.gouv.vitam.collect.internal.server.CollectConfiguration;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.query.action.SetAction;
import fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.InsertMultiQuery;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.multiple.UpdateMultiQuery;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.format.identification.FormatIdentifier;
import fr.gouv.vitam.common.format.identification.FormatIdentifierFactory;
import fr.gouv.vitam.common.format.identification.exception.*;
import fr.gouv.vitam.common.format.identification.model.FormatIdentifierResponse;
import fr.gouv.vitam.common.format.identification.siegfried.FormatIdentifierSiegfried;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.mapping.deserializer.IdentifierTypeDeserializer;
import fr.gouv.vitam.common.mapping.deserializer.LevelTypeDeserializer;
import fr.gouv.vitam.common.mapping.deserializer.OrganizationDescriptiveMetadataTypeDeserializer;
import fr.gouv.vitam.common.mapping.deserializer.TextByLangDeserializer;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.objectgroup.*;
import fr.gouv.vitam.common.model.unit.ArchiveUnitModel;
import fr.gouv.vitam.common.model.unit.TextByLang;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
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
import java.util.*;

import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.FILTERARGS.OBJECTGROUPS;
import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTIONARGS.QUALIFIERS;
import static fr.gouv.vitam.common.json.JsonHandler.toJsonNode;

public class TransactionService {

    private final CollectService collectService;
    private final WorkspaceClientFactory workspaceClientFactory;
    private final MetaDataClientFactory metaDataClientFactory;
    private static final String FOLDER_CONTENT = "Content";
    private static final String RESULTS = "$results";
    private final FormatIdentifierFactory formatIdentifierFactory;
    private MetaDataClient metaDataClient;

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(TransactionService.class);

    public TransactionService(CollectService collectService, CollectConfiguration collectConfiguration) {
        this.collectService = collectService;
        WorkspaceClientFactory.changeMode(collectConfiguration.getWorkspaceUrl());
        this.workspaceClientFactory = WorkspaceClientFactory.getInstance(WorkspaceType.COLLECT);
        this.metaDataClientFactory = MetaDataClientFactory.getInstance(MetadataType.COLLECT);
        this.formatIdentifierFactory = FormatIdentifierFactory.getInstance();
        this.metaDataClient = metaDataClientFactory.getClient();
    }

    public Optional<CollectModel> verifyAndGetCollectByTransaction(String transactionId)
            throws InvalidParseOperationException {
        return collectService.findCollect(transactionId);
    }

    public void saveObjectGroupInMetaData(ArchiveUnitModel archiveUnitModel, String qualifier, int version, ObjectGroupDto objectGroupDto)
            throws InvalidParseOperationException, MetaDataExecutionException, MetaDataClientServerException, CollectException {

        try {
            if (archiveUnitModel.getOg() == null) {
                checkVersion(version, 1);
                initializeObjectGroupInMetaData(objectGroupDto, qualifier, version, archiveUnitModel);
            } else {
                RequestResponse<JsonNode> requestResponse = metaDataClient.getObjectGroupByIdRaw(archiveUnitModel.getOg());
                if (!requestResponse.isOk()) {
                    LOGGER.debug("Cannot found got with id({}))", archiveUnitModel.getOg());
                    throw new IllegalArgumentException("Cannot found got with id(" + archiveUnitModel.getOg() + ")");
                }
                JsonNode firstResult = ((RequestResponseOK<JsonNode>) requestResponse).getFirstResult();
                DbObjectGroupModel dbObjectGroupModel = JsonHandler.getFromJsonNode(firstResult, DbObjectGroupModel.class);
                Optional<DbQualifiersModel> qualifierModel = findQualifier(dbObjectGroupModel.getQualifiers(), qualifier);
                if (qualifierModel.isEmpty()) {
                    checkVersion(version, 1);
                    addQualifierToObjectGroups(dbObjectGroupModel, qualifier, version, dbObjectGroupModel.getQualifiers(), objectGroupDto);
                } else {
                    DbQualifiersModel qualifierModelToUpdate = qualifierModel.get();
                    DbVersionsModel dbVersionsModel = getObjectVersionsModel(dbObjectGroupModel, qualifier, version);
                    if (dbVersionsModel != null) {
                        LOGGER.debug("Qualifier already exist with qualifier {} and version {})", qualifier, version);
                        throw new IllegalArgumentException("Qualifier already exist with qualifier " + qualifier + " and version " + version + "");
                    } else {
                        int lastVersion = getLastVersion(qualifierModelToUpdate) + 1;
                        checkVersion(version, lastVersion);
                        addVersionToObjectGroups(qualifierModelToUpdate, dbObjectGroupModel, qualifier, lastVersion, dbObjectGroupModel.getQualifiers(), objectGroupDto);
                    }
                }
            }

        } catch (VitamClientException | InvalidCreateOperationException e) {
            LOGGER.debug("Cannot found got with id({}))", archiveUnitModel.getOg());
            throw new CollectException(e);
        }
    }

    private void checkVersion(int version, int lastVersion) {
        if (version != lastVersion) {
            LOGGER.error("version number not valid({}))", version);
            throw new IllegalArgumentException("version number not valid " + version);
        }
    }


    public void addQualifierToObjectGroups(DbObjectGroupModel objectGroup, String qualifier, int version, List<DbQualifiersModel> qualifiers, ObjectGroupDto objectGroupDto)
            throws InvalidParseOperationException, InvalidCreateOperationException {
        FileInfoModel fileInfoModel = new FileInfoModel();
        fileInfoModel.setFilename(objectGroupDto.getFileInfo().getFileName());
        DbFileInfoModel dbfileInfoModel = new DbFileInfoModel();
        dbfileInfoModel.setFilename(objectGroupDto.getFileInfo().getFileName());
        DbVersionsModel dbversion = new DbVersionsModel();
        dbversion.setId(collectService.createRequestIdVitamFormat().getId());
        dbversion.setFileInfoModel(dbfileInfoModel);
        dbversion.setDataObjectVersion(qualifier + "_" + version);
        DbQualifiersModel dbQualifiersModel = new DbQualifiersModel();
        dbQualifiersModel.setQualifier(qualifier);
        dbQualifiersModel.setVersions(List.of(dbversion));
        qualifiers.add(dbQualifiersModel);

        Map<String, JsonNode> action = new HashMap<>();
        action.put(QUALIFIERS.exactToken(), toJsonNode(qualifiers));
        SetAction setQualifier = new SetAction(action);

        UpdateMultiQuery query = new UpdateMultiQuery();
        query.addHintFilter(OBJECTGROUPS.exactToken());
        query.addActions(
                setQualifier
        );
        try {
            metaDataClient.updateObjectGroupById(query.getFinalUpdate(), objectGroup.getId());
        } catch (final MetaDataException | InvalidParseOperationException e) {
            LOGGER.error(e);
            throw new IllegalArgumentException("Can't save objectGroup by unit ID: " + e.getMessage());
        }

    }


    public void addVersionToObjectGroups(DbQualifiersModel qualifierModelToUpdate, DbObjectGroupModel objectGroup, String qualifier, int version, List<DbQualifiersModel> qualifiers, ObjectGroupDto objectGroupDto)
            throws MetaDataExecutionException, MetaDataClientServerException,
            InvalidParseOperationException, InvalidCreateOperationException {
        int index = qualifiers.indexOf(qualifierModelToUpdate);
        FileInfoModel fileInfoModel = new FileInfoModel();
        fileInfoModel.setFilename(objectGroupDto.getFileInfo().getFileName());
        DbFileInfoModel dbfileInfoModel = new DbFileInfoModel();
        dbfileInfoModel.setFilename(objectGroupDto.getFileInfo().getFileName());
        DbVersionsModel dbversion = new DbVersionsModel();
        dbversion.setId(collectService.createRequestIdVitamFormat().getId());
        dbversion.setFileInfoModel(dbfileInfoModel);
        dbversion.setDataObjectVersion(qualifier + "_" + version);
        qualifierModelToUpdate.getVersions().add(dbversion);
        qualifierModelToUpdate.setNbc(qualifierModelToUpdate.getNbc() + 1);
        qualifiers.set(index, qualifierModelToUpdate);

        Map<String, JsonNode> action = new HashMap<>();
        action.put(QUALIFIERS.exactToken(), toJsonNode(qualifiers));
        action.put(QUALIFIERS.exactToken(), toJsonNode(qualifiers));
        SetAction setQualifier = new SetAction(action);

        UpdateMultiQuery query = new UpdateMultiQuery();
        query.addActions(
                UpdateActionHelper.set(VitamFieldsHelper.nbobjects(), objectGroup.getNbc() + 1),
                setQualifier
        );

        try {
            metaDataClient.updateObjectGroupById(query.getFinalUpdate(), objectGroup.getId());
        } catch (final MetaDataException | InvalidParseOperationException e) {
            LOGGER.error(e);
            throw new IllegalArgumentException("Can't save objectGroup by unit ID: " + e.getMessage());
        }

    }


    public JsonNode initializeObjectGroupInMetaData(ObjectGroupDto objectGroupDto, String usage, int version, ArchiveUnitModel archiveUnitModel) throws InvalidParseOperationException {
        FileInfoModel fileInfoModel = new FileInfoModel();
        fileInfoModel.setFilename(objectGroupDto.getFileInfo().getFileName());
        DbFileInfoModel dbfileInfoModel = new DbFileInfoModel();
        dbfileInfoModel.setFilename(objectGroupDto.getFileInfo().getFileName());
        DbVersionsModel dbversion = new DbVersionsModel();
        dbversion.setId(collectService.createRequestIdVitamFormat().getId());
        dbversion.setFileInfoModel(dbfileInfoModel);
        dbversion.setDataObjectVersion(usage + "_" + version);
        DbQualifiersModel dbQualifiersModel = new DbQualifiersModel();
        dbQualifiersModel.setQualifier(usage);
        dbQualifiersModel.setVersions(List.of(dbversion));

        DbObjectGroupModel dbObjectGroupModel = new DbObjectGroupModel();
        dbObjectGroupModel.setId(objectGroupDto.getId());
        dbObjectGroupModel.setFileInfo(fileInfoModel);
        dbObjectGroupModel.setOpi(archiveUnitModel.getOpi());
        dbObjectGroupModel.setQualifiers(List.of(dbQualifiersModel));


        final InsertMultiQuery insert = new InsertMultiQuery();
        insert.resetFilter();
        insert.addHintFilter(BuilderToken.FILTERARGS.OBJECTGROUPS.exactToken());
        insert.addData((ObjectNode) JsonHandler.toJsonNode(dbObjectGroupModel));
        final ObjectNode insertRequest = insert.getFinalInsert();


        JsonNode jsonNode = null;
        try {
            jsonNode = metaDataClient.insertObjectGroup(insertRequest);
            if (jsonNode != null) {
                UpdateMultiQuery multiQuery = new UpdateMultiQuery();
                multiQuery.addActions(UpdateActionHelper
                        .set(VitamFieldsHelper.object(), objectGroupDto.getId()));
                multiQuery.resetRoots().addRoots(archiveUnitModel.getId());
                metaDataClient.updateUnitBulk(multiQuery.getFinalUpdate());
            }
        } catch (final MetaDataException | InvalidParseOperationException | InvalidCreateOperationException e) {
            LOGGER.error(e);
            throw new IllegalArgumentException("Can't save objectGroup by unit ID: ");
        }
        return jsonNode;
    }

    public DbObjectGroupModel getDbObjectGroup(ArchiveUnitModel archiveUnitModel)
            throws InvalidParseOperationException {

        try {

            if (archiveUnitModel.getOg() == null) {
                LOGGER.debug("Cannot found any got attached to unit with id({}))", archiveUnitModel.getId());
                throw new IllegalArgumentException("Cannot found any object attached to unit with id(" + archiveUnitModel.getId() + ")");
            } else {
                RequestResponse<JsonNode> requestResponse = metaDataClient.getObjectGroupByIdRaw(archiveUnitModel.getOg());
                if (!requestResponse.isOk()) {
                    LOGGER.debug("Cannot found object with id({}))", archiveUnitModel.getOg());
                    throw new IllegalArgumentException("Cannot found object with id(" + archiveUnitModel.getOg() + ")");
                }
                JsonNode firstResult = ((RequestResponseOK<JsonNode>) requestResponse).getFirstResult();
                return JsonHandler.getFromJsonNode(firstResult, DbObjectGroupModel.class);
            }
        } catch (VitamClientException e) {
            e.printStackTrace();
        }
        return null;
    }

    public JsonNode addBinaryInfoToQualifier(DbObjectGroupModel dbObjectGroupModel,
                                             String usage,
                                             int version,
                                             String fileName,
                                             String digest,
                                             int sizeInputStream,
                                             DbFormatIdentificationModel formatIdentifierResponse) throws CollectException {
        Optional<DbQualifiersModel> qualifierModel = findQualifier(dbObjectGroupModel.getQualifiers(), usage);
        if (qualifierModel.isEmpty()) {
            LOGGER.debug("Cannot found usage for object  with id({}))", dbObjectGroupModel.getId());
            throw new IllegalArgumentException("Cannot found usage for object with id(" + dbObjectGroupModel.getId() + ")");
        } else {
            DbQualifiersModel qualifierModelToUpdate = qualifierModel.get();
            DbVersionsModel dbVersionsModel = getObjectVersionsModel(dbObjectGroupModel, usage, version);
            if (dbVersionsModel == null) {
                LOGGER.debug("Cannot found version for object  with id({}))", dbObjectGroupModel.getId());
                throw new IllegalArgumentException("Cannot found version for object with id(" + dbObjectGroupModel.getId() + ")");
            } else {
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
                try {
                    Map<String, JsonNode> action = new HashMap<>();
                    action.put(QUALIFIERS.exactToken(), toJsonNode(dbObjectGroupModel.getQualifiers()));
                    SetAction setQualifier = new SetAction(action);
                    UpdateMultiQuery query = new UpdateMultiQuery();
                    query.addHintFilter(OBJECTGROUPS.exactToken());
                    query.addActions(
                            setQualifier
                    );
                    JsonNode jsonNode = null;

                    metaDataClient.updateObjectGroupById(query.getFinalUpdate(), dbObjectGroupModel.getId());
                    return jsonNode;
                } catch (final MetaDataException | InvalidParseOperationException | InvalidCreateOperationException e) {
                    LOGGER.error(e);
                    throw new CollectException("Can't save objectGroup " + e.getMessage());
                }

            }
        }


    }

    public String pushStreamToWorkspace(String containerName, InputStream uploadedInputStream, String fileName) {
        LOGGER.debug("Try to push stream to workspace...");
        String digest = null;
        try (WorkspaceClient workspaceClient = workspaceClientFactory.getClient()) {
            if (!workspaceClient.isExistingContainer(containerName)) {
                workspaceClient.createContainer(containerName);
                workspaceClient.createFolder(containerName, FOLDER_CONTENT);
            }
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-512");
            uploadedInputStream = new DigestInputStream(uploadedInputStream, messageDigest);
            workspaceClient.putObject(containerName, FOLDER_CONTENT.concat("/").concat(fileName), uploadedInputStream);
            digest = readMessageDigestReturn(messageDigest.digest());
        } catch (ContentAddressableStorageException | NoSuchAlgorithmException e) {
            LOGGER.error("Error when trying to push stream to workspace {} ", e);
            throw new IllegalArgumentException("Error when trying to push stream to workspace {} " + e.getMessage());
        }
        LOGGER.debug(" -> push stream to workspace finished");
        return digest;
    }


    public JsonNode saveArchiveUnitInMetaData(ArchiveUnitDto archiveUnitDto) {
        Integer tenantId = ParameterHelper.getTenantParameter();

        try {
            return metaDataClient.insertUnitBulk(
                    new BulkUnitInsertRequest(Collections.singletonList(getBulkUnitInsertEntry(archiveUnitDto, tenantId))));
        } catch (final MetaDataException | InvalidParseOperationException e) {
            LOGGER.error(e);
        }
        return null;
    }


    public ArchiveUnitModel getArchiveUnitById(String archiveUnitId) {
        ArchiveUnitModel archiveUnitModel = null;
        try {
            JsonNode response = metaDataClient.selectUnitbyId(new SelectMultiQuery().getFinalSelectById(), archiveUnitId);
            if (response == null || response.get(RESULTS) == null) {
                throw new CollectException("Can't get unit by ID: " + archiveUnitId);
            }
            JsonNode results = response.get(RESULTS);
            if (results.size() != 1) {
                throw new CollectException("Can't get unit by ID: " + archiveUnitId);
            }
            JsonNode jsonUnit = results.get(0);
            return buildObjectMapper().treeToValue(jsonUnit, ArchiveUnitModel.class);
        } catch (MetaDataDocumentSizeException | MetaDataClientServerException | MetaDataExecutionException | InvalidParseOperationException | JsonProcessingException | CollectException e) {
            LOGGER.error(e);
            return archiveUnitModel;
        }
    }

    private BulkUnitInsertEntry getBulkUnitInsertEntry(ArchiveUnitDto archiveUnitDto, Integer tenantId)
            throws InvalidParseOperationException {
        String uaData =
                "{ \"_id\": \"" + archiveUnitDto.getId() + "\",\"DescriptionLevel\": \"RecordGrp\", \"_tenant\": " + tenantId + "," +
                        "\"Title\": \"" + archiveUnitDto.getContent().getTitle() + "\",\"_opi\":\"" + archiveUnitDto.getTransactionId() +
                        "\",\"Description\":\"" + archiveUnitDto.getContent().getDescription() + "\"" +
                        ", \"_up\": \"" + archiveUnitDto.getParentUnit() + "\"}";

        if (null == archiveUnitDto.getParentUnit()) {
            return new BulkUnitInsertEntry(Collections.emptySet(), JsonHandler.getFromString(uaData));
        } else {
            return new BulkUnitInsertEntry(Set.of(archiveUnitDto.getParentUnit()), JsonHandler.getFromString(uaData));
        }
    }

    public String readMessageDigestReturn(byte[] theDigestResult) {
        StringBuilder sb = new StringBuilder();
        for (byte b : theDigestResult) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString().toLowerCase();
    }


    private static final String FORMAT_IDENTIFIER_ID = "siegfried-local";

    public DbFormatIdentificationModel getFormatIdentification(String transactionId, String objectName) {
        FormatIdentifier formatIdentifier;
        try (WorkspaceClient workspaceClient = workspaceClientFactory.getClient()) {
            formatIdentifier = formatIdentifierFactory.getFormatIdentifierFor(FORMAT_IDENTIFIER_ID);
            if (workspaceClient.isExistingContainer(transactionId)) {
                InputStream is = workspaceClient.getObject(transactionId, "Content/" + objectName).readEntity(InputStream.class);
                Path path = Paths.get(VitamConfiguration.getVitamTmpFolder(), objectName);
                Files.copy(is, path);
                File tmpFile = path.toFile();
                final List<FormatIdentifierResponse> formats = formatIdentifier.analysePath(tmpFile.toPath());
                final FormatIdentifierResponse format = getFirstPronomFormat(formats);
                DbFormatIdentificationModel formatIdentificationModel = new DbFormatIdentificationModel();
                formatIdentificationModel.setFormatId(format.getPuid());
                formatIdentificationModel.setMimeType(format.getMimetype());
                formatIdentificationModel.setFormatLitteral(format.getFormatLiteral());
                Files.delete(path);
                return formatIdentificationModel;
            }
        } catch (ContentAddressableStorageServerException | ContentAddressableStorageNotFoundException | FileFormatNotFoundException | FormatIdentifierBadRequestException | IOException | FormatIdentifierNotFoundException | FormatIdentifierFactoryException | FormatIdentifierTechnicalException e) {
            LOGGER.error(e);
            throw new IllegalArgumentException("Can't detect format for the object : " + e.getMessage());
        }
        return null;
    }

    private FormatIdentifierResponse getFirstPronomFormat(List<FormatIdentifierResponse> formats) {
        for (final FormatIdentifierResponse format : formats) {
            if (FormatIdentifierSiegfried.PRONOM_NAMESPACE.equals(format.getMatchedNamespace())) {
                return format;
            }
        }
        return null;
    }

    private DbVersionsModel getObjectVersionsModel(DbObjectGroupModel dbObjectGroupModel, String qualifier, int version) {
        if (dbObjectGroupModel.getQualifiers() != null) {
            final String dataObjectVersion = qualifier + "_" + version;
            for (DbQualifiersModel qualifiersResponse : dbObjectGroupModel.getQualifiers()) {
                if (qualifiersResponse.getQualifier() != null && qualifiersResponse.getQualifier().contains("_")) {
                    qualifiersResponse.setQualifier(qualifiersResponse
                            .getQualifier().split("_")[0]);
                }
                if (qualifier.equals(qualifiersResponse.getQualifier())) {
                    for (DbVersionsModel versionResponse : qualifiersResponse.getVersions()) {
                        if (dataObjectVersion.equals(versionResponse.getDataObjectVersion())) {
                            return versionResponse;
                        }
                    }
                }
            }
        }
        return null;
    }


    private ObjectMapper buildObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        SimpleModule module = new SimpleModule();

        module.addDeserializer(TextByLang.class, new TextByLangDeserializer());
        module.addDeserializer(LevelType.class, new LevelTypeDeserializer());
        module.addDeserializer(IdentifierType.class, new IdentifierTypeDeserializer());
        module.addDeserializer(OrganizationDescriptiveMetadataType.class,
                new OrganizationDescriptiveMetadataTypeDeserializer(objectMapper));

        objectMapper.registerModule(module);

        return objectMapper;
    }

    private int getLastVersion(DbQualifiersModel qualifierModelToUpdate) {
        return qualifierModelToUpdate.getVersions()
                .stream()
                .map(DbVersionsModel::getDataObjectVersion)
                .map(dataObjectVersion -> dataObjectVersion.split("_")[1])
                .map(Integer::parseInt)
                .max(Comparator.naturalOrder())
                .orElse(0);
    }

    private Optional<DbQualifiersModel> findQualifier(List<DbQualifiersModel> qualifiers, String targetQualifier) {
        return qualifiers.stream()
                .filter(qualifier -> qualifier.getQualifier().equals(targetQualifier))
                .findFirst();
    }


}
