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
import com.fasterxml.jackson.databind.node.ArrayNode;
import fr.gouv.vitam.collect.internal.dto.ArchiveUnitDto;
import fr.gouv.vitam.collect.internal.dto.ObjectGroupDto;
import fr.gouv.vitam.collect.internal.model.CollectModel;
import fr.gouv.vitam.collect.internal.model.ObjectGroupModel;
import fr.gouv.vitam.collect.internal.model.UnitModel;
import fr.gouv.vitam.collect.internal.server.CollectConfiguration;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.objectgroup.QualifiersModel;
import fr.gouv.vitam.common.model.objectgroup.VersionsModel;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.metadata.api.exception.*;
import fr.gouv.vitam.metadata.api.model.BulkUnitInsertEntry;
import fr.gouv.vitam.metadata.api.model.BulkUnitInsertRequest;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.metadata.client.MetadataType;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import fr.gouv.vitam.workspace.client.WorkspaceType;

import java.io.InputStream;
import java.util.*;

public class TransactionService {

    private final CollectService collectService;
    private final WorkspaceClientFactory workspaceClientFactory;
    private final MetaDataClientFactory metaDataClientFactory;
    private static final String FOLDER_SIP = "SIP";
    private static final String RESULTS = "$results";

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(TransactionService.class);

    public TransactionService(CollectService collectService, CollectConfiguration collectConfiguration) {
        this.collectService = collectService;
        WorkspaceClientFactory.changeMode(collectConfiguration.getWorkspaceUrl());
        this.workspaceClientFactory = WorkspaceClientFactory.getInstance(WorkspaceType.COLLECT);
        this.metaDataClientFactory = MetaDataClientFactory.getInstance(MetadataType.COLLECT);
    }

    public Optional<CollectModel> verifyAndGetCollectByTransaction(String transactionId)
        throws InvalidParseOperationException {
        return collectService.findCollect(transactionId);
    }

    public void uploadVerifications(String archiveUnitId, String gotId)
        throws InvalidParseOperationException, MetaDataExecutionException, MetaDataClientServerException,
        MetaDataDocumentSizeException, MetaDataNotFoundException, MetadataInvalidSelectException {

        try (MetaDataClient metaDataClient = metaDataClientFactory.getClient()) {
            JsonNode archiveUnitJsonNode =
                metaDataClient.selectUnitbyId(new SelectMultiQuery().getFinalSelectById(), archiveUnitId);

            RequestResponseOK<UnitModel> archiveUnitResponse =
                RequestResponseOK.getFromJsonNode(archiveUnitJsonNode, UnitModel.class);
            UnitModel unitModel = archiveUnitResponse.getFirstResult();

            //Check archiveUnit contains got
            if (unitModel == null || unitModel.getGot() == null || !unitModel.getGot().equals(gotId)) {
                LOGGER.debug("Got with id({}) is not attached to archiveUnit({})", gotId, archiveUnitId);
                throw new IllegalArgumentException(
                    "Got with id(" + gotId + ") is not attached to archiveUnit(" + archiveUnitId + ")");
            }

            JsonNode objectGroupJsonNode =
                metaDataClient.selectObjectGrouptbyId(new SelectMultiQuery().getFinalSelectById(), gotId);
            RequestResponseOK<ObjectGroupModel> objectGroupResponse =
                RequestResponseOK.getFromJsonNode(objectGroupJsonNode, ObjectGroupModel.class);
            ObjectGroupModel objectGroupModel = objectGroupResponse.getFirstResult();

            //Check Got exists
            if (objectGroupModel == null) {
                LOGGER.debug("Can found got with id({}))", gotId);
                throw new IllegalArgumentException("Can found got with id(" + gotId + ")");
            }
        }
    }

     public void pushSipStreamToWorkspace(String containerName, InputStream uploadedInputStream) {
        LOGGER.debug("Try to push stream to workspace...");
        try (WorkspaceClient workspaceClient = workspaceClientFactory.getClient()) {
            workspaceClient.createContainer(containerName);
            String fileName = UUID.randomUUID().toString();
            workspaceClient.putObject(containerName, fileName, uploadedInputStream);
        } catch (ContentAddressableStorageException e) {
            LOGGER.error(e);
        } finally {
            StreamUtils.closeSilently(uploadedInputStream);
        }
        LOGGER.debug(" -> push stream to workspace finished");
    }

