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
package fr.gouv.vitam.functional.administration.common;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.model.administration.ActivationStatus;
import fr.gouv.vitam.common.model.administration.IngestContractCheckState;
import org.bson.Document;

import java.util.Set;

/**
 * Defines an Ingest contract model for SIP transfer control. </BR>
 * It's an implementation of the SEDA specification and NF Z44022 MEDONA concerning the communication between a
 * TransferringAgency and an ArchivalAgency.
 */
public class IngestContract extends VitamDocument<IngestContract> {

    /**
     * the serial version uid
     */
    private static final long serialVersionUID = -3547871388720359674L;
    /**
     * the contract id
     */
    public static final String IDENTIFIER = "Identifier";
    /**
     * the contract name
     */
    public static final String NAME = "Name";
    /**
     * the contract description
     */
    public static final String DESCRIPTION = "Description";
    /**
     * the contract status
     */
    public static final String STATUS = "Status";

    /**
     * check parent link status (ACTIVE / INACTIVE)
     */
    public static final String CHECKPARENTLINK = "CheckParentLink";
    /**
     * Archive profile
     */
    public static final String ARCHIVEPROFILES = "ArchiveProfiles";

    /**
     * the creatation date of contract
     */
    public static final String CREATIONDATE = "CreationDate";
    /**
     * the last update of contract
     */
    public static final String LAST_UPDATE = "LastUpdate";
    /**
     * the activation date of contract
     */
    public static final String ACTIVATIONDATE = "ActivationDate";
    /**
     * the desactication date of contract
     */
    public static final String DEACTIVATIONDATE = "DeactivationDate";
    /**
     * the everydataobjectversion false by default
     */
    public static final String EVERYDATAOBJECTVERSION = "EveryDataObjectVersion";
    /**
     * the MasterMandatory true by default
     */
    public static final String MASTERMANDATORY = "MasterMandatory";
    /**
     * the FormatUnidentifiedAuthorized false by default
     */
    public static final String FORMATUNIDENTIFIEDAUTHORIZED = "FormatUnidentifiedAuthorized";
    /**
     * the EveryFormatType true by default
     */
    public static final String EVERYFORMATTYPE = "EveryFormatType";
    /**
     * the identifier of a management contract
     */
    public static final String MANAGEMENTCONTRACTID = "ManagementContractId";

    /**
     * Empty Constructor
     */
    public IngestContract() {
        // Empty
    }

    /**
     * Constructor
     *
     * @param document data in format Document to create contact
     */
    public IngestContract(Document document) {
        super(document);
    }

    /**
     * @param content in format JsonNode to create contract
     */
    public IngestContract(JsonNode content) {
        super(content);
    }

    /**
     * @param content in format String to create contract
     */
    public IngestContract(String content) {
        super(content);
    }

    /**
     * @param tenantId the working tenant
     */
    public IngestContract(Integer tenantId) {
        append(TENANT_ID, tenantId);
    }

    /**
     * @param id the id of ingest contract
     * @return AccessionRegisterDetail
     */
    public IngestContract setId(String id) {
        append(VitamDocument.ID, id);
        return this;
    }

    @Override
    public VitamDocument<IngestContract> newInstance(JsonNode content) {
        return new IngestContract(content);
    }

    /**
     * Name of the contract
     *
     * @return name of contract
     */
    public String getName() {
        return getString(NAME);
    }

    /**
     * Set or change the contract name
     *
     * @param name to set
     * @return this
     */
    public IngestContract setName(String name) {
        append(NAME, name);
        return this;
    }

    /**
     * Get the contract description
     *
     * @return this
     */
    public String getDescription() {
        return getString(DESCRIPTION);
    }

    /**
     * Set or change the contract description
     *
     * @param description to set to contact
     * @return this
     */
    public IngestContract setDescription(String description) {
        append(DESCRIPTION, description);
        return this;
    }

    /**
     * Get the contract status
     *
     * @return status of ingest contact
     */
    public ActivationStatus getStatus() {
        String status = getString(STATUS);
        if (status == null) {
            return null;
        }
        try {
            return ActivationStatus.valueOf(status);
        } catch (IllegalArgumentException exp) {
            throw new IllegalStateException("invalid STATUS value " + status);
        }
    }

