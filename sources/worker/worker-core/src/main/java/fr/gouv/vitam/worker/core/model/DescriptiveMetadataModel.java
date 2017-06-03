package fr.gouv.vitam.worker.core.model;

import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;

import fr.gouv.culture.archivesdefrance.seda.v2.AgentType;
import fr.gouv.culture.archivesdefrance.seda.v2.CoverageType;
import fr.gouv.culture.archivesdefrance.seda.v2.DescriptiveMetadataContentType;
import fr.gouv.culture.archivesdefrance.seda.v2.EventType;
import fr.gouv.culture.archivesdefrance.seda.v2.GpsType;
import fr.gouv.culture.archivesdefrance.seda.v2.KeywordsType;
import fr.gouv.culture.archivesdefrance.seda.v2.LevelType;
import fr.gouv.culture.archivesdefrance.seda.v2.OrganizationType;
import fr.gouv.culture.archivesdefrance.seda.v2.TextType;

public class DescriptiveMetadataModel {

    private LevelType descriptionLevel;

    private String title;

    private TextByLang titles;

    private String filePlanPosition;

    private String systemId;

    private String originatingSystemId;

    private String archivalAgencyArchiveUnitIdentifier;

    private String originatingAgencyArchiveUnitIdentifier;

    private String transferringAgencyArchiveUnitIdentifier;

    private String description;

    private TextByLang descriptions;

    private DescriptiveMetadataContentType.CustodialHistory custodialHistory;

    private TextType type;

    private TextType documentType;

    private String language;

    private String descriptionLanguage;

    private String status;

    private String version;

    private List<String> tag;

    private List<KeywordsType> keyword;

    private CoverageType coverage;

    private OrganizationType originatingAgency;

    private OrganizationType submissionAgency;

    private AgentType authorizedAgent;

    private List<DescriptiveMetadataContentType.Writer> writer;

    private List<AgentType> addressee;

    private List<AgentType> recipient;

    private String source;

    private DescriptiveMetadataContentType.RelatedObjectReference relatedObjectReference;

    private String createdDate;

    private String transactedDate;

    private String acquiredDate;

    private String sentDate;

    private String receivedDate;

    private String registeredDate;

    private String startDate;

    private String endDate;

    private List<EventType> event;

    private DescriptiveMetadataContentType.Signature signature;

    private GpsType gps;

    private List<Object> any;

    private Object restrictionRuleIdRef;

    private String restrictionValue;

    private XMLGregorianCalendar restrictionEndDate;

    private String id;

    private String href;

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

    public TextByLang getTitles() {
        return titles;
    }

    public void setTitles(TextByLang titles) {
        this.titles = titles;
    }

    public String getFilePlanPosition() {
        return filePlanPosition;
    }

    public void setFilePlanPosition(String filePlanPosition) {
        this.filePlanPosition = filePlanPosition;
    }

    public String getSystemId() {
        return systemId;
    }

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    public String getOriginatingSystemId() {
        return originatingSystemId;
    }

    public void setOriginatingSystemId(String originatingSystemId) {
        this.originatingSystemId = originatingSystemId;
    }

    public String getArchivalAgencyArchiveUnitIdentifier() {
        return archivalAgencyArchiveUnitIdentifier;
    }

    public void setArchivalAgencyArchiveUnitIdentifier(String archivalAgencyArchiveUnitIdentifier) {
        this.archivalAgencyArchiveUnitIdentifier = archivalAgencyArchiveUnitIdentifier;
    }

    public String getOriginatingAgencyArchiveUnitIdentifier() {
        return originatingAgencyArchiveUnitIdentifier;
    }

    public void setOriginatingAgencyArchiveUnitIdentifier(String originatingAgencyArchiveUnitIdentifier) {
        this.originatingAgencyArchiveUnitIdentifier = originatingAgencyArchiveUnitIdentifier;
    }

    public String getTransferringAgencyArchiveUnitIdentifier() {
        return transferringAgencyArchiveUnitIdentifier;
    }

    public void setTransferringAgencyArchiveUnitIdentifier(String transferringAgencyArchiveUnitIdentifier) {
        this.transferringAgencyArchiveUnitIdentifier = transferringAgencyArchiveUnitIdentifier;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TextByLang getDescriptions() {
        return descriptions;
    }

    public void setDescriptions(TextByLang descriptions) {
        this.descriptions = descriptions;
    }

    public DescriptiveMetadataContentType.CustodialHistory getCustodialHistory() {
        return custodialHistory;
    }

    public void setCustodialHistory(
        DescriptiveMetadataContentType.CustodialHistory custodialHistory) {
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

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
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

    public AgentType getAuthorizedAgent() {
        return authorizedAgent;
    }

    public void setAuthorizedAgent(AgentType authorizedAgent) {
        this.authorizedAgent = authorizedAgent;
    }

    public List<DescriptiveMetadataContentType.Writer> getWriter() {
        return writer;
    }

    public void setWriter(List<DescriptiveMetadataContentType.Writer> writer) {
        this.writer = writer;
    }

    public List<AgentType> getAddressee() {
        return addressee;
    }

    public void setAddressee(List<AgentType> addressee) {
        this.addressee = addressee;
    }

    public List<AgentType> getRecipient() {
        return recipient;
    }

    public void setRecipient(List<AgentType> recipient) {
        this.recipient = recipient;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public DescriptiveMetadataContentType.RelatedObjectReference getRelatedObjectReference() {
        return relatedObjectReference;
    }

    public void setRelatedObjectReference(
        DescriptiveMetadataContentType.RelatedObjectReference relatedObjectReference) {
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

    public List<EventType> getEvent() {
        return event;
    }

    public void setEvent(List<EventType> event) {
        this.event = event;
    }

    public DescriptiveMetadataContentType.Signature getSignature() {
        return signature;
    }

    public void setSignature(DescriptiveMetadataContentType.Signature signature) {
        this.signature = signature;
    }

    public GpsType getGps() {
        return gps;
    }

    public void setGps(GpsType gps) {
        this.gps = gps;
    }

    public List<Object> getAny() {
        return any;
    }

    public void setAny(List<Object> any) {
        this.any = any;
    }

    public Object getRestrictionRuleIdRef() {
        return restrictionRuleIdRef;
    }

    public void setRestrictionRuleIdRef(Object restrictionRuleIdRef) {
        this.restrictionRuleIdRef = restrictionRuleIdRef;
    }

    public String getRestrictionValue() {
        return restrictionValue;
    }

    public void setRestrictionValue(String restrictionValue) {
        this.restrictionValue = restrictionValue;
    }

    public XMLGregorianCalendar getRestrictionEndDate() {
        return restrictionEndDate;
    }

    public void setRestrictionEndDate(XMLGregorianCalendar restrictionEndDate) {
        this.restrictionEndDate = restrictionEndDate;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }
}
