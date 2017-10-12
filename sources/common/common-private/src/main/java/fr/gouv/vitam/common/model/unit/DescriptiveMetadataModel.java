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
package fr.gouv.vitam.common.model.unit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.datatype.XMLGregorianCalendar;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
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

    private CustodialHistoryModel custodialHistory;

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

    private AgentTypeModel authorizedAgent;

    private List<DescriptiveMetadataContentType.Writer> writer;

    private List<AgentTypeModel> addressee;

    private List<AgentTypeModel> recipient;

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

    @JsonIgnore
    private Map<String, Object> any = new HashMap<>();

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
        if (tag == null) {
            tag = new ArrayList<>();
        }
        return tag;
    }

    public void setTag(List<String> tag) {
        this.tag = tag;
    }

    public List<KeywordsType> getKeyword() {
        if (keyword == null) {
            keyword = new ArrayList<>();
        }
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

    public AgentTypeModel getAuthorizedAgent() {
        return authorizedAgent;
    }

    public void setAuthorizedAgent(AgentTypeModel authorizedAgent) {
        this.authorizedAgent = authorizedAgent;
    }

    public List<DescriptiveMetadataContentType.Writer> getWriter() {
        if (writer == null) {
            writer = new ArrayList<>();
        }
        return writer;
    }

    public void setWriter(List<DescriptiveMetadataContentType.Writer> writer) {
        this.writer = writer;
    }

    public List<AgentTypeModel> getAddressee() {
        if (addressee == null) {
            addressee = new ArrayList<>();
        }
        return addressee;
    }

    public void setAddressee(List<AgentTypeModel> addressee) {
        this.addressee = addressee;
    }

    public List<AgentTypeModel> getRecipient() {
        if (recipient == null) {
            recipient = new ArrayList<>();
        }
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
        if (event == null) {
            event = new ArrayList<>();
        }
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
