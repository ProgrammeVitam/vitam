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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import fr.gouv.culture.archivesdefrance.seda.v2.ArchiveUnitIdentifierKeyType;
import fr.gouv.culture.archivesdefrance.seda.v2.ArchiveUnitType;
import fr.gouv.culture.archivesdefrance.seda.v2.CustodialHistoryType;
import fr.gouv.culture.archivesdefrance.seda.v2.DataObjectOrArchiveUnitReferenceType;
import fr.gouv.culture.archivesdefrance.seda.v2.DataObjectRefType;
import fr.gouv.culture.archivesdefrance.seda.v2.DescriptiveMetadataContentType;
import fr.gouv.culture.archivesdefrance.seda.v2.ObjectGroupRefType;
import fr.gouv.culture.archivesdefrance.seda.v2.RelatedObjectReferenceType;
import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
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
import fr.gouv.vitam.common.mapping.mapper.VitamObjectMapper;
import fr.gouv.vitam.common.model.IngestWorkflowConstants;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.UnitType;
import fr.gouv.vitam.common.model.administration.IngestContractCheckState;
import fr.gouv.vitam.common.model.administration.IngestContractModel;
import fr.gouv.vitam.common.model.administration.ManagementContractModel;
import fr.gouv.vitam.common.model.administration.PersistentIdentifierPolicy;
import fr.gouv.vitam.common.model.administration.PersistentIdentifierPolicyTypeEnum;
import fr.gouv.vitam.common.model.unit.ArchiveUnitRoot;
import fr.gouv.vitam.common.model.unit.DataObjectReference;
import fr.gouv.vitam.common.model.unit.DescriptiveMetadataModel;
import fr.gouv.vitam.common.model.unit.GotObj;
import fr.gouv.vitam.common.model.unit.RuleCategoryModel;
import fr.gouv.vitam.common.model.unit.RuleModel;
import fr.gouv.vitam.common.model.unit.SignatureTypeModel;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleUnitParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.metadata.api.exception.MetaDataException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.metadata.core.database.collections.MetadataDocument;
import fr.gouv.vitam.processing.common.exception.ExceptionType;
import fr.gouv.vitam.processing.common.exception.ProcessingAttachmentUnauthorizedException;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.exception.ProcessingMalformedDataException;
import fr.gouv.vitam.processing.common.exception.ProcessingManifestReferenceException;
import fr.gouv.vitam.processing.common.exception.ProcessingNotFoundException;
import fr.gouv.vitam.processing.common.exception.ProcessingNotValidLinkingException;
import fr.gouv.vitam.processing.common.exception.ProcessingObjectGroupLinkingException;
import fr.gouv.vitam.processing.common.exception.ProcessingObjectReferenceException;
import fr.gouv.vitam.processing.common.exception.ProcessingTooManyUnitsFoundException;
import fr.gouv.vitam.processing.common.exception.ProcessingUnitLinkingException;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.mapping.ArchiveUnitMapper;
import fr.gouv.vitam.worker.core.mapping.DescriptiveMetadataMapper;
import fr.gouv.vitam.worker.core.mapping.RuleMapper;
import fr.gouv.vitam.worker.core.utils.JsonLineDataBase;

