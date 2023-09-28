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

import fr.gouv.culture.archivesdefrance.seda.v2.DescriptiveMetadataContentType;
import fr.gouv.culture.archivesdefrance.seda.v2.EventType;
import fr.gouv.culture.archivesdefrance.seda.v2.ExtendedType;
import fr.gouv.culture.archivesdefrance.seda.v2.LinkingAgentIdentifierType;
import fr.gouv.culture.archivesdefrance.seda.v2.MessageDigestBinaryObjectType;
import fr.gouv.culture.archivesdefrance.seda.v2.ReferencedObjectType;
import fr.gouv.culture.archivesdefrance.seda.v2.RelatedObjectReferenceType;
import fr.gouv.culture.archivesdefrance.seda.v2.SignatureDescriptionType;
import fr.gouv.culture.archivesdefrance.seda.v2.SignatureType;
import fr.gouv.culture.archivesdefrance.seda.v2.SigningInformationType;
import fr.gouv.culture.archivesdefrance.seda.v2.TextType;
import fr.gouv.culture.archivesdefrance.seda.v2.TimestampingInformationType;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.mapping.mapper.ElementMapper;
import fr.gouv.vitam.common.model.unit.AdditionalProofType;
import fr.gouv.vitam.common.model.unit.CustodialHistoryModel;
import fr.gouv.vitam.common.model.unit.DescriptiveMetadataModel;
import fr.gouv.vitam.common.model.unit.DetachedSigningRoleType;
import fr.gouv.vitam.common.model.unit.EventTypeModel;
import fr.gouv.vitam.common.model.unit.LinkingAgentIdentifierTypeModel;
import fr.gouv.vitam.common.model.unit.ReferencedObjectTypeModel;
import fr.gouv.vitam.common.model.unit.SignatureInformationExtendedModel;
import fr.gouv.vitam.common.model.unit.SignatureTypeModel;
import fr.gouv.vitam.common.model.unit.SignedObjectDigestModel;
import fr.gouv.vitam.common.model.unit.SignatureDescriptionTypeModel;
import fr.gouv.vitam.common.model.unit.SigningInformationTypeModel;
import fr.gouv.vitam.common.model.unit.SigningRoleType;
import fr.gouv.vitam.common.model.unit.TextByLang;
import fr.gouv.vitam.common.model.unit.TimestampingInformationTypeModel;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Map the object DescriptiveMetadataContentType generated by jaxb when parse manifest.xml
 * To a local java object DescriptiveMetadataModel that should match Unit data base model
 */
public class DescriptiveMetadataMapper {

    /**
     * CustodialHistory mapper
     */
    private final CustodialHistoryMapper custodialHistoryMapper;

    /**
     * constructor
     */
    public DescriptiveMetadataMapper() {
        this.custodialHistoryMapper = new CustodialHistoryMapper();
    }

