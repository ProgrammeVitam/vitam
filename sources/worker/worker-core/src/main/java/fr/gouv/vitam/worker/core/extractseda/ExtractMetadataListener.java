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
package fr.gouv.vitam.worker.core.extractseda;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.culture.archivesdefrance.seda.v2.ArchiveUnitType;
import fr.gouv.culture.archivesdefrance.seda.v2.BinaryDataObjectType;
import fr.gouv.culture.archivesdefrance.seda.v2.DataObjectGroupType;
import fr.gouv.culture.archivesdefrance.seda.v2.MinimalDataObjectType;
import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.mapping.mapper.VitamObjectMapper;
import fr.gouv.vitam.common.model.administration.DataObjectVersionType;
import fr.gouv.vitam.common.model.logbook.LogbookEvent;
import fr.gouv.vitam.common.model.objectgroup.DbVersionsModel;
import fr.gouv.vitam.common.model.unit.GotObj;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingMalformedDataException;
import fr.gouv.vitam.processing.common.exception.ProcessingObjectReferenceException;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.common.utils.DataObjectDetail;
import fr.gouv.vitam.worker.common.utils.DataObjectInfo;
import fr.gouv.vitam.worker.core.handler.LogbookEventMapper;
import fr.gouv.vitam.worker.core.mapping.ObjectGroupMapper;
import fr.gouv.vitam.worker.core.utils.JsonLineDataBase;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.model.IngestWorkflowConstants.LOGBOOK_OG_FILE_SUFFIX;
import static fr.gouv.vitam.common.model.VitamConstants.JSON_EXTENSION;

