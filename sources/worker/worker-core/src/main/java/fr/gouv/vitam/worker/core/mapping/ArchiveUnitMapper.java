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
package fr.gouv.vitam.worker.core.mapping;

import com.google.common.collect.Iterables;
import fr.gouv.culture.archivesdefrance.seda.v2.AccessRuleType;
import fr.gouv.culture.archivesdefrance.seda.v2.AppraisalRuleType;
import fr.gouv.culture.archivesdefrance.seda.v2.ArchiveUnitType;
import fr.gouv.culture.archivesdefrance.seda.v2.ClassificationRuleType;
import fr.gouv.culture.archivesdefrance.seda.v2.DataObjectRefType;
import fr.gouv.culture.archivesdefrance.seda.v2.DescriptiveMetadataContentType;
import fr.gouv.culture.archivesdefrance.seda.v2.DisseminationRuleType;
import fr.gouv.culture.archivesdefrance.seda.v2.EventType;
import fr.gouv.culture.archivesdefrance.seda.v2.FinalActionAppraisalCodeType;
import fr.gouv.culture.archivesdefrance.seda.v2.FinalActionStorageCodeType;
import fr.gouv.culture.archivesdefrance.seda.v2.HoldRuleType;
import fr.gouv.culture.archivesdefrance.seda.v2.IdentifierType;
import fr.gouv.culture.archivesdefrance.seda.v2.ManagementHistoryType;
import fr.gouv.culture.archivesdefrance.seda.v2.ManagementType;
import fr.gouv.culture.archivesdefrance.seda.v2.ReuseRuleType;
import fr.gouv.culture.archivesdefrance.seda.v2.StorageRuleType;
import fr.gouv.vitam.common.model.logbook.LogbookEvent;
import fr.gouv.vitam.common.model.unit.ArchiveUnitHistoryModel;
import fr.gouv.vitam.common.model.unit.ArchiveUnitInternalModel;
import fr.gouv.vitam.common.model.unit.ArchiveUnitRoot;
import fr.gouv.vitam.common.model.unit.DataObjectReference;
import fr.gouv.vitam.common.model.unit.DescriptiveMetadataModel;
import fr.gouv.vitam.common.model.unit.ManagementModel;
import fr.gouv.vitam.common.model.unit.RuleCategoryModel;
import fr.gouv.vitam.processing.common.exception.ProcessingMalformedDataException;
import fr.gouv.vitam.processing.common.exception.ProcessingObjectReferenceException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.bind.JAXBElement;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * map archive unit to model
 */
public class ArchiveUnitMapper {

    private DescriptiveMetadataMapper descriptiveMetadataMapper;
    private RuleMapper ruleMapper;

    public ArchiveUnitMapper(DescriptiveMetadataMapper descriptiveMetadataMapper, RuleMapper ruleMapper) {
        this.descriptiveMetadataMapper = descriptiveMetadataMapper;
        this.ruleMapper = ruleMapper;
    }

    /**
     * mapping
     *
     * @param archiveUnitType
     * @param id
     * @param groupId
     * @param operationId
     * @param unitType
     * @param sedaVersion
     * @return ArchiveUnitRoot
     */
    public ArchiveUnitRoot map(
        ArchiveUnitType archiveUnitType,
        String id,
        String groupId,
        String operationId,
        String unitType,
        String sedaVersion
    ) throws ProcessingMalformedDataException, ProcessingObjectReferenceException {
        ArchiveUnitRoot archiveUnitRoot = new ArchiveUnitRoot();
        ArchiveUnitInternalModel archiveUnit = archiveUnitRoot.getArchiveUnit();
        archiveUnit.setId(id);
        archiveUnit.setOg(groupId);

        archiveUnit.setOpi(operationId);
        archiveUnit.setOps(Collections.singletonList(operationId));

        archiveUnit.setUnitType(unitType);

        IdentifierType archiveUnitProfile = archiveUnitType.getArchiveUnitProfile();
        if (archiveUnitProfile != null) {
            archiveUnit.setArchiveUnitProfile(archiveUnitProfile.getValue());
        }
        DescriptiveMetadataContentType metadataContentType = archiveUnitType.getContent();
        if (metadataContentType != null) {
            DescriptiveMetadataModel metadataModel = descriptiveMetadataMapper.map(metadataContentType);
            archiveUnit.setDescriptiveMetadataModel(metadataModel);
        } else {
            archiveUnit.setDescriptiveMetadataModel(new DescriptiveMetadataModel());
        }

        mapAndValidateDataObjectReference(archiveUnitType);

        ManagementType management = archiveUnitType.getManagement();
        fillManagement(management, archiveUnit.getManagement());
        if (archiveUnitType.getContent() != null && archiveUnitType.getContent().getHistory() != null) {
            fillHistory(archiveUnitType.getContent().getHistory(), archiveUnit.getHistory());
        }

        if (management != null && management.getLogBook() != null) {
            List<LogbookEvent> logbookExternal = management
                .getLogBook()
                .getEvent()
                .stream()
                .map(this::toLogbookEvent)
                .collect(Collectors.toList());

            archiveUnitRoot.setLogbookLifeCycleExternal(logbookExternal);
        }
        archiveUnit.setSedaVersion(sedaVersion);
        String vitamImplVersion = this.getClass().getPackage().getImplementationVersion();
        if (vitamImplVersion != null) {
            archiveUnit.setImplementationVersion(vitamImplVersion);
        } else {
            archiveUnit.setImplementationVersion("");
        }

        return archiveUnitRoot;
    }