    /**
     * Map jaxb DescriptiveMetadataContentType to local DescriptiveMetadataModel
     *
     * @param metadataContentType JAXB Object
     * @return DescriptiveMetadataModel
     */
    public DescriptiveMetadataModel map(DescriptiveMetadataContentType metadataContentType) {

        DescriptiveMetadataModel descriptiveMetadataModel = new DescriptiveMetadataModel();
        descriptiveMetadataModel.setAcquiredDate(
            LocalDateUtil.transformIsoOffsetDateToIsoOffsetDateTime(metadataContentType.getAcquiredDate()));
        descriptiveMetadataModel.setAddressee(metadataContentType.getAddressee());
        if (metadataContentType.getPersistentIdentifier() != null) {
            descriptiveMetadataModel.setPersistentIdentifier(metadataContentType.getPersistentIdentifier());
        }
        descriptiveMetadataModel.setAny(ElementMapper.toMap(metadataContentType.getAny()));
        descriptiveMetadataModel
            .setArchivalAgencyArchiveUnitIdentifier(metadataContentType.getArchivalAgencyArchiveUnitIdentifier());

        descriptiveMetadataModel.setAuthorizedAgent(metadataContentType.getAuthorizedAgent());

        // Seda2.2 fields
        descriptiveMetadataModel.setAgent(metadataContentType.getAgent());
        descriptiveMetadataModel.setTextContent(metadataContentType.getTextContent());
        descriptiveMetadataModel.setOriginatingSystemIdReplyTo(metadataContentType.getOriginatingSystemIdReplyTo());
        descriptiveMetadataModel.setDateLitteral(metadataContentType.getDateLitteral());

        descriptiveMetadataModel.setCoverage(metadataContentType.getCoverage());
        descriptiveMetadataModel.setCreatedDate(
            LocalDateUtil.transformIsoOffsetDateToIsoOffsetDateTime(metadataContentType.getCreatedDate()));

        CustodialHistoryModel custodialHistoryModel =
            custodialHistoryMapper.map(metadataContentType.getCustodialHistory());
        descriptiveMetadataModel.setCustodialHistory(custodialHistoryModel);

        descriptiveMetadataModel.setDescription(findDefaultTextType(metadataContentType.getDescription()));
        TextByLang description_ = new TextByLang(metadataContentType.getDescription());

        if (description_.isNotEmpty()) {
            descriptiveMetadataModel.setDescription_(description_);
        }

        descriptiveMetadataModel.setDescriptionLanguage(metadataContentType.getDescriptionLanguage());
        descriptiveMetadataModel.setDescriptionLevel(metadataContentType.getDescriptionLevel());
        descriptiveMetadataModel.setDocumentType(metadataContentType.getDocumentType());
        descriptiveMetadataModel.setEndDate(metadataContentType.getEndDate());
        descriptiveMetadataModel.setEvent(mapEvents(metadataContentType.getEvent()));
        descriptiveMetadataModel.setFilePlanPosition(metadataContentType.getFilePlanPosition());
        descriptiveMetadataModel.setGps(metadataContentType.getGps());
        descriptiveMetadataModel.setKeyword(metadataContentType.getKeyword());
        descriptiveMetadataModel.setLanguage(metadataContentType.getLanguage());
        descriptiveMetadataModel.setOriginatingAgency(metadataContentType.getOriginatingAgency());
        descriptiveMetadataModel
            .setOriginatingAgencyArchiveUnitIdentifier(metadataContentType.getOriginatingAgencyArchiveUnitIdentifier());
        descriptiveMetadataModel.setOriginatingSystemId(metadataContentType.getOriginatingSystemId());
        descriptiveMetadataModel.setRecipient(metadataContentType.getRecipient());

        descriptiveMetadataModel.setRegisteredDate(
            LocalDateUtil.transformIsoOffsetDateToIsoOffsetDateTime(metadataContentType.getRegisteredDate()));
        descriptiveMetadataModel.setRelatedObjectReference(metadataContentType.getRelatedObjectReference());
        descriptiveMetadataModel.setReceivedDate(
            LocalDateUtil.transformIsoOffsetDateToIsoOffsetDateTime(metadataContentType.getReceivedDate()));
        descriptiveMetadataModel
            .setSentDate(LocalDateUtil.transformIsoOffsetDateToIsoOffsetDateTime(metadataContentType.getSentDate()));

        // Deprecated Old Signature model (Seda 2.1 & 2.2). Superseded by SigningInformation model in Seda 2.3+.
        descriptiveMetadataModel.setSignature(mapSignatures(metadataContentType.getSignature()));

        descriptiveMetadataModel.setSigningInformation(
            mapSigningInformation(metadataContentType.getSigningInformation()));

        descriptiveMetadataModel.setSource(metadataContentType.getSource());
        descriptiveMetadataModel
            .setStartDate(LocalDateUtil.transformIsoOffsetDateToIsoOffsetDateTime(metadataContentType.getStartDate()));
        descriptiveMetadataModel.setStatus(metadataContentType.getStatus());
        descriptiveMetadataModel.setSubmissionAgency(metadataContentType.getSubmissionAgency());
        descriptiveMetadataModel.setSystemId(metadataContentType.getSystemId());
        descriptiveMetadataModel.setTag(metadataContentType.getTag());

        descriptiveMetadataModel.setTitle(findDefaultTextType(metadataContentType.getTitle()));
        TextByLang title_ = new TextByLang(metadataContentType.getTitle());
        if (title_.isNotEmpty()) {
            descriptiveMetadataModel.setTitle_(title_);
        }

        descriptiveMetadataModel.setTransactedDate(
            LocalDateUtil.transformIsoOffsetDateToIsoOffsetDateTime(metadataContentType.getTransactedDate()));
        descriptiveMetadataModel.setTransferringAgencyArchiveUnitIdentifier(
            metadataContentType.getTransferringAgencyArchiveUnitIdentifier());
        descriptiveMetadataModel.setType(metadataContentType.getType());
        descriptiveMetadataModel.setVersion(metadataContentType.getVersion());
        descriptiveMetadataModel.setWriter(metadataContentType.getWriter());
        descriptiveMetadataModel.setTransmitter(metadataContentType.getTransmitter());
        descriptiveMetadataModel.setSender(metadataContentType.getSender());

        if (metadataContentType.getRelatedObjectReference() != null) {
            RelatedObjectReferenceType relatedObjectRef = metadataContentType.getRelatedObjectReference();
            RelatedObjectReferenceType relatedObjectRefNew = new RelatedObjectReferenceType();

            descriptiveMetadataModel.setRelatedObjectReference(relatedObjectRefNew);

            relatedObjectRefNew.getIsPartOf().addAll(relatedObjectRef.getIsPartOf());
            relatedObjectRefNew.getIsVersionOf().addAll(relatedObjectRef.getIsVersionOf());
            relatedObjectRefNew.getReplaces().addAll(relatedObjectRef.getReplaces());
            relatedObjectRefNew.getRequires().addAll(relatedObjectRef.getRequires());
            relatedObjectRefNew.getReferences().addAll(relatedObjectRef.getReferences());
        }

        return descriptiveMetadataModel;
    }

