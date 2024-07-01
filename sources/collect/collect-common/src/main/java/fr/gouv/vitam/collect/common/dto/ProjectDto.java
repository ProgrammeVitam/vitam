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
        Integer tenant
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
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getArchivalAgreement() {
        return archivalAgreement;
    }

    public void setArchivalAgreement(String archivalAgreement) {
        this.archivalAgreement = archivalAgreement;
    }

    public String getMessageIdentifier() {
        return messageIdentifier;
    }

    public void setMessageIdentifier(String messageIdentifier) {
        this.messageIdentifier = messageIdentifier;
    }

    public String getArchivalAgencyIdentifier() {
        return archivalAgencyIdentifier;
    }

    public void setArchivalAgencyIdentifier(String archivalAgencyIdentifier) {
        this.archivalAgencyIdentifier = archivalAgencyIdentifier;
    }

    public String getTransferringAgencyIdentifier() {
        return transferringAgencyIdentifier;
    }

    public void setTransferringAgencyIdentifier(String transferringAgencyIdentifier) {
        this.transferringAgencyIdentifier = transferringAgencyIdentifier;
    }

    public String getOriginatingAgencyIdentifier() {
        return originatingAgencyIdentifier;
    }

    public void setOriginatingAgencyIdentifier(String originatingAgencyIdentifier) {
        this.originatingAgencyIdentifier = originatingAgencyIdentifier;
    }

    public String getSubmissionAgencyIdentifier() {
        return submissionAgencyIdentifier;
    }

    public void setSubmissionAgencyIdentifier(String submissionAgencyIdentifier) {
        this.submissionAgencyIdentifier = submissionAgencyIdentifier;
    }

    public String getArchivalProfile() {
        return archivalProfile;
    }

    public void setArchivalProfile(String archivalProfile) {
        this.archivalProfile = archivalProfile;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getUnitUp() {
        return unitUp;
    }

    public void setUnitUp(String unitUp) {
        this.unitUp = unitUp;
    }

    public Integer getTenant() {
        return tenant;
    }

    public void setTenant(Integer tenant) {
        this.tenant = tenant;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAcquisitionInformation() {
        return acquisitionInformation;
    }

    public void setAcquisitionInformation(String acquisitionInformation) {
        this.acquisitionInformation = acquisitionInformation;
    }

    public String getLegalStatus() {
        return legalStatus;
    }

    public void setLegalStatus(String legalStatus) {
        this.legalStatus = legalStatus;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    public String getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(String lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public List<MetadataUnitUp> getUnitUps() {
        return unitUps;
    }

    public void setUnitUps(List<MetadataUnitUp> unitUps) {
        this.unitUps = unitUps;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
