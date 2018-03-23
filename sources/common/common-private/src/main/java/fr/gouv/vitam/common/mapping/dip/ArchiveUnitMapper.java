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
package fr.gouv.vitam.common.mapping.dip;

import fr.gouv.culture.archivesdefrance.seda.v2.AccessRuleType;
import fr.gouv.culture.archivesdefrance.seda.v2.AppraisalRuleType;
import fr.gouv.culture.archivesdefrance.seda.v2.ArchiveUnitType;
import fr.gouv.culture.archivesdefrance.seda.v2.ClassificationRuleType;
import fr.gouv.culture.archivesdefrance.seda.v2.DisseminationRuleType;
import fr.gouv.culture.archivesdefrance.seda.v2.IdentifierType;
import fr.gouv.culture.archivesdefrance.seda.v2.ReuseRuleType;
import fr.gouv.culture.archivesdefrance.seda.v2.StorageRuleType;
import fr.gouv.vitam.common.mapping.dip.DescriptiveMetadataMapper;
import fr.gouv.vitam.common.mapping.dip.RuleMapper;
import fr.gouv.vitam.common.model.unit.ArchiveUnitModel;

import javax.xml.datatype.DatatypeConfigurationException;

/**
 * Map the object ArchiveUnitModel generated from Unit data base model
 * To a jaxb object ArchiveUnitType
 * This help convert ArchiveUnitModel to xml using jaxb
 */
public class ArchiveUnitMapper {

    private DescriptiveMetadataMapper descriptiveMetadataMapper;

    private RuleMapper ruleMapper;

    public ArchiveUnitMapper() {
        this.descriptiveMetadataMapper = new DescriptiveMetadataMapper();
        this.ruleMapper = new RuleMapper();
    }

    public ArchiveUnitType map(ArchiveUnitModel model) throws DatatypeConfigurationException {

        ArchiveUnitType archiveUnitType = new ArchiveUnitType();
        archiveUnitType.setId(model.getId());

        if (model.getArchiveUnitProfile() != null) {
            IdentifierType identifierType = new IdentifierType();
            identifierType.setValue(model.getArchiveUnitProfile());
            archiveUnitType.setArchiveUnitProfile(identifierType);
        }

        archiveUnitType.setContent(descriptiveMetadataMapper.map(model.getDescriptiveMetadataModel()));
        ArchiveUnitType.Management management = new ArchiveUnitType.Management();

        management.setAccessRule(ruleMapper.fillCommonRule(model.getManagement().getAccess(), AccessRuleType::new));
        management
            .setAppraisalRule(ruleMapper.fillCommonRule(model.getManagement().getAppraisal(), AppraisalRuleType::new));
        management.setClassificationRule(
            ruleMapper.fillCommonRule(model.getManagement().getClassification(), ClassificationRuleType::new));
        management.setDisseminationRule(
            ruleMapper.fillCommonRule(model.getManagement().getDissemination(), DisseminationRuleType::new));
        management.setReuseRule(ruleMapper.fillCommonRule(model.getManagement().getReuse(), ReuseRuleType::new));
        management.setStorageRule(ruleMapper.fillCommonRule(model.getManagement().getStorage(), StorageRuleType::new));

        archiveUnitType.setManagement(management);

        return archiveUnitType;
    }

}