    private List<SignatureTypeModel> mapSignatures(List<SignatureType> signatures) {
        if (signatures == null) {
            return null;
        }
        return signatures.stream()
            .map(this::mapSignature)
            .collect(Collectors.toList());
    }

    private SignatureTypeModel mapSignature(SignatureType signatureType) {
        return new SignatureTypeModel()
            .setSigner(signatureType.getSigner())
            .setValidator(signatureType.getValidator())
            .setReferencedObject(mapReferencedObject(signatureType.getReferencedObject()))
            // Not supported in R11
            .setMasterdata(signatureType.getMasterdata());
    }

    private ReferencedObjectTypeModel mapReferencedObject(ReferencedObjectType referencedObject) {
        if (referencedObject == null) {
            return null;
        }
        return new ReferencedObjectTypeModel()
            .setSignedObjectId(referencedObject.getSignedObjectId())
            .setSignedObjectDigest(mapSignedObjectDigest(referencedObject.getSignedObjectDigest()));
    }

    private SignedObjectDigestModel mapSignedObjectDigest(MessageDigestBinaryObjectType signedMessageDigest) {
        if (signedMessageDigest == null) {
            return null;
        }
        return new SignedObjectDigestModel()
            .setAlgorithm(signedMessageDigest.getAlgorithm())
            .setValue(signedMessageDigest.getValue());
    }

    private SigningInformationTypeModel mapSigningInformation(SigningInformationType signingInformation) {
        if (signingInformation == null) {
            return null;
        }
        return new SigningInformationTypeModel()
            .setSigningRole(mapSigningRole(signingInformation.getSigningRole()))
            .setDetachedSigningRole(mapDetachedSigningRole(signingInformation.getDetachedSigningRole()))
            .setSignatureDescription(mapSignatureDescription(signingInformation.getSignatureDescription()))
            .setTimestampingInformation(mapTimestampingInformation(signingInformation.getTimestampingInformation()))
            .setAdditionalProof(mapAdditionalProof(signingInformation.getAdditionalProof()))
            .setExtended(mapExtendedParams(signingInformation.getExtended()));
    }

    private List<SigningRoleType> mapSigningRole(
        List<fr.gouv.culture.archivesdefrance.seda.v2.SigningRoleType> signingRole) {
        if (signingRole == null) {
            return null;
        }
        return signingRole.stream()
            .map(role -> {
                switch (role) {
                    case SIGNED_DOCUMENT:
                        return SigningRoleType.SIGNED_DOCUMENT;
                    case TIMESTAMP:
                        return SigningRoleType.TIMESTAMP;
                    case SIGNATURE:
                        return SigningRoleType.SIGNATURE;
                    case ADDITIONAL_PROOF:
                        return SigningRoleType.ADDITIONAL_PROOF;
                    default:
                        throw new IllegalStateException("Unexpected value: " + role);
                }
            })
            .collect(Collectors.toList());
    }

    private List<DetachedSigningRoleType> mapDetachedSigningRole(
        List<fr.gouv.culture.archivesdefrance.seda.v2.DetachedSigningRoleType> detachedSigningRole) {
        if (detachedSigningRole == null) {
            return null;
        }
        return detachedSigningRole.stream()
            .map(role -> {
                switch (role) {
                    case TIMESTAMP:
                        return DetachedSigningRoleType.TIMESTAMP;
                    case SIGNATURE:
                        return DetachedSigningRoleType.SIGNATURE;
                    case ADDITIONAL_PROOF:
                        return DetachedSigningRoleType.ADDITIONAL_PROOF;
                    default:
                        throw new IllegalStateException("Unexpected value: " + role);
                }
            })
            .collect(Collectors.toList());
    }

