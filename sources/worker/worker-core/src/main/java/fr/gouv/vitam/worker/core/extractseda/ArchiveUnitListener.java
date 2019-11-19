/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 *******************************************************************************/
package fr.gouv.vitam.worker.core.extractseda;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.base.Strings;
import fr.gouv.culture.archivesdefrance.seda.v2.ArchiveUnitIdentifierKeyType;
import fr.gouv.culture.archivesdefrance.seda.v2.ArchiveUnitType;
import fr.gouv.culture.archivesdefrance.seda.v2.CustodialHistoryType;
import fr.gouv.culture.archivesdefrance.seda.v2.DataObjectOrArchiveUnitReferenceType;
import fr.gouv.culture.archivesdefrance.seda.v2.DataObjectRefType;
import fr.gouv.culture.archivesdefrance.seda.v2.DescriptiveMetadataContentType;
import fr.gouv.culture.archivesdefrance.seda.v2.IdentifierType;
import fr.gouv.culture.archivesdefrance.seda.v2.KeyType;
import fr.gouv.culture.archivesdefrance.seda.v2.LevelType;
import fr.gouv.culture.archivesdefrance.seda.v2.ObjectGroupRefType;
import fr.gouv.culture.archivesdefrance.seda.v2.OrganizationDescriptiveMetadataType;
import fr.gouv.culture.archivesdefrance.seda.v2.RelatedObjectReferenceType;
import fr.gouv.culture.archivesdefrance.seda.v2.SignatureType;
import fr.gouv.culture.archivesdefrance.seda.v2.TextType;
import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.exception.InvalidGuidOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.guid.GUIDReader;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.mapping.serializer.IdentifierTypeSerializer;
import fr.gouv.vitam.common.mapping.serializer.KeywordTypeSerializer;
import fr.gouv.vitam.common.mapping.serializer.LevelTypeSerializer;
import fr.gouv.vitam.common.mapping.serializer.TextByLangSerializer;
import fr.gouv.vitam.common.mapping.serializer.TextTypeSerializer;
import fr.gouv.vitam.common.mapping.serializer.XMLGregorianCalendarSerializer;
import fr.gouv.vitam.common.model.IngestWorkflowConstants;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.UnitType;
import fr.gouv.vitam.common.model.administration.ActivationStatus;
import fr.gouv.vitam.common.model.administration.IngestContractModel;
import fr.gouv.vitam.common.model.unit.ArchiveUnitRoot;
import fr.gouv.vitam.common.model.unit.DataObjectReference;
import fr.gouv.vitam.common.model.unit.DescriptiveMetadataModel;
import fr.gouv.vitam.common.model.unit.GotObj;
import fr.gouv.vitam.common.model.unit.RuleCategoryModel;
import fr.gouv.vitam.common.model.unit.RuleModel;
import fr.gouv.vitam.common.model.unit.TextByLang;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialNotFoundException;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleUnitParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.metadata.api.exception.MetaDataException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.metadata.core.database.collections.MetadataDocument;
import fr.gouv.vitam.processing.common.exception.ExceptionType;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.exception.ProcessingMalformedDataException;
import fr.gouv.vitam.processing.common.exception.ProcessingManifestReferenceException;
import fr.gouv.vitam.processing.common.exception.ProcessingNotFoundException;
import fr.gouv.vitam.processing.common.exception.ProcessingObjectGroupLinkingException;
import fr.gouv.vitam.processing.common.exception.ProcessingObjectReferenceException;
import fr.gouv.vitam.processing.common.exception.ProcessingTooManyUnitsFoundException;
import fr.gouv.vitam.processing.common.exception.ProcessingUnitLinkingException;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.mapping.ArchiveUnitMapper;
import fr.gouv.vitam.worker.core.mapping.DescriptiveMetadataMapper;
import fr.gouv.vitam.worker.core.mapping.RuleMapper;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper.originatingAgencies;
import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTION.FIELDS;
import static fr.gouv.vitam.common.database.parser.query.ParserTokens.PROJECTIONARGS.ID;
import static fr.gouv.vitam.common.database.parser.query.ParserTokens.PROJECTIONARGS.OBJECT;
import static fr.gouv.vitam.common.database.parser.query.ParserTokens.PROJECTIONARGS.OPERATIONS;
import static fr.gouv.vitam.common.database.parser.query.ParserTokens.PROJECTIONARGS.ORIGINATING_AGENCIES;
import static fr.gouv.vitam.common.database.parser.query.ParserTokens.PROJECTIONARGS.ORIGINATING_AGENCY;
import static fr.gouv.vitam.common.database.parser.query.ParserTokens.PROJECTIONARGS.UNITTYPE;
import static fr.gouv.vitam.common.database.parser.query.ParserTokens.PROJECTIONARGS.UNITUPS;
import static fr.gouv.vitam.worker.core.handler.ExtractSedaActionHandler.SUBTASK_EMPTY_KEY_ATTACHMENT;
import static fr.gouv.vitam.worker.core.handler.ExtractSedaActionHandler.SUBTASK_ERROR_PARSE_ATTACHMENT;
import static fr.gouv.vitam.worker.core.handler.ExtractSedaActionHandler.SUBTASK_INVALID_GUID_ATTACHMENT;
import static fr.gouv.vitam.worker.core.handler.ExtractSedaActionHandler.SUBTASK_NOT_FOUND_ATTACHMENT;
import static fr.gouv.vitam.worker.core.handler.ExtractSedaActionHandler.SUBTASK_NULL_LINK_PARENT_ID_ATTACHMENT;

/**
 * listener to unmarshall seda
 */
