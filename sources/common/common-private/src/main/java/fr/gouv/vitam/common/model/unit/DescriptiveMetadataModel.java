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
package fr.gouv.vitam.common.model.unit;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.culture.archivesdefrance.seda.v2.CoverageType;
import fr.gouv.culture.archivesdefrance.seda.v2.GpsType;
import fr.gouv.culture.archivesdefrance.seda.v2.KeywordsType;
import fr.gouv.culture.archivesdefrance.seda.v2.LevelType;
import fr.gouv.culture.archivesdefrance.seda.v2.OrganizationType;
import fr.gouv.culture.archivesdefrance.seda.v2.RelatedObjectReferenceType;
import fr.gouv.culture.archivesdefrance.seda.v2.TextType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DescriptiveMetadataModel POJO
 */
public class DescriptiveMetadataModel {

    @JsonProperty("DescriptionLevel")
    private LevelType descriptionLevel;

    @JsonProperty("Title")
    private String title;

    @JsonProperty("Title_")
    private TextByLang title_;

    @JsonProperty("FilePlanPosition")
    private List<String> filePlanPosition;

    @JsonProperty("SystemId")
    private List<String> systemId;

    @JsonProperty("OriginatingSystemId")
    private List<String> originatingSystemId;

    @JsonProperty("ArchivalAgencyArchiveUnitIdentifier")
    private List<String> archivalAgencyArchiveUnitIdentifier;

    @JsonProperty("OriginatingAgencyArchiveUnitIdentifier")
    private List<String> originatingAgencyArchiveUnitIdentifier;

    @JsonProperty("TransferringAgencyArchiveUnitIdentifier")
    private List<String> transferringAgencyArchiveUnitIdentifier;

    @JsonProperty("Description")
    private String description;

    @JsonProperty("Description_")
    private TextByLang description_;

    @JsonProperty("CustodialHistory")
    private CustodialHistoryModel custodialHistory;

    @JsonProperty("Type")
    private TextType type;

    @JsonProperty("DocumentType")
    private TextType documentType;

    @JsonProperty("Language")
    private List<String> language;

    @JsonProperty("DescriptionLanguage")
    private String descriptionLanguage;

    @JsonProperty("Status")
    private String status;

    @JsonProperty("Version")
    private String version;

    @JsonProperty("Tag")
    private List<String> tag;

    @JsonProperty("Keyword")
    private List<KeywordsType> keyword;

    @JsonProperty("Coverage")
    private CoverageType coverage;

    @JsonProperty("OriginatingAgency")
    private OrganizationType originatingAgency;

    @JsonProperty("SubmissionAgency")
    private OrganizationType submissionAgency;

    @JsonProperty("AuthorizedAgent")
    private List<AgentTypeModel> authorizedAgent;

    @JsonProperty("Agent")
    private List<AgentTypeModel> agent;

    @JsonProperty("Writer")
    private List<AgentTypeModel> writer;

    @JsonProperty("Addressee")
    private List<AgentTypeModel> addressee;

    @JsonProperty("Recipient")
    private List<AgentTypeModel> recipient;

    @JsonProperty("Transmitter")
    private List<AgentTypeModel> transmitter;

    @JsonProperty("Sender")
    private List<AgentTypeModel> sender;

    @JsonProperty("Source")
    private String source;

    @JsonProperty("RelatedObjectReference")
    private RelatedObjectReferenceType relatedObjectReference;

    @JsonProperty("CreatedDate")
    private String createdDate;

    @JsonProperty("TransactedDate")
    private String transactedDate;

    @JsonProperty("AcquiredDate")
    private String acquiredDate;

    @JsonProperty("SentDate")
    private String sentDate;

    @JsonProperty("ReceivedDate")
    private String receivedDate;

    @JsonProperty("RegisteredDate")
    private String registeredDate;

    @JsonProperty("StartDate")
    private String startDate;

    @JsonProperty("EndDate")
    private String endDate;

    @JsonProperty("Event")
    private List<EventTypeModel> event;

    /**
     * @deprecated Old Signature model (Seda 2.1 & 2.2). Superseded by SigningInformation model in Seda 2.3+.
     */
    @JsonProperty("Signature")
    private List<SignatureTypeModel> signature;

    @JsonProperty("SigningInformation")
    private SigningInformationTypeModel signingInformation;

    @JsonProperty("Gps")
    private GpsType gps;

