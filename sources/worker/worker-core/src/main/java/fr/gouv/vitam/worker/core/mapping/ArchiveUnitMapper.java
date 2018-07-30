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
package fr.gouv.vitam.worker.core.mapping;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;

import com.google.common.collect.Iterables;
import fr.gouv.culture.archivesdefrance.seda.v2.AccessRuleType;
import fr.gouv.culture.archivesdefrance.seda.v2.AppraisalRuleType;
import fr.gouv.culture.archivesdefrance.seda.v2.ArchiveUnitType;
import fr.gouv.culture.archivesdefrance.seda.v2.ClassificationRuleType;
import fr.gouv.culture.archivesdefrance.seda.v2.DataObjectRefType;
import fr.gouv.culture.archivesdefrance.seda.v2.DescriptiveMetadataContentType;
import fr.gouv.culture.archivesdefrance.seda.v2.DisseminationRuleType;
import fr.gouv.culture.archivesdefrance.seda.v2.FinalActionAppraisalCodeType;
import fr.gouv.culture.archivesdefrance.seda.v2.FinalActionStorageCodeType;
import fr.gouv.culture.archivesdefrance.seda.v2.IdentifierType;
import fr.gouv.culture.archivesdefrance.seda.v2.ReuseRuleType;
import fr.gouv.culture.archivesdefrance.seda.v2.StorageRuleType;
import fr.gouv.vitam.common.model.unit.ArchiveUnitModel;
import fr.gouv.vitam.common.model.unit.ArchiveUnitRoot;
import fr.gouv.vitam.common.model.unit.DataObjectReference;
import fr.gouv.vitam.common.model.unit.DescriptiveMetadataModel;
import fr.gouv.vitam.common.model.unit.RuleCategoryModel;
import fr.gouv.vitam.common.model.unit.RuleModel;
import fr.gouv.vitam.processing.common.exception.ProcessingMalformedDataException;

/**
 * map archive unit to model
 */
public class ArchiveUnitMapper {

    private DescriptiveMetadataMapper descriptiveMetadataMapper;
    private RuleMapper ruleMapper;

    public ArchiveUnitMapper(DescriptiveMetadataMapper descriptiveMetadataMapper,
        RuleMapper ruleMapper) {
        this.descriptiveMetadataMapper = descriptiveMetadataMapper;
        this.ruleMapper = ruleMapper;
    }

    /**
     * mapping
     *
     * @param archiveUnitType
     * @param id
     * @param groupId
     * @return ArchiveUnitRoot
     */
    public ArchiveUnitRoot map(ArchiveUnitType archiveUnitType, String id, String groupId)
        throws DatatypeConfigurationException, ProcessingMalformedDataException {

        ArchiveUnitRoot archiveUnitRoot = new ArchiveUnitRoot();
        ArchiveUnitModel archiveUnit = archiveUnitRoot.getArchiveUnit();
        archiveUnit.setId(id);
        archiveUnit.setOg(groupId);

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

        archiveUnit.setDataObjectReference(mapDataObjectReference(archiveUnitType));

        if (archiveUnitType.getManagement() != null) {
            archiveUnit.getManagement().setUpdateOperationType(archiveUnitType.getManagement().getUpdateOperation());
            archiveUnit.getManagement().setNeedAuthorization(archiveUnitType.getManagement().isNeedAuthorization());
            fillAccessRule(archiveUnitType, archiveUnit);
            fillStorageRule(archiveUnitType, archiveUnit);
            fillClassificationRule(archiveUnitType, archiveUnit);
            fillAppraisalRule(archiveUnitType, archiveUnit);
            fillDisseminationRule(archiveUnitType, archiveUnit);
            fillReuseRule(archiveUnitType, archiveUnit);
        }
        return archiveUnitRoot;
    }

    public DataObjectReference mapDataObjectReference(ArchiveUnitType archiveUnitType) {
        List<DataObjectReference> objectReferences =
            archiveUnitType.getArchiveUnitOrDataObjectReferenceOrDataObjectGroup().stream()
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

        return Iterables.getOnlyElement(objectReferences, null);
    }

