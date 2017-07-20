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

import static fr.gouv.vitam.common.VitamConfiguration.getDefaultLang;

import java.util.List;
import java.util.Objects;

import com.google.common.collect.Iterables;

import fr.gouv.culture.archivesdefrance.seda.v2.DescriptiveMetadataContentType;
import fr.gouv.culture.archivesdefrance.seda.v2.TextType;
import fr.gouv.vitam.worker.core.model.DescriptiveMetadataModel;
import fr.gouv.vitam.worker.core.model.TextByLang;

public class DescriptiveMetadataMapper {

    public DescriptiveMetadataModel map(DescriptiveMetadataContentType metadataContentType) {

        DescriptiveMetadataModel descriptiveMetadataModel = new DescriptiveMetadataModel();
        descriptiveMetadataModel.setAcquiredDate(metadataContentType.getAcquiredDate());
        descriptiveMetadataModel.setAddressee(metadataContentType.getAddressee());
        descriptiveMetadataModel.setAny(metadataContentType.getAny());
        descriptiveMetadataModel
            .setArchivalAgencyArchiveUnitIdentifier(metadataContentType.getArchivalAgencyArchiveUnitIdentifier());
        descriptiveMetadataModel.setAuthorizedAgent(metadataContentType.getAuthorizedAgent());
        descriptiveMetadataModel.setCoverage(metadataContentType.getCoverage());
        descriptiveMetadataModel.setCreatedDate(metadataContentType.getCreatedDate());
        descriptiveMetadataModel.setCustodialHistory(metadataContentType.getCustodialHistory());
        descriptiveMetadataModel.setDescription(findTextTypeByLang(metadataContentType.getDescription()));
        descriptiveMetadataModel.setDescriptions(new TextByLang(metadataContentType.getDescription()));
        descriptiveMetadataModel.setDescriptionLanguage(metadataContentType.getDescriptionLanguage());
        descriptiveMetadataModel.setDescriptionLevel(metadataContentType.getDescriptionLevel());
        descriptiveMetadataModel.setDocumentType(metadataContentType.getDocumentType());
        descriptiveMetadataModel.setEndDate(metadataContentType.getEndDate());
        descriptiveMetadataModel.setEvent(metadataContentType.getEvent());
        descriptiveMetadataModel.setFilePlanPosition(metadataContentType.getFilePlanPosition());
        descriptiveMetadataModel.setGps(metadataContentType.getGps());
        descriptiveMetadataModel.setHref(metadataContentType.getHref());
        descriptiveMetadataModel.setId(metadataContentType.getId());
        descriptiveMetadataModel.setKeyword(metadataContentType.getKeyword());
        descriptiveMetadataModel.setLanguage(metadataContentType.getLanguage());
        descriptiveMetadataModel.setOriginatingAgency(metadataContentType.getOriginatingAgency());
        descriptiveMetadataModel
            .setOriginatingAgencyArchiveUnitIdentifier(metadataContentType.getOriginatingAgencyArchiveUnitIdentifier());
        descriptiveMetadataModel.setOriginatingSystemId(metadataContentType.getOriginatingSystemId());
        descriptiveMetadataModel.setReceivedDate(metadataContentType.getReceivedDate());
        descriptiveMetadataModel.setRecipient(metadataContentType.getRecipient());
        descriptiveMetadataModel.setRegisteredDate(metadataContentType.getRegisteredDate());
        descriptiveMetadataModel.setRelatedObjectReference(metadataContentType.getRelatedObjectReference());
        descriptiveMetadataModel.setRestrictionEndDate(metadataContentType.getRestrictionEndDate());
        descriptiveMetadataModel.setRestrictionRuleIdRef(metadataContentType.getRestrictionRuleIdRef());
        descriptiveMetadataModel.setRestrictionValue(metadataContentType.getRestrictionValue());
        descriptiveMetadataModel.setRegisteredDate(metadataContentType.getReceivedDate());
        descriptiveMetadataModel.setSentDate(metadataContentType.getSentDate());
        descriptiveMetadataModel.setSignature(metadataContentType.getSignature());
        descriptiveMetadataModel.setSource(metadataContentType.getSource());
        descriptiveMetadataModel.setStartDate(metadataContentType.getStartDate());
        descriptiveMetadataModel.setStatus(metadataContentType.getStatus());
        descriptiveMetadataModel.setSubmissionAgency(metadataContentType.getSubmissionAgency());
        descriptiveMetadataModel.setSystemId(metadataContentType.getSystemId());
        descriptiveMetadataModel.setTag(metadataContentType.getTag());
        descriptiveMetadataModel.setTitle(findTextTypeByLang(metadataContentType.getTitle()));
        descriptiveMetadataModel.setTitles(new TextByLang(metadataContentType.getTitle()));
        descriptiveMetadataModel.setTransactedDate(metadataContentType.getTransactedDate());
        descriptiveMetadataModel.setTransferringAgencyArchiveUnitIdentifier(
            metadataContentType.getTransferringAgencyArchiveUnitIdentifier());
        descriptiveMetadataModel.setType(metadataContentType.getType());
        descriptiveMetadataModel.setVersion(metadataContentType.getVersion());
        descriptiveMetadataModel.setWriter(metadataContentType.getWriter());

        return descriptiveMetadataModel;
    }

    public String findTextTypeByLang(List<TextType> textTypes) {
        if (Iterables.isEmpty(textTypes)) {
            return null;
        }
        for (TextType textType : textTypes) {
            if (Objects.isNull(textType.getLang())) {
                return textType.getValue();
            }
            if (getDefaultLang().equals(textType.getLang().toLowerCase())) {
                return textType.getValue();
            }
        }
        return Iterables.getLast(textTypes).getValue();
    }

}