    private LogbookEvent toLogbookEvent(EventType eventType) {
        LogbookEvent logbookEvent = new LogbookEvent();

        logbookEvent.setEvId(eventType.getEventIdentifier());
        logbookEvent.setEvTypeProc(eventType.getEventTypeCode());
        logbookEvent.setEvType(eventType.getEventType());
        logbookEvent.setEvDateTime(eventType.getEventDateTime());
        logbookEvent.setOutcome(eventType.getOutcome());
        logbookEvent.setOutDetail(eventType.getOutcomeDetail());
        logbookEvent.setOutMessg(eventType.getOutcomeDetailMessage());
        logbookEvent.setEvDetData(eventType.getEventDetailData());
        logbookEvent.setAgId(getElementAsText(eventType, "AgentIdentifier"));
        logbookEvent.setObId(getElementAsText(eventType, "ObjectIdentifier"));

        return logbookEvent;
    }

    private String getElementAsText(EventType eventType, String name) {
        return eventType
            .getAny()
            .stream()
            .map(object -> (Element) object)
            .filter(element -> element.getLocalName().equals(name))
            .findFirst()
            .map(Node::getFirstChild)
            .map(Node::getTextContent)
            .orElse(null);
    }

    public DataObjectReference mapAndValidateDataObjectReference(ArchiveUnitType archiveUnitType)
        throws ProcessingObjectReferenceException {
        List<DataObjectReference> objectReferences = archiveUnitType
            .getArchiveUnitOrDataObjectReferenceOrDataObjectGroup()
            .stream()
            .filter(item -> item instanceof JAXBElement)
            .filter(item -> ((JAXBElement) item).getDeclaredType().equals(DataObjectRefType.class))
            .map(item -> (JAXBElement) item)
            .map(JAXBElement::getValue)
            .map(item -> (DataObjectRefType) item)
            .map(item -> {
                DataObjectReference dataObjectReference = new DataObjectReference();
                dataObjectReference.setDataObjectGroupReferenceId(item.getDataObjectGroupReferenceId());

                return dataObjectReference;
            })
            .collect(Collectors.toList());

        if (objectReferences.size() > 1) {
            throw new ProcessingObjectReferenceException(
                "archive unit '" + archiveUnitType.getId() + "' references more than one technical object group"
            );
        }

        return Iterables.getOnlyElement(objectReferences, null);
    }

    private void fillManagement(ManagementType managementType, ManagementModel managementModel)
        throws ProcessingMalformedDataException {
        if (managementType != null) {
            managementModel.setUpdateOperationType(managementType.getUpdateOperation());
            managementModel.setNeedAuthorization(managementType.isNeedAuthorization());
            fillAccessRule(managementType, managementModel);
            fillStorageRule(managementType, managementModel);
            fillClassificationRule(managementType, managementModel);
            fillAppraisalRule(managementType, managementModel);
            fillDisseminationRule(managementType, managementModel);
            fillReuseRule(managementType, managementModel);
            fillHoldRule(managementType, managementModel);
        }
    }

    private void fillAccessRule(ManagementType managementType, ManagementModel managementModel) {
        AccessRuleType accessRule = managementType.getAccessRule();
        RuleCategoryModel accessRuleCategory = ruleMapper.fillCommonRule(accessRule);
        if (managementModel.getAccess() != null) {
            managementModel.getAccess().merge(accessRuleCategory);
        } else {
            managementModel.setAccess(accessRuleCategory);
        }
    }

    private void fillStorageRule(ManagementType managementType, ManagementModel managementModel)
        throws ProcessingMalformedDataException {
        StorageRuleType storageRule = managementType.getStorageRule();
        RuleCategoryModel storageRuleCategory = ruleMapper.fillCommonRule(storageRule);
        if (storageRule != null && storageRule.getFinalAction() != null && storageRuleCategory == null) {
            // that means we only have FinalAction set in the rule
            storageRuleCategory = new RuleCategoryModel();
        }

        if (managementModel.getStorage() != null) {
            managementModel.getStorage().merge(storageRuleCategory);
        } else {
            managementModel.setStorage(storageRuleCategory);
        }

        if (storageRuleCategory != null && storageRule != null) {
            FinalActionStorageCodeType sfa = storageRule.getFinalAction();
            if (sfa != null) {
                storageRuleCategory.setFinalAction(sfa.value());
            } else {
                throw new ProcessingMalformedDataException("FinalAction is required for StorageRule");
            }
        }
    }