import javax.annotation.Nonnull;
import javax.xml.bind.JAXBElement;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper.originatingAgencies;
import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTION.FIELDS;
import static fr.gouv.vitam.common.database.parser.query.ParserTokens.PROJECTIONARGS.ALLUNITUPS;
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
public class ArchiveUnitListener {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ArchiveUnitListener.class);

    private static final String LFC_INITIAL_CREATION_EVENT_TYPE = "LFC_CREATION";

    private static final String ARCHIVE_UNIT_TMP_FILE_PREFIX = "AU_TMP_";
    private final ArchiveUnitMapper archiveUnitMapper;
    private final IngestContext ingestContext;
    private final IngestSession ingestSession;

    private final HandlerIO handlerIO;

    private final JsonLineDataBase unitsDatabase;

    private boolean attachByIngestContractChecked = false;
    private final MetaDataClientFactory metaDataClientFactory;

    /**
     * @param handlerIO
     */
    public ArchiveUnitListener(HandlerIO handlerIO, IngestContext ingestContext, IngestSession ingestSession,
        JsonLineDataBase unitsDatabase, MetaDataClientFactory metaDataClientFactory) {
        this.handlerIO = handlerIO;
        this.ingestContext = ingestContext;
        this.ingestSession = ingestSession;
        this.unitsDatabase = unitsDatabase;
        DescriptiveMetadataMapper descriptiveMetadataMapper = new DescriptiveMetadataMapper();
        RuleMapper ruleMapper = new RuleMapper();
        archiveUnitMapper = new ArchiveUnitMapper(descriptiveMetadataMapper, ruleMapper);
        this.metaDataClientFactory = metaDataClientFactory;
    }

    /**
     * listener call after end of unmarshall
     *
     * @param archiveUnitType
     * @param jaxbElementParent
     */
    public void extractArchiveUnit(@Nonnull ArchiveUnitType archiveUnitType,
        @Nonnull JAXBElement<?> jaxbElementParent) {
        String sedaAchiveUnitId = archiveUnitType.getId();

        String elementGUID = GUIDFactory.newUnitGUID(ParameterHelper.getTenantParameter()).toString();

        // Check if we can attach by ingest contract
        checkAutoAttachmentsByIngestContract();

        boolean isValidAttachment =
            archiveUnitType.getManagement() != null && archiveUnitType.getManagement().getUpdateOperation() != null &&
                (archiveUnitType.getManagement().getUpdateOperation().getSystemId() != null ||
                    archiveUnitType.getManagement().getUpdateOperation().getArchiveUnitIdentifierKey() != null);

        if (isValidAttachment) {
            elementGUID = attachArchiveUnitToExisting(archiveUnitType, sedaAchiveUnitId);
        }

        if (archiveUnitType.getArchiveUnitRefId() != null && !jaxbElementParent.isGlobalScope()) {
            if (!archiveUnitType.getArchiveUnitRefId().equals(archiveUnitType.getArchiveUnitRefId().trim())) {
                throw new VitamRuntimeException(new ProcessingMalformedDataException(
                    "The ArchiveUnitRefId " + archiveUnitType.getArchiveUnitRefId() +
                        " contains line break or spaces"));
            }
            return;
        }

        if (archiveUnitType.getArchiveUnitRefId() != null) {

            String childArchiveUnitRef = archiveUnitType.getArchiveUnitRefId();
            ObjectNode childArchiveUnitNode = (ObjectNode) ingestSession.getArchiveUnitTree().get(childArchiveUnitRef);
            if (childArchiveUnitNode == null) {
                // Create new Archive Unit Node
                childArchiveUnitNode = JsonHandler.createObjectNode();
            }

            // Reference Management during tree creation
            final ArrayNode parentsField = childArchiveUnitNode.withArray(SedaConstants.PREFIX_UP);
            parentsField.addAll(
                (ArrayNode) ingestSession.getArchiveUnitTree().get(sedaAchiveUnitId).get(SedaConstants.PREFIX_UP));
            ingestSession.getArchiveUnitTree().set(childArchiveUnitRef, childArchiveUnitNode);
            ingestSession.getArchiveUnitTree().without(sedaAchiveUnitId);
            return;
        }


        List<Object> archiveUnitOrDataObjectReferenceOrAny =
            archiveUnitType.getArchiveUnitOrDataObjectReferenceOrDataObjectGroup();

        String groupId = buildGraph(sedaAchiveUnitId, elementGUID, archiveUnitOrDataObjectReferenceOrAny);

        ObjectNode archiveUnitNode = (ObjectNode) ingestSession.getArchiveUnitTree().get(sedaAchiveUnitId);
        if (archiveUnitNode == null) {
            // Create node
            archiveUnitNode = JsonHandler.createObjectNode();
            // or go search for it
        }
        fillCustodialHistoryReference(archiveUnitType);
        // Add new Archive Unit Entry
        ingestSession.getArchiveUnitTree().set(sedaAchiveUnitId, archiveUnitNode);

        ingestSession.getUnitIdToGuid().put(sedaAchiveUnitId, elementGUID);
        ingestSession.getGuidToUnitId().put(elementGUID, sedaAchiveUnitId);

        ArchiveUnitRoot archiveUnitRoot;
        try {
            String operationId = handlerIO.getContainerName();

            archiveUnitRoot = archiveUnitMapper.map(archiveUnitType, elementGUID, groupId, operationId,
                ingestContext.getWorkflowUnitType().name(), ingestContext.getSedaVersion());

            ManagementContractModel managementContractModel = ingestContext.getManagementContractModel();
            if (managementContractModel != null) {
                List<PersistentIdentifierPolicy> persistentIdentifierPolicies = managementContractModel.getPersistentIdentifierPolicyList();
                if (persistentIdentifierPolicies != null && !persistentIdentifierPolicies.isEmpty()) {
                    Optional<PersistentIdentifierPolicy> arkPolicy = persistentIdentifierPolicies.stream()
                        .filter(policy -> policy.isPersistentIdentifierUnit() && policy.getPersistentIdentifierPolicyType().equals(PersistentIdentifierPolicyTypeEnum.ARK))
                        .findFirst();
                    arkPolicy.ifPresent(policy -> archiveUnitRoot.getArchiveUnit().setManagementContractId(managementContractModel.getIdentifier()));
                }
            }
        } catch (ProcessingMalformedDataException | ProcessingObjectReferenceException e) {
            throw new VitamRuntimeException(e);
        }

        DescriptiveMetadataModel descriptiveMetadataModel =
            archiveUnitRoot.getArchiveUnit().getDescriptiveMetadataModel();

        replaceInternalReferenceForRelatedObjectReference(sedaAchiveUnitId, descriptiveMetadataModel);

        // fill list rules to map
        fillListRulesToMap(sedaAchiveUnitId, archiveUnitRoot.getArchiveUnit().getManagement().getAccess());
        fillListRulesToMap(sedaAchiveUnitId, archiveUnitRoot.getArchiveUnit().getManagement().getStorage());
        fillListRulesToMap(sedaAchiveUnitId, archiveUnitRoot.getArchiveUnit().getManagement().getAppraisal());
        fillListRulesToMap(sedaAchiveUnitId, archiveUnitRoot.getArchiveUnit().getManagement().getClassification());
        fillListRulesToMap(sedaAchiveUnitId, archiveUnitRoot.getArchiveUnit().getManagement().getDissemination());
        fillListRulesToMap(sedaAchiveUnitId, archiveUnitRoot.getArchiveUnit().getManagement().getReuse());
        fillListRulesToMap(sedaAchiveUnitId, archiveUnitRoot.getArchiveUnit().getManagement().getHold());

        // If unit does not already exists then create lifecycle
        if (!ingestSession.getExistingUnitGuids().contains(elementGUID)) {
            storeArchiveUnit(unitsDatabase, elementGUID, archiveUnitRoot);
            createUnitLifeCycle(elementGUID, ingestContext.getOperationId(), ingestContext.getTypeProcess());
        }

        // clean archiveUnitType
        archiveUnitType.setManagement(null);
        archiveUnitType.setContent(null);
        archiveUnitType.getArchiveUnitOrDataObjectReferenceOrDataObjectGroup().clear();
        // transform it to tree
        archiveUnitType.setArchiveUnitRefId(sedaAchiveUnitId);
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
        if (objectGroupReferenceId != null) {
            String objectGroupGuid = ingestSession.getObjectGroupIdToGuid().get(objectGroupReferenceId);
            if (objectGroupGuid == null) {
                throw new VitamRuntimeException(new ProcessingNotValidLinkingException(
                    "Could not find ObjectGroup with Id = " + objectGroupReferenceId));
            }
            custodialHistoryFile.setDataObjectGroupReferenceId(objectGroupGuid);
            custodialHistoryType.setCustodialHistoryFile(custodialHistoryFile);
            content.setCustodialHistory(custodialHistoryType);
            archiveUnitType.setContent(content);
        }

        if (objectReferenceId != null) {
            String objectGroupId = ingestSession.getDataObjectIdToObjectGroupId().get(objectReferenceId);
            if (objectGroupId == null) {
                throw new VitamRuntimeException(
                    new ProcessingNotValidLinkingException("Could not find Object with Id = " + objectReferenceId));
            }
            custodialHistoryFile.setDataObjectReferenceId(ingestSession.getObjectGroupIdToGuid().get(objectGroupId));
            custodialHistoryType.setCustodialHistoryFile(custodialHistoryFile);
            content.setCustodialHistory(custodialHistoryType);
            archiveUnitType.setContent(content);
        }

    }

    private void checkAutoAttachmentsByIngestContract() {
        if (attachByIngestContractChecked) {
            return;
        }

        IngestContractModel ingestContract = ingestContext.getIngestContract();
        if (ingestContract == null) {
            return;
        }

        if (null == ingestContract.getLinkParentId()) {
            return;
        }

        String checkUnit = "CheckAttachment";
        try {
            if (Strings.isNullOrEmpty(ingestContract.getLinkParentId())) {
                throw new ProcessingNotFoundException("IngestContract LinParentId mustn't be null or empty", checkUnit,
                    ingestContract.getLinkParentId(), true, ExceptionType.UNIT, SUBTASK_EMPTY_KEY_ATTACHMENT);
            }

            final SelectMultiQuery select = new SelectMultiQuery();
            try {
                Query qr = QueryHelper.eq(ID.exactToken(), ingestContract.getLinkParentId());
                select.setQuery(qr);
            } catch (InvalidCreateOperationException e) {
                throw new ProcessingNotFoundException("Parse error : " + e.getMessage(), checkUnit,
                    ingestContract.getLinkParentId(), true, ExceptionType.UNIT, SUBTASK_ERROR_PARSE_ATTACHMENT);
            }

            JsonNode linkParentUnitResponse = loadExistingArchiveUnit(select);

            JsonNode linkParentUnitResult =
                (linkParentUnitResponse == null) ? null : linkParentUnitResponse.get("$results");

            if (linkParentUnitResult == null || linkParentUnitResult.size() == 0) {
                throw new ProcessingNotFoundException(
                    "Existing IngestContract LinkParentId :" + ingestContract.getLinkParentId() + ", was not found",
                    checkUnit, ingestContract.getLinkParentId(), true, ExceptionType.UNIT,
                    SUBTASK_NOT_FOUND_ATTACHMENT);
            }

            if (linkParentUnitResult.size() > 1) {
                throw new ProcessingTooManyUnitsFoundException(
                    "Existing IngestContract LinkParentId ::" + ingestContract.getLinkParentId() +
                        ", Multiple unit was found", checkUnit, ingestContract.getLinkParentId(), true);
            }

            JsonNode linkParentUnit = linkParentUnitResult.get(0);
            String type = linkParentUnit.get("#unitType").asText();
            UnitType dataUnitType = UnitType.valueOf(type);

            UnitType workflowUnitType = ingestContext.getWorkflowUnitType();
            if (dataUnitType.ordinal() < workflowUnitType.ordinal()) {
                throw new ProcessingUnitLinkingException(
                    "Auto-linking by ingest contract unauthorized to the ArchiveUnit (" +
                        ingestContract.getLinkParentId() + ") type " + dataUnitType + " and current ingest type is " +
                        workflowUnitType, checkUnit, dataUnitType, workflowUnitType);
            }
        } catch (ProcessingException e) {
            throw new VitamRuntimeException(e);
        } finally {
            attachByIngestContractChecked = true;
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

            RelatedObjectReferenceType relatedObjReference = descriptiveMetadataModel.getRelatedObjectReference();

            fillDataObjectOrArchiveUnitReference(archiveUnitId, relatedObjReference.getIsVersionOf());
            fillDataObjectOrArchiveUnitReference(archiveUnitId, relatedObjReference.getReplaces());
            fillDataObjectOrArchiveUnitReference(archiveUnitId, relatedObjReference.getRequires());
            fillDataObjectOrArchiveUnitReference(archiveUnitId, relatedObjReference.getIsPartOf());
            fillDataObjectOrArchiveUnitReference(archiveUnitId, relatedObjReference.getReferences());
        }
    }

    private void fillDataObjectOrArchiveUnitReference(String archiveUnitId,
        List<DataObjectOrArchiveUnitReferenceType> dataObjectOrArchiveUnitReference) {
        Map<String, String> unitIdToGuid = ingestSession.getUnitIdToGuid();
        Map<String, Boolean> isThereManifestRelatedReferenceRemained =
            ingestSession.getIsThereManifestRelatedReferenceRemained();
        Map<String, String> dataObjectIdToGuid = ingestSession.getDataObjectIdToGuid();
        Map<String, String> objectGroupIdToGuid = ingestSession.getObjectGroupIdToGuid();

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
                    relatedObjectReferenceItem.setRepositoryArchiveUnitPID(
                        objectGroupIdToGuid.get(repositoryArchiveUnitPID));
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

        // check if systemId exist
        String existingArchiveUnitGuid = archiveUnitType.getManagement().getUpdateOperation().getSystemId();

        IngestContractModel ingestContract = ingestContext.getIngestContract();
        UnitType workflowUnitType = ingestContext.getWorkflowUnitType();
        try {
            if (ingestContract != null &&
                IngestContractCheckState.UNAUTHORIZED.equals(ingestContract.getCheckParentLink())) {
                throw new ProcessingAttachmentUnauthorizedException(
                    "ingest contract does not allow to attach archive unit to existing");
            }

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
                        "Unit " + archiveUnitId + ": [" + keyValueUnitId + "] is not a valid systemId [guid]",
                        archiveUnitId, existingArchiveUnitGuid, false, ExceptionType.UNIT,
                        SUBTASK_INVALID_GUID_ATTACHMENT);
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
                    "Existing Unit " + archiveUnitId + ":" + keyValueUnitId + ", was not found", archiveUnitId,
                    existingArchiveUnitGuid, isGuid, ExceptionType.UNIT, SUBTASK_NOT_FOUND_ATTACHMENT);
            }

            if (result.size() > 1) {
                throw new ProcessingTooManyUnitsFoundException(
                    "Unit " + archiveUnitId + ":" + keyValueUnitId + ", Multiple unit was found", archiveUnitId,
                    existingArchiveUnitGuid, isGuid);
            }

            JsonNode unitInDB = result.get(0);
            String type = unitInDB.get("#unitType").asText();
            UnitType dataUnitType = UnitType.valueOf(type);

            if (ingestContractRestrictAttachment(ingestContract, unitInDB)) {
                throw new ProcessingUnitLinkingException(
                    "archive unit is not equals or is not descending from allowed units : " +
                        ingestContract.getCheckParentId(), archiveUnitId, null, workflowUnitType);
            }

            // In case where systemId is key:value format, then erase value with the correct unit id
            existingArchiveUnitGuid = unitInDB.get("#id").asText();
            ingestSession.getExistingUnitGuids().add(existingArchiveUnitGuid);
            if (unitInDB.get("#object") != null && unitInDB.get("#object").asText() != null) {
                ingestSession.getExistingUnitIdWithExistingObjectGroup()
                    .put(existingArchiveUnitGuid, unitInDB.get("#object").asText());
            } else {
                DataObjectReference dataObjectReference =
                    archiveUnitMapper.mapAndValidateDataObjectReference(archiveUnitType);
                if (null != dataObjectReference) {
                    String got = dataObjectReference.getDataObjectGroupReferenceId();
                    throw new ProcessingObjectGroupLinkingException(
                        "Linking object (" + got + ") not allowed for unit (" + existingArchiveUnitGuid +
                            ") without ObjectGroup", existingArchiveUnitGuid, got);
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
                ArrayNode originAgencies = (ArrayNode) unitInDB.get(originatingAgencies());
                List<String> originatingAgencyList = new ArrayList<>();
                for (JsonNode agency : originAgencies) {
                    originatingAgencyList.add(agency.asText());
                }
                ingestSession.getOriginatingAgencies().addAll(originatingAgencyList);
            }

        } catch (ProcessingException e) {
            throw new VitamRuntimeException(e);
        }
        return existingArchiveUnitGuid;
    }

    private boolean ingestContractRestrictAttachment(IngestContractModel ingestContract, JsonNode unitInDB) {
        JsonNode ascendants = unitInDB.get(ALLUNITUPS.exactToken());
        String unitId = unitInDB.get(ID.exactToken()).asText();
        if (ingestContract == null || ingestContract.getCheckParentId() == null ||
            ingestContract.getCheckParentId().isEmpty() || ingestContract.getCheckParentId().contains(unitId)) {
            return false;
        }


        for (JsonNode ascendant : ascendants) {
            if (ingestContract.getCheckParentId().contains(ascendant.asText())) {
                return false;
            }
        }

        return true;
    }

    private void fillListRulesToMap(String archiveUnitId, RuleCategoryModel ruleCategory) {
        if (ruleCategory == null) {
            return;
        }
        Set<String> rulesId =
            ruleCategory.getRules().stream().map(RuleModel::getRule).filter(item -> !Strings.isNullOrEmpty(item))
                .collect(Collectors.toSet());
        if (rulesId.size() == 0) {
            return;
        }
        Map<String, Set<String>> unitIdToSetOfRuleId = ingestSession.getUnitIdToSetOfRuleId();
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
                JAXBElement<?> element = (JAXBElement<?>) o;

                if (element.getDeclaredType().isAssignableFrom(ArchiveUnitType.class)) {
                    fillArchiveUnitTree(archiveUnitId, (ArchiveUnitType) element.getValue());
                } else if (element.getDeclaredType().isAssignableFrom(DataObjectRefType.class)) {
                    groupGUID = fillDataObjectGroup(archiveUnitId, element);

                    if (ingestSession.getExistingUnitIdWithExistingObjectGroup().containsKey(archiveUnitGUID)) {
                        ingestSession.getExistingGOTGUIDToNewGotGUIDInAttachment()
                            .put(ingestSession.getExistingUnitIdWithExistingObjectGroup().get(archiveUnitGUID),
                                groupGUID);
                    }
                } else if (element.getDeclaredType().isAssignableFrom(ObjectGroupRefType.class)) {
                    groupGUID = fillObjectGroup(archiveUnitId, element);
                }
            }
        }
        return groupGUID;
    }

    private String fillDataObjectGroup(String archiveUnitId, JAXBElement<?> element) {
        DataObjectRefType dataObjectRefType = (DataObjectRefType) element.getValue();

        Map<String, String> objectGroupIdToGuid = ingestSession.getObjectGroupIdToGuid();
        Map<String, String> unitIdToGroupId = ingestSession.getUnitIdToGroupId();
        Map<String, List<String>> objectGroupIdToUnitId = ingestSession.getObjectGroupIdToUnitId();
        Map<String, GotObj> dataObjectIdWithoutObjectGroupId = ingestSession.getDataObjectIdWithoutObjectGroupId();

        if (dataObjectRefType.getDataObjectReferenceId() != null) {
            String objRefId = dataObjectRefType.getDataObjectReferenceId();
            unitIdToGroupId.put(archiveUnitId, objRefId);
            if (objectGroupIdToUnitId.get(objRefId) == null) {
                final List<String> archiveUnitList = new ArrayList<>();
                archiveUnitList.add(archiveUnitId);
                if (dataObjectIdWithoutObjectGroupId.containsKey(objRefId)) {
                    final GotObj gotObj = dataObjectIdWithoutObjectGroupId.get(objRefId);
                    final String gotId = gotObj.getGotId();
                    objectGroupIdToUnitId.put(gotId, archiveUnitList);
                    unitIdToGroupId.put(archiveUnitId, gotId); // update unitIdToGroupId with new GOT
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

    private String fillObjectGroup(String archiveUnitId, JAXBElement<?> element) {
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

            ingestSession.getUnitIdToGroupId().put(archiveUnitId, groupId);
            ingestSession.getObjectGroupIdToGuid().put(groupId, groupId);

            Map<String, List<String>> objectGroupIdToUnitId = ingestSession.getObjectGroupIdToUnitId();
            if (objectGroupIdToUnitId.get(groupId) == null) {
                final List<String> archiveUnitList = new ArrayList<>();
                archiveUnitList.add(archiveUnitId);
                if (!ingestSession.getDataObjectIdWithoutObjectGroupId().containsKey(groupId)) {
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
            Map<String, JsonNode> existingGOTs = ingestSession.getExistingGOTs();
            // prevent idempotent, if ObjectGroup have already the operation do not re-treat it
            if (null != ops && ops.toString().contains(ingestContext.getOperationId())) {
                existingGOTs.put(groupId, null);
                return groupId;

            }

            originalOGGraphData.set(MetadataDocument.OPS, ops);
            originalOGGraphData.put(MetadataDocument.OPI, ingestContext.getOperationId());
            originalOGGraphData.set(MetadataDocument.ORIGINATING_AGENCIES,
                ogInDB.get(ORIGINATING_AGENCIES.exactToken()));
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
                    IngestWorkflowConstants.UPDATE_OBJECT_GROUP_FOLDER + "/" + groupId + ".json", tmpFile, true, true);
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
        String childArchiveUnitRef_guid = ingestSession.getUnitIdToGuid().get(childArchiveUnitRef);
        if (ingestSession.getExistingUnitGuids().contains(childArchiveUnitRef_guid)) {
            throw new RuntimeException(new ProcessingManifestReferenceException(
                "The existing unit with guid [" + childArchiveUnitRef_guid + "] and manifest id [" +
                    childArchiveUnitRef + "] should not have as parent a manifest unit id [" + archiveUnitId + "] ",
                childArchiveUnitRef, childArchiveUnitRef_guid, archiveUnitId));
        }

        ObjectNode childArchiveUnitNode = (ObjectNode) ingestSession.getArchiveUnitTree().get(childArchiveUnitRef);
        if (childArchiveUnitNode == null) {
            // Create new Archive Unit Node
            childArchiveUnitNode = JsonHandler.createObjectNode();
        }

        // Reference Management during tree creation
        final ArrayNode parentsField = childArchiveUnitNode.withArray(SedaConstants.PREFIX_UP);
        parentsField.add(archiveUnitId);
        ingestSession.getArchiveUnitTree().set(childArchiveUnitRef, childArchiveUnitNode);
    }

    private void storeArchiveUnit(JsonLineDataBase unitsDatabase, String elementGuid, ArchiveUnitRoot archiveUnitRoot) {
        JsonNode jsonNode =
            VitamObjectMapper.buildSerializationObjectMapper().convertValue(archiveUnitRoot, JsonNode.class);
        unitsDatabase.write(elementGuid, jsonNode);
    }

    /**
     * Get the object group id defined in data object or the data object without GO. In this map the new technical
     * object is created
     *
     * @param objIdRefByUnit il s'agit du DataObjectGroupReferenceId
     * @return
     */
    private String getNewGdoIdFromGdoByUnit(String objIdRefByUnit) throws ProcessingManifestReferenceException {
        Map<String, GotObj> dataObjectIdWithoutObjectGroupId = ingestSession.getDataObjectIdWithoutObjectGroupId();
        final String gotGuid = dataObjectIdWithoutObjectGroupId.get(objIdRefByUnit) != null ?
            dataObjectIdWithoutObjectGroupId.get(objIdRefByUnit).getGotId() :
            null;

        Map<String, String> dataObjectIdToObjectGroupId = ingestSession.getDataObjectIdToObjectGroupId();
        if (Strings.isNullOrEmpty(dataObjectIdToObjectGroupId.get(objIdRefByUnit)) && !Strings.isNullOrEmpty(gotGuid)) {

            // nominal case of do without go
            LOGGER.debug("The data object id " + objIdRefByUnit + ", is defined without the group object id " +
                dataObjectIdWithoutObjectGroupId.get(objIdRefByUnit) + ". The technical group object guid is " +
                gotGuid);

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
            throw new ProcessingManifestReferenceException("The group id " + objIdRefByUnit +
                " doesn't reference a data object or got and it not include in data object", objIdRefByUnit,
                ExceptionType.GOT);
        }
    }

    private void createUnitLifeCycle(String unitGuid, String containerId, LogbookTypeProcess logbookTypeProcess) {
        final LogbookLifeCycleUnitParameters logbookLifecycleUnitParameters =
            (LogbookLifeCycleUnitParameters) initLogbookLifeCycleParameters(unitGuid, true, false);

        logbookLifecycleUnitParameters.setFinalStatus(LFC_INITIAL_CREATION_EVENT_TYPE, null, StatusCode.OK, null);

        logbookLifecycleUnitParameters.putParameterValue(LogbookParameterName.eventIdentifierProcess, containerId);
        logbookLifecycleUnitParameters.putParameterValue(LogbookParameterName.eventIdentifier,
            GUIDFactory.newEventGUID(ParameterHelper.getTenantParameter()).toString());
        logbookLifecycleUnitParameters.putParameterValue(LogbookParameterName.eventTypeProcess,
            logbookTypeProcess.name());

        // Update guidToLifeCycleParameters
        ingestSession.getGuidToLifeCycleParameters().put(unitGuid, logbookLifecycleUnitParameters);
    }

    private LogbookParameters initLogbookLifeCycleParameters(String guid, boolean isArchive, boolean isObjectGroup) {
        LogbookParameters logbookLifeCycleParameters = ingestSession.getGuidToLifeCycleParameters().get(guid);
        if (logbookLifeCycleParameters == null) {
            logbookLifeCycleParameters = isArchive ?
                LogbookParameterHelper.newLogbookLifeCycleUnitParameters() :
                isObjectGroup ?
                    LogbookParameterHelper.newLogbookLifeCycleObjectGroupParameters() :
                    LogbookParameterHelper.newLogbookOperationParameters();

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
                    "] are required values", archiveUnitId,
                "[MetadataName:" + metadataName + ", MetadataValue : " + metadataValue + "]", false, ExceptionType.UNIT,
                SUBTASK_EMPTY_KEY_ATTACHMENT);
        }

        final SelectMultiQuery select = new SelectMultiQuery();
        try {
            Query qr = QueryHelper.eq(metadataName, metadataValue);
            select.setQuery(qr);
        } catch (IllegalStateException e) {
            throw new ProcessingNotFoundException(
                "Unit " + archiveUnitId + ":  [MetadataName:" + metadataName + ", MetadataValue : " + metadataValue +
                    "] : " + e.getMessage(), archiveUnitId,
                "[MetadataName:" + metadataName + ", MetadataValue : " + metadataValue + "]", false, ExceptionType.UNIT,
                SUBTASK_NULL_LINK_PARENT_ID_ATTACHMENT);
        } catch (InvalidCreateOperationException e) {
            throw new ProcessingNotFoundException(
                "Unit " + archiveUnitId + ":  [MetadataName:" + metadataName + ", MetadataValue : " + metadataValue +
                    "] : " + e.getMessage(), archiveUnitId,
                "[MetadataName:" + metadataName + ", MetadataValue : " + metadataValue + "]", false, ExceptionType.UNIT,
                SUBTASK_ERROR_PARSE_ATTACHMENT);
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
            fields.put(ALLUNITUPS.exactToken(), 1);
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
}
