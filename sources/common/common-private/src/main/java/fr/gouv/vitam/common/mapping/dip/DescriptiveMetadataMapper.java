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

import fr.gouv.culture.archivesdefrance.seda.v2.CustodialHistoryType;
import fr.gouv.culture.archivesdefrance.seda.v2.DescriptiveMetadataContentType;
import fr.gouv.culture.archivesdefrance.seda.v2.TextType;
import fr.gouv.vitam.common.model.unit.DescriptiveMetadataModel;

import java.util.Collections;

/**
 * Map the object DescriptiveMetadataModel generated from Unit data base model To a jaxb object
 * DescriptiveMetadataContentType This help convert DescriptiveMetadataModel to xml using jaxb
 */
public class DescriptiveMetadataMapper {

    private CustodialHistoryMapper custodialHistoryMapper = new CustodialHistoryMapper();

    /**
     * Map local DescriptiveMetadataModel to jaxb DescriptiveMetadataContentType
     *
     * @param metadataModel
     * @return a descriptive Metadata Content Type
     */
    public DescriptiveMetadataContentType map(DescriptiveMetadataModel metadataModel) {

        DescriptiveMetadataContentType dmc = new DescriptiveMetadataContentType();
        dmc.setAcquiredDate(metadataModel.getAcquiredDate());

        dmc.getAddressee().addAll(metadataModel.getAddressee());
        dmc.getAny().addAll(
            TransformJsonTreeToListOfXmlElement.mapJsonToElement(Collections.singletonList(metadataModel.getAny())));

        dmc.setCoverage(metadataModel.getCoverage());
        dmc.setCreatedDate(metadataModel.getCreatedDate());

        CustodialHistoryType custodialHistory = custodialHistoryMapper.map(metadataModel.getCustodialHistory());
        dmc.setCustodialHistory(custodialHistory);

        if (metadataModel.getDescription_() != null) {
            dmc.getDescription().addAll(metadataModel.getDescription_().getTextTypes());
        }

        TextType description = new TextType();
        description.setValue(metadataModel.getDescription());
        dmc.getDescription().add(description);

        dmc.setDescriptionLanguage(metadataModel.getDescriptionLanguage());
        dmc.setDescriptionLevel(metadataModel.getDescriptionLevel());
        dmc.setDocumentType(metadataModel.getDocumentType());
        dmc.setEndDate(metadataModel.getEndDate());
        dmc.getEvent().addAll(metadataModel.getEvent());
        dmc.setGps(metadataModel.getGps());
        dmc.setOriginatingAgency(metadataModel.getOriginatingAgency());


        if (metadataModel.getFilePlanPosition() != null && !metadataModel.getFilePlanPosition().isEmpty()){
            dmc.getFilePlanPosition().addAll(metadataModel.getFilePlanPosition());
        }

        if (metadataModel.getSystemId() != null && !metadataModel.getSystemId().isEmpty()) {
            dmc.getSystemId().addAll(metadataModel.getSystemId());
        }

        if ( metadataModel.getOriginatingSystemId() != null && !metadataModel.getOriginatingSystemId().isEmpty()) {
            dmc.getOriginatingSystemId().addAll(metadataModel.getOriginatingSystemId());
        }

        if (metadataModel.getArchivalAgencyArchiveUnitIdentifier() != null && !metadataModel.getArchivalAgencyArchiveUnitIdentifier().isEmpty()){
            dmc.getArchivalAgencyArchiveUnitIdentifier().addAll(metadataModel.getArchivalAgencyArchiveUnitIdentifier());
        }

        if (metadataModel.getOriginatingAgencyArchiveUnitIdentifier() != null && !metadataModel.getOriginatingAgencyArchiveUnitIdentifier().isEmpty()) {
            dmc.getOriginatingAgencyArchiveUnitIdentifier().addAll(metadataModel.getOriginatingAgencyArchiveUnitIdentifier());
        }

        if (metadataModel.getTransferringAgencyArchiveUnitIdentifier() != null && !metadataModel.getTransferringAgencyArchiveUnitIdentifier().isEmpty() ){
            dmc.getTransferringAgencyArchiveUnitIdentifier().addAll(
                metadataModel.getTransferringAgencyArchiveUnitIdentifier());
        }

        if (metadataModel.getLanguage() != null && !metadataModel.getLanguage().isEmpty()) {
            dmc.getLanguage().addAll(metadataModel.getLanguage());
        }

        if(metadataModel.getAuthorizedAgent() != null && !metadataModel.getAuthorizedAgent().isEmpty()){
            dmc.getAuthorizedAgent().addAll(metadataModel.getAuthorizedAgent());
        }


        if (metadataModel.getSignature() != null && !metadataModel.getSignature().isEmpty()) {
            dmc.getSignature().addAll(metadataModel.getSignature());
        }


        if (metadataModel.getRecipient() != null && !metadataModel.getRecipient().isEmpty()) {
            dmc.getRecipient().addAll(metadataModel.getRecipient());
        }

        if (metadataModel.getKeyword() != null && !metadataModel.getKeyword().isEmpty()) {
            dmc.getKeyword().addAll(metadataModel.getKeyword());
        }
        
        dmc.setReceivedDate(metadataModel.getReceivedDate());
        dmc.setRegisteredDate(metadataModel.getRegisteredDate());
        dmc.setRelatedObjectReference(metadataModel.getRelatedObjectReference());
        dmc.setRegisteredDate(metadataModel.getRegisteredDate());
        dmc.setSentDate(metadataModel.getSentDate());
        dmc.setSource(metadataModel.getSource());
        dmc.setStartDate(metadataModel.getStartDate());
        dmc.setStatus(metadataModel.getStatus());
        dmc.setSubmissionAgency(metadataModel.getSubmissionAgency());

        dmc.getTag().addAll(metadataModel.getTag());

        if (metadataModel.getTitle_() != null) {
            dmc.getTitle().addAll(metadataModel.getTitle_().getTextTypes());
        }

        TextType title = new TextType();
        title.setValue(metadataModel.getTitle());
        dmc.getTitle().add(title);

        dmc.setTransactedDate(metadataModel.getTransactedDate());
        dmc.setType(metadataModel.getType());
        dmc.setVersion(metadataModel.getVersion());
        dmc.getWriter().addAll(metadataModel.getWriter());

        dmc.getTransmitter().addAll(metadataModel.getTransmitter());
        dmc.getSender().addAll(metadataModel.getSender());

        return dmc;
    }

}