    private void fillClassificationRule(ManagementType managementType, ManagementModel managementModel) {
        ClassificationRuleType classificationRule = managementType.getClassificationRule();
        RuleCategoryModel classificationRuleCategory = ruleMapper.fillCommonRule(classificationRule);

        if (classificationRule != null) {
            if (classificationRuleCategory == null) {
                classificationRuleCategory = new RuleCategoryModel();
            }

            if (classificationRule.getClassificationLevel() != null) {
                classificationRuleCategory.setClassificationLevel(classificationRule.getClassificationLevel());
            }

            if (classificationRule.getClassificationOwner() != null) {
                classificationRuleCategory.setClassificationOwner(classificationRule.getClassificationOwner());
            }

            if (classificationRule.getClassificationAudience() != null) {
                classificationRuleCategory.setClassificationAudience(classificationRule.getClassificationAudience());
            }

            if (classificationRule.getClassificationReassessingDate() != null) {
                classificationRuleCategory.setClassificationReassessingDate(
                    classificationRule.getClassificationReassessingDate().toString()
                );
            }

            if (classificationRule.isNeedReassessingAuthorization() != null) {
                classificationRuleCategory.setNeedReassessingAuthorization(
                    classificationRule.isNeedReassessingAuthorization()
                );
            }
        }

        if (managementModel.getClassification() != null) {
            managementModel.getClassification().merge(classificationRuleCategory);
        } else {
            managementModel.setClassification(classificationRuleCategory);
        }
    }

    private void fillReuseRule(ManagementType managementType, ManagementModel managementModel) {
        ReuseRuleType reuseRule = managementType.getReuseRule();
        RuleCategoryModel reuseRuleCategory = ruleMapper.fillCommonRule(reuseRule);
        if (managementModel.getReuse() != null) {
            managementModel.getReuse().merge(reuseRuleCategory);
        } else {
            managementModel.setReuse(reuseRuleCategory);
        }
    }

    private void fillDisseminationRule(ManagementType managementType, ManagementModel managementModel) {
        DisseminationRuleType disseminationRule = managementType.getDisseminationRule();
        RuleCategoryModel disseminationRuleCategory = ruleMapper.fillCommonRule(disseminationRule);
        if (managementModel.getDissemination() != null) {
            managementModel.getDissemination().merge(disseminationRuleCategory);
        } else {
            managementModel.setDissemination(disseminationRuleCategory);
        }
    }

    private void fillAppraisalRule(ManagementType managementType, ManagementModel managementModel)
        throws ProcessingMalformedDataException {
        AppraisalRuleType appraisalRule = managementType.getAppraisalRule();
        RuleCategoryModel appraisalRuleCategory = ruleMapper.fillCommonRule(appraisalRule);

        if (appraisalRule != null && appraisalRule.getFinalAction() != null && appraisalRuleCategory == null) {
            // that means we only have FinalAction set in the rule
            appraisalRuleCategory = new RuleCategoryModel();
        }

        if (managementModel.getAppraisal() != null) {
            managementModel.getAppraisal().merge(appraisalRuleCategory);
        } else {
            managementModel.setAppraisal(appraisalRuleCategory);
        }
        if (appraisalRuleCategory != null && appraisalRule != null) {
            FinalActionAppraisalCodeType afa = appraisalRule.getFinalAction();
            if (afa != null) {
                appraisalRuleCategory.setFinalAction(afa.value());
            } else {
                throw new ProcessingMalformedDataException("FinalAction is required for AppraisalRule");
            }
        }
    }

    private void fillHoldRule(ManagementType managementType, ManagementModel managementModel) {
        HoldRuleType holdRule = managementType.getHoldRule();
        RuleCategoryModel holdRuleCategory = ruleMapper.fillHoldRule(holdRule);
        if (managementModel.getHold() != null) {
            managementModel.getHold().merge(holdRuleCategory);
        } else {
            managementModel.setHold(holdRuleCategory);
        }
    }

    private void fillHistory(
        List<ManagementHistoryType> managementHistoryType,
        List<ArchiveUnitHistoryModel> archiveUnitHistoryModel
    ) throws ProcessingMalformedDataException {
        if (managementHistoryType == null) {
            return;
        }

        for (ManagementHistoryType historyType : managementHistoryType) {
            ArchiveUnitHistoryModel historyModel = new ArchiveUnitHistoryModel();
            historyModel.setUpdateDate(historyType.getUpdateDate().toString());
            historyModel.getData().setVersion(historyType.getData().getVersion());
            fillManagement(historyType.getData().getManagement(), historyModel.getData().getManagement());

            archiveUnitHistoryModel.add(historyModel);
        }
    }
}