    /**
     * Set or change the contract status
     *
     * @param status to set
     * @return this
     */
    public IngestContract setStatus(ActivationStatus status) {
        append(STATUS, status.name());
        return this;
    }

    /**
     * Set or change the contract status
     *
     * @param checkParentLink to set
     * @return this
     */
    public IngestContract setCheckParentLink(IngestContractCheckState checkParentLink) {
        append(CHECKPARENTLINK, checkParentLink.name());
        return this;
    }

    /**
     * Get the contract check ParentLink status
     *
     * @return status of checkParentLink for this ingest contact
     */
    public IngestContractCheckState getCheckParentLink() {
        String checkParentLink = getString(CHECKPARENTLINK);
        if (checkParentLink == null) {
            return null;
        }
        try {
            return IngestContractCheckState.valueOf(checkParentLink);
        } catch (IllegalArgumentException exp) {
            throw new IllegalStateException("invalid CHECKPARENTLINK value " + checkParentLink);
        }
    }

    /**
     * @return collection of archive profiles
     */
    public Set<String> getArchiveProfiles() {
        return (Set<String>) get(ARCHIVEPROFILES);
    }

    /**
     * Set the collection of archive profiles
     *
     * @param archiveProfiles
     * @return this
     */
    public IngestContract setArchiveProfiles(Set<String> archiveProfiles) {
        append(ARCHIVEPROFILES, archiveProfiles);
        return this;
    }

    /**
     * @return creation date of contract
     */
    public String getCreationdate() {
        return getString(CREATIONDATE);
    }

    /**
     * @param creationdate to set
     * @return this
     */
    public IngestContract setCreationdate(String creationdate) {
        append(CREATIONDATE, creationdate);
        return this;
    }

    /**
     * @return last update of contract
     */
    public String getLastupdate() {
        return getString(LAST_UPDATE);
    }

    /**
     * @param lastupdate to set
     * @return this
     */
    public IngestContract setLastupdate(String lastupdate) {
        append(LAST_UPDATE, lastupdate);
        return this;
    }

    /**
     * @return activation date of contract
     */
    public String getActivationdate() {
        return getString(ACTIVATIONDATE);
    }

    /**
     * @param activationdate to set
     * @return this
     */
    public IngestContract setActivationdate(String activationdate) {
        append(ACTIVATIONDATE, activationdate);
        return this;
    }

    /**
     * @return desactivation date of contract
     */
    public String getDeactivationdate() {
        return getString(DEACTIVATIONDATE);
    }

    /**
     * @param deactivationdate to set
     * @return this
     */
    public IngestContract setDeactivationdate(String deactivationdate) {
        append(DEACTIVATIONDATE, deactivationdate);
        return this;
    }

    public Boolean getEveryDataObjectVersion() {
        return getBoolean(EVERYDATAOBJECTVERSION);
    }

    public IngestContract setEveryDataObjectVersion(boolean everyDataObjectVersion) {
        append(EVERYDATAOBJECTVERSION, everyDataObjectVersion);
        return this;
    }

    public Boolean getMasterMandatory() {
        return getBoolean(MASTERMANDATORY);
    }

    public IngestContract setMasterMandatory(boolean masterMandatory) {
        append(MASTERMANDATORY, masterMandatory);
        return this;
    }

    public Boolean getEveryFormatType() {
        return getBoolean(EVERYFORMATTYPE);
    }

    public IngestContract setEveryFormatType(boolean everyFormatType) {
        append(EVERYFORMATTYPE, everyFormatType);
        return this;
    }

    public Boolean getFormatUnidentifiedAuthorized() {
        return getBoolean(FORMATUNIDENTIFIEDAUTHORIZED);
    }

    public IngestContract setFormatUnidentifiedAuthorized(boolean formatUnidentifiedAuthorized) {
        append(FORMATUNIDENTIFIEDAUTHORIZED, formatUnidentifiedAuthorized);
        return this;
    }

    public IngestContract setManagementContractId(String managementContractId) {
        append(MANAGEMENTCONTRACTID, managementContractId);
        return this;
    }

    public String getManagementContractId() {
        return getString(MANAGEMENTCONTRACTID);
    }
}