    @JsonProperty("TextContent")
    private List<String> textContent;


    @JsonProperty("OriginatingSystemIdReplyTo")
    private String originatingSystemIdReplyTo;

    @JsonProperty("DateLitteral")
    private String dateLitteral;

    @JsonIgnore
    private Map<String, Object> any = new HashMap<>();


    @JsonProperty("PersistentIdentifier")
    private List<PersistentIdentifierModel> persistentIdentifier;

    public LevelType getDescriptionLevel() {
        return descriptionLevel;
    }

    public void setDescriptionLevel(LevelType descriptionLevel) {
        this.descriptionLevel = descriptionLevel;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public TextByLang getTitle_() {
        return title_;
    }

    public void setTitle_(TextByLang title_) {
        this.title_ = title_;
    }

    public List<String> getFilePlanPosition() {
        return filePlanPosition;
    }

    public void setFilePlanPosition(List<String> filePlanPosition) {
        this.filePlanPosition = filePlanPosition;
    }

    public List<String> getSystemId() {
        return systemId;
    }

    public void setSystemId(List<String> systemId) {
        this.systemId = systemId;
    }

    public List<String> getOriginatingSystemId() {
        return originatingSystemId;
    }

    public void setOriginatingSystemId(List<String> originatingSystemId) {
        this.originatingSystemId = originatingSystemId;
    }

    public List<String> getArchivalAgencyArchiveUnitIdentifier() {
        return archivalAgencyArchiveUnitIdentifier;
    }

    public void setArchivalAgencyArchiveUnitIdentifier(List<String> archivalAgencyArchiveUnitIdentifier) {
        this.archivalAgencyArchiveUnitIdentifier = archivalAgencyArchiveUnitIdentifier;
    }

    public List<String> getOriginatingAgencyArchiveUnitIdentifier() {
        return originatingAgencyArchiveUnitIdentifier;
    }

    public void setOriginatingAgencyArchiveUnitIdentifier(List<String> originatingAgencyArchiveUnitIdentifier) {
        this.originatingAgencyArchiveUnitIdentifier = originatingAgencyArchiveUnitIdentifier;
    }

    public List<String> getTransferringAgencyArchiveUnitIdentifier() {
        return transferringAgencyArchiveUnitIdentifier;
    }

    public void setTransferringAgencyArchiveUnitIdentifier(List<String> transferringAgencyArchiveUnitIdentifier) {
        this.transferringAgencyArchiveUnitIdentifier = transferringAgencyArchiveUnitIdentifier;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TextByLang getDescription_() {
        return description_;
    }

    public void setDescription_(TextByLang description_) {
        this.description_ = description_;
    }

    public CustodialHistoryModel getCustodialHistory() {
        return custodialHistory;
    }

    public void setCustodialHistory(CustodialHistoryModel custodialHistory) {
        this.custodialHistory = custodialHistory;
    }

    public TextType getType() {
        return type;
    }

    public void setType(TextType type) {
        this.type = type;
    }

    public TextType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(TextType documentType) {
        this.documentType = documentType;
    }

    public List<String> getLanguage() {
        return language;
    }

    public void setLanguage(List<String> language) {
        this.language = language;
    }

    public String getDescriptionLanguage() {
        return descriptionLanguage;
    }

    public void setDescriptionLanguage(String descriptionLanguage) {
        this.descriptionLanguage = descriptionLanguage;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<String> getTag() {
        return tag;
    }

    public void setTag(List<String> tag) {
        this.tag = tag;
    }

    public List<KeywordsType> getKeyword() {
        return keyword;
    }

    public void setKeyword(List<KeywordsType> keyword) {
        this.keyword = keyword;
    }

    public CoverageType getCoverage() {
        return coverage;
    }

    public void setCoverage(CoverageType coverage) {
        this.coverage = coverage;
    }

    public OrganizationType getOriginatingAgency() {
        return originatingAgency;
    }

    public void setOriginatingAgency(OrganizationType originatingAgency) {
        this.originatingAgency = originatingAgency;
    }

    public OrganizationType getSubmissionAgency() {
        return submissionAgency;
    }

    public void setSubmissionAgency(OrganizationType submissionAgency) {
        this.submissionAgency = submissionAgency;
    }

    public List<AgentTypeModel> getAuthorizedAgent() {
        return authorizedAgent;
    }

    public List<AgentTypeModel> getAgent() {
        return agent;
    }

    public void setAuthorizedAgent(List<AgentTypeModel> authorizedAgent) {
        this.authorizedAgent = authorizedAgent;
    }

    public void setAgent(List<AgentTypeModel> agent) {
        this.agent = agent;
    }

    public List<AgentTypeModel> getWriter() {
        return writer;
    }

    public void setWriter(List<AgentTypeModel> writer) {
        this.writer = writer;
    }

    public List<AgentTypeModel> getAddressee() {
        return addressee;
    }

    public List<AgentTypeModel> getTransmitter() {
        return transmitter;
    }

    public void setTransmitter(List<AgentTypeModel> transmitter) {
        this.transmitter = transmitter;
    }

    public List<AgentTypeModel> getSender() {
        return sender;
    }

    public void setSender(List<AgentTypeModel> sender) {
        this.sender = sender;
    }

    public void setAddressee(List<AgentTypeModel> addressee) {
        this.addressee = addressee;
    }

    public List<AgentTypeModel> getRecipient() {
        return recipient;
    }

    public void setRecipient(List<AgentTypeModel> recipient) {
        this.recipient = recipient;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public RelatedObjectReferenceType getRelatedObjectReference() {
        return relatedObjectReference;
    }

    public void setRelatedObjectReference(RelatedObjectReferenceType relatedObjectReference) {
        this.relatedObjectReference = relatedObjectReference;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

    public String getTransactedDate() {
        return transactedDate;
    }

    public void setTransactedDate(String transactedDate) {
        this.transactedDate = transactedDate;
    }

    public String getAcquiredDate() {
        return acquiredDate;
    }

    public void setAcquiredDate(String acquiredDate) {
        this.acquiredDate = acquiredDate;
    }

    public String getSentDate() {
        return sentDate;
    }

    public void setSentDate(String sentDate) {
        this.sentDate = sentDate;
    }

    public String getReceivedDate() {
        return receivedDate;
    }

    public void setReceivedDate(String receivedDate) {
        this.receivedDate = receivedDate;
    }

    public String getRegisteredDate() {
        return registeredDate;
    }

    public void setRegisteredDate(String registeredDate) {
        this.registeredDate = registeredDate;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public List<EventTypeModel> getEvent() {
        return event;
    }

    public void setEvent(List<EventTypeModel> event) {
        this.event = event;
    }

    /**
     * @deprecated Old Signature model (Seda 2.1 & 2.2). Superseded by SigningInformation model in Seda 2.3+.
     */
    public List<SignatureTypeModel> getSignature() {
        return signature;
    }

    /**
     * @deprecated Old Signature model (Seda 2.1 & 2.2). Superseded by SigningInformation model in Seda 2.3+.
     */
    public DescriptiveMetadataModel setSignature(
        List<SignatureTypeModel> signature) {
        this.signature = signature;
        return this;
    }

    public SigningInformationTypeModel getSigningInformation() {
        return signingInformation;
    }

    public void setSigningInformation(SigningInformationTypeModel signingInformation) {
        this.signingInformation = signingInformation;
    }

    public GpsType getGps() {
        return gps;
    }

    public void setGps(GpsType gps) {
        this.gps = gps;
    }

    public List<String> getTextContent() {
        return textContent;
    }

    public void setTextContent(List<String> textContent) {
        this.textContent = textContent;
    }

    public String getOriginatingSystemIdReplyTo() {
        return originatingSystemIdReplyTo;
    }

    public void setOriginatingSystemIdReplyTo(String originatingSystemIdReplyTo) {
        this.originatingSystemIdReplyTo = originatingSystemIdReplyTo;
    }

    public String getDateLitteral() {
        return dateLitteral;
    }

    public void setDateLitteral(String dateLitteral) {
        this.dateLitteral = dateLitteral;
    }

    public List<PersistentIdentifierModel> getPersistentIdentifier() {
        return persistentIdentifier;
    }

    public void setPersistentIdentifier(
        List<PersistentIdentifierModel> persistentIdentifier) {
        this.persistentIdentifier = persistentIdentifier;
    }

    @JsonIgnore
    public void setAny(Map<String, Object> any) {
        this.any = any;
    }

    @JsonAnyGetter
    public Map<String, Object> getAny() {
        return any;
    }

    @JsonAnySetter
    public void setAny(String key, Object value) {
        if (key != null && key.startsWith("#")) {
            return;
        }
        this.any.put(key, value);
    }
}
