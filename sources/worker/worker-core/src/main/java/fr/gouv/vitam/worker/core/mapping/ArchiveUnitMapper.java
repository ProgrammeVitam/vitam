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

import com.google.common.collect.Iterables;

import fr.gouv.culture.archivesdefrance.seda.v2.*;
import fr.gouv.vitam.worker.common.CommonRule;
import fr.gouv.vitam.worker.core.model.ArchiveUnitModel;
import fr.gouv.vitam.worker.core.model.ArchiveUnitRoot;
import fr.gouv.vitam.worker.core.model.DataObjectReference;
import fr.gouv.vitam.worker.core.model.DescriptiveMetadataModel;
import fr.gouv.vitam.worker.core.model.RuleModel;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.XMLGregorianCalendar;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * map archive unit to model
 */
public class ArchiveUnitMapper {

    private DescriptiveMetadataMapper descriptiveMetadataMapper;

    public ArchiveUnitMapper(DescriptiveMetadataMapper descriptiveMetadataMapper) {
        this.descriptiveMetadataMapper = descriptiveMetadataMapper;
    }

    /**
     * mapping
     *
     * @param archiveUnitType
     * @param id
     * @param groupId
     * @return ArchiveUnitRoot
     */
    public ArchiveUnitRoot map(ArchiveUnitType archiveUnitType, String id, String groupId) {

        ArchiveUnitRoot archiveUnitRoot = new ArchiveUnitRoot();
        ArchiveUnitModel archiveUnit = archiveUnitRoot.getArchiveUnit();
        archiveUnit.setId(id);
        archiveUnit.setOg(groupId);

        IdentifierType archiveUnitProfile = archiveUnitType.getArchiveUnitProfile();
        if (archiveUnitProfile != null) {
            archiveUnit.setArchiveUnitProfile(archiveUnitProfile.getValue());
        }
        DescriptiveMetadataContentType metadataContentType = Iterables.getLast(archiveUnitType.getContent(), null);
        if (metadataContentType != null) {
            DescriptiveMetadataModel metadataModel = descriptiveMetadataMapper.map(metadataContentType);
            archiveUnit.setDescriptiveMetadataModel(metadataModel);
            archiveUnit.getDescriptiveMetadataModel().setId(null);
        } else {
            archiveUnit.setDescriptiveMetadataModel(new DescriptiveMetadataModel());
        }

        archiveUnit.setDataObjectReference(mapDataObjectReference(archiveUnitType));

        if (archiveUnitType.getManagement() != null) {
            archiveUnit.getManagement().setUpdateOperationType(archiveUnitType.getManagement().getUpdateOperation());
            fillAccessRule(archiveUnitType, archiveUnit);
            fillStorageRule(archiveUnitType, archiveUnit);
            fillClassificationRule(archiveUnitType, archiveUnit);
            fillAppraisalRule(archiveUnitType, archiveUnit);
            fillDisseminationRule(archiveUnitType, archiveUnit);
            fillReuseRule(archiveUnitType, archiveUnit);
        }
        return archiveUnitRoot;
    }

    private DataObjectReference mapDataObjectReference(ArchiveUnitType archiveUnitType) {
        List<DataObjectReference> objectReferences = archiveUnitType.getArchiveUnitOrDataObjectReferenceOrDataObjectGroup().stream()
            .filter(item -> item instanceof JAXBElement)
            .filter(item -> ((JAXBElement) item).getDeclaredType().equals(DataObjectRefType.class))
            .map(item -> ((JAXBElement) item))
            .map(JAXBElement::getValue)
            .map(item -> ((DataObjectRefType) item))
            .map(item -> {
                DataObjectReference dataObjectReference = new DataObjectReference();
                dataObjectReference.setDataObjectGroupReferenceId(item.getDataObjectGroupReferenceId());
                return dataObjectReference;
            })
            .collect(Collectors.toList());

        return Iterables.getOnlyElement(objectReferences, null);
    }

    private void fillAccessRule(ArchiveUnitType archiveUnitType, ArchiveUnitModel archiveUnit) {
        List<RuleModel> rules = fillCommonRule(archiveUnitType.getManagement().getAccessRule());
        archiveUnit.getManagement().getAccessRule().addAll(rules);
    }