public class ArchiveUnitListener extends Unmarshaller.Listener {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ArchiveUnitListener.class);

    private static final String LFC_INITIAL_CREATION_EVENT_TYPE = "LFC_CREATION";

    private static final String ARCHIVE_UNIT_TMP_FILE_PREFIX = "AU_TMP_";
    private final Map<String, String> unitIdToGuid;
    private final Map<String, String> guidToUnitId;
    private final Map<String, JsonNode> existingGOTs;
    private ArchiveUnitMapper archiveUnitMapper;
    private HandlerIO handlerIO;
    private ObjectNode archiveUnitTree;
    private ObjectMapper objectMapper;
    private Map<String, String> unitIdToGroupId;
    private Map<String, List<String>> objectGroupIdToUnitId;
    private Map<String, String> dataObjectIdToObjectGroupId;
    private Map<String, GotObj> dataObjectIdWithoutObjectGroupId;
    private Map<String, LogbookLifeCycleParameters> guidToLifeCycleParameters;
    private Set<String> existingUnitGuids;
    private Map<String, String> existingUnitIdWithExistingObjectGroup;
    private LogbookTypeProcess logbookTypeProcess;
    private String containerId;
    private MetaDataClientFactory metaDataClientFactory;
    private Map<String, String> objectGroupIdToGuid;
    private Map<String, String> dataObjectIdToGuid;
    private Map<String, Set<String>> unitIdToSetOfRuleId;
    private UnitType workflowUnitType;
    private List<String> originatingAgencies;
    private IngestContractModel ingestContract;
    private Map<String, Boolean> isThereManifestRelatedReferenceRemained;
    private Map<String, String> existingGOTGUIDToNewGotGUIDInAttachment;
    private AdminManagementClientFactory adminManagementClientFactory;

    /**
     * @param handlerIO
     * @param archiveUnitTree
     * @param unitIdToGuid
     * @param guidToUnitId
     * @param unitIdToGroupId
     * @param objectGroupIdToUnitId
     * @param dataObjectIdToObjectGroupId
     * @param dataObjectIdWithoutObjectGroupId
     * @param guidToLifeCycleParameters
     * @param existingUnitGuids
     * @param logbookTypeProcess
     * @param containerId
     * @param metaDataClientFactory
     * @param objectGroupIdToGuid
     * @param dataObjectIdToGuid
     * @param unitIdToSetOfRuleId
     * @param workflowUnitType
     * @param originatingAgencies
     * @param existingGOTs
     * @param existingUnitIdWithExistingObjectGroup
     */
    public ArchiveUnitListener(HandlerIO handlerIO, ObjectNode archiveUnitTree, Map<String, String> unitIdToGuid,
        Map<String, String> guidToUnitId,
        Map<String, String> unitIdToGroupId,
        Map<String, List<String>> objectGroupIdToUnitId,
        Map<String, String> dataObjectIdToObjectGroupId,
        Map<String, GotObj> dataObjectIdWithoutObjectGroupId,
        Map<String, LogbookLifeCycleParameters> guidToLifeCycleParameters,
        Set<String> existingUnitGuids, LogbookTypeProcess logbookTypeProcess, String containerId,
        MetaDataClientFactory metaDataClientFactory,
        Map<String, String> objectGroupIdToGuid,
        Map<String, String> dataObjectIdToGuid, Map<String, Set<String>> unitIdToSetOfRuleId, UnitType workflowUnitType,
        List<String> originatingAgencies, Map<String, JsonNode> existingGOTs,
        Map<String, String> existingUnitIdWithExistingObjectGroup,
        Map<String, Boolean> isThereManifestRelatedReferenceRemained,
        Map<String, String> existingGOTGUIDToNewGotGUIDInAttachment,
        AdminManagementClientFactory adminManagementClientFactory) {
        this.unitIdToGroupId = unitIdToGroupId;
        this.objectGroupIdToUnitId = objectGroupIdToUnitId;
        this.dataObjectIdToObjectGroupId = dataObjectIdToObjectGroupId;
        this.dataObjectIdWithoutObjectGroupId = dataObjectIdWithoutObjectGroupId;
        this.guidToLifeCycleParameters = guidToLifeCycleParameters;
        this.existingUnitGuids = existingUnitGuids;
        this.logbookTypeProcess = logbookTypeProcess;
        this.containerId = containerId;
        this.metaDataClientFactory = metaDataClientFactory;
        this.objectGroupIdToGuid = objectGroupIdToGuid;
        this.dataObjectIdToGuid = dataObjectIdToGuid;
        this.unitIdToSetOfRuleId = unitIdToSetOfRuleId;
        this.handlerIO = handlerIO;
        this.archiveUnitTree = archiveUnitTree;
        this.unitIdToGuid = unitIdToGuid;
        this.guidToUnitId = guidToUnitId;
        this.workflowUnitType = workflowUnitType;
        this.originatingAgencies = originatingAgencies;
        this.existingGOTs = existingGOTs;
        this.objectMapper = getObjectMapper();
        this.existingUnitIdWithExistingObjectGroup = existingUnitIdWithExistingObjectGroup;
        this.isThereManifestRelatedReferenceRemained = isThereManifestRelatedReferenceRemained;
        this.existingGOTGUIDToNewGotGUIDInAttachment = existingGOTGUIDToNewGotGUIDInAttachment;

        DescriptiveMetadataMapper descriptiveMetadataMapper = new DescriptiveMetadataMapper();
        RuleMapper ruleMapper = new RuleMapper();
        archiveUnitMapper = new ArchiveUnitMapper(descriptiveMetadataMapper, ruleMapper);
        this.adminManagementClientFactory = adminManagementClientFactory;
    }

    private static ObjectMapper getObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.UPPER_CAMEL_CASE);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        SimpleModule module = new SimpleModule();
        module.addSerializer(TextType.class, new TextTypeSerializer());
        module.addSerializer(LevelType.class, new LevelTypeSerializer());
        module.addSerializer(IdentifierType.class, new IdentifierTypeSerializer());
        module.addSerializer(OrganizationDescriptiveMetadataType.class, new OrganizationDescriptiveMetadataTypeSerializer());
        module.addSerializer(XMLGregorianCalendar.class, new XMLGregorianCalendarSerializer());
        module.addSerializer(TextByLang.class, new TextByLangSerializer());
        module.addSerializer(KeyType.class, new KeywordTypeSerializer());

        objectMapper.registerModule(module);
        JavaTimeModule module1 = new JavaTimeModule();
        objectMapper.registerModule(module1);

        return objectMapper;
    }

    /**
     * listener call after end of unmarshall
     *
     * @param target
     * @param parent
     */
    @Override
    public void afterUnmarshal(Object target, Object parent) {

        if (target instanceof ArchiveUnitType) {
            ArchiveUnitType archiveUnitType = (ArchiveUnitType) target;
            String sedaAchiveUnitId = archiveUnitType.getId();
            JAXBElement jaxbElementParent = (JAXBElement) parent;

            String elementGUID = GUIDFactory.newUnitGUID(ParameterHelper.getTenantParameter()).toString();

            if (archiveUnitType.getArchiveUnitRefId() != null && !jaxbElementParent.isGlobalScope()) {
                // fillArchiveUnitTree(archiveUnitId, archiveUnitType);
                if (!archiveUnitType.getArchiveUnitRefId().equals(archiveUnitType.getArchiveUnitRefId().trim())) {
                    throw new RuntimeException(new ProcessingMalformedDataException(
                        "The ArchiveUnitRefId " + archiveUnitType.getArchiveUnitRefId() +
                            " contains line break or spaces"));
                }
                return;
            }
            if (archiveUnitType.getArchiveUnitRefId() != null) {

                String childArchiveUnitRef = archiveUnitType.getArchiveUnitRefId();
                ObjectNode childArchiveUnitNode = (ObjectNode) archiveUnitTree.get(childArchiveUnitRef);
                if (childArchiveUnitNode == null) {
                    // Create new Archive Unit Node
                    childArchiveUnitNode = JsonHandler.createObjectNode();
                }

                // Reference Management during tree creation
                final ArrayNode parentsField = childArchiveUnitNode.withArray(SedaConstants.PREFIX_UP);
                parentsField.addAll((ArrayNode) archiveUnitTree.get(sedaAchiveUnitId).get(SedaConstants.PREFIX_UP));
                archiveUnitTree.set(childArchiveUnitRef, childArchiveUnitNode);
                archiveUnitTree.without(sedaAchiveUnitId);
                return;
            }

            if (archiveUnitType.getManagement() != null &&
                archiveUnitType.getManagement().getUpdateOperation() != null &&
                (archiveUnitType.getManagement().getUpdateOperation().getSystemId() != null ||
                    archiveUnitType.getManagement().getUpdateOperation().getArchiveUnitIdentifierKey() != null)) {
                elementGUID = attachArchiveUnitToExisting(archiveUnitType, sedaAchiveUnitId);

            }

            List<Object> archiveUnitOrDataObjectReferenceOrAny =
                archiveUnitType.getArchiveUnitOrDataObjectReferenceOrDataObjectGroup();

            String groupId = buildGraph(sedaAchiveUnitId, elementGUID, archiveUnitOrDataObjectReferenceOrAny);

            ObjectNode archiveUnitNode = (ObjectNode) archiveUnitTree.get(sedaAchiveUnitId);
            if (archiveUnitNode == null) {
                // Create node
                archiveUnitNode = JsonHandler.createObjectNode();
                // or go search for it
            }

            fillCustodialHistoryReference(archiveUnitType);
            // Add new Archive Unit Entry
            archiveUnitTree.set(sedaAchiveUnitId, archiveUnitNode);

            unitIdToGuid.put(sedaAchiveUnitId, elementGUID);
            guidToUnitId.put(elementGUID, sedaAchiveUnitId);

            ArchiveUnitRoot archiveUnitRoot;
            try {
                String operationId = handlerIO.getContainerName();

                archiveUnitRoot = archiveUnitMapper.map(archiveUnitType, elementGUID, groupId, operationId,
                    workflowUnitType.name());
            } catch (ProcessingMalformedDataException | ProcessingObjectReferenceException e) {
                throw new VitamRuntimeException(e);
            }

            DescriptiveMetadataModel descriptiveMetadataModel =
                archiveUnitRoot.getArchiveUnit().getDescriptiveMetadataModel();


            enhanceSignatures(descriptiveMetadataModel);

            replaceInternalReferenceForRelatedObjectReference(sedaAchiveUnitId, descriptiveMetadataModel);

            // fill list rules to map
            fillListRulesToMap(sedaAchiveUnitId, archiveUnitRoot.getArchiveUnit().getManagement().getAccess());
            fillListRulesToMap(sedaAchiveUnitId, archiveUnitRoot.getArchiveUnit().getManagement().getStorage());
            fillListRulesToMap(sedaAchiveUnitId,
                archiveUnitRoot.getArchiveUnit().getManagement().getAppraisal());
            fillListRulesToMap(sedaAchiveUnitId,
                archiveUnitRoot.getArchiveUnit().getManagement().getClassification());
            fillListRulesToMap(sedaAchiveUnitId,
                archiveUnitRoot.getArchiveUnit().getManagement().getDissemination());
            fillListRulesToMap(sedaAchiveUnitId, archiveUnitRoot.getArchiveUnit().getManagement().getReuse());

            // If unit does not already exists then create lifecycle
            if (!existingUnitGuids.contains(elementGUID)) {
                storeArchiveUnit(elementGUID, archiveUnitRoot);
                createUnitLifeCycle(elementGUID, containerId, logbookTypeProcess);
            }

            // clean archiveUnitType
            archiveUnitType.setManagement(null);
            archiveUnitType.setContent(null);
            archiveUnitType.getArchiveUnitOrDataObjectReferenceOrDataObjectGroup().clear();
            // transform it to tree
            archiveUnitType.setArchiveUnitRefId(sedaAchiveUnitId);
        }

        super.afterUnmarshal(target, parent);
    }

    private void fillCustodialHistoryReference(ArchiveUnitType archiveUnitType) {
        DescriptiveMetadataContentType content = archiveUnitType.getContent();
        if (content == null || content.getCustodialHistory() == null ||
            content.getCustodialHistory().getCustodialHistoryFile() == null) {
            return;
        }

        CustodialHistoryType custodialHistoryType = content.getCustodialHistory();
        DataObjectRefType dataObjectReference = content.getCustodialHistory().getCustodialHistoryFile();

        String objectGroupReferenceId = dataObjectReference.getDataObjectGroupReferenceId();
        String objectReferenceId = dataObjectReference.getDataObjectReferenceId();

        DataObjectRefType custodialHistoryFile = new DataObjectRefType();
        if (dataObjectReference != null) {
            if (objectGroupReferenceId != null) {
                custodialHistoryFile
                    .setDataObjectGroupReferenceId(objectGroupIdToGuid.get(objectGroupReferenceId));
                custodialHistoryType.setCustodialHistoryFile(custodialHistoryFile);
                content.setCustodialHistory(custodialHistoryType);
                archiveUnitType.setContent(content);
            }

            if (objectReferenceId != null) {
                String objectId = dataObjectIdToObjectGroupId.get(objectReferenceId);
                custodialHistoryFile.setDataObjectReferenceId(objectGroupIdToGuid.get(objectId));
                custodialHistoryType.setCustodialHistoryFile(custodialHistoryFile);
                content.setCustodialHistory(custodialHistoryType);
                archiveUnitType.setContent(content);
            }
        }

    }

    /**
     * fill binaryId instead of internal seda id.
     *
     * @param descriptiveMetadataModel
     */
    private void enhanceSignatures(DescriptiveMetadataModel descriptiveMetadataModel) {

        if (descriptiveMetadataModel.getSignature() != null && !descriptiveMetadataModel.getSignature().isEmpty()) {
            for (SignatureType signature : descriptiveMetadataModel.getSignature()) {

                String signedObjectId =
                    signature.getReferencedObject().getSignedObjectId();

                if (dataObjectIdToGuid.containsKey(signedObjectId)) {
                    signature.getReferencedObject()
                        .setSignedObjectId(dataObjectIdToGuid.get(signedObjectId));
                }
            }
        }

    }

    /**
     * fill sytemGUID for all item of RelatedObjectReference (RelationGroup) instead of internal seda id (defined in manifest).
     *
     * @param archiveUnitId
     * @param descriptiveMetadataModel
     */
    private void replaceInternalReferenceForRelatedObjectReference(String archiveUnitId,
        DescriptiveMetadataModel descriptiveMetadataModel) {

        if (descriptiveMetadataModel.getRelatedObjectReference() != null) {

            RelatedObjectReferenceType relatedObjReference =
                descriptiveMetadataModel.getRelatedObjectReference();

            fillDataObjectOrArchiveUnitReference(archiveUnitId, relatedObjReference.getIsVersionOf());
            fillDataObjectOrArchiveUnitReference(archiveUnitId, relatedObjReference.getReplaces());
            fillDataObjectOrArchiveUnitReference(archiveUnitId, relatedObjReference.getRequires());
            fillDataObjectOrArchiveUnitReference(archiveUnitId, relatedObjReference.getIsPartOf());
            fillDataObjectOrArchiveUnitReference(archiveUnitId, relatedObjReference.getReferences());
        }
    }

    private void fillDataObjectOrArchiveUnitReference(String archiveUnitId,
        List<DataObjectOrArchiveUnitReferenceType> dataObjectOrArchiveUnitReference) {

        for (DataObjectOrArchiveUnitReferenceType relatedObjectReferenceItem : dataObjectOrArchiveUnitReference) {

            String archiveUnitRefId = relatedObjectReferenceItem.getArchiveUnitRefId();

            if (archiveUnitRefId != null) {
                if (unitIdToGuid.containsKey(archiveUnitRefId)) {
                    relatedObjectReferenceItem.setArchiveUnitRefId(unitIdToGuid.get(archiveUnitRefId));
                } else {
                    isThereManifestRelatedReferenceRemained.put(archiveUnitId, true);
                }
            }

            DataObjectRefType dataObjectRefType = relatedObjectReferenceItem.getDataObjectReference();

            if (dataObjectRefType != null) {
                if (dataObjectRefType.getDataObjectReferenceId() != null) {
                    String dataObjecRefId = dataObjectRefType.getDataObjectReferenceId();
                    if (dataObjectIdToGuid.containsKey(dataObjecRefId)) {
                        dataObjectRefType.setDataObjectReferenceId(dataObjectIdToGuid.get(dataObjecRefId));
                    } else {
                        isThereManifestRelatedReferenceRemained.put(archiveUnitId, true);
                    }
                }

                if (dataObjectRefType.getDataObjectGroupReferenceId() != null) {
                    String dataObjecGroupRefId = dataObjectRefType.getDataObjectGroupReferenceId();
                    if (objectGroupIdToGuid.containsKey(dataObjecGroupRefId)) {
                        dataObjectRefType.setDataObjectGroupReferenceId(objectGroupIdToGuid.get(dataObjecGroupRefId));
                    } else {
                        isThereManifestRelatedReferenceRemained.put(archiveUnitId, true);
                    }
                }
            }

            String repositoryArchiveUnitPID = relatedObjectReferenceItem.getRepositoryArchiveUnitPID();
            if (repositoryArchiveUnitPID != null) {
                if (dataObjectIdToGuid.containsKey(repositoryArchiveUnitPID)) {
                    relatedObjectReferenceItem.setRepositoryArchiveUnitPID(objectGroupIdToGuid.get(repositoryArchiveUnitPID));
                } else {
                    isThereManifestRelatedReferenceRemained.put(archiveUnitId, true);
                }
            }

            String getRepositoryObjectPID = relatedObjectReferenceItem.getRepositoryObjectPID();
            if (getRepositoryObjectPID != null) {
                if (dataObjectIdToGuid.containsKey(getRepositoryObjectPID)) {
                    relatedObjectReferenceItem.setRepositoryObjectPID(objectGroupIdToGuid.get(getRepositoryObjectPID));
                } else {
                    isThereManifestRelatedReferenceRemained.put(archiveUnitId, true);
                }
            }

            String externalReference = relatedObjectReferenceItem.getExternalReference();
            if (externalReference != null) {
                    relatedObjectReferenceItem.setExternalReference(externalReference);
            }
        }
    }

    /**
     * link the current archive unit to an existing archive unit We can link by systemId(guid) or by key value
     * (metadataName and metadataValue)
     *
     * @param archiveUnitType
     * @param archiveUnitId
     * @return
     */
    private String attachArchiveUnitToExisting(ArchiveUnitType archiveUnitType, String archiveUnitId) {


        String existingArchiveUnitGuid =
            archiveUnitType.getManagement().getUpdateOperation().getSystemId(); // check if systemId exist
        try {

            boolean isGuid = false;
            String keyValueUnitId;
            final String metadataName;
            final String metadataValue;
            if (null != existingArchiveUnitGuid) {
                isGuid = true;
                metadataName = ID.exactToken();
                metadataValue = existingArchiveUnitGuid;

                try {
                    GUIDReader.getGUID(existingArchiveUnitGuid);
                } catch (final InvalidGuidOperationException e) {
                    keyValueUnitId = "[MetadataName:" + metadataName + ", MetadataValue : " + metadataValue + "]";
                    throw new ProcessingNotFoundException(
                        "Unit " + archiveUnitId + ": [" + keyValueUnitId +
                            "] is not a valid systemId [guid]",
                        archiveUnitId,
                        existingArchiveUnitGuid, false, ExceptionType.UNIT, SUBTASK_INVALID_GUID_ATTACHMENT);
                }

            } else {
                ArchiveUnitIdentifierKeyType archiveUnitIdentifier =
                    archiveUnitType.getManagement().getUpdateOperation().getArchiveUnitIdentifierKey();
                metadataName = archiveUnitIdentifier.getMetadataName();
                metadataValue = archiveUnitIdentifier.getMetadataValue();
            }

            keyValueUnitId = "[MetadataName:" + metadataName + ", MetadataValue : " + metadataValue + "]";
            if (null == existingArchiveUnitGuid) {
                existingArchiveUnitGuid = keyValueUnitId;
            }

            JsonNode existingData = loadExistingArchiveUnitByKeyValue(metadataName, metadataValue, archiveUnitId);

            JsonNode result = (existingData == null) ? null : existingData.get("$results");

            if (result == null || result.size() == 0) {
                throw new ProcessingNotFoundException(
                    "Existing Unit " + archiveUnitId + ":" + keyValueUnitId + ", was not found",
                    archiveUnitId,
                    existingArchiveUnitGuid, isGuid, ExceptionType.UNIT, SUBTASK_NOT_FOUND_ATTACHMENT);
            }

            if (result.size() > 1) {
                throw new ProcessingTooManyUnitsFoundException(
                    "Unit " + archiveUnitId + ":" + keyValueUnitId + ", Multiple unit was found",
                    archiveUnitId,
                    existingArchiveUnitGuid, isGuid);
            }

            JsonNode unitInDB = result.get(0);
            String type = unitInDB.get("#unitType").asText();
            UnitType dataUnitType = UnitType.valueOf(type);

            // In case where systemId is key:value format, then erase value with the correct unit id
            existingArchiveUnitGuid = unitInDB.get("#id").asText();
            existingUnitGuids.add(existingArchiveUnitGuid);
            if (unitInDB.get("#object") != null && unitInDB.get("#object").asText() != null) {
                existingUnitIdWithExistingObjectGroup.put(existingArchiveUnitGuid, unitInDB.get("#object").asText());
            } else {
                DataObjectReference dataObjectReference =
                    archiveUnitMapper.mapAndValidateDataObjectReference(archiveUnitType);
                if (null != dataObjectReference) {
                    String got = dataObjectReference.getDataObjectGroupReferenceId();
                    throw new ProcessingObjectGroupLinkingException(
                        "Linking object (" + got + ") not allowed for unit (" + existingArchiveUnitGuid +
                            ") without ObjectGroup",
                        existingArchiveUnitGuid, got);
                }
            }

            if (dataUnitType.ordinal() < workflowUnitType.ordinal()) {
                throw new ProcessingUnitLinkingException(
                    "Linking Unauthorized to the ArchiveUnit (" + existingArchiveUnitGuid + ") type " + dataUnitType +
                        " and current ingest type is " + workflowUnitType, archiveUnitId, dataUnitType,
                    workflowUnitType);
            }

            // Do not get originating agencies of holding
            if (!UnitType.HOLDING_UNIT.equals(dataUnitType)) {
                ArrayNode originatingAgencies =
                    (ArrayNode) unitInDB.get(originatingAgencies());
                List<String> originatingAgencyList = new ArrayList<>();
                for (JsonNode agency : originatingAgencies) {
                    originatingAgencyList.add(agency.asText());
                }
                this.originatingAgencies.addAll(originatingAgencyList);
            }

        } catch (ProcessingException e) {
            throw new RuntimeException(e);
        }
        return existingArchiveUnitGuid;
    }

    private void fillListRulesToMap(String archiveUnitId, RuleCategoryModel ruleCategory) {
        if (ruleCategory == null) {
            return;
        }
        Set<String> rulesId =
            ruleCategory.getRules().stream()
                .map(RuleModel::getRule)
                .filter(item -> !Strings.isNullOrEmpty(item))
                .collect(Collectors.toSet());
        if (rulesId.size() <= 0) {
            return;
        }
        if (!unitIdToSetOfRuleId.containsKey(archiveUnitId)) {
            unitIdToSetOfRuleId.put(archiveUnitId, new HashSet<>());
        }
        unitIdToSetOfRuleId.get(archiveUnitId).addAll(rulesId);
    }

    private String buildGraph(String archiveUnitId, String archiveUnitGUID,
        List<Object> archiveUnitOrDataObjectReferenceOrAny) {
        String groupGUID = null;

        for (Object o : archiveUnitOrDataObjectReferenceOrAny) {
            if (o instanceof JAXBElement) {
                JAXBElement element = (JAXBElement) o;

                if (element.getDeclaredType().isAssignableFrom(ArchiveUnitType.class)) {
                    fillArchiveUnitTree(archiveUnitId, (ArchiveUnitType) element.getValue());
                } else if (element.getDeclaredType().isAssignableFrom(DataObjectRefType.class)) {
                    groupGUID = fillDataObjectGroup(archiveUnitId, element);

                    if (existingUnitIdWithExistingObjectGroup.containsKey(archiveUnitGUID)) {
                        existingGOTGUIDToNewGotGUIDInAttachment
                            .put(existingUnitIdWithExistingObjectGroup.get(archiveUnitGUID), groupGUID);
                    }
                } else if (element.getDeclaredType().isAssignableFrom(ObjectGroupRefType.class)) {
                    groupGUID = fillObjectGroup(archiveUnitId, element);
                }
            }
        }
        return groupGUID;
    }

    private String fillDataObjectGroup(String archiveUnitId, JAXBElement element) {
        DataObjectRefType dataObjectRefType = (DataObjectRefType) element.getValue();

        if (dataObjectRefType.getDataObjectReferenceId() != null) {
            String objRefId = dataObjectRefType.getDataObjectReferenceId();
            unitIdToGroupId.put(archiveUnitId, objRefId);
            if (objectGroupIdToUnitId.get(objRefId) == null) {
                final List<String> archiveUnitList = new ArrayList<>();
                archiveUnitList.add(archiveUnitId);
                if (dataObjectIdWithoutObjectGroupId.containsKey(objRefId)) {
                    final GotObj gotObj = dataObjectIdWithoutObjectGroupId.get(objRefId);
                    final String gotGuid = gotObj.getGotGuid();
                    objectGroupIdToUnitId.put(gotGuid, archiveUnitList);
                    unitIdToGroupId.put(archiveUnitId, gotGuid); // update unitIdToGroupId with new GOT
                    gotObj.setVisited(true); // update isVisited to true
                    dataObjectIdWithoutObjectGroupId.put(objRefId, gotObj);
                }
            } else {
                objectGroupIdToUnitId.get(objRefId).add(archiveUnitId);
            }

            try {
                return objectGroupIdToGuid.get(getNewGdoIdFromGdoByUnit(objRefId));
            } catch (ProcessingManifestReferenceException e) {
                throw new RuntimeException(e);
            }
        }

        if (dataObjectRefType.getDataObjectGroupReferenceId() != null) {

            final String groupId = dataObjectRefType.getDataObjectGroupReferenceId();
            unitIdToGroupId.put(archiveUnitId, groupId);
            if (objectGroupIdToUnitId.get(groupId) == null) {
                final List<String> archiveUnitList = new ArrayList<>();
                archiveUnitList.add(archiveUnitId);
                if (!dataObjectIdWithoutObjectGroupId.containsKey(groupId)) {
                    objectGroupIdToUnitId.put(groupId, archiveUnitList);
                }
            } else {
                final List<String> archiveUnitList = objectGroupIdToUnitId.get(groupId);
                archiveUnitList.add(archiveUnitId);
                objectGroupIdToUnitId.put(groupId, archiveUnitList);
            }
            // Create new startElement for group with new guid
            try {
                return objectGroupIdToGuid.get(getNewGdoIdFromGdoByUnit(unitIdToGroupId.get(archiveUnitId)));
            } catch (ProcessingManifestReferenceException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    private String fillObjectGroup(String archiveUnitId, JAXBElement element) {
        ObjectGroupRefType dataObjectRefType = (ObjectGroupRefType) element.getValue();

        if (dataObjectRefType.getDataObjectGroupExistingReferenceId() != null) {

            String groupId = dataObjectRefType.getDataObjectGroupExistingReferenceId();
            // Check that the object group exists

            JsonNode existingObjectGroup = loadExistingObjectGroup(groupId);

            if (existingObjectGroup == null || existingObjectGroup.get("$results") == null ||
                existingObjectGroup.get("$results").size() == 0) {
                throw new RuntimeException(new ProcessingNotFoundException(
                    "Existing ObjectGroup " + groupId + " was not found for AU " + archiveUnitId, archiveUnitId,
                    groupId, true, ExceptionType.GOT, SUBTASK_NOT_FOUND_ATTACHMENT));
            }

            unitIdToGroupId.put(archiveUnitId, groupId);
            objectGroupIdToGuid.put(groupId, groupId);

            if (objectGroupIdToUnitId.get(groupId) == null) {
                final List<String> archiveUnitList = new ArrayList<>();
                archiveUnitList.add(archiveUnitId);
                if (!dataObjectIdWithoutObjectGroupId.containsKey(groupId)) {
                    objectGroupIdToUnitId.put(groupId, archiveUnitList);
                }
            } else {
                final List<String> archiveUnitList = objectGroupIdToUnitId.get(groupId);
                archiveUnitList.add(archiveUnitId);
                objectGroupIdToUnitId.put(groupId, archiveUnitList);
            }



            /*
             * Add field _existing with value true in order to skip full indexation and just add necessary information
             * like _sps, _ops, _up
             */
            JsonNode ogInDB = existingObjectGroup.get("$results").get(0);
            ObjectNode work = JsonHandler.createObjectNode();
            ObjectNode originalOGGraphData =
                JsonHandler.createObjectNode();// information to save in LFC, if no ok or warn then we can rollback
            JsonNode ops = ogInDB.get(OPERATIONS.exactToken());

            // prevent idempotent, if ObjectGroup have already the operation do not re-treat it
            if (null != ops && ops.toString().contains(containerId)) {
                existingGOTs.put(groupId, null);
                return groupId;

            }

            originalOGGraphData.set(MetadataDocument.OPS, ops);
            originalOGGraphData.put(MetadataDocument.OPI, containerId);
            originalOGGraphData
                .set(MetadataDocument.ORIGINATING_AGENCIES, ogInDB.get(ORIGINATING_AGENCIES.exactToken()));
            originalOGGraphData.set(MetadataDocument.UP, ogInDB.get(UNITUPS.exactToken()));
            work.set(SedaConstants.PREFIX_EXISTING, originalOGGraphData);

            // This is used to be saved in ObjectGroup Folder and participate in distribution. But we must use it only
            // to update LFC and save in storage.
            ObjectNode existingOG = JsonHandler.createObjectNode();
            existingOG.set(SedaConstants.PREFIX_WORK, work);

            existingGOTs.put(groupId, existingOG);

            // Write existing objectGroup to workspace
            try {
                final File tmpFile = handlerIO.getNewLocalFile(groupId + ".json");
                JsonHandler.writeAsFile(existingOG, tmpFile);
                handlerIO.transferFileToWorkspace(
                    IngestWorkflowConstants.UPDATE_OBJECT_GROUP_FOLDER + "/" + groupId + ".json",
                    tmpFile, true, true);
            } catch (InvalidParseOperationException | ProcessingException e) {
                throw new RuntimeException(new ProcessingException("Error while saving existing got to workspace", e));
            }

            return groupId;
        }
        return null;
    }

    private void fillArchiveUnitTree(String archiveUnitId, ArchiveUnitType archiveUnitType) {

        String childArchiveUnitRef = archiveUnitType.getArchiveUnitRefId();
        // Check that childArchiveUnitRef is not an existing archive unit
        String childArchiveUnitRef_guid = unitIdToGuid.get(childArchiveUnitRef);
        if (existingUnitGuids.contains(childArchiveUnitRef_guid)) {
            throw new RuntimeException(new ProcessingManifestReferenceException(
                "The existing unit with guid [" + childArchiveUnitRef_guid + "] and manifest id [" +
                    childArchiveUnitRef + "] should not have as parent a manifest unit id [" + archiveUnitId + "] ",
                childArchiveUnitRef, childArchiveUnitRef_guid, archiveUnitId));
        }

        ObjectNode childArchiveUnitNode = (ObjectNode) archiveUnitTree.get(childArchiveUnitRef);
        if (childArchiveUnitNode == null) {
            // Create new Archive Unit Node
            childArchiveUnitNode = JsonHandler.createObjectNode();
        }

        // Reference Management during tree creation
        final ArrayNode parentsField = childArchiveUnitNode.withArray(SedaConstants.PREFIX_UP);
        parentsField.add(archiveUnitId);
        archiveUnitTree.set(childArchiveUnitRef, childArchiveUnitNode);
    }

    private void storeArchiveUnit(String elementGuid, ArchiveUnitRoot archiveUnitRoot) {
        File tmpFile = handlerIO.getNewLocalFile(ARCHIVE_UNIT_TMP_FILE_PREFIX + elementGuid);
        try {
            objectMapper.writeValue(tmpFile, archiveUnitRoot);
        } catch (IOException e) {
            throw new RuntimeException(new ProcessingException(e));
        }
    }

    /**
     * Get the object group id defined in data object or the data object without GO. In this map the new technical
     * object is created
     *
     * @param objIdRefByUnit il s'agit du DataObjectGroupReferenceId
     * @return
     */
    private String getNewGdoIdFromGdoByUnit(String objIdRefByUnit) throws ProcessingManifestReferenceException {

        final String gotGuid = dataObjectIdWithoutObjectGroupId.get(objIdRefByUnit) != null
            ? dataObjectIdWithoutObjectGroupId.get(objIdRefByUnit).getGotGuid()
            : null;

        if (Strings.isNullOrEmpty(dataObjectIdToObjectGroupId.get(objIdRefByUnit)) &&
            !Strings.isNullOrEmpty(gotGuid)) {

            // nominal case of do without go
            LOGGER.debug("The data object id " + objIdRefByUnit +
                ", is defined without the group object id " +
                dataObjectIdWithoutObjectGroupId.get(objIdRefByUnit) +
                ". The technical group object guid is " + gotGuid);

            return gotGuid;

        } else if (!Strings.isNullOrEmpty(dataObjectIdToObjectGroupId.get(objIdRefByUnit))) {
            LOGGER.debug("The data object id " + dataObjectIdWithoutObjectGroupId.get(objIdRefByUnit) +
                " referenced defined with the group object id " + objIdRefByUnit);
            // il y a un DO possédant le GO id
            return dataObjectIdToObjectGroupId.get(objIdRefByUnit);
        } else if (dataObjectIdToObjectGroupId.containsValue(objIdRefByUnit)) {
            // case objIdRefByUnit is an GO
            return objIdRefByUnit;
        } else {
            throw new ProcessingManifestReferenceException(
                "The group id " + objIdRefByUnit +
                    " doesn't reference a data object or got and it not include in data object", objIdRefByUnit,
                ExceptionType.GOT);
        }
    }

    private void createUnitLifeCycle(String unitGuid, String containerId,
        LogbookTypeProcess logbookTypeProcess) {
        final LogbookLifeCycleUnitParameters logbookLifecycleUnitParameters =
            (LogbookLifeCycleUnitParameters) initLogbookLifeCycleParameters(
                unitGuid, true, false);

        logbookLifecycleUnitParameters.setFinalStatus(LFC_INITIAL_CREATION_EVENT_TYPE, null, StatusCode.OK, null);

        logbookLifecycleUnitParameters.putParameterValue(LogbookParameterName.eventIdentifierProcess, containerId);
        logbookLifecycleUnitParameters.putParameterValue(LogbookParameterName.eventIdentifier,
            GUIDFactory.newEventGUID(ParameterHelper.getTenantParameter()).toString());
        logbookLifecycleUnitParameters.putParameterValue(LogbookParameterName.eventTypeProcess,
            logbookTypeProcess.name());

        // Update guidToLifeCycleParameters
        guidToLifeCycleParameters.put(unitGuid, logbookLifecycleUnitParameters);
    }

    private LogbookParameters initLogbookLifeCycleParameters(String guid, boolean isArchive, boolean isObjectGroup) {
        LogbookParameters logbookLifeCycleParameters = guidToLifeCycleParameters.get(guid);
        if (logbookLifeCycleParameters == null) {
            logbookLifeCycleParameters = isArchive ? LogbookParametersFactory.newLogbookLifeCycleUnitParameters()
                : isObjectGroup ? LogbookParametersFactory.newLogbookLifeCycleObjectGroupParameters()
                : LogbookParametersFactory.newLogbookOperationParameters();

            logbookLifeCycleParameters.putParameterValue(LogbookParameterName.objectIdentifier, guid);
        }
        return logbookLifeCycleParameters;
    }

    /**
     * @param metadataName field in the archive unit
     * @param metadataValue the value
     * @param archiveUnitId
     * @return
     * @throws ProcessingException
     */
    private JsonNode loadExistingArchiveUnitByKeyValue(String metadataName, String metadataValue, String archiveUnitId)
        throws ProcessingException {
        if (metadataName.isEmpty() || metadataValue.isEmpty()) {
            throw new ProcessingNotFoundException(
                "Unit " + archiveUnitId + ": [MetadataName:" + metadataName + ", MetadataValue : " + metadataValue +
                    "] are required values",
                archiveUnitId,
                "[MetadataName:" + metadataName + ", MetadataValue : " + metadataValue + "]", false, ExceptionType.UNIT,
                SUBTASK_EMPTY_KEY_ATTACHMENT);
        }

        final SelectMultiQuery select = new SelectMultiQuery();
        try {
            Query qr = tryApplyIngestContractRestriction(ingestContract, QueryHelper.eq(metadataName, metadataValue));
            select.setQuery(qr);
        } catch (IllegalStateException e) {
            throw new ProcessingNotFoundException(
                "Unit " + archiveUnitId + ":  [MetadataName:" + metadataName + ", MetadataValue : " + metadataValue +
                    "] : " + e.getMessage(),
                archiveUnitId,
                "[MetadataName:" + metadataName + ", MetadataValue : " + metadataValue +
                    "]",
                false, ExceptionType.UNIT, SUBTASK_NULL_LINK_PARENT_ID_ATTACHMENT);
        } catch (InvalidCreateOperationException e) {
            throw new ProcessingNotFoundException(
                "Unit " + archiveUnitId + ":  [MetadataName:" + metadataName + ", MetadataValue : " + metadataValue +
                    "] : " + e.getMessage(),
                archiveUnitId,
                "[MetadataName:" + metadataName + ", MetadataValue : " + metadataValue +
                    "]",
                false, ExceptionType.UNIT, SUBTASK_ERROR_PARSE_ATTACHMENT);
        }
        return loadExistingArchiveUnit(select);
    }

    /**
     * Load data of an existing archive unit by its vitam id.
     *
     * @return AU response
     * @throws ProcessingNotFoundException thrown if unit not found
     * @throws ProcessingException thrown if a metadata exception occured
     */
    private JsonNode loadExistingArchiveUnit(SelectMultiQuery selectMultiQuery) throws ProcessingException {

        try (MetaDataClient metadataClient = metaDataClientFactory.getClient()) {

            ObjectNode projection = JsonHandler.createObjectNode();
            ObjectNode fields = JsonHandler.createObjectNode();
            fields.put(UNITTYPE.exactToken(), 1);
            fields.put(ID.exactToken(), 1);
            fields.put(ORIGINATING_AGENCIES.exactToken(), 1);
            fields.put(ORIGINATING_AGENCY.exactToken(), 1);
            fields.put(OBJECT.exactToken(), 1);
            projection.set(FIELDS.exactToken(), fields);

            selectMultiQuery.setProjection(projection);

            return metadataClient.selectUnits(selectMultiQuery.getFinalSelect());

        } catch (final MetaDataException e) {
            throw new ProcessingException(e);

        } catch (final InvalidParseOperationException e) {
            throw new ProcessingException("Json Parse error ", e);
        }
    }

    /**
     * Load data of an existing object group by its vitam id.
     *
     * @param objectGroupId guid of archive unit
     * @return AU response
     * @throws ProcessingNotFoundException thrown if unit not found
     * @throws ProcessingException thrown if a metadata exception occured
     */
    private JsonNode loadExistingObjectGroup(String objectGroupId) {

        try (MetaDataClient metadataClient = metaDataClientFactory.getClient()) {
            final SelectParserMultiple selectRequest = new SelectParserMultiple();
            final SelectMultiQuery request = selectRequest.getRequest().reset();
            ObjectNode projection = JsonHandler.createObjectNode();
            ObjectNode fields = JsonHandler.createObjectNode();
            fields.put(UNITUPS.exactToken(), 1);
            fields.put(ID.exactToken(), 1);
            fields.put(ORIGINATING_AGENCIES.exactToken(), 1);
            fields.put(OPERATIONS.exactToken(), 1);
            projection.set(FIELDS.exactToken(), fields);
            request.setProjection(projection);

            return metadataClient.selectObjectGrouptbyId(request.getFinalSelect(), objectGroupId);

        } catch (final MetaDataException e) {
            throw new RuntimeException(new ProcessingException(e));

        } catch (final InvalidParseOperationException e) {
            throw new RuntimeException(
                new ProcessingException("Existing ObjectGroup " + objectGroupId + " was not found"));
        }
    }

    public Query tryApplyIngestContractRestriction(IngestContractModel ingestContract, Query query)
        throws InvalidCreateOperationException {
        if (null != ingestContract) {
            if (ActivationStatus.ACTIVE.equals(ingestContract.getCheckParentLink())) {
                if (!Strings.isNullOrEmpty(ingestContract.getLinkParentId())) {
                    query = QueryHelper.and().add(
                        query,
                        QueryHelper.or().add(
                            QueryHelper.eq(BuilderToken.PROJECTIONARGS.ID.exactToken(),
                                ingestContract.getLinkParentId()),
                            QueryHelper.in(BuilderToken.PROJECTIONARGS.ALLUNITUPS.exactToken(),
                                ingestContract.getLinkParentId())

                        ));
                } else {
                    throw new IllegalStateException(
                        "Cannot apply restriction: IngestContract activation status is active but linkedParentId is NULL");
                }
            }
        }
        return query;
    }

    /**
     * Load Ingest Contract
     *
     * @param ingestContractIdentifier
     */
    public IngestContractModel loadIngestContract(String ingestContractIdentifier) {
        if (Strings.isNullOrEmpty(ingestContractIdentifier)) {
            return null;
        }
        try (final AdminManagementClient adminClient = adminManagementClientFactory.getClient()) {
            RequestResponse<IngestContractModel> referenceContracts =
                adminClient.findIngestContractsByID(ingestContractIdentifier);
            if (referenceContracts.isOk()) {
                List<IngestContractModel> results = ((RequestResponseOK) referenceContracts).getResults();
                if (!results.isEmpty()) {
                    for (IngestContractModel result : results) {
                        this.ingestContract = result;
                        return result;
                    }
                }
            }
            throw new RuntimeException(new ProcessingException(
                "IngestContract with identifier [" + ingestContractIdentifier + "] not found."));
        } catch (ReferentialNotFoundException | AdminManagementClientServerException |
            InvalidParseOperationException e) {
            throw new RuntimeException(new ProcessingException(e));
        }
    }

}
