/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common.model.administration;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import fr.gouv.vitam.common.model.ModelConstants;

/**
 * POJO java use for mapping @{@link fr.gouv.vitam.functional.administration.common.AccessionRegisterDetail}
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class AccessionRegisterDetailModel {

    /**
     * unique id
     */
    @JsonProperty(ModelConstants.HASH + ModelConstants.TAG_ID)
    private String id;

    /**
     * tenant id
     */
    @JsonProperty(ModelConstants.HASH + ModelConstants.TAG_TENANT)
    private Integer tenant;

    /**
     * document version
     */
    @JsonProperty(ModelConstants.HASH + ModelConstants.TAG_VERSION)
    private Integer version;

    /**
     * originating agency
     */
    @JsonProperty("OriginatingAgency")
    private String originatingAgency;

    /**
     * submission agency
     */
    @JsonProperty("SubmissionAgency")
    private String submissionAgency;

    /**
     * archival agreement identifier
     */
    @JsonProperty("ArchivalAgreement")
    private String archivalAgreement;

    /**
     * Acquisition Information
     */
    @JsonProperty("AcquisitionInformation")
    private String AcquisitionInformation;

    /**
     * Legal Status

     */
    @JsonProperty("LegalStatus")
    private String LegalStatus;

    /**
     * end date
     */
    @JsonProperty("EndDate")
    private String endDate;
    // TODO date object
    /**
     * start date
     */
    @JsonProperty("StartDate")
    private String startDate;

    /**
     * last update
     */
    @JsonProperty("LastUpdate")
    private String lastUpdate;

    /**
     * status
     */
    @JsonProperty("Status")
    private AccessionRegisterStatus status;

    /**
     * archive number
     */
    @JsonProperty("TotalObjectGroups")
    private RegisterValueDetailModel totalObjectsGroups;

    /**
     * archive unit number
     */
    @JsonProperty("TotalUnits")
    private RegisterValueDetailModel totalUnits;

    /**
     * archive object number
     */
    @JsonProperty("TotalObjects")
    private RegisterValueDetailModel totalObjects;

    /**
     * archive object size
     */
    @JsonProperty("ObjectSize")
    private RegisterValueDetailModel ObjectSize;

    /**
     * Linked ingest operation id
     */
    @JsonProperty("OperationIds")
    private List<String> operationsIds;

    @JsonProperty("Symbolic")
    private Boolean symbolic;

    /**
     * Constructor without fields use for jackson
     */
    public AccessionRegisterDetailModel() {
    }

    /**
     * @return id
     */
    public String getId() {
        return id;
    }


    /**
     * @param id value to set
     * @return this
     */

    public AccessionRegisterDetailModel setId(String id) {
        this.id = id;
        return this;
    }

    /**
     * @return tenant
     */
    public Integer getTenant() {
        return tenant;
    }

    /**
     * @param tenant the working tenant to set
     * @return this
     */
    public AccessionRegisterDetailModel setTenant(Integer tenant) {
        this.tenant = tenant;
        return this;
    }

    /**
     * @return version
     */
    public Integer getVersion() {
        return version;
    }

    /**
     * @param version
     */
    public void setVersion(Integer version) {
        this.version = version;
    }

    /**
     * @return originatingAgency
     */
    public String getOriginatingAgency() {
        return originatingAgency;
    }

    /**
     * @param originatingAgency value to set
     * @return this
     */
    public AccessionRegisterDetailModel setOriginatingAgency(String originatingAgency) {
        this.originatingAgency = originatingAgency;
        return this;
    }

    /**
     * @return submissionAgency
     */
    public String getSubmissionAgency() {
        return submissionAgency;
    }

    /**
     * @param submissionAgency value to set
     * @return this
     */
    public AccessionRegisterDetailModel setSubmissionAgency(String submissionAgency) {
        this.submissionAgency = submissionAgency;
        return this;
    }

    /**
     * @return archivalAgreement identifier
     */
    public String getArchivalAgreement() {
        return archivalAgreement;
    }

    /**
     * Set the archivalAgreement identifier
     *
     * @param archivalAgreement
     */
    public AccessionRegisterDetailModel setArchivalAgreement(String archivalAgreement) {
        this.archivalAgreement = archivalAgreement;
        return this;
    }

    /**
     * getter for AcquisitionInformation
     *
     * @return AcquisitionInformation value
     */
    public String getAcquisitionInformation() {
        return AcquisitionInformation;
    }

    /**
     * set AcquisitionInformation
     */
    public AccessionRegisterDetailModel setAcquisitionInformation(String acquisitionInformation) {
        AcquisitionInformation = acquisitionInformation;
        return this;
    }

    /**
     * getter for LegalStatus
     *
     * @return LegalStatus value
     */
    public String getLegalStatus() {
        return LegalStatus;
    }

    /**
     * set LegalStatus
     */
    public AccessionRegisterDetailModel  setLegalStatus(String legalStatus) {
        LegalStatus = legalStatus;
        return  this;
    }

    /**
     * @return endDate
     */
    public String getEndDate() {
        return endDate;
    }

    /**
     * @param endDate value to set
     * @return this
     */
    public AccessionRegisterDetailModel setEndDate(String endDate) {
        this.endDate = endDate;
        return this;
    }

    /**
     * @return startDate
     */
    public String getStartDate() {
        return startDate;
    }

    /**
     * @param startDate value to set
     * @return this
     */
    public AccessionRegisterDetailModel setStartDate(String startDate) {
        this.startDate = startDate;
        return this;
    }

    /**
     * @return lastUpdate
     */
    public String getLastUpdate() {
        return lastUpdate;
    }

    /**
     * @param lastUpdate value to set
     * @return this
     */
    public AccessionRegisterDetailModel setLastUpdate(String lastUpdate) {
        this.lastUpdate = lastUpdate;
        return this;
    }

    /**
     * @return status
     */
    public AccessionRegisterStatus getStatus() {
        return status;
    }

    /**
     * @param status value to set
     * @return this
     */
    public AccessionRegisterDetailModel setStatus(AccessionRegisterStatus status) {
        this.status = status;
        return this;
    }

    /**
     * @return totalObjectsGroups
     */
    public RegisterValueDetailModel getTotalObjectsGroups() {
        return totalObjectsGroups;
    }

    /**
     * @param totalObjectsGroups value to set
     * @return this
     */
    public AccessionRegisterDetailModel setTotalObjectsGroups(RegisterValueDetailModel totalObjectsGroups) {
        this.totalObjectsGroups = totalObjectsGroups;
        return this;
    }

    /**
     * @return totalUnits
     */
    public RegisterValueDetailModel getTotalUnits() {
        return totalUnits;
    }

    /**
     * @param totalUnits value to set
     * @return this
     */
    public AccessionRegisterDetailModel setTotalUnits(RegisterValueDetailModel totalUnits) {
        this.totalUnits = totalUnits;
        return this;
    }

    /**
     * @return totalObjects
     */
    public RegisterValueDetailModel getTotalObjects() {
        return totalObjects;
    }

    /**
     * @param totalObjects value to set
     * @return this
     */
    public AccessionRegisterDetailModel setTotalObjects(RegisterValueDetailModel totalObjects) {
        this.totalObjects = totalObjects;
        return this;
    }

    /**
     * @return ObjectSize
     */
    public RegisterValueDetailModel getObjectSize() {
        return ObjectSize;
    }

    /**
     * @param objectSize value to set
     * @return this
     */
    public AccessionRegisterDetailModel setObjectSize(RegisterValueDetailModel objectSize) {
        ObjectSize = objectSize;
        return this;
    }

    public List<String> getOperationsIds() {
        return operationsIds;
    }

    /**
     * Set operationIds in the model and return the updated AccessionRegisterDetailModel
     *
     * @param operationsIds id of linked ingest operations
     * @return this
     */
    public AccessionRegisterDetailModel setOperationsIds(List<String> operationsIds) {
        this.operationsIds = operationsIds;
        return this;
    }

    /**
     * Add an operationId to the model and return the updated AccessionRegisterDetailModel
     *
     * @param operationsId id of linked ingest operations that must be added
     * @return this
     */
    public AccessionRegisterDetailModel addOperationsId(String operationsId) {
        if (operationsIds == null) {
            operationsIds = new ArrayList<>();
        }
        operationsIds.add(operationsId);
        return this;
    }

    /**
     * @param symbolic specify if a detail is a main detail or a symbolic detail.
     * @return
     */
    public AccessionRegisterDetailModel setSymbolic(Boolean symbolic) {
        this.symbolic = symbolic;
        return this;
    }

    /**
     * @return symbolic
     */
    public Boolean isSymbolic() {
        return symbolic;
    }
}