    private void fillStorageRule(ArchiveUnitType archiveUnitType, ArchiveUnitModel archiveUnit) {
        StorageRuleType storageRule = archiveUnitType.getManagement().getStorageRule();
        List<RuleModel> rules = fillCommonRule(storageRule);
        archiveUnit.getManagement().getStorageRule().addAll(rules);

        if (rules.size() > 0) {
            RuleModel lastRule = Iterables.getLast(archiveUnit.getManagement().getStorageRule());
            if (storageRule.getFinalAction() != null) {
                lastRule.setFinalAction(storageRule.getFinalAction().value());
            }
        }
    }

    private void fillClassificationRule(ArchiveUnitType archiveUnitType, ArchiveUnitModel archiveUnit) {
        ClassificationRuleType classificationRule = archiveUnitType.getManagement().getClassificationRule();
        List<RuleModel> rules = fillCommonRule(classificationRule);
        archiveUnit.getManagement().getClassificationRule().addAll(rules);

        if (rules.size() > 0) {
            RuleModel lastRule = Iterables.getLast(rules);
            lastRule.setClassificationLevel(classificationRule.getClassificationLevel());
            lastRule.setClassificationOwner(classificationRule.getClassificationOwner());
            if (classificationRule.getClassificationReassessingDate() != null) {
                lastRule
                    .setClassificationReassessingDate(classificationRule.getClassificationReassessingDate().toString());
            }
            lastRule.setNeedReassessingAuthorization(classificationRule.isNeedReassessingAuthorization());
        }
    }

    private void fillReuseRule(ArchiveUnitType archiveUnitType, ArchiveUnitModel archiveUnit) {
        List<RuleModel> rules = fillCommonRule(archiveUnitType.getManagement().getReuseRule());
        archiveUnit.getManagement().getReuseRule().addAll(rules);
    }

    private void fillDisseminationRule(ArchiveUnitType archiveUnitType, ArchiveUnitModel archiveUnit) {
        List<RuleModel> rules = fillCommonRule(archiveUnitType.getManagement().getDisseminationRule());
        archiveUnit.getManagement().getDisseminationRule().addAll(rules);
    }

    private List<RuleModel> fillCommonRule(CommonRule rule) {
        RuleModel ruleModel = null;

        List<RuleModel> rules = new ArrayList<>();

        if (rule == null) {
            return new ArrayList<>();
        }

        for (Object ruleOrStartDate : rule.getRuleAndStartDate()) {
            if (ruleOrStartDate instanceof RuleIdType) {
                ruleModel = new RuleModel();
                rules.add(ruleModel);
                String ruleId = ((RuleIdType) ruleOrStartDate).getValue();
                ruleModel.setRule(ruleId);
            }
            if (ruleOrStartDate instanceof XMLGregorianCalendar) {
                XMLGregorianCalendar startDate = (XMLGregorianCalendar) ruleOrStartDate;
                ruleModel.setStartDate(startDate.toString());
            }
        }

        if (rule.isPreventInheritance() != null || rule.getRefNonRuleId().size() > 0) {
            RuleModel lastRule = Iterables.getLast(rules, new RuleModel());
            lastRule.setPreventInheritance(rule.isPreventInheritance());
            List<String> refNonRuleId =
                rule.getRefNonRuleId().stream().map(RuleIdType::getValue).collect(Collectors.toList());
            lastRule.setRefNonRuleId(refNonRuleId);

            if (!rules.contains(lastRule)) {
                rules.add(lastRule);
            }
        }

        return rules;
    }

    private void fillAppraisalRule(ArchiveUnitType archiveUnitType, ArchiveUnitModel archiveUnit) {
        AppraisalRuleType appraisalRule = archiveUnitType.getManagement().getAppraisalRule();
        List<RuleModel> rules = fillCommonRule(appraisalRule);
        archiveUnit.getManagement().getAppraisalRule().addAll(rules);

        if (rules.size() > 0) {
            RuleModel lastRule = Iterables.getLast(rules);
            lastRule.setFinalAction(appraisalRule.getFinalAction().value());
        }
    }

}
