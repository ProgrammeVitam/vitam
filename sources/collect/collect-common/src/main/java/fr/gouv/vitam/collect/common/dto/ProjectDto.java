/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
package fr.gouv.vitam.collect.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.annotations.Beta;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProjectDto implements Serializable {

    @JsonProperty(value = "#id")
    private String id;

    @JsonProperty(value = "Name", required = true)
    private String name;

    @JsonProperty(value = "ArchivalAgreement", required = true)
    private String archivalAgreement;

    @JsonProperty(value = "MessageIdentifier", required = true)
    private String messageIdentifier;

    @JsonProperty(value = "ArchivalAgencyIdentifier", required = true)
    private String archivalAgencyIdentifier;

    @JsonProperty(value = "TransferringAgencyIdentifier", required = true)
    private String transferringAgencyIdentifier;

    @JsonProperty(value = "OriginatingAgencyIdentifier", required = true)
    private String originatingAgencyIdentifier;

    @JsonProperty(value = "SubmissionAgencyIdentifier")
    private String submissionAgencyIdentifier;

    @JsonProperty(value = "ArchiveProfile")
    private String archivalProfile;

    @JsonProperty(value = "AcquisitionInformation")
    private String acquisitionInformation;

    @JsonProperty(value = "LegalStatus")
    private String legalStatus;

    @JsonProperty(value = "Comment")
    private String comment;

    @JsonProperty(value = "UnitUp")
    private String unitUp;

    @JsonProperty(value = "#tenant")
    private Integer tenant;

    @JsonProperty(value = "CreationDate")
    private String creationDate;

    @JsonProperty(value = "LastUpdate")
    private String lastUpdate;

    @JsonProperty("UnitUps")
    private List<MetadataUnitUp> unitUps;

    @JsonProperty(value = "Status")
    private String status;

    /**
     * Transformation rules (beta): JSTL transformation template to be applied over Archive Unit JSON documents on zip
     * transaction upload for providing customized updates : add rule, unset field, set field...).
     * @apiNote Beta support only. May be removed or updated in future releases
     */
    @Beta
    @JsonProperty(value = "TransformationRules")
    private String transformationRules;

    @JsonProperty("AutomaticIngest")
    private Boolean automaticIngest;

    @JsonProperty("ArchivingSystemId")
    private String archivingSystemId;

    @JsonProperty("ArchivingSystemTenant")
    private Integer archivingSystemTenant;

    @JsonProperty("ConnectedToArchivingSystem")
    private Boolean connectedToArchivingSystem;

    public ProjectDto() {
        //Empty constructor for serialization
    }

    public ProjectDto(String id) {
        this.id = id;
    }

    public ProjectDto(
        String id,
        String name,
        String acquisitionInformation,
        String legalStatus,
        String creationDate,
        String lastUpdate,
        String status,
        String archivalAgreement,
        String messageIdentifier,
        String archivalAgencyIdentifier,
        String transferringAgencyIdentifier,
        String originatingAgencyIdentifier,
        String submissionAgencyIdentifier,
        String archivalProfile,
        String comment,
        String unitUp,
        Integer tenant,
        Boolean automaticIngest,
        String archivingSystemId,
        Integer archivingSystemTenant,
        Boolean connectedToArchivingSystem
    ) {
        this.id = id;
        this.name = name;
        this.acquisitionInformation = acquisitionInformation;
        this.legalStatus = legalStatus;
        this.creationDate = creationDate;
        this.lastUpdate = lastUpdate;
        this.status = status;
        this.archivalAgreement = archivalAgreement;
        this.messageIdentifier = messageIdentifier;
        this.archivalAgencyIdentifier = archivalAgencyIdentifier;
        this.transferringAgencyIdentifier = transferringAgencyIdentifier;
        this.originatingAgencyIdentifier = originatingAgencyIdentifier;
        this.submissionAgencyIdentifier = submissionAgencyIdentifier;
        this.archivalProfile = archivalProfile;
        this.comment = comment;
        this.unitUp = unitUp;
        this.tenant = tenant;
        this.automaticIngest = automaticIngest;
        this.archivingSystemId = archivingSystemId;
        this.archivingSystemTenant = archivingSystemTenant;
        this.connectedToArchivingSystem = connectedToArchivingSystem;
    }

    public String getId() {
        return id;
    }

    public ProjectDto setId(String id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public ProjectDto setName(String name) {
        this.name = name;
        return this;
    }

    public String getArchivalAgreement() {
        return archivalAgreement;
    }

    public ProjectDto setArchivalAgreement(String archivalAgreement) {
        this.archivalAgreement = archivalAgreement;
        return this;
    }

    public String getMessageIdentifier() {
        return messageIdentifier;
    }

    public ProjectDto setMessageIdentifier(String messageIdentifier) {
        this.messageIdentifier = messageIdentifier;
        return this;
    }

    public String getArchivalAgencyIdentifier() {
        return archivalAgencyIdentifier;
    }

    public ProjectDto setArchivalAgencyIdentifier(String archivalAgencyIdentifier) {
        this.archivalAgencyIdentifier = archivalAgencyIdentifier;
        return this;
    }

    public String getTransferringAgencyIdentifier() {
        return transferringAgencyIdentifier;
    }

    public ProjectDto setTransferringAgencyIdentifier(String transferringAgencyIdentifier) {
        this.transferringAgencyIdentifier = transferringAgencyIdentifier;
        return this;
    }

    public String getOriginatingAgencyIdentifier() {
        return originatingAgencyIdentifier;
    }

    public ProjectDto setOriginatingAgencyIdentifier(String originatingAgencyIdentifier) {
        this.originatingAgencyIdentifier = originatingAgencyIdentifier;
        return this;
    }

    public String getSubmissionAgencyIdentifier() {
        return submissionAgencyIdentifier;
    }

    public ProjectDto setSubmissionAgencyIdentifier(String submissionAgencyIdentifier) {
        this.submissionAgencyIdentifier = submissionAgencyIdentifier;
        return this;
    }

    public String getArchivalProfile() {
        return archivalProfile;
    }

    public ProjectDto setArchivalProfile(String archivalProfile) {
        this.archivalProfile = archivalProfile;
        return this;
    }

    public String getAcquisitionInformation() {
        return acquisitionInformation;
    }

    public ProjectDto setAcquisitionInformation(String acquisitionInformation) {
        this.acquisitionInformation = acquisitionInformation;
        return this;
    }

    public String getLegalStatus() {
        return legalStatus;
    }

    public ProjectDto setLegalStatus(String legalStatus) {
        this.legalStatus = legalStatus;
        return this;
    }

    public String getComment() {
        return comment;
    }

    public ProjectDto setComment(String comment) {
        this.comment = comment;
        return this;
    }

    public String getUnitUp() {
        return unitUp;
    }

    public ProjectDto setUnitUp(String unitUp) {
        this.unitUp = unitUp;
        return this;
    }

    public Integer getTenant() {
        return tenant;
    }

    public ProjectDto setTenant(Integer tenant) {
        this.tenant = tenant;
        return this;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public ProjectDto setCreationDate(String creationDate) {
        this.creationDate = creationDate;
        return this;
    }

    public String getLastUpdate() {
        return lastUpdate;
    }

    public ProjectDto setLastUpdate(String lastUpdate) {
        this.lastUpdate = lastUpdate;
        return this;
    }

    public List<MetadataUnitUp> getUnitUps() {
        return unitUps;
    }

    public ProjectDto setUnitUps(List<MetadataUnitUp> unitUps) {
        this.unitUps = unitUps;
        return this;
    }

    public String getStatus() {
        return status;
    }

    public ProjectDto setStatus(String status) {
        this.status = status;
        return this;
    }

    public Boolean getAutomaticIngest() {
        return automaticIngest;
    }

    public ProjectDto setAutomaticIngest(Boolean automaticIngest) {
        this.automaticIngest = automaticIngest;
        return this;
    }

    public String getArchivingSystemId() {
        return archivingSystemId;
    }

    public ProjectDto setArchivingSystemId(String archivingSystemId) {
        this.archivingSystemId = archivingSystemId;
        return this;
    }

    public Boolean getConnectedToArchivingSystem() {
        return connectedToArchivingSystem;
    }

    public ProjectDto setConnectedToArchivingSystem(Boolean connectedToArchivingSystem) {
        this.connectedToArchivingSystem = connectedToArchivingSystem;
        return this;
    }

    public Integer getArchivingSystemTenant() {
        return archivingSystemTenant;
    }

    public ProjectDto setArchivingSystemTenant(Integer archivingSystemTenant) {
        this.archivingSystemTenant = archivingSystemTenant;
        return this;
    }

    /**
     * Get transformation rules (beta): JSTL transformation template to be applied over Archive Unit JSON documents on zip
     * transaction upload for providing customized updates : add rule, unset field, set field...).
     * @apiNote Beta support only. May be removed or updated in future releases
     */
    @Beta
    public String getTransformationRules() {
        return transformationRules;
    }

    /**
     * Set transformation rules (beta): JSTL transformation template to be applied over Archive Unit JSON documents on zip
     * transaction upload for providing customized updates : add rule, unset field, set field...).
     * @apiNote Beta support only. May be removed or updated in future releases
     */
    @Beta
    public ProjectDto setTransformationRules(String transformationRules) {
        this.transformationRules = transformationRules;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProjectDto that = (ProjectDto) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
