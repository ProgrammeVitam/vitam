/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import fr.gouv.culture.archivesdefrance.seda.v2.SignatureType;
import fr.gouv.culture.archivesdefrance.seda.v2.TextType;

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

    @JsonProperty("Signature")
    private List<SignatureTypeModel> signature;

    @JsonProperty("Gps")
    private GpsType gps;

    @JsonIgnore
    private Map<String, Object> any = new HashMap<>();

    /**
     * @return
     */
    public LevelType getDescriptionLevel() {
        return descriptionLevel;
    }

    /**
     * @param descriptionLevel
     */
    public void setDescriptionLevel(LevelType descriptionLevel) {
        this.descriptionLevel = descriptionLevel;
    }

    /**
     * @return
     */
    public String getTitle() {
        return title;
    }

    /**
     * @param title
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * 
     * @return title_
     */
    public TextByLang getTitle_() {
        return title_;
    }

    /**
     * 
     * @param title_
     */
    public void setTitle_(TextByLang title_) {
        this.title_ = title_;
    }

    /**
     * @return
     */
    public List<String> getFilePlanPosition() {
        return filePlanPosition;
    }

    /**
     * @param filePlanPosition
     */
    public void setFilePlanPosition(List<String> filePlanPosition) {
        this.filePlanPosition = filePlanPosition;
    }

    /**
     * @return
     */
    public List<String> getSystemId() {
        return systemId;
    }

    /**
     * @param systemId
     */
    public void setSystemId(List<String> systemId) {
        this.systemId = systemId;
    }

    /**
     * @return
     */
    public List<String> getOriginatingSystemId() {
        return originatingSystemId;
    }

    /**
     * @param originatingSystemId
     */
    public void setOriginatingSystemId(List<String> originatingSystemId) {
        this.originatingSystemId = originatingSystemId;
    }

    /**
     * @return
     */
    public List<String> getArchivalAgencyArchiveUnitIdentifier() {
        return archivalAgencyArchiveUnitIdentifier;
    }

    /**
     * @param archivalAgencyArchiveUnitIdentifier
     */
    public void setArchivalAgencyArchiveUnitIdentifier(List<String> archivalAgencyArchiveUnitIdentifier) {
        this.archivalAgencyArchiveUnitIdentifier = archivalAgencyArchiveUnitIdentifier;
    }

    /**
     * @return
     */
    public List<String> getOriginatingAgencyArchiveUnitIdentifier() {
        return originatingAgencyArchiveUnitIdentifier;
    }

    /**
     * @param originatingAgencyArchiveUnitIdentifier
     */
    public void setOriginatingAgencyArchiveUnitIdentifier(List<String> originatingAgencyArchiveUnitIdentifier) {
        this.originatingAgencyArchiveUnitIdentifier = originatingAgencyArchiveUnitIdentifier;
    }

    /**
     * @return
     */
    public List<String> getTransferringAgencyArchiveUnitIdentifier() {
        return transferringAgencyArchiveUnitIdentifier;
    }

    /**
     * @param transferringAgencyArchiveUnitIdentifier
     */
    public void setTransferringAgencyArchiveUnitIdentifier(List<String> transferringAgencyArchiveUnitIdentifier) {
        this.transferringAgencyArchiveUnitIdentifier = transferringAgencyArchiveUnitIdentifier;
    }

    /**
     * @return
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * 
     * @return description_
     */
    public TextByLang getDescription_() {
        return description_;
    }

    /**
     * 
     * @param description_
     */
    public void setDescription_(TextByLang description_) {
        this.description_ = description_;
    }

    /**
     * @return
     */
    public CustodialHistoryModel getCustodialHistory() {
        return custodialHistory;
    }

    /**
     * @param custodialHistory
     */
    public void setCustodialHistory(CustodialHistoryModel custodialHistory) {
        this.custodialHistory = custodialHistory;
    }

    /**
     * @return
     */
    public TextType getType() {
        return type;
    }

    /**
     * @param type
     */
    public void setType(TextType type) {
        this.type = type;
    }

    /**
     * @return
     */
    public TextType getDocumentType() {
        return documentType;
    }

    /**
     * @param documentType
     */
    public void setDocumentType(TextType documentType) {
        this.documentType = documentType;
    }

    /**
     * @return
     */
    public List<String> getLanguage() {
        return language;
    }

    /**
     * @param language
     */
    public void setLanguage(List<String> language) {
        this.language = language;
    }

    /**
     * @return
     */
    public String getDescriptionLanguage() {
        return descriptionLanguage;
    }

    /**
     * @param descriptionLanguage
     */
    public void setDescriptionLanguage(String descriptionLanguage) {
        this.descriptionLanguage = descriptionLanguage;
    }

    /**
     * @return
     */
    public String getStatus() {
        return status;
    }

    /**
     * @param status
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * @return
     */
    public String getVersion() {
        return version;
    }

    /**
     * @param version
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * @return
     */
    public List<String> getTag() {
        if (tag == null) {
            tag = new ArrayList<>();
        }
        return tag;
    }

    /**
     * @param tag
     */
    public void setTag(List<String> tag) {
        this.tag = tag;
    }

    /**
     * @return
     */
    public List<KeywordsType> getKeyword() {
        if (keyword == null) {
            keyword = new ArrayList<>();
        }
        return keyword;
    }

    /**
     * @param keyword
     */
    public void setKeyword(List<KeywordsType> keyword) {
        this.keyword = keyword;
    }

    /**
     * @return
     */
    public CoverageType getCoverage() {
        return coverage;
    }

    /**
     * @param coverage
     */
    public void setCoverage(CoverageType coverage) {
        this.coverage = coverage;
    }

    /**
     * @return
     */
    public OrganizationType getOriginatingAgency() {
        return originatingAgency;
    }

    /**
     * @param originatingAgency
     */
    public void setOriginatingAgency(OrganizationType originatingAgency) {
        this.originatingAgency = originatingAgency;
    }

    /**
     * @return
     */
    public OrganizationType getSubmissionAgency() {
        return submissionAgency;
    }

    /**
     * @param submissionAgency
     */
    public void setSubmissionAgency(OrganizationType submissionAgency) {
        this.submissionAgency = submissionAgency;
    }

    /**
     * @return
     */
    public List<AgentTypeModel> getAuthorizedAgent() {
        return authorizedAgent;
    }

    /**
     * @param authorizedAgent
     */
    public void setAuthorizedAgent(List<AgentTypeModel> authorizedAgent) {
        this.authorizedAgent = authorizedAgent;
    }

    /**
     * @return
     */
    public List<AgentTypeModel> getWriter() {
        if (writer == null) {
            writer = new ArrayList<>();
        }
        return writer;
    }

    /**
     * @param writer
     */
    public void setWriter(List<AgentTypeModel> writer) {
        this.writer = writer;
    }

    /**
     * @return
     */
    public List<AgentTypeModel> getAddressee() {
        if (addressee == null) {
            addressee = new ArrayList<>();
        }
        return addressee;
    }

    /**
     * getter for transmitter
     *
     * @return transmitter value
     */
    public List<AgentTypeModel> getTransmitter() {
        if(transmitter == null){
            transmitter = new ArrayList<>();
        }
        return transmitter;
    }

    /**
     * set transmitter
     */
    public void setTransmitter(List<AgentTypeModel> transmitter) {
        this.transmitter = transmitter;
    }

    /**
     * getter for sender
     *
     * @return sender value
     */
    public List<AgentTypeModel> getSender() {
        if(sender == null){
            sender = new ArrayList<>();
        }
        return sender;
    }

    /**
     * set sender
     */
    public void setSender(List<AgentTypeModel> sender) {
        this.sender = sender;
    }

    /**
     * @param addressee
     */
    public void setAddressee(List<AgentTypeModel> addressee) {
        this.addressee = addressee;
    }

    /**
     * @return
     */
    public List<AgentTypeModel> getRecipient() {
        if (recipient == null) {
            recipient = new ArrayList<>();
        }
        return recipient;
    }

    /**
     * @param recipient
     */
    public void setRecipient(List<AgentTypeModel> recipient) {
        this.recipient = recipient;
    }

    /**
     * @return
     */
    public String getSource() {
        return source;
    }

    /**
     * @param source
     */
    public void setSource(String source) {
        this.source = source;
    }

    /**
     * @return
     */
    public RelatedObjectReferenceType getRelatedObjectReference() {
        return relatedObjectReference;
    }

    /**
     * @param relatedObjectReference
     */
    public void setRelatedObjectReference(RelatedObjectReferenceType relatedObjectReference) {
        this.relatedObjectReference = relatedObjectReference;
    }

    /**
     * @return
     */
    public String getCreatedDate() {
        return createdDate;
    }

    /**
     * @param createdDate
     */
    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

    /**
     * @return
     */
    public String getTransactedDate() {
        return transactedDate;
    }

    /**
     * @param transactedDate
     */
    public void setTransactedDate(String transactedDate) {
        this.transactedDate = transactedDate;
    }

    /**
     * @return
     */
    public String getAcquiredDate() {
        return acquiredDate;
    }

    /**
     * @param acquiredDate
     */
    public void setAcquiredDate(String acquiredDate) {
        this.acquiredDate = acquiredDate;
    }

    /**
     * @return
     */
    public String getSentDate() {
        return sentDate;
    }

    /**
     * @param sentDate
     */
    public void setSentDate(String sentDate) {
        this.sentDate = sentDate;
    }

    /**
     * @return
     */
    public String getReceivedDate() {
        return receivedDate;
    }

    /**
     * @param receivedDate
     */
    public void setReceivedDate(String receivedDate) {
        this.receivedDate = receivedDate;
    }

    /**
     * @return
     */
    public String getRegisteredDate() {
        return registeredDate;
    }

    /**
     * @param registeredDate
     */
    public void setRegisteredDate(String registeredDate) {
        this.registeredDate = registeredDate;
    }

    /**
     * @return
     */
    public String getStartDate() {
        return startDate;
    }

    /**
     * @param startDate
     */
    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    /**
     * @return
     */
    public String getEndDate() {
        return endDate;
    }

    /**
     * @param endDate
     */
    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    /**
     * @return
     */
    public List<EventTypeModel> getEvent() {
        if (event == null) {
            event = new ArrayList<>();
        }
        return event;
    }

    /**
     * @param event
     */
    public void setEvent(List<EventTypeModel> event) {
        this.event = event;
    }

    /**
     * @return
     */
    public List<SignatureTypeModel> getSignature() {
        return signature;
    }

    /**
     * @param signature
     */
    public void setSignature(List<SignatureTypeModel> signature) {
        this.signature = signature;
    }

    /**
     * @return
     */
    public GpsType getGps() {
        return gps;
    }

    /**
     * @param gps
     */
    public void setGps(GpsType gps) {
        this.gps = gps;
    }

    /**
     * @param any
     */
    @JsonIgnore
    public void setAny(Map<String, Object> any) {
        this.any = any;
    }

    /**
     * @return
     */
    @JsonAnyGetter
    public Map<String, Object> getAny() {
        return any;
    }

    /**
     * @param key
     * @param value
     */
    @JsonAnySetter
    public void setAny(String key, Object value) {
        if (key != null && key.startsWith("#")) {
            return;
        }
        this.any.put(key, value);
    }
}
