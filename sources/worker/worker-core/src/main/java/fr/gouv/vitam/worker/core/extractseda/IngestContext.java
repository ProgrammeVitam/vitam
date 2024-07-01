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
package fr.gouv.vitam.worker.core.extractseda;

import fr.gouv.vitam.common.model.UnitType;
import fr.gouv.vitam.common.model.administration.IngestContractModel;
import fr.gouv.vitam.common.model.administration.ManagementContractModel;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;

public class IngestContext {

    // Vitam Context

    private String operationId;
    private LogbookTypeProcess typeProcess;
    private UnitType workflowUnitType;

    // Seda Context

    private IngestContractModel ingestContract;

    private ManagementContractModel managementContractModel;

    private String originatingAgency;
    private String submissionAgencyIdentifier;
    private String globalNeedAuthorization;
    private String transferringAgency;
    private String archivalAgency;
    private String archivalProfile;
    private String sedaVersion;

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public LogbookTypeProcess getTypeProcess() {
        return typeProcess;
    }

    public void setTypeProcess(LogbookTypeProcess typeProcess) {
        this.typeProcess = typeProcess;
    }

    public UnitType getWorkflowUnitType() {
        return workflowUnitType;
    }

    public void setWorkflowUnitType(UnitType workflowUnitType) {
        this.workflowUnitType = workflowUnitType;
    }

    public IngestContractModel getIngestContract() {
        return ingestContract;
    }

    public void setIngestContract(IngestContractModel ingestContract) {
        this.ingestContract = ingestContract;
    }

    public ManagementContractModel getManagementContractModel() {
        return managementContractModel;
    }

    public void setManagementContractModel(ManagementContractModel managementContractModel) {
        this.managementContractModel = managementContractModel;
    }

    public String getOriginatingAgency() {
        return originatingAgency;
    }

    public void setOriginatingAgency(String originatingAgency) {
        this.originatingAgency = originatingAgency;
    }

    public String getSubmissionAgencyIdentifier() {
        return submissionAgencyIdentifier;
    }

    public void setSubmissionAgencyIdentifier(String submissionAgencyIdentifier) {
        this.submissionAgencyIdentifier = submissionAgencyIdentifier;
    }

    public String getGlobalNeedAuthorization() {
        return globalNeedAuthorization;
    }

    public void setGlobalNeedAuthorization(String globalNeedAuthorization) {
        this.globalNeedAuthorization = globalNeedAuthorization;
    }

    public String getTransferringAgency() {
        return transferringAgency;
    }

    public void setTransferringAgency(String transferringAgency) {
        this.transferringAgency = transferringAgency;
    }

    public String getArchivalAgency() {
        return archivalAgency;
    }

    public void setArchivalAgency(String archivalAgency) {
        this.archivalAgency = archivalAgency;
    }

    public String getArchivalProfile() {
        return archivalProfile;
    }

    public void setArchivalProfile(String archivalProfile) {
        this.archivalProfile = archivalProfile;
    }

    public String getSedaVersion() {
        return sedaVersion;
    }

    public void setSedaVersion(String sedaVersion) {
        this.sedaVersion = sedaVersion;
    }
}