 public void updateGotWithBinaryInfos(String gotId, String usage)
        throws MetaDataExecutionException, MetaDataNotFoundException, MetaDataClientServerException,
        InvalidParseOperationException, MetaDataDocumentSizeException, MetadataInvalidSelectException {

        try (MetaDataClient metaDataClient = metaDataClientFactory.getClient()) {
            JsonNode objectGroupJsonNode =
                metaDataClient.selectObjectGrouptbyId(new SelectMultiQuery().getFinalSelectById(), gotId);
            RequestResponseOK<ObjectGroupModel> objectGroupResponse =
                RequestResponseOK.getFromJsonNode(objectGroupJsonNode, ObjectGroupModel.class);
            ObjectGroupModel objectGroupModel = objectGroupResponse.getFirstResult();

            if (objectGroupModel == null) {
                LOGGER.debug("Can found got with id({}))", gotId);
                throw new IllegalArgumentException("Can found got with id(" + gotId + ")");
            }

            VersionsModel version = new VersionsModel();
            version.setId(UUID.randomUUID().toString());
            version.setFileInfoModel(objectGroupModel.getFileInfo());

            QualifiersModel qualifier = new QualifiersModel();
            qualifier.setQualifier(usage);
            qualifier.setVersions(List.of(version));

            String qualifierToPush = "{\"$action\": [{ \"$push\": {\"qualifiers\": [" + JsonHandler.writeAsString(qualifier) + "]}}]}";

            metaDataClient.updateObjectGroupById(JsonHandler.getFromString(qualifierToPush), objectGroupModel.getId());
        }

    }

    public JsonNode saveArchiveUnitInMetaData(ArchiveUnitDto archiveUnitDto) {
        Integer tenantId = ParameterHelper.getTenantParameter();

        try (MetaDataClient metaDataClient = metaDataClientFactory.getClient()) {
            return metaDataClient.insertUnitBulk(
                    new BulkUnitInsertRequest(Collections.singletonList(getBulkUnitInsertEntry(archiveUnitDto, tenantId))));
        } catch (final MetaDataException | InvalidParseOperationException e) {
            LOGGER.error(e);
        }
        return null;
    }


    public JsonNode saveObjectGroupInMetaData(ObjectGroupDto objectGroupDto, String archiveUnitId){
        String gotQuery = "{$query: {}, $projection: {}, $filter: {}, \"$data\": { \"_id\": \""+ objectGroupDto.getId()+"\", \"FileInfo\": {\n" +
                "        \"Filename\": \""+ objectGroupDto.getFileInfo().getFileName()+"\",\"LastModified\":\""+objectGroupDto.getFileInfo().getLastModified() +"\"} }}";
        JsonNode jsonNode = null;
        try (MetaDataClient metaDataClient = metaDataClientFactory.getClient()) {
            jsonNode =  metaDataClient.insertObjectGroup(JsonHandler.getFromString(gotQuery));
            if (jsonNode != null) {
                String archiveUnitToPush = "{\"$roots\": [ \""+ archiveUnitId +"\" ],$query: {}, $projection: {}, $filter: {}, $action:[ { $set : {\"_id\": \""+ archiveUnitId +"\" , \"_og\" :\"" + objectGroupDto.getId()+"\" , \"_mgt\":{} ,\"DescriptionLevel\":\"Item\" } } ]}";
                metaDataClient.updateUnitBulk(JsonHandler.getFromString(archiveUnitToPush));

            }
        } catch (final MetaDataException | InvalidParseOperationException e) {
            LOGGER.error(e);
            return jsonNode;
        }
        return jsonNode;
    }


    public ArrayNode getArchiveUnitById(String archiveUnitId){
        ArrayNode result = null;
        try (MetaDataClient metaDataClient = metaDataClientFactory.getClient()) {
            JsonNode archiveUnitJsonNode = metaDataClient.selectUnitbyId(new SelectMultiQuery().getFinalSelectById(), archiveUnitId);
            return (ArrayNode) archiveUnitJsonNode.get(RESULTS);
        } catch (MetaDataDocumentSizeException  | MetaDataClientServerException | MetaDataExecutionException |InvalidParseOperationException e) {
            LOGGER.error(e);
            return result;
        }
    }

    private BulkUnitInsertEntry getBulkUnitInsertEntry(ArchiveUnitDto archiveUnitDto, Integer tenantId)
            throws InvalidParseOperationException {
        String uaData =
                "{ \"_id\": \"" + archiveUnitDto.getId() + "\", \"_tenant\": " + tenantId + ", " + "\"data\": \"data1\"," +
                        "\"Title\": \"" + archiveUnitDto.getContent().getTitle() + "\" }";

        if (null == archiveUnitDto.getParentUnit()) {
            return new BulkUnitInsertEntry(Collections.emptySet(), JsonHandler.getFromString(uaData));
        } else {
            return new BulkUnitInsertEntry(Set.of(archiveUnitDto.getParentUnit()), JsonHandler.getFromString(uaData));
        }
    }
}