    private void fillAccessRule(ArchiveUnitType archiveUnitType, ArchiveUnitModel archiveUnit) {
        AccessRuleType accessRule = archiveUnitType.getManagement().getAccessRule();
        RuleCategoryModel accessRuleCategory = ruleMapper.fillCommonRule(accessRule);
        if (archiveUnit.getManagement().getAccess() != null) {
            archiveUnit.getManagement().getAccess().merge(accessRuleCategory);
        } else {
            archiveUnit.getManagement().setAccess(accessRuleCategory);
        }
    }

    private void fillStorageRule(ArchiveUnitType archiveUnitType, ArchiveUnitModel archiveUnit)
        throws ProcessingMalformedDataException {
        StorageRuleType storageRule = archiveUnitType.getManagement().getStorageRule();
        RuleCategoryModel storageRuleCategory = ruleMapper.fillCommonRule(storageRule);
        if (storageRule != null && storageRule.getFinalAction() != null && storageRuleCategory == null) {
            // that means we only have FinalAction set in the rule
            storageRuleCategory = new RuleCategoryModel();
            List<RuleModel> rules = new ArrayList<>();
            RuleModel newRule = new RuleModel();
            rules.add(newRule);
            storageRuleCategory.getRules().addAll(rules);
        }

        if (archiveUnit.getManagement().getStorage() != null) {
            archiveUnit.getManagement().getStorage().merge(storageRuleCategory);
        } else {
            archiveUnit.getManagement().setStorage(storageRuleCategory);
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

    private void fillClassificationRule(ArchiveUnitType archiveUnitType, ArchiveUnitModel archiveUnit) {
        ClassificationRuleType classificationRule = archiveUnitType.getManagement().getClassificationRule();
        RuleCategoryModel classificationRuleCategory = ruleMapper.fillCommonRule(classificationRule);

        if (classificationRule != null) {

            if (classificationRuleCategory == null ) {
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
                    classificationRule.getClassificationReassessingDate().toString());
            }

            if (classificationRule.isNeedReassessingAuthorization() != null) {
                classificationRuleCategory.setNeedReassessingAuthorization(classificationRule.isNeedReassessingAuthorization());
            }

        }

        if (archiveUnit.getManagement().getClassification() != null) {
            archiveUnit.getManagement().getClassification().merge(classificationRuleCategory);
        } else {
            archiveUnit.getManagement().setClassification(classificationRuleCategory);
        }

    }

    private void fillReuseRule(ArchiveUnitType archiveUnitType, ArchiveUnitModel archiveUnit) {
        ReuseRuleType reuseRule = archiveUnitType.getManagement().getReuseRule();
        RuleCategoryModel reuseRuleCategory = ruleMapper.fillCommonRule(reuseRule);
        if (archiveUnit.getManagement().getReuse() != null) {
            archiveUnit.getManagement().getReuse().merge(reuseRuleCategory);
        } else {
            archiveUnit.getManagement().setReuse(reuseRuleCategory);
        }
    }

    private void fillDisseminationRule(ArchiveUnitType archiveUnitType, ArchiveUnitModel archiveUnit) {
        DisseminationRuleType disseminationRule = archiveUnitType.getManagement().getDisseminationRule();
        RuleCategoryModel disseminationRuleCategory = ruleMapper.fillCommonRule(disseminationRule);
        if (archiveUnit.getManagement().getDissemination() != null) {
            archiveUnit.getManagement().getDissemination().merge(disseminationRuleCategory);
        } else {
            archiveUnit.getManagement().setDissemination(disseminationRuleCategory);
        }
    }

    private void fillAppraisalRule(ArchiveUnitType archiveUnitType, ArchiveUnitModel archiveUnit)
        throws ProcessingMalformedDataException {
        AppraisalRuleType appraisalRule = archiveUnitType.getManagement().getAppraisalRule();
        RuleCategoryModel appraisalRuleCategory = ruleMapper.fillCommonRule(appraisalRule);

        if (appraisalRule != null && appraisalRule.getFinalAction() != null && appraisalRuleCategory == null) {
            // that means we only have FinalAction set in the rule
            appraisalRuleCategory = new RuleCategoryModel();
        }

        if (archiveUnit.getManagement().getAppraisal() != null) {
            archiveUnit.getManagement().getAppraisal().merge(appraisalRuleCategory);
        } else {
            archiveUnit.getManagement().setAppraisal(appraisalRuleCategory);
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


}