public class ExtractMetadataListener extends Unmarshaller.Listener {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ExtractMetadataListener.class);
    private final ArchiveUnitListener archiveUnitListener;
    private final HandlerIO handlerIO;
    private final IngestSession ingestSession;

    private final JsonLineDataBase objectsDatabase;


    public ExtractMetadataListener(HandlerIO handlerIO, IngestContext ingestContext, IngestSession ingestSession,
        JsonLineDataBase unitsDatabase, JsonLineDataBase objectsDatabase, MetaDataClientFactory metaDataClientFactory) {
        archiveUnitListener =
            new ArchiveUnitListener(handlerIO, ingestContext, ingestSession, unitsDatabase, metaDataClientFactory);
        this.handlerIO = handlerIO;
        this.ingestSession = ingestSession;
        this.objectsDatabase = objectsDatabase;
    }

    /*
     * This method used after XML parsing
     * it creates archival units, objects using the information extracted from the XML file and put them on files
     * it assigns to each element a GUID
     */
    @Override
    public void afterUnmarshal(Object target, Object parent) {
        if (target instanceof ArchiveUnitType) {
            archiveUnitListener.extractArchiveUnit((ArchiveUnitType) target, (JAXBElement<?>) parent);
        } else if (target instanceof DataObjectGroupType) {
            DataObjectGroupType objectGroupType = (DataObjectGroupType) target;
            String currentGroupId = objectGroupType.getId();

            if (ingestSession.getObjectGroupIdToGuid().get(currentGroupId) == null) {
                final String objectGroupGuid =
                    GUIDFactory.newObjectGroupGUID(ParameterHelper.getTenantParameter()).toString();
                ingestSession.getObjectGroupIdToGuid().put(currentGroupId, objectGroupGuid);
            }

            final String objectGroupGuid = ingestSession.getObjectGroupIdToGuid().get(currentGroupId);

            if (objectGroupType.getLogBook() != null) {
                extractLogbook(objectGroupType, objectGroupGuid);
            }
        } else if (target instanceof MinimalDataObjectType) {
            MinimalDataObjectType dataObject = (MinimalDataObjectType) target;
            String currentGroupId;
            if (parent instanceof DataObjectGroupType) {
                currentGroupId = ((DataObjectGroupType) parent).getId();
            } else {
                currentGroupId = dataObject.getDataObjectGroupId();
            }
            if (currentGroupId == null) {
                currentGroupId = dataObject.getDataObjectGroupReferenceId();
            }
            if (currentGroupId == null) {
                LOGGER.warn(
                    "Current object does not have an ObjectGroup ! auto creating ObjectGroup using the same ID of Object");
                currentGroupId = dataObject.getId();
            }
            final String objectGroupGuid =
                Objects.requireNonNullElse(ingestSession.getObjectGroupIdToGuid().get(currentGroupId),
                    GUIDFactory.newObjectGroupGUID(ParameterHelper.getTenantParameter()).toString());

            extractDataObject(currentGroupId, objectGroupGuid, dataObject);
            updateState(currentGroupId, dataObject);
        }
        super.afterUnmarshal(target, parent);
    }

    private void extractLogbook(DataObjectGroupType objectGroupType, String objectGroupGuid) {
        List<LogbookEvent> logbookEvents =
            objectGroupType.getLogBook().getEvent().stream().map(LogbookEventMapper::map).collect(Collectors.toList());
        File logbookTmpFile = handlerIO.getNewLocalFile(objectGroupGuid + LOGBOOK_OG_FILE_SUFFIX + JSON_EXTENSION);
        try {
            JsonHandler.writeAsFile(logbookEvents, logbookTmpFile);
        } catch (InvalidParseOperationException e) {
            throw new VitamRuntimeException(e);
        }
    }

    private void extractDataObject(String currentGroupId, String objectGroupGuid, MinimalDataObjectType dataObject) {
        try {
            final String objectGuid = GUIDFactory.newObjectGUID(ParameterHelper.getTenantParameter()).toString();
            DbVersionsModel versionsModel = ObjectGroupMapper.map(dataObject, objectGroupGuid);

            JsonNode value =
                VitamObjectMapper.buildSerializationObjectMapper().convertValue(versionsModel, JsonNode.class);
            objectsDatabase.write(objectGuid, value);

            final DataObjectDetail detail = new DataObjectDetail();
            if (dataObject instanceof BinaryDataObjectType) {
                DataObjectInfo dataObjectInfo = new DataObjectInfo();
                dataObjectInfo.setId(objectGuid);
                dataObjectInfo.setSize(versionsModel.getSize());
                dataObjectInfo.setUri(versionsModel.getUri());
                dataObjectInfo.setAlgo(DigestType.fromValue(versionsModel.getAlgorithm()));
                dataObjectInfo.setMessageDigest(versionsModel.getMessageDigest());

                long gotSize = checkAndComputeSize(versionsModel, dataObjectInfo);
                dataObjectInfo.setSize(gotSize);
                detail.setVersion(Objects.requireNonNullElse(dataObject.getDataObjectVersion(),
                    DataObjectVersionType.BINARY_MASTER.getName()));
                detail.setPhysical(false);
                ingestSession.getObjectGuidToDataObject().put(objectGuid, dataObjectInfo);
                if (dataObject.getDataObjectVersion() == null || dataObject.getDataObjectVersion().startsWith(DataObjectVersionType.BINARY_MASTER.getName())) {
                    ingestSession.getDataObjectGroupMasterMandatory().put(currentGroupId, true);
                } else {
                    ingestSession.getDataObjectGroupMasterMandatory().putIfAbsent(currentGroupId, false);
                }

            } else {
                detail.setVersion(Objects.requireNonNullElse(dataObject.getDataObjectVersion(),
                    DataObjectVersionType.PHYSICAL_MASTER.getName()));
                detail.setPhysical(true);
                ingestSession.getPhysicalDataObjetsGuids().add(objectGuid);
                ingestSession.getDataObjectGroupMasterMandatory().put(currentGroupId, true);
            }

            ingestSession.getDataObjectIdToDetailDataObject().put(dataObject.getId(), detail);
            ingestSession.getDataObjectIdToObjectGroupId().put(versionsModel.getId(), currentGroupId);
            ingestSession.getDataObjectIdToGuid().put(versionsModel.getId(), objectGuid);
            ingestSession.getObjectGroupIdToGuid().put(currentGroupId, objectGroupGuid);
            ingestSession.getUsageToObjectGroupId().put(dataObject.getDataObjectVersion(), objectGroupGuid);

            if (currentGroupId.equals(dataObject.getId()) &&
                ingestSession.getDataObjectIdWithoutObjectGroupId().get(dataObject.getId()) == null) {
                final GotObj gotObj = new GotObj(currentGroupId, false);
                ingestSession.getDataObjectIdWithoutObjectGroupId().put(versionsModel.getId(), gotObj);
            }


        } catch (ProcessingMalformedDataException | ProcessingObjectReferenceException e) {
            throw new VitamRuntimeException(e);
        }
    }

    private long checkAndComputeSize(DbVersionsModel versionsModel, DataObjectInfo dataObjectInfo) {
        ObjectNode diffJsonNodeToPopulate = JsonHandler.createObjectNode();
        long binarySizeInManifest = versionsModel.getSize();
        long binarySizeInWorkspace = ingestSession.getFileWithParmsFromFolder().get(dataObjectInfo.getUri());
        if (binarySizeInManifest == 0) { // Object Size equal 0 mean that Size tag is not present on manifest
            diffJsonNodeToPopulate.put("- " + SedaConstants.TAG_SIZE, "");
            diffJsonNodeToPopulate.put("+ " + SedaConstants.TAG_SIZE, binarySizeInWorkspace);
            dataObjectInfo.setDiffSizeJson(diffJsonNodeToPopulate);
            dataObjectInfo.setSizeIncorrect(Boolean.FALSE);
        } else if (binarySizeInWorkspace != binarySizeInManifest) {
            diffJsonNodeToPopulate.put("- " + SedaConstants.TAG_SIZE, binarySizeInManifest);
            diffJsonNodeToPopulate.put("+ " + SedaConstants.TAG_SIZE, binarySizeInWorkspace);
            dataObjectInfo.setDiffSizeJson(diffJsonNodeToPopulate);
            dataObjectInfo.setSizeIncorrect(Boolean.TRUE);
        }
        return binarySizeInWorkspace;
    }

    private void updateState(String currentGroupId, MinimalDataObjectType dataObject) {
        if (ingestSession.getObjectGroupIdToDataObjectId().get(currentGroupId) == null) {
            final List<String> dataOjectList = new ArrayList<>();
            dataOjectList.add(dataObject.getId());
            ingestSession.getObjectGroupIdToDataObjectId().put(currentGroupId, dataOjectList);
        } else {
            ingestSession.getObjectGroupIdToDataObjectId().get(currentGroupId).add(dataObject.getId());
        }
    }
}