    private List<SignatureDescriptionTypeModel> mapSignatureDescription(
        List<SignatureDescriptionType> signature) {
        if (signature == null) {
            return null;
        }
        return signature.stream()
            .map(this::mapSignatureDescription)
            .collect(Collectors.toList());
    }

    private SignatureDescriptionTypeModel mapSignatureDescription(
        SignatureDescriptionType signatureDescriptionType) {
        return new SignatureDescriptionTypeModel()
            .setSigner(signatureDescriptionType.getSigner())
            .setValidator(signatureDescriptionType.getValidator())
            .setSigningType(signatureDescriptionType.getSigningType());
    }

    private List<TimestampingInformationTypeModel> mapTimestampingInformation(
        List<TimestampingInformationType> timestampingInformation) {
        if (timestampingInformation == null) {
            return null;
        }
        return timestampingInformation.stream()
            .map(timestampingInfo -> new TimestampingInformationTypeModel()
                .setTimeStamp(
                    timestampingInfo.getTimeStamp() == null ? null : timestampingInfo.getTimeStamp().toString())
                .setAdditionalTimestampingInformation(timestampingInfo.getAdditionalTimestampingInformation())
            ).collect(Collectors.toList());
    }

    private List<AdditionalProofType> mapAdditionalProof(
        List<fr.gouv.culture.archivesdefrance.seda.v2.AdditionalProofType> additionalProofs) {
        if (additionalProofs == null) {
            return null;
        }
        return additionalProofs.stream()
            .map(additionalProof ->
                new AdditionalProofType()
                    .setAdditionalProofInformation(additionalProof.getAdditionalProofInformation()))
            .collect(Collectors.toList());
    }

    private SignatureInformationExtendedModel mapExtendedParams(ExtendedType extended) {
        if (extended == null || extended.getAny().isEmpty()) {
            return null;
        }
        SignatureInformationExtendedModel extendedModel = new SignatureInformationExtendedModel();
        ElementMapper
            .toMap(extended.getAny())
            .forEach(extendedModel::setAny);
        return extendedModel;
    }

    private List<EventTypeModel> mapEvents(List<EventType> eventTypes) {
        return eventTypes.stream()
            .map(this::mapEvent)
            .collect(Collectors.toList());
    }

    private EventTypeModel mapEvent(EventType event) {
        return new EventTypeModel()
            .setEventDateTime(event.getEventDateTime())
            .setEventDetail(event.getEventDetail())
            .setEventDetailData(event.getEventDetailData())
            .setEventIdentifier(event.getEventIdentifier())
            .setEventType(event.getEventType())
            .setEventTypeCode(event.getEventTypeCode())
            .setOutcome(event.getOutcome())
            .setOutcomeDetail(event.getOutcomeDetail())
            .setOutcomeDetailMessage(event.getOutcomeDetailMessage())
            .setLinkingAgentIdentifier(event.getLinkingAgentIdentifier().stream().map(this::mapLinkingAgentIdentifier)
                .collect(Collectors.toList()));
    }

    private LinkingAgentIdentifierTypeModel mapLinkingAgentIdentifier(
        LinkingAgentIdentifierType linkingAgentIdentifierType) {
        if (linkingAgentIdentifierType == null) {
            return null;
        }
        var linkingAgentIdentifierTypeModel = new LinkingAgentIdentifierTypeModel();
        linkingAgentIdentifierTypeModel
            .setLinkingAgentIdentifierType(linkingAgentIdentifierType.getLinkingAgentIdentifierType());
        linkingAgentIdentifierTypeModel
            .setLinkingAgentIdentifierValue(linkingAgentIdentifierType.getLinkingAgentIdentifierValue());
        linkingAgentIdentifierTypeModel.setLinkingAgentRole(linkingAgentIdentifierType.getLinkingAgentRole());
        return linkingAgentIdentifierTypeModel;
    }

    public String findDefaultTextType(List<TextType> textTypes) {
        return textTypes.stream()
            .filter(t -> t.getLang() == null)
            .findFirst()
            .map(TextType::getValue).orElse(null);
    }

}
